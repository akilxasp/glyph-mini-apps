package com.akil.glyphlife

import android.content.Context
import android.provider.Settings

/**
 * Nothing's glyph debug flag lets an unsigned sideloaded app drive the matrix, but it lapses
 * after ~48h and on reboot. With the WRITE_SECURE_SETTINGS grant (one-time, over adb —
 * `pm grant com.akil.glyphlife android.permission.WRITE_SECURE_SETTINGS`) the app re-arms it
 * itself, so no cable is ever needed again.
 *
 * runCatching means a missing grant just no-ops — you fall back to setting the flag by hand.
 */
object GlyphDebug {
    private const val FLAG = "nt_glyph_interface_debug_enable"

    fun ensure(ctx: Context) {
        runCatching {
            if (Settings.Global.getInt(ctx.contentResolver, FLAG, 0) != 1)
                Settings.Global.putInt(ctx.contentResolver, FLAG, 1)
        }
    }
}
