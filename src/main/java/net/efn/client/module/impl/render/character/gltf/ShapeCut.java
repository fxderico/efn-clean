package net.efn.client.module.impl.render.character.gltf;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
final class ShapeCut {

    private static final int BANDS = 160;

    private static final int BRIDGE = 5;

    private static final int NO_TRUNK = -2;

    private final PosedMesh mesh;

    private final float height;

    private final float step;

    private final float gap;

    private final List<float[]>[] spans;

    private final float[][][] intervals;

    private final int[][] labels;

    private ShapeCut(PosedMesh mesh2) {
        this.mesh = mesh2;
        this.height = mesh2.bounds()[4];
        this.step = this.height / 160.0F;
        this.gap = this.height * 0.004F;
        this.spans = new List[160];
        for (int i = 0; i < 160; i++) {
            this.spans[i] = new ArrayList<>();
        }
        this.intervals = new float[160][][];
        this.labels = new int[160][];
    }

    static CharacterBake.Cut cut(PosedMesh posedMesh) {
        ShapeCut shapeCut = new ShapeCut(posedMesh);
        if (!(shapeCut.height > 0.0F)) {
            return null;
        } else {
            shapeCut.slice();
            return shapeCut.find();
        }
    }

    private void slice() {
        float[] pos = this.mesh.pos;
        for (int i = 0; i < this.mesh.triangles; i++) {
            int tri = this.mesh.tris[i * 3];
            int tri2 = this.mesh.tris[i * 3 + 1];
            int tri3 = this.mesh.tris[i * 3 + 2];
            float f = pos[tri * 3 + 1];
            float g = pos[tri2 * 3 + 1];
            float h = pos[tri3 * 3 + 1];
            float f2 = Math.min(f, Math.min(g, h));
            float g2 = Math.max(f, Math.max(g, h));
            int j = Math.max(0, (int) Math.ceil(f2 / this.step - 0.5F));
            int k = Math.min(159, (int) Math.floor(g2 / this.step - 0.5F));
            for (int p = j; p <= k; p++) {
                float h2 = (p + 0.5F) * this.step;
                float f3 = 3.4028235E38F;
                float g3 = -3.4028235E38F;
                for (int[] ints : new int[][] {{tri, tri2}, {tri2, tri3}, {tri3, tri}}) {
                    float h3 = pos[ints[0] * 3 + 1];
                    float f4 = pos[ints[1] * 3 + 1];
                    if (h3 != f4 && !((h2 - h3) * (h2 - f4) > 0.0F)) {
                        float g4 =
                                pos[ints[0] * 3]
                                        + (pos[ints[1] * 3] - pos[ints[0] * 3])
                                                * (h2 - h3)
                                                / (f4 - h3);
                        f3 = Math.min(f3, g4);
                        g3 = Math.max(g3, g4);
                    }
                }
                if (f3 <= g3) {
                    this.spans[p].add(new float[] {f3, g3});
                }
            }
        }
        for (int q = 0; q < 160; q++) {
            List span = this.spans[q];
            span.sort((fs, gs) -> Float.compare(((float[]) fs)[0], ((float[]) gs)[0]));
            ArrayList list = new ArrayList();
            for (float[] floats : (Iterable<float[]>) span) {
                float[] floats2 = list.isEmpty() ? null : (float[]) list.get(list.size() - 1);
                if (floats2 != null && floats[0] <= floats2[1] + this.gap) {
                    floats2[1] = Math.max(floats2[1], floats[1]);
                } else {
                    list.add(new float[] {floats[0], floats[1]});
                }
            }
            this.intervals[q] = ((float[][]) list.toArray(new float[0][]));
            this.labels[q] = new int[this.intervals[q].length];
            this.spans[q] = null;
        }
    }

