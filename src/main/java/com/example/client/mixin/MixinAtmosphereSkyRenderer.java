package com.example.client.mixin;

import net.efn.client.module.impl.render.AtmosphereModule;
import net.efn.client.module.impl.render.SkyLyrics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.MoonPhase;
import net.minecraft.world.dimension.DimensionType.Skybox;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({SkyRendering.class})
public abstract class MixinAtmosphereSkyRenderer {

    public MixinAtmosphereSkyRenderer() {}

    @Inject(
            method = {
                "updateRenderState(Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/state/SkyRenderState;)V"
            },
            at = {@At("TAIL")})
    private void efn$readSky(
            ClientWorld world,
            float f,
            Camera camera,
            SkyRenderState skyRenderState,
            CallbackInfo ci) {
        AtmosphereModule module = AtmosphereModule.get();
        if (module != null && skyRenderState.skybox == Skybox.OVERWORLD) {
            module.extract(world, f, skyRenderState);
        }
    }

    @Inject(
            method = {"renderTopSky(I)V"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$dome(int i, CallbackInfo ci) {
        AtmosphereModule module = AtmosphereModule.get();
        if (module != null && module.skyDisc()) {
            ci.cancel();
        }
    }

    @Inject(
            method = {"renderTopSky(I)V"},
            at = {@At("TAIL")})
    private void efn$afterDome(int i, CallbackInfo ci) {
        AtmosphereModule module = AtmosphereModule.get();
        if (module != null) {
            module.afterSkyDisc();
        }
    }

    @Inject(
            method = {"renderGlowingSky(Lnet/minecraft/client/util/math/MatrixStack;FI)V"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$noSunriseFan(MatrixStack matrices, float f, int i, CallbackInfo ci) {
        AtmosphereModule module = AtmosphereModule.get();
        if (module != null && module.replacesSunrise()) {
            ci.cancel();
        }
    }

    @Inject(
            method = {"renderStars(FLnet/minecraft/client/util/math/MatrixStack;)V"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$stars(float f, MatrixStack matrices, CallbackInfo ci) {
        AtmosphereModule module = AtmosphereModule.get();
        if (module != null && module.stars(matrices)) {
            ci.cancel();
        }
    }

    @Inject(
            method = {
                "renderCelestialBodies(Lnet/minecraft/client/util/math/MatrixStack;FFFLnet/minecraft/world/MoonPhase;FF)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/render/SkyRendering;renderMoon(Lnet/minecraft/world/MoonPhase;FLnet/minecraft/client/util/math/MatrixStack;)V")
            })
    private void efn$beforeMoon(
            MatrixStack matrices,
            float f,
            float g,
            float h,
            MoonPhase moonPhase,
            float f2,
            float g2,
            CallbackInfo ci) {
        AtmosphereModule module = AtmosphereModule.get();
        if (module != null) {
            module.beforeMoon(matrices);
        }
    }

    @Inject(
            method = {
                "renderCelestialBodies(Lnet/minecraft/client/util/math/MatrixStack;FFFLnet/minecraft/world/MoonPhase;FF)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/render/SkyRendering;renderMoon(Lnet/minecraft/world/MoonPhase;FLnet/minecraft/client/util/math/MatrixStack;)V",
                        shift = Shift.AFTER)
            })
    private void efn$afterMoon(
            MatrixStack matrices,
            float f,
            float g,
            float h,
            MoonPhase moonPhase,
            float f2,
            float g2,
            CallbackInfo ci) {
        AtmosphereModule module = AtmosphereModule.get();
        if (module != null) {
            module.afterMoon(matrices);
        }
    }

    @Inject(
            method = {
                "renderCelestialBodies(Lnet/minecraft/client/util/math/MatrixStack;FFFLnet/minecraft/world/MoonPhase;FF)V"
            },
            at = {@At("TAIL")})
    private void efn$afterCelestials(
            MatrixStack matrices,
            float f,
            float g,
            float h,
            MoonPhase moonPhase,
            float f2,
            float g2,
            CallbackInfo ci) {
        AtmosphereModule module = AtmosphereModule.get();
        if (module != null) {
            module.afterCelestials();
        }
        SkyLyrics.render();
    }

    @Inject(
            method = {"renderEndSky()V"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$endSky(CallbackInfo ci) {
        AtmosphereModule module = AtmosphereModule.get();
        if (module != null && module.endSky()) {
            ci.cancel();
        }
    }
}
