package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
final class Simplifier {

    private static final double FLIP_LIMIT = 0.25;

    private static final int DEBRIS = 10;

    private final int nv;

    private final double[] pos;

    private final int[] tri;

    private final boolean[] triDead;

    private final int[][] around;

    private final int[] aroundCount;

    private final double[] quad;

    private final int[] version;

    private final boolean[] vertDead;

    private final int[] mark;

    private final int[] mark2;

    private int stamp;

    private int live;

    private boolean relaxed;

    private final double lengthWeight;

    private float[] heapCost = new float[1024];

    private int[] heapA = new int[1024];

    private int[] heapB = new int[1024];

    private int[] heapVa = new int[1024];

    private int[] heapVb = new int[1024];

    private int heapSize;

    private final double[] best = new double[3];

    Simplifier(Mesh mesh) {
        this.nv = mesh.vertexCount;
        int triCount = mesh.triCount;
        this.pos = new double[this.nv * 3];
        for (int j = 0; j < this.nv * 3; j++) {
            this.pos[j] = mesh.pos[j];
        }
        this.tri = Arrays.copyOf(mesh.tris, triCount * 3);
        this.triDead = new boolean[triCount];
        this.aroundCount = new int[this.nv];
        for (int k = 0; k < triCount * 3; k++) {
            int i = this.tri[k];
            this.aroundCount[i]++;
        }
        this.around = new int[this.nv][];
        for (int n = 0; n < this.nv; n++) {
            this.around[n] = new int[Math.max(4, this.aroundCount[n] + 2)];
        }
        Arrays.fill(this.aroundCount, 0);
        for (int o = 0; o < triCount; o++) {
            for (int p = 0; p < 3; p++) {
                int m;
                int q = m = this.tri[o * 3 + p];
                int r = this.aroundCount[q]++;
                this.around[m][r] = o;
            }
        }
        this.quad = new double[this.nv * 10];
        this.version = new int[this.nv];
        this.vertDead = new boolean[this.nv];
        this.mark = new int[this.nv];
        this.mark2 = new int[this.nv];
        this.live = triCount;
        double d = 0.0;
        for (int u = 0; u < triCount; u++) {
            int t = this.tri[u * 3 + 1];
            int v = this.tri[u * 3];
            double e = this.pos[t * 3 + 1] - this.pos[v * 3 + 1];
            int w = this.tri[u * 3 + 2];
            double d2 = this.pos[w * 3 + 2] - this.pos[v * 3 + 2];
            double e2 = this.pos[t * 3 + 2] - this.pos[v * 3 + 2];
            double d3 = this.pos[w * 3 + 1] - this.pos[v * 3 + 1];
            double e3 = e * d2 - e2 * d3;
            double d4 = this.pos[w * 3] - this.pos[v * 3];
            double e4 = this.pos[t * 3] - this.pos[v * 3];
            double d5 = e2 * d4 - e4 * d2;
            double e5 = e4 * d3 - e * d4;
            double d6 = Math.sqrt(e3 * e3 + d5 * d5 + e5 * e5);
            if (!(d6 < 1.0E-12)) {
                double e6 = d6 * 0.5;
                d += e6;
                double d7;
                double e7;
                double d8;
                double e8 =
                        -((d7 = e3 / d6) * this.pos[v * 3]
                                + (e7 = d5 / d6) * this.pos[v * 3 + 1]
                                + (d8 = e5 / d6) * this.pos[v * 3 + 2]);
                this.addPlane(v, d7, e7, d8, e8, e6);
                this.addPlane(t, d7, e7, d8, e8, e6);
                this.addPlane(w, d7, e7, d8, e8, e6);
            }
        }
        this.lengthWeight = triCount > 0 ? 0.04 * d / triCount : 0.0;
        this.dropDebris();
        this.pushAll();
    }

