package com.ngstudio.marblepop.render

class GameTheme(
    val name: String,
    val bgTop: Int, val bgBottom: Int,
    val bubble: Int,
    val wall: Int, val wallDark: Int, val wallLight: Int,
    val glassTop: Int, val glassBottom: Int,
    val interior: Int, val slot: Int,
    val funnel: Int, val funnelDark: Int,
    val dish: Int, val dishDark: Int,
    val groove: Int, val rail: Int, val accent: Int,
    /** Sky behind the machine: top, mid, bottom, glowA, glowB, silhouette (also the home map sky). */
    val sky: IntArray = intArrayOf(),
    /** First level that uses this world. The world stays until the next world's level. */
    val unlockLevel: Int = 1,
    /** Map scenery props: 0 candy, 1 ocean, 2 space, 3 desert crystals, 4 forest, 5 snow. */
    val scenery: Int = 0,
)

/**
 * The worlds, in unlock order. A level uses the world with the highest [GameTheme.unlockLevel] at or
 * below its number. Early worlds come every 40 levels so new players see the look change often; the
 * gaps then widen with the difficulty curve (hard from level 31, long late game up to level 5000).
 */
object Themes {
    private fun c(v: Long) = v.toInt()
    val all = listOf(
        GameTheme(
            "Candy Lab", c(0xFF9C7DFF), c(0xFF6440E6), c(0x33FFFFFF),
            c(0xFF8E6EF2), c(0xFF5534C4), c(0xFFC0ADFF),
            c(0xFFE4DBFF), c(0xFFCFC2FA), c(0xFFF1EDFF), c(0xFFDCD2FA),
            c(0xFFEDE8FC), c(0xFFB7A7EA), c(0xFFF6F3FE), c(0xFFC6B9EE),
            c(0xFF3B3557), c(0xFFFFFFFF), c(0xFFFF5FA2),
            intArrayOf(c(0xFF5A3BEF), c(0xFF8A52F5), c(0xFFE77BD8), c(0xFFFF9AD5), c(0xFF6FD3FF), c(0xFF3B1FA6)),
            unlockLevel = 1, scenery = 0,
        ),
        GameTheme(
            "Ocean", c(0xFF63C9FF), c(0xFF2F6FE0), c(0x33FFFFFF),
            c(0xFF4E92EC), c(0xFF2B5DB6), c(0xFFA3CDFF),
            c(0xFFDDF0FF), c(0xFFC2E0FA), c(0xFFEAF6FF), c(0xFFD0E6FA),
            c(0xFFE6F1FB), c(0xFFA2C2E6), c(0xFFF2F8FE), c(0xFFB5CFEA),
            c(0xFF2F3B57), c(0xFFFFFFFF), c(0xFF33D6F5),
            intArrayOf(c(0xFF1F5FD6), c(0xFF2E9BEA), c(0xFF5FD9E6), c(0xFF9AF0FF), c(0xFF7DFFC8), c(0xFF15489E)),
            unlockLevel = 40, scenery = 1,
        ),
        GameTheme(
            "Forest", c(0xFF86DB7E), c(0xFF2D9460), c(0x33FFFFFF),
            c(0xFF5DB86E), c(0xFF2F7F48), c(0xFFADE6A6),
            c(0xFFE3F7DD), c(0xFFC9EBC1), c(0xFFF0FAEC), c(0xFFD5EECD),
            c(0xFFEAF6E5), c(0xFFA9D59E), c(0xFFF5FBF2), c(0xFFB9DDB0),
            c(0xFF2F3D33), c(0xFFFFFFFF), c(0xFFFFC93C),
            intArrayOf(c(0xFF3CC88A), c(0xFF1FA27A), c(0xFF137A66), c(0xFFC8FF8A), c(0xFF7DF0D0), c(0xFF0C5A4B)),
            unlockLevel = 80, scenery = 4,
        ),
        GameTheme(
            "Sunset", c(0xFFFF9466), c(0xFFC6344C), c(0x33FFFFFF),
            c(0xFFE45A6C), c(0xFFA8303F), c(0xFFFFA6AF),
            c(0xFFFFE2DD), c(0xFFFACAC4), c(0xFFFFF0EC), c(0xFFF8D6D0),
            c(0xFFFDEDEA), c(0xFFE7AFA8), c(0xFFFFF6F4), c(0xFFEDBEB8),
            c(0xFF4A2F38), c(0xFFFFFFFF), c(0xFFFFC93C),
            intArrayOf(c(0xFFFF8A5C), c(0xFFF2557A), c(0xFF9C3FB5), c(0xFFFFD36E), c(0xFFFF9AD5), c(0xFF6E2380)),
            unlockLevel = 120, scenery = 3,
        ),
        GameTheme(
            "Bubblegum Bay", c(0xFFFF8FC8), c(0xFFE0468F), c(0x33FFFFFF),
            c(0xFFF0609F), c(0xFFB8336E), c(0xFFFFB3D3),
            c(0xFFFFE0EE), c(0xFFFAC8DE), c(0xFFFFF0F6), c(0xFFF9D3E4),
            c(0xFFFDEAF2), c(0xFFE9AECA), c(0xFFFFF5F9), c(0xFFEFBCD3),
            c(0xFF4D2A3F), c(0xFFFFFFFF), c(0xFF33D6F5),
            intArrayOf(c(0xFFFF5FA8), c(0xFFFF8C9E), c(0xFFFFC09A), c(0xFFFFE7A0), c(0xFFB9F3FF), c(0xFFA8326A)),
            unlockLevel = 180, scenery = 0,
        ),
        GameTheme(
            "Space", c(0xFF3A2D86), c(0xFF120C35), c(0x22FFFFFF),
            c(0xFF43389A), c(0xFF241C63), c(0xFF7A6FD6),
            c(0xFFD6D0FA), c(0xFFB8AEEE), c(0xFFE7E3FF), c(0xFFCAC2F2),
            c(0xFFE0DBF8), c(0xFF9E94D6), c(0xFFEAE6FB), c(0xFFA79DDB),
            c(0xFF231E3D), c(0xFFFFFFFF), c(0xFFFFC93C),
            intArrayOf(c(0xFF140A3F), c(0xFF2D1B7A), c(0xFF6A2EB8), c(0xFFB57BFF), c(0xFF5FA8FF), c(0xFF0D0630)),
            unlockLevel = 250, scenery = 2,
        ),
        GameTheme(
            "Frosty Peaks", c(0xFFB9ECFF), c(0xFF5FA8E8), c(0x33FFFFFF),
            c(0xFF6FB8E6), c(0xFF3A7DB5), c(0xFFC9ECFF),
            c(0xFFE8F8FF), c(0xFFD2EEFC), c(0xFFF2FBFF), c(0xFFDCF1FB),
            c(0xFFEDF7FC), c(0xFFA9D3EA), c(0xFFF7FCFF), c(0xFFBCDDEE),
            c(0xFF2A3E57), c(0xFFFFFFFF), c(0xFFB57BFF),
            intArrayOf(c(0xFF4F8FE0), c(0xFF7FB8F0), c(0xFFBFDDF8), c(0xFFE6F4FF), c(0xFFD8C2FF), c(0xFF3F64A0)),
            unlockLevel = 350, scenery = 5,
        ),
        GameTheme(
            "Honey Hive", c(0xFFFFD25E), c(0xFFE58A1F), c(0x33FFFFFF),
            c(0xFFF59A23), c(0xFFB8620C), c(0xFFFFD07A),
            c(0xFFFFF3D1), c(0xFFFCE6B0), c(0xFFFFF8E6), c(0xFFF9EBC4),
            c(0xFFFDF3DA), c(0xFFE8C88A), c(0xFFFFFAEE), c(0xFFEFD39E),
            c(0xFF4D3A1F), c(0xFFFFFFFF), c(0xFFFF5FA2),
            intArrayOf(c(0xFFFFC23D), c(0xFFFFA236), c(0xFFF2783E), c(0xFFFFF3A0), c(0xFFFFD0E0), c(0xFF8E4410)),
            unlockLevel = 500, scenery = 4,
        ),
        GameTheme(
            "Jelly Lagoon", c(0xFF5EEBD6), c(0xFF14A7A0), c(0x33FFFFFF),
            c(0xFF2CC4B4), c(0xFF13857E), c(0xFF9BF0E2),
            c(0xFFDDFBF5), c(0xFFC0F0E7), c(0xFFEBFDF9), c(0xFFCDF3EC),
            c(0xFFE5F8F4), c(0xFF9DD9CE), c(0xFFF2FCFA), c(0xFFB0E2D8),
            c(0xFF1F3F3D), c(0xFFFFFFFF), c(0xFFFFC93C),
            intArrayOf(c(0xFF0FA3B1), c(0xFF2EC7B8), c(0xFF8EEBC6), c(0xFFE6FF9A), c(0xFF9AE8FF), c(0xFF0A5E6B)),
            unlockLevel = 700, scenery = 1,
        ),
        GameTheme(
            "Choco Falls", c(0xFFC48A5C), c(0xFF6E3B22), c(0x33FFFFFF),
            c(0xFFA0603A), c(0xFF6B3A1E), c(0xFFE3B48A),
            c(0xFFFAEBDD), c(0xFFF0D6BE), c(0xFFFDF4EB), c(0xFFF2DECB),
            c(0xFFF8ECE0), c(0xFFD9B898), c(0xFFFFF8F1), c(0xFFE2C6AA),
            c(0xFF3A2418), c(0xFFFFFFFF), c(0xFFFF7BBE),
            intArrayOf(c(0xFF5A2E1C), c(0xFF8C4F2E), c(0xFFD8955E), c(0xFFFFD9A0), c(0xFFFF9AC8), c(0xFF3A1C10)),
            unlockLevel = 1000, scenery = 0,
        ),
        GameTheme(
            "Neon Arcade", c(0xFF2B1B5E), c(0xFF0B0A24), c(0x22FFFFFF),
            c(0xFFE040C8), c(0xFF8A1E86), c(0xFFFF9BEA),
            c(0xFFF8E3FA), c(0xFFEFC9F2), c(0xFFFCF0FD), c(0xFFF1D3F4),
            c(0xFFF7E6F8), c(0xFFD6A3DA), c(0xFFFDF4FD), c(0xFFE2B6E6),
            c(0xFF1E1538), c(0xFFFFFFFF), c(0xFF33F5E0),
            intArrayOf(c(0xFF07102E), c(0xFF162C6E), c(0xFF8A1C8C), c(0xFFFF3DCB), c(0xFF2EF0FF), c(0xFF040820)),
            unlockLevel = 1400, scenery = 2,
        ),
        GameTheme(
            "Gear Works", c(0xFF9AA8C4), c(0xFF4A5570), c(0x33FFFFFF),
            c(0xFF7D8BA8), c(0xFF4A5570), c(0xFFC4CDE0),
            c(0xFFE9EDF5), c(0xFFD3DAE8), c(0xFFF3F5FA), c(0xFFDDE2EE),
            c(0xFFECEFF5), c(0xFFAEB8CC), c(0xFFF6F8FB), c(0xFFC0C8D8),
            c(0xFF2A3042), c(0xFFFFFFFF), c(0xFFFF9F1C),
            intArrayOf(c(0xFF2E3A55), c(0xFF55678C), c(0xFFF29A52), c(0xFFFFC46B), c(0xFF7DE3FF), c(0xFF1C2438)),
            unlockLevel = 2000, scenery = 3,
        ),
        GameTheme(
            "Volcano Forge", c(0xFFFF6A3D), c(0xFF7A1420), c(0x33FFFFFF),
            c(0xFFC2361E), c(0xFF6E1210), c(0xFFFF9466),
            c(0xFFFFE6DE), c(0xFFFACDC0), c(0xFFFFF2ED), c(0xFFF7D5CB),
            c(0xFFFDEBE5), c(0xFFE6ADA0), c(0xFFFFF6F3), c(0xFFECBCAF),
            c(0xFF3D1A1A), c(0xFFFFFFFF), c(0xFFFFC93C),
            intArrayOf(c(0xFF1E0A12), c(0xFF5A1420), c(0xFFD9441F), c(0xFFFFB03A), c(0xFFFF6A3D), c(0xFF12060A)),
            unlockLevel = 2800, scenery = 3,
        ),
        GameTheme(
            "Royal Galaxy", c(0xFF2E3F9E), c(0xFF0E1440), c(0x22FFFFFF),
            c(0xFFF2BE30), c(0xFFAD7410), c(0xFFFFE08A),
            c(0xFFFFF5D6), c(0xFFFAE7B5), c(0xFFFFFAEA), c(0xFFF6EBC8),
            c(0xFFFDF5DE), c(0xFFE6CE8E), c(0xFFFFFBEF), c(0xFFEDD9A2),
            c(0xFF1E2440), c(0xFFFFFFFF), c(0xFF7DB8FF),
            intArrayOf(c(0xFF0A0F33), c(0xFF1B2A78), c(0xFF3C4FB8), c(0xFFFFD86E), c(0xFFB57BFF), c(0xFF060A24)),
            unlockLevel = 3800, scenery = 2,
        ),
    )

    fun get(i: Int) = all[i.coerceIn(0, all.size - 1)]

    /** Index of the world level [n] belongs to: the highest unlock level at or below [n]. */
    fun indexForLevel(n: Int): Int = all.indexOfLast { it.unlockLevel <= n }.coerceAtLeast(0)

    /** Index of the world that starts exactly at level [n], or -1. */
    fun startingAt(n: Int): Int = all.indexOfFirst { it.unlockLevel == n }
}
