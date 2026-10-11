package app.template.patches.fourxvr

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Mandatory portable checks: the tracked 4XVR payloads are the verified reference bytes of the
 * Galaxy XR build. These run on a fresh checkout and never need a decoded APK.
 */
class FourXvrPayloadResourceTest {

    private fun bytes(name: String): ByteArray = checkNotNull(
        javaClass.getResourceAsStream("/fourxvr/$name"),
    ) { "Missing bundled resource: fourxvr/$name" }.use { it.readBytes() }

    private fun sha256(name: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes(name))
            .joinToString("") { "%02x".format(it) }

    @Test
    fun `bundled 4XVR payloads match the verified reference hashes`() {
        val expected = mapOf(
            "hdr/libplacebo.so" to "9463639e779e3832d85c9d8ee5b98169407e79c8758d758e1cba6414a5a04048",
            "hdr/lib4xvr_hdr_advanced.so" to "11a57b232e64fc52cc27c9db65e611b85f5819d5a13a09cb9158a42fbea6b38b",
            "audio/lib4xvr_audio_bridge.so" to "7cd3fd2a3ed65949a8804585f57bc16deae5495a5619304b56e945383055c1ec",
            "render/lib4xvr_render_dispatch.so" to "cc78310d1cabfa00e29479a94e7ca43f3674e3d0771a77d6cecf23103c304abf",
            "subtitles/lib4xvr_subtitle_position.so" to "771071820abaff2ad2372f103f6d3b967ba7fac0686b380f84509017f3b99ac6",
            "compat/libXrApiLayer_4XVR_viewconfig_fix.so" to "74d68aaa7ec035c776e8890f98ac2c56a93a0e02cb46df7f42572ae76df55564",
            "compat/libopenxr_loader.so" to "4893f7e8f1df2cbc0716418fe33c0b52867fcc7f02488b877ed46f42dbf4625f",
            "compat/XR_APILAYER_4XVR_viewconfig_fix.json" to
                "782a8263905cbc4d0771f6d9d7683740a94487b0284ef2fba4f53a60c41a5571",
            "licenses/NOTICE.txt" to "77e3bba6e452633bba4b8a3f8ecaa4485a068d2491b5268399419c5be59e7978",
            "controllers/MetaQuestTouchPlus_Left_BaseColor_AO.png" to
                "a084603d283d7628b2b2455b0ad28240872b69b6f4357dd6ba67cc89da5e8988",
            "controllers/MetaQuestTouchPlus_right_BaseColor_AO.png" to
                "a084603d283d7628b2b2455b0ad28240872b69b6f4357dd6ba67cc89da5e8988",
            "controllers/quest3_ctrl_l" to "0fca93c2e3ad7cfbb58b82bd9ece756d2090ac6af48c5b0c6a5310651e4058d9",
            "controllers/quest3_ctrl_r" to "d60fed2a2d4ec82281ac9a7bae53eabea3c41291d56e3ce272e9aef4d174f6fa",
        )
        expected.forEach { (name, hash) ->
            assertEquals(hash, sha256(name), name)
        }
    }

    @Test
    fun `extension smali sources ship for every helper class of the recommended groups`() {
        // 32 files; spot-check the load-bearing classes across all packages.
        val required = listOf(
            "smali/dev/eni/hdrdirect/HdrBridge.smali",
            "smali/cn/vr4p/oculus4xvrplayerov/FrameDispatchCore.smali",
            "smali/cn/vr4p/oculus4xvrplayerov/RenderDispatch.smali",
            "smali/cn/vr4p/oculus4xvrplayerov/RenderDispatch\$Native.smali",
            "smali/cn/vr4p/patch/GxrAudioOutput.smali",
            "smali/cn/vr4p/patch/GxrAudioOutput\$Output.smali",
            "smali/cn/vr4p/patch/GxrToastContinuity.smali",
            "smali/cn/vr4p/patch/SubtitleFont.smali",
            "smali/cn/vr4p/patch/SubtitleFontFiles.smali",
            "smali/cn/vr4p/patch/SubtitleStyleListener.smali",
            "smali/cn/vr4p/patch/SubtitlePositionControls.smali",
            "smali/cn/vr4p/patch/SubtitlePositionStrings.smali",
            "smali/cn/vr4p/patch/PositionValue.smali",
        )
        required.forEach { path ->
            assertNotNull(javaClass.getResource("/fourxvr/$path"), path)
        }
        // The excluded premium/VIP shim must never be staged.
        listOf(
            "smali/cn/vr4p/patch/MetaPremium.smali",
            "smali/cn/vr4p/patch/MetaPremiumCheck.smali",
        ).forEach { path ->
            assertTrue(javaClass.getResource("/fourxvr/$path") == null, "excluded class staged: $path")
        }
    }
}
