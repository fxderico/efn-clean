package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.efn.client.module.impl.movement.NoJumpDelayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class MixinNoJumpDelayLivingEntity {

    @Shadow private int jumpingCooldown;

    public MixinNoJumpDelayLivingEntity() {}

    @WrapOperation(
            method = {"tickMovement()V"},
            at = {
                @At(
                        value = "FIELD",
                        target = "Lnet/minecraft/entity/LivingEntity;jumpingCooldown:I",
                        opcode = 181)
            })
    private void efn$noJumpDelay(LivingEntity entity, int rearm, Operation<Void> operation) {
        NoJumpDelayModule active = NoJumpDelayModule.active;
        if (active != null
                && rearm > this.jumpingCooldown
                && entity == MinecraftClient.getInstance().player) {
            rearm = active.rearm(rearm);
        }
        operation.call(entity, rearm);
    }
}
