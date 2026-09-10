package com.akil.glyphlife

import kotlin.math.abs
import kotlin.random.Random

/**
 * Snake on an n×n grid. Greedy AI: the head steps toward the food, avoiding walls and its own
 * body. When boxed in with no safe move, it EXPLODES (full-screen flash + expanding shockwave),
 * then resets. Eating grows the tail by one. No input — pure engine, shared by toy and tile.
 */
class SnakeGame(val n: Int, private val rng: Random = Random.Default) {

    // Head is body[0]. Cells encoded as y*n + x.
    private val body = ArrayDeque<Int>()
    private var dx = 1; private var dy = 0
    var foodX = 0; private set
    var foodY = 0; private set

    private var sinceEat = 0
    private val noEatLimit = n * n   // greedy AI orbiting food it can't reach → break the livelock

    val blast = Blast(n)
    private var finished = false

    init { reset() }

    fun seed() = reset()

    private fun reset() {
        body.clear()
        val cy = n / 2
        for (x in intArrayOf(n / 2, n / 2 - 1, n / 2 - 2)) body.addLast(cy * n + x)
        dx = 1; dy = 0
        sinceEat = 0
        blast.clear(); finished = false
        spawnFood()
    }

    fun isFinished() = finished

    /** Tile-off: blow up now from the head and stay blank when the blast ends (no respawn). */
    fun detonate() {
        val head = body.firstOrNull()
        if (head == null) { finished = true; return }
        blast.ignite(head % n, head / n, stopAfter = true)
    }

    private fun spawnFood() {
        val occupied = body.toHashSet()
        val free = (0 until n * n).filter { it !in occupied }
        if (free.isEmpty()) { reset(); return }
        val cell = free[rng.nextInt(free.size)]
        foodX = cell % n; foodY = cell / n
    }

    fun isExploding() = blast.exploding

    /** One tick: advance the blast if exploding, else step the snake (exploding on a trap). */
    fun step() {
        if (finished) return

        if (blast.exploding) {
            if (blast.step()) { if (blast.stopAfter) { finished = true; body.clear() } else reset() }
            return
        }

        val head = body.first()
        val hx = head % n; val hy = head / n
        val tail = body.last()

        if (++sinceEat > noEatLimit) {   // stuck circling → detonate and restart
            blast.ignite(hx, hy)
            return
        }

        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
            .filter { (cx, cy) -> !(cx == -dx && cy == -dy) }
            .sortedBy { (cx, cy) -> abs(hx + cx - foodX) + abs(hy + cy - foodY) }

        for ((cx, cy) in dirs) {
            val nx = hx + cx; val ny = hy + cy
            if (nx !in 0 until n || ny !in 0 until n) continue
            val cell = ny * n + nx
            if (cell in body && cell != tail) continue
            dx = cx; dy = cy
            body.addFirst(cell)
            if (nx == foodX && ny == foodY) { spawnFood(); sinceEat = 0 } else body.removeLast()
            return
        }

        // Trapped → blow up from the head.
        blast.ignite(hx, hy)
    }

    fun bodyCells(): List<Int> = body
}
