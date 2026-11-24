package com.puzzel.lab.marbelshort.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.marbelshort.core.Booster
import com.puzzel.lab.marbelshort.core.Sfx
import com.puzzel.lab.marbelshort.core.Stat
import com.puzzel.lab.marbelshort.engine.FailReason
import com.puzzel.lab.marbelshort.engine.GameEngine
import com.puzzel.lab.marbelshort.engine.GameLayout
import com.puzzel.lab.marbelshort.engine.GameListener
import com.puzzel.lab.marbelshort.engine.GameStatus
import com.puzzel.lab.marbelshort.engine.Haptic
import com.puzzel.lab.marbelshort.level.Feature
import com.puzzel.lab.marbelshort.level.LevelGenerator
import com.puzzel.lab.marbelshort.render.GameRenderer
import com.puzzel.lab.marbelshort.render.Painter
import com.puzzel.lab.marbelshort.render.SpriteCache
import com.puzzel.lab.marbelshort.render.Themes
import com.puzzel.lab.marbelshort.ui.Nav
import com.puzzel.lab.marbelshort.ui.Screen
import com.puzzel.lab.marbelshort.ui.components.BtnColor
import com.puzzel.lab.marbelshort.ui.components.Chip
import com.puzzel.lab.marbelshort.ui.components.ConfettiBurst
import com.puzzel.lab.marbelshort.ui.components.GameButton
import com.puzzel.lab.marbelshort.ui.components.GameIcon
import com.puzzel.lab.marbelshort.ui.components.GamePanel
import com.puzzel.lab.marbelshort.ui.components.GameToggle
import com.puzzel.lab.marbelshort.ui.components.Ico
import com.puzzel.lab.marbelshort.ui.components.IconRoundButton
import com.puzzel.lab.marbelshort.ui.components.IconTile
import com.puzzel.lab.marbelshort.ui.components.LocalRepo
import com.puzzel.lab.marbelshort.ui.components.LocalSound
import com.puzzel.lab.marbelshort.ui.components.OutlinedText
import com.puzzel.lab.marbelshort.ui.components.PopupHost
import com.puzzel.lab.marbelshort.ui.components.CoinPill
import com.puzzel.lab.marbelshort.ui.components.Ribbon
import com.puzzel.lab.marbelshort.ui.components.SparklePath
import com.puzzel.lab.marbelshort.ui.components.SunRays
import com.puzzel.lab.marbelshort.ui.components.ball
import com.puzzel.lab.marbelshort.ui.components.idleBounce
import com.puzzel.lab.marbelshort.ui.components.rememberLatest
import com.puzzel.lab.marbelshort.ui.theme.Fredoka
import com.puzzel.lab.marbelshort.ui.theme.GameColors
import com.puzzel.lab.marbelshort.ui.theme.Grad
import com.puzzel.lab.marbelshort.ui.theme.LilitaOne
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.puzzel.lab.marbelshort.core.Analytics

private enum class Overlay { NONE, WORLD, INTRO, TUTORIAL, PAUSE, WIN, FAIL, BUY }

private val PopupText = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = GameColors.Ink, textAlign = TextAlign.Center, lineHeight = 23.sp)

