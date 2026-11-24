/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.efn.client.module.impl.movement.BoatFlyModule;
import net.efn.client.module.impl.movement.EntityControlModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.JumpingMount;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {ClientPlayerEntity.class})
public abstract class MixinEntityControlLocalPlayer {

    @ModifyReturnValue(
            method = {"getMountJumpStrength()F"},
            at = {@At(value = "RETURN")})
    private float efn$entityControlMaxJump(float f) {
        return EntityControlModule.maxJump() ? 1.0f : f;
    }

    @ModifyReturnValue(
            method = {"getJumpingMount()Lnet/minecraft/entity/JumpingMount;"},
            at = {@At(value = "RETURN")})
    private JumpingMount efn$boatFlyCancelJump(JumpingMount jumpingMount) {
        if (jumpingMount == null
                || !BoatFlyModule.cancelsMountJump((ClientPlayerEntity) ((Object) this))) {
            return jumpingMount;
        }
        return null;
    }
}
