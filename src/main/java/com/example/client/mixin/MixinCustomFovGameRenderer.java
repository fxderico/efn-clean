package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.efn.client.module.impl.render.CustomFovModule;
import net.efn.client.module.impl.render.ZoomModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({GameRenderer.class})
public abstract class MixinCustomFovGameRenderer {

    public MixinCustomFovGameRenderer() {}

    @Shadow
    public abstract boolean isRenderingPanorama();

    @ModifyReturnValue(
            method = {"getFov"},
            at = {@At("RETURN")})
    private float efn$customFov(float f, @Local(argsOnly = true) boolean bl) {
        CustomFovModule active = CustomFovModule.active;
        float g = active != null && !this.isRenderingPanorama() ? active.fov() : f;
        if (!bl) {
            CustomFovModule.hudFovApplied(f, g);
        }
        return bl && !this.isRenderingPanorama() ? ZoomModule.fov(g) : g;
    }
}
