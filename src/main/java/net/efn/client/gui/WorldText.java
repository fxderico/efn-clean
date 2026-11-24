package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;

import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.client.HudModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class WorldText {

    public static final int NAME = HudChrome.TEXT_NAME;

    public static final int LABEL = HudChrome.TEXT_LABEL;

    private static final float LINE_GAP = 1.0F;

    private static final float ANCHOR_GAP = 2.0F;

    private WorldText() {}

    public static int hudAccent() {
        HudModule module = ModuleRegistry.get(HudModule.class);
        return module != null && module.customAccent()
                ? GuiPalette.abgr(module.accentArgb())
                : GuiPalette.accent();
    }

    public static void draw(
            ImDrawList imDrawList,
            float f,
            float g,
            String string,
            String[] strings,
            int[] ints,
            float h) {
        if (!(h <= 0.001F)) {
            ImGuiManager imGuiManager = ImGuiManager.getInstance();
            float uiScale = imGuiManager.uiScale();
            ImFont imFont = imGuiManager.hudTextFont();
            ImFont imFont2 = imGuiManager.hudLabelFont();
            int px = HudChrome.px(17.0F, uiScale);
            int px2 = HudChrome.px(14.0F, uiScale);
            boolean bl = strings.length > 0;
            float f2 = Math.round(g - 2.0F * uiScale - (bl ? px2 + 1.0F * uiScale : 0.0F) - px);
            float width = HudChrome.width(imFont, px, string);
            HudChrome.text(
                    imDrawList,
                    imFont,
                    px,
                    Math.round(f - width * 0.5F),
                    f2,
                    HudChrome.scaleAlpha(NAME, h),
                    string,
                    true);
            if (bl) {
                float g2 = 0.0F;
                for (String string2 : strings) {
                    g2 += HudChrome.width(imFont2, px2, string2);
                }
                float h2 = Math.round(f - g2 * 0.5F);
                float f3 = Math.round(f2 + px + 1.0F * uiScale);
                for (int k = 0; k < strings.length; k++) {
                    HudChrome.text(
                            imDrawList,
                            imFont2,
                            px2,
                            Math.round(h2),
                            f3,
                            HudChrome.scaleAlpha(ints[k], h),
                            strings[k],
                            true);
                    h2 += HudChrome.width(imFont2, px2, strings[k]);
                }
            }
        }
    }
}
