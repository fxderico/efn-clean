package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D.Float;
import java.awt.image.BufferedImage;

@Environment(EnvType.CLIENT)
final class PeepArt {

    static final int STREAK = 0;

    static final int DROP = 1;

    static final int PETAL = 2;

    static final int HEART_LEFT = 3;

    static final int HEART_RIGHT = 4;

    static final int FEATHER = 5;

    static final int WISP = 6;

    static final int BEAM = 7;

    static final int TWINKLE = 8;

    static final int ARC = 9;

    static final int MIST = 10;

    static final int BOLT = 11;

    static final int BOLT_FORK = 12;

    static final int HEART = 13;

    static final int CONE = 14;

    static final int PETAL_CURL = 15;

    static final int GLINT = 16;

    static final int COUNT = 17;

    static final float ARC_RADIUS = 0.421875F;

    static final float CONE_FOOT = 0.05F;

    static final float CONE_END = 0.9F;

    static final float FEATHER_ASPECT = 0.28846154F;

    private PeepArt() {}

    static PeepArt.Art[] drawAll() {
        PeepArt.Art[] arts = new PeepArt.Art[17];
        arts[0] = streak();
        arts[1] = drop();
        arts[2] = petal(false);
        arts[15] = petal(true);
        PeepArt.Art[] arts2 = heart();
        arts[13] = arts2[0];
        arts[3] = arts2[1];
        arts[4] = arts2[2];
        arts[5] = feather();
        arts[6] = wisp();
        arts[7] = beam();
        arts[8] = twinkle();
        arts[9] = arc();
        arts[10] = mist();
        arts[11] = bolt(7, false);
        arts[12] = bolt(23, true);
        arts[14] = cone();
        arts[16] = glint();
        return arts;
    }

    static int[] rim(int[] ints, int i, int j) {
        float[] floats = new float[i * j];
        for (int k = 0; k < floats.length; k++) {
            floats[k] = (ints[k] >>> 24) / 255.0F;
        }
        float[] floats2 = PeepSky.blur(floats, i, j, 3);
        float[] floats3 = PeepSky.blur(floats, i, j, 7);
        float[] floats4 = new float[floats.length];
        for (int m = 0; m < floats4.length; m++) {
            float f =
                    MathHelper.clamp(
                                    floats2[m] * 2.2F - smooth(0.55F, 0.95F, floats[m]) * 1.2F,
                                    0.0F,
                                    1.0F)
                            * (1.0F - smooth(0.6F, 0.8F, floats3[m]));
            float g =
                    floats[m]
                            * smooth(0.4F, 0.6F, floats3[m])
                            * (1.0F - smooth(0.85F, 0.97F, floats3[m]));
            float h = floats[m] * (1.0F - smooth(0.45F, 0.6F, floats3[m]));
            floats4[m] = Math.max(f, Math.max(g, h));
        }
        return white(floats4);
    }

    static int[] gold(int[] ints, int i, int j) {
        float[] floats = new float[i * j];
        for (int k = 0; k < floats.length; k++) {
            floats[k] = smooth(0.3F, 0.65F, (ints[k] >>> 24) / 255.0F);
        }
        float[] floats2 = PeepSky.blur(floats, i, j, 7);
        float[] floats3 = PeepSky.blur(floats, i, j, 12);
        float[] floats4 = PeepSky.blur(floats, i, j, 2);
        int[] ints2 = new int[i * j];
        for (int m = 0; m < j; m++) {
            for (int n = 0; n < i; n++) {
                int o = m * i + n;
                float f = floats[o] * (1.0F - smooth(0.88F, 0.98F, floats3[o]));
                if (!((f =
                                Math.max(
                                        f,
                                        Math.min(1.0F, floats4[o] * 1.8F)
                                                * (1.0F - smooth(0.25F, 0.45F, floats2[o]))))
                        <= 0.002F)) {
                    float g =
                            floats4[Math.min(j - 1, m + 2) * i + Math.min(i - 1, n + 2)]
                                    - floats4[Math.max(0, m - 2) * i + Math.max(0, n - 2)];
                    float h = MathHelper.clamp(0.5F + 1.1F * g, 0.0F, 1.0F);
                    int p = Math.round(MathHelper.lerp(h, 0.85F, 1.0F) * 255.0F);
                    int q = Math.round(MathHelper.lerp(h, 0.8F, 1.0F) * 255.0F);
                    int r = Math.round(MathHelper.lerp(h, 0.45F, 1.0F) * 255.0F);
                    ints2[o] =
                            Math.round(MathHelper.clamp(f, 0.0F, 1.0F) * 255.0F) << 24
                                    | p << 16
                                    | q << 8
                                    | r;
                }
            }
        }
        return ints2;
    }

