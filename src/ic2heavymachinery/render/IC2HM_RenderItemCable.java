package ic2heavymachinery.render;

import net.minecraft.src.*;
import ic2heavymachinery.block.*;
import org.lwjgl.opengl.GL11;

public class IC2HM_RenderItemCable {

    public boolean renderWorld(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
        float min = 6.0F / 16.0F;
        float max = 10.0F / 16.0F;
        IC2HM_BlockItemCable cable = (IC2HM_BlockItemCable) block;

        // Center cube — always render all 6 faces
        renderer.renderAllFaces = true;
        block.setBlockBounds(min, min, min, max, max, max);
        renderer.renderStandardBlock(block, x, y, z);

        // Arms — auto-cull face touching opaque neighbors (machines)
        renderer.renderAllFaces = false;
        for (int side = 0; side < 6; side++) {
            if (cable.canConnectToSide(world, x, y, z, side)) {
                switch (side) {
                    case 0: block.setBlockBounds(min, 0.0F, min, max, min, max); break;
                    case 1: block.setBlockBounds(min, max,  min, max, 1.0F, max); break;
                    case 2: block.setBlockBounds(min, min,  0.0F, max, max, min); break;
                    case 3: block.setBlockBounds(min, min,  max, max, max, 1.0F); break;
                    case 4: block.setBlockBounds(0.0F, min, min, min, max, max); break;
                    case 5: block.setBlockBounds(max, min,  min, 1.0F, max, max); break;
                }
                renderer.renderStandardBlock(block, x, y, z);
            }
        }

        renderer.renderAllFaces = true;
        block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }

    public void renderInventory(RenderBlocks renderer, Block block, int metadata) {
        float bMin = 6.0F / 16.0F;
        float bMax = 10.0F / 16.0F;
        block.setBlockBounds(2.0F/16, bMin, bMin, 14.0F/16, bMax, bMax);
        Tessellator t = Tessellator.instance;
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        t.startDrawingQuads(); t.setNormal(0, -1, 0);
        renderer.renderBottomFace(block, 0, 0, 0, block.getBlockTextureFromSide(0)); t.draw();
        t.startDrawingQuads(); t.setNormal(0, 1, 0);
        renderer.renderTopFace(block, 0, 0, 0, block.getBlockTextureFromSide(1)); t.draw();
        t.startDrawingQuads(); t.setNormal(0, 0, -1);
        renderer.renderEastFace(block, 0, 0, 0, block.getBlockTextureFromSide(2)); t.draw();
        t.startDrawingQuads(); t.setNormal(0, 0, 1);
        renderer.renderWestFace(block, 0, 0, 0, block.getBlockTextureFromSide(3)); t.draw();
        t.startDrawingQuads(); t.setNormal(-1, 0, 0);
        renderer.renderNorthFace(block, 0, 0, 0, block.getBlockTextureFromSide(4)); t.draw();
        t.startDrawingQuads(); t.setNormal(1, 0, 0);
        renderer.renderSouthFace(block, 0, 0, 0, block.getBlockTextureFromSide(5)); t.draw();
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }
}
