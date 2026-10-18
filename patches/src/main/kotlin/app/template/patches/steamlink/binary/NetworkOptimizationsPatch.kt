package app.template.patches.steamlink.binary

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_STEAM_LINK_5002363
import app.template.patches.shared.PatchCategories
import java.security.MessageDigest

// Steam Link 2.0.23/5002363 streaming-transport edits, frozen in
// builds/steamlink-patches/build/transport-optimisation-20261011/gate0-network-optimizations.json.
// Every window below is byte-exact and unique in the shipped library, on the exact base:
//   libvrlink_scene.so  size 2,292,008    sha256 628821feab199d7712be8a51273eb9a21ec440a7c91aa6a768cc7307a4fe22f0
//   libmain.so          size 15,826,584   sha256 1bc350c5d0d2cb6501680f98d55512c516802df7d78fb48f8a142baf0d2f5acb
// All transforms are fail-closed at window granularity: the exact stock window must be found
// verbatim (and unique in the file), or the patch aborts with the current file's size and hash.
// A full-file stock hash is NOT required: in a Morphe session other patches may have edited the
// same library before this one runs (this patch edits libvrlink_scene.so and libmain.so; the
// microphone preset, the GXR tongue block, the Visual Delay hook and the OLED calibration all
// touch the scene library first in the recommended set). The frozen hashes below remain the
// pristine-APK expectations and the re-application fast path.

private const val SCENE_SIZE = 2_292_008
// Pristine-APK expectations, kept for diagnostics and records; the per-window guards below are
// authoritative because earlier patches in a session may already have edited the file.
internal const val SCENE_SHA256 = "628821feab199d7712be8a51273eb9a21ec440a7c91aa6a768cc7307a4fe22f0"
private const val SCENE_PATCHED_SHA256 = "4d5ff6937d07747495b82a412592ce017d6bdadd54c8264e1e49ebb24b5aa72c"

private const val MAIN_SIZE = 15_826_584
internal const val MAIN_SHA256 = "1bc350c5d0d2cb6501680f98d55512c516802df7d78fb48f8a142baf0d2f5acb"
private const val MAIN_PATCHED_SHA256 = "17c1a4d91351190fcf2a996c1cb7673faac3bdde8aee70716cb07e295b950ade"

// E1 - libvrlink_scene.so 0x166b60, 44-byte window in SVLFECRXFrame::CheckRetry's abort block.
// Stock logs "too many packets were missing even to retransmit.  Aborting stream.", sets the
// deferred abort latch (strb w9=1 -> SVLFEC+0xc50) and branches to the abort exit at 0x166d98,
// dropping the 512 retransmit-request IDs already accumulated in the sp+0x28 request buffer.
// The replacement keeps the log call, NOPs the latch set and the accompanying zeroing stores,
// and retargets the branch to the normal send-path entry 0x166cd0, so the accumulated request
// (512 IDs, fits the 0x430 frame) is sent. The log line now reports fallback activation, not an
// aborted stream.
private const val E1_OFFSET = 0x166b60
private val E1_STOCK = byteArrayOf(
    0x00, 0xfa.toByte(), 0xff.toByte(), 0xf0.toByte(), 0x61, 0x0a, 0x40, 0xb9.toByte(),
    0x00, 0x54, 0x18, 0x91.toByte(), 0xd9.toByte(), 0xca.toByte(), 0x02, 0x94.toByte(),
    0x68, 0x22, 0x61, 0xf9.toByte(), 0x29, 0x00, 0x80.toByte(), 0x52,
    0xf4.toByte(), 0x03, 0x1f, 0x2a, 0x09, 0x41, 0x31, 0x39,
    0x86.toByte(), 0x00, 0x00, 0x14, 0xe9.toByte(), 0xff.toByte(), 0x9f.toByte(), 0x52,
    0x34, 0x00, 0x80.toByte(), 0x52,
)
private val E1_REPLACEMENT = byteArrayOf(
    0x00, 0xfa.toByte(), 0xff.toByte(), 0xf0.toByte(), 0x61, 0x0a, 0x40, 0xb9.toByte(),
    0x00, 0x54, 0x18, 0x91.toByte(), 0xd9.toByte(), 0xca.toByte(), 0x02, 0x94.toByte(),
    0x1f, 0x20, 0x03, 0xd5.toByte(), 0x1f, 0x20, 0x03, 0xd5.toByte(),
    0x1f, 0x20, 0x03, 0xd5.toByte(), 0x1f, 0x20, 0x03, 0xd5.toByte(),
    0x54, 0x00, 0x00, 0x14, 0xe9.toByte(), 0xff.toByte(), 0x9f.toByte(), 0x52,
    0x34, 0x00, 0x80.toByte(), 0x52,
)

