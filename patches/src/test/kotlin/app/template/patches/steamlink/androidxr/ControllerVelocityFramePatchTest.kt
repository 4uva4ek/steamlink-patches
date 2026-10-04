package app.template.patches.steamlink.androidxr

import app.template.patches.shared.Constants.EXPERIMENTAL_COMPATIBILITY_NAME
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ControllerVelocityFramePatchTest {
    @Test
    fun `bundled layer is the build from the extension source`() {
        val library = controllerVelocityFrameResource(CONTROLLER_VELOCITY_FRAME_LIBRARY)

        assertContentEquals(byteArrayOf(0x7f, 0x45, 0x4c, 0x46), library.copyOfRange(0, 4))
        // extensions/controller-velocity-frame-layer, NDK 28.2.13676358, arm64-v8a, Release.
        assertEquals(
            "dce59f7100d003a04efebece7e9ac156d7f4e408ac949c897c88dfa6f8e0ca89",
            MessageDigest.getInstance("SHA-256").digest(library).joinToString("") { "%02x".format(it) },
        )
        val text = String(library, Charsets.ISO_8859_1)
        assertTrue("xrNegotiateLoaderApiLayerInterface" in text)
        // The action VRLink streams the controllers from, and the switches read at start.
        assertTrue("pamir-stream-pose" in text)
        assertTrue("debug.gxr.velocity_frame" in text)
        assertTrue("debug.gxr.velocity_pitch_linear" in text)
        assertTrue("debug.gxr.velocity_pitch_angular" in text)
    }

    @Test
    fun `manifest names the bundled library and its layer`() {
        val manifest = String(controllerVelocityFrameResource(CONTROLLER_VELOCITY_FRAME_MANIFEST))
        val library = String(
            controllerVelocityFrameResource(CONTROLLER_VELOCITY_FRAME_LIBRARY),
            Charsets.ISO_8859_1,
        )

        assertTrue("\"library_path\": \"$CONTROLLER_VELOCITY_FRAME_LIBRARY\"" in manifest)
        assertTrue("\"name\": \"XR_APILAYER_local_GalaxyXR_controller_velocity_frame\"" in manifest)
        assertTrue("\"disable_environment\": \"GXR_DISABLE_CONTROLLER_VELOCITY_FRAME\"" in manifest)
        // The layer rejects negotiation under any other name.
        assertTrue("XR_APILAYER_local_GalaxyXR_controller_velocity_frame" in library)
    }

    @Test
    fun `layer does not share files with the other controller layers`() {
        assertFalse(CONTROLLER_VELOCITY_FRAME_LIBRARY == CONTROLLER_EXTRAPOLATION_LIBRARY)
        assertFalse(CONTROLLER_VELOCITY_FRAME_MANIFEST == CONTROLLER_EXTRAPOLATION_MANIFEST)
        // The legacy controller velocity layer keeps its own names.
        assertFalse(CONTROLLER_VELOCITY_FRAME_LIBRARY == "libgxr_controller_velocity.so")
        assertFalse(CONTROLLER_VELOCITY_FRAME_MANIFEST == "XR_APILAYER_local_GalaxyXR_controller_velocity.json")
    }

    @Test
    fun `patch is opt-in and limited to the measured base`() {
        assertFalse(controllerVelocityFramePatch.default)
        assertEquals("Controller velocity frame (experimental)", controllerVelocityFramePatch.name)
        assertTrue(controllerVelocityFramePatch.dependencies.isEmpty())

        val compatibility = controllerVelocityFramePatch.compatibility.orEmpty().single()
        assertEquals(EXPERIMENTAL_COMPATIBILITY_NAME, compatibility.name)
        val target = compatibility.targets.single()
        assertEquals("2.0.23", target.version)
        assertEquals(setOf(5002363), target.versionCodes!!.values.toSet())

        assertTrue(isControllerVelocityFrameBuild("2.0.23", "5002363"))
        listOf("2.0.22" to "5002363", "2.0.23" to "5002322", "2.0.23" to "5002364", "2.0.22" to "5002244")
            .forEach { (version, versionCode) ->
                assertFalse(isControllerVelocityFrameBuild(version, versionCode), "$version/$versionCode")
            }
    }
}
