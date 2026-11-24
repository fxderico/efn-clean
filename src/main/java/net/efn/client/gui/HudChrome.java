package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;

import net.efn.client.config.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class HudChrome {

    static final int TEXT_VALUE = GuiPalette.abgr(-657929);

    static final int TEXT_NAME = GuiPalette.abgr(-1447442);

    static final int TEXT_LABEL = GuiPalette.abgr(-6052949);

    static final int WARN = GuiPalette.abgr(-47814);

    static final int BORDER = 402653183;

    static final int DIVIDER = 536870911;

    static final int TRACK = 452984831;

    static final int CHIP_FILL = GuiPalette.abgr(-350873571);

    private static final int TEXT_GUARD = -1946157056;

    private static final Long2FloatOpenHashMap DIGIT_CELLS = new Long2FloatOpenHashMap();

    private static final String[] ASCII = new String[128];

    private HudChrome() {}

    static void card(
            ImDrawList imDrawList,
            Surface surface,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            boolean bl,
            float f3) {
        if (!(h2 <= 0.001F)) {
            if (bl) {
                shadow(imDrawList, f, g, h, f2, g2, h2, f3);
            }
            imDrawList.addRectFilled(
                    f, g, f + h, g + f2, scaleAlpha(GuiPalette.fill(surface), h2), g2);
            SurfaceBackground.draw(imDrawList, surface, f, g, h, f2, g2, h2);
            imDrawList.addRect(f, g, f + h, g + f2, scaleAlpha(402653183, h2), g2, 0, 1.0F);
            int accent = GuiPalette.accent();
            float g3 = Math.max(1.5F, 1.5F * f3);
            float h3 = Math.min(h * 0.5F, 56.0F * Math.max(1.0F, f3));
            float f4 = f + Math.min(g2, h * 0.2F);
            imDrawList.addRectFilledMultiColor(
                    f4,
                    g,
                    f4 + h3,
                    g + g3,
                    scaleAlpha(accent, h2 * 0.95F),
                    scaleAlpha(accent, 0.0F),
                    scaleAlpha(accent, 0.0F),
                    scaleAlpha(accent, h2 * 0.95F));
        }
    }

    static void shadow(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            float f3) {
        float g3 = Math.max(1.0F, f3);
        imDrawList.addRectFilled(
                f - g3,
                g - 0.5F * g3,
                f + h + g3,
                g + f2 + 1.5F * g3,
                scaleAlpha(771751936, h2),
                g2 + g3);
        imDrawList.addRectFilled(
                f - 2.0F * g3,
                g + 0.5F * g3,
                f + h + 2.0F * g3,
                g + f2 + 3.5F * g3,
                scaleAlpha(385875968, h2),
                g2 + 2.0F * g3);
        imDrawList.addRectFilled(
                f - 3.0F * g3,
                g + 1.5F * g3,
                f + h + 3.0F * g3,
                g + f2 + 6.0F * g3,
                scaleAlpha(201326592, h2),
                g2 + 3.0F * g3);
    }

    static void divider(ImDrawList imDrawList, float f, float g, float h, float f2) {
        float g2 = Math.round(f);
        float h2 = Math.round(g - h * 0.5F);
        imDrawList.addRectFilled(g2, h2, g2 + 1.0F, h2 + Math.round(h), scaleAlpha(536870911, f2));
    }

    static boolean needsTextGuard(Surface surface) {
        return surface != null && (surface.opacity.get() < 0.55F || surface.isImage());
    }

    static int px(float f, float g) {
        return Math.max(6, Math.round(f * g));
    }

    static float width(ImFont imFont, int i, String string) {
        return imFont != null && !string.isEmpty()
                ? imFont.calcTextSizeA(i, 3.4028235E38F, 0.0F, string).x
                : string.length() * i * 0.55F;
    }

    static float ascent(ImFont imFont, int i) {
        return imFont != null && !(imFont.getFontSize() <= 0.0F)
                ? imFont.getAscent() * i / imFont.getFontSize()
                : i * 0.77F;
    }

    static float baselineOffset(ImFont imFont, int i, ImFont imFont2, int j) {
        return Math.round(ascent(imFont, i) - ascent(imFont2, j));
    }

    static void text(
            ImDrawList imDrawList,
            ImFont imFont,
            int i,
            float f,
            float g,
            int j,
            String string,
            boolean bl) {
        if (!string.isEmpty() && (j >>> 24 & 0xFF) != 0) {
            if (bl) {
                raw(
                        imDrawList,
                        imFont,
                        i,
                        f + 1.0F,
                        g + 1.0F,
                        scaleAlpha(-1946157056, (j >>> 24 & 0xFF) / 255.0F),
                        string);
            }
            raw(imDrawList, imFont, i, f, g, j, string);
        }
    }

    private static void raw(
            ImDrawList imDrawList, ImFont imFont, int i, float f, float g, int j, String string) {
        if (imFont != null) {
            imDrawList.addText(imFont, i, f, g, j, string);
        } else {
            imDrawList.addText(f, g, j, string);
        }
    }

    private static float digitCell(ImFont imFont, int i) {
        if (imFont == null) {
            return i * 0.6F;
        } else {
            long l = (long) System.identityHashCode(imFont) << 16 ^ i;
            float f = DIGIT_CELLS.get(l);
            if (!Float.isNaN(f)) {
                return f;
            } else {
                float g = 0.0F;
                for (char c = '0'; c <= '9'; c++) {
                    g = Math.max(g, advance(imFont, i, c));
                }
                if (DIGIT_CELLS.size() > 64) {
                    DIGIT_CELLS.clear();
                }
                DIGIT_CELLS.put(l, g);
                return g;
            }
        }
    }

    private static float advance(ImFont imFont, int i, char c) {
        return imFont.getCharAdvance(c) * i / imFont.getFontSize();
    }

    static float tabularWidth(ImFont imFont, int i, String string) {
        if (imFont == null) {
            return width(null, i, string);
        } else {
            float digitCell = digitCell(imFont, i);
            float f = 0.0F;
            for (int j = 0; j < string.length(); j++) {
                char c = string.charAt(j);
                f += Character.isDigit(c) ? digitCell : advance(imFont, i, c);
            }
            return f;
        }
    }

    static void tabular(
            ImDrawList imDrawList,
            ImFont imFont,
            int i,
            float f,
            float g,
            int j,
            String string,
            boolean bl) {
        if (imFont == null) {
            text(imDrawList, null, i, f, g, j, string, bl);
        } else {
            float digitCell = digitCell(imFont, i);
            float h = f;
            for (int k = 0; k < string.length(); k++) {
                char c = string.charAt(k);
                float advance = advance(imFont, i, c);
                if (Character.isDigit(c)) {
                    text(
                            imDrawList,
                            imFont,
                            i,
                            h + (digitCell - advance) * 0.5F,
                            g,
                            j,
                            glyph(c),
                            bl);
                    h += digitCell;
                } else {
                    if (c != ' ') {
                        text(imDrawList, imFont, i, h, g, j, glyph(c), bl);
                    }
                    h += advance;
                }
            }
        }
    }

    private static String glyph(char c) {
        return c < ASCII.length ? ASCII[c] : String.valueOf(c);
    }

    static int scaleAlpha(int i, float f) {
        int j = i >>> 24 & 0xFF;
        int k = Math.round(j * Math.max(0.0F, Math.min(1.0F, f)));
        return i & 16777215 | k << 24;
    }

    static {
        DIGIT_CELLS.defaultReturnValue(0.0F / 0.0F);
        for (char c = 0; c < ASCII.length; c++) {
            ASCII[c] = String.valueOf(c);
        }
    }
}
