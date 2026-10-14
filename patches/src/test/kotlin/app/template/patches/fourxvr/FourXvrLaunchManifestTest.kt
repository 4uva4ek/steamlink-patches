package app.template.patches.fourxvr

import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Fixture check for the G1 launch-compatibility manifest upserts. Synthetic XML supplements the
 * decoded-base evidence; it does not replace a real APK audit (tracked portable test).
 */
class FourXvrLaunchManifestTest {

    private val stockLikeManifest = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="cn.vr4p.oculus4xvrplayerov">
            <uses-feature android:name="com.oculus.feature.PASSTHROUGH" android:required="true"/>
            <uses-feature android:name="com.oculus.feature.PASSTHROUGH" android:required="true"/>
            <application android:label="4XVR">
                <activity android:name=".Main4XActivity"/>
            </application>
        </manifest>
    """.trimIndent()

    private fun parse(xml: String): Document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())

    @Test
    fun `launch manifest upserts produce the Galaxy XR declarations and drop duplicates`() {
        val doc = parse(stockLikeManifest)
        val manifest = doc.documentElement
        val app = manifest.getElementsByTagName("application").item(0) as Element

        upsertManifestFeature(doc, manifest, app, "com.oculus.feature.PASSTHROUGH", required = false, version = null)
        upsertManifestFeature(doc, manifest, app, "android.software.xr.api.openxr", required = false, version = "65537")
        upsertManifestFeature(doc, manifest, app, "android.hardware.xr.input.controller", required = false, version = null)
        upsertManifestFeature(doc, manifest, app, "android.hardware.xr.input.hand_tracking", required = false, version = null)
        upsertManifestFeature(doc, manifest, app, "android.hardware.xr.input.eye_tracking", required = false, version = null)
        upsertManifestPermission(doc, manifest, app, "android.permission.HAND_TRACKING")
        upsertManifestPermission(doc, manifest, app, "android.permission.EYE_TRACKING_FINE")
        upsertApplicationChild(doc, app, "uses-native-library", "libopenxr.google.so") {
            it.setAttribute("android:required", "false")
        }
        upsertApplicationChild(doc, app, "property", "android.window.PROPERTY_XR_ACTIVITY_START_MODE") {
            it.setAttribute("android:value", "XR_ACTIVITY_START_MODE_FULL_SPACE_UNMANAGED")
        }
        upsertApplicationChild(doc, app, "uses-library", "com.android.extensions.xr") {
            it.setAttribute("android:required", "false")
        }

        val features = doc.getElementsByTagName("uses-feature").let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }
        }
        assertEquals(5, features.size)
        val passthrough = features.single { it.getAttribute("android:name") == "com.oculus.feature.PASSTHROUGH" }
        assertEquals("false", passthrough.getAttribute("android:required"))
        assertEquals("", passthrough.getAttribute("android:version"))
        val openxr = features.single { it.getAttribute("android:name") == "android.software.xr.api.openxr" }
        assertEquals("false", openxr.getAttribute("android:required"))
        assertEquals("65537", openxr.getAttribute("android:version"))
        val controller = features.single { it.getAttribute("android:name") == "android.hardware.xr.input.controller" }
        assertEquals("false", controller.getAttribute("android:required"))
        val handTracking = features.single { it.getAttribute("android:name") == "android.hardware.xr.input.hand_tracking" }
        assertEquals("false", handTracking.getAttribute("android:required"))
        val eyeTracking = features.single { it.getAttribute("android:name") == "android.hardware.xr.input.eye_tracking" }
        assertEquals("false", eyeTracking.getAttribute("android:required"))

        val permissions = doc.getElementsByTagName("uses-permission").let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }
        }
        assertEquals(2, permissions.size)
        val permissionNames = permissions.map { it.getAttribute("android:name") }.toSet()
        assertEquals(setOf("android.permission.HAND_TRACKING", "android.permission.EYE_TRACKING_FINE"), permissionNames)

        val nativeLibrary = doc.getElementsByTagName("uses-native-library").item(0) as Element?
        assertNotNull(nativeLibrary)
        assertEquals("libopenxr.google.so", nativeLibrary.getAttribute("android:name"))
        assertEquals("false", nativeLibrary.getAttribute("android:required"))
        val property = doc.getElementsByTagName("property").item(0) as Element?
        assertNotNull(property)
        assertEquals("XR_ACTIVITY_START_MODE_FULL_SPACE_UNMANAGED", property.getAttribute("android:value"))
        val library = doc.getElementsByTagName("uses-library").item(0) as Element?
        assertNotNull(library)
        assertEquals("com.android.extensions.xr", library.getAttribute("android:name"))
        assertEquals("false", library.getAttribute("android:required"))

        // Re-running any upsert is idempotent: no new elements, no new duplicates.
        val childCount = app.childNodes.length
        upsertManifestFeature(doc, manifest, app, "com.oculus.feature.PASSTHROUGH", required = false, version = null)
        upsertManifestFeature(doc, manifest, app, "android.software.xr.api.openxr", required = false, version = "65537")
        upsertManifestFeature(doc, manifest, app, "android.hardware.xr.input.hand_tracking", required = false, version = null)
        upsertManifestPermission(doc, manifest, app, "android.permission.HAND_TRACKING")
        upsertApplicationChild(doc, app, "uses-native-library", "libopenxr.google.so") {
            it.setAttribute("android:required", "false")
        }
        assertEquals(childCount, app.childNodes.length)
        assertEquals(5, doc.getElementsByTagName("uses-feature").length)
        assertEquals(2, doc.getElementsByTagName("uses-permission").length)
    }
}
