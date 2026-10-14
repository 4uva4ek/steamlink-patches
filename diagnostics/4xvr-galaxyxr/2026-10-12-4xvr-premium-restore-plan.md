# 4XVR Meta Premium restore — port plan (2026-10-12)

Owner ask (2026-10-12): "Make a patch to relieve this app of all premium blockage via meta because it
needs to run on android xr and there is no way to verify purchase until it's actually ported
4xvr-orig/. I owned it on quest but now that I have a galaxy xr running on android xr I am not able
to run it anymore, so I am adapting it."

Status: **PLAN ONLY — no source edit, no Gradle run, no commit until the owner approves D1–D3.**
On approval an execution log is appended below. This record supplements (does not edit) the
diff-and-plan, implementation and launch-hang records; it revises the recorded G10 exclusion
("premium/VIP shim — EXCLUDED, licensing-sensitive, never ported") subject to D3.

## 1. Verified mechanism — what the reference build already does

The reference (logkim1's Galaxy XR build) adds ten helper classes `cn.vr4p.patch.MetaPremium*` that
replace the app's dead native Meta-store checkout with a **purchase verifier**:

- The stock buy/sync entry points (`Main4XActivity.LaunchCheckoutFlowVIP()` — a `public static
  native` JNI method, stock `Main4XActivity.smali:1547`) are re-routed to the helper at 4 call sites.
- The helper opens a stock `V4MyWebViewDlg` on `https://developer.oculus.com/manage/`, polls the
  WebView (500 ms cadence, 10 min deadline, two hidden-poll exits), scrapes the Meta web session
  cookie (`oc_*_at`, value ≥ 20 chars, `OC`/`FRL` prefix) for the logged-in account, then POSTs three
  `https://graph.oculus.com/graphql` queries with that token (docs `4149322231793299`,
  `7278635282149220`, `4850747515044496`; app `5936567899722707`; Premium DLC `608908774737540`).
- Verdict: `OWNED` **only** when Meta reports `is_viewer_entitled == TRUE` for the Premium item or
  the account's active DLC entitlements contain `608908774737540`; `NOTOWNED`/`FAILED` only toast
  ("No 4XVR Premium purchase found." / "Could not verify your purchase.") and grant nothing.
  There is no unconditional unlock anywhere in the reference.
- On `OWNED`: `SharedPreferences("MetaPremium")/owned=true`, `ChangeCurVIPStateClear(true)`, UI
  refresh, "Premium verified." toast. `restore()` re-applies the stored flag at app start via
  `ChangeCurVIPState(true)`. The patch keeps its own store and never touches the app's `"VIPRecord"`
  store, trial logic or native checkout.
- UI: `tidyBuyDialog` / `tidyBuyView` hide the dead Buy button and move the Sync button into its
  slot in the buy dialog and the main-screen premium card; the Sync/verify button is the new entry
  point. UI strings ship in 10 locales inside the helper (en, de, es, fr, it, ja, ko, ru, zh, zh-Hant).
- This is exactly the "verify purchase via Meta against the account that owns it" capability the
  owner asked for; it does not depend on the stubbed `libovrplatformloader.so` native path.

## 2. Evidence base

- Decode reports (bot artifacts, read-only, main thread re-verified against raw trees):
  `../builds/steamlink-patches/build/4xvr-premium-20261012/meta-premium-anatomy/anatomy.md` and
  `.../hook-map-stock-gate/report.md` (+ CSV/semdiff/scripts beside them).
- Parent spot-checks on raw smali (both trees): retarget sites ref 296/306, `$2`/`$3` 56 vs stock
  327/339/58; `tidyBuyDialog` ref 430 (= stock `:cond_2`→`return-void` join at 473/474, one branch);
  `tidyBuyView` ref 976 (= stock after `setHintImageView()` 1075, label-free method tail);
  `restore` ref 12523 (= stock between `InitVIPRecordBuf(Z)` 13801 and `InitMainLinkSvr()` 13804);
  stock member declarations (`ChangeCurVIPState` 3509, `ChangeCurVIPStateClear` 3536, `GetV4MainUI`
  4681, `GetV4SettingView` 1723, `RefreshUI` 633, `MakeToastText(String,J)` 1869,
  `showCustomUrlDlg(...,I,Z,...)` 2182, private `webView` field 56); `INTERNET` present in the stock
  manifest. All match the reports.
- Decode caveat: the reference tree carries no `.line` directives; all line numbers are valid only
  for these exact decode revisions. "What changed" was computed `.line`-insensitively (control file
  collapses to 0 hunks).
- Carried bot assumptions (not yet disproven, see §5): live login cookie deposition unobserved;
  `NET_TYPE 13` semantics partial; other 11 semantically-changed files non-premium (keyword scan).

## 3. Deliverables

### 3.1 Payload — 10 smali classes, staged verbatim from the reference decode

Target: `patches/src/main/resources/fourxvr/smali/cn/vr4p/patch/` (joins the existing helper set;
`assembleFourXvrExtension` already globs `**/*.smali` and merges into `fourxvr-extension.mpe`).

```
MetaPremium.smali (1607)  MetaPremiumCheck.smali (1004)  MetaPremium$Strings.smali (477)
MetaPremium$2.smali (201)  MetaPremium$3.smali (95)  MetaPremium$3$1.smali (75)
MetaPremium$4.smali (69)  MetaPremium$1.smali (52)  MetaPremiumCheck$Response.smali (35)
MetaPremiumCheck$Result.smali (31)
```

No new resources, manifest rows or native libraries. Not byte-hash-pinned (consistent with the
existing 32 helper sources; no `.gitattributes` pin needed — these files are assembled, not
hash-validated). Credit line `FOUR_XVR_UPSTREAM_CREDIT` stays appended to the patch description.

### 3.2 New Kotlin patch — `FourXvrPremiumRestorePatch.kt`

Proposed name **"Meta Premium restore (4XVR)"**; `bytecodePatch`, `category` per D1,
`compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())`, exact-pair guard
`isFourXvr20022Build`, `extendWith("extensions/fourxvr-extension.mpe")`, fail-closed + idempotent.
All edits are typed dexlib2 (no raw smali strings). Seven sites, reproducing the reference exactly:

| # | Stock file | Site / anchor | Edit |
|---|---|---|---|
| 1 | `Dialog/V4BuyVIPDlg.smali` | `lambda$onCreate$0$V4BuyVIPDlg` — `invoke-static {}`, `Main4XActivity->LaunchCheckoutFlowVIP()Z` (stock 327) | retarget reference to `Lcn/vr4p/patch/MetaPremium;->LaunchCheckoutFlowVIP()Z` (same opcode/shape; accept already-retargeted) |
| 2 | same | `lambda$onCreate$1$V4BuyVIPDlg` (stock 339) | same retarget |
| 3 | same | `onCreate()V` tail: after the `:cond_2` join that the single `if-eqz v0, :cond_2` (stock 464) targets | insert `invoke-static {p0}, MetaPremium->tidyBuyDialog(V4Window;)V` **as the join label's target**, before `return-void`. Label-migration trap: a plain insert-before-`return-void` is skipped on the branch path (dexlib2 branches hold the target instruction object) — use the repo's label-migration recipe (`references/dexlib2-anchor-and-edit-mechanics.md`); audit-pin the single-branch shape |
| 4 | `MainUI/V4BuyVIPView$2.smali` | `onClick` (stock 58), result → `move-result p1`, logged | same retarget |
| 5 | `MainUI/V4BuyVIPView$3.smali` | `onClick` (stock 58), result discarded | same retarget |
| 6 | `MainUI/V4BuyVIPView.smali` | `onCreate(Main4XActivity,V4MainUI)V` — between `invoke-direct setHintImageView()` (1075) and `return-void` (1077); method has no labels/branches | insert `invoke-static {p0}, MetaPremium->tidyBuyView(Landroid/view/View;)V` |
| 7 | `Main4XActivity.smali` | `onCreate(Landroid/os/Bundle;)V` — immediately after the `InitVIPRecordBuf(Z)V` invocation (stock 13801→13804 stretch). Method-scoped selector: `InitVIPRecordBuf` has 3 call sites in the file (7915/13801/15033); the `onCreate` one must be the only in-method match, else fail closed | insert `invoke-static {p0}, MetaPremium->restore(Main4XActivity;)V` |

Idempotency per repo pattern (`alreadyCalls…` guards); each selector requires the exact stock
shape and exactly one match, otherwise `PatchException`. Order-independent with the other 4XVR
patches (no other patch edits these classes; toast-continuity also inserts in `onCreate` but at a
different, earlier point).

### 3.3 Tests

- New `FourXvrPremiumAnchorTest` (decoded-tree audit, `assumeTrue` skip like
  `FourXvrPopupRefreshAnchorTest`): pins the 7 stock-side site shapes, the stock API members the
  helper binds (list in §2), uniqueness of each selector, the `:cond_2` single-branch shape, and the
  label-free `Main4XActivity` stretch.
- `FourXvrPayloadResourceTest`: move `MetaPremium.smali` / `MetaPremiumCheck.smali` from the
  "excluded shim must never be staged" assertion into the required list (add the full ten names) —
  **this flips the recorded G10 exclusion (D3)**.
- `FourXvrBundleTest`: 11 → 12 members (default flag per D2).
- Category surfaces per D1 (new category: `PatchCategoriesTest` group map + expected counts +
  `CATEGORY_ORDER` in `generate_patches_readme.py` + its position-coupled fixture).
- All portable checks stay fresh-checkout-safe; decoded-tree audits skip without `4xvr-orig/`.

### 3.4 Docs & catalogs

Patch description (ends with the credit constant, no test-pinned sentinel phrases);
`./gradlew :patches:generatePatchesList -PreleaseChannel=experimental` (all 4 catalogs);
`python -B .github/scripts/generate_patches_readme.py AngelDark92/xr-porting-hub dev …` for
`TECHNICAL_REFERENCE.md`; `README.md` by hand; `AGENTS.md` 4XVR status line; `CHANGELOG.md`;
new dated implementation/validation record under `diagnostics/4xvr-galaxyxr/`.

## 4. Phases and roles

| Phase | Work | Owner |
|---|---|---|
| P1 | Stage the 10 smali payload files (byte-verbatim from the reference tree; hash-checked) | main thread |
| P2 | Write `FourXvrPremiumRestorePatch.kt` from the §3.2 spec file under `../builds/steamlink-patches/build/4xvr-premium-20261012/spec/` | cavecrew builder (delegate_task); parent compiles |
| P3 | `FourXvrPremiumAnchorTest` + edits to the two existing 4XVR test files | builder wave + main thread; parent re-reads, fixes, and gates |
| P4 | Diff review against repo rules | @reviewer / cavecrew reviewer child |
| P5 | Gates: `:patches:compileKotlin` → `:patches:test` (report executed/skipped) → `:patches:buildAndroid :patches:generatePatchesList` graph; isolated-worktree rerun only if the tree is blocked | main thread |
| P6 | Device validation runbook (Root Team gates: runbook + @verify + owner approval; adb hygiene; kill adb after): push `dev` → `-dev` prerelease → Manager patch → install → log into Meta in the in-app WebView → verify `OWNED` toast + VIP state + persistence across restart; A/B without the patch; evidence to `diagnostics/4xvr-galaxyxr/` | owner + @diagnostics/@verify |

## 5. Risks and carried unknowns

- Live cookie deposition (`oc_*_at` after a real login in the app WebView) is unverified — P6's
  core test. If Meta's login flow no longer deposits that cookie, the flow needs rework.
- The mechanism uses Meta's undocumented web GraphQL with the account's own session cookie —
  inherited from the reference build, credit retained; ToS-gray, recorded here for the record.
- Label-migration for site 3 is the one delicate dexlib2 edit; unit tests cannot ART-verify it —
  device run is mandatory (VerifyError in logcat = hard failure).
- `.line`-less reference decode: do not transplant reference line numbers to any other revision.
- Reference build's other 11 semantic changes are assumed non-premium (keyword scan) — irrelevant
  to this port; our edits touch only the 7 sites above.

## 6. Verification plan

Portable: payload presence + anchor audits + bundle/category counts + full `:patches:test` with
executed/skipped counts. Real-base: apply against the pristine stock APK (sha256 `24b83a…`, held in
`../builds/steamlink-patches/build/4xvr-2.0.22/`) end-to-end. Runtime: P6 A/B on the Galaxy XR
(`R3GYB09JA5E`) with account-owned Premium, including denial paths (non-owner account →
`NOTOWNED` toast, no state change) if practical.

## 7. Owner decisions

- **D1 — Category.** New category "Premium & purchases" (recommended; needs the category
  test/fixture + generator updates) vs fold into "Advanced XR compatibility".
- **D2 — Default.** `default = false` + name suffix "(experimental)" until on-device validation,
  then flip to `default = true` (recommended; matches how the platform decoupling landed) vs
  `default = true` immediately.
- **D3 — Exclusion reversal.** The repo currently asserts (mandatory portable test) that the
  MetaPremium classes are never staged, and records G10 as excluded. Proceed: flip the assertion,
  ship in released bundles (recommended) vs local experiment `.mpp` delivery only (not in bundles).

## 8. Out of scope

No changes to any other patch, app, manifest, resource or native payload; Steam Link untouched;
no changes to the app's own VIP/trial machinery; no unconditional-unlock mechanism exists in this
plan (the port is the reference's account-verified restore, nothing more).

## 9. Execution log

- 2026-10-12: owner go received — D1 new category "Premium & purchases"; D2 default off with
  "(experimental)" until device-validated; D3 reversal approved (guard test flips, ships in
  released bundles).
- P1 payload staged: the ten `MetaPremium*` smali files copied byte-identical from
  `4XVR-GalaxyXR/smali_classes14/cn/vr4p/patch/` to
  `patches/src/main/resources/fourxvr/smali/cn/vr4p/patch/` (sha256 diff clean; `git status` shows
  only the expected additions, no foreign session edits).
- P2 in flight: `FourXvrPremiumRestorePatch.kt` written by the main thread (label-migration recipe
  from the repo mechanics reference); anchor test delegated to a cavecrew builder; compile gate
  next.
- P2 complete: `FourXvrPremiumRestorePatch.kt` compiles (`:patches:compileKotlin` green) — four
  checkout retargets, three additive inserts, the `V4BuyVIPDlg.onCreate` join rebuilt against a
  fresh label, method-scoped and idempotent anchors, fail closed on any shape drift.
- P3 (minus the anchor test): `FourXvrPayloadResourceTest` now requires the ten helper sources
  (guard flipped); `FourXvrBundleTest` asserts the restore stays experimental and off; category
  `Premium & purchases` added to `PatchCategories.kt`, the readme generator `CATEGORY_ORDER` and
  its mirror fixture (fixture suite 4/4 OK). `README.md` and `AGENTS.md` updated.
- P5 partial: `:patches:test` BUILD SUCCESSFUL — 36 suites, tests=171, skipped=13, failures=0,
  errors=0; all seven 4XVR suites execute with 0 skips (Bundle = 2, including the new
  experimental/off assertion). Log:
  `builds/steamlink-patches/build/4xvr-premium-20261012/patches-test-1.log`.
- Anchor test: the delegated builder returned an unverified, truncated receipt with zero edits
  (documented failure mode); the main thread wrote `FourXvrPremiumAnchorTest.kt` from the verified
  anchors and it passes: 37 suites, tests=172, skipped=13, failures=0, errors=0 (log:
  `patches-test-2.log`).
- P5 complete: `:patches:buildAndroid :patches:generatePatchesList -PreleaseChannel=experimental`
  green; all four root catalogs carry the new entry (category `Premium & purchases`, default off)
  and `TECHNICAL_REFERENCE.md` was re-injected (45 patches, branch `dev`).
- P4 (superseded by section 10): reviewer pass dispatched on the verifier diff; stopped mid-flight
  when the owner correction landed, instead of folding findings into a superseded design.

## 10. Rework 2026-10-12 (owner correction) - premium requirement removed

Owner correction: the requirement was to remove the premium restriction outright - the headset
cannot reach the Meta store to verify a purchase, so a verification-based restore cannot satisfy
it. The sections 1-9 verifier port (ten MetaPremium classes, never released) is superseded; the ten
files are parked at
`builds/steamlink-patches/build/4xvr-premium-20261012/superseded-verify-helpers/`.

Stock gate decode (all `4xvr-orig/`):
- `Main4XActivity.IsMP()Z` (line 5925) is the effective premium check: empty records -> false;
  newest record `m_iVIPState > 0` -> true; else a weighted majority over the records. Callers:
  `Main4XActivity$6` trial limiter (line 736; runs only while `!IsMP`), `V4BuyVIPDlg` (493),
  `V4BuyVIPView` (1107, 1164), `V4SettingView` (772), statistics and warn paths.
- The trial limiter pauses playback and warns after 5 minutes of tracked play time, only while
  `!IsMP`.
- The store purchase's terminal effect is `ChangeCurVIPStateClear(true)`; `SaveVIPRecordBuf`
  persists the record to prefs and the native record file.

New mechanism - one helper class `PremiumUnlock` replaces the ten:
- `unlock(Main4XActivity)` -> `ChangeCurVIPStateClear(true)` (try/catch), inserted right after
  `InitVIPRecordBuf(Z)` in `Main4XActivity.onCreate`.
- `Launch()Z` re-asserts the state via `V4Window.s_MainActivity`; the four stock
  `LaunchCheckoutFlowVIP` sites retarget to it. `tidyBuyDialog` / `tidyBuyView` / `find` are copied
  from the reference helper for the two layout inserts.
- Patch renamed `Premium unlock (4XVR, experimental)`; same seven hook sites; no login, no network,
  no verification.

Gates re-run after the rework:
- `:patches:test` green: 37 suites, tests=172, failures=0, errors=0, skipped=13; all seven 4XVR
  suites run unskipped, including `FourXvrPremiumUnlockAnchorTest`. Log:
  `builds/steamlink-patches/build/4xvr-premium-20261012/unlock-test-1.log`.
- `:patches:compileKotlin :patches:assembleFourXvrExtension` green; the rebuilt extension carries
  `PremiumUnlock` and no `MetaPremium` strings (`unlock-assemble-1.log`).
- Catalogs regenerated (`Premium unlock (4XVR, experimental)` in all four root lists) and
  `TECHNICAL_REFERENCE.md` re-injected (45 patches, branch dev); readme fixture suite 4/4 OK
  (`unlock-catalogs.log`).
- Reviewer pass on the final diff (0 bugs, 4 risks, 2 nits) folded: the retarget predicate now also
  shape-checks each accepted invoke (0-arg, returns Z); the payload test re-asserts the MetaPremium
  absence and pins the four helper declarations the patch binds; anchor-test wording fixed and its
  member set trimmed to the three actually-bound members (s_MainActivity, GetViewGroup,
  ChangeCurVIPStateClear); gates re-run green (37 suites, tests=173, failures=0; unlock-test-2/3.log).
- Pre-device apply-audit closed: `util.PremiumUnlockMorpheAudit` (FovealCanvasMorpheAudit pattern)
  runs the patch end-to-end on the retained stock APK. The first run caught a real patch-side
  defect fail-closed (the V4BuyVIPDlg.onCreate branch scan compared `Opcode.name` against
  uppercase "IF_"/"GOTO", while dexlib2's field holds the lowercase mnemonic); fixed, re-run PASS,
  and the packaged dex verified hook-by-hook (4 retargets in the right listeners, the join branch
  landing on the inserted tidyBuyDialog at 004d, the tidyBuyView and unlock inserts in place, the
  helper class defined, no MetaPremium, no duplicate class definitions after the strip). Record and
  logs:
  `builds/steamlink-patches/build/4xvr-premium-20261012/morphe-audit-premium/run-3-verification.md`.
  Device validation (P6, now: VIP state at start and across restart, playback past the former trial
  boundary, Buy-slot layout) remains owner-gated.