    static int[] veil(int[] ints, int i, int j) {
        float[] floats = new float[i * j];
        for (int k = 0; k < floats.length; k++) {
            floats[k] = (ints[k] >>> 24) / 255.0F;
        }
        float[] floats2 = PeepSky.blur(floats, i, j, 3);
        float[] floats3 = PeepSky.blur(floats, i, j, 7);
        float[] floats4 = PeepSky.blur(floats, i, j, 11);
        float[] floats5 = new float[floats.length];
        for (int m = 0; m < floats5.length; m++) {
            float f = 1.0F - 0.85F * smooth(0.6F, 0.9F, floats3[m]);
            float g = Math.min(1.0F, floats2[m] * 1.15F) * f;
            floats5[m] = Math.max(g, Math.min(1.0F, floats4[m] * 2.2F) * 0.45F * (0.4F + 0.6F * f));
        }
        return white(floats5);
    }

    private static PeepArt.Art streak() {
        short s = 256;
        byte b = 32;
        float[] floats = new float[s * b];
        float f = 0.93F;
        for (int i = 0; i < b; i++) {
            for (int j = 0; j < s; j++) {
                float g = (j + 0.5F) / s;
                float h = i + 0.5F - b * 0.5F;
                float f2 =
                        g < f
                                ? (float) Math.pow(g / f, 2.6)
                                : (float) Math.cos((g - f) / (1.0F - f) * 1.5707964F);
                float g2 = 0.7F + 2.6F * (float) Math.pow(g, 1.6);
                float h2 = f2 * gauss(h, g2);
                float f3 = 0.9F * gauss((float) Math.hypot(j + 0.5F - s * 0.9F, h), 3.4F);
                floats[i * s + j] = Math.min(1.0F, h2 + f3);
            }
        }
        return new PeepArt.Art(s, b, white(fadeBorder(floats, s, b, 2)));
    }

    private static PeepArt.Art drop() {
        byte b = 12;
        byte b2 = 96;
        float[] floats = new float[b * b2];
        for (int i = 0; i < b2; i++) {
            float f = (i + 0.5F) / b2;
            float g = (float) Math.pow(Math.sin(3.141592653589793 * f), 0.7) * (0.3F + 0.7F * f);
            for (int j = 0; j < b; j++) {
                floats[i * b + j] = g * gauss(j + 0.5F - b * 0.5F, 1.15F);
            }
        }
        return new PeepArt.Art(b, b2, white(fadeBorder(floats, b, b2, 1)));
    }

