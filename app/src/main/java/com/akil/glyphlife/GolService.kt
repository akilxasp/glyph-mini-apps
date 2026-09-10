package com.akil.glyphlife

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager

/**
 * On-demand path. QS tile starts/stops this foreground service; it owns the matrix via
 * setAppMatrixFrame and runs the Conway loop while alive. Foreground + persistent notification
 * is mandatory on Android to keep pushing frames.
 */
class GolService : Service() {

    private var gm: GlyphMatrixManager? = null
    private var game: SnakeGame? = null
    private val loop = Handler(Looper.getMainLooper())
    private var registered = false
    private var paused = false

    /** A notification icon flash borrows the matrix; freeze, then carry on where we left off. */
    private val pauseReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { paused = i?.action == MatrixOwner.ACTION_PAUSE }
    }

    companion object {
        const val ACTION_STOP = "com.akil.glyphlife.DETONATE"   // tile-off → explode then blank
        private const val CHANNEL = "gol"
        private const val NOTIF_ID = 1
        private const val TICK_MS = 220L   // fast enough to read on 13×13; tune to taste
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val g = game
            if (g == null) blankAndStop() else g.detonate()   // detonate now; tick blanks + stops when done
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())

        MatrixOwner.claim(this)
        registerReceiver(pauseReceiver, MatrixOwner.pauseFilter(), Context.RECEIVER_NOT_EXPORTED)

        // setAppMatrixFrame requires system build >= 20250801. Guard so old firmware no-ops
        // instead of crashing. VERIFY-SDK: exact availability check API.
        gm = GlyphMatrixManager.getInstance(applicationContext)
        gm?.init(object : GlyphMatrixManager.Callback {   // VERIFY-SDK: Callback interface shape
            override fun onServiceConnected(name: android.content.ComponentName?) {
                val mgr = gm ?: return
                mgr.register(Glyph.DEVICE_25111p)   // Phone (4a) Pro
                registered = true
                val n = Common.getDeviceMatrixLength()  // 13 on 4a Pro
                game = SnakeGame(n)
                loop.post(tick)
            }

            override fun onServiceDisconnected(name: android.content.ComponentName?) {}
        })
    }

    private val tick = object : Runnable {
        override fun run() {
            if (paused) { loop.postDelayed(this, TICK_MS); return }   // overlay owns the matrix
            val g = game ?: return
            val mgr = gm ?: return
            g.step()
            // App-control write. Glyph Toys would override this, but 4a Pro has no button carousel.
            runCatching { mgr.setAppMatrixFrame(MatrixRenderer.toFrame(g)) }
            if (g.isFinished()) { blankAndStop(); return }   // detonation done → blank + stop
            loop.postDelayed(this, TICK_MS)
        }
    }

    private fun blankAndStop() {
        loop.removeCallbacksAndMessages(null)
        val n = game?.n ?: 0
        runCatching {
            if (n > 0) gm?.setAppMatrixFrame(IntArray(n * n))   // all-off frame
            gm?.closeAppMatrix()                               // release the matrix
        }
        stopSelf()
    }

    override fun onDestroy() {
        MatrixOwner.release(this)
        runCatching { unregisterReceiver(pauseReceiver) }
        loop.removeCallbacksAndMessages(null)
        runCatching { if (registered) gm?.unInit() }   // VERIFY-SDK: release/unregister name
        gm = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Snake", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("Snake")
            .setContentText("Running on Glyph Matrix")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }
}
