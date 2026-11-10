#!/usr/bin/env bash
# Build + clean-install the debug APK (fixes Studio "manifest modified" install failures).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export PATH="$JAVA_HOME/bin:${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools:$PATH"
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"

echo "Building debug APK..."
./gradlew :app:assembleDebug --no-configuration-cache

APK="$ROOT/dist/app-debug.apk"
if [[ ! -f "$APK" ]]; then
  APK="$HOME/Library/Caches/volunteer_management_android/build/app/outputs/apk/debug/app-debug.apk"
fi

if [[ ! -f "$APK" ]]; then
  echo "APK not found after build."
  exit 1
fi

echo "Using APK: $APK"
echo "Devices:"
"$ADB" devices

echo "Uninstalling old app (if present)..."
"$ADB" uninstall dev.tintwym >/dev/null 2>&1 || true

echo "Installing..."
"$ADB" install -r -t "$APK"

echo "Launching..."
"$ADB" shell am start -n dev.tintwym/.MainActivity

echo "Done."
