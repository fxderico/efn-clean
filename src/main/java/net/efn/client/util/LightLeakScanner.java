package net.efn.client.util;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import net.efn.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightingView;

@Environment(EnvType.CLIENT)
public final class LightLeakScanner {

    private static final int MIN_INSIDE = 2;

    private static final int DARK_GAP = 3;

    private static final long BUDGET_NANOS = 1500000L;

    private static final int NOISY_CHUNK = 512;

    private final LongLinkedOpenHashSet queue = new LongLinkedOpenHashSet();

    public LightLeakScanner() {}

    public void clear() {
        this.queue.clear();
    }

    public void queueLoaded(ClientWorld world, BlockPos pos, int i) {
        int j = pos.getX() >> 4;
        int k = pos.getZ() >> 4;
        LongArrayList longArrayList = new LongArrayList();
        for (int m = 0; m <= i; m++) {
            for (int n = -m; n <= m; n++) {
                for (int o = -m; o <= m; o++) {
                    if (Math.max(Math.abs(n), Math.abs(o)) == m
                            && LevelUtil.isLoaded(world, j + n, k + o)) {
                        longArrayList.add(ChunkPos.toLong(j + n, k + o));
                    }
                }
            }
        }
        for (int p = 0; p < longArrayList.size(); p++) {
            this.queue.add(longArrayList.getLong(p));
        }
    }

    public void onPacketApplied(Packet<?> packet) {
        if (packet instanceof ChunkDataS2CPacket chunkDataPacket) {
            this.queue.add(
                    ChunkPos.toLong(chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ()));
        } else if (packet instanceof LightUpdateS2CPacket lightUpdatePacket) {
            this.queue.add(
                    ChunkPos.toLong(lightUpdatePacket.getChunkX(), lightUpdatePacket.getChunkZ()));
        }
    }

    public void tick(ClientWorld world) {
        if (!this.queue.isEmpty()) {
            boolean optimizationsEnabled = PerformanceModule.optimizationsEnabled();
            long l = System.nanoTime();
            long l2 = optimizationsEnabled ? PerformanceBudget.acquire(1500000L) : l + 1500000L;
            while (!this.queue.isEmpty() && System.nanoTime() < l2) {
                long removeFirstLong = this.queue.removeFirstLong();
                scan(
                        world,
                        ChunkPos.getPackedX(removeFirstLong),
                        ChunkPos.getPackedZ(removeFirstLong));
            }
            if (optimizationsEnabled) {
                PerformanceBudget.record(l);
            }
        }
    }

    private static void scan(ClientWorld world, int i, int j) {
        WorldChunk chunk = world.getChunkManager().getChunk(i, j, ChunkStatus.FULL, false);
        if (chunk != null) {
            ChunkSection[] sectionArray = chunk.getSectionArray();
            ChunkLightingView chunkLightingView = world.getLightingProvider().get(LightType.BLOCK);
            LongArrayList longArrayList = new LongArrayList();
            for (int k = 0; k < sectionArray.length; k++) {
                ChunkSection section = sectionArray[k];
                int sectionIndexToCoord;
                ChunkNibbleArray lightSection;
                if (!section.isEmpty()
                        && (lightSection =
                                        chunkLightingView.getLightSection(
                                                ChunkSectionPos.from(
                                                        i,
                                                        sectionIndexToCoord =
                                                                chunk.sectionIndexToCoord(k),
                                                        j)))
                                != null
                        && !lightSection.isUninitialized()
                        && anyLight(lightSection)) {
                    scanSection(
                            world,
                            section,
                            lightSection,
                            i << 4,
                            sectionIndexToCoord << 4,
                            j << 4,
                            longArrayList);
                    if (longArrayList.size() > 512) {
                        return;
                    }
                }
            }
            for (int n = 0; n < longArrayList.size(); n++) {
                BlockEntityLeakTracker.hint(
                        BlockPos.fromLong(longArrayList.getLong(n)),
                        Blocks.AIR,
                        BlockEntityLeakTracker.Source.LIGHT);
            }
        }
    }

    private static void scanSection(
            ClientWorld world,
            ChunkSection section,
            ChunkNibbleArray chunkNibbleArray,
            int i,
            int j,
            int k,
            LongArrayList longArrayList) {
        for (int m = 0; m < 16; m++) {
            for (int n = 0; n < 16; n++) {
                for (int o = 0; o < 16; o++) {
                    BlockState blockState = section.getBlockState(o, m, n);
                    int chunkNibble = chunkNibbleArray.get(o, m, n);
                    if (blockState.getOpacity() >= 15) {
                        if (chunkNibble >= 2 && chunkNibble > blockState.getLuminance()) {
                            longArrayList.add(BlockPos.asLong(i + o, j + m, k + n));
                        }
                    } else {
                        BlockPos pos;
                        if (blockState.isAir()
                                && chunkNibble <= 12
                                && brightestInside(chunkNibbleArray, o, m, n) - chunkNibble >= 3
                                && !nextToGeode(world, pos = new BlockPos(i + o, j + m, k + n))) {
                            longArrayList.add(pos.asLong());
                        }
                    }
                }
            }
        }
    }

    private static int brightestInside(ChunkNibbleArray chunkNibbleArray, int i, int j, int k) {
        int m = 0;
        if (i > 0) {
            m = Math.max(m, chunkNibbleArray.get(i - 1, j, k));
        }
        if (i < 15) {
            m = Math.max(m, chunkNibbleArray.get(i + 1, j, k));
        }
        if (j > 0) {
            m = Math.max(m, chunkNibbleArray.get(i, j - 1, k));
        }
        if (j < 15) {
            m = Math.max(m, chunkNibbleArray.get(i, j + 1, k));
        }
        if (k > 0) {
            m = Math.max(m, chunkNibbleArray.get(i, j, k - 1));
        }
        if (k < 15) {
            m = Math.max(m, chunkNibbleArray.get(i, j, k + 1));
        }
        return m;
    }

    private static boolean anyLight(ChunkNibbleArray chunkNibbleArray) {
        for (byte b : chunkNibbleArray.asByteArray()) {
            if (b != 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean nextToGeode(ClientWorld world, BlockPos pos) {
        for (BlockPos pos2 :
                new BlockPos[] {
                    pos.up(), pos.down(), pos.north(), pos.south(), pos.east(), pos.west()
                }) {
            BlockState blockState = world.getBlockState(pos2);
            if (blockState.isOf(Blocks.BUDDING_AMETHYST)
                    || blockState.isOf(Blocks.AMETHYST_BLOCK)) {
                return true;
            }
        }
        return false;
    }
}
