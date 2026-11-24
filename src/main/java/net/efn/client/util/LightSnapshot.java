/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LightingProvider;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Environment(value = EnvType.CLIENT)
public final class LightSnapshot {

    private static final int MAX_HITS = 250000;

    private final ArrayDeque<ChunkPos> chunks;

    private final Map<BlockPos, Integer> hits = new HashMap<BlockPos, Integer>();

    private final int minLight;

    public LightSnapshot(ChunkPos chunkPos, int i, int minLight2) {
        this.minLight = minLight2;
        ArrayList<ChunkPos> list = new ArrayList<ChunkPos>();
        for (int k = -i; k <= i; ++k) {
            for (int i2 = -i; i2 <= i; ++i2) {
                list.add(new ChunkPos(chunkPos.x + k, chunkPos.z + i2));
            }
        }
        list.sort(
                Comparator.comparingInt(
                        chunkPos2 ->
                                (chunkPos2.x - chunkPos.x) * (chunkPos2.x - chunkPos.x)
                                        + (chunkPos2.z - chunkPos.z) * (chunkPos2.z - chunkPos.z)));
        this.chunks = new ArrayDeque(list);
    }

    public Map<BlockPos, Integer> hits() {
        return this.hits;
    }

    public boolean collect(ClientWorld world) {
        long l = System.nanoTime();
        int m = 0;
        while (!this.chunks.isEmpty() && m++ < 16 && System.nanoTime() - l < 2000000L) {
            ChunkPos chunk = this.chunks.removeFirst();
            if (!world.getChunkManager().isChunkLoaded(chunk.x, chunk.z)) continue;
            LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
            ChunkLightingView chunkLightingView = lightingProvider.get(LightType.BLOCK);
            ChunkLightingView chunkLightingView2 = lightingProvider.get(LightType.SKY);
            BlockPos.Mutable mutable = new BlockPos.Mutable();
            for (int i = world.getBottomY() >> 4; i < 0; ++i) {
                ChunkNibbleArray lightSection =
                        chunkLightingView.getLightSection(
                                ChunkSectionPos.from(chunk.x, i, chunk.z));
                if (lightSection == null || lightSection.isUninitialized()) continue;
                for (int j = 0; j < 16; ++j) {
                    for (int k = 0; k < 16; ++k) {
                        for (int i2 = 0; i2 < 16; ++i2) {
                            int n = lightSection.get(i2, j, k);
                            if (n < this.minLight) continue;
                            mutable.set((chunk.x << 4) + i2, (i << 4) + j, (chunk.z << 4) + k);
                            if (n > chunkLightingView2.getLightLevel(mutable)) {
                                this.hits.put(mutable.toImmutable(), n);
                            }
                            if (this.hits.size() < 250000) continue;
                            this.chunks.clear();
                            return true;
                        }
                    }
                }
            }
        }
        return this.chunks.isEmpty();
    }
}
