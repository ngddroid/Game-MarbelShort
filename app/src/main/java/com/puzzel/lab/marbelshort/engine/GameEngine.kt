package com.puzzel.lab.marbelshort.engine

import com.puzzel.lab.marbelshort.core.Sfx
import com.puzzel.lab.marbelshort.level.BlockKind
import com.puzzel.lab.marbelshort.level.BlockState
import com.puzzel.lab.marbelshort.level.Board
import com.puzzel.lab.marbelshort.level.LevelSpec
import com.puzzel.lab.marbelshort.level.MarbleColors
import com.puzzel.lab.marbelshort.level.TapOutcome
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class GameStatus { PLAYING, WON, FAILED }
enum class FailReason { JAM, BOMB, NO_MOVES }
enum class Haptic { LIGHT, MEDIUM, HEAVY, SUCCESS }

interface GameListener {
    fun sound(sfx: Sfx, pitch: Float = 1f, volume: Float = 1f) {}
    fun haptic(kind: Haptic) {}
    fun combo(count: Int) {}
    fun statusChanged(status: GameStatus) {}
}

/**
 * Real-time game simulation.
 * Board rules (which blocks are open) live in [Board]; this class animates everything:
 * marbles streaming out of the board along their path, the hopper physics, the belt and the cups.
 */
class GameEngine(val spec: LevelSpec, val layout: GameLayout, private val listener: GameListener) {
    val board = Board(spec)
    var status = GameStatus.PLAYING
        private set
    var failReason = FailReason.JAM
        private set
    var time = 0f
        private set

    // ---- board visuals (fixed cells, index = row * width + col)
    val visuals: Array<BlockVisual?> = Array(spec.width * spec.height) { i ->
        board.cells[i]?.let { st ->
            val c = i % spec.width; val r = i / spec.width
            BlockVisual(st, i, c, r).also {
                it.dropY = spec.height + 1.5f - r * 0.4f
                it.delay = INTRO_MACHINE + r * 0.08f + c * 0.05f
                it.wasLocked = board.isLocked(st)
                it.wasOpen = board.hasExit(i)
                it.open = if (it.wasOpen) 1f else 0f
            }
        }
    }
    val exiting = ArrayList<ExitingBlock>()
    val flying = ArrayList<FlyingBlock>()

    var introT = 0f
        private set
    val introDone: Float = INTRO_MACHINE + (spec.height - 1) * 0.08f + (spec.width - 1) * 0.05f + 0.5f
    val inIntro: Boolean get() = introT < introDone

    // ---- marbles
    val marbles = ArrayList<Marble>()
    private val physicsList = ArrayList<Marble>()

    // ---- belt
    var capacity = spec.capacity
        private set
    var slots = arrayOfNulls<Marble>(capacity)
        private set
    var spacing = layout.beltLength / capacity
        private set
    var beltOffset = 0f
        private set
    var beltVel = 0f
        private set
    private var beltDir = 1f
    val beltSpeed = 175f
    var beltCount = 0
        private set
    var peakBelt = 0
        private set

    // ---- tray
    val tray: List<TrayColumn> = spec.tray.map { col ->
        TrayColumn().also { tc -> col.forEachIndexed { i, c -> tc.cups += Cup(c.color, c.hidden).also { it.display = i + 5f } } }
    }

    // ---- fx
    val particles = Particles()
    val texts = ArrayList<FloatingText>()
    var shake = 0f
        private set
    var danger = false
        private set
    var dangerLevel = 0f
        private set

    // ---- stats
    var combo = 0
        private set
    var maxCombo = 0
        private set
    var comboCoins = 0
        private set
    var cupsFilled = 0
        private set
    var marblesSorted = 0
        private set
    var boostersUsed = 0
    var continued = false
        private set
    private var lastCupTime = -10f
    private var lastDropTime = 0f
    private var lastClack = 0f
    private var pendingEnd = -1f
    private var pendingStatus = GameStatus.PLAYING
    private val tmp = FloatArray(2)
    private val gridTmp = IntArray(MarbleColors.COUNT)

    private val segs: FloatArray = run {
        val l = layout
        val sl = l.cx - l.spoutHalf
        val sr = l.cx + l.spoutHalf
        floatArrayOf(
            l.gridLeft - 2f, l.gridBottom, sl, l.spoutTop,
            sl, l.spoutTop, sl, l.gateFloor,
            sl, l.gateFloor, sr, l.gateFloor,
            sr, l.gateFloor, sr, l.spoutTop,
            sr, l.spoutTop, l.gridRight + 2f, l.gridBottom,
        )
    }

    val lapTime: Float get() = layout.beltLength / beltSpeed
    val isBusy: Boolean get() = marbles.isNotEmpty() || flying.isNotEmpty()

    // ============================================================ input

    fun cellAt(wx: Float, wy: Float): Int {
        val c = floor((wx - layout.gridLeft) / layout.cell).toInt()
        val r = floor((layout.gridBottom - wy) / layout.cell).toInt()
        if (c !in 0 until spec.width || r !in 0 until spec.height) return -1
        return r * spec.width + c
    }

