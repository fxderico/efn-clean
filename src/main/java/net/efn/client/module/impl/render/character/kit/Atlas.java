/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Environment(value = EnvType.CLIENT)
final class Atlas {

    static final int WHITE = 16;

    private static final int PAD = 3;

    private static final int MAX = 2048;

    private static final int LEVELS = 5;

    final int width;

    final int height;

    final int[][] mips;

    private final Map<Canvas, int[]> rects;

    private Atlas(int width2, int height2, int[][] mips2, Map<Canvas, int[]> rects2) {
        this.width = width2;
        this.height = height2;
        this.mips = mips2;
        this.rects = rects2;
    }

    float whiteU() {
        return 8.0f / (float) this.width;
    }

    float whiteV() {
        return 8.0f / (float) this.height;
    }

    float u(Canvas canvas, float f) {
        int[] rect = this.rects.get(canvas);
        return ((float) rect[0] + f * (float) rect[2]) / (float) this.width;
    }

    float v(Canvas canvas, float f) {
        int[] rect = this.rects.get(canvas);
        return ((float) rect[1] + f * (float) rect[3]) / (float) this.height;
    }

    static Atlas pack(List<Canvas> list) {
        ArrayList<Canvas> list2 = new ArrayList<Canvas>();
        for (Canvas canvas3 : list) {
            if (list2.contains(canvas3)) continue;
            list2.add(canvas3);
        }
        list2.sort((canvas, canvas2) -> Integer.compare(canvas2.height(), canvas.height()));
        long l = 484L;
        int i = 22;
        for (Canvas canvas4 : list2) {
            l += (long) (canvas4.width() + 6) * (long) (canvas4.height() + 6);
            i = Math.max(i, canvas4.width() + 6);
        }
        int pow2 = Atlas.pow2(Math.max(i, (int) Math.ceil(Math.sqrt(l))));
        while (true) {
            IdentityHashMap<Canvas, int[]> map = new IdentityHashMap<Canvas, int[]>();
            int j = 22;
            int k = 0;
            int m = 22;
            for (Canvas canvas5 : list2) {
                int n = canvas5.width() + 6;
                int o = canvas5.height() + 6;
                if (j + n > pow2) {
                    j = 0;
                    k += m;
                    m = 0;
                }
                map.put(canvas5, new int[] {j + 3, k + 3, canvas5.width(), canvas5.height()});
                j += n;
                m = Math.max(m, o);
            }
            int pow22 = Atlas.pow2(Math.max(22, k + m));
            if (pow22 <= 2048 || pow2 >= 2048) {
                if (pow22 > 2048) {
                    throw new IllegalStateException("decals do not fit into a 2048x2048 atlas");
                }
                return Atlas.render(pow2, pow22, list2, map);
            }
            pow2 *= 2;
        }
    }

    private static Atlas render(int i, int j, List<Canvas> list, Map<Canvas, int[]> map) {
        int[] ints = new int[i * j];
        for (int k = 0; k < 19; ++k) {
            for (int i2 = 0; i2 < 19; ++i2) {
                ints[k * i + i2] = -1;
            }
        }
        for (Canvas canvas : list) {
            int[] ints2 = map.get(canvas);
            int[] ints3 = Atlas.dilate(canvas);
            for (int k = -3; k < canvas.height() + 3; ++k) {
                int m = Math.max(0, Math.min(canvas.height() - 1, k));
                for (int i3 = -3; i3 < canvas.width() + 3; ++i3) {
                    int n = Math.max(0, Math.min(canvas.width() - 1, i3));
                    ints[(ints2[1] + k) * i + ints2[0] + i3] = ints3[m * canvas.width() + n];
                }
            }
        }
        return new Atlas(i, j, Atlas.mips(ints, i, j), map);
    }

