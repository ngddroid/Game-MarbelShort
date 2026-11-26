package com.ngstudio.marblepop.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ngstudio.marblepop.R

/** Colour tokens of the "Candy Glass Factory" look (see UI_GRAPHICS_PLAN.md §1.1). */
object GameColors {
    val Ink = Color(0xFF2A1463)
    val InkSoft = Color(0xFF5B4A8E)
    val Night = Color(0xFF1A0B4A)
    val Purple900 = Color(0xFF33178F)
    val Purple800 = Color(0xFF4521C2)
    val Purple700 = Color(0xFF5A33D6)
    val Purple600 = Color(0xFF6C44F0)
    val Purple500 = Color(0xFF7C55FF)
    val Purple400 = Color(0xFF9775FF)
    val Purple300 = Color(0xFFB9A3FF)
    val Purple200 = Color(0xFFD5C8FF)
    val Purple100 = Color(0xFFEEE8FF)
    val Pink = Color(0xFFFF5FA2)
    val PinkDark = Color(0xFFC42A6E)
    val Yellow = Color(0xFFFFC93C)
    val YellowLight = Color(0xFFFFE27A)
    val YellowDark = Color(0xFFD27A00)
    val Green = Color(0xFF34C759)
    val GreenLight = Color(0xFF8EF08A)
    val GreenDark = Color(0xFF198A3A)
    val Cyan = Color(0xFF33D6F5)
    val CyanDark = Color(0xFF1373BE)
    val Red = Color(0xFFFF4D5E)
    val RedDark = Color(0xFFC0283A)
    val Orange = Color(0xFFFF9F1C)
    val White = Color(0xFFFFFFFF)
    val Card = Color(0xFFFFFFFF)
    val CardSoft = Color(0xFFF4EFFF)
    val Gray = Color(0xFFB8B3CC)
    val GrayDark = Color(0xFF77718F)

    // panel frame + interior
    val RimTop = Color(0xFF8A63FF)
    val RimBottom = Color(0xFF4521C2)
    val FillTop = Color(0xFFFFFFFF)
    val FillBottom = Color(0xFFF2ECFF)

    // glass
    val Glass = Color(0x29FFFFFF)
    val GlassRim = Color(0x59FFFFFF)
    val GlassDark = Color(0x8C24105F)

    val MenuBackground = Brush.verticalGradient(listOf(Color(0xFF5A3BEF), Color(0xFF8A52F5), Color(0xFFE77BD8)))
}

/** Gradient fills for candy text and highlights. */
object Grad {
    val Gold = Brush.verticalGradient(listOf(Color(0xFFFFF6B0), Color(0xFFFFD23F), Color(0xFFFFA81A)))
    val Pink = Brush.verticalGradient(listOf(Color(0xFFFFD0EA), Color(0xFFFF7BBE), Color(0xFFFF3D8E)))
    val Aqua = Brush.verticalGradient(listOf(Color(0xFFD9FAFF), Color(0xFF6BE0FF), Color(0xFF1FA8EE)))
    val Mint = Brush.verticalGradient(listOf(Color(0xFFE6FFD6), Color(0xFF8EF08A), Color(0xFF2FBF4F)))
    val Snow = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1EAFF), Color(0xFFD9CCFF)))
    val Coral = Brush.verticalGradient(listOf(Color(0xFFFFE0D6), Color(0xFFFF9A8A), Color(0xFFFF4D5E)))
    val PanelRim = Brush.verticalGradient(listOf(GameColors.RimTop, GameColors.RimBottom))
    val PanelFill = Brush.verticalGradient(listOf(GameColors.FillTop, GameColors.FillBottom))
}

/** A sky backdrop: three gradient stops, two coloured glows and the skyline silhouette colour. */
@Immutable
data class SkyPalette(
    val top: Color, val mid: Color, val bottom: Color,
    val glowA: Color, val glowB: Color,
    val silhouette: Color,
) {
    fun lerpTo(o: SkyPalette, t: Float) = SkyPalette(
        lerp(top, o.top, t), lerp(mid, o.mid, t), lerp(bottom, o.bottom, t),
        lerp(glowA, o.glowA, t), lerp(glowB, o.glowB, t), lerp(silhouette, o.silhouette, t),
    )
}

object Skies {
    val Menu = SkyPalette(Color(0xFF5A3BEF), Color(0xFF8A52F5), Color(0xFFE77BD8), Color(0xFFFF9AD5), Color(0xFF6FD3FF), Color(0xFF3B1FA6))
    private val worlds: List<SkyPalette> = com.ngstudio.marblepop.render.Themes.all.map { t ->
        val k = t.sky
        SkyPalette(Color(k[0]), Color(k[1]), Color(k[2]), Color(k[3]), Color(k[4]), Color(k[5]))
    }
    /** Sky for a world/theme index (same order and colours as render.Themes). */
    fun forWorld(i: Int) = worlds[i.coerceIn(0, worlds.size - 1)]
}

/** Motion tokens (see UI_GRAPHICS_PLAN.md §1.4). */
object Motion {
    const val MICRO = 120
    const val FAST = 200
    const val STANDARD = 300
    const val EMPHASIZED = 420
    val EaseIn = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)   // decelerate, for entering elements
    val EaseOut = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)  // accelerate, for leaving elements
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.5f, stiffness = 450f)
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.75f, stiffness = 700f)
    fun <T> gentle(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = 220f)
}

val LilitaOne = FontFamily(Font(R.font.lilita_one, FontWeight.Normal))

@OptIn(ExperimentalTextApi::class)
val Fredoka = FontFamily(
    Font(R.font.fredoka, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.fredoka, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.fredoka, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.fredoka, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(450))),
)

object GameText {
    val Title = TextStyle(fontFamily = LilitaOne, fontSize = 34.sp, color = GameColors.White)
    val Heading = TextStyle(fontFamily = LilitaOne, fontSize = 22.sp, color = GameColors.Ink)
    val Button = TextStyle(fontFamily = LilitaOne, fontSize = 22.sp, color = GameColors.White)
    val Body = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = GameColors.Ink, lineHeight = 21.sp)
    val Small = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GameColors.InkSoft, lineHeight = 17.sp)
    val Caption = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = GameColors.InkSoft)
}

@Composable
fun MarbleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = GameColors.Purple500,
            secondary = GameColors.Pink,
            background = GameColors.Purple600,
            surface = GameColors.Card,
            onPrimary = GameColors.White,
            onSurface = GameColors.Ink,
        ),
        content = content,
    )
}
