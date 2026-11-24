package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import javax.imageio.ImageIO;

@Environment(EnvType.CLIENT)
final class PeepSky {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    static final int PINK = 16732080;

    static final int HOT = 16766704;

    static final int DEEP = 12720235;

    private static final int BLUR = 9;

    private static final int SEGMENTS = 64;

    private static final int ROWS = 14;

    private static final int TATTOO_PAD = 26;

    private static final int TATTOO_MAX = 320;

    private static final float COVER_SHARE = 0.7F;

    private static final float TATTOO_ART = 0.8F;

    static final float SPIN_PER_SECOND = 0.02F;

    private static final String TATTOOS = "/assets/efn/textures/peep/";

    private static final String[] KINDS =
            new String[] {
                "star",
                "anarchy",
                "cake",
                "heart",
                "wave",
                "rose",
                "mom",
                "horseshoe",
                "hellboy",
                "rip"
            };

    static final int STAR = 0;

    static final int RIP = 9;

    private static final float[] ELEVATION =
            new float[] {41.0F, 18.0F, 34.0F, 46.0F, 22.0F, 30.0F, 38.0F, 49.0F, 23.0F, 36.0F};

    private static final float[] SIZE =
            new float[] {6.0F, 10.0F, 8.0F, 6.0F, 7.0F, 9.0F, 5.0F, 6.0F, 6.0F, 12.0F};

    private static final int STARS = 140;

    private static final int BOKEH = 12;

    private static final float CROSSFADE_SECONDS = 2.0F;

    private static final float STAGE_FOLLOW = 0.5F;

    private static final float FALL_PER_SECOND = 0.014285714F;

    private static final float FALL_LOW = 12.0F;

    private static final float FALL_HIGH = 64.0F;

    private static final float STAR_LOW = -1.5F;

    private static final float STAR_HIGH = 86.0F;

    private static final float LYRIC_EASE = 3.0F;

    private static final float PARTICLE_KEEP_OUT = 1.0F;

    private static final float TEXT_GAP = 3.0F;

    private static final float TEXT_GAP_BELOW = 1.0F;

    private static final float TEXT_FADE = 5.0F;

    private static final float TEXT_FADE_BELOW = 1.5F;

    private static final float COLUMN_HALF = 14.0F;

    private static final float AIM_NEAR = 6.0F;

    private static final float AIM_FAR = 15.0F;

    private static final float HORIZON_HOLD = 0.04F;

    private static final float HORIZON_DEEP = 0.36F;

    private static final float HORIZON_FADE = 0.6F;

    private static final float HORIZON_DEEP_SHARE = 0.78F;

    private static final float BEAT_RISE = 0.14F;

    private static final int GOLD = 16769658;

    private static final int GHOST = 14208255;

    private static final float LIT_GLOW = 0.6F;

    private volatile PeepSky.Built built;

    private volatile int generation;

    private boolean building;

    private SkyImage[] tattoos;

    private SkyImage[] rims;

    private SkyImage[] golds;

    private SkyImage[] veils;

    private int[] kinds;

    SkyImage[] art;

    private final float[] weight = new float[PeepSong.ALL.length];

    private final float[] shown = new float[PeepSong.ALL.length];

    private final float[] songAge = new float[PeepSong.ALL.length];

    private PeepSong song;

    float time;

    float alpha;

    float viewU;

    float aimU;

    float viewUp;

    float stageU = 0.0F / 0.0F;

    float pulse;

    int horizonRgb;

    float horizonShare;

    private float fall;

    private float fallFade;

    private boolean fresh = true;

    static final int ANCHORS = 512;

    private static final int GHOST_SLOTS = 512 - KINDS.length;

    private static final float HAUNT_SPEED = 0.42F;

    private static final float HAUNT_PHASE = 3.5F;

    private static final float[] HAUNT_AZIMUTH =
            new float[] {
                -40.0F, -40.0F, -56.0F, -56.0F, -33.0F, -33.0F, 33.0F, 33.0F, 40.0F, 40.0F, 56.0F
            };

    private static final boolean[] HAUNT_HIGH =
            new boolean[] {false, false, true, true, true, true, true, true, false, false, true};

    private final float[] anchorU = new float[512];

    private final int[] anchorRun = new int[512];

    private final byte[] pick = new byte[512];

    private final int[] pickRun = new int[512];

    private boolean lyricsShown;

    private float lyricU;

    private float titleU;

    private float lyricUp;

    private float lineHalfWide;

    private float lineHalfTall;

    private float titleUp;

    private float titleHalfWide;

    private float titleHalfTall;

    private float wantLineWide;

    private float wantTitleWide;

    private final PeepSky.Look look;

    private static final int MAX_SHOWN = KINDS.length * 2;

    final float[] tu;

    final float[] ty;

    final float[] tw;

    final float[] th;

    final float[] tGlow;

    final float[] tLit;

    private final float[] tSpin;

    private final float[] tAlpha;

    private final float[] tGhost;

    private final int[] tTex;

    int shownTattoos;

    private static final PeepSky.Palette BASE =
            new PeepSky.Palette(
                    16732080, 12720235, 1836056, 525072, 3934768, 16766704, 16732080, 0.42F, 1.0F,
                    1.0F);

    private static final PeepSky.Palette[] PALETTES = new PeepSky.Palette[PeepSong.ALL.length];

    PeepSky() {
        Arrays.fill(this.anchorRun, -2147483648);
        Arrays.fill(this.pickRun, -2147483648);
        this.lyricUp = 28.0F;
        this.look = new PeepSky.Look();
        this.tu = new float[MAX_SHOWN];
        this.ty = new float[MAX_SHOWN];
        this.tw = new float[MAX_SHOWN];
        this.th = new float[MAX_SHOWN];
        this.tGlow = new float[MAX_SHOWN];
        this.tLit = new float[MAX_SHOWN];
        this.tSpin = new float[MAX_SHOWN];
        this.tAlpha = new float[MAX_SHOWN];
        this.tGhost = new float[MAX_SHOWN];
        this.tTex = new int[MAX_SHOWN];
    }

