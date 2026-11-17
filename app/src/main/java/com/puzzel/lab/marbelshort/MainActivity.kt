package com.puzzel.lab.marbelshort

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.puzzel.lab.marbelshort.core.GameRepository
import com.puzzel.lab.marbelshort.core.SoundManager
import com.puzzel.lab.marbelshort.render.Painter
import com.puzzel.lab.marbelshort.ui.MarbleApp
import com.puzzel.lab.marbelshort.ui.theme.MarbleTheme

class MainActivity : ComponentActivity() {
    private lateinit var repo: GameRepository
    private lateinit var sound: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
            isAppearanceLightStatusBars = false
        }
        repo = GameRepository(applicationContext)
        sound = SoundManager(applicationContext, repo)
        ResourcesCompat.getFont(this, R.font.lilita_one)?.let { Painter.typeface = it }
        setContent {
            MarbleTheme { MarbleApp(repo, sound) }
        }
    }

    override fun onResume() { super.onResume(); sound.resume() }
    override fun onPause() { super.onPause(); sound.pause() }
    override fun onDestroy() { super.onDestroy(); if (isFinishing) sound.release() }
}
