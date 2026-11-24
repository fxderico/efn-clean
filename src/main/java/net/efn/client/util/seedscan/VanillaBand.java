package net.efn.client.util.seedscan;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class VanillaBand {

    public static final int MIN_Y = -16;

    public static final int MAX_Y = 63;

    public static final int LAYERS = 80;

    private static final int MAX_CHUNKS = 4;

    private static final int MAX_POSSIBLE = 10;

    private static final int[][][] PERMUTATIONS = permutations(4);

    public final int chunkX;

    public final int chunkZ;

    private final byte[] cells;

    private VanillaBand(int chunkX2, int chunkZ2, byte[] cells2) {
        this.chunkX = chunkX2;
        this.chunkZ = chunkZ2;
        this.cells = cells2;
    }

    private static int index(int i, int j, int k) {
        return j - -16 << 8 | (k & 15) << 4 | i & 15;
    }

    public static boolean inBand(int i) {
        return i >= -16 && i <= 63;
    }

    public byte certain(int i, int j, int k) {
        return (byte) (this.cells[index(i, j, k)] & 15);
    }

    public int families(int i, int j, int k) {
        return this.cells[index(i, j, k)] >> 4 & 15;
    }

    public static VanillaBand of(
            TerrainChunk terrainChunk,
            ChunkDecoration[] chunkDecorations,
            DecorationOrder decorationOrder) {
        byte[] bytes = new byte[20480];
        ArrayList list = new ArrayList(9);
        long[] longs = new long[9];
        int i = terrainChunk.chunkX << 4;
        int j = terrainChunk.chunkZ << 4;
        for (int k = -16; k <= 63; k++) {
            for (int m = j; m < j + 16; m++) {
                for (int n = i; n < i + 16; n++) {
                    byte kind = terrainChunk.kind(n, k, m);
                    list.clear();
                    long l = BlockPos.asLong(n, k, m);
                    for (ChunkDecoration chunkDecoration : chunkDecorations) {
                        short[] shorts;
                        if (chunkDecoration != null
                                && (shorts = sequence(chunkDecoration, l, n, k, m)) != null) {
                            longs[list.size()] =
                                    ChunkPos.toLong(chunkDecoration.chunkX, chunkDecoration.chunkZ);
                            list.add(shorts);
                        }
                    }
                    bytes[index(n, k, m)] = play(kind, list, longs, decorationOrder);
                }
            }
        }
        return new VanillaBand(terrainChunk.chunkX, terrainChunk.chunkZ, bytes);
    }

    private static short[] sequence(ChunkDecoration chunkDecoration, long l, int i, int j, int k) {
        int find = chunkDecoration.find(l);
        int m = find >= 0 ? chunkDecoration.writeCount(find) : 0;
        ArrayList list = null;
        for (ChunkDecoration.Fog fog : chunkDecoration.fogs()) {
            if (fog.contains(i, j, k)) {
                if (list == null) {
                    list = new ArrayList(2);
                }
                list.add(fog);
            }
        }
        if (m == 0 && list == null) {
            return null;
        } else {
            short[] shorts = new short[m + (list == null ? 0 : list.size())];
            for (int o = 0; o < m; o++) {
                shorts[o] = chunkDecoration.write(find, o);
            }
            if (list != null) {
                int n = m;
                for (ChunkDecoration.Fog fog2 : (Iterable<ChunkDecoration.Fog>) list) {
                    shorts[n++] = ChunkDecoration.pack(fog2.order(), fog2.kind(), true);
                }
                Arrays.sort(shorts, 0, shorts.length);
            }
            return shorts;
        }
    }

    private static byte play(
            byte b, List<short[]> list, long[] longs, DecorationOrder decorationOrder) {
        if (list.isEmpty()) {
            return (byte) (VBlock.familyBit(b) << 4 | b);
        } else {
            int i = 0;
            Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                for (short s : (short[]) iterator.next()) {
                    if (ChunkDecoration.possible((short) s)) {
                        i++;
                    }
                }
            }
            if (list.size() <= 4 && i <= 10) {
                int j = 0;
                for (int[] ints : PERMUTATIONS[list.size()]) {
                    if (decorationOrder == null || fits(ints, longs, decorationOrder)) {
                        for (int k = 0; k < 1 << i; k++) {
                            byte b2 = b;
                            int m = 0;
                            for (int n : ints) {
                                for (short s2 : (short[]) list.get(n)) {
                                    boolean bl = true;
                                    if (ChunkDecoration.possible(s2)) {
                                        bl = (k >> m++ & 1) != 0;
                                    }
                                    if (bl && VBlock.isBase(b2)) {
                                        b2 = ChunkDecoration.kind(s2);
                                    }
                                }
                            }
                            j |= 1 << b2;
                        }
                    }
                }
                if (j == 0) {
                    return play(b, list, longs, null);
                } else {
                    int o = 0;
                    for (byte b3 = 0; b3 < 16; b3++) {
                        if ((j >> b3 & 1) != 0) {
                            o |= VBlock.familyBit(b3);
                        }
                    }
                    int p = Integer.bitCount(j) == 1 ? Integer.numberOfTrailingZeros(j) : 15;
                    return (byte) (o << 4 | p);
                }
            } else {
                int familyBit = VBlock.familyBit(b);
                for (short[] shorts : list) {
                    for (short s3 : shorts) {
                        familyBit |= VBlock.familyBit(ChunkDecoration.kind(s3));
                    }
                }
                return (byte) (familyBit << 4 | 15);
            }
        }
    }

    private static boolean fits(int[] ints, long[] longs, DecorationOrder decorationOrder) {
        for (int i = 0; i < ints.length; i++) {
            for (int j = i + 1; j < ints.length; j++) {
                if (decorationOrder.after(longs[ints[i]], longs[ints[j]])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int[][][] permutations(int i) {
        int[][][] intGrid = new int[i + 1][][];
        for (int j = 0; j <= i; j++) {
            ArrayList list = new ArrayList();
            permute(new int[j], new boolean[j], 0, list);
            intGrid[j] = ((int[][]) list.toArray(new int[0][]));
        }
        return intGrid;
    }

    private static void permute(int[] ints, boolean[] booleans, int i, List<int[]> list) {
        if (i == ints.length) {
            list.add((int[]) ints.clone());
        } else {
            for (int j = 0; j < ints.length; j++) {
                if (!booleans[j]) {
                    booleans[j] = true;
                    ints[i] = j;
                    permute(ints, booleans, i + 1, list);
                    booleans[j] = false;
                }
            }
        }
    }
}
