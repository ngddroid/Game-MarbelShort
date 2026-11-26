package com.ngstudio.marblepop.level

class BlockState(val spec: BlockSpec) {
    val id: Int get() = spec.id
    var cracked = false
    var revealed = spec.kind != BlockKind.MYSTERY
}

enum class TapOutcome { NONE, CRACK, POP }

/**
 * Grid rules shared by the real-time engine, the level generator and the unit tests.
 *
 * Blocks never move. A block is *open* (tappable) when it sits in the bottom row or touches an
 * empty cell that is connected through empty cells to the bottom row — its marbles flow out along
 * that path into the hopper. Arrow blocks may alternatively leave straight in their arrow direction.
 */
class Board(val spec: LevelSpec) {
    val w = spec.width
    val h = spec.height
    val mask = spec.mask
    val cells: Array<BlockState?> = Array(w * h) { i -> spec.cells[i]?.let { BlockState(it) } }

    var pops = 0
        private set
    var moves = 0
        private set
    var cupsDone = 0
    var bombBonus = 0
    val openLocks = HashSet<Int>()
    val paintedBuckets = HashSet<Int>()

    /** Empty cells that connect to the exit. */
    private val reach = BooleanArray(w * h)
    /** Block cells with a free path to the exit. */
    val exposed = BooleanArray(w * h)
    private val queue = IntArray(w * h)
    private val parent = IntArray(w * h)

    init { refresh() }

    fun idx(c: Int, r: Int) = r * w + c
    fun col(i: Int) = i % w
    fun row(i: Int) = i / w
    fun active(c: Int, r: Int) = c in 0 until w && r in 0 until h && mask[r * w + c]
    fun blockAt(c: Int, r: Int): BlockState? = if (c in 0 until w && r in 0 until h) cells[r * w + c] else null

    val isEmpty: Boolean get() = cells.all { it == null }

    fun locate(id: Int): Int = cells.indexOfFirst { it?.id == id }

    /** Recomputes reachability after the grid changed. */
    fun refresh() {
        java.util.Arrays.fill(reach, false)
        var head = 0; var tail = 0
        for (c in 0 until w) {
            val i = idx(c, 0)
            if (mask[i] && cells[i] == null) { reach[i] = true; queue[tail++] = i }
        }
        while (head < tail) {
            val i = queue[head++]
            val c = col(i); val r = row(i)
            for (k in 0 until 4) {
                val nc = c + DC[k]; val nr = r + DR[k]
                if (!active(nc, nr)) continue
                val n = idx(nc, nr)
                if (!reach[n] && cells[n] == null) { reach[n] = true; queue[tail++] = n }
            }
        }
        for (i in cells.indices) {
            if (cells[i] == null) { exposed[i] = false; continue }
            val c = col(i); val r = row(i)
            var open = r == 0
            if (!open) for (k in 0 until 4) {
                val nc = c + DC[k]; val nr = r + DR[k]
                if (active(nc, nr) && reach[idx(nc, nr)]) { open = true; break }
            }
            exposed[i] = open
            if (open) cells[i]!!.revealed = true
        }
    }

    fun isLocked(b: BlockState): Boolean = when (b.spec.kind) {
        BlockKind.CHAINED -> b.spec.param !in openLocks
        BlockKind.GRAY -> b.spec.param !in paintedBuckets
        BlockKind.COUNTER -> pops < b.spec.param
        BlockKind.CRATE -> cupsDone < b.spec.param
        else -> false
    }

    fun counterRemaining(b: BlockState): Int = when (b.spec.kind) {
        BlockKind.COUNTER -> (b.spec.param - pops).coerceAtLeast(0)
        BlockKind.CRATE -> (b.spec.param - cupsDone).coerceAtLeast(0)
        BlockKind.BOMB -> (b.spec.param + bombBonus - moves).coerceAtLeast(0)
        else -> 0
    }

    /** Straight exit for arrow blocks: every cell in the arrow direction is empty until the board edge. */
    fun arrowFree(i: Int): Boolean {
        val b = cells[i] ?: return false
        if (b.spec.kind != BlockKind.ARROW) return false
        val dc = when (b.spec.param) { 0 -> -1; 1 -> 1; else -> 0 }
        val dr = if (b.spec.param == 2) 1 else 0
        var c = col(i) + dc; var r = row(i) + dr
        while (active(c, r)) {
            if (cells[idx(c, r)] != null) return false
            c += dc; r += dr
        }
        return true
    }

    /** True if the block has some way out (path or arrow), ignoring locks. */
    fun hasExit(i: Int): Boolean = exposed[i] || arrowFree(i)

    fun canTap(i: Int): Boolean {
        val b = cells.getOrNull(i) ?: return false
        return !isLocked(b) && hasExit(i)
    }

