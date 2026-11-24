package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImFontGlyph;
import imgui.ImGui;

import net.efn.client.config.HudTheme;
import net.efn.client.config.Surface;
import net.efn.client.event.impl.ModuleToggleEvent;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.client.HudModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Environment(EnvType.CLIENT)
public final class ToggleToasts {

    public static final String ID = "toggle_toasts";

    private static final float MARGIN = 10.0F;

    private static final float GAP = 6.0F;

    private static final float CAP_H = 28.0F;

    private static final float PAD_X = 12.0F;

    private static final float PAD_Y = 8.0F;

    private static final float LINE_GAP = 2.0F;

    private static final float GAP_DIV = 10.0F;

    private static final float DIV_H = 12.0F;

    private static final float DOT = 3.0F;

    private static final float GAP_DOT = 8.0F;

    private static final float CHIP_H = 16.0F;

    private static final float CHIP_PAD = 6.0F;

    private static final float CHIP_RADIUS = 4.0F;

    private static final float GAP_CHIP = 10.0F;

    private static final float CHAT_INPUT = 14.0F;

    private static final int MAX_SHOWN = 4;

    private static final float IN_SECONDS = 0.16F;

    private static final float HOLD_SECONDS = 2.0F;

    private static final float OUT_SECONDS = 0.18F;

    private static final float MAX_STEP = 0.033333335F;

    private static final float LIFT_SPEED = 14.0F;

    private static final float OFF_OUTLINE = 0.55F;

    private static final int CHIP_TEXT = -1;

    private static final Object OWNER = new Object();

    private static final Queue<ToggleToasts.Toggle> PENDING = new ConcurrentLinkedQueue<>();

    private static final List<ToggleToasts.Toast> TOASTS = new ArrayList<>();

    private static float lift = 0.0F / 0.0F;

    private ToggleToasts() {}

    public static void install() {
        ModuleToggleEvent.TYPE.listen(
                OWNER,
                moduleToggleEvent -> show(moduleToggleEvent.module, moduleToggleEvent.enabled));
    }

