package net.efn.client.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LightingProvider;

import java.util.BitSet;
import java.util.Iterator;

@Environment(EnvType.CLIENT)
public final class ServerBlockLight {

    public static final int UNKNOWN = -1;

    public static final int TOP_SECTION = 3;

    public static final int TOP_Y = 63;

    private static final byte[] DARK = new byte[0];

    private final Long2ObjectOpenHashMap<ServerBlockLight.Column> columns =
            new Long2ObjectOpenHashMap<>();

    public ServerBlockLight() {}

    public void onChunk(ClientWorld world, int i, int j, LightData lightData) {
        this.columns.put(ChunkPos.toLong(i, j), apply(world, null, lightData));
    }

    public void onLight(ClientWorld world, int i, int j, LightData lightData) {
        long l = ChunkPos.toLong(i, j);
        this.columns.put(l, apply(world, this.columns.get(l), lightData));
    }

    public void onBlockChanged(BlockPos pos) {
        if (pos.getY() <= 63) {
            ServerBlockLight.Column column =
                    this.columns.get(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4));
            if (column != null) {
                if (column.touched == null) {
                    column.touched = new LongOpenHashSet();
                }
                column.touched.add(pos.asLong());
            }
        }
    }

    public ServerBlockLight.Column column(ClientWorld world, int i, int j) {
        long l = ChunkPos.toLong(i, j);
        ServerBlockLight.Column column = this.columns.get(l);
        if (column == null && LevelUtil.isLoaded(world, i, j)) {
            column = fromClient(world, i, j);
            this.columns.put(l, column);
        }
        return column;
    }

    public void forgetUnloaded(ClientWorld world) {
        this.columns
                .keySet()
                .removeIf(
                        value ->
                                !LevelUtil.isLoaded(
                                        world,
                                        ChunkPos.getPackedX(value),
                                        ChunkPos.getPackedZ(value)));
    }

    public void clear() {
        this.columns.clear();
    }

    private static ServerBlockLight.Column apply(
            ClientWorld world, ServerBlockLight.Column column, LightData lightData) {
        LightingProvider lightingProvider = world.getLightingProvider();
        int bottomY = lightingProvider.getBottomY();
        int height = lightingProvider.getHeight();
        byte[][] byteGrid =
                column != null && column.minSection == bottomY && column.sections.length == height
                        ? (byte[][]) column.sections.clone()
                        : new byte[height][];
        BitSet initedBlock = lightData.getInitedBlock();
        BitSet uninitedBlock = lightData.getUninitedBlock();
        Iterator iterator = lightData.getBlockNibbles().iterator();
        for (int k = 0; k < height; k++) {
            boolean bl = bottomY + k <= 3;
            if (initedBlock.get(k)) {
                byte[] bytes = iterator.hasNext() ? (byte[]) iterator.next() : null;
                byteGrid[k] =
                        bl && bytes != null && bytes.length == 2048 ? (byte[]) bytes.clone() : null;
            } else if (uninitedBlock.get(k)) {
                byteGrid[k] = bl ? DARK : null;
            }
        }
        return new ServerBlockLight.Column(bottomY, byteGrid);
    }

    private static ServerBlockLight.Column fromClient(ClientWorld world, int i, int j) {
        LightingProvider lightingProvider = world.getLightingProvider();
        ChunkLightingView chunkLightingView = lightingProvider.get(LightType.BLOCK);
        int bottomY = lightingProvider.getBottomY();
        byte[][] byteGrid = new byte[lightingProvider.getHeight()][];
        for (int m = 0; m < byteGrid.length && bottomY + m <= 3; m++) {
            ChunkNibbleArray lightSection =
                    chunkLightingView.getLightSection(ChunkSectionPos.from(i, bottomY + m, j));
            if (lightSection != null) {
                byteGrid[m] =
                        lightSection.isUninitialized()
                                ? DARK
                                : (byte[]) lightSection.asByteArray().clone();
            }
        }
        return new ServerBlockLight.Column(bottomY, byteGrid);
    }

    @Environment(EnvType.CLIENT)
    public static final class Column {

        private final int minSection;

        private final byte[][] sections;

        private LongOpenHashSet touched;

        private Column(int minSection2, byte[][] sections2) {
            this.minSection = minSection2;
            this.sections = sections2;
        }

        public int get(int i, int j, int k) {
            int m = (j >> 4) - this.minSection;
            if (m >= 0 && m < this.sections.length) {
                byte[] section = this.sections[m];
                if (section == null) {
                    return -1;
                } else if (this.touched != null
                        && this.touched.contains(BlockPos.asLong(i, j, k))) {
                    return -1;
                } else if (section == ServerBlockLight.DARK) {
                    return 0;
                } else {
                    int n = (j & 15) << 8 | (k & 15) << 4 | i & 15;
                    return section[n >> 1] >> ((n & 1) << 2) & 15;
                }
            } else {
                return -1;
            }
        }
    }
}
