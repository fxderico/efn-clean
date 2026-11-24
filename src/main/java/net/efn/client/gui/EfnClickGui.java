package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImString;

import net.efn.client.config.ConfigProfiles;
import net.efn.client.config.FriendStore;
import net.efn.client.config.ModuleConfigStore;
import net.efn.client.config.ProfileException;
import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.ActionSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class EfnClickGui {

    public static final String BRAND = "efn";

    public static final String BRAND_TAG = "client";

    public static final String BRAND_FOOTER = "efn.lol";

    public static final String VERSION = "v1.2.0";

    private static final String[] GENERAL_LABELS =
            new String[] {"HUD", "Friends", "Theme", "Configs"};

    private static final int[] GENERAL_TABS = new int[] {1, 2, 3, 4};

    private static final int HOST_FLAGS = 447;

    private static final int BG_PANEL = rgba(13, 13, 16, 247);

    private static final int BG_SIDEBAR = rgba(18, 18, 22, 255);

    private static final int BG_CARD = rgba(23, 23, 28, 255);

    private static final int BG_CARD_HOVER = rgba(30, 30, 37, 255);

    private static final int BG_FIELD = rgba(10, 10, 13, 255);

    private static final int LINE = rgba(255, 255, 255, 16);

    private static final int TEXT = rgba(236, 235, 240, 255);

    private static final int MUTED = rgba(138, 137, 150, 255);

    private static final int SWITCH_OFF = rgba(52, 52, 63, 255);

    private static final int KNOB = rgba(250, 248, 255, 255);

    private int page;

    private final ImString friendInput = new ImString(32);

    private final ImString profileInput = new ImString(40);

    private String armedDelete;

    private long armedAt;

    private String pageMsg = "";

    private boolean pageMsgErr;

    private long pageMsgAt;

    private final ClickGuiRenderer legacy = new ClickGuiRenderer();

    private final long openedAt = System.nanoTime();

    private EfnClickGui.View view = EfnClickGui.View.MODULES;

    private ModuleCategory category;

    private ClientModule detail;

    private final ImString search = new ImString(128);

    private boolean focusSearch;

    private ClientModule listeningModule;

    private KeySetting listeningKey;

    private boolean listenFirstFrame;

    private String dragId;

    private String popupFor;

    private final Map<String, float[]> anim = new HashMap<>();

    private final Map<Integer, String> keyLabels = new HashMap<>();

    private List<ModuleCategory> catCache = new ArrayList<>();

    private int catCacheRevision = -1;

    private float ga = 1.0F;

    private float dt;

    private float mx;

    private float my;

    private boolean clicked;

    private boolean rclicked;

    private boolean down;

    private boolean winHover;

    private boolean popupBlock;

    private ImFont fTitle;

    private ImFont fMod;

    private ImFont fSmall;

    private ImFont fCat;

    private static final String[] PAGE_FIRST = new String[] {"HUD", "Friends", "Theme", "Config"};

    private static final String[] PAGE_REST =
            new String[] {"Layout", "List", "Settings", "Profiles"};

    private String viewKey = "";

    private long viewChangedAt = System.nanoTime();

    public EfnClickGui() {}

    public void render() {
        GuiPalette.refresh();
        ThemeStore themeStore = ThemeStore.get();
        ImGuiManager imGuiManager = ImGuiManager.getInstance();
        float displaySizeX = ImGui.getIO().getDisplaySizeX();
        float displaySizeY = ImGui.getIO().getDisplaySizeY();
        this.dt = ClickGuiRenderer.animationDelta(Math.max(ImGui.getIO().getDeltaTime(), 0.001F));
        if (this.view == EfnClickGui.View.LEGACY) {
            this.legacy.render();
            this.renderBackPill(displaySizeX, displaySizeY);
        } else {
            this.fTitle = imGuiManager.titleFont();
            this.fMod = imGuiManager.moduleFont();
            this.fSmall = imGuiManager.smallFont();
            this.fCat = imGuiManager.categoryFont();
            this.mx = ImGui.getMousePosX();
            this.my = ImGui.getMousePosY();
            this.clicked = ImGui.isMouseClicked(0);
            this.rclicked = ImGui.isMouseClicked(1);
            this.down = ImGui.isMouseDown(0);
            this.popupBlock = this.popupFor != null && ImGui.isPopupOpen("", 3072);
            float easeOut =
                    ClickGuiRenderer.easeOut(ClickGuiRenderer.elapsed(this.openedAt, 240.0F));
            this.ga = Math.max(0.01F, easeOut);
            ImGui.getBackgroundDrawList()
                    .addRectFilled(
                            0.0F,
                            0.0F,
                            displaySizeX,
                            displaySizeY,
                            EfnWidgets.withAlpha(
                                    -16777216, themeStore.backgroundDim.get() * easeOut));
            if (ImGui.getIO().getKeyCtrl()
                    && (ImGui.isKeyPressed(551) || ImGui.isKeyPressed(556))) {
                this.detail = null;
                this.focusSearch = true;
            }
            this.updateKeyListening();
            this.refreshCategories();
            float f = Math.min(EfnWidgets.s(1040.0F), displaySizeX - EfnWidgets.s(48.0F));
            float g = Math.min(EfnWidgets.s(680.0F), displaySizeY - EfnWidgets.s(48.0F));
            float h = (displaySizeX - f) * 0.5F;
            float f2 = (displaySizeY - g) * 0.5F + (1.0F - easeOut) * EfnWidgets.s(12.0F);
            ImGui.setNextWindowPos(0.0F, 0.0F, 1);
            ImGui.setNextWindowSize(displaySizeX, displaySizeY, 1);
            ImGui.pushStyleVar(2, 0.0F, 0.0F);
            ImGui.pushStyleVar(4, 0.0F);
            ImGui.pushStyleColor(2, 0);
            boolean pushFont = ClickGuiRenderer.pushFont(this.fMod);
            try {
                if (ImGui.begin("##efn_host", 447)) {
                    this.winHover = ImGui.isWindowHovered();
                    this.renderWindow(ImGui.getWindowDrawList(), h, f2, f, g);
                }
                ImGui.end();
            } finally {
                if (pushFont) {
                    ImGui.popFont();
                }
                ImGui.popStyleColor(1);
                ImGui.popStyleVar(2);
            }
        }
    }

    public boolean handleEscape() {
        if (this.view == EfnClickGui.View.LEGACY) {
            if (!this.legacy.handleEscape()) {
                this.view = EfnClickGui.View.MODULES;
            }
            return true;
        } else if (this.listeningKey != null) {
            this.listeningKey.setValue(-1);
            this.listeningKey = null;
            return true;
        } else if (this.listeningModule != null) {
            this.listeningModule.setKeybind(-1);
            this.listeningModule = null;
            return true;
        } else if (this.popupFor != null) {
            this.popupFor = null;
            return true;
        } else if (this.view == EfnClickGui.View.PAGE) {
            this.view = EfnClickGui.View.MODULES;
            return true;
        } else if (this.detail != null) {
            this.detail = null;
            return true;
        } else if (!this.search.get().isEmpty()) {
            this.search.set("");
            return true;
        } else {
            return false;
        }
    }

    public boolean capturesKeys() {
        return this.view == EfnClickGui.View.LEGACY
                ? this.legacy.capturesKeys()
                : this.listeningModule != null
                        || this.listeningKey != null
                        || ImGuiManager.getInstance().typing();
    }

    public void onFilesDropped(List<Path> list) {
        this.legacy.onFilesDropped(list);
    }

    private void renderWindow(ImDrawList imDrawList, float f, float g, float h, float f2) {
        float g2 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding()) + EfnWidgets.s(6.0F);
        ClickGuiRenderer.drawShadow(f, g, h, f2, g2, 6);
        imDrawList.addRectFilled(f, g, f + h, g + f2, this.f(BG_PANEL), g2);
        imDrawList.addRect(f, g, f + h, g + f2, this.f(LINE), g2, 0, 1.0F);
        float px12 = EfnWidgets.s(12.0F);
        float px232 = EfnWidgets.s(232.0F);
        this.renderSidebar(imDrawList, f + px12, g + px12, px232, f2 - px12 * 2.0F);
        float h2 = f + px12 + px232 + EfnWidgets.s(22.0F);
        float f3 = f + h - px12 - EfnWidgets.s(14.0F) - h2;
        float g3 = g + px12 + EfnWidgets.s(14.0F);
        float h3 =
                this.view != EfnClickGui.View.PAGE && this.detail != null
                        ? EfnWidgets.s(96.0F)
                        : EfnWidgets.s(72.0F);
        this.renderHeader(imDrawList, h2, g3, f3);
        float f4 = g3 + h3;
        int accent = GuiPalette.accent();
        imDrawList.addRectFilled(h2, f4, h2 + f3, f4 + EfnWidgets.s(1.0F), this.f(LINE));
        float g4 = f4 + EfnWidgets.s(14.0F);
        float h4 = g + f2 - px12 - EfnWidgets.s(10.0F) - g4;
        if (this.view == EfnClickGui.View.PAGE) {
            this.renderPage(h2, g4, f3, h4);
        } else if (this.detail != null) {
            this.renderDetail(h2, g4, f3, h4, this.detail);
        } else {
            this.renderGrid(h2, g4, f3, h4, this.currentModules());
        }
    }

    private void renderSidebar(ImDrawList imDrawList, float f, float g, float h, float f2) {
        float g2 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding());
        imDrawList.addRectFilled(f, g, f + h, g + f2, this.f(BG_SIDEBAR), g2);
        imDrawList.addRect(f, g, f + h, g + f2, this.f(LINE), g2, 0, 1.0F);
        int accent = GuiPalette.accent();
        float px18 = EfnWidgets.s(18.0F);
        float h2 = g + EfnWidgets.s(16.0F);
        float px36 = EfnWidgets.s(36.0F);
        this.drawLogo(imDrawList, f + px18, h2, px36, accent);
        float f3 = f + px18 + px36 + EfnWidgets.s(12.0F);
        EfnWidgets.text(
                imDrawList,
                this.fCat,
                f3,
                h2 - EfnWidgets.s(1.0F),
                this.f(TEXT),
                "efn".toUpperCase(Locale.ROOT));
        EfnWidgets.text(
                imDrawList, this.fSmall, f3, h2 + EfnWidgets.s(18.0F), this.f(MUTED), "v1.2.0");
        float g3 = h2 + px36 + EfnWidgets.s(18.0F);
        EfnWidgets.text(imDrawList, this.fSmall, f + px18, g3, this.f(MUTED), "MODULES");
        g3 += EfnWidgets.lineHeight(this.fSmall) + EfnWidgets.s(6.0F);
        float px34 = EfnWidgets.s(34.0F);
        float h3 = h - EfnWidgets.s(20.0F);
        float f4 = f + EfnWidgets.s(10.0F);
        for (ModuleCategory moduleCategory : this.catCache) {
            boolean bl =
                    this.view != EfnClickGui.View.PAGE
                            && (moduleCategory == this.category && this.detail == null
                                    || moduleCategory == this.category
                                            && !this.search.get().isEmpty());
            boolean hot = this.hot(f4, g3, h3, px34);
            float[] floats = this.state("cat:" + moduleCategory.name(), bl);
            floats[0] += ((hot ? 1.0F : 0.0F) - floats[0]) * Math.min(1.0F, this.dt * 14.0F);
            floats[1] +=
                    ((this.category == moduleCategory && this.view != EfnClickGui.View.PAGE
                                            ? 1.0F
                                            : 0.0F)
                                    - floats[1])
                            * Math.min(1.0F, this.dt * 12.0F);
            if (floats[1] > 0.01F) {
                imDrawList.addRectFilled(
                        f4,
                        g3,
                        f4 + h3,
                        g3 + px34,
                        this.f(a(accent, 0.16F * floats[1])),
                        EfnWidgets.s(10.0F));
                imDrawList.addRectFilled(
                        f4,
                        g3 + px34 * 0.22F,
                        f4 + EfnWidgets.s(3.0F),
                        g3 + px34 * 0.78F,
                        this.f(a(accent, floats[1])),
                        EfnWidgets.s(2.0F));
            } else if (floats[0] > 0.01F) {
                imDrawList.addRectFilled(
                        f4,
                        g3,
                        f4 + h3,
                        g3 + px34,
                        this.f(a(rgba(255, 255, 255, 255), 0.05F * floats[0])),
                        EfnWidgets.s(10.0F));
            }
            int lerpColor = EfnTheme.lerpColor(MUTED, accent, floats[1]);
            this.drawIcon(
                    imDrawList,
                    moduleCategory.icon(),
                    f4 + EfnWidgets.s(22.0F),
                    g3 + px34 * 0.5F,
                    EfnWidgets.s(7.0F),
                    this.f(lerpColor));
            int lerpColor2 =
                    EfnTheme.lerpColor(
                            rgba(189, 188, 200, 255), TEXT, Math.max(floats[1], floats[0]));
            EfnWidgets.text(
                    imDrawList,
                    this.fMod,
                    f4 + EfnWidgets.s(42.0F),
                    g3 + (px34 - EfnWidgets.lineHeight(this.fMod)) * 0.5F,
                    this.f(lerpColor2),
                    this.label(moduleCategory));
            String string = Integer.toString(ModuleRegistry.inCategory(moduleCategory).size());
            float textWidth = EfnWidgets.textWidth(this.fSmall, string);
            EfnWidgets.text(
                    imDrawList,
                    this.fSmall,
                    f4 + h3 - textWidth - EfnWidgets.s(12.0F),
                    g3 + (px34 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F,
                    this.f(MUTED),
                    string);
            if (hot && this.clicked) {
                this.category = moduleCategory;
                this.view = EfnClickGui.View.MODULES;
                this.detail = null;
                this.search.set("");
            }
            g3 += px34 + EfnWidgets.s(2.0F);
        }
        g3 += EfnWidgets.s(12.0F);
        EfnWidgets.text(imDrawList, this.fSmall, f + px18, g3, this.f(MUTED), "GENERAL");
        g3 += EfnWidgets.lineHeight(this.fSmall) + EfnWidgets.s(6.0F);
        for (int n = 0; n < GENERAL_LABELS.length; n++) {
            boolean hot2 = this.hot(f4, g3, h3, px34);
            float[] floats2 = this.state("gen:" + n, false);
            floats2[0] += ((hot2 ? 1.0F : 0.0F) - floats2[0]) * Math.min(1.0F, this.dt * 14.0F);
            boolean bl2 = this.view == EfnClickGui.View.PAGE && this.page == n;
            floats2[1] += ((bl2 ? 1.0F : 0.0F) - floats2[1]) * Math.min(1.0F, this.dt * 12.0F);
            if (floats2[1] > 0.01F) {
                int accent2 = GuiPalette.accent();
                imDrawList.addRectFilled(
                        f4,
                        g3,
                        f4 + h3,
                        g3 + px34,
                        this.f(a(accent2, 0.16F * floats2[1])),
                        EfnWidgets.s(10.0F));
                imDrawList.addRectFilled(
                        f4,
                        g3 + px34 * 0.22F,
                        f4 + EfnWidgets.s(3.0F),
                        g3 + px34 * 0.78F,
                        this.f(a(accent2, floats2[1])),
                        EfnWidgets.s(2.0F));
            } else if (floats2[0] > 0.01F) {
                imDrawList.addRectFilled(
                        f4,
                        g3,
                        f4 + h3,
                        g3 + px34,
                        this.f(a(rgba(255, 255, 255, 255), 0.05F * floats2[0])),
                        EfnWidgets.s(10.0F));
            }
            this.drawGeneralIcon(
                    imDrawList,
                    n,
                    f4 + EfnWidgets.s(22.0F),
                    g3 + px34 * 0.5F,
                    EfnWidgets.s(6.5F),
                    this.f(
                            EfnTheme.lerpColor(
                                    EfnTheme.lerpColor(MUTED, TEXT, floats2[0]),
                                    GuiPalette.accent(),
                                    floats2[1])));
            EfnWidgets.text(
                    imDrawList,
                    this.fMod,
                    f4 + EfnWidgets.s(42.0F),
                    g3 + (px34 - EfnWidgets.lineHeight(this.fMod)) * 0.5F,
                    this.f(
                            EfnTheme.lerpColor(
                                    rgba(189, 188, 200, 255),
                                    TEXT,
                                    Math.max(floats2[0], floats2[1]))),
                    GENERAL_LABELS[n]);
            if (hot2 && this.clicked) {
                this.page = n;
                this.view = EfnClickGui.View.PAGE;
                this.detail = null;
                this.popupFor = null;
                this.pageMsg = "";
            }
            g3 += px34 + EfnWidgets.s(2.0F);
        }
        float textWidth2 = EfnWidgets.textWidth(this.fSmall, "efn.lol");
        EfnWidgets.text(
                imDrawList,
                this.fSmall,
                f + (h - textWidth2) * 0.5F,
                g + f2 - EfnWidgets.s(26.0F),
                this.f(a(MUTED, 0.8F)),
                "efn.lol");
    }

    private void renderHeader(ImDrawList imDrawList, float f, float g, float h) {
        if (this.view == EfnClickGui.View.PAGE) {
            this.renderPageHeader(imDrawList, f, g, h);
        } else {
            int accent = GuiPalette.accent();
            boolean bl = this.detail == null && !this.search.get().trim().isEmpty();
            if (this.detail != null) {
                ClientModule detail2 = this.detail;
                EfnWidgets.text(imDrawList, this.fTitle, f, g, this.f(TEXT), detail2.name());
                String string = detail2.description() == null ? "" : detail2.description();
                float f2 = h - EfnWidgets.s(260.0F);
                EfnWidgets.text(
                        imDrawList,
                        this.fSmall,
                        f,
                        g + EfnWidgets.lineHeight(this.fTitle) + EfnWidgets.s(6.0F),
                        this.f(MUTED),
                        EfnWidgets.ellipsize(this.fSmall, string, f2));
                float px84 = EfnWidgets.s(84.0F);
                float px30 = EfnWidgets.s(30.0F);
                float g2 = g + EfnWidgets.s(54.0F);
                boolean hot = this.hot(f, g2, px84, px30);
                imDrawList.addRectFilled(
                        f,
                        g2,
                        f + px84,
                        g2 + px30,
                        this.f(hot ? BG_CARD_HOVER : BG_CARD),
                        EfnWidgets.s(9.0F));
                imDrawList.addRect(
                        f, g2, f + px84, g2 + px30, this.f(LINE), EfnWidgets.s(9.0F), 0, 1.0F);
                EfnWidgets.chevronLeft(
                        imDrawList,
                        f + EfnWidgets.s(16.0F),
                        g2 + px30 * 0.5F,
                        EfnWidgets.s(4.5F),
                        this.f(TEXT));
                EfnWidgets.text(
                        imDrawList,
                        this.fSmall,
                        f + EfnWidgets.s(30.0F),
                        g2 + (px30 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F,
                        this.f(TEXT),
                        "Back");
                if (hot && this.clicked) {
                    this.detail = null;
                    this.popupFor = null;
                }
                float px46 = EfnWidgets.s(46.0F);
                float px24 = EfnWidgets.s(24.0F);
                float h2 = f + h - px46;
                float f3 = g + EfnWidgets.s(10.0F);
                float[] floats = this.state("tog:" + detail2.id(), detail2.isEnabled());
                floats[1] +=
                        ((detail2.isEnabled() ? 1.0F : 0.0F) - floats[1])
                                * Math.min(1.0F, this.dt * 10.0F);
                this.drawSwitch(imDrawList, h2, f3, px46, px24, floats[1]);
                if (this.hot(h2, f3, px46, px24) && this.clicked) {
                    this.toggle(detail2);
                }
                String string2 = detail2.isEnabled() ? "Enabled" : "Disabled";
                float textWidth = EfnWidgets.textWidth(this.fSmall, string2);
                EfnWidgets.text(
                        imDrawList,
                        this.fSmall,
                        h2 - textWidth - EfnWidgets.s(12.0F),
                        f3 + (px24 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F,
                        this.f(detail2.isEnabled() ? accent : MUTED),
                        string2);
                if (detail2.hasModifiedSettings()) {
                    float px78 = EfnWidgets.s(78.0F);
                    float px28 = EfnWidgets.s(28.0F);
                    float g3 = f + h - px78;
                    float h3 = g + EfnWidgets.s(54.0F);
                    boolean hot2 = this.hot(g3, h3, px78, px28);
                    imDrawList.addRectFilled(
                            g3,
                            h3,
                            g3 + px78,
                            h3 + px28,
                            this.f(hot2 ? BG_CARD_HOVER : BG_CARD),
                            EfnWidgets.s(9.0F));
                    imDrawList.addRect(
                            g3,
                            h3,
                            g3 + px78,
                            h3 + px28,
                            this.f(LINE),
                            EfnWidgets.s(9.0F),
                            0,
                            1.0F);
                    float textWidth2 = EfnWidgets.textWidth(this.fSmall, "Reset");
                    EfnWidgets.text(
                            imDrawList,
                            this.fSmall,
                            g3 + (px78 - textWidth2) * 0.5F,
                            h3 + (px28 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F,
                            this.f(TEXT),
                            "Reset");
                    if (hot2 && this.clicked) {
                        detail2.resetSettings();
                    }
                }
            } else {
                String string3 =
                        bl
                                ? "Search"
                                : (this.category == null ? "Modules" : this.label(this.category));
                EfnWidgets.text(imDrawList, this.fTitle, f, g, this.f(accent), string3);
                float textWidth3 = EfnWidgets.textWidth(this.fTitle, string3 + " ");
                EfnWidgets.text(
                        imDrawList,
                        this.fTitle,
                        f + textWidth3,
                        g,
                        this.f(TEXT),
                        bl ? "Results" : "Modules");
                List list = this.currentModules();
                int i = 0;
                for (ClientModule module : (Iterable<ClientModule>) list) {
                    if (module.isEnabled()) {
                        i++;
                    }
                }
                EfnWidgets.text(
                        imDrawList,
                        this.fSmall,
                        f,
                        g + EfnWidgets.lineHeight(this.fTitle) + EfnWidgets.s(6.0F),
                        this.f(MUTED),
                        list.size() + " modules  \u00b7  " + i + " enabled");
                float f4 = Math.min(EfnWidgets.s(300.0F), h * 0.42F);
                float px40 = EfnWidgets.s(40.0F);
                float g4 = f + h - f4;
                float h4 = g + EfnWidgets.s(2.0F);
                imDrawList.addRectFilled(
                        g4, h4, g4 + f4, h4 + px40, this.f(BG_FIELD), EfnWidgets.s(11.0F));
                imDrawList.addRect(
                        g4,
                        h4,
                        g4 + f4,
                        h4 + px40,
                        this.f(bl ? a(accent, 0.7F) : LINE),
                        EfnWidgets.s(11.0F),
                        0,
                        1.0F);
                EfnWidgets.magnifier(
                        imDrawList,
                        g4 + EfnWidgets.s(20.0F),
                        h4 + px40 * 0.5F,
                        EfnWidgets.s(6.0F),
                        this.f(MUTED));
                String string4 = "Ctrl K";
                float f5 = EfnWidgets.textWidth(this.fSmall, string4) + EfnWidgets.s(14.0F);
                imDrawList.addRectFilled(
                        g4 + f4 - f5 - EfnWidgets.s(8.0F),
                        h4 + EfnWidgets.s(8.0F),
                        g4 + f4 - EfnWidgets.s(8.0F),
                        h4 + px40 - EfnWidgets.s(8.0F),
                        this.f(rgba(255, 255, 255, 14)),
                        EfnWidgets.s(6.0F));
                EfnWidgets.text(
                        imDrawList,
                        this.fSmall,
                        g4 + f4 - f5 - EfnWidgets.s(1.0F),
                        h4 + (px40 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F,
                        this.f(MUTED),
                        string4);
                ImGui.setCursorScreenPos(
                        g4 + EfnWidgets.s(40.0F),
                        h4 + (px40 - EfnWidgets.lineHeight(this.fMod)) * 0.5F);
                ImGui.setNextItemWidth(f4 - EfnWidgets.s(40.0F) - f5 - EfnWidgets.s(18.0F));
                ImGui.pushStyleColor(7, 0);
                ImGui.pushStyleColor(8, 0);
                ImGui.pushStyleColor(9, 0);
                ImGui.pushStyleColor(0, TEXT);
                ImGui.pushStyleColor(1, MUTED);
                ImGui.pushStyleVar(11, 0.0F, 0.0F);
                if (this.focusSearch) {
                    ImGui.setKeyboardFocusHere();
                    this.focusSearch = false;
                }
                ImGui.inputTextWithHint("##efn_search", "Search modules...", this.search);
                ImGui.popStyleVar(1);
                ImGui.popStyleColor(5);
            }
        }
    }

    private void renderGrid(float f, float g, float h, float f2, List<ClientModule> list) {
        ImGui.setCursorScreenPos(f, g);
        this.pushChildStyle();
        if (ImGui.beginChild("##efn_grid", h, f2, false, 0)) {
            this.winHover = ImGui.isWindowHovered();
            ImDrawList windowDrawList = ImGui.getWindowDrawList();
            float cursorScreenPosX = ImGui.getCursorScreenPosX();
            float g2 = ImGui.getCursorScreenPosY() + EfnWidgets.s(6.0F);
            float px12 = EfnWidgets.s(12.0F);
            this.trackView();
            float h2 = (h - EfnWidgets.s(12.0F) - px12) * 0.5F;
            float px70 = EfnWidgets.s(70.0F);
            if (list.isEmpty()) {
                String string = "No modules found";
                float textWidth = EfnWidgets.textWidth(this.fMod, string);
                EfnWidgets.text(
                        windowDrawList,
                        this.fMod,
                        cursorScreenPosX + (h - textWidth) * 0.5F,
                        g2 + EfnWidgets.s(60.0F),
                        this.f(MUTED),
                        string);
            }
            int i = (list.size() + 1) / 2;
            for (int k = 0; k < list.size(); k++) {
                float f3 = cursorScreenPosX + k % 2 * (h2 + px12);
                float g3 = g2 + k / 2 * (px70 + px12);
                float easeOut =
                        ClickGuiRenderer.easeOut(
                                Math.max(
                                        0.0F,
                                        Math.min(
                                                1.0F,
                                                (this.sinceViewChange() - Math.min(k, 14) * 0.022F)
                                                        / 0.32F)));
                float ga2 = this.ga;
                this.ga = ga2 * Math.max(0.01F, easeOut);
                this.drawModuleCard(
                        windowDrawList,
                        (ClientModule) list.get(k),
                        f3,
                        g3 + (1.0F - easeOut) * EfnWidgets.s(10.0F),
                        h2,
                        px70);
                this.ga = ga2;
            }
            ImGui.setCursorScreenPos(cursorScreenPosX, g2 + i * (px70 + px12) + EfnWidgets.s(6.0F));
            ImGui.dummy(1.0F, 1.0F);
        }
        ImGui.endChild();
        this.popChildStyle();
    }

    private void drawModuleCard(
            ImDrawList imDrawList, ClientModule detail2, float f, float g, float h, float f2) {
        int accent = GuiPalette.accent();
        float g2 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding());
        boolean hot = this.hot(f, g, h, f2);
        boolean enabled = detail2.isEnabled();
        float[] floats = this.state("card:" + detail2.id(), enabled);
        floats[0] += ((hot ? 1.0F : 0.0F) - floats[0]) * Math.min(1.0F, this.dt * 14.0F);
        floats[1] += ((enabled ? 1.0F : 0.0F) - floats[1]) * Math.min(1.0F, this.dt * 10.0F);
        imDrawList.addRectFilled(
                f,
                g,
                f + h,
                g + f2,
                this.f(EfnTheme.lerpColor(BG_CARD, BG_CARD_HOVER, floats[0])),
                g2);
        if (floats[1] > 0.01F) {
            imDrawList.addRectFilled(f, g, f + h, g + f2, this.f(a(accent, 0.08F * floats[1])), g2);
        }
        imDrawList.addRect(
                f,
                g,
                f + h,
                g + f2,
                this.f(EfnTheme.lerpColor(LINE, accent, floats[1])),
                g2,
                0,
                EfnWidgets.s(1.0F));
        float px18 = EfnWidgets.s(18.0F);
        float px44 = EfnWidgets.s(44.0F);
        float px24 = EfnWidgets.s(24.0F);
        float h2 = f + h - px18 - px44;
        float f3 = g + (f2 - px24) * 0.5F;
        float g3 = h2 - EfnWidgets.s(26.0F);
        float h3 = g + f2 * 0.5F;
        float f4 = g3 - EfnWidgets.s(18.0F) - (f + px18);
        String ellipsize =
                EfnWidgets.ellipsize(this.fMod, detail2.name(), f4 - EfnWidgets.s(40.0F));
        float g4 = g + EfnWidgets.s(13.0F);
        EfnWidgets.text(imDrawList, this.fMod, f + px18, g4, this.f(TEXT), ellipsize);
        boolean bl = false;
        int i = detail2.isAction() ? -1 : detail2.keybind();
        if (i >= 0) {
            String string = this.keyLabel(i) + (detail2.isHoldMode() ? " hold" : "");
            float h4 = EfnWidgets.textWidth(this.fSmall, string) + EfnWidgets.s(12.0F);
            float f5 = f + px18 + EfnWidgets.textWidth(this.fMod, ellipsize) + EfnWidgets.s(8.0F);
            float g5 = EfnWidgets.lineHeight(this.fSmall) + EfnWidgets.s(4.0F);
            float h5 = g4 + (EfnWidgets.lineHeight(this.fMod) - g5) * 0.5F;
            imDrawList.addRectFilled(
                    f5, h5, f5 + h4, h5 + g5, this.f(rgba(255, 255, 255, 15)), EfnWidgets.s(5.0F));
            EfnWidgets.text(
                    imDrawList,
                    this.fSmall,
                    f5 + EfnWidgets.s(6.0F),
                    h5 + EfnWidgets.s(2.0F),
                    this.f(MUTED),
                    string);
        }
        String string2 =
                detail2.isAction()
                        ? "Action"
                        : (detail2.description() == null ? "" : detail2.description());
        EfnWidgets.text(
                imDrawList,
                this.fSmall,
                f + px18,
                g + f2 - EfnWidgets.s(14.0F) - EfnWidgets.lineHeight(this.fSmall),
                this.f(MUTED),
                EfnWidgets.ellipsize(this.fSmall, string2, f4));
        boolean hot2 =
                this.hot(
                        g3 - EfnWidgets.s(12.0F),
                        h3 - EfnWidgets.s(12.0F),
                        EfnWidgets.s(24.0F),
                        EfnWidgets.s(24.0F));
        this.drawCog(imDrawList, g3, h3, EfnWidgets.s(7.5F), this.f(hot2 ? TEXT : MUTED));
        this.drawSwitch(imDrawList, h2, f3, px44, px24, floats[1]);
        if (this.clicked) {
            if (hot2) {
                this.detail = detail2;
                this.popupFor = null;
            } else if (hot && !bl) {
                this.toggle(detail2);
            }
        } else if (this.rclicked && hot) {
            this.detail = detail2;
            this.popupFor = null;
        }
    }

    private void renderDetail(float f, float g, float h, float f2, ClientModule module) {
        ImGui.setCursorScreenPos(f, g);
        this.pushChildStyle();
        if (ImGui.beginChild("##efn_detail", h, f2, false, 0)) {
            this.winHover = ImGui.isWindowHovered();
            ImDrawList windowDrawList = ImGui.getWindowDrawList();
            float cursorScreenPosX = ImGui.getCursorScreenPosX();
            float g2 = ImGui.getCursorScreenPosY() + EfnWidgets.s(6.0F);
            float h2 = h - EfnWidgets.s(12.0F);
            float px54 = EfnWidgets.s(54.0F);
            float f3 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding());
            float g3 = g2;
            int i = module.isAction() ? 0 : (module.keybind() >= 0 ? 2 : 1);
            if (i > 0) {
                this.beginContainer(windowDrawList, cursorScreenPosX, g2, h2, i * px54, f3);
                this.rowLabel(windowDrawList, cursorScreenPosX, g2, px54, "Keybind");
                this.keyRow(windowDrawList, cursorScreenPosX, g2, h2, px54, module);
                if (i == 2) {
                    float h3 = g2 + px54;
                    windowDrawList.addLine(
                            cursorScreenPosX + EfnWidgets.s(18.0F),
                            h3,
                            cursorScreenPosX + h2 - EfnWidgets.s(18.0F),
                            h3,
                            this.f(LINE),
                            1.0F);
                    this.rowLabel(windowDrawList, cursorScreenPosX, h3, px54, "Hold to enable");
                    float px44 = EfnWidgets.s(44.0F);
                    float px24 = EfnWidgets.s(24.0F);
                    float f4 = cursorScreenPosX + h2 - EfnWidgets.s(18.0F) - px44;
                    float g4 = h3 + (px54 - px24) * 0.5F;
                    float[] floats = this.state("hold:" + module.id(), module.isHoldMode());
                    floats[1] +=
                            ((module.isHoldMode() ? 1.0F : 0.0F) - floats[1])
                                    * Math.min(1.0F, this.dt * 10.0F);
                    this.drawSwitch(windowDrawList, f4, g4, px44, px24, floats[1]);
                    if (this.clicked && this.hot(f4, g4, px44, px24)) {
                        module.setHoldMode(!module.isHoldMode());
                    }
                }
                g3 = g2 + (i * px54 + EfnWidgets.s(14.0F));
            }
            // Visible settings grouped by section, in the order sections first appear.
            // Ungrouped settings come first; each group is its own titled card.
            java.util.LinkedHashMap<String, java.util.List<Setting>> groups =
                    new java.util.LinkedHashMap<>();
            boolean anySection = false;
            for (Setting setting : module.settings()) {
                if (setting.isVisible()) {
                    String title = setting.section() == null ? "" : setting.section();
                    anySection |= !title.isEmpty();
                    groups.computeIfAbsent(title, key -> new java.util.ArrayList<>()).add(setting);
                }
            }
            if (groups.containsKey("") && anySection) {
                java.util.List<Setting> general = groups.remove("");
                java.util.LinkedHashMap<String, java.util.List<Setting>> ordered =
                        new java.util.LinkedHashMap<>();
                ordered.put("General", general);
                ordered.putAll(groups);
                groups = ordered;
            }
            boolean firstGroup = true;
            for (java.util.Map.Entry<String, java.util.List<Setting>> group : groups.entrySet()) {
                java.util.List<Setting> rows = group.getValue();
                if (!firstGroup) {
                    g3 += EfnWidgets.s(18.0F);
                }
                firstGroup = false;
                if (!group.getKey().isEmpty()) {
                    EfnWidgets.text(
                            windowDrawList,
                            this.fSmall,
                            cursorScreenPosX + EfnWidgets.s(6.0F),
                            g3,
                            this.f(GuiPalette.accent()),
                            group.getKey().toUpperCase(java.util.Locale.ROOT));
                    g3 += EfnWidgets.lineHeight(this.fSmall) + EfnWidgets.s(8.0F);
                }
                this.beginContainer(
                        windowDrawList, cursorScreenPosX, g3, h2, rows.size() * px54, f3);
                for (int k = 0; k < rows.size(); k++) {
                    float h4 = g3 + k * px54;
                    if (k > 0) {
                        windowDrawList.addLine(
                                cursorScreenPosX + EfnWidgets.s(18.0F),
                                h4,
                                cursorScreenPosX + h2 - EfnWidgets.s(18.0F),
                                h4,
                                this.f(LINE),
                                1.0F);
                    }
                    this.settingRow(
                            windowDrawList, cursorScreenPosX, h4, h2, px54, module, rows.get(k));
                }
                g3 += rows.size() * px54;
            }
            ImGui.setCursorScreenPos(cursorScreenPosX, g3 + EfnWidgets.s(24.0F));
            ImGui.dummy(1.0F, 1.0F);
        }
        ImGui.endChild();
        this.popChildStyle();
    }

    private void beginContainer(
            ImDrawList imDrawList, float f, float g, float h, float f2, float g2) {
        imDrawList.addRectFilled(f, g, f + h, g + f2, this.f(BG_CARD), g2);
        imDrawList.addRect(f, g, f + h, g + f2, this.f(LINE), g2, 0, 1.0F);
    }

    /** Setting name with a muted one-line description under it; full text on hover if cut. */
    private void rowLabelWithDescription(
            ImDrawList imDrawList, float f, float g, float width, float h, Setting setting) {
        float nameHeight = EfnWidgets.lineHeight(this.fMod);
        float descHeight = EfnWidgets.lineHeight(this.fSmall);
        float gap = EfnWidgets.s(3.0F);
        float top = g + (h - nameHeight - gap - descHeight) * 0.5F;
        float left = f + EfnWidgets.s(18.0F);
        EfnWidgets.text(imDrawList, this.fMod, left, top, this.f(TEXT), setting.name());
        // leave the right side for the control (switch / slider / field)
        float maxWidth = width * 0.42F - EfnWidgets.s(36.0F);
        String description = setting.description();
        String shown = EfnWidgets.ellipsize(this.fSmall, description, maxWidth);
        EfnWidgets.text(
                imDrawList, this.fSmall, left, top + nameHeight + gap, this.f(MUTED), shown);
        if (!shown.equals(description) && this.hot(left, g, maxWidth, h)) {
            ImGui.setTooltip(description);
        }
    }

    private void rowLabel(ImDrawList imDrawList, float f, float g, float h, String string) {
        EfnWidgets.text(
                imDrawList,
                this.fMod,
                f + EfnWidgets.s(18.0F),
                g + (h - EfnWidgets.lineHeight(this.fMod)) * 0.5F,
                this.f(TEXT),
                string);
    }

    private void keyRow(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            ClientModule listeningModule2) {
        float px140 = EfnWidgets.s(140.0F);
        float px32 = EfnWidgets.s(32.0F);
        float g2 = f + h - EfnWidgets.s(18.0F) - px140;
        float h2 = g + (f2 - px32) * 0.5F;
        boolean bl = this.listeningModule == listeningModule2;
        String string =
                bl
                        ? "Press a key..."
                        : (listeningModule2.keybind() < 0
                                ? "None"
                                : this.keyLabel(listeningModule2.keybind()));
        if (this.pill(imDrawList, g2, h2, px140, px32, string, bl) && this.clicked) {
            this.listeningKey = null;
            this.listeningModule = listeningModule2;
            this.listenFirstFrame = true;
            this.popupFor = null;
        }
    }

    private void settingRow(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            ClientModule module,
            Setting setting) {
        this.settingRow(imDrawList, f, g, h, f2, module.id(), module, setting);
    }

    private void settingRow(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            String string,
            ClientModule module,
            Setting setting) {
        float px18 = EfnWidgets.s(18.0F);
        float g2 = f + h - px18;
        String string2 = string + "/" + setting.id();
        if (setting.description() == null || setting.description().isEmpty()) {
            this.rowLabel(imDrawList, f, g, f2, setting.name());
        } else {
            this.rowLabelWithDescription(imDrawList, f, g, h, f2, setting);
        }
        if (setting instanceof BooleanSetting setting2) {
            float px44 = EfnWidgets.s(44.0F);
            float px24 = EfnWidgets.s(24.0F);
            float h2 = g2 - px44;
            float f3 = g + (f2 - px24) * 0.5F;
            float[] floats = this.state("bs:" + string2, setting2.get());
            floats[1] +=
                    ((setting2.get() ? 1.0F : 0.0F) - floats[1]) * Math.min(1.0F, this.dt * 10.0F);
            this.drawSwitch(imDrawList, h2, f3, px44, px24, floats[1]);
            if (this.clicked && this.hot(f, g, h, f2)) {
                setting2.set(!setting2.get());
            }
        } else if (setting instanceof NumberSetting) {
            this.sliderRow(imDrawList, f, g, h, f2, string2, (NumberSetting) setting);
        } else if (setting instanceof ModeSetting) {
            this.modeRow(imDrawList, g2, g, f2, string2, (ModeSetting) setting);
        } else if (setting instanceof ColorSetting) {
            this.colorRow(imDrawList, g2, g, f2, string2, (ColorSetting) setting);
        } else if (setting instanceof KeySetting setting3) {
            float px140 = EfnWidgets.s(140.0F);
            float px32 = EfnWidgets.s(32.0F);
            float g3 = g2 - px140;
            float h3 = g + (f2 - px32) * 0.5F;
            boolean bl = this.listeningKey == setting3;
            String string3 =
                    bl
                            ? "Press a key..."
                            : (setting3.value() < 0 ? "None" : this.keyLabel(setting3.value()));
            if (this.pill(imDrawList, g3, h3, px140, px32, string3, bl) && this.clicked) {
                this.listeningModule = null;
                this.listeningKey = setting3;
                this.listenFirstFrame = true;
                this.popupFor = null;
            }
        } else if (setting instanceof StringSetting) {
            this.stringRow(imDrawList, f, g2, g, f2, string2, (StringSetting) setting);
        } else if (setting instanceof ActionSetting) {
            float px96 = EfnWidgets.s(96.0F);
            float px322 = EfnWidgets.s(32.0F);
            if (this.pill(imDrawList, g2 - px96, g + (f2 - px322) * 0.5F, px96, px322, "Run", false)
                    && this.clicked) {
                ((ActionSetting) setting).run();
            }
        } else {
            float px130 = EfnWidgets.s(130.0F);
            float px323 = EfnWidgets.s(32.0F);
            if (module != null
                    && this.pill(
                            imDrawList,
                            g2 - px130,
                            g + (f2 - px323) * 0.5F,
                            px130,
                            px323,
                            "Open editor",
                            false)
                    && this.clicked) {
                this.legacy.showModuleSettings(module);
                this.view = EfnClickGui.View.LEGACY;
            }
        }
    }

    private void sliderRow(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            String dragId2,
            NumberSetting setting) {
        int accent = GuiPalette.accent();
        float px18 = EfnWidgets.s(18.0F);
        float px78 = EfnWidgets.s(78.0F);
        float px30 = EfnWidgets.s(30.0F);
        float g2 = f + h - px18 - px78;
        float h2 = g + (f2 - px30) * 0.5F;
        imDrawList.addRectFilled(
                g2, h2, g2 + px78, h2 + px30, this.f(BG_FIELD), EfnWidgets.s(8.0F));
        imDrawList.addRect(g2, h2, g2 + px78, h2 + px30, this.f(LINE), EfnWidgets.s(8.0F), 0, 1.0F);
        String formatted = setting.formatted();
        float textWidth = EfnWidgets.textWidth(this.fSmall, formatted);
        EfnWidgets.text(
                imDrawList,
                this.fSmall,
                g2 + (px78 - textWidth) * 0.5F,
                h2 + (px30 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F,
                this.f(TEXT),
                formatted);
        float f3 = Math.min(EfnWidgets.s(320.0F), h * 0.38F);
        float g3 = g2 - EfnWidgets.s(18.0F) - f3;
        float h3 = g + f2 * 0.5F;
        float f4 = setting.max() - setting.min();
        float g4 = f4 <= 0.0F ? 0.0F : (setting.get() - setting.min()) / f4;
        g4 = Math.max(0.0F, Math.min(1.0F, g4));
        float[] floats = this.state("sl:" + dragId2, false);
        floats[0] += (g4 - floats[0]) * Math.min(1.0F, this.dt * 20.0F);
        float px6 = EfnWidgets.s(6.0F);
        imDrawList.addRectFilled(
                g3, h3 - px6 * 0.5F, g3 + f3, h3 + px6 * 0.5F, this.f(SWITCH_OFF), px6 * 0.5F);
        imDrawList.addRectFilled(
                g3,
                h3 - px6 * 0.5F,
                g3 + f3 * floats[0],
                h3 + px6 * 0.5F,
                this.f(accent),
                px6 * 0.5F);
        float h4 = g3 + f3 * floats[0];
        imDrawList.addCircleFilled(h4, h3, EfnWidgets.s(8.5F), this.f(a(accent, 0.25F)), 20);
        imDrawList.addCircleFilled(h4, h3, EfnWidgets.s(6.5F), this.f(KNOB), 20);
        if (this.clicked
                && this.hot(
                        g3 - EfnWidgets.s(8.0F),
                        h3 - EfnWidgets.s(13.0F),
                        f3 + EfnWidgets.s(16.0F),
                        EfnWidgets.s(26.0F))) {
            this.dragId = dragId2;
        }
        if (dragId2.equals(this.dragId)) {
            if (this.down) {
                float f5 = Math.max(0.0F, Math.min(1.0F, (this.mx - g3) / f3));
                setting.set(setting.min() + f5 * f4);
            } else {
                this.dragId = null;
            }
        }
    }

    private void modeRow(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            String popupFor2,
            ModeSetting setting) {
        float px190 = EfnWidgets.s(190.0F);
        float px32 = EfnWidgets.s(32.0F);
        float f2 = f - px190;
        float g2 = g + (h - px32) * 0.5F;
        boolean bl = popupFor2.equals(this.popupFor);
        if (this.pill(imDrawList, f2, g2, px190, px32, setting.get(), bl) && this.clicked) {
            this.popupFor = popupFor2;
            ImGui.openPopup("##efn_mode");
        }
        EfnWidgets.chevron(
                imDrawList,
                f2 + px190 - EfnWidgets.s(16.0F),
                g2 + px32 * 0.5F,
                EfnWidgets.s(4.0F),
                this.f(MUTED),
                bl);
        if (bl) {
            ImGui.setNextWindowPos(f2, g2 + px32 + EfnWidgets.s(4.0F));
            ImGui.setNextWindowSize(px190, 0.0F);
            this.pushPopupStyle();
            if (!ImGui.beginPopup("##efn_mode")) {
                this.popupFor = null;
            } else {
                for (String mode : setting.modes()) {
                    if (ImGui.selectable(mode, mode.equals(setting.get()))) {
                        setting.set(mode);
                        this.popupFor = null;
                        ImGui.closeCurrentPopup();
                    }
                }
                ImGui.endPopup();
            }
            this.popPopupStyle();
        }
    }

    private void colorRow(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            String popupFor2,
            ColorSetting setting) {
        float px64 = EfnWidgets.s(64.0F);
        float px28 = EfnWidgets.s(28.0F);
        float f2 = f - px64;
        float g2 = g + (h - px28) * 0.5F;
        boolean bl = popupFor2.equals(this.popupFor);
        int argbWithAlpha = setting.argbWithAlpha(255);
        imDrawList.addRectFilled(
                f2,
                g2,
                f2 + px64,
                g2 + px28,
                this.f(GuiPalette.abgr(argbWithAlpha)),
                EfnWidgets.s(8.0F));
        imDrawList.addRect(
                f2,
                g2,
                f2 + px64,
                g2 + px28,
                this.f(bl ? GuiPalette.accent() : rgba(255, 255, 255, 60)),
                EfnWidgets.s(8.0F),
                0,
                1.4F);
        if (this.clicked && this.hot(f2, g2, px64, px28)) {
            this.popupFor = popupFor2;
            ImGui.openPopup("##efn_color");
        }
        if (bl) {
            ImGui.setNextWindowPos(
                    f2 + px64 - EfnWidgets.s(260.0F), g2 + px28 + EfnWidgets.s(6.0F));
            this.pushPopupStyle();
            if (ImGui.beginPopup("##efn_color")) {
                int i = 65808;
                if (ImGui.colorPicker4("##efn_cp", setting.value(), i)) {
                    setting.notifyChanged();
                }
                ImGui.endPopup();
            } else {
                this.popupFor = null;
            }
            this.popPopupStyle();
        }
    }

    private void stringRow(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            String string,
            StringSetting setting) {
        float g2 = Math.min(EfnWidgets.s(300.0F), (g - f) * 0.45F);
        float px32 = EfnWidgets.s(32.0F);
        float h2 = g - g2;
        float f3 = h + (f2 - px32) * 0.5F;
        imDrawList.addRectFilled(h2, f3, h2 + g2, f3 + px32, this.f(BG_FIELD), EfnWidgets.s(8.0F));
        imDrawList.addRect(h2, f3, h2 + g2, f3 + px32, this.f(LINE), EfnWidgets.s(8.0F), 0, 1.0F);
        ImGui.setCursorScreenPos(
                h2 + EfnWidgets.s(12.0F), f3 + (px32 - EfnWidgets.lineHeight(this.fMod)) * 0.5F);
        ImGui.setNextItemWidth(g2 - EfnWidgets.s(24.0F));
        ImGui.pushStyleColor(7, 0);
        ImGui.pushStyleColor(8, 0);
        ImGui.pushStyleColor(9, 0);
        ImGui.pushStyleColor(0, TEXT);
        ImGui.pushStyleVar(11, 0.0F, 0.0F);
        ImString imString = setting.getImString();
        if (ImGui.inputText("##efn_s_" + string, imString)) {
            setting.set(imString.get());
        }
        ImGui.popStyleVar(1);
        ImGui.popStyleColor(4);
    }

    private void renderPageHeader(ImDrawList imDrawList, float f, float g, float h) {
        int accent = GuiPalette.accent();
        String string = PAGE_FIRST[this.page];
        EfnWidgets.text(imDrawList, this.fTitle, f, g, this.f(accent), string);
        float textWidth = EfnWidgets.textWidth(this.fTitle, string + " ");
        EfnWidgets.text(
                imDrawList, this.fTitle, f + textWidth, g, this.f(TEXT), PAGE_REST[this.page]);
        EfnWidgets.text(
                imDrawList,
                this.fSmall,
                f,
                g + EfnWidgets.lineHeight(this.fTitle) + EfnWidgets.s(6.0F),
                this.f(MUTED),
                switch (this.page) {
                    case 0 -> "Move and restyle your on-screen elements.";
                    case 1 -> {
                        int i = FriendStore.getInstance().all().size();
                        yield i == 1 ? "1 player" : i + " players";
                    }
                    case 2 -> "Accent colour and how the client looks.";
                    default -> {
                        String activeProfile = ModuleConfigStore.getInstance().activeProfile();
                        int j = ConfigProfiles.getInstance().list().size();
                        yield j
                                + (j == 1 ? " profile" : " profiles")
                                + (activeProfile != null
                                        ? "  \u00b7  active: " + activeProfile
                                        : "");
                    }
                });
        if (this.page == 2) {
            float px96 = EfnWidgets.s(96.0F);
            float px32 = EfnWidgets.s(32.0F);
            if (this.pill(
                            imDrawList,
                            f + h - px96,
                            g + EfnWidgets.s(4.0F),
                            px96,
                            px32,
                            "Reset all",
                            false)
                    && this.clicked) {
                ThemeStore.get().resetToDefaults();
            }
        }
    }

    private void renderPage(float f, float g, float h, float f2) {
        ImGui.setCursorScreenPos(f, g);
        this.pushChildStyle();
        if (ImGui.beginChild("##efn_page", h, f2, false, 0)) {
            this.winHover = ImGui.isWindowHovered();
            ImDrawList windowDrawList = ImGui.getWindowDrawList();
            float cursorScreenPosX = ImGui.getCursorScreenPosX();
            float g2 = ImGui.getCursorScreenPosY() + EfnWidgets.s(6.0F);
            float h2 = h - EfnWidgets.s(12.0F);
            float pageMessage =
                    this.pageMessage(
                            windowDrawList,
                            cursorScreenPosX,
                            switch (this.page) {
                                        case 0 ->
                                                this.pageHud(
                                                        windowDrawList, cursorScreenPosX, g2, h2);
                                        case 1 ->
                                                this.pageFriends(
                                                        windowDrawList, cursorScreenPosX, g2, h2);
                                        case 2 ->
                                                this.pageTheme(
                                                        windowDrawList, cursorScreenPosX, g2, h2);
                                        default ->
                                                this.pageConfigs(
                                                        windowDrawList, cursorScreenPosX, g2, h2);
                                    }
                                    + EfnWidgets.s(12.0F),
                            h2);
            ImGui.setCursorScreenPos(cursorScreenPosX, pageMessage + EfnWidgets.s(24.0F));
            ImGui.dummy(1.0F, 1.0F);
        }
        ImGui.endChild();
        this.popChildStyle();
    }

    private void say(String pageMsg2, boolean pageMsgErr2) {
        this.pageMsg = pageMsg2;
        this.pageMsgErr = pageMsgErr2;
        this.pageMsgAt = System.nanoTime();
    }

    private float pageMessage(ImDrawList imDrawList, float f, float g, float h) {
        if (this.pageMsg.isEmpty()) {
            return g - EfnWidgets.s(12.0F);
        } else if ((float) (System.nanoTime() - this.pageMsgAt) / 1.0E9F > 6.0F) {
            this.pageMsg = "";
            return g - EfnWidgets.s(12.0F);
        } else {
            int i = this.pageMsgErr ? rgba(255, 110, 110, 255) : GuiPalette.accent();
            EfnWidgets.text(
                    imDrawList, this.fSmall, f + EfnWidgets.s(4.0F), g, this.f(i), this.pageMsg);
            return g + EfnWidgets.lineHeight(this.fSmall);
        }
    }

    private float rowsTop(ImDrawList imDrawList, float f, float g, float h, float f2, int i) {
        float g2 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding());
        this.beginContainer(imDrawList, f, g, h, i * f2, g2);
        for (int k = 1; k < i; k++) {
            float h2 = g + k * f2;
            imDrawList.addLine(
                    f + EfnWidgets.s(18.0F),
                    h2,
                    f + h - EfnWidgets.s(18.0F),
                    h2,
                    this.f(LINE),
                    1.0F);
        }
        return g + i * f2;
    }

    private boolean textField(
            String string, float f, float g, float h, float f2, String string2, ImString imString) {
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        windowDrawList.addRectFilled(f, g, f + h, g + f2, this.f(BG_FIELD), EfnWidgets.s(8.0F));
        windowDrawList.addRect(f, g, f + h, g + f2, this.f(LINE), EfnWidgets.s(8.0F), 0, 1.0F);
        ImGui.setCursorScreenPos(
                f + EfnWidgets.s(12.0F), g + (f2 - EfnWidgets.lineHeight(this.fMod)) * 0.5F);
        ImGui.setNextItemWidth(h - EfnWidgets.s(24.0F));
        ImGui.pushStyleColor(7, 0);
        ImGui.pushStyleColor(8, 0);
        ImGui.pushStyleColor(9, 0);
        ImGui.pushStyleColor(0, TEXT);
        ImGui.pushStyleColor(1, MUTED);
        ImGui.pushStyleVar(11, 0.0F, 0.0F);
        boolean inputTextWithHint = ImGui.inputTextWithHint(string, string2, imString, 64);
        ImGui.popStyleVar(1);
        ImGui.popStyleColor(5);
        return inputTextWithHint;
    }

    private float pageTheme(ImDrawList imDrawList, float f, float g, float h) {
        ThemeStore themeStore = ThemeStore.get();
        float px54 = EfnWidgets.s(54.0F);
        float f2 = g;
        float rowsTop = this.rowsTop(imDrawList, f, g, h, px54, 2);
        this.rowLabel(imDrawList, f, g, px54, "Accent colour");
        float px11 = EfnWidgets.s(11.0F);
        float px12 = EfnWidgets.s(12.0F);
        float g2 = f + h - EfnWidgets.s(18.0F);
        int i = ThemeStore.PRESETS.size();
        float h2 = g2 - i * (2.0F * px11) - (i - 1) * px12 + px11;
        int argbWithAlpha = themeStore.accent.argbWithAlpha(255);
        for (int k = 0; k < i; k++) {
            ThemeStore.Preset preset = ThemeStore.PRESETS.get(k);
            float f3 = h2 + k * (2.0F * px11 + px12);
            float g3 = f2 + px54 * 0.5F;
            boolean hot =
                    this.hot(
                            f3 - px11 - 3.0F,
                            g3 - px11 - 3.0F,
                            2.0F * px11 + 6.0F,
                            2.0F * px11 + 6.0F);
            float[] floats = this.state("sw:" + k, false);
            floats[0] += ((hot ? 1.0F : 0.0F) - floats[0]) * Math.min(1.0F, this.dt * 14.0F);
            boolean bl = (preset.accent() | 0xFF000000) == (argbWithAlpha | 0xFF000000);
            imDrawList.addCircleFilled(
                    f3,
                    g3,
                    px11 + EfnWidgets.s(1.5F) * floats[0],
                    this.f(GuiPalette.abgr(preset.accent() | 0xFF000000)),
                    28);
            if (bl) {
                imDrawList.addCircle(f3, g3, px11 + EfnWidgets.s(5.0F), this.f(TEXT), 28, 1.6F);
            }
            if (hot && this.clicked) {
                themeStore.applyPreset(preset);
            }
        }
        float h3 = f2 + px54;
        this.rowLabel(imDrawList, f, h3, px54, "Custom colour");
        this.colorRow(imDrawList, g2, h3, px54, "theme/accent", themeStore.accent);
        f2 = rowsTop + EfnWidgets.s(14.0F);
        ArrayList list = new ArrayList();
        list.add(themeStore.rounding);
        list.add(themeStore.backgroundDim);
        list.add(themeStore.menuBlur);
        list.add(themeStore.uiScale);
        list.add(themeStore.font);
        list.add(themeStore.animationSpeed);
        list.add(themeStore.customMainMenu);
        rowsTop = this.rowsTop(imDrawList, f, f2, h, px54, list.size());
        for (int m = 0; m < list.size(); m++) {
            this.settingRow(
                    imDrawList, f, f2 + m * px54, h, px54, "theme", null, (Setting) list.get(m));
        }
        return rowsTop;
    }

    private float pageFriends(ImDrawList imDrawList, float f, float g, float h) {
        FriendStore friendStore = FriendStore.getInstance();
        float px54 = EfnWidgets.s(54.0F);
        float px18 = EfnWidgets.s(18.0F);
        float rowsTop = this.rowsTop(imDrawList, f, g, h, px54, 1);
        this.rowLabel(imDrawList, f, g, px54, "Add player");
        float px84 = EfnWidgets.s(84.0F);
        float px32 = EfnWidgets.s(32.0F);
        float f2 = Math.min(EfnWidgets.s(280.0F), h * 0.4F);
        float g2 = f + h - px18 - px84;
        float h2 = g2 - EfnWidgets.s(10.0F) - f2;
        float f3 = g + (px54 - px32) * 0.5F;
        boolean textField =
                this.textField(
                        "##efn_friend", h2, f3, f2, px32, "Exact username", this.friendInput);
        boolean bl = this.pill(imDrawList, g2, f3, px84, px32, "Add", false) && this.clicked;
        if (textField || bl) {
            String string = this.friendInput.get().trim();
            if (!FriendStore.isValidName(string)) {
                this.say("That is not a valid Minecraft username", true);
            } else if (!friendStore.add(string)) {
                this.say(string + " is already on your list", true);
            } else {
                friendStore.save();
                this.friendInput.set("");
                this.say("Added " + string, false);
            }
        }
        float g3 = rowsTop + EfnWidgets.s(14.0F);
        List list = friendStore.all();
        if (list.isEmpty()) {
            float h3 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding());
            this.beginContainer(imDrawList, f, g3, h, px54 * 1.4F, h3);
            String string2 =
                    "No friends yet. Players you add are protected from your combat modules.";
            float textWidth = EfnWidgets.textWidth(this.fSmall, string2);
            EfnWidgets.text(
                    imDrawList,
                    this.fSmall,
                    f + (h - textWidth) * 0.5F,
                    g3 + (px54 * 1.4F - EfnWidgets.lineHeight(this.fSmall)) * 0.5F,
                    this.f(MUTED),
                    string2);
            return g3 + px54 * 1.4F;
        } else {
            rowsTop = this.rowsTop(imDrawList, f, g3, h, px54, list.size());
            for (int i = 0; i < list.size(); i++) {
                String string3 = (String) list.get(i);
                float f4 = g3 + i * px54;
                this.rowLabel(imDrawList, f, f4, px54, string3);
                float px90 = EfnWidgets.s(90.0F);
                if (this.pill(
                                imDrawList,
                                f + h - px18 - px90,
                                f4 + (px54 - px32) * 0.5F,
                                px90,
                                px32,
                                "Remove",
                                false)
                        && this.clicked) {
                    friendStore.remove(string3);
                    friendStore.save();
                    this.say("Removed " + string3, false);
                }
            }
            return rowsTop;
        }
    }

    private float pageConfigs(ImDrawList imDrawList, float f, float g, float h) {
        ConfigProfiles configProfiles = ConfigProfiles.getInstance();
        float px54 = EfnWidgets.s(54.0F);
        float px18 = EfnWidgets.s(18.0F);
        float px32 = EfnWidgets.s(32.0F);
        float rowsTop = this.rowsTop(imDrawList, f, g, h, px54, 2);
        this.rowLabel(imDrawList, f, g, px54, "New profile");
        float px110 = EfnWidgets.s(110.0F);
        float f2 = Math.min(EfnWidgets.s(280.0F), h * 0.4F);
        float g2 = f + h - px18 - px110;
        float h2 = g2 - EfnWidgets.s(10.0F) - f2;
        float f3 = g + (px54 - px32) * 0.5F;
        boolean textField =
                this.textField(
                        "##efn_profile", h2, f3, f2, px32, "Profile name", this.profileInput);
        boolean bl =
                this.pill(imDrawList, g2, f3, px110, px32, "Save current", false) && this.clicked;
        if (textField || bl) {
            try {
                String create = configProfiles.create(this.profileInput.get());
                configProfiles.save(create);
                this.profileInput.set("");
                this.say("Saved " + create, false);
            } catch (ProfileException exception) {
                this.say(exception.getMessage(), true);
            }
        }
        float g3 = g + px54;
        this.rowLabel(imDrawList, f, g3, px54, "Import from clipboard");
        float px1102 = EfnWidgets.s(110.0F);
        if (this.pill(
                        imDrawList,
                        f + h - px18 - px1102,
                        g3 + (px54 - px32) * 0.5F,
                        px1102,
                        px32,
                        "Import",
                        false)
                && this.clicked) {
            try {
                String importCode = configProfiles.importCode(ImGui.getClipboardText());
                this.say("Imported " + importCode, false);
            } catch (ProfileException exception2) {
                this.say(exception2.getMessage(), true);
            }
        }
        float h3 = rowsTop + EfnWidgets.s(14.0F);
        List list = configProfiles.list();
        if (list.isEmpty()) {
            float f4 = Math.max(EfnWidgets.s(8.0F), GuiPalette.rounding());
            this.beginContainer(imDrawList, f, h3, h, px54 * 1.4F, f4);
            String string = "No profiles yet. Name one above and save your current setup.";
            float textWidth = EfnWidgets.textWidth(this.fSmall, string);
            EfnWidgets.text(
                    imDrawList,
                    this.fSmall,
                    f + (h - textWidth) * 0.5F,
                    h3 + (px54 * 1.4F - EfnWidgets.lineHeight(this.fSmall)) * 0.5F,
                    this.f(MUTED),
                    string);
            return h3 + px54 * 1.4F;
        } else {
            String activeProfile = ModuleConfigStore.getInstance().activeProfile();
            if (this.armedDelete != null
                    && (float) (System.nanoTime() - this.armedAt) / 1.0E9F > 3.0F) {
                this.armedDelete = null;
            }
            int accent = GuiPalette.accent();
            float px70 = EfnWidgets.s(70.0F);
            float px8 = EfnWidgets.s(8.0F);
            rowsTop = this.rowsTop(imDrawList, f, h3, h, px54, list.size());
            for (int j = 0; j < list.size(); j++) {
                ConfigProfiles.Profile profile = (ConfigProfiles.Profile) list.get(j);
                float g4 = h3 + j * px54;
                float h4 = g4 + (px54 - px32) * 0.5F;
                boolean equalsIgnoreCase = profile.name().equalsIgnoreCase(activeProfile);
                float f5 = f + px18;
                if (equalsIgnoreCase) {
                    imDrawList.addCircleFilled(
                            f5 + EfnWidgets.s(4.0F),
                            g4 + px54 * 0.5F,
                            EfnWidgets.s(3.5F),
                            this.f(accent),
                            12);
                    f5 += EfnWidgets.s(16.0F);
                }
                float g5 = px70 * 3.0F + EfnWidgets.s(80.0F) + px8 * 3.0F;
                String ellipsize =
                        EfnWidgets.ellipsize(
                                this.fMod,
                                profile.name(),
                                h - px18 * 2.0F - g5 - EfnWidgets.s(20.0F));
                EfnWidgets.text(
                        imDrawList,
                        this.fMod,
                        f5,
                        g4 + px54 * 0.5F - EfnWidgets.lineHeight(this.fMod) + EfnWidgets.s(4.0F),
                        this.f(TEXT),
                        ellipsize);
                StringBuilder sb = new StringBuilder();
                for (ConfigProfiles.Part part : profile.parts()) {
                    if (sb.length() > 0) {
                        sb.append("  \u00b7  ");
                    }
                    sb.append(part.label);
                }
                EfnWidgets.text(
                        imDrawList,
                        this.fSmall,
                        f5,
                        g4 + px54 * 0.5F + EfnWidgets.s(6.0F),
                        this.f(profile.broken() ? rgba(255, 110, 110, 255) : MUTED),
                        profile.broken() ? "Could not be read" : sb.toString());
                float h5 = f + h - px18 - EfnWidgets.s(80.0F);
                boolean bl2 = profile.name().equals(this.armedDelete);
                if (this.pill(
                                imDrawList,
                                h5,
                                h4,
                                EfnWidgets.s(80.0F),
                                px32,
                                bl2 ? "Sure?" : "Delete",
                                bl2)
                        && this.clicked) {
                    if (bl2) {
                        try {
                            configProfiles.delete(profile.name());
                            this.say("Deleted " + profile.name(), false);
                        } catch (ProfileException exception3) {
                            this.say(exception3.getMessage(), true);
                        }
                        this.armedDelete = null;
                    } else {
                        this.armedDelete = profile.name();
                        this.armedAt = System.nanoTime();
                    }
                }
                h5 -= px8 + px70;
                if (this.pill(imDrawList, h5, h4, px70, px32, "Share", false) && this.clicked) {
                    try {
                        ImGui.setClipboardText(configProfiles.shareCode(profile.name()));
                        this.say("Share code for " + profile.name() + " copied", false);
                    } catch (ProfileException exception4) {
                        this.say(exception4.getMessage(), true);
                    }
                }
                h5 -= px8 + px70;
                if (this.pill(imDrawList, h5, h4, px70, px32, "Save", false) && this.clicked) {
                    try {
                        configProfiles.save(profile.name());
                        this.say("Saved over " + profile.name(), false);
                    } catch (ProfileException exception5) {
                        this.say(exception5.getMessage(), true);
                    }
                }
                h5 -= px8 + px70;
                if (this.pill(imDrawList, h5, h4, px70, px32, "Load", equalsIgnoreCase)
                        && this.clicked) {
                    try {
                        configProfiles.load(profile.name());
                        this.say("Loaded " + profile.name(), false);
                    } catch (ProfileException exception6) {
                        this.say(exception6.getMessage(), true);
                    }
                }
            }
            return rowsTop;
        }
    }

    private float pageHud(ImDrawList imDrawList, float f, float g, float h) {
        float px54 = EfnWidgets.s(54.0F);
        float px18 = EfnWidgets.s(18.0F);
        float px32 = EfnWidgets.s(32.0F);
        float rowsTop = this.rowsTop(imDrawList, f, g, h, px54, 2);
        this.rowLabel(imDrawList, f, g, px54, "Layout editor");
        float px110 = EfnWidgets.s(110.0F);
        if (this.pill(
                        imDrawList,
                        f + h - px18 - px110,
                        g + (px54 - px32) * 0.5F,
                        px110,
                        px32,
                        "Open",
                        false)
                && this.clicked) {
            MinecraftClient.getInstance().setScreen(new HudEditorScreen());
        }
        float f2 = g + px54;
        this.rowLabel(imDrawList, f, f2, px54, "Reset layout");
        if (this.pill(
                        imDrawList,
                        f + h - px18 - px110,
                        f2 + (px54 - px32) * 0.5F,
                        px110,
                        px32,
                        "Reset",
                        false)
                && this.clicked) {
            HudLayoutStore.getInstance()
                    .resetToDefaults(
                            ImGui.getIO().getDisplaySizeX(), ImGui.getIO().getDisplaySizeY());
            this.say("HUD layout reset", false);
        }
        ClientModule module = null;
        for (ClientModule module2 : ModuleRegistry.all()) {
            if (module2.category() == ModuleCategory.CLIENT && "HUD".equals(module2.name())) {
                module = module2;
                break;
            }
        }
        if (module == null) {
            return rowsTop;
        } else {
            ArrayList list = new ArrayList();
            for (Setting setting : module.settings()) {
                if (setting.isVisible()) {
                    list.add(setting);
                }
            }
            if (list.isEmpty()) {
                return rowsTop;
            } else {
                float g2 = rowsTop + EfnWidgets.s(14.0F);
                rowsTop = this.rowsTop(imDrawList, f, g2, h, px54, list.size());
                for (int i = 0; i < list.size(); i++) {
                    this.settingRow(
                            imDrawList, f, g2 + i * px54, h, px54, module, (Setting) list.get(i));
                }
                return rowsTop;
            }
        }
    }

    private boolean pill(
            ImDrawList imDrawList, float f, float g, float h, float f2, String string, boolean bl) {
        boolean hot = this.hot(f, g, h, f2);
        imDrawList.addRectFilled(
                f, g, f + h, g + f2, this.f(hot ? BG_CARD_HOVER : BG_FIELD), EfnWidgets.s(9.0F));
        imDrawList.addRect(
                f,
                g,
                f + h,
                g + f2,
                this.f(bl ? GuiPalette.accent() : LINE),
                EfnWidgets.s(9.0F),
                0,
                bl ? 1.4F : 1.0F);
        String ellipsize = EfnWidgets.ellipsize(this.fSmall, string, h - EfnWidgets.s(24.0F));
        float textWidth = EfnWidgets.textWidth(this.fSmall, ellipsize);
        EfnWidgets.text(
                imDrawList,
                this.fSmall,
                f + (h - textWidth) * 0.5F,
                g + (f2 - EfnWidgets.lineHeight(this.fSmall)) * 0.5F,
                this.f(TEXT),
                ellipsize);
        return hot;
    }

    private void trackView() {
        String string =
                (this.category == null ? "" : this.category.name()) + "|" + this.search.get();
        if (!string.equals(this.viewKey)) {
            this.viewKey = string;
            this.viewChangedAt = System.nanoTime();
        }
    }

    private float sinceViewChange() {
        return (float) ((System.nanoTime() - this.viewChangedAt) / 1.0E9);
    }

    private void drawLogo(ImDrawList imDrawList, float f, float g, float h, int i) {
        float f2 = h / 64.0F;
        imDrawList.addRectFilled(f, g, f + h, g + h, this.f(rgba(26, 29, 36, 255)), 15.0F * f2);
        float[][] floatGrid =
                new float[][] {
                    {14.0F, 12.0F, 50.0F, 22.0F},
                    {14.0F, 22.0F, 27.0F, 42.0F},
                    {27.0F, 27.0F, 44.0F, 37.0F},
                    {14.0F, 42.0F, 50.0F, 52.0F}
                };
        for (float[] floats : floatGrid) {
            float g2 = floats[0];
            float h2 = floats[1];
            float f3 = floats[2];
            float g3 = floats[3];
            float h3 = f + (g2 - 0.25F * h2 + 8.0F) * f2;
            float f4 = g + h2 * f2;
            float g4 = f + (f3 - 0.25F * h2 + 8.0F) * f2;
            float h4 = f + (f3 - 0.25F * g3 + 8.0F) * f2;
            float f5 = g + g3 * f2;
            float g5 = f + (g2 - 0.25F * g3 + 8.0F) * f2;
            imDrawList.addQuadFilled(h3, f4, g4, f4, h4, f5, g5, f5, this.f(i));
        }
    }

    private void drawSwitch(ImDrawList imDrawList, float f, float g, float h, float f2, float g2) {
        float h2 = f2 * 0.5F;
        imDrawList.addRectFilled(
                f,
                g,
                f + h,
                g + f2,
                this.f(EfnTheme.lerpColor(SWITCH_OFF, GuiPalette.accent(), g2)),
                h2);
        float f3 = h2 - EfnWidgets.s(3.0F);
        float g3 = f + h2 + (h - 2.0F * h2) * g2;
        imDrawList.addCircleFilled(
                g3, g + h2 + EfnWidgets.s(1.0F), f3, this.f(rgba(0, 0, 0, 50)), 20);
        imDrawList.addCircleFilled(g3, g + h2, f3, this.f(KNOB), 20);
    }

    private void drawCog(ImDrawList imDrawList, float f, float g, float h, int i) {
        imDrawList.addCircle(f, g, h * 0.55F, i, 14, 1.5F);
        for (int j = 0; j < 8; j++) {
            double d = 6.283185307179586 * j / 8.0;
            float f2 = (float) Math.cos(d);
            float g2 = (float) Math.sin(d);
            imDrawList.addLine(
                    f + f2 * h * 0.85F,
                    g + g2 * h * 0.85F,
                    f + f2 * h * 1.15F,
                    g + g2 * h * 1.15F,
                    i,
                    2.0F);
        }
        imDrawList.addCircle(f, g, h * 0.95F, i, 18, 1.5F);
    }

    private void drawIcon(
            ImDrawList imDrawList, ModuleCategory.Icon icon, float f, float g, float h, int i) {
        switch (icon) {
            case CROSSHAIR:
                EfnWidgets.crosshair(imDrawList, f, g, h, i);
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
                break;
            default:
                EfnWidgets.chevronRight(imDrawList, f, g, h, i);
        }
    }

    private void drawGeneralIcon(ImDrawList imDrawList, int i, float f, float g, float h, int j) {
        switch (i) {
            case 0:
                EfnWidgets.monitor(imDrawList, f, g, h, j);
                break;
            case 1:
                EfnWidgets.ring(imDrawList, f, g, h, j);
                break;
            case 2:
                EfnWidgets.eye(imDrawList, f, g, h, j);
                break;
            default:
                EfnWidgets.grid(imDrawList, f, g, h, j);
        }
    }

    private void renderBackPill(float f, float g) {
        ImDrawList foregroundDrawList = ImGui.getForegroundDrawList();
        float px118 = EfnWidgets.s(118.0F);
        float px36 = EfnWidgets.s(36.0F);
        float px20 = EfnWidgets.s(20.0F);
        float h = g - EfnWidgets.s(20.0F) - px36;
        float mousePosX = ImGui.getMousePosX();
        float mousePosY = ImGui.getMousePosY();
        boolean bl =
                mousePosX >= px20
                        && mousePosX < px20 + px118
                        && mousePosY >= h
                        && mousePosY < h + px36;
        foregroundDrawList.addRectFilled(
                px20, h, px20 + px118, h + px36, bl ? BG_CARD_HOVER : BG_SIDEBAR, px36 * 0.5F);
        foregroundDrawList.addRect(
                px20, h, px20 + px118, h + px36, GuiPalette.accent(), px36 * 0.5F, 0, 1.3F);
        EfnWidgets.chevronLeft(
                foregroundDrawList,
                px20 + EfnWidgets.s(20.0F),
                h + px36 * 0.5F,
                EfnWidgets.s(5.0F),
                TEXT);
        ImFont imFont = ImGuiManager.getInstance().smallFont();
        EfnWidgets.text(
                foregroundDrawList,
                imFont,
                px20 + EfnWidgets.s(36.0F),
                h + (px36 - EfnWidgets.lineHeight(imFont)) * 0.5F,
                TEXT,
                "efn");
        if (bl && ImGui.isMouseClicked(0)) {
            this.view = EfnClickGui.View.MODULES;
        }
    }

    private void updateKeyListening() {
        if (this.listeningModule != null || this.listeningKey != null) {
            if (this.listenFirstFrame) {
                this.listenFirstFrame = false;
            } else {
                int scanPressedKey = ClickGuiRenderer.scanPressedKey();
                if (scanPressedKey != -1) {
                    if (this.listeningKey != null) {
                        this.listeningKey.setValue(scanPressedKey);
                    } else {
                        this.listeningModule.setKeybind(scanPressedKey);
                    }
                    this.listeningKey = null;
                    this.listeningModule = null;
                }
            }
        }
    }

    private void toggle(ClientModule module) {
        if (module.isAction()) {
            module.onClick();
        } else {
            module.setEnabled(!module.isEnabled());
        }
    }

    private List<ClientModule> currentModules() {
        String string = this.search.get().trim().toLowerCase(Locale.ROOT);
        if (string.isEmpty()) {
            return (List<ClientModule>)
                    (this.category == null
                            ? new ArrayList<>()
                            : ModuleRegistry.inCategory(this.category));
        } else {
            ArrayList list = new ArrayList();
            for (ClientModule module : ModuleRegistry.all()) {
                String string2 =
                        module.description() == null
                                ? ""
                                : module.description().toLowerCase(Locale.ROOT);
                if (module.name().toLowerCase(Locale.ROOT).contains(string)
                        || string2.contains(string)) {
                    list.add(module);
                }
            }
            return list;
        }
    }

    private void refreshCategories() {
        int revision = ModuleRegistry.revision();
        if (revision != this.catCacheRevision) {
            ArrayList list = new ArrayList();
            for (ModuleCategory moduleCategory : ModuleCategory.values()) {
                if (!ModuleRegistry.inCategory(moduleCategory).isEmpty()) {
                    list.add(moduleCategory);
                }
            }
            this.catCache = list;
            this.catCacheRevision = revision;
        }
        if ((this.category == null || !this.catCache.contains(this.category))
                && !this.catCache.isEmpty()) {
            this.category = this.catCache.get(0);
        }
    }

    private String label(ModuleCategory moduleCategory) {
        return moduleCategory.displayName().replace("EFN", "efn".toUpperCase(Locale.ROOT));
    }

    private String keyLabel(int i) {
        String keyLabel = this.keyLabels.get(i);
        if (keyLabel == null) {
            keyLabel = ClickGuiRenderer.keyName(i);
            this.keyLabels.put(i, keyLabel);
        }
        return keyLabel;
    }

    private float[] state(String string, boolean bl) {
        float[] floats = this.anim.get(string);
        if (floats == null) {
            floats = new float[] {0.0F, bl ? 1.0F : 0.0F};
            this.anim.put(string, floats);
        }
        return floats;
    }

    private boolean hot(float f, float g, float h, float f2) {
        return this.winHover
                && !this.popupBlock
                && this.mx >= f
                && this.mx < f + h
                && this.my >= g
                && this.my < g + f2;
    }

    private void pushChildStyle() {
        ImGui.pushStyleColor(3, 0);
        ImGui.pushStyleColor(14, 0);
        ImGui.pushStyleColor(15, a(GuiPalette.accent(), 0.45F));
        ImGui.pushStyleColor(16, a(GuiPalette.accent(), 0.7F));
        ImGui.pushStyleColor(17, GuiPalette.accent());
        ImGui.pushStyleVar(18, EfnWidgets.s(5.0F));
        ImGui.pushStyleVar(2, 0.0F, 0.0F);
    }

    private void popChildStyle() {
        ImGui.popStyleVar(2);
        ImGui.popStyleColor(5);
    }

    private void pushPopupStyle() {
        ImGui.pushStyleColor(4, rgba(16, 14, 30, 252));
        ImGui.pushStyleColor(5, rgba(255, 255, 255, 28));
        ImGui.pushStyleColor(24, a(GuiPalette.accent(), 0.3F));
        ImGui.pushStyleColor(25, a(GuiPalette.accent(), 0.2F));
        ImGui.pushStyleColor(26, a(GuiPalette.accent(), 0.4F));
        ImGui.pushStyleColor(0, TEXT);
        ImGui.pushStyleVar(2, EfnWidgets.s(8.0F), EfnWidgets.s(8.0F));
        ImGui.pushStyleVar(9, EfnWidgets.s(10.0F));
        ImGui.pushStyleVar(10, 1.0F);
        ImGui.pushStyleVar(14, EfnWidgets.s(6.0F), EfnWidgets.s(4.0F));
    }

    private void popPopupStyle() {
        ImGui.popStyleVar(4);
        ImGui.popStyleColor(6);
    }

    private int f(int i) {
        return a(i, this.ga);
    }

    private static int a(int i, float f) {
        int j = Math.round((i >>> 24) * Math.max(0.0F, Math.min(1.0F, f)));
        return j << 24 | i & 16777215;
    }

    private static int rgba(int i, int j, int k, int m) {
        return m << 24 | k << 16 | j << 8 | i;
    }

    private static enum View {
        MODULES,
        PAGE,
        LEGACY;

        private View() {}
    }
}
