package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.ByteBuffer;

@Environment(EnvType.CLIENT)
public final class ImageDownsampler {

    private ImageDownsampler() {}

    public static int[] dimensions(int i, int j, int k) {
        if (i > 0 && j > 0 && k > 0) {
            double d = Math.min(1.0, (double) k / Math.max(i, j));
            return new int[] {
                Math.max(1, (int) Math.round(i * d)), Math.max(1, (int) Math.round(j * d))
            };
        } else {
            throw new IllegalArgumentException("Image dimensions");
        }
    }

    public static void rgba(
            ByteBuffer byteBuffer, int i, int j, ByteBuffer byteBuffer2, int k, int m) {
        if (k > 0 && m > 0 && k <= i && m <= j) {
            double d = (double) i / k;
            double e = (double) j / m;
            for (int n = 0; n < m; n++) {
                double d2 = n * e;
                double e2 = (n + 1) * e;
                for (int o = 0; o < k; o++) {
                    double d3 = o * d;
                    double e3 = (o + 1) * d;
                    double d4 = 0.0;
                    double e4 = 0.0;
                    double d5 = 0.0;
                    double e5 = 0.0;
                    for (int p = (int) d2; p < Math.min(j, (int) Math.ceil(e2)); p++) {
                        double d6 = Math.min(e2, p + 1.0) - Math.max(d2, (double) p);
                        for (int q = (int) d3; q < Math.min(i, (int) Math.ceil(e3)); q++) {
                            double e6 = d6 * (Math.min(e3, q + 1.0) - Math.max(d3, (double) q));
                            int r = (p * i + q) * 4;
                            double d7 = (byteBuffer.get(r + 3) & 255) * e6;
                            d4 += d7;
                            e4 += (byteBuffer.get(r) & 255) * d7;
                            d5 += (byteBuffer.get(r + 1) & 255) * d7;
                            e5 += (byteBuffer.get(r + 2) & 255) * d7;
                        }
                    }
                    byteBuffer2.put((byte) (d4 == 0.0 ? 0L : Math.round(e4 / d4)));
                    byteBuffer2.put((byte) (d4 == 0.0 ? 0L : Math.round(d5 / d4)));
                    byteBuffer2.put((byte) (d4 == 0.0 ? 0L : Math.round(e5 / d4)));
                    byteBuffer2.put((byte) Math.round(d4 / (d * e)));
                }
            }
            byteBuffer2.flip();
        } else {
            throw new IllegalArgumentException("Only image reduction is supported");
        }
    }
}
