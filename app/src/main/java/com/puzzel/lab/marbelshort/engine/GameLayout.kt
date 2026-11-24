package com.puzzel.lab.marbelshort.engine

import com.puzzel.lab.marbelshort.level.LevelSpec
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Geometry of the play field in world units. The world is always 720 units wide; its height follows
 * the screen. Top to bottom: board (grid) → hopper → spout → belt → tray.
 */
class GameLayout(val worldH: Float, topInset: Float, bottomInset: Float, spec: LevelSpec) {
    val worldW = 720f
    val cx = worldW / 2f
    /** Base size class for small marble sprites (flying marbles); cup marbles use [cupMarbleR]. */
    val marbleR = 12f
    /**
     * How roomy the screen is, 0..1: 1 on tall phones (20:9, about 1260+ units below the HUD),
     * 0 on short 16:9 screens and tablets (about 940). Ball size and funnel height follow it.
     */
    private val roomy = ((worldH - topInset - bottomInset - 940f) / 300f).coerceIn(0f, 1f)
    /**
     * Ball radius when released from a block and on the belt: [BALL_R] (the reference art, ~42 px on
     * 720) on tall phones, down to 14 on short screens, where the funnel can't hold 18 big balls of an
     * x2 block without shrinking the board too much.
     */
    private val maxHopperR = MIN_BALL_R + (BALL_R - MIN_BALL_R) * roomy

    /** Belt radius that keeps the island (gauge) inside the track the same size for any belt marble radius. */
    private fun beltRFor(r: Float) = 22f + r

    /**
     * Largest belt marble radius for which [cap] marbles sit side by side on the belt without overlapping
     * (iterated because a bigger marble needs a taller belt, which is slightly longer).
     */
    private fun beltFitR(cap: Int): Float {
        var r = maxHopperR
        repeat(3) {
            val br = beltRFor(r)
            val len = 2f * ((BELT_RIGHT - BELT_LEFT) - 2f * br) + 2f * PI.toFloat() * br
            r = min(maxHopperR, len / cap.coerceAtLeast(1) * 0.49f)
        }
        return r
    }
    private val beltFit = beltFitR(spec.capacity)
    /** Belt centre line to the top of the tray: clears the lower half of the belt housing. */
    private fun trayGap(r: Float) = beltRFor(r) + r + 6f

    // ---- tray
    val trayCols = spec.tray.size
    val trayLeft = 40f
    val trayRight = 680f
    val colW = (trayRight - trayLeft) / trayCols
    val cupW = colW - 16f
    val cupH = 58f
    val cupGap = 2f
    private val rowH = cupH + cupGap

    // ---- grid
    val gridW = spec.width
    val gridH = spec.height
    val gridMaxH = spec.height
    val mask: BooleanArray = spec.mask
    val boardPad = 20f
    /**
     * Room above the top row: the board frame drawn by the renderer reaches [BOARD_FRAME] above the
     * grid (14 padding + 10 rim), plus a gap so it never touches the booster bar.
     */
    val boardPadTop = BOARD_FRAME + 10f
    val cell: Float
    val gridTop: Float
    val gridBottom: Float
    val funnelH: Float

    // tank aliases kept for the renderer
    val tankLeft = 22f
    val tankRight = 698f
    val tankTop = topInset

    init {
        val estBeltR = beltFit // the real belt marble can only be this size or smaller
        val belowFunnel = 46f + maxHopperR + estBeltR + 6f + beltRFor(estBeltR) + trayGap(estBeltR)
        // bigger balls need a taller funnel to hold an x2 block (18 balls)
        val minFunnel = 110f + 60f * roomy; val maxFunnel = 170f + 80f * roomy
        val rowsMin = 3.2f; val rowsMax = 4.3f
        val avail = worldH - bottomInset - topInset - boardPadTop - boardPad - belowFunnel
        var c = min((worldW - 90f) / gridW, 128f)
        c = min(c, (avail - minFunnel - rowsMin * rowH) / gridH)
        var left = avail - gridH * c - minFunnel - rowsMin * rowH
        val rowsExtra = min(left.coerceAtLeast(0f), (rowsMax - rowsMin) * rowH); left -= rowsExtra
        val funnelExtra = min(left.coerceAtLeast(0f), maxFunnel - minFunnel); left -= funnelExtra
        cell = c
        funnelH = minFunnel + funnelExtra
        gridTop = topInset + boardPadTop + left.coerceAtLeast(0f) * 0.55f
        gridBottom = gridTop + gridH * c
    }

