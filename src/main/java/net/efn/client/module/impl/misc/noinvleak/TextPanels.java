/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.misc.noinvleak;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Objects;

@Environment(value = EnvType.CLIENT)
final class TextPanels {

    private static final int TOOLTIP_MARGIN = 12;

    private static final int LINE = 10;

    private static final Canvas canvas = new Canvas();

    private static OverlayWindow tooltipWindow;

    private static OverlayWindow nameWindow;

    private static ItemStack tooltipStack;

    private static int tooltipMouseX;

    private static int tooltipMouseY;

    private static ItemStack nameStack;

    private static int nameAlpha;

    private static int nameY;

    private static ItemStack shownTooltip;

    private static int shownTooltipCount;

    private static int shownTooltipDamage;

    private static int shownTooltipScale;

    private static int tooltipWidth;

    private static int tooltipHeight;

    private static int tooltipScreenX;

    private static int tooltipScreenY;

    private static ItemStack shownName;

    private static int shownNameAlpha;

    private static int shownNameScale;

    private static int nameScreenX;

    private static int nameScreenY;

    private TextPanels() {}

    static void tooltip(ItemStack stack, int i, int j) {
        tooltipStack = stack;
        tooltipMouseX = i;
        tooltipMouseY = j;
    }

    static void itemName(ItemStack stack, int i, int j) {
        nameStack = stack;
        nameAlpha = i;
        nameY = j;
    }

    static boolean started() {
        return tooltipWindow != null || nameWindow != null;
    }

    static void frameDone() {
        tooltipStack = null;
        nameStack = null;
    }

    static void update(Window window, int i, int j, int k) {
        MinecraftClient client = MinecraftClient.getInstance();
        int scaledWidth = window.getScaledWidth();
        int scaledHeight = window.getScaledHeight();
        TextPanels.updateTooltip(client, window, i, j, k, scaledWidth, scaledHeight);
        TextPanels.updateName(client, window, i, j, k, scaledWidth);
    }

    private static void updateTooltip(
            MinecraftClient client, Window window, int i, int j, int k, int m, int n) {
        int o;
        int p;
        int q;
        ItemStack stack = tooltipStack;
        if (stack == null) {
            if (tooltipWindow != null) {
                tooltipWindow.hide();
            }
            shownTooltip = null;
            return;
        }
        if (tooltipWindow == null) {
            tooltipWindow = new OverlayWindow(window.getHandle());
        }
        boolean bl =
                stack != shownTooltip
                        || stack.getCount() != shownTooltipCount
                        || stack.getDamage() != shownTooltipDamage
                        || i != shownTooltipScale;
        boolean bl2 = bl;
        if (bl) {
            List<Text> tooltipFromItem = Screen.getTooltipFromItem(client, stack);
            q = 0;
            p = tooltipFromItem.size() == 1 ? -2 : 0;
            for (Text text : tooltipFromItem) {
                q = Math.max(q, client.textRenderer.getWidth(text));
                p += 10;
            }
            tooltipWidth = q;
            tooltipHeight = p;
            o = q + 24;
            int r = p + 24;
            canvas.wrap(tooltipWindow.pixels(o * i, r * i), tooltipWindow.stride(), o * i, r * i);
            OverlayArt.slice(canvas, OverlayArt.tooltipBackground(), 0, 0, o, r, i);
            OverlayArt.slice(canvas, OverlayArt.tooltipFrame(), 0, 0, o, r, i);
            int t = 12;
            for (int i2 = 0; i2 < tooltipFromItem.size(); ++i2) {
                OverlayArt.text(canvas, tooltipFromItem.get(i2), 12, t, i, -1, true);
                t += 10 + (i2 == 0 ? 2 : 0);
            }
            shownTooltip = stack;
            shownTooltipCount = stack.getCount();
            shownTooltipDamage = stack.getDamage();
            shownTooltipScale = i;
        }
        int u = tooltipMouseX + 12;
        q = tooltipMouseY - 12;
        if (u + tooltipWidth > m) {
            u = Math.max(u - 24 - tooltipWidth, 4);
        }
        if (q + tooltipHeight + 3 > n) {
            q = n - tooltipHeight - 3;
        }
        p = j + (u - 12) * i;
        o = k + (q - 12) * i;
        if (bl) {
            tooltipWindow.present(p, o, (tooltipWidth + 24) * i, (tooltipHeight + 24) * i);
        } else if (p != tooltipScreenX || o != tooltipScreenY) {
            tooltipWindow.move(p, o);
        }
        tooltipScreenX = p;
        tooltipScreenY = o;
    }

    private static void updateName(
            MinecraftClient client, Window window, int i, int j, int k, int m) {
        ItemStack stack = nameStack;
        if (stack == null || nameAlpha <= 0) {
            if (nameWindow != null) {
                nameWindow.hide();
            }
            shownName = null;
            return;
        }
        if (nameWindow == null) {
            nameWindow = new OverlayWindow(window.getHandle());
        }
        MutableText text =
                Text.empty().append(stack.getName()).formatted(stack.getRarity().getFormatting());
        if (stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            text = text.copy().formatted(Formatting.ITALIC);
        }
        int width = client.textRenderer.getWidth(text);
        int n = (m - width) / 2;
        int o = j + (n - 2) * i;
        int p = k + (nameY - 2) * i;
        boolean bl = stack != shownName || nameAlpha != shownNameAlpha || i != shownNameScale;
        boolean bl2 = bl;
        if (bl) {
            int q = width + 4;
            Objects.requireNonNull(client.textRenderer);
            int r = 13;
            canvas.wrap(nameWindow.pixels(q * i, r * i), nameWindow.stride(), q * i, r * i);
            int textBackgroundColor = client.options.getTextBackgroundColor(0.0f);
            if (textBackgroundColor != 0) {
                int t = (textBackgroundColor >>> 24) * nameAlpha / 255;
                canvas.rect(0, 0, q * i, r * i, t << 24 | textBackgroundColor & 0xFFFFFF);
            }
            OverlayArt.text(canvas, text, 2, 2, i, nameAlpha << 24 | 0xFFFFFF, true);
            nameWindow.present(o, p, q * i, r * i);
            shownName = stack;
            shownNameAlpha = nameAlpha;
            shownNameScale = i;
        } else if (o != nameScreenX || p != nameScreenY) {
            nameWindow.move(o, p);
        }
        nameScreenX = o;
        nameScreenY = p;
    }

    static void hide() {
        if (tooltipWindow != null) {
            tooltipWindow.hide();
        }
        if (nameWindow != null) {
            nameWindow.hide();
        }
        shownTooltip = null;
        shownName = null;
    }

    static void release() {
        if (tooltipWindow != null) {
            tooltipWindow.destroy();
            tooltipWindow = null;
        }
        if (nameWindow != null) {
            nameWindow.destroy();
            nameWindow = null;
        }
        shownTooltip = null;
        shownName = null;
        tooltipStack = null;
        nameStack = null;
    }
}
