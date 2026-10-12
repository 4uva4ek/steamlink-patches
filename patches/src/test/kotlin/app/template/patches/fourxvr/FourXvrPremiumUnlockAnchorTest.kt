package app.template.patches.fourxvr

import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Anchors of the premium unlock patch (`FourXvrPremiumUnlockPatch.kt`), checked against the
 * decoded stock tree when it is present and reported as a skipped audit otherwise (the decoded
 * trees are untracked local evidence, not fresh-checkout inputs).
 *
 * The patch retargets the four stock `Main4XActivity.LaunchCheckoutFlowVIP()Z` invokes to the
 * bundled PremiumUnlock re-assert, inserts `tidyBuyDialog(this)` at the `V4BuyVIPDlg.onCreate`
 * join (the `:cond_2` label must become the inserted call), `tidyBuyView(this)` at the tail of
 * `V4BuyVIPView.onCreate` and `unlock(this)` right after the `Main4XActivity.onCreate`
 * `InitVIPRecordBuf(Z)` call, and binds only members declared in the app classes below.
 */
class FourXvrPremiumUnlockAnchorTest {

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

    /** Trimmed body lines that carry code: blanks, directive and comment lines removed. */
    private fun codeLines(body: List<String>): List<String> =
        body.map { it.trim() }.filter { line ->
            line.isNotEmpty() && !line.startsWith(".") && !line.startsWith("#")
        }