    private static PeepSky.Palette palette(PeepSong peepSong) {
        return switch (peepSong) {
            case STAR_SHOPPING ->
                    new PeepSky.Palette(
                            16732080, 12720235, 1574434, 394260, 3017786, 16766704, 16740296, 0.4F,
                            1.0F, 1.2F);
            case CRYBABY ->
                    new PeepSky.Palette(
                            15751854, 11016294, 1574426, 459791, 3412026, 16766704, 15755960, 0.38F,
                            0.92F, 0.9F);
            case CRY_ALONE ->
                    new PeepSky.Palette(
                            12073606, 7999568, 1114896, 262664, 2230304, 15760056, 12720235, 0.26F,
                            0.68F, 0.4F);
            case HELLBOY ->
                    new PeepSky.Palette(
                            16727406, 11539004, 1967116, 655878, 4720670, 16752312, 16724821, 0.52F,
                            1.0F, 0.85F);
            case BEAMER_BOY ->
                    new PeepSky.Palette(
                            14241744, 8002188, 1181220, 328468, 2363456, 15780095, 11554303, 0.34F,
                            0.9F, 0.9F);
            case SPOTLIGHT ->
                    new PeepSky.Palette(
                            12728968, 7999568, 1442831, 393739, 2492966, 15909598, 12720235, 0.32F,
                            0.85F, 0.8F);
            case THE_BRIGHTSIDE ->
                    new PeepSky.Palette(
                            16740277, 12728960, 4198974, 2886188, 15764620, 16769224, 16756848,
                            0.48F, 1.0F, 0.45F);
            case SUNLIGHT_ON_YOUR_SKIN ->
                    new PeepSky.Palette(
                            16751540, 13654656, 5247556, 3017264, 16746144, 16770760, 16760944,
                            0.4F, 1.0F, 0.6F);
            case LIFE_IS_BEAUTIFUL ->
                    new PeepSky.Palette(
                            16740250, 13117530, 2492440, 1049610, 5904440, 16765660, 16742550,
                            0.46F, 1.0F, 0.9F);
            case FALLING_DOWN ->
                    new PeepSky.Palette(
                            14711487, 9186424, 1509924, 525846, 2889272, 16046320, 15043792, 0.32F,
                            0.9F, 1.0F);
            case SAVE_THAT_SHIT ->
                    new PeepSky.Palette(
                            16732080, 13639802, 2229532, 656144, 4721718, 16766704, 16732080, 0.48F,
                            1.0F, 1.0F);
            case AWFUL_THINGS ->
                    new PeepSky.Palette(
                            9050714, 4852272, 786952, 196868, 2754592, 14701982, 12720235, 0.3F,
                            0.62F, 0.45F);
            case WITCHBLADES ->
                    new PeepSky.Palette(
                            13127167, 8002216, 1705520, 656408, 4593238, 15255807, 14835936, 0.44F,
                            0.95F, 0.85F);
            case FOUR_GOLD_CHAINS ->
                    new PeepSky.Palette(
                            16736170, 12720235, 2033430, 656392, 4854832, 16770736, 16732080, 0.5F,
                            1.0F, 1.0F);
            case GHOST_BOY ->
                    new PeepSky.Palette(
                            12352724, 7617154, 2362428, 1378338, 5121634, 14735600, 13671666, 0.42F,
                            0.85F, 0.7F);
            case GHOST_GIRL ->
                    new PeepSky.Palette(
                            15774948, 12093128, 4991604, 3415640, 14198482, 16773370, 16769268,
                            0.46F, 1.0F, 0.6F);
        };
    }

    void ready(SkyPaint skyPaint, ExecutorService executorService) {
        if (this.tattoos == null) {
            PeepSky.Built built2 = this.built;
            if (built2 != null && built2.generation() != this.generation) {
                this.built = null;
                built2 = null;
            }
            if (built2 == null) {
                if (!this.building) {
                    this.building = true;
                    int generation2 = this.generation;
                    executorService.submit(
                            () -> {
                                if (generation2 == this.generation) {
                                    try {
                                        PeepSky.Built built3 = build(generation2);
                                        if (generation2 == this.generation) {
                                            this.built = built3;
                                        }
                                    } catch (RuntimeException | Error exception) {
                                        LOGGER.warn(
                                                "[EFN] Sky easter egg textures couldn't be drawn",
                                                (Throwable) exception);
                                    }
                                }
                            });
                }
            } else {
                SkyImage[] skyImages = new SkyImage[built2.tattoos().length];
                SkyImage[] skyImages2 = new SkyImage[skyImages.length];
                SkyImage[] skyImages3 = new SkyImage[skyImages.length];
                SkyImage[] skyImages4 = new SkyImage[skyImages.length];
                SkyImage[] skyImages5 = new SkyImage[built2.art().length];
                try {
                    for (int j = 0; j < skyImages.length; j++) {
                        skyImages[j] =
                                skyPaint.upload(
                                        "peep_tattoo_" + j,
                                        built2.widths()[j],
                                        built2.heights()[j],
                                        built2.tattoos()[j]);
                        skyImages2[j] =
                                skyPaint.upload(
                                        "peep_rim_" + j,
                                        built2.widths()[j],
                                        built2.heights()[j],
                                        built2.rims()[j]);
                        skyImages3[j] =
                                skyPaint.upload(
                                        "peep_gold_" + j,
                                        built2.widths()[j],
                                        built2.heights()[j],
                                        built2.golds()[j]);
                        skyImages4[j] =
                                skyPaint.upload(
                                        "peep_veil_" + j,
                                        built2.widths()[j],
                                        built2.heights()[j],
                                        built2.veils()[j]);
                    }
                    for (int k = 0; k < skyImages5.length; k++) {
                        PeepArt.Art art2 = built2.art()[k];
                        skyImages5[k] =
                                skyPaint.upload(
                                        "peep_fx_" + k,
                                        art2.width(),
                                        art2.height(),
                                        built2.artLevels()[k]);
                    }
                } catch (RuntimeException | Error exception2) {
                    closeAll(skyImages);
                    closeAll(skyImages2);
                    closeAll(skyImages3);
                    closeAll(skyImages4);
                    closeAll(skyImages5);
                    throw exception2;
                }
                this.kinds = built2.kinds();
                this.rims = skyImages2;
                this.golds = skyImages3;
                this.veils = skyImages4;
                this.art = skyImages5;
                this.tattoos = skyImages;
                this.built = null;
                this.building = false;
            }
        }
    }

    boolean holding() {
        return this.tattoos != null || this.building || this.built != null;
    }

    private static void closeAll(SkyImage[] skyImages) {
        if (skyImages != null) {
            for (SkyImage skyImage : skyImages) {
                if (skyImage != null) {
                    skyImage.close();
                }
            }
        }
    }

    void close() {
        closeAll(this.tattoos);
        closeAll(this.rims);
        closeAll(this.golds);
        closeAll(this.veils);
        closeAll(this.art);
        this.tattoos = null;
        this.rims = null;
        this.golds = null;
        this.veils = null;
        this.art = null;
        this.generation++;
        this.built = null;
        this.building = false;
        this.restart();
    }

    void restart() {
        Arrays.fill(this.weight, 0.0F);
        Arrays.fill(this.shown, 0.0F);
        Arrays.fill(this.songAge, 0.0F);
        this.song = null;
        this.stageU = 0.0F / 0.0F;
        this.fall = 0.0F;
        this.fallFade = 0.0F;
        this.fresh = true;
        Arrays.fill(this.anchorRun, -2147483648);
        Arrays.fill(this.pickRun, -2147483648);
    }

    float anchor(int i, int j) {
        if (this.anchorRun[i] != j) {
            this.anchorRun[i] = j;
            this.anchorU[i] = Float.isNaN(this.stageU) ? this.viewU : this.stageU;
        }
        return this.anchorU[i];
    }

    float anchored(int i, int j) {
        return this.anchorRun[i] == j ? this.anchorU[i] : 0.0F / 0.0F;
    }

    int picked(int i, int j) {
        return this.pickRun[i] == j ? this.pick[i] : -1;
    }

    void pick(int i, int j, int k) {
        this.pickRun[i] = j;
        this.pick[i] = (byte) k;
    }

