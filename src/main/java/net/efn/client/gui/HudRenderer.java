package net.efn.client.gui;

import com.example.client.mixin.AccessorHudToastInstance;
import com.example.client.mixin.AccessorHudToastManager;
import com.mojang.blaze3d.textures.GpuTexture;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImFontGlyph;
import imgui.ImGui;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.efn.client.config.HudTheme;
import net.efn.client.config.Surface;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.ModuleRevision;
import net.efn.client.module.impl.client.HotkeysModule;
import net.efn.client.module.impl.client.HudModule;
import net.efn.client.module.impl.client.RadarModule;
import net.efn.client.module.impl.client.RegionMapModule;
import net.efn.client.module.impl.client.SpotifyHudModule;
import net.efn.client.module.impl.misc.AntiVanishModule;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.efn.client.module.impl.misc.StaffListModule;
import net.efn.client.util.spotify.AlbumArt;
import net.efn.client.util.spotify.DisplayText;
import net.efn.client.util.spotify.Lyrics;
import net.efn.client.util.spotify.NowPlaying;
import net.efn.client.util.spotify.PlaybackClock;
import net.efn.client.util.spotify.SpotifyWindow;
import net.efn.client.util.spotify.SyncedLyrics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.Toast.Visibility;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.Window;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

@Environment(EnvType.CLIENT)
public final class HudRenderer {

    private static final HudRenderer INSTANCE = new HudRenderer();

    private static final float CAP_H = 28.0F;

    private static final float PAD_X = 12.0F;

    private static final float GAP_UNIT = 4.0F;

    private static final float GAP_DIV = 10.0F;

    private static final float DIV_H = 12.0F;

    private static final float EMBLEM_H = 24.0F;

    static final float ANIM_SPEED = 12.0F;

    private static final float WIDTH_SPEED = 16.0F;

    private static final float INTRO_DURATION = 0.36F;

    private static final float INTRO_STAGGER = 0.04F;

    private static final float INTRO_SLIDE = 6.0F;

    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String MINUS = "\u2212";

    private static final String INFINITY = "\u221e";

    private static final String[] SECONDS =
            IntStream.range(0, 60).mapToObj(i -> String.format(":%02d", i)).toArray(String[]::new);

    private final HudLayoutStore layout = HudLayoutStore.getInstance();

    private final ModuleListRows moduleRows = new ModuleListRows();

    private final TargetCard targetCard = new TargetCard();

    private final ArmorStatus armorStatus = new ArmorStatus();

    private final CooldownTray cooldownTray = new CooldownTray();

    private final Map<String, Int2ObjectOpenHashMap<StableWidth>> slots = new HashMap<>();

    private int slotCount;

    private float moduleListW;

    private float toastShift;

    private boolean layoutDirty;

    private long lastWheelNanos;

    private final HudModule hudModule;

    private final RegionMapModule regionMapModule;

    private final RadarModule radarModule;

    private float introTime;

    private boolean hudVisibleLastFrame;

    private String draggingId;

    private float dragOffsetX;

    private static final SpotifyWindow.MediaKey[] SPOTIFY_BUTTONS =
            new SpotifyWindow.MediaKey[] {
                SpotifyWindow.MediaKey.PREVIOUS,
                SpotifyWindow.MediaKey.PLAY_PAUSE,
                SpotifyWindow.MediaKey.NEXT
            };

    private final float[] spotifyButtons = new float[12];

    private boolean spotifyButtonsShown;

    private boolean spotifyButtonsDrawn;

    private boolean chatInput;

    private float dragOffsetY;

    private Float snapGuideX;

    private Float snapGuideY;

    private static int studioFrame = -2;

    private static volatile float[] infoBarBox;

    private static volatile long infoBarDrawnAt;

    private static final float HIDDEN_ALPHA = 0.35F;

    private long infoSecond = -9223372036854775808L;

    private int infoFps = -1;

    private int infoPing = -1;

    private List<HudRenderer.InfoItem> infoItems = List.of();

    private static final float ROWS_RADIUS = 6.0F;

    private static final int ROWS_SHADOW_LAYERS = 3;

    private static final int ROWS_TEXT = -1;

    private static final float COORD_ROW_H = 20.0F;

    private static final float COORD_PAD = 7.0F;

    private static final float COORD_ICON = 16.0F;

    private static final float COORD_GAP_ICON = 8.0F;

    private static final float COORD_GAP = 12.0F;

    private static final float COORD_ELSEWHERE_ALPHA = 0.62F;

    private static final String[] COORD_SLOTS = new String[] {"coords.0", "coords.1", "coords.2"};

    private final Map<String, Float> coordAnim = new HashMap<>();

    private static final float LIST_ROW_H = 18.0F;

    private static final float LIST_PAD = 6.0F;

    private static final String NO_MODULES = "No active modules";

    private static final float TOAST_GAP = 10.0F;

    private static final float POTION_ROW_H = 22.0F;

    private static final float POTION_PAD = 6.0F;

    private static final float ICON = 18.0F;

    private static final float GAP_ICON = 6.0F;

    private static final float GAP_DURATION = 14.0F;

    private static final String NO_EFFECTS = "No active effects";

    private static final float VANISH_ROW_H = 22.0F;

    private static final float VANISH_PAD = 8.0F;

    private static final float GAP_TAG = 8.0F;

    private static final String NO_VANISH = "No vanish alerts";

    private static final float STAFF_ROW_H = 22.0F;

    private static final float STAFF_HEADER_H = 20.0F;

    private static final float STAFF_PAD = 8.0F;

    private static final float STAFF_FACE = 16.0F;

    private static final float GAP_FACE = 6.0F;

    private static final float GAP_COUNT = 5.0F;

    private static final float GAP_STATE = 14.0F;

    private static final String NO_STAFF = "No staff online";

    private static final String STAFF_HEADER = "STAFF";

    private static final String WATCHING_YOU = "WATCHING YOU";

    private static final float SKIN_FACE_U = 0.125F;

    private static final float SKIN_HAT_U = 0.625F;

    private static final float SKIN_TOP_V = 0.125F;

    private static final float SKIN_PART = 0.125F;

    private final Map<UUID, Identifier> staffSkins = new HashMap<>();

    private final Set<GpuTexture> nearestSkins = Collections.newSetFromMap(new WeakHashMap<>());

    private static final float RADAR_DEFAULT_SIZE = 150.0F;

    private static final float[] RADAR_RINGS = new float[] {0.33333334F, 0.6666667F};

    private static final float RADAR_LETTER_INSET = 10.0F;

    private static final float RADAR_LETTER_CLEAR = 4.0F;

    private static final float RADAR_EDGE = 5.0F;

    private static final float RADAR_SELF = 3.0F;

    private static final float RADAR_MARKER_INSET = 1.5F;

    private static final float RADAR_MARKER_H = 5.0F;

    private static final float RADAR_MARKER_W = 3.5F;

    private static final float RADAR_DOT = 3.2F;

    private static final String[] RADAR_LETTERS = new String[] {"N", "E", "S", "W"};

    private static final int[] RADAR_LETTER_DX = new int[] {0, 1, 0, -1};

    private static final int[] RADAR_LETTER_DZ = new int[] {-1, 0, 1, 0};

    private static final int RADAR_DOT_EDGE = -1946157056;

    private final float[] radarLetterW = new float[4];

    private float radarLetterMid;

    private float radarLetterHalf;

    private ImFont radarLetterFont;

    private int radarLetterPx = -1;

    private static final float HOTKEY_ROW_H = 22.0F;

    private static final float HOTKEY_HEADER_H = 20.0F;

    private static final float HOTKEY_PAD = 8.0F;

    private static final float GAP_HOTKEY = 16.0F;

    private static final float KEYCAP_H = 16.0F;

    private static final float KEYCAP_PAD = 5.0F;

    private static final float KEYCAP_RADIUS = 4.0F;

    private static final String HOTKEYS_HEADER = "HOTKEYS";

    private static final String NO_HOTKEYS = "No keys bound";

    private List<HudRenderer.HotkeyLine> hotkeyLines = List.of();

    private long hotkeyRevision = -9223372036854775808L;

    private ImFont hotkeyFont;

    private int hotkeyTextPx = -1;

    private int hotkeyKeyPx = -1;

    private float hotkeyNameColumn;

    private float hotkeyKeyColumn;

    private float hotkeyHeaderW;

    private float hotkeyCapMid;

    private static final float SPOTIFY_PAD_Y = 8.0F;

    private static final float SPOTIFY_LINE_GAP = 2.0F;

    private static final float SPOTIFY_GLYPH = 10.0F;

    private static final float SPOTIFY_GLYPH_GAP = 10.0F;

    private static final float SPOTIFY_PAUSED = 0.62F;

    private static final float MARQUEE_SPEED = 30.0F;

    private static final float MARQUEE_HOLD = 2.4F;

    private static final float MARQUEE_GAP = 40.0F;

    private static final String ELLIPSIS = "...";

    private static final float CARD_PAD = 10.0F;

    private static final float CARD_MIN_WIDTH = 200.0F;

    private static final float CARD_DEFAULT_WIDTH = 320.0F;

    private static final float ART_GAP = 10.0F;

    private static final float ART_MIN_COLUMN = 128.0F;

    private static final float ART_RADIUS = 4.0F;

    private static final int ART_PLACEHOLDER = 318767103;

    private static final float LABEL_GAP = 3.0F;

    private static final float ARTIST_GAP = 1.0F;

    private static final float TRANSPORT_GAP = 7.0F;

    private static final float PROGRESS_GAP = 6.0F;

    private static final float PROGRESS_H = 2.0F;

    private static final float SKIP_GLYPH = 8.0F;

    private static final float PLAY_GLYPH = 10.0F;

    private static final float GLYPH_SPACING = 14.0F;

    private static final float GLYPH_MARGIN = 8.0F;

    private static final float LYRICS_GAP = 12.0F;

    private static final float LYRIC_ROW_GAP = 6.0F;

    private static final float LYRIC_LINE_GAP = 6.0F;

    private static final float LYRIC_SPRING = 13.0F;

    private static final float LYRIC_SETTLE_TIME = 1.1F;

    private static final float LYRIC_STAGGER = 0.045F;

    private static final int LYRIC_MAX_STAGGERED = 6;

    private static final int LYRIC_MAX_SHIFTS = 8;

    private static final float LYRIC_GLOW_TIME = 0.35F;

    private static final float LYRIC_ARRIVE_TIME = 0.3F;

    private static final float LYRIC_FADE_TIME = 0.32F;

    private static final float LYRIC_RISE = 8.0F;

    private static final int LYRIC_MAX_ROWS = 4;

    private static final float LYRIC_GROW_SPEED = 14.0F;

    private static final float LYRIC_SHRINK_SPEED = 9.0F;

    private static final float LYRIC_FAR_ALPHA = 0.6F;

    private static final float LYRIC_PAUSED_ALPHA = 0.7F;

    private static final float LYRIC_PAUSE_SPEED = 8.0F;

    private static final float LYRIC_BREATH_PERIOD = 1.6F;

    private static final long LYRIC_LEAD_MS = 200L;

    private static final long LYRIC_SWEEP_MS = 180L;

    private static final float LYRIC_SLOW_PACE = 2.0F;

    private static final float LYRIC_CHAR_MS = 90.0F;

    private static final float LYRIC_LIT_ALL = 1.0F / 0.0F;

    private static final long LYRIC_QUIET_MS = 700L;

    private static final float LYRIC_MIN_COVERAGE = 0.75F;

    private static final String[] NO_ROWS = new String[0];

    private static final NowPlaying SAMPLE_TRACK =
            new NowPlaying(
                    NowPlaying.Status.PLAYING,
                    "Golden Hour Drive",
                    "Sample Artist",
                    "",
                    NowPlaying.Source.SESSION,
                    214000L,
                    84000L,
                    0L,
                    1.0,
                    null);

    private static final Lyrics SAMPLE_LYRICS =
            new Lyrics(
                    Lyrics.Status.SYNCED,
                    SyncedLyrics.parse(
                            "[01:21.00]Every light in the city knows my name\n"
                                    + "[01:27.40]We keep on driving till the morning comes\n"
                                    + "[01:33.80]Nothing but the radio and you\n"),
                    List.of(),
                    0L);

    private float marqueeTime;

    private String marqueeTitle = "";

    private String marqueeArtist = "";

    private final PlaybackClock spotifyClock = new PlaybackClock();

    private NowPlaying spotifyFolded;

    private String spotifyTitle = "";

    private String spotifyArtist = "";

    private AlbumArt spotifyArt;

    private ThemeImages.Texture spotifyArtTexture;

    private final Map<Long, Float> capTops = new HashMap<>();

    private Lyrics lyricPrepared;

    private String[] lyricTexts = NO_ROWS;

    private String[][] lyricRows = new String[0][];

    private int lyricRowsPx;

    private int lyricRowsWidth;

    private HudRenderer.LyricWords[] lyricWords = new HudRenderer.LyricWords[0];

    private float lyricCharMs = 90.0F;

    private boolean lyricReadable = true;

    private float lyricViewH = -1.0F;

    private int lyricViewLines;

    private float lyricViewScale;

    private SyncedLyrics lyricSource;

    private int lyricStart;

    private final List<HudRenderer.LyricShift> lyricShifts = new ArrayList<>(8);

    private float lyricClock;

    private List<HudRenderer.LyricItem> lyricFadeItems;

    private float lyricFade = 1.0F;

    private List<HudRenderer.LyricItem> lyricLastItems = List.of();

    private List<HudRenderer.LyricItem> lyricLeaveItems;

    private float lyricLeave;

    private float[] lyricShown = new float[0];

    private float lyricBreath;

    private float lyricPlaying = 1.0F;

    private int lyricCurrentFirst = -1;

    private int lyricCurrentLast = -1;

    private float[] lyricBright = new float[0];

    private int lyricBrightLo;

    private int lyricBrightHi = -1;

    private float lyricBlockH;

    private long lyricLoadingSince;

    private ImFont glyphFont;

    private final Map<Integer, Boolean> glyphs = new HashMap<>();

    private static final float SNAP_THRESHOLD = 6.0F;

    private HudRenderer() {
        this.hudModule = ModuleRegistry.get(HudModule.class);
        this.regionMapModule = ModuleRegistry.get(RegionMapModule.class);
        this.radarModule = ModuleRegistry.get(RadarModule.class);
    }

    public static HudRenderer getInstance() {
        return INSTANCE;
    }

    public static float[] infoBarBox() {
        return System.nanoTime() - infoBarDrawnAt < 250000000L ? infoBarBox : null;
    }

    static void markStudioPreview() {
        studioFrame = ImGuiManager.getInstance().frameCount();
    }

    static boolean studioPreview() {
        int i = ImGuiManager.getInstance().frameCount() - studioFrame;
        return i == 0 || i == 1;
    }

    boolean isDragging() {
        return this.draggingId != null;
    }

    void studioInput(float f, float g, boolean bl) {
        markStudioPreview();
        if (!bl || this.draggingId != null) {
            this.updateEditorInput(f, g);
        }
    }

