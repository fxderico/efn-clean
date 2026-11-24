/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import net.efn.client.module.impl.render.JumpCirclesModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {LivingEntity.class})
public abstract class MixinJumpCirclesLivingEntity {

    @Inject(
            method = {"jump()V"},
            at = {
                @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/entity/LivingEntity;setVelocity(DDD)V")
            })
    private void efn$jumpCircle(CallbackInfo ci) {
        if (JumpCirclesModule.active != null) {
            JumpCirclesModule.onJump((LivingEntity) ((Object) this));
        }
    }
}
