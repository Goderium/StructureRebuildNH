package com.Goderium.StructureRebuildNH.mixin;

import com.Goderium.StructureRebuildNH.common.FormationTracker;
import com.Goderium.StructureRebuildNH.common.WorldHooks;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class MachineCheckMixin {
    @WrapMethod(method = "checkStructure", remap = false)
    private boolean rebuild$check(boolean force, IGregTechTileEntity tile, Operation<Boolean> original) {
        if (!tile.isServerSide()) return original.call(force, tile);
        MTEMultiBlockBase owner = (MTEMultiBlockBase) (Object) this;
        FormationTracker.Record previous = FormationTracker.CHECK.get();
        WorldHooks.ReadObserver previousRead = WorldHooks.READ.get();
        FormationTracker.Record record = new FormationTracker.Record(owner);
        FormationTracker.CHECK.set(record); WorldHooks.READ.set(record);
        boolean completed = false;
        try {
            boolean result = original.call(force, tile);
            completed = true;
            return result;
        } finally {
            FormationTracker.finish(owner, record, completed);
            if (previous == null) FormationTracker.CHECK.remove(); else FormationTracker.CHECK.set(previous);
            if (previousRead == null) WorldHooks.READ.remove(); else WorldHooks.READ.set(previousRead);
        }
    }
}
