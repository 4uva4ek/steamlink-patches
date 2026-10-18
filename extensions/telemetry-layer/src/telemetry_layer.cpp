// Independent decoder telemetry; no codec selection, rewriting or retry logic.
// Actual libmediandk entry points are intercepted by pinned ShadowHook UNIQUE
// proxies, so calls from other layers reach the same observation boundary.
#include <android/log.h>
#include <link.h>
#include <dlfcn.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <time.h>
#include <unistd.h>
#include <media/NdkMediaCodec.h>
#include <algorithm>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <condition_variable>
#include <memory>
#include <map>
#include <mutex>
#include <string>
#include <thread>
#include <utility>
#include <vector>
#include <limits>
#include <openxr/openxr.h>
#include <openxr/openxr_loader_negotiation.h>
#include <shadowhook.h>
#include "sha256.h"
#include "decoder_probe.h"
#include "telemetry_profiles.h" // Generated only from audited, exact stock bytes.

#define EXPORT extern "C" __attribute__((visibility("default")))
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "GXRTelemetry", __VA_ARGS__)

namespace {
constexpr const char* kLayerName = "XR_APILAYER_local_GalaxyXR_telemetry";
constexpr const char* kBuild = "telemetry-v1-ndk-api-input-20261018";
constexpr const char* kSceneSoname = "libvrlink_scene.so";
std::atomic<bool> g_hooksReady{false};
std::atomic<int> g_installState{0}; // 0 wait for scene, 1 installed, 2 refused/rolled back
std::mutex g_installMutex;

constexpr const char* kProbeRequest = "/sdcard/Android/data/com.valvesoftware.steamlinkvr/files/colourlab-probe.request";
constexpr const char* kVerifyRequest = "/sdcard/Android/data/com.valvesoftware.steamlinkvr/files/colourlab-verify.request";
constexpr const char* kProbeCapture = "/sdcard/Android/data/com.valvesoftware.steamlinkvr/files/colourlab-capture.bin";

uint64_t ProbeNow() {
    timespec ts{};
    if (clock_gettime(CLOCK_MONOTONIC, &ts)) return 0;
    return static_cast<uint64_t>(ts.tv_sec) * 1000000000ull + ts.tv_nsec;
}

// Bounded, no-follow reads. Request file I/O is confined to the worker (<=1 Hz).
std::string ReadProbeRequest(const char* path) {
    int fd = open(path, O_RDONLY | O_CLOEXEC | O_NOFOLLOW | O_NONBLOCK);
    if (fd < 0) return {};
    struct stat attributes{};
    time_t now = time(nullptr);
    if (fstat(fd, &attributes) || !S_ISREG(attributes.st_mode) ||
        now < attributes.st_mtime || now - attributes.st_mtime > 30) {
        close(fd);
        return {};
    }
    char text[129];
    ssize_t count = read(fd, text, sizeof(text));
    close(fd);
    return count > 0 && count < static_cast<ssize_t>(sizeof(text)) ?
        std::string(text, static_cast<size_t>(count)) : std::string{};
}

struct ProbeCapture {
    std::string token, nonce;
    uint64_t stream = 0, time = 0;
    bool sequenceHeader = false;
    std::vector<uint8_t> bytes;
};

class DecoderProbe {
public:
    DecoderProbe() {
        try { worker = std::thread([this] { Run(); }); }
        catch (...) { /* Optional diagnostics must never prevent app startup. */ }
    }
    ~DecoderProbe() {
        stopping.store(true);
        wake.notify_all();
        if (worker.joinable()) worker.join();
    }
    bool Enabled() const { return active.load(std::memory_order_relaxed); }

    void VideoCreated(void* codec, const char* mime) {
        std::lock_guard<std::mutex> lock(mutex);
        if (!state.AdmitVideo(reinterpret_cast<uintptr_t>(codec), mime)) {
            // A newly created audio handle may reuse a retired pointer. Never
            // carry video admission across a different factory result.
            if (state.ForgetVideo(reinterpret_cast<uintptr_t>(codec))) ResetLocked();
            return;
        }
        ResetLocked();
    }

