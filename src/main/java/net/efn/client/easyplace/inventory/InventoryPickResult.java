package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
public record InventoryPickResult(
        InventoryPickResult.Status status, Item item, Hand hand, int slot) {

    public static InventoryPickResult ready(Item item2, Hand hand2, int i) {
        return new InventoryPickResult(InventoryPickResult.Status.READY, item2, hand2, i);
    }

    public static InventoryPickResult waiting(Item item2, int i) {
        return new InventoryPickResult(InventoryPickResult.Status.WAITING, item2, null, i);
    }

    public static InventoryPickResult missing(Item item2) {
        return new InventoryPickResult(InventoryPickResult.Status.MISSING, item2, null, -1);
    }

    @Environment(EnvType.CLIENT)
    public static enum Status {
        READY,
        WAITING,
        MISSING;

        private Status() {}
    }
}
