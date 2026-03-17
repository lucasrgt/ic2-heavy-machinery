package ic2heavymachinery.gui;

import net.minecraft.src.*;
import ic2heavymachinery.tile.*;
import ic2heavymachinery.container.*;
import org.lwjgl.opengl.GL11;

public class IC2HM_GuiAlloyBlender extends GuiContainer {

    private IC2HM_TileAlloyBlender tile;
    private int mouseX;
    private int mouseY;

    public IC2HM_GuiAlloyBlender(InventoryPlayer playerInv, IC2HM_TileAlloyBlender tile) {
        super(new IC2HM_ContainerAlloyBlender(playerInv, tile));
        this.tile = tile;
    }

    protected void drawGuiContainerForegroundLayer() {
        this.fontRenderer.drawString("Alloy Blender", 44, 6, 4210752);
        this.fontRenderer.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);

        // Energy tooltip
        int guiLeft = (this.width - this.xSize) / 2;
        int guiTop = (this.height - this.ySize) / 2;
        int relX = this.mouseX - guiLeft;
        int relY = this.mouseY - guiTop;
        if (relX >= 61 && relX < 75 && relY >= 37 && relY < 51) { // energy bolt area
            String line = "Energy: " + this.tile.energy + " / " + this.tile.maxEnergy + " EU";
            int w = this.fontRenderer.getStringWidth(line);
            int tx = relX + 8;
            int ty = relY - 12;
            this.drawGradientRect(tx - 3, ty - 3, tx + w + 3, ty + 11, -1073741824, -1073741824);
            this.fontRenderer.drawStringWithShadow(line, tx, ty, -1);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.drawScreen(mouseX, mouseY, partialTick);
    }

    protected void drawGuiContainerBackgroundLayer(float partialTick) {
        int textureID = this.mc.renderEngine.getTexture("/gui/ic2hm_alloyblender.png");
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture(textureID);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);

        // Progress arrow fill — GUI component at (99,35), sprite at (176,14)
        int cookScale = this.tile.getCookProgressScaled(24);
        if (cookScale > 0) {
            this.drawTexturedModalRect(x + 99, y + 35, 176, 14, cookScale + 1, 17);
        }

        // IC2-style red lightning bolt energy indicator — centered above battery slot at (59,53)
        int ic2Tex = this.mc.renderEngine.getTexture("/IC2sprites/GUIElecFurnace.png");
        this.mc.renderEngine.bindTexture(ic2Tex);
        this.drawTexturedModalRect(x + 61, y + 37, 56, 36, 14, 14); // empty bolt outline
        int energyScale = this.tile.getEnergyScaled(14);
        if (energyScale > 0) {
            int offset = 14 - energyScale;
            this.drawTexturedModalRect(x + 61, y + 37 + offset, 176, offset, 14, energyScale); // filled red bolt
        }
    }
}
