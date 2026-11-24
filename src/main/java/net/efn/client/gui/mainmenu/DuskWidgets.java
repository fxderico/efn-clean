package net.efn.client.gui.mainmenu;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiInputTextCallbackData;
import imgui.callback.ImGuiInputTextCallback;
import imgui.type.ImString;

import net.efn.client.gui.HudDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;

@Environment(EnvType.CLIENT)
public final class DuskWidgets {

    public static final float BUTTON_H = 42.0F;

    public static final float RING = 42.0F;

    public static final float GAP = 11.0F;

    public static final float TAG_H = 24.0F;

    public static final float FIELD_H = 40.0F;

    public static final float PANEL_PAD = 22.0F;

    public static final float DIALOG_W = 560.0F;

    private static final float PANEL_ROUNDING = 8.0F;

    private static final float ROW_ROUNDING = 6.0F;

    private static final float FIELD_ROUNDING = 0.0F;

    private static final float FIELD_PAD = 14.0F;

    private static final float SCROLL_STEP = 64.0F;

    private static final float SCROLLBAR_W = 3.0F;

    private static final float SCROLLBAR_INSET = 5.0F;

    private static final float SCROLL_THUMB_MIN = 28.0F;

    private static final float SCROLLBAR_GRAB = 14.0F;

    private static final float BUTTON_TRACK = 3.0F;

    private static final float TAG_TRACK = 4.0F;

    private static final float DIALOG_FADE = 0.18F;

    private static final float DIALOG_VEIL = 0.62F;

    private static final float LINE_SPACING = 1.35F;

    private static final float EASE = 12.0F;

    private final Map<Object, DuskWidgets.Anim> anims = new HashMap<>();

    private final Map<Object, DuskWidgets.Scroll> scrolls = new HashMap<>();

    private final ArrayDeque<DuskWidgets.Scroll> openAreas = new ArrayDeque<>();

    private final ArrayDeque<float[]> clips = new ArrayDeque<>();

    private MenuFrame f;

    private int frame;

    private boolean click;

    private boolean doubleClick;

    private boolean mouseDown;

    private float wheel;

    private boolean blocked;

    private float alpha = 1.0F;

    public DuskWidgets() {}

    public void begin(MenuFrame menuFrame) {
        this.f = menuFrame;
        this.frame++;
        this.anims.values().removeIf(anim -> anim.frame < this.frame - 1);
        this.click = menuFrame.clicked();
        this.doubleClick = this.click && ImGui.isMouseDoubleClicked(0);
        this.mouseDown = ImGui.isMouseDown(0);
        this.wheel = ImGui.getIO().getMouseWheel();
        this.blocked = false;
        this.alpha = 1.0F;
        this.openAreas.clear();
        this.clips.clear();
    }

    public void end() {
        while (!this.openAreas.isEmpty()) {
            this.openAreas.pop();
            this.popClip();
        }
    }

    public MenuFrame frame() {
        return this.f;
    }

    public void alpha(float alpha2) {
        this.alpha = alpha2;
    }

    public float alpha() {
        return this.alpha;
    }

    public boolean hovered(float g, float f2, float g2, float h2) {
        if (this.blocked) {
            return false;
        } else {
            float mouseX = this.f.mouseX();
            float mouseY = this.f.mouseY();
            if (!(mouseX < g) && !(mouseX > g + g2) && !(mouseY < f2) && !(mouseY > f2 + h2)) {
                float[] clip = this.clips.peek();
                return clip == null
                        || mouseX >= clip[0]
                                && mouseX <= clip[2]
                                && mouseY >= clip[1]
                                && mouseY <= clip[3];
            } else {
                return false;
            }
        }
    }

    public boolean pressed(float g, float f2, float g2, float h2) {
        if (this.click && this.hovered(g, f2, g2, h2)) {
            this.click = false;
            return true;
        } else {
            return false;
        }
    }

    public float hover(Object object, boolean bl) {
        DuskWidgets.Anim anim =
                this.anims.computeIfAbsent(object, objectx -> new DuskWidgets.Anim());
        anim.value =
                anim.value
                        + ((bl ? 1.0F : 0.0F) - anim.value) * Math.min(1.0F, this.f.dt() * 12.0F);
        anim.frame = this.frame;
        return anim.value;
    }

    public float hoverOf(Object object) {
        DuskWidgets.Anim anim = this.anims.get(object);
        return anim != null ? anim.value : 0.0F;
    }

