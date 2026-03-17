package ic2heavymachinery.recipe;

import net.minecraft.src.*;
import java.util.HashMap;

public class IC2HM_RecipesAlloyBlender {

    private static IC2HM_RecipesAlloyBlender instance;
    private HashMap recipes = new HashMap();

    public static IC2HM_RecipesAlloyBlender getInstance() {
        if (instance == null) {
            instance = new IC2HM_RecipesAlloyBlender();
        }
        return instance;
    }

    /**
     * Add a blending recipe. Order matters: input1 in slot 0, input2 in slot 1.
     * A reverse recipe (input2, input1) is also added automatically.
     */
    public void addRecipe(int input1Id, int input1Count, int input2Id, int input2Count, ItemStack output) {
        long key = encodeKey(input1Id, input2Id);
        recipes.put(Long.valueOf(key), new BlendRecipe(input1Count, input2Count, output));
        // Also add reverse
        if (input1Id != input2Id) {
            long reverseKey = encodeKey(input2Id, input1Id);
            recipes.put(Long.valueOf(reverseKey), new BlendRecipe(input2Count, input1Count, output));
        }
    }

    /**
     * Find a recipe matching the given inputs.
     * Returns null if no recipe found.
     */
    public BlendRecipe getRecipe(ItemStack input1, ItemStack input2) {
        if (input1 == null || input2 == null) return null;
        long key = encodeKey(input1.itemID, input2.itemID);
        BlendRecipe recipe = (BlendRecipe) recipes.get(Long.valueOf(key));
        if (recipe == null) return null;
        // Check counts
        if (input1.stackSize >= recipe.input1Count && input2.stackSize >= recipe.input2Count) {
            return recipe;
        }
        return null;
    }

    private long encodeKey(int id1, int id2) {
        return ((long) id1 << 16) | (id2 & 0xFFFF);
    }

    public static class BlendRecipe {
        public final int input1Count;
        public final int input2Count;
        public final ItemStack output;

        public BlendRecipe(int input1Count, int input2Count, ItemStack output) {
            this.input1Count = input1Count;
            this.input2Count = input2Count;
            this.output = output;
        }
    }
}
