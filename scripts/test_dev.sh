#!/bin/bash
# Dev mode: recompile (no obfuscation) + run from bin/ + JDWP debug port
# Much faster than test.sh — skips obfuscation and jar injection
# Enables hot swap: edit source → run hotswap.sh → IDE reloads classes
set -e
BASE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$BASE"

echo "=== Transpiling ==="
bash scripts/transpile.sh

echo "=== Recompiling (dev mode, no obfuscation) ==="
cd "$BASE/mcp"
echo "recompile" | java -jar ../RetroMCP-Java-CLI.jar
cd "$BASE"

# Ensure assets are in temp/merged for F9 hot reload
ASSETS="$BASE/src/ic2heavymachinery/assets"
if [ -d "$ASSETS" ]; then
    mkdir -p "$BASE/temp/merged"
    cp -r "$ASSETS"/* "$BASE/temp/merged/"
fi

mkdir -p tests/data/tmp
echo "=== Launching Minecraft (dev mode) ==="
echo "  JDWP debug port: 5005"
echo "  F9 = hot-reload textures | F10 = debug overlay"
echo "  Use hotswap.sh to push code changes without restarting"
LIBS="../mcp/libraries"
java -Xms1024M -Xmx1024M \
  -Daero.dev=true \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -Djava.library.path="$LIBS/natives" \
  -Djava.io.tmpdir="tests/data/tmp" \
  -cp "temp/merged;mcp/minecraft/bin;tests/data/minecraft_test.jar;$LIBS/net/java/jinput/jinput/2.0.5/jinput-2.0.5.jar;$LIBS/net/java/jutils/jutils/1.0.0/jutils-1.0.0.jar;$LIBS/org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar;$LIBS/org/lwjgl/lwjgl/lwjgl_util/2.9.4-nightly-20150209/lwjgl_util-2.9.4-nightly-20150209.jar;$LIBS/com/paulscode/codecjorbis/20230120/codecjorbis-20230120.jar;$LIBS/com/paulscode/codecwav/20101023/codecwav-20101023.jar;$LIBS/com/paulscode/libraryjavasound/20101123/libraryjavasound-20101123.jar;$LIBS/com/paulscode/librarylwjglopenal/20100824/librarylwjglopenal-20100824.jar;$LIBS/com/paulscode/soundsystem/20120107/soundsystem-20120107.jar" \
  net.minecraft.client.Minecraft
