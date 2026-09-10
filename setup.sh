#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

case "${1:-}" in
  ""|--auto-refresh) ;;
  -h|--help)
    echo "Usage: ./setup.sh [--auto-refresh]"
    exit 0
    ;;
  *)
    echo "Unknown option: $1" >&2
    echo "Usage: ./setup.sh [--auto-refresh]" >&2
    exit 2
    ;;
esac

if [[ ! -f app/libs/glyph-matrix-sdk-2.0.aar ]]; then
  command -v curl >/dev/null || { echo "curl is required to download the Nothing Glyph Matrix SDK." >&2; exit 1; }
  echo "Downloading the Nothing Glyph Matrix SDK..."
  curl --fail --location --output app/libs/glyph-matrix-sdk-2.0.aar \
    https://raw.githubusercontent.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/main/glyph-matrix-sdk-2.0.aar
fi

# Gradle is pinned to JetBrains JDK 21; reuse Android Studio's bundled runtime when present.
for jdk in \
  "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  "/opt/android-studio/jbr"; do
  if [[ -x "$jdk/bin/java" ]]; then
    export JAVA_HOME="$jdk"
    export PATH="$JAVA_HOME/bin:$PATH"
    break
  fi
done

if ! command -v adb >/dev/null; then
  adb_path="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}/platform-tools/adb"
  [[ -x "$adb_path" ]] || { echo "ADB not found. Install Android platform-tools." >&2; exit 1; }
  export PATH="$(dirname "$adb_path"):$PATH"
fi

adb get-state >/dev/null
./gradlew installDebug
adb shell settings put global nt_glyph_interface_debug_enable 1

if [[ "${1:-}" == "--auto-refresh" ]]; then
  adb shell pm grant com.akil.glyphlife android.permission.WRITE_SECURE_SETTINGS
fi

echo "Installed. Add the Glyph Mini Apps tiles from the Quick Settings editor."