    static int[][] mips(int[] ints, int i, int j) {
        int m = Math.min(5, 1 + (31 - Integer.numberOfLeadingZeros(Math.min(i, j))));
        int[][] intGrid = new int[m][];
        intGrid[0] = ints;
        int n = i;
        int o = j;
        for (int k = 1; k < m; ++k) {
            intGrid[k] = Atlas.down(intGrid[k - 1], n, o);
            n = Math.max(1, n / 2);
            o = Math.max(1, o / 2);
        }
        return intGrid;
    }

    private static int[] dilate(Canvas canvas) {
        int m;
        int width2 = canvas.width();
        int height2 = canvas.height();
        int[] ints = Arrays.copyOf(canvas.pixels, canvas.pixels.length);
        boolean[] booleans = new boolean[ints.length];
        for (m = 0; m < ints.length; ++m) {
            booleans[m] = ints[m] >>> 24 != 0;
        }
        for (m = 0; m < 4; ++m) {
            boolean[] booleans2 = (boolean[]) booleans.clone();
            for (int i = 0; i < height2; ++i) {
                for (int j = 0; j < width2; ++j) {
                    int n = i * width2 + j;
                    if (booleans[n]) continue;
                    int o = 0;
                    int p = 0;
                    int q = 0;
                    int r = 0;
                    for (int k = -1; k <= 1; ++k) {
                        for (int i2 = -1; i2 <= 1; ++i2) {
                            int t;
                            int u = j + i2;
                            int v = i + k;
                            if (u < 0
                                    || v < 0
                                    || u >= width2
                                    || v >= height2
                                    || !booleans[t = v * width2 + u]) continue;
                            o += ints[t] >> 16 & 0xFF;
                            p += ints[t] >> 8 & 0xFF;
                            q += ints[t] & 0xFF;
                            ++r;
                        }
                    }
                    if (r == 0) continue;
                    ints[n] = o / r << 16 | p / r << 8 | q / r;
                    booleans2[n] = true;
                }
            }
            booleans = booleans2;
        }
        return ints;
    }

    private static int[] down(int[] ints, int i, int j) {
        int m = Math.max(1, i / 2);
        int n = Math.max(1, j / 2);
        int[] ints2 = new int[m * n];
        for (int k = 0; k < n; ++k) {
            for (int i2 = 0; i2 < m; ++i2) {
                int o;
                int p;
                int q;
                int r;
                float f = 0.0f;
                float g = 0.0f;
                float h = 0.0f;
                float f2 = 0.0f;
                float g2 = 0.0f;
                float h2 = 0.0f;
                float f3 = 0.0f;
                for (r = 0; r < 4; ++r) {
                    int t = Math.min(i - 1, i2 * 2 + (r & 1));
                    int u = Math.min(j - 1, k * 2 + (r >> 1));
                    int v = ints[u * i + t];
                    float g3 = (float) (v >>> 24) / 255.0f;
                    f += g3;
                    g += (float) (v >> 16 & 0xFF) * g3;
                    h += (float) (v >> 8 & 0xFF) * g3;
                    f2 += (float) (v & 0xFF) * g3;
                    g2 += (float) (v >> 16 & 0xFF);
                    h2 += (float) (v >> 8 & 0xFF);
                    f3 += (float) (v & 0xFF);
                }
                r = Math.round(f / 4.0f * 255.0f);
                if (f > 1.0E-4f) {
                    q = Math.round(g / f);
                    p = Math.round(h / f);
                    o = Math.round(f2 / f);
                } else {
                    q = Math.round(g2 / 4.0f);
                    p = Math.round(h2 / 4.0f);
                    o = Math.round(f3 / 4.0f);
                }
                ints2[k * m + i2] =
                        Colors.clamp(r) << 24
                                | Colors.clamp(q) << 16
                                | Colors.clamp(p) << 8
                                | Colors.clamp(o);
            }
        }
        return ints2;
    }

    private static int pow2(int i) {
        int j;
        for (j = 16; j < i; j <<= 1) {}
        return j;
    }
}
