#!/bin/bash
set -e
BASE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$BASE"

echo "=== Transpiling ==="
bash scripts/transpile.sh

echo "=== Building ==="
cd "$BASE/mcp"
echo "build" | java -jar ../RetroMCP-Java-CLI.jar
cd "$BASE"

echo "=== Preparing test jar ==="
cp tests/data/minecraft_test.jar tests/data/minecraft_run.jar

# Inject mod classes from build zip into run jar
TMP="$BASE/temp/build_extract"
rm -rf "$TMP" && mkdir -p "$TMP"
cd "$TMP"
jar xf "$BASE/mcp/build/minecraft.zip"
jar uf "$BASE/tests/data/minecraft_run.jar" *.class 2>/dev/null || true
echo "Injected mod classes"
cd "$BASE"
rm -rf "$TMP"

# Inject custom textures (atlas, models) into run jar
if [ -d "$BASE/temp/merged" ]; then
    cd "$BASE/temp/merged"
    jar uf "$BASE/tests/data/minecraft_run.jar" .
    echo "Injected custom textures and models"
fi

cd "$BASE"

# Runtime libraries (use relative paths for Windows Java compatibility)
LIBS="../mcp/libraries"

mkdir -p tests/data/tmp
echo "=== Launching Minecraft ==="
java -Xms1024M -Xmx1024M \
  -Djava.library.path="$LIBS/natives" \
  -Djava.io.tmpdir="tests/data/tmp" \
  -cp "tests/data/minecraft_run.jar;$LIBS/net/java/jinput/jinput/2.0.5/jinput-2.0.5.jar;$LIBS/net/java/jutils/jutils/1.0.0/jutils-1.0.0.jar;$LIBS/org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar;$LIBS/org/lwjgl/lwjgl/lwjgl_util/2.9.4-nightly-20150209/lwjgl_util-2.9.4-nightly-20150209.jar;$LIBS/com/paulscode/codecjorbis/20230120/codecjorbis-20230120.jar;$LIBS/com/paulscode/codecwav/20101023/codecwav-20101023.jar;$LIBS/com/paulscode/libraryjavasound/20101123/libraryjavasound-20101123.jar;$LIBS/com/paulscode/librarylwjglopenal/20100824/librarylwjglopenal-20100824.jar;$LIBS/com/paulscode/soundsystem/20120107/soundsystem-20120107.jar" \
  net.minecraft.client.Minecraft
