package com.ngstudio.marblepop.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.marblepop.core.AdPlacement
import com.ngstudio.marblepop.core.Ads
import com.ngstudio.marblepop.core.Booster
import com.ngstudio.marblepop.core.Sfx
import com.ngstudio.marblepop.level.LevelGenerator
import com.ngstudio.marblepop.render.Themes
import com.ngstudio.marblepop.ui.Nav
import com.ngstudio.marblepop.ui.Screen
import com.ngstudio.marblepop.ui.components.BtnColor
import com.ngstudio.marblepop.ui.components.Chip
import com.ngstudio.marblepop.ui.components.ConfettiBurst
import com.ngstudio.marblepop.ui.components.GameButton
import com.ngstudio.marblepop.ui.components.GameIcon
import com.ngstudio.marblepop.ui.components.GamePanel
import com.ngstudio.marblepop.ui.components.Ico
import com.ngstudio.marblepop.ui.components.IconRoundButton
import com.ngstudio.marblepop.ui.components.LocalRepo
import com.ngstudio.marblepop.ui.components.LocalSound
import com.ngstudio.marblepop.ui.components.NotifyBadge
import com.ngstudio.marblepop.ui.components.OutlinedText
import com.ngstudio.marblepop.ui.components.PopupHost
import com.ngstudio.marblepop.ui.components.ResourcePill
import com.ngstudio.marblepop.ui.components.CoinPill
import com.ngstudio.marblepop.ui.components.SkyBackground
import com.ngstudio.marblepop.ui.components.StarRow
import com.ngstudio.marblepop.ui.components.SunRays
import com.ngstudio.marblepop.ui.components.WatchAdDialog
import com.ngstudio.marblepop.ui.components.ball
import com.ngstudio.marblepop.ui.components.floaty
import com.ngstudio.marblepop.ui.components.idleBounce
import com.ngstudio.marblepop.ui.components.rememberLatest
import com.ngstudio.marblepop.ui.theme.Fredoka
import com.ngstudio.marblepop.ui.theme.GameColors
import com.ngstudio.marblepop.ui.theme.Grad
import com.ngstudio.marblepop.ui.theme.Motion
import com.ngstudio.marblepop.ui.theme.SkyPalette
import com.ngstudio.marblepop.ui.theme.Skies
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val DockHeight = 82.dp
private val NoRipple = MutableInteractionSource()
private val PopupBody = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = GameColors.Ink, textAlign = TextAlign.Center, lineHeight = 23.sp)

@Composable
fun MainScreen(nav: Nav) {
    val repo = LocalRepo.current
    var exitAsk by remember { mutableStateOf(false) }
    var homeWorld by remember { mutableIntStateOf(LevelGenerator.themeFor(repo.level.intValue)) }
    BackHandler { if (nav.tab != 2) nav.tab = 2 else exitAsk = !exitAsk }
    Box(Modifier.fillMaxSize()) {
        AnimatedSky(if (nav.tab == 2) Skies.forWorld(homeWorld) else Skies.Menu)
        AnimatedContent(
            nav.tab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(Motion.STANDARD + 40, easing = Motion.EaseIn)) { it * dir / 3 } + fadeIn(tween(Motion.FAST + 40))) togetherWith
                    (slideOutHorizontally(tween(Motion.STANDARD + 40, easing = Motion.EaseIn)) { -it * dir / 3 } + fadeOut(tween(Motion.FAST - 40))) using
                    SizeTransform(clip = false)
            },
            label = "tab",
        ) { tab ->
            Box(Modifier.fillMaxSize().padding(bottom = DockHeight)) {
                when (tab) {
                    0 -> ShopTab()
                    1 -> DailyTab(nav)
                    2 -> HomeTab(nav) { homeWorld = it }
                    3 -> CollectionTab(nav)
                    else -> ProfileTab()
                }
            }
        }
        BottomDock(nav.tab, Modifier.align(Alignment.BottomCenter)) { nav.tab = it }
        val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
        PopupHost(visible = exitAsk, onDismiss = { exitAsk = false }) {
            GamePanel("Leaving?", onClose = { exitAsk = false }) {
                MarbleCluster(Modifier.size(110.dp, 80.dp))
                Spacer(Modifier.height(6.dp))
                Text("Your progress is saved. Come back soon for more marbles!", style = PopupBody)
                Spacer(Modifier.height(16.dp))
                GameButton("Keep Playing", Modifier.fillMaxWidth(), color = BtnColor.Green, shine = true) { exitAsk = false }
                Spacer(Modifier.height(10.dp))
                GameButton("Quit", Modifier.fillMaxWidth(), color = BtnColor.Red, height = 50.dp, fontSize = 20.sp) { activity?.finish() }
            }
        }
    }
}

