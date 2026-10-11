package app.template.patches.fourxvr

import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.PatchCategories

// 4XVR stays outside the default-on recommended sets until a pristine stock 2.0.22/20022 APK and
// the modified libplacebo (LGPL-2.1-or-later) source availability are resolved. This opt-in set
// exists so the whole Galaxy XR adaptation can be selected in one step once those gates clear.

@Suppress("unused")
private val fourXvrGalaxyXrPatches = arrayOf(
    fourXvrLaunchCompatibilityPatch,
    fourXvrControllerModelsPatch,
    fourXvrRefreshProfilePatch,
    fourXvrHdrPipelinePatch,
    fourXvrAudioBridgePatch,
    fourXvrRenderDispatchPatch,
    fourXvrSubtitleSuitePatch,
    fourXvrToastContinuityPatch,
)

@Suppress("unused")
val fourXvrGalaxyXrSetPatch = rawResourcePatch(
    name = "Galaxy XR set (4XVR 2.0.22/20022)",
    description = "Selects the 8-patch Galaxy XR set for exact 4XVR 2.0.22/20022: launch " +
        "compatibility, controller models, the 72 Hz refresh profile, the libplacebo HDR " +
        "pipeline, the 5.1-channel audio bridge, the frame-synced render dispatch, the subtitle " +
        "suite and the startup/UI continuity helper. Gated on a pristine 2.0.22/20022 source APK " +
        "and libplacebo source availability; headset validation pending.",
    default = false,
) {
    category(PatchCategories.RECOMMENDED_SETS)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())
    dependsOn(*fourXvrGalaxyXrPatches)
}
