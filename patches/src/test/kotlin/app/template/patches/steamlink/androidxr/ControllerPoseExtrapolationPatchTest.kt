package app.template.patches.steamlink.androidxr

import app.template.patches.shared.Constants.EXPERIMENTAL_COMPATIBILITY_NAME
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ControllerPoseExtrapolationPatchTest {
    @Test
    fun `bundled layer is the build from the extension source`() {
        val library = controllerExtrapolationResource(CONTROLLER_EXTRAPOLATION_LIBRARY)

        assertContentEquals(byteArrayOf(0x7f, 0x45, 0x4c, 0x46), library.copyOfRange(0, 4))
        // extensions/controller-extrapolation-layer, NDK 28.2.13676358, arm64-v8a, Release.
        assertEquals(
            "d69f0830863e62ba9fcec1c910f53325361f847fc128cee77c0e41d34b73d693",
            MessageDigest.getInstance("SHA-256").digest(library).joinToString("") { "%02x".format(it) },
        )
        val text = String(library, Charsets.ISO_8859_1)
        assertTrue("xrNegotiateLoaderApiLayerInterface" in text)
        assertTrue("com.android.xr.flags.enable_controller_pose_extrapolation_consumer_side" in text)
    }

    @Test
    fun `manifest names the bundled library and its layer`() {
        val manifest = String(controllerExtrapolationResource(CONTROLLER_EXTRAPOLATION_MANIFEST))
        val library = String(
            controllerExtrapolationResource(CONTROLLER_EXTRAPOLATION_LIBRARY),
            Charsets.ISO_8859_1,
        )

        assertTrue("\"library_path\": \"$CONTROLLER_EXTRAPOLATION_LIBRARY\"" in manifest)
        assertTrue("\"name\": \"XR_APILAYER_local_GalaxyXR_controller_extrapolation\"" in manifest)
        assertTrue("\"disable_environment\": \"GXR_DISABLE_CONTROLLER_EXTRAPOLATION\"" in manifest)
        // The layer rejects negotiation under any other name.
        assertTrue("XR_APILAYER_local_GalaxyXR_controller_extrapolation" in library)
    }

    @Test
    fun `patch is opt-in and limited to the measured base`() {
        assertFalse(controllerPoseExtrapolationPatch.default)
        assertEquals("Controller pose extrapolation (experimental)", controllerPoseExtrapolationPatch.name)
        assertTrue(controllerPoseExtrapolationPatch.dependencies.isEmpty())

        val compatibility = controllerPoseExtrapolationPatch.compatibility.orEmpty().single()
        assertEquals(EXPERIMENTAL_COMPATIBILITY_NAME, compatibility.name)
        val target = compatibility.targets.single()
        assertEquals("2.0.23", target.version)
        assertEquals(setOf(5002363), target.versionCodes!!.values.toSet())

        assertTrue(isControllerExtrapolationBuild("2.0.23", "5002363"))
        listOf("2.0.22" to "5002363", "2.0.23" to "5002322", "2.0.23" to "5002364", "2.0.22" to "5002244")
            .forEach { (version, versionCode) ->
                assertFalse(isControllerExtrapolationBuild(version, versionCode), "$version/$versionCode")
            }
    }
}
