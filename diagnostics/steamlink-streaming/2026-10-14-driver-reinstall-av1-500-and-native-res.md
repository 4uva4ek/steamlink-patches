# 2026-10-14 — Driver reinstall: AV1@500 live verification, native-resolution staging, install/uninstall inventory

## Context
Driver uninstalled, then reinstalled fresh (new version **GalaxyXRNative 1.9.0-dev.3**,
`driver_GalaxyXRNative.hasBeenRun`), user set AV1 @ 500 Mbps and reported a resolution
regression. Verified live end-to-end; cause found; fix applied and confirmed.

## Live verification (session vrserver PID 8412, start 19:02:35)
- Config: settings.json `nvencCodec: 2` (AV1) + `galaxyXr.streamQuality: "custom"` +
  `customBandwidthMbit: 500`. This is the GUI "custom" route; the alternate route
  (balanced + Bandwidth Override) writes `nvencBandwidthOverrideMbit: 500` into
  streamFrame instead (FINDINGS-C Q3) — both feed `GalaxyXR_EffectiveBandwidthMbit`
  (override wins, clamp 10..2000) and `WriteStreamKeys` (GalaxyXR.cpp:120-128).
- steamvr.vrsettings: `targetBandwidth: 500`, `recommendedBandwidthMbit: 500`,
  `automaticBandwidth/automaticStreamFormatWidth: false`, native-res keys 3552x3840.
- Host init: `nvEncInitializeEncoder override: codec HEVC->AV1; ... avg 0M->500M max
  0M->500M`; `stream quality 'custom': tile 1536, 500 Mbit/s`.
- Counters all clean: `ovrFail->ok 0, fail 0, stockFail 0`; asked max ~247M (vrlink
  internal ~350 ceiling), tap scales x500/350; actual wire ~248 Mbit/s (frames avg
  345KB, static lobby).
- Client: layer v6 loaded, `switched decoder to video/av01`, av1D_79 ~90 fps, geo
  1536x6144; AU dump decodes as **av1 (libdav1d) Main, 10-bit, 1536x6144** (byte-level).

## "Wrong resolution" = native-resolution keys staged but not yet applied
- New driver feature (default ON: `galaxyXr.nativeResolution`, Config.h:312) writes
  `renderWidth/renderHeight/overrideRenderWidth/overrideRenderHeight = 3552x3840`
  into `driver_vrlink` + vrlink model mirrors (GalaxyXR.cpp:445-451). GUI text:
  "Restart SteamVR to apply it."
- First post-install session started ~6 s before the driver's write; compositor
  `GetRecommendedRenderTargetSize` = **2204x2352** per eye (pre-apply default), visibly
  soft. After one SteamVR restart: **4348x4700** per eye = native 3552x3840 x 1.5-area
  clamp, identical to every pre-uninstall session.
- Instrument: `GetRecommendedRenderTargetSize` / `Raw ideal render target scale` lines
  in `logs/vrcompositor.txt`.

### Why run 1 missed it (code-verified, same day)
- `DeviceProvider.cpp:110-112`: the early apply exists for exactly this class of race —
  "vrlink reads its stream/profile keys from steamvr.vrsettings during its own init,
  before any HMD Activate; write ours now so the FIRST connect of a session already runs
  the current config (09-03 race)".
- `GalaxyXR.cpp:359-366` (2026-09-03): vrlink reads its keys in its own init ~3 ms before
  the shim's Activate (run-1 evidence 20:18:25.893 vs .896); the early apply moved the
  writes to provider Init to beat it.
- That fix covers STEADY STATE only: once the keys exist on disk, every later start reads
  them at vrlink's driver init. Built-in vrlink loads before the external GXR driver
  (18:55:35.26 load vs 18:55:40.1 GXR writes in the failing session), so the uncovered
  case is the very FIRST start after install / Clean Settings, when the keys do not exist
  yet. `GalaxyXR.cpp:430-431` states the consumption rule: keys are read at SteamVR start /
  compositor init; mid-session toggles apply next launch — runtime cannot fix run 1.
- Fix options (decision pending with owner): (A) install-time pre-seed of the default
  tuning keys in the GUI vendor install flow (journaled; first run renders native);
  (B) post-install "restart SteamVR once" notice (the vendor flow currently returns
  silently — system-diagnostic.ts:342-359); (C) docs only.

## Install/uninstall semantics (BRIEF-C -> FINDINGS-C.md; spot-checked by parent)
- Install (vendor build): managed copy to
  `%LOCALAPPDATA%/GalaxyXR/Drivers/GalaxyXRNative-<stamp>` + `vrpathreg.exe adddriver`
  with read-back + `driver_GalaxyXRNative.enable=true`; no services/registry/scheduled
  tasks. Old copies under `<steamvr>/drivers` are retired by rename to
  `<steamvr>/.galaxyxr-retired/<stamp>/` (never deleted in place).
- Uninstall (requires SteamVR stopped): wipes `%APPDATA%/GalaxyXR/CustomHeadset`
  except `Distortion` (settings.json + gui-settings.json are deleted); preserves
  `Distortion` + `%APPDATA%/GalaxyXR/Backups`; restores journaled vrsettings values
  (later external edits preserved with a warning); identity cleanup removes companion
  keys only when the current value equals the expected identity (fail-closed).
- `LastKnown.ActualHMDDriver` / `HMDSerialNumber`: no driver code writes or cleans them
  (SteamVR-owned records, vendored OpenVR headers only) — leftovers after uninstall are
  normal, not an uninstall gap.
- Live state matched this inventory in every checked point (settings.json gone after
  uninstall, driver_vrlink bandwidth keys restored, LastKnown kept, LocalAppData
  emptied then repopulated by the reinstall).

## Files
- `../builds/steamlink-patches/build/codec-verify-20261014/install-wave/` — BRIEF-C +
  FINDINGS-C.md (45-row receipt, full both-files AV1@500 state).
- `../builds/steamlink-patches/build/codec-verify-20261014/round-500av1/` — captures,
  logcats, dumps, render-target-before-after.txt, FINDINGS.md.
