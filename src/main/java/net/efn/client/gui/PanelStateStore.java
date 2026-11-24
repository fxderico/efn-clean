package net.efn.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.efn.client.config.BundledDefaults;
import net.efn.client.module.ModuleCategory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class PanelStateStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final PanelStateStore INSTANCE = new PanelStateStore();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int CONFIG_VERSION = 1;

    private final Set<String> collapsed = new LinkedHashSet<>();

    private final Map<String, float[]> homes = new LinkedHashMap<>();

    private boolean loaded;

    private boolean readOnly;

    private PanelStateStore() {}

    public static PanelStateStore getInstance() {
        return INSTANCE;
    }

    public synchronized boolean isCollapsed(ModuleCategory moduleCategory) {
        this.ensureLoaded();
        return this.collapsed.contains(moduleCategory.name());
    }

    public synchronized void setCollapsed(ModuleCategory moduleCategory, boolean bl) {
        this.ensureLoaded();
        boolean bl2 =
                bl
                        ? this.collapsed.add(moduleCategory.name())
                        : this.collapsed.remove(moduleCategory.name());
        if (bl2) {
            this.save();
        }
    }

    public synchronized Map<String, float[]> panelHomes() {
        this.ensureLoaded();
        HashMap map = new HashMap();
        this.homes.forEach((value, fs) -> map.put(value, (float[]) fs.clone()));
        return map;
    }

    public synchronized void setPanelHomes(Map<String, float[]> map) {
        this.ensureLoaded();
        this.homes.clear();
        map.forEach((value, fs) -> this.homes.put(value, (float[]) fs.clone()));
        this.save();
    }

    public synchronized void load() {
        if (!this.loaded) {
            this.loaded = true;
            this.collapsed.clear();
            this.homes.clear();
            Path path = this.configPath();
            if (!Files.isRegularFile(path)) {
                this.collapsed.addAll(BundledDefaults.collapsedPanels());
            } else {
                try {
                    try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
                        JsonObject jsonObject =
                                JsonParser.parseReader(bufferedReader).getAsJsonObject();
                        int i =
                                jsonObject.has("version")
                                        ? jsonObject.get("version").getAsInt()
                                        : 0;
                        if (i <= 1) {
                            if (jsonObject.has("collapsed")
                                    && jsonObject.get("collapsed").isJsonArray()) {
                                for (JsonElement json : jsonObject.getAsJsonArray("collapsed")) {
                                    if (json.isJsonPrimitive()) {
                                        this.collapsed.add(json.getAsString());
                                    }
                                }
                            }
                            if (jsonObject.has("panels")
                                    && jsonObject.get("panels").isJsonObject()) {
                                for (Entry entry :
                                        jsonObject.getAsJsonObject("panels").entrySet()) {
                                    if (((JsonElement) entry.getValue()).isJsonArray()
                                            && ((JsonElement) entry.getValue())
                                                            .getAsJsonArray()
                                                            .size()
                                                    == 2) {
                                        JsonArray jsonArray =
                                                ((JsonElement) entry.getValue()).getAsJsonArray();
                                        float f = jsonArray.get(0).getAsFloat();
                                        float g = jsonArray.get(1).getAsFloat();
                                        if (Float.isFinite(f) && Float.isFinite(g)) {
                                            this.homes.put(
                                                    (String) entry.getKey(), new float[] {f, g});
                                        }
                                    }
                                }
                            }
                            return;
                        }
                        this.readOnly = true;
                        LOGGER.warn(
                                "[EFN] ClickGUI state version {} is newer than supported {};"
                                        + " skipping load",
                                i,
                                1);
                    }
                } catch (RuntimeException | IOException exception) {
                    LOGGER.warn(
                            "[EFN] Failed to load the ClickGUI state; preserving the file",
                            (Throwable) exception);
                    this.collapsed.clear();
                    this.homes.clear();
                    this.readOnly = true;
                }
            }
        }
    }

    public synchronized void save() {
        if (!this.readOnly) {
            JsonObject json = new JsonObject();
            json.addProperty("version", 1);
            JsonArray array = new JsonArray();
            for (String string : this.collapsed) {
                array.add(string);
            }
            json.add("collapsed", array);
            JsonObject json2 = new JsonObject();
            this.homes.forEach(
                    (value, fs) -> {
                        JsonArray array2 = new JsonArray();
                        array2.add(fs[0]);
                        array2.add(fs[1]);
                        json2.add(value, array2);
                    });
            json.add("panels", json2);
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
                LOGGER.warn("[EFN] Failed to save the ClickGUI state", (Throwable) exception2);
            }
        }
    }

    private void ensureLoaded() {
        if (!this.loaded) {
            this.load();
        }
    }

    private Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("clickgui.json");
    }
}
