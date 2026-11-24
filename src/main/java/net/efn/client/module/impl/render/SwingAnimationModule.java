package net.efn.client.module.impl.render;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.combat.CombatTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public final class SwingAnimationModule extends ClientModule {

    public static volatile SwingAnimationModule active;

    private static final String KHOPESH = "Slash";

    private static final String BOLT = "Chop";

    private static final String SICKLE = "Hook";

    private static final String SPIN = "Spin";

    private static final String TAP = "Double Tap";

    private static final String PUSH = "Push";

    private static final long COMBAT_WINDOW_MS = 3000L;

    private static final float PI = 3.1415927F;

    private static final float TAU = 6.2831855F;

    private final ModeSetting style =
            this.addSetting(
                    "style",
                    new ModeSetting(
                            "Style",
                            "Slash",
                            "Slash",
                            "Chop",
                            "Hook",
                            "Spin",
                            "Double Tap",
                            "Push"));

    private final NumberSetting tempo =
            this.addSetting("tempo", new NumberSetting("Tempo", 1.0F, 0.4F, 2.5F).step(0.05F));

    private final NumberSetting intensity =
            this.addSetting(
                    "intensity", new NumberSetting("Intensity", 1.0F, 0.2F, 2.0F).step(0.05F));

    private final BooleanSetting offhand =
            this.addSetting("offhand", new BooleanSetting("Offhand Too", true));

    private final BooleanSetting combatOnly =
            this.addSetting("combat-only", new BooleanSetting("Combat Only", false));

    private int styleIndex;

    private boolean styled;

    public SwingAnimationModule() {
        super(
                "swing-animation",
                "Swing Animation",
                "Changes your first-person swing animation.",
                ModuleCategory.VISUALS);
        this.style.onChange(this::cacheStyle);
        this.cacheStyle();
    }

    @Override
    protected void onEnable() {
        this.latch();
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
    }

    private void cacheStyle() {
        this.styleIndex = Math.max(0, this.style.modes().indexOf(this.style.get()));
    }

    public void latch() {
        this.styled = !this.combatOnly.get() || inCombat();
    }

    private static boolean inCombat() {
        if (CombatTarget.lastHit(3000L) != null) {
            return true;
        } else {
            Entity targetedEntity = MinecraftClient.getInstance().targetedEntity;
            return (targetedEntity instanceof PlayerEntity || targetedEntity instanceof Monster)
                    && targetedEntity.isAlive();
        }
    }

    public boolean covers(boolean bl) {
        return (bl || this.offhand.get()) && this.styled;
    }

    public int swingTicks(int i) {
        return !this.styled ? i : Math.max(1, Math.round(i / this.tempo.get()));
    }

    public void pose(MatrixStack matrices, float f, int i) {
        if (!(f <= 0.0F) && !(f >= 1.0F)) {
            float intensity2 = this.intensity.get();
            switch (this.styleIndex) {
                case 1:
                    bolt(matrices, f, i, intensity2);
                    break;
                case 2:
                    sickle(matrices, f, i, intensity2);
                    break;
                case 3:
                    spin(matrices, f, i, intensity2);
                    break;
                case 4:
                    doubleTap(matrices, f, i, intensity2);
                    break;
                case 5:
                    push(matrices, f, i, intensity2);
                    break;
                default:
                    khopesh(matrices, f, i, intensity2);
            }
        }
    }

    private static void khopesh(MatrixStack matrices, float f, int i, float g) {
        float h = crest(f, 0.3F) * g;
        matrices.translate(i * -0.3F * h, 0.05F * MathHelper.sin(3.1415927F * f) * g, -0.1F * h);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 64.0F * h));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 48.0F * h));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-14.0F * h));
    }

    private static void bolt(MatrixStack matrices, float f, int i, float g) {
        float h = f < 0.28F ? MathHelper.sin(3.1415927F * f / 0.28F) * g : 0.0F;
        float f2 = f <= 0.18F ? 0.0F : crest((f - 0.18F) / 0.82F, 0.28F) * g;
        matrices.translate(i * -0.06F * f2, 0.16F * h - 0.1F * f2, 0.06F * h - 0.24F * f2);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * h - 105.0F * f2));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 10.0F * f2));
    }

    private static void sickle(MatrixStack matrices, float f, int i, float g) {
        float crest = crest(f, 0.42F);
        float h = crest * crest * g;
        float f2;
        matrices.translate(i * -0.1F * (f2 = crest * g), -0.24F * f2, -0.06F * f2);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -100.0F * f2));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 30.0F * h));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F * f2));
    }

    private static void spin(MatrixStack matrices, float f, int i, float g) {
        float h = 6.2831855F * f;
        float f2 = MathHelper.cos(h);
        float g2 = MathHelper.sin(h);
        float h2 = MathHelper.sin(3.1415927F * f) * g;
        float f3 = 0.17F * g;
        matrices.translate(i * f3 * 0.6F * (f2 - 1.0F), f3 * g2, -0.12F * h2);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 40.0F * g2 * g));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40.0F * h2));
    }

    private static void doubleTap(MatrixStack matrices, float f, int i, float g) {
        float h = MathHelper.sin(6.2831855F * f);
        float f2 = h * h * (f < 0.5F ? 1.0F : 0.65F) * g;
        matrices.translate(i * -0.08F * f2, 0.06F * f2, -0.1F * f2);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 26.0F * f2));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F * f2));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -22.0F * f2));
    }

    private static void push(MatrixStack matrices, float f, int i, float g) {
        float h = (0.5F - 0.5F * MathHelper.cos(6.2831855F * f)) * g;
        matrices.translate(0.0F, -0.06F * h, -0.1F * h);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-24.0F * h));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 10.0F * h));
    }

    private static float crest(float f, float g) {
        if (f <= 0.0F || f >= 1.0F) {
            return 0.0F;
        } else if (f < g) {
            float h = 1.0F - f / g;
            return 1.0F - h * h * h;
        } else {
            float f2 = (f - g) / (1.0F - g);
            return 1.0F - f2 * f2 * (3.0F - 2.0F * f2);
        }
    }
}
