package com.puzzel.lab.marbelshort.core

import androidx.compose.runtime.mutableStateOf
import com.puzzel.lab.marbelshort.BuildConfig

/** Where a rewarded ad is shown from — useful for analytics / ad unit mapping later. */
enum class AdPlacement { UNLOCK_LEVEL, UNLOCK_SKIN, UNLOCK_THEME }

/**
 * Rewarded-video abstraction. The UI only talks to this interface, so a real SDK
 * (AdMob, AppLovin, Unity Ads…) can be plugged in later by adding one implementation
 * and changing [Ads.rewarded] — no screen code needs to change.
 */
interface RewardedAds {
    /** False hides every "Watch Ad" button. */
    val isAvailable: Boolean

    /** Shows an ad. [onResult] is called once: true = the user earned the reward. */
    fun show(placement: AdPlacement, onResult: (rewarded: Boolean) -> Unit)
}

/** Release builds until a real SDK is integrated: no ad buttons are shown. */
object NoRewardedAds : RewardedAds {
    override val isAvailable = false
    override fun show(placement: AdPlacement, onResult: (Boolean) -> Unit) = onResult(false)
}

/**
 * Debug builds: a fake full-screen "test ad" with a short countdown (rendered by
 * `DebugAdOverlay` at the app root). Finishing it grants the reward, closing early does not.
 */
class DebugRewardedAds : RewardedAds {
    class Pending(val placement: AdPlacement, val onResult: (Boolean) -> Unit)

    val pending = mutableStateOf<Pending?>(null)
    override val isAvailable = true

    override fun show(placement: AdPlacement, onResult: (Boolean) -> Unit) {
        pending.value = Pending(placement, onResult)
    }

    fun finish(rewarded: Boolean) {
        val p = pending.value ?: return
        pending.value = null
        p.onResult(rewarded)
    }
}

object Ads {
    val rewarded: RewardedAds = if (BuildConfig.DEBUG) DebugRewardedAds() else NoRewardedAds
}
