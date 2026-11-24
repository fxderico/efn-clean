package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
final class AtmosphereStars {

    private static final float RADIUS = 100.0F;

    private static final int STARS = 2600;

    private static final float GLOW_ABOVE = 0.8F;

    private static final long TWINKLE_NANOS = 50000000L;

    private static final int BAND_COLUMNS = 180;

    private static final int BAND_ROWS = 12;

    private static final float BAND_HALF_WIDTH = 17.0F;

    private static final int NEBULA_COLUMNS = 72;

    private static final int NEBULA_ROWS = 36;

    private static final Vector3f BAND_NORMAL = new Vector3f(0.42F, 0.5F, 0.76F).normalize();

    private final SkyMesh starMesh = new SkyMesh("EFN stars", SkyMesh.GLOW, 5200);

    private final SkyMesh bandMesh = new SkyMesh("EFN milky way", SkyMesh.GLOW, 2160);

    private final SkyMesh nebulaMesh = new SkyMesh("EFN end nebulae", SkyMesh.GLOW, 2592);

    private final float[] dir = new float[7800];

    private final float[] size = new float[2600];

    private final float[] light = new float[2600];

    private final float[] roll = new float[2600];

    private final float[] phase = new float[2600];

    private final float[] speed = new float[2600];

    private final int[] rgb = new int[2600];

    private boolean placed;

    private long lastTwinkle;

    private float builtBrightness = -1.0F;

    private int bandTint;

    AtmosphereStars() {}

    void renderStars(MatrixStack matrices, float f, float builtBrightness2) {
        long l = System.nanoTime();
        if (l - this.lastTwinkle > 50000000L || builtBrightness2 != this.builtBrightness) {
            this.lastTwinkle = l;
            this.builtBrightness = builtBrightness2;
            this.buildStars((float) (l % 3600000000000L) / 1.0E9F, builtBrightness2);
        }
        this.starMesh.draw(matrices, f);
    }

    void renderBand(MatrixStack matrices, float f, int bandTint2) {
        if (!this.bandMesh.isBuilt() || bandTint2 != this.bandTint) {
            this.bandTint = bandTint2;
            this.buildBand(bandTint2);
        }
        this.bandMesh.draw(matrices, f);
    }

    void renderNebulae(MatrixStack matrices, float f) {
        if (!this.nebulaMesh.isBuilt()) {
            this.buildNebulae();
        }
        this.nebulaMesh.draw(matrices, f);
    }

    private void place() {
        Random random = Random.create(10842L);
        Vector3f vec = new Vector3f();
        Vector3f vec2 = new Vector3f();
        bandAxes(vec, vec2);
        for (int i = 0; i < 2600; i++) {
            float f;
            float g;
            float h;
            if (random.nextFloat() < 0.3F) {
                float f2 = random.nextFloat() * 6.2831855F;
                float g2 = (float) random.nextGaussian() * 9.0F * 0.017453292F;
                float h2 = MathHelper.cos(g2);
                float f3 = MathHelper.sin(g2);
                h =
                        h2 * (MathHelper.cos(f2) * vec.x + MathHelper.sin(f2) * vec2.x)
                                + f3 * BAND_NORMAL.x;
                g =
                        h2 * (MathHelper.cos(f2) * vec.y + MathHelper.sin(f2) * vec2.y)
                                + f3 * BAND_NORMAL.y;
                f =
                        h2 * (MathHelper.cos(f2) * vec.z + MathHelper.sin(f2) * vec2.z)
                                + f3 * BAND_NORMAL.z;
            } else {
                g = random.nextFloat() * 2.0F - 1.0F;
                float g3 = random.nextFloat() * 6.2831855F;
                float h3 = MathHelper.sqrt(1.0F - g * g);
                h = h3 * MathHelper.cos(g3);
                f = h3 * MathHelper.sin(g3);
            }
            this.dir[i * 3] = h;
            this.dir[i * 3 + 1] = g;
            this.dir[i * 3 + 2] = f;
            float f4 = (float) Math.pow(random.nextFloat(), 6.0);
            this.size[i] = 0.1F + 0.5F * f4;
            this.light[i] =
                    MathHelper.clamp(0.3F + 0.7F * f4 + 0.25F * random.nextFloat(), 0.0F, 1.0F);
            this.roll[i] = random.nextFloat() * 6.2831855F;
            this.phase[i] = random.nextFloat() * 6.2831855F;
            this.speed[i] = 1.5F + 3.5F * random.nextFloat();
            this.rgb[i] = starColour(random.nextFloat());
        }
        this.placed = true;
    }

