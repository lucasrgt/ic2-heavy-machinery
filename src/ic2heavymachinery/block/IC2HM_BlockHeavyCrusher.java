package ic2heavymachinery.block;

import net.minecraft.src.*;
import ic2heavymachinery.tile.*;
import ic2heavymachinery.gui.*;
import forge.ITextureProvider;
import java.util.Random;

public class IC2HM_BlockHeavyCrusher extends BlockContainer implements ITextureProvider {

    // Atlas tile indices (row 1):
    // 16 = heavy_crusher_front
    // 17 = heavy_crusher_front_active (placeholder)
    // 0 = sides (same as machine base)
    // 1 = top
    // 2 = bottom

    public IC2HM_BlockHeavyCrusher(int id) {
        super(id, 16, Material.iron);
        setBlockName("heavyCrusherController");
        setHardness(5.0F);
        setResistance(10.0F);
    }

    @Override
    public String getTextureFile() {
        return mod_IC2HeavyMachinery.ATLAS;
    }

    // Item/inventory texture
    @Override
    public int getBlockTextureFromSide(int side) {
        switch (side) {
            case 0: return 2;   // bottom
            case 1: return 1;   // top
            case 3: return 16;  // front (south = default facing)
            default: return 0;  // sides
        }
    }

    // In-world texture with facing
    @Override
    public int getBlockTexture(IBlockAccess world, int x, int y, int z, int side) {
        int meta = world.getBlockMetadata(x, y, z);
        int blockFacing = meta & 0x7;

        if (side == 0) return 2;  // bottom
        if (side == 1) return 1;  // top

        if (side == blockFacing) {
            return 16; // front
        }

        return 0; // sides
    }

    // Set facing based on player look direction, stored in metadata
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving entity) {
        int dir = MathHelper.floor_double((double)(entity.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        int facing;
        switch (dir) {
            case 0: facing = 2; break; // north
            case 1: facing = 5; break; // east
            case 2: facing = 3; break; // south
            case 3: facing = 4; break; // west
            default: facing = 2; break;
        }
        world.setBlockMetadataWithNotify(x, y, z, facing);
    }

    @Override
    public TileEntity getBlockEntity() {
        return new IC2HM_TileHeavyCrusher();
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int neighborId) {
        if (world.multiplayerWorld) return;
        IC2HM_TileHeavyCrusher tile = (IC2HM_TileHeavyCrusher) world.getBlockTileEntity(x, y, z);
        if (tile != null) {
            tile.checkStructure(world, x, y, z);
        }
    }

    @Override
    public void updateTick(World world, int x, int y, int z, java.util.Random rand) {
        if (world.multiplayerWorld) return;
        IC2HM_TileHeavyCrusher tile = (IC2HM_TileHeavyCrusher) world.getBlockTileEntity(x, y, z);
        if (tile != null) {
            tile.checkStructure(world, x, y, z);
        }
    }

    @Override
    public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
        if (player.isSneaking()) return false;
        if (world.multiplayerWorld) return true;
        IC2HM_TileHeavyCrusher tile = (IC2HM_TileHeavyCrusher) world.getBlockTileEntity(x, y, z);
        if (tile != null) {
            tile.checkStructure(world, x, y, z);
            ModLoader.OpenGUI(player, new IC2HM_GuiHeavyCrusher(player.inventory, tile));
        }
        return true;
    }

    @Override
    public void onBlockRemoval(World world, int x, int y, int z) {
        IC2HM_TileHeavyCrusher tile = (IC2HM_TileHeavyCrusher) world.getBlockTileEntity(x, y, z);
        if (tile != null) {
            tile.onRemoved();
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
