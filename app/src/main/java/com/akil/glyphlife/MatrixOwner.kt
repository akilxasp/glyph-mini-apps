package com.akil.glyphlife

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Exactly one mode drives the Glyph Matrix. The rule lives here, not copy-pasted into every
 * service — adding a mode means adding one line to [MODES].
 *
 * A mode calls [claim] in onCreate and [release] in onDestroy. A transient overlay (the notification
 * icon flash) instead calls [pauseAll] / [resumeAll]: it borrows the matrix for a few seconds and
 * hands it back, so a running game or scroll survives.
 */
object MatrixOwner {
    const val ACTION_PAUSE = "com.akil.glyphlife.MATRIX_PAUSE"
    const val ACTION_RESUME = "com.akil.glyphlife.MATRIX_RESUME"

    private val MODES = listOf(
        GolService::class.java,
        GardenService::class.java,
        GlyphMediaService::class.java,
        ShooterService::class.java,
        ReactionService::class.java,
    )

    private val live = mutableSetOf<Class<out Service>>()

    /** Take the matrix: keep the debug flag alive, then stop every other mode. */
    fun claim(s: Service) {
        GlyphDebug.ensure(s)   // re-arm the 48h flag so games run cable-free
        MODES.filter { it != s.javaClass }.forEach { s.stopService(Intent(s, it)) }
        live += s.javaClass
    }

    fun release(s: Service) { live -= s.javaClass }

    /** Whether a mode is currently running — the QS tiles' source of truth. */
    fun isLive(c: Class<out Service>) = c in live

    /** Whether any matrix mode is running. */
    fun anyLive() = live.isNotEmpty()

    fun pauseAll(c: Context) = c.sendBroadcast(Intent(ACTION_PAUSE).setPackage(c.packageName))
    fun resumeAll(c: Context) = c.sendBroadcast(Intent(ACTION_RESUME).setPackage(c.packageName))

    fun pauseFilter() = IntentFilter().apply { addAction(ACTION_PAUSE); addAction(ACTION_RESUME) }
}
