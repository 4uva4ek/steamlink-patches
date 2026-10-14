# Retirement — `Controller velocity fix` (dead experiment)

Date: 2026-10-12. Status: **RETIRED — deleted from the tree.** Owner approved the removal on 2026-10-12.

## Decision

The patch `Controller velocity fix` (val `controllerVelocityPatch`, formerly category Tracking & audio) is
retired as a dead experiment, superseded by the single **`Controller velocity frame`** layer patch
(`ControllerVelocityFramePatch.kt`) — one mechanism, all supported bases.

## Deleted with the retirement

- `patches/src/main/kotlin/app/template/patches/steamlink/androidxr/ControllerVelocityPatch.kt`
- `.../androidxr/ControllerPoseCadence.kt` — cadence helper, production-dead once the patch was gone
- `patches/src/test/.../androidxr/ControllerPoseCadenceTest.kt`
- payloads `patches/src/main/resources/steamlink/androidxr/libgxr_controller_velocity.so`,
  `XR_APILAYER_local_GalaxyXR_controller_velocity.json`
- tracked layer sources `extensions/controller-velocity-layer/` (`CMakeLists.txt`, `src/`, `tests/`)
- audit references in `util/DecodedSteamLinkPatchAudit.kt` and `util/LegacyAddedNativeAudit.kt`

## Kept — do NOT prune

External build tree `../builds/steamlink-patches/extensions/controller-velocity-layer/build-android/_deps/openxr_headers-src`.
Other layers (foveal-canvas, resolution-trace) build against those downloaded OpenXR headers —
`WORKSPACE_CLEANUP.md` keep-list rule.

## Superseded receipts

`diagnostics/patch-categories/{VerifyCategoryBundle.java, validation.json, README.md}` predate both the
2026-10-12 recommended-defaults change and this removal. Supersede rather than edit.

## Verification (local, at removal)

- `:patches:test` — 169 executed / 13 skipped (decoded-base audits) / 0 failures.
- 4 root catalogs + `TECHNICAL_REFERENCE.md` regenerated against the current patch set:
  29 Steam Link public patches; new `Controller` category of 5.

## Closing rule

Do not re-add the cadence / velocity-fix mechanism — the velocity-frame layer covers it. Future
angular-velocity work targets the Controller group layers.
