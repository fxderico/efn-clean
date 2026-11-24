/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.esp;

import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.util.PerformanceBudget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongPredicate;

@Environment(value = EnvType.CLIENT)
public final class ChunkScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final Object OWNER = new Object();

    private static final Consumer[] NONE = new Consumer[0];

    private static final List<Consumer> consumers = new ArrayList<Consumer>();

    private static Consumer[] snapshot = NONE;

    private static final ArrayDeque<Long> queue = new ArrayDeque();

    private static final Set<Long> queued = new HashSet<Long>();

    private static volatile boolean resetPending;

    private static ScanCursor scanning;

    private static ClientWorld scanLevel;

    private static ScanBatch deliveryBatch;

    static ScanBatch deliveryBatch() {
        return deliveryBatch;
    }

    private ChunkScanner() {}

    public static void init() {
        ClientChunkEvents.CHUNK_LOAD.register(
                (clientWorld, worldChunk) -> ChunkScanner.enqueue(worldChunk.getPos().toLong()));
        ClientTickEvents.END_CLIENT_TICK.register(ChunkScanner::tick);
        BlockUpdateEvent.TYPE.listen(OWNER, ChunkScanner::onBlockUpdate);
        PacketReceiveNettyEvent.TYPE.listen(
                OWNER,
                packetReceiveNettyEvent -> {
                    if (packetReceiveNettyEvent.packet instanceof GameJoinS2CPacket) {
                        resetPending = true;
                    }
                });
    }

    public static void register(Consumer consumer) {
        if (consumers.contains(consumer)) {
            return;
        }
        consumers.add(consumer);
        snapshot = consumers.toArray(NONE);
        ChunkScanner.restartScan();
        ChunkScanner.queueLoadedChunks();
    }

    public static void unregister(Consumer consumer) {
        consumers.remove(consumer);
        snapshot = consumers.toArray(NONE);
        ChunkScanner.restartScan();
        if (consumers.isEmpty()) {
            queue.clear();
            queued.clear();
        }
    }

    public static int pending() {
        return queue.size() + (scanning == null ? 0 : 1);
    }

    public static void rescan() {
        ChunkScanner.restartScan();
        ChunkScanner.queueLoadedChunks();
    }

    private static void restartScan() {
        if (scanning != null) {
            long l = ChunkScanner.scanning.chunk.getPos().toLong();
            ChunkScanner.scanning.batch.finished = true;
            scanning = null;
            ChunkScanner.enqueue(l);
        }
    }

    private static void queueLoadedChunks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        int k = client.player.getBlockX() >> 4;
        int m = client.player.getBlockZ() >> 4;
        int n = client.options.getViewDistance().getValue() + 1;
        ArrayList<long[]> list = new ArrayList<long[]>();
        for (int i = k - n; i <= k + n; ++i) {
            for (int j = m - n; j <= m + n; ++j) {
                if (!client.world.getChunkManager().isChunkLoaded(i, j)) continue;
                int o = i - k;
                int p = j - m;
                list.add(new long[] {ChunkPos.toLong(i, j), o * o + p * p});
            }
        }
        list.sort(Comparator.comparingLong(ls -> ls[1]));
        for (long[] longs : list) {
            ChunkScanner.enqueue(longs[0]);
        }
    }

    private static void enqueue(long l) {
        if (consumers.isEmpty() || !queued.add(l)) {
            return;
        }
        queue.add(l);
    }

    private static void tick(MinecraftClient client) {
        Long long_Value;
        if (scanLevel != client.world) {
            if (scanning != null) {
                ChunkScanner.scanning.batch.finished = true;
            }
            scanning = null;
            scanLevel = client.world;
        }
        if (resetPending) {
            resetPending = false;
            queue.clear();
            queued.clear();
            if (scanning != null) {
                ChunkScanner.scanning.batch.finished = true;
            }
            scanning = null;
            for (Consumer consumer : snapshot) {
                ChunkScanner.call(consumer, Consumer::reset);
            }
            ChunkScanner.queueLoadedChunks();
        }
        if (client.world == null || consumers.isEmpty()) {
            return;
        }
        if (PerformanceModule.optimizationsEnabled()) {
            ChunkScanner.tickIncremental(client);
            return;
        }
        ChunkScanner.restartScan();
        long l = System.nanoTime() + PerformanceModule.scanBudgetNanos();
        while ((long_Value = queue.poll()) != null) {
            queued.remove(long_Value);
            WorldChunk worldChunk =
                    client.world
                            .getChunkManager()
                            .getWorldChunk(
                                    ChunkPos.getPackedX(long_Value),
                                    ChunkPos.getPackedZ(long_Value));
            if (worldChunk != null) {
                ChunkScanner.scan(client, worldChunk);
            }
            if (System.nanoTime() < l) continue;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void tickIncremental(MinecraftClient client) {
        long acquire = PerformanceBudget.acquire(PerformanceModule.scanBudgetNanos());
        long l = System.nanoTime();
        if (l >= acquire) {
            return;
        }
        try {
            while (System.nanoTime() < acquire) {
                Object object;
                if (scanning == null) {
                    object = queue.poll();
                    if (object == null) {
                        break;
                    }
                    queued.remove(object);
                    WorldChunk worldChunk =
                            client.world
                                    .getChunkManager()
                                    .getWorldChunk(
                                            ChunkPos.getPackedX((Long) object),
                                            ChunkPos.getPackedZ((Long) object));
                    if (worldChunk == null) continue;
                    scanning =
                            new ScanCursor(
                                    worldChunk, client.world.getDimension().minY() >> 4, snapshot);
                }
                object = scanning;
                if (client.world
                                .getChunkManager()
                                .getWorldChunk(
                                        ((ScanCursor) object).chunk.getPos().x,
                                        ((ScanCursor) object).chunk.getPos().z)
                        != ((ScanCursor) object).chunk) {
                    ((ScanCursor) object).batch.finished = true;
                    scanning = null;
                    continue;
                }
                boolean step = ((ScanCursor) object).step();
                if (scanning != object || !step) continue;
                scanning = null;
                ChunkScanner.finishScan(((ScanCursor) object).chunk);
            }
        } finally {
            PerformanceBudget.record(l);
        }
    }

    private static void scan(MinecraftClient client, WorldChunk chunk2) {
        int startX = chunk2.getPos().getStartX();
        int startZ = chunk2.getPos().getStartZ();
        int m = client.world.getDimension().minY() >> 4;
        ChunkSection[] sectionArray = chunk2.getSectionArray();
        ArrayList<Consumer> list = new ArrayList<Consumer>(consumers.size());
        for (int i = 0; i < sectionArray.length; ++i) {
            ChunkSection section = sectionArray[i];
            if (section == null || section.isEmpty()) continue;
            list.clear();
            for (Consumer consumer : consumers) {
                if (!section.hasAny(consumer::wants)) continue;
                list.add(consumer);
            }
            if (list.isEmpty()) continue;
            int n = m + i << 4;
            for (int j = 0; j < 16; ++j) {
                for (int k = 0; k < 16; ++k) {
                    for (int i2 = 0; i2 < 16; ++i2) {
                        BlockState blockState = section.getBlockState(j, i2, k);
                        if (blockState.isAir()) continue;
                        long l = BlockPos.asLong(startX + j, n + i2, startZ + k);
                        for (Consumer consumer2 : list) {
                            if (!consumer2.wants(blockState)) continue;
                            ChunkScanner.found(consumer2, l, blockState);
                        }
                    }
                }
            }
        }
        ChunkScanner.finishScan(chunk2);
    }

    private static void finishScan(WorldChunk chunk2) {
        long l = chunk2.getPos().toLong();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (Consumer consumer2 : snapshot) {
            ChunkScanner.call(
                    consumer2,
                    consumer ->
                            consumer.chunkScanned(
                                    l,
                                    packed -> {
                                        BlockState blockState =
                                                chunk2.getBlockState(mutable.set(packed));
                                        return !blockState.isAir() && consumer.wants(blockState);
                                    }));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void onBlockUpdate(BlockUpdateEvent event) {
        Consumer[] consumers2 = snapshot;
        if (consumers2.length == 0) {
            return;
        }
        long l = event.pos.asLong();
        BlockState newState = event.newState;
        ScanBatch scanBatch = deliveryBatch;
        deliveryBatch = null;
        try {
            for (Consumer consumer : consumers2) {
                if (!newState.isAir() && consumer.wants(newState)) {
                    ChunkScanner.found(consumer, l, newState);
                    continue;
                }
                ChunkScanner.removed(consumer, l);
            }
        } finally {
            deliveryBatch = scanBatch;
        }
    }

    private static void found(Consumer consumer, long l, BlockState state) {
        try {
            consumer.found(l, state);
        } catch (RuntimeException exception) {
            ChunkScanner.drop(consumer, exception);
        }
    }

    private static void removed(Consumer consumer, long l) {
        try {
            consumer.removed(l);
        } catch (RuntimeException exception) {
            ChunkScanner.drop(consumer, exception);
        }
    }

    private static void call(Consumer consumer, java.util.function.Consumer<Consumer> consumer2) {
        try {
            consumer2.accept(consumer);
        } catch (RuntimeException exception) {
            ChunkScanner.drop(consumer, exception);
        }
    }

    private static void drop(Consumer consumer, RuntimeException exception) {
        LOGGER.error(
                "[EFN] Chunk scan consumer {} failed; disabling it",
                (Object) consumer.getClass().getSimpleName(),
                (Object) exception);
        consumers.remove(consumer);
        snapshot = consumers.toArray(NONE);
        ChunkScanner.restartScan();
    }

    @Environment(value = EnvType.CLIENT)
    static final class ScanBatch {

        boolean finished;

        ScanBatch() {}
    }

    @Environment(value = EnvType.CLIENT)
    public static interface Consumer {

        public boolean wants(BlockState state);

        public void found(long l, BlockState state);

        public void removed(long l);

        public void reset();

        public default void chunkScanned(long l, LongPredicate longPredicate) {}
    }

    @Environment(value = EnvType.CLIENT)
    private static final class ScanCursor {

        final WorldChunk chunk;

        final int minSection;

        final Consumer[] listeners;

        final ScanBatch batch = new ScanBatch();

        final List<Consumer> interested = new ArrayList<Consumer>();

        int sectionIndex;

        int blockIndex;

        ScanCursor(WorldChunk chunk2, int minSection2, Consumer[] listeners2) {
            this.chunk = chunk2;
            this.minSection = minSection2;
            this.listeners = listeners2;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        boolean step() {
            if (this.batch.finished) {
                return true;
            }
            ScanBatch scanBatch = deliveryBatch;
            deliveryBatch = this.batch;
            try {
                boolean bl;
                boolean stepBlocks = this.stepBlocks();
                if (stepBlocks) {
                    this.batch.finished = true;
                }
                boolean bl2 = bl = stepBlocks;
                return bl2;
            } finally {
                deliveryBatch = scanBatch;
            }
        }

        private boolean stepBlocks() {
            ChunkSection[] sectionArray = this.chunk.getSectionArray();
            if (this.sectionIndex >= sectionArray.length) {
                return true;
            }
            ChunkSection section = sectionArray[this.sectionIndex];
            if (this.blockIndex == 0) {
                this.interested.clear();
                if (section != null && !section.isEmpty()) {
                    for (Consumer listener : this.listeners) {
                        if (!consumers.contains(listener) || !section.hasAny(listener::wants))
                            continue;
                        this.interested.add(listener);
                    }
                }
                if (this.interested.isEmpty()) {
                    ++this.sectionIndex;
                    return this.sectionIndex >= sectionArray.length;
                }
            }
            int i = Math.min(4096, this.blockIndex + 256);
            int startX = this.chunk.getPos().getStartX();
            int startZ = this.chunk.getPos().getStartZ();
            int j = this.minSection + this.sectionIndex << 4;
            while (this.blockIndex < i && !this.batch.finished) {
                int k = this.blockIndex++;
                int m = k >> 8;
                int n = k & 0xF;
                int o = k >> 4 & 0xF;
                BlockState blockState = section.getBlockState(m, n, o);
                if (blockState.isAir()) continue;
                long l = BlockPos.asLong(startX + m, j + n, startZ + o);
                for (Consumer consumer : this.interested) {
                    if (!consumer.wants(blockState)) continue;
                    ChunkScanner.found(consumer, l, blockState);
                }
            }
            if (this.blockIndex == 4096) {
                this.blockIndex = 0;
                ++this.sectionIndex;
            }
            return this.sectionIndex >= sectionArray.length;
        }
    }
}
