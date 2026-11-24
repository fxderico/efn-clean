/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import net.efn.client.module.impl.render.character.CharacterLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {PlayerEntityModel.class})
public abstract class MixinCharacterPlayerModel {

    @Inject(
            method = {"setArmAngle"},
            at = {@At(value = "HEAD")},
            cancellable = true)
    private void efn$characterHand(
            PlayerEntityRenderState playerEntityRenderState,
            Arm arm,
            MatrixStack matrices,
            CallbackInfo ci) {
        if (CharacterLayer.translateToHand(
                (PlayerEntityModel) ((Object) this), playerEntityRenderState, arm, matrices)) {
            ci.cancel();
        }
    }
}