@Composable
fun GameScreen(screen: Screen.Game, nav: Nav) {
    val repo = LocalRepo.current
    val sound = LocalSound.current!!
    val spec = remember(screen) { LevelGenerator.get(screen.level) }
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.getTop(density)

    // Special blocks the player has never been taught: a "New!" card for each, then (once the board has
    // assembled) a spotlight tutorial on a real one, then play starts.
    val newFeatures = remember(screen) { Feature.presentIn(spec).filter { it.ordinal !in repo.seenFeatures.value } }
    var introIndex by remember(screen) { mutableIntStateOf(0) }
    var tutorialIndex by remember(screen) { mutableIntStateOf(-1) }   // -1 = not started, size = done
    // First level played in a world the player has just reached: a "New World!" card before anything else.
    val newWorld = remember(screen) { repo.themeForLevel(spec.number).takeIf { !screen.daily && it > repo.seenTheme.intValue } }
    val afterWorld = if (newFeatures.isNotEmpty()) Overlay.INTRO else Overlay.NONE
    var overlay by remember(screen) { mutableStateOf(if (newWorld != null) Overlay.WORLD else afterWorld) }
    var comboCount by remember { mutableIntStateOf(0) }
    var comboStamp by remember { mutableLongStateOf(0L) }
    var hammerMode by remember { mutableStateOf(false) }
    var buyBooster by remember { mutableStateOf<Booster?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var resultHandled by remember(screen) { mutableStateOf(false) }
    var winStars by remember { mutableIntStateOf(0) }
    var winCoins by remember { mutableIntStateOf(0) }

    LaunchedEffect(toast) { if (toast != null) { delay(1600); toast = null } }
    LaunchedEffect(screen) { Analytics.levelStart(spec.number, screen.daily, spec.isBoss) }

    val listener = remember {
        object : GameListener {
            override fun sound(sfx: Sfx, pitch: Float, volume: Float) = sound.play(sfx, pitch, volume)
            override fun haptic(kind: Haptic) = sound.haptic(kind)
            override fun combo(count: Int) { comboCount = count; comboStamp = System.nanoTime() }
            override fun statusChanged(status: GameStatus) {}
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(Themes.get(repo.themeForLevel(spec.number)).bgBottom))) {
        val wPx = constraints.maxWidth
        val hPx = constraints.maxHeight
        val scale = wPx / 720f
        // bottom of the HUD: top padding 6 + top row 56 + spacer 8 + booster tray (64 + 2·5) = 144dp, plus a small gap
        val hudPx = statusTop + with(density) { 150.dp.toPx() }
        val engine = remember(screen, wPx, hPx) {
            val layout = GameLayout(hPx / scale, hudPx / scale, 8f, spec)
            GameEngine(spec, layout, listener)
        }
        val theme = Themes.get(repo.themeForLevel(spec.number))
        val renderer = remember(engine) { GameRenderer(engine, theme, repo.skin.intValue, wPx, hPx) }
        DisposableEffect(renderer) { onDispose { renderer.release() } }

        val frame = remember { mutableLongStateOf(0L) }
        LaunchedEffect(engine) {
            var last = 0L
            while (isActive) {
                withFrameNanos { now ->
                    if (last != 0L && overlay != Overlay.PAUSE && overlay != Overlay.WORLD && overlay != Overlay.INTRO && overlay != Overlay.TUTORIAL && overlay != Overlay.BUY) {
                        engine.update((now - last) / 1_000_000_000f)
                    }
                    last = now
                    frame.longValue = now
                    // the board has assembled: start the spotlight tutorial for the new special blocks
                    if (tutorialIndex in newFeatures.indices && overlay == Overlay.NONE && !engine.inIntro) overlay = Overlay.TUTORIAL
                }
                if (!resultHandled && engine.status != GameStatus.PLAYING) {
                    resultHandled = true
                    val won = engine.status == GameStatus.WON
                    repo.addStat(Stat.MARBLES, engine.marblesSorted.toLong())
                    repo.addStat(Stat.CUPS, engine.cupsFilled.toLong())
                    repo.addStat(Stat.BLOCKS, engine.board.pops.toLong())
                    repo.maxStat(Stat.MAX_COMBO, engine.maxCombo.toLong())
                    repo.addStat(Stat.BOOSTERS, engine.boostersUsed.toLong())
                    if (won) {
                        winStars = engine.stars
                        winCoins = engine.coinReward() * (if (screen.daily) 2 else 1)
                        if (screen.daily) repo.markDailyWin() else repo.recordWin(spec.number, winStars)
                        repo.addStat(Stat.LEVELS_WON, 1)
                        if (winStars == 3) repo.addStat(Stat.THREE_STARS, 1)
                        if (spec.isBoss) repo.addStat(Stat.BOSS_WINS, 1)
                        Analytics.levelWin(spec.number, screen.daily, winStars, winCoins, engine.maxCombo, engine.boostersUsed)
                        delay(500)
                        sound.play(Sfx.WIN)
                        overlay = Overlay.WIN
                    } else {
                        Analytics.levelFail(spec.number, screen.daily, engine.failReason.name.lowercase(), engine.boostersUsed)
                        delay(450)
                        sound.play(Sfx.FAIL)
                        overlay = Overlay.FAIL
                    }
                }
            }
        }

        // ---------------- play field
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(engine) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val wx = down.position.x / scale
                        val wy = down.position.y / scale
                        if (overlay == Overlay.NONE) {
                            if (hammerMode) {
                                if (engine.hammer(wx, wy)) {
                                    repo.addBooster(Booster.HAMMER, -1)
                                    Analytics.boosterUsed("hammer", spec.number)
                                    hammerMode = false
                                }
                            } else engine.tap(wx, wy)
                        }
                    }
                }
        ) {
            frame.longValue
            drawIntoCanvas { renderer.draw(it.nativeCanvas) }
        }

        // ---------------- tutorial hand
        if (spec.number <= 2 && !screen.daily && engine.board.pops < 3 && overlay == Overlay.NONE) {
            frame.longValue
            val target = engine.hintTarget()
            if (target != null && !engine.isBusy || (target != null && engine.board.pops == 0)) {
                HandPointer(Modifier.offset { IntOffset((target!![0] * scale).toInt() - 6, (target[1] * scale).toInt() - 6) })
            }
            Box(Modifier.fillMaxWidth().padding(top = with(density) { hudPx.toDp() } + 8.dp), contentAlignment = Alignment.TopCenter) {
                OutlinedText(if (engine.board.pops == 0) "Tap a block to pop it!" else "Marbles sort into matching cups!",
                    Modifier.idleBounce(amount = 0.04f), fontSize = 22.sp, brush = Grad.Snow)
            }
        }

        // ---------------- HUD
        Column(Modifier.fillMaxWidth().padding(top = with(density) { statusTop.toDp() } + 6.dp, start = 12.dp, end = 12.dp)) {
            Box(Modifier.fillMaxWidth().height(56.dp)) {
                IconRoundButton(Ico.PAUSE, Modifier.align(Alignment.CenterStart), size = 48.dp, color = BtnColor.Purple, iconScale = 0.5f) {
                    overlay = Overlay.PAUSE
                }
                LevelBadge(spec.number, screen.daily, spec.isBoss, Modifier.align(Alignment.TopCenter).offset(x = (-14).dp, y = 2.dp))
                val coinsShown by animateIntAsState(repo.coins.intValue, tween(700), label = "coins")
                CoinPill(coinsShown, Modifier.align(Alignment.CenterEnd))
            }
            Spacer(Modifier.height(8.dp))
            BoosterTray {
                for (b in Booster.entries) {
                    BoosterButton(b, repo.boosterCount(b), active = hammerMode && b == Booster.HAMMER) {
                        if (engine.status != GameStatus.PLAYING) return@BoosterButton
                        if (repo.boosterCount(b) <= 0) { buyBooster = b; overlay = Overlay.BUY; return@BoosterButton }
                        when (b) {
                            Booster.HAMMER -> hammerMode = !hammerMode
                            Booster.MAGNET -> if (engine.magnet()) { repo.addBooster(b, -1); Analytics.boosterUsed("magnet", spec.number) } else toast = "No marbles to pull yet"
                            Booster.SLOTS -> { engine.boosterSlots(); repo.addBooster(b, -1); Analytics.boosterUsed("slots", spec.number) }
                            Booster.SHUFFLE -> if (engine.boosterShuffle()) { repo.addBooster(b, -1); Analytics.boosterUsed("shuffle", spec.number) } else toast = "Nothing to shuffle"
                        }
                    }
                }
            }
        }

        if (hammerMode) {
            Row(
                Modifier.align(Alignment.TopCenter).padding(top = with(density) { hudPx.toDp() } + 6.dp)
                    .background(Color(0xCC1A0B4A), RoundedCornerShape(50)).border(2.dp, Color(0x59FFFFFF), RoundedCornerShape(50))
                    .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameIcon(Ico.HAMMER, Modifier.size(28.dp))
                Spacer(Modifier.width(6.dp))
                OutlinedText("Tap a block to smash it!", fontSize = 18.sp, shadow = false)
                Spacer(Modifier.width(10.dp))
                GameButton("Cancel", Modifier.width(96.dp), color = BtnColor.Red, height = 40.dp, fontSize = 16.sp) { hammerMode = false }
            }
        }

        ComboBanner(comboCount, comboStamp)

        toast?.let {
            Box(Modifier.align(Alignment.Center)) {
                Text(it, style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color.White),
                    modifier = Modifier.background(Color(0xE61A0B4A), RoundedCornerShape(20.dp)).border(2.dp, Color(0x59FFFFFF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp))
            }
        }

        // ---------------- overlays (each animates in and out)
        PopupHost(visible = overlay == Overlay.WORLD) {
            if (newWorld != null) NewWorldContent(newWorld) {
                repo.markThemeSeen(newWorld)
                sound.play(Sfx.BUTTON)
                overlay = afterWorld
            }
        }
        PopupHost(visible = overlay == Overlay.INTRO) {
            newFeatures.getOrNull(introIndex)?.let { f ->
                val lastCard = introIndex == newFeatures.size - 1
                FeatureIntroContent(f, step = if (newFeatures.size > 1) "${introIndex + 1} / ${newFeatures.size}" else null,
                    button = if (lastCard) "Show me!" else "Next") {
                    if (lastCard) { overlay = Overlay.NONE; tutorialIndex = 0 } else introIndex++
                }
            }
        }
        if (overlay == Overlay.TUTORIAL) {
            newFeatures.getOrNull(tutorialIndex)?.let { f ->
                frame.longValue
                val lastStep = tutorialIndex == newFeatures.size - 1
                FeatureTutorial(f, engine.featureTarget(f), scale, lastStep,
                    step = if (newFeatures.size > 1) "${tutorialIndex + 1} / ${newFeatures.size}" else null,
                    hudBottom = with(density) { hudPx.toDp() }) {
                    sound.play(Sfx.BUTTON)
                    if (lastStep) {
                        repo.markFeaturesSeen(newFeatures)
                        tutorialIndex = newFeatures.size
                        overlay = Overlay.NONE
                    } else tutorialIndex++
                }
            }
        }
        PopupHost(visible = overlay == Overlay.PAUSE, onDismiss = { overlay = Overlay.NONE }) {
            PauseContent(
                onResume = { overlay = Overlay.NONE },
                onRestart = { Analytics.levelRetry(spec.number, "pause"); nav.replace(screen.copy(nonce = screen.nonce + 1)) },
                onHome = { nav.home() },
            )
        }
        PopupHost(visible = overlay == Overlay.WIN) {
            WinContent(spec.number, winStars, winCoins, screen.daily, engine.maxCombo,
                onCollect = { mult -> repo.addCoins(winCoins * mult); Analytics.earn(if (mult > 1) "level_win_bonus" else "level_win", winCoins * mult) },
                onNext = {
                    if (screen.daily) nav.home()
                    else nav.replace(Screen.Game(minOf(spec.number + 1, LevelGenerator.MAX_LEVEL)))
                },
                onHome = { nav.home() })
        }
        PopupHost(visible = overlay == Overlay.FAIL) {
            FailContent(engine.failReason, repo.coins.intValue,
                onContinue = {
                    if (repo.spend(CONTINUE_PRICE)) {
                        Analytics.levelContinue(spec.number, CONTINUE_PRICE)
                        engine.continueGame(); resultHandled = false; overlay = Overlay.NONE
                    }
                },
                onRetry = { Analytics.levelRetry(spec.number, "fail"); nav.replace(screen.copy(nonce = screen.nonce + 1)) },
                onHome = { nav.home() })
        }
        val buyShown = rememberLatest(buyBooster)
        PopupHost(visible = overlay == Overlay.BUY, onDismiss = { overlay = Overlay.NONE }) {
            buyShown?.let { b ->
                BuyBoosterContent(b, repo.coins.intValue,
                    onBuy = { if (repo.spend(b.price)) { repo.addBooster(b, 1); Analytics.spend("booster_${b.name.lowercase()}", b.price); sound.play(Sfx.COIN); overlay = Overlay.NONE } },
                    onClose = { overlay = Overlay.NONE })
            }
        }
    }

    BackHandler {
        if (overlay == Overlay.NONE) overlay = Overlay.PAUSE
        else if (overlay == Overlay.PAUSE || overlay == Overlay.BUY) overlay = Overlay.NONE
    }
}

