package com.example.client.mixin;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.WTapRecodeModule;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinMultiPlayerGameMode {

    public MixinMultiPlayerGameMode() {}

    @Inject(
            method = {"attackEntity"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/network/ClientPlayerInteractionManager;syncSelectedSlot()V")
            })
    private void efn$beforeAttack(PlayerEntity player, Entity entity, CallbackInfo ci) {
        WTapRecodeModule module = ModuleRegistry.get(WTapRecodeModule.class);
        if (module != null && module.isEnabled()) {
            module.beforeAttack(player, entity);
        }
    }

    @Inject(
            method = {"attackEntity"},
            at = {@At("RETURN")})
    private void efn$afterAttack(PlayerEntity player, Entity entity, CallbackInfo ci) {
        if (player == MinecraftClient.getInstance().player) {
            MeleeAttacks.noteAction(player);
        }
        WTapRecodeModule module;
        if ((module = ModuleRegistry.get(WTapRecodeModule.class)) != null && module.isEnabled()) {
            module.afterAttack();
        }
    }

    @Inject(
            method = {"attackWithPiercingWeapon"},
            at = {@At("RETURN")})
    private void efn$afterStab(PiercingWeaponComponent piercingWeaponComponent, CallbackInfo ci) {
        MeleeAttacks.noteAction(MinecraftClient.getInstance().player);
        WTapRecodeModule module = ModuleRegistry.get(WTapRecodeModule.class);
        if (module != null && module.isEnabled()) {
            module.afterStab();
        }
    }

    @Inject(
            method = {"interactItem"},
            at = {@At("RETURN")})
    private void efn$afterUseItem(
            PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player == MinecraftClient.getInstance().player) {
            MeleeAttacks.noteAction(player);
        }
    }

    @Inject(
            method = {"interactBlock"},
            at = {@At("RETURN")})
    private void efn$afterUseItemOn(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult blockHitResult,
            CallbackInfoReturnable<ActionResult> cir) {
        if (player == MinecraftClient.getInstance().player) {
            MeleeAttacks.noteAction(player);
        }
    }

    @Inject(
            method = {"interactEntity"},
            at = {@At("RETURN")})
    private void efn$afterInteract(
            PlayerEntity player,
            Entity entity,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir) {
        if (player == MinecraftClient.getInstance().player) {
            MeleeAttacks.noteAction(player);
        }
    }

    @Inject(
            method = {"interactEntityAtLocation"},
            at = {@At("RETURN")})
    private void efn$afterInteractAt(
            PlayerEntity player,
            Entity entity,
            EntityHitResult entityHitResult,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir) {
        if (player == MinecraftClient.getInstance().player) {
            MeleeAttacks.noteAction(player);
        }
    }

    @Inject(
            method = {"stopUsingItem"},
            at = {@At("RETURN")})
    private void efn$afterRelease(PlayerEntity player, CallbackInfo ci) {
        if (player == MinecraftClient.getInstance().player) {
            MeleeAttacks.noteAction(player);
        }
    }
}
