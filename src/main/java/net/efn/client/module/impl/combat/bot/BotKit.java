package net.efn.client.module.impl.combat.bot;

import net.efn.client.easyplace.inventory.InventoryCalm;
import net.efn.client.module.impl.combat.AutoInvTotemModule;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
final class BotKit {

    private static final int CLICK_GAP = 4;

    private int lastClick = -4;

    BotKit() {}

    boolean needed(ClientPlayerEntity player, boolean bl) {
        return next(player, wants(bl)) != null;
    }

    boolean refill(MinecraftClient client, ClientPlayerEntity player, boolean bl) {
        if (client.interactionManager != null
                && client.currentScreen == null
                && player.currentScreenHandler == player.playerScreenHandler
                && player.playerScreenHandler.getCursorStack().isEmpty()
                && !AutoInvTotemModule.busy()
                && !player.isUsingItem()) {
            int[] ints = next(player, wants(bl));
            if (ints == null) {
                return false;
            } else {
                int age = player.age;
                if (InventoryCalm.settle(client, player)
                        && (age < this.lastClick || age - this.lastClick >= 4)) {
                    client.interactionManager.clickSlot(
                            player.playerScreenHandler.syncId,
                            ints[0],
                            ints[1],
                            SlotActionType.SWAP,
                            player);
                    player.networkHandler.sendPacket(
                            new CloseHandledScreenC2SPacket(player.playerScreenHandler.syncId));
                    MeleeAttacks.noteAction(player);
                    this.lastClick = age;
                    return true;
                } else {
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private static List<BotKit.Want> wants(boolean bl) {
        ArrayList list = new ArrayList();
        list.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.END_CRYSTAL), 16, 32));
        list.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.OBSIDIAN), 8, 16));
        list.add(new BotKit.Want(MeleeAttacks::isMeleeWeapon, 1, 1));
        list.add(
                new BotKit.Want(
                        itemStack ->
                                itemStack.isOf(Items.ENCHANTED_GOLDEN_APPLE)
                                        || itemStack.isOf(Items.GOLDEN_APPLE),
                        1,
                        1));
        list.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.ENDER_PEARL), 1, 1));
        list.add(new BotKit.Want(itemStack -> itemStack.isIn(ItemTags.PICKAXES), 1, 1));
        list.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.EXPERIENCE_BOTTLE), 1, 1));
        if (bl) {
            list.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.RESPAWN_ANCHOR), 2, 4));
            list.add(new BotKit.Want(itemStack -> itemStack.isOf(Items.GLOWSTONE), 2, 4));
        }
        return list;
    }

    private static int[] next(ClientPlayerEntity player, List<BotKit.Want> list) {
        PlayerInventory inventory = player.getInventory();
        for (BotKit.Want want : list) {
            int i = 0;
            int j = -1;
            for (int k = 0; k < 9; k++) {
                ItemStack stack = inventory.getStack(k);
                if (want.item().test(stack)) {
                    i += stack.getCount();
                    if (j < 0 || stack.getCount() < inventory.getStack(j).getCount()) {
                        j = k;
                    }
                }
            }
            if (i < want.low()) {
                int m = -1;
                for (int n = 9; n < 36; n++) {
                    ItemStack stack2 = inventory.getStack(n);
                    if (want.item().test(stack2)
                            && (m < 0 || stack2.getCount() > inventory.getStack(m).getCount())) {
                        m = n;
                    }
                }
                if (m >= 0) {
                    int count = inventory.getStack(m).getCount();
                    if (j >= 0 && count > i && (i < want.topUp() || count >= i + 8)) {
                        return new int[] {m, j};
                    }
                    int freeSlot;
                    if (j < 0 && (freeSlot = freeSlot(inventory, list)) >= 0) {
                        return new int[] {m, freeSlot};
                    }
                }
            }
        }
        return null;
    }

    private static int freeSlot(PlayerInventory playerInventory, List<BotKit.Want> list) {
        for (int i = 0; i < 9; i++) {
            if (playerInventory.getStack(i).isEmpty()) {
                return i;
            }
        }
        for (int j = 8; j >= 0; j--) {
            ItemStack stack = playerInventory.getStack(j);
            if (!stack.isOf(Items.TOTEM_OF_UNDYING)) {
                boolean bl = false;
                for (BotKit.Want want : list) {
                    bl |= want.item().test(stack);
                }
                if (!bl) {
                    return j;
                }
            }
        }
        return -1;
    }

    @Environment(EnvType.CLIENT)
    private record Want(Predicate<ItemStack> item, int low, int topUp) {}
}