    fun tap(wx: Float, wy: Float) {
        if (status != GameStatus.PLAYING || pendingEnd >= 0f || inIntro) return
        val i = cellAt(wx, wy)
        if (i < 0) return
        val v = visuals[i] ?: return
        if (!board.canTap(i)) {
            v.shake = 1f
            if (board.isLocked(v.state)) v.pulse = 1f
            listener.sound(Sfx.TAP, 0.7f, 0.6f)
            listener.haptic(Haptic.LIGHT)
            return
        }
        listener.sound(Sfx.TAP, 1f + Random.nextFloat() * 0.1f)
        val path = if (board.exposed[i]) board.pathOut(i) else null
        when (board.tap(i)) {
            TapOutcome.CRACK -> {
                v.crack = 1f; v.squash = 0.5f
                particles.burst(layout.cellCenterX(v.col), layout.cellCenterY(v.row.toFloat()), 14, 260f, 0xFFE6F8FF.toInt(), Particles.SHARD, 7f, 0.6f, 900f)
                listener.sound(Sfx.ICE)
                listener.haptic(Haptic.MEDIUM)
            }
            TapOutcome.POP -> pop(v, path)
            TapOutcome.NONE -> {}
        }
        afterMove()
    }

    private fun afterMove() {
        if (board.bombExpired()) {
            for (v in visuals) if (v != null && v.state.spec.kind == BlockKind.BOMB && board.counterRemaining(v.state) == 0) {
                val x = layout.cellCenterX(v.col); val y = layout.cellCenterY(v.row.toFloat())
                particles.burst(x, y, 40, 520f, 0xFFFF7A1A.toInt(), Particles.DOT, 14f, 0.8f, 300f)
                particles.burst(x, y, 18, 220f, 0xFF5A5A6E.toInt(), Particles.SMOKE, 26f, 1.1f, -60f)
                particles.emit(x, y, 0f, 0f, 0.5f, layout.cell * 2.2f, 0xFFFFD23F.toInt(), Particles.RING)
            }
            shake = 20f
            listener.sound(Sfx.EXPLODE)
            listener.haptic(Haptic.HEAVY)
            endWith(GameStatus.FAILED, FailReason.BOMB, 0.9f)
        }
        refreshBlockStates()
    }

    /** Removes a block: marbles stream out along [path], or the block flies to the hopper when there's no path. */
    private fun pop(v: BlockVisual, path: IntArray?) {
        visuals[v.index] = null
        val l = layout
        val cell = l.cell
        val left = l.cellLeft(v.col)
        val top = l.cellTop(v.row.toFloat())
        val st = v.state
        if (path == null) {
            val f = FlyingBlock(st, left, top, cell)
            when {
                st.spec.kind == BlockKind.ARROW && st.spec.param == 0 -> { f.mx = l.gridLeft - cell * 1.1f; f.my = top }
                st.spec.kind == BlockKind.ARROW && st.spec.param == 1 -> { f.mx = l.gridRight + cell * 0.1f; f.my = top }
                else -> { f.mx = left; f.my = minOf(top, l.gridTop) - cell * 0.9f }
            }
            f.tx = l.cx - cell / 2; f.ty = l.gridBottom + 6f
            flying += f
            listener.sound(Sfx.WHOOSH)
        } else {
            streamOut(st, v.col, v.row, path)
            exiting += ExitingBlock(st, left, top, cell)
        }
        listener.haptic(Haptic.MEDIUM)
    }

    private fun streamOut(st: BlockState, col: Int, row: Int, path: IntArray) {
        val l = layout
        val cell = l.cell
        val spec = st.spec
        val n = spec.marbleCount
        val color = spec.marbleColor
        val left = l.cellLeft(col); val top = l.cellTop(row.toFloat())
        val exit = path.last()
        val exitCol = exit % this.spec.width
        for (k in 0 until n) {
            val gx = k % 3; val gy = (k / 3) % 3
            val ox = (gx - 1) * cell * 0.22f
            val oy = (gy - 1) * cell * 0.22f
            val m = Marble(color)
            m.state = MarbleState.PATH
            m.x = left + cell * (0.5f) + (gx - 1) * cell * 0.28f
            m.y = top + cell * (0.5f) + (gy - 1) * cell * 0.28f
            val pts = FloatArray((path.size) * 2 + 2)
            var p = 0
            for (j in 1 until path.size) {
                val pc = path[j] % this.spec.width; val pr = path[j] / this.spec.width
                pts[p++] = l.cellCenterX(pc) + ox; pts[p++] = l.cellCenterY(pr.toFloat()) + oy
            }
            pts[p++] = l.cellCenterX(exitCol) + ox; pts[p++] = l.gridBottom + 6f + oy * 0.5f
            m.wp = pts.copyOf(p)
            m.wpi = 0
            m.wait = k * 0.022f + (k / 9) * 0.12f
            marbles += m
        }
        val cx = left + cell / 2; val cy = top + cell / 2
        val argb = if (color == MarbleColors.RAINBOW) 0xFFFFFFFF.toInt() else MarbleColors.argb[color]
        particles.emit(cx, cy, 0f, 0f, 0.35f, cell * 1.15f, argb, Particles.RING)
        particles.burst(cx, cy, 10, 260f, argb, Particles.DOT, 8f, 0.45f, 300f)
        particles.burst(cx, cy, 5, 170f, 0xFFFFFFFF.toInt(), Particles.STAR, 11f, 0.45f)
        shake = maxOf(shake, 3f)
        listener.sound(Sfx.POP, 0.95f + Random.nextFloat() * 0.12f)
        if (spec.kind == BlockKind.SWITCH) {
            beltDir = -beltDir
            texts += FloatingText("REVERSE!", l.cx, l.beltCy, 0xFFFFFFFF.toInt(), 30f)
        }
    }

