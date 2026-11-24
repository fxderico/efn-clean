package com.example.client.mixin;

import net.efn.client.module.impl.render.AtmosphereModule;
import net.efn.client.module.impl.render.PeepWorld;
import net.efn.client.module.impl.render.PinkWater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({FogRenderer.class})
public abstract class MixinAtmosphereFogRenderer {

    public MixinAtmosphereFogRenderer() {}

    @Inject(
            method = {
                "getFogColor(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/world/ClientWorld;IF)Lorg/joml/Vector4f;"
            },
            at = {@At("RETURN")})
    private void efn$fogColour(
            Camera camera,
            float f,
            ClientWorld world,
            int i,
            float g,
            CallbackInfoReturnable<Vector4f> cir) {
        CameraSubmersionType submersionType = camera.getSubmersionType();
        if (submersionType == CameraSubmersionType.WATER) {
            PinkWater.underwater((Vector4f) cir.getReturnValue());
        }
        if (submersionType == CameraSubmersionType.NONE && !(g > 0.0F)) {
            Entity focusedEntity = camera.getFocusedEntity();
            LivingEntity entity;
            if (!(focusedEntity instanceof LivingEntity)
                    || !(entity = (LivingEntity) focusedEntity)
                                    .hasStatusEffect(StatusEffects.BLINDNESS)
                            && !entity.hasStatusEffect(StatusEffects.DARKNESS)) {
                AtmosphereModule module = AtmosphereModule.get();
                if (module != null) {
                    module.fog(camera, world, (Vector4f) cir.getReturnValue());
                }
                PeepWorld.fog((Vector4f) cir.getReturnValue());
            }
        }
    }
}
