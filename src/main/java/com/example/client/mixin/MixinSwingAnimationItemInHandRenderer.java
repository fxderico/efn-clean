package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.efn.client.module.impl.render.SwingAnimationModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({HeldItemRenderer.class})
public abstract class MixinSwingAnimationItemInHandRenderer {

    public MixinSwingAnimationItemInHandRenderer() {}

    @WrapOperation(
            method = {"renderFirstPersonItem"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V")
            })
    private void efn$swingStyle(
            HeldItemRenderer heldItemRenderer,
            float f,
            MatrixStack matrices,
            int i,
            Arm arm,
            Operation<Void> operation,
            @Local(argsOnly = true) Hand hand) {
        SwingAnimationModule active = SwingAnimationModule.active;
        if (active != null && active.covers(hand == Hand.MAIN_HAND)) {
            active.pose(matrices, f, i);
        } else {
            operation.call(heldItemRenderer, f, matrices, i, arm);
        }
    }
}
