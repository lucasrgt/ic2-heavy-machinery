#!/bin/bash
# First-time setup: copy parent RetroNism MCP, inject IC2 deps, create deobfuscated stubs
# Prereq: parent RetroNism must have a working mcp/ (run its setup first)
set -e
BASE="$(cd "$(dirname "$0")/.." && pwd)"
PARENT="$BASE/.."
MCP="$BASE/mcp"

echo "=== Step 1: Copy parent RetroNism mcp/ ==="
if [ ! -d "$PARENT/mcp/minecraft/jars/deobfuscated.jar" ]; then
    echo "ERROR: Parent RetroNism mcp/ not found or not set up. Run RetroNism setup first."
    exit 1
fi
rm -rf "$MCP"
cp -r "$PARENT/mcp" "$MCP"
echo "Copied parent mcp/"

echo "=== Step 2: Remove RetroNism source files ==="
cd "$MCP/minecraft/src/net/minecraft/src"
rm -f Retronism_*.java Aero_*.java mod_Retronism.java
echo "Cleaned RetroNism files"

echo "=== Step 3: Fix options.cfg ==="
cp "$BASE/options.cfg" "$MCP/options.cfg"
echo "options.cfg updated"

echo "=== Step 4: Inject IC2 into minecraft.jar (runtime) ==="
TMP=$(mktemp -d)
cd "$TMP"
unzip -o "$BASE/deps/ic2.zip" > /dev/null 2>&1
jar uf "$MCP/jars/minecraft.jar" ic2/*.class mod_IC2.class IC2sprites/ armor/ 2>/dev/null || true
echo "IC2 injected into minecraft.jar"

echo "=== Step 5: Inject IC2 into deobfuscated.jar (compile classpath) ==="
jar uf "$MCP/minecraft/jars/deobfuscated.jar" ic2/*.class mod_IC2.class
echo "IC2 classes injected into deobfuscated.jar"

echo "=== Step 6: Create deobfuscated IC2 stubs ==="
# IC2 binary uses obfuscated types (ow=TileEntity, fd=World).
# We create deobfuscated interface stubs so IC2HM can compile against them.
STUBS=$(mktemp -d)
mkdir -p "$STUBS/ic2"

cat > "$STUBS/ic2/IEnergyTile.java" << 'JAVA'
package ic2;
public interface IEnergyTile {
    boolean isAddedToEnergyNet();
    void setAddedToEnergyNet(boolean added);
}
JAVA

cat > "$STUBS/ic2/IEnergyAcceptor.java" << 'JAVA'
package ic2;
public interface IEnergyAcceptor extends IEnergyTile {
    boolean acceptsEnergyFrom(net.minecraft.src.TileEntity emitter, Direction direction);
}
JAVA

cat > "$STUBS/ic2/IEnergySink.java" << 'JAVA'
package ic2;
public interface IEnergySink extends IEnergyAcceptor {
    boolean demandsEnergy();
    int injectEnergy(Direction direction, int amount);
}
JAVA

cat > "$STUBS/ic2/Direction.java" << 'JAVA'
package ic2;
public enum Direction {
    Direction_XN, Direction_XP, Direction_YN, Direction_YP, Direction_ZN, Direction_ZP;
    public net.minecraft.src.TileEntity applyToTileEntity(net.minecraft.src.World world, net.minecraft.src.TileEntity te) { return null; }
    public Direction getInverse() { return this; }
}
JAVA

cat > "$STUBS/ic2/EnergyNet.java" << 'JAVA'
package ic2;
public class EnergyNet {
    public static final double minConductionLoss = 0.01;
    public static EnergyNet getForWorld(net.minecraft.src.World world) { return null; }
    public static void onTick() {}
    public void addTileEntity(net.minecraft.src.TileEntity te) {}
    public void removeTileEntity(net.minecraft.src.TileEntity te) {}
    public long getTotalEnergyConducted(net.minecraft.src.TileEntity te) { return 0; }
}
JAVA

javac -cp "$MCP/minecraft/jars/deobfuscated.jar" "$STUBS/ic2/"*.java
cd "$STUBS" && jar uf "$MCP/minecraft/jars/deobfuscated.jar" ic2/*.class
echo "Deobfuscated IC2 stubs injected"

echo "=== Step 7: Create mod_IC2 stub (net.minecraft.src package) ==="
MOD_STUB=$(mktemp -d)
mkdir -p "$MOD_STUB/net/minecraft/src"
cat > "$MOD_STUB/net/minecraft/src/mod_IC2.java" << 'JAVA'
package net.minecraft.src;
public class mod_IC2 extends BaseModMp {
    public static Block blockMachine;
    public static Block blockElectric;
    public static Item itemDustIron;
    public static Item itemDustGold;
    public static Item itemDustCopper;
    public static Item itemDustTin;
    public static Item itemDustBronze;
    public static Item itemCable;
    public static Item itemPartCircuitAdv;
    public static Item itemIngotAdvIron;
    public String Version() { return ""; }
    public void load() {}
}
JAVA
javac -cp "$MCP/minecraft/jars/deobfuscated.jar" "$MOD_STUB/net/minecraft/src/mod_IC2.java"
cd "$MOD_STUB" && jar uf "$MCP/minecraft/jars/deobfuscated.jar" net/minecraft/src/mod_IC2.class
echo "mod_IC2 stub injected"

echo "=== Step 8: Update MD5 baseline ==="
cd "$MCP" && echo "updatemd5" | java -jar "$BASE/RetroMCP-Java-CLI.jar"

# Cleanup
rm -rf "$TMP" "$STUBS" "$MOD_STUB"

echo ""
echo "=== Setup complete ==="
echo "Run 'bash scripts/test.sh' to transpile, build, and launch"
