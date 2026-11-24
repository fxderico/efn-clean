package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.efn.client.event.impl.PlayerAttackEntityEvent;
import net.efn.client.module.impl.movement.FlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({PlayerEntity.class})
public abstract class MixinPlayer {

    public MixinPlayer() {}

    @Inject(
            method = {"attack(Lnet/minecraft/entity/Entity;)V"},
            at = {@At("HEAD")})
    private void efn$onAttack(Entity entity, CallbackInfo ci) {
        if ((Object) this == MinecraftClient.getInstance().player) {
            PlayerAttackEntityEvent.dispatch(entity);
        }
    }

    @ModifyReturnValue(
            method = {"getOffGroundSpeed()F"},
            at = {@At("RETURN")})
    private float efn$flySpeed(float f) {
        FlyModule active = FlyModule.active;
        if (active == null) {
            return f;
        } else {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null && (Object) this == player) {
                float offGroundSpeed = active.getOffGroundSpeed(player);
                return offGroundSpeed != -1.0F ? offGroundSpeed : f;
            } else {
                return f;
            }
        }
    }
}
