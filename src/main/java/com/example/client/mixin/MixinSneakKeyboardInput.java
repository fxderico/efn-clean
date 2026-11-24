/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import net.efn.client.module.impl.movement.SneakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {KeyboardInput.class})
public abstract class MixinSneakKeyboardInput {

    @Inject(
            method = {"tick()V"},
            at = {@At(value = "TAIL")})
    private void efn$forceSneak(CallbackInfo ci) {
        SneakModule active = SneakModule.active;
        if (active == null || !active.doVanilla()) {
            return;
        }
        Input input = (Input) ((Object) this);
        PlayerInput playerInput = input.playerInput;
        if (playerInput.sneak()) {
            return;
        }
        input.playerInput =
                new PlayerInput(
                        playerInput.forward(),
                        playerInput.backward(),
                        playerInput.left(),
                        playerInput.right(),
                        playerInput.jump(),
                        true,
                        playerInput.sprint());
    }
}
