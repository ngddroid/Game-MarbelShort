package com.ngstudio.marblepop.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.marblepop.BuildConfig
import com.ngstudio.marblepop.core.AdPlacement
import com.ngstudio.marblepop.core.Ads
import com.ngstudio.marblepop.core.DebugRewardedAds
import com.ngstudio.marblepop.core.Sfx
import com.ngstudio.marblepop.ui.theme.Fredoka
import com.ngstudio.marblepop.ui.theme.GameColors
import com.ngstudio.marblepop.ui.theme.Grad
import kotlinx.coroutines.delay
import com.ngstudio.marblepop.core.Analytics

/**
 * "Unlock with an ad" dialog used for locked levels, marble skins and themes.
 * The Watch Ad button only appears when [Ads.rewarded] is available (debug builds today);
 * [coinPrice] adds an optional "Buy" alternative.
 */
@Composable
fun WatchAdDialog(
    visible: Boolean = true,
    title: String,
    message: String,
    placement: AdPlacement,
    onUnlocked: () -> Unit,
    onClose: () -> Unit,
    coinPrice: Int? = null,
    coins: Int = 0,
    onBuy: (() -> Unit)? = null,
    preview: @Composable () -> Unit,
) {
    val ads = Ads.rewarded
    PopupHost(visible = visible, onDismiss = onClose) {
        GamePanel(title, ribbon = BtnColor.Pink, onClose = onClose, rays = true) {
            Box(Modifier.size(150.dp, 120.dp), contentAlignment = Alignment.Center) { preview() }
            Spacer(Modifier.height(8.dp))
            Text(message, style = TextStyle(fontFamily = Fredoka, fontSize = 17.sp, color = GameColors.Ink, textAlign = TextAlign.Center))
            Spacer(Modifier.height(16.dp))
            if (ads.isAvailable) {
                GameButton("Watch Ad", Modifier.fillMaxWidth().idleBounce(amount = 0.04f), color = BtnColor.Green, icon = Ico.PLAY, shine = true) {
                    ads.show(placement) { rewarded -> Analytics.rewardedAd(placement, rewarded); if (rewarded) onUnlocked() }
                    onClose()
                }
            }
            if (coinPrice != null && onBuy != null) {
                Spacer(Modifier.height(10.dp))
                GameButton("Buy  $coinPrice", Modifier.fillMaxWidth(), color = BtnColor.Yellow, height = 54.dp, fontSize = 22.sp, icon = Ico.COIN,
                    enabled = coins >= coinPrice, sound = Sfx.COIN) { onBuy() }
            }
            if (!ads.isAvailable && onBuy == null) {
                Text("Keep playing to unlock this!", style = TextStyle(fontFamily = Fredoka, fontSize = 15.sp, color = GameColors.InkSoft))
            }
        }
    }
}

/**
 * Fake rewarded ad for debug builds: a full-screen "TEST AD" with a 3 s countdown.
 * Wait for it and tap "Get Reward" to receive the reward; ✕ closes without a reward.
 * Rendered once at the app root so it covers every screen.
 */
