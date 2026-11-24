package imgui.gl3;

import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.type.ImInt;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public class EfnGl3 extends ImGuiImplGl3 {

    public EfnGl3() {}

    public boolean createFontsTexture() {
        ImFontAtlas fonts = ImGui.getIO().getFonts();
        ImInt imInt = new ImInt();
        ImInt imInt2 = new ImInt();
        ByteBuffer texDataAsRGBA32 = fonts.getTexDataAsRGBA32(imInt, imInt2);
        int glGetInteger = GL11.glGetInteger(3379);
        long l = (long) imInt.get() * imInt2.get() * 4L;
        if (imInt.get() > 0
                && imInt2.get() > 0
                && imInt.get() <= glGetInteger
                && imInt2.get() <= glGetInteger
                && texDataAsRGBA32 != null
                && l <= texDataAsRGBA32.capacity()
                && l <= 2147483647L) {
            ByteBuffer byteBuffer = texDataAsRGBA32.duplicate();
            byteBuffer.position(0);
            byteBuffer.limit((int) l);
            int glGetInteger2 = GL11.glGetInteger(34016);
            GL13.glActiveTexture(33984);
            int glGetInteger3 = GL11.glGetInteger(32873);
            int glGetInteger4 = GL11.glGetInteger(35055);
            int glGetInteger5 = GL11.glGetInteger(3317);
            int glGetInteger6 = GL11.glGetInteger(3314);
            int glGetInteger7 = GL11.glGetInteger(3315);
            int glGetInteger8 = GL11.glGetInteger(3316);
            int fontTexture = this.data.fontTexture;
            int i = 0;
            boolean bl = false;
            label82:
            {
                try {
                    reportExistingErrors();
                    GL15.glBindBuffer(35052, 0);
                    GL11.glPixelStorei(3317, 1);
                    GL11.glPixelStorei(3314, 0);
                    GL11.glPixelStorei(3315, 0);
                    GL11.glPixelStorei(3316, 0);
                    i = GL11.glGenTextures();
                    GL11.glBindTexture(3553, i);
                    GL11.glTexParameteri(3553, 10241, 9729);
                    GL11.glTexParameteri(3553, 10240, 9729);
                    GL11.glTexParameteri(3553, 10242, 33071);
                    GL11.glTexParameteri(3553, 10243, 33071);
                    GL11.glTexParameteri(3553, 33084, 0);
                    GL11.glTexParameteri(3553, 33085, 0);
                    GL11.glTexImage2D(
                            3553, 0, 32856, imInt.get(), imInt2.get(), 0, 6408, 5121, byteBuffer);
                    int glGetError = GL11.glGetError();
                    if (i == 0 || glGetError != 0) {
                        throw new IllegalStateException(
                                "[EFN ImGui] Font upload failed: "
                                        + imInt.get()
                                        + "x"
                                        + imInt2.get()
                                        + ", GL error=0x"
                                        + Integer.toHexString(glGetError)
                                        + ", renderer="
                                        + GL11.glGetString(7937));
                    }
                    this.data.fontTexture = i;
                    fonts.setTexID(i);
                    bl = true;
                    System.err.printf(
                            Locale.ROOT,
                            "[EFN ImGui] Font atlas uploaded: %dx%d RGBA8, max=%d,"
                                    + " previousUnpackBuffer=%d, GL=%s, renderer=%s%n",
                            imInt.get(),
                            imInt2.get(),
                            glGetInteger,
                            glGetInteger4,
                            GL11.glGetString(7938),
                            GL11.glGetString(7937));
                    if (bl || i == 0) {
                        break label82;
                    }
                } catch (Throwable exception) {
                    if (!bl && i != 0) {
                        GL11.glDeleteTextures(i);
                    }
                    int j =
                            bl && fontTexture != 0 && glGetInteger3 == fontTexture
                                    ? i
                                    : glGetInteger3;
                    GL11.glBindTexture(3553, j);
                    GL15.glBindBuffer(35052, glGetInteger4);
                    GL11.glPixelStorei(3317, glGetInteger5);
                    GL11.glPixelStorei(3314, glGetInteger6);
                    GL11.glPixelStorei(3315, glGetInteger7);
                    GL11.glPixelStorei(3316, glGetInteger8);
                    GL13.glActiveTexture(glGetInteger2);
                    if (bl && fontTexture != 0 && fontTexture != i) {
                        GL11.glDeleteTextures(fontTexture);
                    }
                    throw exception;
                }
                GL11.glDeleteTextures(i);
            }
            int k = bl && fontTexture != 0 && glGetInteger3 == fontTexture ? i : glGetInteger3;
            GL11.glBindTexture(3553, k);
            GL15.glBindBuffer(35052, glGetInteger4);
            GL11.glPixelStorei(3317, glGetInteger5);
            GL11.glPixelStorei(3314, glGetInteger6);
            GL11.glPixelStorei(3315, glGetInteger7);
            GL11.glPixelStorei(3316, glGetInteger8);
            GL13.glActiveTexture(glGetInteger2);
            if (bl && fontTexture != 0 && fontTexture != i) {
                GL11.glDeleteTextures(fontTexture);
            }
            return true;
        } else {
            throw new IllegalStateException(
                    "[EFN ImGui] Invalid font atlas "
                            + imInt.get()
                            + "x"
                            + imInt2.get()
                            + "; GL_MAX_TEXTURE_SIZE="
                            + glGetInteger);
        }
    }

    private static void reportExistingErrors() {
        for (int i = 0; i < 16; i++) {
            int glGetError = GL11.glGetError();
            if (glGetError == 0) {
                return;
            }
            System.err.println(
                    "[EFN ImGui] GL error predating font upload: 0x"
                            + Integer.toHexString(glGetError));
        }
    }
}
