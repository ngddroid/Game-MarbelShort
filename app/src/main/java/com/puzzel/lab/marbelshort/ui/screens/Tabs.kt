package com.puzzel.lab.marbelshort.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.marbelshort.core.AdPlacement
import com.puzzel.lab.marbelshort.core.Booster
import com.puzzel.lab.marbelshort.core.Catalog
import com.puzzel.lab.marbelshort.core.GameRepository
import com.puzzel.lab.marbelshort.core.Sfx
import com.puzzel.lab.marbelshort.core.Stat
import com.puzzel.lab.marbelshort.render.Painter
import com.puzzel.lab.marbelshort.render.Themes
import com.puzzel.lab.marbelshort.ui.Nav
import com.puzzel.lab.marbelshort.ui.Screen
import com.puzzel.lab.marbelshort.ui.components.BtnColor
import com.puzzel.lab.marbelshort.ui.components.CardBodyStyle
import com.puzzel.lab.marbelshort.ui.components.CardTitleStyle
import com.puzzel.lab.marbelshort.ui.components.Chip
import com.puzzel.lab.marbelshort.ui.components.GameButton
import com.puzzel.lab.marbelshort.ui.components.GameIcon
import com.puzzel.lab.marbelshort.ui.components.GamePanel
import com.puzzel.lab.marbelshort.ui.components.Ico
import com.puzzel.lab.marbelshort.ui.components.IconTile
import com.puzzel.lab.marbelshort.ui.components.LocalRepo
import com.puzzel.lab.marbelshort.ui.components.LocalSound
import com.puzzel.lab.marbelshort.ui.components.OutlinedText
import com.puzzel.lab.marbelshort.ui.components.PopupHost
import com.puzzel.lab.marbelshort.ui.components.ProgressTube
import com.puzzel.lab.marbelshort.ui.components.ResourcePill
import com.puzzel.lab.marbelshort.ui.components.SectionCard
import com.puzzel.lab.marbelshort.ui.components.SectionTitle
import com.puzzel.lab.marbelshort.ui.components.SegmentedSwitch
import com.puzzel.lab.marbelshort.ui.components.SunRays
import com.puzzel.lab.marbelshort.ui.components.WatchAdDialog
import com.puzzel.lab.marbelshort.ui.components.ball
import com.puzzel.lab.marbelshort.ui.components.drawGameIcon
import com.puzzel.lab.marbelshort.ui.components.floaty
import com.puzzel.lab.marbelshort.ui.components.idleBounce
import com.puzzel.lab.marbelshort.ui.components.rememberLatest
import com.puzzel.lab.marbelshort.ui.components.rememberTime
import com.puzzel.lab.marbelshort.ui.theme.Fredoka
import com.puzzel.lab.marbelshort.ui.theme.GameColors
import com.puzzel.lab.marbelshort.ui.theme.Grad
import com.puzzel.lab.marbelshort.ui.theme.LilitaOne
import com.puzzel.lab.marbelshort.ui.theme.Motion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import com.puzzel.lab.marbelshort.core.Analytics

private val WhiteBody = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White, lineHeight = 18.sp)

@Composable
private fun TabHeader(title: String) {
    val repo = LocalRepo.current
    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp).height(56.dp)) {
        OutlinedText(title, Modifier.align(Alignment.CenterStart), fontSize = 36.sp, brush = Grad.Snow, gloss = true, maxLines = 1)
        ResourcePill(Ico.COIN, repo.coins.intValue.toString(), Modifier.align(Alignment.CenterEnd))
    }
}

/** Scrolling tab body whose edges fade out under the header and above the dock. */
@Composable
private fun TabBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val h = size.height
                val top = 14.dp.toPx() / h
                val bottom = 1f - 22.dp.toPx() / h
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, top to Color.Black, bottom to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn,
                )
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(2.dp))
        content()
        Spacer(Modifier.height(26.dp))
    }
}

/** Springy "+1 Hammer" toast that floats up and fades. */
@Composable
private fun RewardToast(message: String?, icon: Ico?, onDone: () -> Unit) {
    if (message == null) return
    val a = remember(message) { Animatable(0f) }
    LaunchedEffect(message) {
        a.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f))
        delay(700)
        a.animateTo(2f, tween(380))
        onDone()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .graphicsLayer {
                    val v = a.value
                    val s = if (v <= 1f) v else 1f
                    scaleX = s; scaleY = s
                    alpha = if (v <= 1f) v.coerceIn(0f, 1f) else 2f - v
                    translationY = if (v > 1f) -(v - 1f) * 60.dp.toPx() else 0f
                }
                .background(Brush.verticalGradient(listOf(Color(0xF22A1463), Color(0xF2140A3C))), RoundedCornerShape(50))
                .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) { GameIcon(icon, Modifier.size(38.dp)); Spacer(Modifier.width(8.dp)) }
            OutlinedText(message, fontSize = 26.sp, brush = Grad.Gold, maxLines = 1)
        }
    }
}

/** Gradient feature card (gift hero, daily challenge, bundle). */
private fun Modifier.featureCard(top: Color, bottom: Color, rim: Color): Modifier = this
    .drawWithCache {
        val r = CornerRadius(26.dp.toPx())
        val fill = Brush.verticalGradient(listOf(top, bottom))
        val gloss = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent), 0f, size.height * 0.45f)
        val dots = Path().apply {
            val rnd = Random(5)
            repeat(14) { addOval(androidx.compose.ui.geometry.Rect(Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height), (2f + rnd.nextFloat() * 4f) * density)) }
        }
        onDrawBehind {
            drawRoundRect(Color(0x400D0530), Offset(0f, 6.dp.toPx()), size, r)
            drawRoundRect(rim, cornerRadius = r)
            inset(3.dp.toPx(), 3.dp.toPx(), 3.dp.toPx(), 6.dp.toPx()) {
                drawRoundRect(fill, cornerRadius = CornerRadius(r.x - 3.dp.toPx()))
                drawRoundRect(gloss, cornerRadius = CornerRadius(r.x - 3.dp.toPx()))
                drawPath(dots, Color.White.copy(alpha = 0.16f))
            }
        }
    }
    .padding(start = 3.dp, end = 3.dp, top = 3.dp, bottom = 6.dp)