    private CharacterBake.Cut find() {
        int crotch = this.crotch();
        int[] ints = new int[320];
        float[] floats = null;
        for (int j = 0; j < 160; j++) {
            int[] ints2 = this.innerIntervals(j, j < crotch);
            if (j >= crotch && ints2[0] >= 0) {
                float[] floats2 = this.intervals[j][ints2[0]];
                boolean bl = floats2[0] <= 0.0F && floats2[1] >= 0.0F;
                boolean bl2 = floats != null && floats2[0] <= floats[1] && floats2[1] >= floats[0];
                if (!bl && !bl2) {
                    ints2 = new int[] {-2, -2};
                } else {
                    floats = floats2;
                }
            }
            ints[j * 2] = ints2[0];
            ints[j * 2 + 1] = ints2[1];
        }
        ShapeCut.Arm[] arms =
                new ShapeCut.Arm[] {this.arm(ints, crotch, 0), this.arm(ints, crotch, 1)};
        int i = Math.max(arms[0] == null ? -1 : arms[0].to, arms[1] == null ? -1 : arms[1].to);
        int neck = this.neck(ints, crotch, i < 0 ? 112 : i);
        boolean bl3 = crotch > 0;
        if (!bl3 && arms[0] == null && arms[1] == null && neck < 0) {
            return null;
        } else {
            for (int n = 0; n < 160; n++) {
                float[][] interval = this.intervals[n];
                for (int o = 0; o < interval.length; o++) {
                    byte b = 1;
                    if (n < crotch && bl3) {
                        if (o == ints[n * 2]) {
                            b = 4;
                        } else if (o == ints[n * 2 + 1]) {
                            b = 5;
                        }
                    }
                    if (neck >= 0 && n > neck && b == 1) {
                        b = 0;
                    }
                    this.labels[n][o] = b;
                }
            }
            int[] ints3 = new int[this.mesh.vertices];
            for (int p = 0; p < this.mesh.vertices; p++) {
                float f = this.mesh.pos[p * 3];
                float g = this.mesh.pos[p * 3 + 1];
                int k = Math.max(0, Math.min(159, (int) (g / this.step)));
                ints3[p] = this.label(f, g);
                for (int r = 0; r < 2; r++) {
                    ShapeCut.Arm arm = arms[r];
                    if (arm != null
                            && k >= arm.from
                            && k <= arm.to
                            && (r == 0 ? f < arm.edge[k] : f > arm.edge[k])) {
                        ints3[p] = r == 0 ? 2 : 3;
                    }
                }
            }
            if (this.share(ints3, 2) + this.share(ints3, 3) > 0.4F) {
                for (int t = 0; t < ints3.length; t++) {
                    if (ints3[t] == 2 || ints3[t] == 3) {
                        ints3[t] = 1;
                    }
                }
                arms = new ShapeCut.Arm[2];
            }
            if (arms[0] != null || arms[1] != null) {
                ints3 = this.mesh.separate(ints3);
            }
            float[][] floatGrid = new float[6][];
            float[][] floatGrid2 = new float[6][];
            float[][] floatGrid3 = new float[6][];
            float[][] floatGrid4 = new float[6][];
            floatGrid[1] = new float[] {0.0F, 24.0F, 0.0F};
            float h = this.height * 0.02F;
            if (bl3) {
                float f2 = crotch * this.step;
                for (int m : new int[] {4, 5}) {
                    floatGrid[m] = this.mean(ints3, m, f2 - this.height * 0.06F, f2);
                    floatGrid[m][1] = f2;
                    floatGrid3[m] = this.mean(ints3, m, f2 * 0.45F, f2 * 0.55F);
                    floatGrid3[m][1] = f2 * 0.5F;
                    floatGrid4[m] = this.below(floatGrid3[m][1], h);
                }
            }
            for (int v = 0; v < 2; v++) {
                ShapeCut.Arm arm2 = arms[v];
                if (arm2 != null) {
                    int q = v == 0 ? 2 : 3;
                    float g2 = (arm2.to + 1) * this.step;
                    float h2 = arm2.from * this.step;
                    float[] floats3 = this.mean(ints3, q, g2 - this.height * 0.05F, g2);
                    float[] floats4 = this.mean(ints3, q, h2, h2 + this.height * 0.05F);
                    floats3[1] = g2;
                    floats4[1] = h2 + this.height * 0.03F;
                    boolean bl4 =
                            this.attached(arm2, v, arm2.from, -1)
                                    && !this.attached(arm2, v, arm2.to, 1);
                    if (bl4) {
                        float[] floats5 = new float[] {floats4[0], h2, floats4[2]};
                        floatGrid[q] = floats5;
                    } else {
                        floatGrid[q] = floats3;
                    }
                    float[] floats6;
                    if (bl4) {
                        float[] floats7 = new float[] {floats3[0], g2 - this.height * 0.03F, 0.0F};
                        floats6 = floats7;
                        floats7[2] = floats3[2];
                    } else {
                        floats6 = floats4;
                    }
                    floatGrid2[q] = floats6;
                    this.lower(ints3, q, floatGrid[q], floatGrid2[q]);
                    float[] floats8 = new float[3];
                    floatGrid3[q] = new float[3];
                    for (int i2 = 0; i2 < 3; i2++) {
                        floatGrid3[q][i2] = (floatGrid[q][i2] + floatGrid2[q][i2]) * 0.5F;
                        floats8[i2] = floatGrid2[q][i2] - floatGrid[q][i2];
                    }
                    floatGrid4[q] = this.beyond(floatGrid3[q], floats8, h);
                }
            }
            if (neck >= 0) {
                float f3 = (neck + 1) * this.step;
                floatGrid[0] = this.mean(ints3, 0, f3, f3 + this.height * 0.04F);
                floatGrid[0][1] = f3;
            }
            return new CharacterBake.Cut(ints3, floatGrid, floatGrid2, floatGrid3, floatGrid4);
        }
    }