const val CONTINUE_PRICE = 150

@Composable
private fun LevelBadge(level: Int, daily: Boolean, boss: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Ribbon(if (daily) "Daily" else "Level $level", if (boss && !daily) BtnColor.Red else BtnColor.Purple,
            fontSize = 20.sp, minWidth = 104.dp, height = 44.dp)
        if (boss && !daily) {
            Row(Modifier.offset(y = (-10).dp), verticalAlignment = Alignment.CenterVertically) {
                GameIcon(Ico.CROWN, Modifier.size(22.dp))
                Chip("SUPER HARD", BtnColor.Red, fontSize = 11.sp)
            }
        }
    }
}

/** Glass tray holding the four booster buttons. */
@Composable
private fun BoosterTray(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .drawWithCache {
                val r = CornerRadius(24.dp.toPx())
                val fill = Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x14FFFFFF)))
                onDrawBehind {
                    drawRoundRect(Color(0x26140A3C), Offset(0f, 3.dp.toPx()), size, r)
                    drawRoundRect(fill, cornerRadius = r)
                    drawRoundRect(Color.White.copy(alpha = 0.45f), cornerRadius = r, style = Stroke(1.5.dp.toPx()))
                    drawRoundRect(Color.White.copy(alpha = 0.3f), Offset(size.width * 0.06f, 3.dp.toPx()), Size(size.width * 0.88f, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
                }
            }
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) { content() }
}

