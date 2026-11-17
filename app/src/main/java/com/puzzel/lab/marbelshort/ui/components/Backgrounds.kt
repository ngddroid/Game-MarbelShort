package com.puzzel.lab.marbelshort.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.puzzel.lab.marbelshort.ui.theme.SkyPalette
import com.puzzel.lab.marbelshort.ui.theme.Skies
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Seconds since this composable entered the composition, ticking every frame.
 * Read it only inside draw / graphicsLayer lambdas so that only drawing is invalidated.
 */
@Composable
fun rememberTime(): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t.floatValue = (it - start) / 1_000_000_000f }
    }
    return t
}

/** A pre-baked soft white dot used for bokeh and glows (one bitmap, drawn with alpha + scale). */
internal object SoftDot {
    private var cached: ImageBitmap? = null
    fun get(): ImageBitmap = cached ?: run {
        val n = 64
        val bmp = ImageBitmap(n, n)
        val canvas = androidx.compose.ui.graphics.Canvas(bmp)
        val c = Offset(n / 2f, n / 2f)
        val paint = Paint().apply {
            shader = RadialGradientShader(
                c, n / 2f,
                listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0f)),
                listOf(0f, 0.38f, 1f),
            )
        }
        canvas.drawCircle(c, n / 2f, paint)
        cached = bmp
        bmp
    }
}

/** Unit-size four point sparkle (radius 1), drawn with translate + scale. */
internal val SparklePath: Path = Path().apply {
    moveTo(0f, -1f)
    quadraticTo(0.14f, -0.14f, 1f, 0f)
    quadraticTo(0.14f, 0.14f, 0f, 1f)
    quadraticTo(-0.14f, 0.14f, -1f, 0f)
    quadraticTo(-0.14f, -0.14f, 0f, -1f)
    close()
}

/**
 * Layered sky: gradient + two coloured glows + optional candy-factory skyline (static, cached)
 * and a light animated layer of soft bokeh and twinkles.
 */
@Composable
fun SkyBackground(
    palette: SkyPalette = Skies.Menu,
    modifier: Modifier = Modifier,
    skyline: Boolean = true,
    particles: Boolean = true,
    seed: Int = 7,
) {
    Box(modifier.fillMaxSize()) {
        Spacer(
            Modifier
                .fillMaxSize()
                // cached in its own GPU layer: re-rendered only when the palette or size changes
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                val w = size.width
                val h = size.height
                val bg = Brush.verticalGradient(0f to palette.top, 0.55f to palette.mid, 1f to palette.bottom)
                val c1 = Offset(w * 0.1f, h * 0.18f)
                val c2 = Offset(w * 0.98f, h * 0.6f)
                val g1 = Brush.radialGradient(listOf(palette.glowA.copy(alpha = 0.55f), palette.glowA.copy(alpha = 0f)), c1, w * 0.9f)
                val g2 = Brush.radialGradient(listOf(palette.glowB.copy(alpha = 0.42f), palette.glowB.copy(alpha = 0f)), c2, w * 0.85f)
                val far = if (skyline) skylinePath(w, h, far = true) else null
                val near = if (skyline) skylinePath(w, h, far = false) else null
                val haze = Brush.verticalGradient(listOf(Color.Transparent, palette.bottom.copy(alpha = 0.55f)), h * 0.72f, h)
                onDrawBehind {
                    drawRect(bg)
                    drawCircle(g1, w * 0.9f, c1)
                    drawCircle(g2, w * 0.85f, c2)
                    if (far != null) drawPath(far, palette.silhouette.copy(alpha = 0.22f))
                    if (near != null) {
                        drawRect(haze, Offset(0f, h * 0.72f), Size(w, h * 0.28f))
                        drawPath(near, palette.silhouette.copy(alpha = 0.38f))
                    }
                }
            }
        )
        if (particles) SkyParticles(Modifier.fillMaxSize(), seed)
    }
}

/** Old name kept for screens that just want the default menu sky. */
@Composable
fun MenuBackground(modifier: Modifier = Modifier) = SkyBackground(Skies.Menu, modifier)

