package app.template.patches.fourxvr

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.Constants.isFourXvr20022Build
import app.template.patches.shared.PatchCategories
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10t
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

/**
 * G5 — frame-synced render dispatch for exact 4XVR 2.0.22/20022.
 *
 * Payload patch installs the 4XVR render dispatch native (scene queue, adaptive theater
 * background updates). The hook patch mirrors the reference build: the per-frame drain before
 * Main4XActivity.RenderFrame, RenderDispatch.requestScene replacing the retired render-runnable
 * handoff in V4Theatre.SetCurSubTheatreID, and the V4Window teardown routed through the dispatch
 * (ClearStatic reset, SetHide removeWindow, volatile visibility flags).
 */

private const val RENDER_DISPATCH = "Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;"
private const val MAIN_ACTIVITY = "Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;"
private const val V4_RENDER = "Lcn/vr4p/oculus4xvrplayerov/V4Render;"
private const val V4_THEATRE = "Lcn/vr4p/oculus4xvrplayerov/V4Theatre;"
private const val V4_WINDOW = "Lcn/vr4p/oculus4xvrplayerov/V4Window;"

@Suppress("unused")
internal val fourXvrRenderDispatchPayloadPatch = rawResourcePatch(
    name = "Render dispatch payload (4XVR)",
    description = "Installs the 4XVR render dispatch native (frame-synced scene queue and " +
        "adaptive theater background updates) for exact 4XVR 2.0.22 build 20022.",
    default = false,
) {
    category(PatchCategories.ADVANCED_XR_COMPATIBILITY)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute
        get("lib/arm64-v8a/lib4xvr_render_dispatch.so")
            .writeBytes(loadFourXvrResource("render/lib4xvr_render_dispatch.so"))
    }
}

