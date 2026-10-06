#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
PORT="${PORT:-3000}"
export PORT
PROJECT_ROOT="/home/runner/work/Alkendi/Alkendi"
DIST_DIR="/home/runner/work/Alkendi/Alkendi/dist"
WEB_DIR="${OPENCODE_WEB_DIR:-/home/runner/work/_temp/omgithub-web}"
/usr/bin/time -p mkdir -p "$DIST_DIR"
/usr/bin/time -p mkdir -p "$WEB_DIR"
if /usr/bin/time -p test -f package.json; then
  /usr/bin/time -p npm install --no-audit --no-fund
  /usr/bin/time -p npm run build --if-present
fi
/usr/bin/time -p cp -f mursalat-poster.html "$DIST_DIR/index.html"
/usr/bin/time -p /usr/bin/printf '%s' '{"project":"/home/runner/work/Alkendi/Alkendi","directory":"/home/runner/work/Alkendi/Alkendi/dist"}' > "$WEB_DIR/deployment-output.json"
/usr/bin/time -p cat "$WEB_DIR/deployment-output.json"
/usr/bin/time -p ls -lh "$DIST_DIR"
/usr/bin/time -p python3 -m http.server "$PORT" --directory "$DIST_DIR" --bind 0.0.0.0
