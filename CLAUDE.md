# IC2 Heavy Machinery — Minecraft Beta 1.7.3 IC2 Addon

## Workflow
- Edit mod source ONLY in `src/ic2heavymachinery/` (organized packages)
- NEVER edit `mcp/minecraft/src/net/minecraft/src/IC2HM_*.java` directly — those are transpiled output
- `bash scripts/test.sh` auto-transpiles → builds → injects → launches
- `bash scripts/test_unit.sh` auto-transpiles → recompiles → runs JUnit tests

## Build Commands
- Test (in-game): `bash scripts/test.sh` (transpile + build + launch)
- Unit tests: `bash scripts/test_unit.sh` (transpile + recompile + JUnit)
- Transpile only: `bash scripts/transpile.sh` (src/ic2heavymachinery/ → mcp/minecraft/src/)
- Build only: `cd mcp && echo "build" | java -jar RetroMCP-Java-CLI.jar`
- First-time setup: `bash scripts/setup.sh` (decompile + inject deps + updatemd5)

## Transpile System
- Source of truth: `src/ic2heavymachinery/` with organized Java packages
- `scripts/transpile.sh` flattens to `mcp/minecraft/src/net/minecraft/src/` for RetroMCP
- Rewrites `package ic2heavymachinery.xxx;` → `package net.minecraft.src;`
- Removes `import ic2heavymachinery.*`, `import aero.*`, `import ic2.*`, `import net.minecraft.src.*` (all redundant after flatten)
- Preserves external imports (org.lwjgl, java.util, etc.)
- Also copies `src/ic2heavymachinery/assets/` → `temp/merged/` (textures for jar injection)

## Project Structure
- **Mod source (edit here):** `src/ic2heavymachinery/` with subpackages:
  - `api/` — addon-specific interfaces if needed
  - `block/` — Block subclasses
  - `tile/` — TileEntity subclasses
  - `gui/` — GuiContainer subclasses
  - `container/` — Container subclasses
  - `item/` — Item subclasses
  - `recipe/` — Recipe registries
  - `render/` — Block/Item renderers
  - `slot/` — Custom Slot subclasses
  - `assets/blocks/` — Block textures & atlas (ic2_heavy_machinery.png)
  - `assets/gui/` — GUI textures
  - `assets/item/` — Item textures
  - `assets/models/` — 3D models (.obj, .anim.json)
  - `mod_IC2HeavyMachinery.java` — main mod class (package root)
- **Transpiled output (don't edit):** `mcp/minecraft/src/net/minecraft/src/`
- **Libraries:** `../../../libraries/` (modellib + machineapi, shared across workspace)
- **Scripts:** `scripts/` (setup.sh, test.sh, test_unit.sh, transpile.sh)
- **Tests:** `tests/` (data/, libs/, out/, src/)
- **Tools:** `tools/` (gen-atlas.js)
- **Design doc:** `DESIGN.md` (machine specs, tiers, recipes)

## Naming Conventions
- Mod classes use `IC2HM_` prefix (e.g., `IC2HM_BlockHeavyChassis`, `IC2HM_TileHeavyCompressor`)
- Block IDs start at 230 (RetroNism uses 200-229)
- Item IDs start at 600 (RetroNism uses 500-599)

## Dependencies
- IC2 v1.00 (in `mcp/deps/ic2.zip`)
- ModLoader + ModLoaderMP + Forge 1.0.6 (in `mcp/deps/`)
- AeroModelLib + AeroMachineAPI (in `../../../libraries/`)
- LWJGL runtime libraries (in `mcp/libraries/`)

## Texture Atlas
- `src/ic2heavymachinery/assets/blocks/ic2_heavy_machinery.png` (256x256, 16x16 tiles)
- Row 0: Alloy Blender (side, top, bottom, front, front_active×4)
- Row 1-15: Reserved for future machines
- Missing texture: IC2-style pink tile (#D67FFF border #6B3F7F)
- Regenerate: `node tools/gen-atlas.js`

## Running the game
- Always kill existing java processes and then start the game again after updating the code

## Language
- User communicates in Portuguese (BR). Respond in Portuguese.