@Composable
private fun SkyParticles(modifier: Modifier, seed: Int, count: Int = 14) {
    val time = rememberTime()
    val parts = remember(seed) {
        val r = Random(seed)
        List(count) {
            floatArrayOf(
                r.nextFloat(), r.nextFloat(),          // x, y start
                10f + r.nextFloat() * 26f,             // radius dp
                (1 + r.nextInt(3)).toFloat(),          // laps per minute (keeps the loop seamless)
                r.nextFloat() * 6.28f,                 // sway phase
                0.10f + r.nextFloat() * 0.20f,         // alpha
            )
        }
    }
    val dot = remember { SoftDot.get() }
    Canvas(modifier) {
        val t = time.value
        val w = size.width
        val h = size.height
        val src = IntSize(dot.width, dot.height)
        for (p in parts) {
            val period = 60f / p[3]
            val y = (((p[1] - t / period) % 1f) + 1f) % 1f
            val r = p[2].dp.toPx()
            val x = p[0] * w + sin(t * 0.45f + p[4]) * 14.dp.toPx()
            val cy = y * (h + 2 * r) - r
            drawImage(dot, IntOffset.Zero, src, IntOffset((x - r).toInt(), (cy - r).toInt()), IntSize((2 * r).toInt(), (2 * r).toInt()), alpha = p[5])
        }
        for (i in 0 until 9) {
            val a = 0.5f + 0.5f * sin(t * 2.1f + i * 1.9f)
            val sx = ((i * 0.37f + 0.11f) % 1f) * w
            val sy = ((i * 0.61f + 0.07f) % 1f) * h * 0.82f
            val s = (4f + 4f * a).dp.toPx()
            translate(sx, sy) { scale(s, s, Offset.Zero) { drawPath(SparklePath, Color.White.copy(alpha = 0.2f + 0.55f * a)) } }
        }
    }
}

/** Candy-factory silhouettes: domed towers, chimneys, tanks and pipes. */
private fun skylinePath(w: Float, h: Float, far: Boolean): Path {
    val p = Path()
    val u = w / 360f
    val rnd = Random(if (far) 11 else 29)
    val baseH = h * (if (far) 0.13f else 0.075f)
    var x = -12f * u
    while (x < w + 12f * u) {
        val kind = rnd.nextInt(4)
        val bw = (if (far) 38f else 46f) * u * (0.75f + rnd.nextFloat() * 0.6f)
        val bh = baseH * (0.6f + rnd.nextFloat() * 0.8f)
        val top = h - bh
        when (kind) {
            0 -> { // domed tower
                p.addRoundRect(RoundRect(x, top, x + bw, h + 4f, CornerRadius(bw * 0.12f)))
                p.addOval(androidx.compose.ui.geometry.Rect(x - bw * 0.06f, top - bw * 0.5f, x + bw * 1.06f, top + bw * 0.5f))
            }
            1 -> { // chimney pair with round caps
                val cw = bw * 0.32f
                p.addRect(androidx.compose.ui.geometry.Rect(x, top - bh * 0.4f, x + cw, h + 4f))
                p.addOval(androidx.compose.ui.geometry.Rect(x - cw * 0.25f, top - bh * 0.4f - cw * 0.5f, x + cw * 1.25f, top - bh * 0.4f + cw * 0.5f))
                p.addRect(androidx.compose.ui.geometry.Rect(x + cw * 1.6f, top, x + cw * 2.5f, h + 4f))
                p.addRoundRect(RoundRect(x - cw * 0.2f, top + bh * 0.35f, x + bw, h + 4f, CornerRadius(cw * 0.3f)))
            }
            2 -> { // big tank
                val r = bw * 0.62f
                p.addOval(androidx.compose.ui.geometry.Rect(x, h - bh * 0.9f - r, x + 2 * r, h - bh * 0.9f + r))
                p.addRect(androidx.compose.ui.geometry.Rect(x + r * 0.3f, h - bh * 0.9f, x + r * 1.7f, h + 4f))
            }
            else -> { // pipe bridge
                p.addRoundRect(RoundRect(x, top + bh * 0.45f, x + bw * 1.3f, top + bh * 0.62f, CornerRadius(bh * 0.1f)))
                p.addRect(androidx.compose.ui.geometry.Rect(x + bw * 0.1f, top + bh * 0.5f, x + bw * 0.28f, h + 4f))
                p.addRect(androidx.compose.ui.geometry.Rect(x + bw * 1.0f, top + bh * 0.5f, x + bw * 1.18f, h + 4f))
            }
        }
        x += bw * (0.85f + rnd.nextFloat() * 0.5f)
    }
    return p
}

