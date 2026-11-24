package net.efn.client.module.impl.render.character.gltf;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

@Environment(EnvType.CLIENT)
final class TextureAtlas {

    private static final int SIZE = 2048;

    private static final int PAD = 4;

    private static final int MAX_SIDE = 8192;

    final int width;

    final int height;

    private final int[] pixels;

    private final Map<Integer, float[]> regions = new HashMap<>();

    private final Map<Integer, int[]> tiles = new HashMap<>();

    private TextureAtlas(int width2, int height2) {
        this.width = width2;
        this.height = height2;
        this.pixels = new int[width2 * height2];
    }

    static TextureAtlas pack(
            GltfFile gltfFile, Map<Integer, float[]> map, ImageDecoder imageDecoder)
            throws IOException {
        LinkedHashMap map2 = new LinkedHashMap();
        HashMap map3 = new HashMap();
        HashMap map4 = new HashMap();
        for (Entry entry : map.entrySet()) {
            float[] entry2 = (float[]) entry.getValue();
            int i = (int) Math.floor(entry2[0] + 0.001F);
            int j = (int) Math.floor(entry2[1] + 0.001F);
            int k = Math.min(8, Math.max(1, (int) Math.ceil(entry2[2] - 0.001F) - i));
            int m = Math.min(8, Math.max(1, (int) Math.ceil(entry2[3] - 0.001F) - j));
            ImageDecoder.Picture picture =
                    tile(baseColor(gltfFile, (Integer) entry.getKey(), imageDecoder), k, m);
            map4.put((Integer) entry.getKey(), new int[] {i, j, k, m});
            float f = (entry2[0] - i) / k;
            float g = (entry2[1] - j) / m;
            float h = (entry2[2] - i) / k;
            float f2 = (entry2[3] - j) / m;
            int n = clamp((int) Math.floor(f * picture.width()) - 2, 0, picture.width() - 1);
            int o = clamp((int) Math.floor(g * picture.height()) - 2, 0, picture.height() - 1);
            int p = clamp((int) Math.ceil(h * picture.width()) + 2, n + 1, picture.width());
            int q = clamp((int) Math.ceil(f2 * picture.height()) + 2, o + 1, picture.height());
            map2.put((Integer) entry.getKey(), crop(picture, n, o, p, q));
            map3.put(
                    (Integer) entry.getKey(),
                    new float[] {
                        (float) n / picture.width(),
                        (float) o / picture.height(),
                        (float) p / picture.width(),
                        (float) q / picture.height()
                    });
        }
        double d = 0.0;
        for (ImageDecoder.Picture picture2 : (Iterable<ImageDecoder.Picture>) map2.values()) {
            d += (double) (picture2.width() + 8) * (picture2.height() + 8);
        }
        double e = Math.min(1.0, Math.sqrt(3858759.68 / Math.max(1.0, d)));
        TextureAtlas textureAtlas;
        while ((textureAtlas = tryPack(map2, map3, e)) == null) {
            e *= 0.93;
        }
        textureAtlas.tiles.putAll(map4);
        return textureAtlas;
    }

    private static ImageDecoder.Picture baseColor(
            GltfFile gltfFile, int i, ImageDecoder imageDecoder) throws IOException {
        byte[] bytes = gltfFile.baseColorImage(i);
        ImageDecoder.Picture picture =
                bytes == null
                        ? new ImageDecoder.Picture(4, 4, filled(16, -1))
                        : imageDecoder.decode(bytes);
        float[] floats = gltfFile.baseColorFactor(i);
        if (floats[0] == 1.0F && floats[1] == 1.0F && floats[2] == 1.0F && floats[3] == 1.0F) {
            return picture;
        } else {
            for (int j = 0; j < 3; j++) {
                floats[j] = (float) Math.pow(floats[j], 0.45454545454545453);
            }
            int[] ints = new int[picture.argb().length];
            for (int k = 0; k < ints.length; k++) {
                int m = picture.argb()[k];
                int n = Math.round((m >>> 24) * floats[3]);
                int o = Math.round((m >> 16 & 0xFF) * floats[0]);
                int p = Math.round((m >> 8 & 0xFF) * floats[1]);
                int q = Math.round((m & 0xFF) * floats[2]);
                ints[k] = n << 24 | o << 16 | p << 8 | q;
            }
            return new ImageDecoder.Picture(picture.width(), picture.height(), ints);
        }
    }