// ============================================================ SHOP

private val boosterHalo = mapOf(
    Booster.HAMMER to Color(0xFFFFC27A), Booster.MAGNET to Color(0xFFFF9AB0),
    Booster.SLOTS to Color(0xFF8FE3FF), Booster.SHUFFLE to Color(0xFFC6A8FF),
)

@Composable
fun ShopTab() {
    val repo = LocalRepo.current
    val sound = LocalSound.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    var bought by remember { mutableStateOf<Pair<String, Ico?>?>(null) }
    Column(Modifier.fillMaxSize()) {
        TabHeader("Shop")
        TabBody {
            // hero: free coins
            val ready = now >= repo.freeGiftAt.value
            Box(Modifier.fillMaxWidth().featureCard(Color(0xFFFF7BBE), Color(0xFF7C4DFF), Color(0xFF4521C2))) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                        if (ready) SunRays(Modifier.requiredSize(170.dp), color = Color(0xFFFFF3B0), alpha = 0.32f, rays = 12, degPerSec = 20f)
                        GameIcon(Ico.GIFT, Modifier.size(80.dp).floaty(4.dp, 2200))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        OutlinedText("Free Coins", fontSize = 26.sp, brush = Grad.Gold, gloss = true, align = TextAlign.Start, maxLines = 1)
                        Text("100 coins every 4 hours", style = WhiteBody)
                        Spacer(Modifier.height(8.dp))
                        val left = (repo.freeGiftAt.value - now).coerceAtLeast(0) / 1000
                        GameButton(
                            if (ready) "Claim 100" else "%d:%02d:%02d".format(left / 3600, left / 60 % 60, left % 60),
                            Modifier.fillMaxWidth(), color = if (ready) BtnColor.Green else BtnColor.Gray, height = 48.dp,
                            fontSize = if (ready) 21.sp else 18.sp, enabled = ready, shine = ready, icon = if (ready) Ico.COIN else null,
                        ) {
                            if (repo.claimFreeGift(System.currentTimeMillis())) { sound?.play(Sfx.REWARD); bought = "+100" to Ico.COIN }
                        }
                    }
                }
            }
            SectionTitle("Boosters", icon = Ico.HAMMER)
            for (row in Booster.entries.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (b in row) {
                        BoosterTile(b, Modifier.weight(1f)) { count, price ->
                            if (repo.spend(price)) { repo.addBooster(b, count); bought = "+$count ${b.title}" to boosterIcon(b) }
                        }
                    }
                }
            }
            // mega bundle
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().featureCard(Color(0xFFFFE27A), Color(0xFFFF9F1C), Color(0xFFB45C00)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(92.dp)) {
                            SunRays(Modifier.requiredSize(150.dp).align(Alignment.Center), color = Color.White, alpha = 0.3f, rays = 12, degPerSec = 12f)
                            val icons = listOf(Ico.HAMMER, Ico.MAGNET, Ico.SLOTS, Ico.SHUFFLE)
                            val spots = listOf(-20 to -18, 20 to -18, -20 to 20, 20 to 20)
                            icons.forEachIndexed { i, ic ->
                                GameIcon(ic, Modifier.size(42.dp).align(Alignment.Center).offset(x = spots[i].first.dp, y = spots[i].second.dp)
                                    .graphicsLayer { rotationZ = if (i % 2 == 0) -12f else 12f })
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            OutlinedText("Mega Bundle", fontSize = 26.sp, brush = Grad.Snow, gloss = true, align = TextAlign.Start, outline = Color(0xFF7A3A00), maxLines = 1)
                            Text("3 of every booster · save 40%", style = WhiteBody.copy(color = Color(0xFF5A2A00)))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    GameButton("1250", Modifier.fillMaxWidth(), color = BtnColor.Green, height = 52.dp, fontSize = 23.sp, icon = Ico.COIN,
                        enabled = repo.coins.intValue >= 1250, sound = Sfx.COIN, shine = repo.coins.intValue >= 1250) {
                        if (repo.spend(1250)) { Booster.entries.forEach { repo.addBooster(it, 3) }; bought = "Mega Bundle!" to Ico.CHEST }
                    }
                }
                Chip("BEST VALUE", BtnColor.Red, Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = (-9).dp).graphicsLayer { rotationZ = 6f }, fontSize = 14.sp)
            }
            Text(
                "Coins are earned by playing: winning levels, combos, chests, daily rewards and the lucky spin.",
                style = WhiteBody.copy(textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.85f)),
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
        }
    }
    RewardToast(bought?.first, bought?.second) { bought = null }
}

