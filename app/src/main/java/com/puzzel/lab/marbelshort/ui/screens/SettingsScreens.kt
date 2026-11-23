package com.puzzel.lab.marbelshort.ui.screens

import android.content.Intent
import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.puzzel.lab.marbelshort.core.Analytics
import com.puzzel.lab.marbelshort.core.Notifications
import com.puzzel.lab.marbelshort.core.Push
import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.puzzel.lab.marbelshort.R
import com.puzzel.lab.marbelshort.core.Sfx
import com.puzzel.lab.marbelshort.engine.Haptic
import com.puzzel.lab.marbelshort.ui.Nav
import com.puzzel.lab.marbelshort.ui.Screen
import com.puzzel.lab.marbelshort.ui.components.BtnColor
import com.puzzel.lab.marbelshort.ui.components.GameButton
import com.puzzel.lab.marbelshort.ui.components.GameIcon
import com.puzzel.lab.marbelshort.ui.components.GamePanel
import com.puzzel.lab.marbelshort.ui.components.Ico
import com.puzzel.lab.marbelshort.ui.components.IconTile
import com.puzzel.lab.marbelshort.ui.components.LocalRepo
import com.puzzel.lab.marbelshort.ui.components.LocalSound
import com.puzzel.lab.marbelshort.ui.components.OutlinedText
import com.puzzel.lab.marbelshort.ui.components.PopupHost
import com.puzzel.lab.marbelshort.ui.components.ScreenHeader
import com.puzzel.lab.marbelshort.ui.components.SectionCard
import com.puzzel.lab.marbelshort.ui.components.SkyBackground
import com.puzzel.lab.marbelshort.ui.components.floaty
import com.puzzel.lab.marbelshort.ui.theme.Fredoka
import com.puzzel.lab.marbelshort.ui.theme.GameColors
import com.puzzel.lab.marbelshort.ui.theme.Grad
import com.puzzel.lab.marbelshort.ui.theme.LilitaOne
import com.puzzel.lab.marbelshort.ui.theme.Skies

private val GroupTitle = TextStyle(fontFamily = LilitaOne, fontSize = 21.sp, color = GameColors.Purple700)
private val RowLabel = TextStyle(fontFamily = LilitaOne, fontSize = 20.sp, color = GameColors.Ink)
private val Hint = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = GameColors.InkSoft)

@Composable
fun SettingsScreen(nav: Nav) {
    val repo = LocalRepo.current
    val sound = LocalSound.current
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }

    // notifications: is the game allowed to post at all? Re-checked whenever we come back from system settings.
    var systemAllowed by remember { mutableStateOf(Notifications.canPost(context)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) systemAllowed = Notifications.canPost(context) }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Analytics.notificationPermission(granted)
        systemAllowed = Notifications.canPost(context)
    }
    /** Ask for the permission while Android still shows the dialog; after that, send the player to system settings. */
    fun allowNotifications() {
        if (Notifications.canPost(context)) return
        val activity = context as? Activity
        val canAsk = Notifications.needsRuntimePermission(context) && activity != null &&
            (!repo.notifPermissionAsked.value || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS))
        if (canAsk) {
            repo.markNotifPermissionAsked()
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            runCatching { context.startActivity(Notifications.systemSettingsIntent(context)) }
        }
    }
    val version = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0" } catch (_: Exception) { "1.0" }
    }
    Box(Modifier.fillMaxSize()) {
        SkyBackground(Skies.Menu)
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Settings", onBack = { nav.back() })
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(2.dp))
                SettingsGroup("Sound & Feel", Ico.SOUND) {
                    ToggleRow(Ico.MUSIC, "Music", repo.musicOn.value, BtnColor.Pink) { repo.setMusic(it); sound?.onMusicSettingChanged() }
                    RowDivider()
                    ToggleRow(Ico.SOUND, "Sound Effects", repo.sfxOn.value, BtnColor.Blue) { repo.setSfx(it) }
                    RowDivider()
                    ToggleRow(Ico.VIBRATE, "Vibration", repo.hapticsOn.value, BtnColor.Purple) { repo.setHaptics(it); if (it) sound?.haptic(Haptic.MEDIUM) }
                }
                SettingsGroup("Notifications", Ico.CALENDAR) {
                    ToggleRow(Ico.GIFT, "News & Events", repo.pushOn.value, BtnColor.Pink) { on ->
                        repo.setPushOn(on); Push.sync(context); Analytics.settingChanged("push", on)
                        if (on) allowNotifications()
                    }
                    if (!systemAllowed) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Notifications are blocked for this game. Tap here to allow them.",
                            style = Hint.copy(color = GameColors.Purple700),
                            modifier = Modifier.clickable { allowNotifications() },
                        )
                    }
                }
                SettingsGroup("Privacy & Legal", Ico.SHIELD) {
                    ToggleRow(Ico.SPARK, "Share Usage Data", repo.shareData.value, BtnColor.Blue) { on ->
                        if (!on) Analytics.settingChanged("share_data", false)
                        repo.setShareData(on); Analytics.setCollectionEnabled(on)
                    }
                    Text("Anonymous gameplay stats and crash reports help us fix bugs and balance levels. No ads, no personal info.", style = Hint)
                    Spacer(Modifier.height(4.dp))
                    RowDivider()
                    LinkRow(Ico.DOC, BtnColor.Purple, "Terms of Service") { nav.go(Screen.Legal("terms")) }
                    RowDivider()
                    LinkRow(Ico.SHIELD, BtnColor.Green, "Privacy Policy") { nav.go(Screen.Legal("privacy")) }
                }
                SettingsGroup("Progress", Ico.FLAG) {
                    Text("Your game is saved on this device only.", style = Hint)
                    Spacer(Modifier.height(10.dp))
                    GameButton("Reset Progress", Modifier.fillMaxWidth(), color = BtnColor.Red, height = 52.dp, fontSize = 20.sp, icon = Ico.RETRY) { confirmReset = true }
                }
                // footer
                Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ic_splash_logo), contentDescription = null, modifier = Modifier.size(120.dp).floaty(4.dp, 3000))
                    OutlinedText("Marble Pop Factory", Modifier.padding(top = 0.dp), fontSize = 24.sp, brush = Grad.Gold, gloss = true)
                    Spacer(Modifier.height(4.dp))
                    Text("Version $version  ·  NG Studio", style = Hint.copy(color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        PopupHost(visible = confirmReset, onDismiss = { confirmReset = false }) {
            GamePanel("Reset?", ribbon = BtnColor.Red, onClose = { confirmReset = false }) {
                IconTile(Ico.RETRY, BtnColor.Red, size = 72.dp, iconScale = 0.55f)
                Spacer(Modifier.height(10.dp))
                Text("All levels, stars, coins and items will be erased. This cannot be undone.",
                    style = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = GameColors.Ink, textAlign = TextAlign.Center, lineHeight = 23.sp))
                Spacer(Modifier.height(16.dp))
                GameButton("Erase Everything", Modifier.fillMaxWidth(), color = BtnColor.Red, height = 54.dp, fontSize = 20.sp) {
                    repo.resetProgress(); sound?.onMusicSettingChanged(); confirmReset = false
                }
                Spacer(Modifier.height(10.dp))
                GameButton("Keep My Progress", Modifier.fillMaxWidth(), color = BtnColor.Green, height = 54.dp, fontSize = 20.sp, shine = true) { confirmReset = false }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, icon: Ico, content: @Composable ColumnScope.() -> Unit) {
    SectionCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GameIcon(icon, Modifier.size(24.dp), tint = GameColors.Purple700)
            Spacer(Modifier.width(6.dp))
            Text(title, style = GroupTitle)
        }
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 56.dp).height(1.5.dp).background(Color(0x1A4521C2), RoundedCornerShape(1.dp)))
}