    /** Burst at the hopper mouth (flying blocks and hammered blocks without a path). */
    private fun burstAtMouth(st: BlockState) {
        val l = layout
        val n = st.spec.marbleCount
        for (k in 0 until n) {
            val m = Marble(st.spec.marbleColor)
            m.x = l.cx + ((k % 5) - 2) * l.marbleR * 2.1f
            m.y = l.gridBottom + 4f - (k / 5) * l.marbleR * 2f
            m.px = m.x; m.py = m.y - 1f
            marbles += m
        }
        val argb = if (st.spec.marbleColor == MarbleColors.RAINBOW) 0xFFFFFFFF.toInt() else MarbleColors.argb[st.spec.marbleColor]
        particles.emit(l.cx, l.gridBottom, 0f, 0f, 0.4f, l.cell * 1.3f, argb, Particles.RING)
        particles.burst(l.cx, l.gridBottom, 14, 300f, argb, Particles.DOT, 9f, 0.5f, 400f)
        listener.sound(Sfx.POP)
        if (st.spec.kind == BlockKind.SWITCH) beltDir = -beltDir
    }

    // ============================================================ boosters

    fun hammer(wx: Float, wy: Float): Boolean {
        if (status != GameStatus.PLAYING || inIntro) return false
        val i = cellAt(wx, wy)
        if (i < 0) return false
        val v = visuals[i] ?: return false
        val path = if (board.exposed[i]) board.pathOut(i) else null
        board.tap(i, force = true)
        shake = 10f
        listener.sound(Sfx.EXPLODE, 1.4f, 0.7f)
        particles.burst(layout.cellCenterX(v.col), layout.cellCenterY(v.row.toFloat()), 18, 300f, 0xFFFFFFFF.toInt(), Particles.STAR, 12f, 0.6f)
        if (path != null) { streamOut(v.state, v.col, v.row, path); visuals[i] = null; exiting += ExitingBlock(v.state, layout.cellLeft(v.col), layout.cellTop(v.row.toFloat()), layout.cell) }
        else pop(v, null)
        boostersUsed++
        afterMove()
        return true
    }

    fun magnet(): Boolean {
        if (status != GameStatus.PLAYING) return false
        val counts = IntArray(MarbleColors.COUNT)
        for (m in slots) if (m != null && m.state == MarbleState.BELT && m.color != MarbleColors.RAINBOW) counts[m.color]++
        var best = -1; var bestScore = 0
        for (k in counts.indices) {
            if (counts[k] == 0) continue
            val room = tray.sumOf { col -> col.cups.filter { it.color == k && !it.popping }.sumOf { 3 - it.reserved } }
            val score = min(room, counts[k])
            if (score > bestScore) { bestScore = score; best = k }
        }
        if (best < 0) return false
        var moved = 0
        for (i in slots.indices) {
            val m = slots[i] ?: continue
            if (m.state != MarbleState.BELT || m.color != best) continue
            var placed = false
            for ((ci, col) in tray.withIndex()) {
                for (cup in col.cups) {
                    if (cup.color == best && !cup.popping && cup.reserved < 3) {
                        cup.reserved++
                        startDrop(m, ci, cup, 0.5f + moved * 0.03f)
                        cup.reveal = 1f; cup.hidden = false
                        placed = true; break
                    }
                }
                if (placed) break
            }
            if (placed) moved++
        }
        if (moved > 0) {
            boostersUsed++
            listener.sound(Sfx.REWARD, 1.2f, 0.8f)
            texts += FloatingText("MAGNET!", layout.cx, layout.beltCy - 20f, 0xFFFFFFFF.toInt(), 32f)
        }
        return moved > 0
    }

