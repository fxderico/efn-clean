package net.efn.client.module.impl.render;

import net.efn.client.util.glyph.GlyphShapes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
final class IconGlyphs {

    static final float[] BOLT = GlyphShapes.BOLT;

    static final float[] GEM = GlyphShapes.GEM;

    static final float[] DOLLAR = dollar(16);

    static final float[] EURO = euro(26);

    static final float[] BITCOIN = bitcoin(12, 44);

    static final float[] LETTER_E = letterE();

    static final float[] LETTER_F = letterF();

    static final float[] LETTER_N = letterN();

    private IconGlyphs() {}

    private static void sq(
            IconGlyphs.Sketch sketch,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            float f3,
            float g3) {
        float h3 = 0.16F;
        sketch.quad(f - h3 * g, g, h - h3 * f2, f2, g2 - h3 * h2, h2, f3 - h3 * g3, g3);
    }

    private static float[] letterE() {
        IconGlyphs.Sketch sketch = new IconGlyphs.Sketch();
        sq(sketch, -0.55F, -0.9F, -0.2F, -0.9F, -0.2F, 0.9F, -0.55F, 0.9F);
        sq(sketch, -0.2F, -0.9F, 0.55F, -0.9F, 0.55F, -0.58F, -0.2F, -0.58F);
        sq(sketch, -0.2F, -0.16F, 0.45F, -0.16F, 0.45F, 0.16F, -0.2F, 0.16F);
        sq(sketch, -0.2F, 0.58F, 0.55F, 0.58F, 0.55F, 0.9F, -0.2F, 0.9F);
        return sketch.build();
    }

    private static float[] letterF() {
        IconGlyphs.Sketch sketch = new IconGlyphs.Sketch();
        sq(sketch, -0.55F, -0.9F, -0.2F, -0.9F, -0.2F, 0.9F, -0.55F, 0.9F);
        sq(sketch, -0.2F, -0.9F, 0.55F, -0.9F, 0.55F, -0.58F, -0.2F, -0.58F);
        sq(sketch, -0.2F, -0.16F, 0.45F, -0.16F, 0.45F, 0.16F, -0.2F, 0.16F);
        return sketch.build();
    }

    private static float[] letterN() {
        IconGlyphs.Sketch sketch = new IconGlyphs.Sketch();
        sq(sketch, -0.6F, -0.9F, -0.25F, -0.9F, -0.25F, 0.9F, -0.6F, 0.9F);
        sq(sketch, 0.25F, -0.9F, 0.6F, -0.9F, 0.6F, 0.9F, 0.25F, 0.9F);
        sq(sketch, -0.25F, -0.9F, -0.0F, -0.9F, 0.25F, 0.9F, 0.0F, 0.9F);
        return sketch.build();
    }

    static float[] mobile(float[] floats) {
        if (floats == DOLLAR) {
            return IconGlyphs.Mobile.DOLLAR;
        } else if (floats == EURO) {
            return IconGlyphs.Mobile.EURO;
        } else {
            return floats == BITCOIN ? IconGlyphs.Mobile.BITCOIN : floats;
        }
    }

    private static float[] dollar(int i) {
        float f = 0.34F;
        float[] floats = arc(0.0F, f, f, 28.0F, 270.0F, i);
        float[] floats2 = arc(0.0F, -f, f, 90.0F, -152.0F, i);
        IconGlyphs.Sketch sketch = new IconGlyphs.Sketch();
        sketch.stroke(0.17F, join(floats, Arrays.copyOfRange(floats2, 2, floats2.length)));
        sketch.quad(-0.055F, -0.98F, 0.055F, -0.98F, 0.055F, 0.98F, -0.055F, 0.98F);
        return sketch.build();
    }

    private static float[] euro(int i) {
        IconGlyphs.Sketch sketch = new IconGlyphs.Sketch();
        sketch.stroke(0.17F, arc(0.1F, 0.0F, 0.62F, 48.0F, 312.0F, i));
        sketch.quad(-0.82F, 0.06F, 0.16F, 0.06F, 0.16F, 0.2F, -0.82F, 0.2F);
        sketch.quad(-0.82F, -0.22F, 0.1F, -0.22F, 0.1F, -0.08F, -0.82F, -0.08F);
        return sketch.build();
    }

