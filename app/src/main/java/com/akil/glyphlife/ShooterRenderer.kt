package com.akil.glyphlife

/**
 * Renders ShooterGame to the flat int[] the Glyph Matrix expects. Monochrome, so the layers
 * separate by brightness: shots brightest, ship bright, invaders mid. The blast draws itself.
 */
object ShooterRenderer {
    private const val SHOT = 255
    private const val SHIP = 255
    private const val INVADER = 160
    private const val OFF = 0

    fun toFrame(game: ShooterGame): IntArray = when {
        game.isFinished() -> IntArray(game.n * game.n) { OFF }
        else -> game.blast.render() ?: scene(game)
    }

    private fun scene(game: ShooterGame): IntArray {
        val n = game.n
        val buf = IntArray(n * n) { OFF }
        fun put(x: Int, y: Int, v: Int) {
            if (x !in 0 until n || y !in 0 until n) return
            if (v > buf[y * n + x]) buf[y * n + x] = v      // brighter layer wins
        }

        for (r in 0 until ShooterGame.ROWS) for (c in 0 until ShooterGame.COLS) {
            if (game.isAlive(r, c)) put(game.invaderX(c), game.invaderY(r), INVADER)
        }
        for (b in game.bullets()) put(b.x, b.y, SHOT)
        for (b in game.bombs()) put(b.x, b.y, SHOT)

        // Missile: two pixels tall, so it reads as heavier than a bullet.
        game.missile()?.let { m -> put(m.x, m.y, SHOT); put(m.x, m.y + 1, SHOT) }

        // Ship: a 3-wide base with a raised muzzle, so it reads as a turret, not a dot.
        put(game.shipX, game.shipY, SHIP)
        put(game.shipX - 1, game.shipY, SHIP)
        put(game.shipX + 1, game.shipY, SHIP)
        put(game.shipX, game.shipY - 1, SHIP)

        // Missile strike: a brief full-width line on the rank it just cleared.
        if (game.isFlashing()) for (x in 0 until n) put(x, game.flashRow(), SHOT)
        return buf
    }
}
