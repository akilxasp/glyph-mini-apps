package com.akil.glyphlife

import kotlin.math.abs
import kotlin.random.Random

/**
 * Fixed-line shooter on an n×n grid. The ship holds the bottom line; ranks of invaders march
 * sideways, drop a row at the wall, and lob bombs. Self-playing: the ship slides under the
 * nearest live column, fires, and dodges bombs in its own column.
 *
 * Ship bombed, or invaders reaching the bottom line → Blast, then reset. Clearing a wave respawns
 * a faster one. Pure Kotlin, no Android deps — see ShooterGameCheck.
 */
class ShooterGame(val n: Int, private val rng: Random = Random.Default) {

    companion object {
        const val COLS = 5
        const val ROWS = 3
        private const val GAP = 2              // spacing between invaders
        private const val TOP = 1              // formation's starting top row
        private const val BOMB_CHANCE = 1.0 / 30
        private const val FIRE_COOLDOWN = 4
        private const val DODGE_ROWS = 5       // react to a bomb this far above
        // Must be well under a typical lifetime (~475 ticks) or the turret dies before it ever
        // earns one — the charge resets on death.
        private const val MISSILE_EVERY = 320  // ~42 s at 130 ms
    }

    val blast = Blast(n)
    private var finished = false

    /** alive[row][col] */
    private val alive = Array(ROWS) { BooleanArray(COLS) { true } }
    private var ox = 0                          // formation offset
    private var oy = TOP
    private var dir = 1
    private var marchEvery = 8
    private var tick = 0

    var shipX = n / 2; private set
    val shipY get() = n - 1

    class Shot(var x: Int, var y: Int)
    private val bullets = mutableListOf<Shot>()
    private val bombs = mutableListOf<Shot>()

    private var cooldown = 0
    private var charge = 0            // builds toward the next missile
    private var missile: Shot? = null
    private var flashRow = -1        // row a missile just cleared; lit for FLASH_TICKS, then -1
    private var flashTicks = 0

    init { reset() }

    fun isFinished() = finished
    fun isExploding() = blast.exploding
    fun bullets(): List<Shot> = bullets
    fun bombs(): List<Shot> = bombs
    fun missile(): Shot? = missile
    fun isFlashing() = flashTicks > 0
    fun flashRow() = flashRow
    fun invaderX(col: Int) = ox + col * GAP
    fun invaderY(row: Int) = oy + row * GAP
    fun isAlive(row: Int, col: Int) = alive[row][col]
    fun aliveCount() = alive.sumOf { r -> r.count { it } }

    /** Widest x the formation can reach without leaving the board. */
    private val maxOx get() = n - ((COLS - 1) * GAP + 1)

    private fun reset() {
        for (r in alive) r.fill(true)
        ox = 0; oy = TOP; dir = 1; marchEvery = 8; tick = 0
        shipX = n / 2; cooldown = 0
        charge = 0; missile = null; flashRow = -1; flashTicks = 0
        bullets.clear(); bombs.clear()
        blast.clear(); finished = false
    }

    private fun newWave() {
        for (r in alive) r.fill(true)
        ox = 0; oy = TOP; dir = 1
        marchEvery = (marchEvery - 1).coerceAtLeast(3)   // each wave marches faster
        bullets.clear(); bombs.clear()
        missile = null
    }

    /** Tile-off: blow up from the ship and stay blank. */
    fun detonate() = blast.ignite(shipX, shipY, stopAfter = true)

    /** Essential Key: launch a missile now, if one isn't already in flight. Resets the charge. */
    fun fireMissile() {
        if (missile != null || blast.exploding || finished) return
        missile = Shot(shipX, shipY - 1)
        charge = 0
    }

    private fun die() = blast.ignite(shipX, shipY)

    fun step() {
        if (finished) return
        if (blast.exploding) {
            if (blast.step()) { if (blast.stopAfter) finished = true else reset() }
            return
        }
        tick++
        if (flashTicks > 0) flashTicks--   // brief lit line where the last missile struck

        marchInvaders()
        if (invadersLanded()) { die(); return }

        dropBombs()
        moveShots()
        moveMissile()
        flyShip()
        bulletsHitInvaders()
        if (bombsHitShip()) { die(); return }

        if (aliveCount() == 0) newWave()
    }

