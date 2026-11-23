package com.puzzel.lab.marbelshort.core

import android.content.Context
import android.util.Log
import com.puzzel.lab.marbelshort.BuildConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging.
 *
 * Send campaigns from Firebase console → Messaging, targeting the app or the [TOPIC_ALL] topic.
 * While the game is in the background, Android shows "notification" messages by itself (channel, icon and colour
 * come from the manifest meta-data); while it is open, [MarbleMessagingService] shows them.
 */
object Push {
    const val TOPIC_ALL = "all"
    private const val LOG_TAG = "MarblePush"

    /** Applies the player's "News & Events" choice: subscribe and keep a token, or drop both. */
    fun sync(context: Context) {
        val on = GameRepository.prefs(context).getBoolean(GameRepository.KEY_PUSH, true)
        val fm = runCatching { FirebaseMessaging.getInstance() }.getOrNull() ?: return
        if (on) {
            fm.isAutoInitEnabled = true
            fm.subscribeToTopic(TOPIC_ALL)
            if (BuildConfig.DEBUG) {
                // Debug only: paste this into Firebase console → Messaging → "Send test message" to push to this device.
                fm.token.addOnSuccessListener { Log.d(LOG_TAG, "FCM token: $it") }
                    .addOnFailureListener { Log.w(LOG_TAG, "FCM token unavailable", it) }
            }
        } else {
            fm.unsubscribeFromTopic(TOPIC_ALL)
            fm.isAutoInitEnabled = false
            fm.deleteToken()
        }
    }
}

class MarbleMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        if (!GameRepository.prefs(this).getBoolean(GameRepository.KEY_PUSH, true)) return
        // "notification" messages carry title/body there; data-only messages may send "title"/"body" keys instead.
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val channel = message.notification?.channelId ?: Notifications.CHANNEL_NEWS
        Notifications.show(this, Notifications.ID_PUSH, channel, title, body, "push")
        Analytics.pushReceived(foreground = Notifications.appInForeground)
    }

    override fun onNewToken(token: String) {
        // No game server stores tokens; campaigns go to the app or the topic. Re-subscribe so a fresh token joins it.
        Push.sync(this)
    }
}
