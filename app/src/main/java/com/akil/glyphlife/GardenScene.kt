package com.akil.glyphlife

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ambient garden: one flower sways in wind (two layered sines + occasional gusts),
 * rain shows up randomly and dampens the sway, dandelion seeds blow away on gusts.
 * Tile-off → wilt: droop, dim, petals fall, then blank. Pure Kotlin — no Android deps.
 */
class GardenScene(
    val n: Int,
    val flower: Flower,
    style: FlowerSprites.Style = FlowerSprites.Style.DETAILED,
    private val rng: Random = Random.Default,
    showClouds: Boolean = true
) {

    private val sprite = flower.headFor(style)   // chosen head pixels
    fun headPixels(): List<HeadPx> = sprite

    // Dim drifting background clouds — a mix of puffs and wisps. Brightness stays below every
    // real element so max-blend keeps them behind the flower automatically.
    data class Cloud(var x: Double, val y: Int, val speed: Double, val px: List<HeadPx>)
    val clouds = mutableListOf<Cloud>()

    init {
        if (showClouds) {
            clouds.add(Cloud(rng.nextDouble() * n, 0, 0.045, WISP))
            clouds.add(Cloud(rng.nextDouble() * n, 1, 0.060, WISP))
            clouds.add(Cloud(rng.nextDouble() * n, 2, 0.030, WISP))
        }
    }

    enum class Mode { PLAYING, WILTING, FINISHED }
    var mode = Mode.PLAYING; private set

    private var t = 0                    // tick counter
    private var gust = 0.0               // decaying gust amplitude
    private var wilt = 0                 // wilt progress

    // Rain.
    var raining = false; private set
    var rainWind = 0.0; private set     // horizontal slant, oscillates over time
    private var rainLeft = 0
    private var sparkleLeft = 0
    data class Drop(var x: Double, var y: Int)
    val drops = mutableListOf<Drop>()

    // Detached petals / dandelion seeds drifting away.
    data class Particle(var x: Double, var y: Double, val vx: Double, val vy: Double, val b: Int)
    val particles = mutableListOf<Particle>()

    // Head pixels currently blown off (dandelion), by index; regrow after a while.
    private val blown = mutableSetOf<Int>()
    private var regrowAt = 0

    companion object {
        const val WILT_TICKS = 34
        const val CLOUD_MAX_B = 30                         // dim ceiling — tune on device (LED perception)
        private const val CLOUD_EDGE = 16
        private const val RAIN_START_CHANCE = 1.0 / 900   // ~2 min @120ms ticks
        private const val GUST_CHANCE = 1.0 / 260

        // dx, dy offsets from the cloud's top-left; dy>=0 (down). b = brightness.
        private val WISP = listOf(
            HeadPx(0, 0, CLOUD_EDGE), HeadPx(1, 0, CLOUD_MAX_B), HeadPx(2, 0, CLOUD_MAX_B), HeadPx(3, 0, CLOUD_EDGE)
        )
    }

    val groundY get() = n - 1
    val baseX get() = n / 2
    val stemTopY get() = groundY - flower.stemH

    /** Horizontal sway offset for a stem row; rowFrac 0 at base → 1 at top. Quadratic bend. */
    fun swayOffset(rowFrac: Double): Int {
        if (mode != Mode.PLAYING) return wiltLean(rowFrac)
        var a = 1.1 + 0.7 * sin(t * 0.045) + 0.45 * sin(t * 0.013 + 1.0) + gust
        if (raining) a *= 0.4
        return (a * sin(t * 0.22) * rowFrac * rowFrac).roundToInt()
    }

    private fun wiltLean(rowFrac: Double): Int =
        (2.0 * (wilt.toDouble() / WILT_TICKS) * rowFrac * rowFrac).roundToInt()

    /** Extra head droop (down-shift): 1px in rain, grows during wilt. */
    fun droop(): Int = when {
        mode == Mode.WILTING -> 1 + (3.0 * wilt / WILT_TICKS).roundToInt()
        raining -> 1
        else -> 0
    }

    /** Brightness multiplier 0..1 (fades out while wilting). */
    fun dim(): Double = if (mode == Mode.WILTING) (1.0 - wilt.toDouble() / WILT_TICKS).coerceAtLeast(0.0) else 1.0

    fun isSparkling() = sparkleLeft > 0
    fun isBlown(headIndex: Int) = headIndex in blown
    fun rngPick(bound: Int) = rng.nextInt(bound)

    fun wiltNow() { if (mode == Mode.PLAYING) { mode = Mode.WILTING; wilt = 0 } }

    fun step() {
        if (mode == Mode.FINISHED) return
        t++
        stepClouds()   // drift continues through play and wilt

        if (mode == Mode.WILTING) {
            wilt++
            // shed a petal every 3 ticks
            if (wilt % 3 == 0) shedPetal(fall = true)
            stepParticles()
            if (wilt >= WILT_TICKS && particles.isEmpty()) mode = Mode.FINISHED
            return
        }

        // Weather.
        if (!raining && rng.nextDouble() < RAIN_START_CHANCE) {
            raining = true
            rainLeft = 60 + rng.nextInt(100)   // ~7–19 s @120ms
            repeat(4) { drops.add(Drop(rng.nextDouble() * n, -rng.nextInt(n))) }
        }
        if (raining) {
            rainWind = 0.8 * sin(t * 0.015)    // slant swings direction over the storm
            for (d in drops) {
                d.x += rainWind
                if (d.x < 0) d.x += n else if (d.x >= n) d.x -= n
                d.y++
                if (d.y > groundY) { d.y = -1 - rng.nextInt(3); d.x = rng.nextDouble() * n }
            }
            if (--rainLeft <= 0) { raining = false; drops.clear(); sparkleLeft = 8 }
        }
        if (sparkleLeft > 0) sparkleLeft--

        // Gusts.
        if (!raining && rng.nextDouble() < GUST_CHANCE) {
            gust = 2.2
            if (flower.key == "dandelion") { shedPetal(fall = false); shedPetal(fall = false) }
        }
        gust *= 0.92

        // Dandelion regrow.
        if (blown.isNotEmpty() && t >= regrowAt) blown.clear()

        stepParticles()
    }

    private fun stepClouds() {
        for (c in clouds) { c.x += c.speed; if (c.x > n) c.x = -4.0 }   // drift right, wrap off-screen
    }

    private fun shedPetal(fall: Boolean) {
        val candidates = sprite.indices.filter { it !in blown && sprite[it].b > 100 }
        if (candidates.isEmpty()) return
        val i = candidates[rng.nextInt(candidates.size)]
        blown.add(i)
        regrowAt = t + 200
        val p = sprite[i]
        val hx = (baseX + p.dx).toDouble(); val hy = (stemTopY + p.dy).toDouble()
        particles.add(
            if (fall) Particle(hx, hy, (rng.nextDouble() - 0.5) * 0.4, 0.55, 120)     // wilting: flutter down
            else Particle(hx, hy, 0.7 + rng.nextDouble() * 0.5, -0.35, 120)           // gust: drift up + away
        )
    }

    private fun stepParticles() {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx; p.y += p.vy
            if (p.x < -1 || p.x > n || p.y < -1 || p.y > n) it.remove()
        }
    }
}
