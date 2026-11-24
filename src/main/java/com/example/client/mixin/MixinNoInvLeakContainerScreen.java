package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.efn.client.module.impl.misc.noinvleak.StreamOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({HandledScreen.class})
public abstract class MixinNoInvLeakContainerScreen {

    @Shadow protected Slot focusedSlot;

    @Shadow protected int x;

    @Shadow protected int y;

    public MixinNoInvLeakContainerScreen() {}

    @Shadow
    public abstract ScreenHandler getScreenHandler();

    @WrapOperation(
            method = {"drawSlot"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/screen/slot/Slot;getStack()Lnet/minecraft/item/ItemStack;")
            })
    private ItemStack efn$noInvLeakSlot(
            Slot slot,
            Operation<ItemStack> operation,
            @Local(argsOnly = true) DrawContext context) {
        ItemStack stack = (ItemStack) operation.call(slot);
        if (!NoInvLeakModule.ownSlot(slot)) {
            return stack;
        } else {
            int screenX = NoInvLeakModule.screenX(context, slot.x, slot.y, this.x + slot.x);
            int screenY = NoInvLeakModule.screenY(context, slot.x, slot.y, this.y + slot.y);
            return NoInvLeakModule.container(
                    stack, slot, screenX, screenY, slot == this.focusedSlot);
        }
    }

    @WrapOperation(
            method = {"drawSlot"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/item/ItemStack;copyWithCount(I)Lnet/minecraft/item/ItemStack;")
            })
    private ItemStack efn$noInvLeakDragPreview(
            ItemStack stack, int i, Operation<ItemStack> operation) {
        return NoInvLeakModule.dragged((ItemStack) operation.call(stack, i));
    }

    @WrapOperation(
            method = {"drawSlot"},
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/screen/slot/Slot;getBackgroundSprite()Lnet/minecraft/util/Identifier;")
            })
    private Identifier efn$noInvLeakSilhouette(Slot slot, Operation<Identifier> operation) {
        Identifier id = (Identifier) operation.call(slot);
        return id != null
                        && NoInvLeakModule.overlaying()
                        && NoInvLeakModule.ownSlot(slot)
                        && slot.hasStack()
                ? null
                : id;
    }

    @ModifyVariable(
            method = {"drawItem"},
            at = {@At("HEAD")},
            argsOnly = true)
    private ItemStack efn$noInvLeakCarried(
            ItemStack stack,
            @Local(argsOnly = true, ordinal = 0) int i,
            @Local(argsOnly = true, ordinal = 1) int j) {
        return NoInvLeakModule.carried(stack, i, j);
    }

    @Inject(
            method = {"drawMouseoverTooltip"},
            at = {@At("HEAD")},
            cancellable = true)
    private void efn$noInvLeakTooltip(DrawContext context, int i, int j, CallbackInfo ci) {
        if (NoInvLeakModule.hidesTooltips() && NoInvLeakModule.ownSlot(this.focusedSlot)) {
            if (NoInvLeakModule.overlaying()
                    && this.focusedSlot.hasStack()
                    && this.getScreenHandler().getCursorStack().isEmpty()) {
                StreamOverlay.tooltip(this.focusedSlot.getStack(), i, j);
            }
            ci.cancel();
        }
    }
}
