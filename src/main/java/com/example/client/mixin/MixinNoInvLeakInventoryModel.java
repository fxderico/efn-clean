package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin({InventoryScreen.class})
public abstract class MixinNoInvLeakInventoryModel {

    public MixinNoInvLeakInventoryModel() {}

    @WrapOperation(
            method = {"drawEntity"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;drawEntity(Lnet/minecraft/entity/LivingEntity;)Lnet/minecraft/client/render/entity/state/EntityRenderState;")
            })
    private static EntityRenderState efn$noInvLeakModel(
            LivingEntity entity, Operation<EntityRenderState> operation) {
        if (!NoInvLeakModule.hidesModelOf(entity)) {
            return (EntityRenderState) operation.call(entity);
        } else {
            NoInvLeakModule.bareModel(true);
            EntityRenderState entityRenderState;
            try {
                EntityRenderState entityRenderState2 = (EntityRenderState) operation.call(entity);
                entityRenderState = entityRenderState2;
            } finally {
                NoInvLeakModule.bareModel(false);
            }
            return entityRenderState;
        }
    }
}
