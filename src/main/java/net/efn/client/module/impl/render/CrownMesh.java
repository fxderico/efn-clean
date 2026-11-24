package net.efn.client.module.impl.render;

import net.efn.client.util.MobilePerformance;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
final class CrownMesh {

    static final int GOLD = 0;

    static final int INLAY = 1;

    static final int GEM = 2;

    static final int BRONZE = 3;

    static final int FUR = 4;

    static final int MATERIALS = 5;

    private static final float PX = 0.0625F;

    private static final float TWIST = 0.12F;

    private static final float[] CORNER_U = new float[] {0.0F, 1.0F, 1.0F, 0.0F};

    private static final float[] CORNER_V = new float[] {0.0F, 0.0F, 1.0F, 1.0F};

    private static final float BAND_TOP = -6.6F;

    private static final float BAND_BOTTOM = -5.3F;

    private final float[] pos;

    private final float[] normal;

    private final float[] sweep;

    private final byte[] material;

    private final float[] tone;

    private final int quads;

    private CrownMesh.Look coloredLook;

    private int[] cachedColors;

    CrownMesh(CrownMesh.Builder builder) {
        this.quads = builder.quads;
        this.pos = Arrays.copyOf(builder.pos, this.quads * 12);
        this.normal = Arrays.copyOf(builder.normal, this.quads * 3);
        this.sweep = Arrays.copyOf(builder.sweep, this.quads * 8);
        this.material = Arrays.copyOf(builder.material, this.quads);
        this.tone = Arrays.copyOf(builder.tone, this.quads);
    }

    boolean isEmpty() {
        return this.quads == 0;
    }

    void emit(Entry entry, VertexConsumer vertexConsumer, int i, CrownMesh.Look coloredLook2) {
        int[] ints = null;
        if (MobilePerformance.active()) {
            if (this.cachedColors == null) {
                this.cachedColors = new int[this.quads * 4];
            }
            if (this.coloredLook != coloredLook2) {
                this.colorVertices(coloredLook2, this.cachedColors);
                this.coloredLook = coloredLook2;
            }
            ints = this.cachedColors;
        } else {
            this.cachedColors = null;
            this.coloredLook = null;
        }
        int[] ints2 = coloredLook2.palette();
        float sweepGain2 = coloredLook2.sweepGain();
        boolean bl = sweepGain2 > 0.004F;
        float sweepCos2 = coloredLook2.sweepCos();
        float sweepSin2 = coloredLook2.sweepSin();
        for (int j = 0; j < this.quads; j++) {
            byte b = this.material[j];
            int k = ints2[b];
            float f = b == 2 ? this.tone[j] * coloredLook2.gemPulse() : this.tone[j];
            boolean bl2 = bl && (b == 0 || b == 3);
            int m = b == 2 && coloredLook2.glow() ? 15728880 : i;
            int n = j * 3;
            float g = this.normal[n];
            float h = this.normal[n + 1];
            float f2 = this.normal[n + 2];
            for (int o = 0; o < 4; o++) {
                int p = j * 12 + o * 3;
                float g2 = f;
                int q;
                float h2;
                if (ints == null
                        && bl2
                        && (h2 =
                                        this.sweep[q = j * 8 + o * 2] * sweepCos2
                                                + this.sweep[q + 1] * sweepSin2)
                                > 0.0F) {
                    h2 *= h2;
                    h2 *= h2;
                    h2 *= h2;
                    g2 = f + sweepGain2 * h2;
                }
                vertexConsumer
                        .vertex(entry, this.pos[p], this.pos[p + 1], this.pos[p + 2])
                        .color(ints == null ? tint(k, g2) : ints[j * 4 + o])
                        .texture(CORNER_U[o], CORNER_V[o])
                        .overlay(OverlayTexture.DEFAULT_UV)
                        .light(m)
                        .normal(entry, g, h, f2);
            }
        }
    }

