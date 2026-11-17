package com.puzzel.lab.marbelshort.ui.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.puzzel.lab.marbelshort.level.LevelGenerator
import com.puzzel.lab.marbelshort.ui.components.SparklePath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Horizontal position of level [n]'s node as a fraction of the width from the centre. */
fun nodeX(n: Int): Float = sin(n * 0.85f) * 0.2f

/** Which side the scenery goes on (opposite the node): -1 left, +1 right. */
fun scenerySide(n: Int): Int = if (nodeX(n) > 0f) -1 else 1

/** Glass-tube road from node [n] up to node n+1. Finished stretches are filled with gold. */
fun DrawScope.drawRoad(n: Int, current: Int) {
    if (n >= LevelGenerator.MAX_LEVEL) return
    val w = size.width
    val h = size.height
    val x0 = w / 2 + nodeX(n) * w
    val x1 = w / 2 + nodeX(n + 1) * w
    val y0 = h / 2
    val y1 = y0 - h
    val path = Path().apply { moveTo(x0, y0); cubicTo(x0, y0 - h * 0.5f, x1, y1 + h * 0.5f, x1, y1) }
    drawPath(path, Color(0x33140A3C), style = Stroke(30.dp.toPx(), cap = StrokeCap.Round))
    drawPath(path, Color.White.copy(alpha = 0.26f), style = Stroke(25.dp.toPx(), cap = StrokeCap.Round))
    if (n < current) {
        drawPath(path, Color(0xFFE59A00), style = Stroke(15.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, Color(0xFFFFCB3D), style = Stroke(11.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, Color(0xFFFFF3B8), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 9.dp.toPx()))))
    } else {
        drawPath(path, Color(0x4D140A3C), style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, Color.White.copy(alpha = 0.6f), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 15.dp.toPx()))))
    }
    translate(-7.dp.toPx(), 0f) { drawPath(path, Color.White.copy(alpha = 0.3f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)) }
}

/** Disc colours per node state: top, bottom, depth, outline. */
private val NodeDone = arrayOf(Color(0xFFFFF0A0), Color(0xFFFFB300), Color(0xFFC47A00), Color(0xFF8A4A00))
private val NodeCurrent = arrayOf(Color(0xFFFFB8DC), Color(0xFFFF4C97), Color(0xFFC42A6E), Color(0xFF7E1546))
private val NodeLocked = arrayOf(Color(0xFFF4F1FB), Color(0xFFC9C1E0), Color(0xFF9D95B8), Color(0xFF5E5878))
private val NodeAd = arrayOf(Color(0xFFB5F3FF), Color(0xFF26ACF2), Color(0xFF1373BE), Color(0xFF0A4A80))
private val NodeBoss = arrayOf(Color(0xFFFFB0B0), Color(0xFFFF4D5E), Color(0xFFC0283A), Color(0xFF7D1222))

/** state: 0 done, 1 current, 2 locked, 3 unlocked by an ad. */
fun nodeOutline(state: Int, boss: Boolean): Color = nodeColors(state, boss)[3]

private fun nodeColors(state: Int, boss: Boolean) = when {
    state == 2 -> NodeLocked
    state == 3 -> NodeAd
    boss && state != 0 -> NodeBoss
    state == 1 -> NodeCurrent
    else -> NodeDone
}

/** Glossy 3D level disc filling the draw area. */
fun DrawScope.drawNodeDisc(state: Int, boss: Boolean) {
    val (top, bot, dark, outline) = nodeColors(state, boss)
    val r = size.width / 2
    val c = Offset(r, r)
    val d = r * 0.14f
    drawOval(Color(0x38140A3C), Offset(r * 0.12f, r * 1.62f), Size(r * 1.76f, r * 0.56f))
    drawCircle(dark, r, c + Offset(0f, d))
    drawCircle(Brush.verticalGradient(listOf(top, bot), 0f, r * 2f), r, c)
    drawCircle(Brush.radialGradient(listOf(Color.Transparent, dark.copy(alpha = 0.28f)), c, r), r, c)
    drawCircle(Color.White.copy(alpha = 0.55f), r * 0.82f, c, style = Stroke(2.2.dp.toPx()))
    drawOval(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.05f)), r * 0.14f, r * 0.7f), Offset(r * 0.42f, r * 0.14f), Size(r * 1.16f, r * 0.52f))
    drawCircle(outline, r, c + Offset(0f, d / 2), style = Stroke(2.2.dp.toPx()))
}

