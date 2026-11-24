package net.efn.client.imgui;

import com.mojang.blaze3d.opengl.GlStateManager;

import imgui.EfnAndroidNative;
import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImFontGlyphRangesBuilder;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.EfnGl3;
import imgui.glfw.EfnGlfw;
import imgui.glfw.EfnKeys;

import net.efn.client.config.HudTheme;
import net.efn.client.config.ThemeStore;
import net.efn.client.gui.AndroidSystemPicker;
import net.efn.client.gui.GuiPalette;
import net.efn.client.gui.ThemeImages;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.Cursor;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.util.Window;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMonitorCallback;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

@Environment(EnvType.CLIENT)
public final class ImGuiManager {

    private static final ImGuiManager INSTANCE = new ImGuiManager();

    private static final String MODULE_FONT_RESOURCE = "/assets/efn/fonts/geist-medium.ttf";

    private static final String MENU_FONT_RESOURCE = "/assets/efn/fonts/rajdhani-bold.ttf";

    public static final float MENU_FONT_SIZE = 24.0F;

    public static final float MENU_TAG_FONT_SIZE = 15.0F;

    public static final float MENU_BODY_FONT_SIZE = 18.0F;

    public static final float MENU_NAME_FONT_SIZE = 20.0F;

    public static final float MENU_ITEM_FONT_SIZE = 32.0F;

    public static final float HUD_VALUE_FONT_SIZE = 18.0F;

    public static final float HUD_TEXT_FONT_SIZE = 17.0F;

    public static final float HUD_LABEL_FONT_SIZE = 14.0F;

    private static final String EXTENDED_LATIN_GLYPHS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\u00c7\u011e\u0130\u00d6\u015e\u00dc\u00e7\u011f\u0131\u00f6\u015f\u00fc\u00c2\u00e2\u00ca\u00ea\u00ce\u00ee\u00d4\u00f4\u00db\u00fb\u00c0\u00c1\u00c4\u00c8\u00c9\u00cb\u00cc\u00cd\u00cf\u00d2\u00d3\u00d6\u00d9\u00da\u00dc\u00dd\u00e0\u00e1\u00e4\u00e8\u00e9\u00eb\u00ec\u00ed\u00ef\u00f2\u00f3\u00f6\u00f9\u00fa\u00fc\u00fd\u00ff\u2212\u221e";

    private static final float REFERENCE_HEIGHT = 1080.0F;

    private final EfnGlfw glfw =
            new EfnGlfw() {

                public void keyCallback(long l, int i, int j, int k, int m) {
                    EfnKeys.record(l, i, k);
                    boolean bl = i >= 0 && i < ImGuiManager.this.imguiKeysDown.length;
                    boolean bl2 =
                            k == 0
                                    ? bl && ImGuiManager.this.imguiKeysDown[i]
                                    : ImGuiManager.this.feedsKeyPress(i, m);
                    if (!bl2) {
                        if (ImGuiManager.this.minecraftKeys != null) {
                            ImGuiManager.this.minecraftKeys.invoke(l, i, j, k, m);
                        }
                    } else {
                        if (bl) {
                            ImGuiManager.this.imguiKeysDown[i] = k != 0;
                        }
                        super.keyCallback(l, i, j, k, m);
                    }
                }

                public void monitorCallback(long l2, int n) {
                    super.monitorCallback(l2, n);
                    if (ImGuiManager.this.minecraftMonitors != null) {
                        ImGuiManager.this.minecraftMonitors.invoke(l2, n);
                    }
                }

                public void cursorPosCallback(long l3, double d, double e) {
                    super.cursorPosCallback(l3, d, e);
                    ImGuiManager.feedScaledCursor(d, e);
                }

                protected void updateMouseData() {
                    super.updateMouseData();
                    long handle = MinecraftClient.getInstance().getWindow().getHandle();
                    if (GLFW.glfwGetWindowAttrib(handle, 131073) != 0) {
                        double[] doubles = new double[1];
                        double[] doubles2 = new double[1];
                        GLFW.glfwGetCursorPos(handle, doubles, doubles2);
                        ImGuiManager.feedScaledCursor(doubles[0], doubles2[0]);
                    }
                }
            };

