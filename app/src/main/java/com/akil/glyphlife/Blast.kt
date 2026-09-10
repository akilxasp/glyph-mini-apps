package com.akil.glyphlife

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Death animation shared by the arcade games: a two-frame full-screen flash, then a shockwave
 * ring expanding from the point of death until it clears the far corner.
 *
 * [stopAfter] marks a blast lit by the tile turning off — the caller stays blank instead of
 * restarting. Pure Kotlin, no Android deps.
 */
class Blast(private val n: Int) {

    companion object {
        const val FLASH_FRAMES = 2      // full-screen white at the start of the blast
        const val RING_STEP = 2.5       // shockwave radius growth per tick
        private const val RING_THICKNESS = 1.3
        const val ON = 255
    }

    var exploding = false; private set
    var stopAfter = false; private set
    private var frame = 0
    private var cx = 0
    private var cy = 0

    fun ignite(x: Int, y: Int, stopAfter: Boolean = false) {
        cx = x; cy = y; frame = 0
        exploding = true
        this.stopAfter = stopAfter
    }

    fun clear() { exploding = false; stopAfter = false; frame = 0 }

    /** Advance one tick. Returns true on the single tick the blast finishes. */
    fun step(): Boolean {
        if (!exploding) return false
        frame++
        if ((frame - FLASH_FRAMES) * RING_STEP > n * 1.3) { exploding = false; return true }
        return false
    }

    /** The blast's frame, or null when nothing is exploding. */
    fun render(): IntArray? {
        if (!exploding) return null
        if (frame < FLASH_FRAMES) return IntArray(n * n) { ON }
        val r = (frame - FLASH_FRAMES) * RING_STEP
        val buf = IntArray(n * n)
        for (y in 0 until n) for (x in 0 until n) {
            if (abs(hypot(x - cx.toDouble(), y - cy.toDouble()) - r) <= RING_THICKNESS) buf[y * n + x] = ON
        }
        return buf
    }
}