// E2 - libvrlink_scene.so 0x167904, 16-byte window; the reorder bound `cmp w24, #6` at 0x16790c
// becomes `cmp w24, #12` (imm 0x006 -> 0x00c), widening the frame-reorder (backtrack) window
// from 6 to 12 frames.
private const val E2_OFFSET = 0x167904
private val E2_STOCK = byteArrayOf(
    0xf8.toByte(), 0x03, 0x8b.toByte(), 0x1a, 0xff.toByte(), 0x02, 0x00, 0xf9.toByte(),
    0x1f, 0x1b, 0x00, 0x71, 0x56, 0x21, 0x07, 0xb9.toByte(),
)
private val E2_REPLACEMENT = byteArrayOf(
    0xf8.toByte(), 0x03, 0x8b.toByte(), 0x1a, 0xff.toByte(), 0x02, 0x00, 0xf9.toByte(),
    0x1f, 0x33, 0x00, 0x71, 0x56, 0x21, 0x07, 0xb9.toByte(),
)

// E3 - libvrlink_scene.so 0x17575c, 16-byte window in TransferUDP::Restart; the SO_SNDBUF value
// `mov w8, #0x20000` (128 KiB) becomes `mov w8, #0x100000` (1 MiB), so a burst of accumulated
// retransmission requests no longer overruns the UDP socket's send buffer.
private const val E3_OFFSET = 0x17575c
private val E3_STOCK = byteArrayOf(
    0x48, 0x00, 0xa0.toByte(), 0x52, 0x80.toByte(), 0x9a.toByte(), 0x40, 0xb9.toByte(),
    0xe8.toByte(), 0x0b, 0x00, 0xb9.toByte(), 0xe3.toByte(), 0x23, 0x00, 0x91.toByte(),
)
private val E3_REPLACEMENT = byteArrayOf(
    0x08, 0x02, 0xa0.toByte(), 0x52, 0x80.toByte(), 0x9a.toByte(), 0x40, 0xb9.toByte(),
    0xe8.toByte(), 0x0b, 0x00, 0xb9.toByte(), 0xe3.toByte(), 0x23, 0x00, 0x91.toByte(),
)

// E4 - libmain.so 0x8ec5c0, 16-byte window in the decoder's TRY_AGAIN_LATER branch; the
// AMediaCodec_flush call (bl at 0x8ec5c4) becomes a NOP, so a momentarily-starving decoder
// keeps its buffered frames instead of flushing them.
private const val E4_OFFSET = 0x8ec5c0
private val E4_STOCK = byteArrayOf(
    0x00, 0x30, 0x40, 0xf9.toByte(), 0xff.toByte(), 0x36, 0x0c, 0x94.toByte(),
    0x20, 0x00, 0x80.toByte(), 0x52, 0x97.toByte(), 0xff.toByte(), 0xff.toByte(), 0x17,
)
private val E4_REPLACEMENT = byteArrayOf(
    0x00, 0x30, 0x40, 0xf9.toByte(), 0x1f, 0x20, 0x03, 0xd5.toByte(),
    0x20, 0x00, 0x80.toByte(), 0x52, 0x97.toByte(), 0xff.toByte(), 0xff.toByte(), 0x17,
)

