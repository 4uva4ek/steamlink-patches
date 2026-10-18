package app.template.patches.steamlink.androidxr

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.SupportedAbi
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.EXPERIMENTAL_COMPATIBILITY_NAME
import app.template.patches.shared.PatchCategories

internal const val CONTROLLER_VELOCITY_FRAME_LIBRARY = "libgxr_controller_velocity_frame.so"
internal const val CONTROLLER_VELOCITY_FRAME_MANIFEST =
    "XR_APILAYER_local_GalaxyXR_controller_velocity_frame.json"

private data class ControllerVelocityFrameBuild(
    val version: String,
    val versionCode: Int,
    val measured: Boolean,
    val angularWorld: Boolean,
)

// The layer edits no Steam Link code, but it keys on VRLink's pose action name and its angles
// depend on the controller pose offset. All 5 bases use the same action name and, with this
// repository's controller_config.json, the same offset; the angles were measured on 5002363 only.
// One patch covers all 5 bases; the angular-velocity frame written into the bundled layer differs
// per build (2.0.23: the streamed-pose frame; the legacy bases: the base-space frame).
private val CONTROLLER_VELOCITY_FRAME_BUILDS = listOf(
    ControllerVelocityFrameBuild("2.0.23", 5002363, measured = true, angularWorld = false),
    ControllerVelocityFrameBuild("2.0.20", 5001712, measured = false, angularWorld = true),
    ControllerVelocityFrameBuild("2.0.20", 5001812, measured = false, angularWorld = true),
    ControllerVelocityFrameBuild("2.0.21", 5001968, measured = false, angularWorld = true),
    ControllerVelocityFrameBuild("2.0.22", 5002244, measured = false, angularWorld = true),
)

private fun List<ControllerVelocityFrameBuild>.has(version: String, versionCode: String): Boolean =
    any { it.version == version && it.versionCode.toString() == versionCode }

internal fun isControllerVelocityFrameNativeBuild(version: String, versionCode: String): Boolean =
    CONTROLLER_VELOCITY_FRAME_BUILDS.filter { !it.angularWorld }.has(version, versionCode)

internal fun isControllerVelocityFrameLegacyBuild(version: String, versionCode: String): Boolean =
    CONTROLLER_VELOCITY_FRAME_BUILDS.filter { it.angularWorld }.has(version, versionCode)

internal fun isControllerVelocityFrameBuild(version: String, versionCode: String): Boolean =
    CONTROLLER_VELOCITY_FRAME_BUILDS.has(version, versionCode)

internal fun controllerVelocityFrameResource(name: String): ByteArray =
    (object {}.javaClass.getResourceAsStream("/steamlink/androidxr/$name")
        ?: throw PatchException("Missing bundled resource: steamlink/androidxr/$name"))
        .use { it.readBytes() }

internal fun controllerVelocityFrameLibrary(angularWorld: Boolean): ByteArray =
    controllerVelocityFrameResource(CONTROLLER_VELOCITY_FRAME_LIBRARY)
        .withAngularVelocityFrame(CONTROLLER_VELOCITY_FRAME_CONFIG_MAGIC, angularWorld)

private const val CONTROLLER_VELOCITY_FRAME_DESCRIPTION =
    "Fixes thrown-object directions by rotating controller speeds into the frame SteamVR expects."

@Suppress("unused")
val controllerVelocityFramePatch = rawResourcePatch(
    name = "Controller velocity frame",
    description = "$CONTROLLER_VELOCITY_FRAME_DESCRIPTION Builds: 2.0.23, angular velocity is " +
        "reported local to the streamed pose; 2.0.20 - 2.0.22, it is reported in the base space. " +
        "Needs: nothing. " +
        STEAM_LINK_CONTROLLER_CREDIT,
    default = false,
) {
    category(PatchCategories.CONTROLLER)
    compatibleWith(*CONTROLLER_VELOCITY_FRAME_BUILDS.map { build ->
        Compatibility(
            name = EXPERIMENTAL_COMPATIBILITY_NAME,
            packageName = "com.valvesoftware.steamlinkvr",
            targets = listOf(AppTarget(
                version = build.version,
                versionCodes = SupportedAbi.entries.associateWith { build.versionCode },
                description = "Controller velocity frame layer for exact Steam Link " +
                    "${build.version}/${build.versionCode}; " +
                    if (build.measured) "measured on a Galaxy XR headset."
                    else "measured on 2.0.23/5002363 only, not run on this base.",
            )),
        )
    }.toTypedArray())

    execute {
        // Morphe dependencies do not re-check compatibility before execution.
        val build = CONTROLLER_VELOCITY_FRAME_BUILDS.singleOrNull {
            it.version == packageMetadata.versionName &&
                it.versionCode.toString() == packageMetadata.versionCode
        } ?: return@execute

        val library = get("lib/arm64-v8a/$CONTROLLER_VELOCITY_FRAME_LIBRARY")
        library.parentFile!!.mkdirs()
        library.writeBytes(controllerVelocityFrameLibrary(build.angularWorld))

        val manifest = get("assets/openxr/1/api_layers/implicit.d/$CONTROLLER_VELOCITY_FRAME_MANIFEST")
        manifest.parentFile!!.mkdirs()
        manifest.writeBytes(controllerVelocityFrameResource(CONTROLLER_VELOCITY_FRAME_MANIFEST))
    }
}
