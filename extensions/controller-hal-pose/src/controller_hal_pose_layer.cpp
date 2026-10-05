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
#include <mutex>
#include <thread>
#include <unordered_map>

#define GXR_LOG(...) __android_log_print(ANDROID_LOG_INFO, "GxrHalPose", __VA_ARGS__)

namespace {

constexpr char LAYER_NAME[] = "XR_APILAYER_local_GalaxyXR_controller_hal_pose";

// VRLink streams the controllers from this pose action (bound to the controller grip pose).
constexpr char STREAM_POSE_ACTION[] = "pamir-stream-pose";

// The Galaxy XR runtime hands an application one controller pose per display frame. The
// controller HAL fuses the controller's IMU at about 1 kHz and predicts a pose for any requested
// time, but an application cannot call it. gxr.pose.PoseService relays to it from a Shizuku user
// service; without that service every call goes to the runtime unchanged.
constexpr char SERVICE_DESCRIPTOR[] = "gxr.pose.IPoseService";
constexpr transaction_code_t SERVICE_POSES = 1;
constexpr int HAL_REPLY_WORDS = 35;
constexpr int CONTROLLERS = 2;
// VRLink locates both controllers back to back; one HAL read serves both.
constexpr int64_t REUSE_NS = 1500000;

// Measured 2026-10-04 on still controllers: runtime grip pose = A * HAL pose * B, where B is a
// pitch of 42.25 degrees about +X with no offset (both hands, residual 0.04 degrees / 0.03 mm) and
// A is the session's base space against the HAL's world (a yaw and an offset; it changes when the
// session restarts or recenters). In motion the runtime's pose trails the HAL's pose for "now" by
// about 35 ms, so A is taken only while the controller is nearly still.
constexpr double DEFAULT_GRIP_PITCH_DEG = 42.25;
// Loose limits find the base space quickly; after that only calmer samples refine it, because
// its rotation error is multiplied by the distance to the HAL's origin (about 1.5 m).
constexpr float FIND_LINEAR = 0.05f;
constexpr float FIND_ANGULAR = 0.25f;
constexpr float STILL_LINEAR = 0.02f;
constexpr float STILL_ANGULAR = 0.1f;
// A still sample further than this from the current A counts towards a jump of the base space.
constexpr float JUMP_ANGLE = 0.05f;
constexpr float JUMP_DISTANCE = 0.03f;
constexpr int JUMP_SAMPLES = 8;
constexpr double BLEND = 0.005;

// Jitter filter on the reported pose: a low-pass whose cutoff rises with the controller's speed
// (the HAL's own velocities), so a resting hand is smoothed hard and a fast one barely lags.
// Cutoff = MIN_CUTOFF + BETA * speed, in Hz; speed in m/s for the position, rad/s for the rotation.
constexpr float DEFAULT_POSITION_MIN_CUTOFF = 3.0f;
constexpr float DEFAULT_POSITION_BETA = 60.0f;
constexpr float DEFAULT_ROTATION_MIN_CUTOFF = 3.0f;
constexpr float DEFAULT_ROTATION_BETA = 60.0f;
// The same low-pass on the velocities, where the noise of the controller's accelerometer (linear)
// and gyroscope (angular) shows; the raw IMU samples themselves are not available to the layer.
constexpr float DEFAULT_LINEAR_MIN_CUTOFF = 10.0f;
constexpr float DEFAULT_LINEAR_BETA = 40.0f;
constexpr float DEFAULT_ANGULAR_MIN_CUTOFF = 10.0f;
constexpr float DEFAULT_ANGULAR_BETA = 10.0f;
constexpr int64_t FILTER_RESET_NS = 100000000LL;
constexpr int64_t TUNING_REFRESH_NS = 1000000000LL;

// VRLink asks for a controller pose four times per display frame. A thread of the layer reads the
// HAL more often than that and runs the jitter filter on every read, so the filter averages more
// samples over the same time and the reported pose is quieter without trailing further behind.
// The default is VRLink's own request rate, 360 per second: at 1000 the pose looked no smoother
// in the headset.
constexpr int DEFAULT_POLL_HZ = 360;
constexpr int MAX_POLL_HZ = 2000;
// The thread reads only while poses are being asked for.
constexpr int64_t POLL_IDLE_NS = 300000000LL;
constexpr int64_t POLL_FRESH_NS = 20000000LL;
constexpr int64_t STATS_NS = 5000000000LL;

// The HAL's velocities, checked against the motion of its own poses in the same recording: the
// linear one is in the HAL's world axes (5 degrees off the positions' displacement; 26-31 degrees
// if read as local to the controller) and the angular one is in the controller's own axes
// (5 degrees; 21-25 degrees if read as a world vector). The runtime forwards both unconverted.
// Reported the way VRLink's receiver reads them: linear in the base space, angular local to the
// grip pose, which is what the controller velocity frame layer produces from the runtime's.
// Milliseconds added to "now" in every HAL request; chosen by feel on the headset.
constexpr double DEFAULT_AHEAD_MS = 3.0;
// The HAL computes a pose only for a time later than the latest one anybody has asked it for;
// a request for an earlier time gets a copy of an older reply. The system's controller service
// asks once per display frame for that frame's display time, some 20 ms ahead, so a request
// for "now" mostly got the system's per-frame answers, hopping between two neighbouring frames
// (measured 2026-10-05: with requests for now + 30 ms 0.1 % of the replies went backwards along
// the hand's path, against 26-43 % for requests for now). So the HAL is asked this far ahead
// and the reply is stepped back to the wanted time along its own velocities.
constexpr double DEFAULT_LEAD_MS = 30.0;

// The HAL's reply words after the exception code.
struct HalPose {
    int32_t words[HAL_REPLY_WORDS];
    bool valid = false;

