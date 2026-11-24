package net.efn.client.module.impl.render.character.gltf;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;

@Environment(EnvType.CLIENT)
final class MeshReducer {

    private final float[] pos;

    private final int[] tri;

    private final boolean[] dead;

    private final int[] corner;

    private final List<List<Integer>> around = new ArrayList<>();

    private final boolean[] locked;

    private final double[] quad;

    private final int[] version;

    private int live;

    MeshReducer(PosedMesh posedMesh, List<Integer> list, boolean bl) {
        HashMap map = new HashMap();
        HashMap map2 = new HashMap();
        ArrayList list2 = new ArrayList();
        this.tri = new int[list.size() * 3];
        for (int i = 0; i < list.size(); i++) {
            int j = (Integer) list.get(i);
            boolean bl2 = j < 0;
            int k = bl2 ? ~j : j;
            for (int m = 0; m < 3; m++) {
                int tri2 = posedMesh.tris[k * 3 + (bl2 ? 2 - m : m)];
                int n = bl2 ? ~tri2 : tri2;
                Integer integer = (Integer) map.get(n);
                if (integer == null && bl) {
                    String string =
                            Float.floatToIntBits(posedMesh.pos[tri2 * 3])
                                    + ","
                                    + Float.floatToIntBits(posedMesh.pos[tri2 * 3 + 1])
                                    + ","
                                    + Float.floatToIntBits(posedMesh.pos[tri2 * 3 + 2])
                                    + ","
                                    + Float.floatToIntBits(posedMesh.uv[tri2 * 2])
                                    + ","
                                    + Float.floatToIntBits(posedMesh.uv[tri2 * 2 + 1])
                                    + ","
                                    + posedMesh.material[tri2]
                                    + (bl2 ? "b" : "");
                    integer = (Integer) map2.get(string);
                    if (integer == null) {
                        map2.put(string, list2.size());
                    }
                    if (integer != null) {
                        map.put((long) n, integer);
                    }
                }
                if (integer == null) {
                    integer = list2.size();
                    map.put((long) n, integer);
                    list2.add(n);
                }
                this.tri[i * 3 + m] = integer;
            }
        }
        int o = list2.size();
        this.corner = new int[o];
        this.pos = new float[o * 3];
        for (int q = 0; q < o; q++) {
            this.corner[q] = (Integer) list2.get(q);
            int p = this.corner[q] < 0 ? ~this.corner[q] : this.corner[q];
            System.arraycopy(posedMesh.pos, p * 3, this.pos, q * 3, 3);
        }
        this.dead = new boolean[list.size()];
        this.live = list.size();
        for (int t = 0; t < o; t++) {
            this.around.add(new ArrayList<>());
        }
        for (int u = 0; u < list.size(); u++) {
            int r = this.tri[u * 3];
            int v = this.tri[u * 3 + 1];
            int w = this.tri[u * 3 + 2];
            if (r != v && v != w && r != w) {
                for (int j2 = 0; j2 < 3; j2++) {
                    this.around.get(this.tri[u * 3 + j2]).add(u);
                }
            } else {
                this.dead[u] = true;
                this.live--;
            }
        }
        this.locked = new boolean[o];
        HashMap map3 = new HashMap();
        for (int k2 = 0; k2 < list.size(); k2++) {
            if (!this.dead[k2]) {
                for (int m2 = 0; m2 < 3; m2++) {
                    map3.merge(
                            key(this.tri[k2 * 3 + m2], this.tri[k2 * 3 + (m2 + 1) % 3]),
                            1,
                            (a, value) -> (Integer) a + (Integer) value);
                }
            }
        }
        for (Entry entry : (Iterable<Entry>) map3.entrySet()) {
            if ((Integer) entry.getValue() != 2) {
                this.locked[(int) (((Long) entry.getKey()) >> 32)] = true;
                this.locked[(int) (((Long) entry.getKey()) & 4294967295L)] = true;
            }
        }
        this.quad = new double[o * 10];
        for (int n2 = 0; n2 < list.size(); n2++) {
            double d;
            int i2;
            int o2;
            int p2;
            float[] floats;
            if (!this.dead[n2]
                    && !((d =
                                    Math.sqrt(
                                            (floats =
                                                                            Mat4.faceNormal(
                                                                                    this.pos,
                                                                                    p2 =
                                                                                            this
                                                                                                    .tri[
                                                                                                    n2
                                                                                                            * 3],
                                                                                    o2 =
                                                                                            this
                                                                                                    .tri[
                                                                                                    n2
                                                                                                                    * 3
                                                                                                            + 1],
                                                                                    i2 =
                                                                                            this
                                                                                                    .tri[
                                                                                                    n2
                                                                                                                    * 3
                                                                                                            + 2]))
                                                                    [0]
                                                            * floats[0]
                                                    + floats[1] * floats[1]
                                                    + floats[2] * floats[2]))
                            < 1.0E-12)) {
                double e = floats[0] / d;
                double d2 = floats[1] / d;
                double e2 = floats[2] / d;
                double d3 =
                        -(e * this.pos[p2 * 3]
                                + d2 * this.pos[p2 * 3 + 1]
                                + e2 * this.pos[p2 * 3 + 2]);
                for (int q2 : new int[] {p2, o2, i2}) {
                    this.addPlane(q2, e, d2, e2, d3, d * 0.5);
                }
            }
        }
        this.version = new int[o];
    }