    private static int starColour(float f) {
        if (f < 0.18F) {
            return ColorHelper.lerp(f / 0.18F, 16757870, 16774116);
        } else {
            return f < 0.75F ? 16775408 : ColorHelper.lerp((f - 0.75F) / 0.25F, 16774134, 11125503);
        }
    }

    private void buildStars(float f, float g) {
        if (!this.placed) {
            this.place();
        }
        BufferBuilder bufferBuilder = this.starMesh.begin();
        for (int i = 0; i < 2600; i++) {
            float h = 0.62F + 0.38F * MathHelper.sin(f * this.speed[i] + this.phase[i]);
            float f2 = MathHelper.clamp(this.light[i] * h * g, 0.0F, 1.0F);
            this.star(bufferBuilder, i, this.size[i], f2);
            if (this.light[i] > 0.8F) {
                this.star(bufferBuilder, i, this.size[i] * 4.0F, f2 * 0.12F);
            }
        }
        this.starMesh.upload(bufferBuilder);
    }

    private void star(BufferBuilder bufferBuilder, int i, float f, float g) {
        float h = this.dir[i * 3];
        float f2 = this.dir[i * 3 + 1];
        float g2 = this.dir[i * 3 + 2];
        float h2;
        float f3;
        float g3;
        if (Math.abs(f2) < 0.9F) {
            g3 = g2;
            f3 = 0.0F;
            h2 = -h;
        } else {
            g3 = 0.0F;
            f3 = -g2;
            h2 = f2;
        }
        float h3 = MathHelper.sqrt(g3 * g3 + f3 * f3 + h2 * h2);
        float f4;
        float g4;
        float h4 = f2 * (f4 = h2 / h3) - g2 * (g4 = f3 / h3);
        float f5;
        float g5 = g2 * (f5 = g3 / h3) - h * f4;
        float h5 = h * g4 - f2 * f5;
        float f6 = MathHelper.cos(this.roll[i]) * f;
        float g6 = MathHelper.sin(this.roll[i]) * f;
        float h6 = f5 * f6 + h4 * g6;
        float f7 = g4 * f6 + g5 * g6;
        float g7 = f4 * f6 + h5 * g6;
        float h7 = h4 * f6 - f5 * g6;
        float f8 = g5 * f6 - g4 * g6;
        float g8 = h5 * f6 - f4 * g6;
        float h8 = h * 100.0F;
        float f9 = f2 * 100.0F;
        float g9 = g2 * 100.0F;
        int j = Math.round(g * 255.0F) << 24 | this.rgb[i];
        SkyMesh.vertex(bufferBuilder, h8 - h6 - h7, f9 - f7 - f8, g9 - g7 - g8, j);
        SkyMesh.vertex(bufferBuilder, h8 + h6 - h7, f9 + f7 - f8, g9 + g7 - g8, j);
        SkyMesh.vertex(bufferBuilder, h8 + h6 + h7, f9 + f7 + f8, g9 + g7 + g8, j);
        SkyMesh.vertex(bufferBuilder, h8 - h6 + h7, f9 - f7 + f8, g9 - g7 + g8, j);
    }

    private static void bandAxes(Vector3f vec, Vector3f vec2) {
        new Vector3f(0.0F, 1.0F, 0.0F).cross(BAND_NORMAL, vec).normalize();
        new Vector3f(BAND_NORMAL).cross(vec, vec2).normalize();
    }

