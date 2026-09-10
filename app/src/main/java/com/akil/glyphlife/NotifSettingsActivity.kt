package com.akil.glyphlife

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * Nothing-styled app picker: choose which apps' notifications flash their icon on the matrix.
 * Opened by long-pressing the Notify Glyph QS tile. Selection persists to prefs; the listener reads it.
 */
class NotifSettingsActivity : Activity() {

    private lateinit var prefs: android.content.SharedPreferences
    private val selected = HashSet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(NotifGlyphListener.PREFS, Context.MODE_PRIVATE)
        selected.addAll(prefs.getStringSet(NotifGlyphListener.PREF_APPS, emptySet()) ?: emptySet())

        val col = NothingUi
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(col.BG)
            fitsSystemWindows = true
            setOnApplyWindowInsetsListener { v, i ->
                v.setPadding(col.dp(this@NotifSettingsActivity, 24), col.dp(this@NotifSettingsActivity, 28) + i.systemWindowInsetTop,
                    col.dp(this@NotifSettingsActivity, 24), col.dp(this@NotifSettingsActivity, 28) + i.systemWindowInsetBottom); i
            }
            setPadding(col.dp(this@NotifSettingsActivity, 24), col.dp(this@NotifSettingsActivity, 40),
                col.dp(this@NotifSettingsActivity, 24), col.dp(this@NotifSettingsActivity, 28))
        }
        root.addView(col.label(this, "NOTIFY GLYPH", col.INK, 30f, 0.10f, col.doto(this), 800))
        root.addView(col.label(this, "TAP APPS TO SHOW ON MATRIX", col.MUTE, 10f, 0.18f).apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = col.dp(this@NotifSettingsActivity, 6)
        })

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, col.dp(this@NotifSettingsActivity, 18), 0, 0)
        }
        // ponytail: load launchable apps synchronously on open — fine for a settings screen.
        val pm = packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        for (ri in apps) {
            val pkg = ri.activityInfo.packageName
            list.addView(row(pkg, ri.loadLabel(pm).toString(), NothingUi.appListIcon(this, pkg, ri.loadIcon(pm))))
        }
        root.addView(list)

        setContentView(ScrollView(this).apply { setBackgroundColor(col.BG); addView(root) })
    }

    private fun row(pkg: String, name: String, icon: android.graphics.drawable.Drawable): LinearLayout {
        val col = NothingUi
        val label = col.label(this, name.uppercase(), col.INK, 13f, 0.12f).apply {
            (layoutParams as LinearLayout.LayoutParams).apply { marginStart = col.dp(this@NotifSettingsActivity, 14); weight = 1f }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(col.dp(this@NotifSettingsActivity, 12), col.dp(this@NotifSettingsActivity, 10),
                col.dp(this@NotifSettingsActivity, 14), col.dp(this@NotifSettingsActivity, 10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = col.dp(this@NotifSettingsActivity, 8) }
            addView(ImageView(this@NotifSettingsActivity).apply {
                setImageDrawable(icon)
                layoutParams = LinearLayout.LayoutParams(col.dp(this@NotifSettingsActivity, 28), col.dp(this@NotifSettingsActivity, 28))
            })
            addView(label)
            paint(this, pkg)
            setOnClickListener {
                if (!selected.add(pkg)) selected.remove(pkg)
                prefs.edit().putStringSet(NotifGlyphListener.PREF_APPS, HashSet(selected)).apply()
                paint(this, pkg)
                (getChildAt(1) as android.widget.TextView).setTextColor(if (pkg in selected) col.RED else col.INK)
            }
        }
    }

    private fun paint(row: LinearLayout, pkg: String) {
        val on = pkg in selected
        row.background = NothingUi.cardBg(this, if (on) NothingUi.RED else NothingUi.LINE, if (on) 2 else 1)
    }
}