/** Sky that blends to a new palette (home worlds ↔ menu tabs). Own scope: only the sky recomposes while blending. */
@Composable
private fun AnimatedSky(target: SkyPalette) {
    val spec = tween<Color>(700)
    val top by animateColorAsState(target.top, spec, label = "t")
    val mid by animateColorAsState(target.mid, spec, label = "m")
    val bottom by animateColorAsState(target.bottom, spec, label = "b")
    val ga by animateColorAsState(target.glowA, spec, label = "ga")
    val gb by animateColorAsState(target.glowB, spec, label = "gb")
    val sil by animateColorAsState(target.silhouette, spec, label = "s")
    SkyBackground(SkyPalette(top, mid, bottom, ga, gb, sil))
}

// ============================================================ bottom dock

private data class TabDef(val icon: Ico, val label: String)
private val tabs = listOf(TabDef(Ico.SHOP, "Shop"), TabDef(Ico.CALENDAR, "Daily"), TabDef(Ico.PLAY, "Play"), TabDef(Ico.MARBLES, "Collection"), TabDef(Ico.TROPHY, "Profile"))
private val tabWeights = floatArrayOf(1f, 1f, 1.25f, 1f, 1f)

@Composable
private fun BottomDock(selected: Int, modifier: Modifier, onSelect: (Int) -> Unit) {
    val repo = LocalRepo.current
    val sound = LocalSound.current
    val pos = animateFloatAsState(selected.toFloat(), spring(dampingRatio = 0.72f, stiffness = 480f), label = "dock")
    val pink = BtnColor.Pink
    Box(modifier.fillMaxWidth().navigationBarsPadding().height(DockHeight + 18.dp)) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(DockHeight)
                .drawWithCache {
                    val r = 26.dp.toPx()
                    val w = size.width
                    val h = size.height
                    val body = Path().apply {
                        addRoundRect(RoundRect(0f, 0f, w, h + r, topLeftCornerRadius = CornerRadius(r), topRightCornerRadius = CornerRadius(r), bottomRightCornerRadius = CornerRadius.Zero, bottomLeftCornerRadius = CornerRadius.Zero))
                    }
                    val fill = Brush.verticalGradient(listOf(Color(0xFF6038E0), Color(0xFF2E1689)), 0f, h)
                    val shadow = Brush.verticalGradient(listOf(Color.Transparent, Color(0x40140A3C)), -16.dp.toPx(), 0f)
                    val total = tabWeights.sum()
                    val centers = FloatArray(tabWeights.size)
                    val widths = FloatArray(tabWeights.size)
                    var acc = 0f
                    for (i in tabWeights.indices) { widths[i] = w * tabWeights[i] / total; centers[i] = acc + widths[i] / 2; acc += widths[i] }
                    val pillH = h - 14.dp.toPx()
                    val d = 4.dp.toPx()
                    val pr = CornerRadius(20.dp.toPx())
                    val pillBrush = Brush.verticalGradient(listOf(pink.top, pink.bottom), 7.dp.toPx(), 7.dp.toPx() + pillH - d)
                    onDrawBehind {
                        drawRect(shadow, Offset(0f, -16.dp.toPx()), Size(w, 16.dp.toPx()))
                        drawPath(body, fill)
                        drawPath(body, Color.White.copy(alpha = 0.3f), style = Stroke(1.5.dp.toPx()))
                        // sliding selection pill (fades out under the centre play button)
                        val p = pos.value.coerceIn(0f, tabWeights.size - 1f)
                        val i0 = p.toInt().coerceAtMost(tabWeights.size - 2)
                        val f = p - i0
                        val cx = centers[i0] + (centers[i0 + 1] - centers[i0]) * f
                        val pw = (widths[i0] + (widths[i0 + 1] - widths[i0]) * f) - 10.dp.toPx()
                        val a = (abs(p - 2f) * 1.6f).coerceIn(0f, 1f)
                        if (a > 0.01f) {
                            val top = 7.dp.toPx()
                            drawRoundRect(pink.depth.copy(alpha = a), Offset(cx - pw / 2, top + d), Size(pw, pillH - d), pr)
                            drawRoundRect(pillBrush, Offset(cx - pw / 2, top), Size(pw, pillH - d), pr, alpha = a)
                            drawRoundRect(Color.White.copy(alpha = 0.4f * a), Offset(cx - pw / 2 + pw * 0.08f, top + 4.dp.toPx()), Size(pw * 0.84f, (pillH - d) * 0.3f), CornerRadius(8.dp.toPx()))
                            drawRoundRect(pink.outline.copy(alpha = a), Offset(cx - pw / 2, top), Size(pw, pillH), pr, style = Stroke(1.8.dp.toPx()))
                        }
                    }
                },
        )
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(DockHeight + 18.dp)) {
            tabs.forEachIndexed { i, t ->
                val sel = i == selected
                val lift = animateFloatAsState(if (sel) 1f else 0f, Motion.bouncy(), label = "lift")
                val badge = when (i) {
                    1 -> repo.canSpin || repo.canClaimLogin || !repo.dailyChallengeDone
                    0 -> System.currentTimeMillis() >= repo.freeGiftAt.value
                    else -> false
                }
                Box(
                    Modifier.weight(tabWeights[i]).fillMaxHeight()
                        .clickable(NoRipple, indication = null) { if (!sel) { sound?.play(Sfx.BUTTON); onSelect(i) } },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (i == 2) {
                        DockPlayButton(lift, Modifier.padding(bottom = 18.dp))
                    } else {
                        Column(
                            Modifier.padding(bottom = 10.dp).graphicsLayer { translationY = -lift.value * 5.dp.toPx() },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box {
                                GameIcon(t.icon, Modifier.size(40.dp).graphicsLayer {
                                    val s = 0.88f + 0.2f * lift.value
                                    scaleX = s; scaleY = s
                                    rotationZ = sin(lift.value * PI.toFloat()) * -8f
                                })
                                if (badge) NotifyBadge(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp), size = 15.dp)
                            }
                            OutlinedText(t.label, fontSize = 12.sp, color = if (sel) Color.White else Color.White.copy(alpha = 0.78f),
                                outline = if (sel) pink.outline else GameColors.Night, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** Raised gold marble button in the middle of the dock. */
@Composable
private fun DockPlayButton(lift: State<Float>, modifier: Modifier) {
    val pulse = rememberInfiniteTransition(label = "dp").animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "p")
    Box(
        modifier
            .size(70.dp)
            .graphicsLayer { val s = 0.94f + 0.08f * lift.value; scaleX = s; scaleY = s }
            .drawBehind {
                val r = size.minDimension / 2
                val c = center
                val l = lift.value
                if (l > 0.01f) {
                    val p = pulse.value
                    drawCircle(Color.White.copy(alpha = (1f - p) * 0.5f * l), r * (1.02f + p * 0.35f), c, style = Stroke(3.dp.toPx() * (1f - p) + 1f))
                }
                drawCircle(Color(0x40140A3C), r, c + Offset(0f, 5.dp.toPx()))
                drawCircle(Color(0xFFB86A00), r, c + Offset(0f, 3.dp.toPx()))
                drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFF0A0), Color(0xFFFFB21A)), c.y - r, c.y + r), r, c)
                drawCircle(Color.White.copy(alpha = 0.6f), r * 0.8f, c, style = Stroke(2.dp.toPx()))
                drawOval(Color.White.copy(alpha = 0.55f), Offset(c.x - r * 0.55f, c.y - r * 0.82f), Size(r * 1.1f, r * 0.5f))
                drawCircle(Color(0xFF8A4A00), r, c + Offset(0f, 1.5.dp.toPx()), style = Stroke(2.2.dp.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) {
        GameIcon(Ico.PLAY, Modifier.offset(x = 3.dp).size(34.dp))
    }
}

// ============================================================ home / level map

@Composable
private fun HomeTab(nav: Nav, onWorld: (Int) -> Unit) {
    val repo = LocalRepo.current
    val sound = LocalSound.current
    val current = repo.level.intValue
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (current - 3).coerceAtLeast(0))
    val scope = rememberCoroutineScope()
    var chestPopup by remember { mutableIntStateOf(-1) }
    var giftPopup by remember { mutableStateOf(false) }
    var lockedLevel by remember { mutableIntStateOf(-1) }
    repo.starsVersion.intValue
    repo.adUnlockedLevels.value
    val avatar = repo.avatar.intValue

    val centerWorld by remember { derivedStateOf { LevelGenerator.themeFor((list.firstVisibleItemIndex + 3).coerceIn(1, LevelGenerator.MAX_LEVEL)) } }
    LaunchedEffect(centerWorld) { onWorld(centerWorld) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 150.dp, bottom = 130.dp),
        ) {
            items(count = LevelGenerator.MAX_LEVEL, key = { it }) { idx ->
                val n = idx + 1
                LevelNode(
                    n, current, repo.starsFor(n),
                    chestClaimable = n % 10 == 0 && n < current && (n / 10) !in repo.openedChests.value,
                    chestOpened = n % 10 == 0 && (n / 10) in repo.openedChests.value,
                    adUnlocked = n > current && n in repo.adUnlockedLevels.value,
                    avatar = avatar,
                    onChest = { chestPopup = n / 10 },
                ) {
                    when {
                        repo.isLevelPlayable(n) -> nav.go(Screen.Game(n))
                        Ads.rewarded.isAvailable -> lockedLevel = n
                        else -> sound?.play(Sfx.TAP, 0.7f)
                    }
                }
            }
        }

        // top bar over a soft scrim so the road slides under it
        Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(Color(0x80140A3C), Color(0x00140A3C)))))
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarButton(avatar) { nav.tab = 4 }
            Spacer(Modifier.width(4.dp))
            ResourcePill(Ico.STAR, repo.totalStars.toString())
            Spacer(Modifier.weight(1f))
            CoinPill(repo.coins.intValue, onShop = { nav.tab = 0 })
            Spacer(Modifier.width(6.dp))
            IconRoundButton(Ico.GEAR, Modifier, size = 46.dp, color = BtnColor.Purple, iconScale = 0.66f) { nav.go(Screen.Settings) }
        }

        // side widgets
        Column(Modifier.align(Alignment.CenterStart).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SideWidget(Ico.WHEEL, "Spin", repo.canSpin, 0f) { nav.tab = 1 }
            SideWidget(Ico.CALENDAR, "Daily", !repo.dailyChallengeDone, 0.33f) { nav.tab = 1 }
            SideWidget(Ico.GIFT, "Gift", System.currentTimeMillis() >= repo.freeGiftAt.value, 0.66f) { giftPopup = true }
        }
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val nextChest = ((current - 1) / 10 + 1) * 10
            SideWidget(Ico.CHEST, "Lv.$nextChest", false, 0.15f) {
                scope.launch { list.animateScrollToItem((nextChest - 3).coerceAtLeast(0)) }
            }
            SideWidget(Ico.PALETTE, "Worlds", false, 0.5f) { nav.collectionPage = 1; nav.tab = 3 }
        }

        // play button
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val spec = remember(current) { LevelGenerator.get(current) }
            if (spec.isBoss) {
                Chip("BOSS LEVEL", BtnColor.Red, Modifier.idleBounce(amount = 0.08f), fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
            }
            GameButton(
                "Level $current", Modifier.width(250.dp).idleBounce(amount = 0.035f),
                color = if (spec.isBoss) BtnColor.Red else BtnColor.Green, height = 72.dp, fontSize = 32.sp, shine = true, icon = Ico.PLAY,
            ) { nav.go(Screen.Game(current)) }
        }

        val chest = rememberLatest(chestPopup.takeIf { it >= 0 })
        PopupHost(visible = chestPopup >= 0, onDismiss = null) {
            if (chest != null) ChestPopupContent(chest) { chestPopup = -1 }
        }
        val locked = rememberLatest(lockedLevel.takeIf { it > 0 })
        if (locked != null) {
            WatchAdDialog(
                visible = lockedLevel > 0,
                title = "Level $locked",
                message = "This level is locked. Watch a short ad to unlock it now!",
                placement = AdPlacement.UNLOCK_LEVEL,
                onUnlocked = { repo.unlockLevelByAd(locked); nav.go(Screen.Game(locked)) },
                onClose = { lockedLevel = -1 },
            ) {
                Box(contentAlignment = Alignment.Center) { GameIcon(Ico.LOCK, Modifier.size(96.dp).idleBounce()) }
            }
        }
        FreeGiftPopup(giftPopup) { giftPopup = false }
        // one-time notice for players who bought themes before worlds became level unlocks
        val refund = repo.themeRefund.intValue
        PopupHost(visible = refund > 0 && chestPopup < 0 && !giftPopup, onDismiss = { repo.clearThemeRefund() }) {
            GamePanel("Worlds Update", onClose = { repo.clearThemeRefund() }) {
                GameIcon(Ico.PALETTE, Modifier.size(84.dp).idleBounce())
                Spacer(Modifier.height(6.dp))
                Text("Themes are no longer sold: a new world now unlocks as you beat levels. We gave back the $refund coins you spent on themes.", style = PopupBody)
                Spacer(Modifier.height(16.dp))
                GameButton("Thanks!", Modifier.fillMaxWidth(), color = BtnColor.Green, shine = true) { repo.clearThemeRefund() }
            }
        }
    }
}

