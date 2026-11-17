package com.puzzel.lab.marbelshort.level

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {

    @Test
    fun allLevelsAreSolvableWithinBeltCapacity() {
        val featureCounts = HashMap<BlockKind, Int>()
        var minCap = 99; var maxCap = 0; var maxBlocks = 0
        for (n in 1..LevelGenerator.MAX_LEVEL) {
            val spec = LevelGenerator.generate(n)
            val sim = PuzzleSim(spec)
            assertTrue("level $n has no open block at start", sim.board.hasTappable())
            for (id in spec.plan) assertTrue("level $n: planned tap $id illegal", sim.tapId(id))
            assertTrue("level $n not solved", sim.solved)
            assertTrue("level $n peak ${sim.peakLoad} > cap ${spec.capacity}", sim.peakLoad <= spec.capacity)
            // marble / slot balance per colour
            val slots = IntArray(MarbleColors.COUNT)
            spec.tray.flatten().forEach { slots[it.color] += 3 }
            val real = IntArray(MarbleColors.COUNT)
            var rainbow = 0
            spec.blocks.forEach { if (it.kind == BlockKind.RAINBOW) rainbow += 9 else real[it.color] += it.marbleCount }
            assertEquals("level $n balance", slots.sum(), real.sum() + rainbow)
            for (i in slots.indices) assertTrue("level $n colour $i", real[i] <= slots[i])
            spec.blocks.forEach { featureCounts.merge(it.kind, 1, Int::plus) }
            minCap = minOf(minCap, spec.capacity); maxCap = maxOf(maxCap, spec.capacity)
            maxBlocks = maxOf(maxBlocks, spec.blockCount)
            if (spec.introduces != null) {
                val kinds = spec.blocks.map { it.kind }.toSet()
                val f = spec.introduces!!
                val ok = when (f) {
                    Feature.HIDDEN_CUP -> spec.tray.flatten().any { it.hidden }
                    Feature.KEY -> BlockKind.KEY in kinds
                    else -> kinds.any { it.name == f.name || (f == Feature.COUNTER && it == BlockKind.COUNTER) }
                }
                assertTrue("level $n should introduce $f", ok)
            }
        }
        println("open-at-start check passed")
        println("capacity range $minCap..$maxCap, max blocks $maxBlocks")
        println("kinds: $featureCounts")
    }

    @Test
    fun deterministic() {
        val a = LevelGenerator.generate(1234)
        val b = LevelGenerator.generate(1234)
        assertEquals(a.plan, b.plan)
        assertEquals(a.capacity, b.capacity)
    }
}
