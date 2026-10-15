# H.264 over NVENC / SteamVR vrlink — investigation handoff (2026-10-15)

**Audience:** the next technician picking up H.264 support for the GalaxyXR driver's NVENC tap.
**State at handoff:** H.264 sessions init, apply the full profile, and vrlink goes through its
start sequence — but the **first real `nvEncEncodePicture` on an H.264 session fails with
`NV_ENC_ERR_INVALID_PARAM` (status 14)**; no bitstream ever locks; vrlink tears the stream down
("Frame 0 not in expected state", fatal after 2 s). HEVC and AV1 stream normally on the same build.
**Everything below is reproduced from live logs; every claim has a signature to re-verify.**

---

## 1. Deliverable state (what is installed, where the code is)

| Item | Value |
| --- | --- |
| Installed driver DLL | `C:/Users/Angelo/AppData/Local/GalaxyXR/Drivers/GalaxyXRNative-1791996853369019400/bin/win64/driver_GalaxyXRNative.dll` |
| Installed DLL sha256 | `a4953850de9eed8d520663415b26951877981a4b69e2bcd1d6d23635ab065ddb` (package `GalaxyXRDriver-Test-20261015-141140`) |
| Source diff (final) | `builds/steamlink-patches/build/codec-verify-20261014/round-600h264/fix-diff-v3b.patch` (603 lines) |
| Rollback DLLs (round dir) | `.pre-fix-rollback` (`d0a6d7f5…`), `.r2-prev-rollback` … `.r8-prev-rollback` — one per round |
| Driver settings in test | `settings.json`: `nvencCodec: 1` (H.264), `customBandwidthMbit: 350`, `customStreamFormatWidth: 1536` (driver auto-caps to 1024 for H.264) |
| Test gate | `tools/Test-NvencToggle.ps1` → **152 checks, 0 failures** |
| Build gate | `tools/Build-Portable.ps1 -DriverOnly` exit 0 |

**Verification discipline that must be kept:** never edit `settings.json` or hot-swap the DLL while
SteamVR is up (a live settings edit wedges reconfigure into a status-8 loop; a swap while
`vrmonitor.exe` lingers is silently deferred — always confirm **vrserver + vrmonitor both gone**).

---

## 2. Architecture in one paragraph

`driver_vrlink` (Valve) creates NVENC sessions for a 10-bit HEVC 1536x6144 (tile-driven) stream.
The GalaxyXR driver's **NvencTap** (`GalaxyXRDriver/src/Driver/NvencTap.cpp`) hooks
`NvEncodeAPICreateInstance` in vrserver and rewrites NVENC calls at the boundary: caps answers,
`nvEncInitializeEncoder`/`ReconfigureEncoder` (codec/profile/rc/field overrides with
snapshot→restore symmetry), `nvEncEncodePicture` (11.1→12.1 struct retag), `LockBitstream`, etc.
For H.264 (`cfg.codec==1`) the tap swaps vrlink's HEVC session to H.264 **in place** and overlays
the H.264 profile. SteamFrame-Advanced-Options (github.com/elliotttate/SteamFrame-Advanced-Options)
solves the same family of problems on the same driver family by **owning session creation** — its
`Native/AVLab/legacy_api_bridge.cpp` is the reference implementation for every field decision below.
Reference clone: `builds/steamframe-ao-study/repo1` @ `c1e173e`.

---

## 3. Eliminations — everything tried, with evidence

