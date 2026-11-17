package com.puzzel.lab.marbelshort.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.marbelshort.R
import com.puzzel.lab.marbelshort.core.Sfx
import com.puzzel.lab.marbelshort.render.Painter
import com.puzzel.lab.marbelshort.ui.components.BtnColor
import com.puzzel.lab.marbelshort.ui.components.GameButton
import com.puzzel.lab.marbelshort.ui.components.IconTile
import com.puzzel.lab.marbelshort.ui.components.Ico
import com.puzzel.lab.marbelshort.ui.components.LocalSound
import com.puzzel.lab.marbelshort.ui.components.OutlinedText
import com.puzzel.lab.marbelshort.ui.components.ProgressTube
import com.puzzel.lab.marbelshort.ui.components.SkyBackground
import com.puzzel.lab.marbelshort.ui.components.SparklePath
import com.puzzel.lab.marbelshort.ui.components.SunRays
import com.puzzel.lab.marbelshort.ui.components.ball
import com.puzzel.lab.marbelshort.ui.components.floaty
import com.puzzel.lab.marbelshort.ui.components.idleBounce
import com.puzzel.lab.marbelshort.ui.components.rememberTime
import com.puzzel.lab.marbelshort.ui.theme.Fredoka
import com.puzzel.lab.marbelshort.ui.theme.GameColors
import com.puzzel.lab.marbelshort.ui.theme.Grad
import com.puzzel.lab.marbelshort.ui.theme.Motion
import com.puzzel.lab.marbelshort.ui.theme.Skies
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ============================================================ SPLASH

/**
 * Starts identical to the system splash (same purple, same logo, same size), then the sky fades in,
 * the logo glides up and the title pops in word by word while a glass tube loads.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val sound = LocalSound.current
    val intro = remember { Animatable(0f) }
    val words = remember { List(3) { Animatable(0f) } }
    val tagline = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    val time = rememberTime()
    LaunchedEffect(Unit) {
        launch { progress.animateTo(1f, tween(2700, delayMillis = 200, easing = FastOutSlowInEasing)) }
        delay(120)
        launch { intro.animateTo(1f, tween(720, easing = Motion.EaseIn)) }
        delay(380)
        words.forEachIndexed { i, a ->
            launch { a.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f)) }
            sound?.play(Sfx.POP, 1f + i * 0.12f, 0.7f)
            delay(170)
        }
        launch { tagline.animateTo(1f, tween(450)) }
        sound?.play(Sfx.CUP_COMPLETE)
        delay(1650)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF6A44F0))) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = intro.value }) {
            SkyBackground(Skies.Menu)
            SunRays(Modifier.align(Alignment.Center).offset(y = (-130).dp).requiredSize(620.dp), alpha = 0.16f, degPerSec = 9f)
        }
        Image(
            painterResource(R.drawable.ic_splash_logo), contentDescription = null,
            modifier = Modifier.align(Alignment.Center).size(288.dp).graphicsLayer {
                val k = intro.value
                val s = 1f - 0.2f * k
                scaleX = s; scaleY = s
                translationY = -k * 130.dp.toPx() + sin(time.value * 2.2f) * 4.dp.toPx() * k
            },
        )
        Column(Modifier.align(Alignment.Center).offset(y = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val spec = listOf(Triple("Marble", Grad.Gold, 54), Triple("Pop", Grad.Pink, 78), Triple("Factory", Grad.Aqua, 50))
            spec.forEachIndexed { i, (w, brush, size) ->
                val a = words[i]
                OutlinedText(
                    w,
                    Modifier.offset(y = (-i * 12).dp).graphicsLayer {
                        val v = a.value
                        scaleX = v; scaleY = v; alpha = v.coerceIn(0f, 1f)
                        rotationZ = (if (i % 2 == 0) -4f else 4f) * v + (1f - v) * 18f
                    },
                    fontSize = size.sp, brush = brush, gloss = true, outline = GameColors.Night,
                )
            }
            Text(
                "Tap  ·  Pop  ·  Sort!",
                style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White, letterSpacing = 1.sp),
                modifier = Modifier.offset(y = (-24).dp).graphicsLayer { alpha = tagline.value; translationY = (1f - tagline.value) * 10.dp.toPx() },
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 64.dp).graphicsLayer { alpha = intro.value },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadingTube({ progress.value }, time)
        }
    }
}

/** Own recomposition scope so the per-frame progress only refreshes the bar and its label. */
@Composable
private fun LoadingTube(progress: () -> Float, time: androidx.compose.runtime.State<Float>) {
    val p = progress()
    Box(Modifier.width(250.dp).height(30.dp)) {
        ProgressTube(p, Modifier.fillMaxSize(), height = 30.dp, fill = BtnColor.Pink)
        Canvas(Modifier.fillMaxSize()) {
            val w = (size.width * p).coerceAtLeast(size.height)
            ball(this, Offset(w - size.height / 2, size.height / 2), size.height * 0.4f, ((time.value * 2.5f).toInt()) % 7)
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Loading ${(p * 100).toInt()}%",
        style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White.copy(alpha = 0.9f)),
    )
}

