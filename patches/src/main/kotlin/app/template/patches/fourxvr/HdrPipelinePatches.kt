package app.template.patches.fourxvr

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.Constants.isFourXvr20022Build
import app.template.patches.shared.PatchCategories
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableTypeReference

/**
 * G3 — libplacebo HDR rendering pipeline for exact 4XVR 2.0.22/20022.
 *
 * Payload patch installs the modified libplacebo 7.360.1 (LGPL-2.1+), the 4XVR HDR bridge native
 * and the bundled license notices. The hook patch mirrors the reference build: HdrBridge loads
 * in Main4XActivity.<clinit>, V4Render notifies the bridge before every UpdateMovie submission
 * (HLG detected from the MediaFormat), and the decoder output surface is routed through the
 * bridge for the FP16 hand-off. Ghosting fixes use notifySeek/beginMovieTransition/
 * routeResumeSurface on the player lifecycle.
 */

private const val HDR_BRIDGE = "Ldev/eni/hdrdirect/HdrBridge;"
private const val V4_MEDIA_PLAYER = "Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;"
private const val V4_MEDIA_PLAYER_RUNNABLE = "Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer\$MyVideoRunnable;"
private const val MAIN_ACTIVITY = "Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;"
private const val RENDER_DISPATCH = "Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;"

@Suppress("unused")
internal val fourXvrHdrPayloadPatch = rawResourcePatch(
    name = "HDR pipeline payload (4XVR)",
    description = "Installs the modified libplacebo 7.360.1 (LGPL-2.1-or-later), the 4XVR HDR " +
        "bridge native and the bundled open-source notices for exact 4XVR 2.0.22 build 20022. " +
        FOUR_XVR_UPSTREAM_CREDIT,
    default = true,
) {
    category(PatchCategories.IMAGE_QUALITY)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute
        get("lib/arm64-v8a/libplacebo.so").writeBytes(loadFourXvrResource("hdr/libplacebo.so"))
        get("lib/arm64-v8a/lib4xvr_hdr_advanced.so")
            .writeBytes(loadFourXvrResource("hdr/lib4xvr_hdr_advanced.so"))
        get("assets/licenses/NOTICE.txt").writeBytes(loadFourXvrResource("licenses/NOTICE.txt"))
    }
}

