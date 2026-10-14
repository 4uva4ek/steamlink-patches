# 4XVR on-device video freeze — VerifyError root cause and fix (2026-10-11)

Scope: exact `cn.vr4p.oculus4xvrplayerov` 2.0.22/20022 running the Morphe-patched Galaxy XR set on
Galaxy XR `R3GYB09JA5E` (AFZ3). Supersedes nothing; supplements the
[implementation record](2026-10-11-4xvr-galaxyxr-implementation.md).

## Symptom (owner, on headset)

Clicking a video in the Featured-Videos browser shows a frozen picture and the app exits; the app's
own "Click volume+ button to select，Click volume- button to back." hint (string id `0x7f0f0344`,
`Main4XActivity$6.lambda$run$0` → `V4Window.MakeToastText`) was on screen. Reproduced 20:54:16 and
21:00:36 (fresh process 15678/19600, same install).

## Root cause (device evidence)

```
E AndroidRuntime: java.lang.VerifyError: Verifier rejected class
cn.vr4p.vr4pmovieplayer.Vr4pMediaPlayer$MyVideoRunnable: void ...MyVideoRunnable.run() failed to
verify: [0x345] register v2 has type Conflict but expected Reference: android.view.Surface
(declaration ... appears in base.apk!classes8.dex)
```

ART rejects the patched `MyVideoRunnable.run()` → the video pipeline class never runs → frozen
frame. Cause: the G3 `libplacebo HDR pipeline (4XVR)` edit inserted the `HdrBridge.routeOutputSurface`
call *before* the `color-transfer` const-string and parked the result in v10. With dexlib2 the
branch targets (`:cond_1d`/`:goto_b`) stay attached to the const-string instruction, so inserted
instructions before it are skipped on the av01 path; v10 then merges (int/String) to Conflict and
the configure-time `move-object v2, v10` feeds a Conflict register to `MediaCodec.configure`
(expects `android.view.Surface`). The reference build avoids this by adding local v43 (`.locals
43→44`) for the parked value; it never reuses a stock scratch register.

## Fix (patches/src/main/kotlin/app/template/patches/fourxvr/HdrPipelinePatches.kt)

- `routeOutputSurface(codec, surface, format, listener)` now runs immediately **after** the
  `color-transfer` const-string (the branch target) and before `const/4 v2, 0x3`; fail-closed check
  on the transfer constant.
- Result parked in v9 (stock rewrites v9 at `const/4 v9, 0x0` before its next read); the stock
  surface reload at the configure site is replaced by `move-object v2, v9` (two nops). No register
  count change; no path can reach `MediaCodec.configure` without passing the definition
  (`:cond_1e` at 1454 skips the whole configure block; no label exists between the insert and the
  configure).
- Scratch registers used: v2 (rewritten by stock `const/4 v2, 0x3`), v9/v10 (rewritten by stock
  `const/4 v9/v10, 0x0`); v7 (MediaFormat) read-only.

## Verified / pending

- Local: `:patches:compileKotlin` pass; `:patches:test` 170 tests / 0 failures / 12 skipped (all
  payload pins included). The `FourXvrPayloadResourceTest` icon failure seen locally was a Windows
  CRLF checkout artifact (`i/lf w/crlf`, no attribute); fixed by adding LF pins for the 4XVR
  byte-hashed text payloads in `.gitattributes` (hashes unchanged) and re-checkout.
- PENDING: device proof. Requires a new Manager build of the fixed patch; the class must load and
  play without VerifyError, then HDR/SDR playback and seek/resume hooks are re-tested.

## Stock APK recovered (closes open gate 1)

- Pristine stock `cn.vr4p.oculus4xvrplayerov` 2.0.22/20022, 390,601,516 B,
  sha256 `24b83ab584675e02570e5318a35c7fa9b8da7eabb1f0763711c74c1bad3a84a8`
  — two identical copies on device (`app.morphe.manager/app_original-apks/` and
  `/sdcard/Download/4xvr.apk`); pulled to `builds/steamlink-patches/build/4xvr-2.0.22/`.
- Installed patched build (Manager `app_patched-apps/`), 396,682,935 B,
  sha256 `c596a54643f8af09633b93db7b83488414ca2c1704a2c2550846a805dbf75b7f`; pulled alongside.

## Input findings (owner wants controllers + hands working)

- Runtime (`/system/lib64/libopenxr_android.so`) implements `XR_EXT_hand_tracking`,
  `xrCreateHandTrackerEXT`, `xrLocateHandJointsEXT`, `XR_FB_hand_tracking_aim/_capsules/_mesh`,
  `XR_EXT_hand_interaction`, `XR_ANDROIDSYS_hand_tracking_parameters` → the engine's hand path is
  runtime-supported.
- Engine binds exactly one action profile: `/interaction_profiles/oculus/touch_controller`
  (1× `xrSuggestInteractionProfileBindings`); hand pose comes from EXT/FB hand tracking, not action
  bindings. HaT service is live (60 fps, `Number of hands tracked: 0` while controllers are held).
- In-app hand tracking appears never enabled: `shared_prefs/HandTracking.xml` holds only
  `HaveWarning=true` (no `HandTracking` key; `V4HandTrackDlg` → `SettingHandTracking()`).
  Next step is an in-app enable + controllers-down test, not a patch.