// ============================================================ ONBOARDING (no skip)

private data class OnbPage(val title: String, val text: String)
private val pages = listOf(
    OnbPage("Tap to Pop!", "Tap a block with a clear path to the bottom. It bursts into a shower of shiny marbles!"),
    OnbPage("Ride the Belt", "Marbles pour through the funnel onto the conveyor belt and loop around the track."),
    OnbPage("Sort the Cups", "Each marble drops into a cup of the same colour. Fill a cup with 3 to clear it!"),
    OnbPage("Don't Jam the Belt!", "The belt has limited slots. Plan your taps, chain combos and use boosters when things get busy."),
)
private val pageColors = intArrayOf(6, 2, 1, 3)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val time = rememberTime()
    val sound = LocalSound.current
    LaunchedEffect(pager.currentPage) { if (pager.currentPage > 0) sound?.play(Sfx.WHOOSH, 1.2f, 0.5f) }
    Box(Modifier.fillMaxSize()) {
        SkyBackground(Skies.Menu)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedText("How to play", fontSize = 22.sp, brush = Grad.Snow)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.background(Color(0x66140A3C), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 2.dp)) {
                    OutlinedText("${pager.currentPage + 1} / ${pages.size}", fontSize = 15.sp, shadow = false)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { p ->
                Column(Modifier.fillMaxSize().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val offset = { (pager.currentPage - p) + pager.currentPageOffsetFraction }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.95f)
                            .graphicsLayer {
                                val o = offset()
                                translationX = o * size.width * 0.32f
                                val s = 1f - min(1f, abs(o)) * 0.1f
                                scaleX = s; scaleY = s
                                rotationZ = o * -4f
                            }
                            .drawBehind { drawRoundRect(Color(0x4D0D0530), Offset(0f, 12.dp.toPx()), size, CornerRadius(38.dp.toPx())) }
                            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD9CCFF))), RoundedCornerShape(38.dp))
                            .padding(5.dp)
                            .clip(RoundedCornerShape(33.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFFFBF9FF), Color(0xFFE6DDFF)))),
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val t = time.value
                            when (p) {
                                0 -> demoTap(this, t)
                                1 -> demoBelt(this, t, 12, false)
                                2 -> demoSort(this, t)
                                else -> demoBelt(this, t, 0, true)
                            }
                        }
                        if (p == 3) {
                            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(Ico.HAMMER, Ico.MAGNET, Ico.SLOTS, Ico.SHUFFLE).forEachIndexed { i, ic ->
                                    IconTile(ic, BtnColor.Blue, Modifier.floaty(3.dp, 2200, i * 0.25f), size = 54.dp, iconScale = 0.66f)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Column(
                        Modifier.graphicsLayer { val o = offset(); translationX = o * size.width * 0.12f; alpha = 1f - min(1f, abs(o)) * 0.7f },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        OutlinedText(pages[p].title, fontSize = 38.sp, brush = Grad.Gold, gloss = true, outline = GameColors.Night)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            pages[p].text,
                            style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center, lineHeight = 25.sp),
                        )
                    }
                }
            }
            // marble progress dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                for (i in pages.indices) {
                    val sel by animateFloatAsState(if (i <= pager.currentPage) 1f else 0f, Motion.bouncy(), label = "dot")
                    val cur by animateFloatAsState(if (i == pager.currentPage) 1f else 0f, Motion.bouncy(), label = "cur")
                    Canvas(Modifier.size(30.dp)) {
                        val rIdle = size.width * 0.18f
                        drawCircle(Color.White.copy(alpha = 0.35f), rIdle)
                        val r = rIdle + (size.width * 0.27f - rIdle) * sel + size.width * 0.12f * cur
                        if (sel > 0.02f) ball(this, center, r * sel.coerceIn(0f, 1f), pageColors[i])
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            val last = pager.currentPage == pages.lastIndex
            GameButton(
                if (last) "Let's Play!" else "Next",
                Modifier.padding(horizontal = 22.dp).fillMaxWidth().idleBounce(last),
                color = if (last) BtnColor.Green else BtnColor.Yellow, height = 68.dp, fontSize = 30.sp, shine = last,
                icon = if (last) Ico.PLAY else null,
            ) {
                if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1, animationSpec = tween(Motion.EMPHASIZED, easing = Motion.EaseIn)) }
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