    private void colorVertices(CrownMesh.Look look, int[] ints) {
        int[] ints2 = look.palette();
        float sweepGain2 = look.sweepGain();
        float sweepCos2 = look.sweepCos();
        float sweepSin2 = look.sweepSin();
        boolean bl = sweepGain2 > 0.004F;
        for (int i = 0; i < this.quads; i++) {
            byte b = this.material[i];
            int j = ints2[b];
            float f = b == 2 ? this.tone[i] * look.gemPulse() : this.tone[i];
            boolean bl2 = bl && (b == 0 || b == 3);
            for (int k = 0; k < 4; k++) {
                float g = f;
                int m;
                float h;
                if (bl2
                        && (h =
                                        this.sweep[m = i * 8 + k * 2] * sweepCos2
                                                + this.sweep[m + 1] * sweepSin2)
                                > 0.0F) {
                    h *= h;
                    h *= h;
                    h *= h;
                    g = f + sweepGain2 * h;
                }
                ints[i * 4 + k] = tint(j, g);
            }
        }
    }

    private static int tint(int i, float f) {
        int j = i >> 16 & 0xFF;
        int k = i >> 8 & 0xFF;
        int m = i & 0xFF;
        if (f <= 1.0F) {
            j = (int) (j * f);
            k = (int) (k * f);
            m = (int) (m * f);
        } else {
            float g = Math.min(1.0F, f - 1.0F);
            j += (int) ((255 - j) * g);
            k += (int) ((255 - k) * g);
            m += (int) ((255 - m) * g);
        }
        return 0xFF000000 | j << 16 | k << 8 | m;
    }

    static CrownMesh head(int i, int j, boolean bl) {
        CrownMesh.Builder builder = new CrownMesh.Builder();
        float f = bl ? 5.35F : 4.8F;
        float g = -4.0F - f;
        switch (i) {
            case 1:
                halo(builder, g);
                break;
            case 2:
                shards(builder, g);
                break;
            default:
                brow(builder, f);
                crown(builder, f);
        }
        return new CrownMesh(builder);
    }

    private static void spike(
            CrownMesh.Builder builder, float f, float g, float h, float f2, float g2, int i) {
        builder.slab(
                -6.6F,
                f - h,
                f + h,
                g - f2,
                g + f2,
                -6.6F - g2,
                f - 0.1F,
                f + 0.1F,
                g - 0.1F,
                g + 0.1F,
                i);
    }

    private static void crown(CrownMesh.Builder builder, float f) {
        float g = f + 0.075F;
        float h = f + 0.45F;
        spike(builder, 0.0F, -g, 1.05F, 0.55F, 7.17F, 2);
        for (int i = -1; i <= 1; i += 2) {
            spike(builder, i * 2.9F, -g, 0.95F, 0.55F, 4.99F, 0);
            spike(builder, i * (g - 0.15F), -(g - 0.15F), 0.85F, 0.85F, 3.84F, 3);
            spike(builder, i * g, 0.0F, 0.55F, 1.05F, 4.61F, 0);
            spike(builder, i * g, 2.9F, 0.55F, 0.95F, 3.58F, 0);
            spike(builder, i * (g - 0.15F), g - 0.15F, 0.85F, 0.85F, 3.33F, 3);
            spike(builder, i * 2.9F, g, 0.95F, 0.55F, 3.84F, 0);
        }
        spike(builder, 0.0F, g, 1.05F, 0.55F, 4.61F, 0);
        builder.box(-0.65F, -6.45F, -h - 0.2F, 0.65F, -5.45F, -h + 0.05F, 2);
    }

    private static void halo(CrownMesh.Builder builder, float f) {
        float g = f - 2.2F;
        float h = g - 0.3F;
        float f2 = g + 0.3F;
        float g2 = 3.5F;
        float h2 = 4.6F;
        float f3 = (g2 + h2) * 0.5F;
        byte b = 28;
        for (int i = 0; i < b; i++) {
            double d = 6.283185307179586 * i / b;
            double e = 6.283185307179586 * (i + 1) / b;
            double d2 = (d + e) * 0.5;
            float g3 = f3 * (float) Math.cos(d2);
            float h3 = f3 * (float) Math.sin(d2);
            float f4 = g2 * (float) Math.cos(d);
            float g4 = g2 * (float) Math.sin(d);
            float h4 = g2 * (float) Math.cos(e);
            float f5 = g2 * (float) Math.sin(e);
            float g5 = h2 * (float) Math.cos(d);
            float h5 = h2 * (float) Math.sin(d);
            float f6 = h2 * (float) Math.cos(e);
            float g6 = h2 * (float) Math.sin(e);
            builder.quad(f4, h, g4, g5, h, h5, f6, h, g6, h4, h, f5, g3, g, h3, 0);
            builder.quad(f4, f2, g4, g5, f2, h5, f6, f2, g6, h4, f2, f5, g3, g, h3, 3);
            builder.quad(g5, h, h5, f6, h, g6, f6, f2, g6, g5, f2, h5, g3, g, h3, 0);
            builder.quad(f4, h, g4, h4, h, f5, h4, f2, f5, f4, f2, g4, g3, g, h3, 3);
        }
        for (int j = 0; j < 4; j++) {
            double e2 = 1.5707963267948966 * j;
            float h6 = f3 * (float) Math.cos(e2);
            float f7 = f3 * (float) Math.sin(e2);
            builder.box(h6 - 0.55F, g - 0.5F, f7 - 0.55F, h6 + 0.55F, g + 0.5F, f7 + 0.55F, 2);
        }
    }