/** Tappable row with an icon tile and a chevron; squishes slightly while pressed. */
@Composable
private fun LinkRow(icon: Ico, color: BtnColor, label: String, onClick: () -> Unit) {
    val sound = LocalSound.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "row")
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { val s = 1f - press * 0.03f; scaleX = s; scaleY = s }
            .clip(RoundedCornerShape(14.dp))
            .drawBehind { if (press > 0.01f) drawRoundRect(Color(0x144521C2).copy(alpha = 0.08f * press), cornerRadius = CornerRadius(14.dp.toPx())) }
            .clickable(src, indication = null) { sound?.play(Sfx.BUTTON); onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, color, size = 44.dp, iconScale = 0.62f)
        Spacer(Modifier.width(12.dp))
        Text(label, style = RowLabel, modifier = Modifier.weight(1f), maxLines = 1)
        Box(
            Modifier.size(34.dp).background(Color(0x144521C2), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { GameIcon(Ico.CHEVRON, Modifier.size(20.dp), tint = GameColors.Purple700) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LegalScreen(page: String, onBack: () -> Unit) {
    val title = if (page == "terms") "Terms of Service" else "Privacy Policy"
    Box(Modifier.fillMaxSize()) {
        SkyBackground(Skies.Menu, particles = false)
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title, onBack = onBack)
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                    .navigationBarsPadding()
                    .drawBehind {
                        val r = CornerRadius(30.dp.toPx())
                        drawRoundRect(Color(0x4D0D0530), Offset(0f, 8.dp.toPx()), size, r)
                        drawRoundRect(Grad.PanelRim, cornerRadius = r)
                    }
                    .padding(4.dp)
                    .clip(RoundedCornerShape(26.dp)),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.parseColor("#7C55FF"))
                            settings.javaScriptEnabled = false
                            settings.allowFileAccess = false
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val uri = request?.url ?: return true
                                    if (uri.toString().startsWith("file:///android_asset/")) return false
                                    // Developer contact links open the phone's email / dialer app; everything else stays blocked.
                                    val action = when (uri.scheme) {
                                        "mailto" -> Intent.ACTION_SENDTO
                                        "tel" -> Intent.ACTION_DIAL
                                        else -> return true
                                    }
                                    runCatching { view?.context?.startActivity(Intent(action, uri)) }
                                    return true
                                }
                            }
                            loadUrl("file:///android_asset/legal/$page.html")
                        }
                    },
                )
            }
        }
    }
}
