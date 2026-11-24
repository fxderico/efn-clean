package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.efn.client.module.impl.movement.TridentBoostModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.TridentItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({TridentItem.class})
public abstract class MixinTridentBoostTridentItem {

    public MixinTridentBoostTridentItem() {}

    @WrapOperation(
            method = {
                "onStoppedUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)Z"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/entity/player/PlayerEntity;addVelocity(DDD)V")
            },
            require = 0)
    private void efn$tridentBoostPush(
            PlayerEntity player, double d, double e, double d2, Operation<Void> operation) {
        double riptideMultiplier = TridentBoostModule.riptideMultiplier();
        operation.call(
                player, d * riptideMultiplier, e * riptideMultiplier, d2 * riptideMultiplier);
    }

    @ModifyExpressionValue(
            method = {
                "use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z")
            },
            require = 0)
    private boolean efn$tridentBoostUseOutOfWater(boolean bl) {
        return bl || TridentBoostModule.allowOutOfWater();
    }

    @ModifyExpressionValue(
            method = {
                "onStoppedUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)Z"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/entity/player/PlayerEntity;isTouchingWaterOrRain()Z")
            },
            require = 0)
    private boolean efn$tridentBoostReleaseOutOfWater(boolean bl) {
        return bl || TridentBoostModule.allowOutOfWater();
    }
}
