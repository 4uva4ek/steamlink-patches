package util

import app.template.patches.steamlink.androidxr.controllerVelocityFramePatch
import app.template.patches.steamlink.binary.hmdOnlyPatch
import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PatchListGeneratorTest {
    private val patches = setOf(hmdOnlyPatch, controllerVelocityFramePatch)

    @Test
    fun `stable channel drops every experimental-channel patch as a whole`() {
        val json = JsonParser.parseString(
            buildPatchListJson("test-version", patches, ReleaseChannel.STABLE),
        ).asJsonObject
        val names = json.getAsJsonArray("patches").map { it.asJsonObject.get("name").asString }
        assertTrue("Controller velocity frame" !in names)
        assertTrue("Visual Delay Fix" in names)
    }

    @Test
    fun `experimental and all channels keep the experimental-channel patch`() {
        listOf(ReleaseChannel.EXPERIMENTAL, ReleaseChannel.ALL).forEach { channel ->
            val json = JsonParser.parseString(
                buildPatchListJson("test-version", patches, channel),
            ).asJsonObject
            val names = json.getAsJsonArray("patches").map { it.asJsonObject.get("name").asString }
            assertTrue("Controller velocity frame" in names, channel.name)
            assertTrue("Visual Delay Fix" in names, channel.name)
        }
    }

    @Test
    fun `per-build experimental flags follow the constants build set`() {
        val json = JsonParser.parseString(
            buildPatchListJson("test-version", patches, ReleaseChannel.ALL),
        ).asJsonObject
        val entry = json.getAsJsonArray("patches")
            .single { it.asJsonObject.get("name").asString == "Controller velocity frame" }
            .asJsonObject
        val flagged = entry.getAsJsonArray("compatiblePackages").map { compat ->
            val target = compat.asJsonObject.getAsJsonArray("targets").single().asJsonObject
            Triple(
                target.get("version").asString,
                target.get("versionCodes").asJsonObject.entrySet().map { it.value.asInt }.toSet().single(),
                target.get("isExperimental").asBoolean,
            )
        }.toSet()
        assertEquals(
            setOf(
                Triple("2.0.20", 5001712, false),
                Triple("2.0.20", 5001812, true),
                Triple("2.0.21", 5001968, true),
                Triple("2.0.22", 5002244, true),
                Triple("2.0.23", 5002363, false),
            ),
            flagged,
        )
    }
}
