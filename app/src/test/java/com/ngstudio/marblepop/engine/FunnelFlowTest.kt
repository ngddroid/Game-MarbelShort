package com.ngstudio.marblepop.engine

import com.ngstudio.marblepop.level.LevelGenerator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plays levels in the real-time engine (tapping as fast as the plan allows, the worst case for the
 * funnel) and checks that balls never get stuck in the funnel / pipe ("tunnel"):
 *  - balls are in the funnel but none reaches the pipe for [STUCK_ABOVE_PIPE] seconds, or
 *  - a ball waits at the gate while the belt has a free slot for more than a lap, or
 *  - balls pile up in the board waiting for funnel space (blocks must not release balls then), or
 *  - an opened block keeps some of its balls back (a block releases all its balls at once, or none).
 */
class FunnelFlowTest {

    private data class Stuck(val level: Int, val screenH: Float, val t: Float, val what: String)

    private fun play(n: Int, worldH: Float, topInset: Float): Stuck? {
        val spec = LevelGenerator.generate(n)
        val layout = GameLayout(worldH, topInset, 8f, spec)
        val e = GameEngine(spec, layout, object : GameListener {})
        val dt = 1f / 60f
        var t = 0f
        var nextTap = 0f
        var aboveTimer = 0f
        var gateTimer = 0f
        var pileTimer = 0f
        val highTime = HashMap<Marble, Float>()
        val startTime = HashMap<Marble, Float>()
        while (t < 300f && e.status == GameStatus.PLAYING) {
            if (t >= nextTap && !e.inIntro) {
                e.hintTarget()?.let { e.tap(it[0], it[1]) }
                nextTap = t + 0.15f
            }
            e.update(dt)
            t += dt
            var inFunnel = 0; var inPipe = 0; var atGate = false
            for (m in e.marbles) {
                if (m.state != MarbleState.PHYSICS) continue
                inFunnel++
                if (m.y > layout.spoutTop - 2f) inPipe++
                if (m.y > layout.gateY - 4f && kotlin.math.abs(m.x - layout.cx) < layout.spoutHalf) atGate = true
            }
            aboveTimer = if (inFunnel > 0 && inPipe == 0) aboveTimer + dt else 0f
            if (aboveTimer > STUCK_ABOVE_PIPE) {
                val pos = e.marbles.filter { it.state == MarbleState.PHYSICS }.joinToString { "(%.0f,%.0f)".format(it.x, it.y) }
                return Stuck(n, worldH, t, "balls not reaching pipe: $pos spoutTop=${layout.spoutTop} half=${layout.spoutHalf} r=${layout.hopperR}")
            }
            // piled: a streaming ball held at its exit cell because the funnel is full, or the same funnel
            // ball sitting above the funnel's top edge (up in the board) for over a second
            val held = inFunnel >= e.hopperCap
            val heldOnPath = e.marbles.count { held && it.state == MarbleState.PATH && it.wpi > 0 && it.wait > 0f && it.wp != null && it.wpi * 2 >= it.wp!!.size - 2 }
            // all-or-nothing: once a block opens, every ball leaves it (only the short start stagger is allowed)
            for (m in e.marbles) if (m.state == MarbleState.PATH && m.wpi == 0) startTime[m] = (startTime[m] ?: 0f) + dt else startTime.remove(m)
            if (startTime.values.any { it > 0.8f }) return Stuck(n, worldH, t, "a released block kept some balls back (partial release)")
            for (m in e.marbles) {
                if (m.state == MarbleState.PHYSICS && m.y < layout.gridBottom - layout.hopperR * 0.25f) highTime[m] = (highTime[m] ?: 0f) + dt else highTime.remove(m)
            }
            val longHigh = highTime.values.count { it > 1f }
            pileTimer = if (heldOnPath > 0) pileTimer + dt else 0f
            if (pileTimer > 0.6f || longHigh > 0) {
                return Stuck(n, worldH, t, "balls piled up in the board (held on path $heldOnPath, above the funnel for >1s $longHigh, funnel $inFunnel/${e.hopperCap}) doubles=${spec.blocks.count { it.kind == com.ngstudio.marblepop.level.BlockKind.DOUBLE }}")
            }
            gateTimer = if (atGate && e.beltCount < e.capacity) gateTimer + dt else 0f
            if (gateTimer > e.lapTime + 1f) return Stuck(n, worldH, t, "ball waiting at gate with free slots (belt ${e.beltCount}/${e.capacity})")
        }
        return null
    }

    @Test
    fun ballsNeverStickInTheFunnel() {
        val levels = (1..300) + (325..LevelGenerator.MAX_LEVEL step 25)
        val problems = ArrayList<Stuck>()
        for ((h, top) in listOf(1600f to 330f, 1280f to 300f)) {
            for (n in levels) play(n, h, top)?.let { problems += it }
        }
        problems.take(40).forEach { println("STUCK level ${it.level} (worldH ${it.screenH}) at ${"%.1f".format(it.t)}s: ${it.what}") }
        assertTrue("${problems.size} levels had stuck balls, first: ${problems.firstOrNull()}", problems.isEmpty())
    }

    private companion object {
        const val STUCK_ABOVE_PIPE = 2f
    }
}