    void lyrics(
            float f,
            float g,
            float h,
            float lineHalfTall2,
            float f2,
            float titleUp2,
            float g2,
            float titleHalfTall2) {
        this.lyricsShown = !Float.isNaN(g);
        this.lyricU = wrap(f);
        this.titleU = wrap(f2);
        this.lyricUp = this.lyricsShown ? g : 28.0F;
        this.lineHalfTall = lineHalfTall2;
        this.titleUp = titleUp2;
        this.titleHalfTall = titleHalfTall2;
        this.wantLineWide = this.lyricsShown ? h : 0.0F;
        this.wantTitleWide = this.lyricsShown ? g2 : 0.0F;
    }

    void update(float f, PeepSong song2, float g, float h, float f2, float viewUp2, float pulse2) {
        this.song = song2;
        boolean bl = g <= 0.0F || this.fresh;
        this.fresh = false;
        float g2 = f / 2.0F;
        float h2 = 0.0F;
        for (int m = 0; m < this.weight.length; m++) {
            boolean bl2 = this.song != null && this.song.ordinal() == m;
            float f3 = bl2 ? 1.0F : 0.0F;
            this.weight[m] =
                    bl
                            ? f3
                            : (this.weight[m] < f3
                                    ? Math.min(f3, this.weight[m] + g2)
                                    : Math.max(f3, this.weight[m] - g2));
            this.shown[m] = smootherstep(this.weight[m]);
            h2 += this.shown[m];
            this.songAge[m] =
                    bl2
                            ? Math.min(this.songAge[m] + f, 100000.0F)
                            : (this.weight[m] <= 0.0F ? 0.0F : this.songAge[m]);
        }
        if (h2 > 1.0F) {
            int i = 0;
            while (i < this.shown.length) {
                int j = i++;
                this.shown[j] = this.shown[j] / h2;
            }
        }
        this.viewU = wrap(h);
        this.aimU = wrap(f2);
        this.viewUp = viewUp2;
        if (!this.lyricsShown) {
            this.lyricU = this.viewU;
            this.titleU = this.viewU;
        }
        float g3 = bl ? 1.0F : Math.min(1.0F, f * 3.0F);
        this.lineHalfWide = this.lineHalfWide + (this.wantLineWide - this.lineHalfWide) * g3;
        this.titleHalfWide = this.titleHalfWide + (this.wantTitleWide - this.titleHalfWide) * g3;
        this.pulse = pulse2;
        this.stageU =
                !Float.isNaN(this.stageU) && !bl
                        ? wrap(
                                this.stageU
                                        + wrapDelta(this.viewU - this.stageU)
                                                * Math.min(1.0F, f * 0.5F))
                        : this.viewU;
        float h3 = this.w(PeepSong.FALLING_DOWN);
        if (h3 > 0.001F) {
            this.fall = MathHelper.fractionalPart(this.fall + f * 0.014285714F * h3);
        }
        float f4 =
                this.fallFade =
                        this.song == PeepSong.FALLING_DOWN
                                ? Math.min(1.0F, this.fallFade + g2)
                                : Math.max(0.0F, this.fallFade - g2);
        if (this.fallFade <= 0.0F) {
            this.fall = 0.0F;
        }
    }

    float w(PeepSong peepSong) {
        return this.shown[peepSong.ordinal()];
    }

    float age(PeepSong peepSong) {
        return this.songAge[peepSong.ordinal()];
    }

    void draw(SkyPaint skyPaint, float f, float time2) {
        this.time = time2;
        float smootherstep = smootherstep(MathHelper.clamp(f / 0.7F, 0.0F, 1.0F));
        float smootherstep2 = smootherstep(MathHelper.clamp((f - 0.7F) / 0.3F, 0.0F, 1.0F));
        if (smootherstep <= 0.0F) {
            this.horizonShare = 0.0F;
            PeepWorld.horizon(this.horizonRgb, 0.0F);
        } else {
            this.alpha = smootherstep2;
            this.blendLook();
            float g = -0.78F;
            float h = 1.27F;
            float f2 = MathHelper.lerp(smootherstep, h, g - 0.05F);
            this.horizon(f2, smootherstep, time2);
            for (int i = 0; i < 14; i++) {
                float g2 = MathHelper.lerp(i / 14.0F, f2, h);
                float h2 = MathHelper.lerp((i + 1) / 14.0F, f2, h);
                int shade = this.shade(g2 - f2, g2, 0.72F);
                int shade2 = this.shade(h2 - f2, h2, 0.72F);
                for (int m = 0; m < 64; m++) {
                    float f3 = m / 64.0F;
                    float g3 = (m + 1) / 64.0F;
                    skyPaint.gradient(false, f3, g3, g2, h2, shade, shade2);
                }
            }
            float h3 = 0.09F;
            int tint = tint(0.85F, this.look.topColor);
            int tint2 = tint(0.0F, this.look.topColor);
            for (int p = 0; p < 64; p++) {
                float f4 = p / 64.0F;
                float g4 = (p + 1) / 64.0F;
                skyPaint.gradient(true, f4, g4, f2 - h3, f2, tint2, tint);
                skyPaint.gradient(
                        true, f4, g4, f2, f2 + h3 * 2.2F, tint(0.55F, this.look.rimColor), tint2);
            }
            boolean bl = this.art != null && smootherstep2 > 0.0F;
            if (bl) {
                PeepEffects.skyLight(this, skyPaint, smootherstep, time2);
            }
            this.horizonGlow(skyPaint, f2, smootherstep, time2);
            if (this.pulse > 0.0F && this.lyricsShown) {
                skyPaint.glow(
                        this.lyricU,
                        elevationToY(this.lyricUp + 1.0F),
                        0.4F,
                        16732080,
                        0.18F * this.pulse * smootherstep2);
            }
            float fall2 = this.fall;
            float smootherstep3 = smootherstep(this.fallFade);
            float h4 = 1.0F - 0.5F * this.w(PeepSong.CRY_ALONE);
            for (int q = 0; q < 12; q++) {
                float fractionalPart =
                        MathHelper.fractionalPart(
                                q * 0.2718F
                                        + time2
                                                * (0.004F + 0.002F * (q % 3))
                                                * (q % 2 == 0 ? 1.0F : -1.0F));
                float f5 =
                        MathHelper.lerp(
                                MathHelper.fractionalPart(q * 0.5713F + 0.21F), g + 0.45F, 0.67F);
                float g5 = 1.0F;
                if (smootherstep3 > 0.0F) {
                    float h5 = (0.72F - f5) * 60.0F;
                    float fractionalPart2 = MathHelper.fractionalPart((h5 - -1.5F) / 87.5F - fall2);
                    f5 =
                            elevationToY(
                                    MathHelper.lerp(
                                            smootherstep3, h5, -1.5F + fractionalPart2 * 87.5F));
                    g5 =
                            MathHelper.lerp(
                                    smootherstep3,
                                    1.0F,
                                    smooth(0.0F, 0.08F, fractionalPart2)
                                            * (1.0F - smooth(0.7F, 0.85F, fractionalPart2)));
                }
                if (!(f5 < f2 + 0.05F)) {
                    float f6 = 0.6F + 0.4F * MathHelper.sin(time2 * (0.35F + 0.07F * q) + q * 1.3F);
                    float g6 = 0.07F + 0.05F * (q % 4);
                    skyPaint.glow(
                            fractionalPart,
                            f5,
                            g6,
                            q % 3 == 0 ? this.look.deepColor : this.look.topColor,
                            0.16F
                                    * f6
                                    * smootherstep
                                    * this.look.light
                                    * (g5 * smooth(0.02F, 0.02F + g6, 0.72F - f5))
                                    * h4
                                    * (1.0F + 0.8F * this.pulse));
                }
            }
            for (int r = 0; r < 140; r++) {
                float fractionalPart3 = MathHelper.fractionalPart(r * 0.618034F);
                float h6 =
                        MathHelper.lerp(
                                MathHelper.fractionalPart(r * 0.41421F + 0.13F), g + 0.08F, 0.74F);
                float f7 = 1.0F;
                if (smootherstep3 > 0.0F) {
                    float g7 = (0.72F - h6) * 60.0F;
                    float fractionalPart4 = MathHelper.fractionalPart((g7 - -1.5F) / 87.5F - fall2);
                    h6 =
                            elevationToY(
                                    MathHelper.lerp(
                                            smootherstep3, g7, -1.5F + fractionalPart4 * 87.5F));
                    f7 =
                            MathHelper.lerp(
                                    smootherstep3,
                                    1.0F,
                                    smooth(0.0F, 0.03F, fractionalPart4)
                                            * (1.0F - smooth(0.9F, 0.99F, fractionalPart4)));
                }
                if (!(h6 < f2 + 0.01F)) {
                    float h7 =
                            0.35F
                                    + 0.65F
                                            * Math.max(
                                                    0.0F,
                                                    MathHelper.sin(
                                                            time2 * (0.9F + r % 7 * 0.31F)
                                                                    + r * 2.17F));
                    skyPaint.glow(
                            fractionalPart3,
                            h6,
                            0.004F + 0.0035F * (r % 4),
                            r % 5 == 0 ? 16766704 : 16773882,
                            Math.min(1.0F, 0.8F * h7 * this.look.starLight) * smootherstep * f7);
                }
            }
            boolean bl2 = smootherstep2 > 0.0F && this.tattoos != null;
            this.shownTattoos = 0;
            if (bl2) {
                this.layoutTattoos(smooth(0.35F, 1.0F, smootherstep2) * smootherstep2, time2);
            }
            if (bl) {
                PeepEffects.behind(this, skyPaint, time2);
            }
            if (bl2) {
                this.drawTattoos(skyPaint, time2);
            }
            if (bl) {
                PeepEffects.front(this, skyPaint, time2);
            }
        }
    }