    fun addSlots(n: Int) {
        val old = slots
        val oldSpacing = spacing
        capacity += n
        spacing = layout.beltLength / capacity
        slots = arrayOfNulls(capacity)
        for (i in old.indices) {
            val m = old[i] ?: continue
            val s = i * oldSpacing
            var idx = ((s / spacing).roundToInt()) % capacity
            while (slots[idx] != null) idx = (idx + 1) % capacity
            slots[idx] = m
            m.slot = idx
        }
        beltCount = slots.count { it != null }
        texts += FloatingText("+$n SLOTS", layout.cx, layout.beltCy, 0xFFFFFFFF.toInt(), 30f)
        listener.sound(Sfx.REWARD)
    }

    fun boosterSlots() { addSlots(6); boostersUsed++ }

    fun shuffleTray(): Boolean {
        val cups = tray.flatMap { col -> col.cups.filter { it.reserved == 0 && !it.popping } }
        if (cups.size < 2) return false
        val colors = cups.map { it.color }.shuffled()
        cups.forEachIndexed { i, cup ->
            cup.color = colors[i]
            cup.hidden = false; cup.reveal = 1f
            cup.shuffleT = 1f
        }
        listener.sound(Sfx.WHOOSH, 1.2f)
        return true
    }

    fun boosterShuffle(): Boolean = shuffleTray().also { if (it) boostersUsed++ }

    fun continueGame() {
        if (status != GameStatus.FAILED) return
        continued = true
        when (failReason) {
            FailReason.BOMB -> board.bombBonus += 6
            FailReason.JAM -> addSlots(10)
            FailReason.NO_MOVES -> { shuffleTray(); addSlots(6) }
        }
        lastDropTime = time
        pendingEnd = -1f
        status = GameStatus.PLAYING
        listener.statusChanged(status)
    }

    // ============================================================ update

    fun update(dtIn: Float) {
        val dt = dtIn.coerceIn(0f, 1f / 30f)
        time += dt
        if (introT < 10f) {
            val before = introT
            introT += dt
            if (before == 0f) listener.sound(Sfx.WHOOSH, 0.8f, 0.7f)
            if (before < INTRO_MACHINE && introT >= INTRO_MACHINE) { shake = 5f; listener.haptic(Haptic.LIGHT) }
        }
        updateBlocks(dt)
        updateFlying(dt)
        updatePaths(dt)
        physics(dt)
        updateBelt(dt)
        updateDropping(dt)
        updateCups(dt)
        particles.update(dt)
        var i = 0
        while (i < texts.size) { val t = texts[i]; t.life += dt; t.y -= 50f * dt; if (t.life > t.maxLife) texts.removeAt(i) else i++ }
        i = 0
        while (i < exiting.size) { val e = exiting[i]; e.t += dt / 0.22f; if (e.t >= 1f) exiting.removeAt(i) else i++ }
        shake = (shake - dt * 40f).coerceAtLeast(0f)
        dangerLevel += ((if (danger) 1f else 0f) - dangerLevel) * min(1f, dt * 4f)
        checkEnd(dt)
    }

    private fun updateBlocks(dt: Float) {
        for (v in visuals) {
            if (v == null) continue
            if (v.delay > 0f) { v.delay -= dt; continue }
            if (v.dropY > 0f || v.dropVel != 0f) {
                v.dropVel += 34f * dt
                v.dropY -= v.dropVel * dt
                if (v.dropY <= 0f) {
                    if (v.dropVel > 2f) { v.squash = min(0.3f, v.dropVel * 0.03f); listener.sound(Sfx.CLACK, 0.6f + 0.08f * v.row, 0.35f) }
                    v.dropY = 0f; v.dropVel = 0f
                }
            }
            val target = if (board.hasExit(v.index)) 1f else 0f
            v.open += (target - v.open) * min(1f, dt * 9f)
            v.squash = (v.squash - dt * 2.4f).coerceAtLeast(0f)
            v.shake = (v.shake - dt * 3f).coerceAtLeast(0f)
            v.pulse = (v.pulse - dt * 2f).coerceAtLeast(0f)
            v.unlock = (v.unlock - dt * 1.6f).coerceAtLeast(0f)
            v.crack = (v.crack - dt * 3f).coerceAtLeast(0f)
            v.wake = (v.wake - dt * 2.8f).coerceAtLeast(0f)
            if (v.state.revealed && v.reveal < 1f) v.reveal = (v.reveal + dt * 3f).coerceAtMost(1f)
            val painted = v.state.spec.kind == BlockKind.GRAY && v.state.spec.param in board.paintedBuckets
            if (painted && v.paint < 1f) v.paint = (v.paint + dt * 2.5f).coerceAtMost(1f)
            v.flash = (v.flash - dt * 3f).coerceAtLeast(0f)
        }
    }

    private fun refreshBlockStates() {
        var woke = false
        for (v in visuals) {
            if (v == null) continue
            val locked = board.isLocked(v.state)
            if (v.wasLocked && !locked) {
                v.unlock = 1f; v.flash = 1f
                particles.burst(layout.cellCenterX(v.col), layout.cellCenterY(v.row.toFloat()), 12, 240f, 0xFFFFD23F.toInt(), Particles.STAR, 12f, 0.6f)
                listener.sound(Sfx.UNLOCK)
            }
            v.wasLocked = locked
            val open = board.hasExit(v.index)
            if (open && !v.wasOpen) { v.wake = 1f; woke = true }
            v.wasOpen = open
            if (v.state.revealed && v.reveal == 0f) {
                v.reveal = 0.001f
                listener.sound(Sfx.STAR, 1.3f, 0.5f)
            }
        }
        if (woke) listener.sound(Sfx.PLINK, 1.5f, 0.35f)
    }

