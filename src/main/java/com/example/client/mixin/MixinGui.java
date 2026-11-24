package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.NoRenderModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({InGameHud.class})
public abstract class MixinGui {

    public MixinGui() {}

    @ModifyExpressionValue(
            method = {"renderCrosshair"},
            at = {
                @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/option/Perspective;isFirstPerson()Z")
            })
    private boolean efn$freecamCrosshair(boolean bl) {
        FreecamModule active = FreecamModule.active;
        return active != null && active.showBody() && !bl || bl;
    }

    @Inject(
            method = {"render"},
            at = {@At("TAIL")})
    private void efn$onRender2D(
            DrawContext context, RenderTickCounter renderTickCounter, CallbackInfo ci) {
        Render2DEvent.dispatch(context, renderTickCounter.getTickProgress(false));
    }

    @Inject(
            method = {"renderStatusEffectOverlay"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$cancelVanillaEffects(
            DrawContext context, RenderTickCounter renderTickCounter, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(
            method = {"renderOverlay"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$cancelTextureOverlay(
            DrawContext context, Identifier id, float f, CallbackInfo ci) {
        if (NoRenderModule.hidePumpkinPowderSnow()) {
            ci.cancel();
        }
    }

    @Inject(
            method = {"renderPortalOverlay"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$cancelPortalOverlay(DrawContext context, float f, CallbackInfo ci) {
        if (NoRenderModule.hideNauseaPortal()) {
            ci.cancel();
        }
    }

    @Inject(
            method = {"renderNauseaOverlay"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$cancelConfusionOverlay(DrawContext context, float f, CallbackInfo ci) {
        if (NoRenderModule.hideNauseaPortal()) {
            ci.cancel();
        }
    }
}