    private static void shards(CrownMesh.Builder builder, float f) {
        byte b = 9;
        float g = 5.9F;
        for (int i = 0; i < b; i++) {
            double d = 6.283185307179586 * i / b;
            float h = g * (float) Math.cos(d);
            float f2 = g * (float) Math.sin(d);
            float g2 = 0.85F + 0.3F * (i * 5 % 3) / 2.0F;
            float h2 = f - 0.4F - (i % 2 == 0 ? 0.0F : 0.9F);
            float f3 = 0.95F * g2;
            builder.slab(
                    h2,
                    h - f3,
                    h + f3,
                    f2 - f3,
                    f2 + f3,
                    h2 - 2.8F * g2,
                    h - 0.05F,
                    h + 0.05F,
                    f2 - 0.05F,
                    f2 + 0.05F,
                    2);
            builder.slab(
                    h2,
                    h - f3,
                    h + f3,
                    f2 - f3,
                    f2 + f3,
                    h2 + 1.7F * g2,
                    h - 0.05F,
                    h + 0.05F,
                    f2 - 0.05F,
                    f2 + 0.05F,
                    2);
        }
    }

    static CrownMesh broadCollar(boolean bl, boolean bl2) {
        return new CrownMesh(new CrownMesh.Builder());
    }

    private static float lift(boolean bl) {
        return bl ? 1.15F : 0.2F;
    }

    private static void brow(CrownMesh.Builder builder, float f) {
        float g = f + 0.45F;
        float h = f - 0.3F;
        builder.box(-g, -6.6F, -g, g, -5.3F, -h, 0);
        builder.box(-g, -6.6F, h, g, -5.3F, g, 0);
        builder.box(-g, -6.6F, -h, -h, -5.3F, h, 0);
        builder.box(h, -6.6F, -h, g, -5.3F, h, 0);
        for (int i = -1; i <= 1; i += 2) {
            for (int j = 0; j < 2; j++) {
                float f2 = i * (1.7F + 1.6F * j);
                builder.box(
                        f2 - 0.45F,
                        -6.2999997F,
                        -g - 0.12F,
                        f2 + 0.45F,
                        -5.6000004F,
                        -g + 0.05F,
                        1);
            }
        }
    }

    private static void ring(float[] floats, float f, float g, float h, float f2, float g2) {
        set(floats, 0, g, f, f2);
        set(floats, 1, h, f, f2);
        set(floats, 2, h, f, g2);
        set(floats, 3, g, f, g2);
    }

    private static void set(float[] floats, int i, float f, float g, float h) {
        floats[i * 3] = f;
        floats[i * 3 + 1] = g;
        floats[i * 3 + 2] = h;
    }

    private static float lerp(float f, float g, float h) {
        return f + (g - f) * h;
    }

    @Environment(EnvType.CLIENT)
    static final class Builder {

        private float[] pos = new float[3072];

        private float[] normal = new float[768];

        private float[] sweep = new float[2048];

        private byte[] material = new byte[256];

        private float[] tone = new float[256];

        private int quads;

        private float shade = 1.0F;

        private final float[] corners = new float[24];

        Builder() {}

        void shade(float shade2) {
            this.shade = shade2;
        }

