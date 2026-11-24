/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.render.EspModule;
import net.efn.client.module.impl.render.ItemEspModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {Entity.class})
public class MixinEntity {

    @Inject(
            method = {"isGlowing()Z"},
            at = {@At(value = "HEAD")},
            cancellable = true)
    private void efn$isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) ((Object) this);
        if (!entity.getEntityWorld().isClient()) {
            return;
        }
        if (EspModule.shouldGlow(entity) || ItemEspModule.shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = {"getTeamColorValue()I"},
            at = {@At(value = "HEAD")},
            cancellable = true)
    private void efn$getTeamColor(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) ((Object) this);
        if (!entity.getEntityWorld().isClient()) {
            return;
        }
        if (EspModule.shouldGlow(entity)) {
            cir.setReturnValue(EspModule.glowColor(entity));
        } else if (ItemEspModule.shouldGlow(entity)) {
            cir.setReturnValue(ItemEspModule.glowColor(entity));
        }
    }

    @Inject(
            method = {"isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z"},
            at = {@At(value = "HEAD")},
            cancellable = true)
    private void efn$freecamOwnBody(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        FreecamModule active = FreecamModule.active;
        if (active != null && active.drawsSelf((Entity) ((Object) this), player)) {
            cir.setReturnValue(false);
        }
    }
}
