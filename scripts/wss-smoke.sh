#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.visualtasker.wss"
ACTIVITY="$PACKAGE/.MainActivity"

cd "$ROOT_DIR"
./gradlew :app:testDebugUnitTest :app:assembleDebug

if [[ "${1:-}" != "--device" ]]; then
  echo "WSS smoke passed: unit tests and debug APK."
  exit 0
fi

command -v adb >/dev/null || { echo "adb is required for --device" >&2; exit 2; }
DEVICE_COUNT="$(adb devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
[[ "$DEVICE_COUNT" -eq 1 ]] || {
  echo "Exactly one ready Android device is required; found $DEVICE_COUNT." >&2
  exit 3
}

adb logcat -c
adb install -r "$APK"
adb shell am force-stop "$PACKAGE"
adb shell am start -W -n "$ACTIVITY"
sleep 2

FOREGROUND="$(adb shell dumpsys activity activities | grep -m1 -E 'topResumedActivity|mResumedActivity' || true)"
[[ "$FOREGROUND" == *"$PACKAGE/.MainActivity"* ]] || {
  echo "WSS MainActivity is not foreground: $FOREGROUND" >&2
  exit 4
}

CRASHES="$(adb logcat -d -v brief 'AndroidRuntime:E' '*:S' || true)"
[[ -z "$CRASHES" ]] || {
  echo "AndroidRuntime crash detected:" >&2
  echo "$CRASHES" >&2
  exit 5
}

echo "WSS device smoke passed: install, cold launch, foreground, crash check."