@Suppress("unused")
internal val fourXvrRenderDispatchPatch = bytecodePatch(
    name = "Frame-synced render dispatch (4XVR)",
    description = "Drains the render dispatch queue before every frame, hands scene switches to " +
        "RenderDispatch.requestScene instead of the retired render runnable, and routes window " +
        "teardown through the dispatch for exact 4XVR 2.0.22 build 20022. Headset validation " +
        "pending.",
    default = false,
) {
    category(PatchCategories.ADVANCED_XR_COMPATIBILITY)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())
    dependsOn(fourXvrRenderDispatchPayloadPatch)
    extendWith("extensions/fourxvr-extension.mpe")

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute

        // ------------------------------------------------------------------
        // Main4XActivity.RenderFrame(ZJJ)V: drain the dispatch queue before the per-frame render.
        // The reference inserts the drain between :cond_0 and the p1 branch; anchor on the
        // V4Render->RenderFrame call and walk back to that branch.
        // ------------------------------------------------------------------
        val mainActivity = mutableClassDefBy(MAIN_ACTIVITY)
        val renderFrame = mainActivity.methods.first { it.name == "RenderFrame" && it.parameterTypes.size == 3 }
        val drainCandidates = renderFrame.invocationIndexes(V4_RENDER, "RenderFrame").filter { index ->
            val branch = renderFrame.implementation!!.instructions.getOrNull(index - 2)
            branch?.opcode == Opcode.IF_EQZ &&
                (branch as? OneRegisterInstruction)?.registerA == renderFrame.pRegister(1)
        }
        check(drainCandidates.size == 1) {
            "Expected one V4Render->RenderFrame call behind the p1 branch in Main4XActivity.RenderFrame; " +
                "found ${drainCandidates.size}"
        }
        renderFrame.addInstruction(
            drainCandidates.single() - 2,
            invokeStaticNoArgs(RENDER_DISPATCH, "drain", "V"),
        )

        // ------------------------------------------------------------------
        // V4Theatre.SetCurSubTheatreID(IZ)V: replace both retired handoff paths with
        // RenderDispatch.requestScene(this, m_iCurSubTheatreID).
        // ------------------------------------------------------------------
        val theatre = mutableClassDefBy(V4_THEATRE)
        val setCurTheatre = theatre.methods.first {
            it.name == "SetCurSubTheatreID" && it.parameterTypes.size == 2
        }
        val requestSceneCall = invokeStatic35c(
            RENDER_DISPATCH,
            "requestScene",
            listOf(V4_THEATRE, "I"),
            "Z",
            listOf(setCurTheatre.pRegister(0), setCurTheatre.pRegister(2)),
        )

        // Region (i): the retired clear-the-runnable monitor block. The try/catch structure cannot
        // be removed through the public dexlib2 API, so the reference's "requestScene + goto"
        // replacement is applied in place: the store becomes the requestScene call, the monitor
        // entry becomes a jump to the post-switch point, and the now-dead monitor/put bytes are
        // neutralized with nops (the catchall handler can never run because the try body no
        // longer throws).
        val lambdaStoreIndex = setCurTheatre.implementation!!.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.SGET_OBJECT &&
                ((instruction as? ReferenceInstruction)?.reference as? FieldReference)
                    ?.let { it.name == "INSTANCE" && it.definingClass.contains("dCnKuXhCjQRbtM-u8RUeYdyAXn4") } == true
        }
        check(lambdaStoreIndex >= 0) {
            "Missing retired render-runnable store in V4Theatre.SetCurSubTheatreID"
        }
        val blockShape = setCurTheatre.implementation!!.instructions
        fun requireOpcode(offset: Int, opcode: Opcode) {
            check(blockShape[lambdaStoreIndex + offset].opcode == opcode) {
                "Unexpected instruction at +$offset of the retired render-runnable block " +
                    "(${blockShape[lambdaStoreIndex + offset].opcode}, expected $opcode)"
            }
        }
        requireOpcode(1, Opcode.MONITOR_ENTER)
        val renderRunWrite = blockShape[lambdaStoreIndex + 2]
        check(
            renderRunWrite.opcode == Opcode.IPUT_OBJECT &&
                ((renderRunWrite as? ReferenceInstruction)?.reference as? FieldReference)?.name == "m_RenderRun",
        ) { "Unexpected m_RenderRun store shape in V4Theatre.SetCurSubTheatreID" }
        requireOpcode(3, Opcode.MONITOR_EXIT)
        requireOpcode(4, Opcode.GOTO)
        requireOpcode(5, Opcode.MOVE_EXCEPTION)
        requireOpcode(6, Opcode.MONITOR_EXIT)
        requireOpcode(7, Opcode.THROW)

        val saveTheatreIndex = setCurTheatre.invocationIndex(V4_THEATRE, "SaveTheatre")
        val postSwitchLabel = setCurTheatre.implementation!!.newLabelForIndex(saveTheatreIndex)
        setCurTheatre.replaceInstruction(lambdaStoreIndex, requestSceneCall)
        setCurTheatre.replaceInstruction(lambdaStoreIndex + 1, BuilderInstruction10t(Opcode.GOTO, postSwitchLabel))
        setCurTheatre.replaceInstruction(lambdaStoreIndex + 2, nop())
        setCurTheatre.replaceInstruction(lambdaStoreIndex + 3, nop())
        setCurTheatre.replaceInstruction(lambdaStoreIndex + 6, nop())

        // Region (ii): the RecoveryTheatres thread-spawn fallback becomes a single requestScene
        // call, exactly like the reference build.
        val recoveryIndex = setCurTheatre.invocationIndex(MAIN_ACTIVITY, "RecoveryTheatres")
        setCurTheatre.replaceInstruction(
            recoveryIndex,
            invokeStatic35c(
                RENDER_DISPATCH,
                "requestScene",
                listOf(V4_THEATRE, "I"),
                "Z",
                listOf(setCurTheatre.pRegister(0), setCurTheatre.pRegister(2)),
            ),
        )
        val fallbackShape = setCurTheatre.implementation!!.instructions
        (1..7).forEach { offset ->
            val instruction = fallbackShape[recoveryIndex + offset]
            val reference = (instruction as? ReferenceInstruction)?.reference
            val expected = when (offset) {
                1 -> instruction.opcode == Opcode.MOVE_RESULT
                2 -> instruction.opcode == Opcode.IF_NEZ
                3 -> instruction.opcode == Opcode.NEW_INSTANCE &&
                    (reference as? TypeReference)?.type == "Ljava/lang/Thread;"
                4 -> instruction.opcode == Opcode.NEW_INSTANCE &&
                    (reference as? TypeReference)?.type?.contains("uyDzj7PDi8mpAl48Uy6UoFHO77Y") == true
                5 -> instruction.opcode == Opcode.INVOKE_DIRECT &&
                    (reference as? com.android.tools.smali.dexlib2.iface.reference.MethodReference)?.name == "<init>"
                6 -> instruction.opcode == Opcode.INVOKE_DIRECT &&
                    (reference as? com.android.tools.smali.dexlib2.iface.reference.MethodReference)?.name == "<init>"
                7 -> instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                    (reference as? com.android.tools.smali.dexlib2.iface.reference.MethodReference)?.name == "start"
                else -> false
            }
            check(expected) {
                "Unexpected instruction shape at +$offset of the RecoveryTheatres fallback in " +
                    "V4Theatre.SetCurSubTheatreID"
            }
        }
        setCurTheatre.removeInstructions(recoveryIndex + 1, 7)

        // ------------------------------------------------------------------
        // V4Window: route teardown through the dispatch and publish the frame state to the
        // dispatch thread (volatile), matching the reference build.
        // ------------------------------------------------------------------
        val window = mutableClassDefBy(V4_WINDOW)
        val clearStatic = window.methods.first { it.name == "ClearStatic" && it.parameterTypes.isEmpty() }
        clearStatic.addInstruction(0, invokeStaticNoArgs(RENDER_DISPATCH, "reset", "V"))

        val setHide = window.methods.first { it.name == "SetHide" && it.parameterTypes.size == 1 }
        val removeBufferIndex = setHide.invocationIndex(V4_WINDOW, "RemoveBufferFromCPP")
        val removeBufferInstruction = setHide.implementation!!.instructions[removeBufferIndex]
        setHide.replaceInstruction(
            removeBufferIndex,
            retypeInvoke(
                removeBufferInstruction,
                Opcode.INVOKE_STATIC,
                ImmutableMethodReference(RENDER_DISPATCH, "removeWindow", listOf(V4_WINDOW, "J", "Z"), "V"),
            ),
        )

        listOf("m_bImageUpdate", "m_bVisible").forEach { fieldName ->
            val field = window.fields.first { it.name == fieldName }
            field.setAccessFlags(field.accessFlags or AccessFlags.VOLATILE.value)
        }
    }
}
