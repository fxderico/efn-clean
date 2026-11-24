package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.efn.client.module.impl.render.character.CharacterModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntityRenderer.class})
public abstract class MixinCharacterLivingEntityRenderer {

    public MixinCharacterLivingEntityRenderer() {}

    @Inject(
            method = {
                "getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"
            },
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$hideModelUnderCharacter(
            LivingEntityRenderState livingEntityRenderState,
            boolean bl,
            boolean bl2,
            boolean bl3,
            CallbackInfoReturnable<RenderLayer> cir) {
        if (CharacterModule.hidesModel(livingEntityRenderState)) {
            cir.setReturnValue(null);
        }
    }

    @WrapOperation(
            method = {
                "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/EntityRenderState;FF)V")
            })
    private void efn$hideLayersUnderCharacter(
            FeatureRenderer<?, ?> featureRenderer,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            EntityRenderState entityRenderState,
            float f,
            float g,
            Operation<Void> operation) {
        if (!CharacterModule.hidesLayer(featureRenderer, entityRenderState)) {
            operation.call(
                    featureRenderer,
                    matrices,
                    orderedRenderCommandQueue,
                    i,
                    entityRenderState,
                    f,
                    g);
        }
    }
}
