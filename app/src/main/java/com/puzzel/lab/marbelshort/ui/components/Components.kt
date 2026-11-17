package com.puzzel.lab.marbelshort.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.marbelshort.core.GameRepository
import com.puzzel.lab.marbelshort.core.Sfx
import com.puzzel.lab.marbelshort.core.SoundManager
import com.puzzel.lab.marbelshort.engine.Haptic
import com.puzzel.lab.marbelshort.ui.theme.Fredoka
import com.puzzel.lab.marbelshort.ui.theme.GameColors
import com.puzzel.lab.marbelshort.ui.theme.Grad
import com.puzzel.lab.marbelshort.ui.theme.LilitaOne
import com.puzzel.lab.marbelshort.ui.theme.Motion
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

val LocalSound = staticCompositionLocalOf<SoundManager?> { null }
val LocalRepo = staticCompositionLocalOf<GameRepository> { error("no repo") }

/** Jelly colours: gradient top/bottom, the darker "side" (depth) and the outline. */
enum class BtnColor(val top: Color, val bottom: Color, val depth: Color, val outline: Color) {
    Green(Color(0xFF9DF58A), Color(0xFF2FBF4F), Color(0xFF198A3A), Color(0xFF0E5A26)),
    Yellow(Color(0xFFFFEB85), Color(0xFFFFB21A), Color(0xFFD27A00), Color(0xFF8A4A00)),
    Pink(Color(0xFFFFA8D4), Color(0xFFFF4C97), Color(0xFFC42A6E), Color(0xFF7E1546)),
    Purple(Color(0xFFC8ADFF), Color(0xFF8150FF), Color(0xFF5428C9), Color(0xFF331589)),
    Blue(Color(0xFF9BE9FF), Color(0xFF26ACF2), Color(0xFF1373BE), Color(0xFF0A4A80)),
    Red(Color(0xFFFFA3A3), Color(0xFFFF4D5E), Color(0xFFC0283A), Color(0xFF7D1222)),
    Orange(Color(0xFFFFCB85), Color(0xFFFF8A1F), Color(0xFFCF5E00), Color(0xFF853A00)),
    Gray(Color(0xFFE6E3EF), Color(0xFFABA6C2), Color(0xFF77718F), Color(0xFF4A4562)),
}

private val NoRipple = MutableInteractionSource()

private val GlossTextBrush = Brush.verticalGradient(
    0f to Color.White.copy(alpha = 0.62f), 0.46f to Color.White.copy(alpha = 0.12f), 0.5f to Color.Transparent, 1f to Color.Transparent,
)

/**
 * Candy text: soft drop-shadow stroke, thick outline, flat or gradient fill and an optional gloss layer.
 * Outline width scales with the font size (density independent).
 */
@Composable
fun OutlinedText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    color: Color = Color.White,
    outline: Color = GameColors.Ink,
    outlineWidth: Float = 0f,
    align: TextAlign = TextAlign.Center,
    shadow: Boolean = true,
    brush: Brush? = null,
    gloss: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    val px = with(LocalDensity.current) { fontSize.toPx() }
    val ow = if (outlineWidth > 0f) outlineWidth else px * 0.17f
    val base = TextStyle(fontFamily = LilitaOne, fontSize = fontSize, textAlign = align, lineHeight = fontSize * 1.05f)
    val stroke = Stroke(ow, join = StrokeJoin.Round)
    val overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis
    Box(modifier) {
        if (shadow) Text(text, style = base.copy(color = outline.copy(alpha = 0.5f), drawStyle = stroke), maxLines = maxLines, overflow = overflow,
            modifier = Modifier.offset { IntOffset(0, (px * 0.09f).roundToInt()) })
        Text(text, style = base.copy(color = outline, drawStyle = stroke), maxLines = maxLines, overflow = overflow)
        if (brush != null) Text(text, style = base.merge(TextStyle(brush = brush)), maxLines = maxLines, overflow = overflow)
        else Text(text, style = base.copy(color = color), maxLines = maxLines, overflow = overflow)
        if (gloss) Text(text, style = base.merge(TextStyle(brush = GlossTextBrush)), maxLines = maxLines, overflow = overflow)
    }
}

