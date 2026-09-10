package com.akil.glyphlife

/**
 * Pixel-art flower catalog for the 13×13 matrix. Monochrome — flowers read by
 * silhouette + brightness layers (bright petals, mid tones, dim centers).
 *
 * Head pixels are (dx, dy) relative to the STEM TOP; dy negative = up. The stem grows
 * from the ground upward; the renderer bends it in the wind and shifts the head by the top offset.
 */
data class HeadPx(val dx: Int, val dy: Int, val b: Int)

class Flower(
    val key: String,
    val stemH: Int,
    val stemB: Int,
    val head: List<HeadPx>,
    val headSolid: List<HeadPx> = head
) {
    fun headFor(style: FlowerSprites.Style) =
        if (style == FlowerSprites.Style.SOLID) headSolid else head
}

object FlowerSprites {
    enum class Style { DETAILED, SOLID }

    private const val PETAL = 255
    private const val SOFT = 150
    private const val CORE = 70
    private const val B = 255

    // Liked as-is: sparse airy seed puff.
    val DANDELION = Flower(
        key = "dandelion", stemH = 5, stemB = 90,
        head = listOf(
            HeadPx(0, -1, SOFT), HeadPx(0, -3, SOFT),
            HeadPx(-2, -1, SOFT), HeadPx(2, -1, SOFT),
            HeadPx(-1, -2, SOFT), HeadPx(1, -2, SOFT),
            HeadPx(-2, -3, SOFT), HeadPx(2, -3, SOFT),
            HeadPx(0, -2, CORE)
        ),
        headSolid = listOf(
            HeadPx(-1, -4, B), HeadPx(0, -4, B), HeadPx(1, -4, B),
            HeadPx(-2, -3, B), HeadPx(-1, -3, B), HeadPx(0, -3, B), HeadPx(1, -3, B), HeadPx(2, -3, B),
            HeadPx(-2, -2, B), HeadPx(-1, -2, B), HeadPx(0, -2, B), HeadPx(1, -2, B), HeadPx(2, -2, B),
            HeadPx(-1, -1, B), HeadPx(0, -1, B), HeadPx(1, -1, B)
        )
    )

    // Liked as-is: dense layered bloom with a dark eye.
    val ROSE = Flower(
        key = "rose", stemH = 4, stemB = 90,
        head = listOf(
            HeadPx(-1, -1, SOFT), HeadPx(0, -1, PETAL), HeadPx(1, -1, SOFT),
            HeadPx(-1, -2, PETAL), HeadPx(0, -2, CORE), HeadPx(1, -2, PETAL),
            HeadPx(-1, -3, SOFT), HeadPx(0, -3, PETAL), HeadPx(1, -3, SOFT)
        ),
        headSolid = listOf(
            HeadPx(-1, -3, B), HeadPx(0, -3, B), HeadPx(1, -3, B),
            HeadPx(-2, -2, B), HeadPx(-1, -2, B), HeadPx(1, -2, B), HeadPx(2, -2, B),
            HeadPx(-1, -1, B), HeadPx(0, -1, B), HeadPx(1, -1, B),
            HeadPx(0, -2, CORE)
        )
    )

    // Redrawn: tulip cup with a notched top (two petal tips + a dim dip between).
    val TULIP = Flower(
        key = "tulip", stemH = 4, stemB = 90,
        head = listOf(
            HeadPx(-1, -4, PETAL), HeadPx(0, -4, SOFT), HeadPx(1, -4, PETAL),  // two tips, notch
            HeadPx(-2, -3, PETAL), HeadPx(-1, -3, SOFT), HeadPx(0, -3, SOFT), HeadPx(1, -3, SOFT), HeadPx(2, -3, PETAL),
            HeadPx(-2, -2, PETAL), HeadPx(-1, -2, SOFT), HeadPx(0, -2, SOFT), HeadPx(1, -2, SOFT), HeadPx(2, -2, PETAL),
            HeadPx(-1, -1, SOFT), HeadPx(0, -1, SOFT), HeadPx(1, -1, SOFT)     // cup base to stem
        ),
        headSolid = listOf(
            HeadPx(-1, -4, B), HeadPx(1, -4, B),
            HeadPx(-2, -3, B), HeadPx(-1, -3, B), HeadPx(0, -3, B), HeadPx(1, -3, B), HeadPx(2, -3, B),
            HeadPx(-2, -2, B), HeadPx(-1, -2, B), HeadPx(0, -2, B), HeadPx(1, -2, B), HeadPx(2, -2, B),
            HeadPx(-1, -1, B), HeadPx(0, -1, B), HeadPx(1, -1, B)
        )
    )

