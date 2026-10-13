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
}

int main() {
    CheckSha();
    CheckSniff();
    CheckRecipe();
    std::printf("codecs-support pure logic: %d checks, %d failures\n", g_checks, g_failures);
    return g_failures == 0 ? 0 : 1;
}
