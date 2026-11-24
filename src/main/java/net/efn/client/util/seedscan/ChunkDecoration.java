package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ChunkDecoration {

    public final int chunkX;

    public final int chunkZ;

    private final long[] keys;

    private final int[] first;

    private final short[] writes;

    private final ChunkDecoration.Fog[] fogs;

    private ChunkDecoration(
            int chunkX2,
            int chunkZ2,
            long[] keys2,
            int[] first2,
            short[] writes2,
            ChunkDecoration.Fog[] fogs2) {
        this.chunkX = chunkX2;
        this.chunkZ = chunkZ2;
        this.keys = keys2;
        this.first = first2;
        this.writes = writes2;
        this.fogs = fogs2;
    }

    static short pack(int i, byte b, boolean bl) {
        return (short) (i << 5 | b << 1 | (bl ? 1 : 0));
    }

    public static byte kind(short s) {
        return (byte) (s >> 1 & 15);
    }

    public static boolean possible(short s) {
        return (s & 1) != 0;
    }

    public int find(long l) {
        return Arrays.binarySearch(this.keys, l);
    }

    public int writeCount(int i) {
        return this.first[i + 1] - this.first[i];
    }

    public short write(int i, int j) {
        return this.writes[this.first[i] + j];
    }

    public List<ChunkDecoration.Fog> fogs() {
        return Arrays.asList(this.fogs);
    }

    public int size() {
        return this.keys.length;
    }

    public long key(int i) {
        return this.keys[i];
    }

    public ChunkDecoration trimmed(int i, int j, boolean bl) {
        ChunkDecoration.Builder builder = new ChunkDecoration.Builder(this.chunkX, this.chunkZ);
        for (int k = 0; k < this.keys.length; k++) {
            int unpackLongY = BlockPos.unpackLongY(this.keys[k]);
            boolean bl2 = unpackLongY >= i && unpackLongY <= j;
            for (int n = this.first[k]; n < this.first[k + 1]; n++) {
                if (bl2 || !bl && !VBlock.isBase(kind(this.writes[n]))) {
                    builder.add(this.keys[k], this.writes[n]);
                }
            }
        }
        for (ChunkDecoration.Fog fog : this.fogs) {
            builder.fog(fog);
        }
        return builder.build();
    }

    @Environment(EnvType.CLIENT)
    static final class Builder {

        final int chunkX;

        final int chunkZ;

        private final Long2ObjectOpenHashMap<ShortArrayList> byPos = new Long2ObjectOpenHashMap<>();

        private final ArrayList<ChunkDecoration.Fog> fogs = new ArrayList<>();

        Builder(int chunkX2, int chunkZ2) {
            this.chunkX = chunkX2;
            this.chunkZ = chunkZ2;
        }

        void add(long l, short s) {
            this.byPos.computeIfAbsent(l, lx -> new ShortArrayList(2)).add(s);
        }

        ShortArrayList at(long l) {
            return this.byPos.get(l);
        }

        void fog(ChunkDecoration.Fog fog) {
            this.fogs.add(fog);
        }

        List<ChunkDecoration.Fog> fogList() {
            return this.fogs;
        }

        ChunkDecoration build() {
            long[] longArray = this.byPos.keySet().toLongArray();
            Arrays.sort(longArray);
            int[] ints = new int[longArray.length + 1];
            int i = 0;
            for (Entry long2ObjectEntry : this.byPos.long2ObjectEntrySet()) {
                i += ((ShortArrayList) long2ObjectEntry.getValue()).size();
            }
            short[] shorts = new short[i];
            int j = 0;
            for (int k = 0; k < longArray.length; k++) {
                ints[k] = j;
                ShortArrayList byPo = this.byPos.get(longArray[k]);
                for (int m = 0; m < byPo.size(); m++) {
                    shorts[j++] = byPo.getShort(m);
                }
            }
            ints[longArray.length] = j;
            return new ChunkDecoration(
                    this.chunkX,
                    this.chunkZ,
                    longArray,
                    ints,
                    shorts,
                    this.fogs.toArray(new ChunkDecoration.Fog[0]));
        }
    }

    @Environment(EnvType.CLIENT)
    public record Fog(
            int order, byte kind, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        boolean contains(int i, int j, int k) {
            return i >= this.minX
                    && i <= this.maxX
                    && j >= this.minY
                    && j <= this.maxY
                    && k >= this.minZ
                    && k <= this.maxZ;
        }
    }
}