    // Redrawn: 8 separated petals around a dim eye — reads clearly as a daisy.
    val DAISY = Flower(
        key = "daisy", stemH = 4, stemB = 90,
        head = listOf(
            HeadPx(0, -5, PETAL),
            HeadPx(-1, -4, SOFT), HeadPx(1, -4, SOFT),
            HeadPx(-2, -3, PETAL), HeadPx(0, -3, CORE), HeadPx(2, -3, PETAL),
            HeadPx(-1, -2, SOFT), HeadPx(1, -2, SOFT),
            HeadPx(0, -1, PETAL)
        ),
        headSolid = listOf(
            HeadPx(0, -5, B), HeadPx(0, -4, B),
            HeadPx(-2, -3, B), HeadPx(-1, -3, B), HeadPx(1, -3, B), HeadPx(2, -3, B),
            HeadPx(0, -2, B), HeadPx(0, -1, B),
            HeadPx(0, -3, CORE)
        )
    )

    // Redrawn: big round head, wide dark seed core ringed by bright petals.
    val SUNFLOWER = Flower(
        key = "sunflower", stemH = 3, stemB = 90,
        head = listOf(
            HeadPx(-1, -5, PETAL), HeadPx(0, -5, PETAL), HeadPx(1, -5, PETAL),
            HeadPx(-2, -4, PETAL), HeadPx(-1, -4, CORE), HeadPx(0, -4, CORE), HeadPx(1, -4, CORE), HeadPx(2, -4, PETAL),
            HeadPx(-2, -3, PETAL), HeadPx(-1, -3, CORE), HeadPx(0, -3, SOFT), HeadPx(1, -3, CORE), HeadPx(2, -3, PETAL),
            HeadPx(-2, -2, PETAL), HeadPx(-1, -2, CORE), HeadPx(0, -2, CORE), HeadPx(1, -2, CORE), HeadPx(2, -2, PETAL),
            HeadPx(-1, -1, PETAL), HeadPx(0, -1, PETAL), HeadPx(1, -1, PETAL)
        ),
        headSolid = listOf(
            HeadPx(-1, -5, B), HeadPx(0, -5, B), HeadPx(1, -5, B),
            HeadPx(-2, -4, B), HeadPx(2, -4, B),
            HeadPx(-2, -3, B), HeadPx(2, -3, B),
            HeadPx(-2, -2, B), HeadPx(2, -2, B),
            HeadPx(-1, -1, B), HeadPx(0, -1, B), HeadPx(1, -1, B),
            HeadPx(-1, -4, SOFT), HeadPx(0, -4, SOFT), HeadPx(1, -4, SOFT),
            HeadPx(-1, -3, SOFT), HeadPx(0, -3, SOFT), HeadPx(1, -3, SOFT),
            HeadPx(-1, -2, SOFT), HeadPx(0, -2, SOFT), HeadPx(1, -2, SOFT)
        )
    )

    // New (replaces cactus): a trumpet lily flaring open upward, three petal tips.
    val LILY = Flower(
        key = "lily", stemH = 4, stemB = 90,
        head = listOf(
            HeadPx(-2, -5, PETAL), HeadPx(0, -5, PETAL), HeadPx(2, -5, PETAL),  // three flared tips
            HeadPx(-1, -4, SOFT), HeadPx(0, -4, SOFT), HeadPx(1, -4, SOFT),
            HeadPx(-1, -3, PETAL), HeadPx(0, -3, CORE), HeadPx(1, -3, PETAL),   // open mouth, dark throat
            HeadPx(0, -2, SOFT), HeadPx(0, -1, SOFT)                            // tube down to stem
        ),
        headSolid = listOf(
            HeadPx(-2, -5, B), HeadPx(0, -5, B), HeadPx(2, -5, B),
            HeadPx(-1, -4, B), HeadPx(0, -4, B), HeadPx(1, -4, B),
            HeadPx(-1, -3, B), HeadPx(1, -3, B),
            HeadPx(0, -2, B), HeadPx(0, -1, B),
            HeadPx(0, -3, CORE)
        )
    )

    val ALL = listOf(DANDELION, ROSE, TULIP, DAISY, SUNFLOWER, LILY)

    fun byKey(key: String): Flower = ALL.firstOrNull { it.key == key } ?: DANDELION
}
