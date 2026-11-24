package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.zip.InflaterInputStream;

@Environment(EnvType.CLIENT)
public final class BakedModel {

    public static final int MAGIC = 1095648072;

    public static final int VERSION = 3;

    private static final int SHIPPED_VERSION = 4;

    private BakedModel() {}

    static CharacterModel load(BakedDesign bakedDesign, Detail detail) {
        long l = System.nanoTime();
        try {
            CharacterModel characterModel;
            try (InputStream inValue = bakedDesign.open()) {
                DataInputStream dataInputStream =
                        new DataInputStream(new BufferedInputStream(inValue));
                int i = dataInputStream.readInt() == 1095648072 ? dataInputStream.readInt() : -1;
                if (i != 3 && i != 4) {
                    throw new IllegalStateException(
                            bakedDesign.id() + ": not a version 3 baked character");
                }
                int j = 0;
                int k = 0;
                int[] ints = null;
                if (i == 4) {
                    j = dataInputStream.readInt();
                    k = dataInputStream.readInt();
                    byte[] bytes = new byte[j * k * 4];
                    dataInputStream.readFully(bytes);
                    ints = unfilter(bytes, j, k);
                }
                float f = dataInputStream.readFloat();
                float g = dataInputStream.readFloat();
                float h = dataInputStream.readFloat();
                float f2 = dataInputStream.readFloat();
                boolean bl = dataInputStream.readBoolean();
                boolean bl2 = dataInputStream.readBoolean();
                int m = dataInputStream.readInt();
                int n = Math.min(detail.ordinal(), m - 1);
                PartMesh[] partMeshs = new PartMesh[dataInputStream.readInt()];
                for (int o = 0; o < partMeshs.length; o++) {
                    Bone bone = Bone.values()[dataInputStream.readByte()];
                    float g2 = dataInputStream.readFloat();
                    float h2 = dataInputStream.readFloat();
                    float f3 = dataInputStream.readFloat();
                    float g3 = dataInputStream.readFloat();
                    float h3 = dataInputStream.readFloat();
                    float f4 = dataInputStream.readFloat();
                    float[] floats = new float[6];
                    for (int p = 0; p < 6; p++) {
                        floats[p] = dataInputStream.readFloat();
                    }
                    float g4 = dataInputStream.readFloat();
                    float h4 = dataInputStream.readFloat();
                    boolean bl3 = dataInputStream.readBoolean();
                    float f5 = 0.0F;
                    float g5 = 0.0F;
                    float h5 = 0.0F;
                    if (bl3) {
                        f5 = dataInputStream.readFloat() - g2;
                        g5 = dataInputStream.readFloat() - h2;
                        h5 = dataInputStream.readFloat() - f3;
                    }
                    Surface surface = null;
                    Surface surface2 = null;
                    for (int q = 0; q < m; q++) {
                        Surface surface3 =
                                surface(dataInputStream, floats, g2, h2, f3, f2, bl2, bl3, q == n);
                        Surface surface4 =
                                surface(dataInputStream, floats, g2, h2, f3, f2, bl2, bl3, q == n);
                        if (q == n) {
                            surface = surface3;
                            surface2 = surface4;
                        }
                    }
                    partMeshs[o] =
                            new PartMesh(
                                    bone.name(),
                                    bone,
                                    -1,
                                    false,
                                    g2,
                                    h2,
                                    f3,
                                    g3,
                                    h3,
                                    f4,
                                    null,
                                    surface,
                                    null,
                                    surface2,
                                    -1,
                                    g4,
                                    h4,
                                    bl3,
                                    f5,
                                    g5,
                                    h5,
                                    false,
                                    0.0F,
                                    0.0F,
                                    0.0F);
                }
                if (i == 3) {
                    j = dataInputStream.readInt();
                    k = dataInputStream.readInt();
                    byte[] bytes2 = new byte[dataInputStream.readInt()];
                    dataInputStream.readFully(bytes2);
                    ints = pixels(bytes2, j, k);
                }
                long l2 = (System.nanoTime() - l) / 1000000L;
                characterModel =
                        new CharacterModel(
                                bakedDesign.id(),
                                bakedDesign.name(),
                                detail,
                                1.0F,
                                partMeshs,
                                0,
                                j,
                                k,
                                Atlas.mips(ints, j, k),
                                0.0F,
                                0.0F,
                                f,
                                g,
                                h,
                                bl,
                                l2);
            }
            return characterModel;
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    bakedDesign.name() + ": " + exception.getMessage(), exception);
        }
    }

    private static int[] pixels(byte[] bytes, int i, int j) throws IOException {
        byte[] bytes2 = new byte[i * j * 4];
        try (InflaterInputStream inflaterInputStream =
                new InflaterInputStream(new ByteArrayInputStream(bytes))) {
            int nBytes = inflaterInputStream.readNBytes(bytes2, 0, bytes2.length);
            if (nBytes != bytes2.length) {
                throw new IOException("atlas is truncated");
            }
        }
        return unfilter(bytes2, i, j);
    }

    private static int[] unfilter(byte[] bytes, int i, int j) {
        int[] ints = new int[i * j];
        for (int k = 0; k < ints.length; k++) {
            int m = k % i == 0 ? 0 : ints[k - 1];
            int n = 0;
            for (int o = 0; o < 4; o++) {
                int p = 24 - o * 8;
                n |= (bytes[k * 4 + o] + (m >>> p) & 0xFF) << p;
            }
            ints[k] = n;
        }
        return ints;
    }

    private static Surface surface(
            DataInputStream dataInputStream,
            float[] floats,
            float f,
            float g,
            float h,
            float f2,
            boolean bl,
            boolean bl2,
            boolean bl3)
            throws IOException {
        int i = dataInputStream.readInt();
        if (!bl3) {
            dataInputStream.skipNBytes(i * (13L + (bl ? 3L : 0L) + (bl2 ? 1L : 0L)));
            dataInputStream.skipNBytes(dataInputStream.readInt() * (i > 65535 ? 12L : 6L));
            return null;
        } else {
            float[] floats2 = new float[i * 3];
            for (int k = 0; k < floats2.length; k++) {
                int j = k % 3;
                floats2[k] =
                        floats[j]
                                + dataInputStream.readChar()
                                        / 65535.0F
                                        * (floats[j + 3] - floats[j]);
            }
            byte[] bytes = new byte[i * 3];
            dataInputStream.readFully(bytes);
            float[] floats3 = new float[i * 2];
            for (int n = 0; n < floats3.length; n++) {
                floats3[n] = dataInputStream.readChar() / 65535.0F;
            }
            int[] ints = new int[i];
            for (int o = 0; o < i; o++) {
                ints[o] =
                        bl
                                ? 0xFF000000
                                        | dataInputStream.readUnsignedByte() << 16
                                        | dataInputStream.readUnsignedByte() << 8
                                        | dataInputStream.readUnsignedByte()
                                : -1;
            }
            float[] floats4 = null;
            if (bl2) {
                floats4 = new float[i];
                for (int p = 0; p < i; p++) {
                    floats4[p] = dataInputStream.readUnsignedByte() / 255.0F;
                }
            }
            int m = dataInputStream.readInt();
            int[] ints2 = new int[m * 3];
            for (int r = 0; r < ints2.length; r++) {
                ints2[r] = i > 65535 ? dataInputStream.readInt() : dataInputStream.readChar();
            }
            if (i != 0 && m != 0) {
                Surface.Builder builder = new Surface.Builder();
                for (int t = 0; t < i; t++) {
                    float g2 = bytes[t * 3] / 127.0F;
                    float h2 = bytes[t * 3 + 1] / 127.0F;
                    float f3 = bytes[t * 3 + 2] / 127.0F;
                    float g3 = (float) Math.sqrt(g2 * g2 + h2 * h2 + f3 * f3);
                    if (g3 > 1.0E-6F) {
                        g2 /= g3;
                        h2 /= g3;
                        f3 /= g3;
                    }
                    builder.vertex(
                            floats2[t * 3] - f,
                            floats2[t * 3 + 1] - g,
                            floats2[t * 3 + 2] - h,
                            g2,
                            h2,
                            f3,
                            floats3[t * 2],
                            floats3[t * 2 + 1],
                            ints[t],
                            false,
                            f2);
                }
                CharacterBuilder.pair(ints2, m, i, builder);
                Surface surface = builder.build();
                if (surface != null && floats4 != null) {
                    for (float h3 : floats4) {
                        if (h3 > 0.0F) {
                            surface.joint = floats4;
                            break;
                        }
                    }
                }
                return surface;
            } else {
                return null;
            }
        }
    }
}
