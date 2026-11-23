package com.puzzel.lab.marbelshort.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class DailyRemindersTest {
    private fun at(day: Int, hour: Int, minute: Int = 0, second: Int = 0): Long = Calendar.getInstance().apply {
        clear(); set(2026, Calendar.NOVEMBER, day, hour, minute, second)
    }.timeInMillis

    @Test fun beforeTheHour_firesToday() {
        assertEquals(at(20, 9), DailyReminders.nextTrigger(9, at(20, 8, 15)))
        assertEquals(at(20, 19), DailyReminders.nextTrigger(19, at(20, 9, 0, 5)))
    }

    @Test fun exactlyOnTheHour_firesTomorrow() {
        assertEquals(at(21, 9), DailyReminders.nextTrigger(9, at(20, 9)))
    }

    @Test fun afterTheHour_firesTomorrow() {
        assertEquals(at(21, 19), DailyReminders.nextTrigger(19, at(20, 22, 30)))
        assertEquals(at(21, 9), DailyReminders.nextTrigger(9, at(20, 9, 40)))
    }

    @Test fun rollsOverMonthEnd() {
        val endOfMonth = Calendar.getInstance().apply { clear(); set(2026, Calendar.NOVEMBER, 30, 23, 0) }.timeInMillis
        val dec1 = Calendar.getInstance().apply { clear(); set(2026, Calendar.DECEMBER, 1, 9, 0) }.timeInMillis
        assertEquals(dec1, DailyReminders.nextTrigger(9, endOfMonth))
    }
}
