package net.efn.client.util.antivanish;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class AntiVanishHeuristics {

    private AntiVanishHeuristics() {}

    public static boolean suspiciousSound(String string) {
        String path = path(string);
        return path.contains("chest.open")
                || path.contains("chest.close")
                || path.contains("chest.locked")
                || path.contains("barrel.open")
                || path.contains("barrel.close")
                || path.contains("shulker_box.open")
                || path.contains("shulker_box.close")
                || path.contains("door.open")
                || path.contains("door.close")
                || path.contains("fence_gate.open")
                || path.contains("fence_gate.close")
                || path.contains("button.click")
                || path.contains("lever.click");
    }

    public static boolean suspiciousParticle(String string) {
        String path = path(string);
        return path.contains("crit")
                || path.contains("enchanted_hit")
                || path.contains("damage_indicator")
                || path.contains("smoke")
                || path.equals("block");
    }

    public static String path(String string) {
        if (string == null) {
            return "unknown";
        } else {
            int i = string.indexOf(58);
            return (i >= 0 && i + 1 < string.length() ? string.substring(i + 1) : string)
                    .toLowerCase(Locale.ROOT);
        }
    }
}