    val tankBottom = gridBottom
    val gridLeft = cx - cell * gridW / 2f
    val gridRight = cx + cell * gridW / 2f

    // ---- hopper & spout
    /** Radius of marbles that burst out of a block (path, hopper, spout); belt marbles keep this size when the belt has room. */
    val hopperR = maxHopperR
    val spoutTop = gridBottom + funnelH
    val spoutHalf = hopperR * 1.3f
    val gateY = spoutTop + 46f
    val gateFloor = gateY + hopperR

    // ---- belt
    /** Radius of marbles on the belt: the same size as the funnel marbles, unless the belt is too crowded for that. */
    val beltMarbleR = min(hopperR, beltFit)
    val beltR = beltRFor(beltMarbleR)
    val beltLeft = BELT_LEFT
    val beltRight = BELT_RIGHT
    val topRunY = gateFloor + beltMarbleR + 6f
    val beltCy = topRunY + beltR
    val trayTop: Float = beltCy + trayGap(beltMarbleR)
    val straight = (beltRight - beltLeft) - 2 * beltR
    val xl = cx - straight / 2f
    val xr = cx + straight / 2f
    val beltLength = (2 * straight + 2 * PI * beltR).toFloat()
    val dishTop = beltCy - beltR - 36f
    val dishLeft = 26f
    val dishRight = 694f

    fun colCenter(c: Int) = trayLeft + colW * (c + 0.5f)
    fun cupTop(index: Float) = trayTop + index * (cupH + cupGap)

    /** Hole centres of a cup (see MachineArt.puck): x offsets from the column centre, y from the cup top. */
    val dimpleDx = floatArrayOf(-cupW * HOLE_SPREAD, 0f, cupW * HOLE_SPREAD)
    val dimpleDy = cupH * 0.42f
    /**
     * Radius of marbles sitting in a cup: the belt size, capped so three fit side by side in a cup
     * with a thin wall between the holes (HOLE_SPREAD · cupW apart) and fit the cup's height.
     * 4 columns: about 15.7 units (height-limited); 5 columns: about 14 units (was 10.8 for all).
     */
    val cupMarbleR = minOf(beltMarbleR, cupW * HOLE_SPREAD * 0.43f, cupH * 0.27f)
    /** Hole radius in the cup art: just wider than the marble, so it reads as sitting in the hole. */
    val cupHoleR = cupMarbleR * 1.04f

    fun active(c: Int, r: Int) = c in 0 until gridW && r in 0 until gridH && mask[r * gridW + c]
    fun cellLeft(c: Int) = gridLeft + c * cell
    fun cellCenterX(c: Int) = gridLeft + (c + 0.5f) * cell
    /** Top y of a cell whose (animated) row is [row]; row 0 is the bottom row. */
    fun cellTop(row: Float) = gridBottom - (row + 1f) * cell
    fun cellCenterY(row: Float) = gridBottom - (row + 0.5f) * cell

    /** Belt path position for arc-length s (0 = entry point at the top centre, moving right). */
    fun beltPoint(sIn: Float, out: FloatArray) {
        var s = sIn % beltLength
        if (s < 0) s += beltLength
        val half = straight / 2f
        val arc = (PI * beltR).toFloat()
        when {
            s < half -> { out[0] = cx + s; out[1] = topRunY }
            s < half + arc -> {
                val t = (s - half) / beltR
                out[0] = xr + beltR * sin(t); out[1] = beltCy - beltR * cos(t)
            }
            s < half + arc + straight -> { out[0] = xr - (s - half - arc); out[1] = beltCy + beltR }
            s < half + 2 * arc + straight -> {
                val t = (s - half - arc - straight) / beltR
                out[0] = xl - beltR * sin(t); out[1] = beltCy + beltR * cos(t)
            }
            else -> { out[0] = xl + (s - half - 2 * arc - straight); out[1] = topRunY }
        }
    }

    companion object {
        /** How far the board frame (padding + rim) reaches outside the grid; see GameRenderer.drawBoard. */
        const val BOARD_FRAME = 24f
        private const val BELT_LEFT = 76f
        private const val BELT_RIGHT = 644f
        /**
         * Ball radius for released and belt balls. The belt holds 28 balls of this size side by side,
         * which is why LevelGenerator caps capacity at 28.
         */
        const val BALL_R = 21f
        /** Ball radius on short screens (16:9 phones, tablets). */
        const val MIN_BALL_R = 14f
        /** Must match MachineArt.HOLE_SPREAD (cup hole centres at ± spread · cupW). */
        private const val HOLE_SPREAD = 0.29f
    }
}