    private final EfnGl3 gl3 = new EfnGl3();

    private final boolean[] imguiKeysDown = new boolean[349];

    private GLFWKeyCallback minecraftKeys;

    private GLFWMonitorCallback minecraftMonitors;

    private boolean initialized;

    private boolean idleSkipped;

    private Runnable activeRenderer;

    private Runnable hudRenderer;

    private BooleanSupplier hudHasWork = () -> true;

    private Runnable toastRenderer;

    private BooleanSupplier toastHasWork = () -> false;

    private Runnable overlayRenderer;

    private Runnable streamRenderer;

    private BooleanSupplier streamHasWork = () -> false;

    private BooleanSupplier overlayHasWork = () -> false;

    private Runnable alertRenderer;

    private BooleanSupplier alertHasWork = () -> false;

    private float uiScale = 1.0F;

    private float userScale = 1.0F;

    private UiFont uiFont = UiFont.GEIST;

    private UiFont hudFont = UiFont.GEIST;

    private int frameCount;

    private float frameDelta;

    private ImFont categoryFont;

    private ImFont moduleFont;

    private ImFont titleFont;

    private ImFont smallFont;

    private ImFont hudValueFont;

    private ImFont hudTextFont;

    private ImFont hudLabelFont;

    private ImFont menuFont;

    private ImFont menuTagFont;

    private ImFont menuBodyFont;

    private ImFont menuNameFont;

    private ImFont menuItemFont;

    private short[] glyphRanges;

    private static void feedScaledCursor(double d, double e) {
        float pixelsPerPointX = pixelsPerPointX();
        float pixelsPerPointY = pixelsPerPointY();
        if (pixelsPerPointX != 1.0F || pixelsPerPointY != 1.0F) {
            ImGui.getIO()
                    .addMousePosEvent((float) (d * pixelsPerPointX), (float) (e * pixelsPerPointY));
        }
    }

    private static float pixelsPerPointX() {
        Window window = MinecraftClient.getInstance().getWindow();
        return window.getWidth() > 0
                ? (float) window.getFramebufferWidth() / window.getWidth()
                : 1.0F;
    }

    private static float pixelsPerPointY() {
        Window window = MinecraftClient.getInstance().getWindow();
        return window.getHeight() > 0
                ? (float) window.getFramebufferHeight() / window.getHeight()
                : 1.0F;
    }

    private ImGuiManager() {}

    public static ImGuiManager getInstance() {
        return INSTANCE;
    }

    public ImFont categoryFont() {
        return this.categoryFont;
    }

    public ImFont moduleFont() {
        return this.moduleFont;
    }

    public ImFont titleFont() {
        return this.titleFont;
    }

    public ImFont smallFont() {
        return this.smallFont;
    }

    public ImFont hudValueFont() {
        return this.hudValueFont;
    }

    public ImFont hudTextFont() {
        return this.hudTextFont;
    }

    public ImFont hudLabelFont() {
        return this.hudLabelFont;
    }

    public ImFont menuFont() {
        return this.menuFont;
    }

    public ImFont menuTagFont() {
        return this.menuTagFont;
    }

    public ImFont menuBodyFont() {
        return this.menuBodyFont;
    }

    public ImFont menuNameFont() {
        return this.menuNameFont;
    }

    public ImFont menuItemFont() {
        return this.menuItemFont;
    }

    public float uiScale() {
        return this.uiScale;
    }

    public int frameCount() {
        return this.frameCount;
    }

    public float frameDelta() {
        return this.frameDelta;
    }

    public void activate(Runnable activeRenderer2) {
        this.activeRenderer = activeRenderer2;
    }

    public boolean typing() {
        return this.initialized && ImGui.getIO().getWantTextInput();
    }

