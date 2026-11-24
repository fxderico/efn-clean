package com.example.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;

import net.efn.client.module.impl.render.ShaderEspModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({WorldRenderer.class})
public abstract class MixinShaderEspLevelRenderer {

    public MixinShaderEspLevelRenderer() {}

    @Inject(
            method = {"getAndUpdateRenderState"},
            at = {@At("RETURN")})
    private void efn$glowEspKeepState(
            Entity entity, float f, CallbackInfoReturnable<EntityRenderState> cir) {
        if (ShaderEspModule.active != null) {
            ShaderEspModule.extracted(entity, (EntityRenderState) cir.getReturnValue());
        }
    }

    @Inject(
            method = {"render"},
            at = {@At("TAIL")})
    private void efn$glowEsp(
            CallbackInfo ci,
            @Local(argsOnly = true) Camera camera,
            @Local(argsOnly = true, ordinal = 0) Matrix4f matrix,
            @Local(argsOnly = true, ordinal = 1) Matrix4f matrix2) {
        if (ShaderEspModule.active != null) {
            ShaderEspModule.afterLevel(camera, matrix, matrix2);
        }
    }
}
