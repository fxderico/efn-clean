package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.util.Arrays;

@Environment(EnvType.CLIENT)
final class GifDecoder {

    private static final int MAX_CANVAS = 4096;

    private static final int IMAGE = 44;

    private static final int EXTENSION = 33;

    private static final int GRAPHIC_CONTROL = 249;

    private static final int DISPOSE_BACKGROUND = 2;

    private static final int DISPOSE_PREVIOUS = 3;

    private static final int MAX_CODES = 4096;

    private final byte[] data;

    private final int width;

    private final int height;

    private final int[] globalPalette;

    private final int blocksStart;

    private final int frameCount;

    private int pos;

    private GifDecoder(byte[] data2) throws IOException {
        this.data = data2;
        this.pos = 6;
        this.width = this.u16();
        this.height = this.u16();
        int u8 = this.u8();
        this.pos += 2;
        this.globalPalette = (u8 & 128) != 0 ? this.palette(u8) : null;
        this.blocksStart = this.pos;
        if (this.width == 0 || this.height == 0) {
            throw new IOException("GIF has an empty canvas");
        } else if (this.width <= 4096 && this.height <= 4096) {
            this.frameCount = this.countFrames();
            if (this.frameCount == 0) {
                throw new IOException("GIF has no frames");
            }
        } else {
            throw new IOException(
                    "GIF is " + this.width + "x" + this.height + ", larger than 4096 px");
        }
    }

    static GifDecoder open(byte[] bytes) throws IOException {
        if (bytes.length >= 13 && bytes[0] == 71 && bytes[1] == 73 && bytes[2] == 70) {
            return new GifDecoder(bytes);
        } else {
            throw new IOException("Not a GIF file");
        }
    }

    int width() {
        return this.width;
    }

    int height() {
        return this.height;
    }

    int frameCount() {
        return this.frameCount;
    }

    void decode(int i, int j, int k, GifDecoder.Sink sink) {
        int[] ints = new int[this.width * this.height];
        int[] ints2 = null;
        int m = 0;
        int n = 0;
        int o = -1;
        int p = 0;
        int q = 0;
        int r = 0;
        int t = 0;
        int u = 0;
        int v = 0;
        this.pos = this.blocksStart;
        while (v < k && this.pos < this.data.length) {
            int u8 = this.u8();
            if (u8 == 33) {
                if (this.u8() == 249) {
                    int u82 = this.u8();
                    int w = this.pos + u82;
                    int u83 = this.u8();
                    m = u83 >> 2 & 7;
                    n = this.u16();
                    int u84 = this.u8();
                    o = (u83 & 1) != 0 ? u84 : -1;
                    this.pos = w;
                }
                this.skipSubBlocks();
            } else {
                if (u8 != 44) {
                    return;
                }
                int u16 = this.u16();
                int u162 = this.u16();
                int u163 = this.u16();
                int u164 = this.u16();
                int u85 = this.u8();
                int[] ints3 = (u85 & 128) != 0 ? this.palette(u85) : this.globalPalette;
                boolean bl = (u85 & 64) != 0;
                int u86 = this.u8();
                byte[] subBlocks = this.readSubBlocks();
                if ((long) u163 * u164 > (long) this.width * this.height) {
                    return;
                }
                if (p == 2) {
                    this.clear(ints, q, r, t, u);
                } else if (p == 3 && ints2 != null) {
                    System.arraycopy(ints2, 0, ints, 0, ints.length);
                }
                if (m == 3) {
                    if (ints2 == null) {
                        ints2 = new int[ints.length];
                    }
                    System.arraycopy(ints, 0, ints2, 0, ints.length);
                }
                byte[] bytes = new byte[u163 * u164];
                int lzw = lzw(subBlocks, u86, bytes);
                if (ints3 != null) {
                    this.draw(ints, bytes, lzw, ints3, o, u16, u162, u163, u164, bl);
                }
                int[] ints4 =
                        i == this.width && j == this.height
                                ? (int[]) ints.clone()
                                : downscale(ints, this.width, this.height, i, j);
                if (!sink.accept(new GifDecoder.Frame(ints4, i, j, delayMs(n)))) {
                    return;
                }
                v++;
                p = m;
                q = u16;
                r = u162;
                t = u163;
                u = u164;
                m = 0;
                n = 0;
                o = -1;
            }
        }
    }

    private static int delayMs(int i) {
        return i <= 1 ? 100 : i * 10;
    }

    private static int lzw(byte[] bytes, int i, byte[] bytes2) {
        if (i >= 1 && i <= 8) {
            int j = 1 << i;
            int k = j + 1;
            short[] shorts = new short[4096];
            byte[] bytes3 = new byte[4096];
            byte[] bytes4 = new byte[4097];
            for (int m = 0; m < j; m++) {
                bytes3[m] = (byte) m;
            }
            int n = i + 1;
            int o = j + 2;
            int p = -1;
            byte b = 0;
            int q = 0;
            int r = 0;
            int t = 0;
            int u = 0;
            while (u < bytes2.length) {
                while (q < n) {
                    if (t >= bytes.length) {
                        return u;
                    }
                    r |= (bytes[t++] & 255) << q;
                    q += 8;
                }
                int v = r & (1 << n) - 1;
                r >>>= n;
                q -= n;
                if (v == j) {
                    n = i + 1;
                    o = j + 2;
                    p = -1;
                } else {
                    if (v == k) {
                        break;
                    }
                    if (p == -1) {
                        if (v >= j) {
                            break;
                        }
                        b = bytes3[v];
                        bytes2[u++] = b;
                        p = v;
                    } else {
                        int w = v;
                        int i2 = 0;
                        if (v >= o) {
                            if (v > o) {
                                break;
                            }
                            bytes4[i2++] = b;
                            v = p;
                        }
                        while (v >= j) {
                            bytes4[i2++] = bytes3[v];
                            v = shorts[v];
                        }
                        b = bytes3[v];
                        bytes4[i2++] = b;
                        if (o < 4096) {
                            shorts[o] = (short) p;
                            bytes3[o] = b;
                            if (++o == 1 << n && n < 12) {
                                n++;
                            }
                        }
                        p = w;
                        while (i2 > 0 && u < bytes2.length) {
                            bytes2[u++] = bytes4[--i2];
                        }
                    }
                }
            }
            return u;
        } else {
            return 0;
        }
    }

