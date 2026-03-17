package ic2heavymachinery.container;

import net.minecraft.src.*;
import ic2heavymachinery.tile.*;
import ic2heavymachinery.slot.*;

public class IC2HM_ContainerHeavyCrusher extends Container {

    private IC2HM_TileHeavyCrusher tile;
    private int lastEnergy = -1;
    private int[] lastProcessTime = {-1, -1, -1, -1};
    private int lastMaxProcessTime = -1;

    public IC2HM_ContainerHeavyCrusher(InventoryPlayer playerInv, IC2HM_TileHeavyCrusher tile) {
        this.tile = tile;

        // 4 input slots — GUI bg at (36,22), slot +1 inside
        addSlot(new Slot(tile, 0, 37, 23));
        addSlot(new Slot(tile, 1, 72, 23));
        addSlot(new Slot(tile, 2, 107, 23));
        addSlot(new Slot(tile, 3, 142, 23));

        // 4 primary output slots — GUI bg at (36,68)
        addSlot(new IC2HM_SlotOutput(tile, 4, 37, 69));
        addSlot(new IC2HM_SlotOutput(tile, 5, 72, 69));
        addSlot(new IC2HM_SlotOutput(tile, 6, 107, 69));
        addSlot(new IC2HM_SlotOutput(tile, 7, 142, 69));

        // 4 subproduct output slots — GUI bg at (36,90)
        addSlot(new IC2HM_SlotOutput(tile, 8, 37, 91));
        addSlot(new IC2HM_SlotOutput(tile, 9, 72, 91));
        addSlot(new IC2HM_SlotOutput(tile, 10, 107, 91));
        addSlot(new IC2HM_SlotOutput(tile, 11, 142, 91));

        // Player inventory (3 rows) — ySize=210, starts at 210-82=128
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 128 + row * 18));
            }
        }

        // Hotbar — 128 + 3*18 + 4 = 186
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 186));
        }
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return tile.canInteractWith(player);
    }

    public void updateCraftingResults() {
        super.updateCraftingResults();
        for (int j = 0; j < this.field_20121_g.size(); j++) {
            ICrafting crafter = (ICrafting) this.field_20121_g.get(j);
            if (lastEnergy != tile.energy) {
                crafter.func_20158_a(this, 0, tile.energy);
            }
            for (int lane = 0; lane < 4; lane++) {
                if (lastProcessTime[lane] != tile.processTime[lane]) {
                    crafter.func_20158_a(this, 1 + lane, tile.processTime[lane]);
                }
            }
            if (lastMaxProcessTime != tile.maxProcessTime) {
                crafter.func_20158_a(this, 5, tile.maxProcessTime);
            }
        }
        lastEnergy = tile.energy;
        for (int lane = 0; lane < 4; lane++) lastProcessTime[lane] = tile.processTime[lane];
        lastMaxProcessTime = tile.maxProcessTime;
    }

    public void func_20112_a(int id, int value) {
        if (id == 0) tile.energy = value;
        if (id >= 1 && id <= 4) tile.processTime[id - 1] = value;
        if (id == 5) tile.maxProcessTime = value;
    }

    public ItemStack getStackInSlot(int slotIndex) {
        ItemStack result = null;
        Slot slot = (Slot) this.slots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();
            if (slotIndex < 12) {
                this.func_28125_a(slotStack, 12, 48, true);
            } else {
                this.func_28125_a(slotStack, 0, 4, false);
            }
            if (slotStack.stackSize == 0) slot.putStack(null);
            else slot.onSlotChanged();

            if (slotStack.stackSize == result.stackSize) return null;
            slot.onPickupFromSlot(slotStack);
        }
        return result;
    }
}