    private static PeepArt.Art petal(boolean bl) {
        byte b = 60;
        byte b2 = 68;
        byte b3 = 6;
        Float value = new Float();
        if (bl) {
            value.moveTo(0.52, 0.99);
            value.curveTo(0.2, 0.84, 0.03, 0.52, 0.12, 0.22);
            value.curveTo(0.17, 0.06, 0.31, 0.02, 0.41, 0.13);
            value.curveTo(0.56, 0.25, 0.76, 0.21, 0.93, 0.07);
            value.curveTo(1.0, 0.42, 0.85, 0.8, 0.52, 0.99);
        } else {
            value.moveTo(0.46, 0.99);
            value.curveTo(0.3, 0.9, 0.06, 0.62, 0.05, 0.38);
            value.curveTo(0.04, 0.14, 0.24, 0.02, 0.45, 0.04);
            value.curveTo(0.55, 0.05, 0.6, 0.075, 0.64, 0.07);
            value.curveTo(0.8, 0.05, 0.97, 0.18, 0.95, 0.4);
            value.curveTo(0.93, 0.62, 0.7, 0.88, 0.54, 0.99);
        }
        value.closePath();
        float[] floats = fill(value, b, b2, b3);
        float[] floats2 = PeepSky.blur(floats, b, b2, 1);
        float[] floats3 = PeepSky.blur(floats, b, b2, 4);
        int[] ints = new int[b * b2];
        for (int i = 0; i < b2; i++) {
            for (int j = 0; j < b; j++) {
                int k = i * b + j;
                float f = floats2[k];
                if (!(f <= 0.002F)) {
                    float g = (j + 0.5F - b3) / (b - b3 * 2.0F);
                    float h = (i + 0.5F - b3) / (b2 - b3 * 2.0F);
                    float f2 = (float) Math.hypot((g - 0.5F) * 1.2F, h - 0.42F) / 0.6F;
                    int m = mix(16763356, 16088488, smooth(0.08F, 0.75F, f2));
                    m = mix(m, 13582964, smooth(0.7F, 1.15F, f2) * 0.65F);
                    float g2 = smooth(0.64F, 1.0F, h);
                    m = mix(m, 11020378, g2 * 0.5F);
                    float h2 = (float) Math.atan2(g - 0.5F, 1.0F - h);
                    float f3 =
                            (float) Math.pow(Math.abs(Math.cos(h2 * 11.0F)), 24.0)
                                    * (1.0F - g2)
                                    * smooth(0.1F, 0.5F, 1.0F - h);
                    m = mix(m, 16768232, f3 * 0.2F);
                    float g3 =
                            (1.0F - smooth(0.55F, 0.9F, floats3[k]))
                                    * (1.0F - smooth(0.4F, 0.9F, h));
                    m = mix(m, 16758996, g3 * 0.55F);
                    if (bl) {
                        float h3 =
                                (1.0F - smooth(0.5F, 0.85F, floats3[k]))
                                        * (1.0F - smooth(0.16F, 0.4F, h));
                        float f4 =
                                smooth(0.14F, 0.26F, h)
                                        * (1.0F - smooth(0.26F, 0.46F, h))
                                        * smooth(0.3F, 0.8F, floats3[k]);
                        m = mix(mix(m, 16767462, h3 * 0.7F), 11546719, f4 * 0.35F);
                    }
                    ints[k] = ColorHelper.withAlpha(MathHelper.clamp(f * 0.95F, 0.0F, 1.0F), m);
                }
            }
        }
        return new PeepArt.Art(b, b2, ints);
    }

    private static PeepArt.Art[] heart() {
        short s = 128;
        byte b = 120;
        byte b2 = 16;
        Float value = new Float();
        value.moveTo(0.5, 0.95);
        value.curveTo(0.13, 0.68, 0.0, 0.44, 0.08, 0.24);
        value.curveTo(0.17, 0.04, 0.42, 0.03, 0.5, 0.24);
        value.curveTo(0.58, 0.03, 0.83, 0.04, 0.92, 0.24);
        value.curveTo(1.0, 0.44, 0.87, 0.68, 0.5, 0.95);
        value.closePath();
        float[] floats = fill(value, s, b, b2);
        float[][] floatGrid = new float[][] {floats, new float[s * b], new float[s * b]};
        for (int i = 0; i < b; i++) {
            float f = s * 0.5F + crack((i + 0.5F - b2) / (b - b2 * 2.0F)) * (s - b2 * 2.0F);
            for (int j = 0; j < s; j++) {
                floatGrid[j + 0.5F < f ? 1 : 2][i * s + j] = floats[i * s + j];
            }
        }
        PeepArt.Art[] arts = new PeepArt.Art[floatGrid.length];
        for (int k = 0; k < floatGrid.length; k++) {
            float[] floats2 = floatGrid[k];
            float[] floats3 = PeepSky.blur(floats2, s, b, 6);
            float[] floats4 = PeepSky.blur(floats2, s, b, 7);
            int[] ints = new int[s * b];
            for (int m = 0; m < b; m++) {
                for (int n = 0; n < s; n++) {
                    int o = m * s + n;
                    float g = (n + 0.5F - b2) / (s - b2 * 2.0F);
                    float h = (m + 0.5F - b2) / (b - b2 * 2.0F);
                    float f2 = floats2[o];
                    float g2 = MathHelper.clamp((f2 - floats3[o]) * 2.6F, 0.0F, 1.0F) * f2;
                    float h2 = gauss((float) Math.hypot(g - 0.3F, h - 0.27F), 0.09F) * f2;
                    int p = mix(12720235, 16732080, 0.25F + h2 * 0.5F);
                    float f3 = Math.max(f2 * (0.16F + 0.1F * (1.0F - h)), g2);
                    float g3 = f3 + (1.0F - f2) * Math.min(1.0F, floats4[o] * 1.5F) * 0.65F;
                    if (!(g3 <= 0.002F)) {
                        int q = mix(16751320, 16770806, smooth(0.75F, 1.0F, g2) * 0.45F);
                        int r = mix(16732080, mix(p, q, g2 / Math.max(f3, 1.0E-4F)), f3 / g3);
                        ints[o] = ColorHelper.withAlpha(MathHelper.clamp(g3, 0.0F, 1.0F), r);
                    }
                }
            }
            arts[k] = new PeepArt.Art(s, b, ints);
        }
        return arts;
    }

