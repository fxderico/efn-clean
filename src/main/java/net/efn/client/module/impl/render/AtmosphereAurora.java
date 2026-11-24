package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class AtmosphereAurora {

    private static final float RADIUS = 100.0F;

    private static final int COLUMNS = 64;

    private static final int ROWS = 6;

    private static final float[][] CURTAINS =
            new float[][] {
                {-40.0F, 22.0F, 78.0F, 18.0F},
                {6.0F, 28.0F, 100.0F, 23.0F},
                {54.0F, 20.0F, 70.0F, 16.0F}
            };

    private final SkyMesh mesh = new SkyMesh("EFN aurora", SkyMesh.GLOW, CURTAINS.length * 64 * 6);

    private final float[] x = new float[455];

    private final float[] y = new float[this.x.length];

    private final float[] z = new float[this.x.length];

    private final int[] color = new int[this.x.length];

    AtmosphereAurora() {}

    void render(MatrixStack matrices, float f, float g, int i, int j) {
        BufferBuilder bufferBuilder = this.mesh.begin();
        for (int k = 0; k < CURTAINS.length; k++) {
            this.curtain(bufferBuilder, CURTAINS[k], k, g, i, j);
        }
        this.mesh.upload(bufferBuilder);
        this.mesh.draw(matrices, f);
    }

    private void curtain(
            BufferBuilder bufferBuilder, float[] floats, int i, float f, int j, int k) {
        float g = floats[0];
        float h = floats[1];
        float f2 = floats[2];
        float g2 = floats[3];
        for (int m = 0; m <= 64; m++) {
            float h2 = m / 64.0F;
            float f3 = g + (h2 - 0.5F) * f2 + 3.0F * wave(h2 * 2.1F + f * 0.05F + i);
            float g3 =
                    h
                            + 5.0F * MathHelper.sin(3.1415927F * h2)
                            + 3.0F * wave(h2 * 3.7F - f * 0.07F + i * 2.0F);
            float h3 = smoothstep(h2 / 0.25F) * smoothstep((1.0F - h2) / 0.25F);
            float f4 = h3 * (0.55F + 0.45F * wave01(h2 * 5.3F + f * 0.11F + i * 1.3F));
            float g4 = 0.72F + 0.28F * wave01(h2 * 23.0F + f * 0.35F + i);
            float h4 = g2 * (0.8F + 0.35F * wave01(h2 * 4.1F + f * 0.06F + i * 3.0F));
            float f5 = MathHelper.cos(f3 * 0.017453292F);
            float g5 = MathHelper.sin(f3 * 0.017453292F);
            for (int n = 0; n <= 6; n++) {
                float h5 = n / 6.0F;
                float f6 = (g3 + h5 * h4) * 0.017453292F;
                float g6 = MathHelper.cos(f6);
                int o = m * 7 + n;
                this.x[o] = 100.0F * g6 * g5;
                this.y[o] = 100.0F * MathHelper.sin(f6);
                this.z[o] = -100.0F * g6 * f5;
                float h6 =
                        h5 < 0.12F
                                ? smoothstep(h5 / 0.12F)
                                : (float) Math.pow(1.0F - (h5 - 0.12F) / 0.88F, 1.5);
                float f7 = MathHelper.clamp(f4 * g4 * h6 * 0.8F, 0.0F, 1.0F);
                int p = ColorHelper.lerp((float) Math.pow(h5, 0.7), j, k) & 16777215;
                this.color[o] = Math.round(f7 * 255.0F) << 24 | p;
            }
        }
        for (int q = 0; q < 64; q++) {
            for (int r = 0; r < 6; r++) {
                int t = q * 7 + r;
                int u = (q + 1) * 7 + r;
                this.vertex(bufferBuilder, t);
                this.vertex(bufferBuilder, u);
                this.vertex(bufferBuilder, u + 1);
                this.vertex(bufferBuilder, t + 1);
            }
        }
    }

    private void vertex(BufferBuilder bufferBuilder, int i) {
        SkyMesh.vertex(bufferBuilder, this.x[i], this.y[i], this.z[i], this.color[i]);
    }

    static float wave(float f) {
        return 0.5F * MathHelper.sin(f)
                + 0.3F * MathHelper.sin(f * 2.13F + 1.3F)
                + 0.2F * MathHelper.sin(f * 4.37F + 2.9F);
    }

    static float wave01(float f) {
        return 0.5F + 0.5F * wave(f);
    }

    static float smoothstep(float f) {
        f = MathHelper.clamp(f, 0.0F, 1.0F);
        return f * f * (3.0F - 2.0F * f);
    }

    void close() {
        this.mesh.close();
    }
}
