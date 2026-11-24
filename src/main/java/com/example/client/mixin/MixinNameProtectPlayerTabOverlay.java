package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.efn.client.module.impl.misc.NameProtectModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin({PlayerListHud.class})
public abstract class MixinNameProtectPlayerTabOverlay {

    public MixinNameProtectPlayerTabOverlay() {}

    @ModifyReturnValue(
            method = {
                "getPlayerName(Lnet/minecraft/client/network/PlayerListEntry;)Lnet/minecraft/text/Text;"
            },
            at = {@At("RETURN")})
    private Text efn$nameProtectTabName(Text text) {
        return NameProtectModule.protect(text);
    }

    @ModifyArg(
            method = {
                "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        ordinal = 0,
                        target =
                                "Lnet/minecraft/client/font/TextRenderer;wrapLines(Lnet/minecraft/text/StringVisitable;I)Ljava/util/List;")
            },
            index = 0)
    private StringVisitable efn$nameProtectTabHeader(StringVisitable stringVisitable) {
        return NameProtectModule.protectText(stringVisitable);
    }

    @ModifyArg(
            method = {
                "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        ordinal = 1,
                        target =
                                "Lnet/minecraft/client/font/TextRenderer;wrapLines(Lnet/minecraft/text/StringVisitable;I)Ljava/util/List;")
            },
            index = 0)
    private StringVisitable efn$nameProtectTabFooter(StringVisitable stringVisitable) {
        return NameProtectModule.protectText(stringVisitable);
    }

    @WrapOperation(
            method = {
                "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/network/PlayerListEntry;getSkinTextures()Lnet/minecraft/entity/player/SkinTextures;")
            })
    private SkinTextures efn$nameProtectTabHead(
            PlayerListEntry listEntry, Operation<SkinTextures> operation) {
        SkinTextures skinTextures = (SkinTextures) operation.call(listEntry);
        return NameProtectModule.hidesName(listEntry.getProfile().name())
                ? NameProtectModule.hiddenHead(listEntry.getProfile().id(), skinTextures)
                : skinTextures;
    }
}