/** Sweep value for a periodic light streak: 0..1 while sweeping, >1 while resting. */
@Composable
fun rememberShine(periodMs: Int = 3000, sweepMs: Int = 700): State<Float> =
    rememberInfiniteTransition(label = "shine").animateFloat(
        0f, periodMs / sweepMs.toFloat(), infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "s",
    )

/**
 * Chunky jelly button: soft shadow, depth side, gradient body, inner shade, gloss, outline,
 * an optional light streak ([shine]) and a springy press that sinks the body into its side.
 */
@Composable
fun GameButton(
    text: String,
    modifier: Modifier = Modifier,
    color: BtnColor = BtnColor.Green,
    height: Dp = 62.dp,
    fontSize: TextUnit = 26.sp,
    enabled: Boolean = true,
    icon: Ico? = null,
    corner: Dp = 20.dp,
    sound: Sfx = Sfx.BUTTON,
    shine: Boolean = false,
    iconSize: Dp = Dp.Unspecified,
    onClick: () -> Unit,
) {
    val snd = LocalSound.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press = animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 1100f), label = "press")
    val col = if (enabled) color else BtnColor.Gray
    val shineT = if (shine && enabled) rememberShine() else null
    val depthPx = with(LocalDensity.current) { height.toPx() * 0.11f }
    Box(
        modifier
            .height(height)
            .graphicsLayer { val s = 1f - press.value * 0.035f; scaleX = s; scaleY = s }
            .clickable(src, indication = null, enabled = enabled) { snd?.play(sound); snd?.haptic(Haptic.LIGHT); onClick() }
            .drawWithCache {
                val w = size.width
                val h = size.height
                val d = depthPx
                val bodyH = h - d
                val r = corner.toPx().coerceAtMost(bodyH / 2)
                val cr = CornerRadius(r)
                val body = Path().apply { addRoundRect(RoundRect(0f, 0f, w, bodyH, cr)) }
                val bodyBrush = Brush.verticalGradient(listOf(col.top, col.bottom), 0f, bodyH)
                val shade = Brush.verticalGradient(0.5f to Color.Transparent, 1f to col.depth.copy(alpha = 0.45f), startY = 0f, endY = bodyH)
                val glossTop = bodyH * 0.07f
                val glossH = bodyH * 0.38f
                val glossBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.62f), Color.White.copy(alpha = 0.08f)), glossTop, glossTop + glossH)
                val bandW = w * 0.14f + bodyH * 0.2f
                val band = Path().apply {
                    moveTo(bodyH * 0.55f, -2f); lineTo(bodyH * 0.55f + bandW, -2f)
                    lineTo(bandW, bodyH + 2f); lineTo(0f, bodyH + 2f); close()
                }
                val ow = 2.2.dp.toPx()
                val sh = 3.dp.toPx()
                onDrawBehind {
                    val py = press.value * d * 0.8f
                    drawRoundRect(Color(0x38140A3C), Offset(0f, d + sh), Size(w, bodyH), cr)
                    drawRoundRect(col.depth, Offset(0f, d), Size(w, bodyH), cr)
                    translate(0f, py) {
                        drawPath(body, bodyBrush)
                        drawPath(body, shade)
                        drawRoundRect(glossBrush, Offset(w * 0.05f, glossTop), Size(w * 0.9f, glossH), CornerRadius(min(r * 0.8f, glossH / 2)))
                        if (shineT != null) {
                            val p = shineT.value
                            if (p <= 1f) clipPath(body) {
                                translate(-bandW - bodyH * 0.6f + p * (w + bandW * 2 + bodyH), 0f) { drawPath(band, Color.White.copy(alpha = 0.4f)) }
                            }
                        }
                    }
                    drawRoundRect(col.outline, Offset(ow / 2, py + ow / 2), Size(w - ow, h - py - ow), CornerRadius((r - ow / 2).coerceAtLeast(0f)), style = Stroke(ow))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .offset { IntOffset(0, (-depthPx / 2 + press.value * depthPx * 0.8f).roundToInt()) }
                .then(if (text.isNotEmpty()) Modifier.padding(horizontal = 14.dp) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                GameIcon(icon, Modifier.size(if (iconSize != Dp.Unspecified) iconSize else height * 0.5f))
                if (text.isNotEmpty()) Spacer(Modifier.width(7.dp))
            }
            if (text.isNotEmpty()) OutlinedText(text, fontSize = fontSize, outline = col.outline, maxLines = 1)
        }
    }
}

