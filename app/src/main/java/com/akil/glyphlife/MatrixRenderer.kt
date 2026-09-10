package com.akil.glyphlife

/**
 * Renders a SnakeGame to the flat int[] the Glyph Matrix expects (169 pixels on the 4a Pro).
 * The blast draws itself; this only knows about the snake.
 */
object MatrixRenderer {
    // Per-pixel brightness. ponytail: 255 = full white per LED. Tune if the matrix scale differs.
    const val ON = 255
    const val OFF = 0

    fun toFrame(game: SnakeGame): IntArray = when {
        game.isFinished() -> IntArray(game.n * game.n) { OFF }   // blank after detonation
        else -> game.blast.render() ?: snake(game)
    }

    private fun snake(game: SnakeGame): IntArray {
        val buf = IntArray(game.n * game.n) { OFF }
        for (cell in game.bodyCells()) buf[cell] = ON
        buf[game.foodY * game.n + game.foodX] = ON
        return buf
    }
}
