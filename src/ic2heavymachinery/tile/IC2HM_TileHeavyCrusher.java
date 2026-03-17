package ic2heavymachinery.tile;

import net.minecraft.src.*;
import ic2.*;
import ic2heavymachinery.mod_IC2HeavyMachinery;
import ic2heavymachinery.IC2HM_IC2Ref;
import ic2heavymachinery.recipe.IC2HM_RecipesHeavyCrusher;
import aero.machineapi.Aero_ISlotAccess;

public class IC2HM_TileHeavyCrusher extends TileEntity implements IInventory, IEnergySink, Aero_ISlotAccess {

    private ItemStack[] inventory = new ItemStack[12]; // 0-3 input, 4-11 output
    public boolean isFormed = false;

    public int energy = 0;
    public int maxEnergy = 128000; // HV storage
    public int[] processTime = new int[4]; // per-lane progress
    public int maxProcessTime = 200;
    public static final int ENERGY_PER_TICK = 64;
    public static final int MAX_INPUT = 512; // HV

    private boolean addedToEnergyNet = false;

    // Structure type constants
    private static final int TYPE_AIR = 0;
    private static final int TYPE_CONTROLLER = 1;
    private static final int TYPE_ADVANCED_MACHINE_BLOCK = 2;
    private static final int TYPE_MACHINE_BLOCK = 3;

    // 3x3x3 structure definition [y][z][x]
    // Controller at center of south face (y=1, z=2, x=1)
    private static final int[][][] STRUCTURE = {
        { // Layer 0 (bottom)
            {2, 3, 2},
            {3, 3, 3},
            {2, 3, 2},
        },
        { // Layer 1 (middle — controller level)
            {3, 3, 3},
            {3, 3, 3},
            {3, 1, 3},
        },
        { // Layer 2 (top)
            {2, 3, 2},
            {3, 3, 3},
            {2, 3, 2},
        },
    };

    // Port positions in structure space: {structX, structY, structZ, portType, ioMode}
    // portType: 1=energy, 0=item
    // ioMode: 0=input, 1=output (only used for item ports)
    private static final int[][] PORTS = {
        {1, 0, 1, 1, 0},  // bottom center — energy
        {1, 1, 0, 1, 0},  // north center — energy
        {0, 1, 1, 0, 1},  // west center — item OUTPUT
        {2, 1, 1, 0, 1},  // east center — item OUTPUT
        {1, 2, 1, 0, 0},  // top center — item INPUT
    };

    // Controller position in structure space
    private static final int CTRL_SX = 1, CTRL_SY = 1, CTRL_SZ = 2;

    // Rotation matrices for 4 facing directions
    private static final int[][] FACINGS = {
        { 1, 0, 0, 1},
        { 0, 1,-1, 0},
        {-1, 0, 0,-1},
        { 0,-1, 1, 0},
    };

    private int formedRotation = -1;
    private boolean wasFormed = false;
    private boolean checkedAfterLoad = false;

