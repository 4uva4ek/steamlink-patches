# 4XVR input modes and startup popups (2026-10-12)

Device: Galaxy XR `R3GYB09JA5E`. Target: `cn.vr4p.oculus4xvrplayerov` 2.0.22/20022.
Stock tree: `4xvr-orig/`. Reference port tree: `4XVR-GalaxyXR/`. Evidence:
`../builds/steamlink-patches/build/4xvr-input-20261012/` — `popup1.png`, `popup2.png`, `after-back.png`,
`state1.png`, `repro-before-tap.png`, `live-popup.log` (`:17235` `Dropping event because there is no
touchable window at (3774.0, 1923.0) on display 0`, 17:11:44.838), `pre-popup.log`, `repro.log`,
`activity-top.txt`, `windows.txt`, `ui.xml`, `pm-list-features.txt`, `pm-list-permissions.txt`,
`device-fingerprint.txt`; the OpenXR runtime shims `libopenxr.google.so` / `libopenxr_android.so`.

## Findings

**Hands and gaze never reach the app (controllers only).** 4XVR declares only the Meta input
contract (`AndroidManifest.xml:4-6`: `android.hardware.vr.headtracking`, `com.oculus.permission
.HAND_TRACKING`, `oculus.software.handtracking`) and never declares Android XR's input features.
Steam Link VR 2.0.23 declares them all — `android.hardware.xr.input.controller`,
`.hand_tracking`, `.eye_tracking` (all `required=true`, `AndroidManifest.xml:41-43`) plus
`android.permission.HAND_TRACKING` / `EYE_TRACKING_FINE` (`:44-45`) — and binds
`/interaction_profiles/ext/hand_interaction_ext` (hand aim → pointer, `aim_activate_ext` →
select) and `/interaction_profiles/ext/eye_gaze_interaction` in its config/native
(`assets/config/{controller,ui,hmd}_config.json`). The Galaxy XR platform advertises
`android.hardware.xr.input.{controller,hand_tracking,eye_tracking}` (`pm-list-features.txt:47-49`); the
AOSP permissions `HAND_TRACKING` / `EYE_TRACKING_FINE` are *not defined* on this Samsung build
(`pm-list-permissions.txt`, 1820 lines, 0 hits for either), so declaring them here is inert but
future-proof.
The app's own mode switch is `V4Config.m_bHandTracking` (prefs `VR4PMoviePlayer`/`HandTracking`,
pushed once per `InitRender` via `Main4XActivity.SettingHandTracking(Z)`), and its Control
setting ("The control method when the Quest controller is not detected", `res/values
/strings.xml:536`) only matters with the controllers idle. Its native lib requests
`XR_EXT_hand_tracking` + `XR_FB_hand_tracking_aim/capsules/mesh`; no eye-gaze extension is
referenced anywhere, so the "Eye Gaze" card is an app-internal head-gaze mode, not OpenXR gaze.

**The three startup popups never clear in-session.** `Main4XActivity.lambda$InitOerator$46`
evaluates them exactly once per process (permission → release notes → tutorial, each returns):
- Permission: gated on `Environment.isExternalStorageManager()` (`Main4XActivity.smali:10176`); the
  dialog's only button hides the window unconditionally (`tool/V4PermissionsUI.smali:41`) before
  opening its All-files-access settings page with request `0x59E2` (`:78-80`). A prompt that stays on
  screen therefore means the press never landed; the result of `0x59E2` is never re-checked —
  `Main4XActivity.onActivityResult` (`:13208`) has no `0x59E2` branch (only `V4UsbBaseView` request
  `0x59E0` is delegated) and no lifecycle callback re-evaluates the gate, so access granted outside
  the dialog leaves the prompt up for the rest of the session.
- Release notes: `SaveVersion()` writes `4XVersionSave`/`Version` *before* `SetVisible()`
  (`:10235-10243`), and the show condition is `saved/100 < versionCode/100` (`:10205-10232`).
- Tutorial: `HaveShowGuide=true` is written before `ShowDlg(0, …)` (`:10278-10297`). Its Skip /
  Next handlers are state-correct (`Dialog/V4HelpGuideDlg.smali:409/423/437`: `SetHide()` then
  `listener.onClick(bNext)`; every listener guard `if-eqz p1` makes `onClick(false)` a no-op by
  design). The card visible on the headset was page 2, "Pause / play"
  (`ShowDlg` tab 2, `0x7f0f0030` / `0x7f0f002a`).
- A hover/pointer press was dropped by the system at 17:11:44 (`live-popup.log:17235`), which is why
  nothing on screen responded: (3774, 1923) sits at the centre of the 7104x3840 system desktop and
  outside the app's window `(0,36)-(1536,996)`, so the dropped event is the desktop pointer stream,
  not the XR ray the app consumes (`:17255` adds `the hovering pointer is not in any windows in
  display 0`). Hypothesis: presses reach the app only when the platform routes them as XR input, and
  the app declares none of the Android XR input features the runtime gates that routing on.

## Changes in this commit

- `Galaxy XR launch compatibility (4XVR)` now also injects the Android XR input contract into the
  manifest: `android.hardware.xr.input.{controller,hand_tracking,eye_tracking}` (all
  `required=false`) and `android.permission.HAND_TRACKING` / `EYE_TRACKING_FINE`; fixture test
  extended (5 features, 2 permissions, idempotent).
- New patch `Popup refresh on resume (4XVR)` (`PopupRefreshPatch.kt`, default on, guarded against
  reapplication): injects `cn.vr4p.patch.PopupRefresh.onHostResume(activity)` at the head of
  `Main4XActivity.onResume()` and `onActivityResult(...)` (index 0 is not a label/branch target and
  neither method has a try range). The helper hides the permission window while
  `isExternalStorageManager()` is true (field `m_PermissionsUI` read reflectively; the class has
  no getter) and hides the release-notes window when its saved version already covers the
  installed build (public `GetV4VersionUpdateDlg()` + `4XVersionSave`/`Version`).
- This is a state-refresh safety net, not the press-delivery fix: it clears both windows at the next
  resume or activity result. The tutorial card is deliberately untouched (its Skip/Next handlers
  already hide the window; the press is what never arrived), and the delivery hypothesis is the
  input contract above.
- `TECHNICAL_REFERENCE.md` / `README.md` regenerated from `patches-list.json` (44 patches total,
  4XVR 15) and all four catalogs carry the new patch with `default: true`.

## Checks / open items

- Executed here: `:patches:assembleFourXvrExtension` (helper assembles; the built
  `fourxvr-extension.mpe` contains `Lcn/vr4p/patch/PopupRefresh;`),
  `:patches:test` 169 tests / 0 failures / 0 errors / 13 skipped (in-place, Windows,
  JAVA_HOME=F:/Runtimes/Java21, `--offline`), `:patches:generatePatchesList
  -PreleaseChannel=experimental`.
- Verified statically: anchors, access flags (`SetHide`/`IsVisible` public, `m_PermissionsUI`
  package-private with no getter), prefs names and the version comparison, no class-name collision.
- Open: headset re-test after the next Morphe patch run (input features actually unlocking hand /
  gaze routing; popups clearing on return from Settings); whether presses reach the app at all
  without the input declaration is still unproven — the dropped pointer event above is the only
  captured press, and it never reached any window.