@Composable
private fun BoosterTile(b: Booster, modifier: Modifier, onBuy: (count: Int, price: Int) -> Unit) {
    val repo = LocalRepo.current
    val coins = repo.coins.intValue
    val p3 = b.price * 5 / 2
    SectionCard(modifier, padding = 10.dp) {
        Box(
            Modifier.fillMaxWidth().height(86.dp).clip(RoundedCornerShape(18.dp))
                .background(Brush.radialGradient(listOf(boosterHalo.getValue(b), Color(0xFFF4EFFF)))),
            contentAlignment = Alignment.Center,
        ) {
            GameIcon(boosterIcon(b), Modifier.size(62.dp))
            Chip("x${repo.boosterCount(b)}", BtnColor.Purple, Modifier.align(Alignment.TopEnd).padding(6.dp), fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(b.title, style = CardTitleStyle.copy(fontSize = 19.sp, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth(), maxLines = 1)
        Text(b.description, style = CardBodyStyle.copy(fontSize = 12.sp, lineHeight = 15.sp, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth(), maxLines = 2, minLines = 2)
        Spacer(Modifier.height(8.dp))
        GameButton("${b.price}", Modifier.fillMaxWidth(), color = BtnColor.Yellow, height = 44.dp, fontSize = 19.sp, icon = Ico.COIN, enabled = coins >= b.price, sound = Sfx.COIN) { onBuy(1, b.price) }
        Spacer(Modifier.height(8.dp))
        Box {
            GameButton("x3  $p3", Modifier.fillMaxWidth(), color = BtnColor.Pink, height = 42.dp, fontSize = 17.sp, icon = Ico.COIN, enabled = coins >= p3, sound = Sfx.COIN) { onBuy(3, p3) }
            Chip("-17%", BtnColor.Green, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-10).dp), fontSize = 11.sp)
        }
    }
}

// ============================================================ DAILY

private val wheelPrizes = listOf(
    "50" to 50, "Hammer" to -1, "100" to 100, "Magnet" to -2, "25" to 25, "Slots" to -3, "250" to 250, "Shuffle" to -4,
)
private val wheelColors = listOf(
    Color(0xFFFF5FA2) to Color(0xFFD12D78), Color(0xFFFFD23F) to Color(0xFFFF9F1C), Color(0xFF33D6F5) to Color(0xFF1597C9), Color(0xFF8EF08A) to Color(0xFF2FBF4F),
    Color(0xFFB394FF) to Color(0xFF7B4DFF), Color(0xFFFFB86B) to Color(0xFFFF7A1F), Color(0xFFFF8A8A) to Color(0xFFE8273D), Color(0xFF7FE0F5) to Color(0xFF2A8FD6),
)

@Composable
fun DailyTab(nav: Nav) {
    val repo = LocalRepo.current
    val sound = LocalSound.current
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    var prize by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var loginReward by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabHeader("Daily")
        TabBody {
            SectionCard(Modifier.fillMaxWidth()) {
                CardHeader("Lucky Spin", Ico.WHEEL)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp), contentAlignment = Alignment.Center) {
                    LuckyWheel(rotation, spinning)
                }
                val free = repo.canSpin
                GameButton(
                    if (free) "SPIN FREE" else "SPIN  100", Modifier.fillMaxWidth().idleBounce(free && !spinning, amount = 0.035f),
                    color = if (free) BtnColor.Green else BtnColor.Yellow, icon = if (free) null else Ico.COIN, shine = free && !spinning,
                    enabled = !spinning && (free || repo.coins.intValue >= 100),
                ) {
                    if (!free && !repo.spend(100)) return@GameButton
                    if (!free) Analytics.spend("spin", 100)
                    if (free) repo.markSpin() else repo.addStat(Stat.SPINS, 1)
                    spinning = true
                    val target = Random.nextInt(wheelPrizes.size)
                    val sweep = 360f / wheelPrizes.size
                    val base = rotation.value - rotation.value % 360f
                    val end = base + 360f * 5 + (360f - target * sweep) + (Random.nextFloat() - 0.5f) * sweep * 0.6f
                    scope.launch {
                        var lastTick = rotation.value
                        launch {
                            while (spinning) {
                                if (rotation.value - lastTick > sweep) { lastTick = rotation.value; sound?.play(Sfx.SPIN_TICK) }
                                delay(16)
                            }
                        }
                        rotation.animateTo(end, tween(4200, easing = CubicBezierEasing(0.15f, 0.9f, 0.3f, 1f)))
                        spinning = false
                        val p = wheelPrizes[target]
                        grantPrize(repo, p.second)
                        sound?.play(Sfx.REWARD)
                        prize = p
                    }
                }
            }
            // daily challenge
            Column(Modifier.fillMaxWidth().featureCard(Color(0xFF9B6BFF), Color(0xFF4F2BD6), Color(0xFF2E1689)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Ico.TROPHY, BtnColor.Yellow, size = 64.dp, iconScale = 0.7f)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedText("Daily Challenge", fontSize = 22.sp, brush = Grad.Snow, align = TextAlign.Start, maxLines = 1)
                        }
                        Text(if (repo.dailyChallengeDone) "Completed! Come back tomorrow." else "A special hard puzzle every day.", style = WhiteBody)
                        Spacer(Modifier.height(4.dp))
                        Chip("x2 COINS", BtnColor.Yellow, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                GameButton(if (repo.dailyChallengeDone) "Done" else "Play", Modifier.fillMaxWidth(), color = BtnColor.Green, height = 52.dp,
                    icon = if (repo.dailyChallengeDone) Ico.CHECK else Ico.PLAY, enabled = !repo.dailyChallengeDone, shine = !repo.dailyChallengeDone) {
                    nav.go(Screen.Game(repo.dailyLevel, daily = true))
                }
            }
            // login calendar
            SectionCard(Modifier.fillMaxWidth()) {
                CardHeader("Daily Rewards", Ico.CALENDAR)
                Spacer(Modifier.height(10.dp))
                val idx = repo.loginIndex
                val claimable = repo.canClaimLogin
                for (row in 0 until 2) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val range = if (row == 0) 0 until 4 else 4 until 7
                        for (d in range) {
                            DayTile(d, claimed = d < idx, today = claimable && d == idx, Modifier.weight(if (d == 6) 2.1f else 1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                GameButton(if (claimable) "Claim Day ${idx + 1}" else "Come back tomorrow", Modifier.fillMaxWidth(), color = BtnColor.Green, height = 52.dp,
                    fontSize = if (claimable) 23.sp else 18.sp, enabled = claimable, shine = claimable) {
                    loginReward = repo.claimLogin()
                    sound?.play(Sfx.REWARD)
                }
            }
        }
    }
    val shownPrize = rememberLatest(prize)
    PopupHost(visible = prize != null, onDismiss = { prize = null }) {
        shownPrize?.let { (label, v) ->
            GamePanel("You won!", ribbon = BtnColor.Yellow, rays = true) {
                RewardTile(if (v > 0) Ico.COIN else boosterIcon(Booster.entries[-v - 1]), if (v > 0) "+$label" else "+1 $label")
                Spacer(Modifier.height(14.dp))
                GameButton("Collect", Modifier.fillMaxWidth(), color = BtnColor.Green, shine = true) { prize = null }
            }
        }
    }
    val shownLogin = rememberLatest(loginReward.takeIf { it > 0 })
    PopupHost(visible = loginReward > 0, onDismiss = { loginReward = 0 }) {
        if (shownLogin != null) GamePanel("Daily Reward", ribbon = BtnColor.Green, rays = true) {
            RewardTile(Ico.COIN, "+$shownLogin")
            Spacer(Modifier.height(14.dp))
            GameButton("Great!", Modifier.fillMaxWidth(), color = BtnColor.Green, shine = true) { loginReward = 0 }
        }
    }
}

@Composable
private fun CardHeader(title: String, icon: Ico) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        GameIcon(icon, Modifier.size(30.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = CardTitleStyle.copy(fontSize = 25.sp))
    }
}

/**
 * Lucky wheel: the face (segments, icons, amounts) is baked into a bitmap once and rotated each frame;
 * the gold rim has chasing bulbs and the gem pointer flicks as segments pass under it.
 */
@Composable
private fun LuckyWheel(rotation: Animatable<Float, *>, spinning: Boolean) {
    val time = rememberTime()
    Box(
        Modifier.fillMaxSize().drawWithCache {
            val r = size.minDimension / 2
            val c = Offset(size.width / 2, size.height / 2)
            val inner = r * 0.86f
            val sweep = 360f / wheelPrizes.size
            val faceSize = (inner * 2).toInt().coerceAtLeast(1)
            val face = ImageBitmap(faceSize, faceSize)
            CanvasDrawScope().draw(this, LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(face), Size(faceSize.toFloat(), faceSize.toFloat())) {
                val fc = Offset(inner, inner)
                for (i in wheelPrizes.indices) {
                    val (a, b) = wheelColors[i]
                    val start = -90f + i * sweep - sweep / 2
                    drawArc(Brush.radialGradient(listOf(a, b), fc, inner), start, sweep, true, Offset.Zero, Size(inner * 2, inner * 2))
                    drawArc(Color.White.copy(alpha = 0.16f), start, sweep / 2, true, Offset.Zero, Size(inner * 2, inner * 2))
                }
                for (i in wheelPrizes.indices) {
                    val ang = (-90f + i * sweep - sweep / 2) * PI.toFloat() / 180f
                    drawLine(Color.White.copy(alpha = 0.85f), fc, fc + Offset(cos(ang) * inner, sin(ang) * inner), 3.dp.toPx())
                }
                for (i in wheelPrizes.indices) {
                    val (label, v) = wheelPrizes[i]
                    rotate(i * sweep, fc) {
                        val icon = if (v > 0) Ico.COIN else boosterIcon(Booster.entries[-v - 1])
                        val s = inner * (if (v > 0) 0.24f else 0.3f)
                        val iy = fc.y - inner * (if (v > 0) 0.74f else 0.66f)
                        inset(fc.x - s / 2, iy - s / 2, size.width - (fc.x + s / 2), size.height - (iy + s / 2)) { drawGameIcon(icon) }
                        if (v > 0) drawIntoCanvas { cv ->
                            Painter.text(cv.nativeCanvas, label, fc.x, fc.y - inner * 0.44f, inner * 0.17f, android.graphics.Color.WHITE, 0xFF2A1463.toInt())
                        }
                    }
                }
                drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color(0x40000000)), fc, inner), inner, fc)
            }
            val rim = Brush.verticalGradient(listOf(Color(0xFFFFF0A0), Color(0xFFFFB21A), Color(0xFFC47A00)), c.y - r, c.y + r)
            val hub = Brush.verticalGradient(listOf(Color(0xFFFFF6C8), Color(0xFFFFB21A)), c.y - r * 0.18f, c.y + r * 0.18f)
            val pointer = Path().apply {
                moveTo(c.x, c.y - r * 0.74f); lineTo(c.x - r * 0.11f, c.y - r * 1.0f)
                quadraticTo(c.x, c.y - r * 1.1f, c.x + r * 0.11f, c.y - r * 1.0f); close()
            }
            onDrawBehind {
                drawCircle(Color(0x47140A3C), r, c + Offset(0f, 8.dp.toPx()))
                drawCircle(rim, r, c)
                drawCircle(Color(0xFF8A4A00), r, c, style = Stroke(2.5.dp.toPx()))
                // chasing bulbs
                val t = time.value
                val phase = ((if (spinning) t * 14f else t * 3f).toInt())
                for (i in 0 until 16) {
                    val a = i * 2 * PI.toFloat() / 16
                    val p = c + Offset(cos(a) * r * 0.93f, sin(a) * r * 0.93f)
                    val lit = (i + phase) % 2 == 0
                    drawCircle(Color(0xFF8A4A00), r * 0.042f, p)
                    drawCircle(if (lit) Color(0xFFFFFDE8) else Color(0xFFFFC94D), r * 0.034f, p)
                    if (lit) drawCircle(Color(0x66FFFFFF), r * 0.06f, p)
                }
                rotate(rotation.value, c) { drawImage(face, Offset(c.x - inner, c.y - inner)) }
                drawCircle(Color(0xFF8A4A00), inner, c, style = Stroke(3.dp.toPx()))
                // hub
                drawCircle(Color(0xFFB86A00), r * 0.19f, c + Offset(0f, 3.dp.toPx()))
                drawCircle(hub, r * 0.18f, c)
                drawOval(Color.White.copy(alpha = 0.6f), c + Offset(-r * 0.1f, -r * 0.14f), Size(r * 0.2f, r * 0.09f))
                drawCircle(Color(0xFF8A4A00), r * 0.18f, c, style = Stroke(2.dp.toPx()))
                // gem pointer: flicks back when a segment edge passes
                val f = ((rotation.value % sweep) + sweep) % sweep / sweep
                val kick = if (spinning) (1f - f * 3f).coerceIn(0f, 1f) * 22f else 0f
                rotate(-kick, Offset(c.x, c.y - r * 1.0f)) {
                    drawPath(pointer, Color(0x55140A3C), alpha = 1f)
                    drawPath(pointer, Brush.verticalGradient(listOf(Color(0xFFFF8A9A), Color(0xFFE8273D)), c.y - r * 1.08f, c.y - r * 0.74f))
                    drawPath(pointer, Color(0xFF7D1222), style = Stroke(2.2.dp.toPx(), join = StrokeJoin.Round))
                    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.025f, Offset(c.x - r * 0.03f, c.y - r * 0.97f))
                }
            }
        },
    )
}

