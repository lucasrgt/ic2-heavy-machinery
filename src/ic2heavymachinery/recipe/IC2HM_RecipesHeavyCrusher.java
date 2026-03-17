package ic2heavymachinery.recipe;

import net.minecraft.src.*;
import java.util.HashMap;
import java.util.Random;

public class IC2HM_RecipesHeavyCrusher {

    private static IC2HM_RecipesHeavyCrusher instance;
    private HashMap recipes = new HashMap();
    private Random rand = new Random();

    public static IC2HM_RecipesHeavyCrusher getInstance() {
        if (instance == null) {
            instance = new IC2HM_RecipesHeavyCrusher();
        }
        return instance;
    }

    public void addRecipe(int inputId, int inputMeta, ItemStack mainOutput, ItemStack byproduct, int byproductChance) {
        long key = encodeKey(inputId, inputMeta);
        recipes.put(Long.valueOf(key), new CrushRecipe(mainOutput, byproduct, byproductChance));
    }

    public void addRecipe(int inputId, ItemStack mainOutput, ItemStack byproduct, int byproductChance) {
        addRecipe(inputId, 0, mainOutput, byproduct, byproductChance);
    }

    public CrushRecipe getRecipe(ItemStack input) {
        if (input == null) return null;
        long key = encodeKey(input.itemID, input.getItemDamage());
        CrushRecipe r = (CrushRecipe) recipes.get(Long.valueOf(key));
        if (r != null) return r;
        // Try wildcard meta
        key = encodeKey(input.itemID, 0);
        return (CrushRecipe) recipes.get(Long.valueOf(key));
    }

    /** Roll byproduct chance. Returns byproduct ItemStack or null. */
    public ItemStack rollByproduct(CrushRecipe recipe) {
        if (recipe.byproduct == null || recipe.byproductChance <= 0) return null;
        if (rand.nextInt(100) < recipe.byproductChance) {
            return recipe.byproduct.copy();
        }
        return null;
    }

    private long encodeKey(int id, int meta) {
        return ((long) id << 16) | (meta & 0xFFFF);
    }

    public static class CrushRecipe {
        public final ItemStack mainOutput;
        public final ItemStack byproduct;
        public final int byproductChance; // percentage 0-100

        public CrushRecipe(ItemStack mainOutput, ItemStack byproduct, int byproductChance) {
            this.mainOutput = mainOutput;
            this.byproduct = byproduct;
            this.byproductChance = byproductChance;
        }
    }
}
