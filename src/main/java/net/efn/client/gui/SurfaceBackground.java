package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImGui;

import net.efn.client.config.Surface;
import net.efn.client.config.ThemeStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SurfaceBackground {

    private SurfaceBackground() {}

    public static float bannerHeight(Surface surface) {
        return surface.isBanner() && isVisible(surface)
                ? EfnWidgets.s(surface.headerHeight.get())
                : 0.0F;
    }

    public static boolean hasImageBanner(Surface surface) {
        return surface.isImage() && bannerHeight(surface) > 0.0F;
    }

    public static void draw(
            ImDrawList imDrawList,
            Surface surface,
            float f,
            float g,
            float f2,
            float g2,
            float h2) {
        draw(imDrawList, surface, f, g, f2, g2, h2, 1.0F);
    }

    public static void draw(
            ImDrawList imDrawList,
            Surface surface,
            float f,
            float g,
            float f2,
            float g2,
            float h2,
            float f3) {
        if (!(f3 <= 0.001F) && isVisible(surface)) {
            boolean banner2 = surface.isBanner();
            float g3 = banner2 ? Math.min(g2, EfnWidgets.s(surface.headerHeight.get())) : g2;
            if (surface.isImage()) {
                drawImage(
                        imDrawList,
                        surface,
                        f,
                        g,
                        f2,
                        g3,
                        h2,
                        banner2,
                        f3,
                        surface.imageScale.get());
            } else {
                drawPattern(imDrawList, surface, f, g, f2, g3, h2, banner2, f3);
            }
        }
    }

    public static void drawDisc(
            ImDrawList imDrawList, Surface surface, float f, float g, float f2, float g2) {
        if (!(g2 <= 0.001F) && isVisible(surface)) {
            float h2 = f - f2;
            float f3 = g - f2;
            float g3 = 2.0F * f2;
            if (surface.isImage()) {
                drawImage(
                        imDrawList,
                        surface,
                        h2,
                        f3,
                        g3,
                        g3,
                        f2,
                        false,
                        g2,
                        Math.max(1.0F, surface.imageScale.get()));
            } else {
                drawPattern(imDrawList, surface, h2, f3, g3, g3, f2, false, g2);
            }
        }
    }

    private static boolean isVisible(Surface surface) {
        return surface.background.is("None")
                ? false
                : !surface.isImage() || ThemeImages.get().texture(surface.image.get()) != null;
    }

    private static void drawImage(
            ImDrawList imDrawList,
            Surface surface,
            float f,
            float g,
            float f2,
            float g2,
            float h2,
            boolean bl,
            float f3,
            float g3) {
        ThemeImages.Texture texture = ThemeImages.get().texture(surface.image.get());
        if (texture != null) {
            float[] floats =
                    surface.isScreenMapped()
                            ? place(
                                    texture,
                                    surface,
                                    0.0F,
                                    0.0F,
                                    ImGui.getIO().getDisplaySizeX(),
                                    ImGui.getIO().getDisplaySizeY(),
                                    g3)
                            : place(texture, surface, f, g, f2, g2, g3);
            drawClipped(
                    imDrawList,
                    texture.id(),
                    floats,
                    f,
                    g,
                    f2,
                    g2,
                    EfnWidgets.c(EfnWidgets.withAlpha(-1, surface.imageOpacity.get() * f3)),
                    h2,
                    bl ? 48 : 240);
            if (bl) {
                int scaleAlpha = HudChrome.scaleAlpha(GuiPalette.fill(surface), f3);
                int i = scaleAlpha & 16777215;
                imDrawList.addRectFilledMultiColor(
                        f,
                        g + g2 * 0.3F,
                        f + f2,
                        g + g2 + 1.0F,
                        EfnWidgets.c(i),
                        EfnWidgets.c(i),
                        EfnWidgets.c(scaleAlpha),
                        EfnWidgets.c(scaleAlpha));
            }
        }
    }

    public static float[] coverUv(int i, int j, float f, float g, float f2) {
        float g2 = (float) i / Math.max(1, j);
        float h2;
        if (g2 > (h2 = f / Math.max(1.0F, g))) {
            float f3 = h2 / g2;
            float g3 = (1.0F - f3) * 0.5F;
            return new float[] {g3, 0.0F, g3 + f3, 1.0F};
        } else {
            float h3 = g2 / h2;
            float f4 = (1.0F - h3) * Math.max(0.0F, Math.min(1.0F, f2));
            return new float[] {0.0F, f4, 1.0F, f4 + h3};
        }
    }

    public static float[] place(
            int i, int j, float f, float g, float f2, float g2, float h2, float f3, float g3) {
        float h3 = Math.max(f2 / Math.max(1, i), g2 / Math.max(1, j)) * h2;
        float f4 = i * h3;
        float g4 = j * h3;
        float h4 = f + (f2 - f4) * f3;
        float f5 = g + (g2 - g4) * g3;
        return new float[] {h4, f5, h4 + f4, f5 + g4};
    }

    private static float[] place(
            ThemeImages.Texture texture, Surface surface, float f, float g, float f2, float g2) {
        return place(texture, surface, f, g, f2, g2, surface.imageScale.get());
    }

    private static float[] place(
            ThemeImages.Texture texture,
            Surface surface,
            float f,
            float g,
            float f2,
            float g2,
            float h2) {
        return place(
                texture.width(),
                texture.height(),
                f,
                g,
                f2,
                g2,
                h2,
                surface.imageX.get(),
                surface.imageY.get());
    }

    private static void drawClipped(
            ImDrawList imDrawList,
            int i,
            float[] floats,
            float f,
            float g,
            float f2,
            float g2,
            int j,
            float h2,
            int k) {
        float f3 = Math.max(f, floats[0]);
        float g3 = Math.max(g, floats[1]);
        float h3 = Math.min(f + f2, floats[2]);
        float f4 = Math.min(g + g2, floats[3]);
        if (!(h3 <= f3) && !(f4 <= g3)) {
            float g4 = floats[2] - floats[0];
            float h4 = floats[3] - floats[1];
            float f5 = (f3 - floats[0]) / g4;
            float g5 = (g3 - floats[1]) / h4;
            float h5 = (h3 - floats[0]) / g4;
            float f6 = (f4 - floats[1]) / h4;
            boolean bl = f3 - f <= h2;
            boolean bl2 = g3 - g <= h2;
            boolean bl3 = f + f2 - h3 <= h2;
            boolean bl4 = g + g2 - f4 <= h2;
            int m =
                    (bl && bl2 ? 16 : 0)
                            | (bl3 && bl2 ? 32 : 0)
                            | (bl && bl4 ? 64 : 0)
                            | (bl3 && bl4 ? 128 : 0);
            int n = k & m;
            if (h2 > 0.0F && n != 0) {
                imDrawList.addImageRounded(i, f3, g3, h3, f4, f5, g5, h5, f6, j, h2, n);
            } else {
                imDrawList.addImage(i, f3, g3, h3, f4, f5, g5, h5, f6, j);
            }
        }
    }

    public static void drawPart(
            ImDrawList imDrawList,
            Surface surface,
            float f,
            float g,
            float f2,
            float g2,
            float h2,
            float f3,
            float g3,
            float h3,
            float f4,
            float g4,
            int i) {
        if (!(f4 <= 0.001F)
                && !(g3 <= 0.0F)
                && !(h3 <= 0.0F)
                && !(f2 <= 0.0F)
                && !(g2 <= 0.0F)
                && isVisible(surface)) {
            float displaySizeX = ImGui.getIO().getDisplaySizeX();
            float displaySizeY = ImGui.getIO().getDisplaySizeY();
            boolean screenMapped = surface.isScreenMapped();
            if (surface.isImage()) {
                ThemeImages.Texture texture = ThemeImages.get().texture(surface.image.get());
                if (texture != null) {
                    float[] floats =
                            screenMapped
                                    ? place(
                                            texture,
                                            surface,
                                            0.0F,
                                            0.0F,
                                            displaySizeX,
                                            displaySizeY)
                                    : place(texture, surface, f, g, f2, g2);
                    drawClipped(
                            imDrawList,
                            texture.id(),
                            floats,
                            h2,
                            f3,
                            g3,
                            h3,
                            EfnWidgets.c(EfnWidgets.withAlpha(-1, surface.imageOpacity.get() * f4)),
                            g4,
                            i);
                }
            } else {
                SurfaceBackground.Lines lines =
                        new SurfaceBackground.Lines(
                                imDrawList,
                                h2,
                                f3,
                                g3,
                                h3,
                                0.0F,
                                surface.lineOpacity.get() * f4,
                                false,
                                screenMapped ? h2 : h2 - f,
                                screenMapped ? f3 : f3 - g,
                                screenMapped ? displaySizeX : f2,
                                false);
                imDrawList.pushClipRect(h2, f3, h2 + g3, f3 + h3, true);
                drawLines(surface, lines);
                imDrawList.popClipRect();
            }
        }
    }

    private static void drawPattern(
            ImDrawList imDrawList,
            Surface surface,
            float f,
            float g,
            float f2,
            float g2,
            float h2,
            boolean bl,
            float f3) {
        boolean screenMapped = surface.isScreenMapped();
        SurfaceBackground.Lines lines =
                new SurfaceBackground.Lines(
                        imDrawList,
                        f,
                        g,
                        f2,
                        g2,
                        h2,
                        surface.lineOpacity.get() * f3,
                        bl,
                        screenMapped ? f : 0.0F,
                        screenMapped ? g : 0.0F,
                        screenMapped ? ImGui.getIO().getDisplaySizeX() : f2,
                        true);
        drawLines(surface, lines);
    }

    private static void drawLines(Surface surface, SurfaceBackground.Lines lines) {
        ThemeStore themeStore = ThemeStore.get();
        float f =
                themeStore.animateLines.get()
                        ? (float) ImGui.getTime() * GuiPalette.animationSpeed()
                        : 0.0F;
        String background = surface.background.get();
        switch (background) {
            case "Flow":
                flow(lines, f);
                break;
            case "Topographic":
                topographic(lines, f);
                break;
            case "Rings":
                rings(lines, f);
                break;
            case "Grid":
                grid(lines, f);
        }
    }

    private static void flow(SurfaceBackground.Lines lines, float f) {
        float px6 = EfnWidgets.s(6.0F);
        float px10 = EfnWidgets.s(10.0F);
        float g = f * 0.25F;
        int i = (int) Math.floor((lines.originY - EfnWidgets.s(40.0F)) / px10);
        int j = (int) Math.ceil((lines.originY + lines.h + EfnWidgets.s(40.0F)) / px10);
        for (int k = i; k <= j; k++) {
            float f2 = 0.0F;
            float g2 = 0.0F;
            for (float g3 = 0.0F; g3 <= lines.w + px6; g3 += px6) {
                float h2 = (lines.originX + g3) / EfnWidgets.s(1.0F);
                float f3 =
                        k * px10
                                + (float) Math.sin(h2 * 0.018F + k * 0.35F + g)
                                        * EfnWidgets.s(14.0F)
                                + (float) Math.sin(h2 * 0.007F - k * 0.2F - g * 0.6F)
                                        * EfnWidgets.s(10.0F);
                float h3 = f3 - lines.originY;
                if (g3 > 0.0F) {
                    lines.segment(f2, g2, g3, h3);
                }
                f2 = g3;
                g2 = h3;
            }
        }
    }

    private static void rings(SurfaceBackground.Lines lines, float f) {
        float g = lines.worldW + EfnWidgets.s(10.0F) - lines.originX;
        float f2 = -EfnWidgets.s(30.0F) - lines.originY;
        float px14 = EfnWidgets.s(14.0F);
        float g2 = f * EfnWidgets.s(4.0F) % px14;
        float h2 =
                (float)
                        Math.hypot(
                                Math.max(0.0F, Math.max(-g, g - lines.w)),
                                Math.max(0.0F, Math.max(-f2, f2 - lines.h)));
        float f3 =
                (float)
                        Math.hypot(
                                Math.max(Math.abs(g), Math.abs(g - lines.w)),
                                Math.max(Math.abs(f2), Math.abs(f2 - lines.h)));
        float g3 = EfnWidgets.s(24.0F) + g2;
        if (h2 > g3) {
            g3 += (float) Math.floor((h2 - g3) / px14) * px14;
        }
        for (float f4 = g3; f4 <= f3; f4 += px14) {
            int i = Math.max(24, (int) (f4 * 0.35F));
            float h3 = g + f4;
            float g4 = f2;
            for (int j = 1; j <= i; j++) {
                double d = 6.283185307179586 * j / i;
                float h4 = g + (float) Math.cos(d) * f4;
                float f5 = f2 + (float) Math.sin(d) * f4;
                lines.segment(h3, g4, h4, f5);
                h3 = h4;
                g4 = f5;
            }
        }
    }

    private static void grid(SurfaceBackground.Lines lines, float f) {
        float px16 = EfnWidgets.s(16.0F);
        float px8 = EfnWidgets.s(8.0F);
        float g = f * EfnWidgets.s(3.0F) % px16;
        float f2 = floorMod(g - lines.originX, px16);
        float g2 = floorMod(g - lines.originY, px16);
        for (float g3 = f2; g3 <= lines.w; g3 += px16) {
            for (float h3 = 0.0F; h3 < lines.h; h3 += px8) {
                lines.segment(g3, h3, g3, h3 + px8);
            }
        }
        for (float f4 = g2; f4 <= lines.h; f4 += px16) {
            for (float g4 = 0.0F; g4 < lines.w; g4 += px8) {
                lines.segment(g4, f4, g4 + px8, f4);
            }
        }
    }

    private static void topographic(SurfaceBackground.Lines lines, float f) {
        float px6 = EfnWidgets.s(6.0F);
        int i = (int) Math.ceil(lines.w / px6) + 1;
        int j = (int) Math.ceil(lines.h / px6) + 1;
        float[] floats = new float[i * j];
        float g = f * 0.15F;
        for (int k = 0; k < j; k++) {
            for (int m = 0; m < i; m++) {
                float f2 = (lines.originX + m * px6) / EfnWidgets.s(1.0F);
                float g2 = (lines.originY + k * px6) / EfnWidgets.s(1.0F);
                floats[k * i + m] =
                        (float)
                                (Math.sin(f2 * 0.021 + Math.sin(g2 * 0.03 + g) * 1.6)
                                        + Math.cos(g2 * 0.028 + Math.sin(f2 * 0.013 - g) * 2.1)
                                        + 0.6 * Math.sin((f2 + g2) * 0.017 + g));
            }
        }
        for (float f3 = -2.4F; f3 <= 2.4F; f3 += 0.4F) {
            for (int n = 0; n < j - 1; n++) {
                for (int o = 0; o < i - 1; o++) {
                    contourCell(lines, floats, i, o, n, px6, f3);
                }
            }
        }
    }

    private static void contourCell(
            SurfaceBackground.Lines lines, float[] floats, int i, int j, int k, float f, float g) {
        float f2 = floats[k * i + j];
        float g2;
        float h2;
        float f3;
        int m =
                (f2 > g ? 8 : 0)
                        | ((f3 = floats[k * i + j + 1]) > g ? 4 : 0)
                        | ((h2 = floats[(k + 1) * i + j + 1]) > g ? 2 : 0)
                        | ((g2 = floats[(k + 1) * i + j]) > g ? 1 : 0);
        if (m != 0 && m != 15) {
            float g3 = j * f;
            float h3 = k * f;
            float f4 = g3 + f * t(f2, f3, g);
            float g4 = g3 + f;
            float h4 = h3 + f * t(f3, h2, g);
            float f5 = g3 + f * t(g2, h2, g);
            float g5 = h3 + f;
            float h5 = h3 + f * t(f2, g2, g);
            switch (m) {
                case 1:
                case 14:
                    lines.segment(g3, h5, f5, g5);
                    break;
                case 2:
                case 13:
                    lines.segment(f5, g5, g4, h4);
                    break;
                case 3:
                case 12:
                    lines.segment(g3, h5, g4, h4);
                    break;
                case 4:
                case 11:
                    lines.segment(f4, h3, g4, h4);
                    break;
                case 5:
                    lines.segment(g3, h5, f4, h3);
                    lines.segment(f5, g5, g4, h4);
                    break;
                case 6:
                case 9:
                    lines.segment(f4, h3, f5, g5);
                    break;
                case 7:
                case 8:
                    lines.segment(g3, h5, f4, h3);
                    break;
                case 10:
                    lines.segment(f4, h3, g4, h4);
                    lines.segment(g3, h5, f5, g5);
            }
        }
    }

    private static float t(float f, float g, float f2) {
        float g2 = g - f;
        return Math.abs(g2) < 1.0E-6F ? 0.5F : (f2 - f) / g2;
    }

    private static float floorMod(float f, float g) {
        float f2 = f % g;
        return f2 < 0.0F ? f2 + g : f2;
    }

    @Environment(EnvType.CLIENT)
    private static final class Lines {

        final ImDrawList dl;

        final float x;

        final float y;

        final float w;

        final float h;

        final float edge;

        final float originX;

        final float originY;

        final float worldW;

        final boolean banner;

        final int color;

        Lines(
                ImDrawList dl2,
                float f,
                float g,
                float f2,
                float g2,
                float h2,
                float f3,
                boolean banner2,
                float originX2,
                float originY2,
                float worldW2,
                boolean bl) {
            this.dl = dl2;
            this.x = f;
            this.y = g;
            this.w = f2;
            this.h = g2;
            this.banner = banner2;
            this.originX = originX2;
            this.originY = originY2;
            this.worldW = worldW2;
            this.edge = bl ? Math.max(EfnWidgets.s(8.0F), h2) : 0.0F;
            float g3 = banner2 ? 0.24F : 0.16F;
            this.color =
                    EfnWidgets.withAlpha(
                            EfnTheme.lerpColor(-1, GuiPalette.accent(), 0.25F), g3 * f3);
        }

        void segment(float f, float g, float f2, float g2) {
            float h2 = (f + f2) * 0.5F;
            float f3 = (g + g2) * 0.5F;
            if (!(f3 < 0.0F) && !(f3 > this.h) && !(h2 < 0.0F) && !(h2 > this.w)) {
                float g3 = 1.0F;
                if (this.banner) {
                    g3 = 1.0F - f3 / this.h;
                    g3 *= g3;
                } else if (this.edge > 0.0F) {
                    g3 *= Math.min(1.0F, Math.min(f3, this.h - f3) / this.edge);
                }
                if (this.edge > 0.0F) {
                    g3 *= Math.min(1.0F, Math.min(h2, this.w - h2) / this.edge);
                }
                if (!(g3 <= 0.02F)) {
                    this.dl.addLine(
                            this.x + f,
                            this.y + g,
                            this.x + f2,
                            this.y + g2,
                            EfnWidgets.c(EfnWidgets.withAlpha(this.color, g3)),
                            1.0F);
                }
            }
        }
    }
}
