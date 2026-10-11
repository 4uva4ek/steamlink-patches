package app.template.patches.fourxvr

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction10x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11n
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction11x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction12x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21ih
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21s
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22x
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableFieldReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableMethodReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import java.security.MessageDigest

/**
 * Shared helpers for the 4XVR Galaxy XR patch set (cn.vr4p.oculus4xvrplayerov).
 *
 * All dex edits use typed dexlib2 builders, never raw smali strings: the historical
 * InlineSmaliCompiler path crashes on several released Morphe Manager builds.
 */

internal fun loadFourXvrResource(name: String): ByteArray =
    (object {}.javaClass.getResourceAsStream("/fourxvr/$name")
        ?: error("Missing bundled resource: fourxvr/$name"))
        .use { it.readBytes() }

internal fun ByteArray.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }

/**
 * Register index of parameter word [index] (p0..) inside the method's register file.
 * Wide parameters (J/D) occupy 2 register words, so the offset counts words, not parameter
 * types: RenderFrame(ZJJ)V has 5 parameter words plus `this` at the stock register count.
 */
internal fun MutableMethod.pRegister(index: Int): Int {
    val registerCount = implementation!!.registerCount
    val parameterWords = parameterWordCount(parameterTypes) +
        if (AccessFlags.STATIC.isSet(accessFlags)) 0 else 1
    return registerCount - parameterWords + index
}

/** Parameter register words: long/double occupy 2, everything else 1. */
internal fun parameterWordCount(parameterTypes: List<CharSequence>): Int =
    parameterTypes.sumOf { type -> if (type.toString() == "J" || type.toString() == "D") 2 else 1 }

/** invoke-static/range {start .. start+count-1} for the given method reference. */
internal fun invokeStaticRange(
    definingClass: String,
    methodName: String,
    parameterTypes: List<String>,
    returnType: String,
    startRegister: Int,
    registerCount: Int,
): BuilderInstruction = BuilderInstruction3rc(
    Opcode.INVOKE_STATIC_RANGE,
    startRegister,
    registerCount,
    ImmutableMethodReference(definingClass, methodName, parameterTypes, returnType),
)

internal fun moveResult(destination: Int, objectResult: Boolean): BuilderInstruction =
    BuilderInstruction11x(
        if (objectResult) Opcode.MOVE_RESULT_OBJECT else Opcode.MOVE_RESULT,
        destination,
    )

internal fun nop(): BuilderInstruction = BuilderInstruction10x(Opcode.NOP)

internal fun const4(destination: Int, value: Int): BuilderInstruction =
    BuilderInstruction11n(Opcode.CONST_4, destination, value)

internal fun moveObject(destination: Int, source: Int): BuilderInstruction =
    BuilderInstruction12x(Opcode.MOVE_OBJECT, destination, source)

internal fun moveObjectFrom16(destination: Int, source: Int): BuilderInstruction =
    BuilderInstruction22x(Opcode.MOVE_OBJECT_FROM16, destination, source)

internal fun moveFrom16(destination: Int, source: Int): BuilderInstruction =
    BuilderInstruction22x(Opcode.MOVE_FROM16, destination, source)

internal fun const16(destination: Int, literal: Int): BuilderInstruction =
    BuilderInstruction21s(Opcode.CONST_16, destination, literal)

internal fun constHigh16(destination: Int, literal: Int): BuilderInstruction =
    BuilderInstruction21ih(Opcode.CONST_HIGH16, destination, literal)

internal fun sgetObject(destination: Int, definingClass: String, fieldName: String, fieldType: String): BuilderInstruction =
    BuilderInstruction21c(Opcode.SGET_OBJECT, destination, ImmutableFieldReference(definingClass, fieldName, fieldType))

internal fun igetObject(destination: Int, objectRegister: Int, definingClass: String, fieldName: String, fieldType: String): BuilderInstruction =
    BuilderInstruction22c(
        Opcode.IGET_OBJECT,
        destination,
        objectRegister,
        ImmutableFieldReference(definingClass, fieldName, fieldType),
    )

