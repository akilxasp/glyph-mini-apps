package com.akil.glyphlife

import kotlin.random.Random

/**
 * Engine self-check. Run: `kotlinc SnakeGame.kt SnakeGameCheck.kt -include-runtime -d t.jar && java -jar t.jar`.
 * Verifies: in-bounds, no self-overlap, food off-body, snake grows, and death triggers an
 * explosion that later ends (game recovers to playing).
 */
fun main() {
    val n = 13
    val g = SnakeGame(n, Random(7))
    var maxLen = g.bodyCells().size
    var sawExplosion = false
    var sawRecovery = false
    var sinceProgress = 0
    var prevLen = g.bodyCells().size
    val stuckBound = n * n + 10   // watchdog fires at n*n, then explodes

    repeat(8000) {
        val wasExploding = g.isExploding()
        g.step()

        val cells = g.bodyCells()
        for (c in cells) check(c in 0 until n * n) { "cell out of bounds: $c" }
        check(cells.toHashSet().size == cells.size) { "snake overlaps itself" }
        check(g.foodY * n + g.foodX !in cells) { "food spawned on body" }

        // Progress = ate (grew) or exploding. Never stuck longer than the watchdog bound.
        if (cells.size > prevLen || g.isExploding()) sinceProgress = 0 else sinceProgress++
        prevLen = cells.size
        check(sinceProgress <= stuckBound) { "snake stuck $sinceProgress ticks without progress" }

        if (g.isExploding()) sawExplosion = true
        if (wasExploding && !g.isExploding()) sawRecovery = true   // blast ended → reset
        if (!g.isExploding()) maxLen = maxOf(maxLen, cells.size)
    }

    check(maxLen > 3) { "snake never grew — eating is broken" }
    check(sawExplosion) { "snake never trapped/exploded in 8000 steps" }
    check(sawRecovery) { "explosion never finished — snake stuck blown up" }

    // Detonate (tile-off): must explode, then finish blank and STAY finished (no respawn).
    val d = SnakeGame(n, Random(3))
    d.detonate()
    var guard = 0
    while (!d.isFinished() && guard++ < 200) { check(d.isExploding()) { "detonate not exploding" }; d.step() }
    check(d.isFinished()) { "detonate never finished" }
    check(d.bodyCells().isEmpty()) { "finished snake should be blank" }
    d.step(); d.step()
    check(d.isFinished() && d.bodyCells().isEmpty()) { "finished must be terminal (no respawn)" }
    println("OK: grew to $maxLen, exploded and recovered, detonate ends blank, invariants held")
}