@Composable
fun IconRoundButton(icon: Ico, modifier: Modifier = Modifier, size: Dp = 48.dp, color: BtnColor = BtnColor.Purple, iconScale: Float = 0.6f, onClick: () -> Unit) {
    GameButton("", modifier.size(size), color = color, height = size, corner = size * 0.36f, icon = icon, iconSize = size * iconScale, onClick = onClick)
}

/** Dark glass capsule with an overlapping icon; the icon bumps whenever the value changes. */
@Composable
fun ResourcePill(icon: Ico, value: String, modifier: Modifier = Modifier, onPlus: (() -> Unit)? = null) {
    val bump = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (first) first = false
        else { bump.snapTo(1.32f); bump.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 520f)) }
    }
    Box(modifier.height(44.dp)) {
        Row(
            Modifier
                .padding(start = 16.dp, top = 5.dp, bottom = 5.dp)
                .drawWithCache {
                    val r = CornerRadius(size.height / 2)
                    val fill = Brush.verticalGradient(listOf(Color(0xD9180A4C), Color(0xD93A1C9E)))
                    val inner = Brush.verticalGradient(listOf(Color(0x55000000), Color.Transparent), 0f, size.height * 0.45f)
                    val rim = 1.2.dp.toPx()
                    val ow = 1.8.dp.toPx()
                    onDrawBehind {
                        drawRoundRect(Color.White.copy(alpha = 0.3f), Offset(-rim, -rim), Size(size.width + rim * 2, size.height + rim * 2), CornerRadius(r.x + rim), style = Stroke(rim))
                        drawRoundRect(fill, cornerRadius = r)
                        drawRoundRect(inner, cornerRadius = r)
                        drawRoundRect(Color.White.copy(alpha = 0.14f), Offset(size.height * 0.5f, size.height * 0.74f), Size(size.width - size.height, size.height * 0.1f), CornerRadius(size.height * 0.05f))
                        drawRoundRect(Color(0xFF12073A), Offset(ow / 2, ow / 2), Size(size.width - ow, size.height - ow), CornerRadius(r.x - ow / 2), style = Stroke(ow))
                    }
                }
                .padding(start = 26.dp, end = if (onPlus != null) 36.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedText(value, fontSize = 19.sp, modifier = Modifier.padding(vertical = 2.dp), maxLines = 1)
        }
        GameIcon(icon, Modifier.size(40.dp).align(Alignment.CenterStart).graphicsLayer { scaleX = bump.value; scaleY = bump.value })
        if (onPlus != null) {
            IconRoundButton(Ico.PLUS, Modifier.align(Alignment.CenterEnd).offset(x = 2.dp), size = 34.dp, color = BtnColor.Green, iconScale = 0.5f, onClick = onPlus)
        }
    }
}

