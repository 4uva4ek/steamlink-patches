// Codecs Support — OpenXR implicit layer for the Steam Link Galaxy XR client.
//
// The headset client hardcodes a 'video/hevc' MediaCodec decoder: it never
// asks the host which codec is on the wire (verified: no codec field exists
// anywhere in the stream-setup path). This layer makes the client accept
// whatever the host's NVENC tap selects:
//
//   1. Loads as an implicit XR layer (assets/openxr/1/api_layers/implicit.d).
//   2. Finds libvrlink_scene.so via dl_iterate_phdr (from XR dispatch and
//      from the layer constructor) and verifies its identity against pinned
//      per-base SHA-256 code anchors — fail closed, never patch a stranger.
//   3. Patches the scene library's PLT GOT slots for the 15 imported
//      libmediandk functions (BIND_NOW + full RELRO, so mprotect first).
//   4. While armed, records the MediaFormat recipe the client builds for the
//      decoder and captures the input buffer handed out by getInputBuffer.
//   5. On the first compressed access unit: sniff it. HEVC / unknown keeps
//      the stock path. H264 / AV1 => delete the HEVC decoder, create and
//      configure the sniffed codec from the recorded recipe, start it,
//      re-queue the same access unit, retire the old decoder, and translate
//      every later handle. One-shot, fail-safe.
//
// Exact builds only (pinned anchors): 2.0.20/5001712 and 2.0.23/5002363.
// Build: see Build-Native.ps1. Nothing here runs unless the layer is loaded.

#include <android/log.h>
#include <link.h>
#include <sys/mman.h>
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

#include <openxr/openxr.h>
#include <openxr/openxr_loader_negotiation.h>

#include "codec_sniff.h"
#include "format_recipe.h"
#include "av1_geometry.h"
#include "sha256.h"
#include "decoder_probe.h"

#define EXPORT extern "C" __attribute__((visibility("default")))
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "GXRCodecsSupport", __VA_ARGS__)

