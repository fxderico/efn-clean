package net.efn.client.gui;

import imgui.ImDrawList;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class HudGlyphs {

    private static final float TAU = 6.2831855F;

    private HudGlyphs() {}

    static int ink(int i, float f) {
        return HudDraw.col(i, f);
    }

    static int vitality(float f) {
        float g = Math.max(0.0F, Math.min(1.0F, f));
        return g >= 0.55F
                ? HudDraw.mix(-15408912, -12600956, (g - 0.55F) / 0.45F)
                : HudDraw.mix(-2537681, -15408912, g / 0.55F);
    }

    static float approach(float f, float g, float h, float f2) {
        float g2 = f + (g - f) * (1.0F - (float) Math.exp(-h * f2));
        return Math.abs(g2 - g) < 5.0E-4F ? g : g2;
    }

    static float easeOutQuint(float f) {
        float g = 1.0F - Math.max(0.0F, Math.min(1.0F, f));
        return 1.0F - g * g * g * g * g;
    }

    static void register(ImDrawList imDrawList, float f, float g, float h, float f2, int i) {
        if (!(g - f < 2.0F)) {
            float g2 = (f + g) * 0.5F;
            int j = i & 16777215;
            imDrawList.addRectFilledMultiColor(f, h, g2, h + f2, j, i, i, j);
            imDrawList.addRectFilledMultiColor(g2, h, g, h + f2, i, j, j, i);
        }
    }

    static void arc(
            ImDrawList imDrawList, float f, float g, float h, float f2, float g2, float h2, int i) {
        if (!(Math.abs(g2) < 0.002F) && (i >>> 24 & 0xFF) != 0) {
            int j = Math.max(6, Math.round(Math.abs(g2) / 6.2831855F * 40.0F));
            imDrawList.pathClear();
            imDrawList.pathArcTo(f, g, h, f2, f2 + g2, j);
            imDrawList.pathStroke(i, 0, h2);
        }
    }

    static void bolt(ImDrawList imDrawList, float f, float g, float h, float f2, int i) {
        imDrawList.addTriangleFilled(
                f + h * 0.18F,
                g - h * 0.5F,
                f - h * 0.26F,
                g + h * 0.06F,
                f + h * 0.08F,
                g + h * 0.06F,
                i);
        imDrawList.addTriangleFilled(
                f - h * 0.08F,
                g - h * 0.06F,
                f + h * 0.26F,
                g - h * 0.06F,
                f - h * 0.18F,
                g + h * 0.5F,
                i);
    }

    static void sparkle(ImDrawList imDrawList, float f, float g, float h, int i) {
        float f2 = h * 0.3F;
        imDrawList.addQuadFilled(f, g - h, f + f2, g, f, g + h, f - f2, g, i);
        imDrawList.addQuadFilled(f - h, g, f, g - f2, f + h, g, f, g + f2, i);
    }

    static void hourglass(ImDrawList imDrawList, float f, float g, float h, float f2, int i) {
        float g2 = h * 0.5F;
        float h2 = h * 0.36F;
        imDrawList.addLine(f - h2, g - g2, f + h2, g - g2, i, f2);
        imDrawList.addLine(f - h2, g + g2, f + h2, g + g2, i, f2);
        imDrawList.addTriangleFilled(f - h2 * 0.8F, g - g2, f + h2 * 0.8F, g - g2, f, g, i);
        imDrawList.addTriangle(f - h2 * 0.8F, g + g2, f + h2 * 0.8F, g + g2, f, g, i, f2);
    }

    static void reticle(ImDrawList imDrawList, float f, float g, float h, float f2, int i) {
        byte b = 28;
        float g2 = h * 0.82F;
        for (int k = 0; k < b; k++) {
            double d = 6.283185307179586 * k / b;
            double e = 6.283185307179586 * (k + 1) / b;
            imDrawList.addLine(
                    f + g2 * (float) Math.cos(d),
                    g + g2 * (float) Math.sin(d),
                    f + g2 * (float) Math.cos(e),
                    g + g2 * (float) Math.sin(e),
                    i,
                    f2);
        }
        imDrawList.addCircleFilled(f, g, h * 0.16F, i, 12);
        imDrawList.addLine(f - h * 1.05F, g, f - h * 0.5F, g, i, f2);
        imDrawList.addLine(f + h * 0.5F, g, f + h * 1.05F, g, i, f2);
        imDrawList.addLine(f, g - h * 1.05F, f, g - h * 0.5F, i, f2);
        imDrawList.addLine(f, g + h * 0.5F, f, g + h * 1.05F, i, f2);
    }
}
