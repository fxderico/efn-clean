package net.efn.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.efn.client.EfnClientMod;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class HudLayoutStore {

    public static final String INFO_BAR_ID = "info_bar";

    public static final String COORDINATES_ID = "coordinates";

    public static final String POTION_TRACKER_ID = "potion_tracker";

    public static final String MODULE_LIST_ID = "module_list";

    public static final String REGION_MAP_ID = "region_map";

    public static final String ANTI_VANISH_ID = "anti_vanish";

    public static final String STAFF_LIST_ID = "staff_list";

    public static final String SPOTIFY_ID = "spotify";

    public static final String RADAR_ID = "radar";

    public static final String HOTKEYS_ID = "hotkeys";

    public static final String TARGET_CARD_ID = "target_card";

    public static final String ARMOR_STATUS_ID = "armor_status";

    public static final String ITEM_COOLDOWNS_ID = "item_cooldowns";

    private static final HudLayoutStore INSTANCE = new HudLayoutStore();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int CONFIG_VERSION = 11;

    private static final float DEFAULT_MARGIN = 10.0F;

    private static final float DEFAULT_GAP = 6.0F;

    private static final float CENTER_TOLERANCE = 24.0F;

    static final int ANCHOR_LEFT = 0;

    static final int ANCHOR_CENTER = 1;

    static final int ANCHOR_RIGHT = 2;

    private static final String[] KNOWN_IDS =
            new String[] {
                "info_bar",
                "coordinates",
                "module_list",
                "potion_tracker",
                "region_map",
                "anti_vanish",
                "staff_list",
                "spotify",
                "hotkeys",
                "radar",
                "target_card",
                "armor_status",
                "item_cooldowns"
            };

    private final Map<String, HudLayoutStore.HudElementState> elements = new LinkedHashMap<>();

    private boolean loaded;

    private HudLayoutStore() {}

    public static HudLayoutStore getInstance() {
        return INSTANCE;
    }

    synchronized HudLayoutStore.HudElementState element(String string) {
        this.loadIfNeeded();
        HudLayoutStore.HudElementState element = this.elements.get(string);
        if (element == null) {
            element = defaultElement(string);
            this.elements.put(string, element);
        }
        return element;
    }

    synchronized List<HudLayoutStore.HudElementState> orderedElements() {
        this.loadIfNeeded();
        return List.copyOf(this.elements.values());
    }

    synchronized void updateBounds(String string, float f, float g, float h, float f2) {
        HudLayoutStore.HudElementState hudElementState = this.element(string);
        float g2 = Math.max(1.0F, f);
        float h2 = Math.max(1.0F, g);
        if (hudElementState.positioned
                && hudElementState.autoPlaced
                && h > 0.0F
                && f2 > 0.0F
                && dockMoved(hudElementState, h, f2)) {
            hudElementState.positioned = false;
        }
        if (hudElementState.positioned && !(h <= 0.0F) && !(f2 <= 0.0F)) {
            if (!hudElementState.anchored) {
                this.adoptLoadedPosition(hudElementState, h, f2);
            }
            rebaseToScreen(hudElementState, h, f2);
            float f3 = hudElementState.width - g2;
            if (hudElementState.anchorX == 2) {
                hudElementState.x += f3;
            } else if (hudElementState.anchorX == 1) {
                hudElementState.x += f3 * 0.5F;
            }
            if (hudElementState.anchorBottom) {
                hudElementState.y = hudElementState.y + (hudElementState.height - h2);
            }
            hudElementState.width = g2;
            hudElementState.height = h2;
            clampToScreen(hudElementState, h, f2);
        } else {
            hudElementState.width = g2;
            hudElementState.height = h2;
            if (!hudElementState.positioned
                    && h > 0.0F
                    && f2 > 0.0F
                    && !isCollapsed(hudElementState)) {
                this.placeDefault(hudElementState, h, f2);
            }
        }
    }

    private void adoptLoadedPosition(
            HudLayoutStore.HudElementState hudElementState, float f, float g) {
        if (hudElementState.refScreenW > 0.0F && hudElementState.refScreenH > 0.0F) {
            hudElementState.anchored = true;
        } else {
            if ("info_bar".equals(hudElementState.id)) {
                hudElementState.x = (f - hudElementState.width) * 0.5F;
            } else if (hudElementState.x > f - hudElementState.width
                    || hudElementState.y > g - hudElementState.height) {
                this.placeDefault(hudElementState, f, g);
                return;
            }
            computeAnchor(hudElementState, f, g);
        }
    }

    private static void rebaseToScreen(
            HudLayoutStore.HudElementState hudElementState, float f, float g) {
        float h = f - hudElementState.refScreenW;
        float f2 = g - hudElementState.refScreenH;
        if (h != 0.0F || f2 != 0.0F) {
            if (hudElementState.anchorX == 2) {
                hudElementState.x += h;
            } else if (hudElementState.anchorX == 1) {
                hudElementState.x += h * 0.5F;
            }
            if (hudElementState.anchorBottom) {
                hudElementState.y += f2;
            }
            hudElementState.refScreenW = f;
            hudElementState.refScreenH = g;
        }
    }

    synchronized void moveTo(String string, float f, float g, float h, float f2) {
        HudLayoutStore.HudElementState hudElementState = this.element(string);
        hudElementState.x = f;
        hudElementState.y = g;
        hudElementState.positioned = true;
        hudElementState.autoPlaced = false;
        clampToScreen(hudElementState, h, f2);
        computeAnchor(hudElementState, h, f2);
    }

    private static void computeAnchor(
            HudLayoutStore.HudElementState hudElementState, float f, float g) {
        float h = hudElementState.x + hudElementState.width * 0.5F;
        hudElementState.anchorX =
                Math.abs(h - f * 0.5F) <= 24.0F * uiScale() ? 1 : (h > f * 0.5F ? 2 : 0);
        hudElementState.anchorBottom = hudElementState.y + hudElementState.height * 0.5F > g * 0.5F;
        hudElementState.refScreenW = f;
        hudElementState.refScreenH = g;
        hudElementState.anchored = true;
    }

    private static float uiScale() {
        return ImGuiManager.getInstance().uiScale();
    }

    synchronized void resetToDefaults(float f, float g) {
        LinkedHashMap map = new LinkedHashMap();
        for (String string : KNOWN_IDS) {
            map.put(
                    string,
                    new HudLayoutStore.Dimensions(
                            this.existingWidth(string, fallbackWidth(string)),
                            this.existingHeight(string, 26.0F)));
        }
        this.elements.clear();
        for (String string2 : KNOWN_IDS) {
            HudLayoutStore.HudElementState hudElementState = defaultElement(string2);
            HudLayoutStore.Dimensions dimensions = (HudLayoutStore.Dimensions) map.get(string2);
            hudElementState.width = dimensions.width();
            hudElementState.height = dimensions.height();
            this.elements.put(hudElementState.id, hudElementState);
        }
        for (HudLayoutStore.HudElementState element : this.elements.values()) {
            if (!isCollapsed(element)) {
                this.placeDefault(element, f, g);
                clampToScreen(element, f, g);
            }
        }
        this.save();
    }

    public synchronized JsonObject profileSnapshot() {
        this.loadIfNeeded();
        return GSON.toJsonTree(this.buildConfigFile()).getAsJsonObject();
    }

    public synchronized boolean applyProfile(JsonObject json) {
        this.loadIfNeeded();
        HudLayoutStore.ConfigFile configFile = parse(json);
        if (configFile == null) {
            return false;
        } else {
            this.applyConfig(configFile);
            this.save();
            return true;
        }
    }

    public static JsonObject shareable(JsonObject json) {
        HudLayoutStore.ConfigFile configFile = parse(json);
        return configFile == null ? null : GSON.toJsonTree(configFile).getAsJsonObject();
    }

    private static HudLayoutStore.ConfigFile parse(JsonObject json) {
        HudLayoutStore.ConfigFile configFile;
        try {
            configFile = GSON.fromJson(json, HudLayoutStore.ConfigFile.class);
        } catch (RuntimeException exception) {
            return null;
        }
        if (configFile != null && configFile.elements != null) {
            ArrayList list = new ArrayList();
            for (HudLayoutStore.HudElementState element : configFile.elements) {
                if (element != null
                        && element.id != null
                        && isKnownId(element.id)
                        && finite(element)) {
                    list.add(element);
                }
            }
            configFile.elements = list;
            return configFile;
        } else {
            return null;
        }
    }

    private static boolean finite(HudLayoutStore.HudElementState hudElementState) {
        return finite(hudElementState.x)
                && finite(hudElementState.y)
                && finite(hudElementState.width)
                && finite(hudElementState.height)
                && finite(hudElementState.scale)
                && finite(hudElementState.refScreenW)
                && finite(hudElementState.refScreenH)
                && finite(hudElementState.placedGuiScale)
                && finite(hudElementState.placedUiScale);
    }

    public synchronized void save() {
        this.loadIfNeeded();
        HudLayoutStore.ConfigFile configFile = this.buildConfigFile();
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Path sibling = path.resolveSibling(path.getFileName().toString() + ".tmp");
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(sibling)) {
                GSON.toJson(configFile, bufferedWriter);
            }
            try {
                Files.move(
                        sibling,
                        path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                Files.move(sibling, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception2) {
            EfnClientMod.LOGGER.warn(
                    "[EFN] Failed to save HUD layout config", (Throwable) exception2);
        }
    }

    private void loadIfNeeded() {
        if (!this.loaded) {
            this.loaded = true;
            this.installDefaults();
            Path path = configPath();
            if (Files.isRegularFile(path)) {
                try {
                    try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
                        HudLayoutStore.ConfigFile configFile =
                                GSON.fromJson(bufferedReader, HudLayoutStore.ConfigFile.class);
                        if (configFile != null && configFile.elements != null) {
                            this.applyConfig(configFile);
                            return;
                        }
                    }
                } catch (RuntimeException | IOException exception) {
                    EfnClientMod.LOGGER.warn(
                            "[EFN] Failed to load HUD layout config; using defaults",
                            (Throwable) exception);
                    this.elements.clear();
                    this.installDefaults();
                }
            }
        }
    }

    private HudLayoutStore.ConfigFile buildConfigFile() {
        HudLayoutStore.ConfigFile configFile = new HudLayoutStore.ConfigFile();
        configFile.version = 11;
        configFile.elements = new ArrayList<>();
        for (HudLayoutStore.HudElementState element : this.elements.values()) {
            if (element.positioned) {
                configFile.elements.add(element);
            }
        }
        return configFile;
    }

    private void applyConfig(HudLayoutStore.ConfigFile configFile) {
        this.elements.clear();
        this.installDefaults();
        boolean bl = configFile.version < 11;
        for (HudLayoutStore.HudElementState element : configFile.elements) {
            if (element != null
                    && element.id != null
                    && !element.id.isBlank()
                    && isKnownId(element.id)) {
                normalize(element);
                this.elements.put(element.id, element);
            }
        }
        this.ensureKnownElements();
        if (bl) {
            this.resetPositionsForMigration();
        }
    }

    private void installDefaults() {
        for (String string : KNOWN_IDS) {
            this.elements.put(string, defaultElement(string));
        }
    }

    private void ensureKnownElements() {
        for (String string : KNOWN_IDS) {
            this.elements.computeIfAbsent(string, HudLayoutStore::defaultElement);
        }
    }

    private void resetPositionsForMigration() {
        for (String string : KNOWN_IDS) {
            HudLayoutStore.HudElementState element = this.elements.get(string);
            if (element != null) {
                element.positioned = false;
            }
        }
    }

    private static HudLayoutStore.HudElementState defaultElement(String string) {
        HudLayoutStore.HudElementState hudElementState = new HudLayoutStore.HudElementState();
        hudElementState.id = string;
        hudElementState.x = 0.0F;
        hudElementState.y = 0.0F;
        hudElementState.width = fallbackWidth(string);
        hudElementState.height = 26.0F;
        hudElementState.visible = true;
        hudElementState.scale = 1.0F;
        hudElementState.positioned = false;
        return hudElementState;
    }

    private static void normalize(HudLayoutStore.HudElementState hudElementState) {
        hudElementState.scale =
                finite(hudElementState.scale) ? clamp(hudElementState.scale, 0.5F, 3.0F) : 1.0F;
        hudElementState.width =
                finite(hudElementState.width) && hudElementState.width > 0.0F
                        ? hudElementState.width
                        : 1.0F;
        hudElementState.height =
                finite(hudElementState.height) && hudElementState.height > 0.0F
                        ? hudElementState.height
                        : 1.0F;
        boolean bl =
                hudElementState.positioned = finite(hudElementState.x) && finite(hudElementState.y);
        if (hudElementState.anchorX < 0
                || hudElementState.anchorX > 2
                || !finite(hudElementState.refScreenW)
                || !finite(hudElementState.refScreenH)) {
            hudElementState.refScreenW = 0.0F;
            hudElementState.refScreenH = 0.0F;
        }
    }

    private void placeDefault(HudLayoutStore.HudElementState hudElementState, float f, float g) {
        float h = Math.round(10.0F * uiScale());
        float f2 = Math.round(6.0F * uiScale());
        float g2 = h + this.existingHeight("coordinates", 28.0F) + f2;
        String id2 = hudElementState.id;
        switch (id2) {
            case "potion_tracker":
                hudElementState.x = Math.max(h, f - h - Math.max(1.0F, hudElementState.width));
                hudElementState.y = Math.max(h, g - h - Math.max(1.0F, hudElementState.height));
                break;
            case "info_bar":
                hudElementState.x = Math.max(h, (f - hudElementState.width) * 0.5F);
                hudElementState.y = h;
                break;
            case "module_list":
                hudElementState.x = Math.max(h, f - h - Math.max(1.0F, hudElementState.width));
                hudElementState.y = h;
                break;
            case "region_map":
                hudElementState.x = h;
                hudElementState.y = this.firstFreeY(hudElementState, g2, f2);
                break;
            case "anti_vanish":
                hudElementState.x = Math.max(h, (f - hudElementState.width) * 0.5F);
                hudElementState.y = h + this.existingHeight("info_bar", 28.0F) + f2;
                break;
            case "spotify":
                hudElementState.x = h;
                hudElementState.y = this.firstFreeY(hudElementState, g2, f2);
                break;
            case "staff_list":
                hudElementState.x = h;
                hudElementState.y = this.firstFreeY(hudElementState, Math.round(g * 0.38F), f2);
                break;
            case "hotkeys":
                hudElementState.x = Math.max(h, f - h - Math.max(1.0F, hudElementState.width));
                hudElementState.y = this.firstFreeY(hudElementState, Math.round(g * 0.36F), f2);
                break;
            case "radar":
                hudElementState.x = Math.max(h, f - h - Math.max(1.0F, hudElementState.width));
                hudElementState.y = this.firstFreeY(hudElementState, Math.round(g * 0.58F), f2);
                break;
            case "target_card":
                hudElementState.x = Math.round(f * 0.5F + 44.0F * uiScale());
                hudElementState.y =
                        this.firstFreeY(
                                hudElementState, Math.round(g * 0.5F + 30.0F * uiScale()), f2);
                break;
            case "armor_status":
                hudElementState.x = Math.round(f * 0.5F + 119.0F * vanillaGuiScale());
                hudElementState.y = Math.max(h, g - h - Math.max(1.0F, hudElementState.height));
                break;
            case "item_cooldowns":
                hudElementState.x =
                        Math.max(h, (float) Math.round((f - hudElementState.width) * 0.5F));
                hudElementState.y =
                        Math.max(
                                h,
                                g
                                        - Math.round(76.0F * vanillaGuiScale())
                                        - Math.max(1.0F, hudElementState.height));
                break;
            default:
                hudElementState.x = h;
                hudElementState.y = h;
        }
        hudElementState.positioned = true;
        clampToScreen(hudElementState, f, g);
        computeAnchor(hudElementState, f, g);
        hudElementState.autoPlaced = followsVanillaHud(hudElementState.id);
        if (hudElementState.autoPlaced) {
            hudElementState.anchorX = "item_cooldowns".equals(hudElementState.id) ? 1 : 0;
            hudElementState.placedGuiScale = vanillaGuiScale();
            hudElementState.placedUiScale = uiScale();
        }
    }

    private static boolean followsVanillaHud(String string) {
        return "target_card".equals(string)
                || "armor_status".equals(string)
                || "item_cooldowns".equals(string);
    }

    private static boolean dockMoved(
            HudLayoutStore.HudElementState hudElementState, float f, float g) {
        return f != hudElementState.refScreenW
                || g != hudElementState.refScreenH
                || vanillaGuiScale() != hudElementState.placedGuiScale
                || uiScale() != hudElementState.placedUiScale;
    }

    private static float fallbackWidth(String string) {
        return switch (string) {
            case "module_list" -> 170.0F;
            case "info_bar" -> 290.0F;
            case "coordinates" -> 210.0F;
            case "potion_tracker" -> 190.0F;
            case "region_map" -> 240.0F;
            case "anti_vanish" -> 180.0F;
            case "staff_list" -> 190.0F;
            case "spotify" -> 320.0F;
            case "hotkeys" -> 190.0F;
            case "radar" -> 150.0F;
            case "target_card" -> 244.0F;
            case "armor_status" -> 120.0F;
            case "item_cooldowns" -> 80.0F;
            default -> 110.0F;
        };
    }

    private static float vanillaGuiScale() {
        return Math.max(1, MinecraftClient.getInstance().getWindow().getScaleFactor());
    }

    private float firstFreeY(HudLayoutStore.HudElementState hudElementState, float f, float g) {
        float h = f;
        float f2 = Math.max(1.0F, hudElementState.width);
        float g2 = Math.max(1.0F, hudElementState.height);
        for (int i = 0; i < this.elements.size(); i++) {
            boolean bl = false;
            for (HudLayoutStore.HudElementState element : this.elements.values()) {
                if (element != hudElementState
                        && element.positioned
                        && !isCollapsed(element)
                        && hudElementState.x < element.x + element.width
                        && element.x < hudElementState.x + f2
                        && h < element.y + element.height
                        && element.y < h + g2) {
                    h = element.y + element.height + g;
                    bl = true;
                }
            }
            if (!bl) {
                break;
            }
        }
        return h;
    }

    private static boolean isCollapsed(HudLayoutStore.HudElementState hudElementState) {
        return hudElementState.width <= 1.0F;
    }

    private static void clampToScreen(
            HudLayoutStore.HudElementState hudElementState, float f, float g) {
        float h = Math.max(0.0F, f - Math.max(1.0F, hudElementState.width));
        float f2 = Math.max(0.0F, g - Math.max(1.0F, hudElementState.height));
        hudElementState.x = clamp(hudElementState.x, 0.0F, h);
        hudElementState.y = clamp(hudElementState.y, 0.0F, f2);
    }

    private float existingWidth(String string, float f) {
        HudLayoutStore.HudElementState element = this.elements.get(string);
        return element != null && finite(element.width) && element.width > 0.0F ? element.width : f;
    }

    private float existingHeight(String string, float f) {
        HudLayoutStore.HudElementState element = this.elements.get(string);
        return element != null && finite(element.height) && element.height > 0.0F
                ? element.height
                : f;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("hud.json");
    }

    private static boolean isKnownId(String string) {
        for (String string2 : KNOWN_IDS) {
            if (string2.equals(string)) {
                return true;
            }
        }
        return false;
    }

    private static boolean finite(float f) {
        return !Float.isNaN(f) && !Float.isInfinite(f);
    }

    private static float clamp(float f, float g, float h) {
        return Math.max(g, Math.min(h, f));
    }

    @Environment(EnvType.CLIENT)
    private static final class ConfigFile {

        int version;

        List<HudLayoutStore.HudElementState> elements;

        private ConfigFile() {}
    }

    @Environment(EnvType.CLIENT)
    private record Dimensions(float width, float height) {}

    @Environment(EnvType.CLIENT)
    static final class HudElementState {

        String id;

        float x;

        float y;

        float width;

        float height;

        boolean visible;

        float scale;

        transient boolean positioned;

        int anchorX;

        boolean anchorBottom;

        float refScreenW;

        float refScreenH;

        transient boolean anchored;

        boolean autoPlaced;

        float placedGuiScale;

        float placedUiScale;

        HudElementState() {}
    }
}
