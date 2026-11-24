package com.example.client.mixin;

import net.efn.client.module.impl.combat.FriendGuardModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinFriendGuardMultiPlayerGameMode {

    public MixinFriendGuardMultiPlayerGameMode() {}

    @Inject(
            method = {
                "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V"
            },
            at = {@At("HEAD")},
            cancellable = true,
            order = 900)
    private void efn$friendGuard(PlayerEntity player, Entity entity, CallbackInfo ci) {
        if (FriendGuardModule.intercepts(player, entity)) {
            player.resetTicksSince();
            ci.cancel();
        }
    }

    @Inject(
            method = {
                "attackWithPiercingWeapon(Lnet/minecraft/component/type/PiercingWeaponComponent;)V"
            },
            at = {@At("HEAD")},
            cancellable = true,
            order = 900)
    private void efn$friendGuardStab(
            PiercingWeaponComponent piercingWeaponComponent, CallbackInfo ci) {
        if (FriendGuardModule.interceptsStab(MinecraftClient.getInstance().player)) {
            ci.cancel();
        }
    }
}