@Composable
private fun AvatarButton(avatar: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clickable(NoRipple, indication = null, onClick = onClick)
            .drawBehind {
                val r = size.minDimension / 2
                drawCircle(Color(0x40140A3C), r, center + Offset(0f, 3.dp.toPx()))
                drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFF0A0), Color(0xFFFFA81A))), r, center)
                drawCircle(Brush.verticalGradient(listOf(Color(0xFF9775FF), Color(0xFF4521C2))), r - 4.dp.toPx(), center)
                ball(this, center, r * 0.62f, avatar, 6)
                drawCircle(Color(0xFF8A4A00), r, center, style = Stroke(1.8.dp.toPx()))
            },
    )
}

@Composable
private fun SideWidget(icon: Ico, label: String, dot: Boolean, phase: Float, onClick: () -> Unit) {
    val sound = LocalSound.current
    val wiggle = rememberInfiniteTransition(label = "w").animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "r")
    Column(
        Modifier.floaty(3.dp, 2800, phase).clickable(NoRipple, indication = null) { sound?.play(Sfx.BUTTON); onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                Modifier.size(58.dp).drawWithCache {
                    val r = size.minDimension / 2
                    val c = Offset(r, r)
                    val rim = Brush.verticalGradient(listOf(Color.White, Color(0xFFC9B8FF)))
                    val inner = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE3D8FF)))
                    val gloss = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0f)), r * 0.15f, r * 0.8f)
                    onDrawBehind {
                        drawCircle(Color(0x47140A3C), r, c + Offset(0f, 4.dp.toPx()))
                        drawCircle(rim, r, c)
                        drawCircle(inner, r - 3.dp.toPx(), c)
                        drawCircle(Color(0x1F4521C2), r - 3.dp.toPx(), c + Offset(0f, r * 0.35f))
                        drawOval(gloss, Offset(r * 0.42f, r * 0.15f), Size(r * 1.16f, r * 0.6f))
                        drawCircle(GameColors.Purple800, r, c, style = Stroke(2.5.dp.toPx()))
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                GameIcon(icon, Modifier.size(40.dp).graphicsLayer {
                    if (dot) {
                        val k = wiggle.value
                        rotationZ = if (k < 0.35f) sin(k / 0.35f * 4f * PI.toFloat()) * 10f else 0f
                    }
                })
            }
            if (dot) NotifyBadge(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-1).dp))
        }
        Box(Modifier.offset(y = (-7).dp).background(Color(0xCC1A0B4A), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 1.dp)) {
            OutlinedText(label, fontSize = 12.sp, shadow = false, maxLines = 1)
        }
    }
}

