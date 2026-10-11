package util

import app.morphe.patcher.Patcher
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.apk.ApkUtils.applyTo
import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.rawResourcePatch
import app.template.patches.steamlink.androidxr.gxrModernTongueBridgePatch
import app.template.patches.steamlink.androidxr.patchModernTongueTransport
import app.template.patches.steamlink.androidxr.unrestrictedBatteryUsagePatch
import app.template.patches.steamlink.androidxr.xrGalaxyXrHighResolutionPatch
import app.template.patches.steamlink.binary.*
import app.template.patches.steamlink.galaxyXrRecommended5002363Patch
import com.android.apksig.ApkVerifier
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * Standalone genuine-APK audit for the experimental Uber link-rescan-disable patch,
 * mirroring the retired FEC duplicate-reservation audit (`git show 7e47a8e`).
 *
 * Deliberate reductions and adaptations for this experiment:
 *  - single exact base 2.0.23/5002363 only (FEC covered 2.0.22/5002322 and 2.0.23/5002363);
 *  - cases reduced to the five this experiment needs: baseline, standalone, bundle-first,
 *    guard-first, reapply. The FEC observe/pipeline variants (baseline-observe,
 *    observe-guard-last, guard-first-observe) and the decoder-helper checks are dropped:
 *    no decoder helper exists for this experiment;
 *  - the expected edit is recomputed independently here and never trusted from the
 *    production helper: NOP `1f2003d5` at file offsets 0x174c74 and 0x16fdf8 in
 *    lib/arm64-v8a/libvrlink_scene.so, pinned to the frozen gate0 evidence
 *    (stock sha256 628821fe..., expected patched sha256 836803ef..., size 2292008);
 *  - the stock 1 MiB UDP SO_RCVBUF request at 0x1757a8 must survive unchanged in every
 *    case, reported as stock_udp_receive_buffer_preserved;
 *  - audit.json keeps the FEC per-case keys verbatim so the receipt tooling works
 *    unchanged; the "guard" wording in two of those keys is retained for compatibility.
 *
 * Harness contract (mirrors the FEC build script): bundle-first/guard-first receive the
 * baseline case's result-unsigned.apk as the extra reference argument; reapply receives the
 * previous fully patched result (the guard-first result-unsigned.apk, the FEC analog of
 * observe-guard-last) as its input, so it is unsigned and its signature check is skipped.
 *
 * One case per JVM, with the tested MPP first and no compiled-class/resource directories.
 */
object UberLinkRescanDisableApkAudit {
    private const val SCENE = "lib/arm64-v8a/libvrlink_scene.so"
    private const val STOCK_SCENE_SHA256 = "628821feab199d7712be8a51273eb9a21ec440a7c91aa6a768cc7307a4fe22f0"
    private const val PATCHED_SCENE_SHA256 = "836803ef2140d347772eebe1118d4ad69b1e3e59f06570a1b46b55a06c6ca91b"
    private const val SOURCE_APK_SHA256 = "36b21974db9f5cd9f54cdf850565b80d31c76a2dd7608406fe160e3005d976d6"
    private const val SCENE_SIZE = 2_292_008
    private const val STOCK_UDP_OFFSET = 0x1757a8
    private val signature = Regex("META-INF/(MANIFEST\\.MF|[^/]+\\.(SF|RSA|DSA|EC))", RegexOption.IGNORE_CASE)
    private val selections = setOf("baseline", "standalone", "bundle-first", "guard-first", "reapply")

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        require(args.size in 6..7) {
            "Usage: UberLinkRescanDisableApkAudit <archive.mpp> <input.apk> <version> <code> " +
                "<${selections.joinToString("|")}> <case-directory> [reference.apk]"
        }
        val archive = File(args[0]).canonicalFile
        val input = File(args[1]).canonicalFile
        val version = args[2]
        val code = args[3]
        val selection = args[4]
        val directory = File(args[5]).canonicalFile
        val suppliedReference = args.getOrNull(6)?.let { File(it).canonicalFile }
        require(archive.isFile && input.isFile)
        require(version == "2.0.23" && code == "5002363") { "Unsupported exact APK pair" }
        require(selection in selections) { "Unknown audit selection: $selection" }
        val baselineCase = selection == "baseline"
        val reference = when (selection) {
            "standalone", "reapply" -> input.also {
                require(suppliedReference == null || suppliedReference == input) {
                    "$selection must compare against its input APK"
                }
            }
            "baseline" -> null.also {
                require(suppliedReference == null) { "Baseline cases do not take a reference APK" }
            }
            else -> requireNotNull(suppliedReference) { "Combined cases require their baseline APK" }
        }
        require(reference == null || reference.isFile)
        require(!directory.exists()) { "Refusing to reuse an audit case directory: $directory" }
        check(File(UberLinkRescanDisableApkAudit::class.java.protectionDomain.codeSource.location.toURI())
            .canonicalFile == archive) { "Audit code is not loaded from the requested MPP" }
        val patchClass = Class.forName("app.template.patches.steamlink.binary.UberLinkRescanDisablePatchKt")
        check(File(patchClass.protectionDomain.codeSource.location.toURI())
            .canonicalFile == archive) { "Link-rescan-disable patch code is not loaded from the requested MPP" }

