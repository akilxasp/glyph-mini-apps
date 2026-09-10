package com.akil.glyphlife

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Reacts to posted notifications two ways:
 *
 *  - If Notify Glyph is on for the posting app, forwards its small icon to NotifDisplayService,
 *    which flashes it on the matrix (that service pauses/resumes the running mode itself).
 *  - Otherwise, briefly pauses the running matrix mode so Nothing's own system notification glyph
 *    gets the matrix to itself and animates cleanly — without this, our per-tick frames fight it
 *    and neither renders.
 *
 * Needs the Notification Access grant (Settings → Notifications → Device & app notifications).
 */
class NotifGlyphListener : NotificationListenerService() {

    companion object {
        const val PREFS = "notif"
        const val PREF_ENABLED = "enabled"
        const val PREF_APPS = "apps"
        private const val YIELD_MS = 2500L   // window handed to the system glyph
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.isOngoing) return                       // skip persistent/foreground notifs

        val sp = getSharedPreferences(PREFS, MODE_PRIVATE)
        val ours = sp.getBoolean(PREF_ENABLED, false) &&
            sbn.packageName in (sp.getStringSet(PREF_APPS, emptySet()) ?: emptySet())

        if (ours) {
            val icon: Icon = sbn.notification.smallIcon ?: return
            startForegroundService(Intent(this, NotifDisplayService::class.java).apply {
                putExtra(NotifDisplayService.EXTRA_ICON, icon)
                putExtra(NotifDisplayService.EXTRA_PKG, sbn.packageName)
            })
            return
        }

        // Yield the matrix so the system notification glyph can play, then take it back.
        if (MatrixOwner.anyLive()) {
            MatrixOwner.pauseAll(this)
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ MatrixOwner.resumeAll(this) }, YIELD_MS)
        }
    }
}