@Composable
private fun LevelNode(
    n: Int, current: Int, stars: Int, chestClaimable: Boolean, chestOpened: Boolean, adUnlocked: Boolean, avatar: Int,
    onChest: () -> Unit, onClick: () -> Unit,
) {
    // 0 done, 1 current, 2 locked, 3 unlocked early by an ad
    val state = when { n < current -> 0; n == current -> 1; adUnlocked -> 3; else -> 2 }
    val boss = LevelGenerator.isBoss(n)
    val world = LevelGenerator.themeFor(n)
    val worldStart = Themes.startingAt(n) >= 0
    val hasChest = n % 10 == 0
    Box(Modifier.fillMaxWidth().height(118.dp).zIndex(if (state == 1 || chestClaimable) 1f else 0f)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoad(n, current)
            if (!worldStart && !hasChest) drawScenery(n, world)
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val x = maxWidth * nodeX(n)
            val size: Dp = if (state == 1) 84.dp else if (boss) 74.dp else 64.dp
            val side = scenerySide(n)
            if (worldStart) {
                WorldSign(n, world, Modifier.align(Alignment.Center).offset(x = maxWidth * 0.3f * side))
            }
            if (hasChest) {
                ChestSpot(chestClaimable, chestOpened, n < current, Modifier.align(Alignment.Center).offset(x = x + 92.dp * side), onChest)
            }
            val pulse = if (state == 1) rememberInfiniteTransition(label = "node").animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "p") else null
            if (pulse != null) CurrentNodeFx(pulse, size, Modifier.align(Alignment.Center).offset(x = x))
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(x = x)
                    .size(size)
                    .graphicsLayer {
                        if (pulse != null) { val s = 1f + 0.05f * sin(pulse.value * 2f * PI.toFloat()); scaleX = s; scaleY = s }
                    }
                    .clickable(NoRipple, indication = null) { onClick() }
                    .drawBehind { drawNodeDisc(state, boss) },
                contentAlignment = Alignment.Center,
            ) {
                if (state == 2) GameIcon(Ico.LOCK, Modifier.offset(y = (-2).dp).size(size * 0.46f))
                else OutlinedText(n.toString(), Modifier.offset(y = (-2).dp),
                    fontSize = if (n >= 1000) 19.sp else if (state == 1) 30.sp else 24.sp, outline = nodeOutline(state, boss), maxLines = 1)
            }
            if (pulse != null) PlayerPin(pulse, avatar, Modifier.align(Alignment.Center).offset(x = x, y = -size / 2 - 19.dp))
            if (boss && state != 0) {
                GameIcon(Ico.CROWN, Modifier.align(Alignment.Center).offset(x = x, y = -size * 0.62f).size(size * 0.5f))
            }
            if (state == 0) StarRow(stars, Modifier.align(Alignment.Center).offset(x = x, y = size * 0.58f), size = 19.dp)
        }
    }
}

