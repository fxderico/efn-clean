package com.example.client.mixin;

import net.efn.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.fog.StatusEffectFogModifier;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({StatusEffectFogModifier.class})
public abstract class MixinMobEffectFogEnvironment {

    public MixinMobEffectFogEnvironment() {}

    @Inject(
            method = {
                "shouldApply(Lnet/minecraft/block/enums/CameraSubmersionType;Lnet/minecraft/entity/Entity;)Z"
            },
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$cancelBlindnessDarknessFog(
            CameraSubmersionType cameraSubmersionType,
            Entity entity,
            CallbackInfoReturnable<Boolean> cir) {
        if (NoRenderModule.hideBlindnessDarkness()) {
            cir.setReturnValue(false);
        }
    }
}