namespace {

using gxr::FindAv1SeqAndFrame;
using gxr::FormatEntry;
using gxr::PatchAv1SeqHeaders;
using gxr::Sniffed;

constexpr const char* kLayerName = "XR_APILAYER_local_GalaxyXR_codecs_support";
constexpr const char* kBuild = "codecs-support-v7-probe-20261018";
constexpr const char* kDisableEnv = "GXR_DISABLE_CODECS_SUPPORT";
constexpr const char* kSceneSoname = "libvrlink_scene.so";
constexpr size_t kMaxSniffedAu = 4u << 20;

// Separate from the retired buffering experiment: observes original calls,
// never delays/retries/reorders decoder submission or changes buffer ownership.
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
        if (!state.AdmitVideo(reinterpret_cast<uintptr_t>(codec), mime)) return;
        ResetLocked();
    }

    void VideoRemapped(void* oldCodec, void* liveCodec) {
        std::lock_guard<std::mutex> lock(mutex);
        if (!state.ForgetVideo(reinterpret_cast<uintptr_t>(oldCodec))) return;
        if (liveCodec) state.AdmitVideo(reinterpret_cast<uintptr_t>(liveCodec), "video/remapped");
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

    void Input(void* codec, size_t index, uint8_t* data, size_t capacity) {
        if (!Enabled() || !data) return;
        std::lock_guard<std::mutex> lock(mutex);
        if (!state.IsVideo(reinterpret_cast<uintptr_t>(codec))) return;
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

    void Output(void* codec, const AMediaCodecBufferInfo* info) {
        if (!Enabled() || !info) return;
        std::lock_guard<std::mutex> lock(mutex);
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
                "scope=decoder_input stage=before_codec_rewrite accepted=1 sequence_header=%d",
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

DecoderProbe& Probe() { static DecoderProbe probe; return probe; }

// ---------------------------------------------------------------------------
// Pinned scene-library layout (extracted from the exact signed bases).
// Slot order fixed by SlotIx; runtime address = dlpi_addr + offset.
// Anchor = 0x80-byte code region ending 0x40 bytes before the decoder
// factory call site; its SHA-256 identifies the build.
// ---------------------------------------------------------------------------
enum SlotIx {
    SlotCreateDecoder = 0, SlotFormatNew, SlotSetString, SlotSetInt32, SlotConfigure,
    SlotStart, SlotFormatDelete, SlotDequeueOutput, SlotReleaseOutput, SlotDequeueInput,
    SlotGetInput, SlotQueueInput, SlotFlush, SlotStop, SlotDelete, kSlotCount,
};

struct GotProfile {
    const char* build;
    uintptr_t anchorOffset;
    const char* anchorSha256;
    uintptr_t slots[kSlotCount];
};

const GotProfile kProfile5001712 = {
    "2.0.20/5001712", 0xfd304,
    "6e5dcb4f5094763e948388ca35c6132af242159fc3c22e812f0fe2f4ab4f2649",
    {0x21b5d0, 0x21b5d8, 0x21b5e0, 0x21b5e8, 0x21b608, 0x21b610, 0x21b618,
     0x21b640, 0x21b648, 0x21b660, 0x21b668, 0x21b670, 0x21b6c0, 0x21b6d0, 0x21b6d8},
};
const GotProfile kProfile5002363 = {
    "2.0.23/5002363", 0xfd6fc,
    "f5c43c6aacb69e61c3f929f361612e1b91bb509803d2c14824b9601b3dd8c795",
    {0x22f6b0, 0x22f6b8, 0x22f6c0, 0x22f6c8, 0x22f6e8, 0x22f6f0, 0x22f6f8,
     0x22f720, 0x22f728, 0x22f740, 0x22f748, 0x22f750, 0x22f7a0, 0x22f7b0, 0x22f7b8},
};

// Originals: resolved libmediandk entry points read out of the scene GOT.
using FnCreateDecoder = void* (*)(const char*);
using FnFormatNew = void* (*)();
using FnSetString = int32_t (*)(void*, const char*, const char*);
using FnSetInt32 = int32_t (*)(void*, const char*, int32_t);
using FnConfigure = int32_t (*)(void*, const void*, void*, void*, uint32_t);
using FnCodecVoid = int32_t (*)(void*);
using FnFormatDelete = void (*)(void*);
using FnDequeueOutput = int64_t (*)(void*, void*, int64_t);
using FnReleaseOutput = int32_t (*)(void*, size_t, bool);
using FnDequeueInput = int64_t (*)(void*, int64_t);
using FnGetInput = uint8_t* (*)(void*, size_t, size_t*);
using FnQueueInput = int32_t (*)(void*, size_t, int64_t, size_t, uint64_t, uint32_t);

FnCreateDecoder o_createDecoder;
FnFormatNew o_formatNew;
FnSetString o_setString;
FnSetInt32 o_setInt32;
FnConfigure o_configure;
FnCodecVoid o_start;
FnFormatDelete o_formatDelete;
FnDequeueOutput o_dequeueOutput;
FnReleaseOutput o_releaseOutput;
FnDequeueInput o_dequeueInput;
FnGetInput o_getInput;
FnQueueInput o_queueInput;
FnCodecVoid o_flush;
FnCodecVoid o_stop;
FnCodecVoid o_delete;

// ---------------------------------------------------------------------------
// Runtime state (single decoder in the app; one MediaThread drives it).
// ---------------------------------------------------------------------------
std::recursive_mutex g_mutex;
bool g_armed = false;             // decoder exists, first AU not yet sniffed
bool g_sniffDone = false;
void* g_pendingCodec = nullptr;   // decoder returned by createDecoderByType
void* g_armedCodec = nullptr;     // decoder after configure
void* g_buildingFormat = nullptr;
std::vector<FormatEntry> g_recipe;
void* g_capturedFormat = nullptr;
void* g_savedWindow = nullptr;
void* g_savedCrypto = nullptr;
uint32_t g_savedFlags = 0;
int g_auDumps = 0;              // first-AU structure diagnostics (one-shot per process)
bool g_recipeLogged = false;    // recorded-recipe dump (one-shot per process)
bool g_av1GeoDone = false;      // AV1 geometry parsed + patched for this session
uint32_t g_av1FrameW = 0;       // real frame width (from the frame header)
uint32_t g_av1FrameH = 0;       // real frame height
gxr::Av1SeqInfo g_av1Seq{};     // sequence-header geometry of the live session
std::map<void*, std::pair<size_t, uint8_t*>> g_inputPtrs;  // codec -> (idx, ptr)
std::map<void*, void*> g_handleMap;                        // retired -> live

std::atomic<int> g_patchState{0};  // 0 = not yet, 1 = installed, 2 = skipped/failed
std::atomic<bool> g_patchBusy{false};
uintptr_t g_sceneBase = 0;
bool g_sceneFound = false;

// ---------------------------------------------------------------------------
// Interception wrappers (installed into the scene GOT).
// ---------------------------------------------------------------------------
void* RealHandle(void* codec) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    auto it = g_handleMap.find(codec);
    return it == g_handleMap.end() ? codec : it->second;
}

// Queue an access unit into a freshly started decoder. Returns true when the
// AU was copied into one of its input buffers and accepted.
bool QueueAu(void* codec, const std::vector<uint8_t>& au, uint64_t timeUs, uint32_t flags,
             const char* what) {
    for (int attempt = 0; attempt < 100; attempt++) {
        int64_t idx = o_dequeueInput(codec, 10000);
        if (idx < 0) continue;
        size_t cap = 0;
        uint8_t* dst = o_getInput(codec, static_cast<size_t>(idx), &cap);
        if (!dst || cap < au.size()) {
            LOG("no input buffer large enough on %s; aborting swap", what);
            return false;
        }
        std::memcpy(dst, au.data(), au.size());
        if (o_queueInput(codec, static_cast<size_t>(idx), 0, au.size(), timeUs, flags) == 0) return true;
        LOG("re-queue attempt %d on %s failed", attempt, what);
    }
    return false;
}

// FindAv1SeqAndFrame / PatchAv1SeqHeaders are in av1_geometry.h (pure
// logic, exercised by the host tests; see the using declarations above).

// A MediaCodec output Surface accepts one producer connection at a time:
// configuring a second decoder while the app's HEVC decoder still holds the
// window fails nativeWindowConnect with EINVAL (-22). So the swap tears the
// old decoder down BEFORE the new one connects; if the swap cannot complete
// after that point, a stock decoder (original recipe mime) is rebuilt and the
// access unit re-queued into it. From the teardown on, this call owns the AU:
// the caller must never fall through to the retired handle (true is always
// returned once the teardown has happened).
bool TrySwap(void* oldCodec, const uint8_t* au, size_t size, uint64_t timeUs,
             uint32_t flags, const char* mime) {
    if (!o_createDecoder || !o_configure || !o_start || !o_formatNew || !o_formatDelete ||
        !o_dequeueInput || !o_getInput || !o_queueInput || !o_stop || !o_delete ||
        !o_setString || !o_setInt32) {
        return false;
    }
    if (g_recipe.empty() || !gxr::RecipeHasMime(g_recipe)) {
        LOG("no recorded decoder format recipe; keeping stock path");
        return false;
    }
    // The AU lives in the retired decoder's input buffer: copy it out before
    // any teardown invalidates that mapping.
    std::vector<uint8_t> auCopy;
    auCopy.assign(au, au + size);
    // Diagnostics: dump the structure of the first 2 AV1 AUs (one-shot) so a
    // rejected stream can be walked offline, and dump the recorded recipe once.
    if (!std::strcmp(mime, "video/av01") && g_auDumps < 2 && auCopy.size()) {
        g_auDumps++;
        LOG("AU%d size=%zu", g_auDumps, auCopy.size());
        char hex[3 * 48 + 1];
        const size_t hn = auCopy.size() < 48 ? auCopy.size() : 48;
        for (size_t i = 0; i < hn; i++) {
            static const char* digits = "0123456789abcdef";
            hex[i * 3] = digits[auCopy[i] >> 4];
            hex[i * 3 + 1] = digits[auCopy[i] & 15];
            hex[i * 3 + 2] = i + 1 < hn ? ' ' : '\0';
        }
        LOG("AU%d head: %s", g_auDumps, hex);
        struct ObuCtx { int au; };
        ObuCtx obuCtx{ g_auDumps };
        auto emit = [](void* c, size_t ix, uint8_t t, size_t off, size_t hl, uint64_t sz) {
            LOG("AU%d OBU%zu type=%u off=%zu hdr=%zu size=%llu",
                static_cast<ObuCtx*>(c)->au, ix, t, off, hl, (unsigned long long)sz);
        };
        gxr::ObuWalkResult w = gxr::WalkAv1Obus(auCopy.data(), auCopy.size(), emit, &obuCtx);
        if (w.failOffset) {
            LOG("AU%d OBU walk STOPPED at offset %zu (type=%u): %s (%zu OBUs walked)",
                obuCtx.au, w.failOffset, w.failType, w.failReason, w.obus);
        } else {
            LOG("AU%d OBU walk clean: %zu OBUs", obuCtx.au, w.obus);
        }
    }
    if (!g_recipeLogged) {
        g_recipeLogged = true;
        LOG("recipe dump (%zu entries):", g_recipe.size());
        for (const FormatEntry& e : g_recipe) {
            if (e.isString) LOG("recipe: %s = \"%s\"", e.key.c_str(), e.svalue.c_str());
            else LOG("recipe: %s = %d", e.key.c_str(), e.ivalue);
        }
    }
    void* live = o_createDecoder(mime);
    if (!live) {
        LOG("createDecoderByType(%s) failed; keeping stock path", mime);
        return false;
    }
    // AV1 geometry: the host's sequence header carries the encoder's init max
    // (3200x8192) while the real frame is smaller; the decoder then allocates
    // the max and crops, and the client's placement math expects buffer ==
    // frame. Parse both sizes, patch the sequence header in this AU down to
    // the frame size, and hand the frame size to the format hints below.
    if (!std::strcmp(mime, "video/av01")) {
        size_t so = 0, sl = 0, fo = 0, fl = 0;
        if (FindAv1SeqAndFrame(auCopy.data(), auCopy.size(), &so, &sl, &fo, &fl)) {
            gxr::Av1SeqInfo si = gxr::ParseAv1SeqMax(auCopy.data() + so, sl);
            if (si.ok) {
                gxr::Av1FrameSize fs =
                    gxr::ScanAv1FrameSize(auCopy.data() + fo, fl, si.widthBits, si.heightBits, si.maxW, si.maxH);
                if (fs.ok) {
                    LOG("geo: seq max=%ux%u bits=%u/%u frame=%ux%u; patching seq max + hints",
                        si.maxW, si.maxH, si.widthBits, si.heightBits, fs.w, fs.h);
                    if (gxr::PatchAv1SeqMax(auCopy.data() + so, sl, si, fs.w, fs.h)) {
                        g_av1GeoDone = true;
                        g_av1FrameW = fs.w;
                        g_av1FrameH = fs.h;
                        g_av1Seq = si;
                    }
                } else {
                    LOG("geo: frame-size scan failed (seq max %ux%u)", si.maxW, si.maxH);
                }
            } else {
                LOG("geo: sequence header parse failed");
            }
        } else {
            LOG("geo: no sequence/frame-header OBUs in the first AU");
        }
    }
    std::vector<FormatEntry> entries = gxr::FilterCodecSwapRecipe(gxr::SubstituteMime(g_recipe, mime));
    if (!std::strcmp(mime, "video/av01") && g_av1GeoDone) {
        for (FormatEntry& e : entries) {
            if (e.key == "width" || e.key == "max-width") e.ivalue = static_cast<int32_t>(g_av1FrameW);
            else if (e.key == "height" || e.key == "max-height") e.ivalue = static_cast<int32_t>(g_av1FrameH);
        }
    }
    void* fmt = o_formatNew();
    if (!fmt) { LOG("format allocation failed; keeping stock path"); o_delete(live); return false; }
    for (const FormatEntry& e : entries) {
        if (e.isString) { LOG("swap fmt: %s = \"%s\"", e.key.c_str(), e.svalue.c_str()); o_setString(fmt, e.key.c_str(), e.svalue.c_str()); }
        else { LOG("swap fmt: %s = %d", e.key.c_str(), e.ivalue); o_setInt32(fmt, e.key.c_str(), e.ivalue); }
    }
    // Teardown begins: release the Surface producer before the new connect.
    o_stop(oldCodec);
    o_delete(oldCodec);
    bool ok = o_configure(live, fmt, g_savedWindow, g_savedCrypto, g_savedFlags) == 0;
    if (ok) ok = o_start(live) == 0;
    if (ok) ok = QueueAu(live, auCopy, timeUs, flags, mime);
    o_formatDelete(fmt);
    if (ok) {
        g_handleMap[oldCodec] = live;
        Probe().VideoRemapped(oldCodec, live);
        LOG("switched decoder to %s; re-queued first AU (%zu bytes)", mime, size);
        return true;
    }
    LOG("swap to %s failed after teardown; rebuilding the stock decoder", mime);
    o_stop(live);
    o_delete(live);
    bool restored = false;
    const char* origMime = gxr::RecipeMime(g_recipe);
    if (origMime) {
        void* fmt2 = o_formatNew();
        if (fmt2) {
            for (const FormatEntry& e : g_recipe) {
                if (e.isString) o_setString(fmt2, e.key.c_str(), e.svalue.c_str());
                else o_setInt32(fmt2, e.key.c_str(), e.ivalue);
            }
            void* reborn = o_createDecoder(origMime);
            if (reborn) {
                if (o_configure(reborn, fmt2, g_savedWindow, g_savedCrypto, g_savedFlags) == 0 &&
                    o_start(reborn) == 0 && QueueAu(reborn, auCopy, timeUs, flags, origMime)) {
                    g_handleMap[oldCodec] = reborn;
                    Probe().VideoRemapped(oldCodec, reborn);
                    restored = true;
                    LOG("stock %s decoder rebuilt; stream continues on the fallback path", origMime);
                } else {
                    o_stop(reborn);
                    o_delete(reborn);
                }
            }
            o_formatDelete(fmt2);
        }
    }
    if (!restored) {
        Probe().VideoRemapped(oldCodec, nullptr);
        LOG("stock decoder rebuild failed; access unit dropped");
    }
    return true;  // the retired handle is gone: never fall through to it
}

void* WrapCreateDecoder(const char* mime) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    void* handle = o_createDecoder(mime);
    if (handle) {
        Probe().VideoCreated(handle, mime);
        g_armed = true;
        g_sniffDone = false;
        g_pendingCodec = handle;
        g_armedCodec = nullptr;
        g_buildingFormat = nullptr;
        g_capturedFormat = nullptr;
        g_recipe.clear();
        g_inputPtrs.clear();
    }
    return handle;
}

void* WrapFormatNew() {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    void* format = o_formatNew();
    if (g_armed && format) g_buildingFormat = format;
    return format;
}

int32_t WrapSetString(void* format, const char* key, const char* value) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    if (g_armed && format == g_buildingFormat && key && value) {
        g_recipe.push_back(FormatEntry{true, key, value, 0});
    }
    return o_setString(format, key, value);
}

