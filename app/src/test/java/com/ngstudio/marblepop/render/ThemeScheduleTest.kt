package com.ngstudio.marblepop.render

import com.ngstudio.marblepop.level.LevelGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeScheduleTest {
    private val themes = Themes.all

    @Test
    fun scheduleIsSortedAndCoversAllLevels() {
        assertTrue("at least 13 worlds", themes.size >= 13)
        assertEquals(1, themes.first().unlockLevel)
        for (i in 1 until themes.size) {
            assertTrue("${themes[i].name} unlocks after ${themes[i - 1].name}", themes[i].unlockLevel > themes[i - 1].unlockLevel)
        }
        assertTrue(themes.last().unlockLevel <= LevelGenerator.MAX_LEVEL)
        assertEquals(themes.size, themes.map { it.name }.toSet().size)
    }

    @Test
    fun levelUsesHighestThresholdAtOrBelowIt() {
        assertEquals(0, Themes.indexForLevel(1))
        assertEquals(0, Themes.indexForLevel(39))
        assertEquals(1, Themes.indexForLevel(40))
        for ((i, t) in themes.withIndex()) {
            assertEquals(i, Themes.indexForLevel(t.unlockLevel))
            if (i > 0) assertEquals(i - 1, Themes.indexForLevel(t.unlockLevel - 1))
            assertEquals(i, LevelGenerator.themeFor(t.unlockLevel))
            assertEquals(i, Themes.startingAt(t.unlockLevel))
        }
        assertEquals(themes.size - 1, Themes.indexForLevel(LevelGenerator.MAX_LEVEL))
        assertEquals(-1, Themes.startingAt(41))
    }

    @Test
    fun everyThemeHasAFullPalette() {
        for (t in themes) {
            assertEquals("${t.name} sky", 6, t.sky.size)
            assertTrue("${t.name} scenery", t.scenery in 0..5)
            val opaque = listOf(t.bgTop, t.bgBottom, t.wall, t.wallDark, t.wallLight, t.glassTop, t.glassBottom, t.interior, t.slot,
                t.funnel, t.funnelDark, t.dish, t.dishDark, t.groove, t.rail, t.accent) + t.sky.toList()
            for (c in opaque) assertEquals("${t.name} colour ${Integer.toHexString(c)} is opaque", 0xFF, c ushr 24)
        }
    }
}
