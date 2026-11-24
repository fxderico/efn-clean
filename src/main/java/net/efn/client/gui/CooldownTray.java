package net.efn.client.gui;

import com.example.client.mixin.AccessorHudCooldownInstance;
import com.example.client.mixin.AccessorHudItemCooldowns;

import imgui.ImDrawList;
import imgui.ImFont;

import net.efn.client.config.Surface;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.impl.client.HudModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

@Environment(EnvType.CLIENT)
final class CooldownTray {

    private static final float PAD = 6.0F;

    private static final float TILE_W = 32.0F;

    private static final float TILE_GAP = 5.0F;

    private static final float DIAL = 26.0F;

    private static final float DIAL_ICON = 16.0F;

    private static final float DIAL_STROKE = 2.0F;

    private static final float BAR_ICON = 18.0F;

    private static final float BAR_H = 3.0F;

    private static final float BAR_GAP = 3.0F;

    private static final float TEXT_GAP = 2.0F;

    private static final float APPEAR_TIME = 0.18F;

    private static final float READY_TIME = 0.32F;

    private static final float VANISH_TIME = 0.22F;

    private static final float LOOKUP_INTERVAL = 0.5F;

    private static final float TICKS_PER_SECOND = 20.0F;

    private static final float TAU = 6.2831855F;

    private static final float TOP = -1.5707964F;

    private static final String[] TENTHS = new String[100];

    private static final String[] SECONDS = new String[100];

    private static final String[] MINUTES = new String[100];

    private final List<CooldownTray.Slot> slots = new ArrayList<>();

    private final List<CooldownTray.Slot> pool = new ArrayList<>();

    private final CooldownTray.Slot[] samples =
            new CooldownTray.Slot[] {new CooldownTray.Slot(), new CooldownTray.Slot()};

    private final List<CooldownTray.Slot> sampleView = List.of(this.samples);

    private ClientPlayerEntity owner;

    private boolean sampling;

    private boolean barStyle;

    private float clock;

    private float tickRate = 20.0F;

    CooldownTray() {
        this.samples[0].item = Items.ENDER_PEARL;
        this.samples[0].total = 20.0F;
        this.samples[1].item = Items.WIND_CHARGE;
        this.samples[1].total = 10.0F;
        for (CooldownTray.Slot sample : this.samples) {
            sample.presence = 1.0F;
        }
    }

    void reset() {
        for (CooldownTray.Slot slot : this.slots) {
            slot.group = null;
            slot.item = null;
            this.pool.add(slot);
        }
        this.slots.clear();
        this.owner = null;
    }

    boolean visible() {
        return this.sampling || !this.slots.isEmpty();
    }

    void update(MinecraftClient client, HudModule module, boolean bl, boolean bl2, float clock2) {
        this.clock += clock2;
        this.barStyle = module.cooldownBar();
        float f = client.world == null ? 20.0F : client.world.getTickManager().getTickRate();
        this.tickRate = f > 0.0F ? f : 20.0F;
        ClientPlayerEntity player = client.player;
        if (player != this.owner) {
            this.reset();
            this.owner = player;
        }
        for (CooldownTray.Slot slot : this.slots) {
            slot.live = false;
        }
        if (bl && player != null) {
            this.collect(client, player);
        }
        for (int i = this.slots.size() - 1; i >= 0; i--) {
            CooldownTray.Slot slot2 = this.slots.get(i);
            if (slot2.live) {
                slot2.presence = Math.min(1.0F, slot2.presence + clock2 / 0.18F);
                if (slot2.item == null && this.clock >= slot2.lookupAt && player != null) {
                    slot2.item = resolve(player, slot2.group);
                    slot2.lookupAt = this.clock + 0.5F;
                }
            } else {
                if (!slot2.done) {
                    slot2.done = true;
                    slot2.remaining = 0.0F;
                    float g = slot2.ready = bl ? 1.0F : 0.0F;
                }
                if (slot2.ready > 0.0F) {
                    slot2.ready = Math.max(0.0F, slot2.ready - clock2 / 0.32F);
                } else {
                    slot2.presence -= clock2 / 0.22F;
                    if (slot2.presence <= 0.0F) {
                        this.slots.remove(i);
                        slot2.group = null;
                        slot2.item = null;
                        this.pool.add(slot2);
                    }
                }
            }
        }
        boolean bl3 = this.sampling = bl2 && this.slots.isEmpty();
        if (this.sampling) {
            for (int j = 0; j < this.samples.length; j++) {
                float h = (this.clock * 0.45F + j * 0.43F) % 1.0F;
                this.samples[j].remaining = (1.0F - h) * this.samples[j].total;
            }
        }
    }