| # | Hypothesis | Change made | Result / evidence |
| --- | --- | --- | --- |
| 1 | Split bits forced on H.264 (API: "Split Encoding is not applicable to H264") | Skip split for `codec==1` (`NvencTap.cpp` split branch + one-shot log) | Old: `split-frame: mode 1 state REJECTED applied 0 (status 8)` → now `state untried applied 0 (status 0)`. Rejection gone. |
| 2 | 10-bit class race (vrlink probes `SUPPORT_10BIT_ENCODE` before the driver config publishes → H.264 forced onto a 10-bit session) | Caps shim answers `SUPPORT_10BIT_ENCODE=0` + `WIDTH_MAX=1024`/`HEIGHT_MAX=4096` for H.264, with a one-time settings-file fallback pre-publish (`cfgPublished` discriminator) | `vrlink: Using 10bit mode: 1` → **`0`**. Verified in vrlink's own log. |
| 3 | Frame geometry > 4096/side | H.264 auto tile cap (1024 → frame 1024x4096) + maxEncode clamp 4096 with snapshot/restore | Session + encode consistently at 1024x4096; the old `NVENC: Frame Dimension greater than the maximum supported value` never reappears. |
| 4 | `multipass=1` raised on a ULL session by the preset merge | Pinned `multiPass=0` for H.264 in init + reconfigure (`multipass 1->0 [h264]` fragment) | Fragment fires; **hang identical** → cleared. |
| 5 | Bitrate too high (SteamFrame reports: clean at 225 Mbps, stall at 500) | 600 → **350** Mbps, settings only | **Identical failure at 350** → bitrate cleared. |
| 6 | GPU driver TDR masquerading as hang | — | No `nvlddmkm`/`Display`/`dxgkrnl` events in Windows System log over the whole window → cleared. |
| 7 | Our foveation QP map sized for HEVC (32x32) attached to H.264 | — | Feature OFF in this setup (no `qp` keys in settings, "QP delta map rebuilt" never logged) → our map cleared. **See §5 for the caller's map.** |
| 8 | Blind 11.1→12.1 `NV_ENC_PIC_PARAMS` copy (3344 bytes verbatim) | Replaced by explicit field copy `CopyPicParams111To121` (`NvencTap.cpp:233-254`), reading 11.1 offsets (qpDeltaMap@1696/1704), writing modern members (@1712/1720), static_asserts + compiled geometry probe | **Failure mode changed from WEDGE to clean rejection.** Pre-fix: one encode entry, call never returns, no retries, vrlink timeout in ~50 ms. Post-fix: encodes **return** `NV_ENC_ERR_INVALID_PARAM`, are retried, counted (`encode 7 / lock 0 / encFail 6`). |
| 9 | Full SteamFrame H.264 profile missing | v2+v3: keep vrlink RC mode (VBR, `rc … left [h264]`), `gop=idr=fps(90)`, 1-frame VBV, P2+ULL, multipass 0, CAVLC >200 Mbps, refs=3 + VUI `bitstreamRestrictionFlag=1`, `outputAUD=1`, `repeatSPSPPS=1`/`disableSPSPPS=0`, `sliceMode=3/sliceModeData=1`, `vbvInitialDelay=0`, filler off | All fragments fire (`init 1 (ovr 1, … fail 0)`), **encode still fails status 14** → none of these individually unblocks it (but they remain required profile work). |

Also eliminated earlier: input format (`fmt=0x1` = NV12, 8-bit, matches session), first-frame flags
(`0x7` = FORCEINTRA|FORCEIDR|OUTPUT_SPSPPS — normal), registered input resources 1024x4096, QP-map
of the tap (never built), and the client side (Steam Link app: `hevcD` idle at 0 Kbps = zero video
bytes arrive; nothing to fix client-side until the host produces frames).

---

## 4. The current failure, precisely

Final heartbeat (rotation-proof, printed every 10 s while vrserver lives):

```
heartbeat api=12 enabled=1 1024x4096 avg=350M max=350M level=0 tier=0 bitDepth=0 |
init 1 (ovr 1, ovrFail->ok 0, fail 0) | reconf 7 (ovr 7, ovrFail->ok 0, fail 0, stockFail 0) |
register 12 encode 7 lock 0 (last 0 bytes) encFail 6 | lastEncFail 14 ""
```

- `lastEncFail 14` = **NV_ENC_ERR_INVALID_PARAM**; the empty `"%s"` is normal (the driver returns
  no string for param validation rejects — do not read the emptiness as "no error").
- `lock 0` = no bitstream was ever produced; `encFail 6` of 7 encode calls (the 7th = EOS flush, null buffers).
- vrlink side: `Timed out waiting for another accepted video packet` → `HandleUnrecoverableError(1)`
  → `Frame 0 not in expected state … Waited 2 seconds` → fatal → server exit. The "Frame 0"
  flood line rate is ~1k lines/s — detail lines rotate out of the 10 MB vrserver.txt in ~1-2 min
  (why the heartbeat now carries the reason).

---

## 5. Prime diagnosis + the fix to try first