    void Reset(void* codec, bool deleted = false) {
        std::lock_guard<std::mutex> lock(mutex);
        if (!state.IsVideo(reinterpret_cast<uintptr_t>(codec))) return;
        if (deleted) state.ForgetVideo(reinterpret_cast<uintptr_t>(codec));
        ResetLocked();
    }

    void ResetLocked() {
        state.Reset();
        buffers.clear();
        captureReady.reset();
        captureBusy = false;
    }

    uint64_t Generation(void* codec) {
        if (!Enabled()) return 0;
        std::lock_guard<std::mutex> lock(mutex);
        return state.IsVideo(reinterpret_cast<uintptr_t>(codec)) ? state.counters.stream : 0;
    }

    void Input(void* codec, size_t index, uint8_t* data, size_t capacity, uint64_t generation) {
        if (!Enabled()) return;
        std::lock_guard<std::mutex> lock(mutex);
        buffers.erase(std::make_pair(codec, index));
        if (!data) return;
        if (!generation || generation != state.counters.stream ||
            !state.IsVideo(reinterpret_cast<uintptr_t>(codec))) return;
        if (buffers.size() >= 64) buffers.clear();
        try { buffers[std::make_pair(codec, index)] = Buffer{data, capacity}; }
        catch (...) { buffers.clear(); }
    }

    std::shared_ptr<ProbeCapture> BeforeQueue(void* codec, size_t index, int64_t offset,
                                             size_t size, uint32_t flags) {
        if (!Enabled()) return {};
        std::lock_guard<std::mutex> lock(mutex);
        if (!state.IsVideo(reinterpret_cast<uintptr_t>(codec))) return {};
        auto found = buffers.find(std::make_pair(codec, index));
        if (found == buffers.end()) return {};
        Buffer buffer = found->second;
        buffers.erase(found); // never retain a queued decoder-owned pointer
        if (captureBusy || pendingNonce.empty() || !size || (flags & (2u | 4u | 8u)) ||
            !gxr::ProbeBufferRange(buffer.capacity, offset, size)) return {};
        const uint8_t* bytes = buffer.data + static_cast<size_t>(offset);
        bool header = gxr::ProbeHasSequenceHeader(bytes, size);
        // Wait for independently parsable headers without asking the encoder
        // for a keyframe. A bounded fallback remains honestly inconclusive.
        if (!header && ProbeNow() - verifyStarted < 15000000000ull) return {};
        try {
            auto capture = std::make_shared<ProbeCapture>();
            capture->token = state.token;
            capture->nonce = pendingNonce;
            capture->stream = state.counters.stream;
            capture->time = ProbeNow();
            capture->sequenceHeader = header;
            capture->bytes.assign(bytes, bytes + size); // one bounded opt-in AU copy
            captureBusy = true;
            return capture;
        } catch (...) { return {}; } // observation must never fail a decoder call
    }

    std::pair<uint64_t, uint64_t> BeginQueue(void* codec) {
        if (!Enabled()) return {};
        std::lock_guard<std::mutex> lock(mutex);
        if (!state.IsVideo(reinterpret_cast<uintptr_t>(codec))) return {};
        return {state.counters.stream, ProbeNow()};
    }

    void Queue(void* codec, uint64_t pts, size_t size, uint32_t flags, int32_t status,
               std::pair<uint64_t, uint64_t> submission, std::shared_ptr<ProbeCapture> capture) {
        if (!Enabled()) return;
        std::lock_guard<std::mutex> lock(mutex);
        if (!submission.second || submission.first != state.counters.stream) {
            if (capture) captureBusy = false;
            return;
        }
        try { state.Queue(reinterpret_cast<uintptr_t>(codec), pts, size, flags, status, submission.second); }
        catch (...) { state.Reset(); buffers.clear(); }
        if (!capture) return;
        if (!status && capture->token == state.token && capture->stream == state.counters.stream &&
            capture->nonce == pendingNonce) {
            captureReady = std::move(capture);
            wake.notify_all();
        } else captureBusy = false;
    }

