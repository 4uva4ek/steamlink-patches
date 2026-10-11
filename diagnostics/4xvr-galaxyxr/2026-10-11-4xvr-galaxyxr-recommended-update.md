# 4XVR Galaxy XR set — recommended by default + first on-device feedback fix (2026-10-11)

Appends to [the implementation record](2026-10-11-4xvr-galaxyxr-implementation.md); its
`default-off` conclusion is superseded here.

## Owner decision

All 13 non-experimental 4XVR patches (G1 launch, G7 controller models, G8 refresh profile, G3 HDR +
payload, G4 audio + payload, G5 render dispatch + payload, G6 subtitle suite + payload, G9 continuity
helper, and the `Galaxy XR set (4XVR 2.0.22/20022)`) are now `default = true` (recommended). The
experimental `Meta platform decoupling (4XVR, experimental)` stays `default = false` — its own label
says to verify on device before enabling. Guidance updated to match: `AGENTS.md`, `README.md`,
`TECHNICAL_REFERENCE.md`, `PATCH_CATALOG.md`, `DEBUGGING.md`, and the in-repo morphe-patches skill.
`FourXvrBundleTest` now asserts the recommended flags (set, 8 members, 4 payloads on; decoupling off).

## First on-device feedback and root cause (manager on SM-S928B, 2026-10-11)

- Manager 1.35.0 / Patcher 1.15.1 / source 1.28.0-dev.7; app 4XVR 2.0.22/20022; 14 patches selected.
- Failure: `Frame-synced render dispatch (4XVR)` →
  `IllegalStateException: Expected one V4Render->RenderFrame call behind the p1 branch in Main4XActivity.RenderFrame; found 0`.
- Root cause (ours, in `FourXvrDexKit.pRegister`): the parameter offset counted parameter **types**
  instead of parameter **words**. `RenderFrame(ZJJ)V` has 5 parameter words (`Z` + `J` + `J`), so the
  boolean parameter `p1` sits 5 registers below the register count, not 3. Every wide-parameter
  method was mis-anchored; the drain check could never match. Confirmed against the decoded stock
  tree (`4xvr-orig/smali/cn/vr4p/oculus4xvrplayerov/Main4XActivity.smali:6637-6642`):
  `:cond_0` / `if-eqz p1, :cond_1` / `iget-object v5 … m_V4Render` /
  `invoke-virtual {v5, v1, v2, v3, v4}, Lcn/vr4p/oculus4xvrplayerov/V4Render;->RenderFrame(JJ)V`.
- Fix: `parameterWordCount` (J/D = 2 words) feeds `pRegister`; the drain anchor now requires the
  single `V4Render->RenderFrame` call and validates the `IF_EQZ p1` branch, reporting the actual
  opcode/register on mismatch. Regression test: `FourXvrDexKitTest` (portable, no APK).
- Still pending: re-run on the phone against a build that contains the fix (needs a new release).

## Verification (local, 2026-10-11, after the fix)

- `:patches:test :patches:buildAndroid :patches:generatePatchesList -PreleaseChannel=experimental`:
  BUILD SUCCESSFUL. Test results on disk: 170 tests — 158 executed + 12 skipped (0 failures,
  0 errors). The 12 skipped are the SteamLink real decoded-input audits with explicit
  `assumeTrue("… BLOCKED: missing exact …")` gates (no 5001812 decoded tree and similar absent
  local fixtures — the designed fresh-checkout behavior; no synthetic replacement). The 4XVR
  suites are 8/8 executed with 0 skips: payload hashes, native guards, manifest fixture,
  parameter-word regression, recommended-flags gate.
- `patches-list*.json` regenerated: the 13 4XVR entries are `default: true`, the experimental
  decoupling `default: false`.
- Evidence labels: static + local Gradle only. The phone run above was against the previous
  release (before this fix); the headset has not run this build.