/** Ping rings + glow around the current node. */
@Composable
private fun CurrentNodeFx(t: State<Float>, size: Dp, modifier: Modifier) {
    Box(modifier.size(size)) {
        Canvas(Modifier.requiredSize(size * 2.4f).align(Alignment.Center)) {
            val r = size.toPx() / 2
            val c = center
            drawCircle(Brush.radialGradient(listOf(Color(0x80FF9AD5), Color(0x00FF9AD5)), c, r * 1.9f), r * 1.9f, c)
            for (k in 0 until 2) {
                val p = (t.value + k * 0.5f) % 1f
                drawCircle(Color.White.copy(alpha = (1f - p) * 0.7f), r * (1.05f + p * 0.75f), c, style = Stroke(3.dp.toPx() * (1f - p) + 1f))
            }
        }
    }
}

/** The player's avatar in a map pin, bobbing on top of the current node. */
@Composable
private fun PlayerPin(t: State<Float>, avatar: Int, modifier: Modifier) {
    Box(modifier) {
        Canvas(
            Modifier.size(40.dp, 50.dp)
                .graphicsLayer { translationY = sin(t.value * 2f * PI.toFloat()) * 4.dp.toPx() },
        ) {
            val r = this.size.width / 2
            val pin = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(Offset(r, r), r))
                moveTo(r * 0.45f, r * 1.6f); lineTo(r, this@Canvas.size.height); lineTo(r * 1.55f, r * 1.6f); close()
            }
            drawPath(pin, Color(0x40140A3C), alpha = 1f)
            drawPath(pin, Color.White)
            drawPath(pin, GameColors.Ink, style = Stroke(2.dp.toPx()))
            ball(this, Offset(r, r), r * 0.7f, avatar, 6)
        }
    }
}

