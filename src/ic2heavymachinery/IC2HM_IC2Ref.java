package ic2heavymachinery;

import net.minecraft.src.*;

/**
 * Bridge to IC2's mod_IC2 class via reflection.
 * mod_IC2 is in the default package at runtime, so we can't reference it
 * directly from net.minecraft.src. This class loads fields on demand.
 */
public class IC2HM_IC2Ref {
    public static Block blockMachine;
    public static Block blockElectric;
    public static Item itemDustIron;
    public static Item itemDustGold;
    public static Item itemDustCopper;
    public static Item itemDustTin;
    public static Item itemDustBronze;
    public static Item itemCable;
    public static Item itemPartCircuitAdv;
    public static Item itemIngotAdvIron;

    private static boolean loaded = false;

    public static void init() {
        if (loaded) return;
        loaded = true;
        try {
            Class<?> ic2 = Class.forName("mod_IC2");
            blockMachine = (Block) ic2.getField("blockMachine").get(null);
            blockElectric = (Block) ic2.getField("blockElectric").get(null);
            itemDustIron = (Item) ic2.getField("itemDustIron").get(null);
            itemDustGold = (Item) ic2.getField("itemDustGold").get(null);
            itemDustCopper = (Item) ic2.getField("itemDustCopper").get(null);
            itemDustTin = (Item) ic2.getField("itemDustTin").get(null);
            itemDustBronze = (Item) ic2.getField("itemDustBronze").get(null);
            itemCable = (Item) ic2.getField("itemCable").get(null);
            itemPartCircuitAdv = (Item) ic2.getField("itemPartCircuitAdv").get(null);
            itemIngotAdvIron = (Item) ic2.getField("itemIngotAdvIron").get(null);
            System.out.println("[IC2HM] IC2 reference loaded successfully");
        } catch (Exception e) {
            System.err.println("[IC2HM] Failed to load IC2 references: " + e);
        }
    }
}