    public void requestCursor(DrawContext context) {
        if (this.initialized) {
            Cursor cursor =
                    switch (ImGui.getMouseCursor()) {
                        case 1 -> StandardCursors.IBEAM;
                        case 2 -> StandardCursors.RESIZE_ALL;
                        case 3 -> StandardCursors.RESIZE_NS;
                        case 4 -> StandardCursors.RESIZE_EW;
                        default -> null;
                        case 7 -> StandardCursors.POINTING_HAND;
                        case 8 -> StandardCursors.NOT_ALLOWED;
                    };
            if (cursor != null) {
                context.setCursor(cursor);
            }
        }
    }

    private boolean feedsKeyPress(int i, int j) {
        return this.activeRenderer != null && (i != 258 || (j & 2) == 0);
    }

    public void deactivate() {
        this.activeRenderer = null;
        String iniFilename;
        if (this.initialized && (iniFilename = ImGui.getIO().getIniFilename()) != null) {
            ImGui.saveIniSettingsToDisk(iniFilename);
        }
    }

    public void setHudRenderer(Runnable hudRenderer2, BooleanSupplier hudHasWork2) {
        this.hudRenderer = hudRenderer2;
        this.hudHasWork = hudHasWork2;
    }

    public void setToastRenderer(Runnable toastRenderer2, BooleanSupplier toastHasWork2) {
        this.toastRenderer = toastRenderer2;
        this.toastHasWork = toastHasWork2;
    }

    public void setStreamRenderer(Runnable streamRenderer2, BooleanSupplier streamHasWork2) {
        this.streamRenderer = streamRenderer2;
        this.streamHasWork = streamHasWork2;
    }

    public void setOverlayRenderer(Runnable overlayRenderer2, BooleanSupplier overlayHasWork2) {
        this.overlayRenderer = overlayRenderer2;
        this.overlayHasWork = overlayHasWork2;
    }

    public void setAlertRenderer(Runnable alertRenderer2, BooleanSupplier alertHasWork2) {
        this.alertRenderer = alertRenderer2;
        this.alertHasWork = alertHasWork2;
    }