    private void pushAll() {
        this.heapSize = 0;
        for (int i = 0; i < this.triDead.length; i++) {
            if (!this.triDead[i]) {
                for (int j = 0; j < 3; j++) {
                    int k = this.tri[i * 3 + j];
                    int m = this.tri[i * 3 + (j + 1) % 3];
                    if (k < m || !this.hasEdgeTriangleReverse(i, k, m)) {
                        this.push(k, m);
                    }
                }
            }
        }
    }

    private void dropDebris() {
        int i = this.triDead.length;
        int[] ints = new int[this.nv];
        int j = 0;
        while (j < this.nv) {
            ints[j] = j++;
        }
        for (int k = 0; k < i; k++) {
            int find = find(ints, this.tri[k * 3]);
            for (int n = 1; n < 3; n++) {
                int find2 = find(ints, this.tri[k * 3 + n]);
                if (find != find2) {
                    ints[find2] = find;
                }
            }
        }
        int[] ints2 = new int[this.nv];
        for (int p = 0; p < i; p++) {
            int find3 = find(ints, this.tri[p * 3]);
            ints2[find3]++;
        }
        for (int r = 0; r < i; r++) {
            if (ints2[find(ints, this.tri[r * 3])] < 10) {
                this.triDead[r] = true;
                this.live--;
            }
        }
    }

    private static int find(int[] ints, int i) {
        while (ints[i] != i) {
            ints[i] = ints[ints[i]];
            i = ints[i];
        }
        return i;
    }