/** Page 1: only bottom-row blocks are open; tapping one pops it, its marbles stream out and the block above wakes up. */
private fun demoTap(d: DrawScope, t: Float) = with(d) {
    val w = size.width
    val h = size.height
    val cell = w * 0.2f
    val gx = w / 2 - cell * 1.5f
    val gy = h * 0.1f
    val boardW = cell * 3
    val boardH = cell * 3
    val cycle = 3.8f
    val k = t % cycle
    val colors = arrayOf(intArrayOf(2, 6, 1), intArrayOf(0, 3, 4), intArrayOf(5, 2, 7)) // [row from bottom][col]
    val popAt = 1.0f
    val popped = k >= popAt && k < cycle - 0.25f
    val wake = ((k - 1.35f) / 0.4f).coerceIn(0f, 1f)
    val fadeAll = if (k > cycle - 0.25f) (cycle - k) / 0.25f else 1f

    // board + exit chute
    val ex = gx + cell * 1.5f
    val chute = Path().apply {
        moveTo(gx - 14f, gy + boardH + 10f); lineTo(gx + boardW + 14f, gy + boardH + 10f)
        lineTo(ex + cell * 0.34f, h * 0.84f); lineTo(ex - cell * 0.34f, h * 0.84f); close()
    }
    drawPath(chute, Color(0xFFEDE6FF)); drawPath(chute, Color(0xFFB7A7EA), style = Stroke(4f))
    drawRoundRect(Color(0xFF8E6EF2), Offset(gx - 14f, gy - 14f), Size(boardW + 28f, boardH + 28f), CornerRadius(28f))
    drawRoundRect(Color(0xFFF6F2FF), Offset(gx - 5f, gy - 5f), Size(boardW + 10f, boardH + 10f), CornerRadius(20f))
    // exit arrows under each column (the bottom row is the exit)
    for (col in 0 until 3) {
        val ax = gx + cell * (col + 0.5f)
        val ay = gy + boardH + 22f
        val bob = sin(t * 5f + col) * 3f
        val arr = Path().apply { moveTo(ax - 10f, ay + bob); lineTo(ax, ay + 10f + bob); lineTo(ax + 10f, ay + bob) }
        drawPath(arr, Color(0xFF8E6EF2).copy(alpha = 0.55f), style = Stroke(5f, cap = StrokeCap.Round))
    }

    drawIntoCanvas { cv ->
        val c = cv.nativeCanvas
        for (row in 0 until 3) for (col in 0 until 3) {
            if (row == 0 && col == 1 && popped) continue
            val x = gx + col * cell
            val y = gy + (2 - row) * cell
            val open = row == 0 || (row == 1 && col == 1 && wake >= 0.5f && popped)
            var s = cell * 0.96f
            if (row == 0 && col == 1 && k in 0.78f..popAt) s *= 0.9f
            if (row == 1 && col == 1 && popped && wake in 0.01f..0.99f) s *= 1f + 0.14f * sin(wake * PI.toFloat())
            Painter.block(c, x + (cell - s) / 2, y + (cell - s) / 2, s, colors[row][col], 0, bumps = open)
        }
    }

    if (popped) {
        val tt = k - popAt
        val y0 = gy + 2.5f * cell
        if (tt < 0.4f) {
            val p = tt / 0.4f
            drawCircle(Color.White.copy(alpha = 1f - p), cell * 0.3f + p * cell * 0.6f, Offset(ex, y0), style = Stroke(8f * (1f - p) + 1f))
            for (sIdx in 0 until 6) {
                val a = sIdx * PI.toFloat() / 3f
                val rr = cell * (0.3f + p * 0.7f)
                drawIntoCanvas { Painter.star4(it.nativeCanvas, ex + cos(a) * rr, y0 + sin(a) * rr, 12f * (1f - p) + 3f, android.graphics.Color.argb(((1f - p) * 255).toInt(), 255, 220, 90)) }
            }
        }
        for (m in 0 until 9) {
            val lt = tt - m * 0.08f
            if (lt < 0f) continue
            val y = y0 + lt * lt * h * 1.6f + lt * cell
            if (y > h * 0.86f) continue
            val spread = 1f - ((y - y0) / (h * 0.86f - y0)).coerceIn(0f, 1f)
            val x = ex + sin(m * 1.7f) * cell * 0.28f * spread
            ball(this, Offset(x, y), cell * 0.13f, colors[0][1])
        }
    }
    // "open" sparkle on the block that just woke up
    if (popped && wake > 0.3f) {
        val a = (1f - abs(wake - 0.65f) * 2.5f).coerceIn(0f, 1f) * fadeAll
        val cx = ex; val cy = gy + 1.5f * cell
        translateSparkle(cx + cell * 0.38f, cy - cell * 0.38f, 14f, a)
    }

    // hand
    val hx = ex + cell * 0.1f
    val hy = gy + 2.6f * cell
    val approach = min(1f, k / 0.65f)
    val press = if (k in 0.65f..1.0f) 1f else 0f
    if (k < 1.5f) {
        val leave = ((k - 1.1f) / 0.4f).coerceIn(0f, 1f)
        val px = hx + (1f - approach) * w * 0.3f + leave * w * 0.25f
        val py = hy + (1f - approach) * h * 0.2f + press * 8f + leave * h * 0.12f
        val alpha = 1f - leave
        drawCircle(Color.White.copy(alpha = alpha), cell * 0.22f, Offset(px, py))
        drawRoundRect(Color.White.copy(alpha = alpha), Offset(px - cell * 0.1f, py), Size(cell * 0.38f, cell * 0.5f), CornerRadius(cell * 0.15f))
        drawCircle(GameColors.Ink.copy(alpha = alpha), cell * 0.22f, Offset(px, py), style = Stroke(4f))
        if (press > 0f) drawCircle(Color.White.copy(alpha = 0.7f), cell * 0.42f, Offset(px, py - 6f), style = Stroke(5f))
    }
}