    private void horizon(float f, float g, float h) {
        float f2 = 0.72F;
        int shade = this.shade(f2 - f, f2, f2);
        float g2 = 0.0F;
        if (Math.max(f, f2 - 0.42F) < f2) {
            float h2 = 0.85F + 0.15F * MathHelper.sin(h * 0.6F);
            g2 =
                    MathHelper.clamp(
                            this.look.glowStrength * g * h2 * (1.0F + 0.45F * this.pulse),
                            0.0F,
                            1.0F);
        }
        int glowColor2 = this.look.glowColor;
        int i = Math.min(255, Math.round((shade >> 16 & 0xFF) + (glowColor2 >> 16 & 0xFF) * g2));
        int j = Math.min(255, Math.round((shade >> 8 & 0xFF) + (glowColor2 >> 8 & 0xFF) * g2));
        int k = Math.min(255, Math.round((shade & 0xFF) + (glowColor2 & 0xFF) * g2));
        this.horizonRgb = i << 16 | j << 8 | k;
        this.horizonShare = 0.97F * MathHelper.clamp((f2 - f) / 0.1F, 0.0F, 1.0F);
        PeepWorld.horizon(this.horizonRgb, this.horizonShare);
    }

    private void horizonGlow(SkyPaint skyPaint, float f, float g, float h) {
        float f2 = Math.max(f, 0.30000004F);
        float g2 = 0.72F;
        float h2 = 0.76000005F;
        float f3 = 1.08F;
        float g3 = 1.32F;
        if (!(f2 >= g2)) {
            float h3 = 0.85F + 0.15F * MathHelper.sin(h * 0.6F);
            float f4 = this.look.glowStrength * g * h3 * (1.0F + 0.45F * this.pulse);
            int tint = tint(f4, this.look.glowColor);
            int tint2 = tint(f4 * 0.78F, this.look.glowColor);
            int tint3 = tint(0.0F, this.look.glowColor);
            for (int m = 0; m < 64; m++) {
                float g4 = m / 64.0F;
                float h4 = (m + 1) / 64.0F;
                skyPaint.gradient(true, g4, h4, f2, g2, tint3, tint);
                skyPaint.gradient(true, g4, h4, g2, h2, tint, tint);
                skyPaint.gradient(true, g4, h4, h2, f3, tint, tint2);
                skyPaint.gradient(true, g4, h4, f3, g3, tint2, tint3);
            }
        }
    }

    float lyricBox(float f, float g, float h, float f2) {
        if (!this.lyricsShown) {
            return 0.0F;
        } else {
            float g2 = 360.0F * MathHelper.cos(g * 0.017453292F);
            float h2 =
                    this.lineHalfWide <= 0.05F
                            ? 0.0F
                            : smooth(0.0F, 3.0F, this.lineHalfWide)
                                    * box(
                                            Math.abs(wrapDelta(f - this.lyricU)) * g2,
                                            g - this.lyricUp,
                                            this.lineHalfWide + h + 2.5F,
                                            this.lineHalfTall + f2 + 2.0F);
            float f3 =
                    this.titleHalfWide <= 0.05F
                            ? 0.0F
                            : smooth(0.0F, 3.0F, this.titleHalfWide)
                                    * box(
                                            Math.abs(wrapDelta(f - this.titleU)) * g2,
                                            g - this.titleUp,
                                            this.titleHalfWide + h + 2.5F,
                                            this.titleHalfTall + f2 + 2.0F);
            return Math.max(h2, f3);
        }
    }

    float clear(float f, float g) {
        float h = 1.0F - 1.0F * this.lyricBox(f, g, 0.8F, 0.8F);
        float f2 = g * 0.017453292F;
        float g2 = this.viewUp * 0.017453292F;
        float h2 =
                MathHelper.sin(f2) * MathHelper.sin(g2)
                        + MathHelper.cos(f2)
                                * MathHelper.cos(g2)
                                * MathHelper.cos((f - this.aimU) * 6.2831855F);
        if (h2 < 0.93F) {
            return h;
        } else {
            float f3 = (float) Math.acos(Math.min(1.0F, h2)) * 57.295776F;
            return h * smooth(9.0F, 20.0F, f3);
        }
    }

    private float aimClear(float f, float g, float h, float f2) {
        float g2 =
                Math.abs(wrapDelta(f - this.aimU))
                        * 360.0F
                        * MathHelper.cos((g + this.viewUp) * 0.5F * 0.017453292F);
        float h2 = Math.max(0.0F, g2 - h);
        float f3 = Math.max(0.0F, Math.abs(g - this.viewUp) - f2);
        return smooth(6.0F, 15.0F, (float) Math.sqrt(h2 * h2 + f3 * f3));
    }

