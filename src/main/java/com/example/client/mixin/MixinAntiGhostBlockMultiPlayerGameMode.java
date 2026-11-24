package com.example.client.mixin;

import net.efn.client.module.impl.misc.AntiGhostBlockModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinAntiGhostBlockMultiPlayerGameMode {

    @Shadow private int blockBreakingCooldown;

    public MixinAntiGhostBlockMultiPlayerGameMode() {}

    @Shadow
    public abstract void cancelBlockBreaking();

    @Inject(
            method = {"breakBlock(Lnet/minecraft/util/math/BlockPos;)Z"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$holdBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (AntiGhostBlockModule.holdBreak(pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = {
                "attackBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"
            },
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$waitBeforeStart(
            BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (AntiGhostBlockModule.waiting(pos)) {
            this.cancelBlockBreaking();
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = {
                "updateBlockBreakingProgress(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"
            },
            at = {@At("HEAD")},
            cancellable = true,
            order = 1100)
    private void efn$waitBeforeContinue(
            BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (AntiGhostBlockModule.waiting(pos)) {
            if (this.blockBreakingCooldown > 0) {
                this.blockBreakingCooldown--;
            }
            this.cancelBlockBreaking();
            cir.setReturnValue(false);
        }
    }
}
