/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;

import net.efn.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {ClientPlayerEntity.class})
public abstract class MixinSpeedLocalPlayer {

    @ModifyVariable(
            method = {"move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"},
            at = {@At(value = "HEAD")},
            argsOnly = true)
    private Vec3d efn$speedMove(Vec3d vec, @Local(argsOnly = true) MovementType movementType) {
        SpeedModule active = SpeedModule.active;
        if (active == null || movementType != MovementType.SELF) {
            return vec;
        }
        return active.onSelfMove((ClientPlayerEntity) ((Object) this), vec);
    }
}
