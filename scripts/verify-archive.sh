#!/usr/bin/env sh
set -eu
test -f README.md
test -f .github/workflows/ci.yml
test -f web/src/components/AndampApp.svelte
test -f android/app/src/main/java/app/andamp/dev/playback/PlaybackService.kt
test -f docs/ARCHITECTURE.md
if find . -type f \( -name '*.jks' -o -name '*.keystore' -o -name '.env' \) | grep -q .; then
  echo "Secret-like files found" >&2; exit 1
fi
echo "Archive source verification passed."
