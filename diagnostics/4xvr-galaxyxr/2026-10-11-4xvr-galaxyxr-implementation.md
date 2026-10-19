# 4XVR Galaxy XR patch set — implementation record (2026-10-11)

Supplements [the diff and plan record](2026-10-11-4xvr-galaxyxr-diff-and-plan.md); it supersedes nothing.
Scope: implemented the 4XVR patch set for exact `cn.vr4p.oculus4xvrplayerov` 2.0.22/20022 from the
decoded stock/reference diff. Every patch is default-off and stays outside the default-on
recommended sets.

## Implemented (all `default = false`)

| Group | Patch name (catalog) | Type |
|---|---|---|
| G1 | Galaxy XR launch compatibility (4XVR) | resourcePatch |
| G7 | Galaxy XR controller models (4XVR) | rawResourcePatch |
| G8 | Galaxy XR refresh profile (72 Hz, 4XVR) | rawResourcePatch |
| — | Meta platform decoupling (4XVR, experimental) | rawResourcePatch (EXPERIMENTS, not in the set) |
| G3 | libplacebo HDR pipeline (4XVR) + HDR pipeline payload (4XVR) | bytecodePatch + rawResourcePatch |
| G4 | 5.1-channel audio bridge (4XVR) + Audio bridge payload (4XVR) | bytecodePatch + rawResourcePatch |
| G5 | Frame-synced render dispatch (4XVR) + Render dispatch payload (4XVR) | bytecodePatch + rawResourcePatch |
| G6 | Subtitle suite (4XVR) + Subtitle suite payload (4XVR) | bytecodePatch + rawResourcePatch |
| G9 | Startup and UI continuity helper (4XVR) | bytecodePatch |
| Set | Galaxy XR set (4XVR 2.0.22/20022) | rawResourcePatch, opt-in only |

Sources: `patches/src/main/kotlin/app/template/patches/fourxvr/` (10 Kotlin files), payloads
`patches/src/main/resources/fourxvr/` (45 files / ~20 MB), extension smali 32 classes merged by the
`assembleFourXvrExtension` task into `extensions/fourxvr-extension.mpe`. G4/G6/G9 hook files were
built by delegated cavecrew builders; the main thread verified every citation, fixed the G6 opcode
name (`INT_TO_FLOAT`), and ran all checks.

## Verification (local, 2026-10-11)

- `:patches:compileKotlin` and `:patches:compileTestKotlin`: pass.
- `:patches:test`: **174 tests / 0 failures / 0 errors / 0 skipped** (35 suites). 4XVR suites 7/7:
  `FourXvrPayloadResourceTest` (2), `FourXvrNativeGuardTest` (3), `FourXvrBundleTest` (1),
  `FourXvrLaunchManifestTest` (1) — payload hashes, stock-lib byte guards (incl. the three
  72 Hz refresh-profile bytes), manifest fixture, default-off gate.
- `:patches:test :patches:buildAndroid :patches:generatePatchesList -PreleaseChannel=experimental`:
  BUILD SUCCESSFUL; all 4 catalogs regenerated with the 13 4XVR entries (+465 lines each).
- Evidence labels: static analysis + local Gradle build only. No headset run; no pristine stock
  2.0.22/20022 APK was ever available, so patch application against stock remains unproven
  end-to-end. CI on the pushed commit is not yet run.

## Design notes carried from the reference

- `V4Theatre.SetCurSubTheatreID` region (i): the retired monitor-block (clear the render runnable)
  is neutralized **in place** — the store becomes `RenderDispatch.requestScene(this, id)`, the
  monitor entry becomes a jump to the post-switch point, the now-dead monitor/put bytes become
  nops, and the catchall handler's monitor-exit is nop'd. Public dexlib2 cannot remove try blocks;
  reachable behavior equals the reference (`requestScene` + jump). Verifier acceptance is to be
  confirmed on device.
- The MyVideoRunnable surface routing parks the routed surface in `v10` (dead across the region)
  because using the reference's `v43` would collide with the parameter register at the stock
  register count of 43.
- 72 Hz mechanism: `InitializeDevice` refresh-picker floors `121.0f/91.0f -> 73.0f` in
  `libvr4p-oculus.so` (3 guarded bytes; stock sha256 pinned in `FourXvrNativeGuardTest`).

## Open gates (unchanged)

1. Pristine stock 4XVR 2.0.22/20022 APK absent — end-to-end patch application unproven.
2. Bundled libplacebo is modified LGPL-2.1+ with no corresponding source shipped — private use /
   obtain source / drop the G3 payload before any release claim.
3. Headset runtime acceptance of every group unproven (no device run in this record).
4. `Meta platform decoupling (4XVR, experimental)` default-off, excluded from the set; the
   premium/VIP shim (G10) remains excluded (licensing-sensitive mechanism only, never ported).
