package net.efn.client.module.impl.combat;

import com.example.client.mixin.AccessorCombatMinecraft;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@Environment(EnvType.CLIENT)
public final class FastXpModule extends ClientModule {

    private final NumberSetting delay =
            this.addSetting("delay", new NumberSetting("Delay", 0.0F, 0.0F, 3.0F).step(1.0F));

    public FastXpModule() {
        super(
                "fastxp",
                "FastXP",
                "Throws experience bottles faster while use is held.",
                ModuleCategory.COMBAT);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && client.currentScreen == null && client.options.useKey.isPressed()) {
            if (!player.isUsingItem() && holdsXp(player)) {
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

    private static boolean holdsXp(ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        return mainHandStack.isOf(Items.EXPERIENCE_BOTTLE)
                || player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)
                        && !(mainHandStack.getItem() instanceof BlockItem);
    }
}
