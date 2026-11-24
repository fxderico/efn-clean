package net.efn.client.module.impl.movement.elytrafly;

import com.example.client.mixin.AccessorElytraFlyMultiPlayerGameMode;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class ElytraFlyInventory {

    public static final int OFFHAND = 40;

    private static final int CHEST_ARMOR_ID = 6;

    private static int previousSlot = -1;

    private ElytraFlyInventory() {}

    public static int find(Predicate<ItemStack> predicate) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player == null ? -1 : find(predicate, 0, player.getInventory().size() - 1);
    }

    public static int find(Predicate<ItemStack> predicate, int i, int j) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return -1;
        } else {
            PlayerInventory inventory = player.getInventory();
            for (int k = i; k <= j; k++) {
                if (predicate.test(inventory.getStack(k))) {
                    return k;
                }
            }
            return -1;
        }
    }

    public static int findInHotbar(Predicate<ItemStack> predicate) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return -1;
        } else if (predicate.test(player.getOffHandStack())) {
            return 40;
        } else {
            return predicate.test(player.getMainHandStack())
                    ? player.getInventory().getSelectedSlot()
                    : find(predicate, 0, 8);
        }
    }

    public static boolean isHotbar(int i) {
        return i >= 0 && i <= 8;
    }

    public static void moveToHotbar(int i, int j) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            move(player, indexToId(player, i), indexToId(player, j));
        }
    }

    public static void moveToChest(int i) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            int indexToId = indexToId(player, i);
            move(player, indexToId, indexToId == -1 ? -1 : 6);
        }
    }

    private static void move(ClientPlayerEntity player, int i, int j) {
        ClientPlayerInteractionManager interactionManager =
                MinecraftClient.getInstance().interactionManager;
        if (interactionManager != null && i != -1 && j != -1) {
            ScreenHandler currentScreenHandler = player.currentScreenHandler;
            boolean bl = currentScreenHandler.getCursorStack().isEmpty();
            interactionManager.clickSlot(
                    currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, player);
            interactionManager.clickSlot(
                    currentScreenHandler.syncId, j, 0, SlotActionType.PICKUP, player);
            if (bl && !currentScreenHandler.getCursorStack().isEmpty()) {
                interactionManager.clickSlot(
                        currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, player);
            }
        }
    }

    private static int indexToId(ClientPlayerEntity player, int i) {
        if (player.currentScreenHandler != player.playerScreenHandler) {
            return -1;
        } else if (i >= 0 && i <= 8) {
            return 36 + i;
        } else if (i >= 9 && i < 36) {
            return i;
        } else if (i >= 36 && i <= 39) {
            return 8 - (i - 36);
        } else {
            return i == 40 ? 45 : -1;
        }
    }

    public static boolean swap(int selectedSlot, boolean bl) {
        if (selectedSlot == 40) {
            return true;
        } else if (selectedSlot >= 0 && selectedSlot <= 8) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player != null && client.interactionManager != null) {
                if (bl && previousSlot == -1) {
                    previousSlot = player.getInventory().getSelectedSlot();
                } else if (!bl) {
                    previousSlot = -1;
                }
                player.getInventory().setSelectedSlot(selectedSlot);
                ((AccessorElytraFlyMultiPlayerGameMode) client.interactionManager)
                        .efn$elytraFlySyncSelected();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean swapBack() {
        if (previousSlot == -1) {
            return false;
        } else {
            boolean swap = swap(previousSlot, false);
            previousSlot = -1;
            return swap;
        }
    }

    public static void chestSwap() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            ItemStack equippedStack = player.getEquippedStack(EquipmentSlot.CHEST);
            EquippableComponent equippableComponent =
                    equippedStack.get(DataComponentTypes.EQUIPPABLE);
            if (equippedStack.contains(DataComponentTypes.GLIDER)) {
                equipChestplate(player);
            } else if (equippableComponent != null
                    && equippableComponent.slot().getEntitySlotId()
                            == EquipmentSlot.CHEST.getEntitySlotId()) {
                equipElytra(player);
            } else if (!equipChestplate(player)) {
                equipElytra(player);
            }
        }
    }

    private static boolean equipChestplate(ClientPlayerEntity player) {
        DefaultedList mainStacks = player.getInventory().getMainStacks();
        int i = -1;
        for (int j = 0; j < mainStacks.size(); j++) {
            Item item = ((ItemStack) mainStacks.get(j)).getItem();
            if (item == Items.DIAMOND_CHESTPLATE) {
                i = j;
            } else if (item == Items.NETHERITE_CHESTPLATE) {
                i = j;
                break;
            }
        }
        if (i != -1) {
            equip(player, i);
        }
        return i != -1;
    }

    private static void equipElytra(ClientPlayerEntity player) {
        DefaultedList mainStacks = player.getInventory().getMainStacks();
        for (int i = 0; i < mainStacks.size(); i++) {
            if (((ItemStack) mainStacks.get(i)).contains(DataComponentTypes.GLIDER)) {
                equip(player, i);
                break;
            }
        }
    }

    private static void equip(ClientPlayerEntity player, int i) {
        if (indexToId(player, i) != -1) {
            moveToChest(i);
            player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
        }
    }
}
