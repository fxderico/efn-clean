package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Environment(EnvType.CLIENT)
final class HudEmblem {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final String SOURCE = "/assets/efn/textures/gui/hud_emblem.png";

    private static final float INK_GAMMA = 0.65F;

    private static final float INK_GAIN = 1.35F;

    private static float[] ink;

    private static int inkW;

    private static int inkH;

    private static boolean missing;

    private static ThemeImages.Texture sized;

    private static int sizedHeight = -1;

    private HudEmblem() {}

    static ThemeImages.Texture forHeight(int i) {
        if (i < 4 || missing) {
            return null;
        } else if (i == sizedHeight && sized != null) {
            return sized;
        } else if (ink == null && !load()) {
            missing = true;
            return null;
        } else {
            int j = Math.max(1, Math.round((float) inkW * i / inkH));
            ByteBuffer byteBuffer = MemoryUtil.memAlloc(j * i * 4);
            ThemeImages.Texture texture;
            try {
                for (int k = 0; k < i; k++) {
                    for (int m = 0; m < j; m++) {
                        float coverage = coverage(m, k, j, i);
                        coverage =
                                Math.min(
                                        1.0F,
                                        (float) Math.pow(coverage, 0.6499999761581421) * 1.35F);
                        byteBuffer
                                .put((byte) -1)
                                .put((byte) -1)
                                .put((byte) -1)
                                .put((byte) Math.round(coverage * 255.0F));
                    }
                }
                byteBuffer.flip();
                if (sized != null) {
                    GL11.glDeleteTextures(sized.id());
                }
                sized = ThemeImages.fromPixels(byteBuffer, j, i, false);
                sizedHeight = i;
                ThemeImages.Texture texture2 = sized;
                texture = texture2;
            } finally {
                MemoryUtil.memFree(byteBuffer);
            }
            return texture;
        }
    }

    private static float coverage(int i, int j, int k, int m) {
        float f = (float) i * inkW / k;
        float g = (float) (i + 1) * inkW / k;
        float h = (float) j * inkH / m;
        float f2 = (float) (j + 1) * inkH / m;
        float g2 = 0.0F;
        float h2 = 0.0F;
        for (int n = (int) h; n < Math.min(inkH, (int) Math.ceil(f2)); n++) {
            float f3 = Math.min(f2, (float) (n + 1)) - Math.max(h, (float) n);
            for (int o = (int) f; o < Math.min(inkW, (int) Math.ceil(g)); o++) {
                float g3 = Math.min(g, (float) (o + 1)) - Math.max(f, (float) o);
                float h3 = g3 * f3;
                g2 += ink[n * inkW + o] * h3;
                h2 += h3;
            }
        }
        return h2 > 0.0F ? g2 / h2 : 0.0F;
    }

    private static boolean load() {
        byte[] bytes;
        try {
            label189:
            {
                boolean bl;
                try (InputStream resourceAsStream =
                        HudEmblem.class.getResourceAsStream(
                                "/assets/efn/textures/gui/hud_emblem.png")) {
                    if (resourceAsStream != null) {
                        bytes = resourceAsStream.readAllBytes();
                        break label189;
                    }
                    LOGGER.warn(
                            "[EFN] Missing texture {}", "/assets/efn/textures/gui/hud_emblem.png");
                    bl = false;
                }
                return bl;
            }
        } catch (IOException exception) {
            LOGGER.warn(
                    "[EFN] Failed to read {}",
                    "/assets/efn/textures/gui/hud_emblem.png",
                    exception);
            return false;
        }
        ByteBuffer byteBuffer = MemoryUtil.memAlloc(bytes.length);
        ByteBuffer byteBuffer2 = null;
        boolean bl2;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            byteBuffer.put(bytes).flip();
            IntBuffer intBuffer = memoryStack.mallocInt(1);
            IntBuffer intBuffer2 = memoryStack.mallocInt(1);
            IntBuffer intBuffer3 = memoryStack.mallocInt(1);
            byteBuffer2 =
                    STBImage.stbi_load_from_memory(
                            byteBuffer, intBuffer, intBuffer2, intBuffer3, 4);
            if (byteBuffer2 == null) {
                LOGGER.warn(
                        "[EFN] Can't decode {}: {}",
                        "/assets/efn/textures/gui/hud_emblem.png",
                        STBImage.stbi_failure_reason());
                return false;
            }
            inkW = intBuffer.get(0);
            inkH = intBuffer2.get(0);
            ink = new float[inkW * inkH];
            for (int i = 0; i < ink.length; i++) {
                ink[i] = (byteBuffer2.get(i * 4 + 3) & 255) / 255.0F;
            }
            bl2 = true;
        } finally {
            if (byteBuffer2 != null) {
                STBImage.stbi_image_free(byteBuffer2);
            }
            MemoryUtil.memFree(byteBuffer);
        }
        return bl2;
    }
}