/** One decorative prop per item on the open side, themed by world. */
fun DrawScope.drawScenery(n: Int, world: Int) {
    val rnd = Random(n * 7919 + 13)
    if (rnd.nextFloat() < 0.18f) return
    val w = size.width
    val h = size.height
    val side = scenerySide(n)
    val cx = if (side < 0) w * (0.1f + rnd.nextFloat() * 0.1f) else w * (0.8f + rnd.nextFloat() * 0.1f)
    val by = h * (0.62f + rnd.nextFloat() * 0.3f)
    val s = (54f + rnd.nextFloat() * 22f).dp.toPx()
    val kind = rnd.nextInt(3)
    val tint = rnd.nextInt(4)
    drawOval(Color(0x2E140A3C), Offset(cx - s * 0.34f, by - s * 0.05f), Size(s * 0.68f, s * 0.12f))
    when (world.mod(5)) {
        0 -> when (kind) { 0 -> lollipop(cx, by, s, tint); 1 -> candyCane(cx, by, s); else -> gumdrop(cx, by, s, tint) }
        1 -> when (kind) { 0 -> seaweed(cx, by, s); 1 -> coral(cx, by, s, tint); else -> shell(cx, by, s) }
        2 -> when (kind) { 0 -> planet(cx, by - s * 0.3f, s, tint); 1 -> moon(cx, by - s * 0.3f, s); else -> starCluster(cx, by - s * 0.3f, s) }
        3 -> when (kind) { 0 -> crystals(cx, by, s, tint); 1 -> palm(cx, by, s); else -> cactus(cx, by, s) }
        else -> when (kind) { 0 -> pine(cx, by, s); 1 -> mushroom(cx, by, s); else -> bush(cx, by, s) }
    }
    // a couple of tiny sparkles near the prop
    for (i in 0 until 2) {
        val sx = cx + (rnd.nextFloat() - 0.5f) * s * 1.2f
        val sy = by - s * (0.4f + rnd.nextFloat() * 0.8f)
        val sr = (3f + rnd.nextFloat() * 4f).dp.toPx()
        translate(sx, sy) { scale(sr, sr, Offset.Zero) { drawPath(SparklePath, Color.White.copy(alpha = 0.6f)) } }
    }
}

private val CandyTints = listOf(Color(0xFFFF5FA2), Color(0xFF33D6F5), Color(0xFFFFC93C), Color(0xFF8EF08A))
private val Ink = Color(0xFF2A1463)

private fun DrawScope.outlinePath(p: Path, w: Float = 2.dp.toPx(), c: Color = Ink.copy(alpha = 0.55f)) =
    drawPath(p, c, style = Stroke(w, join = StrokeJoin.Round))

// ---------------- Candy Lab

private fun DrawScope.lollipop(cx: Float, by: Float, s: Float, tint: Int) {
    drawRoundRect(Color(0xFFDCD3F2), Offset(cx - s * 0.045f, by - s * 0.62f), Size(s * 0.09f, s * 0.62f), CornerRadius(s * 0.04f))
    drawRoundRect(Color.White, Offset(cx - s * 0.03f, by - s * 0.62f), Size(s * 0.035f, s * 0.6f), CornerRadius(s * 0.02f))
    val c = Offset(cx, by - s * 0.78f)
    val r = s * 0.3f
    val col = CandyTints[tint]
    drawCircle(col, r, c)
    for (i in 0 until 3) {
        val rr = r * (0.82f - i * 0.24f)
        drawArc(Color.White.copy(alpha = 0.9f), i * 120f, 210f, false, c - Offset(rr, rr), Size(rr * 2, rr * 2), style = Stroke(r * 0.15f, cap = StrokeCap.Round))
    }
    drawOval(Color.White.copy(alpha = 0.55f), c + Offset(-r * 0.6f, -r * 0.75f), Size(r * 0.7f, r * 0.35f))
    drawCircle(Ink.copy(alpha = 0.55f), r, c, style = Stroke(2.dp.toPx()))
}