        private void ensure() {
            if (this.quads >= this.material.length) {
                int i = this.material.length * 2;
                this.pos = Arrays.copyOf(this.pos, i * 12);
                this.normal = Arrays.copyOf(this.normal, i * 3);
                this.sweep = Arrays.copyOf(this.sweep, i * 8);
                this.material = Arrays.copyOf(this.material, i);
                this.tone = Arrays.copyOf(this.tone, i);
            }
        }

        void quad(
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
                float h4,
                float f5,
                float g5,
                float h5,
                int i) {
            float f6 = g3 - g;
            float g6 = h4 - h2;
            float h6 = h3 - h;
            float f7 = g4 - g2;
            float g7 = f6 * g6 - h6 * f7;
            float h7 = f4 - f2;
            float f8 = f3 - f;
            float g8 = h6 * h7 - f8 * g6;
            float h8 = f8 * f7 - f6 * h7;
            float f9 = (float) Math.sqrt(g7 * g7 + g8 * g8 + h8 * h8);
            if (!(f9 < 1.0E-6F)) {
                float g9 = (f + f2 + f3 + f4) * 0.25F - f5;
                float h9 = (g + g2 + g3 + g4) * 0.25F - g5;
                float f10 = (h + h2 + h3 + h4) * 0.25F - h5;
                if ((g7 = g7 / f9) * g9 + (g8 = g8 / f9) * h9 + (h8 = h8 / f9) * f10 < 0.0F) {
                    g7 = -g7;
                    g8 = -g8;
                    h8 = -h8;
                }
                this.ensure();
                int j = this.quads * 12;
                this.vertex(j, f, g, h);
                this.vertex(j + 3, f2, g2, h2);
                this.vertex(j + 6, f3, g3, h3);
                this.vertex(j + 9, f4, g4, h4);
                this.normal[this.quads * 3] = g7;
                this.normal[this.quads * 3 + 1] = g8;
                this.normal[this.quads * 3 + 2] = h8;
                this.material[this.quads] = (byte) i;
                int k = this.quads * -1640531535;
                k ^= k >>> 15;
                this.tone[this.quads] = this.shade * (0.955F + 0.065F * ((k & 0xFF) / 255.0F));
                this.quads++;
            }
        }

        private void vertex(int i, float f, float g, float h) {
            this.pos[i] = f * 0.0625F;
            this.pos[i + 1] = g * 0.0625F;
            this.pos[i + 2] = h * 0.0625F;
            double d = Math.atan2(f, -h) + g * 0.12F;
            int j = this.quads * 8 + (i - this.quads * 12) / 3 * 2;
            this.sweep[j] = (float) Math.cos(d);
            this.sweep[j + 1] = (float) Math.sin(d);
        }

        void prism(float[] floats, int i) {
            float f = 0.0F;
            float g = 0.0F;
            float h = 0.0F;
            for (int j = 0; j < 8; j++) {
                f += floats[j * 3];
                g += floats[j * 3 + 1];
                h += floats[j * 3 + 2];
            }
            float f2;
            float g2;
            float h2;
            this.face(floats, 0, 1, 2, 3, f2 = f * 0.125F, g2 = g * 0.125F, h2 = h * 0.125F, i);
            this.face(floats, 4, 5, 6, 7, f2, g2, h2, i);
            this.face(floats, 0, 1, 5, 4, f2, g2, h2, i);
            this.face(floats, 1, 2, 6, 5, f2, g2, h2, i);
            this.face(floats, 2, 3, 7, 6, f2, g2, h2, i);
            this.face(floats, 3, 0, 4, 7, f2, g2, h2, i);
        }

        private void face(
                float[] floats, int i, int j, int k, int m, float f, float g, float h, int n) {
            this.quad(
                    floats[i * 3],
                    floats[i * 3 + 1],
                    floats[i * 3 + 2],
                    floats[j * 3],
                    floats[j * 3 + 1],
                    floats[j * 3 + 2],
                    floats[k * 3],
                    floats[k * 3 + 1],
                    floats[k * 3 + 2],
                    floats[m * 3],
                    floats[m * 3 + 1],
                    floats[m * 3 + 2],
                    f,
                    g,
                    h,
                    n);
        }

