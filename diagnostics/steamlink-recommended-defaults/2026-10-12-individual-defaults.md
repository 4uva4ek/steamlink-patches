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

Follow-ups: the 5 wrapper sets may be retired entirely once the individual-default model has device mileage.