    private void collect(MinecraftClient client, ClientPlayerEntity player) {
        ItemCooldownManager itemCooldownManager = player.getItemCooldownManager();
        AccessorHudItemCooldowns accessorHudItemCooldowns =
                (AccessorHudItemCooldowns) itemCooldownManager;
        int efn$hudTickCount = accessorHudItemCooldowns.efn$hudTickCount();
        float tickProgress = client.getRenderTickCounter().getTickProgress(true);
        for (Entry entry : accessorHudItemCooldowns.efn$hudCooldowns().entrySet()) {
            AccessorHudCooldownInstance entry2 = (AccessorHudCooldownInstance) entry.getValue();
            int efn$hudStartTime = entry2.efn$hudStartTime();
            int efn$hudEndTime = entry2.efn$hudEndTime();
            float f = efn$hudEndTime - (efn$hudTickCount + tickProgress);
            if (!(f <= 0.0F)) {
                CooldownTray.Slot slot = this.find((Identifier) entry.getKey());
                if (slot == null) {
                    slot = this.open((Identifier) entry.getKey());
                }
                slot.live = true;
                slot.done = false;
                slot.ready = 0.0F;
                slot.remaining = f;
                slot.total = Math.max(1.0F, (float) (efn$hudEndTime - efn$hudStartTime));
            }
        }
    }

    private CooldownTray.Slot find(Identifier id) {
        for (CooldownTray.Slot slot : this.slots) {
            if (slot.group.equals(id)) {
                return slot;
            }
        }
        return null;
    }

    private CooldownTray.Slot open(Identifier id) {
        CooldownTray.Slot slot =
                this.pool.isEmpty()
                        ? new CooldownTray.Slot()
                        : this.pool.remove(this.pool.size() - 1);
        slot.group = id;
        slot.item = null;
        slot.lookupAt = 0.0F;
        slot.presence = 0.0F;
        slot.ready = 0.0F;
        slot.done = false;
        this.slots.add(slot);
        return slot;
    }