internal fun iputObject(objectRegister: Int, objectRegister2: Int, definingClass: String, fieldName: String, fieldType: String): BuilderInstruction =
    BuilderInstruction22c(
        Opcode.IPUT_OBJECT,
        objectRegister,
        objectRegister2,
        ImmutableFieldReference(definingClass, fieldName, fieldType),
    )

/** invoke-static with up to 5 explicit registers (35c form; registerCount = registers.size). */
internal fun invokeStatic35c(
    definingClass: String,
    methodName: String,
    parameterTypes: List<String>,
    returnType: String,
    registers: List<Int>,
): BuilderInstruction {
    require(registers.size <= 5) { "invokeStatic35c supports at most 5 registers" }
    return BuilderInstruction35c(
        Opcode.INVOKE_STATIC,
        registers.size,
        registers.getOrElse(0) { 0 },
        registers.getOrElse(1) { 0 },
        registers.getOrElse(2) { 0 },
        registers.getOrElse(3) { 0 },
        registers.getOrElse(4) { 0 },
        ImmutableMethodReference(definingClass, methodName, parameterTypes, returnType),
    )
}

internal fun invokeStaticNoArgs(definingClass: String, methodName: String, returnType: String): BuilderInstruction =
    invokeStatic35c(definingClass, methodName, emptyList(), returnType, emptyList())

internal fun constString(destination: Int, value: String): BuilderInstruction =
    BuilderInstruction21c(Opcode.CONST_STRING, destination, ImmutableStringReference(value))

/** All instruction indexes whose method reference matches class + name, in order. */
internal fun MutableMethod.invocationIndexes(definingClass: String, methodName: String): List<Int> =
    implementation!!.instructions.withIndex()
        .filter { (_, instruction) ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == definingClass && reference.name == methodName
        }
        .map { it.index }

/**
 * The [occurrence]-th instruction index whose method reference matches class + name.
 * Fails closed when the invocation is absent, so unknown layouts report a PatchException.
 */
internal fun MutableMethod.invocationIndex(
    definingClass: String,
    methodName: String,
    occurrence: Int = 0,
): Int = invocationIndexes(definingClass, methodName).let { indexes ->
    require(indexes.size > occurrence) {
        "Missing invocation #$occurrence of $definingClass->$methodName in $name (found ${indexes.size})"
    }
    indexes[occurrence]
}

internal fun MutableMethod.fieldAccessIndex(definingClass: String, fieldName: String): Int =
    implementation!!.instructions.indexOfFirst { instruction ->
        val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
        reference?.definingClass == definingClass && reference.name == fieldName
    }.also { index ->
        require(index >= 0) { "Missing field access $definingClass->$fieldName in $name" }
    }

/**
 * Rebuilds an invoke instruction with the same opcode and register operands but a new
 * method reference, preserving /range vs 35c encoding.
 */
internal fun retargetInvoke(instruction: BuilderInstruction, newReference: MethodReference): BuilderInstruction =
    when (instruction) {
        is RegisterRangeInstruction -> BuilderInstruction3rc(
            instruction.opcode,
            instruction.startRegister,
            instruction.registerCount,
            newReference,
        )
        is FiveRegisterInstruction -> BuilderInstruction35c(
            instruction.opcode,
            instruction.registerCount,
            instruction.registerC,
            instruction.registerD,
            instruction.registerE,
            instruction.registerF,
            instruction.registerG,
            newReference,
        )
        else -> error("Unsupported invoke shape ${instruction.opcode} in retargetInvoke")
    }

/**
 * Rebuilds an invoke instruction with a new opcode and method reference but the same register
 * operands, preserving /range vs 35c encoding (used to retarget an invoke-virtual onto a static
 * helper).
 */
internal fun retypeInvoke(instruction: BuilderInstruction, opcode: Opcode, newReference: MethodReference): BuilderInstruction =
    when (instruction) {
        is RegisterRangeInstruction -> BuilderInstruction3rc(
            opcode,
            instruction.startRegister,
            instruction.registerCount,
            newReference,
        )
        is FiveRegisterInstruction -> BuilderInstruction35c(
            opcode,
            instruction.registerCount,
            instruction.registerC,
            instruction.registerD,
            instruction.registerE,
            instruction.registerF,
            instruction.registerG,
            newReference,
        )
        else -> error("Unsupported invoke shape ${instruction.opcode} in retypeInvoke")
    }
