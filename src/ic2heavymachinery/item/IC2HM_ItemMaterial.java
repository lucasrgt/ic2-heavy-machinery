package ic2heavymachinery.item;

import net.minecraft.src.*;

public class IC2HM_ItemMaterial extends Item {

    private int iconIndex;

    public IC2HM_ItemMaterial(int id) {
        super(id);
        maxStackSize = 64;
    }

    public IC2HM_ItemMaterial setName(String name) {
        setItemName(name);
        return this;
    }

    public IC2HM_ItemMaterial setIconPath(String path) {
        this.iconIndex = ModLoader.addOverride("/gui/items.png", path);
        return this;
    }

    @Override
    public int getIconFromDamage(int damage) {
        return iconIndex;
    }
}
