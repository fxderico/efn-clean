package com.example.client.mixin;

import net.efn.client.module.impl.movement.JesusModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Entity.class})
public abstract class MixinJesusEntity {

    public MixinJesusEntity() {}

    @Inject(
            method = {"onBubbleColumnSurfaceCollision(ZLnet/minecraft/util/math/BlockPos;)V"},
            at = {@At("HEAD")})
    private void efn$jesusAboveBubbleColumn(CallbackInfo ci) {
        this.efn$jesusMarkBubbleColumn();
    }

    @Inject(
            method = {"onBubbleColumnCollision(Z)V"},
            at = {@At("HEAD")})
    private void efn$jesusInsideBubbleColumn(CallbackInfo ci) {
        this.efn$jesusMarkBubbleColumn();
    }

    @Unique
    private void efn$jesusMarkBubbleColumn() {
        JesusModule active = JesusModule.active;
        if (active != null && (Object) this == MinecraftClient.getInstance().player) {
            active.markInBubbleColumn();
        }
    }
}
