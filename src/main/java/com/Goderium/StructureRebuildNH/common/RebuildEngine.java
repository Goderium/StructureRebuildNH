package com.Goderium.StructureRebuildNH.common;

import com.Goderium.StructureRebuildNH.StructureRebuildNH;
import com.Goderium.StructureRebuildNH.config.RebuildConfig;
import com.Goderium.StructureRebuildNH.util.Bounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;

/** One native construct invocation records deferred placements; all selected volumes clear before replay. */
public final class RebuildEngine {
    public static final String REBUILD_CHANNEL = "rebuild";
    public static final ThreadLocal<Session> ACTIVE = new ThreadLocal<Session>();
    public static final class Session implements WorldHooks.WriteGuard {
        final World world;
        final TileEntity target;
        final LinkedHashSet<Bounds> regions = new LinkedHashSet<Bounds>();
        final List<Runnable> placements = new ArrayList<Runnable>();
        List<Bounds> formed = Collections.emptyList();
        Bounds legacy;
        boolean planning = true, internalWrite, clearing;
        long cleared, skipped;
        Session(World world, TileEntity target) { this.world = world; this.target = target; }
        public boolean planning() { return planning; }
        public void defer(Bounds bounds, Runnable placement) {
            if (placements.size() >= RebuildConfig.maxVolume) throw new IllegalStateException("Too many placement operations");
            regions.add(bounds); placements.add(placement);
        }
        public Boolean setBlock(final World w, final int x, final int y, final int z, final Block block, final int meta, final int flags) {
            if (internalWrite || w != world) return null;
            if (planning) {
                Bounds point = Bounds.point(x, y, z);
                legacy = legacy == null ? point : legacy.union(point);
                if (placements.size() >= RebuildConfig.maxVolume) throw new IllegalStateException("Too many placement operations");
                placements.add(() -> w.setBlock(x, y, z, block, meta, flags));
                return true;
            }
            if (protect(w, x, y, z)) { skipped++; return false; }
            return null;
        }
        public boolean protect(World w, int x, int y, int z) {
            if (internalWrite || w != world) return false;
            if (planning) throw new IllegalStateException("Unsupported direct tile/metadata mutation during blueprint planning");
            return Protection.protects(w, x, y, z, target, formed);
        }
        void execute() {
            if (legacy != null) regions.add(legacy);
            if (regions.isEmpty()) throw new IllegalStateException("Blueprint selected no buildable structure");
            long volume = 0;
            for (Bounds b : regions) {
                volume = Math.addExact(volume, b.volume());
                if (volume > RebuildConfig.maxVolume) throw new IllegalStateException("Selected volume exceeds maxVisitedBlocks=" + RebuildConfig.maxVolume);
                if (b.minY < 0 || b.maxY >= world.getHeight()) throw new IllegalStateException("Structure extends outside world height");
                for (int cx = b.minX >> 4; cx <= b.maxX >> 4; cx++) for (int cz = b.minZ >> 4; cz <= b.maxZ >> 4; cz++) {
                    if (!world.getChunkProvider().chunkExists(cx, cz)) throw new IllegalStateException("Structure reaches an unloaded chunk");
                }
            }
            formed = FormationTracker.protectedVolumes(world, target, new ArrayList<Bounds>(regions));
            planning = false;
            clearing = true;
            // All protection decisions are evaluated before writes, then preserved even if native callbacks alter the world.
            List<long[]> toClear = new ArrayList<long[]>();
            for (Bounds b : regions) {
                for (int x = b.minX; x <= b.maxX; x++) for (int y = b.minY; y <= b.maxY; y++) for (int z = b.minZ; z <= b.maxZ; z++) {
                    if (world.isAirBlock(x, y, z)) continue;
                    if (Protection.protects(world, x, y, z, target, formed)) { skipped++; continue; }
                    toClear.add(new long[] { x, y, z });
                }
            }
            for (long[] p : toClear) {
                int x = (int) p[0], y = (int) p[1], z = (int) p[2];
                if (world.isAirBlock(x, y, z) || Protection.protects(world, x, y, z, target, formed)) continue;
                if (!world.setBlock(x, y, z, Blocks.air, 0, 2)) throw new IllegalStateException("Could not clear " + x + "," + y + "," + z);
                cleared++;
            }
            clearing = false;
            // Whitelist applies ONLY to pre-clearing. Native placement may replace obsolete module positions.
            WorldHooks.WRITE.remove();
            for (Runnable placement : placements) placement.run();
        }
    }
    public static void construct(IConstructable machine, ItemStack stack, EntityPlayer player, World world, int x, int y, int z) {
        // Check presence first: StructureLib otherwise falls back to the blueprint stack size.
        if (!RebuildConfig.enabled || stack == null
            || !ChannelDataAccessor.hasSubChannel(stack, REBUILD_CHANNEL)
            || ChannelDataAccessor.getChannelData(stack, REBUILD_CHANNEL) != 1
            || world.isRemote || !player.capabilities.isCreativeMode
            || !player.isSneaking() || RebuildConfig.excludedControllers.contains(machine.getClass().getName().toLowerCase(Locale.ROOT))) {
            machine.construct(stack, false); return;
        }
        if (ACTIVE.get() != null) throw new IllegalStateException("Nested blueprint rebuild");
        Session session = new Session(world, world.getTileEntity(x, y, z));
        WorldHooks.WriteGuard previous = WorldHooks.WRITE.get();
        ACTIVE.set(session); WorldHooks.WRITE.set(session);
        try {
            machine.construct(stack, false);
            session.execute();
            player.addChatMessage(new ChatComponentText("[StructureRebuildNH] Cleared " + session.cleared + ", protected "
                + session.skipped + "; native placement completed. Check structure formation."));
            StructureRebuildNH.LOG.info("Rebuilt {} at {},{},{}: cleared={}, protected={}, pieces={}",
                machine.getClass().getName(), x, y, z, session.cleared, session.skipped, session.regions.size());
        } catch (RuntimeException error) {
            player.addChatMessage(new ChatComponentText("[StructureRebuildNH] Stopped: " + error.getMessage()
                + (session.planning ? " (nothing cleared)" : " (partial changes retained; no automatic retry)")));
            StructureRebuildNH.LOG.warn("Blueprint rebuild stopped", error);
        } finally {
            ACTIVE.remove();
            if (previous == null) WorldHooks.WRITE.remove(); else WorldHooks.WRITE.set(previous);
        }
    }
}
