package com.ngstudio.marblepop.engine

import com.ngstudio.marblepop.level.BlockKind
import com.ngstudio.marblepop.level.LevelGenerator
import com.ngstudio.marblepop.level.MarbleColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the four boosters (hammer, magnet, +6 slots, shuffle) in the real-time engine. */
class BoosterTest {

    private fun engine(n: Int): Pair<GameEngine, GameLayout> {
        val spec = LevelGenerator.generate(n)
        val l = GameLayout(1600f, 330f, 8f, spec)
        return GameEngine(spec, l, object : GameListener {}) to l
    }

    private fun run(e: GameEngine, seconds: Float) { var t = 0f; while (t < seconds && e.status == GameStatus.PLAYING) { e.update(1f / 60f); t += 1f / 60f } }

    /** Pops planned blocks until the belt holds at least [want] marbles (or nothing is left to tap). */
    private fun fillBelt(e: GameEngine, want: Int) {
        var t = 0f
        while (t < 60f && e.beltCount < want && e.status == GameStatus.PLAYING) {
            if (!e.inIntro) e.hintTarget()?.let { e.tap(it[0], it[1]) }
            run(e, 0.25f); t += 0.25f
        }
    }

    /** Every belt slot points back at its marble and every belt marble sits in its slot. */
    private fun assertBeltConsistent(e: GameEngine) {
        for (i in e.slots.indices) e.slots[i]?.let { assertEquals("slot index", i, it.slot) }
        assertEquals("beltCount", e.slots.count { it != null }, e.beltCount)
    }

    /** Every cup holds at most 3 marbles and never more than were assigned to it. */
    private fun assertCupsConsistent(e: GameEngine) {
        for (col in e.tray) for (cup in col.cups) {
            assertTrue("cup over-filled", cup.arrived <= 3 && cup.reserved <= 3)
            assertTrue("arrived > reserved", cup.arrived <= cup.reserved)
        }
    }

    @Test
    fun slotsBoosterAddsSixSlotsAndKeepsEveryMarble() {
        for (n in listOf(5, 31, 97, 247)) {
            val (e, _) = engine(n)
            run(e, 3f)
            fillBelt(e, 8)
            val cap = e.capacity
            val onBelt = e.beltCount
            val colors = e.slots.filterNotNull().map { it.color }.sorted()
            e.boosterSlots()
            assertEquals("level $n capacity", cap + 6, e.capacity)
            assertEquals("level $n marbles kept", onBelt, e.beltCount)
            assertEquals("level $n colours kept", colors, e.slots.filterNotNull().map { it.color }.sorted())
            assertBeltConsistent(e)
            run(e, 3f)
            assertBeltConsistent(e)
        }
    }

    @Test
    fun magnetMovesOneColourIntoMatchingCups() {
        var used = 0
        for (n in listOf(5, 31, 46, 97, 127, 247)) {
            val (e, _) = engine(n)
            run(e, 3f)
            fillBelt(e, 12)
            val before = e.beltCount
            if (!e.magnet()) continue
            used++
            assertTrue("level $n: magnet took nothing off the belt", e.beltCount < before)
            // every dropping marble is heading to a cup of its own colour
            for (m in e.marbles) if (m.state == MarbleState.DROPPING) {
                val cup = m.targetCup!!
                assertTrue("level $n: wrong colour", m.color == cup.color || m.color == MarbleColors.RAINBOW)
            }
            run(e, 2f)
            assertBeltConsistent(e)
            assertCupsConsistent(e)
        }
        assertTrue("magnet never had anything to pull", used >= 3)
    }

    @Test
    fun magnetWithEmptyBeltDoesNothing() {
        val (e, _) = engine(31)
        run(e, 3f)
        assertFalse(e.magnet())
        assertEquals(0, e.boostersUsed)
    }

