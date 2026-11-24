package net.efn.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public final class WindowIcon {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final int[] SIZES = new int[] {16, 32, 48, 64, 128, 256};

    private WindowIcon() {}

    public static void apply(long l) {
        int glfwGetPlatform = GLFW.glfwGetPlatform();
        if (glfwGetPlatform == 393217 || glfwGetPlatform == 393220) {
            ArrayList list = new ArrayList(SIZES.length);
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                Buffer buffer = GLFWImage.malloc(SIZES.length, memoryStack);
                int i = 0;
                for (int j : SIZES) {
                    ByteBuffer byteBuffer =
                            load("/assets/efn/icons/icon_" + j + ".png", memoryStack);
                    if (byteBuffer != null) {
                        list.add(byteBuffer);
                        buffer.position(i++);
                        buffer.width(j);
                        buffer.height(j);
                        buffer.pixels(byteBuffer);
                    }
                }
                if (i == 0) {
                    return;
                }
                buffer.position(0);
                buffer.limit(i);
                GLFW.glfwSetWindowIcon(l, buffer);
            } finally {
                list.forEach(value -> STBImage.stbi_image_free((java.nio.ByteBuffer) value));
            }
        }
    }

    private static ByteBuffer load(String string, MemoryStack memoryStack) {
        try {
            ByteBuffer byteBuffer;
            try (InputStream resourceAsStream = WindowIcon.class.getResourceAsStream(string)) {
                if (resourceAsStream == null) {
                    return null;
                }
                byte[] bytes = resourceAsStream.readAllBytes();
                ByteBuffer byteBuffer2 = MemoryUtil.memAlloc(bytes.length);
                ByteBuffer byteBuffer3;
                try {
                    byteBuffer2.put(bytes).flip();
                    IntBuffer intBuffer = memoryStack.mallocInt(1);
                    IntBuffer intBuffer2 = memoryStack.mallocInt(1);
                    IntBuffer intBuffer3 = memoryStack.mallocInt(1);
                    byteBuffer3 =
                            STBImage.stbi_load_from_memory(
                                    byteBuffer2, intBuffer, intBuffer2, intBuffer3, 4);
                } catch (Throwable exception) {
                    MemoryUtil.memFree(byteBuffer2);
                    throw exception;
                }
                MemoryUtil.memFree(byteBuffer2);
                byteBuffer = byteBuffer3;
            }
            return byteBuffer;
        } catch (RuntimeException | IOException exception2) {
            LOGGER.warn("[EFN+] Failed to load window icon {}", string, exception2);
            return null;
        }
    }
}
