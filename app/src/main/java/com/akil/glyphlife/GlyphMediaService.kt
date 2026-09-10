package com.akil.glyphlife

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager

/**
 * Now Playing: while alive, listens to media sessions and scrolls "Title  Artist" across the
 * matrix three times on each new song, then blanks and keeps listening.
 *
 * Owns the matrix, but a Notify Glyph icon flash only PAUSES it (ACTION_PAUSE/ACTION_RESUME);
 * Snake/Garden stop it outright, per the one-owner rule.
 */
class GlyphMediaService : Service() {

    private var gm: GlyphMatrixManager? = null
    private var registered = false
    private var scroller: MarqueeScroller? = null
    private var ticking = false
    private var paused = false
    private var lastKey: String? = null
    private val handler = Handler(Looper.getMainLooper())

    private var msm: MediaSessionManager? = null
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()
    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { list -> bind(list ?: emptyList()) }

    companion object {
        private const val CHANNEL = "media"
        private const val NOTIF_ID = 4
        private const val TICK_MS = 90L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Notif flash borrows the matrix; we stop pushing, then pick the same scroller back up. */
    private val pauseReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { paused = i?.action == MatrixOwner.ACTION_PAUSE }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())

        MatrixOwner.claim(this)
        registerReceiver(pauseReceiver, MatrixOwner.pauseFilter(), Context.RECEIVER_NOT_EXPORTED)

        gm = GlyphMatrixManager.getInstance(applicationContext)
        gm?.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: ComponentName?) {
                gm?.register(Glyph.DEVICE_25111p)
                registered = true
                listenToSessions()
            }
            override fun onServiceDisconnected(name: ComponentName?) {}
        })
    }

    // --- media sessions ----------------------------------------------------------------

    private fun listenToSessions() {
        // getActiveSessions is authorised by any enabled NotificationListenerService.
        val comp = ComponentName(this, NotifGlyphListener::class.java)
        msm = getSystemService(MediaSessionManager::class.java)
        runCatching {
            msm?.addOnActiveSessionsChangedListener(sessionsListener, comp, handler)
            bind(msm?.getActiveSessions(comp) ?: emptyList())
        }
    }

    private fun bind(controllers: List<MediaController>) {
        // Drop controllers that went away.
        callbacks.keys.filter { it !in controllers }.forEach { c ->
            callbacks.remove(c)?.let { runCatching { c.unregisterCallback(it) } }
        }
        for (c in controllers) {
            if (c in callbacks) continue
            val cb = object : MediaController.Callback() {
                override fun onMetadataChanged(md: MediaMetadata?) = onTrack(c)
                override fun onPlaybackStateChanged(state: PlaybackState?) = onTrack(c)
                override fun onSessionDestroyed() {
                    callbacks.remove(c)?.let { runCatching { c.unregisterCallback(it) } }
                }
            }
            callbacks[c] = cb
            c.registerCallback(cb, handler)
            onTrack(c)            // catch a song already playing when the tile turns on
        }
    }

    /**
     * Metadata and playback-state changes both land here. Only scroll for a PLAYING session, and
     * dedup by title+artist so pause/resume/seek of the same track never re-triggers.
     */
    private fun onTrack(c: MediaController) {
        if (c.playbackState?.state != PlaybackState.STATE_PLAYING) return   // no music, no scroll
        val md = c.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty()
        if (title.isBlank()) return
        val key = "$title$artist"
        if (key == lastKey) return          // same track re-emitted → ignore
        lastKey = key
        val body = if (artist.isBlank()) title else "$title  $artist"
        startScroll("🎵 $body")   // 🎵 prefix
    }

    // --- rendering ---------------------------------------------------------------------

    private fun startScroll(text: String) {
        val n = Common.getDeviceMatrixLength().takeIf { it > 0 } ?: 13
        val (strip, w) = DotoText.rasterize(this, text, n) ?: return
        scroller = MarqueeScroller(n, w, strip)   // a new song mid-scroll restarts at pass 1
        if (!ticking) { ticking = true; handler.post(tick) }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (paused) { handler.postDelayed(this, TICK_MS); return }   // notif flash owns the matrix
            val s = scroller
            val mgr = gm
            if (s == null || mgr == null) { ticking = false; return }
            runCatching { mgr.setAppMatrixFrame(s.frame()) }
            s.step()
            if (s.isDone()) {                       // blank, but stay alive for the next song
                val n = s.n
                runCatching { mgr.setAppMatrixFrame(IntArray(n * n)) }
                scroller = null; ticking = false; return
            }
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onDestroy() {
        MatrixOwner.release(this)
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(pauseReceiver) }
        runCatching { msm?.removeOnActiveSessionsChangedListener(sessionsListener) }
        callbacks.forEach { (c, cb) -> runCatching { c.unregisterCallback(cb) } }
        callbacks.clear()
        val n = Common.getDeviceMatrixLength().takeIf { it > 0 } ?: 13
        runCatching { gm?.setAppMatrixFrame(IntArray(n * n)); gm?.closeAppMatrix() }
        runCatching { if (registered) gm?.unInit() }
        gm = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(CHANNEL, "Now Playing", NotificationManager.IMPORTANCE_LOW)
                )
        }
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("Now Playing")
            .setContentText("Scrolling tracks on Glyph Matrix")
            .setSmallIcon(R.drawable.ic_music)
            .build()
    }
}
