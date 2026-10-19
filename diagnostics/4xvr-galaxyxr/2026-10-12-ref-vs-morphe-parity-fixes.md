# 4XVR reference-vs-Morphe parity audit and fixes (2026-10-12)

Supplements the [diff and plan record](2026-10-11-4xvr-galaxyxr-diff-and-plan.md), the
[implementation record](2026-10-11-4xvr-galaxyxr-implementation.md) and the
[input/popup record](2026-10-12-4xvr-input-modes-and-popup-refresh.md); supersedes nothing.

Scope: compare the reference build (`4XVR-GalaxyXR/`, the directly patched distribution the patch
set was modelled on) against the dev.20 Morphe output (`4XVR_Video_Player-Patched/`, decode of
`4XVR_Video_Player-v2.0.22-patches-v1.28.0-dev.20.apk`, premium unlock excluded by the owner),
then close every remaining behavioural gap in the patch sources.

## Method

- Three-way class matrix (stock `4xvr-orig` / reference / Morphe output) over 42,099 smali class
  paths: raw SHA-256 plus normalized text compare (strip `.line`, `.param`/`.local` debug items,
  default-field tails). Script and outputs under
  `../builds/steamlink-patches/build/diag-4xvr-ref-vs-morphe-20261012/` (`three_way.py`,
  `matrix_smali.tsv`, `diffs/`, `findings-botA.md` / `findings-botB.md` / `findings-botC.md` from
  three delegated investigators).
- Raw deltas collapse to: 10 classes edited by both sides (semantic review below), 4 + 2
  reference-only diffs (the premium `V4BuyVIP*` retargets plus two try-label recompile-churn
  files), all 32 helper classes byte-identical, and non-smali deltas fully attributed
  (5 churn, 5 ours, 1 toolchain artifact + 1 observation).

## Behavioural deviations found and fixed (five; all applied to the patch sources)

1. `Main4XActivity.RenderFrame` drain unreachable — `RenderDispatchPatches.kt`. The `if-nez v5`
   branch into `:cond_0` skipped the inserted `RenderDispatch.drain()` call, and that branch is
   the only entry to the block, so the drain never ran. Fix: insert the call, create a fresh
   label on it, rebuild the branch against that label (`BuilderInstruction21t`, constructor-only
   retarget).
2. `V4SubtitlesUI` attach skippable — `SubtitleSuitePatches.kt`. The `if-eqz v0, :cond_a` entry
   ran `SubtitlePositionControls.attach()` in the reference but skipped it in the Morphe output
   (label sat on the `AftCreate` super call). Same branch-join rebuild.
3. `SubtitleStyleListener` wiring missing — `SubtitleSuitePatches.kt`. The reference constructs
   the extended listener (`SubtitleStyleListener`, cue cache plus italic refresh) as
   `mysubtitleListener`; the Morphe build kept the stock `V4VRSubtitleListener`, which made the
   shipped `SubtitleStyleListener.refresh()` call a no-op (instance-of gate). Fix: retarget the
   single instantiation (`new-instance` + `invoke-direct`).