/** Ribbon banner with folded tails, used as the title of popups. */
@Composable
fun Ribbon(title: String, color: BtnColor, modifier: Modifier = Modifier, fontSize: TextUnit = 30.sp, minWidth: Dp = 220.dp, height: Dp = 58.dp) {
    Box(
        modifier
            .height(height)
            .widthIn(min = minWidth)
            .drawWithCache {
                val w = size.width
                val d = size.height * 0.1f
                val bandH = size.height - d
                val tail = size.height * 0.41f
                val drop = size.height * 0.2f
                val tailH = bandH * 0.78f
                val r = CornerRadius(size.height * 0.28f)
                fun tailPath(left: Boolean) = Path().apply {
                    val s = if (left) -1f else 1f
                    val x0 = if (left) tail * 0.7f else w - tail * 0.7f
                    val xo = if (left) -tail else w + tail
                    moveTo(x0, drop); lineTo(xo, drop); lineTo(xo + s * -tail * 0.45f, drop + tailH / 2); lineTo(xo, drop + tailH); lineTo(x0, drop + tailH); close()
                }
                val lt = tailPath(true)
                val rt = tailPath(false)
                val fold = Path().apply {
                    moveTo(tail * 0.7f, drop + tailH); lineTo(tail * 0.7f + tail * 0.6f, bandH + d); lineTo(tail * 0.7f + tail * 0.6f, drop + tailH); close()
                    moveTo(w - tail * 0.7f, drop + tailH); lineTo(w - tail * 0.7f - tail * 0.6f, bandH + d); lineTo(w - tail * 0.7f - tail * 0.6f, drop + tailH); close()
                }
                val bandBrush = Brush.verticalGradient(listOf(color.top, color.bottom), 0f, bandH)
                val tailBrush = Brush.verticalGradient(listOf(color.bottom, color.depth), drop, drop + tailH)
                val gloss = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.05f)), bandH * 0.08f, bandH * 0.48f)
                val ow = 2.2.dp.toPx()
                onDrawBehind {
                    drawPath(lt, tailBrush); drawPath(rt, tailBrush)
                    drawPath(lt, color.outline, style = Stroke(ow, join = StrokeJoin.Round))
                    drawPath(rt, color.outline, style = Stroke(ow, join = StrokeJoin.Round))
                    drawPath(fold, color.outline)
                    drawRoundRect(Color(0x40140A3C), Offset(0f, d + 3.dp.toPx()), Size(w, bandH), r)
                    drawRoundRect(color.depth, Offset(0f, d), Size(w, bandH), r)
                    drawRoundRect(bandBrush, Offset.Zero, Size(w, bandH), r)
                    drawRoundRect(gloss, Offset(w * 0.04f, bandH * 0.08f), Size(w * 0.92f, bandH * 0.4f), CornerRadius(r.x * 0.7f))
                    drawRoundRect(color.outline, Offset(ow / 2, ow / 2), Size(w - ow, size.height - ow), CornerRadius(r.x - ow / 2), style = Stroke(ow))
                }
            },
    ) {
        Box(Modifier.align(Alignment.TopCenter).height(height * 0.9f).padding(horizontal = height * 0.45f), contentAlignment = Alignment.Center) {
            OutlinedText(title, fontSize = fontSize, outline = color.outline, gloss = true, maxLines = 1)
        }
    }
}

/** Popup card: gradient rim frame, cream interior with a faint dot pattern, ribbon title, optional sun rays behind. */
@Composable
fun GamePanel(
    title: String,
    modifier: Modifier = Modifier,
    ribbon: BtnColor = BtnColor.Purple,
    onClose: (() -> Unit)? = null,
    rays: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.widthIn(max = 380.dp)) {
        if (rays) SunRays(Modifier.matchParentSize(), color = Color(0xFFFFF3B0), alpha = 0.28f, radiusScale = 0.95f)
        Column(
            Modifier
                .padding(top = 30.dp)
                .fillMaxWidth()
                .drawBehind {
                    val r = CornerRadius(34.dp.toPx())
                    drawRoundRect(Color(0x4D0D0530), Offset(0f, 10.dp.toPx()), size, r)
                    drawRoundRect(Color(0x260D0530), Offset(-4.dp.toPx(), 4.dp.toPx()), Size(size.width + 8.dp.toPx(), size.height + 12.dp.toPx()), CornerRadius(r.x + 4.dp.toPx()))
                }
                .background(Grad.PanelRim, RoundedCornerShape(34.dp))
                .padding(start = 5.dp, end = 5.dp, top = 5.dp, bottom = 9.dp)
                .clip(RoundedCornerShape(29.dp))
                .drawWithCache {
                    val dots = Path()
                    val step = 18.dp.toPx()
                    val dr = 1.7.dp.toPx()
                    var y = step / 2
                    var row = 0
                    while (y < size.height) {
                        var x = if (row % 2 == 0) step / 2 else step
                        while (x < size.width) { dots.addOval(androidx.compose.ui.geometry.Rect(Offset(x, y), dr)); x += step }
                        y += step; row++
                    }
                    val bottomShade = Brush.verticalGradient(listOf(Color.Transparent, Color(0x1A4521C2)), size.height * 0.7f, size.height)
                    onDrawBehind {
                        drawRect(Grad.PanelFill)
                        drawPath(dots, Color(0x0F4521C2))
                        drawRect(bottomShade)
                        drawRoundRect(Color.White, Offset(2.dp.toPx(), 2.dp.toPx()), Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), CornerRadius(27.dp.toPx()), style = Stroke(2.dp.toPx()))
                    }
                }
                .padding(start = 18.dp, end = 18.dp, top = 44.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
        Ribbon(title, ribbon, Modifier.align(Alignment.TopCenter).padding(horizontal = 40.dp), minWidth = 190.dp)
        if (onClose != null) {
            IconRoundButton(Ico.CLOSE, Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = 18.dp), size = 46.dp, color = BtnColor.Red, iconScale = 0.46f, onClick = onClose)
        }
    }
}

