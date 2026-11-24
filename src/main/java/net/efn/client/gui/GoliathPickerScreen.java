package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;

import net.efn.client.config.ThemeStore;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.util.donut.DonutGoliaths;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class GoliathPickerScreen extends Screen {

    private static final float SQUARE = 7.0F;

    private static final float PAD = 14.0F;

    private static final float HEADER_H = 42.0F;

    private static final float INFO_H = 28.0F;

    private static final float BUTTON_W = 96.0F;

    private static final float BUTTON_H = 26.0F;

    private static final int HEADER_BAND = 251658239;

    private static final int WINDOW_FLAGS = 367;

    private final Set<DonutGoliaths.Goliath> picked;

    private final Consumer<Set<DonutGoliaths.Goliath>> onPick;

    private boolean confirmed;

    private boolean dragging;

    private boolean dragAdds;

    private DonutGoliaths.Goliath dragLast;

    public GoliathPickerScreen(
            Set<DonutGoliaths.Goliath> set, Consumer<Set<DonutGoliaths.Goliath>> onPick2) {
        super(Text.literal("Goliath RTP"));
        this.picked = new LinkedHashSet<>(set);
        this.onPick = onPick2;
    }

    @Override
    protected void init() {
        super.init();
        ImGuiManager.getInstance().activate(this::draw);
    }

    @Override
    public void removed() {
        super.removed();
        ImGuiManager.getInstance().deactivate();
        this.onPick.accept(this.confirmed ? Set.copyOf(this.picked) : null);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (ThemeStore.get().menuBlur.get()) {
            super.renderBackground(context, mouseX, mouseY, deltaTicks);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        ImGuiManager.getInstance().requestCursor(context);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() != 257 && input.key() != 335) {
            return super.keyPressed(input);
        } else {
            this.confirm();
            return true;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void confirm() {
        if (!this.picked.isEmpty()) {
            this.confirmed = true;
            this.client.execute(() -> this.close());
        }
    }

    private void draw() {
        float px252 = EfnWidgets.s(252.0F);
        float px14 = EfnWidgets.s(14.0F);
        float px42 = EfnWidgets.s(42.0F);
        float px28 = EfnWidgets.s(28.0F);
        float f = EfnWidgets.s(GuiPalette.rounding());
        ImGui.setNextWindowPos(
                ImGui.getIO().getDisplaySizeX() * 0.5F,
                ImGui.getIO().getDisplaySizeY() * 0.5F,
                1,
                0.5F,
                0.5F);
        ImGui.pushStyleColor(2, GuiPalette.panelBg());
        ImGui.pushStyleVar(2, 0.0F, 0.0F);
        ImGui.pushStyleVar(14, 0.0F, 0.0F);
        ImGui.pushStyleVar(3, f);
        ImGui.pushStyleVar(4, 0.0F);
        if (ImGui.begin("##goliath_rtp", 367)) {
            float windowPosX = ImGui.getWindowPosX();
            float windowPosY = ImGui.getWindowPosY();
            float g = px252 + px14 * 2.0F;
            float windowHeight = ImGui.getWindowHeight();
            ClickGuiRenderer.drawShadow(windowPosX, windowPosY, g, windowHeight, f, 4);
            ImDrawList windowDrawList = ImGui.getWindowDrawList();
            SurfaceBackground.draw(
                    windowDrawList,
                    ThemeStore.get().panels,
                    windowPosX,
                    windowPosY,
                    g,
                    windowHeight,
                    f);
            boolean needsTextGuard = HudChrome.needsTextGuard(ThemeStore.get().panels);
            drawHeader(windowDrawList, windowPosX, windowPosY, g, px42, f, needsTextGuard);
            ImGui.dummy(g, px42);
            ImGui.setCursorPos(px14, px42);
            DonutGoliaths.Goliath goliath = this.drawMap(windowDrawList, px252);
            ImFont imFont = ImGuiManager.getInstance().smallFont();
            String string =
                    goliath != null
                            ? goliath.label() + (goliath.region() == 2 ? "  \u00b7  no /rtp" : "")
                            : (this.picked.isEmpty()
                                    ? "Click or drag to pick goliaths"
                                    : this.picked.size()
                                            + (this.picked.size() == 1
                                                    ? " goliath picked"
                                                    : " goliaths picked"));
            float h = windowPosY + px42 + px252;
            EfnWidgets.text(
                    windowDrawList,
                    imFont,
                    windowPosX + px14,
                    h + (px28 - EfnWidgets.lineHeight(imFont)) * 0.5F,
                    -4542808,
                    string,
                    needsTextGuard);
            float px96 = EfnWidgets.s(96.0F);
            float px26 = EfnWidgets.s(26.0F);
            float f2 = px42 + px252 + px28;
            boolean pushFont = ClickGuiRenderer.pushFont(imFont);
            ImGui.setCursorPos(px14, f2);
            if (EfnWidgets.chipButton("##goliath_clear", "Clear", px96, px26, false)) {
                this.picked.clear();
            }
            ImGui.setCursorPos(px14 + px252 - px96, f2);
            String string2 = this.picked.isEmpty() ? "Go" : "Go (" + this.picked.size() + ")";
            if (EfnWidgets.chipButton(
                    "##goliath_go", string2, px96, px26, !this.picked.isEmpty())) {
                this.confirm();
            }
            if (pushFont) {
                ImGui.popFont();
            }
            ImGui.setCursorPos(0.0F, f2 + px26);
            ImGui.dummy(g, px14);
        }
        ImGui.end();
        ImGui.popStyleVar(4);
        ImGui.popStyleColor();
    }

    private static void drawHeader(
            ImDrawList imDrawList, float f, float g, float h, float f2, float g2, boolean bl) {
        float h2 = Math.min(f2, g2 + EfnWidgets.s(2.0F));
        int i = EfnWidgets.c(EfnWidgets.withAlpha(251658239, 1.0F - h2 / (2.0F * f2)));
        int j = EfnWidgets.c(EfnWidgets.withAlpha(251658239, 1.0F - h2 / f2));
        int k = EfnWidgets.c(16777215);
        imDrawList.addRectFilled(f, g, f + h, g + h2, i, g2, 48);
        imDrawList.addRectFilledMultiColor(f, g + h2, f + h, g + f2, j, j, k, k);
        ImFont imFont = ImGuiManager.getInstance().categoryFont();
        float px7 = EfnWidgets.s(7.0F);
        float f3 = f + EfnWidgets.s(14.0F) + px7;
        float g3 = g + f2 * 0.5F;
        EfnWidgets.grid(imDrawList, f3, g3, px7, -4542808);
        EfnWidgets.text(
                imDrawList,
                imFont,
                f3 + px7 + EfnWidgets.s(9.0F),
                g + (f2 - EfnWidgets.lineHeight(imFont)) * 0.5F,
                -461582,
                "Goliath RTP",
                bl);
    }

    private DonutGoliaths.Goliath drawMap(ImDrawList imDrawList, float f) {
        float cursorScreenPosX = ImGui.getCursorScreenPosX();
        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        ImGui.invisibleButton("##goliath_map", f, f);
        float g = f / 36.0F;
        DonutGoliaths.Goliath goliath =
                ImGui.isItemHovered()
                        ? goliathAt(
                                ImGui.getMousePosX() - cursorScreenPosX,
                                ImGui.getMousePosY() - cursorScreenPosY,
                                g)
                        : null;
        boolean bl = goliath != null && goliath.region() != 2;
        if (ImGui.isItemClicked(0) && bl) {
            this.dragAdds = !this.picked.contains(goliath);
            this.toggle(goliath);
            this.dragging = true;
            this.dragLast = goliath;
        } else {
            DonutGoliaths.Goliath goliath2;
            if (this.dragging
                    && ImGui.isMouseDown(0)
                    && (goliath2 =
                                    goliathAt(
                                            ImGui.getMousePosX() - cursorScreenPosX,
                                            ImGui.getMousePosY() - cursorScreenPosY,
                                            g))
                            != null
                    && goliath2.region() != 2
                    && !goliath2.equals(this.dragLast)) {
                this.toggle(goliath2);
                this.dragLast = goliath2;
            }
        }
        if (!ImGui.isMouseDown(0)) {
            this.dragging = false;
            this.dragLast = null;
        }
        float uiScale = ImGuiManager.getInstance().uiScale();
        float h = Math.min(EfnWidgets.s(4.0F), g);
        boolean bl2 = !this.picked.isEmpty();
        for (int i = 0; i < DonutGoliaths.CELLS.length; i++) {
            int regionColor = RegionMapRenderer.regionColor(i);
            boolean bl3 = i == 2;
            int[][] intGrid = DonutGoliaths.CELLS[i];
            for (int k = 0; k < intGrid.length; k++) {
                DonutGoliaths.Goliath goliath3 = new DonutGoliaths.Goliath(i, k);
                boolean bl4 = !bl3 && (this.picked.contains(goliath3) || goliath3.equals(goliath));
                float f2 = bl3 ? 0.3F : (bl4 ? 1.0F : (bl2 ? 0.45F : 0.85F));
                RegionMapRenderer.tile(
                        imDrawList,
                        cursorScreenPosX,
                        cursorScreenPosY,
                        g,
                        intGrid[k],
                        HudChrome.scaleAlpha(regionColor, f2),
                        h);
            }
        }
        RegionMapRenderer.drawDividers(
                imDrawList, cursorScreenPosX, cursorScreenPosY, g, 1.0F, uiScale);
        for (int m = 0; m < DonutGoliaths.CELLS.length; m++) {
            boolean bl5 = m == 2;
            int[][] intGrid2 = DonutGoliaths.CELLS[m];
            for (int n = 0; n < intGrid2.length; n++) {
                int[] ints = intGrid2[n];
                float g2 = cursorScreenPosX + ints[0] * g;
                float h2 = cursorScreenPosY + ints[1] * g;
                float f3 = cursorScreenPosX + ints[2] * g;
                float g3 = cursorScreenPosY + ints[3] * g;
                DonutGoliaths.Goliath goliath4 = new DonutGoliaths.Goliath(m, n);
                float px0_75 = EfnWidgets.s(0.75F);
                if (this.picked.contains(goliath4)) {
                    imDrawList.addRect(
                            g2 + px0_75,
                            h2 + px0_75,
                            f3 - px0_75,
                            g3 - px0_75,
                            -1,
                            0.0F,
                            0,
                            EfnWidgets.s(1.5F));
                } else if (!bl5 && goliath4.equals(goliath)) {
                    imDrawList.addRect(
                            g2 + px0_75,
                            h2 + px0_75,
                            f3 - px0_75,
                            g3 - px0_75,
                            -1711276033,
                            0.0F,
                            0,
                            EfnWidgets.s(1.0F));
                }
                RegionMapRenderer.drawCenteredTileText(
                        imDrawList,
                        n + 1,
                        g2,
                        h2,
                        f3 - g2,
                        g3 - h2,
                        1.0F,
                        bl5 ? 0.5F : 1.0F,
                        uiScale);
            }
        }
        if (this.client.player != null
                && this.client.world != null
                && this.client.world.getRegistryKey() != World.END) {
            double d = this.client.world.getRegistryKey() == World.NETHER ? 8.0 : 1.0;
            double e = 450000.0;
            double d2 = (this.client.player.getX() * d + 225000.0) / e;
            double e2 = (this.client.player.getZ() * d + 225000.0) / e;
            if (d2 >= 0.0 && d2 < 1.0 && e2 >= 0.0 && e2 < 1.0) {
                float h3 = cursorScreenPosX + (float) (d2 * f);
                float f4 = cursorScreenPosY + (float) (e2 * f);
                imDrawList.addCircleFilled(h3, f4, EfnWidgets.s(3.0F), -13487361);
                imDrawList.addCircle(
                        h3, f4, EfnWidgets.s(3.0F), -1728053248, 16, EfnWidgets.s(1.0F));
            }
        }
        return goliath;
    }

    private void toggle(DonutGoliaths.Goliath goliath) {
        if (this.dragAdds) {
            this.picked.add(goliath);
        } else {
            this.picked.remove(goliath);
        }
    }

    private static DonutGoliaths.Goliath goliathAt(float f, float g, float h) {
        return !(f < 0.0F) && !(g < 0.0F)
                ? DonutGoliaths.at(f / h * 12500.0F - 225000.0F, g / h * 12500.0F - 225000.0F)
                : null;
    }
}
