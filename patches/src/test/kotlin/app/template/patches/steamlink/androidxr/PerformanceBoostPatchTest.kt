package app.template.patches.steamlink.androidxr

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class PerformanceBoostPatchTest {
    @Test fun defaultsAndCompatibilityKeepRecommendedAndOptionalScopesSeparate() {
        val recommended = setOf("2.0.20" to 5001712, "2.0.20" to 5001812, "2.0.23" to 5002363)
        val optional = setOf("2.0.21" to 5001968, "2.0.22" to 5002244)
        assertEquals("OpenXR CPU and GPU BOOST", performanceBoostPatch.name)
        assertEquals("OpenXR CPU and GPU BOOST (2.0.21/2.0.22)", performanceBoostOptionalPatch.name)
        assertTrue(performanceBoostPatch.default)
        assertFalse(performanceBoostOptionalPatch.default)
        listOf(performanceBoostPatch to recommended, performanceBoostOptionalPatch to optional).forEach { (patch, expected) ->
            assertEquals("App & device identity", patch.category)
            assertTrue(patch.dependencies.isEmpty())
            val actual = patch.compatibility!!.flatMap { compatibility ->
                assertEquals("com.valvesoftware.steamlinkvr", compatibility.packageName)
                assertEquals("Steam Link", compatibility.name)
                compatibility.targets.flatMap { target ->
                    assertFalse(target.isExperimental)
                    target.versionCodes!!.values.map { target.version to it }
                }
            }.toSet()
            assertEquals(expected, actual)
        }
        (recommended + optional).forEach { (version, code) ->
            assertTrue(isPerformanceBoostTarget("com.valvesoftware.steamlinkvr", version, code.toString()))
            assertEquals((version to code) in recommended,
                isRecommendedPerformanceBoostTarget("com.valvesoftware.steamlinkvr", version, code.toString()))
            assertEquals((version to code) in optional,
                isOptionalPerformanceBoostTarget("com.valvesoftware.steamlinkvr", version, code.toString()))
            assertFalse(isPerformanceBoostTarget("cn.vr4p.oculus4xvrplayerov", version, code.toString()))
            assertFalse(isRecommendedPerformanceBoostTarget("cn.vr4p.oculus4xvrplayerov", version, code.toString()))
            assertFalse(isOptionalPerformanceBoostTarget("cn.vr4p.oculus4xvrplayerov", version, code.toString()))
        }
        listOf("2.0.22" to "5002363", "2.0.24" to "5002364", "2.0.20" to "5001968").forEach { (version, code) ->
            assertFalse(isPerformanceBoostTarget("com.valvesoftware.steamlinkvr", version, code))
            assertFalse(isRecommendedPerformanceBoostTarget("com.valvesoftware.steamlinkvr", version, code))
            assertFalse(isOptionalPerformanceBoostTarget("com.valvesoftware.steamlinkvr", version, code))
        }
    }

    @Test fun packagedLayerHasArm64EntryPointAndMatchingManifest() {
        val bytes = performanceBoostResource(PERFORMANCE_BOOST_LIBRARY)
        assertTrue(bytes.size > 1000)
        assertArrayEquals(byteArrayOf(0x7f, 69, 76, 70, 2, 1), bytes.copyOfRange(0, 6))
        assertEquals(0xb7, bytes[18].toInt() and 255)
        val strings = bytes.toString(Charsets.ISO_8859_1)
        listOf("xrNegotiateLoaderApiLayerInterface", "XR_EXT_performance_settings",
            "xrPerfSettingsSetPerformanceLevelEXT", "GxrPerformanceBoost").forEach {
            assertTrue("Missing native contract: $it", strings.contains(it))
        }
        val manifest = JsonParser.parseString(performanceBoostResource(PERFORMANCE_BOOST_MANIFEST).decodeToString())
            .asJsonObject.getAsJsonObject("api_layer")
        assertEquals(PERFORMANCE_BOOST_LIBRARY, manifest.get("library_path").asString)
        assertEquals("XR_APILAYER_local_GalaxyXR_performance_boost", manifest.get("name").asString)
        assertEquals("GXR_DISABLE_PERFORMANCE_BOOST", manifest.get("disable_environment").asString)
    }
}
