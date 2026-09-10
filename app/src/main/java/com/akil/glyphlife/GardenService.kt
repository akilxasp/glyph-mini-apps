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
 * Garden tile's foreground service: owns the matrix via setAppMatrixFrame and runs the
 * garden loop. Tile-off sends ACTION_WILT → flower wilts, petals fall, matrix blanks, stops.
 */
class GardenService : Service() {

    private var gm: GlyphMatrixManager? = null
    private var scene: GardenScene? = null
    private val loop = Handler(Looper.getMainLooper())
    private var registered = false
    private var paused = false

    /** A notification icon flash borrows the matrix; freeze, then carry on where we left off. */
    private val pauseReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { paused = i?.action == MatrixOwner.ACTION_PAUSE }
    }

    companion object {
        const val ACTION_WILT = "com.akil.glyphlife.WILT"
        const val ACTION_RELOAD = "com.akil.glyphlife.RELOAD"   // picker → live flower/style swap
        const val PREFS = "garden"
        const val PREF_FLOWER = "flower"
        const val PREF_STYLE = "style"   // "DETAILED" | "SOLID"
        private const val CHANNEL = "garden"
        private const val NOTIF_ID = 2
        private const val TICK_MS = 120L
    }

    // Live swap: only a running service is registered, so picking a flower while stopped starts nothing.
    private val reload = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            if (registered) scene = loadScene()
        }
    }

    private fun loadScene(): GardenScene {
        val sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = sp.getString(PREF_FLOWER, "tulip") ?: "tulip"
        val style = runCatching {
            FlowerSprites.Style.valueOf(sp.getString(PREF_STYLE, "DETAILED") ?: "DETAILED")
        }.getOrDefault(FlowerSprites.Style.DETAILED)
        return GardenScene(Common.getDeviceMatrixLength(), FlowerSprites.byKey(key), style)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_WILT) {
            val s = scene
            if (s == null) blankAndStop() else s.wiltNow()
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())

        MatrixOwner.claim(this)
        registerReceiver(reload, IntentFilter(ACTION_RELOAD), Context.RECEIVER_NOT_EXPORTED)
        registerReceiver(pauseReceiver, MatrixOwner.pauseFilter(), Context.RECEIVER_NOT_EXPORTED)

        gm = GlyphMatrixManager.getInstance(applicationContext)
        gm?.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: android.content.ComponentName?) {
                val mgr = gm ?: return
                mgr.register(Glyph.DEVICE_25111p)
                registered = true
                scene = loadScene()
                loop.post(tick)
            }
            override fun onServiceDisconnected(name: android.content.ComponentName?) {}
        })
    }

    private val tick = object : Runnable {
        override fun run() {
            if (paused) { loop.postDelayed(this, TICK_MS); return }   // overlay owns the matrix
            val s = scene ?: return
            val mgr = gm ?: return
            s.step()
            runCatching { mgr.setAppMatrixFrame(GardenRenderer.toFrame(s)) }
            if (s.mode == GardenScene.Mode.FINISHED) { blankAndStop(); return }
            loop.postDelayed(this, TICK_MS)
        }
    }

    private fun blankAndStop() {
        loop.removeCallbacksAndMessages(null)
        val n = scene?.n ?: 0
        runCatching {
            if (n > 0) gm?.setAppMatrixFrame(IntArray(n * n))
            gm?.closeAppMatrix()
        }
        stopSelf()
    }

    override fun onDestroy() {
        MatrixOwner.release(this)
        loop.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(reload) }
        runCatching { unregisterReceiver(pauseReceiver) }
        runCatching { if (registered) gm?.unInit() }
        gm = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Garden", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("Garden")
            .setContentText("Blooming on Glyph Matrix")
            .setSmallIcon(R.drawable.ic_flower)
            .build()
    }
}
