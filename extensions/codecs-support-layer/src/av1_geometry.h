// AV1 sequence/frame geometry for the Codecs Support layer.
//
// The host encoder is initialized at the HMD's full stream size (3200x8192)
// and only *reconfigured* down to the session's real size. NVENC's AV1
// sequence header keeps the init-time maximum, so the decoder allocates a
// buffer of the max and crops the real frame out of it -- a geometry the
// client's foveal/background placement math does not expect (HEVC rewrites
// its SPS on reconfigure, so only AV1 shows this). The layer therefore:
//   1. parses the sequence header's max_frame_width/height (bit-exact),
//   2. finds the real frame size in the frame header,
//   3. patches every sequence header's max down to the real frame size and
//      configures the swapped decoder with the real size as its hints.
//
// Pure logic (bit reader + parse + patch + constrained size scan); vectors
// live in pure_tests.cpp with ffprobe-verified expected values.
#pragma once
#include <cstdint>
#include <cstddef>

namespace gxr {

inline uint32_t Av1GetBits(const uint8_t* p, size_t bitOff, uint32_t n) {
    uint32_t v = 0;
    for (uint32_t i = 0; i < n; i++) {
        const size_t b = bitOff + i;
        v = (v << 1) | ((p[b >> 3] >> (7 - (b & 7))) & 1u);
    }
    return v;
}

inline void Av1SetBits(uint8_t* p, size_t bitOff, uint32_t n, uint32_t v) {
    for (uint32_t i = 0; i < n; i++) {
        const size_t b = bitOff + i;
        const uint8_t mask = static_cast<uint8_t>(1u << (7 - (b & 7)));
        if ((v >> (n - 1 - i)) & 1u) p[b >> 3] |= mask;
        else p[b >> 3] &= static_cast<uint8_t>(~mask);
    }
}

struct Av1SeqInfo {
    bool ok;
    uint32_t maxW, maxH;        // max_frame_width/height (plus one)
    uint32_t widthBits;         // frame_width_bits  (min 1)
    uint32_t heightBits;        // frame_height_bits (min 1)
    size_t maxWidthBitOff;      // bit offset of max_frame_width_minus_1
    size_t maxHeightBitOff;     // bit offset of max_frame_height_minus_1
};

// Parse the sequence-header OBU payload far enough to reach and read
// frame_width_bits/height_bits and the two max_frame size fields.
inline Av1SeqInfo ParseAv1SeqMax(const uint8_t* p, size_t n) {
    Av1SeqInfo r{false, 0, 0, 0, 0, 0, 0};
    if (!p || n < 2) return r;
    const size_t total = n * 8;
    size_t o = 0;
    auto avail = [&](size_t bits) { return o + bits <= total; };

    if (!avail(5)) return r;
    o += 3;                                     // seq_profile
    o += 1;                                     // still_picture
    const uint32_t reduced = Av1GetBits(p, o, 1); o += 1;

    if (!reduced) {
        if (!avail(1)) return r;
        const uint32_t timingPresent = Av1GetBits(p, o, 1); o += 1;
        uint32_t decoderModelPresent = 0;
        uint32_t bufferDelayLen = 0;
        if (timingPresent) {
            o += 64;                            // num_units_in_display_tick + time_scale
            if (!avail(1)) return r;
            const uint32_t equalPic = Av1GetBits(p, o, 1); o += 1;
            if (equalPic) {                     // uvlc skip: zeros.. then 1 then zeros bits
                uint32_t zeros = 0;
                while (avail(1) && Av1GetBits(p, o, 1) == 0 && zeros < 32) { zeros++; o++; }
                if (!avail(1)) return r;
                o += 1;                         // the terminating 1
                o += zeros;                     // payload bits
            }
            if (!avail(1)) return r;
            decoderModelPresent = Av1GetBits(p, o, 1); o += 1;
            if (decoderModelPresent) {
                if (!avail(5)) return r;
                bufferDelayLen = Av1GetBits(p, o, 5) + 1; o += 5;
                o += 32;                        // num_units_in_decoding_tick
                o += 5 + 5;                     // buffer_removal_time_length + frame_presentation_time_length
            }
        }
        if (!avail(1)) return r;
        const uint32_t initialDelayPresent = Av1GetBits(p, o, 1); o += 1;
        if (!avail(5)) return r;
        const uint32_t opCntMinus1 = Av1GetBits(p, o, 5); o += 5;
        if (opCntMinus1 > 31) return r;
        for (uint32_t i = 0; i <= opCntMinus1; i++) {
            if (!avail(12 + 5)) return r;
            o += 12;                            // operating_point_idc
            const uint32_t level = Av1GetBits(p, o, 5); o += 5;
            if (level > 7) { if (!avail(1)) return r; o += 1; }      // seq_tier
            if (decoderModelPresent) {
                if (!avail(1)) return r;
                if (Av1GetBits(p, o, 1)) { o += 1 + bufferDelayLen + bufferDelayLen + 1; }
                else o += 1;
            }
            if (initialDelayPresent) {
                if (!avail(1)) return r;
                if (Av1GetBits(p, o, 1)) o += 1 + 4;
                else o += 1;
            }
        }
    } else {
        // reduced_still_picture_header: one implicit operating point; only the
        // level index is still read.
        if (!avail(5)) return r;
        o += 5;                                 // seq_level_idx[0]
    }

    if (!avail(4 + 4)) return r;
    const uint32_t widthBits = Av1GetBits(p, o, 4) + 1; o += 4;
    const uint32_t heightBits = Av1GetBits(p, o, 4) + 1; o += 4;
    if (widthBits > 16 || heightBits > 16) return r;
    if (!avail(widthBits + heightBits)) return r;
    r.maxWidthBitOff = o;
    r.maxW = Av1GetBits(p, o, widthBits) + 1; o += widthBits;
    r.maxHeightBitOff = o;
    r.maxH = Av1GetBits(p, o, heightBits) + 1;
    r.widthBits = widthBits;
    r.heightBits = heightBits;
    r.ok = r.maxW > 0 && r.maxH > 0;
    return r;
}

// Rewrite the max_frame size fields (same field widths, smaller-or-equal
// values) so the decoder allocates the real frame geometry.
inline bool PatchAv1SeqMax(uint8_t* p, size_t n, const Av1SeqInfo& si,
                           uint32_t newMaxW, uint32_t newMaxH) {
    if (!p || !si.ok || newMaxW == 0 || newMaxH == 0) return false;
    if (newMaxW > si.maxW || newMaxH > si.maxH) return false;
    if ((si.maxWidthBitOff + si.widthBits) > n * 8) return false;
    if ((si.maxHeightBitOff + si.heightBits) > n * 8) return false;
    Av1SetBits(p, si.maxWidthBitOff, si.widthBits, newMaxW - 1);
    Av1SetBits(p, si.maxHeightBitOff, si.heightBits, newMaxH - 1);
    return true;
}

struct Av1FrameSize {
    bool ok;
    uint32_t w, h;
};

// Locate the real frame size inside the frame-header OBU payload. The exact
// bit offset of the frame_size() fields depends on a long chain of frame
// flags; instead of re-deriving it, scan for the (width_minus_1,
// height_minus_1) pair at the sequence's own field widths, constrained to
// legal values (<= seq max, 16-aligned). The constraints isolate the pair
// uniquely on real streams (verified against ffprobe's ground truth).
inline Av1FrameSize ScanAv1FrameSize(const uint8_t* p, size_t n,
                                     uint32_t widthBits, uint32_t heightBits,
                                     uint32_t maxW, uint32_t maxH) {
    Av1FrameSize r{false, 0, 0};
    if (!p || widthBits == 0 || heightBits == 0 || widthBits > 16 || heightBits > 16) return r;
    const size_t total = n * 8;
    if (total < widthBits + heightBits) return r;
    for (size_t o = 0; o + widthBits + heightBits <= total; o++) {
        const uint32_t w = Av1GetBits(p, o, widthBits) + 1;
        if (w < 16 || w > maxW || (w & 15) != 0) continue;
        const uint32_t h = Av1GetBits(p, o + widthBits, heightBits) + 1;
        if (h < 16 || h > maxH || (h & 15) != 0) continue;
        r.ok = true; r.w = w; r.h = h;
        return r;
    }
    return r;
}

// Walk one AU's OBUs (bounds-checked, AV1 rules); report the first
// sequence-header (type 1) and frame-header (type 3) payload spans.
// Moved here from the layer TU so the host tests exercise the exact logic
// the layer runs on the first access unit.
inline bool FindAv1SeqAndFrame(const uint8_t* p, size_t n, size_t* seqOff, size_t* seqLen,
                               size_t* fhOff, size_t* fhLen) {
    if (!p || n < 2) return false;
    bool haveSeq = false, haveFh = false;
    size_t off = 0;
    while (off < n) {
        const uint8_t hb = p[off];
        if (hb & 0x80) return haveSeq && haveFh;
        const uint8_t type = (hb >> 3) & 0x0F;
        if (type == 0 || (type >= 9 && type <= 14)) return haveSeq && haveFh;
        const bool ext = ((hb >> 2) & 1) != 0;
        const bool hasSize = ((hb >> 1) & 1) != 0;
        off++;
        if (ext) { if (off + 1 >= n) return haveSeq && haveFh; off += 2; }
        uint64_t sz = 0;
        if (hasSize) {
            int shift = 0; size_t bytes = 0; bool done = false;
            while (off < n && bytes < 8) {
                const uint8_t b = p[off++]; bytes++;
                sz |= (uint64_t)(b & 0x7F) << shift; shift += 7;
                if (!(b & 0x80)) { done = true; break; }
            }
            if (!done) return haveSeq && haveFh;
        }
        if (type == 2) {
            // Sized TD (0x12 0x00): the size field is already consumed and
            // must read zero; do NOT demand an extra zero byte -- that read
            // eats the next OBU's header (the v5 live bail). Size-less TD:
            // one trailing zero byte, per the WalkAv1Obus rule.
            if (hasSize) { if (sz != 0) return haveSeq && haveFh; }
            else if (off < n && p[off] == 0) off++;
            else return haveSeq && haveFh;
            sz = 0;
        }
        else if (!hasSize) return haveSeq && haveFh;
        if ((uint64_t)off + sz > n) return haveSeq && haveFh;
        if (type == 1 && !haveSeq) { *seqOff = off; *seqLen = (size_t)sz; haveSeq = true; }
        if (type == 3 && !haveFh) { *fhOff = off; *fhLen = (size_t)sz; haveFh = true; }
        if (haveSeq && haveFh) return true;
        off += (size_t)sz;
    }
    return haveSeq && haveFh;
}

// Patch every sequence-header OBU in place (repeatSeqHdr streams carry one
// per IDR, so later AUs need it too). Returns the number patched.
inline int PatchAv1SeqHeaders(uint8_t* p, size_t n, uint32_t w, uint32_t h) {
    if (!p || n < 2) return 0;
    int patched = 0;
    size_t off = 0;
    while (off < n) {
        const uint8_t hb = p[off];
        if (hb & 0x80) return patched;
        const uint8_t type = (hb >> 3) & 0x0F;
        if (type == 0 || (type >= 9 && type <= 14)) return patched;
        const bool ext = ((hb >> 2) & 1) != 0;
        const bool hasSize = ((hb >> 1) & 1) != 0;
        off++;
        if (ext) { if (off + 1 >= n) return patched; off += 2; }
        uint64_t sz = 0;
        if (hasSize) {
            int shift = 0; size_t bytes = 0; bool done = false;
            while (off < n && bytes < 8) {
                const uint8_t b = p[off++]; bytes++;
                sz |= (uint64_t)(b & 0x7F) << shift; shift += 7;
                if (!(b & 0x80)) { done = true; break; }
            }
            if (!done) return patched;
        }
        if (type == 2) {
            // Sized TD: size already consumed, must read zero. Size-less TD:
            // one trailing zero byte. Same rule as the finder / WalkAv1Obus.
            if (hasSize) { if (sz != 0) return patched; }
            else if (off < n && p[off] == 0) off++;
            else return patched;
            sz = 0;
        }
        else if (!hasSize) return patched;
        if ((uint64_t)off + sz > n) return patched;
        if (type == 1) {
            Av1SeqInfo si = ParseAv1SeqMax(p + off, (size_t)sz);
            if (si.ok && PatchAv1SeqMax(p + off, (size_t)sz, si, w, h)) patched++;
        }
        off += (size_t)sz;
    }
    return patched;
}

}  // namespace gxr
