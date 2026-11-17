package com.puzzel.lab.marbelshort.render

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathDashPathEffect
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.puzzel.lab.marbelshort.engine.BlockVisual
import com.puzzel.lab.marbelshort.engine.GameEngine
import com.puzzel.lab.marbelshort.engine.MarbleState
import com.puzzel.lab.marbelshort.engine.Particles
import com.puzzel.lab.marbelshort.level.BlockKind
import com.puzzel.lab.marbelshort.level.MarbleColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * "Candy Glass Machine" playground (see PLAYGROUND_GRAPHICS_PLAN.md): the world sky with a candy
 * skyline, a glass board set in a candy rim that flows into a hopper and pipe, a candy belt housing
 * with a dark track, rollers and a fill gauge, and a frosted tray cabinet with glass lanes.
 * Static art is baked into three layers (backdrop / board / base) so the scene can assemble with a
 * springy intro; everything moving is a cached sprite blit or a handful of cheap primitives.
 */
class GameRenderer(
    private val engine: GameEngine,
    private val theme: GameTheme,
    private val skin: Int,
    val widthPx: Int,
    val heightPx: Int,
) {
    private val l = engine.layout
    val scale = widthPx / l.worldW
    val sprites = SpriteCache(scale)
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dst = RectF()
    private val tmp = FloatArray(2)
    private val path = Path()
    private val vignette = Paint().apply {
        shader = RadialGradient(l.worldW / 2, l.worldH / 2, l.worldH * 0.65f,
            intArrayOf(0x00FFB04A, 0x00FFB04A, 0xAAFF8A3D.toInt()), floatArrayOf(0f, 0.68f, 1f), Shader.TileMode.CLAMP)
    }

    // ---------------- palette
    private val frame = theme.wall
    private val frameDark = theme.wallDark
    private val sky = if (theme.sky.size == 6) theme.sky else intArrayOf(theme.bgTop, Col.mix(theme.bgTop, theme.bgBottom, 0.5f), theme.bgBottom, Color.WHITE, Color.WHITE, theme.wallDark)
    private val silhouette = sky[5]
    private val glassTop = Col.lighten(theme.wallLight, 0.9f)
    private val glassMid = Col.lighten(theme.wallLight, 0.8f)
    private val glassBottom = Col.lighten(theme.wallLight, 0.66f)
    private val wellCol = Col.mix(glassMid, 0xFF5A5F8A.toInt(), 0.13f)
    private val trackTop = Col.darken(Col.mix(0xFF2A1F6B.toInt(), frameDark, 0.15f), 0.12f)
    private val trackBottom = Col.darken(Col.mix(0xFF16113E.toInt(), frameDark, 0.1f), 0.05f)

    private val pad = 14f
    private val rimW = 10f
    private val boardTop = l.gridTop - pad
    private val boardLayerTop = boardTop - rimW - 8f
    private val boardLayerBottom = l.gateFloor + 26f
    private val baseTop = l.dishTop - 16f
    private val islandR = l.beltR - l.marbleR - 7f
    private val hubR = islandR * 0.95f
    private val gx0 = l.xl + islandR * 1.15f
    private val gx1 = l.xr - islandR * 1.15f
    private val gh = islandR * 1.2f

    private var backdrop: Bitmap? = null
    private var board: Bitmap? = null
    private var base: Bitmap? = null

    // per-frame shaders, created once
    private val gaugeGreen = LinearGradient(0f, l.beltCy - gh / 2, 0f, l.beltCy + gh / 2, 0xFF8EF08A.toInt(), 0xFF22A447.toInt(), Shader.TileMode.CLAMP)
    private val gaugeYellow = LinearGradient(0f, l.beltCy - gh / 2, 0f, l.beltCy + gh / 2, 0xFFFFE58A.toInt(), 0xFFF0A000.toInt(), Shader.TileMode.CLAMP)
    private val gaugeRed = LinearGradient(0f, l.beltCy - gh / 2, 0f, l.beltCy + gh / 2, 0xFFFFA07A.toInt(), 0xFFE8323F.toInt(), Shader.TileMode.CLAMP)
    private val fogTop = l.cupTop(0.95f)
    private val fogShader = LinearGradient(0f, fogTop, 0f, l.worldH, intArrayOf(Col.alpha(glassBottom, 0), Col.alpha(glassBottom, 70), Col.alpha(glassBottom, 175)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
    private val fogPaint = Paint().apply { shader = fogShader }
    private var gaugeCount = -1
    private var gaugeLabel = ""

    /** Twinkling sparkles in the sky: x, y, size, phase. */
    private val sparkles: FloatArray = run {
        val rnd = Random(11)
        val n = 7
        FloatArray(n * 4).also { a ->
            for (i in 0 until n) {
                val left = i % 2 == 0
                a[i * 4] = if (left) 14f + rnd.nextFloat() * 70f else l.worldW - 14f - rnd.nextFloat() * 70f
                a[i * 4 + 1] = l.gridTop + rnd.nextFloat() * (l.dishTop - l.gridTop - 40f)
                a[i * 4 + 2] = 5f + rnd.nextFloat() * 6f
                a[i * 4 + 3] = rnd.nextFloat() * 6.28f
            }
        }
    }

    init {
        val colors = HashSet<Int>()
        for (b in engine.spec.blocks) colors += b.color
        for (col in engine.spec.tray) for (c in col) colors += c.color
        for (c in colors) {
            sprites.marbleShadowed(c, skin, l.marbleR)
            sprites.marble(c, skin, l.marbleR * 0.9f)
            sprites.block(c, 0, l.cell, skin, true); sprites.block(c, 0, l.cell, skin, false)
            sprites.cup(c, false, l.cupW, l.cupH)
        }
        sprites.marbleShadowed(MarbleColors.RAINBOW, skin, l.marbleR)
        sprites.socket(l.marbleR * 1.02f)
        sprites.hub(hubR, frame)
        backdrop = layer(0f, l.worldH) { drawBackdrop(it) }
        board = layer(boardLayerTop, boardLayerBottom) { drawBoard(it) }
        base = layer(baseTop, l.worldH) { drawBase(it) }
    }

    private fun layer(top: Float, bottom: Float, draw: (Canvas) -> Unit): Bitmap {
        val h = ((bottom - top) * scale).toInt().coerceIn(1, heightPx)
        val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(scale, scale)
        c.translate(0f, -top)
        draw(c)
        return bmp
    }

    fun release() {
        sprites.clear()
        backdrop?.recycle(); board?.recycle(); base?.recycle()
        backdrop = null; board = null; base = null
    }

    // ================================================================ static art

    private fun blurred(radius: Float) = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)

    private fun drawBackdrop(c: Canvas) {
        val w = l.worldW; val h = l.worldH
        p.reset(); p.isAntiAlias = true
        p.shader = LinearGradient(0f, 0f, 0f, h, intArrayOf(sky[0], sky[1], sky[2]), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(w * 0.12f, h * 0.2f, w * 0.75f, Col.alpha(sky[3], 120), Col.alpha(sky[3], 0), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(w * 0.95f, h * 0.58f, w * 0.62f, Col.alpha(sky[4], 100), Col.alpha(sky[4], 0), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        // soft bokeh
        val rnd = Random(5)
        repeat(12) {
            val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h * 0.85f; val r = 26f + rnd.nextFloat() * 70f
            p.shader = RadialGradient(x, y, r, Col.alpha(Color.WHITE, 30 + rnd.nextInt(30)), 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawCircle(x, y, r, p)
        }
        p.shader = null
        // candy factory skyline peeking above the tray cabinet
        val horizon = l.dishTop + 40f
        skyline(c, horizon, 21, 110f, 230f, Col.alpha(Col.lighten(silhouette, 0.3f), 95), false)
        skyline(c, horizon, 8, 60f, 150f, Col.alpha(silhouette, 165), true)
        // haze at the horizon
        p.shader = LinearGradient(0f, horizon - 160f, 0f, horizon, Col.alpha(sky[2], 0), Col.alpha(Col.lighten(sky[2], 0.3f), 120), Shader.TileMode.CLAMP)
        c.drawRect(0f, horizon - 160f, w, horizon, p); p.shader = null
        repeat(10) { Painter.star4(c, rnd.nextFloat() * w, rnd.nextFloat() * h * 0.7f, 4f + rnd.nextFloat() * 5f, Col.alpha(Color.WHITE, 110)) }
    }

    private fun skyline(c: Canvas, horizon: Float, seed: Int, minH: Float, maxH: Float, color: Int, windows: Boolean) {
        val rnd = Random(seed)
        val sk = Path()
        val lights = ArrayList<RectF>()
        var x = -20f
        while (x < l.worldW + 20f) {
            val bw = 44f + rnd.nextFloat() * 56f
            val bh = minH + rnd.nextFloat() * (maxH - minH)
            val top = horizon - bh
            when (rnd.nextInt(4)) {
                0 -> { // flat roof + chimney
                    sk.addRect(x, top, x + bw, horizon, Path.Direction.CW)
                    sk.addRoundRect(RectF(x + bw * 0.62f, top - bh * 0.28f, x + bw * 0.82f, top + 4f), 4f, 4f, Path.Direction.CW)
                }
                1 -> { // dome
                    sk.addRect(x, top + bw * 0.3f, x + bw, horizon, Path.Direction.CW)
                    sk.addOval(RectF(x, top - bw * 0.2f, x + bw, top + bw * 0.8f), Path.Direction.CW)
                }
                2 -> { // lollipop tower
                    sk.addRect(x + bw * 0.1f, top + bh * 0.35f, x + bw * 0.9f, horizon, Path.Direction.CW)
                    sk.addRect(x + bw * 0.44f, top, x + bw * 0.56f, top + bh * 0.4f, Path.Direction.CW)
                    sk.addCircle(x + bw / 2, top, bw * 0.3f, Path.Direction.CW)
                }
                else -> { // stepped roof
                    sk.addRect(x, top + bh * 0.2f, x + bw, horizon, Path.Direction.CW)
                    sk.addRoundRect(RectF(x + bw * 0.2f, top, x + bw * 0.8f, top + bh * 0.3f), 6f, 6f, Path.Direction.CW)
                }
            }
            if (windows) {
                val rows = ((bh * 0.6f) / 26f).toInt().coerceAtMost(4)
                for (r in 0 until rows) if (rnd.nextFloat() < 0.6f) {
                    val wx = x + bw * (0.25f + rnd.nextFloat() * 0.4f)
                    val wy = horizon - bh * 0.55f + r * 26f
                    lights += RectF(wx, wy, wx + 9f, wy + 12f)
                }
            }
            x += bw + 4f + rnd.nextFloat() * 16f
        }
        p.reset(); p.isAntiAlias = true; p.color = color
        c.drawPath(sk, p)
        p.color = Col.alpha(sky[3], 110)
        for (r in lights) c.drawRoundRect(r, 3f, 3f, p)
    }

    /**
     * Board outline: every active cell as a rounded rect (true circular corners), bridges between
     * neighbouring cells so their rounded corners never leave notches, and the hopper + spout.
     */
    private fun boardShape(grow: Float, radius: Float): Path {
        val shape = Path()
        val g = pad + grow
        fun add(path: Path) { shape.op(path, Path.Op.UNION) }
        for (r in 0 until l.gridH) for (col in 0 until l.gridW) {
            if (!l.active(col, r)) continue
            val x = l.cellLeft(col); val y = l.cellTop(r.toFloat())
            add(Path().apply { addRoundRect(RectF(x - g, y - g, x + l.cell + g, y + l.cell + g), radius, radius, Path.Direction.CW) })
            if (l.active(col + 1, r)) add(Path().apply { addRect(x + l.cell / 2, y - g, x + l.cell * 1.5f, y + l.cell + g, Path.Direction.CW) })
            if (l.active(col, r + 1)) add(Path().apply { addRect(x - g, y - l.cell / 2, x + l.cell + g, y + l.cell / 2, Path.Direction.CW) })
            if (l.active(col + 1, r + 1) && l.active(col + 1, r) && l.active(col, r + 1)) {
                add(Path().apply { addRect(x + l.cell / 2, y - l.cell / 2, x + l.cell * 1.5f, y + l.cell / 2, Path.Direction.CW) })
            }
        }
        val sl = l.cx - l.spoutHalf - 10f - grow; val sr = l.cx + l.spoutHalf + 10f + grow
        val bottom = l.gateFloor + 2f + grow * 0.4f
        val left = l.gridLeft - g; val right = l.gridRight + g
        add(Path().apply {
            moveTo(left, l.gridBottom - l.cell * 0.5f)
            lineTo(right, l.gridBottom - l.cell * 0.5f)
            lineTo(right, l.gridBottom)
            cubicTo(right, l.gridBottom + l.funnelH * 0.55f, sr + 10f, l.spoutTop - l.funnelH * 0.25f, sr, l.spoutTop)
            lineTo(sr, bottom)
            lineTo(sl, bottom)
            lineTo(sl, l.spoutTop)
            cubicTo(sl - 10f, l.spoutTop - l.funnelH * 0.25f, left, l.gridBottom + l.funnelH * 0.55f, left, l.gridBottom)
            close()
        })
        return shape
    }

    /** Applies rounded corners for real (so the result also works as a clip). */
    @Suppress("DEPRECATION")
    private fun rounded(src: Path, radius: Float): Path {
        val pp = Paint(Paint.ANTI_ALIAS_FLAG).apply { pathEffect = CornerPathEffect(radius) }
        val out = Path()
        pp.getFillPath(src, out)
        return out
    }

    private fun drawBoard(c: Canvas) {
        // concave steps of the staircase are softened by the corner effect; convex corners are true arcs
        val outer = rounded(boardShape(rimW, 22f + rimW), 14f)
        val inner = rounded(boardShape(0f, 22f), 22f)
        val bx0 = l.gridLeft - pad; val bx1 = l.gridRight + pad
        p.reset(); p.isAntiAlias = true
        // drop shadow + depth side
        p.color = Col.alpha(silhouette, 120); p.maskFilter = blurred(14f)
        c.save(); c.translate(0f, 16f); c.drawPath(outer, p); c.restore()
        p.maskFilter = null
        p.color = frameDark
        c.save(); c.translate(0f, 9f); c.drawPath(outer, p); c.restore()
        // candy rim
        p.shader = LinearGradient(0f, boardTop - rimW, 0f, l.gateFloor, intArrayOf(Col.lighten(frame, 0.32f), frame, Col.darken(frame, 0.08f)), floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        c.drawPath(outer, p); p.shader = null
        p.style = Paint.Style.STROKE
        c.save(); c.clipPath(outer)
        p.strokeWidth = 4f; p.color = 0x99FFFFFF.toInt()
        c.save(); c.translate(0f, 3f); c.drawPath(outer, p); c.restore()
        c.restore()
        p.strokeWidth = 2.5f; p.color = Col.alpha(Col.darken(frameDark, 0.3f), 150); c.drawPath(outer, p)
        p.style = Paint.Style.FILL
        // glass interior
        p.shader = LinearGradient(0f, boardTop, 0f, l.gateFloor, intArrayOf(glassTop, glassMid, glassBottom), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawPath(inner, p); p.shader = null
        c.save(); c.clipPath(inner)
        // diagonal glass streaks
        val bh = l.gateFloor - boardTop
        for ((k, a) in listOf(0.12f to 40, 0.3f to 26, 0.68f to 22)) {
            val sx = bx0 + (bx1 - bx0) * k
            path.reset()
            path.moveTo(sx, l.gateFloor); path.lineTo(sx + 34f, l.gateFloor)
            path.lineTo(sx + 34f + bh * 0.45f, boardTop); path.lineTo(sx + bh * 0.45f, boardTop); path.close()
            p.color = Col.alpha(Color.WHITE, a); c.drawPath(path, p)
        }
        // hopper deepens toward the spout
        p.shader = RadialGradient(l.cx, l.spoutTop, l.funnelH + 60f, Col.alpha(frame, 64), Col.alpha(frame, 0), Shader.TileMode.CLAMP)
        c.drawRect(bx0 - rimW, l.gridBottom, bx1 + rimW, l.gateFloor + 10f, p); p.shader = null
        // dotted guide lanes from each exit into the spout
        p.style = Paint.Style.STROKE
        val dot = Path().apply { addCircle(0f, 0f, 2.6f, Path.Direction.CW) }
        p.pathEffect = PathDashPathEffect(dot, 13f, 0f, PathDashPathEffect.Style.TRANSLATE)
        p.color = Col.alpha(Color.WHITE, 170)
        for (col in 0 until l.gridW) {
            if (!l.active(col, 0)) continue
            val x = l.cellCenterX(col)
            path.reset(); path.moveTo(x, l.gridBottom + 28f)
            path.quadTo(x, l.spoutTop - l.funnelH * 0.3f, l.cx, l.spoutTop - 4f)
            c.drawPath(path, p)
        }
        p.pathEffect = null
        // inner shadow (glass sits inside the rim)
        p.strokeWidth = 22f; p.color = Col.alpha(frame, 85); p.maskFilter = blurred(9f)
        c.save(); c.translate(0f, 7f); c.drawPath(inner, p); c.restore()
        p.maskFilter = null
        c.restore()
        p.strokeWidth = 2f; p.color = Col.alpha(frameDark, 90); c.drawPath(inner, p)
        // exit chevrons
        p.strokeWidth = 4.5f; p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
        for (col in 0 until l.gridW) {
            if (!l.active(col, 0)) continue
            val x = l.cellCenterX(col); val y = l.gridBottom + 10f
            path.reset(); path.moveTo(x - 9f, y - 3f); path.lineTo(x, y + 4f); path.lineTo(x + 9f, y - 3f)
            p.color = Col.alpha(frameDark, 60); c.save(); c.translate(0f, 2f); c.drawPath(path, p); c.restore()
            p.color = Color.WHITE; c.drawPath(path, p)
        }
        p.style = Paint.Style.FILL; p.strokeCap = Paint.Cap.BUTT
        // recessed cell wells
        for (r in 0 until l.gridH) for (col in 0 until l.gridW) {
            if (!l.active(col, r)) continue
            val x = l.cellLeft(col); val y = l.cellTop(r.toFloat())
            val rr = l.cell * 0.23f
            dst.set(x + l.cell * 0.065f, y + l.cell * 0.065f, x + l.cell * 0.935f, y + l.cell * 0.935f)
            p.color = 0xCCFFFFFF.toInt(); dst.offset(0f, 2.5f); c.drawRoundRect(dst, rr, rr, p); dst.offset(0f, -2.5f)
            p.color = wellCol; c.drawRoundRect(dst, rr, rr, p)
            c.save(); path.reset(); path.addRoundRect(dst, rr, rr, Path.Direction.CW); c.clipPath(path)
            p.shader = LinearGradient(0f, dst.top, 0f, dst.top + l.cell * 0.3f, Col.alpha(Col.darken(frameDark, 0.2f), 70), Col.alpha(frameDark, 0), Shader.TileMode.CLAMP)
            c.drawRect(dst, p); p.shader = null
            c.restore()
        }
        // glass pipe with candy collars
        val sl = l.cx - l.spoutHalf - 3f; val sr = l.cx + l.spoutHalf + 3f
        dst.set(sl, l.spoutTop - 10f, sr, l.gateFloor + 4f)
        p.shader = LinearGradient(sl, 0f, sr, 0f, intArrayOf(Col.alpha(frameDark, 110), Col.alpha(Color.WHITE, 220), Col.alpha(Color.WHITE, 150), Col.alpha(frameDark, 120)), floatArrayOf(0f, 0.3f, 0.6f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(dst, p); p.shader = null
        collar(c, l.spoutTop - 8f, sl - 8f, sr + 8f)
        collar(c, l.gateFloor - 6f, sl - 6f, sr + 6f)
    }

    private fun collar(c: Canvas, cy: Float, x0: Float, x1: Float) {
        dst.set(x0, cy - 7f, x1, cy + 7f)
        p.color = frameDark; dst.offset(0f, 3f); c.drawRoundRect(dst, 6f, 6f, p); dst.offset(0f, -3f)
        p.shader = LinearGradient(0f, dst.top, 0f, dst.bottom, Col.lighten(frame, 0.45f), Col.darken(frame, 0.1f), Shader.TileMode.CLAMP)
        c.drawRoundRect(dst, 6f, 6f, p); p.shader = null
        p.color = 0xB3FFFFFF.toInt(); c.drawRoundRect(dst.left + 5f, dst.top + 2.5f, dst.right - 5f, dst.top + 5f, 2f, 2f, p)
    }

    private fun stadium(radius: Float): Path = Path().apply {
        addRoundRect(RectF(l.xl - radius, l.beltCy - radius, l.xr + radius, l.beltCy + radius), radius, radius, Path.Direction.CW)
    }

    private fun drawBase(c: Canvas) {
        p.reset(); p.isAntiAlias = true
        // ---- tray cabinet
        val dish = RectF(l.dishLeft, l.dishTop, l.dishRight, l.worldH + 60f)
        p.color = Col.alpha(silhouette, 110); p.maskFilter = blurred(14f)
        dst.set(dish); dst.offset(0f, -4f); c.drawRoundRect(dst, 46f, 46f, p); p.maskFilter = null
        p.color = frameDark; dst.set(dish); dst.offset(0f, 7f); c.drawRoundRect(dst, 46f, 46f, p)
        p.shader = LinearGradient(0f, dish.top, 0f, dish.top + 240f, Col.lighten(frame, 0.32f), frame, Shader.TileMode.CLAMP)
        c.drawRoundRect(dish, 46f, 46f, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = 0x99FFFFFF.toInt()
        dst.set(dish); dst.inset(3f, 3f); dst.offset(0f, 1.5f)
        c.save(); c.clipRect(dish.left, dish.top, dish.right, dish.top + 60f); c.drawRoundRect(dst, 43f, 43f, p); c.restore()
        p.strokeWidth = 2.5f; p.color = Col.alpha(Col.darken(frameDark, 0.3f), 150); c.drawRoundRect(dish, 46f, 46f, p)
        p.style = Paint.Style.FILL
        val inn = RectF(dish); inn.inset(rimW, rimW)
        p.shader = LinearGradient(0f, inn.top, 0f, l.worldH, intArrayOf(glassTop, glassMid, glassBottom), floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP)
        c.drawRoundRect(inn, 36f, 36f, p); p.shader = null
        c.save(); path.reset(); path.addRoundRect(inn, 36f, 36f, Path.Direction.CW); c.clipPath(path)
        // glass lanes
        for (col in 0 until l.trayCols) {
            val cx = l.colCenter(col)
            dst.set(cx - l.colW / 2 + 5f, l.trayTop - 10f, cx + l.colW / 2 - 5f, l.worldH + 40f)
            p.color = Col.alpha(frameDark, 26); c.drawRoundRect(dst, 22f, 22f, p)
            p.shader = LinearGradient(dst.left, 0f, dst.left + 16f, 0f, Col.alpha(frameDark, 50), Col.alpha(frameDark, 0), Shader.TileMode.CLAMP)
            c.drawRoundRect(dst, 22f, 22f, p); p.shader = null
            p.color = 0x99FFFFFF.toInt(); c.drawRect(dst.right - 4f, dst.top + 20f, dst.right - 2f, dst.bottom, p)
        }
        p.style = Paint.Style.STROKE; p.strokeWidth = 20f; p.color = Col.alpha(frame, 80); p.maskFilter = blurred(8f)
        c.save(); c.translate(0f, 6f); c.drawRoundRect(inn, 36f, 36f, p); c.restore()
        p.maskFilter = null; p.style = Paint.Style.FILL
        c.restore()
        // ---- belt housing
        val r = l.marbleR
        val hR = l.beltR + r + 13f
        val tR = l.beltR + r + 6f
        p.color = Col.alpha(frameDark, 90); p.maskFilter = blurred(7f)
        c.save(); c.translate(0f, 7f); c.drawPath(stadium(hR), p); c.restore(); p.maskFilter = null
        p.color = frameDark
        c.save(); c.translate(0f, 5f); c.drawPath(stadium(hR), p); c.restore()
        p.shader = LinearGradient(0f, l.beltCy - hR, 0f, l.beltCy + hR, intArrayOf(Col.lighten(frame, 0.38f), frame, Col.darken(frame, 0.12f)), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        c.drawPath(stadium(hR), p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 3f
        p.shader = LinearGradient(0f, l.beltCy - hR, 0f, l.beltCy, 0xCCFFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawPath(stadium(hR - 2.5f), p); p.shader = null
        p.strokeWidth = 2f; p.color = Col.alpha(Col.darken(frameDark, 0.3f), 150); c.drawPath(stadium(hR), p)
        p.style = Paint.Style.FILL
        // track
        p.shader = LinearGradient(0f, l.beltCy - tR, 0f, l.beltCy + tR, trackTop, trackBottom, Shader.TileMode.CLAMP)
        c.drawPath(stadium(tR), p); p.shader = null
        c.save(); c.clipPath(stadium(tR))
        p.style = Paint.Style.STROKE; p.strokeWidth = 10f; p.color = 0x99000000.toInt(); p.maskFilter = blurred(5f)
        c.save(); c.translate(0f, 4f); c.drawPath(stadium(tR), p); c.restore()
        p.maskFilter = null; p.style = Paint.Style.FILL
        c.restore()
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f; p.color = 0x40FFFFFF
        c.save(); c.translate(0f, 1.2f); c.drawPath(stadium(tR), p); c.restore()
        p.style = Paint.Style.FILL
        // island
        p.color = 0x66000000; c.save(); c.translate(0f, 2.5f); c.drawPath(stadium(islandR), p); c.restore()
        p.shader = LinearGradient(0f, l.beltCy - islandR, 0f, l.beltCy + islandR, Color.WHITE, Col.lighten(theme.wallLight, 0.35f), Shader.TileMode.CLAMP)
        c.drawPath(stadium(islandR), p); p.shader = null
        // gauge recess
        dst.set(gx0, l.beltCy - gh / 2, gx1, l.beltCy + gh / 2)
        p.color = 0xCCFFFFFF.toInt(); dst.offset(0f, 1.5f); c.drawRoundRect(dst, gh / 2, gh / 2, p); dst.offset(0f, -1.5f)
        p.shader = LinearGradient(0f, dst.top, 0f, dst.bottom, Col.mix(trackTop, frameDark, 0.3f), Col.mix(trackBottom, frameDark, 0.4f), Shader.TileMode.CLAMP)
        c.drawRoundRect(dst, gh / 2, gh / 2, p); p.shader = null
    }

    // ================================================================ frame

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f; val c3 = c1 + 1f
        val x = t - 1f
        return 1f + c3 * x * x * x + c1 * x * x
    }

    fun draw(c: Canvas) {
        val it = engine.introT
        val dur = GameEngine.INTRO_MACHINE
        val tm = (it / dur).coerceIn(0f, 1f)
        val boardScale = if (tm < 1f) 0.6f + 0.4f * easeOutBack(tm) else 1f
        val boardAlpha = (tm * 3f).coerceAtMost(1f)
        val tb = ((it - 0.08f) / dur).coerceIn(0f, 1f)
        val offB = if (tb < 1f) (1f - easeOutBack(tb)) * (l.worldH - baseTop + 60f) else 0f
        val pivotY = (l.gridTop + l.gridBottom) / 2f

        backdrop?.let { c.drawBitmap(it, 0f, 0f, null) }
        drawSparkles(c)
        base?.let { c.drawBitmap(it, 0f, (baseTop + offB) * scale, null) }
        board?.let {
            c.save()
            if (boardScale != 1f) c.scale(boardScale, boardScale, l.cx * scale, pivotY * scale)
            bmpPaint.alpha = (255 * boardAlpha).toInt()
            c.drawBitmap(it, 0f, boardLayerTop * scale, bmpPaint)
            bmpPaint.alpha = 255
            c.restore()
        }

        c.save()
        c.scale(scale, scale)
        if (engine.shake > 0f) {
            val s = engine.shake
            c.translate((Random.nextFloat() - 0.5f) * s, (Random.nextFloat() - 0.5f) * s)
        }
        c.save()
        if (boardScale != 1f) c.scale(boardScale, boardScale, l.cx, pivotY)
        drawBlocks(c)
        drawExiting(c)
        c.restore()
        c.save(); c.translate(0f, offB)
        drawBelt(c)
        drawCups(c)
        c.restore()
        drawLooseMarbles(c)
        drawFlying(c)
        drawParticles(c)
        drawTexts(c)
        c.restore()
        if (engine.dangerLevel > 0.02f) {
            val a = engine.dangerLevel * (0.6f + 0.4f * sin(engine.time * 5f))
            c.save(); c.scale(scale, scale)
            vignette.alpha = (a * 100).toInt().coerceIn(0, 255)
            c.drawRect(0f, 0f, l.worldW, l.worldH, vignette)
            c.restore()
        }
    }

    private fun marbleAt(c: Canvas, x: Float, y: Float, color: Int, squash: Float = 0f) {
        val r = l.marbleR
        val bmp = sprites.marbleShadowed(color, skin, r)
        if (squash > 0f) {
            c.save(); c.scale(1f + squash * 0.22f, 1f - squash * 0.22f, x, y + r)
            dst.set(x - r * 1.35f, y - r * 1.35f, x + r * 1.35f, y + r * 1.35f)
            c.drawBitmap(bmp, null, dst, bmpPaint); c.restore()
        } else {
            dst.set(x - r * 1.35f, y - r * 1.35f, x + r * 1.35f, y + r * 1.35f)
            c.drawBitmap(bmp, null, dst, bmpPaint)
        }
    }

    private fun drawSparkles(c: Canvas) {
        c.save(); c.scale(scale, scale)
        val t = engine.time
        for (i in 0 until sparkles.size / 4) {
            val k = (sin(t * 1.6f + sparkles[i * 4 + 3]) + 1f) * 0.5f
            if (k < 0.15f) continue
            Painter.star4(c, sparkles[i * 4], sparkles[i * 4 + 1], sparkles[i * 4 + 2] * (0.5f + 0.5f * k), Col.alpha(Color.WHITE, (k * 200).toInt()))
        }
        c.restore()
    }

    private fun drawBelt(c: Canvas) {
        val r = l.marbleR
        val sr = r * 1.02f
        val sock = sprites.socket(sr)
        val cap = engine.capacity
        for (i in 0 until cap) {
            l.beltPoint(engine.slotS(i), tmp)
            dst.set(tmp[0] - sr, tmp[1] - sr, tmp[0] + sr, tmp[1] + sr)
            c.drawBitmap(sock, null, dst, bmpPaint)
        }
        // rivets between sockets show the belt running even when it is empty
        if (cap > 1) {
            val half = (engine.slotS(1) - engine.slotS(0)) / 2f
            p.reset(); p.isAntiAlias = true
            for (i in 0 until cap) {
                l.beltPoint(engine.slotS(i) + half, tmp)
                p.color = 0xFF0B0822.toInt(); c.drawCircle(tmp[0], tmp[1] + 0.8f, 2.8f, p)
                p.color = 0xFFB9C2EE.toInt(); c.drawCircle(tmp[0], tmp[1], 2.3f, p)
            }
        }
        for (i in 0 until engine.slots.size) {
            val m = engine.slots[i] ?: continue
            if (m.state == MarbleState.BELT) marbleAt(c, m.x, m.y, m.color, m.squash)
        }
        // rollers
        val hub = sprites.hub(hubR, frame)
        val deg = engine.beltOffset / l.beltR * (180f / PI.toFloat())
        for (hx in floatArrayOf(l.xl, l.xr)) {
            c.save(); c.rotate(deg, hx, l.beltCy)
            dst.set(hx - hubR, l.beltCy - hubR, hx + hubR, l.beltCy + hubR)
            c.drawBitmap(hub, null, dst, bmpPaint)
            c.restore()
        }
        // fill gauge
        val ratio = (engine.beltCount / cap.toFloat()).coerceIn(0f, 1f)
        if (ratio > 0f) {
            p.reset(); p.isAntiAlias = true
            val inset = 2.5f
            val w = (gx1 - gx0 - inset * 2) * ratio
            val fh = gh - inset * 2
            dst.set(gx0 + inset, l.beltCy - fh / 2, gx0 + inset + w.coerceAtLeast(fh), l.beltCy + fh / 2)
            p.shader = when { ratio >= 0.78f -> gaugeRed; ratio >= 0.55f -> gaugeYellow; else -> gaugeGreen }
            if (ratio >= 0.78f) p.alpha = (200 + 55 * sin(engine.time * 9f)).toInt().coerceIn(0, 255)
            c.drawRoundRect(dst, fh / 2, fh / 2, p); p.shader = null
            p.color = 0x8CFFFFFF.toInt()
            c.drawRoundRect(dst.left + fh * 0.4f, dst.top + fh * 0.16f, (dst.right - fh * 0.4f).coerceAtLeast(dst.left + fh * 0.4f), dst.top + fh * 0.36f, fh * 0.1f, fh * 0.1f, p)
        }
        if (engine.beltCount != gaugeCount) { gaugeCount = engine.beltCount; gaugeLabel = "$gaugeCount / $cap" }
        Painter.text(c, gaugeLabel, l.cx, l.beltCy + 0.5f, gh * 0.95f, Color.WHITE, 0xFF22124F.toInt(), gh * 0.2f)
    }

    private fun variantOf(v: BlockVisual): Int {
        val k = v.state.spec.kind
        return when {
            k == BlockKind.RAINBOW -> 3
            k == BlockKind.MYSTERY && v.reveal < 0.5f -> 1
            k == BlockKind.GRAY && v.paint < 0.5f -> 2
            else -> 0
        }
    }

    private fun drawBlocks(c: Canvas) {
        for (v in engine.visuals) {
            if (v == null) continue
            drawBlock(c, v, l.cellLeft(v.col), l.cellTop(v.row + v.dropY), l.cell)
        }
    }

    private fun drawBlock(c: Canvas, v: BlockVisual, x0: Float, y: Float, s: Float) {
        val spec = v.state.spec
        val x = x0 + if (v.shake > 0f) sin(engine.time * 70f) * v.shake * s * 0.07f else 0f
        c.save()
        val wakeBump = sin(v.wake * PI.toFloat()) * 0.1f
        val sy = 1f - v.squash * 0.22f + v.crack * 0.05f + wakeBump
        val sx = 1f + v.squash * 0.12f + wakeBump
        var flipX = 1f
        if (spec.kind == BlockKind.MYSTERY && v.reveal in 0.001f..0.999f) flipX = abs(cos(v.reveal * PI.toFloat()))
        c.scale(sx * flipX.coerceAtLeast(0.05f), sy, x + s / 2, y + s * 0.9f)
        val variant = variantOf(v)
        dst.set(x, y, x + s, y + s)
        if (v.open < 0.99f) c.drawBitmap(sprites.block(spec.color, variant, s, skin, false), null, dst, bmpPaint)
        if (v.open > 0.01f) {
            bmpPaint.alpha = (255 * v.open).toInt().coerceIn(0, 255)
            c.drawBitmap(sprites.block(spec.color, variant, s, skin, true), null, dst, bmpPaint)
            bmpPaint.alpha = 255
        }
        if (spec.kind == BlockKind.GRAY && v.paint in 0.01f..0.99f) {
            bmpPaint.alpha = (255 * v.paint).toInt().coerceIn(0, 255)
            c.drawBitmap(sprites.block(spec.color, 0, s, skin, v.open > 0.5f), null, dst, bmpPaint)
            bmpPaint.alpha = 255
        }
        val locked = engine.board.isLocked(v.state)
        when (spec.kind) {
            BlockKind.KEY -> overlay(c, SpriteCache.OV_KEY, x, y, s)
            BlockKind.CHAINED -> if (locked) overlay(c, SpriteCache.OV_CHAINS, x, y, s) else if (v.unlock > 0f) overlayFade(c, SpriteCache.OV_CHAINS, x, y, s, v.unlock)
            BlockKind.COUNTER -> if (locked) {
                overlay(c, SpriteCache.OV_CAGE, x, y, s)
                Painter.badge(c, x + s / 2, y + s * 0.47f, s * 0.2f, engine.board.counterRemaining(v.state).toString(), 0xFF3D6BFF.toInt())
            } else if (v.unlock > 0f) overlayFade(c, SpriteCache.OV_CAGE, x, y, s, v.unlock)
            BlockKind.CRATE -> if (locked) {
                overlay(c, SpriteCache.OV_CRATE, x, y, s)
                Painter.badge(c, x + s / 2, y + s * 0.44f, s * 0.2f, engine.board.counterRemaining(v.state).toString(), 0xFFFF9F1C.toInt())
            } else if (v.unlock > 0f) overlayFade(c, SpriteCache.OV_CRATE, x, y, s, v.unlock)
            BlockKind.ICE -> overlay(c, if (v.state.cracked) SpriteCache.OV_ICE_CRACKED else SpriteCache.OV_ICE, x, y, s)
            BlockKind.ARROW -> overlay(c, SpriteCache.OV_ARROW, x, y, s, spec.param)
            BlockKind.DOUBLE -> Painter.badge(c, x + s * 0.8f, y + s * 0.2f, s * 0.16f, "x2", 0xFF3CCB5A.toInt())
            BlockKind.BOMB -> {
                val rem = engine.board.counterRemaining(v.state)
                val pulse = if (rem <= 3) 1f + 0.08f * sin(engine.time * 12f) else 1f
                c.save(); c.scale(pulse, pulse, x + s / 2, y + s / 2)
                overlay(c, SpriteCache.OV_BOMB, x, y, s)
                c.restore()
                Painter.badge(c, x + s * 0.78f, y + s * 0.24f, s * 0.16f, rem.toString(), if (rem <= 3) 0xFFFF4D5E.toInt() else 0xFF4A4E69.toInt())
            }
            BlockKind.PAINT -> overlay(c, SpriteCache.OV_BUCKET, x, y, s, spec.color)
            BlockKind.SWITCH -> overlay(c, SpriteCache.OV_SWITCH, x, y, s)
            else -> {}
        }
        if (v.pulse > 0f) {
            p.reset(); p.isAntiAlias = true; p.color = Col.alpha(0xFFFF3040.toInt(), (v.pulse * 100).toInt())
            dst.set(x + s * 0.06f, y + s * 0.06f, x + s * 0.94f, y + s * 0.9f)
            c.drawRoundRect(dst, s * 0.22f, s * 0.22f, p)
        }
        if (v.flash > 0f || v.wake > 0f) {
            p.reset(); p.isAntiAlias = true; p.color = Col.alpha(Color.WHITE, (maxOf(v.flash, v.wake * 0.6f) * 170).toInt())
            dst.set(x + s * 0.06f, y + s * 0.06f, x + s * 0.94f, y + s * 0.9f)
            c.drawRoundRect(dst, s * 0.22f, s * 0.22f, p)
        }
        c.restore()
    }

    private fun drawExiting(c: Canvas) {
        for (e in engine.exiting) {
            val t = e.t
            val sc = 1f + 0.18f * sin(t * PI.toFloat()) - 0.5f * t * t
            c.save(); c.scale(sc, sc, e.x + e.cell / 2, e.y + e.cell / 2)
            bmpPaint.alpha = ((1f - t) * 255).toInt().coerceIn(0, 255)
            dst.set(e.x, e.y, e.x + e.cell, e.y + e.cell)
            val variant = if (e.state.spec.kind == BlockKind.RAINBOW) 3 else 0
            c.drawBitmap(sprites.block(e.state.spec.color, variant, e.cell, skin, true), null, dst, bmpPaint)
            bmpPaint.alpha = 255
            c.restore()
        }
    }

    private fun overlay(c: Canvas, type: Int, x: Float, y: Float, s: Float, arg: Int = 0) {
        dst.set(x, y, x + s, y + s)
        c.drawBitmap(sprites.overlay(type, s, arg), null, dst, bmpPaint)
    }

    private fun overlayFade(c: Canvas, type: Int, x: Float, y: Float, s: Float, t: Float) {
        val k = 1f + (1f - t) * 0.5f
        val half = s * k / 2
        dst.set(x + s / 2 - half, y + s / 2 - half - (1 - t) * s * 0.3f, x + s / 2 + half, y + s / 2 + half - (1 - t) * s * 0.3f)
        bmpPaint.alpha = (255 * t).toInt()
        c.drawBitmap(sprites.overlay(type, s), null, dst, bmpPaint)
        bmpPaint.alpha = 255
    }

    private fun drawCups(c: Canvas) {
        val w = l.cupW; val h = l.cupH
        val mr = l.marbleR * 0.9f
        for ((ci, col) in engine.tray.withIndex()) {
            val cx = l.colCenter(ci)
            var last = col.cups.size - 1
            while (last > 0 && l.cupTop(col.cups[last].display) > l.worldH + 10f) last--
            for (idx in last downTo 0) {
                val cup = col.cups[idx]
                var y = l.cupTop(cup.display)
                c.save()
                var scale = 1f
                if (cup.popping) {
                    val t = cup.popT / 0.42f
                    scale = 1f + 0.3f * sin(t * PI.toFloat() * 0.5f)
                    y -= t * 55f
                    bmpPaint.alpha = ((1f - t) * 255).toInt().coerceIn(0, 255)
                }
                val bounce = sin(cup.bounce * PI.toFloat())
                y += bounce * 4f
                val sh = if (cup.shuffleT > 0f) 1f + sin(cup.shuffleT * PI.toFloat()) * 0.12f else 1f
                var flip = 1f
                if (cup.revealing) flip = abs(cos(cup.reveal * PI.toFloat())).coerceAtLeast(0.05f)
                c.scale(scale * sh * flip * (1f + bounce * 0.04f), scale * sh * (1f - bounce * 0.04f), cx, y + h)
                val hiddenNow = cup.hidden && cup.reveal < 0.5f
                dst.set(cx - w / 2, y, cx + w / 2, y + h)
                c.drawBitmap(sprites.cup(cup.color, hiddenNow, w, h), null, dst, bmpPaint)
                if (!hiddenNow) for (k in 0 until cup.arrived.coerceAtMost(3)) {
                    val mx = cx + l.dimpleDx[k]; val my = y + l.dimpleDy
                    dst.set(mx - mr, my - mr, mx + mr, my + mr)
                    c.drawBitmap(sprites.marble(cup.color, skin, mr), null, dst, bmpPaint)
                }
                bmpPaint.alpha = 255
                c.restore()
            }
        }
        // queue fog: rows behind the active one fade into the frosted cabinet
        c.drawRect(l.dishLeft + rimW, fogTop, l.dishRight - rimW, l.worldH + 2f, fogPaint)
    }

    private fun drawLooseMarbles(c: Canvas) {
        for (m in engine.marbles) {
            if (m.state == MarbleState.BELT) continue
            marbleAt(c, m.x, m.y, m.color)
        }
    }

    private fun drawFlying(c: Canvas) {
        for (f in engine.flying) {
            val s = f.cell * f.scale
            c.save()
            c.rotate(f.rot, f.x + s / 2, f.y + s / 2)
            val variant = if (f.state.spec.kind == BlockKind.RAINBOW) 3 else 0
            dst.set(f.x, f.y, f.x + s, f.y + s)
            c.drawBitmap(sprites.block(f.state.spec.color, variant, f.cell, skin, true), null, dst, bmpPaint)
            c.restore()
        }
    }

    private fun drawParticles(c: Canvas) {
        val ps = engine.particles
        p.reset(); p.isAntiAlias = true
        for (i in 0 until ps.count) {
            val t = ps.life[i] / ps.maxLife[i]
            val a = (255 * t.coerceIn(0f, 1f)).toInt()
            when (ps.type[i]) {
                Particles.DOT -> { p.style = Paint.Style.FILL; p.color = Col.alpha(ps.color[i], a); c.drawCircle(ps.x[i], ps.y[i], ps.size[i] * (0.4f + 0.6f * t), p) }
                Particles.STAR -> Painter.star4(c, ps.x[i], ps.y[i], ps.size[i] * (0.5f + 0.5f * t), Col.alpha(ps.color[i], a))
                Particles.CONFETTI -> {
                    p.style = Paint.Style.FILL; p.color = Col.alpha(ps.color[i], (255 * (t * 2).coerceAtMost(1f)).toInt())
                    c.save(); c.rotate(ps.rot[i], ps.x[i], ps.y[i])
                    val w = ps.size[i]; val hh = w * 0.55f * abs(cos(ps.rot[i] * 0.05f))
                    c.drawRect(ps.x[i] - w / 2, ps.y[i] - hh / 2, ps.x[i] + w / 2, ps.y[i] + hh / 2, p)
                    c.restore()
                }
                Particles.RING -> {
                    p.style = Paint.Style.STROKE; p.strokeWidth = 7f * t + 1f; p.color = Col.alpha(ps.color[i], (a * 0.9f).toInt())
                    c.drawCircle(ps.x[i], ps.y[i], ps.size[i] * (1.1f - t) * 0.9f, p)
                    p.style = Paint.Style.FILL
                }
                Particles.SHARD -> {
                    p.style = Paint.Style.FILL; p.color = Col.alpha(ps.color[i], a)
                    c.save(); c.rotate(ps.rot[i], ps.x[i], ps.y[i])
                    path.reset(); val s = ps.size[i]
                    path.moveTo(ps.x[i], ps.y[i] - s); path.lineTo(ps.x[i] + s * 0.6f, ps.y[i] + s * 0.5f); path.lineTo(ps.x[i] - s * 0.6f, ps.y[i] + s * 0.5f); path.close()
                    c.drawPath(path, p); c.restore()
                }
                Particles.SMOKE -> { p.style = Paint.Style.FILL; p.color = Col.alpha(ps.color[i], (a * 0.5f).toInt()); c.drawCircle(ps.x[i], ps.y[i], ps.size[i] * (1.6f - t), p) }
                else -> {}
            }
        }
    }

    private fun drawTexts(c: Canvas) {
        for (t in engine.texts) {
            val k = t.life / t.maxLife
            val sc = if (k < 0.15f) 0.5f + k / 0.15f * 0.6f else 1.1f - (k - 0.15f) * 0.1f
            val alpha = if (k > 0.7f) (1 - (k - 0.7f) / 0.3f) else 1f
            c.saveLayerAlpha(t.x - 200f, t.y - 60f, t.x + 200f, t.y + 60f, (alpha * 255).toInt())
            Painter.text(c, t.text, t.x, t.y, t.size * sc, t.color, 0xFF3A1C8A.toInt(), t.size * sc * 0.18f)
            c.restore()
        }
    }
}
