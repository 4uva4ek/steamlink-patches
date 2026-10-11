# Technical reference

Implementation notes, compatibility details, validation status, and build instructions for the Galaxy XR app patch set (Steam Link VR and 4XVR). For installation and patch selection, see the [README](README.md). The tables below cover the Steam Link VR patch set; the 4XVR set is default-off and not yet released — see its [plan](diagnostics/4xvr-galaxyxr/2026-10-11-4xvr-galaxyxr-diff-and-plan.md) and the README status table.

## Steam Link 2.0.20 / 5001812 and 2.0.21 / 5001968

Both exact bases expose the same 17-patch recommended set and 22 standalone
patches as 2.0.20/5001712. Native addresses, complete-function guards and shader
callers were derived independently for each build. Both use the verified
2-projection helper, mouse-only SDL routing and array-shaped HMD extension
configuration. The 2 blue-noise experiments remain default-off and outside the
bundle. See the [adaptation and validation record](diagnostics/steamlink-legacy-1812-1968/README.md).

## Steam Link 2.0.23 / 5002363

The new exact base supports the 7 modern individual patches and **Galaxy XR recommended set (2.0.23/5002363)**, with independently verified native addresses and stock Java startup. Device identity remains optional. The 14 legacy patches stay excluded. See the [5002363 audit](diagnostics/steamlink-5002363/README.md) for source APK provenance, exact addresses, mutation/option tests, APK patching and remaining runtime gaps.

## Compatibility and implementation notes

The Android-Surface Fovea experiment was **tried and retired on 2026-09-07** after
the user reported it did not work. It copied actual fovea pixels into a Surface,
kept 3 projections and omitted the dummy quad. Its failure cause is unknown; the
working terminal-quad fix is unchanged. See the [retirement record](diagnostics/steamlink-surface-fovea/README.md).

Steam Link VR (`com.valvesoftware.steamlinkvr`) was not built for Android XR. These patches adapt it to run on the Samsung Galaxy XR headset by injecting the missing OpenXR permissions and features, bundling the Galaxy XR XR-bridge native library, providing an optional standalone face-bridge layer for face-tracking, fixing broken permission flows, tuning the rendering pipeline, and optionally allowing the patched APK to coexist with the original install.

Target APK: `com.valvesoftware.steamlinkvr`. Exact compatibility metadata and guarded adaptations include v2.0.20/5001712, v2.0.20/5001812, v2.0.21/5001968, v2.0.22/5002244, and v2.0.23/5002363. The permission-free high-resolution fix accepts exactly those 5 builds; 5001712 and 5002244 have verified layouts, while 5002363 is a decoded-base adaptation with pending headset validation. Reconstruction, quad-view, permission-matrix, warm-up/omit, and DFR re-arm experiments are retired.

Use Morphe Manager 1.30.0 or newer with compatibility checks enabled for build-specific filtering. Manager 1.7 cannot distinguish APKs that share versionName `2.0.22`, and Expert mode may intentionally show incompatible patches. Morphe's `default` flag is global, so 5 exact-build dependency bundles provide version-aware recommendations while every individual patch remains default-off. All 4 legacy bundles (2.0.20/5001712, 2.0.20/5001812, 2.0.21/5001968 and 2.0.22/5002244) select the same [17-patch set](PATCH_CATALOG.md#recommendation-bundles), including the 3 native force-gate patches, required XR foundation patches, Device identity with the Meta Quest Pro spoof, and both explicit older startup patches. 2.0.23/5002363 selects only GXR tongue bridge (native face tracking), Galaxy XR high-resolution fix, Microphone input preset (Voice Recognition), OLED color calibration (`final-balanced`), Unrestricted battery usage, and Visual Delay Fix (60 ms). The full GXR face bridge is restricted to the 4 legacy builds so it cannot replace Valve's native 5002363 face mappings. **Appear on top (legacy)** and **Change package name** are never recommended.

**Startup permission requests (before 5002322)** and **Startup splash and XR launch mode (before 5002322)** are independently selectable and default-off. Each accepts only exact 2.0.20/5001712 and 2.0.22/5002244. The permission patch declares missing hand, eye, face, microphone and Bluetooth permissions and enables their startup requests. The splash patch enables the black startup screen and unmanaged VRLink start-mode override; older legacy builds also receive panel sizing. The shared helper defaults both behaviors off. Earlier battery/overlay settings flows use a transparent bootstrap unless those explicit patches are selected.

