package app.template.patches.steamlink.androidxr

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * Pins the shipped Galaxy XR bridge payload and its neutralised telemetry transport.
 *
 * The bridge is a prebuilt library with no source in this repository. Its telemetry
 * (`gxr::TelemetryTransport`: TCP control socket + UDP tracking channel) is disabled by
 * stubbing the transport entry point, so the layer can never start it even if a manifest
 * supplies `gxr.telemetry.*` meta-data later.
 */
class XrBridgeTelemetryRemovalTest {
    private val bridge: ByteArray = object {}.javaClass
        .getResourceAsStream("/steamlink/androidxr/libgxr_xr_bridge.so")!!
        .use { it.readBytes() }

    private val elf: ByteBuffer = ByteBuffer.wrap(bridge).order(ByteOrder.LITTLE_ENDIAN)

    private fun u16(at: Int): Int = elf.getShort(at).toInt() and 0xffff
    private fun u32(at: Int): Long = elf.getInt(at).toLong() and 0xffffffffL
    private fun u64(at: Int): Long = elf.getLong(at)

    private data class Section(
        val name: String,
        val type: Long,
        val offset: Long,
        val size: Long,
        val link: Int,
        val entitySize: Long,
    )

    private data class Symbol(val name: String, val value: Long, val size: Long)

    private fun sections(): List<Section> {
        val headerOffset = u64(40)
        val entrySize = u16(58)
        val count = u16(60)
        val stringSection = u16(62)

        data class Raw(
            val nameOffset: Int,
            val type: Long,
            val offset: Long,
            val size: Long,
            val link: Int,
            val entitySize: Long,
        )

        val raw = (0 until count).map { index ->
            val at = (headerOffset + index.toLong() * entrySize).toInt()
            Raw(
                nameOffset = u32(at).toInt(),
                type = u32(at + 4),
                offset = u64(at + 24),
                size = u64(at + 32),
                link = u32(at + 40).toInt(),
                entitySize = u64(at + 56),
            )
        }
        val stringOffset = raw[stringSection].offset.toInt()
        fun name(offset: Int): String {
            val start = stringOffset + offset
            val end = (start until bridge.size).first { bridge[it] == 0.toByte() }
            return String(bridge, start, end - start, Charsets.US_ASCII)
        }
        return raw.map { Section(name(it.nameOffset), it.type, it.offset, it.size, it.link, it.entitySize) }
    }

    private fun symbols(): List<Symbol> {
        val table = sections().first { it.type == 2L }
        val strings = sections()[table.link].offset.toInt()
        fun name(offset: Int): String {
            val start = strings + offset
            val end = (start until bridge.size).first { bridge[it] == 0.toByte() }
            return String(bridge, start, end - start, Charsets.US_ASCII)
        }
        return (0 until (table.size / table.entitySize).toInt()).mapNotNull { index ->
            val at = (table.offset + index.toLong() * table.entitySize).toInt()
            val nameOffset = u32(at).toInt()
            if (nameOffset == 0) null else Symbol(name(nameOffset), u64(at + 8), u64(at + 16))
        }
    }

    private fun fileOffset(virtualAddress: Long): Int {
        val headerOffset = u64(32)
        val entrySize = u16(54)
        val count = u16(56)
        (0 until count).forEach { index ->
            val at = (headerOffset + index.toLong() * entrySize).toInt()
            val type = u32(at)
            val offset = u64(at + 8)
            val address = u64(at + 16)
            val fileSize = u64(at + 32)
            if (type == 1L && virtualAddress >= address && virtualAddress < address + fileSize) {
                return (offset + (virtualAddress - address)).toInt()
            }
        }
        throw AssertionError("Virtual address 0x${virtualAddress.toString(16)} is not mapped")
    }

    @Test
    fun payloadMatchesPinnedNeutralisedBuild() {
        assertEquals(181_984, bridge.size)
        assertEquals(
            "31b99d1c210d833841f427a2428474918a60982d1c6af26fac3d25918b66aa9d",
            MessageDigest.getInstance("SHA-256").digest(bridge).joinToString("") { "%02x".format(it) },
        )
    }

    @Test
    fun telemetryTransportStartIsAPlainReturn() {
        val start = symbols().single { it.name == "_ZN3gxr18TelemetryTransport5StartEv" }
        assertEquals(0xd8d4L, start.value)
        assertEquals(492L, start.size)
        val at = fileOffset(start.value)
        assertArrayEquals(
            byteArrayOf(0xc0.toByte(), 0x03.toByte(), 0x5f.toByte(), 0xd6.toByte()),
            bridge.copyOfRange(at, at + 4),
        )
    }
}
