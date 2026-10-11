package app.template.patches.fourxvr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Mandatory portable checks: every 4XVR patch stays default-off, and the Galaxy XR set is an
 * opt-in selection only. The app's patches remain outside the default-on recommended sets until
 * a pristine stock 2.0.22/20022 APK and the modified libplacebo source availability are resolved.
 */
class FourXvrBundleTest {

    @Test
    fun `4XVR patches stay default-off and the Galaxy XR set is opt-in`() {
        val members = listOf(
            fourXvrLaunchCompatibilityPatch,
            fourXvrControllerModelsPatch,
            fourXvrRefreshProfilePatch,
            fourXvrHdrPipelinePatch,
            fourXvrAudioBridgePatch,
            fourXvrRenderDispatchPatch,
            fourXvrSubtitleSuitePatch,
            fourXvrToastContinuityPatch,
        )
        assertEquals(8, members.size)
        members.forEach { member -> assertFalse(member.default, member.name) }
        assertFalse(fourXvrGalaxyXrSetPatch.default, fourXvrGalaxyXrSetPatch.name)
    }
}