int32_t WrapSetInt32(void* format, const char* key, int32_t value) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    if (g_armed && format == g_buildingFormat && key) {
        g_recipe.push_back(FormatEntry{false, key, "", value});
    }
    return o_setInt32(format, key, value);
}

int32_t WrapConfigure(void* codec, const void* format, void* window, void* crypto, uint32_t flags) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    if (g_armed && codec == g_pendingCodec) {
        g_armedCodec = codec;
        g_capturedFormat = const_cast<void*>(format);
        g_savedWindow = window;
        g_savedCrypto = crypto;
        g_savedFlags = flags;
    }
    return o_configure(RealHandle(codec), format, window, crypto, flags);
}

int32_t WrapCbStart(void* codec) { return o_start(RealHandle(codec)); }
int32_t WrapCbFlush(void* codec) {
    void* live = RealHandle(codec);
    int32_t status = o_flush(live);
    Probe().Reset(live);
    return status;
}
int32_t WrapCbStop(void* codec) {
    void* live = RealHandle(codec);
    int32_t status = o_stop(live);
    Probe().Reset(live);
    return status;
}

void WrapFormatDelete(void* format) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    if (format == g_buildingFormat) g_buildingFormat = nullptr;
    if (format == g_capturedFormat) g_capturedFormat = nullptr;
    o_formatDelete(format);
}

