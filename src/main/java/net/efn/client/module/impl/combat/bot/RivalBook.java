package net.efn.client.module.impl.combat.bot;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Environment(EnvType.CLIENT)
final class RivalBook {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final Gson GSON = new Gson();

    private static final int VERSION = 1;

    private static final int MAX_FILES = 64;

    private static final long MAX_BYTES = 16384L;

    private static final float WEIGHT = 0.25F;

    static final int KNOCK_UP = 0;

    static final int CRYSTALS = 1;

    static final int OPENERS = 2;

    static final int BUCKETS = 3;

    private static final float[] PRIOR = new float[] {0.45F, 0.55F};

    private static final float FADE = 0.97F;

    private final Map<UUID, RivalBook.Rival> loaded = new HashMap<>();

    private long lastSave;

    RivalBook() {}

    RivalBook.Rival of(UUID uuid) {
        return this.loaded.computeIfAbsent(uuid, RivalBook::read);
    }

    void save(boolean bl) {
        long l = System.currentTimeMillis();
        if (bl || l - this.lastSave >= 30000L) {
            this.lastSave = l;
            ArrayList list = new ArrayList();
            for (RivalBook.Rival rival : this.loaded.values()) {
                if (rival.dirty) {
                    rival.dirty = false;
                    rival.seen = l;
                    list.add(rival.copy());
                }
            }
            if (!list.isEmpty()) {
                Util.getIoWorkerExecutor()
                        .execute(
                                () -> {
                                    for (RivalBook.Rival rival2 :
                                            (Iterable<RivalBook.Rival>) list) {
                                        write(rival2);
                                    }
                                    prune();
                                });
            }
        }
    }