    private void buildBand(int i) {
        Vector3f vec = new Vector3f();
        Vector3f vec2 = new Vector3f();
        bandAxes(vec, vec2);
        float[][] floatGrid = new float[181][13];
        float[][] floatGrid2 = new float[181][13];
        float[][] floatGrid3 = new float[181][13];
        int[][] intGrid = new int[181][13];
        for (int j = 0; j <= 180; j++) {
            float f = j / 180.0F * 6.2831855F;
            float g = MathHelper.cos(f) * vec.x + MathHelper.sin(f) * vec2.x;
            float h = MathHelper.cos(f) * vec.y + MathHelper.sin(f) * vec2.y;
            float f2 = MathHelper.cos(f) * vec.z + MathHelper.sin(f) * vec2.z;
            float g2 = 0.55F + 0.45F * fbm(g * 1.7F, h * 1.7F, f2 * 1.7F);
            float h2 = 3.0F * (fbm(g * 2.3F + 5.0F, h * 2.3F, f2 * 2.3F) - 0.5F);
            float f3 =
                    MathHelper.clamp(
                            fbm(g * 1.4F, h * 1.4F + 9.0F, f2 * 1.4F) * 1.6F - 0.35F, 0.0F, 1.0F);
            for (int k = 0; k <= 12; k++) {
                float g3 = (k / 12.0F * 2.0F - 1.0F) * 17.0F;
                float h3 = g3 * 0.017453292F;
                float f4 = MathHelper.cos(h3);
                float g4 = MathHelper.sin(h3);
                float h4 = f4 * g + g4 * BAND_NORMAL.x;
                float f5 = f4 * h + g4 * BAND_NORMAL.y;
                float g5 = f4 * f2 + g4 * BAND_NORMAL.z;
                floatGrid[j][k] = h4 * 100.0F;
                floatGrid2[j][k] = f5 * 100.0F;
                floatGrid3[j][k] = g5 * 100.0F;
                float h5 = g3 / (17.0F * g2);
                float f6 = (float) Math.exp(-h5 * h5 * 2.2F);
                float g6 = 0.35F + 0.65F * fbm(h4 * 6.0F, f5 * 6.0F, g5 * 6.0F);
                float h6 = 1.0F - 0.75F * f3 * (float) Math.exp(-sq((g3 - h2) / 2.6F));
                float f7 = MathHelper.clamp(f6 * g6 * h6, 0.0F, 1.0F);
                int m = ColorHelper.lerp(f6, 7306440, i) & 16777215;
                intGrid[j][k] = Math.round(f7 * 0.42F * 255.0F) << 24 | m;
            }
        }
        BufferBuilder bufferBuilder = this.bandMesh.begin();
        for (int n = 0; n < 180; n++) {
            for (int o = 0; o < 12; o++) {
                SkyMesh.vertex(
                        bufferBuilder,
                        floatGrid[n][o],
                        floatGrid2[n][o],
                        floatGrid3[n][o],
                        intGrid[n][o]);
                SkyMesh.vertex(
                        bufferBuilder,
                        floatGrid[n + 1][o],
                        floatGrid2[n + 1][o],
                        floatGrid3[n + 1][o],
                        intGrid[n + 1][o]);
                SkyMesh.vertex(
                        bufferBuilder,
                        floatGrid[n + 1][o + 1],
                        floatGrid2[n + 1][o + 1],
                        floatGrid3[n + 1][o + 1],
                        intGrid[n + 1][o + 1]);
                SkyMesh.vertex(
                        bufferBuilder,
                        floatGrid[n][o + 1],
                        floatGrid2[n][o + 1],
                        floatGrid3[n][o + 1],
                        intGrid[n][o + 1]);
            }
        }
        this.bandMesh.upload(bufferBuilder);
    }

