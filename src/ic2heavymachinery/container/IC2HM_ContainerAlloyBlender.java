package ic2heavymachinery.container;

import net.minecraft.src.*;
import ic2heavymachinery.tile.*;
import ic2heavymachinery.slot.*;

public class IC2HM_ContainerAlloyBlender extends Container {

    private IC2HM_TileAlloyBlender tile;
    private int lastCookTime;
    private int lastEnergy;

    public IC2HM_ContainerAlloyBlender(InventoryPlayer playerInv, IC2HM_TileAlloyBlender tile) {
        this.tile = tile;

        // Input slot 1 (slot 0) — GUI slot at (47,17), item renders +1,+1
        addSlot(new Slot(tile, 0, 48, 18));
        // Input slot 2 (slot 1) — GUI slot at (71,17)
        addSlot(new Slot(tile, 1, 72, 18));
        // Output slot (slot 2) — GUI big_slot at (133,30), item renders +5,+5
        addSlot(new IC2HM_SlotOutput(tile, 2, 138, 35));
        // Battery slot (slot 3) — GUI slot at (59,53), centered below inputs
        addSlot(new Slot(tile, 3, 60, 54));

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Player hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return tile.canInteractWith(player);
    }

    public void updateCraftingResults() {
        super.updateCraftingResults();
        for (int i = 0; i < this.field_20121_g.size(); i++) {
            ICrafting crafter = (ICrafting) this.field_20121_g.get(i);
            if (lastCookTime != tile.cookTime)
                crafter.func_20158_a(this, 0, tile.cookTime);
            if (lastEnergy != tile.energy)
                crafter.func_20158_a(this, 1, tile.energy);
        }
        lastCookTime = tile.cookTime;
        lastEnergy = tile.energy;
    }

    public void func_20112_a(int id, int value) {
        if (id == 0) tile.cookTime = value;
        if (id == 1) tile.energy = value;
    }

    public ItemStack getStackInSlot(int slotIndex) {
        ItemStack result = null;
        Slot slot = (Slot) this.slots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            result = stackInSlot.copy();

            if (slotIndex < 4) {
                // Machine slot → player inventory
                this.func_28125_a(stackInSlot, 4, 40, true);
            } else {
                // Player inventory → machine input slots
                this.func_28125_a(stackInSlot, 0, 2, false);
            }

            if (stackInSlot.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }

            if (stackInSlot.stackSize == result.stackSize) return null;
            slot.onPickupFromSlot(stackInSlot);
        }
        return result;
    }
}