        void slab(
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
                int i) {
            float[] corners2 = this.corners;
            CrownMesh.set(corners2, 0, g, f, f2);
            CrownMesh.set(corners2, 1, h, f, f2);
            CrownMesh.set(corners2, 2, h, f, g2);
            CrownMesh.set(corners2, 3, g, f, g2);
            CrownMesh.set(corners2, 4, f3, h2, h3);
            CrownMesh.set(corners2, 5, g3, h2, h3);
            CrownMesh.set(corners2, 6, g3, h2, f4);
            CrownMesh.set(corners2, 7, f3, h2, f4);
            this.prism(corners2, i);
        }

        void box(float f, float g, float h, float f2, float g2, float h2, int i) {
            this.slab(g, f, f2, h, h2, g2, f, f2, h, h2, i);
        }

        void stripedSlab(
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
                int i,
                int j,
                int k) {
            float[] floats = new float[12];
            float[] floats2 = new float[12];
            CrownMesh.ring(floats, f, g, h, f2, g2);
            CrownMesh.ring(floats2, h2, f3, g3, h3, f4);
            this.striped(floats, floats2, i, j, k);
        }

        void striped(float[] floats, float[] floats2, int i, int j, int k) {
            float[] corners2 = this.corners;
            for (int m = 0; m < i; m++) {
                float f = (float) m / i;
                float g = (float) (m + 1) / i;
                for (int n = 0; n < 12; n++) {
                    corners2[n] = CrownMesh.lerp(floats[n], floats2[n], f);
                    corners2[12 + n] = CrownMesh.lerp(floats[n], floats2[n], g);
                }
                this.prism(corners2, (m & 1) == 0 ? j : k);
            }
        }

        void studs(
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
                float h4,
                boolean bl,
                boolean bl2,
                float f5,
                float g5,
                float h5) {
            int i = bl ? 3 : 2;
            for (int u = 0; u < i; u++) {
                float f6 = bl ? 0.2F + 0.3F * u : 0.35F + 0.3F * u;
                if (!bl2 || !bl || u != 1) {
                    this.stud(f, g, h, f2, g2, h2, f3, g3, h3, f4, g4, h4, f6, 0.5F, f5, g5, h5);
                }
            }
        }

        private void stud(
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
                float h4,
                float f5,
                float g5,
                float h5,
                float f6,
                float g6) {
            float h6 = CrownMesh.lerp(CrownMesh.lerp(f, f2, f5), CrownMesh.lerp(f4, f3, f5), g5);
            float f7 = CrownMesh.lerp(CrownMesh.lerp(g, g2, f5), CrownMesh.lerp(g4, g3, f5), g5);
            float g7 = CrownMesh.lerp(CrownMesh.lerp(h, h2, f5), CrownMesh.lerp(h4, h3, f5), g5);
            float h7 = f2 - f;
            float f8 = g2 - g;
            float g8 = h2 - h;
            float h8 = (float) Math.sqrt(h7 * h7 + f8 * f8 + g8 * g8);
            float f9 = f4 - f;
            float g9 = g4 - g;
            float h9 = h4 - h;
            float f10 = (float) Math.sqrt(f9 * f9 + g9 * g9 + h9 * h9);
            if (!(h8 < 1.0E-5F) && !(f10 < 1.0E-5F)) {
                float g10;
                float h10;
                float f11;
                float g11;
                float h11 = (g10 = f8 / h8) * (g11 = h9 / f10) - (h10 = g8 / h8) * (f11 = g9 / f10);
                float f12;
                float g12;
                float h12;
                if (!((h12 =
                                (float)
                                        Math.sqrt(
                                                h11 * h11
                                                        + (g12 =
                                                                        h10 * (f9 = f9 / f10)
                                                                                - (h7 = h7 / h8)
                                                                                        * g11)
                                                                * g12
                                                        + (f12 = h7 * f11 - g10 * f9) * f12))
                        < 1.0E-5F)) {
                    if ((h11 = h11 / h12) * (h6 - h5)
                                    + (g12 = g12 / h12) * (f7 - f6)
                                    + (f12 = f12 / h12) * (g7 - g6)
                            < 0.0F) {
                        h11 = -h11;
                        g12 = -g12;
                        f12 = -f12;
                    }
                    float f13 = 0.32F;
                    float g13 = 0.07F;
                    float h13;
                    float f14;
                    float g14;
                    this.quad(
                            (h13 = h6 + h11 * g13) - h7 * f13 - f9 * f13,
                            (f14 = f7 + g12 * g13) - g10 * f13 - f11 * f13,
                            (g14 = g7 + f12 * g13) - h10 * f13 - g11 * f13,
                            h13 + h7 * f13 - f9 * f13,
                            f14 + g10 * f13 - f11 * f13,
                            g14 + h10 * f13 - g11 * f13,
                            h13 + h7 * f13 + f9 * f13,
                            f14 + g10 * f13 + f11 * f13,
                            g14 + h10 * f13 + g11 * f13,
                            h13 - h7 * f13 + f9 * f13,
                            f14 - g10 * f13 + f11 * f13,
                            g14 - h10 * f13 + g11 * f13,
                            h13 - h11,
                            f14 - g12,
                            g14 - f12,
                            0);
                }
            }
        }

