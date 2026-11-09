package com.Goderium.StructureRebuildNH.mixin;

import com.Goderium.StructureRebuildNH.common.FormationTracker;
import com.Goderium.StructureRebuildNH.common.RebuildEngine;
import com.Goderium.StructureRebuildNH.common.ShapeRegistry;
import net.minecraft.world.World;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureWalker;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = StructureUtility.class, remap = false)
public abstract class StructureIterationMixin {
    @WrapMethod(method = "iterateV2", remap = false)
    private static <T> boolean rebuild$iterate(IStructureElement<T>[] elements, World world, ExtendedFacing facing,
        int x, int y, int z, int a, int b, int c, IStructureWalker<T> walker, String type, Operation<Boolean> original) {
        RebuildEngine.Session session = RebuildEngine.ACTIVE.get();
        if (session != null && session.planning() && "placeBlock".equals(type)) {
            session.defer(ShapeRegistry.bounds(elements, facing, x, y, z, a, b, c),
                () -> original.call(elements, world, facing, x, y, z, a, b, c, walker, type));
            return true;
        }
        FormationTracker.Record record = FormationTracker.CHECK.get();
        if (record == null) return original.call(elements, world, facing, x, y, z, a, b, c, walker, type);
        // Unknown/custom definitions still have their actual native read footprint recorded by WorldMixin.
        try { record.add(ShapeRegistry.bounds(elements, facing, x, y, z, a, b, c)); }
        catch (IllegalStateException unknown) { return original.call(elements, world, facing, x, y, z, a, b, c, walker, type); }
        record.structureDepth++;
        try { return original.call(elements, world, facing, x, y, z, a, b, c, walker, type); }
        finally { record.structureDepth--; }
    }
}