    /** Every MISSILE_EVERY ticks the turret earns a missile that clears ONE line — the lowest live
     *  rank it climbs into. Peels the formation bottom-up, one missile per row. */
    private fun moveMissile() {
        val m = missile
        if (m == null) {
            if (++charge >= MISSILE_EVERY) { missile = Shot(shipX, shipY - 1); charge = 0 }
            return
        }
        m.y--
        val low = (ROWS - 1 downTo 0).firstOrNull { r -> (0 until COLS).any { alive[r][it] } }
        if (low == null || m.y <= invaderY(low)) {
            if (low != null) {
                for (c in 0 until COLS) alive[low][c] = false   // one rank only
                flashRow = invaderY(low); flashTicks = 2
            }
            missile = null
        }
    }

    private fun marchInvaders() {
        // Speed up as the ranks thin — the classic tell that you're losing.
        val every = (marchEvery - (ROWS * COLS - aliveCount()) / 3).coerceAtLeast(2)
        if (tick % every != 0) return
        val next = ox + dir
        if (next < 0 || next > maxOx) { dir = -dir; oy++ } else ox = next
    }

    private fun invadersLanded() =
        (0 until ROWS).any { r -> (0 until COLS).any { c -> alive[r][c] && invaderY(r) >= shipY } }

    private fun dropBombs() {
        if (rng.nextDouble() >= BOMB_CHANCE) return
        // Only the lowest invader in a column can shoot.
        val shooters = (0 until COLS).mapNotNull { c ->
            (ROWS - 1 downTo 0).firstOrNull { r -> alive[r][c] }?.let { r -> c to r }
        }
        if (shooters.isEmpty()) return
        val (c, r) = shooters[rng.nextInt(shooters.size)]
        bombs += Shot(invaderX(c), invaderY(r) + 1)
    }

    private fun moveShots() {
        bullets.forEach { it.y-- }
        bullets.removeAll { it.y < 0 }
        bombs.forEach { it.y++ }
        bombs.removeAll { it.y > shipY }
    }

    /** A hull centred at x is unsafe if any incoming bomb can clip it. */
    private fun unsafe(x: Int) = bombs.any { b ->
        shipY - b.y in 0..DODGE_ROWS && abs(b.x - x) <= 1
    }

    private fun flyShip() {
        if (cooldown > 0) cooldown--

        // Standing in a bomb's path? Walk to the nearest safe column and stay there.
        // (A one-step twitch is fatal: the ship steps out, then chases its target back in.)
        if (unsafe(shipX)) {
            val safe = (0 until n).filter { !unsafe(it) }.minByOrNull { abs(it - shipX) }
            if (safe != null && safe != shipX) shipX += if (safe > shipX) 1 else -1
            return
        }

        // Otherwise slide under the nearest live column — but never step into a bomb's path.
        val targets = (0 until COLS).filter { c -> (0 until ROWS).any { alive[it][c] } }
        val targetX = targets.minByOrNull { abs(invaderX(it) - shipX) }?.let { invaderX(it) } ?: return
        val next = shipX + if (targetX > shipX) 1 else if (targetX < shipX) -1 else 0
        if (next != shipX && !unsafe(next)) shipX = next

        if (shipX == targetX && cooldown == 0) {
            bullets += Shot(shipX, shipY - 1)
            cooldown = FIRE_COOLDOWN
        }
    }

    private fun bulletsHitInvaders(): Boolean {
        val dead = mutableListOf<Shot>()
        for (b in bullets) {
            outer@ for (r in 0 until ROWS) for (c in 0 until COLS) {
                if (!alive[r][c]) continue
                if (b.x == invaderX(c) && b.y == invaderY(r)) {
                    alive[r][c] = false
                    dead += b
                    break@outer
                }
            }
        }
        bullets.removeAll(dead)
        return dead.isNotEmpty()
    }

    /** Hull is the drawn 3-wide base — a bomb clipping a wing counts. */
    private fun bombsHitShip() = bombs.any { abs(it.x - shipX) <= 1 && it.y == shipY }
}