    private float keepOut(float f, float g, float h, float f2) {
        return (1.0F - this.textRoom(f, g, f2, h)) * this.aimClear(f, g, f2, h);
    }

    private float textRoom(float f, float g, float h, float f2) {
        if (!this.lyricsShown) {
            return 0.0F;
        } else {
            float g2 = 360.0F * MathHelper.cos(g * 0.017453292F);
            float h2 = 0.0F;
            if (this.lineHalfWide > 0.05F) {
                float f3 = Math.abs(wrapDelta(f - this.lyricU)) * g2;
                float g3 = smooth(0.0F, 3.0F, this.lineHalfWide);
                h2 =
                        g3
                                * near(f3, this.lineHalfWide + h)
                                * nearUp(g - this.lyricUp, this.lineHalfTall + f2);
                if (g > this.lyricUp) {
                    h2 = Math.max(h2, g3 * near(f3, 14.0F + h));
                }
            }
            if (this.titleHalfWide > 0.05F) {
                float h3 = Math.abs(wrapDelta(f - this.titleU)) * g2;
                float f4 = smooth(0.0F, 3.0F, this.titleHalfWide);
                float g4 =
                        g > this.titleUp ? 1.0F : nearUp(g - this.titleUp, this.titleHalfTall + f2);
                h2 = Math.max(h2, f4 * near(h3, this.titleHalfWide + h) * g4);
            }
            return h2;
        }
    }

    private static float near(float f, float g) {
        return 1.0F - smooth(g + 3.0F, g + 3.0F + 5.0F, Math.abs(f));
    }

    private static float nearUp(float f, float g) {
        return f >= 0.0F ? near(f, g) : 1.0F - smooth(g + 1.0F, g + 1.0F + 1.5F, -f);
    }

    private static float box(float f, float g, float h, float f2) {
        return (1.0F - smooth(h * 0.85F, h, Math.abs(f)))
                * (1.0F - smooth(f2 * 0.8F, f2, Math.abs(g)));
    }

    private static float artShare(int i) {
        return 1.0F - 52.0F / i;
    }

    int kindAt(int i) {
        return this.kinds[this.tTex[i]];
    }

    private void layoutTattoos(float f, float g) {
        int i = this.tattoos.length;
        float h = this.w(PeepSong.CRY_ALONE);
        float f2 = this.w(PeepSong.GHOST_BOY);
        float g2 = this.w(PeepSong.GHOST_GIRL);
        float h2 = this.w(PeepSong.SPOTLIGHT);
        float f3 = MathHelper.lerp(0.5F, 1.0F, this.look.light);
        float g3 = 1.0F + 0.1F * this.pulse;
        float smootherstep = smootherstep(this.fallFade);
        int j = 0;
        for (int k = 0; k < i; k++) {
            SkyImage tattoo = this.tattoos[k];
            int m = this.kinds[k] % ELEVATION.length;
            float h3 = f * (1.0F - h) * f3;
            if (!(h3 <= 0.003F)) {
                float f4 = 0.85F + 0.15F * MathHelper.sin(g * 2.8F + k);
                float g4 = (float) tattoo.width() / tattoo.height();
                float fractionalPart = MathHelper.fractionalPart((float) k / i + g * 0.02F);
                float h4 = ELEVATION[m] + 2.2F * MathHelper.sin(g * 0.8F + k * 1.7F);
                float f5 = 1.0F;
                if (smootherstep > 0.0F) {
                    float fractionalPart2 =
                            MathHelper.fractionalPart((h4 - 12.0F) / 52.0F - this.fall);
                    h4 = MathHelper.lerp(smootherstep, h4, 12.0F + fractionalPart2 * 52.0F);
                    f5 =
                            MathHelper.lerp(
                                    smootherstep,
                                    1.0F,
                                    smooth(0.0F, 0.12F, fractionalPart2)
                                            * (1.0F - smooth(0.88F, 0.98F, fractionalPart2)));
                }
                if (f2 > 0.0F) {
                    float g5 = (g * 0.42F + k * 3.5F) / 6.2831855F + 0.25F;
                    int n = MathHelper.floor(g5);
                    int o = k % HAUNT_AZIMUTH.length;
                    float h5 = (HAUNT_HIGH[o] ? 41.0F : 12.5F) + 6.0F * hash01(k, n, 1);
                    float f6 =
                            this.anchor(GHOST_SLOTS + k, n)
                                    + (HAUNT_AZIMUTH[o] + 8.0F * (hash01(k, n, 2) - 0.5F))
                                            / (360.0F * MathHelper.cos(h5 * 0.017453292F));
                    fractionalPart = wrap(fractionalPart + wrapDelta(f6 - fractionalPart) * f2);
                    h4 = MathHelper.lerp(f2, h4, h5);
                }
                float g6 =
                        SIZE[m]
                                * (0.94F + 0.06F * MathHelper.sin(g * 2.0F + k))
                                * (1.0F + 0.15F * f2);
                float keepOut =
                        this.keepOut(
                                fractionalPart,
                                h4,
                                g6 * 0.5F * artShare(tattoo.height()),
                                g6 * g4 * 0.5F * artShare(tattoo.width()));
                j =
                        this.place(
                                j,
                                k,
                                fractionalPart,
                                h4,
                                g6 / 60.0F,
                                g4,
                                0.1F * MathHelper.sin(g * 0.7F + k * 2.1F),
                                h3 * (f5 * (keepOut * smooth(0.15F, 0.45F, keepOut))) * f4,
                                g,
                                g2,
                                f2,
                                h2,
                                g3);
            }
        }
        this.shownTattoos = j;
    }

    float clearOfTattoos(float f, float g, float h) {
        float f2 = 1.0F;
        float g2 = 360.0F * MathHelper.cos(g * 0.017453292F);
        for (int i = 0; i < this.shownTattoos && f2 > 0.0F; i++) {
            if (!(this.tGlow[i] <= 0.05F)) {
                float h2 = (0.72F - this.ty[i]) * 1.0471976F * 57.295776F;
                float f3 = Math.abs(wrapDelta(f - this.tu[i])) * g2;
                float g3 = this.tw[i] * 30.0F * 0.8F;
                float h3 = this.th[i] * 30.0F * 0.8F;
                float f4 = Math.max(0.0F, f3 - g3 - h);
                float g4 = Math.max(0.0F, Math.abs(g - h2) - h3 - h);
                if (!(f4 > 1.5F) && !(g4 > 1.5F)) {
                    f2 *=
                            1.0F
                                    - Math.min(1.0F, this.tGlow[i])
                                            * (1.0F
                                                    - smooth(
                                                            0.0F,
                                                            1.5F,
                                                            (float) Math.sqrt(f4 * f4 + g4 * g4)));
                }
            }
        }
        return f2;
    }