    public boolean checkStructure(World world, int cx, int cy, int cz) {
        int portBlockId = mod_IC2HeavyMachinery.heavyPort != null ? mod_IC2HeavyMachinery.heavyPort.blockID : -1;

        for (int f = 0; f < 4; f++) {
            boolean ok = true;
            for (int sy = 0; sy < STRUCTURE.length && ok; sy++) {
                for (int sz = 0; sz < STRUCTURE[sy].length && ok; sz++) {
                    for (int sx = 0; sx < STRUCTURE[sy][sz].length && ok; sx++) {
                        int expected = STRUCTURE[sy][sz][sx];
                        if (expected == TYPE_AIR) continue;
                        if (expected == TYPE_CONTROLLER) continue;

                        int relX = sx - CTRL_SX;
                        int relZ = sz - CTRL_SZ;
                        int wx = cx + relX * FACINGS[f][0] + relZ * FACINGS[f][2];
                        int wy = cy + (sy - CTRL_SY);
                        int wz = cz + relX * FACINGS[f][1] + relZ * FACINGS[f][3];
                        int blockId = world.getBlockId(wx, wy, wz);
                        int meta = world.getBlockMetadata(wx, wy, wz);

                        boolean match = false;
                        if (expected == TYPE_ADVANCED_MACHINE_BLOCK) {
                            match = (blockId == IC2HM_IC2Ref.blockMachine.blockID && meta == 12);
                        } else if (expected == TYPE_MACHINE_BLOCK) {
                            // Accept IC2 Machine Block (meta 0) OR already-placed port block
                            match = (blockId == IC2HM_IC2Ref.blockMachine.blockID && meta == 0)
                                 || (portBlockId != -1 && blockId == portBlockId);
                        }

                        if (!match) ok = false;
                    }
                }
            }
            if (ok) {
                boolean wasFormedBefore = wasFormed;
                isFormed = true;
                formedRotation = f;
                if (!wasFormedBefore) {
                    applyPortBlocks(world, cx, cy, cz, f);
                    wasFormed = true;
                }
                return true;
            }
        }

        // Structure invalid — restore ports if they were placed
        if (wasFormed && formedRotation >= 0) {
            restorePortBlocks(world, cx, cy, cz, formedRotation);
            wasFormed = false;
        }
        isFormed = false;
        formedRotation = -1;
        return false;
    }

    /** Convert structure-space port position to world coordinates */
    private int[] structToWorld(int sx, int sy, int sz, int cx, int cy, int cz, int rotation) {
        int relX = sx - CTRL_SX;
        int relZ = sz - CTRL_SZ;
        return new int[] {
            cx + relX * FACINGS[rotation][0] + relZ * FACINGS[rotation][2],
            cy + (sy - CTRL_SY),
            cz + relX * FACINGS[rotation][1] + relZ * FACINGS[rotation][3]
        };
    }

    /** Replace machine blocks at port positions with IC2HM_BlockHeavyPort + correct TE type */
    private void applyPortBlocks(World world, int cx, int cy, int cz, int rotation) {
        int portId = mod_IC2HeavyMachinery.heavyPort.blockID;
        for (int p = 0; p < PORTS.length; p++) {
            boolean isEnergy = PORTS[p][3] == 1;
            boolean isOutput = PORTS[p][4] == 1;
            int portMeta = isEnergy ? 0 : 1;
            int[] w = structToWorld(PORTS[p][0], PORTS[p][1], PORTS[p][2], cx, cy, cz, rotation);

            // Place port block with metadata + notify (creates default TE via getBlockEntity)
            world.setBlockAndMetadataWithNotify(w[0], w[1], w[2], portId, portMeta);

            // For energy ports, replace the default TE with an IEnergySink TE
            // For item ports, configure controller coords + I/O mode
            TileEntity te = world.getBlockTileEntity(w[0], w[1], w[2]);
            if (isEnergy) {
                // Remove the default item port TE and replace with energy port
                if (te != null) {
                    world.removeBlockTileEntity(w[0], w[1], w[2]);
                }
                IC2HM_TileEnergyPort ep = new IC2HM_TileEnergyPort();
                ep.setController(cx, cy, cz);
                world.setBlockTileEntity(w[0], w[1], w[2], ep);
            } else if (te instanceof IC2HM_TileHeavyPort) {
                ((IC2HM_TileHeavyPort) te).setController(cx, cy, cz);
                ((IC2HM_TileHeavyPort) te).setOutputMode(isOutput);
            }
        }
    }

