package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum Detail {
    LOW("Low", 7500, 0.42F),
    MEDIUM("Medium", 15000, 0.3F),
    HIGH("High", 30000, 0.21F);

    public final String label;

    public final int budget;

    public final float step;

    private Detail(String label2, int budget2, float step2) {
        this.label = label2;
        this.budget = budget2;
        this.step = step2;
    }

    public static Detail byLabel(String string) {
        for (Detail detail : values()) {
            if (detail.label.equalsIgnoreCase(string) || detail.name().equalsIgnoreCase(string)) {
                return detail;
            }
        }
        return MEDIUM;
    }
}
