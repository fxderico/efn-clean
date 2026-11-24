package net.efn.client.easyplace.builder;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.efn.client.easyplace.EasyPlace;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Environment(EnvType.CLIENT)
final class ScaffoldStore {

    private static final ExecutorService WRITER =
            Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread = new Thread(runnable, "EasyPlace scaffold list");
                        thread.setDaemon(true);
                        return thread;
                    });

    private final Object lock = new Object();

    private int savedHash;

    private String savedWorld;

    ScaffoldStore() {}

    private static Path file() {
        return MinecraftClient.getInstance()
                .runDirectory
                .toPath()
                .resolve("config")
                .resolve("easyplace-builder-scaffold.json");
    }

    static String worldKey(MinecraftClient client) {
        if (client.world == null) {
            return null;
        } else {
            String string =
                    client.getServer() != null
                            ? "local:" + client.getServer().getSaveProperties().getLevelName()
                            : (client.getCurrentServerEntry() != null
                                    ? client.getCurrentServerEntry()
                                            .address
                                            .toLowerCase(Locale.ROOT)
                                    : "unknown");
            return string + " " + client.world.getRegistryKey();
        }
    }

    Set<Long> load(String savedWorld2) {
        HashSet set = new HashSet();
        Object lock2 = this.lock;
        synchronized (lock2) {
            JsonObject json = read();
            JsonElement json2 = json.get(savedWorld2);
            if (json2 instanceof JsonArray) {
                for (JsonElement json3 : (JsonArray) json2) {
                    set.add(json3.getAsLong());
                }
            }
        }
        this.savedWorld = savedWorld2;
        this.savedHash = set.hashCode();
        return set;
    }

    void save(String savedWorld2, Set<Long> set) {
        if (savedWorld2 != null) {
            int i = set.hashCode();
            if (!savedWorld2.equals(this.savedWorld) || i != this.savedHash) {
                this.savedWorld = savedWorld2;
                this.savedHash = i;
                long[] longs = set.stream().mapToLong(Long::longValue).toArray();
                WRITER.execute(() -> this.write(savedWorld2, longs));
            }
        }
    }

    private void write(String string, long[] longs) {
        Object lock2 = this.lock;
        synchronized (lock2) {
            JsonObject json = read();
            if (longs.length == 0) {
                json.remove(string);
            } else {
                JsonArray array = new JsonArray();
                for (long l : longs) {
                    array.add(l);
                }
                json.add(string, array);
            }
            try {
                Path path = file();
                Files.createDirectories(path.getParent());
                Path sibling = path.resolveSibling(path.getFileName() + ".tmp");
                Files.writeString(
                        sibling,
                        new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement) json),
                        StandardCharsets.UTF_8);
                Files.move(
                        sibling,
                        path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                EasyPlace.LOG.warn(
                        "Could not save the builder's scaffold list", (Throwable) exception);
            }
        }
    }

    private static JsonObject read() {
        Path path = file();
        if (!Files.isRegularFile(path)) {
            return new JsonObject();
        } else {
            try {
                JsonElement string =
                        JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
                return string.isJsonObject() ? string.getAsJsonObject() : new JsonObject();
            } catch (RuntimeException | IOException exception) {
                EasyPlace.LOG.warn(
                        "Could not read the builder's scaffold list", (Throwable) exception);
                return new JsonObject();
            }
        }
    }
}
