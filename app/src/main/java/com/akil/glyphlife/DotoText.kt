package com.akil.glyphlife

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.ceil

/**
 * Doto dot-font text → wide 1-bit column strip for the matrix, indexed col*n + row.
 *
 * Doto is a 5×7 dot grid, unitsPerEm 1000, dot pitch exactly 0.1 em. At TEXT_SIZE=10px one
 * font-dot maps to exactly one LED pixel — the smallest size with no information loss. Smaller
 * and dots merge. Antialiasing off + an integer baseline keep every dot pixel-centred.
 *
 * Shared by GlyphMediaService (Now Playing) and ReactionService (ms result).
 */
object DotoText {
    private const val ALPHA_ON = 96
    const val TEXT_SIZE = 10f

    fun rasterize(ctx: Context, text: String, n: Int): Pair<IntArray, Int>? {
        val paint = Paint().apply {
            isAntiAlias = false                     // 1 dot = 1 pixel; AA only smears it
            typeface = NothingUi.doto(ctx)
            fontVariationSettings = "'wght' 700"    // Doto is variable; fatter dots
            textSize = TEXT_SIZE
            color = Color.WHITE
        }
        val w = ceil(paint.measureText(text).toDouble()).toInt()
        if (w <= 0) return null
        val bmp = Bitmap.createBitmap(w, n, Bitmap.Config.ARGB_8888)
        // 7 cap rows + 2 descender rows = 9; centre that block, baseline on an integer row.
        val baseline = ((n - 9) / 2 + 7).toFloat()
        Canvas(bmp).drawText(text, 0f, baseline, paint)
        val strip = IntArray(w * n)
        for (col in 0 until w) for (row in 0 until n)
            strip[col * n + row] = if (Color.alpha(bmp.getPixel(col, row)) > ALPHA_ON) 255 else 0
        return strip to w
    }
}
