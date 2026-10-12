package app.template.patches.fourxvr

import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.PatchCategories

// Owner decision 2026-10-11: the full 4XVR Galaxy XR set is recommended by default so the whole
// adaptation applies in one step while iterating on-device. The libplacebo (LGPL-2.1-or-later)
// source obligation and the pristine-stock-APK verification stay open — see
// diagnostics/4xvr-galaxyxr/2026-10-11-4xvr-galaxyxr-recommended-update.md.

@Suppress("unused")
private val fourXvrGalaxyXrPatches = arrayOf(
    fourXvrLaunchCompatibilityPatch,
    fourXvrLauncherIconPatch,
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
    description = "Selects the 9-patch Galaxy XR set for exact 4XVR 2.0.22/20022: launch " +
        "compatibility, the canonical launcher icon, controller models, the 72 Hz refresh " +
        "profile, the libplacebo HDR pipeline, the 5.1-channel audio bridge, the frame-synced " +
        "render dispatch, the subtitle suite and the startup/UI continuity helper. Headset " +
        "validation pending; the libplacebo source obligation is tracked in the implementation " +
        "record. " +
        FOUR_XVR_UPSTREAM_CREDIT,
    default = true,
) {
    category(PatchCategories.RECOMMENDED_SETS)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())
    dependsOn(*fourXvrGalaxyXrPatches)
}
