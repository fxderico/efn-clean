/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.efn.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.vehicle.AbstractBoatEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {AbstractBoatEntity.class})
public abstract class MixinEntityControlAbstractBoat {

    @ModifyExpressionValue(
            method = {"updatePaddles()V"},
            at = {
                @At(
                        value = "FIELD",
                        target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;pressingLeft:Z",
                        opcode = 180)
            })
    private boolean efn$entityControlInputLeft(boolean bl) {
        return bl
                && !EntityControlVehicleHook.locksBoatTurning((AbstractBoatEntity) ((Object) this));
    }

    @ModifyExpressionValue(
            method = {"updatePaddles()V"},
            at = {
                @At(
                        value = "FIELD",
                        target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;pressingRight:Z",
                        opcode = 180)
            })
    private boolean efn$entityControlInputRight(boolean bl) {
        return bl
                && !EntityControlVehicleHook.locksBoatTurning((AbstractBoatEntity) ((Object) this));
    }
}