    public void tooltip(String string, float g, float f2) {
        this.f.tooltip(string, g, f2);
    }

    public boolean button(Object object, String string, float g, float f2, float g2, float h2) {
        return this.button(object, string, g, f2, g2, h2, true);
    }

    public boolean button(
            Object object, String string, float g, float f2, float g2, float h2, boolean bl) {
        boolean bl2 = bl && this.hovered(g, f2, g2, h2);
        boolean bl3 = bl2 && this.pressed(g, f2, g2, h2);
        float hover = this.hover(object, bl2);
        plateButton(
                this.f.bg(),
                this.f.menuFont(),
                g,
                f2,
                g2,
                h2,
                string.toUpperCase(Locale.ROOT),
                bl,
                hover,
                this.f.scale(),
                this.alpha);
        return bl3;
    }

    public boolean iconButton(
            Object object, DuskWidgets.Icon icon, String string, float g, float f2, float g2) {
        boolean hovered = this.hovered(g, f2, g2, g2);
        boolean bl = hovered && this.pressed(g, f2, g2, g2);
        float hover = this.hover(object, hovered);
        float scale = this.f.scale();
        ringButton(this.f.bg(), icon, g, f2, g2, hover, scale, this.alpha);
        if (string != null && hover > 0.5F) {
            this.f.tooltip(string, g + g2 * 0.5F, f2 + g2 + 8.0F * scale);
        }
        return bl;
    }

    public void panel(float g, float f2, float g2, float h2, String string) {
        float scale = this.f.scale();
        panel(this.f.bg(), g, f2, g2, h2, scale, this.alpha);
        if (string != null) {
            float f3 = 24.0F * scale;
            tag(
                    this.f.bg(),
                    this.f.tagFont(),
                    g + g2 * 0.5F,
                    f2 - f3 * 0.5F,
                    f3,
                    string.toUpperCase(Locale.ROOT),
                    scale,
                    this.alpha);
        }
    }

    public int row(Object object, float g, float f2, float g2, float h2, boolean bl) {
        boolean hovered = this.hovered(g, f2, g2, h2);
        int i = 0;
        if (hovered && this.pressed(g, f2, g2, h2)) {
            i = this.doubleClick ? 2 : 1;
        }
        float hover = this.hover(object, hovered);
        row(this.f.bg(), g, f2, g2, h2, hover, bl, this.f.scale(), this.alpha);
        return i;
    }

    public boolean textField(
            DuskWidgets.TextField textField, float g, float f2, float nextItemWidth, float g2) {
        ImDrawList imDrawList = this.f.bg();
        float scale = this.f.scale();
        boolean hovered = this.hovered(g, f2, nextItemWidth, g2);
        if (hovered && this.click) {
            this.click = false;
        }
        float hover = this.hover(textField, hovered);
        textField.focus =
                textField.focus
                        + ((textField.active ? 1.0F : 0.0F) - textField.focus)
                                * Math.min(1.0F, this.f.dt() * 12.0F);
        field(imDrawList, g, f2, nextItemWidth, g2, hover, textField.focus, scale, this.alpha);
        ImFont imFont = this.f.bodyFont();
        float h2 = 14.0F * scale;
        float f3 = Math.max(0.0F, (g2 - this.f.textHeight(imFont)) * 0.5F);
        if (this.blocked) {
            textField.active = false;
            String text = textField.text();
            boolean bl = text.isEmpty();
            String string =
                    bl
                            ? textField.placeholder
                            : (textField.password
                                    ? "*".repeat(text.codePointCount(0, text.length()))
                                    : text);
            imDrawList.pushClipRect(g + h2, f2, g + nextItemWidth - h2, f2 + g2, true);
            this.f.text(
                    imDrawList,
                    imFont,
                    g + h2,
                    f2 + f3,
                    bl ? -7693914 : -1575946,
                    this.alpha,
                    string);
            imDrawList.popClipRect();
            return false;
        } else {
            ImGui.setCursorScreenPos(g, f2);
            if (imFont != null) {
                ImGui.pushFont(imFont);
            }
            ImGui.pushStyleVar(11, h2, f3);
            ImGui.pushStyleVar(13, 0.0F);
            ImGui.pushStyleVar(12, 0.0F);
            ImGui.pushStyleColor(7, 0);
            ImGui.pushStyleColor(8, 0);
            ImGui.pushStyleColor(9, 0);
            ImGui.pushStyleColor(53, 0);
            ImGui.pushStyleColor(0, HudDraw.col(-1575946, this.alpha));
            ImGui.pushStyleColor(1, HudDraw.col(-7693914, this.alpha));
            ImGui.pushStyleColor(51, HudDraw.col(-15408912, 0.35F * this.alpha));
            boolean bl2;
            try {
                ImGui.setNextItemWidth(nextItemWidth);
                if (textField.focusRequested) {
                    ImGui.setKeyboardFocusHere();
                    textField.focusRequested = false;
                }
                boolean inputTextWithHint =
                        ImGui.inputTextWithHint(
                                textField.id,
                                textField.placeholder,
                                textField.buffer,
                                textField.flags(),
                                textField.limiter);
                textField.active = ImGui.isItemActive();
                bl2 = inputTextWithHint;
            } finally {
                ImGui.popStyleColor(7);
                ImGui.popStyleVar(3);
                if (imFont != null) {
                    ImGui.popFont();
                }
            }
            return bl2;
        }
    }