    /**
     * Cells the marbles travel through, from the block's own cell to an exit cell in the bottom row.
     * Returns null when the block can only leave by its arrow.
     */
    fun pathOut(i: Int): IntArray? {
        if (row(i) == 0) return intArrayOf(i)
        java.util.Arrays.fill(parent, -2)
        var head = 0; var tail = 0
        queue[tail++] = i; parent[i] = -1
        while (head < tail) {
            val cur = queue[head++]
            if (cur != i && row(cur) == 0) {
                val out = ArrayList<Int>()
                var p = cur
                while (p != -1) { out += p; p = parent[p] }
                out.reverse()
                return out.toIntArray()
            }
            val c = col(cur); val r = row(cur)
            for (k in DOWN_FIRST) {
                val nc = c + DC[k]; val nr = r + DR[k]
                if (!active(nc, nr)) continue
                val n = idx(nc, nr)
                if (parent[n] != -2 || cells[n] != null) continue
                parent[n] = cur; queue[tail++] = n
            }
        }
        return null
    }

    /** Applies a tap on cell i. Returns POP when the block left the board. */
    fun tap(i: Int, force: Boolean = false): TapOutcome {
        val b = cells.getOrNull(i) ?: return TapOutcome.NONE
        if (!force && !canTap(i)) return TapOutcome.NONE
        moves++
        if (!force && b.spec.kind == BlockKind.ICE && !b.cracked) {
            b.cracked = true
            return TapOutcome.CRACK
        }
        cells[i] = null
        pops++
        when (b.spec.kind) {
            BlockKind.KEY -> openLocks += b.spec.param
            BlockKind.PAINT -> paintedBuckets += b.spec.param
            else -> {}
        }
        refresh()
        return TapOutcome.POP
    }

    fun bombExpired(): Boolean = cells.any { it != null && it.spec.kind == BlockKind.BOMB && moves >= it.spec.param + bombBonus }

    fun hasTappable(): Boolean {
        for (i in cells.indices) if (canTap(i)) return true
        return false
    }

    /** Real (non-rainbow) marbles still on the board, per colour. */
    fun gridMarbles(out: IntArray) {
        for (b in cells) if (b != null && b.spec.kind != BlockKind.RAINBOW) out[b.spec.color] += b.spec.marbleCount
    }

    companion object {
        // left, right, down, up
        val DC = intArrayOf(-1, 1, 0, 0)
        val DR = intArrayOf(0, 0, -1, 1)
        private val DOWN_FIRST = intArrayOf(2, 0, 1, 3)
    }
}

/** Cup columns for the discrete simulation. */
class TraySim(spec: LevelSpec) {
    class Cup(val color: Int) { var filled = 0 }
    val columns: Array<ArrayDeque<Cup>> = Array(spec.tray.size) { c -> ArrayDeque(spec.tray[c].map { Cup(it.color) }) }

    fun slotsRemaining(out: IntArray) {
        for (col in columns) for (cup in col) out[cup.color] += 3 - cup.filled
    }
    val isEmpty get() = columns.all { it.isEmpty() }
}

/**
 * Discrete, idealised simulation of a level: every marble that can go to a top cup goes there instantly.
 * Used by the generator to size the belt and by tests to prove every level is solvable.
 */
class PuzzleSim(val spec: LevelSpec) {
    val board = Board(spec)
    val tray = TraySim(spec)
    val belt = IntArray(MarbleColors.COUNT)
    var rainbow = 0
    var peakLoad = 0
        private set
    val load: Int get() = belt.sum() + rainbow
    private val tmpA = IntArray(MarbleColors.COUNT)
    private val tmpB = IntArray(MarbleColors.COUNT)

    fun tapId(id: Int): Boolean {
        val i = board.locate(id)
        if (i < 0 || !board.canTap(i)) return false
        val b = board.cells[i]!!
        val out = board.tap(i)
        if (board.bombExpired()) return false
        if (out == TapOutcome.POP) {
            if (b.spec.kind == BlockKind.RAINBOW) rainbow += b.spec.marbleCount
            else belt[b.spec.color] += b.spec.marbleCount
            peakLoad = maxOf(peakLoad, load)
            settle()
        }
        return true
    }

    fun need(out: IntArray) {
        java.util.Arrays.fill(out, 0)
        java.util.Arrays.fill(tmpB, 0)
        tray.slotsRemaining(out)
        board.gridMarbles(tmpB)
        for (i in out.indices) out[i] -= tmpB[i] + belt[i]
    }

    fun settle() {
        var changed = true
        while (changed) {
            changed = false
            for (col in tray.columns) {
                val cup = col.firstOrNull() ?: continue
                val k = cup.color
                val take = minOf(3 - cup.filled, belt[k])
                if (take > 0) { belt[k] -= take; cup.filled += take; changed = true }
                if (cup.filled < 3 && rainbow > 0) {
                    need(tmpA)
                    val r = minOf(3 - cup.filled, rainbow, tmpA[k])
                    if (r > 0) { rainbow -= r; cup.filled += r; changed = true }
                }
                if (cup.filled == 3) { col.removeFirst(); board.cupsDone++; changed = true }
            }
        }
    }

    val solved: Boolean get() = board.isEmpty && load == 0 && tray.isEmpty
}
