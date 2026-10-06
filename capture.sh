#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${CAPTURE_URL:?Set CAPTURE_URL environment variable}"
: "${CAPTURE_DIR:?Set CAPTURE_DIR environment variable}"
: "${RUNTIME_DIR:?Set RUNTIME_DIR environment variable}"
/usr/bin/time -p mkdir -p "$CAPTURE_DIR"
STATUS=0
if /usr/bin/time -p node "$RUNTIME_DIR/scripts/default-capture.mjs"; then
  STATUS=0
else
  STATUS=$?
fi
/usr/bin/time -p ls -lh "$CAPTURE_DIR"
/usr/bin/time -p test -f "$CAPTURE_DIR/final-desktop.png"
/usr/bin/time -p test -f "$CAPTURE_DIR/final-mobile.png"
exit "$STATUS"
