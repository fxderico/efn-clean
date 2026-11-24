package com.example.client.mixin;

import net.efn.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderTickCounter.Dynamic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({Dynamic.class})
public abstract class MixinSpeedDeltaTrackerTimer {

    @Shadow private float dynamicDeltaTicks;

    public MixinSpeedDeltaTrackerTimer() {}

    @Inject(
            method = {"beginRenderTick(J)I"},
            at = {
                @At(
                        value = "FIELD",
                        target =
                                "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;lastTimeMillis:J",
                        opcode = 181)
            })
    private void efn$speedTimer(long l, CallbackInfoReturnable<Integer> cir) {
        float timerMultiplier = SpeedModule.timerMultiplier;
        if (timerMultiplier != 1.0F) {
            this.dynamicDeltaTicks *= timerMultiplier;
        }
    }
}