    static float crack(float f) {
        float[] floats = new float[] {0.2F, 0.34F, 0.47F, 0.6F, 0.73F, 0.86F, 1.0F};
        float[] floats2 = new float[] {0.0F, -0.05F, 0.045F, -0.04F, 0.04F, -0.025F, 0.0F};
        if (f <= floats[0]) {
            return 0.0F;
        } else {
            for (int i = 1; i < floats.length; i++) {
                if (f <= floats[i]) {
                    return MathHelper.lerp(
                            (f - floats[i - 1]) / (floats[i] - floats[i - 1]),
                            floats2[i - 1],
                            floats2[i]);
                }
            }
            return 0.0F;
        }
    }

    private static PeepArt.Art feather() {
        int i = Math.round(60.0F);
        short s = 208;
        BufferedImage bufferedImage = new BufferedImage(i, s, 2);
        Graphics2D graphics2D = smooth(bufferedImage);
        java.awt.geom.QuadCurve2D.Float value =
                new java.awt.geom.QuadCurve2D.Float(
                        i * 0.42F, s * 0.97F, i * 0.56F, s * 0.5F, i * 0.44F, s * 0.035F);
        byte b = 120;
        for (int j = 0; j < b; j++) {
            float f = 0.06F + 0.92F * j / (b - 1.0F);
            float quad = quad(value.x1, value.ctrlx, value.x2, f);
            float quad2 = quad(value.y1, value.ctrly, value.y2, f);
            float quadSlope = quadSlope(value.x1, value.ctrlx, value.x2, f);
            float quadSlope2 = quadSlope(value.y1, value.ctrly, value.y2, f);
            float g = (float) Math.hypot(quadSlope, quadSlope2);
            quadSlope /= g;
            quadSlope2 /= g;
            float h =
                    (float)
                            Math.pow(
                                    Math.sin(
                                            3.141592653589793
                                                    * Math.min(1.0F, (f - 0.02F) / 0.97F)),
                                    0.75);
            boolean bl = f < 0.3F;
            for (int k = -1; k <= 1; k += 2) {
                float hash01 = PeepSky.hash01(j * 7 + k * 3 + 11);
                if ((bl || !(hash01 < 0.15F))
                        && (k != 1 || j < 50 || j > 53)
                        && (k != -1 || j < 82 || j > 85)) {
                    float f2 =
                            i
                                    * (bl ? 0.36F : 0.4F)
                                    * h
                                    * (0.9F + 0.1F * hash01)
                                    * (k < 0 ? 0.7F : 1.05F);
                    float g2 = (bl ? 0.55F : 0.95F) * k;
                    float h2 = quadSlope * (float) Math.cos(g2) - quadSlope2 * (float) Math.sin(g2);
                    float f3 = quadSlope * (float) Math.sin(g2) + quadSlope2 * (float) Math.cos(g2);
                    float g3 = bl ? 0.35F : 0.12F;
                    graphics2D.setColor(new Color(1.0F, 1.0F, 1.0F, bl ? 0.32F : 0.72F));
                    graphics2D.setStroke(new BasicStroke(bl ? 0.9F : 1.05F, 1, 1));
                    graphics2D.draw(
                            new java.awt.geom.QuadCurve2D.Float(
                                    quad,
                                    quad2,
                                    quad + h2 * f2 * 0.55F,
                                    quad2 + f3 * f2 * 0.55F,
                                    quad
                                            + h2 * f2
                                            - k * quadSlope2 * f2 * g3 * 0.0F
                                            + quadSlope * f2 * g3,
                                    quad2 + f3 * f2 + quadSlope2 * f2 * g3));
                }
            }
        }
        graphics2D.dispose();
        BufferedImage bufferedImage2 = new BufferedImage(i, s, 2);
        graphics2D = smooth(bufferedImage2);
        graphics2D.setColor(new Color(1.0F, 1.0F, 1.0F, 1.0F));
        graphics2D.setStroke(new BasicStroke(1.7F, 1, 1));
        graphics2D.draw(value);
        graphics2D.dispose();
        float[] floats = alpha(bufferedImage);
        float[] floats2 = alpha(bufferedImage2);
        float[] floats3 = new float[floats.length];
        for (int m = 0; m < floats3.length; m++) {
            floats3[m] = floats2[m] + floats[m] * (1.0F - floats2[m]);
        }
        float[] floats4 = PeepSky.blur(floats3, i, s, 1);
        for (int n = 0; n < floats3.length; n++) {
            floats3[n] = Math.min(1.0F, floats3[n] * 0.8F + floats4[n] * 0.45F);
        }
        fadeBorder(floats3, i, s, 2);
        float[] floats5 = PeepSky.blur(floats2, i, s, 1);
        int[] ints = new int[i * s];
        for (int o = 0; o < ints.length; o++) {
            ints[o] =
                    ColorHelper.withAlpha(
                            MathHelper.clamp(floats3[o], 0.0F, 1.0F),
                            mix(16764134, 16777215, Math.min(1.0F, floats5[o] * 1.6F)));
        }
        return new PeepArt.Art(i, s, ints);
    }

