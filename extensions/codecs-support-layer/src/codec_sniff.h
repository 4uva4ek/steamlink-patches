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

inline const char* SniffedName(Sniffed s) {
    switch (s) {
        case Sniffed::KeepHevc: return "HEVC";
        case Sniffed::H264: return "H264";
        case Sniffed::AV1: return "AV1";
        default: return "unknown";
    }
}

}  // namespace gxr
