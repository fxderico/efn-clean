package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class SlashTrail {

    private static final int CAPACITY = 96;

    private static final long GAP_NANOS = 100000000L;

    private static final float SLOW = 0.6F;

    private static final float FAST = 2.8F;

    private static final float ALPHA = 0.6F;

    private static final float BASE_ALPHA = 0.12F;

    private final float[] points = new float[576];

    private final long[] times = new long[96];

    private int newest = -1;

    private int size;

    SlashTrail() {}

    void add(long l, float[] floats, int i) {
        this.newest = (this.newest + 1) % 96;
        System.arraycopy(floats, i, this.points, this.newest * 6, 6);
        this.times[this.newest] = l;
        this.size = Math.min(this.size + 1, 96);
    }

    void clear() {
        this.size = 0;
        this.newest = -1;
    }

    boolean isEmpty(long l, long l2) {
        return this.size == 0 || l - this.times[this.newest] > l2;
    }

    void emit(Entry entry, VertexConsumer vertexConsumer, long l, long l2, int i, float f) {
        int j = i >> 16 & 0xFF;
        int k = i >> 8 & 0xFF;
        int m = i & 0xFF;
        for (int o = 0; o + 1 < this.size; o++) {
            int n = Math.floorMod(this.newest - o, 96);
            int p = Math.floorMod(this.newest - o - 1, 96);
            long l3 = l - this.times[n];
            long l4 = l - this.times[p];
            if (l4 > l2) {
                break;
            }
            long l5 = this.times[n] - this.times[p];
            float g;
            float h;
            int q;
            int r;
            float f2;
            float g2;
            if (l5 > 0L
                    && l5 <= 100000000L
                    && !((g2 =
                                    MathHelper.clamp(
                                            (MathHelper.sqrt(
                                                                            (f2 =
                                                                                                    this
                                                                                                                    .points[
                                                                                                                    (r =
                                                                                                                                    n
                                                                                                                                            * 6)
                                                                                                                            + 3]
                                                                                                            - this
                                                                                                                    .points[
                                                                                                                    (q =
                                                                                                                                    p
                                                                                                                                            * 6)
                                                                                                                            + 3])
                                                                                            * f2
                                                                                    + (h =
                                                                                                    this
                                                                                                                    .points[
                                                                                                                    r
                                                                                                                            + 4]
                                                                                                            - this
                                                                                                                    .points[
                                                                                                                    q
                                                                                                                            + 4])
                                                                                            * h
                                                                                    + (g =
                                                                                                    this
                                                                                                                    .points[
                                                                                                                    r
                                                                                                                            + 5]
                                                                                                            - this
                                                                                                                    .points[
                                                                                                                    q
                                                                                                                            + 5])
                                                                                            * g)
                                                                    / ((float) l5 / 1.0E9F)
                                                            - 0.6F)
                                                    / 2.1999998F,
                                            0.0F,
                                            1.0F))
                            <= 0.0F)) {
                float h2 = fade(l3, l2) * g2 * f;
                float f3 = fade(l4, l2) * g2 * f;
                float[] floats =
                        new float[] {
                            this.points[q],
                            this.points[q + 1],
                            this.points[q + 2],
                            f3 * 0.12F,
                            this.points[q + 3],
                            this.points[q + 4],
                            this.points[q + 5],
                            f3,
                            this.points[r + 3],
                            this.points[r + 4],
                            this.points[r + 5],
                            h2,
                            this.points[r],
                            this.points[r + 1],
                            this.points[r + 2],
                            h2 * 0.12F
                        };
                for (int u = 0; u < 4; u++) {
                    vertex(entry, vertexConsumer, floats, u, j, k, m);
                }
                for (int v = 3; v >= 0; v--) {
                    vertex(entry, vertexConsumer, floats, v, j, k, m);
                }
            }
        }
    }

    private static float fade(long l, long l2) {
        float f = 1.0F - MathHelper.clamp((float) l / (float) l2, 0.0F, 1.0F);
        return f * f * 0.6F;
    }

    private static void vertex(
            Entry entry,
            VertexConsumer vertexConsumer,
            float[] floats,
            int i,
            int j,
            int k,
            int m) {
        int n = i * 4;
        vertexConsumer
                .vertex(entry, floats[n], floats[n + 1], floats[n + 2])
                .color(j, k, m, Math.round(MathHelper.clamp(floats[n + 3], 0.0F, 1.0F) * 255.0F))
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880)
                .normal(entry, 0.0F, 0.0F, 1.0F);
    }

    static void sparkle(
            Entry entry,
            VertexConsumer vertexConsumer,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            int i,
            float h2) {
        int j = i >> 16 & 0xFF;
        int k = i >> 8 & 0xFF;
        int m = i & 0xFF;
        int n = Math.round(MathHelper.clamp(h2, 0.0F, 1.0F) * 255.0F);
        for (int q = 0; q < 4; q++) {
            float f3 = g2 + q * 1.5707964F;
            float g3 = MathHelper.cos(f3);
            float h3 = MathHelper.sin(f3);
            float f4 = f2 * (q % 2 == 0 ? 1.0F : 0.7F);
            float g4 = f2 * 0.16F;
            float h4 = f + g3 * f4;
            float f5 = g + h3 * f4;
            float g5 = f - h3 * g4;
            float h5 = g + g3 * g4;
            float f6 = f + h3 * g4;
            float g6 = g - g3 * g4;
            star(entry, vertexConsumer, f, g, h, j, k, m, n);
            star(entry, vertexConsumer, g5, h5, h, j, k, m, n / 3);
            star(entry, vertexConsumer, h4, f5, h, j, k, m, 0);
            star(entry, vertexConsumer, f6, g6, h, j, k, m, n / 3);
            star(entry, vertexConsumer, f6, g6, h, j, k, m, n / 3);
            star(entry, vertexConsumer, h4, f5, h, j, k, m, 0);
            star(entry, vertexConsumer, g5, h5, h, j, k, m, n / 3);
            star(entry, vertexConsumer, f, g, h, j, k, m, n);
        }
    }

    private static void star(
            Entry entry,
            VertexConsumer vertexConsumer,
            float f,
            float g,
            float h,
            int i,
            int j,
            int k,
            int m) {
        vertexConsumer
                .vertex(entry, f, g, h)
                .color(i, j, k, m)
                .texture(0.5F, 0.5F)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880)
                .normal(entry, 0.0F, 0.0F, 1.0F);
    }
}
