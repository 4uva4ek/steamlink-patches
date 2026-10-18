// Opt-in decoder observation only. No transport IDs or presentation claims.
// Platform-independent state; Android I/O and logging live in the layer.
#pragma once
#include <cstddef>
#include <cstdint>
#include <array>
#include <cstring>
#include <map>
#include <sstream>
#include <string>
#include <utility>

namespace gxr {

constexpr size_t kProbeMaxCapture = 16u << 20;
constexpr size_t kProbeMaxPending = 128;

inline bool ProbeToken(const std::string& value) {
    if (value.size() != 32) return false;
    for (char c : value)
        if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') ||
              (c >= 'A' && c <= 'F'))) return false;
    return true;
}

inline bool ProbeRequest(const std::string& text, std::string& token, std::string* nonce = nullptr) {
    std::istringstream input(text);
    std::string extra, parsedToken, parsedNonce;
    if (!(input >> parsedToken) || !ProbeToken(parsedToken)) return false;
    if (nonce && (!(input >> parsedNonce) || !ProbeToken(parsedNonce))) return false;
    if (input >> extra) return false;
    token = parsedToken;
    if (nonce) *nonce = parsedNonce;
    return true;
}

inline bool ProbeBufferRange(size_t capacity, int64_t offset, size_t size) {
    return offset >= 0 && static_cast<uint64_t>(offset) <= capacity &&
           size <= capacity - static_cast<size_t>(offset) && size <= kProbeMaxCapture;
}

// Preference for a useful independently parsable capture, never codec proof.
inline bool ProbeHasSequenceHeader(const uint8_t* bytes, size_t size) {
    if (!bytes) return false;
    size_t limit = size < 65536 ? size : 65536;
    // AV1 low-overhead sequence OBU, optionally following the temporal delimiter.
    size_t first = size >= 2 && bytes[0] == 0x12 && bytes[1] == 0 ? 2 : 0;
    if (first + 1 < size && bytes[first] == 0x0a) return true;
    for (size_t i = 0; i + 4 < limit; ++i) {
        if (bytes[i] || bytes[i + 1] || bytes[i + 2] != 1) continue;
        uint8_t nal = bytes[i + 3];
        if ((nal & 0x80) == 0 && ((nal & 31) == 7 ||
            (((nal >> 1) & 63) == 33 && (bytes[i + 4] & 7)))) return true;
    }
    return false;
}

struct DecoderProbeCounters {
    uint64_t stream = 0, discontinuities = 0;
    uint64_t queue_successes = 0, queue_failures = 0;
    uint64_t accepted_input_bytes = 0, accepted_input_frames = 0, output_frames = 0;
    uint64_t queue_to_output_count = 0, queue_to_output_sum_ns = 0, queue_to_output_max_ns = 0;
    uint64_t ambiguous_pts = 0, unmatched_outputs = 0, pending_evictions = 0;
};

class DecoderProbeState {
public:
    std::string token;
    DecoderProbeCounters counters;

    bool IsVideo(uintptr_t codec) const {
        if (!codec) return false;
        for (uintptr_t tracked : videoHandles) if (tracked == codec) return true;
        return false;
    }

    bool AdmitVideo(uintptr_t codec, const char* mime) {
        if (!codec || !mime || std::strncmp(mime, "video/", 6) || !mime[6]) return false;
        if (IsVideo(codec)) return true;
        for (auto& tracked : videoHandles) if (!tracked) { tracked = codec; return true; }
        return false; // unknown/excess handles are never admitted by inference
    }

    bool ForgetVideo(uintptr_t codec) {
        for (auto& tracked : videoHandles) if (codec && tracked == codec) { tracked = 0; return true; }
        return false;
    }

    bool Session(const std::string& next) {
        if (next == token) return false;
        token = next;
        Reset();
        return true;
    }

    void Reset() {
        uint64_t generation = counters.stream + 1;
        uint64_t discontinuities = counters.discontinuities + 1;
        counters = DecoderProbeCounters{};
        counters.stream = generation;
        counters.discontinuities = discontinuities;
        pending.clear();
    }

    void Queue(uintptr_t codec, uint64_t pts, size_t size, uint32_t flags,
               int32_t status, uint64_t now) {
        if (token.empty() || !IsVideo(codec)) return;
        if (status) { ++counters.queue_failures; return; }
        ++counters.queue_successes;
        if (!size || (flags & (2u | 4u))) return; // codec configuration / EOS
        if (!(flags & 8u)) ++counters.accepted_input_frames; // partial input is not a complete frame
        counters.accepted_input_bytes += size;
        auto key = std::make_pair(codec, pts);
        auto found = pending.find(key);
        if (found != pending.end()) {
            found->second.ambiguous = true;
            ++found->second.outstanding;
            ++counters.ambiguous_pts;
            return;
        }
        if (pending.size() >= kProbeMaxPending) {
            auto oldest = pending.begin();
            for (auto it = pending.begin(); it != pending.end(); ++it)
                if (it->second.time < oldest->second.time) oldest = it;
            pending.erase(oldest);
            ++counters.pending_evictions;
        }
        pending.emplace(key, Pending{now, false, 1});
    }

    void Output(uintptr_t codec, uint64_t pts, uint32_t flags, uint64_t now) {
        if (token.empty() || !IsVideo(codec) || (flags & (2u | 4u))) return;
        ++counters.output_frames; // MediaCodec output, not display/scanout
        auto found = pending.find(std::make_pair(codec, pts));
        if (found == pending.end()) { ++counters.unmatched_outputs; return; }
        if (!found->second.ambiguous && now >= found->second.time) {
            uint64_t duration = now - found->second.time;
            ++counters.queue_to_output_count;
            counters.queue_to_output_sum_ns += duration;
            if (duration > counters.queue_to_output_max_ns) counters.queue_to_output_max_ns = duration;
        }
        if (!found->second.ambiguous || --found->second.outstanding == 0) pending.erase(found);
    }

    size_t PendingCount() const { return pending.size(); }

private:
    std::array<uintptr_t, 16> videoHandles{};
    struct Pending { uint64_t time; bool ambiguous; uint64_t outstanding; };
    std::map<std::pair<uintptr_t, uint64_t>, Pending> pending;
};

} // namespace gxr
