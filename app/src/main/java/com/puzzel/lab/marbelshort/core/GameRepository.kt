package com.puzzel.lab.marbelshort.core

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.puzzel.lab.marbelshort.level.LevelGenerator
import java.time.LocalDate

enum class Booster(val title: String, val price: Int, val description: String) {
    HAMMER("Hammer", 150, "Smash any block, even locked ones."),
    MAGNET("Magnet", 200, "Pull marbles of one colour straight into their cups."),
    SLOTS("+6 Slots", 120, "Adds 6 extra slots to the belt."),
    SHUFFLE("Shuffle", 100, "Shuffles the colours of the waiting cups."),
}

enum class Stat { LEVELS_WON, THREE_STARS, MARBLES, CUPS, BLOCKS, MAX_COMBO, BOOSTERS, SPINS, DAILY_WINS, COINS_EARNED, CHESTS, BOSS_WINS }

data class Skin(val name: String, val price: Int)
data class ThemeInfo(val name: String, val price: Int)

object Catalog {
    val skins = listOf(
        Skin("Glossy", 0), Skin("Glass", 600), Skin("Candy", 900), Skin("Galaxy", 1500),
        Skin("Metal", 1200), Skin("Neon", 1800), Skin("Smiley", 2500),
    )
    val themes = listOf(
        ThemeInfo("Candy Lab", 0), ThemeInfo("Ocean", 800), ThemeInfo("Space", 1500),
        ThemeInfo("Lava", 1200), ThemeInfo("Forest", 1000),
    )
}

/** All persistent player data, exposed as Compose state. */
class GameRepository(context: Context) {
    private val prefs = context.getSharedPreferences("marble_pop", Context.MODE_PRIVATE)

    val onboardingDone = mutableStateOf(prefs.getBoolean("onboarding", false))
    val level = mutableIntStateOf(prefs.getInt("level", 1))
    val coins = mutableIntStateOf(prefs.getInt("coins", 300))
    val boosters = mutableStateListOf<Int>().apply {
        Booster.entries.forEach { add(prefs.getInt("booster_${it.name}", 2)) }
    }
    val skin = mutableIntStateOf(prefs.getInt("skin", 0))
    val ownedSkins = mutableStateOf(decodeSet(prefs.getString("owned_skins", "0")!!))
    val theme = mutableIntStateOf(prefs.getInt("theme", -1)) // -1 = auto rotate
    val ownedThemes = mutableStateOf(decodeSet(prefs.getString("owned_themes", "0")!!))
    val musicOn = mutableStateOf(prefs.getBoolean("music", true))
    val sfxOn = mutableStateOf(prefs.getBoolean("sfx", true))
    val hapticsOn = mutableStateOf(prefs.getBoolean("haptics", true))
    val playerName = mutableStateOf(prefs.getString("name", "Marble Master")!!)
    val avatar = mutableIntStateOf(prefs.getInt("avatar", 0))

    private val starChars: CharArray = (prefs.getString("stars", null) ?: "").padEnd(LevelGenerator.MAX_LEVEL + 1, '0').toCharArray()
    val starsVersion = mutableIntStateOf(0)

    val stats = mutableStateListOf<Long>().apply { Stat.entries.forEach { add(prefs.getLong("stat_${it.name}", 0)) } }
    val claimedAchievements = mutableStateOf(decodeSet(prefs.getString("ach", "")!!))
    val openedChests = mutableStateOf(decodeSet(prefs.getString("chests", "")!!))
    /** Levels unlocked early by watching a rewarded ad. */
    val adUnlockedLevels = mutableStateOf(decodeSet(prefs.getString("ad_levels", "")!!))

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

    /** Grants a theme without spending coins (rewarded ad). */
    fun grantTheme(i: Int) {
        ownedThemes.value = ownedThemes.value + i
        prefs.edit().putString("owned_themes", encodeSet(ownedThemes.value)).apply()
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
        ownedSkins.value = ownedSkins.value + i
        prefs.edit().putString("owned_skins", encodeSet(ownedSkins.value)).apply()
        return true
    }
    fun selectTheme(i: Int) { theme.intValue = i; prefs.edit().putInt("theme", i).apply() }
    fun buyTheme(i: Int): Boolean {
        if (i in ownedThemes.value) return true
        if (!spend(Catalog.themes[i].price)) return false
        ownedThemes.value = ownedThemes.value + i
        prefs.edit().putString("owned_themes", encodeSet(ownedThemes.value)).apply()
        return true
    }

    /** Theme used for a level: the player's pick, or the rotating level theme. */
    fun themeForLevel(n: Int): Int = theme.intValue.takeIf { it >= 0 } ?: LevelGenerator.themeFor(n)

    fun setMusic(on: Boolean) { musicOn.value = on; prefs.edit().putBoolean("music", on).apply() }
    fun setSfx(on: Boolean) { sfxOn.value = on; prefs.edit().putBoolean("sfx", on).apply() }
    fun setHaptics(on: Boolean) { hapticsOn.value = on; prefs.edit().putBoolean("haptics", on).apply() }
    fun setName(n: String) { playerName.value = n; prefs.edit().putString("name", n).apply() }
    fun setAvatar(a: Int) { avatar.intValue = a; prefs.edit().putInt("avatar", a).apply() }

    fun claimAchievement(id: Int, reward: Int) {
        if (id in claimedAchievements.value) return
        claimedAchievements.value = claimedAchievements.value + id
        prefs.edit().putString("ach", encodeSet(claimedAchievements.value)).apply()
        addCoins(reward)
    }

    fun openChest(index: Int) {
        openedChests.value = openedChests.value + index
        prefs.edit().putString("chests", encodeSet(openedChests.value)).apply()
        addStat(Stat.CHESTS, 1)
    }

    // ---- daily
    val canSpin: Boolean get() = lastSpinDay.value != today
    fun markSpin() { lastSpinDay.value = today; prefs.edit().putLong("spin_day", today).apply(); addStat(Stat.SPINS, 1) }

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
        return true
    }

    fun resetProgress() {
        prefs.edit().clear().putBoolean("onboarding", true).apply()
        level.intValue = 1; coins.intValue = 300
        Booster.entries.forEach { boosters[it.ordinal] = 2 }
        java.util.Arrays.fill(starChars, '0'); starsVersion.intValue++
        Stat.entries.forEach { stats[it.ordinal] = 0 }
        skin.intValue = 0; ownedSkins.value = setOf(0); theme.intValue = -1; ownedThemes.value = setOf(0)
        claimedAchievements.value = emptySet(); openedChests.value = emptySet(); adUnlockedLevels.value = emptySet()
        lastSpinDay.value = -1; loginStreak.intValue = 0; lastLoginClaimDay.value = -1; dailyWinDay.value = -1; freeGiftAt.value = 0
        musicOn.value = true; sfxOn.value = true; hapticsOn.value = true
    }

    companion object {
        val LOGIN_REWARDS = intArrayOf(50, 75, 100, 125, 150, 200, 400)
        const val FREE_GIFT_COOLDOWN = 4 * 60 * 60 * 1000L
    }
}
