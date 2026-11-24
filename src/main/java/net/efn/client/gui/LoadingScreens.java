package net.efn.client.gui;

import com.example.client.mixin.AccessorLevelLoadingScreen;
import com.example.client.mixin.AccessorLoadingOverlay;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;

import net.efn.client.gui.mainmenu.EfnLogo;
import net.efn.client.gui.mainmenu.EfnScene;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.util.Util;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class LoadingScreens {

    private static final String HEAD_RESOURCE = "/assets/efn/textures/gui/head.png";

    private static final int BACKGROUND_RGB = 722950;

    private static final float FADE_OUT_SECONDS = 0.45F;

    private static final long ORIGIN = System.nanoTime();

    private static long lastActiveAt = -1L;

    private static float lastProgress;

    private static float lastAlpha;

    private static String lastStatus = "";

    private static float shownFill;

    private LoadingScreens() {}

    public static int backgroundColor(int i) {
        return !EfnTitleScreen.enabled() ? i : i & 0xFF000000 | 526346;
    }

    public static boolean hasWork() {
        if (!EfnTitleScreen.enabled()) {
            return false;
        } else if (lastActiveAt >= 0L) {
            return true;
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            return client.getOverlay() instanceof SplashOverlay
                    || client.currentScreen instanceof LevelLoadingScreen
                    || client.currentScreen instanceof MessageScreen
                    || client.currentScreen instanceof ProgressScreen;
        }
    }

    public static void render() {
        if (EfnTitleScreen.enabled()) {
            MinecraftClient client = MinecraftClient.getInstance();
            long l = System.nanoTime();
            String string;
            float efn$getProgress;
            float reloadAlpha;
            if (client.getOverlay() instanceof SplashOverlay splashOverlay) {
                AccessorLoadingOverlay accessorLoadingOverlay =
                        (AccessorLoadingOverlay) splashOverlay;
                reloadAlpha = reloadAlpha(accessorLoadingOverlay);
                efn$getProgress = accessorLoadingOverlay.efn$getProgress();
                string = "Loading";
            } else {
                Screen currentScreen = client.currentScreen;
                if (currentScreen instanceof LevelLoadingScreen levelLoadingScreen) {
                    efn$getProgress =
                            ((AccessorLevelLoadingScreen) levelLoadingScreen).efn$getProgress();
                    string = "Entering world";
                } else {
                    if (!(currentScreen instanceof MessageScreen)
                            && !(currentScreen instanceof ProgressScreen)) {
                        drawFadingOut(l);
                        return;
                    }
                    efn$getProgress = -1.0F;
                    String string2 = currentScreen.getTitle().getString();
                    string = string2.isBlank() ? "Loading" : string2;
                }
                reloadAlpha = 1.0F;
            }
            lastActiveAt = l;
            lastProgress = efn$getProgress;
            lastStatus = string;
            lastAlpha = reloadAlpha;
            draw(reloadAlpha, efn$getProgress, string, l);
        }
    }

    private static void drawFadingOut(long l) {
        if (lastActiveAt >= 0L) {
            float seconds = seconds(l - lastActiveAt);
            if (seconds >= 0.45F) {
                lastActiveAt = -1L;
            } else {
                draw(
                        lastAlpha * (1.0F - seconds / 0.45F),
                        lastProgress < 0.0F ? -1.0F : 1.0F,
                        lastStatus,
                        l);
            }
        }
    }

    private static float reloadAlpha(AccessorLoadingOverlay accessorLoadingOverlay) {
        long measuringTimeMs = Util.getMeasuringTimeMs();
        long efn$getFadeOutStart = accessorLoadingOverlay.efn$getFadeOutStart();
        long efn$getFadeInStart = accessorLoadingOverlay.efn$getFadeInStart();
        float f =
                efn$getFadeOutStart > -1L
                        ? (float) (measuringTimeMs - efn$getFadeOutStart) / 1000.0F
                        : -1.0F;
        float g =
                efn$getFadeInStart > -1L
                        ? (float) (measuringTimeMs - efn$getFadeInStart) / 500.0F
                        : -1.0F;
        if (f >= 1.0F) {
            return 1.0F - clamp01(f - 1.0F);
        } else {
            return accessorLoadingOverlay.efn$isFadeIn() ? clamp01(g) : 1.0F;
        }
    }

    private static void draw(float f, float g, String string, long l) {
        if (!(f <= 0.001F)) {
            ImGuiIO iO = ImGui.getIO();
            float displaySizeX = iO.getDisplaySizeX();
            float displaySizeY = iO.getDisplaySizeY();
            float uiScale = ImGuiManager.getInstance().uiScale();
            float seconds = seconds(l - ORIGIN);
            float h = Math.max(0.001F, iO.getDeltaTime());
            ImDrawList foregroundDrawList = ImGui.getForegroundDrawList();
            int accent = GuiPalette.accent();
            EfnScene.draw(
                    foregroundDrawList,
                    displaySizeX,
                    displaySizeY,
                    uiScale,
                    seconds,
                    0.0F,
                    0.0F,
                    accent,
                    f);
            float f2 = g >= 0.0F ? clamp01(g) : (float) (seconds * 0.45 % 1.0);
            shownFill =
                    !(g < 0.0F) && !(f2 < shownFill)
                            ? (shownFill = shownFill + (f2 - shownFill) * Math.min(1.0F, h * 6.0F))
                            : f2;
            float g2 = displaySizeX * 0.5F;
            float h2 = Math.min(displaySizeY * 0.2F, 132.0F * uiScale);
            float f3 = displaySizeY * 0.5F - h2 * 0.75F;
            EfnScene.glow(foregroundDrawList, g2, f3 + h2 * 0.5F, h2 * 2.4F, accent, 0.14F * f);
            EfnLogo.tileFilling(foregroundDrawList, g2 - h2 * 0.5F, f3, h2, accent, shownFill, f);
            ImFont imFont = ImGuiManager.getInstance().titleFont();
            ImFont imFont2 = ImGuiManager.getInstance().smallFont();
            float g3 = 14.0F * uiScale;
            String string2 = "EFN";
            float h3 = HudDraw.spacedWidth(imFont, string2, g3) - g3;
            float f4 = f3 + h2 + 26.0F * uiScale;
            HudDraw.drawSpaced(
                    foregroundDrawList, imFont, g2 + 0.0F * h3, f4, string2, g3, -1250320, f, true);
            float g4 = imFont != null ? imFont.getFontSize() : 24.0F * uiScale;
            float h4 = 280.0F * uiScale;
            float f5 = Math.max(3.0F, 3.0F * uiScale);
            float g5 = f4 + g4 + 26.0F * uiScale;
            float h5 = g2 - h4 * 0.5F;
            foregroundDrawList.addRectFilled(
                    h5, g5, h5 + h4, g5 + f5, EfnLogo.alpha(-1, 0.08F * f), f5);
            float f6;
            float g6;
            if (g >= 0.0F) {
                g6 = h5;
                f6 = h5 + h4 * shownFill;
            } else {
                float h6 = (float) (seconds * 0.6 % 1.0);
                g6 = h5 + (h4 + h4 * 0.3F) * h6 - h4 * 0.3F;
                f6 = g6 + h4 * 0.3F;
                g6 = Math.max(h5, g6);
                f6 = Math.min(h5 + h4, f6);
            }
            if (f6 > g6) {
                foregroundDrawList.addQuadFilled(
                        g6,
                        g5,
                        f6 + f5,
                        g5,
                        f6,
                        g5 + f5,
                        g6 - f5,
                        g5 + f5,
                        EfnLogo.alpha(accent, f));
            }
            String string3 =
                    g >= 0.0F
                            ? string + "  \u00b7  " + Math.round(clamp01(g) * 100.0F) + "%"
                            : string;
            float f7 = 3.0F * uiScale;
            String string4 = string3.toUpperCase(Locale.ROOT);
            float g7 = HudDraw.spacedWidth(imFont2, string4, f7) - f7;
            HudDraw.drawSpaced(
                    foregroundDrawList,
                    imFont2,
                    g2,
                    g5 + 22.0F * uiScale,
                    string4,
                    f7,
                    -7698026,
                    f,
                    false);
        }
    }

    private static float seconds(long l) {
        return (float) l / 1.0E9F;
    }

    private static float clamp01(float f) {
        return Math.max(0.0F, Math.min(1.0F, f));
    }
}
