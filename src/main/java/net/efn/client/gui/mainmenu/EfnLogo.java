package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EfnLogo {

    private static final float[][] BARS =
            new float[][] {
                {14.0F, 12.0F, 50.0F, 22.0F},
                {14.0F, 22.0F, 27.0F, 42.0F},
                {27.0F, 27.0F, 44.0F, 37.0F},
                {14.0F, 42.0F, 50.0F, 52.0F}
            };

    private static final int TILE = -14410470;

    private EfnLogo() {}

    public static int alpha(int i, float f) {
        int j = Math.round((i >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, f)));
        return j << 24 | i & 16777215;
    }

    public static void tile(ImDrawList imDrawList, float f, float g, float h, int i, float f2) {
        float g2 = h / 64.0F;
        imDrawList.addRectFilled(f, g, f + h, g + h, alpha(-14410470, f2), 15.0F * g2);
        imDrawList.addRect(
                f, g, f + h, g + h, alpha(-1, 0.07F * f2), 15.0F * g2, 0, Math.max(1.0F, g2));
        mark(imDrawList, f, g, h, alpha(i, f2));
    }

    public static void tileFilling(
            ImDrawList imDrawList, float f, float g, float h, int i, float f2, float g2) {
        float h2 = h / 64.0F;
        imDrawList.addRectFilled(f, g, f + h, g + h, alpha(-14410470, g2), 15.0F * h2);
        imDrawList.addRect(
                f, g, f + h, g + h, alpha(-1, 0.07F * g2), 15.0F * h2, 0, Math.max(1.0F, h2));
        mark(imDrawList, f, g, h, alpha(i, 0.16F * g2));
        float f3 = g + h * (1.0F - Math.max(0.0F, Math.min(1.0F, f2)));
        imDrawList.pushClipRect(f - h, f3, f + h * 2.0F, g + h * 2.0F, true);
        mark(imDrawList, f, g, h, alpha(i, g2));
        imDrawList.popClipRect();
    }

    public static void mark(ImDrawList imDrawList, float f, float g, float h, int i) {
        float f2 = h / 64.0F;
        for (float[] floats : BARS) {
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
            imDrawList.addQuadFilled(h3, f4, g4, f4, h4, f5, g5, f5, i);
        }
    }
}
