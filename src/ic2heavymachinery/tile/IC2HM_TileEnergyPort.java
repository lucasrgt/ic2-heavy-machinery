package ic2heavymachinery.tile;

import net.minecraft.src.*;
import ic2.*;

/**
 * Energy port tile for Heavy Crusher multiblock.
 * Implements IEnergySink — IC2 cables will connect and deliver EU.
 * Forwards all energy to the controller tile entity.
 */
public class IC2HM_TileEnergyPort extends TileEntity implements IEnergySink {

    private int controllerX, controllerY, controllerZ;
    private boolean hasController = false;
    private boolean addedToEnergyNet = false;

    public void setController(int cx, int cy, int cz) {
        this.controllerX = cx;
        this.controllerY = cy;
        this.controllerZ = cz;
        this.hasController = true;
    }

    private IC2HM_TileHeavyCrusher getController() {
        if (!hasController || worldObj == null) return null;
        TileEntity te = worldObj.getBlockTileEntity(controllerX, controllerY, controllerZ);
        if (te instanceof IC2HM_TileHeavyCrusher) return (IC2HM_TileHeavyCrusher) te;
        return null;
    }

    @Override
    public void updateEntity() {
        if (worldObj.multiplayerWorld) return;
        if (!addedToEnergyNet) {
            EnergyNet.getForWorld(worldObj).addTileEntity(this);
            addedToEnergyNet = true;
        }
    }

    public void onRemoved() {
        if (addedToEnergyNet && worldObj != null) {
            EnergyNet.getForWorld(worldObj).removeTileEntity(this);
            addedToEnergyNet = false;
        }
    }

    // === IEnergySink ===
    @Override
    public boolean isAddedToEnergyNet() { return addedToEnergyNet; }

    @Override
    public void setAddedToEnergyNet(boolean added) { this.addedToEnergyNet = added; }

    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, Direction direction) {
        return hasController;
    }

    @Override
    public boolean demandsEnergy() {
        IC2HM_TileHeavyCrusher ctrl = getController();
        return ctrl != null && ctrl.demandsEnergy();
    }

    @Override
    public int injectEnergy(Direction direction, int amount) {
        IC2HM_TileHeavyCrusher ctrl = getController();
        if (ctrl == null) return amount;
        return ctrl.injectEnergy(direction, amount);
    }

    // === NBT ===
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        hasController = nbt.getBoolean("HasCtrl");
        controllerX = nbt.getInteger("CtrlX");
        controllerY = nbt.getInteger("CtrlY");
        controllerZ = nbt.getInteger("CtrlZ");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("HasCtrl", hasController);
        nbt.setInteger("CtrlX", controllerX);
        nbt.setInteger("CtrlY", controllerY);
        nbt.setInteger("CtrlZ", controllerZ);
    }
}