4. `GxrAudioOutput.bind()` hook missing — `AudioBridgePatches.kt`. The reference binds the bridge
   to the player's fresh AAudio handle at the end of `Vr4pMediaPlayer.access$7702`; the patch set
   never had that site (the bridge could not re-route this player's audio output). Fix: insert
   `bind(player, J)` immediately after the `m_AAudioPtr` store.
5. `routeResumeSurface` outside the stock try — `HdrPipelinePatches.kt`. The reference covers the
   call with the stock `Exception -> catch_0` (codec recovery) and catchall handlers; the Morphe
   call sat before the stock try start, so a routing failure propagated instead of recovering.
   Fix: `addCatch` coverage over the inserted call reusing the stock handler labels; the two
   added ranges merge in the written dex into one try item carrying both handlers.

Equivalent, no action taken: `MyVideoRunnable` routeOutputSurface block (all three entry paths
equivalent; v9/v10 liveness verified), `MyAudioRunnable` duplicated `iget-object` (benign
straight-line reload), `V4VRSubtitleListener` italic register choice, `V4Render` / `V4Window` /
`V4Config` (normalized diffs empty), `V4Theatre` try/catch scaffold retained for the neutralized
monitor block (public dexlib2 cannot remove try blocks).

## Verification

- Host-side Morphe apply-audit: new `util.FourXvrParityMorpheAudit` applied the four changed hook
  patches plus their dependency closures to the retained stock APK — all five Morphe results
  PASS, unsigned output packaged, input unchanged (sha256
  `24b83ab584675e02570e5318a35c7fa9b8da7eabb1f0763711c74c1bad3a84a8`). Receipt + console:
  `../builds/steamlink-patches/build/4xvr-parity-20261012/morphe-audit-parity/run/` (output APK
  sha256 `362f7bd65bc0a3490bd3e484e3726168194c8076889c662ba0f93bf62d05dc6c`).
- Packaged-dex verification (dexdump, new 44-class output `classes.dex`; dump kept as
  `newdex-dexdump.txt` in the audit directory):
  - `Main4XActivity.RenderFrame`: `0008: if-nez v5, 000b` and `000b: invoke-static {},
    Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;.drain:()V` — the branch lands ON the drain
    (previously dead code).
  - `V4SubtitlesUI`: `020b: if-eqz v0, 0267`, `0267: iget-object ... m_ViewGroup`, `0269:
    invoke-static ... SubtitlePositionControls.attach`.
  - `Main4XActivity`: `023a: new-instance v12, Lcn/vr4p/patch/SubtitleStyleListener` and `023c:
    invoke-direct {v12, v11}, ...SubtitleStyleListener;.<init>:(Landroid/content/Context;)V`.
  - `access$7702`: `0000: iput-wide v1, v0, ...m_AAudioPtr:J`, `0002: invoke-static {v0, v1, v2},
    Lcn/vr4p/patch/GxrAudioOutput;.bind:(...)V`, `0005: return-wide v1`.
  - `ActResume`: try `0x0038 - 0x003f` with `Ljava/lang/Exception; -> 0x0041` (catch_0) and
    `<any> -> 0x00f1` (catchall_0), covering `routeResumeSurface` + `setOutputSurface`.
  - Packaging: 10 modified host classes stripped from originals into the new dex together with
    the 34 extension helpers; no duplicate class definitions in any output dex.
- `:patches:test`: 173 tests / 0 failures / 0 errors / 13 skipped (the standard fixture-gated
  Steam Link audits) — run after the source edits and again after the new audit util landed.
- Release graph `:patches:buildAndroid :patches:generatePatchesList -PreleaseChannel=experimental`
  BUILD SUCCESSFUL; the four catalogs regenerate byte-identical (line-ending only; restored).

## Observations / open items

- Toolchain artifact, not a reference fix: the dev.20 Manager-side build loses the resource
  NAMES of two strings per locale (id `0x7f0f0149` and `0x7f0f0193` render as `string_0x...` in
  the decode). Values and IDs are intact; the reference tree and this audit's desktop-toolchain
  output both keep the names (`aapt2 dump resources` checked). Harmless for `R.string` ID
  access; relevant only if the app ever resolves strings by `getIdentifier`. No patch-source
  action taken; candidate for an upstream Morphe-side look if it ever matters.
- Headset validation of every group (including these fixes) is still pending; the on-device
  flow needs a commit + push so Morphe Manager picks up the next `-dev` release.
- Premium/VIP work stays excluded per owner instruction (the reference `MetaPremium` /
  `V4BuyVIP*` retargets are deliberately not ported; the separate `Premium unlock (4XVR,
  experimental)` patch is unchanged by this session).
- Anchor tests for the new site shapes were not added: the patch bodies fail closed on the exact
  stock shapes and the apply-audit receipt covers the live surface; add per the
  `FourXvrPopupRefreshAnchorTest` pattern on request.
- Concurrent session note: `.gitignore` gained `4XVR_Video_Player-Patched/` (foreign uncommitted
  edit). This session's changes: the four patch files plus `util/FourXvrParityMorpheAudit.kt`.
