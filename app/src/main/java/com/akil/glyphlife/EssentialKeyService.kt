package com.akil.glyphlife

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Catches the Essential Key. The key emits Linux scancode 250 on gpio-keys and is mapped in no
 * keylayout, so Android delivers it as KEYCODE_UNKNOWN — but the scanCode survives, and an
 * accessibility service with flagRequestFilterKeyEvents still sees the event.
 *
 * Fires the Shooter's missile. Only consumes the key while Shooter is running; otherwise the
 * key behaves as it always did.
 */
class EssentialKeyService : AccessibilityService() {

    companion object {
        const val ACTION_FIRE_MISSILE = "com.akil.glyphlife.FIRE_MISSILE"
        const val ACTION_ESSENTIAL_TAP = "com.akil.glyphlife.ESSENTIAL_TAP"
        const val EXTRA_EVENT_TIME = "eventTime"   // KeyEvent.eventTime (uptimeMillis base) — broadcast adds latency, this doesn't
        private const val ESSENTIAL_SCANCODE = 250
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.scanCode != ESSENTIAL_SCANCODE) return false
        val shooter = MatrixOwner.isLive(ShooterService::class.java)
        val reaction = MatrixOwner.isLive(ReactionService::class.java)
        if (!shooter && !reaction) return false                // not our key when no game runs — pass DOWN and UP through
        if (event.action != KeyEvent.ACTION_DOWN) return true  // swallow the UP only while a game runs
        if (shooter) sendBroadcast(Intent(ACTION_FIRE_MISSILE).setPackage(packageName))
        if (reaction) sendBroadcast(Intent(ACTION_ESSENTIAL_TAP).setPackage(packageName)
            .putExtra(EXTRA_EVENT_TIME, event.eventTime))
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
