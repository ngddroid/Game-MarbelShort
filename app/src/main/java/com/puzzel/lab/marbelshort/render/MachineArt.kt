package com.puzzel.lab.marbelshort.render

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
import com.puzzel.lab.marbelshort.level.MarbleColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * High quality procedural art for the factory-machine playground.
 * Marbles are split into a rotating body layer (glass + inner swirl) and a fixed
 * lighting layer (specular, caustic, rim) so they look like they really roll.
 */
object MachineArt {
    /** Cup hole centres sit at x ± w * HOLE_SPREAD (wide enough for belt-size marbles in 5 columns). */
    const val HOLE_SPREAD = 0.29f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val path = Path()
    private val path2 = Path()
    private val rect = RectF()

    val CHROME = intArrayOf(0xFFFFFFFF.toInt(), 0xFFD9DEE8.toInt(), 0xFF9AA3B5.toInt(), 0xFFC9CFDB.toInt(), 0xFF7D8699.toInt())
    val CHROME_POS = floatArrayOf(0f, 0.25f, 0.55f, 0.75f, 1f)

    private fun reset() { p.reset(); p.isAntiAlias = true; p.isFilterBitmap = true }

    // =================================================================== marbles

    fun marble(c: Canvas, cx: Float, cy: Float, r: Float, colorId: Int, skin: Int) {
        marbleBody(c, cx, cy, r, colorId, skin)
        marbleGloss(c, cx, cy, r, colorId, skin)
    }

    fun marbleArgb(colorId: Int): Int = if (colorId == MarbleColors.RAINBOW) 0xFFB070FF.toInt() else MarbleColors.argb[colorId]

