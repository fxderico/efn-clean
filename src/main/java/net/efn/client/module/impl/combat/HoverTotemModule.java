package net.efn.client.module.impl.combat;

import com.example.client.mixin.AccessorAbstractContainerScreen;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@Environment(EnvType.CLIENT)
public final class HoverTotemModule extends ClientModule {

    private final BooleanSetting hotbarRefill =
            this.addSetting("hotbar-refill", new BooleanSetting("Hotbar Refill", false));

    private final NumberSetting hotbarSlot =
            this.addSetting(
                    "hotbar-slot", new NumberSetting("Hotbar Slot", 1.0F, 1.0F, 9.0F).step(1.0F));

    public HoverTotemModule() {
        super(
                "hovertotem",
                "Hover Totem",
                "Swaps a hovered totem from any open container into the offhand or selected hotbar"
                        + " slot.",
                ModuleCategory.COMBAT);
        this.hotbarSlot.visibleWhen(this.hotbarRefill::get);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && client.interactionManager != null && !player.isInCreativeMode()) {
            if (client.currentScreen instanceof HandledScreen handledScreen
                    && AutoInvTotemModule.clickSafe(player)
                    && !AutoInvTotemModule.busy()) {
                ScreenHandler screenHandler = handledScreen.getScreenHandler();
                Slot slot = ((AccessorAbstractContainerScreen) handledScreen).efn$getHoveredSlot();
                if (slot != null
                        && screenHandler.getCursorStack().isEmpty()
                        && slot.getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                    if (slot.inventory != player.getInventory() || slot.getIndex() != 40) {
                        int i;
                        if (!player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                            i = 40;
                        } else {
                            if (!this.hotbarRefill.get()) {
                                return;
                            }
                            i = this.hotbarSlot.getInt() - 1;
                            if (player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                                return;
                            }
                        }
                        client.interactionManager.clickSlot(
                                screenHandler.syncId, slot.id, i, SlotActionType.SWAP, player);
                    }
                }
            }
        }
    }
}
