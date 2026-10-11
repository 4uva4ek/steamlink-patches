package app.template.patches.fourxvr

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.Constants.isFourXvr20022Build
import app.template.patches.shared.PatchCategories
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction12x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction23x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31i
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

/**
 * G6 — 4XVR subtitle suite for exact 4XVR 2.0.22/20022.
 *
 * Payload patch installs the subtitle position native library. The hook patch mirrors the
 * reference build: the subtitle panel grows by 60 px, SubtitlePositionControls.attach() adds
 * the position-control row right before the V4Window.AftCreate() super call, a color-tab change
 * persists the config immediately, V4VRSubtitleListener loads a custom subtitle font from the
 * 4XPlayer/fonts folder in place of the stock defaults, the wrapped-cue advance is clamped to
 * the 448 px texture budget and V4Config.m_bUseSubtitleItalic becomes volatile so the repaint
 * path observes style changes.
 */

private const val V4_SUBTITLES_UI = "Lcn/vr4p/oculus4xvrplayerov/PanelUI/V4SubtitlesUI;"
private const val V4_WINDOW = "Lcn/vr4p/oculus4xvrplayerov/V4Window;"
private const val V4_VR_SUBTITLE_LISTENER = "Lcn/vr4p/oculus4xvrplayerov/V4VRSubtitleListener;"
private const val V4_CONFIG = "Lcn/vr4p/vr4pmovieplayer/V4Config;"
private const val MAIN_ACTIVITY = "Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;"
private const val SUBTITLE_POSITION_CONTROLS = "Lcn/vr4p/patch/SubtitlePositionControls;"
private const val SUBTITLE_FONT = "Lcn/vr4p/patch/SubtitleFont;"

@Suppress("unused")
internal val fourXvrSubtitleSuitePayloadPatch = rawResourcePatch(
    name = "Subtitle suite payload (4XVR)",
    description = "Installs the subtitle position native library used by the 4XVR subtitle " +
        "suite controls on exact 4XVR 2.0.22 build 20022.",
    default = true,
) {
    category(PatchCategories.SUBTITLES_AND_PLAYBACK)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute
        get("lib/arm64-v8a/lib4xvr_subtitle_position.so")
            .writeBytes(loadFourXvrResource("subtitles/lib4xvr_subtitle_position.so"))
    }
}

