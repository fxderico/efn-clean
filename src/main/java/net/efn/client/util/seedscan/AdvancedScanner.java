package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

@Environment(EnvType.CLIENT)
public final class AdvancedScanner implements AutoCloseable {

    public static final int MAX_RADIUS = 8;

    public static final int WORLD_LIMIT = 250000;

    private static final int SETTLE_TICKS = 20;

    private static final int SEED_CHECK_AFTER = 40;

    private static final double SEED_CHECK_FAIL = 0.5;

    private static final int HIDDEN_AFTER = 10;

    private static final int GROUND_CHECK_AFTER = 20;

    private final long seed;

    private final Thread worker;

    private volatile boolean closed;

    private volatile boolean stopped;

    private final ConcurrentLinkedQueue<Object> inbox = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<Object> outbox = new ConcurrentLinkedQueue<>();

    private final Long2ObjectOpenHashMap<List<BlobJudge.Cluster>> clusters =
            new Long2ObjectOpenHashMap<>();

    private final Long2ObjectOpenHashMap<AdvancedScanner.ChunkResult> results =
            new Long2ObjectOpenHashMap<>();

    private final LongOpenHashSet shownSent = new LongOpenHashSet();

    private final LongArrayFIFOQueue toJudge = new LongArrayFIFOQueue();

    private final LongOpenHashSet judgeQueued = new LongOpenHashSet();

    private final Long2LongOpenHashMap settling = new Long2LongOpenHashMap();

    private final LongOpenHashSet counted = new LongOpenHashSet();

    private final List<String> notices = new ArrayList<>();

    private LongOpenHashSet area = new LongOpenHashSet();

    private int areaX = -2147483648;

    private int areaZ = -2147483648;

    private int areaRadius = -1;

    private boolean areaStale;

    private long ticks;

    private int trusted;

    private int mispredicted;

    private int hidden;

    private boolean hashChecked;

    private boolean seedWarned;

    private boolean hiddenWarned;

    private boolean distrusted;

    private boolean wrongHeight;

    private boolean released;

    private AdvancedScanner(long seed2) {
        this.seed = seed2;
        this.worker = new Thread(this::work, "EFN Advanced Scanner");
        this.worker.setDaemon(true);
        this.worker.setPriority(2);
        this.worker.start();
    }

    public static AdvancedScanner start(long l) {
        return new AdvancedScanner(l);
    }

    public long seed() {
        return this.seed;
    }

    @Override
    public void close() {
        this.closed = true;
        LockSupport.unpark(this.worker);
    }

    public static boolean matchesHashedSeed(long l, long l2) {
        return BiomeAccess.hashSeed(l) == l2;
    }

    static OptionalLong hashedSeed(ClientWorld world) {
        try {
            Field field = null;
            for (Field declaredField : BiomeAccess.class.getDeclaredFields()) {
                if (declaredField.getType() == long.class
                        && !Modifier.isStatic(declaredField.getModifiers())) {
                    if (field != null) {
                        return OptionalLong.empty();
                    }
                    field = declaredField;
                }
            }
            if (field == null) {
                return OptionalLong.empty();
            } else {
                field.setAccessible(true);
                return OptionalLong.of(field.getLong(world.getBiomeAccess()));
            }
        } catch (RuntimeException | ReflectiveOperationException exception) {
            return OptionalLong.empty();
        }
    }

    public List<String> drainNotices() {
        ArrayList list = new ArrayList<>(this.notices);
        this.notices.clear();
        return list;
    }