    float f(int index) const {
        float value;
        std::memcpy(&value, &words[index], sizeof(value));
        return value;
    }
};

PFN_xrGetInstanceProcAddr NEXT_GET_INSTANCE_PROC_ADDR = nullptr;
PFN_xrCreateAction NEXT_CREATE_ACTION = nullptr;
PFN_xrDestroyAction NEXT_DESTROY_ACTION = nullptr;
PFN_xrCreateActionSpace NEXT_CREATE_ACTION_SPACE = nullptr;
PFN_xrDestroySpace NEXT_DESTROY_SPACE = nullptr;
PFN_xrDestroySession NEXT_DESTROY_SESSION = nullptr;
PFN_xrLocateSpace NEXT_LOCATE_SPACE = nullptr;
XrPath LEFT_HAND = XR_NULL_PATH;
XrPath RIGHT_HAND = XR_NULL_PATH;

std::mutex MUTEX;
std::unordered_map<XrAction, int> STREAM_ACTIONS;
// Controller (0 = left, 1 = right) per stream pose space.
std::unordered_map<XrSpace, int> STREAM_SPACES;
std::atomic<bool> ENABLED{true};
std::atomic<AIBinder*> SERVICE{nullptr};

struct Quat {
    double x = 0, y = 0, z = 0, w = 1;
};
struct Vec {
    double x = 0, y = 0, z = 0;
};

Quat mul(const Quat& a, const Quat& b) {
    return {
        a.w * b.x + a.x * b.w + a.y * b.z - a.z * b.y,
        a.w * b.y - a.x * b.z + a.y * b.w + a.z * b.x,
        a.w * b.z + a.x * b.y - a.y * b.x + a.z * b.w,
        a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z,
    };
}

Quat conj(const Quat& q) { return {-q.x, -q.y, -q.z, q.w}; }

Quat normalized(const Quat& q) {
    const double n = std::sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w);
    return n > 0 ? Quat{q.x / n, q.y / n, q.z / n, q.w / n} : Quat{};
}

Vec rotate(const Quat& q, const Vec& v) {
    const double cx = q.y * v.z - q.z * v.y, cy = q.z * v.x - q.x * v.z, cz = q.x * v.y - q.y * v.x;
    const double dx = q.y * cz - q.z * cy, dy = q.z * cx - q.x * cz, dz = q.x * cy - q.y * cx;
    return {v.x + 2 * (q.w * cx + dx), v.y + 2 * (q.w * cy + dy), v.z + 2 * (q.w * cz + dz)};
}

// Angle between two rotations, radians.
double angle(const Quat& a, const Quat& b) {
    const double dot = std::fabs(a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w);
    return 2.0 * std::acos(dot > 1.0 ? 1.0 : dot);
}

// The base space against the HAL's world, shared by both controllers.
struct BaseSpace {
    bool known = false;
    Quat rotation;
    Vec offset;
    int jumpSamples = 0;
    int samples = 0;
};

struct Filter {
    int64_t at = 0;
    Vec position;
    Quat rotation;
    // In the HAL's frames: world axes and the controller's own axes.
    Vec linearVelocity;
    Vec angularVelocity;
};

std::mutex BASE_MUTEX;
BaseSpace BASE;
Quat GRIP_PITCH;
std::atomic<int64_t> AHEAD_NS{0};
std::atomic<int64_t> LEAD_NS{static_cast<int64_t>(DEFAULT_LEAD_MS * 1e6)};
std::atomic<bool> VELOCITIES{true};
Filter FILTERS[CONTROLLERS];
bool FILTER_ON = true;
float POSITION_MIN_CUTOFF = DEFAULT_POSITION_MIN_CUTOFF;
float POSITION_BETA = DEFAULT_POSITION_BETA;
float ROTATION_MIN_CUTOFF = DEFAULT_ROTATION_MIN_CUTOFF;
float ROTATION_BETA = DEFAULT_ROTATION_BETA;
float LINEAR_MIN_CUTOFF = DEFAULT_LINEAR_MIN_CUTOFF;
float LINEAR_BETA = DEFAULT_LINEAR_BETA;
float ANGULAR_MIN_CUTOFF = DEFAULT_ANGULAR_MIN_CUTOFF;
float ANGULAR_BETA = DEFAULT_ANGULAR_BETA;
int64_t TUNING_READ_AT = 0;
int64_t STATS_AT = 0;
int STATS_REPLACED = 0, STATS_PASSED = 0, STATS_STILL = 0, STATS_JUMPS = 0;
int STATS_HAL_LOST = 0, STATS_RUNTIME_LOST = 0, STATS_BRIDGED = 0;
// A HAL reply that still carries a position but is not marked tracked is used for this long
// after the last tracked one, so a short loss does not switch to the runtime's pose and back.
constexpr int64_t BRIDGE_NS = 500000000LL;
int64_t TRACKED_AT[CONTROLLERS] = {};
double STATS_SHIFT = 0;

std::mutex HAL_MUTEX;
HalPose HAL_POSES[CONTROLLERS];
int64_t HAL_READ_AT = 0;
int64_t HAL_CALL_NS = 0;
std::atomic<int> POLL_HZ{DEFAULT_POLL_HZ};
std::atomic<bool> POLL_STARTED{false};
std::atomic<int64_t> LOCATED_AT{0};
std::atomic<int> STATS_POLLS{0};
std::atomic<int64_t> STATS_READ_NS{0}, STATS_READ_MAX_NS{0}, STATS_LATE_MAX_NS{0};
std::atomic<int64_t> REPLACED_AT{0};
std::atomic<int64_t> VELOCITY_AT{0};
constexpr int64_t ACTIVE_NS = 300000000LL;