    private fun updateFlying(dt: Float) {
        var i = 0
        while (i < flying.size) {
            val f = flying[i]
            f.t += dt / f.duration
            val t = f.t.coerceAtMost(1f)
            if (t < 0.35f) {
                val k = easeOut(t / 0.35f)
                f.x = f.x0 + (f.mx - f.x0) * k; f.y = f.y0 + (f.my - f.y0) * k
                f.scale = 1f + 0.12f * k
            } else {
                val k = (t - 0.35f) / 0.65f
                val ctrlX = f.mx; val ctrlY = minOf(f.my, f.ty) - layout.cell * 1.2f
                val a = (1 - k) * (1 - k); val b = 2 * (1 - k) * k; val cc = k * k
                f.x = a * f.mx + b * ctrlX + cc * f.tx
                f.y = a * f.my + b * ctrlY + cc * f.ty
                f.scale = 1.12f - 0.3f * k
                f.rot = 360f * k
            }
            if (f.t >= 1f) {
                flying.removeAt(i)
                burstAtMouth(f.state)
            } else i++
        }
    }

    /** How many loose marbles the hopper holds before streams wait in the board. */
    private val hopperCap: Int = run {
        val l = layout
        val area = (l.gridRight - l.gridLeft + l.spoutHalf * 2) * 0.5f * l.funnelH + l.spoutHalf * 2 * (l.gateFloor - l.spoutTop)
        (area / (4 * l.marbleR * l.marbleR) * 0.72f).toInt().coerceIn(14, 60)
    }

    /** Marbles gliding through empty cells towards the exit. */
    private fun updatePaths(dt: Float) {
        val speed = 820f
        var inHopper = 0
        for (m in marbles) if (m.state == MarbleState.PHYSICS) inHopper++
        for (m in marbles) {
            if (m.state != MarbleState.PATH) continue
            if (m.wait > 0f) { m.wait -= dt; continue }
            val wp = m.wp ?: continue
            var budget = speed * dt
            val lastIdx = wp.size / 2 - 1
            while (budget > 0f && m.wpi * 2 < wp.size) {
                // the final hop drops into the hopper: wait in the exit cell while the hopper is full
                if (m.wpi == lastIdx && inHopper >= hopperCap) { m.wait = 0.06f; break }
                val tx = wp[m.wpi * 2]; val ty = wp[m.wpi * 2 + 1]
                val dx = tx - m.x; val dy = ty - m.y
                val d = sqrt(dx * dx + dy * dy)
                if (d <= budget) { m.x = tx; m.y = ty; budget -= d; m.wpi++ }
                else { m.x += dx / d * budget; m.y += dy / d * budget; budget = 0f }
                m.angle += 8f
            }
            if (m.wpi * 2 >= wp.size) {
                inHopper++
                m.state = MarbleState.PHYSICS
                m.wp = null
                m.px = m.x; m.py = m.y - 5f
            }
        }
    }

    private fun physics(dt: Float) {
        physicsList.clear()
        for (m in marbles) if (m.state == MarbleState.PHYSICS) physicsList += m
        if (physicsList.isEmpty()) return
        val sub = 3
        val h = dt / sub
        val g = 2300f * h * h
        val r = layout.marbleR
        val d2 = 4 * r * r
        val l = layout
        repeat(sub) {
            for (m in physicsList) {
                val vx = ((m.x - m.px) * 0.995f).coerceIn(-9f, 9f)
                val vy = ((m.y - m.py) * 0.995f).coerceIn(-2f, 12f)
                m.px = m.x; m.py = m.y
                m.x += vx; m.y += vy + g
                m.age += h
                if (m.y > l.gridBottom) {
                    val f = ((m.y - l.gridBottom) / (l.spoutTop - l.gridBottom)).coerceIn(0f, 1f)
                    m.x += ((l.cx - m.x) * (0.004f + 0.02f * f)).coerceIn(-0.8f, 0.8f)
                    if (abs(vx) + abs(vy) < 0.12f && m.y < l.gateY - 3f) {
                        m.stuckT += h
                        if (m.stuckT > 0.18f) {
                            val nx = ((l.cx - m.x) * 0.25f).coerceIn(-6f, 6f) + (Random.nextFloat() - 0.5f) * 2f
                            m.x += nx; m.px += nx
                            m.y += 1.5f; m.py += 1.5f
                            m.stuckT = 0f
                        }
                    } else m.stuckT = 0f
                }
            }
            repeat(2) {
                val n = physicsList.size
                for (a in 0 until n) {
                    val ma = physicsList[a]
                    for (b in a + 1 until n) {
                        val mb = physicsList[b]
                        val dx = mb.x - ma.x; val dy = mb.y - ma.y
                        val dd = dx * dx + dy * dy
                        if (dd < d2 && dd > 0.0001f) {
                            val d = sqrt(dd)
                            val soft = if (ma.y > l.gridBottom + r && mb.y > l.gridBottom + r) 0.32f else 0.5f
                            val push = (2 * r - d) * soft / d
                            ma.x -= dx * push; ma.y -= dy * push
                            mb.x += dx * push; mb.y += dy * push
                            if (push * d > 2.5f && time - lastClack > 0.06f) {
                                lastClack = time
                                listener.sound(Sfx.CLACK, 0.9f + Random.nextFloat() * 0.5f, 0.22f)
                            }
                        }
                    }
                }
                for (m in physicsList) walls(m, r)
            }
        }
        for (m in physicsList) m.angle += (m.x - m.px) * 6f
    }

