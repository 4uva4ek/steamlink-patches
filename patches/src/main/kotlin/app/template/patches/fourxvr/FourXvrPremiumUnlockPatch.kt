package app.template.patches.fourxvr

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.Constants.isFourXvr20022Build
import app.template.patches.shared.PatchCategories
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

/**
 * G10 — Premium unlock for exact 4XVR 2.0.22/20022.
 *
 * Removes the premium requirement on Galaxy XR, where the Meta Store cannot be reached to verify
 * a purchase. At every start the patch drives the app's own VIP state through the stock
 * `Main4XActivity.ChangeCurVIPStateClear(true)` - the same state transition the store purchase
 * applies - which makes `IsMP()` true for the session and keeps the trial playback limiter in
 * `Main4XActivity$6` disabled. The dead Meta-store Buy entry points re-assert the unlocked state
 * instead of calling the native checkout, and the Buy buttons are hidden in favor of the Sync
 * slot. No purchase verification is performed.
 *
 * Hook sites (stock smali, exact 4XVR 2.0.22/20022):
 * - V4BuyVIPDlg.lambda$onCreate$0 / $1 and V4BuyVIPView$2 / $3 onClick: retarget the static
 *   Main4XActivity.LaunchCheckoutFlowVIP()Z invoke to PremiumUnlock.Launch()Z.
 * - V4BuyVIPDlg.onCreate tail: insert tidyBuyDialog(this) at the :cond_2 join. The join label must
 *   become the inserted call, so the existing branch onto the old target (return-void) is rebuilt
 *   against a fresh label created at the new call - an insert before a branch target alone is
 *   skipped on the incoming-branch path.
 * - V4BuyVIPView.onCreate tail: insert tidyBuyView(this) before return-void (branch-free tail).
 * - Main4XActivity.onCreate: insert unlock(this) right after the InitVIPRecordBuf(Z) call.
 */

private const val MAIN_ACTIVITY = "Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;"
private const val BUY_VIP_DLG = "Lcn/vr4p/oculus4xvrplayerov/Dialog/V4BuyVIPDlg;"
private const val BUY_VIP_VIEW = "Lcn/vr4p/oculus4xvrplayerov/MainUI/V4BuyVIPView;"
private const val BUY_VIP_VIEW_2 = "Lcn/vr4p/oculus4xvrplayerov/MainUI/V4BuyVIPView\$2;"
private const val BUY_VIP_VIEW_3 = "Lcn/vr4p/oculus4xvrplayerov/MainUI/V4BuyVIPView\$3;"
private const val PREMIUM_UNLOCK = "Lcn/vr4p/patch/PremiumUnlock;"
private const val STOCK_CHECKOUT_FLOW = "LaunchCheckoutFlowVIP"
private const val UNLOCK_LAUNCH = "Launch"
private const val V4_WINDOW_TYPE = "Lcn/vr4p/oculus4xvrplayerov/V4Window;"
private const val VIEW_TYPE = "Landroid/view/View;"
private const val FRAME_LAYOUT = "Landroid/widget/FrameLayout;"

private fun methodReferenceOf(instruction: Any?): MethodReference? =
    (instruction as? ReferenceInstruction)?.reference as? MethodReference

private fun MutableMethod.containsCall(definingClass: String, name: String): Boolean =
    implementation!!.instructions.any { instruction ->
        methodReferenceOf(instruction)?.let { reference ->
            reference.definingClass == definingClass && reference.name == name
        } == true
    }

/**
 * Retargets every stock `Main4XActivity.LaunchCheckoutFlowVIP()Z` invoke in [methods] to the
 * PremiumUnlock `Launch()Z` re-assert. [expected] is the exact number of such invokes in the
 * stock class; already-retargeted sites are accepted so re-runs converge, and any other defining
 * class or method name fails closed.
 */
