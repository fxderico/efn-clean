package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.type.ImString;

import net.efn.client.imgui.ImGuiManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class EfnWidgets {

    private static final float POPUP_MAX_H = 280.0F;

    private static final Motion.Store ANIM = new Motion.Store();

    private EfnWidgets() {}

    public static float s(float f) {
        return f * ImGuiManager.getInstance().uiScale();
    }

    public static float animate(int i, boolean bl, float f) {
        return ease(i, bl ? 1.0F : 0.0F, f);
    }

    private static float ease(int i, float f, float g) {
        return ANIM.ease(
                i, f, ClickGuiRenderer.animationDelta(ImGuiManager.getInstance().frameDelta()), g);
    }

    public static void text(
            ImDrawList imDrawList, ImFont imFont, float f, float g, int i, String string) {
        text(imDrawList, imFont, f, g, i, string, false);
    }

    public static void text(
            ImDrawList imDrawList,
            ImFont imFont,
            float f,
            float g,
            int i,
            String string,
            boolean bl) {
        int j = imFont != null ? Math.round(imFont.getFontSize()) : 0;
        HudChrome.text(imDrawList, imFont, j, f, g, c(i), string, bl);
    }

    public static float textWidth(ImFont imFont, String string) {
        return imFont != null
                ? imFont.calcTextSizeAX(imFont.getFontSize(), 3.4028235E38F, 0.0F, string)
                : ImGui.calcTextSize(string).x;
    }

    public static float lineHeight(ImFont imFont) {
        return imFont != null ? imFont.getFontSize() : ImGui.getTextLineHeight();
    }

    public static String ellipsize(ImFont imFont, String string, float f) {
        if (textWidth(imFont, string) <= f) {
            return string;
        } else {
            int i = string.length();
            while (i > 0 && textWidth(imFont, string.substring(0, i) + "...") > f) {
                i--;
            }
            return string.substring(0, i).stripTrailing() + "...";
        }
    }

    public static int c(int i) {
        return ImGui.getColorU32i(i);
    }

    public static int withAlpha(int i, float f) {
        int j = Math.round((i >>> 24 & 0xFF) * Math.max(0.0F, Math.min(1.0F, f)));
        return j << 24 | i & 16777215;
    }

    public static void drawSwitch(
            ImDrawList imDrawList, float f, float g, float h, float f2, float g2) {
        float h2 = f2 * 0.5F;
        imDrawList.addRectFilled(
                f, g, f + h, g + f2, c(EfnTheme.lerpColor(-11911366, GuiPalette.accent(), g2)), h2);
        float f3 = h2 - s(2.5F);
        float g3 = f + h2 + (h - 2.0F * h2) * g2;
        imDrawList.addCircleFilled(g3, g + h2 + s(1.0F), f3, c(855638016), 24);
        imDrawList.addCircleFilled(g3, g + h2, f3, c(-461067), 24);
    }

    public static boolean sliderFloat(String string, float[] floats, float f, float g, float h) {
        return sliderFloat(string, floats, f, g, h, null);
    }

    public static boolean sliderFloat(
            String string, float[] floats, float f, float g, float h, EfnWidgets.Snap snap) {
        float px18 = s(18.0F);
        float cursorScreenPosX = ImGui.getCursorScreenPosX();
        float f2 = ImGui.getCursorScreenPosY() + px18 * 0.5F;
        ImGui.invisibleButton(string, h, px18);
        boolean itemHovered = ImGui.isItemHovered();
        boolean itemActive = ImGui.isItemActive();
        float animate = animate(ImGui.getItemID(), itemHovered || itemActive, 16.0F);
        boolean bl = false;
        if (itemActive) {
            float g2 =
                    Math.max(
                            0.0F,
                            Math.min(
                                    1.0F,
                                    (ImGui.getIO().getMousePosX() - cursorScreenPosX)
                                            / Math.max(1.0F, h)));
            float h2 = f + g2 * (g - f);
            if (snap != null) {
                h2 = snap.apply(h2);
            }
            if (h2 != floats[0]) {
                floats[0] = h2;
                bl = true;
            }
        }
        float frac = frac(floats[0], f, g);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        float px4 = s(4.0F);
        float f3 = s(6.5F) + s(1.5F) * animate;
        float px7 = s(7.0F);
        float g3 = cursorScreenPosX + px7;
        float h3 = cursorScreenPosX + h - px7;
        float f4 = g3 + frac * (h3 - g3);
        windowDrawList.addRectFilled(g3, f2 - px4 * 0.5F, h3, f2 + px4 * 0.5F, c(-12240588), px4);
        if (f4 > g3 + 0.5F) {
            windowDrawList.addRectFilled(
                    g3, f2 - px4 * 0.5F, f4, f2 + px4 * 0.5F, c(GuiPalette.accent()), px4);
        }
        if (animate > 0.01F) {
            windowDrawList.addCircleFilled(
                    f4, f2, f3 + s(4.0F), c(withAlpha(GuiPalette.accent(), 0.25F * animate)), 24);
        }
        windowDrawList.addCircleFilled(f4, f2 + s(1.0F), f3, c(1073741824), 24);
        windowDrawList.addCircleFilled(f4, f2, f3, c(-461067), 24);
        return bl;
    }

    public static boolean chipButton(String string, String string2, float f, float g, boolean bl) {
        float cursorScreenPosX = ImGui.getCursorScreenPosX();
        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        boolean invisibleButton = ImGui.invisibleButton(string, f, g);
        boolean itemHovered = ImGui.isItemHovered();
        boolean itemActive = ImGui.isItemActive();
        float animate = animate(ImGui.getItemID(), itemHovered, 18.0F);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        int i =
                bl
                        ? EfnTheme.lerpColor(
                                GuiPalette.accent(), GuiPalette.accentHover(), animate * 0.35F)
                        : EfnTheme.lerpColor(-13226454, -12240588, animate);
        if (itemActive) {
            i = EfnTheme.lerpColor(i, -16777216, 0.12F);
        }
        windowDrawList.addRectFilled(
                cursorScreenPosX,
                cursorScreenPosY,
                cursorScreenPosX + f,
                cursorScreenPosY + g,
                c(i),
                s(7.0F));
        ImFont font = ImGui.getFont();
        float textWidth = textWidth(font, string2);
        text(
                windowDrawList,
                font,
                cursorScreenPosX + (f - textWidth) * 0.5F,
                cursorScreenPosY + (g - lineHeight(font)) * 0.5F,
                bl ? -1 : -461582,
                string2);
        return invisibleButton;
    }

    public static boolean iconButton(String string, float f) {
        float cursorScreenPosX = ImGui.getCursorScreenPosX();
        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        boolean invisibleButton = ImGui.invisibleButton(string, f, f);
        float animate = animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
        ImGui.getWindowDrawList()
                .addCircleFilled(
                        cursorScreenPosX + f * 0.5F,
                        cursorScreenPosY + f * 0.5F,
                        f * 0.5F,
                        c(EfnTheme.lerpColor(-13226454, -12240588, animate)),
                        32);
        return invisibleButton;
    }

    public static int segmented(String string, String[] strings, int i, float f, float g) {
        float cursorPosX = ImGui.getCursorPosX();
        float cursorPosY = ImGui.getCursorPosY();
        float cursorScreenPosX = ImGui.getCursorScreenPosX();
        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        float px3 = s(3.0F);
        float px8 = s(8.0F);
        float h = (f - px3 * 2.0F) / strings.length;
        float f2 = ease(ImGui.getID(string), px3 + i * h, 18.0F);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        windowDrawList.addRectFilled(
                cursorScreenPosX,
                cursorScreenPosY,
                cursorScreenPosX + f,
                cursorScreenPosY + g,
                c(-13226454),
                px8);
        windowDrawList.addRectFilled(
                cursorScreenPosX + f2,
                cursorScreenPosY + px3,
                cursorScreenPosX + f2 + h,
                cursorScreenPosY + g - px3,
                c(GuiPalette.accent()),
                px8 - px3);
        ImFont font = ImGui.getFont();
        int j = -1;
        for (int k = 0; k < strings.length; k++) {
            ImGui.setCursorPos(cursorPosX + px3 + k * h, cursorPosY + px3);
            if (ImGui.invisibleButton(string + k, h, g - px3 * 2.0F)) {
                j = k;
            }
            float animate = animate(ImGui.getItemID(), ImGui.isItemHovered(), 18.0F);
            float g2 = cursorScreenPosX + px3 + k * h + h * 0.5F;
            float h2 =
                    Math.max(
                            0.0F,
                            1.0F - Math.abs(g2 - (cursorScreenPosX + f2 + h * 0.5F)) / (h * 0.5F));
            int lerpColor =
                    EfnTheme.lerpColor(EfnTheme.lerpColor(-4542808, -461582, animate), -1, h2);
            float textWidth = textWidth(font, strings[k]);
            text(
                    windowDrawList,
                    font,
                    g2 - textWidth * 0.5F,
                    cursorScreenPosY + (g - lineHeight(font)) * 0.5F,
                    lerpColor,
                    strings[k]);
        }
        return j;
    }

    public static int dropdown(String string, String string2, List<String> list, float f, float g) {
        return dropdown(string, string2, string2, list, f, g, null);
    }

    public static int dropdown(
            String string,
            String string2,
            String string3,
            List<String> list,
            float f,
            float g,
            ImString imString) {
        String string4 = string + "_popup";
        float cursorScreenPosX = ImGui.getCursorScreenPosX();
        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        if (ImGui.invisibleButton(string, f, g)) {
            ImGui.openPopup(string4);
            if (imString != null) {
                imString.set("");
            }
        }
        boolean popupOpen = ImGui.isPopupOpen(string4);
        float animate = animate(ImGui.getItemID(), ImGui.isItemHovered() || popupOpen, 18.0F);
        ImDrawList windowDrawList = ImGui.getWindowDrawList();
        float px7 = s(7.0F);
        windowDrawList.addRectFilled(
                cursorScreenPosX,
                cursorScreenPosY,
                cursorScreenPosX + f,
                cursorScreenPosY + g,
                c(EfnTheme.lerpColor(-13226454, -12240588, animate)),
                px7);
        if (popupOpen) {
            windowDrawList.addRect(
                    cursorScreenPosX,
                    cursorScreenPosY,
                    cursorScreenPosX + f,
                    cursorScreenPosY + g,
                    c(withAlpha(GuiPalette.accent(), 0.8F)),
                    px7,
                    0,
                    s(1.2F));
        }
        ImFont font = ImGui.getFont();
        text(
                windowDrawList,
                font,
                cursorScreenPosX + s(10.0F),
                cursorScreenPosY + (g - lineHeight(font)) * 0.5F,
                string2.isEmpty() ? -4542808 : -461582,
                ellipsize(font, string3, f - s(34.0F)));
        chevron(
                windowDrawList,
                cursorScreenPosX + f - s(14.0F),
                cursorScreenPosY + g * 0.5F,
                s(4.0F),
                -4542808,
                popupOpen);
        int i = -1;
        float animate2 = animate(ImGui.getID(string4), popupOpen, 22.0F);
        float px30 = s(30.0F);
        float h = ImGui.getIO().getDisplaySizeY() - (cursorScreenPosY + g);
        float f2 = s(6.0F) * (1.0F - animate2);
        if (h < s(290.0F) && cursorScreenPosY > h) {
            ImGui.setNextWindowPos(
                    cursorScreenPosX, cursorScreenPosY - s(4.0F) + f2, 1, 0.0F, 1.0F);
        } else {
            ImGui.setNextWindowPos(cursorScreenPosX, cursorScreenPosY + g + s(4.0F) - f2);
        }
        ImGui.setNextWindowSizeConstraints(f, 0.0F, f, s(280.0F));
        ImGui.pushStyleVar(0, Math.max(0.05F, animate2));
        ImGui.pushStyleVar(2, s(4.0F), s(4.0F));
        ImGui.pushStyleVar(14, 0.0F, s(2.0F));
        if (ImGui.beginPopup(string4, 8)) {
            float g2 = f - s(8.0F);
            String string5 = "";
            if (imString != null) {
                if (ImGui.isWindowAppearing()) {
                    ImGui.setKeyboardFocusHere();
                }
                ImGui.pushStyleVar(11, s(8.0F), s(6.0F));
                ImGui.setNextItemWidth(g2);
                ImGui.inputTextWithHint("##search", "Search...", imString);
                boolean submittedWithEnter = submittedWithEnter();
                ImGui.popStyleVar();
                string5 = imString.get().trim().toLowerCase(Locale.ROOT);
                if (submittedWithEnter) {
                    for (int j = 0; j < list.size() && i < 0; j++) {
                        if (matches((String) list.get(j), string5)) {
                            i = j;
                        }
                    }
                    if (i >= 0) {
                        ImGui.closeCurrentPopup();
                    }
                }
            }
            ImDrawList windowDrawList2 = ImGui.getWindowDrawList();
            boolean bl = false;
            for (int k = 0; k < list.size(); k++) {
                String string6 = (String) list.get(k);
                if (matches(string6, string5)) {
                    bl = true;
                    float cursorScreenPosX2 = ImGui.getCursorScreenPosX();
                    float cursorScreenPosY2 = ImGui.getCursorScreenPosY();
                    if (ImGui.invisibleButton("##opt" + k, g2, px30)) {
                        i = k;
                        ImGui.closeCurrentPopup();
                    }
                    boolean bl2 = string6.equals(string2);
                    float animate3 = animate(ImGui.getItemID(), ImGui.isItemHovered(), 20.0F);
                    if (animate3 > 0.01F) {
                        windowDrawList2.addRectFilled(
                                cursorScreenPosX2,
                                cursorScreenPosY2,
                                cursorScreenPosX2 + g2,
                                cursorScreenPosY2 + px30,
                                c(withAlpha(-12240588, animate3)),
                                s(6.0F));
                    }
                    text(
                            windowDrawList2,
                            font,
                            cursorScreenPosX2 + s(10.0F),
                            cursorScreenPosY2 + (px30 - lineHeight(font)) * 0.5F,
                            bl2 ? GuiPalette.accent() : -461582,
                            ellipsize(font, string6, g2 - s(34.0F)));
                    if (bl2) {
                        check(
                                windowDrawList2,
                                cursorScreenPosX2 + g2 - s(14.0F),
                                cursorScreenPosY2 + px30 * 0.5F,
                                s(5.0F),
                                GuiPalette.accent());
                    }
                }
            }
            if (!bl) {
                float cursorScreenPosX3 = ImGui.getCursorScreenPosX();
                float cursorScreenPosY3 = ImGui.getCursorScreenPosY();
                ImGui.dummy(g2, px30);
                text(
                        windowDrawList2,
                        font,
                        cursorScreenPosX3 + s(10.0F),
                        cursorScreenPosY3 + (px30 - lineHeight(font)) * 0.5F,
                        -4542808,
                        list.isEmpty() ? "Nothing to pick yet" : "No matches");
            }
            ImGui.endPopup();
        }
        ImGui.popStyleVar(3);
        return i;
    }

    private static boolean matches(String string, String string2) {
        return string2.isEmpty() || string.toLowerCase(Locale.ROOT).contains(string2);
    }

    public static boolean submittedWithEnter() {
        return ImGui.isItemDeactivated()
                && (ImGui.isKeyPressed(525, false) || ImGui.isKeyPressed(627, false));
    }

    public static void chevron(
            ImDrawList imDrawList, float f, float g, float h, int i, boolean bl) {
        float f2 = bl ? -h * 0.5F : h * 0.5F;
        imDrawList.addLine(f - h, g - f2, f, g + f2, c(i), s(1.6F));
        imDrawList.addLine(f, g + f2, f + h, g - f2, c(i), s(1.6F));
    }

    public static void chevronLeft(ImDrawList imDrawList, float f, float g, float h, int i) {
        imDrawList.addLine(f + h * 0.5F, g - h, f - h * 0.5F, g, c(i), s(2.0F));
        imDrawList.addLine(f - h * 0.5F, g, f + h * 0.5F, g + h, c(i), s(2.0F));
    }

    public static void chevronRight(ImDrawList imDrawList, float f, float g, float h, int i) {
        imDrawList.addLine(f - h * 0.5F, g - h, f + h * 0.5F, g, c(i), s(2.0F));
        imDrawList.addLine(f + h * 0.5F, g, f - h * 0.5F, g + h, c(i), s(2.0F));
    }

    public static void chevronRotated(
            ImDrawList imDrawList, float f, float g, float h, int i, float f2) {
        float g2 = (float) Math.cos(f2);
        float h2 = (float) Math.sin(f2);
        float f3 = f + g2 * h * 0.5F;
        float g3 = g + h2 * h * 0.5F;
        float h3 = f - g2 * h * 0.5F;
        float f4 = g - h2 * h * 0.5F;
        imDrawList.addLine(h3 - h2 * h, f4 + g2 * h, f3, g3, c(i), s(1.6F));
        imDrawList.addLine(f3, g3, h3 + h2 * h, f4 - g2 * h, c(i), s(1.6F));
    }

    public static void check(ImDrawList imDrawList, float f, float g, float h, int i) {
        imDrawList.addLine(f - h, g, f - h * 0.3F, g + h * 0.7F, c(i), s(1.8F));
        imDrawList.addLine(f - h * 0.3F, g + h * 0.7F, f + h, g - h * 0.7F, c(i), s(1.8F));
    }

    public static void cross(ImDrawList imDrawList, float f, float g, float h, int i) {
        imDrawList.addLine(f - h, g - h, f + h, g + h, c(i), s(1.6F));
        imDrawList.addLine(f - h, g + h, f + h, g - h, c(i), s(1.6F));
    }

    public static void magnifier(ImDrawList imDrawList, float f, float g, float h, int i) {
        float f2 = h * 0.62F;
        imDrawList.addCircle(f - h * 0.15F, g - h * 0.15F, f2, c(i), 20, s(1.6F));
        imDrawList.addLine(f + h * 0.3F, g + h * 0.3F, f + h, g + h, c(i), s(1.8F));
    }

    public static void crosshair(ImDrawList imDrawList, float f, float g, float h, int i) {
        float px1_5 = s(1.5F);
        imDrawList.addCircle(f, g, h * 0.62F, c(i), 20, px1_5);
        imDrawList.addLine(f, g - h, f, g - h * 0.35F, c(i), px1_5);
        imDrawList.addLine(f, g + h * 0.35F, f, g + h, c(i), px1_5);
        imDrawList.addLine(f - h, g, f - h * 0.35F, g, c(i), px1_5);
        imDrawList.addLine(f + h * 0.35F, g, f + h, g, c(i), px1_5);
    }

    public static void move(ImDrawList imDrawList, float f, float g, float h, int i) {
        int j = c(i);
        float px1_5 = s(1.5F);
        float f2 = h * 0.42F;
        float g2 = h - f2 * 0.8F;
        imDrawList.addLine(f - g2, g, f + g2, g, j, px1_5);
        imDrawList.addLine(f, g - g2, f, g + g2, j, px1_5);
        imDrawList.addTriangleFilled(f - h, g, f - h + f2, g - f2, f - h + f2, g + f2, j);
        imDrawList.addTriangleFilled(f + h, g, f + h - f2, g + f2, f + h - f2, g - f2, j);
        imDrawList.addTriangleFilled(f, g - h, f + f2, g - h + f2, f - f2, g - h + f2, j);
        imDrawList.addTriangleFilled(f, g + h, f - f2, g + h - f2, f + f2, g + h - f2, j);
    }

    public static void grid(ImDrawList imDrawList, float f, float g, float h, int i) {
        float f2 = h * 0.18F;
        float g2 = h * 0.8F;
        imDrawList.addRectFilled(f - g2, g - g2, f - f2, g - f2, c(i), s(1.5F));
        imDrawList.addRectFilled(f + f2, g - g2, f + g2, g - f2, c(i), s(1.5F));
        imDrawList.addRectFilled(f - g2, g + f2, f - f2, g + g2, c(i), s(1.5F));
        imDrawList.addRectFilled(f + f2, g + f2, f + g2, g + g2, c(i), s(1.5F));
    }

    public static void ring(ImDrawList imDrawList, float f, float g, float h, int i) {
        imDrawList.addCircle(f, g, h * 0.62F, c(i), 24, h * 0.42F);
    }

    public static void eye(ImDrawList imDrawList, float f, float g, float h, int i) {
        float px1_5 = s(1.5F);
        imDrawList.addBezierQuadratic(f - h, g, f, g - h * 1.15F, f + h, g, c(i), px1_5, 16);
        imDrawList.addBezierQuadratic(f - h, g, f, g + h * 1.15F, f + h, g, c(i), px1_5, 16);
        imDrawList.addCircleFilled(f, g, h * 0.3F, c(i), 16);
    }

    public static void monitor(ImDrawList imDrawList, float f, float g, float h, int i) {
        float px1_5 = s(1.5F);
        imDrawList.addRect(f - h, g - h * 0.8F, f + h, g + h * 0.4F, c(i), s(2.0F), 0, px1_5);
        imDrawList.addLine(f, g + h * 0.4F, f, g + h * 0.8F, c(i), px1_5);
        imDrawList.addLine(f - h * 0.5F, g + h * 0.85F, f + h * 0.5F, g + h * 0.85F, c(i), px1_5);
    }

    public static void scroll(ImDrawList imDrawList, float f, float g, float h, int i) {
        float px1_5 = s(1.5F);
        float f2 = h * 0.36F;
        float g2 = h * 0.85F;
        float h2 = h * 0.62F;
        imDrawList.addRect(f - h, g - g2, f - h + f2, g + g2, c(i), f2 * 0.5F, 0, px1_5);
        imDrawList.addRect(f + h - f2, g - g2, f + h, g + g2, c(i), f2 * 0.5F, 0, px1_5);
        imDrawList.addLine(f - h + f2, g - h2, f + h - f2, g - h2, c(i), px1_5);
        imDrawList.addLine(f - h + f2, g + h2, f + h - f2, g + h2, c(i), px1_5);
        imDrawList.addLine(f - h * 0.3F, g - h * 0.15F, f + h * 0.3F, g - h * 0.15F, c(i), px1_5);
        imDrawList.addLine(f - h * 0.3F, g + h * 0.22F, f + h * 0.12F, g + h * 0.22F, c(i), px1_5);
    }

    public static void house(ImDrawList imDrawList, float f, float g, float h, int i) {
        float px1_5 = s(1.5F);
        imDrawList.addLine(f - h, g - h * 0.05F, f, g - h * 0.95F, c(i), px1_5);
        imDrawList.addLine(f, g - h * 0.95F, f + h, g - h * 0.05F, c(i), px1_5);
        imDrawList.addRect(
                f - h * 0.7F, g - h * 0.3F, f + h * 0.7F, g + h * 0.85F, c(i), 0.0F, 0, px1_5);
        imDrawList.addRectFilled(f - h * 0.2F, g + h * 0.25F, f + h * 0.2F, g + h * 0.85F, c(i));
    }

    public static void bolt(ImDrawList imDrawList, float f, float g, float h, int i) {
        float px1_5 = s(1.5F);
        float[] floats =
                new float[] {
                    f + h * 0.25F,
                    f - h * 0.45F,
                    f + h * 0.05F,
                    f - h * 0.25F,
                    f + h * 0.45F,
                    f - h * 0.05F
                };
        float[] floats2 =
                new float[] {
                    g - h, g + h * 0.1F, g + h * 0.1F, g + h, g - h * 0.15F, g - h * 0.15F
                };
        for (int j = 0; j < 6; j++) {
            int k = (j + 1) % 6;
            imDrawList.addLine(floats[j], floats2[j], floats[k], floats2[k], c(i), px1_5);
        }
    }

    private static float frac(float f, float g, float h) {
        float f2 = h - g;
        return f2 < 1.0E-6F ? 0.0F : Math.max(0.0F, Math.min(1.0F, (f - g) / f2));
    }

    @FunctionalInterface
    @Environment(EnvType.CLIENT)
    public interface Snap {

        float apply(float f);
    }
}