    private int crotch() {
        int i = -1;
        int j = 0;
        while (j < 160 && this.intervals[j].length == 0) {
            j++;
        }
        for (int k = j + 19; j < 160; j++) {
            boolean splitAtCentre = this.splitAtCentre(j);
            if (i < 0) {
                if (splitAtCentre) {
                    i = j;
                } else if (j > k) {
                    return 0;
                }
            } else if (!splitAtCentre) {
                return j * this.step >= this.height * 0.1F && j * this.step <= this.height * 0.65F
                        ? j
                        : 0;
            }
        }
        return 0;
    }

    private boolean splitAtCentre(int i) {
        float[][] interval = this.intervals[i];
        if (interval.length < 2) {
            return false;
        } else {
            for (float[] floats : interval) {
                if (floats[0] <= 0.0F && floats[1] >= 0.0F) {
                    return false;
                }
            }
            return interval[0][1] < 0.0F && interval[interval.length - 1][0] > 0.0F;
        }
    }

    private int[] innerIntervals(int i, boolean bl) {
        float[][] interval = this.intervals[i];
        if (interval.length == 0) {
            return new int[] {-1, -1};
        } else if (bl) {
            int j = -1;
            int k = -1;
            for (int m = 0; m < interval.length; m++) {
                if (interval[m][1] < 0.0F) {
                    j = m;
                } else if (k < 0) {
                    k = m;
                }
            }
            return new int[] {j, k};
        } else {
            int n = 0;
            float f = 3.4028235E38F;
            for (int o = 0; o < interval.length; o++) {
                float g =
                        interval[o][0] > 0.0F
                                ? interval[o][0]
                                : (interval[o][1] < 0.0F ? -interval[o][1] : 0.0F);
                if (g < f) {
                    f = g;
                    n = o;
                }
            }
            return new int[] {n, n};
        }
    }

    private ShapeCut.Arm arm(int[] ints, int i, int j) {
        float[] floats = new float[160];
        Arrays.fill(floats, 0.0F / 0.0F);
        for (int k = 0; k < 160; k++) {
            int m = ints[k * 2 + j];
            float[][] interval = this.intervals[k];
            if (m == -2
                    && interval.length > 0
                    && (j == 0 ? interval[0][0] < 0.0F : interval[interval.length - 1][1] > 0.0F)) {
                floats[k] = 0.0F;
            }
            if (m >= 0) {
                if (j == 0 && m > 0) {
                    floats[k] = (interval[m - 1][1] + interval[m][0]) * 0.5F;
                }
                if (j == 1 && m < interval.length - 1) {
                    floats[k] = (interval[m][1] + interval[m + 1][0]) * 0.5F;
                }
            }
        }
        int n = -1;
        int o = -1;
        int p = -1;
        int q = -1;
        for (int r = 0; r <= 160; r++) {
            boolean bl = r < 160 && !Float.isNaN(floats[r]);
            if (bl) {
                if (p >= 0 && r - q - 1 > 5) {
                    if (q - p > o - n) {
                        n = p;
                        o = q;
                    }
                    p = -1;
                }
                if (p < 0) {
                    p = r;
                }
                q = r;
            }
        }
        if (p >= 0 && q - p > o - n) {
            n = p;
            o = q;
        }
        if (n >= 0 && !(o - n + 1 < 12.799999F) && o > i) {
            for (int t = n + 1; t < o; t++) {
                if (Float.isNaN(floats[t])) {
                    int u = t - 1;
                    int v = t + 1;
                    while (Float.isNaN(floats[v])) {
                        v++;
                    }
                    floats[t] =
                            j == 0
                                    ? Math.min(floats[u], floats[v])
                                    : Math.max(floats[u], floats[v]);
                }
            }
            return new ShapeCut.Arm(n, o, floats);
        } else {
            return null;
        }
    }

