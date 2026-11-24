package com.example.client.mixin;

import net.efn.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.FallingBlockEntityRenderer;
import net.minecraft.entity.FallingBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({FallingBlockEntityRenderer.class})
public abstract class MixinFallingBlockRenderer {

    public MixinFallingBlockRenderer() {}

    @Inject(
            method = {
                "shouldRender(Lnet/minecraft/entity/FallingBlockEntity;Lnet/minecraft/client/render/Frustum;DDD)Z"
            },
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$cancelFallingBlock(
            FallingBlockEntity fallingBlockEntity,
            Frustum frustum,
            double d,
            double e,
            double d2,
            CallbackInfoReturnable<Boolean> cir) {
        if (NoRenderModule.hideFallingBlocks()) {
            cir.setReturnValue(false);
        }
    }
}
