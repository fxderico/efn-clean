package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class SkyMips {

    private static final int MAX_LEVELS = 6;

    private SkyMips() {}

    static int size(int i, int j) {
        return Math.max(1, i >> j);
    }

    static int[][] chain(int[] ints, int i, int j) {
        int k = 1;
        while (k < 6 && Math.min(i, j) >> k >= 1) {
            k++;
        }
        int[][] intGrid = new int[k][];
        intGrid[0] = ints;
        for (int m = 1; m < k; m++) {
            int n = size(i, m - 1);
            int o = size(j, m - 1);
            int p = size(i, m);
            int q = size(j, m);
            int[] ints2 = intGrid[m - 1];
            int[] ints3 = new int[p * q];
            for (int r = 0; r < q; r++) {
                for (int t = 0; t < p; t++) {
                    float f = 0.0F;
                    float g = 0.0F;
                    float h = 0.0F;
                    float f2 = 0.0F;
                    float g2 = 0.0F;
                    float h2 = 0.0F;
                    float f3 = 0.0F;
                    for (int u = 0; u < 2; u++) {
                        int v = Math.min(o - 1, r * 2 + u);
                        for (int w = 0; w < 2; w++) {
                            int i2 = ints2[v * n + Math.min(n - 1, t * 2 + w)];
                            float g3 = (i2 >>> 24) / 255.0F;
                            f += g3;
                            g += g3 * (i2 >> 16 & 0xFF);
                            h += g3 * (i2 >> 8 & 0xFF);
                            f2 += g3 * (i2 & 0xFF);
                            g2 += i2 >> 16 & 0xFF;
                            h2 += i2 >> 8 & 0xFF;
                            f3 += i2 & 0xFF;
                        }
                    }
                    int j2 = Math.min(255, Math.round(f * 0.25F * 255.0F));
                    if (f <= 1.0E-4F) {
                        f = 4.0F;
                        g = g2;
                        h = h2;
                        f2 = f3;
                    }
                    ints3[r * p + t] =
                            j2 << 24
                                    | Math.min(255, Math.round(g / f)) << 16
                                    | Math.min(255, Math.round(h / f)) << 8
                                    | Math.min(255, Math.round(f2 / f));
                }
            }
            intGrid[m] = ints3;
        }
        return intGrid;
    }
}