For exact 2.0.23/5002363, the recommended set preserves stock launcher, splash, XR start mode and runtime permission handling. The battery patch alone inserts a battery-settings helper call after stock `SteamLink.onCreate` calls its superclass, before parameter registers are reused. Face/tongue bridges and high resolution no longer pull in startup permission requests or splash changes; high resolution no longer forces unmanaged Full Space.

For the legacy bundles, **2.0.20/5001712**, **2.0.20/5001812**, **2.0.21/5001968**, and **2.0.22/5002244**, leave **HMD identity** on **Recommended for this build** or explicitly choose **Meta Quest Pro**. The spoof reports `Oculus Quest Pro` while retaining Galaxy XR tracking/controller routing. Existing saved explicit Samsung, Stock, or Pico choices are respected, so change those if necessary. On 2.0.23/5002363, Device identity remains optional and resolves to the Galaxy XR identity.

Bundle selection does not broaden native compatibility: the high-resolution edit safely skips instead of guessing a native layout, and unsupported builds remain unchanged. All 4 exact legacy targets have the complete legacy set available.

**The standalone Video dither patch remains retired.** The existing OLED patch now offers optional Comparison dithering (Off, Low, Standard), a Neutral calibration profile, and sRGB8/RGB10/experimental FP16 output. Defaults are Final balanced, 8-bit sRGB, and dithering Off. RGB10 and FP16 remain optional. See the [controlled comparison guide](PATCH_CATALOG.md#controlled-oled-comparison).

### Retired underside experiment — 2026-09-03

The user reported that the 2.0.22/5002322 Surface-backed underside experiment
"doesn't work" and requested retirement. Its option, implementation, build target
and bundled resources are removed. No new capture was reviewed, so the cause and
GPU effect remain undetermined. The tested terminal quad is the only active mode;
both existing helper binaries, including 2.0.20/5001712, remain unchanged.

### Terminal-quad helper CPU revision — 2026-09-03

The shared resolution patch now supplies 3-projection helper `v1.4-20260903` and 5001712 2-projection helper `v1.2-20260903`. Its generation-validated, non-owning render-thread cache removes steady-frame shared-ownership reference-count operations while the registry retains session ownership. It relies on OpenXR's externally synchronized destruction; lifecycle changes invalidate cached handles. Event readers still acquire ownership, and `xrPollEvent` dispatch processes data only for `XR_SUCCESS`, never `XR_EVENT_UNAVAILABLE`. Existing recommendation bundles automatically receive these helpers through their unchanged resolution-patch dependency.

Host CTest registry and actual 2-/3-projection helper integration checks passed, as did both Android rebuilds using NDK `27.2.12479018` and cached OpenXR `1.1.43` headers. These revisions have no headset result or measured speed gain yet. All 6 exact supported version/build pairs, continuous 2x2 quad submission, source formats, and GPU work remain unchanged; this does not enable 10-bit output. Compare against the previous artifact with the same base, scene, options, and host settings, checking cold launch, stop/restart, palm/DFR transitions, and loss/resume. See the [patch catalog](PATCH_CATALOG.md#galaxy-xr-high-resolution-3-projection-fix-xrgalaxyxrhighresolutionpatch) for validation boundaries and A/B guidance.


## Additional setup context

The previous README described 2.0.20/5001712 as more stable according to community reports and 2.0.22/5002244 as newer but less stable. This is historical community reporting, not a controlled comparison or a guarantee for a particular setup. The depot commands for both builds remain in the [installation instructions](README.md#1-get-the-steam-link-vr-apk).

No desktop IP, pairing token, APK hash, or native telemetry enrollment is required to apply the recommended patches.

## Full patch list

This section is generated from the patch catalog during releases.

<!-- PATCHES_START EXPANDED -->
> **[v1.28.0-dev.6](https://github.com/AngelDark92/xr-porting-hub/releases/tag/v1.28.0-dev.6)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;36 patches total
<details open>
<summary>📦 Steam Link&nbsp;&nbsp;•&nbsp;&nbsp;26 patches</summary>
<br>

**🎯 Supported versions:**

| 2.0.20 (5001712) | 2.0.20 (5001812) | 2.0.21 (5001968) | 2.0.22 (5002244) | 2.0.23 (5002363) | 2.0.23 (5002363) | 2.0.20 (5001712) | 2.0.20 (5001812) | 2.0.21 (5001968) | 2.0.22 (5002244) | 2.0.23 (5002363) |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Verified Steam Link 2.0.20 build 5001712. | Native and APK adaptation for exact Steam Link 2.0.20 build 5001812; headset validation pending. | Native and APK adaptation for exact Steam Link 2.0.21 build 5001968; headset validation pending. | Verified Steam Link 2.0.22 build 5002244. | Build 5002363 recommends Galaxy XR high-resolution 3-projection fix, GXR tongue bridge, Microphone input preset (Voice Recognition), Unrestricted battery usage, Visual Delay Fix (60 ms), and OLED color calibration with the Final balanced tested profile. The retired projection experiments are excluded. Decoded-base adaptation; headset validation pending. | Native and APK adaptation for exact Steam Link 2.0.23 build 5002363; headset validation pending. | Exact Steam Link 2.0.20/5001712 high-resolution target with its isolated 2-projection to 3-layer payload. The topology correction has prior user-reported startup and delayed-frame runtime evidence; this rebuilt binary remains uninstalled. | Static decoded-base adaptation of the Galaxy XR high-resolution patch for exact Steam Link 2.0.20 build 5001812; headset validation pending. | Static decoded-base adaptation of the Galaxy XR high-resolution patch for exact Steam Link 2.0.21 build 5001968; headset validation pending. | Static decoded-base adaptation of the Galaxy XR high-resolution patch for exact Steam Link 2.0.22 build 5002244; headset validation pending. | Static decoded-base adaptation of the Galaxy XR high-resolution patch for exact Steam Link 2.0.23 build 5002363; headset validation pending. |

#### Recommended sets

| 💊&nbsp;Patch | 📜&nbsp;Description | 🔢&nbsp;Builds | ⚙️&nbsp;Options |
|----------|----------------|----------------|-----------|
| [Galaxy XR legacy foundation (through 2.0.22/5002244)](#galaxy-xr-legacy-foundation-through-2-0-22-5002244) | Selects the 17-patch Galaxy XR legacy set for builds through 2.0.22/5002244: identity, native gates, face bridge, OLED, microphone, battery, Visual Delay, startup permissions and splash. | 5002244 |  |
| [Galaxy XR recommended set (2.0.20/5001712)](#galaxy-xr-recommended-set-2-0-20-5001712) | Applies the 17-patch Galaxy XR set for exact 2.0.20/5001712: startup permissions and splash, Meta Quest Pro identity, high-resolution fix and the tested balanced OLED profile. | 5001712 |  |
| [Galaxy XR recommended set (2.0.20/5001812)](#galaxy-xr-recommended-set-2-0-20-5001812) | Applies the same 17-patch set as 5001712, for exact 2.0.20/5001812. Headset validation pending. | 5001812 |  |
| [Galaxy XR recommended set (2.0.21/5001968)](#galaxy-xr-recommended-set-2-0-21-5001968) | Applies the same 17-patch set as 5001712, for exact 2.0.21/5001968. Headset validation pending. | 5001968 |  |
| [Galaxy XR recommended set (2.0.23/5002363)](#galaxy-xr-recommended-set-2-0-23-5002363) | Applies the 6-patch Galaxy XR set for exact 2.0.23/5002363: high resolution, tongue bridge, battery page, Visual Delay and the balanced OLED profile. Keeps stock startup permissions. Headset validation pending. | 5002363 |  |

#### Image quality

| 💊&nbsp;Patch | 📜&nbsp;Description | 🔢&nbsp;Builds | ⚙️&nbsp;Options |
|----------|----------------|----------------|-----------|
| [Galaxy XR high-resolution 3-projection fix](#galaxy-xr-high-resolution-3-projection-fix) | Permission-free high-resolution fix: appends a static 2x2 Android-surface compositor trigger and leaves each build's native projection layers and formats, including future RGB10_A2, untouched. No image copy. Exact builds 5001712, 5001812, 5001968, 5002244, 5002363. | 5001712, 5001812, 5001968, 5002244, 5002363 |  |
| [OLED color calibration](#oled-color-calibration) | Applies OLED gamma/saturation calibration with 8-bit sRGB output, plus optional VD-like SDR foveal processing and a separate foveal gamma. Exact builds 5001712, 5001812, 5001968, 5002244, 5002363; visible improvement needs headset verification. | 5001712, 5001812, 5001968, 5002244, 5002363 | • Calibration profile<br>• Gamma<br>• Saturation<br>• Fovea VD-Like Input 10 bit<br>• Fovea VD-Like Input 8 bit<br>• Foveal gamma adjustment |

#### Tracking & audio

| 💊&nbsp;Patch | 📜&nbsp;Description | 🔢&nbsp;Builds | ⚙️&nbsp;Options |
|----------|----------------|----------------|-----------|
| [Controller velocity fix](#controller-velocity-fix) | Derives controller linear and angular velocity from grip/aim pose history, and can cut VRLink's 4 controller pose sends per display frame to 2 or 1. | 5001712, 5001812, 5001968, 5002244 | • Maximum sample gap (ms)<br>• Controller pose-send cadence<br>• Derived velocity smoothing<br>• Maximum linear speed (m/s)<br>• Maximum angular speed (rad/s) |
| [GXR face bridge (version 5002318 and below)](#gxr-face-bridge-version-5002318-and-below) | Installs libgxr_face_bridge.so, mapping XR_FB_face_tracking2 to XR_ANDROID_face_tracking, and adds FACE_TRACKING to the manifest on builds up to 5002318. Feeds the Galaxy XR VRCFT module. | 5001712, 5001812, 5001968, 5002244 |  |
| [GXR tongue bridge (version 5002322 and above)](#gxr-tongue-bridge-version-5002322-and-above) | Adds Galaxy XR tongue out/left/right/up/down on exact 2.0.23/5002363, keeping Valve's native face expressions and standard TongueOut, for the Galaxy XR VRCFT module. | 5002363 |  |
| [Microphone input preset](#microphone-input-preset) | Selects Steam Link's AAudio microphone processing mode. On Galaxy XR, Voice Recognition was clearer and louder than stock Voice Communication. | 5001712, 5001812, 5001968, 5002244, 5002363 | • Microphone mode |
| [Visual Delay Fix](#visual-delay-fix) | Offsets the HMD OpenXR pose-query time and zeroes the exported HMD velocity fields, so head tracking leads the rendered frame. Controllers are unaffected. | 5001712, 5001812, 5001968, 5002244, 5002363 | • Pose offset (ms) |
| [XR Input Routing Config](#xr-input-routing-config) | Installs ui_config.json so XR pointer and button input reaches the launcher UI. | 5001712, 5001812, 5001968, 5002244 |  |

#### Startup & permissions

| 💊&nbsp;Patch | 📜&nbsp;Description | 🔢&nbsp;Builds | ⚙️&nbsp;Options |
|----------|----------------|----------------|-----------|
| [Appear on top (legacy)](#appear-on-top-legacy) | Legacy overlay fallback for older builds: adds SYSTEM_ALERT_WINDOW and the compositor signal window. | 5001712, 5001812, 5001968, 5002244 |  |
| [Startup permission requests (before 5002322)](#startup-permission-requests-before-5002322) | Requests hand, eye, face, microphone and Bluetooth permissions at startup on older builds. Battery settings and the splash are separate patches. | 5001712, 5001812, 5001968, 5002244 |  |
| [Startup splash and XR launch mode (before 5002322)](#startup-splash-and-xr-launch-mode-before-5002322) | Adds the Launching Steam Link splash and unmanaged VRLink startup on older builds. Requests no permissions; pick Startup permission requests for those. | 5001712, 5001812, 5001968, 5002244 |  |
| [Unrestricted battery usage](#unrestricted-battery-usage) | Opens Android's Battery usage page at startup so Unrestricted can be selected for XR streaming. | 5001712, 5001812, 5001968, 5002244, 5002363 |  |

#### App & device identity

| 💊&nbsp;Patch | 📜&nbsp;Description | 🔢&nbsp;Builds | ⚙️&nbsp;Options |
|----------|----------------|----------------|-----------|
| [Change package name](#change-package-name) | Renames the package and Steam Link's internal VR-launch component, so the patched app can be installed next to the original. | 5001712, 5001812, 5001968, 5002244 | • Package name |
| [Device identity](#device-identity) | Overrides the HMD identity reported to SteamVR. Recommended: Meta Quest Pro on the legacy targets through 5002244, Galaxy XR otherwise. Explicit profiles override that. Optional on 2.0.23/5002363. | 5001712, 5001812, 5001968, 5002244, 5002363 | • HMD identity |

#### Advanced XR compatibility

| 💊&nbsp;Patch | 📜&nbsp;Description | 🔢&nbsp;Builds | ⚙️&nbsp;Options |
|----------|----------------|----------------|-----------|
| [Android XR native permission names](#android-xr-native-permission-names) | Rewrites native Oculus face/eye permission checks to the Android XR names, on exact 2.0.20/5001712, 2.0.20/5001812, 2.0.21/5001968 and 2.0.22/5002244 builds. | 5001712, 5001812, 5001968, 5002244 |  |
| [Force HMD initialization gates](#force-hmd-initialization-gates) | Bypasses the 2 capability gates in QSVLDeviceHmd::Init, on exact 2.0.20/5001712, 2.0.20/5001812, 2.0.21/5001968 and 2.0.22/5002244 builds. | 5001712, 5001812, 5001968, 5002244 |  |
| [Force lobby permission-state gate](#force-lobby-permission-state-gate) | Bypasses the permission-state gate in XrSceneLobby, on exact 2.0.20/5001712, 2.0.20/5001812, 2.0.21/5001968 and 2.0.22/5002244 builds. | 5001712, 5001812, 5001968, 5002244 |  |
| [Force stream XR gates](#force-stream-xr-gates) | Bypasses the 3 XR gates in the stream path, on exact 2.0.20/5001712, 2.0.20/5001812, 2.0.21/5001968 and 2.0.22/5002244 builds. | 5001712, 5001812, 5001968, 5002244 |  |
| [XR Core Runtime](#xr-core-runtime) | Installs the XR runtime bridge resources and extension DEX that the other XR patches need. | 5001712, 5001812, 5001968, 5002244 |  |
| [XR Device Config Baseline](#xr-device-config-baseline) | Installs baseline Galaxy XR HMD, controller and default config payloads plus the dashboard bootstrap assets. | 5001712, 5001812, 5001968, 5002244 |  |
| [XR Manifest Capability Pack](#xr-manifest-capability-pack) | Adds the Android XR/OpenXR permissions, features and app properties Steam Link needs. | 5001712, 5001812, 5001968, 5002244 |  |

</details>

<details open>
<summary>📦 Steam Link Experimental&nbsp;&nbsp;•&nbsp;&nbsp;10 patches</summary>
<br>

**🎯 Supported versions:**

| 2.0.20 (5001812) | 2.0.21 (5001968) | 2.0.20 (5001712) | 2.0.22 (5002244) | 2.0.23 (5002363) | 2.0.20 (5001712) | 2.0.20 (5001812) | 2.0.21 (5001968) | 2.0.22 (5002244) | 2.0.23 (5002363) | 2.0.20 (5001712) | 2.0.20 (5001812) | 2.0.21 (5001968) | 2.0.22 (5002244) | 2.0.23 (5002363) | 2.0.23 (5002363) | 2.0.20 (5001712) | 2.0.20 (5001812) | 2.0.21 (5001968) | 2.0.22 (5002244) | 2.0.20 (5001812) |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Exact native blue-noise adaptation 2.0.20/5001812; static validation only. | Exact native blue-noise adaptation 2.0.21/5001968; static validation only. | Exact native blue-noise adaptation 2.0.20/5001712; static validation only. | Exact native blue-noise adaptation 2.0.22/5002244; static validation only. | Exact native blue-noise adaptation 2.0.23/5002363; static validation only. | Experimental patches for Steam Link 2.0.20 build 5001712. | Experimental patches for Steam Link 2.0.20 build 5001812. | Experimental patches for Steam Link 2.0.21 build 5001968. | Experimental patches for Steam Link 2.0.22 build 5002244. | Experimental patches for Steam Link 2.0.23 build 5002363. | Controller pose extrapolation layer for exact Steam Link 2.0.20/5001712; measured on 2.0.23/5002363 only, not run on this base. | Controller pose extrapolation layer for exact Steam Link 2.0.20/5001812; measured on 2.0.23/5002363 only, not run on this base. | Controller pose extrapolation layer for exact Steam Link 2.0.21/5001968; measured on 2.0.23/5002363 only, not run on this base. | Controller pose extrapolation layer for exact Steam Link 2.0.22/5002244; measured on 2.0.23/5002363 only, not run on this base. | Controller pose extrapolation layer for exact Steam Link 2.0.23/5002363; measured on a Galaxy XR headset. | Controller velocity frame layer for exact Steam Link 2.0.23/5002363; measured on a Galaxy XR headset. | Controller velocity frame layer for exact Steam Link 2.0.20/5001712; measured on 2.0.23/5002363 only, not run on this base. Do not combine with Controller velocity fix. | Controller velocity frame layer for exact Steam Link 2.0.20/5001812; measured on 2.0.23/5002363 only, not run on this base. Do not combine with Controller velocity fix. | Controller velocity frame layer for exact Steam Link 2.0.21/5001968; measured on 2.0.23/5002363 only, not run on this base. Do not combine with Controller velocity fix. | Controller velocity frame layer for exact Steam Link 2.0.22/5002244; measured on 2.0.23/5002363 only, not run on this base. Do not combine with Controller velocity fix. | Native and APK adaptation for exact Steam Link 2.0.20 build 5001812; headset validation pending. |

#### Experiments

| 💊&nbsp;Patch | 📜&nbsp;Description | 🔢&nbsp;Builds | ⚙️&nbsp;Options |
|----------|----------------|----------------|-----------|
| [Background blue-noise dithering (experimental)](#background-blue-noise-dithering-experimental) | Adds static blue noise after video colour processing and fade, on the background/base layer only; 8-bit or 10-bit input, always 8-bit sRGB output. Selectable per layer, but turn OLED's VD-like fovea options off when combining. Unknown host shaders pass through unchanged. | 5001712, 5001812, 5001968, 5002244, 5002363 | • Declared input depth |
| [Controller grip haptics through Shizuku (experimental)](#controller-grip-haptics-through-shizuku-experimental) | Routes controller vibration to the grip vibrator instead of the trigger, which Galaxy XR uses for every OpenXR vibration. The grip one needs shell rights, so this needs Shizuku running with its permission granted to Steam Link; without it vibration is stock. | 5001712, 5001812, 5001968, 5002244, 5002363 |  |
| [Controller pose extrapolation (experimental)](#controller-pose-extrapolation-experimental) | Makes the Galaxy XR runtime extrapolate controller poses to the time VRLink asks, instead of repeating one sample per display frame, and filters jitter out of the streamed pose. Adds an OpenXR API layer; no Steam Link code changes. | 5001712, 5001812, 5001968, 5002244, 5002363 |  |
| [Controller tracking from the controller HAL through Shizuku (experimental)](#controller-tracking-from-the-controller-hal-through-shizuku-experimental) | Takes controller poses straight from the Galaxy XR controller HAL instead of the OpenXR runtime, removing the runtime's ~35 ms sample lag, and filters jitter out of pose and velocity. The HAL needs shell rights, so this needs Shizuku running with its permission granted to Steam Link; without Shizuku tracking stays as it is. Steam Link 2.0.23: angular velocity is reported local to the grip pose. | 5002363 |  |
| [Controller tracking from the controller HAL through Shizuku, 2.0.20 - 2.0.22 (experimental)](#controller-tracking-from-the-controller-hal-through-shizuku-2-0-20-2-0-22-experimental) | Takes controller poses straight from the Galaxy XR controller HAL instead of the OpenXR runtime, removing the runtime's ~35 ms sample lag, and filters jitter out of pose and velocity. The HAL needs shell rights, so this needs Shizuku running with its permission granted to Steam Link; without Shizuku tracking stays as it is. Steam Link 2.0.20 - 2.0.22: angular velocity is reported in the base space. | 5001712, 5001812, 5001968, 5002244 |  |
| [Controller velocity frame (experimental)](#controller-velocity-frame-experimental) | Rotates the controller velocities the Galaxy XR runtime reports into the frames SteamVR reads, so thrown objects leave in the right direction. Stock, they arrive in a controller-attached frame. Adds an OpenXR API layer; no Steam Link code is changed. Steam Link 2.0.23: angular velocity is reported local to the streamed pose. | 5002363 |  |
| [Controller velocity frame, 2.0.20 - 2.0.22 (experimental)](#controller-velocity-frame-2-0-20-2-0-22-experimental) | Rotates the controller velocities the Galaxy XR runtime reports into the frames SteamVR reads, so thrown objects leave in the right direction. Stock, they arrive in a controller-attached frame. Adds an OpenXR API layer; no Steam Link code is changed. Steam Link 2.0.20 - 2.0.22: angular velocity is reported in the base space. | 5001712, 5001812, 5001968, 5002244 |  |
| [Foveal blue-noise dithering (experimental)](#foveal-blue-noise-dithering-experimental) | Adds static blue noise after video colour processing and fade, on the foveal layer only; 8-bit or 10-bit input, always 8-bit sRGB output. Selectable per layer, but turn OLED's VD-like fovea options off when combining. Unknown host shaders pass through unchanged. | 5001712, 5001812, 5001968, 5002244, 5002363 | • Declared input depth |
| [Full-FOV foveal canvas (experimental)](#full-fov-foveal-canvas-experimental) | Only for exact 2.0.20/5001812: draws just the foveal image on a transparent 5000x6000 canvas per eye and submits it as the 2nd projection, keeping the background projection and any static trigger quad. Requires headset validation. | 5001812 |  |
| [Uber link rescan disable (experimental)](#uber-link-rescan-disable-experimental) | Stops the Uber UDP transport's post-startup link rescans on exact Steam Link 2.0.23/5002363. The stream then cannot discover new links, so do not switch networks while streaming. Experimental; headset validation pending. | 5002363 |  |

</details>

<!-- PATCHES_END -->

## Building from source

On Windows, verify JVM tests and build the Android bundle with automatic cleanup:

```powershell
.\Verify-Build.ps1
.\Verify-Build.ps1 -Tasks ':patches:test', ':patches:auditDecodedSteamLinkPatches'
```

Each run uses a fresh workspace. Bundles remain under
`../builds/steamlink-patches/build/verification/<run>/artifacts/patches/libs`, alongside retained test reports,
the Gradle log and a cleanup receipt. Compiler files and decoded audit derivatives
are removed after Gradle exits, including on failure. Pass `-KeepBuildOutputs` to
retain intermediates. Exact fixtures, tools, caches and existing outputs are
preserved. See [verification and disk cleanup](diagnostics/build-verification/README.md).

Direct Gradle commands use the external output folders described below;
they do not perform the wrapper's automatic cleanup:

```
./gradlew buildAndroid
```

Output: `../builds/steamlink-patches/gradle/patches/libs/patches-*.mpp`

All generated output and project Gradle/Kotlin state live beneath
`../builds/steamlink-patches/`. Retained audit fixtures/tools/evidence use its
`build/` directory; Gradle uses separate `gradle/root` and `gradle/patches`
directories so `clean` preserves those inputs. Native CMake output uses
`extensions/<name>/build-*` under the same external root. See the
[2026-10-08 relocation record](diagnostics/build-layout/README.md).

No Android SDK is required. The extension DEX is assembled from smali sources directly by the build.

## Documentation maintenance

Release automation refreshes the full patch list in this file and commits it with the release metadata. To refresh it locally from an existing catalog:

```powershell
$env:PYTHONIOENCODING = 'utf-8'
python .github/scripts/generate_patches_readme.py AngelDark92/steamlink-patches main patches-list-all.json TECHNICAL_REFERENCE.md
```

The generator also defaults to `TECHNICAL_REFERENCE.md` when no output path is supplied. Keep the README focused on user setup; update its bundle guidance when patch recommendations change.

## Further reference

- [Patch catalog](PATCH_CATALOG.md): per-patch edits, dependencies, guarded layouts, and controlled OLED comparison instructions.
- [Debugging guide](DEBUGGING.md): diagnostic capture procedures.
- [Changelog](CHANGELOG.md): release history.
