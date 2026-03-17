package ic2heavymachinery.tile;

import net.minecraft.src.*;
import aero.machineapi.Aero_ISlotAccess;

/**
 * Item port tile for Heavy Crusher multiblock.
 * Implements IInventory + Aero_ISlotAccess — forwards item access to the controller.
 * Does NOT implement IEnergySink, so IC2 cables won't connect.
 */
public class IC2HM_TileHeavyPort extends TileEntity implements IInventory, Aero_ISlotAccess {

    protected int controllerX, controllerY, controllerZ;
    protected boolean hasController = false;
    private boolean isOutput = false; // false=input port, true=output port

    public void setController(int cx, int cy, int cz) {
        this.controllerX = cx;
        this.controllerY = cy;
        this.controllerZ = cz;
        this.hasController = true;
    }

    public void setOutputMode(boolean output) {
        this.isOutput = output;
    }

    public boolean isOutputMode() {
        return isOutput;
    }

    public void toggleMode() {
        isOutput = !isOutput;
    }

    protected IC2HM_TileHeavyCrusher getController() {
        if (!hasController || worldObj == null) return null;
        TileEntity te = worldObj.getBlockTileEntity(controllerX, controllerY, controllerZ);
        if (te instanceof IC2HM_TileHeavyCrusher) return (IC2HM_TileHeavyCrusher) te;
        return null;
    }

    public void onRemoved() {
        // Item ports have nothing special to clean up
    }

    // === Aero_ISlotAccess (forward to controller, respecting I/O mode) ===
    @Override
    public int[] getInsertSlots() {
        if (isOutput) return new int[0]; // output ports don't accept items
        IC2HM_TileHeavyCrusher ctrl = getController();
        return ctrl != null ? ctrl.getInsertSlots() : new int[0];
    }
    @Override
    public int[] getExtractSlots() {
        if (!isOutput) return new int[0]; // input ports don't allow extraction
        IC2HM_TileHeavyCrusher ctrl = getController();
        return ctrl != null ? ctrl.getExtractSlots() : new int[0];
    }

    // === IInventory (forward to controller) ===
    @Override
    public int getSizeInventory() {
        IC2HM_TileHeavyCrusher ctrl = getController();
        return ctrl != null ? ctrl.getSizeInventory() : 0;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        IC2HM_TileHeavyCrusher ctrl = getController();
        return ctrl != null ? ctrl.getStackInSlot(slot) : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        IC2HM_TileHeavyCrusher ctrl = getController();
        return ctrl != null ? ctrl.decrStackSize(slot, amount) : null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        IC2HM_TileHeavyCrusher ctrl = getController();
        if (ctrl != null) ctrl.setInventorySlotContents(slot, stack);
    }

    @Override
    public String getInvName() { return "Heavy Crusher Item Port"; }

    @Override
    public int getInventoryStackLimit() {
        IC2HM_TileHeavyCrusher ctrl = getController();
        return ctrl != null ? ctrl.getInventoryStackLimit() : 64;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) { return false; }

    // === NBT ===
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        hasController = nbt.getBoolean("HasCtrl");
        controllerX = nbt.getInteger("CtrlX");
        controllerY = nbt.getInteger("CtrlY");
        controllerZ = nbt.getInteger("CtrlZ");
        isOutput = nbt.getBoolean("IsOutput");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("HasCtrl", hasController);
        nbt.setInteger("CtrlX", controllerX);
        nbt.setInteger("CtrlY", controllerY);
        nbt.setInteger("CtrlZ", controllerZ);
        nbt.setBoolean("IsOutput", isOutput);
    }
}
