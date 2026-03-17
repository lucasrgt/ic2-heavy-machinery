package ic2heavymachinery.tile;

import net.minecraft.src.*;
import ic2.*;
import aero.machineapi.*;
import ic2heavymachinery.recipe.*;

public class IC2HM_TileAlloyBlender extends TileEntity
        implements IInventory, IEnergySink, Aero_IEnergyReceiver, Aero_ISideConfigurable, Aero_ISlotAccess {

    private ItemStack[] inventory = new ItemStack[4]; // 0=input1, 1=input2, 2=output, 3=battery
    public int energy = 0;
    public int maxEnergy = 16000;
    public int cookTime = 0;
    public int maxCookTime = 200;
    public static final int ENERGY_PER_TICK = 8;
    public static final int MAX_INPUT = 128; // MV

    public boolean active = false;
    public int facing = 2; // north by default
    private int animTick = 0;

    private int[] sideConfig = new int[24];
    private boolean addedToEnergyNet = false;

    {
        for (int s = 0; s < 6; s++) {
            Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.MODE_INPUT);
            if (s == Aero_SideConfig.SIDE_BOTTOM) {
                Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_ITEM, Aero_SideConfig.MODE_OUTPUT);
            } else {
                Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_ITEM, Aero_SideConfig.MODE_INPUT);
            }
        }
    }

    // === Aero_IEnergyReceiver ===
    @Override
    public int receiveEnergy(int amount) {
        int accepted = Math.min(amount, maxEnergy - energy);
        energy += accepted;
        return accepted;
    }

    @Override
    public int getStoredEnergy() { return energy; }

    @Override
    public int getMaxEnergy() { return maxEnergy; }

    // === Aero_ISideConfigurable ===
    @Override
    public int[] getSideConfig() { return sideConfig; }

    @Override
    public void setSideMode(int side, int type, int mode) {
        Aero_SideConfig.set(sideConfig, side, type, mode);
    }

    @Override
    public boolean supportsType(int type) {
        return type == Aero_SideConfig.TYPE_ENERGY || type == Aero_SideConfig.TYPE_ITEM;
    }

    @Override
    public int[] getAllowedModes(int type) {
        return new int[] { Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_INPUT,
                Aero_SideConfig.MODE_OUTPUT, Aero_SideConfig.MODE_INPUT_OUTPUT };
    }

    // === Aero_ISlotAccess ===
    @Override
    public int[] getInsertSlots() { return new int[] { 0, 1 }; }

    @Override
    public int[] getExtractSlots() { return new int[] { 2 }; }

    // === IC2 Energy Sink ===
    @Override
    public boolean isAddedToEnergyNet() { return addedToEnergyNet; }

    @Override
    public void setAddedToEnergyNet(boolean added) { this.addedToEnergyNet = added; }

    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, Direction direction) {
        return Aero_SideConfig.canInput(
                Aero_SideConfig.get(sideConfig, direction.ordinal(), Aero_SideConfig.TYPE_ENERGY));
    }

    @Override
    public boolean demandsEnergy() { return energy < maxEnergy; }

    @Override
    public int injectEnergy(Direction direction, int amount) {
        if (amount > MAX_INPUT) {
            // Overvoltage — explode
            worldObj.setBlockWithNotify(xCoord, yCoord, zCoord, 0);
            worldObj.createExplosion(null, xCoord, yCoord, zCoord, 2.0F);
            return 0;
        }
        int accepted = receiveEnergy(amount);
        return amount - accepted;
    }

    // === IInventory ===
    @Override public int getSizeInventory() { return inventory.length; }
    @Override public ItemStack getStackInSlot(int i) { return inventory[i]; }

    @Override
    public ItemStack decrStackSize(int i, int j) {
        if (inventory[i] != null) {
            if (inventory[i].stackSize <= j) {
                ItemStack stack = inventory[i];
                inventory[i] = null;
                return stack;
            }
            ItemStack stack = inventory[i].splitStack(j);
            if (inventory[i].stackSize == 0) inventory[i] = null;
            return stack;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack stack) { inventory[i] = stack; }

    @Override public String getInvName() { return "Alloy Blender"; }
    @Override public int getInventoryStackLimit() { return 64; }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this;
    }

    // === Processing ===
    @Override
    public void updateEntity() {
        if (worldObj.multiplayerWorld) return;

        if (!addedToEnergyNet) {
            EnergyNet.getForWorld(worldObj).addTileEntity(this);
            addedToEnergyNet = true;
        }

        boolean wasActive = active;

        if (canBlend()) {
            if (energy >= ENERGY_PER_TICK) {
                energy -= ENERGY_PER_TICK;
                cookTime++;
                active = true;
                animTick++;
                if (cookTime >= maxCookTime) {
                    blendItem();
                    cookTime = 0;
                }
            } else {
                active = false;
            }
        } else {
            cookTime = 0;
            active = false;
        }

        if (wasActive != active) {
            worldObj.markBlockNeedsUpdate(xCoord, yCoord, zCoord);
        }
    }

    private boolean canBlend() {
        IC2HM_RecipesAlloyBlender.BlendRecipe recipe =
                IC2HM_RecipesAlloyBlender.getInstance().getRecipe(inventory[0], inventory[1]);
        if (recipe == null) return false;
        if (inventory[2] == null) return true;
        if (!inventory[2].isItemEqual(recipe.output)) return false;
        int resultSize = inventory[2].stackSize + recipe.output.stackSize;
        return resultSize <= getInventoryStackLimit() && resultSize <= inventory[2].getMaxStackSize();
    }

    private void blendItem() {
        IC2HM_RecipesAlloyBlender.BlendRecipe recipe =
                IC2HM_RecipesAlloyBlender.getInstance().getRecipe(inventory[0], inventory[1]);
        if (recipe == null) return;

        if (inventory[2] == null) {
            inventory[2] = recipe.output.copy();
        } else {
            inventory[2].stackSize += recipe.output.stackSize;
        }

        inventory[0].stackSize -= recipe.input1Count;
        if (inventory[0].stackSize <= 0) inventory[0] = null;

        inventory[1].stackSize -= recipe.input2Count;
        if (inventory[1].stackSize <= 0) inventory[1] = null;
    }

    public int getCookProgressScaled(int scale) {
        return maxCookTime > 0 ? (cookTime * scale) / maxCookTime : 0;
    }

    public int getEnergyScaled(int scale) {
        return maxEnergy > 0 ? (energy * scale) / maxEnergy : 0;
    }

    /** Returns the current animation frame (0-3) for the front_active texture */
    public int getAnimFrame() {
        return (animTick / 4) % 4;
    }

    // === NBT ===
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        NBTTagList list = nbt.getTagList("Items");
        inventory = new ItemStack[getSizeInventory()];
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = (NBTTagCompound) list.tagAt(i);
            int slot = tag.getByte("Slot") & 0xFF;
            if (slot < inventory.length) inventory[slot] = new ItemStack(tag);
        }
        energy = nbt.getInteger("Energy");
        cookTime = nbt.getInteger("CookTime");
        facing = nbt.getInteger("Facing");
        if (facing == 0) facing = 2; // default north if not set
        if (nbt.hasKey("SideConfig0")) {
            for (int i = 0; i < 24; i++) sideConfig[i] = nbt.getInteger("SideConfig" + i);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("Energy", energy);
        nbt.setInteger("CookTime", cookTime);
        nbt.setInteger("Facing", facing);
        for (int i = 0; i < 24; i++) nbt.setInteger("SideConfig" + i, sideConfig[i]);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setByte("Slot", (byte) i);
                inventory[i].writeToNBT(tag);
                list.setTag(tag);
            }
        }
        nbt.setTag("Items", list);
    }
}