    private static int[] filled(int i, int j) {
        int[] ints = new int[i];
        Arrays.fill(ints, j);
        return ints;
    }

    private static ImageDecoder.Picture tile(ImageDecoder.Picture picture, int i, int j) {
        if (i == 1 && j == 1) {
            return picture;
        } else {
            if (picture.width() * i > 8192 || picture.height() * j > 8192) {
                picture =
                        resize(
                                picture,
                                Math.max(1, Math.min(picture.width(), 8192 / i)),
                                Math.max(1, Math.min(picture.height(), 8192 / j)));
            }
            int k = picture.width() * i;
            int m = picture.height() * j;
            int[] ints = new int[k * m];
            for (int n = 0; n < m; n++) {
                for (int o = 0; o < k; o++) {
                    ints[n * k + o] =
                            picture.argb()[
                                    n % picture.height() * picture.width() + o % picture.width()];
                }
            }
            return new ImageDecoder.Picture(k, m, ints);
        }
    }

    private static ImageDecoder.Picture crop(
            ImageDecoder.Picture picture, int i, int j, int k, int m) {
        int n = k - i;
        int o = m - j;
        int[] ints = new int[n * o];
        for (int p = 0; p < o; p++) {
            System.arraycopy(picture.argb(), (j + p) * picture.width() + i, ints, p * n, n);
        }
        return new ImageDecoder.Picture(n, o, ints);
    }

    private static TextureAtlas tryPack(
            Map<Integer, ImageDecoder.Picture> map, Map<Integer, float[]> map2, double d) {
        ArrayList list = new ArrayList(map.keySet());
        list.sort(
                (integer, integer2) ->
                        Integer.compare(
                                ((ImageDecoder.Picture) map.get(integer2)).height(),
                                ((ImageDecoder.Picture) map.get(integer)).height()));
        int i = 0;
        int j = 0;
        int k = 0;
        int m = 0;
        HashMap map3 = new HashMap();
        for (int n : (Iterable<Integer>) list) {
            ImageDecoder.Picture picture = (ImageDecoder.Picture) map.get(n);
            int o = Math.max(1, (int) Math.round(picture.width() * d));
            int p = Math.max(1, (int) Math.round(picture.height() * d));
            if (i + o + 8 > 2048) {
                i = 0;
                j += k;
                k = 0;
            }
            if (o + 8 > 2048 || j + p + 8 > 2048) {
                return null;
            }
            map3.put(n, new int[] {i + 4, j + 4, o, p});
            k = Math.max(k, p + 8);
            m = Math.max(m, i += o + 8);
        }
        TextureAtlas textureAtlas = new TextureAtlas(pow2(m), pow2(j + k));
        for (int q : (Iterable<Integer>) list) {
            int[] ints = (int[]) map3.get(q);
            ImageDecoder.Picture picture2 =
                    resize((ImageDecoder.Picture) map.get(q), ints[2], ints[3]);
            for (int r = -4; r < ints[3] + 4; r++) {
                for (int t = -4; t < ints[2] + 4; t++) {
                    int u = clamp(t, 0, ints[2] - 1);
                    int v = clamp(r, 0, ints[3] - 1);
                    textureAtlas.pixels[(ints[1] + r) * textureAtlas.width + ints[0] + t] =
                            picture2.argb()[v * ints[2] + u];
                }
            }
            float[] floats = (float[]) map2.get(q);
            textureAtlas.regions.put(
                    q,
                    new float[] {
                        floats[0], floats[1], floats[2], floats[3], ints[0], ints[1], ints[2],
                        ints[3]
                    });
        }
        return textureAtlas;
    }

