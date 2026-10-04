# Controller grip haptics (2026-10-04)

Source of `libgxr_haptic_main.so` and `extensions/controller-grip-haptics.mpe`, installed by
the opt-in patch **Controller grip haptics through Shizuku (experimental)**.

## What it changes

A Galaxy XR controller has two vibrators: one at the trigger (`SUB`) and one in the grip
(`MAIN`). The system controller service (`com.sec.android.secxrcontrollerservice`) sends
every OpenXR `XrHapticVibration` to `SUB`, so in Steam Link only the trigger vibrates. No
setting or system property selects the other one.

`MAIN` is reachable through the controller HAL
(`vendor.samsung.hardware.secxrcontroller.ISecXRController/default`, `performHapticFeedback`
= transaction 21, `stopHapticFeedback` = 22), which takes the vibrator as an argument. The
HAL answers a process with shell rights; an application cannot look it up. Tested from
inside Steam Link on 2026-10-04: the service lookup returns nothing, and the runtime does
not offer `XR_FB_haptic_pcm` (`xrCreateInstance` fails with `XR_ERROR_EXTENSION_NOT_PRESENT`).

So the call is made from a [Shizuku](https://github.com/RikkaApps/Shizuku) user service:

- `java/gxr/haptic/HapticProvider` (a `ShizukuProvider`) asks Shizuku for permission when
  Steam Link starts, binds the user service and hands its binder to the layer.
- `java/gxr/haptic/HapticService` runs in the user service process with shell rights and
  relays `vibrate` / `stop` to the HAL.
- `src/controller_grip_haptics_layer.cpp` is an OpenXR API layer that wraps
  `xrApplyHapticFeedback` and `xrStopHapticFeedback`. While the user service is connected it
  sends `XrHapticVibration` to `MAIN` instead of the runtime.

Without Shizuku, without its permission, or when a call fails, the layer passes the
vibration to the runtime unchanged, which is the stock behaviour.

## Mapping

The stock service clamps amplitude to 0.1..0.8, turns the frequency into a step
`round(Hz / 50)` clamped to 1..10 and uses a 30 ms minimum duration. At those values the
grip vibrator is barely noticeable, so the layer uses its own defaults, picked by feel on a
Galaxy XR (SM-I610) with Steam Link 2.0.23/5002363 on SteamVR dashboard ticks (21 ms,
amplitude 0.16) and in Beat Saber:

| Property | Default | Meaning |
|---|---|---|
| `debug.gxr.haptic` | `1` | `0` OpenXR only, `1` grip, `2` grip and trigger |
| `debug.gxr.haptic.gain` | `5` | amplitude multiplier before the 0.1..0.8 clamp |
| `debug.gxr.haptic.freq` | `2` | HAL frequency step 1..10; `0` derives it from the OpenXR frequency. Step 3 is already shrill |
| `debug.gxr.haptic.minms` | `60` | shortest pulse in milliseconds |

Set with `adb shell setprop`; the layer re-reads them twice a second while streaming.
Logcat tag: `GxrHapticMain` (first 40 vibrations are logged).

With gain 5 any amplitude above 0.16 reaches the 0.8 limit, so weak and strong effects
feel alike.

The legacy bases (5001712, 5001812, 5001968, 5002244) call the same two OpenXR functions
with the same `/user/hand/left|right` subaction paths, but the patch was run on a headset
only with 5002363.

## Build

Native layer (use a short build directory, the fetched OpenXR-SDK tree is deep):

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$cmake = "$sdk\cmake\3.22.1\bin"
$ndk = "$sdk\ndk\28.2.13676358"
& "$cmake\cmake.exe" -S extensions/controller-grip-haptics -B <short build dir> -G Ninja `
    "-DCMAKE_MAKE_PROGRAM=$cmake\ninja.exe" `
    "-DCMAKE_TOOLCHAIN_FILE=$ndk\build\cmake\android.toolchain.cmake" `
    -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29 -DANDROID_STL=c++_static `
    -DCMAKE_BUILD_TYPE=Release
& "$cmake\cmake.exe" --build <short build dir> --target gxr_haptic_main
```

Java extension: take `classes.jar` out of the `api`, `provider`, `aidl` and `shared` AARs of
`dev.rikka.shizuku` 13.1.5 (Maven Central, Apache-2.0), then

```powershell
javac --release 8 -cp "<android.jar>;api.jar;provider.jar;aidl.jar;shared.jar" -d classes java/gxr/haptic/*.java
jar cf gxr.jar -C classes gxr
d8 --release --min-api 29 --lib <android.jar> --output <dir> gxr.jar api.jar provider.jar aidl.jar shared.jar
```

Copy `libgxr_haptic_main.so` to `patches/src/main/resources/steamlink/androidxr/` and
`classes.dex` to `patches/src/main/resources/extensions/controller-grip-haptics.mpe`, then
update both SHA-256 values in `ControllerGripHapticsPatchTest`.