private fun DrawScope.candyCane(cx: Float, by: Float, s: Float) {
    val p = Path().apply {
        moveTo(cx + s * 0.12f, by)
        lineTo(cx + s * 0.12f, by - s * 0.7f)
        cubicTo(cx + s * 0.12f, by - s * 1.02f, cx - s * 0.3f, by - s * 1.02f, cx - s * 0.3f, by - s * 0.74f)
    }
    drawPath(p, Ink.copy(alpha = 0.55f), style = Stroke(s * 0.2f, cap = StrokeCap.Round))
    drawPath(p, Color.White, style = Stroke(s * 0.16f, cap = StrokeCap.Round))
    drawPath(p, Color(0xFFFF4D5E), style = Stroke(s * 0.16f, cap = StrokeCap.Butt, pathEffect = PathEffect.dashPathEffect(floatArrayOf(s * 0.09f, s * 0.09f))))
    drawPath(p, Color.White.copy(alpha = 0.5f), style = Stroke(s * 0.04f, cap = StrokeCap.Round))
}

private fun DrawScope.gumdrop(cx: Float, by: Float, s: Float, tint: Int) {
    val col = CandyTints[(tint + 1) % 4]
    val p = Path().apply {
        moveTo(cx - s * 0.36f, by)
        cubicTo(cx - s * 0.4f, by - s * 0.55f, cx - s * 0.18f, by - s * 0.7f, cx, by - s * 0.7f)
        cubicTo(cx + s * 0.18f, by - s * 0.7f, cx + s * 0.4f, by - s * 0.55f, cx + s * 0.36f, by)
        close()
    }
    drawPath(p, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.8f).compositeOver(col), col), by - s * 0.7f, by))
    for (i in 0 until 7) {
        val a = i * 0.9f
        drawCircle(Color.White.copy(alpha = 0.85f), s * 0.025f, Offset(cx + cos(a) * s * 0.2f, by - s * 0.32f + sin(a * 1.7f) * s * 0.16f))
    }
    drawOval(Color.White.copy(alpha = 0.6f), Offset(cx - s * 0.22f, by - s * 0.6f), Size(s * 0.18f, s * 0.12f))
    outlinePath(p)
}

private fun Color.compositeOver(bg: Color): Color {
    val a = alpha
    return Color(red * a + bg.red * (1 - a), green * a + bg.green * (1 - a), blue * a + bg.blue * (1 - a), 1f)
}

// ---------------- Ocean

private fun DrawScope.seaweed(cx: Float, by: Float, s: Float) {
    val blades = listOf(-0.16f to 0.95f, 0f to 1.15f, 0.16f to 0.8f)
    blades.forEachIndexed { i, (dx, hh) ->
        val p = Path().apply {
            moveTo(cx + dx * s, by)
            cubicTo(cx + dx * s + s * 0.18f, by - s * hh * 0.35f, cx + dx * s - s * 0.18f, by - s * hh * 0.65f, cx + dx * s + s * 0.06f, by - s * hh)
        }
        drawPath(p, Color(0xFF0E6B4A), style = Stroke(s * 0.13f, cap = StrokeCap.Round))
        drawPath(p, if (i == 1) Color(0xFF3CCB7A) else Color(0xFF2FAE6A), style = Stroke(s * 0.09f, cap = StrokeCap.Round))
        drawPath(p, Color.White.copy(alpha = 0.3f), style = Stroke(s * 0.025f, cap = StrokeCap.Round))
    }
    for (k in 0 until 3) drawCircle(Color.White.copy(alpha = 0.6f), s * (0.035f + k * 0.012f), Offset(cx + s * 0.28f, by - s * (0.7f + k * 0.22f)), style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.coral(cx: Float, by: Float, s: Float, tint: Int) {
    val col = if (tint % 2 == 0) Color(0xFFFF7A9E) else Color(0xFFFF9F5A)
    val branches = listOf(
        floatArrayOf(0f, 0f, 0f, -0.75f), floatArrayOf(0f, -0.35f, -0.28f, -0.68f), floatArrayOf(0f, -0.28f, 0.26f, -0.6f),
        floatArrayOf(-0.28f, -0.68f, -0.34f, -0.88f), floatArrayOf(0.26f, -0.6f, 0.36f, -0.8f), floatArrayOf(0f, -0.75f, 0.08f, -0.95f),
    )
    for (b in branches) drawLine(Ink.copy(alpha = 0.5f), Offset(cx + b[0] * s, by + b[1] * s), Offset(cx + b[2] * s, by + b[3] * s), s * 0.15f, StrokeCap.Round)
    for (b in branches) drawLine(col, Offset(cx + b[0] * s, by + b[1] * s), Offset(cx + b[2] * s, by + b[3] * s), s * 0.11f, StrokeCap.Round)
    for (b in branches) drawCircle(Color.White.copy(alpha = 0.55f), s * 0.025f, Offset(cx + b[2] * s - s * 0.02f, by + b[3] * s - s * 0.02f))
}

private fun DrawScope.shell(cx: Float, by: Float, s: Float) {
    val p = Path().apply {
        moveTo(cx, by - s * 0.05f)
        lineTo(cx - s * 0.38f, by - s * 0.42f)
        cubicTo(cx - s * 0.36f, by - s * 0.78f, cx + s * 0.36f, by - s * 0.78f, cx + s * 0.38f, by - s * 0.42f)
        close()
    }
    drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFE6D6), Color(0xFFFFB38A)), by - s * 0.72f, by))
    for (i in -3..3) drawLine(Color(0xFFE08A6A), Offset(cx, by - s * 0.08f), Offset(cx + i * s * 0.1f, by - s * (0.66f - kotlin.math.abs(i) * 0.04f)), s * 0.025f, StrokeCap.Round)
    drawRoundRect(Color(0xFFFFC7A8), Offset(cx - s * 0.12f, by - s * 0.12f), Size(s * 0.24f, s * 0.1f), CornerRadius(s * 0.04f))
    outlinePath(p)
}

