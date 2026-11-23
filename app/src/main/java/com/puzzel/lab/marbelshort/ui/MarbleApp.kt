package com.puzzel.lab.marbelshort.ui

import android.app.Activity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.puzzel.lab.marbelshort.ui.screens.StudioIntroScreen
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.marbelshort.core.InAppUpdater
import com.puzzel.lab.marbelshort.core.Notifications
import com.puzzel.lab.marbelshort.ui.components.BtnColor
import com.puzzel.lab.marbelshort.ui.components.GameButton
import com.puzzel.lab.marbelshort.ui.components.GamePanel
import com.puzzel.lab.marbelshort.ui.components.Ico
import com.puzzel.lab.marbelshort.ui.components.PopupHost
import com.puzzel.lab.marbelshort.ui.theme.Fredoka
import com.puzzel.lab.marbelshort.ui.theme.GameColors
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.puzzel.lab.marbelshort.core.GameRepository
import com.puzzel.lab.marbelshort.core.Music
import com.puzzel.lab.marbelshort.core.SoundManager
import com.puzzel.lab.marbelshort.ui.components.LocalRepo
import com.puzzel.lab.marbelshort.ui.components.LocalSound
import com.puzzel.lab.marbelshort.ui.screens.GameScreen
import com.puzzel.lab.marbelshort.ui.screens.LegalScreen
import com.puzzel.lab.marbelshort.ui.screens.MainScreen
import com.puzzel.lab.marbelshort.ui.screens.OnboardingScreen
import com.puzzel.lab.marbelshort.ui.screens.SettingsScreen
import com.puzzel.lab.marbelshort.ui.screens.SplashScreen
import com.puzzel.lab.marbelshort.ui.theme.Motion
import com.puzzel.lab.marbelshort.core.Analytics

sealed interface Screen {
    /** NG Studio ident, shown on every launch before the game's own splash. */
    data object StudioIntro : Screen
    data object Splash : Screen
    data object Onboarding : Screen
    data object Main : Screen
    data class Game(val level: Int, val daily: Boolean = false, val nonce: Int = 0) : Screen
    data object Settings : Screen
    data class Legal(val page: String) : Screen
}

class Nav {
    var screen by mutableStateOf<Screen>(Screen.StudioIntro)
    var tab by mutableIntStateOf(2)
    /** Direction of the last navigation, used to pick push vs. pop animations. */
    var forward = true
        private set
    private val stack = ArrayList<Screen>()
    fun go(s: Screen) { forward = true; stack += screen; screen = s }
    fun replace(s: Screen) { forward = true; screen = s }
    fun back(): Boolean {
        if (stack.isEmpty()) return false
        forward = false
        screen = stack.removeAt(stack.size - 1)
        return true
    }
    fun home() { forward = false; stack.clear(); screen = Screen.Main }
}

/** Screen-to-screen motion (UI_GRAPHICS_PLAN.md §4). */
private fun AnimatedContentTransitionScope<Screen>.screenTransition(forward: Boolean): ContentTransform {
    val from = initialState
    val to = targetState
    val t = when {
        from is Screen.StudioIntro ->
            fadeIn(tween(600)) togetherWith fadeOut(tween(600))
        from is Screen.Splash ->
            (fadeIn(tween(420, easing = Motion.EaseIn)) + scaleIn(tween(520, easing = Motion.EaseIn), initialScale = 1.06f)) togetherWith
                fadeOut(tween(320))
        from is Screen.Game && to is Screen.Game ->
            fadeIn(tween(260, delayMillis = 60)) togetherWith fadeOut(tween(200))
        to is Screen.Game ->
            (fadeIn(tween(300, delayMillis = 80)) + scaleIn(tween(420, easing = Motion.EaseIn), initialScale = 0.9f)) togetherWith
                (fadeOut(tween(240)) + scaleOut(tween(380, easing = Motion.EaseOut), targetScale = 1.1f))
        from is Screen.Game ->
            (fadeIn(tween(300, delayMillis = 60)) + scaleIn(tween(420, easing = Motion.EaseIn), initialScale = 1.1f)) togetherWith
                (fadeOut(tween(240)) + scaleOut(tween(360, easing = Motion.EaseOut), targetScale = 0.9f))
        forward ->
            (slideInHorizontally(tween(Motion.EMPHASIZED, easing = Motion.EaseIn)) { it } + fadeIn(tween(Motion.FAST))) togetherWith
                (slideOutHorizontally(tween(Motion.EMPHASIZED, easing = Motion.EaseIn)) { -it / 4 } + fadeOut(tween(Motion.STANDARD)))
        else ->
            (slideInHorizontally(tween(Motion.EMPHASIZED, easing = Motion.EaseIn)) { -it / 4 } + fadeIn(tween(Motion.STANDARD))) togetherWith
                (slideOutHorizontally(tween(Motion.STANDARD, easing = Motion.EaseOut)) { it } + fadeOut(tween(Motion.STANDARD)))
    }
    // the popped screen slides away on top of the one revealed underneath it
    t.targetContentZIndex = if (forward || from is Screen.Splash) 1f else -1f
    return t using SizeTransform(clip = false)
}

