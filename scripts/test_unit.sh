#!/bin/bash
set -e
BASE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$BASE"

echo "=== Transpiling ==="
bash scripts/transpile.sh

echo "=== Recompiling ==="
cd "$BASE"
echo "recompile" | java -jar RetroMCP-Java-CLI.jar
cd "$BASE"

echo "=== Compiling tests ==="
TEST_SRC="$BASE/tests/src"
TEST_OUT="$BASE/tests/out"
MCP_BIN="$BASE/minecraft/bin"

rm -rf "$TEST_OUT" && mkdir -p "$TEST_OUT"

find "$TEST_SRC" -name "*.java" > /tmp/ic2hm_test_files.txt
TEST_COUNT=$(wc -l < /tmp/ic2hm_test_files.txt)

if [ "$TEST_COUNT" -eq 0 ]; then
    echo "No test files found in $TEST_SRC"
    exit 0
fi

javac \
    -cp "$MCP_BIN;$BASE/tests/libs/junit-4.13.2.jar;$BASE/tests/libs/hamcrest-core-1.3.jar" \
    -d "$TEST_OUT" \
    @/tmp/ic2hm_test_files.txt

echo "Compiled $TEST_COUNT test files"

echo "=== Running tests ==="
java \
    -cp "$TEST_OUT;$MCP_BIN;$BASE/tests/libs/junit-4.13.2.jar;$BASE/tests/libs/hamcrest-core-1.3.jar" \
    org.junit.runner.JUnitCore \
    $(find "$TEST_OUT" -name "*Test.class" | sed 's|.*/||;s|\.class||' | sed "s|^|net.minecraft.src.|")

echo "=== All tests passed ==="