int64_t WrapDequeueOutput(void* codec, void* info, int64_t timeoutUs) {
    void* live = RealHandle(codec);
    int64_t index = o_dequeueOutput(live, info, timeoutUs);
    if (index >= 0) Probe().Output(live, static_cast<const AMediaCodecBufferInfo*>(info));
    return index;
}

int32_t WrapReleaseOutput(void* codec, size_t idx, bool render) {
    return o_releaseOutput(RealHandle(codec), idx, render);
}

int64_t WrapDequeueInput(void* codec, int64_t timeoutUs) {
    return o_dequeueInput(RealHandle(codec), timeoutUs);
}

uint8_t* WrapGetInput(void* codec, size_t idx, size_t* outSize) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    void* live = RealHandle(codec);
    uint8_t* ptr = o_getInput(live, idx, outSize);
    if (outSize) Probe().Input(live, idx, ptr, *outSize);
    if (g_armed && !g_sniffDone && codec == g_armedCodec && ptr) {
        g_inputPtrs[codec] = std::make_pair(idx, ptr);
    }
    return ptr;
}

int32_t WrapQueueInput(void* codec, size_t idx, int64_t offset, size_t size, uint64_t timeUs,
                       uint32_t flags) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    void* live = RealHandle(codec);
    if (live == codec && g_armed && !g_sniffDone && codec == g_armedCodec && size > 0 &&
        size <= kMaxSniffedAu) {
        auto it = g_inputPtrs.find(codec);
        if (it != g_inputPtrs.end() && it->second.first == idx && it->second.second) {
            const uint8_t* au = it->second.second + offset;
            uint8_t head[64];
            size_t hn = size < sizeof(head) ? size : sizeof(head);
            std::memcpy(head, au, hn);
            Sniffed sniffed = gxr::SniffAccessUnit(head, hn);
            if (sniffed == Sniffed::H264 || sniffed == Sniffed::AV1) {
                const char* mime = sniffed == Sniffed::H264 ? "video/avc" : "video/av01";
                if (TrySwap(codec, au, size, timeUs, flags, mime)) {
                    // Swap owns/re-writes/re-queues its AU internally: omit it
                    // rather than label its original bytes as an accepted AU.
                    g_armed = false;
                    g_sniffDone = true;
                    g_inputPtrs.clear();
                    return 0;
                }
                g_sniffDone = true;  // one shot: never retry a failed swap
            } else {
                LOG("first AU reads as %s; stock HEVC path kept",
                    gxr::SniffedName(sniffed));
                g_sniffDone = true;
            }
        }
    }
    auto capture = Probe().BeforeQueue(live, idx, offset, size, flags);
    if (live != codec && g_av1GeoDone && offset == 0 && size > 0) {
        size_t cap = 0;
        uint8_t* dst = o_getInput(live, idx, &cap);
        if (dst && cap >= size) {
            const int patched = PatchAv1SeqHeaders(dst + offset, size, g_av1FrameW, g_av1FrameH);
            if (patched) LOG("geo: patched %d sequence header(s) this AU", patched);
        }
    }
    auto submission = Probe().BeginQueue(live);
    int32_t status = o_queueInput(live, idx, offset, size, timeUs, flags);
    Probe().Queue(live, timeUs, size, flags, status, submission, std::move(capture));
    return status;
}