/** Analytics screen names for the bottom-dock tabs of [Screen.Main], by tab index. */
private val MAIN_TABS = listOf("shop", "daily", "home_map", "collection", "profile")

@Composable
fun MarbleApp(repo: GameRepository, sound: SoundManager, updater: InAppUpdater? = null) {
    val nav = remember { Nav() }
    val context = LocalContext.current
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Analytics.notificationPermission(granted)
    }
    CompositionLocalProvider(LocalSound provides sound, LocalRepo provides repo) {
        LaunchedEffect(nav.screen::class) {
            when (nav.screen) {
                is Screen.Game -> sound.playMusic(Music.GAME)
                is Screen.StudioIntro, is Screen.Splash -> {}
                else -> sound.playMusic(Music.HOME)
            }
        }
        LaunchedEffect(nav.screen, nav.tab) {
            val s = nav.screen
            when (s) {
                Screen.StudioIntro -> Analytics.screen("studio_intro")
                Screen.Splash -> Analytics.screen("splash")
                Screen.Onboarding -> { Analytics.screen("onboarding"); Analytics.tutorialBegin() }
                Screen.Main -> Analytics.screen(MAIN_TABS.getOrElse(nav.tab) { "main" })
                is Screen.Game -> Analytics.screen(if (s.daily) "game_daily" else "game")
                Screen.Settings -> Analytics.screen("settings")
                is Screen.Legal -> Analytics.screen("legal_${s.page}")
            }
        }
        val view = LocalView.current
        val inIntro = nav.screen is Screen.StudioIntro
        LaunchedEffect(inIntro) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            val bars = WindowCompat.getInsetsController(window, view)
            if (inIntro) bars.hide(WindowInsetsCompat.Type.statusBars()) else bars.show(WindowInsetsCompat.Type.statusBars())
        }
        BackHandler(enabled = nav.screen !is Screen.Main && nav.screen !is Screen.StudioIntro && nav.screen !is Screen.Splash && nav.screen !is Screen.Onboarding && nav.screen !is Screen.Game) {
            if (!nav.back()) nav.home()
        }
        Box(Modifier.fillMaxSize().background(Color(0xFF2A1463))) {
            AnimatedContent(
                targetState = nav.screen,
                transitionSpec = { screenTransition(nav.forward) },
                contentKey = { it },
                label = "nav",
            ) { s ->
                when (s) {
                    Screen.StudioIntro -> StudioIntroScreen { nav.replace(Screen.Splash) }
                    Screen.Splash -> SplashScreen { nav.replace(if (repo.onboardingDone.value) Screen.Main else Screen.Onboarding) }
                    Screen.Onboarding -> OnboardingScreen { repo.finishOnboarding(); Analytics.tutorialComplete(); nav.replace(Screen.Main) }
                    Screen.Main -> MainScreen(nav)
                    is Screen.Game -> GameScreen(s, nav)
                    Screen.Settings -> SettingsScreen(nav)
                    is Screen.Legal -> LegalScreen(s.page) { nav.back() }
                }
            }
            // Android 13+: ask for notification permission once, the first time the player is back on the map
            // after beating level 1 (they know the game by then, so the reminders make sense).
            LaunchedEffect(nav.screen, repo.level.intValue) {
                if (nav.screen is Screen.Main && repo.level.intValue >= 2 && !repo.notifPermissionAsked.value &&
                    Notifications.needsRuntimePermission(context)
                ) {
                    delay(900)
                    repo.markNotifPermissionAsked()
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            // a flexible in-app update finished downloading: offer a restart, but never in the middle of a level
            if (updater != null) {
                var later by remember { mutableStateOf(false) }
                UpdateReadyPopup(
                    visible = updater.readyToInstall.value && !later && nav.screen !is Screen.Game && nav.screen !is Screen.Splash && nav.screen !is Screen.StudioIntro,
                    onRestart = { updater.completeUpdate() },
                    onLater = { later = true; Analytics.inAppUpdate("flexible", "later") },
                )
            }
            // debug-only fake rewarded ad (no-op in release)
            com.puzzel.lab.marbelshort.ui.components.DebugAdOverlay()
        }
    }
}

@Composable
private fun UpdateReadyPopup(visible: Boolean, onRestart: () -> Unit, onLater: () -> Unit) {
    PopupHost(visible = visible, onDismiss = onLater) {
        GamePanel("Update Ready!", ribbon = BtnColor.Green, onClose = onLater, rays = true) {
            Text(
                "A new version of Marble Pop Factory has downloaded. Restart now to get the latest levels and fixes. Your progress is safe!",
                style = TextStyle(fontFamily = Fredoka, fontSize = 17.sp, color = GameColors.Ink, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(16.dp))
            GameButton("Restart Now", Modifier.fillMaxWidth(), color = BtnColor.Green, icon = Ico.RETRY, shine = true) { onRestart() }
            Spacer(Modifier.height(8.dp))
            GameButton("Later", Modifier.fillMaxWidth(), color = BtnColor.Blue, height = 50.dp, fontSize = 20.sp) { onLater() }
        }
    }
}
