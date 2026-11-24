/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import net.efn.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {Entity.class})
public abstract class MixinEntityControlEntity {

    @ModifyVariable(
            method = {"move"},
            at = {@At(value = "HEAD")},
            argsOnly = true)
    private Vec3d efn$entityControlVehicleMove(Vec3d vec, MovementType movementType) {
        if (!EntityControlVehicleHook.anyActive) {
            return vec;
        }
        return EntityControlVehicleHook.onEntityMove((Entity) ((Object) this), movementType, vec);
    }
}
