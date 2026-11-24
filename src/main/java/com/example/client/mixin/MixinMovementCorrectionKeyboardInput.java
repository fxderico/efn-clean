/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import net.efn.client.util.pvp.InputControl;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {KeyboardInput.class})
public abstract class MixinMovementCorrectionKeyboardInput {

    @Inject(
            method = {"tick"},
            at = {@At(value = "TAIL")})
    private void efn$shapeInput(CallbackInfo ci) {
        Input input = (Input) ((Object) this);
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.input != input) {
            return;
        }
        PlayerInput playerInput = input.playerInput;
        PlayerInput playerInput2 =
                InputControl.shape(player, ServerRotation.correctInput(playerInput));
        if (playerInput2 == playerInput) {
            return;
        }
        input.playerInput = playerInput2;
        ((AccessorMovementClientInput) ((Object) input))
                .efn$setMoveVector(
                        new Vec2f(
                                        MixinMovementCorrectionKeyboardInput.impulse(
                                                playerInput2.left(), playerInput2.right()),
                                        MixinMovementCorrectionKeyboardInput.impulse(
                                                playerInput2.forward(), playerInput2.backward()))
                                .normalize());
    }

    private static float impulse(boolean bl, boolean bl2) {
        return bl == bl2 ? 0.0f : (bl ? 1.0f : -1.0f);
    }
}
