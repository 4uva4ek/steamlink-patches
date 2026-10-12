# Controller group, duplicate-patch merges, Experiments removal — implementation plan

Date: 2026-10-12. Status: **PLAN ONLY — no sources touched.** Owner approval gates every phase.
Implementation, when approved, runs through bots only (see §7): cavecrew children (`delegate_task` with
`.agents/skills/cavecrew/references/hermes-roles.md` contracts) + the SteamLink Patches crew
(@patcher / @reviewer / @diagnostics / @verify). The main thread compiles, runs Gradle and owns every
verification claim; children never run Gradle.

## 1. Merge list — controller patches duplicated only by version/build

Repo-wide grep: exactly **2 exact duplicate pairs** exist in the Steam Link set. Both merge to 1 patch
each; the per-build difference moves **inside** the single patch body — the canonical repo idiom
(1 val + per-build data table + exact-pair guard + `return@execute`, as in
`MicrophoneInputPresetPatch.kt`, `OledCalibrationPatch.kt`, `HmdOnlyPatch.kt`).

| # | Keep | Merge away | Current names (file:line) | Merged name | One patch covers | Per-build difference inside the body |
|---|---|---|---|---|---|---|
| **M1** | `controllerVelocityFramePatch` | `controllerVelocityFrameLegacyPatch` | `Controller velocity frame (experimental)` / `Controller velocity frame, 2.0.20 - 2.0.22 (experimental)` — `ControllerVelocityFramePatch.kt:101/:109` | **Controller velocity frame** | 2.0.23/5002363 + 2.0.20/5001712, 2.0.20/5001812, 2.0.21/5001968, 2.0.22/5002244 | `angularWorld` config bit baked into the bundled layer `.so` (`GXRVFRCFG0000001` +20): `false` @5002363 (local to streamed pose), `true` @legacy (base space) — today chosen by which patch runs; after merge read from the matched build record |
| **M2** | `controllerHalPosePatch` | `controllerHalPoseLegacyPatch` | `Controller tracking from the controller HAL through Shizuku (experimental)` / `..., 2.0.20 - 2.0.22 (experimental)` — `ControllerHalPosePatch.kt:79/:88` | **Controller tracking from the controller HAL through Shizuku** | all 5 bases | same `angularWorld` split (`false` @5002363 = grip pose; `true` @legacy = base space); compat list legacy vs native → single guard via existing `isShizukuBridgeBuild` (all 5 bases) |

Merge mechanics (both files, per current factory shape):

- one build-record list covering all 5 bases, add per-build `angularWorld` to the record;
- `compatibleWith(*builds.map { Compatibility(name = EXPERIMENTAL_COMPATIBILITY_NAME, ...) })` keeps its
  shape — 1 object per build; 2.0.20 stays 2 separate objects (Morphe rejects duplicate version strings
  inside one Compatibility);
- `execute` keeps the exact-pair guard + `return@execute`; writes `library(angularWorld = build.angularWorld)`;
- merged descriptions keep every phrase the tests pin: `needs Shizuku`, `local to the grip pose`,
  `local to the streamed pose`, `in the base space`, and per-target validation notes
  (`measured on a Galaxy XR headset.` / `not run on this base`);
- drop `Do not combine with Controller velocity fix.` from the M1 legacy target description (patch deleted, §5);
- `default = false` unchanged; M1 dependencies stay empty (test asserts `dependencies.isEmpty()`);
  M2 keeps `dependsOn(shizukuBridgeExtensionPatch, shizukuBridgeManifestPatch, controllerHalPoseExtensionPatch)`.

**Deliberately NOT merged** (create new patches only when doing something different / better modular):

- `GXR face bridge (version 5002318 and below)` vs `GXR tongue bridge (version 5002322 and above)`:
  adjacent version-boundary pair but different mechanisms/payloads (layer install vs in-place scene edit).
  Owner call — mergeable into 1 ranged patch; **default = keep modular** (D3).
- Recommended sets: 4 legacy bundles share the identical 17-member closure; the source comment marks the
  per-build split as intentional → keep.
- Blue-noise fovea vs background: separate user-selectable layers, not version duplicates → keep 2, just de-mark (§4).
- Controller angular world to local / velocity frame / HAL pose: 3 alternative mechanisms for the
  angular-frame problem, mutually exclusive at runtime (conflict guards) → keep modular.

## 2. New category: Controller

- `shared/PatchCategories.kt`: delete `EXPERIMENTS`, add `const val CONTROLLER = "Controller"` in its slot.
- Members (5), all **non-experimental** — name loses `(experimental)` and any version suffix:

  1. `Controller velocity frame` (merged M1) — no deps
  2. `Controller tracking from the controller HAL through Shizuku` (merged M2) — Shizuku deps
  3. `Controller grip haptics through Shizuku` — Shizuku deps unchanged
  4. `Controller pose extrapolation` — no deps
  5. `Controller angular world to local` — no deps; description keeps `Exact 2.0.23/5002363 only`

