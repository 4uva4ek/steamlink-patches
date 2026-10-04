package app.template.patches.steamlink.androidxr

import app.template.patches.shared.Constants.EXPERIMENTAL_COMPATIBILITY_NAME
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ControllerHalPosePatchTest {
    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test
    fun `bundled layer is the build from the extension source`() {
        val library = controllerHalPoseResource("steamlink/androidxr/$CONTROLLER_HAL_POSE_LIBRARY")

        assertContentEquals(byteArrayOf(0x7f, 0x45, 0x4c, 0x46), library.copyOfRange(0, 4))
        // extensions/controller-hal-pose, NDK 28.2.13676358, arm64-v8a, Release.
        assertEquals("b310c80b1d17978c806ccd4cbfa06a1b7b0f8da41084ee4d5544295efadf9e8e", sha256(library))
        val text = String(library, Charsets.ISO_8859_1)
        assertTrue("xrNegotiateLoaderApiLayerInterface" in text)
        // Called by the bridge class in the extension, and the interface of its user service.
        assertTrue("Java_gxr_pose_PoseBridge_nativeSetBinder" in text)
        assertTrue("gxr.pose.IPoseService" in text)
        assertTrue("pamir-stream-pose" in text)
        // Asked by the pose filter of the extrapolation layer and by the velocity frame layer.
        assertTrue("gxr_controller_hal_pose_active" in text)
        assertTrue("gxr_controller_hal_velocity_active" in text)
        assertTrue("debug.gxr.halpose.velocity" in text)
        assertTrue("debug.gxr.halpose" in text)
        assertTrue("debug.gxr.halpose.ahead" in text)
        assertTrue("debug.gxr.halpose.pitch" in text)
        assertTrue("debug.gxr.halpose.hz" in text)
        assertTrue("debug.gxr.halpose.filter" in text)
        assertTrue("debug.gxr.halpose.pos.cutoff" in text)
        assertTrue("debug.gxr.halpose.pos.beta" in text)
        assertTrue("debug.gxr.halpose.rot.cutoff" in text)
        assertTrue("debug.gxr.halpose.rot.beta" in text)
    }

    @Test
    fun `manifest names the bundled library and its layer`() {
        val manifest = String(controllerHalPoseResource("steamlink/androidxr/$CONTROLLER_HAL_POSE_MANIFEST"))
        val library = String(
            controllerHalPoseResource("steamlink/androidxr/$CONTROLLER_HAL_POSE_LIBRARY"),
            Charsets.ISO_8859_1,
        )

        assertTrue("\"library_path\": \"$CONTROLLER_HAL_POSE_LIBRARY\"" in manifest)
        assertTrue("\"name\": \"XR_APILAYER_local_GalaxyXR_controller_hal_pose\"" in manifest)
        assertTrue("\"disable_environment\": \"GXR_DISABLE_CONTROLLER_HAL_POSE\"" in manifest)
        // The layer rejects negotiation under any other name.
        assertTrue("XR_APILAYER_local_GalaxyXR_controller_hal_pose" in library)
    }

    @Test
    fun `extension adds only the pose classes`() {
        val extension = controllerHalPoseResource(CONTROLLER_HAL_POSE_EXTENSION)
        // extensions/controller-hal-pose/java, d8 --min-api 29.
        assertEquals("51674530c7baee366422f08e8c7d0e56d79fb708178981ad1f093608c7c2c4ac", sha256(extension))

        val types = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), extension.inputStream().buffered())
            .classes
            .map { it.type }
        assertEquals(setOf("Lgxr/pose/PoseBridge;", "Lgxr/pose/PoseService;"), types.toSet())
    }

    @Test
    fun `layer does not share files with the other controller layers`() {
        assertFalse(CONTROLLER_HAL_POSE_LIBRARY == CONTROLLER_EXTRAPOLATION_LIBRARY)
        assertFalse(CONTROLLER_HAL_POSE_MANIFEST == CONTROLLER_EXTRAPOLATION_MANIFEST)
        assertFalse(CONTROLLER_HAL_POSE_LIBRARY == CONTROLLER_VELOCITY_FRAME_LIBRARY)
        assertFalse(CONTROLLER_HAL_POSE_MANIFEST == CONTROLLER_VELOCITY_FRAME_MANIFEST)
        assertFalse(CONTROLLER_HAL_POSE_EXTENSION == SHIZUKU_BRIDGE_EXTENSION)
    }

    @Test
    fun `patch is opt-in, experimental and names Shizuku`() {
        assertFalse(controllerHalPosePatch.default)
        assertEquals(
            "Controller tracking from the controller HAL through Shizuku (experimental)",
            controllerHalPosePatch.name,
        )
        assertTrue("needs Shizuku" in controllerHalPosePatch.description.orEmpty())

        val compatibilities = controllerHalPosePatch.compatibility.orEmpty()
        assertTrue(compatibilities.all { it.name == EXPERIMENTAL_COMPATIBILITY_NAME })
        assertEquals(
            setOf(5001712, 5001812, 5001968, 5002244, 5002363),
            compatibilities.flatMap { it.targets }.flatMap { it.versionCodes!!.values }.toSet(),
        )

        listOf("2.0.20" to "5001712", "2.0.20" to "5001812", "2.0.21" to "5001968", "2.0.22" to "5002244",
            "2.0.23" to "5002363")
            .forEach { (version, versionCode) ->
                assertTrue(isControllerHalPoseBuild(version, versionCode), "$version/$versionCode")
            }
        listOf("2.0.23" to "5002364", "2.0.19" to "5001712", "2.0.24" to "5002363")
            .forEach { (version, versionCode) ->
                assertFalse(isControllerHalPoseBuild(version, versionCode), "$version/$versionCode")
            }
    }
}