    void Output(void* codec, const AMediaCodecBufferInfo* info, uint64_t generation) {
        if (!Enabled() || !info) return;
        std::lock_guard<std::mutex> lock(mutex);
        if (!generation || generation != state.counters.stream) return;
        state.Output(reinterpret_cast<uintptr_t>(codec), static_cast<uint64_t>(info->presentationTimeUs),
                     info->flags, ProbeNow());
    }

private:
    struct Buffer { uint8_t* data; size_t capacity; };
    std::atomic<bool> active{false}, stopping{false};
    std::mutex mutex;
    std::condition_variable wake;
    gxr::DecoderProbeState state;
    std::map<std::pair<void*, size_t>, Buffer> buffers;
    std::string pendingNonce, lastNonce;
    uint64_t verifyStarted = 0;
    bool captureBusy = false;
    std::shared_ptr<ProbeCapture> captureReady;
    std::thread worker;

    void Poll() {
        if (!g_hooksReady.load(std::memory_order_acquire)) {
            active.store(false, std::memory_order_relaxed);
            return;
        }
        std::string token, nonce, verifyToken;
        if (!gxr::ProbeRequest(ReadProbeRequest(kProbeRequest), token)) token.clear();
        bool verify = !token.empty() && gxr::ProbeRequest(ReadProbeRequest(kVerifyRequest), verifyToken, &nonce) &&
                      verifyToken == token;
        std::lock_guard<std::mutex> lock(mutex);
        if (state.Session(token)) {
            buffers.clear();
            pendingNonce.clear();
            lastNonce.clear();
            captureReady.reset();
            captureBusy = false;
        }
        active.store(!token.empty(), std::memory_order_relaxed);
        if (verify && nonce != lastNonce) {
            pendingNonce = nonce;
            lastNonce = nonce;
            verifyStarted = ProbeNow();
        }
        if (!verify) {
            pendingNonce.clear();
            captureReady.reset();
            captureBusy = false;
        }
        if (token.empty()) return;
        const auto& c = state.counters;
        __android_log_print(ANDROID_LOG_INFO, "GXRDecoderProbe",
            "event=stats token=%s stream=%llu monotonic_ns=%llu discontinuities=%llu "
            "accepted_input_bytes=%llu accepted_input_frames=%llu output_frames=%llu "
            "queue_to_output_count=%llu queue_to_output_sum_ns=%llu queue_to_output_max_ns=%llu "
            "queue_successes=%llu queue_failures=%llu ambiguous_pts=%llu unmatched_outputs=%llu pending_evictions=%llu scope=decoder_input",
            token.c_str(), static_cast<unsigned long long>(c.stream), static_cast<unsigned long long>(ProbeNow()),
            static_cast<unsigned long long>(c.discontinuities), static_cast<unsigned long long>(c.accepted_input_bytes),
            static_cast<unsigned long long>(c.accepted_input_frames), static_cast<unsigned long long>(c.output_frames),
            static_cast<unsigned long long>(c.queue_to_output_count), static_cast<unsigned long long>(c.queue_to_output_sum_ns),
            static_cast<unsigned long long>(c.queue_to_output_max_ns), static_cast<unsigned long long>(c.queue_successes),
            static_cast<unsigned long long>(c.queue_failures), static_cast<unsigned long long>(c.ambiguous_pts),
            static_cast<unsigned long long>(c.unmatched_outputs), static_cast<unsigned long long>(c.pending_evictions));
    }