    public float beginScroll(Object object, float g, float f2, float g2, float h2, float f3) {
        DuskWidgets.Scroll scroll =
                this.scrolls.computeIfAbsent(object, objectx -> new DuskWidgets.Scroll());
        float scale = this.f.scale();
        scroll.x = g;
        scroll.y = f2;
        scroll.w = g2;
        scroll.h = h2;
        scroll.content = f3;
        float g3 = Math.max(0.0F, f3 - h2);
        if (this.wheel != 0.0F && this.hovered(g, f2, g2, h2)) {
            scroll.target = scroll.target - this.wheel * 64.0F * scale;
            this.wheel = 0.0F;
        }
        if (g3 > 0.0F) {
            float[] floats = this.thumb(scroll, scale);
            if (Float.isNaN(scroll.grab)) {
                float h3 = floats[0] + floats[2] * 0.5F - 14.0F * scale * 0.5F;
                if (this.pressed(h3, floats[1], 14.0F * scale, floats[3])) {
                    scroll.grab = this.f.mouseY() - floats[1];
                }
            } else if (this.mouseDown && !this.blocked) {
                float f4 = h2 - 10.0F * scale - floats[3];
                float g4 =
                        (this.f.mouseY() - scroll.grab - (f2 + 5.0F * scale)) / Math.max(1.0F, f4);
                scroll.target = scroll.offset = g4 * g3;
            } else {
                scroll.grab = 0.0F / 0.0F;
            }
        } else {
            scroll.grab = 0.0F / 0.0F;
        }
        scroll.target = Math.max(0.0F, Math.min(g3, scroll.target));
        scroll.offset =
                scroll.offset
                        + (scroll.target - scroll.offset) * Math.min(1.0F, this.f.dt() * 16.0F);
        if (Math.abs(scroll.target - scroll.offset) < 0.5F) {
            scroll.offset = scroll.target;
        }
        scroll.offset = Math.max(0.0F, Math.min(g3, scroll.offset));
        this.openAreas.push(scroll);
        this.pushClip(g, f2, g + g2, f2 + h2);
        return Math.round(scroll.offset);
    }

    public void endScroll() {
        DuskWidgets.Scroll openArea = this.openAreas.pop();
        this.popClip();
        float g = openArea.content - openArea.h;
        if (!(g <= 0.0F)) {
            float scale = this.f.scale();
            float[] floats = this.thumb(openArea, scale);
            float f2 = openArea.y + 5.0F * scale;
            float g2 = openArea.h - 10.0F * scale;
            ImDrawList imDrawList = this.f.bg();
            boolean hovered =
                    this.hovered(
                            floats[0] - 14.0F * scale * 0.5F, floats[1], 14.0F * scale, floats[3]);
            float hover = this.hover(openArea, hovered || !Float.isNaN(openArea.grab));
            imDrawList.addRectFilled(
                    floats[0],
                    f2,
                    floats[0] + floats[2],
                    f2 + g2,
                    HudDraw.col(-15408912, 0.12F * this.alpha),
                    floats[2]);
            imDrawList.addRectFilled(
                    floats[0],
                    floats[1],
                    floats[0] + floats[2],
                    floats[1] + floats[3],
                    HudDraw.col(
                            HudDraw.mix(-15408912, -6621958, hover),
                            (0.55F + 0.35F * hover) * this.alpha),
                    floats[2]);
        }
    }

