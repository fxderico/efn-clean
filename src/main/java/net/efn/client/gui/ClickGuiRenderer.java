package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.efn.client.config.BundledDefaults;
import net.efn.client.config.ConfigProfiles;
import net.efn.client.config.FriendStore;
import net.efn.client.config.HudTheme;
import net.efn.client.config.ModuleCodes;
import net.efn.client.config.ModuleConfigStore;
import net.efn.client.config.ProfileException;
import net.efn.client.config.Surface;
import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.client.HotkeysModule;
import net.efn.client.module.impl.client.HudModule;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.module.impl.client.RadarModule;
import net.efn.client.module.impl.client.RegionMapModule;
import net.efn.client.module.setting.ActionSetting;
import net.efn.client.module.setting.BlockColorListSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ItemColorListSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.efn.client.module.setting.StringListSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class ClickGuiRenderer {

    private static final float PANEL_W = 212.0F;

    private static final float PANEL_GAP = 14.0F;

    private static final float PANEL_TOP = 28.0F;

    private static final float HEADER_H = 42.0F;

    private static final float ROW_H = 30.0F;

    private static final float ROW_GAP = 2.0F;

    private static final float ROW_INSET = 6.0F;

    private static final int HEADER_BAND = 251658239;

    private static final int ROW_RULE = 620756991;

    private static final float CARD_W = 460.0F;

    private static final float CARD_MAX_H = 760.0F;

    private static final float CARD_PAD = 24.0F;

    private static final float SETTING_ROW_H = 46.0F;

    private static final float TAB_BAR_H = 44.0F;

    private static final float TAB_BAR_BOTTOM = 26.0F;

    private static final float INSPECTOR_W = 360.0F;

    private static final float SMALL_TOG_W = 16.0F;

    private static final float SMALL_TOG_H = 16.0F;

    public static final int ERROR_TEXT = -10856974;

    private static final int[] AVATAR_COLORS =
            new int[] {
                -624588, -10893516, -16080897, -2207057, -10536961, -3690448, -2730408, -16724737
            };

    private static final int PANEL_FLAGS = 123;

    private static final int CARD_FLAGS = 8511;

    public static final int CHILD_FLAGS = 24;

    private static final String PANEL_ID_PREFIX = "##panel_";

    private static final String SEARCH_PANEL_ID = "##panel_search";

    private static final int STYLE_VAR_COUNT = 11;

    private static final int DYNAMIC_COLOR_COUNT = 10;

    private static final int[] THEME_COLORS =
            new int[] {
                0,
                -461582,
                1,
                -4542808,
                3,
                0,
                4,
                -14015199,
                5,
                352321535,
                6,
                0,
                7,
                -13226454,
                8,
                -12240588,
                9,
                -12240588,
                21,
                -13226454,
                22,
                -12240588,
                24,
                -13226454,
                25,
                -12240588,
                27,
                268435455,
                14,
                0,
                15,
                822083583,
                16,
                1358954495,
                17,
                1895825407,
                30,
                0,
                31,
                0,
                32,
                0,
                53,
                0
            };

    private ClickGuiRenderer.Tab tab = ClickGuiRenderer.Tab.MODULES;

    private final long guiOpenedAt;

    private long viewChangedAt;

    private float tabIndicatorX;

    private float tabIndicatorW;

    private ClientModule settingsModule;

    private boolean cardJustOpened;

    private boolean dividerPending;

    private boolean panelGuard;

    private boolean tabBarGuard;

    private boolean cardGuard;

    private boolean cardTextLift;

    private final HashMap<String, float[]> scroll;

    private static float scrollDragFrom;

    private static final ImBoolean SCROLLBAR_HOVERED = new ImBoolean();

    private static final ImBoolean SCROLLBAR_HELD = new ImBoolean();

    private ClientModule listeningModule;

    private boolean listeningFirstFrame;

    private KeySetting listeningKeySetting;

    private boolean listeningKeyFirstFrame;

    private final ImBoolean colorPickerOpen;

    private ClientModule activeColorModule;

    private ColorSetting activeColorSetting;

    private final ImString globalSearch;

    private boolean focusSearch;

    private float searchBodyH;

    private final Map<String, float[]> panelSpots;

    private final Map<String, float[]> panelHomes;

    private final Map<String, float[]> panelTargets;

    private String panelLayoutKey;

    private boolean panelHomesDirty;

    private ClickGuiRenderer.CategoryPanel[] panelCache;

    private int panelCacheRevision;

    private final Map<String, float[]> panelFold;

    private final Map<String, Motion.Rows> rowMotion;

    private final Int2ObjectOpenHashMap<String> keyLabels;

    private int cachedRegistryRevision;

    private String cachedGlobalSearch;

    private List<ClientModule> cachedGlobalResults;

    private final ImString friendInput;

    private boolean focusFriendInput;

    private String friendMessage;

    private long friendMessageAt;

    private final ImString profileInput;

    private boolean focusProfileInput;

    private String configMessage;

    private boolean configMessageError;

    private long configMessageAt;

    private String moduleCodeFor;

    private String moduleCodeMessage;

    private boolean moduleCodeError;

    private long moduleCodeAt;

    private final ProfileList profileList;

    private String pendingConfirm;

    private long pendingConfirmAt;

    private String themeMessage;

    private boolean themeMessageError;

    private long themeMessageAt;

    private Surface themeSurface;

    private boolean inspectMode;

    private final List<float[]> panelRects;

    private float[] cardRect;

    private float[] tabBarRect;

    private float[] inspectorRect;

    private Surface hoveredSurface;

    private boolean rowDividers;

    private float rowAboveEmphasis;

    private boolean focusInspector;

    private long previewChangedAt;

    private String hudElementId;

    private boolean focusHudInspector;

    private String hudMessage;

    private boolean hudMessageError;

    private long hudMessageAt;

    private final BlockListEditor blockLists;

    private final ItemPickerEditor itemPickers;

    private ItemColorListSetting activeItemColorPalette;

    private boolean itemColorWindowOpen;

    private final ImString itemSearchBuf;

    private ItemColorListSetting.ItemEntry activeItemColorEntry;

    private boolean editingItemDefaultColor;

    private final HashMap<ItemColorListSetting.ItemEntry, Float> itemEntryTogAnim;

    private final ImBoolean itemColorPickerOpen;

    public ClickGuiRenderer() {
        this.viewChangedAt = this.guiOpenedAt = System.nanoTime();
        this.tabIndicatorX = -1.0F;
        this.scroll = new HashMap<>();
        this.listeningModule = null;
        this.listeningFirstFrame = false;
        this.listeningKeySetting = null;
        this.listeningKeyFirstFrame = false;
        this.colorPickerOpen = new ImBoolean(false);
        this.activeColorModule = null;
        this.activeColorSetting = null;
        this.globalSearch = new ImString(128);
        this.panelSpots = new HashMap<>();
        this.panelHomes = new HashMap<>();
        this.panelTargets = new HashMap<>();
        this.panelLayoutKey = "";
        this.panelCache = new ClickGuiRenderer.CategoryPanel[0];
        this.panelCacheRevision = -1;
        this.panelFold = new HashMap<>();
        this.rowMotion = new HashMap<>();
        this.keyLabels = new Int2ObjectOpenHashMap<>();
        this.cachedRegistryRevision = -1;
        this.cachedGlobalSearch = "";
        this.cachedGlobalResults = List.of();
        this.friendInput = new ImString(32);
        this.profileInput = new ImString(64);
        this.profileList = new ProfileList(this::setConfigMessage);
        this.panelRects = new ArrayList<>();
        this.blockLists = new BlockListEditor();
        this.itemPickers = new ItemPickerEditor();
        this.activeItemColorPalette = null;
        this.itemColorWindowOpen = false;
        this.itemSearchBuf = new ImString(256);
        this.activeItemColorEntry = null;
        this.editingItemDefaultColor = false;
        this.itemEntryTogAnim = new HashMap<>();
        this.itemColorPickerOpen = new ImBoolean(false);
    }

    public void render() {
        float animationDelta = animationDelta(Math.max(ImGui.getIO().getDeltaTime(), 0.001F));
        ThemeStore themeStore = ThemeStore.get();
        this.panelGuard = HudChrome.needsTextGuard(themeStore.panels);
        this.tabBarGuard = HudChrome.needsTextGuard(themeStore.tabBar);
        this.cardGuard = HudChrome.needsTextGuard(themeStore.cards);
        this.cardTextLift = themeStore.cards.isImage() && !themeStore.cards.isBanner();
        applyTheme();
        boolean pushFont = pushFont(ImGuiManager.getInstance().moduleFont());
        try {
            float displaySizeX = ImGui.getIO().getDisplaySizeX();
            float displaySizeY = ImGui.getIO().getDisplaySizeY();
            float easeOut = easeOut(elapsed(this.guiOpenedAt, 240.0F));
            float easeOut2 = easeOut(elapsed(this.viewChangedAt, 260.0F));
            float f = Math.max(0.01F, easeOut * easeOut2);
            ImGui.getBackgroundDrawList()
                    .addRectFilled(
                            0.0F,
                            0.0F,
                            displaySizeX,
                            displaySizeY,
                            EfnWidgets.withAlpha(
                                    -16777216, themeStore.backgroundDim.get() * easeOut));
            Path path = ThemeImages.get().takePendingImport();
            if (path != null) {
                this.importHeaderImage(path);
            }
            this.handleShortcuts();
            switch (this.tab) {
                case MODULES:
                    if (this.settingsModule == null) {
                        this.renderModulePanels(animationDelta, f);
                    } else {
                        this.renderModuleCard(this.settingsModule, animationDelta, f, easeOut2);
                    }
                    break;
                case HUD:
                    this.renderHudStudio(animationDelta, f, easeOut2);
                    break;
                case FRIENDS:
                    this.renderFriendsCard(animationDelta, f, easeOut2);
                    break;
                case THEME:
                    this.renderThemeStudio(animationDelta, f, easeOut2);
                    break;
                case CONFIG:
                    this.renderConfigCard(animationDelta, f, easeOut2);
            }
            this.renderTabBar(animationDelta, easeOut);
            this.savePanelHomes();
            this.renderColorPickerWindow();
            this.blockLists.renderWindows();
            this.itemPickers.renderWindows();
            this.renderItemColorPaletteWindow();
            this.closeCardOnOutsideClick();
        } finally {
            if (pushFont) {
                ImGui.popFont();
            }
            removeTheme();
        }
    }

    private void handleShortcuts() {
        if (this.tab == ClickGuiRenderer.Tab.MODULES
                && this.settingsModule == null
                && ImGui.getIO().getKeyCtrl()
                && ImGui.isKeyPressed(551)) {
            this.focusSearch = true;
        }
    }

    public void showModuleSettings(ClientModule module) {
        this.switchTab(ClickGuiRenderer.Tab.MODULES);
        this.openSettings(module);
    }

    private void switchTab(ClickGuiRenderer.Tab tab2) {
        if (tab2 != this.tab) {
            this.closeSettings();
            this.tab = tab2;
            this.viewChangedAt = System.nanoTime();
            this.pendingConfirm = null;
            this.profileList.disarm();
            if (tab2 == ClickGuiRenderer.Tab.CONFIG) {
                ConfigProfiles.getInstance().refresh();
            }
        }
    }

    private void renderModulePanels(float f, float g) {
        ClickGuiRenderer.CategoryPanel[] categoryPanels = this.visiblePanels();
        boolean bl = this.panelHomes.containsKey("##panel_search");
        for (ClickGuiRenderer.CategoryPanel categoryPanel : categoryPanels) {
            bl &= this.panelHomes.containsKey(categoryPanel.id());
        }
        if (!bl) {
            this.placeNewPanels(categoryPanels);
        }
        this.fitPanels(categoryPanels);
        for (ClickGuiRenderer.CategoryPanel categoryPanel2 : categoryPanels) {
            this.renderCategoryPanel(categoryPanel2, f, g);
        }
        this.renderSearchPanel(f, g);
    }

    private ClickGuiRenderer.CategoryPanel[] visiblePanels() {
        int revision = ModuleRegistry.revision();
        if (revision != this.panelCacheRevision) {
            ArrayList list = new ArrayList();
            for (ModuleCategory moduleCategory : ModuleCategory.values()) {
                if (!this.modulesFor(moduleCategory).isEmpty()) {
                    list.add(
                            new ClickGuiRenderer.CategoryPanel(
                                    moduleCategory,
                                    "##panel_" + moduleCategory.name(),
                                    "##rows_" + moduleCategory.name()));
                }
            }
            this.panelCache =
                    ((ClickGuiRenderer.CategoryPanel[])
                            list.toArray(ClickGuiRenderer.CategoryPanel[]::new));
            this.panelCacheRevision = revision;
        }
        return this.panelCache;
    }

    private void renderCategoryPanel(
            ClickGuiRenderer.CategoryPanel categoryPanel, float f, float g) {
        List list = this.modulesFor(categoryPanel.category());
        float h = Math.min(rowListHeight(list.size()), maxListHeight());
        float unfolded = this.unfolded(categoryPanel, f);
        if (unfolded < 1.0F) {
            ImGui.setNextWindowSize(EfnWidgets.s(212.0F), EfnWidgets.s(42.0F) + h * unfolded, 1);
        }
        if (this.beginPanel(categoryPanel.id(), g)) {
            String displayName = categoryPanel.category().displayName();
            this.drawPanelChrome(displayName, categoryPanel.category());
            this.recordPanelRect();
            this.renderFoldControl(categoryPanel.category(), unfolded);
            if (unfolded > 0.0F) {
                this.renderRowList(categoryPanel.rowsId(), list, h, false, f);
            }
        }
        endPanel();
    }

    private float unfolded(ClickGuiRenderer.CategoryPanel categoryPanel, float f) {
        float g = PanelStateStore.getInstance().isCollapsed(categoryPanel.category()) ? 0.0F : 1.0F;
        float[] floats = this.panelFold.get(categoryPanel.id());
        if (floats == null) {
            floats = new float[] {g};
            this.panelFold.put(categoryPanel.id(), floats);
        }
        floats[0] = smooth(floats[0], g, f, 14.0F);
        if (Math.abs(floats[0] - g) < 0.002F) {
            floats[0] = g;
        }
        return floats[0];
    }

    private void renderFoldControl(ModuleCategory moduleCategory, float f) {
        float windowPosX = ImGui.getWindowPosX();
        float windowPosY = ImGui.getWindowPosY();
        float windowWidth = ImGui.getWindowWidth();
        float px42 = EfnWidgets.s(42.0F);
        float px22 = EfnWidgets.s(22.0F);
        float g = windowPosX + windowWidth - EfnWidgets.s(20.0F);
        float h = windowPosY + px42 * 0.5F;
        boolean bl =
                !this.inspectMode
                        && ImGui.isWindowHovered()
                        && ImGui.isMouseHoveringRect(
                                windowPosX,
                                windowPosY,
                                windowPosX + windowWidth,
                                windowPosY + px42);
        ImGui.setCursorScreenPos(g - px22 * 0.5F, h - px22 * 0.5F);
        boolean bl2 = ImGui.invisibleButton("##fold", px22, px22) && !this.inspectMode;
        float animate = EfnWidgets.animate(ImGui.getItemID(), bl, 18.0F);
        ImGui.setCursorPos(0.0F, px42);
        if (bl2 || bl && ImGui.isMouseClicked(1)) {
            PanelStateStore panelStateStore = PanelStateStore.getInstance();
            panelStateStore.setCollapsed(
                    moduleCategory, !panelStateStore.isCollapsed(moduleCategory));
        }
        EfnWidgets.chevronRotated(
                ImGui.getWindowDrawList(),
                g,
                h,
                EfnWidgets.s(4.0F),
                EfnTheme.lerpColor(-4542808, -461582, animate),
                f * 1.5707964F);
    }

    private void renderSearchPanel(float f, float g) {
        if (this.beginPanel("##panel_search", g)) {
            this.drawPanelChrome("Search", null);
            this.recordPanelRect();
            float h = EfnWidgets.s(6.0F) + EfnWidgets.s(2.0F);
            ImGui.setCursorPos(h, EfnWidgets.s(42.0F) - EfnWidgets.s(4.0F));
            ImGui.pushStyleVar(11, EfnWidgets.s(10.0F), EfnWidgets.s(7.0F));
            ImGui.setNextItemWidth(EfnWidgets.s(212.0F) - h * 2.0F);
            if (this.focusSearch) {
                ImGui.setKeyboardFocusHere();
                this.focusSearch = false;
            }
            ImGui.inputTextWithHint("##globalsearch", "Search modules...", this.globalSearch);
            ImGui.popStyleVar();
            ImGui.dummy(0.0F, EfnWidgets.s(8.0F));
            String normalizeSearch = normalizeSearch(this.globalSearch.get());
            List list = normalizeSearch.isEmpty() ? List.of() : this.searchModules(normalizeSearch);
            float f2 =
                    normalizeSearch.isEmpty()
                            ? 0.0F
                            : (list.isEmpty()
                                    ? EfnWidgets.s(34.0F)
                                    : Math.min(rowListHeight(list.size()), maxListHeight()));
            this.searchBodyH = smooth(this.searchBodyH, f2, f, 14.0F);
            if (Math.abs(this.searchBodyH - f2) < 0.5F) {
                this.searchBodyH = f2;
            }
            if (this.searchBodyH >= 1.0F) {
                if (list.isEmpty()) {
                    ImGui.beginChild(
                            "##search_empty", EfnWidgets.s(212.0F), this.searchBodyH, false, 24);
                    String string = "No results";
                    ImFont font = ImGui.getFont();
                    EfnWidgets.text(
                            ImGui.getWindowDrawList(),
                            font,
                            ImGui.getWindowPosX()
                                    + (EfnWidgets.s(212.0F) - EfnWidgets.textWidth(font, string))
                                            * 0.5F,
                            ImGui.getWindowPosY() + EfnWidgets.s(4.0F),
                            -4542808,
                            string,
                            this.panelGuard);
                    ImGui.endChild();
                } else {
                    this.renderRowList("##search_rows", list, this.searchBodyH, true, f);
                }
            }
        }
        endPanel();
    }

    private boolean beginPanel(String string, float f) {
        float[] panelTarget = this.panelTargets.remove(string);
        if (panelTarget != null) {
            ImGui.setNextWindowPos(panelTarget[0], panelTarget[1], 1);
        }
        ImGui.setNextWindowSizeConstraints(
                EfnWidgets.s(212.0F), 0.0F, EfnWidgets.s(212.0F), 3.4028235E38F);
        ImGui.pushStyleVar(0, f);
        ImGui.pushStyleVar(2, 0.0F, 0.0F);
        ImGui.pushStyleVar(14, 0.0F, 0.0F);
        ImGui.pushStyleVar(3, panelRounding());
        boolean begin = ImGui.begin(string, 123);
        float windowPosX = ImGui.getWindowPosX();
        float windowPosY = ImGui.getWindowPosY();
        float[] floats =
                this.panelSpots.computeIfAbsent(
                        string, stringx -> new float[] {windowPosX, windowPosY});
        boolean bl =
                Math.abs(windowPosX - floats[0]) > 0.5F || Math.abs(windowPosY - floats[1]) > 0.5F;
        if (panelTarget == null && bl && ImGui.isMouseDown(0)) {
            this.panelHomes.put(
                    string,
                    new float[] {windowPosX / EfnWidgets.s(1.0F), windowPosY / EfnWidgets.s(1.0F)});
            this.panelHomesDirty = true;
        }
        floats[0] = windowPosX;
        floats[1] = windowPosY;
        return begin;
    }

    private void savePanelHomes() {
        if (this.panelHomesDirty && !ImGui.isMouseDown(0)) {
            this.panelHomesDirty = false;
            PanelStateStore.getInstance().setPanelHomes(this.panelHomes);
        }
    }

    private static void endPanel() {
        ImGui.end();
        ImGui.popStyleVar(4);
    }

    private void drawPanelChrome(String string, ModuleCategory moduleCategory) {
        float windowPosX = ImGui.getWindowPosX();
        float windowPosY = ImGui.getWindowPosY();
        float windowWidth = ImGui.getWindowWidth();
        float windowHeight = ImGui.getWindowHeight();
        drawShadow(windowPosX, windowPosY, windowWidth, windowHeight, panelRounding(), 4);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        SurfaceBackground.draw(
                windowDrawList,
                ThemeStore.get().panels,
                windowPosX,
                windowPosY,
                windowWidth,
                windowHeight,
                panelRounding());
        ImFont imFont = ImGuiManager.getInstance().categoryFont();
        float px42 = EfnWidgets.s(42.0F);
        drawHeaderBand(windowDrawList, windowPosX, windowPosY, windowWidth, px42);
        float px7 = EfnWidgets.s(7.0F);
        float f = windowPosX + EfnWidgets.s(14.0F) + px7;
        float g = windowPosY + px42 * 0.5F;
        if (moduleCategory == null) {
            EfnWidgets.magnifier(windowDrawList, f, g, px7, -4542808);
        } else {
            drawCategoryIcon(windowDrawList, moduleCategory, f, g, px7, -4542808);
        }
        float h = f + px7 + EfnWidgets.s(9.0F);
        float f2 = windowPosY + (px42 - EfnWidgets.lineHeight(imFont)) * 0.5F;
        EfnWidgets.text(windowDrawList, imFont, h, f2, -461582, string, this.panelGuard);
        ImGui.dummy(windowWidth, px42);
    }

    private static void drawHeaderBand(ImDrawList imDrawList, float f, float g, float h, float f2) {
        float panelRounding = panelRounding();
        float g2 = Math.min(f2, panelRounding + EfnWidgets.s(2.0F));
        int i = EfnWidgets.c(EfnWidgets.withAlpha(251658239, 1.0F - g2 / (2.0F * f2)));
        int j = EfnWidgets.c(EfnWidgets.withAlpha(251658239, 1.0F - g2 / f2));
        int k = EfnWidgets.c(16777215);
        imDrawList.addRectFilled(f, g, f + h, g + g2, i, panelRounding, 48);
        imDrawList.addRectFilledMultiColor(f, g + g2, f + h, g + f2, j, j, k, k);
    }

    private static void softRule(ImDrawList imDrawList, float f, float g, float h, int i) {
        float f2 = (f + g) * 0.5F;
        int j = EfnWidgets.c(i);
        int k = EfnWidgets.c(i & 16777215);
        imDrawList.addRectFilledMultiColor(f, h, f2, h + 1.0F, k, j, j, k);
        imDrawList.addRectFilledMultiColor(f2, h, g, h + 1.0F, j, k, k, j);
    }

    private static void drawCategoryIcon(
            ImDrawList imDrawList,
            ModuleCategory moduleCategory,
            float f,
            float g,
            float h,
            int i) {
        switch (moduleCategory.icon()) {
            case CROSSHAIR:
                EfnWidgets.crosshair(imDrawList, f, g, h, i);
                break;
            case CHEVRON:
                EfnWidgets.chevron(imDrawList, f, g, h, i, true);
                break;
            case MOVE:
                EfnWidgets.move(imDrawList, f, g, h, i);
                break;
            case GRID:
                EfnWidgets.grid(imDrawList, f, g, h, i);
                break;
            case RING:
                EfnWidgets.ring(imDrawList, f, g, h, i);
                break;
            case EYE:
                EfnWidgets.eye(imDrawList, f, g, h, i);
                break;
            case MONITOR:
                EfnWidgets.monitor(imDrawList, f, g, h, i);
                break;
            case HOUSE:
                EfnWidgets.house(imDrawList, f, g, h, i);
                break;
            case SCROLL:
                EfnWidgets.scroll(imDrawList, f, g, h, i);
                break;
            case ROBOT:
                EfnWidgets.robot(imDrawList, f, g, h, i);
                break;
            case BOLT:
                EfnWidgets.bolt(imDrawList, f, g, h, i);
        }
    }

    public static void drawShadow(float f, float g, float h, float f2, float g2, int i) {
        ImDrawList backgroundDrawList = ImGui.getBackgroundDrawList();
        for (int m = 1; m <= i; m++) {
            float h2 = EfnWidgets.s(2.5F) * m;
            backgroundDrawList.addRectFilled(
                    f - h2,
                    g - h2 + EfnWidgets.s(4.0F),
                    f + h + h2,
                    g + f2 + h2 + EfnWidgets.s(4.0F),
                    EfnWidgets.c(335544320),
                    g2 + h2);
        }
    }

    private static float rowListHeight(int i) {
        return i * EfnWidgets.s(32.0F) - EfnWidgets.s(2.0F) + EfnWidgets.s(6.0F);
    }

    private static float maxListHeight() {
        return Math.max(
                EfnWidgets.s(120.0F),
                ImGui.getIO().getDisplaySizeY() * 0.62F - EfnWidgets.s(42.0F));
    }

    private void renderRowList(
            String string, List<ClientModule> list, float f, boolean bl, float g) {
        if (ImGui.beginChild(string, EfnWidgets.s(212.0F), Math.max(1.0F, f), false, 24)) {
            Motion.Rows rows = this.rowMotion.computeIfAbsent(string, stringx -> new Motion.Rows());
            rows.bind(list);
            this.rowDividers = ThemeStore.get().moduleDividers.get();
            for (int i = 0; i < list.size(); i++) {
                this.renderModuleRow((ClientModule) list.get(i), bl, rows, i, g);
            }
            ImGui.dummy(1.0F, EfnWidgets.s(6.0F) - EfnWidgets.s(2.0F));
            this.applySmoothScroll(string, g);
        }
        ImGui.endChild();
    }

    private void renderModuleRow(
            ClientModule module, boolean bl, Motion.Rows rows, int i, float f) {
        ImGui.pushID(module.id());
        float px6 = EfnWidgets.s(6.0F);
        float px30 = EfnWidgets.s(30.0F);
        float cursorPosY = ImGui.getCursorPosY();
        ImGui.setCursorPosX(px6);
        float cursorScreenPosX = ImGui.getCursorScreenPosX();
        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        float g = ImGui.getWindowWidth() - px6 * 2.0F;
        if (ImGui.invisibleButton("##row", g, px30) && !this.inspectMode) {
            if (module.isAction()) {
                module.onClick();
            } else {
                module.toggle();
            }
        }
        boolean itemHovered = ImGui.isItemHovered();
        if (!this.inspectMode && ImGui.isItemClicked(1)) {
            this.openSettings(module);
        }
        if (!this.inspectMode && ImGui.isItemHovered(65536)) {
            String description = module.description();
            showTooltip(
                    description != null && !description.isBlank()
                            ? description + "\nRight-click for settings"
                            : "Right-click for settings");
        }
        float hover = rows.hover(i, itemHovered, f, 18.0F);
        float on = rows.on(i, module.isEnabled(), f, 14.0F);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        float h = Math.max(hover, on);
        float f2;
        if (this.rowDividers && i > 0 && (f2 = 1.0F - Math.max(h, this.rowAboveEmphasis)) > 0.01F) {
            float g2 = (float) Math.floor(cursorScreenPosY - EfnWidgets.s(2.0F) * 0.5F);
            softRule(
                    windowDrawList,
                    cursorScreenPosX + EfnWidgets.s(8.0F),
                    cursorScreenPosX + g - EfnWidgets.s(8.0F),
                    g2,
                    EfnWidgets.withAlpha(620756991, f2));
        }
        this.rowAboveEmphasis = h;
        float px62 = EfnWidgets.s(6.0F);
        if (hover > 0.01F) {
            windowDrawList.addRectFilled(
                    cursorScreenPosX,
                    cursorScreenPosY,
                    cursorScreenPosX + g,
                    cursorScreenPosY + px30,
                    EfnWidgets.c(EfnWidgets.withAlpha(318767103, hover)),
                    px62);
        }
        if (on > 0.01F) {
            windowDrawList.addRectFilled(
                    cursorScreenPosX,
                    cursorScreenPosY,
                    cursorScreenPosX + g,
                    cursorScreenPosY + px30,
                    EfnWidgets.c(
                            EfnWidgets.withAlpha(
                                    EfnTheme.lerpColor(
                                            GuiPalette.accent(),
                                            GuiPalette.accentHover(),
                                            hover * 0.3F),
                                    on)),
                    px62);
        }
        ImFont font = ImGui.getFont();
        ImFont imFont = ImGuiManager.getInstance().smallFont();
        String string =
                bl
                        ? module.category().displayName()
                        : (module.keybind() == -1 ? null : this.keyLabel(module.keybind()));
        float h2 = string == null ? 0.0F : EfnWidgets.textWidth(imFont, string);
        int lerpColor = EfnTheme.lerpColor(EfnTheme.lerpColor(-2042416, -461582, hover), -1, on);
        float f3 = g - EfnWidgets.s(string == null && !module.isAction() ? 20.0F : 28.0F) - h2;
        String ellipsize = EfnWidgets.ellipsize(font, module.name(), f3);
        EfnWidgets.text(
                windowDrawList,
                font,
                cursorScreenPosX + EfnWidgets.s(10.0F),
                cursorScreenPosY + (px30 - EfnWidgets.lineHeight(font)) * 0.5F,
                lerpColor,
                ellipsize,
                this.panelGuard);
        if (string != null) {
            int lerpColor2 = EfnTheme.lerpColor(-4542808, -419430401, on);
            EfnWidgets.text(
                    windowDrawList,
                    imFont,
                    cursorScreenPosX + g - EfnWidgets.s(10.0F) - h2,
                    cursorScreenPosY + (px30 - EfnWidgets.lineHeight(imFont)) * 0.5F,
                    lerpColor2,
                    string,
                    this.panelGuard);
        } else if (module.isAction()) {
            EfnWidgets.chevronRight(
                    windowDrawList,
                    cursorScreenPosX + g - EfnWidgets.s(14.0F),
                    cursorScreenPosY + px30 * 0.5F,
                    EfnWidgets.s(4.0F),
                    EfnTheme.lerpColor(-4542808, -461582, hover));
        }
        ImGui.setCursorPos(0.0F, cursorPosY + px30 + EfnWidgets.s(2.0F));
        ImGui.popID();
    }

    public static void showTooltip(String string) {
        ImGui.pushStyleVar(2, EfnWidgets.s(10.0F), EfnWidgets.s(8.0F));
        ImGui.beginTooltip();
        ImGui.pushTextWrapPos(EfnWidgets.s(280.0F));
        ImGui.textUnformatted(string);
        ImGui.popTextWrapPos();
        ImGui.endTooltip();
        ImGui.popStyleVar();
    }

    private void applySmoothScroll(String string, float f) {
        smoothScroll(this.scroll.computeIfAbsent(string, stringx -> new float[2]), f);
    }

    public static void smoothScroll(float[] floats, float f) {
        float scrollMaxY = ImGui.getScrollMaxY();
        float windowPosX = ImGui.getWindowPosX();
        float windowPosY = ImGui.getWindowPosY();
        float windowWidth = ImGui.getWindowWidth();
        float windowHeight = ImGui.getWindowHeight();
        float g =
                Math.max(
                        EfnWidgets.s(24.0F),
                        windowHeight * windowHeight / (windowHeight + scrollMaxY));
        float h = Math.max(1.0F, windowHeight - g);
        boolean bl = false;
        boolean bl2 = false;
        if (scrollMaxY > 0.5F) {
            int iD = ImGui.getID("##scrollbar");
            imgui.internal.ImGui.keepAliveID(iD);
            imgui.internal.ImGui.buttonBehavior(
                    windowPosX + windowWidth - EfnWidgets.s(8.0F),
                    windowPosY,
                    windowPosX + windowWidth,
                    windowPosY + windowHeight,
                    iD,
                    SCROLLBAR_HOVERED,
                    SCROLLBAR_HELD);
            bl = SCROLLBAR_HELD.get();
            bl2 = bl || SCROLLBAR_HOVERED.get();
            if (bl && ImGui.isMouseClicked(0)) {
                float f2 = windowPosY + h * (floats[0] / scrollMaxY);
                float mousePosY = ImGui.getMousePosY();
                floats[1] =
                        mousePosY >= f2 && mousePosY <= f2 + g
                                ? floats[0]
                                : (mousePosY - windowPosY - g * 0.5F) / h * scrollMaxY;
                scrollDragFrom = floats[1];
            } else if (bl) {
                floats[1] = scrollDragFrom + ImGui.getMouseDragDeltaY(0, 0.0F) * scrollMaxY / h;
            }
        }
        float mouseWheel;
        if (!bl
                && ImGui.isWindowHovered()
                && (mouseWheel = ImGui.getIO().getMouseWheel()) != 0.0F) {
            floats[1] -= mouseWheel * EfnWidgets.s(56.0F);
        }
        floats[1] = Math.max(0.0F, Math.min(scrollMaxY, floats[1]));
        float g2 = floats[0] = bl ? floats[1] : smooth(floats[0], floats[1], f, 16.0F);
        if (Math.abs(floats[0] - floats[1]) < 0.3F) {
            floats[0] = floats[1];
        }
        ImGui.setScrollY(floats[0]);
        if (scrollMaxY > 0.5F) {
            float h2 = windowPosY + h * (floats[0] / scrollMaxY);
            ImGui.getWindowDrawList()
                    .addRectFilled(
                            windowPosX
                                    + windowWidth
                                    - (bl2 ? EfnWidgets.s(6.0F) : EfnWidgets.s(4.0F)),
                            h2,
                            windowPosX + windowWidth - EfnWidgets.s(1.5F),
                            h2 + g,
                            EfnWidgets.c(bl2 ? 1728053247 : 872415231),
                            EfnWidgets.s(2.0F));
        }
    }

    private ClickGuiRenderer.PanelLayout panelLayout(
            ClickGuiRenderer.CategoryPanel[] categoryPanels) {
        ArrayList list = new ArrayList();
        ArrayList list2 = new ArrayList();
        ArrayList list3 = new ArrayList();
        PanelStateStore panelStateStore = PanelStateStore.getInstance();
        for (ClickGuiRenderer.CategoryPanel categoryPanel : categoryPanels) {
            list.add(categoryPanel.id());
            list2.add(categoryPanel.category().name());
            list3.add(
                    panelStateStore.isCollapsed(categoryPanel.category())
                            ? EfnWidgets.s(42.0F)
                            : EfnWidgets.s(42.0F)
                                    + Math.min(
                                            rowListHeight(
                                                    this.modulesFor(categoryPanel.category())
                                                            .size()),
                                            maxListHeight()));
        }
        list.add("##panel_search");
        list2.add("search");
        list3.add(
                EfnWidgets.s(42.0F) + ImGui.getFontSize() + EfnWidgets.s(18.0F) + maxListHeight());
        return new ClickGuiRenderer.PanelLayout(list, list2, list3);
    }

    private void placeNewPanels(ClickGuiRenderer.CategoryPanel[] categoryPanels) {
        ClickGuiRenderer.PanelLayout panelLayout = this.panelLayout(categoryPanels);
        List list = panelLayout.ids();
        List list2 = panelLayout.keys();
        List list3 = panelLayout.heights();
        float px1 = EfnWidgets.s(1.0F);
        Map map = PanelStateStore.getInstance().panelHomes();
        Map map2 = map.isEmpty() ? savedPanelSpots() : Map.of();
        ArrayList list4 = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            String string = (String) list.get(i);
            if (!this.panelHomes.containsKey(string)) {
                float[] floats = (float[]) map.get(string);
                float[] floats2 = (float[]) map2.get(string);
                if (floats == null
                        && floats2 != null
                        && coveredArea(floats2[0], floats2[1], (Float) list3.get(i), list4)
                                == 0.0F) {
                    floats = new float[] {floats2[0] / px1, floats2[1] / px1};
                    this.panelHomesDirty = true;
                }
                if (floats != null) {
                    this.panelHomes.put(string, floats);
                }
            }
            float[] panelHome;
            if ((panelHome = this.panelHomes.get(string)) != null) {
                list4.add(
                        new float[] {
                            panelHome[0] * px1,
                            panelHome[1] * px1,
                            EfnWidgets.s(212.0F),
                            (Float) list3.get(i)
                        });
            }
        }
        boolean bl = list4.isEmpty();
        for (int j = 0; j < list.size(); j++) {
            if (!this.panelHomes.containsKey(list.get(j))) {
                float f = (Float) list3.get(j);
                float[] floats3 = bundledSpot((String) list2.get(j));
                if (floats3 == null && bl) {
                    floats3 = panelPosFor(j, list.size());
                }
                if (floats3 == null || coveredArea(floats3[0], floats3[1], f, list4) > 0.0F) {
                    floats3 = freeSpot(f, list4);
                }
                this.panelHomes.put(
                        (String) list.get(j), new float[] {floats3[0] / px1, floats3[1] / px1});
                list4.add(new float[] {floats3[0], floats3[1], EfnWidgets.s(212.0F), f});
                this.panelHomesDirty = true;
            }
        }
        this.panelLayoutKey = "";
    }

    private void fitPanels(ClickGuiRenderer.CategoryPanel[] categoryPanels) {
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float px1;
        float displaySizeY;
        String string =
                displaySizeX
                        + "x"
                        + (displaySizeY = ImGui.getIO().getDisplaySizeY())
                        + "@"
                        + (px1 = EfnWidgets.s(1.0F));
        if (!string.equals(this.panelLayoutKey)) {
            this.panelLayoutKey = string;
            ClickGuiRenderer.PanelLayout panelLayout = this.panelLayout(categoryPanels);
            float px212 = EfnWidgets.s(212.0F);
            float f = displaySizeY - EfnWidgets.s(70.0F) - EfnWidgets.s(42.0F);
            ArrayList list = new ArrayList();
            ArrayList list2 = new ArrayList();
            for (int i = 0; i < panelLayout.ids().size(); i++) {
                String string2 = panelLayout.ids().get(i);
                float[] panelHome = this.panelHomes.get(string2);
                float g = panelHome[0] * px1;
                float h = panelHome[1] * px1;
                if (g >= 0.0F && h >= 0.0F && g + px212 <= displaySizeX && h <= f) {
                    this.panelTargets.put(string2, new float[] {g, h});
                    list.add(new float[] {g, h, px212, panelLayout.heights().get(i)});
                } else {
                    list2.add(i);
                }
            }
            for (int j : (Iterable<Integer>) list2) {
                float height = panelLayout.heights().get(j);
                float[] floats = freeSpot(height, list);
                this.panelTargets.put(panelLayout.ids().get(j), floats);
                list.add(new float[] {floats[0], floats[1], px212, height});
            }
        }
    }

    private static float[] bundledSpot(String string) {
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY;
        float[] floats =
                BundledDefaults.panelSpot(
                        string, displaySizeX, displaySizeY = ImGui.getIO().getDisplaySizeY());
        if (floats == null) {
            return null;
        } else {
            floats[0] = Math.max(0.0F, Math.min(floats[0], displaySizeX - EfnWidgets.s(212.0F)));
            floats[1] = Math.max(0.0F, Math.min(floats[1], displaySizeY - EfnWidgets.s(42.0F)));
            return floats;
        }
    }

    private static float[] panelPosFor(int i, int j) {
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        float px212 = EfnWidgets.s(212.0F);
        float px14 = EfnWidgets.s(14.0F);
        int k = Math.max(1, (int) ((displaySizeX - EfnWidgets.s(40.0F) + px14) / (px212 + px14)));
        int m = Math.min(k, j);
        float f = m * (px212 + px14) - px14;
        float g = Math.max(EfnWidgets.s(20.0F), (displaySizeX - f) * 0.5F);
        return new float[] {
            g + i % k * (px212 + px14), EfnWidgets.s(28.0F) + i / k * displaySizeY * 0.45F
        };
    }

    private static float[] freeSpot(float f, List<float[]> list) {
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        float px212 = EfnWidgets.s(212.0F);
        float px14 = EfnWidgets.s(14.0F);
        float px20 = EfnWidgets.s(20.0F);
        float g = displaySizeY - EfnWidgets.s(70.0F) - px14;
        float h = 3.4028235E38F;
        for (float[] floats : list) {
            h = Math.min(h, floats[1]);
        }
        float[] floats2 = null;
        for (float[] floats3 : list) {
            boolean bl = floats3[1] < h + EfnWidgets.s(42.0F);
            if (bl && (floats2 == null || floats3[0] + floats3[2] > floats2[0] + floats2[2])) {
                floats2 = floats3;
            }
        }
        float[] floats4;
        if (floats2 == null) {
            float[] floats5 = new float[] {px20, 0.0F};
            floats4 = floats5;
            floats5[1] = EfnWidgets.s(28.0F);
        } else {
            float[] floats6 = new float[] {floats2[0] + floats2[2] + px14, 0.0F};
            floats4 = floats6;
            floats6[1] = floats2[1];
        }
        ArrayList list2 = new ArrayList();
        list2.add(new float[] {px20, EfnWidgets.s(28.0F)});
        for (float[] floats7 : list) {
            list2.add(new float[] {floats7[0] + floats7[2] + px14, floats7[1]});
            list2.add(new float[] {floats7[0], floats7[1] + floats7[3] + px14});
        }
        list2.sort(
                Comparator.<float[]>comparingDouble(fs -> fs[1]).thenComparingDouble(fs -> fs[0]));
        list2.addFirst(floats4);
        for (float[] floats8 : (Iterable<float[]>) list2) {
            boolean bl2 =
                    floats8[0] >= 0.0F
                            && floats8[1] >= 0.0F
                            && floats8[0] + px212 <= displaySizeX - px20
                            && floats8[1] + f <= g;
            if (bl2 && coveredArea(floats8[0], floats8[1], f, list) == 0.0F) {
                return floats8;
            }
        }
        float[] floats9 = null;
        float f2 = 3.4028235E38F;
        for (float[] floats10 : (Iterable<float[]>) list2) {
            float g2 = Math.max(0.0F, Math.min(floats10[0], displaySizeX - px212 - px20));
            float h2;
            float coveredArea =
                    coveredArea(
                            g2,
                            h2 = Math.max(0.0F, Math.min(floats10[1], displaySizeY - f - px20)),
                            f,
                            list);
            if (coveredArea < f2) {
                floats9 = new float[] {g2, h2};
                f2 = coveredArea;
            }
        }
        return floats9;
    }

    private static float coveredArea(float f, float g, float h, List<float[]> list) {
        float px212 = EfnWidgets.s(212.0F);
        float f2 = EfnWidgets.s(14.0F) * 0.5F;
        float g2 = 0.0F;
        for (float[] floats : list) {
            float h2 = Math.min(f + px212, floats[0] + floats[2]) + f2 - Math.max(f, floats[0]);
            float f3 = Math.min(g + h, floats[1] + floats[3]) + f2 - Math.max(g, floats[1]);
            if (h2 > 0.0F && f3 > 0.0F) {
                g2 += h2 * f3;
            }
        }
        return g2;
    }

    private static Map<String, float[]> savedPanelSpots() {
        HashMap map = new HashMap();
        String iniFilename = ImGui.getIO().getIniFilename();
        if (iniFilename != null && Files.isRegularFile(Path.of(iniFilename))) {
            String string = "[Window][";
            try {
                String string2 = null;
                for (String string3 : Files.readAllLines(Path.of(iniFilename))) {
                    if (string3.startsWith("[")) {
                        boolean bl =
                                string3.startsWith(string + "##panel_") && string3.endsWith("]");
                        string2 =
                                bl
                                        ? string3.substring(string.length(), string3.length() - 1)
                                        : null;
                    } else if (string2 != null && string3.startsWith("Pos=")) {
                        String[] strings = string3.substring(4).split(",");
                        map.put(
                                string2,
                                new float[] {
                                    Float.parseFloat(strings[0]), Float.parseFloat(strings[1])
                                });
                    }
                }
            } catch (RuntimeException | IOException exception) {
                map.clear();
            }
            return map;
        } else {
            return map;
        }
    }

    private List<ClientModule> modulesFor(ModuleCategory moduleCategory) {
        return ModuleRegistry.inCategory(moduleCategory);
    }

    private List<ClientModule> searchModules(String cachedGlobalSearch2) {
        int revision = ModuleRegistry.revision();
        if (revision != this.cachedRegistryRevision) {
            this.cachedRegistryRevision = revision;
            this.cachedGlobalSearch = "";
            this.cachedGlobalResults = List.of();
        }
        if (cachedGlobalSearch2.equals(this.cachedGlobalSearch)) {
            return this.cachedGlobalResults;
        } else {
            ArrayList list = new ArrayList();
            for (ClientModule module : ModuleRegistry.all()) {
                if (module.name().toLowerCase(Locale.ROOT).contains(cachedGlobalSearch2)) {
                    list.add(module);
                }
            }
            this.cachedGlobalSearch = cachedGlobalSearch2;
            this.cachedGlobalResults = List.copyOf(list);
            return this.cachedGlobalResults;
        }
    }

    private static String normalizeSearch(String string) {
        return string == null ? "" : string.toLowerCase(Locale.ROOT).trim();
    }

    private void renderTabBar(float f, float g) {
        ClickGuiRenderer.Tab[] tabs = ClickGuiRenderer.Tab.values();
        ImFont font = ImGui.getFont();
        float px44 = EfnWidgets.s(44.0F);
        float px4 = EfnWidgets.s(4.0F);
        float px20 = EfnWidgets.s(20.0F);
        float[] floats = new float[tabs.length];
        float h = px4 * 2.0F;
        for (int i = 0; i < tabs.length; i++) {
            floats[i] = EfnWidgets.textWidth(font, tabs[i].label) + px20 * 2.0F;
            h += floats[i];
        }
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        float f2 = (displaySizeX - h) * 0.5F;
        float g2 = displaySizeY - px44 - EfnWidgets.s(26.0F) + EfnWidgets.s(16.0F) * (1.0F - g);
        ImGui.setNextWindowPos(f2, g2, 1);
        ImGui.setNextWindowSize(h, px44, 1);
        ImGui.pushStyleVar(0, Math.max(0.01F, g));
        ImGui.pushStyleVar(2, 0.0F, 0.0F);
        ImGui.pushStyleVar(3, px44 * 0.5F);
        ImGui.pushStyleColor(2, GuiPalette.tabBarBg());
        if (ImGui.begin("##tabbar", 319)) {
            this.tabBarRect = new float[] {f2, g2, h, px44};
            if (this.tab == ClickGuiRenderer.Tab.THEME) {
                this.outlineSurface(ThemeStore.get().tabBar, f2, g2, h, px44, px44 * 0.5F);
            }
            drawShadow(f2, g2, h, px44, px44 * 0.5F, 4);
            ImDrawList windowDrawList = ImGui.getWindowDrawList();
            SurfaceBackground.draw(
                    windowDrawList, ThemeStore.get().tabBar, f2, g2, h, px44, px44 * 0.5F);
            windowDrawList.addRect(
                    f2, g2, f2 + h, g2 + px44, EfnWidgets.c(318767103), px44 * 0.5F, 0, 1.0F);
            float h2 = px4;
            for (int j = 0; j < this.tab.ordinal(); j++) {
                h2 += floats[j];
            }
            float f3 = floats[this.tab.ordinal()];
            if (this.tabIndicatorX < 0.0F) {
                this.tabIndicatorX = h2;
                this.tabIndicatorW = f3;
            }
            this.tabIndicatorX = smooth(this.tabIndicatorX, h2, f, 18.0F);
            this.tabIndicatorW = smooth(this.tabIndicatorW, f3, f, 18.0F);
            float g3 = px44 - px4 * 2.0F;
            windowDrawList.addRectFilled(
                    f2 + this.tabIndicatorX,
                    g2 + px4,
                    f2 + this.tabIndicatorX + this.tabIndicatorW,
                    g2 + px4 + g3,
                    EfnWidgets.c(GuiPalette.accent()),
                    g3 * 0.5F);
            float h3 = px4;
            for (int k = 0; k < tabs.length; k++) {
                ImGui.setCursorPos(h3, px4);
                if (ImGui.invisibleButton("##tab_" + tabs[k].name(), floats[k], g3)) {
                    this.switchTab(tabs[k]);
                }
                float animate = EfnWidgets.animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
                float f4 = f2 + h3 + floats[k] * 0.5F;
                float g4 =
                        Math.max(
                                0.0F,
                                1.0F
                                        - Math.abs(
                                                        f4
                                                                - (f2
                                                                        + this.tabIndicatorX
                                                                        + this.tabIndicatorW
                                                                                * 0.5F))
                                                / Math.max(1.0F, floats[k] * 0.5F));
                int lerpColor =
                        EfnTheme.lerpColor(EfnTheme.lerpColor(-4542808, -461582, animate), -1, g4);
                float textWidth = EfnWidgets.textWidth(font, tabs[k].label);
                EfnWidgets.text(
                        windowDrawList,
                        font,
                        f2 + h3 + (floats[k] - textWidth) * 0.5F,
                        g2 + (px44 - EfnWidgets.lineHeight(font)) * 0.5F,
                        lerpColor,
                        tabs[k].label,
                        this.tabBarGuard);
                h3 += floats[k];
            }
        }
        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleVar(3);
    }

    private float[] beginCard(String string, float f, float g) {
        return this.beginCard(string, f, g, ImGui.getIO().getDisplaySizeX() * 0.5F);
    }

    private float[] beginCard(String string, float f, float g, float h) {
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        float px86 = EfnWidgets.s(86.0F);
        float f2 = Math.min(displaySizeX - EfnWidgets.s(32.0F), EfnWidgets.s(460.0F));
        float g2 = Math.min(displaySizeY - px86 - EfnWidgets.s(32.0F), EfnWidgets.s(760.0F));
        float h2 = Math.max(EfnWidgets.s(16.0F), h - f2 * 0.5F);
        float f3 =
                Math.max(
                                EfnWidgets.s(16.0F),
                                (displaySizeY - px86 - g2) * 0.5F + EfnWidgets.s(8.0F))
                        + EfnWidgets.s(24.0F) * (1.0F - g);
        ImGui.setNextWindowPos(h2, f3, 1);
        ImGui.setNextWindowSize(f2, g2, 1);
        ImGui.pushStyleVar(0, f);
        ImGui.pushStyleVar(2, 0.0F, 0.0F);
        ImGui.pushStyleVar(14, 0.0F, 0.0F);
        ImGui.pushStyleVar(3, cardRounding());
        ImGui.pushStyleColor(2, GuiPalette.cardBg());
        if (this.cardTextLift) {
            ImGui.pushStyleColor(0, -1);
            ImGui.pushStyleColor(1, -2042416);
        }
        ImGui.begin(string, 8511);
        drawShadow(h2, f3, f2, g2, cardRounding(), 6);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        Surface cards = ThemeStore.get().cards;
        SurfaceBackground.draw(windowDrawList, cards, h2, f3, f2, g2, cardRounding());
        windowDrawList.addRect(
                h2, f3, h2 + f2, f3 + g2, EfnWidgets.c(318767103), cardRounding(), 0, 1.0F);
        float g3 =
                SurfaceBackground.hasImageBanner(cards)
                        ? Math.max(
                                EfnWidgets.s(22.0F),
                                SurfaceBackground.bannerHeight(cards) - EfnWidgets.s(44.0F))
                        : EfnWidgets.s(22.0F);
        return new float[] {f2, g2, g3, h2, f3};
    }

    private static float panelRounding() {
        return EfnWidgets.s(GuiPalette.rounding());
    }

    public static float cardRounding() {
        return EfnWidgets.s(Math.min(28.0F, GuiPalette.rounding() * 2.2F));
    }

    private void endCard() {
        ImGui.end();
        ImGui.popStyleColor(this.cardTextLift ? 3 : 1);
        ImGui.popStyleVar(4);
    }

    public static void drawCardTitle(String string, float f, float g) {
        drawCardTitle(string, f, g, false);
    }

    private static void drawCardTitle(String string, float f, float g, boolean bl) {
        ImFont imFont = ImGuiManager.getInstance().titleFont();
        EfnWidgets.text(
                ImGui.getWindowDrawList(),
                imFont,
                ImGui.getWindowPosX() + f,
                ImGui.getWindowPosY() + g - EfnWidgets.lineHeight(imFont) * 0.5F,
                -461582,
                string,
                bl);
    }

    public static float mutedParagraph(String string, float f, float g, float h) {
        ImGui.setCursorPos(f, g);
        ImGui.pushTextWrapPos(h);
        ImGui.pushStyleColor(0, ImGui.getStyleColorVec4(1));
        ImGui.textUnformatted(string);
        ImGui.popStyleColor();
        ImGui.popTextWrapPos();
        return ImGui.getCursorPosY();
    }

    public static void cardDivider(float f, float g, float h) {
        float windowPosX = ImGui.getWindowPosX();
        float windowPosY = ImGui.getWindowPosY();
        ImGui.getWindowDrawList()
                .addLine(
                        windowPosX + f,
                        windowPosY + h,
                        windowPosX + g - f,
                        windowPosY + h,
                        EfnWidgets.c(268435455),
                        1.0F);
    }

    private void renderModuleCard(ClientModule module, float f, float g, float h) {
        this.renderModuleCard(module, f, g, h, false);
    }

    private void renderModuleCard(ClientModule module, float f, float g, float h, boolean bl) {
        this.renderModuleCard(module, f, g, h, bl, !bl);
    }

    private void renderModuleCard(
            ClientModule module, float f, float g, float h, boolean bl, boolean bl2) {
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        float[] floats;
        if (bl) {
            floats =
                    this.beginCard(
                            "##preview_card", g, h, (displaySizeX - EfnWidgets.s(408.0F)) * 0.5F);
            this.cardRect = new float[] {floats[3], floats[4], floats[0], floats[1]};
            this.outlineSurface(
                    ThemeStore.get().cards,
                    floats[3],
                    floats[4],
                    floats[0],
                    floats[1],
                    cardRounding());
            ImGui.pushStyleVar(1, 1.0F);
            ImGui.beginDisabled();
        } else {
            ImGui.getBackgroundDrawList()
                    .addRectFilled(
                            0.0F,
                            0.0F,
                            displaySizeX,
                            displaySizeY,
                            EfnWidgets.withAlpha(1426063360, h));
            floats = this.beginCard("##module_card", g, h);
        }
        float f2 = floats[0];
        float g2 = floats[1];
        float px24 = EfnWidgets.s(24.0F);
        float h2 = floats[2];
        float px32 = EfnWidgets.s(32.0F);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        float windowPosX = ImGui.getWindowPosX();
        float windowPosY = ImGui.getWindowPosY();
        float f3 = px24;
        if (!bl && bl2) {
            ImGui.setCursorPos(px24 - EfnWidgets.s(4.0F), h2);
            if (EfnWidgets.iconButton("##back", px32)) {
                this.closeSettings();
            }
            EfnWidgets.chevronLeft(
                    windowDrawList,
                    windowPosX + px24 - EfnWidgets.s(4.0F) + px32 * 0.5F - EfnWidgets.s(1.0F),
                    windowPosY + h2 + px32 * 0.5F,
                    EfnWidgets.s(5.0F),
                    -461582);
            f3 = px24 + px32 + EfnWidgets.s(8.0F);
        }
        drawCardTitle(module.name(), f3, h2 + px32 * 0.5F, this.cardGuard);
        if (module.isAction()) {
            float px72 = EfnWidgets.s(72.0F);
            float px30 = EfnWidgets.s(30.0F);
            ImGui.setCursorPos(f2 - px24 - px72, h2 + (px32 - px30) * 0.5F);
            if (EfnWidgets.chipButton("##open", "Open", px72, px30, true)) {
                module.onClick();
            }
        } else {
            float px46 = EfnWidgets.s(46.0F);
            float px26 = EfnWidgets.s(26.0F);
            ImGui.setCursorPos(f2 - px24 - px46, h2 + (px32 - px26) * 0.5F);
            float cursorScreenPosX = ImGui.getCursorScreenPosX();
            float cursorScreenPosY = ImGui.getCursorScreenPosY();
            if (ImGui.invisibleButton("##enabled", px46, px26)) {
                module.toggle();
            }
            EfnWidgets.drawSwitch(
                    windowDrawList,
                    cursorScreenPosX,
                    cursorScreenPosY,
                    px46,
                    px26,
                    EfnWidgets.animate(ImGui.getItemID(), module.isEnabled(), 14.0F));
        }
        float g3 = h2 + px32 + EfnWidgets.s(12.0F);
        String description = module.description();
        if (description != null && !description.isBlank()) {
            g3 = mutedParagraph(description, px24, g3, f2 - px24) + EfnWidgets.s(4.0F);
        }
        float h3;
        cardDivider(px24, f2, h3 = g3 + EfnWidgets.s(12.0F));
        ImGui.setCursorPos(0.0F, h3 + 1.0F);
        if (ImGui.beginChild(
                "##body", f2, Math.max(1.0F, g2 - h3 - 1.0F - EfnWidgets.s(10.0F)), false, 24)) {
            float f4 = f2 - px24 * 2.0F;
            float g4 = ImGui.getCursorPosY() + EfnWidgets.s(16.0F);
            this.sectionLabel("Settings", px24);
            if (!bl && module.hasModifiedSettings()) {
                this.renderResetAllChip(module, px24, f4, g4);
            }
            boolean bl3 = false;
            for (Setting setting : module.settings()) {
                if (setting.isVisible() && (!module.isAction() || setting != module.showInHud())) {
                    bl3 = true;
                    ImGui.pushID(setting.id());
                    this.renderSetting(module, setting, px24, f4);
                    ImGui.popID();
                }
            }
            if (!bl3) {
                mutedParagraph(
                        "This module has no settings.",
                        px24,
                        ImGui.getCursorPosY() + EfnWidgets.s(8.0F),
                        f2 - px24);
            }
            this.sectionLabel("Keybind", px24);
            ImGui.pushID("##keybind_row");
            this.renderKeybindRow(module, px24, f4);
            ImGui.popID();
            if (isHudModule(module)) {
                this.renderHudActions(px24, f4);
            }
            if (module.inConfigs()) {
                this.sectionLabel("Config", px24);
                this.renderModuleConfigActions(module, px24, f4);
            }
            ImGui.dummy(1.0F, EfnWidgets.s(18.0F));
            this.applySmoothScroll("card_" + module.id(), f);
        }
        ImGui.endChild();
        if (bl) {
            ImGui.endDisabled();
            ImGui.popStyleVar();
        }
        this.endCard();
    }

    private void renderResetAllChip(ClientModule module, float f, float g, float h) {
        float cursorPosY = ImGui.getCursorPosY();
        String string = "reset:" + module.id();
        String string2 = this.isConfirming(string) ? "Confirm reset" : "Reset all";
        ImFont imFont = ImGuiManager.getInstance().smallFont();
        boolean pushFont = pushFont(imFont);
        float f2 = EfnWidgets.textWidth(imFont, string2) + EfnWidgets.s(18.0F);
        float g2 = EfnWidgets.lineHeight(imFont) + EfnWidgets.s(8.0F);
        ImGui.setCursorPos(f + g - f2, h + (ImGui.getTextLineHeight() - g2) * 0.5F);
        if (EfnWidgets.chipButton("##reset_all", string2, f2, g2, this.isConfirming(string))
                && this.confirmClick(string)) {
            module.resetSettings();
        }
        if (ImGui.isItemHovered(65536)) {
            showTooltip("Restore every setting to its default");
        }
        if (pushFont) {
            ImGui.popFont();
        }
        ImGui.setCursorPosY(cursorPosY);
    }

    private boolean confirmClick(String pendingConfirm2) {
        if (this.isConfirming(pendingConfirm2)) {
            this.pendingConfirm = null;
            return true;
        } else {
            this.pendingConfirm = pendingConfirm2;
            this.pendingConfirmAt = System.nanoTime();
            return false;
        }
    }

    private boolean isConfirming(String string) {
        return string.equals(this.pendingConfirm)
                && System.nanoTime() - this.pendingConfirmAt < 3000000000L;
    }

    private void sectionLabel(String string, float f) {
        ImGui.setCursorPos(f, ImGui.getCursorPosY() + EfnWidgets.s(16.0F));
        boolean pushFont = pushFont(ImGuiManager.getInstance().smallFont());
        ImGui.pushStyleColor(0, ImGui.getStyleColorVec4(1));
        ImGui.textUnformatted(string);
        ImGui.popStyleColor();
        if (pushFont) {
            ImGui.popFont();
        }
        ImGui.setCursorPosY(ImGui.getCursorPosY() + EfnWidgets.s(4.0F));
        this.dividerPending = false;
    }

    private float rowStart(float f, float g) {
        float cursorPosY = ImGui.getCursorPosY();
        if (this.dividerPending) {
            float windowPosX = ImGui.getWindowPosX();
            float cursorScreenPosY = ImGui.getCursorScreenPosY();
            ImGui.getWindowDrawList()
                    .addLine(
                            windowPosX + f,
                            cursorScreenPosY,
                            windowPosX + f + g,
                            cursorScreenPosY,
                            EfnWidgets.c(268435455),
                            1.0F);
        }
        this.dividerPending = true;
        return cursorPosY;
    }

    public static void rowLabel(String string, float f, float g, float h) {
        ImGui.setCursorPos(f, g + (h - ImGui.getTextLineHeight()) * 0.5F);
        ImGui.textUnformatted(string);
    }

    private void openSettings(ClientModule settingsModule2) {
        this.settingsModule = settingsModule2;
        this.viewChangedAt = System.nanoTime();
        this.cardJustOpened = true;
    }

    private void closeSettings() {
        if (this.settingsModule != null) {
            this.viewChangedAt = System.nanoTime();
        }
        this.settingsModule = null;
        this.listeningModule = null;
        this.listeningKeySetting = null;
        this.activeColorModule = null;
        this.activeColorSetting = null;
        this.colorPickerOpen.set(false);
        this.blockLists.close();
        this.itemPickers.close();
        this.itemColorWindowOpen = false;
    }

    private void closeCardOnOutsideClick() {
        if (this.cardJustOpened) {
            this.cardJustOpened = false;
        } else if (this.settingsModule != null
                && this.listeningModule == null
                && this.listeningKeySetting == null) {
            if (ImGui.isMouseClicked(0) && !ImGui.isWindowHovered(4) && !ImGui.isAnyItemActive()) {
                this.closeSettings();
            }
        }
    }

    private void renderFriendsCard(float f, float g, float h) {
        float[] floats = this.beginCard("##friends_card", g, h);
        float f2 = floats[0];
        float g2 = floats[1];
        float px24 = EfnWidgets.s(24.0F);
        float h2 = floats[2];
        float px32 = EfnWidgets.s(32.0F);
        FriendStore friendStore = FriendStore.getInstance();
        List list = friendStore.all();
        drawCardTitle("Friends", px24, h2 + px32 * 0.5F, this.cardGuard);
        String string = list.size() == 1 ? "1 player" : list.size() + " players";
        ImFont imFont = ImGuiManager.getInstance().smallFont();
        EfnWidgets.text(
                ImGui.getWindowDrawList(),
                imFont,
                ImGui.getWindowPosX() + f2 - px24 - EfnWidgets.textWidth(imFont, string),
                ImGui.getWindowPosY() + h2 + (px32 - EfnWidgets.lineHeight(imFont)) * 0.5F,
                -4542808,
                string,
                this.cardGuard);
        float f3 =
                mutedParagraph(
                                "Add players by their exact username.",
                                px24,
                                h2 + px32 + EfnWidgets.s(12.0F),
                                f2 - px24)
                        + EfnWidgets.s(14.0F);
        float px84 = EfnWidgets.s(84.0F);
        float g3 = ImGui.getTextLineHeight() + EfnWidgets.s(16.0F);
        ImGui.setCursorPos(px24, f3);
        ImGui.pushStyleVar(11, EfnWidgets.s(12.0F), EfnWidgets.s(8.0F));
        ImGui.setNextItemWidth(f2 - px24 * 2.0F - px84 - EfnWidgets.s(8.0F));
        if (this.focusFriendInput) {
            ImGui.setKeyboardFocusHere();
            this.focusFriendInput = false;
        }
        ImGui.inputTextWithHint("##friend_input", "Username", this.friendInput, 16);
        boolean submittedWithEnter = EfnWidgets.submittedWithEnter();
        ImGui.popStyleVar();
        ImGui.setCursorPos(f2 - px24 - px84, f3);
        if (EfnWidgets.chipButton("##friend_add", "Add", px84, g3, true)) {
            submittedWithEnter = true;
        }
        if (submittedWithEnter) {
            this.tryAddFriend(friendStore);
        }
        f3 += g3 + EfnWidgets.s(8.0F);
        if (this.friendMessage != null && elapsed(this.friendMessageAt, 2500.0F) < 1.0F) {
            ImGui.setCursorPos(px24, f3);
            boolean pushFont = pushFont(imFont);
            ImGui.pushStyleColor(0, -10856974);
            ImGui.textUnformatted(this.friendMessage);
            ImGui.popStyleColor();
            if (pushFont) {
                ImGui.popFont();
            }
        }
        float h3;
        cardDivider(px24, f2, h3 = f3 + (EfnWidgets.lineHeight(imFont) + EfnWidgets.s(10.0F)));
        ImGui.setCursorPos(0.0F, h3 + 1.0F);
        if (ImGui.beginChild(
                "##friend_list",
                f2,
                Math.max(1.0F, g2 - h3 - 1.0F - EfnWidgets.s(10.0F)),
                false,
                24)) {
            if (list.isEmpty()) {
                String string2 = "No friends yet";
                ImGui.setCursorPos(
                        (f2 - ImGui.calcTextSize(string2).x) * 0.5F, EfnWidgets.s(40.0F));
                ImGui.textDisabled(string2);
            } else {
                ImGui.dummy(1.0F, EfnWidgets.s(8.0F));
                for (String string3 : (Iterable<String>) list) {
                    this.renderFriendRow(friendStore, string3, px24, f2 - px24 * 2.0F);
                }
            }
            ImGui.dummy(1.0F, EfnWidgets.s(12.0F));
            this.applySmoothScroll("friends", f);
        }
        ImGui.endChild();
        this.endCard();
    }

    private void renderFriendRow(FriendStore friendStore, String string, float f, float g) {
        ImGui.pushID("friend_" + string);
        float px48 = EfnWidgets.s(48.0F);
        float cursorPosY = ImGui.getCursorPosY();
        ImGui.setCursorPos(f, cursorPosY);
        float cursorScreenPosX = ImGui.getCursorScreenPosX();
        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        ImGui.setNextItemAllowOverlap();
        ImGui.invisibleButton("##row", g, px48);
        boolean mouseHoveringRect =
                ImGui.isMouseHoveringRect(
                        cursorScreenPosX,
                        cursorScreenPosY,
                        cursorScreenPosX + g,
                        cursorScreenPosY + px48);
        float animate = EfnWidgets.animate(ImGui.getItemID(), mouseHoveringRect, 18.0F);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        if (animate > 0.01F) {
            windowDrawList.addRectFilled(
                    cursorScreenPosX - EfnWidgets.s(8.0F),
                    cursorScreenPosY,
                    cursorScreenPosX + g + EfnWidgets.s(8.0F),
                    cursorScreenPosY + px48,
                    EfnWidgets.c(EfnWidgets.withAlpha(218103807, animate)),
                    EfnWidgets.s(10.0F));
        }
        float px15 = EfnWidgets.s(15.0F);
        float h = cursorScreenPosX + px15;
        float f2 = cursorScreenPosY + px48 * 0.5F;
        int i =
                AVATAR_COLORS[
                        Math.floorMod(
                                string.toLowerCase(Locale.ROOT).hashCode(), AVATAR_COLORS.length)];
        windowDrawList.addCircleFilled(h, f2, px15, EfnWidgets.c(i), 32);
        ImFont imFont = ImGuiManager.getInstance().categoryFont();
        String string2 = string.substring(0, 1).toUpperCase(Locale.ROOT);
        EfnWidgets.text(
                windowDrawList,
                imFont,
                h - EfnWidgets.textWidth(imFont, string2) * 0.5F,
                f2 - EfnWidgets.lineHeight(imFont) * 0.5F,
                -1,
                string2);
        ImFont font = ImGui.getFont();
        EfnWidgets.text(
                windowDrawList,
                font,
                cursorScreenPosX + px15 * 2.0F + EfnWidgets.s(12.0F),
                cursorScreenPosY + (px48 - EfnWidgets.lineHeight(font)) * 0.5F,
                -461582,
                string);
        float px28 = EfnWidgets.s(28.0F);
        ImGui.setCursorPos(f + g - px28, cursorPosY + (px48 - px28) * 0.5F);
        float cursorScreenPosX2 = ImGui.getCursorScreenPosX();
        float cursorScreenPosY2 = ImGui.getCursorScreenPosY();
        if (ImGui.invisibleButton("##remove", px28, px28)) {
            friendStore.remove(string);
        }
        float animate2 = EfnWidgets.animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
        float g2 = cursorScreenPosX2 + px28 * 0.5F;
        float h2 = cursorScreenPosY2 + px28 * 0.5F;
        if (animate2 > 0.01F) {
            windowDrawList.addCircleFilled(
                    g2,
                    h2,
                    px28 * 0.5F,
                    EfnWidgets.c(EfnWidgets.withAlpha(-10856974, 0.18F * animate2)),
                    32);
        }
        EfnWidgets.cross(
                windowDrawList,
                g2,
                h2,
                EfnWidgets.s(4.5F),
                EfnTheme.lerpColor(
                        EfnWidgets.withAlpha(-4542808, 0.25F + 0.75F * animate),
                        -10856974,
                        animate2));
        ImGui.setCursorPos(0.0F, cursorPosY + px48 + EfnWidgets.s(4.0F));
        ImGui.popID();
    }

    private void tryAddFriend(FriendStore friendStore) {
        String string = this.friendInput.get().trim();
        if (!FriendStore.isValidName(string)) {
            this.friendMessage = "Use 1-16 letters, digits or _ (Bedrock names start with .)";
        } else if (friendStore.isFriend(string)) {
            this.friendMessage = string + " is already on your list";
        } else {
            friendStore.add(string);
            this.friendInput.set("");
            this.friendMessage = null;
        }
        this.friendMessageAt = System.nanoTime();
        this.focusFriendInput = true;
    }

    private void renderConfigCard(float f, float g, float h) {
        float[] floats = this.beginCard("##config_card", g, h);
        float f2 = floats[0];
        float g2 = floats[1];
        float px24 = EfnWidgets.s(24.0F);
        float h2 = floats[2];
        float px32 = EfnWidgets.s(32.0F);
        float f3 = f2 - px24 * 2.0F;
        ModuleConfigStore moduleConfigStore = ModuleConfigStore.getInstance();
        ConfigProfiles configProfiles = ConfigProfiles.getInstance();
        ImFont imFont = ImGuiManager.getInstance().smallFont();
        drawCardTitle("Config", px24, h2 + px32 * 0.5F, this.cardGuard);
        String string =
                moduleConfigStore.isReadOnly()
                        ? "Read-only"
                        : (moduleConfigStore.hasUnsavedChanges() ? "Saving..." : "Saved");
        int i = moduleConfigStore.isReadOnly() ? -10856974 : -4542808;
        EfnWidgets.text(
                ImGui.getWindowDrawList(),
                imFont,
                ImGui.getWindowPosX() + f2 - px24 - EfnWidgets.textWidth(imFont, string),
                ImGui.getWindowPosY() + h2 + (px32 - EfnWidgets.lineHeight(imFont)) * 0.5F,
                i,
                string,
                this.cardGuard);
        if (moduleConfigStore.isReadOnly()
                && ImGui.isMouseHoveringRect(
                        ImGui.getWindowPosX() + f2 - px24 - EfnWidgets.s(80.0F),
                        ImGui.getWindowPosY() + h2,
                        ImGui.getWindowPosX() + f2 - px24,
                        ImGui.getWindowPosY() + h2 + px32)) {
            showTooltip("modules.json could not be read. Fix or delete it.");
        }
        float g3 =
                mutedParagraph(
                                "Changes save automatically. Profiles can be shared with a code.",
                                px24,
                                h2 + px32 + EfnWidgets.s(12.0F),
                                f2 - px24)
                        + EfnWidgets.s(14.0F);
        float px8 = EfnWidgets.s(8.0F);
        float h3 = ImGui.getTextLineHeight() + EfnWidgets.s(16.0F);
        float f4 = (f3 - px8 * 2.0F) / 3.0F;
        ImGui.setCursorPos(px24, g3);
        if (EfnWidgets.chipButton("##cfg_save", "Save all", f4, h3, true)) {
            this.saveAllConfigs(moduleConfigStore);
        }
        if (ImGui.isItemHovered(65536)) {
            showTooltip("Save everything now");
        }
        ImGui.setCursorPos(px24 + f4 + px8, g3);
        if (EfnWidgets.chipButton("##cfg_folder", "Open folder", f4, h3, false)) {
            this.openProfilesFolder(configProfiles);
        }
        ImGui.setCursorPos(px24 + (f4 + px8) * 2.0F, g3);
        boolean confirming = this.isConfirming("reset-all-modules");
        if (EfnWidgets.chipButton(
                        "##cfg_reset", confirming ? "Confirm" : "Reset all", f4, h3, confirming)
                && this.confirmClick("reset-all-modules")) {
            for (ClientModule module : ModuleRegistry.all()) {
                module.resetSettings();
            }
            this.setConfigMessage("Every module setting is back to default", false);
        }
        if (ImGui.isItemHovered(65536)) {
            showTooltip("Reset all module settings. Keybinds stay.");
        }
        float g4;
        cardDivider(px24, f2, g4 = g3 + (h3 + EfnWidgets.s(18.0F)));
        float px84 = EfnWidgets.s(84.0F);
        ImGui.setCursorPos(px24, g3 = g4 + EfnWidgets.s(14.0F));
        ImGui.pushStyleVar(11, EfnWidgets.s(12.0F), EfnWidgets.s(8.0F));
        ImGui.setNextItemWidth(f3 - px84 - px8);
        if (this.focusProfileInput) {
            ImGui.setKeyboardFocusHere();
            this.focusProfileInput = false;
        }
        ImGui.inputTextWithHint("##profile_input", "New profile name", this.profileInput);
        boolean submittedWithEnter = EfnWidgets.submittedWithEnter();
        ImGui.popStyleVar();
        ImGui.setCursorPos(f2 - px24 - px84, g3);
        if (EfnWidgets.chipButton("##profile_create", "Create", px84, h3, true)) {
            submittedWithEnter = true;
        }
        if (ImGui.isItemHovered(65536)) {
            showTooltip("Save your current modules as a new profile");
        }
        if (submittedWithEnter) {
            this.tryCreateProfile(configProfiles);
        }
        float h4 = (f3 - px8) * 0.5F;
        float f5;
        ImGui.setCursorPos(px24, f5 = g3 + (h3 + EfnWidgets.s(8.0F)));
        if (EfnWidgets.chipButton("##profile_import", "Import file", h4, h3, false)) {
            this.importProfileFile();
        }
        if (ImGui.isItemHovered(65536)) {
            showTooltip("Import a profile .json file. You can also drop it here.");
        }
        ImGui.setCursorPos(px24 + h4 + px8, f5);
        if (EfnWidgets.chipButton("##profile_paste", "Paste code", h4, h3, false)) {
            this.pasteProfileCode();
        }
        if (ImGui.isItemHovered(65536)) {
            showTooltip(
                    "Load a share code from your clipboard. Your current settings go to Auto"
                            + " backup.");
        }
        g3 = f5 + (h3 + EfnWidgets.s(8.0F));
        if (this.configMessage != null && elapsed(this.configMessageAt, 2500.0F) < 1.0F) {
            ImGui.setCursorPos(px24, g3);
            boolean pushFont = pushFont(imFont);
            ImGui.pushStyleColor(0, this.configMessageError ? -10856974 : -4542808);
            ImGui.textUnformatted(this.configMessage);
            ImGui.popStyleColor();
            if (pushFont) {
                ImGui.popFont();
            }
        }
        float g5;
        cardDivider(px24, f2, g5 = g3 + (EfnWidgets.lineHeight(imFont) + EfnWidgets.s(10.0F)));
        List list = configProfiles.list();
        ImGui.setCursorPos(0.0F, g5 + 1.0F);
        if (ImGui.beginChild(
                "##profile_list",
                f2,
                Math.max(1.0F, g2 - g5 - 1.0F - EfnWidgets.s(10.0F)),
                false,
                24)) {
            if (list.isEmpty()) {
                String string2 = "No profiles yet";
                ImGui.setCursorPos(
                        (f2 - ImGui.calcTextSize(string2).x) * 0.5F, EfnWidgets.s(40.0F));
                ImGui.textDisabled(string2);
            } else {
                ImGui.dummy(1.0F, EfnWidgets.s(8.0F));
                this.profileList.render(
                        configProfiles, list, moduleConfigStore.activeProfile(), px24, f3);
            }
            ImGui.dummy(1.0F, EfnWidgets.s(12.0F));
            float takeScrollTarget = this.profileList.takeScrollTarget();
            if (!Float.isNaN(takeScrollTarget)) {
                this.scroll.computeIfAbsent("profiles", value -> new float[2])[1] =
                        takeScrollTarget;
            }
            this.applySmoothScroll("profiles", f);
        }
        ImGui.endChild();
        this.endCard();
    }

    private void tryCreateProfile(ConfigProfiles configProfiles) {
        try {
            String create = configProfiles.create(this.profileInput.get());
            this.profileInput.set("");
            this.profileList.open(create);
            this.setConfigMessage("Created " + create + " from the current modules", false);
        } catch (ProfileException exception) {
            this.setConfigMessage(exception.getMessage(), true);
        }
        this.focusProfileInput = true;
    }

    private void importProfileFile() {
        if (FileDialogs.busy()) {
            this.setConfigMessage("A file window is already open", true);
        } else {
            FileDialogs.openProfile("Import an EFN profile", this::importProfile);
        }
    }

    private void importProfile(Path path) {
        try {
            String importFile = ConfigProfiles.getInstance().importFile(path);
            this.profileList.open(importFile);
            this.setConfigMessage("Imported " + importFile + " - press Load to use it", false);
        } catch (ProfileException exception) {
            this.setConfigMessage(exception.getMessage(), true);
        }
    }

    private void pasteProfileCode() {
        ConfigProfiles configProfiles = ConfigProfiles.getInstance();
        try {
            String importCode =
                    configProfiles.importCode(
                            MinecraftClient.getInstance().keyboard.getClipboard());
            ConfigProfiles.Profile profile = configProfiles.find(importCode);
            configProfiles.backup(
                    profile == null ? Set.of(ConfigProfiles.Part.MODULES) : profile.parts());
            configProfiles.load(importCode);
            this.profileList.open(importCode);
            this.setConfigMessage(
                    "Loaded " + importCode + " - your previous settings are in Auto backup", false);
        } catch (ProfileException exception) {
            this.setConfigMessage(exception.getMessage(), true);
        }
    }

    private void saveAllConfigs(ModuleConfigStore moduleConfigStore) {
        moduleConfigStore.save();
        ThemeStore.get().save();
        HudTheme.get().save();
        HudLayoutStore.getInstance().save();
        FriendStore.getInstance().save();
        if (moduleConfigStore.isReadOnly()) {
            this.setConfigMessage("modules.json is unreadable, so it was left untouched", true);
        } else {
            this.setConfigMessage("Everything saved", false);
        }
    }

    private void openProfilesFolder(ConfigProfiles configProfiles) {
        try {
            Path path = configProfiles.directory();
            Files.createDirectories(path);
            Util.getOperatingSystem().open(path);
        } catch (RuntimeException | IOException exception) {
            this.setConfigMessage("Could not open the profiles folder", true);
        }
    }

    private void setConfigMessage(String configMessage2, boolean configMessageError2) {
        this.configMessage = configMessage2;
        this.configMessageError = configMessageError2;
        this.configMessageAt = System.nanoTime();
    }

    private void renderThemeStudio(float f, float g, float h) {
        ThemeStore themeStore = ThemeStore.get();
        float easeOut = easeOut(elapsed(this.previewChangedAt, 260.0F));
        this.panelRects.clear();
        this.cardRect = null;
        this.inspectMode = true;
        if (this.themeSurface == themeStore.cards) {
            ClientModule module = previewModule();
            if (module != null) {
                this.renderModuleCard(module, f, g * easeOut, Math.min(h, easeOut), true);
            }
        } else {
            this.renderModulePanels(f, g * easeOut);
        }
        this.inspectMode = false;
        this.renderInspector(themeStore, f, g);
        this.highlightSurfaces(themeStore);
    }

    private void renderInspector(ThemeStore themeStore, float f, float g) {
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        float px360 = EfnWidgets.s(360.0F);
        float h =
                Math.min(
                        displaySizeY - EfnWidgets.s(86.0F) - EfnWidgets.s(40.0F),
                        EfnWidgets.s(760.0F));
        ImGui.setNextWindowPos(displaySizeX - px360 - EfnWidgets.s(24.0F), EfnWidgets.s(24.0F), 4);
        ImGui.setNextWindowSize(px360, h, 1);
        if (this.focusInspector) {
            ImGui.setNextWindowFocus();
            this.focusInspector = false;
        }
        ImGui.pushStyleVar(0, g);
        ImGui.pushStyleVar(2, 0.0F, 0.0F);
        ImGui.pushStyleVar(14, 0.0F, 0.0F);
        ImGui.pushStyleVar(3, cardRounding());
        ImGui.pushStyleColor(2, GuiPalette.abgr(themeStore.cards.color.argbWithAlpha(255)));
        byte b = 59;
        if (ImGui.begin("##theme_inspector", b)) {
            float windowPosX = ImGui.getWindowPosX();
            float windowPosY = ImGui.getWindowPosY();
            this.inspectorRect = new float[] {windowPosX, windowPosY, px360, h};
            drawShadow(windowPosX, windowPosY, px360, h, cardRounding(), 6);
            ImGui.getWindowDrawList()
                    .addRect(
                            windowPosX,
                            windowPosY,
                            windowPosX + px360,
                            windowPosY + h,
                            EfnWidgets.c(318767103),
                            cardRounding(),
                            0,
                            1.0F);
            float px20 = EfnWidgets.s(20.0F);
            float px202 = EfnWidgets.s(20.0F);
            drawCardTitle("Theme", px20, px202 + EfnWidgets.s(14.0F));
            float f2 = px202 + EfnWidgets.s(34.0F);
            if (this.themeMessage != null
                    && System.nanoTime() - this.themeMessageAt < 3000000000L) {
                ImGui.setCursorPos(px20, f2);
                ImGui.pushTextWrapPos(px360 - px20);
                ImGui.pushStyleColor(0, this.themeMessageError ? -10856974 : -461582);
                ImGui.textUnformatted(this.themeMessage);
                ImGui.popStyleColor();
                ImGui.popTextWrapPos();
                f2 = ImGui.getCursorPosY();
            } else {
                f2 =
                        mutedParagraph(
                                "Click a panel or card on screen to edit it.",
                                px20,
                                f2,
                                px360 - px20);
            }
            f2 =
                    this.renderSurfaceTabs(
                                    themeStore, px20, f2 + EfnWidgets.s(12.0F), px360 - px20 * 2.0F)
                            + EfnWidgets.s(14.0F);
            cardDivider(px20, px360, f2);
            ImGui.setCursorPos(0.0F, f2 + 1.0F);
            if (ImGui.beginChild(
                    "##inspector_body",
                    px360,
                    Math.max(1.0F, h - f2 - 1.0F - EfnWidgets.s(10.0F)),
                    false,
                    24)) {
                float g2 = px360 - px20 * 2.0F;
                if (this.themeSurface == null) {
                    this.renderGlobalOptions(themeStore, px20, g2);
                } else {
                    this.renderSurfaceOptions(this.themeSurface, px20, g2);
                }
                ImGui.dummy(1.0F, EfnWidgets.s(18.0F));
                this.applySmoothScroll("inspector", f);
            }
            ImGui.endChild();
        }
        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleVar(4);
    }

    private float renderSurfaceTabs(ThemeStore themeStore, float f, float g, float h) {
        String[] strings = new String[] {"Global", "Panels", "Cards", "Tab bar"};
        Surface[] surfaces =
                new Surface[] {null, themeStore.panels, themeStore.cards, themeStore.tabBar};
        int i = 0;
        for (int j = 1; j < surfaces.length; j++) {
            if (surfaces[j] == this.themeSurface) {
                i = j;
            }
        }
        float px32 = EfnWidgets.s(32.0F);
        ImGui.setCursorPos(f, g);
        int segmented = EfnWidgets.segmented("##surface", strings, i, h, px32);
        if (segmented >= 0) {
            this.selectSurface(surfaces[segmented]);
        }
        return g + px32;
    }

    private void renderGlobalOptions(ThemeStore themeStore, float f, float g) {
        this.sectionLabel("Presets", f);
        this.renderPresetRow(themeStore, f, g);
        this.sectionLabel("Colors", f);
        this.settingRows(f, g, themeStore.accent);
        this.sectionLabel("Layout", f);
        this.settingRows(
                f,
                g,
                themeStore.rounding,
                themeStore.moduleDividers,
                themeStore.backgroundDim,
                themeStore.menuBlur);
        this.sectionLabel("Interface", f);
        this.settingRows(
                f,
                g,
                themeStore.uiScale,
                themeStore.font,
                themeStore.animationSpeed,
                themeStore.animateLines,
                themeStore.customMainMenu,
                themeStore.communityPopup);
        this.sectionLabel("Share", f);
        this.renderThemeActions(themeStore, f, g);
    }

    private void renderSurfaceOptions(Surface surface, float f, float g) {
        this.sectionLabel("Fill", f);
        this.settingRows(f, g, surface.color, surface.opacity);
        this.sectionLabel("Background", f);
        this.settingRows(f, g, surface.background);
        if (surface.isImage()) {
            this.renderImageGallery(surface, f, g);
        }
        this.settingRows(
                f,
                g,
                surface.mapping,
                surface.coverage,
                surface.headerHeight,
                surface.imageOpacity,
                surface.imageScale,
                surface.imageX,
                surface.imageY,
                surface.lineOpacity);
        if (surface.isScreenMapped()) {
            mutedParagraph(
                    "Screen mapping pins the background to the screen, so each surface is a window"
                            + " onto it. Drag a panel to see another part.",
                    f,
                    ImGui.getCursorPosY() + EfnWidgets.s(12.0F),
                    f + g);
        }
    }

    private void selectSurface(Surface themeSurface2) {
        if (themeSurface2 != this.themeSurface) {
            ThemeStore themeStore = ThemeStore.get();
            if (themeSurface2 == themeStore.cards != (this.themeSurface == themeStore.cards)) {
                this.previewChangedAt = System.nanoTime();
            }
            this.themeSurface = themeSurface2;
            this.focusInspector = true;
        }
    }

    private void highlightSurfaces(ThemeStore themeStore) {
        float mousePosX = ImGui.getIO().getMousePosX();
        float mousePosY;
        boolean bl =
                contains(this.inspectorRect, mousePosX, mousePosY = ImGui.getIO().getMousePosY())
                        || ImGui.isPopupOpen("", 3072);
        Surface surface = null;
        if (!bl) {
            for (float[] panelRect : this.panelRects) {
                if (contains(panelRect, mousePosX, mousePosY)) {
                    surface = themeStore.panels;
                }
            }
            if (contains(this.cardRect, mousePosX, mousePosY)) {
                surface = themeStore.cards;
            }
            if (contains(this.tabBarRect, mousePosX, mousePosY)) {
                surface = themeStore.tabBar;
            }
        }
        this.hoveredSurface = surface;
        if (surface != null) {
            ImDrawList foregroundDrawList = ImGui.getForegroundDrawList();
            boolean bl2 = surface == themeStore.tabBar;
            String string =
                    surface.label
                            + (bl2 ? "  \u00b7  right-click to edit" : "  \u00b7  click to edit");
            ImFont imFont = ImGuiManager.getInstance().smallFont();
            float f = EfnWidgets.textWidth(imFont, string) + EfnWidgets.s(18.0F);
            float g = EfnWidgets.lineHeight(imFont) + EfnWidgets.s(10.0F);
            float h =
                    Math.min(
                            mousePosX + EfnWidgets.s(14.0F),
                            ImGui.getIO().getDisplaySizeX() - f - EfnWidgets.s(4.0F));
            float f2 =
                    Math.min(
                            mousePosY + EfnWidgets.s(18.0F),
                            ImGui.getIO().getDisplaySizeY() - g - EfnWidgets.s(4.0F));
            foregroundDrawList.addRectFilled(h, f2, h + f, f2 + g, GuiPalette.accent(), g * 0.5F);
            EfnWidgets.text(
                    foregroundDrawList,
                    imFont,
                    h + EfnWidgets.s(9.0F),
                    f2 + EfnWidgets.s(5.0F),
                    -1,
                    string);
            boolean bl3 =
                    bl2
                            ? ImGui.isMouseClicked(1)
                            : ImGui.isMouseClicked(0) || ImGui.isMouseClicked(1);
            if (bl3) {
                this.selectSurface(surface);
            }
        }
    }

    private void outlineSurface(Surface surface, float f, float g, float h, float f2, float g2) {
        int withAlpha;
        if (surface == this.themeSurface) {
            withAlpha = EfnWidgets.withAlpha(GuiPalette.accent(), 0.8F);
        } else {
            if (surface != this.hoveredSurface) {
                return;
            }
            withAlpha = -2130706433;
        }
        float px3 = EfnWidgets.s(3.0F);
        float h2 = px3 + EfnWidgets.s(2.0F);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        windowDrawList.pushClipRect(f - h2, g - h2, f + h + h2, g + f2 + h2, false);
        windowDrawList.addRect(
                f - px3,
                g - px3,
                f + h + px3,
                g + f2 + px3,
                withAlpha,
                g2 + px3,
                0,
                EfnWidgets.s(1.5F));
        windowDrawList.popClipRect();
    }

    private void recordPanelRect() {
        if (this.inspectMode) {
            float windowPosX = ImGui.getWindowPosX();
            float windowPosY = ImGui.getWindowPosY();
            float windowWidth = ImGui.getWindowWidth();
            float windowHeight = ImGui.getWindowHeight();
            this.panelRects.add(new float[] {windowPosX, windowPosY, windowWidth, windowHeight});
            this.outlineSurface(
                    ThemeStore.get().panels,
                    windowPosX,
                    windowPosY,
                    windowWidth,
                    windowHeight,
                    panelRounding());
        }
    }

    private static boolean contains(float[] floats, float f, float g) {
        return floats != null
                && f >= floats[0]
                && f <= floats[0] + floats[2]
                && g >= floats[1]
                && g <= floats[1] + floats[3];
    }

    private static ClientModule previewModule() {
        ClientModule module = null;
        for (ClientModule module2 : ModuleRegistry.all()) {
            if (module == null) {
                module = module2;
            }
            if (module2.settings().size() >= 3) {
                return module2;
            }
        }
        return module;
    }

    private void settingRows(float f, float g, Setting... setting) {
        for (Setting setting2 : setting) {
            if (setting2.isVisible()) {
                ImGui.pushID(setting2.id());
                this.renderSetting(null, setting2, f, g);
                ImGui.popID();
            }
        }
    }

    private void renderPresetRow(ThemeStore themeStore, float f, float g) {
        float h = EfnWidgets.s(46.0F) + EfnWidgets.s(6.0F);
        float rowStart = this.rowStart(f, g);
        float px28 = EfnWidgets.s(28.0F);
        float px12 = EfnWidgets.s(12.0F);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        for (int i = 0; i < ThemeStore.PRESETS.size(); i++) {
            ThemeStore.Preset preset = ThemeStore.PRESETS.get(i);
            ImGui.setCursorPos(f + i * (px28 + px12), rowStart + (h - px28) * 0.5F);
            float f2 = ImGui.getCursorScreenPosX() + px28 * 0.5F;
            float g2 = ImGui.getCursorScreenPosY() + px28 * 0.5F;
            if (ImGui.invisibleButton("##preset" + i, px28, px28)) {
                themeStore.applyPreset(preset);
            }
            boolean itemHovered = ImGui.isItemHovered();
            float animate = EfnWidgets.animate(ImGui.getItemID(), itemHovered, 18.0F);
            boolean matchesPreset = themeStore.matchesPreset(preset);
            windowDrawList.addCircleFilled(
                    f2,
                    g2,
                    px28 * 0.5F - EfnWidgets.s(3.0F),
                    EfnWidgets.c(GuiPalette.abgr(preset.accent())),
                    32);
            if (matchesPreset) {
                windowDrawList.addCircle(
                        f2, g2, px28 * 0.5F, EfnWidgets.c(-1), 32, EfnWidgets.s(1.5F));
            } else if (animate > 0.01F) {
                windowDrawList.addCircle(
                        f2,
                        g2,
                        px28 * 0.5F,
                        EfnWidgets.c(EfnWidgets.withAlpha(-1, 0.35F * animate)),
                        32,
                        EfnWidgets.s(1.2F));
            }
            if (itemHovered) {
                showTooltip(preset.name());
            }
        }
        ImGui.setCursorPos(0.0F, rowStart + h);
    }

    private void renderImageGallery(Surface surface, float f, float g) {
        List list = ThemeImages.get().files();
        byte b = 3;
        float px8 = EfnWidgets.s(8.0F);
        float px82 = EfnWidgets.s(8.0F);
        float h = (g - px8 * (b - 1)) / b;
        float f2 = h * 0.6F;
        float g2 = ImGui.getCursorPosY() + EfnWidgets.s(4.0F);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        ImFont imFont = ImGuiManager.getInstance().smallFont();
        String string = null;
        int i = list.size() + 1;
        for (int j = 0; j < i; j++) {
            ImGui.setCursorPos(f + j % b * (h + px8), g2 + j / b * (f2 + px8));
            float cursorScreenPosX = ImGui.getCursorScreenPosX();
            float cursorScreenPosY = ImGui.getCursorScreenPosY();
            String string2 = j == 0 ? null : (String) list.get(j - 1);
            String string3 = string2 == null ? null : "image-delete:" + string2;
            boolean bl = string3 != null && this.isConfirming(string3);
            float px20 = EfnWidgets.s(20.0F);
            float h2 = cursorScreenPosX + h - px20 - EfnWidgets.s(5.0F);
            float f3 = cursorScreenPosY + EfnWidgets.s(5.0F);
            boolean bl2 =
                    ImGui.isWindowHovered(128)
                            && ImGui.isMouseHoveringRect(
                                    cursorScreenPosX,
                                    cursorScreenPosY,
                                    cursorScreenPosX + h,
                                    cursorScreenPosY + f2);
            boolean bl3 = string3 != null && (bl || bl2);
            boolean bl4 = false;
            float g3 = 0.0F;
            if (bl3) {
                ImGui.setCursorScreenPos(h2, f3);
                if (ImGui.invisibleButton("##tiledel" + j, px20, px20)
                        && this.confirmClick(string3)) {
                    string = string2;
                }
                bl4 = ImGui.isItemHovered();
                g3 = EfnWidgets.animate(ImGui.getItemID(), bl4, 18.0F);
                bl = this.isConfirming(string3);
                if (bl4) {
                    showTooltip(bl ? "Click again to delete" : "Delete image");
                }
                ImGui.setCursorScreenPos(cursorScreenPosX, cursorScreenPosY);
            }
            boolean invisibleButton = ImGui.invisibleButton("##tile" + j, h, f2);
            if (!PerformanceModule.optimizationsEnabled() || ImGui.isItemVisible()) {
                boolean bl5 = ImGui.isItemHovered() || bl4;
                float animate = EfnWidgets.animate(ImGui.getItemID(), bl5, 18.0F);
                if (j == 0) {
                    windowDrawList.addRectFilled(
                            cursorScreenPosX,
                            cursorScreenPosY,
                            cursorScreenPosX + h,
                            cursorScreenPosY + f2,
                            EfnWidgets.c(EfnTheme.lerpColor(-13226454, -12240588, animate)),
                            px82);
                    int lerpColor = EfnTheme.lerpColor(-4542808, -461582, animate);
                    float h3 = cursorScreenPosX + h * 0.5F;
                    float f4 = cursorScreenPosY + f2 * 0.5F - EfnWidgets.s(8.0F);
                    windowDrawList.addLine(
                            h3 - EfnWidgets.s(7.0F),
                            f4,
                            h3 + EfnWidgets.s(7.0F),
                            f4,
                            EfnWidgets.c(lerpColor),
                            EfnWidgets.s(1.8F));
                    windowDrawList.addLine(
                            h3,
                            f4 - EfnWidgets.s(7.0F),
                            h3,
                            f4 + EfnWidgets.s(7.0F),
                            EfnWidgets.c(lerpColor),
                            EfnWidgets.s(1.8F));
                    String string4 = "Add image";
                    EfnWidgets.text(
                            windowDrawList,
                            imFont,
                            h3 - EfnWidgets.textWidth(imFont, string4) * 0.5F,
                            f4 + EfnWidgets.s(12.0F),
                            lerpColor,
                            string4);
                    if (invisibleButton) {
                        ThemeImages.get().browseAsync();
                    }
                } else {
                    ThemeImages.Texture texture = ThemeImages.get().preview(string2);
                    boolean bl6 = texture == null && ThemeImages.get().isBroken(string2);
                    windowDrawList.addRectFilled(
                            cursorScreenPosX,
                            cursorScreenPosY,
                            cursorScreenPosX + h,
                            cursorScreenPosY + f2,
                            EfnWidgets.c(-13226454),
                            px82);
                    if (texture != null) {
                        float[] floats =
                                SurfaceBackground.coverUv(
                                        texture.width(), texture.height(), h, f2, 0.5F);
                        windowDrawList.addImageRounded(
                                texture.id(),
                                cursorScreenPosX,
                                cursorScreenPosY,
                                cursorScreenPosX + h,
                                cursorScreenPosY + f2,
                                floats[0],
                                floats[1],
                                floats[2],
                                floats[3],
                                EfnWidgets.c(-1),
                                px82,
                                240);
                    } else if (bl6 && !bl) {
                        float g4 = cursorScreenPosX + h * 0.5F;
                        float h4 = cursorScreenPosY + f2 * 0.5F - EfnWidgets.s(8.0F);
                        EfnWidgets.cross(windowDrawList, g4, h4, EfnWidgets.s(5.0F), -4542808);
                        String string5 = "Can't load";
                        EfnWidgets.text(
                                windowDrawList,
                                imFont,
                                g4 - EfnWidgets.textWidth(imFont, string5) * 0.5F,
                                h4 + EfnWidgets.s(10.0F),
                                -4542808,
                                string5);
                    }
                    if (ThemeImages.isGif(string2)) {
                        String string6 = "GIF";
                        float px4 = EfnWidgets.s(4.0F);
                        float f5 = EfnWidgets.lineHeight(imFont) + EfnWidgets.s(2.0F);
                        float g5 = EfnWidgets.textWidth(imFont, string6) + px4 * 2.0F;
                        float h5 = cursorScreenPosX + EfnWidgets.s(5.0F);
                        float f6 = cursorScreenPosY + f2 - f5 - EfnWidgets.s(5.0F);
                        windowDrawList.addRectFilled(
                                h5,
                                f6,
                                h5 + g5,
                                f6 + f5,
                                EfnWidgets.c(EfnWidgets.withAlpha(-16777216, 0.6F)),
                                EfnWidgets.s(4.0F));
                        EfnWidgets.text(
                                windowDrawList,
                                imFont,
                                h5 + px4,
                                f6 + EfnWidgets.s(1.0F),
                                -1,
                                string6);
                    }
                    if (bl) {
                        String string7 = "Delete?";
                        windowDrawList.addRectFilled(
                                cursorScreenPosX,
                                cursorScreenPosY,
                                cursorScreenPosX + h,
                                cursorScreenPosY + f2,
                                EfnWidgets.c(EfnWidgets.withAlpha(-16777216, 0.6F)),
                                px82);
                        EfnWidgets.text(
                                windowDrawList,
                                imFont,
                                cursorScreenPosX
                                        + (h - EfnWidgets.textWidth(imFont, string7)) * 0.5F,
                                cursorScreenPosY + (f2 - EfnWidgets.lineHeight(imFont)) * 0.5F,
                                -1,
                                string7);
                    }
                    if (string2.equals(surface.image.get())) {
                        windowDrawList.addRect(
                                cursorScreenPosX - EfnWidgets.s(1.0F),
                                cursorScreenPosY - EfnWidgets.s(1.0F),
                                cursorScreenPosX + h + EfnWidgets.s(1.0F),
                                cursorScreenPosY + f2 + EfnWidgets.s(1.0F),
                                EfnWidgets.c(GuiPalette.accent()),
                                px82 + EfnWidgets.s(1.0F),
                                0,
                                EfnWidgets.s(2.0F));
                    } else if (animate > 0.01F) {
                        windowDrawList.addRect(
                                cursorScreenPosX,
                                cursorScreenPosY,
                                cursorScreenPosX + h,
                                cursorScreenPosY + f2,
                                EfnWidgets.c(EfnWidgets.withAlpha(-1, 0.4F * animate)),
                                px82,
                                0,
                                1.0F);
                    }
                    if (bl3) {
                        float g6 = bl ? 1.0F : animate;
                        float h6 = h2 + px20 * 0.5F;
                        float f7 = f3 + px20 * 0.5F;
                        int k =
                                bl
                                        ? -10856974
                                        : EfnTheme.lerpColor(
                                                EfnWidgets.withAlpha(-16777216, 0.55F),
                                                GuiPalette.accent(),
                                                g3);
                        windowDrawList.addCircleFilled(
                                h6, f7, px20 * 0.5F, EfnWidgets.c(EfnWidgets.withAlpha(k, g6)), 32);
                        EfnWidgets.cross(
                                windowDrawList,
                                h6,
                                f7,
                                EfnWidgets.s(3.5F),
                                EfnWidgets.withAlpha(-1, g6));
                    }
                    if (invisibleButton) {
                        if (bl) {
                            this.pendingConfirm = null;
                        } else {
                            surface.image.set(string2);
                        }
                    }
                    if (!bl4 && ImGui.isItemHovered(65536)) {
                        showTooltip(bl6 ? string2 + "\nCouldn't load this image" : string2);
                    }
                }
            }
        }
        if (string != null) {
            this.deleteImage(string);
        }
        int m = (i + b - 1) / b;
        float g7 = g2 + m * (f2 + px8) - px8 + EfnWidgets.s(10.0F);
        String string8 = "Drop an image on the screen or ";
        String string9 = "choose an image";
        float textWidth = EfnWidgets.textWidth(imFont, string8);
        float textWidth2 = EfnWidgets.textWidth(imFont, string9);
        float lineHeight = EfnWidgets.lineHeight(imFont);
        float windowPosX = ImGui.getWindowPosX();
        float h7 = ImGui.getWindowPosY() - ImGui.getScrollY();
        EfnWidgets.text(windowDrawList, imFont, windowPosX + f, h7 + g7, -4542808, string8);
        ImGui.setCursorPos(f + textWidth, g7);
        if (ImGui.invisibleButton("##openfolder", textWidth2, lineHeight)) {
            ThemeImages.get().openFolder();
        }
        float animate2 = EfnWidgets.animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
        EfnWidgets.text(
                windowDrawList,
                imFont,
                windowPosX + f + textWidth,
                h7 + g7,
                EfnTheme.lerpColor(GuiPalette.accent(), GuiPalette.accentHover(), animate2),
                string9);
        ImGui.setCursorPos(0.0F, g7 + lineHeight + EfnWidgets.s(8.0F));
    }

    private void renderThemeActions(ThemeStore themeStore, float f, float g) {
        float px8 = EfnWidgets.s(8.0F);
        float px34 = EfnWidgets.s(34.0F);
        float h = (g - px8 * 2.0F) / 3.0F;
        float f2 = ImGui.getCursorPosY() + EfnWidgets.s(6.0F);
        ImGui.setCursorPos(f, f2);
        if (EfnWidgets.chipButton("##copytheme", "Copy theme", h, px34, false)) {
            MinecraftClient.getInstance().keyboard.setClipboard(themeStore.exportString());
            this.showThemeMessage("Theme copied to the clipboard", false);
        }
        ImGui.setCursorPos(f + h + px8, f2);
        if (EfnWidgets.chipButton("##pastetheme", "Paste theme", h, px34, false)) {
            boolean importString =
                    themeStore.importString(MinecraftClient.getInstance().keyboard.getClipboard());
            this.showThemeMessage(
                    importString
                            ? "Theme applied from the clipboard"
                            : "The clipboard doesn't contain an EFN theme",
                    !importString);
        }
        ImGui.setCursorPos(f + (h + px8) * 2.0F, f2);
        if (EfnWidgets.chipButton("##resettheme", "Reset", h, px34, false)) {
            themeStore.resetToDefaults();
            this.showThemeMessage("Theme reset to the defaults", false);
        }
        ImGui.setCursorPos(0.0F, f2 + px34);
    }

    private void showThemeMessage(String themeMessage2, boolean themeMessageError2) {
        this.themeMessage = themeMessage2;
        this.themeMessageError = themeMessageError2;
        this.themeMessageAt = System.nanoTime();
    }

    public void onFilesDropped(List<Path> list) {
        if (!list.isEmpty()) {
            if (this.tab == ClickGuiRenderer.Tab.CONFIG) {
                for (Path path : list) {
                    if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
                        this.importProfile(path);
                        return;
                    }
                }
            }
            for (Path path2 : list) {
                if (ThemeImages.isImage(path2)) {
                    if (this.tab == ClickGuiRenderer.Tab.HUD) {
                        this.importHudHeaderImage(path2);
                    } else {
                        this.switchTab(ClickGuiRenderer.Tab.THEME);
                        this.importHeaderImage(path2);
                    }
                    return;
                }
            }
            if (this.tab == ClickGuiRenderer.Tab.HUD) {
                this.showHudMessage("Only PNG, JPG and GIF images can be used", true);
            } else {
                this.switchTab(ClickGuiRenderer.Tab.THEME);
                this.showThemeMessage("Only PNG, JPG and GIF images can be used", true);
            }
        }
    }

    private void importHudHeaderImage(Path path) {
        String importFile = ThemeImages.get().importFile(path);
        if (importFile == null) {
            this.showHudMessage("Only PNG, JPG and GIF images can be used", true);
        } else {
            String string = this.hudElementId != null ? this.hudElementId : "info_bar";
            Surface surface = HudTheme.get().forElementId(string);
            if (surface != null) {
                surface.image.set(importFile);
                surface.background.set("Image");
                this.selectHudElement(string);
                this.showHudMessage(
                        importFile
                                + " is now the "
                                + surface.label.toLowerCase(Locale.ROOT)
                                + " background",
                        false);
            }
        }
    }

    private void importHeaderImage(Path path) {
        String importFile = ThemeImages.get().importFile(path);
        if (importFile == null) {
            this.showThemeMessage("Only PNG, JPG and GIF images can be used", true);
        } else {
            ThemeStore themeStore = ThemeStore.get();
            Surface surface = this.themeSurface != null ? this.themeSurface : themeStore.cards;
            surface.image.set(importFile);
            surface.background.set("Image");
            this.showThemeMessage(
                    importFile
                            + " is now the "
                            + surface.label.toLowerCase(Locale.ROOT)
                            + " background",
                    false);
        }
    }

    private void deleteImage(String string) {
        boolean delete = ThemeImages.get().delete(string);
        String string2 = delete ? "Deleted " + string : "Couldn't delete " + string;
        if (this.tab == ClickGuiRenderer.Tab.HUD) {
            this.showHudMessage(string2, !delete);
        } else {
            this.showThemeMessage(string2, !delete);
        }
    }

    private static void drawCheck(float f, float g, float h, int i) {
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        windowDrawList.addRectFilled(f, g, f + 16.0F, g + 16.0F, -13226454, 4.0F);
        windowDrawList.addRect(
                f + 0.5F,
                g + 0.5F,
                f + 16.0F - 0.5F,
                g + 16.0F - 0.5F,
                EfnTheme.lerpColor(452984831, i, h),
                4.0F);
        if (h > 0.01F) {
            windowDrawList.addLine(f + 4.0F, g + 8.0F, f + 7.0F, g + 11.0F, i, 2.0F * h);
            windowDrawList.addLine(f + 7.0F, g + 11.0F, f + 12.0F, g + 5.0F, i, 2.0F * h);
        }
    }

    static float smooth(float f, float g, float h, float f2) {
        return f + (g - f) * (1.0F - (float) Math.exp(-f2 * Math.max(0.0F, h)));
    }

    public static float animationDelta(float f) {
        float animationSpeed = GuiPalette.animationSpeed();
        return animationSpeed <= 0.0F ? 1.0F : f * animationSpeed;
    }

    public static float elapsed(long l, float f) {
        float animationSpeed = GuiPalette.animationSpeed();
        return animationSpeed <= 0.0F
                ? 1.0F
                : Math.min(
                        1.0F, (float) (System.nanoTime() - l) * animationSpeed / (f * 1000000.0F));
    }

    public static float easeOut(float f) {
        float g = 1.0F - f;
        return 1.0F - g * g * g;
    }

    private void renderKeybindRow(ClientModule listeningModule2, float f, float g) {
        float px46 = EfnWidgets.s(46.0F);
        float rowStart = this.rowStart(f, g);
        boolean bl = this.listeningModule == listeningModule2;
        if (bl) {
            if (this.listeningFirstFrame) {
                this.listeningFirstFrame = false;
            } else {
                int scanPressedKey = scanPressedKey();
                if (scanPressedKey == 256) {
                    listeningModule2.setKeybind(-1);
                    this.listeningModule = null;
                    bl = false;
                } else if (scanPressedKey != -1) {
                    listeningModule2.setKeybind(scanPressedKey);
                    this.listeningModule = null;
                    bl = false;
                }
            }
        }
        rowLabel("Key", f, rowStart, px46);
        float px30 = EfnWidgets.s(30.0F);
        float px96 = EfnWidgets.s(96.0F);
        float px76 = EfnWidgets.s(76.0F);
        float px8 = EfnWidgets.s(8.0F);
        float h = rowStart + (px46 - px30) * 0.5F;
        if (!listeningModule2.isAction()) {
            ImGui.setCursorPos(f + g - px96 - px8 - px76, h);
            if (EfnWidgets.chipButton(
                    "##bindmode",
                    listeningModule2.isHoldMode() ? "Hold" : "Toggle",
                    px76,
                    px30,
                    false)) {
                listeningModule2.setHoldMode(!listeningModule2.isHoldMode());
            }
        }
        String string =
                bl
                        ? "Press a key..."
                        : (listeningModule2.keybind() == -1
                                ? "None"
                                : keyName(listeningModule2.keybind()));
        ImGui.setCursorPos(f + g - px96, h);
        if (EfnWidgets.chipButton("##bind", string, px96, px30, bl)) {
            if (bl) {
                this.listeningModule = null;
            } else {
                this.listeningModule = listeningModule2;
                this.listeningFirstFrame = true;
                this.listeningKeySetting = null;
                this.listeningKeyFirstFrame = false;
            }
        }
        ImGui.setCursorPos(0.0F, rowStart + px46);
    }

    public boolean capturesKeys() {
        return this.listeningModule != null
                || this.listeningKeySetting != null
                || ImGuiManager.getInstance().typing();
    }

    public boolean handleEscape() {
        if (this.listeningKeySetting != null) {
            this.listeningKeySetting.setValue(-1);
            this.listeningKeySetting = null;
            this.listeningKeyFirstFrame = false;
            return true;
        } else if (this.listeningModule != null) {
            this.listeningModule.setKeybind(-1);
            this.listeningModule = null;
            this.listeningFirstFrame = false;
            return true;
        } else if (this.blockLists.closePicker()) {
            return true;
        } else if (this.itemPickers.closeWindow()) {
            return true;
        } else if (this.settingsModule != null) {
            this.closeSettings();
            return true;
        } else {
            return false;
        }
    }

    public static int scanPressedKey() {
        long handle = MinecraftClient.getInstance().getWindow().getHandle();
        for (int i = 32; i <= 348; i++) {
            if (GLFW.glfwGetKey(handle, i) == 1) {
                return i;
            }
        }
        for (int j = 0; j <= 7; j++) {
            if (GLFW.glfwGetMouseButton(handle, j) == 1) {
                return 1000 + j;
            }
        }
        return -1;
    }

    private String keyLabel(int i) {
        String keyLabel = this.keyLabels.get(i);
        if (keyLabel == null) {
            keyLabel = keyName(i);
            this.keyLabels.put(i, keyLabel);
        }
        return keyLabel;
    }

    public static String keyName(int i) {
        if (i >= 1000) {
            int j = i - 1000;
            return switch (j) {
                case 0 -> "LMB";
                case 1 -> "RMB";
                case 2 -> "MMB";
                default -> "MB" + (j + 1);
            };
        } else {
            String glfwGetKeyName;
            if ((i >= 39 && i <= 96 || i >= 161 && i <= 162)
                    && (glfwGetKeyName = GLFW.glfwGetKeyName(i, -1)) != null
                    && !glfwGetKeyName.isBlank()) {
                return glfwGetKeyName.toUpperCase(Locale.ROOT);
            } else {
                return switch (i) {
                    case 32 -> "SPACE";
                    case 256 -> "ESC";
                    case 257 -> "ENTER";
                    case 258 -> "TAB";
                    case 259 -> "BKSP";
                    case 260 -> "INS";
                    case 261 -> "DEL";
                    case 262 -> "RIGHT";
                    case 263 -> "LEFT";
                    case 264 -> "DOWN";
                    case 265 -> "UP";
                    case 266 -> "PGUP";
                    case 267 -> "PGDN";
                    case 268 -> "HOME";
                    case 269 -> "END";
                    case 290 -> "F1";
                    case 291 -> "F2";
                    case 292 -> "F3";
                    case 293 -> "F4";
                    case 294 -> "F5";
                    case 295 -> "F6";
                    case 296 -> "F7";
                    case 297 -> "F8";
                    case 298 -> "F9";
                    case 299 -> "F10";
                    case 300 -> "F11";
                    case 301 -> "F12";
                    case 335 -> "NUM ENTER";
                    case 340 -> "L SHIFT";
                    case 341 -> "L CTRL";
                    case 342 -> "L ALT";
                    case 344 -> "R SHIFT";
                    case 345 -> "R CTRL";
                    case 346 -> "R ALT";
                    default -> "K" + i;
                };
            }
        }
    }

    private void renderModuleConfigActions(ClientModule module, float f, float g) {
        float px8 = EfnWidgets.s(8.0F);
        float px36 = EfnWidgets.s(36.0F);
        float h = (g - px8) * 0.5F;
        float f2 = ImGui.getCursorPosY() + EfnWidgets.s(6.0F);
        ImGui.setCursorPos(f, f2);
        if (EfnWidgets.chipButton("##copy_config", "Copy Config", h, px36, true)) {
            this.copyModuleConfig(module);
        }
        if (ImGui.isItemHovered(65536)) {
            showTooltip("Copy this module's settings as a short code");
        }
        String string = "paste:" + module.id();
        boolean confirming = this.isConfirming(string);
        ImGui.setCursorPos(f + h + px8, f2);
        if (EfnWidgets.chipButton(
                        "##paste_config",
                        confirming ? "Confirm paste" : "Paste Config",
                        h,
                        px36,
                        confirming)
                && this.confirmClick(string)) {
            this.pasteModuleConfig(module);
        }
        if (ImGui.isItemHovered(65536)) {
            showTooltip("Replace this module's settings with a code from your clipboard");
        }
        f2 += px36 + EfnWidgets.s(8.0F);
        ImFont imFont = ImGuiManager.getInstance().smallFont();
        if (module.id().equals(this.moduleCodeFor)
                && this.moduleCodeMessage != null
                && elapsed(this.moduleCodeAt, 2500.0F) < 1.0F) {
            ImGui.setCursorPos(f, f2);
            boolean pushFont = pushFont(imFont);
            ImGui.pushStyleColor(0, this.moduleCodeError ? -10856974 : -4542808);
            ImGui.textUnformatted(this.moduleCodeMessage);
            ImGui.popStyleColor();
            if (pushFont) {
                ImGui.popFont();
            }
        }
        ImGui.setCursorPos(0.0F, f2 + EfnWidgets.lineHeight(imFont));
    }

    private void copyModuleConfig(ClientModule module) {
        String string = ModuleCodes.copy(module);
        MinecraftClient.getInstance().keyboard.setClipboard(string);
        this.setModuleCodeMessage(module, "Copied, " + string.length() + " characters", false);
    }

    private void pasteModuleConfig(ClientModule module) {
        try {
            ModuleCodes.paste(module, MinecraftClient.getInstance().keyboard.getClipboard());
            this.setModuleCodeMessage(module, "Config pasted", false);
        } catch (ProfileException exception) {
            this.setModuleCodeMessage(module, exception.getMessage(), true);
        }
    }

    private void setModuleCodeMessage(
            ClientModule module, String moduleCodeMessage2, boolean moduleCodeError2) {
        this.moduleCodeFor = module.id();
        this.moduleCodeMessage = moduleCodeMessage2;
        this.moduleCodeError = moduleCodeError2;
        this.moduleCodeAt = System.nanoTime();
    }

    private void renderHudActions(float f, float g) {
        float px8 = EfnWidgets.s(8.0F);
        float px36 = EfnWidgets.s(36.0F);
        float h = (g - px8) * 0.5F;
        float f2 = ImGui.getCursorPosY() + EfnWidgets.s(14.0F);
        ImGui.setCursorPos(f, f2);
        if (EfnWidgets.chipButton("##editlayout", "Edit Layout", h, px36, true)) {
            MinecraftClient.getInstance().setScreen(new HudEditorScreen());
        }
        ImGui.setCursorPos(f + h + px8, f2);
        if (EfnWidgets.chipButton("##resetlayout", "Reset Layout", h, px36, false)) {
            HudLayoutStore.getInstance()
                    .resetToDefaults(
                            ImGui.getIO().getDisplaySizeX(), ImGui.getIO().getDisplaySizeY());
        }
        ImGui.setCursorPos(0.0F, f2 + px36);
    }

    private static boolean isHudModule(ClientModule module) {
        return module.category() == ModuleCategory.CLIENT && "HUD".equals(module.name());
    }

    private static ClientModule hudModule() {
        for (ClientModule module : ModuleRegistry.all()) {
            if (isHudModule(module)) {
                return module;
            }
        }
        return null;
    }

    private void renderHudStudio(float f, float g, float h) {
        this.renderHudInspector(f, g);
        this.highlightHudElements();
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        HudRenderer.getInstance().studioInput(displaySizeX, displaySizeY, hudStudioBlocked());
    }

    private static boolean hudStudioBlocked() {
        return ImGui.isWindowHovered(4) || ImGui.isAnyItemActive() || ImGui.isPopupOpen("", 3072);
    }

    private void renderHudInspector(float f, float g) {
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        float px360 = EfnWidgets.s(360.0F);
        float h =
                Math.min(
                        displaySizeY - EfnWidgets.s(86.0F) - EfnWidgets.s(40.0F),
                        EfnWidgets.s(760.0F));
        ImGui.setNextWindowPos(displaySizeX - px360 - EfnWidgets.s(24.0F), EfnWidgets.s(24.0F), 4);
        ImGui.setNextWindowSize(px360, h, 1);
        if (this.focusHudInspector) {
            ImGui.setNextWindowFocus();
            this.focusHudInspector = false;
        }
        ImGui.pushStyleVar(0, g);
        ImGui.pushStyleVar(2, 0.0F, 0.0F);
        ImGui.pushStyleVar(14, 0.0F, 0.0F);
        ImGui.pushStyleVar(3, cardRounding());
        ImGui.pushStyleColor(2, GuiPalette.abgr(ThemeStore.get().cards.color.argbWithAlpha(255)));
        byte b = 59;
        if (ImGui.begin("##hud_inspector", b)) {
            float windowPosX = ImGui.getWindowPosX();
            float windowPosY = ImGui.getWindowPosY();
            drawShadow(windowPosX, windowPosY, px360, h, cardRounding(), 6);
            ImGui.getWindowDrawList()
                    .addRect(
                            windowPosX,
                            windowPosY,
                            windowPosX + px360,
                            windowPosY + h,
                            EfnWidgets.c(318767103),
                            cardRounding(),
                            0,
                            1.0F);
            float px20 = EfnWidgets.s(20.0F);
            float px202 = EfnWidgets.s(20.0F);
            drawCardTitle("HUD", px20, px202 + EfnWidgets.s(14.0F));
            float f2 = px202 + EfnWidgets.s(34.0F);
            if (this.hudMessage != null && System.nanoTime() - this.hudMessageAt < 3000000000L) {
                ImGui.setCursorPos(px20, f2);
                ImGui.pushTextWrapPos(px360 - px20);
                ImGui.pushStyleColor(0, this.hudMessageError ? -10856974 : -461582);
                ImGui.textUnformatted(this.hudMessage);
                ImGui.popStyleColor();
                ImGui.popTextWrapPos();
                f2 = ImGui.getCursorPosY();
            } else {
                f2 =
                        mutedParagraph(
                                "Click an element on screen to edit it.", px20, f2, px360 - px20);
            }
            f2 =
                    this.renderHudElementTabs(
                                    px20, f2 + EfnWidgets.s(12.0F), px360 - px20 * 2.0F, f)
                            + EfnWidgets.s(14.0F);
            cardDivider(px20, px360, f2);
            ImGui.setCursorPos(0.0F, f2 + 1.0F);
            if (ImGui.beginChild(
                    "##hud_inspector_body",
                    px360,
                    Math.max(1.0F, h - f2 - 1.0F - EfnWidgets.s(10.0F)),
                    false,
                    24)) {
                float g2 = px360 - px20 * 2.0F;
                this.dividerPending = false;
                if (this.hudElementId == null) {
                    this.renderHudGeneralOptions(px20, g2);
                } else {
                    this.renderHudElementOptions(this.hudElementId, px20, g2);
                }
                ImGui.dummy(1.0F, EfnWidgets.s(18.0F));
                this.applySmoothScroll("hud_inspector", f);
            }
            ImGui.endChild();
        }
        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleVar(4);
    }

    private float renderHudElementTabs(float f, float g, float h, float f2) {
        @Environment(EnvType.CLIENT)
        record Item(String id, String label) {}
        List list =
                List.of(
                        new Item(null, "General"),
                        new Item("info_bar", HudTheme.get().infoBar.label),
                        new Item("coordinates", HudTheme.get().coordinates.label),
                        new Item("module_list", HudTheme.get().moduleList.label),
                        new Item("potion_tracker", HudTheme.get().potions.label),
                        new Item("region_map", HudTheme.get().regionMap.label),
                        new Item("anti_vanish", HudTheme.get().antiVanish.label),
                        new Item("staff_list", HudTheme.get().staffList.label),
                        new Item("spotify", HudTheme.get().spotify.label),
                        new Item("hotkeys", HudTheme.get().hotkeys.label),
                        new Item("radar", HudRenderer.editorLabel("radar")),
                        new Item("target_card", HudTheme.get().targetCard.label),
                        new Item("armor_status", HudTheme.get().armorStatus.label),
                        new Item("item_cooldowns", HudTheme.get().itemCooldowns.label),
                        new Item("toggle_toasts", HudTheme.get().toasts.label));
        ImFont imFont = ImGuiManager.getInstance().smallFont();
        boolean pushFont = pushFont(imFont);
        float px24 = EfnWidgets.s(24.0F);
        float px6 = EfnWidgets.s(6.0F);
        float px62 = EfnWidgets.s(6.0F);
        float g2 = f;
        float h2 = g;
        try {
            for (Item item : (Iterable<Item>) list) {
                boolean bl = Objects.equals(item.id(), this.hudElementId);
                float f3 = EfnWidgets.textWidth(imFont, item.label()) + EfnWidgets.s(18.0F);
                if (g2 + f3 > f + h && g2 > f) {
                    g2 = f;
                    h2 += px24 + px62;
                }
                ImGui.setCursorPos(g2, h2);
                if (EfnWidgets.chipButton(
                        "##hudel_" + (item.id() == null ? "general" : item.id()),
                        item.label(),
                        f3,
                        px24,
                        bl)) {
                    this.selectHudElement(item.id());
                }
                g2 += f3 + px6;
            }
        } finally {
            if (pushFont) {
                ImGui.popFont();
            }
        }
        return h2 + px24;
    }

    private void renderHudGeneralOptions(float f, float g) {
        ClientModule module = hudModule();
        if (module != null) {
            this.sectionLabel("Settings", f);
            this.settingRows(f, g, module.settings().toArray(new Setting[0]));
        }
        this.sectionLabel("Layout", f);
        this.settingRows(f, g, HudTheme.get().font, HudTheme.get().rounding);
        float h = ImGui.getCursorPosY() + EfnWidgets.s(6.0F);
        float px34 = EfnWidgets.s(34.0F);
        ImGui.setCursorPos(f, h);
        if (EfnWidgets.chipButton("##resethudlayout", "Reset positions & sizes", g, px34, false)) {
            HudLayoutStore.getInstance()
                    .resetToDefaults(
                            ImGui.getIO().getDisplaySizeX(), ImGui.getIO().getDisplaySizeY());
            this.showHudMessage("HUD layout reset", false);
        }
        ImGui.setCursorPos(0.0F, h + px34);
        this.sectionLabel("Share", f);
        this.renderHudThemeActions(f, g);
    }

    private void renderHudElementOptions(String string, float f, float g) {
        ClientModule module = hudModule();
        HudModule module2;
        List list;
        if (module instanceof HudModule
                && !(list = (module2 = (HudModule) module).elementSettings(string)).isEmpty()) {
            this.sectionLabel(hudOptionsTitle(string), f);
            this.settingRows(f, g, ((Setting[]) list.toArray(new Setting[0])));
        }
        RegionMapModule module3;
        if ("region_map".equals(string)
                && (module3 = ModuleRegistry.get(RegionMapModule.class)) != null) {
            this.renderInlineModuleToggle(module3, f, g);
            this.sectionLabel("Settings", f);
            this.settingRows(f, g, module3.settings().toArray(new Setting[0]));
        }
        ClientModule module4 =
                (ClientModule)
                        ("radar".equals(string)
                                ? ModuleRegistry.get(RadarModule.class)
                                : ("hotkeys".equals(string)
                                        ? ModuleRegistry.get(HotkeysModule.class)
                                        : null));
        if (module4 != null) {
            this.renderInlineModuleToggle(module4, f, g);
            if (!module4.settings().isEmpty()) {
                this.sectionLabel("Settings", f);
                this.settingRows(f, g, module4.settings().toArray(new Setting[0]));
            }
        }
        Surface surface;
        if ((surface = HudTheme.get().forElementId(string)) != null) {
            this.renderSurfaceOptions(surface, f, g);
        }
    }

    private static String hudOptionsTitle(String string) {
        return switch (string) {
            case "toggle_toasts" -> "Style";
            case "target_card", "armor_status", "item_cooldowns" -> "Options";
            default -> "Show";
        };
    }

    private void renderInlineModuleToggle(ClientModule module, float f, float g) {
        float px46 = EfnWidgets.s(46.0F);
        float rowStart = this.rowStart(f, g);
        rowLabel("Active", f, rowStart, px46);
        float px40 = EfnWidgets.s(40.0F);
        float px22 = EfnWidgets.s(22.0F);
        ImGui.setCursorPos(f + g - px40, rowStart + (px46 - px22) * 0.5F);
        float cursorScreenPosX = ImGui.getCursorScreenPosX();
        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        if (ImGui.invisibleButton("##active", px40, px22)) {
            module.toggle();
        }
        EfnWidgets.drawSwitch(
                ImGui.getWindowDrawList(),
                cursorScreenPosX,
                cursorScreenPosY,
                px40,
                px22,
                EfnWidgets.animate(ImGui.getItemID(), module.isEnabled(), 16.0F));
        ImGui.setCursorPos(0.0F, rowStart + px46);
    }

    private void renderHudThemeActions(float f, float g) {
        HudTheme hudTheme = HudTheme.get();
        float px8 = EfnWidgets.s(8.0F);
        float px34 = EfnWidgets.s(34.0F);
        float h = (g - px8 * 2.0F) / 3.0F;
        float f2 = ImGui.getCursorPosY() + EfnWidgets.s(6.0F);
        ImGui.setCursorPos(f, f2);
        if (EfnWidgets.chipButton("##copyhudtheme", "Copy theme", h, px34, false)) {
            MinecraftClient.getInstance().keyboard.setClipboard(hudTheme.exportString());
            this.showHudMessage("HUD theme copied to the clipboard", false);
        }
        ImGui.setCursorPos(f + h + px8, f2);
        if (EfnWidgets.chipButton("##pastehudtheme", "Paste theme", h, px34, false)) {
            boolean importString =
                    hudTheme.importString(MinecraftClient.getInstance().keyboard.getClipboard());
            this.showHudMessage(
                    importString
                            ? "HUD theme applied from the clipboard"
                            : "The clipboard doesn't contain an EFN HUD theme",
                    !importString);
        }
        ImGui.setCursorPos(f + (h + px8) * 2.0F, f2);
        if (EfnWidgets.chipButton("##resethudtheme", "Reset", h, px34, false)) {
            hudTheme.resetToDefaults();
            this.showHudMessage("HUD theme reset to the defaults", false);
        }
        ImGui.setCursorPos(0.0F, f2 + px34);
    }

    private void showHudMessage(String hudMessage2, boolean hudMessageError2) {
        this.hudMessage = hudMessage2;
        this.hudMessageError = hudMessageError2;
        this.hudMessageAt = System.nanoTime();
    }

    private void selectHudElement(String hudElementId2) {
        if (!Objects.equals(hudElementId2, this.hudElementId)) {
            this.hudElementId = hudElementId2;
            this.focusHudInspector = true;
        }
    }

    private void highlightHudElements() {
        float mousePosX = ImGui.getIO().getMousePosX();
        float mousePosY = ImGui.getIO().getMousePosY();
        boolean dragging = HudRenderer.getInstance().isDragging();
        HudLayoutStore.HudElementState hudElementState = null;
        if (!dragging && !hudStudioBlocked()) {
            for (HudLayoutStore.HudElementState orderedElement :
                    HudLayoutStore.getInstance().orderedElements()) {
                if (orderedElement.positioned
                        && contains(
                                new float[] {
                                    orderedElement.x,
                                    orderedElement.y,
                                    orderedElement.width,
                                    orderedElement.height
                                },
                                mousePosX,
                                mousePosY)) {
                    hudElementState = orderedElement;
                }
            }
        }
        ImDrawList backgroundDrawList = ImGui.getBackgroundDrawList();
        HudLayoutStore.HudElementState hudElementState2;
        if (this.hudElementId != null
                && (hudElementState2 = findHudElementState(this.hudElementId)) != null) {
            this.outlineHudElement(
                    backgroundDrawList,
                    hudElementState2,
                    EfnWidgets.withAlpha(GuiPalette.accent(), 0.8F));
        }
        if (hudElementState != null) {
            if (!hudElementState.id.equals(this.hudElementId)) {
                this.outlineHudElement(backgroundDrawList, hudElementState, -2130706433);
            }
            ImDrawList foregroundDrawList = ImGui.getForegroundDrawList();
            Surface surface = HudTheme.get().forElementId(hudElementState.id);
            String string =
                    surface != null ? surface.label : HudRenderer.editorLabel(hudElementState.id);
            String string2 = string + "  \u00b7  click to edit, drag to move, scroll to resize";
            ImFont imFont = ImGuiManager.getInstance().smallFont();
            float f = EfnWidgets.textWidth(imFont, string2) + EfnWidgets.s(18.0F);
            float g = EfnWidgets.lineHeight(imFont) + EfnWidgets.s(10.0F);
            float h =
                    Math.min(
                            mousePosX + EfnWidgets.s(14.0F),
                            ImGui.getIO().getDisplaySizeX() - f - EfnWidgets.s(4.0F));
            float f2 =
                    Math.min(
                            mousePosY + EfnWidgets.s(18.0F),
                            ImGui.getIO().getDisplaySizeY() - g - EfnWidgets.s(4.0F));
            foregroundDrawList.addRectFilled(h, f2, h + f, f2 + g, GuiPalette.accent(), g * 0.5F);
            EfnWidgets.text(
                    foregroundDrawList,
                    imFont,
                    h + EfnWidgets.s(9.0F),
                    f2 + EfnWidgets.s(5.0F),
                    -1,
                    string2);
            if (ImGui.isMouseClicked(0) || ImGui.isMouseClicked(1)) {
                this.selectHudElement(hudElementState.id);
            }
        }
    }

    private void outlineHudElement(
            ImDrawList imDrawList, HudLayoutStore.HudElementState hudElementState, int i) {
        float px3 = EfnWidgets.s(3.0F);
        float f = HudRenderer.hudRounding() * HudRenderer.scaleOf(hudElementState);
        imDrawList.addRect(
                hudElementState.x - px3,
                hudElementState.y - px3,
                hudElementState.x + hudElementState.width + px3,
                hudElementState.y + hudElementState.height + px3,
                i,
                f + px3,
                0,
                EfnWidgets.s(1.5F));
    }

    private static HudLayoutStore.HudElementState findHudElementState(String string) {
        for (HudLayoutStore.HudElementState orderedElement :
                HudLayoutStore.getInstance().orderedElements()) {
            if (orderedElement.id.equals(string)) {
                return orderedElement;
            }
        }
        return null;
    }

    private void openColorPicker(
            ClientModule activeColorModule2, ColorSetting activeColorSetting2) {
        this.activeColorModule = activeColorModule2;
        this.activeColorSetting = activeColorSetting2;
        this.colorPickerOpen.set(true);
    }

    private void renderColorPickerWindow() {
        if (this.activeColorSetting != null) {
            if (!this.colorPickerOpen.get()) {
                this.activeColorSetting = null;
                this.activeColorModule = null;
            } else {
                ImGui.setNextWindowSize(270.0F, 330.0F, 4);
                ImGui.setNextWindowPos(260.0F, 90.0F, 4);
                short s = 352;
                String string =
                        this.activeColorModule != null ? this.activeColorModule.name() + " / " : "";
                if (ImGui.begin(
                        "Color - " + string + this.activeColorSetting.name() + "##color_picker",
                        this.colorPickerOpen,
                        s)) {
                    int i = 135593984;
                    if (ImGui.colorPicker4("##picker", this.activeColorSetting.value(), i)) {
                        this.activeColorSetting.notifyChanged();
                    }
                    ImGui.setNextItemWidth(240.0F);
                    if (ImGui.colorEdit4("RGBA", this.activeColorSetting.value(), i)) {
                        this.activeColorSetting.notifyChanged();
                    }
                    if (ImGui.button("Close", 80.0F, 22.0F)) {
                        this.activeColorSetting.notifyChanged();
                        this.colorPickerOpen.set(false);
                    }
                }
                ImGui.end();
                if (!this.colorPickerOpen.get()) {
                    this.activeColorSetting = null;
                    this.activeColorModule = null;
                }
            }
        }
    }

    private void renderItemColorPaletteWindow() {
        if (this.itemColorWindowOpen && this.activeItemColorPalette != null) {
            float f = Math.max(ImGui.getIO().getDeltaTime(), 0.001F);
            ImGui.setNextWindowSize(340.0F, 500.0F, 4);
            ImGui.setNextWindowPos(340.0F, 100.0F, 4);
            short s = 288;
            boolean begin = ImGui.begin("Item Colors##icpalette", s);
            if (!begin) {
                ImGui.end();
                if (this.activeItemColorPalette != null) {
                    this.activeItemColorPalette.notifyChanged();
                }
                this.itemColorWindowOpen = false;
            } else {
                ImGui.text("Default:");
                ImGui.sameLine();
                int i = 131648;
                if (ImGui.colorButton(
                        "##icdefcol",
                        this.activeItemColorPalette.defaultColor(),
                        i,
                        42.0F,
                        16.0F)) {
                    this.editingItemDefaultColor = true;
                    this.activeItemColorEntry = null;
                    this.itemColorPickerOpen.set(true);
                }
                ImGui.separator();
                ImGui.text("Search:");
                ImGui.sameLine();
                ImGui.setNextItemWidth(200.0F);
                ImGui.inputText("##icsearch", this.itemSearchBuf);
                String string = this.itemSearchBuf.get().toLowerCase().trim();
                ImGui.separator();
                ImGui.beginChild("##iclist", 0.0F, 360.0F, false, 0);
                for (ItemColorListSetting.ItemEntry entry : this.activeItemColorPalette.entries()) {
                    boolean bl =
                            string.isEmpty()
                                    || entry.displayName().toLowerCase().contains(string)
                                    || entry.id().toLowerCase().contains(string);
                    if (bl) {
                        float cursorPosY = ImGui.getCursorPosY();
                        float g = 22.0F;
                        float contentRegionAvailX = ImGui.getContentRegionAvailX();
                        ImGui.setCursorPos(4.0F, cursorPosY + (g - 16.0F) * 0.5F);
                        ImGui.pushID(entry.id() + "_ictog");
                        this.renderItemEntryToggle(this.activeItemColorPalette, entry, f);
                        ImGui.popID();
                        float textLineHeight = ImGui.getTextLineHeight();
                        ImGui.setCursorPos(24.0F, cursorPosY + (g - textLineHeight) * 0.5F);
                        ImGui.textUnformatted(entry.displayName());
                        float h = contentRegionAvailX - 46.0F;
                        if (h < 96.0F) {
                            h = 96.0F;
                        }
                        ImGui.setCursorPos(h, cursorPosY + (g - 16.0F) * 0.5F);
                        int j = 131648;
                        ImGui.pushID(entry.id() + "_iccol");
                        if (ImGui.colorButton("##ic", entry.color(), j, 42.0F, 16.0F)) {
                            this.activeItemColorEntry = entry;
                            this.editingItemDefaultColor = false;
                            this.itemColorPickerOpen.set(true);
                        }
                        ImGui.popID();
                        ImGui.setCursorPosY(cursorPosY + g + 2.0F);
                    }
                }
                ImGui.endChild();
                ImGui.separator();
                if (ImGui.button("Close##icclose", 80.0F, 22.0F)) {
                    this.activeItemColorPalette.notifyChanged();
                    this.itemColorWindowOpen = false;
                    this.activeItemColorPalette = null;
                }
                ImGui.end();
                this.renderItemColorPicker();
            }
        }
    }

    private void renderItemColorPicker() {
        if (!this.itemColorPickerOpen.get()) {
            this.activeItemColorEntry = null;
            this.editingItemDefaultColor = false;
        } else if (this.activeItemColorEntry == null && !this.editingItemDefaultColor) {
            this.itemColorPickerOpen.set(false);
        } else {
            String string;
            float[] floats;
            if (this.editingItemDefaultColor && this.activeItemColorPalette != null) {
                floats = this.activeItemColorPalette.defaultColor();
                string = "Default Color";
            } else {
                floats = this.activeItemColorEntry.color();
                string = this.activeItemColorEntry.displayName();
            }
            ImGui.setNextWindowSize(270.0F, 330.0F, 4);
            ImGui.setNextWindowPos(700.0F, 100.0F, 4);
            short s = 352;
            if (ImGui.begin("Item Color - " + string, this.itemColorPickerOpen, s)) {
                int i = 135593984;
                ImGui.colorPicker4("##icpicker", floats, i);
                ImGui.setNextItemWidth(240.0F);
                if (ImGui.colorEdit4("RGBA##icce", floats, i)
                        && this.activeItemColorPalette != null) {
                    this.activeItemColorPalette.notifyChanged();
                }
                if (ImGui.button("Close##iccp", 80.0F, 22.0F)) {
                    if (this.activeItemColorPalette != null) {
                        this.activeItemColorPalette.notifyChanged();
                    }
                    this.itemColorPickerOpen.set(false);
                    this.activeItemColorEntry = null;
                    this.editingItemDefaultColor = false;
                }
            }
            ImGui.end();
            if (!this.itemColorPickerOpen.get()) {
                this.activeItemColorEntry = null;
                this.editingItemDefaultColor = false;
            }
        }
    }

    private void renderItemEntryToggle(
            ItemColorListSetting setting, ItemColorListSetting.ItemEntry itemEntry, float f) {
        float g = 16.0F;
        float h = 16.0F;
        float cursorScreenPosX = ImGui.getCursorScreenPosX();
        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        if (ImGui.invisibleButton("##ietog", 16.0F, 16.0F)) {
            itemEntry.enabled().set(!itemEntry.enabled().get());
            setting.notifyChanged();
        }
        float f2 = itemEntry.enabled().get() ? 1.0F : 0.0F;
        float g2 = this.itemEntryTogAnim.getOrDefault(itemEntry, f2);
        g2 = smooth(g2, f2, f, 14.0F);
        this.itemEntryTogAnim.put(itemEntry, g2);
        drawCheck(cursorScreenPosX, cursorScreenPosY, g2, GuiPalette.accent());
    }

    private void renderSetting(ClientModule module, Setting setting, float f, float nextItemWidth) {
        float px46 = EfnWidgets.s(46.0F);
        float rowStart = this.rowStart(f, nextItemWidth);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        if (setting instanceof BooleanSetting setting2) {
            ImGui.setCursorPos(f, rowStart);
            float cursorScreenPosX = ImGui.getCursorScreenPosX();
            float cursorScreenPosY = ImGui.getCursorScreenPosY();
            if (ImGui.invisibleButton("##row", nextItemWidth, px46)) {
                setting2.set(!setting2.get());
            }
            float animate;
            if ((animate = EfnWidgets.animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F))
                    > 0.01F) {
                windowDrawList.addRectFilled(
                        cursorScreenPosX - EfnWidgets.s(10.0F),
                        cursorScreenPosY + EfnWidgets.s(3.0F),
                        cursorScreenPosX + nextItemWidth + EfnWidgets.s(10.0F),
                        cursorScreenPosY + px46 - EfnWidgets.s(3.0F),
                        EfnWidgets.c(EfnWidgets.withAlpha(184549375, animate)),
                        EfnWidgets.s(10.0F));
            }
            rowLabel(setting.name(), f, rowStart, px46);
            float px40 = EfnWidgets.s(40.0F);
            float px22 = EfnWidgets.s(22.0F);
            EfnWidgets.drawSwitch(
                    windowDrawList,
                    cursorScreenPosX + nextItemWidth - px40,
                    cursorScreenPosY + (px46 - px22) * 0.5F,
                    px40,
                    px22,
                    EfnWidgets.animate(ImGui.getID("##switch"), setting2.get(), 16.0F));
        } else if (setting instanceof NumberSetting setting3) {
            px46 = EfnWidgets.s(64.0F);
            ImGui.setCursorPos(f, rowStart + EfnWidgets.s(10.0F));
            ImGui.textUnformatted(setting.name());
            ImFont imFont = ImGuiManager.getInstance().smallFont();
            String formatted = setting3.formatted();
            float textWidth = EfnWidgets.textWidth(imFont, formatted);
            float g = textWidth + EfnWidgets.s(16.0F);
            float h = EfnWidgets.lineHeight(imFont) + EfnWidgets.s(8.0F);
            float windowPosX = ImGui.getWindowPosX();
            float f2 = ImGui.getWindowPosY() - ImGui.getScrollY();
            float g2 = windowPosX + f + nextItemWidth - g;
            float h2 = f2 + rowStart + EfnWidgets.s(8.0F);
            windowDrawList.addRectFilled(g2, h2, g2 + g, h2 + h, EfnWidgets.c(-13226454), h * 0.5F);
            EfnWidgets.text(
                    windowDrawList,
                    imFont,
                    g2 + EfnWidgets.s(8.0F),
                    h2 + EfnWidgets.s(4.0F),
                    -461582,
                    formatted);
            ImGui.setCursorPos(f - EfnWidgets.s(7.0F), rowStart + EfnWidgets.s(36.0F));
            if (EfnWidgets.sliderFloat(
                    "##v",
                    setting3.value(),
                    setting3.min(),
                    setting3.max(),
                    nextItemWidth + EfnWidgets.s(14.0F),
                    setting3::snap)) {
                setting3.notifyChanged();
            }
        } else if (setting instanceof ModeSetting setting4) {
            rowLabel(setting.name(), f, rowStart, px46);
            float f3 = 0.0F;
            for (String mode : setting4.modes()) {
                f3 = Math.max(f3, ImGui.calcTextSize(mode).x);
            }
            float g3 =
                    Math.min(
                            nextItemWidth * 0.6F,
                            Math.max(EfnWidgets.s(120.0F), f3 + EfnWidgets.s(44.0F)));
            float px30 = EfnWidgets.s(30.0F);
            ImGui.setCursorPos(f + nextItemWidth - g3, rowStart + (px46 - px30) * 0.5F);
            int dropdown =
                    EfnWidgets.dropdown("##mode", setting4.get(), setting4.modes(), g3, px30);
            if (dropdown >= 0) {
                setting4.set(setting4.modes().get(dropdown));
            }
        } else if (setting instanceof ColorSetting setting5) {
            rowLabel(setting.name(), f, rowStart, px46);
            float px44 = EfnWidgets.s(44.0F);
            float px24 = EfnWidgets.s(24.0F);
            ImGui.setCursorPos(f + nextItemWidth - px44, rowStart + (px46 - px24) * 0.5F);
            float cursorScreenPosX2 = ImGui.getCursorScreenPosX();
            float cursorScreenPosY2 = ImGui.getCursorScreenPosY();
            if (ImGui.invisibleButton("##color", px44, px24)) {
                this.openColorPicker(module, setting5);
            }
            boolean itemHovered = ImGui.isItemHovered();
            float[] setting52 = setting5.value();
            int i = (int) (setting52[0] * 255.0F) & 0xFF;
            int j = (int) (setting52[1] * 255.0F) & 0xFF;
            int k = (int) (setting52[2] * 255.0F) & 0xFF;
            int m = setting52.length > 3 ? (int) (setting52[3] * 255.0F) & 0xFF : 255;
            windowDrawList.addRectFilled(
                    cursorScreenPosX2,
                    cursorScreenPosY2,
                    cursorScreenPosX2 + px44,
                    cursorScreenPosY2 + px24,
                    EfnWidgets.c(-13226454),
                    EfnWidgets.s(7.0F));
            windowDrawList.addRectFilled(
                    cursorScreenPosX2,
                    cursorScreenPosY2,
                    cursorScreenPosX2 + px44,
                    cursorScreenPosY2 + px24,
                    EfnWidgets.c(m << 24 | k << 16 | j << 8 | i),
                    EfnWidgets.s(7.0F));
            windowDrawList.addRect(
                    cursorScreenPosX2,
                    cursorScreenPosY2,
                    cursorScreenPosX2 + px44,
                    cursorScreenPosY2 + px24,
                    EfnWidgets.c(itemHovered ? 1728053247 : 654311423),
                    EfnWidgets.s(7.0F),
                    0,
                    1.0F);
            if (itemHovered) {
                showTooltip(String.format("#%02X%02X%02X", i, j, k));
            }
        } else if (setting instanceof KeySetting setting6) {
            boolean bl = this.listeningKeySetting == setting6;
            if (bl) {
                if (this.listeningKeyFirstFrame) {
                    this.listeningKeyFirstFrame = false;
                } else {
                    int scanPressedKey = scanPressedKey();
                    if (scanPressedKey == 256) {
                        setting6.setValue(-1);
                        this.listeningKeySetting = null;
                        bl = false;
                    } else if (scanPressedKey != -1) {
                        setting6.setValue(scanPressedKey);
                        this.listeningKeySetting = null;
                        bl = false;
                    }
                }
            }
            rowLabel(setting.name(), f, rowStart, px46);
            float px96 = EfnWidgets.s(96.0F);
            float px302 = EfnWidgets.s(30.0F);
            String string =
                    bl
                            ? "Press a key..."
                            : (setting6.value() == -1 ? "None" : keyName(setting6.value()));
            ImGui.setCursorPos(f + nextItemWidth - px96, rowStart + (px46 - px302) * 0.5F);
            if (EfnWidgets.chipButton("##keyval", string, px96, px302, bl)) {
                if (bl) {
                    this.listeningKeySetting = null;
                } else {
                    this.listeningKeySetting = setting6;
                    this.listeningKeyFirstFrame = true;
                    this.listeningModule = null;
                    this.listeningFirstFrame = false;
                }
            }
        } else {
            StringSetting setting7;
            if (setting instanceof StringSetting
                    && !(setting7 = (StringSetting) setting).presets().isEmpty()) {
                px46 = this.itemPickers.renderPresets(module, setting7, f, nextItemWidth, rowStart);
            } else {
                StringSetting setting8;
                if (setting instanceof StringSetting
                        && (setting8 = (StringSetting) setting).isItemPicker()) {
                    px46 =
                            this.itemPickers.renderItem(
                                    module, setting8, f, nextItemWidth, rowStart);
                } else {
                    StringSetting setting9;
                    if (setting instanceof StringSetting
                            && (setting9 = (StringSetting) setting).enchantItem() != null) {
                        px46 =
                                this.itemPickers.renderEnchants(
                                        module, setting9, f, nextItemWidth, rowStart);
                    } else if (setting instanceof StringSetting setting10) {
                        ImGui.setCursorPos(f, rowStart + EfnWidgets.s(10.0F));
                        ImGui.textUnformatted(setting.name());
                        ImGui.setCursorPos(f, rowStart + EfnWidgets.s(34.0F));
                        ImGui.pushStyleVar(11, EfnWidgets.s(12.0F), EfnWidgets.s(8.0F));
                        ImGui.setNextItemWidth(nextItemWidth);
                        boolean bl2 =
                                setting10.hint() != null
                                        ? ImGui.inputTextWithHint(
                                                "##str", setting10.hint(), setting10.getImString())
                                        : ImGui.inputText("##str", setting10.getImString());
                        if (bl2) {
                            setting10.notifyChanged();
                        }
                        ImGui.popStyleVar();
                        px46 =
                                EfnWidgets.s(34.0F)
                                        + ImGui.getTextLineHeight()
                                        + EfnWidgets.s(16.0F)
                                        + EfnWidgets.s(12.0F);
                    } else {
                        StringListSetting setting11;
                        if (setting instanceof StringListSetting
                                && (setting11 = (StringListSetting) setting).isItemPicker()) {
                            px46 =
                                    this.itemPickers.renderList(
                                            module, setting11, f, nextItemWidth, rowStart);
                        } else if (setting instanceof StringListSetting setting12) {
                            px46 = renderStringList(setting12, f, nextItemWidth, rowStart);
                        } else if (setting instanceof ActionSetting setting13) {
                            float px36 = EfnWidgets.s(36.0F);
                            ImGui.setCursorPos(
                                    f, rowStart + (px46 + EfnWidgets.s(6.0F) - px36) * 0.5F);
                            if (EfnWidgets.chipButton(
                                    "##action", setting.name(), nextItemWidth, px36, false)) {
                                setting13.run();
                            }
                            px46 += EfnWidgets.s(6.0F);
                        } else if (setting instanceof BlockColorListSetting setting14) {
                            px46 =
                                    this.blockLists.render(
                                            module, setting14, f, nextItemWidth, rowStart);
                        } else if (setting instanceof ItemColorListSetting setting15) {
                            long l =
                                    setting15.entries().stream()
                                            .filter(itemEntry -> itemEntry.enabled().get())
                                            .count();
                            if (listChip(
                                    setting.name(),
                                    "Edit (" + l + ")",
                                    f,
                                    nextItemWidth,
                                    rowStart,
                                    px46)) {
                                this.activeItemColorPalette = setting15;
                                this.itemColorWindowOpen = true;
                                this.activeItemColorEntry = null;
                                this.editingItemDefaultColor = false;
                            }
                        }
                    }
                }
            }
        }
        if (!(setting instanceof ActionSetting)) {
            this.renderResetAffordance(setting, f, nextItemWidth, rowStart, px46);
        }
        ImGui.setCursorPos(0.0F, rowStart + px46);
    }

    private void renderResetAffordance(Setting setting, float f, float g, float h, float f2) {
        if (this.tab != ClickGuiRenderer.Tab.THEME) {
            boolean bl = !setting.isDefault();
            float windowPosX = ImGui.getWindowPosX();
            float g2 = ImGui.getWindowPosY() - ImGui.getScrollY();
            boolean bl2 =
                    setting instanceof NumberSetting
                            || setting instanceof StringSetting
                            || setting instanceof StringListSetting
                            || setting instanceof BlockColorListSetting;
            float h2 =
                    bl2
                            ? h + EfnWidgets.s(10.0F) + ImGui.getTextLineHeight() * 0.5F
                            : h + f2 * 0.5F;
            if (bl) {
                float f3 = windowPosX + f - EfnWidgets.s(11.0F);
                float g3 = g2 + h2;
                float px3 = EfnWidgets.s(3.0F);
                ImGui.getWindowDrawList()
                        .addCircleFilled(f3, g3, px3, EfnWidgets.c(GuiPalette.accent()), 12);
                if (ImGui.isMouseHoveringRect(
                        f3 - px3 * 2.0F, g3 - px3 * 2.0F, f3 + px3 * 2.0F, g3 + px3 * 2.0F)) {
                    showTooltip("Modified - right-click the row to reset");
                }
            }
            float h3 = windowPosX + f - EfnWidgets.s(10.0F);
            float f4 = g2 + h;
            if (ImGui.isWindowHovered()
                    && ImGui.isMouseClicked(1)
                    && ImGui.isMouseHoveringRect(h3, f4, h3 + g + EfnWidgets.s(20.0F), f4 + f2)) {
                ImGui.openPopup("##setting_ctx");
            }
            ImGui.pushStyleVar(2, EfnWidgets.s(6.0F), EfnWidgets.s(6.0F));
            if (ImGui.beginPopup("##setting_ctx")) {
                ImGui.textDisabled(setting.name());
                ImGui.separator();
                if (bl) {
                    if (ImGui.selectable("Reset to default")) {
                        setting.resetToDefault();
                    }
                } else {
                    ImGui.textDisabled("Already at default");
                }
                ImGui.endPopup();
            }
            ImGui.popStyleVar();
        }
    }

    private static float renderStringList(StringListSetting setting, float f, float g, float h) {
        ImGui.setCursorPos(f, h + EfnWidgets.s(10.0F));
        ImGui.textUnformatted(setting.name());
        float f2 = h + EfnWidgets.s(34.0F);
        float px6 = EfnWidgets.s(6.0F);
        float g2;
        float h2 = g2 = ImGui.getTextLineHeight() + EfnWidgets.s(16.0F);
        int i = -1;
        ImGui.pushStyleVar(11, EfnWidgets.s(12.0F), EfnWidgets.s(8.0F));
        for (int j = 0; j < setting.size(); j++) {
            ImGui.pushID(j);
            ImGui.setCursorPos(f, f2);
            ImGui.setNextItemWidth(g - h2 - px6);
            if (ImGui.inputText("##entry", setting.entry(j))) {
                setting.edited();
            }
            ImGui.setCursorPos(f + g - h2, f2);
            if (EfnWidgets.chipButton("##remove", "x", h2, g2, false)) {
                i = j;
            }
            ImGui.popID();
            f2 += g2 + px6;
        }
        ImGui.popStyleVar();
        if (i >= 0) {
            setting.remove(i);
        }
        if (setting.canAdd()) {
            float px30 = EfnWidgets.s(30.0F);
            ImGui.setCursorPos(f, f2);
            if (EfnWidgets.chipButton("##add", "+ Add", g, px30, false)) {
                setting.add();
            }
            f2 += px30;
        }
        return f2 - h + EfnWidgets.s(12.0F);
    }

    private static boolean listChip(
            String string, String string2, float f, float g, float h, float f2) {
        rowLabel(string, f, h, f2);
        float g2 =
                Math.max(EfnWidgets.s(96.0F), ImGui.calcTextSize(string2).x + EfnWidgets.s(28.0F));
        float px30 = EfnWidgets.s(30.0F);
        ImGui.setCursorPos(f + g - g2, h + (f2 - px30) * 0.5F);
        return EfnWidgets.chipButton("##listedit", string2, g2, px30, false);
    }

    public static void applyTheme() {
        ImGui.pushStyleVar(3, EfnWidgets.s(10.0F));
        ImGui.pushStyleVar(12, EfnWidgets.s(8.0F));
        ImGui.pushStyleVar(9, EfnWidgets.s(10.0F));
        ImGui.pushStyleVar(21, EfnWidgets.s(6.0F));
        ImGui.pushStyleVar(2, EfnWidgets.s(10.0F), EfnWidgets.s(10.0F));
        ImGui.pushStyleVar(11, EfnWidgets.s(10.0F), EfnWidgets.s(6.0F));
        ImGui.pushStyleVar(14, EfnWidgets.s(8.0F), EfnWidgets.s(6.0F));
        ImGui.pushStyleVar(4, 0.0F);
        ImGui.pushStyleVar(10, 1.0F);
        ImGui.pushStyleVar(18, EfnWidgets.s(6.0F));
        ImGui.pushStyleVar(19, EfnWidgets.s(3.0F));
        for (int i = 0; i < THEME_COLORS.length; i += 2) {
            ImGui.pushStyleColor(THEME_COLORS[i], THEME_COLORS[i + 1]);
        }
        int accent = GuiPalette.accent();
        int cardBg = GuiPalette.cardBg();
        ImGui.pushStyleColor(2, GuiPalette.panelBg());
        ImGui.pushStyleColor(10, cardBg);
        ImGui.pushStyleColor(11, cardBg);
        ImGui.pushStyleColor(12, cardBg);
        ImGui.pushStyleColor(18, accent);
        ImGui.pushStyleColor(19, accent);
        ImGui.pushStyleColor(20, accent);
        ImGui.pushStyleColor(23, accent);
        ImGui.pushStyleColor(26, accent);
        ImGui.pushStyleColor(51, EfnWidgets.withAlpha(accent, 0.4F));
    }

    public static void removeTheme() {
        ImGui.popStyleColor(THEME_COLORS.length / 2 + 10);
        ImGui.popStyleVar(11);
    }

    public static boolean pushFont(ImFont imFont) {
        if (imFont == null) {
            return false;
        } else {
            ImGui.pushFont(imFont);
            return true;
        }
    }

    @Environment(EnvType.CLIENT)
    private record CategoryPanel(ModuleCategory category, String id, String rowsId) {}

    @Environment(EnvType.CLIENT)
    private record PanelLayout(List<String> ids, List<String> keys, List<Float> heights) {}

    @Environment(EnvType.CLIENT)
    private static enum Tab {
        MODULES("Modules"),
        HUD("HUD"),
        FRIENDS("Friends"),
        THEME("Theme"),
        CONFIG("Config");

        final String label;

        private Tab(String label2) {
            this.label = label2;
        }
    }
}
