package app.template.patches.steamlink.binary

import app.morphe.patcher.patch.PatchException
import app.template.patches.shared.Constants.EXPERIMENTAL_COMPATIBILITY_NAME
import app.template.patches.steamlink.galaxyXrRecommended5002363Patch
import java.io.File
import java.security.MessageDigest
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class UberLinkRescanDisablePatchTest {
    private data class Site(
        val name: String, val windowOffset: Int, val instructionOffset: Int,
        val regionOffset: Int, val regionSize: Int,
    )

    private val version = "2.0.23"
    private val code = "5002363"
    private val librarySize = 2_292_008
    private val stockHash = "628821feab199d7712be8a51273eb9a21ec440a7c91aa6a768cc7307a4fe22f0"
    private val patchedHash = "836803ef2140d347772eebe1118d4ad69b1e3e59f06570a1b46b55a06c6ca91b"
    private val guardedInstruction = byteArrayOf(0x1f, 0x20, 0x03, 0xd5.toByte())

    // Exact guarded instruction slots and their 32-byte guard windows in the decoded 2.0.23/5002363 library.
    private val sites = listOf(
        Site("netlink_thread_rescan", 0x174c64, 0x174c74, 0x174b8c, 476),
        Site("tick_fallback_rescan", 0x16fde8, 0x16fdf8, 0x16fdb8, 464),
    )

    private fun fixture(): File {
        val relative = "decoded-apk-android-steamlinkvr-release-base-$version-$code/lib/arm64-v8a/libvrlink_scene.so"
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .firstOrNull { File(it, relative).isFile }
        assumeTrue("BLOCKED: actual $version/$code decoded native fixture is required", root != null)
        return File(requireNotNull(root), relative)
    }

    private fun ByteArray.hash() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(it) }

    @Test
    fun standalone_metadata_is_exact_default_off_without_dependencies_or_options() {
        val patch = uberLinkRescanDisablePatch
        assertEquals("Uber link rescan disable (experimental)", patch.name)
        assertTrue("2.0.23/5002363" in patch.description.orEmpty())
        assertTrue("Experimental" in patch.description.orEmpty())
        assertFalse(patch.default)
        assertTrue(patch.dependencies.isEmpty())
        assertTrue(patch.options.isEmpty())
        val bundle = galaxyXrRecommended5002363Patch
        assertFalse(uberLinkRescanDisablePatch in bundle.dependencies)
        assertFalse(bundle.dependencies.any { uberLinkRescanDisablePatch in it.dependencies })
        val pairs = patch.compatibility.orEmpty().flatMap { compatibility ->
            assertEquals(EXPERIMENTAL_COMPATIBILITY_NAME, compatibility.name)
            compatibility.targets.flatMap { target ->
                requireNotNull(target.versionCodes).values.map { target.version to it.toString() }
            }
        }.toSet()
        assertEquals(setOf(version to code), pairs)
    }

    @Test
    fun unsupported_exact_pairs_remain_unchanged_before_elf_access() {
        val bytes = byteArrayOf(1, 2, 3)
        listOf("2.0.22" to "5002363", "2.0.23" to "5002322", "2.0.23" to "5002364",
            "2.0.22" to "5002244", "2.0.20" to "5001712", "" to "").forEach { (otherVersion, otherCode) ->
            assertFalse(isUberLinkRescanDisableBuild(otherVersion, otherCode))
            val output = patchUberLinkRescanDisable(bytes, otherVersion, otherCode)
            assertNotSame(bytes, output)
            assertContentEquals(bytes, output)
        }
        assertFailsWith<PatchException> { patchUberLinkRescanDisable(bytes, version, code) }
    }

    @Test
    fun supported_pair_rejects_malformed_or_short_input_without_mutating_it() {
        listOf(byteArrayOf(), byteArrayOf(1, 2, 3), ByteArray(64), ByteArray(librarySize - 1),
            ByteArray(librarySize), ByteArray(librarySize + 1)).forEach { input ->
            val saved = input.copyOf()
            assertFailsWith<PatchException>("malformed ${input.size}-byte input accepted") {
                patchUberLinkRescanDisable(input, version, code)
            }
            assertContentEquals(saved, input)
        }
    }

    @Test
    fun actual_base_changes_only_the_guarded_instructions_and_is_atomic_idempotent() {
        val file = fixture()
        val bytes = file.readBytes()
        val saved = bytes.copyOf()
        assertEquals(librarySize, bytes.size)
        assertEquals(stockHash, bytes.hash())
        assertTrue(isUberLinkRescanDisableBuild(version, code))
        val patched = patchUberLinkRescanDisable(bytes, version, code)
        assertNotSame(bytes, patched)
        // The caller-owned input and the on-disk decoded library stay untouched.
        assertContentEquals(saved, bytes)
        assertEquals(bytes.size, patched.size)
        val expectedChanges = sites.flatMap { site -> (site.instructionOffset until site.instructionOffset + 4).toList() }
            .sorted()
        assertEquals(expectedChanges, bytes.indices.filter { bytes[it] != patched[it] })
        sites.forEach { site ->
            assertContentEquals(guardedInstruction,
                patched.copyOfRange(site.instructionOffset, site.instructionOffset + 4), site.name)
        }
        assertEquals(patchedHash, patched.hash())
        val reapplied = patchUberLinkRescanDisable(patched, version, code)
        assertNotSame(patched, reapplied)
        assertContentEquals(patched, reapplied)
        assertContentEquals(saved, file.readBytes())
    }

    @Test
    fun actual_base_rejects_corrupted_guard_windows_without_mutating_input() {
        val bytes = fixture().readBytes()
        assertEquals(stockHash, bytes.hash())
        fun corrupted(offset: Int) = bytes.copyOf().apply { this[offset] = (this[offset].toInt() xor 1).toByte() }
        fun rejects(input: ByteArray, label: String) {
            val saved = input.copyOf()
            assertFailsWith<PatchException>(label) { patchUberLinkRescanDisable(input, version, code) }
            assertContentEquals(saved, input, label)
        }
        rejects(bytes.copyOf(bytes.size - 1), "truncated library accepted")
        rejects(bytes.copyOf(bytes.size + 1), "extended library accepted")
        sites.forEach { site ->
            // One byte at a time across the first 16 positions of each 32-byte guarded window.
            (0 until 16).forEach { index ->
                rejects(corrupted(site.windowOffset + index),
                    "${site.name} corrupted guard window byte 0x${(site.windowOffset + index).toString(16)} accepted")
            }
            // The guarded instruction slot itself (window bytes 16-19) must also fail closed.
            (0 until 4).forEach { index ->
                rejects(corrupted(site.instructionOffset + index),
                    "${site.name} corrupted instruction byte 0x${(site.instructionOffset + index).toString(16)} accepted")
            }
            // Any other byte of the guarded region (outside the window) breaks its region hash.
            rejects(corrupted(site.regionOffset), "${site.name} corrupted region start accepted")
            rejects(corrupted(site.regionOffset + site.regionSize - 1), "${site.name} corrupted region end accepted")
        }
    }
}
