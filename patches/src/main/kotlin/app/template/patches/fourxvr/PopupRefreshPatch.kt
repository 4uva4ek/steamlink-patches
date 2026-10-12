package app.template.patches.fourxvr

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.template.patches.shared.Constants.COMPATIBILITIES_FOUR_XVR
import app.template.patches.shared.Constants.isFourXvr20022Build
import app.template.patches.shared.PatchCategories
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * G10 — popup refresh for exact 4XVR 2.0.22/20022.
 *
 * The stock startup flow evaluates the storage prompt, the release notes and the tutorial exactly
 * once per process: Main4XActivity.lambda$InitOerator$46 checks Environment.isExternalStorageManager
 * first, shows tool/V4PermissionsUI and returns, and V4PermissionsUI.lambda$onCreate$0 hides the
 * window before opening the All-files-access settings page (request 0x59E2) - Main4XActivity
 * .onActivityResult has no 0x59E2 branch and no lifecycle callback re-checks, so access granted
 * while the prompt is up only clears at the next process start. The release-notes window is shown
 * after SaveVersion() already recorded the installed build, so its saved version covers the running
 * build while it is up. Re-check both on resume and after any activity result.
 */

private const val MAIN_ACTIVITY = "Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;"
private const val POPUP_REFRESH = "Lcn/vr4p/patch/PopupRefresh;"

private fun MutableMethod.alreadyCallsOnHostResume(): Boolean =
    implementation!!.instructions.firstOrNull()
        .let { (it as? ReferenceInstruction)?.reference as? MethodReference }
        ?.let { reference -> reference.definingClass == POPUP_REFRESH && reference.name == "onHostResume" } == true

@Suppress("unused")
internal val fourXvrPopupRefreshPatch = bytecodePatch(
    name = "Popup refresh on resume (4XVR)",
    description = "Re-checks the 4XVR startup popups after the activity resumes or returns from " +
        "system settings: hides the All-files-access prompt once external storage access is " +
        "granted (the stock flow never re-checks the result of its 0x59E2 request) and hides the " +
        "release-notes window when its saved version already covers the installed build, for " +
        "exact 4XVR 2.0.22 build 20022. Headset validation pending. " +
        FOUR_XVR_UPSTREAM_CREDIT,
    default = true,
) {
    category(PatchCategories.STARTUP_AND_PERMISSIONS)
    compatibleWith(*COMPATIBILITIES_FOUR_XVR.toTypedArray())
    extendWith("extensions/fourxvr-extension.mpe")

    execute {
        if (!isFourXvr20022Build(packageMetadata.versionName, packageMetadata.versionCode)) return@execute

        val mainActivity = mutableClassDefBy(MAIN_ACTIVITY)
        val onResume = mainActivity.methods.first { it.name == "onResume" && it.parameterTypes.isEmpty() }
        if (!onResume.alreadyCallsOnHostResume()) {
            onResume.addInstruction(
                0,
                invokeStatic35c(
                    POPUP_REFRESH,
                    "onHostResume",
                    listOf(MAIN_ACTIVITY),
                    "V",
                    listOf(onResume.pRegister(0)),
                ),
            )
        }

        val onActivityResult = mainActivity.methods.first {
            it.name == "onActivityResult" && it.parameterTypes.size == 3
        }
        if (!onActivityResult.alreadyCallsOnHostResume()) {
            onActivityResult.addInstruction(
                0,
                invokeStatic35c(
                    POPUP_REFRESH,
                    "onHostResume",
                    listOf(MAIN_ACTIVITY),
                    "V",
                    listOf(onActivityResult.pRegister(0)),
                ),
            )
        }
    }
}
