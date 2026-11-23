package com.puzzel.lab.marbelshort.core

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Google Play in-app updates.
 *
 * - Update priority 4-5 (set when releasing through the Play Developer API) → IMMEDIATE: Play's full-screen flow.
 *   If the player backs out, they are asked again on the next launch.
 * - Anything else → FLEXIBLE: Play asks, the update downloads in the background, then [readyToInstall] turns true
 *   and the game shows its own "Update Ready!" popup. If the player declines, that version is not offered again
 *   for [UpdatePolicy.DECLINE_COOLDOWN_MS].
 *
 * Only works for builds installed from Google Play (internal app sharing / testing tracks included); for any other
 * install the check fails quietly. Create it in `onCreate` (it registers an activity-result launcher).
 */
class InAppUpdater(private val activity: ComponentActivity) {
    private val manager = AppUpdateManagerFactory.create(activity)
    private val prefs = activity.getSharedPreferences("in_app_updates", Activity.MODE_PRIVATE)

    /** A flexible update finished downloading and is waiting for [completeUpdate]. */
    val readyToInstall = mutableStateOf(false)

    /** True while Play's own update screen is showing, so we never launch it twice. */
    private var flowShowing = false
    private var pendingType = AppUpdateType.FLEXIBLE
    private var pendingVersion = 0

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        flowShowing = false
        val type = if (pendingType == AppUpdateType.IMMEDIATE) "immediate" else "flexible"
        Analytics.inAppUpdate(type, UpdatePolicy.resultName(result.resultCode))
        if (UpdatePolicy.countsAsDecline(pendingType, result.resultCode)) {
            prefs.edit().putInt(KEY_DECLINED_VERSION, pendingVersion).putLong(KEY_DECLINED_AT, System.currentTimeMillis()).apply()
        }
    }

    private val listener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> readyToInstall.value = true
            InstallStatus.FAILED -> Analytics.inAppUpdate("flexible", "download_failed")
        }
    }

    init { manager.registerListener(listener) }

    /** Call once per launch: asks Play whether an update exists and starts the right flow. */
    fun checkForUpdate() {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) readyToInstall.value = true
                val action = UpdatePolicy.onLaunch(
                    availability = info.updateAvailability(),
                    installStatus = info.installStatus(),
                    priority = info.updatePriority(),
                    immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
                    flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
                    declinedRecently = recentlyDeclined(info.availableVersionCode()),
                )
                when (action) {
                    UpdatePolicy.Action.IMMEDIATE -> launch(info, AppUpdateType.IMMEDIATE)
                    UpdatePolicy.Action.FLEXIBLE -> launch(info, AppUpdateType.FLEXIBLE)
                    UpdatePolicy.Action.NONE -> {}
                }
            }
            .addOnFailureListener { Analytics.breadcrumb("in-app update check failed: ${it.message}") }
    }

    /** Call from onResume: shows the restart prompt for a finished download and resumes an interrupted immediate update. */
    fun onResume() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) readyToInstall.value = true
            if (UpdatePolicy.shouldResumeImmediate(info.updateAvailability(), flowShowing)) launch(info, AppUpdateType.IMMEDIATE)
        }
    }

    /** Installs a downloaded flexible update; Play restarts the game into the new version. */
    fun completeUpdate() {
        readyToInstall.value = false
        Analytics.inAppUpdate("flexible", "installed")
        manager.completeUpdate()
    }

    fun release() = manager.unregisterListener(listener)

    private fun launch(info: AppUpdateInfo, type: Int) {
        if (flowShowing) return
        pendingType = type
        pendingVersion = info.availableVersionCode()
        val started = runCatching { manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build()) }
            .onFailure { Analytics.recordError(it) }
            .getOrDefault(false)
        flowShowing = started
    }

    private fun recentlyDeclined(version: Int): Boolean =
        prefs.getInt(KEY_DECLINED_VERSION, -1) == version &&
            System.currentTimeMillis() - prefs.getLong(KEY_DECLINED_AT, 0) < UpdatePolicy.DECLINE_COOLDOWN_MS

    private companion object {
        const val KEY_DECLINED_VERSION = "declined_version"
        const val KEY_DECLINED_AT = "declined_at"
    }
}

/** The update decisions, kept free of Android so they can be unit-tested. */
internal object UpdatePolicy {
    enum class Action { NONE, IMMEDIATE, FLEXIBLE }

    const val DECLINE_COOLDOWN_MS = 3L * 24 * 60 * 60 * 1000

    /** Install states where an update is already on its way, so Play must not be asked again. */
    private val BUSY = setOf(InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.DOWNLOADED, InstallStatus.INSTALLING)

    fun onLaunch(
        availability: Int, installStatus: Int, priority: Int,
        immediateAllowed: Boolean, flexibleAllowed: Boolean, declinedRecently: Boolean,
    ): Action = when {
        availability != UpdateAvailability.UPDATE_AVAILABLE -> Action.NONE
        installStatus in BUSY -> Action.NONE
        priority >= 4 && immediateAllowed -> Action.IMMEDIATE
        flexibleAllowed && !declinedRecently -> Action.FLEXIBLE
        else -> Action.NONE
    }

    /** An immediate update the player left half-way (e.g. app was backgrounded) is resumed, unless its screen is up. */
    fun shouldResumeImmediate(availability: Int, flowShowing: Boolean): Boolean =
        availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS && !flowShowing

    /** Only a real "No thanks" on a flexible prompt starts the cooldown; Play errors don't. */
    fun countsAsDecline(type: Int, resultCode: Int): Boolean =
        type == AppUpdateType.FLEXIBLE && resultCode == Activity.RESULT_CANCELED

    fun resultName(resultCode: Int): String = when (resultCode) {
        Activity.RESULT_OK -> "accepted"
        Activity.RESULT_CANCELED -> "declined"
        ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> "failed"
        else -> "result_$resultCode"
    }
}
