package com.puzzel.lab.marbelshort.level

/** Marble / block colour palette (ARGB). Index is the colour id used everywhere. */
object MarbleColors {
    const val RAINBOW = 99
    val argb = longArrayOf(
        0xFFFF4D5E, // 0 red
        0xFFFFD23F, // 1 yellow
        0xFF33C8F5, // 2 sky
        0xFF3CCB5A, // 3 green
        0xFF9B5CFF, // 4 purple
        0xFFFF9F1C, // 5 orange
        0xFFFF7AC8, // 6 pink
        0xFF3D6BFF, // 7 blue
        0xFFEFF3FA, // 8 white
        0xFF9C5A2E, // 9 brown
        0xFF1F7A4D, // 10 dark green
        0xFF4A4E69, // 11 charcoal
    ).map { it.toInt() }.toIntArray()
    val names = arrayOf("Red", "Yellow", "Sky", "Green", "Purple", "Orange", "Pink", "Blue", "White", "Brown", "Forest", "Charcoal")
    const val COUNT = 12
}

enum class BlockKind {
    NORMAL, MYSTERY, COUNTER, KEY, CHAINED, CRATE, ICE, ARROW, DOUBLE, RAINBOW, BOMB, PAINT, GRAY, SWITCH
}

/** Mechanics that unlock over the level curve. */
enum class Feature(val unlockLevel: Int, val title: String, val description: String) {
    MYSTERY(6, "Mystery Block", "Its colour is revealed as soon as it has a way out."),
    COUNTER(8, "Caged Block", "The cage opens after you pop the shown number of blocks."),
    HIDDEN_CUP(11, "Mystery Cup", "Covered cups reveal their colour when they reach the top."),
    KEY(15, "Key & Chains", "Pop the key block to break every chain."),
    ARROW(20, "Arrow Block", "Can also slide out in its arrow direction when that way is clear."),
    CRATE(25, "Crate", "Opens after the shown number of cups are filled."),
    DOUBLE(30, "Double Block", "Holds twice the marbles. Make room on the belt!"),
    ICE(40, "Ice Block", "Tap once to crack the ice, tap again to pop it."),
    RAINBOW(55, "Rainbow Block", "Rainbow marbles fill any cup that still needs them."),
    BOMB(70, "Bomb Block", "Pop it before its move timer runs out!"),
    PAINT(90, "Paint Bucket", "Pop the bucket to paint the grey blocks with colour."),
    SWITCH(120, "Belt Switch", "Popping it reverses the belt direction.");

    companion object {
        fun introducedAt(level: Int): Feature? = entries.firstOrNull { it.unlockLevel == level }
        fun nextAfter(level: Int): Feature? = entries.firstOrNull { it.unlockLevel > level }
    }
}

/**
 * @param param meaning depends on kind: COUNTER pops needed, CRATE cups needed, BOMB move limit,
 * ARROW direction (0 left, 1 right, 2 up), KEY/CHAINED lock id, PAINT/GRAY bucket id.
 */
data class BlockSpec(
    val id: Int,
    val color: Int,
    val kind: BlockKind = BlockKind.NORMAL,
    val param: Int = 0,
) {
    val marbleCount: Int get() = if (kind == BlockKind.DOUBLE) 18 else 9
    val marbleColor: Int get() = if (kind == BlockKind.RAINBOW) MarbleColors.RAINBOW else color
}

data class CupSpec(val color: Int, val hidden: Boolean = false)

/**
 * Grid cells are indexed `row * width + col`, row 0 being the bottom row that opens into the hopper.
 * Blocks never fall: a block can be tapped when it has a path of empty cells to the bottom exit.
 */
class LevelSpec(
    val number: Int,
    val width: Int,
    val height: Int,
    /** Active (playable) cells of the board shape. */
    val mask: BooleanArray,
    /** Block per cell, null = empty slot. */
    val cells: Array<BlockSpec?>,
    /** Tray columns, top cup first. */
    val tray: List<List<CupSpec>>,
    val capacity: Int,
    val theme: Int,
    val colorCount: Int,
    val isBoss: Boolean,
    /** Planned (verified) tap order of block ids — includes the double tap of ice blocks. */
    val plan: List<Int>,
    val introduces: Feature?,
) {
    val maxHeight: Int get() = height
    val blocks: List<BlockSpec> get() = cells.filterNotNull()
    val blockCount: Int get() = cells.count { it != null }
    fun active(c: Int, r: Int) = c in 0 until width && r in 0 until height && mask[r * width + c]
}
