package app.template.patches.fourxvr

import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.Constants.isFourXvr20022Build
import app.template.patches.shared.PatchCategories
import app.morphe.patcher.patch.rawResourcePatch

/**
 * G7 — Galaxy XR controller models for exact 4XVR 2.0.22/20022.
 *
 * Replaces the Quest 3 / Touch Plus controller assets with the re-authored 2048² textures and
 * reduced-vertex meshes shipped by the Galaxy XR reference build. Device/model selection stays
 * native and unchanged; this patch swaps the exact asset paths it resolves.
 */

private val CONTROLLER_ASSETS = listOf(
    "MetaQuestTouchPlus_Left_BaseColor_AO.png",
    "MetaQuestTouchPlus_right_BaseColor_AO.png",
    "quest3_ctrl_l",
    "quest3_ctrl_r",
)

@Suppress("unused")
internal val fourXvrControllerModelsPatch = rawResourcePatch(
    name = "Galaxy XR controller models (4XVR)",
    description = "Replaces the four Quest 3 controller assets with the Galaxy XR reference " +
        "models (2048x2048 textures, re-authored meshes) for exact 4XVR 2.0.22 build 20022. " +
        "Static asset swap; headset validation pending. " +
        FOUR_XVR_UPSTREAM_CREDIT,
    default = true,
) {
    category(PatchCategories.TRACKING_AND_AUDIO)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute

        CONTROLLER_ASSETS.forEach { asset ->
            get("assets/$asset").writeBytes(loadFourXvrResource("controllers/$asset"))
        }
    }
}
