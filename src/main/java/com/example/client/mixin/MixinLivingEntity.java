package com.example.client.mixin;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.SlowSwingModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({LivingEntity.class})
public abstract class MixinLivingEntity {

    public MixinLivingEntity() {}

    @Inject(
            method = {"getHandSwingDuration"},
            at = {@At("RETURN")},
            cancellable = true)
    private void efn$modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        SlowSwingModule module = ModuleRegistry.get(SlowSwingModule.class);
        if (module != null && module.isEnabled()) {
            if ((Object) this == MinecraftClient.getInstance().player) {
                cir.setReturnValue(module.getModifiedSwingDuration((Integer) cir.getReturnValue()));
            }
        }
    }
}
