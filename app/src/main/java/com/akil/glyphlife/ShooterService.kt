package com.akil.glyphlife

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager

/**
 * Line-shooter matrix mode. Tile-off sends ACTION_STOP → the ship detonates, the blast plays,
 * the matrix blanks, the service stops.
 */
class ShooterService : Service() {

    private var gm: GlyphMatrixManager? = null
    private var game: ShooterGame? = null
    private val loop = Handler(Looper.getMainLooper())
    private var registered = false
    private var paused = false

    /** A notification icon flash borrows the matrix; freeze, then carry on where we left off. */
    private val pauseReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { paused = i?.action == MatrixOwner.ACTION_PAUSE }
    }

    /** Essential Key pressed (see EssentialKeyService). */
    private val missileReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { game?.fireMissile() }
    }

    companion object {
        const val ACTION_STOP = "com.akil.glyphlife.SHOOTER_STOP"
        private const val CHANNEL = "shooter"
        private const val NOTIF_ID = 5
        private const val TICK_MS = 130L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val g = game
            if (g == null) blankAndStop() else g.detonate()   // tick blanks + stops when the blast ends
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        MatrixOwner.claim(this)
        registerReceiver(pauseReceiver, MatrixOwner.pauseFilter(), Context.RECEIVER_NOT_EXPORTED)
        registerReceiver(missileReceiver, IntentFilter(EssentialKeyService.ACTION_FIRE_MISSILE),
            Context.RECEIVER_NOT_EXPORTED)

        gm = GlyphMatrixManager.getInstance(applicationContext)
        gm?.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: android.content.ComponentName?) {
                val mgr = gm ?: return
                mgr.register(Glyph.DEVICE_25111p)
                registered = true
                game = ShooterGame(Common.getDeviceMatrixLength())
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
            runCatching { mgr.setAppMatrixFrame(ShooterRenderer.toFrame(g)) }
            if (g.isFinished()) { blankAndStop(); return }
            loop.postDelayed(this, TICK_MS)
        }
    }

    private fun blankAndStop() {
        loop.removeCallbacksAndMessages(null)
        val n = game?.n ?: 0
        runCatching {
            if (n > 0) gm?.setAppMatrixFrame(IntArray(n * n))
            gm?.closeAppMatrix()
        }
        stopSelf()
    }

    override fun onDestroy() {
        MatrixOwner.release(this)
        runCatching { unregisterReceiver(pauseReceiver) }
        runCatching { unregisterReceiver(missileReceiver) }
        loop.removeCallbacksAndMessages(null)
        runCatching { if (registered) gm?.unInit() }
        gm = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(CHANNEL, "Shooter", NotificationManager.IMPORTANCE_LOW)
                )
        }
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("Shooter")
            .setContentText("Running on Glyph Matrix")
            .setSmallIcon(R.drawable.ic_shooter)
            .build()
    }
}
