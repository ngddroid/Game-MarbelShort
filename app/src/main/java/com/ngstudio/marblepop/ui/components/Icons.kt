package com.ngstudio.marblepop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.ngstudio.marblepop.render.Painter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class Ico {
    COIN, STAR, STAR_EMPTY, GEAR, SHOP, GIFT, PLAY, MARBLES, TROPHY, HAMMER, MAGNET, SLOTS, SHUFFLE, LOCK,
    MUSIC, SOUND, VIBRATE, BACK, CLOSE, DOC, SHIELD, CHEST, CALENDAR, WHEEL, PAUSE, RETRY, HOME, CHECK, PALETTE, USER, PLUS,
    CROWN, CHEVRON, FLAME, CUP, FLAG, SPARK
}

private val Outline = Color(0xFF2A1463)
private val GlyphShadow = Color(0x592A1463)

@Composable
fun GameIcon(icon: Ico, modifier: Modifier, tint: Color? = null) {
    Canvas(modifier) { drawGameIcon(icon, tint) }
}

fun DrawScope.drawGameIcon(icon: Ico, tint: Color? = null) {
    val s = min(size.width, size.height)
    val c = Offset(size.width / 2, size.height / 2)
    val ow = s * 0.07f
    when (icon) {
        Ico.COIN -> {
            drawCircle(Color(0xFFB86E00), s * 0.46f, c + Offset(0f, s * 0.04f))
            drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFE680), Color(0xFFFFB800)), c.y - s * 0.45f, c.y + s * 0.45f), s * 0.44f, c)
            drawCircle(Color(0xFFE09000), s * 0.31f, c, style = Stroke(s * 0.05f))
            star(this, c, s * 0.2f, Color(0xFFFFF4C2))
            drawCircle(Color(0xFF9A5600), s * 0.445f, c + Offset(0f, s * 0.01f), style = Stroke(s * 0.035f))
            drawArc(Color.White.copy(alpha = 0.55f), 200f, 70f, false, c - Offset(s * 0.36f, s * 0.36f), Size(s * 0.72f, s * 0.72f), style = Stroke(s * 0.05f, cap = StrokeCap.Round))
            drawCircle(Color.White.copy(alpha = 0.75f), s * 0.05f, c + Offset(-s * 0.2f, -s * 0.24f))
        }
        Ico.STAR, Ico.STAR_EMPTY -> {
            val filled = icon == Ico.STAR
            val p = starPath(c + Offset(0f, s * 0.04f), s * 0.48f, s * 0.22f)
            drawPath(p, if (filled) Color(0xFFC77700) else Color(0xFF3A2C78))
            val p2 = starPath(c, s * 0.48f, s * 0.22f)
            if (filled) drawPath(p2, Brush.verticalGradient(listOf(Color(0xFFFFEE88), Color(0xFFFFB000)), 0f, size.height))
            else drawPath(p2, Color(0xFF6A58B8))
            drawPath(p2, if (filled) Color(0xFFB06000) else Color(0xFF2E2266), style = Stroke(ow * 0.7f, join = StrokeJoin.Round))
            if (filled) drawCircle(Color.White.copy(alpha = 0.8f), s * 0.05f, c + Offset(-s * 0.1f, -s * 0.14f))
        }
        Ico.GEAR -> {
            val col = tint ?: Color.White
            val p = Path()
            val teeth = 8
            for (i in 0 until teeth * 2) {
                val a = i * PI / teeth
                val r = if (i % 2 == 0) s * 0.46f else s * 0.36f
                val a1 = a - PI / teeth * 0.45; val a2 = a + PI / teeth * 0.45
                if (i == 0) p.moveTo(c.x + (cos(a1) * r).toFloat(), c.y + (sin(a1) * r).toFloat()) else p.lineTo(c.x + (cos(a1) * r).toFloat(), c.y + (sin(a1) * r).toFloat())
                p.lineTo(c.x + (cos(a2) * r).toFloat(), c.y + (sin(a2) * r).toFloat())
            }
            p.close()
            translate(0f, s * 0.04f) { drawPath(p, Outline.copy(alpha = 0.5f)) }
            drawPath(p, col)
            drawPath(p, Outline, style = Stroke(ow * 0.6f, join = StrokeJoin.Round))
            drawCircle(Color(0xFF7B4DFF), s * 0.15f, c)
            drawCircle(Outline, s * 0.15f, c, style = Stroke(ow * 0.6f))
        }
        Ico.SHOP -> {
            // stall with striped awning
            drawRoundRect(Color(0xFFE59A00), Offset(s * 0.16f, s * 0.48f), Size(s * 0.68f, s * 0.42f), CornerRadius(s * 0.06f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFD35A), Color(0xFFFFB300))), Offset(s * 0.16f, s * 0.46f), Size(s * 0.68f, s * 0.4f), CornerRadius(s * 0.06f))
            drawRoundRect(Color(0xFF8A5200), Offset(s * 0.38f, s * 0.6f), Size(s * 0.24f, s * 0.26f), CornerRadius(s * 0.04f))
            val stripes = 5
            val w = s * 0.84f / stripes
            for (i in 0 until stripes) {
                val col = if (i % 2 == 0) Color(0xFFFF5FA2) else Color.White
                drawRoundRect(col, Offset(s * 0.08f + i * w, s * 0.14f), Size(w, s * 0.3f), CornerRadius(s * 0.06f))
                drawCircle(col, w / 2, Offset(s * 0.08f + i * w + w / 2, s * 0.44f))
            }
            drawRoundRect(Outline, Offset(s * 0.08f, s * 0.14f), Size(s * 0.84f, s * 0.3f), CornerRadius(s * 0.06f), style = Stroke(ow * 0.6f))
        }
        Ico.GIFT -> {
            drawRoundRect(Color(0xFFC0306A), Offset(s * 0.14f, s * 0.42f), Size(s * 0.72f, s * 0.48f), CornerRadius(s * 0.08f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFF8AC0), Color(0xFFFF4D8D))), Offset(s * 0.14f, s * 0.4f), Size(s * 0.72f, s * 0.46f), CornerRadius(s * 0.08f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFA6D0), Color(0xFFFF5FA2))), Offset(s * 0.08f, s * 0.28f), Size(s * 0.84f, s * 0.18f), CornerRadius(s * 0.06f))
            drawRect(Color(0xFFFFD23F), Offset(s * 0.43f, s * 0.28f), Size(s * 0.14f, s * 0.58f))
            drawOval(Color(0xFFFFD23F), Offset(s * 0.22f, s * 0.1f), Size(s * 0.26f, s * 0.2f), style = Stroke(s * 0.07f))
            drawOval(Color(0xFFFFD23F), Offset(s * 0.52f, s * 0.1f), Size(s * 0.26f, s * 0.2f), style = Stroke(s * 0.07f))
        }
        Ico.PLAY -> {
            val p = Path().apply { moveTo(s * 0.3f, s * 0.18f); lineTo(s * 0.82f, s * 0.5f); lineTo(s * 0.3f, s * 0.82f); close() }
            translate(0f, s * 0.04f) { drawPath(p, Outline.copy(alpha = 0.35f), style = Stroke(s * 0.12f, join = StrokeJoin.Round)) }
            drawPath(p, tint ?: Color.White, style = Stroke(s * 0.12f, join = StrokeJoin.Round))
            drawPath(p, tint ?: Color.White)
        }
        Ico.MARBLES -> {
            ball(this, Offset(s * 0.34f, s * 0.62f), s * 0.22f, 2)
            ball(this, Offset(s * 0.68f, s * 0.64f), s * 0.2f, 6)
            ball(this, Offset(s * 0.52f, s * 0.32f), s * 0.21f, 1)
        }
        Ico.TROPHY -> {
            val gold = Brush.verticalGradient(listOf(Color(0xFFFFE680), Color(0xFFFFB000)))
            drawArc(Color(0xFFE59A00), 90f, 180f, false, Offset(s * 0.08f, s * 0.2f), Size(s * 0.3f, s * 0.3f), style = Stroke(s * 0.07f))
            drawArc(Color(0xFFE59A00), -90f, 180f, false, Offset(s * 0.62f, s * 0.2f), Size(s * 0.3f, s * 0.3f), style = Stroke(s * 0.07f))
            val cup = Path().apply {
                moveTo(s * 0.22f, s * 0.14f); lineTo(s * 0.78f, s * 0.14f)
                cubicTo(s * 0.78f, s * 0.5f, s * 0.62f, s * 0.6f, s * 0.5f, s * 0.6f)
                cubicTo(s * 0.38f, s * 0.6f, s * 0.22f, s * 0.5f, s * 0.22f, s * 0.14f); close()
            }
            drawPath(cup, gold)
            drawPath(cup, Color(0xFFB06000), style = Stroke(ow * 0.6f))
            drawRect(Color(0xFFE59A00), Offset(s * 0.44f, s * 0.58f), Size(s * 0.12f, s * 0.16f))
            drawRoundRect(Color(0xFF8E5BFF), Offset(s * 0.26f, s * 0.72f), Size(s * 0.48f, s * 0.16f), CornerRadius(s * 0.04f))
            star(this, Offset(s * 0.5f, s * 0.32f), s * 0.11f, Color.White)
        }
        Ico.HAMMER -> {
            rotate(-35f, c) {
                drawRoundRect(Color(0xFF8A5200), Offset(s * 0.45f, s * 0.36f), Size(s * 0.11f, s * 0.56f), CornerRadius(s * 0.05f))
                drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFD5A060), Color(0xFFA86A2A))), Offset(s * 0.46f, s * 0.36f), Size(s * 0.09f, s * 0.54f), CornerRadius(s * 0.04f))
                drawRoundRect(Color(0xFF3B3F52), Offset(s * 0.18f, s * 0.12f), Size(s * 0.64f, s * 0.28f), CornerRadius(s * 0.07f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE6EAF2), Color(0xFF8C93A8))), Offset(s * 0.2f, s * 0.13f), Size(s * 0.6f, s * 0.24f), CornerRadius(s * 0.06f))
            }
        }
        Ico.MAGNET -> {
            drawArc(Color(0xFFB31E35), 0f, 180f, false, Offset(s * 0.16f, s * 0.12f), Size(s * 0.68f, s * 0.68f), style = Stroke(s * 0.22f))
            drawArc(Brush.horizontalGradient(listOf(Color(0xFFFF7A86), Color(0xFFFF2E4A))), 0f, 180f, false, Offset(s * 0.16f, s * 0.1f), Size(s * 0.68f, s * 0.68f), style = Stroke(s * 0.18f))
            drawRect(Color(0xFFFF2E4A), Offset(s * 0.07f, s * 0.2f), Size(s * 0.18f, s * 0.24f))
            drawRect(Color(0xFFFF2E4A), Offset(s * 0.75f, s * 0.2f), Size(s * 0.18f, s * 0.24f))
            drawRect(Color(0xFFE6EAF2), Offset(s * 0.07f, s * 0.08f), Size(s * 0.18f, s * 0.14f))
            drawRect(Color(0xFFE6EAF2), Offset(s * 0.75f, s * 0.08f), Size(s * 0.18f, s * 0.14f))
        }
        Ico.SLOTS -> {
            drawRoundRect(Color(0xFF3B3557), Offset(s * 0.06f, s * 0.32f), Size(s * 0.88f, s * 0.38f), CornerRadius(s * 0.19f))
            for (i in 0 until 4) drawCircle(if (i < 2) Color(0xFF33C8F5) else Color(0xFF6E6A86), s * 0.07f, Offset(s * (0.22f + i * 0.19f), s * 0.51f))
            drawCircle(Color(0xFF3CCB5A), s * 0.2f, Offset(s * 0.74f, s * 0.26f))
            drawLine(Color.White, Offset(s * 0.74f, s * 0.15f), Offset(s * 0.74f, s * 0.37f), s * 0.07f, StrokeCap.Round)
            drawLine(Color.White, Offset(s * 0.63f, s * 0.26f), Offset(s * 0.85f, s * 0.26f), s * 0.07f, StrokeCap.Round)
        }
        Ico.SHUFFLE -> {
            val col = tint ?: Color(0xFF7B4DFF)
            val p1 = Path().apply { moveTo(s * 0.1f, s * 0.3f); cubicTo(s * 0.45f, s * 0.3f, s * 0.45f, s * 0.7f, s * 0.82f, s * 0.7f) }
            val p2 = Path().apply { moveTo(s * 0.1f, s * 0.7f); cubicTo(s * 0.45f, s * 0.7f, s * 0.45f, s * 0.3f, s * 0.82f, s * 0.3f) }
            drawPath(p1, col, style = Stroke(s * 0.11f, cap = StrokeCap.Round))
            drawPath(p2, col, style = Stroke(s * 0.11f, cap = StrokeCap.Round))
            for (y in listOf(0.3f, 0.7f)) {
                val h = Path().apply { moveTo(s * 0.92f, s * y); lineTo(s * 0.74f, s * (y - 0.13f)); lineTo(s * 0.74f, s * (y + 0.13f)); close() }
                drawPath(h, col)
            }
        }
        Ico.LOCK -> {
            drawArc(Color(0xFF8A8FA8), 180f, 180f, false, Offset(s * 0.28f, s * 0.12f), Size(s * 0.44f, s * 0.5f), style = Stroke(s * 0.1f))
            drawRoundRect(Color(0xFFB07000), Offset(s * 0.18f, s * 0.4f), Size(s * 0.64f, s * 0.5f), CornerRadius(s * 0.1f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFD35A), Color(0xFFFFAA00))), Offset(s * 0.18f, s * 0.38f), Size(s * 0.64f, s * 0.48f), CornerRadius(s * 0.1f))
            drawCircle(Color(0xFF7A4A00), s * 0.07f, Offset(s * 0.5f, s * 0.58f))
        }
        Ico.MUSIC -> {
            val col = tint ?: Color.White
            drawCircle(col, s * 0.13f, Offset(s * 0.3f, s * 0.74f)); drawCircle(col, s * 0.13f, Offset(s * 0.72f, s * 0.64f))
            drawLine(col, Offset(s * 0.42f, s * 0.74f), Offset(s * 0.42f, s * 0.2f), s * 0.08f)
            drawLine(col, Offset(s * 0.84f, s * 0.64f), Offset(s * 0.84f, s * 0.12f), s * 0.08f)
            drawLine(col, Offset(s * 0.42f, s * 0.22f), Offset(s * 0.84f, s * 0.12f), s * 0.12f)
        }
        Ico.SOUND -> {
            val col = tint ?: Color.White
            val p = Path().apply { moveTo(s * 0.12f, s * 0.38f); lineTo(s * 0.3f, s * 0.38f); lineTo(s * 0.52f, s * 0.18f); lineTo(s * 0.52f, s * 0.82f); lineTo(s * 0.3f, s * 0.62f); lineTo(s * 0.12f, s * 0.62f); close() }
            drawPath(p, col)
            drawArc(col, -45f, 90f, false, Offset(s * 0.42f, s * 0.3f), Size(s * 0.32f, s * 0.4f), style = Stroke(s * 0.07f, cap = StrokeCap.Round))
            drawArc(col, -50f, 100f, false, Offset(s * 0.42f, s * 0.16f), Size(s * 0.48f, s * 0.68f), style = Stroke(s * 0.07f, cap = StrokeCap.Round))
        }
        Ico.VIBRATE -> {
            val col = tint ?: Color.White
            drawRoundRect(col, Offset(s * 0.32f, s * 0.12f), Size(s * 0.36f, s * 0.76f), CornerRadius(s * 0.08f), style = Stroke(s * 0.08f))
            drawLine(col, Offset(s * 0.42f, s * 0.78f), Offset(s * 0.58f, s * 0.78f), s * 0.06f, StrokeCap.Round)
            for (side in listOf(-1f, 1f)) {
                val x = s * 0.5f + side * s * 0.32f
                drawLine(col, Offset(x, s * 0.32f), Offset(x + side * s * 0.06f, s * 0.42f), s * 0.06f, StrokeCap.Round)
                drawLine(col, Offset(x + side * s * 0.06f, s * 0.42f), Offset(x, s * 0.52f), s * 0.06f, StrokeCap.Round)
                drawLine(col, Offset(x, s * 0.52f), Offset(x + side * s * 0.06f, s * 0.62f), s * 0.06f, StrokeCap.Round)
            }
        }
        Ico.BACK -> glyph(tint) { col ->
            val p = Path().apply { moveTo(s * 0.62f, s * 0.18f); lineTo(s * 0.3f, s * 0.5f); lineTo(s * 0.62f, s * 0.82f) }
            drawPath(p, col, style = Stroke(s * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Ico.CLOSE -> glyph(tint) { col ->
            drawLine(col, Offset(s * 0.26f, s * 0.26f), Offset(s * 0.74f, s * 0.74f), s * 0.16f, StrokeCap.Round)
            drawLine(col, Offset(s * 0.74f, s * 0.26f), Offset(s * 0.26f, s * 0.74f), s * 0.16f, StrokeCap.Round)
        }
        Ico.DOC -> {
            drawRoundRect(Color(0xFFDCD3FF), Offset(s * 0.2f, s * 0.12f), Size(s * 0.6f, s * 0.78f), CornerRadius(s * 0.08f))
            drawRoundRect(Color.White, Offset(s * 0.2f, s * 0.08f), Size(s * 0.6f, s * 0.76f), CornerRadius(s * 0.08f))
            for (i in 0 until 4) drawLine(Color(0xFF9B87E8), Offset(s * 0.3f, s * (0.24f + i * 0.14f)), Offset(s * (if (i == 3) 0.55f else 0.7f), s * (0.24f + i * 0.14f)), s * 0.06f, StrokeCap.Round)
        }
        Ico.SHIELD -> {
            val p = Path().apply {
                moveTo(s * 0.5f, s * 0.08f); lineTo(s * 0.84f, s * 0.2f)
                cubicTo(s * 0.84f, s * 0.6f, s * 0.66f, s * 0.8f, s * 0.5f, s * 0.92f)
                cubicTo(s * 0.34f, s * 0.8f, s * 0.16f, s * 0.6f, s * 0.16f, s * 0.2f); close()
            }
            drawPath(p, Brush.verticalGradient(listOf(Color(0xFF7BE88E), Color(0xFF1E9440))))
            drawPath(p, Color(0xFF136B2E), style = Stroke(ow * 0.6f))
            val ck = Path().apply { moveTo(s * 0.33f, s * 0.5f); lineTo(s * 0.46f, s * 0.63f); lineTo(s * 0.68f, s * 0.38f) }
            drawPath(ck, Color.White, style = Stroke(s * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Ico.CHEST -> {
            drawRoundRect(Color(0xFF6E3F18), Offset(s * 0.1f, s * 0.36f), Size(s * 0.8f, s * 0.54f), CornerRadius(s * 0.08f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE0A35C), Color(0xFFB0692A))), Offset(s * 0.12f, s * 0.4f), Size(s * 0.76f, s * 0.46f), CornerRadius(s * 0.06f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFF0B86E), Color(0xFFC07A36))), Offset(s * 0.1f, s * 0.14f), Size(s * 0.8f, s * 0.28f), CornerRadius(s * 0.14f))
            drawRect(Color(0xFFFFC93C), Offset(s * 0.1f, s * 0.38f), Size(s * 0.8f, s * 0.07f))
            drawRoundRect(Color(0xFFFFC93C), Offset(s * 0.42f, s * 0.32f), Size(s * 0.16f, s * 0.2f), CornerRadius(s * 0.04f))
            drawCircle(Color(0xFF7A4A00), s * 0.03f, Offset(s * 0.5f, s * 0.42f))
        }
        Ico.CALENDAR -> {
            drawRoundRect(Color(0xFFC92A3C), Offset(s * 0.12f, s * 0.18f), Size(s * 0.76f, s * 0.72f), CornerRadius(s * 0.1f))
            drawRoundRect(Color.White, Offset(s * 0.12f, s * 0.32f), Size(s * 0.76f, s * 0.56f), CornerRadius(s * 0.08f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFF7A86), Color(0xFFFF4D5E))), Offset(s * 0.12f, s * 0.16f), Size(s * 0.76f, s * 0.2f), CornerRadius(s * 0.1f))
            drawLine(Color(0xFF5B4A8E), Offset(s * 0.32f, s * 0.08f), Offset(s * 0.32f, s * 0.24f), s * 0.07f, StrokeCap.Round)
            drawLine(Color(0xFF5B4A8E), Offset(s * 0.68f, s * 0.08f), Offset(s * 0.68f, s * 0.24f), s * 0.07f, StrokeCap.Round)
            star(this, Offset(s * 0.5f, s * 0.62f), s * 0.16f, Color(0xFFFFC93C))
        }
        Ico.WHEEL -> {
            val cols = listOf(Color(0xFFFF4D5E), Color(0xFFFFD23F), Color(0xFF3CCB5A), Color(0xFF33C8F5), Color(0xFF9B5CFF), Color(0xFFFF7AC8))
            for (i in 0 until 6) drawArc(cols[i], i * 60f, 60f, true, Offset(s * 0.1f, s * 0.1f), Size(s * 0.8f, s * 0.8f))
            drawCircle(Color.White, s * 0.4f, c, style = Stroke(s * 0.06f))
            drawCircle(Color.White, s * 0.1f, c)
            val p = Path().apply { moveTo(s * 0.5f, s * 0.2f); lineTo(s * 0.42f, s * 0.02f); lineTo(s * 0.58f, s * 0.02f); close() }
            drawPath(p, Color(0xFF2B1A5E))
        }
        Ico.PAUSE -> glyph(tint) { col ->
            drawRoundRect(col, Offset(s * 0.26f, s * 0.2f), Size(s * 0.16f, s * 0.6f), CornerRadius(s * 0.06f))
            drawRoundRect(col, Offset(s * 0.58f, s * 0.2f), Size(s * 0.16f, s * 0.6f), CornerRadius(s * 0.06f))
        }
        Ico.RETRY -> glyph(tint) { col ->
            drawArc(col, 40f, 290f, false, Offset(s * 0.2f, s * 0.2f), Size(s * 0.6f, s * 0.6f), style = Stroke(s * 0.12f, cap = StrokeCap.Round))
            val p = Path().apply { moveTo(s * 0.86f, s * 0.38f); lineTo(s * 0.62f, s * 0.42f); lineTo(s * 0.8f, s * 0.18f); close() }
            drawPath(p, col)
        }
        Ico.HOME -> {
            glyph(tint) { col ->
                val p = Path().apply { moveTo(s * 0.5f, s * 0.12f); lineTo(s * 0.9f, s * 0.48f); lineTo(s * 0.78f, s * 0.48f); lineTo(s * 0.78f, s * 0.86f); lineTo(s * 0.22f, s * 0.86f); lineTo(s * 0.22f, s * 0.48f); lineTo(s * 0.1f, s * 0.48f); close() }
                drawPath(p, col, style = Stroke(s * 0.04f, join = StrokeJoin.Round))
                drawPath(p, col)
            }
            drawRoundRect(Color(0xFF7B4DFF), Offset(s * 0.42f, s * 0.58f), Size(s * 0.16f, s * 0.28f), CornerRadius(s * 0.04f))
        }
        Ico.CHECK -> glyph(tint) { col ->
            val ck = Path().apply { moveTo(s * 0.2f, s * 0.52f); lineTo(s * 0.42f, s * 0.74f); lineTo(s * 0.8f, s * 0.3f) }
            drawPath(ck, col, style = Stroke(s * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Ico.PALETTE -> {
            drawCircle(Color(0xFFF5E6C8), s * 0.42f, c)
            drawCircle(Color(0xFFB08A50), s * 0.42f, c, style = Stroke(s * 0.05f))
            drawCircle(Color(0xFFFF4D5E), s * 0.08f, Offset(s * 0.32f, s * 0.32f))
            drawCircle(Color(0xFF33C8F5), s * 0.08f, Offset(s * 0.56f, s * 0.24f))
            drawCircle(Color(0xFF3CCB5A), s * 0.08f, Offset(s * 0.74f, s * 0.42f))
            drawCircle(Color(0xFFFFD23F), s * 0.08f, Offset(s * 0.28f, s * 0.58f))
            drawCircle(Color(0xFF9B5CFF), s * 0.1f, Offset(s * 0.6f, s * 0.68f))
        }
        Ico.USER -> {
            drawCircle(tint ?: Color.White, s * 0.2f, Offset(s * 0.5f, s * 0.34f))
            drawArc(tint ?: Color.White, 180f, 180f, true, Offset(s * 0.16f, s * 0.56f), Size(s * 0.68f, s * 0.6f))
        }
        Ico.PLUS -> glyph(tint) { col ->
            drawLine(col, Offset(s * 0.5f, s * 0.2f), Offset(s * 0.5f, s * 0.8f), s * 0.18f, StrokeCap.Round)
            drawLine(col, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.8f, s * 0.5f), s * 0.18f, StrokeCap.Round)
        }
        Ico.CROWN -> {
            val p = Path().apply {
                moveTo(s * 0.1f, s * 0.34f); lineTo(s * 0.31f, s * 0.56f); lineTo(s * 0.5f, s * 0.2f); lineTo(s * 0.69f, s * 0.56f)
                lineTo(s * 0.9f, s * 0.34f); lineTo(s * 0.81f, s * 0.82f); lineTo(s * 0.19f, s * 0.82f); close()
            }
            translate(0f, s * 0.05f) { drawPath(p, Color(0xFFA85A00)) }
            drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFF4B0), Color(0xFFFFC21A), Color(0xFFFF9E00)), s * 0.2f, s * 0.82f))
            drawRect(Color(0x33A85A00), Offset(s * 0.19f, s * 0.68f), Size(s * 0.62f, s * 0.14f))
            drawPath(p, Color(0xFF8A4A00), style = Stroke(ow * 0.6f, join = StrokeJoin.Round))
            drawCircle(Color(0xFFFF4D8D), s * 0.07f, Offset(s * 0.5f, s * 0.6f))
            drawCircle(Color(0xFF33D6F5), s * 0.05f, Offset(s * 0.31f, s * 0.7f))
            drawCircle(Color(0xFF33D6F5), s * 0.05f, Offset(s * 0.69f, s * 0.7f))
            for (pt in listOf(Offset(s * 0.1f, s * 0.34f), Offset(s * 0.5f, s * 0.2f), Offset(s * 0.9f, s * 0.34f))) {
                drawCircle(Color(0xFF8A4A00), s * 0.075f, pt); drawCircle(Color(0xFFFFF4B0), s * 0.05f, pt)
            }
            drawCircle(Color.White.copy(alpha = 0.8f), s * 0.03f, Offset(s * 0.46f, s * 0.57f))
        }
        Ico.CHEVRON -> glyph(tint ?: Outline) { col ->
            val p = Path().apply { moveTo(s * 0.38f, s * 0.2f); lineTo(s * 0.68f, s * 0.5f); lineTo(s * 0.38f, s * 0.8f) }
            drawPath(p, col, style = Stroke(s * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Ico.FLAME -> {
            val outer = Path().apply {
                moveTo(s * 0.5f, s * 0.06f)
                cubicTo(s * 0.62f, s * 0.3f, s * 0.86f, s * 0.42f, s * 0.84f, s * 0.64f)
                cubicTo(s * 0.82f, s * 0.84f, s * 0.66f, s * 0.94f, s * 0.5f, s * 0.94f)
                cubicTo(s * 0.32f, s * 0.94f, s * 0.16f, s * 0.82f, s * 0.17f, s * 0.62f)
                cubicTo(s * 0.18f, s * 0.46f, s * 0.3f, s * 0.38f, s * 0.34f, s * 0.24f)
                cubicTo(s * 0.4f, s * 0.34f, s * 0.42f, s * 0.4f, s * 0.44f, s * 0.44f)
                cubicTo(s * 0.5f, s * 0.32f, s * 0.52f, s * 0.2f, s * 0.5f, s * 0.06f); close()
            }
            drawPath(outer, Brush.verticalGradient(listOf(Color(0xFFFFB24A), Color(0xFFFF5A3C), Color(0xFFE8273D)), s * 0.06f, s * 0.94f))
            drawPath(outer, Color(0xFF9A1A1A), style = Stroke(ow * 0.55f, join = StrokeJoin.Round))
            val inner = Path().apply {
                moveTo(s * 0.5f, s * 0.42f)
                cubicTo(s * 0.6f, s * 0.56f, s * 0.68f, s * 0.64f, s * 0.66f, s * 0.75f)
                cubicTo(s * 0.64f, s * 0.86f, s * 0.56f, s * 0.9f, s * 0.5f, s * 0.9f)
                cubicTo(s * 0.4f, s * 0.9f, s * 0.33f, s * 0.82f, s * 0.34f, s * 0.72f)
                cubicTo(s * 0.36f, s * 0.6f, s * 0.46f, s * 0.54f, s * 0.5f, s * 0.42f); close()
            }
            drawPath(inner, Brush.verticalGradient(listOf(Color(0xFFFFF6B0), Color(0xFFFFC93C)), s * 0.42f, s * 0.9f))
        }
        Ico.CUP -> {
            val body = Path().apply {
                moveTo(s * 0.16f, s * 0.36f); lineTo(s * 0.84f, s * 0.36f); lineTo(s * 0.74f, s * 0.88f); lineTo(s * 0.26f, s * 0.88f); close()
            }
            translate(0f, s * 0.04f) { drawPath(body, Color(0xFF8A1E5A)) }
            drawPath(body, Brush.horizontalGradient(listOf(Color(0xFFFF8AC4), Color(0xFFFF4C97), Color(0xFFD12D78)), s * 0.16f, s * 0.84f))
            drawRect(Color.White.copy(alpha = 0.35f), Offset(s * 0.27f, s * 0.42f), Size(s * 0.07f, s * 0.38f))
            drawPath(body, Color(0xFF7E1546), style = Stroke(ow * 0.6f, join = StrokeJoin.Round))
            ball(this, Offset(s * 0.36f, s * 0.3f), s * 0.12f, 1)
            ball(this, Offset(s * 0.62f, s * 0.29f), s * 0.12f, 1)
            ball(this, Offset(s * 0.49f, s * 0.16f), s * 0.12f, 1)
            drawRoundRect(Color(0xFFFFD0E8), Offset(s * 0.12f, s * 0.34f), Size(s * 0.76f, s * 0.08f), CornerRadius(s * 0.04f))
            drawRoundRect(Color(0xFF7E1546), Offset(s * 0.12f, s * 0.34f), Size(s * 0.76f, s * 0.08f), CornerRadius(s * 0.04f), style = Stroke(ow * 0.45f))
        }
        Ico.FLAG -> {
            drawRoundRect(Color(0xFF6E5A8A), Offset(s * 0.2f, s * 0.1f), Size(s * 0.07f, s * 0.82f), CornerRadius(s * 0.035f))
            drawRoundRect(Color(0xFFE6E0F5), Offset(s * 0.21f, s * 0.1f), Size(s * 0.035f, s * 0.8f), CornerRadius(s * 0.02f))
            val flag = Path().apply {
                moveTo(s * 0.27f, s * 0.14f)
                cubicTo(s * 0.45f, s * 0.08f, s * 0.6f, s * 0.22f, s * 0.86f, s * 0.14f)
                lineTo(s * 0.86f, s * 0.5f)
                cubicTo(s * 0.6f, s * 0.58f, s * 0.45f, s * 0.44f, s * 0.27f, s * 0.5f); close()
            }
            translate(0f, s * 0.04f) { drawPath(flag, Color(0xFF1E9440)) }
            drawPath(flag, Brush.verticalGradient(listOf(Color(0xFF9DF58A), Color(0xFF2FBF4F)), s * 0.1f, s * 0.55f))
            drawPath(flag, Color(0xFF0E5A26), style = Stroke(ow * 0.5f, join = StrokeJoin.Round))
            star(this, Offset(s * 0.56f, s * 0.32f), s * 0.1f, Color.White)
            drawCircle(Color(0xFFFFC93C), s * 0.06f, Offset(s * 0.235f, s * 0.1f))
        }
        Ico.SPARK -> {
            val p = Path().apply {
                moveTo(c.x, c.y - s * 0.48f)
                quadraticTo(c.x + s * 0.07f, c.y - s * 0.07f, c.x + s * 0.48f, c.y)
                quadraticTo(c.x + s * 0.07f, c.y + s * 0.07f, c.x, c.y + s * 0.48f)
                quadraticTo(c.x - s * 0.07f, c.y + s * 0.07f, c.x - s * 0.48f, c.y)
                quadraticTo(c.x - s * 0.07f, c.y - s * 0.07f, c.x, c.y - s * 0.48f); close()
            }
            drawPath(p, tint ?: Color(0xFFFFF3B0))
            drawCircle(Color.White, s * 0.08f, c)
        }
    }
}

/** Draws a flat glyph with a soft drop shadow underneath (for white icons on jelly buttons). */
private inline fun DrawScope.glyph(tint: Color?, draw: DrawScope.(Color) -> Unit) {
    val s = min(size.width, size.height)
    translate(0f, s * 0.06f) { draw(GlyphShadow) }
    draw(tint ?: Color.White)
}

fun starPath(c: Offset, outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = -PI / 2 + i * PI / 5
        val x = c.x + (cos(a) * r).toFloat(); val y = c.y + (sin(a) * r).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun star(scope: DrawScope, c: Offset, r: Float, color: Color) {
    scope.drawPath(starPath(c, r, r * 0.45f), color)
}

/** Glossy marble drawn with the game's painter. */
fun ball(scope: DrawScope, c: Offset, r: Float, colorId: Int, skin: Int = 0) {
    scope.drawCircle(Color.Black.copy(alpha = 0.25f), r * 0.95f, c + Offset(r * 0.1f, r * 0.2f))
    scope.drawIntoCanvas { Painter.marble(it.nativeCanvas, c.x, c.y, r, colorId, skin) }
}
