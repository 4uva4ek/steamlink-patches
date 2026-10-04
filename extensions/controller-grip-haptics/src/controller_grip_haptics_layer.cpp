#include <openxr/openxr.h>
#include <openxr/openxr_loader_negotiation.h>

#include <android/binder_ibinder.h>
#include <android/binder_ibinder_jni.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <android/log.h>
#include <jni.h>
#include <sys/system_properties.h>
#include <time.h>

#include <atomic>
#include <cmath>
#include <cstdlib>
#include <cstring>

#define GXR_LOG(...) __android_log_print(ANDROID_LOG_INFO, "GxrHapticMain", __VA_ARGS__)

namespace {

constexpr char LAYER_NAME[] = "XR_APILAYER_local_GalaxyXR_haptic_main";

// The Galaxy XR controller service sends every OpenXR vibration to the trigger (SUB) vibrator.
// The grip (MAIN) vibrator is reachable only through the controller HAL, which an application
// cannot call. gxr.haptic.HapticService relays to it from a Shizuku user service; without that
// service every call goes to the runtime unchanged.
constexpr char SERVICE_DESCRIPTOR[] = "gxr.haptic.IHapticService";
constexpr transaction_code_t SERVICE_VIBRATE = 1;
constexpr transaction_code_t SERVICE_STOP = 2;
constexpr int32_t VIBRATOR_MAIN = 1;
// Limits the stock controller service applies before it calls the HAL.
constexpr int32_t MIN_DURATION_MS = 30;
constexpr float MIN_AMPLITUDE = 0.1f;
constexpr float HZ_PER_FREQUENCY_STEP = 50.0f;
constexpr float MIN_FREQUENCY = 1.0f;
constexpr float MAX_FREQUENCY = 10.0f;
// The grip vibrator is much weaker than the trigger one at the same amplitude and gets shrill above
// step 2. Values picked by feel on SteamVR dashboard ticks (21 ms, amplitude 0.16).
constexpr float DEFAULT_GAIN = 5.0f;
// The stock service stops at 0.8. The HAL sends amplitude * 100 in a 7-bit field, so 1.27 is the
// largest value that does not wrap; 0.8, 1.0 and 1.27 each feel stronger than the one before.
constexpr float DEFAULT_MAX_AMPLITUDE = 1.0f;
constexpr float AMPLITUDE_LIMIT = 1.27f;
constexpr float DEFAULT_FREQUENCY = 2.0f;
constexpr int32_t DEFAULT_MIN_DURATION_MS = 60;
constexpr int64_t TUNING_REFRESH_NS = 500000000;
constexpr int LOGGED_CALLS = 40;

enum Mode { MODE_OFF = 0, MODE_MAIN = 1, MODE_BOTH = 2 };

PFN_xrGetInstanceProcAddr NEXT_GET_INSTANCE_PROC_ADDR = nullptr;
PFN_xrApplyHapticFeedback NEXT_APPLY_HAPTIC_FEEDBACK = nullptr;
PFN_xrStopHapticFeedback NEXT_STOP_HAPTIC_FEEDBACK = nullptr;
XrPath LEFT_HAND = XR_NULL_PATH;
XrPath RIGHT_HAND = XR_NULL_PATH;
std::atomic<int> MODE{MODE_MAIN};
std::atomic<float> GAIN{DEFAULT_GAIN};
std::atomic<float> MAX_AMPLITUDE{DEFAULT_MAX_AMPLITUDE};
std::atomic<float> FREQUENCY{DEFAULT_FREQUENCY};
std::atomic<int32_t> MIN_DURATION{DEFAULT_MIN_DURATION_MS};
std::atomic<int64_t> TUNING_READ_AT{0};
// Per controller: when its current pulse ends, and whether a stop was already sent.
std::atomic<int64_t> BUSY_UNTIL[2]{};
std::atomic<bool> STOPPED[2]{};
std::atomic<AIBinder*> SERVICE{nullptr};
std::atomic<int> LOGGED{0};

float clamp(float value, float low, float high) {
    return value < low ? low : value > high ? high : value;
}

float readProperty(const char* name, float fallback) {
    char value[PROP_VALUE_MAX]{};
    if (__system_property_get(name, value) <= 0) return fallback;
    return static_cast<float>(std::atof(value));
}

// Tuning from adb, picked up while the stream runs:
//   debug.gxr.haptic        0|1|2  OpenXR only, grip, grip + trigger
//   debug.gxr.haptic.gain   amplitude multiplier for the grip vibrator
//   debug.gxr.haptic.max    strongest amplitude sent, up to 1.27
//   debug.gxr.haptic.freq   1..10 fixed HAL frequency step, 0 = derive from the OpenXR frequency
//   debug.gxr.haptic.minms  shortest pulse in milliseconds
int64_t monotonicNs() {
    timespec now{};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return now.tv_sec * 1000000000LL + now.tv_nsec;
}

void refreshTuning() {
    const int64_t nowNs = monotonicNs();
    const int64_t readAt = TUNING_READ_AT.load();
    if (readAt != 0 && nowNs - readAt < TUNING_REFRESH_NS) return;
    TUNING_READ_AT.store(nowNs);
    MODE.store(static_cast<int>(readProperty("debug.gxr.haptic", MODE_MAIN)));
    GAIN.store(readProperty("debug.gxr.haptic.gain", DEFAULT_GAIN));
    MAX_AMPLITUDE.store(
        clamp(readProperty("debug.gxr.haptic.max", DEFAULT_MAX_AMPLITUDE), MIN_AMPLITUDE, AMPLITUDE_LIMIT));
    FREQUENCY.store(readProperty("debug.gxr.haptic.freq", DEFAULT_FREQUENCY));
    MIN_DURATION.store(static_cast<int32_t>(readProperty("debug.gxr.haptic.minms", DEFAULT_MIN_DURATION_MS)));
}

void* serviceCreate(void*) { return nullptr; }
void serviceDestroy(void*) {}
binder_status_t serviceTransact(AIBinder*, transaction_code_t, const AParcel*, AParcel*) {
    return STATUS_UNKNOWN_TRANSACTION;
}

int32_t deviceFor(XrPath subactionPath) {
    if (subactionPath == LEFT_HAND) return 0;
    if (subactionPath == RIGHT_HAND) return 1;
    return -1;
}

binder_status_t sendVibrate(
    AIBinder* service,
    int32_t device,
    int32_t durationMs,
    float frequency,
    float amplitude
) {
    AParcel* in = nullptr;
    binder_status_t status = AIBinder_prepareTransaction(service, &in);
    if (status != STATUS_OK) return status;
    AParcel_writeInt32(in, device);
    AParcel_writeInt32(in, VIBRATOR_MAIN);
    AParcel_writeInt32(in, durationMs);
    AParcel_writeFloat(in, frequency);
    AParcel_writeFloat(in, amplitude);
    AParcel* out = nullptr;
    status = AIBinder_transact(service, SERVICE_VIBRATE, &in, &out, FLAG_ONEWAY);
    if (out) AParcel_delete(out);
    return status;
}

binder_status_t sendStop(AIBinder* service, int32_t device) {
    AParcel* in = nullptr;
    binder_status_t status = AIBinder_prepareTransaction(service, &in);
    if (status != STATUS_OK) return status;
    AParcel_writeInt32(in, device);
    AParcel* out = nullptr;
    status = AIBinder_transact(service, SERVICE_STOP, &in, &out, FLAG_ONEWAY);
    if (out) AParcel_delete(out);
    return status;
}

XrResult XRAPI_CALL layerApplyHapticFeedback(
    XrSession session,
    const XrHapticActionInfo* info,
    const XrHapticBaseHeader* haptic
) {
    refreshTuning();
    const int mode = MODE.load();
    AIBinder* service = SERVICE.load();
    if (!service || mode == MODE_OFF || !info || !haptic || haptic->type != XR_TYPE_HAPTIC_VIBRATION) {
        return NEXT_APPLY_HAPTIC_FEEDBACK(session, info, haptic);
    }
    const auto* vibration = reinterpret_cast<const XrHapticVibration*>(haptic);
    const int32_t device = deviceFor(info->subactionPath);
    int32_t durationMs = static_cast<int32_t>(vibration->duration / 1000000);
    const int32_t minDuration = MIN_DURATION.load() > MIN_DURATION_MS ? MIN_DURATION.load() : MIN_DURATION_MS;
    if (durationMs < minDuration) durationMs = minDuration;
    const float amplitude = clamp(vibration->amplitude * GAIN.load(), MIN_AMPLITUDE, MAX_AMPLITUDE.load());
    float frequency = FREQUENCY.load();
    if (frequency <= 0.0f) frequency = std::round(vibration->frequency / HZ_PER_FREQUENCY_STEP);
    frequency = clamp(frequency, MIN_FREQUENCY, MAX_FREQUENCY);

    // SteamVR can repeat a vibration every frame. The stock service drops requests while a pulse
    // is playing; without that the controller link floods and the controller loses tracking.
    binder_status_t status = STATUS_BAD_VALUE;
    if (device >= 0) {
        const int64_t nowNs = monotonicNs();
        if (vibration->amplitude <= 0.0f) {
            status = STOPPED[device].exchange(true) ? STATUS_OK : sendStop(service, device);
            BUSY_UNTIL[device].store(0);
        } else if (nowNs < BUSY_UNTIL[device].load()) {
            return mode == MODE_BOTH ? NEXT_APPLY_HAPTIC_FEEDBACK(session, info, haptic) : XR_SUCCESS;
        } else {
            BUSY_UNTIL[device].store(nowNs + durationMs * 1000000LL);
            STOPPED[device].store(false);
            status = sendVibrate(service, device, durationMs, frequency, amplitude);
        }
    }
    if (LOGGED.fetch_add(1) < LOGGED_CALLS) {
        GXR_LOG("vibration dur=%.1fms freq=%.0f amp=%.2f -> main device=%d dur=%dms freq=%.0f amp=%.2f status=%d",
            vibration->duration * 1e-6, vibration->frequency, vibration->amplitude, device, durationMs,
            frequency, amplitude, status);
    }
    if (mode == MODE_BOTH || status != STATUS_OK) return NEXT_APPLY_HAPTIC_FEEDBACK(session, info, haptic);
    return XR_SUCCESS;
}

XrResult XRAPI_CALL layerStopHapticFeedback(XrSession session, const XrHapticActionInfo* info) {
    AIBinder* service = SERVICE.load();
    if (service && MODE.load() != MODE_OFF && info) {
        const int32_t device = deviceFor(info->subactionPath);
        if (device >= 0) {
            if (!STOPPED[device].exchange(true)) sendStop(service, device);
            BUSY_UNTIL[device].store(0);
        }
    }
    return NEXT_STOP_HAPTIC_FEEDBACK(session, info);
}

XrResult XRAPI_PTR layerGetInstanceProcAddr(
    XrInstance instance,
    const char* name,
    PFN_xrVoidFunction* function
) {
    if (!name || !function) return XR_ERROR_VALIDATION_FAILURE;
    if (std::strcmp(name, "xrGetInstanceProcAddr") == 0) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(layerGetInstanceProcAddr);
        return XR_SUCCESS;
    }
    if (!NEXT_GET_INSTANCE_PROC_ADDR) {
        *function = nullptr;
        return XR_ERROR_FUNCTION_UNSUPPORTED;
    }
    if (NEXT_APPLY_HAPTIC_FEEDBACK && std::strcmp(name, "xrApplyHapticFeedback") == 0) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(layerApplyHapticFeedback);
        return XR_SUCCESS;
    }
    if (NEXT_STOP_HAPTIC_FEEDBACK && std::strcmp(name, "xrStopHapticFeedback") == 0) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(layerStopHapticFeedback);
        return XR_SUCCESS;
    }
    return NEXT_GET_INSTANCE_PROC_ADDR(instance, name, function);
}

