package net.efn.client.util.glyph;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class GlyphTint {

    public static final String THEME = "Theme";

    public static final String MANUAL = "Manual";

    private static final float WARM = 0.16666667F;

    private static final float COOL = 0.6666667F;

    private static final float COMPANION_TURN = -0.09F;

    private int primary;

    private int secondary;

    private int highlight;

    private int companion;

    private int dust;

    private int flash;

    private float hue;

    private float saturation;

    private float value;

    public GlyphTint() {}

    public GlyphTint resolve(String string, int i, int j, int k) {
        boolean bl = "Manual".equals(string);
        this.primary = ColorHelper.fullAlpha(bl ? j : i);
        this.split(this.primary);
        if (bl) {
            this.secondary = ColorHelper.fullAlpha(k);
            this.companion = mix(this.primary, this.secondary, 0.5F);
        } else {
            this.secondary =
                    hsv(
                            toward(this.hue, 0.6666667F, 0.025F),
                            this.saturation * 1.05F,
                            this.value * 0.66F);
            this.companion = hsv(this.hue + -0.09F, this.saturation * 0.9F, this.value * 0.94F);
        }
        this.highlight =
                hsv(
                        toward(this.hue, 0.16666667F, 0.03F),
                        this.saturation * 0.5F,
                        this.value + (1.0F - this.value) * 0.85F);
        this.dust =
                hsv(
                        toward(this.hue, 0.16666667F, 0.02F),
                        this.saturation * 0.34F,
                        0.5F + this.value * 0.4F);
        this.flash = mix(this.highlight, -1, 0.6F);
        return this;
    }

    private void split(int i) {
        int j = ColorHelper.getRed(i);
        int k = ColorHelper.getGreen(i);
        int m = ColorHelper.getBlue(i);
        int n = Math.max(j, Math.max(k, m));
        int o = Math.min(j, Math.min(k, m));
        float f = n - o;
        this.value = n / 255.0F;
        float g = this.saturation = n == 0 ? 0.0F : f / n;
        this.hue =
                f == 0.0F
                        ? 0.0F
                        : (n == j
                                ? MathHelper.fractionalPart((k - m) / f / 6.0F)
                                : (n == k
                                        ? (2.0F + (m - j) / f) / 6.0F
                                        : (4.0F + (j - k) / f) / 6.0F));
    }

    private static float toward(float f, float g, float h) {
        float f2 = g - f;
        float g2;
        return Math.abs(g2 = f2 - MathHelper.floor(f2 + 0.5F)) <= h ? g : f + Math.copySign(h, g2);
    }

    private static int hsv(float f, float g, float h) {
        float fractionalPart = MathHelper.fractionalPart(f);
        if (fractionalPart >= 1.0F) {
            fractionalPart = 0.0F;
        }
        return MathHelper.hsvToArgb(
                fractionalPart,
                MathHelper.clamp(g, 0.0F, 1.0F),
                MathHelper.clamp(h, 0.0F, 1.0F),
                255);
    }

    public int primary() {
        return this.primary;
    }

    public int secondary() {
        return this.secondary;
    }

    public int highlight() {
        return this.highlight;
    }

    public int companion() {
        return this.companion;
    }

    public int dust() {
        return this.dust;
    }

    public int flash() {
        return this.flash;
    }

    public static int alpha(int i, float f) {
        float g = f <= 0.0F ? 0.0F : Math.min(1.0F, f);
        return (int) (g * 255.0F + 0.5F) << 24 | i & 16777215;
    }

    public static int mix(int i, int j, float f) {
        return ColorHelper.lerp(Math.max(0.0F, Math.min(1.0F, f)), i, j);
    }

    public static int lift(int i, float f) {
        return mix(ColorHelper.fullAlpha(i), -1, f);
    }
}
