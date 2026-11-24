package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
final class AtmosphereSky {

    private static final float RADIUS = 100.0F;

    private static final float[] RINGS =
            new float[] {
                -30.0F, -12.0F, -4.0F, 0.0F, 4.0F, 9.0F, 16.0F, 26.0F, 40.0F, 58.0F, 76.0F, 90.0F
            };

    private static final int SEGMENTS = 48;

    private static final int HALO_RINGS = 4;

    private static final int HALO_SEGMENTS = 32;

    private static final int METEOR_SEGMENTS = 10;

    private static final float[] KEYS = new float[] {-0.35F, -0.08F, 0.06F, 0.3F};

    private static final int GOLDEN_ZENITH = 5210048;

    private static final int GOLDEN_HORIZON = 16032864;

    private static final int GOLDEN_GLOW = 16747069;

    private static final int DAY_ZENITH = 3108822;

    private static final int DAY_HORIZON = 11850996;

    private static final int DAY_GLOW = 16774362;

    private static final int BLUE_HOUR_ZENITH = 1319497;

    private final SkyMesh dome =
            new SkyMesh("EFN sky dome", SkyMesh.PAINT, (RINGS.length - 1) * 48);

    private final SkyMesh endDome =
            new SkyMesh("EFN end sky", SkyMesh.PAINT, (RINGS.length - 1) * 48);

    private final SkyMesh halo = new SkyMesh("EFN sun and moon glow", SkyMesh.GLOW, 256);

    private final SkyMesh meteorMesh = new SkyMesh("EFN shooting stars", SkyMesh.GLOW, 12);

    private final Random random = Random.create();

    private static final float DOME_SUN_STEP = 0.002F;

    private static final float DOME_STRENGTH_STEP = 0.004F;

    private int zenith;

    private int horizon;

    private int glow;

    private float glowStrength;

    private int builtZenith;

    private int builtHorizon;

    private int builtGlow;

    private float builtStrength;

    private float builtSunX = 0.0F / 0.0F;

    private float builtSunY = 0.0F / 0.0F;

    private float sunX;

    private float sunY;

    private float sunAngle;

    private float moonAngle;

    private boolean ready;

    private long meteorStart;

    private long meteorEnd;

    private long nextMeteor;

    private float mx;

    private float my;

    private float mz;

    private float tx;

    private float ty;

    private float tz;

    AtmosphereSky() {}

    void update(float sunAngle2, float moonAngle2, float f, float g, float h, int i, int j, int k) {
        this.sunAngle = sunAngle2;
        this.moonAngle = moonAngle2;
        this.sunX = -MathHelper.sin(sunAngle2);
        this.sunY = MathHelper.cos(sunAngle2);
        int m = ColorHelper.lerp(0.3F, 1319497, i);
        int[] ints = new int[] {i, m, 5210048, 3108822};
        int[] ints2 = new int[] {j, scale(k, 0.75F), 16032864, 11850996};
        int[] ints3 = new int[] {j, k, 16747069, 16774362};
        float[] floats = new float[] {0.0F, 0.55F, 1.0F, 0.25F};
        int n = 0;
        while (n < KEYS.length - 2 && this.sunY > KEYS[n + 1]) {
            n++;
        }
        float f2 = AtmosphereAurora.smoothstep((this.sunY - KEYS[n]) / (KEYS[n + 1] - KEYS[n]));
        this.zenith = ColorHelper.lerp(f2, ints[n], ints[n + 1]);
        this.horizon = ColorHelper.lerp(f2, ints2[n], ints2[n + 1]);
        this.glow = ColorHelper.lerp(f2, ints3[n], ints3[n + 1]);
        this.glowStrength = MathHelper.lerp(f2, floats[n], floats[n + 1]) * (1.0F - f);
        this.zenith = weather(this.zenith, f, g, h);
        this.horizon = weather(this.horizon, f, g, h);
        this.ready = true;
    }