    private void drawTattoos(SkyPaint skyPaint, float f) {
        int shownTattoos2 = this.shownTattoos;
        float g = this.w(PeepSong.GHOST_BOY);
        float h = this.w(PeepSong.GHOST_GIRL);
        float f2 = this.w(PeepSong.SPOTLIGHT);
        float g2 = this.w(PeepSong.FOUR_GOLD_CHAINS);
        float h2 = this.w(PeepSong.WITCHBLADES);
        float f3 = 1.0F + 1.3F * this.pulse;
        int i =
                mix(
                        mix(mix(16732080, 16757575, g2 * 0.4F), 14208255, g * 0.7F),
                        16763118,
                        h * 0.3F);
        for (int k = 0; k < shownTattoos2; k++) {
            skyPaint.glow(
                    this.tu[k],
                    this.ty[k],
                    Math.max(this.th[k], this.tw[k]) * 0.6F,
                    i,
                    0.22F
                            * this.tGlow[k]
                            * f3
                            * (1.0F + 0.6F * this.tLit[k])
                            * (1.0F + 0.5F * g * this.tGhost[k]));
        }
        int j = mix(mix(16732080, 16740552, h), 13150463, g);
        float g3 = Math.min(1.0F, h + g);
        float h3 = 1.0F - 0.7F * g;
        float f4 = smooth(0.7F, 1.0F, this.look.light);
        for (int n = 0; n < shownTattoos2; n++) {
            int kind = this.kindAt(n);
            boolean bl = kind == 0;
            float filled = this.filled(n, g2, g);
            float g4 =
                    this.tAlpha[n]
                            * (bl ? 0.3F : h3 * (1.0F - bodyBack(kind) * filled))
                            * (1.0F - g3 * (1.0F - smooth(0.18F, 0.45F, this.tAlpha[n])));
            float h4 =
                    smooth(0.2F, 0.8F, this.tAlpha[n])
                            * f4
                            * 0.7F
                            * (1.0F - 0.5F * g2)
                            * (1.0F - filled)
                            * (1.0F - 0.7F * this.tLit[n])
                            * (1.0F - 0.5F * h);
            int m = bl ? mix(16732080, 16740296, h4) : mix(mix(j, 16732080, filled), 16777215, h4);
            if (g > 0.0F) {
                m = mix(m, mix(10121471, 14203135, this.tGhost[n]), g);
                g4 *= 1.0F - g * (1.0F - this.haunt(n));
            }
            if (g2 > 0.0F) {
                g4 *= 1.0F - 0.35F * g2;
                m = mix(m, 12720235, 0.4F * g2);
            }
            skyPaint.sprite(
                    this.tattoos[this.tTex[n]],
                    this.tu[n],
                    this.ty[n],
                    this.tw[n],
                    this.th[n],
                    this.tSpin[n],
                    tint(g4, m),
                    true);
        }
        if (g > 0.003F) {
            for (int q = 0; q < shownTattoos2; q++) {
                float f5 =
                        0.72F
                                * this.tAlpha[q]
                                * g
                                * smooth(0.1F, 0.35F, this.tAlpha[q])
                                * (1.0F - 0.08F * smooth(0.8F, 1.0F, this.tGhost[q]))
                                * this.haunt(q);
                if (!(f5 <= 0.003F)) {
                    int o = mix(11697407, 13145855, this.tGhost[q]);
                    skyPaint.sprite(
                            this.veils[this.tTex[q]],
                            this.tu[q],
                            this.ty[q],
                            this.tw[q],
                            this.th[q],
                            this.tSpin[q],
                            tint(f5, o),
                            true);
                }
            }
        }
        if (g2 > 0.003F) {
            for (int t = 0; t < shownTattoos2; t++) {
                float g5 = g2 * smooth(0.12F, 0.6F, this.tGlow[t]);
                if (!(g5 <= 0.003F)) {
                    skyPaint.sprite(
                            this.golds[this.tTex[t]],
                            this.tu[t],
                            this.ty[t],
                            this.tw[t],
                            this.th[t],
                            this.tSpin[t],
                            tint(0.95F * g5, 4859392),
                            false);
                    skyPaint.sprite(
                            this.golds[this.tTex[t]],
                            this.tu[t],
                            this.ty[t],
                            this.tw[t],
                            this.th[t],
                            this.tSpin[t],
                            tint(0.9F * g5, 16769658),
                            true);
                }
            }
        }
        for (int u = 0; u < shownTattoos2; u++) {
            float filled2 = this.filled(u, g2, g);
            if (!(filled2 <= 0.003F)) {
                float h5 =
                        (this.kindAt(u) == 9 ? 0.92F : 0.8F)
                                * Math.min(1.0F, filled2 * 1.35F)
                                * this.tGlow[u];
                skyPaint.sprite(
                        this.golds[this.tTex[u]],
                        this.tu[u],
                        this.ty[u],
                        this.tw[u],
                        this.th[u],
                        this.tSpin[u],
                        tint(h5, 16749268),
                        true);
            }
        }
        int p = mix(16732080, 12099839, g);
        for (int w = 0; w < shownTattoos2; w++) {
            float f6 = f2 * this.tLit[w] * 0.35F;
            float g6 = g * this.tGhost[w] * 0.26F;
            float h6 = this.kindAt(w) == 0 ? 0.6F * (1.0F - g2) * (1.0F - g) : 0.0F;
            float f7 = f6 + g6 + h6;
            if (!(f7 <= 0.003F)) {
                int r =
                        mix(
                                p,
                                average(16743115, f6, 15916287, g6, 16732080, h6),
                                smooth(0.2F, 0.8F, this.tGlow[w]));
                skyPaint.sprite(
                        this.rims[this.tTex[w]],
                        this.tu[w],
                        this.ty[w],
                        this.tw[w],
                        this.th[w],
                        this.tSpin[w],
                        tint(Math.min(1.0F, f7) * this.tGlow[w], r),
                        true);
            }
        }
        if (h2 > 0.0F) {
            PeepEffects.orbits(this, skyPaint, f, h2);
        }
        if (g2 > 0.0F) {
            PeepEffects.goldSparkles(this, skyPaint, f, g2);
        }
    }

    private static float bodyBack(int i) {
        return i == 9 ? 0.45F : 0.25F;
    }

    private float haunt(int i) {
        boolean bl = HAUNT_HIGH[this.tTex[i] % HAUNT_HIGH.length];
        return (bl ? 1.0F : 0.8F) * (1.0F - 0.4F * this.hazeAt(this.ty[i]));
    }

    private float filled(int i, float f, float g) {
        float h = this.kindAt(i) == 9 ? 0.6F : 0.0F;
        return h * (1.0F - f) * (1.0F - g);
    }

