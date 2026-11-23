package com.puzzel.lab.marbelshort

import android.app.Application
import com.puzzel.lab.marbelshort.core.Analytics
import com.puzzel.lab.marbelshort.core.DailyReminders
import com.puzzel.lab.marbelshort.core.Notifications

class MarbleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase itself is initialised automatically from google-services.json; this wires up our wrapper.
        Analytics.init(this)
        Notifications.createChannels(this)
        // Re-arm the 9 AM / 7 PM reminders (alarms are lost on force-stop; scheduling again just replaces them).
        DailyReminders.sync(this)
    }
}
