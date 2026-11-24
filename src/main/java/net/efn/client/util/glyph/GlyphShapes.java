package net.efn.client.util.glyph;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public final class GlyphShapes {

    public static final float[] BOLT = bolt();

    public static final float[] GEM = gem();

    public static final float[] SPARK = spark();

    public static final float[] GRAIN = grain();

    public static final float[] DIAMOND = diamond();

    public static final float[] DISK =
            new GlyphShapes.Builder().disk(0.0F, 0.0F, 1.0F, 1.0F, 12).build();

    public static final float[] OCTAGON =
            new GlyphShapes.Builder().disk(0.0F, 0.0F, 1.0F, 1.0F, 8).build();

    public static final float[] HALO =
            new GlyphShapes.Builder()
                    .arc(0.0F, 0.0F, 0.84F, 0.84F, 1.0F, 1.0F, 0.0F, 6.2831855F, 20)
                    .build();

    private GlyphShapes() {}

    private static float[] bolt() {
        GlyphShapes.Builder builder = new GlyphShapes.Builder();
        builder.quad(-0.17F, -1.0F, 0.17F, -1.0F, 0.09F, 0.06F, -0.09F, 0.06F);
        builder.quad(-0.56F, -0.02F, -0.09F, 0.04F, -0.09F, 0.2F, -0.56F, 0.26F);
        builder.quad(-0.09F, 0.04F, 0.09F, 0.04F, 0.09F, 0.2F, -0.09F, 0.2F);
        builder.quad(0.09F, 0.04F, 0.56F, -0.02F, 0.56F, 0.26F, 0.09F, 0.2F);
        builder.arc(0.0F, 0.6F, 0.19F, 0.28F, 0.3F, 0.42F, -1.5707964F, 4.712389F, 10);
        return builder.build();
    }

    private static float[] gem() {
        GlyphShapes.Builder builder = new GlyphShapes.Builder();
        float f = 0.035F;
        float g = -0.12F;
        float h = 0.56F;
        float f2 = 0.78F;
        byte b = 5;
        for (int i = 0; i < b; i++) {
            double d = 1.5707963267948966 + 3.141592653589793 * i / b;
            double e = 1.5707963267948966 + 3.141592653589793 * (i + 1) / b;
            float g2 = g + (float) Math.sin(d) * f2;
            float h2 = g + (float) Math.sin(e) * f2;
            float f3 = (float) Math.cos(d) * h - f;
            float g3 = (float) Math.cos(e) * h - f;
            builder.quad(-f, g2, f3, g2, g3, h2, -f, h2);
            builder.quad(
                    f, -g2 + 2.0F * g, -f3, -g2 + 2.0F * g, -g3, -h2 + 2.0F * g, f, -h2 + 2.0F * g);
        }
        builder.quad(-0.44F, 0.62F, 0.44F, 0.62F, 0.33F, 0.86F, -0.33F, 0.86F);
        builder.quad(-0.24F, 0.86F, 0.24F, 0.86F, 0.13F, 1.0F, -0.13F, 1.0F);
        builder.stroke(0.055F, -0.34F, 0.7F, -0.62F, 0.8F, -0.74F, 0.98F);
        builder.stroke(0.055F, 0.34F, 0.7F, 0.62F, 0.8F, 0.74F, 0.98F);
        builder.stroke(0.055F, -0.5F, 0.12F, -0.82F, 0.2F, -0.92F, 0.04F);
        builder.stroke(0.055F, 0.5F, 0.12F, 0.82F, 0.2F, 0.92F, 0.04F);
        builder.stroke(0.055F, -0.44F, -0.52F, -0.74F, -0.72F, -0.8F, -0.96F);
        builder.stroke(0.055F, 0.44F, -0.52F, 0.74F, -0.72F, 0.8F, -0.96F);
        return builder.build();
    }

    private static float[] spark() {
        GlyphShapes.Builder builder = new GlyphShapes.Builder();
        builder.quad(0.0F, -1.0F, 0.15F, 0.0F, 0.0F, 1.0F, -0.15F, 0.0F);
        builder.quad(-0.62F, 0.0F, 0.0F, -0.13F, 0.62F, 0.0F, 0.0F, 0.13F);
        return builder.build();
    }

    private static float[] grain() {
        return new GlyphShapes.Builder()
                .quad(-0.8F, -0.62F, 0.72F, -0.88F, 0.9F, 0.46F, -0.52F, 0.82F)
                .build();
    }

    private static float[] diamond() {
        return new GlyphShapes.Builder()
                .quad(0.0F, -1.0F, 1.0F, 0.0F, 0.0F, 1.0F, -1.0F, 0.0F)
                .build();
    }

    private static float[] curve(float f, float g, float h, float f2, float g2, float h2, int i) {
        float[] floats = new float[(i + 1) * 2];
        for (int n = 0; n <= i; n++) {
            float f3 = (float) n / i;
            float g3 = 1.0F - f3;
            floats[n * 2] = g3 * g3 * f + 2.0F * g3 * f3 * h + f3 * f3 * g2;
            floats[n * 2 + 1] = g3 * g3 * g + 2.0F * g3 * f3 * f2 + f3 * f3 * h2;
        }
        return floats;
    }

    private static float[] append(float[] floats, float f, float g) {
        float[] floats2 = Arrays.copyOf(floats, floats.length + 2);
        floats2[floats.length] = f;
        floats2[floats.length + 1] = g;
        return floats2;
    }

    @Environment(EnvType.CLIENT)
    private static final class Builder {

        private float[] data = new float[64];

        private int size;

        private float offsetX;

        private float offsetY;

        private Builder() {}

        GlyphShapes.Builder offset(float offsetX2, float offsetY2) {
            this.offsetX = offsetX2;
            this.offsetY = offsetY2;
            return this;
        }

        GlyphShapes.Builder quad(
                float f, float g, float h, float f2, float g2, float h2, float f3, float g3) {
            if (this.size + 8 > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            this.data[this.size++] = f + this.offsetX;
            this.data[this.size++] = g + this.offsetY;
            this.data[this.size++] = h + this.offsetX;
            this.data[this.size++] = f2 + this.offsetY;
            this.data[this.size++] = g2 + this.offsetX;
            this.data[this.size++] = h2 + this.offsetY;
            this.data[this.size++] = f3 + this.offsetX;
            this.data[this.size++] = g3 + this.offsetY;
            return this;
        }

        GlyphShapes.Builder arc(
                float f,
                float g,
                float h,
                float f2,
                float g2,
                float h2,
                float f3,
                float g3,
                int i) {
            for (int o = 0; o < i; o++) {
                double d = f3 + (double) ((g3 - f3) * o) / i;
                double e = f3 + (double) ((g3 - f3) * (o + 1)) / i;
                float h3 = (float) Math.cos(d);
                float f4 = (float) Math.sin(d);
                float g4 = (float) Math.cos(e);
                float h4 = (float) Math.sin(e);
                this.quad(
                        f + h3 * h,
                        g + f4 * f2,
                        f + h3 * g2,
                        g + f4 * h2,
                        f + g4 * g2,
                        g + h4 * h2,
                        f + g4 * h,
                        g + h4 * f2);
            }
            return this;
        }

        GlyphShapes.Builder disk(float f, float g, float h, float f2, int i) {
            float[] floats = new float[i * 2];
            for (int k = 0; k < i; k++) {
                double d = 6.283185307179586 * k / i + 3.141592653589793 / i;
                floats[k * 2] = f + (float) Math.cos(d) * h;
                floats[k * 2 + 1] = g + (float) Math.sin(d) * f2;
            }
            for (int m = 1; m + 1 < i; m += 2) {
                int j = Math.min(m + 2, i - 1);
                this.quad(
                        floats[0],
                        floats[1],
                        floats[m * 2],
                        floats[m * 2 + 1],
                        floats[(m + 1) * 2],
                        floats[(m + 1) * 2 + 1],
                        floats[j * 2],
                        floats[j * 2 + 1]);
            }
            return this;
        }

        GlyphShapes.Builder stroke(float f, float... g) {
            int i = g.length / 2;
            if (i < 2) {
                return this;
            } else {
                float h = f * 0.5F;
                float[] floats = new float[i];
                float[] floats2 = new float[i];
                for (int j = 0; j < i; j++) {
                    float f2 = 0.0F;
                    float g2 = 0.0F;
                    float h2 = 0.0F;
                    float f3 = 0.0F;
                    float g3;
                    float h3;
                    float f4;
                    if (j > 0
                            && (f4 =
                                            (float)
                                                    Math.sqrt(
                                                            (h3 = g[j * 2] - g[j * 2 - 2]) * h3
                                                                    + (g3 =
                                                                                    g[j * 2 + 1]
                                                                                            - g[
                                                                                                    j
                                                                                                                    * 2
                                                                                                            - 1])
                                                                            * g3))
                                    > 1.0E-6F) {
                        h2 = -g3 / f4;
                        f3 = h3 / f4;
                        f2 += h2;
                        g2 += f3;
                    }
                    if (j < i - 1
                            && (f4 =
                                            (float)
                                                    Math.sqrt(
                                                            (h3 = g[j * 2 + 2] - g[j * 2]) * h3
                                                                    + (g3 =
                                                                                    g[j * 2 + 3]
                                                                                            - g[
                                                                                                    j
                                                                                                                    * 2
                                                                                                            + 1])
                                                                            * g3))
                                    > 1.0E-6F) {
                        f2 += -g3 / f4;
                        g2 += h3 / f4;
                        if (j == 0) {
                            h2 = -g3 / f4;
                            f3 = h3 / f4;
                        }
                    }
                    float g4;
                    if ((g4 = (float) Math.sqrt(f2 * f2 + g2 * g2)) < 1.0E-6F) {
                        f2 = h2;
                        g2 = f3;
                        g4 = 1.0F;
                    }
                    float h4;
                    float f5;
                    float g5 = Math.max(0.5F, (h4 = f2 / g4) * h2 + (f5 = g2 / g4) * f3);
                    floats[j] = h4 * h / g5;
                    floats2[j] = f5 * h / g5;
                }
                for (int k = 0; k < i - 1; k++) {
                    float h5 = g[k * 2];
                    float f6 = g[k * 2 + 1];
                    float g6 = g[k * 2 + 2];
                    float h6 = g[k * 2 + 3];
                    this.quad(
                            h5 - floats[k],
                            f6 - floats2[k],
                            g6 - floats[k + 1],
                            h6 - floats2[k + 1],
                            g6 + floats[k + 1],
                            h6 + floats2[k + 1],
                            h5 + floats[k],
                            f6 + floats2[k]);
                }
                return this;
            }
        }

        float[] build() {
            return Arrays.copyOf(this.data, this.size);
        }
    }
}
