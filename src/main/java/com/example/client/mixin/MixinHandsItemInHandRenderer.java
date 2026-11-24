package com.example.client.mixin;

import net.efn.client.module.impl.render.HandsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinHandsItemInHandRenderer {

    public MixinHandsItemInHandRenderer() {}

    @Inject(
            method = {
                "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                        shift = Shift.AFTER)
            })
    private void efn$handsPose(
            AbstractClientPlayerEntity player,
            float f,
            float g,
            Hand hand,
            float h,
            ItemStack stack,
            float f2,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            CallbackInfo ci) {
        if (HandsModule.live != null) {
            boolean bl = hand == Hand.MAIN_HAND;
            Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
            HandsModule.shape(matrices, bl, arm == Arm.RIGHT ? 1 : -1);
        }
    }
}
