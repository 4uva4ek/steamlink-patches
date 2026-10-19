# Foveal canvas — on-device crash RCA and fix (2026-10-12)

Trigger: Steam Link 2.0.20/5001812 patched build (installed 15:48 on `R3GYB09JA5E`) crashed on every start ("crashing on PC connection"). Default-off `Full-FOV foveal canvas (experimental)` was selected. This is the experiment's first device run; no device outcome had been recorded before.

## Crash receipt (device, two identical runs: 15:49:40, 15:51:51)

```
F libc: Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0  (pid 21771, tid Thread-4)
F DEBUG: Cause: null pointer dereference
F DEBUG:   x0 0  x1 0x73b921d1de ("xrInitializeLoaderKHR")  x2 &out  x3 0  x8 adrp page
F DEBUG:   pc 0000000000000000   lr 0x73b9238c60 (libopenxr_loader.so+0x36c60)
F DEBUG: #00 pc 0 <unknown>
F DEBUG: #01 pc libopenxr_loader.so+0x36c5c (blr x8; forwarding call)
F DEBUG: #02 pc libopenxr_loader.so+0x3cd44   #03 xrCreateInstance+8
F DEBUG: #04 libvrlink_scene.so XRQCreateXRInstance+2460   #05 XrAppManager::Init+220   #06 AppSetup+420
I OpenXR-Loader: LoadApiLayers succeeded loading layer XR_APILAYER_local_GalaxyXR_face_bridge using interface version 1 and OpenXR API version 1.1   (last loader line before both crashes)
```

Installed APK `implicit.d` order (pulled `base.apk`): `face_bridge` → `foveal_canvas` → `xr_bridge`. Crash layer = the 2nd = **foveal_canvas** (no further loader log; face_bridge loaded fine).

## Root cause (instruction-level)

The OpenXR loader (`libopenxr_loader.so`, Valve-shipped, upstream OpenXR-SDK `api_layer_interface.cpp`) loads implicit layers inside `ApiLayerInterface::LoadApiLayers` and forwards loader init per layer:

```cpp
if (XR_SUCCEEDED(res) && !forwardedInitLoader && LoaderInitData::instance().getPlatformParam() != nullptr) {
    ...
    if (XR_SUCCEEDED(api_layer_info.getInstanceProcAddr(XR_NULL_HANDLE, "xrInitializeLoaderKHR", &initializeVoid))) { ... }
```

This GIPA call runs **before any `xrCreateInstance`/`createApiLayerInstance` chain exists**. `libgxr_foveal_canvas.so`'s `get()` routed only its known names and then `else return g.get(instance,name,function);` — `g.get` is assigned only in `create()` (the layer's `createApiLayerInstance` callback). Unassigned null was tail-branched: new-build disasm proof (`llvm-objdump`):

```
25550: adrp x8, 0x6c000
25554: ldr  x3, [x8, #0xf78]     ; x3 = g.get (null pre-create)
<old build>  25570: br x3        ; → PC=0, SIGSEGV   (device: x3=0, pc=0)
```

All 6 other layer extensions in this repo (controller-velocity-frame, controller-hal-pose, controller-extrapolation, controller-grip-haptics, resolution-trace/android_surface_trigger, plus the shipped face_bridge/xr_bridge binaries) guard this state; the foveal layer was the only unguarded one.

## Fix

- `extensions/foveal-canvas-layer/src/foveal_canvas_layer.cpp` `get()`: `else return g.get ? g.get(instance,name,function) : XR_ERROR_FUNCTION_UNSUPPORTED;` (a failed call is the loader-tolerated "no forward" answer; `XR_SUCCESS` with null output would make the loader abort the layer).
- `extensions/foveal-canvas-layer/tests/layer_tests.cpp`: mock `runtimeGetInstanceProcAddr` + pre-create/forwarding regression asserts in `main()`.
- Rebuilt helper staged to `patches/src/main/resources/steamlink/androidxr/libgxr_foveal_canvas.so`; new sha256 `d1426255e07674d604d1288429676c0c7f4a67d6a1b575d55c5a94e47d030d3f` (old `2939db18…`); `FOVEAL_CANVAS_HELPER_SHA256` updated to match. Manifest + scene guards unchanged (manifest sha `2ff0b8e9…` untouched).
- New-build disasm guard: `cbz x3, +0x268 → mov w0,#-7; ret` else `br x3` (0x25558/0x25574).

## Verification (offline; device proof still pending)

- Native layer harness from edited source: PASS (all prior scenarios + new pre-create guard asserts). Geometry tests: PASS.
- Toolchain: NDK 27.2.12479018 `aarch64-linux-android29-clang++` (Build-Native.ps1), zig 0.15.2 for host doubles.
- `:patches:test --offline`: BUILD SUCCESSFUL 43s (executed/skipped counts in the wrap-up message; JUnit XML under `../builds/steamlink-patches/gradle/patches/test-results/test/`).
- Source edits were absorbed into another session's commit `f1175ad`; Kotlin pin + staged `.so` remain uncommitted working-tree changes at record time.

## Remaining device step (not claimed done)

Repatch the original 2.0.20/5001812 APK with a bundle containing this fix (local `:patches:buildAndroid` or a release containing this change), install, launch, connect. Expected loader lines: `LoadApiLayers succeeded loading layer XR_APILAYER_local_GalaxyXR_foveal_canvas` then `GXRFovealCanvas: initialized …` / `session …` / `canvas_created …` / `frame …` (per README log events), no SIGSEGV. Rollback: same recommended set without the canvas patch.

Evidence files: `../builds/steamlink-patches/build/foveal-crash-20261012/` (crash excerpts, loader/canvas `.so` pulled from device, full loader disasm, upstream `api_layer_interface.cpp`, builder spec, installed-APK hash record).
