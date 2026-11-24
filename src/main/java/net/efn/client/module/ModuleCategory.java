package net.efn.client.module;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public final class ModuleCategory {

    private static final List<ModuleCategory> REGISTERED = new ArrayList<>();

    private static volatile List<ModuleCategory> values = List.of();

    public static final ModuleCategory COMBAT =
            register("COMBAT", "Combat", ModuleCategory.Icon.CROSSHAIR);

    public static final ModuleCategory MISC = register("MISC", "Misc", ModuleCategory.Icon.GRID);

    public static final ModuleCategory DONUT = register("DONUT", "Donut", ModuleCategory.Icon.RING);

    public static final ModuleCategory RENDER =
            register("RENDER", "Render", ModuleCategory.Icon.EYE);

    public static final ModuleCategory CLIENT =
            register("CLIENT", "EFN Client", ModuleCategory.Icon.BOLT);

    public static final ModuleCategory MOVEMENT =
            register("MOVEMENT", "Movement", ModuleCategory.Icon.MOVE);

    public static final ModuleCategory BASE_RAID =
            register("BASE_RAID", "Base Finding", ModuleCategory.Icon.HOUSE);

    public static final ModuleCategory VISUALS =
            register("VISUALS", "Visuals", ModuleCategory.Icon.MONITOR);

    public static final ModuleCategory DONUT_MACROS =
            register("DONUT_MACROS", "Macro", ModuleCategory.Icon.SCROLL);

    private final String id;

    private final String displayName;

    private final ModuleCategory.Icon icon;

    private ModuleCategory(String id2, String displayName2, ModuleCategory.Icon icon2) {
        this.id = id2;
        this.displayName = displayName2;
        this.icon = icon2;
    }

    public static synchronized ModuleCategory register(
            String string, String string2, ModuleCategory.Icon icon2) {
        if (!isValidId(string)) {
            throw new IllegalArgumentException(
                    "Category id must be upper-case letters, digits and '_': " + string);
        } else {
            for (ModuleCategory moduleCategory : REGISTERED) {
                if (moduleCategory.id.equals(string)) {
                    throw new IllegalArgumentException("Duplicate category id: " + string);
                }
            }
            ModuleCategory moduleCategory2 =
                    new ModuleCategory(
                            string,
                            Objects.requireNonNull(string2, "displayName"),
                            Objects.requireNonNull(icon2, "icon"));
            REGISTERED.add(moduleCategory2);
            values = List.copyOf(REGISTERED);
            return moduleCategory2;
        }
    }

    private static boolean isValidId(String string) {
        if (string != null
                && !string.isEmpty()
                && string.charAt(0) >= 'A'
                && string.charAt(0) <= 'Z') {
            for (int i = 1; i < string.length(); i++) {
                char c = string.charAt(i);
                if ((c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '_') {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static ModuleCategory register(String string, String string2) {
        return register(string, string2, ModuleCategory.Icon.BOLT);
    }

    public static List<ModuleCategory> values() {
        return values;
    }

    public String name() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public ModuleCategory.Icon icon() {
        return this.icon;
    }

    @Override
    public String toString() {
        return this.id;
    }

    @Environment(EnvType.CLIENT)
    public static enum Icon {
        CROSSHAIR,
        CHEVRON,
        GRID,
        RING,
        EYE,
        MONITOR,
        HOUSE,
        SCROLL,
        MOVE,
        BOLT;

        private Icon() {}
    }
}
