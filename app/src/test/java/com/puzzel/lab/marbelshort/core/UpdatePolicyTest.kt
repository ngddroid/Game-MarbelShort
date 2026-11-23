package com.puzzel.lab.marbelshort.core

import android.app.Activity
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.puzzel.lab.marbelshort.core.UpdatePolicy.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {
    private fun launch(
        availability: Int = UpdateAvailability.UPDATE_AVAILABLE,
        installStatus: Int = InstallStatus.UNKNOWN,
        priority: Int = 0,
        immediate: Boolean = true,
        flexible: Boolean = true,
        declined: Boolean = false,
    ) = UpdatePolicy.onLaunch(availability, installStatus, priority, immediate, flexible, declined)

    @Test fun noUpdate_doesNothing() {
        assertEquals(Action.NONE, launch(availability = UpdateAvailability.UPDATE_NOT_AVAILABLE))
        assertEquals(Action.NONE, launch(availability = UpdateAvailability.UNKNOWN))
    }

    @Test fun normalUpdate_isFlexible() {
        assertEquals(Action.FLEXIBLE, launch(priority = 0))
        assertEquals(Action.FLEXIBLE, launch(priority = 3))
    }

    @Test fun highPriority_isImmediate() {
        assertEquals(Action.IMMEDIATE, launch(priority = 4))
        assertEquals(Action.IMMEDIATE, launch(priority = 5))
    }

    @Test fun highPriority_fallsBackToFlexible_whenImmediateNotAllowed() {
        assertEquals(Action.FLEXIBLE, launch(priority = 5, immediate = false))
    }

    @Test fun highPriority_ignoresDeclineCooldown() {
        assertEquals(Action.IMMEDIATE, launch(priority = 5, declined = true))
    }

    @Test fun declinedFlexible_isNotAskedAgain() {
        assertEquals(Action.NONE, launch(declined = true))
    }

    @Test fun downloadAlreadyUnderway_isNotAskedAgain() {
        for (s in listOf(InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.DOWNLOADED, InstallStatus.INSTALLING)) {
            assertEquals("status $s", Action.NONE, launch(installStatus = s))
            assertEquals("status $s", Action.NONE, launch(installStatus = s, priority = 5))
        }
    }

    @Test fun failedOrCanceledDownload_canBeOfferedAgain() {
        assertEquals(Action.FLEXIBLE, launch(installStatus = InstallStatus.FAILED))
        assertEquals(Action.FLEXIBLE, launch(installStatus = InstallStatus.CANCELED))
    }

    @Test fun nothingAllowed_doesNothing() {
        assertEquals(Action.NONE, launch(immediate = false, flexible = false, priority = 5))
    }

    @Test fun interruptedImmediate_resumes_onlyWhenItsScreenIsNotUp() {
        assertTrue(UpdatePolicy.shouldResumeImmediate(UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS, flowShowing = false))
        assertFalse(UpdatePolicy.shouldResumeImmediate(UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS, flowShowing = true))
        assertFalse(UpdatePolicy.shouldResumeImmediate(UpdateAvailability.UPDATE_AVAILABLE, flowShowing = false))
    }

    @Test fun onlyFlexibleNoThanks_startsCooldown() {
        assertTrue(UpdatePolicy.countsAsDecline(AppUpdateType.FLEXIBLE, Activity.RESULT_CANCELED))
        assertFalse(UpdatePolicy.countsAsDecline(AppUpdateType.FLEXIBLE, Activity.RESULT_OK))
        assertFalse(UpdatePolicy.countsAsDecline(AppUpdateType.FLEXIBLE, ActivityResult.RESULT_IN_APP_UPDATE_FAILED))
        assertFalse(UpdatePolicy.countsAsDecline(AppUpdateType.IMMEDIATE, Activity.RESULT_CANCELED))
    }

    @Test fun resultNames() {
        assertEquals("accepted", UpdatePolicy.resultName(Activity.RESULT_OK))
        assertEquals("declined", UpdatePolicy.resultName(Activity.RESULT_CANCELED))
        assertEquals("failed", UpdatePolicy.resultName(ActivityResult.RESULT_IN_APP_UPDATE_FAILED))
    }
}
