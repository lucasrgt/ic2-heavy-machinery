package net.minecraft.src;

import ic2heavymachinery.block.*;
import ic2heavymachinery.tile.*;
import aero.machineapi.*;
import aero.modellib.*;
import java.io.File;

public class mod_IC2HeavyMachinery extends BaseMod {
    public static mod_IC2HeavyMachinery instance;

    // Configurações e IDs
    public static int blockChassisID = 240;
    public static int blockMachineCoreID = 241;
    public static int blockPortID = 242;

    // Blocos
    public static Block heavyChassis;
    public static Block machineCore;
    public static Block machinePort;

    // Modelos
    public static Aero_JsonModel modelHeavyCompressor;

    @Override
    public String getVersion() {
        return "v0.1 - Integrated";
    }

    @Override
    public void load() {
        instance = this;

        // Inicialização de Blocos
        heavyChassis = new BlockHeavyChassis(blockChassisID);
        ModLoader.RegisterBlock(heavyChassis);
        ModLoader.AddName(heavyChassis, "Heavy Machinery Chassis");

        machineCore = new BlockHeavyCompressor(blockMachineCoreID);
        ModLoader.RegisterBlock(machineCore);
        ModLoader.RegisterTileEntity(TileHeavyCompressor.class, "HeavyCompressorCore");
        ModLoader.AddName(machineCore, "Heavy Compressor Core");

        machinePort = new BlockHeavyPort(blockPortID);
        ModLoader.RegisterBlock(machinePort);
        ModLoader.RegisterTileEntity(TileHeavyPort.class, "HeavyMachinePort");
        ModLoader.AddName(machinePort, "Heavy Machinery Port");

        // Renders
        ((BlockHeavyCompressor) machineCore).renderID = ModLoader.getUniqueBlockModelID(this, true);

        // Carregamento de Modelos (Exemplo de caminho - ajuste conforme necessário)
        try {
            // modelHeavyCompressor = Aero_JsonModelLoader.loadModel(new
            // File(Minecraft.getMinecraftDir(),
            // "mods/heavymachinery/models/heavy_compressor.aero.json"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO: Receitas e Integração com IC2 API
    }

    @Override
    public void renderInvBlock(RenderBlocks rb, Block block, int i, int j) {
        if (block == machineCore) {
            // Render de inventário opcional
        }
    }

    @Override
    public boolean renderWorldBlock(RenderBlocks rb, IBlockAccess iba, int x, int y, int z, Block block, int modelID) {
        if (modelID == ((BlockHeavyCompressor) machineCore).renderID) {
            TileEntity te = iba.getBlockTileEntity(x, y, z);
            if (te instanceof TileHeavyCompressor) {
                // Aqui chamamos o renderizador da Modellib
                // Aero_JsonModelRenderer.render(modelHeavyCompressor, (TileHeavyCompressor)te,
                // x, y, z);
                return true;
            }
        }
        return false;
    }
}
