package ic2heavymachinery.slot;

import net.minecraft.src.*;

public class IC2HM_SlotOutput extends Slot {

    public IC2HM_SlotOutput(IInventory inv, int slotIndex, int x, int y) {
        super(inv, slotIndex, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }
}