private val WorldColors = listOf(
    BtnColor.Purple, BtnColor.Blue, BtnColor.Green, BtnColor.Orange, BtnColor.Pink, BtnColor.Purple, BtnColor.Blue,
    BtnColor.Yellow, BtnColor.Green, BtnColor.Orange, BtnColor.Pink, BtnColor.Blue, BtnColor.Red, BtnColor.Yellow,
)

/** Chip colour of a world (theme index), shared by the map signs and the Worlds gallery. */
internal fun worldColor(world: Int): BtnColor = WorldColors[world.coerceIn(0, WorldColors.size - 1)]

@Composable
private fun WorldSign(n: Int, world: Int, modifier: Modifier) {
    val name = Themes.get(world).name
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Chip("WORLD ${world + 1}", worldColor(world), fontSize = 13.sp)
        Spacer(Modifier.height(3.dp))
        OutlinedText(name, fontSize = 20.sp, brush = Grad.Snow, maxLines = 1)
    }
}

@Composable
private fun ChestSpot(claimable: Boolean, opened: Boolean, reached: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val t = if (claimable) rememberInfiniteTransition(label = "c").animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "w") else null
    Box(modifier.size(70.dp).clickable(NoRipple, indication = null) { if (claimable) onClick() }, contentAlignment = Alignment.Center) {
        if (claimable) SunRays(Modifier.requiredSize(130.dp), color = Color(0xFFFFF3B0), alpha = 0.35f, rays = 12, degPerSec = 30f)
        Canvas(Modifier.fillMaxSize()) {
            drawOval(Color(0x40140A3C), Offset(size.width * 0.12f, size.height * 0.72f), Size(size.width * 0.76f, size.height * 0.2f))
        }
        GameIcon(
            Ico.CHEST,
            Modifier.size(54.dp).graphicsLayer {
                if (t != null) {
                    val k = t.value
                    rotationZ = if (k < 0.4f) sin(k / 0.4f * 6f * PI.toFloat()) * 8f else 0f
                    val s = 1f + 0.06f * sin(k * 2f * PI.toFloat()); scaleX = s; scaleY = s
                } else alpha = if (opened) 0.45f else if (reached) 0.75f else 0.92f
            },
        )
        if (opened) GameIcon(Ico.CHECK, Modifier.align(Alignment.BottomEnd).size(24.dp), tint = GameColors.Green)
    }
}

