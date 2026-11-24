package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class HudDraw {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    public static final int GOLD = -15408912;

    public static final int GOLD_LIGHT = -6621958;

    public static final int GOLD_TEXT = -6557705;

    public static final int LACQUER = -16381684;

    public static final int TAN = -3812390;

    public static final int TAN_PALE = -1839888;

    public static final int TAN_DARK = -6772301;

    public static final int DEEP = -13738055;

    public static final int DEEP_LIGHT = -12750874;

    public static final int DEEP_DEEP = -14994064;

    public static final int TURQUOISE = -12794702;

    public static final int MALACHITE = -12600956;

    public static final int CARNELIAN = -2537681;

    public static final int TEXT = -1575946;

    public static final int MUTED = -7693914;

    public static final int BLACK = -16777216;

    private static final Map<String, ThemeImages.Texture> TEXTURES = new HashMap<>();

    private HudDraw() {}

    public static int col(int i, float f) {
        int j = Math.round((i >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, f)));
        return GuiPalette.abgr(j << 24 | i & 16777215);
    }

    public static int mix(int i, int j, float f) {
        f = Math.max(0.0F, Math.min(1.0F, f));
        int k = Math.round((i >>> 24 & 0xFF) + ((j >>> 24 & 0xFF) - (i >>> 24 & 0xFF)) * f);
        int m = Math.round((i >> 16 & 0xFF) + ((j >> 16 & 0xFF) - (i >> 16 & 0xFF)) * f);
        int n = Math.round((i >> 8 & 0xFF) + ((j >> 8 & 0xFF) - (i >> 8 & 0xFF)) * f);
        int o = Math.round((i & 0xFF) + ((j & 0xFF) - (i & 0xFF)) * f);
        return k << 24 | m << 16 | n << 8 | o;
    }

    public static void glow(ImDrawList imDrawList, float f, float g, float h, int i, float f2) {
        byte b = 18;
        for (int k = 0; k < b; k++) {
            float g2 = 1.0F - (float) k / b;
            imDrawList.addCircleFilled(f, g, h * g2, col(i, f2 / b * 2.2F), 48);
        }
    }

    public static void sunDisk(
            ImDrawList imDrawList, float f, float g, float h, float f2, float g2, float h2) {
        glow(imDrawList, f, g, h * 1.9F, -1466054, 0.16F * h2);
        imDrawList.addCircleFilled(f, g, h, col(-14018806, 0.55F * h2), 96);
        imDrawList.addCircle(f, g, h, col(-15408912, 0.55F * h2), 96, Math.max(1.0F, 1.4F * g2));
        imDrawList.addCircle(f, g, h * 0.93F, col(-15408912, 0.22F * h2), 96, Math.max(1.0F, g2));
        byte b = 48;
        for (int m = 0; m < b; m++) {
            float f3 = f2 + (float) (m * 3.141592653589793 * 2.0 / b);
            float g3 = h * 1.07F;
            float h3 = h * (m % 2 == 0 ? 1.34F : 1.2F);
            float f4 = (float) Math.cos(f3);
            float g4 = (float) Math.sin(f3);
            imDrawList.addLine(
                    f + f4 * g3,
                    g + g4 * g3,
                    f + f4 * h3,
                    g + g4 * h3,
                    col(-15408912, (m % 2 == 0 ? 0.28F : 0.16F) * h2),
                    Math.max(1.0F, 1.3F * g2));
        }
    }

    public static void text(
            ImDrawList imDrawList, ImFont imFont, float f, float g, String string, int i) {
        if (imFont != null) {
            imDrawList.addText(imFont, Math.round(imFont.getFontSize()), f, g, i, string);
        } else {
            imDrawList.addText(f, g, i, string);
        }
    }

    public static float textWidth(ImFont imFont, String string) {
        return imFont == null
                ? string.length() * 7.0F
                : imFont.calcTextSizeAX(imFont.getFontSize(), 3.4028235E38F, 0.0F, string);
    }

    public static float spacedWidth(ImFont imFont, String string, float f) {
        float g = 0.0F;
        for (int i = 0; i < string.length(); i++) {
            g += textWidth(imFont, String.valueOf(string.charAt(i)));
        }
        return g + f * Math.max(0, string.length() - 1);
    }

    public static void drawSpaced(
            ImDrawList imDrawList,
            ImFont imFont,
            float f,
            float g,
            String string,
            float h,
            int i,
            float f2,
            boolean bl) {
        float g2 = f - spacedWidth(imFont, string, h) * 0.5F;
        for (int k = 0; k < string.length(); k++) {
            String string2 = String.valueOf(string.charAt(k));
            if (bl) {
                text(imDrawList, imFont, g2 + 1.0F, g + 1.0F, string2, col(-16777216, 0.7F * f2));
            }
            text(imDrawList, imFont, g2, g, string2, col(i, f2));
            g2 += textWidth(imFont, string2) + h;
        }
    }

    public static ThemeImages.Texture texture(String string) {
        if (TEXTURES.containsKey(string)) {
            return TEXTURES.get(string);
        } else {
            ThemeImages.Texture texture = null;
            try (InputStream resourceAsStream = HudDraw.class.getResourceAsStream(string)) {
                if (resourceAsStream != null) {
                    texture = ThemeImages.decode(resourceAsStream.readAllBytes(), string, false);
                } else {
                    LOGGER.warn("[EFN+] Missing texture {}", string);
                }
            } catch (RuntimeException | IOException exception) {
                LOGGER.warn("[EFN+] Failed to load {}", string, exception);
            }
            TEXTURES.put(string, texture);
            return texture;
        }
    }

    public static float intro(float f, float g) {
        float h = Math.max(0.0F, Math.min(1.0F, (f - g) / 0.6F));
        float f2 = 1.0F - h;
        return 1.0F - f2 * f2 * f2;
    }
}
