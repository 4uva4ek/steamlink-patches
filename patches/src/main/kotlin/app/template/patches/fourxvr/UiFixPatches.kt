package app.template.patches.fourxvr

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.Constants.isFourXvr20022Build
import app.template.patches.shared.PatchCategories
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * G9 — 4XVR startup and UI continuity helper for exact 4XVR 2.0.22/20022.
 *
 * Pure extension-class hook (no payload): the reference build registers GxrToastContinuity (bundled
 * in fourxvr-extension.mpe) immediately after the Main4XActivity.onCreate invoke-super
 * NativeActivity->onCreate(Landroid/os/Bundle;)V. The helper's I610UEU2AZF3-gated memory-buildup fix
 * keeps the Galaxy XR toast/window pipeline warm and the startup toast continuous across activity
 * recreation, so this patch mirrors that exact call site and instruction.
 */

private const val MAIN_ACTIVITY = "Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;"
private const val GXR_TOAST_CONTINUITY = "Lcn/vr4p/patch/GxrToastContinuity;"

@Suppress("unused")
internal val fourXvrToastContinuityPatch = bytecodePatch(
    name = "Startup and UI continuity helper (4XVR)",
    description = "Registers the bundled GxrToastContinuity helper, whose I610UEU2AZF3-gated " +
        "memory-buildup fix and toast continuity keep the Galaxy XR UI pipeline warm across " +
        "activity recreation, right after Main4XActivity.onCreate for exact 4XVR 2.0.22 build " +
        "20022. Headless validation pending. " +
        FOUR_XVR_UPSTREAM_CREDIT,
    default = true,
) {
    category(PatchCategories.STARTUP_AND_PERMISSIONS)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())
    extendWith("extensions/fourxvr-extension.mpe")

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute

        // ------------------------------------------------------------------
        // Main4XActivity.onCreate(Landroid/os/Bundle;)V: register the extension helper exactly
        // where the reference build calls it — directly after the invoke-super call. Content
        // scan for the super onCreate; the anchor must occur exactly once.
        // ------------------------------------------------------------------
        val mainActivity = mutableClassDefBy(MAIN_ACTIVITY)
        val onCreate = mainActivity.methods.first { it.name == "onCreate" && it.parameterTypes.size == 1 }
        val superOnCreateIndexes = onCreate.implementation!!.instructions.withIndex()
            .filter { (_, instruction) ->
                (instruction.opcode == Opcode.INVOKE_SUPER || instruction.opcode == Opcode.INVOKE_SUPER_RANGE) &&
                    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "onCreate"
            }
            .map { it.index }
        check(superOnCreateIndexes.size == 1) {
            "Expected one invoke-super onCreate in Main4XActivity.onCreate; " +
                "found ${superOnCreateIndexes.size}"
        }
        onCreate.addInstruction(
            superOnCreateIndexes.single() + 1,
            invokeStatic35c(
                GXR_TOAST_CONTINUITY,
                "installForActivity",
                listOf("Landroid/app/Activity;"),
                "V",
                listOf(onCreate.pRegister(0)),
            ),
        )
    }
}
