package net.efn.client.module.impl.misc;

import com.example.client.mixin.AccessorCombatMinecraft;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class FastUseModule extends ClientModule {

    private final NumberSetting delay =
            this.addSetting("delay", new NumberSetting("Delay", 0.0F, 0.0F, 3.0F).step(1.0F));

    private final BooleanSetting blocks =
            this.addSetting("blocks", new BooleanSetting("Blocks", true));

    private final BooleanSetting items =
            this.addSetting("items", new BooleanSetting("Items", true));

    public FastUseModule() {
        super(
                "fast-use",
                "Fast Use",
                "Places blocks and uses items faster while use is held.",
                ModuleCategory.MISC);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && client.currentScreen == null && client.options.useKey.isPressed()) {
            if (!player.isUsingItem() && this.covers(player)) {
                AccessorCombatMinecraft accessorCombatMinecraft = (AccessorCombatMinecraft) client;
                int efn$rightClickDelay = accessorCombatMinecraft.efn$rightClickDelay();
                if (!ServerRotation.pending() && !MeleeAttacks.actedThisTick(player)) {
                    if (efn$rightClickDelay > this.delay.getInt() + 1) {
                        accessorCombatMinecraft.efn$setRightClickDelay(this.delay.getInt() + 1);
                    }
                } else {
                    accessorCombatMinecraft.efn$setRightClickDelay(
                            Math.max(efn$rightClickDelay, 2));
                    KeyBinding useKey = client.options.useKey;
                    while (useKey.wasPressed()) {}
                }
            }
        }
    }

    private boolean covers(ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        if (mainHandStack.isEmpty()) {
            mainHandStack = player.getOffHandStack();
        }
        if (mainHandStack.isEmpty()) {
            return false;
        } else {
            return mainHandStack.getItem() instanceof BlockItem
                    ? this.blocks.get()
                    : this.items.get();
        }
    }
}
