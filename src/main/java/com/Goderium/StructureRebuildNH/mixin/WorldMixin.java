package com.Goderium.StructureRebuildNH.mixin;

import com.Goderium.StructureRebuildNH.common.WorldHooks;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = World.class, remap = false)
public abstract class WorldMixin {
    @Inject(method = {"setBlock(IIILnet/minecraft/block/Block;II)Z", "func_147465_d(IIILnet/minecraft/block/Block;II)Z"},
        at = @At("HEAD"), cancellable = true, require = 1, allow = 1, remap = false)
    private void rebuild$set(int x, int y, int z, Block block, int meta, int flags, CallbackInfoReturnable<Boolean> cir) {
        WorldHooks.WriteGuard guard = WorldHooks.WRITE.get();
        if (guard == null) return;
        Boolean result = guard.setBlock((World) (Object) this, x, y, z, block, meta, flags);
        if (result != null) cir.setReturnValue(result);
    }
    @Inject(method = {"getBlock(III)Lnet/minecraft/block/Block;", "func_147439_a(III)Lnet/minecraft/block/Block;"},
        at = @At("HEAD"), require = 1, allow = 1, remap = false)
    private void rebuild$read(int x, int y, int z, CallbackInfoReturnable<Block> cir) {
        WorldHooks.ReadObserver observer = WorldHooks.READ.get();
        if (observer != null) observer.read((World) (Object) this, x, y, z);
    }
    @Inject(method = {"getTileEntity(III)Lnet/minecraft/tileentity/TileEntity;", "func_147438_o(III)Lnet/minecraft/tileentity/TileEntity;"},
        at = @At("HEAD"), require = 1, allow = 1, remap = false)
    private void rebuild$readTile(int x, int y, int z, CallbackInfoReturnable<TileEntity> cir) {
        WorldHooks.ReadObserver observer = WorldHooks.READ.get();
        if (observer != null) observer.read((World) (Object) this, x, y, z);
    }
    @Inject(method = {"setBlockMetadataWithNotify(IIIII)Z", "func_72921_c(IIIII)Z"},
        at = @At("HEAD"), cancellable = true, require = 1, allow = 1, remap = false)
    private void rebuild$meta(int x, int y, int z, int meta, int flags, CallbackInfoReturnable<Boolean> cir) {
        WorldHooks.WriteGuard guard = WorldHooks.WRITE.get();
        if (guard != null && guard.protect((World) (Object) this, x, y, z)) cir.setReturnValue(false);
    }
    @Inject(method = {"removeTileEntity(III)V", "func_147475_p(III)V"},
        at = @At("HEAD"), cancellable = true, require = 1, allow = 1, remap = false)
    private void rebuild$remove(int x, int y, int z, CallbackInfo ci) {
        WorldHooks.WriteGuard guard = WorldHooks.WRITE.get();
        if (guard != null && guard.protect((World) (Object) this, x, y, z)) ci.cancel();
    }
    @Inject(method = {"setTileEntity(IIILnet/minecraft/tileentity/TileEntity;)V", "func_147455_a(IIILnet/minecraft/tileentity/TileEntity;)V"},
        at = @At("HEAD"), cancellable = true, require = 1, allow = 1, remap = false)
    private void rebuild$tile(int x, int y, int z, TileEntity tile, CallbackInfo ci) {
        WorldHooks.WriteGuard guard = WorldHooks.WRITE.get();
        if (guard != null && guard.protect((World) (Object) this, x, y, z)) ci.cancel();
    }
}
