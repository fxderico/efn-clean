package net.efn.client.module.impl.misc.noinvleak;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Environment(EnvType.CLIENT)
final class OverlayArt {

    private static final int GLYPH = 8;

    private static final int SHADOW_MASK = 16579836;

    private static OverlayArt.Image selection;

    private static OverlayArt.Slice tooltipBackground;

    private static OverlayArt.Slice tooltipFrame;

    private static boolean[] glyphs;

    private static boolean loaded;

    private OverlayArt() {}

    static void reset() {
        selection = null;
        tooltipBackground = null;
        tooltipFrame = null;
        glyphs = null;
        loaded = false;
    }

    static OverlayArt.Image selection() {
        load();
        return selection;
    }

    static OverlayArt.Slice tooltipBackground() {
        load();
        return tooltipBackground;
    }

    static OverlayArt.Slice tooltipFrame() {
        load();
        return tooltipFrame;
    }

    private static void load() {
        if (!loaded) {
            loaded = true;
            selection = image("textures/gui/sprites/hud/hotbar_selection.png");
            tooltipBackground = slice("textures/gui/sprites/tooltip/background.png", 9, false);
            tooltipFrame = slice("textures/gui/sprites/tooltip/frame.png", 10, true);
            OverlayArt.Image image2 = image("textures/font/ascii.png");
            if (image2 != null) {
                glyphs = new boolean[image2.width() * image2.height()];
                for (int i = 0; i < glyphs.length; i++) {
                    glyphs[i] = image2.pixels()[i] >>> 24 != 0;
                }
            }
        }
    }

    private static OverlayArt.Image image(String string) {
        Resource resource =
                MinecraftClient.getInstance()
                        .getResourceManager()
                        .getResource(Identifier.ofVanilla(string))
                        .orElse(null);
        if (resource == null) {
            return null;
        } else {
            try {
                OverlayArt.Image image2;
                try (InputStream inputStream = resource.getInputStream()) {
                    NativeImage nativeImage = NativeImage.read(inputStream);
                    OverlayArt.Image image3;
                    label58:
                    {
                        try {
                            int[] ints = nativeImage.copyPixelsAbgr();
                            int[] ints2 = new int[ints.length];
                            for (int i = 0; i < ints.length; i++) {
                                int j = ints[i] >>> 24;
                                ints2[i] =
                                        Canvas.premultiply(
                                                j << 24
                                                        | (ints[i] & 0xFF) << 16
                                                        | (ints[i] >>> 8 & 0xFF) << 8
                                                        | ints[i] >>> 16 & 0xFF,
                                                j);
                            }
                            image3 =
                                    new OverlayArt.Image(
                                            ints2, nativeImage.getWidth(), nativeImage.getHeight());
                            if (nativeImage == null) {
                                break label58;
                            }
                        } catch (Throwable exception) {
                            if (nativeImage != null) {
                                try {
                                    nativeImage.close();
                                } catch (Throwable exception2) {
                                    exception.addSuppressed(exception2);
                                }
                            }
                            throw exception;
                        }
                        nativeImage.close();
                    }
                    image2 = image3;
                }
                return image2;
            } catch (RuntimeException | IOException exception3) {
                return null;
            }
        }
    }

    private static OverlayArt.Slice slice(String string, int i, boolean bl) {
        OverlayArt.Image image2 = image(string);
        if (image2 == null) {
            return null;
        } else {
            Resource resource =
                    MinecraftClient.getInstance()
                            .getResourceManager()
                            .getResource(Identifier.ofVanilla(string + ".mcmeta"))
                            .orElse(null);
            if (resource != null) {
                try (BufferedReader reader = resource.getReader()) {
                    JsonObject jsonObject =
                            JsonParser.parseReader(reader)
                                    .getAsJsonObject()
                                    .getAsJsonObject("gui")
                                    .getAsJsonObject("scaling");
                    if (jsonObject.has("border")) {
                        i = jsonObject.get("border").getAsInt();
                    }
                    bl =
                            jsonObject.has("stretch_inner")
                                    && jsonObject.get("stretch_inner").getAsBoolean();
                } catch (RuntimeException | IOException exception) {
                }
            }
            return new OverlayArt.Slice(image2, i, bl);
        }
    }

