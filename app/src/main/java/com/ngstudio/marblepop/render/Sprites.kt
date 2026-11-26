package com.ngstudio.marblepop.render

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import com.ngstudio.marblepop.level.MarbleColors
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object Col {
    fun lighten(c: Int, f: Float): Int {
        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
        return Color.argb(Color.alpha(c), (r + (255 - r) * f).toInt(), (g + (255 - g) * f).toInt(), (b + (255 - b) * f).toInt())
    }
    fun darken(c: Int, f: Float): Int {
        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
        return Color.argb(Color.alpha(c), (r * (1 - f)).toInt(), (g * (1 - f)).toInt(), (b * (1 - f)).toInt())
    }
    fun alpha(c: Int, a: Int) = (c and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)
    fun mix(a: Int, b: Int, t: Float): Int {
        val u = 1 - t
        return Color.argb(
            (Color.alpha(a) * u + Color.alpha(b) * t).toInt(), (Color.red(a) * u + Color.red(b) * t).toInt(),
            (Color.green(a) * u + Color.green(b) * t).toInt(), (Color.blue(a) * u + Color.blue(b) * t).toInt(),
        )
    }
    val rainbow = intArrayOf(0xFFFF4D5E.toInt(), 0xFFFFD23F.toInt(), 0xFF3CCB5A.toInt(), 0xFF33C8F5.toInt(), 0xFF9B5CFF.toInt(), 0xFFFF7AC8.toInt(), 0xFFFF4D5E.toInt())
}

/** Procedural painters. All coordinates are in the canvas' current units. */
object Painter {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    fun colorOf(id: Int): Int = if (id == MarbleColors.RAINBOW) Color.WHITE else MarbleColors.argb[id]

    // ------------------------------------------------------------------ marbles

    fun marble(c: Canvas, cx: Float, cy: Float, r: Float, colorId: Int, skin: Int) =
        MachineArt.marble(c, cx, cy, r, colorId, skin)

    /** Grid block: a glass candy crate. variant: 0 normal, 1 mystery, 2 gray, 3 rainbow */
    @Suppress("UNUSED_PARAMETER")
    fun block(c: Canvas, x: Float, y: Float, s: Float, colorId: Int, variant: Int, bumps: Boolean = true, skin: Int = 0) =
        MachineArt.jelly(c, x, y, s, colorId, variant, bumps, skin)

    /** Tray cup: a cylindrical puck with 3 sockets. */
    fun cup(c: Canvas, x: Float, y: Float, w: Float, h: Float, colorId: Int, hidden: Boolean) =
        MachineArt.puck(c, x, y, w, h, colorId, hidden)

    // ------------------------------------------------------------------ icons / overlays

    var typeface: Typeface = Typeface.DEFAULT_BOLD
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    fun text(c: Canvas, s: String, cx: Float, cy: Float, size: Float, fill: Int, stroke: Int, strokeW: Float = size * 0.14f) {
        tp.typeface = typeface; tp.textSize = size
        val y = cy - (tp.descent() + tp.ascent()) / 2
        if (strokeW > 0) {
            tp.style = Paint.Style.STROKE; tp.strokeWidth = strokeW; tp.strokeJoin = Paint.Join.ROUND; tp.color = stroke
            c.drawText(s, cx, y, tp)
        }
        tp.style = Paint.Style.FILL; tp.color = fill
        c.drawText(s, cx, y, tp)
    }