    public void renderIfActive() {
        boolean bl = this.hudRenderer != null && this.hudHasWork.getAsBoolean();
        boolean bl2 = this.toastRenderer != null && this.toastHasWork.getAsBoolean();
        boolean bl3 = this.overlayRenderer != null && this.overlayHasWork.getAsBoolean();
        boolean bl4 = this.alertRenderer != null && this.alertHasWork.getAsBoolean();
        boolean bl5 = this.streamRenderer != null && this.streamHasWork.getAsBoolean();
        if (this.activeRenderer == null && !bl && !bl2 && !bl3 && !bl4 && !bl5) {
            if (this.initialized) {
                ImGui.getIO().clearEventsQueue();
                this.idleSkipped = true;
            }
        } else {
            this.initIfNeeded();
            this.rebuildFontsIfNeeded();
            if (this.idleSkipped) {
                ImGuiIO iO = ImGui.getIO();
                iO.clearInputKeys();
                iO.clearInputMouse();
                Arrays.fill(this.imguiKeysDown, false);
                this.idleSkipped = false;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            int framebufferWidth = client.getWindow().getFramebufferWidth();
            int framebufferHeight = client.getWindow().getFramebufferHeight();
            GL30.glBindFramebuffer(36160, 0);
            GL11.glViewport(0, 0, framebufferWidth, framebufferHeight);
            GlStateManager._viewport(0, 0, framebufferWidth, framebufferHeight);
            this.glfw.newFrame();
            this.frameDelta = Math.min(ImGui.getIO().getDeltaTime(), 0.1F);
            ImGui.getIO().setDeltaTime(this.frameDelta);
            this.updateDisplayMetrics();
            this.gl3.newFrame();
            ThemeImages.get().beginFrame();
            ImGui.newFrame();
            this.frameCount++;
            boolean bl6 = false;
            try {
                GuiPalette.refresh();
                if (this.streamRenderer != null) {
                    this.streamRenderer.run();
                }
                if (this.hudRenderer != null) {
                    this.hudRenderer.run();
                }
                if (this.activeRenderer != null) {
                    this.activeRenderer.run();
                }
                if (this.toastRenderer != null) {
                    this.toastRenderer.run();
                }
                if (this.alertRenderer != null) {
                    this.alertRenderer.run();
                }
                if (this.overlayRenderer != null) {
                    this.overlayRenderer.run();
                }
                ImGui.render();
                bl6 = true;
                this.gl3.renderDrawData(ImGui.getDrawData());
            } finally {
                try {
                    if (!bl6) {
                        ImGui.endFrame();
                    }
                } finally {
                    ThemeImages.get().endFrame();
                }
            }
        }
    }

    public void dispose() {
        AndroidSystemPicker.cancel();
        if (this.initialized) {
            this.gl3.shutdown();
            this.glfw.shutdown();
            ImGui.destroyContext();
            this.categoryFont = null;
            this.moduleFont = null;
            this.titleFont = null;
            this.smallFont = null;
            this.hudValueFont = null;
            this.hudTextFont = null;
            this.hudLabelFont = null;
            this.menuFont = null;
            this.menuTagFont = null;
            this.menuBodyFont = null;
            this.menuNameFont = null;
            this.menuItemFont = null;
            this.initialized = false;
        }
    }

    private void initIfNeeded() {
        if (!this.initialized) {
            long handle = MinecraftClient.getInstance().getWindow().getHandle();
            ImGui.createContext();
            ImGuiIO iO = ImGui.getIO();
            iO.setIniFilename(iniPath());
            iO.addConfigFlags(1);
            iO.addConfigFlags(32);
            ImGui.styleColorsDark();
            this.uiScale = this.computeScale(this.uiScale);
            this.uiFont = chosenUiFont();
            this.hudFont = chosenHudFont();
            this.loadFonts(iO);
            this.minecraftKeys = GLFW.glfwSetKeyCallback(handle, null);
            GLFW.glfwSetKeyCallback(handle, this.minecraftKeys);
            this.minecraftMonitors = GLFW.glfwSetMonitorCallback(null);
            GLFW.glfwSetMonitorCallback(this.minecraftMonitors);
            this.glfw.init(handle, true);
            this.gl3.init(null);
            this.initialized = true;
        }
    }

    private void rebuildFontsIfNeeded() {
        float scale = this.computeScale(this.uiScale);
        UiFont uiFont2 = chosenUiFont();
        UiFont uiFont3 = chosenHudFont();
        if (scale != this.uiScale || uiFont2 != this.uiFont || uiFont3 != this.hudFont) {
            this.uiScale = scale;
            this.uiFont = uiFont2;
            this.hudFont = uiFont3;
            ImGuiIO iO = ImGui.getIO();
            iO.getFonts().clear();
            this.loadFonts(iO);
            this.gl3.destroyFontsTexture();
            this.gl3.createFontsTexture();
        }
    }

    private static UiFont chosenUiFont() {
        return UiFont.of(ThemeStore.get().font.get());
    }

    private static UiFont chosenHudFont() {
        return UiFont.of(HudTheme.get().font.get());
    }

    private float computeScale(float f) {
        int framebufferHeight = MinecraftClient.getInstance().getWindow().getFramebufferHeight();
        if (framebufferHeight < 200) {
            return f;
        } else {
            if (!this.initialized || !ImGui.getIO().getMouseDown(0)) {
                this.userScale = ThemeStore.get().uiScale.get();
            }
            float g = Math.max(0.6F, Math.min(3.5F, framebufferHeight / 1080.0F * this.userScale));
            return Math.round(g * 20.0F) / 20.0F;
        }
    }

    private static String iniPath() {
        try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("efn");
            Files.createDirectories(path);
            return path.resolve("gui.ini").toString();
        } catch (RuntimeException | IOException exception) {
            System.err.println("[EFN] GUI layout will not be saved: " + exception.getMessage());
            return null;
        }
    }

