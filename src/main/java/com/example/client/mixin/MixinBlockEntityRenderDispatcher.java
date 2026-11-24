package com.example.client.mixin;

import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({BlockEntityRenderManager.class})
public abstract class MixinBlockEntityRenderDispatcher {

    public MixinBlockEntityRenderDispatcher() {}

    @Inject(
            method = {
                "getRenderState(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)Lnet/minecraft/client/render/block/entity/state/BlockEntityRenderState;"
            },
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$xraySkipHiddenBlockEntity(
            BlockEntity blockEntity,
            float f,
            CrumblingOverlayCommand crumblingOverlayCommand,
            CallbackInfoReturnable<BlockEntityRenderState> cir) {
        XrayModule.Snapshot active = XrayModule.active;
        if (active != null) {
            World world = blockEntity.getWorld();
            if (world != null) {
                BlockPos pos = blockEntity.getPos();
                BlockState cachedState = blockEntity.getCachedState();
                if (active.alphaFor(world, cachedState, pos) <= 0.0F) {
                    cir.setReturnValue(null);
                }
            }
        }
    }
}