@Composable
private fun DayTile(d: Int, claimed: Boolean, today: Boolean, modifier: Modifier) {
    val glow = if (today) rememberInfiniteTransition(label = "day").animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "g") else null
    Box(
        modifier
            .height(94.dp)
            .drawWithCache {
                val r = CornerRadius(18.dp.toPx())
                val fill = when {
                    today -> Brush.verticalGradient(listOf(Color(0xFFFFF2A8), Color(0xFFFFC21A)))
                    claimed -> Brush.verticalGradient(listOf(Color(0xFFE2D8FF), Color(0xFFC9B8FF)))
                    else -> Brush.verticalGradient(listOf(Color.White, Color(0xFFEDE6FF)))
                }
                val rim = if (today) Color(0xFFC47A00) else Color(0xFF9F87F0)
                onDrawBehind {
                    if (glow != null) {
                        val k = 0.5f + 0.5f * sin(glow.value * 2f * PI.toFloat())
                        drawRoundRect(Color(0xFFFFE680).copy(alpha = 0.35f + 0.4f * k), Offset(-4.dp.toPx(), -4.dp.toPx()), Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), CornerRadius(r.x + 4.dp.toPx()))
                    }
                    drawRoundRect(rim, Offset(0f, 3.dp.toPx()), size, r)
                    drawRoundRect(fill, Offset.Zero, Size(size.width, size.height - 1.dp.toPx()), r)
                    drawRoundRect(Color.White.copy(alpha = 0.5f), Offset(size.width * 0.1f, 4.dp.toPx()), Size(size.width * 0.8f, 8.dp.toPx()), CornerRadius(4.dp.toPx()))
                    drawRoundRect(rim, cornerRadius = r, style = Stroke(2.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = if (claimed) 0.55f else 1f }) {
            Text("Day ${d + 1}", style = TextStyle(fontFamily = LilitaOne, fontSize = 14.sp, color = GameColors.Ink))
            GameIcon(if (d == 6) Ico.CHEST else Ico.COIN, Modifier.size(if (d == 6) 42.dp else 32.dp))
            Text(GameRepository.LOGIN_REWARDS[d].toString(), style = TextStyle(fontFamily = LilitaOne, fontSize = 15.sp, color = GameColors.Ink))
        }
        if (claimed) {
            Box(
                Modifier.size(38.dp).graphicsLayer { rotationZ = -12f }
                    .background(Brush.verticalGradient(listOf(GameColors.GreenLight, GameColors.Green)), CircleShape)
                    .border(2.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { GameIcon(Ico.CHECK, Modifier.size(24.dp)) }
        }
    }
}

private fun grantPrize(repo: GameRepository, v: Int) {
    if (v > 0) repo.addCoins(v) else repo.addBooster(Booster.entries[-v - 1], 1)
}

// ============================================================ COLLECTION

private enum class Rarity(val label: String, val color: BtnColor, val glow: Color) {
    COMMON("COMMON", BtnColor.Gray, Color(0xFFE6E3EF)),
    RARE("RARE", BtnColor.Blue, Color(0xFFBDEFFF)),
    EPIC("EPIC", BtnColor.Purple, Color(0xFFDCCBFF)),
    LEGENDARY("LEGENDARY", BtnColor.Orange, Color(0xFFFFE0A8)),
}

private fun rarityOf(price: Int) = when { price == 0 -> Rarity.COMMON; price <= 900 -> Rarity.RARE; price <= 1500 -> Rarity.EPIC; else -> Rarity.LEGENDARY }

@Composable
fun CollectionTab() {
    val repo = LocalRepo.current
    val sound = LocalSound.current
    var page by remember { mutableIntStateOf(0) }
    var lockedSkin by remember { mutableIntStateOf(-1) }
    var lockedTheme by remember { mutableIntStateOf(-1) }
    Column(Modifier.fillMaxSize()) {
        TabHeader("Collection")
        SegmentedSwitch(listOf("Marbles", "Themes"), page, Modifier.padding(horizontal = 14.dp).fillMaxWidth()) { page = it }
        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            page,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(Motion.STANDARD, easing = Motion.EaseIn)) { it * dir / 3 } + fadeIn(tween(Motion.FAST))) togetherWith
                    (slideOutHorizontally(tween(Motion.STANDARD, easing = Motion.EaseIn)) { -it * dir / 3 } + fadeOut(tween(Motion.MICRO)))
            },
            label = "coll",
        ) { pg ->
            TabBody {
                if (pg == 0) {
                    Catalog.skins.chunked(2).forEachIndexed { row, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEachIndexed { k, skin ->
                                val i = row * 2 + k
                                val owned = i in repo.ownedSkins.value
                                val selected = repo.skin.intValue == i
                                SkinTile(i, skin.name, skin.price, owned, selected, Modifier.weight(1f)) {
                                    if (owned) { repo.selectSkin(i); sound?.play(Sfx.BUTTON) } else lockedSkin = i
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                } else {
                    ThemeCard("Auto Rotate", "A new world every 25 levels", true, repo.theme.intValue == -1, 0, onAction = { repo.selectTheme(-1) }) {
                        Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (t in Themes.all) Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(Brush.verticalGradient(listOf(Color(t.bgTop), Color(t.bgBottom)))))
                        }
                    }
                    Catalog.themes.forEachIndexed { i, info ->
                        val owned = i in repo.ownedThemes.value
                        val name = if (info.name == "Lava") "Sunset" else info.name
                        ThemeCard(name, "Playground theme", owned, repo.theme.intValue == i, info.price, onAction = { if (owned) repo.selectTheme(i) else lockedTheme = i }) {
                            ThemePreview(Themes.get(i))
                        }
                    }
                }
            }
        }
    }
    val skinShown = rememberLatest(lockedSkin.takeIf { it >= 0 })
    if (skinShown != null) {
        val i = skinShown
        WatchAdDialog(
            visible = lockedSkin >= 0,
            title = Catalog.skins[i].name, message = "Unlock the ${Catalog.skins[i].name} marbles!", placement = AdPlacement.UNLOCK_SKIN,
            onUnlocked = { repo.grantSkin(i); repo.selectSkin(i) },
            onClose = { lockedSkin = -1 },
            coinPrice = Catalog.skins[i].price, coins = repo.coins.intValue,
            onBuy = { if (repo.buySkin(i)) { repo.selectSkin(i); sound?.play(Sfx.REWARD) }; lockedSkin = -1 },
        ) { SkinPreview(i) }
    }
    val themeShown = rememberLatest(lockedTheme.takeIf { it >= 0 })
    if (themeShown != null) {
        val i = themeShown
        val name = Catalog.themes[i].name.let { if (it == "Lava") "Sunset" else it }
        WatchAdDialog(
            visible = lockedTheme >= 0,
            title = name, message = "Unlock the $name playground theme!", placement = AdPlacement.UNLOCK_THEME,
            onUnlocked = { repo.grantTheme(i); repo.selectTheme(i) },
            onClose = { lockedTheme = -1 },
            coinPrice = Catalog.themes[i].price, coins = repo.coins.intValue,
            onBuy = { if (repo.buyTheme(i)) { repo.selectTheme(i); sound?.play(Sfx.REWARD) }; lockedTheme = -1 },
        ) { Box(Modifier.size(150.dp, 110.dp).clip(RoundedCornerShape(16.dp))) { ThemePreview(Themes.get(i)) } }
    }
}

