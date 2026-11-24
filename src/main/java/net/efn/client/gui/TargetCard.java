package net.efn.client.gui;

import com.mojang.blaze3d.textures.GpuTexture;

import imgui.ImDrawList;
import imgui.ImFont;

import net.efn.client.config.Surface;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.impl.client.HudModule;
import net.efn.client.module.impl.misc.NameProtectModule;
import net.efn.client.util.combat.CombatTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
final class TargetCard {

    private static final long COMBAT_MEMORY_MS = 4000L;

    private static final float LINGER = 0.8F;

    private static final float SHOW_TIME = 0.2F;

    private static final float HIDE_TIME = 0.34F;

    private static final float ENTER_LIFT = 7.0F;

    private static final float HEALTH_RATE = 13.0F;

    private static final float TRAIL_HOLD = 0.4F;

    private static final float TRAIL_RATE = 4.5F;

    private static final float STRIKE_TIME = 0.45F;

    private static final long NAME_REFRESH_MS = 1000L;

    private static final float PAD = 8.0F;

    private static final float PORTRAIT = 40.0F;

    private static final float FACE = 32.0F;

    private static final float CONTENT_H = 46.0F;

    private static final float COLUMN_GAP = 10.0F;

    private static final float COLUMN_W = 178.0F;

    private static final float NAME_ROW = 18.0F;

    private static final float BAR_TOP = 22.0F;

    private static final float BAR_H = 6.0F;

    private static final float GEAR = 16.0F;

    private static final float GEAR_GAP = 3.0F;

    private static final float BOLT_H = 11.0F;

    private static final float BOLT_GAP = 4.0F;

    private static final float CLUSTER_GAP = 8.0F;

    private static final int MAT = 1493172224;

    private static final int NOTCH = 1191182336;

    private static final String ELLIPSIS = "...";

    private static final float SKIN_FACE_U = 0.125F;

    private static final float SKIN_HAT_U = 0.625F;

    private static final float SKIN_TOP_V = 0.125F;

    private static final float SKIN_PART = 0.125F;

    private static final EquipmentSlot[] GEAR_ORDER =
            new EquipmentSlot[] {
                EquipmentSlot.MAINHAND,
                EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
            };

    private LivingEntity target;

    private float presence;

    private float lost;

    private float health;

    private float trail;

    private float trailHold;

    private float absorb;

    private float lastHealth;

    private int lastHurt;

    private float strike;

    private float clock;

    private LivingEntity namedFor;

    private int nameGeneration = -2147483648;

    private long namedAt;

    private String rawName = "";

    private String fittedName = "";

    private int fittedWidth = -1;

    private int fittedPx = -1;

    private int distanceTenths = -2147483648;

    private String distanceText = "";

    private int healthTenths = -2147483648;

    private String healthText = "";

    private int absorbWhole = -2147483648;

    private String absorbText = "";

    private final Set<GpuTexture> crispSkins = Collections.newSetFromMap(new WeakHashMap<>());

    TargetCard() {}

    void reset() {
        this.target = null;
        this.namedFor = null;
        this.presence = 0.0F;
        this.lost = 0.0F;
    }

    boolean visible() {
        return this.target != null && this.presence > 0.0F;
    }

    float planWidth(float f) {
        return this.visible() ? Math.round(244.0F * f) : 1.0F;
    }

    float planHeight(float f) {
        return Math.round(62.0F * f);
    }

    void update(MinecraftClient client, HudModule module, boolean bl, boolean bl2, float clock2) {
        this.clock += clock2;
        if (this.target != null
                && (this.target.getEntityWorld() != client.world
                        || !bl2 && this.target == client.player)) {
            this.reset();
        }
        Object object = bl ? pick(client, module) : null;
        if (object == null && bl2) {
            object = client.player;
        }
        if (object != null && object != this.target) {
            this.adopt((LivingEntity) object);
        }
        this.lost = object != null ? 0.0F : this.lost + clock2;
        boolean bl3 = this.target != null && (object != null || this.lost < 0.8F);
        this.presence =
                bl2 && bl3
                        ? 1.0F
                        : MathHelper.clamp(
                                this.presence + (bl3 ? clock2 / 0.2F : -clock2 / 0.34F),
                                0.0F,
                                1.0F);
        if (this.presence <= 0.0F && !bl3) {
            this.target = null;
            this.namedFor = null;
        } else {
            if (this.target != null) {
                this.follow(clock2);
            }
        }
    }

