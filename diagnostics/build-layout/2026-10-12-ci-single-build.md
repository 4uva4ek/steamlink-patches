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

Remaining gap: no GitHub Actions run of the corrected commit has been observed, so CI
success for this change is unverified (`gh` is not available on this host). The workflow
file was YAML-parsed locally and its step order reviewed; the first post-push run of the
`dev` branch release must be checked before treating the CI change as proven.
