package ic2heavymachinery.tile;

import net.minecraft.src.*;
import aero.machineapi.*;

public class IC2HM_TileItemCable extends TileEntity implements IInventory, Aero_ISideConfigurable {
    private ItemStack buffer = null;
    private int transferRate; // items per transfer: 1, 8, or 64
    private int tier; // 0=basic, 1=reinforced, 2=advanced
    private static final int TRANSFER_COOLDOWN = 8;
    private int cooldown = 0;
    private int lastReceivedFrom = -1;
    private int[] sideConfig = new int[24];

    private static final int[][] DIRS = {
        {0,-1,0}, {0,1,0}, {0,0,-1}, {0,0,1}, {-1,0,0}, {1,0,0}
    };

    public IC2HM_TileItemCable() {
        this(0);
    }

    public IC2HM_TileItemCable(int tier) {
        this.tier = tier;
        switch (tier) {
            case 0: this.transferRate = 1; break;
            case 1: this.transferRate = 8; break;
            case 2: this.transferRate = 64; break;
            default: this.transferRate = 1;
        }
        for (int s = 0; s < 6; s++) {
            Aero_SideConfig.set(sideConfig, s, Aero_SideConfig.TYPE_ITEM, Aero_SideConfig.MODE_INPUT_OUTPUT);
        }
    }

    public int getTier() { return tier; }
    public int getTransferRate() { return transferRate; }

    // --- Aero_ISideConfigurable ---
    public int[] getSideConfig() { return sideConfig; }
    public void setSideMode(int side, int type, int mode) {
        if (!supportsType(type)) return;
        int[] allowed = getAllowedModes(type);
        for (int m : allowed) { if (m == mode) { Aero_SideConfig.set(sideConfig, side, type, mode); return; } }
    }
    public boolean supportsType(int type) { return type == Aero_SideConfig.TYPE_ITEM; }
    public int[] getAllowedModes(int type) {
        if (type == Aero_SideConfig.TYPE_ITEM) return new int[]{Aero_SideConfig.MODE_NONE, Aero_SideConfig.MODE_INPUT, Aero_SideConfig.MODE_OUTPUT, Aero_SideConfig.MODE_INPUT_OUTPUT};
        return new int[]{Aero_SideConfig.MODE_NONE};
    }

    public int getSideMode(int side) {
        return Aero_SideConfig.get(sideConfig, side, Aero_SideConfig.TYPE_ITEM);
    }

    private boolean canSendTo(int side, TileEntity te) {
        if (!Aero_SideConfig.canOutput(getSideMode(side))) return false;
        int oppSide = Aero_SideConfig.oppositeSide(side);
        if (te instanceof Aero_ISideConfigurable) {
            int neighborMode = Aero_SideConfig.get(((Aero_ISideConfigurable) te).getSideConfig(), oppSide, Aero_SideConfig.TYPE_ITEM);
            if (!Aero_SideConfig.canInput(neighborMode)) return false;
        }
        // If target declares slot access, it must have insert slots
        if (te instanceof Aero_ISlotAccess) {
            int[] insertSlots = ((Aero_ISlotAccess) te).getInsertSlots();
            if (insertSlots != null && insertSlots.length == 0) return false;
        }
        return true;
    }

    private boolean canReceiveFrom(int side) {
        return Aero_SideConfig.canInput(getSideMode(side));
    }

    private int[] getExtractSlotsFor(TileEntity te) {
        if (te instanceof Aero_ISlotAccess) return ((Aero_ISlotAccess) te).getExtractSlots();
        if (te instanceof TileEntityFurnace) return new int[]{2};
        return null;
    }

    private int[] getInsertSlotsFor(TileEntity te) {
        if (te instanceof Aero_ISlotAccess) return ((Aero_ISlotAccess) te).getInsertSlots();
        if (te instanceof TileEntityFurnace) return new int[]{0};
        return null;
    }

    private boolean isItemCable(TileEntity te) {
        return te instanceof IC2HM_TileItemCable;
    }

    private boolean hasViableOutput(int excludeX, int excludeY, int excludeZ) {
        for (int side = 0; side < 6; side++) {
            if (!Aero_SideConfig.canOutput(getSideMode(side))) continue;
            int[] d = DIRS[side];
            int nx = xCoord + d[0], ny = yCoord + d[1], nz = zCoord + d[2];
            if (nx == excludeX && ny == excludeY && nz == excludeZ) continue;
            TileEntity te = Aero_PortRegistry.resolveHandler(worldObj, nx, ny, nz);
            if (te == null) continue;
            if (isItemCable(te)) return true;
            if (te instanceof IInventory && canSendTo(side, te)) return true;
        }
        return false;
    }

