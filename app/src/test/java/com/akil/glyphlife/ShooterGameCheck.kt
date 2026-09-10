package com.akil.glyphlife

import kotlin.random.Random

/**
 * Engine self-check. Run:
 *   kotlinc Blast.kt ShooterGame.kt ShooterRenderer.kt ShooterGameCheck.kt -include-runtime -d t.jar && java -jar t.jar
 */
fun main() {
    val n = 13
    val g = ShooterGame(n, Random(5))

    var sawShot = false
    var sawBomb = false
    var sawKill = false
    var sawWave = false      // cleared a wave and got a fresh one
    var sawBlast = false
    var sawRecovery = false
    var sawMissile = false
    var sawWipe = false      // missile detonated → line sweep
    var prevAlive = g.aliveCount()

    repeat(30_000) {
        val wasExploding = g.isExploding()
        g.step()

        // Nothing leaves the board.
        check(g.shipX in 0 until n) { "ship off grid: ${g.shipX}" }
        for (b in g.bullets()) check(b.x in 0 until n && b.y in 0 until n) { "bullet off grid" }
        for (b in g.bombs()) check(b.x in 0 until n && b.y in 0 until n) { "bomb off grid" }

        g.missile()?.let { m ->
            sawMissile = true
            check(m.x in 0 until n && m.y in 0 until n) { "missile off grid" }
        }
        if (g.isFlashing()) {
            sawWipe = true
            val row = g.flashRow()
            check(row in 0 until n) { "flash line off grid: $row" }
            check((0 until n).all { x -> ShooterRenderer.toFrame(g)[row * n + x] == 255 }) { "flash line not full-width" }
        }

        if (!g.isExploding()) {
            check(g.aliveCount() > 0) { "wave cleared but no respawn" }
            if (g.bullets().isNotEmpty()) sawShot = true
            if (g.bombs().isNotEmpty()) sawBomb = true
            if (g.aliveCount() < prevAlive) sawKill = true
            if (g.aliveCount() > prevAlive) sawWave = true   // count jumped back up = new wave
            prevAlive = g.aliveCount()
        }
        if (g.isExploding()) sawBlast = true
        if (wasExploding && !g.isExploding()) { sawRecovery = true; prevAlive = g.aliveCount() }
    }

    check(sawShot) { "ship never fired" }
    check(sawBomb) { "invaders never bombed" }
    check(sawKill) { "no invader was ever shot" }
    check(sawWave) { "never cleared a wave" }
    check(sawBlast) { "ship never died in 30000 ticks" }
    check(sawRecovery) { "blast never finished — stuck exploding" }
    check(sawMissile) { "turret never earned a missile" }
    check(sawWipe) { "missile never wiped the formation" }

    // Detonate (tile-off): explodes, then stays finished and blank.
    val d = ShooterGame(n, Random(9))
    d.detonate()
    var guard = 0
    while (!d.isFinished() && guard++ < 500) { check(d.isExploding()); d.step() }
    check(d.isFinished()) { "detonate never finished" }
    check(ShooterRenderer.toFrame(d).all { it == 0 }) { "finished frame not blank" }
    d.step(); d.step()
    check(d.isFinished()) { "finished must be terminal" }

    println("OK: fires, bombs, kills, clears waves, dies and recovers; detonate ends blank")
}
