package com.puzzel.lab.marbelshort.render

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
    /** Sky behind the machine: top, mid, bottom, glowA, glowB, silhouette (same as ui.theme.Skies). */
    val sky: IntArray = intArrayOf(),
)

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
        ),
        GameTheme(
            "Ocean", c(0xFF63C9FF), c(0xFF2F6FE0), c(0x33FFFFFF),
            c(0xFF4E92EC), c(0xFF2B5DB6), c(0xFFA3CDFF),
            c(0xFFDDF0FF), c(0xFFC2E0FA), c(0xFFEAF6FF), c(0xFFD0E6FA),
            c(0xFFE6F1FB), c(0xFFA2C2E6), c(0xFFF2F8FE), c(0xFFB5CFEA),
            c(0xFF2F3B57), c(0xFFFFFFFF), c(0xFF33D6F5),
            intArrayOf(c(0xFF1F5FD6), c(0xFF2E9BEA), c(0xFF5FD9E6), c(0xFF9AF0FF), c(0xFF7DFFC8), c(0xFF15489E)),
        ),
        GameTheme(
            "Space", c(0xFF3A2D86), c(0xFF120C35), c(0x22FFFFFF),
            c(0xFF43389A), c(0xFF241C63), c(0xFF7A6FD6),
            c(0xFFD6D0FA), c(0xFFB8AEEE), c(0xFFE7E3FF), c(0xFFCAC2F2),
            c(0xFFE0DBF8), c(0xFF9E94D6), c(0xFFEAE6FB), c(0xFFA79DDB),
            c(0xFF231E3D), c(0xFFFFFFFF), c(0xFFFFC93C),
            intArrayOf(c(0xFF140A3F), c(0xFF2D1B7A), c(0xFF6A2EB8), c(0xFFB57BFF), c(0xFF5FA8FF), c(0xFF0D0630)),
        ),
        GameTheme(
            "Lava", c(0xFFFF9466), c(0xFFC6344C), c(0x33FFFFFF),
            c(0xFFE45A6C), c(0xFFA8303F), c(0xFFFFA6AF),
            c(0xFFFFE2DD), c(0xFFFACAC4), c(0xFFFFF0EC), c(0xFFF8D6D0),
            c(0xFFFDEDEA), c(0xFFE7AFA8), c(0xFFFFF6F4), c(0xFFEDBEB8),
            c(0xFF4A2F38), c(0xFFFFFFFF), c(0xFFFFC93C),
            intArrayOf(c(0xFFFF8A5C), c(0xFFF2557A), c(0xFF9C3FB5), c(0xFFFFD36E), c(0xFFFF9AD5), c(0xFF6E2380)),
        ),
        GameTheme(
            "Forest", c(0xFF86DB7E), c(0xFF2D9460), c(0x33FFFFFF),
            c(0xFF5DB86E), c(0xFF2F7F48), c(0xFFADE6A6),
            c(0xFFE3F7DD), c(0xFFC9EBC1), c(0xFFF0FAEC), c(0xFFD5EECD),
            c(0xFFEAF6E5), c(0xFFA9D59E), c(0xFFF5FBF2), c(0xFFB9DDB0),
            c(0xFF2F3D33), c(0xFFFFFFFF), c(0xFFFFC93C),
            intArrayOf(c(0xFF3CC88A), c(0xFF1FA27A), c(0xFF137A66), c(0xFFC8FF8A), c(0xFF7DF0D0), c(0xFF0C5A4B)),
        ),
    )

    fun get(i: Int) = all[i.mod(all.size)]
}
