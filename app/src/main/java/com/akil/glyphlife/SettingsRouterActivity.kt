package com.akil.glyphlife

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle

/**
 * QS_TILE_PREFERENCES is one settings activity per app, not per tile — so every tile's long-press
 * lands here. Dispatch to the right screen by the source tile, then vanish (Theme.NoDisplay).
 */
class SettingsRouterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val src = if (Build.VERSION.SDK_INT >= 33)
            intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
        // Only two tiles have settings. Anything else (Snake, Shooter, Now Playing) just closes.
        val target = when {
            src?.className?.contains("NotifTileService") == true -> NotifSettingsActivity::class.java
            src?.className?.contains("GardenTileService") == true -> FlowerPickerActivity::class.java
            else -> null
        }
        target?.let { startActivity(Intent(this, it)) }
        finish()
    }
}