    private static int weather(int i, float f, float g, float h) {
        int scale = scale(luma(i), 0.85F);
        i = ColorHelper.lerp(f * 0.65F, i, scale);
        i = scale(i, 1.0F - 0.35F * f - 0.25F * g);
        return h > 0.0F ? ColorHelper.lerp(h * 0.45F, i, 15265023) : i;
    }

    boolean ready() {
        return this.ready;
    }

    int horizonToward(float f, float g) {
        float h = MathHelper.sqrt(f * f + g * g);
        return h < 1.0E-4F ? this.horizon : this.colourAt(f / h, 0.02F, g / h);
    }

    float golden() {
        return 1.0F - Math.min(1.0F, Math.abs(this.sunY - 0.06F) / 0.22F);
    }

    float nightness() {
        return AtmosphereAurora.smoothstep((0.05F - this.sunY) / 0.4F);
    }

    int twilightGlow() {
        return this.glow;
    }

    void renderDome(MatrixStack matrices) {
        if (!this.dome.isBuilt() || this.domeStale()) {
            BufferBuilder bufferBuilder = this.dome.begin();
            this.domeQuads(bufferBuilder, false);
            this.dome.upload(bufferBuilder);
            this.builtZenith = this.zenith;
            this.builtHorizon = this.horizon;
            this.builtGlow = this.glow;
            this.builtStrength = this.glowStrength;
            this.builtSunX = this.sunX;
            this.builtSunY = this.sunY;
        }
        this.dome.draw(matrices, 1.0F);
    }

    private boolean domeStale() {
        return this.zenith != this.builtZenith
                || this.horizon != this.builtHorizon
                || this.glow != this.builtGlow
                || Math.abs(this.glowStrength - this.builtStrength) > 0.004F
                || Math.abs(this.sunX - this.builtSunX) > 0.002F
                || Math.abs(this.sunY - this.builtSunY) > 0.002F;
    }

    void renderEndDome(MatrixStack matrices) {
        if (!this.endDome.isBuilt()) {
            BufferBuilder bufferBuilder = this.endDome.begin();
            this.domeQuads(bufferBuilder, true);
            this.endDome.upload(bufferBuilder);
        }
        this.endDome.draw(matrices, 1.0F);
    }

    private void domeQuads(BufferBuilder bufferBuilder, boolean bl) {
        for (int i = 0; i < RINGS.length - 1; i++) {
            for (int j = 0; j < 48; j++) {
                this.corner(bufferBuilder, RINGS[i], j, bl);
                this.corner(bufferBuilder, RINGS[i], j + 1, bl);
                this.corner(bufferBuilder, RINGS[i + 1], j + 1, bl);
                this.corner(bufferBuilder, RINGS[i + 1], j, bl);
            }
        }
    }

    private void corner(BufferBuilder bufferBuilder, float f, int i, boolean bl) {
        float g = f * 0.017453292F;
        float h = i / 48.0F * 6.2831855F;
        float f2 = MathHelper.cos(g) * MathHelper.cos(h);
        float g2 = MathHelper.sin(g);
        float h2 = MathHelper.cos(g) * MathHelper.sin(h);
        int j = bl ? endColourAt(g2) : this.colourAt(f2, g2, h2);
        SkyMesh.vertex(bufferBuilder, f2 * 100.0F, g2 * 100.0F, h2 * 100.0F, 0xFF000000 | j);
    }

    private int colourAt(float f, float g, float h) {
        int i = ColorHelper.lerp((float) Math.sqrt(Math.max(g, 0.0F)), this.horizon, this.zenith);
        if (g < 0.0F) {
            i = scale(i, 1.0F - 0.35F * AtmosphereAurora.smoothstep(-g / 0.4F));
        }
        if (this.glowStrength > 0.001F) {
            float f2 = Math.max(0.0F, f * this.sunX + g * this.sunY);
            float g2 = Math.max(0.0F, this.sunX < 0.0F ? -f : f);
            float h2 = (float) (Math.pow(f2, 8.0) * 0.9 + Math.pow(f2, 2.0) * 0.25);
            float f3 = (float) (Math.exp(-Math.abs(g) * 9.0F) * Math.pow(g2, 1.5));
            float g3 = MathHelper.clamp(this.glowStrength * (h2 + f3 * 0.6F), 0.0F, 1.0F);
            i = ColorHelper.lerp(g3, i, this.glow);
        }
        return i & 16777215;
    }