    public void pushClip(float g, float f2, float g2, float h2) {
        float[] clip = this.clips.peek();
        if (clip != null) {
            g = Math.max(g, clip[0]);
            f2 = Math.max(f2, clip[1]);
            g2 = Math.min(g2, clip[2]);
            h2 = Math.min(h2, clip[3]);
        }
        this.clips.push(new float[] {g, f2, g2, h2});
        ImGui.pushClipRect(g, f2, g2, h2, true);
    }

    public void popClip() {
        this.clips.pop();
        ImGui.popClipRect();
    }

    public void dialog(DuskWidgets.Dialog dialog) {
        ImDrawList imDrawList = this.f.bg();
        float scale = this.f.scale();
        float width = this.f.width();
        float height = this.f.height();
        if (Float.isNaN(dialog.openedAt)) {
            dialog.openedAt = this.f.time();
        }
        float g = Math.min(1.0F, (this.f.time() - dialog.openedAt) / 0.18F);
        float f2 = 1.0F - (1.0F - g) * (1.0F - g) * (1.0F - g);
        float alpha2 = this.alpha;
        this.alpha = f2;
        try {
            MenuVeil.veil(imDrawList, width, height, 0.62F * f2);
            ImFont imFont = this.f.bodyFont();
            float g2 = 22.0F * scale;
            float h2 = 11.0F * scale;
            float f3 = 42.0F * scale;
            float g3 = Math.min(560.0F * scale, width - 48.0F * scale);
            float h3 = g3 - g2 * 2.0F;
            List list = dialog.body.isEmpty() ? List.of() : wrap(imFont, dialog.body, h3);
            float f4 = this.f.textHeight(imFont) * 1.35F;
            float g4 = list.size() * f4;
            float h4 = dialog.content != null ? dialog.contentHeight * scale : 0.0F;
            float f5 = g2 + 24.0F * scale * 0.5F;
            float g5 =
                    f5
                            + g4
                            + (h4 > 0.0F ? (list.isEmpty() ? 0.0F : h2) + h4 : 0.0F)
                            + h2 * 2.0F
                            + f3
                            + g2;
            float h5 = (width - g3) * 0.5F;
            float f6 = (height - g5) * 0.5F + (1.0F - f2) * 8.0F * scale;
            this.panel(h5, f6, g3, g5, dialog.title);
            float g6 = f6 + f5;
            for (String string : (Iterable<String>) list) {
                this.f.text(
                        imDrawList,
                        imFont,
                        h5 + (g3 - this.f.textWidth(imFont, string)) * 0.5F,
                        g6,
                        -1575946,
                        0.9F * f2,
                        string);
                g6 += f4;
            }
            if (dialog.content != null) {
                if (!list.isEmpty()) {
                    g6 += h2;
                }
                dialog.content.draw(this, dialog, h5 + g2, g6, h3, h4);
            }
            List list2 = dialog.choices();
            int i = list2.size();
            float h6 = (h3 - h2 * (i - 1)) / Math.max(1, i);
            float f7 = f6 + g5 - g2 - f3;
            for (int j = 0; j < i; j++) {
                DuskWidgets.Dialog.Choice choice = (DuskWidgets.Dialog.Choice) list2.get(j);
                if (this.button(choice, choice.label, h5 + g2 + j * (h6 + h2), f7, h6, f3)) {
                    dialog.press(choice);
                }
            }
        } finally {
            this.alpha = alpha2;
        }
    }

    public static void plate(
            ImDrawList imDrawList,
            float g,
            float f2,
            float g2,
            float h2,
            float f3,
            float g3,
            float h3) {
        float f4 = h2 * 0.5F;
        float g4 = Math.max(1.2F, 1.8F * g3);
        float h4 = Math.max(1.0F, g3);
        float f5 = 4.0F * g3;
        imDrawList.addRectFilled(
                g + 2.0F * g3,
                f2 + 3.0F * g3,
                g + g2 + 2.0F * g3,
                f2 + h2 + 3.0F * g3,
                HudDraw.col(-16777216, 0.5F * h3),
                f4);
        imDrawList.addRectFilled(
                g,
                f2,
                g + g2,
                f2 + h2,
                HudDraw.col(HudDraw.mix(-16381684, -15408912, f3), (0.9F + 0.1F * f3) * h3),
                f4);
        imDrawList.addRect(g, f2, g + g2, f2 + h2, HudDraw.col(-15408912, h3), f4, 0, g4);
        imDrawList.addRect(
                g + f5,
                f2 + f5,
                g + g2 - f5,
                f2 + h2 - f5,
                HudDraw.col(HudDraw.mix(-15408912, -16777216, f3), (0.55F + 0.25F * f3) * h3),
                f4 - f5,
                0,
                h4);
    }

