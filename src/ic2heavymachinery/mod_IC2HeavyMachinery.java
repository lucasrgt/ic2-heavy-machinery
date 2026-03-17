package ic2heavymachinery;

import net.minecraft.src.*;
import ic2heavymachinery.block.*;
import ic2heavymachinery.tile.*;
import ic2heavymachinery.item.*;
import ic2heavymachinery.recipe.*;
import ic2heavymachinery.render.*;
import java.util.Random;

public class mod_IC2HeavyMachinery extends BaseMod {

    // === Block IDs ===
    public static final int ID_HEAVY_CRUSHER = 180;
    public static final int ID_HEAVY_PORT = 181;
    // 182 = reserved for future controllers
    public static final int ID_NICKEL_ORE = 183;
    public static final int ID_SILVER_ORE = 184;
    public static final int ID_PLATINUM_ORE = 185;
    public static final int ID_CINNABAR_ORE = 186;
    public static final int ID_ALLOY_BLENDER = 187;
    public static final int ID_ITEM_CABLE_BASIC = 188;
    public static final int ID_ITEM_CABLE_REINFORCED = 189;
    public static final int ID_ITEM_CABLE_ADVANCED = 190;

    // === Item IDs ===
    public static final int ID_NICKEL_DUST = 600;
    public static final int ID_SILVER_DUST = 601;
    public static final int ID_PLATINUM_DUST = 602;
    public static final int ID_CINNABAR_DUST = 603;
    public static final int ID_SULFUR_DUST = 604;
    public static final int ID_INVAR_BLEND = 605;
    public static final int ID_ELECTRUM_BLEND = 606;
    public static final int ID_NICKEL_INGOT = 607;
    public static final int ID_SILVER_INGOT = 608;
    public static final int ID_PLATINUM_INGOT = 609;
    public static final int ID_INVAR_INGOT = 610;
    public static final int ID_ELECTRUM_INGOT = 611;
    public static final int ID_INVAR_PLATE = 612;
    public static final int ID_PLATINUM_PLATE = 613;
    public static final int ID_HEAVY_CIRCUIT = 614;
    public static final int ID_INDUSTRIAL_CIRCUIT = 615;
    public static final int ID_VULCANIZED_RUBBER = 616;
    public static final int ID_SOLID_MERCURY = 617;
    public static final int ID_COKE = 618;
    public static final int ID_TAR = 619;

    // === Block Instances ===
    public static Block heavyCrusher;
    public static Block heavyPort;
    public static Block nickelOre;
    public static Block silverOre;
    public static Block platinumOre;
    public static Block cinnabarOre;
    public static Block alloyBlender;
    public static Block itemCableBasic;
    public static Block itemCableReinforced;
    public static Block itemCableAdvanced;

    // === Render IDs ===
    public static int itemCableRenderID;

    // === Renderer ===
    private static IC2HM_RenderItemCable itemCableRenderer;

    // === Item Instances ===
    public static Item nickelDust;
    public static Item silverDust;
    public static Item platinumDust;
    public static Item cinnabarDust;
    public static Item sulfurDust;
    public static Item invarBlend;
    public static Item electrumBlend;
    public static Item nickelIngot;
    public static Item silverIngot;
    public static Item platinumIngot;
    public static Item invarIngot;
    public static Item electrumIngot;
    public static Item invarPlate;
    public static Item platinumPlate;
    public static Item heavyCircuit;
    public static Item industrialCircuit;
    public static Item vulcanizedRubber;
    public static Item solidMercury;
    public static Item coke;
    public static Item tar;

    public static final String ATLAS = "/blocks/ic2_heavy_machinery.png";

    @Override
    public String Version() {
        return "0.1.0";
    }

