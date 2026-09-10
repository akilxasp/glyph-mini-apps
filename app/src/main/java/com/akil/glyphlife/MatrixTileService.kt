package com.akil.glyphlife

import android.app.Service
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Shared QS tile for the matrix modes: tap starts the service, tap again stops it.
 * [stopAction] lets a mode play an outro (snake detonates, flower wilts) and stop itself;
 * null means stop immediately.
 *
 * Tile state comes from MatrixOwner, not from qsTile.state — the latter survives process death
 * and would show ACTIVE for a service the system already killed.
 */
abstract class MatrixTileService(
    private val serviceClass: Class<out Service>,
    private val iconRes: Int,
    private val stopAction: String? = null
) : TileService() {

    private val intent get() = Intent(this, serviceClass)

    override fun onStartListening() =
        setState(if (MatrixOwner.isLive(serviceClass)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE)

    override fun onClick() {
        if (qsTile?.state == Tile.STATE_ACTIVE) {
            if (stopAction != null) startForegroundService(intent.apply { action = stopAction })
            else stopService(intent)
            setState(Tile.STATE_INACTIVE)
        } else {
            startForegroundService(intent)
            setState(Tile.STATE_ACTIVE)
        }
    }

    private fun setState(state: Int) {
        qsTile?.apply {
            this.state = state
            icon = Icon.createWithResource(this@MatrixTileService, iconRes)
            updateTile()
        }
    }
}

/** Snake: tap again → detonate, then blank. */
class GolTileService : MatrixTileService(GolService::class.java, R.drawable.ic_snake, GolService.ACTION_STOP)

/** Garden: tap again → wilt, then blank. */
class GardenTileService : MatrixTileService(GardenService::class.java, R.drawable.ic_flower, GardenService.ACTION_WILT)

/** Now Playing: tap again → stop listening, blank. */
class MediaTileService : MatrixTileService(GlyphMediaService::class.java, R.drawable.ic_music)

/** Line shooter: tap again → the ship detonates, then blank. */
class ShooterTileService :
    MatrixTileService(ShooterService::class.java, R.drawable.ic_shooter, ShooterService.ACTION_STOP)

/** Reaction timer: tap again → stop, blank. */
class ReactionTileService : MatrixTileService(ReactionService::class.java, R.drawable.ic_reaction)
