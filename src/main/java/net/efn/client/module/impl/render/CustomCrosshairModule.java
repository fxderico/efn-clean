package net.efn.client.module.impl.render;

import imgui.ImDrawList;
import imgui.ImGui;

import net.efn.client.gui.EfnTheme;
import net.efn.client.gui.EfnWidgets;
import net.efn.client.gui.GuiPalette;
import net.efn.client.gui.ThemeTint;
import net.efn.client.imgui.ImGuiManager;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

@Environment(EnvType.CLIENT)
public final class CustomCrosshairModule extends ClientModule implements HudOverlayProvider {

    public static volatile CustomCrosshairModule active;

    private static final String STYLE_CROSS = "Cross";

    private static final String STYLE_T = "T";

    private static final String STYLE_X = "X";

    private static final String STYLE_DOT = "Dot";

    private static final String STYLE_CIRCLE = "Circle";

    private static final String COLOR_CUSTOM = "Custom";

    private static final String COLOR_ACCENT = "Accent";

    private static final int OUTLINE = -1946157056;

    private static final int TRACK_ARGB = -870967267;

    private static final float BAR_HALF_WIDTH = 10.0F;

    private static final float BAR_HEIGHT = 3.0F;

    private static final float DIAGONAL = 0.70710677F;

    private static final double FULL_SPREAD_SPEED = 0.28;

    private static final float TINT_RATE = 14.0F;

    private static final float SPREAD_RATE = 12.0F;

    private static final float FADE_RATE = 18.0F;

    private final ModeSetting style =
            this.addSetting(
                    "style", new ModeSetting("Style", "Cross", "Cross", "T", "X", "Dot", "Circle"));

    private final NumberSetting size =
            this.addSetting("size", new NumberSetting("Size", 6.0F, 1.0F, 20.0F).step(1.0F));

    private final NumberSetting gap =
            this.addSetting("gap", new NumberSetting("Gap", 2.0F, 0.0F, 16.0F).step(1.0F));

    private final NumberSetting thickness =
            this.addSetting(
                    "thickness", new NumberSetting("Thickness", 2.0F, 1.0F, 5.0F).step(1.0F));

    private final BooleanSetting centerDot =
            this.addSetting("center-dot", new BooleanSetting("Center Dot", false));

    private final BooleanSetting outline =
            this.addSetting("outline", new BooleanSetting("Outline", true));

    private final ModeSetting colorMode =
            this.addSetting("color-mode", new ModeSetting("Color", "Custom", "Custom", "Accent"));

    private final ColorSetting color =
            this.addSetting("color", new ColorSetting("Custom Color", -657929));

    private final BooleanSetting targetTint =
            this.addSetting("target-tint", new BooleanSetting("Target Tint", false));

    private final ColorSetting targetColor =
            this.addSetting("target-color", new ColorSetting("Target Color", -47814));

    private final BooleanSetting dynamicGap =
            this.addSetting("dynamic-gap", new BooleanSetting("Dynamic Gap", false));

    private final NumberSetting spread =
            this.addSetting("spread", new NumberSetting("Spread", 4.0F, 1.0F, 12.0F).step(1.0F));

    private float tint;

    private float extraGap;

    private float barAlpha;

    private float barFill;

    public CustomCrosshairModule() {
        super(
                "custom-crosshair",
                "Custom Crosshair",
                "Draws a configurable crosshair in place of the vanilla one.",
                ModuleCategory.VISUALS);
        this.gap.visibleWhen(this::hasArms);
        this.centerDot.visibleWhen(() -> !this.style.is("Dot"));
        this.color.visibleWhen(() -> this.colorMode.is("Custom"));
        this.targetColor.visibleWhen(this.targetTint::get);
        this.spread.visibleWhen(() -> this.dynamicGap.get() && !this.style.is("Dot"));
    }

    @Override
    protected void onEnable() {
        this.tint = 0.0F;
        this.extraGap = 0.0F;
        this.barAlpha = 0.0F;
        this.barFill = 1.0F;
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled();
    }

