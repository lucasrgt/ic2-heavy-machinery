package ic2heavymachinery.block;

import net.minecraft.src.*;
import ic2heavymachinery.*;
import ic2heavymachinery.tile.*;
import aero.machineapi.*;
import forge.ITextureProvider;
import java.util.Random;

public class IC2HM_BlockItemCable extends BlockContainer implements ITextureProvider {

    private final int tier; // 0=basic(1/t), 1=reinforced(8/t), 2=advanced(64/t)
    private final int atlasIndex; // texture index in atlas

    public IC2HM_BlockItemCable(int id, int tier, int atlasIndex) {
        super(id, Material.iron);
        this.tier = tier;
        this.atlasIndex = atlasIndex;
        this.blockIndexInTexture = atlasIndex;
        this.setBlockBounds(6.0F/16, 6.0F/16, 6.0F/16, 10.0F/16, 10.0F/16, 10.0F/16);
        setHardness(1.5F);
        setResistance(5.0F);
    }

    public int getTier() { return tier; }

    @Override
    public String getTextureFile() {
        return mod_IC2HeavyMachinery.ATLAS;
    }

    @Override
    protected TileEntity getBlockEntity() {
        return new IC2HM_TileItemCable(tier);
    }

    @Override public boolean isOpaqueCube() { return false; }
    @Override public boolean renderAsNormalBlock() { return false; }

    @Override
    public int getRenderType() {
        return mod_IC2HeavyMachinery.itemCableRenderID;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int neighborBlockID) {
        world.markBlockNeedsUpdate(x, y, z);
    }

    @Override
    public void onBlockRemoval(World world, int x, int y, int z) {
        TileEntity te = world.getBlockTileEntity(x, y, z);
        if (te instanceof IC2HM_TileItemCable) {
            ItemStack buffer = ((IC2HM_TileItemCable) te).getStackInSlot(0);
            if (buffer != null) {
                EntityItem drop = new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, buffer);
                world.entityJoinedWorld(drop);
            }
        }
        super.onBlockRemoval(world, x, y, z);
    }

    @Override
    public int quantityDropped(Random random) { return 1; }

    @Override
    public int idDropped(int metadata, Random random) { return this.blockID; }

    private static final int[][] DIRS = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};

    public boolean canConnectTo(IBlockAccess world, int x, int y, int z) {
        int id = world.getBlockId(x, y, z);
        // Connect to same-tier cables
        if (id == this.blockID) return true;
        // Connect to any other item cable tier
        if (isItemCable(id)) return true;
        // Connect to inventories (machines)
        TileEntity te = world.getBlockTileEntity(x, y, z);
        if (te instanceof IInventory) return true;
        // Connect to Aero ports of item type
        return Aero_PortRegistry.isPortOfType(x, y, z, Aero_PortRegistry.PORT_TYPE_ITEM);
    }

    private boolean isItemCable(int id) {
        return id == mod_IC2HeavyMachinery.ID_ITEM_CABLE_BASIC
            || id == mod_IC2HeavyMachinery.ID_ITEM_CABLE_REINFORCED
            || id == mod_IC2HeavyMachinery.ID_ITEM_CABLE_ADVANCED;
    }

    public boolean canConnectToSide(IBlockAccess world, int myX, int myY, int myZ, int side) {
        int nx = myX + DIRS[side][0], ny = myY + DIRS[side][1], nz = myZ + DIRS[side][2];
        if (!canConnectTo(world, nx, ny, nz)) return false;
        TileEntity myTe = world.getBlockTileEntity(myX, myY, myZ);
        if (myTe instanceof Aero_ISideConfigurable) {
            int mode = Aero_SideConfig.get(((Aero_ISideConfigurable) myTe).getSideConfig(), side, Aero_SideConfig.TYPE_ITEM);
            if (mode == Aero_SideConfig.MODE_NONE) return false;
        }
        TileEntity nTe = world.getBlockTileEntity(nx, ny, nz);
        if (nTe instanceof Aero_ISideConfigurable) {
            int opp = Aero_SideConfig.oppositeSide(side);
            int mode = Aero_SideConfig.get(((Aero_ISideConfigurable) nTe).getSideConfig(), opp, Aero_SideConfig.TYPE_ITEM);
            if (mode == Aero_SideConfig.MODE_NONE) return false;
        }
        return true;
    }

    private float[] calcBounds(IBlockAccess world, int i, int j, int k) {
        float min = 6.0F / 16.0F;
        float max = 10.0F / 16.0F;
        float minX = min, minY = min, minZ = min;
        float maxX = max, maxY = max, maxZ = max;

        for (int side = 0; side < 6; side++) {
            if (!canConnectToSide(world, i, j, k, side)) continue;
            switch (side) {
                case 0: minY = 0.0F; break;
                case 1: maxY = 1.0F; break;
                case 2: minZ = 0.0F; break;
                case 3: maxZ = 1.0F; break;
                case 4: minX = 0.0F; break;
                case 5: maxX = 1.0F; break;
            }
        }
        return new float[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int i, int j, int k) {
        float[] b = calcBounds(world, i, j, k);
        return AxisAlignedBB.getBoundingBoxFromPool(
            (double)i + b[0], (double)j + b[1], (double)k + b[2],
            (double)i + b[3], (double)j + b[4], (double)k + b[5]
        );
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int i, int j, int k) {
        float[] b = calcBounds(world, i, j, k);
        this.setBlockBounds(b[0], b[1], b[2], b[3], b[4], b[5]);
    }
}