    private static long key(int i, int j) {
        return (long) Math.min(i, j) << 32 | Math.max(i, j);
    }

    private void addPlane(int i, double d, double e, double d2, double e2, double d3) {
        int j;
        int k = j = i * 10;
        this.quad[k] = this.quad[k] + d3 * d * d;
        int m = j + 1;
        this.quad[m] = this.quad[m] + d3 * d * e;
        int n = j + 2;
        this.quad[n] = this.quad[n] + d3 * d * d2;
        int o = j + 3;
        this.quad[o] = this.quad[o] + d3 * d * e2;
        int p = j + 4;
        this.quad[p] = this.quad[p] + d3 * e * e;
        int q = j + 5;
        this.quad[q] = this.quad[q] + d3 * e * d2;
        int r = j + 6;
        this.quad[r] = this.quad[r] + d3 * e * e2;
        int t = j + 7;
        this.quad[t] = this.quad[t] + d3 * d2 * d2;
        int u = j + 8;
        this.quad[u] = this.quad[u] + d3 * d2 * e2;
        int v = j + 9;
        this.quad[v] = this.quad[v] + d3 * e2 * e2;
    }

    private double error(int i, double d, double e, double d2) {
        double[] quad2 = this.quad;
        int j = i * 10;
        return quad2[j] * d * d
                + 2.0 * quad2[j + 1] * d * e
                + 2.0 * quad2[j + 2] * d * d2
                + 2.0 * quad2[j + 3] * d
                + quad2[j + 4] * e * e
                + 2.0 * quad2[j + 5] * e * d2
                + 2.0 * quad2[j + 6] * e
                + quad2[j + 7] * d2 * d2
                + 2.0 * quad2[j + 8] * d2
                + quad2[j + 9];
    }

    int[][] reduce(int[] ints) {
        PriorityQueue priorityQueue =
                new PriorityQueue<>(
                        (MeshReducer.Move move, MeshReducer.Move move2) ->
                                Double.compare(move.cost, move2.cost));
        for (int i = 0; i < this.locked.length; i++) {
            this.push(priorityQueue, i);
        }
        int[][] intGrid = new int[ints.length][];
        Integer[] integers = new Integer[ints.length];
        for (int j = 0; j < integers.length; j++) {
            integers[j] = j;
        }
        Arrays.sort(
                integers, (integer, integer2) -> Integer.compare(ints[integer2], ints[integer]));
        Integer[] integers2 = integers;
        int k = integers.length;
        for (int m = 0; m < k; m++) {
            int n = integers2[m];
            while (this.live > ints[n] && !priorityQueue.isEmpty()) {
                MeshReducer.Move priority = (MeshReducer.Move) priorityQueue.poll();
                if (this.version[priority.from] == priority.vf
                        && this.version[priority.to] == priority.vt
                        && !this.locked[priority.from]
                        && this.collapse(priority.from, priority.to)) {
                    HashSet set = new HashSet();
                    set.add(priority.to);
                    for (int o : this.around.get(priority.to)) {
                        if (!this.dead[o]) {
                            for (int p = 0; p < 3; p++) {
                                set.add(this.tri[o * 3 + p]);
                            }
                        }
                    }
                    for (int q : (Iterable<Integer>) set) {
                        this.push(priorityQueue, q);
                    }
                }
            }
            intGrid[n] = this.snapshot();
        }
        return intGrid;
    }