@Composable
fun DebugAdOverlay() {
    val debug = Ads.rewarded as? DebugRewardedAds ?: return
    val pending = debug.pending.value ?: return
    val sound = LocalSound.current
    var secondsLeft by remember(pending) { mutableIntStateOf(3) }
    val progress = remember(pending) { Animatable(0f) }
    LaunchedEffect(pending) {
        sound?.pause()
        progress.animateTo(1f, tween(3000, easing = LinearEasing))
    }
    LaunchedEffect(pending) {
        while (secondsLeft > 0) { delay(1000); secondsLeft-- }
    }
    fun close(rewarded: Boolean) {
        sound?.resume()
        if (rewarded) sound?.play(Sfx.REWARD)
        debug.finish(rewarded)
    }
    Box(
        Modifier.fillMaxSize().background(Color(0xF20E0826))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.background(GameColors.Yellow, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("TEST AD · ${pending.placement.name.lowercase().replace('_', ' ')}", style = TextStyle(fontFamily = Fredoka, fontSize = 13.sp, color = GameColors.Ink))
            }
            Spacer(Modifier.weight(1f))
            if (secondsLeft > 0) {
                OutlinedText("$secondsLeft", fontSize = 24.sp)
            } else {
                IconRoundButton(Ico.CLOSE, Modifier, size = 42.dp, color = BtnColor.Gray, iconScale = 0.45f) { close(false) }
            }
        }
        Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(260.dp, 200.dp).clip(RoundedCornerShape(28.dp))
                    .background(Brush.verticalGradient(listOf(GameColors.Purple400, GameColors.Pink)))
                    .border(4.dp, Color.White, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    for (i in 0 until 9) {
                        val x = size.width * (0.15f + (i % 5) * 0.18f)
                        val y = size.height * (0.3f + (i / 5) * 0.4f) + (progress.value * 30f * if (i % 2 == 0) 1 else -1)
                        ball(this, Offset(x, y), size.height * 0.08f, i % 8)
                    }
                }
                OutlinedText("Your Ad Here", fontSize = 30.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text("This is a placeholder. A real rewarded video will play here once an ad SDK is added.",
                style = TextStyle(fontFamily = Fredoka, fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center))
            Spacer(Modifier.height(22.dp))
            Canvas(Modifier.width(260.dp).height(12.dp)) {
                drawRoundRect(Color.White.copy(alpha = 0.2f), cornerRadius = CornerRadius(size.height / 2))
                drawRoundRect(GameColors.Green, size = Size(size.width * progress.value, size.height), cornerRadius = CornerRadius(size.height / 2))
            }
            Spacer(Modifier.height(22.dp))
            GameButton(if (secondsLeft > 0) "Reward in $secondsLeft…" else "Get Reward", Modifier.width(260.dp), color = BtnColor.Green, enabled = secondsLeft == 0, icon = Ico.GIFT) {
                close(true)
            }
        }
    }
}



// ---------------------------------------------------------------- debug coins

/** Debug-build coin tools, opened from a coin pill's "+". Rendered once at the app root ([DebugCoinsOverlay]). */
object DebugCoins {
    val visible = mutableStateOf(false)
    var openShop: (() -> Unit)? = null
    fun open(shop: (() -> Unit)?) { openShop = shop; visible.value = true }
}

/**
 * Coin balance pill. Release builds: "+" opens the shop when [onShop] is given (no "+" otherwise).
 * Debug builds: every coin pill has a "+" that opens the debug coin menu (add test coins, or the shop).
 */
@Composable
fun CoinPill(value: Int, modifier: Modifier = Modifier, onShop: (() -> Unit)? = null) {
    val plus: (() -> Unit)? = if (BuildConfig.DEBUG) ({ DebugCoins.open(onShop) }) else onShop
    ResourcePill(Ico.COIN, value.toString(), modifier, onPlus = plus)
}

/** Debug-build popup to add test coins. Does nothing in release builds. */
@Composable
fun DebugCoinsOverlay() {
    if (!BuildConfig.DEBUG) return
    val repo = LocalRepo.current
    val sound = LocalSound.current
    fun close() { DebugCoins.visible.value = false }
    PopupHost(visible = DebugCoins.visible.value, onDismiss = { close() }) {
        GamePanel("Coins", ribbon = BtnColor.Yellow, onClose = { close() }) {
            Chip("DEBUG BUILD", BtnColor.Red)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameIcon(Ico.COIN, Modifier.size(40.dp))
                Spacer(Modifier.width(8.dp))
                OutlinedText("%,d".format(repo.coins.intValue), fontSize = 32.sp, brush = Grad.Gold, gloss = true)
            }
            Spacer(Modifier.height(14.dp))
            for (amount in listOf(100, 1_000, 10_000)) {
                GameButton("+%,d".format(amount), Modifier.fillMaxWidth(), color = BtnColor.Yellow, height = 54.dp, fontSize = 22.sp, icon = Ico.COIN, sound = Sfx.COIN) {
                    repo.addCoins(amount)
                }
                Spacer(Modifier.height(8.dp))
            }
            DebugCoins.openShop?.let { shop ->
                Spacer(Modifier.height(4.dp))
                GameButton("Open Shop", Modifier.fillMaxWidth(), color = BtnColor.Purple, height = 50.dp, fontSize = 20.sp, icon = Ico.SHOP) { close(); shop() }
            }
        }
    }
}