@Composable
private fun SkinTile(i: Int, name: String, price: Int, owned: Boolean, selected: Boolean, modifier: Modifier, onAction: () -> Unit) {
    val rarity = rarityOf(price)
    SectionCard(modifier, padding = 10.dp) {
        Box(
            Modifier.fillMaxWidth().height(124.dp)
                .drawWithCache {
                    val r = CornerRadius(18.dp.toPx())
                    val spot = Brush.radialGradient(listOf(Color.White, rarity.glow, rarity.glow.copy(alpha = 0.6f)), Offset(size.width / 2, size.height * 0.45f), size.maxDimension * 0.7f)
                    onDrawBehind {
                        drawRoundRect(spot, cornerRadius = r)
                        drawRoundRect(if (selected) GameColors.Green else Color(0x332A1463), cornerRadius = r, style = Stroke(if (selected) 3.5.dp.toPx() else 1.5.dp.toPx()))
                    }
                },
        ) {
            Box(Modifier.fillMaxSize().padding(6.dp).floaty(3.dp, 2600, i * 0.17f).graphicsLayer { alpha = if (owned) 1f else 0.85f }) { SkinPreview(i) }
            Chip(rarity.label, rarity.color, Modifier.align(Alignment.TopStart).padding(6.dp), fontSize = 10.sp)
            if (selected) Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp)
                    .background(Brush.verticalGradient(listOf(GameColors.GreenLight, GameColors.Green)), CircleShape).border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { GameIcon(Ico.CHECK, Modifier.size(18.dp)) }
            if (!owned) GameIcon(Ico.LOCK, Modifier.align(Alignment.BottomEnd).padding(6.dp).size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(name, style = CardTitleStyle.copy(fontSize = 19.sp, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth(), maxLines = 1)
        Spacer(Modifier.height(6.dp))
        when {
            selected -> GameButton("Selected", Modifier.fillMaxWidth(), color = BtnColor.Gray, height = 44.dp, fontSize = 18.sp, enabled = false) {}
            owned -> GameButton("Use", Modifier.fillMaxWidth(), color = BtnColor.Green, height = 44.dp, fontSize = 18.sp) { onAction() }
            else -> GameButton("Unlock", Modifier.fillMaxWidth(), color = BtnColor.Yellow, height = 44.dp, fontSize = 18.sp, icon = Ico.LOCK) { onAction() }
        }
    }
}

@Composable
private fun ThemeCard(name: String, subtitle: String, owned: Boolean, selected: Boolean, price: Int, onAction: () -> Unit, preview: @Composable () -> Unit) {
    SectionCard(Modifier.fillMaxWidth(), padding = 10.dp) {
        Box(
            Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(18.dp))
                .border(if (selected) 3.5.dp else 0.dp, GameColors.Green, RoundedCornerShape(18.dp)),
        ) {
            preview()
            if (selected) Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(30.dp)
                    .background(Brush.verticalGradient(listOf(GameColors.GreenLight, GameColors.Green)), CircleShape).border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { GameIcon(Ico.CHECK, Modifier.size(20.dp)) }
            if (!owned) Chip(rarityOf(price).label, rarityOf(price).color, Modifier.align(Alignment.TopStart).padding(8.dp), fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = CardTitleStyle.copy(fontSize = 20.sp), maxLines = 1)
                Text(subtitle, style = CardBodyStyle.copy(fontSize = 12.sp), maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            val w = Modifier.width(126.dp)
            when {
                selected -> GameButton("Selected", w, color = BtnColor.Gray, height = 44.dp, fontSize = 17.sp, enabled = false) {}
                owned -> GameButton("Use", w, color = BtnColor.Green, height = 44.dp, fontSize = 18.sp) { onAction() }
                else -> GameButton("Unlock", w, color = BtnColor.Yellow, height = 44.dp, fontSize = 18.sp, icon = Ico.LOCK) { onAction() }
            }
        }
    }
}

@Composable
private fun SkinPreview(skin: Int) {
    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension * 0.21f
        ball(this, Offset(size.width * 0.32f, size.height * 0.64f), r, 0, skin)
        ball(this, Offset(size.width * 0.68f, size.height * 0.64f), r, 2, skin)
        ball(this, Offset(size.width * 0.5f, size.height * 0.31f), r, 1, skin)
    }
}

@Composable
private fun ThemePreview(t: com.puzzel.lab.marbelshort.render.GameTheme) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(t.bgTop), Color(t.bgBottom))))
        val w = size.width; val h = size.height
        for (k in 0 until 6) drawCircle(Color.White.copy(alpha = 0.12f), h * (0.05f + k % 3 * 0.03f), Offset(w * (0.1f + k * 0.17f), h * (0.15f + (k % 2) * 0.6f)))
        drawRoundRect(Color(t.wallDark), Offset(w * 0.06f, h * 0.08f), Size(w * 0.5f, h * 0.84f), CornerRadius(16f))
        drawRoundRect(Color(t.wall), Offset(w * 0.07f, h * 0.1f), Size(w * 0.48f, h * 0.8f), CornerRadius(14f))
        drawRoundRect(Color(t.interior), Offset(w * 0.1f, h * 0.16f), Size(w * 0.42f, h * 0.6f), CornerRadius(12f))
        val bs = h * 0.24f
        drawIntoCanvas { cv ->
            for (k in 0 until 3) Painter.block(cv.nativeCanvas, w * 0.115f + k * bs * 1.0f, h * 0.46f, bs, k + 1, 0)
            for (k in 0 until 3) Painter.block(cv.nativeCanvas, w * 0.115f + k * bs * 1.0f, h * 0.46f - bs, bs, k + 4, 0, bumps = false)
        }
        drawRoundRect(Color(t.dishDark), Offset(w * 0.6f, h * 0.2f), Size(w * 0.36f, h * 0.6f), CornerRadius(20f))
        drawRoundRect(Color(t.dish), Offset(w * 0.61f, h * 0.22f), Size(w * 0.34f, h * 0.56f), CornerRadius(18f))
        drawRoundRect(Color(0xFF3B2F86), Offset(w * 0.635f, h * 0.3f), Size(w * 0.29f, h * 0.15f), CornerRadius(40f))
        for (k in 0 until 4) ball(this, Offset(w * 0.67f + k * w * 0.075f, h * 0.375f), h * 0.05f, k + 2)
        drawIntoCanvas { cv ->
            for (k in 0 until 3) Painter.cup(cv.nativeCanvas, w * 0.635f + k * w * 0.1f, h * 0.55f, w * 0.085f, h * 0.1f, k * 2, false)
        }
    }
}

