package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.efn.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class MixinElytraFlyLivingEntity {

    public MixinElytraFlyLivingEntity() {}

    @ModifyReturnValue(
            method = {"isGliding()Z"},
            at = {@At("RETURN")})
    private boolean efn$elytraFlyIsFallFlying(boolean bl) {
        ElytraFlyModule active = ElytraFlyModule.active;
        if (active == null) {
            return bl;
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            return (Object) this == player && client.isOnThread()
                    ? active.modifyFallFlying(player, bl)
                    : bl;
        }
    }
}
