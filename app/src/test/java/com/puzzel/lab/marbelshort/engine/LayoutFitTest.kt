package com.puzzel.lab.marbelshort.engine

import com.puzzel.lab.marbelshort.level.LevelGenerator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lays out every level on a range of phone and tablet screens and checks that nothing overlaps:
 *  - the board frame stays below the HUD (top bar + booster tray), so no block is hidden under it,
 *  - blocks stay above a minimum size,
 *  - the belt sits below the funnel, and at least 3 rows of cups are visible below the belt.
 *
 * The HUD height must match GameScreen (`hudPx` = status bar + 150dp).
 */
class LayoutFitTest {

    private data class Screen(val name: String, val wPx: Int, val hPx: Int, val density: Float, val statusDp: Float)

    private val screens = listOf(
        Screen("720x1280 16:9", 720, 1280, 2f, 24f),
        Screen("1080x1920 16:9", 1080, 1920, 2.625f, 24f),
        Screen("720x1600 20:9", 720, 1600, 1.875f, 30f),
        Screen("1080x2400 20:9", 1080, 2400, 2.75f, 32f),
        Screen("1440x3120 19.5:9", 1440, 3120, 3.5f, 40f),
        Screen("1600x2560 tablet", 1600, 2560, 2f, 24f),
    )

    @Test
    fun everyLevelFitsEveryScreen() {
        val problems = ArrayList<String>()
        var minCell = Float.MAX_VALUE
        var minCellAt = ""
        for (n in 1..LevelGenerator.MAX_LEVEL) {
            val spec = LevelGenerator.generate(n)
            for (s in screens) {
                val scale = s.wPx / 720f
                val worldH = s.hPx / scale
                val hudBottom = (s.statusDp + HUD_DP) * s.density / scale
                val l = GameLayout(worldH, hudBottom, 8f, spec)
                val frameTop = l.gridTop - GameLayout.BOARD_FRAME
                if (frameTop < hudBottom + MIN_GAP) problems += "level $n on ${s.name}: board frame top $frameTop is under the HUD (ends at $hudBottom)"
                if (l.cell < MIN_CELL) problems += "level $n on ${s.name}: blocks too small (cell ${l.cell})"
                if (l.dishTop < l.spoutTop) problems += "level $n on ${s.name}: belt housing ${l.dishTop} overlaps the funnel (ends at ${l.spoutTop})"
                val threeRows = l.cupTop(3f)
                if (threeRows > worldH) problems += "level $n on ${s.name}: fewer than 3 cup rows visible (3 rows end at $threeRows, screen $worldH)"
                if (l.cell < minCell) { minCell = l.cell; minCellAt = "level $n on ${s.name} (${spec.width}x${spec.height})" }
            }
        }
        println("smallest block: $minCell units, $minCellAt")
        problems.take(30).forEach { println(it) }
        assertTrue("${problems.size} layout problems, first: ${problems.firstOrNull()}", problems.isEmpty())
    }

    private companion object {
        const val HUD_DP = 150f
        const val MIN_GAP = 4f
        /** Sanity floor: 7-row boards on short 16:9 screens are about 53 units per block. */
        const val MIN_CELL = 50f
    }
}