    private static PeepArt.Art wisp() {
        byte b = 96;
        short s = 256;
        float[] floats = new float[b * s];
        for (int i = 0; i < s; i++) {
            float f = (i + 0.5F) / s;
            float g = smooth(0.0F, 0.2F, f) * (float) Math.pow(1.0F - f, 1.5);
            float h = (float) Math.pow(f, 1.2);
            float f2 = 0.5F + 0.13F * (float) Math.sin(f * 6.0F + 0.4F) * h;
            float g2 = 0.5F + 0.13F * (float) Math.sin(f * 6.0F + 2.3F) * h;
            float h2 = b * (0.12F * (float) Math.pow(1.0F - f, 0.8) + 0.018F);
            for (int j = 0; j < b; j++) {
                float f3 = (j + 0.5F) / b;
                float gauss = gauss((f3 - f2) * b, h2);
                float g3 = 0.5F * gauss((f3 - g2) * b, h2 * 0.6F) * smooth(0.22F, 0.5F, f);
                float h3 = 0.55F * gauss((f3 - f2) * b, h2 * 0.35F) * (1.0F - f);
                floats[i * b + j] = Math.min(1.0F, g * (0.7F * gauss + g3 + h3));
            }
        }
        return new PeepArt.Art(b, s, white(fadeBorder(PeepSky.blur(floats, b, s, 3), b, s, 3)));
    }

    private static PeepArt.Art beam() {
        short s = 128;
        short s2 = 512;
        float[] floats = new float[s * s2];
        for (int i = 0; i < s2; i++) {
            float f = 1.0F - (i + 0.5F) / s2;
            float g = smooth(0.0F, 0.09F, f) * (float) Math.pow(1.0F - f, 1.25);
            float h = 1.6F + 27.0F * f;
            float f2 = 0.7F + 3.2F * f;
            for (int j = 0; j < s; j++) {
                float g2 = j + 0.5F - s * 0.5F;
                floats[i * s + j] =
                        Math.min(
                                1.0F,
                                g * (0.7F * gauss(g2, h) + 0.45F * gauss(g2, f2) * (1.0F - f)));
            }
        }
        return new PeepArt.Art(s, s2, white(fadeBorder(floats, s, s2, 2)));
    }

