package net.efn.client.easyplace.builder;

import net.efn.client.easyplace.inventory.InventoryCalm;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
final class Hands {

    private static final long SWAP_WAIT_NANOS = 1000000000L;

    private static long swapSentAt = System.nanoTime() - 1000000000L;

    private Hands() {}

    static boolean hold(
            MinecraftClient client, ClientPlayerEntity player, Predicate<ItemStack> predicate) {
        PlayerInventory inventory = player.getInventory();
        if (predicate.test(player.getMainHandStack())) {
            return true;
        } else {
            for (int i = 0; i < 9; i++) {
                if (predicate.test(inventory.getStack(i))) {
                    inventory.setSelectedSlot(i);
                    return true;
                }
            }
            if (player.currentScreenHandler == player.playerScreenHandler
                    && client.interactionManager != null) {
                for (int j = 9; j < 36; j++) {
                    if (predicate.test(inventory.getStack(j))) {
                        if (!InventoryCalm.settle(client, player)) {
                            return false;
                        }
                        client.interactionManager.clickSlot(
                                player.playerScreenHandler.syncId,
                                j,
                                inventory.getSelectedSlot(),
                                SlotActionType.SWAP,
                                player);
                        close(client, player);
                        return false;
                    }
                }
                return false;
            } else {
                return false;
            }
        }
    }

    static boolean has(ClientPlayerEntity player, Predicate<ItemStack> predicate) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (predicate.test(inventory.getStack(i))) {
                return true;
            }
        }
        return predicate.test(player.getOffHandStack());
    }

    static boolean toOffhand(
            MinecraftClient client, ClientPlayerEntity player, Predicate<ItemStack> predicate) {
        if (player.currentScreenHandler == player.playerScreenHandler
                && client.interactionManager != null
                && client.getNetworkHandler() != null
                && player.playerScreenHandler.getCursorStack().isEmpty()) {
            PlayerInventory inventory = player.getInventory();
            if (predicate.test(player.getMainHandStack())) {
                long l = System.nanoTime();
                if (l - swapSentAt < 1000000000L) {
                    return true;
                } else {
                    swapSentAt = l;
                    client.getNetworkHandler()
                            .sendPacket(
                                    new PlayerActionC2SPacket(
                                            Action.SWAP_ITEM_WITH_OFFHAND,
                                            BlockPos.ORIGIN,
                                            Direction.DOWN));
                    return true;
                }
            } else {
                for (int i = 0; i < 9; i++) {
                    if (predicate.test(inventory.getStack(i))) {
                        inventory.setSelectedSlot(i);
                        return true;
                    }
                }
                for (int j = 9; j < 36; j++) {
                    if (predicate.test(inventory.getStack(j))) {
                        if (!InventoryCalm.settle(client, player)) {
                            return true;
                        }
                        client.interactionManager.clickSlot(
                                player.playerScreenHandler.syncId,
                                j,
                                40,
                                SlotActionType.SWAP,
                                player);
                        close(client, player);
                        return true;
                    }
                }
                return false;
            }
        } else {
            return false;
        }
    }

    private static void close(MinecraftClient client, ClientPlayerEntity player) {
        if (client.currentScreen == null) {
            player.networkHandler.sendPacket(
                    new CloseHandledScreenC2SPacket(player.playerScreenHandler.syncId));
        }
    }
}