    private static float[] bitcoin(int i, int j) {
        IconGlyphs.Sketch sketch = new IconGlyphs.Sketch();
        sketch.quad(-0.3F, -0.56F, -0.17F, -0.56F, -0.17F, 0.56F, -0.3F, 0.56F);
        sketch.stroke(
                0.13F,
                join(
                        new float[] {-0.24F, 0.5F},
                        arc(0.06F, 0.27F, 0.23F, 90.0F, -90.0F, i),
                        new float[] {-0.24F, 0.04F}));
        sketch.stroke(
                0.13F,
                join(
                        new float[] {-0.24F, 0.04F},
                        arc(0.1F, -0.24F, 0.28F, 90.0F, -90.0F, i),
                        new float[] {-0.24F, -0.52F}));
        sketch.quad(-0.21F, 0.5F, -0.13F, 0.5F, -0.13F, 0.74F, -0.21F, 0.74F);
        sketch.quad(-0.03F, 0.5F, 0.05F, 0.5F, 0.05F, 0.74F, -0.03F, 0.74F);
        sketch.quad(-0.21F, -0.74F, -0.13F, -0.74F, -0.13F, -0.5F, -0.21F, -0.5F);
        sketch.quad(-0.03F, -0.74F, 0.05F, -0.74F, 0.05F, -0.5F, -0.03F, -0.5F);
        float[] floats = sketch.build();
        float f = (float) Math.cos(Math.toRadians(-14.0));
        float g = (float) Math.sin(Math.toRadians(-14.0));
        for (int k = 0; k < floats.length; k += 2) {
            float h = floats[k] + 0.04F;
            float f2 = floats[k + 1];
            floats[k] = h * f - f2 * g;
            floats[k + 1] = h * g + f2 * f;
        }
        IconGlyphs.Sketch sketch2 = new IconGlyphs.Sketch();
        sketch2.stroke(0.1F, arc(0.0F, 0.0F, 0.92F, 0.0F, 360.0F, j));
        return join(sketch2.build(), floats);
    }

    private static float[] arc(float f, float g, float h, float f2, float g2, int i) {
        float[] floats = new float[(i + 1) * 2];
        for (int m = 0; m <= i; m++) {
            double d = Math.toRadians(f2 + (g2 - f2) * m / i);
            floats[m * 2] = f + h * (float) Math.cos(d);
            floats[m * 2 + 1] = g + h * (float) Math.sin(d);
        }
        return floats;
    }

    private static float[] join(float[]... floats) {
        int i = 0;
        for (float[] floats2 : floats) {
            i += floats2.length;
        }
        float[] floats3 = new float[i];
        int j = 0;
        for (float[] floats4 : floats) {
            System.arraycopy(floats4, 0, floats3, j, floats4.length);
            j += floats4.length;
        }
        return floats3;
    }

    @Environment(EnvType.CLIENT)
    private static final class Mobile {

        static final float[] DOLLAR = IconGlyphs.dollar(10);

        static final float[] EURO = IconGlyphs.euro(16);

        static final float[] BITCOIN = IconGlyphs.bitcoin(8, 24);

        private Mobile() {}
    }

    @Environment(EnvType.CLIENT)
    private static final class Sketch {

        private float[] data = new float[64];

        private int size;

        private Sketch() {}

        void quad(float f, float g, float h, float f2, float g2, float h2, float f3, float g3) {
            if (this.size + 8 > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            this.data[this.size++] = f;
            this.data[this.size++] = g;
            this.data[this.size++] = h;
            this.data[this.size++] = f2;
            this.data[this.size++] = g2;
            this.data[this.size++] = h2;
            this.data[this.size++] = f3;
            this.data[this.size++] = g3;
        }

        void stroke(float f, float... g) {
            float[] floats = new float[g.length / 2];
            float[] floats2 = new float[g.length / 2];
            Arrays.fill(floats, -f * 0.5F);
            Arrays.fill(floats2, f * 0.5F);
            this.band(g, floats, floats2);
        }

        void band(float[] floats, float[] floats2, float[] floats3) {
            int i = floats.length / 2;
            float[] floats4 = new float[i];
            float[] floats5 = new float[i];
            for (int j = 0; j < i; j++) {
                int k = Math.max(0, j - 1);
                int m = Math.min(i - 1, j + 1);
                float f = floats[m * 2] - floats[k * 2];
                float g = floats[m * 2 + 1] - floats[k * 2 + 1];
                float h = (float) Math.sqrt(f * f + g * g);
                floats4[j] = -g / h;
                floats5[j] = f / h;
            }
            int n = 0;
            for (int o = 1; o < i; o++) {
                float f2 = floats[n * 2];
                float g2 = floats[n * 2 + 1];
                float h2 = floats[o * 2];
                float f3 = floats[o * 2 + 1];
                this.quad(
                        f2 + floats4[n] * floats2[n],
                        g2 + floats5[n] * floats2[n],
                        h2 + floats4[o] * floats2[o],
                        f3 + floats5[o] * floats2[o],
                        h2 + floats4[o] * floats3[o],
                        f3 + floats5[o] * floats3[o],
                        f2 + floats4[n] * floats3[n],
                        g2 + floats5[n] * floats3[n]);
                n++;
            }
        }

        float[] build() {
            return Arrays.copyOf(this.data, this.size);
        }
    }
}