    @Override
    public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && isVisible(client)) {
            float h = Math.min(Math.max(ImGui.getIO().getDeltaTime(), 0.0F), 0.1F);
            float uiScale = ImGuiManager.getInstance().uiScale();
            this.updateMotion(client, player, h);
            CustomCrosshairModule.Geometry geometry = this.geometry(f * 0.5F, g * 0.5F, uiScale);
            int abgr = GuiPalette.abgr(this.currentColor());
            float f2 = (abgr >>> 24 & 0xFF) / 255.0F;
            float g2 = this.outline.get() ? Math.max(1.0F, (float) Math.round(uiScale)) : 0.0F;
            if (g2 > 0.0F) {
                this.drawShape(imDrawList, geometry, EfnWidgets.withAlpha(-1946157056, f2), g2);
            }
            this.drawShape(imDrawList, geometry, abgr, 0.0F);
            this.drawCooldown(imDrawList, client, player, geometry, abgr, g2, uiScale, h);
        }
    }

    private static boolean isVisible(MinecraftClient client) {
        if (client.world == null || client.interactionManager == null || client.options.hudHidden) {
            return false;
        } else if (client.currentScreen != null && !(client.currentScreen instanceof ChatScreen)) {
            return false;
        } else if (!client.options.getPerspective().isFirstPerson()) {
            return false;
        } else {
            return client.debugHudEntryList.isEntryVisible(
                            DebugHudEntries.THREE_DIMENSIONAL_CROSSHAIR)
                    ? false
                    : client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR
                            || spectatorCanInteract(client);
        }
    }

    private static boolean spectatorCanInteract(MinecraftClient client) {
        HitResult crosshairTarget = client.crosshairTarget;
        if (crosshairTarget instanceof EntityHitResult entityHitResult) {
            return entityHitResult.getEntity() instanceof NamedScreenHandlerFactory;
        } else if (crosshairTarget instanceof BlockHitResult blockHitResult
                && crosshairTarget.getType() == Type.BLOCK) {
            BlockPos pos = blockHitResult.getBlockPos();
            return client.world.getBlockState(pos).createScreenHandlerFactory(client.world, pos)
                    != null;
        } else {
            return false;
        }
    }

    private void updateMotion(MinecraftClient client, ClientPlayerEntity player, float f) {
        boolean bl;
        label23:
        {
            if (this.targetTint.get()) {
                Entity targetedEntity = client.targetedEntity;
                LivingEntity entity;
                if (client.targetedEntity instanceof LivingEntity
                        && (entity = (LivingEntity) targetedEntity).isAlive()) {
                    bl = true;
                    break label23;
                }
            }
            bl = false;
        }
        boolean bl2 = bl;
        this.tint = approach(this.tint, bl2 ? 1.0F : 0.0F, 14.0F, f);
        float g = 0.0F;
        if (this.dynamicGap.get()) {
            double d = Math.hypot(player.getX() - player.lastX, player.getZ() - player.lastZ);
            float h = (float) Math.min(1.0, d / 0.28);
            float f2 = 1.0F - player.getAttackCooldownProgress(0.0F);
            g = this.spread.get() * Math.max(h, f2);
        }
        this.extraGap = approach(this.extraGap, g, 12.0F, f);
    }

    private static float approach(float f, float g, float h, float f2) {
        float g2 = f + (g - f) * (1.0F - (float) Math.exp(-h * f2));
        return Math.abs(g - g2) < 0.001F ? g : g2;
    }

    private int currentColor() {
        int i =
                this.colorMode.is("Accent")
                        ? CosmeticAccent.argb()
                        : ThemeTint.world(this.color.argb());
        return this.tint <= 0.0F ? i : EfnTheme.lerpColor(i, this.targetColor.argb(), this.tint);
    }

    private boolean hasArms() {
        return this.style.is("Cross") || this.style.is("T") || this.style.is("X");
    }

    private CustomCrosshairModule.Geometry geometry(float f, float g, float h) {
        float f2 = Math.max(1.0F, (float) Math.round(this.thickness.get() * h));
        float g2 = Math.max(1.0F, (float) Math.round(this.size.get() * h));
        float h2 = this.hasArms() ? this.gap.get() : 0.0F;
        float f3 = Math.round((h2 + this.extraGap) * h);
        float g3 = this.dynamicGap.get() && !this.style.is("Dot") ? this.spread.get() : 0.0F;
        float reachBelow = this.reachBelow(g2, Math.round((h2 + g3) * h), f2);
        return new CustomCrosshairModule.Geometry(f, g, g2, f3, f2, reachBelow);
    }

    private float reachBelow(float f, float g, float h) {
        String style2 = this.style.get();
        float f2 =
                switch (style2) {
                    case "Dot" -> f * 0.5F;
                    case "Circle" -> f + g + h * 0.5F;
                    case "X" -> (g + f) * 0.70710677F + h * 0.5F;
                    default -> g + f + h * 0.5F;
                };
        return this.centerDot.get() ? Math.max(f2, h * 0.5F) : f2;
    }

    private boolean armsJoined(CustomCrosshairModule.Geometry geometry) {
        return (this.style.is("Cross") || this.style.is("T")) && geometry.open() < 1.0F;
    }

    private void drawShape(
            ImDrawList imDrawList, CustomCrosshairModule.Geometry geometry, int i, float f) {
        String style2 = this.style.get();
        switch (style2) {
            case "Dot":
                imDrawList.addCircleFilled(
                        geometry.cx(),
                        geometry.cy(),
                        Math.max(1.0F, geometry.size() * 0.5F) + f,
                        i,
                        0);
                break;
            case "Circle":
                imDrawList.addCircle(
                        geometry.cx(),
                        geometry.cy(),
                        geometry.size() + geometry.open(),
                        i,
                        0,
                        geometry.thick() + f * 2.0F);
                break;
            case "X":
                drawDiagonals(imDrawList, geometry, i, f);
                break;
            default:
                drawArms(imDrawList, geometry, i, f, !this.style.is("T"));
        }
        if (this.centerDot.get() && !this.style.is("Dot") && !this.armsJoined(geometry)) {
            float g = (float) Math.floor(geometry.cx() - geometry.thick() * 0.5F);
            float h = (float) Math.floor(geometry.cy() - geometry.thick() * 0.5F);
            rect(imDrawList, g, h, g + geometry.thick(), h + geometry.thick(), i, f);
        }
    }

    private static void drawArms(
            ImDrawList imDrawList,
            CustomCrosshairModule.Geometry geometry,
            int i,
            float f,
            boolean bl) {
        float g = (float) Math.floor(geometry.cx() - geometry.thick() * 0.5F);
        float h = g + geometry.thick();
        float f2 = (float) Math.floor(geometry.cy() - geometry.thick() * 0.5F);
        float g2 = f2 + geometry.thick();
        float open2 = geometry.open();
        float h2 = geometry.open() + geometry.size();
        if (open2 < 1.0F) {
            rect(imDrawList, g - h2, f2, h + h2, g2, i, f);
            imDrawList.addRectFilled(g - f, g2 + f, h + f, g2 + h2 + f, i);
            if (bl) {
                imDrawList.addRectFilled(g - f, f2 - h2 - f, h + f, f2 - f, i);
            }
        } else {
            rect(imDrawList, g - h2, f2, g - open2, g2, i, f);
            rect(imDrawList, h + open2, f2, h + h2, g2, i, f);
            rect(imDrawList, g, g2 + open2, h, g2 + h2, i, f);
            if (bl) {
                rect(imDrawList, g, f2 - h2, h, f2 - open2, i, f);
            }
        }
    }

    private static void drawDiagonals(
            ImDrawList imDrawList, CustomCrosshairModule.Geometry geometry, int i, float f) {
        float g = Math.max(0.0F, geometry.open() - f) * 0.70710677F;
        float h = (geometry.open() + geometry.size() + f) * 0.70710677F;
        float f2 = geometry.thick() + f * 2.0F;
        for (int j = -1; j <= 1; j += 2) {
            for (int k = -1; k <= 1; k += 2) {
                imDrawList.addLine(
                        geometry.cx() + j * g,
                        geometry.cy() + k * g,
                        geometry.cx() + j * h,
                        geometry.cy() + k * h,
                        i,
                        f2);
            }
        }
    }

    private static void rect(
            ImDrawList imDrawList, float f, float g, float h, float f2, int i, float g2) {
        imDrawList.addRectFilled(f - g2, g - g2, h + g2, f2 + g2, i);
    }

    private void drawCooldown(
            ImDrawList imDrawList,
            MinecraftClient client,
            ClientPlayerEntity player,
            CustomCrosshairModule.Geometry geometry,
            int i,
            float f,
            float g,
            float h) {
        boolean bl = client.options.getAttackIndicator().getValue() == AttackIndicator.CROSSHAIR;
        float attackCooldownProgress = player.getAttackCooldownProgress(0.0F);
        boolean bl2 = bl && readyOnTarget(client, player, attackCooldownProgress);
        this.barFill = bl2 ? 1.0F : Math.min(1.0F, attackCooldownProgress);
        this.barAlpha =
                approach(
                        this.barAlpha,
                        !bl || !bl2 && !(attackCooldownProgress < 1.0F) ? 0.0F : 1.0F,
                        18.0F,
                        h);
        if (!(this.barAlpha < 0.01F)) {
            float f2 = Math.max(12.0F, 2.0F * Math.round(10.0F * g));
            float g2 = Math.max(2.0F, (float) Math.round(3.0F * g));
            float h2 = (float) Math.floor(geometry.cx() - f2 * 0.5F);
            float f3 = Math.round(geometry.cy() + geometry.below() + f + 5.0F * g);
            float g3 = this.barAlpha * (i >>> 24 & 0xFF) / 255.0F;
            if (f > 0.0F) {
                imDrawList.addRectFilled(
                        h2 - f,
                        f3 - f,
                        h2 + f2 + f,
                        f3 + g2 + f,
                        EfnWidgets.withAlpha(-1946157056, g3),
                        g2 * 0.5F + f);
            }
            imDrawList.addRectFilled(
                    h2,
                    f3,
                    h2 + f2,
                    f3 + g2,
                    EfnWidgets.withAlpha(GuiPalette.abgr(-870967267), g3),
                    g2 * 0.5F);
            float h3 = Math.round(f2 * this.barFill);
            if (h3 >= 1.0F) {
                imDrawList.addRectFilled(
                        h2,
                        f3,
                        h2 + h3,
                        f3 + g2,
                        EfnWidgets.withAlpha(i | 0xFF000000, g3),
                        g2 * 0.5F);
            }
        }
    }

    private static boolean readyOnTarget(
            MinecraftClient client, ClientPlayerEntity player, float f) {
        if (!(f < 1.0F)) {
            Entity targetedEntity = client.targetedEntity;
            LivingEntity entity;
            if (client.targetedEntity instanceof LivingEntity
                    && (entity = (LivingEntity) targetedEntity).isAlive()) {
                if (player.getAttackCooldownProgressPerTick() <= 5.0F) {
                    return false;
                }
                AttackRangeComponent attackRangeComponent =
                        player.getActiveOrMainHandStack().get(DataComponentTypes.ATTACK_RANGE);
                return attackRangeComponent == null
                        || client.crosshairTarget == null
                        || attackRangeComponent.isWithinRange(
                                player, client.crosshairTarget.getPos());
            }
        }
        return false;
    }

    @Environment(EnvType.CLIENT)
    private record Geometry(float cx, float cy, float size, float open, float thick, float below) {}
}
