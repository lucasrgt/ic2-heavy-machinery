#!/bin/bash
# Hot restart: kill Minecraft + transpile + recompile + relaunch (dev mode)
# Use when hot swap isn't enough (structural changes, new classes, etc.)
set -e
BASE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$BASE"

echo "=== Killing Minecraft ==="
taskkill //F //IM java.exe 2>/dev/null || true
sleep 1

echo "=== Restarting in dev mode ==="
bash scripts/test_dev.sh