    public boolean receiveItem(ItemStack stack, int fromSide) {
        if (buffer != null) return false;
        this.buffer = stack;
        this.cooldown = TRANSFER_COOLDOWN;
        this.lastReceivedFrom = fromSide;
        return true;
    }

    public void updateEntity() {
        if (this.worldObj.multiplayerWorld) return;

        // Pull items from neighboring inventories on input sides
        if (buffer == null) {
            lastReceivedFrom = -1;
            for (int side = 0; side < 6; side++) {
                if (!canReceiveFrom(side)) continue;
                int[] d = DIRS[side];
                TileEntity te = Aero_PortRegistry.resolveHandler(worldObj, xCoord + d[0], yCoord + d[1], zCoord + d[2]);
                if (te == null || isItemCable(te)) continue;
                if (!(te instanceof IInventory)) continue;
                int oppSide = Aero_SideConfig.oppositeSide(side);
                if (te instanceof Aero_ISideConfigurable) {
                    int neighborMode = Aero_SideConfig.get(((Aero_ISideConfigurable) te).getSideConfig(), oppSide, Aero_SideConfig.TYPE_ITEM);
                    if (!Aero_SideConfig.canOutput(neighborMode)) continue;
                }
                // If target declares slot access, it must have extract slots
                if (te instanceof Aero_ISlotAccess) {
                    int[] eSlots = ((Aero_ISlotAccess) te).getExtractSlots();
                    if (eSlots == null || eSlots.length == 0) continue;
                }
                if (!hasViableOutput(xCoord + d[0], yCoord + d[1], zCoord + d[2])) continue;
                IInventory inv = (IInventory) te;
                int[] extractSlots = getExtractSlotsFor(te);
                if (extractSlots != null) {
                    for (int slot : extractSlots) {
                        ItemStack stack = inv.getStackInSlot(slot);
                        if (stack != null) {
                            int amount = Math.min(transferRate, stack.stackSize);
                            buffer = inv.decrStackSize(slot, amount);
                            cooldown = TRANSFER_COOLDOWN;
                            lastReceivedFrom = side;
                            return;
                        }
                    }
                } else {
                    for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
                        ItemStack stack = inv.getStackInSlot(slot);
                        if (stack != null) {
                            int amount = Math.min(transferRate, stack.stackSize);
                            buffer = inv.decrStackSize(slot, amount);
                            cooldown = TRANSFER_COOLDOWN;
                            lastReceivedFrom = side;
                            return;
                        }
                    }
                }
            }
            return;
        }

        // Cooldown before pushing
        if (cooldown > 0) { cooldown--; return; }