    public static void show(ClientModule module2, boolean bl) {
        if (module2.toggleMessageOn()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.world != null) {
                PENDING.add(new ToggleToasts.Toggle(module2, bl));
            }
        }
    }

    public static boolean hasWork() {
        return !TOASTS.isEmpty() || !PENDING.isEmpty();
    }

    public static void render() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            PENDING.clear();
            TOASTS.clear();
            lift = 0.0F / 0.0F;
        } else {
            float f = Math.min(ImGuiManager.getInstance().frameDelta(), 0.033333335F);
            step(f);
            ToggleToasts.Toggle toggle;
            while ((toggle = PENDING.poll()) != null) {
                add(toggle);
            }
            if (TOASTS.isEmpty()) {
                lift = 0.0F / 0.0F;
            } else {
                float uiScale = ImGuiManager.getInstance().uiScale();
                lift = Motion.step(lift, liftTarget(client, uiScale), f, 14.0F);
                if (!hidden(client)) {
                    draw(uiScale);
                }
            }
        }
    }

    private static void add(ToggleToasts.Toggle toggle) {
        ToggleToasts.Toast toast = null;
        for (ToggleToasts.Toast toast2 : TOASTS) {
            if (toast2.module == toggle.module()) {
                toast = toast2;
                break;
            }
        }
        if (toast == null) {
            toast = new ToggleToasts.Toast(toggle.module());
            TOASTS.add(toast);
        }
        toast.enabled = toggle.enabled();
        toast.leaving = false;
        toast.hold = 2.0F;
        int i = 1;
        for (int j = TOASTS.size() - 1; j >= 0; j--) {
            ToggleToasts.Toast toast3 = TOASTS.get(j);
            if (toast3 != toast && !toast3.leaving) {
                if (++i > 4) {
                    toast3.leaving = true;
                }
            }
        }
    }

    private static void step(float f) {
        for (int i = TOASTS.size() - 1; i >= 0; i--) {
            ToggleToasts.Toast toast = TOASTS.get(i);
            if (toast.leaving) {
                toast.shown -= f / 0.18F;
                if (toast.shown <= 0.0F) {
                    TOASTS.remove(i);
                }
            } else if (toast.shown < 1.0F) {
                toast.shown = Math.min(1.0F, toast.shown + f / 0.16F);
            } else {
                toast.hold -= f;
                if (toast.hold <= 0.0F) {
                    toast.leaving = true;
                }
            }
        }
    }

    private static float liftTarget(MinecraftClient client, float f) {
        return client.currentScreen instanceof ChatScreen
                ? 14.0F * client.getWindow().getScaleFactor() + 6.0F * f
                : 10.0F * f;
    }

    private static boolean hidden(MinecraftClient client) {
        return client.options.hudHidden && client.currentScreen == null
                ? true
                : client.getOverlay() != null
                        || client.currentScreen instanceof LevelLoadingScreen
                        || client.currentScreen instanceof MessageScreen
                        || client.currentScreen instanceof ProgressScreen;
    }

    private static void draw(float f) {
        HudModule module2 = ModuleRegistry.get(HudModule.class);
        HudModule.ToastStyle toastStyle =
                module2 != null ? module2.toastStyle() : HudModule.ToastStyle.BAR;
        Surface toasts = HudTheme.get().toasts;
        ToggleToasts.Look look = look(toastStyle, f, HudChrome.needsTextGuard(toasts));
        float g = ImGui.getIO().getDisplaySizeY() - lift;
        for (int i = TOASTS.size() - 1; i >= 0; i--) {
            ToggleToasts.Toast toast = TOASTS.get(i);
            toast.open = ClickGuiRenderer.easeOut(Math.max(0.0F, Math.min(1.0F, toast.shown)));
            toast.bottom = Math.round(g);
            toast.top = Math.round(g - look.h() * toast.open);
            g -= (look.h() + 6.0F * f) * toast.open;
        }
        ImDrawList foregroundDrawList = ImGui.getForegroundDrawList();
        boolean bl = module2 == null || module2.shadow();
        float f2 = HudTheme.get().rounding.get() * f;
        float g2 = Math.round(10.0F * f);
        for (ToggleToasts.Toast toast2 : TOASTS) {
            float h2 = toast2.bottom - toast2.top;
            if (!(h2 < 1.0F)) {
                String string = toast2.module.name();
                float width = HudChrome.width(look.name(), look.namePx(), string);
                float measure = measure(toastStyle, look, width);
                float f3 = Math.round(g2 - (measure + g2) * (1.0F - toast2.open));
                float open2 = toast2.open;
                HudChrome.card(
                        foregroundDrawList, toasts, f3, toast2.top, measure, h2, f2, open2, bl, f);
                boolean bl2 = h2 < look.h() - 0.5F;
                if (bl2) {
                    foregroundDrawList.pushClipRect(
                            f3, toast2.top, f3 + measure, toast2.bottom, true);
                }
                float g3 = Math.round(toast2.top - (look.h() - h2) * 0.5F);
                switch (toastStyle) {
                    case CARD:
                        drawCard(foregroundDrawList, look, toast2, f3, g3, open2);
                        break;
                    case CHIP:
                        drawChip(foregroundDrawList, look, toast2, f3, g3, measure, open2);
                        break;
                    default:
                        drawBar(foregroundDrawList, look, toast2, f3, g3, width, open2);
                }
                if (bl2) {
                    foregroundDrawList.popClipRect();
                }
            }
        }
    }

    private static ToggleToasts.Look look(HudModule.ToastStyle toastStyle, float f, boolean bl) {
        ImGuiManager imGuiManager = ImGuiManager.getInstance();
        ImFont imFont = imGuiManager.hudTextFont();
        ImFont imFont2 = imGuiManager.hudLabelFont();
        int px = HudChrome.px(17.0F, f);
        int px2 = HudChrome.px(14.0F, f);
        float g =
                toastStyle == HudModule.ToastStyle.CARD
                        ? Math.round(8.0F * f * 2.0F + px + 2.0F * f + px2)
                        : Math.round(28.0F * f);
        float f2 =
                Math.max(
                        HudChrome.width(imFont2, px2, state(toastStyle, true)),
                        HudChrome.width(imFont2, px2, state(toastStyle, false)));
        if (toastStyle == HudModule.ToastStyle.CHIP) {
            f2 = (float) Math.ceil(f2 + 6.0F * f * 2.0F);
        }
        return new ToggleToasts.Look(imFont, px, imFont2, px2, f, g, f2, WorldText.hudAccent(), bl);
    }

    private static String state(HudModule.ToastStyle toastStyle, boolean bl) {
        return switch (toastStyle) {
            case CARD -> bl ? "Enabled" : "Disabled";
            case CHIP -> bl ? "ON" : "OFF";
            case BAR -> bl ? "on" : "off";
        };
    }

    private static float measure(HudModule.ToastStyle toastStyle, ToggleToasts.Look look, float f) {
        float g = look.s();
        float f2 =
                switch (toastStyle) {
                    case CARD -> 12.0F * g * 2.0F + 14.0F * g + Math.max(f, look.stateW());
                    case CHIP -> 12.0F * g + f + 10.0F * g + look.stateW() + chipInset(look);
                    case BAR -> 12.0F * g * 2.0F + f + 10.0F * g * 2.0F + 1.0F + look.stateW();
                };
        return (float) Math.ceil(f2);
    }

    private static float chipInset(ToggleToasts.Look look) {
        return (float) Math.floor((look.h() - Math.round(16.0F * look.s())) * 0.5F);
    }

    private static void drawBar(
            ImDrawList imDrawList,
            ToggleToasts.Look look,
            ToggleToasts.Toast toast,
            float f,
            float g,
            float f2,
            float g2) {
        float h2 = look.s();
        float f3 = g + (float) Math.floor((look.h() - look.namePx()) * 0.5F);
        float g3 =
                f3
                        + HudChrome.baselineOffset(
                                look.name(), look.namePx(), look.label(), look.labelPx());
        float h3 = Math.round(f + 12.0F * h2);
        HudChrome.text(
                imDrawList,
                look.name(),
                look.namePx(),
                h3,
                f3,
                HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, g2),
                toast.module.name(),
                look.guard());
        float f4;
        HudChrome.divider(
                imDrawList, f4 = h3 + (f2 + 10.0F * h2), g + look.h() * 0.5F, 12.0F * h2, g2);
        int scaleAlpha =
                HudChrome.scaleAlpha(toast.enabled ? look.accent() : HudChrome.TEXT_LABEL, g2);
        HudChrome.text(
                imDrawList,
                look.label(),
                look.labelPx(),
                Math.round(h3 = f4 + (1.0F + 10.0F * h2)),
                g3,
                scaleAlpha,
                state(HudModule.ToastStyle.BAR, toast.enabled),
                look.guard());
    }

    private static void drawCard(
            ImDrawList imDrawList,
            ToggleToasts.Look look,
            ToggleToasts.Toast toast,
            float f,
            float g,
            float f2) {
        float g2 = look.s();
        imDrawList.addCircleFilled(
                f + 15.0F * g2,
                g + look.h() * 0.5F,
                3.0F * g2,
                HudChrome.scaleAlpha(toast.enabled ? look.accent() : HudChrome.TEXT_LABEL, f2),
                0);
        float h2 = Math.round(f + 26.0F * g2);
        float f3 = Math.round(g + 8.0F * g2);
        HudChrome.text(
                imDrawList,
                look.name(),
                look.namePx(),
                h2,
                f3,
                HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, f2),
                toast.module.name(),
                look.guard());
        HudChrome.text(
                imDrawList,
                look.label(),
                look.labelPx(),
                h2,
                Math.round(f3 + look.namePx() + 2.0F * g2),
                HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, f2),
                state(HudModule.ToastStyle.CARD, toast.enabled),
                look.guard());
    }

    private static void drawChip(
            ImDrawList imDrawList,
            ToggleToasts.Look look,
            ToggleToasts.Toast toast,
            float f,
            float g,
            float f2,
            float g2) {
        float h2 = look.s();
        float f3 = g + (float) Math.floor((look.h() - look.namePx()) * 0.5F);
        HudChrome.text(
                imDrawList,
                look.name(),
                look.namePx(),
                Math.round(f + 12.0F * h2),
                f3,
                HudChrome.scaleAlpha(HudChrome.TEXT_VALUE, g2),
                toast.module.name(),
                look.guard());
        float chipInset = chipInset(look);
        float g3 = Math.round(16.0F * h2);
        float h3 = f + f2 - chipInset;
        float f4 = h3 - look.stateW();
        float g4 = g + chipInset;
        float h4 = Math.min(HudTheme.get().rounding.get(), 4.0F) * h2;
        if (toast.enabled) {
            imDrawList.addRectFilled(
                    f4, g4, h3, g4 + g3, HudChrome.scaleAlpha(look.accent(), g2), h4);
        } else {
            imDrawList.addRect(
                    f4,
                    g4,
                    h3,
                    g4 + g3,
                    HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, g2 * 0.55F),
                    h4,
                    0,
                    1.0F);
        }
        String state = state(HudModule.ToastStyle.CHIP, toast.enabled);
        float f5 =
                Math.round(
                        f4
                                + (look.stateW()
                                                - HudChrome.width(
                                                        look.label(), look.labelPx(), state))
                                        * 0.5F);
        float g5 = Math.round(g4 + g3 * 0.5F - capMiddle(look.label(), look.labelPx()));
        HudChrome.text(
                imDrawList,
                look.label(),
                look.labelPx(),
                f5,
                g5,
                HudChrome.scaleAlpha(toast.enabled ? -1 : HudChrome.TEXT_LABEL, g2),
                state,
                !toast.enabled && look.guard());
    }

    private static float capMiddle(ImFont imFont, int i) {
        float f = i * 0.2F;
        ImFontGlyph glyphNoFallback;
        if (imFont != null
                && imFont.getFontSize() > 0.0F
                && (glyphNoFallback = imFont.findGlyphNoFallback(72)).isValidPtr()) {
            f = glyphNoFallback.getY0() * i / imFont.getFontSize();
        }
        return (f + HudChrome.ascent(imFont, i)) * 0.5F;
    }

    @Environment(EnvType.CLIENT)
    private record Look(
            ImFont name,
            int namePx,
            ImFont label,
            int labelPx,
            float s,
            float h,
            float stateW,
            int accent,
            boolean guard) {}

    @Environment(EnvType.CLIENT)
    private static final class Toast {

        final ClientModule module;

        boolean enabled;

        boolean leaving;

        float shown;

        float hold;

        float open;

        float top;

        float bottom;

        Toast(ClientModule module2) {
            this.module = module2;
        }
    }

    @Environment(EnvType.CLIENT)
    private record Toggle(ClientModule module, boolean enabled) {}
}
