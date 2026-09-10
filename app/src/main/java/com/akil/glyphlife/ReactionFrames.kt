package com.akil.glyphlife

/**
 * Static matrix frames for the reaction timer. Pure Kotlin — flat int[n*n], index y*n+x, 0..255.
 * No Android deps, so it carries a main() self-check (ReactionFramesCheck).
 */
object ReactionFrames {
    private const val READY = 100   // dim, so it never reads as the GO flash
    private const val ON = 255

    /** Small centred 3×3 square: "press to start". */
    fun ready(n: Int): IntArray {
        val f = IntArray(n * n)
        val c = n / 2
        for (y in c - 1..c + 1) for (x in c - 1..c + 1) f[y * n + x] = READY
        return f
    }

    /** Whole matrix lit: "press NOW". */
    fun go(n: Int) = IntArray(n * n) { ON }

    /** Diagonal X: false start (jumped the gun). */
    fun cross(n: Int): IntArray {
        val f = IntArray(n * n)
        for (i in 0 until n) { f[i * n + i] = ON; f[i * n + (n - 1 - i)] = ON }
        return f
    }
}