private fun retargetCheckoutFlowToPremiumUnlock(
    methods: Collection<MutableMethod>,
    classType: String,
    expected: Int,
) {
    val sites = methods.flatMap { method ->
        method.implementation!!.instructions.withIndex()
            .filter { (_, instruction) ->
                val reference = methodReferenceOf(instruction)
                reference != null &&
                    reference.parameterTypes.isEmpty() &&
                    reference.returnType == "Z" &&
                    ((reference.definingClass == MAIN_ACTIVITY && reference.name == STOCK_CHECKOUT_FLOW) ||
                        (reference.definingClass == PREMIUM_UNLOCK && reference.name == UNLOCK_LAUNCH))
            }
            .map { (index, instruction) -> Triple(method, index, methodReferenceOf(instruction)!!) }
    }
    require(sites.size == expected && sites.all {
        it.third.parameterTypes.isEmpty() && it.third.returnType == "Z" &&
            ((it.third.definingClass == MAIN_ACTIVITY && it.third.name == STOCK_CHECKOUT_FLOW) ||
                (it.third.definingClass == PREMIUM_UNLOCK && it.third.name == UNLOCK_LAUNCH))
    }) {
        "Premium unlock: expected $expected $STOCK_CHECKOUT_FLOW invokes on $classType, " +
            "found ${sites.size} (${sites.map { it.third.definingClass }})"
    }
    sites.filter { it.third.definingClass == MAIN_ACTIVITY }.forEach { (method, index, _) ->
        method.implementation!!.replaceInstruction(
            index,
            retargetInvoke(
                method.implementation!!.instructions[index],
                ImmutableMethodReference(PREMIUM_UNLOCK, UNLOCK_LAUNCH, emptyList(), "Z"),
            ),
        )
    }
}

