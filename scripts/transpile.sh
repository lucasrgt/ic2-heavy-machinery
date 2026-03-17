#!/bin/bash
# Transpiles organized src/ic2heavymachinery/ + libraries/ -> flat mcp/minecraft/src/net/minecraft/src/
# Rewrites packages and removes internal imports so RetroMCP can compile
set -e
BASE="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$BASE/src/ic2heavymachinery"
LIBS="$BASE/../../libraries"
DEST="$BASE/mcp/minecraft/src/net/minecraft/src"

# Remove old transpiled mod files (only IC2HM_ prefixed + Aero_ prefixed + mod_IC2HeavyMachinery)
find "$DEST" -maxdepth 1 -name "IC2HM_*.java" -delete 2>/dev/null || true
find "$DEST" -maxdepth 1 -name "Aero_*.java" -delete 2>/dev/null || true
find "$DEST" -maxdepth 1 -name "mod_IC2Heavy*.java" -delete 2>/dev/null || true

# Transpile function: flatten packages and strip internal imports
transpile_file() {
    local file="$1"
    local filename
    filename=$(basename "$file")
    sed \
        -e 's/^package ic2heavymachinery\(\.[a-z]*\)\?;/package net.minecraft.src;/' \
        -e 's/^package aero\.\([a-z_]*\);/package net.minecraft.src;/' \
        -e '/^import ic2heavymachinery\./d' \
        -e '/^import static ic2heavymachinery\./d' \
        -e '/^import aero\./d' \
        -e '/^import static aero\./d' \
        -e '/^import net\.minecraft\.src\.\*;/d' \
        "$file" > "$DEST/$filename"
}

# Transpile libraries (aero modellib, machineapi, etc.)
LIB_COUNT=0
if [ -d "$LIBS" ]; then
    find "$LIBS" -name "*.java" | while read -r file; do
        transpile_file "$file"
    done
    LIB_COUNT=$(find "$LIBS" -name '*.java' | wc -l)
fi

# Transpile mod source
find "$SRC" -name "*.java" | while read -r file; do
    transpile_file "$file"
done
SRC_COUNT=$(find "$SRC" -name '*.java' | wc -l)

echo "Transpiled $LIB_COUNT library + $SRC_COUNT mod files to $DEST"

# Copy assets (textures, models) to temp/merged for jar injection
ASSETS="$SRC/assets"
if [ -d "$ASSETS" ]; then
    mkdir -p "$BASE/temp/merged"
    cp -r "$ASSETS"/* "$BASE/temp/merged/"
    echo "Copied assets to temp/merged/"
fi