    fun badge(c: Canvas, cx: Float, cy: Float, r: Float, number: String, bg: Int) {
        p.reset(); p.isAntiAlias = true
        p.color = Col.darken(bg, 0.4f); c.drawCircle(cx, cy + r * 0.12f, r, p)
        p.shader = LinearGradient(0f, cy - r, 0f, cy + r, intArrayOf(Col.lighten(bg, 0.25f), bg), null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = r * 0.14f; p.color = Color.WHITE; c.drawCircle(cx, cy, r * 0.93f, p); p.style = Paint.Style.FILL
        text(c, number, cx, cy, r * 1.15f, Color.WHITE, Col.darken(bg, 0.5f), r * 0.16f)
    }

    fun key(c: Canvas, cx: Float, cy: Float, s: Float) {
        val gold = 0xFFFFC93C.toInt(); val dark = 0xFFB07000.toInt()
        p.reset(); p.isAntiAlias = true
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = s * 0.2f; p.color = dark
        c.drawCircle(cx - s * 0.22f, cy, s * 0.2f, p)
        c.drawLine(cx, cy, cx + s * 0.45f, cy, p)
        c.drawLine(cx + s * 0.3f, cy, cx + s * 0.3f, cy + s * 0.18f, p)
        c.drawLine(cx + s * 0.44f, cy, cx + s * 0.44f, cy + s * 0.14f, p)
        p.strokeWidth = s * 0.12f; p.color = gold
        c.drawCircle(cx - s * 0.22f, cy, s * 0.2f, p)
        c.drawLine(cx, cy, cx + s * 0.45f, cy, p)
        c.drawLine(cx + s * 0.3f, cy, cx + s * 0.3f, cy + s * 0.18f, p)
        c.drawLine(cx + s * 0.44f, cy, cx + s * 0.44f, cy + s * 0.14f, p)
        p.style = Paint.Style.FILL; p.color = 0xCCFFFFFF.toInt()
        c.drawCircle(cx - s * 0.3f, cy - s * 0.08f, s * 0.04f, p)
    }

    fun padlock(c: Canvas, cx: Float, cy: Float, s: Float) {
        p.reset(); p.isAntiAlias = true
        p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.13f; p.color = 0xFF6B6F80.toInt()
        rect.set(cx - s * 0.22f, cy - s * 0.42f, cx + s * 0.22f, cy + s * 0.05f)
        c.drawArc(rect, 180f, 180f, false, p)
        p.style = Paint.Style.FILL
        rect.set(cx - s * 0.34f, cy - s * 0.15f, cx + s * 0.34f, cy + s * 0.36f)
        p.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, 0xFFFFD35A.toInt(), 0xFFE59A00.toInt(), Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, s * 0.08f, s * 0.08f, p); p.shader = null
        p.color = 0xFF7A4A00.toInt()
        c.drawCircle(cx, cy + s * 0.06f, s * 0.07f, p)
        rect.set(cx - s * 0.03f, cy + s * 0.06f, cx + s * 0.03f, cy + s * 0.22f); c.drawRect(rect, p)
    }

    fun chains(c: Canvas, x: Float, y: Float, s: Float) {
        p.reset(); p.isAntiAlias = true; p.style = Paint.Style.STROKE
        for (diag in 0..1) {
            c.save()
            c.rotate(if (diag == 0) 45f else -45f, x + s / 2, y + s / 2)
            val n = 5
            for (i in 0 until n) {
                val lx = x - s * 0.1f + i * s * 0.3f
                rect.set(lx, y + s / 2 - s * 0.08f, lx + s * 0.26f, y + s / 2 + s * 0.08f)
                p.strokeWidth = s * 0.06f; p.color = 0xFF4A4E5E.toInt(); c.drawRoundRect(rect, s * 0.08f, s * 0.08f, p)
                p.strokeWidth = s * 0.035f; p.color = 0xFFC9CEDB.toInt(); c.drawRoundRect(rect, s * 0.08f, s * 0.08f, p)
            }
            c.restore()
        }
        p.style = Paint.Style.FILL
    }

    fun cage(c: Canvas, x: Float, y: Float, s: Float) {
        p.reset(); p.isAntiAlias = true
        p.color = 0x553A3F55
        rect.set(x + s * 0.05f, y + s * 0.05f, x + s * 0.95f, y + s * 0.88f); c.drawRoundRect(rect, s * 0.18f, s * 0.18f, p)
        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        for (i in 1..4) {
            val bx = x + s * (0.05f + 0.18f * i)
            p.strokeWidth = s * 0.06f; p.color = 0xFF3B3F52.toInt(); c.drawLine(bx, y + s * 0.08f, bx, y + s * 0.85f, p)
            p.strokeWidth = s * 0.03f; p.color = 0xFFD7DBE6.toInt(); c.drawLine(bx - s * 0.008f, y + s * 0.1f, bx - s * 0.008f, y + s * 0.83f, p)
        }
        p.style = Paint.Style.FILL
    }

