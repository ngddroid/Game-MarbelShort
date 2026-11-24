package com.puzzel.lab.marbelshort.core

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.puzzel.lab.marbelshort.level.Feature
import com.puzzel.lab.marbelshort.level.LevelGenerator
import com.puzzel.lab.marbelshort.render.Themes
import java.time.LocalDate

enum class Booster(val title: String, val price: Int, val description: String) {
    HAMMER("Hammer", 150, "Smash any block, even locked ones."),
    MAGNET("Magnet", 200, "Pull marbles of one colour straight into their cups."),
    SLOTS("+6 Slots", 120, "Adds 6 extra slots to the belt."),
    SHUFFLE("Shuffle", 100, "Shuffles the colours of the waiting cups."),
}

enum class Stat { LEVELS_WON, THREE_STARS, MARBLES, CUPS, BLOCKS, MAX_COMBO, BOOSTERS, SPINS, DAILY_WINS, COINS_EARNED, CHESTS, BOSS_WINS }

data class Skin(val name: String, val price: Int)

object Catalog {
    val skins = listOf(
        Skin("Glossy", 0), Skin("Glass", 600), Skin("Candy", 900), Skin("Galaxy", 1500),
        Skin("Metal", 1200), Skin("Neon", 1800), Skin("Smiley", 2500),
    )
    /**
     * Coin prices of the themes that used to be sold, by their old index (Ocean, Space, Lava, Forest).
     * Only used to refund players who bought them before themes became level unlocks.
     */
    val legacyThemePrices = mapOf(1 to 800, 2 to 1500, 3 to 1200, 4 to 1000)
}

