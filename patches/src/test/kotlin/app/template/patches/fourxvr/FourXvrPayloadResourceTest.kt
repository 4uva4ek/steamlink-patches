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
            "icon/ic_launcher.xml" to "b84b96df85272c211ed01b12c384fa856677b2c4120743066e2b31271002da71",
            "icon/ic_launcher_round.xml" to "b84b96df85272c211ed01b12c384fa856677b2c4120743066e2b31271002da71",
            "icon/ic_launcher_mdpi.png" to "90660bebcaae3e6217ad72b4f9ee904065e07137dec3c853892f4e19cd771651",
            "icon/ic_launcher_hdpi.png" to "70cc8acfc358c1383ac5f4a53e87950858a8dfb3ff23eba2f502a4950675f814",
            "icon/ic_launcher_xhdpi.png" to "75b94d02b95c18a959c1b5260a84e390ceed1f5cbcba348718ee7bd43d7c7b4f",
            "icon/ic_launcher_xxhdpi.png" to "68467edc995c4164f4695bef36b07188a40e0129fc3ab7478d973e2cb656d47e",
            "icon/ic_launcher_xxxhdpi.png" to "0becf74d16971a9e2354068da5af4111f3597824d5fe5f7b231ef8bc5ec4ca53",
            "icon/ic_launcher_round_mdpi.png" to "b1ec20df325584df74e32c3ab99e8d0cd377b45b0b1d44d8d890bfce3ff1e851",
            "icon/ic_launcher_round_hdpi.png" to "bcefe66f43b764a2280b61afa153048c2307a5e59defb85e4c03c5fcd7cb978b",
            "icon/ic_launcher_round_xhdpi.png" to "b5b9c88955cb816ce13e54ece04f326b7e3f12814b8e1a8f1d991ae32a6e5252",
            "icon/ic_launcher_round_xxhdpi.png" to "6ed123a11d880c90418f759757b20b831eeaf6481f8e63afe896a1e6b30d1973",
            "icon/ic_launcher_round_xxxhdpi.png" to "3c5a1055a4bf455da6fc4e87e1b46fc2378ddcabd50c7abdc91034a372e5257d",
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
        // 20 files; spot-check the load-bearing classes across all packages.
        val required = listOf(
            "smali/dev/eni/hdrdirect/HdrBridge.smali",
            "smali/cn/vr4p/oculus4xvrplayerov/FrameDispatchCore.smali",
            "smali/cn/vr4p/oculus4xvrplayerov/RenderDispatch.smali",
            "smali/cn/vr4p/oculus4xvrplayerov/RenderDispatch\$Native.smali",
            "smali/cn/vr4p/patch/GxrAudioOutput.smali",
            "smali/cn/vr4p/patch/GxrAudioOutput\$Output.smali",
            "smali/cn/vr4p/patch/GxrToastContinuity.smali",
            "smali/cn/vr4p/patch/PopupRefresh.smali",
            "smali/cn/vr4p/patch/SubtitleFont.smali",
            "smali/cn/vr4p/patch/SubtitleFontFiles.smali",
            "smali/cn/vr4p/patch/SubtitleStyleListener.smali",
            "smali/cn/vr4p/patch/SubtitlePositionControls.smali",
            "smali/cn/vr4p/patch/SubtitlePositionStrings.smali",
            "smali/cn/vr4p/patch/PositionValue.smali",
            // The premium unlock helper (owner decision 2026-10-12; replaces the parked
            // MetaPremium verifier set).
            "smali/cn/vr4p/patch/PremiumUnlock.smali",
        )
        required.forEach { path ->
            assertNotNull(javaClass.getResource("/fourxvr/$path"), path)
        }
        // The parked MetaPremium verifier set must never be staged again.
        listOf(
            "MetaPremium",
            "MetaPremium\$1",
            "MetaPremium\$2",
            "MetaPremium\$3",
            "MetaPremium\$3\$1",
            "MetaPremium\$4",
            "MetaPremium\$Strings",
            "MetaPremiumCheck",
            "MetaPremiumCheck\$Response",
            "MetaPremiumCheck\$Result",
        ).forEach { stem ->
            val path = "smali/cn/vr4p/patch/$stem.smali"
            assertTrue(javaClass.getResource("/fourxvr/$path") == null, "excluded class staged: $path")
        }
    }

    @Test
    fun `premium unlock payload declares the methods the patch binds`() {
        val source = checkNotNull(
            javaClass.getResourceAsStream("/fourxvr/smali/cn/vr4p/patch/PremiumUnlock.smali"),
        ).use { it.reader().readText() }
        listOf(
            ".method public static Launch()Z",
            ".method public static unlock(Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;)V",
            ".method public static tidyBuyDialog(Lcn/vr4p/oculus4xvrplayerov/V4Window;)V",
            ".method public static tidyBuyView(Landroid/view/View;)V",
        ).forEach { declaration ->
            assertTrue(source.contains(declaration), "PremiumUnlock.smali lost: $declaration")
        }
    }
}
