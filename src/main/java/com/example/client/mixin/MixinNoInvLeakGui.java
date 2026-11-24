package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.efn.client.module.impl.misc.noinvleak.StreamOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({InGameHud.class})
public abstract class MixinNoInvLeakGui {

    @Unique
    private static final Identifier EFN$SELECTION = Identifier.ofVanilla("hud/hotbar_selection");

    @Shadow private int heldItemTooltipFade;

    @Shadow private ItemStack currentStack;

    public MixinNoInvLeakGui() {}

    @ModifyVariable(
            method = {"renderHotbarItem"},
            at = {@At("HEAD")},
            argsOnly = true)
    private ItemStack efn$noInvLeakHotbar(
            ItemStack stack,
            @Local(argsOnly = true) DrawContext context,
            @Local(argsOnly = true, ordinal = 0) int i,
            @Local(argsOnly = true, ordinal = 1) int j,
            @Local(argsOnly = true, ordinal = 2) int k) {
        return NoInvLeakModule.hotbar(
                stack,
                NoInvLeakModule.screenX(context, i, j, i),
                NoInvLeakModule.screenY(context, i, j, j),
                k);
    }

    @Inject(
            method = {"renderHeldItemTooltip"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$noInvLeakItemName(DrawContext context, CallbackInfo ci) {
        if (NoInvLeakModule.hidesItemName()) {
            if (NoInvLeakModule.overlaying()
                    && this.heldItemTooltipFade > 0
                    && !this.currentStack.isEmpty()) {
                int i = Math.min(255, (int) (this.heldItemTooltipFade * 256.0F / 10.0F));
                int j = context.getScaledWindowHeight() - 59;
                if (!MinecraftClient.getInstance().interactionManager.hasStatusBars()) {
                    j += 14;
                }
                StreamOverlay.itemName(this.currentStack, i, j);
            }
            ci.cancel();
        }
    }

    @WrapWithCondition(
            method = {"renderHotbar"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V")
            })
    private boolean efn$noInvLeakSelection(
            DrawContext context,
            RenderPipeline renderPipeline,
            Identifier id,
            int i,
            int j,
            int k,
            int m) {
        if (NoInvLeakModule.hidesSelectedSlot() && EFN$SELECTION.equals(id)) {
            if (NoInvLeakModule.overlaying()) {
                StreamOverlay.selection(i, j);
            }
            return false;
        } else {
            return true;
        }
    }
}
