# Steam Link 2.0.23 angular adapter

Standalone Morphe patch: **Controller angular world to local**.
Category **Controller**, default off, no recommended-set dependency. Exact
2.0.23/5002363 only, for the original package and the inspected `.x_23` clone.

Requires the Galaxy XR OpenXR Velocity Frame runtime correction **actually active**
in this app (native status `CORRECTED`). A checked module alone does not prove this.
The layer does not activate, configure, or test the module. With stock/raw runtime
velocities, this adapter is inappropriate. Remove the adapter before returning to
stock velocities.

For `pamir-stream-pose` action spaces, after successful `xrLocateSpace`, convert
`omega_local = inverse(R(location.pose.orientation)) * omega_base`. Only valid,
finite angular velocity and a finite near-unit orientation are accepted. Position,
orientation, linear velocity, timestamps, flags and the runtime predictor remain
unchanged. Other actions/reference spaces are passed through. No empirical pitch
constants, root calls, HAL access, process-memory writes or system modifications.

Separate OpenXR instance/session/action/space dispatch prevents cross-session
tracking; failed calls do not mutate outputs. Bounded output-chain traversal
rejects cycles and duplicate velocity structures. Source is in `src/`.

The Morphe installer checks the exact consumer function (0x107d30..0x1087cc,
SHA256 38053102f7461f9ebc9aa3a5542fdcc4baaab3a5e43c95881cd566fcf34fac5f)
before adding two files. This allows unrelated image patches without guessing
neighboring native layouts. It rejects legacy HAL, velocity and extrapolation APK
layers both during execution and after other selected patches have run.

## Validation and limits

NDK 28.2.13676358 / ARM64 / android-29 / static libc++ / 16 KiB ELF page alignment.
6000 randomized native math cases use an independent transposed rotation-matrix
oracle. Tests cover invalid inputs, output preservation, stream-only conversion,
failed calls, session/instance cleanup and loader negotiation. Kotlin tests cover
exact metadata, required resources and rejection of modified native consumers.
The real-input audit requires `GXR_ANGULAR_TEST_APK`; only that test may skip when
the proprietary APK is absent.

The separate local MPP contains only this patch and its two resources, with JVM
classes for Desktop and classes.dex for Manager. `build_standalone.py` accepts
explicit local tool paths; its build is not a substitute for the Gradle release
gate. Use build-tools 37 D8 with Kotlin 2.3 metadata (build-tools 36 is too old).
The full fresh-source Gradle gate is currently blocked by resolution of
`app.morphe.patches:1.3.4`; no CI/release pass is claimed.

Historical HAL-layer measurements motivated this adapter. They are not a new
headset A/B result. The app update was installed on 2026-10-11 and its installed APK hash was verified.
Live layer loading and physical wrist/throw A/B testing remain pending. No module
enablement is part of this patch. Native throwing remains unvalidated.

## Revert

Rebuild/reinstall the app without this patch, using the same signing key and package
to preserve userdata. Keep the original APK. Reboot alone does not remove an APK
layer. Loader-managed environments may also use `GXR_DISABLE_CONTROLLER_ANGULAR_LOCAL`.
All changes are confined to the non-system Steam Link APK; no startup/service hooks.

## PR validation (2026-10-12)

Rebased onto upstream `dev` at `ace87e7`. Native payload SHA-256:
`7c2df2a4e084d9493f5f84c56c1a98941cd876c7063ee6128ac372dc20226954`.
Native host tests passed, including 6000 independent rotation cases. Diagnostic
Kotlin/JUnit tests passed with the retained real 5002363 APK (6 executed); without
the proprietary input, 5 passed and the real-input audit skipped. This diagnostic
runner uses a cached dependency/classpath and is not a fresh Gradle build.
The fresh Gradle gate again stopped at Morphe plugin 1.3.4 resolution, before tests
or Android packaging. Catalog additions were generated with the production
generator; existing app entries and the stable catalog are preserved.
The installer now also checks the package at both mutation phases, so invocation
as a dependency on another app remains inert. No headset changes were made for
this PR.
