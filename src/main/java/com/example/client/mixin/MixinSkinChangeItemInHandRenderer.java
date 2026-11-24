package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.SkinChangeModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinSkinChangeItemInHandRenderer {

    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private EntityRenderManager entityRenderDispatcher;

    public MixinSkinChangeItemInHandRenderer() {}

    @WrapOperation(
            method = {
                "renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IFFLnet/minecraft/util/Arm;)V",
                "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Arm;)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V"),
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V")
            })
    private void efn$skinChangeArm(
            PlayerEntityRenderer<?> playerEntityRenderer,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            Identifier id,
            boolean bl,
            Operation<Void> operation) {
        SkinChangeModule module = ModuleRegistry.get(SkinChangeModule.class);
        ClientPlayerEntity player = this.client.player;
        if (module != null && module.isEnabled() && player != null) {
            SkinTextures skin = player.getSkin();
            SkinTextures skinTextures = module.skinFor(player.getId(), skin);
            if (skinTextures != skin) {
                id = skinTextures.body().texturePath();
                PlayerEntityRenderer efn$skinChangePlayerRenderer =
                        ((AccessorSkinChangeEntityRenderDispatcher) this.entityRenderDispatcher)
                                .efn$skinChangePlayerRenderers()
                                .get(skinTextures.model());
                if (efn$skinChangePlayerRenderer != null) {
                    playerEntityRenderer = efn$skinChangePlayerRenderer;
                }
            }
        }
        operation.call(playerEntityRenderer, matrices, orderedRenderCommandQueue, i, id, bl);
    }
}