    public static void plateButton(
            ImDrawList imDrawList,
            ImFont imFont,
            float g,
            float f2,
            float g2,
            float h2,
            String string,
            boolean bl,
            float f3,
            float g3,
            float h3) {
        float f4 = h3 * (bl ? 1.0F : 0.45F);
        plate(imDrawList, g, f2, g2, h2, f3, g3, f4);
        float g4 = imFont != null ? imFont.getFontSize() : 15.0F * g3;
        int i = bl ? HudDraw.mix(-6557705, -16777216, f3) : -7693914;
        HudDraw.drawSpaced(
                imDrawList,
                imFont,
                g + g2 * 0.5F,
                f2 + (h2 - g4) * 0.5F + g3,
                string,
                3.0F * g3,
                i,
                f4,
                f3 < 0.5F);
    }

    public static void ringMark(
            ImDrawList imDrawList, float g, float f2, float g2, float h2, float f3, float g3) {
        float h3 = Math.max(1.2F, 1.8F * f3);
        float f4 = Math.max(1.0F, f3);
        imDrawList.addCircleFilled(
                g + 2.0F * f3, f2 + 3.0F * f3, g2, HudDraw.col(-16777216, 0.5F * g3), 32);
        imDrawList.addCircleFilled(
                g,
                f2,
                g2,
                HudDraw.col(HudDraw.mix(-16381684, -15408912, h2), (0.9F + 0.1F * h2) * g3),
                32);
        imDrawList.addCircle(g, f2, g2, HudDraw.col(-15408912, g3), 32, h3);
        imDrawList.addCircle(
                g,
                f2,
                g2 - 3.5F * f3,
                HudDraw.col(HudDraw.mix(-15408912, -16777216, h2), (0.5F + 0.3F * h2) * g3),
                32,
                f4);
        imDrawList.addLine(
                g - g2 * 0.75F,
                f2 + g2 + 2.5F * f3,
                g + g2 * 0.75F,
                f2 + g2 + 2.5F * f3,
                HudDraw.col(-15408912, g3),
                h3);
    }

    public static void ringButton(
            ImDrawList imDrawList,
            DuskWidgets.Icon icon,
            float g,
            float f2,
            float g2,
            float h2,
            float f3,
            float g3) {
        float h3 = g + g2 * 0.5F;
        float f4 = f2 + g2 * 0.5F - 2.0F * f3;
        float g4 = g2 * 0.44F;
        ringMark(imDrawList, h3, f4, g4, h2, f3, g3);
        icon.draw(
                imDrawList,
                h3,
                f4,
                g4 * 0.5F,
                HudDraw.col(HudDraw.mix(-6557705, -16777216, h2), g3),
                Math.max(1.2F, 1.6F * f3));
    }

    public static float tag(
            ImDrawList imDrawList,
            ImFont imFont,
            float g,
            float f2,
            float g2,
            String string,
            float h2,
            float f3) {
        float g3 = 4.0F * h2;
        float h3 = HudDraw.spacedWidth(imFont, string, g3) + 28.0F * h2;
        float f4 = g - h3 * 0.5F;
        imDrawList.addRectFilled(f4, f2, f4 + h3, f2 + g2, HudDraw.col(-15408912, f3), 2.0F * h2);
        imDrawList.addRect(
                f4 + 2.5F * h2,
                f2 + 2.5F * h2,
                f4 + h3 - 2.5F * h2,
                f2 + g2 - 2.5F * h2,
                HudDraw.col(-16777216, 0.45F * f3),
                h2,
                0,
                Math.max(1.0F, h2));
        float g4 = imFont != null ? imFont.getFontSize() : 15.0F * h2;
        HudDraw.drawSpaced(
                imDrawList,
                imFont,
                g,
                f2 + (g2 - g4) * 0.5F + h2,
                string,
                g3,
                -16777216,
                f3,
                false);
        return h3;
    }

