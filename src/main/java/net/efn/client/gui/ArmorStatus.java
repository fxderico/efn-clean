package net.efn.client.gui;

import imgui.ImDrawList;
import imgui.ImFont;

import net.efn.client.config.Surface;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.impl.client.HudModule;
import net.efn.client.module.impl.misc.NoInvLeakModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
final class ArmorStatus {

    private static final EquipmentSlot[] ARMOR =
            new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
            };

    private static final EquipmentSlot[] ARMOR_AND_HANDS =
            new EquipmentSlot[] {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND,
                EquipmentSlot.OFFHAND
            };

    private static final float PAD = 6.0F;

    private static final float ICON = 18.0F;

    private static final float TILE_GAP = 5.0F;

    private static final float HANDS_GAP = 11.0F;

    private static final float GAUGE_H = 3.0F;

    private static final float GAUGE_GAP = 3.0F;

    private static final float TEXT_GAP = 2.0F;

    private static final float ROW_GAP = 4.0F;

    private static final float SIDE_GAP = 7.0F;

    private static final float SIDE_GAUGE_W = 44.0F;

    private static final float CRITICAL = 0.12F;

    private static final float GAUGE_RATE = 10.0F;

    private static final float SHOW_TIME = 0.25F;

    private static final String WIDEST = "100%";

    private static final String[] PERCENT = new String[101];

    private static final String[] COUNT = new String[100];

    private final float[] wear = new float[ARMOR_AND_HANDS.length];

    private final Item[] seen = new Item[ARMOR_AND_HANDS.length];

    private float presence;

    private float clock;

    private boolean column;

    private boolean hands;

    private boolean gauge;

    private boolean percent;

    ArmorStatus() {}

    void reset() {
        this.presence = 0.0F;
        for (int i = 0; i < this.seen.length; i++) {
            this.seen[i] = null;
        }
    }

    boolean visible() {
        return this.presence > 0.0F;
    }

    void update(MinecraftClient client, HudModule module, boolean bl, boolean bl2, float clock2) {
        this.clock += clock2;
        this.column = module.armorColumn();
        this.hands = module.armorHands();
        HudModule.ArmorWear armorWear = module.armorWear();
        this.gauge = armorWear != HudModule.ArmorWear.PERCENT;
        this.percent = armorWear != HudModule.ArmorWear.GAUGE;
        ClientPlayerEntity player = client.player;
        boolean bl3 = false;
        EquipmentSlot[] equipmentSlots = this.order();
        for (int i = 0; i < equipmentSlots.length; i++) {
            ItemStack stack =
                    player == null ? ItemStack.EMPTY : player.getEquippedStack(equipmentSlots[i]);
            if (!stack.isEmpty()) {
                bl3 = true;
            }
            float remaining = remaining(stack);
            if (stack.getItem() != this.seen[i]) {
                this.seen[i] = stack.getItem();
                this.wear[i] = remaining;
            } else {
                this.wear[i] =
                        remaining < 0.0F
                                ? remaining
                                : HudGlyphs.approach(
                                        Math.max(0.0F, this.wear[i]), remaining, 10.0F, clock2);
            }
        }
        boolean bl4 = bl2 || bl && bl3;
        this.presence =
                bl2
                        ? 1.0F
                        : Math.max(
                                0.0F,
                                Math.min(1.0F, this.presence + (bl4 ? clock2 : -clock2) / 0.25F));
    }

    private EquipmentSlot[] order() {
        return this.hands ? ARMOR_AND_HANDS : ARMOR;
    }

    private static float remaining(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageable() && stack.getMaxDamage() > 0
                ? Math.max(0.0F, 1.0F - (float) stack.getDamage() / stack.getMaxDamage())
                : -1.0F;
    }

    float planWidth(float f) {
        if (!this.visible()) {
            return 1.0F;
        } else {
            int i = this.order().length;
            float pixels = ItemIcons.pixels(18.0F * f);
            float g = this.percent ? (float) Math.ceil(percentWidth(f)) : 0.0F;
            if (this.column) {
                float h =
                        (this.percent ? g : 0.0F)
                                + (this.gauge ? 44.0F * f : 0.0F)
                                + (this.percent && this.gauge ? 7.0F * f : 0.0F);
                return Math.round(6.0F * f * 2.0F + pixels + 7.0F * f + h);
            } else {
                float f2 = Math.max(pixels, g);
                return Math.round(
                        6.0F * f * 2.0F
                                + i * f2
                                + (i - 1) * 5.0F * f
                                + (this.hands ? 11.0F * f - 5.0F * f : 0.0F));
            }
        }
    }

    float planHeight(float f) {
        int i = this.order().length;
        float pixels = ItemIcons.pixels(18.0F * f);
        if (this.column) {
            return Math.round(
                    6.0F * f * 2.0F
                            + i * pixels
                            + (i - 1) * 4.0F * f
                            + (this.hands ? 11.0F * f - 4.0F * f : 0.0F));
        } else {
            float g =
                    (this.gauge ? 3.0F * f + gaugeHeight(f) : 0.0F)
                            + (this.percent ? 2.0F * f + HudChrome.px(14.0F, f) : 0.0F);
            return Math.round(6.0F * f * 2.0F + pixels + g);
        }
    }

    private static float percentWidth(float f) {
        return HudChrome.tabularWidth(
                ImGuiManager.getInstance().hudLabelFont(), HudChrome.px(14.0F, f), "100%");
    }

    private static float gaugeHeight(float f) {
        return Math.max(2.0F, (float) Math.round(3.0F * f));
    }

    void draw(
            ImDrawList imDrawList,
            HudLayoutStore.HudElementState hudElementState,
            float f,
            float g,
            float h,
            boolean bl,
            MinecraftClient client) {
        float f2 = h * HudGlyphs.easeOutQuint(this.presence);
        if (!(f2 <= 0.004F) && client.player != null) {
            float scale = HudRenderer.scaleOf(hudElementState);
            float width = hudElementState.width;
            float height = hudElementState.height;
            Surface surface = HudRenderer.hudSurface(hudElementState);
            boolean needsTextGuard = HudChrome.needsTextGuard(surface);
            HudChrome.card(
                    imDrawList,
                    surface,
                    f,
                    g,
                    width,
                    height,
                    HudRenderer.hudRounding() * scale,
                    f2,
                    bl,
                    scale);
            ImFont imFont = ImGuiManager.getInstance().hudLabelFont();
            int px = HudChrome.px(14.0F, scale);
            int pixels = (int) ItemIcons.pixels(18.0F * scale);
            float g2 = Math.round(6.0F * scale);
            float h2 = this.percent ? (float) Math.ceil(percentWidth(scale)) : 0.0F;
            float f3 = Math.max((float) pixels, h2);
            EquipmentSlot[] equipmentSlots = this.order();
            float g3 = f + g2;
            float h3 = g + g2;
            for (int k = 0; k < equipmentSlots.length; k++) {
                if (k == ARMOR.length) {
                    this.drawHandsRule(imDrawList, f, g, width, height, g3, h3, scale, f2);
                    if (this.column) {
                        h3 += 7.0F * scale;
                    } else {
                        g3 += 6.0F * scale;
                    }
                }
                ItemStack equippedStack;
                ItemStack stack;
                float f4 =
                        (stack =
                                                NoInvLeakModule.shownInHud(
                                                        equippedStack =
                                                                client.player.getEquippedStack(
                                                                        equipmentSlots[k]),
                                                        equipmentSlots[k]))
                                        == equippedStack
                                ? this.wear[k]
                                : -1.0F;
                if (this.column) {
                    this.drawSideways(
                            imDrawList,
                            stack,
                            f4,
                            g3,
                            Math.round(h3),
                            pixels,
                            h2,
                            imFont,
                            px,
                            scale,
                            f2,
                            needsTextGuard);
                    h3 += pixels + 4.0F * scale;
                } else {
                    this.drawStacked(
                            imDrawList,
                            stack,
                            f4,
                            Math.round(g3),
                            h3,
                            pixels,
                            f3,
                            imFont,
                            px,
                            scale,
                            f2,
                            needsTextGuard);
                    g3 += f3 + 5.0F * scale;
                }
            }
        }
    }

    private void drawHandsRule(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            float f3,
            float g3) {
        int ink = HudGlyphs.ink(-15408912, g3 * 0.45F);
        float h3 = Math.max(1.0F, (float) Math.round(f3));
        if (this.column) {
            float f4 = Math.round(h2 + (11.0F * f3 - 4.0F * f3) * 0.5F - 4.0F * f3 * 0.5F);
            HudGlyphs.register(imDrawList, f + h * 0.15F, f + h * 0.85F, f4, h3, ink);
        } else {
            float g4 = Math.round(g2 + (11.0F * f3 - 5.0F * f3) * 0.5F - 5.0F * f3 * 0.5F);
            int i = ink & 16777215;
            imDrawList.addRectFilledMultiColor(
                    g4, g + f2 * 0.18F, g4 + h3, g + f2 * 0.5F, i, i, ink, ink);
            imDrawList.addRectFilledMultiColor(
                    g4, g + f2 * 0.5F, g4 + h3, g + f2 * 0.82F, ink, ink, i, i);
        }
    }

    private void drawStacked(
            ImDrawList imDrawList,
            ItemStack stack,
            float f,
            float g,
            float h,
            int i,
            float f2,
            ImFont imFont,
            int j,
            float g2,
            float h2,
            boolean bl) {
        float f3 = Math.round(g + (f2 - i) * 0.5F);
        this.drawIcon(imDrawList, stack, f, f3, h, i, imFont, j, g2, h2);
        float g3 = h + i;
        if (this.gauge) {
            float h3;
            this.drawGauge(imDrawList, stack, f, g, h3 = g3 + 3.0F * g2, f2, g2, h2);
            g3 = h3 + gaugeHeight(g2);
        }
        if (this.percent && f >= 0.0F) {
            String percentText = percentText(f);
            float tabularWidth = HudChrome.tabularWidth(imFont, j, percentText);
            HudChrome.tabular(
                    imDrawList,
                    imFont,
                    j,
                    Math.round(g + (f2 - tabularWidth) * 0.5F),
                    Math.round(g3 + 2.0F * g2),
                    HudGlyphs.ink(HudGlyphs.vitality(f), h2 * this.criticalPulse(f)),
                    percentText,
                    bl);
        }
    }

    private void drawSideways(
            ImDrawList imDrawList,
            ItemStack stack,
            float f,
            float g,
            float h,
            int i,
            float f2,
            ImFont imFont,
            int j,
            float g2,
            float h2,
            boolean bl) {
        this.drawIcon(imDrawList, stack, f, g, h, i, imFont, j, g2, h2);
        float f3 = g + i + 7.0F * g2;
        float g3 = h + i * 0.5F;
        if (this.percent) {
            if (f >= 0.0F) {
                String percentText = percentText(f);
                float tabularWidth = HudChrome.tabularWidth(imFont, j, percentText);
                HudChrome.tabular(
                        imDrawList,
                        imFont,
                        j,
                        Math.round(f3 + f2 - tabularWidth),
                        Math.round(g3 - j * 0.5F),
                        HudGlyphs.ink(HudGlyphs.vitality(f), h2 * this.criticalPulse(f)),
                        percentText,
                        bl);
            }
            f3 += f2 + 7.0F * g2;
        }
        if (this.gauge) {
            this.drawGauge(
                    imDrawList,
                    stack,
                    f,
                    f3,
                    Math.round(g3 - gaugeHeight(g2) * 0.5F),
                    44.0F * g2,
                    g2,
                    h2);
        }
    }

    private void drawIcon(
            ImDrawList imDrawList,
            ItemStack stack,
            float f,
            float g,
            float h,
            int i,
            ImFont imFont,
            int j,
            float f2,
            float g2) {
        if (stack.isEmpty()) {
            float h2 = Math.max(1.0F, (float) Math.round(2.0F * f2));
            imDrawList.addRect(
                    g + h2,
                    h + h2,
                    g + i - h2,
                    h + i - h2,
                    HudChrome.scaleAlpha(536870911, g2),
                    Math.max(1.0F, 3.0F * f2),
                    0,
                    1.0F);
            imDrawList.addCircleFilled(
                    g + i * 0.5F,
                    h + i * 0.5F,
                    Math.max(1.0F, 1.2F * f2),
                    HudChrome.scaleAlpha(536870911, g2),
                    8);
        } else {
            if (f >= 0.0F && f < 0.12F) {
                float f3 = 0.5F + 0.5F * (float) Math.sin(this.clock * 6.5F);
                imDrawList.addCircleFilled(
                        g + i * 0.5F,
                        h + i * 0.5F,
                        i * 0.62F,
                        HudGlyphs.ink(-2537681, g2 * (0.18F + 0.22F * f3)),
                        20);
            }
            ItemIcons.draw(imDrawList, stack, g, h, (float) i, g2);
            if (stack.hasGlint()) {
                HudGlyphs.sparkle(
                        imDrawList,
                        g + i - 1.5F * f2,
                        h + 1.5F * f2,
                        2.8F * f2,
                        HudGlyphs.ink(-6621958, g2 * 0.9F));
            }
            int count;
            if ((count = stack.getCount()) > 1) {
                String countText = countText(count);
                float width = HudChrome.width(imFont, j, countText);
                HudChrome.text(
                        imDrawList,
                        imFont,
                        j,
                        Math.round(g + i - width + 2.0F * f2),
                        Math.round(h + i - j + 3.0F * f2),
                        HudChrome.scaleAlpha(-1, g2),
                        countText,
                        true);
            }
        }
    }

    private void drawGauge(
            ImDrawList imDrawList,
            ItemStack stack,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2) {
        float gaugeHeight = gaugeHeight(g2);
        float f3 = gaugeHeight * 0.5F;
        imDrawList.addRectFilled(
                g, h, g + f2, h + gaugeHeight, HudChrome.scaleAlpha(452984831, h2), f3);
        if (!stack.isEmpty() && !(f < 0.0F)) {
            float g3 = f2 * Math.max(0.0F, Math.min(1.0F, f));
            if (!(g3 < 0.5F)) {
                imDrawList.addRectFilled(
                        g,
                        h,
                        g + g3,
                        h + gaugeHeight,
                        HudGlyphs.ink(HudGlyphs.vitality(f), h2 * this.criticalPulse(f)),
                        Math.min(f3, g3 * 0.5F));
            }
        }
    }

    private float criticalPulse(float f) {
        return f >= 0.12F
                ? 1.0F
                : 0.6F + 0.4F * (0.5F + 0.5F * (float) Math.sin(this.clock * 6.5F));
    }

    private static String percentText(float f) {
        int i = Math.max(0, Math.min(100, Math.round(f * 100.0F)));
        String string = PERCENT[i];
        if (string == null) {
            PERCENT[i] = string = i + "%";
        }
        return string;
    }

    private static String countText(int i) {
        if (i >= COUNT.length) {
            return Integer.toString(i);
        } else {
            String string = COUNT[i];
            if (string == null) {
                COUNT[i] = string = Integer.toString(i);
            }
            return string;
        }
    }
}