int32_t WrapDelete(void* codec) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    Probe().Reset(RealHandle(codec), true);
    auto it = g_handleMap.find(codec);
    if (it != g_handleMap.end()) {
        void* live = it->second;
        g_handleMap.erase(it);
        return o_delete(live);
    }
    if (g_armed && codec == g_pendingCodec) {
        g_armed = false;
        g_sniffDone = true;
    }
    return o_delete(codec);
}

// ---------------------------------------------------------------------------
// Scene-library discovery + GOT patch installation.
// ---------------------------------------------------------------------------
int PhdrCallback(struct dl_phdr_info* info, size_t, void*) {
    if (info->dlpi_name && std::strstr(info->dlpi_name, kSceneSoname)) {
        g_sceneBase = static_cast<uintptr_t>(info->dlpi_addr);
        g_sceneFound = true;
        return 1;
    }
    return 0;
}

void* WrapperForSlot(size_t ix) {
    switch (ix) {
        case SlotCreateDecoder: return reinterpret_cast<void*>(&WrapCreateDecoder);
        case SlotFormatNew: return reinterpret_cast<void*>(&WrapFormatNew);
        case SlotSetString: return reinterpret_cast<void*>(&WrapSetString);
        case SlotSetInt32: return reinterpret_cast<void*>(&WrapSetInt32);
        case SlotConfigure: return reinterpret_cast<void*>(&WrapConfigure);
        case SlotStart: return reinterpret_cast<void*>(&WrapCbStart);
        case SlotFormatDelete: return reinterpret_cast<void*>(&WrapFormatDelete);
        case SlotDequeueOutput: return reinterpret_cast<void*>(&WrapDequeueOutput);
        case SlotReleaseOutput: return reinterpret_cast<void*>(&WrapReleaseOutput);
        case SlotDequeueInput: return reinterpret_cast<void*>(&WrapDequeueInput);
        case SlotGetInput: return reinterpret_cast<void*>(&WrapGetInput);
        case SlotQueueInput: return reinterpret_cast<void*>(&WrapQueueInput);
        case SlotFlush: return reinterpret_cast<void*>(&WrapCbFlush);
        case SlotStop: return reinterpret_cast<void*>(&WrapCbStop);
        case SlotDelete: return reinterpret_cast<void*>(&WrapDelete);
        default: return nullptr;
    }
}

