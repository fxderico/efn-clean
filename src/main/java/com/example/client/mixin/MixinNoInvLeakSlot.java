/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {Slot.class})
public abstract class MixinNoInvLeakSlot {

    @ModifyReturnValue(
            method = {"getBackgroundSprite()Lnet/minecraft/util/Identifier;"},
            at = {@At(value = "RETURN")})
    private Identifier efn$noInvLeakSilhouette(Identifier id) {
        Slot slot = (Slot) ((Object) this);
        return id != null
                        && NoInvLeakModule.overlaying()
                        && slot.inventory instanceof PlayerInventory
                        && slot.hasStack()
                ? null
                : id;
    }
}
