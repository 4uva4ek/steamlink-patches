package app.template.patches.steamlink.androidxr

import app.template.patches.shared.PatchCategories
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_STEAM_LINK_5001712
import app.template.patches.shared.Constants.COMPATIBILITIES_STEAM_LINK_5002363
import app.template.patches.shared.Constants.EXPERIMENTAL_COMPATIBILITY_NAME
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

internal const val CODECS_SUPPORT_LIBRARY = "libgxr_codecs_support.so"
internal const val CODECS_SUPPORT_MANIFEST = "XR_APILAYER_local_GalaxyXR_codecs_support.json"
internal const val CODECS_SUPPORT_LIBRARY_SHA256 =
    "d0cad4be86a9de7c92ae699f80131c65349e83c8f213edcbc85f0f5bed51f1e7"
internal const val CODECS_SUPPORT_MANIFEST_SHA256 =
    "801f2a6cf8155486879339a7eb7b83b1554a537f517ba50b6406a5a98a96fec5"

internal fun isCodecsSupportSteamLinkBuild(versionName: String, versionCode: String): Boolean =
    (versionName == "2.0.20" && versionCode == "5001712") ||
        (versionName == "2.0.23" && versionCode == "5002363")

private fun codecsSha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it) }

// Code regions the bundled layer hashes at runtime before touching the
// scene library's decoder-import GOT slots; the installer checks the same
// regions so an unknown scene layout fails before any resource is written.
// Other patches may modify their own sites elsewhere; these regions must
// stay byte-identical.
private val codecsSupportScenePreconditions: Map<String, List<Triple<Int, Int, String>>> = mapOf(
    "2.0.20/5001712" to listOf(
        Triple(0xfd304, 0x80, "6e5dcb4f5094763e948388ca35c6132af242159fc3c22e812f0fe2f4ab4f2649"),
        Triple(0xfdca8, 0x80, "86ceb824e4a972fbac71d7cb6b88a99d5decde4b42fbd1b80cb5d84361a94b67"),
        Triple(0xfd2f8, 0x40, "25174852802b65e709401364232a3dd9490617cfe9983f8bfc948a27a90e11f4"),
    ),
    "2.0.23/5002363" to listOf(
        Triple(0xfd6fc, 0x80, "f5c43c6aacb69e61c3f929f361612e1b91bb509803d2c14824b9601b3dd8c795"),
        Triple(0xfe0a0, 0x80, "e08a664f0fd116e2735f68ff8d10a0fcbc4eac8e216fd986beec191a078bddb4"),
        Triple(0xfd6f0, 0x40, "3a38bb855e0af4c544b8568414a9e022803a8c47e5a128b20126c84fb40c7eb3"),
    ),
)

internal fun validateCodecsSupportScene(
    scene: ByteArray,
    versionName: String,
    versionCode: String,
) {
    val regions = codecsSupportScenePreconditions["$versionName/$versionCode"]
        ?: throw PatchException("Codecs Support has no scene contract for $versionName/$versionCode")
    regions.forEach { (offset, size, expectedHash) ->
        if (offset + size > scene.size) {
            throw PatchException("Codecs Support found a truncated scene library at 0x${offset.toString(16)}")
        }
        if (codecsSha256(scene.copyOfRange(offset, offset + size)) != expectedHash) {
            throw PatchException("Codecs Support found an unknown scene function at 0x${offset.toString(16)}")
        }
    }
}

internal fun validateCodecsSupportPayload(helper: ByteArray, manifest: ByteArray) {
    if (helper.size < 20 || !helper.copyOfRange(0, 4)
            .contentEquals(byteArrayOf(0x7f, 0x45, 0x4c, 0x46)) ||
        helper[4] != 2.toByte() || helper[5] != 1.toByte() ||
        helper[18] != 0xb7.toByte() || helper[19] != 0.toByte()
    ) {
        throw PatchException("Bundled codecs support helper is not an ARM64 ELF library")
    }
    if (codecsSha256(helper) != CODECS_SUPPORT_LIBRARY_SHA256) {
        throw PatchException("Bundled codecs support helper failed its SHA-256 precondition")
    }
    if (codecsSha256(manifest) != CODECS_SUPPORT_MANIFEST_SHA256) {
        throw PatchException("Bundled codecs support manifest failed its SHA-256 precondition")
    }
}

