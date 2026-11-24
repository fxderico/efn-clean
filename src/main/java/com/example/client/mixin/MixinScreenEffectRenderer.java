package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.efn.client.module.impl.movement.BoatNoclipModule;
import net.efn.client.module.impl.render.CustomFovModule;
import net.efn.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({InGameOverlayRenderer.class})
public abstract class MixinScreenEffectRenderer {

    public MixinScreenEffectRenderer() {}

    @ModifyExpressionValue(
            method = {
                "renderOverlays(ZFLnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V"
            },
            at = {@At(value = "NEW", target = "()Lnet/minecraft/client/util/math/MatrixStack;")})
    private MatrixStack efn$undoCustomFov(MatrixStack matrices) {
        float screenEffectScale = CustomFovModule.screenEffectScale();
        if (screenEffectScale != 1.0F) {
            matrices.peek().getPositionMatrix().scale(screenEffectScale, screenEffectScale, 1.0F);
        }
        return matrices;
    }

    @Inject(
            method = {
                "renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/texture/Sprite;)V"
            },
            at = {@At("HEAD")},
            cancellable = true)
    private static void efn$cancelFireOverlay(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumerProvider,
            Sprite sprite,
            CallbackInfo ci) {
        if (NoRenderModule.hideFireOverlay()) {
            ci.cancel();
        }
    }

    @Inject(
            method = {
                "getInWallBlockState(Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/block/BlockState;"
            },
            at = {@At("HEAD")},
            cancellable = true)
    private static void efn$noWallOverlayInNoclipBoat(
            PlayerEntity player, CallbackInfoReturnable<BlockState> cir) {
        if (BoatNoclipModule.hidesWallOverlay(player)) {
            cir.setReturnValue(null);
        }
    }
}