    @Test
    fun `premium unlock anchors hold on the decoded stock tree`() {
        val dlg = fixture("4xvr-orig/smali/cn/vr4p/oculus4xvrplayerov/Dialog/V4BuyVIPDlg.smali")
        val view = fixture("4xvr-orig/smali/cn/vr4p/oculus4xvrplayerov/MainUI/V4BuyVIPView.smali")
        val view2 = fixture("4xvr-orig/smali/cn/vr4p/oculus4xvrplayerov/MainUI/V4BuyVIPView\$2.smali")
        val view3 = fixture("4xvr-orig/smali/cn/vr4p/oculus4xvrplayerov/MainUI/V4BuyVIPView\$3.smali")
        val activity = fixture("4xvr-orig/smali/cn/vr4p/oculus4xvrplayerov/Main4XActivity.smali")
        val window = fixture("4xvr-orig/smali/cn/vr4p/oculus4xvrplayerov/V4Window.smali")
        assumeTrue(
            "BLOCKED: decoded 4XVR fixtures absent; need 4xvr-orig/ next to the checkout",
            listOf(dlg, view, view2, view3, activity, window).all { it != null },
        )
        dlg!!; view!!; view2!!; view3!!; activity!!; window!!

        val stockCheckout =
            "invoke-static {}, Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;->LaunchCheckoutFlowVIP()Z"
        val setOnClickListener =
            "invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->setOnClickListener(Landroid/view/View\$OnClickListener;)V"

        // V4BuyVIPDlg — the two checkout button listeners each carry exactly one stock invoke,
        // preceded by SetHide(); the patch retargets each of them to PremiumUnlock.Launch().
        val dlgLines = linesOf(dlg)
        assertEquals(2, dlgLines.count { it.trim() == stockCheckout }, "V4BuyVIPDlg stock checkout invokes")
        listOf(
            ".method public synthetic lambda\$onCreate\$0\$V4BuyVIPDlg(Landroid/view/View;)V",
            ".method public synthetic lambda\$onCreate\$1\$V4BuyVIPDlg(Landroid/view/View;)V",
        ).forEach { signature ->
            assertEquals(1, methodCount(dlgLines, signature), signature)
            val body = methodBody(dlgLines, signature)!!
            assertEquals(1, body.count { it.trim() == stockCheckout }, "$signature checkout invoke")
            assertTrue(
                body.any { it.trim().startsWith("invoke-virtual") && it.trim().endsWith("->SetHide()V") },
                "$signature no longer calls SetHide() before the checkout flow",
            )
        }

        // V4BuyVIPDlg.onCreate — the :cond_2 join right before return-void, reached by one branch;
        // the patch promotes the inserted tidyBuyDialog call to be the join's target.
        val dlgOnCreate = codeLines(methodBody(dlgLines, ".method protected onCreate()V")!!)
        assertEquals("return-void", dlgOnCreate.last(), "V4BuyVIPDlg.onCreate tail")
        assertEquals(":cond_2", dlgOnCreate[dlgOnCreate.size - 2], "V4BuyVIPDlg.onCreate join label")
        assertEquals(
            setOnClickListener,
            dlgOnCreate[dlgOnCreate.size - 3],
            "V4BuyVIPDlg.onCreate close-button wiring",
        )
        assertEquals(
            1,
            dlgOnCreate.count { it.startsWith("if-eqz") && it.contains(":cond_2") },
            "the single branch onto the :cond_2 join moved",
        )
        assertEquals(1, dlgOnCreate.count { it == ":cond_2" }, "the :cond_2 join label moved")

        // V4BuyVIPView$2 / $3 — one stock invoke each; the buy listener logs the result, the sync
        // listener discards it.
        val view2Lines = linesOf(view2)
        assertEquals(1, view2Lines.count { it.trim() == stockCheckout }, "V4BuyVIPView\$2 checkout invoke")
        val view2OnClick = codeLines(methodBody(view2Lines, ".method public onClick(Landroid/view/View;)V")!!)
        val view2Invoke = view2OnClick.indexOfFirst { it.endsWith("->LaunchCheckoutFlowVIP()Z") }
        assertTrue(view2Invoke >= 0, "V4BuyVIPView\$2 onClick lost the checkout invoke")
        assertEquals("move-result p1", view2OnClick[view2Invoke + 1], "V4BuyVIPView\$2 result handling")
        assertTrue(
            view2Lines.any { it.contains("setBuyButton():Main4XActivity.LaunchCheckoutFlowVIP()=") },
            "the buy-listener log string moved",
        )

        val view3Lines = linesOf(view3)
        assertEquals(1, view3Lines.count { it.trim() == stockCheckout }, "V4BuyVIPView\$3 checkout invoke")
        val view3OnClick = codeLines(methodBody(view3Lines, ".method public onClick(Landroid/view/View;)V")!!)
        val view3Invoke = view3OnClick.indexOfFirst { it.endsWith("->LaunchCheckoutFlowVIP()Z") }
        assertTrue(view3Invoke >= 0, "V4BuyVIPView\$3 onClick lost the checkout invoke")
        assertEquals("return-void", view3OnClick[view3Invoke + 1], "V4BuyVIPView\$3 discards the result")

        // V4BuyVIPView.onCreate — label-free and branch-free tail; the patch inserts before the
        // final return-void.
        val viewLines = linesOf(view)
        val viewOnCreate = codeLines(
            methodBody(
                viewLines,
                ".method public onCreate(Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;Lcn/vr4p/oculus4xvrplayerov/V4MainUI;)V",
            )!!,
        )
        assertEquals("return-void", viewOnCreate.last(), "V4BuyVIPView.onCreate tail")
        assertEquals(
            "invoke-direct {p0}, Lcn/vr4p/oculus4xvrplayerov/MainUI/V4BuyVIPView;->setHintImageView()V",
            viewOnCreate[viewOnCreate.size - 2],
            "V4BuyVIPView.onCreate wiring order",
        )
        assertEquals(
            0,
            viewOnCreate.count { it.startsWith(":") || it.startsWith("if-") || it.startsWith("goto") },
            "V4BuyVIPView.onCreate gained labels or branches; the tail insert would be skipped",
        )

        // Main4XActivity — the unlock insert is method-scoped to onCreate (three call sites in the
        // file) and sits between InitVIPRecordBuf(Z) and InitMainLinkSvr().
        val activityLines = linesOf(activity)
        assertEquals(
            3,
            activityLines.count {
                it.trim().startsWith("invoke") && it.contains("Main4XActivity;->InitVIPRecordBuf(Z)V")
            },
            "the InitVIPRecordBuf call-site count changed; re-derive the onCreate selector",
        )
        val activityOnCreate = codeLines(methodBody(activityLines, ".method protected onCreate(Landroid/os/Bundle;)V")!!)
        val initIndexes = activityOnCreate.withIndex()
            .filter { (_, line) -> line.contains("Main4XActivity;->InitVIPRecordBuf(Z)V") }
        assertEquals(1, initIndexes.size, "expected one InitVIPRecordBuf call in Main4XActivity.onCreate")
        val initIndex = initIndexes.single().index
        assertEquals(
            "invoke-virtual {p0}, Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;->InitMainLinkSvr()V",
            activityOnCreate[initIndex + 1],
            "the unlock insert point between InitVIPRecordBuf and InitMainLinkSvr moved",
        )

        // Members the helper binds, by exact declaration: the activity holder and view-group
        // getter on V4Window, and the stock VIP state transition on the activity.
        val windowLines = linesOf(window)
        listOf(
            ".field public static s_MainActivity:Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;",
            ".method public GetViewGroup()Landroid/view/ViewGroup;",
        ).forEach { declaration ->
            assertTrue(windowLines.any { it.trim() == declaration }, "V4Window lost: $declaration")
        }
        assertTrue(
            activityLines.any { it.trim() == ".method public ChangeCurVIPStateClear(Z)V" },
            "Main4XActivity lost: ChangeCurVIPStateClear(Z)V",
        )
    }
}
