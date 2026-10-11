package app.template.patches.fourxvr

import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.Constants.isFourXvr20022Build
import app.template.patches.shared.PatchCategories
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

/** Iterate a [NodeList] as a [Sequence]. */
private fun NodeList.asSequence(): Sequence<Node> = sequence {
    for (i in 0 until length) yield(item(i))
}

/**
 * G1 — Galaxy XR launch compatibility for exact 4XVR 2.0.22/20022.
 *
 * Installs the Khronos OpenXR loader 1.1.58 (assets-based layer discovery + system runtime
 * broker), the 4XVR view-config/FP16/passthrough API layer with its implicit-layer manifest,
 * and the manifest declarations Android XR needs. Stock 4XVR was built against the Meta-only
 * loader 1.0 behaviour and closes right after launch on Galaxy XR.
 */

internal fun upsertApplicationChild(
    doc: Document,
    app: Element,
    tag: String,
    name: String,
    configure: (Element) -> Unit,
): Unit {
    val matches = doc.getElementsByTagName(tag).asSequence()
        .filterIsInstance<Element>()
        .filter { it.getAttribute("android:name") == name }
        .toList()
    val element = matches.firstOrNull { it.parentNode == app } ?: doc.createElement(tag).also {
        it.setAttribute("android:name", name)
        app.insertBefore(it, app.firstChild)
    }
    configure(element)
    matches.filter { it !== element && it.parentNode == app }.forEach { duplicate ->
        app.removeChild(duplicate)
    }
}

internal fun upsertManifestFeature(
    doc: Document,
    manifest: Element,
    app: Element,
    name: String,
    required: Boolean,
    version: String?,
) {
    val matches = doc.getElementsByTagName("uses-feature").asSequence()
        .filterIsInstance<Element>()
        .filter { it.getAttribute("android:name") == name }
        .toList()
    val feature = matches.firstOrNull() ?: doc.createElement("uses-feature").also {
        it.setAttribute("android:name", name)
        manifest.insertBefore(it, app)
    }
    feature.setAttribute("android:required", required.toString())
    version?.let { feature.setAttribute("android:version", it) }
        ?: feature.removeAttribute("android:version")
    matches.drop(1).forEach { duplicate -> duplicate.parentNode.removeChild(duplicate) }
}

@Suppress("unused")
internal val fourXvrLaunchCompatibilityPatch = resourcePatch(
    name = "Galaxy XR launch compatibility (4XVR)",
    description = "Installs the Khronos OpenXR loader 1.1.58, the 4XVR view-config API layer and " +
        "the Galaxy XR manifest declarations for exact 4XVR 2.0.22 build 20022. " +
        "Decoded-base adaptation; headset validation pending.",
    default = false,
) {
    category(PatchCategories.STARTUP_AND_PERMISSIONS)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute

        // Khronos OpenXR loader 1.1.58 replaces the stock Meta-era 1.0 loader. Loader 1.1's
        // AAssetManager discovery is what loads the implicit API layer from inside the APK.
        get("lib/arm64-v8a/libopenxr_loader.so")
            .writeBytes(loadFourXvrResource("compat/libopenxr_loader.so"))

        // View-config/passthrough/perf-level/FP16 interposer layer for Galaxy XR.
        get("lib/arm64-v8a/libXrApiLayer_4XVR_viewconfig_fix.so")
            .writeBytes(loadFourXvrResource("compat/libXrApiLayer_4XVR_viewconfig_fix.so"))

        val layerManifest =
            get("assets/openxr/1/api_layers/implicit.d/XR_APILAYER_4XVR_viewconfig_fix.json")
        layerManifest.parentFile!!.mkdirs()
        layerManifest.writeBytes(loadFourXvrResource("compat/XR_APILAYER_4XVR_viewconfig_fix.json"))
    }

    finalize {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@finalize
        document("AndroidManifest.xml").use { doc ->
            val manifest = doc.documentElement
            val app = manifest.getElementsByTagName("application").item(0) as Element

            // Galaxy XR has no Oculus passthrough feature; keep the declaration optional.
            upsertManifestFeature(
                doc = doc,
                manifest = manifest,
                app = app,
                name = "com.oculus.feature.PASSTHROUGH",
                required = false,
                version = null,
            )

            // OpenXR 1.1 platform runtime feature declaration (0x10001 = OpenXR 1.1).
            upsertManifestFeature(
                doc = doc,
                manifest = manifest,
                app = app,
                name = "android.software.xr.api.openxr",
                required = false,
                version = "65537",
            )

            // Native loader shim exposed by Android XR (not present on Meta devices).
            upsertApplicationChild(doc, app, "uses-native-library", "libopenxr.google.so") {
                it.setAttribute("android:required", "false")
            }

            // Full-space unmanaged start mode: required for a NativeActivity XR app on Android XR.
            upsertApplicationChild(
                doc,
                app,
                "property",
                "android.window.PROPERTY_XR_ACTIVITY_START_MODE",
            ) {
                it.setAttribute("android:value", "XR_ACTIVITY_START_MODE_FULL_SPACE_UNMANAGED")
            }

            // Android XR extensions (spatial audio/media nodes) are optional at load time.
            upsertApplicationChild(doc, app, "uses-library", "com.android.extensions.xr") {
                it.setAttribute("android:required", "false")
            }
        }
    }
}
