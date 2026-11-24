package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;

@Environment(EnvType.CLIENT)
public final class InventoryManager {

    private final MinecraftClient mc;

    private final ServerRangeInventorySource serverRange;

    public InventoryManager(MinecraftClient mc2) {
        this.mc = mc2;
        this.serverRange = new ServerRangeInventorySource(mc2);
    }

    public InventoryPickResult prepare(
            InventoryPickSourceMode inventoryPickSourceMode, Item item, long l, boolean bl) {
        return switch (inventoryPickSourceMode) {
            case SERVER_RANGE -> this.serverRange.prepare(item, l, bl);
            case HOTBAR_ONLY -> this.hotbarOnly(item);
        };
    }

    private InventoryPickResult hotbarOnly(Item item) {
        if (this.mc.player == null) {
            return InventoryPickResult.missing(item);
        } else {
            for (int i = 0; i < 9; i++) {
                if (this.mc.player.getInventory().getStack(i).getItem() == item) {
                    return InventoryPickResult.ready(item, null, i);
                }
            }
            return InventoryPickResult.missing(item);
        }
    }

    public void reset() {
        this.serverRange.reset();
    }
}
