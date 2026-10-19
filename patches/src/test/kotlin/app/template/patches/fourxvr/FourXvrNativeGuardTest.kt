package app.template.patches.fourxvr

import app.morphe.patcher.patch.PatchException
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Guard checks for the 4XVR native in-place edits (G8 refresh profile, G2 platform stubs).
 *
 * The synthetic cases are portable and mandatory. The decoded-byte audit runs only when the
 * untracked decoded fixtures are present and is reported as a skipped audit otherwise; it
 * compares the patched stock bytes with the reference build's exact bytes.
 */
class FourXvrNativeGuardTest {

    private fun synthetic(edits: List<FourXvrNativeEdit>): ByteArray {
        val size = edits.maxOf { it.offset + it.original.size } + 64
        return ByteArray(size) { 0x11 }.also { bytes ->
            edits.forEach { edit -> edit.original.copyInto(bytes, edit.offset) }
        }
    }

    private val suites = listOf("refresh" to REFRESH_PROFILE_EDITS, "ovr stubs" to OVR_STUB_EDITS)

    @Test
    fun `edit sites apply, keep patched bytes and reject foreign bytes`() {
        suites.forEach { (label, edits) ->
            val stock = synthetic(edits)
            val patched = applyFourXvrEditSites(stock, label, edits)
            edits.forEach { edit ->
                assertContentEquals(
                    edit.replacement,
                    patched.copyOfRange(edit.offset, edit.offset + edit.replacement.size),
                    "$label site 0x${edit.offset.toString(16)}",
                )
            }
            // Idempotent re-apply keeps the patched layout unchanged.
            assertContentEquals(patched, applyFourXvrEditSites(patched, label, edits))
            // Foreign bytes at any site fail closed.
            val foreign = stock.copyOf().also { it[edits.first().offset] = 0x7e }
            assertFailsWith<PatchException> { applyFourXvrEditSites(foreign, label, edits) }
        }
    }

    @Test
    fun `pinned guards reject wrong size and unpinned layouts`() {
        assertFailsWith<PatchException> {
            applyFourXvrNativeEdits(
                ByteArray(16),
                "refresh",
                VR4P_OCULUS_SIZE,
                VR4P_OCULUS_STOCK_SHA256,
                REFRESH_PROFILE_EDITS,
            )
        }
        // Right size, wrong hash, unpatched bytes: fail closed.
        assertFailsWith<PatchException> {
            applyFourXvrNativeEdits(
                ByteArray(VR4P_OCULUS_SIZE),
                "refresh",
                VR4P_OCULUS_SIZE,
                VR4P_OCULUS_STOCK_SHA256,
                REFRESH_PROFILE_EDITS,
            )
        }
        assertFailsWith<PatchException> {
            applyFourXvrNativeEdits(
                ByteArray(OVR_PLATFORM_LOADER_SIZE),
                "ovr stubs",
                OVR_PLATFORM_LOADER_SIZE,
                OVR_PLATFORM_LOADER_STOCK_SHA256,
                OVR_STUB_EDITS,
            )
        }
    }

    @Test
    fun `decoded stock libraries patch to the exact reference bytes`() {
        fun fixture(relative: String): File? =
            listOf(File(relative), File("../$relative")).firstOrNull(File::isFile)

        val stockVr4p = fixture("4xvr-orig/lib/arm64-v8a/libvr4p-oculus.so")
        val refVr4p = fixture("4XVR-GalaxyXR/lib/arm64-v8a/libvr4p-oculus.so")
        val stockOvr = fixture("4xvr-orig/lib/arm64-v8a/libovrplatformloader.so")
        val refOvr = fixture("4XVR-GalaxyXR/lib/arm64-v8a/libovrplatformloader.so")
        assumeTrue(
            "BLOCKED: decoded 4XVR fixtures absent; need 4xvr-orig/ and 4XVR-GalaxyXR/ next to the checkout",
            listOfNotNull(stockVr4p, refVr4p, stockOvr, refOvr).size == 4,
        )
        stockVr4p!!; refVr4p!!; stockOvr!!; refOvr!!

        val patchedVr4p = applyFourXvrNativeEdits(
            stockVr4p.readBytes(),
            "refresh",
            VR4P_OCULUS_SIZE,
            VR4P_OCULUS_STOCK_SHA256,
            REFRESH_PROFILE_EDITS,
        )
        assertEquals(
            refVr4p.readBytes().sha256Hex(),
            patchedVr4p.sha256Hex(),
            "patched libvr4p-oculus.so must equal the reference build bytes",
        )
        // Re-apply against the already patched reference layout converges to itself.
        assertEquals(
            refVr4p.readBytes().sha256Hex(),
            applyFourXvrNativeEdits(
                refVr4p.readBytes(),
                "refresh",
                VR4P_OCULUS_SIZE,
                VR4P_OCULUS_STOCK_SHA256,
                REFRESH_PROFILE_EDITS,
            ).sha256Hex(),
        )

        val patchedOvr = applyFourXvrNativeEdits(
            stockOvr.readBytes(),
            "ovr stubs",
            OVR_PLATFORM_LOADER_SIZE,
            OVR_PLATFORM_LOADER_STOCK_SHA256,
            OVR_STUB_EDITS,
        )
        assertEquals(
            refOvr.readBytes().sha256Hex(),
            patchedOvr.sha256Hex(),
            "patched libovrplatformloader.so must equal the reference build bytes",
        )
        assertEquals(
            refOvr.readBytes().sha256Hex(),
            applyFourXvrNativeEdits(
                refOvr.readBytes(),
                "ovr stubs",
                OVR_PLATFORM_LOADER_SIZE,
                OVR_PLATFORM_LOADER_STOCK_SHA256,
                OVR_STUB_EDITS,
            ).sha256Hex(),
        )
    }
}