        // Push buffer to neighbors
        for (int side = 0; side < 6; side++) {
            if (buffer == null) break;
            if (side == lastReceivedFrom) continue;
            if (!Aero_SideConfig.canOutput(getSideMode(side))) continue;
            int[] d = DIRS[side];
            TileEntity te = Aero_PortRegistry.resolveHandler(worldObj, xCoord + d[0], yCoord + d[1], zCoord + d[2]);
            if (te == null || !canSendTo(side, te)) continue;

            if (isItemCable(te)) {
                IC2HM_TileItemCable other = (IC2HM_TileItemCable) te;
                int oppSide = Aero_SideConfig.oppositeSide(side);
                // Respect both this cable's and target cable's transfer rate
                int maxTransfer = Math.min(transferRate, other.getTransferRate());
                if (buffer.stackSize <= maxTransfer) {
                    if (other.receiveItem(buffer, oppSide)) {
                        buffer = null;
                        lastReceivedFrom = -1;
                    }
                } else {
                    // Split stack: send what we can
                    ItemStack toSend = new ItemStack(buffer.itemID, maxTransfer, buffer.getItemDamage());
                    if (other.receiveItem(toSend, oppSide)) {
                        buffer.stackSize -= maxTransfer;
                        if (buffer.stackSize <= 0) {
                            buffer = null;
                            lastReceivedFrom = -1;
                        }
                    }
                }
            } else if (te instanceof IInventory) {
                IInventory inv = (IInventory) te;
                int[] insertSlots = getInsertSlotsFor(te);
                buffer = addToInventory(inv, buffer, insertSlots);
                if (buffer == null) lastReceivedFrom = -1;
            }
        }
    }

    private ItemStack addToInventory(IInventory inv, ItemStack stack, int[] slots) {
        // Limit by transfer rate
        int maxToInsert = Math.min(transferRate, stack.stackSize);
        ItemStack toInsert;
        ItemStack remainder = null;
        if (maxToInsert < stack.stackSize) {
            toInsert = new ItemStack(stack.itemID, maxToInsert, stack.getItemDamage());
            remainder = new ItemStack(stack.itemID, stack.stackSize - maxToInsert, stack.getItemDamage());
        } else {
            toInsert = stack;
        }

        ItemStack leftover = insertIntoSlots(inv, toInsert, slots);
        if (leftover != null && remainder != null) {
            remainder.stackSize += leftover.stackSize;
            return remainder;
        }
        if (leftover != null) return leftover;
        return remainder;
    }

    private ItemStack insertIntoSlots(IInventory inv, ItemStack stack, int[] slots) {
        if (slots != null) {
            for (int i : slots) {
                ItemStack existing = inv.getStackInSlot(i);
                if (existing != null && existing.itemID == stack.itemID && existing.getItemDamage() == stack.getItemDamage()) {
                    int space = existing.getMaxStackSize() - existing.stackSize;
                    if (space > 0) {
                        int toAdd = Math.min(stack.stackSize, space);
                        existing.stackSize += toAdd;
                        stack.stackSize -= toAdd;
                        if (stack.stackSize <= 0) return null;
                    }
                }
            }
            for (int i : slots) {
                if (inv.getStackInSlot(i) == null) {
                    inv.setInventorySlotContents(i, stack);
                    return null;
                }
            }
        } else {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack existing = inv.getStackInSlot(i);
                if (existing != null && existing.itemID == stack.itemID && existing.getItemDamage() == stack.getItemDamage()) {
                    int space = existing.getMaxStackSize() - existing.stackSize;
                    if (space > 0) {
                        int toAdd = Math.min(stack.stackSize, space);
                        existing.stackSize += toAdd;
                        stack.stackSize -= toAdd;
                        if (stack.stackSize <= 0) return null;
                    }
                }
            }
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (inv.getStackInSlot(i) == null) {
                    inv.setInventorySlotContents(i, stack);
                    return null;
                }
            }
        }
        return stack;
    }

    // === IInventory (single slot buffer) ===
    public int getSizeInventory() { return 1; }
    public ItemStack getStackInSlot(int slot) { return slot == 0 ? buffer : null; }
    public ItemStack decrStackSize(int slot, int amount) {
        if (slot != 0 || buffer == null) return null;
        if (amount >= buffer.stackSize) {
            ItemStack result = buffer;
            buffer = null;
            return result;
        }
        buffer.stackSize -= amount;
        return new ItemStack(buffer.itemID, amount, buffer.getItemDamage());
    }
    public ItemStack getStackInSlotOnClosing(int slot) { return null; }
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot == 0) buffer = stack;
    }
    public String getInvName() {
        switch (tier) {
            case 1: return "Reinforced Item Cable";
            case 2: return "Advanced Item Cable";
            default: return "Item Cable";
        }
    }
    public int getInventoryStackLimit() { return 64; }
    public boolean isUseableByPlayer(EntityPlayer player) { return false; }
    public boolean canInteractWith(EntityPlayer player) { return false; }
    public void openChest() {}
    public void closeChest() {}

    // === NBT ===
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        tier = nbt.getInteger("Tier");
        switch (tier) {
            case 0: transferRate = 1; break;
            case 1: transferRate = 8; break;
            case 2: transferRate = 64; break;
            default: transferRate = 1;
        }
        if (nbt.hasKey("Buffer")) {
            NBTTagCompound bufTag = nbt.getCompoundTag("Buffer");
            buffer = new ItemStack(bufTag.getShort("id"), bufTag.getByte("Count"), bufTag.getShort("Damage"));
        }
        cooldown = nbt.getInteger("Cooldown");
        lastReceivedFrom = nbt.getInteger("LastFrom");
        if (lastReceivedFrom < -1 || lastReceivedFrom > 5) lastReceivedFrom = -1;
        if (nbt.hasKey("SC0")) {
            for (int i = 0; i < 24; i++) this.sideConfig[i] = nbt.getInteger("SC" + i);
        }
    }

    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("Tier", tier);
        if (buffer != null) {
            NBTTagCompound bufTag = new NBTTagCompound();
            buffer.writeToNBT(bufTag);
            nbt.setCompoundTag("Buffer", bufTag);
        }
        nbt.setInteger("Cooldown", cooldown);
        nbt.setInteger("LastFrom", lastReceivedFrom);
        for (int i = 0; i < 24; i++) nbt.setInteger("SC" + i, this.sideConfig[i]);
    }
}
