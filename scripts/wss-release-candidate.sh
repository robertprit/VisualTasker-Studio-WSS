#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RELEASE_APK="$ROOT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"

cd "$ROOT_DIR"

GRADLE_COMMON=(
  --no-daemon
  --console=plain
  "-Dorg.gradle.jvmargs=-Xmx2048m -XX:+UseSerialGC -Dfile.encoding=UTF-8"
  -Dorg.gradle.workers.max=2
)

./gradlew "${GRADLE_COMMON[@]}" -p visualtasker-blockeditor test
./gradlew "${GRADLE_COMMON[@]}" -p visualtasker-flowchart test
./gradlew "${GRADLE_COMMON[@]}" :app:testDebugUnitTest
./gradlew "${GRADLE_COMMON[@]}" :app:lintDebug
./gradlew "${GRADLE_COMMON[@]}" :app:assembleRelease
git diff --check

[[ -f "$RELEASE_APK" ]] || {
  echo "Release APK missing: $RELEASE_APK" >&2
  exit 2
}

echo "Release candidate artifact:"
ls -lh "$RELEASE_APK"
sha256sum "$RELEASE_APK"

if [[ -n "$(git status --short)" ]]; then
  echo "Working tree is dirty; commit and tag are intentionally deferred."
else
  echo "Working tree is clean."
fi

echo "WSS release-candidate gate passed. Signing, manual acceptance and tag remain separate gates."
