package app.template.patches.fourxvr

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Portable regression check for the parameter-word math behind pRegister: wide parameters (J/D)
 * occupy 2 register words. Counting parameter types instead of words mis-anchored the
 * Main4XActivity.RenderFrame(ZJJ)V drain site (p1 sits 5 words below the register count, not 3)
 * and failed the first on-device run of the render-dispatch patch on 2026-10-11.
 */
class FourXvrDexKitTest {

    @Test
    fun `parameter words count wide types twice`() {
        assertEquals(0, parameterWordCount(emptyList()))
        assertEquals(1, parameterWordCount(listOf("Landroid/os/Bundle;")))
        assertEquals(2, parameterWordCount(listOf("I", "Z")))
        assertEquals(5, parameterWordCount(listOf("Z", "J", "J")))
        assertEquals(8, parameterWordCount(listOf("J", "F", "F", "F", "F", "F", "F")))
    }
}
