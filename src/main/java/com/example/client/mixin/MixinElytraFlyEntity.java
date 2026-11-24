package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.efn.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({Entity.class})
public abstract class MixinElytraFlyEntity {

    public MixinElytraFlyEntity() {}

    @ModifyReturnValue(
            method = {"getPose()Lnet/minecraft/entity/EntityPose;"},
            at = {@At("RETURN")})
    private EntityPose efn$elytraFlyGetPose(EntityPose entityPose) {
        ElytraFlyModule active = ElytraFlyModule.active;
        if (active == null) {
            return entityPose;
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if ((Object) this == player && client.isOnThread()) {
                return active.canPacketEfly(player) ? EntityPose.GLIDING : entityPose;
            } else {
                return entityPose;
            }
        }
    }
}