    private void updateDisplayMetrics() {
        MinecraftClient client = MinecraftClient.getInstance();
        int i = Math.max(1, client.getWindow().getFramebufferWidth());
        int j = Math.max(1, client.getWindow().getFramebufferHeight());
        ImGuiIO iO = ImGui.getIO();
        iO.setDisplaySize(i, j);
        iO.setDisplayFramebufferScale(1.0F, 1.0F);
    }

    private void loadFonts(ImGuiIO imGuiIO) {
        if (this.glyphRanges == null) {
            this.glyphRanges = buildGlyphRanges(imGuiIO);
        }
        try {
            imGuiIO.getFonts().setFreeTypeRenderer(true);
        } catch (Throwable exception) {
            System.err.println(
                    "[EFN] FreeType unavailable, using stb_truetype: " + exception.getMessage());
        }
        if (!this.buildFonts(imGuiIO, this.uiFont, this.hudFont)) {
            if (this.uiFont != UiFont.GEIST || this.hudFont != UiFont.GEIST) {
                System.err.println(
                        "[EFN] Couldn't build "
                                + this.uiFont.label
                                + " / "
                                + this.hudFont.label
                                + " fonts, using Geist");
                imGuiIO.getFonts().clear();
                if (this.buildFonts(imGuiIO, UiFont.GEIST, UiFont.GEIST)) {
                    return;
                }
            }
            throw new IllegalStateException("ImGui font atlas could not be built");
        }
    }

    private boolean buildFonts(ImGuiIO imGuiIO, UiFont uiFont2, UiFont uiFont3) {
        float uiScale2 = this.uiScale;
        this.moduleFont = this.loadFont(imGuiIO, uiFont2, false, 16.0F * uiScale2);
        this.categoryFont = this.loadFont(imGuiIO, uiFont2, true, 17.0F * uiScale2);
        this.titleFont = this.loadFont(imGuiIO, uiFont2, true, 24.0F * uiScale2);
        this.smallFont = this.loadFont(imGuiIO, uiFont2, false, 13.0F * uiScale2);
        this.hudValueFont = this.loadFont(imGuiIO, uiFont3, true, 18.0F * uiScale2);
        this.hudTextFont = this.loadFont(imGuiIO, uiFont3, true, 17.0F * uiScale2);
        this.hudLabelFont = this.loadFont(imGuiIO, uiFont3, false, 14.0F * uiScale2);
        this.menuFont =
                loadFont(
                        imGuiIO,
                        "/assets/efn/fonts/rajdhani-bold.ttf",
                        24.0F * uiScale2,
                        this.glyphRanges);
        this.menuTagFont =
                loadFont(
                        imGuiIO,
                        "/assets/efn/fonts/rajdhani-bold.ttf",
                        15.0F * uiScale2,
                        this.glyphRanges);
        this.menuBodyFont =
                loadFont(
                        imGuiIO,
                        "/assets/efn/fonts/geist-medium.ttf",
                        18.0F * uiScale2,
                        this.glyphRanges);
        this.menuNameFont =
                loadFont(
                        imGuiIO,
                        "/assets/efn/fonts/rajdhani-bold.ttf",
                        20.0F * uiScale2,
                        this.glyphRanges);
        this.menuItemFont =
                loadFont(
                        imGuiIO,
                        "/assets/efn/fonts/rajdhani-bold.ttf",
                        32.0F * uiScale2,
                        this.glyphRanges);
        if (this.moduleFont == null && this.categoryFont == null) {
            imGuiIO.getFonts().addFontDefault();
        } else if (this.moduleFont != null) {
            imGuiIO.setFontDefault(this.moduleFont);
        }
        return imGuiIO.getFonts().build();
    }