    private boolean attached(ShapeCut.Arm arm, int i, int j, int k) {
        float f = arm.edge[j];
        boolean bl = i == 0;
        for (int m = 1; m <= 2; m++) {
            int n = j + k * m;
            if (n < 0 || n >= 160) {
                return false;
            }
            for (float[] floats : this.intervals[n]) {
                if (bl ? floats[0] < f : floats[1] > f) {
                    return true;
                }
            }
        }
        return false;
    }

    private void lower(int[] ints, int i, float[] floats, float[] floats2) {
        float f = floats2[0] - floats[0];
        float g = floats2[1] - floats[1];
        float h = floats2[2] - floats[2];
        float f2 = (float) Math.sqrt(f * f + g * g + h * h);
        if (!(f2 < 1.0E-4F)) {
            float g2 = (float) Math.acos(Math.max(-1.0F, Math.min(1.0F, -g / f2)));
            if (!(g2 < Math.toRadians(40.0))) {
                float h2 = h;
                float f3 = -f;
                float g3 = (float) Math.sqrt(h * h + f3 * f3);
                if (g3 < 1.0E-5F) {
                    h2 = 0.0F;
                    f3 = 1.0F;
                    g3 = 1.0F;
                }
                h2 /= g3;
                f3 /= g3;
                float h3 = g2 - (float) Math.toRadians(10.0);
                float f4 = (float) Math.cos(h3);
                float g4 = (float) Math.sin(h3);
                for (int j = 0; j < ints.length; j++) {
                    if (ints[j] == i) {
                        rotate(this.mesh.pos, j * 3, floats, h2, f3, f4, g4);
                        rotate(this.mesh.nrm, j * 3, null, h2, f3, f4, g4);
                    }
                }
                float[] floats3 = (float[]) floats2.clone();
                rotate(floats3, 0, floats, h2, f3, f4, g4);
                System.arraycopy(floats3, 0, floats2, 0, 3);
            }
        }
    }

    private static void rotate(
            float[] floats, int i, float[] floats2, float f, float g, float h, float f2) {
        float g2 = floats[i] - (floats2 == null ? 0.0F : floats2[0]);
        float h2 = floats[i + 1] - (floats2 == null ? 0.0F : floats2[1]);
        float f3 = floats[i + 2] - (floats2 == null ? 0.0F : floats2[2]);
        float g3 = f * g2 + g * f3;
        float h3 = -g * h2;
        float f4 = g * g2 - f * f3;
        float g4 = f * h2;
        float h4 = g2 * h + h3 * f2 + f * g3 * (1.0F - h);
        float f5 = h2 * h + f4 * f2;
        float g5 = f3 * h + g4 * f2 + g * g3 * (1.0F - h);
        floats[i] = h4 + (floats2 == null ? 0.0F : floats2[0]);
        floats[i + 1] = f5 + (floats2 == null ? 0.0F : floats2[1]);
        floats[i + 2] = g5 + (floats2 == null ? 0.0F : floats2[2]);
    }

