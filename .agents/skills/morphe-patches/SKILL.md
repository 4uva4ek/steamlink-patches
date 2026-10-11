---
name: morphe-patches
description: Author, edit, debug and validate Morphe patches for the apps in this repository (Steam Link VR, 4XVR), including exact-build guards and fresh-checkout test inputs.
---

# morphe-patches skill

Use when authoring, editing, or debugging patches in this project.

## Project facts
- Library: morphe-patcher 1.13.0; Morphe Manager 1.30.0 or newer
- Target apps (this repository patches several): Steam Link VR `com.valvesoftware.steamlinkvr` and 4XVR `cn.vr4p.oculus4xvrplayerov`; exact `(versionName, versionCode)` pairs are defined per app in `shared/Constants.kt` (`COMPATIBILITIES_STEAM_LINK*`, `COMPATIBILITIES_FOUR_XVR`)
- Required compatibility rules: read the repository-root `AGENTS.md` before editing; its app registry maps each app to its package, exact bases, sources, resources and extension payload
- Kotlin source roots: `patches/src/main/kotlin/app/template/patches/steamlink/` and `.../fourxvr/`; shared code lives in `.../patches/shared/` and `patches/src/main/kotlin/util/`
- Resources roots: `patches/src/main/resources/steamlink/` and `patches/src/main/resources/fourxvr/`
- App isolation: a patch declares `compatibleWith` only its own app's `Compatibility`, and an app's extension payload never references another app's `.mpe`, helper classes or resource paths

## Patch type selection

| Need | Use |
|------|-----|
| Copy/replace raw APK file (lib, assets, .so) | `rawResourcePatch {}` |
| Edit AndroidManifest.xml or other XML | `resourcePatch {}` |
| Merge a DEX extension into the app | `bytecodePatch {}` + `extendWith(...)` |

## Patch structure template

```kotlin
@Suppress("unused")
val myPatch = bytecodePatch(          // or rawResourcePatch / resourcePatch
    name = "Human readable name",
    description = "What it does.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_STEAM_LINK)   // always required
    dependsOn(someOtherPatch)                  // if ordering matters

    extendWith("extensions/extension.mpe")     // bytecodePatch only, when needed

    execute {
        // patch logic here
        // rawResourcePatch: get("lib/arm64-v8a/libfoo.so").writeBytes(...)
        // bytecodePatch:    (no fingerprint injection — see FORBIDDEN below)
    }

    finalize {
        // runs after APK rebuild; use for XML/manifest edits via document(...)
    }
}
```

## Adding an app

A new app is a new package, never a widening of an existing patch. Checklist:

