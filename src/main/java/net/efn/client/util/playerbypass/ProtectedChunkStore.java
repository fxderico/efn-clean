package net.efn.client.util.playerbypass;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongConsumer;

@Environment(EnvType.CLIENT)
public final class ProtectedChunkStore {

    public static final int RADIUS = 2;

    static final int CONFIG_VERSION = 1;

    static final long SAVE_DELAY_NANOS = 10000000000L;

    private static final long STOP_WAIT_SECONDS = 10L;

    private static final String DEFAULT_PORT_SUFFIX = ":25565";

    private static final int NAME_PART_LENGTH = 40;

    private static final long NOT_AN_INT = -9223372036854775808L;

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final ProtectedChunkStore INSTANCE = new ProtectedChunkStore();

    private final Map<String, ProtectedChunkStore.Bucket> buckets = new HashMap<>();

    private final Executor writer =
            Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread = new Thread(runnable, "EFN Player Bypass saves");
                        thread.setDaemon(true);
                        return thread;
                    });

    private final ConcurrentHashMap<Path, long[]> queued = new ConcurrentHashMap<>();

    private CompletableFuture<Void> lastWrite = CompletableFuture.completedFuture(null);

    private volatile boolean writeFailed;

    private ProtectedChunkStore.Bucket active;

    private boolean dirty;

    private long dirtySince;

    private ProtectedChunkStore() {}

    public static ProtectedChunkStore get() {
        return INSTANCE;
    }

    public static long key(int i, int j) {
        return i & 4294967295L | (j & 4294967295L) << 32;
    }

    static int chunkX(long l) {
        return (int) l;
    }

    static int chunkZ(long l) {
        return (int) (l >>> 32);
    }

    public static String serverKey(String string) {
        String string2 = string == null ? "" : string.trim().toLowerCase(Locale.ROOT);
        if (string2.endsWith(":25565")) {
            string2 = string2.substring(0, string2.length() - ":25565".length());
        }
        return string2.isEmpty() ? "unknown" : string2;
    }

    public static String singleplayerKey(String string) {
        return "singleplayer/" + string;
    }

    public static void forEachInArea(int i, int j, LongConsumer longConsumer) {
        for (int k = i - 2; k <= i + 2; k++) {
            for (int m = j - 2; m <= j + 2; m++) {
                longConsumer.accept(key(k, m));
            }
        }
    }

    static String fileName(String string, String string2) {
        try {
            byte[] bytes =
                    MessageDigest.getInstance("SHA-256")
                            .digest((string + "\n" + string2).getBytes(StandardCharsets.UTF_8));
            String formatHex = HexFormat.of().formatHex(bytes, 0, 8);
            return readable(string) + "_" + readable(string2) + "-" + formatHex + ".json";
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is always available", exception);
        }
    }

    public void select(String string, String string2) {
        if (string != null && string2 != null) {
            String fileName = fileName(string, string2);
            ProtectedChunkStore.Bucket bucket = this.buckets.get(fileName);
            if (bucket == null) {
                bucket =
                        new ProtectedChunkStore.Bucket(
                                this.directory().resolve(fileName), string, string2);
                load(bucket);
                this.buckets.put(fileName, bucket);
            }
            this.active = bucket;
        } else {
            this.active = null;
        }
    }

    public boolean isProtected(long l) {
        return this.active != null && this.active.chunks.contains(l);
    }

    public boolean protect(long l) {
        if (this.active != null && this.active.chunks.add(l)) {
            this.changed(this.active);
            return true;
        } else {
            return false;
        }
    }

    public boolean flagAllowed(long l, boolean bl) {
        return bl || !this.isProtected(l);
    }

    public void tickAutosave() {
        this.retryFailedWrites();
        if (this.dirty && System.nanoTime() - this.dirtySince >= 10000000000L) {
            this.saveChanged();
        }
    }

    public void flush() {
        this.retryFailedWrites();
        if (this.dirty) {
            this.saveChanged();
        }
    }

    public void flushAndWait() {
        this.flush();
        try {
            this.lastWrite.get(10L, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (TimeoutException | ExecutionException exception2) {
            LOGGER.warn(
                    "[EFN] Player Bypass protected chunks may not all be saved",
                    (Throwable) exception2);
        }
    }

    private void changed(ProtectedChunkStore.Bucket bucket) {
        if (!bucket.readOnly) {
            bucket.dirty = true;
            if (!this.dirty) {
                this.dirty = true;
                this.dirtySince = System.nanoTime();
            }
        }
    }

    private void retryFailedWrites() {
        if (this.writeFailed) {
            this.writeFailed = false;
            for (ProtectedChunkStore.Bucket bucket : this.buckets.values()) {
                if (bucket.writeFailed) {
                    bucket.writeFailed = false;
                    this.changed(bucket);
                }
            }
        }
    }

    private void saveChanged() {
        this.dirty = false;
        for (ProtectedChunkStore.Bucket bucket : this.buckets.values()) {
            if (bucket.dirty) {
                bucket.dirty = false;
                this.save(bucket);
            }
        }
    }

    private void save(ProtectedChunkStore.Bucket bucket) {
        if (this.queued.put(bucket.file, bucket.chunks.toLongArray()) == null) {
            this.lastWrite =
                    CompletableFuture.runAsync(() -> this.writeQueued(bucket), this.writer);
        }
    }

    private void writeQueued(ProtectedChunkStore.Bucket bucket) {
        long[] longs = this.queued.remove(bucket.file);
        if (longs != null) {
            try {
                write(bucket, longs);
            } catch (RuntimeException | IOException exception) {
                LOGGER.warn(
                        "[EFN] Failed to save Player Bypass protected chunks to {}",
                        bucket.file,
                        exception);
                bucket.writeFailed = true;
                this.writeFailed = true;
            }
        }
    }

    private static void write(ProtectedChunkStore.Bucket bucket, long[] longs) throws IOException {
        Files.createDirectories(bucket.file.getParent());
        Path sibling = bucket.file.resolveSibling(bucket.file.getFileName() + ".tmp");
        try (JsonWriter jsonWriter = new JsonWriter(Files.newBufferedWriter(sibling))) {
            jsonWriter.beginObject();
            jsonWriter.name("version").value(1L);
            jsonWriter.name("server").value(bucket.server);
            jsonWriter.name("dimension").value(bucket.dimension);
            jsonWriter.name("count").value((long) longs.length);
            jsonWriter.name("chunks").beginArray();
            for (long l : longs) {
                jsonWriter.value((long) chunkX(l)).value((long) chunkZ(l));
            }
            jsonWriter.endArray();
            jsonWriter.endObject();
        }
        try {
            Files.move(
                    sibling,
                    bucket.file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            Files.move(sibling, bucket.file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void load(ProtectedChunkStore.Bucket bucket) {
        if (Files.isRegularFile(bucket.file)) {
            int i = 0;
            try (JsonReader jsonReader = new JsonReader(Files.newBufferedReader(bucket.file))) {
                long l = Files.size(bucket.file) / 4L;
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String nextName = jsonReader.nextName();
                    switch (nextName) {
                        case "version":
                            i = jsonReader.nextInt();
                            break;
                        case "count":
                            long wholeInt = wholeInt(jsonReader);
                            if (wholeInt > 0L) {
                                bucket.chunks.ensureCapacity((int) Math.min(wholeInt, l));
                            }
                            break;
                        case "chunks":
                            if (jsonReader.peek() == JsonToken.BEGIN_ARRAY) {
                                readChunks(jsonReader, bucket.chunks);
                            } else {
                                jsonReader.skipValue();
                            }
                            break;
                        default:
                            jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
            } catch (RuntimeException | IOException exception) {
                LOGGER.warn(
                        "[EFN] Failed to load Player Bypass protected chunks from {}; preserving"
                                + " the file",
                        bucket.file,
                        exception);
                bucket.chunks.clear();
                bucket.readOnly = true;
                return;
            }
            if (i > 1) {
                LOGGER.warn(
                        "[EFN] Player Bypass protected chunks file {} is version {}, newer than"
                                + " supported {}; skipping load",
                        bucket.file,
                        i,
                        1);
                bucket.chunks.clear();
                bucket.readOnly = true;
            }
        }
    }

    private static void readChunks(JsonReader jsonReader, LongOpenHashSet longOpenHashSet)
            throws IOException {
        jsonReader.beginArray();
        int i = 0;
        long l = -9223372036854775808L;
        while (jsonReader.hasNext()) {
            long wholeInt = wholeInt(jsonReader);
            if ((i++ & 1) == 0) {
                l = wholeInt;
            } else if (l != -9223372036854775808L && wholeInt != -9223372036854775808L) {
                longOpenHashSet.add(key((int) l, (int) wholeInt));
            }
        }
        jsonReader.endArray();
    }

    private static long wholeInt(JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() != JsonToken.NUMBER) {
            jsonReader.skipValue();
            return -9223372036854775808L;
        } else {
            double d = jsonReader.nextDouble();
            return d == Math.rint(d) && !(d < -2.147483648E9) && !(d > 2.147483647E9)
                    ? (long) d
                    : -9223372036854775808L;
        }
    }

    private static String readable(String string) {
        StringBuilder sb = new StringBuilder(Math.min(string.length(), 40));
        for (int i = 0; i < string.length() && sb.length() < 40; i++) {
            char c = Character.toLowerCase(string.charAt(i));
            sb.append(
                    (char)
                            ((c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '.' && c != '-'
                                    ? 95
                                    : c));
        }
        return sb.toString();
    }

    private Path directory() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("efn")
                .resolve("playerbypass-protected");
    }

    @Environment(EnvType.CLIENT)
    private static final class Bucket {

        final Path file;

        final String server;

        final String dimension;

        final LongOpenHashSet chunks = new LongOpenHashSet();

        boolean readOnly;

        boolean dirty;

        volatile boolean writeFailed;

        Bucket(Path file2, String server2, String dimension2) {
            this.file = file2;
            this.server = server2;
            this.dimension = dimension2;
        }
    }
}