    private int place(
            int i,
            int j,
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
            float g4) {
        if (i >= MAX_SHOWN) {
            return i;
        } else {
            if (g3 > 0.0F) {
                f +=
                        g3
                                * 0.35F
                                * (MathHelper.sin(f3 * 2.3F + j * 1.1F)
                                        + 0.5F * MathHelper.sin(f3 * 3.7F + j * 2.9F))
                                / (360.0F * Math.max(0.2F, MathHelper.cos(g * 0.017453292F)));
                g +=
                        g3
                                * 0.3F
                                * (MathHelper.sin(f3 * 2.9F + j * 1.7F)
                                        + 0.5F * MathHelper.sin(f3 * 4.3F + j));
                h2 *= 1.0F - g3 * 0.15F * (0.5F + 0.5F * MathHelper.sin(f3 * 3.1F + j));
            }
            float h4 = 1.0F;
            if (h3 > 0.0F) {
                float f5 = 0.5F + 0.5F * MathHelper.sin(f3 * 0.42F + j * 3.5F);
                h4 = smootherstep(smooth(0.38F, 0.68F, f5));
                g += h3 * 1.5F * f5;
            }
            float g5 = f4 > 0.0F ? f4 * PeepEffects.spotlightOn(this, f, g) : 0.0F;
            float h5 = g4 * (1.0F + 0.06F * g5);
            this.tTex[i] = j;
            this.tu[i] = wrap(f);
            this.ty[i] = elevationToY(g);
            this.th[i] = h * h5;
            this.tw[i] = h * f2 * h5;
            this.tSpin[i] = g2;
            this.tGhost[i] = h4;
            this.tLit[i] = g5;
            this.tAlpha[i] = Math.min(h2 * MathHelper.lerp(h3, 1.0F, h4 * 0.85F), 1.0F - 0.3F * h3);
            this.tGlow[i] = h2 * MathHelper.lerp(h3, 1.0F, h4);
            return i + 1;
        }
    }

    private void blendLook() {
        float[] rgb2 = this.look.rgb;
        Arrays.fill(rgb2, 0.0F);
        float f = 1.0F;
        float g = 0.0F;
        float h = 0.0F;
        float f2 = 0.0F;
        for (PeepSong peepSong : PeepSong.ALL) {
            float g2 = this.shown[peepSong.ordinal()];
            if (!(g2 <= 0.0F)) {
                f -= g2;
                PeepSky.Palette palette = PALETTES[peepSong.ordinal()];
                add(rgb2, palette, g2);
                g += g2 * palette.glowStrength();
                h += g2 * palette.light();
                f2 += g2 * palette.starLight();
            }
        }
        f = Math.max(0.0F, f);
        add(rgb2, BASE, f);
        this.look.glowStrength = g + f * BASE.glowStrength();
        this.look.light = h + f * BASE.light();
        this.look.starLight = f2 + f * BASE.starLight();
        this.look.topColor = packed(rgb2, 0);
        this.look.deepColor = packed(rgb2, 1);
        this.look.duskColor = packed(rgb2, 2);
        this.look.nightColor = packed(rgb2, 3);
        this.look.hazeColor = packed(rgb2, 4);
        this.look.hazeLight =
                MathHelper.clamp(
                        (0.6F * luma(this.look.hazeColor)
                                        + this.look.glowStrength * luma(packed(rgb2, 6)))
                                / 255.0F,
                        0.0F,
                        1.0F);
        this.look.rimColor = packed(rgb2, 5);
        this.look.glowColor = packed(rgb2, 6);
    }

    private static void add(float[] floats, PeepSky.Palette palette, float f) {
        if (!(f <= 0.0F)) {
            add(floats, 0, palette.top(), f);
            add(floats, 1, palette.deep(), f);
            add(floats, 2, palette.dusk(), f);
            add(floats, 3, palette.night(), f);
            add(floats, 4, palette.haze(), f);
            add(floats, 5, palette.rim(), f);
            add(floats, 6, palette.glow(), f);
        }
    }

    private static void add(float[] floats, int i, int j, float f) {
        int k = i * 3;
        floats[k] += f * (j >> 16 & 0xFF);
        int m = i * 3 + 1;
        floats[m] += f * (j >> 8 & 0xFF);
        int n = i * 3 + 2;
        floats[n] += f * (j & 0xFF);
    }

    private static int packed(float[] floats, int i) {
        return MathHelper.clamp(Math.round(floats[i * 3]), 0, 255) << 16
                | MathHelper.clamp(Math.round(floats[i * 3 + 1]), 0, 255) << 8
                | MathHelper.clamp(Math.round(floats[i * 3 + 2]), 0, 255);
    }

    float hazeAt(float f) {
        return MathHelper.clamp(1.0F - Math.max(0.0F, 0.72F - f) / 0.4F, 0.0F, 1.0F)
                * this.look.hazeLight;
    }

    private static float luma(int i) {
        return 0.2126F * (i >> 16 & 0xFF) + 0.7152F * (i >> 8 & 0xFF) + 0.0722F * (i & 0xFF);
    }

    private int shade(float f, float g, float h) {
        float f2 = Math.max(0.0F, f);
        int i =
                f2 < 0.05F
                        ? mix(this.look.topColor, this.look.deepColor, f2 / 0.05F)
                        : (f2 < 0.3F
                                ? mix(
                                        this.look.deepColor,
                                        this.look.duskColor,
                                        (f2 - 0.05F) / 0.25F)
                                : mix(
                                        this.look.duskColor,
                                        this.look.nightColor,
                                        Math.min(1.0F, (f2 - 0.3F) / 0.5F)));
        float g2 = MathHelper.clamp(1.0F - Math.max(0.0F, h - g) / 0.4F, 0.0F, 1.0F);
        i = mix(i, this.look.hazeColor, g2 * 0.6F);
        return tint(f2 < 0.02F ? 0.92F : 0.97F, i);
    }

    static float heartbeat(float f) {
        return Math.max(beat(f, 0.17F), 0.6F * beat(f - 0.3F, 0.42F));
    }

    private static float beat(float f, float g) {
        if (f < 0.0F) {
            return 0.0F;
        } else {
            return f < 0.14F ? smooth(0.0F, 0.14F, f) : (float) Math.exp(-(f - 0.14F) / g);
        }
    }

    private static int average(int i, float f, int j, float g, int k, float h) {
        float f2 = Math.max(f + g + h, 1.0E-4F);
        int m =
                Math.round(
                        ((i >> 16 & 0xFF) * f + (j >> 16 & 0xFF) * g + (k >> 16 & 0xFF) * h) / f2);
        int n = Math.round(((i >> 8 & 0xFF) * f + (j >> 8 & 0xFF) * g + (k >> 8 & 0xFF) * h) / f2);
        int o = Math.round(((i & 0xFF) * f + (j & 0xFF) * g + (k & 0xFF) * h) / f2);
        return MathHelper.clamp(m, 0, 255) << 16
                | MathHelper.clamp(n, 0, 255) << 8
                | MathHelper.clamp(o, 0, 255);
    }

    static float elevationToY(float f) {
        return 0.72F - f * 0.017453292F / 1.0471976F;
    }

    static float hash01(int i) {
        int j = i * -1640531535;
        j ^= j >>> 15;
        j *= -2048144777;
        j ^= j >>> 13;
        j *= -1028477379;
        j ^= j >>> 16;
        return (j >>> 8) * 5.9604645E-8F;
    }

    static float hash01(int i, int j, int k) {
        return hash01(i * 73856093 ^ j * 19349663 ^ k * 83492791);
    }

    static int tint(float f, int i) {
        return ColorHelper.withAlpha(MathHelper.clamp(f, 0.0F, 1.0F), i);
    }

    static int mix(int i, int j, float f) {
        return ColorHelper.lerp(MathHelper.clamp(f, 0.0F, 1.0F), i, j) & 16777215;
    }