1. `shared/Constants.kt`: a private `data class <App>Build(version, versionCode)`, its exact build list, an `is<App><Build>(version, versionCode)` helper that every patch body re-checks, a `Compatibility` built on the primary `AppTarget` constructor with a distinct `name`, and an exposed `COMPATIBILITIES_<APP>`. Never touch another app's lists.
2. Layout: patches `app/template/patches/<app>/`, payloads `resources/<app>/<group>/`, extension smali `resources/<app>/smali/<package path>/`, tests `test/.../<app>/`.
3. Categories: reuse `shared/PatchCategories.kt`; a new category needs the matching `PatchCategoriesTest` group map and expected counts in the same change. That test enumerates patches by hand (it does not discover new top-level patches), so a new app's patches stay outside its category and catalog assertions until they are listed there explicitly. A new category also needs an entry in `CATEGORY_ORDER` in `.github/scripts/generate_patches_readme.py`; unknown categories are appended alphabetically after the known ones.
4. Extension payload: a JavaExec task mirroring `assembleExtension` (input that app's smali dir, args `a -a 33 -o <out>`) emitting `<app>-extension.mpe`, plus `resources.srcDir(...)` and `processResources`/`sourcesJar` dependsOn. Never merge two apps' classes into one `.mpe`.
5. Every patch declares `compatibleWith(*COMPATIBILITIES_<APP>.toTypedArray())` and re-checks the exact pair first: `if (!is<App><Build>(packageMetadata.versionName, packageMetadata.versionCode)) return@execute`.
6. Payloads load through one private loader per app (`object {}.javaClass.getResourceAsStream("/<app>/<path>")`) that errors with the missing path.
7. Native in-place edits: model each as `(offset, original, replacement)`, pin the library by exact size + stock SHA-256, accept original or replacement per site (idempotent), fail closed otherwise; keep an unpinned per-site applier so synthetic tests run without the decoded base.
8. Tests: tracked payload SHA-256 pins, synthetic apply / keep-patched / reject-foreign / wrong-size / wrong-hash cases, and the real-base audit behind `org.junit.Assume.assumeTrue("BLOCKED: ...")`. Report executed and skipped counts separately.
9. Worked example in repo: the 4XVR set (`fourxvr/`, `resources/fourxvr/`, `fourxvr-extension.mpe`). Cross-check every app-member reference in extension smali against the stock decode before assembling — the app's existing classes are patched in place, never replaced.

## Extension DEX (smali)

- Sources: `patches/src/main/resources/steamlink/androidxr/smali/` (Steam Link) and `patches/src/main/resources/fourxvr/smali/` (4XVR)
- Built by `assembleExtension` (Steam Link, plus `assembleMinimalExtension` and `assembleBatteryExtension`) and `assembleFourXvrExtension` (4XVR). Each task emits its own payload: `extension.mpe`, `minimal-extension.mpe`, `battery-extension.mpe`, `fourxvr-extension.mpe`
- Only brand-new helper classes are assembled. Edits to an app's existing classes stay in build-aware Kotlin code (typed dexlib2 builders, no raw smali strings). A new app adds its own task and output directory; it does not rewire an existing app's task.
- Output: `../builds/steamlink-patches/gradle/patches/generated/extension-resources/extensions/extension.mpe`
- Delete cached `.mpe` before rebuilding: `Remove-Item ../builds/steamlink-patches/gradle/patches/generated/extension-resources/extensions/extension.mpe`
- **Smali API level: `-a 33`** — never use 35 or higher

### DEX format follows the pinned toolchain
The current smali `d856bad65f` emits standard DEX 040 with a `0x70` header at API 33.
Patcher 1.13.0 parses this format; keep the fresh-helper parsing checks in
`NativeXrTrackingConfigTest` mandatory. The earlier smali pin emitted DEX 039 at
API 33. Do not change DEX headers manually or raise the API level to fix a format mismatch.

Historical Morphe 1.7.0 builds could not parse container-format output from higher
API-level assembly and crashed:
```
Caused by: com.android.tools.smali.dexlib2.util.DexUtil$InvalidFile: Unexpected container offset in header
```
This historical failure does not make every DEX 040 file a multi-DEX container.

## Patch descriptions

Patch `description` text is user-facing (Morphe Manager list) and is copied verbatim into
the tracked `patches-list*.json` catalogs. Tests pin some wording:

- `AndroidSurfaceTriggerResourceTest` asserts `patchSource.contains("future RGB10_A2")`
  against `OptionalXrPatches.kt`.
- `ControllerHalPosePatchTest` / `ControllerVelocityFramePatchTest` assert `needs Shizuku`,
  `local to the grip pose`, `local to the streamed pose`, `in the base space`.
- `UberLinkRescanDisablePatchTest` asserts `2.0.23/5002363` and `Experimental`.
- The 4XVR tests cover the new app instead of wording: `FourXvrNativeGuardTest` pins
  `libvr4p-oculus.so` and `libovrplatformloader.so` size/SHA-256 plus the refresh-profile
  and stub edit bytes and fails closed on wrong-size, wrong-hash or unpinned layouts;
  `FourXvrPayloadResourceTest` pins the bundled 4XVR payload hashes and requires the
  shipped smali helper sources.

Rewriting a description therefore needs a matching test update (or keep the phrase), then
`:patches:generatePatchesList` to rewrite all 4 root catalogs, and `:patches:test`. The
`build` task pulls in `check`/`test`, so a failing test also stops catalog regeneration.
Option and AppTarget descriptions are not covered by these assertions.

## Bytecode editing

- Prefer typed dexlib2 builders such as `BuilderInstruction21c` and the repository's
  `InstructionExtensions` helpers for exact bytecode edits.
- Do **not** call `addInstructions(index, "smali string")`. Historical Morphe Manager 1.7
  builds can crash in `InlineSmaliCompiler` with:
```
Caused by: java.util.NoSuchElementException: Collection is empty.
    at app.morphe.patcher.util.smali.InlineSmaliCompiler$Companion.compile
```
- Structural fingerprints or exact class/method lookups are allowed when validated against every
  declared base. Keep mutation preconditions build-aware and fail closed.
- Parameter register offsets must count register WORDS, not parameter types: `J` and `D` occupy two
  words, so in `RenderFrame(ZJJ)V` p1 sits 5 words below the register count, not 3. Counting types
  mis-anchors every wide-parameter method and silently matches nothing (regression: the 4XVR
  render-dispatch drain anchor; fixed by `parameterWordCount`, pinned by `FourXvrDexKitTest`).

## Manager app tile icon

Morphe Manager renders a stored APK's app tile from its manifest icon
(`packageManager.getResourcesForApplication` + `ResourcesCompat.getDrawable(resources, appInfo.icon)`).
A custom icon name or an adaptive XML in `mipmap-anydpi` without `-v26` can resolve to nothing and
show a blank tile. Expose the artwork under the canonical `ic_launcher` / `ic_launcher_round` names
(adaptive XML in `res/mipmap-anydpi-v26/` plus density PNGs) and point the manifest
`android:icon`/`android:roundIcon` at them — see the 4XVR `Galaxy XR launcher icon` patch.

## Constants
Use the exact `(versionName, versionCode)` model and primary `AppTarget` constructor documented in
the repository-root `AGENTS.md`. Never replace the existing compatibility list when adding a base.

## Helpers available
- `loadResource(name: String): ByteArray` — Steam Link private loader for `/steamlink/androidxr/` (`AndroidXrCompatibilityPatch.kt`)
- `loadFourXvrResource(name: String): ByteArray` — 4XVR loader for `/fourxvr/` (`fourxvr/FourXvrDexKit.kt`). Each app keeps its own private loader; there is no cross-app loader.
- `BinaryPatchHelper.findUniqueAndReplace(bytes, search, replace)` — AArch64 .so patching
- `BinaryPatchHelper.vaddrToFileOffset(...)` — virtual address → file offset in ELF

## Build commands

### Required output location

Every local build, native compile, diagnostic runner, temporary checkout and
project cache must write under `../builds/steamlink-patches/`:
`D:/Angelo/Desktop/SteamLink-GalaxyXR-Windows-Toolkit-FULL/builds/steamlink-patches`
in this workspace. Gradle uses `gradle/root` and `gradle/patches`; fixtures/tools
and retained evidence use mixed `build/`; native CMake output uses
`extensions/<name>/build-*`. Keep release catalogs and canonical source/resources
in the checkout. Use the wrappers' configured paths; update release consumers
with output changes. Never create a local `build`, `patches/build`, `.gradle`,
`.kotlin` or Zig cache tree. Read [the layout and validation](../../../diagnostics/build-layout/README.md)
when building or relocating artifacts. Saved classpaths/receipts must be resolved
through the known relocation map; regenerate moved CMake configure state.

### Dependency scopes and release preflight

Tests importing a production `compileOnly` library must declare their own
`testImplementation` dependency. Gson belongs in `compileOnly(libs.gson)`,
`testImplementation(libs.gson)` and the separate catalog generator configuration;
do not promote it to production `implementation` or `runtimeOnly` to fix tests.

Run the workflow's complete Gradle gate in a fresh checkout before accepting
dependency/plugin, generator or test changes:

```powershell
.\gradlew.bat clean :patches:test :patches:buildAndroid :patches:generatePatchesList -PreleaseChannel=experimental --no-daemon
```

Use `stable` on main. The workflow must run this gate before semantic-release,
including commits that do not produce a release. Preserve generated extensions
and mandatory portable tests in this graph.
Attestation uses the exact release version through
`.github/scripts/resolve_release_artifact.cjs`, yielding a canonical absolute
path without `..`. Prefixing a relative pattern with the workspace is insufficient.
Keep release preparation's Gradle `clean` before rebuilding; this removes the
preflight version from upload candidates while preserving retained `build/` inputs.
Run `node .github/scripts/test_release_artifact_paths.cjs` after `npm ci` for its
actual `@actions/glob` regression checks. On Windows, configure GitHub Packages
with `tools/Configure-GitHubPackages.ps1` and a classic `read:packages` token in
user Gradle properties; tokens must stay outside source, chat and logs.
Use an isolated checkout for this clean command; retained inputs are now in
`../builds/steamlink-patches/build/`, separately from cleanable `gradle/` outputs.
Wrappers route project caches outside the checkout. Use `Verify-Build.ps1` for
scoped local test/Android checks rather than cleaning that mixed directory.

Manual compiler classpaths and desktop fat JARs can hide missing declarations and
use a different compiler. Never call their output a Gradle or CI pass. When local
plugin resolution blocks the gate, report that limitation and use the corrected
commit's GitHub run for authoritative build verification.

For workflow failures, identify the exact SHA/run/job and first failed task before
editing; verify that same corrected SHA afterward. Run `37129119119` failed in
`:patches:compileTestKotlin`: `PatchCategoriesTest` imported Gson without a test
dependency, while manual validation supplied `gson.jar`. Production compilation
and Android packaging passed, so changing the patcher/smali pins or skipping tests
would not fix that failure.

Before accepting new tests, check whether each file input is tracked (`git ls-files -- <path>`) or explicitly provisioned by `.github/workflows/release.yml`. The workflow's ordinary `:patches:test` has no decoded APK provisioning. A local cached-compiler pass with ignored inputs present does not test that environment.

Keep tracked resource/hash checks, metadata tests and malformed-input rejection runnable without proprietary decoded APKs. For real-byte audits, use `org.junit.Assume.assumeTrue` with an explicit `BLOCKED` reason only when the exact decoded input is absent. Do not skip missing canonical payloads, invalid present inputs or assertion failures. Synthetic invalid bytes can prove rejection, never real-base compatibility.

Run fixture-dependent tests once with retained inputs and once from a fresh/isolated working directory where they are absent, using only tracked runtime resources and explicit classpaths. Check JUnit executed/skipped/failure counts. The 2026-10-01 failure in run `36907870366` was 5 `FovealCanvasPatchTest` failures caused by an ignored 5001812 scene; `buildAndroid` had already passed. Fix that input boundary rather than changing production guards or disabling the test task.

Report cached compilation, full Gradle/Android packaging, real APK audits and GitHub CI separately. Confirm the corrected commit's workflow result before claiming CI success.

Byte-pinned JSON/text resources need deterministic checkout bytes. Set an explicit `.gitattributes` rule (the foveal canvas manifest uses `text eol=lf`) and check `git ls-files --eol` plus the resource SHA-256 after checkout. Windows `core.autocrlf` can otherwise invalidate a correct Linux manifest pin. Preserve the canonical hash and installer guard; do not accept multiple line-ending hashes to hide the mismatch.

```powershell
.\gradlew.bat build                    # full build (every app)
.\gradlew.bat assembleExtension        # rebuild the Steam Link extension DEX only
.\gradlew.bat assembleFourXvrExtension # rebuild the 4XVR extension DEX only
.\gradlew.bat generatePatchesList      # regenerate the root patches-list.json catalogs
```