void TryPatch() {
    if (g_patchState.load() != 0) return;
    if (std::getenv(kDisableEnv)) {
        g_patchState.store(2);
        LOG("disabled by %s", kDisableEnv);
        return;
    }
    bool expected = false;
    if (!g_patchBusy.compare_exchange_strong(expected, true)) return;
    g_sceneFound = false;
    g_sceneBase = 0;
    dl_iterate_phdr(PhdrCallback, nullptr);
    if (!g_sceneFound) {
        g_patchBusy.store(false);  // not loaded yet: retried from later XR calls
        return;
    }
    uint8_t digest[32];
    char hex[65];
    gxr::Sha256::Digest(reinterpret_cast<const void*>(g_sceneBase + kProfile5001712.anchorOffset),
                        0x80, digest);
    gxr::Sha256::Hex(digest, hex);
    const GotProfile* profile = nullptr;
    if (std::strcmp(hex, kProfile5001712.anchorSha256) == 0) {
        profile = &kProfile5001712;
    } else {
        gxr::Sha256::Digest(
            reinterpret_cast<const void*>(g_sceneBase + kProfile5002363.anchorOffset), 0x80, digest);
        gxr::Sha256::Hex(digest, hex);
        if (std::strcmp(hex, kProfile5002363.anchorSha256) == 0) profile = &kProfile5002363;
    }
    if (!profile) {
        LOG("libvrlink_scene.so matches no pinned base layout; no interception (fail closed)");
        g_patchState.store(2);
        g_patchBusy.store(false);
        return;
    }
    void* orig[kSlotCount];
    for (size_t i = 0; i < kSlotCount; i++) {
        void** slot = reinterpret_cast<void**>(g_sceneBase + profile->slots[i]);
        orig[i] = *slot;
    }
    o_createDecoder = reinterpret_cast<FnCreateDecoder>(orig[SlotCreateDecoder]);
    o_formatNew = reinterpret_cast<FnFormatNew>(orig[SlotFormatNew]);
    o_setString = reinterpret_cast<FnSetString>(orig[SlotSetString]);
    o_setInt32 = reinterpret_cast<FnSetInt32>(orig[SlotSetInt32]);
    o_configure = reinterpret_cast<FnConfigure>(orig[SlotConfigure]);
    o_start = reinterpret_cast<FnCodecVoid>(orig[SlotStart]);
    o_formatDelete = reinterpret_cast<FnFormatDelete>(orig[SlotFormatDelete]);
    o_dequeueOutput = reinterpret_cast<FnDequeueOutput>(orig[SlotDequeueOutput]);
    o_releaseOutput = reinterpret_cast<FnReleaseOutput>(orig[SlotReleaseOutput]);
    o_dequeueInput = reinterpret_cast<FnDequeueInput>(orig[SlotDequeueInput]);
    o_getInput = reinterpret_cast<FnGetInput>(orig[SlotGetInput]);
    o_queueInput = reinterpret_cast<FnQueueInput>(orig[SlotQueueInput]);
    o_flush = reinterpret_cast<FnCodecVoid>(orig[SlotFlush]);
    o_stop = reinterpret_cast<FnCodecVoid>(orig[SlotStop]);
    o_delete = reinterpret_cast<FnCodecVoid>(orig[SlotDelete]);
    long pageSize = sysconf(_SC_PAGESIZE);
    uintptr_t lowest = ~static_cast<uintptr_t>(0);
    uintptr_t highest = 0;
    for (size_t i = 0; i < kSlotCount; i++) {
        uintptr_t addr = g_sceneBase + profile->slots[i];
        if (addr < lowest) lowest = addr;
        if (addr > highest) highest = addr;
    }
    uintptr_t mapStart = lowest & ~static_cast<uintptr_t>(pageSize - 1);
    uintptr_t mapEnd = (highest + sizeof(void*) + static_cast<uintptr_t>(pageSize) - 1) &
                       ~static_cast<uintptr_t>(pageSize - 1);
    if (mprotect(reinterpret_cast<void*>(mapStart), mapEnd - mapStart, PROT_READ | PROT_WRITE) != 0) {
        LOG("mprotect(RW) on the scene GOT failed; no interception");
        g_patchState.store(2);
        g_patchBusy.store(false);
        return;
    }
    size_t installed = 0;
    for (size_t i = 0; i < kSlotCount; i++) {
        void** slot = reinterpret_cast<void**>(g_sceneBase + profile->slots[i]);
        void* target = WrapperForSlot(i);
        if (!target) continue;
        *slot = target;
        installed++;
    }
    mprotect(reinterpret_cast<void*>(mapStart), mapEnd - mapStart, PROT_READ);
    g_patchState.store(1);
    g_patchBusy.store(false);
    Probe(); // start opt-in request polling only after exact-base admission
    LOG("interception installed on %s (%zu slots) build=%s", profile->build, installed, kBuild);
}