private fun ByteArray.matchesAt(offset: Int, pattern: ByteArray): Boolean =
    offset >= 0 && offset + pattern.size <= size &&
        pattern.indices.all { this[offset + it] == pattern[it] }

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

// Idempotent, fail-closed window write: an already-patched window is left alone, a stock window
// must match byte-exactly and be unique in the file, anything else aborts with the current
// file's size and hash so a drifted base is diagnosable from the error alone.
private fun replaceExact(
    bytes: ByteArray,
    offset: Int,
    stock: ByteArray,
    replacement: ByteArray,
    label: String,
): ByteArray {
    if (stock.size != replacement.size) {
        throw PatchException("$label stock/replacement lengths differ: ${stock.size} != ${replacement.size}")
    }
    if (bytes.matchesAt(offset, replacement)) return bytes.copyOf()
    if (!bytes.matchesAt(offset, stock)) {
        throw PatchException(
            "$label did not match at 0x${offset.toString(16)} " +
                "(size=${bytes.size}, sha256=${sha256(bytes)})",
        )
    }
    var occurrences = 0
    outer@ for (i in 0..bytes.size - stock.size) {
        for (j in stock.indices) {
            if (bytes[i + j] != stock[j]) continue@outer
        }
        occurrences++
    }
    if (occurrences != 1) {
        throw PatchException("ambiguous $label stock window: $occurrences occurrences at 0x${offset.toString(16)}")
    }
    return bytes.copyOf().apply { replacement.copyInto(this, offset) }
}

internal fun applyNetworkOptimizationsScene(bytes: ByteArray): ByteArray {
    if (bytes.size != SCENE_SIZE) {
        throw PatchException("unexpected libvrlink_scene.so size=${bytes.size}; expected $SCENE_SIZE for 2.0.23/5002363")
    }
    if (sha256(bytes) == SCENE_PATCHED_SHA256) return bytes.copyOf()
    // No full-file stock-hash gate here: earlier patches in the same session may have edited this
    // library already (the per-window guards below verify exactly the regions this patch owns).
    val e1 = replaceExact(bytes, E1_OFFSET, E1_STOCK, E1_REPLACEMENT, "E1 retransmit-abort fallback")
    val e2 = replaceExact(e1, E2_OFFSET, E2_STOCK, E2_REPLACEMENT, "E2 backtrack window")
    return replaceExact(e2, E3_OFFSET, E3_STOCK, E3_REPLACEMENT, "E3 UDP send buffer")
}

internal fun applyNetworkOptimizationsMain(bytes: ByteArray): ByteArray {
    if (bytes.size != MAIN_SIZE) {
        throw PatchException("unexpected libmain.so size=${bytes.size}; expected $MAIN_SIZE for 2.0.23/5002363")
    }
    if (sha256(bytes) == MAIN_PATCHED_SHA256) return bytes.copyOf()
    return replaceExact(bytes, E4_OFFSET, E4_STOCK, E4_REPLACEMENT, "E4 decoder flush removal")
}

@Suppress("unused")
val networkOptimizationsPatch = rawResourcePatch(
    name = "Network Optimizations",
    description = "Keeps streaming smooth after Wi-Fi hiccups: lost packets are re-requested, recent " +
        "video frames are kept, and bigger buffers absorb bursts; headset verification still " +
        "pending. Builds: 2.0.23/5002363 only.",
    default = true,
) {
    category(PatchCategories.STREAMING_AND_NETWORK)
    compatibleWith(*COMPATIBILITIES_STEAM_LINK_5002363.toTypedArray())

    execute {
        val scene = get("lib/arm64-v8a/libvrlink_scene.so")
        scene.writeBytes(applyNetworkOptimizationsScene(scene.readBytes()))
        val main = get("lib/arm64-v8a/libmain.so")
        main.writeBytes(applyNetworkOptimizationsMain(main.readBytes()))
    }
}