// ---------------- Space

private fun DrawScope.planet(cx: Float, cy: Float, s: Float, tint: Int) {
    val r = s * 0.3f
    val cols = listOf(Color(0xFFFFB86B) to Color(0xFFFF5F8F), Color(0xFF8FE3FF) to Color(0xFF5A6BFF), Color(0xFFB9F07A) to Color(0xFF2FAE8A), Color(0xFFFFE680) to Color(0xFFFF9F1C))[tint]
    val ringRect = Offset(cx - r * 1.75f, cy - r * 0.45f)
    val ringSize = Size(r * 3.5f, r * 0.9f)
    rotate(-18f, Offset(cx, cy)) {
        drawArc(Color.White.copy(alpha = 0.75f), 180f, 180f, false, ringRect, ringSize, style = Stroke(s * 0.05f))
        drawCircle(Brush.verticalGradient(listOf(cols.first, cols.second), cy - r, cy + r), r, Offset(cx, cy))
        drawArc(Color(0x33000000), 20f, 140f, false, Offset(cx - r, cy - r), Size(2 * r, 2 * r), style = Stroke(r * 0.3f))
        drawCircle(Ink.copy(alpha = 0.5f), r, Offset(cx, cy), style = Stroke(1.8.dp.toPx()))
        drawArc(Color.White.copy(alpha = 0.9f), 0f, 180f, false, ringRect, ringSize, style = Stroke(s * 0.05f))
        drawOval(Color.White.copy(alpha = 0.5f), Offset(cx - r * 0.6f, cy - r * 0.75f), Size(r * 0.6f, r * 0.3f))
    }
}

private fun DrawScope.moon(cx: Float, cy: Float, s: Float) {
    val r = s * 0.3f
    drawCircle(Color(0x40FFFFFF), r * 1.35f, Offset(cx, cy))
    drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFFBEA), Color(0xFFE0D8F5)), cy - r, cy + r), r, Offset(cx, cy))
    drawCircle(Color(0xFFCFC6EA), r * 0.22f, Offset(cx - r * 0.3f, cy - r * 0.2f))
    drawCircle(Color(0xFFCFC6EA), r * 0.14f, Offset(cx + r * 0.35f, cy + r * 0.25f))
    drawCircle(Color(0xFFCFC6EA), r * 0.1f, Offset(cx + r * 0.1f, cy - r * 0.5f))
    drawCircle(Ink.copy(alpha = 0.4f), r, Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
}

private fun DrawScope.starCluster(cx: Float, cy: Float, s: Float) {
    val pts = listOf(Triple(0f, 0f, 0.22f), Triple(-0.3f, 0.25f, 0.12f), Triple(0.28f, -0.22f, 0.1f), Triple(0.22f, 0.3f, 0.08f))
    for ((dx, dy, r) in pts) {
        translate(cx + dx * s, cy + dy * s) { scale(r * s, r * s, Offset.Zero) { drawPath(SparklePath, Color(0xFFFFF3B0)) } }
    }
    drawCircle(Color.White.copy(alpha = 0.8f), s * 0.03f, Offset(cx - s * 0.12f, cy - s * 0.3f))
    drawCircle(Color.White.copy(alpha = 0.8f), s * 0.025f, Offset(cx + s * 0.35f, cy + s * 0.05f))
}