    /** Restore port blocks back to IC2 Machine Block (meta 0) on deformation */
    private void restorePortBlocks(World world, int cx, int cy, int cz, int rotation) {
        int portBlockId = mod_IC2HeavyMachinery.heavyPort != null ? mod_IC2HeavyMachinery.heavyPort.blockID : -1;
        for (int p = 0; p < PORTS.length; p++) {
            int[] w = structToWorld(PORTS[p][0], PORTS[p][1], PORTS[p][2], cx, cy, cz, rotation);
            if (world.getBlockId(w[0], w[1], w[2]) == portBlockId) {
                // Clean up TE before replacing
                TileEntity te = world.getBlockTileEntity(w[0], w[1], w[2]);
                if (te instanceof IC2HM_TileEnergyPort) {
                    ((IC2HM_TileEnergyPort) te).onRemoved();
                } else if (te instanceof IC2HM_TileHeavyPort) {
                    ((IC2HM_TileHeavyPort) te).onRemoved();
                }
                world.setBlockWithNotify(w[0], w[1], w[2], IC2HM_IC2Ref.blockMachine.blockID);
                world.setBlockMetadata(w[0], w[1], w[2], 0);
            }
        }
    }

    @Override
    public void updateEntity() {
        if (worldObj.multiplayerWorld) return;

        // Register with IC2 EnergyNet
        if (!addedToEnergyNet) {
            EnergyNet.getForWorld(worldObj).addTileEntity(this);
            addedToEnergyNet = true;
        }

        // Schedule a block tick to check structure (runs OUTSIDE the TE tick loop)
        if (!checkedAfterLoad) {
            checkedAfterLoad = true;
            worldObj.scheduleBlockUpdate(xCoord, yCoord, zCoord, worldObj.getBlockId(xCoord, yCoord, zCoord), 1);
        }

        if (!isFormed) {
            for (int i = 0; i < 4; i++) processTime[i] = 0;
            return;
        }

        // Process each lane independently
        IC2HM_RecipesHeavyCrusher recipes = IC2HM_RecipesHeavyCrusher.getInstance();
        for (int lane = 0; lane < 4; lane++) {
            IC2HM_RecipesHeavyCrusher.CrushRecipe recipe = (inventory[lane] != null) ? recipes.getRecipe(inventory[lane]) : null;
            if (recipe == null || !canFitInLaneOutput(lane, recipe.mainOutput)) {
                processTime[lane] = 0;
                continue;
            }
            if (energy >= ENERGY_PER_TICK) {
                energy -= ENERGY_PER_TICK;
                processTime[lane]++;
                if (processTime[lane] >= maxProcessTime) {
                    processTime[lane] = 0;
                    processLane(lane, recipe, recipes);
                }
            }
        }
    }

    private void processLane(int lane, IC2HM_RecipesHeavyCrusher.CrushRecipe recipe, IC2HM_RecipesHeavyCrusher recipes) {
        int outSlot = lane + 4;      // main output: 4,5,6,7
        int bySlot = lane + 8;       // byproduct: 8,9,10,11

        // Consume 1 input
        inventory[lane].stackSize--;
        if (inventory[lane].stackSize <= 0) inventory[lane] = null;

        // Place main output
        addToSlot(outSlot, recipe.mainOutput.copy());

        // Roll byproduct
        ItemStack byproduct = recipes.rollByproduct(recipe);
        if (byproduct != null) {
            addToSlot(bySlot, byproduct);
        }
    }

    private boolean canFitInLaneOutput(int lane, ItemStack mainOut) {
        int outSlot = lane + 4;
        if (inventory[outSlot] == null) return true;
        return inventory[outSlot].itemID == mainOut.itemID
            && inventory[outSlot].getItemDamage() == mainOut.getItemDamage()
            && inventory[outSlot].stackSize + mainOut.stackSize <= inventory[outSlot].getMaxStackSize();
    }

    private void addToSlot(int slot, ItemStack stack) {
        if (inventory[slot] == null) {
            inventory[slot] = stack.copy();
        } else if (inventory[slot].itemID == stack.itemID && inventory[slot].getItemDamage() == stack.getItemDamage()) {
            inventory[slot].stackSize += stack.stackSize;
            if (inventory[slot].stackSize > inventory[slot].getMaxStackSize()) {
                inventory[slot].stackSize = inventory[slot].getMaxStackSize();
            }
        }
    }

