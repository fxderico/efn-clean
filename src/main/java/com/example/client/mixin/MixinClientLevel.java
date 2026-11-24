package com.example.client.mixin;

import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.WorldEntityAddEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientWorld.class})
public abstract class MixinClientLevel {

    public MixinClientLevel() {}

    @Inject(
            method = {"addEntity"},
            at = {@At("HEAD")})
    private void efn$onEntityAdd(Entity entity, CallbackInfo ci) {
        WorldEntityAddEvent.dispatch(entity);
    }

    @Inject(
            method = {"updateListeners"},
            at = {@At("HEAD")})
    private void efn$onBlockUpdated(
            BlockPos pos, BlockState state, BlockState state2, int i, CallbackInfo ci) {
        BlockUpdateEvent.dispatch(pos, state, state2);
    }
}
