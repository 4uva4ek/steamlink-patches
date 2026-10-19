// Pure-logic tests for the Codecs Support layer: SHA-256 vectors, the
// access-unit sniff predicate, and the MediaFormat recipe MIME substitution.
// Compiled and run on the host; no Android dependencies.
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <initializer_list>
#include <string>
#include <vector>

#include "../src/codec_sniff.h"
#include "../src/format_recipe.h"
#include "../src/av1_geometry.h"
#include "../src/sha256.h"

static int g_failures = 0;
static int g_checks = 0;

#define CHECK(cond, label)                                              \
    do {                                                                \
        g_checks++;                                                     \
        if (!(cond)) {                                                  \
            g_failures++;                                               \
            std::printf("FAIL: %s (line %d)\n", label, __LINE__);       \
        }                                                               \
    } while (0)

static void CheckSha() {
    uint8_t d[32];
    char hex[65];
    gxr::Sha256::Digest("", 0, d);
    gxr::Sha256::Hex(d, hex);
    CHECK(std::strcmp(hex, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855") == 0,
          "sha256 empty vector");
    gxr::Sha256::Digest("abc", 3, d);
    gxr::Sha256::Hex(d, hex);
    CHECK(std::strcmp(hex, "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad") == 0,
          "sha256 abc vector");
    const char* longInput =
        "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq";
    gxr::Sha256::Digest(longInput, std::strlen(longInput), d);
    gxr::Sha256::Hex(d, hex);
    CHECK(std::strcmp(hex, "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1") == 0,
          "sha256 two-block vector");
    // split update must equal one-shot
    gxr::Sha256 s;
    for (int i = 0; i < 1000000; i++) s.Update("a", 1);
    s.Final(d);
    gxr::Sha256::Hex(d, hex);
    CHECK(std::strcmp(hex, "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0") == 0,
          "sha256 million-a vector");
}

static gxr::Sniffed SniffVec(const std::vector<uint8_t>& v) {
    return gxr::SniffAccessUnit(v.data(), v.size());
}

static void CheckSniff() {
    using gxr::Sniffed;
    auto vec = [](std::initializer_list<int> bytes) {
        std::vector<uint8_t> v;
        for (int b : bytes) v.push_back(static_cast<uint8_t>(b));
        return v;
    };
    // HEVC: VPS (0x40 0x01), SPS, IDR slice (0x26 0x01), TRAIL_R (0x02 0x01)
    CHECK(SniffVec(vec({0x00, 0x00, 0x00, 0x01, 0x40, 0x01, 0x0c})) == Sniffed::KeepHevc,
          "hevc vps 4-byte sc");
    CHECK(SniffVec(vec({0x00, 0x00, 0x01, 0x42, 0x01, 0x01})) == Sniffed::KeepHevc,
          "hevc sps 3-byte sc");
    CHECK(SniffVec(vec({0x00, 0x00, 0x00, 0x01, 0x26, 0x01, 0xaf})) == Sniffed::KeepHevc,
          "hevc idr");
    CHECK(SniffVec(vec({0x00, 0x00, 0x00, 0x01, 0x02, 0x01})) == Sniffed::KeepHevc,
          "hevc trail slice");
    // H264: SPS (0x67), PPS (0x68), IDR (0x65), SEI (0x06), AUD (0x09)
    CHECK(SniffVec(vec({0x00, 0x00, 0x00, 0x01, 0x67, 0x42, 0xc0})) == Sniffed::H264, "h264 sps");
    CHECK(SniffVec(vec({0x00, 0x00, 0x01, 0x68, 0xce})) == Sniffed::H264, "h264 pps 3-byte sc");
    CHECK(SniffVec(vec({0x00, 0x00, 0x00, 0x01, 0x65, 0x88})) == Sniffed::H264, "h264 idr");
    CHECK(SniffVec(vec({0x00, 0x00, 0x00, 0x01, 0x06, 0x05})) == Sniffed::H264, "h264 sei");
    CHECK(SniffVec(vec({0x00, 0x00, 0x00, 0x01, 0x09, 0xf0})) == Sniffed::H264, "h264 aud");
    // AV1: TD OBU (0x12 0x00), seq-header OBU (0x0a 0x0b ...), Annex-B TD (00 00 01 10)
    CHECK(SniffVec(vec({0x12, 0x00, 0x0a, 0x0b, 0x00})) == Sniffed::AV1, "av1 td obu");
    CHECK(SniffVec(vec({0x0a, 0x0b, 0x00, 0x00, 0x00})) == Sniffed::AV1, "av1 seq header obu");
    CHECK(SniffVec(vec({0x00, 0x00, 0x01, 0x10, 0x00})) == Sniffed::AV1, "av1 annexb td");
    // Ambiguity / refusals: 4-byte length prefix, empty, single byte
    CHECK(SniffVec(vec({0x00, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00})) == Sniffed::Unknown ||
              SniffVec(vec({0x00, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00})) == Sniffed::KeepHevc,
          "ambiguous start-code byte never swaps");
    CHECK(SniffVec(vec({0x00, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00})) != Sniffed::H264 &&
              SniffVec(vec({0x00, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00})) != Sniffed::AV1,
          "ambiguous start-code byte stays stock");
    std::vector<uint8_t> lengthPrefixed = {0x00, 0x00, 0x00, 0x08, 0x26, 0x01, 0x00, 0x00,
                                           0x00, 0x00, 0x00, 0x00};
    CHECK(SniffVec(lengthPrefixed) == Sniffed::Unknown, "length-prefixed frame is unknown");
    CHECK(gxr::SniffAccessUnit(nullptr, 0) == Sniffed::KeepHevc, "null buffer keeps stock");
    CHECK(SniffVec(vec({0x12})) == Sniffed::KeepHevc, "one byte keeps stock");
    // 0x12 with high bit set on the second byte is NOT the AV1 TD shape
    CHECK(SniffVec(vec({0x12, 0x80, 0x00})) != Sniffed::AV1, "0x12 0x80 not av1");
}

static void CheckRecipe() {
    using gxr::FormatEntry;
    std::vector<FormatEntry> recipe = {
        {true, "mime", "video/hevc", 0},
        {false, "width", "", 3584},
        {false, "height", "", 1536},
        {true, "vendor.test", "value", 0},
    };
    CHECK(gxr::RecipeHasMime(recipe), "recipe has mime");
    auto h264 = gxr::SubstituteMime(recipe, "video/avc");
    CHECK(h264.size() == recipe.size(), "substitution keeps entry count");
    CHECK(h264[0].svalue == "video/avc", "mime substituted");
    CHECK(h264[1].ivalue == 3584 && !h264[1].isString, "int entries survive");
    CHECK(h264[3].svalue == "value", "non-mime string survives");
    CHECK(recipe[0].svalue == "video/hevc", "original recipe untouched");
    std::vector<FormatEntry> noMime = {{false, "width", "", 8}};
    CHECK(!gxr::RecipeHasMime(noMime), "recipe without mime detected");
    CHECK(std::string(gxr::RecipeMime(recipe)) == "video/hevc", "recipe mime recovered for rebuild");
    CHECK(gxr::RecipeMime(noMime) == nullptr, "recipe without mime yields null rebuild mime");
    std::vector<FormatEntry> full = {
        {true, "mime", "video/hevc", 0},
        {false, "width", "", 896},
        {false, "height", "", 3584},
        {true, "color-standard", "", 6},
        {true, "low-latency", "", 1},
        {false, "operating-rate", "", 32767},
        {true, "vendor.qti-ext-dec-low-latency.enable", "", 1},
        {true, "vendor.qti-ext-dec-picture-order.enable", "", 1},
        {true, "vendor.qti-ext-dec-heif-mode.value", "", 1},
        {false, "priority", "", 0},
    };
    auto filtered = gxr::FilterCodecSwapRecipe(full);
    CHECK(filtered.size() == 4, "swap filter drops exactly the QTI-era hints");
    bool hasVendor = false, hasLl = false, hasRate = false, hasPrio = false, keptSize = false;
    for (const auto& e : filtered) {
        if (e.key.rfind("vendor.", 0) == 0) hasVendor = true;
        if (e.key == "low-latency") hasLl = true;
        if (e.key == "operating-rate") hasRate = true;
        if (e.key == "priority") hasPrio = true;
        if (e.key == "width" && e.ivalue == 896) keptSize = true;
    }
    CHECK(!hasVendor && !hasLl && !hasRate && !hasPrio && keptSize,
          "swap filter keeps mime/size/color, drops vendor/low-latency/rate/priority");
    auto swappable = gxr::SubstituteMime(gxr::FilterCodecSwapRecipe(full), "video/av01");
    CHECK(swappable.size() == 4 && swappable[0].svalue == "video/av01",
          "filtered recipe still substitutes the mime");
}

static void CheckObuWalk() {
    using gxr::WalkAv1Obus;
    // valid unit: TD, sequence header, TD, frame header, tile group
    const std::vector<uint8_t> ok = {
        0x12, 0x00,                          // TD
        0x0A, 0x04, 0x41, 0x42, 0x43, 0x44,  // sequence header (type 1), 4 payload bytes
        0x12, 0x00,                          // TD
        0x1A, 0x02, 0x11, 0x22,              // frame header (type 3), 2 payload bytes
        0x22, 0x03, 0x01, 0x02, 0x03,        // tile group (type 4), 3 payload bytes
    };
    auto r1 = WalkAv1Obus(ok.data(), ok.size(), nullptr, nullptr);
    CHECK(r1.obus == 5 && r1.failOffset == 0, "obu walk: clean unit walks 5 OBUs");
    // truncated payload: sequence header claims 100 bytes, only 1 present
    const std::vector<uint8_t> trunc = { 0x12, 0x00, 0x0A, 0x64, 0x41 };
    auto r2 = WalkAv1Obus(trunc.data(), trunc.size(), nullptr, nullptr);
    CHECK(r2.obus == 1 && r2.failOffset == 2 && std::string(r2.failReason) == "payload truncated",
          "obu walk: truncated payload reported");
    // forbidden bit on the third OBU header
    const std::vector<uint8_t> bad = { 0x12, 0x00, 0x0A, 0x04, 0x41, 0x42, 0x43, 0x44, 0x8A, 0x02, 0x11, 0x22 };
    auto r3 = WalkAv1Obus(bad.data(), bad.size(), nullptr, nullptr);
    CHECK(r3.obus == 2 && r3.failOffset == 8 && std::string(r3.failReason) == "forbidden bit set",
          "obu walk: forbidden bit reported");
    // unterminated LEB128 size
    const std::vector<uint8_t> leb = { 0x12, 0x00, 0x0A, 0x80 };
    auto r4 = WalkAv1Obus(leb.data(), leb.size(), nullptr, nullptr);
    CHECK(r4.failOffset == 2 && std::string(r4.failReason) == "unterminated LEB128 size",
          "obu walk: unterminated size reported");
}

static void CheckAv1Geometry() {
    using gxr::ParseAv1SeqMax;
    using gxr::ScanAv1FrameSize;
    using gxr::PatchAv1SeqMax;
    // Captured from the live NVENC AV1 stream; ffprobe ground truth:
    // max 3200x8192, frame 1536x6144, frame_width_bits 12 / height_bits 14.
    std::vector<uint8_t> seq = {0x00, 0x00, 0x00, 0x96, 0xf7, 0x1f, 0xdf, 0xff, 0x00, 0x86, 0x70, 0x10};
    auto si = ParseAv1SeqMax(seq.data(), seq.size());
    CHECK(si.ok && si.maxW == 3200 && si.maxH == 8192 && si.widthBits == 12 && si.heightBits == 14,
          "seq geometry: max 3200x8192 with 12/14-bit size fields");
    std::vector<uint8_t> fh = {0x14, 0x02, 0xff, 0xaf, 0xfe, 0x3f, 0x3f, 0xf6, 0x3e, 0x10, 0x06, 0x0e,
                               0x0e, 0x0e, 0x08, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x82, 0x08,
                               0x20, 0x20, 0x00, 0x20, 0x04, 0x00, 0x90, 0x0c, 0x14};
    auto fs = ScanAv1FrameSize(fh.data(), fh.size(), si.widthBits, si.heightBits, si.maxW, si.maxH);
    CHECK(fs.ok && fs.w == 1536 && fs.h == 6144, "frame geometry: scan finds 1536x6144");
    std::vector<uint8_t> patched = seq;
    CHECK(PatchAv1SeqMax(patched.data(), patched.size(), si, fs.w, fs.h), "seq patch applies");
    auto si2 = ParseAv1SeqMax(patched.data(), patched.size());
    CHECK(si2.ok && si2.maxW == 1536 && si2.maxH == 6144 && si2.widthBits == 12 && si2.heightBits == 14,
          "seq patch round-trips to 1536x6144");
    CHECK(patched.size() == seq.size(), "seq patch keeps the OBU size");
    CHECK(PatchAv1SeqMax(patched.data(), patched.size(), si2, si.maxW, si.maxH) == false,
          "seq patch refuses to grow the max");
    std::vector<uint8_t> junk(16, 0x00);
    auto nofs = ScanAv1FrameSize(junk.data(), junk.size(), 12, 14, 3200, 8192);
    CHECK(!nofs.ok, "frame scan rejects an all-zero payload");
}

static void CheckAv1AuWalk() {
    using gxr::FindAv1SeqAndFrame;
    using gxr::PatchAv1SeqHeaders;
    using gxr::ParseAv1SeqMax;
    using gxr::WalkAv1Obus;
    // The live NVENC AV1 stream's first access unit (303 bytes, captured
    // from the tap's AU dump; head matches the layer's runtime AU1 log).
    // TD(2) + seq header(12B payload) + frame header(33B) + tile group(247B).
    std::vector<uint8_t> au = {
        0x12,0x00,0x0a,0x0c,0x00,0x00,0x00,0x96,0xf7,0x1f,0xdf,0xff,0x00,0x86,0x70,0x10,
        0x1a,0x21,0x14,0x02,0xff,0xaf,0xfe,0x3f,0x3f,0xf6,0x3e,0x10,0x06,0x0e,0x0e,0x0e,
        0x08,0x02,0x00,0x00,0x00,0x00,0x00,0x80,0x82,0x08,0x20,0x20,0x00,0x20,0x04,0x00,
        0x90,0x0c,0x14,0x22,0xf7,0x81,0x80,0x00,0x00,0x79,0x00,0x00,0x00,0x01,0x73,0x1b,
        0xaf,0x2f,0xd9,0x6f,0xb8,0xe0,0xa0,0xc8,0x99,0xdf,0x77,0xb6,0xb4,0x5a,0xfe,0xb8,
        0x1d,0x9a,0x04,0x3d,0x24,0xbb,0x1c,0xd1,0x0f,0x1d,0x5d,0x29,0x8e,0xc0,0xa4,0xd0,
        0x1e,0x6b,0xed,0xbe,0x3b,0x88,0xa3,0xe6,0x90,0x0b,0xfd,0x84,0xb2,0xdd,0x90,0xb5,
        0xba,0xad,0x84,0x54,0x6c,0x80,0x18,0x64,0x1d,0xd1,0xb2,0x20,0x8b,0x6b,0x60,0x26,
        0x3d,0x8f,0x7e,0x19,0x92,0xeb,0x34,0x8f,0x7b,0x8b,0x06,0x21,0x8d,0x5a,0x54,0xf1,
        0xcb,0x51,0x7d,0x94,0xef,0x74,0xb9,0x73,0x9b,0xf8,0xf7,0xfb,0x56,0x26,0x2f,0x2b,
        0xe1,0x99,0xe4,0xb9,0x80,0xaa,0x79,0xbc,0xe8,0xc4,0x1e,0x70,0xc8,0x8a,0xca,0xe1,
        0x88,0xe5,0xab,0xfd,0x63,0xc8,0x0b,0x0e,0xe0,0x7a,0xaa,0xc9,0x1a,0xab,0x87,0xee,
        0x41,0x90,0xba,0xa0,0x83,0x6b,0x60,0x54,0xbd,0x3d,0x43,0x42,0x95,0x51,0x1c,0xed,
        0x0d,0xe1,0x6a,0xd7,0x91,0x15,0xd7,0xb5,0x2b,0x41,0xdd,0xb2,0xfd,0xb1,0x46,0x2f,
        0x7d,0xde,0x85,0x2c,0x01,0x33,0x3a,0xac,0xef,0x6f,0xc1,0x4f,0x8e,0xeb,0x78,0x23,
        0xbf,0x32,0xc0,0x24,0x54,0xd5,0x3a,0xb3,0x79,0x77,0x27,0xd8,0x31,0x21,0xd2,0x1e,
        0x23,0xbe,0x47,0xb0,0xa5,0x26,0xf2,0xa4,0xc0,0x72,0xc1,0x72,0x0f,0x73,0x63,0xa0,
        0x67,0xb9,0xf3,0x1a,0x03,0x2d,0x52,0x52,0x6d,0x15,0x87,0x1a,0x92,0x46,0x99,0x71,
        0x2e,0x22,0xc4,0x0a,0xf2,0xea,0x85,0xae,0x47,0xf5,0x3e,0x33,0xd1,0x40,0xfa,
    };
    CHECK(au.size() == 303, "au walk: vector is the captured 303-byte first AU");
    size_t so = 0, sl = 0, fo = 0, fl = 0;
    // Guards the v5 failure: with the TD branch demanding an extra zero byte,
    // the finder bailed at OBU0 and the layer logged "geo: no sequence/
    // frame-header OBUs in the first AU" on every live session (2026-10-14).
    CHECK(FindAv1SeqAndFrame(au.data(), au.size(), &so, &sl, &fo, &fl),
          "au walk: sized TD does not stop the seq/frame-header finder");
    CHECK(sl == 12 && fl == 33, "au walk: finds the 12B seq and 33B frame-header payloads");
    auto si = ParseAv1SeqMax(au.data() + so, sl);
    CHECK(si.ok && si.maxW == 3200 && si.maxH == 8192,
          "au walk: seq carries the 3200x8192 init maximum");
    auto walk = WalkAv1Obus(au.data(), au.size(), nullptr, nullptr);
    CHECK(walk.obus == 4 && walk.failOffset == 0,
          "au walk: diagnostic OBU walk stays clean on the same AU");
    std::vector<uint8_t> patched = au;
    CHECK(PatchAv1SeqHeaders(patched.data(), patched.size(), 1536, 6144) == 1,
          "au patch: exactly one sequence header patched");
    size_t so2 = 0, sl2 = 0, fo2 = 0, fl2 = 0;
    CHECK(FindAv1SeqAndFrame(patched.data(), patched.size(), &so2, &sl2, &fo2, &fl2) && sl2 == 12,
          "au patch: the finder still walks the patched AU");
    auto si2 = ParseAv1SeqMax(patched.data() + so2, sl2);
    CHECK(si2.ok && si2.maxW == 1536 && si2.maxH == 6144,
          "au patch: seq max is now the real frame size");
    CHECK(patched.size() == au.size(), "au patch: AU size unchanged");
}

int main() {
    CheckSha();
    CheckSniff();
    CheckRecipe();
    CheckObuWalk();
    CheckAv1Geometry();
    CheckAv1AuWalk();
    std::printf("codecs-support pure logic: %d checks, %d failures\n", g_checks, g_failures);
    return g_failures == 0 ? 0 : 1;
}
