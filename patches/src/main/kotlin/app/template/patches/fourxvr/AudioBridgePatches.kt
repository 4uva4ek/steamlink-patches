package app.template.patches.fourxvr

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.Constants.isFourXvr20022Build
import app.template.patches.shared.PatchCategories
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference

/**
 * G4 — 5.1-channel audio bridge for exact 4XVR 2.0.22/20022.
 *
 * Payload patch installs the bundled 5.1-channel AAudio bridge native (lib4xvr_audio_bridge.so).
 * The hook patch mirrors the reference build: every stock AAudio call site of
 * Vr4pMediaPlayer$MyAudioRunnable and the player-level SetListenPoseAAudio/ReleaseAAudio calls are
 * re-routed to the GxrAudioOutput bridge shipped by extensions/fourxvr-extension.mpe,
 * Main4XActivity.onCreate initializes the bridge, and the subtitle-style listener refreshes on the
 * playback-speed tick.
 */

private const val AUDIO_BRIDGE = "Lcn/vr4p/patch/GxrAudioOutput;"
private const val SUBTITLE_STYLE_LISTENER = "Lcn/vr4p/patch/SubtitleStyleListener;"
private const val MAIN_ACTIVITY = "Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;"
private const val V4_MEDIA_PLAYER = "Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;"
private const val V4_MEDIA_PLAYER_AUDIO_RUNNABLE =
    "Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer\$MyAudioRunnable;"
private const val V4_MEDIA_PLAYER_SPEED = "Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer\$MediaPlayerSpeed;"
private const val V4_MEDIA_PLAYER_SUBTITLE_LISTENER =
    "Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer\$SubtitleListener;"

@Suppress("unused")
internal val fourXvrAudioBridgePayloadPatch = rawResourcePatch(
    name = "Audio bridge payload (4XVR)",
    description = "Installs the bundled 5.1-channel AAudio bridge native for exact 4XVR 2.0.22 " +
        "build 20022.",
    default = false,
) {
    category(PatchCategories.TRACKING_AND_AUDIO)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute
        get("lib/arm64-v8a/lib4xvr_audio_bridge.so")
            .writeBytes(loadFourXvrResource("audio/lib4xvr_audio_bridge.so"))
    }
}