// ---------------------------------------------------------------------------
// OpenXR layer plumbing: negotiate, then use per-frame dispatch calls as the
// scene-library readiness trigger (layers mount before the scene library).
// ---------------------------------------------------------------------------
struct Dispatch {
    PFN_xrGetInstanceProcAddr get{};
    PFN_xrCreateSession createSession{};
    PFN_xrWaitFrame waitFrame{};
    PFN_xrEndFrame endFrame{};
} g;

XrResult XRAPI_PTR WrapCreateSession(XrInstance instance, const XrSessionCreateInfo* info,
                                     XrSession* session) {
    TryPatch();
    return g.createSession(instance, info, session);
}

XrResult XRAPI_PTR WrapWaitFrame(XrSession session, const XrFrameWaitInfo* frameWaitInfo,
                                 XrFrameState* frameState) {
    TryPatch();
    return g.waitFrame(session, frameWaitInfo, frameState);
}

XrResult XRAPI_PTR WrapEndFrame(XrSession session, const XrFrameEndInfo* frameEndInfo) {
    TryPatch();
    return g.endFrame(session, frameEndInfo);
}

XrResult XRAPI_PTR LayerGetInstanceProcAddr(XrInstance instance, const char* name,
                                            PFN_xrVoidFunction* function) {
    if (!name || !function) return XR_ERROR_VALIDATION_FAILURE;
#define ROUTE(fnName, member)                          \
    if (std::strcmp(name, fnName) == 0) {              \
        *function = reinterpret_cast<PFN_xrVoidFunction>(member); \
        return XR_SUCCESS;                             \
    }
    ROUTE("xrCreateSession", WrapCreateSession);
    ROUTE("xrWaitFrame", WrapWaitFrame);
    ROUTE("xrEndFrame", WrapEndFrame);
#undef ROUTE
    return g.get ? g.get(instance, name, function) : XR_ERROR_FUNCTION_UNSUPPORTED;
}