    private void push(PriorityQueue<MeshReducer.Move> priorityQueue, int i) {
        if (!this.locked[i]) {
            for (int j : this.neighbours(i)) {
                double d = this.pos[j * 3] - this.pos[i * 3];
                double e = this.pos[j * 3 + 1] - this.pos[i * 3 + 1];
                double d2 = this.pos[j * 3 + 2] - this.pos[i * 3 + 2];
                double e2 =
                        this.error(i, this.pos[j * 3], this.pos[j * 3 + 1], this.pos[j * 3 + 2])
                                + 1.0E-4 * (d * d + e * e + d2 * d2);
                priorityQueue.add(new MeshReducer.Move(e2, i, j, this.version[i], this.version[j]));
            }
        }
    }

    private List<Integer> neighbours(int i) {
        ArrayList list = new ArrayList();
        for (int j : this.around.get(i)) {
            if (!this.dead[j]) {
                for (int k = 0; k < 3; k++) {
                    int m = this.tri[j * 3 + k];
                    if (m != i && !list.contains(m)) {
                        list.add(m);
                    }
                }
            }
        }
        return list;
    }

    private boolean collapse(int i, int j) {
        List list = this.neighbours(i);
        List list2 = this.neighbours(j);
        if (!list.contains(j)) {
            return false;
        } else {
            int k = 0;
            int m = 0;
            for (int n : this.around.get(i)) {
                if (!this.dead[n] && this.has(n, j)) {
                    k++;
                }
            }
            for (int o : (Iterable<Integer>) list) {
                if (list2.contains(o)) {
                    m++;
                }
            }
            if (m != k) {
                return false;
            } else {
                for (int p : this.around.get(i)) {
                    if (!this.dead[p] && !this.has(p, j)) {
                        float[] floats =
                                Mat4.faceNormal(
                                        this.pos,
                                        this.tri[p * 3],
                                        this.tri[p * 3 + 1],
                                        this.tri[p * 3 + 2]);
                        int[] ints =
                                new int[] {
                                    this.tri[p * 3], this.tri[p * 3 + 1], this.tri[p * 3 + 2]
                                };
                        for (int q = 0; q < 3; q++) {
                            if (ints[q] == i) {
                                ints[q] = j;
                            }
                        }
                        float[] floats2 = Mat4.faceNormal(this.pos, ints[0], ints[1], ints[2]);
                        double d =
                                Math.sqrt(
                                        floats[0] * floats[0]
                                                + floats[1] * floats[1]
                                                + floats[2] * floats[2]);
                        double e =
                                Math.sqrt(
                                        floats2[0] * floats2[0]
                                                + floats2[1] * floats2[1]
                                                + floats2[2] * floats2[2]);
                        if (e < 1.0E-12 || d < 1.0E-12) {
                            return false;
                        }
                        if ((floats[0] * floats2[0]
                                                + floats[1] * floats2[1]
                                                + floats[2] * floats2[2])
                                        / (e * d)
                                < 0.5) {
                            return false;
                        }
                    }
                }
                for (int r : this.around.get(i)) {
                    if (!this.dead[r]) {
                        if (this.has(r, j)) {
                            this.dead[r] = true;
                            this.live--;
                        } else {
                            for (int t = 0; t < 3; t++) {
                                if (this.tri[r * 3 + t] == i) {
                                    this.tri[r * 3 + t] = j;
                                }
                            }
                            this.around.get(j).add(r);
                        }
                    }
                }
                this.around.get(i).clear();
                for (int u = 0; u < 10; u++) {
                    int v = j * 10 + u;
                    this.quad[v] = this.quad[v] + this.quad[i * 10 + u];
                }
                this.version[i]++;
                this.version[j]++;
                this.locked[i] = true;
                return true;
            }
        }
    }

    private boolean has(int i, int j) {
        return this.tri[i * 3] == j || this.tri[i * 3 + 1] == j || this.tri[i * 3 + 2] == j;
    }

    private int[] snapshot() {
        int[] ints = new int[this.live * 3];
        int i = 0;
        for (int j = 0; j < this.dead.length; j++) {
            if (!this.dead[j]) {
                for (int k = 0; k < 3; k++) {
                    ints[i++] = this.corner[this.tri[j * 3 + k]];
                }
            }
        }
        return Arrays.copyOf(ints, i);
    }

    @Environment(EnvType.CLIENT)
    private record Move(double cost, int from, int to, int vf, int vt) {}
}