@Composable
private fun BoosterButton(b: Booster, count: Int, active: Boolean, onClick: () -> Unit) {
    val icon = boosterIcon(b)
    val pulse = rememberInfiniteTransition(label = "bp").animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "p")
    Box(
        Modifier.size(64.dp).graphicsLayer {
            if (active) { val s = 1f + 0.07f * sin(pulse.value * 2f * PI.toFloat()); scaleX = s; scaleY = s }
        }.drawBehind {
            if (active) {
                val p = pulse.value
                drawCircle(Color(0xFFFFE680).copy(alpha = 0.55f * (1f - p)), size.minDimension * (0.45f + 0.25f * p), center)
            }
        },
    ) {
        GameButton("", Modifier.align(Alignment.Center).size(56.dp), color = if (active) BtnColor.Yellow else BtnColor.Blue, height = 56.dp,
            corner = 18.dp, icon = icon, iconSize = 38.dp, onClick = onClick)
        Box(
            Modifier.align(Alignment.TopEnd).size(25.dp)
                .background(
                    if (count > 0) Brush.verticalGradient(listOf(GameColors.GreenLight, GameColors.Green)) else Brush.verticalGradient(listOf(Color(0xFFFFA8D4), GameColors.Pink)),
                    CircleShape,
                )
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (count > 0) OutlinedText(count.toString(), fontSize = 13.sp, outline = GameColors.GreenDark, shadow = false)
            else GameIcon(Ico.PLUS, Modifier.size(13.dp))
        }
    }
}

@Composable
private fun ComboBanner(count: Int, stamp: Long) {
    if (count < 2 || stamp == 0L) return
    val anim = remember(stamp) { Animatable(0f) }
    LaunchedEffect(stamp) {
        anim.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
        delay(560)
        anim.animateTo(0f, tween(240))
    }
    val text = when (count) { 2 -> "Nice!"; 3 -> "Great!"; 4 -> "Awesome!"; 5 -> "Amazing!"; else -> "Marble-ous!" }
    val brush = when (count) { 2 -> Grad.Aqua; 3 -> Grad.Mint; 4 -> Grad.Gold; 5 -> Grad.Coral; else -> Grad.Pink }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.offset(y = (-90).dp).graphicsLayer {
            val v = anim.value
            scaleX = v; scaleY = v; alpha = v.coerceIn(0f, 1f); rotationZ = -6f
        }, contentAlignment = Alignment.Center) {
            SunRays(Modifier.requiredSize(240.dp), color = Color(0xFFFFF3B0), alpha = 0.35f, rays = 14, degPerSec = 60f, radiusScale = 0.5f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedText(text, fontSize = 52.sp, brush = brush, gloss = true, outline = GameColors.Night)
                Chip("COMBO x$count", BtnColor.Pink, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun HandPointer(modifier: Modifier) {
    val t by rememberInfiniteTransition(label = "hand").animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "h")
    val press = if (t < 0.5f) t * 2 else (1 - t) * 2
    Canvas(modifier.size(90.dp).graphicsLayer { translationX = 10f * (1 - press); translationY = 14f * (1 - press) }) {
        val s = size.width
        if (press > 0.7f) drawCircle(Color.White.copy(alpha = (press - 0.7f) * 2.5f), s * 0.16f * (1 + (press - 0.7f) * 3), Offset(s * 0.08f, s * 0.08f), style = Stroke(4.dp.toPx()))
        rotate(-28f, Offset(s * 0.2f, s * 0.2f)) {
            val finger = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(s * 0.02f, s * 0.04f, s * 0.24f, s * 0.6f, s * 0.11f, s * 0.11f)) }
            val palm = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(s * 0.02f, s * 0.38f, s * 0.6f, s * 0.86f, s * 0.18f, s * 0.18f)) }
            for (p in listOf(palm, finger)) drawPath(p, Color(0x55000000), alpha = 1f)
            drawPath(palm, Color.White); drawPath(finger, Color.White)
            for (i in 0 until 3) drawRoundRect(Color(0xFFE8E2F7), Offset(s * (0.26f + i * 0.11f), s * 0.36f), Size(s * 0.12f, s * 0.2f), CornerRadius(s * 0.06f))
            drawPath(palm, GameColors.Ink, style = Stroke(3.dp.toPx()))
            drawPath(finger, GameColors.Ink, style = Stroke(3.dp.toPx()))
            drawRoundRect(GameColors.Pink, Offset(s * 0.04f, s * 0.8f), Size(s * 0.54f, s * 0.16f), CornerRadius(s * 0.06f))
            drawRoundRect(GameColors.Ink, Offset(s * 0.04f, s * 0.8f), Size(s * 0.54f, s * 0.16f), CornerRadius(s * 0.06f), style = Stroke(3.dp.toPx()))
        }
    }
}