    void WriteCapture(const std::shared_ptr<ProbeCapture>& capture) {
        // Hash and file writes run off MediaThread, after the original queue accepted.
        uint8_t digest[32]; char hash[65];
        gxr::Sha256::Digest(capture->bytes.data(), capture->bytes.size(), digest);
        gxr::Sha256::Hex(digest, hash);
        std::string temporary = std::string(kProbeCapture) + ".tmp";
        int fd = open(temporary.c_str(), O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC | O_NOFOLLOW | O_NONBLOCK, 0600);
        bool ok = fd >= 0;
        size_t done = 0;
        while (ok && done < capture->bytes.size()) {
            ssize_t count = write(fd, capture->bytes.data() + done, capture->bytes.size() - done);
            if (count <= 0) ok = false;
            else done += static_cast<size_t>(count);
        }
        if (fd >= 0 && close(fd)) ok = false;
        std::lock_guard<std::mutex> lock(mutex);
        bool current = capture->token == state.token && capture->stream == state.counters.stream &&
                       capture->nonce == pendingNonce && active.load();
        if (ok && current) ok = rename(temporary.c_str(), kProbeCapture) == 0;
        else ok = false;
        if (ok) {
            __android_log_print(ANDROID_LOG_INFO, "GXRDecoderProbe",
                "event=capture token=%s nonce=%s stream=%llu monotonic_ns=%llu size=%zu sha256=%s path=%s "
                "scope=decoder_input stage=decoder_api_input accepted=1 sequence_header=%d",
                capture->token.c_str(), capture->nonce.c_str(), static_cast<unsigned long long>(capture->stream),
                static_cast<unsigned long long>(capture->time), capture->bytes.size(), hash, kProbeCapture,
                static_cast<int>(capture->sequenceHeader));
            pendingNonce.clear();
        } else {
            unlink(temporary.c_str());
            if (current) __android_log_print(ANDROID_LOG_INFO, "GXRDecoderProbe",
                "event=capture_error token=%s nonce=%s reason=write_failed", capture->token.c_str(), capture->nonce.c_str());
            if (current) pendingNonce.clear();
        }
        captureBusy = false;
    }