// ---------------- Sunset

private fun DrawScope.crystals(cx: Float, by: Float, s: Float, tint: Int) {
    val cols = listOf(Color(0xFFFF8AD8) to Color(0xFFB04FE0), Color(0xFF8FE3FF) to Color(0xFF5A6BFF), Color(0xFFFFD36E) to Color(0xFFFF7A59), Color(0xFFC6A8FF) to Color(0xFF7A3FE0))[tint]
    val gems = listOf(Triple(0f, 0.95f, 0.17f), Triple(-0.22f, 0.62f, 0.13f), Triple(0.22f, 0.7f, 0.14f))
    for ((dx, hh, ww) in gems) {
        val x = cx + dx * s
        val p = Path().apply {
            moveTo(x - ww * s, by); lineTo(x - ww * s, by - hh * s * 0.72f); lineTo(x, by - hh * s); lineTo(x + ww * s, by - hh * s * 0.72f); lineTo(x + ww * s, by); close()
        }
        drawPath(p, Brush.horizontalGradient(listOf(cols.first, cols.second), x - ww * s, x + ww * s))
        drawPath(Path().apply { moveTo(x, by - hh * s); lineTo(x, by); lineTo(x - ww * s, by); lineTo(x - ww * s, by - hh * s * 0.72f); close() }, Color.White.copy(alpha = 0.25f))
        outlinePath(p)
    }
}

private fun DrawScope.palm(cx: Float, by: Float, s: Float) {
    val trunk = Path().apply { moveTo(cx, by); cubicTo(cx + s * 0.05f, by - s * 0.4f, cx + s * 0.12f, by - s * 0.7f, cx + s * 0.06f, by - s * 0.95f) }
    drawPath(trunk, Color(0xFF6B3A2E), style = Stroke(s * 0.12f, cap = StrokeCap.Round))
    drawPath(trunk, Color(0xFFB0704A), style = Stroke(s * 0.08f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(s * 0.07f, s * 0.03f))))
    val top = Offset(cx + s * 0.06f, by - s * 0.95f)
    for (i in 0 until 5) {
        val a = (-160f + i * 35f) * PI.toFloat() / 180f
        val end = top + Offset(cos(a) * s * 0.42f, sin(a) * s * 0.42f + s * 0.18f)
        val leaf = Path().apply { moveTo(top.x, top.y); quadraticTo(top.x + cos(a) * s * 0.3f, top.y + sin(a) * s * 0.3f - s * 0.08f, end.x, end.y) }
        drawPath(leaf, Color(0xFF145E3A), style = Stroke(s * 0.11f, cap = StrokeCap.Round))
        drawPath(leaf, Color(0xFF3CB46A), style = Stroke(s * 0.075f, cap = StrokeCap.Round))
    }
    drawCircle(Color(0xFF7A4A2E), s * 0.05f, top + Offset(-s * 0.04f, s * 0.04f))
}

