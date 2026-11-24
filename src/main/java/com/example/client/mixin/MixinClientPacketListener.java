package com.example.client.mixin;

import net.efn.client.event.impl.ChatSendEvent;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayNetworkHandler.class})
public abstract class MixinClientPacketListener {

    public MixinClientPacketListener() {}

    @Inject(
            method = {"sendChatMessage"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$onSendChat(String string, CallbackInfo ci) {
        if (ChatSendEvent.dispatch(string)) {
            ci.cancel();
        }
    }

    @Inject(
            method = {"onEntityStatus"},
            at = {@At("TAIL")})
    private void efn$afterEntityEvent(EntityStatusS2CPacket entityStatusPacket, CallbackInfo ci) {
        PacketAppliedEvent.dispatch(entityStatusPacket);
    }

    @Inject(
            method = {"onGameJoin"},
            at = {@At("TAIL")})
    private void efn$afterLogin(GameJoinS2CPacket gameJoinPacket, CallbackInfo ci) {
        PacketAppliedEvent.dispatch(gameJoinPacket);
    }

    @Inject(
            method = {"onChunkData"},
            at = {@At("TAIL")})
    private void efn$afterChunkData(ChunkDataS2CPacket chunkDataPacket, CallbackInfo ci) {
        PacketAppliedEvent.dispatch(chunkDataPacket);
    }

    @Inject(
            method = {"onBlockUpdate"},
            at = {@At("TAIL")})
    private void efn$afterBlockUpdate(BlockUpdateS2CPacket blockUpdatePacket, CallbackInfo ci) {
        PacketAppliedEvent.dispatch(blockUpdatePacket);
    }

    @Inject(
            method = {"onChunkDeltaUpdate"},
            at = {@At("TAIL")})
    private void efn$afterSectionUpdate(
            ChunkDeltaUpdateS2CPacket chunkDeltaUpdatePacket, CallbackInfo ci) {
        PacketAppliedEvent.dispatch(chunkDeltaUpdatePacket);
    }

    @Inject(
            method = {"onLightUpdate"},
            at = {@At("TAIL")})
    private void efn$afterLightUpdate(LightUpdateS2CPacket lightUpdatePacket, CallbackInfo ci) {
        PacketAppliedEvent.dispatch(lightUpdatePacket);
    }

    @Inject(
            method = {"onEntityVelocityUpdate"},
            at = {@At("TAIL")})
    private void efn$afterEntityMotion(
            EntityVelocityUpdateS2CPacket entityVelocityUpdatePacket, CallbackInfo ci) {
        PacketAppliedEvent.dispatch(entityVelocityUpdatePacket);
    }
}
