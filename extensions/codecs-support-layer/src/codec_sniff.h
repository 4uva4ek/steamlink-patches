// First-access-unit codec detection for the Codecs Support layer.
// Pure logic, no Android deps; exhaustive vectors in tests/pure_tests.cpp.
//
// The client never learns the stream codec from the host, so the layer
// sniffs the first compressed access unit after the decoder starts:
//   - AV1  : OBU stream, temporal-delimiter OBU (0x12 0x00) or a
//            sequence-header OBU first; Annex-B TD variant 00 00 01 10.
//   - HEVC : Annex-B, NAL type (b>>1)&0x3f in the HEVC set.
//   - H264 : Annex-B, NAL type b&0x1f in the H264 set.
// Anything else (including 4-byte length prefixes) is UNKNOWN and the
// stock HEVC path is kept: never swap on a guess.
#pragma once
#include <cstdint>
#include <cstddef>

namespace gxr {

enum class Sniffed { KeepHevc, H264, AV1, Unknown };

inline bool IsHevcNalByte(uint8_t b) {
    uint32_t t = (b >> 1) & 0x3f;
    if (t <= 1) return true;                  // TRAIL_N / TRAIL_R
    if (t >= 16 && t <= 21) return true;      // BLA/IDR/CRA slices
    if (t >= 32 && t <= 35) return true;      // VPS/SPS/PPS/AUD
    return false;
}

inline bool IsH264NalByte(uint8_t b) {
    uint32_t t = b & 0x1f;
    switch (t) {
        case 1: case 2: case 5: case 6: case 7: case 8: case 9:
            return true;
        default:
            return false;
    }
}

inline Sniffed SniffAccessUnit(const uint8_t* p, size_t n) {
    if (!p || n < 2) return Sniffed::KeepHevc;
    // AV1 checks first: 0x12 would otherwise alias H264 type 2 / HEVC
    // reserved types, and 0x0A (sequence header, small LEB128 size) does
    // not collide with any Annex-B start-code byte.
    if (p[0] == 0x12 && (p[1] & 0x80) == 0) return Sniffed::AV1;
    if (p[0] == 0x0A && (p[1] & 0x80) == 0 && p[1] <= 0x40) return Sniffed::AV1;
    // Annex-B start code, first unit only.
    size_t sc = 0;
    if (n >= 4 && p[0] == 0 && p[1] == 0 && p[2] == 0 && p[3] == 1) sc = 4;
    else if (n >= 3 && p[0] == 0 && p[1] == 0 && p[2] == 1) sc = 3;
    if (sc && n > sc) {
        if (sc == 3 && p[sc] == 0x10) return Sniffed::AV1;   // Annex-B TD
        uint8_t b = p[sc];
        if (IsHevcNalByte(b)) return Sniffed::KeepHevc;
        if (IsH264NalByte(b)) return Sniffed::H264;
        return Sniffed::Unknown;
    }
    // 4-byte length prefix (AVCC/HVCC-framed stream): not supported, keep stock.
    if (n >= 4) {
        uint32_t len = (static_cast<uint32_t>(p[0]) << 24) | (static_cast<uint32_t>(p[1]) << 16) |
                       (static_cast<uint32_t>(p[2]) << 8) | static_cast<uint32_t>(p[3]);
        if (len == n - 4 && len > 0) return Sniffed::Unknown;
    }
    return Sniffed::KeepHevc;
}

// ---------------------------------------------------------------------------
// AV1 temporal-unit structure walk (diagnostics). Validates the OBU chain of
// one access unit -- forbidden bit, LEB128 size fields, TD rules, payload
// framing -- and reports the first malformed or truncated unit. Pure logic.
// ---------------------------------------------------------------------------
struct ObuWalkResult {
    size_t obus;         // OBUs walked successfully
    size_t failOffset;   // 0 when the walk is clean
    uint8_t failType;
    const char* failReason;
};
typedef void (*ObuEmitFn)(void* ctx, size_t index, uint8_t type, size_t offset, size_t headerLen,
                          uint64_t payload);

inline ObuWalkResult WalkAv1Obus(const uint8_t* p, size_t n, ObuEmitFn emit, void* ctx) {
    ObuWalkResult r{0, 0, 0, nullptr};
    if (!p) return r;
    size_t off = 0;
    while (off < n) {
        const size_t start = off;
        const uint8_t h = p[off];
        if (h & 0x80) { r.failOffset = start; r.failType = h; r.failReason = "forbidden bit set"; return r; }
        const uint8_t type = (h >> 3) & 0x0F;
        if (type == 0 || (type >= 9 && type <= 14)) {
            r.failOffset = start; r.failType = type; r.failReason = "reserved OBU type"; return r;
        }
        const bool extHdr = ((h >> 2) & 1) != 0;
        const bool hasSize = ((h >> 1) & 1) != 0;
        off++;
        if (extHdr) {
            if (off + 1 >= n) {
                r.failOffset = start; r.failType = type; r.failReason = "truncated extension header"; return r;
            }
            off += 2;
        }
        uint64_t sz = 0;
        if (hasSize) {
            int shift = 0; size_t bytes = 0; bool done = false;
            while (off < n && bytes < 8) {
                const uint8_t b = p[off++]; bytes++;
                sz |= static_cast<uint64_t>(b & 0x7F) << shift; shift += 7;
                if (!(b & 0x80)) { done = true; break; }
            }
            if (!done) {
                r.failOffset = start; r.failType = type; r.failReason = "unterminated LEB128 size"; return r;
            }
        }
        if (type == 2) {  // temporal delimiter: zero-size unit
            if (hasSize) {
                if (sz != 0) { r.failOffset = start; r.failType = type; r.failReason = "TD with nonzero size"; return r; }
            } else if (off < n && p[off] == 0) {
                off++;  // size-less TD still carries one zero byte
            } else {
                r.failOffset = start; r.failType = type; r.failReason = "TD without zero byte"; return r;
            }
            sz = 0;
        } else if (!hasSize) {
            r.failOffset = start; r.failType = type; r.failReason = "OBU without size field"; return r;
        }
        if (static_cast<uint64_t>(off) + sz > n) {
            r.failOffset = start; r.failType = type; r.failReason = "payload truncated"; return r;
        }
        if (emit) emit(ctx, r.obus, type, start, off - start, sz);
        off += static_cast<size_t>(sz);
        r.obus++;
    }
    return r;
}

inline const char* SniffedName(Sniffed s) {
    switch (s) {
        case Sniffed::KeepHevc: return "HEVC";
        case Sniffed::H264: return "H264";
        case Sniffed::AV1: return "AV1";
        default: return "unknown";
    }
}

}  // namespace gxr