int64_t monotonicNs() {
    timespec now{};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return now.tv_sec * 1000000000LL + now.tv_nsec;
}

float readFloat(const char* name, float fallback, float low, float high) {
    char value[PROP_VALUE_MAX]{};
    if (__system_property_get(name, value) <= 0) return fallback;
    char* end = nullptr;
    const float parsed = std::strtof(value, &end);
    return end != value && parsed >= low && parsed <= high ? parsed : fallback;
}

// Read once a second while poses are replaced (BASE_MUTEX held):
//   debug.gxr.halpose.filter      0 = report the HAL's pose unfiltered (default 1)
//   debug.gxr.halpose.pos.cutoff  position cutoff at rest, Hz (3)
//   debug.gxr.halpose.pos.beta    position cutoff added per m/s, Hz (60)
//   debug.gxr.halpose.rot.cutoff  rotation cutoff at rest, Hz (3)
//   debug.gxr.halpose.rot.beta    rotation cutoff added per rad/s, Hz (60)
//   debug.gxr.halpose.ahead       milliseconds added to "now" in the HAL request (3)
//   debug.gxr.halpose.lin.cutoff  linear velocity cutoff at rest, Hz (10)
//   debug.gxr.halpose.lin.beta    linear velocity cutoff added per m/s, Hz (40)
//   debug.gxr.halpose.ang.cutoff  angular velocity cutoff at rest, Hz (10)
//   debug.gxr.halpose.ang.beta    angular velocity cutoff added per rad/s, Hz (10)
//   debug.gxr.halpose.lead        how far ahead of now the HAL is asked, milliseconds (30); the
//                                 reply is stepped back to now + ahead
void refreshTuning(int64_t now) {
    if (TUNING_READ_AT != 0 && now - TUNING_READ_AT < TUNING_REFRESH_NS) return;
    const bool first = TUNING_READ_AT == 0;
    TUNING_READ_AT = now;
    const float aheadMs =
        readFloat("debug.gxr.halpose.ahead", static_cast<float>(DEFAULT_AHEAD_MS), -50.0f, 100.0f);
    const int64_t aheadNs = static_cast<int64_t>(aheadMs * 1e6);
    if (!first && aheadNs != AHEAD_NS.load()) GXR_LOG("ahead=%.1fms", aheadMs);
    AHEAD_NS.store(aheadNs);
    const float leadMs =
        readFloat("debug.gxr.halpose.lead", static_cast<float>(DEFAULT_LEAD_MS), 0.0f, 100.0f);
    const int64_t leadNs = static_cast<int64_t>(leadMs * 1e6);
    if (!first && leadNs != LEAD_NS.load()) GXR_LOG("lead=%.1fms", leadMs);
    LEAD_NS.store(leadNs);
    const bool on = readFloat("debug.gxr.halpose.filter", 1.0f, 0.0f, 1.0f) != 0.0f;
    const float positionCutoff =
        readFloat("debug.gxr.halpose.pos.cutoff", DEFAULT_POSITION_MIN_CUTOFF, 0.05f, 1000.0f);
    const float positionBeta = readFloat("debug.gxr.halpose.pos.beta", DEFAULT_POSITION_BETA, 0.0f, 10000.0f);
    const float rotationCutoff =
        readFloat("debug.gxr.halpose.rot.cutoff", DEFAULT_ROTATION_MIN_CUTOFF, 0.05f, 1000.0f);
    const float rotationBeta = readFloat("debug.gxr.halpose.rot.beta", DEFAULT_ROTATION_BETA, 0.0f, 10000.0f);
    const float linearCutoff =
        readFloat("debug.gxr.halpose.lin.cutoff", DEFAULT_LINEAR_MIN_CUTOFF, 0.05f, 1000.0f);
    const float linearBeta = readFloat("debug.gxr.halpose.lin.beta", DEFAULT_LINEAR_BETA, 0.0f, 10000.0f);
    const float angularCutoff =
        readFloat("debug.gxr.halpose.ang.cutoff", DEFAULT_ANGULAR_MIN_CUTOFF, 0.05f, 1000.0f);
    const float angularBeta = readFloat("debug.gxr.halpose.ang.beta", DEFAULT_ANGULAR_BETA, 0.0f, 10000.0f);
    if (first || on != FILTER_ON || positionCutoff != POSITION_MIN_CUTOFF || positionBeta != POSITION_BETA ||
        rotationCutoff != ROTATION_MIN_CUTOFF || rotationBeta != ROTATION_BETA ||
        linearCutoff != LINEAR_MIN_CUTOFF || linearBeta != LINEAR_BETA ||
        angularCutoff != ANGULAR_MIN_CUTOFF || angularBeta != ANGULAR_BETA) {
        GXR_LOG("filter %s: position %.2f Hz + %.1f per m/s, rotation %.2f Hz + %.1f per rad/s, "
            "linear velocity %.2f Hz + %.1f per m/s, angular velocity %.2f Hz + %.1f per rad/s",
            on ? "on" : "off", positionCutoff, positionBeta, rotationCutoff, rotationBeta,
            linearCutoff, linearBeta, angularCutoff, angularBeta);
    }
    LINEAR_MIN_CUTOFF = linearCutoff;
    LINEAR_BETA = linearBeta;
    ANGULAR_MIN_CUTOFF = angularCutoff;
    ANGULAR_BETA = angularBeta;
    FILTER_ON = on;
    POSITION_MIN_CUTOFF = positionCutoff;
    POSITION_BETA = positionBeta;
    ROTATION_MIN_CUTOFF = rotationCutoff;
    ROTATION_BETA = rotationBeta;
}