    private static Item resolve(ClientPlayerEntity player, Identifier id) {
        PlayerInventory inventory = player.getInventory();
        ItemCooldownManager itemCooldownManager = player.getItemCooldownManager();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && id.equals(itemCooldownManager.getGroup(stack))) {
                return stack.getItem();
            }
        }
        return Registries.ITEM.containsId(id) ? Registries.ITEM.get(id) : null;
    }

    float planWidth(float f) {
        if (!this.visible()) {
            return 1.0F;
        } else {
            float g = 0.0F;
            for (CooldownTray.Slot slot : this.sampling ? this.sampleView : this.slots) {
                g += HudGlyphs.easeOutQuint(slot.presence) * 37.0F * f;
            }
            return Math.max(
                    1.0F, (float) Math.round(6.0F * f * 2.0F + Math.max(0.0F, g - 5.0F * f)));
        }
    }

    float planHeight(float f) {
        float g =
                this.barStyle
                        ? ItemIcons.pixels(18.0F * f) + 3.0F * f + barHeight(f)
                        : Math.round(26.0F * f);
        return Math.round(6.0F * f * 2.0F + g + 2.0F * f + HudChrome.px(14.0F, f));
    }

    private static float barHeight(float f) {
        return Math.max(2.0F, (float) Math.round(3.0F * f));
    }

    void draw(
            ImDrawList imDrawList,
            HudLayoutStore.HudElementState hudElementState,
            float f,
            float g,
            float h,
            boolean bl,
            int i) {
        if (!(h <= 0.004F) && this.visible()) {
            List list = this.sampling ? this.sampleView : this.slots;
            float f2 = 0.0F;
            for (CooldownTray.Slot slot : (Iterable<CooldownTray.Slot>) list) {
                f2 += slot.presence;
            }
            float g2 = h * HudGlyphs.easeOutQuint(Math.min(1.0F, f2));
            if (!(g2 <= 0.004F)) {
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
                        g2,
                        bl,
                        scale);
                ImFont imFont = ImGuiManager.getInstance().hudLabelFont();
                int px = HudChrome.px(14.0F, scale);
                float h2 = Math.round(6.0F * scale);
                float f3 = 32.0F * scale;
                float g3 =
                        this.barStyle
                                ? ItemIcons.pixels(18.0F * scale) + 3.0F * scale + barHeight(scale)
                                : Math.round(26.0F * scale);
                float h3 = Math.round(g + h2 + g3 + 2.0F * scale);
                float f4 = 10.0F * scale;
                imDrawList.pushClipRect(f, g - f4, f + width, g + height + f4, true);
                float g4 = f + h2;
                for (CooldownTray.Slot slot2 : (Iterable<CooldownTray.Slot>) list) {
                    float easeOutQuint = HudGlyphs.easeOutQuint(slot2.presence);
                    float h4 = h * easeOutQuint;
                    if (h4 > 0.004F) {
                        if (this.barStyle) {
                            this.drawBar(
                                    imDrawList, slot2, Math.round(g4), g + h2, f3, scale, h4, i);
                        } else {
                            this.drawHalo(
                                    imDrawList,
                                    slot2,
                                    g4 + f3 * 0.5F,
                                    g + h2 + Math.round(26.0F * scale) * 0.5F,
                                    scale,
                                    h4,
                                    i);
                        }
                        this.drawCaption(
                                imDrawList,
                                slot2,
                                g4 + f3 * 0.5F,
                                h3,
                                imFont,
                                px,
                                scale,
                                h4,
                                needsTextGuard);
                    }
                    g4 += easeOutQuint * (f3 + 5.0F * scale);
                }
                imDrawList.popClipRect();
            }
        }
    }

    private void drawHalo(
            ImDrawList imDrawList,
            CooldownTray.Slot slot,
            float f,
            float g,
            float h,
            float f2,
            int i) {
        float g2 = Math.round(26.0F * h) * 0.5F - 2.0F * h * 0.5F;
        float h2 = Math.max(1.5F, 2.0F * h);
        float fraction = fraction(slot);
        imDrawList.addCircle(f, g, g2, HudChrome.scaleAlpha(452984831, f2), 0, h2);
        if (fraction > 0.0F) {
            int j = HudDraw.mix(i, GuiPalette.abgr(-15408912), 1.0F - fraction);
            HudGlyphs.arc(
                    imDrawList,
                    f,
                    g,
                    g2,
                    -1.5707964F,
                    6.2831855F * fraction,
                    h2,
                    HudChrome.scaleAlpha(j, f2));
        }
        int pixels = (int) ItemIcons.pixels(16.0F * h);
        drawItem(
                imDrawList,
                slot,
                f - pixels * 0.5F,
                g - pixels * 0.5F,
                pixels,
                h,
                f2 * (0.62F + 0.38F * (1.0F - fraction)));
        if (slot.ready > 0.0F) {
            float f3 = (1.0F - slot.ready) * 7.0F * h;
            imDrawList.addCircle(
                    f, g, g2 + f3, HudGlyphs.ink(-15408912, f2 * slot.ready * 0.9F), 0, h2);
        }
    }

    private void drawBar(
            ImDrawList imDrawList,
            CooldownTray.Slot slot,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            int i) {
        int pixels = (int) ItemIcons.pixels(18.0F * f2);
        float fraction = fraction(slot);
        float h2 = Math.round(f + (h - pixels) * 0.5F);
        drawItem(imDrawList, slot, h2, g, pixels, f2, g2 * (0.62F + 0.38F * (1.0F - fraction)));
        float f3 = g + pixels + 3.0F * f2;
        float barHeight = barHeight(f2);
        float g3 = Math.round(3.0F * f2);
        float h3 = f + g3;
        float f4 = f + h - g3;
        imDrawList.addRectFilled(
                h3, f3, f4, f3 + barHeight, HudChrome.scaleAlpha(452984831, g2), barHeight * 0.5F);
        if (fraction > 0.0F) {
            int j = HudDraw.mix(i, GuiPalette.abgr(-15408912), 1.0F - fraction);
            float g4 = (f4 - h3) * fraction;
            imDrawList.addRectFilled(
                    h3,
                    f3,
                    h3 + g4,
                    f3 + barHeight,
                    HudChrome.scaleAlpha(j, g2),
                    Math.min(barHeight * 0.5F, g4 * 0.5F));
        }
        if (slot.ready > 0.0F) {
            float ready2 = slot.ready;
            imDrawList.addRectFilled(
                    h3,
                    f3,
                    f4,
                    f3 + barHeight,
                    HudGlyphs.ink(-6621958, g2 * ready2),
                    barHeight * 0.5F);
        }
    }

    private static void drawItem(
            ImDrawList imDrawList,
            CooldownTray.Slot slot,
            float f,
            float g,
            int i,
            float h,
            float f2) {
        if (slot.item != null) {
            ItemIcons.draw(imDrawList, slot.item, f, g, (float) i, f2);
        } else {
            HudGlyphs.hourglass(
                    imDrawList,
                    f + i * 0.5F,
                    g + i * 0.5F,
                    i * 0.8F,
                    Math.max(1.0F, 1.3F * h),
                    HudGlyphs.ink(-3812390, f2));
        }
    }

    private void drawCaption(
            ImDrawList imDrawList,
            CooldownTray.Slot slot,
            float f,
            float g,
            ImFont imFont,
            int i,
            float h,
            float f2,
            boolean bl) {
        if (slot.done) {
            float g2 = i * 0.85F;
            HudGlyphs.bolt(
                    imDrawList,
                    f,
                    g + i * 0.5F,
                    g2,
                    Math.max(1.0F, 1.2F * h),
                    HudGlyphs.ink(-15408912, f2 * Math.max(slot.ready, 0.4F)));
        } else {
            String seconds = seconds(slot.remaining / this.tickRate);
            float tabularWidth = HudChrome.tabularWidth(imFont, i, seconds);
            HudChrome.tabular(
                    imDrawList,
                    imFont,
                    i,
                    Math.round(f - tabularWidth * 0.5F),
                    g,
                    HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, f2),
                    seconds,
                    bl);
        }
    }

    private static float fraction(CooldownTray.Slot slot) {
        return slot.total <= 0.0F
                ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, slot.remaining / slot.total));
    }

    private static String seconds(float f) {
        if (f < 9.95F) {
            int i = Math.max(1, Math.min(99, (int) Math.ceil(f * 10.0F)));
            String string = TENTHS[i];
            if (string == null) {
                TENTHS[i] = string = i / 10 + "." + i % 10 + "s";
            }
            return string;
        } else {
            int j = (int) Math.ceil(f);
            if (j < 100) {
                String string2 = SECONDS[j];
                if (string2 == null) {
                    SECONDS[j] = string2 = j + "s";
                }
                return string2;
            } else {
                int k = Math.min(99, (int) Math.ceil(f / 60.0F));
                String string3 = MINUTES[k];
                if (string3 == null) {
                    MINUTES[k] = string3 = k + "m";
                }
                return string3;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Slot {

        Identifier group;

        Item item;

        float lookupAt;

        float remaining;

        float total;

        float presence;

        float ready;

        boolean live;

        boolean done;

        private Slot() {}
    }
}
