package net.efn.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import net.efn.client.gui.HudLayoutStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class ConfigProfiles {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final ConfigProfiles INSTANCE = new ConfigProfiles();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Pattern VALID_NAME = Pattern.compile("[\\p{L}\\p{Nd} _()-]{1,32}");

    private static final Pattern NUMBERED = Pattern.compile("(.+) \\(\\d{1,3}\\)");

    private static final Set<String> RESERVED =
            Set.of(
                    "con", "prn", "aux", "nul", "com1", "com2", "com3", "com4", "com5", "com6",
                    "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7",
                    "lpt8", "lpt9");

    private static final String EXTENSION = ".json";

    private static final String FORMAT_KEY = "efnProfile";

    private static final int FORMAT = 2;

    private static final int OLDEST_FORMAT = 2;

    public static final String TOO_OLD = "Made by an old EFN - old configs no longer work";

    private static final String UPGRADED_MARKER = ".format2";

    private static final String PRESETS_MARKER = ".presets";

    private static final String PRIVATE_MARKER = ".private-values-removed";

    private static final int MAX_NAME = 32;

    private static final int MAX_FRIENDS = 1000;

    private static final long MAX_LOCAL_BYTES = 33554432L;

    public static final int MAX_SERVERS = 16;

    public static final String NAME_RULE = "Use 1-32 letters, digits, spaces, _ - ( )";

    public static final String BACKUP_NAME = "Auto backup";

    private final ModuleConfigStore store;

    private final Map<String, ConfigProfiles.Header> headers = new HashMap<>();

    private List<ConfigProfiles.Profile> cached;

    private boolean upgradeChecked;

    private boolean presetsChecked;

    private boolean privateChecked;

    private ConfigProfiles() {
        this(ModuleConfigStore.getInstance());
    }

    private ConfigProfiles(ModuleConfigStore store2) {
        this.store = store2;
    }

    public static ConfigProfiles getInstance() {
        return INSTANCE;
    }

    public static boolean isValidName(String string) {
        return string != null
                && string.equals(string.trim())
                && VALID_NAME.matcher(string).matches()
                && !RESERVED.contains(string.toLowerCase(Locale.ROOT));
    }

    public Path directory() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("efn")
                .resolve("profiles")
                .toAbsolutePath()
                .normalize();
    }

    public synchronized List<ConfigProfiles.Profile> list() {
        if (this.cached == null) {
            this.cached = this.scan();
        }
        return this.cached;
    }

    public synchronized void refresh() {
        this.cached = null;
    }

    public synchronized ConfigProfiles.Profile find(String string) {
        if (string == null) {
            return null;
        } else {
            for (ConfigProfiles.Profile profile2 : this.list()) {
                if (profile2.name().equalsIgnoreCase(string)) {
                    return profile2;
                }
            }
            return null;
        }
    }

    public synchronized String create(String string) throws ProfileException {
        String string2 = string == null ? "" : string.trim();
        if (!isValidName(string2)) {
            throw new ProfileException("Use 1-32 letters, digits, spaces, _ - ( )");
        } else {
            ConfigProfiles.Profile profile2 = this.find(string2);
            if (profile2 != null) {
                throw new ProfileException(
                        profile2.name() + " already exists - use its Save button");
            } else if (Files.exists(this.file(string2))) {
                this.cached = null;
                throw new ProfileException(string2 + " already exists - use its Save button");
            } else {
                ConfigProfiles.Contents contents = new ConfigProfiles.Contents();
                contents.data.put(
                        ConfigProfiles.Part.MODULES, this.capture(ConfigProfiles.Part.MODULES));
                this.write(string2, contents);
                this.activate(string2);
                return string2;
            }
        }
    }

    public synchronized Set<ConfigProfiles.Part> save(String string) throws ProfileException {
        ConfigProfiles.Profile profile2 = this.require(string);
        ConfigProfiles.Contents contents = this.read(profile2.name());
        EnumSet enumSet =
                contents.data.isEmpty()
                        ? EnumSet.of(ConfigProfiles.Part.MODULES)
                        : EnumSet.copyOf(contents.data.keySet());
        for (ConfigProfiles.Part part : (Iterable<ConfigProfiles.Part>) enumSet) {
            contents.data.put(part, this.capture(part));
        }
        this.write(profile2.name(), contents);
        this.activate(profile2.name());
        return Collections.unmodifiableSet(enumSet);
    }

    public synchronized Set<ConfigProfiles.Part> load(String string) throws ProfileException {
        ConfigProfiles.Profile profile2 = this.require(string);
        ConfigProfiles.Contents contents = this.read(profile2.name());
        EnumSet enumSet = EnumSet.noneOf(ConfigProfiles.Part.class);
        for (Entry entry : contents.data.entrySet()) {
            if (this.apply((ConfigProfiles.Part) entry.getKey(), (JsonElement) entry.getValue())) {
                enumSet.add((ConfigProfiles.Part) entry.getKey());
            }
        }
        if (enumSet.isEmpty()) {
            throw new ProfileException("Nothing in " + profile2.name() + " could be applied");
        } else {
            this.activate(profile2.name());
            return Collections.unmodifiableSet(enumSet);
        }
    }

    public synchronized void delete(String string) throws ProfileException {
        ConfigProfiles.Profile profile2 = this.require(string);
        try {
            Files.deleteIfExists(this.file(profile2.name()));
        } catch (IOException exception) {
            LOGGER.warn("[EFN] Failed to delete profile '{}'", profile2.name(), exception);
            throw new ProfileException("Could not delete " + profile2.name());
        }
        this.cached = null;
        if (profile2.name().equalsIgnoreCase(this.store.activeProfile())) {
            this.store.setActiveProfile(null);
            this.store.save();
        }
    }

    public synchronized String rename(String string, String string2) throws ProfileException {
        ConfigProfiles.Profile profile2 = this.require(string);
        String string3 = string2 == null ? "" : string2.trim();
        if (string3.equals(profile2.name())) {
            return string3;
        } else if (!isValidName(string3)) {
            throw new ProfileException("Use 1-32 letters, digits, spaces, _ - ( )");
        } else {
            ConfigProfiles.Profile profile3 = this.find(string3);
            boolean bl = profile3 != null && profile3.name().equals(profile2.name());
            if (profile3 != null && !bl) {
                throw new ProfileException(profile3.name() + " already exists");
            } else {
                Path path = this.file(profile2.name());
                Path path2 = this.file(string3);
                try {
                    if (bl) {
                        Path sibling = path.resolveSibling(profile2.name() + ".rename.tmp");
                        Files.move(path, sibling, StandardCopyOption.REPLACE_EXISTING);
                        try {
                            Files.move(sibling, path2);
                        } catch (IOException exception) {
                            Files.move(sibling, path, StandardCopyOption.REPLACE_EXISTING);
                            throw exception;
                        }
                    } else {
                        Files.move(path, path2);
                    }
                } catch (IOException exception2) {
                    LOGGER.warn(
                            "[EFN] Failed to rename profile '{}' to '{}'",
                            profile2.name(),
                            string3,
                            exception2);
                    throw new ProfileException("Could not rename " + profile2.name());
                }
                this.cached = null;
                if (profile2.name().equalsIgnoreCase(this.store.activeProfile())) {
                    this.store.setActiveProfile(string3);
                    this.store.save();
                }
                return string3;
            }
        }
    }

    public synchronized void setPart(String string, ConfigProfiles.Part part, boolean bl)
            throws ProfileException {
        ConfigProfiles.Profile profile2 = this.require(string);
        ConfigProfiles.Contents contents = this.read(profile2.name());
        if (bl != contents.data.containsKey(part)) {
            if (bl) {
                contents.data.put(part, this.capture(part));
            } else {
                if (contents.data.size() == 1) {
                    throw new ProfileException("A profile needs at least one part");
                }
                contents.data.remove(part);
            }
            this.write(profile2.name(), contents);
        }
    }

    public synchronized String addServer(String string, String string2) throws ProfileException {
        ServerAddresses.Address address = ServerAddresses.parse(string2);
        if (address == null) {
            throw new ProfileException("That isn't a server address");
        } else {
            ConfigProfiles.Profile profile2 = this.require(string);
            ConfigProfiles.Contents contents = this.read(profile2.name());
            String string3 = address.toString();
            if (contents.servers.contains(string3)) {
                throw new ProfileException(string3 + " is already on the list");
            } else if (contents.servers.size() >= 16) {
                throw new ProfileException("A profile can list up to 16 servers");
            } else {
                contents.servers.add(string3);
                this.write(profile2.name(), contents);
                return string3;
            }
        }
    }

    public synchronized void removeServer(String string, String string2) throws ProfileException {
        ConfigProfiles.Profile profile2 = this.require(string);
        ConfigProfiles.Contents contents = this.read(profile2.name());
        if (contents.servers.remove(string2)) {
            this.write(profile2.name(), contents);
        }
    }

    public synchronized void exportTo(String string, Path path) throws ProfileException {
        JsonObject json = this.exportJson(string);
        try {
            Files.writeString(path, GSON.toJson((JsonElement) json), StandardCharsets.UTF_8);
        } catch (RuntimeException | IOException exception) {
            LOGGER.warn("[EFN] Failed to export profile '{}' to {}", string, path, exception);
            throw new ProfileException("Could not write " + path.getFileName());
        }
    }

    public synchronized String shareCode(String string) throws ProfileException {
        return ProfileCodes.encode(this.exportJson(string));
    }

    public synchronized String importFile(Path path) throws ProfileException {
        String string;
        try {
            if (!Files.isRegularFile(path)) {
                throw new ProfileException("That file doesn't exist");
            }
            if (Files.size(path) > 4194304L) {
                throw new ProfileException("That file is too large to be a profile");
            }
            string = Files.readString(path, StandardCharsets.UTF_8);
        } catch (RuntimeException | IOException exception) {
            throw new ProfileException("Couldn't read " + path.getFileName());
        }
        String string2 = path.getFileName().toString();
        int i = string2.lastIndexOf(46);
        String string3 = i > 0 ? string2.substring(0, i) : string2;
        return this.importRoot(
                ProfileCodes.parseObject(string, "That file isn't an EFN profile"), string3);
    }

    public synchronized String importCode(String string) throws ProfileException {
        try {
            return this.importRoot(ProfileCodes.decode(string), "Shared profile");
        } catch (ProfileException exception) {
            LOGGER.warn(
                    "[EFN] Share code import failed ({} characters on the clipboard): {}",
                    string == null ? 0 : string.length(),
                    exception.getMessage());
            throw exception;
        }
    }

    public synchronized void backup(Set<ConfigProfiles.Part> set) throws ProfileException {
        ConfigProfiles.Contents contents = new ConfigProfiles.Contents();
        for (ConfigProfiles.Part part : set) {
            contents.data.put(part, this.capture(part));
        }
        if (contents.data.isEmpty()) {
            contents.data.put(
                    ConfigProfiles.Part.MODULES, this.capture(ConfigProfiles.Part.MODULES));
        }
        this.write("Auto backup", contents);
        this.cached = null;
    }

    public synchronized List<String> loadForServer(String string) {
        ServerAddresses.Address address = ServerAddresses.parse(string);
        if (address == null) {
            return List.of();
        } else {
            this.cached = null;
            ArrayList list = new ArrayList();
            for (ConfigProfiles.Profile profile2 : this.list()) {
                if (!profile2.broken()) {
                    int i = 0;
                    int j = 0;
                    for (String server : profile2.servers()) {
                        ServerAddresses.Address address2 = ServerAddresses.parse(server);
                        int score2 = ServerAddresses.score(address2, address);
                        if (score2 > i
                                || score2 == i && score2 > 0 && address2.host().length() > j) {
                            i = score2;
                            j = address2.host().length();
                        }
                    }
                    if (i > 0) {
                        list.add(new ConfigProfiles.Match(profile2.name(), i, j));
                    }
                }
            }
            list.sort(
                    Comparator.comparingInt(ConfigProfiles.Match::score)
                            .thenComparingInt(ConfigProfiles.Match::length)
                            .thenComparing(match -> match.name().toLowerCase(Locale.ROOT)));
            ArrayList list2 = new ArrayList();
            for (ConfigProfiles.Match match2 : (Iterable<ConfigProfiles.Match>) list) {
                try {
                    this.load(match2.name());
                    list2.add(match2.name());
                } catch (ProfileException exception) {
                    LOGGER.warn(
                            "[EFN] Could not load profile '{}' for {}: {}",
                            match2.name(),
                            address,
                            exception.getMessage());
                }
            }
            return list2;
        }
    }

    private ConfigProfiles.Profile require(String string) throws ProfileException {
        ConfigProfiles.Profile profile2 = this.find(string);
        if (profile2 == null) {
            throw new ProfileException("That profile no longer exists");
        } else {
            return profile2;
        }
    }

    private void activate(String activeProfile) {
        this.store.setActiveProfile(activeProfile);
        this.store.save();
    }

    private JsonObject exportJson(String string) throws ProfileException {
        ConfigProfiles.Profile profile2 = this.require(string);
        ConfigProfiles.Contents contents =
                this.shareable(
                        this.read(profile2.name()),
                        "Could not prepare " + profile2.name() + " for sharing");
        if (contents.data.isEmpty()) {
            throw new ProfileException(profile2.name() + " has nothing to share");
        } else {
            JsonObject json = new JsonObject();
            json.addProperty("efnProfile", 2);
            json.addProperty("name", profile2.name());
            json.add("data", data(contents));
            return json;
        }
    }

    private String importRoot(JsonObject json, String string) throws ProfileException {
        ConfigProfiles.Contents contents =
                this.shareable(parse(json, false, false), "That profile is damaged");
        if (contents.data.isEmpty()) {
            throw new ProfileException("That profile has nothing EFN can use");
        } else {
            JsonElement json2 = json.get("name");
            String string2 =
                    json2 != null && json2.isJsonPrimitive() ? json2.getAsString() : string;
            String uniqueName = this.uniqueName(string2);
            this.write(uniqueName, contents);
            return uniqueName;
        }
    }

    private JsonElement capture(ConfigProfiles.Part part) throws ProfileException {
        try {
            return (JsonElement)
                    (switch (part) {
                        case MODULES -> this.store.snapshot();
                        case HUD -> {
                            JsonObject json = new JsonObject();
                            json.add("layout", HudLayoutStore.getInstance().profileSnapshot());
                            json.add("theme", HudTheme.get().profileSnapshot());
                            yield json;
                        }
                        case THEME -> ThemeStore.get().profileSnapshot();
                        case FRIENDS -> {
                            JsonArray array = new JsonArray();
                            for (String string : FriendStore.getInstance().all()) {
                                array.add(string);
                            }
                            yield array;
                        }
                    });
        } catch (RuntimeException exception) {
            LOGGER.warn("[EFN] Failed to capture the {} part of a profile", part.key, exception);
            throw new ProfileException("Could not read your current " + part.label);
        }
    }

    private boolean apply(ConfigProfiles.Part part, JsonElement json) {
        try {
            return switch (part) {
                case MODULES ->
                        json.isJsonObject() && this.store.applyProfile(json.getAsJsonObject());
                case HUD -> applyHud(json);
                case THEME -> {
                    if (!json.isJsonObject()) {
                        yield false;
                    } else {
                        ThemeStore themeStore = ThemeStore.get();
                        themeStore.applyProfile(json.getAsJsonObject());
                        themeStore.save();
                        yield true;
                    }
                }
                case FRIENDS -> {
                    if (!json.isJsonArray()) {
                        yield false;
                    } else {
                        FriendStore.getInstance().replaceAll(friendNames(json.getAsJsonArray()));
                        yield true;
                    }
                }
            };
        } catch (RuntimeException exception) {
            LOGGER.warn("[EFN] Failed to apply the {} part of a profile", part.key, exception);
            return false;
        }
    }

    private static boolean applyHud(JsonElement json) {
        if (!json.isJsonObject()) {
            return false;
        } else {
            JsonObject jsonObject = json.getAsJsonObject();
            boolean bl = false;
            if (jsonObject.has("layout") && jsonObject.get("layout").isJsonObject()) {
                bl =
                        HudLayoutStore.getInstance()
                                .applyProfile(jsonObject.getAsJsonObject("layout"));
            }
            if (jsonObject.has("theme") && jsonObject.get("theme").isJsonObject()) {
                HudTheme hudTheme = HudTheme.get();
                hudTheme.applyProfile(jsonObject.getAsJsonObject("theme"));
                hudTheme.save();
                bl = true;
            }
            return bl;
        }
    }

    private ConfigProfiles.Contents shareable(ConfigProfiles.Contents contents, String string)
            throws ProfileException {
        ConfigProfiles.Contents contents2 = new ConfigProfiles.Contents();
        try {
            for (Entry entry : contents.data.entrySet()) {
                JsonElement json =
                        this.shareable(
                                (ConfigProfiles.Part) entry.getKey(),
                                (JsonElement) entry.getValue());
                if (json != null) {
                    contents2.data.put((ConfigProfiles.Part) entry.getKey(), json);
                }
            }
            return contents2;
        } catch (RuntimeException exception) {
            LOGGER.warn("[EFN] Failed to filter a profile for sharing", (Throwable) exception);
            throw new ProfileException(string);
        }
    }

    private JsonElement shareable(ConfigProfiles.Part part, JsonElement json) {
        return (JsonElement)
                (switch (part) {
                    case MODULES ->
                            json.isJsonObject()
                                    ? this.store.shareable(json.getAsJsonObject())
                                    : null;
                    case HUD -> {
                        if (!json.isJsonObject()) {
                            yield null;
                        } else {
                            JsonObject jsonObject = json.getAsJsonObject();
                            JsonObject json2 = new JsonObject();
                            JsonObject json3;
                            if (jsonObject.has("layout")
                                    && jsonObject.get("layout").isJsonObject()
                                    && (json3 =
                                                    HudLayoutStore.shareable(
                                                            jsonObject.getAsJsonObject("layout")))
                                            != null) {
                                json2.add("layout", json3);
                            }
                            if (jsonObject.has("theme") && jsonObject.get("theme").isJsonObject()) {
                                json2.add(
                                        "theme",
                                        HudTheme.get()
                                                .shareable(jsonObject.getAsJsonObject("theme")));
                            }
                            yield json2.size() == 0 ? null : json2;
                        }
                    }
                    case THEME ->
                            json.isJsonObject()
                                    ? ThemeStore.get().shareable(json.getAsJsonObject())
                                    : null;
                    case FRIENDS -> {
                        if (!json.isJsonArray()) {
                            yield null;
                        } else {
                            JsonArray array = new JsonArray();
                            for (String string : friendNames(json.getAsJsonArray())) {
                                array.add(string);
                            }
                            yield array;
                        }
                    }
                });
    }

    private static List<String> friendNames(JsonArray array) {
        LinkedHashMap map = new LinkedHashMap();
        for (JsonElement json : array) {
            if (map.size() >= 1000) {
                break;
            }
            String string;
            if (json.isJsonPrimitive()
                    && FriendStore.isValidName(string = json.getAsString().trim())) {
                map.putIfAbsent(string.toLowerCase(Locale.ROOT), string);
            }
        }
        return List.copyOf(map.values());
    }

    private static ConfigProfiles.Contents parse(JsonObject json, boolean bl, boolean bl2)
            throws ProfileException {
        ConfigProfiles.Contents contents = new ConfigProfiles.Contents();
        if (json.has("efnProfile")) {
            int i;
            try {
                i = json.get("efnProfile").getAsInt();
            } catch (RuntimeException exception) {
                throw new ProfileException("That isn't an EFN profile");
            }
            if (i > 2) {
                throw new ProfileException("Made by a newer EFN - update to open it");
            }
            if (i < 1) {
                throw new ProfileException("That isn't an EFN profile");
            }
            if (i < 2 && !bl2) {
                throw new ProfileException("Made by an old EFN - old configs no longer work");
            }
            JsonElement json2 = json.get("data");
            if (json2 != null && json2.isJsonObject()) {
                for (ConfigProfiles.Part part : ConfigProfiles.Part.values()) {
                    JsonElement json3 = json2.getAsJsonObject().get(part.key);
                    if (json3 != null
                            && (part == ConfigProfiles.Part.FRIENDS
                                    ? json3.isJsonArray()
                                    : json3.isJsonObject())) {
                        contents.data.put(part, json3);
                    }
                }
            }
            JsonElement json4 = json.get("loadOnJoin");
            if (bl && json4 != null && json4.isJsonArray()) {
                for (JsonElement json5 : json4.getAsJsonArray()) {
                    if (json5.isJsonPrimitive()) {
                        addServer(contents.servers, json5.getAsString());
                    }
                }
            }
        } else {
            if (!json.has("modules") || !json.get("modules").isJsonObject()) {
                throw new ProfileException("That isn't an EFN profile");
            }
            if (!bl2) {
                throw new ProfileException("Made by an old EFN - old configs no longer work");
            }
            contents.data.put(ConfigProfiles.Part.MODULES, json);
        }
        return contents;
    }

    private static void addServer(List<String> list, String string) {
        ServerAddresses.Address address = ServerAddresses.parse(string);
        if (address != null && list.size() < 16) {
            String string2 = address.toString();
            if (!list.contains(string2)) {
                list.add(string2);
            }
        }
    }

    private static JsonObject data(ConfigProfiles.Contents contents) {
        JsonObject json = new JsonObject();
        for (Entry entry : contents.data.entrySet()) {
            json.add(((ConfigProfiles.Part) entry.getKey()).key, (JsonElement) entry.getValue());
        }
        return json;
    }

    private ConfigProfiles.Contents read(String string) throws ProfileException {
        Path path = this.file(string);
        try {
            if (Files.size(path) > 33554432L) {
                throw new ProfileException(string + " is too large to open");
            } else {
                ConfigProfiles.Contents contents;
                try (BufferedReader bufferedReader =
                        Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    JsonElement reader = JsonParser.parseReader(bufferedReader);
                    if (!reader.isJsonObject()) {
                        throw new ProfileException(string + " isn't a profile file");
                    }
                    contents = parse(reader.getAsJsonObject(), true, false);
                }
                return contents;
            }
        } catch (RuntimeException | IOException exception) {
            LOGGER.warn("[EFN] Failed to read profile '{}'", string, exception);
            throw new ProfileException("Could not read " + string);
        }
    }

    private void write(String string, ConfigProfiles.Contents contents) throws ProfileException {
        JsonElement json = contents.data.get(ConfigProfiles.Part.MODULES);
        if (json != null && json.isJsonObject()) {
            contents.data.put(
                    ConfigProfiles.Part.MODULES,
                    ModuleConfigStore.withoutPrivateValues(json.getAsJsonObject()));
        }
        JsonObject json2 = new JsonObject();
        json2.addProperty("efnProfile", 2);
        JsonArray array = new JsonArray();
        for (String server : contents.servers) {
            array.add(server);
        }
        json2.add("loadOnJoin", array);
        json2.add("data", data(contents));
        Path path = this.file(string);
        try {
            Files.createDirectories(path.getParent());
            Path sibling = path.resolveSibling(string + ".json.tmp");
            try (BufferedWriter bufferedWriter =
                    Files.newBufferedWriter(sibling, StandardCharsets.UTF_8)) {
                GSON.toJson((JsonElement) json2, bufferedWriter);
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
        } catch (RuntimeException | IOException exception2) {
            LOGGER.warn("[EFN] Failed to save profile '{}'", string, exception2);
            throw new ProfileException("Could not save " + string);
        }
        this.cached = null;
    }

    private Path file(String string) throws ProfileException {
        if (!isValidName(string)) {
            throw new ProfileException("Use 1-32 letters, digits, spaces, _ - ( )");
        } else {
            Path path = this.directory();
            Path path2;
            if (!path.equals((path2 = path.resolve(string + ".json").normalize()).getParent())) {
                throw new ProfileException("Use 1-32 letters, digits, spaces, _ - ( )");
            } else {
                return path2;
            }
        }
    }

    private String uniqueName(String string) throws ProfileException {
        String cleanName = cleanName(string);
        if (!this.taken(cleanName)) {
            return cleanName;
        } else {
            Matcher matcher = NUMBERED.matcher(cleanName);
            String string2 = matcher.matches() ? matcher.group(1) : cleanName;
            for (int i = 2; i < 1000; i++) {
                String string3 = " (" + i + ")";
                String string4 = cut(string2, 32 - string3.length()).stripTrailing() + string3;
                if (isValidName(string4) && !this.taken(string4)) {
                    return string4;
                }
            }
            throw new ProfileException("Too many profiles share that name");
        }
    }

    private boolean taken(String string) {
        return this.find(string) != null
                || Files.exists(this.directory().resolve(string + ".json"));
    }

    private static String cleanName(String string) {
        StringBuilder sb = new StringBuilder();
        String string2 = string == null ? "" : string;
        int i = 0;
        while (i < string2.length()) {
            int j = string2.codePointAt(i);
            if (Character.isLetter(j) || Character.isDigit(j) || "_-()".indexOf(j) >= 0) {
                sb.appendCodePoint(j);
            }
            i += Character.charCount(j);
        }
        String string3 = cut(sb.toString().replaceAll(" {2,}", " ").trim(), 32).trim();
        return isValidName(string3) ? string3 : "Imported";
    }

    private static String cut(String string, int i) {
        return string.codePointCount(0, string.length()) <= i
                ? string
                : string.substring(0, string.offsetByCodePoints(0, i));
    }

    private List<ConfigProfiles.Profile> scan() {
        Path path = this.directory();
        this.upgradeLocalProfiles(path);
        this.removePrivateValues(path);
        this.installPresets(path);
        if (!Files.isDirectory(path)) {
            this.headers.clear();
            return List.of();
        } else {
            ArrayList list = new ArrayList();
            HashSet set = new HashSet();
            try (DirectoryStream directoryStream = Files.newDirectoryStream(path, "*.json")) {
                for (Path path2 : (Iterable<Path>) directoryStream) {
                    String string = path2.getFileName().toString();
                    String string2 = string.substring(0, string.length() - ".json".length());
                    if (isValidName(string2)
                            && Files.isRegularFile(path2)
                            && set.add(string2.toLowerCase(Locale.ROOT))) {
                        list.add(this.header(string2, path2));
                    }
                }
            } catch (IOException exception) {
                LOGGER.warn("[EFN] Failed to list profiles", (Throwable) exception);
            }
            HashSet set2 = new HashSet();
            for (ConfigProfiles.Profile profile2 : (Iterable<ConfigProfiles.Profile>) list) {
                set2.add(profile2.name());
            }
            this.headers.keySet().retainAll(set2);
            list.sort(
                    Comparator.comparing(
                            (ConfigProfiles.Profile profile3) ->
                                    profile3.name().toLowerCase(Locale.ROOT)));
            return List.copyOf(list);
        }
    }

    private void upgradeLocalProfiles(Path path) {
        if (!this.upgradeChecked) {
            this.upgradeChecked = true;
            Path path2 = path.resolve(".format2");
            try {
                if (Files.exists(path2)) {
                    return;
                }
                Files.createDirectories(path);
                try (DirectoryStream directoryStream = Files.newDirectoryStream(path, "*.json")) {
                    for (Path path3 : (Iterable<Path>) directoryStream) {
                        String string = path3.getFileName().toString();
                        String string2 = string.substring(0, string.length() - ".json".length());
                        if (isValidName(string2)
                                && Files.isRegularFile(path3)
                                && Files.size(path3) <= 33554432L) {
                            this.upgrade(string2, path3);
                        }
                    }
                }
                Files.writeString(
                        path2, "Profiles here were moved to format 2.\n", StandardCharsets.UTF_8);
            } catch (RuntimeException | IOException exception) {
                LOGGER.warn(
                        "[EFN] Failed to move old profiles to the current format",
                        (Throwable) exception);
            }
        }
    }

    private void removePrivateValues(Path path) {
        if (!this.privateChecked) {
            this.privateChecked = true;
            Path path2 = path.resolve(".private-values-removed");
            try {
                if (Files.exists(path2) || !Files.isDirectory(path)) {
                    return;
                }
                try (DirectoryStream directoryStream = Files.newDirectoryStream(path, "*.json")) {
                    for (Path path3 : (Iterable<Path>) directoryStream) {
                        String string = path3.getFileName().toString();
                        String string2 = string.substring(0, string.length() - ".json".length());
                        if (isValidName(string2)
                                && Files.isRegularFile(path3)
                                && Files.size(path3) <= 33554432L) {
                            try {
                                ConfigProfiles.Contents contents = this.read(string2);
                                JsonElement json = contents.data.get(ConfigProfiles.Part.MODULES);
                                if (json != null
                                        && json.isJsonObject()
                                        && !ModuleConfigStore.withoutPrivateValues(
                                                        json.getAsJsonObject())
                                                .equals(json)) {
                                    this.write(string2, contents);
                                }
                            } catch (ProfileException exception) {
                                LOGGER.warn(
                                        "[EFN] Couldn't clean private values out of profile '{}'",
                                        string2);
                            }
                        }
                    }
                }
                Files.writeString(
                        path2,
                        "Private values were removed from the profiles here.\n",
                        StandardCharsets.UTF_8);
            } catch (RuntimeException | IOException exception2) {
                LOGGER.warn(
                        "[EFN] Failed to clean private values out of the profiles",
                        (Throwable) exception2);
            }
        }
    }

    private void installPresets(Path path) {
        if (!this.presetsChecked) {
            this.presetsChecked = true;
            List list = BundledDefaults.presets();
            if (!list.isEmpty()) {
                Path path2 = path.resolve(".presets");
                try {
                    ArrayList list2 = new ArrayList();
                    if (Files.isRegularFile(path2)) {
                        for (String string : Files.readAllLines(path2, StandardCharsets.UTF_8)) {
                            if (!string.isBlank()) {
                                list2.add(string.trim().toLowerCase(Locale.ROOT));
                            }
                        }
                    }
                    int i = list2.size();
                    for (JsonObject json : (Iterable<JsonObject>) list) {
                        JsonElement json2 = json.get("name");
                        String string2;
                        String string3;
                        if (json2 != null
                                && json2.isJsonPrimitive()
                                && !list2.contains(
                                        string3 =
                                                (string2 = json2.getAsString())
                                                        .toLowerCase(Locale.ROOT))) {
                            try {
                                Path path3 = this.file(string2);
                                ConfigProfiles.Contents contents =
                                        this.shareable(
                                                parse(json, false, false),
                                                "That profile is damaged");
                                if (!Files.exists(path3) && !contents.data.isEmpty()) {
                                    this.write(string2, contents);
                                }
                                list2.add(string3);
                            } catch (ProfileException exception) {
                                LOGGER.warn(
                                        "[EFN] Could not add the bundled profile '{}': {}",
                                        string2,
                                        exception.getMessage());
                            }
                        }
                    }
                    if (list2.size() == i) {
                        return;
                    }
                    Files.createDirectories(path);
                    Files.write(path2, list2, StandardCharsets.UTF_8);
                } catch (RuntimeException | IOException exception2) {
                    LOGGER.warn("[EFN] Failed to add the bundled profiles", (Throwable) exception2);
                }
            }
        }
    }

    private void upgrade(String string, Path path) {
        try {
            JsonElement reader;
            try (BufferedReader bufferedReader =
                    Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                reader = JsonParser.parseReader(bufferedReader);
            }
            if (!reader.isJsonObject()) {
                return;
            }
            JsonObject jsonObject = reader.getAsJsonObject();
            JsonElement json = jsonObject.get("efnProfile");
            if (json != null && json.isJsonPrimitive() && json.getAsInt() >= 2) {
                return;
            }
            this.write(string, parse(jsonObject, true, true));
            LOGGER.info("[EFN] Moved profile '{}' to the current format", string);
        } catch (RuntimeException | ProfileException | IOException exception) {
            LOGGER.warn(
                    "[EFN] Could not move profile '{}' to the current format", string, exception);
        }
    }

    private ConfigProfiles.Profile header(String string, Path path) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            ConfigProfiles.Header header = this.headers.get(string);
            if (header != null
                    && header.modified().equals(attributes.lastModifiedTime())
                    && header.size() == attributes.size()) {
                return header.profile();
            } else {
                ConfigProfiles.Profile header2 = readHeader(string, path);
                this.headers.put(
                        string,
                        new ConfigProfiles.Header(
                                attributes.lastModifiedTime(), attributes.size(), header2));
                return header2;
            }
        } catch (IOException exception) {
            return new ConfigProfiles.Profile(string, Set.of(), List.of(), true);
        }
    }

    private static ConfigProfiles.Profile readHeader(String string, Path path) {
        EnumSet enumSet = EnumSet.noneOf(ConfigProfiles.Part.class);
        ArrayList list = new ArrayList();
        int i = -1;
        boolean bl = false;
        try (JsonReader jsonReader =
                new JsonReader(Files.newBufferedReader(path, StandardCharsets.UTF_8))) {
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String nextName = jsonReader.nextName();
                switch (nextName) {
                    case "efnProfile":
                        i = jsonReader.nextInt();
                        break;
                    case "loadOnJoin":
                        readServers(jsonReader, list);
                        break;
                    case "data":
                        readParts(jsonReader, enumSet);
                        break;
                    case "modules":
                        bl = jsonReader.peek() == JsonToken.BEGIN_OBJECT;
                        jsonReader.skipValue();
                        break;
                    default:
                        jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
        } catch (RuntimeException | IOException exception) {
            LOGGER.warn("[EFN] Failed to read profile '{}'", string, exception);
            return new ConfigProfiles.Profile(string, Set.of(), List.of(), true);
        }
        if (i < 0 && bl) {
            return new ConfigProfiles.Profile(
                    string,
                    Collections.unmodifiableSet(EnumSet.of(ConfigProfiles.Part.MODULES)),
                    List.of(),
                    true);
        } else {
            boolean bl2 = i < 2 || i > 2 || enumSet.isEmpty();
            return new ConfigProfiles.Profile(
                    string, Collections.unmodifiableSet(enumSet), List.copyOf(list), bl2);
        }
    }

    private static void readServers(JsonReader jsonReader, List<String> list) throws IOException {
        if (jsonReader.peek() != JsonToken.BEGIN_ARRAY) {
            jsonReader.skipValue();
        } else {
            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                if (jsonReader.peek() == JsonToken.STRING) {
                    addServer(list, jsonReader.nextString());
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endArray();
        }
    }

    private static void readParts(JsonReader jsonReader, Set<ConfigProfiles.Part> set)
            throws IOException {
        if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
            jsonReader.skipValue();
        } else {
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String nextName = jsonReader.nextName();
                JsonToken jsonToken = jsonReader.peek();
                for (ConfigProfiles.Part part : ConfigProfiles.Part.values()) {
                    if (part.key.equals(nextName)
                            && jsonToken
                                    == (part == ConfigProfiles.Part.FRIENDS
                                            ? JsonToken.BEGIN_ARRAY
                                            : JsonToken.BEGIN_OBJECT)) {
                        set.add(part);
                    }
                }
                jsonReader.skipValue();
            }
            jsonReader.endObject();
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Contents {

        final List<String> servers = new ArrayList<>();

        final Map<ConfigProfiles.Part, JsonElement> data = new EnumMap<>(ConfigProfiles.Part.class);

        private Contents() {}
    }

    @Environment(EnvType.CLIENT)
    private record Header(FileTime modified, long size, ConfigProfiles.Profile profile) {}

    @Environment(EnvType.CLIENT)
    private record Match(String name, int score, int length) {}

    @Environment(EnvType.CLIENT)
    public static enum Part {
        MODULES("modules", "Modules"),
        HUD("hud", "HUD"),
        THEME("theme", "Theme"),
        FRIENDS("friends", "Friends");

        public final String key;

        public final String label;

        private Part(String key2, String label2) {
            this.key = key2;
            this.label = label2;
        }
    }

    @Environment(EnvType.CLIENT)
    public record Profile(
            String name, Set<ConfigProfiles.Part> parts, List<String> servers, boolean broken) {}
}
