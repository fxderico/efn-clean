package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EfnTheme {

    public static final float CATEGORY_FONT_SIZE = 17.0F;

    public static final float MODULE_FONT_SIZE = 16.0F;

    public static final float TITLE_FONT_SIZE = 24.0F;

    public static final float SMALL_FONT_SIZE = 13.0F;

    public static final int GUI_SURFACE = -13226454;

    public static final int GUI_SURFACE_HOVER = -12240588;

    public static final int GUI_DIVIDER = 268435455;

    public static final int GUI_TEXT = -461582;

    public static final int GUI_TEXT_ROW = -2042416;

    public static final int GUI_TEXT_MUTED = -4542808;

    public static final int GUI_SWITCH_OFF = -11911366;

    public static final int GUI_KNOB = -461067;

    public static final int GUI_SHADOW = 335544320;

    public static final int HUD_LABEL = -7106930;

    private EfnTheme() {}

    public static int lerpColor(int i, int j, float f) {
        int k = i & 0xFF;
        int m = j & 0xFF;
        int n = i >> 8 & 0xFF;
        int o = j >> 8 & 0xFF;
        int p = i >> 16 & 0xFF;
        int q = j >> 16 & 0xFF;
        int r = i >>> 24;
        int t = j >>> 24;
        return (int) (r + (t - r) * f) << 24
                | (int) (p + (q - p) * f) << 16
                | (int) (n + (o - n) * f) << 8
                | (int) (k + (m - k) * f);
    }
}