XrResult XRAPI_PTR layerCreateApiLayerInstance(
    const XrInstanceCreateInfo* instanceCreateInfo,
    const XrApiLayerCreateInfo* apiLayerInfo,
    XrInstance* instance
) {
    if (!apiLayerInfo || !apiLayerInfo->nextInfo) return XR_ERROR_INITIALIZATION_FAILED;
    XrApiLayerCreateInfo nextInfo = *apiLayerInfo;
    nextInfo.nextInfo = apiLayerInfo->nextInfo->next;
    const XrResult result = apiLayerInfo->nextInfo->nextCreateApiLayerInstance(
        instanceCreateInfo,
        &nextInfo,
        instance
    );
    if (XR_FAILED(result)) return result;

    NEXT_GET_INSTANCE_PROC_ADDR = apiLayerInfo->nextInfo->nextGetInstanceProcAddr;
    NEXT_GET_INSTANCE_PROC_ADDR(*instance, "xrApplyHapticFeedback",
        reinterpret_cast<PFN_xrVoidFunction*>(&NEXT_APPLY_HAPTIC_FEEDBACK));
    NEXT_GET_INSTANCE_PROC_ADDR(*instance, "xrStopHapticFeedback",
        reinterpret_cast<PFN_xrVoidFunction*>(&NEXT_STOP_HAPTIC_FEEDBACK));
    PFN_xrStringToPath stringToPath = nullptr;
    NEXT_GET_INSTANCE_PROC_ADDR(*instance, "xrStringToPath",
        reinterpret_cast<PFN_xrVoidFunction*>(&stringToPath));
    if (stringToPath) {
        stringToPath(*instance, "/user/hand/left", &LEFT_HAND);
        stringToPath(*instance, "/user/hand/right", &RIGHT_HAND);
    }
    refreshTuning();
    GXR_LOG("mode=%d gain=%.1f service=%d", MODE.load(), GAIN.load(), SERVICE.load() != nullptr);
    return XR_SUCCESS;
}

}  // namespace

