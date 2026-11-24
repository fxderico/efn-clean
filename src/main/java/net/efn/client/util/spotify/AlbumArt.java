package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Environment(EnvType.CLIENT)
public final class AlbumArt {

    private static final int MAX_SOURCE_SIDE = 4096;

    private final int size;

    private final byte[] rgba;

    private AlbumArt(int size2, byte[] rgba2) {
        this.size = size2;
        this.rgba = rgba2;
    }

    public static AlbumArt decode(byte[] bytes, int i) {
        if (bytes != null && bytes.length != 0 && i > 0) {
            ByteBuffer byteBuffer = MemoryUtil.memAlloc(bytes.length);
            ByteBuffer byteBuffer2 = null;
            Object object;
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                byteBuffer.put(bytes).flip();
                IntBuffer intBuffer = memoryStack.mallocInt(1);
                IntBuffer intBuffer2 = memoryStack.mallocInt(1);
                IntBuffer intBuffer3 = memoryStack.mallocInt(1);
                if (!STBImage.stbi_info_from_memory(
                        byteBuffer, intBuffer, intBuffer2, intBuffer3)) {
                    return null;
                }
                if (intBuffer.get(0) > 0
                        && intBuffer2.get(0) > 0
                        && intBuffer.get(0) <= 4096
                        && intBuffer2.get(0) <= 4096) {
                    byteBuffer2 =
                            STBImage.stbi_load_from_memory(
                                    byteBuffer, intBuffer, intBuffer2, intBuffer3, 4);
                    if (byteBuffer2 == null) {
                        return null;
                    }
                    return crop(byteBuffer2, intBuffer.get(0), intBuffer2.get(0), i);
                }
                object = null;
            } finally {
                if (byteBuffer2 != null) {
                    STBImage.stbi_image_free(byteBuffer2);
                }
                MemoryUtil.memFree(byteBuffer);
            }
            return (AlbumArt) object;
        } else {
            return null;
        }
    }

    static AlbumArt crop(ByteBuffer byteBuffer, int i, int j, int k) {
        int m = Math.min(i, j);
        int n = (i - m) / 2;
        int o = (j - m) / 2;
        int p = Math.min(m, k);
        byte[] bytes = new byte[p * p * 4];
        int q = 0;
        for (int r = 0; r < p; r++) {
            int t = o + (int) ((long) r * m / p);
            int u = Math.max(t + 1, o + (int) ((long) (r + 1) * m / p));
            for (int v = 0; v < p; v++) {
                int w = n + (int) ((long) v * m / p);
                int i2 = Math.max(w + 1, n + (int) ((long) (v + 1) * m / p));
                long l = 0L;
                long l2 = 0L;
                long l3 = 0L;
                long l4 = 0L;
                for (int j2 = t; j2 < u; j2++) {
                    int k2 = (j2 * i + w) * 4;
                    for (int m2 = w; m2 < i2; k2 += 4) {
                        l += byteBuffer.get(k2) & 255;
                        l2 += byteBuffer.get(k2 + 1) & 255;
                        l3 += byteBuffer.get(k2 + 2) & 255;
                        l4 += byteBuffer.get(k2 + 3) & 255;
                        m2++;
                    }
                }
                long l5 = (long) (u - t) * (i2 - w);
                bytes[q++] = (byte) ((l + l5 / 2L) / l5);
                bytes[q++] = (byte) ((l2 + l5 / 2L) / l5);
                bytes[q++] = (byte) ((l3 + l5 / 2L) / l5);
                bytes[q++] = (byte) ((l4 + l5 / 2L) / l5);
            }
        }
        return new AlbumArt(p, bytes);
    }

    public int size() {
        return this.size;
    }

    public int byteSize() {
        return this.rgba.length;
    }

    public void copyTo(ByteBuffer byteBuffer) {
        byteBuffer.put(this.rgba);
    }
}