XrResult XRAPI_PTR LayerCreateInstance(const XrInstanceCreateInfo* info,
                                       const XrApiLayerCreateInfo* layer, XrInstance* instance) {
    if (!info || !layer || !layer->nextInfo || !instance) return XR_ERROR_INITIALIZATION_FAILED;
    XrApiLayerCreateInfo next = *layer;
    next.nextInfo = layer->nextInfo->next;
    const XrResult result = layer->nextInfo->nextCreateApiLayerInstance(info, &next, instance);
    if (XR_FAILED(result)) return result;
    g.get = layer->nextInfo->nextGetInstanceProcAddr;
#define LOAD(fnName, member)                                                            \
    if (XR_FAILED(g.get(*instance, fnName, reinterpret_cast<PFN_xrVoidFunction*>(&g.member))) || \
        !g.member) {                                                                    \
        g = Dispatch{};                                                                 \
        return XR_ERROR_INITIALIZATION_FAILED;                                          \
    }
    LOAD("xrCreateSession", createSession);
    LOAD("xrWaitFrame", waitFrame);
    LOAD("xrEndFrame", endFrame);
#undef LOAD
    LOG("initialized build=%s", kBuild);
    TryPatch();
    return result;
}

__attribute__((constructor)) void OnLoad() {
    LOG("library loaded build=%s", kBuild);
    TryPatch();
}

}  // namespace

EXPORT XrResult XRAPI_PTR xrNegotiateLoaderApiLayerInterface(const XrNegotiateLoaderInfo* loader,
                                                             const char* name,
                                                             XrNegotiateApiLayerRequest* request) {
    if (!loader || !name || !request || std::strcmp(name, kLayerName) != 0 ||
        loader->minInterfaceVersion > XR_CURRENT_LOADER_API_LAYER_VERSION ||
        loader->maxInterfaceVersion < XR_CURRENT_LOADER_API_LAYER_VERSION ||
        loader->maxApiVersion < XR_MAKE_VERSION(1, 0, 0) ||
        loader->minApiVersion > XR_MAKE_VERSION(1, 1, 0)) {
        return XR_ERROR_INITIALIZATION_FAILED;
    }
    request->layerInterfaceVersion = XR_CURRENT_LOADER_API_LAYER_VERSION;
    request->layerApiVersion = std::min<XrVersion>(loader->maxApiVersion, XR_MAKE_VERSION(1, 1, 0));
    request->getInstanceProcAddr = LayerGetInstanceProcAddr;
    request->createApiLayerInstance = LayerCreateInstance;
    return XR_SUCCESS;
}
