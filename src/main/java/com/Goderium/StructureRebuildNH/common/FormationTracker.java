package com.Goderium.StructureRebuildNH.common;

import com.Goderium.StructureRebuildNH.util.Bounds;
import com.Goderium.StructureRebuildNH.util.RebuildLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

/** Remembers geometry observed during real native structure checks; never triggers checkMachine to discover it. */
public final class FormationTracker {
    private static final Map<MTEMultiBlockBase, Record> RECORDS = Collections.synchronizedMap(new WeakHashMap<MTEMultiBlockBase, Record>());
    public static final ThreadLocal<Record> CHECK = new ThreadLocal<Record>();
    public static final class Record implements WorldHooks.ReadObserver {
        final WeakReference<World> world;
        final int x, y, z;
        final ExtendedFacing facing;
        final List<Bounds> pieces = new ArrayList<Bounds>();
        Bounds legacy;
        public int structureDepth;
        public Record(MTEMultiBlockBase owner) {
            IGregTechTileEntity tile = owner.getBaseMetaTileEntity();
            world = new WeakReference<World>(tile.getWorld());
            x = tile.getXCoord(); y = tile.getYCoord(); z = tile.getZCoord();
            facing = orientation(owner);
        }
        public void read(World w, int px, int py, int pz) {
            if (w != world.get() || structureDepth != 0) return;
            Bounds point = Bounds.point(px, py, pz);
            legacy = legacy == null ? point : legacy.union(point);
        }
        public void add(Bounds bounds) { if (!pieces.contains(bounds)) pieces.add(bounds); }
    }
    private static ExtendedFacing orientation(MTEMultiBlockBase owner) {
        return owner instanceof IAlignment ? ((IAlignment) owner).getExtendedFacing()
            : ExtendedFacing.of(owner.getBaseMetaTileEntity().getFrontFacing());
    }
    public static void finish(MTEMultiBlockBase owner, Record record, boolean success) {
        if (!success || !owner.mMachine) { RECORDS.remove(owner); return; }
        if (record.legacy != null) record.add(record.legacy);
        if (!record.pieces.isEmpty()) RECORDS.put(owner, record);
    }
    public static List<Bounds> protectedVolumes(World world, TileEntity target, List<Bounds> requested, RebuildLog log) {
        List<Bounds> result = new ArrayList<Bounds>();
        int controllers = 0;
        for (Object object : new ArrayList<Object>(world.loadedTileEntityList)) {
            if (!(object instanceof IGregTechTileEntity) || object == target) continue;
            IGregTechTileEntity tile = (IGregTechTileEntity) object;
            if (tile.getWorld() != world || ((TileEntity) object).isInvalid()
                || !(tile.getMetaTileEntity() instanceof MTEMultiBlockBase)) continue;
            MTEMultiBlockBase owner = (MTEMultiBlockBase) tile.getMetaTileEntity();
            controllers++;
            String identity = owner.getClass().getName() + " at=" + tile.getXCoord() + "," + tile.getYCoord() + "," + tile.getZCoord();
            log.line("GT_MACHINE " + identity + " formed=" + owner.mMachine + " facing=" + orientation(owner));
            if (!owner.mMachine) {
                log.line("GT_STRUCTURE_NOT_PROTECTED " + identity + " reason=not_formed; controller_block_remains_protected");
                continue;
            }
            Record record = RECORDS.get(owner);
            if (record == null || record.world.get() != world || record.x != tile.getXCoord() || record.y != tile.getYCoord()
                || record.z != tile.getZCoord() || record.facing != orientation(owner)) {
                throw new IllegalStateException("Formed GT machine at " + tile.getXCoord() + "," + tile.getYCoord()
                    + "," + tile.getZCoord() + " has no current geometry; wait for its native structure recheck");
            }
            log.line("GT_GEOMETRY " + identity + " pieces=" + record.pieces.size());
            int before = result.size();
            for (Bounds b : record.pieces) for (Bounds selected : requested) if (b.intersects(selected)) {
                log.line("GT_STRUCTURE_PROTECTED " + identity + " bounds=" + b);
                result.add(b); break;
            }
            if (result.size() == before) log.line("GT_STRUCTURE_OUTSIDE " + identity + " reason=no_intersection");
        }
        log.checkpoint("GT_SCAN_END otherControllers=" + controllers + " protectedRegions=" + result.size());
        return result;
    }
}
