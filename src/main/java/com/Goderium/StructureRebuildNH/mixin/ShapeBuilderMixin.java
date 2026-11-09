package com.Goderium.StructureRebuildNH.mixin;

import java.util.HashMap;
import java.util.Map;
import com.Goderium.StructureRebuildNH.common.ShapeRegistry;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StructureDefinition.Builder.class, remap = false)
public abstract class ShapeBuilderMixin {
    @Unique private final Map<String, int[]> rebuild$sizes = new HashMap<String, int[]>();
    @Inject(method = "addShape", at = @At("HEAD"), require = 1, remap = false)
    private void rebuild$shape(String name, String[][] shape, CallbackInfoReturnable<StructureDefinition.Builder<?>> cir) {
        int a = 0, b = 0;
        for (String[] slice : shape) {
            b = Math.max(b, slice.length);
            for (String row : slice) a = Math.max(a, row.length());
        }
        rebuild$sizes.put(name, new int[] { a, b, shape.length });
    }
    @Inject(method = "build", at = @At("RETURN"), require = 1, remap = false)
    private void rebuild$built(CallbackInfoReturnable<IStructureDefinition<?>> cir) {
        for (Map.Entry<String, int[]> entry : rebuild$sizes.entrySet()) {
            ShapeRegistry.put(cir.getReturnValue().getStructureFor(entry.getKey()), entry.getValue());
        }
    }
}