    private boolean hasEdgeTriangleReverse(int i, int j, int k) {
        int[] ints = this.around[j];
        for (int m = 0; m < this.aroundCount[j]; m++) {
            int n = ints[m];
            if (n != i) {
                for (int o = 0; o < 3; o++) {
                    if (this.tri[n * 3 + o] == k && this.tri[n * 3 + (o + 1) % 3] == j) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    void reduce(int i) {
        this.drain(i);
        if (this.live > i && !this.relaxed) {
            this.relaxed = true;
            this.pushAll();
            this.drain(i);
        }
    }

    private void drain(int i) {
        while (this.live > i && this.heapSize > 0) {
            int j = this.heapA[0];
            int k = this.heapB[0];
            int m = this.heapVa[0];
            int n = this.heapVb[0];
            this.pop();
            if (!this.vertDead[j]
                    && !this.vertDead[k]
                    && this.version[j] == m
                    && this.version[k] == n) {
                this.cost(j, k);
                double d = this.best[0];
                double e = this.best[1];
                double d2 = this.best[2];
                if (this.valid(j, k, d, e, d2)) {
                    this.collapse(j, k, d, e, d2);
                }
            }
        }
    }

    Mesh snapshot() {
        int[] ints = new int[this.nv];
        Arrays.fill(ints, -1);
        int i = 0;
        int[] ints2 = new int[this.live * 3];
        int j = 0;
        for (int k = 0; k < this.triDead.length; k++) {
            if (!this.triDead[k]) {
                for (int m = 0; m < 3; m++) {
                    int n = this.tri[k * 3 + m];
                    if (ints[n] < 0) {
                        ints[n] = i++;
                    }
                    ints2[j * 3 + m] = ints[n];
                }
                j++;
            }
        }
        float[] floats = new float[i * 3];
        for (int o = 0; o < this.nv; o++) {
            int p = ints[o];
            if (p >= 0) {
                floats[p * 3] = (float) this.pos[o * 3];
                floats[p * 3 + 1] = (float) this.pos[o * 3 + 1];
                floats[p * 3 + 2] = (float) this.pos[o * 3 + 2];
            }
        }
        return new Mesh(floats, i, ints2, j);
    }

    private float cost(int i, int j) {
        double[] quad2 = this.quad;
        int k = i * 10;
        int m = j * 10;
        double d = quad2[k] + quad2[m];
        double e = quad2[k + 1] + quad2[m + 1];
        double d2 = quad2[k + 2] + quad2[m + 2];
        double e2 = quad2[k + 3] + quad2[m + 3];
        double d3 = quad2[k + 4] + quad2[m + 4];
        double e3 = quad2[k + 5] + quad2[m + 5];
        double d4 = quad2[k + 6] + quad2[m + 6];
        double e4 = quad2[k + 7] + quad2[m + 7];
        double d5 = quad2[k + 8] + quad2[m + 8];
        double e5 = quad2[k + 9] + quad2[m + 9];
        double d6 = this.pos[i * 3];
        double e6 = this.pos[i * 3 + 1];
        double d7 = this.pos[i * 3 + 2];
        double e7 = this.pos[j * 3];
        double d8 = this.pos[j * 3 + 1];
        double e8 = this.pos[j * 3 + 2];
        double d9 = e7 - d6;
        double e9 = d8 - e6;
        double d10 = e8 - d7;
        double e10 = d9 * d9 + e9 * e9 + d10 * d10;
        double d11 = d * (d3 * e4 - e3 * e3) - e * (e * e4 - e3 * d2) + d2 * (e * e3 - d3 * d2);
        double e11 = Math.abs(d * d3 * e4) + 1.0E-30;
        double d12 = 1.7976931348623157E308;
        double e12;
        double d13;
        double e13;
        double d14;
        double e14;
        double d15;
        double e15;
        if (Math.abs(d11) > 1.0E-9 * e11
                && (e15 =
                                                (d6 + e7) * 0.5
                                                        - (d15 =
                                                                -(e14 = 1.0 / d11)
                                                                        * (e2 * (d3 * e4 - e3 * e3)
                                                                                - e
                                                                                        * (d4 * e4
                                                                                                - e3
                                                                                                        * d5)
                                                                                + d2
                                                                                        * (d4 * e3
                                                                                                - d3
                                                                                                        * d5))))
                                        * e15
                                + (d14 =
                                                (e6 + d8) * 0.5
                                                        - (e13 =
                                                                -e14
                                                                        * (d * (d4 * e4 - d5 * e3)
                                                                                - e2
                                                                                        * (e * e4
                                                                                                - e3
                                                                                                        * d2)
                                                                                + d2
                                                                                        * (e * d5
                                                                                                - d4
                                                                                                        * d2))))
                                        * d14
                                + (d13 =
                                                (d7 + e8) * 0.5
                                                        - (e12 =
                                                                -e14
                                                                        * (d * (d3 * d5 - e3 * d4)
                                                                                - e
                                                                                        * (e * d5
                                                                                                - d4
                                                                                                        * d2)
                                                                                + e2
                                                                                        * (e * e3
                                                                                                - d3
                                                                                                        * d2))))
                                        * d13
                        <= e10) {
            d12 = error(d, e, d2, e2, d3, e3, d4, e4, d5, e5, d15, e13, e12);
            this.best[0] = d15;
            this.best[1] = e13;
            this.best[2] = e12;
        }
        if (d12 == 1.7976931348623157E308) {
            for (int n = 0; n < 3; n++) {
                double d16 = n * 0.5;
                double e16 = d6 + d9 * d16;
                double d17 = e6 + e9 * d16;
                double e17 = d7 + d10 * d16;
                double error = error(d, e, d2, e2, d3, e3, d4, e4, d5, e5, e16, d17, e17);
                if (error < d12) {
                    d12 = error;
                    this.best[0] = e16;
                    this.best[1] = d17;
                    this.best[2] = e17;
                }
            }
        }
        return (float) (Math.max(0.0, d12) + this.lengthWeight * e10);
    }

    private static double error(
            double d,
            double e,
            double d2,
            double e2,
            double d3,
            double e3,
            double d4,
            double e4,
            double d5,
            double e5,
            double d6,
            double e6,
            double d7) {
        return d * d6 * d6
                + 2.0 * e * d6 * e6
                + 2.0 * d2 * d6 * d7
                + 2.0 * e2 * d6
                + d3 * e6 * e6
                + 2.0 * e3 * e6 * d7
                + 2.0 * d4 * e6
                + e4 * d7 * d7
                + 2.0 * d5 * d7
                + e5;
    }

    private boolean valid(int i, int j, double d, double e, double d2) {
        int k = ++this.stamp;
        int m = 0;
        int[] ints = this.around[i];
        for (int n = 0; n < this.aroundCount[i]; n++) {
            int o = ints[n];
            if (!this.triDead[o]) {
                boolean bl = false;
                for (int p = 0; p < 3; p++) {
                    int q = this.tri[o * 3 + p];
                    if (q == j) {
                        bl = true;
                    }
                    if (q != i) {
                        this.mark[q] = k;
                    }
                }
                if (bl) {
                    m++;
                }
            }
        }
        if (m != 0 && m <= 2) {
            int r = 0;
            int[] ints2 = this.around[j];
            for (int t = 0; t < this.aroundCount[j]; t++) {
                int u = ints2[t];
                if (!this.triDead[u]) {
                    for (int v = 0; v < 3; v++) {
                        int w = this.tri[u * 3 + v];
                        if (w != j && w != i && this.mark[w] == k && this.mark2[w] != k) {
                            this.mark2[w] = k;
                            r++;
                        }
                    }
                }
            }
            return r != m
                    ? false
                    : this.keepsOrientation(i, j, d, e, d2)
                            && this.keepsOrientation(j, i, d, e, d2);
        } else {
            return false;
        }
    }

    private boolean keepsOrientation(int i, int j, double d, double e, double d2) {
        int[] ints = this.around[i];
        for (int k = 0; k < this.aroundCount[i]; k++) {
            int m = ints[k];
            if (!this.triDead[m]) {
                int n = this.tri[m * 3];
                int o = this.tri[m * 3 + 1];
                int p = this.tri[m * 3 + 2];
                if (n != j && o != j && p != j) {
                    double e2 = this.pos[n * 3];
                    double d3 = this.pos[n * 3 + 1];
                    double e3 = this.pos[n * 3 + 2];
                    double d4 = this.pos[o * 3];
                    double e4 = this.pos[o * 3 + 1];
                    double d5 = this.pos[o * 3 + 2];
                    double e5 = this.pos[p * 3];
                    double d6 = this.pos[p * 3 + 1];
                    double e6 = this.pos[p * 3 + 2];
                    double d7 = (e4 - d3) * (e6 - e3) - (d5 - e3) * (d6 - d3);
                    double e7 = (d5 - e3) * (e5 - e2) - (d4 - e2) * (e6 - e3);
                    double d8 = (d4 - e2) * (d6 - d3) - (e4 - d3) * (e5 - e2);
                    if (n == i) {
                        e2 = d;
                        d3 = e;
                        e3 = d2;
                    } else if (o == i) {
                        d4 = d;
                        e4 = e;
                        d5 = d2;
                    } else {
                        e5 = d;
                        d6 = e;
                        e6 = d2;
                    }
                    double e8 = d4 - e2;
                    double d9 = e4 - d3;
                    double e9 = d5 - e3;
                    double d10 = e5 - e2;
                    double e10 = d6 - d3;
                    double d11 = e6 - e3;
                    double e11 = d9 * d11 - e9 * e10;
                    double d12 = e9 * d10 - e8 * d11;
                    double e12 = e8 * e10 - d9 * d10;
                    double d13 = Math.sqrt(d7 * d7 + e7 * e7 + d8 * d8);
                    double e13 = Math.sqrt(e11 * e11 + d12 * d12 + e12 * e12);
                    if (e13 < 1.0E-14) {
                        return false;
                    }
                    double d14 = this.relaxed ? 0.0 : 0.25;
                    if (d13 > 1.0E-14 && d7 * e11 + e7 * d12 + d8 * e12 < d14 * d13 * e13) {
                        return false;
                    }
                    double e14;
                    double d15;
                    double e15;
                    if (!this.relaxed
                            && e13 * 3.4641016
                                    < 0.06
                                            * (e8 * e8
                                                    + d9 * d9
                                                    + e9 * e9
                                                    + d10 * d10
                                                    + e10 * e10
                                                    + d11 * d11
                                                    + (e15 = e5 - d4) * e15
                                                    + (d15 = d6 - e4) * d15
                                                    + (e14 = e6 - d5) * e14)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void collapse(int i, int j, double d, double e, double d2) {
        this.pos[j * 3] = d;
        this.pos[j * 3 + 1] = e;
        this.pos[j * 3 + 2] = d2;
        for (int k = 0; k < 10; k++) {
            int m = j * 10 + k;
            this.quad[m] = this.quad[m] + this.quad[i * 10 + k];
        }
        int[] ints = this.around[i];
        for (int n = 0; n < this.aroundCount[i]; n++) {
            int o = ints[n];
            if (!this.triDead[o]) {
                int p = o * 3;
                if (this.tri[p] != j && this.tri[p + 1] != j && this.tri[p + 2] != j) {
                    for (int q = 0; q < 3; q++) {
                        if (this.tri[p + q] == i) {
                            this.tri[p + q] = j;
                        }
                    }
                    this.append(j, o);
                } else {
                    this.triDead[o] = true;
                    this.live--;
                }
            }
        }
        this.vertDead[i] = true;
        this.aroundCount[i] = 0;
        this.version[i]++;
        this.version[j]++;
        this.compact(j);
        int r = ++this.stamp;
        int[] ints2 = this.around[j];
        for (int t = 0; t < this.aroundCount[j]; t++) {
            int u = ints2[t];
            for (int v = 0; v < 3; v++) {
                int w = this.tri[u * 3 + v];
                if (w != j && this.mark[w] != r) {
                    this.mark[w] = r;
                    this.push(j, w);
                }
            }
        }
    }

    private void append(int i, int j) {
        int[] ints = this.around[i];
        if (this.aroundCount[i] == ints.length) {
            ints = Arrays.copyOf(ints, ints.length * 2);
            this.around[i] = ints;
        }
        int k = this.aroundCount[i]++;
        ints[k] = j;
    }

    private void compact(int i) {
        int[] ints = this.around[i];
        int j = 0;
        for (int k = 0; k < this.aroundCount[i]; k++) {
            if (!this.triDead[ints[k]]) {
                ints[j++] = ints[k];
            }
        }
        this.aroundCount[i] = j;
    }

    private void push(int i, int j) {
        float cost = this.cost(i, j);
        if (this.heapSize == this.heapCost.length) {
            int k = this.heapSize * 2;
            this.heapCost = Arrays.copyOf(this.heapCost, k);
            this.heapA = Arrays.copyOf(this.heapA, k);
            this.heapB = Arrays.copyOf(this.heapB, k);
            this.heapVa = Arrays.copyOf(this.heapVa, k);
            this.heapVb = Arrays.copyOf(this.heapVb, k);
        }
        int m = this.heapSize++;
        int n;
        while (m > 0 && !(this.heapCost[n = m - 1 >> 1] <= cost)) {
            this.move(n, m);
            m = n;
        }
        this.heapCost[m] = cost;
        this.heapA[m] = i;
        this.heapB[m] = j;
        this.heapVa[m] = this.version[i];
        this.heapVb[m] = this.version[j];
    }

    private void pop() {
        int i;
        if ((i = --this.heapSize) != 0) {
            float f = this.heapCost[i];
            int j = this.heapA[i];
            int k = this.heapB[i];
            int m = this.heapVa[i];
            int n = this.heapVb[i];
            int o = 0;
            int p;
            while ((p = o * 2 + 1) < i) {
                int q = p + 1;
                int r = q < i && this.heapCost[q] < this.heapCost[p] ? q : p;
                if (this.heapCost[r] >= f) {
                    break;
                }
                this.move(r, o);
                o = r;
            }
            this.heapCost[o] = f;
            this.heapA[o] = j;
            this.heapB[o] = k;
            this.heapVa[o] = m;
            this.heapVb[o] = n;
        }
    }

    private void move(int i, int j) {
        this.heapCost[j] = this.heapCost[i];
        this.heapA[j] = this.heapA[i];
        this.heapB[j] = this.heapB[i];
        this.heapVa[j] = this.heapVa[i];
        this.heapVb[j] = this.heapVb[i];
    }
}
