package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.AimAssistModule;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.FreelookModule;
import net.efn.client.module.impl.render.ZoomModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Mouse.class})
public abstract class MixinMouseHandler {

    public MixinMouseHandler() {}

    @Inject(
            method = {"onMouseButton"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$onMouseButton(long l, MouseInput mouseInput, int i, CallbackInfo ci) {
        if (MouseInputEvent.dispatch(l, mouseInput.button(), i, mouseInput.modifiers())) {
            ci.cancel();
        }
    }

    @Inject(
            method = {"onMouseScroll"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$wheel(long l, double d, double e, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (e != 0.0
                && l == client.getWindow().getHandle()
                && client.currentScreen == null
                && client.getOverlay() == null) {
            if (ZoomModule.scroll(e) || FreecamModule.scroll(e)) {
                ci.cancel();
            }
        }
    }

    @WrapOperation(
            method = {"updateMouse"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V")
            })
    private void efn$cameraTurn(
            ClientPlayerEntity player,
            double d,
            double e,
            Operation<Void> operation,
            @Local(argsOnly = true) double d2) {
        double turnScale = ZoomModule.turnScale();
        d *= turnScale;
        e *= turnScale;
        FreecamModule active = FreecamModule.active;
        if (active != null) {
            active.turn(d, e);
        } else {
            FreelookModule module = ModuleRegistry.get(FreelookModule.class);
            if (module != null && module.cameraMode()) {
                module.turnCamera(d, e);
            } else {
                AimAssistModule module2 = ModuleRegistry.get(AimAssistModule.class);
                if (module2 != null && module2.isEnabled()) {
                    double[] doubles = module2.nudge(player, d, e, d2);
                    d += doubles[0];
                    e += doubles[1];
                }
                operation.call(player, d, e);
            }
        }
    }
}
