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
        return reason(world, x, y, z, target, formed) != null;
    }
    /** Null means unprotected. The reason is diagnostic only and does not perform a structure check. */
    public static String reason(World world, int x, int y, int z, TileEntity target, List<Bounds> formed) {
        if (target != null && x == target.xCoord && y == target.yCoord && z == target.zCoord) return "target_controller";
        for (Bounds bounds : formed) if (bounds.contains(x, y, z)) return "formed_gt_structure:" + bounds;
        Block block = world.getBlock(x, y, z);
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(block);
        if (id != null) {
            String key = id.toString().toLowerCase(Locale.ROOT);
            if (RebuildConfig.protectedMods.contains(id.modId.toLowerCase(Locale.ROOT))) return "mod_whitelist:" + id.modId;
            if (RebuildConfig.protectedBlocks.contains(key)) return "block_whitelist:" + key;
            String variant = key + "@" + world.getBlockMetadata(x, y, z);
            if (RebuildConfig.protectedBlocks.contains(variant)) return "block_whitelist:" + variant;
        }
        // World.getTileEntity may create a missing tile via setTileEntity, re-entering our write guard.
        // Inspect an existing tile, or a detached factory result without installing it in the world.
        // Some BlockContainer variants declare tiles but legitimately return null for decorative metadata.
        TileEntity tile = world.getChunkFromBlockCoords(x, z).getTileEntityUnsafe(x & 15, y, z & 15);
        int metadata = world.getBlockMetadata(x, y, z);
        if (tile == null && block.hasTileEntity(metadata)) tile = block.createTileEntity(world, metadata);
        if (tile == null) return null;
        if (tile instanceof IGregTechTileEntity) {
            Object meta = ((IGregTechTileEntity) tile).getMetaTileEntity();
            if (meta instanceof MTEHatch) return "gt_hatch:" + meta.getClass().getName();
            if (meta instanceof MTEMultiBlockBase) return "gt_controller:" + meta.getClass().getName()
                + ":formed=" + ((MTEMultiBlockBase) meta).mMachine;
        }
        if (tile instanceof IInventory) return "inventory:" + tile.getClass().getName();
        if (tile instanceof IFluidHandler) return "fluid_handler:" + tile.getClass().getName();
        if (RebuildConfig.protectTileEntities) return "all_tile_entities:" + tile.getClass().getName();
        return null;
    }
}
