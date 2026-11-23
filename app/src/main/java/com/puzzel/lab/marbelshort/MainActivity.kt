package com.puzzel.lab.marbelshort

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.puzzel.lab.marbelshort.core.Analytics
import com.puzzel.lab.marbelshort.core.GameRepository
import com.puzzel.lab.marbelshort.core.InAppUpdater
import com.puzzel.lab.marbelshort.core.Notifications
import com.puzzel.lab.marbelshort.core.Push
import com.puzzel.lab.marbelshort.core.SoundManager
import com.puzzel.lab.marbelshort.render.Painter
import com.puzzel.lab.marbelshort.ui.MarbleApp
import com.puzzel.lab.marbelshort.ui.theme.MarbleTheme

class MainActivity : ComponentActivity() {
    private lateinit var repo: GameRepository
    private lateinit var sound: SoundManager
    private lateinit var updater: InAppUpdater

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
        updater = InAppUpdater(this)
        Analytics.progress(repo.level.intValue)
        Push.sync(applicationContext)
        if (savedInstanceState == null) {
            logNotificationOpen(intent)
            updater.checkForUpdate()
        }
        ResourcesCompat.getFont(this, R.font.lilita_one)?.let { Painter.typeface = it }
        setContent {
            MarbleTheme { MarbleApp(repo, sound, updater) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        logNotificationOpen(intent)
    }

    private fun logNotificationOpen(intent: Intent?) {
        val source = intent?.getStringExtra(Notifications.EXTRA_SOURCE) ?: return
        intent.removeExtra(Notifications.EXTRA_SOURCE)
        Analytics.notificationOpened(source)
    }

    override fun onResume() { super.onResume(); Notifications.appInForeground = true; sound.resume(); updater.onResume() }
    override fun onPause() { super.onPause(); Notifications.appInForeground = false; sound.pause() }
    override fun onDestroy() {
        super.onDestroy()
        updater.release()
        if (isFinishing) sound.release()
    }
}
