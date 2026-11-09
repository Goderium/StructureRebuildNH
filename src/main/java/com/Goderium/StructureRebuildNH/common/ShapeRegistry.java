package com.Goderium.StructureRebuildNH.common;

import com.Goderium.StructureRebuildNH.StructureRebuildNH;
import com.Goderium.StructureRebuildNH.config.RebuildConfig;
import com.Goderium.StructureRebuildNH.util.Bounds;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureElement;

public final class ShapeRegistry {
    private static final Map<IStructureElement<?>[], int[]> SIZES =
        Collections.synchronizedMap(new WeakHashMap<IStructureElement<?>[], int[]>());
    public static void put(IStructureElement<?>[] elements, int[] size) { SIZES.put(elements, size.clone()); }
    public static Bounds bounds(IStructureElement<?>[] elements, ExtendedFacing facing,
        int x, int y, int z, int a, int b, int c) {
        int[] size = SIZES.get(elements);
        if (size == null || size[0] < 1 || size[1] < 1 || size[2] < 1) {
            throw new IllegalStateException("No complete registered shape dimensions; rebuild cancelled");
        }
        Bounds result = null;
        for (int corner = 0; corner < 8; corner++) {
            int[] abc = { ((corner & 1) == 0 ? 0 : size[0] - 1) - a,
                ((corner & 2) == 0 ? 0 : size[1] - 1) - b, ((corner & 4) == 0 ? 0 : size[2] - 1) - c };
            int[] xyz = new int[3];
            facing.getWorldOffset(abc, xyz);
            Bounds point = Bounds.point(Math.addExact(x, xyz[0]), Math.addExact(y, xyz[1]), Math.addExact(z, xyz[2]));
            result = result == null ? point : result.union(point);
        }
        return result;
    }
}
