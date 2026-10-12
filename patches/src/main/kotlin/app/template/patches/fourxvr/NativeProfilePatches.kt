package app.template.patches.fourxvr

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.Constants.isFourXvr20022Build
import app.template.patches.shared.PatchCategories
import java.io.File

/**
 * Native in-place edits for exact 4XVR 2.0.22/20022.
 *
 * Every edit is pinned by library size + stock SHA-256 + per-site original bytes; already-patched
 * bytes are accepted so re-runs converge, anything else fails closed with a PatchException.
 */

internal data class FourXvrNativeEdit(
    val offset: Int,
    val original: ByteArray,
    val replacement: ByteArray,
)

internal fun hexBytes(hex: String): ByteArray =
    hex.split(" ").filter { it.isNotBlank() }.map { it.toInt(16).toByte() }.toByteArray()

/**
 * Applies the per-site edits. Each site must hold either the original bytes (edit) or the
 * replacement bytes (already patched, kept). Any other content fails closed.
 */
internal fun applyFourXvrEditSites(
    bytes: ByteArray,
    patchName: String,
    edits: List<FourXvrNativeEdit>,
): ByteArray {
    val mutable = bytes.copyOf()
    edits.forEach { edit ->
        require(edit.original.size == edit.replacement.size) {
            "$patchName original/replacement length mismatch"
        }
        val end = edit.offset + edit.original.size
        if (edit.offset < 0 || end > mutable.size) {
            throw PatchException(
                "$patchName offset 0x${edit.offset.toString(16)} is outside the library",
            )
        }
        val actual = mutable.copyOfRange(edit.offset, end)
        when {
            actual.contentEquals(edit.replacement) -> Unit
            actual.contentEquals(edit.original) -> edit.replacement.copyInto(mutable, edit.offset)
            else -> throw PatchException(
                "$patchName precondition failed at 0x${edit.offset.toString(16)}: " +
                    "actual=${actual.joinToString("") { "%02x".format(it) }}, " +
                    "expected=${edit.original.joinToString("") { "%02x".format(it) }}",
            )
        }
    }
    return mutable
}

internal fun applyFourXvrNativeEdits(
    bytes: ByteArray,
    patchName: String,
    fileSize: Int,
    stockSha256: String,
    edits: List<FourXvrNativeEdit>,
): ByteArray {
    if (bytes.size != fileSize) {
        throw PatchException(
            "$patchName library size=${bytes.size}; expected $fileSize, stockSha256=$stockSha256",
        )
    }
    if (bytes.sha256Hex() != stockSha256) {
        // Idempotent re-apply: accept only the exact fully-patched layout; anything else fails closed.
        val fullyPatched = edits.all { edit ->
            val end = edit.offset + edit.original.size
            edit.offset >= 0 && end <= bytes.size &&
                bytes.copyOfRange(edit.offset, end).contentEquals(edit.replacement)
        }
        if (!fullyPatched) {
            throw PatchException(
                "$patchName stockSha256 mismatch and not the patched layout; refusing to edit",
            )
        }
        return bytes.copyOf()
    }
    return applyFourXvrEditSites(bytes, patchName, edits)
}

private fun applyToFile(
    file: File,
    patchName: String,
    fileSize: Int,
    stockSha256: String,
    edits: List<FourXvrNativeEdit>,
) {
    val bytes = file.readBytes()
    val patched = applyFourXvrNativeEdits(bytes, patchName, fileSize, stockSha256, edits)
    if (!patched.contentEquals(bytes)) file.writeBytes(patched)
}

// ---------------------------------------------------------------------------
// G8 — refresh-rate picker clamp in bsVr4pProgram::InitializeDevice (3 bytes).
// Stock picks "largest supported rate < 121" (Quest Pro arm) or "< 91" (Quest 2 /
// default arm). Both floors become 73.0f so Galaxy XR's {60, 72} rates resolve to 72 Hz
// before myxrRequestDisplayRefreshRateFB is called.
// ---------------------------------------------------------------------------

internal const val VR4P_OCULUS_SIZE = 3_162_872
internal const val VR4P_OCULUS_STOCK_SHA256 =
    "df6914fa2c90887238f66c52fdaaa05efd4bce03855c836f55cd268c19a3f8f8"

internal val REFRESH_PROFILE_EDITS = listOf(
    FourXvrNativeEdit(0x30355, hexBytes("5e"), hexBytes("52")),
    FourXvrNativeEdit(0x3038c, hexBytes("c9 56"), hexBytes("49 52")),
)

@Suppress("unused")
internal val fourXvrRefreshProfilePatch = rawResourcePatch(
    name = "Galaxy XR refresh profile (72 Hz, 4XVR)",
    description = "Clamps the InitializeDevice refresh-rate picker floors (121.0f/91.0f -> 73.0f) " +
        "so exact 4XVR 2.0.22 build 20022 requests Galaxy XR's 72 Hz mode. " +
        "Three guarded bytes in libvr4p-oculus.so; runtime application pending headset validation. " +
        FOUR_XVR_UPSTREAM_CREDIT,
    default = true,
) {
    category(PatchCategories.ADVANCED_XR_COMPATIBILITY)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute
        applyToFile(
            get("lib/arm64-v8a/libvr4p-oculus.so"),
            "Galaxy XR refresh profile",
            VR4P_OCULUS_SIZE,
            VR4P_OCULUS_STOCK_SHA256,
            REFRESH_PROFILE_EDITS,
        )
    }
}

