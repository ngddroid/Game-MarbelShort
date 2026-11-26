package com.ngstudio.marblepop.core

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ngstudio.marblepop.R
import com.ngstudio.marblepop.engine.Haptic

enum class Sfx(val res: Int, val minGapMs: Long = 0) {
    TAP(R.raw.sfx_tap, 30),
    POP(R.raw.sfx_pop, 40),
    CLACK(R.raw.sfx_clack, 45),
    PLINK(R.raw.sfx_plink, 25),
    CUP_COMPLETE(R.raw.sfx_cup_complete, 60),
    COMBO(R.raw.sfx_combo, 100),
    WIN(R.raw.sfx_win),
    FAIL(R.raw.sfx_fail),
    COIN(R.raw.sfx_coin, 40),
    BUTTON(R.raw.sfx_button, 40),
    WHOOSH(R.raw.sfx_whoosh, 60),
    STAR(R.raw.sfx_star, 50),
    UNLOCK(R.raw.sfx_unlock, 80),
    ICE(R.raw.sfx_ice_crack, 50),
    BOMB_TICK(R.raw.sfx_bomb_tick, 80),
    EXPLODE(R.raw.sfx_explode, 100),
    SPIN_TICK(R.raw.sfx_spin_tick, 20),
    REWARD(R.raw.sfx_reward, 100),
    WARNING(R.raw.sfx_warning, 600),
}

enum class Music(val res: Int) { HOME(R.raw.music_home), GAME(R.raw.music_game) }

/** Low-latency SFX via SoundPool plus looping background music. */
class SoundManager(private val context: Context, private val repo: GameRepository) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    private val ids = IntArray(Sfx.entries.size)
    private val last = LongArray(Sfx.entries.size)
    private var player: MediaPlayer? = null
    private var currentMusic: Music? = null
    private var paused = false

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        Sfx.entries.forEach { ids[it.ordinal] = pool.load(context, it.res, 1) }
    }

    fun play(sfx: Sfx, pitch: Float = 1f, volume: Float = 1f) {
        if (!repo.sfxOn.value) return
        val now = SystemClock.uptimeMillis()
        if (now - last[sfx.ordinal] < sfx.minGapMs) return
        last[sfx.ordinal] = now
        val v = (volume * 0.9f).coerceIn(0f, 1f)
        pool.play(ids[sfx.ordinal], v, v, 1, 0, pitch.coerceIn(0.5f, 2f))
    }

    fun haptic(kind: Haptic) {
        if (!repo.hapticsOn.value) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val effect = when (kind) {
                    Haptic.LIGHT -> VibrationEffect.createOneShot(12, 70)
                    Haptic.MEDIUM -> VibrationEffect.createOneShot(22, 140)
                    Haptic.HEAVY -> VibrationEffect.createOneShot(70, 255)
                    Haptic.SUCCESS -> VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 40), intArrayOf(0, 160, 0, 220), -1)
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(if (kind == Haptic.HEAVY) 60 else 18)
            }
        } catch (_: Exception) {
        }
    }

    fun playMusic(music: Music) {
        if (currentMusic == music && player != null) { if (!paused) resumeIfAllowed(); return }
        stopMusic()
        currentMusic = music
        if (!repo.musicOn.value) return
        player = MediaPlayer.create(context, music.res)?.apply {
            isLooping = true
            val v = if (music == Music.GAME) 0.45f else 0.55f
            setVolume(v, v)
            start()
        }
    }

    private fun resumeIfAllowed() {
        val p = player ?: return
        if (repo.musicOn.value && !p.isPlaying) p.start()
    }

    fun stopMusic() {
        player?.run { try { stop() } catch (_: Exception) {}; release() }
        player = null
    }

    fun onMusicSettingChanged() {
        if (repo.musicOn.value) { val m = currentMusic; currentMusic = null; if (m != null) playMusic(m) } else stopMusic()
    }

    fun pause() { paused = true; player?.let { if (it.isPlaying) it.pause() } }
    fun resume() { paused = false; resumeIfAllowed() ; if (player == null && currentMusic != null && repo.musicOn.value) { val m = currentMusic!!; currentMusic = null; playMusic(m) } }

    fun release() { stopMusic(); pool.release() }
}
