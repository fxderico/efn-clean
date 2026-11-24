/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Environment(value = EnvType.CLIENT)
public final class BlockClusterGeometry {

    private static final int[][] AXES = new int[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

    private BlockClusterGeometry() {}

    public static Mesh build(Collection<BlockPos> collection, Membership membership) {
        ArrayList<int[]> list = new ArrayList<int[]>(collection.size() * 4);
        ArrayList<Face> list2 = new ArrayList<Face>(collection.size() * 3);
        for (BlockPos pos : collection) {
            int[] ints = new int[] {pos.getX(), pos.getY(), pos.getZ()};
            BlockClusterGeometry.addFaces(ints, membership, list2);
            BlockClusterGeometry.addEdges(ints, membership, list);
        }
        return new Mesh(BlockClusterGeometry.mergeCollinear(list), list2);
    }

    private static List<Segment> mergeCollinear(List<int[]> list2) {
        int j;
        int k;
        HashMap<List, List> map = new HashMap<List, List>();
        for (int[] ints : list2) {
            int m = ints[0];
            k = (m + 1) % 3;
            j = (m + 2) % 3;
            map.computeIfAbsent(
                            List.of(
                                    Integer.valueOf(m),
                                    Integer.valueOf(ints[1 + k]),
                                    Integer.valueOf(ints[1 + j])),
                            list -> new ArrayList())
                    .add(ints[1 + m]);
        }
        ArrayList list3 = new ArrayList(list2.size());
        for (Map.Entry entry : map.entrySet()) {
            k = (Integer) ((List) entry.getKey()).get(0);
            j = (k + 1) % 3;
            int n = (k + 2) % 3;
            List entry2 = (List) entry.getValue();
            entry2.sort(null);
            int o = (Integer) entry2.get(0);
            int p = o + 1;
            for (int i = 1; i <= entry2.size(); ++i) {
                if (i < entry2.size() && (Integer) entry2.get(i) == p) {
                    ++p;
                    continue;
                }
                int[] ints2 = new int[3];
                int[] ints3 = new int[3];
                ints2[j] = ints3[j] = ((Integer) ((List) entry.getKey()).get(1)).intValue();
                ints2[n] = ints3[n] = ((Integer) ((List) entry.getKey()).get(2)).intValue();
                ints2[k] = o;
                ints3[k] = p;
                list3.add(
                        new Segment(
                                BlockClusterGeometry.vec(ints2), BlockClusterGeometry.vec(ints3)));
                if (i >= entry2.size()) continue;
                o = (Integer) entry2.get(i);
                p = o + 1;
            }
        }
        return list3;
    }

    public static List<List<BlockPos>> connectedComponents(Collection<BlockPos> collection) {
        HashSet<BlockPos> set = new HashSet<BlockPos>(collection);
        ArrayList<List<BlockPos>> list = new ArrayList<List<BlockPos>>();
        ArrayDeque<BlockPos> arrayDeque = new ArrayDeque<BlockPos>();
        while (!set.isEmpty()) {
            BlockPos pos = set.iterator().next();
            set.remove(pos);
            ArrayList<BlockPos> list2 = new ArrayList<BlockPos>();
            arrayDeque.add(pos);
            while (!arrayDeque.isEmpty()) {
                BlockPos pos2 = (BlockPos) arrayDeque.poll();
                list2.add(pos2);
                for (int[] ints : AXES) {
                    for (int i = -1; i <= 1; i += 2) {
                        BlockPos pos3 = pos2.add(ints[0] * i, ints[1] * i, ints[2] * i);
                        if (!set.remove(pos3)) continue;
                        arrayDeque.add(pos3);
                    }
                }
            }
            list.add(list2);
        }
        return list;
    }

    private static void addFaces(int[] ints, Membership membership, List<Face> list) {
        for (int i = 0; i < 3; ++i) {
            int k = (i + 1) % 3;
            int m = (i + 2) % 3;
            for (int j = -1; j <= 1; j += 2) {
                if (membership.contains(
                        ints[0] + AXES[i][0] * j,
                        ints[1] + AXES[i][1] * j,
                        ints[2] + AXES[i][2] * j)) continue;
                int[] ints2 = (int[]) ints.clone();
                if (j > 0) {
                    int n = i;
                    ints2[n] = ints2[n] + 1;
                }
                list.add(
                        new Face(
                                BlockClusterGeometry.corner(ints2, k, 0, m, 0),
                                BlockClusterGeometry.corner(ints2, k, 1, m, 0),
                                BlockClusterGeometry.corner(ints2, k, 1, m, 1),
                                BlockClusterGeometry.corner(ints2, k, 0, m, 1)));
            }
        }
    }

    private static void addEdges(int[] ints, Membership membership, List<int[]> list) {
        for (int i = 0; i < 3; ++i) {
            int m = (i + 1) % 3;
            int n = (i + 2) % 3;
            for (int j = -1; j <= 1; j += 2) {
                for (int k = -1; k <= 1; k += 2) {
                    int o;
                    boolean bl = BlockClusterGeometry.contains(membership, ints, m, j, -1, 0);
                    boolean bl2 = BlockClusterGeometry.contains(membership, ints, n, k, -1, 0);
                    boolean bl3 = BlockClusterGeometry.contains(membership, ints, m, j, n, k);
                    boolean bl4 = !bl && !bl2;
                    boolean bl5 = bl && bl2 && !bl3;
                    boolean bl6 = bl5;
                    if (!bl4 && !bl5) continue;
                    int[] ints2 = (int[]) ints.clone();
                    if (j > 0) {
                        o = m;
                        ints2[o] = ints2[o] + 1;
                    }
                    if (k > 0) {
                        o = n;
                        ints2[o] = ints2[o] + 1;
                    }
                    list.add(new int[] {i, ints2[0], ints2[1], ints2[2]});
                }
            }
        }
    }

    private static boolean contains(Membership membership, int[] ints, int i, int j, int k, int m) {
        int[] ints2 = (int[]) ints.clone();
        int n = i;
        ints2[n] = ints2[n] + j;
        if (k >= 0) {
            int o = k;
            ints2[o] = ints2[o] + m;
        }
        return membership.contains(ints2[0], ints2[1], ints2[2]);
    }

    private static Vec3d corner(int[] ints, int i, int j, int k, int m) {
        int[] ints2 = (int[]) ints.clone();
        int n = i;
        ints2[n] = ints2[n] + j;
        int o = k;
        ints2[o] = ints2[o] + m;
        return BlockClusterGeometry.vec(ints2);
    }

    private static Vec3d vec(int[] ints) {
        return new Vec3d(ints[0], ints[1], ints[2]);
    }

    @FunctionalInterface
    @Environment(value = EnvType.CLIENT)
    public static interface Membership {

        public boolean contains(int i, int j, int k);
    }

    @Environment(value = EnvType.CLIENT)
    public record Mesh(List<Segment> edges, List<Face> faces) {}

    @Environment(value = EnvType.CLIENT)
    public record Segment(Vec3d a, Vec3d b) {}

    @Environment(value = EnvType.CLIENT)
    public record Face(Vec3d a, Vec3d b, Vec3d c, Vec3d d) {}
}
