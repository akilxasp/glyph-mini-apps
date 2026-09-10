package com.akil.glyphlife

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

/**
 * Ambient path. Registered as an Always-on Glyph Toy: user selects it in
 * Settings > Glyph Interface > Always-on Glyph Toy, then it shows face-down.
 * Cannot be triggered programmatically and updates at slow AOD cadence — one
 * generation per EVENT_AOD tick. This is the passive "flip phone over, see Life idling" mode.
 */
class GolToyService : Service() {

    private var gm: GlyphMatrixManager? = null
    private var game: SnakeGame? = null

    // VERIFY-SDK: message + event constants (MSG_GLYPH_TOY, MSG_GLYPH_TOY_DATA, EVENT_AOD/EVENT_CHANGE).
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what != GlyphToy.MSG_GLYPH_TOY) return
            when (msg.data.getString(GlyphToy.MSG_GLYPH_TOY_DATA)) {
                GlyphToy.EVENT_AOD -> renderNextGeneration()   // AOD tick = advance one step
                GlyphToy.EVENT_CHANGE -> game?.seed()          // long press (if 4a Pro delivers it): reset
            }
        }
    }
    private val messenger = Messenger(handler)

    override fun onBind(intent: Intent?): IBinder {
        init()
        return messenger.binder
    }

    private fun init() {
        gm = GlyphMatrixManager.getInstance(applicationContext)
        gm?.init(object : GlyphMatrixManager.Callback {   // VERIFY-SDK
            override fun onServiceConnected(name: android.content.ComponentName?) {
                gm?.register(Glyph.DEVICE_25111p)
                game = SnakeGame(Common.getDeviceMatrixLength())
            }
            override fun onServiceDisconnected(name: android.content.ComponentName?) {}
        })
    }

    private fun renderNextGeneration() {
        val g = game ?: return
        g.step()
        // Toy path uses setMatrixFrame (not setAppMatrixFrame).
        runCatching { gm?.setMatrixFrame(MatrixRenderer.toFrame(g)) }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        runCatching { gm?.unInit() }
        gm = null
        return false
    }
}
