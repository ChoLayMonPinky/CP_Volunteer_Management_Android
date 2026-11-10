#!/usr/bin/env bash
# Remove iCloud/Finder conflict copies that break Android builds.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "Stopping Gradle daemons..."
./gradlew --stop >/dev/null 2>&1 || true

echo "Removing local build folders on Desktop (if any)..."
rm -rf app/build build .kotlin
# Keep .gradle for Studio but wipe corrupted caches
rm -rf .gradle/configuration-cache .gradle/buildOutputCleanup .gradle/*/fileHashes 2>/dev/null || true

echo "Removing conflict copies (* 2.*, * 3.*, * 4.*)..."
python3 - <<'PY'
from pathlib import Path
import re
root = Path(".").resolve()
pat = re.compile(r" \d+\.[^/]+$")
removed = 0
for p in root.rglob("*"):
    if p.is_file() and pat.search(p.name):
        p.unlink(missing_ok=True)
        removed += 1
print(f"Removed {removed} conflict files")
PY

echo "Done. Next: Android Studio → Sync Project with Gradle Files → Run app"
