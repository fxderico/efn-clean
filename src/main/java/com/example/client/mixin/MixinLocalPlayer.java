/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.efn.client.event.impl.PlayerMoveEvent;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {ClientPlayerEntity.class})
public abstract class MixinLocalPlayer {

    @WrapMethod(method = {"move"})
    private void efn$onMove(MovementType movementType, Vec3d vec, Operation<Void> operation) {
        if (!PlayerMoveEvent.TYPE.hasListeners()) {
            operation.call(new Object[] {movementType, vec});
            return;
        }
        PlayerMoveEvent.move(vec, vec3d -> operation.call(new Object[] {movementType, vec3d}));
    }

    @Inject(
            method = {"tick"},
            at = {@At(value = "HEAD")})
    private void efn$walkServerRotation(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) ((Object) this);
        if (MinecraftClient.getInstance().player == player) {
            ServerRotation.applyForMovement(player);
        }
    }

    @Inject(
            method = {"tick"},
            at = {@At(value = "RETURN")})
    private void efn$finishServerRotation(CallbackInfo ci) {
        ServerRotation.finishTick((ClientPlayerEntity) ((Object) this));
    }

    @Inject(
            method = {"tickMovement"},
            at = {
                @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/client/input/Input;tick()V",
                        shift = At.Shift.AFTER)
            })
    private void efn$freecamAfterInput(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) ((Object) this);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == player) {
            FreecamModule.drive(client);
        }
    }

    @Inject(
            method = {"sendMovementPackets"},
            at = {@At(value = "HEAD")})
    private void efn$freecamBeforeSend(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) ((Object) this);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == player) {
            FreecamModule.drive(client);
        }
    }

    @Inject(
            method = {"sendMovementPackets"},
            at = {@At(value = "RETURN")})
    private void efn$swapOutServerRotation(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) ((Object) this);
        if (MinecraftClient.getInstance().player == player) {
            ServerRotation.afterSend(player);
        }
    }
}
