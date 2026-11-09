package com.Goderium.StructureRebuildNH.common;

import com.Goderium.StructureRebuildNH.StructureRebuildNH;
import com.Goderium.StructureRebuildNH.config.RebuildConfig;
import com.Goderium.StructureRebuildNH.util.Bounds;

import java.util.List;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

public final class Protection {
    public static boolean protects(World world, int x, int y, int z, TileEntity target, List<Bounds> formed) {
        if (target != null && x == target.xCoord && y == target.yCoord && z == target.zCoord) return true;
        for (Bounds bounds : formed) if (bounds.contains(x, y, z)) return true;
        Block block = world.getBlock(x, y, z);
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(block);
        if (id != null) {
            String key = id.toString().toLowerCase(Locale.ROOT);
            if (RebuildConfig.protectedMods.contains(id.modId.toLowerCase(Locale.ROOT))
                || RebuildConfig.protectedBlocks.contains(key)
                || RebuildConfig.protectedBlocks.contains(key + "@" + world.getBlockMetadata(x, y, z))) return true;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile == null) return false;
        if (tile instanceof IInventory || tile instanceof IFluidHandler || RebuildConfig.protectTileEntities) return true;
        if (tile instanceof IGregTechTileEntity) {
            Object meta = ((IGregTechTileEntity) tile).getMetaTileEntity();
            if (meta instanceof MTEHatch || meta instanceof MTEMultiBlockBase) return true;
        }
        return false;
    }
}
