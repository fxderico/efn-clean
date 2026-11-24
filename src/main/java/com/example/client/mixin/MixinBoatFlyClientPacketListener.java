package com.example.client.mixin;

import net.efn.client.module.impl.movement.BoatFlyModule;
import net.efn.client.module.impl.movement.BoatNoclipModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayNetworkHandler.class})
public abstract class MixinBoatFlyClientPacketListener {

    public MixinBoatFlyClientPacketListener() {}

    @Inject(
            method = {
                "onVehicleMove(Lnet/minecraft/network/packet/s2c/play/VehicleMoveS2CPacket;)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/packet/c2s/play/VehicleMoveC2SPacket;fromVehicle(Lnet/minecraft/entity/Entity;)Lnet/minecraft/network/packet/c2s/play/VehicleMoveC2SPacket;")
            })
    private void efn$boatFlyVehicleSetback(
            VehicleMoveS2CPacket vehicleMovePacket, CallbackInfo ci) {
        BoatFlyModule active = BoatFlyModule.active;
        if (active != null) {
            active.onVehicleSetback();
        }
        BoatNoclipModule active2 = BoatNoclipModule.active;
        if (BoatNoclipModule.active != null) {
            active2.onVehicleSetback();
        }
    }
}
