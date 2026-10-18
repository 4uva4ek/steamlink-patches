"""Compile the audited TSV admission data into an external native header."""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import re


def generate(profiles, output):
    rows = []
    for line in Path(profiles).read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        fields = line.split("\t")
        if len(fields) != 7 or not re.fullmatch(r"\d+\.\d+\.\d+", fields[0]) or not fields[1].isdigit():
            raise ValueError("Malformed exact-build telemetry profile")
        if not re.fullmatch(r"[0-9a-f]{64}", fields[3]) or int(fields[2]) < 64:
            raise ValueError("Invalid scene size/hash")
        anchors = []
        for item in fields[4].split(";"):
            offset, size, digest = item.split(":")
            offset, size = int(offset), int(size)
            if offset < 0 or not 0 < size <= 4096 or offset + size > int(fields[2]) or not re.fullmatch(r"[0-9a-f]{64}", digest):
                raise ValueError("Invalid telemetry anchor")
            anchors.append((offset, size, digest))
        segments = [tuple(map(int, item.split(":"))) for item in fields[6].split(";")]
        if any(len(s) != 4 or min(s) < 0 or s[1] > s[2] for s in segments):
            raise ValueError("Invalid telemetry PT_LOAD metadata")
        if not any(s[0] == 0 and s[3] & 5 == 5 for s in segments):
            raise ValueError("Only audited zero-based executable layouts are supported")
        if len(anchors) != 8 or len(fields[5].split(";")) != 8:
            raise ValueError("All eight MediaCodec API paths must be audited")
        if any(not any(s[3] & 5 == 5 and s[0] <= a[0] and a[0] + a[1] <= s[0] + s[1] for s in segments) for a in anchors):
            raise ValueError("Telemetry anchor lies outside readable executable file mapping")
        rows.append((fields, anchors, segments))
    pairs = [(f[0], f[1]) for f, _, _ in rows]
    if not rows or len(set(pairs)) != len(pairs):
        raise ValueError("Empty/duplicate telemetry profiles")
    lines = ["// Generated from the canonical audited telemetry profiles; do not edit.",
             "#pragma once", "#include <cstddef>", "#include <cstdint>",
             "namespace gxr::telemetry_profiles {",
             "struct Segment { uintptr_t virtualAddress; size_t fileSize; size_t memorySize; uint32_t flags; };",
             "struct Anchor { uintptr_t virtualAddress; size_t size; const char* sha256; };",
             "struct Profile { const char* versionName; uint64_t versionCode; size_t fileSize; const Segment* segments; size_t segmentCount; const Anchor* anchors; size_t anchorCount; };"]
    for index, (_, anchors, segments) in enumerate(rows):
        lines.append(f"inline constexpr Segment kSegments{index}[] = {{")
        lines += ["    {" + ", ".join(str(value) + "u" for value in s) + "}," for s in segments]
        lines.append("};")
        lines.append(f"inline constexpr Anchor kAnchors{index}[] = {{")
        lines += [f'    {{{off}u, {size}u, "{digest}"}},' for off, size, digest in anchors]
        lines.append("};")
    lines.append("inline constexpr Profile kProfiles[] = {")
    for index, (fields, anchors, segments) in enumerate(rows):
        lines.append(f'    {{"{fields[0]}", {fields[1]}u, {fields[2]}u, kSegments{index}, {len(segments)}u, kAnchors{index}, {len(anchors)}u}},')
    lines += ["};", "inline constexpr size_t kProfileCount = sizeof(kProfiles) / sizeof(kProfiles[0]);", "}"]
    output = Path(output).resolve()
    repo = Path(__file__).resolve().parents[3]
    if output.is_relative_to(repo):
        raise ValueError("Generated headers must stay outside the source checkout")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Generated {len(rows)} exact-build profiles; input SHA256 {hashlib.sha256(Path(profiles).read_bytes()).hexdigest()}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profiles", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    generate(args.profiles, args.output)