    private static int endColourAt(float f) {
        return ColorHelper.lerp((float) Math.sqrt(Math.max(f, 0.0F)), 1839152, 459789) & 16777215;
    }

    void renderHalos(MatrixStack matrices, float f, float g) {
        BufferBuilder bufferBuilder = this.halo.begin();
        float h = 1.0F - AtmosphereAurora.smoothstep(this.sunY / 0.35F);
        float f2 =
                AtmosphereAurora.smoothstep((this.sunY + 0.12F) / 0.12F) * f * (0.45F + 0.35F * h);
        int i = ColorHelper.lerp(h, 16773584, 16742963);
        haloQuads(bufferBuilder, this.sunAngle, 28.0F * (1.0F + 0.6F * h), i, f2);
        haloQuads(bufferBuilder, this.moonAngle, 34.0F, 11520255, 0.32F * g * f);
        this.halo.upload(bufferBuilder);
        this.halo.draw(matrices, 1.0F);
    }

    private static void haloQuads(BufferBuilder bufferBuilder, float f, float g, int i, float h) {
        if (!(h <= 0.002F)) {
            float[] floats = new float[] {0.0F, g * 0.16F, g * 0.38F, g * 0.66F, g};
            float[] floats2 = new float[] {1.0F, 0.55F, 0.22F, 0.07F, 0.0F};
            float f2 = MathHelper.cos(f);
            float g2 = MathHelper.sin(f);
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 32; k++) {
                    haloVertex(bufferBuilder, floats[j], k, f2, g2, i, floats2[j] * h);
                    haloVertex(bufferBuilder, floats[j], k + 1, f2, g2, i, floats2[j] * h);
                    haloVertex(bufferBuilder, floats[j + 1], k + 1, f2, g2, i, floats2[j + 1] * h);
                    haloVertex(bufferBuilder, floats[j + 1], k, f2, g2, i, floats2[j + 1] * h);
                }
            }
        }
    }

    private static void haloVertex(
            BufferBuilder bufferBuilder, float f, int i, float g, float h, int j, float f2) {
        float g2 = i / 32.0F * 6.2831855F;
        float h2 = MathHelper.cos(g2) * f;
        float f3 = 100.0F;
        float g3 = MathHelper.sin(g2) * f;
        float h3 = f3 * g - g3 * h;
        float f4 = f3 * h + g3 * g;
        SkyMesh.vertex(
                bufferBuilder,
                -f4,
                h3,
                h2,
                Math.round(MathHelper.clamp(f2, 0.0F, 1.0F) * 255.0F) << 24 | j & 16777215);
    }

    void renderMeteors(MatrixStack matrices, float f, float g) {
        long l = System.nanoTime();
        if (l >= this.meteorEnd) {
            if (this.nextMeteor == 0L) {
                this.nextMeteor = l + seconds(4.0F + 10.0F * this.random.nextFloat());
            }
            if (l < this.nextMeteor || f < 0.6F || g < 0.8F) {
                return;
            }
            this.launch(l);
        }
        float h = (float) (l - this.meteorStart) / (float) (this.meteorEnd - this.meteorStart);
        float f2 = MathHelper.sin(3.1415927F * MathHelper.clamp(h, 0.0F, 1.0F)) * f * g;
        BufferBuilder bufferBuilder = this.meteorMesh.begin();
        float g2 = h * 0.55F;
        float h2 = 0.16F;
        float[] floats = null;
        for (int i = 0; i <= 10; i++) {
            float f3 = g2 - h2 * i / 10.0F;
            float[] floats2 =
                    new float[] {
                        (this.mx + this.tx * f3) * 100.0F,
                        (this.my + this.ty * f3) * 100.0F,
                        (this.mz + this.tz * f3) * 100.0F
                    };
            if (floats != null) {
                streak(
                        bufferBuilder,
                        floats,
                        floats2,
                        f2 * (1.0F - (i - 1.0F) / 10.0F),
                        f2 * (1.0F - i / 10.0F));
            }
            floats = floats2;
        }
        this.meteorMesh.upload(bufferBuilder);
        this.meteorMesh.draw(matrices, 1.0F);
    }

    private void launch(long meteorStart2) {
        float f = (30.0F + 40.0F * this.random.nextFloat()) * 0.017453292F;
        float g = this.random.nextFloat() * 6.2831855F;
        this.mx = MathHelper.cos(f) * MathHelper.cos(g);
        this.my = MathHelper.sin(f);
        this.mz = MathHelper.cos(f) * MathHelper.sin(g);
        float h = -MathHelper.sin(g);
        float f2 = MathHelper.cos(g);
        float g2 = -0.6F - 0.4F * this.random.nextFloat();
        float h2 = h * this.mx + g2 * this.my + f2 * this.mz;
        this.tx = h - h2 * this.mx;
        this.ty = g2 - h2 * this.my;
        this.tz = f2 - h2 * this.mz;
        float f3 = MathHelper.sqrt(this.tx * this.tx + this.ty * this.ty + this.tz * this.tz);
        this.tx /= f3;
        this.ty /= f3;
        this.tz /= f3;
        this.meteorStart = meteorStart2;
        this.meteorEnd = meteorStart2 + seconds(0.7F + 0.5F * this.random.nextFloat());
        this.nextMeteor = this.meteorEnd + seconds(6.0F + 18.0F * this.random.nextFloat());
    }

    private static void streak(
            BufferBuilder bufferBuilder, float[] floats, float[] floats2, float f, float g) {
        float h = floats2[2] - floats[2];
        float f2 = floats2[1] - floats[1];
        float g2 = floats[1] * h - floats[2] * f2;
        float h2 = floats2[0] - floats[0];
        float f3 = floats[2] * h2 - floats[0] * h;
        float g3 = floats[0] * f2 - floats[1] * h2;
        float h3 = MathHelper.sqrt(g2 * g2 + f3 * f3 + g3 * g3);
        if (!(h3 < 1.0E-5F)) {
            float f4 = 0.22F / h3;
            int i = Math.round(MathHelper.clamp(f, 0.0F, 1.0F) * 255.0F) << 24 | 15397631;
            int j = Math.round(MathHelper.clamp(g, 0.0F, 1.0F) * 255.0F) << 24 | 15397631;
            float g4;
            float h4;
            float f5;
            SkyMesh.vertex(
                    bufferBuilder,
                    floats[0] - (g4 = g2 * f4),
                    floats[1] - (h4 = f3 * f4),
                    floats[2] - (f5 = g3 * f4),
                    i);
            SkyMesh.vertex(bufferBuilder, floats[0] + g4, floats[1] + h4, floats[2] + f5, i);
            SkyMesh.vertex(bufferBuilder, floats2[0] + g4, floats2[1] + h4, floats2[2] + f5, j);
            SkyMesh.vertex(bufferBuilder, floats2[0] - g4, floats2[1] - h4, floats2[2] - f5, j);
        }
    }

    private static long seconds(float f) {
        return (long) (f * 1.0E9F);
    }

    static int scale(int i, float f) {
        int j = MathHelper.clamp(Math.round((i >> 16 & 0xFF) * f), 0, 255);
        int k = MathHelper.clamp(Math.round((i >> 8 & 0xFF) * f), 0, 255);
        int m = MathHelper.clamp(Math.round((i & 0xFF) * f), 0, 255);
        return i & 0xFF000000 | j << 16 | k << 8 | m;
    }

    static int luma(int i) {
        int j =
                Math.round(
                        0.2126F * (i >> 16 & 0xFF)
                                + 0.7152F * (i >> 8 & 0xFF)
                                + 0.0722F * (i & 0xFF));
        return i & 0xFF000000 | j << 16 | j << 8 | j;
    }

    void close() {
        this.dome.close();
        this.endDome.close();
        this.halo.close();
        this.meteorMesh.close();
        this.ready = false;
    }
}