    public static void panel(
            ImDrawList imDrawList, float g, float f2, float g2, float h2, float f3, float g3) {
        float h3 = 8.0F * f3;
        float f4 = 4.0F * f3;
        imDrawList.addRectFilled(
                g + 3.0F * f3,
                f2 + 4.0F * f3,
                g + g2 + 3.0F * f3,
                f2 + h2 + 4.0F * f3,
                HudDraw.col(-16777216, 0.45F * g3),
                h3);
        imDrawList.addRectFilled(g, f2, g + g2, f2 + h2, HudDraw.col(-16381684, g3), h3);
        imDrawList.addRect(
                g,
                f2,
                g + g2,
                f2 + h2,
                HudDraw.col(-15408912, 0.9F * g3),
                h3,
                0,
                Math.max(1.0F, 1.4F * f3));
        imDrawList.addRect(
                g + f4,
                f2 + f4,
                g + g2 - f4,
                f2 + h2 - f4,
                HudDraw.col(-15408912, 0.3F * g3),
                h3 - f4,
                0,
                Math.max(1.0F, f3));
    }

    public static void row(
            ImDrawList imDrawList,
            float g,
            float f2,
            float g2,
            float h2,
            float f3,
            boolean bl,
            float g3,
            float h3) {
        float f4 = 6.0F * g3;
        float g4 = Math.max(1.0F, g3);
        if (bl) {
            float h4 = 3.0F * g3;
            imDrawList.addRectFilled(
                    g, f2, g + g2, f2 + h2, HudDraw.col(-15408912, (0.11F + 0.03F * f3) * h3), f4);
            imDrawList.addRect(
                    g,
                    f2,
                    g + g2,
                    f2 + h2,
                    HudDraw.col(-15408912, 0.85F * h3),
                    f4,
                    0,
                    Math.max(1.0F, 1.2F * g3));
            imDrawList.addRect(
                    g + h4,
                    f2 + h4,
                    g + g2 - h4,
                    f2 + h2 - h4,
                    HudDraw.col(-15408912, 0.25F * h3),
                    f4 - h4,
                    0,
                    g4);
        } else {
            imDrawList.addRectFilled(
                    g, f2, g + g2, f2 + h2, HudDraw.col(-16777216, 0.22F * h3), f4);
            if (f3 > 0.01F) {
                imDrawList.addRectFilled(
                        g, f2, g + g2, f2 + h2, HudDraw.col(-15408912, 0.06F * f3 * h3), f4);
                imDrawList.addRect(
                        g, f2, g + g2, f2 + h2, HudDraw.col(-15408912, 0.45F * f3 * h3), f4, 0, g4);
            }
        }
    }

    public static void field(
            ImDrawList imDrawList,
            float g,
            float f2,
            float g2,
            float h2,
            float f3,
            float g3,
            float h3,
            float f4) {
        float g4 = 0.0F * h3;
        imDrawList.addRectFilled(g, f2, g + g2, f2 + h2, HudDraw.col(-16777216, 0.35F * f4), g4);
        imDrawList.addRect(
                g,
                f2,
                g + g2,
                f2 + h2,
                HudDraw.col(
                        HudDraw.mix(-1575946, -15408912, g3), (0.12F + 0.1F * f3 + 0.5F * g3) * f4),
                g4,
                0,
                Math.max(1.0F, h3));
    }

    public static void drawTooltip(
            ImDrawList imDrawList,
            ImFont imFont,
            String string,
            float g,
            float f2,
            float g2,
            float h2,
            float f3) {
        float g3 = imFont != null ? imFont.getFontSize() : 15.0F * f3;
        float h3 = HudDraw.textWidth(imFont, string) + 18.0F * f3;
        float f4 = g3 + 10.0F * f3;
        float g4 = 4.0F * f3;
        float h4 = Math.max(g4, Math.min(g2 - h3 - g4, g - h3 * 0.5F));
        f2 = Math.min(f2, h2 - f4 - g4);
        imDrawList.addRectFilled(
                h4, f2, h4 + h3, f2 + f4, HudDraw.col(-16381684, 0.96F), 3.0F * f3);
        imDrawList.addRect(
                h4,
                f2,
                h4 + h3,
                f2 + f4,
                HudDraw.col(-15408912, 0.7F),
                3.0F * f3,
                0,
                Math.max(1.0F, f3));
        HudDraw.text(
                imDrawList,
                imFont,
                h4 + 9.0F * f3,
                f2 + 5.0F * f3,
                string,
                HudDraw.col(-6621958, 1.0F));
    }

