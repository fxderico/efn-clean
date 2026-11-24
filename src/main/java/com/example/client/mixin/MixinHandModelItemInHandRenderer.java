package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.efn.client.module.impl.render.HandModelModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinHandModelItemInHandRenderer {

    public MixinHandModelItemInHandRenderer() {}

    @WrapOperation(
            method = {
                "renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IFFLnet/minecraft/util/Arm;)V",
                "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Arm;)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V")
            })
    private void efn$longRightArm(
            PlayerEntityRenderer<?> playerEntityRenderer,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            Identifier id,
            boolean bl,
            Operation<Void> operation) {
        operation.call(playerEntityRenderer, matrices, orderedRenderCommandQueue, i, id, bl);
        if (HandModelModule.active != null) {
            matrices.push();
            HandModelModule.shoulder(matrices, true, playerEntityRenderer);
            operation.call(playerEntityRenderer, matrices, orderedRenderCommandQueue, i, id, bl);
            matrices.pop();
        }
    }

    @WrapOperation(
            method = {
                "renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IFFLnet/minecraft/util/Arm;)V",
                "renderArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Arm;)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V")
            })
    private void efn$longLeftArm(
            PlayerEntityRenderer<?> playerEntityRenderer,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            Identifier id,
            boolean bl,
            Operation<Void> operation) {
        operation.call(playerEntityRenderer, matrices, orderedRenderCommandQueue, i, id, bl);
        if (HandModelModule.active != null) {
            matrices.push();
            HandModelModule.shoulder(matrices, false, playerEntityRenderer);
            operation.call(playerEntityRenderer, matrices, orderedRenderCommandQueue, i, id, bl);
            matrices.pop();
        }
    }
}
