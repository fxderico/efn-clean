package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
final class KnifeMesh {

    private static final float SHINE_WIDTH = 2.6F;

    private static final float SHINE_BOOST = 1.5F;

    private static final float WIPE_FRONT = 1.5F;

    private static final float GLOW_ALPHA = 0.85F;

    private static final float GLINT_UV = 0.09F;

    private final float[] pos;

    private final float[] normal;

    private final byte[] material;

    private final float[] tone;

    private final int quads;

    private final float shineFrom;

    private final float shineTo;

    private KnifeMesh(
            float[] pos2,
            float[] normal2,
            byte[] material2,
            float[] tone2,
            int quads2,
            float shineFrom2,
            float shineTo2) {
        this.pos = pos2;
        this.normal = normal2;
        this.material = material2;
        this.tone = tone2;
        this.quads = quads2;
        this.shineFrom = shineFrom2;
        this.shineTo = shineTo2;
    }

    void emit(
            Entry entry, VertexConsumer vertexConsumer, int[] ints, int i, KnifeMesh.Shine shine) {
        boolean bl = shine.gain() > 0.004F;
        for (int j = 0; j < this.quads; j++) {
            byte b = this.material[j];
            int k = ints[b];
            boolean bl2 =
                    bl
                            && (shine.tail() > 0.0F
                                    ? KnifeMaterial.blade(b)
                                    : KnifeMaterial.metallic(b));
            float f = this.normal[j * 3];
            float g = this.normal[j * 3 + 1];
            float h = this.normal[j * 3 + 2];
            for (int m = 0; m < 4; m++) {
                int n = j * 12 + m * 3;
                float f2 = this.pos[n];
                float g2 = this.pos[n + 1];
                float h2 = this.pos[n + 2];
                float f3 = this.tone[j];
                if (bl2) {
                    f3 += shine.gain() * 1.5F * this.profile(f2, shine);
                }
                vertexConsumer
                        .vertex(entry, f2, g2, h2)
                        .color(tint(k, f3))
                        .texture((f2 + h2) * 0.09F, g2 * 0.09F)
                        .overlay(OverlayTexture.DEFAULT_UV)
                        .light(i)
                        .normal(entry, f, g, h);
            }
        }
    }

    void emitGlow(Entry entry, VertexConsumer vertexConsumer, int i, KnifeMesh.Shine shine) {
        int j = i >> 16 & 0xFF;
        int k = i >> 8 & 0xFF;
        int m = i & 0xFF;
        float[] floats = new float[4];
        for (int n = 0; n < this.quads; n++) {
            if (KnifeMaterial.blade(this.material[n])) {
                float f = 0.0F;
                for (int o = 0; o < 4; o++) {
                    floats[o] =
                            Math.min(
                                    1.0F,
                                    shine.gain()
                                            * 0.85F
                                            * this.profile(this.pos[n * 12 + o * 3], shine));
                    f = Math.max(f, floats[o]);
                }
                if (!(f < 0.01F)) {
                    float g = this.normal[n * 3];
                    float h = this.normal[n * 3 + 1];
                    float f2 = this.normal[n * 3 + 2];
                    for (int p = 0; p < 4; p++) {
                        int q = n * 12 + p * 3;
                        vertexConsumer
                                .vertex(entry, this.pos[q], this.pos[q + 1], this.pos[q + 2])
                                .color(j, k, m, Math.round(floats[p] * 255.0F))
                                .texture(0.5F, 0.5F)
                                .overlay(OverlayTexture.DEFAULT_UV)
                                .light(15728880)
                                .normal(entry, g, h, f2);
                    }
                }
            }
        }
    }

