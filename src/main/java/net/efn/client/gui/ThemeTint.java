package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ThemeTint {

    private static final int PINK = 16732080;

    private static final int DUSK = 2754594;

    private static final int WARM_ROSE = 16751309;

    private static final float SURFACE_SHARE = 0.45F;

    private static final float WORLD_SHARE = 0.3F;

    private static volatile float pink;

    private ThemeTint() {}

    public static void pink(float f) {
        pink = Math.max(0.0F, Math.min(1.0F, f));
    }

    public static float pink() {
        return pink;
    }

    public static int accent(int i) {
        return mix(i, 16732080, pink);
    }

    public static int surface(int i) {
        return mix(i, 2754594, pink * 0.45F);
    }

    public static float world() {
        float f = pink;
        return f * f * (3.0F - 2.0F * f) * 0.3F;
    }

    public static int world(int i) {
        float world = world();
        if (world <= 0.0F) {
            return i;
        } else {
            float warmth = warmth(i);
            int j = warmth > 0.0F ? mix(16732080, 16751309, warmth) & 16777215 : 16732080;
            return mix(i, j, world * (1.0F - 0.4F * warmth));
        }
    }

    private static float warmth(int i) {
        int j = i >> 16 & 0xFF;
        int k = i >> 8 & 0xFF;
        int m = i & 0xFF;
        int n = Math.max(j, Math.max(k, m));
        int o = Math.min(j, Math.min(k, m));
        if (n != 0 && !(n - o < 0.25F * n) && n != m) {
            float f =
                    n == j ? 60.0F * (k - m) / (n - o) : 60.0F * ((float) (m - j) / (n - o) + 2.0F);
            if (f < 0.0F) {
                f += 360.0F;
            }
            return Math.min(ramp(f, 10.0F, 20.0F), 1.0F - ramp(f, 60.0F, 70.0F));
        } else {
            return 0.0F;
        }
    }

    private static float ramp(float f, float g, float h) {
        return Math.max(0.0F, Math.min(1.0F, (f - g) / (h - g)));
    }

    private static int mix(int i, int j, float f) {
        if (f <= 0.0F) {
            return i;
        } else {
            int k = lerp(i >> 16 & 0xFF, j >> 16 & 0xFF, f);
            int m = lerp(i >> 8 & 0xFF, j >> 8 & 0xFF, f);
            int n = lerp(i & 0xFF, j & 0xFF, f);
            return i & 0xFF000000 | k << 16 | m << 8 | n;
        }
    }

    private static int lerp(int i, int j, float f) {
        return Math.round(i + (j - i) * f);
    }
}