    fun crate(c: Canvas, x: Float, y: Float, s: Float) {
        p.reset(); p.isAntiAlias = true
        val pad = s * 0.05f
        p.color = 0xFF7A4A22.toInt(); rect.set(x + pad, y + pad + s * 0.07f, x + s - pad, y + s - pad); c.drawRoundRect(rect, s * 0.12f, s * 0.12f, p)
        rect.set(x + pad, y + pad, x + s - pad, y + s - pad - s * 0.07f)
        p.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, 0xFFE0A35C.toInt(), 0xFFC07A36.toInt(), Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, s * 0.12f, s * 0.12f, p); p.shader = null
        p.color = 0xFF8F5526.toInt()
        for (k in 1..2) { val ly = rect.top + rect.height() * k / 3f; c.drawRect(rect.left + s * 0.06f, ly - s * 0.012f, rect.right - s * 0.06f, ly + s * 0.012f, p) }
        p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.09f; p.color = 0xFFA8692F.toInt()
        c.drawLine(rect.left + s * 0.12f, rect.top + s * 0.12f, rect.right - s * 0.12f, rect.bottom - s * 0.12f, p)
        p.strokeWidth = s * 0.05f; p.color = 0xFF6E3F18.toInt()
        val inset = RectF(rect); inset.inset(s * 0.06f, s * 0.06f); c.drawRoundRect(inset, s * 0.08f, s * 0.08f, p)
        p.style = Paint.Style.FILL
        p.color = 0xFF55606E.toInt()
        for ((nx, ny) in listOf(0.18f to 0.18f, 0.82f to 0.18f, 0.18f to 0.75f, 0.82f to 0.75f)) c.drawCircle(x + s * nx, y + s * ny, s * 0.035f, p)
    }

    fun ice(c: Canvas, x: Float, y: Float, s: Float, cracked: Boolean) {
        p.reset(); p.isAntiAlias = true
        rect.set(x + s * 0.03f, y + s * 0.03f, x + s * 0.97f, y + s * 0.9f)
        p.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, intArrayOf(0xCCEAFBFF.toInt(), 0x99B6E8FF.toInt(), 0xBBE2F7FF.toInt()), null, Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, s * 0.2f, s * 0.2f, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.03f; p.color = 0xEEFFFFFF.toInt()
        c.drawRoundRect(rect, s * 0.2f, s * 0.2f, p)
        p.strokeWidth = s * 0.05f; p.strokeCap = Paint.Cap.ROUND; p.color = 0xBBFFFFFF.toInt()
        c.drawLine(x + s * 0.2f, y + s * 0.25f, x + s * 0.4f, y + s * 0.15f, p)
        c.drawLine(x + s * 0.18f, y + s * 0.38f, x + s * 0.28f, y + s * 0.33f, p)
        if (cracked) {
            p.strokeWidth = s * 0.025f; p.color = 0xFF7FB6D6.toInt()
            path.reset()
            path.moveTo(x + s * 0.5f, y + s * 0.45f); path.lineTo(x + s * 0.3f, y + s * 0.6f); path.lineTo(x + s * 0.2f, y + s * 0.85f)
            path.moveTo(x + s * 0.5f, y + s * 0.45f); path.lineTo(x + s * 0.72f, y + s * 0.3f); path.lineTo(x + s * 0.9f, y + s * 0.25f)
            path.moveTo(x + s * 0.5f, y + s * 0.45f); path.lineTo(x + s * 0.62f, y + s * 0.7f); path.lineTo(x + s * 0.8f, y + s * 0.8f)
            path.moveTo(x + s * 0.5f, y + s * 0.45f); path.lineTo(x + s * 0.42f, y + s * 0.15f)
            c.drawPath(path, p)
        }
        p.style = Paint.Style.FILL
    }

    fun arrowIcon(c: Canvas, cx: Float, cy: Float, s: Float, dir: Int) {
        p.reset(); p.isAntiAlias = true
        p.color = 0x55000000; c.drawCircle(cx, cy + s * 0.04f, s * 0.36f, p)
        p.color = Color.WHITE; c.drawCircle(cx, cy, s * 0.34f, p)
        c.save()
        c.rotate(when (dir) { 0 -> 180f; 1 -> 0f; else -> -90f }, cx, cy)
        path.reset()
        path.moveTo(cx + s * 0.22f, cy); path.lineTo(cx, cy - s * 0.2f); path.lineTo(cx, cy - s * 0.08f)
        path.lineTo(cx - s * 0.2f, cy - s * 0.08f); path.lineTo(cx - s * 0.2f, cy + s * 0.08f); path.lineTo(cx, cy + s * 0.08f)
        path.lineTo(cx, cy + s * 0.2f); path.close()
        p.color = 0xFF4B25B8.toInt(); c.drawPath(path, p)
        c.restore()
    }

    fun bomb(c: Canvas, cx: Float, cy: Float, s: Float, fuse: Float) {
        p.reset(); p.isAntiAlias = true
        p.shader = RadialGradient(cx - s * 0.12f, cy - s * 0.1f, s * 0.5f, intArrayOf(0xFF6B6F85.toInt(), 0xFF2A2C3A.toInt()), null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy + s * 0.05f, s * 0.3f, p); p.shader = null
        p.color = 0xFF3A3D4F.toInt(); c.drawRect(cx + s * 0.08f, cy - s * 0.3f, cx + s * 0.22f, cy - s * 0.18f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.04f; p.color = 0xFFC8A060.toInt(); p.strokeCap = Paint.Cap.ROUND
        c.drawLine(cx + s * 0.15f, cy - s * 0.3f, cx + s * 0.25f, cy - s * 0.42f, p)
        p.style = Paint.Style.FILL
        p.color = 0xFFFFD23F.toInt(); c.drawCircle(cx + s * 0.27f, cy - s * 0.44f, s * (0.06f + 0.03f * fuse), p)
        p.color = 0xFFFF6A1A.toInt(); c.drawCircle(cx + s * 0.27f, cy - s * 0.44f, s * 0.035f, p)
        p.color = 0x99FFFFFF.toInt(); c.drawCircle(cx - s * 0.12f, cy - s * 0.06f, s * 0.07f, p)
    }

    fun bucket(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        p.reset(); p.isAntiAlias = true
        path.reset()
        path.moveTo(cx - s * 0.26f, cy - s * 0.18f); path.lineTo(cx + s * 0.26f, cy - s * 0.18f)
        path.lineTo(cx + s * 0.2f, cy + s * 0.3f); path.lineTo(cx - s * 0.2f, cy + s * 0.3f); path.close()
        p.color = 0xFFE9ECF5.toInt(); c.drawPath(path, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.04f; p.color = 0xFF6B6F85.toInt(); c.drawPath(path, p)
        rect.set(cx - s * 0.26f, cy - s * 0.42f, cx + s * 0.26f, cy + s * 0.06f); c.drawArc(rect, 180f, 180f, false, p)
        p.style = Paint.Style.FILL
        p.color = color
        rect.set(cx - s * 0.26f, cy - s * 0.25f, cx + s * 0.26f, cy - s * 0.1f); c.drawOval(rect, p)
        c.drawCircle(cx + s * 0.16f, cy - s * 0.02f, s * 0.06f, p)
        c.drawCircle(cx + s * 0.18f, cy + s * 0.1f, s * 0.045f, p)
    }

    fun switchIcon(c: Canvas, cx: Float, cy: Float, s: Float) {
        p.reset(); p.isAntiAlias = true
        p.color = 0x55000000; c.drawCircle(cx, cy + s * 0.04f, s * 0.36f, p)
        p.color = Color.WHITE; c.drawCircle(cx, cy, s * 0.34f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.07f; p.color = 0xFF4B25B8.toInt(); p.strokeCap = Paint.Cap.ROUND
        rect.set(cx - s * 0.2f, cy - s * 0.2f, cx + s * 0.2f, cy + s * 0.2f)
        c.drawArc(rect, 200f, 120f, false, p); c.drawArc(rect, 20f, 120f, false, p)
        p.style = Paint.Style.FILL
        for (k in 0..1) {
            val a = Math.toRadians(if (k == 0) 320.0 else 140.0)
            val ax = cx + cos(a).toFloat() * s * 0.2f; val ay = cy + sin(a).toFloat() * s * 0.2f
            c.drawCircle(ax, ay, s * 0.06f, p)
        }
    }

    fun star4(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        path.reset()
        path.moveTo(cx, cy - r)
        path.quadTo(cx, cy, cx + r, cy); path.quadTo(cx, cy, cx, cy + r)
        path.quadTo(cx, cy, cx - r, cy); path.quadTo(cx, cy, cx, cy - r)
        p.reset(); p.isAntiAlias = true; p.color = color
        c.drawPath(path, p)
    }
}

/** Bitmap cache so frame rendering is just blits. */
class SpriteCache(val pxPerUnit: Float) {
    private val map = HashMap<Long, Bitmap>()

    private fun key(type: Int, a: Int, b: Int, size: Int) = (type.toLong() shl 48) or ((a.toLong() and 0xFFF) shl 32) or ((b.toLong() and 0xFFFF) shl 16) or (size.toLong() and 0xFFFF)

    private fun make(w: Int, h: Int, draw: (Canvas) -> Unit): Bitmap {
        val bmp = Bitmap.createBitmap(max(1, w), max(1, h), Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp))
        return bmp
    }

    private val SS = 1.6f // supersampling for crisp marbles

    fun marble(colorId: Int, skin: Int, radiusUnits: Float): Bitmap {
        val px = (radiusUnits * 2 * pxPerUnit * SS).toInt().coerceAtLeast(8)
        return map.getOrPut(key(1, colorId, skin, px)) {
            make(px, px) { MachineArt.marble(it, px / 2f, px / 2f, px / 2f - 0.5f, colorId, skin) }
        }
    }

    fun marbleBody(colorId: Int, skin: Int, radiusUnits: Float): Bitmap {
        val px = (radiusUnits * 2 * pxPerUnit * SS).toInt().coerceAtLeast(8)
        return map.getOrPut(key(4, colorId, skin, px)) {
            make(px, px) { MachineArt.marbleBody(it, px / 2f, px / 2f, px / 2f - 0.5f, colorId, skin) }
        }
    }

    fun marbleGloss(colorId: Int, skin: Int, radiusUnits: Float): Bitmap {
        val px = (radiusUnits * 2 * pxPerUnit * SS).toInt().coerceAtLeast(8)
        return map.getOrPut(key(5, colorId, skin, px)) {
            make(px, px) { MachineArt.marbleGloss(it, px / 2f, px / 2f, px / 2f - 0.5f, colorId, skin) }
        }
    }

    /** Soft shadow sprite; drawn at 2x the marble radius footprint. */
    fun shadow(radiusUnits: Float): Bitmap {
        val px = (radiusUnits * 3 * pxPerUnit).toInt().coerceAtLeast(8)
        return map.getOrPut(key(6, 0, 0, px)) {
            make(px, px) { MachineArt.shadow(it, px / 2f, px / 2f, px * 0.36f, px * 0.24f, 150) }
        }
    }

    fun block(colorId: Int, variant: Int, sizeUnits: Float, skin: Int = 0, open: Boolean = true): Bitmap {
        val px = (sizeUnits * pxPerUnit).toInt().coerceAtLeast(8)
        return map.getOrPut(key(2, colorId + variant * 100 + (if (open) 1000 else 0), skin, px)) {
            make(px, px) { MachineArt.jelly(it, 0f, 0f, px.toFloat(), colorId, variant, open, skin) }
        }
    }

    /** Marble with its soft contact shadow baked in; draw into a box of ±1.35 r around the centre. */
    fun marbleShadowed(colorId: Int, skin: Int, radiusUnits: Float): Bitmap {
        val px = (radiusUnits * 2.7f * pxPerUnit * SS).toInt().coerceAtLeast(8)
        return map.getOrPut(key(9, colorId, skin, px)) {
            make(px, px) {
                val r = px / 2.7f
                MachineArt.shadow(it, px / 2f + r * 0.12f, px / 2f + r * 0.55f, r * 0.85f, r * 0.42f, 120)
                MachineArt.marble(it, px / 2f, px / 2f, r - 0.5f, colorId, skin)
            }
        }
    }

    fun cup(colorId: Int, hidden: Boolean, wUnits: Float, hUnits: Float, holeUnits: Float): Bitmap {
        val w = (wUnits * pxPerUnit).toInt(); val h = (hUnits * pxPerUnit).toInt()
        val hole = holeUnits * pxPerUnit
        return map.getOrPut(key(3, colorId, (if (hidden) 1 else 0) or (hole.toInt() shl 1), w * 7 + h)) {
            make(w, h) { MachineArt.puck(it, 0f, 0f, w.toFloat(), h.toFloat(), colorId, hidden, hole) }
        }
    }

    fun gear(radiusUnits: Float, tint: Int): Bitmap {
        val px = (radiusUnits * 2 * pxPerUnit * 1.2f).toInt().coerceAtLeast(8)
        return map.getOrPut(key(7, 0, tint and 0xFFFF, px)) { make(px, px) { MachineArt.gear(it, px / 2f, px / 2f, px / 2f * 0.92f, 12, tint) } }
    }

    fun cradle(lenUnits: Float, widUnits: Float): Bitmap {
        val w = (lenUnits * pxPerUnit * 1.4f).toInt().coerceAtLeast(4); val h = (widUnits * pxPerUnit * 1.4f).toInt().coerceAtLeast(4)
        return map.getOrPut(key(8, w, h, 0)) { make(w, h) { MachineArt.cradle(it, w / 2f, h / 2f, w.toFloat() - 1f, h.toFloat() - 1f) } }
    }

    /** Belt socket; draw into a box of ±r around the slot centre. */
    fun socket(radiusUnits: Float): Bitmap {
        val px = (radiusUnits * 2 * pxPerUnit * SS).toInt().coerceAtLeast(8)
        return map.getOrPut(key(20, 0, 0, px)) { make(px, px) { MachineArt.socket(it, px / 2f, px / 2f, px / 2f - 0.5f) } }
    }

    /** Roller hub; draw into a box of ±r around its centre and rotate the canvas. */
    fun hub(radiusUnits: Float, tint: Int): Bitmap {
        val px = (radiusUnits * 2 * pxPerUnit * SS).toInt().coerceAtLeast(8)
        return map.getOrPut(key(21, 0, tint and 0xFFFF, px)) { make(px, px) { MachineArt.hub(it, px / 2f, px / 2f, px / 2f - 0.5f, tint) } }
    }

    /** Overlay sprite: type ids 10.. */
    fun overlay(type: Int, sizeUnits: Float, arg: Int = 0): Bitmap {
        val px = (sizeUnits * pxPerUnit).toInt().coerceAtLeast(8)
        return map.getOrPut(key(type, arg, 0, px)) {
            make(px, px) { c ->
                val s = px.toFloat()
                when (type) {
                    OV_CHAINS -> { Painter.chains(c, 0f, 0f, s); Painter.padlock(c, s / 2, s / 2, s * 0.42f) }
                    OV_CAGE -> Painter.cage(c, 0f, 0f, s)
                    OV_CRATE -> Painter.crate(c, 0f, 0f, s)
                    OV_ICE -> Painter.ice(c, 0f, 0f, s, false)
                    OV_ICE_CRACKED -> Painter.ice(c, 0f, 0f, s, true)
                    OV_KEY -> Painter.key(c, s / 2, s * 0.44f, s * 0.6f)
                    OV_ARROW -> Painter.arrowIcon(c, s / 2, s * 0.45f, s * 0.7f, arg)
                    OV_BOMB -> Painter.bomb(c, s / 2, s * 0.48f, s * 0.9f, 1f)
                    OV_BUCKET -> Painter.bucket(c, s / 2, s * 0.46f, s * 0.75f, Painter.colorOf(arg))
                    OV_SWITCH -> Painter.switchIcon(c, s / 2, s * 0.45f, s * 0.7f)
                }
            }
        }
    }

    fun clear() { map.values.forEach { it.recycle() }; map.clear() }

    companion object {
        const val OV_CHAINS = 10; const val OV_CAGE = 11; const val OV_CRATE = 12; const val OV_ICE = 13; const val OV_ICE_CRACKED = 14
        const val OV_KEY = 15; const val OV_ARROW = 16; const val OV_BOMB = 17; const val OV_BUCKET = 18; const val OV_SWITCH = 19
    }
}
