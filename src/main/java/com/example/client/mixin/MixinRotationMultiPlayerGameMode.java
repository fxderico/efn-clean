package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayerInteractionManager.class})
public abstract class MixinRotationMultiPlayerGameMode {

    @Shadow @Final private MinecraftClient client;

    public MixinRotationMultiPlayerGameMode() {}

    @WrapMethod(method = {"interactItem"})
    private ActionResult efn$useItemRotation(
            PlayerEntity player, Hand hand, Operation<ActionResult> operation) {
        return this.efn$asSent(player, () -> (ActionResult) operation.call(player, hand));
    }

    @WrapMethod(method = {"stopUsingItem"})
    private void efn$releaseRotation(PlayerEntity player, Operation<Void> operation) {
        this.efn$asSent(player, () -> (Void) operation.call(player));
    }

    private <T> T efn$asSent(PlayerEntity player, Supplier<T> supplier) {
        ClientPlayerEntity player2 = this.client.player;
        if (player == player2 && !ServerRotation.isSwapped()) {
            Rotation rotation = ServerRotation.commit(player2);
            float yaw = player2.getYaw();
            float pitch = player2.getPitch();
            if (rotation.yaw() == yaw && rotation.pitch() == pitch) {
                return (T) supplier.get();
            } else {
                player2.setYaw(rotation.yaw());
                player2.setPitch(rotation.pitch());
                Object object;
                try {
                    Object supplier2 = supplier.get();
                    object = supplier2;
                } finally {
                    player2.setYaw(yaw);
                    player2.setPitch(pitch);
                }
                return (T) object;
            }
        } else {
            return (T) supplier.get();
        }
    }

    @Inject(
            method = {"attackEntity"},
            at = {@At("HEAD")},
            cancellable = true,
            order = 950)
    private void efn$attackRotationGate(PlayerEntity player, Entity entity, CallbackInfo ci) {
        ClientPlayerEntity player2 = this.client.player;
        if (player == player2) {
            Box boundingBox = entity.getBoundingBox();
            double entityInteractionRange = player2.getEntityInteractionRange();
            AttackRangeComponent attackRangeComponent =
                    player2.getMainHandStack().get(DataComponentTypes.ATTACK_RANGE);
            if (attackRangeComponent != null) {
                boundingBox = boundingBox.expand(attackRangeComponent.hitboxMargin());
                entityInteractionRange = attackRangeComponent.getEffectiveMaxRange(player2);
            }
            if (ServerRotation.misses(player2, boundingBox, entityInteractionRange)) {
                player.resetTicksSince();
                ci.cancel();
            }
        }
    }

    @Inject(
            method = {"interactEntity"},
            at = {@At("HEAD")},
            cancellable = true,
            order = 950)
    private void efn$interactRotationGate(
            PlayerEntity player,
            Entity entity,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir) {
        ClientPlayerEntity player2 = this.client.player;
        if (player == player2
                && ServerRotation.misses(
                        player2, entity.getBoundingBox(), player2.getEntityInteractionRange())) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(
            method = {"interactEntityAtLocation"},
            at = {@At("HEAD")},
            cancellable = true,
            order = 950)
    private void efn$interactAtRotationGate(
            PlayerEntity player,
            Entity entity,
            EntityHitResult entityHitResult,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir) {
        ClientPlayerEntity player2 = this.client.player;
        if (player == player2
                && ServerRotation.misses(
                        player2, entity.getBoundingBox(), player2.getEntityInteractionRange())) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(
            method = {"interactBlock"},
            at = {@At("HEAD")},
            cancellable = true,
            order = 950)
    private void efn$useItemOnRotationGate(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult blockHitResult,
            CallbackInfoReturnable<ActionResult> cir) {
        if (player == this.client.player
                && ServerRotation.missesBlock(
                        player,
                        new Box(blockHitResult.getBlockPos()),
                        player.getBlockInteractionRange())) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(
            method = {"attackBlock"},
            at = {@At("HEAD")},
            cancellable = true,
            order = 950)
    private void efn$startDestroyRotationGate(
            BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        ClientPlayerEntity player = this.client.player;
        if (player != null
                && ServerRotation.missesBlock(
                        player, new Box(pos), player.getBlockInteractionRange())) {
            cir.setReturnValue(false);
        }
    }
}