// ============================================================ PROFILE & ACHIEVEMENTS

data class Achievement(val id: Int, val title: String, val stat: Stat, val target: Long, val reward: Int)

val achievements: List<Achievement> = run {
    var id = 0
    fun a(title: String, stat: Stat, target: Long, reward: Int) = Achievement(id++, title, stat, target, reward)
    listOf(
        a("First Steps", Stat.LEVELS_WON, 5, 50), a("Getting Rolling", Stat.LEVELS_WON, 25, 100), a("Marble Pro", Stat.LEVELS_WON, 100, 250),
        a("Factory Boss", Stat.LEVELS_WON, 250, 400), a("Unstoppable", Stat.LEVELS_WON, 500, 600), a("Legend", Stat.LEVELS_WON, 1000, 1000),
        a("Grand Master", Stat.LEVELS_WON, 2500, 2000), a("The Whole Factory", Stat.LEVELS_WON, 5000, 5000),
        a("Shining Star", Stat.THREE_STARS, 10, 80), a("Star Collector", Stat.THREE_STARS, 50, 200), a("Galaxy of Stars", Stat.THREE_STARS, 200, 500), a("Perfectionist", Stat.THREE_STARS, 1000, 1500),
        a("Marble Mover", Stat.MARBLES, 500, 60), a("Marble Hauler", Stat.MARBLES, 5000, 200), a("Marble Mountain", Stat.MARBLES, 50000, 800), a("Marble Ocean", Stat.MARBLES, 250000, 2000),
        a("Cup Filler", Stat.CUPS, 100, 60), a("Cup Champion", Stat.CUPS, 1000, 250), a("Cup Factory", Stat.CUPS, 10000, 1000),
        a("Combo Starter", Stat.MAX_COMBO, 5, 100), a("Combo King", Stat.MAX_COMBO, 10, 300), a("Combo Legend", Stat.MAX_COMBO, 20, 800),
        a("Power Up", Stat.BOOSTERS, 5, 60), a("Booster Fan", Stat.BOOSTERS, 50, 300),
        a("Lucky Week", Stat.SPINS, 7, 120), a("Wheel Whiz", Stat.SPINS, 30, 400),
        a("Daily Player", Stat.DAILY_WINS, 5, 150), a("Daily Devotee", Stat.DAILY_WINS, 30, 600),
        a("Treasure Hunter", Stat.CHESTS, 5, 100), a("Treasure Hoarder", Stat.CHESTS, 50, 700),
        a("Boss Breaker", Stat.BOSS_WINS, 5, 300), a("Boss Crusher", Stat.BOSS_WINS, 50, 1500),
        a("Big Saver", Stat.COINS_EARNED, 5000, 250), a("Coin Tycoon", Stat.COINS_EARNED, 50000, 1000),
    )
}