/** Keeps the last non-null value so a closing popup can still render its content while animating out. */
@Composable
fun <T : Any> rememberLatest(value: T?): T? {
    val holder = remember { arrayOfNulls<Any>(1) }
    if (value != null) holder[0] = value
    @Suppress("UNCHECKED_CAST")
    return holder[0] as T?
}

/**
 * Dimmed overlay hosting a popup. Animates in (dim fade + springy pop) and, when [visible] turns
 * false, animates out before leaving the composition.
 */
@Composable
fun PopupHost(visible: Boolean = true, onDismiss: (() -> Unit)? = null, dim: Float = 0.66f, content: @Composable BoxScope.() -> Unit) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState && state.isIdle) return
    val transition = rememberTransition(state, label = "popup")
    val p = transition.animateFloat(
        transitionSpec = { if (targetState) spring(dampingRatio = 0.6f, stiffness = 430f) else tween(170, easing = Motion.EaseOut) },
        label = "pop",
    ) { if (it) 1f else 0f }
    val dimA = transition.animateFloat(transitionSpec = { tween(if (targetState) 220 else 190) }, label = "dim") { if (it) 1f else 0f }
    val snd = LocalSound.current
    LaunchedEffect(visible) { if (visible) snd?.play(Sfx.WHOOSH, 1.1f, 0.6f) }
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val a = dim * dimA.value
                drawRect(Color(0xFF12082E).copy(alpha = a))
                drawRect(Brush.radialGradient(listOf(Color(0x00000000), Color(0xFF0A0420).copy(alpha = a * 0.5f)), center, size.maxDimension * 0.75f))
            }
            .clickable(NoRipple, indication = null, enabled = visible) { onDismiss?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .padding(20.dp)
                .graphicsLayer {
                    val v = p.value
                    val entering = state.targetState
                    val s = if (entering) 0.72f + 0.28f * v else 0.88f + 0.12f * v
                    scaleX = s; scaleY = s
                    alpha = v.coerceIn(0f, 1f)
                    translationY = if (entering) (1f - v) * 36.dp.toPx() else 0f
                }
                .clickable(NoRipple, indication = null) {},
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
fun StarRow(stars: Int, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((-2).dp)) {
        for (i in 0 until 3) GameIcon(if (i < stars) Ico.STAR else Ico.STAR_EMPTY, Modifier.size(size).offset(y = if (i == 1) (-3).dp else 0.dp))
    }
}

