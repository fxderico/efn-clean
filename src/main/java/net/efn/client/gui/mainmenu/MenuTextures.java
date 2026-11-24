package net.efn.client.gui.mainmenu;

import net.efn.client.gui.ImageDownsampler;
import net.efn.client.gui.ThemeImages;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

@Environment(EnvType.CLIENT)
final class MenuTextures {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final Map<String, ThemeImages.Texture> TEXTURES = new HashMap<>();

    private static boolean optimized;

    private static void changeProfile() {
        TEXTURES.values().forEach(ThemeImages.get()::retireTexture);
        TEXTURES.clear();
        optimized = ThemeImages.get().optimizedImages();
    }

    private MenuTextures() {}

    static ThemeImages.Texture original(String string) {
        return cached(string, string, UnaryOperator.identity());
    }

    static ThemeImages.Texture mask(String string) {
        return cached(
                string + "#mask",
                string,
                pixels -> {
                    int[] ints = new int[pixels.argb().length];
                    for (int i = 0; i < ints.length; i++) {
                        ints[i] = pixels.argb()[i] & 0xFF000000 | 16777215;
                    }
                    return new MenuTextures.Pixels(pixels.width(), pixels.height(), ints);
                });
    }

    static ThemeImages.Texture blurred(String string, int i) {
        return cached(string + "#blur" + i, string, pixels -> blur(pixels, i));
    }

    private static ThemeImages.Texture cached(
            String string, String string2, UnaryOperator<MenuTextures.Pixels> unaryOperator) {
        boolean optimizedImages = ThemeImages.get().optimizedImages();
        if (optimized != optimizedImages) {
            changeProfile();
        }
        if (TEXTURES.containsKey(string)) {
            return TEXTURES.get(string);
        } else {
            ThemeImages.Texture texture = null;
            try (InputStream resourceAsStream = MenuTextures.class.getResourceAsStream(string2)) {
                if (resourceAsStream == null) {
                    LOGGER.warn("[EFN] Missing menu image {}", string2);
                } else {
                    MenuTextures.Pixels pixels = decode(resourceAsStream.readAllBytes(), string2);
                    if (pixels != null) {
                        pixels = unaryOperator.apply(pixels);
                        texture = upload(pixels.argb(), pixels.width(), pixels.height());
                    }
                }
            } catch (Exception exception) {
                LOGGER.warn("[EFN] Failed to load menu image {}", string, exception);
            }
            TEXTURES.put(string, texture);
            return texture;
        }
    }

    private static MenuTextures.Pixels blur(MenuTextures.Pixels pixels, int i) {
        int j = i * 2;
        int k = pixels.width() + j * 2;
        int m = pixels.height() + j * 2;
        float[] floats = new float[k * m];
        for (int n = 0; n < pixels.height(); n++) {
            for (int o = 0; o < pixels.width(); o++) {
                floats[(n + j) * k + o + j] =
                        (pixels.argb()[n * pixels.width() + o] >>> 24) / 255.0F;
            }
        }
        for (int p = 0; p < 3; p++) {
            floats = box(floats, k, m, i, true);
            floats = box(floats, k, m, i, false);
        }
        int[] ints = new int[k * m];
        for (int q = 0; q < ints.length; q++) {
            ints[q] = Math.round(Math.min(1.0F, floats[q]) * 255.0F) << 24 | 16777215;
        }
        return new MenuTextures.Pixels(k, m, ints);
    }

    private static float[] box(float[] floats, int i, int j, int k, boolean bl) {
        if (optimized) {
            return slidingBox(floats, i, j, k, bl);
        } else {
            float[] floats2 = new float[floats.length];
            float f = 1.0F / (k * 2 + 1);
            for (int m = 0; m < j; m++) {
                for (int n = 0; n < i; n++) {
                    float g = 0.0F;
                    for (int o = -k; o <= k; o++) {
                        int p = bl ? n + o : n;
                        int q = bl ? m : m + o;
                        if (p >= 0 && q >= 0 && p < i && q < j) {
                            g += floats[q * i + p];
                        }
                    }
                    floats2[m * i + n] = g * f;
                }
            }
            return floats2;
        }
    }