    private static LivingEntity pick(MinecraftClient client, HudModule module) {
        LivingEntity entity =
                module.targetOnLook() ? CombatTarget.current(4000L) : CombatTarget.lastHit(4000L);
        if (entity == null || entity == client.player || entity instanceof ArmorStandEntity) {
            return null;
        } else {
            return module.targetPlayersOnly() && !(entity instanceof PlayerEntity) ? null : entity;
        }
    }

    private void adopt(LivingEntity target2) {
        this.target = target2;
        float f = Math.max(1.0F, target2.getMaxHealth());
        this.trail = this.lastHealth = MathHelper.clamp(target2.getHealth(), 0.0F, f);
        this.health = this.lastHealth;
        this.absorb = Math.max(0.0F, target2.getAbsorptionAmount());
        this.trailHold = 0.0F;
        this.strike = 0.0F;
        this.lastHurt = target2.hurtTime;
    }

    private void follow(float trailHold2) {
        float f = Math.max(1.0F, this.target.getMaxHealth());
        float g = MathHelper.clamp(this.target.getHealth(), 0.0F, f);
        if (g < this.lastHealth - 0.01F) {
            this.trailHold = 0.4F;
            this.strike = 1.0F;
        }
        if (this.target.hurtTime > this.lastHurt) {
            this.strike = 1.0F;
        }
        this.lastHurt = this.target.hurtTime;
        this.lastHealth = g;
        this.health = HudGlyphs.approach(this.health, g, 13.0F, trailHold2);
        if (this.trailHold > 0.0F) {
            this.trailHold -= trailHold2;
        } else {
            this.trail = HudGlyphs.approach(this.trail, this.health, 4.5F, trailHold2);
        }
        if (this.trail < this.health) {
            this.trail = this.health;
        }
        this.absorb =
                HudGlyphs.approach(
                        this.absorb,
                        Math.max(0.0F, this.target.getAbsorptionAmount()),
                        13.0F,
                        trailHold2);
        this.strike = Math.max(0.0F, this.strike - trailHold2 / 0.45F);
    }

    void draw(
            ImDrawList imDrawList,
            HudLayoutStore.HudElementState hudElementState,
            float f,
            float g,
            float h,
            boolean bl,
            HudModule module) {
        if (this.target != null) {
            float easeOutQuint = HudGlyphs.easeOutQuint(this.presence);
            float f2 = h * easeOutQuint;
            if (!(f2 <= 0.004F)) {
                MinecraftClient client = MinecraftClient.getInstance();
                float scale = HudRenderer.scaleOf(hudElementState);
                g = Math.round(g + 7.0F * scale * (1.0F - easeOutQuint));
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
                ImGuiManager imGuiManager = ImGuiManager.getInstance();
                ImFont imFont = imGuiManager.hudTextFont();
                ImFont imFont2 = imGuiManager.hudLabelFont();
                int px = HudChrome.px(17.0F, scale);
                int px2 = HudChrome.px(14.0F, scale);
                float g2 = f + Math.round(8.0F * scale);
                float h2 = g + Math.round(8.0F * scale);
                float f3 = Math.round(40.0F * scale);
                this.drawPortrait(
                        imDrawList,
                        client,
                        g2,
                        h2 + Math.round((46.0F * scale - f3) * 0.5F),
                        f3,
                        scale,
                        f2);
                float g3 = g2 + f3 + Math.round(10.0F * scale);
                float h3 = f + width - Math.round(8.0F * scale);
                float f4 = Math.max(1.0F, this.target.getMaxHealth());
                float g4 = MathHelper.clamp(this.health / f4, 0.0F, 1.0F);
                float h4 = h2 + (float) Math.floor((18.0F * scale - px) * 0.5F);
                float f5 = h4 + HudChrome.baselineOffset(imFont, px, imFont2, px2);
                this.refreshNumbers(client);
                float g5 =
                        this.absorbText.isEmpty()
                                ? 0.0F
                                : HudChrome.width(imFont2, px2, this.absorbText);
                float tabularWidth = HudChrome.tabularWidth(imFont, px, this.healthText);
                float h5 = 11.0F * scale;
                float f6 =
                        h5 * 0.62F
                                + 4.0F * scale
                                + tabularWidth
                                + (g5 > 0.0F ? 4.0F * scale + g5 : 0.0F);
                float g6 = h3 - f6;
                HudGlyphs.bolt(
                        imDrawList,
                        g6 + h5 * 0.31F,
                        h4 + px * 0.52F,
                        h5,
                        Math.max(1.0F, 1.3F * scale),
                        HudGlyphs.ink(-15408912, f2));
                float h6;
                HudChrome.tabular(
                        imDrawList,
                        imFont,
                        px,
                        h6 = g6 + (h5 * 0.62F + 4.0F * scale),
                        h4,
                        HudGlyphs.ink(HudGlyphs.vitality(g4), f2),
                        this.healthText,
                        needsTextGuard);
                if (g5 > 0.0F) {
                    HudChrome.text(
                            imDrawList,
                            imFont2,
                            px2,
                            h6 + tabularWidth + 4.0F * scale,
                            f5,
                            HudGlyphs.ink(-12750874, f2),
                            this.absorbText,
                            needsTextGuard);
                }
                String fittedName2 = this.fittedName(imFont, px, h3 - f6 - 8.0F * scale - g3);
                HudChrome.text(
                        imDrawList,
                        imFont,
                        px,
                        g3,
                        h4,
                        HudChrome.scaleAlpha(HudChrome.TEXT_NAME, f2),
                        fittedName2,
                        needsTextGuard);
                float f7 = h2 + Math.round(22.0F * scale);
                this.drawBar(
                        imDrawList,
                        g3,
                        f7,
                        h3,
                        Math.max(3.0F, (float) Math.round(6.0F * scale)),
                        f4,
                        scale,
                        f2);
                int pixels = (int) ItemIcons.pixels(16.0F * scale);
                float g7 = h2 + Math.round(46.0F * scale) - pixels;
                float width2 = HudChrome.width(imFont2, px2, this.distanceText);
                float h7 = h3 - width2;
                HudChrome.text(
                        imDrawList,
                        imFont2,
                        px2,
                        h7,
                        g7 + Math.round((pixels - px2) * 0.5F),
                        HudChrome.scaleAlpha(HudChrome.TEXT_LABEL, f2),
                        this.distanceText,
                        needsTextGuard);
                if (module.targetGear()) {
                    this.drawGear(imDrawList, g3, g7, pixels, h7 - 6.0F * scale, scale, f2);
                }
                float f8 = Math.max(1.0F, (float) Math.round(scale));
                HudGlyphs.register(
                        imDrawList,
                        f + width * 0.12F,
                        f + width * 0.88F,
                        g + height - f8 - Math.max(1.0F, (float) Math.round(2.0F * scale)),
                        f8,
                        HudGlyphs.ink(-15408912, f2 * 0.35F));
            }
        }
    }