/** Candy switch with ON/OFF label and a marble knob. */
@Composable
fun GameToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val snd = LocalSound.current
    val pos = animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 520f), label = "tg")
    val labelStyle = TextStyle(fontFamily = LilitaOne, fontSize = 13.sp, color = Color.White)
    Box(
        Modifier
            .size(78.dp, 40.dp)
            .clip(RoundedCornerShape(50))
            .clickable(NoRipple, indication = null) { snd?.play(Sfx.BUTTON); snd?.haptic(Haptic.LIGHT); onChange(!checked) }
            .drawBehind {
                val p = pos.value
                val rr = CornerRadius(size.height / 2)
                val d = 3.dp.toPx()
                drawRoundRect(lerp(Color(0xFF6E6889), Color(0xFF167A33), p), cornerRadius = rr)
                val top = lerp(Color(0xFFD3CFE2), Color(0xFF7BE37F), p)
                val bot = lerp(Color(0xFFA7A1BE), Color(0xFF2FBF4F), p)
                val inH = size.height - d - 2.dp.toPx()
                drawRoundRect(Brush.verticalGradient(listOf(bot, top), 0f, inH), Offset(1.5.dp.toPx(), 1.5.dp.toPx()), Size(size.width - 3.dp.toPx(), inH), CornerRadius(inH / 2))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0x40000000), Color.Transparent), 0f, inH * 0.5f), Offset(1.5.dp.toPx(), 1.5.dp.toPx()), Size(size.width - 3.dp.toPx(), inH), CornerRadius(inH / 2))
                val r = size.height * 0.4f
                val cx = r + 3.dp.toPx() + (size.width - 2 * r - 6.dp.toPx()) * p
                ball(this, Offset(cx, size.height / 2 - 1.5.dp.toPx()), r, if (checked) 3 else 8)
            },
    ) {
        Text("ON", style = labelStyle, modifier = Modifier.align(Alignment.CenterStart).padding(start = 11.dp, bottom = 3.dp).graphicsLayer { alpha = pos.value })
        Text("OFF", style = labelStyle, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 9.dp, bottom = 3.dp).graphicsLayer { alpha = 1f - pos.value })
    }
}

/** Header used by full screen sub pages. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp).height(58.dp)) {
        IconRoundButton(Ico.BACK, Modifier.align(Alignment.CenterStart), size = 50.dp, color = BtnColor.Yellow, iconScale = 0.5f, onClick = onBack)
        OutlinedText(title, Modifier.align(Alignment.Center), fontSize = 32.sp, brush = Grad.Snow, gloss = true, maxLines = 1)
    }
}

/** Gentle breathing pulse for call-to-action elements (draw-phase only, no recomposition). */
@Composable
fun Modifier.idleBounce(enabled: Boolean = true, amount: Float = 0.05f, periodMs: Int = 1400): Modifier {
    if (!enabled) return this
    val t = rememberInfiniteTransition(label = "bounce").animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "s")
    return this.graphicsLayer {
        val k = 0.5f + 0.5f * sin(t.value * 2f * PI.toFloat())
        val s = 1f + amount * k
        scaleX = s; scaleY = s
    }
}

/** Slow floating bob (translationY), phase-shifted so neighbours don't move in lockstep. */
@Composable
fun Modifier.floaty(amplitude: Dp = 3.dp, periodMs: Int = 2600, phase: Float = 0f): Modifier {
    val t = rememberInfiniteTransition(label = "float").animateFloat(0f, 1f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "f")
    return this.graphicsLayer { translationY = sin((t.value + phase) * 2f * PI.toFloat()) * amplitude.toPx() }
}

