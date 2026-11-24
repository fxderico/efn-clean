package net.efn.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class FriendStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final FriendStore INSTANCE = new FriendStore();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int CONFIG_VERSION = 1;

    private static final Pattern VALID_NAME = Pattern.compile("[.*]?[A-Za-z0-9_]{1,16}");

    private final Map<String, String> friends = new LinkedHashMap<>();

    private boolean loaded;

    private boolean readOnly;

    private FriendStore() {}

    public static FriendStore getInstance() {
        return INSTANCE;
    }

    public static boolean isValidName(String string) {
        return string != null && VALID_NAME.matcher(string).matches();
    }

    public synchronized boolean isFriend(String string) {
        this.ensureLoaded();
        return string != null && this.friends.containsKey(string.toLowerCase(Locale.ROOT));
    }

    public synchronized List<String> all() {
        this.ensureLoaded();
        return List.copyOf(this.friends.values());
    }

    public synchronized boolean add(String string) {
        this.ensureLoaded();
        String string2 = string == null ? "" : string.trim();
        if (!isValidName(string2)) {
            return false;
        } else if (this.friends.putIfAbsent(string2.toLowerCase(Locale.ROOT), string2) != null) {
            return false;
        } else {
            this.save();
            return true;
        }
    }

    public synchronized boolean remove(String string) {
        this.ensureLoaded();
        if (string != null && this.friends.remove(string.toLowerCase(Locale.ROOT)) != null) {
            this.save();
            return true;
        } else {
            return false;
        }
    }

    public synchronized void replaceAll(Collection<String> collection) {
        this.ensureLoaded();
        this.friends.clear();
        for (String string : collection) {
            String string2 = string == null ? "" : string.trim();
            if (isValidName(string2)) {
                this.friends.putIfAbsent(string2.toLowerCase(Locale.ROOT), string2);
            }
        }
        this.save();
    }

    public synchronized void load() {
        if (!this.loaded) {
            this.loaded = true;
            this.friends.clear();
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
                        if (i > 1) {
                            this.readOnly = true;
                            LOGGER.warn(
                                    "[EFN] Friend list version {} is newer than supported {};"
                                            + " skipping load",
                                    i,
                                    1);
                            return;
                        }
                        if (jsonObject.has("friends") && jsonObject.get("friends").isJsonArray()) {
                            for (JsonElement json : jsonObject.getAsJsonArray("friends")) {
                                String string;
                                if (json.isJsonPrimitive()
                                        && isValidName(string = json.getAsString().trim())) {
                                    this.friends.putIfAbsent(
                                            string.toLowerCase(Locale.ROOT), string);
                                }
                            }
                            return;
                        }
                    }
                } catch (RuntimeException | IOException exception) {
                    LOGGER.warn(
                            "[EFN] Failed to load friend list; preserving the file",
                            (Throwable) exception);
                    this.friends.clear();
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
            for (String friend : this.friends.values()) {
                array.add(friend);
            }
            json.add("friends", array);
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
                LOGGER.warn("[EFN] Failed to save friend list", (Throwable) exception2);
            }
        }
    }

    private void ensureLoaded() {
        if (!this.loaded) {
            this.load();
        }
    }

    private Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("friends.json");
    }
}