    public void ModsLoaded() {
        IC2HM_IC2Ref.init();

        // ======= RENDER IDS =======
        itemCableRenderID = ModLoader.getUniqueBlockModelID(this, true);
        itemCableRenderer = new IC2HM_RenderItemCable();

        // ======= ITEMS =======
        nickelDust = new IC2HM_ItemMaterial(ID_NICKEL_DUST - 256).setName("nickelDust").setIconPath("/item/ic2hm_nickel_dust.png");
        silverDust = new IC2HM_ItemMaterial(ID_SILVER_DUST - 256).setName("silverDust").setIconPath("/item/ic2hm_silver_dust.png");
        platinumDust = new IC2HM_ItemMaterial(ID_PLATINUM_DUST - 256).setName("platinumDust").setIconPath("/item/ic2hm_platinum_dust.png");
        cinnabarDust = new IC2HM_ItemMaterial(ID_CINNABAR_DUST - 256).setName("cinnabarDust").setIconPath("/item/ic2hm_cinnabar_dust.png");
        sulfurDust = new IC2HM_ItemMaterial(ID_SULFUR_DUST - 256).setName("sulfurDust").setIconPath("/item/ic2hm_sulfur_dust.png");
        invarBlend = new IC2HM_ItemMaterial(ID_INVAR_BLEND - 256).setName("invarBlend").setIconPath("/item/ic2hm_invar_blend.png");
        electrumBlend = new IC2HM_ItemMaterial(ID_ELECTRUM_BLEND - 256).setName("electrumBlend").setIconPath("/item/ic2hm_electrum_blend.png");
        nickelIngot = new IC2HM_ItemMaterial(ID_NICKEL_INGOT - 256).setName("nickelIngot").setIconPath("/item/ic2hm_nickel_ingot.png");
        silverIngot = new IC2HM_ItemMaterial(ID_SILVER_INGOT - 256).setName("silverIngot").setIconPath("/item/ic2hm_silver_ingot.png");
        platinumIngot = new IC2HM_ItemMaterial(ID_PLATINUM_INGOT - 256).setName("platinumIngot").setIconPath("/item/ic2hm_platinum_ingot.png");
        invarIngot = new IC2HM_ItemMaterial(ID_INVAR_INGOT - 256).setName("invarIngot").setIconPath("/item/ic2hm_invar_ingot.png");
        electrumIngot = new IC2HM_ItemMaterial(ID_ELECTRUM_INGOT - 256).setName("electrumIngot").setIconPath("/item/ic2hm_electrum_ingot.png");
        invarPlate = new IC2HM_ItemMaterial(ID_INVAR_PLATE - 256).setName("invarPlate").setIconPath("/item/ic2hm_invar_plate.png");
        platinumPlate = new IC2HM_ItemMaterial(ID_PLATINUM_PLATE - 256).setName("platinumPlate").setIconPath("/item/ic2hm_platinum_plate.png");
        heavyCircuit = new IC2HM_ItemMaterial(ID_HEAVY_CIRCUIT - 256).setName("heavyCircuit").setIconPath("/item/ic2hm_heavy_circuit.png");
        industrialCircuit = new IC2HM_ItemMaterial(ID_INDUSTRIAL_CIRCUIT - 256).setName("industrialCircuit").setIconPath("/item/ic2hm_industrial_circuit.png");
        vulcanizedRubber = new IC2HM_ItemMaterial(ID_VULCANIZED_RUBBER - 256).setName("vulcanizedRubber").setIconPath("/item/ic2hm_vulcanized_rubber.png");
        solidMercury = new IC2HM_ItemMaterial(ID_SOLID_MERCURY - 256).setName("solidMercury").setIconPath("/item/ic2hm_solid_mercury.png");
        coke = new IC2HM_ItemMaterial(ID_COKE - 256).setName("coke").setIconPath("/item/ic2hm_coke.png");
        tar = new IC2HM_ItemMaterial(ID_TAR - 256).setName("tar").setIconPath("/item/ic2hm_tar.png");

        ModLoader.AddName(nickelDust, "Nickel Dust");
        ModLoader.AddName(silverDust, "Silver Dust");
        ModLoader.AddName(platinumDust, "Platinum Dust");
        ModLoader.AddName(cinnabarDust, "Cinnabar Dust");
        ModLoader.AddName(sulfurDust, "Sulfur Dust");
        ModLoader.AddName(invarBlend, "Invar Blend");
        ModLoader.AddName(electrumBlend, "Electrum Blend");
        ModLoader.AddName(nickelIngot, "Nickel Ingot");
        ModLoader.AddName(silverIngot, "Silver Ingot");
        ModLoader.AddName(platinumIngot, "Platinum Ingot");
        ModLoader.AddName(invarIngot, "Invar Ingot");
        ModLoader.AddName(electrumIngot, "Electrum Ingot");
        ModLoader.AddName(invarPlate, "Invar Plate");
        ModLoader.AddName(platinumPlate, "Platinum Plate");
        ModLoader.AddName(heavyCircuit, "Heavy Circuit");
        ModLoader.AddName(industrialCircuit, "Industrial Circuit");
        ModLoader.AddName(vulcanizedRubber, "Vulcanized Rubber");
        ModLoader.AddName(solidMercury, "Solid Mercury");
        ModLoader.AddName(coke, "Coke");
        ModLoader.AddName(tar, "Tar");

        // ======= BLOCKS =======
        heavyCrusher = new IC2HM_BlockHeavyCrusher(ID_HEAVY_CRUSHER);
        heavyPort = new IC2HM_BlockHeavyPort(ID_HEAVY_PORT); // textures hardcoded per metadata (red=energy, green=item, blue=fluid)
        nickelOre = new IC2HM_BlockOre(ID_NICKEL_ORE, 9, true, "nickelOre");
        silverOre = new IC2HM_BlockOre(ID_SILVER_ORE, 10, true, "silverOre");
        platinumOre = new IC2HM_BlockOre(ID_PLATINUM_ORE, 11, true, "platinumOre");
        cinnabarOre = new IC2HM_BlockOre(ID_CINNABAR_ORE, 12, false, "cinnabarOre");
        ((IC2HM_BlockOre) cinnabarOre).setDrop(ID_CINNABAR_DUST, 2);
        alloyBlender = new IC2HM_BlockAlloyBlender(ID_ALLOY_BLENDER);
        itemCableBasic = new IC2HM_BlockItemCable(ID_ITEM_CABLE_BASIC, 0, 64).setBlockName("itemCableBasic");
        itemCableReinforced = new IC2HM_BlockItemCable(ID_ITEM_CABLE_REINFORCED, 1, 65).setBlockName("itemCableReinforced");
        itemCableAdvanced = new IC2HM_BlockItemCable(ID_ITEM_CABLE_ADVANCED, 2, 66).setBlockName("itemCableAdvanced");

        ModLoader.RegisterBlock(heavyCrusher);
        ModLoader.RegisterBlock(heavyPort);
        ModLoader.RegisterBlock(nickelOre);
        ModLoader.RegisterBlock(silverOre);
        ModLoader.RegisterBlock(platinumOre);
        ModLoader.RegisterBlock(cinnabarOre);
        ModLoader.RegisterBlock(alloyBlender);
        ModLoader.RegisterBlock(itemCableBasic);
        ModLoader.RegisterBlock(itemCableReinforced);
        ModLoader.RegisterBlock(itemCableAdvanced);

        ModLoader.AddName(heavyCrusher, "Heavy Crusher Controller");
        ModLoader.AddName(heavyPort, "Heavy Port");
        ModLoader.AddName(nickelOre, "Nickel Ore");
        ModLoader.AddName(silverOre, "Silver Ore");
        ModLoader.AddName(platinumOre, "Platinum Ore");
        ModLoader.AddName(cinnabarOre, "Cinnabar Ore");
        ModLoader.AddName(alloyBlender, "Alloy Blender");
        ModLoader.AddName(itemCableBasic, "Item Cable");
        ModLoader.AddName(itemCableReinforced, "Reinforced Item Cable");
        ModLoader.AddName(itemCableAdvanced, "Advanced Item Cable");

        // ======= TILE ENTITIES =======
        ModLoader.RegisterTileEntity(IC2HM_TileAlloyBlender.class, "IC2HM_AlloyBlender");
        ModLoader.RegisterTileEntity(IC2HM_TileHeavyCrusher.class, "IC2HM_HeavyCrusher");
        ModLoader.RegisterTileEntity(IC2HM_TileHeavyPort.class, "IC2HM_ItemPort");
        ModLoader.RegisterTileEntity(IC2HM_TileEnergyPort.class, "IC2HM_EnergyPort");
        ModLoader.RegisterTileEntity(IC2HM_TileItemCable.class, "IC2HM_ItemCable");

        // ======= SMELTING RECIPES =======
        ModLoader.AddSmelting(ID_NICKEL_DUST, new ItemStack(nickelIngot));
        ModLoader.AddSmelting(ID_SILVER_DUST, new ItemStack(silverIngot));
        ModLoader.AddSmelting(ID_PLATINUM_DUST, new ItemStack(platinumIngot));
        ModLoader.AddSmelting(ID_INVAR_BLEND, new ItemStack(invarIngot));
        ModLoader.AddSmelting(ID_ELECTRUM_BLEND, new ItemStack(electrumIngot));
        ModLoader.AddSmelting(ID_NICKEL_ORE, new ItemStack(nickelIngot));
        ModLoader.AddSmelting(ID_SILVER_ORE, new ItemStack(silverIngot));
        ModLoader.AddSmelting(ID_PLATINUM_ORE, new ItemStack(platinumIngot));

        // ======= ALLOY BLENDER RECIPES =======
        IC2HM_RecipesAlloyBlender blenderRecipes = IC2HM_RecipesAlloyBlender.getInstance();
        // 2x Iron Dust + 1x Nickel Dust → 3x Invar Blend
        blenderRecipes.addRecipe(IC2HM_IC2Ref.itemDustIron.shiftedIndex, 2, nickelDust.shiftedIndex, 1,
                new ItemStack(invarBlend, 3));
        // 1x Gold Dust + 1x Silver Dust → 2x Electrum Blend
        blenderRecipes.addRecipe(IC2HM_IC2Ref.itemDustGold.shiftedIndex, 1, silverDust.shiftedIndex, 1,
                new ItemStack(electrumBlend, 2));
        // 3x Copper Dust + 1x Tin Dust → 4x Bronze Dust
        blenderRecipes.addRecipe(IC2HM_IC2Ref.itemDustCopper.shiftedIndex, 3, IC2HM_IC2Ref.itemDustTin.shiftedIndex, 1,
                new ItemStack(IC2HM_IC2Ref.itemDustBronze, 4));

        // ======= HEAVY CRUSHER RECIPES =======
        IC2HM_RecipesHeavyCrusher crusherRecipes = IC2HM_RecipesHeavyCrusher.getInstance();
        // Vanilla ores: 2x yield + 10% byproduct
        crusherRecipes.addRecipe(Block.oreIron.blockID, new ItemStack(IC2HM_IC2Ref.itemDustIron, 2), new ItemStack(nickelDust), 10);
        crusherRecipes.addRecipe(Block.oreGold.blockID, new ItemStack(IC2HM_IC2Ref.itemDustGold, 2), new ItemStack(silverDust), 10);
        // IC2 ores
        crusherRecipes.addRecipe(15 /* copper ore meta */, new ItemStack(IC2HM_IC2Ref.itemDustCopper, 2), null, 0);
        crusherRecipes.addRecipe(16 /* tin ore meta */, new ItemStack(IC2HM_IC2Ref.itemDustTin, 2), null, 0);
        // IC2HM ores
        crusherRecipes.addRecipe(ID_NICKEL_ORE, new ItemStack(nickelDust, 2), new ItemStack(platinumDust), 5);
        crusherRecipes.addRecipe(ID_SILVER_ORE, new ItemStack(silverDust, 2), null, 0);
        crusherRecipes.addRecipe(ID_PLATINUM_ORE, new ItemStack(platinumDust, 2), new ItemStack(nickelDust), 10);
        crusherRecipes.addRecipe(ID_CINNABAR_ORE, new ItemStack(cinnabarDust, 3), null, 0);
        // Stone → Cobblestone, Cobblestone → Sand (utility)
        crusherRecipes.addRecipe(Block.stone.blockID, new ItemStack(Block.cobblestone), null, 0);
        crusherRecipes.addRecipe(Block.cobblestone.blockID, new ItemStack(Block.sand), null, 0);

        // ======= REAL RECIPES =======

        // Heavy Circuit: Glowstone + Gold Cable + Electrum Ingot + Advanced Circuit
        ModLoader.AddRecipe(new ItemStack(heavyCircuit, 1),
                new Object[] { "GcG", "EAE", "ccc",
                    'G', Item.lightStoneDust,
                    'c', new ItemStack(IC2HM_IC2Ref.itemCable, 1, 2), // Gold Cable
                    'E', electrumIngot,
                    'A', IC2HM_IC2Ref.itemPartCircuitAdv });

        // Alloy Blender: Refined Iron + Piston + Advanced Circuit + Machine Block + MFE
        ModLoader.AddRecipe(new ItemStack(alloyBlender, 1),
                new Object[] { "RPR", "AMA", "RER",
                    'R', IC2HM_IC2Ref.itemIngotAdvIron,
                    'P', Block.pistonBase,
                    'A', IC2HM_IC2Ref.itemPartCircuitAdv,
                    'M', new ItemStack(IC2HM_IC2Ref.blockMachine, 1, 1), // Machine Block
                    'E', new ItemStack(IC2HM_IC2Ref.blockElectric, 1, 1) }); // MFE

        // Heavy Crusher: Diamond + Piston + Heavy Circuit + IC2 Macerator + MFE
        ModLoader.AddRecipe(new ItemStack(heavyCrusher, 1),
                new Object[] { "DPD", "HMH", "DED",
                    'D', Item.diamond,
                    'P', Block.pistonBase,
                    'H', heavyCircuit,
                    'M', new ItemStack(IC2HM_IC2Ref.blockMachine, 1, 3), // Macerator
                    'E', new ItemStack(IC2HM_IC2Ref.blockElectric, 1, 1) }); // MFE

        // ======= DEBUG RECIPES (temporary — easy to get for testing) =======
        // 1 soul sand → 1 alloy blender
        ModLoader.AddRecipe(new ItemStack(alloyBlender, 1),
                new Object[] { "#", '#', Block.slowSand });
        // 1 mossy cobblestone → 1 heavy crusher
        ModLoader.AddRecipe(new ItemStack(heavyCrusher, 1),
                new Object[] { "#", '#', Block.cobblestoneMossy });
        // Dusts for testing
        ModLoader.AddRecipe(new ItemStack(nickelDust, 8),
                new Object[] { "#", '#', Block.gravel });
        ModLoader.AddRecipe(new ItemStack(silverDust, 8),
                new Object[] { "#", '#', Block.sand });
        ModLoader.AddRecipe(new ItemStack(cinnabarDust, 8),
                new Object[] { "#", '#', Block.netherrack });
        // 1 dirt → 16 item cables (basic)
        ModLoader.AddRecipe(new ItemStack(itemCableBasic, 16),
                new Object[] { "#", '#', Block.dirt });
        // 1 clay → 16 reinforced item cables
        ModLoader.AddRecipe(new ItemStack(itemCableReinforced, 16),
                new Object[] { "#", '#', Block.blockClay });
        // 1 lapis block → 16 advanced item cables
        ModLoader.AddRecipe(new ItemStack(itemCableAdvanced, 16),
                new Object[] { "#", '#', Block.blockLapis });

        System.out.println("[IC2HM] IC2 Heavy Machinery v" + Version() + " loaded!");
    }