    void Run() {
        uint64_t lastPoll = 0;
        while (!stopping.load()) {
            uint64_t now = ProbeNow();
            if (!lastPoll || now - lastPoll >= 1000000000ull) {
                lastPoll = now;
                try { Poll(); }
                catch (...) {
                    std::lock_guard<std::mutex> lock(mutex);
                    state.Reset();
                    buffers.clear();
                    active.store(false);
                }
            }
            std::shared_ptr<ProbeCapture> capture;
            {
                std::unique_lock<std::mutex> lock(mutex);
                if (!captureReady) wake.wait_for(lock, std::chrono::seconds(1));
                capture.swap(captureReady);
            }
            if (capture) {
                try { WriteCapture(capture); }
                catch (...) {
                    std::lock_guard<std::mutex> lock(mutex);
                    captureBusy = false;
                    pendingNonce.clear();
                }
            }
        }
    }
};

// ShadowHook proxies and the worker remain valid for process lifetime.
DecoderProbe& Probe() { static DecoderProbe* probe = new DecoderProbe; return *probe; }

template<class Action> void Observe(Action action) noexcept {
    try { action(); } catch (...) { /* Optional diagnostics cannot fail an API call. */ }
}

using FnCreate = decltype(&AMediaCodec_createDecoderByType);
using FnConfigure = decltype(&AMediaCodec_configure);
using FnGetInput = decltype(&AMediaCodec_getInputBuffer);
using FnQueue = decltype(&AMediaCodec_queueInputBuffer);
using FnOutput = decltype(&AMediaCodec_dequeueOutputBuffer);
using FnLifecycle = decltype(&AMediaCodec_flush);
FnCreate o_create = nullptr;
FnConfigure o_configure = nullptr;
FnGetInput o_getInput = nullptr;
FnQueue o_queue = nullptr;
FnOutput o_output = nullptr;
FnLifecycle o_flush = nullptr, o_stop = nullptr, o_delete = nullptr;

AMediaCodec* HookCreate(const char* mime) {
    AMediaCodec* codec = o_create(mime);
    // Admission is independent of the PC collection lease and occurs for the
    // actual handle, including replacement decoders created by another layer.
    Observe([&] { Probe().VideoCreated(codec, mime); });
    return codec;
}

media_status_t HookConfigure(AMediaCodec* codec, const AMediaFormat* format,
                            ANativeWindow* surface, AMediaCrypto* crypto, uint32_t flags) {
    Observe([&] { Probe().Reset(codec); });
    return o_configure(codec, format, surface, crypto, flags);
}

uint8_t* HookGetInput(AMediaCodec* codec, size_t index, size_t* capacity) {
    uint64_t generation = 0;
    if (g_hooksReady.load(std::memory_order_acquire)) Observe([&] { generation = Probe().Generation(codec); });
    uint8_t* bytes = o_getInput(codec, index, capacity);
    if (g_hooksReady.load(std::memory_order_acquire) && capacity)
        Observe([&] { Probe().Input(codec, index, bytes, *capacity, generation); });
    return bytes;
}

struct QueueObservation {
    std::pair<uint64_t, uint64_t> submission{};
    std::shared_ptr<ProbeCapture> capture;
};

media_status_t HookQueue(AMediaCodec* codec, size_t index, off_t offset, size_t size,
                        uint64_t pts, uint32_t flags) {
    if (!g_hooksReady.load(std::memory_order_acquire)) return o_queue(codec, index, offset, size, pts, flags);
    return gxr::ProbeObservedCall(
        [&] {
            QueueObservation observation;
            observation.submission = Probe().BeginQueue(codec);
            // At this actual NDK entry point every caller's codec helper has
            // already finished changing input. Copy before ownership transfers;
            // commit/export only after this actual queue reports success.
            observation.capture = Probe().BeforeQueue(codec, index, offset, size, flags);
            return observation;
        },
        [&] { return o_queue(codec, index, offset, size, pts, flags); },
        [&](media_status_t status, QueueObservation observation) {
            Probe().Queue(codec, pts, size, flags, status, observation.submission, std::move(observation.capture));
        });
}

ssize_t HookOutput(AMediaCodec* codec, AMediaCodecBufferInfo* info, int64_t timeoutUs) {
    // Avoid matching a result across a concurrent flush/configure/session reset.
    uint64_t generation = 0;
    if (g_hooksReady.load(std::memory_order_acquire)) Observe([&] { generation = Probe().Generation(codec); });
    ssize_t result = o_output(codec, info, timeoutUs);
    if (result >= 0 && g_hooksReady.load(std::memory_order_acquire))
        Observe([&] { Probe().Output(codec, info, generation); });
    return result;
}

media_status_t HookFlush(AMediaCodec* codec) {
    Observe([&] { Probe().Reset(codec); });
    return o_flush(codec);
}
media_status_t HookStop(AMediaCodec* codec) {
    Observe([&] { Probe().Reset(codec); });
    return o_stop(codec);
}
media_status_t HookDelete(AMediaCodec* codec) {
    Observe([&] { Probe().Reset(codec, true); });
    return o_delete(codec);
}

using gxr::telemetry_profiles::Profile;

bool CheckedAddress(uintptr_t base, uintptr_t offset, size_t length, uintptr_t* address) {
    const uintptr_t limit = std::numeric_limits<uintptr_t>::max();
    if (offset > limit - base || length > limit - (base + offset)) return false;
    *address = base + offset;
    return true;
}

bool AnchorInExecutableLoad(const dl_phdr_info& info, uintptr_t offset, size_t length) {
    for (size_t i = 0; i < info.dlpi_phnum; ++i) {
        const auto& p = info.dlpi_phdr[i];
        if (p.p_type != PT_LOAD || (p.p_flags & (PF_R | PF_X)) != (PF_R | PF_X)) continue;
        if (offset >= p.p_vaddr && offset - p.p_vaddr <= p.p_filesz &&
            length <= p.p_filesz - (offset - p.p_vaddr)) return true;
    }
    return false;
}

bool MatchesProfile(const dl_phdr_info& info, const Profile& profile) {
    size_t segment = 0;
    for (size_t i = 0; i < info.dlpi_phnum; ++i) {
        const auto& p = info.dlpi_phdr[i];
        if (p.p_type != PT_LOAD) continue;
        if (segment >= profile.segmentCount) return false;
        const auto& expected = profile.segments[segment++];
        if (p.p_vaddr != expected.virtualAddress || p.p_filesz != expected.fileSize ||
            p.p_memsz != expected.memorySize || p.p_flags != expected.flags) return false;
    }
    if (segment != profile.segmentCount || !profile.anchorCount) return false;
    for (size_t i = 0; i < profile.anchorCount; ++i) {
        const auto& anchor = profile.anchors[i];
        uintptr_t address = 0;
        if (!anchor.size || !anchor.sha256 || std::strlen(anchor.sha256) != 64 ||
            !AnchorInExecutableLoad(info, anchor.virtualAddress, anchor.size) ||
            !CheckedAddress(info.dlpi_addr, anchor.virtualAddress, anchor.size, &address)) return false;
        uint8_t digest[32]; char hex[65];
        gxr::Sha256::Digest(reinterpret_cast<const void*>(address), anchor.size, digest);
        gxr::Sha256::Hex(digest, hex);
        if (std::strcmp(hex, anchor.sha256)) return false;
    }
    return true;
}

struct SceneAdmission { bool found = false; const Profile* profile = nullptr; };
int FindScene(dl_phdr_info* info, size_t, void* opaque) {
    auto& admission = *static_cast<SceneAdmission*>(opaque);
    const char* path = info->dlpi_name;
    const char* base = path ? std::strrchr(path, '/') : nullptr;
    base = base ? base + 1 : path;
    if (!base || std::strcmp(base, kSceneSoname)) return 0;
    admission.found = true;
    for (size_t i = 0; i < gxr::telemetry_profiles::kProfileCount; ++i) {
        const auto& profile = gxr::telemetry_profiles::kProfiles[i];
        if (MatchesProfile(*info, profile)) { admission.profile = &profile; break; }
    }
    return 1;
}

bool IsMediaNdkAddress(void* address) {
    Dl_info info{};
    if (!address || !dladdr(address, &info) || !info.dli_fname) return false;
    const char* base = std::strrchr(info.dli_fname, '/');
    base = base ? base + 1 : info.dli_fname;
    return std::strcmp(base, "libmediandk.so") == 0;
}

// Keep this module, its dependency and proxy/trampoline storage resident even
// when the XR loader closes an instance. We never tear down live proxies.
void* g_selfPin = nullptr;
void* g_mediaNdk = nullptr;
void* g_stubs[8]{};

unsigned RollbackHooks() noexcept {
    unsigned failures = 0;
    for (size_t i = 8; i > 0; --i) {
        if (!g_stubs[i - 1]) continue;
        if (shadowhook_unhook(g_stubs[i - 1]) == 0) g_stubs[i - 1] = nullptr;
        else ++failures; // Retain self, originals and state for any live proxy.
    }
    return failures;
}

void TryInstall() noexcept {
    if (g_installState.load(std::memory_order_acquire)) return;
    std::unique_lock<std::mutex> lock(g_installMutex, std::try_to_lock);
    if (!lock.owns_lock() || g_installState.load()) return;
    try {
        SceneAdmission admission;
        dl_iterate_phdr(FindScene, &admission);
        if (!admission.found) return; // XR layer can load before the scene.
        if (!admission.profile) {
            g_installState.store(2);
            LOG("scene mapping/code anchors unknown; telemetry refused");
            return;
        }
        Dl_info self{};
        if (!dladdr(reinterpret_cast<void*>(TryInstall), &self) || !self.dli_fname ||
            !(g_selfPin = dlopen(self.dli_fname, RTLD_NOW | RTLD_LOCAL | RTLD_NODELETE))) {
            g_installState.store(2);
            LOG("cannot keep proxy code resident; telemetry refused");
            return;
        }
        const char* version = shadowhook_get_version();
        if (!version || std::strcmp(version, "2.0.1") ||
            shadowhook_init(SHADOWHOOK_MODE_UNIQUE, false) != 0 || shadowhook_get_disable()) {
            g_installState.store(2);
            LOG("pinned ShadowHook 2.0.1 unavailable; telemetry refused");
            return;
        }
        g_mediaNdk = dlopen("libmediandk.so", RTLD_NOW | RTLD_LOCAL | RTLD_NODELETE);
        struct Target { const char* name; void* proxy; void** original; };
        const Target targets[] = {
            {"AMediaCodec_createDecoderByType", reinterpret_cast<void*>(HookCreate), reinterpret_cast<void**>(&o_create)},
            {"AMediaCodec_configure", reinterpret_cast<void*>(HookConfigure), reinterpret_cast<void**>(&o_configure)},
            {"AMediaCodec_getInputBuffer", reinterpret_cast<void*>(HookGetInput), reinterpret_cast<void**>(&o_getInput)},
            {"AMediaCodec_queueInputBuffer", reinterpret_cast<void*>(HookQueue), reinterpret_cast<void**>(&o_queue)},
            {"AMediaCodec_dequeueOutputBuffer", reinterpret_cast<void*>(HookOutput), reinterpret_cast<void**>(&o_output)},
            {"AMediaCodec_flush", reinterpret_cast<void*>(HookFlush), reinterpret_cast<void**>(&o_flush)},
            {"AMediaCodec_stop", reinterpret_cast<void*>(HookStop), reinterpret_cast<void**>(&o_stop)},
            {"AMediaCodec_delete", reinterpret_cast<void*>(HookDelete), reinterpret_cast<void**>(&o_delete)},
        };
        void* addresses[8]{};
        bool valid = g_mediaNdk != nullptr;
        for (size_t i = 0; valid && i < 8; ++i) {
            addresses[i] = dlsym(g_mediaNdk, targets[i].name);
            valid = IsMediaNdkAddress(addresses[i]);
        }
        if (!valid) {
            g_installState.store(2);
            LOG("actual libmediandk symbols unavailable; telemetry refused");
            return;
        }
        Probe(); // process-lifetime state; factory admission also precedes leases
        size_t installed = 0;
        for (; installed < 8; ++installed) {
            g_stubs[installed] = shadowhook_hook_sym_addr_2(addresses[installed], targets[installed].proxy,
                targets[installed].original, SHADOWHOOK_HOOK_WITH_UNIQUE_MODE);
            if (!g_stubs[installed] || !*targets[installed].original) break;
        }
        if (installed != 8) {
            const int error = shadowhook_get_errno();
            // Observation stays inactive throughout installation/rollback. Any
            // in-flight proxy keeps valid process-lifetime original storage.
            const unsigned rollbackFailures = RollbackHooks();
            g_installState.store(2, std::memory_order_release);
            LOG("hook batch refused; observation disabled error=%d rollback_failures=%u", error, rollbackFailures);
            return;
        }
        g_hooksReady.store(true, std::memory_order_release);
        g_installState.store(1, std::memory_order_release);
        LOG("actual decoder API observation installed base=%s/%llu build=%s stage=decoder_api_input",
            admission.profile->versionName, static_cast<unsigned long long>(admission.profile->versionCode), kBuild);
    } catch (...) {
        g_hooksReady.store(false, std::memory_order_release);
        const unsigned rollbackFailures = RollbackHooks();
        g_installState.store(2, std::memory_order_release);
        LOG("telemetry initialization failed closed; rollback_failures=%u", rollbackFailures);
    }
}

struct Dispatch {
    PFN_xrGetInstanceProcAddr get{};
    PFN_xrCreateSession createSession{};
    PFN_xrWaitFrame waitFrame{};
    PFN_xrEndFrame endFrame{};
} g;

XrResult XRAPI_PTR WrapCreateSession(XrInstance instance, const XrSessionCreateInfo* info, XrSession* session) {
    TryInstall();
    return g.createSession(instance, info, session);
}
XrResult XRAPI_PTR WrapWaitFrame(XrSession session, const XrFrameWaitInfo* info, XrFrameState* state) {
    TryInstall();
    return g.waitFrame(session, info, state);
}
XrResult XRAPI_PTR WrapEndFrame(XrSession session, const XrFrameEndInfo* info) {
    TryInstall();
    return g.endFrame(session, info);
}

XrResult XRAPI_PTR LayerGetInstanceProcAddr(XrInstance instance, const char* name, PFN_xrVoidFunction* function) {
    if (!name || !function) return XR_ERROR_VALIDATION_FAILURE;
    *function = nullptr;
    // Loader queries xrInitializeLoaderKHR with XR_NULL_HANDLE before a chain
    // exists. Never advertise a null implementation as XR_SUCCESS.
    if (!g.get) return XR_ERROR_FUNCTION_UNSUPPORTED;
    TryInstall();
    if (!std::strcmp(name, "xrCreateSession") && g.createSession)
        *function = reinterpret_cast<PFN_xrVoidFunction>(WrapCreateSession);
    else if (!std::strcmp(name, "xrWaitFrame") && g.waitFrame)
        *function = reinterpret_cast<PFN_xrVoidFunction>(WrapWaitFrame);
    else if (!std::strcmp(name, "xrEndFrame") && g.endFrame)
        *function = reinterpret_cast<PFN_xrVoidFunction>(WrapEndFrame);
    else return g.get(instance, name, function);
    return XR_SUCCESS;
}

XrResult XRAPI_PTR LayerCreateInstance(const XrInstanceCreateInfo* info,
                                     const XrApiLayerCreateInfo* layer, XrInstance* instance) {
    if (!info || !layer || !layer->nextInfo || !instance ||
        !layer->nextInfo->nextCreateApiLayerInstance || !layer->nextInfo->nextGetInstanceProcAddr)
        return XR_ERROR_INITIALIZATION_FAILED;
    XrApiLayerCreateInfo next = *layer;
    next.nextInfo = layer->nextInfo->next;
    XrResult result = layer->nextInfo->nextCreateApiLayerInstance(info, &next, instance);
    if (XR_FAILED(result)) return result;
    g.get = layer->nextInfo->nextGetInstanceProcAddr;
    g.get(*instance, "xrCreateSession", reinterpret_cast<PFN_xrVoidFunction*>(&g.createSession));
    g.get(*instance, "xrWaitFrame", reinterpret_cast<PFN_xrVoidFunction*>(&g.waitFrame));
    g.get(*instance, "xrEndFrame", reinterpret_cast<PFN_xrVoidFunction*>(&g.endFrame));
    TryInstall();
    return result;
}

__attribute__((constructor)) void OnLoad() { TryInstall(); }
} // namespace

