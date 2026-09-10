# Phone changes (Nothing Phone 4a Pro)

Device-side changes made so the Glyph app works. None touch the app's own code — they're
system settings, disabled packages, and permission grants. Everything here is reversible.

`adb` isn't on PATH; it lives at `~/Library/Android/sdk/platform-tools/adb`. Either use the full
path or add it once:

```
echo 'export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"' >> ~/.zshrc && source ~/.zshrc
```

Below assumes `adb` is on PATH.

---

## 1. Glyph debug flag (REQUIRED, expires every 48h — now auto-refreshed, see item 7)

An unsigned sideloaded app can't drive the matrix without this. It lapses after ~48h and on
reboot. As of the WRITE_SECURE_SETTINGS grant (item 7) the app re-arms it itself every time a
tile starts, so you no longer need a cable. If the matrix ever stays dark (e.g. grant was
revoked), set it by hand:

```
adb shell settings put global nt_glyph_interface_debug_enable 1
```

Revert (matrix goes dark for the app):
```
adb shell settings put global nt_glyph_interface_debug_enable 0
```

## 2. Essential Space disabled

The Essential Key opened Essential Space. Disabled so the key is free for the Shooter missile.
Your existing Essential Space notes/recordings are NOT deleted — the app is only disabled.

```
adb shell pm disable-user --user 0 com.nothing.ntessentialspace
```

Revert:
```
adb shell pm enable com.nothing.ntessentialspace
```

## 3. Essential Recorder disabled

A press/hold of the Essential Key was launching the Recorder (its side-key handler). Disabled
for the same reason.

```
adb shell pm disable-user --user 0 com.nothing.ntessentialrecorder
```

Revert:
```
adb shell pm enable com.nothing.ntessentialrecorder
```

## 4. Accessibility service enabled (EssentialKeyService)

Lets the app catch the Essential Key (scancode 250, which no keylayout maps) and fire the Shooter
missile. Reads key presses only — no screen content. Enabled via **Settings → Accessibility →
Glyph Snake**. If disabled there, the Essential Key stops firing missiles.

Revert: toggle it off in Settings → Accessibility, or:
```
adb shell settings put secure enabled_accessibility_services ""
```
(clears ALL accessibility services — re-enable others afterward if you use any.)

## 5. Notification access granted (NotifGlyphListener)

Needed for the Notify Glyph feature AND for reading media sessions (Now Playing marquee).
Granted via **Settings → Notifications → Device & app notifications → Glyph Snake**.

Revert: toggle off in that same Settings screen.

## 6. WRITE_SETTINGS app-op (granted, now unused — safe to revoke)

Briefly used to auto-mute the system notification glyph during games; that approach was reverted,
so this grant is now orphaned. Harmless, but to clean up:

```
adb shell appops set com.akil.glyphlife WRITE_SETTINGS default
```

## 7. WRITE_SECURE_SETTINGS grant (lets the app self-refresh the debug flag)

One-time grant so the app can re-arm the item-1 flag on its own — no more 48h cable ritual.
Survives reboot; never expires. Only writes that one flag (see `GlyphDebug.kt`).

```
adb shell pm grant com.akil.glyphlife android.permission.WRITE_SECURE_SETTINGS
```

Revert (back to manual 48h flag-setting):
```
adb shell pm revoke com.akil.glyphlife android.permission.WRITE_SECURE_SETTINGS
```

## Not changed (left at defaults)

- `nt_system_notification` — Nothing's system notification glyph. Currently **1 (on)**. It was
  toggled to 0 and back during debugging; net change is none. Notifications still light the Glyph
  normally.

---

## One-shot full revert

```
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB shell pm enable com.nothing.ntessentialspace
$ADB shell pm enable com.nothing.ntessentialrecorder
$ADB shell appops set com.akil.glyphlife WRITE_SETTINGS default
$ADB shell pm revoke com.akil.glyphlife android.permission.WRITE_SECURE_SETTINGS
$ADB shell settings put global nt_glyph_interface_debug_enable 0
# Accessibility + Notification access: toggle off in Settings (per items 4 and 5)
```

Uninstalling the app removes its accessibility/notification grants automatically; only the two
disabled Essential packages and the debug flag survive an uninstall — revert those manually.
