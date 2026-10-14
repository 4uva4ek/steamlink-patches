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
#include <unistd.h>

#include <algorithm>
#include <atomic>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <map>
#include <mutex>
#include <string>
#include <utility>
#include <vector>

#include <openxr/openxr.h>
#include <openxr/openxr_loader_negotiation.h>

#include "codec_sniff.h"
#include "format_recipe.h"
#include "sha256.h"

#define EXPORT extern "C" __attribute__((visibility("default")))
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "GXRCodecsSupport", __VA_ARGS__)

namespace {

using gxr::FormatEntry;
using gxr::Sniffed;

constexpr const char* kLayerName = "XR_APILAYER_local_GalaxyXR_codecs_support";
constexpr const char* kBuild = "codecs-support-v4-20261014";
constexpr const char* kDisableEnv = "GXR_DISABLE_CODECS_SUPPORT";
constexpr const char* kSceneSoname = "libvrlink_scene.so";
constexpr size_t kMaxSniffedAu = 4u << 20;

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
    std::vector<FormatEntry> entries = gxr::FilterCodecSwapRecipe(gxr::SubstituteMime(g_recipe, mime));
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
    if (!restored) LOG("stock decoder rebuild failed; access unit dropped");
    return true;  // the retired handle is gone: never fall through to it
}

void* WrapCreateDecoder(const char* mime) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    void* handle = o_createDecoder(mime);
    if (handle) {
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
int32_t WrapCbFlush(void* codec) { return o_flush(RealHandle(codec)); }
int32_t WrapCbStop(void* codec) { return o_stop(RealHandle(codec)); }

void WrapFormatDelete(void* format) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    if (format == g_buildingFormat) g_buildingFormat = nullptr;
    if (format == g_capturedFormat) g_capturedFormat = nullptr;
    o_formatDelete(format);
}

int64_t WrapDequeueOutput(void* codec, void* info, int64_t timeoutUs) {
    return o_dequeueOutput(RealHandle(codec), info, timeoutUs);
}

int32_t WrapReleaseOutput(void* codec, size_t idx, bool render) {
    return o_releaseOutput(RealHandle(codec), idx, render);
}

int64_t WrapDequeueInput(void* codec, int64_t timeoutUs) {
    return o_dequeueInput(RealHandle(codec), timeoutUs);
}

uint8_t* WrapGetInput(void* codec, size_t idx, size_t* outSize) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
    uint8_t* ptr = o_getInput(RealHandle(codec), idx, outSize);
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
    return o_queueInput(live, idx, offset, size, timeUs, flags);
}

int32_t WrapDelete(void* codec) {
    std::lock_guard<std::recursive_mutex> lock(g_mutex);
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
