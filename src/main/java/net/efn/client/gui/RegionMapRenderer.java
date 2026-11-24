package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;

import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.impl.client.RegionMapModule;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.util.donut.DonutGoliaths;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class RegionMapRenderer {

    private static final float PAD = 10.0F;

    private static final float INFO_GAP = 8.0F;

    private static final float INFO_LINE_H = 22.0F;

    private static final float LEGEND_GAP = 4.0F;

    private static final float LEGEND_ROW_H = 16.0F;

    private static final float LEGEND_SWATCH = 9.0F;

    private static final float LABEL_GAP = 8.0F;

    private static final int MIN_ID_PX = 6;

    private static final String MINUS = "\u2212";

    private static final String COORDINATE_LABEL = "Coordinate";

    private static final String REGION_LABEL = "Region";

    private static final String NO_REGION = "\u2014";

    private static final int[] REGION_COLORS =
            new int[] {-10236257, -10246656, -1012536, -4559313, -1397425, -12401931, -16545540};

    private static final int[] LEGEND_ORDER = new int[] {0, 1, 2, 4, 3, 5, 6};

    private static final float PIN_SIZE = 6.0F;

    private static final float MAP_ROUNDING = 4.0F;

    private static final int GOLIATH_DIVIDER = 771751936;

    private static final int REGION_DIVIDER = 1191182336;

    private static final List<int[]> GOLIATH_LINES = new ArrayList<>();

    private static final List<int[]> REGION_LINES = new ArrayList<>();

    private RegionMapRenderer() {}

    private static void dividerRuns(int i, boolean bl) {
        byte b = 36;
        int j = 0;
        int dividerKind = dividerKind(i, 0, bl);
        for (int m = 1; m <= b; m++) {
            int k = m < b ? dividerKind(i, m, bl) : -1;
            if (k != dividerKind) {
                if (dividerKind == 2) {
                    REGION_LINES.add(new int[] {bl ? 1 : 0, i, j, m});
                } else if (dividerKind == 1) {
                    GOLIATH_LINES.add(new int[] {bl ? 1 : 0, i, j, m});
                }
                dividerKind = k;
                j = m;
            }
        }
    }

    private static int dividerKind(int i, int j, boolean bl) {
        int k = bl ? DonutGoliaths.square(i - 1, j) : DonutGoliaths.square(j, i - 1);
        int m;
        if (k < 0 == (m = bl ? DonutGoliaths.square(i, j) : DonutGoliaths.square(j, i)) < 0
                && k / 1000 == m / 1000) {
            return k != m ? 1 : 0;
        } else {
            return 2;
        }
    }

    public static RegionMapRenderer.Size measureSize(RegionMapModule module, float f) {
        float gridSize = gridSize(module, f);
        float g = 10.0F * f;
        float h = Math.max(gridSize, measureInfoWidth(module, f));
        return new RegionMapRenderer.Size(
                Math.round(h + g * 2.0F),
                Math.round(gridSize + g * 2.0F + 8.0F * f + measureInfoHeight(module) * f));
    }

    private static float gridSize(RegionMapModule module, float f) {
        return 36.0F * module.squareSize() * f;
    }

    private static float measureInfoWidth(RegionMapModule module, float f) {
        ImGuiManager imGuiManager = ImGuiManager.getInstance();
        int px = HudChrome.px(17.0F, f);
        int px2 = HudChrome.px(14.0F, f);
        float g = labelColumn(module, imGuiManager.hudLabelFont(), px2) + 8.0F * f;
        float h =
                module.showCoordinates()
                        ? HudChrome.tabularWidth(
                                imGuiManager.hudTextFont(), px, "\u2212000000, \u2212000000")
                        : 0.0F;
        float f2 = 0.0F;
        for (String string : DonutGoliaths.REGIONS) {
            h =
                    Math.max(
                            h,
                            HudChrome.width(
                                    imGuiManager.hudTextFont(), px, "00  \u00b7  " + string));
            f2 =
                    Math.max(
                            f2,
                            15.0F * f + HudChrome.width(imGuiManager.hudLabelFont(), px2, string));
        }
        return (float) Math.ceil(Math.max(g + h, module.showLegend() ? f2 : 0.0F));
    }

    public static void render(
            ImDrawList imDrawList,
            RegionMapModule module,
            HudLayoutStore.HudElementState hudElementState) {
        render(imDrawList, module, hudElementState, 1.0F);
    }

    public static void render(
            ImDrawList imDrawList,
            RegionMapModule module,
            HudLayoutStore.HudElementState hudElementState,
            float f) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && imDrawList != null && module != null && hudElementState != null) {
            float scale = HudRenderer.scaleOf(hudElementState);
            float g = module.squareSize() * scale;
            float h = 10.0F * scale;
            float gridSize = gridSize(module, scale);
            float f2 = Math.round(hudElementState.x);
            float g2 = Math.round(hudElementState.y);
            float h2 = HudRenderer.hudRounding() * scale;
            float f3 = module.opacity() * f;
            HudChrome.card(
                    imDrawList,
                    HudRenderer.hudSurface(hudElementState),
                    f2,
                    g2,
                    hudElementState.width,
                    hudElementState.height,
                    h2,
                    f3,
                    module.shadow(),
                    scale);
            float g3 = Math.round(f2 + (hudElementState.width - gridSize) * 0.5F);
            float h3 = g2 + h;
            float f4 = Math.min(4.0F * scale, g);
            for (int i = 0; i < DonutGoliaths.CELLS.length; i++) {
                int scaleAlpha = HudChrome.scaleAlpha(REGION_COLORS[i], f3);
                for (int[] ints : DonutGoliaths.CELLS[i]) {
                    tile(imDrawList, g3, h3, g, ints, scaleAlpha, f4);
                }
            }
            drawDividers(imDrawList, g3, h3, g, f3, scale);
            if (module.showRegionIds()) {
                for (int[][] intGrid : DonutGoliaths.CELLS) {
                    for (int k = 0; k < intGrid.length; k++) {
                        int[] ints2 = intGrid[k];
                        drawCenteredTileText(
                                imDrawList,
                                k + 1,
                                g3 + ints2[0] * g,
                                h3 + ints2[1] * g,
                                (ints2[2] - ints2[0]) * g,
                                (ints2[3] - ints2[1]) * g,
                                module.textScale(),
                                f3,
                                scale);
                    }
                }
            }
            renderPlayerPin(imDrawList, module, client, player, g3, h3, gridSize, f3, scale);
            renderInfoPanel(
                    imDrawList,
                    module,
                    client,
                    player,
                    f2 + h,
                    h3 + gridSize + 8.0F * scale,
                    f3,
                    scale);
        }
    }

    static int regionColor(int i) {
        return REGION_COLORS[i];
    }

    static void tile(
            ImDrawList imDrawList, float f, float g, float h, int[] ints, int i, float f2) {
        int j = 36;
        int k = 0;
        if (ints[0] == 0 && ints[1] == 0) {
            k |= 16;
        }
        if (ints[2] == j && ints[1] == 0) {
            k |= 32;
        }
        if (ints[0] == 0 && ints[3] == j) {
            k |= 64;
        }
        if (ints[2] == j && ints[3] == j) {
            k |= 128;
        }
        float g2 = f + ints[0] * h;
        float h2 = g + ints[1] * h;
        float f3 = f + ints[2] * h;
        float g3 = g + ints[3] * h;
        if (k == 0) {
            imDrawList.addRectFilled(g2, h2, f3, g3, i);
        } else {
            imDrawList.addRectFilled(g2, h2, f3, g3, i, f2, k);
        }
    }

    static void drawDividers(ImDrawList imDrawList, float f, float g, float h, float f2, float g2) {
        float h2 = Math.max(1.0F, g2);
        drawRuns(imDrawList, GOLIATH_LINES, f, g, h, h2, HudChrome.scaleAlpha(771751936, f2));
        drawRuns(imDrawList, REGION_LINES, f, g, h, h2, HudChrome.scaleAlpha(1191182336, f2));
    }

    private static void drawRuns(
            ImDrawList imDrawList, List<int[]> list, float f, float g, float h, float f2, int i) {
        float g2 = f2 * 0.5F;
        for (int[] ints : list) {
            if (ints[0] == 1) {
                float h2 = f + ints[1] * h;
                imDrawList.addRectFilled(h2 - g2, g + ints[2] * h, h2 + g2, g + ints[3] * h, i);
            } else {
                float f3 = g + ints[1] * h;
                imDrawList.addRectFilled(f + ints[2] * h, f3 - g2, f + ints[3] * h, f3 + g2, i);
            }
        }
    }

    static void drawCenteredTileText(
            ImDrawList imDrawList,
            int i,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            float f3) {
        ImFont imFont = ImGuiManager.getInstance().hudLabelFont();
        String string = String.valueOf(i);
        int j = Math.max(6, HudChrome.px(14.0F * g2 * 0.85F, f3));
        float g3 = h - 2.0F * f3;
        float width2 = HudChrome.width(imFont, j, "00");
        if (width2 > g3) {
            j = (int) Math.floor(j * g3 / width2);
        }
        if ((j = Math.min(j, (int) Math.floor(f2 - f3))) >= 6) {
            float h3 = f + (h - HudChrome.width(imFont, j, string)) * 0.5F;
            float f4 = g + (f2 - j) * 0.5F;
            HudChrome.text(
                    imDrawList,
                    imFont,
                    j,
                    h3,
                    f4,
                    HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, h2),
                    string,
                    true);
        }
    }

    private static void renderPlayerPin(
            ImDrawList imDrawList,
            RegionMapModule module,
            MinecraftClient client,
            ClientPlayerEntity player,
            float f,
            float g,
            float h,
            float f2,
            float g2) {
        if (inOverworld(client)) {
            double d = 450000.0;
            double e = (player.getX() + 225000.0) / d;
            double d2 = (player.getZ() + 225000.0) / d;
            if (!(e < 0.0) && !(e >= 1.0) && !(d2 < 0.0) && !(d2 >= 1.0)) {
                float h2 = f + (float) (e * h);
                float f3 = g + (float) (d2 * h);
                double e2 = Math.toRadians(-player.getYaw() - 90.0);
                float g3 = 6.0F * module.playerScale() * g2;
                int scaleAlpha = HudChrome.scaleAlpha(toAbgr(module.playerIndicatorColor()), f2);
                float h3 = h2 + (float) (Math.cos(e2) * g3);
                float f4 = f3 - (float) (Math.sin(e2) * g3);
                float g4 = h2 + (float) (Math.cos(e2 + Math.toRadians(135.0)) * g3);
                float h4 = f3 - (float) (Math.sin(e2 + Math.toRadians(135.0)) * g3);
                float f5 = h2 + (float) (Math.cos(e2 - Math.toRadians(135.0)) * g3);
                float g5 = f3 - (float) (Math.sin(e2 - Math.toRadians(135.0)) * g3);
                imDrawList.addTriangleFilled(h3, f4, g4, h4, f5, g5, scaleAlpha);
                imDrawList.addTriangle(
                        h3, f4, g4, h4, f5, g5, HudChrome.scaleAlpha(-1728053248, f2), 1.0F);
            }
        }
    }

    private static void renderInfoPanel(
            ImDrawList imDrawList,
            RegionMapModule module,
            MinecraftClient client,
            ClientPlayerEntity player,
            float f,
            float g,
            float h,
            float f2) {
        ImGuiManager imGuiManager = ImGuiManager.getInstance();
        ImFont imFont = imGuiManager.hudTextFont();
        ImFont imFont2 = imGuiManager.hudLabelFont();
        int px = HudChrome.px(17.0F, f2);
        int px2 = HudChrome.px(14.0F, f2);
        float baselineOffset = HudChrome.baselineOffset(imFont, px, imFont2, px2);
        float g2 = f + labelColumn(module, imFont2, px2) + 8.0F * f2;
        float h2 = 22.0F * f2;
        float f3 = (float) Math.floor((h2 - px) * 0.5F);
        float g3 = g + f3;
        if (module.showCoordinates()) {
            HudChrome.text(
                    imDrawList,
                    imFont2,
                    px2,
                    f,
                    g3 + baselineOffset,
                    HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, h),
                    "Coordinate",
                    false);
            HudChrome.tabular(
                    imDrawList,
                    imFont,
                    px,
                    g2,
                    g3,
                    HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, h),
                    number((long) Math.floor(player.getX()))
                            + ", "
                            + number((long) Math.floor(player.getZ())),
                    false);
            g3 += h2;
        }
        DonutGoliaths.Goliath goliath =
                inOverworld(client) ? DonutGoliaths.at(player.getX(), player.getZ()) : null;
        String string =
                goliath == null
                        ? "\u2014"
                        : goliath.index()
                                + 1
                                + "  \u00b7  "
                                + DonutGoliaths.REGIONS[goliath.region()];
        HudChrome.text(
                imDrawList,
                imFont2,
                px2,
                f,
                g3 + baselineOffset,
                HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, h),
                "Region",
                false);
        HudChrome.text(
                imDrawList,
                imFont,
                px,
                g2,
                g3,
                HudChrome.scaleAlpha(HudChrome.TEXT_NAME, h),
                string,
                false);
        if (module.showLegend()) {
            float h3 = g + h2 * infoLines(module) + 4.0F * f2;
            float f4 = 16.0F * f2;
            float g4 = 9.0F * f2;
            for (int m = 0; m < LEGEND_ORDER.length; m++) {
                int i = LEGEND_ORDER[m];
                float h4 = h3 + m * f4;
                float f5 = h4 + (f4 - g4) * 0.5F;
                imDrawList.addRectFilled(
                        f,
                        f5,
                        f + g4,
                        f5 + g4,
                        HudChrome.scaleAlpha(REGION_COLORS[i], h),
                        2.0F * f2);
                HudChrome.text(
                        imDrawList,
                        imFont2,
                        px2,
                        f + g4 + 6.0F * f2,
                        h4 + (float) Math.floor((f4 - px2) * 0.5F),
                        HudChrome.scaleAlpha(HudChrome.TEXT_NAME, h),
                        DonutGoliaths.REGIONS[i],
                        false);
            }
        }
    }

    private static boolean inOverworld(MinecraftClient client) {
        return client.world != null && client.world.getRegistryKey() == World.OVERWORLD;
    }

    private static int infoLines(RegionMapModule module) {
        return module.showCoordinates() ? 2 : 1;
    }

    private static float labelColumn(RegionMapModule module, ImFont imFont, int i) {
        float width2 = HudChrome.width(imFont, i, "Region");
        return module.showCoordinates()
                ? Math.max(HudChrome.width(imFont, i, "Coordinate"), width2)
                : width2;
    }

    private static float measureInfoHeight(RegionMapModule module) {
        float f = 22.0F * infoLines(module);
        if (module.showLegend()) {
            f += 4.0F + LEGEND_ORDER.length * 16.0F;
        }
        return f;
    }

    private static String number(long l) {
        return l < 0L ? "\u2212" + -l : Long.toString(l);
    }

    private static int toAbgr(ColorSetting setting) {
        return GuiPalette.abgr(setting.argb());
    }

    static {
        for (int i = 1; i < 36; i++) {
            dividerRuns(i, true);
            dividerRuns(i, false);
        }
    }

    @Environment(EnvType.CLIENT)
    public record Size(float width, float height) {}
}