/** All persistent player data, exposed as Compose state. */
class GameRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val onboardingDone = mutableStateOf(prefs.getBoolean("onboarding", false))
    val level = mutableIntStateOf(prefs.getInt("level", 1))
    val coins = mutableIntStateOf(prefs.getInt("coins", 300))
    val boosters = mutableStateListOf<Int>().apply {
        Booster.entries.forEach { add(prefs.getInt("booster_${it.name}", 2)) }
    }
    val skin = mutableIntStateOf(prefs.getInt("skin", 0))
    val ownedSkins = mutableStateOf(decodeSet(prefs.getString("owned_skins", "0")!!))
    /** Highest world (theme index) whose "new world" card the player has seen. */
    val seenTheme = mutableIntStateOf(prefs.getInt("theme_seen", Themes.indexForLevel(prefs.getInt("level", 1))))
    /** Coins returned for themes bought before themes became level unlocks; shown once on the map. */
    val themeRefund = mutableIntStateOf(prefs.getInt("theme_refund", 0))
    val musicOn = mutableStateOf(prefs.getBoolean("music", true))
    val sfxOn = mutableStateOf(prefs.getBoolean("sfx", true))
    val hapticsOn = mutableStateOf(prefs.getBoolean("haptics", true))
    val playerName = mutableStateOf(prefs.getString("name", "Marble Master")!!)
    val avatar = mutableIntStateOf(prefs.getInt("avatar", 0))

    // notifications & privacy (kept across "Reset Progress")
    val pushOn = mutableStateOf(prefs.getBoolean(KEY_PUSH, true))
    val shareData = mutableStateOf(prefs.getBoolean(KEY_SHARE_DATA, true))
    /** Whether we already showed the Android 13+ notification permission prompt once. */
    val notifPermissionAsked = mutableStateOf(prefs.getBoolean("notif_asked", false))

    private val starChars: CharArray = (prefs.getString("stars", null) ?: "").padEnd(LevelGenerator.MAX_LEVEL + 1, '0').toCharArray()
    val starsVersion = mutableIntStateOf(0)

    val stats = mutableStateListOf<Long>().apply { Stat.entries.forEach { add(prefs.getLong("stat_${it.name}", 0)) } }
    val claimedAchievements = mutableStateOf(decodeSet(prefs.getString("ach", "")!!))
    val openedChests = mutableStateOf(decodeSet(prefs.getString("chests", "")!!))
    /** Levels unlocked early by watching a rewarded ad. */
    val adUnlockedLevels = mutableStateOf(decodeSet(prefs.getString("ad_levels", "")!!))

    /**
     * Special-block features already taught (by ordinal). The first time this version runs, features
     * whose intro level the player has already passed count as seen, so nobody is re-taught.
     */
    val seenFeatures = mutableStateOf(
        prefs.getString("seen_features", null)?.let { decodeSet(it) }
            ?: Feature.entries.filter { it.unlockLevel < prefs.getInt("level", 1) }.map { it.ordinal }.toSet(),
    )

    fun markFeaturesSeen(fs: Collection<Feature>) {
        seenFeatures.value = seenFeatures.value + fs.map { it.ordinal }
        prefs.edit().putString("seen_features", encodeSet(seenFeatures.value)).apply()
    }

    init { migrateBoughtThemes() }

    /**
     * Themes used to be bought (or unlocked with an ad) and picked by hand. Now each world unlocks at a
     * level, so the old `theme` pick and `owned_themes` set are dropped once, and the coins paid for
     * them are given back. Ad-granted themes can't be told apart from bought ones, so they count too.
     */
    private fun migrateBoughtThemes() {
        // pin the starting point so worlds already reached never show the "new world" card
        if (!prefs.contains("theme_seen")) prefs.edit().putInt("theme_seen", seenTheme.intValue).apply()
        if (!prefs.contains("owned_themes") && !prefs.contains("theme")) return
        val owned = decodeSet(prefs.getString("owned_themes", "") ?: "")
        val refund = owned.sumOf { Catalog.legacyThemePrices[it] ?: 0 }
        val e = prefs.edit().remove("owned_themes").remove("theme")
        if (refund > 0) {
            coins.intValue += refund
            themeRefund.intValue += refund
            e.putInt("coins", coins.intValue).putInt("theme_refund", themeRefund.intValue)
        }
        e.apply()
    }

    fun clearThemeRefund() { themeRefund.intValue = 0; prefs.edit().remove("theme_refund").apply() }

    // daily
    val lastSpinDay = mutableStateOf(prefs.getLong("spin_day", -1))
    val loginStreak = mutableIntStateOf(prefs.getInt("login_streak", 0))
    val lastLoginClaimDay = mutableStateOf(prefs.getLong("login_day", -1))
    val dailyWinDay = mutableStateOf(prefs.getLong("daily_win", -1))
    val freeGiftAt = mutableStateOf(prefs.getLong("free_gift_at", 0))

    val today: Long get() = LocalDate.now().toEpochDay()

    private fun decodeSet(s: String): Set<Int> = s.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    private fun encodeSet(s: Set<Int>) = s.joinToString(",")

    fun finishOnboarding() { onboardingDone.value = true; prefs.edit().putBoolean("onboarding", true).apply() }

    fun starsFor(n: Int): Int = if (n in 1..LevelGenerator.MAX_LEVEL) starChars[n] - '0' else 0
    val totalStars: Int get() { starsVersion.intValue; return starChars.sumOf { it - '0' } }

    fun recordWin(n: Int, stars: Int) {
        if (stars > starsFor(n)) {
            starChars[n] = ('0' + stars)
            prefs.edit().putString("stars", String(starChars)).apply()
            starsVersion.intValue++
        }
        if (n == level.intValue && n < LevelGenerator.MAX_LEVEL) {
            level.intValue = n + 1
            prefs.edit().putInt("level", n + 1).apply()
            Analytics.progress(n + 1)
        }
    }

    fun isLevelPlayable(n: Int) = n <= level.intValue || n in adUnlockedLevels.value

    fun unlockLevelByAd(n: Int) {
        adUnlockedLevels.value = adUnlockedLevels.value + n
        prefs.edit().putString("ad_levels", encodeSet(adUnlockedLevels.value)).apply()
    }

    /** Grants a skin without spending coins (rewarded ad). */
    fun grantSkin(i: Int) {
        ownedSkins.value = ownedSkins.value + i
        prefs.edit().putString("owned_skins", encodeSet(ownedSkins.value)).apply()
    }

    fun addCoins(amount: Int) {
        coins.intValue = (coins.intValue + amount).coerceAtLeast(0)
        prefs.edit().putInt("coins", coins.intValue).apply()
        if (amount > 0) addStat(Stat.COINS_EARNED, amount.toLong())
    }

    fun spend(amount: Int): Boolean {
        if (coins.intValue < amount) return false
        addCoins(-amount)
        return true
    }

    fun boosterCount(b: Booster) = boosters[b.ordinal]
    fun addBooster(b: Booster, n: Int) {
        boosters[b.ordinal] = (boosters[b.ordinal] + n).coerceAtLeast(0)
        prefs.edit().putInt("booster_${b.name}", boosters[b.ordinal]).apply()
    }

    fun addStat(s: Stat, v: Long) {
        stats[s.ordinal] = stats[s.ordinal] + v
        prefs.edit().putLong("stat_${s.name}", stats[s.ordinal]).apply()
    }
    fun maxStat(s: Stat, v: Long) {
        if (v > stats[s.ordinal]) { stats[s.ordinal] = v; prefs.edit().putLong("stat_${s.name}", v).apply() }
    }
    fun stat(s: Stat) = stats[s.ordinal]

    fun selectSkin(i: Int) { skin.intValue = i; prefs.edit().putInt("skin", i).apply() }
    fun buySkin(i: Int): Boolean {
        if (i in ownedSkins.value) return true
        if (!spend(Catalog.skins[i].price)) return false
        Analytics.spend("skin_$i", Catalog.skins[i].price)
        ownedSkins.value = ownedSkins.value + i
        prefs.edit().putString("owned_skins", encodeSet(ownedSkins.value)).apply()
        return true
    }
    /** Worlds unlock by progress: the world of the highest level the player has reached. */
    val unlockedTheme: Int get() = Themes.indexForLevel(level.intValue)
    fun isThemeUnlocked(i: Int) = i <= unlockedTheme

    /**
     * Theme used for level [n]: its own world, but never one the player hasn't reached yet (daily
     * challenges and ad-unlocked levels can be far ahead), so worlds are only ever revealed by progress.
     */
    fun themeForLevel(n: Int): Int = Themes.indexForLevel(minOf(n, level.intValue))

    fun markThemeSeen(i: Int) {
        if (i <= seenTheme.intValue) return
        seenTheme.intValue = i
        prefs.edit().putInt("theme_seen", i).apply()
    }

    fun setMusic(on: Boolean) { musicOn.value = on; prefs.edit().putBoolean("music", on).apply() }
    fun setSfx(on: Boolean) { sfxOn.value = on; prefs.edit().putBoolean("sfx", on).apply() }
    fun setHaptics(on: Boolean) { hapticsOn.value = on; prefs.edit().putBoolean("haptics", on).apply() }
    fun setName(n: String) { playerName.value = n; prefs.edit().putString("name", n).apply() }
    fun setAvatar(a: Int) { avatar.intValue = a; prefs.edit().putInt("avatar", a).apply() }
    fun setPushOn(on: Boolean) { pushOn.value = on; prefs.edit().putBoolean(KEY_PUSH, on).apply() }
    fun setShareData(on: Boolean) { shareData.value = on; prefs.edit().putBoolean(KEY_SHARE_DATA, on).apply() }
    fun markNotifPermissionAsked() { notifPermissionAsked.value = true; prefs.edit().putBoolean("notif_asked", true).apply() }

    fun claimAchievement(id: Int, reward: Int) {
        if (id in claimedAchievements.value) return
        claimedAchievements.value = claimedAchievements.value + id
        prefs.edit().putString("ach", encodeSet(claimedAchievements.value)).apply()
        addCoins(reward)
        Analytics.reward("achievement_$id", reward)
    }

    fun openChest(index: Int) {
        openedChests.value = openedChests.value + index
        prefs.edit().putString("chests", encodeSet(openedChests.value)).apply()
        addStat(Stat.CHESTS, 1)
        Analytics.reward("chest", index)
    }

    // ---- daily
    val canSpin: Boolean get() = lastSpinDay.value != today
    fun markSpin() { lastSpinDay.value = today; prefs.edit().putLong("spin_day", today).apply(); addStat(Stat.SPINS, 1); Analytics.reward("free_spin") }

    /** Streak index (0..6) of the next login reward, resetting when a day was missed. */
    val loginIndex: Int get() {
        val last = lastLoginClaimDay.value
        return if (last == today - 1 || last == today) loginStreak.intValue % 7 else 0
    }
    val canClaimLogin: Boolean get() = lastLoginClaimDay.value != today
    fun claimLogin(): Int {
        val idx = if (canClaimLogin) loginIndex else return 0
        val reward = LOGIN_REWARDS[idx]
        val streakBase = if (lastLoginClaimDay.value == today - 1) loginStreak.intValue else 0
        loginStreak.intValue = streakBase + 1
        lastLoginClaimDay.value = today
        prefs.edit().putInt("login_streak", loginStreak.intValue).putLong("login_day", today).apply()
        addCoins(reward)
        if (idx == 6) Booster.entries.forEach { addBooster(it, 1) }
        Analytics.reward("daily_login_day_${idx + 1}", reward)
        return reward
    }

    val dailyChallengeDone: Boolean get() = dailyWinDay.value == today
    fun markDailyWin() { dailyWinDay.value = today; prefs.edit().putLong("daily_win", today).apply(); addStat(Stat.DAILY_WINS, 1) }
    /** Today's challenge maps to a deterministic level well ahead of the player. */
    val dailyLevel: Int get() = ((today * 37 + 11) % 3000 + 100).toInt()

    fun claimFreeGift(now: Long): Boolean {
        if (now < freeGiftAt.value) return false
        freeGiftAt.value = now + FREE_GIFT_COOLDOWN
        prefs.edit().putLong("free_gift_at", freeGiftAt.value).apply()
        addCoins(100)
        Analytics.reward("free_gift", 100)
        return true
    }

    fun resetProgress() {
        prefs.edit().clear().putBoolean("onboarding", true)
            .putBoolean(KEY_PUSH, pushOn.value)
            .putBoolean(KEY_SHARE_DATA, shareData.value).putBoolean("notif_asked", notifPermissionAsked.value)
            .apply()
        level.intValue = 1; coins.intValue = 300
        Booster.entries.forEach { boosters[it.ordinal] = 2 }
        java.util.Arrays.fill(starChars, '0'); starsVersion.intValue++
        Stat.entries.forEach { stats[it.ordinal] = 0 }
        skin.intValue = 0; ownedSkins.value = setOf(0); seenTheme.intValue = 0; themeRefund.intValue = 0
        claimedAchievements.value = emptySet(); openedChests.value = emptySet(); adUnlockedLevels.value = emptySet()
        lastSpinDay.value = -1; loginStreak.intValue = 0; lastLoginClaimDay.value = -1; dailyWinDay.value = -1; freeGiftAt.value = 0
        musicOn.value = true; sfxOn.value = true; hapticsOn.value = true
    }

    companion object {
        const val PREFS_NAME = "marble_pop"
        const val KEY_PUSH = "push_on"
        const val KEY_SHARE_DATA = "share_data"

        fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val LOGIN_REWARDS = intArrayOf(50, 75, 100, 125, 150, 200, 400)
        const val FREE_GIFT_COOLDOWN = 4 * 60 * 60 * 1000L
    }
}
