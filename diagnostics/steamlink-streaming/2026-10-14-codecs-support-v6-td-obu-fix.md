# 2026-10-14 — Codecs Support v6: AV1 AU walk past the sized TD OBU (geometry fix live)

## Symptom (v5 build; live rounds 16:53 and 17:08)
AV1 stream decodes at a steady ~90 fps but the headset renders the transport
frame as misaligned square patches (dominant flat-green region ~3/4 of the
view, displaced floor/window/dashboard segments elsewhere). Host side clean:
tap swap HEVC->AV1 accepted, SPLIT live, 10-bit mode; the host's own bitstream
decodes (libdav1d) as valid AV1 Main yuv420p10le 1536x6144, tiles 2x1.

## Root cause
- The AV1 sequence header keeps the encoder's init-time maximum (3200x8192)
  while the real frames are 1536x6144 (HEVC rewrites its SPS on reconfigure;
  AV1 does not). The QCOM decoder then allocates at the max and crops; the
  client's tile placement math expects buffer == frame -> misaligned tiles.
- v5 added the fix for exactly this (patch the seq max down + real-size hints)
  but its worker walkers `FindAv1SeqAndFrame` / `PatchAv1SeqHeaders` mishandled
  the sized temporal-delimiter OBU (0x12 0x00): after consuming the TD's
  LEB128 size byte they demanded an extra 0x00 and bailed on the next OBU's
  0x0a. Runtime proof: `geo: no sequence/frame-header OBUs in the first AU`
  on every session, while the same session's OBU diagnostics walk found all
  four OBUs cleanly.

## Fix (commit 2eaa85c, branch dev; release v1.28.0-dev.29)
- Both walkers moved verbatim to `av1_geometry.h` (namespace gxr) so the host
  tests cover them; TD branch aligned with the tested `WalkAv1Obus` rule
  (sized TD: require zero size, consume nothing extra; size-less TD: one
  trailing zero byte).
- `kBuild` bumped to `codecs-support-v6-20261014`; `.so` rebuilt + staged
  (sha256 `ac30d452d0ac86de5f97f66d8582f5343d2e53ed326a2039bf56b5dccfda3a08`),
  Kotlin pin updated.
- Regression vector: the real 303-byte first AU from the NVENC tap AU dump
  (OBU spans TD 2B / seq 12B / frame-hdr 33B / tile-group 247B).
  RED before the fix: 54 checks / 6 failures; GREEN after: 54 / 0.

## Verification
- Host tests 54/0 (RED->GREEN, tests/Run-Tests.ps1); `:patches:test` 189
  tests / 0 failures / 0 errors / 13 skipped (assumption-gated decoded-base
  audits); @reviewer diff pass: SHIP, zero findings (independent
  re-verification of the semantics, the vector, the .so build string, the pin).
- Live round (17:22, v6 client on 2.0.20/5001712): `geo: seq max=3200x8192
  bits=12/14 frame=1536x6144; patching seq max + hints`; swap hints
  width=1536 height=6144; `geo: patched 1 sequence header(s) this AU` for
  later AUs; decoder output allocation 1350 MB -> 486 MB (no longer sized at
  the max); av1D ~90 fps; headset screenshots fully aligned (see round-fixed).
- Evidence: `builds/steamlink-patches/build/codec-verify-20261014/`
  (`round-baseline/`, `round-fixed/`: screenshots, logcats, AU dumps,
  FINDINGS notes, review findings).

## Note
Unrelated to the misalignment: the QC2Comp `Stream:` fps/Kbps fields read
garbage (`1000000.00fps ...Mbps`) for the av01 decoder in every session;
Work-Rate (90/s) is real. Cosmetic so far; keep an eye on it if display-side
timing questions come up.

## Refinement idea (not implemented)
The host tap could patch the sequence-header max down at LockBitstream time,
making the stream self-consistent for any decoder. The client layer owns this
today; revisit only if a stock (unpatched) client ever needs the AV1 path.

## HEVC sanity (same day, ~17:24)
Driver back on codec 0: the tap init runs `codec=HEVC` with no codec swap (its
usual preset/level/split tuning only); the v6 layer logs `first AU reads as
HEVC; stock HEVC path kept` (transparent on HEVC); `c2.qti.hevc.decoder`
decodes at ~90 fps; stream rendered, screenshots aligned. The codec-selection
feature does not affect the stock path.