// Share of the way from the filtered value to the new one for this cutoff and time step.
double blendFor(double cutoffHz, double seconds) {
    return 1.0 - std::exp(-2.0 * 3.14159265358979323846 * cutoffHz * seconds);
}

Vec blended(const Vec& last, const Vec& next, double share) {
    return {last.x + (next.x - last.x) * share, last.y + (next.y - last.y) * share,
        last.z + (next.z - last.z) * share};
}

// `linear` and `angular` are the unfiltered speeds; the velocities are filtered in place.
void filterPose(
    Filter& filter,
    int64_t now,
    float linear,
    float angular,
    Vec* position,
    Quat* rotation,
    Vec* linearVelocity,
    Vec* angularVelocity
) {
    const int64_t step = now - filter.at;
    // A read the reading thread already filtered, or one older than its latest: VRLink's request
    // can pick a read up just before the thread filters the next one. Report the filtered state
    // instead of letting the raw sample through and resetting the filter to it.
    if (filter.at != 0 && step <= 0) {
        *position = filter.position;
        *rotation = filter.rotation;
        *linearVelocity = filter.linearVelocity;
        *angularVelocity = filter.angularVelocity;
        return;
    }
    if (FILTER_ON && filter.at != 0 && step > 0 && step < FILTER_RESET_NS) {
        const double seconds = step / 1e9;
        const double p = blendFor(POSITION_MIN_CUTOFF + POSITION_BETA * linear, seconds);
        position->x = filter.position.x + (position->x - filter.position.x) * p;
        position->y = filter.position.y + (position->y - filter.position.y) * p;
        position->z = filter.position.z + (position->z - filter.position.z) * p;
        const double r = blendFor(ROTATION_MIN_CUTOFF + ROTATION_BETA * angular, seconds);
        Quat next = *rotation;
        const Quat& last = filter.rotation;
        if (next.x * last.x + next.y * last.y + next.z * last.z + next.w * last.w < 0) {
            next = {-next.x, -next.y, -next.z, -next.w};
        }
        *rotation = normalized({last.x + (next.x - last.x) * r, last.y + (next.y - last.y) * r,
            last.z + (next.z - last.z) * r, last.w + (next.w - last.w) * r});
        *linearVelocity = blended(filter.linearVelocity, *linearVelocity,
            blendFor(LINEAR_MIN_CUTOFF + LINEAR_BETA * linear, seconds));
        *angularVelocity = blended(filter.angularVelocity, *angularVelocity,
            blendFor(ANGULAR_MIN_CUTOFF + ANGULAR_BETA * angular, seconds));
    }
    filter.at = now;
    filter.position = *position;
    filter.rotation = *rotation;
    filter.linearVelocity = *linearVelocity;
    filter.angularVelocity = *angularVelocity;
}

// Read when the instance is created:
//   debug.gxr.halpose        0 = report the runtime's pose unchanged
//   debug.gxr.halpose.pitch  pitch of the grip pose against the HAL's pose, degrees (42.25)
//   debug.gxr.halpose.velocity  0 = leave the runtime's velocities in place
//   debug.gxr.halpose.hz     HAL reads per second by the layer's own thread (360); 0 = read only
//                            when VRLink asks for a pose
void readConfig() {
    char value[PROP_VALUE_MAX]{};
    ENABLED.store(__system_property_get("debug.gxr.halpose", value) <= 0 || std::atoi(value) != 0);
    double aheadMs = DEFAULT_AHEAD_MS;
    if (__system_property_get("debug.gxr.halpose.ahead", value) > 0) aheadMs = std::atof(value);
    if (!(aheadMs >= -50.0 && aheadMs <= 100.0)) aheadMs = DEFAULT_AHEAD_MS;
    AHEAD_NS.store(static_cast<int64_t>(aheadMs * 1e6));
    double pitch = DEFAULT_GRIP_PITCH_DEG;
    if (__system_property_get("debug.gxr.halpose.pitch", value) > 0) pitch = std::atof(value);
    VELOCITIES.store(__system_property_get("debug.gxr.halpose.velocity", value) <= 0 || std::atoi(value) != 0);
    int pollHz = DEFAULT_POLL_HZ;
    if (__system_property_get("debug.gxr.halpose.hz", value) > 0) pollHz = std::atoi(value);
    POLL_HZ.store(pollHz < 0 ? 0 : pollHz > MAX_POLL_HZ ? MAX_POLL_HZ : pollHz);
    const double half = pitch * 3.14159265358979323846 / 360.0;
    GRIP_PITCH = {std::sin(half), 0, 0, std::cos(half)};
    {
        std::lock_guard<std::mutex> lock(BASE_MUTEX);
        BASE = BaseSpace{};
        for (Filter& filter : FILTERS) filter = Filter{};
        TUNING_READ_AT = 0;
    }
    GXR_LOG("controller HAL poses %s, velocities %s, ahead=%.1fms pitch=%.2f poll=%dHz service=%d",
        ENABLED.load() ? "on" : "off", VELOCITIES.load() ? "on" : "off", aheadMs, pitch, POLL_HZ.load(),
        SERVICE.load() != nullptr);
}

void* serviceCreate(void*) { return nullptr; }
void serviceDestroy(void*) {}
binder_status_t serviceTransact(AIBinder*, transaction_code_t, const AParcel*, AParcel*) {
    return STATUS_UNKNOWN_TRANSACTION;
}