    private static float[] slidingBox(float[] floats, int i, int j, int k, boolean bl) {
        float[] floats2 = new float[floats.length];
        int m = bl ? i : j;
        int n = bl ? j : i;
        float f = 1.0F / (k * 2 + 1);
        for (int o = 0; o < n; o++) {
            double d = 0.0;
            for (int p = 0; p <= Math.min(k, m - 1); p++) {
                d += floats[bl ? o * i + p : p * i + o];
            }
            for (int q = 0; q < m; q++) {
                floats2[bl ? o * i + q : q * i + o] = (float) d * f;
                int r = q - k;
                int t = q + k + 1;
                if (r >= 0) {
                    d -= floats[bl ? o * i + r : r * i + o];
                }
                if (t < m) {
                    d += floats[bl ? o * i + t : t * i + o];
                }
            }
        }
        return floats2;
    }

    private static MenuTextures.Pixels decode(byte[] bytes, String string) {
        ByteBuffer byteBuffer = null;
        ByteBuffer byteBuffer2 = null;
        ByteBuffer byteBuffer3 = null;
        MenuTextures.Pixels pixels;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            byteBuffer = MemoryUtil.memAlloc(bytes.length);
            byteBuffer.put(bytes).flip();
            IntBuffer intBuffer = memoryStack.mallocInt(1);
            IntBuffer intBuffer2 = memoryStack.mallocInt(1);
            IntBuffer intBuffer3 = memoryStack.mallocInt(1);
            byteBuffer2 =
                    STBImage.stbi_load_from_memory(
                            byteBuffer, intBuffer, intBuffer2, intBuffer3, 4);
            if (byteBuffer2 == null) {
                LOGGER.warn("[EFN] Can't decode {}: {}", string, STBImage.stbi_failure_reason());
                return null;
            }
            int i = intBuffer.get(0);
            int j = intBuffer2.get(0);
            if (optimized) {
                int[] ints = ImageDownsampler.dimensions(i, j, 1536);
                if (i != ints[0] || j != ints[1]) {
                    byteBuffer3 = MemoryUtil.memAlloc(ints[0] * ints[1] * 4);
                    ImageDownsampler.rgba(byteBuffer2, i, j, byteBuffer3, ints[0], ints[1]);
                    i = ints[0];
                    j = ints[1];
                }
            }
            ByteBuffer byteBuffer4 = byteBuffer3 == null ? byteBuffer2 : byteBuffer3;
            int[] ints2 = new int[i * j];
            for (int k = 0; k < ints2.length; k++) {
                int m = byteBuffer4.get(k * 4) & 255;
                int n = byteBuffer4.get(k * 4 + 1) & 255;
                int o = byteBuffer4.get(k * 4 + 2) & 255;
                int p = byteBuffer4.get(k * 4 + 3) & 255;
                ints2[k] = p << 24 | m << 16 | n << 8 | o;
            }
            pixels = new MenuTextures.Pixels(i, j, ints2);
        } finally {
            if (byteBuffer3 != null) {
                MemoryUtil.memFree(byteBuffer3);
            }
            if (byteBuffer2 != null) {
                STBImage.stbi_image_free(byteBuffer2);
            }
            if (byteBuffer != null) {
                MemoryUtil.memFree(byteBuffer);
            }
        }
        return pixels;
    }

    private static ThemeImages.Texture upload(int[] ints, int i, int j) {
        ByteBuffer byteBuffer = MemoryUtil.memAlloc(ints.length * 4);
        ThemeImages.Texture texture;
        try {
            for (int k : ints) {
                byteBuffer
                        .put((byte) (k >> 16))
                        .put((byte) (k >> 8))
                        .put((byte) k)
                        .put((byte) (k >>> 24));
            }
            byteBuffer.flip();
            ThemeImages.Texture texture2 = ThemeImages.fromPixels(byteBuffer, i, j, false);
            texture = texture2;
        } finally {
            MemoryUtil.memFree(byteBuffer);
        }
        return texture;
    }

    static {
        ThemeImages.get().onImageProfileChange(MenuTextures::changeProfile);
    }

    @Environment(EnvType.CLIENT)
    private record Pixels(int width, int height, int[] argb) {}
}
