package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EfnScape {

    private static final int LAYERS = 5;

    private static final int RAYS = 15;

    private static final int RAY_SEGMENTS = 9;

    private static final int STARS = 70;

    private EfnScape() {}

    private static float clamp01(float f) {
        return Math.max(0.0F, Math.min(1.0F, f));
    }

    private static float smooth(float f) {
        f = clamp01(f);
        return f * f * (3.0F - 2.0F * f);
    }

    public static void draw(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            int i,
            float f3) {
        float g3 = g * 0.64F;
        int rgba = EfnScene.rgba(5, 7, 12, f3);
        int rgba2 = EfnScene.rgba(8, 20, 30, f3);
        int rgba3 = EfnScene.rgba(5, 7, 10, f3);
        imDrawList.addRectFilledMultiColor(0.0F, 0.0F, f, g3, rgba, rgba, rgba2, rgba2);
        imDrawList.addRectFilled(0.0F, g3, f, g, rgba3);
        float h3 = f * 0.5F - g2 * 18.0F * h;
        float f4 = g3 - g * 0.1F - h2 * 8.0F * h;
        EfnScene.glow(imDrawList, h3, f4, g * 1.05F, i, 0.2F * f3);
        EfnScene.glow(imDrawList, h3, f4, g * 0.38F, EfnScene.mix(i, -1, 0.5F), 0.22F * f3);
        stars(imDrawList, f, g3, h, f2, g2, f3);
        rays(imDrawList, h3, f4, g, f2, i, f3);
        for (int r = 0; r < 5; r++) {
            ridge(imDrawList, f, g, h, f2, g2, g3, r, i, f3);
        }
        EfnScene.streak(imDrawList, f, g, h, f2, i, f3);
        EfnScene.motes(imDrawList, f, g, h, f2, g2, h2, i, f3 * 0.8F);
        EfnScene.vignette(imDrawList, f, g, f3);
    }

    private static void stars(
            ImDrawList imDrawList, float f, float g, float h, float f2, float g2, float h2) {
        for (int m = 0; m < 70; m++) {
            float f3 = EfnScene.rnd(m, 21) * f - g2 * 6.0F * h;
            float g3 = EfnScene.rnd(m, 22) * g * 0.8F;
            float h3 =
                    0.4F
                            + 0.6F
                                    * (float)
                                            Math.sin(
                                                    f2 * (0.5F + EfnScene.rnd(m, 23) * 1.5F)
                                                            + EfnScene.rnd(m, 24) * 6.28F);
            float f4 = (0.6F + 0.9F * EfnScene.rnd(m, 25)) * h;
            imDrawList.addCircleFilled(
                    f3,
                    g3,
                    f4,
                    EfnScene.rgba(220, 240, 255, (0.12F + 0.55F * h3 * EfnScene.rnd(m, 26)) * h2),
                    6);
        }
    }

    private static void rays(
            ImDrawList imDrawList, float f, float g, float h, float f2, int i, float g2) {
        float h2 = h * 1.5F;
        for (int m = 0; m < 15; m++) {
            float f3 = (m / 14.0F - 0.5F) * 3.1415927F * 0.95F;
            float g3 = -1.5707964F + f3 + (float) Math.sin(f2 * 0.07F + m) * 0.025F;
            float h3 = 0.01F + 0.03F * EfnScene.rnd(m, 31);
            float f4 =
                    0.5F
                            + 0.5F
                                    * (float)
                                            Math.sin(
                                                    f2 * (0.2F + 0.3F * EfnScene.rnd(m, 32))
                                                            + EfnScene.rnd(m, 33) * 6.28F);
            float g4 = (0.035F + 0.065F * EfnScene.rnd(m, 34)) * (0.4F + 0.6F * f4) * g2;
            for (int n = 0; n < 9; n++) {
                float h4 = h2 * n / 9.0F;
                float f5 = h2 * (n + 1) / 9.0F;
                float g5 = 1.0F - n / 9.0F;
                int alpha = EfnLogo.alpha(i, g4 * g5 * g5);
                float h5 = h3 * (0.3F + 0.7F * n / 9.0F);
                float f6 = h3 * (0.3F + 0.7F * (n + 1) / 9.0F);
                float g6 = f + (float) Math.cos(g3 - h5) * h4;
                float h6 = g + (float) Math.sin(g3 - h5) * h4;
                float f7 = f + (float) Math.cos(g3 + h5) * h4;
                float g7 = g + (float) Math.sin(g3 + h5) * h4;
                float h7 = f + (float) Math.cos(g3 + f6) * f5;
                float f8 = g + (float) Math.sin(g3 + f6) * f5;
                float g8 = f + (float) Math.cos(g3 - f6) * f5;
                float h8 = g + (float) Math.sin(g3 - f6) * f5;
                imDrawList.addQuadFilled(g6, h6, f7, g7, h7, f8, g8, h8, alpha);
            }
        }
    }

    private static void ridge(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            int i,
            int j,
            float f3) {
        float g3 = (i + 1) / 5.0F;
        float h3 = h2 + (float) Math.pow(g3, 1.6) * g * 0.2F - g * 0.05F;
        float f4 = g * (0.58F - 0.07F * i);
        float g4 = (24.0F + 52.0F * g3) * h;
        float h4 = g4 * 0.85F;
        float f5 = f2 * (2.0F + 9.0F * g3) * h;
        float h5 = -g2 * (10.0F + 46.0F * g3) * h;
        int rgba = EfnScene.rgba(26, 74, 92, 1.0F);
        int rgba2 = EfnScene.rgba(5, 7, 10, 1.0F);
        int alpha = EfnLogo.alpha(EfnScene.mix(rgba, rgba2, (float) Math.pow(g3, 0.8)), f3);
        int alpha2 = EfnLogo.alpha(j, (0.55F - 0.32F * g3) * f3);
        float f6 = f * 0.5F;
        int k = (int) Math.floor((-g4 - h5 + f5) / g4);
        float g6 = k * g4 - f5 + h5;
        for (float g5 = g6; g5 < f + g4; g5 += g4) {
            int m = Math.round((g5 - g6) / g4) + k;
            float h6 =
                    0.6F * (float) Math.sin(m * 0.045F + i * 1.7F)
                            + 0.3F * (float) Math.sin(m * 0.11F + i * 3.1F)
                            + 0.18F * (EfnScene.rnd(m, 40 + i) - 0.5F);
            float f7 = clamp01(0.55F + 0.7F * h6);
            float g7 = g5 + g4 * 0.5F;
            float h7 = 0.1F + 0.9F * smooth((Math.abs(g7 - f6) - f6 * 0.12F) / (f6 * 0.7F));
            float f8 = Math.round(f4 * f7 * h7 / h4) * h4;
            if (f8 < h4) {
                f8 = h4;
            }
            float g8 = h3 - f8;
            imDrawList.addRectFilled(g5, g8, g5 + g4 + 1.0F, g, alpha);
            imDrawList.addRectFilled(g5, g8, g5 + g4 + 1.0F, g8 + Math.max(1.5F, 2.0F * h), alpha2);
        }
        int rgba3 = EfnScene.rgba(14, 44, 56, 0.0F);
        int rgba4 = EfnScene.rgba(14, 44, 56, (0.3F - 0.18F * g3) * f3);
        imDrawList.addRectFilledMultiColor(
                0.0F, h3 - f4 * 0.55F, f, h3 + 6.0F * h, rgba3, rgba3, rgba4, rgba4);
    }
}
