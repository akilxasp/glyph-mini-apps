# Glyph Mini Apps

A collection of tiny experiences for the 13×13 Glyph Matrix on the Nothing Phone (4a) Pro. Each mode runs from a Quick Settings tile, so there is no traditional launcher screen.

![Glyph Mini Apps showcase](docs/glyph-mini-apps-showcase-solid.png)

## Mini apps

| Mode | What it does | How to use it |
| --- | --- | --- |
| **Snake** | Runs a self-playing snake that searches for food and grows. | Tap the **Snake** tile to start. Tap it again to trigger the closing explosion and clear the matrix. |
| **Garden** | Animates a flower swaying in the wind, with drifting clouds and a wilting outro. | Tap the **Garden** tile to start or wilt the flower. Long-press the tile to choose a flower and switch between detailed and solid styles. |
| **Shooter** | Runs a miniature fixed-line shooter with marching invaders. | Tap the **Shooter** tile to start. Use the Essential Key to fire missiles. Tap the tile again to detonate the ship and stop. |
| **Reaction** | Measures how quickly you press the Essential Key after the matrix flashes. | Tap the **Reaction** tile, then press the Essential Key once to arm it. Press again after the flash. Your reaction time scrolls across the matrix; pressing early shows an X. |
| **Notify Glyph** | Flashes the sending app's icon on the matrix when a notification arrives. | Long-press the **Notify Glyph** tile to select apps, then tap the tile to enable or disable notification flashes. |
| **Now Playing** | Scrolls the current song title and artist across the matrix when a new track starts. | Grant notification access, then tap the **Now Playing** tile. Tap it again to stop listening. |

Snake is also available as an Always-on Glyph Toy. The Quick Settings tile and Glyph Toy are independent because Glyph Toys cannot be activated programmatically.

Only one main mode owns the matrix at a time. Starting another mode stops the previous one; notification icons temporarily borrow the display and then return it.

## Before you begin

- Nothing Phone (4a) Pro with its 13×13 Glyph Matrix
- Android 14 or newer
- Nothing OS system version **August 1, 2025 (`20250801`) or later**. This is the minimum version specified by Nothing for app-based Glyph Matrix control. The simplest way to meet it is to open **Settings → System → System update → Check for updates** and install every available update.
- [Android Studio](https://developer.android.com/studio) with Android SDK 35 installed
- A USB cable
- USB debugging enabled on the phone

## Build and install

### 1. Get this project

The recommended option is to clone it with Git:

```bash
git clone https://github.com/akilxasp/glyph-mini-apps.git
cd glyph-mini-apps
```

If you do not use Git, click **Code → Download ZIP** at the top of this GitHub page and extract the ZIP. The project folder will be named `glyph-mini-apps-main`.

### 2. Prepare the phone

1. Enable **Developer options** on the phone.
2. In Developer options, enable **USB debugging**.
3. Connect the phone to the computer with USB.
4. Accept the **Allow USB debugging?** message on the phone.

### 3. Run the installer

#### macOS or Linux

Open Terminal in the project folder and run:

```bash
./setup.sh
```

If you downloaded the ZIP and macOS says the script is not executable, run this once and try again:

```bash
chmod +x setup.sh
./setup.sh
```

#### Windows

Open the extracted project folder in File Explorer. Right-click an empty area, choose **Open in Terminal**, and run:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup.ps1
```

The script:

1. Downloads `glyph-matrix-sdk-2.0.aar` from the official [Nothing Glyph Matrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) if it is missing.
2. Uses Android Studio's bundled JetBrains JDK 21.
3. Checks that ADB can see the connected phone.
4. Builds and installs the debug app.
5. Enables Glyph Matrix debug access on the phone.

### Optional: keep Glyph debug access enabled

The Glyph debug flag can expire after roughly 48 hours or a reboot. You can rerun the installer when that happens.

Alternatively, after reviewing the source, allow the app to refresh the flag automatically:

```bash
./setup.sh --auto-refresh
```

On Windows, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup.ps1 -AutoRefresh
```

## Add the controls

1. Open the phone's Quick Settings editor.
2. Add any of these tiles: **Snake**, **Garden**, **Shooter**, **Reaction**, **Notify Glyph**, and **Now Playing**.
3. Tap a tile to start or stop that mode.

To use Snake as an Always-on Glyph Toy, open **Settings → Glyph Interface → Always-on Glyph Toy**, select **Snake**, and place the phone face-down. If the matrix stays blank after switching toys, toggle Glyph Interface off and on once.

## Enable optional features

### Shooter and Reaction controls

Shooter and Reaction use the phone's Essential Key through an accessibility service that reads key presses only; it does not read screen content.

> [!WARNING]
> Using the Essential Key with Shooter and Reaction requires disabling Nothing's **Essential Space** and **Essential Recorder** system apps. While they are disabled, the Essential Key will no longer open those features and you cannot use either app. Their existing notes and recordings are not deleted, and both apps can be restored later with ADB. This change is optional—the other mini apps work without it.

1. Open **Settings → Accessibility → Glyph Snake** and enable the service.
2. Follow the **Essential Space** and **Essential Recorder** sections in [PHONE_CHANGES.md](PHONE_CHANGES.md) to disable the two apps with ADB. The same document includes the commands to restore them.
3. Start Shooter or Reaction and press the Essential Key. It should now control the mini app instead of opening a Nothing system app.

### Notify Glyph and Now Playing

Open **Settings → Notifications → Device & app notifications → Glyph Snake** and grant notification access. Notify Glyph needs it to receive notifications; Now Playing needs it to read active media sessions.

## Project structure

- `app/src/main/java/com/akil/glyphlife/` — game engines, renderers, Android services, tiles, and settings screens
- `app/src/test/java/com/akil/glyphlife/` — small engine and renderer checks
- `PHONE_CHANGES.md` — device changes, reasons, and reversal commands

This is an independent project and is not affiliated with or endorsed by Nothing Technology Limited.
