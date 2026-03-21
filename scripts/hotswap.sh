#!/bin/bash
# Hot swap: transpile + recompile only (no restart needed)
# Run while Minecraft is running via test_dev.sh
# Updated classes in bin/ are picked up by DCEVM or IDE debug reload
set -e
BASE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$BASE"

echo "=== Transpiling ==="
bash scripts/transpile.sh

echo "=== Recompiling ==="
cd "$BASE/mcp"
echo "recompile" | java -jar ../RetroMCP-Java-CLI.jar
cd "$BASE"

echo "=== Done! Classes updated in mcp/minecraft/bin/ ==="
echo "  With DCEVM: changes are live immediately"
echo "  Without DCEVM: press Build in your IDE to hot swap method bodies"
echo "  Textures: press F9 in-game to reload from disk"