    private int neck(int[] ints, int i, int j) {
        short s = 154;
        int k = Math.max(j, i + 1);
        int m = -1;
        float f = 3.4028235E38F;
        for (int n = k; n < s; n++) {
            float torsoWidth = this.torsoWidth(ints, n);
            if (torsoWidth > 0.0F && torsoWidth < f) {
                f = torsoWidth;
                m = n;
            }
        }
        if (m < 0) {
            return -1;
        } else {
            float g = 0.0F;
            float torsoWidth2 = this.torsoWidth(ints, k);
            for (int o = m + 1; o < 160; o++) {
                g = Math.max(g, this.torsoWidth(ints, o));
            }
            float h = (160 - m) * this.step;
            boolean bl = f < g * 0.75F && f < torsoWidth2 * 0.8F;
            return bl && h > this.height * 0.08F && h < this.height * 0.45F ? m : -1;
        }
    }

    private float torsoWidth(int[] ints, int i) {
        int j = ints[i * 2];
        return j < 0 ? 0.0F : this.intervals[i][j][1] - this.intervals[i][j][0];
    }

    private int label(float f, float g) {
        int i = Math.max(0, Math.min(159, (int) (g / this.step)));
        for (int j = 0; j < 160; j++) {
            for (int k : new int[] {i - j, i + j}) {
                if (k >= 0 && k < 160 && this.intervals[k].length != 0) {
                    float[][] interval = this.intervals[k];
                    int m = 0;
                    float h = 3.4028235E38F;
                    for (int n = 0; n < interval.length; n++) {
                        float f2 =
                                f < interval[n][0]
                                        ? interval[n][0] - f
                                        : (f > interval[n][1] ? f - interval[n][1] : 0.0F);
                        if (f2 < h) {
                            h = f2;
                            m = n;
                        }
                    }
                    return this.labels[k][m];
                }
            }
        }
        return 1;
    }

    private float share(int[] ints, int i) {
        int j = 0;
        for (int k : ints) {
            if (k == i) {
                j++;
            }
        }
        return (float) j / Math.max(1, ints.length);
    }

    private float[] mean(int[] ints, int i, float f, float g) {
        double d = 0.0;
        double e = 0.0;
        double d2 = 0.0;
        int j = 0;
        for (int k = 0; k < 2 && j == 0; k++) {
            for (int m = 0; m < ints.length; m++) {
                float h = this.mesh.pos[m * 3 + 1];
                if (ints[m] == i && (k != 0 || !(h < f) && !(h > g))) {
                    d += this.mesh.pos[m * 3];
                    e += h;
                    d2 += this.mesh.pos[m * 3 + 2];
                    j++;
                }
            }
        }
        float[] floats;
        if (j == 0) {
            floats = new float[3];
        } else {
            float[] floats2 = new float[] {(float) (d / j), (float) (e / j), 0.0F};
            floats = floats2;
            floats2[2] = (float) (d2 / j);
        }
        return floats;
    }

    private float[] beyond(float[] floats, float[] floats2, float f) {
        float g =
                (float)
                        Math.sqrt(
                                floats2[0] * floats2[0]
                                        + floats2[1] * floats2[1]
                                        + floats2[2] * floats2[2]);
        float[] floats3 = new float[this.mesh.vertices];
        if (g < 1.0E-5F) {
            return floats3;
        } else {
            for (int i = 0; i < floats3.length; i++) {
                float h =
                        ((this.mesh.pos[i * 3] - floats[0]) * floats2[0]
                                        + (this.mesh.pos[i * 3 + 1] - floats[1]) * floats2[1]
                                        + (this.mesh.pos[i * 3 + 2] - floats[2]) * floats2[2])
                                / g;
                float f2 = Math.max(0.0F, Math.min(1.0F, (h + f) / (2.0F * f)));
                floats3[i] = f2 * f2 * (3.0F - 2.0F * f2);
            }
            return floats3;
        }
    }

    private float[] below(float f, float g) {
        float[] floats = new float[this.mesh.vertices];
        for (int i = 0; i < floats.length; i++) {
            float h =
                    Math.max(0.0F, Math.min(1.0F, (f + g - this.mesh.pos[i * 3 + 1]) / (2.0F * g)));
            floats[i] = h * h * (3.0F - 2.0F * h);
        }
        return floats;
    }

    @Environment(EnvType.CLIENT)
    private record Arm(int from, int to, float[] edge) {}
}