@Composable
fun ProfileTab() {
    val repo = LocalRepo.current
    val sound = LocalSound.current
    Column(Modifier.fillMaxSize()) {
        TabHeader("Profile")
        TabBody {
            // player card
            Column(Modifier.fillMaxWidth().featureCard(Color(0xFFFF8AC4), Color(0xFF6A3FF0), Color(0xFF3A1CA8)).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(92.dp)
                            .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {
                                repo.setAvatar((repo.avatar.intValue + 1) % 9); sound?.play(Sfx.BUTTON)
                            }
                            .drawBehind {
                                val r = size.minDimension / 2
                                drawCircle(Color(0x40140A3C), r, center + Offset(0f, 4.dp.toPx()))
                                drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFF0A0), Color(0xFFFFA81A))), r, center)
                                drawCircle(Brush.verticalGradient(listOf(Color(0xFFB9A3FF), Color(0xFF4521C2))), r - 6.dp.toPx(), center)
                                drawCircle(Color(0xFF8A4A00), r, center, style = Stroke(2.dp.toPx()))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(64.dp)) { ball(this, center, size.width * 0.44f, repo.avatar.intValue, 6) }
                        Chip("Lv ${repo.level.intValue}", BtnColor.Pink, Modifier.align(Alignment.BottomCenter).offset(y = 8.dp), fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        OutlinedText(repo.playerName.value, fontSize = 24.sp, brush = Grad.Snow, align = TextAlign.Start, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GameIcon(Ico.STAR, Modifier.size(24.dp)); Spacer(Modifier.width(4.dp))
                            OutlinedText("${repo.totalStars} stars", fontSize = 17.sp, shadow = false)
                        }
                        Text("Tap the avatar to change it", style = WhiteBody.copy(fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f)))
                    }
                }
            }
            val stats = listOf(
                Triple(Ico.FLAG, "Levels", repo.stat(Stat.LEVELS_WON)), Triple(Ico.STAR, "3-Star", repo.stat(Stat.THREE_STARS)),
                Triple(Ico.MARBLES, "Marbles", repo.stat(Stat.MARBLES)), Triple(Ico.CUP, "Cups", repo.stat(Stat.CUPS)),
                Triple(Ico.FLAME, "Best Combo", repo.stat(Stat.MAX_COMBO)), Triple(Ico.CROWN, "Bosses", repo.stat(Stat.BOSS_WINS)),
            )
            for (row in stats.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for ((icon, label, v) in row) StatTile(icon, label, v, Modifier.weight(1f))
                }
            }
            val claimedCount = repo.claimedAchievements.value.size
            SectionTitle("Achievements", icon = Ico.TROPHY) { Chip("$claimedCount / ${achievements.size}", BtnColor.Yellow, fontSize = 14.sp) }
            val sorted = achievements.sortedBy {
                val done = repo.stat(it.stat) >= it.target
                val claimed = it.id in repo.claimedAchievements.value
                when { done && !claimed -> 0; !done -> 1; else -> 2 }
            }
            for (a in sorted) AchievementRow(a)
        }
    }
}