private fun DrawScope.cactus(cx: Float, by: Float, s: Float) {
    val g = Brush.horizontalGradient(listOf(Color(0xFF5ED38A), Color(0xFF239A5C)))
    drawRoundRect(Ink.copy(alpha = 0.5f), Offset(cx - s * 0.13f, by - s * 0.82f), Size(s * 0.26f, s * 0.82f), CornerRadius(s * 0.13f))
    drawRoundRect(g, Offset(cx - s * 0.11f, by - s * 0.8f), Size(s * 0.22f, s * 0.8f), CornerRadius(s * 0.11f))
    val armL = Path().apply { moveTo(cx - s * 0.1f, by - s * 0.38f); lineTo(cx - s * 0.28f, by - s * 0.38f); lineTo(cx - s * 0.28f, by - s * 0.62f) }
    val armR = Path().apply { moveTo(cx + s * 0.1f, by - s * 0.48f); lineTo(cx + s * 0.26f, by - s * 0.48f); lineTo(cx + s * 0.26f, by - s * 0.7f) }
    for (a in listOf(armL, armR)) {
        drawPath(a, Ink.copy(alpha = 0.5f), style = Stroke(s * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(a, Color(0xFF3CB46A), style = Stroke(s * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    drawLine(Color.White.copy(alpha = 0.4f), Offset(cx - s * 0.04f, by - s * 0.72f), Offset(cx - s * 0.04f, by - s * 0.12f), s * 0.03f, StrokeCap.Round)
    drawCircle(Color(0xFFFF5FA2), s * 0.07f, Offset(cx, by - s * 0.82f))
    drawCircle(Color(0xFFFFC93C), s * 0.03f, Offset(cx, by - s * 0.82f))
}

// ---------------- Forest

private fun DrawScope.pine(cx: Float, by: Float, s: Float) {
    drawRoundRect(Color(0xFF6B3A2E), Offset(cx - s * 0.06f, by - s * 0.2f), Size(s * 0.12f, s * 0.2f), CornerRadius(s * 0.03f))
    val tiers = listOf(Triple(0.18f, 0.4f, Color(0xFF1E7A4C)), Triple(0.42f, 0.33f, Color(0xFF2A9A5C)), Triple(0.64f, 0.25f, Color(0xFF3CB46A)))
    for ((y, hw, col) in tiers) {
        val base = by - s * y
        val p = Path().apply { moveTo(cx - s * hw, base); lineTo(cx, base - s * 0.42f); lineTo(cx + s * hw, base); close() }
        drawPath(p, col)
        drawPath(Path().apply { moveTo(cx, base - s * 0.42f); lineTo(cx - s * hw, base); lineTo(cx - s * hw * 0.4f, base); close() }, Color.White.copy(alpha = 0.18f))
        outlinePath(p, 1.8.dp.toPx())
    }
}

private fun DrawScope.mushroom(cx: Float, by: Float, s: Float) {
    drawRoundRect(Color(0xFFF6EBDD), Offset(cx - s * 0.1f, by - s * 0.42f), Size(s * 0.2f, s * 0.42f), CornerRadius(s * 0.08f))
    drawRoundRect(Ink.copy(alpha = 0.4f), Offset(cx - s * 0.1f, by - s * 0.42f), Size(s * 0.2f, s * 0.42f), CornerRadius(s * 0.08f), style = Stroke(1.6.dp.toPx()))
    val cap = Path().apply {
        moveTo(cx - s * 0.4f, by - s * 0.38f)
        cubicTo(cx - s * 0.4f, by - s * 0.88f, cx + s * 0.4f, by - s * 0.88f, cx + s * 0.4f, by - s * 0.38f)
        close()
    }
    drawPath(cap, Brush.verticalGradient(listOf(Color(0xFFFF7A7A), Color(0xFFE8273D)), by - s * 0.8f, by - s * 0.38f))
    drawCircle(Color.White, s * 0.06f, Offset(cx - s * 0.16f, by - s * 0.58f))
    drawCircle(Color.White, s * 0.045f, Offset(cx + s * 0.14f, by - s * 0.66f))
    drawCircle(Color.White, s * 0.04f, Offset(cx + s * 0.24f, by - s * 0.47f))
    drawCircle(Color.White, s * 0.035f, Offset(cx - s * 0.02f, by - s * 0.74f))
    outlinePath(cap)
}

private fun DrawScope.bush(cx: Float, by: Float, s: Float) {
    val blobs = listOf(Triple(-0.2f, -0.24f, 0.24f), Triple(0.2f, -0.24f, 0.24f), Triple(0f, -0.4f, 0.28f))
    for ((dx, dy, r) in blobs) drawCircle(Ink.copy(alpha = 0.45f), r * s + 1.5.dp.toPx(), Offset(cx + dx * s, by + dy * s))
    for ((dx, dy, r) in blobs) drawCircle(Brush.verticalGradient(listOf(Color(0xFF7BE07F), Color(0xFF2A9A5C)), by + (dy - r) * s, by + (dy + r) * s), r * s, Offset(cx + dx * s, by + dy * s))
    for (k in 0 until 4) drawCircle(Color(0xFFFF4D6D), s * 0.045f, Offset(cx + (k - 1.5f) * s * 0.14f, by - s * (0.3f + (k % 2) * 0.16f)))
    drawOval(Color.White.copy(alpha = 0.35f), Offset(cx - s * 0.16f, by - s * 0.62f), Size(s * 0.22f, s * 0.1f))
}