// ---------------------------------------------------------------------------
// G2 — Meta platform decoupling (default-on since 2026-10-12). 11 exported functions of
// libovrplatformloader.so are stubbed to "mov x0, #0; ret" so the platform SDK never
// initialises on a runtime without Meta Horizon services. On-device A/B evidence (Galaxy XR
// R3GYB09JA5E, 4XVR 2.0.22/20022, 2026-10-12): without the stubs the app hangs on its splash (its
// session is created but never presents a first frame); with them the UI loads. Meta-store VIP
// flows degrade, matching the reference build.
// ---------------------------------------------------------------------------

internal const val OVR_PLATFORM_LOADER_SIZE = 829_880
internal const val OVR_PLATFORM_LOADER_STOCK_SHA256 =
    "a1c5b1ec5bfc2ed17d52d9a2c2a2e4a9d5c127b53ef185baebcbb40da41bff45"

private const val OVR_STUB_HEX = "00 00 80 d2 c0 03 5f d6"

internal val OVR_STUB_EDITS = listOf(
    // ovr_PlatformInitializeAndroidHelper
    FourXvrNativeEdit(0x56948, hexBytes("ff 83 01 d1 fe 2b 00 f9"), hexBytes(OVR_STUB_HEX)),
    // ovr_PlatformInitializeAndroidInHeadlessMode
    FourXvrNativeEdit(0x85ce0, hexBytes("ff 03 01 d1 fe 1b 00 f9"), hexBytes("00 00 80 d2 c0 03 5f d6")),
    // ovr_PlatformInitializeAndroidInHeadlessModeWithOptions
    FourXvrNativeEdit(0x85d50, hexBytes("ff 43 01 d1 fe 23 00 f9"), hexBytes("00 00 80 d2 c0 03 5f d6")),
    // ovr_PlatformInitializeAndroid
    FourXvrNativeEdit(0x85e14, hexBytes("ff c3 00 d1 fe 13 00 f9"), hexBytes("00 00 80 d2 c0 03 5f d6")),
    // ovr_PlatformInitializeAndroidWithOptions
    FourXvrNativeEdit(0x85e6c, hexBytes("ff 43 01 d1 fe 23 00 f9"), hexBytes("00 00 80 d2 c0 03 5f d6")),
    // ovr_PlatformInitializeAndroidAsynchronous
    FourXvrNativeEdit(0x85f88, hexBytes("ff c3 00 d1 fe 13 00 f9"), hexBytes("00 00 80 d2 c0 03 5f d6")),
    // ovr_PlatformInitializeAndroidAsynchronousWithOptions
    FourXvrNativeEdit(0x85fe0, hexBytes("ff 43 01 d1 fe 23 00 f9"), hexBytes("00 00 80 d2 c0 03 5f d6")),
    // ovr_Entitlement_GetIsViewerEntitled
    FourXvrNativeEdit(0x8a0c0, hexBytes("fe 0f 1f f8 e8 01 00 f0"), hexBytes("00 00 80 d2 c0 03 5f d6")),
    // ovr_Message_GetType
    FourXvrNativeEdit(0x8de78, hexBytes("ff 83 00 d1 fe 0b 00 f9"), hexBytes("00 00 80 d2 c0 03 5f d6")),
    // ovr_Message_IsError
    FourXvrNativeEdit(0x8dfd8, hexBytes("ff 83 00 d1 fe 0b 00 f9"), hexBytes("00 00 80 d2 c0 03 5f d6")),
    // ovr_PopMessage
    FourXvrNativeEdit(0x8facc, hexBytes("fe 0f 1f f8 c8 01 00 d0"), hexBytes("00 00 80 d2 c0 03 5f d6")),
)

@Suppress("unused")
internal val fourXvrPlatformDecouplingPatch = rawResourcePatch(
    name = "Meta platform decoupling (4XVR)",
    description = "Stubs the 11 exported Oculus platform-init/entitlement/message functions in " +
        "libovrplatformloader.so of exact 4XVR 2.0.22 build 20022. The reference Galaxy XR build " +
        "requires the Meta platform channel to stay inert on a non-Meta runtime: without these " +
        "stubs the app hangs on the splash — the OpenXR session is created but never presents " +
        "its first frame (Galaxy XR on-device A/B, 2026-10-12). Meta-store VIP flows degrade. " +
        FOUR_XVR_UPSTREAM_CREDIT,
    default = true,
) {
    category(PatchCategories.ADVANCED_XR_COMPATIBILITY)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute
        applyToFile(
            get("lib/arm64-v8a/libovrplatformloader.so"),
            "Meta platform decoupling",
            OVR_PLATFORM_LOADER_SIZE,
            OVR_PLATFORM_LOADER_STOCK_SHA256,
            OVR_STUB_EDITS,
        )
    }
}