    private static Path folder() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("efn")
                .resolve("bots")
                .resolve("rivals");
    }

    private static RivalBook.Rival read(UUID uuid) {
        Path path = folder().resolve(uuid + ".json");
        try {
            RivalBook.Rival rival;
            if (Files.isRegularFile(path)
                    && Files.size(path) <= 16384L
                    && (rival =
                                    GSON.fromJson(
                                            Files.readString(path, StandardCharsets.UTF_8),
                                            RivalBook.Rival.class))
                            != null
                    && rival.version == 1) {
                rival.id = uuid.toString();
                if (rival.openerReward == null || rival.openerReward.length != 6) {
                    rival.openerReward = new float[6];
                }
                if (rival.openerTries == null || rival.openerTries.length != 6) {
                    rival.openerTries = new float[6];
                }
                return rival;
            }
        } catch (IOException | JsonSyntaxException exception) {
            LOGGER.warn(
                    "[EFN] Could not read a crystal bot rival entry, starting it over",
                    (Throwable) exception);
        }
        RivalBook.Rival rival2 = new RivalBook.Rival();
        rival2.id = uuid.toString();
        return rival2;
    }

    private static void write(RivalBook.Rival rival) {
        Path path = folder();
        Path path2 = path.resolve(UUID.fromString(rival.id) + ".json");
        try {
            Files.createDirectories(path);
            Path path3 = path.resolve(path2.getFileName() + ".tmp");
            Files.writeString(path3, GSON.toJson(rival), StandardCharsets.UTF_8);
            try {
                Files.move(
                        path3,
                        path2,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                Files.move(path3, path2, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IllegalArgumentException | IOException exception2) {
            LOGGER.warn("[EFN] Could not save a crystal bot rival entry", (Throwable) exception2);
        }
    }

    private static void prune() {
        ArrayList list = new ArrayList();
        try (DirectoryStream directoryStream = Files.newDirectoryStream(folder(), "*.json")) {
            for (Path path3 : (Iterable<Path>) directoryStream) {
                list.add(path3);
            }
        } catch (IOException exception) {
            return;
        }
        if (list.size() > 64) {
            list.sort((path, path2) -> Long.compare(modified((Path) path2), modified((Path) path)));
            for (Path path4 : (Iterable<Path>) list.subList(64, list.size())) {
                try {
                    Files.deleteIfExists(path4);
                } catch (IOException exception2) {
                }
            }
        }
    }

    private static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
    }

    private static float average(float f, int i, float g) {
        return i == 0 ? g : f + (g - f) * 0.25F;
    }

    @Environment(EnvType.CLIENT)
    static final class Rival {

        int version = 1;

        String id;

        long seen;

        int fights;

        float launchRate = 0.5F;

        int launches;

        float airAlong;

        float airAcross;

        int flights;

        float retotemTicks;

        int retotems;

        float holeTicks;

        int holes;

        float pearlLowRate;

        int lowSpells;

        float[] openerReward = new float[6];

        float[] openerTries = new float[6];

        transient boolean dirty;

        Rival() {}

        void fought() {
            this.fights++;
            this.dirty = true;
        }

        void launched(boolean bl) {
            this.launchRate = RivalBook.average(this.launchRate, this.launches++, bl ? 1.0F : 0.0F);
            this.dirty = true;
        }

        void flew(double d, double e) {
            this.airAlong = RivalBook.average(this.airAlong, this.flights, (float) d);
            this.airAcross = RivalBook.average(this.airAcross, this.flights++, (float) e);
            this.dirty = true;
        }

        boolean knowsFlight() {
            return this.flights >= 2;
        }

        void retotemed(int i) {
            this.retotemTicks = RivalBook.average(this.retotemTicks, this.retotems++, (float) i);
            this.dirty = true;
        }

        void leftHole(int i) {
            this.holeTicks = RivalBook.average(this.holeTicks, this.holes++, (float) i);
            this.dirty = true;
        }

        void lowSpell(boolean bl) {
            this.pearlLowRate =
                    RivalBook.average(this.pearlLowRate, this.lowSpells++, bl ? 1.0F : 0.0F);
            this.dirty = true;
        }

        boolean shrugsOffLaunches() {
            return this.launches >= 4 && this.launchRate < 0.35F;
        }

        int retotemWindow(int i) {
            return this.retotems >= 2 ? Math.round(this.retotemTicks) : i;
        }

        boolean campsInHoles() {
            return this.holes >= 2 && this.holeTicks >= 100.0F;
        }

        int opener(double d) {
            int bucket = bucket(d);
            float f = 0.0F;
            for (int j = 0; j < 2; j++) {
                f += this.openerTries[j * 3 + bucket];
            }
            int i = 0;
            double e = -1.0 / 0.0;
            for (int m = 0; m < 2; m++) {
                float openerTry = this.openerTries[m * 3 + bucket];
                if (openerTry < 0.5F) {
                    return m;
                }
                double d2 =
                        (this.openerReward[m * 3 + bucket] + RivalBook.PRIOR[m])
                                / (openerTry + 1.0F);
                double e2 =
                        d2
                                + 0.5 * Math.sqrt(Math.log(f + 1.0) / openerTry)
                                + ThreadLocalRandom.current().nextDouble(0.02);
                if (e2 > e) {
                    e = e2;
                    i = m;
                }
            }
            return i;
        }

        void opened(int i, double d, float f) {
            int j = i * 3 + bucket(d);
            for (int k = 0; k < this.openerTries.length; k++) {
                if (k % 3 == j % 3) {
                    this.openerReward[k] = this.openerReward[k] * 0.97F;
                    this.openerTries[k] = this.openerTries[k] * 0.97F;
                }
            }
            this.openerReward[j] = this.openerReward[j] + f;
            this.openerTries[j]++;
            this.dirty = true;
        }

        private static int bucket(double d) {
            return d <= 2.6 ? 0 : (d <= 3.0 ? 1 : 2);
        }

        boolean pearlsWhenLow() {
            return this.lowSpells >= 2 && this.pearlLowRate >= 0.5F;
        }

        private RivalBook.Rival copy() {
            return RivalBook.GSON.fromJson(RivalBook.GSON.toJson(this), RivalBook.Rival.class);
        }
    }
}
