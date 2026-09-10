package com.akil.glyphlife

/**
 * Engine self-check. Run:
 *   kotlinc MarqueeScroller.kt MarqueeScrollerCheck.kt -include-runtime -d t.jar && java -jar t.jar
 */
fun main() {
    val n = 13

    // Strip: every column fully lit, so coverage is easy to assert.
    fun solidStrip(w: Int) = IntArray(w * n) { 255 }

    // 1-4: exact 3 passes, valid frames, blank start, idempotent terminal.
    run {
        val w = 20
        val m = MarqueeScroller(n, w, solidStrip(w))
        check(m.frame().all { it == 0 }) { "first frame should be blank (text off-right)" }

        var steps = 0
        while (!m.isDone() && steps < 10_000) {
            val f = m.frame()
            check(f.size == n * n) { "frame wrong size" }
            check(f.all { it == 0 || it == 255 }) { "frame values must be 0 or 255" }
            check(m.passCount <= 3) { "passCount overshot: ${m.passCount}" }
            m.step(); steps++
        }
        check(m.isDone()) { "never finished" }
        check(m.passCount == 3) { "expected 3 passes, got ${m.passCount}" }
        check(steps == 3 * (w + n)) { "expected ${3 * (w + n)} steps, got $steps" }

        m.step(); m.step()
        check(m.isDone() && m.passCount == 3) { "done must be terminal/idempotent" }
    }

    // 5: full coverage — every source column shows up during one pass.
    run {
        val w = 9
        val m = MarqueeScroller(n, w, solidStrip(w))
        val seen = BooleanArray(w)
        repeat(w + n) {
            val offset = -n + it
            for (x in 0 until n) {
                val src = offset + x
                if (src in 0 until w) seen[src] = true
            }
            m.step()
        }
        check(seen.all { it }) { "not every source column crossed the window in one pass" }
    }

    // 6: direction — a single lit column drifts right-to-left (its x strictly decreases).
    run {
        val w = 5
        val strip = IntArray(w * n)
        for (y in 0 until n) strip[2 * n + y] = 255      // only column 2 lit
        val m = MarqueeScroller(n, w, strip)
        var prevX = Int.MAX_VALUE
        repeat(w + n) {
            val f = m.frame()
            val x = (0 until n).firstOrNull { col -> (0 until n).any { row -> f[row * n + col] > 0 } }
            if (x != null) {
                check(x < prevX) { "column moved right ($prevX -> $x); must scroll right-to-left" }
                prevX = x
            }
            m.step()
        }
        check(prevX != Int.MAX_VALUE) { "lit column never appeared" }
    }

    // 7: text narrower than the window still completes, and lights something.
    run {
        val w = 4
        val m = MarqueeScroller(n, w, solidStrip(w))
        var steps = 0; var everLit = false
        while (!m.isDone() && steps < 10_000) {
            if (m.frame().any { it > 0 }) everLit = true
            m.step(); steps++
        }
        check(steps == 3 * (w + n)) { "short text: wrong step count $steps" }
        check(everLit) { "short text never lit a pixel" }
    }

    println("OK: 3 exact passes, right-to-left, full coverage, short strips safe")
}
