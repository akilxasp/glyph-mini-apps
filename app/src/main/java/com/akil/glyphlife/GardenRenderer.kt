package com.akil.glyphlife

import kotlin.math.roundToInt

/**
 * Composites a GardenScene to the flat int[] the Glyph Matrix expects.
 * Layers: ground → stem (bent by wind) → head (drooped/dimmed) → particles → rain.
 * Pure Kotlin — also used by the picker activity for previews.
 */
object GardenRenderer {

    fun toFrame(s: GardenScene): IntArray {
        val n = s.n
        val buf = IntArray(n * n)
        if (s.mode == GardenScene.Mode.FINISHED) return buf
        val dim = s.dim()

        fun set(x: Int, y: Int, b: Int) {
            if (x in 0 until n && y in 0 until n) {
                val v = (b * dim).roundToInt().coerceIn(0, 255)
                val i = y * n + x
                if (v > buf[i]) buf[i] = v
            }
        }

        // Dim drifting clouds first (background). max-blend keeps them behind everything brighter.
        for (c in s.clouds) for (p in c.px) set(c.x.roundToInt() + p.dx, c.y + p.dy, p.b)

        // Ground: sparse dim dots along the bottom row.
        for (x in 0 until n step 2) set(x, s.groundY, 40)

        // Stem, bent by the wind (quadratic: base pinned, top sways most).
        val f = s.flower
        var topOffset = 0
        for (i in 1..f.stemH) {
            val frac = i.toDouble() / f.stemH
            val off = s.swayOffset(frac)
            set(s.baseX + off, s.groundY - i, f.stemB)
            if (i == f.stemH) topOffset = off
        }

        // Head follows the stem top; droops in rain / while wilting.
        val droop = s.droop()
        for ((idx, p) in s.headPixels().withIndex()) {
            if (s.isBlown(idx)) continue
            var b = p.b
            if (s.isSparkling() && p.b > 100 && s.rngPick(4) == 0) b = 255   // post-rain droplet glints
            set(s.baseX + topOffset + p.dx, s.stemTopY + p.dy + droop, b)
        }

        // Drifting petals / seeds.
        for (p in s.particles) set(p.x.roundToInt(), p.y.roundToInt(), p.b)

        // Rain on top, mid-brightness with a 1px tail slanting opposite the drop's motion.
        val lean = s.rainWind.roundToInt()
        for (d in s.drops) {
            val dx = d.x.roundToInt()
            set(dx, d.y, 110)
            set(dx - lean, d.y - 1, 60)
        }
        return buf
    }
}
