package com.ngstudio.marblepop.engine

import com.ngstudio.marblepop.level.BlockState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class MarbleState { PATH, PHYSICS, BOARDING, BELT, DROPPING, FLYING }

class Marble(val color: Int) {
    var state = MarbleState.PHYSICS
    var x = 0f; var y = 0f
    var px = 0f; var py = 0f
    var channel = -1
    var slot = -1
    var angle = Random.nextFloat() * 360f
    // tween
    var t = 0f
    var dur = 0.2f
    var sx = 0f; var sy = 0f
    var targetCol = -1
    var targetCup: Cup? = null
    var targetDimple = 0
    var prevX = 0f
    var squash = 0f
    var age = 0f
    var stuckT = 0f
    /** PATH waypoints (x,y pairs) the marble follows out of the board. */
    var wp: FloatArray? = null
    var wpi = 0
    var wait = 0f
}

class Cup(var color: Int, var hidden: Boolean) {
    var reserved = 0
    var arrived = 0
    var display = 0f        // animated stack index
    var popping = false
    var popT = 0f
    var bounce = 0f
    var vel = 0f
    var reveal = if (hidden) 0f else 1f
    var revealing = false
    var shuffleT = 0f
}

class TrayColumn {
    val cups = ArrayList<Cup>()
}

class BlockVisual(val state: BlockState, val index: Int, val col: Int, val row: Int) {
    /** Extra rows above the resting place (intro drop). */
    var dropY = 0f
    var dropVel = 0f
    var delay = 0f
    var squash = 0f
    var shake = 0f
    var flash = 0f
    var reveal = if (state.revealed) 1f else 0f
    var crack = 0f
    var unlock = 0f
    var wasLocked = false
    var pulse = 0f
    var paint = 0f
    /** 0 = blocked (flat), 1 = open (has a way out). Animated for smooth wake-ups. */
    var open = 0f
    var wasOpen = false
    var wake = 0f
}

/** A block that just left the board (quick shrink + fade). */
class ExitingBlock(val state: BlockState, val x: Float, val y: Float, val cell: Float) {
    var t = 0f
}

class FlyingBlock(val state: BlockState, val x0: Float, val y0: Float, val cell: Float) {
    var t = 0f
    val duration = 0.55f
    var mx = 0f; var my = 0f // exit point
    var tx = 0f; var ty = 0f // burst point
    var x = x0; var y = y0
    var rot = 0f
    var scale = 1f
}

class FloatingText(val text: String, var x: Float, var y: Float, val color: Int, val size: Float) {
    var life = 0f
    val maxLife = 0.9f
}

/** Pooled particle system using flat arrays (no allocation per frame). */
class Particles(val capacity: Int = 420) {
    val x = FloatArray(capacity); val y = FloatArray(capacity)
    val vx = FloatArray(capacity); val vy = FloatArray(capacity)
    val life = FloatArray(capacity); val maxLife = FloatArray(capacity)
    val size = FloatArray(capacity); val rot = FloatArray(capacity); val vrot = FloatArray(capacity)
    val grav = FloatArray(capacity)
    val color = IntArray(capacity); val type = IntArray(capacity)
    var count = 0

    companion object {
        const val DOT = 0; const val STAR = 1; const val CONFETTI = 2; const val RING = 3; const val SHARD = 4; const val SMOKE = 5; const val LID = 6
    }

    fun emit(px: Float, py: Float, pvx: Float, pvy: Float, l: Float, s: Float, c: Int, t: Int, g: Float = 0f, vr: Float = 0f) {
        if (count >= capacity) return
        val i = count++
        x[i] = px; y[i] = py; vx[i] = pvx; vy[i] = pvy; life[i] = l; maxLife[i] = l
        size[i] = s; color[i] = c; type[i] = t; grav[i] = g; rot[i] = Random.nextFloat() * 360f; vrot[i] = vr
    }

    fun burst(px: Float, py: Float, n: Int, speed: Float, c: Int, t: Int, s: Float, l: Float, g: Float = 0f) {
        repeat(n) {
            val a = Random.nextFloat() * 2f * PI.toFloat()
            val sp = speed * (0.4f + Random.nextFloat() * 0.8f)
            emit(px, py, cos(a) * sp, sin(a) * sp, l * (0.7f + Random.nextFloat() * 0.5f), s * (0.6f + Random.nextFloat() * 0.7f), c, t, g,
                (Random.nextFloat() - 0.5f) * 720f)
        }
    }

    fun update(dt: Float) {
        var i = 0
        while (i < count) {
            life[i] -= dt
            if (life[i] <= 0f) {
                val last = --count
                if (i != last) {
                    x[i] = x[last]; y[i] = y[last]; vx[i] = vx[last]; vy[i] = vy[last]
                    life[i] = life[last]; maxLife[i] = maxLife[last]; size[i] = size[last]
                    rot[i] = rot[last]; vrot[i] = vrot[last]; grav[i] = grav[last]
                    color[i] = color[last]; type[i] = type[last]
                }
                continue
            }
            vy[i] += grav[i] * dt
            if (type[i] == CONFETTI) { vx[i] *= 0.985f; vy[i] = vy[i].coerceAtMost(260f) } else { vx[i] *= 0.97f; vy[i] *= 0.97f }
            x[i] += vx[i] * dt; y[i] += vy[i] * dt
            rot[i] += vrot[i] * dt
            i++
        }
    }

    fun clear() { count = 0 }
}
