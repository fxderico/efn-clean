package com.example.client.mixin.indigo;

import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractTerrainRenderContext;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.util.math.ColorHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(
        value = {AbstractTerrainRenderContext.class},
        remap = false)
public abstract class MixinAbstractTerrainRenderContext {

    public MixinAbstractTerrainRenderContext() {}

    @Inject(
            method = {"bufferQuad"},
            at = {@At("HEAD")})
    private void efn$xrayBufferQuad(MutableQuadViewImpl mutableQuadViewImpl, CallbackInfo ci) {
        if (XrayModule.active != null) {
            float f = XrayModule.CURRENT_ALPHA.get()[0];
            if (!(f <= 0.0F) && !(f >= 1.0F)) {
                mutableQuadViewImpl.renderLayer(BlockRenderLayer.TRANSLUCENT);
                for (int i = 0; i < 4; i++) {
                    mutableQuadViewImpl.color(
                            i, ColorHelper.scaleAlpha(mutableQuadViewImpl.color(i), f));
                }
            }
        }
    }
}
