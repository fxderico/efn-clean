package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;

import net.efn.client.imgui.ImGuiManager;
import net.efn.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class WorldLabel {

    private static final int PRIMARY = -1;

    private static final int SECONDARY = -1315861;

    private WorldLabel() {}

    public static float[] project(MinecraftClient client, Vec3d vec, float f, float g) {
        Camera camera = client.gameRenderer.getCamera();
        if (camera != null && camera.isReady() && TracerEngine.isInFront(camera, vec)) {
            Vec3d vec2 = client.gameRenderer.project(vec);
            if (Double.isFinite(vec2.x) && Double.isFinite(vec2.y)) {
                float h = (float) (vec2.x + 1.0) * 0.5F * f;
                float f2 = (float) (1.0 - vec2.y) * 0.5F * g;
                return !(h < -200.0F) && !(h > f + 200.0F) && !(f2 < -100.0F) && !(f2 > g + 100.0F)
                        ? new float[] {h, f2}
                        : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static void drawText(
            ImDrawList imDrawList, float f, float g, String string, String string2, float h) {
        ImGuiManager imGuiManager = ImGuiManager.getInstance();
        ImFont imFont = imGuiManager.hudTextFont();
        ImFont imFont2 = imGuiManager.hudLabelFont();
        int fontPx = fontPx(imFont);
        int fontPx2 = fontPx(imFont2);
        float f2 = 5.0F;
        float g2 = 6.0F;
        float width = HudChrome.width(imFont, fontPx, string);
        float h2 = string2 == null ? 0.0F : HudChrome.width(imFont2, fontPx2, string2);
        float f3 = Math.round(f - (width + (string2 == null ? 0.0F : g2 + h2)) * 0.5F);
        float g3 = Math.round(g - f2 - fontPx);
        HudChrome.text(imDrawList, imFont, fontPx, f3, g3, fade(-1, h), string, true);
        if (string2 != null) {
            HudChrome.text(
                    imDrawList,
                    imFont2,
                    fontPx2,
                    Math.round(f3 + width + g2),
                    g3 + HudChrome.baselineOffset(imFont, fontPx, imFont2, fontPx2),
                    fade(-1315861, h),
                    string2,
                    true);
        }
    }

    private static int fontPx(ImFont imFont) {
        return imFont != null ? Math.round(imFont.getFontSize()) : 13;
    }

    private static int fade(int i, float f) {
        int j = Math.round((i >>> 24) * Math.max(0.0F, Math.min(1.0F, f)));
        return j << 24 | i & 16777215;
    }
}
