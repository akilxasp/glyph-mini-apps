package com.akil.glyphlife

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView

/** Shared Nothing-OS styling: black canvas, mono/Doto type, single red accent. */
object NothingUi {
    val BG = Color.BLACK
    val CARD = Color.parseColor("#0D0D0D")
    val LINE = Color.parseColor("#242424")
    val INK = Color.parseColor("#EDEDED")
    val MUTE = Color.parseColor("#6E6E6E")
    val RED = Color.parseColor("#D71921")

    private var dotoCache: Typeface? = null
    fun doto(c: Context): Typeface =
        dotoCache ?: Typeface.createFromAsset(c.assets, "Doto.ttf").also { dotoCache = it }

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    fun label(
        c: Context, text: String, color: Int, sp: Float, tracking: Float,
        face: Typeface = Typeface.MONOSPACE, weight: Int = 0
    ) = TextView(c).apply {
        this.text = text
        setTextColor(color)
        typeface = face
        if (weight > 0) fontVariationSettings = "'wght' $weight"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        letterSpacing = tracking
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    /** App's monochrome (themed) icon layer — the Nothing-style silhouette — or null if none. */
    fun appGlyph(c: Context, pkg: String): Drawable? {
        val d = runCatching { c.packageManager.getApplicationIcon(pkg) }.getOrNull()
        return if (Build.VERSION.SDK_INT >= 33 && d is AdaptiveIconDrawable) d.monochrome else null
    }

    /** List icon: themed monochrome layer if present, else the app's normal icon. */
    fun appListIcon(c: Context, pkg: String, fallback: Drawable): Drawable =
        appGlyph(c, pkg)?.mutate()?.apply { setTint(INK) } ?: fallback

    fun cardBg(c: Context, stroke: Int, strokeDp: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(c, 6).toFloat()
        setColor(CARD)
        setStroke(dp(c, strokeDp), stroke)
    }
}
