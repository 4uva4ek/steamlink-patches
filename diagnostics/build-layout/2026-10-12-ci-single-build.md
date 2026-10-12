# Single-build CI, tests moved local (2026-10-12)

The Release workflow built the Gradle graph twice per run (a preflight gate plus the
release build) and compiled/ran `:patches:test` in every one of those builds. It now
builds exactly once and never runs tests.

Changed:

- `.github/workflows/release.yml` — preflight gate removed; `actions/setup-java`
  caches Gradle; `npm ci` replaces `npm install`; `Run unit tests` runs `:patches:test`
  only for a manual `workflow_dispatch` with `run_tests`; `Verify build when no release
  was published` runs `./gradlew :patches:buildAndroid :patches:generatePatchesList
  -PreleaseChannel=... clean` when semantic-release published nothing. The release
  path, attestation resolver and step ordering are unchanged.
- `patches/build.gradle.kts` — `generatePatchesList` no longer `dependsOn(build)`
  (which pulled `check`/`test`); it still depends on `buildAndroid`, so the jar + D8
  bundle exists before catalogs are generated. This also removes tests from the
  `./gradlew publish` graph that `gradle-semantic-release-plugin` runs.
- Test policy is now local: `./gradlew :patches:test` at the end of every task
  (`AGENTS.md`, `.agents/skills/morphe-patches/SKILL.md`).

Semantic-release still bumps `gradle.properties`, `@semantic-release/exec` prepareCmd
still runs `./gradlew clean generatePatchesList -PreleaseChannel=...` before any git
tag exists (a broken tree fails the run with nothing committed), and a verified push
that publishes no release is still fully built. Double-build and test-compile cost are
the savings; the dangling-tag risk after a successful prepare is unchanged.

Validation (local, Windows, Java 21 / Gradle 9.6.1):

- `:patches:generatePatchesList --dry-run`: compileKotlin, compileJava, 4 extension
  tasks, processResources, classes, jar, buildAndroid, generatePatchesList — no `test`,
  `compileTestKotlin` or `check`. `publish --dry-run`: buildAndroid, generatePatchesList,
  publish — no test tasks. `:patches:build --dry-run` still schedules test/compileTestKotlin.
- `:patches:test`: **170 tests, 0 failures/errors, 12 skipped** (all 12 are
  `Real decoded-input audit BLOCKED: missing ...` assumptions).
- `:patches:buildAndroid :patches:generatePatchesList -PreleaseChannel=experimental`:
  succeeded; the 4 root catalogs are byte-identical to `HEAD`.
- `.github/scripts/test_generate_patches_readme.py`: 4 tests passed.
- `npm ci` (325 packages, Node 26.7.0 / npm 11.19.0), then
  `.github/scripts/test_release_artifact_paths.cjs`: 9 assertions passed.

Remaining gap: the publishing path (version bump, prepareCmd build, publish, attestation)
has not yet run under the new workflow; it needs a `fix:`/`feat:` push to dev, which
publishes a prerelease.

## Measured on GitHub Actions

[Run 256](https://github.com/AngelDark92/xr-porting-hub/actions/runs/38226588183), commit
`e140c3c`, a `ci:` commit, so no release was published and the single-build fallback path
ran: **136 s total** (created 08:55:36Z, finished 08:57:52Z), job 131 s, queue 3 s.

| Step | Duration |
|---|---:|
| Install dependencies (`npm ci`) | 4 s |
| Verify patch category documentation | 1 s |
| Verify external release artifact paths | 0 s |
| Run unit tests | skipped (local step) |
| Release (plugin verifyConditions, version check, no release) | 64 s |
| Verify build when no release was published | 43 s |

The 43 s fallback step is the complete release graph without tests: compile, the 4
extension assemblies, jar, `buildAndroid` (D8) and `generatePatchesList`.

Old workflow, same job, for comparison:

| Run | Path | Total | Preflight gate | Release |
|---|---|---:|---:|---:|
| 254 `a5ef0aa` (docs push, no release) | old | 173 s | 94 s (incl. `:patches:test`) | 56 s |
| 255 `d7526f4` (merge, published v1.28.0-dev.12) | old | 166 s | 94 s | 52 s |
| 256 `e140c3c` (ci push, no release) | new | 136 s | — | 64 s + 43 s build |

No-release path: 173 s to 136 s (-21%). The 94 s test-carrying gate is replaced by the
43 s single build; the Release step grew by ~8 s because it now resolves the plugins
cold as the first Gradle invocation of the job.

Warm-cache repeat: [run 257](https://github.com/AngelDark92/xr-porting-hub/actions/runs/38226946656)
(docs commit `2b06ffa`, no release) finished in **59 s** total, job 55 s, queue 2 s.
`Setup Java` restored the cached Gradle home in 7 s, the Release step dropped from 64 s
to 17 s and the fallback build from 43 s to 9 s. Same path cold 136 s, warm 59 s; old
workflow 173 s.
