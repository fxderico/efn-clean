/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.efn.client.module.impl.movement.BoatNoclipModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.vehicle.AbstractBoatEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {AbstractBoatEntity.class})
public abstract class MixinBoatNoclipAbstractBoat {

    @ModifyReturnValue(
            method = {"collidesWith"},
            at = {@At(value = "RETURN")})
    private boolean efn$boatNoclipCollide(boolean bl) {
        if (!bl || BoatNoclipModule.active == null) {
            return bl;
        }
        AbstractBoatEntity abstractBoatEntity = (AbstractBoatEntity) ((Object) this);
        return !abstractBoatEntity.getEntityWorld().isClient()
                || abstractBoatEntity.getControllingPassenger()
                        != MinecraftClient.getInstance().player;
    }
}