    public List<AdvancedScanner.ChunkResult> tick(ClientWorld world, long l) {
        this.ticks++;
        Object object;
        while ((object = this.outbox.poll()) != null) {
            if (object instanceof PredictionArea.ChunkClusters chunkClusters) {
                if (chunkClusters.clusters() == null) {
                    this.clusters.remove(chunkClusters.chunk());
                } else {
                    this.clusters.put(chunkClusters.chunk(), chunkClusters.clusters());
                    this.queueJudge(chunkClusters.chunk());
                }
            } else if (object instanceof String string) {
                this.notices.add(string);
            }
        }
        if (this.stopped) {
            this.release();
            return List.of();
        } else if (world.getBottomY() == -64 && world.getHeight() == 384) {
            if (!this.hashChecked) {
                this.hashChecked = true;
                OptionalLong optionalLong = hashedSeed(world);
                if (optionalLong.isPresent()
                        && !matchesHashedSeed(this.seed, optionalLong.getAsLong())) {
                    this.notices.add(
                            "Seed " + this.seed + " does not match this server. Check the seed.");
                }
            }
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                this.updateArea(
                        world,
                        client.player.getChunkPos(),
                        Math.min(client.options.getViewDistance().getValue(), 8));
            }
            this.copyArrived(world, l);
            ArrayList list = new ArrayList();
            BlobJudge.Client client2 = clientView(world);
            int i = this.toJudge.size();
            while (i-- > 0 && !this.toJudge.isEmpty() && System.nanoTime() < l) {
                long dequeueLong = this.toJudge.dequeueLong();
                this.judgeQueued.remove(dequeueLong);
                if (this.settling.containsKey(dequeueLong)) {
                    if (this.settling.get(dequeueLong) > this.ticks) {
                        this.queueJudge(dequeueLong);
                        continue;
                    }
                    this.settling.remove(dequeueLong);
                }
                AdvancedScanner.ChunkResult chunkResult;
                List cluster;
                if ((cluster = this.clusters.get(dequeueLong)) != null
                        && this.area.contains(dequeueLong)
                        && (chunkResult = this.judge(dequeueLong, cluster, client2)) != null) {
                    this.results.put(dequeueLong, chunkResult);
                    list.add(chunkResult);
                }
            }
            return list;
        } else {
            if (!this.wrongHeight) {
                this.wrongHeight = true;
                this.notices.add("Advanced Scanner only works with vanilla world height.");
            }
            return List.of();
        }
    }

    private AdvancedScanner.ChunkResult judge(
            long l, List<BlobJudge.Cluster> list, BlobJudge.Client client) {
        ArrayList list2 = new ArrayList(list.size());
        for (BlobJudge.Cluster cluster : list) {
            BlobJudge.Verdict verdict = BlobJudge.judge(cluster, client, 1);
            list2.add(verdict);
            this.count(verdict);
        }
        return this.distrusted
                ? new AdvancedScanner.ChunkResult(l, 0, new long[0], List.of())
                : decide(l, this.results.get(l), list2);
    }

    static AdvancedScanner.ChunkResult decide(
            long l, AdvancedScanner.ChunkResult chunkResult, List<BlobJudge.Verdict> list) {
        HashMap map = new HashMap();
        if (chunkResult != null) {
            for (BlobJudge.Verdict verdict : chunkResult.verdicts()) {
                if (verdict.status() != BlobJudge.Status.INCOMPLETE) {
                    map.put(clusterKey(verdict.owner(), verdict.kind()), verdict);
                }
            }
        }
        boolean bl = false;
        ArrayList list2 = new ArrayList(list.size());
        LongArrayList longArrayList = new LongArrayList();
        LongArrayList longArrayList2 = new LongArrayList();
        for (BlobJudge.Verdict verdict2 : list) {
            if (verdict2.status() == BlobJudge.Status.INCOMPLETE) {
                BlobJudge.Verdict verdict3 =
                        (BlobJudge.Verdict) map.get(clusterKey(verdict2.owner(), verdict2.kind()));
                if (verdict3 != null) {
                    verdict2 = verdict3;
                } else {
                    bl = true;
                }
            }
            list2.add(verdict2);
            if (verdict2.alert()) {
                longArrayList2.add(verdict2.changes()[0]);
                longArrayList.addElements(longArrayList.size(), verdict2.changes());
            }
        }
        if (bl && longArrayList2.isEmpty()) {
            return null;
        } else {
            long[] longArray = longArrayList.toLongArray();
            Arrays.sort(longArray);
            int i = 0;
            for (long l2 : longArray) {
                if (i == 0 || longArray[i - 1] != l2) {
                    longArray[i++] = l2;
                }
            }
            return new AdvancedScanner.ChunkResult(
                    l, i, longArrayList2.toLongArray(), List.copyOf(list2));
        }
    }

    static void dropOutside(
            Long2ObjectOpenHashMap<AdvancedScanner.ChunkResult> long2ObjectOpenHashMap,
            LongOpenHashSet longOpenHashSet) {
        ObjectIterator objectIterator = long2ObjectOpenHashMap.long2ObjectEntrySet().fastIterator();
        while (objectIterator.hasNext()) {
            Entry entry = (Entry) objectIterator.next();
            if (!longOpenHashSet.contains(entry.getLongKey())
                    && !((AdvancedScanner.ChunkResult) entry.getValue()).flagged()) {
                objectIterator.remove();
            }
        }
    }

    private static long clusterKey(long l, byte b) {
        return l * 31L + b;
    }

    public void blockChanged(BlockPos pos) {
        if (!this.stopped) {
            if (pos.getY() >= -17 && pos.getY() <= 64) {
                int i = pos.getX() >> 4;
                int j = pos.getZ() >> 4;
                for (int k = -2; k <= 2; k++) {
                    for (int m = -2; m <= 2; m++) {
                        long l = ChunkPos.toLong(i + k, j + m);
                        if (this.clusters.containsKey(l)) {
                            this.settling.put(l, this.ticks + 20L);
                            this.queueJudge(l);
                        }
                    }
                }
            }
        }
    }

    public void chunkArrived(int i, int j) {
        if (!this.stopped) {
            this.areaStale = true;
            for (int k = -1; k <= 1; k++) {
                for (int m = -1; m <= 1; m++) {
                    this.queueJudge(ChunkPos.toLong(i + k, j + m));
                }
            }
        }
    }

    static boolean insideWorldLimit(int i, int j) {
        short s = 15625;
        return i >= -s && i < s && j >= -s && j < s;
    }

    private void queueJudge(long l) {
        if (this.judgeQueued.add(l)) {
            this.toJudge.enqueue(l);
        }
    }

    private void updateArea(ClientWorld world, ChunkPos chunkPos, int areaRadius2) {
        boolean bl =
                chunkPos.x != this.areaX
                        || chunkPos.z != this.areaZ
                        || areaRadius2 != this.areaRadius;
        if (bl || this.areaStale && this.ticks % 20L == 0L) {
            this.areaStale = false;
            this.areaX = chunkPos.x;
            this.areaZ = chunkPos.z;
            this.areaRadius = areaRadius2;
            LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
            for (int j = chunkPos.x - areaRadius2; j <= chunkPos.x + areaRadius2; j++) {
                for (int k = chunkPos.z - areaRadius2; k <= chunkPos.z + areaRadius2; k++) {
                    if (insideWorldLimit(j, k)
                            && world.getChunkManager().getWorldChunk(j, k) != null) {
                        longOpenHashSet.add(ChunkPos.toLong(j, k));
                    }
                }
            }
            this.area = longOpenHashSet;
            this.clusters.keySet().retainAll(longOpenHashSet);
            dropOutside(this.results, longOpenHashSet);
            int i = areaRadius2 + 1 + 1 + 1;
            this.shownSent.removeIf(
                    value ->
                            Math.max(
                                            Math.abs(ChunkPos.getPackedX(value) - chunkPos.x),
                                            Math.abs(ChunkPos.getPackedZ(value) - chunkPos.z))
                                    > i);
            this.send(new AdvancedScanner.Want(longOpenHashSet, chunkPos.x, chunkPos.z));
        }
    }

    private void copyArrived(ClientWorld world, long l) {
        if (this.areaRadius >= 0) {
            int i = this.areaRadius + 1 + 1 + 1;
            for (int j = 0; j <= i; j++) {
                if (System.nanoTime() >= l) {
                    return;
                }
                for (int k = this.areaX - j; k <= this.areaX + j; k++) {
                    for (int m = this.areaZ - j; m <= this.areaZ + j; m++) {
                        WorldChunk worldChunk;
                        long l2;
                        if (Math.max(Math.abs(k - this.areaX), Math.abs(m - this.areaZ)) == j
                                && !this.shownSent.contains(l2 = ChunkPos.toLong(k, m))
                                && (worldChunk = world.getChunkManager().getWorldChunk(k, m))
                                        != null) {
                            ChunkSection[] sectionArray = worldChunk.getSectionArray();
                            PalettedContainer[] palettedContainers =
                                    new PalettedContainer[sectionArray.length];
                            for (int n = 0; n < sectionArray.length; n++) {
                                if (sectionArray[n] != null && !sectionArray[n].isEmpty()) {
                                    palettedContainers[n] =
                                            sectionArray[n].getBlockStateContainer().copy();
                                }
                            }
                            this.shownSent.add(l2);
                            this.send(new AdvancedScanner.Copied(k, m, palettedContainers));
                            if (System.nanoTime() >= l) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private static BlobJudge.Client clientView(ClientWorld world) {
        return new BlobJudge.Client() {

            private int lastX = -2147483648;

            private int lastZ = -2147483648;

            private WorldChunk last;

            @Override
            public BlockState state(int i, int j, int k) {
                int m = i >> 4;
                int n = k >> 4;
                if (m != this.lastX || n != this.lastZ) {
                    this.lastX = m;
                    this.lastZ = n;
                    this.last = world.getChunkManager().getWorldChunk(m, n);
                }
                if (this.last == null) {
                    return null;
                } else {
                    int sectionIndex = this.last.getSectionIndex(j);
                    return sectionIndex >= 0 && sectionIndex < this.last.countVerticalSections()
                            ? this.last
                                    .getSection(sectionIndex)
                                    .getBlockState(i & 15, j & 15, k & 15)
                            : Blocks.VOID_AIR.getDefaultState();
                }
            }
        };
    }

    private void count(BlobJudge.Verdict verdict) {
        switch (verdict.status()) {
            case CLEAN:
            case ALERT:
            case MISPREDICTED:
            case HIDDEN:
                if (this.counted.add(clusterKey(verdict.owner(), verdict.kind()))) {
                    switch (verdict.status()) {
                        case CLEAN:
                        case ALERT:
                            this.trusted++;
                            break;
                        case MISPREDICTED:
                            this.mispredicted++;
                            break;
                        default:
                            this.hidden++;
                    }
                    int i = this.trusted + this.mispredicted;
                    if (!this.seedWarned && i >= 40 && this.mispredicted > 0.5 * i) {
                        this.seedWarned = true;
                        this.distrusted = true;
                        this.notices.add(
                                "Ores do not match seed "
                                        + this.seed
                                        + ". Advanced alerts are off until you change the seed.");
                    }
                    if (!this.hiddenWarned && this.hidden >= 10 && this.hidden > i) {
                        this.hiddenWarned = true;
                        this.notices.add(
                                "This server hides ores with anti-xray. Advanced Scanner can't"
                                        + " check them.");
                    }
                    return;
                }
                return;
        }
    }

    private void send(Object object) {
        if (!this.stopped && !this.closed) {
            this.inbox.add(object);
            LockSupport.unpark(this.worker);
        }
    }

    private void release() {
        this.inbox.clear();
        if (!this.released) {
            this.released = true;
            this.clusters.clear();
            this.toJudge.clear();
            this.judgeQueued.clear();
            this.settling.clear();
            this.shownSent.clear();
            this.area = new LongOpenHashSet();
        }
    }

    private void work() {
        SeedWorldgen seedWorldgen = null;
        Throwable throwable = null;
        try {
            seedWorldgen = SeedWorldgen.create(this.seed);
            this.predict(seedWorldgen);
        } catch (Throwable exception) {
            throwable = exception;
        } finally {
            this.stopped = true;
            this.inbox.clear();
        }
        if (throwable != null) {
            try {
                this.outbox.add(
                        (seedWorldgen == null
                                        ? "Advanced Scanner can't work out this world: "
                                        : "Advanced Scanner stopped: ")
                                + throwable);
            } catch (Throwable exception2) {
            }
        }
    }

    private void predict(SeedWorldgen seedWorldgen) {
        PredictionArea predictionArea = new PredictionArea(seedWorldgen);
        boolean bl = false;
        while (!this.closed) {
            Object object;
            while ((object = this.inbox.poll()) != null) {
                if (object instanceof AdvancedScanner.Want want) {
                    predictionArea.want(want.chunks(), want.x(), want.z());
                } else if (object instanceof AdvancedScanner.Copied copied) {
                    predictionArea.offer(ShownChunk.of(copied.x(), copied.z(), copied.sections()));
                }
            }
            if (predictionArea.idle()) {
                LockSupport.parkNanos(50000000L);
            } else {
                PredictionArea.ChunkClusters chunkClusters = predictionArea.step();
                if (chunkClusters != null) {
                    this.outbox.add(chunkClusters);
                }
                if (!bl && predictionArea.compared() >= 20) {
                    bl = true;
                    if (predictionArea.mismatched() * 2 > predictionArea.compared()) {
                        this.outbox.add(
                                "The terrain here does not match seed "
                                        + this.seed
                                        + ". Check the seed.");
                    }
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public record ChunkResult(
            long chunk, int blocks, long[] anchors, List<BlobJudge.Verdict> verdicts) {

        public boolean flagged() {
            return this.anchors.length > 0;
        }
    }

    @Environment(EnvType.CLIENT)
    private record Copied(int x, int z, PalettedContainer<BlockState>[] sections) {}

    @Environment(EnvType.CLIENT)
    private record Want(LongOpenHashSet chunks, int x, int z) {}
}
