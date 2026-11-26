package com.ngstudio.marblepop.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ngstudio.marblepop.level.LevelGenerator
import java.util.Calendar

/**
 * Two local reminders a day, at 9 AM and 7 PM device time, each with a rotating game-themed message.
 * There is no in-game switch: players turn them off with the "Daily reminders" channel in Android's settings.
 *
 * Uses AlarmManager without the exact-alarm permission (Play only allows that for alarm/calendar apps), so on
 * Android 12+ a reminder can arrive a few minutes after the hour. Every fired alarm schedules the next day's,
 * and [RescheduleReceiver] restores them after a reboot, an app update or a clock/time-zone change.
 */
object DailyReminders {
    enum class Slot(val hour: Int, val notificationId: Int, val tag: String) {
        MORNING(9, Notifications.ID_MORNING, "morning"),
        EVENING(19, Notifications.ID_EVENING, "evening"),
    }

    private const val ACTION_FIRE = "com.ngstudio.marblepop.action.DAILY_REMINDER"
    private const val EXTRA_SLOT = "slot"

    /** (Re)schedules both reminders; scheduling again just replaces the alarm. Safe to call any time. */
    fun sync(context: Context) {
        Slot.entries.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, slot: Slot) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val at = nextTrigger(slot.hour, System.currentTimeMillis())
        val pi = pendingIntent(context, slot)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun pendingIntent(context: Context, slot: Slot): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_SLOT, slot.ordinal)
        return PendingIntent.getBroadcast(context, slot.notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** The next time the clock reads [hour]:00, strictly after [now]. */
    internal fun nextTrigger(hour: Int, now: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (c.timeInMillis <= now) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    internal fun slotFrom(intent: Intent): Slot? =
        if (intent.action == ACTION_FIRE) Slot.entries.getOrNull(intent.getIntExtra(EXTRA_SLOT, -1)) else null

    /** Posts [slot]'s reminder now (also used by the debug tools). False when notifications are blocked. */
    fun show(context: Context, slot: Slot): Boolean {
        val (title, body) = message(context, slot)
        val shown = Notifications.show(context, slot.notificationId, Notifications.CHANNEL_REMINDERS, title, body, "reminder_${slot.tag}")
        if (shown) Analytics.reminderShown(slot.tag)
        return shown
    }

    // ------------------------------------------------------------------ messages

    /** Picks today's message for [slot]: general lines plus ones about the player's actual progress, rotated daily. */
    fun message(context: Context, slot: Slot): Pair<String, String> {
        val repo = GameRepository(context)
        val level = repo.level.intValue
        val pool = ArrayList<Pair<String, String>>()
        if (slot == Slot.MORNING) {
            pool += "Good morning, Marble Master! ☀️" to "Level $level is warmed up and waiting. Pop a few blocks with your coffee!"
            pool += "The factory belt is rolling 🏭" to "Fresh marbles just arrived. Can you sort them all into the right cups?"
            pool += "Rise, shine, sort & pop! 🔮" to "Start your day with a quick, satisfying round of marble sorting."
            pool += "Morning brain workout 🧠" to "One quick level is the perfect warm-up. Level $level is ready for you."
            pool += "Three stars before lunch? ⭐" to "Clear Level $level with a big combo and grab every star."
            if (repo.canSpin) pool += "Your free Lucky Spin is ready 🎡" to "Spin the wheel today for free coins and boosters!"
            if (repo.canClaimLogin) pool += "Day ${repo.loginIndex + 1} reward is waiting 🎁" to "Open the game to collect it and keep your login streak going."
        } else {
            pool += "Time to unwind 🌙" to "Relax with a few rounds of marble sorting. Level $level is waiting."
            pool += "Your marbles miss you 💜" to "The cups are empty and the belt is quiet. Come back and fill them up!"
            pool += "Evening combo challenge 🔥" to "Can you chain a 5x combo tonight? Jump into Level $level."
            pool += "One more level before bed? 😴" to "Just one… okay, maybe two. Level $level is ready."
            pool += "The factory is still open 🏭" to "Pop blocks, sort marbles and earn coins before the day ends."
            if (!repo.dailyChallengeDone) pool += "Daily Challenge closes at midnight 🏆" to "Finish today's challenge to earn double coins!"
        }
        if (level < LevelGenerator.MAX_LEVEL && LevelGenerator.isBoss(level)) {
            pool += "A Boss Level awaits 👑" to "Level $level is a boss level. Bring your boosters and show it who's boss!"
        }
        val chestAt = ((level - 1) / 10 + 1) * 10
        val toChest = chestAt - level + 1
        if (toChest in 1..3) {
            pool += "Treasure chest ahead 🧰" to
                "Beat $toChest more level${if (toChest > 1) "s" else ""} to unlock the chest at Level $chestAt."
        }
        val day = (System.currentTimeMillis() / 86_400_000L).toInt()
        return pool[day % pool.size]
    }
}

/** Fires at 9 AM / 7 PM: schedules tomorrow's alarm, then shows the reminder unless the game is already open. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slot = DailyReminders.slotFrom(intent) ?: return
        DailyReminders.schedule(context, slot)
        if (Notifications.appInForeground) return
        DailyReminders.show(context, slot)
    }
}

/** Alarms are wiped on reboot and can drift after clock changes, so put them back. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> DailyReminders.sync(context)
        }
    }
}