    static void slice(Canvas canvas, OverlayArt.Slice slice, int i, int j, int k, int m, int n) {
        if (slice != null) {
            OverlayArt.Image image2 = slice.image();
            int border2 = slice.border();
            int o = image2.width() - 2 * border2;
            int p = image2.height() - 2 * border2;
            if (o > 0 && p > 0) {
                int[] ints = new int[k];
                int[] ints2 = new int[m];
                for (int r = 0; r < k; r++) {
                    ints[r] = sourceAt(r, k, border2, o, slice.stretch());
                }
                for (int t = 0; t < m; t++) {
                    ints2[t] = sourceAt(t, m, border2, p, slice.stretch());
                }
                for (int u = 0; u < m; u++) {
                    for (int v = 0; v < k; v++) {
                        int pixel = image2.pixels()[ints2[u] * image2.width() + ints[v]];
                        if (pixel >>> 24 != 0) {
                            canvas.blendPixel(pixel, (i + v) * n, (j + u) * n, n);
                        }
                    }
                }
            }
        }
    }

    private static int sourceAt(int i, int j, int k, int m, boolean bl) {
        if (i < k) {
            return i;
        } else if (i >= j - k) {
            return k + m + (i - (j - k));
        } else {
            int n = i - k;
            int o = j - 2 * k;
            return k + (bl ? n * m / Math.max(o, 1) : n % m);
        }
    }

    static void text(Canvas canvas, Text text, int i, int j, int k, int m, boolean bl) {
        load();
        if (glyphs != null) {
            int[] ints = new int[] {i};
            text.visit(
                    (style, value) -> {
                        int n =
                                style.getColor() != null
                                        ? m & 0xFF000000 | style.getColor().getRgb()
                                        : m;
                        int o = 0;
                        while (o < value.length()) {
                            int p = value.codePointAt(o);
                            o += Character.charCount(p);
                            int advance = advance(p, style);
                            if (p < 128 && p > 32 && !style.isObfuscated()) {
                                if (bl) {
                                    glyph(
                                            canvas,
                                            p,
                                            ints[0] + 1,
                                            j + 1,
                                            k,
                                            n & 0xFF000000 | (n & 16579836) >> 2,
                                            style);
                                }
                                glyph(canvas, p, ints[0], j, k, n, style);
                            }
                            if (style.isStrikethrough()) {
                                canvas.rect(ints[0] * k, (j + 4) * k, advance * k, k, n);
                            }
                            if (style.isUnderlined()) {
                                canvas.rect(ints[0] * k, (j + 8) * k, advance * k, k, n);
                            }
                            ints[0] += advance;
                        }
                        return Optional.empty();
                    },
                    Style.EMPTY);
        }
    }

    private static int advance(int i, Style style) {
        return MinecraftClient.getInstance()
                .textRenderer
                .getWidth(StringVisitable.styled(new String(Character.toChars(i)), style));
    }

    private static void glyph(Canvas canvas, int i, int j, int k, int m, int n, Style style) {
        int o = i % 16 * 8;
        int p = i / 16 * 8;
        short s = 128;
        for (int q = 0; q < 8; q++) {
            int r = style.isItalic() && q < 4 ? 1 : 0;
            int t = 0;
            while (t < 8) {
                if (!glyphs[(p + q) * s + o + t]) {
                    t++;
                } else {
                    int u = t;
                    while (t < 8 && glyphs[(p + q) * s + o + t]) {
                        t++;
                    }
                    canvas.rect((j + u + r) * m, (k + q) * m, (t - u) * m, m, n);
                    if (style.isBold()) {
                        canvas.rect((j + u + r + 1) * m, (k + q) * m, (t - u) * m, m, n);
                    }
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    record Image(int[] pixels, int width, int height) {}

    @Environment(EnvType.CLIENT)
    record Slice(OverlayArt.Image image, int border, boolean stretch) {}
}