    private void drawPortrait(
            ImDrawList imDrawList,
            MinecraftClient client,
            float f,
            float g,
            float h,
            float f2,
            float g2) {
        float h2 = Math.min(HudRenderer.hudRounding(), 6.0F) * f2;
        imDrawList.addRectFilled(f, g, f + h, g + h, HudChrome.scaleAlpha(1493172224, g2), h2);
        float f3 = Math.max(8.0F, Math.round(32.0F * f2 / 8.0F) * 8.0F);
        if (f3 > h - 4.0F) {
            f3 = Math.max(8.0F, f3 - 8.0F);
        }
        float g3 = Math.round(f + (h - f3) * 0.5F);
        float h3 = Math.round(g + (h - f3) * 0.5F);
        boolean bl = false;
        if (this.target instanceof AbstractClientPlayerEntity player) {
            int skinTexture = this.skinTexture(client, player);
            if (skinTexture != 0) {
                float f4 = Math.max(1.0F, (float) Math.round(2.0F * f2));
                int scaleAlpha = HudChrome.scaleAlpha(-1, g2);
                imDrawList.addImageRounded(
                        skinTexture,
                        g3,
                        h3,
                        g3 + f3,
                        h3 + f3,
                        0.125F,
                        0.125F,
                        0.25F,
                        0.25F,
                        scaleAlpha,
                        f4,
                        240);
                imDrawList.addImageRounded(
                        skinTexture,
                        g3,
                        h3,
                        g3 + f3,
                        h3 + f3,
                        0.625F,
                        0.125F,
                        0.75F,
                        0.25F,
                        scaleAlpha,
                        f4,
                        240);
                bl = true;
            }
        } else {
            SpawnEggItem spawnEggItem = SpawnEggItem.forEntity(this.target.getType());
            if (spawnEggItem != null) {
                float pixels = ItemIcons.pixels(f3);
                float g4 = Math.round(f + (h - pixels) * 0.5F);
                float h4 = Math.round(g + (h - pixels) * 0.5F);
                ItemIcons.draw(imDrawList, spawnEggItem, g4, h4, pixels, g2);
                bl = true;
            }
        }
        if (!bl) {
            HudGlyphs.reticle(
                    imDrawList,
                    f + h * 0.5F,
                    g + h * 0.46F,
                    h * 0.3F,
                    Math.max(1.0F, 1.4F * f2),
                    HudGlyphs.ink(-15408912, g2 * 0.85F));
        }
        int ink =
                HudGlyphs.ink(
                        HudDraw.mix(-15408912, -2537681, this.strike),
                        g2 * (0.7F + 0.3F * this.strike));
        imDrawList.addRect(f, g, f + h, g + h, ink, h2, 0, Math.max(1.0F, f2));
        float f5 = Math.max(1.0F, (float) Math.round(1.5F * f2));
        float g5 = g + h + Math.max(1.0F, (float) Math.round(f2));
        imDrawList.addRectFilled(
                f + h * 0.22F,
                g5,
                f + h * 0.78F,
                g5 + f5,
                HudGlyphs.ink(-15408912, g2 * 0.8F),
                f5 * 0.5F);
        if (this.strike > 0.0F) {
            float h5 = (1.0F - this.strike) * 6.0F * f2;
            imDrawList.addRect(
                    f - h5,
                    g - h5,
                    f + h + h5,
                    g + h + h5,
                    HudGlyphs.ink(-2537681, g2 * this.strike * 0.7F),
                    h2 + h5,
                    0,
                    Math.max(1.0F, 1.5F * f2));
        }
    }