@Suppress("unused")
internal val fourXvrPremiumUnlockPatch = bytecodePatch(
    name = "Premium unlock (4XVR, experimental)",
    description = "Removes the 4XVR premium gate on Galaxy XR, where the Meta Store cannot be " +
        "reached to verify a purchase: every start forces the app's own VIP state on through " +
        "ChangeCurVIPStateClear(true), the same state transition a store purchase applies, so " +
        "IsMP() is true and the trial playback limiter stays off. The dead Meta-store Buy entry " +
        "points re-assert the unlocked state instead of calling the native checkout; the Buy " +
        "buttons are hidden in favor of the Sync slot. Exact 4XVR 2.0.22 build 20022. " +
        "Headset validation pending. " +
        FOUR_XVR_UPSTREAM_CREDIT,
    default = false,
) {
    category(PatchCategories.PREMIUM_AND_PURCHASES)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())
    extendWith("extensions/fourxvr-extension.mpe")

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute

        // 1/2 — the buy-dialog button listeners re-assert the unlocked state.
        retargetCheckoutFlowToPremiumUnlock(mutableClassDefBy(BUY_VIP_DLG).methods, BUY_VIP_DLG, expected = 2)

        // 3/4 — the buy-card button listeners re-assert the unlocked state.
        retargetCheckoutFlowToPremiumUnlock(mutableClassDefBy(BUY_VIP_VIEW_2).methods, BUY_VIP_VIEW_2, expected = 1)
        retargetCheckoutFlowToPremiumUnlock(mutableClassDefBy(BUY_VIP_VIEW_3).methods, BUY_VIP_VIEW_3, expected = 1)

        // 5 — dialog re-layout at the tail of V4BuyVIPDlg.onCreate. Both paths converge on the
        // :cond_2 join, so the join label must become the inserted call: insert the call before
        // the final return-void, create a fresh label at the call, and rebuild the single branch
        // that targeted the old join. A plain insert would be skipped on the branch path.
        run {
            val dialog = mutableClassDefBy(BUY_VIP_DLG)
            val onCreate = dialog.methods.first { it.name == "onCreate" && it.parameterTypes.isEmpty() }
            if (!onCreate.containsCall(PREMIUM_UNLOCK, "tidyBuyDialog")) {
                val instructions = onCreate.implementation!!.instructions
                val last = instructions.size - 1
                require(last >= 4 && instructions[last].opcode == Opcode.RETURN_VOID) {
                    "Premium unlock: V4BuyVIPDlg.onCreate does not end in return-void"
                }
                val beforeReturn = methodReferenceOf(instructions[last - 1])
                require(
                    beforeReturn?.definingClass == FRAME_LAYOUT && beforeReturn.name == "setOnClickListener",
                ) {
                    "Premium unlock: unexpected V4BuyVIPDlg.onCreate tail $beforeReturn"
                }
                // Opcode.name is dexlib2's lowercase mnemonic field ("if-eqz", "goto"), not the enum
                // constant name; matching on "IF_"/"GOTO" never fired on a live dex (caught by the
                // 2026-10-12 apply-audit).
                val branchIndex = instructions.indexOfLast { instruction ->
                    instruction.opcode.name.startsWith("if-") || instruction.opcode.name.startsWith("goto")
                }
                require(branchIndex >= 0 && branchIndex + 4 == last) {
                    "Premium unlock: no branch directly above the V4BuyVIPDlg.onCreate join " +
                        "(found branch index $branchIndex, method size ${instructions.size})"
                }
                require(instructions[branchIndex + 1].opcode == Opcode.NEW_INSTANCE) {
                    "Premium unlock: unexpected instruction above the V4BuyVIPDlg.onCreate join: " +
                        "${instructions[branchIndex + 1].opcode}"
                }
                val branch = instructions[branchIndex] as? BuilderInstruction21t
                require(branch != null && branch.opcode == Opcode.IF_EQZ) {
                    "Premium unlock: expected if-eqz at the V4BuyVIPDlg.onCreate join; found " +
                        "${instructions[branchIndex].opcode}"
                }
                onCreate.addInstruction(
                    last,
                    invokeStatic35c(
                        PREMIUM_UNLOCK,
                        "tidyBuyDialog",
                        listOf(V4_WINDOW_TYPE),
                        "V",
                        listOf(onCreate.pRegister(0)),
                    ),
                )
                val join = onCreate.implementation!!.newLabelForIndex(last)
                onCreate.implementation!!.replaceInstruction(
                    branchIndex,
                    BuilderInstruction21t(Opcode.IF_EQZ, branch.registerA, join),
                )
            }
        }

        // 6 — buy-card re-layout at the tail of V4BuyVIPView.onCreate (branch-free body).
        run {
            val buyView = mutableClassDefBy(BUY_VIP_VIEW)
            val onCreate = buyView.methods.first { it.name == "onCreate" && it.parameterTypes.size == 2 }
            if (!onCreate.containsCall(PREMIUM_UNLOCK, "tidyBuyView")) {
                val instructions = onCreate.implementation!!.instructions
                val last = instructions.size - 1
                require(instructions[last].opcode == Opcode.RETURN_VOID) {
                    "Premium unlock: V4BuyVIPView.onCreate does not end in return-void"
                }
                val beforeReturn = methodReferenceOf(instructions[last - 1])
                require(beforeReturn?.definingClass == BUY_VIP_VIEW && beforeReturn.name == "setHintImageView") {
                    "Premium unlock: unexpected V4BuyVIPView.onCreate tail $beforeReturn"
                }
                onCreate.addInstruction(
                    last,
                    invokeStatic35c(
                        PREMIUM_UNLOCK,
                        "tidyBuyView",
                        listOf(VIEW_TYPE),
                        "V",
                        listOf(onCreate.pRegister(0)),
                    ),
                )
            }
        }

        // 7 — force the unlocked state at app start, right after InitVIPRecordBuf(Z).
        run {
            val activity = mutableClassDefBy(MAIN_ACTIVITY)
            val onCreate = activity.methods.first { it.name == "onCreate" && it.parameterTypes.size == 1 }
            if (!onCreate.containsCall(PREMIUM_UNLOCK, "unlock")) {
                val instructions = onCreate.implementation!!.instructions
                val sites = instructions.withIndex()
                    .filter { (_, instruction) ->
                        methodReferenceOf(instruction)?.let { reference ->
                            reference.definingClass == MAIN_ACTIVITY && reference.name == "InitVIPRecordBuf"
                        } == true
                    }
                    .map { it.index }
                require(sites.size == 1) {
                    "Premium unlock: expected one InitVIPRecordBuf call in Main4XActivity.onCreate; " +
                        "found ${sites.size}"
                }
                val index = sites.single()
                val next = methodReferenceOf(instructions.getOrNull(index + 1))
                require(next?.definingClass == MAIN_ACTIVITY && next.name == "InitMainLinkSvr") {
                    "Premium unlock: unexpected instruction after InitVIPRecordBuf: $next"
                }
                onCreate.addInstruction(
                    index + 1,
                    invokeStatic35c(
                        PREMIUM_UNLOCK,
                        "unlock",
                        listOf(MAIN_ACTIVITY),
                        "V",
                        listOf(onCreate.pRegister(0)),
                    ),
                )
            }
        }
    }
}