/** Soft halo + floor shadow behind a showcased item. */
@Composable
private fun Pedestal(modifier: Modifier, glow: Color = Color(0xFFFFE680), content: @Composable () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.55f), glow.copy(alpha = 0f)), center, size.minDimension * 0.55f), size.minDimension * 0.55f, center)
            drawOval(Color(0x332A1463), Offset(size.width * 0.2f, size.height * 0.84f), Size(size.width * 0.6f, size.height * 0.12f))
        }
        content()
    }
}

@Composable
private fun NewWorldContent(world: Int, onOk: () -> Unit) {
    val t = Themes.get(world)
    GamePanel("New World!", ribbon = BtnColor.Green, rays = true) {
        Chip("WORLD ${world + 1}", BtnColor.Purple)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.size(230.dp, 150.dp).idleBounce(amount = 0.03f).clip(RoundedCornerShape(18.dp)).border(3.dp, Color.White, RoundedCornerShape(18.dp))) {
            ThemePreview(t)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedText(t.name, fontSize = 30.sp, brush = Grad.Gold, gloss = true, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        val next = Themes.all.getOrNull(world + 1)
        Text(
            if (next != null) "You unlocked a new world! Your levels look like this until level ${next.unlockLevel}."
            else "You unlocked the final world. Enjoy it all the way to the top!",
            style = PopupText,
        )
        Spacer(Modifier.height(18.dp))
        GameButton("Let's go!", Modifier.fillMaxWidth(), color = BtnColor.Green, shine = true) { onOk() }
    }
}

@Composable
private fun FeatureIntroContent(f: Feature, step: String? = null, button: String = "Got it!", onOk: () -> Unit) {
    GamePanel("New!", ribbon = BtnColor.Pink, rays = true) {
        step?.let { Chip(it, BtnColor.Purple); Spacer(Modifier.height(4.dp)) }
        Pedestal(Modifier.size(170.dp)) {
            val sprites = remember { SpriteCache(1f) }
            DisposableEffect(sprites) { onDispose { sprites.clear() } }
            Canvas(Modifier.size(124.dp).idleBounce(amount = 0.06f)) {
                val s = size.width
                drawIntoCanvas { cv ->
                    val c = cv.nativeCanvas
                    if (f == Feature.HIDDEN_CUP) {
                        Painter.cup(c, s * 0.05f, s * 0.3f, s * 0.9f, s * 0.42f, 0, true)
                    } else {
                        val variant = when (f) { Feature.MYSTERY -> 1; Feature.RAINBOW -> 3; else -> 0 }
                        Painter.block(c, 0f, 0f, s, 2, variant)
                        val ov = when (f) {
                            Feature.COUNTER -> SpriteCache.OV_CAGE; Feature.KEY -> SpriteCache.OV_KEY; Feature.CRATE -> SpriteCache.OV_CRATE
                            Feature.ICE -> SpriteCache.OV_ICE; Feature.ARROW -> SpriteCache.OV_ARROW; Feature.BOMB -> SpriteCache.OV_BOMB
                            Feature.PAINT -> SpriteCache.OV_BUCKET; Feature.SWITCH -> SpriteCache.OV_SWITCH; else -> -1
                        }
                        if (ov > 0) c.drawBitmap(sprites.overlay(ov, s, if (ov == SpriteCache.OV_BUCKET) 2 else 1), 0f, 0f, null)
                        if (f == Feature.COUNTER) Painter.badge(c, s / 2, s * 0.45f, s * 0.2f, "3", 0xFF3D6BFF.toInt())
                        if (f == Feature.CRATE) Painter.badge(c, s / 2, s * 0.44f, s * 0.2f, "4", 0xFFFF9F1C.toInt())
                        if (f == Feature.BOMB) Painter.badge(c, s * 0.76f, s * 0.24f, s * 0.17f, "8", 0xFF4A4E69.toInt())
                        if (f == Feature.DOUBLE) Painter.badge(c, s * 0.78f, s * 0.2f, s * 0.17f, "x2", 0xFF3CCB5A.toInt())
                    }
                }
            }
        }
        OutlinedText(f.title, fontSize = 30.sp, brush = Grad.Gold, gloss = true)
        Spacer(Modifier.height(6.dp))
        Text(f.description, style = PopupText)
        Spacer(Modifier.height(18.dp))
        GameButton(button, Modifier.fillMaxWidth(), color = BtnColor.Green, shine = true) { onOk() }
    }
}

/** How-to line for the spotlight tutorial (the "New!" card already gave the short description). */
private fun tutorialTip(f: Feature): String = when (f) {
    Feature.MYSTERY -> "This block hides its colour. Clear a way out for it and its colour appears!"
    Feature.COUNTER -> "Caged! Pop the number of blocks shown on the cage and it opens."
    Feature.HIDDEN_CUP -> "This cup hides its colour. It shows once the cup reaches the top of its column."
    Feature.KEY -> "Pop this key block to break every chain. Chained blocks can't be popped before that!"
    Feature.ARROW -> "Arrow blocks can also slide out the way the arrow points, if that side is clear."
    Feature.CRATE -> "Fill the number of cups shown on the crate and it opens."
    Feature.DOUBLE -> "x2 holds 18 marbles. Make sure the funnel and belt have room before you pop it!"
    Feature.ICE -> "Tap once to crack the ice, then tap again to pop the block."
    Feature.RAINBOW -> "Rainbow marbles fit any cup. Save them for the colours you can't reach!"
    Feature.BOMB -> "Pop the bomb before its number reaches zero. Every move counts down!"
    Feature.PAINT -> "Pop the paint bucket to colour all the grey blocks with its colour."
    Feature.SWITCH -> "Popping this switch flips the belt's direction."
}

/**
 * Spotlight tutorial for one special block: the board dims, a real block of that kind (or a hidden
 * cup) is ringed and pointed at, and a card explains how it works. The game is paused meanwhile.
 */
@Composable
private fun BoxWithConstraintsScope.FeatureTutorial(
    f: Feature, target: FloatArray?, scale: Float, last: Boolean, step: String?, hudBottom: androidx.compose.ui.unit.Dp, onNext: () -> Unit,
) {
    val pulse by rememberInfiniteTransition(label = "spot").animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "p")
    val hPx = constraints.maxHeight.toFloat()
    Box(Modifier.fillMaxSize().pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } }) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }) {
            drawRect(Color(0xB81A0B4A))
            if (target != null) {
                val c = Offset(target[0] * scale, target[1] * scale); val r = target[2] * scale
                drawCircle(Color.Black, r, c, blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
                drawCircle(Color.White.copy(alpha = 1f - pulse), r * (1f + 0.4f * pulse), c, style = Stroke(4.dp.toPx()))
                drawCircle(Color(0xFFFFD54A), r, c, style = Stroke(3.5.dp.toPx()))
            }
        }
        if (target != null) HandPointer(Modifier.offset { IntOffset((target[0] * scale).toInt() - 6, (target[1] * scale).toInt() - 6) })
        val cardAtBottom = target == null || target[1] * scale < hPx * 0.5f
        Column(
            Modifier.align(if (cardAtBottom) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(start = 20.dp, end = 20.dp, top = if (cardAtBottom) 0.dp else hudBottom + 12.dp, bottom = if (cardAtBottom) 36.dp else 0.dp)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(26.dp))
                .border(3.dp, GameColors.Purple500, RoundedCornerShape(26.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            step?.let { Chip(it, BtnColor.Purple); Spacer(Modifier.height(6.dp)) }
            OutlinedText(f.title, fontSize = 26.sp, brush = Grad.Gold, gloss = true)
            Spacer(Modifier.height(6.dp))
            Text(tutorialTip(f), style = PopupText)
            Spacer(Modifier.height(14.dp))
            GameButton(if (last) "Let's play!" else "Next", Modifier.fillMaxWidth(), color = BtnColor.Green, height = 56.dp, fontSize = 22.sp, shine = true) { onNext() }
        }
    }
}

@Composable
private fun PauseContent(onResume: () -> Unit, onRestart: () -> Unit, onHome: () -> Unit) {
    val repo = LocalRepo.current
    val sound = LocalSound.current
    GamePanel("Paused", onClose = onResume) {
        Column(
            Modifier.fillMaxWidth().background(Color(0x0F4521C2), RoundedCornerShape(20.dp)).border(1.5.dp, Color(0x1F4521C2), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            ToggleRow(Ico.MUSIC, "Music", repo.musicOn.value, BtnColor.Pink) { repo.setMusic(it); sound?.onMusicSettingChanged() }
            ToggleRow(Ico.SOUND, "Sound", repo.sfxOn.value, BtnColor.Blue) { repo.setSfx(it) }
            ToggleRow(Ico.VIBRATE, "Vibration", repo.hapticsOn.value, BtnColor.Purple) { repo.setHaptics(it) }
        }
        Spacer(Modifier.height(16.dp))
        GameButton("Resume", Modifier.fillMaxWidth(), color = BtnColor.Green, icon = Ico.PLAY, shine = true) { onResume() }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GameButton("Restart", Modifier.weight(1f), color = BtnColor.Yellow, fontSize = 20.sp, height = 54.dp, icon = Ico.RETRY) { onRestart() }
            GameButton("Home", Modifier.weight(1f), color = BtnColor.Blue, fontSize = 20.sp, height = 54.dp, icon = Ico.HOME) { onHome() }
        }
    }
}

@Composable
fun ToggleRow(icon: Ico, label: String, value: Boolean, color: BtnColor = BtnColor.Purple, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, color, size = 44.dp, iconScale = 0.6f)
        Spacer(Modifier.width(12.dp))
        Text(label, style = TextStyle(fontFamily = LilitaOne, fontSize = 21.sp, color = GameColors.Ink), modifier = Modifier.weight(1f), maxLines = 1)
        GameToggle(value, onChange)
    }
}

