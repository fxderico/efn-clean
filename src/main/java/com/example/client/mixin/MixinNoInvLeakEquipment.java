/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {LivingEntity.class})
public abstract class MixinNoInvLeakEquipment {

    @ModifyReturnValue(
            method = {"getEquippedStack"},
            at = {@At(value = "RETURN")})
    private ItemStack efn$noInvLeakBareModel(ItemStack stack) {
        return NoInvLeakModule.modelItem((LivingEntity) ((Object) this), stack);
    }
}