EXPORT XrResult XRAPI_PTR xrNegotiateLoaderApiLayerInterface(const XrNegotiateLoaderInfo* loader,
                                                            const char* name,
                                                            XrNegotiateApiLayerRequest* request) {
    if (!loader || !name || !request || std::strcmp(name, kLayerName) ||
        loader->structType != XR_LOADER_INTERFACE_STRUCT_LOADER_INFO ||
        loader->structVersion != XR_LOADER_INFO_STRUCT_VERSION || loader->structSize != sizeof(*loader) ||
        request->structType != XR_LOADER_INTERFACE_STRUCT_API_LAYER_REQUEST ||
        request->structVersion != XR_API_LAYER_INFO_STRUCT_VERSION || request->structSize != sizeof(*request) ||
        loader->minInterfaceVersion > XR_CURRENT_LOADER_API_LAYER_VERSION ||
        loader->maxInterfaceVersion < XR_CURRENT_LOADER_API_LAYER_VERSION ||
        loader->maxApiVersion < XR_MAKE_VERSION(1, 0, 0) || loader->minApiVersion > XR_MAKE_VERSION(1, 1, 0))
        return XR_ERROR_INITIALIZATION_FAILED;
    request->layerInterfaceVersion = XR_CURRENT_LOADER_API_LAYER_VERSION;
    request->layerApiVersion = std::min<XrVersion>(loader->maxApiVersion, XR_MAKE_VERSION(1, 1, 0));
    request->getInstanceProcAddr = LayerGetInstanceProcAddr;
    request->createApiLayerInstance = LayerCreateInstance;
    return XR_SUCCESS;
}

