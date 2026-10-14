// Recorded MediaFormat recipe: the layer captures every AMEDIAFORMAT_SET_*
// call the scene library makes while building the decoder format, then
// replays it onto the sniffed codec's decoder at first-access-unit swap
// time with the MIME entry substituted. Pure logic; tested.
#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace gxr {

struct FormatEntry {
    bool isString;
    std::string key;
    std::string svalue;   // when isString
    int32_t ivalue;       // when !isString
};

inline std::vector<FormatEntry> SubstituteMime(const std::vector<FormatEntry>& in, const char* mime) {
    std::vector<FormatEntry> out = in;
    for (FormatEntry& e : out) {
        if (e.isString && e.key == "mime") e.svalue = mime;
    }
    return out;
}

inline bool RecipeHasMime(const std::vector<FormatEntry>& in) {
    for (const FormatEntry& e : in) {
        if (e.isString && e.key == "mime") return true;
    }
    return false;
}

// The recipe's own mime value (e.g. "video/hevc"), used when a failed swap
// must rebuild the stock decoder. Returns nullptr when no mime entry exists.
inline const char* RecipeMime(const std::vector<FormatEntry>& in) {
    for (const FormatEntry& e : in) {
        if (e.isString && e.key == "mime") return e.svalue.c_str();
    }
    return nullptr;
}

}  // namespace gxr
