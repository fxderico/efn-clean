package com.example.client.mixin;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.SkinChangeModule;
import net.efn.client.module.impl.render.NametagsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntityRenderer.class})
public abstract class MixinPlayerRenderer {

    public MixinPlayerRenderer() {}

    @Inject(
            method = {
                "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V"
            },
            at = {@At("TAIL")})
    private void efn$injectSkin(
            PlayerLikeEntity playerLikeEntity,
            PlayerEntityRenderState playerEntityRenderState,
            float f,
            CallbackInfo ci) {
        SkinChangeModule module = ModuleRegistry.get(SkinChangeModule.class);
        if (module != null && module.isEnabled()) {
            playerEntityRenderState.skinTextures =
                    module.skinFor(
                            playerEntityRenderState.id, playerEntityRenderState.skinTextures);
        }
    }

    @Inject(
            method = {
                "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"
            },
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$suppressVanillaNameTag(
            PlayerEntityRenderState playerEntityRenderState,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            CameraRenderState cameraRenderState,
            CallbackInfo ci) {
        if (NametagsModule.hideVanillaNametags()) {
            ci.cancel();
        }
    }
}