/** All layout, payload and existing-file preconditions are checked before writes. */
internal fun installCodecsSupportResources(
    apkRoot: File,
    versionName: String,
    versionCode: String,
    helperBytes: ByteArray? = null,
    manifestBytes: ByteArray? = null,
): Boolean {
    // Dependencies bypass Morphe compatibility. Excluded pairs must not even read files.
    if (!isCodecsSupportSteamLinkBuild(versionName, versionCode)) return false

    val sceneFile = File(apkRoot, "lib/arm64-v8a/libvrlink_scene.so")
    if (!sceneFile.isFile) throw PatchException("Missing exact codecs support renderer library")
    validateCodecsSupportScene(sceneFile.readBytes(), versionName, versionCode)
    val helper = helperBytes ?: projectionModeResource(CODECS_SUPPORT_LIBRARY)
    val manifest = manifestBytes ?: projectionModeResource(CODECS_SUPPORT_MANIFEST)
    validateCodecsSupportPayload(helper, manifest)

    val files = listOf(
        File(sceneFile.parentFile, CODECS_SUPPORT_LIBRARY) to helper,
        File(apkRoot, "assets/openxr/1/api_layers/implicit.d/$CODECS_SUPPORT_MANIFEST") to manifest,
    )
    files.forEach { (target, expected) ->
        if (target.exists() && (!target.isFile || !target.readBytes().contentEquals(expected))) {
            throw PatchException("Codecs Support found a stale or modified resource: ${target.path}")
        }
    }
    val missing = files.filterNot { (target, _) -> target.exists() }
    if (missing.isEmpty()) return true

    val staged = mutableListOf<Pair<File, File>>()
    val created = mutableListOf<File>()
    try {
        missing.forEach { (target, bytes) ->
            if (!target.parentFile.isDirectory && !target.parentFile.mkdirs()) {
                throw PatchException("Could not create codecs support resource directory")
            }
            val temporary = File.createTempFile("gxr-codecs-support-", ".tmp", target.parentFile)
            staged += temporary to target
            temporary.writeBytes(bytes)
        }
        staged.forEach { (temporary, target) ->
            // No replacement: an unexpected concurrent file must never be overwritten.
            Files.move(temporary.toPath(), target.toPath())
            created += target
        }
    } catch (failure: Exception) {
        created.asReversed().forEach { target ->
            try { Files.deleteIfExists(target.toPath()) } catch (rollback: Exception) {
                failure.addSuppressed(rollback)
            }
        }
        throw failure
    } finally {
        staged.forEach { (temporary, _) -> Files.deleteIfExists(temporary.toPath()) }
    }
    return true
}

@Suppress("unused")
val xrCodecsSupportPatch = rawResourcePatch(
    name = "Codecs Support",
    description = "Only for exact 2.0.20/5001712 and 2.0.23/5002363: lets the headset client " +
        "decode H.264 or AV1 streams selected by the GalaxyXR driver's Stream codec setting. " +
        "A bundled OpenXR layer detects the host's codec from the first video frame and " +
        "switches the decoder in place; stock HEVC streams are untouched. Requires the " +
        "matching driver setting and headset validation.",
    default = false,
) {
    category(PatchCategories.STREAMING_AND_NETWORK)
    compatibleWith(*(COMPATIBILITIES_STEAM_LINK_5001712 + COMPATIBILITIES_STEAM_LINK_5002363)
        .map { exact ->
            Compatibility(
                name = EXPERIMENTAL_COMPATIBILITY_NAME,
                packageName = requireNotNull(exact.packageName),
                targets = exact.targets,
            )
        }.toTypedArray())
    execute {
        if (!isCodecsSupportSteamLinkBuild(packageMetadata.versionName, packageMetadata.versionCode)) {
            return@execute
        }
        val sceneFile = get("lib/arm64-v8a/libvrlink_scene.so")
        val apkRoot = sceneFile.parentFile!!.parentFile!!.parentFile!!
        installCodecsSupportResources(apkRoot, packageMetadata.versionName, packageMetadata.versionCode)
    }
}