private fun DrawScope.translateSparkle(x: Float, y: Float, r: Float, alpha: Float) {
    translate(x, y) { scale(r, r, Offset.Zero) { drawPath(SparklePath, Color(0xFFFFE680).copy(alpha = alpha)) } }
}

private fun demoBelt(d: DrawScope, t: Float, fixedCount: Int, danger: Boolean) = with(d) {
    val w = size.width; val h = size.height
    val cy = h * 0.42f
    val r = h * 0.12f
    val left = w * 0.18f; val right = w * 0.82f
    val straight = right - left
    val len = 2 * straight + 2 * PI.toFloat() * r
    drawRoundRect(Color(0xFFC6B9EE), Offset(left - r - 26f, cy - r - 26f), Size(straight + 2 * r + 52f, 2 * r + 52f), CornerRadius(r + 26f))
    drawRoundRect(Color(0xFF3B2F86), Offset(left - r - 16f, cy - r - 16f), Size(straight + 2 * r + 32f, 2 * r + 32f), CornerRadius(r + 16f))
    drawRoundRect(Color.White, Offset(left - r + 18f, cy - r + 18f), Size(straight + 2 * r - 36f, 2 * r - 36f), CornerRadius(r - 18f))
    val slots = 30
    val count = if (danger) (((t % 5f) / 4f).coerceAtMost(1f) * slots).toInt() else fixedCount
    val off = t * 90f
    fun pt(s0: Float): Offset {
        var s = s0 % len; if (s < 0) s += len
        val half = straight / 2
        val arc = PI.toFloat() * r
        return when {
            s < half -> Offset(w / 2 + s, cy - r)
            s < half + arc -> { val a = (s - half) / r; Offset(right + r * sin(a), cy - r * cos(a)) }
            s < half + arc + straight -> Offset(right - (s - half - arc), cy + r)
            s < half + 2 * arc + straight -> { val a = (s - half - arc - straight) / r; Offset(left - r * sin(a), cy + r * cos(a)) }
            else -> Offset(left + (s - half - 2 * arc - straight), cy - r)
        }
    }
    for (i in 0 until slots) drawCircle(Color(0xFF1E1750), r * 0.2f, pt(i * len / slots + off))
    for (i in 0 until count.coerceAtMost(slots)) ball(this, pt(i * len / slots + off), r * 0.21f, intArrayOf(0, 1, 2, 3, 4, 6)[i % 6])
    if (!danger) {
        val funnel = Path().apply { moveTo(w * 0.3f, h * 0.02f); lineTo(w * 0.7f, h * 0.02f); lineTo(w / 2 + 20f, cy - r - 30f); lineTo(w / 2 - 20f, cy - r - 30f); close() }
        drawPath(funnel, Color(0xFFEDE6FF)); drawPath(funnel, Color(0xFFB7A7EA), style = Stroke(4f))
        val fall = (t * 1.6f) % 1f
        ball(this, Offset(w / 2, h * 0.08f + fall * (cy - r - h * 0.1f)), r * 0.21f, 2)
        for (k in 0 until 3) {
            val cx = w * (0.3f + k * 0.2f)
            drawIntoCanvas { Painter.cup(it.nativeCanvas, cx - w * 0.08f, h * 0.7f, w * 0.16f, h * 0.09f, k * 2, false) }
        }
    } else {
        val ratio = count / slots.toFloat()
        val col = if (ratio > 0.78f) 0xFFFF4D5E.toInt() else 0xFF6A5ACD.toInt()
        drawIntoCanvas { Painter.text(it.nativeCanvas, "$count/$slots", w / 2, cy, r * 0.7f, col, android.graphics.Color.WHITE, 6f) }
        if (ratio > 0.78f) {
            val a = 0.5f + 0.5f * sin(t * 10f)
            drawRoundRect(Color(0xFFFF2E4A).copy(alpha = 0.35f * a), Offset.Zero, size, CornerRadius(36f), style = Stroke(26f))
            drawIntoCanvas { Painter.text(it.nativeCanvas, "Almost full!", w / 2, h * 0.68f, h * 0.07f, 0xFFFF4D5E.toInt(), android.graphics.Color.WHITE, 8f) }
        }
    }
}

