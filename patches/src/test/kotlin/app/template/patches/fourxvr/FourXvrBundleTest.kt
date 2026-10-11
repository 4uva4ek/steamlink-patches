package app.template.patches.fourxvr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mandatory portable checks: the 4XVR Galaxy XR set is recommended by default (owner decision
 * 2026-10-11) and every member patch plus payload is default-on; the experimental Meta platform
 * decoupling stays opt-in until it is verified on device.
 */
class FourXvrBundleTest {

    @Test
    fun `4XVR patches are recommended by default and the experimental decoupling stays opt-in`() {
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
        members.forEach { member -> assertTrue(member.default, member.name) }
        listOf(
            fourXvrHdrPayloadPatch,
            fourXvrAudioBridgePayloadPatch,
            fourXvrRenderDispatchPayloadPatch,
            fourXvrSubtitleSuitePayloadPatch,
        ).forEach { payload -> assertTrue(payload.default, payload.name) }
        assertTrue(fourXvrGalaxyXrSetPatch.default, fourXvrGalaxyXrSetPatch.name)
        assertFalse(fourXvrPlatformDecouplingPatch.default, fourXvrPlatformDecouplingPatch.name)
    }
}
