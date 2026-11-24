package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Strand extends Shape {

    private static final int STRIDE = 20;

    private final int count;

    private final float[] seg;

    private float ratio = 1.0F;

    private int flatMode;

    private float fx;

    private float fy;

    private float fz;

    private float minHalf;

    private Strand(float[] seg2, int count2) {
        this.seg = seg2;
        this.count = count2;
        float f = 100000.0F;
        float g = 100000.0F;
        float h = 100000.0F;
        float f2 = -100000.0F;
        float g2 = -100000.0F;
        float h2 = -100000.0F;
        for (int j = 0; j < count2; j++) {
            int i = j * 20;
            f = Math.min(f, seg2[i + 14]);
            g = Math.min(g, seg2[i + 15]);
            h = Math.min(h, seg2[i + 16]);
            f2 = Math.max(f2, seg2[i + 17]);
            g2 = Math.max(g2, seg2[i + 18]);
            h2 = Math.max(h2, seg2[i + 19]);
        }
        this.bounds(f, g, h, f2, g2, h2);
    }

    static Strand tapered(float f, float g, float[] floats) {
        if (floats.length >= 6 && floats.length % 3 == 0) {
            int i = floats.length / 3;
            float[] floats2 = new float[i];
            for (int j = 1; j < i; j++) {
                float h = floats[j * 3] - floats[j * 3 - 3];
                float f2 = floats[j * 3 + 1] - floats[j * 3 - 2];
                float g2 = floats[j * 3 + 2] - floats[j * 3 - 1];
                floats2[j] = floats2[j - 1] + (float) Math.sqrt(h * h + f2 * f2 + g2 * g2);
            }
            float h2 = Math.max(1.0E-6F, floats2[i - 1]);
            float[] floats3 = new float[i * 4];
            for (int k = 0; k < i; k++) {
                floats3[k * 4] = floats[k * 3];
                floats3[k * 4 + 1] = floats[k * 3 + 1];
                floats3[k * 4 + 2] = floats[k * 3 + 2];
                floats3[k * 4 + 3] = f + (g - f) * floats2[k] / h2;
            }
            return of(floats3, false);
        } else {
            throw new IllegalArgumentException(
                    "strand needs at least two points given as x,y,z triples");
        }
    }

    static Strand of(float[] floats, boolean bl) {
        if (floats.length >= 8 && floats.length % 4 == 0) {
            int i = floats.length / 4;
            if (bl) {
                float[] floats2 = new float[20];
                fill(
                        floats2,
                        0,
                        floats[0],
                        floats[1],
                        floats[2],
                        floats[3],
                        floats[4],
                        floats[5],
                        floats[6],
                        floats[7],
                        floats[8],
                        floats[9],
                        floats[10],
                        floats[11]);
                return new Strand(floats2, 1);
            } else if (i == 2) {
                float[] floats3 = new float[20];
                fill(
                        floats3,
                        0,
                        floats[0],
                        floats[1],
                        floats[2],
                        floats[3],
                        (floats[0] + floats[4]) * 0.5F,
                        (floats[1] + floats[5]) * 0.5F,
                        (floats[2] + floats[6]) * 0.5F,
                        (floats[3] + floats[7]) * 0.5F,
                        floats[4],
                        floats[5],
                        floats[6],
                        floats[7]);
                return new Strand(floats3, 1);
            } else {
                int j = i - 2;
                float[] floats4 = new float[j * 20];
                for (int k = 1; k <= j; k++) {
                    int m = (k - 1) * 4;
                    int n = k * 4;
                    int o = (k + 1) * 4;
                    float f;
                    float g;
                    float h;
                    float f2;
                    if (k == 1) {
                        f2 = floats[0];
                        h = floats[1];
                        g = floats[2];
                        f = floats[3];
                    } else {
                        f2 = (floats[m] + floats[n]) * 0.5F;
                        h = (floats[m + 1] + floats[n + 1]) * 0.5F;
                        g = (floats[m + 2] + floats[n + 2]) * 0.5F;
                        f = (floats[m + 3] + floats[n + 3]) * 0.5F;
                    }
                    float g2;
                    float h2;
                    float f3;
                    float g3;
                    if (k == j) {
                        g3 = floats[o];
                        f3 = floats[o + 1];
                        h2 = floats[o + 2];
                        g2 = floats[o + 3];
                    } else {
                        g3 = (floats[n] + floats[o]) * 0.5F;
                        f3 = (floats[n + 1] + floats[o + 1]) * 0.5F;
                        h2 = (floats[n + 2] + floats[o + 2]) * 0.5F;
                        g2 = (floats[n + 3] + floats[o + 3]) * 0.5F;
                    }
                    fill(
                            floats4,
                            (k - 1) * 20,
                            f2,
                            h,
                            g,
                            f,
                            floats[n],
                            floats[n + 1],
                            floats[n + 2],
                            floats[n + 3],
                            g3,
                            f3,
                            h2,
                            g2);
                }
                return new Strand(floats4, j);
            }
        } else {
            throw new IllegalArgumentException(
                    "tube needs at least two points given as x,y,z,radius quadruples");
        }
    }

    private static void fill(
            float[] floats,
            int i,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            float f3,
            float g3,
            float h3,
            float f4,
            float g4,
            float h4) {
        floats[i] = f;
        floats[i + 1] = g;
        floats[i + 2] = h;
        floats[i + 3] = g2 - f;
        floats[i + 4] = h2 - g;
        floats[i + 5] = f3 - h;
        floats[i + 6] = f - 2.0F * g2 + h3;
        floats[i + 7] = g - 2.0F * h2 + f4;
        floats[i + 8] = h - 2.0F * f3 + g4;
        floats[i + 9] = f2;
        floats[i + 10] = g3;
        floats[i + 11] = h4;
        float f5 =
                floats[i + 6] * floats[i + 6]
                        + floats[i + 7] * floats[i + 7]
                        + floats[i + 8] * floats[i + 8];
        float g5 = h3 - f;
        float h5 = f4 - g;
        float f6 = g4 - h;
        float g6 = g5 * g5 + h5 * h5 + f6 * f6;
        boolean bl = f5 < 1.0E-5F * Math.max(g6, 1.0E-6F);
        float h6 = floats[i + 12] = bl ? -1.0F : 1.0F / f5;
        floats[i + 13] = bl ? (g6 > 1.0E-12F ? 1.0F / g6 : 0.0F) : 0.0F;
        float f7 = Math.max(f2, Math.max(g3, h4));
        floats[i + 14] = Math.min(f, Math.min(g2, h3)) - f7;
        floats[i + 15] = Math.min(g, Math.min(h2, f4)) - f7;
        floats[i + 16] = Math.min(h, Math.min(f3, g4)) - f7;
        floats[i + 17] = Math.max(f, Math.max(g2, h3)) + f7;
        floats[i + 18] = Math.max(g, Math.max(h2, f4)) + f7;
        floats[i + 19] = Math.max(h, Math.max(f3, g4)) + f7;
    }

    Strand thickened(float f) {
        if (f <= this.minHalf) {
            return this;
        } else {
            Strand strand = new Strand(this.seg, this.count);
            strand.ratio = this.ratio;
            strand.flatMode = this.flatMode;
            strand.fx = this.fx;
            strand.fy = this.fy;
            strand.fz = this.fz;
            strand.minHalf = f;
            strand.paint = this.paint;
            return strand;
        }
    }

    public Strand flatten(float ratio2, float f, float g, float h) {
        float f2 = (float) Math.sqrt(f * f + g * g + h * h);
        if (!(f2 < 1.0E-6F) && !(ratio2 <= 1.0F)) {
            this.ratio = ratio2;
            this.flatMode = 1;
            this.fx = f / f2;
            this.fy = g / f2;
            this.fz = h / f2;
            return this;
        } else {
            this.flatMode = 0;
            return this;
        }
    }

    public Strand flattenFrom(float ratio2, float fx2, float fy2, float fz2) {
        if (ratio2 <= 1.0F) {
            this.flatMode = 0;
            return this;
        } else {
            this.ratio = ratio2;
            this.flatMode = 2;
            this.fx = fx2;
            this.fy = fy2;
            this.fz = fz2;
            return this;
        }
    }

    @Override
    public float distance(float f, float g, float h) {
        return this.distance(f, g, h, 3.4028235E38F);
    }

    @Override
    float distance(float f, float g, float h, float i) {
        float[] seg2 = this.seg;
        int j = 0;
        float f2 = 3.4028235E38F;
        for (int k = 0; k < this.count; k++) {
            float gap = gap(seg2, k * 20, f, g, h);
            if (gap < f2) {
                f2 = gap;
                j = k;
            }
        }
        if (f2 >= i) {
            return f2;
        } else {
            float segment = this.segment(seg2, j * 20, f, g, h);
            for (int m = 0; m < this.count; m++) {
                float segment2;
                if (m != j
                        && !(gap(seg2, m * 20, f, g, h) >= Math.min(segment, i))
                        && (segment2 = this.segment(seg2, m * 20, f, g, h)) < segment) {
                    segment = segment2;
                }
            }
            return segment;
        }
    }

    private static float gap(float[] floats, int i, float f, float g, float h) {
        float f2 =
                f < floats[i + 14]
                        ? floats[i + 14] - f
                        : (f > floats[i + 17] ? f - floats[i + 17] : 0.0F);
        float g2 =
                g < floats[i + 15]
                        ? floats[i + 15] - g
                        : (g > floats[i + 18] ? g - floats[i + 18] : 0.0F);
        float h2 =
                h < floats[i + 16]
                        ? floats[i + 16] - h
                        : (h > floats[i + 19] ? h - floats[i + 19] : 0.0F);
        return (float) Math.sqrt(f2 * f2 + g2 * g2 + h2 * h2);
    }

    private float segment(float[] floats, int i, float f, float g, float h) {
        float f2 = floats[i];
        float g2 = floats[i + 1];
        float h2 = floats[i + 2];
        float f3 = floats[i + 3];
        float g3 = floats[i + 4];
        float h3 = floats[i + 5];
        float f4 = floats[i + 6];
        float g4 = floats[i + 7];
        float h4 = floats[i + 8];
        float f5 = floats[i + 12];
        float g5;
        if (f5 < 0.0F) {
            float h5 = 2.0F * f3 + f4;
            float f6 = 2.0F * g3 + g4;
            float g6 = 2.0F * h3 + h4;
            g5 = Shapes.clamp01(((f - f2) * h5 + (g - g2) * f6 + (h - h2) * g6) * floats[i + 13]);
        } else {
            float h6 = f5 * (f3 * f4 + g3 * g4 + h3 * h4);
            float f7 = f2 - f;
            float g7 = g2 - g;
            float h7 = h2 - h;
            float f8 =
                    f5
                            * (2.0F * (f3 * f3 + g3 * g3 + h3 * h3) + f7 * f4 + g7 * g4 + h7 * h4)
                            / 3.0F;
            float g8 = f5 * (f7 * f3 + g7 * g3 + h7 * h3);
            float h8 = h6 * (2.0F * h6 * h6 - 3.0F * f8) + g8;
            float f9 = f8 - h6 * h6;
            float g9 = f9 * f9 * f9;
            float h9 = h8 * h8 + 4.0F * g9;
            if (h9 >= 0.0F) {
                h9 = (float) Math.sqrt(h9);
                g5 =
                        Shapes.clamp01(
                                (float) (Math.cbrt((h9 - h8) * 0.5F) + Math.cbrt((-h9 - h8) * 0.5F))
                                        - h6);
            } else {
                float f10 = (float) Math.sqrt(-f9);
                float g10 = h8 / (f9 * f10 * 2.0F);
                g10 = g10 < -1.0F ? -1.0F : Math.min(g10, 1.0F);
                float h10 = (float) Math.acos(g10) / 3.0F;
                float f11 = (float) Math.cos(h10);
                float g11 = (float) Math.sin(h10) * 1.7320508F;
                float h11 = Shapes.clamp01((f11 + f11) * f10 - h6);
                float f12 = Shapes.clamp01((-g11 - f11) * f10 - h6);
                float g12 = f7 + (2.0F * f3 + f4 * h11) * h11;
                float h12 = g7 + (2.0F * g3 + g4 * h11) * h11;
                float f13 = h7 + (2.0F * h3 + h4 * h11) * h11;
                float g13 = f7 + (2.0F * f3 + f4 * f12) * f12;
                float h13 = g7 + (2.0F * g3 + g4 * f12) * f12;
                float f14 = h7 + (2.0F * h3 + h4 * f12) * f12;
                g5 =
                        g12 * g12 + h12 * h12 + f13 * f13 <= g13 * g13 + h13 * h13 + f14 * f14
                                ? h11
                                : f12;
            }
        }
        float g14 = f2 + (2.0F * f3 + f4 * g5) * g5;
        float h14 = g2 + (2.0F * g3 + g4 * g5) * g5;
        float f15 = h2 + (2.0F * h3 + h4 * g5) * g5;
        float g15 = f - g14;
        float h15 = g - h14;
        float f16 = h - f15;
        float g16 = 1.0F - g5;
        float h16 =
                g16 * g16 * floats[i + 9]
                        + 2.0F * g16 * g5 * floats[i + 10]
                        + g5 * g5 * floats[i + 11];
        float f17 = g15 * g15 + h15 * h15 + f16 * f16;
        if (this.flatMode == 0) {
            return (float) Math.sqrt(f17) - Math.max(h16, this.minHalf);
        } else {
            h16 = Math.max(h16, this.minHalf * this.ratio);
            float g17 = 2.0F * (f3 + f4 * g5);
            float h17 = 2.0F * (g3 + g4 * g5);
            float f18 = 2.0F * (h3 + h4 * g5);
            float g18 = (float) Math.sqrt(g17 * g17 + h17 * h17 + f18 * f18);
            if (g18 > 1.0E-6F) {
                g17 /= g18;
                h17 /= g18;
                f18 /= g18;
            }
            float fz2;
            float fy2;
            float fx2;
            if (this.flatMode == 1) {
                fx2 = this.fx;
                fy2 = this.fy;
                fz2 = this.fz;
            } else {
                fx2 = g14 - this.fx;
                fy2 = h14 - this.fy;
                fz2 = f15 - this.fz;
            }
            float h18 = fx2 * g17 + fy2 * h17 + fz2 * f18;
            float f19;
            float g19;
            float h19;
            float f20 =
                    (float)
                            Math.sqrt(
                                    (h19 = fx2 - h18 * g17) * h19
                                            + (g19 = fy2 - h18 * h17) * g19
                                            + (f19 = fz2 - h18 * f18) * f19);
            if (f20 < 1.0E-6F) {
                return (float) Math.sqrt(f17) - h16;
            } else {
                float g20 = (g15 * h19 + h15 * g19 + f16 * f19) / f20;
                float h20 = Math.max(0.0F, f17 - g20 * g20);
                return ((float) Math.sqrt(h20 + this.ratio * this.ratio * g20 * g20) - h16)
                        / this.ratio;
            }
        }
    }
}