- Shizuku split honored: {2, 3} keep shizuku-bridge deps; {1, 4, 5} stay dependency-free.
- Not in recommended bundles; `default = false` unchanged; members keep `EXPERIMENTAL_COMPATIBILITY_NAME`
  (marking scope — D1).

## 3. Experiments category removal

- Reassign every current member (8 call sites, 11 objects incl. 4XVR):
  - 5 controller patches → **Controller** (§2)
  - `fovealBlueNoisePatch` / `backgroundBlueNoisePatch` → **Image quality**, names drop `(experimental)`
    → `Foveal blue-noise dithering` / `Background blue-noise dithering` (explicit owner instruction)
  - `xrFovealCanvasPatch` → Image quality (proposed; keeps `(experimental)` in name) — D4
  - 4XVR `fourXvrPlatformDecouplingPatch` → Advanced XR compatibility (proposed; name unchanged) — D4
- `.github/scripts/generate_patches_readme.py` `CATEGORY_ORDER`: replace `"Experiments"` with
  `"Controller"`; mirror the fixture change in `test_generate_patches_readme.py:21`.
- Sweep surfaces for `Experiments` / renamed patch names: `README.md:71`, `PATCH_CATALOG.md:21` (table)
  + patch sections, `TECHNICAL_REFERENCE.md` (generated), extension READMEs (`controller-*`, `shizuku-bridge`),
  `diagnostics/patch-categories/{README.md, VerifyCategoryBundle.java:13, validation.json}` (supersede with a
  new dated receipt), skills/instructions if they enumerate categories or patch names.

Counts after §2 + §5 (Steam Link public patches):

| Surface | Planned delta (pre-rebase) | Executed (rebased onto 6b789c0) |
|---|---|---|
| `PatchCategoriesTest` total / sizes | 36 → 33 / `[5,5,5,4,2,7,5]` | **32 → 29; `[2,6,4,3,7,10]` → `[5,5,4,3,7,5]`** |
| catalog entries stable / non-stable (Steam Link) | 26 → 25 / 36 → 33 | **22 → 21 / 32 → 29** |
| root catalogs total entries (all apps) | 51 (stable 41) → 48 (stable 40) | **47 (37) → 44 (36)** |
| Experiments-count assert | 0 stable / 10 else → Controller 0 / 5 | **Controller 0 stable / 5 else (executed)** |

## 4. Blue-noise dithering de-marking

Per §3: both names lose `(experimental)`; category → Image quality.
Update `BackgroundBlueNoisePatchTest.kt:21-22` name asserts (BlueNoisePatchTest has no name asserts);
catalogs/docs regenerated.

## 5. Delete "Controller velocity fix" + its code (dead experiment)

Delete:

- `ControllerVelocityPatch.kt` (val `controllerVelocityPatch`, 5 options, cadence application, ids.xml fallback)
- resources: `libgxr_controller_velocity.so`, `XR_APILAYER_local_GalaxyXR_controller_velocity.json`
- tracked extension source: `extensions/controller-velocity-layer/` (CMakeLists.txt, src/ ×3, tests/ ×1)
- `ControllerPoseCadence.kt` + `ControllerPoseCadenceTest.kt` + `LegacyAddedNativeAudit.kt` cadence refs
  (production-dead once the patch is gone; audit-only) — D2, default = remove too

Update refs:

- `ControllerVelocityFramePatch.kt:79` `"Do not combine with Controller velocity fix."` → gone with M1
- `ControllerVelocityFramePatchTest.kt:66-68` (distinct-from-old-layer pins) → drop lines
- `PatchCategoriesTest.kt:24-27` (Tracking & audio list) + counts (§3 table)
- `PatchCompatibilityMatrixTest.kt` import `:6`, `:201` not-in-bundle list, `:209` default assert,
  test name + size `:215-218` (22 → 21), companion `:329`
- `util/DecodedSteamLinkPatchAudit.kt:20, :124, :309, :339-345`
- `PATCH_CATALOG.md:17/66/207-219/246/652/657`; catalogs + `TECHNICAL_REFERENCE.md` regenerated
- `diagnostics/patch-categories/VerifyCategoryBundle.java:13` (`"Tracking & audio", 6` → 5)

Do NOT delete: external `../builds/steamlink-patches/extensions/controller-velocity-layer/build-android/_deps/openxr_headers-src`
— other layers compile against those OpenXR headers (`extensions/resolution-trace-layer/README.md:53`,
`extensions/foveal-canvas-layer/Build-Native.ps1:7`, `WORKSPACE_CLEANUP.md:170`).

Record: dated retirement entry `diagnostics/patch-categories/2026-10-12-controller-velocity-fix-retirement.md`.