    // === Aero_ISlotAccess ===
    @Override
    public int[] getInsertSlots() { return new int[]{0, 1, 2, 3}; }
    @Override
    public int[] getExtractSlots() { return new int[]{4, 5, 6, 7, 8, 9, 10, 11}; }

    // === IC2 IEnergySink ===
    @Override
    public boolean isAddedToEnergyNet() { return addedToEnergyNet; }

    @Override
    public void setAddedToEnergyNet(boolean added) { this.addedToEnergyNet = added; }

    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, Direction direction) {
        return true; // Accept from all sides
    }

    @Override
    public boolean demandsEnergy() { return energy < maxEnergy; }

    @Override
    public int injectEnergy(Direction direction, int amount) {
        if (amount > MAX_INPUT) {
            // Overvoltage — explode
            worldObj.setBlockWithNotify(xCoord, yCoord, zCoord, 0);
            worldObj.createExplosion(null, xCoord, yCoord, zCoord, 3.0F);
            return 0;
        }
        int accepted = Math.min(amount, maxEnergy - energy);
        energy += accepted;
        return amount - accepted;
    }

    // === IInventory ===
    @Override
    public int getSizeInventory() { return inventory.length; }

    @Override
    public ItemStack getStackInSlot(int slot) { return inventory[slot]; }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (inventory[slot] != null) {
            if (inventory[slot].stackSize <= amount) {
                ItemStack stack = inventory[slot];
                inventory[slot] = null;
                return stack;
            }
            ItemStack split = inventory[slot].splitStack(amount);
            if (inventory[slot].stackSize == 0) inventory[slot] = null;
            return split;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inventory[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
    }

    @Override
    public String getInvName() { return "Heavy Crusher"; }

    @Override
    public int getInventoryStackLimit() { return 64; }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this
            && player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0;
    }

    // === Scaled values for GUI ===
    public int getEnergyScaled(int scale) {
        return maxEnergy > 0 ? energy * scale / maxEnergy : 0;
    }

    public int getCookProgressScaled(int lane, int scale) {
        return maxProcessTime > 0 ? processTime[lane] * scale / maxProcessTime : 0;
    }

    public int getStoredEnergy() { return energy; }
    public int getMaxEnergy() { return maxEnergy; }

    // === NBT ===
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        isFormed = nbt.getBoolean("Formed");
        formedRotation = nbt.getInteger("FormedRotation");
        wasFormed = nbt.getBoolean("WasFormed");
        energy = nbt.getInteger("Energy");
        for (int i = 0; i < 4; i++) processTime[i] = nbt.getShort("PT" + i);

        NBTTagList items = nbt.getTagList("Items");
        inventory = new ItemStack[12];
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound slot = (NBTTagCompound) items.tagAt(i);
            int idx = slot.getByte("Slot") & 255;
            if (idx < inventory.length) {
                inventory[idx] = new ItemStack(slot);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("Formed", isFormed);
        nbt.setInteger("FormedRotation", formedRotation);
        nbt.setBoolean("WasFormed", wasFormed);
        nbt.setInteger("Energy", energy);
        for (int i = 0; i < 4; i++) nbt.setShort("PT" + i, (short) processTime[i]);

        NBTTagList items = new NBTTagList();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null) {
                NBTTagCompound slot = new NBTTagCompound();
                slot.setByte("Slot", (byte) i);
                inventory[i].writeToNBT(slot);
                items.setTag(slot);
            }
        }
        nbt.setTag("Items", items);
    }

    /** Called from BlockHeavyCrusher.onBlockRemoval to clean up EnergyNet and restore ports */
    public void onRemoved() {
        if (wasFormed && formedRotation >= 0) {
            restorePortBlocks(worldObj, xCoord, yCoord, zCoord, formedRotation);
            wasFormed = false;
        }
        if (addedToEnergyNet) {
            EnergyNet.getForWorld(worldObj).removeTileEntity(this);
            addedToEnergyNet = false;
        }
    }
}
