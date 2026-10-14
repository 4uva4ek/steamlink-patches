package util

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.apk.ApkUtils.applyTo
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.fourxvr.fourXvrAudioBridgePatch
import app.template.patches.fourxvr.fourXvrHdrPipelinePatch
import app.template.patches.fourxvr.fourXvrRenderDispatchPatch
import app.template.patches.fourxvr.fourXvrSubtitleSuitePatch
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/** Actual Morphe execution and unsigned APK packaging of the retained exact 4XVR 2.0.22/20022
 * stock APK with the four changed hook patches plus their dependency closures: render dispatch
 * (Main4XActivity drain label-join), subtitle suite (V4SubtitlesUI attach label-join and the
 * SubtitleStyleListener retarget), audio bridge (the access$7702 GxrAudioOutput bind insert) and
 * the HDR pipeline (ActResume try-range coverage for routeResumeSurface).
 *
 * Run in a fresh JVM: singleton patch execution state is not reusable across runs.
 * This audit proves patch application and unsigned APK packaging from the retained stock input;
 * it is not an installation, Android runtime or headset validation.
 */
object FourXvrParityMorpheAudit {
    private const val ORIGINAL_PACKAGE = "cn.vr4p.oculus4xvrplayerov"
    private const val ORIGINAL_APK_SIZE = 390_601_516L
    private const val ORIGINAL_APK_SHA256 =
        "24b83ab584675e02570e5318a35c7fa9b8da7eabb1f0763711c74c1bad3a84a8"

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) {
            "Usage: FourXvrParityMorpheAudit <original.apk> <new-output-directory>"
        }
        val originalApk = File(args[0]).canonicalFile
        val directory = File(args[1]).canonicalFile
        require(originalApk.isFile && originalApk.length() == ORIGINAL_APK_SIZE) {
            "Expected the retained exact 4XVR 2.0.22/20022 stock APK; got ${originalApk.path}"
        }
        require(originalApk.sha256() == ORIGINAL_APK_SHA256) { "Original APK SHA-256 mismatch" }
        require(!directory.exists() || directory.listFiles().isNullOrEmpty()) {
            "Use an absent or empty output directory: $directory"
        }
        check(directory.isDirectory || directory.mkdirs()) { "Could not create audit output directory" }
        val lines = mutableListOf<String>()
        fun record(line: String) { lines += line; println(line) }
        try {
            record("Input: ${originalApk.path}; bytes=$ORIGINAL_APK_SIZE; SHA-256=$ORIGINAL_APK_SHA256")
            record("Case: exact 4XVR 2.0.22/20022 stock base; metadata guard then the four changed hook patches")
            record("Evidence: actual Morphe execution/finalize and unsigned APK packaging from the retained stock input; no installation, Android runtime or headset proof.")

            val metadataGuard = rawResourcePatch(name = "4XVR parity hooks original APK metadata audit") {
                execute {
                    check(packageMetadata.packageName == ORIGINAL_PACKAGE) {
                        "Original package mismatch: ${packageMetadata.packageName}"
                    }
                    check(packageMetadata.versionName == "2.0.22" && packageMetadata.versionCode == "20022") {
                        "Exact metadata mismatch: ${packageMetadata.versionName}/${packageMetadata.versionCode}"
                    }
                }
            }
            val selection = linkedSetOf<Patch<*>>(metadataGuard)
            selection += fourXvrRenderDispatchPatch
            selection += fourXvrSubtitleSuitePatch
            selection += fourXvrAudioBridgePatch
            selection += fourXvrHdrPipelinePatch

            val closure = linkedSetOf<Patch<*>>()
            fun collect(patch: Patch<*>) {
                if (closure.add(patch)) patch.dependencies.forEach(::collect)
            }
            selection.forEach(::collect)
            record("Selection: ${selection.joinToString { it.name.orEmpty() }}")
            record("Dependency closure: ${closure.joinToString { it.name.orEmpty() }}")

            val isolatedInput = File(directory, "isolated-original-input.apk")
            val output = File(directory, "4xvr-2.0.22-20022-parity-hooks-unsigned.apk")
            originalApk.copyTo(isolatedInput)
            originalApk.copyTo(output)
            runBlocking {
                Patcher(PatcherConfig(isolatedInput, File(directory, "temporary"))).use { patcher ->
                    patcher += selection
                    val results = patcher().toList()
                    results.forEach { result ->
                        record("Morphe ${if (result.exception == null) "PASS" else "FAIL"}: ${result.patch.name}")
                        result.exception?.let { record(it.stackTraceToString()) }
                    }
                    check(results.none { it.exception != null }) { "Morphe execution/finalize failed" }
                    patcher.get().applyTo(output)
                }
            }
            check(output.isFile) { "Morphe emitted no APK" }
            ZipFile(originalApk).use { original -> ZipFile(output).use { patched ->
                val before = original.fileNames()
                val after = patched.fileNames()
                check("AndroidManifest.xml" in after && "classes.dex" in after) {
                    "Output lacks the manifest or the primary DEX entry"
                }
                (before - after).forEach { record("Entry removed: $it") }
                (after - before).forEach { record("Entry added: $it") }
                record("Packaged entries: input=${before.size}; output=${after.size}")
            } }
            check(originalApk.sha256() == ORIGINAL_APK_SHA256) { "Original stock input was modified" }
            record("Output: ${output.path}; bytes=${output.length()}; larger-than-input=${output.length() > ORIGINAL_APK_SIZE}; SHA-256=${output.sha256()}; unsigned=true")
            record("PASS retained stock input unchanged; unsigned package written; hook-level DEX verification follows as recorded verification steps.")
        } catch (failure: Throwable) {
            record("FAIL: ${failure.stackTraceToString()}")
            throw failure
        } finally {
            File(directory, "report.txt").writeText(lines.joinToString("\n", postfix = "\n"))
        }
    }

    private fun ZipFile.fileNames(): Set<String> {
        val names = entries().asSequence().filterNot { it.isDirectory }.map { it.name }.toList()
        check(names.distinct().size == names.size) { "Duplicate APK entries" }
        return names.toSet()
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { stream ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
