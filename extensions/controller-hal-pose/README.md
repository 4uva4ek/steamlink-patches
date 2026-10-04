# Controller HAL pose layer (2026-10-04)

Source of `libgxr_controller_hal_pose.so` and `extensions/controller-hal-pose.mpe`, installed
by the opt-in patch **Controller tracking from the controller HAL through Shizuku
(experimental)**.

## What it changes

The Galaxy XR runtime hands an application one controller pose per display frame (see the
[extrapolation layer notes](../controller-extrapolation-layer/README.md)). The controller
HAL (`vendor.samsung.hardware.secxrcontroller.ISecXRController/default`) holds more: it
fuses the controller's IMU and answers `getPoseAtTimestamp` (transaction 18) with a pose
predicted for the requested time. The HAL answers a process with shell rights; an
application cannot look it up. So the call is made from a
[Shizuku](https://github.com/RikkaApps/Shizuku) user service:

- The shared [Shizuku bridge](../shizuku-bridge/README.md) binds the user service;
  `java/gxr/pose/PoseBridge` hands its binder to the layer.
- `java/gxr/pose/PoseService` runs in the user service process with shell rights and returns
  the HAL's reply for both controllers for one requested time.
- `src/controller_hal_pose_layer.cpp` is an OpenXR API layer that wraps `xrLocateSpace` for
  the action spaces of VRLink's controller pose action (`pamir-stream-pose`) and reports the
  HAL's pose in place of the runtime's. Velocities, other spaces and hand tracking are not
  touched.

Without Shizuku, without its permission, or while a controller is not tracked, the runtime's
pose is reported unchanged, which is the stock behaviour.

## Measurements (Galaxy XR SM-I610, Steam Link 2.0.23/5002363, 2026-10-04)

The HAL, polled from the shell:

| | Result |
|---|---|
| Distinct poses in 1000 polls per second | 840-990, also with the requested time held for 100 ms |
| Pose differs between "now" and "now + 30 ms" | 90-100 % of pairs |
| One call | 0.3-0.6 ms |
| Clock of the requested time | `CLOCK_MONOTONIC`, nanoseconds |

A request with a time from another clock (hundreds of seconds ahead) made the controllers
freeze in the headset even at 90 calls per second. With the monotonic clock about 1200 HAL
calls per second during a stream caused no freezing.

The HAL's pose against the runtime's, from a 78 s recording of both in a stream:

- On still controllers `runtime grip pose = A * HAL pose * B`. `B` is a pitch of 42.25
  degrees about +X with no offset, the same for both hands (residual 0.04 degrees and
  0.03 mm). `A` is the session's base space in the HAL's world, a yaw and an offset; it
  changed when the session restarted.
- In motion the runtime's pose trailed the HAL's pose for "now" by about 35 ms (regression
  on the HAL's velocities, R2 0.86). Which of the two is closer to the hand was judged by
  feel only, not against an external reference.
- At rest the HAL's pose is as quiet as the runtime's (0.04 mm, 0.02 degrees RMS); in motion
  it carries two to three times more high-frequency content, hence the filter.

## How the pose is produced

- **Base space.** `A` is learned from the runtime's own pose while the controller is nearly
  still (below 0.02 m/s and 0.1 rad/s; looser limits until it is first found), as a slow
  running average. Eight consecutive still samples more than 3 cm or 0.05 rad away replace
  it at once, which follows a recenter or a new session.
- **Reads.** A thread of the layer reads the HAL 1000 times per second while VRLink is
  locating the controllers (912-936 reads per second were reached in a stream). VRLink
  itself asks four times per display frame and gets the latest read. Each request is for
  "now" plus half a read period, the read's own duration and `debug.gxr.halpose.ahead`.
- **Filter.** Every read goes through a low-pass whose cutoff rises with the controller's
  speed, taken from the HAL's own velocities: `cutoff = base + beta * speed`. A resting hand
  is smoothed hard and a fast one barely lags (at most about 2.6 mm and 0.15 degrees with
  the defaults).

The [extrapolation layer](../controller-extrapolation-layer/README.md) has the same filter
for the runtime's pose; it stands down while this layer supplies the pose.

## Properties

`debug.gxr.halpose`, `.ahead`, `.pitch` and `.hz` are read when Steam Link starts; the
filter properties are re-read every second while streaming. Logcat tag: `GxrHalPose`
(a statistics line every 5 s).

| Property | Default | Meaning |
|---|---|---|
| `debug.gxr.halpose` | on | `0` reports the runtime's pose unchanged |
| `debug.gxr.halpose.hz` | 1000 | HAL reads per second by the layer's thread; `0` reads only when VRLink asks |
| `debug.gxr.halpose.ahead` | 1 | Milliseconds added to the requested time |
| `debug.gxr.halpose.pitch` | 42.25 | Pitch of the grip pose against the HAL's pose, degrees |
| `debug.gxr.halpose.filter` | 1 | `0` reports the HAL's pose unfiltered |
| `debug.gxr.halpose.pos.cutoff` | 1.5 | Position cutoff at rest, Hz |
| `debug.gxr.halpose.pos.beta` | 60 | Position cutoff added per m/s, Hz |
| `debug.gxr.halpose.rot.cutoff` | 3 | Rotation cutoff at rest, Hz |
| `debug.gxr.halpose.rot.beta` | 60 | Rotation cutoff added per rad/s, Hz |

The filter values and the 1 ms were chosen by feel on the headset.

## Limits

- Run on 2.0.23/5002363 only. The legacy bases create the same pose action but were not run.
- Velocities still come from the runtime, so they are older than the pose they travel with.
- The pose read costs two HAL calls per read, about 2000 per second.

## Build

Native layer (use a short build directory, the fetched OpenXR-SDK tree is deep):

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$cmake = "$sdk\cmake\3.22.1\bin"
$ndk = "$sdk\ndk\28.2.13676358"
& "$cmake\cmake.exe" -S extensions/controller-hal-pose -B <short build dir> -G Ninja `
    "-DCMAKE_MAKE_PROGRAM=$cmake\ninja.exe" `
    "-DCMAKE_TOOLCHAIN_FILE=$ndk\build\cmake\android.toolchain.cmake" `
    -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29 -DANDROID_STL=c++_static `
    -DCMAKE_BUILD_TYPE=Release
& "$cmake\cmake.exe" --build <short build dir> --target gxr_controller_hal_pose
```

Java extension (the Shizuku API itself is in the Shizuku bridge's extension):

```powershell
javac --release 8 -cp "<android.jar>" -d classes java/gxr/pose/*.java
jar cf gxr.jar -C classes gxr
d8 --release --min-api 29 --lib <android.jar> --output <dir> gxr.jar
```

Copy `libgxr_controller_hal_pose.so` to `patches/src/main/resources/steamlink/androidxr/` and
`classes.dex` to `patches/src/main/resources/extensions/controller-hal-pose.mpe`, then
update both SHA-256 values in `ControllerHalPosePatchTest`.
