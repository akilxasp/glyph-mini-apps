package com.akil.glyphlife

private fun check(cond: Boolean, msg: String) { if (!cond) throw AssertionError(msg) }

/** kotlinc ReactionFrames.kt ReactionFramesCheck.kt -include-runtime -d t.jar && java -jar t.jar */
fun main() {
    val n = 13

    val ready = ReactionFrames.ready(n)
    check(ready.size == n * n, "ready size")
    check(ready.all { it == 0 || it == 100 }, "ready values ∈ {0,100}")
    check(ready.count { it > 0 } == 9, "ready is a 3×3 block")
    check(ready[6 * n + 6] == 100, "ready centre lit")
    check(ready[0] == 0, "ready corner dark")

    val go = ReactionFrames.go(n)
    check(go.size == n * n && go.all { it == 255 }, "go all on")

    val x = ReactionFrames.cross(n)
    check(x.size == n * n, "cross size")
    check(x[0] == 255 && x[(n - 1) * n + (n - 1)] == 255, "cross main-diagonal ends")
    check(x[0 * n + (n - 1)] == 255 && x[(n - 1) * n + 0] == 255, "cross anti-diagonal ends")
    check(x[6 * n + 6] == 255, "cross centre (diagonals meet)")
    check(x.count { it > 0 } == 2 * n - 1, "cross lit = 2n-1 (overlap at centre)")

    println("OK reaction frames")
}
