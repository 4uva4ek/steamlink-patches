package app.template.patches.steamlink.binary

import app.template.patches.shared.PatchCategories
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_STEAM_LINK_NATIVE_XR_EXPERIMENTAL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

// Post-startup interface rescans of the Uber UDP transport on the exact 2.0.23/5002363
// decoded base. Both sites call SVLDataLinkUber::ScanForLinkChanges via its PLT stub
// 0x21d8d0: the hidden netlink monitoring thread and the gated fallback inside
// SVLDataLinkUber::SendCurrentAddressList, reached only via Uber::Tick every 2.000001 s.
// The initial scan in SetupScanningForLinkChanges and the Tick address-list send are kept.
private data class UberLinkRescanSite(
    val name: String,
    val instructionOffset: Int,
    val instructionStock: String,
    val instructionPatched: String,
    val windowStock: String,
    val windowPatched: String,
    val regionOffset: Int,
    val regionSize: Int,
    val regionStockHash: String,
    val regionPatchedHash: String,
)

private data class UberLinkRescanDisableLayout(
    val version: String, val code: String, val size: Int, val buildId: String,
    val stockHash: String, val patchedHash: String, val sites: List<UberLinkRescanSite>,
)

// Independently decoded from the exact signed Valve APK. The build ID is read from the
// raw GNU note because Visual Delay deliberately repurposes its PT_NOTE program header.
private val uberLinkRescanDisableLayouts = listOf(
    UberLinkRescanDisableLayout("2.0.23", "5002363", 2_292_008,
        "c31bb979123b76736930d3c820d8d0619fb5bed2",
        "628821feab199d7712be8a51273eb9a21ec440a7c91aa6a768cc7307a4fe22f0",
        "836803ef2140d347772eebe1118d4ad69b1e3e59f06570a1b46b55a06c6ca91b",
        listOf(
            UberLinkRescanSite("netlink_thread_rescan", 0x174c74,
                "17a30294", "1f2003d5",
                "e4031faae5031faaa9a10294e00313aa17a30294a8e35e781f051d72e1010054",
                "e4031faae5031faaa9a10294e00313aa1f2003d5a8e35e781f051d72e1010054",
                0x174b8c, 476,
                "fee035c1efae01f6e455f2fb4ce45f165a012de4e950f3a329be5c32ba9e543c",
                "cb00f06e972b11328675796c8daba199bd593a95cdb170607e8b2f3f0ada9e57"),
            UberLinkRescanSite("tick_fallback_rescan", 0x16fdf8,
                "b6b60294", "1f2003d5",
                "a8031ff808a44a3968000034e00313aab6b60294e0630091e1031f2a02818052",
                "a8031ff808a44a3968000034e00313aa1f2003d5e0630091e1031f2a02818052",
                0x16fdb8, 464,
                "d631d0029eb7e3c8922bd4e5c023754e10706b77838cc19f00541c6baf3157e5",
                "bc2151811e332267997b1a3a7290a329571ca5d2a49b712dd1434becb162bc4e"),
        )),
)

internal fun isUberLinkRescanDisableBuild(version: String, code: String): Boolean =
    uberLinkRescanDisableLayouts.any { it.version == version && it.code == code }

private fun uberLinkRescanDisableRequire(condition: Boolean, message: String) {
    if (!condition) throw PatchException("Uber link rescan disable: $message")
}

private fun uberLinkRescanDisableBytes(hex: String): ByteArray =
    hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