## 6. Decision points (owner)

| # | Question | Default |
|---|---|---|
| D1 | Scope of "not experimental": (a) marking only — names lose suffix, compat objects/channels unchanged (stay out of stable catalog) vs (b) full promotion — also rename compat to non-experimental → stable catalog + channel ripple | **(a)**; (b) as separate release decision |
| D2 | Cadence helper (`ControllerPoseCadence.kt` + test + audit refs): remove with the dead patch, or keep audit-only | **remove** |
| D3 | Face/tongue bridge pair: keep modular or merge into 1 ranged patch | **keep modular** |
| D4 | Foveal canvas destination category: Image quality vs Advanced XR compatibility; 4XVR Meta decoupling → Advanced XR compatibility | **as proposed** |
| D5 | Category display name: `Controller` vs `Controllers` / `Controller fixes` | **Controller** |

## 7. Execution via bots (after "go")

Roles: cavecrew children via `delegate_task` (contracts in `.agents/skills/cavecrew/references/hermes-roles.md`)
+ crew roster handoffs (Goal / Context / Expected output / Evidence required). Builders ≤2 files, never
run Gradle; deletions/destructive ops stay with the main thread (`needs-confirm.` receipts).

| Phase | Who | What |
|---|---|---|
| P0 gate | investigator bot + owner | re-confirm ref lists on current HEAD, no new refs since plan; owner picks D1–D5 |
| P1 merges | builder ×2 (1 file each) | M1 `ControllerVelocityFramePatch.kt`, M2 `ControllerHalPosePatch.kt`; parent re-reads + compiles |
| P2 category/name | builder ×~4 | `PatchCategories.kt` + grip haptics; pose extrapolation + angular local; `BlueNoisePatch.kt`; `FovealCanvasPatch.kt` + 4XVR `NativeProfilePatches.kt`; parent compiles |
| P3 deletions | main thread + @reviewer + @verify | `git rm` per §5 after sign-off; @diagnostics writes retirement record |
| P4 tests | builder ×2 | ControllerVelocityFramePatchTest + ControllerHalPosePatchTest; PatchCategoriesTest + PatchCompatibilityMatrixTest; D2 removals; parent runs `:patches:test` |
| P5 generated surfaces | main thread | `./gradlew :patches:generatePatchesList` + readme generator; builder for `PATCH_CATALOG.md`/`README.md` manual sections; @diagnostics refreshes receipts |
| P6 review | @reviewer per diff | AGENTS.md rules: guards/compat/defaults/payload isolation preserved, output routing, generated-vs-hand surfaces in sync |
| P7 verification | main thread | `:patches:test` executed/skipped counts; release-graph repro `:patches:buildAndroid :patches:generatePatchesList -PreleaseChannel=experimental` in isolated checkout; apply merged patches against local decoded bases (untracked) with existing audit runners; receipts under `../builds/steamlink-patches/`. Device run stays in the owner Morphe-Manager loop |

Evidence rules: static/local checks ≠ Gradle/Morphe/CI results; only the corrected commit's workflow proves CI.

## 8. Planned touch inventory

- Sources: `ControllerVelocityFramePatch.kt`, `ControllerHalPosePatch.kt`, `ControllerGripHapticsPatch.kt`,
  `ControllerPoseExtrapolationPatch.kt`, `ControllerAngularLocalPatch.kt`, `BlueNoisePatch.kt`,
  `FovealCanvasPatch.kt`, `PatchCategories.kt`, `fourxvr/NativeProfilePatches.kt`;
  DELETE `ControllerVelocityPatch.kt`, `ControllerPoseCadence.kt`, 2 resources, `extensions/controller-velocity-layer/`.
- Tests: `ControllerVelocityFramePatchTest`, `ControllerHalPosePatchTest`, `ControllerGripHapticsPatchTest`,
  `ControllerPoseExtrapolationPatchTest`, `ControllerAngularLocalPatchTest`, `BackgroundBlueNoisePatchTest`,
  `PatchCategoriesTest`, `PatchCompatibilityMatrixTest`, `ControllerPoseCadenceTest` (delete, D2),
  `util/DecodedSteamLinkPatchAudit.kt`.
- Docs: `README.md`, `PATCH_CATALOG.md`, extension READMEs (controller-velocity-frame-layer, controller-hal-pose,
  controller-grip-haptics, controller-angular-local, controller-extrapolation-layer, shizuku-bridge),
  `TECHNICAL_REFERENCE.md` + 4 root catalogs (generated), diagnostics receipts.
- Repo-wide guidance sweep at implementation: grep names / `Experiments` in `AGENTS.md`, skills,
  `.github/agents`, copilot instructions, `DEBUGGING.md`; update only where they enumerate patches/categories.
  (Noted unrelated staleness: `.agents/skills/morphe-patches/SKILL.md` still references a
  `UberLinkRescanDisablePatchTest` that no longer exists — fix opportunistically with owner nod.)