/** Candy card: gradient rim with a 3D bottom edge, cream interior with a soft bottom shade. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, padding: Dp = 14.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .drawBehind {
                drawRoundRect(Color(0x400D0530), Offset(0f, 6.dp.toPx()), size, CornerRadius(26.dp.toPx()))
            }
            .background(Grad.PanelRim, RoundedCornerShape(26.dp))
            .padding(start = 3.dp, end = 3.dp, top = 3.dp, bottom = 6.dp)
            .drawWithCache {
                val r = CornerRadius(23.dp.toPx())
                val shade = Brush.verticalGradient(listOf(Color.Transparent, Color(0x144521C2)), size.height * 0.6f, size.height)
                onDrawBehind {
                    drawRoundRect(Grad.PanelFill, cornerRadius = r)
                    drawRoundRect(shade, cornerRadius = r)
                }
            }
            .padding(padding),
        content = content,
    )
}

/** Gradient rounded-square tile holding an icon. */
@Composable
fun IconTile(icon: Ico, color: BtnColor, modifier: Modifier = Modifier, size: Dp = 44.dp, iconScale: Float = 0.64f) {
    Box(
        modifier
            .size(size)
            .drawWithCache {
                val w = this.size.width
                val h = this.size.height
                val d = 3.dp.toPx()
                val r = CornerRadius(w * 0.3f)
                val body = Brush.verticalGradient(listOf(color.top, color.bottom), 0f, h - d)
                val gloss = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.05f)), 0f, (h - d) * 0.46f)
                val ow = 1.6.dp.toPx()
                onDrawBehind {
                    drawRoundRect(color.depth, Offset(0f, d), Size(w, h - d), r)
                    drawRoundRect(body, Offset.Zero, Size(w, h - d), r)
                    drawRoundRect(gloss, Offset(w * 0.08f, h * 0.05f), Size(w * 0.84f, (h - d) * 0.42f), CornerRadius(r.x * 0.7f))
                    drawRoundRect(color.outline, Offset(ow / 2, ow / 2), Size(w - ow, h - ow), CornerRadius(r.x), style = Stroke(ow))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        GameIcon(icon, Modifier.offset(y = (-1.5).dp).size(size * iconScale))
    }
}

/** Small gradient pill label (e.g. "BEST VALUE", rarity, "-17%"). */
@Composable
fun Chip(text: String, color: BtnColor, modifier: Modifier = Modifier, fontSize: TextUnit = 12.sp) {
    Box(
        modifier
            .drawWithCache {
                val r = CornerRadius(size.height / 2)
                val b = Brush.verticalGradient(listOf(color.top, color.bottom))
                val ow = 1.5.dp.toPx()
                onDrawBehind {
                    drawRoundRect(b, cornerRadius = r)
                    drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(size.height * 0.3f, size.height * 0.12f), Size(size.width - size.height * 0.6f, size.height * 0.28f), CornerRadius(size.height * 0.14f))
                    drawRoundRect(color.outline, Offset(ow / 2, ow / 2), Size(size.width - ow, size.height - ow), CornerRadius(r.x - ow / 2), style = Stroke(ow))
                }
            }
            .padding(horizontal = 9.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        OutlinedText(text, fontSize = fontSize, outline = color.outline, maxLines = 1, shadow = false)
    }
}

/** Track with a sliding jelly thumb. */
@Composable
fun SegmentedSwitch(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val pos = animateFloatAsState(selected.toFloat(), Motion.snappy(), label = "seg")
    val snd = LocalSound.current
    val c = BtnColor.Pink
    Box(
        modifier
            .height(54.dp)
            .drawWithCache {
                val rr = CornerRadius(size.height / 2)
                val pad = 4.dp.toPx()
                val tw = (size.width - pad * 2) / options.size
                val th = size.height - pad * 2
                val d = 4.dp.toPx()
                val tr = CornerRadius((th - d) / 2)
                val thumb = Brush.verticalGradient(listOf(c.top, c.bottom), pad, pad + th - d)
                val gloss = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.05f)), pad, pad + (th - d) * 0.5f)
                onDrawBehind {
                    drawRoundRect(Color(0x73140A3C), cornerRadius = rr)
                    drawRoundRect(Color(0x33000000), Offset(0f, 0f), Size(size.width, size.height * 0.5f), rr)
                    drawRoundRect(Color.White.copy(alpha = 0.28f), cornerRadius = rr, style = Stroke(1.5.dp.toPx()))
                    val x = pad + tw * pos.value
                    drawRoundRect(c.depth, Offset(x, pad + d), Size(tw, th - d), tr)
                    drawRoundRect(thumb, Offset(x, pad), Size(tw, th - d), tr)
                    drawRoundRect(gloss, Offset(x + tw * 0.06f, pad + (th - d) * 0.08f), Size(tw * 0.88f, (th - d) * 0.38f), CornerRadius(tr.x * 0.7f))
                    drawRoundRect(c.outline, Offset(x, pad), Size(tw, th), tr, style = Stroke(1.8.dp.toPx()))
                }
            },
    ) {
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .clickable(NoRipple, indication = null) { if (i != selected) { snd?.play(Sfx.BUTTON); onSelect(i) } },
                    contentAlignment = Alignment.Center,
                ) {
                    OutlinedText(label, Modifier.offset(y = (-1).dp), fontSize = 20.sp,
                        color = if (i == selected) Color.White else Color.White.copy(alpha = 0.72f),
                        outline = if (i == selected) c.outline else GameColors.Ink, maxLines = 1)
                }
            }
        }
    }
}

