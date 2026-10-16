# Codecs Support recommended on 2.0.20/5001712 — per-scope defaults (2026-10-15)

Change: `Codecs Support` becomes recommended (default-selected) on exact **2.0.20/5001712** only.
Morphe's `default` flag is one global value per patch object, so the patch is split into one object
per recommendation scope over one shared guarded body (the `Device identity` pattern):

- `xrCodecsSupportPatch` — name `Codecs Support`, `default = true`, compatible with exact 2.0.20/5001712.
- `xrCodecsSupportNativeXrPatch` — name `Codecs Support (2.0.23/5002363)`, `default = false`, optional on
  exact 2.0.23/5002363 (headset validation pending there).
- Shared `createCodecsSupportPatch` factory, guarded body, scene preconditions and payload pins are
  unchanged; both objects stay in the experimental channel (`Steam Link Experimental` compatibility
  name — no channel move).

## Per-base recommended sets

| Base | Before | After |
|---|---|---|
| 2.0.20/5001712 | 17 | **18** (+ Codecs Support) |
| 2.0.20/5001812 / 2.0.21/5001968 / 2.0.22/5002244 | 17 | 17 |
| 2.0.23/5002363 | 7 | 7 (native object stays optional) |

Steam Link default-true patches: 18 -> 19. Catalogs carry 48 patch objects (was 47).

## Verification (local, 2026-10-15)

- `:patches:test` (JAVA_HOME `F:/Runtimes/Java21`): **195 tests, 0 failures, 0 errors, 13 skipped**
  (assumption-gated decoded-input audits: ControllerAngularLocal 1, FovealCanvas 4, VdSdrFovea 8;
  CodecsSupportPatchTest ran 8/8 with the real 5001712/5002363 scene inputs present).
- `:patches:generatePatchesList -PreleaseChannel=experimental`: catalogs regenerated; catalog scan of
  `patches-list-all.json` reproduces 18/17/17/17/7 and 19 default-true patches.
- `generate_patches_readme.py AngelDark92/xr-porting-hub dev`: TECHNICAL_REFERENCE.md re-injected
  (generator counted 49 patch rows).
- Independent review (cavecrew-reviewer, findings-only): 0 bugs, 0 risks, 4 nits and 2 questions; all folded (category-table total 30 -> 31, shared-file conflict matrix rows for both codecs objects, native recommended enumeration + Network Optimizations).
- Headset acceptance is pending: the dev pre-release published from this commit is the first build whose Manager pre-selection includes Codecs Support for 2.0.20/5001712.
  Support when patching 2.0.20/5001712).

## Scope notes

- `PATCH_CATALOG.md`: category table (3 entries), per-build recommended prose, numbered legacy list
  (+ item 18), codecs sections (recommended header + optional native section), and the
  `17-patch legacy recommended set` phrases generalized to `legacy recommended set`.
- `TECHNICAL_REFERENCE.md` intro counts updated by hand (outside the generated markers).
- No scene/payload `.so` edits; no driver-repo change; `LEGACY_VS_MORPHE_PATCHES.md` untouched
  (historical record; its native `7-patch` count is still current).