    private static PeepArt.Art cone() {
        short s = 256;
        short s2 = 512;
        float[] floats = new float[s * s2];
        for (int i = 0; i < s2; i++) {
            float f = 1.0F - (i + 0.5F) / s2;
            float g =
                    smooth(0.0F, 0.035F, f)
                            * (1.0F - smooth(0.3F, 1.0F, f))
                            * (0.6F + 0.4F * (1.0F - f));
            float h = (0.05F + 0.84999996F * f) * s * 0.5F;
            for (int j = 0; j < s; j++) {
                float f2 = (j + 0.5F - s * 0.5F) / h;
                float g2 = Math.abs(f2);
                if (!(g2 >= 1.2F)) {
                    float h2 = 1.0F - smooth(0.5F, 1.05F, g2);
                    float f3 = 0.35F * gauss(g2, 0.2F);
                    float g3 = 0.88F + 0.12F * valueNoise((f2 + 1.2F) * 3.5F, f * 2.2F, 64, 1);
                    floats[i * s + j] = Math.min(1.0F, g * (0.7F * h2 + f3) * g3);
                }
            }
        }
        return new PeepArt.Art(s, s2, white(fadeBorder(floats, s, s2, 2)));
    }

    private static PeepArt.Art glint() {
        byte b = 32;
        byte b2 = 32;
        float[] floats = new float[b * b2];
        for (int i = 0; i < b2; i++) {
            for (int j = 0; j < b; j++) {
                float f = j + 0.5F - b * 0.5F;
                float g = i + 0.5F - b2 * 0.5F;
                float h = (float) Math.hypot(f, g);
                float f2 = glintRay(f, g) + glintRay(g, f);
                float g2 = gauss(h, 1.6F) + 0.4F * gauss(h, 4.0F);
                floats[i * b + j] = Math.min(1.0F, f2 + g2);
            }
        }
        return new PeepArt.Art(b, b2, white(fadeBorder(floats, b, b2, 1)));
    }

    private static float glintRay(float f, float g) {
        float h = Math.max(0.0F, 1.0F - Math.abs(f) / 15.0F);
        return gauss(g, 0.7F + 0.9F * h) * h * h * 0.9F;
    }

    private static PeepArt.Art twinkle() {
        byte b = 64;
        byte b2 = 64;
        float[] floats = new float[b * b2];
        for (int i = 0; i < b2; i++) {
            for (int j = 0; j < b; j++) {
                float f = j + 0.5F - b * 0.5F;
                float g = i + 0.5F - b2 * 0.5F;
                float h = ray(f, g, 29.0F) + ray(g, f, 29.0F);
                float f2 =
                        0.35F
                                * (ray((f + g) * 0.7071F, (f - g) * 0.7071F, 14.0F)
                                        + ray((f - g) * 0.7071F, (f + g) * 0.7071F, 14.0F));
                float g2 =
                        gauss((float) Math.hypot(f, g), 2.6F)
                                + 0.35F * gauss((float) Math.hypot(f, g), 7.0F);
                floats[i * b + j] = Math.min(1.0F, h + f2 + g2);
            }
        }
        return new PeepArt.Art(b, b2, white(fadeBorder(floats, b, b2, 2)));
    }

    private static PeepArt.Art arc() {
        short s = 256;
        short s2 = 256;
        float f = 0.421875F * s;
        float g = 2.6F;
        float[] floats = new float[s * s2];
        for (int i = 0; i < s2; i++) {
            for (int j = 0; j < s; j++) {
                float h = j + 0.5F - s * 0.5F;
                float f2 = i + 0.5F - s2 * 0.5F;
                float g2 = (float) Math.hypot(h, f2);
                float h2 = (float) (-Math.atan2(f2, h));
                if (h2 < 0.0F) {
                    h2 += 6.2831855F;
                }
                float f3 =
                        h2 < 0.06F ? 1.0F : (h2 > g ? 0.0F : (float) Math.pow(1.0F - h2 / g, 1.6));
                if (h2 > 6.2031856F) {
                    f3 = (float) Math.cos((6.2831855F - h2) / 0.08F * 1.5707964F);
                }
                float g3 = gauss(g2 - f, 1.3F) + 0.3F * gauss(g2 - f, 4.5F);
                floats[i * s + j] = Math.min(1.0F, g3 * f3);
            }
        }
        return new PeepArt.Art(s, s2, white(fadeBorder(floats, s, s2, 2)));
    }