        void tube(float[] floats, float[] floats2, int i, int j) {
            float[] corners2 = this.corners;
            for (int k = 0; k + 1 < i; k++) {
                section(corners2, 0, floats, floats2, i, k);
                section(corners2, 4, floats, floats2, i, k + 1);
                this.prism(corners2, k + 2 == i ? 3 : j);
            }
        }

        private static void section(
                float[] floats, int i, float[] floats2, float[] floats3, int j, int k) {
            int m = Math.max(0, k - 1);
            int n = Math.min(j - 1, k + 1);
            float f = floats2[n * 3] - floats2[m * 3];
            float g;
            float h = (float) Math.sqrt(f * f + (g = floats2[n * 3 + 1] - floats2[m * 3 + 1]) * g);
            float f2 = h > 1.0E-5F ? -g / h : 1.0F;
            float g2 = h > 1.0E-5F ? f / h : 0.0F;
            float h2 = floats3[k];
            float f3 = floats2[k * 3];
            float g3 = floats2[k * 3 + 1];
            float h3 = floats2[k * 3 + 2];
            CrownMesh.set(floats, i, f3 + f2 * h2, g3 + g2 * h2, h3 - h2);
            CrownMesh.set(floats, i + 1, f3 + f2 * h2, g3 + g2 * h2, h3 + h2);
            CrownMesh.set(floats, i + 2, f3 - f2 * h2, g3 - g2 * h2, h3 + h2);
            CrownMesh.set(floats, i + 3, f3 - f2 * h2, g3 - g2 * h2, h3 - h2);
        }

        void disc(float f, float g, float h, float f2, float g2, int i, int j, int k) {
            float h2 = h - g2;
            float f3 = h + g2;
            for (int o = 0; o < i; o++) {
                double d = 6.283185307179586 * o / i;
                double e = 6.283185307179586 * (o + 1) / i;
                float g3 = f + f2 * (float) Math.cos(d);
                float h3 = g + f2 * (float) Math.sin(d);
                float f4 = f + f2 * (float) Math.cos(e);
                float g4 = g + f2 * (float) Math.sin(e);
                this.quad(f, g, h2, g3, h3, h2, f4, g4, h2, f4, g4, h2, f, g, h, j);
                this.quad(f, g, f3, g3, h3, f3, f4, g4, f3, f4, g4, f3, f, g, h, j);
                this.quad(g3, h3, h2, f4, g4, h2, f4, g4, f3, g3, h3, f3, f, g, h, k);
            }
        }

        void annulus(float f, float g, float h, float f2, float g2, int i, int j, float h2) {
            for (int o = 0; o < i; o++) {
                double d = 6.283185307179586 * o / i;
                double e = 6.283185307179586 * (o + 1) / i;
                float f3 = (float) Math.cos(d);
                float g3 = (float) Math.sin(d);
                float h3 = (float) Math.cos(e);
                float f4 = (float) Math.sin(e);
                this.quad(
                        f + f3 * f2,
                        g + g3 * f2,
                        h,
                        f + f3 * g2,
                        g + g3 * g2,
                        h,
                        f + h3 * g2,
                        g + f4 * g2,
                        h,
                        f + h3 * f2,
                        g + f4 * f2,
                        h,
                        f,
                        g,
                        h2,
                        j);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    record Look(
            int[] palette,
            boolean glow,
            float sweepCos,
            float sweepSin,
            float sweepGain,
            float gemPulse) {}
}
