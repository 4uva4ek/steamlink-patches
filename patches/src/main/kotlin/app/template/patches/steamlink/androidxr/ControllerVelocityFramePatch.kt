package app.template.patches.steamlink.androidxr

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.SupportedAbi
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.EXPERIMENTAL_COMPATIBILITY_NAME

internal const val CONTROLLER_VELOCITY_FRAME_LIBRARY = "libgxr_controller_velocity_frame.so"
internal const val CONTROLLER_VELOCITY_FRAME_MANIFEST =
    "XR_APILAYER_local_GalaxyXR_controller_velocity_frame.json"

private data class ControllerVelocityFrameBuild(val version: String, val versionCode: Int)

// The layer edits no Steam Link code, but it keys on VRLink's pose action name and its angles
// were measured only on these exact bases.
private val CONTROLLER_VELOCITY_FRAME_BUILDS = listOf(
    ControllerVelocityFrameBuild("2.0.23", 5002363),
)

internal fun isControllerVelocityFrameBuild(version: String, versionCode: String): Boolean =
    CONTROLLER_VELOCITY_FRAME_BUILDS.any {
        it.version == version && it.versionCode.toString() == versionCode
    }

internal fun controllerVelocityFrameResource(name: String): ByteArray =
    (object {}.javaClass.getResourceAsStream("/steamlink/androidxr/$name")
        ?: throw PatchException("Missing bundled resource: steamlink/androidxr/$name"))
        .use { it.readBytes() }

@Suppress("unused")
val controllerVelocityFramePatch = rawResourcePatch(
    name = "Controller velocity frame (experimental)",
    description = "Rotates the controller velocities the Galaxy XR runtime reports into the frames SteamVR reads them in. Stock, they arrive in a frame attached to the controller, so thrown objects leave in the wrong direction. Adds an OpenXR API layer; no Steam Link code is changed.",
    default = false,
) {
    compatibleWith(*CONTROLLER_VELOCITY_FRAME_BUILDS.map { build ->
        Compatibility(
            name = EXPERIMENTAL_COMPATIBILITY_NAME,
            packageName = "com.valvesoftware.steamlinkvr",
            targets = listOf(AppTarget(
                version = build.version,
                versionCodes = SupportedAbi.entries.associateWith { build.versionCode },
                description = "Controller velocity frame layer for exact Steam Link " +
                    "${build.version}/${build.versionCode}; measured on a Galaxy XR headset.",
            )),
        )
    }.toTypedArray())

    execute {
        // Morphe dependencies do not re-check compatibility before execution.
        if (!isControllerVelocityFrameBuild(packageMetadata.versionName, packageMetadata.versionCode)) {
            return@execute
        }

        val library = get("lib/arm64-v8a/$CONTROLLER_VELOCITY_FRAME_LIBRARY")
        library.parentFile!!.mkdirs()
        library.writeBytes(controllerVelocityFrameResource(CONTROLLER_VELOCITY_FRAME_LIBRARY))

        val manifest = get("assets/openxr/1/api_layers/implicit.d/$CONTROLLER_VELOCITY_FRAME_MANIFEST")
        manifest.parentFile!!.mkdirs()
        manifest.writeBytes(controllerVelocityFrameResource(CONTROLLER_VELOCITY_FRAME_MANIFEST))
    }
}
