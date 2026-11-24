package net.efn.client.easyplace.builder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.TransparentBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;

@Environment(EnvType.CLIENT)
final class Scaffold {

    private Scaffold() {}

    static boolean item(ItemStack stack) {
        return glass(stack) || stack.isIn(ItemTags.LEAVES);
    }

    static boolean glass(ItemStack stack) {
        Item item = stack.getItem();
        BlockItem blockItem;
        return item instanceof BlockItem
                && (blockItem = (BlockItem) item).getBlock() instanceof TransparentBlock;
    }

    static boolean block(BlockState state) {
        return state.isIn(BlockTags.LEAVES) || state.getBlock() instanceof TransparentBlock;
    }

    static int count(ClientPlayerEntity player) {
        int i = 0;
        PlayerInventory inventory = player.getInventory();
        for (int j = 0; j < 36; j++) {
            if (item(inventory.getStack(j))) {
                i += inventory.getStack(j).getCount();
            }
        }
        if (item(player.getOffHandStack())) {
            i += player.getOffHandStack().getCount();
        }
        return i;
    }

    static boolean has(ClientPlayerEntity player) {
        return Hands.has(player, Scaffold::item);
    }

    static boolean hold(MinecraftClient client, ClientPlayerEntity player) {
        if (Hands.has(player, itemStack -> itemStack.isOf(Items.GLASS))) {
            return Hands.hold(client, player, itemStack -> itemStack.isOf(Items.GLASS));
        } else {
            return Hands.has(player, itemStack -> itemStack.isIn(ItemTags.LEAVES))
                    ? Hands.hold(client, player, itemStack -> itemStack.isIn(ItemTags.LEAVES))
                    : Hands.hold(client, player, Scaffold::glass);
        }
    }
}
