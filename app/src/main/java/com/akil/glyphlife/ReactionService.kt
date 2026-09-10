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
import android.os.SystemClock
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import kotlin.random.Random

/**
 * Reaction timer. Press the Essential Key to arm; after a random wait the whole matrix flashes
 * ON — press again as fast as you can and it scrolls your reaction time in ms, then resets to
 * ready. Press during the wait (before the flash) is a false start → shows an X.
 *
 * Input arrives as ACTION_ESSENTIAL_TAP from EssentialKeyService (only while this mode is live).
 */
class ReactionService : Service() {

    private enum class State { IDLE, ARMED, GO, RESULT }

    private var gm: GlyphMatrixManager? = null
    private var registered = false
    private var n = 13
    private var state = State.IDLE
    private var goAt = 0L
    private val handler = Handler(Looper.getMainLooper())

    private var scroller: MarqueeScroller? = null   // result-number scroll
    private var current = IntArray(0)               // frame the loop keeps re-pushing

    private val tapReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            // The key's own timestamp, not arrival time — broadcasts add hundreds of ms.
            onTap(i?.getLongExtra(EssentialKeyService.EXTRA_EVENT_TIME, 0L) ?: 0L)
        }
    }
    private val goRunnable = Runnable { fire() }

    companion object {
        private const val CHANNEL = "reaction"
        private const val NOTIF_ID = 6
        private const val TICK_MS = 90L
        private const val MIN_WAIT = 1200L
        private const val MAX_WAIT = 4000L
        private const val FALSE_HOLD = 1300L    // how long the X lingers before reset
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        MatrixOwner.claim(this)
        registerReceiver(tapReceiver, IntentFilter(EssentialKeyService.ACTION_ESSENTIAL_TAP),
            Context.RECEIVER_NOT_EXPORTED)

        gm = GlyphMatrixManager.getInstance(applicationContext)
        gm?.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: android.content.ComponentName?) {
                val mgr = gm ?: return
                mgr.register(Glyph.DEVICE_25111p)
                registered = true
                n = Common.getDeviceMatrixLength().takeIf { it > 0 } ?: 13
                toIdle()
                handler.post(tick)   // matrix drops frames that aren't refreshed — tick like every other mode
            }
            override fun onServiceDisconnected(name: android.content.ComponentName?) {}
        })
    }

    /** Essential Key press. [pressedAt] = KeyEvent.eventTime, uptimeMillis base. */
    private fun onTap(pressedAt: Long) {
        when (state) {
            State.IDLE -> arm()
            State.ARMED -> falseStart()   // jumped the gun
            State.GO -> measure(pressedAt)
            State.RESULT -> {}            // busy showing X / scrolling → ignore
        }
    }

    private fun toIdle() {
        state = State.IDLE
        push(ReactionFrames.ready(n))
    }

    private fun arm() {
        state = State.ARMED
        push(IntArray(n * n))             // blank: give no tell of when GO comes
        handler.postDelayed(goRunnable, Random.nextLong(MIN_WAIT, MAX_WAIT))
    }

    private fun fire() {
        state = State.GO
        push(ReactionFrames.go(n))        // full flash — stamp AFTER the push so render lag doesn't count
        goAt = SystemClock.uptimeMillis() // same base as KeyEvent.eventTime
    }

    private fun falseStart() {
        handler.removeCallbacks(goRunnable)
        state = State.RESULT
        push(ReactionFrames.cross(n))
        handler.postDelayed({ toIdle() }, FALSE_HOLD)
    }

    private fun measure(pressedAt: Long) {
        // Fall back to now if the extra is missing (0) — worse, but never negative/absurd.
        val at = if (pressedAt > goAt) pressedAt else SystemClock.uptimeMillis()
        val ms = (at - goAt).toInt()
        state = State.RESULT
        val (strip, w) = DotoText.rasterize(this, "$ms ms", n) ?: run { toIdle(); return }
        scroller = MarqueeScroller(n, w, strip, totalPasses = 2)
    }

    /** Always-on loop: advances the scroller when one is live, and re-pushes the current frame
     *  every tick regardless — a frame pushed once and left alone never lights. */
    private val tick = object : Runnable {
        override fun run() {
            val s = scroller
            if (s != null) {
                current = s.frame()
                s.step()
                if (s.isDone()) { scroller = null; toIdle() }
            }
            if (current.isNotEmpty()) runCatching { gm?.setAppMatrixFrame(current) }
            handler.postDelayed(this, TICK_MS)
        }
    }

    private fun push(frame: IntArray) {
        current = frame
        runCatching { gm?.setAppMatrixFrame(frame) }
    }

    override fun onDestroy() {
        MatrixOwner.release(this)
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(tapReceiver) }
        runCatching {
            if (n > 0) gm?.setAppMatrixFrame(IntArray(n * n))
            gm?.closeAppMatrix()
        }
        runCatching { if (registered) gm?.unInit() }
        gm = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(CHANNEL, "Reaction", NotificationManager.IMPORTANCE_LOW)
                )
        }
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("Reaction")
            .setContentText("Essential Key reaction timer")
            .setSmallIcon(R.drawable.ic_reaction)
            .build()
    }
}