@Composable
private fun ChestPopupContent(index: Int, onClose: () -> Unit) {
    val repo = LocalRepo.current
    val sound = LocalSound.current
    val coins = 100 + index * 10
    val booster = Booster.entries[index % Booster.entries.size]
    var opened by remember { mutableStateOf(false) }
    val shake = rememberInfiniteTransition(label = "c").animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "s")
    Box {
        GamePanel(if (opened) "Rewards!" else "Treasure", ribbon = BtnColor.Yellow, rays = true) {
            if (!opened) {
                GameIcon(Ico.CHEST, Modifier.size(150.dp).graphicsLayer {
                    val k = shake.value
                    rotationZ = if (k < 0.45f) sin(k / 0.45f * 6f * PI.toFloat()) * 7f else 0f
                })
                Spacer(Modifier.height(4.dp))
                Text("You reached a treasure chest!", style = PopupBody)
                Spacer(Modifier.height(14.dp))
                GameButton("Open", Modifier.fillMaxWidth().idleBounce(), color = BtnColor.Green, shine = true) {
                    repo.openChest(index); repo.addCoins(coins); repo.addBooster(booster, 1)
                    sound?.play(Sfx.REWARD); opened = true
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    RewardTile(Ico.COIN, "+$coins", 0)
                    RewardTile(boosterIcon(booster), "+1", 1)
                }
                Spacer(Modifier.height(16.dp))
                GameButton("Awesome!", Modifier.fillMaxWidth(), color = BtnColor.Green) { onClose() }
            }
        }
        if (opened) ConfettiBurst(Modifier.matchParentSize())
    }
}