bool readHal(AIBinder* service, int64_t timeNs, HalPose* poses) {
    AParcel* in = nullptr;
    if (AIBinder_prepareTransaction(service, &in) != STATUS_OK) return false;
    AParcel_writeInt64(in, timeNs);
    AParcel* out = nullptr;
    if (AIBinder_transact(service, SERVICE_POSES, &in, &out, 0) != STATUS_OK) {
        if (out) AParcel_delete(out);
        return false;
    }
    int32_t exception = -1;
    bool complete = AParcel_readInt32(out, &exception) == STATUS_OK && exception == 0;
    for (int device = 0; complete && device < CONTROLLERS; ++device) {
        int32_t present = 0;
        complete = AParcel_readInt32(out, &present) == STATUS_OK;
        poses[device].valid = false;
        if (!complete || !present) continue;
        for (int word = 0; complete && word < HAL_REPLY_WORDS; ++word) {
            complete = AParcel_readInt32(out, &poses[device].words[word]) == STATUS_OK;
        }
        poses[device].valid = complete;
    }
    AParcel_delete(out);
    return complete;
}

// Moves a reply from the time it was asked for back by `backNs` along its own velocities: the
// linear one is in the HAL's world axes, the angular one in the controller's.
void stepBack(HalPose& pose, int64_t backNs) {
    // Words: 21 and 27 say the linear and the angular velocity are present.
    if (backNs <= 0 || !pose.valid || pose.words[21] == 0 || pose.words[27] == 0) return;
    const double seconds = backNs / 1e9;
    const auto put = [&pose](int index, double value) {
        const float narrow = static_cast<float>(value);
        std::memcpy(&pose.words[index], &narrow, sizeof(narrow));
    };
    for (int axis = 0; axis < 3; ++axis) put(18 + axis, pose.f(18 + axis) - pose.f(24 + axis) * seconds);
    const double wx = pose.f(30), wy = pose.f(31), wz = pose.f(32);
    const double speed = std::sqrt(wx * wx + wy * wy + wz * wz);
    if (speed < 1e-6) return;
    const double half = -speed * seconds / 2;
    const double scale = std::sin(half) / speed;
    const Quat turned = normalized(mul(
        normalized({pose.f(11), pose.f(12), pose.f(13), pose.f(14)}),
        {wx * scale, wy * scale, wz * scale, std::cos(half)}));
    put(11, turned.x);
    put(12, turned.y);
    put(13, turned.z);
    put(14, turned.w);
}

// How far ahead of `wanted` (nanoseconds from now) the HAL has to be asked.
int64_t askAhead(int64_t wanted) {
    const int64_t lead = LEAD_NS.load();
    return wanted > lead ? wanted : lead;
}

bool halPose(int device, HalPose* pose, int64_t* readAt) {
    AIBinder* service = SERVICE.load();
    if (!service) return false;
    std::lock_guard<std::mutex> lock(HAL_MUTEX);
    const int64_t now = monotonicNs();
    const int64_t reuse = POLL_STARTED.load() && POLL_HZ.load() > 0 ? POLL_FRESH_NS : REUSE_NS;
    if (HAL_READ_AT == 0 || now - HAL_READ_AT > reuse) {
        const int64_t wanted = AHEAD_NS.load();
        const int64_t asked = askAhead(wanted);
        if (!readHal(service, now + asked, HAL_POSES)) return false;
        for (HalPose& read : HAL_POSES) stepBack(read, asked - wanted);
        HAL_READ_AT = now;
        HAL_CALL_NS = monotonicNs() - now;
    }
    *pose = HAL_POSES[device];
    *readAt = HAL_READ_AT;
    return pose->valid;
}

XrResult XRAPI_PTR layerCreateAction(
    XrActionSet actionSet,
    const XrActionCreateInfo* createInfo,
    XrAction* action
) {
    const XrResult result = NEXT_CREATE_ACTION(actionSet, createInfo, action);
    if (XR_SUCCEEDED(result) && createInfo->actionType == XR_ACTION_TYPE_POSE_INPUT &&
        std::strcmp(createInfo->actionName, STREAM_POSE_ACTION) == 0) {
        std::lock_guard<std::mutex> lock(MUTEX);
        STREAM_ACTIONS[*action] = 1;
    }
    return result;
}

XrResult XRAPI_PTR layerDestroyAction(XrAction action) {
    const XrResult result = NEXT_DESTROY_ACTION(action);
    if (XR_SUCCEEDED(result)) {
        std::lock_guard<std::mutex> lock(MUTEX);
        STREAM_ACTIONS.erase(action);
    }
    return result;
}

XrResult XRAPI_PTR layerCreateActionSpace(
    XrSession session,
    const XrActionSpaceCreateInfo* createInfo,
    XrSpace* space
) {
    const XrResult result = NEXT_CREATE_ACTION_SPACE(session, createInfo, space);
    if (XR_SUCCEEDED(result)) {
        std::lock_guard<std::mutex> lock(MUTEX);
        if (STREAM_ACTIONS.count(createInfo->action) != 0) {
            const int device = createInfo->subactionPath == LEFT_HAND ? 0
                : createInfo->subactionPath == RIGHT_HAND ? 1 : -1;
            GXR_LOG("controller stream pose space, controller %d", device);
            if (device >= 0) STREAM_SPACES[*space] = device;
        }
    }
    return result;
}

XrResult XRAPI_PTR layerDestroySpace(XrSpace space) {
    {
        std::lock_guard<std::mutex> lock(MUTEX);
        STREAM_SPACES.erase(space);
    }
    return NEXT_DESTROY_SPACE(space);
}

XrResult XRAPI_PTR layerDestroySession(XrSession session) {
    {
        // Destroying the session destroys its spaces without xrDestroySpace calls.
        std::lock_guard<std::mutex> lock(MUTEX);
        STREAM_SPACES.clear();
    }
    return NEXT_DESTROY_SESSION(session);
}

// Reports the HAL's pose in place of the runtime's. The runtime's pose is still used, while the
// controller is nearly still, to learn where the base space sits in the HAL's world.
XrSpaceVelocity* velocityOf(XrSpaceLocation* location) {
    for (auto* next = static_cast<XrBaseOutStructure*>(location->next); next; next = next->next) {
        if (next->type == XR_TYPE_SPACE_VELOCITY) return reinterpret_cast<XrSpaceVelocity*>(next);
    }
    return nullptr;
}