@Suppress("unused")
internal val fourXvrAudioBridgePatch = bytecodePatch(
    name = "5.1-channel audio bridge (4XVR)",
    description = "Routes the AAudio calls of Vr4pMediaPlayer and its audio runnable through the " +
        "bundled 5.1-channel bridge, initializes the bridge in Main4XActivity and refreshes the " +
        "subtitle style on the playback-speed tick for exact 4XVR 2.0.22 build 20022. " +
        "Headset validation pending.",
    default = false,
) {
    category(PatchCategories.TRACKING_AND_AUDIO)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())
    dependsOn(fourXvrAudioBridgePayloadPatch)
    extendWith("extensions/fourxvr-extension.mpe")

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute

        // ------------------------------------------------------------------
        // MyAudioRunnable: re-route every stock AAudio call site to the GxrAudioOutput bridge.
        // The reference build redirects exactly these 17 invocations; each keeps its stock method
        // name, parameters and return type — only the defining class changes.
        // ------------------------------------------------------------------
        val expectedRedirectCounts = mapOf(
            "GetSampleCountInBuf" to 2,
            "IsPlayingAAudio" to 3,
            "PlayAAudio" to 2,
            "PauseAAudio" to 1,
            "ResetAAudio" to 1,
            "CreateAAudio" to 2,
            "NeedWaitAAudio" to 1,
            "ReleaseAAudio" to 2,
            "UpdateVolumeBoost" to 2,
            "WriteAAudio" to 1,
        )

        val audioRunnable = mutableClassDefBy(V4_MEDIA_PLAYER_AUDIO_RUNNABLE)
        val audioCallSites = audioRunnable.methods
            .filter { it.implementation != null }
            .associateWith { method ->
                method.implementation!!.instructions.withIndex().filter { (_, instruction) ->
                    val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                    reference != null &&
                        reference.definingClass == V4_MEDIA_PLAYER &&
                        reference.name in expectedRedirectCounts
                }.toList()
            }

        expectedRedirectCounts.forEach { (methodName, expected) ->
            val found = audioCallSites.values.sumOf { sites ->
                sites.count { (_, instruction) ->
                    ((instruction as ReferenceInstruction).reference as MethodReference).name == methodName
                }
            }
            check(found == expected) {
                "Expected $expected stock $methodName call sites in " +
                    "$V4_MEDIA_PLAYER_AUDIO_RUNNABLE; found $found"
            }
        }

        audioCallSites.forEach { (method, sites) ->
            sites.forEach { (index, instruction) ->
                val reference = (instruction as ReferenceInstruction).reference as MethodReference
                check(
                    instruction.opcode == Opcode.INVOKE_STATIC ||
                        instruction.opcode == Opcode.INVOKE_STATIC_RANGE,
                ) {
                    "Unexpected ${instruction.opcode} for ${reference.name} in ${method.name}"
                }
                method.replaceInstruction(
                    index,
                    retargetInvoke(
                        instruction,
                        ImmutableMethodReference(
                            AUDIO_BRIDGE,
                            reference.name,
                            reference.parameterTypes,
                            reference.returnType,
                        ),
                    ),
                )
            }
        }

        // ------------------------------------------------------------------
        // Subtitle-style refresh tick: insert the reference build's five-instruction block
        // immediately before the access$3800 speed read whose following instructions test
        // MediaPlayerSpeed.bAudioSpeed (unique site).
        // ------------------------------------------------------------------
        val run = audioRunnable.methods.first { it.name == "run" && it.parameterTypes.isEmpty() }
        val speedReadIndexes = run.implementation!!.instructions.withIndex()
            .filter { (index, instruction) ->
                val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                reference != null && reference.definingClass == V4_MEDIA_PLAYER &&
                    reference.name == "access\$3800" &&
                    (1..2).any { offset ->
                        val following = run.implementation!!.instructions.getOrNull(index + offset)
                        following?.opcode == Opcode.IGET_BOOLEAN &&
                            ((following as? ReferenceInstruction)?.reference as? FieldReference)
                                ?.let {
                                    it.name == "bAudioSpeed" && it.definingClass == V4_MEDIA_PLAYER_SPEED
                                } == true
                    }
            }
            .map { it.index }
        check(speedReadIndexes.size == 1) {
            "Expected one access\$3800 speed read followed by a bAudioSpeed read in " +
                "$V4_MEDIA_PLAYER_AUDIO_RUNNABLE.run(); found ${speedReadIndexes.size}"
        }
        run.addInstructions(
            speedReadIndexes.single(),
            listOf(
                igetObject(1, 0, V4_MEDIA_PLAYER_AUDIO_RUNNABLE, "this\$0", V4_MEDIA_PLAYER),
                invokeStatic35c(
                    V4_MEDIA_PLAYER,
                    "access\$8400",
                    listOf(V4_MEDIA_PLAYER),
                    V4_MEDIA_PLAYER_SUBTITLE_LISTENER,
                    listOf(1),
                ),
                moveResult(1, objectResult = true),
                invokeStatic35c(
                    SUBTITLE_STYLE_LISTENER,
                    "refresh",
                    listOf(V4_MEDIA_PLAYER_SUBTITLE_LISTENER),
                    "V",
                    listOf(1),
                ),
                igetObject(1, 0, V4_MEDIA_PLAYER_AUDIO_RUNNABLE, "this\$0", V4_MEDIA_PLAYER),
            ),
        )

        // ------------------------------------------------------------------
        // Vr4pMediaPlayer: re-route the SetCameraPose AAudio pose call and the release()
        // ReleaseAAudio call to the bridge, keeping each stock signature and invoke encoding.
        // ------------------------------------------------------------------
        val player = mutableClassDefBy(V4_MEDIA_PLAYER)

        val setCameraPose = player.methods.first {
            it.name == "SetCameraPose" && it.parameterTypes.size == 6
        }
        val poseCallIndexes = setCameraPose.invocationIndexes(V4_MEDIA_PLAYER, "SetListenPoseAAudio")
        check(poseCallIndexes.size == 1) {
            "Expected one SetListenPoseAAudio call in SetCameraPose; found ${poseCallIndexes.size}"
        }
        val poseCallIndex = poseCallIndexes.single()
        val poseInvocation = setCameraPose.implementation!!.instructions[poseCallIndex]
        val poseReference = (poseInvocation as ReferenceInstruction).reference as MethodReference
        check(
            poseInvocation.opcode == Opcode.INVOKE_STATIC ||
                poseInvocation.opcode == Opcode.INVOKE_STATIC_RANGE,
        ) {
            "Unexpected ${poseInvocation.opcode} for the SetListenPoseAAudio call in SetCameraPose"
        }
        setCameraPose.replaceInstruction(
            poseCallIndex,
            retargetInvoke(
                poseInvocation,
                ImmutableMethodReference(
                    AUDIO_BRIDGE,
                    poseReference.name,
                    poseReference.parameterTypes,
                    poseReference.returnType,
                ),
            ),
        )

        val release = player.methods.first { it.name == "release" && it.parameterTypes.isEmpty() }
        val releaseCallIndexes = release.invocationIndexes(V4_MEDIA_PLAYER, "ReleaseAAudio")
        check(releaseCallIndexes.size == 1) {
            "Expected one ReleaseAAudio call in release(); found ${releaseCallIndexes.size}"
        }
        val releaseCallIndex = releaseCallIndexes.single()
        val releaseInvocation = release.implementation!!.instructions[releaseCallIndex]
        val releaseReference = (releaseInvocation as ReferenceInstruction).reference as MethodReference
        check(
            releaseInvocation.opcode == Opcode.INVOKE_STATIC ||
                releaseInvocation.opcode == Opcode.INVOKE_STATIC_RANGE,
        ) {
            "Unexpected ${releaseInvocation.opcode} for the ReleaseAAudio call in release()"
        }
        release.replaceInstruction(
            releaseCallIndex,
            retargetInvoke(
                releaseInvocation,
                ImmutableMethodReference(
                    AUDIO_BRIDGE,
                    releaseReference.name,
                    releaseReference.parameterTypes,
                    releaseReference.returnType,
                ),
            ),
        )

        // ------------------------------------------------------------------
        // Main4XActivity.onCreate: initialize the bridge right after the NativeActivity onCreate
        // super call, where the reference build places it.
        // ------------------------------------------------------------------
        val mainActivity = mutableClassDefBy(MAIN_ACTIVITY)
        val onCreate = mainActivity.methods.first {
            it.name == "onCreate" && it.parameterTypes.size == 1
        }
        val superCallIndexes = onCreate.implementation!!.instructions.withIndex()
            .filter { (_, instruction) ->
                (instruction.opcode == Opcode.INVOKE_SUPER ||
                    instruction.opcode == Opcode.INVOKE_SUPER_RANGE) &&
                    ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == "onCreate"
            }
            .map { it.index }
        check(superCallIndexes.size == 1) {
            "Expected one NativeActivity.onCreate super call in Main4XActivity.onCreate; " +
                "found ${superCallIndexes.size}"
        }
        onCreate.addInstruction(
            superCallIndexes.single() + 1,
            invokeStaticNoArgs(AUDIO_BRIDGE, "initialize", "V"),
        )
    }
}