    private fun walls(m: Marble, r: Float) {
        val l = layout
        if (m.y < l.gridBottom) m.x = m.x.coerceIn(l.gridLeft + r, l.gridRight - r)
        var k = 0
        while (k < segs.size) {
            val ax = segs[k]; val ay = segs[k + 1]; val bx = segs[k + 2]; val by = segs[k + 3]
            val ex = bx - ax; val ey = by - ay
            val len2 = ex * ex + ey * ey
            var t = ((m.x - ax) * ex + (m.y - ay) * ey) / len2
            t = t.coerceIn(0f, 1f)
            val qx = ax + ex * t; val qy = ay + ey * t
            val dx = m.x - qx; val dy = m.y - qy
            val dd = dx * dx + dy * dy
            if (dd < r * r && dd > 0.0001f) {
                val d = sqrt(dd)
                val pen = r - d
                m.x += dx / d * pen; m.y += dy / d * pen
                if (pen > 3f && time - lastClack > 0.08f) {
                    lastClack = time
                    listener.sound(Sfx.CLACK, 0.8f + Random.nextFloat() * 0.3f, 0.18f)
                }
            }
            k += 4
        }
        if (m.y > l.gridBottom + 4f) {
            val f = ((m.y - l.gridBottom) / (l.spoutTop - l.gridBottom)).coerceIn(0f, 1f)
            val halfW = (l.gridRight - l.gridLeft) / 2f * (1 - f) + l.spoutHalf * f
            m.x = m.x.coerceIn(l.cx - halfW + r * 0.7f, l.cx + halfW - r * 0.7f)
        }
        if (m.y > l.gateFloor - r) m.y = l.gateFloor - r
    }

    fun slotS(i: Int): Float = i * spacing + beltOffset

    private fun updateBelt(dt: Float) {
        beltVel += (beltDir * beltSpeed - beltVel) * min(1f, dt * 2.5f)
        beltOffset = (beltOffset + beltVel * dt) % layout.beltLength
        val l = layout
        var i0 = ((-beltOffset) / spacing).roundToInt() % capacity
        if (i0 < 0) i0 += capacity
        var dist = (slotS(i0) % l.beltLength)
        if (dist > l.beltLength / 2) dist -= l.beltLength
        if (dist < -l.beltLength / 2) dist += l.beltLength
        if (abs(dist) < spacing * 0.42f && slots[i0] == null) {
            var gate: Marble? = null
            for (m in marbles) if (m.state == MarbleState.PHYSICS && abs(m.x - l.cx) < l.spoutHalf && m.y > l.gateY - 4f) {
                if (gate == null || m.y > gate.y) gate = m
            }
            if (gate != null) {
                gate.state = MarbleState.BOARDING
                gate.slot = i0; slots[i0] = gate
                gate.t = 0f; gate.dur = 0.12f; gate.sx = gate.x; gate.sy = gate.y
                listener.sound(Sfx.CLACK, 1.25f + Random.nextFloat() * 0.2f, 0.3f)
            }
        }
        for (i in slots.indices) {
            val m = slots[i] ?: continue
            l.beltPoint(slotS(i), tmp)
            if (m.state == MarbleState.BOARDING) {
                m.t += dt / m.dur
                val k = easeOut(m.t.coerceAtMost(1f))
                m.prevX = m.x
                m.x = m.sx + (tmp[0] - m.sx) * k; m.y = m.sy + (tmp[1] - m.sy) * k
                if (m.t >= 1f) { m.state = MarbleState.BELT; m.squash = 0.3f }
                continue
            }
            m.prevX = m.x
            m.x = tmp[0]; m.y = tmp[1]
            m.angle += beltVel * dt * 4.8f
            m.squash = (m.squash - dt * 2f).coerceAtLeast(0f)
            if (m.y > l.beltCy + l.beltR * 0.3f && m.prevX != m.x) {
                for (c in tray.indices) {
                    val x = l.colCenter(c)
                    if ((x - m.prevX) * (x - m.x) <= 0f) {
                        val cup = tray[c].cups.firstOrNull() ?: continue
                        if (accepts(cup, m.color)) {
                            cup.reserved++
                            slots[i] = null
                            startDrop(m, c, cup, 0.2f)
                            lastDropTime = time
                            break
                        }
                    }
                }
            }
        }
        beltCount = slots.count { it != null }
        peakBelt = maxOf(peakBelt, beltCount)
        val wasDanger = danger
        danger = beltCount >= capacity * 0.78f
        if (danger && !wasDanger) { listener.sound(Sfx.WARNING); listener.haptic(Haptic.LIGHT) }
    }

