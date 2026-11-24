package net.efn.client.module.impl.render;

import net.efn.client.module.impl.client.SpotifyHudModule;
import net.efn.client.util.spotify.Lyrics;
import net.efn.client.util.spotify.NowPlaying;
import net.efn.client.util.spotify.SyncedLyrics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Environment(EnvType.CLIENT)
public final class SkyLyrics {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final SkyLyrics INSTANCE = new SkyLyrics();

    private static final String FONT = "/assets/efn/fonts/manrope-bold.ttf";

    private static final long LEAD_MS = 200L;

    private static final float LINE_DEGREES = 7.0F;

    private static final float TITLE_DEGREES = 4.0F;

    private static final float TITLE_GAP_DEGREES = 0.6F;

    private static final float MAX_WIDTH_DEGREES = 140.0F;

    private static final float RISE_DEGREES = 1.6F;

    private static final float FADE_RATE = 2.6F;

    private static final float FOLLOW_RATE = 2.2F;

    private static final float LINE_PX = 96.0F;

    private static final float TITLE_PX = 64.0F;

    private static final float GLOW_ALPHA = 0.8F;

    private static final int PAD = 28;

    private static final int MAX_TEXTURE = 4096;

    private static final int BLUR = 10;

    private static final int SEGMENTS = 32;

    private static final int FALLBACK_GLOW = 9063935;

    private static final float EMOJI_SCALE = 1.0F;

    private static final float EMOJI_GAP_DEGREES = 0.8F;

    private static final long EMOJI_COOLDOWN_NS = 5000000000L;

    private static final float WORD_EMOJI_EM = 1.6F;

    private static final float BEHIND_SCALE = 2.8F;

    private static final float BEHIND_ALPHA = 0.32F;

    private static final float PEEP_IN_SECONDS = 5.0F;

    private static final float PEEP_OUT_SECONDS = 1.8F;

    private static final float PEEP_KEEP_SECONDS = 30.0F;

    private static final double PEEP_CLOCK_WRAP = 21600.0;

    private static final int PEEP_LINE = -24876;

    private static final int PEEP_TITLE = -34104;

    private static final int PEEP_GLOW = 16723349;