    private static PeepArt.Art mist() {
        short s = 512;
        byte b = 96;
        float[] floats = new float[s * b];
        for (int i = 0; i < b; i++) {
            float f = (i + 0.5F) / b;
            float g = (float) Math.pow(Math.sin(3.141592653589793 * f), 1.4) * (0.55F + 0.45F * f);
            for (int j = 0; j < s; j++) {
                float h = 0.0F;
                float f2 = 0.55F;
                float g2 = 0.0F;
                for (int k = 0; k < 4; k++) {
                    int m = 4 << k;
                    h += f2 * valueNoise((float) j / s * m, f * (2.5F + k * 2.5F) + k * 7.3F, m, k);
                    g2 += f2;
                    f2 *= 0.5F;
                }
                float h2;
                floats[i * s + j] = g * smooth(0.28F, 0.78F, h2 = h / g2);
            }
        }
        float[] floats2 = PeepSky.blur(floats, s, b, 1);
        for (int n = 0; n < b; n++) {
            float f3 = smooth(0.0F, 3.0F, n) * smooth(0.0F, 3.0F, b - 1 - n);
            for (int o = 0; o < s; o++) {
                int p = n * s + o;
                floats2[p] *= f3;
            }
        }
        return new PeepArt.Art(s, b, white(floats2));
    }

    private static PeepArt.Art bolt(int i, boolean bl) {
        short s = 128;
        short s2 = 256;
        BufferedImage bufferedImage = new BufferedImage(s, s2, 2);
        Graphics2D graphics2D = smooth(bufferedImage);
        graphics2D.setColor(Color.WHITE);
        float[] floats = new float[33];
        float[] floats2 = new float[33];
        jag(
                floats,
                floats2,
                s * 0.5F,
                12.0F,
                s * 0.5F + (PeepSky.hash01(i) - 0.5F) * 30.0F,
                s2 - 12.0F,
                i);
        graphics2D.setStroke(new BasicStroke(2.6F, 1, 1));
        graphics2D.draw(polyline(floats, floats2));
        float[] floats3 = new float[17];
        float[] floats4 = new float[17];
        int j = bl ? 2 : 1;
        for (int k = 0; k < j; k++) {
            int m = 9 + k * 8;
            float f = (k % 2 == 0 ? 1.0F : -1.0F) * (PeepSky.hash01(i + k) < 0.5F ? 1.0F : -1.0F);
            jag(
                    floats3,
                    floats4,
                    floats[m],
                    floats2[m],
                    floats[m] + f * (26.0F + 14.0F * k),
                    floats2[m] + 58.0F - 10.0F * k,
                    i * 3 + 1 + k);
            graphics2D.setStroke(new BasicStroke(1.5F - 0.3F * k, 1, 1));
            graphics2D.draw(polyline(floats3, floats4));
        }
        graphics2D.dispose();
        float[] floats5 = alpha(bufferedImage);
        for (int n = 0; n < s2; n++) {
            float g = smooth(12.0F, 44.0F, n);
            for (int o = 0; o < s; o++) {
                int p = n * s + o;
                floats5[p] *= g;
            }
        }
        float[] floats6 = PeepSky.blur(floats5, s, s2, 6);
        int[] ints = new int[s * s2];
        for (int q = 0; q < ints.length; q++) {
            float h = Math.min(1.0F, floats6[q] * 3.0F);
            ints[q] =
                    ColorHelper.withAlpha(
                            MathHelper.clamp(Math.max(floats5[q], h * 0.7F), 0.0F, 1.0F),
                            mix(16732080, 16774395, floats5[q]));
        }
        return new PeepArt.Art(s, s2, ints);
    }

    private static void jag(
            float[] floats, float[] floats2, float f, float g, float h, float f2, int i) {
        int j = floats.length - 1;
        floats[0] = f;
        floats2[0] = g;
        floats[j] = h;
        floats2[j] = f2;
        float g2 = (float) Math.hypot(h - f, f2 - g) * 0.32F;
        for (int m = j; m > 1; m /= 2) {
            for (int n = 0; n + m <= j; n += m) {
                int k = n + m / 2;
                floats[k] =
                        (floats[n] + floats[n + m]) * 0.5F
                                + (PeepSky.hash01(i * 131 + k * 17 + m) - 0.5F) * g2;
                float h2 =
                        (floats2[n] + floats2[n + m]) * 0.5F
                                + (PeepSky.hash01(i * 151 + k * 13 + m) - 0.5F) * g2 * 0.25F;
                float f3 = floats2[n] + 0.3F * (floats2[n + m] - floats2[n]);
                float g3 = floats2[n] + 0.7F * (floats2[n + m] - floats2[n]);
                floats2[k] = MathHelper.clamp(h2, Math.min(f3, g3), Math.max(f3, g3));
            }
            g2 *= 0.62F;
        }
    }

