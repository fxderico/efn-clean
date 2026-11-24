package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class DecorationOrder {

    public static final int MIN_VOTES = 3;

    private final Map<DecorationOrder.Pair, int[]> votes = new HashMap<>();

    private final Long2ObjectOpenHashMap<Map<DecorationOrder.Pair, int[]>> bySource =
            new Long2ObjectOpenHashMap<>();

    public DecorationOrder() {}

    public int compare(long l, long l2) {
        if (l == l2) {
            return 0;
        } else {
            int[] vote = this.votes.get(DecorationOrder.Pair.of(l, l2));
            if (vote == null) {
                return 0;
            } else {
                boolean bl = l < l2;
                if (vote[0] >= 3 && vote[1] == 0) {
                    return bl ? 1 : -1;
                } else if (vote[1] >= 3 && vote[0] == 0) {
                    return bl ? -1 : 1;
                } else {
                    return 0;
                }
            }
        }
    }

    public boolean after(long l, long l2) {
        return this.compare(l, l2) > 0;
    }

    public Set<long[]> collect(
            ChunkDecoration chunkDecoration,
            Long2ObjectMap<ChunkDecoration> long2ObjectMap,
            DecorationOrder.TerrainLookup terrainLookup,
            DecorationOrder.Shown shown) {
        long l = ChunkPos.toLong(chunkDecoration.chunkX, chunkDecoration.chunkZ);
        HashMap map = new HashMap();
        for (int i = 0; i < chunkDecoration.size(); i++) {
            long key = chunkDecoration.key(i);
            byte lastStone = lastStone(chunkDecoration, i);
            if (lastStone >= 0) {
                int unpackLongX = BlockPos.unpackLongX(key);
                int unpackLongY = BlockPos.unpackLongY(key);
                int unpackLongZ = BlockPos.unpackLongZ(key);
                byte b = -1;
                long l2 = 0L;
                boolean bl = true;
                boolean bl2 = false;
                for (int n = -1; n <= 1 && bl; n++) {
                    for (int o = -1; o <= 1 && bl; o++) {
                        long l3 = ChunkPos.toLong((unpackLongX >> 4) + n, (unpackLongZ >> 4) + o);
                        ChunkDecoration chunkDecoration2 = (ChunkDecoration) long2ObjectMap.get(l3);
                        if (chunkDecoration2 != null) {
                            for (ChunkDecoration.Fog fog : chunkDecoration2.fogs()) {
                                if (fog.contains(unpackLongX, unpackLongY, unpackLongZ)) {
                                    bl = false;
                                }
                            }
                            int find;
                            if (l3 != l && (find = chunkDecoration2.find(key)) >= 0) {
                                if (bl2) {
                                    bl = false;
                                } else {
                                    bl2 = true;
                                    b = lastStone(chunkDecoration2, find);
                                    l2 = l3;
                                    if (b < 0) {
                                        bl = false;
                                    }
                                }
                            }
                        }
                    }
                }
                TerrainChunk terrainChunk;
                if (bl
                        && bl2
                        && b != lastStone
                        && l2 >= l
                        && (terrainChunk =
                                        terrainLookup.terrain(unpackLongX >> 4, unpackLongZ >> 4))
                                != null
                        && VBlock.isBase(
                                terrainChunk.kind(unpackLongX, unpackLongY, unpackLongZ))) {
                    byte kind = shown.kind(unpackLongX, unpackLongY, unpackLongZ);
                    int[] ints =
                            (int[])
                                    map.computeIfAbsent(
                                            DecorationOrder.Pair.of(l, l2), pair -> new int[2]);
                    if (kind == lastStone) {
                        ints[0]++;
                    } else if (kind == b) {
                        ints[1]++;
                    }
                }
            }
        }
        HashSet set = new HashSet(map.keySet());
        Map map2 = this.bySource.put(l, map);
        if (map2 != null) {
            set.addAll(map2.keySet());
        }
        HashSet set2 = new HashSet();
        for (DecorationOrder.Pair pair2 : (Iterable<DecorationOrder.Pair>) set) {
            int compare = this.compare(pair2.first(), pair2.second());
            int[] ints2 = this.votes.computeIfAbsent(pair2, pair -> new int[2]);
            int[] ints3 = map2 == null ? null : (int[]) map2.get(pair2);
            int[] ints4 = (int[]) map.get(pair2);
            if (ints3 != null) {
                ints2[0] -= ints3[0];
                ints2[1] -= ints3[1];
            }
            if (ints4 != null) {
                ints2[0] += ints4[0];
                ints2[1] += ints4[1];
            }
            if (ints2[0] == 0 && ints2[1] == 0) {
                this.votes.remove(pair2);
            }
            if (this.compare(pair2.first(), pair2.second()) != compare) {
                set2.add(new long[] {pair2.first(), pair2.second()});
            }
        }
        return set2;
    }

    static long evidenceSignature(ChunkDecoration chunkDecoration) {
        long l = 1125899906842597L;
        for (int i = 0; i < chunkDecoration.size(); i++) {
            byte lastStone = lastStone(chunkDecoration, i);
            if (lastStone >= 0) {
                l = 31L * l + chunkDecoration.key(i);
                l = 31L * l + lastStone;
            }
        }
        return 31L * l + chunkDecoration.fogs().hashCode();
    }

    private static byte lastStone(ChunkDecoration chunkDecoration, int i) {
        byte b = -1;
        for (int j = 0; j < chunkDecoration.writeCount(i); j++) {
            short write = chunkDecoration.write(i, j);
            byte kind = ChunkDecoration.kind(write);
            if (ChunkDecoration.possible(write) || !VBlock.isBase(kind)) {
                return -1;
            }
            b = kind;
        }
        return b;
    }

    @Environment(EnvType.CLIENT)
    private record Pair(long first, long second) {

        static DecorationOrder.Pair of(long l, long l2) {
            return l < l2 ? new DecorationOrder.Pair(l, l2) : new DecorationOrder.Pair(l2, l);
        }
    }

    @Environment(EnvType.CLIENT)
    public interface Shown {

        byte kind(int i, int j, int k);
    }

    @Environment(EnvType.CLIENT)
    public interface TerrainLookup {

        TerrainChunk terrain(int i, int j);
    }
}