    private void buildNebulae() {
        float[][] floatGrid = new float[73][37];
        float[][] floatGrid2 = new float[73][37];
        float[][] floatGrid3 = new float[73][37];
        int[][] intGrid = new int[73][37];
        for (int i = 0; i <= 72; i++) {
            float f = i / 72.0F * 6.2831855F;
            for (int j = 0; j <= 36; j++) {
                float g = (j / 36.0F - 0.5F) * 3.1415927F;
                float h = MathHelper.cos(g) * MathHelper.cos(f);
                float f2 = MathHelper.sin(g);
                float g2 = MathHelper.cos(g) * MathHelper.sin(f);
                floatGrid[i][j] = h * 100.0F;
                floatGrid2[i][j] = f2 * 100.0F;
                floatGrid3[i][j] = g2 * 100.0F;
                float h2 =
                        AtmosphereAurora.smoothstep(
                                (fbm(h * 2.2F, f2 * 2.2F, g2 * 2.2F) - 0.42F) / 0.35F);
                float fbm = fbm(h * 1.3F + 11.0F, f2 * 1.3F, g2 * 1.3F);
                int k =
                        fbm < 0.5F
                                ? ColorHelper.lerp(fbm * 2.0F, 8011007, 16732104)
                                : ColorHelper.lerp((fbm - 0.5F) * 2.0F, 16732104, 3989712);
                intGrid[i][j] = Math.round(h2 * 0.55F * 255.0F) << 24 | k & 16777215;
            }
        }
        BufferBuilder bufferBuilder = this.nebulaMesh.begin();
        for (int m = 0; m < 72; m++) {
            for (int n = 0; n < 36; n++) {
                SkyMesh.vertex(
                        bufferBuilder,
                        floatGrid[m][n],
                        floatGrid2[m][n],
                        floatGrid3[m][n],
                        intGrid[m][n]);
                SkyMesh.vertex(
                        bufferBuilder,
                        floatGrid[m + 1][n],
                        floatGrid2[m + 1][n],
                        floatGrid3[m + 1][n],
                        intGrid[m + 1][n]);
                SkyMesh.vertex(
                        bufferBuilder,
                        floatGrid[m + 1][n + 1],
                        floatGrid2[m + 1][n + 1],
                        floatGrid3[m + 1][n + 1],
                        intGrid[m + 1][n + 1]);
                SkyMesh.vertex(
                        bufferBuilder,
                        floatGrid[m][n + 1],
                        floatGrid2[m][n + 1],
                        floatGrid3[m][n + 1],
                        intGrid[m][n + 1]);
            }
        }
        this.nebulaMesh.upload(bufferBuilder);
    }

    static float fbm(float f, float g, float h) {
        float f2 = 0.0F;
        float g2 = 0.5F;
        float h2 = 0.0F;
        for (int i = 0; i < 4; i++) {
            f2 += g2 * noise(f, g, h);
            h2 += g2;
            f *= 2.03F;
            g *= 2.03F;
            h *= 2.03F;
            g2 *= 0.5F;
        }
        return f2 / h2;
    }

    private static float noise(float f, float g, float h) {
        int i = MathHelper.floor(f);
        int j = MathHelper.floor(g);
        int k = MathHelper.floor(h);
        float f2 = f - i;
        float g2 = g - j;
        float h2 = h - k;
        f2 = f2 * f2 * (3.0F - 2.0F * f2);
        g2 = g2 * g2 * (3.0F - 2.0F * g2);
        h2 = h2 * h2 * (3.0F - 2.0F * h2);
        float f3 = MathHelper.lerp(f2, hash(i, j, k), hash(i + 1, j, k));
        float g3 = MathHelper.lerp(f2, hash(i, j + 1, k), hash(i + 1, j + 1, k));
        float h3 = MathHelper.lerp(f2, hash(i, j, k + 1), hash(i + 1, j, k + 1));
        float f4 = MathHelper.lerp(f2, hash(i, j + 1, k + 1), hash(i + 1, j + 1, k + 1));
        return MathHelper.lerp(h2, MathHelper.lerp(g2, f3, g3), MathHelper.lerp(g2, h3, f4));
    }

    private static float hash(int i, int j, int k) {
        int m = i * 374761393 + j * 668265263 + k * 1274126177;
        m = (m ^ m >>> 13) * 1274126177;
        return ((m ^ m >>> 16) & 16777215) / 1.6777215E7F;
    }

    private static float sq(float f) {
        return f * f;
    }

    void close() {
        this.starMesh.close();
        this.bandMesh.close();
        this.nebulaMesh.close();
        this.builtBrightness = -1.0F;
    }
}
