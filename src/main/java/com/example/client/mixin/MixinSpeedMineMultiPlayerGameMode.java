package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.efn.client.module.impl.misc.SpeedMineModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinSpeedMineMultiPlayerGameMode {

    @Shadow @Final private MinecraftClient client;

    @Shadow private int blockBreakingCooldown;

    public MixinSpeedMineMultiPlayerGameMode() {}

    @Inject(
            method = {
                "updateBlockBreakingProgress(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"
            },
            at = {@At("HEAD")})
    private void efn$speedMineDelay(
            BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        ClientPlayerEntity player = this.client.player;
        if (this.blockBreakingCooldown > 0
                && player != null
                && this.client.world != null
                && !player.getAbilities().creativeMode) {
            BlockState blockState = this.client.world.getBlockState(pos);
            if (SpeedMineModule.dropsDelay(
                    blockState,
                    blockState.calcBlockBreakingDelta(player, this.client.world, pos))) {
                this.blockBreakingCooldown = 0;
            }
        }
    }

    @WrapOperation(
            method = {
                "updateBlockBreakingProgress(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F")
            })
    private float efn$speedMineProgress(
            BlockState state,
            PlayerEntity player,
            BlockView blockView,
            BlockPos pos,
            Operation<Float> operation) {
        return SpeedMineModule.scaleProgress(
                state, (Float) operation.call(state, player, blockView, pos));
    }
}