    private void drawBar(
            ImDrawList imDrawList,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            float f3) {
        float g3 = h - f;
        float h3 = f2 * 0.5F;
        float f4 = MathHelper.clamp(this.health / g2, 0.0F, 1.0F);
        float g4 = MathHelper.clamp(this.trail / g2, 0.0F, 1.0F);
        imDrawList.addRectFilled(f, g, h, g + f2, HudChrome.scaleAlpha(452984831, f3), h3);
        if (g4 > f4 + 0.002F) {
            float h4 = f + g3 * g4;
            imDrawList.addRectFilled(
                    f,
                    g,
                    h4,
                    g + f2,
                    HudGlyphs.ink(-6621958, f3 * 0.55F),
                    Math.min(h3, (h4 - f) * 0.5F));
        }
        if (f4 > 0.0F) {
            float f5 = f + g3 * f4;
            float g5 = f3;
            if (f4 < 0.25F) {
                g5 = f3 * (0.72F + 0.28F * (0.5F + 0.5F * (float) Math.sin(this.clock * 7.5F)));
            }
            float f6 = Math.min(h3, (f5 - f) * 0.5F);
            imDrawList.addRectFilled(
                    f, g, f5, g + f2, HudGlyphs.ink(HudGlyphs.vitality(f4), g5), f6);
            imDrawList.addRectFilled(
                    f, g, f5, g + f2 * 0.45F, HudGlyphs.ink(-1, g5 * 0.16F), f6, 48);
        }
        float g6 = g2 <= 40.0F ? 2.0F : (g2 <= 200.0F ? 10.0F : g2 / 20.0F);
        float h6 = Math.max(1.0F, (float) Math.round(0.75F * h2));
        int scaleAlpha = HudChrome.scaleAlpha(1191182336, f3);
        for (float h5 = g6; h5 < g2 - 0.01F; h5 += g6) {
            float f7 = Math.round(f + g3 * h5 / g2);
            imDrawList.addRectFilled(f7, g, f7 + h6, g + f2, scaleAlpha);
        }
        float g7 = MathHelper.clamp(this.absorb / g2, 0.0F, 1.0F);
        if (g7 > 0.002F) {
            float h7 = f + g3 * g7;
            imDrawList.addRectFilled(
                    f,
                    g,
                    h7,
                    g + Math.max(1.5F, f2 * 0.45F),
                    HudGlyphs.ink(-12750874, f3 * 0.95F),
                    Math.min(h3, (h7 - f) * 0.5F),
                    48);
        }
    }

