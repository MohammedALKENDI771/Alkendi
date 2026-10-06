#!/usr/bin/env bash
# Serve the Quran Tadabbur static preview (dist/) in the foreground.
# Writes deployment-output.json for the controller. Per-command timing via time -p.
set -euo pipefail
time -p cd "$(dirname "$0")"
time -p test -f web/build.mjs
time -p test -f web/serve.mjs
PORT="${PORT:-3000}"
time -p test "$PORT" -ge 1
export PORT
PROJECT_DIR="$PWD"
WEB_DIR="${OPENCODE_WEB_DIR:-/home/runner/work/_temp/omgithub-web}"
if test -f package.json; then
  if test -f package-lock.json; then
    time -p npm ci --no-audit --no-fund
  else
    time -p npm install --no-audit --no-fund
  fi
fi
# Build the static output when needed (missing or stale vs quran-app assets).
time -p node web/build.mjs
time -p test -f dist/index.html
# Publish source + built directory for the controller (metadata only goes to OPENCODE_WEB_DIR).
time -p env WEB_DIR="$WEB_DIR" PROJECT_DIR="$PROJECT_DIR" node -e "require('fs').writeFileSync(process.env.WEB_DIR + '/deployment-output.json', JSON.stringify({ project: process.env.PROJECT_DIR, directory: process.env.PROJECT_DIR + '/dist' }))"
time -p cat "$WEB_DIR/deployment-output.json"
time -p echo "Serving $PROJECT_DIR/dist on PORT=$PORT"
time -p node web/serve.mjs