    static float smootherstep(float f) {
        return f * f * f * (f * (f * 6.0F - 15.0F) + 10.0F);
    }

    static float smooth(float f, float g, float h) {
        float f2 = MathHelper.clamp((h - f) / (g - f), 0.0F, 1.0F);
        return f2 * f2 * (3.0F - 2.0F * f2);
    }

    static float wrap(float f) {
        return f - (float) Math.floor(f);
    }

    static float wrapDelta(float f) {
        return f - Math.round(f);
    }

    private static PeepSky.Built build(int i) {
        ArrayList list = new ArrayList();
        ArrayList list2 = new ArrayList();
        ArrayList list3 = new ArrayList();
        ArrayList list4 = new ArrayList();
        ArrayList list5 = new ArrayList();
        ArrayList list6 = new ArrayList();
        ArrayList list7 = new ArrayList();
        for (int j = 0; j < KINDS.length; j++) {
            BufferedImage bufferedImage = load(KINDS[j]);
            if (bufferedImage != null) {
                float f =
                        Math.min(
                                3.0F,
                                320.0F
                                        / Math.max(
                                                bufferedImage.getWidth(),
                                                bufferedImage.getHeight()));
                int k = Math.round(bufferedImage.getWidth() * f) + 52;
                int m = Math.round(bufferedImage.getHeight() * f) + 52;
                BufferedImage bufferedImage2 = new BufferedImage(k, m, 2);
                Graphics2D graphics = bufferedImage2.createGraphics();
                graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(
                        RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(bufferedImage, 26, 26, k - 52, m - 52, null);
                graphics.dispose();
                int[] rGB = bufferedImage2.getRGB(0, 0, k, m, null, 0, k);
                list.add(SkyMips.chain(neon(rGB, k, m), k, m));
                list2.add(SkyMips.chain(PeepArt.rim(rGB, k, m), k, m));
                list3.add(SkyMips.chain(PeepArt.gold(rGB, k, m), k, m));
                list4.add(SkyMips.chain(PeepArt.veil(rGB, k, m), k, m));
                list5.add(k);
                list6.add(m);
                list7.add(j);
            }
        }
        PeepArt.Art[] arts = PeepArt.drawAll();
        int[][][] intGrid = new int[arts.length][][];
        for (int n = 0; n < arts.length; n++) {
            intGrid[n] = SkyMips.chain(arts[n].argb(), arts[n].width(), arts[n].height());
        }
        return new PeepSky.Built(
                i,
                ((int[][][]) list.toArray(new int[0][][])),
                ((int[][][]) list2.toArray(new int[0][][])),
                ((int[][][]) list3.toArray(new int[0][][])),
                ((int[][][]) list4.toArray(new int[0][][])),
                list5.stream().mapToInt(o -> (Integer) o).toArray(),
                list6.stream().mapToInt(o -> (Integer) o).toArray(),
                list7.stream().mapToInt(o -> (Integer) o).toArray(),
                arts,
                intGrid);
    }

    private static BufferedImage load(String string) {
        try {
            BufferedImage bufferedImage;
            try (InputStream resourceAsStream =
                    PeepSky.class.getResourceAsStream(
                            "/assets/efn/textures/peep/" + string + ".png")) {
                if (resourceAsStream == null) {
                    LOGGER.warn("[EFN] Sky easter egg tattoo {} is missing", string);
                    return null;
                }
                BufferedImage bufferedImage2 = ImageIO.read(resourceAsStream);
                bufferedImage = bufferedImage2;
            }
            return bufferedImage;
        } catch (IOException exception) {
            LOGGER.warn("[EFN] Sky easter egg tattoo {} couldn't be read", string, exception);
            return null;
        }
    }

    private static int[] neon(int[] ints, int i, int j) {
        float[] floats = new float[i * j];
        for (int k = 0; k < floats.length; k++) {
            floats[k] = (ints[k] >>> 24) / 255.0F;
        }
        float[] floats2 = blur(floats, i, j, 9);
        float[] floats3 = new float[i * j];
        for (int m = 0; m < floats3.length; m++) {
            floats3[m] = smooth(0.6F, 0.9F, floats[m]);
        }
        float[] floats4 = blur(floats3, i, j, 7);
        float[] floats5 = blur(floats3, i, j, 1);
        int[] ints2 = new int[i * j];
        for (int n = 0; n < ints2.length; n++) {
            float f =
                    Math.max(
                            floats[n],
                            Math.min(1.0F, floats5[n] * 1.6F)
                                    * (1.0F - smooth(0.2F, 0.4F, floats4[n])));
            float g = Math.min(1.0F, floats2[n] * 2.6F);
            float h = smooth(0.7F, 1.0F, f) * (1.0F - 0.75F * smooth(0.45F, 0.8F, floats4[n]));
            ints2[n] = tint(Math.max(f, g * 0.85F), mix(16732080, 16766704, h));
        }
        return ints2;
    }

    static float[] blur(float[] floats, int i, int j, int k) {
        float[] floats2 = (float[]) floats.clone();
        float[] floats3 = new float[floats.length];
        for (int m = 0; m < 2; m++) {
            for (int n = 0; n < j; n++) {
                float f = 0.0F;
                for (int o = -k; o <= k; o++) {
                    f += floats2[n * i + MathHelper.clamp(o, 0, i - 1)];
                }
                for (int p = 0; p < i; p++) {
                    floats3[n * i + p] = f / (k * 2 + 1);
                    f +=
                            floats2[n * i + Math.min(i - 1, p + k + 1)]
                                    - floats2[n * i + Math.max(0, p - k)];
                }
            }
            for (int q = 0; q < i; q++) {
                float g = 0.0F;
                for (int r = -k; r <= k; r++) {
                    g += floats3[MathHelper.clamp(r, 0, j - 1) * i + q];
                }
                for (int t = 0; t < j; t++) {
                    floats2[t * i + q] = g / (k * 2 + 1);
                    g +=
                            floats3[Math.min(j - 1, t + k + 1) * i + q]
                                    - floats3[Math.max(0, t - k) * i + q];
                }
            }
        }
        return floats2;
    }

    static {
        for (PeepSong peepSong : PeepSong.ALL) {
            PALETTES[peepSong.ordinal()] = palette(peepSong);
        }
    }

    @Environment(EnvType.CLIENT)
    private record Built(
            int generation,
            int[][][] tattoos,
            int[][][] rims,
            int[][][] golds,
            int[][][] veils,
            int[] widths,
            int[] heights,
            int[] kinds,
            PeepArt.Art[] art,
            int[][][] artLevels) {}

    @Environment(EnvType.CLIENT)
    private static final class Look {

        final float[] rgb = new float[21];

        float glowStrength;

        float light;

        float starLight;

        float hazeLight;

        int topColor;

        int deepColor;

        int duskColor;

        int nightColor;

        int hazeColor;

        int rimColor;

        int glowColor;

        private Look() {}
    }

    @Environment(EnvType.CLIENT)
    private record Palette(
            int top,
            int deep,
            int dusk,
            int night,
            int haze,
            int rim,
            int glow,
            float glowStrength,
            float light,
            float starLight) {}
}
