package app.template.patches.steamlink

import app.template.patches.shared.PatchCategories
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_STEAM_LINK_5001712
import app.template.patches.shared.Constants.COMPATIBILITIES_STEAM_LINK_5001812
import app.template.patches.shared.Constants.COMPATIBILITIES_STEAM_LINK_5001968
import app.template.patches.shared.Constants.COMPATIBILITIES_STEAM_LINK_5002363
import app.template.patches.shared.Constants.COMPATIBILITIES_STEAM_LINK_LEGACY_RECOMMENDED
import app.template.patches.steamlink.androidxr.gxrFacebridgePatch
import app.template.patches.steamlink.androidxr.gxrModernTongueBridgePatch
import app.template.patches.steamlink.androidxr.unrestrictedBatteryUsagePatch
import app.template.patches.steamlink.androidxr.xrCoreRuntimePatch
import app.template.patches.steamlink.androidxr.xrDeviceConfigBaselinePatch
import app.template.patches.steamlink.androidxr.xrGalaxyXrHighResolutionPatch
import app.template.patches.steamlink.androidxr.xrInputRoutingConfigPatch
import app.template.patches.steamlink.androidxr.xrStartupPermissionsPatch
import app.template.patches.steamlink.androidxr.xrLauncherBootstrapPatch
import app.template.patches.steamlink.androidxr.xrManifestCapabilityPackPatch
import app.template.patches.steamlink.binary.androidXrNativePermissionNamesPatch
import app.template.patches.steamlink.binary.forceHmdInitializationGatesPatch
import app.template.patches.steamlink.binary.forceLobbyPermissionStateGatePatch
import app.template.patches.steamlink.binary.forceStreamXrGatesPatch
import app.template.patches.steamlink.binary.hmdOnlyPatch
import app.template.patches.steamlink.binary.microphoneInputPresetPatch
import app.template.patches.steamlink.binary.oledCalibrationPatch
import app.template.patches.steamlink.identity.deviceIdentityPatch

// Patch.default belongs to the patch object rather than to an AppTarget. Since the
// recommendation-model change of 2026-10-12 the recommended defaults live on the individual
// patches (default = true with exact-base compatibility), so Morphe Manager auto-selects the
// recommended set for the detected version and build code in both Simple and Expert mode.
// These wrapper sets remain as optional one-step conveniences and are
// deliberately default = false.

// All legacy bundles intentionally select the same 17 public patches. Individual native edits
// still obey their exact-build guards; selecting a bundle does not verify a new layout.
private val legacyRecommendedPatches = arrayOf(
    androidXrNativePermissionNamesPatch,
    forceHmdInitializationGatesPatch,
    forceLobbyPermissionStateGatePatch,
    forceStreamXrGatesPatch,
    gxrFacebridgePatch,
    xrGalaxyXrHighResolutionPatch,
    microphoneInputPresetPatch,
    oledCalibrationPatch,
    unrestrictedBatteryUsagePatch,
    hmdOnlyPatch,
    xrCoreRuntimePatch,
    xrDeviceConfigBaselinePatch,
    xrInputRoutingConfigPatch,
    xrLauncherBootstrapPatch,
    xrStartupPermissionsPatch,
    xrManifestCapabilityPackPatch,
    deviceIdentityPatch,
)

@Suppress("unused")
val galaxyXrRecommended5001712Patch = rawResourcePatch(
    name = "Galaxy XR recommended set (2.0.20/5001712)",
    description = "Applies the 17-patch Galaxy XR set for exact 2.0.20/5001712: startup permissions and splash, Meta Quest Pro identity, high-resolution fix and the tested balanced OLED profile.",
    default = false,
) {
    category(PatchCategories.RECOMMENDED_SETS)
    compatibleWith(*COMPATIBILITIES_STEAM_LINK_5001712.toTypedArray())
    dependsOn(*legacyRecommendedPatches)
}

@Suppress("unused")
val galaxyXrRecommended5001812Patch = rawResourcePatch(
    name = "Galaxy XR recommended set (2.0.20/5001812)",
    description = "Applies the same 17-patch set as 5001712, for exact 2.0.20/5001812. Headset validation pending.",
    default = false,
) {
    category(PatchCategories.RECOMMENDED_SETS)
    compatibleWith(*COMPATIBILITIES_STEAM_LINK_5001812.toTypedArray())
    dependsOn(*legacyRecommendedPatches)
}

@Suppress("unused")
val galaxyXrRecommended5001968Patch = rawResourcePatch(
    name = "Galaxy XR recommended set (2.0.21/5001968)",
    description = "Applies the same 17-patch set as 5001712, for exact 2.0.21/5001968. Headset validation pending.",
    default = false,
) {
    category(PatchCategories.RECOMMENDED_SETS)
    compatibleWith(*COMPATIBILITIES_STEAM_LINK_5001968.toTypedArray())
    dependsOn(*legacyRecommendedPatches)
}

@Suppress("unused")
val galaxyXrRecommended5002363Patch = rawResourcePatch(
    name = "Galaxy XR recommended set (2.0.23/5002363)",
    description = "Applies the 6-patch Galaxy XR set for exact 2.0.23/5002363: high resolution, tongue bridge, battery page, Visual Delay and the balanced OLED profile. Keeps stock startup permissions. Headset validation pending.",
    default = false,
) {
    category(PatchCategories.RECOMMENDED_SETS)
    compatibleWith(*COMPATIBILITIES_STEAM_LINK_5002363.toTypedArray())
    dependsOn(
        xrGalaxyXrHighResolutionPatch,
        gxrModernTongueBridgePatch,
        microphoneInputPresetPatch,
        unrestrictedBatteryUsagePatch,
        hmdOnlyPatch,
        oledCalibrationPatch,
    )
}

@Suppress("unused")
val galaxyXrLegacyFoundationPatch = rawResourcePatch(
    name = "Galaxy XR legacy foundation (through 2.0.22/5002244)",
    description = "Selects the 17-patch Galaxy XR legacy set for builds through 2.0.22/5002244: identity, native gates, face bridge, OLED, microphone, battery, Visual Delay, startup permissions and splash.",
    default = false,
) {
    category(PatchCategories.RECOMMENDED_SETS)
    compatibleWith(*COMPATIBILITIES_STEAM_LINK_LEGACY_RECOMMENDED.toTypedArray())
    dependsOn(*legacyRecommendedPatches)
}
