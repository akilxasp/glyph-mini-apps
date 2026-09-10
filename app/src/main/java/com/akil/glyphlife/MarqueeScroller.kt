package com.akil.glyphlife

/**
 * Scrolls a pre-rasterized text strip right-to-left across an n×n window, exactly [totalPasses]
 * times, then latches done. Pure Kotlin — no Android deps, so it has a main() self-check.
 *
 * [strip] holds stripWidth × n brightness values, indexed col*n + row.
 * A pass runs from fully-off-right to fully-off-left, so it starts and ends on a blank frame
 * and every source column crosses the window exactly once: travelPerPass = stripWidth + n.
 */
class MarqueeScroller(
    val n: Int,
    val stripWidth: Int,
    private val strip: IntArray,
    val totalPasses: Int = 3
) {
    private val travelPerPass = stripWidth + n
    private var position = 0
    var passCount = 0; private set

    fun isDone() = passCount >= totalPasses

    fun step() {
        if (isDone()) return
        position++
        if (position >= travelPerPass) {
            position -= travelPerPass
            passCount++
        }
    }

    /** Current n×n frame. Columns outside the strip are blank — that's the inter-pass gap. */
    fun frame(): IntArray {
        val out = IntArray(n * n)
        val offset = -n + position
        for (x in 0 until n) {
            val src = offset + x
            if (src < 0 || src >= stripWidth) continue
            for (y in 0 until n) out[y * n + x] = strip[src * n + y]
        }
        return out
    }
}
