package com.akil.glyphlife

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.random.Random

/**
 * Flower picker in the Nothing OS idiom (see NothingUi): black canvas, monochrome dot-matrix
 * previews, wide-tracked type, a single red accent for the selection.
 * Opened by long-pressing the Garden QS tile. Selection persists immediately.
 */
class FlowerPickerActivity : Activity() {

    private val cards = mutableListOf<Card>()
    private lateinit var prefs: android.content.SharedPreferences

    private var style = FlowerSprites.Style.DETAILED
    private val pills = mutableMapOf<FlowerSprites.Style, TextView>()

    private class Card(val key: String, val root: LinearLayout, val name: TextView, val preview: ImageView)

    private fun dp(v: Int) = NothingUi.dp(this, v)
    private fun cardBg(stroke: Int, strokeDp: Int) = NothingUi.cardBg(this, stroke, strokeDp)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(GardenService.PREFS, Context.MODE_PRIVATE)
        val selected = prefs.getString(GardenService.PREF_FLOWER, "tulip")
        style = runCatching {
            FlowerSprites.Style.valueOf(prefs.getString(GardenService.PREF_STYLE, "DETAILED") ?: "DETAILED")
        }.getOrDefault(FlowerSprites.Style.DETAILED)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(NothingUi.BG)
            setPadding(dp(24), dp(28), dp(24), dp(28))
            // Pad below the status bar / above the nav bar so nothing is clipped.
            fitsSystemWindows = true
            setOnApplyWindowInsetsListener { v, insets ->
                v.setPadding(dp(24), dp(28) + insets.systemWindowInsetTop,
                    dp(24), dp(28) + insets.systemWindowInsetBottom)
                insets
            }
        }

        root.addView(NothingUi.label(this, "SELECT FLOWER", NothingUi.INK, 34f, 0.10f, NothingUi.doto(this), 800))

        // Style toggle: DETAILED / SOLID.
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            (layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )).also { it.topMargin = dp(16); layoutParams = it }
            addView(stylePill(FlowerSprites.Style.DETAILED, "DETAILED"))
            addView(stylePill(FlowerSprites.Style.SOLID, "SOLID").apply {
                (layoutParams as LinearLayout.LayoutParams).marginStart = dp(10)
            })
        })

        // 2-column grid of flower cards.
        val grid = GridLayout(this).apply {
            columnCount = 2
            setPadding(0, dp(22), 0, 0)
        }
        val colW = (resources.displayMetrics.widthPixels - dp(24) * 2 - dp(12)) / 2
        for (flower in FlowerSprites.ALL) {
            val card = buildCard(flower, colW)
            grid.addView(card.root)
            cards.add(card)
        }
        root.addView(grid)

        root.addView(NothingUi.label(this, "TAP TO SELECT · BLOOMS ON GLYPH MATRIX", NothingUi.MUTE, 10f, 0.18f).apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dp(20)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        setContentView(ScrollView(this).apply { setBackgroundColor(NothingUi.BG); addView(root) })
        highlight(selected)
        selectStyle(style)   // paint the active pill + previews
    }

    private fun buildCard(flower: Flower, colW: Int): Card {
        val name = NothingUi.label(this, flower.key.uppercase(), NothingUi.INK, 12f, 0.22f).apply {
            gravity = Gravity.CENTER
            (layoutParams as LinearLayout.LayoutParams).topMargin = dp(10)
        }
        val preview = ImageView(this).apply {
            setImageBitmap(previewBitmap(flower))
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(96))
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(18), dp(14), dp(14))
            background = cardBg(NothingUi.LINE, 1)
            layoutParams = GridLayout.LayoutParams().apply {
                width = colW; setMargins(0, 0, dp(12), dp(12))
            }
            addView(preview)
            addView(name)
            setOnClickListener {
                prefs.edit().putString(GardenService.PREF_FLOWER, flower.key).apply()
                highlight(flower.key)
                notifyGarden()
            }
        }
        return Card(flower.key, card, name, preview)
    }

    private fun highlight(key: String?) {
        for (c in cards) {
            val on = c.key == key
            c.root.background = cardBg(if (on) NothingUi.RED else NothingUi.LINE, if (on) 2 else 1)
            c.name.setTextColor(if (on) NothingUi.RED else NothingUi.INK)
        }
    }

    private fun stylePill(s: FlowerSprites.Style, text: String): TextView {
        val pill = NothingUi.label(this, text, NothingUi.INK, 12f, 0.20f).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener { selectStyle(s) }
        }
        pills[s] = pill
        return pill
    }

    private fun selectStyle(s: FlowerSprites.Style) {
        style = s
        prefs.edit().putString(GardenService.PREF_STYLE, s.name).apply()
        for ((ps, pill) in pills) {
            val on = ps == s
            pill.background = cardBg(if (on) NothingUi.RED else NothingUi.LINE, if (on) 2 else 1)
            pill.setTextColor(if (on) NothingUi.RED else NothingUi.INK)
        }
        for (c in cards) c.preview.setImageBitmap(previewBitmap(FlowerSprites.byKey(c.key)))
        notifyGarden()
    }

    private fun notifyGarden() =
        sendBroadcast(Intent(GardenService.ACTION_RELOAD).setPackage(packageName))

    /** Static 13×13 flower on pure black, scaled up nearest-neighbor — matrix look. */
    private fun previewBitmap(flower: Flower): Bitmap {
        val n = 13
        val frame = GardenRenderer.toFrame(GardenScene(n, flower, style, Random(1), showClouds = false))
        val small = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        for (y in 0 until n) for (x in 0 until n) {
            val v = frame[y * n + x]
            small.setPixel(x, y, Color.rgb(v, v, v))
        }
        return Bitmap.createScaledBitmap(small, n * 16, n * 16, false)
    }
}
