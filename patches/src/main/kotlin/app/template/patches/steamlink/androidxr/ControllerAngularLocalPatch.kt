package app.template.patches.steamlink.androidxr

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.SupportedAbi
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.EXPERIMENTAL_COMPATIBILITY_NAME
import app.template.patches.shared.PatchCategories
import java.security.MessageDigest

internal const val CONTROLLER_ANGULAR_LOCAL_LIBRARY = "libgxr_controller_angular_local.so"
internal const val CONTROLLER_ANGULAR_LOCAL_MANIFEST = "XR_APILAYER_local_GalaxyXR_controller_angular_local.json"
internal fun isControllerAngularLocalBuild(version: String, code: String) = version == "2.0.23" && code == "5002363"
internal fun isControllerAngularLocalTarget(packageName: String, version: String, code: String) =
    packageName == "com.valvesoftware.steamlinkvr" &&
        isControllerAngularLocalBuild(version, code)

// These are legacy providers of local/raw velocity, incompatible with corrected base-space input.
internal val controllerAngularLocalConflicts = listOf(
    "controller_hal_pose", "controller_velocity_frame", "controller_velocity", "controller_extrapolation",
)
internal fun validateControllerAngularLocalScene(bytes: ByteArray) {
    // Pin the whole consumer function while allowing unrelated, already verified image patches.
    // Original, old test and current installed 5002363 share these exact bytes.
    if (bytes.size < 0x1087cc || bytes[0] != 0x7f.toByte() ||
        bytes.copyOfRange(1,6).toList() != listOf(69,76,70,2,1).map(Int::toByte) ||
        bytes[18] != 0xb7.toByte() || bytes[19] != 0.toByte()) {
        throw PatchException("Expected an AArch64 ELF64 Steam Link 5002363 scene library")
    }
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes.copyOfRange(0x107d30,0x1087cc))
        .joinToString("") { "%02x".format(it) }
    if (digest != "38053102f7461f9ebc9aa3a5542fdcc4baaab3a5e43c95881cd566fcf34fac5f") {
        throw PatchException("Unknown or modified Steam Link 5002363 controller consumer; no angular adapter installed")
    }
}
internal fun controllerAngularLocalResource(name: String): ByteArray =
    (object {}.javaClass.getResourceAsStream("/steamlink/androidxr/$name")
        ?: throw PatchException("Missing angular adapter resource: $name")).use { it.readBytes() }

@Suppress("unused")
val controllerAngularLocalPatch = rawResourcePatch(
    name = "Controller angular world to local (experimental)",
    description = "Adapts Galaxy XR's angular velocity to Steam Link 2.0.23's controller axes. " +
        "Builds: exact 2.0.23/5002363. Experimental there: headset validation pending. " +
        "Needs: the Velocity Frame runtime correction active in the app (it does not enable it itself); " +
        "not with stock velocities or the old velocity patches. " +
        STEAM_LINK_CONTROLLER_CREDIT,
    default = false,
) {
    category(PatchCategories.CONTROLLER)
    compatibleWith(Compatibility(
        name = EXPERIMENTAL_COMPATIBILITY_NAME,
        packageName = "com.valvesoftware.steamlinkvr",
        targets = listOf(AppTarget(
            version = "2.0.23",
            versionCodes = SupportedAbi.entries.associateWith { 5002363 },
            description = "Angular adapter for corrected base-space velocities; requires active runtime correction.",
        )),
    ))
    execute {
        if (!isControllerAngularLocalTarget(packageMetadata.packageName, packageMetadata.versionName, packageMetadata.versionCode)) return@execute
        validateControllerAngularLocalScene(get("lib/arm64-v8a/libvrlink_scene.so").readBytes())
        controllerAngularLocalConflicts.forEach { name ->
            if (get("lib/arm64-v8a/libgxr_$name.so").exists() ||
                get("assets/openxr/1/api_layers/implicit.d/XR_APILAYER_local_GalaxyXR_$name.json").exists()) {
                throw PatchException("Remove legacy $name APK layer before selecting the angular adapter")
            }
        }
        val library = get("lib/arm64-v8a/$CONTROLLER_ANGULAR_LOCAL_LIBRARY")
        val manifest = get("assets/openxr/1/api_layers/implicit.d/$CONTROLLER_ANGULAR_LOCAL_MANIFEST")
        // Load both inputs before writing either output.
        val libraryBytes = controllerAngularLocalResource(CONTROLLER_ANGULAR_LOCAL_LIBRARY)
        val manifestBytes = controllerAngularLocalResource(CONTROLLER_ANGULAR_LOCAL_MANIFEST)
        library.parentFile!!.mkdirs(); manifest.parentFile!!.mkdirs()
        library.writeBytes(libraryBytes); manifest.writeBytes(manifestBytes)
    }
    finalize {
        if (!isControllerAngularLocalTarget(packageMetadata.packageName, packageMetadata.versionName, packageMetadata.versionCode)) return@finalize
        // All execute blocks have run: also reject conflicting patches selected later in the bundle.
        controllerAngularLocalConflicts.forEach { name ->
            if (get("lib/arm64-v8a/libgxr_$name.so").exists() ||
                get("assets/openxr/1/api_layers/implicit.d/XR_APILAYER_local_GalaxyXR_$name.json").exists()) {
                throw PatchException("Angular adapter cannot be combined with legacy $name APK layer")
            }
        }
        validateControllerAngularLocalScene(get("lib/arm64-v8a/libvrlink_scene.so").readBytes())
    }
}
