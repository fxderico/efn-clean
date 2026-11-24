package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.efn.client.module.impl.movement.JesusModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({PowderSnowBlock.class})
public abstract class MixinJesusPowderSnowBlock {

    public MixinJesusPowderSnowBlock() {}

    @ModifyReturnValue(
            method = {"canWalkOnPowderSnow(Lnet/minecraft/entity/Entity;)Z"},
            at = {@At("RETURN")})
    private static boolean efn$jesusWalkOnPowderSnow(boolean bl, Entity entity) {
        if (bl) {
            return true;
        } else {
            JesusModule active = JesusModule.active;
            return active != null
                    && entity == MinecraftClient.getInstance().player
                    && active.canWalkOnPowderSnow();
        }
    }
}
