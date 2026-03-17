package ic2heavymachinery.gui;

import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;
import ic2heavymachinery.tile.IC2HM_TileHeavyCrusher;
import ic2heavymachinery.container.IC2HM_ContainerHeavyCrusher;

public class IC2HM_GuiHeavyCrusher extends GuiContainer {

    private IC2HM_TileHeavyCrusher tile;
    private int mouseX;
    private int mouseY;

    public IC2HM_GuiHeavyCrusher(InventoryPlayer playerInv, IC2HM_TileHeavyCrusher tile) {
        super(new IC2HM_ContainerHeavyCrusher(playerInv, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 210;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    @Override
    protected void drawGuiContainerForegroundLayer() {
        String title = "Heavy Crusher";
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, 4210752);
        fontRenderer.drawString("Inventory", 7, ySize - 96 + 2, 4210752);

        if (!tile.isFormed) {
            String msg = "Structure incomplete!";
            fontRenderer.drawString(msg, (xSize - fontRenderer.getStringWidth(msg)) / 2, 58, 0xFF4444);
            return;
        }

        // Energy tooltip on hover — energy bar at (12, 22) 8x86
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        int relMouseX = this.mouseX - guiLeft;
        int relMouseY = this.mouseY - guiTop;

        if (relMouseX >= 12 && relMouseX < 20 && relMouseY >= 22 && relMouseY < 108) {
            String tooltip = "Energy: " + tile.getStoredEnergy() + " / " + tile.getMaxEnergy() + " EU";
            int tw = this.fontRenderer.getStringWidth(tooltip);
            int tx = relMouseX + 12;
            int ty = relMouseY - 12;
            this.drawGradientRect(tx - 3, ty - 3, tx + tw + 3, ty + 11, -1073741824, -1073741824);
            this.fontRenderer.drawStringWithShadow(tooltip, tx, ty, -1);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int textureID = this.mc.renderEngine.getTexture("/gui/ic2hm_heavycrusher.png");
        this.mc.renderEngine.bindTexture(textureID);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);

        // Energy bar fill (green gradient) — bar at (13, 23) 6x84
        int barX = x + 13, barY = y + 23, barW = 6, barH = 84;
        int scaled = tile.getEnergyScaled(barH);
        if (scaled > 0) {
            int top = barY + barH - scaled;
            for (int sy = top; sy < barY + barH; sy++) {
                int color = (sy % 2 == 0) ? 0xFF3BFB98 : 0xFF36E38A;
                drawRect(barX, sy, barX + barW, sy + 1, color);
            }
        }

        // Reset GL color after drawRect (it taints subsequent textured draws)
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(textureID);

        // 4 independent progress arrows pointing DOWN — sprite at (176, 14), 17x24
        int[] arrowX = {36, 71, 106, 141};
        for (int lane = 0; lane < 4; lane++) {
            int cookScale = tile.getCookProgressScaled(lane, 24);
            if (cookScale > 0) {
                this.drawTexturedModalRect(x + arrowX[lane], y + 42, 176, 14, 17, cookScale);
            }
        }
    }
}