    float[] map(int i, float f, float g) {
        float[] region = this.regions.get(i);
        int[] tile = this.tiles.get(i);
        float h = (f - tile[0]) / tile[2];
        float f2 = (g - tile[1]) / tile[3];
        float g2 = (h - region[0]) / (region[2] - region[0]);
        float h2 = (f2 - region[1]) / (region[3] - region[1]);
        return new float[] {
            (region[4] + g2 * region[6]) / this.width, (region[5] + h2 * region[7]) / this.height
        };
    }

    byte[] packed() throws IOException {
        byte[] bytes = new byte[this.pixels.length * 4];
        for (int i = 0; i < this.pixels.length; i++) {
            int j = i % this.width == 0 ? 0 : this.pixels[i - 1];
            for (int k = 0; k < 4; k++) {
                bytes[i * 4 + k] = (byte) ((this.pixels[i] >>> 24 - k * 8) - (j >>> 24 - k * 8));
            }
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflaterOutputStream =
                new DeflaterOutputStream(byteArrayOutputStream, new Deflater(9))) {
            deflaterOutputStream.write(bytes);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private static ImageDecoder.Picture resize(ImageDecoder.Picture picture, int i, int j) {
        ImageDecoder.Picture picture2 = picture;
        while (picture2.width() / 2 >= i && picture2.height() / 2 >= j) {
            picture2 = half(picture2);
        }
        return picture2.width() == i && picture2.height() == j
                ? picture2
                : bilinear(picture2, i, j);
    }

    private static ImageDecoder.Picture half(ImageDecoder.Picture picture) {
        int i = picture.width() / 2;
        int j = picture.height() / 2;
        int[] ints = new int[i * j];
        for (int k = 0; k < j; k++) {
            for (int m = 0; m < i; m++) {
                int[] ints2 = picture.argb();
                int n = k * 2 * picture.width() + m * 2;
                ints[k * i + m] =
                        average(
                                ints2[n],
                                ints2[n + 1],
                                ints2[n + picture.width()],
                                ints2[n + picture.width() + 1]);
            }
        }
        return new ImageDecoder.Picture(i, j, ints);
    }

    private static int average(int i, int j, int k, int m) {
        int n = 0;
        for (int o = 0; o < 32; o += 8) {
            int p = (i >>> o & 0xFF) + (j >>> o & 0xFF) + (k >>> o & 0xFF) + (m >>> o & 0xFF);
            n |= (p + 2) / 4 << o;
        }
        return n;
    }

    private static ImageDecoder.Picture bilinear(ImageDecoder.Picture picture, int i, int j) {
        int[] ints = new int[i * j];
        int width2 = picture.width();
        int height2 = picture.height();
        for (int n = 0; n < j; n++) {
            float f = Math.max(0.0F, (n + 0.5F) * height2 / j - 0.5F);
            int k = Math.min(height2 - 1, (int) f);
            int m = Math.min(height2 - 1, k + 1);
            float g = f - k;
            for (int q = 0; q < i; q++) {
                float h = Math.max(0.0F, (q + 0.5F) * width2 / i - 0.5F);
                int o = Math.min(width2 - 1, (int) h);
                int p = Math.min(width2 - 1, o + 1);
                float f2 = h - o;
                int[] ints2 = picture.argb();
                int r = 0;
                for (int v = 0; v < 32; v += 8) {
                    float g2 =
                            (ints2[k * width2 + o] >>> v & 0xFF) * (1.0F - f2)
                                    + (ints2[k * width2 + p] >>> v & 0xFF) * f2;
                    float h2 =
                            (ints2[m * width2 + o] >>> v & 0xFF) * (1.0F - f2)
                                    + (ints2[m * width2 + p] >>> v & 0xFF) * f2;
                    r |= Math.round(g2 * (1.0F - g) + h2 * g) << v;
                }
                ints[n * i + q] = r;
            }
        }
        return new ImageDecoder.Picture(i, j, ints);
    }

    private static int clamp(int i, int j, int k) {
        return Math.max(j, Math.min(k, i));
    }

    private static int pow2(int i) {
        int j = 16;
        while (j < i) {
            j <<= 1;
        }
        return j;
    }
}
