package net.efn.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.efn.client.imgui.UiFont;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ThemeStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final ThemeStore INSTANCE = new ThemeStore();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int CONFIG_VERSION = 3;

    private static final String THEME_KEY = "efnTheme";

    public static final List<ThemeStore.Preset> PRESETS =
            List.of(
                    new ThemeStore.Preset("Cyan", -15408912, -15592938, -15263972),
                    new ThemeStore.Preset("Azure", -12743681, -15592938, -15263972),
                    new ThemeStore.Preset("Cobalt", -11903489, -15592938, -15263972),
                    new ThemeStore.Preset("Lime", -4721846, -15592938, -15263972),
                    new ThemeStore.Preset("Ember", -42442, -15592938, -15263972),
                    new ThemeStore.Preset("Mono", -1513238, -15592938, -15263972));

    public final ColorSetting accent = new ColorSetting("Accent", -15408912);

    public final NumberSetting rounding =
            new NumberSetting("Corner radius", 16.0F, 0.0F, 16.0F).step(1.0F);

    public final NumberSetting backgroundDim =
            new NumberSetting("Background dim", 0.55F, 0.0F, 0.8F).step(0.05F);

    public final BooleanSetting menuBlur = new BooleanSetting("Menu blur", true);

    public final BooleanSetting moduleDividers = new BooleanSetting("Module dividers", true);

    public final NumberSetting uiScale =
            new NumberSetting("UI scale", 1.0F, 0.75F, 1.5F).step(0.05F);

    public final ModeSetting font = new ModeSetting("Font", UiFont.GEIST.label, UiFont.labels());

    public final NumberSetting animationSpeed =
            new NumberSetting("Animation speed", 1.3F, 0.0F, 2.0F).step(0.1F);

    public final BooleanSetting animateLines = new BooleanSetting("Animate patterns", true);

    public final BooleanSetting customMainMenu = new BooleanSetting("Custom menus", true);

    public final BooleanSetting communityPopup = new BooleanSetting("Community popup", true);

    public final Surface panels =
            new Surface("panels", "Panels", false, -14736856, 1.0F, 0.45F, "Flow");

    public final Surface cards = new Surface("cards", "Cards", true, -15263458, 1.0F, 0.85F);

    public final Surface tabBar =
            new Surface("tabbar", "Tab bar", false, -15263458, 1.0F, 0.5F, "Flow");

    private final List<Surface> surfaces = List.of(this.panels, this.cards, this.tabBar);

    private final List<Setting> shared;

    private final List<Setting> settings;

    private final JsonObject defaults;

    private boolean loaded;

    private boolean readOnly;

    private ThemeStore() {
        ArrayList list =
                new ArrayList<>(
                        List.of(
                                this.accent,
                                this.rounding,
                                this.backgroundDim,
                                this.menuBlur,
                                this.moduleDividers,
                                this.uiScale,
                                this.font,
                                this.animationSpeed,
                                this.animateLines));
        for (Surface surface : this.surfaces) {
            list.addAll(surface.settings());
        }
        this.shared = List.copyOf(list);
        list.add(this.customMainMenu);
        list.add(this.communityPopup);
        this.settings = List.copyOf(list);
        this.defaults = snapshot(this.settings, true);
        this.customMainMenu.onChange(
                () -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null && client.getWindow() != null) {
                        client.updateWindowTitle();
                    }
                });
    }

    public static ThemeStore get() {
        INSTANCE.ensureLoaded();
        return INSTANCE;
    }

    public List<Surface> surfaces() {
        return this.surfaces;
    }

    public void applyPreset(ThemeStore.Preset preset) {
        this.accent.setArgb(preset.accent());
        this.panels.color.setArgb(preset.panel());
        this.cards.color.setArgb(preset.card());
        this.tabBar.color.setArgb(preset.card());
    }

    public boolean matchesPreset(ThemeStore.Preset preset) {
        return this.accent.rgb() == (preset.accent() & 16777215)
                && this.panels.color.rgb() == (preset.panel() & 16777215)
                && this.cards.color.rgb() == (preset.card() & 16777215);
    }

    public void resetToDefaults() {
        apply(this.shared, this.defaults, true);
    }

    public static Path backgroundsDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("backgrounds");
    }

    public String exportString() {
        JsonObject json = new JsonObject();
        json.add("efnTheme", snapshot(this.shared, false));
        return json.toString();
    }

    public boolean importString(String string) {
        if (string != null && !string.isBlank()) {
            try {
                JsonElement string2 = JsonParser.parseString(string.trim());
                if (string2.isJsonObject() && string2.getAsJsonObject().has("efnTheme")) {
                    JsonElement json = string2.getAsJsonObject().get("efnTheme");
                    if (!json.isJsonObject()) {
                        return false;
                    } else {
                        apply(this.shared, json.getAsJsonObject(), false);
                        return true;
                    }
                } else {
                    return false;
                }
            } catch (RuntimeException exception) {
                return false;
            }
        } else {
            return false;
        }
    }

    public JsonObject profileSnapshot() {
        return snapshot(this.shared, true);
    }

    public void applyProfile(JsonObject json) {
        for (Setting setting : this.shared) {
            JsonElement json2 = json.get(setting.id());
            if (json2 == null && !setting.isPrivate()) {
                json2 = this.defaults.get(setting.id());
            }
            if (json2 != null) {
                setting.deserialize(json2);
            }
        }
    }

    public JsonObject shareable(JsonObject json) {
        JsonObject json2 = new JsonObject();
        for (Setting setting : this.shared) {
            JsonElement json3 = json.get(setting.id());
            if (json3 != null
                    && !setting.isPrivate()
                    && !Setting.sameValue(json3, this.defaults.get(setting.id()))) {
                json2.add(setting.id(), json3.deepCopy());
            }
        }
        return json2;
    }

    public synchronized void load() {
        if (!this.loaded) {
            this.loaded = true;
            Path path = this.configPath();
            if (Files.isRegularFile(path)) {
                try {
                    try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
                        JsonObject jsonObject =
                                JsonParser.parseReader(bufferedReader).getAsJsonObject();
                        int i =
                                jsonObject.has("version")
                                        ? jsonObject.get("version").getAsInt()
                                        : 0;
                        if (i <= 3) {
                            if (jsonObject.has("values")
                                    && jsonObject.get("values").isJsonObject()) {
                                apply(this.settings, jsonObject.getAsJsonObject("values"), true);
                            }
                            if (i < 3) {
                                this.adoptNewDefaults();
                            }
                            return;
                        }
                        this.readOnly = true;
                        LOGGER.warn(
                                "[EFN] Theme version {} is newer than supported {}; skipping load",
                                i,
                                3);
                    }
                } catch (RuntimeException | IOException exception) {
                    LOGGER.warn(
                            "[EFN] Failed to load theme; preserving the file",
                            (Throwable) exception);
                    this.readOnly = true;
                }
            }
        }
    }

    private void adoptNewDefaults() {
        if (this.panels.background.is("None")) {
            this.panels.background.set("Flow");
        }
        if (Math.abs(this.panels.opacity.get() - 0.92F) < 0.001F) {
            this.panels.opacity.set(1.0F);
        }
        for (Surface surface : List.of(this.cards, this.tabBar)) {
            if (Math.abs(surface.opacity.get() - 0.97F) < 0.001F) {
                surface.opacity.set(1.0F);
            }
        }
    }

    public synchronized void save() {
        if (!this.readOnly && this.loaded) {
            JsonObject json = new JsonObject();
            json.addProperty("version", 3);
            json.add("values", snapshot(this.settings, true));
            Path path = this.configPath();
            try {
                Files.createDirectories(path.getParent());
                Path sibling = path.resolveSibling(path.getFileName().toString() + ".tmp");
                try (BufferedWriter bufferedWriter = Files.newBufferedWriter(sibling)) {
                    GSON.toJson((JsonElement) json, bufferedWriter);
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
                LOGGER.warn("[EFN] Failed to save theme", (Throwable) exception2);
            }
        }
    }

    private static JsonObject snapshot(List<Setting> list, boolean bl) {
        JsonObject json = new JsonObject();
        for (Setting setting : list) {
            if (bl || !setting.isPrivate()) {
                json.add(setting.id(), setting.serialize());
            }
        }
        return json;
    }

    private static void apply(List<Setting> list, JsonObject json, boolean bl) {
        for (Setting setting : list) {
            if ((bl || !setting.isPrivate()) && json.has(setting.id())) {
                setting.deserialize(json.get(setting.id()));
            }
        }
    }

    private void ensureLoaded() {
        if (!this.loaded) {
            this.load();
        }
    }

    private Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("theme.json");
    }

    @Environment(EnvType.CLIENT)
    public record Preset(String name, int accent, int panel, int card) {}
}