@Composable
private fun WinContent(level: Int, stars: Int, coins: Int, daily: Boolean, maxCombo: Int, onCollect: (Int) -> Unit, onNext: () -> Unit, onHome: () -> Unit) {
    val sound = LocalSound.current
    val starAnims = remember { List(3) { Animatable(0f) } }
    val coinCount = remember { Animatable(0f) }
    var collected by remember { mutableStateOf(false) }
    var shownCoins by remember { mutableIntStateOf(coins) }
    LaunchedEffect(Unit) {
        launch { delay(250); coinCount.animateTo(coins.toFloat(), tween(900)) }
        delay(350)
        for (i in 0 until stars) {
            sound?.play(Sfx.STAR, 1f + i * 0.15f)
            launch { starAnims[i].animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f)) }
            delay(260)
        }
    }
    val needle = rememberInfiniteTransition(label = "needle").animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "n")
    val segments = intArrayOf(2, 3, 5, 3, 2)
    var frozen by remember { mutableStateOf<Float?>(null) }

    Box {
        GamePanel(if (daily) "Daily Done!" else "Level $level", ribbon = BtnColor.Green, rays = true) {
            OutlinedText("COMPLETE!", fontSize = 36.sp, brush = Grad.Gold, gloss = true, outline = GameColors.Night)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                for (i in 0 until 3) {
                    val a = starAnims[i]
                    Box(Modifier.size(if (i == 1) 82.dp else 64.dp).offset(y = if (i == 1) (-12).dp else 0.dp), contentAlignment = Alignment.Center) {
                        GameIcon(Ico.STAR_EMPTY, Modifier.fillMaxSize())
                        GameIcon(Ico.STAR, Modifier.fillMaxSize().graphicsLayer {
                            val v = a.value
                            scaleX = v; scaleY = v; alpha = v.coerceIn(0f, 1f); rotationZ = (1 - v) * 90f
                        })
                        // sparkle burst as the star lands
                        Canvas(Modifier.requiredSize(130.dp)) {
                            val v = a.value
                            if (v <= 0.05f) return@Canvas
                            val p = v.coerceIn(0f, 1.2f) / 1.2f
                            if (p >= 0.98f) return@Canvas
                            for (k in 0 until 6) {
                                val ang = k * PI.toFloat() / 3f + 0.3f
                                val rr = size.minDimension * (0.18f + 0.3f * p)
                                val sr = (7f * (1f - p) + 2f).dp.toPx()
                                translate(center.x + cos(ang) * rr, center.y + sin(ang) * rr) { scale(sr, sr, Offset.Zero) { drawPath(SparklePath, Color(0xFFFFF3B0).copy(alpha = 1f - p)) } }
                            }
                        }
                    }
                }
            }
            if (maxCombo >= 3) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GameIcon(Ico.FLAME, Modifier.size(20.dp)); Spacer(Modifier.width(4.dp))
                    Text("Best combo x$maxCombo", style = PopupText.copy(fontSize = 15.sp, color = GameColors.InkSoft))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.background(Color(0x144521C2), RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameIcon(Ico.COIN, Modifier.size(40.dp))
                Spacer(Modifier.width(6.dp))
                CoinCount { if (collected) shownCoins.toFloat() else coinCount.value }
            }
            Spacer(Modifier.height(12.dp))
            if (!collected) {
                MultiplierBar(segments, { frozen ?: needle.value })
                Spacer(Modifier.height(14.dp))
                val mult by remember { androidx.compose.runtime.derivedStateOf { segments[(needle.value * segments.size).toInt().coerceIn(0, segments.size - 1)] } }
                GameButton("Collect x$mult", Modifier.fillMaxWidth().idleBounce(amount = 0.035f), color = BtnColor.Yellow, icon = Ico.COIN, sound = Sfx.COIN, shine = true) {
                    frozen = needle.value
                    collected = true
                    shownCoins = coins * mult
                    onCollect(mult)
                    sound?.play(Sfx.REWARD)
                }
                Spacer(Modifier.height(8.dp))
                Text("Claim x1", style = PopupText.copy(fontSize = 16.sp, color = GameColors.InkSoft),
                    modifier = Modifier.padding(4.dp).pointerInput(Unit) { awaitEachGesture { awaitFirstDown(); if (!collected) { collected = true; onCollect(1); sound?.play(Sfx.COIN) } } })
            } else {
                GameButton(if (daily) "Home" else "Next Level", Modifier.fillMaxWidth().idleBounce(amount = 0.035f), color = BtnColor.Green, icon = Ico.PLAY, shine = true) { onNext() }
                Spacer(Modifier.height(10.dp))
                if (!daily) GameButton("Home", Modifier.fillMaxWidth(), color = BtnColor.Blue, height = 50.dp, fontSize = 20.sp, icon = Ico.HOME) { onHome() }
            }
        }
        ConfettiBurst(Modifier.matchParentSize(), count = 80)
    }
}

