package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightingView;

import java.util.function.LongConsumer;

@Environment(EnvType.CLIENT)
public final class GeodeGlowScan {

    public static final int MIN_Y = -58;

    public static final int MAX_Y = 30;

    private static final int EDGE_LIGHT = 4;

    private static final int LOW_SECTION = -4;

    private static final int HIGH_SECTION = 1;

    private static final int FIRST_CACHED = -5;

    private static final int CACHED = 8;

    private static final Direction[] SIDES = Direction.values();

    private final ChunkNibbleArray[] layers = new ChunkNibbleArray[72];

    private final Mutable cursor = new Mutable();

    private int chunkX;

    private int chunkZ;

    public GeodeGlowScan() {}

    public int scan(ClientWorld world, WorldChunk chunk, LongConsumer longConsumer) {
        this.chunkX = chunk.getPos().x;
        this.chunkZ = chunk.getPos().z;
        if (!this.cacheLight(world)) {
            return 0;
        } else {
            ChunkSection[] sectionArray = chunk.getSectionArray();
            int i = this.chunkX << 4;
            int j = this.chunkZ << 4;
            int k = 0;
            for (int m = -4; m <= 1; m++) {
                int sectionCoordToIndex = chunk.sectionCoordToIndex(m);
                if (sectionCoordToIndex >= 0
                        && sectionCoordToIndex < sectionArray.length
                        && sectionArray[sectionCoordToIndex] != null
                        && this.nearLight(m)) {
                    ChunkSection section = sectionArray[sectionCoordToIndex];
                    int n = Math.max(-58, m << 4);
                    int o = Math.min(30, (m << 4) + 15);
                    for (int q = n; q <= o; q++) {
                        for (int r = 0; r < 16; r++) {
                            for (int t = 0; t < 16; t++) {
                                int p;
                                int u;
                                if (open(section.getBlockState(t, q & 15, r))
                                        && this.lightAt(u = i + t, q, p = j + r) == 0
                                        && this.edge(world, u, q, p)) {
                                    k++;
                                    if (longConsumer != null) {
                                        longConsumer.accept(BlockPos.asLong(u, q, p));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return k;
        }
    }

    private boolean cacheLight(ClientWorld world) {
        ChunkLightingView chunkLightingView = world.getLightingProvider().get(LightType.BLOCK);
        boolean bl = false;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = 0; k < 8; k++) {
                    ChunkNibbleArray lightSection =
                            chunkLightingView.getLightSection(
                                    ChunkSectionPos.from(this.chunkX + i, -5 + k, this.chunkZ + j));
                    if (lightSection != null && lightSection.isUninitialized()) {
                        lightSection = null;
                    }
                    this.layers[slot(i, j, k)] = lightSection;
                    bl |= lightSection != null;
                }
            }
        }
        return bl;
    }

    private boolean nearLight(int i) {
        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
                for (int m = i - 1; m <= i + 1; m++) {
                    if (this.layers[slot(j, k, m - -5)] != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean edge(ClientWorld world, int i, int j, int k) {
        boolean bl = false;
        int m = 0;
        for (Direction direction : SIDES) {
            int n = i + direction.getOffsetX();
            int o;
            int p;
            int light =
                    this.lightAt(n, p = j + direction.getOffsetY(), o = k + direction.getOffsetZ());
            if (light > 4) {
                return false;
            }
            m = Math.max(m, light);
            if (light == 4 && !bl && open(world.getBlockState(this.cursor.set(n, p, o)))) {
                bl = true;
            }
        }
        return m == 4 && bl;
    }

    private int lightAt(int i, int j, int k) {
        int m = (j >> 4) - -5;
        if (m >= 0 && m < 8) {
            ChunkNibbleArray layer =
                    this.layers[slot((i >> 4) - this.chunkX, (k >> 4) - this.chunkZ, m)];
            return layer == null ? 0 : layer.get(i & 15, j & 15, k & 15);
        } else {
            return 0;
        }
    }

    private static int slot(int i, int j, int k) {
        return ((i + 1) * 3 + j + 1) * 8 + k;
    }

    private static boolean open(BlockState state) {
        return state.isAir() || state.isOf(Blocks.AMETHYST_CLUSTER);
    }
}