@Composable
private fun StatTile(icon: Ico, label: String, v: Long, modifier: Modifier) {
    SectionCard(modifier, padding = 8.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            GameIcon(icon, Modifier.size(34.dp))
            Text(compact(v), style = CardTitleStyle.copy(fontSize = 22.sp, color = GameColors.Purple700))
            Text(label, style = CardBodyStyle.copy(fontSize = 12.sp), maxLines = 1)
        }
    }
}

@Composable
private fun AchievementRow(a: Achievement) {
    val repo = LocalRepo.current
    val progress = repo.stat(a.stat)
    val done = progress >= a.target
    val claimed = a.id in repo.claimedAchievements.value
    val (medal, color) = when {
        a.reward >= 1000 -> Ico.CROWN to BtnColor.Orange
        a.reward >= 300 -> Ico.TROPHY to BtnColor.Pink
        else -> Ico.STAR to BtnColor.Purple
    }
    SectionCard(Modifier.fillMaxWidth().graphicsLayer { alpha = if (claimed) 0.8f else 1f }, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(medal, if (claimed || done) color else BtnColor.Gray, size = 50.dp, iconScale = 0.66f)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(a.title, style = CardTitleStyle.copy(fontSize = 18.sp), maxLines = 1)
                Text("${statLabel(a.stat)} ${compact(a.target)}", style = CardBodyStyle.copy(fontSize = 12.sp), maxLines = 1)
                Spacer(Modifier.height(5.dp))
                ProgressTube((progress.toFloat() / a.target).coerceIn(0f, 1f), Modifier.fillMaxWidth(), height = 16.dp,
                    fill = if (done) BtnColor.Green else BtnColor.Blue, label = "${compact(progress.coerceAtMost(a.target))} / ${compact(a.target)}")
            }
            Spacer(Modifier.width(10.dp))
            if (claimed) {
                Box(
                    Modifier.size(44.dp).background(Brush.verticalGradient(listOf(GameColors.GreenLight, GameColors.Green)), CircleShape).border(2.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { GameIcon(Ico.CHECK, Modifier.size(26.dp)) }
            } else {
                GameButton(a.reward.toString(), Modifier.width(98.dp), color = if (done) BtnColor.Green else BtnColor.Gray, height = 44.dp, fontSize = 17.sp,
                    icon = Ico.COIN, enabled = done, sound = Sfx.REWARD, shine = done) {
                    repo.claimAchievement(a.id, a.reward)
                }
            }
        }
    }
}

private fun statLabel(s: Stat) = when (s) {
    Stat.LEVELS_WON -> "Win levels:"; Stat.THREE_STARS -> "3-star levels:"; Stat.MARBLES -> "Sort marbles:"; Stat.CUPS -> "Fill cups:"
    Stat.BLOCKS -> "Pop blocks:"; Stat.MAX_COMBO -> "Reach combo:"; Stat.BOOSTERS -> "Use boosters:"; Stat.SPINS -> "Lucky spins:"
    Stat.DAILY_WINS -> "Daily challenges:"; Stat.COINS_EARNED -> "Earn coins:"; Stat.CHESTS -> "Open chests:"; Stat.BOSS_WINS -> "Beat boss levels:"
}

fun compact(v: Long): String = when {
    v >= 1_000_000 -> "%.1fM".format(v / 1_000_000f)
    v >= 10_000 -> "%.1fK".format(v / 1000f)
    else -> v.toString()
}