    private float profile(float f, KnifeMesh.Shine shine) {
        float g = f - (this.shineFrom + (this.shineTo - this.shineFrom) * shine.at());
        if (shine.tail() <= 0.0F) {
            float h = g / 2.6F;
            return (float) Math.exp(-h * h);
        } else if (g > 0.0F) {
            float f2 = g / 1.5F;
            return (float) Math.exp(-f2 * f2);
        } else {
            return (float) Math.exp(g / shine.tail());
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

    @Environment(EnvType.CLIENT)
    record Shine(float at, float gain, float tail) {

        static final KnifeMesh.Shine NONE = new KnifeMesh.Shine(0.0F, 0.0F, 0.0F);
    }

    @Environment(EnvType.CLIENT)
    static final class Voxels {

        private static final int SX = 128;

        private static final int SY = 64;

        private static final int SZ = 16;

        private static final int OX = 64;

        private static final int OY = 32;

        private static final int OZ = 8;

        private static final int[][] FACES =
                new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

        private final byte[] cells = new byte[131072];

        private final float[] shade = new float[131072];

        Voxels() {}

        void set(int i, int j, int k, int m) {
            this.set(i, j, k, m, 1.0F);
        }

        void set(int i, int j, int k, int m, float f) {
            int index = index(i, j, k);
            this.cells[index] = (byte) (m + 1);
            this.shade[index] = f;
        }

        void clear(int i, int j, int k) {
            this.cells[index(i, j, k)] = 0;
        }

        boolean filled(int i, int j, int k) {
            int m = i + 64;
            int n = j + 32;
            int o = k + 8;
            return m >= 0 && n >= 0 && o >= 0 && m < 128 && n < 64 && o < 16
                    ? this.cells[(m * 64 + n) * 16 + o] != 0
                    : false;
        }

        KnifeMesh mesh(float f, float g) {
            int i = 256;
            float[] floats = new float[i * 12];
            float[] floats2 = new float[i * 3];
            byte[] bytes = new byte[i];
            float[] floats3 = new float[i];
            int j = 0;
            for (int k = 0; k < 128; k++) {
                for (int m = 0; m < 64; m++) {
                    for (int n = 0; n < 16; n++) {
                        int o = (k * 64 + m) * 16 + n;
                        if (this.cells[o] != 0) {
                            int p = k - 64;
                            int q = m - 32;
                            int r = n - 8;
                            for (int[] ints : FACES) {
                                if (!this.filled(p + ints[0], q + ints[1], r + ints[2])) {
                                    if (j == i) {
                                        floats = Arrays.copyOf(floats, (i *= 2) * 12);
                                        floats2 = Arrays.copyOf(floats2, i * 3);
                                        bytes = Arrays.copyOf(bytes, i);
                                        floats3 = Arrays.copyOf(floats3, i);
                                    }
                                    face(floats, j * 12, p, q, r, ints);
                                    floats2[j * 3] = ints[0];
                                    floats2[j * 3 + 1] = ints[1];
                                    floats2[j * 3 + 2] = ints[2];
                                    bytes[j] = (byte) (this.cells[o] - 1);
                                    floats3[j] = this.shade[o];
                                    j++;
                                }
                            }
                        }
                    }
                }
            }
            return new KnifeMesh(
                    Arrays.copyOf(floats, j * 12),
                    Arrays.copyOf(floats2, j * 3),
                    Arrays.copyOf(bytes, j),
                    Arrays.copyOf(floats3, j),
                    j,
                    f,
                    g);
        }

        private static void face(float[] floats, int i, int j, int k, int m, int[] ints) {
            float f = j;
            float g = j + 1;
            float h = k;
            float f2 = k + 1;
            float g2 = m;
            float h2 = m + 1;
            float[] floats2 =
                    ints[0] == 1
                            ? new float[] {g, h, h2, g, h, g2, g, f2, g2, g, f2, h2}
                            : (ints[0] == -1
                                    ? new float[] {f, h, g2, f, h, h2, f, f2, h2, f, f2, g2}
                                    : (ints[1] == 1
                                            ? new float[] {
                                                f, f2, h2, g, f2, h2, g, f2, g2, f, f2, g2
                                            }
                                            : (ints[1] == -1
                                                    ? new float[] {
                                                        f, h, g2, g, h, g2, g, h, h2, f, h, h2
                                                    }
                                                    : (ints[2] == 1
                                                            ? new float[] {
                                                                f, h, h2, g, h, h2, g, f2, h2, f,
                                                                f2, h2
                                                            }
                                                            : new float[] {
                                                                g, h, g2, f, h, g2, f, f2, g2, g,
                                                                f2, g2
                                                            }))));
            System.arraycopy(floats2, 0, floats, i, 12);
        }

        private static int index(int i, int j, int k) {
            return ((i + 64) * 64 + j + 32) * 16 + k + 8;
        }
    }
}
