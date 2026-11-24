package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class PeepEffects {

    private static final float SPOT_BASE = -7.0F;

    private static final float SPOT_LENGTH = 95.0F;

    private static final float SPOT_WIDTH = 30.0F;

    private static final float SPOT_SIDE = 0.085F;

    private static final int BLUE = 9074943;

    private static final int MAGENTA = 16736192;

    private static final int LILAC = 13995263;

    private static final int COOL = 8029951;

    static final int LIGHTNING_LANES = 2;

    static final int HEARTS = 20;

    private static final int METEOR_LANES = 15;

    private static final int RAIN_DROPS = 330;

    private static final int BEAMS = 8;

    private static final int SPARKS = 135;

    private static final int PETALS = 38;

    private static final int WISPS = 21;

    private static final int MOTES = 55;

    private static final int GLITTER = 84;

    private static final int SPOT_DUST = 44;

    private static final int SPIRITS = 12;

    private static final int FEATHERS = 22;

    private static final int METEOR_TRIES = 4;

    private static final int METEOR_SLOTS = 0;

    private static final int PETAL_SLOTS = 15;

    private static final int HEART_SLOTS = 53;

    private static final int WISP_SLOTS = 73;

    private static final int MOTE_SLOTS = 94;

    private static final int GLITTER_SLOTS = 149;

    private static final int SPIRIT_SLOTS = 233;

    private static final int FEATHER_SLOTS = 245;

    private static final float IN_VIEW = 0.8F;

    private static final float VIEW_SPREAD = 0.375F;

    private static final float CRY_ALONE_RAIN = 2.2F;

    private static final float RAIN_SLANT = 0.06F;

    private static final int BEAM_SAMPLES = 8;

    private static final float[] SCRATCH = new float[8];

    private static final float[] OTHER = new float[8];

    private static final float[] ARC = new float[9];

    private static final float[] AT = new float[2];

    private static final float METEOR_GAP = 5.0F;

    private PeepEffects() {}

    static void skyLight(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        float h = peepSky.w(PeepSong.THE_BRIGHTSIDE) * f;
        if (h > 0.003F) {
            sunrise(peepSky, skyPaint, h, g);
        }
        float f2;
        if ((f2 = peepSky.w(PeepSong.SUNLIGHT_ON_YOUR_SKIN) * f) > 0.003F) {
            sunlight(peepSky, skyPaint, f2, g);
        }
    }

    private static void sunrise(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        float age = peepSky.age(PeepSong.THE_BRIGHTSIDE);
        float smootherstep = PeepSky.smootherstep(MathHelper.clamp(age / 14.0F, 0.0F, 1.0F));
        float h = MathHelper.lerp(smootherstep, 0.28F, 0.7F) + 0.02F * MathHelper.sin(g * 0.25F);
        float f2 = 0.72F;
        float g2 = f2 - 0.16F;
        int tint = PeepSky.tint(0.36F * f, 16743088);
        int tint2 = PeepSky.tint(0.0F, 16743088);
        for (int k = 0; k < 64; k++) {
            float h2 = k / 64.0F;
            float f3 = (k + 1) / 64.0F;
            float g3 = PeepSky.smooth(0.06F, 0.0F, Math.abs(PeepSky.wrapDelta(h2 + 0.0078125F)));
            int tint3 = PeepSky.tint(0.28F * f * (1.0F - 0.5F * g3), 16761963);
            skyPaint.gradient(true, h2, f3, f2 - h, g2, tint2, tint);
            skyPaint.gradient(true, h2, f3, g2, f2 + 0.04F, tint, tint3);
        }
        float h3 =
                MathHelper.lerp(
                        PeepSky.smootherstep(MathHelper.clamp(age / 18.0F, 0.0F, 1.0F)),
                        -3.0F,
                        9.0F);
        SkyImage skyImage = peepSky.art[7];
        for (int n = 0; n < 3; n++) {
            float f4 = (n - 1) * 0.55F + 0.08F * MathHelper.sin(g * 0.12F + n * 2.0F);
            float g4 =
                    0.085F
                            * (0.8F + 0.2F * MathHelper.sin(g * 0.3F + n * 1.9F))
                            * (1.0F - 0.3F * peepSky.hazeAt(y(h3)));
            beam(
                    skyPaint,
                    skyImage,
                    0.0F,
                    h3 - 1.0F,
                    f4,
                    n == 0 ? 36.0F : (n == 2 ? 44.0F : 40.0F),
                    n == 0 ? 12.0F : (n == 2 ? 16.0F : 14.0F),
                    16763024,
                    g4 * f);
        }
        float h4 = y(h3);
        float clear = clear(peepSky, 0.0F, h3);
        skyPaint.glow(0.0F, h4, units(34.0F), 16752250, 0.23F * f);
        skyPaint.glow(0.0F, h4, units(13.0F), 16763024, 0.2F * f * (0.4F + 0.6F * clear));
        skyPaint.glow(
                0.0F,
                h4,
                units(3.6F),
                16760468,
                0.2F * f * (0.1F + 0.9F * clear) * core(peepSky, h4));
    }

    private static void sunlight(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        float h = peepSky.stageU + 0.05F;
        float f2 = 64.0F;
        skyPaint.glow(h, y(50.0F), units(64.0F), 16736150, 0.13F * f);
        skyPaint.glow(h, y(40.0F), units(70.0F), 16756848, 0.07F * f);
        skyPaint.glow(h, y(f2 - 8.0F), units(34.0F), 16752762, 0.2F * f);
        skyPaint.glow(h, y(f2), units(14.0F), 16762976, 0.32F * f);
        skyPaint.glow(h, y(f2), units(5.0F), 16765562, 0.3F * f * core(peepSky, y(f2)));
        SkyImage skyImage = peepSky.art[14];
        for (int i = 0; i < 5; i++) {
            float g2 =
                    3.1415927F + (i - 2.0F) * 0.3F + 0.07F * MathHelper.sin(g * 0.17F + i * 1.3F);
            float h2 = 62.0F + 8.0F * PeepSky.hash01(i * 3 + 1);
            float f3 = 15.0F + 6.0F * PeepSky.hash01(i * 3 + 2);
            float g3 = 0.8F + 0.2F * MathHelper.sin(g * 0.31F + i * 1.7F);
            float h3 = g3 * beamClear(peepSky, h, f2 + 2.0F, g2, h2, f3) * f;
            beam(skyPaint, skyImage, h, f2 + 2.0F, g2, h2, f3, 16749704, 0.17F * h3);
            beam(
                    skyPaint,
                    skyImage,
                    h,
                    f2 + 2.0F,
                    g2,
                    h2 * 0.45F,
                    f3 * 0.7F,
                    14721070,
                    0.62F * h3,
                    false);
            beam(skyPaint, skyImage, h, f2 + 2.0F, g2, h2 * 0.45F, f3 * 0.6F, 16761418, 0.5F * h3);
        }
    }

    static void behind(PeepSky peepSky, SkyPaint skyPaint, float f) {
        float alpha = peepSky.alpha;
        float g = peepSky.w(PeepSong.CRY_ALONE) * alpha;
        if (g > 0.003F) {
            lonely(peepSky, skyPaint, g, f);
        }
        float h;
        if ((h = peepSky.w(PeepSong.HELLBOY) * alpha) > 0.003F) {
            float f2 = 0.85F + 0.15F * (0.5F + 0.5F * MathHelper.sin(f * 6.2831855F * 0.7F));
            horizonLine(skyPaint, 1.5F, 6.0F, 16724821, 0.17F * f2 * h);
        }
        float g2;
        if ((g2 = peepSky.w(PeepSong.FOUR_GOLD_CHAINS) * alpha) > 0.003F) {
            horizonLine(skyPaint, 2.0F, 7.0F, 16765562, 0.22F * g2);
        }
        float h2;
        if ((h2 = peepSky.w(PeepSong.STAR_SHOPPING) * alpha) > 0.003F) {
            shootingStars(peepSky, skyPaint, h2, f);
        }
        float f3;
        if ((f3 = peepSky.w(PeepSong.BEAMER_BOY) * alpha) > 0.003F) {
            concertBeams(peepSky, skyPaint, f3, f);
        }
        float g3;
        if ((g3 = peepSky.w(PeepSong.SPOTLIGHT) * alpha) > 0.003F) {
            spotlight(peepSky, skyPaint, g3, f);
        }
        float h3;
        if ((h3 = peepSky.w(PeepSong.AWFUL_THINGS) * alpha) > 0.003F) {
            storm(peepSky, skyPaint, h3, f);
        }
        float f4;
        if ((f4 = peepSky.w(PeepSong.WITCHBLADES) * alpha) > 0.003F) {
            mist(skyPaint, peepSky.art[10], 5, f * 0.004F, -3.0F, 18.0F, 10108104, 0.2F * f4, true);
            mist(
                    skyPaint,
                    peepSky.art[10],
                    4,
                    -f * 0.0025F + 0.3F,
                    4.0F,
                    34.0F,
                    16736200,
                    0.38F * f4,
                    true);
        }
        float g4;
        if ((g4 = peepSky.w(PeepSong.GHOST_BOY) * alpha) > 0.003F) {
            mist(
                    skyPaint,
                    peepSky.art[10],
                    6,
                    f * 0.0015F,
                    -4.0F,
                    11.0F,
                    12356338,
                    0.28F * g4,
                    false);
            mist(
                    skyPaint,
                    peepSky.art[10],
                    5,
                    -f * 0.001F + 0.5F,
                    0.0F,
                    16.0F,
                    10516706,
                    0.1F * g4,
                    true);
            mist(
                    skyPaint,
                    peepSky.art[10],
                    5,
                    f * 0.0012F + 0.2F,
                    -2.0F,
                    9.0F,
                    14715600,
                    0.14F * g4,
                    true);
        }
    }

    private static void lonely(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        mist(skyPaint, peepSky.art[10], 6, g * 0.0012F, -2.0F, 8.0F, 2755106, 0.35F * f, false);
    }

    private static void shootingStars(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        SkyImage skyImage = peepSky.art[0];
        float[] floats = SCRATCH;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 15; j++) {
                boolean bl = j % 4 == 3;
                float h = 3.2F + 2.6F * PeepSky.hash01(j * 3 + 1);
                float f2 = g / h + PeepSky.hash01(j * 3 + 2);
                int k;
                float g2 = (f2 - (k = MathHelper.floor(f2))) * h;
                float h2;
                if (!(g2 > (h2 = (1.1F + 0.6F * PeepSky.hash01(j, k, 1)) * (bl ? 1.3F : 1.0F)))) {
                    float f3 = g2 / h2;
                    int m = 0 + j;
                    int picked = peepSky.picked(m, k);
                    float anchor = peepSky.anchor(m, k);
                    if (picked < 0) {
                        picked = 4;
                        for (int o = 0; o < 4 && picked == 4; o++) {
                            if (meteorClear(peepSky, j, k * 4 + o, bl, anchor, h2, g2, g, floats)) {
                                picked = o;
                            }
                        }
                        peepSky.pick(m, k, picked);
                    }
                    if (picked < 4) {
                        int n = k * 4 + picked;
                        meteor(j, n, bl, anchor, 1.0F - (float) Math.pow(1.0F - f3, 1.7), floats);
                        float g3 = floats[0];
                        float h3 = floats[1];
                        float f4 = floats[2];
                        float g4 = floats[3];
                        float h4 = floats[4];
                        float f5 = floats[5];
                        float g5 =
                                PeepSky.smooth(0.0F, 0.1F, f3)
                                        * (1.0F - PeepSky.smooth(0.55F, 1.0F, f3))
                                        * PeepSky.smooth(3.0F, 10.0F, h3)
                                        * f
                                        * clear(peepSky, g3, h3)
                                        * PeepSky.smooth(0.5F, 1.0F, peepSky.alpha);
                        if (!(g5 <= 0.003F)) {
                            if (i == 0) {
                                meteor(j, n, bl, anchor, 1.0F, OTHER);
                                float h5 =
                                        bl
                                                ? 34.0F
                                                : (peepSky.lyricBox(OTHER[0], OTHER[1], 6.0F, 6.0F)
                                                                > 0.05F
                                                        ? 20.0F
                                                        : 26.0F);
                                float f6 = Math.min(h4 * f5, h5) * (0.6F + 0.4F * (1.0F - f3));
                                float g6 = h3 - g4 * f6 * 0.4F;
                                float h6 = g3 - du(f4 * f6 * 0.4F, g6);
                                float f7 = (float) Math.atan2(-g4, f4);
                                skyPaint.sprite(
                                        skyImage,
                                        h6,
                                        y(g6),
                                        units(f6),
                                        units(4.5F),
                                        f7,
                                        PeepSky.tint(0.42F * g5, 16732080),
                                        true);
                                skyPaint.sprite(
                                        skyImage,
                                        h6,
                                        y(g6),
                                        units(f6),
                                        units(1.2F),
                                        f7,
                                        PeepSky.tint(0.5F * g5, 16756960),
                                        true);
                            } else {
                                skyPaint.glow(
                                        g3,
                                        y(h3),
                                        units(1.45F),
                                        16756960,
                                        0.38F * g5 * core(peepSky, y(h3)));
                                skyPaint.glow(g3, y(h3), units(7.0F), 16732080, 0.3F * g5);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void meteor(int i, int j, boolean bl, float f, float g, float[] floats) {
        float h = f + 0.04F + (PeepSky.hash01(i, j, 2) - 0.5F) * 0.36F;
        float f2 = 25.0F + 33.0F * PeepSky.hash01(i, j, 3);
        float g2 = (16.0F + 22.0F * PeepSky.hash01(i, j, 5)) * 0.017453292F;
        float h2 =
                bl
                        ? 40.0F + 15.0F * PeepSky.hash01(i, j, 6)
                        : 22.0F + 16.0F * PeepSky.hash01(i, j, 6);
        float f3 = -MathHelper.cos(g2);
        float g3 = -MathHelper.sin(g2);
        floats[1] = f2 + g3 * h2 * g;
        floats[0] = h + du(f3 * h2 * g, floats[1]);
        floats[2] = f3;
        floats[3] = g3;
        floats[4] = h2;
        floats[5] = g;
    }

    private static boolean meteorClear(
            PeepSky peepSky,
            int i,
            int j,
            boolean bl,
            float f,
            float g,
            float h,
            float f2,
            float[] floats) {
        for (int m = 1; m <= 6; m++) {
            float g2 = 0.08F + 0.84F * (m - 1) / 5.0F;
            meteor(i, j, bl, f, 1.0F - (float) Math.pow(1.0F - g2, 1.7), floats);
            float h2 = g2 * g - h;
            if (crowded(peepSky, i, f2 + h2, floats[0], floats[1])) {
                return false;
            }
            if (peepSky.lyricBox(floats[0], floats[1], 2.5F, 2.5F) > 0.02F) {
                return false;
            }
            float f3;
            if (!(floats[1] < 3.0F)
                    && peepSky.clearOfTattoos(floats[0] - (f3 = 0.02F * h2), floats[1], 1.5F)
                            < 0.9F) {
                return false;
            }
        }
        return true;
    }

    private static boolean crowded(PeepSky peepSky, int i, float f, float g, float h) {
        for (int j = 0; j < 15; j++) {
            if (j != i) {
                boolean bl = j % 4 == 3;
                float f2 = 3.2F + 2.6F * PeepSky.hash01(j * 3 + 1);
                float g2 = f / f2 + PeepSky.hash01(j * 3 + 2);
                int k;
                float h2 = (g2 - (k = MathHelper.floor(g2))) * f2;
                float f3;
                if (!(h2 > (f3 = (1.1F + 0.6F * PeepSky.hash01(j, k, 1)) * (bl ? 1.3F : 1.0F)))) {
                    int picked = peepSky.picked(0 + j, k);
                    float anchored = peepSky.anchored(0 + j, k);
                    if (picked >= 0 && picked < 4 && !Float.isNaN(anchored)) {
                        meteor(
                                j,
                                k * 4 + picked,
                                bl,
                                anchored,
                                1.0F - (float) Math.pow(1.0F - h2 / f3, 1.7),
                                OTHER);
                        float g3 =
                                PeepSky.wrapDelta(OTHER[0] - g)
                                        * 360.0F
                                        * MathHelper.cos(h * 0.017453292F);
                        float h3 = OTHER[1] - h;
                        if (g3 * g3 + h3 * h3 < 25.0F) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void concertBeams(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        SkyImage skyImage = peepSky.art[7];
        SkyImage skyImage2 = peepSky.art[14];
        for (int i = 0; i < 8; i++) {
            float h = (i + 0.5F) / 8.0F + 0.01F * MathHelper.sin(g * 0.05F + i);
            for (int j = 0; j < 2; j++) {
                float f2 = MathHelper.sin(g * 0.28F * (1.0F + 0.12F * i) + i * 1.9F + j * 0.7F);
                float g2 = (j == 0 ? 1.0F : -1.0F) * (0.12F + 0.5F * f2);
                boolean bl = (i + j) % 2 == 0;
                float h2 = bl ? 24.0F : 26.0F;
                float f3 =
                        (0.78F + 0.22F * MathHelper.sin(g * 1.1F + i * 2.0F + j))
                                * f
                                * beamClear(peepSky, h, -6.0F, g2, 82.0F, h2);
                beam(
                        skyPaint,
                        skyImage2,
                        h,
                        -6.0F,
                        g2,
                        70.0F,
                        h2 * 0.8F,
                        bl ? 16736192 : 8029951,
                        (bl ? 0.21F : 0.24F) * f3);
                beam(
                        skyPaint,
                        skyImage,
                        h,
                        -6.0F,
                        g2,
                        82.0F,
                        h2,
                        bl ? 16736192 : 9074943,
                        0.33F * f3);
                beam(
                        skyPaint,
                        skyImage,
                        h,
                        -6.0F,
                        g2,
                        80.0F,
                        3.0F,
                        bl ? 16769268 : 14866175,
                        0.2F * f3);
            }
        }
        horizonLine(skyPaint, 1.0F, 7.0F, 9074943, 0.08F * f);
        for (int k = 0; k < 8; k++) {
            float g3 = (k + 0.5F) / 8.0F + 0.01F * MathHelper.sin(g * 0.05F + k);
            float h3 = f * clear(peepSky, g3, 2.5F);
            skyPaint.glow(g3, y(2.5F), units(9.0F), k % 2 == 0 ? 16736192 : 9074943, 0.3F * h3);
            skyPaint.glow(g3, y(2.5F), units(3.9F), k % 2 == 0 ? 16770806 : 14866175, 0.36F * h3);
        }
    }

    private static void spotlight(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        SkyImage skyImage = peepSky.art[14];
        float spotTilt = spotTilt(g);
        float h = peepSky.stageU + 0.085F;
        float f2 = f * (0.3F + 0.7F * beamClear(peepSky, h, -7.0F, spotTilt, 95.0F, 30.0F));
        beam(skyPaint, skyImage, h, -7.0F, spotTilt, 95.0F, 54.0F, 16732080, 0.13F * f2);
        beam(skyPaint, skyImage, h, -7.0F, spotTilt, 95.0F, 30.0F, 15746463, 0.45F * f2);
        beam(skyPaint, skyImage, h, -7.0F, spotTilt, 52.25F, 13.5F, 16751318, 0.2F * f2);
        arc(h, -7.0F, spotTilt);
        onArc(8.5F / Math.max(0.3F, MathHelper.cos(spotTilt)), 0.0F);
        float g2 = AT[0];
        float h2 = AT[1];
        float f3 = f * (0.25F + 0.75F * clear(peepSky, g2, h2));
        skyPaint.glow(g2, y(h2), units(13.0F), 16766704, 0.3F * f3);
        skyPaint.glow(g2, y(h2), units(5.8F), 16759008, 0.48F * f3);
        for (int i = 0; i < 44; i++) {
            float fractionalPart =
                    MathHelper.fractionalPart(PeepSky.hash01(i * 5 + 1) + g * 3.0F / 58.9F);
            float g3 = 95.0F * (0.08F + 0.62F * fractionalPart);
            float coneHalf = coneHalf(g3 / 95.0F);
            float h3 = PeepSky.hash01(i * 5 + 2) - 0.5F;
            float f4 = h3 * coneHalf + 0.5F * MathHelper.sin(g * 0.45F + i * 2.0F);
            onArc(g3, f4);
            float g4 = AT[0];
            float h4 = AT[1];
            float f5 =
                    0.5F
                            + 0.5F
                                    * (float)
                                            Math.pow(
                                                    0.5F
                                                            + 0.5F
                                                                    * MathHelper.sin(
                                                                            g
                                                                                            * (0.7F
                                                                                                    + 0.5F
                                                                                                            * PeepSky
                                                                                                                    .hash01(
                                                                                                                            i
                                                                                                                                            * 5
                                                                                                                                    + 3))
                                                                                    + i * 1.7F),
                                                    2.0);
            float g5 =
                    PeepSky.smooth(0.0F, 0.12F, fractionalPart)
                            * (1.0F - PeepSky.smooth(0.8F, 1.0F, fractionalPart));
            float h5 =
                    (1.0F - 0.6F * Math.min(1.0F, Math.abs(h3) * 2.0F))
                            * (1.0F - PeepSky.smooth(0.3F, 0.5F, Math.abs(h3)));
            skyPaint.glow(
                    g4,
                    y(h4),
                    units(0.4F + 0.4F * PeepSky.hash01(i * 5 + 4)),
                    16765164,
                    0.55F * f5 * h5 * g5 * f * clear(peepSky, g4, h4));
        }
    }

    static float spotTilt(float f) {
        return -0.05F + 0.4F * MathHelper.sin(f * 0.2F + 0.5F);
    }

    static float spotlightOn(PeepSky peepSky, float f, float g) {
        arc(peepSky.stageU + 0.085F, -7.0F, spotTilt(peepSky.time));
        float alongArc = alongArc(f, g);
        float acrossArc = acrossArc(f, g);
        if (!(alongArc <= 0.0F) && !(alongArc >= 95.0F)) {
            float h = alongArc / 95.0F;
            float coneHalf = coneHalf(h);
            return (1.0F - PeepSky.smooth(coneHalf * 0.55F, coneHalf * 1.1F, acrossArc))
                    * (1.0F - PeepSky.smooth(0.75F, 1.0F, h));
        } else {
            return 0.0F;
        }
    }

    private static float coneHalf(float f) {
        return (0.05F + 0.84999996F * f) * 30.0F * 0.5F;
    }

    private static void storm(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        SkyImage skyImage = peepSky.art[10];
        mist(skyPaint, skyImage, 6, g * 0.002F, -2.0F, 24.0F, 1967638, 0.75F * f, false);
        mist(skyPaint, skyImage, 5, -g * 0.0013F + 0.4F, 10.0F, 34.0F, 2755104, 0.75F * f, false);
        mist(skyPaint, skyImage, 6, g * 0.002F + 0.37F, 2.0F, 20.0F, 5902140, 0.18F * f, true);
        mist(skyPaint, skyImage, 5, -g * 0.0016F + 0.15F, 16.0F, 52.0F, 9050714, 0.15F * f, true);
        float[] floats = SCRATCH;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                float h = lightning(j, g, floats) * f;
                if (!(h <= 0.003F)) {
                    float f2 = peepSky.stageU + floats[0];
                    float g2 = floats[1];
                    h *= clear(peepSky, f2, g2 * 0.5F);
                    if (i == 0) {
                        skyPaint.glow(f2, y(g2 * 0.7F), units(g2 * 2.4F), 9050714, 0.2F * h);
                        skyPaint.glow(f2, y(g2 * 0.55F), units(g2 * 1.5F), 12720235, 0.26F * h);
                        skyPaint.glow(f2, y(g2 * 0.45F), units(g2 * 0.6F), 16732080, 0.16F * h);
                    } else {
                        skyPaint.sprite(
                                peepSky.art[floats[2] < 0.5F ? 11 : 12],
                                f2,
                                y(g2 * 0.5F - 0.5F),
                                units(g2 * 0.5F),
                                units(g2 + 1.0F),
                                floats[3],
                                PeepSky.tint(0.6F * h, 16777215),
                                true);
                    }
                }
            }
        }
    }

    static float lightning(int i, float f, float[] floats) {
        float g = 18.0F + 12.0F * PeepSky.hash01(i * 7 + 1);
        float h = f / g + PeepSky.hash01(i * 7 + 2);
        int j;
        float f2 = (h - (j = MathHelper.floor(h))) * g;
        if (f2 > 2.6F) {
            return 0.0F;
        } else {
            floats[0] =
                    (PeepSky.hash01(i, j, 1) < 0.5F ? -1.0F : 1.0F)
                            * (0.07F + 0.14F * PeepSky.hash01(i, j, 5));
            floats[1] = 16.0F + 12.0F * PeepSky.hash01(i, j, 2);
            floats[2] = PeepSky.hash01(i, j, 3);
            floats[3] = (PeepSky.hash01(i, j, 4) - 0.5F) * 0.25F;
            return Math.min(
                    1.0F, flicker(f2) + 0.55F * flicker(f2 - 0.25F) + 0.3F * flicker(f2 - 0.65F));
        }
    }

    private static float flicker(float f) {
        if (f < 0.0F) {
            return 0.0F;
        } else {
            return f < 0.2F ? PeepSky.smooth(0.0F, 0.2F, f) : (float) Math.exp(-(f - 0.2F) / 0.24F);
        }
    }

    static void front(PeepSky peepSky, SkyPaint skyPaint, float f) {
        float alpha = peepSky.alpha;
        float g = peepSky.w(PeepSong.CRY_ALONE);
        float h = (peepSky.w(PeepSong.CRYBABY) + 2.2F * g) * alpha;
        if (h > 0.003F) {
            rain(peepSky, skyPaint, h, 1.0F + 0.5F * g, g, f);
        }
        float f2;
        if ((f2 = peepSky.w(PeepSong.LIFE_IS_BEAUTIFUL) * alpha) > 0.003F) {
            petals(peepSky, skyPaint, f2, f);
        }
        float g2;
        if ((g2 = peepSky.w(PeepSong.SAVE_THAT_SHIT) * alpha) > 0.003F) {
            hearts(peepSky, skyPaint, g2, f);
        }
        float h2;
        if ((h2 = peepSky.w(PeepSong.FALLING_DOWN) * alpha) > 0.003F) {
            feathers(peepSky, skyPaint, h2, f);
        }
        float f3;
        if ((f3 = peepSky.w(PeepSong.HELLBOY) * alpha) > 0.003F) {
            sparks(peepSky, skyPaint, f3, f);
        }
        float g3;
        if ((g3 = peepSky.w(PeepSong.GHOST_GIRL) * alpha) > 0.003F) {
            ghosts(peepSky, skyPaint, g3, f);
        }
        float h3;
        if ((h3 = peepSky.w(PeepSong.GHOST_BOY) * alpha) > 0.003F) {
            spirits(peepSky, skyPaint, h3, f);
        }
        float f4;
        if ((f4 = peepSky.w(PeepSong.SUNLIGHT_ON_YOUR_SKIN) * alpha) > 0.003F) {
            motes(peepSky, skyPaint, f4, f);
        }
        float g4;
        if ((g4 = peepSky.w(PeepSong.FOUR_GOLD_CHAINS) * alpha) > 0.003F) {
            glitter(peepSky, skyPaint, g4, f);
        }
    }

    private static void rain(
            PeepSky peepSky, SkyPaint skyPaint, float f, float g, float h, float f2) {
        SkyImage skyImage = peepSky.art[1];
        float g2 = f / 2.2F;
        float h2 = Math.min(1.0F, f) * g;
        int i = PeepSky.mix(16747216, 16736184, 0.5F * h);
        float f3 = MathHelper.sin(0.05999999865889549);
        float g3 = MathHelper.cos(0.05999999865889549);
        float h3 = MathHelper.lerp(h, 0.28F, 0.22F);
        for (int k = 0; k < 2; k++) {
            int j = k == 0 ? 1 : 3;
            for (int n = k; n < 330; n += j) {
                float f4 = MathHelper.clamp((g2 - PeepSky.hash01(n * 5 + 1)) * 10.0F, 0.0F, 1.0F);
                if (!(f4 <= 0.0F)) {
                    float g4 = 2.2F + 1.2F * PeepSky.hash01(n * 5 + 2);
                    float h4 = f2 / g4 + PeepSky.hash01(n * 5 + 3);
                    int m = MathHelper.floor(h4);
                    float f5 = h4 - m;
                    float hash01 = PeepSky.hash01(n * 5 + 4);
                    float g5 = MathHelper.lerp(f5, 78.0F, -2.0F);
                    float h5 = PeepSky.hash01(n, m, 7) + du(-(78.0F - g5) * 0.06F, g5);
                    float f6 =
                            PeepSky.smooth(0.0F, 0.08F, f5)
                                    * PeepSky.smooth(2.0F, 10.0F, g5)
                                    * f4
                                    * clear(peepSky, h5, g5);
                    if (!(f6 <= 0.003F)) {
                        f6 *=
                                h3
                                        + (1.0F - h3)
                                                * PeepSky.smooth(
                                                        0.25F,
                                                        1.0F,
                                                        peepSky.clearOfTattoos(h5, g5, 0.4F));
                        boolean bl = hash01 > 0.85F - 0.08F * h;
                        float g6 =
                                bl
                                        ? MathHelper.lerp(h, 3.0F + 3.0F * hash01, 7.5F)
                                        : 3.0F + 3.0F * hash01;
                        if (bl) {
                            f6 *= 1.0F + 0.3F * h;
                        }
                        if (k == 0) {
                            skyPaint.sprite(
                                    skyImage,
                                    h5,
                                    y(g5),
                                    units(0.35F + 0.2F * hash01),
                                    units(g6),
                                    0.06F,
                                    PeepSky.tint((0.4F + 0.38F * hash01) * f6 * h2, i),
                                    true);
                        } else {
                            float h6 = g5 - 0.35F * g6 * g3;
                            skyPaint.glow(
                                    h5 + du(-0.35F * g6 * f3, h6),
                                    y(h6),
                                    units(0.7F),
                                    16754397,
                                    0.22F * f6 * h2);
                        }
                    }
                }
            }
        }
    }

    private static void petals(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 38; j++) {
                float hash01 = PeepSky.hash01(j * 7 + 1);
                float h = 16.0F + 8.0F * hash01;
                float f2 = g / h + PeepSky.hash01(j * 7 + 2);
                int k = MathHelper.floor(f2);
                float g2 = f2 - k;
                float hash012 = PeepSky.hash01(j * 7 + 3);
                float h2 = MathHelper.lerp(g2, 76.0F, 2.0F);
                float f3 =
                        around(peepSky, 15 + j, k, PeepSky.hash01(j, k, 9))
                                + du(
                                        3.5F
                                                * MathHelper.sin(
                                                        g * 0.55F * (0.8F + 0.4F * hash01)
                                                                + j * 1.3F),
                                        h2);
                float g3 = 2.2F + 1.6F * hash012;
                float h3 =
                        PeepSky.smooth(0.0F, 0.07F, g2)
                                * PeepSky.smooth(3.0F, 11.0F, h2)
                                * f
                                * clearAround(peepSky, f3, h2)
                                * peepSky.clearOfTattoos(f3, h2, g3 * 0.4F);
                if (!(h3 <= 0.003F)) {
                    if (i == 0) {
                        skyPaint.glow(
                                f3,
                                y(h2),
                                units(g3 * 1.1F),
                                16732080,
                                0.1F * h3 * PeepSky.smooth(6.0F, 13.0F, h2));
                    } else {
                        float f4 = MathHelper.cos(g * (0.7F + 0.5F * hash01) + j);
                        float g4 = 0.42F + 0.58F * Math.abs(f4);
                        float h4 =
                                hash01 * 6.2831855F
                                        + g * (0.22F + 0.3F * hash012) * (j % 2 == 0 ? 1.0F : -1.0F)
                                        + 0.6F * MathHelper.sin(g * 0.7F + j);
                        int m =
                                PeepSky.mix(
                                        f4 < 0.0F ? 16765668 : 16770800,
                                        15916262,
                                        0.4F * (1.0F - hash012));
                        m = PeepSky.mix(m, 16740264, 1.0F - h3);
                        float f5 = PeepSky.smooth(0.35F, 0.75F, h3);
                        SkyImage skyImage = peepSky.art[j % 3 == 0 ? 15 : 2];
                        float units = units(g3 * 0.8F * g4);
                        float units2 = units(g3);
                        skyPaint.sprite(
                                skyImage,
                                f3,
                                y(h2),
                                units,
                                units2,
                                h4,
                                PeepSky.tint(0.9F * f5, m),
                                false);
                        skyPaint.sprite(
                                skyImage,
                                f3,
                                y(h2),
                                units,
                                units2,
                                h4,
                                PeepSky.tint(0.1F * f5, 16777215),
                                true);
                    }
                }
            }
        }
    }

    static void heart(int i, float f, float[] floats) {
        float hash01 = PeepSky.hash01(i * 11 + 1);
        float g = 10.0F + 5.0F * hash01;
        float h = f / g + PeepSky.hash01(i * 11 + 2);
        int j = MathHelper.floor(h);
        float f2 = h - j;
        boolean bl = PeepSky.hash01(i, j, 5) < 0.18F;
        floats[1] = MathHelper.lerp(f2, 76.0F, 3.0F);
        floats[0] =
                spread(PeepSky.hash01(i, j, 13))
                        + du(2.4F * MathHelper.sin(f * 0.6F + i * 1.7F), floats[1]);
        floats[2] = f2 * g;
        floats[3] = bl ? floats[2] - (0.26F + 0.3F * PeepSky.hash01(i, j, 6)) * g : 0.0F / 0.0F;
        floats[4] = (2.8F + 1.8F * PeepSky.hash01(i * 11 + 3)) * (bl ? 1.35F : 1.0F);
        floats[5] = f2;
        floats[6] = j;
    }

    private static void hearts(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        float[] floats = SCRATCH;
        for (int i = 0; i < 2; i++) {
            SkyImage skyImage = peepSky.art[i == 0 ? 3 : 4];
            for (int j = 0; j < 20; j++) {
                heart(j, g, floats);
                float h = peepSky.anchor(53 + j, (int) floats[6]) + floats[0];
                float f2 = floats[1];
                float g2 = floats[3];
                float h2 = floats[4];
                float f3 = h2 * 128.0F / 120.0F;
                float g3 = floats[5];
                f3 /=
                        1.0F
                                + 0.35F
                                        * PeepSky.smooth(
                                                15.0F,
                                                40.0F,
                                                Math.abs(PeepSky.wrapDelta(h - peepSky.aimU))
                                                        * 360.0F
                                                        * MathHelper.cos(f2 * 0.017453292F));
                boolean bl = !Float.isNaN(g2) && g2 >= 0.0F;
                if (bl || i != 1) {
                    float h3 = bl ? (i == 0 ? -1.0F : 1.0F) : 0.0F;
                    float f4 = 0.35F * MathHelper.sin(g * 0.5F + j);
                    float g4 = PeepSky.smooth(0.0F, 0.07F, g3) * PeepSky.smooth(1.0F, 8.0F, f2) * f;
                    float h4 = 0.0F;
                    float f5 = 0.0F;
                    if (!Float.isNaN(g2) && g2 > -0.4F) {
                        if (g2 < 0.0F) {
                            h +=
                                    du(
                                            PeepSky.smooth(-0.4F, 0.0F, g2)
                                                    * 0.15F
                                                    * MathHelper.sin(g * 37.0F + j),
                                            f2);
                        } else {
                            float smootherstep = PeepSky.smootherstep(Math.min(1.0F, g2 / 1.8F));
                            h4 = 0.55F * smootherstep;
                            f5 = (0.04F + 0.3F * smootherstep) * f3;
                            f2 -= 2.5F * smootherstep * smootherstep;
                            g4 *= 1.0F - PeepSky.smooth(0.8F, 2.2F, g2);
                        }
                    }
                    float g5;
                    if (!((g4 =
                                    g4
                                            * ((g5 =
                                                            clearAround(peepSky, h, f2)
                                                                    * peepSky.clearOfTattoos(
                                                                            h, f2, h2 * 0.45F))
                                                    * g5))
                            <= 0.003F)) {
                        float h5 = f4 + h3 * h4;
                        float f6 = 0.33F * h2;
                        float g6 = f2 - f6;
                        float h6 = f6 * MathHelper.sin(h5) + h3 * f5;
                        float f7 = -f6 * MathHelper.cos(h5);
                        float g7 = g6 - f7;
                        float h7 = h + du(h6, g7);
                        if (i == 0) {
                            skyPaint.glow(h, y(f2), units(h2 * 0.95F), 16732080, 0.12F * g4);
                        }
                        skyPaint.sprite(
                                bl ? skyImage : peepSky.art[13],
                                h7,
                                y(g7),
                                units(f3),
                                units(h2),
                                h5,
                                PeepSky.tint(
                                        g4 * PeepSky.smooth(0.12F, 0.45F, g4),
                                        j % 3 == 0 ? 16747208 : 16759014),
                                true);
                    }
                }
            }
        }
        SkyImage skyImage2 = peepSky.art[8];
        for (int k = 0; k < 2; k++) {
            for (int m = 0; m < 20; m++) {
                heart(m, g, floats);
                float f8 = floats[3];
                if (!Float.isNaN(f8) && !(f8 < 0.0F) && !(f8 > 0.8F)) {
                    float g8 = peepSky.anchor(53 + m, (int) floats[6]) + floats[0];
                    float h8 =
                            PeepSky.smooth(0.0F, 0.12F, f8)
                                    * (1.0F - PeepSky.smooth(0.2F, 0.8F, f8))
                                    * f
                                    * clearAround(peepSky, g8, floats[1]);
                    if (!(h8 <= 0.003F)) {
                        if (k == 0) {
                            skyPaint.glow(
                                    g8, y(floats[1]), units(floats[4] * 1.2F), 16732080, 0.3F * h8);
                        } else {
                            skyPaint.sprite(
                                    skyImage2,
                                    g8,
                                    y(floats[1]),
                                    units(floats[4] * 2.6F),
                                    units(floats[4] * 2.6F),
                                    0.3F,
                                    PeepSky.tint(0.8F * h8, 16766704),
                                    true);
                        }
                    }
                }
            }
        }
    }

    private static void feathers(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        SkyImage skyImage = peepSky.art[5];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 22; j++) {
                float hash01 = PeepSky.hash01(j * 13 + 1);
                float h = 30.0F + 10.0F * hash01;
                float f2 = g / h + PeepSky.hash01(j * 13 + 2);
                int k = MathHelper.floor(f2);
                float g2 = f2 - k;
                float hash012 = PeepSky.hash01(j * 13 + 3);
                float h2 = g * 0.75F * (0.85F + 0.3F * hash01) + j * 2.0F;
                float f3 =
                        MathHelper.lerp(g2, 78.0F, 4.0F)
                                - 0.8F * MathHelper.abs(MathHelper.sin(h2));
                float g3 =
                        around(peepSky, 245 + j, k, PeepSky.hash01(j, k, 17))
                                + du(4.5F * MathHelper.sin(h2), f3);
                float h3 = (4.0F + 2.0F * hash012) * 1.1F;
                float f4 =
                        PeepSky.smooth(0.0F, 0.08F, g2)
                                * PeepSky.smooth(2.0F, 10.0F, f3)
                                * f
                                * clearAround(peepSky, g3, f3)
                                * peepSky.clearOfTattoos(g3, f3, h3 * 0.25F);
                if (!(f4 <= 0.003F)) {
                    if (i == 0) {
                        skyPaint.glow(g3, y(f3), units(h3 * 0.5F), 16732080, 0.22F * f4);
                    } else {
                        float g4 =
                                0.5F * MathHelper.cos(h2)
                                        + (PeepSky.hash01(j * 13 + 4) - 0.5F) * 0.6F;
                        int m = PeepSky.mix(16756446, 14696602, 1.0F - f4);
                        skyPaint.sprite(
                                skyImage,
                                g3,
                                y(f3),
                                units(h3 * 0.28846154F),
                                units(h3),
                                g4,
                                PeepSky.tint(f4, m),
                                true);
                    }
                }
            }
        }
    }

    private static void sparks(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        SkyImage skyImage = peepSky.art[0];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 135; j++) {
                float hash01 = PeepSky.hash01(j * 17 + 1);
                float h = 3.5F + 2.5F * hash01;
                float f2 = g / h + PeepSky.hash01(j * 17 + 2);
                int k = MathHelper.floor(f2);
                float g2 = f2 - k;
                float hash012 = PeepSky.hash01(j * 17 + 3);
                float h2 = 6.0F + 18.0F * PeepSky.hash01(j, k, 4);
                float f3 = -1.0F + (h2 + 1.0F) * (1.0F - (float) Math.pow(1.0F - g2, 2.2));
                float g3 = g * (1.1F + 0.6F * hash01) + j;
                float h3 = PeepSky.hash01(j, k, 19) + du(1.8F * MathHelper.sin(g3) * g2, f3);
                float f4 =
                        0.65F
                                + 0.35F
                                        * (0.5F
                                                + 0.5F
                                                        * MathHelper.sin(
                                                                g * (13.0F + 12.0F * hash01)
                                                                        + j * 2.3F));
                float g4 =
                        PeepSky.smooth(0.03F, 0.12F, g2)
                                * (1.0F - PeepSky.smooth(0.3F, 0.85F, g2))
                                * f4
                                * f
                                * clearAround(peepSky, h3, f3);
                if (!(g4 <= 0.003F)) {
                    float h4 = 0.4F + 0.5F * hash012;
                    int m =
                            PeepSky.mix(
                                    PeepSky.mix(16751206, 16727150, PeepSky.smooth(0.1F, 0.5F, g2)),
                                    12720235,
                                    PeepSky.smooth(0.5F, 1.0F, g2));
                    float f5 =
                            1.8F
                                    * MathHelper.cos(g3)
                                    * (1.1F + 0.6F * hash01)
                                    * g2
                                    / Math.max(1.0F, h2);
                    if (i == 0) {
                        float g5 = 3.5F + 2.5F * hash012;
                        skyPaint.sprite(
                                skyImage,
                                h3,
                                y(f3 - g5 * 0.4F),
                                units(g5),
                                units(h4 * 0.7F),
                                -1.5707964F + f5,
                                PeepSky.tint(0.45F * g4, m),
                                true);
                    } else {
                        skyPaint.glow(h3, y(f3), units(h4 * 1.2F), m, 0.58F * g4);
                        skyPaint.glow(
                                h3,
                                y(f3),
                                units(h4 * 0.7F),
                                16763024,
                                0.4F * g4 * (1.0F - PeepSky.smooth(0.15F, 0.45F, g2)));
                        skyPaint.sprite(
                                peepSky.art[1],
                                h3,
                                y(f3 - h4 * 0.45F),
                                units(h4 * 0.5F),
                                units(h4 * 1.8F),
                                3.1415927F + f5,
                                PeepSky.tint(0.75F * g4, 16762010),
                                true);
                    }
                }
            }
        }
    }

    private static void ghosts(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        SkyImage skyImage = peepSky.art[6];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 21; j++) {
                float hash01 = PeepSky.hash01(j * 19 + 1);
                float h = 10.0F + 4.0F * hash01;
                float f2 = g / h + PeepSky.hash01(j * 19 + 2);
                int k = MathHelper.floor(f2);
                float g2 = f2 - k;
                float hash012 = PeepSky.hash01(j * 19 + 3);
                float h2 = MathHelper.lerp((float) Math.pow(g2, 0.9), -4.0F, 58.0F);
                float f3 = g * 0.45F + j * 1.9F;
                float g3 = 0.85F + 0.35F * g2;
                float h3 = (8.0F + 5.0F * hash012) * g3;
                float f4 = h3 * 96.0F / 256.0F * 1.8F;
                float g4 = h2 - h3 * 0.3F;
                float h4 =
                        around(peepSky, 73 + j, k, PeepSky.hash01(j, k, 23))
                                + du(3.6F * MathHelper.sin(f3), g4);
                float f5 =
                        PeepSky.smooth(0.0F, 0.12F, g2)
                                * (1.0F - PeepSky.smooth(0.55F, 1.0F, g2))
                                * PeepSky.smooth(1.0F, 12.0F, h2)
                                * f
                                * clearAround(peepSky, h4, h2)
                                * clearAround(peepSky, h4, g4);
                if (!(f5 <= 0.003F)) {
                    if (i == 0) {
                        skyPaint.sprite(
                                skyImage,
                                h4,
                                y(g4),
                                units(f4),
                                units(h3),
                                0.32F * MathHelper.cos(f3),
                                PeepSky.tint(0.42F * f5, 16750300),
                                true);
                    } else {
                        float g5 = 0.32F * MathHelper.cos(f3);
                        float h5 = h3 * 0.3F;
                        float f6 = g4 + h5 * MathHelper.cos(g5);
                        skyPaint.glow(
                                h4 + du(h5 * MathHelper.sin(g5), f6),
                                y(f6),
                                units(2.0F),
                                16759020,
                                0.24F * f5);
                    }
                }
            }
        }
    }

    private static void spirits(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        for (int i = 0; i < 12; i++) {
            float hash01 = PeepSky.hash01(i * 31 + 1);
            float h = 11.0F + 6.0F * hash01;
            float f2 = g / h + PeepSky.hash01(i * 31 + 2);
            int j = MathHelper.floor(f2);
            float g2 = f2 - j;
            float h2 =
                    MathHelper.lerp(
                            (float) Math.pow(g2, 0.85),
                            1.0F,
                            26.0F + 10.0F * PeepSky.hash01(i, j, 3));
            float f3 = g * (0.4F + 0.2F * hash01) + i * 2.3F;
            float g3 =
                    around(peepSky, 233 + i, j, PeepSky.hash01(i, j, 2))
                            + du(2.2F * MathHelper.sin(f3), h2);
            float h3 = 0.8F + 0.2F * MathHelper.sin(g * (1.3F + 0.6F * hash01) + i);
            float f4 =
                    PeepSky.smooth(0.0F, 0.15F, g2)
                            * (1.0F - PeepSky.smooth(0.5F, 1.0F, g2))
                            * h3
                            * f
                            * clearAround(peepSky, g3, h2);
            if (!(f4 <= 0.003F)) {
                float g4 = 0.7F + 0.5F * PeepSky.hash01(i * 31 + 4);
                skyPaint.glow(g3, y(h2), units(g4 * 6.0F), 10258144, 0.12F * f4);
                skyPaint.glow(g3, y(h2), units(g4 * 2.0F), 14208255, 0.3F * f4);
                skyPaint.glow(
                        g3, y(h2), units(g4 * 0.7F), 16184063, 0.6F * f4 * core(peepSky, y(h2)));
            }
        }
    }

    private static void motes(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        float h = 1.0F + 0.5F * peepSky.pulse;
        for (int i = 0; i < 55; i++) {
            float hash01 = PeepSky.hash01(i * 23 + 1);
            float f2 = 9.0F + 6.0F * hash01;
            float g2 = g / f2 + PeepSky.hash01(i * 23 + 2);
            int j = MathHelper.floor(g2);
            float h2 = g2 - j;
            float f3 =
                    4.0F
                            + 56.0F * PeepSky.hash01(i, j, 3)
                            + 3.0F * h2
                            + 1.5F * MathHelper.sin(g * 0.15F + i * 1.3F);
            float g3 =
                    around(peepSky, 94 + i, j, PeepSky.hash01(i, j, 2))
                            + du(1.2F * MathHelper.sin(g * 0.11F + i), f3);
            float h3 = 0.55F + 0.45F * MathHelper.sin(g * (0.8F + 0.5F * hash01) + i);
            float f4 =
                    h3
                            * PeepSky.smooth(0.0F, 0.2F, h2)
                            * (1.0F - PeepSky.smooth(0.75F, 1.0F, h2))
                            * f
                            * clearAround(peepSky, g3, f3)
                            * h;
            if (!(f4 <= 0.003F)) {
                float g4 = (0.6F + 0.72F * PeepSky.hash01(i * 23 + 4)) * (i % 2 == 0 ? 1.3F : 1.0F);
                int k = i % 2 == 0 ? 16762714 : (i % 4 == 1 ? 16752324 : 16765050);
                skyPaint.glow(g3, y(f3), units(g4 * 4.0F), k, 0.2F * f4);
                skyPaint.glow(g3, y(f3), units(g4 * 1.4F), k, 0.35F * f4);
                skyPaint.glow(g3, y(f3), units(g4 * 0.6F), k, 0.42F * f4 * core(peepSky, y(f3)));
            }
        }
    }

    private static void glitter(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        SkyImage skyImage = peepSky.art[16];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 84; j++) {
                float hash01 = PeepSky.hash01(j * 29 + 1);
                float h = (g * (0.9F + 0.9F * hash01) + j * 2.7F) / 6.2831855F;
                int k;
                float f2 = h - (k = MathHelper.floor(h));
                if (!(f2 >= 0.5F)) {
                    float g2 =
                            (float)
                                    Math.pow(
                                            MathHelper.sin(
                                                    3.1415927F * (float) Math.pow(f2 * 2.0F, 0.6)),
                                            2.0);
                    float h2 =
                            5.0F
                                    + 60.0F * PeepSky.hash01(j, k, 3)
                                    + 1.5F * MathHelper.sin(g * 0.2F + j);
                    float f3 =
                            around(peepSky, 149 + j, k, PeepSky.hash01(j, k, 2))
                                    + du(0.6F * MathHelper.sin(g * 0.1F + j), h2);
                    float g3 = PeepSky.smooth(0.05F, 0.3F, g2) * f * clearAround(peepSky, f3, h2);
                    if (!(g3 <= 0.003F)) {
                        float h3 =
                                (1.3F + 1.2F * PeepSky.hash01(j * 29 + 4)) * (0.25F + 0.75F * g2);
                        boolean bl = j % 4 == 3;
                        int m = bl ? 16747213 : (j % 2 == 0 ? 16769952 : 16771504);
                        float f4 =
                                (1.0F - 0.35F * PeepSky.smooth(14.0F, 4.0F, h2))
                                        * core(peepSky, y(h2));
                        if (i == 0) {
                            skyPaint.glow(
                                    f3,
                                    y(h2),
                                    units(h3 * 0.9F),
                                    bl ? 16732080 : 16758858,
                                    0.28F * g3 * g2);
                        } else {
                            skyPaint.sprite(
                                    skyImage,
                                    f3,
                                    y(h2),
                                    units(h3),
                                    units(h3),
                                    0.4F * MathHelper.sin(g * 0.3F + j),
                                    PeepSky.tint((bl ? 0.65F : 0.8F) * g3 * f4, m),
                                    true);
                        }
                    }
                }
            }
        }
    }

    static void orbits(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        SkyImage skyImage = peepSky.art[9];
        float units = units(18.0F);
        for (int i = 0; i < peepSky.shownTattoos; i++) {
            float h = Math.min(units, (peepSky.tw[i] + peepSky.th[i]) * 0.5F);
            float f2 =
                    peepSky.tGlow[i]
                            * (0.6F + 0.4F * peepSky.tGlow[i])
                            * PeepSky.smooth(0.3F, 0.5F, peepSky.tGlow[i])
                            * g;
            if (!(f2 <= 0.003F)) {
                float g2 = f * 0.55F + i * 1.3F;
                float h2 = f * 0.33F + i * 2.1F + 2.0F;
                skyPaint.sprite(
                        skyImage,
                        peepSky.tu[i],
                        peepSky.ty[i],
                        h * 1.35F,
                        h * 1.35F,
                        g2,
                        PeepSky.tint(0.7F * f2, 13995263),
                        true);
                skyPaint.sprite(
                        skyImage,
                        peepSky.tu[i],
                        peepSky.ty[i],
                        h * 1.6F,
                        h * 1.6F,
                        h2,
                        PeepSky.tint(0.5F * f2, 16748512),
                        true);
                arcHead(peepSky, skyPaint, i, h * 1.35F, g2, 0.5F * f2);
                arcHead(peepSky, skyPaint, i, h * 1.6F, h2, 0.35F * f2);
            }
        }
    }

    private static void arcHead(
            PeepSky peepSky, SkyPaint skyPaint, int i, float f, float g, float h) {
        float f2 = f * 0.421875F;
        float g2 = (0.72F - peepSky.ty[i]) * 1.0471976F * 57.295776F;
        float h2 = peepSky.tu[i] + du(f2 * MathHelper.cos(g) * 60.0F, g2);
        float f3 = peepSky.ty[i] + f2 * MathHelper.sin(g);
        skyPaint.glow(h2, f3, units(3.0F), 16732080, 0.3F * h);
        skyPaint.glow(h2, f3, units(1.8F), 16748512, 0.3F * h);
        skyPaint.glow(h2, f3, units(0.8F), 16766704, h);
    }

    static void goldSparkles(PeepSky peepSky, SkyPaint skyPaint, float f, float g) {
        SkyImage skyImage = peepSky.art[8];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < peepSky.shownTattoos; j++) {
                float h = PeepSky.smooth(0.15F, 0.6F, peepSky.tGlow[j]) * g;
                if (!(h <= 0.003F)) {
                    for (int k = 0; k < 3; k++) {
                        float f2 = k * 2.1F + j + f * 0.15F;
                        float g2 =
                                (float)
                                        Math.pow(
                                                Math.max(
                                                        0.0F,
                                                        MathHelper.sin(
                                                                f * (1.1F + 0.3F * k)
                                                                        + j * 1.9F
                                                                        + k * 2.2F)),
                                                4.0);
                        if (!(g2 <= 0.03F)) {
                            float h2 =
                                    peepSky.tu[j]
                                            + MathHelper.cos(f2)
                                                    * peepSky.tw[j]
                                                    * 0.5F
                                                    * 60.0F
                                                    / (360.0F
                                                            * Math.max(
                                                                    0.2F,
                                                                    MathHelper.cos(
                                                                            (0.72F - peepSky.ty[j])
                                                                                    * 1.0471976F)));
                            float f3 = peepSky.ty[j] + MathHelper.sin(f2) * peepSky.th[j] * 0.55F;
                            float g3 =
                                    Math.min(peepSky.th[j] * 0.6F, units(7.0F))
                                            * (0.2F + 0.8F * g2);
                            float h3 = h * PeepSky.smooth(0.03F, 0.25F, g2);
                            if (i == 0) {
                                skyPaint.glow(h2, f3, g3 * 0.35F, 16758858, 0.25F * h3 * g2);
                            } else {
                                skyPaint.sprite(
                                        skyImage,
                                        h2,
                                        f3,
                                        g3,
                                        g3,
                                        f * 0.4F + k,
                                        PeepSky.tint(h3, k == 1 ? 16774354 : 16770720),
                                        true);
                            }
                        }
                    }
                }
            }
        }
    }

    static float spread(float f) {
        return f < 0.8F ? (f / 0.8F - 0.5F) * 0.375F : (f - 0.8F) / 0.19999999F;
    }

    private static float around(PeepSky peepSky, int i, int j, float f) {
        return peepSky.anchor(i, j) + spread(f);
    }

    private static void horizonLine(SkyPaint skyPaint, float f, float g, int i, float h) {
        if (!(h <= 0.003F)) {
            int tint = PeepSky.tint(h, i);
            int tint2 = PeepSky.tint(0.0F, i);
            float f2 = y(g);
            float g2 = y(f);
            float h2 = y(-1.0F);
            for (int m = 0; m < 48; m++) {
                float f3 = m / 48.0F;
                float g3 = (m + 1) / 48.0F;
                skyPaint.gradient(true, f3, g3, f2, g2, tint2, tint);
                skyPaint.gradient(true, f3, g3, g2, h2, tint, tint2);
            }
        }
    }

    private static void mist(
            SkyPaint skyPaint,
            SkyImage skyImage,
            int i,
            float f,
            float g,
            float h,
            int j,
            float f2,
            boolean bl) {
        if (!(f2 <= 0.003F)) {
            float g2 = y(h);
            float h2 = y(g);
            float wrap = PeepSky.wrap(f);
            int tint = PeepSky.tint(f2, j);
            for (int n = 0; n < i; n++) {
                float f3 = (n + wrap) / i;
                skyPaint.band(skyImage, f3, f3 + 1.0F / i, g2, h2, tint, bl, 24);
            }
        }
    }

    private static void beam(
            SkyPaint skyPaint,
            SkyImage skyImage,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            int i,
            float h2) {
        beam(skyPaint, skyImage, f, g, h, f2, g2, i, h2, true);
    }

    private static void beam(
            SkyPaint skyPaint,
            SkyImage skyImage,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            int i,
            float h2,
            boolean bl) {
        if (!(h2 <= 0.003F)) {
            arc(f, g, h);
            float onArc = onArc(f2 * 0.5F, 0.0F);
            skyPaint.sprite(
                    skyImage,
                    AT[0],
                    y(AT[1]),
                    units(g2),
                    units(f2),
                    onArc,
                    PeepSky.tint(h2, i),
                    bl);
        }
    }

    private static void arc(float f, float g, float h) {
        float f2 = f * 6.2831855F;
        float g2 = g * 0.017453292F;
        float h2 = MathHelper.cos(f2);
        float f3 = MathHelper.sin(f2);
        float g3 = MathHelper.cos(g2);
        float h3 = MathHelper.sin(g2);
        float f4 = MathHelper.cos(h);
        float g4 = MathHelper.sin(h);
        ARC[0] = g3 * h2;
        ARC[1] = g3 * f3;
        ARC[2] = h3;
        ARC[3] = -f4 * h3 * h2 - g4 * f3;
        ARC[4] = -f4 * h3 * f3 + g4 * h2;
        ARC[5] = f4 * g3;
    }

    private static float onArc(float f, float g) {
        float h = f * 0.017453292F;
        float f2 = MathHelper.cos(h);
        float g2 = MathHelper.sin(h);
        float h2 = f2 * ARC[0] + g2 * ARC[3];
        float f3 = f2 * ARC[1] + g2 * ARC[4];
        float g3 = f2 * ARC[2] + g2 * ARC[5];
        float h3 = f2 * ARC[3] - g2 * ARC[0];
        float f4 = f2 * ARC[4] - g2 * ARC[1];
        float g4 = f2 * ARC[5] - g2 * ARC[2];
        if (g != 0.0F) {
            float h4 = g * 0.017453292F;
            float f5 = MathHelper.cos(h4);
            float g5 = MathHelper.sin(h4);
            float h5 = f4 * g3 - g4 * f3;
            float f6 = g4 * h2 - h3 * g3;
            float g6 = h3 * f3 - f4 * h2;
            h2 = f5 * h2 + g5 * h5;
            f3 = f5 * f3 + g5 * f6;
            g3 = f5 * g3 + g5 * g6;
        }
        ARC[6] = h2;
        ARC[7] = f3;
        ARC[8] = g3;
        float h6 = (float) Math.atan2(f3, h2);
        float f7 = (float) Math.asin(MathHelper.clamp(g3, -1.0F, 1.0F));
        AT[0] = PeepSky.wrap(h6 / 6.2831855F);
        AT[1] = f7 * 57.295776F;
        float g7 = MathHelper.cos(h6);
        float h7 = MathHelper.sin(h6);
        float f8 = MathHelper.sin(f7);
        float g8 = MathHelper.cos(f7);
        float h8 = -h3 * f8 * g7 - f4 * f8 * h7 + g4 * g8;
        float f9 = -h3 * h7 + f4 * g7;
        return (float) Math.atan2(f9, h8);
    }

    private static float alongArc(float f, float g) {
        float h = f * 6.2831855F;
        float f2 = g * 0.017453292F;
        float g2 = MathHelper.cos(f2);
        float h2 = g2 * MathHelper.cos(h);
        float f3 = g2 * MathHelper.sin(h);
        float g3 = MathHelper.sin(f2);
        return (float)
                        Math.atan2(
                                h2 * ARC[3] + f3 * ARC[4] + g3 * ARC[5],
                                h2 * ARC[0] + f3 * ARC[1] + g3 * ARC[2])
                * 57.295776F;
    }

    private static float acrossArc(float f, float g) {
        float h = f * 6.2831855F;
        float f2 = g * 0.017453292F;
        float g2 = MathHelper.cos(f2);
        float h2 = g2 * MathHelper.cos(h);
        float f3 = g2 * MathHelper.sin(h);
        float g3 = MathHelper.sin(f2);
        float h3 = ARC[4] * ARC[2] - ARC[5] * ARC[1];
        float f4 = ARC[5] * ARC[0] - ARC[3] * ARC[2];
        float g4 = ARC[3] * ARC[1] - ARC[4] * ARC[0];
        return (float)
                        Math.asin(
                                MathHelper.clamp(Math.abs(h2 * h3 + f3 * f4 + g3 * g4), 0.0F, 1.0F))
                * 57.295776F;
    }

    private static float beamClear(PeepSky peepSky, float f, float g, float h, float f2, float g2) {
        arc(f, g, h);
        float h2 = MathHelper.clamp(alongArc(peepSky.aimU, peepSky.viewUp), 0.0F, f2);
        onArc(h2, 0.0F);
        float f3 = peepSky.aimU * 6.2831855F;
        float g3 = peepSky.viewUp * 0.017453292F;
        float h3 = MathHelper.cos(g3);
        float f4 =
                h3 * MathHelper.cos(f3) * ARC[6]
                        + h3 * MathHelper.sin(f3) * ARC[7]
                        + MathHelper.sin(g3) * ARC[8];
        float g4 =
                (float) Math.acos(MathHelper.clamp(f4, -1.0F, 1.0F)) * 57.295776F
                        - g2 * (0.025F + 0.42F * h2 / f2);
        float h4 = 0.0F;
        for (int k = 1; k <= 8; k++) {
            float f5 = f2 * k / 9.0F;
            float g5 = g2 * (0.025F + 0.42F * f5 / f2) * 0.5F;
            onArc(f5, 0.0F);
            h4 = Math.max(h4, peepSky.lyricBox(AT[0], AT[1], g5, g5));
        }
        return (0.35F + 0.65F * PeepSky.smooth(3.0F, 12.0F, g4)) * (1.0F - 0.45F * h4);
    }

    private static float clear(PeepSky peepSky, float f, float g) {
        return peepSky.clear(f, g);
    }

    private static float core(PeepSky peepSky, float f) {
        return 1.0F - 0.4F * peepSky.hazeAt(f);
    }

    private static float clearAround(PeepSky peepSky, float f, float g) {
        return peepSky.clear(f, g);
    }

    private static float y(float f) {
        return PeepSky.elevationToY(f);
    }

    private static float units(float f) {
        return f * 0.017453292F / 1.0471976F;
    }

    private static float du(float f, float g) {
        return f / (360.0F * Math.max(0.2F, MathHelper.cos(g * 0.017453292F)));
    }
}