bool isTracked(const HalPose& hal) {
    // Words: 0 result, 15 position present, 34 state (2 = tracked).
    return hal.valid && hal.words[0] == 0 && hal.words[15] != 0 && hal.words[34] == 2;
}

bool hasPose(const HalPose& hal) {
    return hal.valid && hal.words[0] == 0 && hal.words[15] != 0;
}

// Reads the HAL at its own rate and filters every read. A pose is asked for the moment VRLink
// will, on average, pick it up: half a period and one read later.
void pollLoop() {
    int64_t next = monotonicNs();
    while (true) {
        const int hz = POLL_HZ.load();
        AIBinder* service = SERVICE.load();
        const int64_t now = monotonicNs();
        if (hz <= 0 || !service || !ENABLED.load() || now - LOCATED_AT.load() > POLL_IDLE_NS) {
            timespec idle{0, 50000000};
            nanosleep(&idle, nullptr);
            next = monotonicNs();
            continue;
        }
        if (now - next > STATS_LATE_MAX_NS.load()) STATS_LATE_MAX_NS.store(now - next);
        const int64_t period = 1000000000LL / hz;
        HalPose poses[CONTROLLERS];
        int64_t lead = 0;
        {
            std::lock_guard<std::mutex> lock(HAL_MUTEX);
            lead = HAL_CALL_NS + period / 2;
        }
        const int64_t wanted = AHEAD_NS.load() + lead;
        const int64_t asked = askAhead(wanted);
        if (readHal(service, now + asked, poses)) {
            const int64_t done = monotonicNs();
            for (HalPose& read : poses) stepBack(read, asked - wanted);
            {
                std::lock_guard<std::mutex> lock(HAL_MUTEX);
                for (int device = 0; device < CONTROLLERS; ++device) HAL_POSES[device] = poses[device];
                HAL_READ_AT = now;
                // Smoothed: one slow read must not push the next requests far ahead.
                HAL_CALL_NS += (done - now - HAL_CALL_NS) / 16;
            }
            ++STATS_POLLS;
            STATS_READ_NS += done - now;
            if (done - now > STATS_READ_MAX_NS.load()) STATS_READ_MAX_NS.store(done - now);
            std::lock_guard<std::mutex> lock(BASE_MUTEX);
            for (int device = 0; BASE.known && device < CONTROLLERS; ++device) {
                const HalPose& hal = poses[device];
                if (!isTracked(hal)) continue;
                const Quat halRotation = normalized({hal.f(11), hal.f(12), hal.f(13), hal.f(14)});
                const Vec turned = rotate(BASE.rotation, {hal.f(18), hal.f(19), hal.f(20)});
                Quat rotation = normalized(mul(mul(BASE.rotation, halRotation), GRIP_PITCH));
                Vec position{turned.x + BASE.offset.x, turned.y + BASE.offset.y, turned.z + BASE.offset.z};
                const float linear =
                    std::sqrt(hal.f(24) * hal.f(24) + hal.f(25) * hal.f(25) + hal.f(26) * hal.f(26));
                const float angular =
                    std::sqrt(hal.f(30) * hal.f(30) + hal.f(31) * hal.f(31) + hal.f(32) * hal.f(32));
                Vec linearVelocity{hal.f(24), hal.f(25), hal.f(26)};
                Vec angularVelocity{hal.f(30), hal.f(31), hal.f(32)};
                filterPose(FILTERS[device], now, linear, angular, &position, &rotation, &linearVelocity,
                    &angularVelocity);
            }
        }
        next += period;
        const int64_t after = monotonicNs();
        if (next <= after) {
            // Behind: no burst of reads to catch up.
            next = after;
            continue;
        }
        timespec until{static_cast<time_t>(next / 1000000000LL), static_cast<long>(next % 1000000000LL)};
        clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &until, nullptr);
    }
}