/** Glass tube progress bar with gloss; optional centred label. */
@Composable
fun ProgressTube(progress: Float, modifier: Modifier = Modifier, height: Dp = 16.dp, fill: BtnColor = BtnColor.Green, label: String? = null) {
    Box(
        modifier
            .height(height)
            .drawWithCache {
                val h = size.height
                val rr = CornerRadius(h / 2)
                val fillBrush = Brush.verticalGradient(listOf(fill.top, fill.bottom))
                val inner = Brush.verticalGradient(listOf(Color(0x40000000), Color.Transparent), 0f, h * 0.55f)
                onDrawBehind {
                    drawRoundRect(Color(0x2E2A1463), cornerRadius = rr)
                    drawRoundRect(inner, cornerRadius = rr)
                    val p = progress.coerceIn(0f, 1f)
                    if (p > 0f) {
                        val w = (size.width * p).coerceAtLeast(h)
                        drawRoundRect(fill.depth, Offset.Zero, Size(w, h), rr)
                        drawRoundRect(fillBrush, Offset.Zero, Size(w, h - 2.dp.toPx()), rr)
                        drawRoundRect(Color.White.copy(alpha = 0.5f), Offset(h * 0.32f, h * 0.15f), Size((w - h * 0.64f).coerceAtLeast(0f), h * 0.24f), CornerRadius(h * 0.12f))
                    }
                    drawRoundRect(Color(0x592A1463), cornerRadius = rr, style = Stroke(1.4.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) OutlinedText(label, fontSize = (height.value * 0.62f).sp, shadow = false, maxLines = 1)
    }
}

/** Pulsing red notification badge with an optional count. */
@Composable
fun NotifyBadge(modifier: Modifier = Modifier, size: Dp = 16.dp, count: Int? = null) {
    val t = rememberInfiniteTransition(label = "nb").animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "t")
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                val k = sin(t.value * 2f * PI.toFloat()).coerceAtLeast(0f)
                scaleX = 1f + 0.16f * k; scaleY = 1f + 0.16f * k
            }
            .drawBehind {
                val r = this.size.minDimension / 2
                drawCircle(Color.White, r)
                drawCircle(Brush.verticalGradient(listOf(Color(0xFFFF8A8A), Color(0xFFE8273D))), r - 2.dp.toPx())
                drawCircle(Color.White.copy(alpha = 0.55f), r * 0.28f, Offset(r * 0.72f, r * 0.68f))
            },
        contentAlignment = Alignment.Center,
    ) {
        if (count != null) OutlinedText(count.toString(), fontSize = (size.value * 0.55f).sp, shadow = false, outline = Color(0xFF8A1020))
    }
}

/** Section heading on the sky: glossy white candy text with optional icon and trailing content. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, icon: Ico? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp, start = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { GameIcon(icon, Modifier.size(30.dp)); Spacer(Modifier.width(6.dp)) }
        OutlinedText(text, fontSize = 24.sp, brush = Grad.Snow, maxLines = 1)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

val CardTitleStyle = TextStyle(fontFamily = LilitaOne, fontSize = 21.sp, color = GameColors.Ink)
val CardBodyStyle = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GameColors.InkSoft, lineHeight = 18.sp)

@Composable
fun Dot(color: Color, size: Dp) { Box(Modifier.size(size).background(color, CircleShape)) }
