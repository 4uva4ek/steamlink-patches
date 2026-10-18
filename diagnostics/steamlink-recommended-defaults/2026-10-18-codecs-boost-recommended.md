# Codec and BOOST recommendation defaults, 2026-10-18

Owner request: recommend Network Optimizations, CPU/GPU BOOST and Codecs Support
on Steam Link 2.0.23; recommend BOOST and Codecs Support on 2.0.20.

| Exact Steam Link pair | Network | BOOST | Codecs Support |
|---|---|---|---|
| 2.0.20/5001712 | excluded | default on | default on |
| 2.0.20/5001812 | excluded | default on | excluded; no verified codec layout |
| 2.0.21/5001968 | excluded | optional, default off | excluded |
| 2.0.22/5002244 | excluded | optional, default off | excluded |
| 2.0.23/5002363 | default on | default on | default on |

`xrCodecsSupportPatch` replaces the previous 2 codec entries with 1 default-on
patch covering exact 5001712 and 5002363. It keeps the existing per-build scene
anchors and native runtime layout selection. No offsets or native payloads were
changed for this request. Telemetry remains optional and default off. Codec
support keeps its existing experimental-channel classification.

`performanceBoostPatch` covers the requested 2.0.20 and 2.0.23 exact builds.
`performanceBoostOptionalPatch` preserves optional 2.0.21 and 2.0.22 support
without changing their recommendation defaults. Both execute through a shared
body that checks its own package and exact build scope before file access.
Network Optimizations was already default on for exact 5002363; its execute body
now also rejects other packages and pairs before file access.

Updated the compatibility description, portable metadata/default tests, 4 JSON
catalogs and current catalog/reference documentation. The core matrix recommends
20 / 19 / 18 / 18 / 10 selections on 5001712 / 5001812 / 5001968 / 5002244 / 5002363.
Controller and other optional patches are outside those recommendation counts.

## Validation

- Java 22.0.1; repository Gradle 9.6.1 wrapper and pinned Kotlin toolchain.
- Local `:patches:buildAndroid :patches:generatePatchesList -PreleaseChannel=stable
  --offline --console=plain`: passed; all app extension tasks remain in the graph.
- Local `:patches:test --offline --console=plain`: 206 executed/passed,
  14 missing-input skips, 0 failures/errors (220 total).
- Isolated detached checkout with all current source changes and untracked
  payloads, without ignored decoded APKs: Android packaging/catalog generation
  passed; full tests executed 183, passed 181, skipped 37, failed 2, errors 0.
  Both failures are in the existing `GxrSpacewarpLayerPatchTest`: its install
  test supplies a truncated synthetic scene when the real input is missing;
  its byte-pinned manifest lacks a deterministic checkout line-ending rule.
  These unrelated spacewarp sources/tests were not changed for this request.
- All requested-scope suites passed in that isolated run: codec 4 passed/7
  skipped, BOOST 2/0, telemetry 5/5, network 1/7, categories 2/0 and compatibility
  matrix 8/0. Skipped native audits are not compatibility evidence.
- The initial isolated snapshot checked out HEAD before overlaying the pending
  `.gitattributes` changes, leaving the codec manifest CRLF. Re-extracting that
  tracked manifest as LF restored its canonical hash; production pins were not
  weakened. Its existing `text eol=lf` rule remains authoritative.
- Stable mirror is byte-identical to `patches-list-stable.json`; the all catalog
  contains exactly 1 Codecs Support entry with both exact targets and default on.
- Cavecrew review found no issues in the requested change. No GitHub CI run,
  commit/push, APK installation, ADB action or headset validation was performed.
  Codec runtime validation on 5002363 remains pending.

Logs, JUnit XML and `test-receipt.json` are retained under
`../builds/steamlink-patches/build/defaults-validation-20261018/`.
Automatic approval review rejected the cleanup command before execution with
`blocked by policy`; 0 files were deleted. Cleanup is deferred for the task-owned
checkout `../builds/steamlink-patches/build/defaults-check-20261018/` and its
derived Gradle output `../builds/steamlink-patches/build/builds/steamlink-patches/`.
Canonical inputs, the main output and validation receipts remain available.
