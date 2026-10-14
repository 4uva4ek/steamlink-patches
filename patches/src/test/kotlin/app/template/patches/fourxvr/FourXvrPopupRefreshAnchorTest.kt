package app.template.patches.fourxvr

import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Anchors of the popup-refresh patch, checked against the decoded stock tree when it is present
 * and reported as a skipped audit otherwise (the decoded trees are untracked local evidence, not
 * fresh-checkout inputs).
 *
 * The patch inserts `PopupRefresh.onHostResume` at instruction 0 of `Main4XActivity.onResume()`
 * and `onActivityResult(...)`, and the helper calls `V4Window.IsVisible()` / `SetHide()` plus the
 * public `GetV4VersionUpdateDlg()` getter while reading the package-private `m_PermissionsUI`
 * field reflectively. Every one of those facts has to hold for the injected call to verify and to
 * reach the windows, so pin them here.
 */
class FourXvrPopupRefreshAnchorTest {

    private fun fixture(relative: String): File? =
        listOf(File(relative), File("../$relative")).firstOrNull(File::isFile)

    private fun linesOf(file: File) = file.readLines()

    private fun methodBody(lines: List<String>, signature: String): List<String>? {
        val start = lines.indexOfFirst { it.trim().startsWith(signature) }
        if (start < 0) return null
        var end = start + 1
        while (end < lines.size && lines[end].trim() != ".end method") end++
        return lines.subList(start + 1, end)
    }

    private fun methodCount(lines: List<String>, signature: String) =
        lines.count { it.trim().startsWith(signature) }

    /** First executable instruction of a method body, i.e. past labels, annotations and directives. */
    private fun firstInstruction(body: List<String>): Pair<Int, String> {
        val directives = listOf(".locals", ".line", ".param", ".prologue", "#")
        var inAnnotation = false
        for ((index, line) in body.withIndex()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith(".annotation") -> inAnnotation = true
                trimmed.startsWith(".end annotation") -> inAnnotation = false
                inAnnotation || trimmed.isEmpty() -> Unit
                directives.any { trimmed.startsWith(it) } -> Unit
                else -> return index to trimmed
            }
        }
        error("no instruction in method body")
    }

    private fun isLabel(line: String) = line.trim().startsWith(":")

    @Test
    fun `popup refresh anchors hold on the decoded stock tree`() {
        val activity = fixture("4xvr-orig/smali/cn/vr4p/oculus4xvrplayerov/Main4XActivity.smali")
        val window = fixture("4xvr-orig/smali/cn/vr4p/oculus4xvrplayerov/V4Window.smali")
        assumeTrue(
            "BLOCKED: decoded 4XVR fixtures absent; need 4xvr-orig/ next to the checkout",
            activity != null && window != null,
        )
        activity!!; window!!

        val activityLines = linesOf(activity)
        val windowLines = linesOf(window)

        // Both injected call sites are unique and the patch's selectors are unambiguous.
        assertEquals(1, methodCount(activityLines, ".method protected onResume()V"))
        assertEquals(
            1,
            methodCount(activityLines, ".method protected onActivityResult(IILandroid/content/Intent;)V"),
        )

        listOf(".method protected onResume()V", ".method protected onActivityResult(IILandroid/content/Intent;)V")
            .forEach { signature ->
                val body = methodBody(activityLines, signature)!!
                val (index, instruction) = firstInstruction(body)
                assertTrue(
                    instruction.startsWith("invoke-super"),
                    "$signature starts with $instruction, not the super call",
                )
                assertTrue(
                    index == 0 || !isLabel(body[index - 1]),
                    "$signature head is a branch target; an index-0 insert would be skipped at runtime",
                )
                assertEquals(
                    0,
                    body.count { it.trim().startsWith(".catch") },
                    "$signature has a try range; keep the injected call out of it",
                )
            }

        // The helper's call surface: public window API and the public version-dialog getter.
        assertTrue(
            windowLines.any { it.trim() == ".method public IsVisible()Z" },
            "V4Window.IsVisible()Z is missing or not public",
        )
        assertTrue(
            windowLines.any { it.trim() == ".method public SetHide()V" },
            "V4Window.SetHide()V is missing or not public",
        )
        val dialogGetter = methodBody(activityLines, ".method public GetV4VersionUpdateDlg()")
        assertTrue(dialogGetter != null, "Main4XActivity.GetV4VersionUpdateDlg() is missing or not public")

        // The permission window has no getter, so the helper reads its field reflectively; the
        // field name and the missing accessor are the contract the helper depends on.
        assertTrue(
            activityLines.any { it.trim().startsWith(".field") && it.contains("m_PermissionsUI:") },
            "Main4XActivity.m_PermissionsUI field is gone; PopupRefresh cannot reach the window",
        )
        assertTrue(
            methodCount(activityLines, ".method public GetV4PermissionsUI()") == 0,
            "a public permissions getter now exists; read the field through it instead of reflection",
        )
    }
}
