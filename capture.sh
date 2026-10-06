#!/usr/bin/env bash
# Capture desktop + mobile screenshots of the running preview.
# Env: CAPTURE_URL (exact URL to open), CAPTURE_DIR (output outside the source tree).
# Exit 75 = temporary navigation/browser infrastructure failure; exit 1 = script/rendering defect.
# Leaves the app server running. Per-command timing via time -p.
set -euo pipefail
time -p cd "$(dirname "$0")"
time -p test -n "${CAPTURE_URL:-}" || { echo "CAPTURE_URL is not set" >&2; exit 1; }
time -p test -n "${CAPTURE_DIR:-}" || { echo "CAPTURE_DIR is not set" >&2; exit 1; }
time -p mkdir -p "$CAPTURE_DIR"
time -p test -d "$HOME/.local/share/omgithub-playwright/node_modules"
time -p test -f "$HOME/.local/share/omgithub-playwright/linux.json"
CAPTURE_SCRIPT="${RUNNER_TEMP:-/tmp}/quran-capture.mjs"
time -p cat > "$CAPTURE_SCRIPT" <<'NODE_EOF'
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { createRequire } from 'node:module';
const runtime = join(process.env.HOME, '.local/share/omgithub-playwright');
const require = createRequire(join(runtime, 'package.json'));
const { chromium } = require('playwright');
const config = JSON.parse(readFileSync(join(runtime, 'linux.json'), 'utf8'));
process.env.DISPLAY ||= ':' + readFileSync(join(runtime, 'display'), 'utf8').trim();
const url = process.env.CAPTURE_URL, output = process.env.CAPTURE_DIR;
if (!url || !output) { console.error('Set CAPTURE_URL and CAPTURE_DIR.'); process.exit(1); }
const transient = (error) => { throw Object.assign(error, { exitCode: 75 }); };
let browser;
try {
  browser = await chromium.launch({ ...config.browser.launchOptions, timeout: 30000 }).catch(transient);
  for (const [name, width, height] of [['desktop', 1440, 900], ['mobile', 390, 844]]) {
    const page = await browser.newPage({ viewport: { width, height } }).catch(transient);
    page.setDefaultTimeout(30000);
    page.on('pageerror', (error) => console.error(`pageerror(${name}):`, error.message));
    const response = await page.goto(url, { waitUntil: 'load', timeout: 45000 }).catch(transient);
    if (!response?.ok()) {
      const s = response?.status();
      throw Object.assign(new Error(`HTTP ${s} loading preview`), { exitCode: !s || [408, 429, 500, 502, 503, 504].includes(s) ? 75 : 1 });
    }
    try {
      await page.locator('body').waitFor({ state: 'visible' });
      await page.waitForFunction(() => document.fonts.status === 'loaded');
    } catch (error) { throw Object.assign(error, { exitCode: 1 }); }
    await page.waitForTimeout(1500);
    await page.screenshot({ path: join(output, `final-${name}.png`), timeout: 30000 }).catch((error) => {
      if (error.name === 'TimeoutError' || !browser.isConnected()) transient(error);
      throw error;
    });
    console.log(`captured final-${name}.png`);
    await page.close();
  }
} catch (error) { console.error(error); process.exitCode = error.exitCode || 1; }
finally { await browser?.close().catch((error) => { console.error(error); process.exitCode ||= 75; }); }
NODE_EOF
time -p node --check "$CAPTURE_SCRIPT"
time -p node "$CAPTURE_SCRIPT"
time -p test -f "$CAPTURE_DIR/final-desktop.png"
time -p test -f "$CAPTURE_DIR/final-mobile.png"
time -p ls -la "$CAPTURE_DIR"
