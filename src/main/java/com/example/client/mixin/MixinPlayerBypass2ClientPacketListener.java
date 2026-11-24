package com.example.client.mixin;

import net.efn.client.util.playerbypass2.SimulationSignals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.SimulationDistanceS2CPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayNetworkHandler.class})
public abstract class MixinPlayerBypass2ClientPacketListener {

    private static final String ON_MAIN_THREAD =
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V";

    public MixinPlayerBypass2ClientPacketListener() {}

    @Inject(
            method = {"onGameJoin"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2Login(GameJoinS2CPacket gameJoinPacket, CallbackInfo ci) {
        SimulationSignals.onLogin(
                gameJoinPacket.simulationDistance(),
                gameJoinPacket.viewDistance(),
                gameJoinPacket.commonPlayerSpawnInfo().dimension());
    }

    @Inject(
            method = {"onPlayerRespawn"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2Respawn(PlayerRespawnS2CPacket playerRespawnPacket, CallbackInfo ci) {
        SimulationSignals.onRespawn(playerRespawnPacket.commonPlayerSpawnInfo().dimension());
    }

    @Inject(
            method = {"onPlayerPositionLook"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2Teleport(
            PlayerPositionLookS2CPacket playerPositionLookPacket, CallbackInfo ci) {
        SimulationSignals.onTeleport();
    }

    @Inject(
            method = {"onSimulationDistance"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2Simulation(
            SimulationDistanceS2CPacket simulationDistancePacket, CallbackInfo ci) {
        SimulationSignals.onSimulationDistance(simulationDistancePacket.simulationDistance());
    }

    @Inject(
            method = {"onChunkLoadDistance"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2View(ChunkLoadDistanceS2CPacket chunkLoadDistancePacket, CallbackInfo ci) {
        SimulationSignals.onViewDistance(chunkLoadDistancePacket.getDistance());
    }

    @Inject(
            method = {"onChunkData"},
            at = {@At("TAIL")})
    private void efn$pb2Chunk(ChunkDataS2CPacket chunkDataPacket, CallbackInfo ci) {
        SimulationSignals.onChunk(chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ());
    }

    @Inject(
            method = {"onEntity"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2Move(EntityS2CPacket entityPacket, CallbackInfo ci) {
        SimulationSignals.onMove(entityPacket);
    }

    @Inject(
            method = {"onEntityPositionSync"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2PositionSync(
            EntityPositionSyncS2CPacket entityPositionSyncPacket, CallbackInfo ci) {
        SimulationSignals.onPositionSync(entityPositionSyncPacket);
    }

    @Inject(
            method = {"onEntitySetHeadYaw"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2Head(EntitySetHeadYawS2CPacket entitySetHeadYawPacket, CallbackInfo ci) {
        SimulationSignals.onHeadRotation(entitySetHeadYawPacket);
    }

    @Inject(
            method = {"onEntityTrackerUpdate"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2EntityData(
            EntityTrackerUpdateS2CPacket entityTrackerUpdatePacket, CallbackInfo ci) {
        SimulationSignals.onEntityData(entityTrackerUpdatePacket);
    }

    @Inject(
            method = {"onEntityStatus"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2EntityEvent(EntityStatusS2CPacket entityStatusPacket, CallbackInfo ci) {
        SimulationSignals.onEntityEvent(entityStatusPacket);
    }

    @Inject(
            method = {"onEntitySpawn"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2AddEntity(EntitySpawnS2CPacket entitySpawnPacket, CallbackInfo ci) {
        SimulationSignals.onAddEntity(entitySpawnPacket);
    }

    @Inject(
            method = {"onEntitiesDestroy"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2RemoveEntities(
            EntitiesDestroyS2CPacket entitiesDestroyPacket, CallbackInfo ci) {
        SimulationSignals.onRemoveEntities(entitiesDestroyPacket.getEntityIds());
    }

    @Inject(
            method = {"onBlockUpdate"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2Block(BlockUpdateS2CPacket blockUpdatePacket, CallbackInfo ci) {
        SimulationSignals.onBlockUpdate(blockUpdatePacket);
    }

    @Inject(
            method = {"onChunkDeltaUpdate"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2Section(ChunkDeltaUpdateS2CPacket chunkDeltaUpdatePacket, CallbackInfo ci) {
        SimulationSignals.onSectionUpdate(chunkDeltaUpdatePacket);
    }

    @Inject(
            method = {"onItemPickupAnimation"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2TakeItem(
            ItemPickupAnimationS2CPacket itemPickupAnimationPacket, CallbackInfo ci) {
        SimulationSignals.onTakeItem(itemPickupAnimationPacket);
    }

    @Inject(
            method = {"onEntityDamage"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2Damage(EntityDamageS2CPacket entityDamagePacket, CallbackInfo ci) {
        SimulationSignals.onDamage(entityDamagePacket);
    }

    @Inject(
            method = {"onEntityEquipmentUpdate"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/network/PacketApplyBatcher;)V",
                        shift = Shift.AFTER)
            })
    private void efn$pb2Equipment(
            EntityEquipmentUpdateS2CPacket entityEquipmentUpdatePacket, CallbackInfo ci) {
        SimulationSignals.onEquipment(entityEquipmentUpdatePacket);
    }
}
