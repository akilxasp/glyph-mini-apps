package com.akil.glyphlife

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager

/**
 * Flashes a notification's app icon on the Glyph Matrix for a few seconds, then blanks.
 * Owns the matrix briefly — stops the garden/snake renderers while showing.
 */
class NotifDisplayService : Service() {

    private var gm: GlyphMatrixManager? = null
    private var registered = false
    private var pending: Icon? = null
    private var pendingPkg: String? = null
    private var frame: IntArray? = null
    private var showUntil = 0L
    private var ticking = false
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        const val EXTRA_ICON = "icon"
        const val EXTRA_PKG = "pkg"
        private const val CHANNEL = "notifglyph"
        private const val NOTIF_ID = 3
        private const val SHOW_MS = 3000L
        private const val PUSH_MS = 150L
        private const val ALPHA_ON = 96   // silhouette threshold (midpoint after AA downscale)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        // One owner of the matrix.
        // Transient overlay: borrow the matrix from every mode, hand it back when done.
        // Nothing is killed — a running game or scroll resumes where it left off.
        MatrixOwner.pauseAll(this)

        gm = GlyphMatrixManager.getInstance(applicationContext)
        gm?.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: android.content.ComponentName?) {
                gm?.register(Glyph.DEVICE_25111p)
                registered = true
                showNow()
            }
            override fun onServiceDisconnected(name: android.content.ComponentName?) {}
        })

        // Safety net: never linger holding Now Playing paused.
        handler.postDelayed({ if (!ticking) blankAndStop() }, SHOW_MS * 2)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        pending = intent?.let {
            if (Build.VERSION.SDK_INT >= 33) it.getParcelableExtra(EXTRA_ICON, Icon::class.java)
            else @Suppress("DEPRECATION") it.getParcelableExtra(EXTRA_ICON)
        } ?: pending
        pendingPkg = intent?.getStringExtra(EXTRA_PKG) ?: pendingPkg
        if (registered) showNow()
        return START_NOT_STICKY
    }

    private fun showNow() {
        // Prefer the app's Nothing-style monochrome glyph; fall back to the notification's smallIcon.
        // Nothing to draw → stop, or we'd leak the service and leave Now Playing paused forever.
        val d = pendingPkg?.let { NothingUi.appGlyph(this, it) } ?: pending?.loadDrawable(this)
            ?: run { blankAndStop(); return }
        val n = Common.getDeviceMatrixLength().takeIf { it > 0 } ?: 13
        frame = iconFrame(d, n)
        showUntil = System.currentTimeMillis() + SHOW_MS   // latest notif extends the hold
        if (!ticking) { ticking = true; handler.post(push) }
    }

    // Re-push every PUSH_MS so early frames retry until the matrix session is live.
    private val push = object : Runnable {
        override fun run() {
            val f = frame; val mgr = gm
            if (f != null && mgr != null) runCatching { mgr.setAppMatrixFrame(f) }
            if (System.currentTimeMillis() < showUntil) handler.postDelayed(this, PUSH_MS)
            else { ticking = false; blankAndStop() }
        }
    }

    /** Render the icon's alpha silhouette to an n×n frame. Supersample then area-average
     *  down for smooth edges, then threshold to a crisp 1-bit shape. */
    private fun iconFrame(d: android.graphics.drawable.Drawable, n: Int): IntArray {
        val s = n * 8
        val big = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, s, s)
        d.draw(Canvas(big))
        val small = Bitmap.createScaledBitmap(big, n, n, true)   // bilinear = anti-aliased downscale
        return IntArray(n * n) { i ->
            if (Color.alpha(small.getPixel(i % n, i / n)) > ALPHA_ON) 255 else 0
        }
    }

    private fun blankAndStop() {
        handler.removeCallbacksAndMessages(null)
        MatrixOwner.resumeAll(this)
        val n = Common.getDeviceMatrixLength().takeIf { it > 0 } ?: 13
        runCatching { gm?.setAppMatrixFrame(IntArray(n * n)); gm?.closeAppMatrix() }
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        // Every death path must hand the matrix back.
        MatrixOwner.resumeAll(this)
        runCatching { if (registered) gm?.unInit() }
        gm = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(CHANNEL, "Notif Glyph", NotificationManager.IMPORTANCE_LOW)
                )
        }
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("Notif Glyph")
            .setContentText("Showing icon on Glyph Matrix")
            .setSmallIcon(R.drawable.ic_flower)
            .build()
    }
}