    private void drawGear(
            ImDrawList imDrawList, float f, float g, int i, float h, float f2, float g2) {
        float h2 = i + Math.round(3.0F * f2);
        for (EquipmentSlot equipmentSlot : GEAR_ORDER) {
            ItemStack equippedStack = this.target.getEquippedStack(equipmentSlot);
            if (!equippedStack.isEmpty()) {
                if (f + i > h) {
                    break;
                }
                ItemIcons.draw(imDrawList, equippedStack, f, g, (float) i, g2);
                if (equippedStack.hasGlint()) {
                    HudGlyphs.sparkle(
                            imDrawList,
                            f + i - 1.5F * f2,
                            g + 1.5F * f2,
                            2.6F * f2,
                            HudGlyphs.ink(-6621958, g2 * 0.9F));
                }
                if (equippedStack.isDamageable()
                        && equippedStack.getMaxDamage() > 0
                        && 1.0F - (float) equippedStack.getDamage() / equippedStack.getMaxDamage()
                                < 0.25F) {
                    float f3 = Math.max(1.0F, (float) Math.round(1.5F * f2));
                    imDrawList.addRectFilled(
                            f + 1.0F,
                            g + i + f3 * 0.5F,
                            f + i - 1.0F,
                            g + i + f3 * 1.5F,
                            HudGlyphs.ink(-2537681, g2),
                            f3 * 0.5F);
                }
                f += h2;
            }
        }
    }

    private void refreshNumbers(MinecraftClient client) {
        float f = Math.max(1.0F, this.target.getMaxHealth());
        int i = Math.round(MathHelper.clamp(this.target.getHealth(), 0.0F, f) * 10.0F);
        if (i != this.healthTenths) {
            this.healthTenths = i;
            String string =
                    this.healthText =
                            i % 10 == 0 ? Integer.toString(i / 10) : i / 10 + "." + i % 10;
        }
        int j;
        if ((j = Math.round(Math.max(0.0F, this.target.getAbsorptionAmount())))
                != this.absorbWhole) {
            this.absorbWhole = j;
            this.absorbText = j > 0 ? "+" + j : "";
        }
        int k =
                client.player == null
                        ? 0
                        : Math.round(client.player.distanceTo(this.target) * 10.0F);
        if (k != this.distanceTenths) {
            this.distanceTenths = k;
            this.distanceText = k / 10 + "." + k % 10 + "m";
        }
    }

    private String fittedName(ImFont imFont, int fittedPx2, float f) {
        int generation = NameProtectModule.generation();
        long measuringTimeMs = Util.getMeasuringTimeMs();
        if (this.target != this.namedFor
                || generation != this.nameGeneration
                || measuringTimeMs - this.namedAt > 1000L) {
            this.namedFor = this.target;
            this.nameGeneration = generation;
            this.namedAt = measuringTimeMs;
            String string = NameProtectModule.protect(this.target.getName()).getString();
            if (!string.equals(this.rawName)) {
                this.rawName = string;
                this.fittedWidth = -1;
            }
        }
        int i;
        if ((i = Math.max(0, (int) f)) == this.fittedWidth && fittedPx2 == this.fittedPx) {
            return this.fittedName;
        } else {
            this.fittedWidth = i;
            this.fittedPx = fittedPx2;
            this.fittedName = fit(imFont, fittedPx2, this.rawName, i);
            return this.fittedName;
        }
    }

    private static String fit(ImFont imFont, int i, String string, float f) {
        if (HudChrome.width(imFont, i, string) <= f) {
            return string;
        } else {
            float width = HudChrome.width(imFont, i, "...");
            int j = 0;
            int k = string.length();
            while (j < k) {
                int m = j + k + 1 >>> 1;
                if (HudChrome.width(imFont, i, string.substring(0, m)) + width <= f) {
                    j = m;
                } else {
                    k = m - 1;
                }
            }
            return j == 0 ? "" : string.substring(0, j).stripTrailing() + "...";
        }
    }

    private int skinTexture(MinecraftClient client, AbstractClientPlayerEntity player) {
        SkinTextures skin = player.getSkin();
        if (NameProtectModule.hidesName(player.getGameProfile().name())) {
            skin = NameProtectModule.hiddenHead(player.getUuid(), skin);
        }
        Identifier id = skin.body().texturePath();
        GpuTexture glTexture;
        try {
            glTexture = client.getTextureManager().getTexture(id).getGlTexture();
        } catch (IllegalStateException exception) {
            return 0;
        }
        GlTexture glTexture2;
        if (glTexture instanceof GlTexture && !(glTexture2 = (GlTexture) glTexture).isClosed()) {
            if (this.crispSkins.add(glTexture)) {
                int glGetInteger = GL11.glGetInteger(32873);
                GL11.glBindTexture(3553, glTexture2.getGlId());
                GL11.glTexParameteri(3553, 10241, 9728);
                GL11.glTexParameteri(3553, 10240, 9728);
                GL11.glBindTexture(3553, glGetInteger);
            }
            return glTexture2.getGlId();
        } else {
            return 0;
        }
    }
}