/** Isolated so the per-frame count-up only recomposes this text. */
@Composable
private fun CoinCount(value: () -> Float) {
    OutlinedText("+${value().toInt()}", fontSize = 34.sp, brush = Grad.Gold, gloss = true)
}

@Composable
private fun MultiplierBar(segments: IntArray, needle: () -> Float) {
    val cols = listOf(BtnColor.Blue, BtnColor.Green, BtnColor.Pink, BtnColor.Green, BtnColor.Blue)
    Box(Modifier.fillMaxWidth().height(50.dp)) {
        Row(
            Modifier.fillMaxSize().padding(top = 6.dp)
                .drawBehind {
                    drawRoundRect(Color(0xFF331589), Offset(0f, 3.dp.toPx()), size, CornerRadius(16.dp.toPx()))
                    drawRoundRect(Color(0xFF4521C2), cornerRadius = CornerRadius(16.dp.toPx()))
                }
                .padding(3.dp),
        ) {
            segments.forEachIndexed { i, m ->
                val c = cols[i]
                val shape = when (i) { 0 -> RoundedCornerShape(topStart = 13.dp, bottomStart = 13.dp); segments.lastIndex -> RoundedCornerShape(topEnd = 13.dp, bottomEnd = 13.dp); else -> RoundedCornerShape(0.dp) }
                Box(
                    Modifier.weight(1f).fillMaxHeight().background(Brush.verticalGradient(listOf(c.top, c.bottom)), shape)
                        .drawBehind { drawRect(Color.White.copy(alpha = 0.3f), Offset(0f, 2.dp.toPx()), Size(size.width, size.height * 0.28f)) },
                    contentAlignment = Alignment.Center,
                ) { OutlinedText("x$m", fontSize = 19.sp, outline = c.outline) }
                if (i < segments.lastIndex) Box(Modifier.width(2.dp).fillMaxHeight().background(Color(0x664521C2)))
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val x = size.width * needle().coerceIn(0f, 1f)
            val p = Path().apply { moveTo(x, size.height * 0.36f); lineTo(x - 10.dp.toPx(), 0f); lineTo(x + 10.dp.toPx(), 0f); close() }
            drawRoundRect(Color(0x552A1463), Offset(x - 1.dp.toPx(), 4.dp.toPx()), Size(4.dp.toPx(), size.height - 4.dp.toPx()), CornerRadius(2.dp.toPx()))
            drawRoundRect(Color.White, Offset(x - 2.dp.toPx(), 2.dp.toPx()), Size(4.dp.toPx(), size.height - 4.dp.toPx()), CornerRadius(2.dp.toPx()))
            drawPath(p, Color.White)
            drawPath(p, GameColors.Ink, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun FailContent(reason: FailReason, coins: Int, onContinue: () -> Unit, onRetry: () -> Unit, onHome: () -> Unit) {
    val (title, text) = when (reason) {
        FailReason.JAM -> "Belt Jammed!" to "The belt is full. Continue with 10 extra slots?"
        FailReason.BOMB -> "Boom!" to "A bomb went off. Continue with 6 more moves?"
        FailReason.NO_MOVES -> "Stuck!" to "No moves left. Continue with a tray shuffle and extra slots?"
    }
    val shake = rememberInfiniteTransition(label = "f").animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "s")
    GamePanel(title, ribbon = BtnColor.Red, onClose = onHome) {
        Pedestal(Modifier.size(200.dp, 110.dp), glow = Color(0xFFFF9AA3)) {
            Canvas(Modifier.size(180.dp, 96.dp)) {
                val k = shake.value
                val jitter = if (k < 0.3f) sin(k / 0.3f * 8f * PI.toFloat()) * 3.dp.toPx() else 0f
                if (reason == FailReason.BOMB) {
                    drawIntoCanvas { Painter.bomb(it.nativeCanvas, size.width / 2 + jitter, size.height * 0.55f, size.height * 0.75f, 0f) }
                } else {
                    // a jammed belt crammed with marbles
                    val top = size.height * 0.3f
                    drawRoundRect(Color(0xFF1E1750), Offset(jitter, top), Size(size.width, size.height * 0.5f), CornerRadius(size.height * 0.25f))
                    drawRoundRect(Color(0xFF3B2F86), Offset(jitter + 4.dp.toPx(), top + 4.dp.toPx()), Size(size.width - 8.dp.toPx(), size.height * 0.5f - 8.dp.toPx()), CornerRadius(size.height * 0.2f))
                    val cols = intArrayOf(0, 1, 2, 3, 4, 6, 7, 5, 1)
                    for (i in 0 until 9) ball(this, Offset(jitter + size.width * (0.09f + i * 0.103f), top + size.height * 0.25f), size.height * 0.11f, cols[i])
                    // warning sign
                    val wx = size.width / 2
                    val tri = Path().apply { moveTo(wx, 0f); lineTo(wx + size.height * 0.24f, size.height * 0.4f); lineTo(wx - size.height * 0.24f, size.height * 0.4f); close() }
                    drawPath(tri, Color(0xFFFFC93C)); drawPath(tri, Color(0xFF8A4A00), style = Stroke(2.5.dp.toPx(), join = StrokeJoin.Round))
                    drawLine(Color(0xFF2A1463), Offset(wx, size.height * 0.12f), Offset(wx, size.height * 0.27f), 3.dp.toPx())
                    drawCircle(Color(0xFF2A1463), 2.dp.toPx(), Offset(wx, size.height * 0.33f))
                }
            }
        }
        Text(text, style = PopupText)
        Spacer(Modifier.height(16.dp))
        GameButton("Continue  $CONTINUE_PRICE", Modifier.fillMaxWidth(), color = BtnColor.Yellow, icon = Ico.COIN, enabled = coins >= CONTINUE_PRICE, shine = coins >= CONTINUE_PRICE) { onContinue() }
        Spacer(Modifier.height(10.dp))
        GameButton("Try Again", Modifier.fillMaxWidth(), color = BtnColor.Green, icon = Ico.RETRY) { onRetry() }
    }
}

@Composable
private fun BuyBoosterContent(b: Booster, coins: Int, onBuy: () -> Unit, onClose: () -> Unit) {
    GamePanel(b.title, ribbon = BtnColor.Blue, onClose = onClose, rays = true) {
        Pedestal(Modifier.size(150.dp), glow = Color(0xFF9BE9FF)) {
            GameIcon(boosterIcon(b), Modifier.size(100.dp).idleBounce(amount = 0.06f))
        }
        Text(b.description, style = PopupText)
        Spacer(Modifier.height(16.dp))
        GameButton("Buy  ${b.price}", Modifier.fillMaxWidth(), color = BtnColor.Green, icon = Ico.COIN, enabled = coins >= b.price, shine = coins >= b.price) { onBuy() }
        Spacer(Modifier.height(6.dp))
        Text(if (coins < b.price) "Not enough coins" else "You have $coins coins",
            style = PopupText.copy(fontSize = 15.sp, color = if (coins < b.price) GameColors.Red else GameColors.InkSoft))
    }
}
