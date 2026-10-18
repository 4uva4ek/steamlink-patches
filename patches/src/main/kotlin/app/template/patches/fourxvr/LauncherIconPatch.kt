package app.template.patches.fourxvr

import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.Constants.isFourXvr20022Build
import app.template.patches.shared.PatchCategories
import org.w3c.dom.Element

/**
 * G11 — canonical Galaxy XR launcher icon for exact 4XVR 2.0.22/20022.
 *
 * The stock build ships its launcher icon only under the custom name
 * `@mipmap/ic_4xplayer_launch` with the adaptive XML in a nonstandard `mipmap-anydpi` directory,
 * which icon extraction tooling (such as Morphe Manager's saved-APK icon loader) renders as a
 * blank tile. This patch re-exposes the same artwork from the decoded base under the canonical
 * `ic_launcher`/`ic_launcher_round` names — adaptive XML in `mipmap-anydpi-v26` plus the stock
 * density PNGs — and points the manifest at them, the same shape the Steam Link set gives its
 * launcher icon.
 */

private val iconDensities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")

@Suppress("unused")
internal val fourXvrLauncherIconPatch = rawResourcePatch(
    name = "Galaxy XR launcher icon (4XVR)",
    description = "Restores the proper 4XVR icon on the device's app launcher and in the patch manager. Builds: 4XVR 2.0.22/20022. " +
        FOUR_XVR_UPSTREAM_CREDIT,
    default = true,
) {
    category(PatchCategories.STARTUP_AND_PERMISSIONS)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute

        val adaptiveIcon = get("res/mipmap-anydpi-v26/ic_launcher.xml")
        adaptiveIcon.parentFile!!.mkdirs()
        adaptiveIcon.writeBytes(loadFourXvrResource("icon/ic_launcher.xml"))
        get("res/mipmap-anydpi-v26/ic_launcher_round.xml")
            .writeBytes(loadFourXvrResource("icon/ic_launcher_round.xml"))

        iconDensities.forEach { density ->
            get("res/mipmap-$density/ic_launcher.png")
                .writeBytes(loadFourXvrResource("icon/ic_launcher_$density.png"))
            get("res/mipmap-$density/ic_launcher_round.png")
                .writeBytes(loadFourXvrResource("icon/ic_launcher_round_$density.png"))
        }
    }

    finalize {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@finalize
        document("AndroidManifest.xml").use { doc ->
            val application =
                doc.documentElement.getElementsByTagName("application").item(0) as Element
            application.setAttribute("android:icon", "@mipmap/ic_launcher")
            application.setAttribute("android:roundIcon", "@mipmap/ic_launcher_round")
        }
    }
}
