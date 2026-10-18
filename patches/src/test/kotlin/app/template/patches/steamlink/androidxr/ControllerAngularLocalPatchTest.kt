package app.template.patches.steamlink.androidxr

import app.morphe.patcher.patch.PatchException
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class ControllerAngularLocalPatchTest {
    @Test fun metadataIsExactExperimentalOptIn() {
        val patch = controllerAngularLocalPatch
        assertFalse(patch.default)
        assertTrue(patch.dependencies.isEmpty())
        assertEquals("Controller", patch.category)
        assertEquals(setOf("com.valvesoftware.steamlinkvr","com.valvesoftware.steamlinkvr.x_23"),
            patch.compatibility!!.map {it.packageName}.toSet())
        val compatibility = patch.compatibility!!.first()
        assertEquals("Steam Link",compatibility.name)
        val target = compatibility.targets.single()
        assertTrue(target.isExperimental)
        assertEquals("2.0.23",target.version)
        assertEquals(setOf(5002363),target.versionCodes!!.values.toSet())
        assertTrue(isControllerAngularLocalBuild("2.0.23","5002363"))
        assertFalse(isControllerAngularLocalBuild("2.0.20","5001712"))
        assertFalse(isControllerAngularLocalBuild("2.0.23","5002364"))
        assertFalse(isControllerAngularLocalBuild("2.0.22","5002363"))
        assertTrue(isControllerAngularLocalTarget("com.valvesoftware.steamlinkvr", "2.0.23", "5002363"))
        assertTrue(isControllerAngularLocalTarget("com.valvesoftware.steamlinkvr.x_23", "2.0.23", "5002363"))
        assertFalse(isControllerAngularLocalTarget("cn.vr4p.oculus4xvrplayerov", "2.0.23", "5002363"))
    }
    @Test fun mandatoryPayloadAndManifestArePresent() {
        val bytes=controllerAngularLocalResource(CONTROLLER_ANGULAR_LOCAL_LIBRARY)
        assertTrue(bytes.size>1000)
        assertEquals(0x7f,bytes[0].toInt())
        assertEquals(0xb7,bytes[18].toInt() and 255)
        val text=bytes.toString(Charsets.ISO_8859_1)
        assertTrue(text.contains("pamir-stream-pose"))
        assertTrue(text.contains("xrNegotiateLoaderApiLayerInterface"))
        val manifest=controllerAngularLocalResource(CONTROLLER_ANGULAR_LOCAL_MANIFEST).decodeToString()
        assertTrue(manifest.contains(CONTROLLER_ANGULAR_LOCAL_LIBRARY))
        assertTrue(manifest.contains("GXR_DISABLE_CONTROLLER_ANGULAR_LOCAL"))
        assertTrue(controllerAngularLocalConflicts.containsAll(listOf("controller_hal_pose","controller_velocity_frame")))
    }
    @Test fun malformedAndUnknownConsumerRejected() {
        for(bytes in listOf(byteArrayOf(),ByteArray(0x1087cc))) {
            try {validateControllerAngularLocalScene(bytes);fail("Unknown bytes accepted")}
            catch(_:PatchException) {}
        }
    }
    @Test fun exactRealConsumerAcceptedAndCorruptionRejected() {
        val input=System.getenv("GXR_ANGULAR_TEST_APK")?.let(::File)
        assumeTrue("BLOCKED: set GXR_ANGULAR_TEST_APK to retained exact 5002363 APK",input?.isFile==true)
        val bytes=ZipFile(input!!).use {it.getInputStream(it.getEntry("lib/arm64-v8a/libvrlink_scene.so")).readBytes()}
        val original=bytes.copyOf()
        validateControllerAngularLocalScene(bytes)
        assertArrayEquals(original,bytes)
        bytes[0x1082cc]=(bytes[0x1082cc].toInt() xor 1).toByte()
        try {validateControllerAngularLocalScene(bytes);fail("Changed controller call accepted")}
        catch(_:PatchException) {}
    }
}
