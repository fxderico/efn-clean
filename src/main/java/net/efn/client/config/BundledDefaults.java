package net.efn.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class BundledDefaults {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final String ROOT = "/assets/efn/defaults/";

    private static JsonObject modules;

    private static JsonObject clickGui;

    private static List<JsonObject> presets;

    private BundledDefaults() {}

    public static synchronized void adopt(List<ClientModule> list) {
        for (ClientModule module : list) {
            JsonObject json = entry(module);
            if (json != null && module.inConfigs()) {
                try {
                    JsonObject json2 = object(json, "settings");
                    if (json2 != null) {
                        for (Setting setting : module.settings()) {
                            JsonElement json3 = json2.get(setting.id());
                            if (json3 != null && !setting.isPrivate()) {
                                setting.adoptDefault(json3);
                            }
                        }
                    }
                    int i;
                    if (json.has("keybind")
                            && ModuleKeybinds.isBindable(i = json.get("keybind").getAsInt())) {
                        module.setKeybind(i);
                    }
                    if (json.has("holdMode")) {
                        module.setHoldMode(json.get("holdMode").getAsBoolean());
                    }
                } catch (RuntimeException exception) {
                    LOGGER.warn(
                            "[EFN] Failed to apply bundled defaults of '{}'",
                            module.id(),
                            exception);
                }
            }
        }
    }

    public static synchronized boolean enabledByDefault(ClientModule module) {
        JsonObject json = module.inConfigs() ? entry(module) : null;
        JsonElement json2 = json == null ? null : json.get("enabled");
        if (json2 != null && json2.isJsonPrimitive()) {
            try {
                return json2.getAsBoolean();
            } catch (RuntimeException exception) {
                return module.enabledByDefault();
            }
        } else {
            return module.enabledByDefault();
        }
    }

    public static synchronized float[] panelSpot(String string, float f, float g) {
        JsonObject json = clickGui();
        JsonObject json2 = object(json, "panels");
        JsonElement json3 = json.get("screen");
        if (json2 != null && json3 != null && json3.isJsonArray() && json2.has(string)) {
            try {
                JsonArray jsonArray = json3.getAsJsonArray();
                JsonArray jsonArray2 = json2.getAsJsonArray(string);
                return new float[] {
                    jsonArray2.get(0).getAsFloat() * f / jsonArray.get(0).getAsFloat(),
                    jsonArray2.get(1).getAsFloat() * g / jsonArray.get(1).getAsFloat()
                };
            } catch (RuntimeException exception) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static synchronized Set<String> collapsedPanels() {
        LinkedHashSet set = new LinkedHashSet();
        JsonElement json = clickGui().get("collapsed");
        if (json != null && json.isJsonArray()) {
            for (JsonElement json2 : json.getAsJsonArray()) {
                if (json2.isJsonPrimitive()) {
                    set.add(json2.getAsString());
                }
            }
            return set;
        } else {
            return set;
        }
    }

    public static synchronized List<JsonObject> presets() {
        if (presets == null) {
            ArrayList list = new ArrayList();
            JsonElement json = read("presets.json").get("presets");
            if (json != null && json.isJsonArray()) {
                for (JsonElement json2 : json.getAsJsonArray()) {
                    if (json2.isJsonObject()) {
                        list.add(json2.getAsJsonObject());
                    }
                }
            }
            presets = List.copyOf(list);
        }
        ArrayList list2 = new ArrayList(presets.size());
        for (JsonObject preset : presets) {
            list2.add(preset.deepCopy());
        }
        return list2;
    }

    private static JsonObject entry(ClientModule module) {
        JsonObject json = object(modules(), "modules");
        return json == null ? null : object(json, module.id());
    }

    private static JsonObject modules() {
        if (modules == null) {
            modules = read("modules.json");
        }
        return modules;
    }

    private static JsonObject clickGui() {
        if (clickGui == null) {
            clickGui = read("clickgui.json");
        }
        return clickGui;
    }

    private static JsonObject object(JsonObject json, String string) {
        JsonElement json2 = json.get(string);
        return json2 != null && json2.isJsonObject() ? json2.getAsJsonObject() : null;
    }

    private static JsonObject read(String string) {
        try {
            JsonObject json;
            try (InputStream resourceAsStream =
                    BundledDefaults.class.getResourceAsStream("/assets/efn/defaults/" + string)) {
                if (resourceAsStream == null) {
                    return new JsonObject();
                }
                JsonObject jsonObject;
                try (InputStreamReader inputStreamReader =
                        new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8)) {
                    jsonObject = JsonParser.parseReader(inputStreamReader).getAsJsonObject();
                }
                json = jsonObject;
            }
            return json;
        } catch (Exception exception) {
            LOGGER.warn("[EFN] Failed to read bundled defaults {}", string, exception);
            return new JsonObject();
        }
    }
}