No changes to 4XVR beyond the Meta-decoupling category reassignment; bundles untouched; no compatibility
lists, offsets or payload bytes change in this plan except the two merged patches' internal selection logic.

## Execution log — 2026-10-12 (applied)

Rebased onto `6b789c0` (the concurrent recommended-defaults session deleted the 5 wrapper bundles before
the apply; its follow-up credits change — `STEAM_LINK_CONTROLLER_CREDIT` / `FOUR_XVR_UPSTREAM_CREDIT`
appended to patch descriptions — was absorbed into the same change, owner-approved).

Executed via cavecrew children: Batch A = 1 sweep investigator + 6 source builders (2 merges, category
constant, 4 name/category groups); Batch B = 7 builders (test surfaces, util audits); Batch C = 7 builders
(PATCH_CATALOG, README, doc-generator script + fixture, 6 extension READMEs, retirement record). Main
thread: deletions (`git rm`: patch, cadence helper + test, 2 payloads, `extensions/controller-velocity-layer/`),
the `PatchCompatibilityMatrixTest` 15→14 count fix, all Gradle runs, catalog + docs regeneration.

Results: `:patches:test` 169 executed / 13 skipped (decoded-base audits) / 0 failures;
`:patches:buildAndroid :patches:generatePatchesList -PreleaseChannel=experimental` green — 4 root catalogs
regenerated (44 patch objects; 36 stable; `Controller` ×5); `TECHNICAL_REFERENCE.md` regenerated and the
doc-generator fixture test passes (4 tests). `PatchCategoriesTest` pins 29 public patches, sizes
[5, 5, 4, 3, 7, 5], Controller 0 stable / 5 experimental. Retirement record:
`diagnostics/patch-categories/2026-10-12-controller-velocity-fix-retirement.md`. Not committed — the
worktree holds this change plus the absorbed credits for one commit.

### Wave 2 — remaining version-split merges (same day)

Owner follow-up: the sweep continued beyond the controller pairs. Merged in the same change:

- **Device identity** — the two vals (`deviceIdentityPatch` legacy + `deviceIdentityNativeXrPatch`
  2.0.23) collapsed into ONE patch over all 5 bases; the factory body already selected per build
  (legacy Quest Pro resource swap vs native Galaxy XR transport identity). `default = true` now
  applies on all 5 bases, so 2.0.23's Simple-mode set gains Device identity (7 patches) — flagged
  to the owner, revertible by flipping the single flag.
- **GXR face bridge + GXR tongue bridge → `GXR face and tongue bridge`** — one patch over the 5
  bases; the face-tracking layer + FACE_TRACKING permission on builds up to 5002318, the tongue
  transport edit on 2.0.23/5002363. Both were `default = true`, so recommendation behavior is
  unchanged per build.

Follow-ups in the same change: the native-closure invariant in `PatchCompatibilityMatrixTest` now
expects the merged identity's guarded legacy chain (`{xrDeviceConfigBaselinePatch, xrCoreRuntimePatch}`,
each a build-aware no-op on native); `Constants.kt` per-build "recommends" prose updated (the
5002363 branch now names Device identity + the merged bridge); the `AGENTS.md` default-flag doctrine
no longer cites the deleted split and now prefers per-build single patches; `WORKSPACE_CLEANUP.md`
retained-references line updated.

State after wave 2: 27 Steam Link public patches (sizes 5/4/4/2/7/5); root catalogs 42 objects
(34 stable; `Controller` x5); `:patches:test` 169 executed / 13 skipped / 0 failures;
`TECHNICAL_REFERENCE.md` regenerated; doc-generator fixture test 4/4. Owner approved commit + push
to `dev` so the next dev pre-release reaches Morphe Manager.

### Owner correction — Device identity recommendation scope (same day, after dev.16)

Owner: `Device identity` must be recommended on the legacy bases and **not** on 2.0.23. Morphe
exposes one global `default` flag per patch object, so recommendation scope cannot be expressed
inside the single merged object. The wave-2 merge is therefore refined into ONE shared factory +
guarded body with TWO scoped objects: `deviceIdentityPatch` (the 4 legacy bases
5001712/5001812/5001968/5002244, `default = true`) and `deviceIdentityNativeXrPatch`
(2.0.23/5002363 only, `default = false`, optional/selectable). No body or mutation is duplicated —
the split exists only for the global `default` flag. Effects: the 2.0.23 recommended set is 6
patches again (the native build still exposes 7 stable individual patches); the legacy recommended
set stays 17. `PatchCompatibilityMatrixTest`, `PatchCategoriesTest`, `DecodedSteamLinkPatchAudit`,
the per-build `Constants` "recommends" prose and `PATCH_CATALOG.md` were updated to match.
