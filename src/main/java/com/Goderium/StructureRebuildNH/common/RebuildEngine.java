package com.Goderium.StructureRebuildNH.common;

import com.Goderium.StructureRebuildNH.StructureRebuildNH;
import com.Goderium.StructureRebuildNH.config.RebuildConfig;
import com.Goderium.StructureRebuildNH.util.Bounds;
import com.Goderium.StructureRebuildNH.util.RebuildLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    public static final class Session implements WorldHooks.WriteGuard, WorldHooks.WriteObserver {
        final World world;
        final TileEntity target;
        final LinkedHashSet<Bounds> regions = new LinkedHashSet<Bounds>();
        final List<Runnable> placements = new ArrayList<Runnable>();
        final RebuildLog log = new RebuildLog(RebuildConfig.logDirectory, RebuildConfig.writeDebugLogs);
        final Map<String, Long> protectionCounts = new LinkedHashMap<String, Long>();
        final boolean detailed = RebuildConfig.logBlockDecisions;
        List<Bounds> formed = Collections.emptyList();
        Bounds legacy;
        boolean planning = true, internalWrite, clearing;
        long cleared, skipped, visited, air, placementWrites, placementWritesSucceeded;
        String cursor = "none";
        Session(World world, TileEntity target) { this.world = world; this.target = target; }
        public boolean planning() { return planning; }
        public void defer(Bounds bounds, Runnable placement) {
            if (placements.size() >= RebuildConfig.maxVolume) throw new IllegalStateException("Too many placement operations");
            regions.add(bounds); placements.add(placement);
            log.line("PLAN_PIECE index=" + placements.size() + " bounds=" + bounds + " volume=" + bounds.volume());
        }
        public Boolean setBlock(final World w, final int x, final int y, final int z, final Block block, final int meta, final int flags) {
            if (internalWrite || w != world) return null;
            if (planning) {
                Bounds point = Bounds.point(x, y, z);
                legacy = legacy == null ? point : legacy.union(point);
                if (placements.size() >= RebuildConfig.maxVolume) throw new IllegalStateException("Too many placement operations");
                placements.add(() -> w.setBlock(x, y, z, block, meta, flags));
                if (details()) log.line("PLAN_DIRECT at=" + position(x, y, z) + " block=" + blockName(block, meta) + " flags=" + flags);
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
        private boolean details() { return detailed && log.enabled(); }
        private String at(int x, int y, int z) {
            return "at=" + position(x, y, z) + " block=" + blockName(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
        }
        private void countProtection(String reason) {
            int colon = reason.indexOf(':');
            String category = colon < 0 ? reason : reason.substring(0, colon);
            Long count = protectionCounts.get(category);
            protectionCounts.put(category, count == null ? 1L : count + 1);
        }
        public void before(World w, int x, int y, int z, Block block, int meta, int flags) {
            if (planning || clearing || w != world) return;
            cursor = "placement at=" + position(x, y, z);
            placementWrites++;
            if (details()) log.line("PLACE_WRITE " + at(x, y, z) + " requested=" + blockName(block, meta) + " flags=" + flags);
        }
        public void after(World w, int x, int y, int z, boolean success) {
            if (planning || clearing || w != world) return;
            if (success) placementWritesSucceeded++;
            if (details()) log.line("PLACE_RESULT " + at(x, y, z) + " success=" + success);
        }
        void execute() {
            if (legacy != null) regions.add(legacy);
            if (regions.isEmpty()) throw new IllegalStateException("Blueprint selected no buildable structure");
            Bounds bounds = Bounds.enclosing(regions);
            long volume = bounds.volume();
            log.checkpoint("PLAN_END uniquePieces=" + regions.size() + " operations=" + placements.size()
                + " bounds=" + bounds + " volume=" + volume + " limit=" + RebuildConfig.maxVolume);
            if (volume > RebuildConfig.maxVolume) throw new IllegalStateException("Selected cuboid volume=" + volume
                + " exceeds maxVisitedBlocks=" + RebuildConfig.maxVolume);
            if (bounds.minY < 0 || bounds.maxY >= world.getHeight()) throw new IllegalStateException("Structure extends outside world height");
            for (int cx = bounds.minX >> 4; cx <= bounds.maxX >> 4; cx++) for (int cz = bounds.minZ >> 4; cz <= bounds.maxZ >> 4; cz++) {
                if (!world.getChunkProvider().chunkExists(cx, cz)) throw new IllegalStateException("Structure reaches unloaded chunk " + cx + "," + cz);
            }
            log.line("CHUNKS_VALIDATED x=" + (bounds.minX >> 4) + ".." + (bounds.maxX >> 4)
                + " z=" + (bounds.minZ >> 4) + ".." + (bounds.maxZ >> 4));
            formed = FormationTracker.protectedVolumes(world, target, Collections.singletonList(bounds), log);
            planning = false;
            clearing = true;
            log.checkpoint("CLEAR_SCAN_BEGIN");
            // All protection decisions are evaluated before writes, then preserved even if native callbacks alter the world.
            List<long[]> toClear = new ArrayList<long[]>();
            for (int x = bounds.minX; x <= bounds.maxX; x++) for (int y = bounds.minY; y <= bounds.maxY; y++) for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
                // Modded air-like blocks are still structure leftovers; skip only actual vanilla air.
                visited++;
                if (world.getBlock(x, y, z) == Blocks.air) { air++; continue; }
                cursor = "clear_scan at=" + position(x, y, z);
                String reason = Protection.reason(world, x, y, z, target, formed);
                if (reason != null) {
                    skipped++; countProtection(reason);
                    if (details()) log.line("KEEP " + at(x, y, z) + " reason=" + reason);
                    continue;
                }
                if (details()) log.line("CLEAR_CANDIDATE " + at(x, y, z));
                toClear.add(new long[] { x, y, z });
            }
            log.checkpoint("CLEAR_SCAN_END visited=" + visited + " air=" + air + " candidates=" + toClear.size()
                + " protected=" + skipped + " protectionCounts=" + protectionCounts);
            for (long[] p : toClear) {
                int x = (int) p[0], y = (int) p[1], z = (int) p[2];
                cursor = "clearing at=" + position(x, y, z);
                if (world.getBlock(x, y, z) == Blocks.air) {
                    if (details()) log.line("CLEAR_SKIPPED at=" + position(x, y, z) + " reason=already_air_after_callback");
                    continue;
                }
                String reason = Protection.reason(world, x, y, z, target, formed);
                if (reason != null) {
                    log.line("CLEAR_SKIPPED " + at(x, y, z) + " reason=" + reason);
                    continue;
                }
                if (!world.setBlock(x, y, z, Blocks.air, 0, 2)) throw new IllegalStateException("Could not clear " + x + "," + y + "," + z);
                cleared++;
                if (details()) log.line("CLEAR_RESULT " + at(x, y, z) + " success=true");
            }
            clearing = false;
            log.checkpoint("CLEAR_END cleared=" + cleared + " protected=" + skipped);
            // Whitelist applies ONLY to pre-clearing. Native placement may replace obsolete module positions.
            WorldHooks.WRITE.remove();
            log.checkpoint("PLACE_BEGIN nativePlacement=true preclearProtectionApplied=false");
            int operation = 0;
            for (Runnable placement : placements) {
                log.line("PLACE_OPERATION_BEGIN index=" + (++operation));
                placement.run();
                log.line("PLACE_OPERATION_END index=" + operation);
            }
            log.checkpoint("PLACE_END writes=" + placementWrites + " succeeded=" + placementWritesSucceeded);
        }
    }
    private static String position(int x, int y, int z) { return x + "," + y + "," + z; }
    private static String blockName(Block block, int meta) { return Block.blockRegistry.getNameForObject(block) + "@" + meta; }
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
        WorldHooks.WriteObserver previousTrace = WorldHooks.TRACE.get();
        ACTIVE.set(session); WorldHooks.WRITE.set(session);
        WorldHooks.TRACE.set(session);
        boolean completed = false;
        try {
            session.log.checkpoint("BEGIN machine=" + machine.getClass().getName() + " dimension=" + world.provider.dimensionId
                + " target=" + position(x, y, z) + " player=" + player.getCommandSenderName()
                + " creative=" + player.capabilities.isCreativeMode + " sneaking=" + player.isSneaking()
                + " blueprintCount=" + stack.stackSize + " blueprintTag=" + stack.getTagCompound());
            session.log.line("CONFIG maxVisitedBlocks=" + RebuildConfig.maxVolume + " protectAllTileEntities="
                + RebuildConfig.protectTileEntities + " logBlockDecisions=" + RebuildConfig.logBlockDecisions);
            session.log.line("MOD_WHITELIST " + RebuildConfig.protectedMods);
            session.log.line("BLOCK_WHITELIST " + RebuildConfig.protectedBlocks);
            StructureRebuildNH.LOG.info("Rebuild begin {} at {},{},{}; trace={}", machine.getClass().getName(), x, y, z, session.log.path());
            machine.construct(stack, false);
            session.execute();
            completed = true;
            player.addChatMessage(new ChatComponentText("[StructureRebuildNH] Cleared " + session.cleared + ", protected "
                + session.skipped + "; native placement completed. Check structure formation."));
            StructureRebuildNH.LOG.info("Rebuilt {} at {},{},{}: cleared={}, protected={}, pieces={}",
                machine.getClass().getName(), x, y, z, session.cleared, session.skipped, session.regions.size());
        } catch (RuntimeException error) {
            session.log.error(error);
            player.addChatMessage(new ChatComponentText("[StructureRebuildNH] Stopped: " + error.getMessage()
                + (session.planning ? " (nothing cleared)" : " (partial changes retained; no automatic retry)")));
            StructureRebuildNH.LOG.warn("Blueprint rebuild stopped", error);
        } catch (Error error) {
            session.log.error(error);
            throw error;
        } finally {
            ACTIVE.remove();
            if (previous == null) WorldHooks.WRITE.remove(); else WorldHooks.WRITE.set(previous);
            if (previousTrace == null) WorldHooks.TRACE.remove(); else WorldHooks.TRACE.set(previousTrace);
            session.log.checkpoint("END completed=" + completed + " phase=" + (session.planning ? "planning" : session.clearing ? "clearing" : "placement")
                + " cleared=" + session.cleared + " protected=" + session.skipped + " lastOperation=" + session.cursor
                + " elapsedMs=" + session.log.elapsedMillis());
            session.log.close();
            if (session.log.failure() != null) StructureRebuildNH.LOG.warn("Rebuild trace I/O failure: {}", session.log.failure());
        }
    }
}
