/*
 * Decompiled with CFR 0.152.
 */
package com.example.client.mixin;

import net.efn.client.module.impl.misc.FakeStats;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value = EnvType.CLIENT)
@Mixin(value = {Scoreboard.class})
public abstract class MixinFakeStatsScoreboard {

    @Inject(
            method = {"setObjectiveSlot"},
            at = {@At(value = "HEAD")},
            cancellable = true)
    private void efn$fakeSidebar(
            ScoreboardDisplaySlot scoreboardDisplaySlot,
            ScoreboardObjective scoreboardObjective,
            CallbackInfo ci) {
        if (FakeStats.redirect(
                (Scoreboard) ((Object) this), scoreboardDisplaySlot, scoreboardObjective)) {
            ci.cancel();
        }
    }
}
