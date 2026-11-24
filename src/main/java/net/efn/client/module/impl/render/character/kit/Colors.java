package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Colors {

    public static final int WHITE = -1;

    public static final int BLACK = -16777216;

    private Colors() {}

    public static int rgb(int i, int j, int k) {
        return 0xFF000000 | clamp(i) << 16 | clamp(j) << 8 | clamp(k);
    }

    public static int argb(int i, int j, int k, int m) {
        return clamp(i) << 24 | clamp(j) << 16 | clamp(k) << 8 | clamp(m);
    }

    public static int hex(String string) {
        String string2 = string.startsWith("#") ? string.substring(1) : string;
        long l = Long.parseLong(string2, 16);
        return string2.length() <= 6 ? (int) (4278190080L | l) : (int) l;
    }

    public static int hsv(float f, float g, float h) {
        float f2 = (f % 360.0F + 360.0F) % 360.0F / 60.0F;
        float g2 = h * g;
        float h2 = g2 * (1.0F - Math.abs(f2 % 2.0F - 1.0F));
        float f3;
        float g3;
        float h3;
        if (f2 < 1.0F) {
            h3 = g2;
            g3 = h2;
            f3 = 0.0F;
        } else if (f2 < 2.0F) {
            h3 = h2;
            g3 = g2;
            f3 = 0.0F;
        } else if (f2 < 3.0F) {
            h3 = 0.0F;
            g3 = g2;
            f3 = h2;
        } else if (f2 < 4.0F) {
            h3 = 0.0F;
            g3 = h2;
            f3 = g2;
        } else if (f2 < 5.0F) {
            h3 = h2;
            g3 = 0.0F;
            f3 = g2;
        } else {
            h3 = g2;
            g3 = 0.0F;
            f3 = h2;
        }
        float f4 = h - g2;
        return rgb(
                Math.round((h3 + f4) * 255.0F),
                Math.round((g3 + f4) * 255.0F),
                Math.round((f3 + f4) * 255.0F));
    }

    public static int mix(int i, int j, float f) {
        float g = 1.0F - f;
        return argb(
                Math.round(alpha(i) * g + alpha(j) * f),
                Math.round(red(i) * g + red(j) * f),
                Math.round(green(i) * g + green(j) * f),
                Math.round(blue(i) * g + blue(j) * f));
    }

    public static int shade(int i, float f) {
        if (f <= 1.0F) {
            return argb(
                    alpha(i),
                    Math.round(red(i) * f),
                    Math.round(green(i) * f),
                    Math.round(blue(i) * f));
        } else {
            float g = Math.min(1.0F, f - 1.0F);
            return argb(
                    alpha(i),
                    Math.round(red(i) + (255 - red(i)) * g),
                    Math.round(green(i) + (255 - green(i)) * g),
                    Math.round(blue(i) + (255 - blue(i)) * g));
        }
    }

    public static int withAlpha(int i, float f) {
        return clamp(Math.round(f * 255.0F)) << 24 | i & 16777215;
    }

    public static int alpha(int i) {
        return i >>> 24;
    }

    public static int red(int i) {
        return i >> 16 & 0xFF;
    }

    public static int green(int i) {
        return i >> 8 & 0xFF;
    }

    public static int blue(int i) {
        return i & 0xFF;
    }

    static int clamp(int i) {
        return i < 0 ? 0 : Math.min(i, 255);
    }
}
