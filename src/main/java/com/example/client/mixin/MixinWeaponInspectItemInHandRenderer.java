package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.efn.client.module.impl.render.HandModelModule;
import net.efn.client.module.impl.render.WeaponInspectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinWeaponInspectItemInHandRenderer {

    public MixinWeaponInspectItemInHandRenderer() {}

    @Inject(
            method = {
                "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"
            },
            at = {@At("HEAD")})
    private void efn$inspectHands(
            float f,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            ClientPlayerEntity player,
            int i,
            CallbackInfo ci) {
        if (WeaponInspectModule.active != null) {
            WeaponInspectModule.handsBegin(matrices);
        }
    }

    @WrapOperation(
            method = {
                "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V")
            })
    private void efn$inspectItem(
            HeldItemRenderer heldItemRenderer,
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext itemDisplayContext,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            Operation<Void> operation,
            @Local(argsOnly = true) Hand hand) {
        boolean bl = hand == Hand.MAIN_HAND;
        if (WeaponInspectModule.active != null) {
            if (!WeaponInspectModule.renderItem(
                    matrices,
                    entity,
                    stack,
                    itemDisplayContext,
                    orderedRenderCommandQueue,
                    i,
                    bl)) {
                operation.call(
                        heldItemRenderer,
                        entity,
                        stack,
                        itemDisplayContext,
                        matrices,
                        orderedRenderCommandQueue,
                        i);
            }
        } else {
            if (HandModelModule.active != null) {
                HandModelModule.renderArm(
                        matrices,
                        entity,
                        stack,
                        itemDisplayContext,
                        orderedRenderCommandQueue,
                        i,
                        bl);
            }
            operation.call(
                    heldItemRenderer,
                    entity,
                    stack,
                    itemDisplayContext,
                    matrices,
                    orderedRenderCommandQueue,
                    i);
        }
    }
}