private fun demoSort(d: DrawScope, t: Float) = with(d) {
    val w = size.width; val h = size.height
    val cupW = w * 0.24f; val cupH = h * 0.13f
    val cupY = h * 0.6f
    val cycle = 3f
    val k = t % cycle
    val colors = intArrayOf(2, 6, 1)
    val target = 1
    for (i in 0 until 3) {
        val cx = w * (0.22f + i * 0.28f)
        var y = cupY
        var s = 1f
        if (i == target && k > 2.1f) { val p = (k - 2.1f) / 0.6f; s = 1f + 0.3f * sin(min(1f, p) * PI.toFloat() / 2); y -= p * 40f }
        if (i == target && k > 2.7f) continue
        drawIntoCanvas { cv ->
            val c = cv.nativeCanvas
            c.save(); c.scale(s, s, cx, y + cupH / 2)
            Painter.cup(c, cx - cupW / 2, y, cupW, cupH, colors[i], false)
            c.restore()
        }
        drawIntoCanvas { Painter.cup(it.nativeCanvas, cx - cupW / 2, cupY + cupH * 1.15f, cupW, cupH, colors[(i + 1) % 3], false) }
        if (i == target) {
            val landed = ((k - 0.3f) / 0.6f).toInt().coerceIn(0, 3)
            for (m in 0 until landed) {
                if (k > 2.7f) break
                ball(this, Offset(cx + (m - 1) * cupW * 0.28f, y + cupH * 0.3f - 4f), cupH * 0.2f, colors[i])
            }
            val falling = ((k - 0.3f) % 0.6f) / 0.6f
            val idx = ((k - 0.3f) / 0.6f).toInt()
            if (k > 0.3f && idx < 3) {
                val tx = cx + (idx - 1) * cupW * 0.28f
                ball(this, Offset(tx, h * 0.15f + falling * falling * (y - h * 0.15f)), cupH * 0.2f, colors[i])
            }
            if (k in 2.1f..2.9f) {
                val p = (k - 2.1f) / 0.8f
                for (sIdx in 0 until 8) {
                    val a = sIdx * PI.toFloat() / 4
                    val rr = p * w * 0.22f
                    drawIntoCanvas { Painter.star4(it.nativeCanvas, cx + cos(a) * rr, y + cupH / 2 + sin(a) * rr, 14f * (1 - p) + 4f, android.graphics.Color.argb(((1 - p) * 255).toInt(), 255, 210, 63)) }
                }
            }
        }
    }
    drawIntoCanvas { Painter.text(it.nativeCanvas, "3 = clear!", w / 2, h * 0.92f, h * 0.07f, 0xFF7B4DFF.toInt(), android.graphics.Color.WHITE, 8f) }
}