/** Slowly rotating sunburst with a soft centre glow. */
@Composable
fun SunRays(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    alpha: Float = 0.2f,
    rays: Int = 16,
    degPerSec: Float = 14f,
    radiusScale: Float = 0.75f,
) {
    val time = rememberTime()
    Spacer(
        modifier.drawWithCache {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.maxDimension * radiusScale
            val step = (2 * PI / rays).toFloat()
            val path = Path().apply {
                for (i in 0 until rays) {
                    val a0 = i * step - step * 0.23f
                    val a1 = i * step + step * 0.23f
                    moveTo(c.x, c.y)
                    lineTo(c.x + cos(a0) * r, c.y + sin(a0) * r)
                    lineTo(c.x + cos(a1) * r, c.y + sin(a1) * r)
                    close()
                }
            }
            val fade = Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), c, r)
            val glow = Brush.radialGradient(listOf(color.copy(alpha = (alpha * 1.8f).coerceAtMost(1f)), color.copy(alpha = 0f)), c, r * 0.42f)
            onDrawBehind {
                rotate(time.value * degPerSec, c) { drawPath(path, fade) }
                drawCircle(glow, r * 0.42f, c)
            }
        }
    )
}

private val ConfettiColors = listOf(
    Color(0xFFFF5FA2), Color(0xFFFFC93C), Color(0xFF33D6F5), Color(0xFF8EF08A), Color(0xFF9775FF), Color(0xFFFF9F1C), Color.White,
)

/** One-shot confetti burst from the top-centre of its bounds (pieces may fly outside them). Drag keeps it floaty. */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, count: Int = 70, seed: Int = 3) {
    val time = rememberTime()
    val parts = remember(seed) {
        val r = Random(seed)
        List(count) {
            val a = (-PI / 2 + (r.nextFloat() - 0.5f) * PI * 1.1f).toFloat()
            val v = 380f + r.nextFloat() * 560f
            floatArrayOf(
                cos(a) * v, sin(a) * v,                 // launch velocity dp/s
                r.nextFloat() * 360f, (r.nextFloat() - 0.5f) * 720f, // rotation, spin
                r.nextInt(ConfettiColors.size).toFloat(),
                6f + r.nextFloat() * 6f,                // size dp
                r.nextFloat() * 0.3f,                   // launch delay
                r.nextInt(3).toFloat(),                 // shape
                r.nextFloat() * 6.28f,                  // flutter phase
            )
        }
    }
    Canvas(modifier) {
        val now = time.value
        val ox = size.width / 2
        val oy = size.height * 0.12f
        val d = density
        val k = 2.4f          // air drag (1/s)
        val g = 520f          // gravity dp/s²; terminal fall speed g/k ≈ 215 dp/s
        for (p in parts) {
            val tt = now - p[6]
            if (tt <= 0f || tt > 3.6f) continue
            val e = 1f - kotlin.math.exp(-k * tt)
            val x = ox + (p[0] / k * e + sin(tt * 5f + p[8]) * 8f) * d
            val y = oy + (g / k * tt + (p[1] - g / k) / k * e) * d
            val alpha = if (tt > 2.8f) (3.6f - tt) / 0.8f else 1f
            val col = ConfettiColors[p[4].toInt()].copy(alpha = alpha)
            val s = p[5] * d
            val rot = p[2] + p[3] * tt
            val flip = cos(rot * 0.035f)
            rotate(rot, Offset(x, y)) {
                when (p[7].toInt()) {
                    0 -> drawRect(col, Offset(x - s / 2, y - s * 0.3f), Size(s, s * 0.6f * flip.coerceIn(0.15f, 1f)))
                    1 -> drawCircle(col, s * 0.42f, Offset(x, y))
                    else -> drawRoundRect(col, Offset(x - s * 0.2f, y - s * 0.6f), Size(s * 0.4f, s * 1.2f * flip.coerceIn(0.2f, 1f)), CornerRadius(s * 0.2f))
                }
            }
        }
    }
}