    private static short[] buildGlyphRanges(ImGuiIO imGuiIO) {
        ImFontGlyphRangesBuilder imFontGlyphRangesBuilder = new ImFontGlyphRangesBuilder();
        imFontGlyphRangesBuilder.addRanges(imGuiIO.getFonts().getGlyphRangesDefault());
        imFontGlyphRangesBuilder.addRanges(imGuiIO.getFonts().getGlyphRangesVietnamese());
        imFontGlyphRangesBuilder.addText(
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789\u00c7\u011e\u0130\u00d6\u015e\u00dc\u00e7\u011f\u0131\u00f6\u015f\u00fc\u00c2\u00e2\u00ca\u00ea\u00ce\u00ee\u00d4\u00f4\u00db\u00fb\u00c0\u00c1\u00c4\u00c8\u00c9\u00cb\u00cc\u00cd\u00cf\u00d2\u00d3\u00d6\u00d9\u00da\u00dc\u00dd\u00e0\u00e1\u00e4\u00e8\u00e9\u00eb\u00ec\u00ed\u00ef\u00f2\u00f3\u00f6\u00f9\u00fa\u00fc\u00fd\u00ff\u2212\u221e");
        return imFontGlyphRangesBuilder.buildRanges();
    }

    private ImFont loadFont(ImGuiIO imGuiIO, UiFont uiFont2, boolean bl, float f) {
        float g = uiFont2.size(f);
        float offsetY = uiFont2.offsetY(g);
        ImFont font =
                loadFont(
                        imGuiIO,
                        uiFont2.resource(bl),
                        g,
                        this.glyphRanges,
                        uiFont2.flags(),
                        offsetY,
                        false);
        if (font == null) {
            return uiFont2 == UiFont.GEIST ? null : this.loadFont(imGuiIO, UiFont.GEIST, bl, f);
        } else {
            if (uiFont2 != UiFont.GEIST) {
                loadFont(
                        imGuiIO,
                        UiFont.GEIST.resource(bl),
                        uiFont2.fallbackSize(g),
                        this.glyphRanges,
                        UiFont.GEIST.flags(),
                        offsetY,
                        true);
            }
            return font;
        }
    }

    private static ImFont loadFont(ImGuiIO imGuiIO, String string, float f, short[] shorts) {
        return loadFont(imGuiIO, string, f, shorts, 8, 0.0F, false);
    }

    private static ImFont loadFont(
            ImGuiIO imGuiIO,
            String string,
            float f,
            short[] shorts,
            int fontBuilderFlags,
            float g,
            boolean mergeMode) {
        try {
            ImFont imFont;
            try (InputStream resourceAsStream = ImGuiManager.class.getResourceAsStream(string)) {
                if (resourceAsStream == null) {
                    System.err.println("[EFN] Font resource not found: " + string);
                    return null;
                }
                byte[] bytes = resourceAsStream.readAllBytes();
                ImFontConfig imFontConfig = new ImFontConfig();
                ImFont imFont2;
                try {
                    imFontConfig.setOversampleH(3);
                    imFontConfig.setOversampleV(1);
                    imFontConfig.setPixelSnapH(true);
                    imFontConfig.setFontBuilderFlags(fontBuilderFlags);
                    imFontConfig.setGlyphOffset(0.0F, g);
                    imFontConfig.setMergeMode(mergeMode);
                    imFont2 =
                            imGuiIO.getFonts()
                                    .addFontFromMemoryTTF(
                                            bytes, Math.round(f), imFontConfig, shorts);
                } catch (Throwable exception) {
                    imFontConfig.destroy();
                    throw exception;
                }
                imFontConfig.destroy();
                imFont = imFont2;
            }
            return imFont;
        } catch (RuntimeException | IOException exception2) {
            System.err.println(
                    "[EFN] Failed to load font " + string + ": " + exception2.getMessage());
            return null;
        }
    }

    static {
        EfnAndroidNative.prepare();
    }
}
