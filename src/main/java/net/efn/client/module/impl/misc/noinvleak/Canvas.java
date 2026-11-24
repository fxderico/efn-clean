package net.efn.client.module.impl.misc.noinvleak;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.IntBuffer;

@Environment(EnvType.CLIENT)
final class Canvas implements DigitFont.Sink {

    private IntBuffer pixels;

    private int stride;

    private int width;

    private int height;

    Canvas() {}

    void wrap(IntBuffer pixels2, int stride2, int width2, int height2) {
        this.pixels = pixels2;
        this.stride = stride2;
        this.width = width2;
        this.height = height2;
    }

    @Override
    public void rect(int i, int j, int k, int n, int m) {
        int o = m >>> 24;
        if (o != 0) {
            int p = Math.max(i, 0);
            int q = Math.max(j, 0);
            int r = Math.min(i + k, this.width);
            int t = Math.min(j + n, this.height);
            if (p < r && q < t) {
                int u = o == 255 ? m : premultiply(m, o);
                for (int v = q; v < t; v++) {
                    int w = v * this.stride;
                    for (int i2 = p; i2 < r; i2++) {
                        this.pixels.put(w + i2, o == 255 ? u : over(u, this.pixels.get(w + i2)));
                    }
                }
            }
        }
    }

    void copy(int[] ints, int i, int j, int k, int m) {
        for (int n = 0; n < j; n++) {
            int o = m + n;
            int p;
            int q;
            if (o >= 0
                    && o < this.height
                    && (q = Math.max(0, -k)) < (p = Math.min(i, this.width - k))) {
                this.pixels.put(o * this.stride + k + q, ints, n * i + q, p - q);
            }
        }
    }

    void blend(int[] ints, int i, int j, int k, int m, int n) {
        for (int o = 0; o < j; o++) {
            for (int p = 0; p < i; p++) {
                this.blendPixel(ints[o * i + p], k + p * n, m + o * n, n);
            }
        }
    }

    void blendPixel(int i, int j, int k, int m) {
        if (i >>> 24 != 0) {
            int n = Math.max(j, 0);
            int o = Math.max(k, 0);
            int p = Math.min(j + m, this.width);
            int q = Math.min(k + m, this.height);
            for (int r = o; r < q; r++) {
                int t = r * this.stride;
                for (int u = n; u < p; u++) {
                    this.pixels.put(t + u, over(i, this.pixels.get(t + u)));
                }
            }
        }
    }

    static int premultiply(int i, int j) {
        return j << 24
                | (i >>> 16 & 0xFF) * j / 255 << 16
                | (i >>> 8 & 0xFF) * j / 255 << 8
                | (i & 0xFF) * j / 255;
    }

    static int over(int i, int j) {
        int k = i >>> 24;
        if (k == 255) {
            return i;
        } else if (k == 0) {
            return j;
        } else {
            int m = 255 - k;
            return k + (j >>> 24) * m / 255 << 24
                    | (i >>> 16 & 0xFF) + (j >>> 16 & 0xFF) * m / 255 << 16
                    | (i >>> 8 & 0xFF) + (j >>> 8 & 0xFF) * m / 255 << 8
                    | (i & 0xFF) + (j & 0xFF) * m / 255;
        }
    }
}
