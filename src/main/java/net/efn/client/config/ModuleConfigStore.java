package net.efn.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.efn.client.event.impl.ModuleToggleEvent;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.ModuleRevision;
import net.efn.client.module.setting.Setting;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class ModuleConfigStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final ModuleConfigStore INSTANCE = new ModuleConfigStore();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int CONFIG_VERSION = 3;

    private static final long AUTOSAVE_DELAY_NANOS = 2000000000L;

    private static final long SHUTDOWN_WRITE_WAIT_SECONDS = 10L;

    private static final ExecutorService WRITER =
            Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread = new Thread(runnable, "EFN config writer");
                        thread.setDaemon(true);
                        return thread;
                    });

    private static final Map<String, String> RENAMED_MODULES =
            Map.of("activity-bypass", "player-bypass", "Activity Bypass", "player-bypass");

    private final Map<String, JsonObject> orphanModules = new LinkedHashMap<>();

    private final Map<String, Boolean> pendingEnabled = new LinkedHashMap<>();

    private boolean loaded;

    private boolean readOnly;

    private final Map<String, JsonObject> retainedEntries = new LinkedHashMap<>();

    private String activeProfile;

    private volatile boolean writeFailed;

    private Future<?> lastWrite;

    private long savedRevision = -1L;

    private long observedRevision = -1L;

    private long observedAt;

    private ModuleConfigStore() {}

    public static ModuleConfigStore getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        if (!this.loaded) {
            this.loaded = true;
            this.orphanModules.clear();
            this.pendingEnabled.clear();
            BundledDefaults.adopt(ModuleRegistry.all());
            this.readConfig();
            for (ClientModule module : ModuleRegistry.all()) {
                if (BundledDefaults.enabledByDefault(module)
                        && !this.retainedEntries.containsKey(module.id())) {
                    this.pendingEnabled.put(module.id(), true);
                }
            }
        }
    }

    private void readConfig() {
        Path path = this.configPath();
        deleteQuietly(tmpPath(path));
        if (Files.isRegularFile(path)) {
            try {
                label87:
                {
                    try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
                        JsonElement reader = JsonParser.parseReader(bufferedReader);
                        if (reader.isJsonObject()) {
                            JsonObject jsonObject = reader.getAsJsonObject();
                            this.applyConfigRoot(jsonObject);
                            if (jsonObject.has("activeProfile")
                                    && jsonObject.get("activeProfile").isJsonPrimitive()) {
                                this.activeProfile = jsonObject.get("activeProfile").getAsString();
                            }
                            int i =
                                    jsonObject.has("version")
                                            ? jsonObject.get("version").getAsInt()
                                            : 1;
                            Path path2;
                            if (!this.readOnly
                                    && i < 3
                                    && !Files.exists(path2 = backupPath(path, i))) {
                                writeJson(path2, withoutSettings(jsonObject, Setting::isSecret));
                            }
                            break label87;
                        }
                        LOGGER.warn("[EFN] Module config isn't a JSON object; preserving the file");
                        this.readOnly = true;
                    }
                    return;
                }
            } catch (RuntimeException | IOException exception) {
                LOGGER.warn(
                        "[EFN] Failed to load module config; preserving the file",
                        (Throwable) exception);
                this.orphanModules.clear();
                this.pendingEnabled.clear();
                this.readOnly = true;
            }
            if (!this.readOnly) {
                scrubBackups(path);
            }
        }
    }

    private static Path backupPath(Path path, int i) {
        return path.resolveSibling(path.getFileName() + ".v" + i + ".bak");
    }

    private static void scrubBackups(Path path) {
        ArrayList list = new ArrayList();
        for (ClientModule module : ModuleRegistry.all()) {
            for (Setting setting : module.settings()) {
                if (setting.isSecret()) {
                    list.add("\"" + setting.id() + "\"");
                    list.add("\"" + setting.name() + "\"");
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            Path path2 = backupPath(path, i);
            if (Files.isRegularFile(path2)) {
                try {
                    String string = Files.readString(path2);
                    boolean bl = false;
                    for (String string2 : (Iterable<String>) list) {
                        bl |= string.contains(string2);
                    }
                    JsonObject json;
                    JsonElement string3;
                    if (bl
                            && (string3 = JsonParser.parseString(string)).isJsonObject()
                            && !(json =
                                            withoutSettings(
                                                    string3.getAsJsonObject(), Setting::isSecret))
                                    .equals(string3)) {
                        writeJson(path2, json);
                    }
                } catch (RuntimeException | IOException exception) {
                    LOGGER.warn("[EFN] Couldn't clean {}", path2.getFileName(), exception);
                }
            }
        }
    }

    public static JsonObject withoutPrivateValues(JsonObject json) {
        return withoutSettings(json, Setting::isPrivate);
    }

    private static JsonObject withoutSettings(JsonObject json, Predicate<Setting> predicate) {
        JsonObject json2 = json.deepCopy();
        if (!(json2.get("modules") instanceof JsonObject json3)) {
            return json2;
        } else {
            HashMap map = new HashMap();
            for (ClientModule module : ModuleRegistry.all()) {
                map.putIfAbsent(module.name(), module);
            }
            for (ClientModule module2 : ModuleRegistry.all()) {
                map.put(module2.id(), module2);
            }
            for (String string : List.copyOf(json3.keySet())) {
                if (json3.get(string) instanceof JsonObject json4) {
                    String renamedModuleId = renamedModuleId(string);
                    ClientModule module3 =
                            (ClientModule)
                                    map.get(renamedModuleId != null ? renamedModuleId : string);
                    JsonPrimitive jsonPrimitive;
                    JsonElement json5;
                    if (module3 == null
                            && (json5 = json4.get("name")) instanceof JsonPrimitive
                            && (jsonPrimitive = (JsonPrimitive) json5).isString()) {
                        module3 = (ClientModule) map.get(jsonPrimitive.getAsString());
                    }
                    if (module3 != null) {
                        json3.add(string, withoutSettings(module3, json4, predicate));
                    }
                }
            }
            return json2;
        }
    }

    public synchronized boolean enablesOnLoad(ClientModule module) {
        return Boolean.TRUE.equals(this.pendingEnabled.get(module.id()));
    }

    public synchronized void applyPendingEnabled() {
        if (!this.pendingEnabled.isEmpty()) {
            ModuleToggleEvent.silently(
                    () -> {
                        for (Entry entry : this.pendingEnabled.entrySet()) {
                            ClientModule module;
                            if (Boolean.TRUE.equals(entry.getValue())
                                    && (module = ModuleRegistry.resolve((String) entry.getKey()))
                                            != null
                                    && module.persistEnabled()) {
                                module.setEnabled(true);
                            }
                        }
                    });
            this.pendingEnabled.clear();
        }
    }

    public synchronized void save() {
        this.write(false);
    }

    public synchronized void saveIfChanged() {
        if (!this.loaded
                || this.readOnly
                || this.writeFailed
                || ModuleRevision.current() != this.savedRevision) {
            this.write(false);
        }
    }

    public synchronized void saveAndWait() {
        this.write(true);
    }

    private void write(boolean bl) {
        this.ensureLoaded();
        if (!this.readOnly) {
            long l = ModuleRevision.current();
            JsonObject configRoot = this.buildConfigRoot();
            if (this.activeProfile != null) {
                configRoot.addProperty("activeProfile", this.activeProfile);
            }
            Path path = this.configPath();
            this.writeFailed = false;
            this.savedRevision = l;
            try {
                this.lastWrite =
                        WRITER.submit(
                                () -> {
                                    try {
                                        Files.createDirectories(path.getParent());
                                        writeJson(path, configRoot);
                                    } catch (RuntimeException | IOException exception) {
                                        this.writeFailed = true;
                                        LOGGER.warn(
                                                "[EFN] Failed to save module config",
                                                (Throwable) exception);
                                    }
                                });
            } catch (RuntimeException exception2) {
                this.writeFailed = true;
                return;
            }
            if (bl) {
                try {
                    this.lastWrite.get(10L, TimeUnit.SECONDS);
                } catch (Exception exception3) {
                    LOGGER.warn(
                            "[EFN] Module config was still being written at shutdown",
                            (Throwable) exception3);
                }
            }
        }
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Path path2 = tmpPath(path);
        try {
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path2)) {
                GSON.toJson((JsonElement) json, bufferedWriter);
            }
            try {
                Files.move(
                        path2,
                        path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                Files.move(path2, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            deleteQuietly(path2);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            LOGGER.warn("[EFN] Couldn't delete {}", path.getFileName());
        }
    }

    private static Path tmpPath(Path path) {
        return path.resolveSibling(path.getFileName() + ".tmp");
    }

    public synchronized void tickAutosave() {
        if (!this.writeFailed
                || !this.loaded
                || this.readOnly
                || this.lastWrite != null && !this.lastWrite.isDone()) {
            long l = System.nanoTime();
            if (this.autosaveDue(ModuleRevision.current(), l)) {
                this.save();
                this.observedAt = l;
            }
        } else {
            long l2 = System.nanoTime();
            if (l2 - this.observedAt >= 2000000000L) {
                this.observedAt = l2;
                this.save();
            }
        }
    }

    private boolean autosaveDue(long observedRevision2, long observedAt2) {
        if (!this.loaded || this.readOnly) {
            return false;
        } else if (this.savedRevision < 0L) {
            this.savedRevision = this.observedRevision = observedRevision2;
            return false;
        } else if (observedRevision2 == this.savedRevision) {
            return false;
        } else if (observedRevision2 != this.observedRevision) {
            this.observedRevision = observedRevision2;
            this.observedAt = observedAt2;
            return false;
        } else {
            return observedAt2 - this.observedAt >= 2000000000L;
        }
    }

    public synchronized boolean hasUnsavedChanges() {
        return !this.readOnly
                && this.savedRevision >= 0L
                && ModuleRevision.current() != this.savedRevision;
    }

    public synchronized boolean isReadOnly() {
        return this.readOnly;
    }

    public synchronized String activeProfile() {
        return this.activeProfile;
    }

    synchronized void setActiveProfile(String activeProfile2) {
        this.activeProfile = activeProfile2;
    }

    public synchronized JsonObject snapshot() {
        this.ensureLoaded();
        JsonObject configRoot = this.buildConfigRoot();
        JsonObject jsonObject = configRoot.getAsJsonObject("modules");
        for (ClientModule module : ModuleRegistry.all()) {
            if (module.inConfigs()) {
                jsonObject.add(
                        module.id(),
                        withoutPrivate(module, jsonObject.getAsJsonObject(module.id())));
            } else {
                jsonObject.remove(module.id());
            }
        }
        return configRoot;
    }

    public synchronized boolean applyProfile(JsonObject json) {
        this.ensureLoaded();
        int profileVersion = profileVersion(json);
        if (profileVersion < 0) {
            return false;
        } else {
            List list = ModuleRegistry.all();
            Map map = matchEntries(json, profileVersion, list);
            HashMap map2 = new HashMap();
            ModuleToggleEvent.silently(
                    () -> {
                        for (ClientModule module : (Iterable<ClientModule>) list) {
                            if (module.inConfigs()) {
                                JsonObject json2 = (JsonObject) map.get(module);
                                for (Setting setting : module.settings()) {
                                    if (!setting.isPrivate()) {
                                        setting.resetToDefault();
                                    }
                                }
                                module.setKeybind(-1);
                                module.setHoldMode(false);
                                JsonObject retainedEntry = this.retainedEntries.get(module.id());
                                if (json2 != null) {
                                    json2 = withoutPrivate(module, json2);
                                    this.applyModuleEntry(module, json2, map2);
                                }
                                if ((json2 = keepSealed(module, retainedEntry, json2)) != null) {
                                    this.retainedEntries.put(module.id(), json2);
                                } else {
                                    this.retainedEntries.remove(module.id());
                                }
                                if (module.persistEnabled()) {
                                    module.setEnabled(Boolean.TRUE.equals(map2.get(module.id())));
                                }
                            }
                        }
                    });
            this.pendingEnabled.clear();
            return true;
        }
    }

    public synchronized JsonObject shareable(JsonObject json) {
        int profileVersion = profileVersion(json);
        if (profileVersion < 0) {
            return null;
        } else {
            List list = ModuleRegistry.all();
            Map map = matchEntries(json, profileVersion, list);
            JsonObject json2 = new JsonObject();
            for (ClientModule module : (Iterable<ClientModule>) list) {
                JsonObject json3 = module.inConfigs() ? (JsonObject) map.get(module) : null;
                JsonObject json4 = json3 == null ? null : shareableEntry(module, json3);
                if (json4 != null) {
                    json2.add(module.id(), json4);
                }
            }
            JsonObject json5 = new JsonObject();
            json5.addProperty("version", 3);
            json5.add("modules", json2);
            return json5;
        }
    }

    private static int profileVersion(JsonObject json) {
        if (json != null && json.has("modules") && json.get("modules").isJsonObject()) {
            try {
                int i = json.has("version") ? json.get("version").getAsInt() : 0;
                return i > 3 ? -1 : i;
            } catch (RuntimeException exception) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    private static Map<ClientModule, JsonObject> matchEntries(
            JsonObject json, int i, List<ClientModule> list) {
        HashMap map = new HashMap();
        for (ClientModule module : list) {
            map.putIfAbsent(module.name(), module);
        }
        for (ClientModule module2 : list) {
            map.put(module2.id(), module2);
        }
        HashMap map2 = new HashMap();
        for (Entry entry : renameModules(json.getAsJsonObject("modules"), i).entrySet()) {
            if (((JsonElement) entry.getValue()).isJsonObject()) {
                JsonObject jsonObject = ((JsonElement) entry.getValue()).getAsJsonObject();
                ClientModule module3 = (ClientModule) map.get(entry.getKey());
                if (module3 == null
                        && jsonObject.has("name")
                        && jsonObject.get("name").isJsonPrimitive()) {
                    module3 = (ClientModule) map.get(jsonObject.get("name").getAsString());
                }
                if (module3 != null) {
                    map2.putIfAbsent(module3, jsonObject);
                }
            }
        }
        return map2;
    }

    private static JsonObject shareableEntry(ClientModule module, JsonObject json) {
        JsonObject json2 = new JsonObject();
        if (module.persistEnabled() && flag(json, "enabled")) {
            json2.addProperty("enabled", true);
        }
        int number;
        if ((number = number(json, "keybind", -1)) != -1 && ModuleKeybinds.isBindable(number)) {
            json2.addProperty("keybind", number);
        }
        if (flag(json, "holdMode")) {
            json2.addProperty("holdMode", true);
        }
        JsonObject json3 =
                json.has("settings") && json.get("settings").isJsonObject()
                        ? json.getAsJsonObject("settings")
                        : null;
        JsonObject json4 = new JsonObject();
        if (json3 != null) {
            for (Setting setting : module.settings()) {
                JsonElement json5;
                if (!setting.isPrivate()
                        && (json5 =
                                        json3.has(setting.id())
                                                ? json3.get(setting.id())
                                                : json3.get(setting.name()))
                                != null
                        && !json5.isJsonNull()
                        && !setting.isDefault(json5)) {
                    json4.add(setting.id(), setting.compact(json5.deepCopy()));
                }
            }
        }
        if (json4.size() > 0) {
            json2.add("settings", json4);
        }
        if (json2.size() == 0) {
            return null;
        } else {
            int number2 = number(json, "settingsVersion", 0);
            if (number2 > 0) {
                json2.addProperty("settingsVersion", number2);
            }
            return json2;
        }
    }

    private static JsonObject withoutPrivate(ClientModule module, JsonObject json) {
        return withoutSettings(module, json, Setting::isPrivate);
    }

    private static JsonObject withoutSettings(
            ClientModule module, JsonObject json, Predicate<Setting> predicate) {
        JsonObject json2 = json.deepCopy();
        if (json2.get("settings") instanceof JsonObject json3) {
            for (Setting setting : module.settings()) {
                if (predicate.test(setting)) {
                    json3.remove(setting.id());
                    json3.remove(setting.name());
                    json3.remove(sealedKey(setting));
                }
            }
        }
        return json2;
    }

    private static JsonObject keepSealed(ClientModule module, JsonObject json, JsonObject json2) {
        JsonElement json3;
        if (json != null && (json3 = json.get("settings")) instanceof JsonObject) {
            JsonObject json4 = (JsonObject) json3;
            for (Setting setting : module.settings()) {
                JsonElement json5;
                if (setting.isSecret()
                        && (json5 = json4.get(sealedKey(setting))) instanceof JsonPrimitive) {
                    JsonPrimitive jsonPrimitive = (JsonPrimitive) json5;
                    if (json2 == null) {
                        json2 = new JsonObject();
                    }
                    JsonObject json6;
                    JsonElement json7;
                    JsonObject json8 =
                            (json7 = json2.get("settings")) instanceof JsonObject
                                    ? (json6 = (JsonObject) json7)
                                    : null;
                    if (json8 == null) {
                        json8 = new JsonObject();
                        json2.add("settings", json8);
                    }
                    json8.add(sealedKey(setting), jsonPrimitive.deepCopy());
                }
            }
            return json2;
        } else {
            return json2;
        }
    }

    private static String sealedKey(Setting setting) {
        return setting.id() + "#sealed";
    }

    private static boolean flag(JsonObject json, String string) {
        JsonElement json2 = json.get(string);
        if (json2 != null && json2.isJsonPrimitive()) {
            try {
                return json2.getAsBoolean();
            } catch (RuntimeException exception) {
                return false;
            }
        } else {
            return false;
        }
    }

    private static int number(JsonObject json, String string, int i) {
        JsonElement json2 = json.get(string);
        if (json2 != null && json2.isJsonPrimitive()) {
            try {
                return json2.getAsInt();
            } catch (RuntimeException exception) {
                return i;
            }
        } else {
            return i;
        }
    }

    public synchronized void adopt(ClientModule module) {
        if (this.loaded) {
            JsonObject orphanModule = this.orphanModules.remove(module.id());
            if (orphanModule != null) {
                this.retainedEntries.put(module.id(), orphanModule.deepCopy());
                this.applyModuleEntry(module, orphanModule, this.pendingEnabled);
            }
        }
    }

    public synchronized void release(ClientModule module) {
        if (this.loaded) {
            JsonObject json = this.serializeModule(module);
            if (Boolean.TRUE.equals(this.pendingEnabled.remove(module.id()))) {
                json.addProperty("enabled", true);
            }
            this.orphanModules.put(module.id(), json);
            this.retainedEntries.remove(module.id());
        }
    }

    public static String renamedModuleId(String string) {
        for (Entry entry : RENAMED_MODULES.entrySet()) {
            if (((String) entry.getKey()).equalsIgnoreCase(string)) {
                return (String) entry.getValue();
            }
        }
        return null;
    }

    private void applyConfigRoot(JsonObject json) {
        if (json != null) {
            int i = json.has("version") ? json.get("version").getAsInt() : 0;
            if (i > 3) {
                this.readOnly = true;
                LOGGER.warn(
                        "[EFN] Module config version {} is newer than supported {}; skipping load",
                        i,
                        3);
            } else if (json.has("modules") && json.get("modules").isJsonObject()) {
                JsonObject json2 = renameModules(json.getAsJsonObject("modules"), i);
                for (Entry entry : json2.entrySet()) {
                    String key = (String) entry.getKey();
                    if (((JsonElement) entry.getValue()).isJsonObject()) {
                        JsonObject jsonObject = ((JsonElement) entry.getValue()).getAsJsonObject();
                        ClientModule module = ModuleRegistry.resolve(key);
                        if (module == null
                                && i >= 2
                                && jsonObject.has("name")
                                && jsonObject.get("name").isJsonPrimitive()) {
                            module = ModuleRegistry.resolve(jsonObject.get("name").getAsString());
                        }
                        if (module == null) {
                            this.orphanModules.put(key, jsonObject.deepCopy());
                        } else {
                            this.retainedEntries.put(module.id(), jsonObject.deepCopy());
                            this.applyModuleEntry(module, jsonObject, this.pendingEnabled);
                        }
                    }
                }
            }
        }
    }

    private JsonObject buildConfigRoot() {
        JsonObject json = new JsonObject();
        json.addProperty("version", 3);
        JsonObject json2 = new JsonObject();
        for (ClientModule module : ModuleRegistry.all()) {
            json2.add(module.id(), this.serializeModule(module));
        }
        for (Entry entry : this.orphanModules.entrySet()) {
            if (!json2.has((String) entry.getKey())) {
                json2.add((String) entry.getKey(), ((JsonObject) entry.getValue()).deepCopy());
            }
        }
        json.add("modules", json2);
        return json;
    }

    private void ensureLoaded() {
        if (!this.loaded) {
            this.load();
        }
    }

    private Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("modules.json");
    }

    private void applyModuleEntry(ClientModule module, JsonObject json, Map<String, Boolean> map) {
        if (json.has("keybind") && json.get("keybind").isJsonPrimitive()) {
            try {
                int i = json.get("keybind").getAsInt();
                if (i == -1 || ModuleKeybinds.isBindable(i)) {
                    module.setKeybind(i);
                }
            } catch (RuntimeException exception) {
            }
        }
        if (json.has("holdMode") && json.get("holdMode").isJsonPrimitive()) {
            try {
                module.setHoldMode(json.get("holdMode").getAsBoolean());
            } catch (RuntimeException exception2) {
            }
        }
        if (json.has("settings") && json.get("settings").isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject("settings");
            for (Setting setting : module.settings()) {
                JsonElement json2;
                if (!setting.isSessionOnly()
                        && (json2 =
                                        jsonObject.has(setting.id())
                                                ? jsonObject.get(setting.id())
                                                : jsonObject.get(setting.name()))
                                != null) {
                    setting.deserialize(json2);
                }
            }
        }
        migrateSettings(module, json);
        if (json.has("enabled") && json.get("enabled").isJsonPrimitive()) {
            boolean bl;
            try {
                bl = json.get("enabled").getAsBoolean();
            } catch (RuntimeException exception3) {
                return;
            }
            if (module.persistEnabled()) {
                String string = module.id();
                if (bl) {
                    map.put(string, true);
                } else {
                    module.setEnabled(false);
                }
            }
        }
    }

    private static void migrateSettings(ClientModule module, JsonObject json) {
        int settingsVersion = module.settingsVersion();
        if (settingsVersion > 0) {
            int i = 0;
            if (json.has("settingsVersion") && json.get("settingsVersion").isJsonPrimitive()) {
                try {
                    i = json.get("settingsVersion").getAsInt();
                } catch (RuntimeException exception) {
                }
            }
            if (i < settingsVersion) {
                try {
                    module.migrateSettings(i);
                } catch (RuntimeException exception2) {
                    LOGGER.warn(
                            "[EFN] Failed to migrate settings of '{}' from version {}",
                            module.id(),
                            i,
                            exception2);
                }
            }
        }
    }

    private static JsonObject renameModules(JsonObject json, int i) {
        if (i >= 3) {
            return json;
        } else {
            JsonObject json2 = new JsonObject();
            for (Entry entry : json.entrySet()) {
                String string = RENAMED_MODULES.get(entry.getKey());
                if (string == null) {
                    json2.add((String) entry.getKey(), (JsonElement) entry.getValue());
                } else if (!json.has(string) && !json2.has(string)) {
                    json2.add(string, (JsonElement) entry.getValue());
                }
            }
            return json2;
        }
    }

    private JsonObject serializeModule(ClientModule module) {
        JsonObject retainedEntry = this.retainedEntries.get(module.id());
        JsonObject json = retainedEntry == null ? new JsonObject() : retainedEntry.deepCopy();
        json.addProperty("name", module.name());
        json.addProperty("enabled", module.isEnabled());
        json.addProperty("keybind", module.keybind());
        json.addProperty("holdMode", module.isHoldMode());
        if (module.settingsVersion() > 0) {
            json.addProperty("settingsVersion", module.settingsVersion());
        }
        JsonObject json2 =
                json.has("settings") && json.get("settings").isJsonObject()
                        ? json.getAsJsonObject("settings").deepCopy()
                        : new JsonObject();
        for (Setting setting : module.settings()) {
            json2.remove(setting.name());
            json2.remove(sealedKey(setting));
            if (setting.isSessionOnly()) {
                json2.remove(setting.id());
            } else {
                json2.add(setting.id(), setting.serialize());
            }
        }
        json.add("settings", json2);
        return json;
    }
}