    // ======= ORE GENERATION =======
    @Override
    public void GenerateSurface(World world, Random random, int chunkX, int chunkZ) {
        // Nickel Ore: Y 5-40, vein size 6, 3 per chunk
        for (int i = 0; i < 3; i++) {
            int x = chunkX + random.nextInt(16);
            int y = 5 + random.nextInt(35);
            int z = chunkZ + random.nextInt(16);
            new WorldGenMinable(ID_NICKEL_ORE, 6).generate(world, random, x, y, z);
        }
        // Silver Ore: Y 5-30, vein size 5, 2 per chunk
        for (int i = 0; i < 2; i++) {
            int x = chunkX + random.nextInt(16);
            int y = 5 + random.nextInt(25);
            int z = chunkZ + random.nextInt(16);
            new WorldGenMinable(ID_SILVER_ORE, 5).generate(world, random, x, y, z);
        }
        // Platinum Ore: Y 5-15, vein size 3, 1 per chunk
        {
            int x = chunkX + random.nextInt(16);
            int y = 5 + random.nextInt(10);
            int z = chunkZ + random.nextInt(16);
            new WorldGenMinable(ID_PLATINUM_ORE, 3).generate(world, random, x, y, z);
        }
        // Cinnabar Ore: Y 10-35, vein size 5, 2 per chunk
        for (int i = 0; i < 2; i++) {
            int x = chunkX + random.nextInt(16);
            int y = 10 + random.nextInt(25);
            int z = chunkZ + random.nextInt(16);
            new WorldGenMinable(ID_CINNABAR_ORE, 5).generate(world, random, x, y, z);
        }
    }

    // ======= CUSTOM BLOCK RENDERING =======
    @Override
    public boolean RenderWorldBlock(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block, int modelID) {
        if (modelID == itemCableRenderID) {
            return itemCableRenderer.renderWorld(renderer, world, x, y, z, block);
        }
        return false;
    }

    @Override
    public void RenderInvBlock(RenderBlocks renderer, Block block, int metadata, int modelID) {
        if (modelID == itemCableRenderID) {
            itemCableRenderer.renderInventory(renderer, block, metadata);
        }
    }
}
