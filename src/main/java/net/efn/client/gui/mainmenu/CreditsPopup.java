package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;

import net.efn.client.gui.HudDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class CreditsPopup {

    private static final List<String> CREDITS = List.of("EFN", "Meteor Client");

    private static final float ROW_H = 34.0F;

    private static final float DIVIDER_HALF = 56.0F;

    private static final float DIAMOND = 3.0F;

    private final DuskWidgets ui = new DuskWidgets();

    private final MenuFrame frame = new MenuFrame();

    private DuskWidgets.Dialog dialog;

    private long openedAt;

    public CreditsPopup() {}

    public void open() {
        this.openedAt = System.nanoTime();
        this.dialog =
                new DuskWidgets.Dialog("Credits", "")
                        .content(CREDITS.size() * 34.0F, this::drawContent)
                        .cancel("Close");
    }

    public boolean isOpen() {
        return this.dialog != null && !this.dialog.isClosed();
    }

    public boolean keyPressed(int i) {
        if (!this.isOpen()) {
            return false;
        } else {
            if (i == 256) {
                this.dialog.dismiss();
            }
            return true;
        }
    }

    public void render(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            boolean bl) {
        if (this.isOpen()) {
            float f3 = (float) (System.nanoTime() - this.openedAt) / 1.0E9F;
            this.frame.begin(imDrawList, imDrawList, f, g, h, f3, f2, g2, h2, bl);
            this.ui.begin(this.frame);
            this.ui.dialog(this.dialog);
        }
    }

    private void drawContent(
            DuskWidgets duskWidgets,
            DuskWidgets.Dialog dialog2,
            float f,
            float g,
            float h,
            float f2) {
        MenuFrame menuFrame = duskWidgets.frame();
        ImDrawList imDrawList = menuFrame.bg();
        float scale = menuFrame.scale();
        float alpha = duskWidgets.alpha();
        ImFont imFont = menuFrame.menuFont();
        float g2 = 34.0F * scale;
        float h2 = f + h * 0.5F;
        for (int j = 0; j < CREDITS.size(); j++) {
            String string = CREDITS.get(j);
            float f3 = g + j * g2;
            menuFrame.text(
                    imDrawList,
                    imFont,
                    h2 - menuFrame.textWidth(imFont, string) * 0.5F,
                    f3 + (g2 - menuFrame.textHeight(imFont)) * 0.5F,
                    -6621958,
                    alpha,
                    string);
            if (j < CREDITS.size() - 1) {
                divider(imDrawList, h2, f3 + g2, scale, alpha);
            }
        }
    }

    private static void divider(ImDrawList imDrawList, float f, float g, float h, float f2) {
        float g2 = 56.0F * h;
        float h2 = 3.0F * h;
        float f3 = h2 * 2.0F;
        float g3 = Math.max(1.0F, h);
        int col = HudDraw.col(-15408912, 0.4F * f2);
        imDrawList.addLine(f - g2, g, f - f3, g, col, g3);
        imDrawList.addLine(f + f3, g, f + g2, g, col, g3);
        imDrawList.addQuadFilled(
                f, g - h2, f + h2, g, f, g + h2, f - h2, g, HudDraw.col(-15408912, 0.75F * f2));
    }
}
