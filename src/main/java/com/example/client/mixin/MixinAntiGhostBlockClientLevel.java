/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.efn.client.module.impl.misc.AntiGhostBlockModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {ClientWorld.class})
public abstract class MixinAntiGhostBlockClientLevel {

    @WrapOperation(
            method = {
                "handleBlockUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/network/PendingUpdateManager;hasPendingUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z")
            })
    private boolean efn$serverAnswer(
            PendingUpdateManager pendingUpdateManager,
            BlockPos pos,
            BlockState state,
            Operation<Boolean> operation) {
        boolean bl = operation.call(pendingUpdateManager, pos, state);
        ClientWorld world = (ClientWorld) ((Object) this);
        return bl && !AntiGhostBlockModule.serverAnswered(world, pos);
    }

    @Inject(
            method = {"handlePlayerActionResponse(I)V"},
            at = {@At(value = "TAIL")})
    private void efn$acknowledged(int i, CallbackInfo ci) {
        ClientWorld world = (ClientWorld) ((Object) this);
        AntiGhostBlockModule.acknowledged(world, i);
    }
}
