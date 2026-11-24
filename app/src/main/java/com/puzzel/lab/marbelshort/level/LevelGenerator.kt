package com.puzzel.lab.marbelshort.level

import java.util.Random
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Deterministic level factory. Level N is always identical on every device.
 * Levels 1–30 follow a hand-tuned recipe table, later levels come from a difficulty curve.
 *
 * Every level is built from a planned solution: blocks are removed one by one, each only when it
 * has a free path to the exit (same rule as the game). Cups and belt capacity are derived from that
 * plan, so every level is solvable by construction (verified for all 5000 levels in unit tests).
 */
object LevelGenerator {
    const val MAX_LEVEL = 5000
    private const val MAX_CAPACITY = 48
    private const val MIN_CAPACITY = 14
    private const val MAX_COLORS = 10
    /** Levels 1..10 stay an easy tutorial. */
    private const val TUTORIAL_LEVELS = 10

    class Recipe(
        val heights: IntArray,
        val colors: Int,
        val trayCols: Int,
        val window: Int,
        val margin: Int,
        val features: Map<Feature, Int>,
        val boss: Boolean = false,
        val holes: Int = 0,
        val walls: Int = 0,
    )

    private val cache = object : LinkedHashMap<Int, LevelSpec>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, LevelSpec>?) = size > 12
    }

    @Synchronized
    fun get(n: Int): LevelSpec = cache.getOrPut(n.coerceIn(1, MAX_LEVEL)) { generate(n.coerceIn(1, MAX_LEVEL)) }

    /** World (theme index) of level [n], from the unlock table in render.Themes. */
    fun themeFor(n: Int): Int = com.puzzel.lab.marbelshort.render.Themes.indexForLevel(n)

    fun isBoss(n: Int) = n % 50 == 0

    fun generate(n: Int): LevelSpec {
        for (attempt in 0 until 14) {
            val rnd = Random(n * 7919L + 104729L * attempt + 17)
            val recipe = recipeFor(n, rnd, attempt)
            build(n, recipe, rnd)?.let { return it }
        }
        val rnd = Random(n.toLong())
        return build(n, Recipe(intArrayOf(3, 3, 3, 3), 3, 4, 0, 14, emptyMap()), rnd)!!
    }

    // ---------------------------------------------------------------- recipes

    private fun r(h: String, c: Int, w: Int, m: Int, vararg f: Pair<Feature, Int>, holes: Int = 0, k: Int = 4) =
        Recipe(h.split(",").map { it.trim().toInt() }.toIntArray(), c, k, w, m, f.toMap(), holes = holes)

    private val handmade: List<Recipe> by lazy {
        listOf(
            r("2,2,2", 2, 0, 22),
            r("2,2,2,2", 2, 0, 20),
            r("3,3,3", 3, 1, 18, holes = 1),
            r("2,3,3,2", 3, 1, 16, holes = 1),
            r("3,3,3,3", 3, 2, 14, holes = 2),
            r("3,4,4,3", 3, 2, 14, Feature.MYSTERY to 3, holes = 1),
            r("4,4,4,4", 4, 2, 13, Feature.MYSTERY to 3, holes = 2),
            r("3,3,3,3,3", 4, 2, 13, Feature.COUNTER to 2, holes = 1),
            r("4,3,4,3,4", 4, 3, 12, Feature.MYSTERY to 2, Feature.COUNTER to 1, holes = 2),
            r("4,4,4,4,4", 4, 3, 12, Feature.MYSTERY to 3, holes = 2),
            r("3,4,5,4,3", 4, 3, 12, Feature.HIDDEN_CUP to 4, holes = 2),
            r("5,4,3,4,5", 5, 3, 12, Feature.MYSTERY to 2, Feature.HIDDEN_CUP to 3, holes = 2),
            r("4,4,4,4,4", 5, 4, 11, Feature.COUNTER to 2, Feature.HIDDEN_CUP to 3, holes = 2),
            r("2,4,6,4,2", 5, 3, 12, Feature.MYSTERY to 3, Feature.COUNTER to 1, holes = 2),
            r("4,4,4,4,4", 4, 2, 13, Feature.KEY to 1, holes = 1),
            r("5,5,0,5,5", 5, 3, 12, Feature.KEY to 1, Feature.MYSTERY to 2, holes = 2),
            r("4,5,5,5,4", 5, 4, 11, Feature.HIDDEN_CUP to 4, Feature.COUNTER to 2, holes = 2),
            r("3,4,5,5,4,3", 5, 4, 11, Feature.KEY to 1, Feature.MYSTERY to 3, holes = 3),
            r("5,5,5,5,5", 6, 4, 11, Feature.COUNTER to 2, Feature.HIDDEN_CUP to 3, holes = 3),
            r("4,4,4,4,4", 5, 3, 12, Feature.ARROW to 4, holes = 1),
            r("5,3,5,3,5", 5, 4, 11, Feature.ARROW to 3, Feature.MYSTERY to 2, holes = 2),
            r("4,5,6,5,4", 6, 4, 11, Feature.KEY to 1, Feature.ARROW to 2, holes = 3),
            r("6,5,4,4,5,6", 6, 4, 10, Feature.COUNTER to 2, Feature.HIDDEN_CUP to 4, holes = 3),
            r("5,5,5,5,5,5", 6, 5, 10, Feature.MYSTERY to 4, Feature.ARROW to 2, holes = 3),
            r("4,4,4,4,4", 5, 3, 12, Feature.CRATE to 2, holes = 2),
            r("5,6,6,5", 5, 4, 11, Feature.CRATE to 1, Feature.KEY to 1, holes = 2),
            r("3,5,7,5,3", 6, 5, 10, Feature.ARROW to 2, Feature.CRATE to 1, Feature.MYSTERY to 2, holes = 3),
            r("5,5,5,5,5,5", 6, 4, 10, Feature.COUNTER to 2, Feature.HIDDEN_CUP to 4, holes = 3),
            r("6,4,6,4,6", 6, 5, 10, Feature.KEY to 1, Feature.MYSTERY to 3, Feature.CRATE to 1, holes = 3),
            r("4,5,5,5,4", 5, 3, 12, Feature.DOUBLE to 2, holes = 2),
        )
    }

    private fun recipeFor(n: Int, rnd: Random, attempt: Int): Recipe {
        if (n <= handmade.size) {
            val h = handmade[n - 1]
            if (n <= TUTORIAL_LEVELS) return h
            // 11-30 keep their hand-made boards and feature intros, but with a tight belt and mixed cups
            // ...and one more of each of their special blocks (the intro feature stays the star)
            val more = h.features.mapValues { (f, c) -> if (f == Feature.KEY) c else c + 1 }
            return Recipe(h.heights, h.colors + (if (n > 20) 1 else 0), h.trayCols, h.window + 2, if (n <= 20) 3 else 2,
                more, h.boss, h.holes, h.walls)
        }
        val boss = isBoss(n)
        val intro = Feature.introducedAt(n)
        // Difficulty is high right after the hand-made levels and close to the maximum by about level 60:
        // full-size boards, 9-10 colours, only 1-2 spare belt slots and a heavily mixed cup order.
        val base = 1.0 - exp(-(n - 30) / 15.0)
        val saw = doubleArrayOf(0.0, 0.05, 0.1, 0.15, -0.1)[n % 5]
        var d = 0.6 + 0.4 * base + saw + (if (boss) 0.15 else 0.0) + (if (n % 10 == 7) 0.06 else 0.0)
        if (intro != null) d -= 0.12
        d = (d - attempt * 0.05).coerceIn(0.05, 1.0)

        val target = (20 + d * 22 + rnd.nextInt(5) - 2).roundToInt().coerceIn(12, 42)
        val w = (sqrt(target * 0.8).roundToInt() + (if (rnd.nextDouble() < 0.3) 1 else 0)).coerceIn(4, 6)
        val h = ceil(target / w.toDouble()).toInt().coerceIn(2, 7)
        val heights = shape(rnd, w, h)
        val cellsCount = heights.sum()
        val colors = (4 + (d * 6).roundToInt() + rnd.nextInt(3) - 1).coerceIn(3, minOf(MAX_COLORS, cellsCount - 2))
        val trayCols = if (n < 45) 4 else if (rnd.nextDouble() < 0.2 + 0.3 * d) 5 else 4
        val window = (2 + d * 9).roundToInt()
        val margin = (4 - d * 2.5).roundToInt().coerceAtLeast(2) - (if (boss) 1 else 0)
        val holes = rnd.nextInt(1 + cellsCount / 12).coerceAtMost(cellsCount / 6)
        val walls = if (n > 40 && rnd.nextDouble() < 0.3 + 0.4 * d) 1 + rnd.nextInt(2) else 0

        val features = HashMap<Feature, Int>()
        val unlocked = Feature.entries.filter { it.unlockLevel <= n }
        // many special blocks and hidden cups: they hide information and lock blocks, which is what
        // makes a level hard for a thinking player (a tighter belt mostly makes it unfair)
        val maxTypes = 2 + (d * 3).roundToInt()
        for (f in unlocked.shuffled(rnd)) {
            if (features.size >= maxTypes) break
            val p = (0.6 + 0.4 * d) * weight(f)
            if (rnd.nextDouble() < p) features[f] = countFor(f, d, rnd)
        }
        if (intro != null) features[intro] = maxOf(features[intro] ?: 0, if (intro == Feature.KEY) 1 else 2)
        return Recipe(heights, colors, trayCols, window, margin, features, boss, holes, walls)
    }

    private fun weight(f: Feature) = when (f) {
        Feature.MYSTERY, Feature.HIDDEN_CUP -> 1.2
        Feature.BOMB, Feature.SWITCH, Feature.RAINBOW -> 0.6
        else -> 0.9
    }

    private fun countFor(f: Feature, d: Double, rnd: Random) = when (f) {
        Feature.KEY, Feature.PAINT -> 1 + rnd.nextInt(1 + (d * 2).toInt())
        Feature.BOMB, Feature.SWITCH, Feature.RAINBOW -> 1
        Feature.DOUBLE -> 1 + rnd.nextInt(2)
        Feature.MYSTERY, Feature.HIDDEN_CUP -> 3 + rnd.nextInt(3 + (d * 6).toInt())
        else -> 2 + rnd.nextInt(2 + (d * 5).toInt())
    }

    private fun shape(rnd: Random, w: Int, h: Int): IntArray {
        val mid = (w - 1) / 2.0
        val out = when (rnd.nextInt(11)) {
            0 -> IntArray(w) { h }
            1 -> IntArray(w) { (h - (abs(it - mid) * 0.9).roundToInt()) }
            2 -> IntArray(w) { (h - 2 + (abs(it - mid) * 0.9).roundToInt()) }
            3 -> IntArray(w) { h - (w - 1 - it) * 2 / maxOf(1, w - 1) - (w - 1 - it) / 2 }
            4 -> IntArray(w) { h - it * 2 / maxOf(1, w - 1) - it / 2 }
            5 -> IntArray(w) { if (it % 2 == 0) h else h - 2 }
            6 -> IntArray(w) { if (it % 2 == 0) h else h - 1 }
            7 -> IntArray(w) { if (it == 1 || it == w - 2) h else h - 2 }
            8 -> IntArray(w) { if (w >= 5 && w % 2 == 1 && it == w / 2) 0 else h }
            9 -> IntArray(w) { if (it == 0 || it == w - 1) h else h - 1 }
            else -> {
                var cur = h
                IntArray(w) { cur = (cur + rnd.nextInt(3) - 1).coerceIn(maxOf(2, h - 3), h); cur }
            }
        }
        for (i in out.indices) if (out[i] != 0) out[i] = out[i].coerceIn(1, 7)
        if (out.max() < h) out[rnd.nextInt(w)] = h
        return out
    }

    /** All active cells must reach the bottom row through active cells. */
    private fun connected(w: Int, h: Int, mask: BooleanArray): Boolean {
        val seen = BooleanArray(w * h)
        val q = ArrayDeque<Int>()
        for (c in 0 until w) if (mask[c]) { seen[c] = true; q += c }
        while (q.isNotEmpty()) {
            val i = q.removeFirst()
            val c = i % w; val r = i / w
            for (k in 0 until 4) {
                val nc = c + Board.DC[k]; val nr = r + Board.DR[k]
                if (nc !in 0 until w || nr !in 0 until h) continue
                val n = nr * w + nc
                if (mask[n] && !seen[n]) { seen[n] = true; q += n }
            }
        }
        return mask.indices.all { !mask[it] || seen[it] }
    }

    // ---------------------------------------------------------------- builder

    private class Slot(val index: Int, val col: Int, val row: Int) {
        var kind = BlockKind.NORMAL
        var color = 0
        var param = 0
        var id = 0
        var startsOpen = false
    }

    private fun build(n: Int, recipe: Recipe, rnd: Random): LevelSpec? {
        val heights = recipe.heights
        val w = heights.size
        val h = heights.max()
        if (h <= 0) return null
        val mask = BooleanArray(w * h) { i -> (i / w) < heights[i % w] }
        // inner walls (never in the bottom row, never disconnecting the shape)
        repeat(recipe.walls) {
            val cands = mask.indices.filter { mask[it] && it / w in 1 until h - 1 }
            if (cands.isEmpty()) return@repeat
            val i = cands[rnd.nextInt(cands.size)]
            mask[i] = false
            if (!connected(w, h, mask)) mask[i] = true
        }
        if (!connected(w, h, mask)) return null
        val activeCells = mask.indices.filter { mask[it] }
        // initial empty slots
        val empty = HashSet<Int>()
        val holeCands = activeCells.shuffled(rnd)
        for (i in holeCands) { if (empty.size >= recipe.holes) break; empty += i }
        val slots = activeCells.filter { it !in empty }.map { Slot(it, it % w, it / w) }
        val total = slots.size
        if (total < 2) return null
        val byIndex = slots.associateBy { it.index }

        // 1. Planned removal order under the real rule (path to exit).
        val dummyCells = Array<BlockSpec?>(w * h) { i -> if (byIndex.containsKey(i)) BlockSpec(i + 1, 0) else null }
        val dummy = LevelSpec(n, w, h, mask, dummyCells, emptyList(), 99, 0, 1, false, emptyList(), null)
        val planBoard = Board(dummy)
        for (s in slots) s.startsOpen = planBoard.exposed[s.index]
        val order = ArrayList<Slot>(total)
        while (order.size < total) {
            val open = slots.filter { planBoard.cells[it.index] != null && planBoard.exposed[it.index] }
            if (open.isEmpty()) return null
            // favour lower blocks a little so plans feel natural
            val pick = open.minByOrNull { it.row + rnd.nextDouble() * 2.5 }!!
            order += pick
            planBoard.tap(pick.index, force = true)
        }
        order.forEachIndexed { i, s -> s.id = i + 1 }

        // 2. Colours.
        val paletteChoices = (0 until MarbleColors.COUNT).filter { n >= 60 || it < 8 }.shuffled(rnd)
        val colorCount = recipe.colors.coerceIn(2, minOf(MAX_COLORS, total, paletteChoices.size))
        val palette = paletteChoices.take(colorCount)
        val colorSeq = ArrayList<Int>(total)
        palette.forEach { colorSeq += it }
        while (colorSeq.size < total) colorSeq += palette[rnd.nextInt(colorCount)]
        colorSeq.shuffle(rnd)
        order.forEachIndexed { i, s -> s.color = colorSeq[i] }

        // 3. Special kinds, consistent with the plan.
        val f = recipe.features
        fun free(pred: (Int, Slot) -> Boolean): MutableList<Int> =
            order.indices.filter { order[it].kind == BlockKind.NORMAL && pred(it, order[it]) }.toMutableList()

        var lockId = 0
        repeat(f[Feature.KEY] ?: 0) {
            val keyCands = free { i, _ -> i < total - 2 && i < total * 0.6 }
            if (keyCands.isEmpty()) return@repeat
            val k = keyCands[rnd.nextInt(keyCands.size)]
            val chained = free { i, _ -> i > k + 1 }.shuffled(rnd).take(2 + rnd.nextInt(3))
            if (chained.isEmpty()) return@repeat
            order[k].kind = BlockKind.KEY; order[k].param = lockId
            chained.forEach { order[it].kind = BlockKind.CHAINED; order[it].param = lockId }
            lockId++
        }
        var bucketId = 0
        repeat(f[Feature.PAINT] ?: 0) {
            val cands = free { i, _ -> i < total - 2 && i < total * 0.6 }
            if (cands.isEmpty()) return@repeat
            val p = cands[rnd.nextInt(cands.size)]
            val bucket = order[p]
            val grays = free { i, s -> i > p + 1 && abs(s.col - bucket.col) + abs(s.row - bucket.row) <= 2 }.shuffled(rnd).take(1 + rnd.nextInt(3))
                .ifEmpty { free { i, _ -> i > p + 1 }.shuffled(rnd).take(2) }
            if (grays.isEmpty()) return@repeat
            bucket.kind = BlockKind.PAINT; bucket.param = bucketId
            grays.forEach { order[it].kind = BlockKind.GRAY; order[it].param = bucketId; order[it].color = bucket.color }
            bucketId++
        }
        fun assign(feature: Feature, kind: BlockKind, pred: (Int, Slot) -> Boolean = { _, _ -> true }) {
            free(pred).shuffled(rnd).take(f[feature] ?: 0).forEach { order[it].kind = kind }
        }
        assign(Feature.DOUBLE, BlockKind.DOUBLE)
        if (colorCount >= 2) assign(Feature.RAINBOW, BlockKind.RAINBOW)
        assign(Feature.ICE, BlockKind.ICE)
        assign(Feature.MYSTERY, BlockKind.MYSTERY) { _, s -> !s.startsOpen }
        assign(Feature.COUNTER, BlockKind.COUNTER) { i, _ -> i >= 1 }
        assign(Feature.CRATE, BlockKind.CRATE) { i, _ -> i >= 2 }
        assign(Feature.BOMB, BlockKind.BOMB) { i, _ -> i in 2 until total - 1 }
        assign(Feature.SWITCH, BlockKind.SWITCH)
        f[Feature.ARROW]?.let { cnt ->
            free { _, s -> !s.startsOpen }.shuffled(rnd).take(cnt).forEach {
                order[it].kind = BlockKind.ARROW
                order[it].param = rnd.nextInt(3)
            }
        }

        // 4. Tray cups from the plan, perturbed by the difficulty window.
        val cups = ArrayList<Int>()
        for (s in order) repeat(if (s.kind == BlockKind.DOUBLE) 6 else 3) { cups += s.color }
        for (attemptWindow in recipe.window downTo 0) {
            val seq = ArrayList(cups)
            if (attemptWindow > 0) for (i in seq.indices) {
                val j = (i + rnd.nextInt(attemptWindow + 1)).coerceAtMost(seq.size - 1)
                val t = seq[i]; seq[i] = seq[j]; seq[j] = t
            }
            val trayCols = List(recipe.trayCols) { ArrayList<CupSpec>() }
            for (color in seq) {
                val minLen = trayCols.minOf { it.size }
                val opts = trayCols.filter { it.size <= minLen + 1 }
                opts[rnd.nextInt(opts.size)] += CupSpec(color)
            }
            val hiddenCount = f[Feature.HIDDEN_CUP] ?: 0
            if (hiddenCount > 0) {
                val cands = trayCols.flatMapIndexed { c, col -> col.indices.filter { it > 0 }.map { c to it } }.shuffled(rnd)
                cands.take(hiddenCount).forEach { (c, i) -> trayCols[c][i] = trayCols[c][i].copy(hidden = true) }
            }

            // 5. Timeline with timers open.
            val planIds = ArrayList<Int>()
            for (s in order) { if (s.kind == BlockKind.ICE) planIds += s.id; planIds += s.id }
            val timelineSpec = makeSpec(n, recipe, w, h, mask, slots, trayCols, 999, planIds, colorCount, openTimers = true)
            val sim = PuzzleSim(timelineSpec)
            val popsBefore = HashMap<Int, Int>()
            val movesBefore = HashMap<Int, Int>()
            val cupsBefore = HashMap<Int, Int>()
            var ok = true
            for (id in planIds) {
                popsBefore[id] = sim.board.pops
                movesBefore[id] = sim.board.moves
                cupsBefore[id] = sim.board.cupsDone
                if (!sim.tapId(id)) { ok = false; break }
            }
            if (!ok || !sim.solved) continue

            // 6. Timer parameters derived from the plan.
            for (s in order) when (s.kind) {
                BlockKind.COUNTER -> { val p = popsBefore[s.id]!!; if (p < 1) s.kind = BlockKind.NORMAL else s.param = 1 + rnd.nextInt(minOf(p, 6)) }
                BlockKind.CRATE -> { val c = cupsBefore[s.id]!!; if (c < 1) s.kind = BlockKind.NORMAL else s.param = maxOf(1, c - rnd.nextInt(minOf(c, 4))) }
                BlockKind.BOMB -> s.param = movesBefore[s.id]!! + 2 + rnd.nextInt(3)
                else -> {}
            }

            // 7. Verify and size the belt.
            val spec0 = makeSpec(n, recipe, w, h, mask, slots, trayCols, 999, planIds, colorCount, openTimers = false)
            val verify = PuzzleSim(spec0)
            if (!planIds.all { verify.tapId(it) } || !verify.solved) continue
            val peak = verify.peakLoad
            if (peak + 2 > MAX_CAPACITY) continue
            val capacity = (peak + recipe.margin).coerceIn(maxOf(MIN_CAPACITY, peak), MAX_CAPACITY)
            return makeSpec(n, recipe, w, h, mask, slots, trayCols, capacity, planIds, colorCount, openTimers = false)
        }
        return null
    }

    private fun makeSpec(
        n: Int, recipe: Recipe, w: Int, h: Int, mask: BooleanArray, slots: List<Slot>, tray: List<List<CupSpec>>,
        capacity: Int, plan: List<Int>, colorCount: Int, openTimers: Boolean,
    ): LevelSpec {
        val cells = arrayOfNulls<BlockSpec>(w * h)
        for (s in slots) {
            val timer = s.kind == BlockKind.COUNTER || s.kind == BlockKind.CRATE || s.kind == BlockKind.BOMB
            cells[s.index] = BlockSpec(s.id, s.color, if (openTimers && timer) BlockKind.NORMAL else s.kind, s.param)
        }
        return LevelSpec(
            number = n, width = w, height = h, mask = mask.copyOf(), cells = cells,
            tray = tray.map { it.toList() }, capacity = capacity, theme = themeFor(n), colorCount = colorCount,
            isBoss = recipe.boss, plan = plan, introduces = Feature.introducedAt(n),
        )
    }
}
