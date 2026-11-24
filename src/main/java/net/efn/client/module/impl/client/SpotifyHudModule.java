package net.efn.client.module.impl.client;

import com.sun.jna.Platform;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ThemeTint;
import net.efn.client.input.EfnKeyPolling;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.render.PinkWater;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.spotify.AlbumArt;
import net.efn.client.util.spotify.Lyrics;
import net.efn.client.util.spotify.LyricsQuery;
import net.efn.client.util.spotify.LyricsService;
import net.efn.client.util.spotify.MediaSession;
import net.efn.client.util.spotify.NowPlaying;
import net.efn.client.util.spotify.PlayerSource;
import net.efn.client.util.spotify.SpotifyWindow;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Environment(EnvType.CLIENT)
public final class SpotifyHudModule extends ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final long POLL_INTERVAL_MS = 500L;

    private static final float TINT_STEP_IN = 0.016666668F;

    private static final float TINT_STEP_OUT = 0.033333335F;

    private static final long WINDOW_POLL_MS = 1000L;

    private static final long REPOLL_AFTER_KEY_MS = 250L;

    private static final long KEY_COOLDOWN_MS = 200L;

    private static final int ART_MAX_SIDE = 256;

    private static final int STABLE_POLLS = 2;

    private static final int SESSION_HICCUPS = 2;

    private static final long ERROR_LOG_INTERVAL_MS = 60000L;

    private final BooleanSetting showArtist =
            this.addSetting("show-artist", new BooleanSetting("Show Artist", true));

    private final BooleanSetting showAlbumArt =
            this.addSetting("show-album-art", new BooleanSetting("Show Album Art", true));

    private final BooleanSetting showProgress =
            this.addSetting("show-progress", new BooleanSetting("Show Progress", true));

    private final BooleanSetting scrollTitles =
            this.addSetting("scroll-titles", new BooleanSetting("Scroll Long Titles", true));

    private final BooleanSetting showLyrics =
            this.addSetting("show-lyrics", new BooleanSetting("Show Lyrics", true));

    private final ModeSetting lyricLines =
            this.addSetting("lyric-lines", new ModeSetting("Lyric Lines", "2", "1", "2", "3"));

    private final ModeSetting lyricMotion =
            this.addSetting(
                    "lyric-motion",
                    new ModeSetting("Lyric Motion", "Wave", "Wave", "Glide", "Fade"));

    private final ModeSetting lyricHighlight =
            this.addSetting(
                    "lyric-highlight",
                    new ModeSetting("Lyric Highlight", "Karaoke", "Karaoke", "Line", "Off"));

    private final BooleanSetting lyricsOnline =
            this.addSetting("lyrics-online", new BooleanSetting("Lyrics Online", true));

    private final BooleanSetting skyLyrics =
            this.addSetting("sky-lyrics", new BooleanSetting("Sky Lyrics", true));

    private final BooleanSetting skyTitle =
            this.addSetting("sky-title", new BooleanSetting("Sky Song Title", false));

    private final BooleanSetting skyEmojis =
            this.addSetting("sky-emojis", new BooleanSetting("Sky Emojis", true));

    private final ModeSetting skyEmojiStyle =
            this.addSetting(
                    "sky-emoji-style",
                    new ModeSetting(
                            "Sky Emoji Style", "Behind", "Beside", "Behind", "Replace Word"));

    private final BooleanSetting skyFollow =
            this.addSetting("sky-follow", new BooleanSetting("Sky Follows View", true));

    private final NumberSetting skyHeight =
            this.addSetting(
                    "sky-height", new NumberSetting("Sky Height", 10.0F, 10.0F, 70.0F).step(1.0F));

    private final NumberSetting skySize =
            this.addSetting(
                    "sky-size", new NumberSetting("Sky Text Size", 1.25F, 0.3F, 3.0F).step(0.05F));

    private final NumberSetting skyTitleSize =
            this.addSetting(
                    "sky-title-size",
                    new NumberSetting("Sky Title Size", 1.0F, 0.3F, 3.0F).step(0.05F));

    private final NumberSetting skyEmojiSize =
            this.addSetting(
                    "sky-emoji-size",
                    new NumberSetting("Sky Emoji Size", 1.35F, 0.3F, 3.0F).step(0.05F));

    private final NumberSetting skyOpacity =
            this.addSetting(
                    "sky-opacity", new NumberSetting("Sky Opacity", 0.9F, 0.1F, 1.0F).step(0.05F));

    private final ColorSetting skyTextColor =
            this.addSetting("sky-text-color", new ColorSetting("Sky Text Color", -1));

    private final ColorSetting skyTitleColor =
            this.addSetting("sky-title-color", new ColorSetting("Sky Title Color", -1447442));

    private final BooleanSetting skyGlowAtmosphere =
            this.addSetting(
                    "sky-glow-atmosphere", new BooleanSetting("Sky Glow From Atmosphere", true));

    private final ColorSetting skyGlowColor =
            this.addSetting("sky-glow-color", new ColorSetting("Sky Glow Color", -7713281));

    private final NumberSetting skyGlow =
            this.addSetting(
                    "sky-glow", new NumberSetting("Sky Glow", 0.8F, 0.0F, 1.0F).step(0.05F));

    private final BooleanSetting easterEggs =
            this.addSetting("easter-eggs", new BooleanSetting("Easter Eggs", true));

    private final ModeSetting pinkWater =
            this.addSetting(
                    "pink-water",
                    new ModeSetting(
                            "Pink Water", PinkWater.Design.DEFAULT.label(), PinkWater.modes()));

    private final NumberSetting cardWidth =
            this.addSetting(
                    "card-width",
                    new NumberSetting("Card Width", 320.0F, 200.0F, 560.0F).step(1.0F));

    private final ColorSetting titleColor =
            this.addSetting("title-color", new ColorSetting("Title Color", -1447442));

    private final ColorSetting artistColor =
            this.addSetting("artist-color", new ColorSetting("Artist Color", -6052949));

    private final ColorSetting lyricsColor =
            this.addSetting("lyrics-color", new ColorSetting("Lyrics Color", -657929));

    private final ColorSetting upcomingLyricsColor =
            this.addSetting(
                    "upcoming-lyrics-color", new ColorSetting("Upcoming Lyrics Color", -6052949));

    private final ColorSetting highlightColor =
            this.addSetting("highlight-color", new ColorSetting("Highlight Color", -14756000));

    private final BooleanSetting showWhenIdle =
            this.addSetting("show-when-idle", new BooleanSetting("Show When Idle", false));

    private final BooleanSetting mediaKeys =
            this.addSetting("media-keys", new BooleanSetting("Media Keys", false));

    private final KeySetting playPauseKey =
            this.addSetting("play-pause-key", new KeySetting("Play / Pause", -1));

    private final KeySetting nextKey =
            this.addSetting("next-key", new KeySetting("Next Track", -1));

    private final KeySetting previousKey =
            this.addSetting("previous-key", new KeySetting("Previous Track", -1));

    private volatile NowPlaying nowPlaying = NowPlaying.WAITING;

    private volatile SpotifyHudModule.TrackLyrics lyrics;

    private volatile SpotifyHudModule.Poller poller;

    private final Object publishLock = new Object();

    private final KeySetting[] keys =
            new KeySetting[] {this.playPauseKey, this.nextKey, this.previousKey};

    private final SpotifyWindow.MediaKey[] actions =
            new SpotifyWindow.MediaKey[] {
                SpotifyWindow.MediaKey.PLAY_PAUSE,
                SpotifyWindow.MediaKey.NEXT,
                SpotifyWindow.MediaKey.PREVIOUS
            };

    private final boolean[] wasDown = new boolean[this.keys.length];

    private long lastKeyAt;

    public SpotifyHudModule() {
        super(
                "spotify-hud",
                "Spotify HUD",
                "Shows what Spotify is playing, with synced lyrics and optional media keys.",
                ModuleCategory.CLIENT);
        this.usesNotifications();
        this.artistColor.visibleWhen(this.showArtist::get);
        this.lyricsColor.visibleWhen(
                () -> this.showLyrics.get() && !this.lyricHighlight.is("Line"));
        this.upcomingLyricsColor.visibleWhen(this.showLyrics::get);
        this.highlightColor.visibleWhen(
                () -> this.showLyrics.get() && !this.lyricHighlight.is("Off"));
        this.lyricLines.visibleWhen(this.showLyrics::get);
        this.lyricMotion.visibleWhen(this.showLyrics::get);
        this.lyricHighlight.visibleWhen(this.showLyrics::get);
        this.lyricsOnline.visibleWhen(this.showLyrics::get);
        this.skyLyrics.visibleWhen(this.showLyrics::get);
        this.skyTitle.visibleWhen(this::skyLyrics);
        this.skyEmojis.visibleWhen(this::skyLyrics);
        this.skyFollow.visibleWhen(this::skyLyrics);
        this.skyHeight.visibleWhen(this::skyLyrics);
        this.skySize.visibleWhen(this::skyLyrics);
        this.skyTitleSize.visibleWhen(() -> this.skyLyrics() && this.skyTitle.get());
        this.skyEmojiStyle.visibleWhen(() -> this.skyLyrics() && this.skyEmojis.get());
        this.skyEmojiSize.visibleWhen(() -> this.skyLyrics() && this.skyEmojis.get());
        this.skyOpacity.visibleWhen(this::skyLyrics);
        this.skyTextColor.visibleWhen(this::skyLyrics);
        this.skyTitleColor.visibleWhen(() -> this.skyLyrics() && this.skyTitle.get());
        this.skyGlowAtmosphere.visibleWhen(this::skyLyrics);
        this.skyGlowColor.visibleWhen(this::skyLyrics);
        this.skyGlow.visibleWhen(this::skyLyrics);
        this.lyricsOnline.onChange(
                () -> {
                    SpotifyHudModule.Poller poller2 = this.poller;
                    if (poller2 != null) {
                        poller2.lyricsService.setOnline(this.lyricsOnline.get());
                    }
                });
        this.pinkWater.visibleWhen(this.easterEggs::get);
        this.playPauseKey.visibleWhen(this.mediaKeys::get);
        this.nextKey.visibleWhen(this.mediaKeys::get);
        this.previousKey.visibleWhen(this.mediaKeys::get);
    }

    @Override
    protected void onEnable() {
        Arrays.fill(this.wasDown, true);
        if (!PlayerSource.supported()) {
            Object publishLock2 = this.publishLock;
            synchronized (publishLock2) {
                this.nowPlaying = NowPlaying.UNSUPPORTED;
                this.lyrics = null;
            }
            ModuleChat.send(
                    this,
                    ModuleChat.body()
                            .danger("Not supported")
                            .text(", only the Spotify app on Windows, macOS and Linux works"));
        } else {
            SpotifyHudModule.Poller poller2 = new SpotifyHudModule.Poller();
            Object publishLock3 = this.publishLock;
            synchronized (publishLock3) {
                this.poller = poller2;
                this.nowPlaying = NowPlaying.WAITING;
                this.lyrics = null;
            }
            poller2.start();
            this.on(TickEvent.TYPE, Stage.POST, this::onTick);
        }
    }

    @Override
    protected void onDisable() {
        ThemeTint.pink(0.0F);
        PinkWater.reset(MinecraftClient.getInstance());
        Object publishLock2 = this.publishLock;
        SpotifyHudModule.Poller poller2;
        synchronized (publishLock2) {
            poller2 = this.poller;
            this.poller = null;
            this.nowPlaying =
                    PlayerSource.supported() ? NowPlaying.WAITING : NowPlaying.UNSUPPORTED;
            this.lyrics = null;
        }
        if (poller2 != null) {
            poller2.stop();
        }
    }

    private void publish(
            SpotifyHudModule.Poller poller2,
            NowPlaying nowPlaying2,
            SpotifyHudModule.TrackLyrics lyrics2) {
        Object publishLock2 = this.publishLock;
        synchronized (publishLock2) {
            if (poller2 == this.poller) {
                this.nowPlaying = nowPlaying2;
                this.lyrics = lyrics2;
            }
        }
    }

    public static SpotifyHudModule get() {
        return ModuleRegistry.get(SpotifyHudModule.class);
    }

    public static SpotifyHudModule active() {
        SpotifyHudModule module = get();
        return module != null && module.isEnabled() ? module : null;
    }

    public static void hudButton(SpotifyWindow.MediaKey mediaKey) {
        SpotifyHudModule module = active();
        if (module != null) {
            module.press(mediaKey);
        }
    }

    public static boolean shouldShowHud() {
        SpotifyHudModule module = active();
        if (module == null) {
            return false;
        } else {
            NowPlaying nowPlaying2 = module.nowPlaying;
            return nowPlaying2.status() != NowPlaying.Status.UNSUPPORTED
                            && nowPlaying2.status() != NowPlaying.Status.WAITING
                    ? nowPlaying2.active() || module.showWhenIdle.get()
                    : false;
        }
    }

    public NowPlaying nowPlaying() {
        return this.nowPlaying;
    }

    public Lyrics lyricsFor(NowPlaying nowPlaying2) {
        SpotifyHudModule.TrackLyrics lyrics2 = this.lyrics;
        SpotifyHudModule.Poller poller2 = this.poller;
        if (lyrics2 != null && poller2 != null && this.wantsLyrics(nowPlaying2)) {
            return lyrics2.title().equals(nowPlaying2.title())
                            && lyrics2.artist().equals(nowPlaying2.artist())
                    ? poller2.lyricsService.lookup(lyrics2.query())
                    : null;
        } else {
            return null;
        }
    }

    public boolean wantsLyrics(NowPlaying nowPlaying2) {
        return this.showLyrics.get() && nowPlaying2 != null && nowPlaying2.lyricsEligible();
    }

    public boolean showArtist() {
        return this.showArtist.get();
    }

    public boolean showAlbumArt() {
        return this.showAlbumArt.get();
    }

    public boolean showProgress() {
        return this.showProgress.get();
    }

    public boolean scrollTitles() {
        return this.scrollTitles.get();
    }

    public boolean showLyrics() {
        return this.showLyrics.get();
    }

    public boolean skyLyrics() {
        return this.showLyrics.get() && this.skyLyrics.get();
    }

    public boolean skyTitle() {
        return this.skyTitle.get();
    }

    public boolean skyEmojis() {
        return this.skyEmojis.get();
    }

    public SpotifyHudModule.SkyEmojiStyle skyEmojiStyle() {
        String skyEmojiStyle2 = this.skyEmojiStyle.get();
        return switch (skyEmojiStyle2) {
            case "Behind" -> SpotifyHudModule.SkyEmojiStyle.BEHIND;
            case "Replace Word" -> SpotifyHudModule.SkyEmojiStyle.REPLACE_WORD;
            default -> SpotifyHudModule.SkyEmojiStyle.BESIDE;
        };
    }

    public boolean skyFollow() {
        return this.skyFollow.get();
    }

    public float skyHeight() {
        return this.skyHeight.get();
    }

    public float skySize() {
        return this.skySize.get();
    }

    public float skyTitleSize() {
        return this.skyTitleSize.get();
    }

    public float skyEmojiSize() {
        return this.skyEmojiSize.get();
    }

    public float skyOpacity() {
        return this.skyOpacity.get();
    }

    public int skyTextColor() {
        return this.skyTextColor.argb();
    }

    public int skyTitleColor() {
        return this.skyTitleColor.argb();
    }

    public boolean skyGlowAtmosphere() {
        return this.skyGlowAtmosphere.get();
    }

    public int skyGlowColor() {
        return this.skyGlowColor.argb();
    }

    public boolean peepPlaying() {
        NowPlaying nowPlaying2 = this.nowPlaying;
        return this.easterEggs.get()
                && nowPlaying2 != null
                && nowPlaying2.playing()
                && nowPlaying2.artist() != null
                && nowPlaying2
                        .artist()
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z]", "")
                        .contains("lilpeep");
    }

    public PinkWater.Design pinkWaterDesign() {
        return this.easterEggs.get() ? PinkWater.Design.byLabel(this.pinkWater.get()) : null;
    }

    public float skyGlow() {
        return this.skyGlow.get();
    }

    public int lyricLines() {
        try {
            return Math.max(1, Math.min(3, Integer.parseInt(this.lyricLines.get())));
        } catch (NumberFormatException exception) {
            return 2;
        }
    }

    public SpotifyHudModule.LyricMotion lyricMotion() {
        String lyricMotion2 = this.lyricMotion.get();
        return switch (lyricMotion2) {
            case "Glide" -> SpotifyHudModule.LyricMotion.GLIDE;
            case "Fade" -> SpotifyHudModule.LyricMotion.FADE;
            default -> SpotifyHudModule.LyricMotion.WAVE;
        };
    }

    public SpotifyHudModule.LyricHighlight lyricHighlight() {
        String lyricHighlight2 = this.lyricHighlight.get();
        return switch (lyricHighlight2) {
            case "Line" -> SpotifyHudModule.LyricHighlight.LINE;
            case "Off" -> SpotifyHudModule.LyricHighlight.OFF;
            default -> SpotifyHudModule.LyricHighlight.KARAOKE;
        };
    }

    public boolean lyricsOnline() {
        return this.lyricsOnline.get();
    }

    public float cardWidth() {
        return this.cardWidth.get();
    }

    public int titleColor() {
        return this.titleColor.argb();
    }

    public int artistColor() {
        return this.artistColor.argb();
    }

    public int lyricsColor() {
        return this.lyricsColor.argb();
    }

    public int upcomingLyricsColor() {
        return this.upcomingLyricsColor.argb();
    }

    public int highlightColor() {
        return this.highlightColor.argb();
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        float pink = ThemeTint.pink();
        boolean peepPlaying = this.peepPlaying();
        ThemeTint.pink(peepPlaying ? pink + 0.016666668F : pink - 0.033333335F);
        PinkWater.tick(client, peepPlaying, this.pinkWaterDesign());
        boolean bl =
                this.mediaKeys.get()
                        && client.currentScreen == null
                        && client.isWindowFocused()
                        && client.player != null;
        for (int i = 0; i < this.keys.length; i++) {
            boolean down = isDown(client, this.keys[i].value());
            if (down && !this.wasDown[i] && bl) {
                this.press(this.actions[i]);
            }
            this.wasDown[i] = down;
        }
    }

    private void press(SpotifyWindow.MediaKey mediaKey) {
        long l = System.currentTimeMillis();
        SpotifyHudModule.Poller poller2 = this.poller;
        if (poller2 != null && l - this.lastKeyAt >= 200L) {
            this.lastKeyAt = l;
            if (poller2.press(mediaKey)) {
                if (mediaKey == SpotifyWindow.MediaKey.PLAY_PAUSE) {
                    Object publishLock2 = this.publishLock;
                    synchronized (publishLock2) {
                        if (this.poller == poller2) {
                            this.nowPlaying = this.nowPlaying.toggled();
                        }
                    }
                }
            }
        }
    }

    private static boolean isDown(MinecraftClient client, int i) {
        if (!ModuleKeybinds.isBindable(i)) {
            return false;
        } else {
            return i >= 1000
                    ? GLFW.glfwGetMouseButton(client.getWindow().getHandle(), i - 1000) == 1
                    : EfnKeyPolling.isKeyDown(client.getWindow(), i);
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum LyricHighlight {
        KARAOKE,
        LINE,
        OFF;

        private LyricHighlight() {}
    }

    @Environment(EnvType.CLIENT)
    public static enum LyricMotion {
        WAVE,
        GLIDE,
        FADE;

        private LyricMotion() {}
    }

    @Environment(EnvType.CLIENT)
    private final class Poller {

        final ScheduledThreadPoolExecutor executor;

        final LyricsService lyricsService = new LyricsService();

        private PlayerSource media;

        private boolean mediaFailed;

        private SpotifyWindow window;

        private boolean windowFailed;

        private long nextWindowReadAt;

        private AlbumArt art;

        private int artVersion;

        private boolean artDecoded;

        private LyricsQuery lyricsCandidate;

        private int lyricsCandidatePolls;

        private LyricsQuery lyricsQuery;

        private SpotifyHudModule.TrackLyrics published;

        private int sessionFailures;

        private String lastError;

        private long lastErrorLoggedAt;

        Poller() {
            this.lyricsService.setOnline(SpotifyHudModule.this.lyricsOnline.get());
            this.executor =
                    new ScheduledThreadPoolExecutor(
                            1,
                            runnable -> {
                                Thread thread = new Thread(runnable, "EFN-Spotify");
                                thread.setDaemon(true);
                                return thread;
                            });
            this.executor.setRemoveOnCancelPolicy(true);
            this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        }

        void start() {
            this.executor.scheduleWithFixedDelay(this::poll, 0L, 500L, TimeUnit.MILLISECONDS);
        }

        void stop() {
            try {
                this.executor.execute(this::closeMedia);
            } catch (RejectedExecutionException exception) {
            }
            this.executor.shutdown();
            this.lyricsService.close();
        }

        private void closeMedia() {
            if (this.media != null) {
                this.media.close();
            }
        }

        boolean press(SpotifyWindow.MediaKey mediaKey) {
            try {
                this.executor.execute(() -> this.control(mediaKey));
                this.executor.schedule(this::poll, 250L, TimeUnit.MILLISECONDS);
                return true;
            } catch (RejectedExecutionException exception) {
                return false;
            }
        }

        private void poll() {
            if (SpotifyHudModule.this.poller == this) {
                try {
                    NowPlaying session = this.readSession();
                    if (session == null) {
                        if (this.sessionFailures > 0
                                && this.sessionFailures <= 2
                                && SpotifyHudModule.this.nowPlaying.source()
                                        == NowPlaying.Source.SESSION) {
                            return;
                        }
                        long l = System.currentTimeMillis();
                        if (SpotifyHudModule.this.nowPlaying.source() != NowPlaying.Source.SESSION
                                && l < this.nextWindowReadAt) {
                            return;
                        }
                        this.nextWindowReadAt = l + 1000L;
                        session = this.readWindow();
                    }
                    if (session != null) {
                        SpotifyHudModule.this.publish(this, session, this.lyricsFor(session));
                    }
                } catch (RuntimeException | LinkageError exception) {
                    SpotifyHudModule.LOGGER.debug(
                            "[EFN] Spotify HUD poll failed", (Throwable) exception);
                }
            }
        }

        private NowPlaying readSession() {
            if (this.mediaFailed) {
                return null;
            } else {
                if (this.media == null) {
                    try {
                        this.media = PlayerSource.spotify();
                    } catch (LinkageError exception) {
                        SpotifyHudModule.LOGGER.warn(
                                "[EFN] Spotify HUD: media session unavailable ({})",
                                exception.toString());
                        this.mediaFailed = true;
                        return null;
                    }
                }
                if (this.media.isUnsupported()) {
                    return null;
                } else {
                    MediaSession.Snapshot snapshot = this.media.poll();
                    if (snapshot != null) {
                        this.sessionFailures = 0;
                        return NowPlaying.fromSession(
                                snapshot, this.artFor(snapshot), SpotifyHudModule.this.nowPlaying);
                    } else {
                        String lastError2 = this.media.lastError();
                        this.logError(lastError2);
                        this.sessionFailures =
                                lastError2 != null && !this.media.isUnsupported()
                                        ? this.sessionFailures + 1
                                        : 0;
                        return null;
                    }
                }
            }
        }

        private AlbumArt artFor(MediaSession.Snapshot snapshot) {
            if (!SpotifyHudModule.this.showAlbumArt.get()) {
                this.art = null;
                this.artDecoded = false;
                return null;
            } else {
                if (!this.artDecoded || snapshot.thumbnailVersion() != this.artVersion) {
                    this.artDecoded = true;
                    this.artVersion = snapshot.thumbnailVersion();
                    this.art = null;
                    if (snapshot.hasThumbnail()) {
                        try {
                            this.art = AlbumArt.decode(snapshot.thumbnail(), 256);
                        } catch (RuntimeException | LinkageError exception) {
                            SpotifyHudModule.LOGGER.debug(
                                    "[EFN] Spotify HUD: album art could not be decoded",
                                    (Throwable) exception);
                        }
                    }
                }
                return this.art;
            }
        }

        private NowPlaying readWindow() {
            if (!Platform.isWindows()) {
                if (this.media != null && !this.mediaFailed && !this.media.isUnsupported()) {
                    return NowPlaying.NOT_RUNNING;
                } else {
                    this.executor.shutdown();
                    return NowPlaying.UNSUPPORTED;
                }
            } else {
                if (!this.windowFailed && this.window == null) {
                    try {
                        this.window = new SpotifyWindow();
                    } catch (LinkageError exception) {
                        this.windowFailed(exception);
                    }
                }
                if (this.window != null) {
                    try {
                        return NowPlaying.fromWindowTitle(
                                this.window.readTitle(), SpotifyHudModule.this.nowPlaying);
                    } catch (LinkageError exception2) {
                        this.windowFailed(exception2);
                    }
                }
                if (this.media != null && !this.mediaFailed && !this.media.isUnsupported()) {
                    return NowPlaying.NOT_RUNNING;
                } else {
                    this.executor.shutdown();
                    return NowPlaying.UNSUPPORTED;
                }
            }
        }

        private void windowFailed(LinkageError linkageError) {
            SpotifyHudModule.LOGGER.warn(
                    "[EFN] Spotify HUD: Win32 access unavailable ({})", linkageError.toString());
            this.window = null;
            this.windowFailed = true;
        }

        private SpotifyHudModule.TrackLyrics lyricsFor(NowPlaying nowPlaying2) {
            if (!SpotifyHudModule.this.wantsLyrics(nowPlaying2)) {
                this.lyricsCandidate = null;
                this.lyricsCandidatePolls = 0;
                this.lyricsQuery = null;
                this.published = null;
                return null;
            } else {
                LyricsQuery lyricsQuery2 =
                        new LyricsQuery(
                                        nowPlaying2.title(),
                                        nowPlaying2.artist(),
                                        nowPlaying2.album(),
                                        0)
                                .withDurationMillis(nowPlaying2.durationMs());
                if (lyricsQuery2.equals(this.lyricsCandidate)) {
                    this.lyricsCandidatePolls++;
                } else {
                    this.lyricsCandidate = lyricsQuery2;
                    this.lyricsCandidatePolls = 1;
                }
                if (this.lyricsCandidatePolls >= 2) {
                    this.lyricsQuery = this.lyricsCandidate;
                }
                if (this.lyricsQuery != null
                        && this.lyricsQuery.title().equals(nowPlaying2.title())
                        && this.lyricsQuery.artist().equals(nowPlaying2.artist())) {
                    if (this.published == null
                            || !this.published.query().equals(this.lyricsQuery)
                            || !this.published.title().equals(nowPlaying2.title())
                            || !this.published.artist().equals(nowPlaying2.artist())) {
                        this.published =
                                new SpotifyHudModule.TrackLyrics(
                                        nowPlaying2.title(),
                                        nowPlaying2.artist(),
                                        this.lyricsQuery);
                    }
                    return this.published;
                } else {
                    return this.published;
                }
            }
        }

        private void control(SpotifyWindow.MediaKey mediaKey) {
            if (SpotifyHudModule.this.poller == this) {
                if (this.media != null
                        && SpotifyHudModule.this.nowPlaying.source() == NowPlaying.Source.SESSION) {
                    if (switch (mediaKey) {
                                case PLAY_PAUSE -> this.media.togglePlayPause();
                                case NEXT -> this.media.skipNext();
                                case PREVIOUS -> this.media.skipPrevious();
                            }
                            || this.media.lastError() == null) {
                        return;
                    }
                    SpotifyHudModule.LOGGER.debug(
                            "[EFN] Spotify HUD: media session control failed ({}); sending the"
                                    + " media key",
                            this.media.lastError());
                }
                if (Platform.isWindows()) {
                    try {
                        SpotifyWindow.tap(mediaKey);
                    } catch (RuntimeException | LinkageError exception) {
                        SpotifyHudModule.LOGGER.debug(
                                "[EFN] Spotify HUD media key failed", (Throwable) exception);
                    }
                }
            }
        }

        private void logError(String lastError2) {
            if (lastError2 != null) {
                long l = System.currentTimeMillis();
                if (!Objects.equals(lastError2, this.lastError)
                        || l - this.lastErrorLoggedAt >= 60000L) {
                    this.lastError = lastError2;
                    this.lastErrorLoggedAt = l;
                    SpotifyHudModule.LOGGER.debug(
                            "[EFN] Spotify HUD media session: {}", lastError2);
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum SkyEmojiStyle {
        BESIDE,
        BEHIND,
        REPLACE_WORD;

        private SkyEmojiStyle() {}
    }

    @Environment(EnvType.CLIENT)
    private record TrackLyrics(String title, String artist, LyricsQuery query) {}
}
