Date: 2026-10-11 · Repo: steamlink-patches · Author: @diagnostics
Scope: 4XVR 2.0.22/20022 stock vs Galaxy XR reference mod — diff analysis + Morphe patch plan
Status: ANALYSIS COMPLETE. Implementation NOT started. Static-only evidence (no device/runtime); byte/dex/asset/ELF/strings/hash verification of a decoded APK cross-build diff against the distributed reference APK (`Tools/apk-tools/4XVR-2.0.22-GalaxyXR.apk`).

Exact bases
- Package: cn.vr4p.oculus4xvrplayerov (both trees identical here)
- versionName: 2.0.22 / versionCode: 20022 (both sides)
- Reference distribution: D:/Angelo/Desktop/SteamLink-GalaxyXR-Windows-Toolkit-FULL/Tools/apk-tools/4XVR-2.0.22-GalaxyXR.apk (383.8 MB; sha256 c77930d0b1c7607e98615199c54771bfbfdf64fed513dead9fde687163eb6c61)
- Decoded fixtures (untracked, repo root): 4xvr-orig/ = stock (44,285 files, 771 MB); 4XVR-GalaxyXR/ = ref mod (44,332 files, 786 MB)

Method one-liner (cross-build APK decode diff + targeted byte/dex trace)
- Whole-tree diff: files_only_in_gxr.txt (50 entries), files_only_in_orig.txt (3), files_size_diff.txt (403 KB), files_same_size_content_diff.txt (92 B). Normalization = strip .line, default-field tails, debug info → 15 functional smali candidates; 2 (javax.imageio, bouncycastle) = try-label recompile churn → 13 real hook files + 43 new classes.
- DEX layout: stock 8 dex → GXR 15 dex. Vr4pMediaPlayer moved dex1 → classes12; method set identical (423 vs 423), zero renames; only 6 functional inserts, one .locals bump (43 → 44).
- Native libs: 6 new, 3 changed, 21 unchanged. Distribution-vs-tree hash compare of lib/arm64-v8a/*.so: 30/30 match.

Verified facts (offsets/file bytes copied from the bot reports)
- AndroidManifest.xml: 5 edits (passthrough required=false; +openxr feature 65537; +uses-native-library libopenxr.google.so; +XR activity start-mode property; +uses-library com.android.extensions.xr).
- lib/arm64-v8a: 6 new .so (lib4xvr_hdr_advanced 987,176 B; libplacebo 8,769,936 B; libXrApiLayer_4XVR_viewconfig_fix 511,264 B; lib4xvr_audio_bridge 426,384 B; lib4xvr_render_dispatch 10,152 B; lib4xvr_subtitle_position 4,312 B); libopenxr_loader.so swapped 477,208 → 1,656,736 (Khronos loader 1.1.58, NDK r26, AAssetManager asset-based runtime/layer discovery, broker strings); libvr4p-oculus.so 3 bytes; libovrplatformloader.so 11 × 8-byte stubs `00 00 80 d2 c0 03 5f d6` (mov x0,#0; ret) at 0x56948, 0x85ce0, 0x85d50, 0x85e14, 0x85e6c, 0x85f88, 0x85fe0, 0x8a0c0, 0x8de78, 0x8dfd8, 0x8facc. All 30 libs in distributed APK sha256-identical to decoded tree.
- libvr4p-oculus.so 72 Hz fix: `_ZN13bsVr4pProgram16InitializeDeviceEv` clamps device-rate picker to 73.0f. Single function affected (0x2ebcc..0x30528). 0x30348 arm (Quest Pro/Headset1): movz w9,#0x42F20000 = 121.0f (stock); 0x30380 arm (Quest2/default): movz w9,#0x42BA0000 = 91.0f (stock). GXR replaces both with 0x42920000 = 73.0f. Sweep confirms 121.0f/91.0f nowhere else in .text in either build; 73.0f only at these two sites (GXR); 72.0f absent everywhere. Effect: "largest supported rate strictly below 73" = 72 Hz on Galaxy XR (which reports {60, 72[, 90]}); handed to myxrRequestDisplayRefreshRateFB. NOT a literal 72.0 write; NOT panel nits; NOT FOV.
- Hidden implicit OpenXR layer: assets/openxr/1/api_layers/implicit.d/XR_APILAYER_4XVR_viewconfig_fix.json (XR_FB_passthrough v4, api_version 1.0, env kill-switch DISABLE_4XVR_VIEWCONFIG_FIX_LAYER). View-config interposition set: passthrough lifecycle (xrCreatePassthroughFB/PassthroughStartFB/CreateLayer/SetStyle/Pause/Resume/Destroy, xrDestroyPassthroughLayerANDROID); xrPerfSettingsSetPerformanceLevelEXT with "SUSTAINED_HIGH rejected → BOOST fallback"; Swapchain/FP16 (xrEnumerateSwapchainFormats, xrCreateSwapchain, acquire/wait/release; cross-lib probes Hdr4xvrFp16HandoffReady, Hdr4xvrIsMovieTransitionActive; "HDR transition video-buffer reset deferred sc=%p generation=%llu"); Subtitle surface ("SubtitlePositionGetPercent" reads the JNI nativeSetPercent); Audio world-space ("Audio movie reference space result=%d"); View-config transform (xrEnumerateViewConfigurationViews / xrLocateViews) — exact rewrite UNPROVEN at instruction level.
- HDR/placebo flow: lib4xvr_hdr_advanced JNI `dev.eni.hdrdirect.HdrBridge` (notifyMovie/notifySeek/beginMovieTransition; routeOutputSurface + routeResumeSurface redirect codec output to AImageReader/AHB → pl_vulkan_wrap_external_ycbcr → libplacebo with PQ/HLG color spaces, pl_deband_default_params, HDR contrast-map log "4XVR contrast map: %s ... final-consumer=%s, tone-map=%s, input=%.1f nits, output=%.1f nits") → EGLImage GLES handoff, injected GLSL string fragments hdr4xvrStockMain/hdr4xvrDecodeSrgb/hdr4xvrEncodeSrgb/hdr4xvrLinearTarget with "uniform int hdr4xvrMappedEnabled". Smoke: Java hooks Main4XActivity.<clinit> (loadLibrary "4xvr_hdr_advanced"; RenderDispatch.initialize()), V4Render ×3 (notifyMovie; isHlg(MediaFormat) checks color-standard==6 && color-transfer==7), Vr4pMediaPlayer$MyVideoRunnable (routeOutputSurface; .locals 43→44 at v43), Vr4pMediaPlayer (notifySeek in seekPosForce; routeResumeSurface in ActResume; beginMovieTransition in SetDataSource).
- Audio bridge: lib4xvr_audio_bridge.so (JNI GxrAudioOutput.moviePose([F)) + cn.vr4p.patch.GxrAudioOutput (smali classes13, Output extends AAudio wrapper, channelMask(layout<<2): 6ch FFmpeg 0x3f → Android 0xfc = CHANNEL_OUT_5POINT1; PCM_FLOAT AudioTrack bound to com.android.extensions.xr.node reflection; gate Build.DEVICE=="xrvst2" + XrExtensions.getOpenXrWorldReferenceSpaceType()==0x3ba1ea38; bounded 1.92 MB ring + ≤64 KiB blocking writes + ~100 ms needWait cap = stutter fix; graceful permanent fallback to stock AAudio). Hooks: MyAudioRunnable 17 redirects; Main4XActivity.initialize(); Vr4pMediaPlayer bind + SetListenPoseAAudio (line 9581, SetCameraPose) + ReleaseAAudio (line 14506).
- Subtitle suite: lib4xvr_subtitle_position.so JNI nativeSetPercent + cn.vr4p.patch.SubtitlePositionControls (row: −/value/+ drawable radiobutton_sync_subtitle_btn; panel must have exactly 2 children; SharedPreferences "4xvr_subtitle_position"/"offset_percent"; 10 entries / 9 codes en, zh-Hans, zh-Hant, ru, ko, ja, it, fr, es, de = 10 languages per owner) + PositionValue + SubtitlePositionStrings; SubtitleFont.load() ← SubtitleFontFiles.select("4XPlayer/fonts", .ttf/.otf) consumed in V4VRSubtitleListener.<init>; V4SubtitlesUI.AftCreate attaches the row + tab change persists via UpdateConfigSetting + V4SubtitlesUI.onCreate passes 0x1b8→0x1f4 (440→500) panel height param. Wrap-clamp: V4VRSubtitleListener text layout routine adds `u = max(0, min(textSize*1.328f - v3, (448.0f - v10)/(v4-1.0f)))` at two mirror branches (~L1133, ~L1315). SubtitleStyleListener extends V4VRSubtitleListener (cue cache + italic refresh; V4Config.m_bUseSubtitleItalic volatile).
- GxrToastContinuity AZF3 gate: EXACTLY `MANUFACTURER.equalsIgnoreCase("samsung") && DEVICE=="xrvst2" && VERSION.INCREMENTAL=="I610UEU2AZF3"`. Periodic invisible Toast (300 ms shown / 3500 ms period). helper registers as Application.ActivityLifecycleCallbacks from Main4XActivity.onCreate. No ActivityManager / no trim calls. Causal mechanism (system-side memory/window pipeline) UNPROVEN.
- MetaPremium / MetaPremiumCheck (classes14): WebView at https://developer.oculus.com/manage/ with cookie scrape (oc_-prefixed + _at names) → graph.oculus.com GraphQL POST (DOC_USER 4149322231793299 / DOC_ITEM 7278635282149220 / DOC_ENTITLEMENTS 4850747515044496) against PREMIUM_ID 608908774737540. MetaPremium.restore in Main4XActivity.onCreate (line 12523); tidyBuyDialog/tidyBuyView rewire stock buy/sync buttons. Licensing-sensitive.

Groups/patches (condensed — see plan §3 for full table)
- G1 Launch (loader 1.1.58 + layer + 5 manifest edits)
- G2 Meta platform decoupling — DEFAULT-OFF (11 guarded stubs; affects VIP flows)
- G3 HDR pipeline (libplacebo + hdr_advanced + HdrBridge; HdrBridge + 5 hook sites)
- G4 Audio bridge (audio_bridge + GxrAudioOutput; MyAudioRunnable×17 + 3 player hooks)
- G5 Render dispatch (render_dispatch + RenderDispatch/FrameDispatchCore; 13 files; V4Render/V4Theatre×2/V4Window×2/ClearStatic)
- G6 Subtitle suite (subtitle_position + SubtitleFont/PositionControls/StyleListener; 11 classes; V4SubtitlesUI×3 + V4Config volatile)
- G7 Controller models (4 asset replacements)
- G8 Refresh profile (3-byte in-place edit libvr4p-oculus.so @0x30355/0x3038c)
- G9 AZF3 toast continuity (GxrToastContinuity; Main4XActivity hook)
- G10 Premium/VIP shim — EXCLUDED (MetaPremium* described only, not ported)

Recommended bundle: G1+G3+G4+G5+G6+G7+G8+G9 (G2 opt-in/default-off; G10 not ported).

Carried unknowns / UNPROVEN
- Exact view-config/FOV field rewrite inside the implicit layer (string evidence only; needs full interposer disasm).
- AZF3 memory-fix causal mechanism (system-side; no native fingerprint found).
- Exact SDR shader path ("accurate SDR color" — linear-light mapped pipeline consistent, no dedicated SDR symbol).
- Runtime acceptance of all claims on device (static-only; byte/symbol evidence; nothing executed on Galaxy XR R3GYB09JA5E here).
- Controller-model selection path in libvr4p-oculus (g_iQuestDeviceType / ForceSetQuestDeviceType exists; selection unchanged from stock — exact runtime trigger not traced).
- Bit-pattern 0x3ba1ea38 = GxrAudioOutput XrExtensions.getOpenXrWorldReferenceSpaceType() equality gate (just an equality gate at smali level; semantic UNPROVEN).
- `routeResumeSurface` exact semantics for ghosting-mitigation (handoff buffer reset is generation-counted; full pipeline behavior UNPROVEN).

Decisions pending (owner)
1. Stock APK input — pristine 4XVR 2.0.22 APK absent from workspace; required for end-to-end patch runs and installer-compat. Source/store-copy or device dump.
2. Licensing — modified libplacebo.so is LGPL-2.1+; reference build's claimed libplacebo-source.zip is ABSENT from distributed APK (no source redistribution artifact shipped). Options: private-only usage, obtain modified source, drop G3 placebo payload.
3. G2 default-off (already decided in plan); G10 EXCLUDED (already decided in plan). Owner confirms before implementation phase.

Evidence paths (this diagnostic run, written/read here)
- PLAN.md (88 lines, 14,094 B) — owner-verified synthesis.
- analysis/ (1.8 MB, 29 files) — native-analysis.md (228 lines), smali-analysis.md (112 lines), patch-architecture.md, smali-facts.json; analysis/analysis/ (28 native decoder artifacts: native-facts.json, hash-compare.json, ovrplatform_stubs.json, scan_results.json, *.py, strings_*.txt, disasm_*.txt).
- elf/ (10.9 KB, 1 file) — elf_report.json.
- real_smali_diffs2/ (26.5 KB, 15 files) — hook diffs.
- real_smali_diffs/ (94 KB, 185 files), prompts/ (35.4 KB, 10 files), venv-analysis/ (8.9 MB, 144 files; the analysis venv used by the decoder) — supplementary.
- _scratch/ (2.4 KB) — this session's helper sizes.py (kept under diag per instructions).

Retention / cleanup
- Retained (matches existing `decoded-apk-*` untracked convention): both decoded trees 4xvr-orig/ (771 MB, 44,285 files) and 4XVR-GalaxyXR/ (786 MB, 44,332 files) sit at repo root as untracked fixtures; diag root 13.8 MB / 400 files external to the repo under the toolkit builds root.
- No repo-local build/cache trees created this session.
- Python run with `python -B` (no `__pycache__` in source tree; only inside .venv/.git/.android-sdk caches which already existed before this session).
- Obsolete/empty candidates under diag root for OWNER deletion review (nothing actively broken; just candidates):
  * `venv-analysis/` (8.9 MB, 144 files) — the analysis venv if not needed again (the decoder bots own their own envs in practice; keep only if reusable).
  * `real_smali_diffs/` (94 KB, 185 files) — superseded by `real_smali_diffs2/` (the second iteration with re-numbering stripped per the normalization method).
  * `PLAN-WORKING.md.superseded-by-PLAN.md` (14,805 B) — explicit supersede marker in name.
  * `files_size_diff.txt` (403 KB) — main inventory output, kept while the run is current but redundant with `analysis/analysis/scan_results.json` (447 KB) if the workspace is later compacted.
- No deletions performed this session (per instructions).
