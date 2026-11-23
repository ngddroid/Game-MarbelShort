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
    /** Marble radius on the belt and in the cups (the belt can hold 48+ marbles, so this stays small). */
    val marbleR = 12f
    private val maxHopperR = 19f

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
    val cell: Float
    val gridTop: Float
    val gridBottom: Float
    val funnelH: Float

    // tank aliases kept for the renderer
    val tankLeft = 22f
    val tankRight = 698f
    val tankTop = topInset

    init {
        val belowFunnel = 46f + maxHopperR + marbleR + 6f + 34f + 52f
        val minFunnel = 110f; val maxFunnel = 170f
        val rowsMin = 3.2f; val rowsMax = 4.3f
        val avail = worldH - bottomInset - topInset - boardPad * 2 - belowFunnel
        var c = min((worldW - 90f) / gridW, 128f)
        c = min(c, (avail - minFunnel - rowsMin * rowH) / gridH)
        var left = avail - gridH * c - minFunnel - rowsMin * rowH
        val rowsExtra = min(left.coerceAtLeast(0f), (rowsMax - rowsMin) * rowH); left -= rowsExtra
        val funnelExtra = min(left.coerceAtLeast(0f), maxFunnel - minFunnel); left -= funnelExtra
        cell = c
        funnelH = minFunnel + funnelExtra
        gridTop = topInset + boardPad + left.coerceAtLeast(0f) * 0.55f
        gridBottom = gridTop + gridH * c
    }

    val tankBottom = gridBottom
    val gridLeft = cx - cell * gridW / 2f
    val gridRight = cx + cell * gridW / 2f

    // ---- hopper & spout
    /** Bigger radius for marbles that burst out of a block (path, hopper, spout); they shrink onto the belt. */
    val hopperR = (cell * 0.16f).coerceIn(14f, maxHopperR)
    val spoutTop = gridBottom + funnelH
    val spoutHalf = hopperR * 1.3f
    val gateY = spoutTop + 46f
    val gateFloor = gateY + hopperR

    // ---- belt
    val beltR = 34f
    val beltLeft = 76f
    val beltRight = 644f
    val topRunY = gateFloor + marbleR + 6f
    val beltCy = topRunY + beltR
    val trayTop: Float = beltCy + 52f
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
    val dimpleDx = floatArrayOf(-cupW * 0.27f, 0f, cupW * 0.27f)
    val dimpleDy = cupH * 0.42f

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
}