    public void render() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            this.releaseSpotifyArt();
            this.resetCombatElements();
        } else {
            boolean bl = client.currentScreen instanceof HudEditorScreen;
            boolean studioPreview = studioPreview();
            boolean bl2 = bl || studioPreview;
            if (client.currentScreen instanceof ImGuiScreen && !studioPreview) {
                if (this.draggingId != null || this.layoutDirty) {
                    this.saveLayout();
                }
                this.draggingId = null;
                this.snapGuideX = null;
                this.snapGuideY = null;
                this.hudVisibleLastFrame = false;
            } else {
                boolean bl3 = client.currentScreen instanceof ChatScreen;
                boolean bl4 = bl || bl3;
                this.chatInput = bl3;
                this.spotifyButtonsShown = this.spotifyButtonsDrawn;
                this.spotifyButtonsDrawn = false;
                boolean shouldRenderHudOverlays = shouldRenderHudOverlays(client);
                float f = Math.max(ImGui.getIO().getDeltaTime(), 0.001F);
                float displaySizeX = ImGui.getIO().getDisplaySizeX();
                float displaySizeY = ImGui.getIO().getDisplaySizeY();
                this.flushLayoutIfIdle();
                boolean hudOverlays = computeHudOverlays();
                boolean regionMapEnabled = this.isRegionMapEnabled();
                HudLayoutStore.HudElementState hudElementState = this.layout.element("region_map");
                boolean bl5 = regionMapEnabled && hudElementState.visible;
                boolean bl6 = this.hudModule != null && (bl2 || this.hudModule.isEnabled());
                if (!bl6) {
                    this.hudVisibleLastFrame = false;
                    this.introTime = 0.0F;
                    this.clearSlots();
                    this.moduleListW = 0.0F;
                    this.toastShift = 0.0F;
                    this.releaseSpotifyArt();
                    this.resetCombatElements();
                    if ((hudOverlays || bl5) && shouldRenderHudOverlays) {
                        this.drawOverlayOnly(displaySizeX, displaySizeY, bl3, bl5);
                    }
                } else {
                    boolean bl7 = bl2 || this.hudModule.ping();
                    boolean bl8 = bl2 || this.hudModule.fps();
                    boolean bl9 = bl2 || this.hudModule.clock();
                    boolean shadow = this.hudModule.shadow();
                    this.updateIntro(bl2, f);
                    this.moduleRows.update(ModuleRegistry.enabled(), bl2, f);
                    if (this.slotCount > 256) {
                        this.clearSlots();
                    }
                    HudLayoutStore.HudElementState hudElementState2 =
                            this.layout.element("info_bar");
                    HudLayoutStore.HudElementState hudElementState3 =
                            this.layout.element("coordinates");
                    HudLayoutStore.HudElementState hudElementState4 =
                            this.layout.element("module_list");
                    HudLayoutStore.HudElementState hudElementState5 =
                            this.layout.element("potion_tracker");
                    HudLayoutStore.HudElementState hudElementState6 =
                            this.layout.element("anti_vanish");
                    HudLayoutStore.HudElementState hudElementState7 =
                            this.layout.element("staff_list");
                    HudLayoutStore.HudElementState hudElementState8 =
                            this.layout.element("spotify");
                    HudLayoutStore.HudElementState hudElementState9 = this.layout.element("radar");
                    HudLayoutStore.HudElementState hudElementState10 =
                            this.layout.element("hotkeys");
                    HudLayoutStore.HudElementState hudElementState11 =
                            this.layout.element("target_card");
                    HudLayoutStore.HudElementState hudElementState12 =
                            this.layout.element("armor_status");
                    HudLayoutStore.HudElementState hudElementState13 =
                            this.layout.element("item_cooldowns");
                    HudRenderer.InfoPlan infoPlan =
                            this.planInfoBar(
                                    hudElementState2,
                                    this.buildInfoItems(bl8, bl9, bl7, client),
                                    f);
                    this.layout.updateBounds(
                            hudElementState2.id,
                            infoPlan.width(),
                            infoPlan.height(),
                            displaySizeX,
                            displaySizeY);
                    HudRenderer.CoordPlan coordPlan =
                            this.planCoordinates(
                                    hudElementState3, this.coordinateRows(client, bl2, f), bl2, f);
                    this.layout.updateBounds(
                            hudElementState3.id,
                            coordPlan.width(),
                            coordPlan.height(),
                            displaySizeX,
                            displaySizeY);
                    HudRenderer.ListPlan listPlan = this.planModuleList(hudElementState4, bl2, f);
                    this.layout.updateBounds(
                            hudElementState4.id,
                            listPlan.width(),
                            listPlan.height(),
                            displaySizeX,
                            displaySizeY);
                    HudRenderer.PotionPlan potionPlan =
                            this.planPotions(
                                    hudElementState5, this.buildPotionRows(client), bl2, f);
                    this.layout.updateBounds(
                            hudElementState5.id,
                            potionPlan.width(),
                            potionPlan.height(),
                            displaySizeX,
                            displaySizeY);
                    HudRenderer.VanishPlan vanishPlan = this.planVanish(hudElementState6, bl2, f);
                    this.layout.updateBounds(
                            hudElementState6.id,
                            vanishPlan.width(),
                            vanishPlan.height(),
                            displaySizeX,
                            displaySizeY);
                    HudRenderer.StaffPlan staffPlan = this.planStaff(hudElementState7, bl2, f);
                    this.layout.updateBounds(
                            hudElementState7.id,
                            staffPlan.width(),
                            staffPlan.height(),
                            displaySizeX,
                            displaySizeY);
                    HudRenderer.SpotifyPlan spotifyPlan =
                            this.planSpotify(hudElementState8, bl2, f);
                    this.layout.updateBounds(
                            hudElementState8.id,
                            spotifyPlan.width(),
                            spotifyPlan.height(),
                            displaySizeX,
                            displaySizeY);
                    float planRadar = this.planRadar(hudElementState9, bl2);
                    this.layout.updateBounds(
                            hudElementState9.id, planRadar, planRadar, displaySizeX, displaySizeY);
                    HudRenderer.HotkeyPlan hotkeyPlan = this.planHotkeys(hudElementState10, bl2, f);
                    this.layout.updateBounds(
                            hudElementState10.id,
                            hotkeyPlan.width(),
                            hotkeyPlan.height(),
                            displaySizeX,
                            displaySizeY);
                    boolean bl10 = this.hudModule.isEnabled() && !client.options.hudHidden;
                    boolean bl11 = bl10 && hudElementState11.visible && this.hudModule.targetCard();
                    boolean bl12 =
                            bl10 && hudElementState12.visible && this.hudModule.armorStatus();
                    boolean bl13 =
                            bl10 && hudElementState13.visible && this.hudModule.itemCooldowns();
                    float scale = scaleOf(hudElementState11);
                    float scale2 = scaleOf(hudElementState12);
                    float scale3 = scaleOf(hudElementState13);
                    this.targetCard.update(client, this.hudModule, bl11, bl2, f);
                    this.layout.updateBounds(
                            hudElementState11.id,
                            this.targetCard.planWidth(scale),
                            this.targetCard.planHeight(scale),
                            displaySizeX,
                            displaySizeY);
                    this.armorStatus.update(client, this.hudModule, bl12, bl2, f);
                    this.layout.updateBounds(
                            hudElementState12.id,
                            this.armorStatus.planWidth(scale2),
                            this.armorStatus.planHeight(scale2),
                            displaySizeX,
                            displaySizeY);
                    this.cooldownTray.update(client, this.hudModule, bl13, bl2, f);
                    this.layout.updateBounds(
                            hudElementState13.id,
                            this.cooldownTray.planWidth(scale3),
                            this.cooldownTray.planHeight(scale3),
                            displaySizeX,
                            displaySizeY);
                    if (regionMapEnabled || bl2) {
                        this.updateRegionMapBounds(displaySizeX, displaySizeY);
                    }
                    if (bl4) {
                        this.updateEditorInput(displaySizeX, displaySizeY);
                    } else if (!studioPreview) {
                        if (this.draggingId != null || this.layoutDirty) {
                            this.saveLayout();
                        }
                        this.draggingId = null;
                        this.snapGuideX = null;
                        this.snapGuideY = null;
                    }
                    float updateToastShift =
                            this.updateToastShift(client, hudElementState4, bl2, f);
                    ImGui.setNextWindowPos(0.0F, 0.0F, 1);
                    ImGui.setNextWindowSize(displaySizeX, displaySizeY);
                    int i = 205215;
                    if (!bl4) {
                        i |= 197120;
                    }
                    ImGui.pushStyleVar(4, 0.0F);
                    if (!ImGui.begin("##hud_overlay", i)) {
                        ImGui.end();
                        ImGui.popStyleVar();
                    } else {
                        ImGui.popStyleVar();
                        ImDrawList windowDrawList = ImGui.getWindowDrawList();
                        if (shouldRenderHudOverlays) {
                            drawHudOverlays(windowDrawList, displaySizeX, displaySizeY);
                        }
                        boolean enabled2;
                        boolean bl14 =
                                (enabled2 = this.hudModule.isEnabled())
                                        && hudElementState2.visible
                                        && (this.hudModule.fps()
                                                || this.hudModule.clock()
                                                || this.hudModule.ping());
                        boolean bl15 = enabled2 && hudElementState3.visible;
                        boolean bl16 =
                                enabled2 && hudElementState4.visible && this.hudModule.moduleList();
                        boolean bl17 =
                                enabled2 && hudElementState5.visible && this.hudModule.potions();
                        boolean bl18 =
                                enabled2
                                        && hudElementState6.visible
                                        && AntiVanishModule.shouldShowHud();
                        boolean bl19 =
                                enabled2
                                        && hudElementState7.visible
                                        && StaffListModule.shouldShowHud();
                        boolean bl20 =
                                enabled2
                                        && hudElementState8.visible
                                        && SpotifyHudModule.shouldShowHud();
                        boolean gameHidesHud = gameHidesHud(client);
                        boolean bl21 =
                                enabled2
                                        && hudElementState9.visible
                                        && RadarModule.shouldShowHud()
                                        && !gameHidesHud;
                        boolean bl22 =
                                enabled2
                                        && hudElementState10.visible
                                        && HotkeysModule.shouldShowHud()
                                        && !gameHidesHud;
                        if (bl14 && !infoPlan.segments().isEmpty() || bl2) {
                            this.drawInfoBar(
                                    windowDrawList,
                                    hudElementState2,
                                    infoPlan,
                                    shadow,
                                    this.transform(hudElementState2, bl2, bl14));
                        }
                        if (bl14 && !infoPlan.segments().isEmpty() && !bl2) {
                            infoBarBox =
                                    new float[] {
                                        hudElementState2.x,
                                        hudElementState2.y,
                                        hudElementState2.x + hudElementState2.width,
                                        hudElementState2.y + hudElementState2.height
                                    };
                            infoBarDrawnAt = System.nanoTime();
                        }
                        if (bl15 && !coordPlan.rows().isEmpty() || bl2) {
                            this.drawCoordinates(
                                    windowDrawList,
                                    hudElementState3,
                                    coordPlan,
                                    shadow,
                                    this.transform(hudElementState3, bl2, bl15),
                                    displaySizeX);
                        }
                        if (bl16 && !listPlan.rows().isEmpty() || bl2) {
                            this.drawModuleList(
                                    windowDrawList,
                                    hudElementState4,
                                    listPlan,
                                    shadow,
                                    this.transform(hudElementState4, bl2, bl16)
                                            .down(updateToastShift),
                                    displaySizeX);
                        }
                        if (bl17 && !potionPlan.lines().isEmpty() || bl2) {
                            this.drawPotions(
                                    windowDrawList,
                                    hudElementState5,
                                    potionPlan,
                                    shadow,
                                    this.transform(hudElementState5, bl2, bl17));
                        }
                        if (bl18 && !vanishPlan.lines().isEmpty() || bl2) {
                            this.drawVanish(
                                    windowDrawList,
                                    hudElementState6,
                                    vanishPlan,
                                    shadow,
                                    this.transform(hudElementState6, bl2, bl18));
                        }
                        if (bl19 || bl2) {
                            this.drawStaff(
                                    windowDrawList,
                                    hudElementState7,
                                    staffPlan,
                                    shadow,
                                    this.transform(hudElementState7, bl2, bl19));
                        }
                        if (bl20 || bl2) {
                            this.drawSpotify(
                                    windowDrawList,
                                    hudElementState8,
                                    spotifyPlan,
                                    shadow,
                                    this.transform(hudElementState8, bl2, bl20));
                        }
                        if (bl21 || bl2) {
                            this.drawRadar(
                                    windowDrawList,
                                    hudElementState9,
                                    planRadar,
                                    client,
                                    shadow,
                                    this.transform(hudElementState9, bl2, bl21));
                        }
                        if (bl22 && !hotkeyPlan.lines().isEmpty() || bl2) {
                            this.drawHotkeys(
                                    windowDrawList,
                                    hudElementState10,
                                    hotkeyPlan,
                                    shadow,
                                    this.transform(hudElementState10, bl2, bl22));
                        }
                        if (this.targetCard.visible()) {
                            HudRenderer.DrawTransform drawTransform =
                                    this.transform(hudElementState11, bl2, bl11);
                            this.targetCard.draw(
                                    windowDrawList,
                                    hudElementState11,
                                    Math.round(hudElementState11.x + drawTransform.xOffset()),
                                    Math.round(hudElementState11.y + drawTransform.yOffset()),
                                    drawTransform.alpha(),
                                    shadow,
                                    this.hudModule);
                        }
                        if (this.armorStatus.visible()) {
                            HudRenderer.DrawTransform drawTransform2 =
                                    this.transform(hudElementState12, bl2, bl12);
                            this.armorStatus.draw(
                                    windowDrawList,
                                    hudElementState12,
                                    Math.round(hudElementState12.x + drawTransform2.xOffset()),
                                    Math.round(hudElementState12.y + drawTransform2.yOffset()),
                                    drawTransform2.alpha(),
                                    shadow,
                                    client);
                        }
                        if (this.cooldownTray.visible()) {
                            HudRenderer.DrawTransform drawTransform3 =
                                    this.transform(hudElementState13, bl2, bl13);
                            this.cooldownTray.draw(
                                    windowDrawList,
                                    hudElementState13,
                                    Math.round(hudElementState13.x + drawTransform3.xOffset()),
                                    Math.round(hudElementState13.y + drawTransform3.yOffset()),
                                    drawTransform3.alpha(),
                                    shadow,
                                    this.hudAccent());
                        }
                        if (bl5 || bl2) {
                            RegionMapRenderer.render(
                                    windowDrawList,
                                    this.regionMapModule,
                                    hudElementState,
                                    bl2 && !bl5 ? 0.35F : 1.0F);
                        }
                        if (bl) {
                            this.drawEditorChrome(windowDrawList, displaySizeX, displaySizeY);
                        } else if (studioPreview) {
                            this.drawSnapGuides(windowDrawList, displaySizeX, displaySizeY);
                        }
                        ImGui.end();
                    }
                }
            }
        }
    }

    private HudRenderer.DrawTransform transform(
            HudLayoutStore.HudElementState hudElementState, boolean bl, boolean bl2) {
        HudRenderer.DrawTransform drawTransform = this.introTransform(hudElementState, bl);
        return bl && !bl2
                ? new HudRenderer.DrawTransform(
                        drawTransform.xOffset(),
                        drawTransform.yOffset(),
                        drawTransform.alpha() * 0.35F)
                : drawTransform;
    }

    private void resetCombatElements() {
        this.targetCard.reset();
        this.armorStatus.reset();
        this.cooldownTray.reset();
    }

    private void saveLayout() {
        this.layout.save();
        this.layoutDirty = false;
    }

    private void flushLayoutIfIdle() {
        if (this.layoutDirty && System.nanoTime() - this.lastWheelNanos > 400000000L) {
            this.saveLayout();
        }
    }

    private void drawOverlayOnly(float f, float g, boolean bl, boolean bl2) {
        if (bl2) {
            this.updateRegionMapBounds(f, g);
        }
        ImGui.setNextWindowPos(0.0F, 0.0F, 1);
        ImGui.setNextWindowSize(f, g);
        int i = 205215;
        if (!bl) {
            i |= 197120;
        }
        ImGui.pushStyleVar(4, 0.0F);
        if (!ImGui.begin("##efn_overlay_only", i)) {
            ImGui.end();
            ImGui.popStyleVar();
        } else {
            ImGui.popStyleVar();
            if (bl) {
                this.updateEditorInput(f, g);
            } else {
                if (this.draggingId != null || this.layoutDirty) {
                    this.saveLayout();
                }
                this.draggingId = null;
            }
            ImDrawList windowDrawList = ImGui.getWindowDrawList();
            drawHudOverlays(windowDrawList, f, g);
            if (bl2 && this.regionMapModule != null) {
                RegionMapRenderer.render(
                        windowDrawList, this.regionMapModule, this.layout.element("region_map"));
            }
            ImGui.end();
        }
    }

    private boolean isRegionMapEnabled() {
        return this.regionMapModule != null && this.regionMapModule.isEnabled();
    }

    private void updateRegionMapBounds(float f, float g) {
        if (this.regionMapModule != null) {
            HudLayoutStore.HudElementState hudElementState = this.layout.element("region_map");
            RegionMapRenderer.Size size =
                    RegionMapRenderer.measureSize(this.regionMapModule, scaleOf(hudElementState));
            this.layout.updateBounds("region_map", size.width(), size.height(), f, g);
        }
    }

    public boolean hasWork() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            this.resetCombatElements();
            return false;
        } else {
            return client.currentScreen instanceof HudEditorScreen
                    || studioPreview()
                    || this.hudModule != null
                            && (this.hudModule.isEnabled() || this.hudVisibleLastFrame)
                    || this.isRegionMapEnabled()
                    || computeHudOverlays();
        }
    }

    private static boolean shouldRenderHudOverlays(MinecraftClient client) {
        return client.currentScreen == null || client.currentScreen instanceof ChatScreen;
    }

    private static boolean gameHidesHud(MinecraftClient client) {
        return client.options.hudHidden || client.debugHudEntryList.isF3Enabled();
    }

    private static boolean computeHudOverlays() {
        for (HudOverlayProvider hudOverlayProvider :
                ModuleRegistry.instancesOf(HudOverlayProvider.class)) {
            if (hudOverlayProvider.isHudOverlayEnabled()) {
                return true;
            }
        }
        return false;
    }

    private static void drawHudOverlays(ImDrawList imDrawList, float f, float g) {
        for (HudOverlayProvider hudOverlayProvider :
                ModuleRegistry.instancesOf(HudOverlayProvider.class)) {
            if (hudOverlayProvider.isHudOverlayEnabled()) {
                hudOverlayProvider.renderHudOverlay(imDrawList, f, g);
            }
        }
    }

    private void updateIntro(boolean bl, float f) {
        float g = 0.56F;
        if (bl) {
            this.introTime = g;
            this.hudVisibleLastFrame = true;
        } else if (!this.hudVisibleLastFrame) {
            this.introTime = 0.0F;
            this.hudVisibleLastFrame = true;
        } else {
            this.introTime = Math.min(this.introTime + f, g);
        }
    }

    private HudRenderer.DrawTransform introTransform(
            HudLayoutStore.HudElementState hudElementState, boolean bl) {
        if (bl) {
            return HudRenderer.DrawTransform.identity();
        } else {
            float f = clamp01((this.introTime - introDelay(hudElementState.id)) / 0.36F);
            float easeOutCubic = easeOutCubic(f);
            float g = 6.0F * scaleOf(hudElementState) * (1.0F - easeOutCubic);
            return switch (hudElementState.anchorX) {
                case 1 -> new HudRenderer.DrawTransform(0.0F, -g, easeOutCubic);
                case 2 -> new HudRenderer.DrawTransform(g, 0.0F, easeOutCubic);
                default -> new HudRenderer.DrawTransform(-g, 0.0F, easeOutCubic);
            };
        }
    }

    private static float introDelay(String string) {
        return switch (string) {
            case "info_bar" -> 0.04F;
            case "coordinates" -> 0.08F;
            case "anti_vanish" -> 0.12F;
            case "spotify" -> 0.12F;
            case "module_list" -> 0.16F;
            case "staff_list" -> 0.16F;
            case "hotkeys" -> 0.16F;
            case "radar" -> 0.19999999F;
            case "potion_tracker" -> 0.19999999F;
            case "armor_status" -> 0.16F;
            case "item_cooldowns" -> 0.12F;
            case "target_card" -> 0.08F;
            default -> 0.0F;
        };
    }

    private float slot(String string, int i, float f, float g) {
        Int2ObjectOpenHashMap int2ObjectOpenHashMap =
                this.slots.computeIfAbsent(string, stringx -> new Int2ObjectOpenHashMap<>());
        StableWidth stableWidth = (StableWidth) int2ObjectOpenHashMap.get(i);
        if (stableWidth == null) {
            stableWidth = new StableWidth();
            int2ObjectOpenHashMap.put(i, stableWidth);
            this.slotCount++;
        }
        return stableWidth.update(f, g);
    }

    private void removeSlot(String string, int i) {
        Int2ObjectOpenHashMap slot2 = this.slots.get(string);
        if (slot2 != null && slot2.remove(i) != null) {
            this.slotCount--;
        }
    }

    private void clearSlots() {
        this.slots.clear();
        this.slotCount = 0;
    }

    private static String number(long l2) {
        return l2 < 0L ? "\u2212" + -l2 : Long.toString(l2);
    }

    private List<HudRenderer.InfoItem> buildInfoItems(
            boolean bl, boolean bl2, boolean bl3, MinecraftClient client) {
        long l2 = bl2 ? System.currentTimeMillis() / 1000L : -1L;
        int i = bl ? Math.max(0, client.getCurrentFps()) : -1;
        int j = -1;
        if (bl3) {
            ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
            int k = 0;
            if (networkHandler != null
                    && client.player != null
                    && (k = readTabLatency(client, networkHandler)) <= 0) {
                k = readServerPing(networkHandler);
            }
            j = Math.max(0, k);
        }
        if (l2 == this.infoSecond && i == this.infoFps && j == this.infoPing) {
            return this.infoItems;
        } else {
            this.infoSecond = l2;
            this.infoFps = i;
            this.infoPing = j;
            ArrayList list = new ArrayList(3);
            if (bl2) {
                LocalTime localTime = LocalTime.now();
                list.add(
                        new HudRenderer.InfoItem(
                                "info.time",
                                localTime.format(CLOCK_FORMAT),
                                SECONDS[localTime.getSecond()],
                                null));
            }
            if (bl) {
                list.add(new HudRenderer.InfoItem("info.fps", String.valueOf(i), null, "fps"));
            }
            if (bl3) {
                list.add(new HudRenderer.InfoItem("info.ping", String.valueOf(j), null, "ms"));
            }
            this.infoItems = List.copyOf(list);
            return this.infoItems;
        }
    }

    private HudRenderer.InfoPlan planInfoBar(
            HudLayoutStore.HudElementState hudElementState,
            List<HudRenderer.InfoItem> list,
            float f) {
        float scale = scaleOf(hudElementState);
        HudRenderer.Type type2 = HudRenderer.Type.of(scale);
        ArrayList list2 = new ArrayList(list.size());
        float g = 12.0F * scale * 2.0F;
        for (int i = 0; i < list.size(); i++) {
            HudRenderer.InfoItem infoItem = (HudRenderer.InfoItem) list.get(i);
            float tabularWidth =
                    HudChrome.tabularWidth(type2.value(), type2.valuePx(), infoItem.value());
            float h =
                    infoItem.dim() != null
                            ? HudChrome.tabularWidth(type2.value(), type2.valuePx(), infoItem.dim())
                            : 0.0F;
            float f2 = tabularWidth + h;
            if (infoItem.unit() != null) {
                f2 +=
                        4.0F * scale
                                + HudChrome.width(type2.label(), type2.labelPx(), infoItem.unit());
            }
            float slot2 = this.slot(infoItem.slot(), type2.valuePx(), (float) Math.ceil(f2), f);
            list2.add(new HudRenderer.Segment(infoItem, tabularWidth, h, f2, slot2));
            g += slot2;
        }
        if (list2.size() > 1) {
            g += (list2.size() - 1) * (10.0F * scale * 2.0F + 1.0F);
        }
        float g2 = 0.0F;
        ThemeImages.Texture texture =
                this.hudModule != null && this.hudModule.logo() && !list2.isEmpty()
                        ? HudEmblem.forHeight(Math.round(24.0F * scale))
                        : null;
        if (texture != null) {
            g2 = texture.width();
            g += g2 + 10.0F * scale * 2.0F + 1.0F;
        }
        return new HudRenderer.InfoPlan(
                list2, type2, (float) Math.round(g), (float) Math.round(28.0F * scale), g2);
    }

    private void drawInfoBar(
            ImDrawList imDrawList,
            HudLayoutStore.HudElementState hudElementState,
            HudRenderer.InfoPlan infoPlan,
            boolean bl,
            HudRenderer.DrawTransform drawTransform) {
        float alpha2 = drawTransform.alpha();
        if (!(alpha2 <= 0.001F)) {
            float scale = scaleOf(hudElementState);
            float f = Math.round(hudElementState.x + drawTransform.xOffset());
            float g = Math.round(hudElementState.y + drawTransform.yOffset());
            float width2 = hudElementState.width;
            float height2 = hudElementState.height;
            Surface surface = hudSurface(hudElementState);
            boolean needsTextGuard = HudChrome.needsTextGuard(surface);
            HudChrome.card(
                    imDrawList,
                    surface,
                    f,
                    g,
                    width2,
                    height2,
                    hudRounding() * scale,
                    alpha2,
                    bl,
                    scale);
            HudRenderer.Type type2 = infoPlan.type();
            float h = g + (float) Math.floor((height2 - type2.valuePx()) * 0.5F);
            float f2 =
                    h
                            + HudChrome.baselineOffset(
                                    type2.value(), type2.valuePx(), type2.label(), type2.labelPx());
            float g2 = f + 12.0F * scale;
            ThemeImages.Texture texture =
                    infoPlan.emblemW() > 0.0F
                            ? HudEmblem.forHeight(Math.round(24.0F * scale))
                            : null;
            if (texture != null) {
                float h2 = Math.round(g2);
                float f3 = Math.round(g + (height2 - texture.height()) * 0.5F);
                imDrawList.addImage(
                        texture.id(),
                        h2,
                        f3,
                        h2 + texture.width(),
                        f3 + texture.height(),
                        0.0F,
                        0.0F,
                        1.0F,
                        1.0F,
                        HudChrome.scaleAlpha(this.hudAccent(), alpha2));
                float g3;
                HudChrome.divider(
                        imDrawList,
                        (g3 = g2 + infoPlan.emblemW()) + 10.0F * scale,
                        g + height2 * 0.5F,
                        12.0F * scale,
                        alpha2);
                g2 = g3 + (10.0F * scale * 2.0F + 1.0F);
            }
            List list = infoPlan.segments();
            for (int i = 0; i < list.size(); i++) {
                HudRenderer.Segment segment = (HudRenderer.Segment) list.get(i);
                HudRenderer.InfoItem infoItem = segment.item();
                float h3 = infoItem.unit() != null ? g2 + segment.slotW() - segment.contentW() : g2;
                HudChrome.tabular(
                        imDrawList,
                        type2.value(),
                        type2.valuePx(),
                        h3,
                        h,
                        HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, alpha2),
                        infoItem.value(),
                        needsTextGuard);
                h3 += segment.valueW();
                if (infoItem.dim() != null) {
                    HudChrome.tabular(
                            imDrawList,
                            type2.value(),
                            type2.valuePx(),
                            h3,
                            h,
                            HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, alpha2),
                            infoItem.dim(),
                            needsTextGuard);
                    h3 += segment.dimW();
                }
                if (infoItem.unit() != null) {
                    HudChrome.text(
                            imDrawList,
                            type2.label(),
                            type2.labelPx(),
                            h3 + 4.0F * scale,
                            f2,
                            HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, alpha2),
                            infoItem.unit(),
                            needsTextGuard);
                }
                g2 += segment.slotW();
                if (i < list.size() - 1) {
                    HudChrome.divider(
                            imDrawList,
                            g2 + 10.0F * scale,
                            g + height2 * 0.5F,
                            12.0F * scale,
                            alpha2);
                    g2 += 10.0F * scale * 2.0F + 1.0F;
                }
            }
        }
    }

    private static float rowsRadius(float f) {
        return Math.min(hudRounding(), 6.0F) * f;
    }

    private void drawRows(
            ImDrawList imDrawList,
            List<HudRenderer.Step> list,
            Surface surface,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            boolean bl) {
        if (bl) {
            drawRowsShadow(imDrawList, list, g2, h2);
        }
        int fill2 = GuiPalette.fill(surface);
        for (int n = 0; n < list.size(); n++) {
            HudRenderer.Step step = (HudRenderer.Step) list.get(n);
            HudRenderer.Step step2 = n > 0 ? (HudRenderer.Step) list.get(n - 1) : null;
            HudRenderer.Step step3 =
                    n < list.size() - 1 ? (HudRenderer.Step) list.get(n + 1) : null;
            int convexCorners = convexCorners(step, step2, step3);
            int scaleAlpha = HudChrome.scaleAlpha(fill2, step.alpha());
            if (convexCorners == 256) {
                imDrawList.addRectFilled(step.l(), step.t(), step.r(), step.b(), scaleAlpha);
            } else {
                imDrawList.addRectFilled(
                        step.l(), step.t(), step.r(), step.b(), scaleAlpha, g2, convexCorners);
            }
            SurfaceBackground.drawPart(
                    imDrawList,
                    surface,
                    f,
                    g,
                    h,
                    f2,
                    step.l(),
                    step.t(),
                    step.r() - step.l(),
                    step.b() - step.t(),
                    step.alpha(),
                    g2,
                    convexCorners);
            float f3 = step.b() - step.t();
            if (step2 != null && step2.l() < step.l() - 0.5F) {
                fillet(
                        imDrawList,
                        step.l(),
                        step.t(),
                        -1,
                        1,
                        Math.min(g2, Math.min(step.l() - step2.l(), f3)),
                        scaleAlpha);
            }
            if (step2 != null && step2.r() > step.r() + 0.5F) {
                fillet(
                        imDrawList,
                        step.r(),
                        step.t(),
                        1,
                        1,
                        Math.min(g2, Math.min(step2.r() - step.r(), f3)),
                        scaleAlpha);
            }
            if (step3 != null && step3.l() < step.l() - 0.5F) {
                fillet(
                        imDrawList,
                        step.l(),
                        step.b(),
                        -1,
                        -1,
                        Math.min(g2, Math.min(step.l() - step3.l(), f3)),
                        scaleAlpha);
            }
            if (step3 != null && step3.r() > step.r() + 0.5F) {
                fillet(
                        imDrawList,
                        step.r(),
                        step.b(),
                        1,
                        -1,
                        Math.min(g2, Math.min(step3.r() - step.r(), f3)),
                        scaleAlpha);
            }
        }
    }

    private static int convexCorners(
            HudRenderer.Step step, HudRenderer.Step step2, HudRenderer.Step step3) {
        int i = 0;
        if (step2 == null || step2.l() > step.l() + 0.5F) {
            i |= 16;
        }
        if (step2 == null || step2.r() < step.r() - 0.5F) {
            i |= 32;
        }
        if (step3 == null || step3.l() > step.l() + 0.5F) {
            i |= 64;
        }
        if (step3 == null || step3.r() < step.r() - 0.5F) {
            i |= 128;
        }
        return i == 0 ? 256 : i;
    }

    private static void fillet(
            ImDrawList imDrawList, float f, float g, int i, int j, float h, int k) {
        if (!(h < 1.0F)) {
            float f2 = f + i * h;
            float g2 = g + j * h;
            float h2 = i < 0 ? 0.0F : 3.1415927F;
            float f3 = j > 0 ? (i < 0 ? -1.5707964F : 4.712389F) : 1.5707964F;
            imDrawList.pathClear();
            imDrawList.pathLineTo(f, g);
            imDrawList.pathArcTo(f2, g2, h, h2, f3, 8);
            imDrawList.pathFillConcave(k);
        }
    }

    private static void drawRowsShadow(
            ImDrawList imDrawList, List<HudRenderer.Step> list, float f, float g) {
        for (int i = 3; i >= 1; i--) {
            float h = i * Math.max(1.0F, g);
            for (int j = 0; j < list.size(); j++) {
                HudRenderer.Step step = (HudRenderer.Step) list.get(j);
                boolean bl = j == 0;
                boolean bl2 = j == list.size() - 1;
                int convexCorners =
                        convexCorners(
                                step,
                                bl ? null : (HudRenderer.Step) list.get(j - 1),
                                bl2 ? null : (HudRenderer.Step) list.get(j + 1));
                float f2 = step.t() - (bl ? h : 0.0F);
                float g2 = step.b() + (bl2 ? h + Math.max(1.0F, g) : 0.0F);
                int scaleAlpha = HudChrome.scaleAlpha(218103808, step.alpha());
                if (convexCorners == 256) {
                    imDrawList.addRectFilled(step.l() - h, f2, step.r() + h, g2, scaleAlpha);
                } else {
                    imDrawList.addRectFilled(
                            step.l() - h, f2, step.r() + h, g2, scaleAlpha, f + h, convexCorners);
                }
            }
        }
    }

    private static boolean alignsRight(HudLayoutStore.HudElementState hudElementState, float f) {
        return switch (hudElementState.anchorX) {
            case 0 -> false;
            case 2 -> true;
            default -> hudElementState.x + hudElementState.width * 0.5F >= f * 0.5F;
        };
    }

    private List<HudRenderer.CoordRow> coordinateRows(MinecraftClient client, boolean bl, float f) {
        long l2 = 0L;
        long l3 = 0L;
        long l4 = 0L;
        if (client.player != null) {
            l2 = (long) Math.floor(client.player.getX());
            l3 = (long) Math.floor(client.player.getY());
            l4 = (long) Math.floor(client.player.getZ());
        }
        boolean bl2 = client.world != null && client.world.getRegistryKey() == World.NETHER;
        boolean bl3 = client.world != null && client.world.getRegistryKey() == World.END;
        boolean coordinates = this.hudModule.coordinates();
        boolean netherCoordinates = this.hudModule.netherCoordinates();
        float g = 1.0F - (float) Math.exp(-12.0F * f);
        ArrayList list = new ArrayList(2);
        if (bl3) {
            this.addCoordRow(
                    list,
                    "end",
                    "end_stone",
                    true,
                    coordinates || netherCoordinates,
                    bl,
                    new long[] {l2, l3, l4},
                    g);
        } else {
            long[] longs;
            if (bl2) {
                long[] longs2 = new long[] {l2 * 8L, l3, 0L};
                longs = longs2;
                longs2[2] = l4 * 8L;
            } else {
                long[] longs3 = new long[] {l2, l3, 0L};
                longs = longs3;
                longs3[2] = l4;
            }
            long[] longs4;
            if (bl2) {
                long[] longs5 = new long[] {l2, l3, 0L};
                longs4 = longs5;
                longs5[2] = l4;
            } else {
                long[] longs6 = new long[] {Math.floorDiv(l2, 8), l3, 0L};
                longs4 = longs6;
                longs6[2] = Math.floorDiv(l4, 8);
            }
            this.addCoordRow(
                    list, "overworld", "grass_block_side", !bl2, coordinates, bl, longs, g);
            this.addCoordRow(list, "nether", "netherrack", bl2, netherCoordinates, bl, longs4, g);
        }
        return list;
    }

    private void addCoordRow(
            List<HudRenderer.CoordRow> list,
            String string,
            String string2,
            boolean bl,
            boolean bl2,
            boolean bl3,
            long[] longs,
            float f) {
        float g = !bl2 && !bl3 ? 0.0F : 1.0F;
        float h = this.coordAnim.getOrDefault(string, g);
        if (Math.abs((h = h + (g - h) * f) - g) < 0.005F) {
            h = g;
        }
        this.coordAnim.put(string, h);
        if (!(h <= 0.005F)) {
            list.add(
                    new HudRenderer.CoordRow(
                            string2,
                            bl,
                            bl2,
                            new String[] {number(longs[0]), number(longs[1]), number(longs[2])},
                            h));
        }
    }

    private static float coordIconSize(float f) {
        return f >= 1.0F
                ? 16.0F * Math.max(1.0F, (float) Math.floor(f + 0.25F))
                : Math.round(16.0F * f);
    }

    private HudRenderer.CoordPlan planCoordinates(
            HudLayoutStore.HudElementState hudElementState,
            List<HudRenderer.CoordRow> list,
            boolean bl,
            float f) {
        float scale = scaleOf(hudElementState);
        HudRenderer.Type type2 = HudRenderer.Type.of(scale);
        float g = 20.0F * scale;
        float[] floats = new float[3];
        float h = 0.0F;
        for (HudRenderer.CoordRow coordRow : list) {
            for (int i = 0; i < 3; i++) {
                floats[i] =
                        Math.max(
                                floats[i],
                                HudChrome.tabularWidth(
                                        type2.text(), type2.textPx(), coordRow.values()[i]));
            }
            h += g * smooth(coordRow.anim());
        }
        float[] floats2 = new float[3];
        float f2 =
                7.0F * scale * 2.0F
                        + Math.min(coordIconSize(scale), g - 2.0F)
                        + 8.0F * scale
                        + 12.0F * scale * 2.0F;
        for (int j = 0; j < 3; j++) {
            floats2[j] = this.slot(COORD_SLOTS[j], type2.textPx(), (float) Math.ceil(floats[j]), f);
            f2 += floats2[j];
        }
        if (bl) {
            h = Math.max(h, g);
        }
        return new HudRenderer.CoordPlan(
                list, type2, floats2, (float) Math.round(f2), (float) Math.max(1, Math.round(h)));
    }

    private void drawCoordinates(
            ImDrawList imDrawList,
            HudLayoutStore.HudElementState hudElementState,
            HudRenderer.CoordPlan coordPlan,
            boolean bl,
            HudRenderer.DrawTransform drawTransform,
            float f) {
        float alpha2 = drawTransform.alpha();
        if (!(alpha2 <= 0.001F) && !coordPlan.rows().isEmpty()) {
            float scale = scaleOf(hudElementState);
            float g = Math.round(hudElementState.x + drawTransform.xOffset());
            float h = Math.round(hudElementState.y + drawTransform.yOffset());
            float width2 = hudElementState.width;
            float height2 = hudElementState.height;
            HudRenderer.Type type2 = coordPlan.type();
            boolean alignsRight = alignsRight(hudElementState, f);
            float f2 = 20.0F * scale;
            ArrayList list = new ArrayList(coordPlan.rows().size());
            float g2 = h;
            for (int i = 0; i < coordPlan.rows().size(); i++) {
                HudRenderer.CoordRow row2 = coordPlan.rows().get(i);
                float h2 = smooth(row2.anim());
                float f3 = f2 * h2;
                float g3 = Math.round(g2);
                float h3 = Math.round(g2 + f3);
                g2 += f3;
                if (!(h3 <= g3)) {
                    float f4 = Math.round(width2 * (1.0F - h2));
                    float g4 = alignsRight ? g + f4 : g - f4;
                    float h4 =
                            alpha2
                                    * (row2.enabled() ? 1.0F : 0.35F)
                                    * Math.min(1.0F, row2.anim() * 1.5F);
                    list.add(new HudRenderer.Step(i, g4, g3, g4 + width2, h3, h4));
                }
            }
            this.drawRows(
                    imDrawList,
                    list,
                    hudSurface(hudElementState),
                    g,
                    h,
                    width2,
                    height2,
                    rowsRadius(scale),
                    scale,
                    bl);
            float f5 = Math.min(coordIconSize(scale), f2 - 2.0F);
            float g5 = 2.5F * scale;
            float h5 = (float) Math.floor((f2 - type2.textPx()) * 0.5F);
            for (HudRenderer.Step step : (Iterable<HudRenderer.Step>) list) {
                HudRenderer.CoordRow row3 = coordPlan.rows().get(step.item());
                float f6 = step.b() - step.t();
                float g6 = (f2 - f6) * 0.5F;
                float h6 = step.alpha() * row3.anim();
                boolean bl2 = f6 < f2 - 0.5F;
                if (bl2) {
                    imDrawList.pushClipRect(step.l(), step.t(), step.r(), step.b(), true);
                }
                float f7 = Math.round(step.l() + 7.0F * scale);
                float g7 = Math.round(step.t() + (f2 - f5) * 0.5F - g6);
                ThemeImages.Texture texture = GameIcons.block(row3.block());
                if (texture != null) {
                    imDrawList.addImageRounded(
                            texture.id(),
                            f7,
                            g7,
                            f7 + f5,
                            g7 + f5,
                            0.0F,
                            0.0F,
                            1.0F,
                            1.0F,
                            HudChrome.scaleAlpha(-1, h6),
                            g5,
                            240);
                }
                float h7 = f7 + f5 + 8.0F * scale;
                float f8 = Math.round(step.t() + h5 - g6);
                int scaleAlpha = HudChrome.scaleAlpha(-1, h6 * (row3.here() ? 1.0F : 0.62F));
                for (int k = 0; k < 3; k++) {
                    HudChrome.tabular(
                            imDrawList,
                            type2.text(),
                            type2.textPx(),
                            h7,
                            f8,
                            scaleAlpha,
                            row3.values()[k],
                            true);
                    h7 += coordPlan.slotW()[k] + 12.0F * scale;
                }
                if (bl2) {
                    imDrawList.popClipRect();
                }
            }
        }
    }

    private static float rowWidth(float f, float g) {
        return Math.round(12.0F * g + f);
    }

    private HudRenderer.ListPlan planModuleList(
            HudLayoutStore.HudElementState hudElementState, boolean bl, float f) {
        float scale = scaleOf(hudElementState);
        HudRenderer.Type type2 = HudRenderer.Type.of(scale);
        float g = 18.0F * scale;
        List list =
                this.moduleRows.ordered(
                        type2.text(),
                        type2.textPx(),
                        HudChrome::width,
                        hudElementState.anchorBottom);
        float h = 0.0F;
        float f2 = 0.0F;
        for (int i = 0; i < list.size(); i++) {
            ModuleListRows.Row row2 = (ModuleListRows.Row) list.get(i);
            h = Math.max(h, row2.nameW());
            f2 += g * smooth(row2.anim());
        }
        if (list.isEmpty()) {
            this.moduleListW =
                    bl
                            ? rowWidth(
                                    HudChrome.width(
                                            type2.label(), type2.labelPx(), "No active modules"),
                                    scale)
                            : 0.0F;
            return new HudRenderer.ListPlan(
                    list,
                    type2,
                    (float) Math.max(1, Math.round(this.moduleListW)),
                    (float) Math.round(g));
        } else {
            if (bl) {
                f2 = Math.max(f2, g);
            }
            float rowWidth;
            this.moduleListW =
                    !((rowWidth = rowWidth(h, scale)) >= this.moduleListW)
                                    && !(Math.abs(rowWidth - this.moduleListW) < 0.5F)
                            ? (this.moduleListW =
                                    this.moduleListW
                                            + (rowWidth - this.moduleListW)
                                                    * (1.0F - (float) Math.exp(-16.0F * f)))
                            : rowWidth;
            return new HudRenderer.ListPlan(
                    list,
                    type2,
                    (float) Math.round(this.moduleListW),
                    (float) Math.max(1, Math.round(f2)));
        }
    }

    private void drawModuleList(
            ImDrawList imDrawList,
            HudLayoutStore.HudElementState hudElementState,
            HudRenderer.ListPlan listPlan,
            boolean bl,
            HudRenderer.DrawTransform drawTransform,
            float f) {
        float alpha2 = drawTransform.alpha();
        if (!(alpha2 <= 0.001F)) {
            float scale = scaleOf(hudElementState);
            float g = Math.round(hudElementState.x + drawTransform.xOffset());
            float h = Math.round(hudElementState.y + drawTransform.yOffset());
            float width2 = hudElementState.width;
            float height2 = hudElementState.height;
            HudRenderer.Type type2 = listPlan.type();
            boolean alignsRight = alignsRight(hudElementState, f);
            float f2 = 18.0F * scale;
            ArrayList list = new ArrayList(Math.max(1, listPlan.rows().size()));
            if (listPlan.rows().isEmpty()) {
                float rowWidth =
                        rowWidth(
                                HudChrome.width(
                                        type2.label(), type2.labelPx(), "No active modules"),
                                scale);
                float g2 = alignsRight ? g + width2 - rowWidth : g;
                list.add(
                        new HudRenderer.Step(
                                -1, g2, h, g2 + rowWidth, (float) Math.round(h + f2), alpha2));
            } else {
                float h2 = h;
                for (int i = 0; i < listPlan.rows().size(); i++) {
                    ModuleListRows.Row row2 = listPlan.rows().get(i);
                    float f3 = smooth(row2.anim());
                    float g3 = f2 * f3;
                    float h3 = Math.round(h2);
                    float f4 = Math.round(h2 + g3);
                    h2 += g3;
                    if (!(f4 <= h3)) {
                        float rowWidth2 = rowWidth(row2.nameW(), scale);
                        float g4 = Math.round(rowWidth2 * (1.0F - f3));
                        float h4 = alignsRight ? g + width2 - rowWidth2 + g4 : g - g4;
                        list.add(
                                new HudRenderer.Step(
                                        i,
                                        h4,
                                        h3,
                                        h4 + rowWidth2,
                                        f4,
                                        alpha2 * Math.min(1.0F, row2.anim() * 1.5F)));
                    }
                }
            }
            this.drawRows(
                    imDrawList,
                    list,
                    hudSurface(hudElementState),
                    g,
                    h,
                    width2,
                    height2,
                    rowsRadius(scale),
                    scale,
                    bl);
            float f5 = (float) Math.floor((f2 - type2.textPx()) * 0.5F);
            float g5 = 6.0F * scale;
            for (HudRenderer.Step step : (Iterable<HudRenderer.Step>) list) {
                if (step.item() < 0) {
                    float width3 =
                            HudChrome.width(type2.label(), type2.labelPx(), "No active modules");
                    float h5 = alignsRight ? step.r() - g5 - width3 : step.l() + g5;
                    HudChrome.text(
                            imDrawList,
                            type2.label(),
                            type2.labelPx(),
                            h5,
                            step.t() + (float) Math.floor((f2 - type2.labelPx()) * 0.5F),
                            HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, alpha2),
                            "No active modules",
                            true);
                } else {
                    ModuleListRows.Row row3 = listPlan.rows().get(step.item());
                    float f6 = step.b() - step.t();
                    float g6 = alignsRight ? step.r() - g5 - row3.nameW() : step.l() + g5;
                    float h6 = step.t() + f5 - (f2 - f6) * 0.5F;
                    boolean bl2 = f6 < f2 - 0.5F;
                    if (bl2) {
                        imDrawList.pushClipRect(step.l(), step.t(), step.r(), step.b(), true);
                    }
                    int j = listPlan.rows().size();
                    float f7 = 1.0F - 0.6F * (j <= 1 ? 0.0F : (float) step.item() / (j - 1));
                    float g7 = Math.max(1.0F, (float) Math.round(2.0F * scale));
                    float h7 = alignsRight ? step.r() - g7 : step.l();
                    imDrawList.addRectFilled(
                            h7,
                            step.t() + 2.0F * scale,
                            h7 + g7,
                            step.b() - 2.0F * scale,
                            HudChrome.scaleAlpha(this.hudAccent(), alpha2 * row3.anim() * f7),
                            g7 * 0.5F);
                    HudChrome.text(
                            imDrawList,
                            type2.text(),
                            type2.textPx(),
                            Math.round(g6),
                            Math.round(h6),
                            HudChrome.scaleAlpha(-1, alpha2 * row3.anim() * row3.anim()),
                            row3.module().name(),
                            true);
                    if (bl2) {
                        imDrawList.popClipRect();
                    }
                }
            }
        }
    }

    private float updateToastShift(
            MinecraftClient client,
            HudLayoutStore.HudElementState hudElementState,
            boolean bl,
            float f) {
        float g = !bl && this.draggingId == null ? toastClearance(client, hudElementState) : 0.0F;
        this.toastShift =
                !bl && !(Math.abs(g - this.toastShift) < 0.5F)
                        ? (this.toastShift =
                                this.toastShift
                                        + (g - this.toastShift)
                                                * (1.0F - (float) Math.exp(-12.0F * f)))
                        : g;
        return this.toastShift;
    }

    private static float toastClearance(
            MinecraftClient client, HudLayoutStore.HudElementState hudElementState) {
        if (hudElementState.anchorX == 2
                && !hudElementState.anchorBottom
                && !client.options.hudHidden) {
            Window window = client.getWindow();
            int scaleFactor = window.getScaleFactor();
            int scaledWidth = window.getScaledWidth();
            float f = 0.0F;
            for (Object efn$hudVisibleToast :
                    ((AccessorHudToastManager) client.getToastManager()).efn$hudVisibleToasts()) {
                AccessorHudToastInstance accessorHudToastInstance =
                        (AccessorHudToastInstance) efn$hudVisibleToast;
                Toast toast = accessorHudToastInstance.efn$hudToast();
                float g =
                        accessorHudToastInstance.efn$hudVisibility() == Visibility.HIDE
                                ? accessorHudToastInstance.efn$hudVisiblePortion()
                                : 1.0F;
                float h = toast.getXPos(scaledWidth, g) * scaleFactor;
                if (!(h >= hudElementState.x + hudElementState.width)
                        && !(h + toast.getWidth() * scaleFactor <= hudElementState.x)) {
                    f =
                            Math.max(
                                    f,
                                    (toast.getYPos(accessorHudToastInstance.efn$hudFirstSlot())
                                                    + toast.getHeight())
                                            * scaleFactor);
                }
            }
            return f <= 0.0F
                    ? 0.0F
                    : MathHelper.clamp(
                            f + 10.0F * ImGuiManager.getInstance().uiScale() - hudElementState.y,
                            0.0F,
                            f);
        } else {
            return 0.0F;
        }
    }

    private static float iconSize(float f) {
        return f >= 1.0F
                ? 18.0F * Math.max(1.0F, (float) Math.floor(f + 0.25F))
                : Math.round(18.0F * f);
    }

    private HudRenderer.PotionPlan planPotions(
            HudLayoutStore.HudElementState hudElementState,
            List<HudRenderer.PotionRow> list,
            boolean bl,
            float f) {
        float scale = scaleOf(hudElementState);
        HudRenderer.Type type2 = HudRenderer.Type.of(scale);
        float g = 22.0F * scale;
        ArrayList list2 = new ArrayList(list);
        list2.sort(
                Comparator.comparingInt(HudRenderer.PotionRow::ticks)
                        .thenComparing(HudRenderer.PotionRow::name));
        if (hudElementState.anchorBottom) {
            Collections.reverse(list2);
        }
        ArrayList list3 = new ArrayList(list2.size());
        float h = 0.0F;
        float f2 =
                6.0F * scale * 2.0F
                        + Math.min(iconSize(scale), g - 2.0F)
                        + 6.0F * scale
                        + 14.0F * scale;
        for (HudRenderer.PotionRow potionRow : (Iterable<HudRenderer.PotionRow>) list2) {
            float width2 = HudChrome.width(type2.text(), type2.textPx(), potionRow.name());
            float g2 =
                    potionRow.amplifier().isEmpty()
                            ? 0.0F
                            : HudChrome.width(type2.text(), type2.textPx(), potionRow.amplifier());
            float h2 =
                    potionRow.infinite()
                            ? HudChrome.width(type2.text(), type2.textPx(), "\u221e")
                            : HudChrome.tabularWidth(
                                    type2.text(), type2.textPx(), potionRow.duration());
            list3.add(new HudRenderer.PotionLine(potionRow, width2, g2, h2));
            h = Math.max(h, f2 + width2 + (g2 > 0.0F ? 4.0F * scale + g2 : 0.0F) + h2);
        }
        if (list3.isEmpty()) {
            this.removeSlot("potions", type2.textPx());
            float f3 =
                    bl
                            ? 6.0F * scale * 2.0F
                                    + HudChrome.width(
                                            type2.label(), type2.labelPx(), "No active effects")
                            : 1.0F;
            return new HudRenderer.PotionPlan(
                    list3, type2, (float) Math.round(f3), (float) Math.round(g));
        } else {
            float slot2 = this.slot("potions", type2.textPx(), (float) Math.ceil(h), f);
            return new HudRenderer.PotionPlan(
                    list3, type2, (float) Math.round(slot2), (float) Math.round(g * list3.size()));
        }
    }

    private void drawPotions(
            ImDrawList imDrawList,
            HudLayoutStore.HudElementState hudElementState,
            HudRenderer.PotionPlan potionPlan,
            boolean bl,
            HudRenderer.DrawTransform drawTransform) {
        float alpha2 = drawTransform.alpha();
        if (!(alpha2 <= 0.001F)) {
            float scale = scaleOf(hudElementState);
            float f = Math.round(hudElementState.x + drawTransform.xOffset());
            float g = Math.round(hudElementState.y + drawTransform.yOffset());
            float width2 = hudElementState.width;
            float height2 = hudElementState.height;
            HudRenderer.Type type2 = potionPlan.type();
            float h = 22.0F * scale;
            float f2 = 6.0F * scale;
            int i = Math.max(1, potionPlan.lines().size());
            ArrayList list = new ArrayList(i);
            for (int j = 0; j < i; j++) {
                list.add(
                        new HudRenderer.Step(
                                potionPlan.lines().isEmpty() ? -1 : j,
                                f,
                                (float) Math.round(g + h * j),
                                f + width2,
                                (float) Math.round(g + h * (j + 1)),
                                alpha2));
            }
            this.drawRows(
                    imDrawList,
                    list,
                    hudSurface(hudElementState),
                    f,
                    g,
                    width2,
                    height2,
                    rowsRadius(scale),
                    scale,
                    bl);
            if (potionPlan.lines().isEmpty()) {
                HudChrome.text(
                        imDrawList,
                        type2.label(),
                        type2.labelPx(),
                        f + f2,
                        g + (float) Math.floor((h - type2.labelPx()) * 0.5F),
                        HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, alpha2),
                        "No active effects",
                        true);
            } else {
                float g2 = Math.min(iconSize(scale), h - 2.0F);
                float h2 = (float) Math.floor((h - type2.textPx()) * 0.5F);
                for (HudRenderer.Step step : (Iterable<HudRenderer.Step>) list) {
                    HudRenderer.PotionLine line = potionPlan.lines().get(step.item());
                    HudRenderer.PotionRow potionRow = line.row();
                    float f3 = Math.round(step.l() + f2);
                    float g3 = Math.round(step.t() + (h - g2) * 0.5F);
                    float h3 = alpha2;
                    if (!potionRow.infinite() && potionRow.ticks() <= 200) {
                        h3 =
                                alpha2
                                        * (0.55F
                                                + 0.45F
                                                        * Math.abs(
                                                                (float)
                                                                        Math.cos(
                                                                                potionRow.ticks()
                                                                                        * 3.141592653589793
                                                                                        / 10.0)));
                    }
                    ThemeImages.Texture texture;
                    if ((texture = GameIcons.effect(potionRow.effect())) != null) {
                        imDrawList.addImage(
                                texture.id(),
                                f3,
                                g3,
                                f3 + g2,
                                g3 + g2,
                                0.0F,
                                0.0F,
                                1.0F,
                                1.0F,
                                HudChrome.scaleAlpha(-1, h3));
                    } else {
                        int abgr =
                                GuiPalette.abgr(0xFF000000 | potionRow.effect().value().getColor());
                        imDrawList.addCircleFilled(
                                f3 + g2 * 0.5F,
                                g3 + g2 * 0.5F,
                                g2 * 0.3F,
                                HudChrome.scaleAlpha(abgr, h3),
                                16);
                    }
                    float f4 = step.t() + h2;
                    float g4 = f3 + g2 + 6.0F * scale;
                    HudChrome.text(
                            imDrawList,
                            type2.text(),
                            type2.textPx(),
                            g4,
                            f4,
                            HudChrome.scaleAlpha(-1, alpha2),
                            potionRow.name(),
                            true);
                    if (line.ampW() > 0.0F) {
                        HudChrome.text(
                                imDrawList,
                                type2.text(),
                                type2.textPx(),
                                g4 + line.nameW() + 4.0F * scale,
                                f4,
                                HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, alpha2),
                                potionRow.amplifier(),
                                true);
                    }
                    float h4 = step.r() - f2 - line.durationW();
                    if (potionRow.infinite()) {
                        HudChrome.text(
                                imDrawList,
                                type2.text(),
                                type2.textPx(),
                                h4,
                                f4,
                                HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, alpha2),
                                "\u221e",
                                true);
                    } else {
                        int k = potionRow.expiringSoon() ? HudChrome.WARN : HudChrome.TEXT_LABEL;
                        HudChrome.tabular(
                                imDrawList,
                                type2.text(),
                                type2.textPx(),
                                h4,
                                f4,
                                HudChrome.scaleAlpha(k, alpha2),
                                potionRow.duration(),
                                true);
                    }
                }
            }
        }
    }

    private HudRenderer.VanishPlan planVanish(
            HudLayoutStore.HudElementState hudElementState, boolean bl, float f) {
        float scale = scaleOf(hudElementState);
        HudRenderer.Type type2 = HudRenderer.Type.of(scale);
        float g = 22.0F * scale;
        ArrayList list = new ArrayList();
        for (AntiVanishModule.HudEntry hudEntry : AntiVanishModule.hudEntries()) {
            String hudTag = AntiVanishModule.hudTag(hudEntry);
            String hudValue = AntiVanishModule.hudValue(hudEntry);
            boolean bl2 = "ALERT".equals(hudTag) || "VANISH".equals(hudTag);
            list.add(
                    new HudRenderer.VanishLine(
                            hudTag,
                            hudValue,
                            bl2,
                            HudChrome.width(type2.label(), type2.labelPx(), hudTag),
                            HudChrome.width(type2.text(), type2.textPx(), hudValue)));
        }
        if (hudElementState.anchorBottom) {
            Collections.reverse(list);
        }
        if (list.isEmpty()) {
            this.removeSlot("vanish", type2.textPx());
            float h =
                    bl
                            ? 8.0F * scale * 2.0F
                                    + HudChrome.width(
                                            type2.label(), type2.labelPx(), "No vanish alerts")
                            : 1.0F;
            return new HudRenderer.VanishPlan(
                    list, type2, 0.0F, (float) Math.round(h), (float) Math.round(g));
        } else {
            float f2 = 0.0F;
            float g2 = 0.0F;
            for (HudRenderer.VanishLine vanishLine : (Iterable<HudRenderer.VanishLine>) list) {
                f2 = Math.max(f2, vanishLine.tagW());
                g2 = Math.max(g2, vanishLine.valueW());
            }
            float h2 = 8.0F * scale * 2.0F + f2 + 8.0F * scale + g2;
            float slot2 = this.slot("vanish", type2.textPx(), (float) Math.ceil(h2), f);
            return new HudRenderer.VanishPlan(
                    list,
                    type2,
                    f2,
                    (float) Math.round(slot2),
                    (float) Math.round(g * list.size()));
        }
    }

    private void drawVanish(
            ImDrawList imDrawList,
            HudLayoutStore.HudElementState hudElementState,
            HudRenderer.VanishPlan vanishPlan,
            boolean bl,
            HudRenderer.DrawTransform drawTransform) {
        float alpha2 = drawTransform.alpha();
        if (!(alpha2 <= 0.001F)) {
            float scale = scaleOf(hudElementState);
            float f = Math.round(hudElementState.x + drawTransform.xOffset());
            float g = Math.round(hudElementState.y + drawTransform.yOffset());
            float width2 = hudElementState.width;
            float height2 = hudElementState.height;
            HudRenderer.Type type2 = vanishPlan.type();
            float h = 22.0F * scale;
            float f2 = 8.0F * scale;
            int i = Math.max(1, vanishPlan.lines().size());
            ArrayList list = new ArrayList(i);
            for (int j = 0; j < i; j++) {
                list.add(
                        new HudRenderer.Step(
                                vanishPlan.lines().isEmpty() ? -1 : j,
                                f,
                                (float) Math.round(g + h * j),
                                f + width2,
                                (float) Math.round(g + h * (j + 1)),
                                alpha2));
            }
            this.drawRows(
                    imDrawList,
                    list,
                    hudSurface(hudElementState),
                    f,
                    g,
                    width2,
                    height2,
                    rowsRadius(scale),
                    scale,
                    bl);
            if (vanishPlan.lines().isEmpty()) {
                HudChrome.text(
                        imDrawList,
                        type2.label(),
                        type2.labelPx(),
                        f + f2,
                        g + (float) Math.floor((h - type2.labelPx()) * 0.5F),
                        HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, alpha2),
                        "No vanish alerts",
                        true);
            } else {
                float g2 = (float) Math.floor((h - type2.textPx()) * 0.5F);
                for (HudRenderer.Step step : (Iterable<HudRenderer.Step>) list) {
                    HudRenderer.VanishLine line = vanishPlan.lines().get(step.item());
                    float h2 = step.t() + g2;
                    float f3 =
                            h2
                                    + HudChrome.baselineOffset(
                                            type2.text(),
                                            type2.textPx(),
                                            type2.label(),
                                            type2.labelPx());
                    float g3 = step.l() + f2;
                    HudChrome.text(
                            imDrawList,
                            type2.label(),
                            type2.labelPx(),
                            g3,
                            f3,
                            HudChrome.scaleAlpha(
                                    line.urgent() ? HudChrome.WARN : HudChrome.TEXT_LABEL, alpha2),
                            line.tag(),
                            true);
                    HudChrome.text(
                            imDrawList,
                            type2.text(),
                            type2.textPx(),
                            g3 + vanishPlan.tagColumn() + 8.0F * scale,
                            h2,
                            HudChrome.scaleAlpha(-1, alpha2),
                            line.value(),
                            true);
                }
            }
        }
    }

    private HudRenderer.StaffPlan planStaff(
            HudLayoutStore.HudElementState hudElementState, boolean bl, float f) {
        float scale = scaleOf(hudElementState);
        HudRenderer.Type type2 = HudRenderer.Type.of(scale);
        float g = 22.0F * scale;
        StaffListModule.HudList hudList = StaffListModule.hudRows();
        ArrayList list = new ArrayList(hudList.rows().size() + 1);
        for (StaffListModule.HudRow row2 : hudList.rows()) {
            int staffFace = this.staffFace(row2.id(), row2.nameHidden());
            String string =
                    row2.presence() == StaffListModule.Presence.VANISHED
                            ? "VANISHED"
                            : (row2.presence() == StaffListModule.Presence.SPECTATOR
                                    ? "SPECTATOR"
                                    : "");
            float h =
                    string.isEmpty()
                            ? 0.0F
                            : HudChrome.width(type2.label(), type2.labelPx(), string);
            if (row2.watching()) {
                String string2 = row2.nearby() ? row2.distanceLabel() : row2.watchingFor();
                float tabularWidth = HudChrome.tabularWidth(type2.text(), type2.textPx(), string2);
                float width2 = HudChrome.width(type2.label(), type2.labelPx(), "WATCHING YOU");
                float f2 =
                        HudChrome.width(type2.text(), type2.textPx(), row2.name())
                                + (string.isEmpty() ? 0.0F : 8.0F * scale + h);
                list.add(
                        new HudRenderer.StaffLine(
                                row2.name(),
                                staffFace,
                                "WATCHING YOU",
                                false,
                                f2,
                                width2 + 4.0F * scale + tabularWidth,
                                true,
                                row2.nearby(),
                                row2.alertLevel(),
                                string,
                                h,
                                string2,
                                tabularWidth,
                                width2));
            } else {
                list.add(
                        HudRenderer.StaffLine.plain(
                                row2.name(),
                                staffFace,
                                string,
                                false,
                                HudChrome.width(type2.text(), type2.textPx(), row2.name()),
                                h));
            }
        }
        if (hudList.more() > 0) {
            String string3 = "+" + hudList.more() + " more";
            list.add(
                    HudRenderer.StaffLine.plain(
                            string3,
                            0,
                            "",
                            true,
                            HudChrome.width(type2.label(), type2.labelPx(), string3),
                            0.0F));
        }
        if (hudElementState.anchorBottom) {
            Collections.reverse(list);
        }
        if (list.isEmpty()) {
            this.removeSlot("staff", type2.textPx());
            boolean bl2 = bl || StaffListModule.shouldShowHud();
            float g2 =
                    bl2
                            ? 8.0F * scale * 2.0F
                                    + HudChrome.width(
                                            type2.label(), type2.labelPx(), "No staff online")
                            : 1.0F;
            return new HudRenderer.StaffPlan(
                    list, type2, "", (float) Math.round(g2), (float) Math.round(g));
        } else {
            String string4 = Integer.toString(hudList.rows().size() + hudList.more());
            float h2 =
                    HudChrome.width(type2.label(), type2.labelPx(), "STAFF")
                            + 5.0F * scale
                            + HudChrome.tabularWidth(type2.text(), type2.textPx(), string4);
            float f3 = 0.0F;
            float g3 = 0.0F;
            for (HudRenderer.StaffLine staffLine : (Iterable<HudRenderer.StaffLine>) list) {
                f3 = Math.max(f3, staffLine.nameW());
                g3 = Math.max(g3, staffLine.stateW());
            }
            float h3 =
                    faceSize(scale) + 6.0F * scale + f3 + (g3 > 0.0F ? 14.0F * scale + g3 : 0.0F);
            float f4 = 8.0F * scale * 2.0F + Math.max(h2, h3);
            float slot2 = this.slot("staff", type2.textPx(), (float) Math.ceil(f4), f);
            return new HudRenderer.StaffPlan(
                    list,
                    type2,
                    string4,
                    (float) Math.round(slot2),
                    (float) Math.round(20.0F * scale + g * list.size()));
        }
    }

    private void drawStaff(
            ImDrawList imDrawList,
            HudLayoutStore.HudElementState hudElementState,
            HudRenderer.StaffPlan staffPlan,
            boolean bl,
            HudRenderer.DrawTransform drawTransform) {
        float alpha2 = drawTransform.alpha();
        if (!(alpha2 <= 0.001F)) {
            float scale = scaleOf(hudElementState);
            float f = Math.round(hudElementState.x + drawTransform.xOffset());
            float g = Math.round(hudElementState.y + drawTransform.yOffset());
            float width2 = hudElementState.width;
            float height2 = hudElementState.height;
            HudRenderer.Type type2 = staffPlan.type();
            float h = 22.0F * scale;
            float f2 = 8.0F * scale;
            float g2 = (float) Math.floor((h - type2.labelPx()) * 0.5F);
            if (staffPlan.lines().isEmpty()) {
                List list =
                        List.of(
                                new HudRenderer.Step(
                                        -1, f, g, f + width2, (float) Math.round(g + h), alpha2));
                this.drawRows(
                        imDrawList,
                        list,
                        hudSurface(hudElementState),
                        f,
                        g,
                        width2,
                        height2,
                        rowsRadius(scale),
                        scale,
                        bl);
                HudChrome.text(
                        imDrawList,
                        type2.label(),
                        type2.labelPx(),
                        f + f2,
                        g + g2,
                        HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, alpha2),
                        "No staff online",
                        true);
            } else {
                float h2 = 20.0F * scale;
                ArrayList list2 = new ArrayList(staffPlan.lines().size() + 1);
                list2.add(
                        new HudRenderer.Step(
                                -1, f, g, f + width2, (float) Math.round(g + h2), alpha2));
                for (int i = 0; i < staffPlan.lines().size(); i++) {
                    list2.add(
                            new HudRenderer.Step(
                                    i,
                                    f,
                                    (float) Math.round(g + h2 + h * i),
                                    f + width2,
                                    (float) Math.round(g + h2 + h * (i + 1)),
                                    alpha2));
                }
                this.drawRows(
                        imDrawList,
                        list2,
                        hudSurface(hudElementState),
                        f,
                        g,
                        width2,
                        height2,
                        rowsRadius(scale),
                        scale,
                        bl);
                for (int j = 1; j < list2.size(); j++) {
                    HudRenderer.Step step = (HudRenderer.Step) list2.get(j);
                    HudRenderer.StaffLine line = staffPlan.lines().get(step.item());
                    if (line.server()) {
                        int convexCorners =
                                convexCorners(
                                        step,
                                        (HudRenderer.Step) list2.get(j - 1),
                                        j < list2.size() - 1
                                                ? (HudRenderer.Step) list2.get(j + 1)
                                                : null);
                        float f3 = 0.14F;
                        if (line.alert() > 0.0F) {
                            float g3 = (1.0F - line.alert()) * 4000.0F;
                            float h3 =
                                    0.5F
                                            + 0.5F
                                                    * (float)
                                                            Math.cos(
                                                                    g3
                                                                            * 2.0
                                                                            * 3.141592653589793
                                                                            / 500.0);
                            f3 += 0.24F * line.alert() * h3;
                        }
                        int scaleAlpha = HudChrome.scaleAlpha(HudChrome.WARN, alpha2 * f3);
                        if (convexCorners == 256) {
                            imDrawList.addRectFilled(
                                    step.l(), step.t(), step.r(), step.b(), scaleAlpha);
                        } else {
                            imDrawList.addRectFilled(
                                    step.l(),
                                    step.t(),
                                    step.r(),
                                    step.b(),
                                    scaleAlpha,
                                    rowsRadius(scale),
                                    convexCorners);
                        }
                        imDrawList.addRectFilled(
                                step.l() + 3.0F * scale,
                                step.t() + 4.0F * scale,
                                step.l() + 5.0F * scale,
                                step.b() - 4.0F * scale,
                                HudChrome.scaleAlpha(HudChrome.WARN, alpha2),
                                1.0F * scale);
                    }
                }
                HudRenderer.Step step2 = (HudRenderer.Step) list2.get(0);
                float f4 =
                        step2.t()
                                + (float)
                                        Math.floor((step2.b() - step2.t() - type2.textPx()) * 0.5F);
                HudChrome.text(
                        imDrawList,
                        type2.label(),
                        type2.labelPx(),
                        f + f2,
                        f4
                                + HudChrome.baselineOffset(
                                        type2.text(),
                                        type2.textPx(),
                                        type2.label(),
                                        type2.labelPx()),
                        HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, alpha2),
                        "STAFF",
                        true);
                HudChrome.tabular(
                        imDrawList,
                        type2.text(),
                        type2.textPx(),
                        f
                                + f2
                                + HudChrome.width(type2.label(), type2.labelPx(), "STAFF")
                                + 5.0F * scale,
                        f4,
                        HudChrome.scaleAlpha(-1, alpha2),
                        staffPlan.count(),
                        true);
                float faceSize = faceSize(scale);
                float g4 = (float) Math.floor((h - type2.textPx()) * 0.5F);
                float h4 = f2 + faceSize + 6.0F * scale;
                for (int n = 1; n < list2.size(); n++) {
                    HudRenderer.Step step3 = (HudRenderer.Step) list2.get(n);
                    HudRenderer.StaffLine line2 = staffPlan.lines().get(step3.item());
                    if (line2.more()) {
                        HudChrome.text(
                                imDrawList,
                                type2.label(),
                                type2.labelPx(),
                                step3.l() + h4,
                                step3.t() + g2,
                                HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, alpha2),
                                line2.name(),
                                true);
                    } else {
                        if (line2.face() != 0) {
                            drawFace(
                                    imDrawList,
                                    line2.face(),
                                    Math.round(step3.l() + f2),
                                    Math.round(
                                            step3.t() + (step3.b() - step3.t() - faceSize) * 0.5F),
                                    faceSize,
                                    scale,
                                    alpha2);
                        }
                        float f5 = step3.t() + g4;
                        float g5 =
                                f5
                                        + HudChrome.baselineOffset(
                                                type2.text(),
                                                type2.textPx(),
                                                type2.label(),
                                                type2.labelPx());
                        HudChrome.text(
                                imDrawList,
                                type2.text(),
                                type2.textPx(),
                                step3.l() + h4,
                                f5,
                                HudChrome.scaleAlpha(-1, alpha2),
                                line2.name(),
                                true);
                        if (line2.server()) {
                            if (!line2.note().isEmpty()) {
                                HudChrome.text(
                                        imDrawList,
                                        type2.label(),
                                        type2.labelPx(),
                                        step3.l() + h4 + line2.nameW() - line2.noteW(),
                                        g5,
                                        HudChrome.scaleAlpha(HudChrome.WARN, alpha2),
                                        line2.note(),
                                        true);
                            }
                            float h5 = step3.r() - f2 - line2.stateW();
                            HudChrome.text(
                                    imDrawList,
                                    type2.label(),
                                    type2.labelPx(),
                                    h5,
                                    g5,
                                    HudChrome.scaleAlpha(HudChrome.WARN, alpha2),
                                    line2.state(),
                                    true);
                            HudChrome.tabular(
                                    imDrawList,
                                    type2.text(),
                                    type2.textPx(),
                                    h5 + line2.serverTagW() + 4.0F * scale,
                                    f5,
                                    HudChrome.scaleAlpha(
                                            line2.nearby() ? -1 : HudChrome.TEXT_LABEL, alpha2),
                                    line2.value(),
                                    true);
                        } else if (!line2.state().isEmpty()) {
                            HudChrome.text(
                                    imDrawList,
                                    type2.label(),
                                    type2.labelPx(),
                                    step3.r() - f2 - line2.stateW(),
                                    g5,
                                    HudChrome.scaleAlpha(HudChrome.WARN, alpha2),
                                    line2.state(),
                                    true);
                        }
                    }
                }
            }
        }
    }

    private static float faceSize(float f) {
        int i = Math.round(Math.round(16.0F * f) / 8.0F) * 8;
        if (i > 22.0F * f - 4.0F * f) {
            i -= 8;
        }
        return Math.max(8, i);
    }

    private static void drawFace(
            ImDrawList imDrawList, int i, float f, float g, float h, float f2, float g2) {
        float h2 = Math.max(1.0F, (float) Math.round(2.0F * f2));
        int scaleAlpha = HudChrome.scaleAlpha(-1, g2);
        imDrawList.addImageRounded(
                i, f, g, f + h, g + h, 0.125F, 0.125F, 0.25F, 0.25F, scaleAlpha, h2, 240);
        imDrawList.addImageRounded(
                i, f, g, f + h, g + h, 0.625F, 0.125F, 0.75F, 0.25F, scaleAlpha, h2, 240);
        imDrawList.addRect(f, g, f + h, g + h, HudChrome.scaleAlpha(402653183, g2), h2, 0, 1.0F);
    }

    int staffFace(UUID uuid, boolean bl) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        PlayerListEntry listEntry =
                networkHandler == null ? null : networkHandler.getPlayerListEntry(uuid);
        Identifier id;
        if (listEntry != null) {
            id = listEntry.getSkinTextures().body().texturePath();
            this.staffSkins.put(uuid, id);
            if (bl) {
                id =
                        NameProtectModule.hiddenHead(uuid, listEntry.getSkinTextures())
                                .body()
                                .texturePath();
            }
        } else if (bl) {
            id =
                    NameProtectModule.hiddenHead(uuid, DefaultSkinHelper.getSkinTextures(uuid))
                            .body()
                            .texturePath();
        } else {
            id = this.staffSkins.get(uuid);
            if (id == null) {
                id = DefaultSkinHelper.getSkinTextures(uuid).body().texturePath();
            }
        }
        GpuTexture glTexture;
        try {
            glTexture = client.getTextureManager().getTexture(id).getGlTexture();
        } catch (IllegalStateException exception) {
            return 0;
        }
        GlTexture glTexture2;
        if (glTexture instanceof GlTexture && !(glTexture2 = (GlTexture) glTexture).isClosed()) {
            if (this.nearestSkins.add(glTexture)) {
                int glGetInteger = GL11.glGetInteger(32873);
                GL11.glBindTexture(3553, glTexture2.getGlId());
                GL11.glTexParameteri(3553, 10241, 9728);
                GL11.glTexParameteri(3553, 10240, 9728);
                GL11.glBindTexture(3553, glGetInteger);
            }
            return glTexture2.getGlId();
        } else {
            return 0;
        }
    }

    private float planRadar(HudLayoutStore.HudElementState hudElementState, boolean bl) {
        if (!bl && !RadarModule.shouldShowHud()) {
            return 1.0F;
        } else {
            float f = this.radarModule != null ? this.radarModule.size() : 150.0F;
            return Math.max(2.0F, (float) Math.round(f * scaleOf(hudElementState)));
        }
    }

    private void drawRadar(
            ImDrawList imDrawList,
            HudLayoutStore.HudElementState hudElementState,
            float f,
            MinecraftClient client,
            boolean bl,
            HudRenderer.DrawTransform drawTransform) {
        float alpha2 = drawTransform.alpha();
        if (!(alpha2 <= 0.001F) && !(f <= 1.0F)) {
            float scale = scaleOf(hudElementState);
            HudRenderer.Type type2 = HudRenderer.Type.of(scale);
            float g = Math.round(hudElementState.x + drawTransform.xOffset());
            float h = Math.round(hudElementState.y + drawTransform.yOffset());
            float f2 = f * 0.5F;
            float g2 = g + f2;
            float h2 = h + f2;
            int hudAccent = this.hudAccent();
            Surface surface = hudSurface(hudElementState);
            if (bl) {
                drawDiscShadow(imDrawList, g2, h2, f2, scale, alpha2);
            }
            imDrawList.addCircleFilled(
                    g2, h2, f2, HudChrome.scaleAlpha(GuiPalette.fill(surface), alpha2), 0);
            SurfaceBackground.drawDisc(imDrawList, surface, g2, h2, f2, alpha2);
            imDrawList.addCircle(
                    g2, h2, f2 - 0.5F, HudChrome.scaleAlpha(402653183, alpha2), 0, 1.0F);
            this.measureRadarLetters(type2.label(), type2.labelPx());
            float f3 = f2 - 10.0F * scale - this.radarLetterHalf;
            float g3 = f3 - this.radarLetterHalf - 4.0F * scale;
            float h3 = f2 - 5.0F * scale;
            int scaleAlpha = HudChrome.scaleAlpha(536870911, alpha2);
            for (float f4 : RADAR_RINGS) {
                if (h3 * f4 < g3) {
                    imDrawList.addCircle(g2, h2, h3 * f4, scaleAlpha, 0, 1.0F);
                }
            }
            float g4 = (float) Math.floor(g2) + 0.5F;
            float h4 = (float) Math.floor(h2) + 0.5F;
            imDrawList.addLine(g4, h2 - g3, g4, h2 + g3, scaleAlpha, 1.0F);
            imDrawList.addLine(g2 - g3, h4, g2 + g3, h4, scaleAlpha, 1.0F);
            ClientPlayerEntity player = client.player;
            boolean bl2 = this.radarModule == null || this.radarModule.rotate();
            float tickProgress = client.getRenderTickCounter().getTickProgress(false);
            double d = player != null ? Math.toRadians(player.getYaw(tickProgress)) : 0.0;
            float f5 = (float) Math.sin(d);
            float g5 = (float) Math.cos(d);
            for (int k = 0; k < RADAR_LETTERS.length; k++) {
                int i = RADAR_LETTER_DX[k];
                int j = RADAR_LETTER_DZ[k];
                float h5 = bl2 ? -i * g5 - j * f5 : i;
                float f6 = bl2 ? i * f5 - j * g5 : j;
                int m = k == 0 ? hudAccent : HudChrome.TEXT_LABEL;
                HudChrome.text(
                        imDrawList,
                        type2.label(),
                        type2.labelPx(),
                        Math.round(g2 + h5 * f3 - this.radarLetterW[k] * 0.5F),
                        Math.round(h2 + f6 * f3 - this.radarLetterMid),
                        HudChrome.scaleAlpha(m, alpha2),
                        RADAR_LETTERS[k],
                        true);
            }
            if (this.radarModule != null && player != null && client.world != null) {
                float g6 = Math.max(1.0F, this.radarModule.range());
                double e = (double) g6 * g6;
                float h6 = h3 / g6;
                double d2 = MathHelper.lerp((double) tickProgress, player.lastX, player.getX());
                double e2 = MathHelper.lerp((double) tickProgress, player.lastZ, player.getZ());
                float f7 = 3.2F * scale;
                float g7 = Math.max(1.0F, 0.75F * scale);
                int scaleAlpha2 = HudChrome.scaleAlpha(-1946157056, alpha2);
                int scaleAlpha3 =
                        HudChrome.scaleAlpha(GuiPalette.abgr(this.radarModule.dotColor()), alpha2);
                for (AbstractClientPlayerEntity player2 : client.world.getPlayers()) {
                    double d3;
                    double e3;
                    if (player2 != player
                            && this.radarModule.shows(player2)
                            && !((e3 =
                                                            MathHelper.lerp(
                                                                            (double) tickProgress,
                                                                            player2.lastX,
                                                                            player2.getX())
                                                                    - d2)
                                                    * e3
                                            + (d3 =
                                                            MathHelper.lerp(
                                                                            (double) tickProgress,
                                                                            player2.lastZ,
                                                                            player2.getZ())
                                                                    - e2)
                                                    * d3
                                    > e)) {
                        float h7 = bl2 ? (float) (-e3 * g5 - d3 * f5) : (float) e3;
                        float f8 = bl2 ? (float) (e3 * f5 - d3 * g5) : (float) d3;
                        float g8 = g2 + h7 * h6;
                        float h8 = h2 + f8 * h6;
                        imDrawList.addCircleFilled(g8, h8, f7 + g7, scaleAlpha2, 12);
                        imDrawList.addCircleFilled(g8, h8, f7, scaleAlpha3, 12);
                    }
                }
            }
            imDrawList.addCircleFilled(
                    g2, h2, 6.0F * scale, HudChrome.scaleAlpha(hudAccent, alpha2 * 0.25F), 0);
            imDrawList.addCircleFilled(
                    g2, h2, 3.0F * scale, HudChrome.scaleAlpha(hudAccent, alpha2), 0);
            float f9 = bl2 ? 0.0F : -f5;
            float g9 = bl2 ? -1.0F : g5;
            float h9 = f2 - 1.0F - 1.5F * scale;
            float f10 = h9 - 5.0F * scale;
            float g10 = 3.5F * scale;
            imDrawList.addTriangleFilled(
                    g2 + f9 * h9,
                    h2 + g9 * h9,
                    g2 + f9 * f10 - g9 * g10,
                    h2 + g9 * f10 + f9 * g10,
                    g2 + f9 * f10 + g9 * g10,
                    h2 + g9 * f10 - f9 * g10,
                    HudChrome.scaleAlpha(hudAccent, alpha2));
        }
    }

    private static void drawDiscShadow(
            ImDrawList imDrawList, float f, float g, float h, float f2, float g2) {
        float h2 = Math.max(1.0F, f2);
        int scaleAlpha = HudChrome.scaleAlpha(218103808, g2);
        for (int m = 3; m >= 1; m--) {
            imDrawList.addCircleFilled(f, g + h2 * 0.5F, h + (m + 0.5F) * h2, scaleAlpha, 0);
        }
    }

    private void measureRadarLetters(ImFont radarLetterFont2, int radarLetterPx2) {
        if (radarLetterFont2 != this.radarLetterFont || radarLetterPx2 != this.radarLetterPx) {
            this.radarLetterFont = radarLetterFont2;
            this.radarLetterPx = radarLetterPx2;
            for (int j = 0; j < RADAR_LETTERS.length; j++) {
                this.radarLetterW[j] =
                        HudChrome.width(radarLetterFont2, radarLetterPx2, RADAR_LETTERS[j]);
            }
            float capTop = this.capTop(radarLetterFont2, radarLetterPx2);
            float ascent = HudChrome.ascent(radarLetterFont2, radarLetterPx2);
            this.radarLetterMid = (capTop + ascent) * 0.5F;
            this.radarLetterHalf = (ascent - capTop) * 0.5F;
        }
    }

    private HudRenderer.HotkeyPlan planHotkeys(
            HudLayoutStore.HudElementState hudElementState, boolean bl, float f) {
        float scale = scaleOf(hudElementState);
        HudRenderer.Type type2 = HudRenderer.Type.of(scale);
        float g = 22.0F * scale;
        List list = !bl && !HotkeysModule.shouldShowHud() ? List.of() : this.hotkeyLinesFor(type2);
        if (list.isEmpty()) {
            this.removeSlot("hotkeys", type2.textPx());
            float h =
                    bl
                            ? 8.0F * scale * 2.0F
                                    + HudChrome.width(
                                            type2.label(), type2.labelPx(), "No keys bound")
                            : 1.0F;
            return new HudRenderer.HotkeyPlan(
                    list, type2, (float) Math.round(h), (float) Math.round(g));
        } else {
            float f2 =
                    this.hotkeyNameColumn
                            + 16.0F * scale
                            + keycapWidth(this.hotkeyKeyColumn, scale);
            float g2 = 8.0F * scale * 2.0F + Math.max(this.hotkeyHeaderW, f2);
            float slot2 = this.slot("hotkeys", type2.textPx(), (float) Math.ceil(g2), f);
            return new HudRenderer.HotkeyPlan(
                    list,
                    type2,
                    (float) Math.round(slot2),
                    (float) Math.round(20.0F * scale + g * list.size()));
        }
    }

    private List<HudRenderer.HotkeyLine> hotkeyLinesFor(HudRenderer.Type type2) {
        long l2 = ModuleRevision.current();
        if (l2 == this.hotkeyRevision
                && type2.text() == this.hotkeyFont
                && type2.textPx() == this.hotkeyTextPx
                && type2.labelPx() == this.hotkeyKeyPx) {
            return this.hotkeyLines;
        } else {
            this.hotkeyRevision = l2;
            this.hotkeyFont = type2.text();
            this.hotkeyTextPx = type2.textPx();
            this.hotkeyKeyPx = type2.labelPx();
            ArrayList list = new ArrayList();
            float f = 0.0F;
            float g = 0.0F;
            for (ClientModule module : ModuleRegistry.all()) {
                int keybind = module.keybind();
                if (keybind != -1) {
                    String string = ClickGuiRenderer.keyName(keybind).toUpperCase(Locale.ROOT);
                    float width2 = HudChrome.width(type2.text(), type2.textPx(), module.name());
                    float width3 = HudChrome.width(type2.text(), type2.labelPx(), string);
                    list.add(
                            new HudRenderer.HotkeyLine(
                                    module.name(), string, module.isEnabled(), width2, width3));
                    f = Math.max(f, width2);
                    g = Math.max(g, width3);
                }
            }
            this.hotkeyNameColumn = f;
            this.hotkeyKeyColumn = g;
            this.hotkeyHeaderW = HudChrome.width(type2.label(), type2.labelPx(), "HOTKEYS");
            this.hotkeyCapMid =
                    (this.capTop(type2.text(), type2.labelPx())
                                    + HudChrome.ascent(type2.text(), type2.labelPx()))
                            * 0.5F;
            this.hotkeyLines = List.copyOf(list);
            return this.hotkeyLines;
        }
    }

    private void drawHotkeys(
            ImDrawList imDrawList,
            HudLayoutStore.HudElementState hudElementState,
            HudRenderer.HotkeyPlan hotkeyPlan,
            boolean bl,
            HudRenderer.DrawTransform drawTransform) {
        float alpha2 = drawTransform.alpha();
        if (!(alpha2 <= 0.001F)) {
            float scale = scaleOf(hudElementState);
            float f = Math.round(hudElementState.x + drawTransform.xOffset());
            float g = Math.round(hudElementState.y + drawTransform.yOffset());
            float width2 = hudElementState.width;
            float height2 = hudElementState.height;
            HudRenderer.Type type2 = hotkeyPlan.type();
            float h = 22.0F * scale;
            float f2 = 8.0F * scale;
            if (hotkeyPlan.lines().isEmpty()) {
                List list =
                        List.of(
                                new HudRenderer.Step(
                                        -1, f, g, f + width2, (float) Math.round(g + h), alpha2));
                this.drawRows(
                        imDrawList,
                        list,
                        hudSurface(hudElementState),
                        f,
                        g,
                        width2,
                        height2,
                        rowsRadius(scale),
                        scale,
                        bl);
                HudChrome.text(
                        imDrawList,
                        type2.label(),
                        type2.labelPx(),
                        f + f2,
                        g + (float) Math.floor((h - type2.labelPx()) * 0.5F),
                        HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, alpha2),
                        "No keys bound",
                        true);
            } else {
                float g2 = 20.0F * scale;
                ArrayList list2 = new ArrayList(hotkeyPlan.lines().size() + 1);
                list2.add(
                        new HudRenderer.Step(
                                -1, f, g, f + width2, (float) Math.round(g + g2), alpha2));
                for (int i = 0; i < hotkeyPlan.lines().size(); i++) {
                    list2.add(
                            new HudRenderer.Step(
                                    i,
                                    f,
                                    (float) Math.round(g + g2 + h * i),
                                    f + width2,
                                    (float) Math.round(g + g2 + h * (i + 1)),
                                    alpha2));
                }
                this.drawRows(
                        imDrawList,
                        list2,
                        hudSurface(hudElementState),
                        f,
                        g,
                        width2,
                        height2,
                        rowsRadius(scale),
                        scale,
                        bl);
                HudRenderer.Step step = (HudRenderer.Step) list2.get(0);
                float h2 =
                        step.t()
                                + (float) Math.floor((step.b() - step.t() - type2.textPx()) * 0.5F);
                float f3 =
                        h2
                                + HudChrome.baselineOffset(
                                        type2.text(),
                                        type2.textPx(),
                                        type2.label(),
                                        type2.labelPx());
                HudChrome.text(
                        imDrawList,
                        type2.label(),
                        type2.labelPx(),
                        f + f2,
                        f3,
                        HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, alpha2),
                        "HOTKEYS",
                        true);
                float keycapWidth = keycapWidth(this.hotkeyKeyColumn, scale);
                float g3 = (float) Math.floor((h - type2.textPx()) * 0.5F);
                for (int j = 1; j < list2.size(); j++) {
                    HudRenderer.Step step2 = (HudRenderer.Step) list2.get(j);
                    HudRenderer.HotkeyLine line = hotkeyPlan.lines().get(step2.item());
                    HudChrome.text(
                            imDrawList,
                            type2.text(),
                            type2.textPx(),
                            step2.l() + f2,
                            step2.t() + g3,
                            HudChrome.scaleAlpha(line.on() ? -1 : HudChrome.TEXT_LABEL, alpha2),
                            line.name(),
                            true);
                    this.drawKeycap(
                            imDrawList,
                            type2,
                            line,
                            step2.r() - f2,
                            (step2.t() + step2.b()) * 0.5F,
                            keycapWidth,
                            scale,
                            alpha2);
                }
            }
        }
    }

    private void drawKeycap(
            ImDrawList imDrawList,
            HudRenderer.Type type2,
            HudRenderer.HotkeyLine hotkeyLine,
            float f,
            float g,
            float h,
            float f2,
            float g2) {
        float keycapHeight = keycapHeight(f2);
        float h2 = Math.round(f - h);
        float f3 = Math.round(g - keycapHeight * 0.5F);
        float g3 = Math.min(hudRounding(), 4.0F) * f2;
        imDrawList.addRectFilled(
                h2, f3, h2 + h, f3 + keycapHeight, HudChrome.scaleAlpha(452984831, g2), g3);
        imDrawList.addRect(
                h2,
                f3,
                h2 + h,
                f3 + keycapHeight,
                HudChrome.scaleAlpha(402653183, g2),
                g3,
                0,
                1.0F);
        HudChrome.text(
                imDrawList,
                type2.text(),
                type2.labelPx(),
                Math.round(h2 + (h - hotkeyLine.keyW()) * 0.5F),
                Math.round(f3 + keycapHeight * 0.5F - this.hotkeyCapMid),
                HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, g2),
                hotkeyLine.key(),
                true);
    }

    private static float keycapHeight(float f) {
        return Math.round(16.0F * f);
    }

    private static float keycapWidth(float f, float g) {
        return Math.max(keycapHeight(g), (float) Math.ceil(f + 5.0F * g * 2.0F));
    }

    private HudRenderer.SpotifyPlan planSpotify(
            HudLayoutStore.HudElementState hudElementState, boolean bl, float f) {
        float scale = scaleOf(hudElementState);
        HudRenderer.Type type2 = HudRenderer.Type.of(scale);
        if (!bl && !SpotifyHudModule.shouldShowHud()) {
            this.removeSlot("spotify", type2.textPx());
            this.releaseSpotifyArt();
            this.lyricBlockH = 0.0F;
            return new HudRenderer.SpotifyPlan(
                    type2,
                    HudRenderer.SpotifyLook.HIDDEN,
                    HudRenderer.SpotifyGlyph.NONE,
                    "",
                    "",
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    false,
                    null,
                    HudRenderer.SpotifyColors.DEFAULT,
                    1.0F,
                    (float) Math.round(28.0F * scale));
        } else {
            SpotifyHudModule module = SpotifyHudModule.get();
            SpotifyHudModule module2 = SpotifyHudModule.active();
            NowPlaying nowPlaying = module2 != null ? module2.nowPlaying() : NowPlaying.WAITING;
            boolean bl2 = nowPlaying.active() && nowPlaying.hasTrack();
            boolean bl3 = bl || hudElementState.visible;
            if (bl2 && nowPlaying.source() == NowPlaying.Source.SESSION) {
                return this.planSpotifyCard(module, nowPlaying, false, bl3, type2, scale, bl, f);
            } else if (bl && !bl2) {
                return this.planSpotifyCard(
                        module, SAMPLE_TRACK, true, true, type2, scale, true, f);
            } else {
                this.releaseSpotifyArt();
                this.lyricBlockH = 0.0F;
                return this.planSpotifyCompact(module, nowPlaying, type2, scale, bl, f);
            }
        }
    }

    private HudRenderer.SpotifyPlan planSpotifyCompact(
            SpotifyHudModule module,
            NowPlaying nowPlaying,
            HudRenderer.Type type2,
            float f,
            boolean bl,
            float g) {
        boolean bl2 = module == null || module.showArtist();
        String string = "";
        HudRenderer.SpotifyGlyph nONE = HudRenderer.SpotifyGlyph.NONE;
        boolean bl3 = false;
        String string2;
        switch (nowPlaying.status()) {
            case PLAYING:
            case PAUSED:
                nONE =
                        nowPlaying.playing()
                                ? HudRenderer.SpotifyGlyph.PLAYING
                                : HudRenderer.SpotifyGlyph.PAUSED;
                this.foldTrack(nowPlaying, type2.text());
                string2 = nowPlaying.hasTrack() ? this.spotifyTitle : "Not playing";
                if (bl2) {
                    string = this.spotifyArtist;
                }
                break;
            case NOT_RUNNING:
                string2 = "Spotify is not open";
                bl3 = true;
                break;
            case UNSUPPORTED:
                string2 = "Spotify \u00b7 not supported here";
                bl3 = true;
                break;
            default:
                string2 = "Nothing playing";
                bl3 = true;
        }
        ImFont imFont = bl3 ? type2.label() : type2.text();
        int i = bl3 ? type2.labelPx() : type2.textPx();
        float width2 = HudChrome.width(imFont, i, string2);
        float h = string.isEmpty() ? 0.0F : HudChrome.width(type2.label(), type2.labelPx(), string);
        float f2 = nONE == HudRenderer.SpotifyGlyph.NONE ? 0.0F : 20.0F * f;
        float g2 = 12.0F * f * 2.0F + f2;
        float h2 = Math.max(40.0F * f, (module != null ? module.cardWidth() : 320.0F) * f - g2);
        float f3 = Math.min(Math.max(width2, h), h2);
        float slot2 = this.slot("spotify", type2.textPx(), (float) Math.ceil(g2 + f3), g);
        float g3 =
                string.isEmpty()
                        ? 28.0F * f
                        : 8.0F * f * 2.0F + type2.textPx() + 2.0F * f + type2.labelPx();
        float h3 = Math.max(1.0F, slot2 - g2);
        boolean bl4 = module == null || module.scrollTitles();
        float f4 = 40.0F * f;
        float[] floats =
                this.marquee(
                        string2, string, width2, h, h3, bl4, nowPlaying.playing() || bl, f4, f, g);
        return new HudRenderer.SpotifyPlan(
                type2,
                bl3 ? HudRenderer.SpotifyLook.IDLE : HudRenderer.SpotifyLook.COMPACT,
                nONE,
                string2,
                string,
                width2,
                h,
                floats[0],
                floats[1],
                f4,
                bl4,
                null,
                HudRenderer.SpotifyColors.of(module),
                (float) Math.round(slot2),
                (float) Math.round(g3));
    }

    private void drawSpotify(
            ImDrawList imDrawList,
            HudLayoutStore.HudElementState hudElementState,
            HudRenderer.SpotifyPlan spotifyPlan,
            boolean bl,
            HudRenderer.DrawTransform drawTransform) {
        float alpha2 = drawTransform.alpha();
        if (!(alpha2 <= 0.001F) && spotifyPlan.look() != HudRenderer.SpotifyLook.HIDDEN) {
            float scale = scaleOf(hudElementState);
            float f = Math.round(hudElementState.x + drawTransform.xOffset());
            float g = Math.round(hudElementState.y + drawTransform.yOffset());
            float width2 = hudElementState.width;
            float height2 = hudElementState.height;
            Surface surface = hudSurface(hudElementState);
            boolean needsTextGuard = HudChrome.needsTextGuard(surface);
            HudChrome.card(
                    imDrawList,
                    surface,
                    f,
                    g,
                    width2,
                    height2,
                    hudRounding() * scale,
                    alpha2,
                    bl,
                    scale);
            if (spotifyPlan.look() == HudRenderer.SpotifyLook.CARD) {
                this.drawSpotifyCard(
                        imDrawList,
                        spotifyPlan,
                        f,
                        g,
                        width2,
                        height2,
                        scale,
                        alpha2,
                        needsTextGuard);
            } else {
                this.drawSpotifyCompact(
                        imDrawList,
                        spotifyPlan,
                        f,
                        g,
                        width2,
                        height2,
                        scale,
                        alpha2,
                        needsTextGuard);
            }
        }
    }

    private void drawSpotifyCompact(
            ImDrawList imDrawList,
            HudRenderer.SpotifyPlan spotifyPlan,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            boolean bl) {
        HudRenderer.Type type2 = spotifyPlan.type();
        float f3 = f + 12.0F * g2;
        if (spotifyPlan.glyph() != HudRenderer.SpotifyGlyph.NONE) {
            this.drawPlayState(
                    imDrawList,
                    f3,
                    g + f2 * 0.5F,
                    10.0F * g2,
                    spotifyPlan.glyph() == HudRenderer.SpotifyGlyph.PLAYING,
                    h2);
            f3 += 20.0F * g2;
        }
        float g3 = Math.max(1.0F, f + h - 12.0F * g2 - f3);
        if (spotifyPlan.look() == HudRenderer.SpotifyLook.IDLE) {
            float h3 = g + (float) Math.floor((f2 - type2.labelPx()) * 0.5F);
            marqueeLine(
                    imDrawList,
                    type2.label(),
                    type2.labelPx(),
                    f3,
                    h3,
                    g3,
                    spotifyPlan.titleW(),
                    0.0F,
                    spotifyPlan.loopGap(),
                    false,
                    spotifyPlan.title(),
                    HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, h2),
                    bl);
        } else {
            float f4 = spotifyPlan.glyph() == HudRenderer.SpotifyGlyph.PAUSED ? 0.62F : 1.0F;
            boolean bl2 = !spotifyPlan.artist().isEmpty();
            float g4 = bl2 ? type2.textPx() + 2.0F * g2 + type2.labelPx() : type2.textPx();
            float h4 = g + (float) Math.floor((f2 - g4) * 0.5F);
            marqueeLine(
                    imDrawList,
                    type2.text(),
                    type2.textPx(),
                    f3,
                    h4,
                    g3,
                    spotifyPlan.titleW(),
                    spotifyPlan.titleOffset(),
                    spotifyPlan.loopGap(),
                    spotifyPlan.scroll(),
                    spotifyPlan.title(),
                    HudChrome.scaleAlpha(spotifyPlan.colors().title(), h2 * f4),
                    bl);
            if (bl2) {
                float f5 = h4 + type2.textPx() + 2.0F * g2;
                marqueeLine(
                        imDrawList,
                        type2.label(),
                        type2.labelPx(),
                        f3,
                        f5,
                        g3,
                        spotifyPlan.artistW(),
                        spotifyPlan.artistOffset(),
                        spotifyPlan.loopGap(),
                        spotifyPlan.scroll(),
                        spotifyPlan.artist(),
                        HudChrome.scaleAlpha(spotifyPlan.colors().artist(), h2),
                        bl);
            }
        }
    }

    private void drawPlayState(
            ImDrawList imDrawList, float f, float g, float h, boolean bl, float f2) {
        float g2 = Math.round(g - h * 0.5F);
        float h2 = g2 + Math.round(h);
        if (bl) {
            imDrawList.addTriangleFilled(
                    f,
                    g2,
                    f + h * 0.9F,
                    (g2 + h2) * 0.5F,
                    f,
                    h2,
                    HudChrome.scaleAlpha(this.hudAccent(), f2));
        } else {
            float f3 = Math.max(2.0F, (float) Math.round(h * 0.3F));
            float g3 = Math.max(2.0F, (float) Math.round(h * 0.25F));
            int scaleAlpha = HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, f2);
            float h3 = Math.round(f + (h - f3 * 2.0F - g3) * 0.5F);
            imDrawList.addRectFilled(h3, g2, h3 + f3, h2, scaleAlpha, 1.0F);
            imDrawList.addRectFilled(h3 + f3 + g3, g2, h3 + f3 * 2.0F + g3, h2, scaleAlpha, 1.0F);
        }
    }

    private HudRenderer.SpotifyPlan planSpotifyCard(
            SpotifyHudModule module,
            NowPlaying nowPlaying,
            boolean bl,
            boolean bl2,
            HudRenderer.Type type2,
            float f,
            boolean bl3,
            float g) {
        boolean bl4 = module == null || module.showArtist();
        boolean bl5 = module == null || module.showAlbumArt();
        boolean bl6 = module == null || module.showProgress();
        boolean bl7 = module == null || module.showLyrics();
        boolean bl8 = module == null || module.scrollTitles();
        long l2 = System.currentTimeMillis();
        this.foldTrack(nowPlaying, type2.text());
        String spotifyTitle2 = this.spotifyTitle;
        String string = bl4 ? this.spotifyArtist : "";
        boolean timeline2 = nowPlaying.hasTimeline();
        long positionMs = nowPlaying.positionMs();
        if (!bl) {
            this.spotifyClock.update(nowPlaying, l2);
            positionMs = this.spotifyClock.positionAt(l2);
        }
        float capTop = this.capTop(type2.label(), type2.labelPx());
        float h =
                type2.labelPx()
                        + 3.0F * f
                        + type2.textPx()
                        + (string.isEmpty() ? 0.0F : 1.0F * f + type2.labelPx())
                        + 7.0F * f;
        boolean bl9 = bl6 && timeline2;
        float f2 =
                bl9
                        ? h + type2.labelPx() + 6.0F * f + progressHeight(f)
                        : h + HudChrome.ascent(type2.label(), type2.labelPx());
        float g2 = Math.round(f2 - capTop);
        float h2 = 10.0F * f;
        float f3 = Math.round(Math.max(200.0F, module != null ? module.cardWidth() : 320.0F) * f);
        boolean bl10 = bl5 && f3 - h2 * 2.0F - g2 - 10.0F * f >= 128.0F * f;
        float g3 = f3 - h2 * 2.0F - (bl10 ? g2 + 10.0F * f : 0.0F);
        String string2 = "";
        String string3 = "";
        float h3 = 0.0F;
        if (timeline2) {
            long l3 = positionMs / 1000L;
            long l4 = (nowPlaying.durationMs() + 500L) / 1000L;
            string2 = formatSeconds(l3);
            string3 = "\u2212" + formatSeconds(Math.max(0L, l4 - l3));
            h3 = clamp01((float) positionMs / (float) nowPlaying.durationMs());
        }
        float width2 = HudChrome.width(type2.text(), type2.textPx(), spotifyTitle2);
        float f4 =
                string.isEmpty() ? 0.0F : HudChrome.width(type2.label(), type2.labelPx(), string);
        float g4 = 40.0F * f;
        float[] floats =
                this.marquee(
                        spotifyTitle2,
                        string,
                        width2,
                        f4,
                        g3,
                        bl8,
                        nowPlaying.playing() || bl3,
                        g4,
                        f,
                        g);
        boolean bl11 =
                bl ? bl7 && timeline2 : bl2 && module != null && module.wantsLyrics(nowPlaying);
        Lyrics lyrics2 = !bl11 ? null : (bl ? SAMPLE_LYRICS : module.lyricsFor(nowPlaying));
        boolean bl12 = module == null || module.lyricsOnline();
        int i = module == null ? 2 : module.lyricLines();
        SpotifyHudModule.LyricMotion lyricMotion =
                module == null ? SpotifyHudModule.LyricMotion.WAVE : module.lyricMotion();
        SpotifyHudModule.LyricHighlight lyricHighlight =
                module == null ? SpotifyHudModule.LyricHighlight.OFF : module.lyricHighlight();
        HudRenderer.LyricView lyricView =
                this.planLyrics(
                        lyrics2,
                        bl11,
                        bl12,
                        positionMs,
                        i,
                        lyricMotion,
                        lyricHighlight,
                        type2,
                        f3 - h2 * 2.0F,
                        f,
                        g,
                        l2);
        float h4 = nowPlaying.playing() ? 1.0F : 0.0F;
        this.lyricPlaying =
                this.lyricPlaying + (h4 - this.lyricPlaying) * (1.0F - (float) Math.exp(-8.0F * g));
        float f5 = 0.7F + 0.3F * this.lyricPlaying;
        HudRenderer.SpotifyCard spotifyCard =
                new HudRenderer.SpotifyCard(
                        nowPlaying,
                        bl,
                        bl10,
                        g2,
                        h2 - capTop,
                        g3,
                        timeline2,
                        string2,
                        string3,
                        h3,
                        bl9,
                        lyricView,
                        f5);
        float g5 = h2 * 2.0F + g2 + lyricView.height();
        return new HudRenderer.SpotifyPlan(
                type2,
                HudRenderer.SpotifyLook.CARD,
                HudRenderer.SpotifyGlyph.NONE,
                spotifyTitle2,
                string,
                width2,
                f4,
                floats[0],
                floats[1],
                g4,
                bl8,
                spotifyCard,
                HudRenderer.SpotifyColors.of(module),
                f3,
                (float) Math.round(g5));
    }

    private void drawSpotifyCard(
            ImDrawList imDrawList,
            HudRenderer.SpotifyPlan spotifyPlan,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            boolean bl) {
        HudRenderer.SpotifyCard spotifyCard = spotifyPlan.card();
        HudRenderer.Type type2 = spotifyPlan.type();
        float f3 = 10.0F * g2;
        boolean playing = spotifyCard.track().playing();
        float g3 = f + f3;
        if (spotifyCard.art()) {
            this.drawAlbumArt(imDrawList, spotifyCard, g3, g + f3, spotifyCard.header(), g2, h2);
            g3 += spotifyCard.header() + 10.0F * g2;
        } else {
            this.releaseSpotifyArt();
        }
        float column2 = spotifyCard.column();
        float h3 = g + spotifyCard.textTop();
        HudChrome.text(
                imDrawList,
                type2.label(),
                type2.labelPx(),
                g3,
                Math.round(h3),
                HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, h2),
                playing ? "NOW PLAYING" : "PAUSED",
                bl);
        float f4;
        marqueeLine(
                imDrawList,
                type2.text(),
                type2.textPx(),
                g3,
                Math.round(f4 = h3 + (type2.labelPx() + 3.0F * g2)),
                column2,
                spotifyPlan.titleW(),
                spotifyPlan.titleOffset(),
                spotifyPlan.loopGap(),
                spotifyPlan.scroll(),
                spotifyPlan.title(),
                HudChrome.scaleAlpha(spotifyPlan.colors().title(), h2),
                bl);
        h3 = f4 + type2.textPx();
        if (!spotifyPlan.artist().isEmpty()) {
            float g4;
            marqueeLine(
                    imDrawList,
                    type2.label(),
                    type2.labelPx(),
                    g3,
                    Math.round(g4 = h3 + 1.0F * g2),
                    column2,
                    spotifyPlan.artistW(),
                    spotifyPlan.artistOffset(),
                    spotifyPlan.loopGap(),
                    spotifyPlan.scroll(),
                    spotifyPlan.artist(),
                    HudChrome.scaleAlpha(spotifyPlan.colors().artist(), h2),
                    bl);
            h3 = g4 + type2.labelPx();
        }
        h3 = Math.round(h3 + 7.0F * g2);
        this.drawTransport(imDrawList, type2, spotifyCard, g3, h3, column2, g2, h2, bl);
        if (spotifyCard.progressBar()) {
            float h4 = Math.round(h3 + type2.labelPx() + 6.0F * g2);
            this.drawProgress(
                    imDrawList,
                    g3,
                    h4,
                    column2,
                    progressHeight(g2),
                    spotifyCard.progress(),
                    playing,
                    h2);
        }
        HudRenderer.LyricView lyricView;
        if ((lyricView = spotifyCard.lyrics()).height() > 0.5F) {
            float f5 = Math.round(g + f3 + spotifyCard.header() + 12.0F * g2);
            this.drawLyrics(
                    imDrawList,
                    type2,
                    lyricView,
                    spotifyPlan.colors(),
                    f + f3,
                    f5,
                    h - f3 * 2.0F,
                    g + f2 - Math.max(1.0F, g2),
                    g2,
                    h2 * spotifyCard.lyricAlpha(),
                    bl);
        }
    }

    private void drawAlbumArt(
            ImDrawList imDrawList,
            HudRenderer.SpotifyCard spotifyCard,
            float f,
            float g,
            float h,
            float f2,
            float g2) {
        float h2 = Math.min(hudRounding() * 0.5F, 4.0F) * f2;
        if (spotifyCard.sample()) {
            this.releaseSpotifyArt();
        }
        ThemeImages.Texture texture =
                spotifyCard.sample() ? null : this.spotifyArtTexture(spotifyCard.track().art());
        if (texture != null) {
            imDrawList.addImageRounded(
                    texture.id(),
                    f,
                    g,
                    f + h,
                    g + h,
                    0.0F,
                    0.0F,
                    1.0F,
                    1.0F,
                    HudChrome.scaleAlpha(-1, g2),
                    h2,
                    240);
        } else {
            imDrawList.addRectFilled(f, g, f + h, g + h, HudChrome.scaleAlpha(318767103, g2), h2);
            float f3 = Math.max(2.0F, (float) Math.round(h * 0.07F));
            float g3 = Math.max(2.0F, (float) Math.round(f3 * 0.8F));
            float h3 = Math.round(g + h * 0.62F);
            float f4 = Math.round(f + (h - f3 * 3.0F - g3 * 2.0F) * 0.5F);
            int scaleAlpha = HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, g2 * 0.45F);
            float[] floats = new float[] {0.2F, 0.34F, 0.26F};
            for (int m = 0; m < floats.length; m++) {
                float g4 = f4 + m * (f3 + g3);
                imDrawList.addRectFilled(
                        g4, h3 - Math.round(h * floats[m]), g4 + f3, h3, scaleAlpha, 1.0F);
            }
        }
        imDrawList.addRect(f, g, f + h, g + h, HudChrome.scaleAlpha(402653183, g2), h2, 0, 1.0F);
    }

    private void drawTransport(
            ImDrawList imDrawList,
            HudRenderer.Type type2,
            HudRenderer.SpotifyCard spotifyCard,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            boolean bl) {
        float h2 = 0.0F;
        float f3 = 0.0F;
        if (spotifyCard.timeline()) {
            h2 = HudChrome.tabularWidth(type2.label(), type2.labelPx(), spotifyCard.elapsed());
            f3 = HudChrome.tabularWidth(type2.label(), type2.labelPx(), spotifyCard.remaining());
            int scaleAlpha = HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, g2);
            HudChrome.tabular(
                    imDrawList,
                    type2.label(),
                    type2.labelPx(),
                    f,
                    g,
                    scaleAlpha,
                    spotifyCard.elapsed(),
                    bl);
            HudChrome.tabular(
                    imDrawList,
                    type2.label(),
                    type2.labelPx(),
                    Math.round(f + h - f3),
                    g,
                    scaleAlpha,
                    spotifyCard.remaining(),
                    bl);
        }
        float g3;
        float h3;
        float f4;
        float g4;
        float h4;
        float f5;
        if (!((f5 = f + h * 0.5F)
                                - (h4 =
                                                (g4 = Math.max(4.0F, (float) Math.round(8.0F * f2)))
                                                                * 2.0F
                                                        + (f4 =
                                                                Math.max(
                                                                        5.0F,
                                                                        (float)
                                                                                Math.round(
                                                                                        10.0F
                                                                                                * f2)))
                                                        + (h3 = Math.round(14.0F * f2)) * 2.0F)
                                        * 0.5F
                        < f + h2 + (g3 = 8.0F * f2))
                && !(f5 + h4 * 0.5F > f + h - f3 - g3)) {
            float g5 =
                    g
                            + (this.capTop(type2.label(), type2.labelPx())
                                            + HudChrome.ascent(type2.label(), type2.labelPx()))
                                    * 0.5F;
            float h5 = Math.round(f5 - f4 * 0.5F);
            float[] floats = new float[] {h5 - h3 - g4, h5, h5 + f4 + h3};
            float[] floats2 = new float[] {g4, f4, g4};
            float f6 = Math.round(3.0F * f2);
            float mousePosX = ImGui.getMousePosX();
            float mousePosY = ImGui.getMousePosY();
            for (int m = 0; m < 3; m++) {
                float g6 = floats[m] - f6;
                float h6 = floats[m] + floats2[m] + f6;
                float f7 = Math.round(g5 - floats2[m] * 0.5F) - f6;
                float g7 = Math.round(g5 + floats2[m] * 0.5F) + f6;
                this.spotifyButtons[m * 4] = g6;
                this.spotifyButtons[m * 4 + 1] = f7;
                this.spotifyButtons[m * 4 + 2] = h6;
                this.spotifyButtons[m * 4 + 3] = g7;
                boolean bl2 =
                        this.chatInput
                                && mousePosX >= g6
                                && mousePosX <= h6
                                && mousePosY >= f7
                                && mousePosY <= g7;
                if (bl2) {
                    imDrawList.addRectFilled(
                            g6, f7, h6, g7, HudChrome.scaleAlpha(654311423, g2), (h6 - g6) * 0.5F);
                }
                int scaleAlpha2 =
                        HudChrome.scaleAlpha(
                                !bl2 && m != 1 ? HudChrome.TEXT_LABEL : HudChrome.TEXT_NAME, g2);
                if (m == 1) {
                    drawPlayPauseGlyph(
                            imDrawList,
                            floats[m],
                            g5,
                            f4,
                            spotifyCard.track().playing(),
                            scaleAlpha2);
                } else {
                    drawSkipGlyph(imDrawList, floats[m], g5, g4, m == 2, scaleAlpha2);
                }
            }
            this.spotifyButtonsDrawn = true;
        }
    }

    private int spotifyButtonAt(float f, float g) {
        for (int i = 0; i < SPOTIFY_BUTTONS.length; i++) {
            if (f >= this.spotifyButtons[i * 4]
                    && f <= this.spotifyButtons[i * 4 + 2]
                    && g >= this.spotifyButtons[i * 4 + 1]
                    && g <= this.spotifyButtons[i * 4 + 3]) {
                return i;
            }
        }
        return -1;
    }

    private static void drawSkipGlyph(
            ImDrawList imDrawList, float f, float g, float h, boolean bl, int i) {
        float f2 = Math.round(g - h * 0.5F);
        float g2 = f2 + h;
        float h2 = (f2 + g2) * 0.5F;
        float f3 = Math.max(1.0F, (float) Math.round(h * 0.2F));
        if (bl) {
            imDrawList.addTriangleFilled(f, f2, f + h - f3, h2, f, g2, i);
            imDrawList.addRectFilled(f + h - f3, f2, f + h, g2, i);
        } else {
            imDrawList.addRectFilled(f, f2, f + f3, g2, i);
            imDrawList.addTriangleFilled(f + h, f2, f + h, g2, f + f3, h2, i);
        }
    }

    private static void drawPlayPauseGlyph(
            ImDrawList imDrawList, float f, float g, float h, boolean bl, int i) {
        float f2 = Math.round(g - h * 0.5F);
        float g2 = f2 + h;
        if (bl) {
            float h2 = Math.max(2.0F, (float) Math.round(h * 0.3F));
            float f3 = Math.max(2.0F, (float) Math.round(h * 0.24F));
            float g3 = Math.round(f + (h - h2 * 2.0F - f3) * 0.5F);
            imDrawList.addRectFilled(g3, f2, g3 + h2, g2, i, 1.0F);
            imDrawList.addRectFilled(g3 + h2 + f3, f2, g3 + h2 * 2.0F + f3, g2, i, 1.0F);
        } else {
            float h3 = Math.round(h * 0.08F);
            imDrawList.addTriangleFilled(
                    f + h3, f2, f + h3 + h * 0.86F, (f2 + g2) * 0.5F, f + h3, g2, i);
        }
    }

    private void drawProgress(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            boolean bl,
            float h2) {
        float f3 = f2 * 0.5F;
        imDrawList.addRectFilled(f, g, f + h, g + f2, HudChrome.scaleAlpha(452984831, h2), f3);
        float g3 = Math.round(h * clamp01(g2));
        if (!(g3 < 1.0F)) {
            imDrawList.addRectFilled(
                    f,
                    g,
                    f + g3,
                    g + f2,
                    HudChrome.scaleAlpha(bl ? this.hudAccent() : HudChrome.TEXT_LABEL, h2),
                    f3);
        }
    }

    private static float progressHeight(float f) {
        return Math.max(1.0F, (float) Math.round(2.0F * f));
    }

    private HudRenderer.LyricView planLyrics(
            Lyrics lyrics2,
            boolean bl,
            boolean bl2,
            long l2,
            int i,
            SpotifyHudModule.LyricMotion lyricMotion,
            SpotifyHudModule.LyricHighlight lyricHighlight,
            HudRenderer.Type type2,
            float f,
            float g,
            float h,
            long lyricLoadingSince2) {
        Object object = List.of();
        String string = null;
        float f2 = -1.0F;
        boolean bl3 = false;
        if (!bl) {
            this.lyricLoadingSince = 0L;
            f2 = 0.0F;
        } else {
            if (lyrics2 == null || lyrics2.status() != Lyrics.Status.LOADING) {
                this.lyricLoadingSince = 0L;
            } else if (this.lyricLoadingSince == 0L) {
                this.lyricLoadingSince = lyricLoadingSince2;
            }
            if (lyrics2 != null) {
                switch (lyrics2.status()) {
                    case LOADING:
                        string =
                                lyricLoadingSince2 - this.lyricLoadingSince < 700L
                                        ? null
                                        : "Loading lyrics";
                        break;
                    case SYNCED:
                        this.prepareLyrics(lyrics2, type2.text());
                        if (this.lyricReadable) {
                            object = new ArrayList(16);
                            float lyricViewHeight =
                                    this.lyricViewHeight(lyrics2.synced(), i, type2, f, g);
                            this.syncedItems(
                                    (List<HudRenderer.LyricItem>) object,
                                    lyrics2.synced(),
                                    l2,
                                    lyricViewHeight,
                                    lyricMotion,
                                    lyricHighlight,
                                    type2,
                                    f,
                                    g,
                                    h);
                            f2 =
                                    lyricViewHeight
                                            - (type2.textPx()
                                                    - HudChrome.ascent(
                                                            type2.text(), type2.textPx()));
                            bl3 = true;
                        } else {
                            string = "Lyrics in a script the HUD font lacks";
                        }
                        break;
                    case PLAIN:
                        string = "No synced lyrics";
                        break;
                    case INSTRUMENTAL:
                        string = "Instrumental";
                        break;
                    case NOT_FOUND:
                        string = bl2 ? "No lyrics found" : "No saved lyrics";
                        break;
                    case UNAVAILABLE:
                        string = "Lyrics unavailable";
                }
            }
        }
        if (string != null) {
            f2 =
                    Math.round((type2.textPx() - type2.labelPx()) * 0.5F)
                            + HudChrome.ascent(type2.label(), type2.labelPx());
        }
        if (bl3) {
            this.lyricLastItems = (List<HudRenderer.LyricItem>) object;
            this.lyricLeaveItems = null;
        } else {
            object = this.leavingItems(string == null, type2, g, h);
        }
        float g2 = f2 < 0.0F ? this.lyricBlockH : (f2 > 0.0F ? 12.0F * g + Math.round(f2) : 0.0F);
        return new HudRenderer.LyricView(
                (List<HudRenderer.LyricItem>) object, string, this.easeLyricBlock(g2, h));
    }

    private List<HudRenderer.LyricItem> leavingItems(
            boolean bl, HudRenderer.Type type2, float f, float g) {
        this.lyricSource = null;
        if (!this.lyricLastItems.isEmpty()) {
            this.lyricLeaveItems = this.lyricLastItems;
            this.lyricLeave = 0.0F;
            this.lyricLastItems = List.of();
        }
        if (this.lyricLeaveItems == null) {
            return List.of();
        } else {
            this.lyricLeave = Math.min(1.0F, this.lyricLeave + g / 0.32F);
            if (bl && !(this.lyricLeave >= 1.0F)) {
                ArrayList list = new ArrayList(this.lyricLeaveItems.size());
                addFaded(list, this.lyricLeaveItems, smooth(this.lyricLeave), 8.0F * f);
                return list;
            } else {
                this.lyricLeaveItems = null;
                return List.of();
            }
        }
    }

    private static void addFaded(
            List<HudRenderer.LyricItem> list, List<HudRenderer.LyricItem> list2, float f, float g) {
        for (HudRenderer.LyricItem lyricItem : list2) {
            float h = lyricItem.alpha() * (1.0F - f);
            if (!(h <= 0.004F)) {
                list.add(
                        new HudRenderer.LyricItem(
                                lyricItem.text(),
                                lyricItem.top() - g * f,
                                lyricItem.highlight(),
                                h,
                                lyricItem.progress(),
                                lyricItem.fill()));
            }
        }
    }

    private void syncedItems(
            List<HudRenderer.LyricItem> list,
            SyncedLyrics lyricSource2,
            long l2,
            float f,
            SpotifyHudModule.LyricMotion lyricMotion,
            SpotifyHudModule.LyricHighlight lyricHighlight,
            HudRenderer.Type type2,
            float g,
            float h,
            float lyricClock2) {
        float f2 = 6.0F * h;
        int index = lyricSource2.indexAt(l2 + 200L);
        int firstWithTime = lyricSource2.firstWithTimeOf(index);
        int i = Math.max(0, firstWithTime);
        if (lyricSource2 != this.lyricSource) {
            this.lyricSource = lyricSource2;
            this.lyricStart = i;
            this.lyricShifts.clear();
            this.lyricClock = 0.0F;
            this.lyricFadeItems = null;
            this.lyricFade = 1.0F;
            this.lyricShown = new float[lyricSource2.size() * 2];
            this.lyricBright = new float[lyricSource2.size()];
            this.lyricBrightLo = 0;
            this.lyricBrightHi = -1;
            for (int n = Math.max(0, firstWithTime); n <= index; n++) {
                this.lyricBright[n] = 1.0F;
            }
            if (index >= 0) {
                this.lyricBrightLo = Math.max(0, firstWithTime);
                this.lyricBrightHi = index;
            }
        }
        this.lyricClock += lyricClock2;
        this.lyricBreath = (this.lyricBreath + lyricClock2) % 1.6F;
        this.lyricCurrentFirst = firstWithTime;
        this.lyricCurrentLast = index;
        float g2 = lyricMotion == SpotifyHudModule.LyricMotion.WAVE ? 0.045F : 0.0F;
        if (i != this.lyricStart) {
            float h2 =
                    i > this.lyricStart
                            ? this.restDistance(this.lyricStart, i, type2, g, h)
                            : -1.0F;
            if (lyricMotion != SpotifyHudModule.LyricMotion.FADE
                    && h2 > 0.0F
                    && h2 <= f + f2 + 0.5F) {
                if (this.lyricShifts.size() == 8) {
                    this.lyricShifts.remove(0);
                }
                this.lyricShifts.add(new HudRenderer.LyricShift(h2, this.lyricClock, i, g2));
            } else {
                this.lyricFadeItems = this.lyricLastItems;
                this.lyricFade = 0.0F;
                this.lyricShifts.clear();
                this.showView(lyricSource2, i, f, type2, g, h);
            }
            this.lyricStart = i;
        }
        this.updateBrightness(firstWithTime, index, lyricClock2);
        this.lyricFade = Math.min(1.0F, this.lyricFade + lyricClock2 / 0.32F);
        if (this.lyricFade >= 1.0F) {
            this.lyricFadeItems = null;
        }
        float f3 = 1.37F;
        this.lyricShifts.removeIf(lyricShift -> this.lyricClock - lyricShift.at() > f3);
        if (this.lyricShifts.isEmpty()) {
            this.lyricClock = 0.0F;
        }
        float g3 = 8.0F * h;
        if (this.lyricFadeItems != null) {
            float h3 = smooth(this.lyricFade);
            addFaded(list, this.lyricFadeItems, h3, g3);
            this.addLyricView(
                    list,
                    lyricSource2,
                    this.lyricStart,
                    h3,
                    g3 * (1.0F - h3),
                    f,
                    l2,
                    lyricHighlight,
                    type2,
                    g,
                    h,
                    lyricClock2);
        } else {
            this.addLyricView(
                    list,
                    lyricSource2,
                    this.lyricStart,
                    1.0F,
                    0.0F,
                    f,
                    l2,
                    lyricHighlight,
                    type2,
                    g,
                    h,
                    lyricClock2);
        }
    }

    private void addLyricView(
            List<HudRenderer.LyricItem> list,
            SyncedLyrics syncedLyrics,
            int i,
            float f,
            float g,
            float h,
            long l2,
            SpotifyHudModule.LyricHighlight lyricHighlight,
            HudRenderer.Type type2,
            float f2,
            float g2,
            float h2) {
        ImFont imFont = type2.text();
        int textPx2 = type2.textPx();
        float f3 = 6.0F * g2;
        float g3 = 6.0F * g2;
        float h3 = textPx2 + g3;
        boolean bl = lyricHighlight == SpotifyHudModule.LyricHighlight.KARAOKE;
        float f4 = 0.0F;
        for (int o = i; o < syncedLyrics.size() && f4 <= h + h3 * 2.0F; o++) {
            String[] strings = this.rowsFor(o, imFont, textPx2, f2);
            int j = Math.max(1, strings.length);
            float g4 = f4 + this.lyricOffset(o) + g;
            float h4 = 1.0F - 0.39999998F * clamp01(g4 / h3 - 1.0F);
            float lyricGlow = this.lyricGlow(o, lyricHighlight);
            float f5 = strings.length == 0 ? this.breakProgress(syncedLyrics, o, l2) : 1.0F;
            for (int q = 0; q < j; q++) {
                float g5 = f4 + q * (textPx2 + f3);
                float updateShown = this.updateShown(o, q, g5 + textPx2 <= h + 0.5F, h2);
                float h5 = g4 + q * (textPx2 + f3);
                float f6 =
                        f
                                * h4
                                * smooth(updateShown)
                                * topFade(h5, g2)
                                * bottomFade(h5 + textPx2 - h, textPx2);
                if (!(f6 <= 0.004F)) {
                    float g6 =
                            bl && strings.length > 0
                                    ? this.rowFill(syncedLyrics, o, strings, q, imFont, textPx2, l2)
                                    : 0.0F;
                    list.add(
                            new HudRenderer.LyricItem(
                                    strings.length == 0 ? null : strings[q],
                                    h5,
                                    lyricGlow,
                                    f6,
                                    f5,
                                    g6));
                }
            }
            f4 += lineHeight(strings, textPx2, f3) + g3;
        }
        float h6 = 0.0F;
        for (int u = i - 1; u >= 0; u--) {
            String[] strings2 = this.rowsFor(u, imFont, textPx2, f2);
            int k = Math.max(1, strings2.length);
            float f7 = (h6 -= lineHeight(strings2, textPx2, f3) + g3) + this.lyricOffset(u) + g;
            if (topFade(f7 + (k - 1) * (textPx2 + f3), g2) <= 0.0F) {
                break;
            }
            float lyricGlow2 = this.lyricGlow(u, lyricHighlight);
            float g7 = strings2.length == 0 ? this.breakProgress(syncedLyrics, u, l2) : 1.0F;
            for (int w = 0; w < k; w++) {
                float h7 = f7 + w * (textPx2 + f3);
                float f8 = f * smooth(this.lyricShown[u * 2 + w]) * topFade(h7, g2);
                if (!(f8 <= 0.004F)) {
                    float g8 =
                            bl && strings2.length > 0
                                    ? this.rowFill(
                                            syncedLyrics, u, strings2, w, imFont, textPx2, l2)
                                    : 0.0F;
                    list.add(
                            new HudRenderer.LyricItem(
                                    strings2.length == 0 ? null : strings2[w],
                                    h7,
                                    lyricGlow2,
                                    f8,
                                    g7,
                                    g8));
                }
            }
        }
    }

    private float lyricOffset(int i) {
        float f = 0.0F;
        for (int j = 0; j < this.lyricShifts.size(); j++) {
            HudRenderer.LyricShift lyricShift = this.lyricShifts.get(j);
            float g = Math.max(0, Math.min(6, i - lyricShift.start())) * lyricShift.stagger();
            float h = this.lyricClock - lyricShift.at() - g;
            f += h <= 0.0F ? lyricShift.distance() : lyricShift.distance() * springLeft(h);
        }
        return f;
    }

    private static float springLeft(float f) {
        float g = 13.0F * f;
        return (1.0F + g) * (float) Math.exp(-g);
    }

    private static float topFade(float f, float g) {
        return f >= 0.0F ? 1.0F : smooth(1.0F + f / (12.0F * g * 0.75F));
    }

    private static float bottomFade(float f, int i) {
        return f <= 0.5F ? 1.0F : smooth(1.0F - f / (i * 0.6F));
    }

    private float updateShown(int i, int j, boolean bl, float f) {
        int k = i * 2 + j;
        float g = f / 0.3F;
        float h;
        this.lyricShown[k] =
                h =
                        bl
                                ? Math.min(1.0F, this.lyricShown[k] + g)
                                : Math.max(0.0F, this.lyricShown[k] - g);
        return h;
    }

    private void showView(
            SyncedLyrics syncedLyrics, int i, float f, HudRenderer.Type type2, float g, float h) {
        Arrays.fill(this.lyricShown, 0.0F);
        int textPx2 = type2.textPx();
        float f2 = 6.0F * h;
        float g2 = 0.0F;
        for (int k = i; k < syncedLyrics.size() && g2 < f; k++) {
            String[] strings = this.rowsFor(k, type2.text(), textPx2, g);
            int j = Math.max(1, strings.length);
            for (int n = 0; n < j; n++) {
                if (g2 + n * (textPx2 + f2) + textPx2 <= f + 0.5F) {
                    this.lyricShown[k * 2 + n] = 1.0F;
                }
            }
            g2 += lineHeight(strings, textPx2, f2) + 6.0F * h;
        }
    }

    private float restDistance(int i, int j, HudRenderer.Type type2, float f, float g) {
        float h = 6.0F * g;
        float f2 = 0.0F;
        for (int k = i; k < j; k++) {
            f2 +=
                    lineHeight(this.rowsFor(k, type2.text(), type2.textPx(), f), type2.textPx(), h)
                            + 6.0F * g;
        }
        return f2;
    }

    private void updateBrightness(int i, int j, float f) {
        int k = this.lyricBrightHi >= 0 ? this.lyricBrightLo : 2147483647;
        int lyricBrightHi2 = this.lyricBrightHi;
        if (j >= 0) {
            k = Math.min(k, Math.max(0, i));
            lyricBrightHi2 = Math.max(lyricBrightHi2, j);
        }
        float g = f / 0.35F;
        int m = 2147483647;
        int n = -1;
        for (int p = Math.max(0, k); p <= lyricBrightHi2 && p < this.lyricBright.length; p++) {
            boolean bl = p >= i && p <= j;
            float h;
            this.lyricBright[p] =
                    h =
                            bl
                                    ? Math.min(1.0F, this.lyricBright[p] + g)
                                    : Math.max(0.0F, this.lyricBright[p] - g);
            if (h > 0.0F) {
                m = Math.min(m, p);
                n = p;
            }
        }
        this.lyricBrightLo = n < 0 ? 0 : m;
        this.lyricBrightHi = n;
    }

    private float lyricGlow(int i, SpotifyHudModule.LyricHighlight lyricHighlight) {
        if (lyricHighlight != SpotifyHudModule.LyricHighlight.OFF
                && i >= 0
                && i < this.lyricCurrentFirst) {
            return 1.0F;
        } else {
            return i >= 0 && i < this.lyricBright.length ? smooth(this.lyricBright[i]) : 0.0F;
        }
    }

    private float breakProgress(SyncedLyrics syncedLyrics, int i, long l2) {
        if (i >= this.lyricCurrentFirst && i <= this.lyricCurrentLast) {
            int j = syncedLyrics.lastWithTimeOf(i) + 1;
            if (j >= syncedLyrics.size()) {
                return 1.0F;
            } else {
                long timeMs = syncedLyrics.line(i).timeMs();
                long l3 = syncedLyrics.line(j).timeMs() - 200L;
                return l3 <= timeMs ? 1.0F : clamp01((float) (l2 - timeMs) / (float) (l3 - timeMs));
            }
        } else {
            return 1.0F;
        }
    }

    private float rowFill(
            SyncedLyrics syncedLyrics,
            int i,
            String[] strings,
            int j,
            ImFont imFont,
            int k,
            long l2) {
        if (i < this.lyricCurrentFirst) {
            return 1.0F / 0.0F;
        } else if (i > this.lyricCurrentLast) {
            return 0.0F;
        } else {
            String string = strings[j];
            int m = 0;
            if (j > 0) {
                String lyricText = this.lyricTexts[i];
                m = strings[0].length();
                while (m < lyricText.length() && lyricText.charAt(m) == ' ') {
                    m++;
                }
            }
            float f;
            if ((f = this.litChars(syncedLyrics, i, l2) - m) <= 0.0F) {
                return 0.0F;
            } else if (f >= string.length()) {
                return 1.0F / 0.0F;
            } else {
                int n = (int) f;
                if (n > 0 && Character.isLowSurrogate(string.charAt(n))) {
                    n--;
                }
                int o = n + Character.charCount(string.codePointAt(n));
                float width2 = HudChrome.width(imFont, k, string.substring(0, n));
                float width3 = HudChrome.width(imFont, k, string.substring(0, o));
                return width2 + (width3 - width2) * clamp01((f - n) / (o - n));
            }
        }
    }

    private float litChars(SyncedLyrics syncedLyrics, int i, long l2) {
        HudRenderer.LyricWords lyricWord = this.lyricWords[i];
        if (lyricWord == null) {
            this.timeWords(syncedLyrics, syncedLyrics.firstWithTimeOf(i));
            lyricWord = this.lyricWords[i];
        }
        float f = 0.0F;
        long[] longs = lyricWord.start();
        for (int j = 0; j < longs.length && l2 >= longs[j]; j++) {
            float g = smooth((float) (l2 - longs[j]) / (float) lyricWord.sweep()[j]);
            f = lyricWord.from()[j] + (lyricWord.to()[j] - lyricWord.from()[j]) * g;
        }
        return f;
    }

    private void timeWords(SyncedLyrics syncedLyrics, int i) {
        int lastWithTime = syncedLyrics.lastWithTimeOf(i);
        int j = lastWithTime + 1;
        long timeMs = syncedLyrics.line(i).timeMs();
        long l2 =
                j < syncedLyrics.size()
                        ? syncedLyrics.line(j).timeMs() - 200L
                        : 9223372036854775807L;
        int k = 0;
        for (int n = i; n <= lastWithTime; n++) {
            k += letterCount(this.lyricTexts[n]);
        }
        long l3 =
                j < syncedLyrics.size()
                        ? Math.min(l2 - timeMs, (long) Math.round(k * this.lyricCharMs * 2.0F))
                        : Math.round(k * this.lyricCharMs);
        l3 = Math.max(0L, l3);
        int m = 0;
        for (int p = i; p <= lastWithTime; p++) {
            String lyricText = this.lyricTexts[p];
            int o = lyricText.isEmpty() ? 0 : lyricText.length() - letterCount(lyricText) + 1;
            int[] ints = new int[o];
            int[] ints2 = new int[o];
            long[] longs = new long[o];
            long[] longs2 = new long[o];
            List list = syncedLyrics.line(p).wordTimes();
            boolean bl = list.size() == o;
            int q = 0;
            for (int v = 0; v < o; v++) {
                ints[v] = q;
                if ((q = lyricText.indexOf(32, q)) < 0) {
                    q = lyricText.length();
                }
                ints2[v] = q++;
                long l4 = bl ? (Long) list.get(v) : timeMs + l3 * m / k;
                long l5 =
                        bl
                                ? (v + 1 < o ? (Long) list.get(v + 1) : Math.min(l2, l4 + 180L))
                                : timeMs + l3 * (m += ints2[v] - ints[v]) / k;
                longs[v] = l4;
                longs2[v] = Math.max(1L, Math.min(180L, l5 - l4));
            }
            this.lyricWords[p] = new HudRenderer.LyricWords(ints, ints2, longs, longs2);
        }
    }

    private float lyricPace(SyncedLyrics syncedLyrics) {
        float[] floats = new float[syncedLyrics.size()];
        int i = 0;
        int j = 0;
        while (j < syncedLyrics.size()) {
            int lastWithTime = syncedLyrics.lastWithTimeOf(j);
            int k = 0;
            for (int n = j; n <= lastWithTime; n++) {
                k += letterCount(this.lyricTexts[n]);
            }
            if (k > 0 && lastWithTime + 1 < syncedLyrics.size()) {
                floats[i++] =
                        (float)
                                        (syncedLyrics.line(lastWithTime + 1).timeMs()
                                                - syncedLyrics.line(j).timeMs())
                                / k;
            }
            j = lastWithTime + 1;
        }
        if (i == 0) {
            return 90.0F;
        } else {
            Arrays.sort(floats, 0, i);
            return floats[i / 2];
        }
    }

    private static int letterCount(String string) {
        int i = string.length();
        for (int j = 0; j < string.length(); j++) {
            if (string.charAt(j) == ' ') {
                i--;
            }
        }
        return i;
    }

    private float lyricViewHeight(
            SyncedLyrics syncedLyrics,
            int lyricViewLines2,
            HudRenderer.Type type2,
            float f,
            float lyricViewScale2) {
        int textPx2 = type2.textPx();
        this.prepareRows(textPx2, f);
        if (this.lyricViewH >= 0.0F
                && this.lyricViewLines == lyricViewLines2
                && this.lyricViewScale == lyricViewScale2) {
            return this.lyricViewH;
        } else {
            int i = lyricViewLines2;
            int j = 0;
            while (j < syncedLyrics.size()) {
                int k = 0;
                int lastWithTime = syncedLyrics.lastWithTimeOf(j);
                for (int p = j; p <= lastWithTime; p++) {
                    k += Math.max(1, this.rowsFor(p, type2.text(), textPx2, f).length);
                }
                i = Math.max(i, Math.min(4, k));
                j = syncedLyrics.lastWithTimeOf(j) + 1;
            }
            this.lyricViewH = i * textPx2 + (i - 1) * 6.0F * lyricViewScale2;
            this.lyricViewLines = lyricViewLines2;
            this.lyricViewScale = lyricViewScale2;
            return this.lyricViewH;
        }
    }

    private static float lineHeight(String[] strings, int i, float f) {
        return strings.length <= 1 ? i : strings.length * i + (strings.length - 1) * f;
    }

    private float easeLyricBlock(float lyricBlockH2, float f) {
        float g = lyricBlockH2 - this.lyricBlockH;
        if (Math.abs(g) < 0.5F) {
            this.lyricBlockH = lyricBlockH2;
        } else {
            float h = g > 0.0F ? 14.0F : 9.0F;
            this.lyricBlockH = this.lyricBlockH + g * (1.0F - (float) Math.exp(-h * f));
        }
        return this.lyricBlockH;
    }

    private void prepareLyrics(Lyrics lyricPrepared2, ImFont imFont) {
        if (lyricPrepared2 != this.lyricPrepared) {
            this.lyricPrepared = lyricPrepared2;
            this.lyricViewH = -1.0F;
            SyncedLyrics syncedLyrics = lyricPrepared2.synced();
            this.lyricTexts = new String[syncedLyrics.size()];
            this.lyricRows = new String[syncedLyrics.size()][];
            this.lyricWords = new HudRenderer.LyricWords[syncedLyrics.size()];
            int j = 0;
            int k = 0;
            IntPredicate intPredicate = i -> this.hudHasGlyph(imFont, i);
            for (int m = 0; m < syncedLyrics.size(); m++) {
                String fold;
                this.lyricTexts[m] =
                        fold = DisplayText.fold(syncedLyrics.line(m).text(), intPredicate);
                int n = 0;
                while (n < fold.length()) {
                    int o = fold.codePointAt(n);
                    n += Character.charCount(o);
                    if (Character.isLetter(o)) {
                        j++;
                        if (o >= 128 && !this.hudHasGlyph(imFont, o)) {
                            k++;
                        }
                    }
                }
            }
            this.lyricReadable = k <= j * 0.25F;
            this.lyricCharMs = this.lyricPace(syncedLyrics);
        }
    }

    private boolean hudHasGlyph(ImFont glyphFont2, int i) {
        if (i < 128 || glyphFont2 == null) {
            return true;
        } else if (i > 65535) {
            return false;
        } else {
            if (glyphFont2 != this.glyphFont) {
                this.glyphFont = glyphFont2;
                this.glyphs.clear();
            }
            Boolean glyph2;
            if ((glyph2 = this.glyphs.get(i)) == null) {
                if (this.glyphs.size() > 1024) {
                    this.glyphs.clear();
                }
                glyph2 = glyphFont2.findGlyphNoFallback(i).isValidPtr();
                this.glyphs.put(i, glyph2);
            }
            return glyph2;
        }
    }

    private void prepareRows(int lyricRowsPx2, float f) {
        int i = (int) f;
        if (lyricRowsPx2 != this.lyricRowsPx || i != this.lyricRowsWidth) {
            Arrays.fill((Object[]) this.lyricRows, null);
            this.lyricRowsPx = lyricRowsPx2;
            this.lyricRowsWidth = i;
            this.lyricViewH = -1.0F;
        }
    }

    private String[] rowsFor(int i, ImFont imFont, int j, float f) {
        this.prepareRows(j, f);
        String[] lyricRow = this.lyricRows[i];
        if (lyricRow == null) {
            lyricRow = wrapLyric(imFont, j, this.lyricTexts[i], this.lyricRowsWidth);
            this.lyricRows[i] = lyricRow;
        }
        return lyricRow;
    }

    private static String[] wrapLyric(ImFont imFont, int i, String string, float f) {
        if (string.isEmpty()) {
            return NO_ROWS;
        } else if (HudChrome.width(imFont, i, string) <= f) {
            return new String[] {string};
        } else {
            int j = -1;
            float g = 3.4028235E38F;
            float width2;
            for (int k = string.indexOf(32);
                    k > 0
                            && !((width2 =
                                            HudChrome.width(
                                                    imFont,
                                                    i,
                                                    string.substring(0, k).stripTrailing()))
                                    > f);
                    k = string.indexOf(32, k + 1)) {
                float width3 = HudChrome.width(imFont, i, string.substring(k + 1).stripLeading());
                if (width3 <= f && Math.max(width2, width3) < g) {
                    g = Math.max(width2, width3);
                    j = k;
                }
            }
            if (j > 0) {
                return new String[] {
                    string.substring(0, j).stripTrailing(), string.substring(j + 1).stripLeading()
                };
            } else {
                int fitPrefix = fitPrefix(imFont, i, string, f);
                String stripTrailing = string.substring(0, fitPrefix).stripTrailing();
                String stripLeading = string.substring(fitPrefix).stripLeading();
                return stripLeading.isEmpty()
                        ? new String[] {stripTrailing}
                        : new String[] {
                            stripTrailing,
                            HudChrome.width(imFont, i, stripLeading) <= f
                                    ? stripLeading
                                    : ellipsize(imFont, i, stripLeading, f)
                        };
            }
        }
    }

    private static int fitPrefix(ImFont imFont, int i, String string, float f) {
        int j = -1;
        for (int k = string.indexOf(32);
                k > 0 && !(HudChrome.width(imFont, i, string.substring(0, k)) > f);
                k = string.indexOf(32, k + 1)) {
            j = k;
        }
        if (j > 0) {
            return j + 1;
        } else {
            int m = 1;
            int n = string.length();
            while (m < n) {
                int o = m + n + 1 >>> 1;
                if (HudChrome.width(imFont, i, string.substring(0, o)) <= f) {
                    m = o;
                } else {
                    n = o - 1;
                }
            }
            if (m > 1 && m < string.length() && Character.isHighSurrogate(string.charAt(m - 1))) {
                m--;
            }
            return m;
        }
    }

    private void drawLyrics(
            ImDrawList imDrawList,
            HudRenderer.Type type2,
            HudRenderer.LyricView lyricView,
            HudRenderer.SpotifyColors spotifyColors,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            boolean bl) {
        float f3 = g - 12.0F * g2 + Math.max(1.0F, 2.0F * g2);
        if (!(f2 <= f3) && !(h2 <= 0.001F)) {
            float g3 = (float) Math.floor(f - 2.0F);
            float h3 = (float) Math.ceil(f + h + 2.0F);
            imDrawList.pushClipRect(g3, f3, h3, f2, true);
            if (lyricView.status() != null) {
                float f4 = Math.round(g + (type2.textPx() - type2.labelPx()) * 0.5F);
                HudChrome.text(
                        imDrawList,
                        type2.label(),
                        type2.labelPx(),
                        f,
                        f4,
                        HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, h2 * 0.85F),
                        lyricView.status(),
                        bl);
            }
            for (HudRenderer.LyricItem item2 : lyricView.items()) {
                float g4 = h2 * item2.alpha();
                if (!(g4 <= 0.004F)) {
                    float h4 = g + item2.top();
                    if (item2.text() == null) {
                        float f5 =
                                h4
                                        + (this.capTop(type2.text(), type2.textPx())
                                                        + HudChrome.ascent(
                                                                type2.text(), type2.textPx()))
                                                * 0.5F;
                        this.drawLyricBreak(
                                imDrawList,
                                spotifyColors,
                                f,
                                f5,
                                g2,
                                item2.highlight(),
                                item2.progress(),
                                g4);
                    } else {
                        int scaleAlpha =
                                HudChrome.scaleAlpha(
                                        EfnTheme.lerpColor(
                                                spotifyColors.upcoming(),
                                                spotifyColors.lyric(),
                                                item2.highlight()),
                                        g4);
                        int scaleAlpha2 = HudChrome.scaleAlpha(spotifyColors.lit(), g4);
                        float g5 = Math.min(h3, (float) Math.floor(f) + Math.round(item2.fill()));
                        if (item2.fill() <= 0.0F) {
                            HudChrome.text(
                                    imDrawList,
                                    type2.text(),
                                    type2.textPx(),
                                    f,
                                    Math.round(h4),
                                    scaleAlpha,
                                    item2.text(),
                                    bl);
                        } else if (g5 >= h3) {
                            HudChrome.text(
                                    imDrawList,
                                    type2.text(),
                                    type2.textPx(),
                                    f,
                                    Math.round(h4),
                                    scaleAlpha2,
                                    item2.text(),
                                    bl);
                        } else {
                            imDrawList.pushClipRect(g3, f3, g5, f2, true);
                            HudChrome.text(
                                    imDrawList,
                                    type2.text(),
                                    type2.textPx(),
                                    f,
                                    Math.round(h4),
                                    scaleAlpha2,
                                    item2.text(),
                                    bl);
                            imDrawList.popClipRect();
                            imDrawList.pushClipRect(g5, f3, h3, f2, true);
                            HudChrome.text(
                                    imDrawList,
                                    type2.text(),
                                    type2.textPx(),
                                    f,
                                    Math.round(h4),
                                    scaleAlpha,
                                    item2.text(),
                                    bl);
                            imDrawList.popClipRect();
                        }
                    }
                }
            }
            imDrawList.popClipRect();
        }
    }

    private void drawLyricBreak(
            ImDrawList imDrawList,
            HudRenderer.SpotifyColors spotifyColors,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2) {
        float f3 = Math.max(1.0F, 1.6F * h);
        float g3 = Math.max(4.0F, (float) Math.round(6.0F * h));
        float h3 =
                0.5F - 0.5F * (float) Math.cos(this.lyricBreath / 1.6F * 2.0 * 3.141592653589793);
        for (int m = 0; m < 3; m++) {
            float f4 = smooth(g2 * 3.0F - m) * f2;
            int scaleAlpha =
                    HudChrome.scaleAlpha(
                            EfnTheme.lerpColor(spotifyColors.upcoming(), spotifyColors.lit(), f4),
                            h2);
            float g4 = f3 * (1.0F + 0.3F * f4 + 0.12F * h3 * f2);
            imDrawList.addCircleFilled(f + f3 + m * g3, g, g4, scaleAlpha, 12);
        }
    }

    private void foldTrack(NowPlaying spotifyFolded2, ImFont imFont) {
        if (spotifyFolded2 != this.spotifyFolded) {
            this.spotifyFolded = spotifyFolded2;
            IntPredicate intPredicate = i -> this.hudHasGlyph(imFont, i);
            String fold = DisplayText.fold(spotifyFolded2.title(), intPredicate);
            this.spotifyTitle = fold.isEmpty() ? spotifyFolded2.title() : fold;
            this.spotifyArtist = DisplayText.fold(spotifyFolded2.artist(), intPredicate);
        }
    }

    private float[] marquee(
            String marqueeTitle2,
            String marqueeArtist2,
            float f,
            float g,
            float h,
            boolean bl,
            boolean bl2,
            float f2,
            float g2,
            float h2) {
        if (!marqueeTitle2.equals(this.marqueeTitle)
                || !marqueeArtist2.equals(this.marqueeArtist)) {
            this.marqueeTitle = marqueeTitle2;
            this.marqueeArtist = marqueeArtist2;
            this.marqueeTime = 0.0F;
        }
        float f3 = 0.0F;
        float g3 = 0.0F;
        float h3 = Math.max(f > h ? f : 0.0F, g > h ? g : 0.0F);
        if (bl && h3 > 0.0F) {
            float f4 = 30.0F * g2;
            float g4 = 2.4F + (h3 + f2) / f4;
            this.marqueeTime %= g4;
            if (bl2 || this.marqueeTime > 2.4F) {
                this.marqueeTime = (this.marqueeTime + h2) % g4;
            }
            float h4 = Math.max(0.0F, this.marqueeTime - 2.4F) * f4;
            if (f > h) {
                f3 = Math.min(h4, f + f2);
            }
            if (g > h) {
                g3 = Math.min(h4, g + f2);
            }
        } else {
            this.marqueeTime = 0.0F;
        }
        return new float[] {f3, g3};
    }

    private static void marqueeLine(
            ImDrawList imDrawList,
            ImFont imFont,
            int i,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            boolean bl,
            String string,
            int j,
            boolean bl2) {
        if (f2 <= h + 0.5F) {
            HudChrome.text(imDrawList, imFont, i, f, g, j, string, bl2);
        } else if (!bl) {
            HudChrome.text(imDrawList, imFont, i, f, g, j, ellipsize(imFont, i, string, h), bl2);
        } else {
            imDrawList.pushClipRect(f, g - 2.0F, f + h, g + i + 3.0F, true);
            HudChrome.text(imDrawList, imFont, i, f - g2, g, j, string, bl2);
            if (g2 > 0.0F) {
                HudChrome.text(imDrawList, imFont, i, f - g2 + f2 + h2, g, j, string, bl2);
            }
            imDrawList.popClipRect();
        }
    }

    private static String ellipsize(ImFont imFont, int i, String string, float f) {
        float g = f - HudChrome.width(imFont, i, "...");
        int j = 0;
        int k = string.length();
        while (j < k) {
            int m = j + k + 1 >>> 1;
            if (HudChrome.width(imFont, i, string.substring(0, m)) <= g) {
                j = m;
            } else {
                k = m - 1;
            }
        }
        return string.substring(0, j).stripTrailing() + "...";
    }

    private static String formatSeconds(long l2) {
        long l3 = Math.max(0L, l2);
        long l4 = l3 / 3600L;
        long l5 = l3 / 60L % 60L;
        StringBuilder sb = new StringBuilder(8);
        if (l4 > 0L) {
            sb.append(l4).append(':').append(l5 < 10L ? "0" : "");
        }
        sb.append(l5).append(':');
        if (l3 % 60L < 10L) {
            sb.append('0');
        }
        return sb.append(l3 % 60L).toString();
    }

    private float capTop(ImFont imFont, int i) {
        if (imFont != null && !(imFont.getFontSize() <= 0.0F)) {
            long l2 = (long) System.identityHashCode(imFont) << 16 ^ i;
            Float capTop = this.capTops.get(l2);
            if (capTop != null) {
                return capTop;
            } else {
                ImFontGlyph glyphNoFallback = imFont.findGlyphNoFallback(72);
                float f =
                        glyphNoFallback.isValidPtr()
                                ? glyphNoFallback.getY0() * i / imFont.getFontSize()
                                : i * 0.2F;
                float g = Math.max(0.0F, (float) Math.round(f));
                if (this.capTops.size() > 64) {
                    this.capTops.clear();
                }
                this.capTops.put(l2, g);
                return g;
            }
        } else {
            return Math.round(i * 0.2F);
        }
    }

    private ThemeImages.Texture spotifyArtTexture(AlbumArt spotifyArt2) {
        if (spotifyArt2 == this.spotifyArt) {
            return this.spotifyArtTexture;
        } else {
            this.releaseSpotifyArt();
            if (spotifyArt2 == null) {
                return null;
            } else {
                this.spotifyArt = spotifyArt2;
                ByteBuffer byteBuffer = MemoryUtil.memAlloc(spotifyArt2.byteSize());
                try {
                    spotifyArt2.copyTo(byteBuffer);
                    byteBuffer.flip();
                    this.spotifyArtTexture =
                            ThemeImages.fromPixels(
                                    byteBuffer, spotifyArt2.size(), spotifyArt2.size(), false);
                } catch (RuntimeException exception) {
                    this.spotifyArtTexture = null;
                } finally {
                    MemoryUtil.memFree(byteBuffer);
                }
                return this.spotifyArtTexture;
            }
        }
    }

    private void releaseSpotifyArt() {
        if (this.spotifyArtTexture != null) {
            GL11.glDeleteTextures(this.spotifyArtTexture.id());
        }
        this.spotifyArtTexture = null;
        this.spotifyArt = null;
    }

    private List<HudRenderer.PotionRow> buildPotionRows(MinecraftClient client) {
        if (client.player == null) {
            return List.of();
        } else {
            ArrayList list = new ArrayList();
            for (StatusEffectInstance statusEffect : client.player.getStatusEffects()) {
                RegistryEntry effectType = statusEffect.getEffectType();
                String translate =
                        I18n.translate(((StatusEffect) effectType.value()).getTranslationKey());
                int amplifier2 = statusEffect.getAmplifier();
                String string = amplifier2 > 0 ? toRomanNumeral(amplifier2 + 1) : "";
                int duration2 = statusEffect.getDuration();
                if (duration2 >= 0 && duration2 != 2147483647 && !statusEffect.isInfinite()) {
                    int i = Math.max(0, duration2 / 20);
                    String string2 = i / 60 + ":" + String.format("%02d", i % 60);
                    list.add(
                            new HudRenderer.PotionRow(
                                    effectType,
                                    translate,
                                    string,
                                    string2,
                                    false,
                                    i < 30,
                                    duration2));
                } else {
                    list.add(
                            new HudRenderer.PotionRow(
                                    effectType, translate, string, "", true, false, 2147483647));
                }
            }
            return list;
        }
    }

    private static String toRomanNumeral(int i) {
        return switch (i) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(i);
        };
    }

    private int hudAccent() {
        return this.hudModule != null && this.hudModule.customAccent()
                ? GuiPalette.abgr(this.hudModule.accentArgb())
                : GuiPalette.accent();
    }

    static Surface hudSurface(HudLayoutStore.HudElementState hudElementState) {
        return HudTheme.get().forElementId(hudElementState.id);
    }

    static float hudRounding() {
        return HudTheme.get().rounding.get();
    }

    private void updateEditorInput(float f, float g) {
        float mousePosX = ImGui.getMousePosX();
        float mousePosY = ImGui.getMousePosY();
        if (this.draggingId != null) {
            if (ImGui.isMouseDown(0)) {
                HudLayoutStore.HudElementState hudElementState =
                        this.layout.element(this.draggingId);
                float h = mousePosX - this.dragOffsetX;
                float f2 = mousePosY - this.dragOffsetY;
                HudRenderer.Snap snap =
                        bestSnap(h, hudElementState.width, this.snapAnchorsX(this.draggingId, f));
                HudRenderer.Snap snap2 =
                        bestSnap(f2, hudElementState.height, this.snapAnchorsY(this.draggingId, g));
                this.snapGuideX = snap != null ? snap.guideAt() : null;
                this.snapGuideY = snap2 != null ? snap2.guideAt() : null;
                this.layout.moveTo(
                        this.draggingId,
                        snap != null ? snap.position() : h,
                        snap2 != null ? snap2.position() : f2,
                        f,
                        g);
            } else {
                this.draggingId = null;
                this.snapGuideX = null;
                this.snapGuideY = null;
                this.saveLayout();
            }
        } else {
            int spotifyButton;
            if (this.chatInput
                    && this.spotifyButtonsShown
                    && ImGui.isMouseClicked(0)
                    && (spotifyButton = this.spotifyButtonAt(mousePosX, mousePosY)) >= 0) {
                SpotifyHudModule.hudButton(SPOTIFY_BUTTONS[spotifyButton]);
            } else if (!ImGui.isAnyItemHovered()) {
                float mouseWheel = ImGui.getIO().getMouseWheel();
                if (mouseWheel != 0.0F) {
                    List list = this.layout.orderedElements();
                    for (int j = list.size() - 1; j >= 0; j--) {
                        HudLayoutStore.HudElementState hudElementState2 =
                                (HudLayoutStore.HudElementState) list.get(j);
                        if (contains(hudElementState2, mousePosX, mousePosY)) {
                            float g2 =
                                    Math.max(
                                            0.5F,
                                            Math.min(
                                                    3.0F,
                                                    hudElementState2.scale + mouseWheel * 0.05F));
                            if (g2 != hudElementState2.scale) {
                                hudElementState2.scale = g2;
                                this.layoutDirty = true;
                                this.lastWheelNanos = System.nanoTime();
                            }
                            break;
                        }
                    }
                }
                if (ImGui.isMouseClicked(0)) {
                    List list2 = this.layout.orderedElements();
                    for (int k = list2.size() - 1; k >= 0; k--) {
                        HudLayoutStore.HudElementState hudElementState3 =
                                (HudLayoutStore.HudElementState) list2.get(k);
                        if (contains(hudElementState3, mousePosX, mousePosY)) {
                            this.draggingId = hudElementState3.id;
                            this.dragOffsetX = mousePosX - hudElementState3.x;
                            this.dragOffsetY = mousePosY - hudElementState3.y;
                            break;
                        }
                    }
                }
            }
        }
    }

    private List<Float> snapAnchorsX(String string, float f) {
        ArrayList list = new ArrayList<>(List.of(0.0F, f, f * 0.5F));
        for (HudLayoutStore.HudElementState orderedElement : this.layout.orderedElements()) {
            if (!orderedElement.id.equals(string) && orderedElement.positioned) {
                list.add(orderedElement.x);
                list.add(orderedElement.x + orderedElement.width);
                list.add(orderedElement.x + orderedElement.width * 0.5F);
            }
        }
        return list;
    }

    private List<Float> snapAnchorsY(String string, float f) {
        ArrayList list = new ArrayList<>(List.of(0.0F, f, f * 0.5F));
        for (HudLayoutStore.HudElementState orderedElement : this.layout.orderedElements()) {
            if (!orderedElement.id.equals(string) && orderedElement.positioned) {
                list.add(orderedElement.y);
                list.add(orderedElement.y + orderedElement.height);
                list.add(orderedElement.y + orderedElement.height * 0.5F);
            }
        }
        return list;
    }

    private static HudRenderer.Snap bestSnap(float f, float g, List<Float> list) {
        float[] floats = new float[] {0.0F, g * 0.5F, g};
        float h = 6.0F;
        HudRenderer.Snap snap = null;
        for (float f2 : list) {
            for (float g2 : floats) {
                float h2 = Math.abs(f + g2 - f2);
                if (h2 < h) {
                    h = h2;
                    snap = new HudRenderer.Snap(f2 - g2, f2);
                }
            }
        }
        return snap;
    }

    private void drawSnapGuides(ImDrawList imDrawList, float f, float g) {
        if (this.draggingId != null) {
            int scaleAlpha = HudChrome.scaleAlpha(this.hudAccent(), 0.8F);
            if (this.snapGuideX != null) {
                imDrawList.addLine(this.snapGuideX, 0.0F, this.snapGuideX, g, scaleAlpha, 1.0F);
            }
            if (this.snapGuideY != null) {
                imDrawList.addLine(0.0F, this.snapGuideY, f, this.snapGuideY, scaleAlpha, 1.0F);
            }
        }
    }

    private void drawEditorChrome(ImDrawList imDrawList, float f, float g) {
        float mousePosX = ImGui.getMousePosX();
        float mousePosY = ImGui.getMousePosY();
        float uiScale = ImGuiManager.getInstance().uiScale();
        ImFont imFont = ImGuiManager.getInstance().hudLabelFont();
        int px = HudChrome.px(14.0F, uiScale);
        this.drawSnapGuides(imDrawList, f, g);
        float h = Math.round(Math.max(2.0F, 3.0F * uiScale));
        List list = this.layout.orderedElements();
        ArrayList list2 = new ArrayList(list.size());
        for (HudLayoutStore.HudElementState hudElementState :
                (Iterable<HudLayoutStore.HudElementState>) list) {
            boolean bl = hudElementState.id.equals(this.draggingId);
            boolean bl2 = contains(hudElementState, mousePosX, mousePosY);
            float f2 = bl ? 1.0F : (bl2 ? 0.85F : 0.4F);
            float g2 = Math.round(hudElementState.x);
            float h2 = Math.round(hudElementState.y);
            imDrawList.addRect(
                    g2 - h,
                    h2 - h,
                    g2 + hudElementState.width + h,
                    h2 + hudElementState.height + h,
                    HudChrome.scaleAlpha(this.hudAccent(), f2),
                    hudRounding() * scaleOf(hudElementState) + h,
                    0,
                    bl ? 1.5F : 1.0F);
            this.drawEditorLabel(
                    imDrawList, imFont, px, uiScale, hudElementState, bl || bl2, list, list2, f, g);
        }
        drawEditorHint(imDrawList, imFont, px, uiScale, f, g);
    }

    private static void drawEditorHint(
            ImDrawList imDrawList, ImFont imFont, int i, float f, float g, float h) {
        String string = "Drag to move  \u00b7  Scroll to resize  \u00b7  Click the eye to hide";
        float f2 = Math.round(HudChrome.width(imFont, i, string) + 12.0F * f * 2.0F);
        float g2 = Math.round(28.0F * f);
        float h2 = Math.round((g - f2) * 0.5F);
        float f3 = Math.round(h - g2 - 96.0F * f);
        float g3 = hudRounding() * f;
        imDrawList.addRectFilled(h2, f3, h2 + f2, f3 + g2, HudChrome.CHIP_FILL, g3);
        imDrawList.addRect(h2, f3, h2 + f2, f3 + g2, 402653183, g3, 0, 1.0F);
        HudChrome.text(
                imDrawList,
                imFont,
                i,
                h2 + 12.0F * f,
                f3 + (float) Math.floor((g2 - i) * 0.5F),
                HudChrome.TEXT_LABEL,
                string,
                false);
    }

    private void drawEditorLabel(
            ImDrawList imDrawList,
            ImFont imFont,
            int i,
            float f,
            HudLayoutStore.HudElementState hudElementState,
            boolean bl,
            List<HudLayoutStore.HudElementState> list,
            List<float[]> list2,
            float g,
            float h) {
        String editorLabel = editorLabel(hudElementState.id);
        float f2 = bl ? 1.0F : 0.6F;
        float g2 = 8.0F * f;
        float h2 = 12.0F * f;
        float f3 = Math.round(20.0F * f);
        float width2 = HudChrome.width(imFont, i, editorLabel);
        float g3 = Math.round(g2 + width2 + 8.0F * f + h2 + g2);
        float[] floats = placeChip(hudElementState, g3, f3, 7.0F * f, list, list2, g, h);
        list2.add(floats);
        float h3 = floats[0];
        float f4 = floats[1];
        float g4 = 6.0F * f;
        imDrawList.addRectFilled(
                h3, f4, h3 + g3, f4 + f3, HudChrome.scaleAlpha(HudChrome.CHIP_FILL, f2), g4);
        imDrawList.addRect(
                h3, f4, h3 + g3, f4 + f3, HudChrome.scaleAlpha(402653183, f2), g4, 0, 1.0F);
        int scaleAlpha = HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, f2);
        HudChrome.text(
                imDrawList,
                imFont,
                i,
                h3 + g2,
                f4 + (float) Math.floor((f3 - i) * 0.5F),
                scaleAlpha,
                editorLabel,
                false);
        float h4 = h3 + g2 + width2 + 8.0F * f;
        float f5 = f4 + (f3 - h2) * 0.5F;
        ImGui.setCursorPos(h4, f5);
        if (ImGui.invisibleButton("##vis_" + hudElementState.id, h2, h2)) {
            hudElementState.visible = !hudElementState.visible;
            this.saveLayout();
        }
        boolean itemHovered = ImGui.isItemHovered();
        float g5 = h4 + h2 * 0.5F;
        float h5 = f5 + h2 * 0.5F;
        float f6 = h2 * 0.34F;
        int j =
                itemHovered
                        ? HudChrome.TEXT_VALUE
                        : HudChrome.scaleAlpha(
                                HudChrome.TEXT_LABEL, hudElementState.visible ? f2 : f2 * 0.6F);
        EfnWidgets.eye(imDrawList, g5, h5, f6, j);
        if (!hudElementState.visible) {
            imDrawList.addLine(g5 - f6, h5 - f6, g5 + f6, h5 + f6, j, Math.max(1.0F, 1.4F * f));
        }
    }

    private static float[] placeChip(
            HudLayoutStore.HudElementState hudElementState,
            float f,
            float g,
            float h,
            List<HudLayoutStore.HudElementState> list,
            List<float[]> list2,
            float f2,
            float g2) {
        float h2 = 4.0F;
        float f3 = hudElementState.x;
        float g3 = hudElementState.x + hudElementState.width - f;
        float h3 = alignsRight(hudElementState, f2) ? g3 : f3;
        float f4 = hudElementState.y + (hudElementState.height - g) * 0.5F;
        float[][] floatGrid;
        for (float[] floats :
                floatGrid =
                        new float[][] {
                            {h3, hudElementState.y - h - g},
                            {h3, hudElementState.y + hudElementState.height + h},
                            {
                                hudElementState.x + hudElementState.width + h,
                                Math.min(f4, hudElementState.y)
                            },
                            {hudElementState.x - h - f, Math.min(f4, hudElementState.y)}
                        }) {
            float g4 = Math.round(floats[0]);
            float h4 = Math.round(floats[1]);
            if (!(g4 < h2)
                    && !(h4 < h2)
                    && !(g4 + f > f2 - h2)
                    && !(h4 + g > g2 - h2)
                    && !overlapsAny(g4, h4, f, g, hudElementState, list, list2, h)) {
                return new float[] {g4, h4, f, g};
            }
        }
        float f5 = Math.round(Math.max(h2, Math.min(h3, f2 - h2 - f)));
        float g5 = floatGrid[0][1] >= h2 ? floatGrid[0][1] : floatGrid[1][1];
        return new float[] {f5, Math.round(Math.max(h2, Math.min(g5, g2 - h2 - g))), f, g};
    }

    private static boolean overlapsAny(
            float f,
            float g,
            float h,
            float f2,
            HudLayoutStore.HudElementState hudElementState,
            List<HudLayoutStore.HudElementState> list,
            List<float[]> list2,
            float g2) {
        for (HudLayoutStore.HudElementState hudElementState2 : list) {
            if (hudElementState2 != hudElementState
                    && hudElementState2.positioned
                    && overlaps(
                            f,
                            g,
                            h,
                            f2,
                            hudElementState2.x - g2 * 0.5F,
                            hudElementState2.y - g2 * 0.5F,
                            hudElementState2.width + g2,
                            hudElementState2.height + g2)) {
                return true;
            }
        }
        for (float[] floats : list2) {
            if (overlaps(f, g, h, f2, floats[0], floats[1], floats[2], floats[3])) {
                return true;
            }
        }
        return false;
    }

    private static boolean overlaps(
            float f, float g, float h, float f2, float g2, float h2, float f3, float g3) {
        return f < g2 + f3 && g2 < f + h && g < h2 + g3 && h2 < g + f2;
    }

    private static boolean contains(
            HudLayoutStore.HudElementState hudElementState, float f, float g) {
        return !hudElementState.positioned
                ? false
                : f >= hudElementState.x
                        && f <= hudElementState.x + hudElementState.width
                        && g >= hudElementState.y
                        && g <= hudElementState.y + hudElementState.height;
    }

    static String editorLabel(String string) {
        return switch (string) {
            case "info_bar" -> "Info Bar";
            case "coordinates" -> "Coordinates";
            case "module_list" -> "Module List";
            case "potion_tracker" -> "Potions";
            case "region_map" -> "Region Map";
            case "anti_vanish" -> "Anti Vanish";
            case "staff_list" -> "Staff List";
            case "spotify" -> "Spotify";
            case "radar" -> "Radar";
            case "hotkeys" -> "Hotkeys";
            case "target_card" -> "Target HUD";
            case "armor_status" -> "Armor HUD";
            case "item_cooldowns" -> "Cooldown";
            default -> string;
        };
    }

    private static int readTabLatency(
            MinecraftClient client, ClientPlayNetworkHandler clientPlayNetworkHandler) {
        PlayerListEntry playerListEntry =
                clientPlayNetworkHandler.getPlayerListEntry(
                        clientPlayNetworkHandler.getProfile().id());
        if (playerListEntry == null) {
            playerListEntry = clientPlayNetworkHandler.getPlayerListEntry(client.player.getUuid());
        }
        if (playerListEntry == null) {
            playerListEntry =
                    clientPlayNetworkHandler.getCaseInsensitivePlayerInfo(
                            clientPlayNetworkHandler.getProfile().name());
        }
        if (playerListEntry == null) {
            playerListEntry =
                    clientPlayNetworkHandler.getCaseInsensitivePlayerInfo(
                            client.player.getName().getString());
        }
        return playerListEntry == null ? 0 : Math.max(0, playerListEntry.getLatency());
    }

    private static int readServerPing(ClientPlayNetworkHandler clientPlayNetworkHandler) {
        ServerInfo serverInfo = clientPlayNetworkHandler.getServerInfo();
        return serverInfo != null && serverInfo.ping > 0L && serverInfo.ping <= 60000L
                ? (int) serverInfo.ping
                : 0;
    }

    private static float easeOutCubic(float f) {
        float g = clamp01(f);
        float h = 1.0F - g;
        return 1.0F - h * h * h;
    }

    private static float smooth(float f) {
        float g = clamp01(f);
        return g * g * (3.0F - 2.0F * g);
    }

    private static float clamp01(float f) {
        return Math.max(0.0F, Math.min(1.0F, f));
    }

    private static float elementScale(HudLayoutStore.HudElementState hudElementState) {
        return !Float.isNaN(hudElementState.scale) && !Float.isInfinite(hudElementState.scale)
                ? Math.max(0.5F, Math.min(3.0F, hudElementState.scale))
                : 1.0F;
    }

    static float scaleOf(HudLayoutStore.HudElementState hudElementState) {
        return elementScale(hudElementState) * ImGuiManager.getInstance().uiScale();
    }

    @Environment(EnvType.CLIENT)
    private record CoordPlan(
            List<HudRenderer.CoordRow> rows,
            HudRenderer.Type type,
            float[] slotW,
            float width,
            float height) {}

    @Environment(EnvType.CLIENT)
    private record CoordRow(
            String block, boolean here, boolean enabled, String[] values, float anim) {}

    @Environment(EnvType.CLIENT)
    private record DrawTransform(float xOffset, float yOffset, float alpha) {

        private static HudRenderer.DrawTransform identity() {
            return new HudRenderer.DrawTransform(0.0F, 0.0F, 1.0F);
        }

        private HudRenderer.DrawTransform down(float f) {
            return f == 0.0F
                    ? this
                    : new HudRenderer.DrawTransform(this.xOffset, this.yOffset + f, this.alpha);
        }
    }

    @Environment(EnvType.CLIENT)
    private record HotkeyLine(String name, String key, boolean on, float nameW, float keyW) {}

    @Environment(EnvType.CLIENT)
    private record HotkeyPlan(
            List<HudRenderer.HotkeyLine> lines, HudRenderer.Type type, float width, float height) {}

    @Environment(EnvType.CLIENT)
    private record InfoItem(String slot, String value, String dim, String unit) {}

    @Environment(EnvType.CLIENT)
    private record InfoPlan(
            List<HudRenderer.Segment> segments,
            HudRenderer.Type type,
            float width,
            float height,
            float emblemW) {}

    @Environment(EnvType.CLIENT)
    private record ListPlan(
            List<ModuleListRows.Row> rows, HudRenderer.Type type, float width, float height) {}

    @Environment(EnvType.CLIENT)
    private record LyricItem(
            String text, float top, float highlight, float alpha, float progress, float fill) {}

    @Environment(EnvType.CLIENT)
    private record LyricShift(float distance, float at, int start, float stagger) {}

    @Environment(EnvType.CLIENT)
    private record LyricView(List<HudRenderer.LyricItem> items, String status, float height) {}

    @Environment(EnvType.CLIENT)
    private record LyricWords(int[] from, int[] to, long[] start, long[] sweep) {}

    @Environment(EnvType.CLIENT)
    private record PotionLine(
            HudRenderer.PotionRow row, float nameW, float ampW, float durationW) {}

    @Environment(EnvType.CLIENT)
    private record PotionPlan(
            List<HudRenderer.PotionLine> lines, HudRenderer.Type type, float width, float height) {}

    @Environment(EnvType.CLIENT)
    private record PotionRow(
            RegistryEntry<StatusEffect> effect,
            String name,
            String amplifier,
            String duration,
            boolean infinite,
            boolean expiringSoon,
            int ticks) {}

    @Environment(EnvType.CLIENT)
    private record Segment(
            HudRenderer.InfoItem item, float valueW, float dimW, float contentW, float slotW) {}

    @Environment(EnvType.CLIENT)
    private record Snap(float position, float guideAt) {}

    @Environment(EnvType.CLIENT)
    private record SpotifyCard(
            NowPlaying track,
            boolean sample,
            boolean art,
            float header,
            float textTop,
            float column,
            boolean timeline,
            String elapsed,
            String remaining,
            float progress,
            boolean progressBar,
            HudRenderer.LyricView lyrics,
            float lyricAlpha) {}

    @Environment(EnvType.CLIENT)
    private record SpotifyColors(int title, int artist, int lyric, int upcoming, int lit) {

        static final HudRenderer.SpotifyColors DEFAULT =
                new HudRenderer.SpotifyColors(
                        HudChrome.TEXT_NAME,
                        HudChrome.TEXT_LABEL,
                        HudChrome.TEXT_VALUE,
                        HudChrome.TEXT_LABEL,
                        HudChrome.TEXT_VALUE);

        static HudRenderer.SpotifyColors of(SpotifyHudModule module) {
            if (module == null) {
                return DEFAULT;
            } else {
                SpotifyHudModule.LyricHighlight lyricHighlight = module.lyricHighlight();
                int abgr = GuiPalette.abgr(module.lyricsColor());
                int abgr2 = GuiPalette.abgr(module.highlightColor());
                return new HudRenderer.SpotifyColors(
                        GuiPalette.abgr(module.titleColor()),
                        GuiPalette.abgr(module.artistColor()),
                        lyricHighlight == SpotifyHudModule.LyricHighlight.LINE ? abgr2 : abgr,
                        GuiPalette.abgr(module.upcomingLyricsColor()),
                        lyricHighlight == SpotifyHudModule.LyricHighlight.OFF ? abgr : abgr2);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static enum SpotifyGlyph {
        NONE,
        PLAYING,
        PAUSED;

        private SpotifyGlyph() {}
    }

    @Environment(EnvType.CLIENT)
    private static enum SpotifyLook {
        HIDDEN,
        IDLE,
        COMPACT,
        CARD;

        private SpotifyLook() {}
    }

    @Environment(EnvType.CLIENT)
    private record SpotifyPlan(
            HudRenderer.Type type,
            HudRenderer.SpotifyLook look,
            HudRenderer.SpotifyGlyph glyph,
            String title,
            String artist,
            float titleW,
            float artistW,
            float titleOffset,
            float artistOffset,
            float loopGap,
            boolean scroll,
            HudRenderer.SpotifyCard card,
            HudRenderer.SpotifyColors colors,
            float width,
            float height) {}

    @Environment(EnvType.CLIENT)
    private record StaffLine(
            String name,
            int face,
            String state,
            boolean more,
            float nameW,
            float stateW,
            boolean server,
            boolean nearby,
            float alert,
            String note,
            float noteW,
            String value,
            float valueW,
            float serverTagW) {

        static HudRenderer.StaffLine plain(
                String string, int i, String string2, boolean bl, float f, float g) {
            return new HudRenderer.StaffLine(
                    string, i, string2, bl, f, g, false, false, 0.0F, "", 0.0F, "", 0.0F, 0.0F);
        }
    }

    @Environment(EnvType.CLIENT)
    private record StaffPlan(
            List<HudRenderer.StaffLine> lines,
            HudRenderer.Type type,
            String count,
            float width,
            float height) {}

    @Environment(EnvType.CLIENT)
    private record Step(int item, float l, float t, float r, float b, float alpha) {}

    @Environment(EnvType.CLIENT)
    private record Type(
            ImFont value, int valuePx, ImFont text, int textPx, ImFont label, int labelPx) {

        static HudRenderer.Type of(float f) {
            ImGuiManager imGuiManager = ImGuiManager.getInstance();
            return new HudRenderer.Type(
                    imGuiManager.hudValueFont(),
                    HudChrome.px(18.0F, f),
                    imGuiManager.hudTextFont(),
                    HudChrome.px(17.0F, f),
                    imGuiManager.hudLabelFont(),
                    HudChrome.px(14.0F, f));
        }
    }

    @Environment(EnvType.CLIENT)
    private record VanishLine(String tag, String value, boolean urgent, float tagW, float valueW) {}

    @Environment(EnvType.CLIENT)
    private record VanishPlan(
            List<HudRenderer.VanishLine> lines,
            HudRenderer.Type type,
            float tagColumn,
            float width,
            float height) {}
}
