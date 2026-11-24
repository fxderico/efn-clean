/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.efn.client.module.impl.render.TimeChangerModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {ClientWorld.class})
public abstract class MixinTimeChangerClientLevel {

    @WrapOperation(
            method = {"setTime"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/world/ClientWorld$Properties;setTimeOfDay(J)V")
            })
    private void efn$timeChangerServerDayTime(
            ClientWorld.Properties properties, long l, Operation<Void> operation) {
        TimeChangerModule active = TimeChangerModule.active;
        if (active == null) {
            operation.call(properties, l);
            return;
        }
        ClientWorld world = (ClientWorld) ((Object) this);
        long timeOfDay = properties.getTimeOfDay();
        long interceptServerDayTime = active.interceptServerDayTime(world, l);
        operation.call(properties, interceptServerDayTime);
        if (interceptServerDayTime != timeOfDay) {
            world.getEnvironmentAttributes().tick();
        }
    }
}