void replacePose(int device, int64_t readAt, const HalPose& hal, XrSpaceLocation* location) {
    constexpr XrSpaceLocationFlags tracked =
        XR_SPACE_LOCATION_ORIENTATION_VALID_BIT | XR_SPACE_LOCATION_POSITION_VALID_BIT |
        XR_SPACE_LOCATION_ORIENTATION_TRACKED_BIT | XR_SPACE_LOCATION_POSITION_TRACKED_BIT;
    const bool halTracked = isTracked(hal);
    const bool runtimeTracked = (location->locationFlags & tracked) == tracked;
    std::lock_guard<std::mutex> lock(BASE_MUTEX);
    if (halTracked) TRACKED_AT[device] = readAt;
    // While this layer reads the HAL the system's own requests are the earlier ones, so the
    // runtime's pose is the degraded one: once the base space is known, the HAL's pose is
    // reported whether or not the runtime calls its own pose tracked.
    const bool bridged = !halTracked && hasPose(hal) && TRACKED_AT[device] != 0 &&
        readAt - TRACKED_AT[device] < BRIDGE_NS;
    if (!halTracked) ++STATS_HAL_LOST;
    if (!runtimeTracked) ++STATS_RUNTIME_LOST;
    if (!(halTracked || bridged) || (!runtimeTracked && !BASE.known)) {
        ++STATS_PASSED;
        return;
    }
    if (bridged) ++STATS_BRIDGED;
    const Quat halRotation = normalized({hal.f(11), hal.f(12), hal.f(13), hal.f(14)});
    const Vec halPosition{hal.f(18), hal.f(19), hal.f(20)};
    const float linear = std::sqrt(hal.f(24) * hal.f(24) + hal.f(25) * hal.f(25) + hal.f(26) * hal.f(26));
    const float angular = std::sqrt(hal.f(30) * hal.f(30) + hal.f(31) * hal.f(31) + hal.f(32) * hal.f(32));
    XrPosef& pose = location->pose;
    refreshTuning(readAt);
    if (halTracked && runtimeTracked &&
        (BASE.known ? linear < STILL_LINEAR && angular < STILL_ANGULAR
                    : linear < FIND_LINEAR && angular < FIND_ANGULAR)) {
        ++STATS_STILL;
        const Quat runtime{pose.orientation.x, pose.orientation.y, pose.orientation.z, pose.orientation.w};
        const Quat rotation = normalized(mul(mul(runtime, conj(GRIP_PITCH)), conj(halRotation)));
        const Vec turned = rotate(rotation, halPosition);
        const Vec offset{pose.position.x - turned.x, pose.position.y - turned.y, pose.position.z - turned.z};
        if (!BASE.known) {
            BASE.known = true;
            BASE.samples = 1;
            BASE.rotation = rotation;
            BASE.offset = offset;
            GXR_LOG("base space found, offset %.3f %.3f %.3f", offset.x, offset.y, offset.z);
        } else {
            const double dx = offset.x - BASE.offset.x, dy = offset.y - BASE.offset.y, dz = offset.z - BASE.offset.z;
            const bool far = angle(rotation, BASE.rotation) > JUMP_ANGLE ||
                std::sqrt(dx * dx + dy * dy + dz * dz) > JUMP_DISTANCE;
            if (far && ++BASE.jumpSamples >= JUMP_SAMPLES) {
                BASE.rotation = rotation;
                BASE.offset = offset;
                BASE.jumpSamples = 0;
                BASE.samples = 1;
                for (Filter& filter : FILTERS) filter = Filter{};
                ++STATS_JUMPS;
                GXR_LOG("base space moved, offset %.3f %.3f %.3f", offset.x, offset.y, offset.z);
            } else if (!far) {
                BASE.jumpSamples = 0;
                // A running average over the first samples, then a slow follow.
                ++BASE.samples;
                const double blend = BLEND > 1.0 / BASE.samples ? BLEND : 1.0 / BASE.samples;
                Quat next = rotation;
                // Blend along the shorter arc.
                if (next.x * BASE.rotation.x + next.y * BASE.rotation.y + next.z * BASE.rotation.z +
                    next.w * BASE.rotation.w < 0) {
                    next = {-next.x, -next.y, -next.z, -next.w};
                }
                BASE.rotation = normalized({
                    BASE.rotation.x + (next.x - BASE.rotation.x) * blend,
                    BASE.rotation.y + (next.y - BASE.rotation.y) * blend,
                    BASE.rotation.z + (next.z - BASE.rotation.z) * blend,
                    BASE.rotation.w + (next.w - BASE.rotation.w) * blend,
                });
                BASE.offset = {BASE.offset.x + dx * blend, BASE.offset.y + dy * blend, BASE.offset.z + dz * blend};
            }
        }
    }
    if (!BASE.known) {
        ++STATS_PASSED;
        return;
    }
    Quat rotation = normalized(mul(mul(BASE.rotation, halRotation), GRIP_PITCH));
    const Vec turned = rotate(BASE.rotation, halPosition);
    Vec position{turned.x + BASE.offset.x, turned.y + BASE.offset.y, turned.z + BASE.offset.z};
    Vec linearVelocity{hal.f(24), hal.f(25), hal.f(26)};
    Vec angularVelocity{hal.f(30), hal.f(31), hal.f(32)};
    filterPose(FILTERS[device], readAt, linear, angular, &position, &rotation, &linearVelocity,
        &angularVelocity);
    const double sx = position.x - pose.position.x, sy = position.y - pose.position.y, sz = position.z - pose.position.z;
    STATS_SHIFT += std::sqrt(sx * sx + sy * sy + sz * sz);
    ++STATS_REPLACED;
    REPLACED_AT.store(readAt);
    location->locationFlags |= tracked;
    pose.orientation = {static_cast<float>(rotation.x), static_cast<float>(rotation.y),
        static_cast<float>(rotation.z), static_cast<float>(rotation.w)};
    pose.position = {static_cast<float>(position.x), static_cast<float>(position.y), static_cast<float>(position.z)};

    // Words: 21 and 27 say the linear and the angular velocity are present.
    XrSpaceVelocity* velocity = VELOCITIES.load() ? velocityOf(location) : nullptr;
    if (velocity && hal.words[21] != 0 && hal.words[27] != 0) {
        const Vec world = rotate(BASE.rotation, linearVelocity);
        const Vec local = rotate(conj(GRIP_PITCH), angularVelocity);
        velocity->linearVelocity =
            {static_cast<float>(world.x), static_cast<float>(world.y), static_cast<float>(world.z)};
        velocity->angularVelocity =
            {static_cast<float>(local.x), static_cast<float>(local.y), static_cast<float>(local.z)};
        velocity->velocityFlags = XR_SPACE_VELOCITY_LINEAR_VALID_BIT | XR_SPACE_VELOCITY_ANGULAR_VALID_BIT;
        VELOCITY_AT.store(readAt);
    }

    const int64_t now = monotonicNs();
    if (STATS_AT == 0) STATS_AT = now;
    if (now - STATS_AT >= STATS_NS) {
        const int polls = STATS_POLLS.exchange(0);
        GXR_LOG("stats: replaced=%d passed=%d still=%d base moves=%d mean shift=%.1fmm reads=%.0f/s "
            "read=%.2f/%.2fms late=%.2fms lost: hal=%d runtime=%d bridged=%d",
            STATS_REPLACED, STATS_PASSED, STATS_STILL, STATS_JUMPS,
            STATS_REPLACED ? STATS_SHIFT * 1000.0 / STATS_REPLACED : 0.0,
            polls * 1e9 / (now - STATS_AT),
            polls ? STATS_READ_NS.exchange(0) / 1e6 / polls : 0.0, STATS_READ_MAX_NS.exchange(0) / 1e6,
            STATS_LATE_MAX_NS.exchange(0) / 1e6, STATS_HAL_LOST, STATS_RUNTIME_LOST, STATS_BRIDGED);
        STATS_HAL_LOST = STATS_RUNTIME_LOST = STATS_BRIDGED = 0;
        STATS_AT = now;
        STATS_REPLACED = STATS_PASSED = STATS_STILL = STATS_JUMPS = 0;
        STATS_SHIFT = 0;
    }
}

