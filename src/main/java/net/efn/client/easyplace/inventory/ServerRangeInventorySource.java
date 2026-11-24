package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
public final class ServerRangeInventorySource implements InventoryPickSource {

    private static final int SYNC_TIMEOUT_TICKS = 12;

    private static final int HOTBAR_SIZE = 9;

    private static final int MAIN_END = 35;

    private static final int OFFHAND_SLOT = 40;

    private final MinecraftClient mc;

    private ServerRangeInventorySource.PendingPick pending;

    public ServerRangeInventorySource(MinecraftClient mc2) {
        this.mc = mc2;
    }

    @Override
    public InventoryPickResult prepare(Item item2, long l) {
        return this.prepare(item2, l, false);
    }

    public InventoryPickResult prepare(Item item2, long l, boolean bl) {
        ClientPlayerEntity player = this.mc.player;
        if (player != null && this.mc.interactionManager != null) {
            if (this.pending != null) {
                if (this.pending.item == item2 && l - this.pending.startedTick <= 12L) {
                    return this.pollPending();
                }
                this.pending = null;
            }
            if (player.getOffHandStack().isOf(item2)) {
                return InventoryPickResult.ready(item2, Hand.OFF_HAND, 40);
            } else {
                PlayerInventory inventory = player.getInventory();
                if (player.getMainHandStack().isOf(item2)) {
                    return InventoryPickResult.ready(
                            item2, Hand.MAIN_HAND, inventory.getSelectedSlot());
                } else {
                    int find = find(inventory, item2);
                    if (find < 0) {
                        return InventoryPickResult.missing(item2);
                    } else if (find < 9) {
                        return InventoryPickResult.ready(item2, Hand.MAIN_HAND, find);
                    } else if (player.currentScreenHandler != player.playerScreenHandler) {
                        return InventoryPickResult.missing(item2);
                    } else {
                        int hotbarDestination = findHotbarDestination(inventory);
                        if (bl
                                ? InventoryCalm.settle(this.mc, player)
                                : InventoryCalm.idle(player)) {
                            this.mc.interactionManager.clickSlot(
                                    player.playerScreenHandler.syncId,
                                    find,
                                    hotbarDestination,
                                    SlotActionType.SWAP,
                                    player);
                            if (this.mc.currentScreen == null) {
                                player.networkHandler.sendPacket(
                                        new CloseHandledScreenC2SPacket(
                                                player.playerScreenHandler.syncId));
                            }
                            this.pending =
                                    new ServerRangeInventorySource.PendingPick(
                                            item2, hotbarDestination, l);
                            return InventoryPickResult.waiting(item2, hotbarDestination);
                        } else {
                            return InventoryPickResult.waiting(item2, hotbarDestination);
                        }
                    }
                }
            }
        } else {
            return InventoryPickResult.missing(item2);
        }
    }

    private InventoryPickResult pollPending() {
        if (!this.mc
                .player
                .getInventory()
                .getStack(this.pending.hotbarSlot)
                .isOf(this.pending.item)) {
            return InventoryPickResult.waiting(this.pending.item, this.pending.hotbarSlot);
        } else {
            InventoryPickResult inventoryPickResult =
                    InventoryPickResult.ready(
                            this.pending.item, Hand.MAIN_HAND, this.pending.hotbarSlot);
            this.pending = null;
            return inventoryPickResult;
        }
    }

    private static int find(PlayerInventory playerInventory, Item item2) {
        for (int i = 0; i <= 35; i++) {
            if (playerInventory.getStack(i).isOf(item2)) {
                return i;
            }
        }
        return -1;
    }

    private static int findHotbarDestination(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; i++) {
            if (playerInventory.getStack(i).isEmpty()) {
                return i;
            }
        }
        return playerInventory.getSelectedSlot();
    }

    @Override
    public void reset() {
        this.pending = null;
    }

    @Environment(EnvType.CLIENT)
    private record PendingPick(Item item, int hotbarSlot, long startedTick) {}
}
