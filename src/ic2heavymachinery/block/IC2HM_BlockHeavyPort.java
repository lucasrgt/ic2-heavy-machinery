package ic2heavymachinery.block;

import net.minecraft.src.*;
import ic2heavymachinery.mod_IC2HeavyMachinery;
import ic2heavymachinery.tile.IC2HM_TileHeavyPort;
import ic2heavymachinery.tile.IC2HM_TileEnergyPort;
import forge.ITextureProvider;

/**
 * Port block for Heavy Crusher multiblock.
 * Metadata determines port type and texture color:
 *   0 = energy (red)
 *   1 = item   (green)
 *   2 = fluid  (blue) — future
 *
 * This block extends BlockContainer and returns a default TileEntity.
 * The controller's applyPortBlocks replaces the TE with the correct type after placement.
 */
public class IC2HM_BlockHeavyPort extends BlockContainer implements ITextureProvider {

    private static final int TEX_ENERGY = 30; // red — row 1 middle
    private static final int TEX_FLUID  = 46; // blue — row 2 middle
    private static final int TEX_ITEM   = 62; // green — row 3 middle

    public IC2HM_BlockHeavyPort(int id) {
        super(id, TEX_ENERGY, Material.iron);
        setHardness(5.0F);
        setResistance(10.0F);
        setStepSound(Block.soundMetalFootstep);
        setBlockName("ic2hmHeavyPort");
    }

    @Override
    public String getTextureFile() {
        return mod_IC2HeavyMachinery.ATLAS;
    }

    @Override
    public int getBlockTextureFromSideAndMetadata(int side, int metadata) {
        switch (metadata) {
            case 0:  return TEX_ENERGY;
            case 1:  return TEX_ITEM;
            case 2:  return TEX_FLUID;
            default: return TEX_ENERGY;
        }
    }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null) return false;
        String className = held.getItem().getClass().getSimpleName();
        if (!className.contains("Wrench")) return false;

        if (world.multiplayerWorld) return true;

        // Only item ports (meta 1) are configurable
        int meta = world.getBlockMetadata(x, y, z);
        if (meta != 1) return false;

        TileEntity te = world.getBlockTileEntity(x, y, z);
        if (te instanceof IC2HM_TileHeavyPort) {
            IC2HM_TileHeavyPort port = (IC2HM_TileHeavyPort) te;
            port.toggleMode();
            String mode = port.isOutputMode() ? "\u00a7cOutput" : "\u00a7aInput";
            player.addChatMessage("Port mode: " + mode);
        }
        return true;
    }

    /**
     * Default TE — returns an item port.
     * The controller replaces this with the correct type (energy/item) after formation.
     */
    @Override
    protected TileEntity getBlockEntity() {
        return new IC2HM_TileHeavyPort();
    }

    @Override
    public void onBlockRemoval(World world, int x, int y, int z) {
        TileEntity te = world.getBlockTileEntity(x, y, z);
        if (te instanceof IC2HM_TileEnergyPort) {
            ((IC2HM_TileEnergyPort) te).onRemoved();
        } else if (te instanceof IC2HM_TileHeavyPort) {
            ((IC2HM_TileHeavyPort) te).onRemoved();
        }
        super.onBlockRemoval(world, x, y, z);
    }

    @Override
    public int quantityDropped(java.util.Random rand) {
        return 0; // Port blocks drop nothing — restored to machine blocks on deformation
    }
}
