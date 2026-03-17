package ic2heavymachinery.block;

import net.minecraft.src.*;
import ic2heavymachinery.tile.*;
import ic2heavymachinery.gui.*;
import forge.ITextureProvider;
import java.util.Random;

public class IC2HM_BlockAlloyBlender extends BlockContainer implements ITextureProvider {

    // Atlas tile indices (from gen-atlas.js):
    // 0 = heavy_chassis (side)
    // 1 = base_top
    // 2 = base_bottom
    // 3 = alloy_blender_front
    // 4-7 = alloy_blender_front_active_0..3
    // 8 = alloy_blender_top
    // 16 = alloy_blender_side

    public IC2HM_BlockAlloyBlender(int id) {
        super(id, 3, Material.iron);
        setBlockName("alloyBlender");
        setHardness(5.0F);
        setResistance(10.0F);
    }

    @Override
    public String getTextureFile() {
        return mod_IC2HeavyMachinery.ATLAS;
    }

    // Item/inventory texture (no tile entity available)
    @Override
    public int getBlockTextureFromSide(int side) {
        switch (side) {
            case 0: return 2;   // bottom
            case 1: return 8;   // top
            case 3: return 3;   // front (south = default facing display)
            default: return 0; // sides
        }
    }

    // In-world texture with facing + active animation
    @Override
    public int getBlockTexture(IBlockAccess world, int x, int y, int z, int side) {
        IC2HM_TileAlloyBlender tile = (IC2HM_TileAlloyBlender) world.getBlockTileEntity(x, y, z);
        if (tile == null) return getBlockTextureFromSide(side);

        if (side == 0) return 2;  // bottom
        if (side == 1) return 8;  // top

        if (side == tile.facing) {
            // Front face
            if (tile.active) {
                return 4 + tile.getAnimFrame(); // front_active_0..3
            }
            return 3; // front idle
        }

        return 0; // side
    }

    // Set facing based on player look direction
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving entity) {
        int dir = MathHelper.floor_double((double)(entity.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        IC2HM_TileAlloyBlender tile = (IC2HM_TileAlloyBlender) world.getBlockTileEntity(x, y, z);
        if (tile != null) {
            // 0=south, 1=west, 2=north, 3=east
            switch (dir) {
                case 0: tile.facing = 2; break; // north
                case 1: tile.facing = 5; break; // east
                case 2: tile.facing = 3; break; // south
                case 3: tile.facing = 4; break; // west
            }
        }
    }

    @Override
    public TileEntity getBlockEntity() {
        return new IC2HM_TileAlloyBlender();
    }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (player.isSneaking()) return false;
        if (world.multiplayerWorld) return true;
        IC2HM_TileAlloyBlender tile = (IC2HM_TileAlloyBlender) world.getBlockTileEntity(x, y, z);
        ModLoader.OpenGUI(player, new IC2HM_GuiAlloyBlender(player.inventory, tile));
        return true;
    }

    @Override
    public void onBlockRemoval(World world, int x, int y, int z) {
        IC2HM_TileAlloyBlender tile = (IC2HM_TileAlloyBlender) world.getBlockTileEntity(x, y, z);
        if (tile != null) {
            Random rand = new Random();
            for (int i = 0; i < tile.getSizeInventory(); i++) {
                ItemStack stack = tile.getStackInSlot(i);
                if (stack != null) {
                    float rx = rand.nextFloat() * 0.8F + 0.1F;
                    float ry = rand.nextFloat() * 0.8F + 0.1F;
                    float rz = rand.nextFloat() * 0.8F + 0.1F;
                    EntityItem entity = new EntityItem(world,
                            (double) x + rx, (double) y + ry, (double) z + rz,
                            new ItemStack(stack.itemID, stack.stackSize, stack.getItemDamage()));
                    entity.motionX = rand.nextGaussian() * 0.05;
                    entity.motionY = rand.nextGaussian() * 0.05 + 0.2;
                    entity.motionZ = rand.nextGaussian() * 0.05;
                    world.entityJoinedWorld(entity);
                }
            }
        }
        super.onBlockRemoval(world, x, y, z);
    }
}
