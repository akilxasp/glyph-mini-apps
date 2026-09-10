package com.akil.glyphlife

import android.content.Context
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * QS tile: master on/off for Notify Glyph. Long-press opens NotifSettingsActivity natively
 * (via its QS_TILE_PREFERENCES intent-filter) to pick which apps trigger it.
 */
class NotifTileService : TileService() {

    private fun prefs() = getSharedPreferences(NotifGlyphListener.PREFS, Context.MODE_PRIVATE)

    override fun onClick() {
        val on = !prefs().getBoolean(NotifGlyphListener.PREF_ENABLED, false)
        prefs().edit().putBoolean(NotifGlyphListener.PREF_ENABLED, on).apply()
        sync()
    }

    override fun onStartListening() = sync()

    private fun sync() {
        val on = prefs().getBoolean(NotifGlyphListener.PREF_ENABLED, false)
        qsTile?.apply {
            state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            icon = Icon.createWithResource(this@NotifTileService, R.drawable.ic_bell)
            updateTile()
        }
    }
}