extern "C" JNIEXPORT void JNICALL Java_gxr_haptic_HapticProvider_nativeSetBinder(
    JNIEnv* env,
    jclass,
    jobject binder
) {
    AIBinder* service = nullptr;
    if (binder) {
        service = AIBinder_fromJavaBinder(env, binder);
        const AIBinder_Class* clazz =
            AIBinder_Class_define(SERVICE_DESCRIPTOR, serviceCreate, serviceDestroy, serviceTransact);
        if (service && !AIBinder_associateClass(service, clazz)) {
            GXR_LOG("user service has an unexpected interface");
            AIBinder_decStrong(service);
            service = nullptr;
        }
    }
    AIBinder* previous = SERVICE.exchange(service);
    if (previous) AIBinder_decStrong(previous);
    GXR_LOG("grip vibrator %s", service ? "available" : "not available");
}

extern "C" __attribute__((visibility("default"))) XrResult XRAPI_CALL xrNegotiateLoaderApiLayerInterface(
    const XrNegotiateLoaderInfo* loaderInfo,
    const char* layerName,
    XrNegotiateApiLayerRequest* request
) {
    if (!loaderInfo || !layerName || !request || std::strcmp(layerName, LAYER_NAME) != 0) {
        return XR_ERROR_INITIALIZATION_FAILED;
    }
    if (loaderInfo->maxInterfaceVersion < XR_CURRENT_LOADER_API_LAYER_VERSION ||
        loaderInfo->maxApiVersion < XR_CURRENT_API_VERSION) {
        return XR_ERROR_INITIALIZATION_FAILED;
    }
    request->layerInterfaceVersion = XR_CURRENT_LOADER_API_LAYER_VERSION;
    request->layerApiVersion = XR_CURRENT_API_VERSION;
    request->getInstanceProcAddr = layerGetInstanceProcAddr;
    request->createApiLayerInstance = layerCreateApiLayerInstance;
    return XR_SUCCESS;
}
