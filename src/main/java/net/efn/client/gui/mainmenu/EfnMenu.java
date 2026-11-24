package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;

import net.efn.client.gui.EfnWidgets;
import net.efn.client.gui.GuiPalette;
import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class EfnMenu {

    private static final List<MenuAction> ICONS =
            List.of(
                    MenuAction.ACCOUNTS,
                    MenuAction.LANGUAGE,
                    MenuAction.ACCESSIBILITY,
                    MenuAction.EFN,
                    MenuAction.MODS,
                    MenuAction.CREDITS);

    private static final int WHITE = -1;

    private static final int TEXT = rgb(236, 235, 240);

    private static final int SOFT = rgb(189, 188, 200);

    private static final int MUTED = rgb(138, 137, 150);

    private static final int CARD = rgb(23, 23, 28);

    private static final int INK = rgb(8, 8, 10);

    private EfnMenu() {}

    private static int rgb(int i, int j, int k) {
        return 0xFF000000 | k << 16 | j << 8 | i;
    }

    private static float ease(float f, float g) {
        float h = Math.max(0.0F, Math.min(1.0F, (f - g) / 0.7F));
        float f2 = 1.0F - h;
        return 1.0F - f2 * f2 * f2 * f2;
    }

    public static void render(MenuContext menuContext) {
        ImDrawList imDrawList = menuContext.bg();
        float width = menuContext.width();
        float height = menuContext.height();
        float scale = menuContext.scale();
        float time = menuContext.time();
        float lookX = menuContext.lookX();
        float lookY = menuContext.lookY();
        int accent = GuiPalette.accent();
        float f = ease(time, 0.0F);
        EfnScape.draw(imDrawList, width, height, scale, time, lookX, lookY, accent, f);
        float g = 150.0F * scale;
        float h = 30.0F * scale;
        ImFont imFont = ImGuiManager.getInstance().titleFont();
        ImFont imFont2 = ImGuiManager.getInstance().smallFont();
        float f2 = 46.0F * scale;
        float g2 = 14.0F * scale;
        float h2 = 380.0F * scale;
        float f3 = 58.0F * scale;
        float g3 = 12.0F * scale;
        float h3 = f3 * 4.0F + g3 * 3.0F + 10.0F * scale;
        float f4 = g + h + f2 + 10.0F * scale + g2 + 46.0F * scale + h3;
        float g4 = width * 0.5F;
        float h4 = Math.max(40.0F * scale, (height - f4) * 0.5F - 6.0F * scale);
        float f5 = ease(time, 0.1F);
        float g5 = (1.0F - f5) * 20.0F * scale;
        float h5 = g4 - g * 0.5F - lookX * 7.0F * scale;
        float f6 = h4 + g5 - lookY * 4.0F * scale;
        EfnScene.glow(
                imDrawList, g4 - lookX * 7.0F * scale, f6 + g * 0.5F, g * 2.6F, accent, 0.16F * f5);
        for (int j = 0; j < 3; j++) {
            float g6 = (time * 0.2F + j / 3.0F) % 1.0F;
            float h6 = g * 0.12F + g6 * g * 0.85F;
            float f7 = (1.0F - g6) * (1.0F - g6) * f5;
            imDrawList.addRect(
                    h5 - h6,
                    f6 - h6,
                    h5 + g + h6,
                    f6 + g + h6,
                    EfnLogo.alpha(accent, 0.3F * f7),
                    g * 0.235F + h6,
                    0,
                    Math.max(1.0F, 1.5F * scale));
        }
        EfnLogo.tile(imDrawList, h5, f6, g, accent, f5);
        h4 += g + h;
        float g7 = ease(time, 0.22F);
        String string = "EFN";
        float h7 = 22.0F * scale;
        float f8 = sizedWidth(imFont, f2, string) + h7 * (string.length() - 1);
        float g8 = g4 - f8 * 0.5F;
        for (int k = 0; k < string.length(); k++) {
            String string2 = String.valueOf(string.charAt(k));
            imDrawList.addText(
                    imFont,
                    Math.round(f2),
                    g8,
                    h4 + (1.0F - g7) * 10.0F * scale,
                    EfnLogo.alpha(TEXT, g7),
                    string2);
            g8 += sizedWidth(imFont, f2, string2) + h7;
        }
        h4 += f2 + 10.0F * scale;
        String string3 = "EFN.LOL";
        float h8 = 5.0F * scale;
        float spacedWidth = menuContext.spacedWidth(imFont2, string3, h8);
        menuContext.spacedText(
                imDrawList,
                imFont2,
                g4 - spacedWidth * 0.5F,
                h4 + (1.0F - g7) * 10.0F * scale,
                h8,
                argb(MUTED),
                g7,
                string3);
        h4 += g2 + 46.0F * scale;
        float f9 = g4 - h2 * 0.5F;
        button(
                menuContext,
                MenuAction.SINGLEPLAYER,
                f9,
                h4,
                h2,
                f3,
                true,
                ease(time, 0.4F),
                accent);
        h4 += f3 + g3;
        button(
                menuContext,
                MenuAction.MULTIPLAYER,
                f9,
                h4,
                h2,
                f3,
                false,
                ease(time, 0.46F),
                accent);
        h4 += f3 + g3;
        float g9 = (h2 - g3) * 0.5F;
        button(menuContext, MenuAction.REALMS, f9, h4, g9, f3, false, ease(time, 0.52F), accent);
        button(
                menuContext,
                MenuAction.OPTIONS,
                f9 + g9 + g3,
                h4,
                g9,
                f3,
                false,
                ease(time, 0.56F),
                accent);
        h4 += f3 + g3 + 10.0F * scale;
        button(menuContext, MenuAction.QUIT, f9, h4, h2, f3, false, ease(time, 0.62F), accent);
        footer(menuContext, accent, ease(time, 0.8F));
    }

    private static float sizedWidth(ImFont imFont, float f, String string) {
        float g = imFont != null && imFont.getFontSize() > 0.0F ? imFont.getFontSize() : f;
        return EfnWidgets.textWidth(imFont, string) * f / g;
    }

    private static int argb(int i) {
        return 0xFF000000 | (i & 0xFF) << 16 | (i >> 8 & 0xFF) << 8 | i >> 16 & 0xFF;
    }

    private static void button(
            MenuContext menuContext,
            MenuAction menuAction,
            float f,
            float g,
            float h,
            float f2,
            boolean bl,
            float g2,
            int i) {
        ImDrawList imDrawList = menuContext.bg();
        float scale = menuContext.scale();
        float h2 = g + (1.0F - g2) * 16.0F * scale;
        float f3 =
                g2 > 0.6F
                        ? menuContext.control(menuAction, f, h2, h, f2)
                        : menuContext.hover(menuAction, false);
        float g3 = 15.0F * scale;
        float h3 = Math.max(1.0F, scale);
        boolean enabled = menuContext.enabled(menuAction);
        ImFont imFont = ImGuiManager.getInstance().moduleFont();
        String label = menuAction.label();
        float f4 = 20.0F * scale;
        float sizedWidth = sizedWidth(imFont, f4, label);
        float g4 = h2 + (f2 - f4) * 0.5F - 1.0F * scale;
        float h4 = f + (h - sizedWidth) * 0.5F;
        if (bl && enabled) {
            int j = EfnScene.mix(i, -1, 0.14F * f3);
            imDrawList.addRectFilled(f, h2, f + h, h2 + f2, EfnLogo.alpha(j, g2), g3);
            imDrawList.addText(imFont, Math.round(f4), h4, g4, EfnLogo.alpha(INK, g2), label);
        } else {
            int alpha = EfnLogo.alpha(CARD, (0.74F + 0.16F * f3) * g2);
            imDrawList.addRectFilled(f, h2, f + h, h2 + f2, alpha, g3);
            if (f3 > 0.01F && enabled) {
                imDrawList.addRectFilled(
                        f, h2, f + h, h2 + f2, EfnLogo.alpha(i, 0.09F * f3 * g2), g3);
            }
            int k = EfnScene.mix(EfnScene.rgba(255, 255, 255, 0.07F), i, f3);
            imDrawList.addRect(
                    f, h2, f + h, h2 + f2, EfnLogo.alpha(k, (0.8F + 0.2F * f3) * g2), g3, 0, h3);
            int m = enabled ? EfnScene.mix(SOFT, -1, f3) : MUTED;
            imDrawList.addText(
                    imFont,
                    Math.round(f4),
                    h4,
                    g4,
                    EfnLogo.alpha(m, g2 * (enabled ? 1.0F : 0.6F)),
                    label);
        }
    }

    private static void footer(MenuContext menuContext, int i, float f) {
        ImDrawList imDrawList = menuContext.bg();
        float scale = menuContext.scale();
        float height = menuContext.height();
        float width = menuContext.width();
        ImFont imFont = ImGuiManager.getInstance().smallFont();
        EfnWidgets.text(
                imDrawList,
                imFont,
                26.0F * scale,
                height - 26.0F * scale - menuContext.textHeight(imFont) * 0.5F,
                EfnLogo.alpha(MUTED, 0.9F * f),
                "efn client  \u00c2\u00b7  v1.2.0");
        float g = height - 26.0F * scale;
        float h = width - 26.0F * scale - (ICONS.size() - 1) * 34.0F * scale;
        for (int j = 0; j < ICONS.size(); j++) {
            MenuAction menuAction = ICONS.get(j);
            float f2 = h + j * 34.0F * scale;
            float g2 = 17.0F * scale;
            float h2 =
                    f > 0.5F
                            ? menuContext.control(menuAction, f2 - g2, g - g2, g2 * 2.0F, g2 * 2.0F)
                            : menuContext.hover(menuAction, false);
            int k = EfnScene.mix(MUTED, i, h2);
            MenuIcons.draw(
                    imDrawList,
                    menuAction,
                    f2,
                    g,
                    9.0F * scale,
                    EfnLogo.alpha(k, f),
                    Math.max(1.2F, 1.5F * scale));
            if (h2 > 0.5F) {
                String string =
                        menuAction == MenuAction.ACCOUNTS
                                ? menuAction.label()
                                        + "  \u00c2\u00b7  "
                                        + menuContext.displayName()
                                : menuAction.label();
                menuContext.tooltip(
                        string,
                        Math.min(
                                f2,
                                width
                                        - menuContext.textWidth(menuContext.tagFont(), string)
                                                * 0.5F
                                        - 18.0F * scale),
                        g - g2 - menuContext.textHeight(menuContext.tagFont()) - 14.0F * scale);
            }
        }
    }
}
