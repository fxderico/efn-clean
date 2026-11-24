/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.efn.client.module.impl.render.PinkWater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.biome.Biome;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {Biome.class})
public abstract class MixinPinkWaterBiome {

    @Unique private volatile byte efn$cave;

    @ModifyReturnValue(
            method = {"getWaterColor"},
            at = {@At(value = "RETURN")})
    private int efn$pinkWater(int i) {
        if (!PinkWater.shows()) {
            return i;
        }
        byte efn$cave2 = this.efn$cave;
        if (efn$cave2 == 0 && (efn$cave2 = PinkWater.caveBiome((Biome) ((Object) this))) != 0) {
            this.efn$cave = efn$cave2;
        }
        return PinkWater.water(i, efn$cave2 == 2);
    }
}