@Suppress("unused")
internal val fourXvrSubtitleSuitePatch = bytecodePatch(
    name = "Subtitle suite (4XVR)",
    description = "Adds the subtitle position controls row, the enlarged subtitle panel height, " +
        "immediate config persistence on color-tab changes, the custom subtitle font load and " +
        "the wrapped-cue size clamp for exact 4XVR 2.0.22 build 20022. Headset validation pending.",
    default = true,
) {
    category(PatchCategories.SUBTITLES_AND_PLAYBACK)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())
    dependsOn(fourXvrSubtitleSuitePayloadPatch)
    extendWith("extensions/fourxvr-extension.mpe")

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute

        val subtitlesUi = mutableClassDefBy(V4_SUBTITLES_UI)

        // ------------------------------------------------------------------
        // V4SubtitlesUI.onCreate()V: grow the panel height argument of the
        // V4Window.onCreate(IIII)V call from 0x1b8 (440) to 0x1f4 (500) so the new
        // subtitle position row fits (the only 0x1b8 in the class).
        // ------------------------------------------------------------------
        val panelHeightSites = subtitlesUi.methods.flatMap { method ->
            method.implementation?.instructions?.withIndex()?.filter { (_, instruction) ->
                instruction.opcode == Opcode.CONST_16 &&
                    instruction is OneRegisterInstruction && instruction.registerA == 2 &&
                    instruction is NarrowLiteralInstruction && instruction.narrowLiteral == 0x1b8
            }?.map { method to it.index } ?: emptyList()
        }
        check(panelHeightSites.size == 1) {
            "Expected exactly one const/16 v2, 0x1b8 panel height in V4SubtitlesUI; " +
                "found ${panelHeightSites.size}"
        }
        val (panelHeightMethod, panelHeightIndex) = panelHeightSites.single()
        panelHeightMethod.replaceInstruction(panelHeightIndex, const16(2, 0x1f4))

        // ------------------------------------------------------------------
        // V4SubtitlesUI.onCreate()V: attach the subtitle position row immediately before
        // the unique V4Window.AftCreate() super call (the only AftCreate reference in the
        // class). The receiver must be the method receiver register.
        // ------------------------------------------------------------------
        val aftCreateSites = subtitlesUi.methods.flatMap { method ->
            method.implementation?.instructions?.withIndex()?.filter { (_, instruction) ->
                instruction.opcode == Opcode.INVOKE_SUPER &&
                    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)
                        ?.let { reference ->
                            reference.definingClass == V4_WINDOW && reference.name == "AftCreate" &&
                                reference.parameterTypes.isEmpty() && reference.returnType == "V"
                        } == true
            }?.map { method to it.index } ?: emptyList()
        }
        check(aftCreateSites.size == 1) {
            "Expected exactly one V4Window->AftCreate()V super call in V4SubtitlesUI; " +
                "found ${aftCreateSites.size}"
        }
        val (aftCreateMethod, aftCreateIndex) = aftCreateSites.single()
        val aftCreateReceiver = aftCreateMethod.pRegister(0)
        val aftCreateCall = aftCreateMethod.implementation!!.instructions[aftCreateIndex]
        check(
            aftCreateCall is FiveRegisterInstruction &&
                aftCreateCall.registerCount == 1 && aftCreateCall.registerC == aftCreateReceiver,
        ) { "Unexpected V4Window->AftCreate super-call shape in V4SubtitlesUI" }
        aftCreateMethod.addInstructions(
            aftCreateIndex,
            listOf(
                igetObject(0, aftCreateReceiver, V4_SUBTITLES_UI, "m_ViewGroup", "Landroid/view/ViewGroup;"),
                invokeStatic35c(
                    SUBTITLE_POSITION_CONTROLS,
                    "attach",
                    listOf("Landroid/view/ViewGroup;"),
                    "V",
                    listOf(0),
                ),
            ),
        )

        // ------------------------------------------------------------------
        // V4SubtitlesUI.lambda$onCreate$3$V4SubtitlesUI()V: persist the config immediately
        // after the RadioGroup.invalidate() that follows the m_iSubtitleColorIdx write
        // (the class has two invalidate sites; only this one follows that write).
        // ------------------------------------------------------------------
        val colorInvalidateSites = subtitlesUi.methods.flatMap { method ->
            method.implementation?.instructions?.withIndex()?.filter { (_, instruction) ->
                instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)
                        ?.let { reference ->
                            reference.definingClass == "Landroid/widget/RadioGroup;" &&
                                reference.name == "invalidate" &&
                                reference.parameterTypes.isEmpty() && reference.returnType == "V"
                        } == true
            }?.map { method to it.index } ?: emptyList()
        }
        val colorChangeSites = colorInvalidateSites.filter { (method, index) ->
            val preceding = method.implementation!!.instructions
            (1..3).any { distance ->
                val candidate = preceding.getOrNull(index - distance)
                candidate?.opcode == Opcode.SPUT &&
                    ((candidate as? ReferenceInstruction)?.reference as? FieldReference)
                        ?.let { it.definingClass == V4_CONFIG && it.name == "m_iSubtitleColorIdx" } == true
            }
        }
        check(colorChangeSites.size == 1) {
            "Expected exactly one RadioGroup->invalidate()V after a V4Config.m_iSubtitleColorIdx " +
                "write in V4SubtitlesUI; found ${colorChangeSites.size}"
        }
        val (colorMethod, colorIndex) = colorChangeSites.single()
        colorMethod.addInstructions(
            colorIndex + 1,
            listOf(
                sgetObject(1, V4_SUBTITLES_UI, "s_MainActivity", MAIN_ACTIVITY),
                const4(2, 0),
                invokeStatic35c(
                    V4_CONFIG,
                    "UpdateConfigSetting",
                    listOf("Landroid/content/Context;", "Z"),
                    "V",
                    listOf(1, 2),
                ),
            ),
        )

        // ------------------------------------------------------------------
        // V4VRSubtitleListener.<init>(Context)V: after the m_cThisContext store, load the
        // custom subtitle fonts from 4XPlayer/fonts and fill the two typeface slots, keeping
        // the stock defaults when the load returns null.
        // ------------------------------------------------------------------
        val listener = mutableClassDefBy(V4_VR_SUBTITLE_LISTENER)
        val constructor = listener.methods.first { it.name == "<init>" && it.parameterTypes.size == 1 }
        val contextStore = constructor.fieldAccessIndex(V4_VR_SUBTITLE_LISTENER, "m_cThisContext")
        val storeInstruction = constructor.implementation!!.instructions[contextStore]
        check(
            storeInstruction.opcode == Opcode.IPUT_OBJECT &&
                storeInstruction is TwoRegisterInstruction &&
                storeInstruction.registerA == constructor.pRegister(1) &&
                storeInstruction.registerB == constructor.pRegister(0),
        ) { "Unexpected m_cThisContext store shape in V4VRSubtitleListener.<init>" }
        check(constructor.implementation!!.instructions[contextStore + 1].opcode == Opcode.RETURN_VOID) {
            "Unexpected instruction after the m_cThisContext store in V4VRSubtitleListener.<init>"
        }
        val listenerReceiver = constructor.pRegister(0)
        constructor.addInstructions(
            contextStore + 1,
            listOf(
                invokeStaticNoArgs(SUBTITLE_FONT, "load", "[Landroid/graphics/Typeface;"),
                moveResult(0, objectResult = true),
            ),
        )
        val fontSkip = constructor.implementation!!.newLabelForIndex(contextStore + 3)
        constructor.addInstructions(
            contextStore + 3,
            listOf(
                BuilderInstruction21t(Opcode.IF_EQZ, 0, fontSkip),
                const4(1, 0),
                BuilderInstruction23x(Opcode.AGET_OBJECT, 1, 0, 1),
                iputObject(
                    1,
                    listenerReceiver,
                    V4_VR_SUBTITLE_LISTENER,
                    "g_SubtitleFace",
                    "Landroid/graphics/Typeface;",
                ),
                const4(1, 1),
                BuilderInstruction23x(Opcode.AGET_OBJECT, 1, 0, 1),
                iputObject(
                    1,
                    listenerReceiver,
                    V4_VR_SUBTITLE_LISTENER,
                    "g_SubtitleFaceItalic",
                    "Landroid/graphics/Typeface;",
                ),
            ),
        )

        // ------------------------------------------------------------------
        // V4RVSubtitleListener.UpdateSubtitle(...)V: clamp the wrapped-cue advance at both
        // mirror sites (the only two mul-float/2addr v4, v3 instructions in the class). The
        // stock line-area multiply becomes slack = max(0, min(getTextSize()*1.328 - lineHeight,
        // (448 - lineArea) / (lineCount - 1))); lineHeight += slack; lineArea += (lineCount - 1)
        // * slack. Sites are re-scanned because each edit shifts later indexes.
        // ------------------------------------------------------------------
        fun wrapSites() = listener.methods.flatMap { method ->
            method.implementation?.instructions?.withIndex()?.filter { (_, instruction) ->
                instruction.opcode == Opcode.MUL_FLOAT_2ADDR &&
                    (instruction as? TwoRegisterInstruction)
                        ?.let { it.registerA == 4 && it.registerB == 3 } == true
            }?.map { method to it.index } ?: emptyList()
        }
        check(wrapSites().size == 2) {
            "Expected 2 subtitle wrap sites (mul-float/2addr v4, v3) in V4VRSubtitleListener; " +
                "found ${wrapSites().size}"
        }
        repeat(2) {
            val (wrapMethod, m) = wrapSites().first()
            val contextInstructions = wrapMethod.implementation!!.instructions
            val intToFloat = contextInstructions[m - 1]
            check(
                intToFloat.opcode == Opcode.INT_TO_FLOAT &&
                    intToFloat is TwoRegisterInstruction &&
                    intToFloat.registerA == 4 && intToFloat.registerB == 4,
            ) { "Unexpected instruction before the wrap site at $m in ${wrapMethod.name}" }
            val paintRead = contextInstructions[m - 10]
            check(
                paintRead.opcode == Opcode.IGET_OBJECT &&
                    (paintRead as? TwoRegisterInstruction)?.registerB == 1 &&
                    ((paintRead as? ReferenceInstruction)?.reference as? FieldReference)
                        ?.let {
                            it.definingClass == V4_VR_SUBTITLE_LISTENER &&
                                it.name == "m_MySubtitlePaint"
                        } == true,
            ) { "Unexpected wrap-site context at $m in ${wrapMethod.name}" }

            wrapMethod.replaceInstruction(m, BuilderInstruction23x(Opcode.MUL_FLOAT, 10, 4, 3))
            wrapMethod.addInstructions(
                m + 1,
                listOf(
                    constHigh16(11, 0x3f800000),
                    BuilderInstruction12x(Opcode.SUB_FLOAT_2ADDR, 4, 11),
                    const4(11, 0),
                    BuilderInstruction23x(Opcode.CMPL_FLOAT, 12, 4, 11),
                ),
            )
            wrapMethod.addInstructions(
                m + 5,
                listOf(
                    igetObject(
                        11,
                        1,
                        V4_VR_SUBTITLE_LISTENER,
                        "m_MySubtitlePaint",
                        "Landroid/graphics/Paint;",
                    ),
                    BuilderInstruction35c(
                        Opcode.INVOKE_VIRTUAL,
                        1,
                        11,
                        0,
                        0,
                        0,
                        0,
                        ImmutableMethodReference("Landroid/graphics/Paint;", "getTextSize", emptyList(), "F"),
                    ),
                    moveResult(11, objectResult = false),
                    BuilderInstruction31i(Opcode.CONST, 12, 0x3fa9e000),
                    BuilderInstruction12x(Opcode.MUL_FLOAT_2ADDR, 11, 12),
                    BuilderInstruction12x(Opcode.SUB_FLOAT_2ADDR, 11, 3),
                    constHigh16(12, 0x43e00000),
                    BuilderInstruction12x(Opcode.SUB_FLOAT_2ADDR, 12, 10),
                    BuilderInstruction12x(Opcode.DIV_FLOAT_2ADDR, 12, 4),
                    invokeStatic35c("Ljava/lang/Math;", "min", listOf("F", "F"), "F", listOf(11, 12)),
                    moveResult(11, objectResult = false),
                    const4(12, 0),
                    invokeStatic35c("Ljava/lang/Math;", "max", listOf("F", "F"), "F", listOf(11, 12)),
                    moveResult(11, objectResult = false),
                    BuilderInstruction12x(Opcode.ADD_FLOAT_2ADDR, 3, 11),
                    BuilderInstruction12x(Opcode.MUL_FLOAT_2ADDR, 4, 11),
                    BuilderInstruction12x(Opcode.ADD_FLOAT_2ADDR, 10, 4),
                ),
            )
            wrapMethod.addInstructions(
                m + 22,
                listOf(BuilderInstruction12x(Opcode.MOVE, 4, 10)),
            )
            val clampEnd = wrapMethod.implementation!!.newLabelForIndex(m + 22)
            wrapMethod.addInstructions(
                m + 5,
                listOf(BuilderInstruction21t(Opcode.IF_LEZ, 12, clampEnd)),
            )
        }

        // ------------------------------------------------------------------
        // V4Config.m_bUseSubtitleItalic: volatile so the subtitle repaint path observes
        // style changes (matches the reference build).
        // ------------------------------------------------------------------
        val config = mutableClassDefBy(V4_CONFIG)
        val italicField = config.fields.first { it.name == "m_bUseSubtitleItalic" }
        check(italicField.type == "Z") { "Unexpected V4Config.m_bUseSubtitleItalic type: ${italicField.type}" }
        italicField.setAccessFlags(italicField.accessFlags or AccessFlags.VOLATILE.value)
    }
}
