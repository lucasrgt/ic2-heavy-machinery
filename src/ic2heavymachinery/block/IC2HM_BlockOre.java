package ic2heavymachinery.block;

import net.minecraft.src.*;
import forge.ITextureProvider;
import java.util.Random;

public class IC2HM_BlockOre extends Block implements ITextureProvider {

    private int textureIndex;
    private int dropId;
    private int dropCount;
    private boolean dropSelf;

    /**
     * @param id       Block ID
     * @param texIdx   Texture index in atlas
     * @param dropSelf true = drops itself (silk-touch-like), false = drops custom item
     */
    public IC2HM_BlockOre(int id, int texIdx, boolean dropSelf, String name) {
        super(id, texIdx, Material.rock);
        this.textureIndex = texIdx;
        this.dropSelf = dropSelf;
        this.dropId = id;
        this.dropCount = 1;
        setHardness(3.0F);
        setResistance(5.0F);
        setBlockName(name);
    }

    public IC2HM_BlockOre setDrop(int itemId, int count) {
        this.dropSelf = false;
        this.dropId = itemId;
        this.dropCount = count;
        return this;
    }

    @Override
    public String getTextureFile() {
        return mod_IC2HeavyMachinery.ATLAS;
    }

    @Override
    public int getBlockTextureFromSide(int side) {
        return textureIndex;
    }

    @Override
    public int idDropped(int metadata, Random random) {
        return dropId;
    }

    @Override
    public int quantityDropped(Random random) {
        return dropCount;
    }
}
