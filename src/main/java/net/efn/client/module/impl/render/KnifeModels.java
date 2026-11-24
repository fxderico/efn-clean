package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class KnifeModels {

    static final int KARAMBIT_DETAIL = 2;

    static final float KARAMBIT_RING_X = -16.0F;

    static final float KARAMBIT_RING_Y = -1.0F;

    static final float KARAMBIT_BLADE_LENGTH = 21.0F;

    static final float[] KARAMBIT_TIP = karambitTip();

    private static final float KATANA_BOW = 0.0016F;

    static final float KATANA_BOW_FROM = 3.0F;

    static final float KATANA_BOW_RADIUS = 312.5F;

    private KnifeModels() {}

    private static float grain(int i, int j, int k) {
        int m = i * 73856093 ^ j * 19349663 ^ k * 83492791;
        m ^= m >>> 13;
        m *= 1540483477;
        m ^= m >>> 15;
        return 0.92F + 0.08F * ((m & 1023) / 1023.0F);
    }

    static KnifeMesh butterflyBlade() {
        KnifeMesh.Voxels voxels = new KnifeMesh.Voxels();
        for (int i = -3; i <= 21; i++) {
            int j = i <= 12 ? 2 : Math.round(2.0F - (i - 12) * 3.0F / 9.0F);
            int k;
            for (int m =
                            k =
                                    i <= 14
                                            ? -2
                                            : Math.round(
                                                    -2.0F
                                                            + (float) Math.pow((i - 14) / 7.0F, 2.0)
                                                                    * 1.0F);
                    m <= j;
                    m++) {
                int n = i < 0 ? 5 : (i <= 1 ? 1 : (m == k ? 2 : (m == j ? (i >= 13 ? 2 : 1) : 0)));
                voxels.set(i, m, 0, n, grain(i, m, 0));
            }
        }
        return voxels.mesh(0.0F, 22.0F);
    }

    static KnifeMesh butterflyHandle(int i) {
        KnifeMesh.Voxels voxels = new KnifeMesh.Voxels();
        int j = i > 0 ? 1 : -1;
        int k = i > 0 ? 2 : -2;
        int m = i > 0 ? 3 : -3;
        for (int n = -27; n <= 0; n++) {
            for (int o = -2; o <= 2; o++) {
                boolean bl = (n == 0 || n == -27) && Math.abs(o) == 2;
                boolean bl2 =
                        Math.abs(o) <= 1
                                && (n <= -6 && n >= -8
                                        || n <= -12 && n >= -14
                                        || n <= -18 && n >= -20);
                if (!bl && !bl2) {
                    voxels.set(n, o, j, 4, grain(n, o, j));
                    byte b = 3;
                    if ((n == -10 || n == -16) && Math.abs(o) <= 1) {
                        b = 6;
                    }
                    if (o == 0 && (n == -2 || n == -25)) {
                        b = 5;
                    }
                    voxels.set(n, o, k, b, grain(n, o, k));
                }
            }
        }
        voxels.set(-2, 0, m, 5, 1.0F);
        voxels.set(-25, 0, m, 5, 1.0F);
        if (i < 0) {
            voxels.set(-28, 1, j, 5, 1.0F);
            voxels.set(-28, 2, j, 5, 1.0F);
            voxels.set(-29, 2, j, 5, 1.0F);
        }
        return voxels.mesh(-29.0F, 0.0F);
    }

    private static float[] karambitCurve(float f) {
        float g = 5.5F;
        float h = 0.5F;
        float f2 = 0.05F;
        for (float g2 = 0.0F; g2 < f; g2 += f2) {
            float h2 = -(0.04F * g2 + 0.0026F * g2 * g2);
            g += (float) Math.cos(h2) * f2;
            h += (float) Math.sin(h2) * f2;
        }
        float f3 = -(0.04F * f + 0.0026F * f * f);
        return new float[] {g, h, (float) Math.cos(f3), (float) Math.sin(f3)};
    }

    private static float[] karambitTip() {
        float[] floats = karambitCurve(21.0F);
        return new float[] {floats[0] * 2.0F, floats[1] * 2.0F};
    }

    static KnifeMesh karambit() {
        KnifeMesh.Voxels voxels = new KnifeMesh.Voxels();
        byte b = 2;
        float f = -16.0F / b;
        float g = -1.0F / b;
        for (int i = Math.round((f + 2.4F) * b); i < 6 * b; i++) {
            float h = (i + 0.5F) / b;
            float f2 = 0.5F - (float) Math.pow((5.0F - h) / 11.0F, 2.0);
            for (int j = -6 * b; j <= 4 * b; j++) {
                float g2 = (j + 0.5F) / b - f2;
                if (!(Math.abs(g2) > 2.45F)) {
                    voxels.set(i, j, 0, 5, grain(i, j, 0));
                    boolean bl =
                            Math.hypot(h + 3.0F, g2) < 0.699999988079071
                                    || Math.hypot(h - 2.5F, g2) < 0.699999988079071;
                    boolean bl2 = Math.abs(g2) < 0.5F && h > -1.5F && h < 1.5F;
                    boolean bl3 = Math.abs(g2) > 1.95F;
                    for (int k : new int[] {-1, 1}) {
                        int m = bl ? 5 : (bl2 ? 6 : (bl3 ? 4 : 3));
                        voxels.set(i, j, k, m, grain(i, j, k));
                    }
                    if (bl) {
                        voxels.set(i, j, -2, 5, 1.0F);
                        voxels.set(i, j, 2, 5, 1.0F);
                    }
                }
            }
        }
        for (int n = Math.round((f - 3.4F) * b); n <= Math.round((f + 3.4F) * b); n++) {
            for (int o = Math.round((g - 3.4F) * b); o <= Math.round((g + 3.4F) * b); o++) {
                float h2 = (n + 0.5F) / b - f;
                float f3 = (o + 0.5F) / b - g;
                float g3 = (float) Math.sqrt(h2 * h2 + f3 * f3);
                if (!(g3 < 2.0F) && !(g3 > 3.0F) && !voxels.filled(n, o, 0)) {
                    float h3 = !(g3 > 2.75F) && !(g3 < 2.25F) ? 1.0F : 0.86F;
                    for (int p = -1; p <= 1; p++) {
                        voxels.set(n, o, p, 5, h3 * grain(n, o, p));
                    }
                }
            }
        }
        short s = 420;
        float[][] floatGrid = new float[s + 1][];
        for (int q = 0; q <= s; q++) {
            floatGrid[q] = karambitCurve(21.0F * q / s);
        }
        for (int r = 5 * b; r <= 31 * b; r++) {
            for (int t = -24 * b; t <= 6 * b; t++) {
                float f4 = (r + 0.5F) / b;
                float g4 = (t + 0.5F) / b;
                int u = 0;
                float h4 = 3.4028235E38F;
                for (int v = 0; v <= s; v++) {
                    float f5 = f4 - floatGrid[v][0];
                    float g5 = g4 - floatGrid[v][1];
                    float h5 = f5 * f5 + g5 * g5;
                    if (h5 < h4) {
                        h4 = h5;
                        u = v;
                    }
                }
                float[] floats = floatGrid[u];
                float f6 = f4 - floats[0];
                float g6 = g4 - floats[1];
                float h6 = f6 * floats[2] + g6 * floats[3];
                float f7 = -f6 * floats[3] + g6 * floats[2];
                if ((u != 0 || !(h6 < -0.5F)) && (u != s || !(h6 > 0.3F))) {
                    float g7 = 1.0F - (float) u / s;
                    float h7 = 2.9F * (float) Math.pow(g7, 0.6) + 0.15F;
                    float f8 = 2.9F * (float) Math.pow(g7, 1.1) + 0.1F;
                    if (!(f7 > h7) && !(f7 < -f8) && !voxels.filled(r, t, 0)) {
                        int w = f7 < -f8 + 0.55F ? 2 : (f7 > h7 - 0.55F ? 1 : 0);
                        voxels.set(r, t, 0, w, grain(r, t, 0));
                    }
                }
            }
        }
        return voxels.mesh(5.0F * b, 27.0F * b);
    }

    static KnifeMesh katanaScabbard() {
        KnifeMesh.Voxels voxels = new KnifeMesh.Voxels();
        for (int i = 1; i <= 51; i++) {
            int katanaBow = katanaBow(Math.min(i, 48));
            boolean bl = i <= 2;
            int j = bl ? katanaBow - 4 : katanaBow - 3;
            int k = bl ? katanaBow + 3 : katanaBow + 2;
            int m = bl ? 2 : 1;
            for (int o = j; o <= k; o++) {
                for (int p = -m; p <= m; p++) {
                    boolean bl2 = o == j || o == k || Math.abs(p) == m;
                    int n =
                            bl || i >= 50
                                    ? 8
                                    : (i >= 12 && i <= 14 && bl2
                                            ? ((i + o + p) % 2 == 0 ? 7 : 6)
                                            : 4);
                    voxels.set(i, o, p, n, n == 4 ? 0.9F + 0.1F * grain(i, o, p) : grain(i, o, p));
                }
            }
        }
        int katanaBow2 = katanaBow(16);
        voxels.set(16, katanaBow2 + 3, 0, 8, 1.0F);
        voxels.set(17, katanaBow2 + 3, 0, 8, 1.0F);
        return voxels.mesh(1.0F, 51.0F);
    }

    static float katanaCentre(float f) {
        return 0.0016F * (f - 3.0F) * (f - 3.0F);
    }

    static int katanaBow(int i) {
        return Math.round(0.0016F * (i - 3.0F) * (i - 3.0F));
    }

    static KnifeMesh katana() {
        KnifeMesh.Voxels voxels = new KnifeMesh.Voxels();
        for (int i = -17; i <= -1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    int m;
                    if (i == -1) {
                        m = 8;
                    } else if (i == -17) {
                        m = j == 0 && k == 0 ? 8 : 5;
                    } else {
                        boolean bl = Math.abs(j) == 1 ^ Math.abs(k) == 1;
                        boolean bl2 = bl && ((i + 20) % 4 == 1 || (i + 20) % 4 == 2);
                        m = bl2 ? 2 : 7;
                    }
                    voxels.set(i, j, k, m, m == 2 ? 0.82F : grain(i, j, k));
                }
            }
        }
        for (int n = -4; n <= 4; n++) {
            for (int o = -4; o <= 4; o++) {
                float f = (float) Math.sqrt(n * n + o * o);
                if (!(f > 3.9F)) {
                    voxels.set(0, n, o, f >= 3.0F ? 8 : 5, grain(0, n, o));
                }
            }
        }
        for (int p = -1; p <= 1; p++) {
            for (int q = -1; q <= 1; q++) {
                voxels.set(1, p, q, 8, grain(1, p, q));
            }
            voxels.set(2, p, 0, 8, grain(2, p, 0));
        }
        for (int r = 3; r <= 48; r++) {
            int katanaBow = katanaBow(r);
            int t =
                    r >= 48
                            ? katanaBow + 1
                            : (r >= 46 ? katanaBow : (r >= 44 ? katanaBow - 1 : katanaBow - 2));
            int u = r >= 48 ? katanaBow + 1 : katanaBow + 1;
            for (int w = t; w <= u; w++) {
                int v =
                        w == t
                                ? 2
                                : (w == u
                                        ? 1
                                        : (w == t + 1
                                                ? (r > 5
                                                                && Math.sin(r * 0.85)
                                                                                + Math.sin(r * 0.37)
                                                                                        * 0.6
                                                                        > 0.35
                                                        ? 2
                                                        : 0)
                                                : 0));
                voxels.set(r, w, 0, v, grain(r, w, 0));
            }
        }
        return voxels.mesh(3.0F, 49.0F);
    }
}