XrResult XRAPI_PTR layerLocateSpace(
    XrSpace space,
    XrSpace baseSpace,
    XrTime time,
    XrSpaceLocation* location
) {
    const XrResult result = NEXT_LOCATE_SPACE(space, baseSpace, time, location);
    if (XR_FAILED(result) || !ENABLED.load() || !location) return result;
    int device = -1;
    {
        std::lock_guard<std::mutex> lock(MUTEX);
        const auto found = STREAM_SPACES.find(space);
        if (found == STREAM_SPACES.end()) return result;
        device = found->second;
    }
    if (POLL_HZ.load() > 0) {
        LOCATED_AT.store(monotonicNs());
        if (!POLL_STARTED.exchange(true)) std::thread(pollLoop).detach();
    }
    HalPose hal;
    int64_t readAt = 0;
    if (halPose(device, &hal, &readAt)) replacePose(device, readAt, hal, location);
    return result;
}

template <typename Function>
void loadNext(XrInstance instance, const char* name, Function& function) {
    PFN_xrVoidFunction loaded = nullptr;
    function = nullptr;
    if (XR_SUCCEEDED(NEXT_GET_INSTANCE_PROC_ADDR(instance, name, &loaded))) {
        function = reinterpret_cast<Function>(loaded);
    }
}

XrResult XRAPI_PTR layerGetInstanceProcAddr(
    XrInstance instance,
    const char* name,
    PFN_xrVoidFunction* function
) {
    if (!name || !function) return XR_ERROR_VALIDATION_FAILURE;
    const auto is = [name](const char* other) { return std::strcmp(name, other) == 0; };
    const auto set = [function](auto pointer) {
        *function = reinterpret_cast<PFN_xrVoidFunction>(pointer);
        return XR_SUCCESS;
    };
    if (is("xrGetInstanceProcAddr")) return set(layerGetInstanceProcAddr);
    if (is("xrCreateAction") && NEXT_CREATE_ACTION) return set(layerCreateAction);
    if (is("xrDestroyAction") && NEXT_DESTROY_ACTION) return set(layerDestroyAction);
    if (is("xrCreateActionSpace") && NEXT_CREATE_ACTION_SPACE) return set(layerCreateActionSpace);
    if (is("xrDestroySpace") && NEXT_DESTROY_SPACE) return set(layerDestroySpace);
    if (is("xrDestroySession") && NEXT_DESTROY_SESSION) return set(layerDestroySession);
    if (is("xrLocateSpace") && NEXT_LOCATE_SPACE) return set(layerLocateSpace);
    if (!NEXT_GET_INSTANCE_PROC_ADDR) {
        *function = nullptr;
        return XR_ERROR_FUNCTION_UNSUPPORTED;
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
    {
        std::lock_guard<std::mutex> lock(MUTEX);
        STREAM_ACTIONS.clear();
        STREAM_SPACES.clear();
    }
    loadNext(*instance, "xrCreateAction", NEXT_CREATE_ACTION);
    loadNext(*instance, "xrDestroyAction", NEXT_DESTROY_ACTION);
    loadNext(*instance, "xrCreateActionSpace", NEXT_CREATE_ACTION_SPACE);
    loadNext(*instance, "xrDestroySpace", NEXT_DESTROY_SPACE);
    loadNext(*instance, "xrDestroySession", NEXT_DESTROY_SESSION);
    loadNext(*instance, "xrLocateSpace", NEXT_LOCATE_SPACE);
    PFN_xrStringToPath stringToPath = nullptr;
    loadNext(*instance, "xrStringToPath", stringToPath);
    if (stringToPath) {
        stringToPath(*instance, "/user/hand/left", &LEFT_HAND);
        stringToPath(*instance, "/user/hand/right", &RIGHT_HAND);
    }
    readConfig();
    return XR_SUCCESS;
}

}  // namespace

// For the other controller layers: 1 while this layer supplies the controller pose, so that a
// pose filter there does not run a second time over an already filtered pose.
extern "C" __attribute__((visibility("default"))) int gxr_controller_hal_pose_active() {
    const int64_t replacedAt = REPLACED_AT.load();
    return replacedAt != 0 && monotonicNs() - replacedAt < ACTIVE_NS ? 1 : 0;
}

// For the controller velocity frame layer: 1 while this layer supplies the velocities, already in
// the frames VRLink's receiver reads them in, so that they are not rotated a second time.
extern "C" __attribute__((visibility("default"))) int gxr_controller_hal_velocity_active() {
    const int64_t suppliedAt = VELOCITY_AT.load();
    return suppliedAt != 0 && monotonicNs() - suppliedAt < ACTIVE_NS ? 1 : 0;
}

extern "C" JNIEXPORT void JNICALL Java_gxr_pose_PoseBridge_nativeSetBinder(
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
    GXR_LOG("controller HAL poses %s", service ? "available" : "not available");
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
