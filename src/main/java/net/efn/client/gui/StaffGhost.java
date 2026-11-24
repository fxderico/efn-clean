package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImGui;

import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.util.NameFilter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;

import java.util.Objects;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class StaffGhost {

    private static final float LIFE_SECONDS = 1.5F;

    private static final float TITLE_SECONDS = 1.25F;

    private static final int TITLE_IN = 10;

    private static final int TITLE_STAY = 40;

    private static final int TITLE_OUT = 15;

    private static final float NOTE_SECONDS = 4.5F;

    private static final float NOTE_IN = 0.5F;

    private static final float NOTE_STAY = 2.5F;

    private static final float NOTE_OUT = 1.0F;

    private static final Text NOTE = Text.literal("Goodluck EFN User, Be Careful");

    private static final int NOTE_COLOR = 16777045;

    private static final float DISTANCE = 1.49F;

    private static final float LIFT = 0.24F;

    private static final float HALF = 0.45F;

    private static final float HAT = 1.125F;

    private static final float HALF_FOV_TAN = (float) Math.tan(Math.toRadians(35.0));

    private static final float NEAR = -0.08F;

    private static final int CELLS = 8;

    private static final float[][] FACES =
            new float[][] {
                {8.0F, 8.0F, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F},
                {0.0F, 8.0F, -1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, -1.0F, -1.0F, -1.0F, 0.8F},
                {16.0F, 8.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 0.8F},
                {8.0F, 0.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, 0.95F},
                {16.0F, 0.0F, -1.0F, -1.0F, 1.0F, 1.0F, -1.0F, 1.0F, -1.0F, -1.0F, -1.0F, 0.6F}
            };

    private static UUID watcher;

    private static boolean hidden;

    private static String title;

    private static int stage;

    private static long startNanos;

    private static long noteNanos;

    private static boolean active;

    private StaffGhost() {}

    public static void watching(UUID uuid, String string, int i) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && string != null) {
            String string2 = NameFilter.apply(string);
            watcher = uuid;
            hidden = !string2.equals(string);
            title = i > 0 ? string2 + " +" + i : string2;
            stage = 0;
            startNanos = System.nanoTime();
            noteNanos = 0L;
            active = true;
        }
    }

    public static boolean hasWork() {
        if (active && MinecraftClient.getInstance().world == null) {
            active = false;
        }
        return active;
    }

    public static void renderNote(Render2DEvent event) {
        if (noteNanos != 0L) {
            float f = (float) (System.nanoTime() - noteNanos) / 1.0E9F;
            if (f >= 4.0F) {
                noteNanos = 0L;
            } else {
                float g = f < 0.5F ? f / 0.5F : Math.min(1.0F, (4.0F - f) / 1.0F);
                int i = Math.round(g * 255.0F);
                if (i > 8) {
                    DrawContext graphics = event.graphics;
                    TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                    graphics.getMatrices().pushMatrix();
                    graphics.getMatrices()
                            .translate(
                                    graphics.getScaledWindowWidth() / 2.0F,
                                    graphics.getScaledWindowHeight() / 2.0F);
                    graphics.getMatrices().scale(2.0F, 2.0F);
                    int j = -textRenderer.getWidth(NOTE) / 2;
                    Objects.requireNonNull(textRenderer);
                    graphics.drawText(
                            textRenderer, NOTE, j, -4, ColorHelper.withAlpha(i, 16777045), true);
                    graphics.getMatrices().popMatrix();
                }
            }
        }
    }

    public static void render() {
        if (hasWork()) {
            MinecraftClient client = MinecraftClient.getInstance();
            float elapsed = elapsed();
            if (stage == 0 && elapsed >= 1.25F) {
                stage = 1;
                client.inGameHud.setTitleTicks(10, 40, 15);
                client.inGameHud.setSubtitle(
                        Text.literal("WATCHING YOU").formatted(Formatting.RED, Formatting.BOLD));
                client.inGameHud.setTitle(
                        Text.literal(title).formatted(Formatting.RED, Formatting.BOLD));
            } else if (stage == 1 && elapsed >= 4.5F) {
                stage = 2;
                client.inGameHud.setDefaultTitleFade();
                noteNanos = System.nanoTime();
                active = false;
                return;
            }
            if (!(elapsed >= 1.5F) && watcher != null) {
                int staffFace = HudRenderer.getInstance().staffFace(watcher, hidden);
                if (staffFace != 0) {
                    float f = elapsed / 1.5F;
                    float g = 0.05F + 0.6F * (float) Math.sin(f * 3.141592653589793);
                    double d = Math.toRadians(60.0 - 150.0 * f);
                    float h = (float) Math.cos(d);
                    float f2 = (float) Math.sin(d);
                    float displaySizeX = ImGui.getIO().getDisplaySizeX();
                    float displaySizeY = ImGui.getIO().getDisplaySizeY();
                    float g2 = displaySizeY * 0.5F / HALF_FOV_TAN;
                    ImDrawList foregroundDrawList = ImGui.getForegroundDrawList();
                    drawCube(
                            foregroundDrawList,
                            staffFace,
                            0.45F,
                            0.0F,
                            h,
                            f2,
                            displaySizeX,
                            displaySizeY,
                            g2,
                            g);
                    drawCube(
                            foregroundDrawList,
                            staffFace,
                            0.50624996F,
                            32.0F,
                            h,
                            f2,
                            displaySizeX,
                            displaySizeY,
                            g2,
                            g);
                }
            }
        }
    }

    private static void drawCube(
            ImDrawList imDrawList,
            int i,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            float f3,
            float g3) {
        float[] floats = new float[3];
        float[] floats2 = new float[3];
        float[] floats3 = new float[3];
        float[] floats4 = new float[81];
        float[] floats5 = new float[floats4.length];
        for (float[] floats6 : FACES) {
            corner(floats6, 2, f, h, f2, floats);
            corner(floats6, 5, f, h, f2, floats2);
            corner(floats6, 8, f, h, f2, floats3);
            float h3 = floats2[0] - floats[0];
            float f4 = floats2[1] - floats[1];
            float g4 = floats2[2] - floats[2];
            float h4 = floats3[0] - floats[0];
            float f5 = floats3[1] - floats[1];
            float g5 = floats3[2] - floats[2];
            float h5 = f4 * g5 - g4 * f5;
            float f6 = g4 * h4 - h3 * g5;
            float g6 = h3 * f5 - f4 * h4;
            float h6 = floats[0] + (h3 + h4) * 0.5F;
            float f7 = floats[1] + (f4 + f5) * 0.5F;
            float g7 = floats[2] + (g4 + g5) * 0.5F;
            if (!(h5 * h6 + f6 * f7 + g6 * g7 <= 0.0F)
                    && !(floats[2] > -0.08F)
                    && !(floats2[2] > -0.08F)
                    && !(floats3[2] > -0.08F)
                    && !(floats3[2] + g4 > -0.08F)) {
                for (int n = 0; n <= 8; n++) {
                    for (int o = 0; o <= 8; o++) {
                        float h7 = o / 8.0F;
                        float f8 = n / 8.0F;
                        float g8 = floats[0] + h3 * h7 + h4 * f8;
                        float h8 = floats[1] + f4 * h7 + f5 * f8;
                        float f9 = floats[2] + g4 * h7 + g5 * f8;
                        floats4[n * 9 + o] = g2 * 0.5F + g8 / -f9 * f3;
                        floats5[n * 9 + o] = h2 * 0.5F - h8 / -f9 * f3;
                    }
                }
                int j = Math.round(floats6[11] * 255.0F);
                int k =
                        Math.round(Math.max(0.0F, Math.min(1.0F, g3)) * 255.0F) << 24
                                | j << 16
                                | j << 8
                                | j;
                float g9 = (floats6[0] + g) / 64.0F;
                float h9 = floats6[1] / 64.0F;
                float f10 = 0.015625F;
                for (int r = 0; r < 8; r++) {
                    for (int t = 0; t < 8; t++) {
                        int m = r * 9 + t;
                        int p = m + 8 + 1;
                        float g10 = g9 + t * f10;
                        float h10 = h9 + r * f10;
                        imDrawList.addImageQuad(
                                i,
                                floats4[m],
                                floats5[m],
                                floats4[m + 1],
                                floats5[m + 1],
                                floats4[p + 1],
                                floats5[p + 1],
                                floats4[p],
                                floats5[p],
                                g10,
                                h10,
                                g10 + f10,
                                h10,
                                g10 + f10,
                                h10 + f10,
                                g10,
                                h10 + f10,
                                k);
                    }
                }
            }
        }
    }

    private static void corner(float[] floats, int i, float f, float g, float h, float[] floats2) {
        float f2 = floats[i] * f;
        float g2 = floats[i + 1] * f + 0.24F;
        float h2 = floats[i + 2] * f - 1.49F;
        floats2[0] = f2;
        floats2[1] = g2 * g - h2 * h;
        floats2[2] = g2 * h + h2 * g;
    }

    private static float elapsed() {
        return (float) (System.nanoTime() - startNanos) / 1.0E9F;
    }
}