    public static List<String> wrap(ImFont imFont, String string, float g) {
        ArrayList list = new ArrayList();
        for (String string2 : string.split("\n", -1)) {
            String string3 = "";
            for (String string4 : string2.split(" +")) {
                if (!string4.isEmpty()) {
                    String string5 = string3.isEmpty() ? string4 : string3 + " " + string4;
                    if (HudDraw.textWidth(imFont, string5) <= g) {
                        string3 = string5;
                    } else {
                        if (!string3.isEmpty()) {
                            list.add(string3);
                        }
                        while (string4.length() > 1 && HudDraw.textWidth(imFont, string4) > g) {
                            int fitCount = fitCount(imFont, string4, g);
                            list.add(string4.substring(0, fitCount));
                            string4 = string4.substring(fitCount);
                        }
                        string3 = string4;
                    }
                }
            }
            list.add(string3);
        }
        return list;
    }

    public static String fit(ImFont imFont, String string, float g) {
        if (HudDraw.textWidth(imFont, string) <= g) {
            return string;
        } else {
            String string2 = "...";
            int fitCount = fitCount(imFont, string, g - HudDraw.textWidth(imFont, string2));
            return string.substring(0, fitCount).stripTrailing() + string2;
        }
    }

    private static int fitCount(ImFont imFont, String string, float g) {
        int i = 1;
        int j = string.length();
        while (i < j) {
            int k = i + j + 1 >>> 1;
            if (HudDraw.textWidth(imFont, string.substring(0, k)) <= g) {
                i = k;
            } else {
                j = k - 1;
            }
        }
        return Math.min(i, string.length());
    }

    private float[] thumb(DuskWidgets.Scroll scroll, float g) {
        float f2 = 5.0F * g;
        float g2 = 3.0F * g;
        float h2 = scroll.h - 2.0F * f2;
        float f3 = Math.min(h2, Math.max(28.0F * g, h2 * scroll.h / scroll.content));
        float g3 = Math.max(1.0F, scroll.content - scroll.h);
        float h3 = scroll.y + f2 + (h2 - f3) * Math.max(0.0F, Math.min(1.0F, scroll.offset / g3));
        return new float[] {scroll.x + scroll.w - f2 - g2, h3, g2, f3};
    }

    @Environment(EnvType.CLIENT)
    private static final class Anim {

        float value;

        int frame;

        private Anim() {}
    }

    @Environment(EnvType.CLIENT)
    public static final class Dialog {

        private final String title;

        private final String body;

        private final List<DuskWidgets.Dialog.Choice> actions = new ArrayList<>();

        private DuskWidgets.Dialog.Choice cancel;

        private DuskWidgets.Dialog.Content content;

        private float contentHeight;

        private boolean closed;

        private float openedAt = 0.0F / 0.0F;

        public Dialog(String title2, String body2) {
            this.title = title2;
            this.body = body2;
        }

        public DuskWidgets.Dialog action(String string, Runnable runnable) {
            this.actions.add(new DuskWidgets.Dialog.Choice(string, runnable, null));
            return this;
        }

        public DuskWidgets.Dialog submit(String string, BooleanSupplier booleanSupplier) {
            this.actions.add(new DuskWidgets.Dialog.Choice(string, null, booleanSupplier));
            return this;
        }

        public DuskWidgets.Dialog cancel(String string) {
            return this.cancel(string, null);
        }

        public DuskWidgets.Dialog cancel(String string, Runnable runnable) {
            this.cancel = new DuskWidgets.Dialog.Choice(string, runnable, null);
            return this;
        }

        public DuskWidgets.Dialog content(
                float contentHeight2, DuskWidgets.Dialog.Content content2) {
            this.contentHeight = contentHeight2;
            this.content = content2;
            return this;
        }

        public void confirm() {
            if (!this.actions.isEmpty()) {
                this.press(this.actions.get(0));
            }
        }

        public void dismiss() {
            if (this.cancel != null) {
                this.press(this.cancel);
            } else {
                this.close();
            }
        }

        public void close() {
            this.closed = true;
        }

        public boolean isClosed() {
            return this.closed;
        }

        private List<DuskWidgets.Dialog.Choice> choices() {
            if (this.cancel == null) {
                return this.actions;
            } else {
                ArrayList list = new ArrayList<>(this.actions);
                list.add(this.cancel);
                return list;
            }
        }