    private fun accepts(cup: Cup, color: Int): Boolean {
        if (cup.popping || cup.reserved >= 3 || cup.display > 0.3f || cup.reveal < 0.5f) return false
        if (color == cup.color) return true
        if (color == MarbleColors.RAINBOW) return need(cup.color) > 0
        return false
    }

    private fun need(k: Int): Int {
        var slotsK = 0
        for (col in tray) for (cup in col.cups) if (cup.color == k) slotsK += 3 - cup.reserved
        java.util.Arrays.fill(gridTmp, 0)
        board.gridMarbles(gridTmp)
        var real = gridTmp[k]
        for (m in marbles) if (m.color == k && m.state != MarbleState.DROPPING) real++
        for (f in flying) if (f.state.spec.marbleColor == k) real += f.state.spec.marbleCount
        return slotsK - real
    }

    private fun startDrop(m: Marble, col: Int, cup: Cup, dur: Float) {
        m.state = MarbleState.DROPPING
        if (m.slot >= 0 && m.slot < slots.size && slots[m.slot] === m) slots[m.slot] = null
        m.slot = -1
        m.targetCol = col; m.targetCup = cup; m.targetDimple = cup.reserved - 1
        m.sx = m.x; m.sy = m.y; m.t = 0f; m.dur = dur
    }

    private fun updateDropping(dt: Float) {
        val l = layout
        var i = 0
        while (i < marbles.size) {
            val m = marbles[i]
            if (m.state != MarbleState.DROPPING) { i++; continue }
            val cup = m.targetCup!!
            m.t += dt / m.dur
            val t = m.t.coerceAtMost(1f)
            val tx = l.colCenter(m.targetCol) + l.dimpleDx[m.targetDimple.coerceIn(0, 2)]
            val ty = l.cupTop(cup.display) + l.dimpleDy
            m.x = m.sx + (tx - m.sx) * t
            m.y = m.sy + (ty - m.sy) * t * t - sin(t * PI.toFloat()) * 22f
            if (m.t >= 1f) {
                marbles.removeAt(i)
                cup.arrived++
                cup.bounce = 1f
                marblesSorted++
                listener.sound(Sfx.PLINK, 0.95f + 0.13f * (cup.arrived - 1) + Random.nextFloat() * 0.04f, 0.8f)
                particles.burst(tx, ty, 4, 120f, 0xFFFFFFFF.toInt(), Particles.DOT, 5f, 0.3f)
                if (cup.arrived >= 3) completeCup(m.targetCol, cup)
                continue
            }
            i++
        }
    }

    private fun completeCup(col: Int, cup: Cup) {
        cup.popping = true; cup.popT = 0f
        cupsFilled++
        board.cupsDone++
        combo = if (time - lastCupTime < 1.5f) combo + 1 else 1
        lastCupTime = time
        maxCombo = maxOf(maxCombo, combo)
        val l = layout
        val x = l.colCenter(col); val y = l.cupTop(cup.display) + l.cupH / 2
        val argb = MarbleColors.argb[cup.color]
        particles.emit(x, y, 0f, 0f, 0.45f, l.cupW * 0.9f, argb, Particles.RING)
        particles.burst(x, y, 10, 260f, argb, Particles.STAR, 13f, 0.7f, -100f)
        particles.burst(x, y, 10, 200f, 0xFFFFFFFF.toInt(), Particles.DOT, 6f, 0.5f)
        listener.sound(Sfx.CUP_COMPLETE, 1f + min(combo - 1, 6) * 0.06f)
        listener.haptic(Haptic.LIGHT)
        if (combo >= 2) {
            if (combo >= 3) comboCoins += combo - 2
            listener.combo(combo)
            listener.sound(Sfx.COMBO, 1f + min(combo - 2, 8) * 0.07f, 0.8f)
            texts += FloatingText("x$combo", x, y - 30f, 0xFFFFE27A.toInt(), 34f)
        }
        refreshBlockStates()
    }

