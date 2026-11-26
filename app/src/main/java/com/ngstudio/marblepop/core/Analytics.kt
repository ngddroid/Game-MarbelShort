package com.ngstudio.marblepop.core

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ngstudio.marblepop.BuildConfig

/**
 * One place for Firebase Analytics events and Crashlytics context, so screens never touch the SDKs directly.
 *
 * Calls made before [init] (e.g. from unit tests) are ignored.
 */
object Analytics {
    private var fa: FirebaseAnalytics? = null
    private var crash: FirebaseCrashlytics? = null

    fun init(context: Context) {
        fa = FirebaseAnalytics.getInstance(context)
        crash = FirebaseCrashlytics.getInstance().apply {
            setCustomKey("debug_build", BuildConfig.DEBUG)
        }
        setCollectionEnabled(GameRepository.prefs(context).getBoolean(GameRepository.KEY_SHARE_DATA, true))
    }

    /** The player's "Share anonymous stats" choice; both SDKs also remember it themselves across launches. */
    fun setCollectionEnabled(enabled: Boolean) {
        fa?.setAnalyticsCollectionEnabled(enabled)
        crash?.isCrashlyticsCollectionEnabled = enabled
    }

    // ---------------------------------------------------------------- core

    private fun log(name: String, build: Bundle.() -> Unit = {}) {
        fa?.logEvent(name, Bundle().apply(build))
    }

    /** Breadcrumb that shows up in the Crashlytics "Logs" tab of the next crash. */
    fun breadcrumb(msg: String) { crash?.log(msg) }

    /** Report a caught exception as a non-fatal issue. */
    fun recordError(t: Throwable) { crash?.recordException(t) }

    // ---------------------------------------------------------------- navigation

    fun screen(name: String) {
        log(FirebaseAnalytics.Event.SCREEN_VIEW) {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        }
        crash?.setCustomKey("screen", name)
        breadcrumb("screen: $name")
    }

    fun tutorialBegin() = log(FirebaseAnalytics.Event.TUTORIAL_BEGIN)
    fun tutorialComplete() = log(FirebaseAnalytics.Event.TUTORIAL_COMPLETE)

    // ---------------------------------------------------------------- levels

    /** Highest unlocked level, as a user property for segmenting players by progress. */
    fun progress(level: Int) {
        fa?.setUserProperty("max_level", level.toString())
        crash?.setCustomKey("max_level", level)
    }

    fun levelStart(level: Int, daily: Boolean, boss: Boolean) {
        log(FirebaseAnalytics.Event.LEVEL_START) {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, levelName(level, daily))
            putLong(FirebaseAnalytics.Param.LEVEL, level.toLong())
            putString("mode", if (daily) "daily" else "map")
            putLong("boss", if (boss) 1 else 0)
        }
        crash?.setCustomKey("level", level)
        crash?.setCustomKey("daily", daily)
        breadcrumb("level start: ${levelName(level, daily)}")
    }

    fun levelWin(level: Int, daily: Boolean, stars: Int, coins: Int, maxCombo: Int, boostersUsed: Int) {
        log(FirebaseAnalytics.Event.LEVEL_END) {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, levelName(level, daily))
            putLong(FirebaseAnalytics.Param.LEVEL, level.toLong())
            putLong(FirebaseAnalytics.Param.SUCCESS, 1)
            putLong("stars", stars.toLong())
            putLong("coins", coins.toLong())
            putLong("max_combo", maxCombo.toLong())
            putLong("boosters_used", boostersUsed.toLong())
        }
        breadcrumb("level win: ${levelName(level, daily)} stars=$stars")
    }

    fun levelFail(level: Int, daily: Boolean, reason: String, boostersUsed: Int) {
        log(FirebaseAnalytics.Event.LEVEL_END) {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, levelName(level, daily))
            putLong(FirebaseAnalytics.Param.LEVEL, level.toLong())
            putLong(FirebaseAnalytics.Param.SUCCESS, 0)
            putString("fail_reason", reason)
            putLong("boosters_used", boostersUsed.toLong())
        }
        breadcrumb("level fail: ${levelName(level, daily)} reason=$reason")
    }

    fun levelRetry(level: Int, from: String) = log("level_retry") {
        putLong(FirebaseAnalytics.Param.LEVEL, level.toLong()); putString("from", from)
    }

    fun levelContinue(level: Int, price: Int) {
        log("level_continue") { putLong(FirebaseAnalytics.Param.LEVEL, level.toLong()) }
        spend("continue", price)
    }

    fun boosterUsed(booster: String, level: Int) = log("booster_used") {
        putString("booster", booster); putLong(FirebaseAnalytics.Param.LEVEL, level.toLong())
    }

    // ---------------------------------------------------------------- economy & rewards

    fun spend(item: String, coins: Int) = log(FirebaseAnalytics.Event.SPEND_VIRTUAL_CURRENCY) {
        putString(FirebaseAnalytics.Param.ITEM_NAME, item)
        putString(FirebaseAnalytics.Param.VIRTUAL_CURRENCY_NAME, "coins")
        putLong(FirebaseAnalytics.Param.VALUE, coins.toLong())
    }

    fun earn(source: String, coins: Int) = log(FirebaseAnalytics.Event.EARN_VIRTUAL_CURRENCY) {
        putString("source", source)
        putString(FirebaseAnalytics.Param.VIRTUAL_CURRENCY_NAME, "coins")
        putLong(FirebaseAnalytics.Param.VALUE, coins.toLong())
    }

    fun reward(kind: String, value: Int = 0) = log("reward_claimed") {
        putString("kind", kind); putLong(FirebaseAnalytics.Param.VALUE, value.toLong())
    }

    fun rewardedAd(placement: AdPlacement, rewarded: Boolean) = log("rewarded_ad") {
        putString("placement", placement.name.lowercase())
        putLong("rewarded", if (rewarded) 1 else 0)
    }

    // ---------------------------------------------------------------- notifications & updates

    fun reminderShown(slot: String) = log("reminder_shown") { putString("slot", slot) }

    fun pushReceived(foreground: Boolean) = log("push_received") { putLong("foreground", if (foreground) 1 else 0) }

    /** A local reminder was tapped (FCM logs its own notification_open for push messages). */
    fun notificationOpened(source: String) = log("notification_opened") { putString("source", source) }

    fun settingChanged(kind: String, enabled: Boolean) = log("setting_changed") {
        putString("kind", kind); putLong("enabled", if (enabled) 1 else 0)
    }

    fun notificationPermission(granted: Boolean) = log("notification_permission") { putLong("granted", if (granted) 1 else 0) }

    fun inAppUpdate(type: String, result: String) = log("in_app_update") { putString("type", type); putString("result", result) }

    private fun levelName(level: Int, daily: Boolean) = if (daily) "daily_$level" else "level_$level"
}
