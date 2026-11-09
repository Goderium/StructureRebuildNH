package com.Goderium.StructureRebuildNH.loader;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public class LateLoader implements ILateMixinLoader {
    public String getMixinConfig() { return "mixins.structurerebuildnh.late.json"; }
    public List<String> getMixins(Set<String> mods) {
        return Arrays.asList("BlueprintMixin", "ShapeBuilderMixin", "StructureIterationMixin", "MachineCheckMixin");
    }
}
