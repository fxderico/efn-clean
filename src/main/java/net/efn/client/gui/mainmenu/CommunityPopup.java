package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;

import net.efn.client.config.ThemeStore;
import net.efn.client.gui.HudDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class CommunityPopup {

    public static final String INVITE = "discord.gg/pxHtFp5b6h";

    public static final String INVITE_URL = "https://discord.com/invite/pxHtFp5b6h";

    private static final float OPEN_DELAY = 0.6F;

    private static final float NEXT_DELAY = 0.35F;

    private static final float MARK_RING = 54.0F;

    private static final float MARK_GAP = 14.0F;

    private static final float LINE_GAP = 3.0F;

    private static final float CAPTION_TRACK = 4.0F;

    private static final float CHECK_BOX = 16.0F;

    private static final float CHECK_LABEL_GAP = 9.0F;

    private static final float CHECK_REACH = 5.0F;

    private static final float ROW_GAP = 18.0F;

    private static final float CONTENT_H = 88.0F;

    private static final float[] CLYDE = clydeOutline();

    private static final float CLYDE_EYE_X = 0.332F;

    private static final float CLYDE_EYE_Y = 0.076F;

    private static final float CLYDE_EYE_RX = 0.18F;

    private static final float CLYDE_EYE_RY = 0.2F;

    private static boolean offered;

    private final DuskWidgets ui = new DuskWidgets();

    private final MenuFrame frame = new MenuFrame();

    private DuskWidgets.Dialog dialog;

    private CommunityPopup.Offer offer = CommunityPopup.Offer.DISCORD;

    private boolean dontShowAgain;

    private long openedAt;

    private float closedAt;

    public CommunityPopup() {}

    public void update(float closedAt2, boolean bl) {
        if (this.dialog != null && this.dialog.isClosed()) {
            this.dialog = null;
            this.closedAt = closedAt2;
        }
        if (this.dialog == null && bl) {
            if (!offered) {
                if (closedAt2 < 0.6F) {
                    return;
                }
                offered = true;
                if (ThemeStore.get().communityPopup.get()) {
                    this.open(CommunityPopup.Offer.DISCORD);
                }
            }
        }
    }

    public boolean isOpen() {
        return this.dialog != null && !this.dialog.isClosed();
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

    private void open(CommunityPopup.Offer offer2) {
        this.offer = offer2;
        this.dontShowAgain = false;
        this.openedAt = System.nanoTime();
        this.dialog =
                new DuskWidgets.Dialog(
                                "Join EFN Community", "Updates, configs and support. Come say hi.")
                        .content(88.0F, this::drawContent)
                        .action("Join", this::join)
                        .cancel("Later", this::answered);
    }

    private void join() {
        Util.getOperatingSystem().open(this.offer.url);
        this.answered();
    }

    private void answered() {
        if (this.dontShowAgain) {
            ThemeStore themeStore = ThemeStore.get();
            themeStore.communityPopup.set(false);
            themeStore.save();
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
        ImFont imFont = menuFrame.tagFont();
        ImFont imFont2 = menuFrame.menuFont();
        String caption2 = this.offer.caption;
        String link2 = this.offer.link;
        float g2 = 54.0F * scale;
        float h2 = 4.0F * scale;
        float spacedWidth = menuFrame.spacedWidth(imFont, caption2, h2);
        float textWidth = menuFrame.textWidth(imFont2, link2);
        float f3 = g2 + 14.0F * scale + Math.max(spacedWidth, textWidth);
        float g3 = f + (h - f3) * 0.5F;
        boolean hovered = duskWidgets.hovered(g3, g, f3, g2);
        if (duskWidgets.pressed(g3, g, f3, g2)) {
            dialog2.confirm();
        }
        float hover = duskWidgets.hover("invite", hovered);
        float h3 = g3 + g2 * 0.5F;
        float f4 = g + g2 * 0.5F - 2.0F * scale;
        float g4 = g2 * 0.44F;
        DuskWidgets.ringMark(imDrawList, h3, f4, g4, hover, scale, alpha);
        discordMark(
                imDrawList,
                h3,
                f4,
                g4 * 1.1F,
                HudDraw.col(HudDraw.mix(-6557705, -16777216, hover), alpha),
                HudDraw.col(HudDraw.mix(-16381684, -15408912, hover), alpha));
        float textHeight = menuFrame.textHeight(imFont);
        float textHeight2 = menuFrame.textHeight(imFont2);
        float h4 = g3 + g2 + 14.0F * scale;
        float f5 = f4 - (textHeight + 3.0F * scale + textHeight2) * 0.5F;
        menuFrame.spacedText(imDrawList, imFont, h4, f5, h2, -7693914, alpha, caption2);
        float g5 = f5 + textHeight + 3.0F * scale;
        float h5 = g5 + textHeight2 + scale;
        menuFrame.text(
                imDrawList,
                imFont2,
                h4,
                g5,
                HudDraw.mix(-15408912, -6621958, hover),
                alpha,
                "discord.gg/pxHtFp5b6h");
        if (hover > 0.01F) {
            imDrawList.addLine(
                    h4,
                    h5,
                    h4 + textWidth * hover,
                    h5,
                    HudDraw.col(-6621958, hover * alpha),
                    Math.max(1.0F, scale));
        }
        ImFont imFont3 = menuFrame.tagFont();
        String string = "Don't show again";
        float f6 = 16.0F * scale;
        float g6 = 5.0F * scale;
        float h6 = f6 + 9.0F * scale + menuFrame.textWidth(imFont3, string);
        float f7 = f + (h - h6) * 0.5F;
        float g7 = g + f2 - f6;
        boolean hovered2 = duskWidgets.hovered(f7, g7 - g6, h6, f6 + 2.0F * g6);
        if (duskWidgets.pressed(f7, g7 - g6, h6, f6 + 2.0F * g6)) {
            this.dontShowAgain = !this.dontShowAgain;
        }
        float hover2 = duskWidgets.hover("dont-show-again", hovered2);
        checkBox(imDrawList, f7, g7, f6, this.dontShowAgain, hover2, scale, alpha);
        int i = HudDraw.mix(this.dontShowAgain ? -6557705 : -7693914, -6621958, hover2);
        menuFrame.text(
                imDrawList,
                imFont3,
                f7 + f6 + 9.0F * scale,
                g7 + (f6 - menuFrame.textHeight(imFont3)) * 0.5F + scale,
                i,
                alpha,
                string);
    }

    private static void checkBox(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            boolean bl,
            float f2,
            float g2,
            float h2) {
        float f3 = 3.0F * g2;
        if (!bl) {
            imDrawList.addRectFilled(f, g, f + h, g + h, HudDraw.col(-16777216, 0.55F * h2), f3);
            imDrawList.addRect(
                    f,
                    g,
                    f + h,
                    g + h,
                    HudDraw.col(-15408912, (0.45F + 0.4F * f2) * h2),
                    f3,
                    0,
                    Math.max(1.0F, 1.2F * g2));
        } else {
            imDrawList.addRectFilled(
                    f, g, f + h, g + h, HudDraw.col(HudDraw.mix(-15408912, -6621958, f2), h2), f3);
            imDrawList.pathClear();
            imDrawList.pathLineTo(f + h * 0.24F, g + h * 0.52F);
            imDrawList.pathLineTo(f + h * 0.43F, g + h * 0.71F);
            imDrawList.pathLineTo(f + h * 0.77F, g + h * 0.31F);
            imDrawList.pathStroke(HudDraw.col(-16777216, h2), 0, Math.max(1.4F, 2.0F * g2));
        }
    }

    private static void discordMark(
            ImDrawList imDrawList, float f, float g, float h, int i, int j) {
        float f2 = h * 0.5F;
        imDrawList.pathClear();
        for (int k = 0; k < CLYDE.length; k += 2) {
            imDrawList.pathLineTo(f + CLYDE[k] * f2, g + CLYDE[k + 1] * f2);
        }
        imDrawList.pathFillConcave(i);
        byte b = 16;
        for (int m = -1; m <= 1; m += 2) {
            float g2 = f + m * 0.332F * f2;
            float h2 = g + 0.076F * f2;
            imDrawList.pathClear();
            for (int n = 0; n < b; n++) {
                double d = n * 3.141592653589793 * 2.0 / b;
                imDrawList.pathLineTo(
                        g2 + (float) Math.cos(d) * 0.18F * f2,
                        h2 + (float) Math.sin(d) * 0.2F * f2);
            }
            imDrawList.pathFillConvex(j);
        }
    }

    private static float[] clydeOutline() {
        byte b = 6;
        byte b2 = 4;
        float[] floats = new float[2 * (4 + b + 3 + b2)];
        int i = 0;
        i = put(floats, i, 0.0F, -0.667F);
        i = put(floats, i, 0.229F, -0.65F);
        i = put(floats, i, 0.282F, -0.758F);
        i = put(floats, i, 0.694F, -0.631F);
        for (int j = 1; j <= b; j++) {
            float f = (float) j / b;
            float g = 1.0F - f;
            float h = g * g * g;
            float f2 = 3.0F * g * g * f;
            float g2 = 3.0F * g * f * f;
            float h2 = f * f * f;
            i =
                    put(
                            floats,
                            i,
                            h * 0.694F + f2 * 0.938F + g2 * 1.03F + h2 * 0.992F,
                            h * -0.631F + f2 * -0.272F + g2 * 0.103F + h2 * 0.504F);
        }
        i = put(floats, i, 0.749F, 0.653F);
        i = put(floats, i, 0.486F, 0.758F);
        i = put(floats, i, 0.344F, 0.531F);
        for (int k = 1; k <= b2; k++) {
            float f3 = 0.344F * (1.0F - (float) k / b2);
            i = put(floats, i, f3, -0.606F + (float) Math.sqrt(1.413721F - f3 * f3));
        }
        int m = i / 2;
        float[] floats2 = new float[2 * (2 * m - 2)];
        System.arraycopy(floats, 0, floats2, 0, i);
        for (int n = m - 2; n >= 1; n--) {
            i = put(floats2, i, -floats[2 * n], floats[2 * n + 1]);
        }
        return floats2;
    }

    private static int put(float[] floats, int i, float f, float g) {
        floats[i] = f;
        floats[i + 1] = g;
        return i + 2;
    }

    @Environment(EnvType.CLIENT)
    private static enum Offer {
        DISCORD("DISCORD", "discord.gg/pxHtFp5b6h", "https://discord.gg/pxHtFp5b6h");

        final String caption;

        final String link;

        final String url;

        private Offer(String caption2, String link2, String url2) {
            this.caption = caption2;
            this.link = link2;
            this.url = url2;
        }
    }
}