@Suppress("unused")
internal val fourXvrHdrPipelinePatch = bytecodePatch(
    name = "libplacebo HDR pipeline (4XVR)",
    description = "Loads the HDR bridge, routes the decoder output surface through it for the " +
        "FP16 hand-off, detects HLG from the video MediaFormat and keeps the bridge informed of " +
        "movie/seek transitions for exact 4XVR 2.0.22 build 20022. Headset validation pending. " +
        FOUR_XVR_UPSTREAM_CREDIT,
    default = true,
) {
    category(PatchCategories.IMAGE_QUALITY)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())
    dependsOn(fourXvrHdrPayloadPatch)
    extendWith("extensions/fourxvr-extension.mpe")

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute

        // ------------------------------------------------------------------
        // Main4XActivity.<clinit>: load 4xvr_hdr_advanced and force-init the render dispatch
        // native right where the reference build placed them (before the stock
        // "ovrplatformloader" loadLibrary statement).
        // ------------------------------------------------------------------
        val mainActivity = mutableClassDefBy(MAIN_ACTIVITY)
        val clinit = mainActivity.methods.first { it.name == "<clinit>" && it.parameterTypes.isEmpty() }
        val ovrLoaderIndex = clinit.implementation!!.instructions.indexOfFirst { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference
            reference is StringReference && reference.string == "ovrplatformloader"
        }
        check(ovrLoaderIndex >= 0) { "Missing ovrplatformloader const-string in Main4XActivity.<clinit>" }
        clinit.addInstructions(
            ovrLoaderIndex,
            listOf(
                constString(0, "4xvr_hdr_advanced"),
                invokeStatic35c(
                    "Ljava/lang/System;",
                    "loadLibrary",
                    listOf("Ljava/lang/String;"),
                    "V",
                    listOf(0),
                ),
                invokeStaticNoArgs(RENDER_DISPATCH, "initialize", "V"),
            ),
        )

        // ------------------------------------------------------------------
        // V4Render.RenderFrame(JJ)V: HdrBridge.notifyMovie before every UpdateMovie submission;
        // the middle site switches the is-HDR boolean to HdrBridge.isHlg(MediaFormat, fallback).
        // ------------------------------------------------------------------
        val v4Render = mutableClassDefBy("Lcn/vr4p/oculus4xvrplayerov/V4Render;")
        val renderFrame = v4Render.methods.first { it.name == "RenderFrame" && it.parameterTypes.size == 2 }
        fun notifyMovie(): BuilderInstruction = invokeStaticRange(
            HDR_BRIDGE,
            "notifyMovie",
            listOf("I", "I", "I", "I", "I", "Z", "Z"),
            "V",
            13,
            7,
        )
        val updateMovieIndexes = renderFrame.invocationIndexes(MAIN_ACTIVITY, "UpdateMovie")
        check(updateMovieIndexes.size == 3) {
            "Expected 3 UpdateMovie sites in V4Render.RenderFrame; found ${updateMovieIndexes.size}"
        }

        // Middle site: move the stock v16/v17 saves in front of the Is_HDR_Bt2020Fix call
        // (the reference does this because the is-HLG block uses v3/v4 as scratch), then
        // replace the boolean feed with isHlg(m_iThisUpdateFormat, previousValue).
        val middleUpdateMovie = renderFrame.invocationIndexes(MAIN_ACTIVITY, "UpdateMovie")[1]
        val bt2020Indexes = renderFrame.invocationIndexes(V4_MEDIA_PLAYER, "Is_HDR_Bt2020Fix")
        val bt2020Index = bt2020Indexes.lastOrNull { it < middleUpdateMovie }
        check(bt2020Index != null) { "Missing Is_HDR_Bt2020Fix feed before the middle UpdateMovie site" }

        // Remove the originals (must be unique: dest/source pair (16,3) and (17,4)).
        fun moveFrom16Indexes(dest: Int, src: Int): List<Int> =
            renderFrame.implementation!!.instructions.withIndex()
                .filter { (_, instruction) ->
                    instruction.opcode == Opcode.MOVE_FROM16 &&
                        instruction is com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction &&
                        instruction.registerA == dest && instruction.registerB == src
                }
                .map { it.index }

        val v16Moves = moveFrom16Indexes(16, 3)
        val v17Moves = moveFrom16Indexes(17, 4)
        check(v16Moves.size == 1 && v17Moves.size == 1) {
            "Expected single move/from16 v16,v3 and v17,v4 before the middle UpdateMovie site; " +
                "found ${v16Moves.size} and ${v17Moves.size}"
        }
        renderFrame.removeInstruction(v17Moves.single())
        renderFrame.removeInstruction(v16Moves.single())

        // Recompute after removal, then move the saves in front of the Is_HDR_Bt2020Fix call.
        val bt2020AfterRemoval = renderFrame.invocationIndexes(V4_MEDIA_PLAYER, "Is_HDR_Bt2020Fix")
            .last { it < renderFrame.invocationIndexes(MAIN_ACTIVITY, "UpdateMovie")[1] }
        renderFrame.addInstructions(
            bt2020AfterRemoval,
            listOf(
                moveFrom16(16, 3),
                moveFrom16(17, 4),
            ),
        )

        // The Bt2020Fix result lands in v19 via a following MOVE_RESULT; insert the isHlg feed
        // right after that move-result.
        val moveResult19Index = renderFrame.implementation!!.instructions.withIndex()
            .first { (index, instruction) ->
                index > bt2020AfterRemoval &&
                    instruction.opcode == Opcode.MOVE_RESULT &&
                    instruction is com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction &&
                    instruction.registerA == 19
            }
            .index
        renderFrame.addInstructions(
            moveResult19Index + 1,
            listOf(
                igetObject(3, 1, V4_MEDIA_PLAYER, "m_iThisUpdateFormat", "Landroid/media/MediaFormat;"),
                moveFrom16(4, 19),
                invokeStatic35c(
                    HDR_BRIDGE,
                    "isHlg",
                    listOf("Landroid/media/MediaFormat;", "Z"),
                    "Z",
                    listOf(3, 4),
                ),
                moveResult(19, objectResult = false),
            ),
        )

        // notifyMovie immediately before every UpdateMovie submission (descending index order).
        renderFrame.invocationIndexes(MAIN_ACTIVITY, "UpdateMovie")
            .sortedDescending()
            .forEach { index -> renderFrame.addInstruction(index, notifyMovie()) }

        // ------------------------------------------------------------------
        // Vr4pMediaPlayer$MyVideoRunnable.run(): route the decoder output surface through the
        // HDR bridge. routeOutputSurface(codec, surface, format, listener) runs immediately after
        // the stock "color-transfer" const-string and the returned surface replaces the stock
        // reload feeding MediaCodec.configure. The const-string is the branch target of
        // :cond_1d/:goto_b, and no label exists between it and MediaCodec.configure, so every
        // path executes the block; scratch registers v2/v9/v10 are each rewritten by the stock
        // code before their next read (const/4 v2,3 / const/4 v9,0 / const/4 v10,0), so the stock
        // register count stays. Placing this call before the const-string instead (the reference
        // build's placement, parking in its extra local v43) leaves the scratch merged to
        // Conflict on the av01 path and the class fails ART verification — device evidence
        // (Galaxy XR AFZ3, 4XVR 2.0.22/20022): "VerifyError ... MyVideoRunnable.run(): [0x345]
        // register v2 has type Conflict but expected Reference: android.view.Surface".
        // ------------------------------------------------------------------
        val videoRunnable = mutableClassDefBy(V4_MEDIA_PLAYER_RUNNABLE)
        val run = videoRunnable.methods.first { it.name == "run" && it.parameterTypes.isEmpty() }

        val colorTransferIndexes = run.implementation!!.instructions.withIndex()
            .filter { (_, instruction) ->
                val reference = (instruction as? ReferenceInstruction)?.reference
                reference is StringReference && reference.string == "color-transfer"
            }
            .map { it.index }
        check(colorTransferIndexes.size == 1) {
            "Expected one color-transfer const-string in MyVideoRunnable.run; " +
                "found ${colorTransferIndexes.size}"
        }
        val colorTransferIndex = colorTransferIndexes.single()
        val transferConstant = run.implementation!!.instructions[colorTransferIndex + 1]
        check(
            transferConstant.opcode == Opcode.CONST_4 &&
                (transferConstant as? com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction)
                    ?.registerA == 2 &&
                (transferConstant as? com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction)
                    ?.narrowLiteral == 3,
        ) { "Unexpected transfer constant after the color-transfer const-string in run()" }

        run.addInstructions(
            colorTransferIndex + 1,
            listOf(
                igetObject(2, 1, V4_MEDIA_PLAYER_RUNNABLE, "this\$0", V4_MEDIA_PLAYER),
                igetObject(2, 2, V4_MEDIA_PLAYER, "m_Listener", "Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer\$MyOnFrameAvailableListener;"),
                igetObject(9, 1, V4_MEDIA_PLAYER_RUNNABLE, "this\$0", V4_MEDIA_PLAYER),
                invokeStatic35c(
                    V4_MEDIA_PLAYER,
                    "access\$200",
                    listOf(V4_MEDIA_PLAYER),
                    "Landroid/media/MediaCodec;",
                    listOf(9),
                ),
                moveResult(9, objectResult = true),
                igetObject(10, 1, V4_MEDIA_PLAYER_RUNNABLE, "this\$0", V4_MEDIA_PLAYER),
                invokeStatic35c(
                    V4_MEDIA_PLAYER,
                    "access\$1500",
                    listOf(V4_MEDIA_PLAYER),
                    "Landroid/view/Surface;",
                    listOf(10),
                ),
                moveResult(10, objectResult = true),
                invokeStatic35c(
                    HDR_BRIDGE,
                    "routeOutputSurface",
                    listOf(
                        "Landroid/media/MediaCodec;",
                        "Landroid/view/Surface;",
                        "Landroid/media/MediaFormat;",
                        "Ljava/lang/Object;",
                    ),
                    "Landroid/view/Surface;",
                    listOf(9, 10, 7, 2),
                ),
                moveResult(9, objectResult = true),
            ),
        )

        // Replace the stock surface reload right before MediaCodec.configure with the routed
        // surface parked in v9 by the block above. run() has four access$1500 surface loads;
        // exactly one feeds the video configure through the [move-result-object v2,
        // const/4 v9, const/4 v10] tail — the same block the reference build swaps for its
        // routed surface (it parks in v43 with a 43->44 register bump; v9 needs none).
        val surfaceReloadAnchors = run.implementation!!.instructions.withIndex()
            .filter { (index, instruction) ->
                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                if (reference?.definingClass != V4_MEDIA_PLAYER || reference.name != "access\$1500") {
                    return@filter false
                }
                val following = run.implementation!!.instructions
                val moveResult = following.getOrNull(index + 1)
                val constNine = following.getOrNull(index + 2)
                val constTen = following.getOrNull(index + 3)
                val configure = following.getOrNull(index + 4)
                moveResult?.opcode == Opcode.MOVE_RESULT_OBJECT &&
                    (moveResult as? com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction)
                        ?.registerA == 2 &&
                    constNine?.opcode == Opcode.CONST_4 &&
                    (constNine as? com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction)
                        ?.registerA == 9 &&
                    (constNine as? com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction)
                        ?.narrowLiteral == 0 &&
                    constTen?.opcode == Opcode.CONST_4 &&
                    (constTen as? com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction)
                        ?.registerA == 10 &&
                    (constTen as? com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction)
                        ?.narrowLiteral == 0 &&
                    (configure as? ReferenceInstruction)?.reference?.let { configureReference ->
                        configureReference is MethodReference &&
                            configureReference.definingClass == "Landroid/media/MediaCodec;" &&
                            configureReference.name == "configure"
                    } == true
            }
            .map { it.index }
        check(surfaceReloadAnchors.size == 1) {
            "Expected one access\$1500 surface reload feeding the MediaCodec.configure v9/v10 " +
                "const tail in run(); found ${surfaceReloadAnchors.size}"
        }
        val surfaceReload = surfaceReloadAnchors.single()
        val reloadPrecondition = run.implementation!!.instructions[surfaceReload - 1]
        check(
            reloadPrecondition.opcode == Opcode.IGET_OBJECT &&
                (reloadPrecondition as? ReferenceInstruction)?.reference?.let { reference ->
                    reference is FieldReference && reference.name == "this\$0"
                } == true,
        ) { "Unexpected instruction before the access\$1500 surface reload in run()" }
        val reloadResult = run.implementation!!.instructions[surfaceReload + 1]
        check(
            reloadResult.opcode == Opcode.MOVE_RESULT_OBJECT &&
                (reloadResult as? com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction)
                    ?.registerA == 2,
        ) { "Unexpected move-result after the access\$1500 surface reload in run()" }
        run.replaceInstruction(surfaceReload - 1, moveObject(2, 9))
        run.replaceInstruction(surfaceReload, nop())
        run.replaceInstruction(surfaceReload + 1, nop())

        // ------------------------------------------------------------------
        // Vr4pMediaPlayer lifecycle hooks: beginMovieTransition at SetDataSource,
        // notifySeek at seekPosForce, routeResumeSurface at ActResume.
        // ------------------------------------------------------------------
        val player = mutableClassDefBy(V4_MEDIA_PLAYER)

        val setDataSource = player.methods.first {
            it.name == "SetDataSource" && it.parameterTypes.size == 5
        }
        // The reference inserts beginMovieTransition right after the m_bInSetDataSourceState
        // write that precedes m_bHWDecodeError; anchor on that pair so a second write of the
        // same field elsewhere in the method cannot be picked.
        val inSetDataSourceIndexes = setDataSource.implementation!!.instructions.withIndex()
            .filter { (_, instruction) ->
                instruction.opcode == Opcode.IPUT_BOOLEAN &&
                    ((instruction as? ReferenceInstruction)?.reference as? FieldReference)
                        ?.let { it.name == "m_bInSetDataSourceState" && it.definingClass == V4_MEDIA_PLAYER } == true
            }
            .map { it.index }
        val beginTransitionIndexes = inSetDataSourceIndexes.filter { index ->
            val following = setDataSource.implementation!!.instructions
            (1..2).any { offset ->
                val instruction = following.getOrNull(index + offset)
                instruction?.opcode == Opcode.IPUT_BOOLEAN &&
                    ((instruction as? ReferenceInstruction)?.reference as? FieldReference)?.name == "m_bHWDecodeError"
            }
        }
        check(beginTransitionIndexes.size == 1) {
            "Expected one m_bInSetDataSourceState write before m_bHWDecodeError in SetDataSource; " +
                "found ${beginTransitionIndexes.size}"
        }
        setDataSource.addInstruction(
            beginTransitionIndexes.single() + 1,
            invokeStaticNoArgs(HDR_BRIDGE, "beginMovieTransition", "V"),
        )

        val seekPosForce = player.methods.first { it.name == "seekPosForce" && it.parameterTypes.size == 1 }
        seekPosForce.addInstruction(0, invokeStaticNoArgs(HDR_BRIDGE, "notifySeek", "V"))

        val actResume = player.methods.first { it.name == "ActResume" && it.parameterTypes.size == 2 }
        val setOutputSurfaceIndex = actResume.invocationIndex("Landroid/media/MediaCodec;", "setOutputSurface")
        val setOutputSurface = actResume.implementation!!.instructions[setOutputSurfaceIndex]
        val setOutputRegisters = when (setOutputSurface) {
            is com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction ->
                listOf(
                    setOutputSurface.registerC,
                    setOutputSurface.registerD,
                )
            is com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction ->
                (0 until setOutputSurface.registerCount).map { setOutputSurface.startRegister + it }
            else -> error("Unsupported setOutputSurface invocation shape")
        }
        check(setOutputRegisters.size == 2) { "Unexpected setOutputSurface register shape in ActResume" }
        // The reference build's routeResumeSurface call sits INSIDE the stock setOutputSurface try
        // range, so a routing failure reaches the stock handlers (Exception -> codec recovery,
        // catchall -> rethrow). dexlib2 cannot relocate the stock range; instead the inserted call
        // gets its own range over the same stock handler labels.
        val actResumeImplementation = actResume.implementation!!
        val stockRangeBlocks = actResumeImplementation.tryBlocks.filter {
            it.start.location.index == setOutputSurfaceIndex
        }
        val exceptionHandler = stockRangeBlocks
            .firstOrNull { it.exceptionHandler.exceptionType == "Ljava/lang/Exception;" }
            ?.exceptionHandler
            ?.handler
        val catchallHandler = stockRangeBlocks
            .firstOrNull { it.exceptionHandler.exceptionType == null }
            ?.exceptionHandler
            ?.handler
        check(exceptionHandler != null && catchallHandler != null) {
            "Expected Exception and catchall handlers over the ActResume setOutputSurface range; " +
                "found ${stockRangeBlocks.map { it.exceptionHandler.exceptionType }}"
        }
        actResume.addInstructions(
            setOutputSurfaceIndex,
            listOf(
                invokeStatic35c(
                    HDR_BRIDGE,
                    "routeResumeSurface",
                    listOf("Landroid/media/MediaCodec;", "Landroid/view/Surface;"),
                    "Landroid/view/Surface;",
                    setOutputRegisters,
                ),
                moveResult(setOutputRegisters[1], objectResult = true),
            ),
        )
        val routeStart = actResumeImplementation.newLabelForIndex(setOutputSurfaceIndex)
        val routeEnd = actResumeImplementation.newLabelForIndex(setOutputSurfaceIndex + 2)
        actResumeImplementation.addCatch(
            ImmutableTypeReference("Ljava/lang/Exception;"),
            routeStart,
            routeEnd,
            exceptionHandler,
        )
        actResumeImplementation.addCatch(routeStart, routeEnd, catchallHandler)
    }
}