    private static Float polyline(float[] floats, float[] floats2) {
        Float value = new Float();
        value.moveTo(floats[0], floats2[0]);
        for (int i = 1; i < floats.length; i++) {
            value.lineTo(floats[i], floats2[i]);
        }
        return value;
    }

    private static float ray(float f, float g, float h) {
        float f2 = Math.max(0.0F, 1.0F - Math.abs(f) / h);
        return gauss(g, 0.85F + 0.6F * f2) * f2 * f2;
    }

    private static float valueNoise(float f, float g, int i, int j) {
        int k = (int) Math.floor(f);
        int m = (int) Math.floor(g);
        float h = f - k;
        float f2 = g - m;
        h = h * h * (3.0F - 2.0F * h);
        f2 = f2 * f2 * (3.0F - 2.0F * f2);
        float lattice = lattice(k, m, i, j);
        float lattice2 = lattice(k + 1, m, i, j);
        float lattice3 = lattice(k, m + 1, i, j);
        float lattice4 = lattice(k + 1, m + 1, i, j);
        return MathHelper.lerp(
                f2, MathHelper.lerp(h, lattice, lattice2), MathHelper.lerp(h, lattice3, lattice4));
    }

    private static float lattice(int i, int j, int k, int m) {
        return PeepSky.hash01(Math.floorMod(i, k) * 7919 + j * 104729 + m * 15485863);
    }

    private static float[] fill(Float value, int i, int j, int k) {
        BufferedImage bufferedImage = new BufferedImage(i, j, 2);
        Graphics2D graphics2D = smooth(bufferedImage);
        graphics2D.translate(k, k);
        graphics2D.scale(i - k * 2.0, j - k * 2.0);
        graphics2D.setColor(Color.WHITE);
        graphics2D.fill(value);
        graphics2D.dispose();
        return alpha(bufferedImage);
    }

    private static Graphics2D smooth(BufferedImage bufferedImage) {
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return graphics;
    }

    private static float[] alpha(BufferedImage bufferedImage) {
        int width2 = bufferedImage.getWidth();
        int height2 = bufferedImage.getHeight();
        int[] rGB = bufferedImage.getRGB(0, 0, width2, height2, null, 0, width2);
        float[] floats = new float[rGB.length];
        for (int k = 0; k < floats.length; k++) {
            floats[k] = (rGB[k] >>> 24) / 255.0F;
        }
        return floats;
    }

    private static float[] fadeBorder(float[] floats, int i, int j, int k) {
        for (int m = 0; m < j; m++) {
            for (int n = 0; n < i; n++) {
                int o = Math.min(Math.min(n, i - 1 - n), Math.min(m, j - 1 - m));
                if (o < k) {
                    int p = m * i + n;
                    floats[p] *= (float) o / k;
                }
            }
        }
        return floats;
    }

    private static int[] white(float[] floats) {
        int[] ints = new int[floats.length];
        for (int i = 0; i < floats.length; i++) {
            ints[i] = Math.round(MathHelper.clamp(floats[i], 0.0F, 1.0F) * 255.0F) << 24 | 16777215;
        }
        return ints;
    }

    private static float quad(float f, float g, float h, float f2) {
        float g2 = 1.0F - f2;
        return g2 * g2 * f + 2.0F * g2 * f2 * g + f2 * f2 * h;
    }

    private static float quadSlope(float f, float g, float h, float f2) {
        return 2.0F * (1.0F - f2) * (g - f) + 2.0F * f2 * (h - g);
    }

    private static float gauss(float f, float g) {
        return (float) Math.exp(-f * f / (2.0F * g * g));
    }

    private static float smooth(float f, float g, float h) {
        float f2 = MathHelper.clamp((h - f) / (g - f), 0.0F, 1.0F);
        return f2 * f2 * (3.0F - 2.0F * f2);
    }

    private static int mix(int i, int j, float f) {
        return ColorHelper.lerp(MathHelper.clamp(f, 0.0F, 1.0F), i, j) & 16777215;
    }

    @Environment(EnvType.CLIENT)
    record Art(int width, int height, int[] argb) {}
}