    /** Rotating layer. */
    fun marbleBody(c: Canvas, cx: Float, cy: Float, r: Float, colorId: Int, skin: Int, argbOverride: Int? = null) {
        val base = argbOverride ?: marbleArgb(colorId)
        val rainbow = colorId == MarbleColors.RAINBOW && argbOverride == null
        reset()
        c.save()
        path.reset(); path.addCircle(cx, cy, r, Path.Direction.CW); c.clipPath(path)
        if (rainbow) {
            p.alpha = 255; p.shader = SweepGradient(cx, cy, Col.rainbow, null)
            c.drawCircle(cx, cy, r, p)
            vane(c, cx, cy, r, Color.WHITE, 0xFF6A3FB0.toInt(), 0.24f, 200)
        } else when (skin) {
            1 -> { // crystal glass: clear body with thin white spiral
                p.alpha = 255; p.shader = RadialGradient(cx - r * 0.2f, cy - r * 0.25f, r * 1.3f,
                    intArrayOf(Col.lighten(base, 0.65f), Col.lighten(base, 0.15f), Col.darken(base, 0.45f)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                c.drawCircle(cx, cy, r, p)
                p.shader = null; p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
                for (k in 0 until 3) {
                    p.strokeWidth = r * (0.12f - k * 0.03f); p.color = Col.alpha(Color.WHITE, 170 - k * 40)
                    rect.set(cx - r * (0.25f + k * 0.22f), cy - r * (0.25f + k * 0.22f), cx + r * (0.25f + k * 0.22f), cy + r * (0.25f + k * 0.22f))
                    c.drawArc(rect, k * 110f, 150f, false, p)
                }
                p.style = Paint.Style.FILL
                bubbles(c, cx, cy, r, colorId)
            }
            2 -> { // candy: white with spiral colour stripes
                p.alpha = 255; p.shader = RadialGradient(cx - r * 0.2f, cy - r * 0.25f, r * 1.3f, intArrayOf(Color.WHITE, Col.lighten(base, 0.8f), Col.lighten(base, 0.4f)), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
                c.drawCircle(cx, cy, r, p)
                p.shader = null; p.color = base
                for (k in 0 until 5) {
                    path2.reset()
                    val a0 = k * 2 * PI.toFloat() / 5
                    path2.moveTo(cx, cy)
                    for (t in 0..12) {
                        val tt = t / 12f
                        val a = a0 + tt * 2.2f
                        path2.lineTo(cx + cos(a) * r * 1.1f * tt, cy + sin(a) * r * 1.1f * tt)
                    }
                    for (t in 12 downTo 0) {
                        val tt = t / 12f
                        val a = a0 + 0.38f + tt * 2.2f
                        path2.lineTo(cx + cos(a) * r * 1.1f * tt, cy + sin(a) * r * 1.1f * tt)
                    }
                    path2.close()
                    c.drawPath(path2, p)
                }
            }
            3 -> { // galaxy
                p.alpha = 255; p.shader = RadialGradient(cx + r * 0.15f, cy + r * 0.2f, r * 1.25f, intArrayOf(Col.darken(base, 0.25f), Col.darken(base, 0.7f), 0xFF0B0820.toInt()), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                c.drawCircle(cx, cy, r, p)
                p.alpha = 255; p.shader = RadialGradient(cx - r * 0.3f, cy + r * 0.15f, r * 0.75f, intArrayOf(Col.alpha(Col.lighten(base, 0.35f), 210), 0x00000000), null, Shader.TileMode.CLAMP)
                c.drawCircle(cx, cy, r, p)
                p.alpha = 255; p.shader = RadialGradient(cx + r * 0.35f, cy - r * 0.2f, r * 0.5f, intArrayOf(0x88FF7AE0.toInt(), 0x00000000), null, Shader.TileMode.CLAMP)
                c.drawCircle(cx, cy, r, p)
                p.shader = null
                val rnd = Random(colorId * 31 + 7)
                repeat(14) {
                    val a = rnd.nextFloat() * 6.28f; val d = rnd.nextFloat() * r * 0.85f
                    p.color = Col.alpha(Color.WHITE, 120 + rnd.nextInt(135))
                    c.drawCircle(cx + cos(a) * d, cy + sin(a) * d, r * (0.02f + rnd.nextFloat() * 0.05f), p)
                }
            }
            4 -> { // chrome metal
                p.alpha = 255; p.shader = LinearGradient(cx, cy - r, cx, cy + r,
                    intArrayOf(Col.lighten(base, 0.9f), Col.lighten(base, 0.35f), Col.darken(base, 0.4f), Col.lighten(base, 0.55f), Col.darken(base, 0.5f)),
                    floatArrayOf(0f, 0.32f, 0.5f, 0.68f, 1f), Shader.TileMode.CLAMP)
                c.drawCircle(cx, cy, r, p)
            }
            5 -> { // neon
                p.alpha = 255; p.shader = RadialGradient(cx, cy, r, intArrayOf(0xFF1A1230.toInt(), 0xFF120C24.toInt(), Col.darken(base, 0.1f)), floatArrayOf(0f, 0.7f, 1f), Shader.TileMode.CLAMP)
                c.drawCircle(cx, cy, r, p)
                p.shader = null; p.style = Paint.Style.STROKE
                p.strokeWidth = r * 0.2f; p.color = Col.alpha(base, 140); c.drawCircle(cx, cy, r * 0.72f, p)
                p.strokeWidth = r * 0.08f; p.color = Col.lighten(base, 0.6f); c.drawCircle(cx, cy, r * 0.72f, p)
                p.strokeWidth = r * 0.05f; p.color = Col.alpha(Col.lighten(base, 0.3f), 200)
                rect.set(cx - r * 0.42f, cy - r * 0.42f, cx + r * 0.42f, cy + r * 0.42f); c.drawArc(rect, 30f, 240f, false, p)
                p.style = Paint.Style.FILL
            }
            6 -> { // smiley
                classicBody(c, cx, cy, r, base, swirl = false)
                p.shader = null; p.color = Col.darken(base, 0.75f)
                rect.set(cx - r * 0.38f, cy - r * 0.22f, cx - r * 0.16f, cy + r * 0.1f); c.drawOval(rect, p)
                rect.set(cx + r * 0.16f, cy - r * 0.22f, cx + r * 0.38f, cy + r * 0.1f); c.drawOval(rect, p)
                p.color = Color.WHITE
                c.drawCircle(cx - r * 0.3f, cy - r * 0.13f, r * 0.05f, p); c.drawCircle(cx + r * 0.24f, cy - r * 0.13f, r * 0.05f, p)
                p.color = Col.darken(base, 0.75f)
                p.style = Paint.Style.STROKE; p.strokeWidth = r * 0.1f; p.strokeCap = Paint.Cap.ROUND
                rect.set(cx - r * 0.3f, cy - r * 0.05f, cx + r * 0.3f, cy + r * 0.42f); c.drawArc(rect, 25f, 130f, false, p)
                p.style = Paint.Style.FILL
                p.color = Col.alpha(0xFFFF5A8A.toInt(), 110)
                c.drawCircle(cx - r * 0.55f, cy + r * 0.18f, r * 0.13f, p); c.drawCircle(cx + r * 0.55f, cy + r * 0.18f, r * 0.13f, p)
            }
            else -> classicBody(c, cx, cy, r, base, swirl = true, seed = colorId)
        }
        // depth vignette (thick glass)
        reset()
        p.alpha = 255; p.shader = RadialGradient(cx - r * 0.12f, cy - r * 0.18f, r * 1.18f,
            intArrayOf(0x00000000, 0x00000000, Col.alpha(Col.darken(base, 0.6f), if (skin == 5) 50 else 85)), floatArrayOf(0f, 0.65f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
        c.restore()
    }

    private fun classicBody(c: Canvas, cx: Float, cy: Float, r: Float, base: Int, swirl: Boolean, seed: Int = 0) {
        p.alpha = 255; p.shader = RadialGradient(cx - r * 0.22f, cy - r * 0.28f, r * 1.4f,
            intArrayOf(Col.lighten(base, 0.55f), base, Col.darken(base, 0.28f)), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
        p.alpha = 255; p.shader = RadialGradient(cx, cy + r * 0.1f, r * 0.75f, intArrayOf(Col.alpha(Col.lighten(base, 0.55f), 120), Col.alpha(base, 0)), null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
        p.shader = null
        if (swirl) {
            // cat's-eye vanes inside the glass
            vane(c, cx, cy, r, Col.lighten(base, 0.85f), Col.darken(base, 0.35f), 0.2f, 215)
            c.save(); c.rotate(90f, cx, cy)
            vane(c, cx, cy, r * 0.8f, Col.lighten(base, 0.5f), Col.darken(base, 0.2f), 0.13f, 150)
            c.restore()
            bubbles(c, cx, cy, r, seed)
        }
    }

    private fun vane(c: Canvas, cx: Float, cy: Float, r: Float, light: Int, dark: Int, width: Float, alpha: Int) {
        path2.reset()
        path2.moveTo(cx - r * 0.95f, cy + r * 0.12f)
        path2.cubicTo(cx - r * 0.35f, cy - r * (0.55f + width), cx + r * 0.35f, cy + r * (0.55f - width), cx + r * 0.95f, cy - r * 0.12f)
        path2.cubicTo(cx + r * 0.35f, cy + r * (0.55f + width), cx - r * 0.35f, cy - r * (0.55f - width), cx - r * 0.95f, cy + r * 0.12f)
        path2.close()
        p.reset(); p.isAntiAlias = true
        p.alpha = 255; p.shader = LinearGradient(cx, cy - r * 0.4f, cx, cy + r * 0.4f, intArrayOf(Col.alpha(light, alpha), Col.alpha(Col.mix(light, dark, 0.5f), alpha), Col.alpha(dark, alpha)), null, Shader.TileMode.CLAMP)
        c.drawPath(path2, p)
        p.shader = null
    }

    private fun bubbles(c: Canvas, cx: Float, cy: Float, r: Float, seed: Int) {
        val rnd = Random(seed * 13 + 5)
        p.shader = null
        repeat(3) {
            val a = rnd.nextFloat() * 6.28f; val d = r * (0.25f + rnd.nextFloat() * 0.45f)
            val br = r * (0.04f + rnd.nextFloat() * 0.04f)
            p.style = Paint.Style.FILL; p.color = 0x55FFFFFF
            c.drawCircle(cx + cos(a) * d, cy + sin(a) * d, br, p)
            p.color = 0xAAFFFFFF.toInt()
            c.drawCircle(cx + cos(a) * d - br * 0.35f, cy + sin(a) * d - br * 0.35f, br * 0.35f, p)
        }
    }

    /** Fixed lighting layer — never rotates. */
    fun marbleGloss(c: Canvas, cx: Float, cy: Float, r: Float, colorId: Int, skin: Int, argbOverride: Int? = null) {
        val base = argbOverride ?: marbleArgb(colorId)
        reset()
        c.save()
        path.reset(); path.addCircle(cx, cy, r, Path.Direction.CW); c.clipPath(path)
        // caustic: light focused through the glass onto the lower side
        p.alpha = 255; p.shader = RadialGradient(cx + r * 0.2f, cy + r * 0.55f, r * 0.62f,
            intArrayOf(Col.alpha(Col.lighten(base, 0.8f), if (skin == 5) 70 else 215), Col.alpha(Col.lighten(base, 0.4f), 60), 0x00FFFFFF), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
        c.restore()
        // rim light
        p.shader = null; p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = r * 0.09f; p.color = 0x70FFFFFF
        rect.set(cx - r * 0.86f, cy - r * 0.86f, cx + r * 0.86f, cy + r * 0.86f)
        c.drawArc(rect, 15f, 75f, false, p)
        // definition outline
        p.strokeWidth = max(1f, r * 0.06f); p.color = Col.alpha(Col.darken(base, 0.5f), 170)
        c.drawCircle(cx, cy, r - p.strokeWidth / 2, p)
        p.style = Paint.Style.FILL
        // main soft specular
        c.save()
        c.rotate(-35f, cx - r * 0.32f, cy - r * 0.42f)
        rect.set(cx - r * 0.72f, cy - r * 0.68f, cx + r * 0.05f, cy - r * 0.2f)
        p.alpha = 255; p.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, intArrayOf(0xF8FFFFFF.toInt(), 0x66FFFFFF, 0x00FFFFFF), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawOval(rect, p)
        c.restore()
        p.shader = null
        // sharp window glint
        p.color = Color.WHITE
        rect.set(cx - r * 0.5f, cy - r * 0.56f, cx - r * 0.28f, cy - r * 0.42f)
        c.drawOval(rect, p)
        p.color = 0xB0FFFFFF.toInt()
        c.drawCircle(cx + r * 0.5f, cy + r * 0.32f, r * 0.07f, p)
    }

    /** Soft blurred contact shadow (software canvas only). */
    fun shadow(c: Canvas, cx: Float, cy: Float, rx: Float, ry: Float, alpha: Int) {
        reset()
        p.color = Col.alpha(0xFF0A0420.toInt(), alpha)
        p.maskFilter = BlurMaskFilter(max(1f, ry * 0.6f), BlurMaskFilter.Blur.NORMAL)
        rect.set(cx - rx, cy - ry, cx + rx, cy + ry)
        c.drawOval(rect, p)
        p.maskFilter = null
    }

    // =================================================================== crates (grid blocks)

    /** Glass candy crate filled with 9 marbles. variant: 0 normal, 1 mystery, 2 grey(paint), 3 rainbow */
    fun crate(c: Canvas, x: Float, y: Float, s: Float, colorId: Int, variant: Int, skin: Int) {
        val base = when (variant) { 1 -> 0xFF7E86A6.toInt(); 2 -> 0xFF9DA2B6.toInt(); 3 -> 0xFFA06BFF.toInt(); else -> Painter.colorOf(colorId) }
        val pad = s * 0.055f
        val lidH = s * 0.15f
        val rad = s * 0.17f
        val body = RectF(x + pad, y + pad + lidH * 0.45f, x + s - pad, y + s - pad)
        // shadow
        shadow(c, x + s / 2, body.bottom - s * 0.02f, s * 0.42f, s * 0.07f, 120)
        reset()
        // back wall
        p.alpha = 255; p.shader = LinearGradient(0f, body.top, 0f, body.bottom, Col.lighten(base, 0.2f), Col.darken(base, 0.4f), Shader.TileMode.CLAMP)
        c.drawRoundRect(body, rad, rad, p)
        p.alpha = 255; p.shader = RadialGradient(body.centerX(), body.centerY(), s * 0.5f, intArrayOf(Col.alpha(Col.lighten(base, 0.55f), 190), Col.alpha(base, 0)), null, Shader.TileMode.CLAMP)
        c.drawRoundRect(body, rad, rad, p)
        p.shader = null
        // contents
        val inner = RectF(body); inner.inset(s * 0.06f, s * 0.05f)
        if (variant == 1) {
            // frosted: no contents visible, just blurry blobs
            p.color = 0x55FFFFFF
            val rnd = Random(9)
            repeat(6) { c.drawCircle(inner.left + rnd.nextFloat() * inner.width(), inner.top + rnd.nextFloat() * inner.height(), s * 0.09f, p) }
        } else {
            val mr = s * 0.118f
            val rnd = Random(colorId * 7 + variant * 3 + 1)
            for (iy in 0 until 3) for (ix in 0 until 3) {
                val mx = inner.left + inner.width() * (0.2f + 0.3f * ix) + (rnd.nextFloat() - 0.5f) * s * 0.025f
                val my = inner.top + inner.height() * (0.22f + 0.29f * iy) + (rnd.nextFloat() - 0.5f) * s * 0.02f
                val id = if (variant == 3) intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 0)[iy * 3 + ix] else colorId
                val over = if (variant == 2) 0xFFA7ABBE.toInt() else null
                marbleBody(c, mx, my, mr, id, skin, over)
                marbleGloss(c, mx, my, mr, id, skin, over)
            }
        }
        // front glass
        reset()
        p.alpha = 255; p.shader = LinearGradient(body.left, body.top, body.right, body.bottom,
            intArrayOf(Col.alpha(Color.WHITE, if (variant == 1) 175 else 70), Col.alpha(Color.WHITE, if (variant == 1) 140 else 12), Col.alpha(base, if (variant == 1) 120 else 55)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        c.drawRoundRect(body, rad, rad, p)
        p.shader = null
        c.save()
        path.reset(); path.addRoundRect(body, rad, rad, Path.Direction.CW); c.clipPath(path)
        path2.reset()
        path2.moveTo(body.left + body.width() * 0.08f, body.bottom); path2.lineTo(body.left + body.width() * 0.3f, body.bottom)
        path2.lineTo(body.left + body.width() * 0.75f, body.top); path2.lineTo(body.left + body.width() * 0.53f, body.top); path2.close()
        p.color = 0x2EFFFFFF; c.drawPath(path2, p)
        path2.reset()
        path2.moveTo(body.left + body.width() * 0.36f, body.bottom); path2.lineTo(body.left + body.width() * 0.42f, body.bottom)
        path2.lineTo(body.left + body.width() * 0.87f, body.top); path2.lineTo(body.left + body.width() * 0.81f, body.top); path2.close()
        p.color = 0x24FFFFFF; c.drawPath(path2, p)
        c.restore()
        // coloured frame
        p.style = Paint.Style.STROKE
        p.strokeWidth = s * 0.075f
        p.alpha = 255; p.shader = LinearGradient(0f, body.top, 0f, body.bottom, intArrayOf(Col.lighten(base, 0.45f), base, Col.darken(base, 0.45f)), null, Shader.TileMode.CLAMP)
        val fr = RectF(body); fr.inset(p.strokeWidth / 2, p.strokeWidth / 2)
        c.drawRoundRect(fr, rad * 0.85f, rad * 0.85f, p)
        p.shader = null
        p.strokeWidth = s * 0.018f; p.color = 0x99FFFFFF.toInt()
        fr.inset(s * 0.045f, s * 0.045f); c.drawRoundRect(fr, rad * 0.6f, rad * 0.6f, p)
        p.strokeWidth = s * 0.02f; p.color = Col.alpha(Col.darken(base, 0.6f), 200)
        c.drawRoundRect(body, rad, rad, p)
        p.style = Paint.Style.FILL
        // chrome lid
        val lid = RectF(x + s * 0.17f, y + pad, x + s - s * 0.17f, y + pad + lidH)
        p.alpha = 255; p.shader = LinearGradient(0f, lid.top, 0f, lid.bottom, CHROME, CHROME_POS, Shader.TileMode.CLAMP)
        c.drawRoundRect(lid, lidH * 0.4f, lidH * 0.4f, p)
        p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.016f; p.color = 0xFF4E5568.toInt()
        c.drawRoundRect(lid, lidH * 0.4f, lidH * 0.4f, p); p.style = Paint.Style.FILL
        // lid bolts + colour LED
        p.color = 0xFF5E667A.toInt()
        c.drawCircle(lid.left + lidH * 0.45f, lid.centerY(), lidH * 0.14f, p)
        c.drawCircle(lid.right - lidH * 0.45f, lid.centerY(), lidH * 0.14f, p)
        val led = when (variant) { 1 -> 0xFFE6E9F2.toInt(); 2 -> 0xFFB6BACB.toInt(); else -> Col.lighten(base, 0.15f) }
        p.color = Col.alpha(led, 110); p.maskFilter = BlurMaskFilter(lidH * 0.3f, BlurMaskFilter.Blur.NORMAL)
        c.drawCircle(lid.centerX(), lid.centerY(), lidH * 0.42f, p); p.maskFilter = null
        p.alpha = 255; if (variant == 3) p.shader = SweepGradient(lid.centerX(), lid.centerY(), Col.rainbow, null) else p.color = led
        c.drawCircle(lid.centerX(), lid.centerY(), lidH * 0.3f, p); p.shader = null
        p.color = 0xCCFFFFFF.toInt(); c.drawCircle(lid.centerX() - lidH * 0.1f, lid.centerY() - lidH * 0.1f, lidH * 0.09f, p)
        if (variant == 1) Painter.text(c, "?", body.centerX(), body.centerY() + s * 0.02f, s * 0.5f, Color.WHITE, 0xFF3E4560.toInt(), s * 0.08f)
    }

    /**
     * Board block — a candy-glass jelly cube. [open] blocks (they have a way out) are raised, glossy
     * and show 9 marbles through a glass window; blocked ones sit lower with the marbles behind a
     * frosted lid. variant: 0 normal, 1 mystery, 2 grey (paint), 3 rainbow.
     */
    fun jelly(c: Canvas, x: Float, y: Float, s: Float, colorId: Int, variant: Int, open: Boolean, skin: Int) {
        val base = when (variant) { 1 -> 0xFF8A5CFF.toInt(); 2 -> 0xFFA9ADC2.toInt(); 3 -> 0xFFB98CFF.toInt(); else -> Painter.colorOf(colorId) }
        val white = colorId == 8 && variant == 0
        val pad = s * 0.05f
        val rad = s * 0.25f
        val depth = if (open) s * 0.1f else s * 0.08f
        val outline = if (white) 0xFF8C93AE.toInt() else Col.darken(base, 0.55f)
        // contact shadow
        shadow(c, x + s / 2, y + s - pad - depth * 0.15f, s * 0.43f, s * 0.075f, if (open) 120 else 100)
        reset()
        // depth side
        rect.set(x + pad, y + pad + depth, x + s - pad, y + s - pad)
        p.color = Col.darken(base, if (white) 0.22f else 0.3f)
        c.drawRoundRect(rect, rad, rad, p)
        p.color = Col.alpha(Color.WHITE, if (open) 46 else 30)
        rect.set(x + pad + rad * 0.5f, y + s - pad - depth * 0.55f, x + s - pad - rad * 0.5f, y + s - pad - depth * 0.35f)
        c.drawRoundRect(rect, depth * 0.1f, depth * 0.1f, p)
        // face
        val face = RectF(x + pad, y + pad, x + s - pad, y + s - pad - depth)
        p.alpha = 255
        p.shader = when {
            variant == 3 -> LinearGradient(face.left, face.top, face.right, face.bottom,
                Col.rainbow, null, Shader.TileMode.CLAMP)
            open -> LinearGradient(0f, face.top, 0f, face.bottom, intArrayOf(Col.lighten(base, 0.24f), base, Col.darken(base, 0.04f)), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            else -> LinearGradient(0f, face.top, 0f, face.bottom, intArrayOf(Col.lighten(base, 0.16f), base, Col.darken(base, 0.03f)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(face, rad, rad, p); p.shader = null
        c.save(); path.reset(); path.addRoundRect(face, rad, rad, Path.Direction.CW); c.clipPath(path)
        if (variant == 1) {
            // candy stripes
            p.color = Col.alpha(Color.WHITE, if (open) 34 else 40)
            val step = s * 0.2f
            var k = -s
            while (k < s * 1.2f) {
                path2.reset()
                path2.moveTo(face.left + k, face.bottom); path2.lineTo(face.left + k + step * 0.5f, face.bottom)
                path2.lineTo(face.left + k + step * 0.5f + face.height(), face.top); path2.lineTo(face.left + k + face.height(), face.top); path2.close()
                c.drawPath(path2, p)
                k += step
            }
        }
        c.restore()
        val win = RectF(face); win.inset(s * 0.125f, s * 0.115f)
        val wr = rad * 0.62f
        if (variant == 1) {
            Painter.text(c, "?", face.centerX(), face.centerY() + s * 0.01f, s * 0.56f, 0xFFFFD54A.toInt(), 0xFF3B1F7A.toInt(), s * 0.075f)
            Painter.star4(c, face.left + face.width() * 0.2f, face.top + face.height() * 0.28f, s * 0.05f, 0xDDFFFFFF.toInt())
            Painter.star4(c, face.right - face.width() * 0.18f, face.bottom - face.height() * 0.26f, s * 0.035f, 0xBBFFFFFF.toInt())
        } else if (!open) {
            // closed: a solid candy lid with a soft embossed panel (marbles hidden, like a sealed box)
            p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.022f
            p.color = Col.alpha(Col.darken(base, 0.35f), if (white) 50 else 70)
            rect.set(win); rect.offset(0f, s * 0.008f); c.drawRoundRect(rect, wr, wr, p)
            p.color = Col.alpha(Color.WHITE, 90); c.drawRoundRect(win, wr, wr, p)
            p.style = Paint.Style.FILL
        } else {
            // recessed window
            p.color = Col.alpha(Col.darken(base, if (white) 0.2f else 0.3f), 120)
            c.drawRoundRect(win, wr, wr, p)
            c.save(); path.reset(); path.addRoundRect(win, wr, wr, Path.Direction.CW); c.clipPath(path)
            p.alpha = 255; p.shader = LinearGradient(0f, win.top, 0f, win.top + win.height() * 0.35f, Col.alpha(Col.darken(base, 0.55f), 80), 0, Shader.TileMode.CLAMP)
            c.drawRect(win, p); p.shader = null
            c.restore()
            val mr = s * 0.112f
            val stepX = win.width() / 3f; val stepY = win.height() / 3f
            for (iy in 0 until 3) for (ix in 0 until 3) {
                val mx = win.left + stepX * (ix + 0.5f)
                val my = win.top + stepY * (iy + 0.5f) - s * 0.008f
                val id = if (variant == 3) intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 0)[iy * 3 + ix] else colorId
                reset(); p.color = Col.alpha(Col.darken(base, 0.7f), 70)
                rect.set(mx - mr * 0.8f, my + mr * 0.55f, mx + mr * 0.8f, my + mr * 1.12f); c.drawOval(rect, p)
                if (variant == 2) { marbleBody(c, mx, my, mr, id, skin, 0xFFB4B8CA.toInt()); marbleGloss(c, mx, my, mr, id, skin, 0xFFB4B8CA.toInt()) }
                else marble(c, mx, my, mr, id, skin)
            }
            reset()
            p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.02f
            p.color = 0x66FFFFFF
            rect.set(win); rect.offset(0f, s * 0.006f); c.drawRoundRect(rect, wr, wr, p)
            p.style = Paint.Style.FILL
        }
        // glass gloss over the whole face
        c.save(); path.reset(); path.addRoundRect(face, rad, rad, Path.Direction.CW); c.clipPath(path)
        path2.reset()
        path2.moveTo(face.left, face.top + face.height() * 0.5f)
        path2.cubicTo(face.left + face.width() * 0.25f, face.top + face.height() * 0.3f, face.left + face.width() * 0.6f, face.top + face.height() * 0.22f, face.right, face.top + face.height() * 0.24f)
        path2.lineTo(face.right, face.top); path2.lineTo(face.left, face.top); path2.close()
        p.alpha = 255; p.shader = LinearGradient(0f, face.top, 0f, face.top + face.height() * 0.5f, Col.alpha(Color.WHITE, if (open) 110 else 95), 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawPath(path2, p); p.shader = null
        c.restore()
        // bevel + outline
        p.style = Paint.Style.STROKE; p.strokeWidth = s * 0.03f
        p.alpha = 255; p.shader = LinearGradient(0f, face.top, 0f, face.bottom, intArrayOf(Col.alpha(Color.WHITE, if (open) 230 else 210), Col.alpha(Color.WHITE, 40), 0x00FFFFFF), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        val rim = RectF(face); rim.inset(s * 0.032f, s * 0.032f)
        c.drawRoundRect(rim, rad * 0.82f, rad * 0.82f, p); p.shader = null
        p.strokeWidth = s * 0.024f; p.color = Col.alpha(outline, if (open) 220 else 190)
        rect.set(x + pad, y + pad, x + s - pad, y + s - pad); c.drawRoundRect(rect, rad, rad, p)
        p.style = Paint.Style.FILL
        // glint
        p.color = Col.alpha(Color.WHITE, if (open) 235 else 215)
        rect.set(face.left + s * 0.1f, face.top + s * 0.07f, face.left + s * 0.27f, face.top + s * 0.12f)
        c.save(); c.rotate(-14f, rect.centerX(), rect.centerY()); c.drawOval(rect, p); c.restore()
        c.drawCircle(face.left + s * 0.31f, face.top + s * 0.085f, s * 0.018f, p)
    }

    // =================================================================== cups (cylindrical pucks)

    /**
     * Tray cup — a "lid box": a solid jelly body whose front face shows below a pale, glossy lid,
     * with 3 round holes in the lid that the marbles drop into.
     * Hole centres: x ± w * [HOLE_SPREAD], y + h * 0.42 (GameLayout.dimpleDx / dimpleDy use the same).
     * [holeR] sizes the holes to the marbles that drop in (GameLayout.cupHoleR); menus leave it null.
     */
    fun puck(c: Canvas, x: Float, y: Float, w: Float, h: Float, colorId: Int, hidden: Boolean, holeR: Float? = null) {
        val base = if (hidden) 0xFF8A5CFF.toInt() else Painter.colorOf(colorId)
        val white = colorId == 8 && !hidden
        val padX = w * 0.03f
        val top = y + h * 0.02f
        val bottom = y + h * 0.95f
        val front = h * 0.2f                                   // visible front face of the body below the lid
        val rad = minOf(w * 0.12f, h * 0.32f)
        val box = RectF(x + padX, top, x + w - padX, bottom)
        val bodyTop = if (white) 0xFFDCE0EC.toInt() else base
        val bodyBottom = if (white) 0xFFB4BACD.toInt() else Col.darken(base, 0.28f)
        val lidTop = if (white) Color.WHITE else Col.lighten(base, 0.66f)
        val lidBottom = if (white) 0xFFEEF0F7.toInt() else Col.lighten(base, 0.42f)
        val edge = if (white) 0xFF8C93AE.toInt() else Col.darken(base, 0.45f)
        reset()
        // soft contact shadow
        p.color = 0x40281468
        rect.set(box.left + w * 0.03f, box.top + front, box.right - w * 0.03f, bottom + h * 0.05f)
        c.drawRoundRect(rect, rad, rad, p)
        // body: the solid jelly, its front face shows under the lid
        p.alpha = 255; p.shader = LinearGradient(0f, box.top, 0f, box.bottom, bodyTop, bodyBottom, Shader.TileMode.CLAMP)
        c.drawRoundRect(box, rad, rad, p); p.shader = null
        // a soft shine across the front face
        p.color = Col.alpha(Color.WHITE, if (white) 70 else 55)
        rect.set(box.left + w * 0.06f, box.bottom - front * 0.62f, box.right - w * 0.06f, box.bottom - front * 0.4f)
        c.drawRoundRect(rect, h * 0.05f, h * 0.05f, p)
        // lid
        val lid = RectF(box.left, box.top, box.right, box.bottom - front)
        p.color = Col.alpha(edge, 110)                          // lip shadow where the lid sits on the body
        rect.set(lid); rect.offset(0f, h * 0.035f); c.drawRoundRect(rect, rad, rad, p)
        p.alpha = 255; p.shader = LinearGradient(0f, lid.top, 0f, lid.bottom, lidTop, lidBottom, Shader.TileMode.CLAMP)
        c.drawRoundRect(lid, rad, rad, p); p.shader = null
        // lid bevel: bright top edge
        p.style = Paint.Style.STROKE
        p.strokeWidth = h * 0.03f; p.color = 0xB4FFFFFF.toInt()
        rect.set(lid); rect.inset(w * 0.025f, h * 0.045f)
        c.drawRoundRect(rect, rad * 0.8f, rad * 0.8f, p)
        // outline
        p.strokeWidth = h * 0.028f; p.color = Col.alpha(edge, 210)
        c.drawRoundRect(box, rad, rad, p)
        p.strokeWidth = h * 0.02f; p.color = Col.alpha(edge, 120)
        c.drawRoundRect(lid, rad, rad, p)
        p.style = Paint.Style.FILL
        // glint
        p.color = 0xE6FFFFFF.toInt()
        rect.set(lid.left + w * 0.06f, lid.top + h * 0.08f, lid.left + w * 0.17f, lid.top + h * 0.15f)
        c.drawOval(rect, p)
        if (hidden) {
            Painter.text(c, "?", x + w / 2, lid.centerY() + h * 0.02f, h * 0.56f, 0xFFFFD54A.toInt(), 0xFF3B1F7A.toInt(), h * 0.1f)
            return
        }
        // holes: a dark well with a soft rim shadow above and a light lip below
        val hy = y + h * 0.42f
        val rx = holeR ?: minOf(w * 0.105f, h * 0.25f)
        val ry = if (holeR != null) holeR * 0.94f else minOf(h * 0.21f, rx * 0.86f)
        val wellTop = if (white) 0xFF6E7590.toInt() else Col.darken(base, 0.7f)
        val wellBottom = if (white) 0xFFA4AAC0.toInt() else Col.darken(base, 0.38f)
        for (k in -1..1) {
            val hx = x + w / 2 + k * w * HOLE_SPREAD
            rect.set(hx - rx, hy - ry, hx + rx, hy + ry)
            p.color = 0x8CFFFFFF.toInt(); rect.offset(0f, h * 0.025f); c.drawOval(rect, p); rect.offset(0f, -h * 0.025f)
            p.color = Col.alpha(edge, 90); rect.offset(0f, -h * 0.012f); c.drawOval(rect, p); rect.offset(0f, h * 0.012f)
            p.alpha = 255; p.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, wellTop, wellBottom, Shader.TileMode.CLAMP)
            c.drawOval(rect, p); p.shader = null
        }
    }

    // =================================================================== belt parts

    /** Recessed belt socket, drawn centred in a box of side [d]. */
    fun socket(c: Canvas, cx: Float, cy: Float, r: Float) {
        reset()
        p.color = 0x40FFFFFF; c.drawCircle(cx, cy + r * 0.1f, r, p)
        p.alpha = 255; p.shader = RadialGradient(cx, cy - r * 0.25f, r * 1.05f, intArrayOf(0xFF07051A.toInt(), 0xFF141038.toInt(), 0xFF3A2F86.toInt()), floatArrayOf(0f, 0.68f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 0.93f, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = r * 0.12f; p.strokeCap = Paint.Cap.ROUND
        p.color = 0x5591A0FF; rect.set(cx - r * 0.72f, cy - r * 0.72f, cx + r * 0.72f, cy + r * 0.72f)
        c.drawArc(rect, 30f, 120f, false, p)
        p.style = Paint.Style.FILL
    }

    /** Candy roller hub at the belt ends; rotated at draw time. */
    fun hub(c: Canvas, cx: Float, cy: Float, r: Float, tint: Int) {
        reset()
        p.color = 0x55000000; c.drawCircle(cx, cy + r * 0.08f, r, p)
        p.alpha = 255; p.shader = LinearGradient(0f, cy - r, 0f, cy + r, CHROME, CHROME_POS, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 0.97f, p); p.shader = null
        p.alpha = 255; p.shader = RadialGradient(cx - r * 0.2f, cy - r * 0.25f, r, intArrayOf(Col.lighten(tint, 0.45f), tint, Col.darken(tint, 0.3f)), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 0.78f, p); p.shader = null
        p.color = Col.lighten(tint, 0.75f)
        for (k in 0 until 5) {
            c.save(); c.rotate(k * 72f, cx, cy)
            rect.set(cx - r * 0.1f, cy - r * 0.7f, cx + r * 0.1f, cy - r * 0.2f)
            c.drawRoundRect(rect, r * 0.1f, r * 0.1f, p)
            c.restore()
        }
        p.alpha = 255; p.shader = RadialGradient(cx - r * 0.06f, cy - r * 0.06f, r * 0.26f, intArrayOf(Color.WHITE, 0xFFA7AFC0.toInt()), null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 0.22f, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = r * 0.06f; p.color = Col.darken(tint, 0.5f)
        c.drawCircle(cx, cy, r * 0.78f, p); p.style = Paint.Style.FILL
    }

    // =================================================================== machine parts

    fun gear(c: Canvas, cx: Float, cy: Float, r: Float, teeth: Int, tint: Int) {
        reset()
        path.reset()
        val inner = r * 0.8f
        for (i in 0 until teeth * 4) {
            val a = i * PI.toFloat() * 2 / (teeth * 4)
            val rr = if ((i / 2) % 2 == 0) r else inner
            val px = cx + cos(a) * rr; val py = cy + sin(a) * rr
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        p.color = 0x66000000
        c.save(); c.translate(0f, r * 0.06f); c.drawPath(path, p); c.restore()
        p.alpha = 255; p.shader = RadialGradient(cx - r * 0.3f, cy - r * 0.3f, r * 1.4f, intArrayOf(Col.lighten(tint, 0.7f), tint, Col.darken(tint, 0.45f)), null, Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = r * 0.05f; p.color = Col.darken(tint, 0.55f); c.drawPath(path, p); p.style = Paint.Style.FILL
        // spoke holes
        p.color = Col.darken(tint, 0.6f)
        for (k in 0 until 5) {
            val a = k * 2 * PI.toFloat() / 5
            c.drawCircle(cx + cos(a) * r * 0.5f, cy + sin(a) * r * 0.5f, r * 0.13f, p)
        }
        p.alpha = 255; p.shader = RadialGradient(cx - r * 0.08f, cy - r * 0.08f, r * 0.3f, intArrayOf(0xFFFFFFFF.toInt(), 0xFF8A93A6.toInt()), null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 0.24f, p); p.shader = null
        p.color = 0xFF3E4456.toInt(); c.drawCircle(cx, cy, r * 0.08f, p)
    }

    /** Chain link / marble cradle, drawn horizontally (direction of travel = +x).
     *  Deep indigo with a dark socket so the bright candy marbles pop on top of it. */
    fun cradle(c: Canvas, cx: Float, cy: Float, len: Float, wid: Float) {
        reset()
        rect.set(cx - len / 2, cy - wid / 2, cx + len / 2, cy + wid / 2)
        p.alpha = 255; p.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, intArrayOf(0xFF7566D6.toInt(), 0xFF4B3DAA.toInt(), 0xFF2C2270.toInt()), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, wid * 0.32f, wid * 0.32f, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = wid * 0.07f; p.color = 0xFF17113D.toInt()
        c.drawRoundRect(rect, wid * 0.32f, wid * 0.32f, p); p.style = Paint.Style.FILL
        p.color = 0x55FFFFFF; c.drawRoundRect(rect.left + wid * 0.2f, rect.top + wid * 0.07f, rect.right - wid * 0.2f, rect.top + wid * 0.15f, wid * 0.04f, wid * 0.04f, p)
        // gold pins
        p.color = 0xFFFFC93C.toInt()
        c.drawCircle(rect.left + wid * 0.17f, cy, wid * 0.085f, p); c.drawCircle(rect.right - wid * 0.17f, cy, wid * 0.085f, p)
        // deep socket
        p.alpha = 255; p.shader = RadialGradient(cx, cy - wid * 0.06f, wid * 0.36f, intArrayOf(0xFF0B0820.toInt(), 0xFF1A1442.toInt(), 0xFF6A5BD0.toInt()), floatArrayOf(0f, 0.74f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, wid * 0.34f, p); p.shader = null
    }

    fun rivet(c: Canvas, cx: Float, cy: Float, r: Float) {
        reset()
        p.color = 0x55000000; c.drawCircle(cx + r * 0.2f, cy + r * 0.3f, r, p)
        p.alpha = 255; p.shader = RadialGradient(cx - r * 0.35f, cy - r * 0.35f, r * 1.4f, intArrayOf(0xFFFFFFFF.toInt(), 0xFFB7BFCD.toInt(), 0xFF6B7387.toInt()), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p); p.shader = null
    }

    fun chromeRect(c: Canvas, r: RectF, radius: Float, vertical: Boolean = true) {
        reset()
        p.alpha = 255; p.shader = if (vertical) LinearGradient(0f, r.top, 0f, r.bottom, CHROME, CHROME_POS, Shader.TileMode.CLAMP)
        else LinearGradient(r.left, 0f, r.right, 0f, CHROME, CHROME_POS, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, radius, radius, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f; p.color = 0xFF4E5568.toInt()
        c.drawRoundRect(r, radius, radius, p); p.style = Paint.Style.FILL
    }
}
