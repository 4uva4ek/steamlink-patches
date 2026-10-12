# Steam Link recommended defaults on individual patches — 2026-10-12

Change: the recommended set is now expressed as per-patch `default = true` flags instead of one wrapper set per exact build.

- Default-enabled individual patches: the 17 legacy members on 2.0.20/5001712, 2.0.20/5001812, 2.0.21/5001968 and 2.0.22/5002244, and the 6 modern members on 2.0.23/5002363 (high resolution, tongue bridge, Microphone input preset, OLED calibration, Unrestricted battery usage, Visual Delay Fix).
- The 5 `Galaxy XR recommended set` wrapper patches stay in the catalog as optional one-step selections with `default = false`. Simple mode and the Expert-mode recommended state now auto-select the same individual set per detected `(versionName, versionCode)`.
- `Device identity` (name kept) narrows to the 4 legacy bases with `default = true`. New `Device identity (2.0.23/5002363)` (`deviceIdentityNativeXrPatch`) is scoped to 2.0.23/5002363 with `default = false` and resolves to the Galaxy XR identity. Both share one private builder (`createDeviceIdentityPatch`) and the same guarded body.
- 4XVR is untouched; its 14 defaults (including its set patch) are unchanged.
- Doctrine updated in `AGENTS.md`; `README.md`, `PATCH_CATALOG.md`, `TECHNICAL_REFERENCE.md` describe the new model; `PatchCompatibilityMatrixTest`, `PatchCategoriesTest` and `StartupBoundaryTest` pin the per-build recommended sets.

Verification (local, 2026-10-12):

- `:patches:test`: 174 tests, 0 failures, 0 errors, 13 skipped (decoded-base audits `ControllerAngularLocalPatchTest`, `FovealCanvasPatchTest`, `VdSdrFoveaPatchTest`, assumption-gated on absent local fixtures).
- Catalog scan of `patches-list-all.json` reproduces the Manager selection: 17/17/17/17 legacy, 6 on 5002363, 18 default-true total, no other default-enabled Steam Link patch on any version code.
- `:patches:generatePatchesList -PreleaseChannel=experimental` plus `generate_patches_readme.py AngelDark92/xr-porting-hub dev` regenerate all 4 catalogs and `TECHNICAL_REFERENCE.md` (generator reports 53 patch rows).
- Independent review: 0 bugs, 3 consistency risks and 3 nits found; all addressed in this change (TECHNICAL_REFERENCE prose, shared `HMD identity` option description, and `DecodedSteamLinkPatchAudit` modern fixture now exercising `deviceIdentityNativeXrPatch` with the recommended Galaxy XR profile).
- Headset runtime acceptance of the defaults is pending; the dev pre-release carries the new build.

Update 2026-10-12 (later, device-confirmed): the 5 wrapper sets were deleted.

- `RecommendedPatchBundles.kt` and its 5 set patches are removed; the Steam Link `Recommended sets` group is gone from `PatchCategoriesTest`, which now pins 32 public patches with counts 2/6/4/3/7/10 and catalog entries 22 stable / 32 otherwise.
- Recommended defaults remain the 18 individual patches. Catalog simulation of the regenerated `patches-list-all.json` reproduces 17/17/17/17 legacy and 6 on 5002363; the catalogs now carry 47 patch objects (32 Steam Link + 15 4XVR, 37 stable).
- `DecodedSteamLinkPatchAudit` lost the `recommended` audit kind and its 5 fixtures; `FovealCanvasMorpheAudit` selects the 17 individual patches explicitly; `Run-MorpheAudits.py`, the resolution-trace-layer README, `LEGACY_VS_MORPHE_PATCHES.md` and `TECHNICAL_REFERENCE.md` were updated. Dated diagnostics records keep their original text; commands that used the removed `recommended` kind are superseded by this note.
- The shared `RECOMMENDED_SETS` category constant remains for the 4XVR set patch, which is untouched.
- Local gate after the deletion: 174 tests, 0 failures, 13 skipped decoded-base audits.