/** Reward tile that pops in (staggered by [index]). */
@Composable
fun RewardTile(icon: Ico, text: String, index: Int = 0) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(90L * index); a.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f)) }
    Column(
        Modifier.graphicsLayer { scaleX = a.value; scaleY = a.value; alpha = a.value.coerceIn(0f, 1f) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(92.dp).drawWithCache {
                val r = CornerRadius(24.dp.toPx())
                val fill = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE6DCFF)))
                onDrawBehind {
                    drawRoundRect(Color(0xFFB9A3FF), Offset(0f, 4.dp.toPx()), size, r)
                    drawRoundRect(fill, cornerRadius = r)
                    drawRoundRect(Brush.radialGradient(listOf(Color(0x66FFE680), Color(0x00FFE680)), center, size.minDimension * 0.6f), cornerRadius = r)
                    drawRoundRect(GameColors.Purple800, cornerRadius = r, style = Stroke(2.5.dp.toPx()))
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            GameIcon(icon, Modifier.size(62.dp).idleBounce())
        }
        OutlinedText(text, fontSize = 26.sp, brush = Grad.Gold, gloss = true)
    }
}

fun boosterIcon(b: Booster) = when (b) { Booster.HAMMER -> Ico.HAMMER; Booster.MAGNET -> Ico.MAGNET; Booster.SLOTS -> Ico.SLOTS; Booster.SHUFFLE -> Ico.SHUFFLE }

@Composable
fun FreeGiftPopup(visible: Boolean, onClose: () -> Unit) {
    val repo = LocalRepo.current
    val sound = LocalSound.current
    PopupHost(visible = visible, onDismiss = onClose) {
        val now = remember { System.currentTimeMillis() }
        val ready = now >= repo.freeGiftAt.value
        GamePanel("Free Gift", ribbon = BtnColor.Pink, onClose = onClose, rays = ready) {
            GameIcon(Ico.GIFT, Modifier.size(120.dp).idleBounce(ready, amount = 0.08f))
            Spacer(Modifier.height(8.dp))
            if (ready) {
                Text("A gift of 100 coins is waiting for you!", style = PopupBody)
                Spacer(Modifier.height(14.dp))
                GameButton("Claim 100", Modifier.fillMaxWidth(), color = BtnColor.Green, icon = Ico.COIN, shine = true) {
                    if (repo.claimFreeGift(System.currentTimeMillis())) sound?.play(Sfx.REWARD)
                    onClose()
                }
            } else {
                val mins = ((repo.freeGiftAt.value - now) / 60000).coerceAtLeast(1)
                Text("Next gift in ${mins / 60}h ${mins % 60}m", style = PopupBody)
                Spacer(Modifier.height(14.dp))
                GameButton("OK", Modifier.fillMaxWidth(), color = BtnColor.Purple) { onClose() }
            }
        }
    }
}

@Composable
fun MarbleCluster(modifier: Modifier, skin: Int = 0) {
    Canvas(modifier) {
        val r = size.minDimension * 0.22f
        ball(this, Offset(size.width * 0.3f, size.height * 0.62f), r, 2, skin)
        ball(this, Offset(size.width * 0.7f, size.height * 0.64f), r * 0.95f, 6, skin)
        ball(this, Offset(size.width * 0.5f, size.height * 0.32f), r, 1, skin)
    }
}