        val inputHash = input.sha256()
        if (selection != "reapply") check(inputHash == SOURCE_APK_SHA256) {
            "Input APK is not the exact verified original 2.0.23/5002363 release"
        }
        val archiveHash = archive.sha256()
        val referenceHash = reference?.sha256()
        val signatureVerified = if (selection == "reapply") null else {
            val result = ApkVerifier.Builder(input).build().verify()
            check(result.isVerified) { "Original APK signature verification failed: ${result.errors}" }
            true
        }
        val inputScene = ZipFile(input).use { zip ->
            zip.contentNames()
            zip.bytes(SCENE)
        }
        if (selection != "reapply") {
            check(inputScene.sha256() == STOCK_SCENE_SHA256) {
                "Input scene is not the exact pristine native library"
            }
            check(inputScene.size == SCENE_SIZE) { "Input scene is not the exact pristine native library size" }
            checkStockRescanSites(inputScene, "Input scene")
        } else rescanOffsets.forEach { offset ->
            check(inputScene.copyOfRange(offset, offset + 4).contentEquals(rescanNop)) {
                "Reapplication input is not a previous audited result at 0x${offset.toString(16)}"
            }
        }
        val bundle = galaxyXrRecommended5002363Patch
        check(bundle.dependencies.toSet() == setOf(xrGalaxyXrHighResolutionPatch, gxrModernTongueBridgePatch,
            microphoneInputPresetPatch, unrestrictedBatteryUsagePatch, hmdOnlyPatch, oledCalibrationPatch)) {
            "Recommended bundle no longer contains exactly its existing six patches"
        }
        check(!uberLinkRescanDisablePatch.default && uberLinkRescanDisablePatch.dependencies.isEmpty()) {
            "Link-rescan-disable patch must remain default-off with no dependencies"
        }
        val selected: Array<Patch<*>> = when (selection) {
            "baseline" -> arrayOf(bundle)
            "standalone", "reapply" -> arrayOf(uberLinkRescanDisablePatch)
            "bundle-first" -> arrayOf(bundle, uberLinkRescanDisablePatch)
            else -> arrayOf(uberLinkRescanDisablePatch, bundle)
        }
        val metadataGuard = rawResourcePatch(name = "Link-rescan-disable audit exact input guard", default = false) {
            execute {
                check(packageMetadata.versionName == version && packageMetadata.versionCode == code) {
                    "APK metadata ${packageMetadata.versionName}/${packageMetadata.versionCode} does not match $version/$code"
                }
            }
        }
        val stageExecution = mutableListOf<String?>()
        val stages = selected.map { selectedPatch ->
            rawResourcePatch(name = "Link-rescan-disable audit completed stage: ${selectedPatch.name}", default = false) {
                dependsOn(selectedPatch)
                execute { stageExecution += selectedPatch.name }
            }
        }
        val casePatch = rawResourcePatch(name = "Link-rescan-disable archive audit: $selection", default = false) {
            dependsOn(metadataGuard, *stages.toTypedArray())
        }
        check(directory.mkdirs())
        val isolated = File(directory, "isolated-input.apk")
        val output = File(directory, "result-unsigned.apk")
        input.copyTo(isolated)
        input.copyTo(output)
        val execution = mutableListOf<String?>()
        Patcher(PatcherConfig(isolated, File(directory, "temporary"))).use { patcher ->
            patcher += setOf(casePatch)
            val results = patcher().toList()
            execution += results.map { it.patch.name }
            val failures = results.filter { it.exception != null }
            check(failures.isEmpty()) {
                failures.joinToString("\n") { "${it.patch.name}: ${it.exception?.stackTraceToString()}" }
            }
            patcher.get().applyTo(output)
        }
        check(stageExecution == selected.map { it.name }) {
            "Requested native mutation stage order was not observed: $stageExecution"
        }
        val beforePatch = when (selection) {
            "standalone", "reapply" -> inputScene
            else -> recommendedScene(inputScene, version, code)
        }
        val expectedScene = if (baselineCase) beforePatch else patchUberLinkRescanDisable(beforePatch, version, code)
        val independentlyExpected = beforePatch.copyOf().also {
            if (!baselineCase) rescanOffsets.forEach { offset -> rescanNop.copyInto(it, offset) }
        }
        check(expectedScene.contentEquals(independentlyExpected)) {
            "Production helper result differs from the independent two-site NOP edit"
        }
        checkStockUdp(beforePatch)
        if (selection == "bundle-first" || selection == "guard-first") {
            checkStockRescanSites(beforePatch, "Recommended bundle output")
        }
        val actualScene = ZipFile(output).use { zip ->
            zip.contentNames()
            zip.bytes(SCENE)
        }
        check(actualScene.contentEquals(expectedScene)) { "Scene differs from the exact production helper result" }
        checkStockUdp(actualScene)
        if (selection == "standalone") check(actualScene.sha256() == PATCHED_SCENE_SHA256) {
            "Standalone scene is not the exact frozen patched library"
        }
        if (baselineCase) checkStockRescanSites(actualScene, "Bundle baseline")
        val comparedEntries = if (baselineCase) 0 else compareOnlySceneChange(
            requireNotNull(reference), output, version, code, selection == "reapply")
        check(beforePatch.size == actualScene.size) { "Scene size changed" }
        val changedOffsets = beforePatch.indices.filter { beforePatch[it] != actualScene[it] }
        if (!baselineCase) {
            val expectedOffsets = if (selection == "reapply") emptyList()
                else rescanOffsets.sorted().flatMap { (it until it + 4).toList() }
            check(changedOffsets == expectedOffsets) { "Unexpected link-rescan-disable mutation offsets: $changedOffsets" }
        }
        check(input.sha256() == inputHash) { "Input APK changed during audit" }
        check(archive.sha256() == archiveHash) { "Tested MPP changed during audit" }
        check(reference == null || reference.sha256() == referenceHash) { "Reference APK changed during audit" }
        val summary = linkedMapOf<String, Any?>(
            "status" to "passed", "version" to version, "version_code" to code,
            "selection" to selection, "input" to input.path, "input_sha256" to inputHash,
            "input_signature_verified" to signatureVerified,
            "signature_check_skipped_reason" to if (selection == "reapply") "Unsigned previous audit result" else null,
            "archive" to archive.path, "archive_sha256" to archiveHash,
            "code_and_resources" to "Packaged MPP first on classpath; patch code source verified",
            "output" to output.path, "output_sha256" to output.sha256(),
            "scene_sha256" to actualScene.sha256(), "reference" to reference?.path,
            "reference_sha256" to referenceHash, "unchanged_other_zip_entries" to comparedEntries,
            "signature_entries_excluded" to true,
            "guard_changed_offsets" to changedOffsets.map { "0x${it.toString(16)}" },
            "reapplication_all_content_identical" to (selection == "reapply"),
            "native_mutation_stage_order" to stageExecution, "patch_results" to execution,
            "stock_udp_receive_buffer_preserved" to true,
            "stock_udp_receive_buffer_offset" to "0x1757a8",
            "independent_guard_expected_bytes" to "1f2003d5",
            "independent_rescan_nop_offsets" to rescanOffsets.map { "0x${it.toString(16)}" },
            "verified_source_apk_sha256" to SOURCE_APK_SHA256,
            "stock_scene_sha256" to STOCK_SCENE_SHA256,
            "expected_patched_scene_sha256" to PATCHED_SCENE_SHA256,
            "signed_or_installed" to false,
        )
        File(directory, "audit.json").writeText(GsonBuilder().setPrettyPrinting().create().toJson(summary) + "\n")
        println("PASS $version/$code $selection: $comparedEntries other ZIP entries unchanged; $output")
    }

    private val rescanNop = byteArrayOf(0x1f, 0x20, 0x03, 0xd5.toByte())
    private val rescanOffsets = intArrayOf(0x174c74, 0x16fdf8)
    private val stockRescanBytes = arrayOf(
        byteArrayOf(0x17, 0xa3.toByte(), 0x02, 0x94.toByte()),
        byteArrayOf(0xb6.toByte(), 0xb6.toByte(), 0x02, 0x94.toByte()),
    )
    private val stockUdpBytes = byteArrayOf(0x08, 0x02, 0xa0.toByte(), 0x52)

    private fun checkStockUdp(bytes: ByteArray) {
        check(bytes.copyOfRange(STOCK_UDP_OFFSET, STOCK_UDP_OFFSET + 4).contentEquals(stockUdpBytes)) {
            "Link-rescan-disable audit requires the stock 1 MiB UDP receive-buffer request at 0x1757a8"
        }
    }

    private fun checkStockRescanSites(bytes: ByteArray, context: String) {
        rescanOffsets.forEachIndexed { index, offset ->
            check(bytes.copyOfRange(offset, offset + 4).contentEquals(stockRescanBytes[index])) {
                "$context no longer contains the stock rescan call at 0x${offset.toString(16)}"
            }
        }
    }

    private fun recommendedScene(input: ByteArray, version: String, code: String): ByteArray {
        var bytes = patchModernTongueTransport(input, version, code)
        bytes = patchNativeMicrophonePreset(bytes, "voice-recognition", version, code)
        bytes = patchVisualDelay(bytes, 60, version, code)
        paddedVideoShader(1.20f, 1.45f, VideoOutputPrecision.SRGB8_HIGHP).copyInto(bytes, findVideoShader(bytes))
        return setProjectionSwapchainFormat(bytes, VideoOutputPrecision.SRGB8_HIGHP, version, code)
    }

    private fun compareOnlySceneChange(reference: File, output: File, version: String, code: String,
        reapply: Boolean): Int {
        ZipFile(reference).use { original -> ZipFile(output).use { patched ->
            val originalNames = original.contentNames()
            val outputNames = patched.contentNames()
            check(originalNames == outputNames) {
                "Unexpected APK entries: added=${outputNames - originalNames}; removed=${originalNames - outputNames}"
            }
            val originalScene = original.bytes(SCENE)
            val patchedScene = patched.bytes(SCENE)
            check(patchedScene.contentEquals(patchUberLinkRescanDisable(originalScene, version, code))) {
                "Scene differs from link-rescan-disable-only change to reference"
            }
            val independent = originalScene.copyOf()
            rescanOffsets.forEach { offset -> rescanNop.copyInto(independent, offset) }
            check(patchedScene.contentEquals(independent)) {
                "Reference comparison exceeds the independent two-site NOP edit"
            }
            checkStockUdp(originalScene)
            checkStockUdp(patchedScene)
            if (reapply) check(patchedScene.contentEquals(originalScene)) { "Reapplication changed the scene" }
            originalNames.filterNot { it == SCENE }.forEach { name ->
                check(original.bytes(name).contentEquals(patched.bytes(name))) { "Unrelated ZIP entry changed: $name" }
            }
            return originalNames.size - 1
        } }
    }

    private fun ZipFile.contentNames(): Set<String> {
        val names = entries().asSequence().filterNot { it.isDirectory }.map { it.name }.toList()
        check(names.size == names.toSet().size) { "Duplicate ZIP entry names" }
        return names.filterNot { signature.matches(it) }.toSet()
    }

    private fun ZipFile.bytes(name: String): ByteArray = getInputStream(requireNotNull(getEntry(name)) {
        "Missing ZIP entry: $name"
    }).use { it.readBytes() }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this).joinToString("") { "%02x".format(it) }

    private fun File.sha256(): String = inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(1024 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}