**Prime suspect: vrlink's own per-frame `qpDeltaMap`.** vrlink (as the session owner) fills
`NV_ENC_PIC_PARAMS.qpDeltaMap` for its HEVC session on a **32x32-block grid**. H.264's encoder
requires a **16x16-block grid** (= 4× the entries for the same frame; 1024x4096: HEVC-sized =
32x128 = 4096 entries, H.264 needs 64x256 = 16384). An 11.1 caller map delivered at the correct
12.1 offsets (as the tap does since fix #8) is a **size mismatch the driver rejects with exactly
`NV_ENC_ERR_INVALID_PARAM`** — the first encode, every encode, deterministic. This is the only
per-call field that today, for the first time, reaches the driver carrying a foreign value
(dims/format/flags/bitstreams/events are all verified consistent with the session).

Why the day-long mask: the old blind copy parked the caller's map pointer into 12.1's
`reserved2[0]` (must be NULL) — that violation wedged the call (never returned). Once the copy
was corrected, the driver simply validated the (wrongly sized) map and rejected it cleanly.

**Verification step (one build, one round):** in `EncodePicture`, for `codec==1`, log the incoming
`params->qpDeltaMap`/`qpDeltaMapSize` once (or into the heartbeat): expect non-null + size 4096
at 1024x4096 for a 32x32 grid. If so, the fix is SteamFrame's (`legacy_api_bridge.cpp:232-253`):

- For `codec==1`, when the caller map is non-null: **resize 32x32 → 16x16** by replicating each
  block 2×2, clamp entries to [-51, 51], keep any timing-tile convention, set
  `qpDeltaMapSize = MB count` (64x256), and pass the resampled map in the 12.1 call.
- If the caller map is NULL after all: proceed to §6.

**Regression warning:** the corrected explicit copy now delivers the caller map to HEVC/AV1 too.
Re-run the AV1 and HEVC sanity rounds on `a4953850` **before** trusting them (if AV1 rejects a
32x32 map where it expects 64x64 superblocks, the same resampling idea applies per codec —
`legacy_api_bridge.cpp` rebuilds maps for AV1 as well).

---

## 6. If the map is not it (ordered next steps)

1. **Bisect the v3 profile fields** (they went in as one batch; INVALID_PARAM can only come from
   per-call fields, so first re-check dims (`inputWidth/Height` vs session), `bufferFmt`, pitch,
   resource registration — all currently consistent). If everything per-call checks out, suspect
   session/call cross-validation: re-test with the v3 field set removed one at a time —
   `entropyCodingMode` (CABAC vs CAVLC), `sliceMode 3/1`, `outputAUD`, `refs=3 + VUI`, `gop/idr=90`.
2. **Build the H.264 session from the canonical baseline wholesale** (SteamFrame's true architecture):
   instead of mutating vrlink's HEVC-shaped config, construct the `NV_ENC_CONFIG` for
   `[H264, P2, ULL]` from `nvEncGetEncodePresetConfigEx` output and apply only the proven fields —
   eliminates any residual vrlink-shaped field the per-field overrides don't reach.
3. **Tuning test**: LL tuning instead of ULL (one variable) — ULL + H.264 remains the least-exercised combo.
4. **Fallback shipped-state options**: (a) gate H.264 to the ≤200 Mbps CABAC regime (the zone
   SteamFrame considers stable); (b) ship HEVC/AV1 (fully working) and leave H.264 experimental;
   (c) client-side check — nothing needed until the host produces frames.

---

## 7. Instruments and capture protocol

- **Heartbeat** (every 10 s, survives any flood): `NvencTap: heartbeat … | init … | reconf … |
  register … encode … lock L (last S bytes) encFail F | lastEncFail <status> "<string>"`.
  This is the first line to read. `ovrFail->ok` must stay 0; `fail 0` after init/reconf.
- **One-shot detail lines** (rotate fast under flood — capture within ~60 s):
  `nvEncInitializeEncoder override:` / `nvEncReconfigureEncoder override:` (all applied fragments),
  `split skipped`, `preset AUTO (H264) -> P2`, `nvEncEncodePicture -> failed (first 3)`,
  `first bitstream lock: N bytes, head XX…` (byte-level codec proof: H.264 `00 00 00 01 67…`,
  HEVC `00 00 00 01 40…`, AV1 `12 00 0a…`).
- **Caps shim provenance**: `caps shim: H.264 selection from settings file …` (pre-publish race
  proof), `caps shim: H.264 WIDTH_MAX/HEIGHT_MAX answered 1024/4096`.
- **Log locations**: `C:/Program Files (x86)/Steam/logs/vrserver.txt` (+`.previous.txt`),
  `driver_vrlink.txt`. SteamVR logs rotate at 10 MB; the flood makes this minutes-scale.
- **Round protocol** (`round-600h264/VERIFY-PROTOCOL.md`): SteamVR fully quit → settings edit/swap
  → start → connect → 45-60 s → capture. adb serial `R3GYB09JA5E`.

---

## 8. Evidence index (round-600h264/)

`FINDINGS-SF1-h264-mode.md`, `FINDINGS-SF2-per-codec-defaults.md`,
`FINDINGS-SF3-h264-call-fields.md` (field-by-field adapter diff, offsets, blind-copy note),
`fix-diff-final*.patch` / `fix-diff-v2*.patch` / `fix-diff-v3*.patch` (progression),
`build-r5.log … build-r9.log`, `test-r5.log … test-r9.log` (gate runs),
rollback DLLs per round, `logcat-h264-*.txt` (client side), `VERIFY-PROTOCOL.md`.

Also relevant: this repo's `diagnostics/steamlink-streaming/2026-10-14-codec-fallback-and-bitrate-verification.md`
and `2026-10-14-driver-reinstall-av1-500-and-native-res.md` (AV1/HEVC verification lineage).

---

## 9. Unrelated open items (do not lose)

- Native-resolution first-run staging gap (options A/B/C presented 2026-10-14, never chosen).
- Pre-existing test failures, verified NOT caused by this work: `Sdr10Startup`, `ToggleMigrations`,
  `VrlinkCapabilities` (missing Zig compiler, environmental).
- Hot-edit ban: never edit `settings.json` while SteamVR runs.
