package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Noise {

    private static final int[] PERM = new int[512];

    private Noise() {}

    public static float noise(float f, float g, float h) {
        int i = floor(f);
        int j = floor(g);
        int k = floor(h);
        float f2 = f - i;
        float g2 = g - j;
        float h2 = h - k;
        int m = i & 0xFF;
        int n = j & 0xFF;
        int o = k & 0xFF;
        float fade = fade(f2);
        float fade2 = fade(g2);
        float fade3 = fade(h2);
        int p = PERM[m] + n;
        int q = PERM[p] + o;
        int r = PERM[p + 1] + o;
        int t = PERM[m + 1] + n;
        int u = PERM[t] + o;
        int v = PERM[t + 1] + o;
        float f3 = lerp(grad(PERM[q], f2, g2, h2), grad(PERM[u], f2 - 1.0F, g2, h2), fade);
        float g3 =
                lerp(
                        grad(PERM[r], f2, g2 - 1.0F, h2),
                        grad(PERM[v], f2 - 1.0F, g2 - 1.0F, h2),
                        fade);
        float h3 = lerp(f3, g3, fade2);
        float f4 =
                lerp(
                        grad(PERM[q + 1], f2, g2, h2 - 1.0F),
                        grad(PERM[u + 1], f2 - 1.0F, g2, h2 - 1.0F),
                        fade);
        float g4 =
                lerp(
                        grad(PERM[r + 1], f2, g2 - 1.0F, h2 - 1.0F),
                        grad(PERM[v + 1], f2 - 1.0F, g2 - 1.0F, h2 - 1.0F),
                        fade);
        return lerp(h3, lerp(f4, g4, fade2), fade3);
    }

    public static float noise(float f, float g) {
        return noise(f, g, 0.37F);
    }

    public static float fbm(float f, float g, float h, int i) {
        float f2 = 0.0F;
        float g2 = 0.5F;
        float h2 = 0.0F;
        for (int j = 0; j < i; j++) {
            f2 += g2 * noise(f, g, h);
            h2 += g2;
            f = f * 2.03F + 17.1F;
            g = g * 2.03F - 4.7F;
            h = h * 2.03F + 9.3F;
            g2 *= 0.5F;
        }
        return f2 / h2;
    }

    public static float fbm(float f, float g, int i) {
        return fbm(f, g, 0.37F, i);
    }

    public static float hash(int i, int j, int k) {
        int m = i * 522133279 ^ j * 1540483477 ^ k * 668265263;
        m ^= m >>> 13;
        m *= -2048144789;
        m ^= m >>> 16;
        return (m & 65535) / 65535.0F;
    }

    private static int floor(float f) {
        int i = (int) f;
        return f < i ? i - 1 : i;
    }

    private static float fade(float f) {
        return f * f * f * (f * (f * 6.0F - 15.0F) + 10.0F);
    }

    private static float lerp(float f, float g, float h) {
        return f + (g - f) * h;
    }

    private static float grad(int i, float f, float g, float h) {
        int j = i & 15;
        float f2 = j < 8 ? f : g;
        float g2 = j < 4 ? g : (j != 12 && j != 14 ? h : f);
        return ((j & 1) == 0 ? f2 : -f2) + ((j & 2) == 0 ? g2 : -g2);
    }

    static {
        int[] ints = new int[256];
        int i = 0;
        while (i < 256) {
            ints[i] = i++;
        }
        long l = 25214903917L;
        for (int j = 255; j > 0; j--) {
            l = l * 6364136223846793005L + 1442695040888963407L;
            int k = (int) ((l >>> 33) % (j + 1));
            int m = ints[j];
            ints[j] = ints[k];
            ints[k] = m;
        }
        for (int n = 0; n < 512; n++) {
            PERM[n] = ints[n & 0xFF];
        }
    }
}
