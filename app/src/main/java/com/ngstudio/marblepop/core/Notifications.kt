package com.ngstudio.marblepop.core

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ngstudio.marblepop.MainActivity
import com.ngstudio.marblepop.R

/** Notification channels, posting, and the Android 13+ permission check, shared by reminders and push. */
object Notifications {
    /** Daily 9 AM / 7 PM reminders generated on the device. */
    const val CHANNEL_REMINDERS = "daily_reminders"
    /** Messages sent from the Firebase console (events, new levels, news). Also the FCM default channel. */
    const val CHANNEL_NEWS = "news_events"

    /** Intent extra on the launch intent telling MainActivity which notification was tapped. */
    const val EXTRA_SOURCE = "notification_source"

    const val ID_MORNING = 1001
    const val ID_EVENING = 1002
    const val ID_PUSH = 2001

    /** True while MainActivity is resumed; reminders are skipped while the player is already in the game. */
    @Volatile var appInForeground = false

    private const val ACCENT = 0xFFFF4C97.toInt()

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_reminders_desc) },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_NEWS, context.getString(R.string.channel_news), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_news_desc) },
        )
    }

    /** Android 13+ needs the POST_NOTIFICATIONS runtime permission; older versions only need notifications switched on. */
    fun needsRuntimePermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    fun canPost(context: Context): Boolean =
        !needsRuntimePermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** The system screen where the player can switch this app's notifications back on. */
    fun systemSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    @SuppressLint("MissingPermission") // checked by canPost()
    fun show(context: Context, id: Int, channel: String, title: String, body: String, source: String): Boolean {
        if (!canPost(context)) return false
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_SOURCE, source)
        val pi = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (channel == CHANNEL_REMINDERS) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
        return true
    }
}
