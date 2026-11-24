package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EfnScene {

    private static final float SLANT = 0.25F;

    private static final int BEAMS = 12;

    private static final int BEAM_SEGMENTS = 10;

    private static final int MOTES = 90;

    private EfnScene() {}

    public static int rgba(int i, int j, int k, float f) {
        return Math.round(Math.max(0.0F, Math.min(1.0F, f)) * 255.0F) << 24 | k << 16 | j << 8 | i;
    }

    public static int mix(int i, int j, float f) {
        f = Math.max(0.0F, Math.min(1.0F, f));
        int k = Math.round((i & 0xFF) + ((j & 0xFF) - (i & 0xFF)) * f);
        int m = Math.round((i >> 8 & 0xFF) + ((j >> 8 & 0xFF) - (i >> 8 & 0xFF)) * f);
        int n = Math.round((i >> 16 & 0xFF) + ((j >> 16 & 0xFF) - (i >> 16 & 0xFF)) * f);
        return 0xFF000000 | n << 16 | m << 8 | k;
    }

    public static float rnd(int i, int j) {
        int k = i * 374761393 + j * 668265263;
        k = (k ^ k >>> 13) * 1274126177;
        k ^= k >>> 16;
        return (k & 16777215) / 1.6777216E7F;
    }

    private static float frac(float f) {
        return f - (float) Math.floor(f);
    }

    public static void glow(ImDrawList imDrawList, float f, float g, float h, int i, float f2) {
        byte b = 28;
        for (int k = 0; k < b; k++) {
            float g2 = (float) k / b;
            float h2 = h * (1.0F - g2);
            float f3 = f2 / b * (0.5F + 1.6F * g2);
            imDrawList.addCircleFilled(f, g, h2, EfnLogo.alpha(i, f3), 48);
        }
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
        int rgba = rgba(7, 7, 10, f3);
        int rgba2 = rgba(12, 14, 19, f3);
        imDrawList.addRectFilledMultiColor(0.0F, 0.0F, f, g, rgba, rgba, rgba2, rgba2);
        int j = mix(i, -41923, 0.55F);
        float g3 = Math.max(f, g);
        glow(
                imDrawList,
                f * (0.24F + 0.05F * (float) Math.sin(f2 * 0.11F)) - g2 * 26.0F * h,
                g * (0.3F + 0.05F * (float) Math.cos(f2 * 0.13F)) - h2 * 14.0F * h,
                g3 * 0.55F,
                i,
                0.1F * f3);
        glow(
                imDrawList,
                f * (0.8F + 0.04F * (float) Math.cos(f2 * 0.09F)) - g2 * 34.0F * h,
                g * (0.74F + 0.05F * (float) Math.sin(f2 * 0.1F)) - h2 * 18.0F * h,
                g3 * 0.5F,
                j,
                0.09F * f3);
        ghost(imDrawList, f, g, h, f2, g2, h2, f3);
        beams(imDrawList, f, g, h, f2, g2, i, f3);
        streak(imDrawList, f, g, h, f2, i, f3);
        motes(imDrawList, f, g, h, f2, g2, h2, i, f3);
        vignette(imDrawList, f, g, f3);
    }

    private static void ghost(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            float f3) {
        float g3 = g * 1.25F;
        float h3 = f * 0.66F - g2 * 30.0F * h;
        float f4 = g * 0.5F - g3 * 0.5F + (float) Math.sin(f2 * 0.2F) * 10.0F * h - h2 * 16.0F * h;
        EfnLogo.mark(imDrawList, h3, f4, g3, rgba(255, 255, 255, 0.04F * f3));
    }

    private static void beams(
            ImDrawList imDrawList, float f, float g, float h, float f2, float g2, int i, float h2) {
        for (int m = 0; m < 12; m++) {
            float f3 = 0.4F + 0.6F * rnd(m, 1);
            float g3 = 0.004F + 0.01F * rnd(m, 2);
            float h3 = (24.0F + 90.0F * rnd(m, 3)) * h * f3;
            float f4 = (frac(rnd(m, 4) + f2 * g3) * 1.5F - 0.25F) * f - g2 * 40.0F * h * f3;
            float g4 =
                    0.55F
                            + 0.45F
                                    * (float)
                                            Math.sin(
                                                    f2 * (0.25F + 0.4F * rnd(m, 5))
                                                            + rnd(m, 6) * 6.28F);
            float h4 = (0.045F + 0.075F * rnd(m, 7)) * g4 * h2;
            int j = m % 3 == 0 ? i : -1;
            for (int o = 0; o < 10; o++) {
                float f5 = g * o / 10.0F;
                float g5 = g * (o + 1) / 10.0F + 1.0F;
                float h5 = (float) Math.sin(3.141592653589793 * (o + 0.5) / 10.0);
                int alpha = EfnLogo.alpha(j, h4 * h5 * h5);
                float f6 = f4 + (g - f5) * 0.25F;
                float g6 = f4 + (g - g5) * 0.25F;
                imDrawList.addQuadFilled(f6, f5, f6 + h3, f5, g6 + h3, g5, g6, g5, alpha);
            }
        }
    }

    static void streak(
            ImDrawList imDrawList, float f, float g, float h, float f2, int i, float g2) {
        float h2 = 7.0F;
        float f3 = f2 % h2 / h2 * 2.0F;
        if (!(f3 > 1.0F)) {
            float g3 = f3 * f3 * (3.0F - 2.0F * f3);
            float h3 = -0.3F * f + g3 * 1.6F * f;
            float f4 = (float) Math.sin(3.141592653589793 * f3);
            for (int m = 0; m < 2; m++) {
                float g4 = m == 0 ? 3.0F * h : 46.0F * h;
                float h4 = (m == 0 ? 0.55F : 0.07F) * f4 * g2;
                for (int n = 0; n < 10; n++) {
                    float f5 = g * n / 10.0F;
                    float g5 = g * (n + 1) / 10.0F + 1.0F;
                    float h5 = (float) Math.sin(3.141592653589793 * (n + 0.5) / 10.0);
                    int alpha = EfnLogo.alpha(i, h4 * h5);
                    float f6 = h3 + (g - f5) * 0.25F;
                    float g6 = h3 + (g - g5) * 0.25F;
                    imDrawList.addQuadFilled(f6, f5, f6 + g4, f5, g6 + g4, g5, g6, g5, alpha);
                }
            }
        }
    }

    static void motes(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            int i,
            float f3) {
        for (int o = 0; o < 90; o++) {
            float g3 = 0.3F + 0.7F * rnd(o, 11);
            float h3 = frac(rnd(o, 12) + f2 * 0.004F * g3) * f - g2 * 24.0F * h * g3;
            float f4 = frac(rnd(o, 13) - f2 * 0.01F * g3) * g - h2 * 12.0F * h * g3;
            float g4 =
                    0.5F
                            + 0.5F
                                    * (float)
                                            Math.sin(
                                                    f2 * (0.6F + rnd(o, 14) * 1.6F)
                                                            + rnd(o, 15) * 6.28F);
            float h4 = (0.7F + 1.5F * g3) * h;
            int j = o % 4 == 0 ? i : -1;
            imDrawList.addCircleFilled(
                    h3, f4, h4, EfnLogo.alpha(j, (0.1F + 0.5F * g3 * g4) * f3), 8);
        }
    }

    static void vignette(ImDrawList imDrawList, float f, float g, float h) {
        int rgba = rgba(4, 4, 6, 0.62F * h);
        int rgba2 = rgba(4, 4, 6, 0.0F);
        float f2 = f * 0.24F;
        float g2 = g * 0.3F;
        imDrawList.addRectFilledMultiColor(0.0F, 0.0F, f2, g, rgba, rgba2, rgba2, rgba);
        imDrawList.addRectFilledMultiColor(f - f2, 0.0F, f, g, rgba2, rgba, rgba, rgba2);
        imDrawList.addRectFilledMultiColor(0.0F, g - g2, f, g, rgba2, rgba2, rgba, rgba);
        imDrawList.addRectFilledMultiColor(0.0F, 0.0F, f, g * 0.18F, rgba, rgba, rgba2, rgba2);
    }
}