    static int[] downscale(int[] ints, int i, int j, int k, int m) {
        int[] ints2 = new int[k * m];
        for (int n = 0; n < m; n++) {
            int o = (int) ((long) n * j / m);
            int p = Math.max(o + 1, (int) ((long) (n + 1) * j / m));
            for (int q = 0; q < k; q++) {
                int r = (int) ((long) q * i / k);
                int t = Math.max(r + 1, (int) ((long) (q + 1) * i / k));
                long l = 0L;
                long l2 = 0L;
                long l3 = 0L;
                long l4 = 0L;
                for (int u = o; u < p; u++) {
                    int v = u * i;
                    for (int w = r; w < t; w++) {
                        int i2 = ints[v + w];
                        int j2 = i2 >>> 24;
                        l += j2;
                        l2 += (long) (i2 >> 16 & 0xFF) * j2;
                        l3 += (long) (i2 >> 8 & 0xFF) * j2;
                        l4 += (long) (i2 & 0xFF) * j2;
                    }
                }
                if (l != 0L) {
                    int k2 = (p - o) * (t - r);
                    ints2[n * k + q] =
                            (int) (l / k2) << 24
                                    | (int) (l2 / l) << 16
                                    | (int) (l3 / l) << 8
                                    | (int) (l4 / l);
                }
            }
        }
        return ints2;
    }

    private void draw(
            int[] ints,
            byte[] bytes,
            int i,
            int[] ints2,
            int j,
            int k,
            int m,
            int n,
            int o,
            boolean bl) {
        int[] ints3 = rowOrder(o, bl);
        int p = 0;
        for (int q = 0; q < o && p < i; q++) {
            int r = Math.min(i, p + n);
            int t = m + ints3[q];
            if (t >= this.height) {
                p = r;
            } else {
                int u = t * this.width;
                for (int v = k; p < r; v++) {
                    int w = bytes[p] & 255;
                    if (w != j && w < ints2.length && v < this.width) {
                        ints[u + v] = ints2[w];
                    }
                    p++;
                }
            }
        }
    }

    private static int[] rowOrder(int i, boolean bl) {
        int[] ints = new int[i];
        if (!bl) {
            int j = 0;
            while (j < i) {
                ints[j] = j++;
            }
            return ints;
        } else {
            int[] ints2 = new int[] {0, 4, 2, 1};
            int[] ints3 = new int[] {8, 8, 4, 2};
            int k = 0;
            for (int m = 0; m < 4; m++) {
                for (int n = ints2[m]; n < i; n += ints3[m]) {
                    ints[k++] = n;
                }
            }
            return ints;
        }
    }

    private void clear(int[] ints, int i, int j, int k, int m) {
        int n = Math.min(this.width, i + k);
        int o = Math.min(this.height, j + m);
        if (i < n) {
            for (int p = j; p < o; p++) {
                Arrays.fill(ints, p * this.width + i, p * this.width + n, 0);
            }
        }
    }

    private int countFrames() {
        int i = 0;
        this.pos = this.blocksStart;
        while (this.pos < this.data.length) {
            int u8 = this.u8();
            if (u8 == 44) {
                i++;
                this.pos += 8;
                int u82 = this.u8();
                if ((u82 & 128) != 0) {
                    this.pos += 3 * (2 << (u82 & 7));
                }
                this.pos++;
                this.skipSubBlocks();
            } else {
                if (u8 != 33) {
                    break;
                }
                this.pos++;
                this.skipSubBlocks();
            }
        }
        return i;
    }

    private int[] palette(int i) {
        int[] ints = new int[2 << (i & 7)];
        for (int j = 0; j < ints.length; j++) {
            ints[j] = 0xFF000000 | this.u8() << 16 | this.u8() << 8 | this.u8();
        }
        return ints;
    }

    private void skipSubBlocks() {
        while (this.pos < this.data.length) {
            int u8 = this.u8();
            if (u8 == 0) {
                return;
            }
            this.pos += u8;
        }
    }

    private byte[] readSubBlocks() {
        int i = 0;
        int pos2 = this.pos;
        int j;
        while (pos2 < this.data.length && (j = this.data[pos2] & 255) != 0) {
            i += j;
            pos2 += 1 + j;
        }
        byte[] bytes = new byte[i];
        int k = 0;
        int u8;
        while (this.pos < this.data.length && (u8 = this.u8()) != 0) {
            int m = Math.min(u8, this.data.length - this.pos);
            System.arraycopy(this.data, this.pos, bytes, k, m);
            k += m;
            this.pos += u8;
        }
        return k == i ? bytes : Arrays.copyOf(bytes, k);
    }

    private int u8() {
        return this.pos < this.data.length ? this.data[this.pos++] & 0xFF : 0;
    }

    private int u16() {
        return this.u8() | this.u8() << 8;
    }

    @Environment(EnvType.CLIENT)
    record Frame(int[] argb, int width, int height, int delayMs) {}

    @Environment(EnvType.CLIENT)
    interface Sink {

        boolean accept(GifDecoder.Frame frame);
    }
}
