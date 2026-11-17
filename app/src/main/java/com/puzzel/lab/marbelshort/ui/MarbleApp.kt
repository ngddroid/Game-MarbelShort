package com.puzzel.lab.marbelshort.ui

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

sealed interface Screen {
    data object Splash : Screen
    data object Onboarding : Screen
    data object Main : Screen
    data class Game(val level: Int, val daily: Boolean = false, val nonce: Int = 0) : Screen
    data object Settings : Screen
    data class Legal(val page: String) : Screen
}

class Nav {
    var screen by mutableStateOf<Screen>(Screen.Splash)
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

@Composable
fun MarbleApp(repo: GameRepository, sound: SoundManager) {
    val nav = remember { Nav() }
    CompositionLocalProvider(LocalSound provides sound, LocalRepo provides repo) {
        LaunchedEffect(nav.screen::class) {
            when (nav.screen) {
                is Screen.Game -> sound.playMusic(Music.GAME)
                is Screen.Splash -> {}
                else -> sound.playMusic(Music.HOME)
            }
        }
        BackHandler(enabled = nav.screen !is Screen.Main && nav.screen !is Screen.Splash && nav.screen !is Screen.Onboarding && nav.screen !is Screen.Game) {
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
                    Screen.Splash -> SplashScreen { nav.replace(if (repo.onboardingDone.value) Screen.Main else Screen.Onboarding) }
                    Screen.Onboarding -> OnboardingScreen { repo.finishOnboarding(); nav.replace(Screen.Main) }
                    Screen.Main -> MainScreen(nav)
                    is Screen.Game -> GameScreen(s, nav)
                    Screen.Settings -> SettingsScreen(nav)
                    is Screen.Legal -> LegalScreen(s.page) { nav.back() }
                }
            }
            // debug-only fake rewarded ad (no-op in release)
            com.puzzel.lab.marbelshort.ui.components.DebugAdOverlay()
        }
    }
}