    private fun updateCups(dt: Float) {
        for ((ci, col) in tray.withIndex()) {
            var i = 0
            while (i < col.cups.size) {
                val cup = col.cups[i]
                if (cup.popping) {
                    cup.popT += dt
                    if (cup.popT >= 0.42f) { col.cups.removeAt(i); continue }
                }
                i++
            }
            val go = if (introT < INTRO_MACHINE + 0.15f + ci * 0.08f) 0f else 1f
            for ((idx, cup) in col.cups.withIndex()) {
                val target = idx.toFloat()
                val acc = (target - cup.display) * 170f - cup.vel * 15f
                cup.vel += acc * dt * go
                cup.display += cup.vel * dt * go
                if (abs(cup.display - target) < 0.002f && abs(cup.vel) < 0.01f) { cup.display = target; cup.vel = 0f }
                cup.bounce = (cup.bounce - dt * 4f).coerceAtLeast(0f)
                cup.shuffleT = (cup.shuffleT - dt * 2f).coerceAtLeast(0f)
                if (idx == 0 && cup.hidden && !cup.revealing && cup.display < 0.5f) {
                    cup.revealing = true
                    listener.sound(Sfx.STAR, 1.2f, 0.5f)
                }
                if (cup.revealing) {
                    cup.reveal = (cup.reveal + dt * 3f).coerceAtMost(1f)
                    if (cup.reveal >= 1f) { cup.hidden = false; cup.revealing = false }
                }
            }
        }
    }

    // ============================================================ end conditions

    private fun endWith(s: GameStatus, reason: FailReason, delay: Float) {
        if (pendingEnd >= 0f) return
        pendingStatus = s; failReason = reason; pendingEnd = delay
    }

    private fun checkEnd(dt: Float) {
        if (status != GameStatus.PLAYING) return
        if (pendingEnd >= 0f) {
            pendingEnd -= dt
            if (pendingEnd < 0f) {
                status = pendingStatus
                if (status == GameStatus.WON) celebrate()
                listener.statusChanged(status)
            }
            return
        }
        if (board.isEmpty && marbles.isEmpty() && flying.isEmpty() && tray.all { it.cups.isEmpty() }) {
            endWith(GameStatus.WON, FailReason.JAM, 0.35f)
            return
        }
        val waiting = marbles.count { it.state == MarbleState.PHYSICS || it.state == MarbleState.PATH }
        val anyDropping = marbles.any { it.state == MarbleState.DROPPING || it.state == MarbleState.BOARDING }
        val sinceDrop = time - lastDropTime
        if (beltCount >= capacity && waiting > 0 && !anyDropping && sinceDrop > lapTime * 1.1f) {
            listener.sound(Sfx.FAIL)
            endWith(GameStatus.FAILED, FailReason.JAM, 0.2f)
            return
        }
        if (beltCount >= capacity && !anyDropping && sinceDrop > lapTime * 1.3f) {
            endWith(GameStatus.FAILED, FailReason.JAM, 0.2f)
            return
        }
        if (!inIntro && waiting == 0 && !anyDropping && flying.isEmpty() && !board.hasTappable() &&
            (beltCount > 0 || !board.isEmpty) && sinceDrop > lapTime * 1.25f
        ) endWith(GameStatus.FAILED, FailReason.NO_MOVES, 0.2f)
    }

    private fun celebrate() {
        val colors = intArrayOf(0xFFFF4D5E.toInt(), 0xFFFFD23F.toInt(), 0xFF33C8F5.toInt(), 0xFF3CCB5A.toInt(), 0xFF9B5CFF.toInt(), 0xFFFF7AC8.toInt())
        repeat(110) {
            val left = it % 2 == 0
            val x = if (left) -10f else layout.worldW + 10f
            val y = layout.worldH * 0.62f
            val vx = (if (left) 1 else -1) * (250f + Random.nextFloat() * 520f)
            val vy = -(700f + Random.nextFloat() * 700f)
            particles.emit(x, y, vx, vy, 2.6f + Random.nextFloat(), 14f + Random.nextFloat() * 10f, colors[it % colors.size], Particles.CONFETTI, 700f, (Random.nextFloat() - 0.5f) * 900f)
        }
        listener.haptic(Haptic.SUCCESS)
    }

    // ============================================================ results & hints

    val stars: Int get() {
        val ratio = peakBelt / capacity.toFloat()
        var s = when { ratio <= 0.62f -> 3; ratio <= 0.86f -> 2; else -> 1 }
        if (boostersUsed > 0) s = min(s, 2)
        if (continued) s = 1
        return s
    }

    fun coinReward(): Int = (10 + spec.number / 40).coerceAtMost(60) + stars * 3 + comboCoins + (if (spec.isBoss) 25 else 0)

    /** World centre of a suggested block to tap (tutorial hand). */
    fun hintTarget(): FloatArray? {
        for (id in spec.plan) {
            val i = board.locate(id)
            if (i < 0) continue
            if (board.canTap(i)) {
                val v = visuals[i] ?: continue
                return floatArrayOf(layout.cellCenterX(v.col), layout.cellCenterY(v.row + v.dropY))
            }
        }
        return null
    }

    companion object {
        const val INTRO_MACHINE = 0.55f
        fun easeOut(t: Float) = 1 - (1 - t) * (1 - t)
    }
}