private fun uberLinkRescanDisableHash(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/** Count every position holding one exact guarded window; no global replace-all is used. */
private fun uberLinkRescanDisableCountOccurrences(bytes: ByteArray, pattern: ByteArray): Int {
    var count = 0
    var index = 0
    while (index <= bytes.size - pattern.size) {
        var matched = true
        for (offset in pattern.indices) {
            if (bytes[index + offset] != pattern[offset]) {
                matched = false
                break
            }
        }
        if (matched) count++
        index++
    }
    return count
}

/** Validate the file-backed executable mapping, including after Visual Delay adds a PT_LOAD. */
private fun uberLinkRescanDisableRegionOffset(bytes: ByteArray, address: Int, size: Int, label: String): Int {
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    fun range(offset: Long, length: Long): Int {
        uberLinkRescanDisableRequire(offset >= 0 && length >= 0 && offset <= bytes.size.toLong() - length,
            "ELF range outside file")
        return offset.toInt()
    }
    fun u16(offset: Int) = buffer.getShort(range(offset.toLong(), 2)).toInt() and 0xffff
    fun u32(offset: Int) = buffer.getInt(range(offset.toLong(), 4)).toLong() and 0xffffffffL
    fun u64(offset: Int) = buffer.getLong(range(offset.toLong(), 8)).also {
        uberLinkRescanDisableRequire(it >= 0, "unsupported ELF 64-bit value")
    }
    uberLinkRescanDisableRequire(bytes.size >= 64 && bytes.copyOfRange(0, 7).contentEquals(
        byteArrayOf(0x7f, 0x45, 0x4c, 0x46, 2, 1, 1)), "expected ELF64 little-endian library")
    uberLinkRescanDisableRequire(u16(16) == 3 && u16(18) == 183 && u32(20) == 1L && u16(52) == 64,
        "expected AArch64 ET_DYN ELF header")
    uberLinkRescanDisableRequire(u16(54) == 56 && u16(56) in 1..64, "unexpected program-header layout")
    val count = u16(56)
    val start = range(u64(32), count.toLong() * 56)
    val matches = mutableListOf<Int>()
    repeat(count) { index ->
        val header = start + index * 56
        val offset = u64(header + 8)
        val fileSize = u64(header + 32)
        range(offset, fileSize)
        if (u32(header) == 1L) {
            val memorySize = u64(header + 40)
            uberLinkRescanDisableRequire(memorySize >= fileSize, "PT_LOAD memory size is smaller than file size")
            val relative = address.toLong() - u64(header + 16)
            if (relative < memorySize && relative > -size.toLong()) {
                // Reject partial or zero-fill overlaps too: a second segment must
                // not remap any bytes of this verified region.
                uberLinkRescanDisableRequire(relative >= 0 && size.toLong() <= fileSize && relative <= fileSize - size,
                    "$label overlaps a partial or non-file-backed PT_LOAD")
                uberLinkRescanDisableRequire(u32(header + 4) and 1L != 0L, "$label mapping is not executable")
                matches += range(offset + relative, size.toLong())
            }
        }
    }
    uberLinkRescanDisableRequire(matches.size == 1, "$label lacks a unique executable PT_LOAD")
    return matches.single()
}

/** Only the guarded instruction slots are normalized before hashing each containing region. */
internal fun patchUberLinkRescanDisable(bytes: ByteArray, version: String, code: String): ByteArray {
    val layout = uberLinkRescanDisableLayouts.singleOrNull { it.version == version && it.code == code }
        ?: return bytes.copyOf()
    uberLinkRescanDisableRequire(bytes.size == layout.size, "unexpected libvrlink_scene.so size for $version/$code")
    // Inspect the raw GNU note: Visual Delay deliberately repurposes its PT_NOTE header.
    val note = uberLinkRescanDisableBytes("040000001400000003000000474e5500" + layout.buildId)
    uberLinkRescanDisableRequire(bytes.copyOfRange(0x2d0, 0x2d0 + note.size).contentEquals(note),
        "GNU build ID does not match $version/$code")
    // Only an exact stock library must reproduce the verified complete patched file.
    val canonicalStock = uberLinkRescanDisableHash(bytes) == layout.stockHash
    val patched = bytes.copyOf()
    for (site in layout.sites) {
        val stock = uberLinkRescanDisableBytes(site.instructionStock)
        val guarded = uberLinkRescanDisableBytes(site.instructionPatched)
        val region = bytes.copyOfRange(site.regionOffset, site.regionOffset + site.regionSize)
        stock.copyInto(region, site.instructionOffset - site.regionOffset)
        uberLinkRescanDisableRequire(
            uberLinkRescanDisableRegionOffset(bytes, site.regionOffset, site.regionSize, site.name) == site.regionOffset,
            "${site.name} file/virtual mapping changed for $version/$code")
        uberLinkRescanDisableRequire(uberLinkRescanDisableHash(region) == site.regionStockHash,
            "${site.name} region guard failed for $version/$code")
        val instruction = bytes.copyOfRange(site.instructionOffset, site.instructionOffset + stock.size)
        val stockWindows = uberLinkRescanDisableCountOccurrences(bytes, uberLinkRescanDisableBytes(site.windowStock))
        val patchedWindows = uberLinkRescanDisableCountOccurrences(bytes, uberLinkRescanDisableBytes(site.windowPatched))
        when {
            instruction.contentEquals(guarded) -> {
                uberLinkRescanDisableRequire(patchedWindows == 1,
                    "${site.name} patched window is not unique for $version/$code")
                uberLinkRescanDisableRequire(stockWindows == 0,
                    "${site.name} unexpected stock window for $version/$code")
            }
            instruction.contentEquals(stock) -> {
                uberLinkRescanDisableRequire(stockWindows == 1,
                    "${site.name} stock window is not unique for $version/$code")
                uberLinkRescanDisableRequire(patchedWindows == 0,
                    "${site.name} unexpected patched window for $version/$code")
                guarded.copyInto(patched, site.instructionOffset)
            }
            else -> throw PatchException(
                "Uber link rescan disable: unexpected ${site.name} instruction for $version/$code")
        }
    }
    // No caller-owned data changes until every layout and code precondition passes.
    if (patched.contentEquals(bytes)) return patched
    // Verify the complete patched copy before the caller may write it.
    for (site in layout.sites) {
        val guarded = uberLinkRescanDisableBytes(site.instructionPatched)
        uberLinkRescanDisableRequire(
            patched.copyOfRange(site.instructionOffset, site.instructionOffset + guarded.size).contentEquals(guarded),
            "${site.name} patch verification failed for $version/$code")
        uberLinkRescanDisableRequire(
            uberLinkRescanDisableCountOccurrences(patched, uberLinkRescanDisableBytes(site.windowPatched)) == 1,
            "${site.name} patched window is not unique in the patched library for $version/$code")
        uberLinkRescanDisableRequire(
            uberLinkRescanDisableHash(patched.copyOfRange(site.regionOffset, site.regionOffset + site.regionSize)) ==
                site.regionPatchedHash,
            "${site.name} patched region guard failed for $version/$code")
    }
    if (canonicalStock) {
        uberLinkRescanDisableRequire(uberLinkRescanDisableHash(patched) == layout.patchedHash,
            "patched library identity failed for $version/$code")
    }
    return patched
}

@Suppress("unused")
val uberLinkRescanDisablePatch = rawResourcePatch(
    name = "Uber link rescan disable (experimental)",
    description = "Stops the post-startup interface rescans of the Uber UDP transport for exact Steam Link " +
        "2.0.23/5002363. After startup the stream cannot discover new links, so do not switch networks while " +
        "a stream is active. Experimental; no options; headset validation pending.",
    default = false,
) {
    category(PatchCategories.EXPERIMENTS)
    compatibleWith(*COMPATIBILITIES_STEAM_LINK_NATIVE_XR_EXPERIMENTAL.filter { compatibility ->
        compatibility.targets.any { target ->
            target.versionCodes?.values?.any { isUberLinkRescanDisableBuild(target.version.orEmpty(), it.toString()) } == true
        }
    }.toTypedArray())
    execute {
        val version = packageMetadata.versionName
        val code = packageMetadata.versionCode
        if (!isUberLinkRescanDisableBuild(version, code)) return@execute
        val file = get("lib/arm64-v8a/libvrlink_scene.so")
        val original = file.readBytes()
        val patched = patchUberLinkRescanDisable(original, version, code)
        if (!patched.contentEquals(original)) file.writeBytes(patched)
    }
}
