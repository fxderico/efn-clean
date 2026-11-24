package com.example.client.mixin;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.MaceModule;
import net.efn.client.module.impl.combat.ShieldBreakerModule;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinMaceMultiPlayerGameMode {

    public MixinMaceMultiPlayerGameMode() {}

    @Inject(
            method = {"attackEntity"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/network/ClientPlayerInteractionManager;syncSelectedSlot()V")
            })
    private void efn$weaponBeforeAttack(PlayerEntity player, Entity entity, CallbackInfo ci) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        if (player == player2 && !MeleeAttacks.actedThisTick(player2)) {
            ShieldBreakerModule module = ModuleRegistry.get(ShieldBreakerModule.class);
            if (module != null && module.isEnabled()) {
                module.beforeAttack(player2, entity);
            }
            MaceModule module2;
            if ((module2 = ModuleRegistry.get(MaceModule.class)) != null && module2.isEnabled()) {
                module2.beforeAttack(player2, entity);
            }
        }
    }
}