    private final ExecutorService raster =
            Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread = new Thread(runnable, "EFN Sky Lyrics");
                        thread.setDaemon(true);
                        return thread;
                    });

    private final SkyLyrics.Slot line = new SkyLyrics.Slot(7.0F, 96.0F);

    private final SkyLyrics.Slot title = new SkyLyrics.Slot(4.0F, 64.0F);

    private SkyCanvas canvas;

    private Font font;

    private boolean fontMissing;

    private boolean failed;

    private final Map<SkyEmoji, SkyTexture> emojiTextures = new EnumMap<>(SkyEmoji.class);

    private final Set<SkyEmoji> uploadedEmojis = ConcurrentHashMap.newKeySet();

    private float viewU = 0.0F / 0.0F;

    private long lastFrame;

    private long lastEmojiAt;

    private boolean emojiRight = true;

    private float emojiScale = 1.0F;

    private float glowAlpha = 0.8F;

    private int serial;

    private final PeepSky peep = new PeepSky();

    private float peepLevel;

    private float peepIdle;

    private double peepClock;

    private String peepTitle;

    private PeepSong peepSong;

    private SkyLyrics() {}

    public static void render() {
        INSTANCE.draw();
    }

    private void draw() {
        if (!this.failed) {
            long l = System.nanoTime();
            float f =
                    this.lastFrame == 0L
                            ? 0.0F
                            : Math.min((float) (l - this.lastFrame) / 1.0E9F, 0.1F);
            this.lastFrame = l;
            SpotifyHudModule module = SpotifyHudModule.active();
            boolean bl = module != null && module.peepPlaying();
            this.peepLevel =
                    bl
                            ? Math.min(1.0F, this.peepLevel + f / 5.0F)
                            : Math.max(0.0F, this.peepLevel - f / 1.8F);
            boolean bl2 = bl && module.showLyrics();
            boolean bl3 = module != null && (module.skyLyrics() || bl2);
            float g = this.peepIdle = !bl && !(this.peepLevel > 0.0F) ? this.peepIdle + f : 0.0F;
            if (this.peepLevel <= 0.0F && !bl) {
                this.peep.restart();
                this.peepClock = 0.0;
                if (this.peepIdle > 30.0F && this.peep.holding()) {
                    this.peep.close();
                }
            }
            if (bl3 || !this.line.idle() || !this.title.idle() || !(this.peepLevel <= 0.0F)) {
                try {
                    if (this.canvas == null) {
                        this.canvas = new SkyCanvas();
                    }
                    Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
                    float h = MathHelper.floorMod(camera.getYaw() / 360.0F + 0.25F, 1.0F);
                    float f2 =
                            this.viewU =
                                    Float.isNaN(this.viewU)
                                            ? h
                                            : wrap(
                                                    this.viewU
                                                            + wrapDelta(h - this.viewU)
                                                                    * Math.min(1.0F, f * 2.2F));
                    if (this.peepLevel > 0.0F) {
                        NowPlaying nowPlaying = module != null ? module.nowPlaying() : null;
                        if (bl && !nowPlaying.title().equals(this.peepTitle)) {
                            this.peepTitle = nowPlaying.title();
                            this.peepSong = PeepSong.of(this.peepTitle);
                        }
                        this.peep.ready(this.canvas, this.raster);
                        this.peepLyrics(bl3 ? module : null);
                        this.peep.update(
                                f,
                                this.peepSong,
                                this.peepLevel,
                                this.viewU,
                                h,
                                -camera.getPitch(),
                                bl2 ? linePulse(module, nowPlaying) : 0.0F);
                        this.peepClock = (this.peepClock + f) % 21600.0;
                        this.peep.draw(this.canvas, this.peepLevel, (float) this.peepClock);
                        this.canvas.flush(1.0F);
                    }
                    NowPlaying nowPlaying2 = bl3 ? module.nowPlaying() : null;
                    boolean bl4 = nowPlaying2 != null && nowPlaying2.playing();
                    this.line.want(
                            bl3 ? currentLine(module, nowPlaying2) : "",
                            bl3 && module.skyEmojis() ? module.skyEmojiStyle() : null,
                            bl3 ? module.skyEmojiSize() : 1.0F);
                    this.title.want(
                            bl3 && bl4 && module.skyTitle() ? nowPlaying2.title().strip() : "",
                            null,
                            1.0F);
                    this.line.step(f, this.viewU);
                    this.title.step(f, this.viewU);
                    if (this.line.idle() && this.title.idle()) {
                        return;
                    }
                    boolean bl5 = !bl3 || module.skyFollow();
                    float g2 = bl3 ? module.skySize() : 1.0F;
                    float h2 = bl3 ? module.skyTitleSize() : 1.0F;
                    this.emojiScale = bl3 ? module.skyEmojiSize() : 1.0F;
                    this.glowAlpha = bl3 ? module.skyGlow() : 0.8F;
                    float f3 = bl3 ? module.skyHeight() : 28.0F;
                    float g3 = bl3 ? module.skyOpacity() : 0.9F;
                    int i = bl2 ? -24876 : (bl3 ? module.skyTextColor() : -1);
                    int j = bl2 ? -34104 : (bl3 ? module.skyTitleColor() : -1);
                    AtmosphereModule module2 =
                            bl3 && !bl2 && module.skyGlowAtmosphere()
                                    ? AtmosphereModule.get()
                                    : null;
                    int k =
                            bl2
                                    ? 16723349
                                    : (module2 != null
                                            ? module2.lyricGlow()
                                            : (bl3 ? module.skyGlowColor() : 9063935));
                    if ((k & 16777215) == 0 && module2 != null) {
                        k = 9063935;
                    }
                    this.drawSlot(this.line, bl5, f3, g2, i, k);
                    this.drawSlot(this.title, bl5, titleElevation(f3, g2, h2), h2, j, k);
                    this.canvas.flush(g3);
                } catch (RuntimeException exception) {
                    this.failed = true;
                    if (this.canvas != null) {
                        this.canvas.clear();
                    }
                    this.line.close();
                    this.title.close();
                    this.peep.close();
                    LOGGER.error("[EFN] Sky lyrics stopped drawing", (Throwable) exception);
                }
            }
        }
    }

    private static float titleElevation(float f, float g, float h) {
        return f + (7.0F * g + 4.0F * h) * 0.5F + 0.6F * Math.max(g, h);
    }

    private void peepLyrics(SpotifyHudModule module) {
        if (module == null) {
            this.peep.lyrics(
                    this.viewU, 0.0F / 0.0F, 0.0F, 0.0F, this.viewU, 0.0F / 0.0F, 0.0F, 0.0F);
        } else {
            float skySize = module.skySize();
            float skyTitleSize = module.skyTitleSize();
            float skyHeight = module.skyHeight();
            SkyLyrics.Shown shown =
                    this.line.current != null ? this.line.current : this.line.leaving;
            SkyLyrics.Shown shown2 =
                    this.title.current != null ? this.title.current : this.title.leaving;
            boolean skyFollow = module.skyFollow();
            float f = !skyFollow && shown != null ? shown.u : this.viewU;
            float g = !skyFollow && shown2 != null ? shown2.u : f;
            this.peep.lyrics(
                    f,
                    skyHeight,
                    halfWidth(shown, 7.0F * skySize),
                    7.0F * skySize * 0.5F,
                    g,
                    titleElevation(skyHeight, skySize, skyTitleSize),
                    halfWidth(shown2, 4.0F * skyTitleSize),
                    4.0F * skyTitleSize * 0.5F);
        }
    }

    private static float halfWidth(SkyLyrics.Shown shown, float f) {
        return shown == null ? 0.0F : Math.min(f * shown.aspect, 140.0F) * 0.5F;
    }

    private void drawSlot(SkyLyrics.Slot slot, boolean bl, float f, float g, int i, int j) {
        SkyLyrics.Shown leaving2 = slot.leaving;
        SkyLyrics.Shown current2 = slot.current;
        if (leaving2 != null) {
            this.drawShown(
                    leaving2, bl, f + (1.0F - leaving2.alpha) * 1.6F * g, slot.degrees * g, i, j);
        }
        if (current2 != null) {
            float h = 1.0F - current2.alpha;
            this.drawShown(current2, bl, f - h * h * 1.6F * g, slot.degrees * g, i, j);
        }
    }

    private void drawShown(SkyLyrics.Shown shown, boolean bl, float f, float g, int i, int j) {
        float h = MathHelper.clamp(shown.alpha, 0.0F, 1.0F);
        if (!(h <= 0.003F)) {
            float f2 = g;
            float g2 = g * shown.aspect;
            if (g2 > 140.0F) {
                f2 = g * (140.0F / g2);
                g2 = 140.0F;
            }
            float h2 = f * 0.017453292F;
            float f3 = 0.72F - h2 / 1.0471976F;
            float g3 = f2 * 0.017453292F / 1.0471976F * 0.5F;
            float h3 = g2 / (360.0F * Math.max(0.2F, MathHelper.cos(h2))) * 0.5F;
            float f4 = bl ? this.viewU : shown.u;
            float g4 = f3 - g3;
            float h4 = f3 + g3;
            float f5 = !(shown.alpha >= 1.0F) && shown == this.line.current ? backOut(h) : h;
            float g5 = (float) (System.nanoTime() % 3600000000000L) / 1.0E9F;
            if (shown.emoji != null && shown.style == SpotifyHudModule.SkyEmojiStyle.BEHIND) {
                float h5 = f2 * 2.8F * this.emojiScale * f5 * 0.017453292F / 1.0471976F;
                this.canvas.sprite(
                        shown.emoji,
                        f4,
                        f3,
                        h5,
                        h5,
                        MathHelper.sin(g5 * 0.9F + shown.phase) * 0.05F,
                        withAlpha(16777215, h * 0.32F),
                        false);
            }
            if (this.glowAlpha > 0.0F) {
                this.canvas.band(
                        shown.glow,
                        f4 - h3,
                        f4 + h3,
                        g4,
                        h4,
                        withAlpha(j, h * this.glowAlpha),
                        true,
                        32);
            }
            this.canvas.band(
                    shown.fill,
                    f4 - h3,
                    f4 + h3,
                    g4,
                    h4,
                    withAlpha(i, h * (i >>> 24) / 255.0F),
                    false,
                    32);
            if (shown.emoji != null && shown.style != SpotifyHudModule.SkyEmojiStyle.BEHIND) {
                if (shown.style == SpotifyHudModule.SkyEmojiStyle.REPLACE_WORD) {
                    float f6 =
                            MathHelper.sin(g5 * 2.2F + shown.phase)
                                    * f2
                                    * 0.03F
                                    * 0.017453292F
                                    / 1.0471976F;
                    float g6 = shown.emojiSize * (h4 - g4) * f5;
                    this.canvas.sprite(
                            shown.emoji,
                            f4 - h3 + shown.emojiX * h3 * 2.0F,
                            g4 + shown.emojiY * (h4 - g4) - f6,
                            g6,
                            g6,
                            MathHelper.sin(g5 * 1.7F + shown.phase) * 0.06F,
                            withAlpha(16777215, h),
                            false);
                } else {
                    float h6 = f2 * 1.0F * this.emojiScale;
                    float f7 = h6 * f5;
                    float g7 = (h6 * 0.5F + 0.8F) / (360.0F * Math.max(0.2F, MathHelper.cos(h2)));
                    float h7 = MathHelper.sin(g5 * 2.2F + shown.phase) * f2 * 0.06F;
                    float f8 = 0.72F - (f + h7) * 0.017453292F / 1.0471976F;
                    float g8 = f7 * 0.017453292F / 1.0471976F;
                    float h8 = MathHelper.sin(g5 * 1.7F + shown.phase) * 0.12F;
                    this.canvas.sprite(
                            shown.emoji,
                            f4 + (shown.right ? 1.0F : -1.0F) * (h3 + g7),
                            f8,
                            g8,
                            g8,
                            h8,
                            withAlpha(16777215, h),
                            false);
                }
            }
        }
    }

    private SkyLyrics.Shown show(SkyLyrics.Raster raster2, float f) {
        int i = this.serial++;
        SkyTexture skyTexture =
                SkyTexture.upload(
                        "sky_lyrics_" + i, raster2.width(), raster2.height(), raster2.fill());
        SkyTexture skyTexture2 =
                SkyTexture.upload(
                        "sky_lyrics_glow_" + i, raster2.width(), raster2.height(), raster2.glow());
        SkyLyrics.Shown shown =
                new SkyLyrics.Shown(
                        skyTexture, skyTexture2, (float) raster2.width() / raster2.height(), f);
        SkyEmoji skyEmoji = raster2.emoji();
        if (skyEmoji == null) {
            return shown;
        } else {
            SkyTexture emojiTexture = this.emojiTextures.get(skyEmoji);
            if (emojiTexture == null && raster2.emojiPixels() != null) {
                emojiTexture =
                        SkyTexture.upload(
                                "sky_emoji_" + skyEmoji.name().toLowerCase(Locale.ROOT),
                                256,
                                256,
                                raster2.emojiPixels());
                this.emojiTextures.put(skyEmoji, emojiTexture);
                this.uploadedEmojis.add(skyEmoji);
            }
            if (emojiTexture == null) {
                return shown;
            } else {
                shown.emoji = emojiTexture;
                shown.style = raster2.style();
                shown.emojiX = raster2.emojiX();
                shown.emojiY = raster2.emojiY();
                shown.emojiSize = raster2.emojiSize();
                if (raster2.style() == SpotifyHudModule.SkyEmojiStyle.BESIDE) {
                    shown.right = this.emojiRight;
                    this.emojiRight = !this.emojiRight;
                }
                this.lastEmojiAt = System.nanoTime();
                return shown;
            }
        }
    }

    private static float backOut(float f) {
        float g = 1.70158F;
        float h = f - 1.0F;
        return 1.0F + (g + 1.0F) * h * h * h + g * h * h;
    }

    private static String currentLine(SpotifyHudModule module, NowPlaying nowPlaying) {
        if (nowPlaying != null && nowPlaying.playing()) {
            Lyrics lyrics = module.lyricsFor(nowPlaying);
            if (lyrics != null && lyrics.status() == Lyrics.Status.SYNCED) {
                SyncedLyrics syncedLyrics = lyrics.synced();
                int index =
                        syncedLyrics.indexAt(
                                nowPlaying.positionAt(System.currentTimeMillis()) + 200L);
                if (index < 0) {
                    return "";
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int j = syncedLyrics.firstWithTimeOf(index);
                            j <= syncedLyrics.lastWithTimeOf(index);
                            j++) {
                        String string = syncedLyrics.line(j).text().strip();
                        if (!string.isEmpty()) {
                            if (!sb.isEmpty()) {
                                sb.append("  \u00b7  ");
                            }
                            sb.append(string);
                        }
                    }
                    return sb.toString();
                }
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    private static float linePulse(SpotifyHudModule module, NowPlaying nowPlaying) {
        if (nowPlaying != null && nowPlaying.playing()) {
            Lyrics lyrics = module.lyricsFor(nowPlaying);
            if (lyrics != null && lyrics.status() == Lyrics.Status.SYNCED) {
                SyncedLyrics syncedLyrics = lyrics.synced();
                long l = nowPlaying.positionAt(System.currentTimeMillis()) + 200L;
                int index = syncedLyrics.indexAt(l);
                float f = 0.0F;
                for (int j = 0; j < 2 && index >= 0; j++) {
                    int firstWithTime = syncedLyrics.firstWithTimeOf(index);
                    if (hasWords(syncedLyrics, firstWithTime, syncedLyrics.lastWithTimeOf(index))) {
                        f =
                                Math.max(
                                        f,
                                        PeepSky.heartbeat(
                                                (float)
                                                                (l
                                                                        - syncedLyrics
                                                                                .line(firstWithTime)
                                                                                .timeMs())
                                                        / 1000.0F));
                    }
                    index = firstWithTime - 1;
                }
                return f;
            } else {
                return 0.0F;
            }
        } else {
            return 0.0F;
        }
    }

    private static boolean hasWords(SyncedLyrics syncedLyrics, int i, int j) {
        for (int k = i; k <= j; k++) {
            if (!syncedLyrics.line(k).text().isBlank()) {
                return true;
            }
        }
        return false;
    }

    private SkyLyrics.Raster rasterize(
            int i, String string, float f, SpotifyHudModule.SkyEmojiStyle skyEmojiStyle, float g) {
        SkyEmoji.Match match = skyEmojiStyle != null ? SkyEmoji.match(string) : null;
        SkyEmoji skyEmoji = match != null ? match.emoji() : null;
        int[] ints =
                skyEmoji != null && !this.uploadedEmojis.contains(skyEmoji)
                        ? skyEmoji.draw()
                        : null;
        if (ints == null && skyEmoji != null && !this.uploadedEmojis.contains(skyEmoji)) {
            skyEmoji = null;
        }
        boolean bl =
                skyEmoji != null && skyEmojiStyle == SpotifyHudModule.SkyEmojiStyle.REPLACE_WORD;
        String string2 = bl ? string.substring(0, match.start()) : string;
        String string3 = bl ? string.substring(match.end()) : "";
        Font font2 = this.fontFor(string);
        BufferedImage bufferedImage = new BufferedImage(1, 1, 2);
        Graphics2D graphics = bufferedImage.createGraphics();
        hints(graphics);
        Font font3 = font2.deriveFont(f);
        FontMetrics fontMetrics = graphics.getFontMetrics(font3);
        float h = bl ? f * 1.6F * g : 0.0F;
        int j =
                Math.max(
                        1,
                        fontMetrics.stringWidth(string2)
                                + Math.round(h)
                                + fontMetrics.stringWidth(string3));
        if (j + 56 > 4096) {
            float f2 = 4040.0F / j;
            font3 = font2.deriveFont(f * f2);
            fontMetrics = graphics.getFontMetrics(font3);
            j =
                    Math.max(
                            1,
                            Math.min(
                                    4040,
                                    fontMetrics.stringWidth(string2)
                                            + Math.round(h *= f2)
                                            + fontMetrics.stringWidth(string3)));
        }
        graphics.dispose();
        int k = j + 56;
        int m = fontMetrics.getAscent() + fontMetrics.getDescent() + 56;
        int n = 28 + fontMetrics.getAscent();
        int stringWidth = fontMetrics.stringWidth(string2);
        BufferedImage bufferedImage2 = new BufferedImage(k, m, 2);
        Graphics2D graphics2 = bufferedImage2.createGraphics();
        hints(graphics2);
        graphics2.setFont(font3);
        graphics2.setColor(Color.WHITE);
        graphics2.drawString(string2, 28, n);
        if (!string3.isEmpty()) {
            graphics2.drawString(string3, 28 + stringWidth + Math.round(h), n);
        }
        float height2 =
                (float)
                        font3.createGlyphVector(graphics2.getFontRenderContext(), "H")
                                .getVisualBounds()
                                .getHeight();
        graphics2.dispose();
        int[] rGB = bufferedImage2.getRGB(0, 0, k, m, null, 0, k);
        int[] ints2 = glow(rGB, k, m);
        float g2 = bl ? (28 + stringWidth + h * 0.5F) / k : 0.0F;
        float h2 = bl ? (n - height2 * 0.5F) / m : 0.0F;
        return new SkyLyrics.Raster(
                i,
                k,
                m,
                rGB,
                ints2,
                skyEmoji,
                ints,
                skyEmoji != null ? skyEmojiStyle : null,
                g2,
                h2,
                bl ? h / m : 0.0F);
    }

    private Font fontFor(String string) {
        if (this.font == null && !this.fontMissing) {
            try (InputStream resourceAsStream =
                    SkyLyrics.class.getResourceAsStream("/assets/efn/fonts/manrope-bold.ttf")) {
                if (resourceAsStream != null) {
                    this.font = Font.createFont(0, resourceAsStream);
                } else {
                    this.fontMissing = true;
                }
            } catch (Exception exception) {
                this.fontMissing = true;
                LOGGER.warn(
                        "[EFN] Sky lyrics font couldn't load; using the system font",
                        (Throwable) exception);
            }
        }
        return this.font != null && this.font.canDisplayUpTo(string) == -1
                ? this.font
                : new Font("SansSerif", 1, 1);
    }

    private static void hints(Graphics2D graphics2D) {
        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics2D.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics2D.setRenderingHint(
                RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static int[] glow(int[] ints, int i, int j) {
        float[] floats = new float[i * j];
        for (int k = 0; k < floats.length; k++) {
            floats[k] = (ints[k] >>> 24) / 255.0F;
        }
        float[] floats2 = new float[floats.length];
        for (int m = 0; m < 2; m++) {
            blurRows(floats, floats2, i, j);
            blurColumns(floats2, floats, i, j);
        }
        int[] ints2 = new int[floats.length];
        for (int n = 0; n < ints2.length; n++) {
            ints2[n] = Math.min(255, Math.round(floats[n] * 400.0F)) << 24 | 16777215;
        }
        return ints2;
    }

    private static void blurRows(float[] floats, float[] floats2, int i, int j) {
        float f = 0.04761905F;
        for (int k = 0; k < j; k++) {
            int m = k * i;
            float g = 0.0F;
            for (int n = 0; n <= Math.min(10, i - 1); n++) {
                g += floats[m + n];
            }
            for (int o = 0; o < i; o++) {
                floats2[m + o] = g * f;
                int p = o + 10 + 1;
                int q = o - 10;
                if (p < i) {
                    g += floats[m + p];
                }
                if (q >= 0) {
                    g -= floats[m + q];
                }
            }
        }
    }

    private static void blurColumns(float[] floats, float[] floats2, int i, int j) {
        float f = 0.04761905F;
        for (int k = 0; k < i; k++) {
            float g = 0.0F;
            for (int m = 0; m <= Math.min(10, j - 1); m++) {
                g += floats[m * i + k];
            }
            for (int n = 0; n < j; n++) {
                floats2[n * i + k] = g * f;
                int o = n + 10 + 1;
                int p = n - 10;
                if (o < j) {
                    g += floats[o * i + k];
                }
                if (p >= 0) {
                    g -= floats[p * i + k];
                }
            }
        }
    }

    private static int withAlpha(int i, float f) {
        return Math.round(MathHelper.clamp(f, 0.0F, 1.0F) * 255.0F) << 24 | i & 16777215;
    }

    private static float wrap(float f) {
        return f - (float) Math.floor(f);
    }

    private static float wrapDelta(float f) {
        return f - Math.round(f);
    }

    @Environment(EnvType.CLIENT)
    private record Raster(
            int id,
            int width,
            int height,
            int[] fill,
            int[] glow,
            SkyEmoji emoji,
            int[] emojiPixels,
            SpotifyHudModule.SkyEmojiStyle style,
            float emojiX,
            float emojiY,
            float emojiSize) {}

    @Environment(EnvType.CLIENT)
    private static final class Shown {

        final SkyTexture fill;

        final SkyTexture glow;

        final float aspect;

        final float u;

        final float phase = (float) (Math.random() * 3.141592653589793 * 2.0);

        float alpha;

        SkyTexture emoji;

        SpotifyHudModule.SkyEmojiStyle style;

        boolean right;

        float emojiX;

        float emojiY;

        float emojiSize;

        Shown(SkyTexture fill2, SkyTexture glow2, float aspect2, float f) {
            this.fill = fill2;
            this.glow = glow2;
            this.aspect = aspect2;
            this.u = f;
        }

        void close() {
            this.fill.close();
            this.glow.close();
        }
    }

    @Environment(EnvType.CLIENT)
    private final class Slot {

        private final float degrees;

        private final float pixels;

        private String wanted = "";

        private SpotifyHudModule.SkyEmojiStyle wantedStyle;

        private volatile int request;

        private volatile SkyLyrics.Raster ready;

        private SkyLyrics.Shown current;

        private SkyLyrics.Shown leaving;

        Slot(float degrees2, float pixels2) {
            this.degrees = degrees2;
            this.pixels = pixels2;
        }

        void want(String wanted2, SpotifyHudModule.SkyEmojiStyle wantedStyle2, float f) {
            if (!wanted2.equals(this.wanted) || wantedStyle2 != this.wantedStyle) {
                this.wanted = wanted2;
                this.wantedStyle = wantedStyle2;
                int i = ++this.request;
                this.ready = null;
                if (!wanted2.isEmpty()) {
                    SpotifyHudModule.SkyEmojiStyle skyEmojiStyle =
                            System.nanoTime() - SkyLyrics.this.lastEmojiAt >= 5000000000L
                                    ? wantedStyle2
                                    : null;
                    SkyLyrics.this.raster.submit(
                            () -> {
                                try {
                                    SkyLyrics.Raster raster2 =
                                            SkyLyrics.this.rasterize(
                                                    i, wanted2, this.pixels, skyEmojiStyle, f);
                                    if (i == this.request) {
                                        this.ready = raster2;
                                    }
                                } catch (RuntimeException | Error exception) {
                                    SkyLyrics.LOGGER.warn(
                                            "[EFN] Sky lyrics couldn't draw a line",
                                            (Throwable) exception);
                                }
                            });
                }
            }
        }

        void step(float f, float g) {
            SkyLyrics.Raster ready2 = this.ready;
            if (ready2 != null && ready2.id() == this.request) {
                this.ready = null;
                this.retire();
                this.current = SkyLyrics.this.show(ready2, g);
            } else if (this.wanted.isEmpty()) {
                this.retire();
            }
            if (this.current != null) {
                this.current.alpha = Math.min(1.0F, this.current.alpha + f * 2.6F);
            }
            if (this.leaving != null) {
                this.leaving.alpha -= f * 2.6F;
                if (this.leaving.alpha <= 0.0F) {
                    this.leaving.close();
                    this.leaving = null;
                }
            }
        }

        boolean idle() {
            return this.current == null && this.leaving == null && this.wanted.isEmpty();
        }

        private void retire() {
            if (this.current != null) {
                if (this.leaving != null) {
                    this.leaving.close();
                }
                this.leaving = this.current;
                this.current = null;
            }
        }

        void close() {
            this.wanted = "";
            this.request++;
            this.ready = null;
            if (this.current != null) {
                this.current.close();
            }
            if (this.leaving != null) {
                this.leaving.close();
            }
            this.current = null;
            this.leaving = null;
        }
    }
}
