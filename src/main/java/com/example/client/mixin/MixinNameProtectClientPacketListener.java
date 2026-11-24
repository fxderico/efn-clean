package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.efn.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({ClientPlayNetworkHandler.class})
public abstract class MixinNameProtectClientPacketListener {

    public MixinNameProtectClientPacketListener() {}

    @Inject(
            method = {"onPlayerList(Lnet/minecraft/network/packet/s2c/play/PlayerListS2CPacket;)V"},
            at = {@At("TAIL")})
    private void efn$nameProtectPlayerList(PlayerListS2CPacket playerListPacket, CallbackInfo ci) {
        NameProtectModule.onPlayerInfoApplied(playerListPacket);
    }

    @ModifyExpressionValue(
            method = {
                "onDeathMessage(Lnet/minecraft/network/packet/s2c/play/DeathMessageS2CPacket;)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/network/packet/s2c/play/DeathMessageS2CPacket;message()Lnet/minecraft/text/Text;")
            })
    private Text efn$nameProtectDeathMessage(Text text) {
        return NameProtectModule.protect(text);
    }
}