        private void press(DuskWidgets.Dialog.Choice choice) {
            if (!this.closed) {
                if (choice.submit != null) {
                    if (choice.submit.getAsBoolean()) {
                        this.closed = true;
                    }
                } else {
                    if (choice.run != null) {
                        choice.run.run();
                    }
                    this.closed = true;
                }
            }
        }

        @Environment(EnvType.CLIENT)
        private record Choice(String label, Runnable run, BooleanSupplier submit) {}

        @FunctionalInterface
        @Environment(EnvType.CLIENT)
        public interface Content {

            void draw(
                    DuskWidgets duskWidgets,
                    DuskWidgets.Dialog dialog,
                    float g,
                    float f2,
                    float g2,
                    float h2);
        }
    }

    @FunctionalInterface
    @Environment(EnvType.CLIENT)
    public interface Icon {

        void draw(ImDrawList imDrawList, float g, float f2, float g2, int i, float h2);
    }

    @Environment(EnvType.CLIENT)
    private static final class Scroll {

        float offset;

        float target;

        float grab = 0.0F / 0.0F;

        float x;

        float y;

        float w;

        float h;

        float content;

        private Scroll() {}
    }

    @Environment(EnvType.CLIENT)
    public static final class TextField {

        private static int nextId;

        private final String id;

        private final int maxLength;

        private final ImString buffer;

        private final ImGuiInputTextCallback limiter;

        private String placeholder;

        private boolean password;

        private boolean focusRequested;

        private boolean active;

        private float focus;

        public TextField(int maxLength2) {
            this.id = "##dusk_field_" + nextId++;
            this.placeholder = "";
            this.maxLength = maxLength2;
            this.buffer = new ImString(maxLength2 * 3);
            this.limiter =
                    new ImGuiInputTextCallback() {

                        public void accept(ImGuiInputTextCallbackData imGuiInputTextCallbackData) {
                            TextField.this.limit(imGuiInputTextCallbackData);
                        }
                    };
        }

        public DuskWidgets.TextField placeholder(String placeholder2) {
            this.placeholder = placeholder2;
            return this;
        }

        public DuskWidgets.TextField password(boolean password2) {
            this.password = password2;
            return this;
        }

        public DuskWidgets.TextField allowedChars(String string) {
            this.buffer.inputData.allowedChars = string;
            return this;
        }

        public String text() {
            return this.buffer.get();
        }

        public void setText(String string) {
            if (string.codePointCount(0, string.length()) > this.maxLength) {
                string = string.substring(0, string.offsetByCodePoints(0, this.maxLength));
            }
            this.buffer.set(string);
        }

        public void focus() {
            this.focusRequested = true;
        }

        public boolean focused() {
            return this.active;
        }

        public int maxLength() {
            return this.maxLength;
        }

        private int flags() {
            return 4194368 | (this.password ? 1024 : 0);
        }

        private void limit(ImGuiInputTextCallbackData imGuiInputTextCallbackData) {
            String buf = imGuiInputTextCallbackData.getBuf();
            int i = buf.codePointCount(0, buf.length()) - this.maxLength;
            if (i > 0) {
                byte[] bytes = buf.getBytes(StandardCharsets.UTF_8);
                int j;
                String string =
                        new String(
                                bytes,
                                0,
                                j =
                                        Math.min(
                                                Math.max(
                                                        0,
                                                        imGuiInputTextCallbackData.getCursorPos()),
                                                bytes.length),
                                StandardCharsets.UTF_8);
                int k = Math.min(i, string.codePointCount(0, string.length()));
                if (k > 0) {
                    int m =
                            string.substring(0, string.offsetByCodePoints(string.length(), -k))
                                    .getBytes(StandardCharsets.UTF_8)
                                    .length;
                    imGuiInputTextCallbackData.deleteChars(m, j - m);
                    i -= k;
                }
                if (i > 0) {
                    String buf2 = imGuiInputTextCallbackData.getBuf();
                    int n =
                            buf2.substring(0, buf2.offsetByCodePoints(0, this.maxLength))
                                    .getBytes(StandardCharsets.UTF_8)
                                    .length;
                    imGuiInputTextCallbackData.deleteChars(
                            n, imGuiInputTextCallbackData.getBufTextLen() - n);
                }
            }
        }
    }
}