    @Test
    fun shuffleKeepsTheSameColoursAndLeavesFillingCupsAlone() {
        for (n in listOf(31, 46, 97, 247)) {
            val (e, _) = engine(n)
            run(e, 3f)
            fillBelt(e, 10)
            run(e, 1f)
            val all = e.tray.flatMap { it.cups }
            val busy = all.filter { it.reserved > 0 }.associateWith { it.color }
            val freeColours = all.filter { it.reserved == 0 && !it.popping }.map { it.color }.sorted()
            assertTrue(e.boosterShuffle())
            for ((cup, c) in busy) assertEquals("level $n: a filling cup changed colour", c, cup.color)
            assertEquals("level $n: colours changed", freeColours, all.filter { it.reserved == 0 && !it.popping }.map { it.color }.sorted())
            assertTrue("level $n: hidden cup left", all.none { it.hidden && !it.popping && it.reserved == 0 })
            run(e, 2f)
            assertCupsConsistent(e)
        }
    }

    @Test
    fun hammerSmashesLockedBlocksAndRespectsAFullFunnel() {
        var smashedLocked = 0
        for (n in listOf(46, 97, 127, 150, 247)) {
            val (e, l) = engine(n)
            run(e, 3f)
            val spec = e.spec
            // a block that can't be tapped normally (locked, caged, chained, no path...)
            val i = (0 until spec.width * spec.height).firstOrNull { e.board.cells[it] != null && !e.board.canTap(it) } ?: continue
            val kind = e.board.cells[i]!!.spec.kind
            val x = l.cellCenterX(i % spec.width); val y = l.cellCenterY((i / spec.width).toFloat())
            assertTrue("level $n: hammer refused a $kind block", e.hammer(x, y))
            assertTrue("level $n: block still there", e.board.cells[i] == null)
            assertEquals(1, e.boostersUsed)
            if (kind != BlockKind.NORMAL) smashedLocked++
            run(e, 4f)
            assertBeltConsistent(e); assertCupsConsistent(e)
        }
        assertTrue("no locked block was tested", smashedLocked >= 2)

        // a full funnel: the hammer says WAIT and is not used up
        val (e, l) = engine(97)
        run(e, 3f)
        var t = 0f
        while (t < 5f) { e.hintTarget()?.let { e.tap(it[0], it[1]) }; run(e, 0.05f); t += 0.05f }
        val i = (0 until e.spec.width * e.spec.height).firstOrNull { e.board.cells[it] != null }
        if (i != null && !e.funnelHasRoom(e.board.cells[i]!!)) {
            val used = e.boostersUsed
            assertFalse(e.hammer(l.cellCenterX(i % e.spec.width), l.cellCenterY((i / e.spec.width).toFloat())))
            assertEquals("hammer used up while refused", used, e.boostersUsed)
        }
    }

    /** Plays levels to the end using every booster along the way: nothing may break, no marble may get lost. */
    @Test
    fun levelsStillFinishCleanlyWithBoosters() {
        for (n in listOf(12, 31, 46, 97, 127, 150, 247, 600)) {
            val (e, l) = engine(n)
            val total = e.spec.blocks.sumOf { it.marbleCount }
            var t = 0f; var next = 0f; var k = 0
            while (t < 400f && e.status == GameStatus.PLAYING) {
                val settled = e.marbles.none { it.state == MarbleState.PATH || it.state == MarbleState.PHYSICS || it.state == MarbleState.BOARDING } && e.flying.isEmpty()
                if (t >= next && !e.inIntro && settled) {
                    when (k++ % 9) {
                        3 -> e.magnet()
                        5 -> e.boosterShuffle()
                        7 -> if (e.beltCount > e.capacity * 0.6f) e.boosterSlots()
                        8 -> {
                            val i = (0 until e.spec.width * e.spec.height).firstOrNull { e.board.cells[it] != null && !e.board.canTap(it) }
                            if (i != null) e.hammer(l.cellCenterX(i % e.spec.width), l.cellCenterY((i / e.spec.width).toFloat()))
                            else e.hintTarget()?.let { e.tap(it[0], it[1]) }
                        }
                        else -> e.hintTarget()?.let { e.tap(it[0], it[1]) }
                    }
                    next = t + 0.3f
                }
                e.update(1f / 60f); t += 1f / 60f
                if (k % 20 == 0) { assertBeltConsistent(e); assertCupsConsistent(e) }
            }
            assertTrue("level $n did not finish (status ${e.status}, t=$t)", e.status != GameStatus.PLAYING)
            if (e.status == GameStatus.WON) assertEquals("level $n: marbles lost", total, e.marblesSorted)
        }
    }
}
