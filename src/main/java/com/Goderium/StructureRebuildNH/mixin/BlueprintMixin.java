package com.Goderium.StructureRebuildNH.mixin;

import com.Goderium.StructureRebuildNH.common.RebuildEngine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ConstructableUtility.class, remap = false)
public abstract class BlueprintMixin {
    @Redirect(method = "handle0", at = @At(value = "INVOKE",
        target = "Lcom/gtnewhorizon/structurelib/alignment/constructable/IConstructable;construct(Lnet/minecraft/item/ItemStack;Z)V"),
        require = 2, allow = 2, remap = false)
    private static void rebuild$construct(IConstructable machine, ItemStack stack, boolean hints,
        ItemStack original, EntityPlayer player, World world, int x, int y, int z, int side) {
        if (hints) machine.construct(stack, true);
        else RebuildEngine.construct(machine, stack, player, world, x, y, z);
    }
}
