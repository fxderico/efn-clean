package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum InventoryPickSourceMode {
    SERVER_RANGE("Server Range"),
    HOTBAR_ONLY("Hotbar Only");

    private final String title;

    private InventoryPickSourceMode(String title2) {
        this.title = title2;
    }

    public String title() {
        return this.title;
    }

    public static String[] titles() {
        InventoryPickSourceMode[] inventoryPickSourceModes = values();
        String[] strings = new String[inventoryPickSourceModes.length];
        for (int i = 0; i < inventoryPickSourceModes.length; i++) {
            strings[i] = inventoryPickSourceModes[i].title;
        }
        return strings;
    }

    public static InventoryPickSourceMode of(String string) {
        for (InventoryPickSourceMode inventoryPickSourceMode : values()) {
            if (inventoryPickSourceMode.title.equals(string)) {
                return inventoryPickSourceMode;
            }
        }
        return HOTBAR_ONLY;
    }

    @Override
    public String toString() {
        return this.title;
    }
}
