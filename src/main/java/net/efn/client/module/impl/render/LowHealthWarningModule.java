package net.efn.client.module.impl.render;

import imgui.ImDrawList;
import imgui.ImGui;

import net.efn.client.gui.GuiPalette;
import net.efn.client.gui.HudDraw;
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
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public final class LowHealthWarningModule extends ClientModule implements HudOverlayProvider {

    private static final String PULSE_HEARTBEAT = "Heartbeat";

    private static final String PULSE_BREATH = "Breath";

    private static final String PULSE_STEADY = "Steady";

    private static final String COLOR_THEME = "Theme";

    private static final String COLOR_MANUAL = "Manual";

    private static final int OPAQUE = -16777216;

    private static final int WHITE = -1;

    private static final float EDGE_LIFT = 0.5F;

    private static final float RISE_RATE = 5.0F;

    private static final float FALL_RATE = 2.6F;

    private static final float DEATH_RATE = 12.0F;

    private static final float CALM_BPM = 64.0F;

    private static final float PANIC_BPM = 150.0F;

    private static final float SECOND_BEAT = 0.2F;

    private static final float BEAT_WIDTH = 0.065F;

    private static final long GAP_NANOS = 250000000L;

    private static final float TAU = 6.2831855F;

    private static final float[] BAND_REACH = new float[] {1.0F, 0.55F, 0.22F};

    private static final float[] BAND_WEIGHT = new float[] {0.5F, 0.34F, 0.3F};

    private final NumberSetting warnBelow =
            this.addSetting(
                    "warn-below", new NumberSetting("Warn Below", 7.0F, 2.0F, 20.0F).step(1.0F));

    private final BooleanSetting countAbsorption =
            this.addSetting("count-absorption", new BooleanSetting("Count Absorption", true));

    private final ModeSetting pulse =
            this.addSetting(
                    "pulse",
                    new ModeSetting("Pulse", "Heartbeat", "Heartbeat", "Breath", "Steady"));

    private final NumberSetting intensity =
            this.addSetting(
                    "intensity", new NumberSetting("Intensity", 0.7F, 0.2F, 1.0F).step(0.05F));

    private final NumberSetting reach =
            this.addSetting("reach", new NumberSetting("Reach", 0.16F, 0.06F, 0.35F).step(0.01F));

    private final BooleanSetting edgeLine =
            this.addSetting("edge-line", new BooleanSetting("Edge Line", true));

    private final ModeSetting colorMode =
            this.addSetting("color", new ModeSetting("Color", "Theme", "Theme", "Manual"));

    private final ColorSetting glowColor =
            this.addSetting("glow-color", new ColorSetting("Glow Color", -13338378));

    private final ColorSetting edgeColor =
            this.addSetting("edge-color", new ColorSetting("Edge Color", -5650177));

    private float level;

    private float danger;

    private float phase;

    private long lastFrame;

    public LowHealthWarningModule() {
        super(
                "low-health-warning",
                "Low Health Warning",
                "Flashes the screen edges when your health is low.",
                ModuleCategory.RENDER);
        this.glowColor.visibleWhen(this::manual);
        this.edgeColor.visibleWhen(() -> this.edgeLine.get() && this.manual());
    }

    @Override
    protected void onEnable() {
        this.level = 0.0F;
        this.danger = 0.0F;
        this.phase = 0.0F;
        this.lastFrame = 0L;
    }

    @Override
    protected void onDisable() {
        this.level = 0.0F;
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled();
    }

    @Override
    public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
        MinecraftClient client = MinecraftClient.getInstance();
        float h = Math.min(Math.max(ImGui.getIO().getDeltaTime(), 0.0F), 0.1F);
        long l = System.nanoTime();
        if (l - this.lastFrame > 250000000L) {
            this.level = 0.0F;
            this.phase = 0.0F;
        }
        this.lastFrame = l;
        ClientPlayerEntity player = client.player;
        boolean bl = player == null || player.isDead();
        float f2 = 0.0F;
        float warnBelow2;
        float g2;
        if (!bl
                && !client.options.hudHidden
                && !player.isSpectator()
                && !player.getAbilities().invulnerable
                && (g2 =
                                player.getHealth()
                                        + (this.countAbsorption.get()
                                                ? player.getAbsorptionAmount()
                                                : 0.0F))
                        < (warnBelow2 = this.warnBelow.get())) {
            f2 = 1.0F;
            this.danger = Math.max(0.0F, Math.min(1.0F, (warnBelow2 - g2) / warnBelow2));
        }
        float h2 = bl ? 12.0F : (f2 > this.level ? 5.0F : 2.6F);
        this.level = this.level + (f2 - this.level) * (1.0F - (float) Math.exp(-h2 * h));
        if (this.level < 0.003F && f2 == 0.0F) {
            this.level = 0.0F;
            this.phase = 0.0F;
        } else {
            float f3 = 64.0F + 86.0F * this.danger;
            if (this.pulse.is("Breath")) {
                f3 *= 0.5F;
            }
            this.phase = (this.phase + h * f3 / 60.0F) % 1.0F;
            float beat = this.beat();
            float g3 = this.level * this.level * (3.0F - 2.0F * this.level);
            float h3 = this.intensity.get() * g3 * (0.45F + 0.55F * this.danger);
            float f4 = this.reach.get() * Math.min(f, g) * (1.0F + 0.12F * beat);
            boolean manual = this.manual();
            int i = manual ? 0 : CosmeticAccent.argb();
            int abgr = GuiPalette.abgr(manual ? this.glowColor.argb() | 0xFF000000 : i);
            for (int k = 0; k < BAND_REACH.length; k++) {
                float g4 = h3 * BAND_WEIGHT[k] * (0.6F + 0.4F * beat);
                frame(imDrawList, f, g, f4 * BAND_REACH[k], withAlpha(abgr, g4));
            }
            if (this.edgeLine.get()) {
                float h4 = Math.max(2.0F, f4 * 0.07F);
                float f5 = this.intensity.get() * g3 * (0.2F + 0.55F * beat);
                int j = manual ? this.edgeColor.argb() | 0xFF000000 : ColorHelper.lerp(0.5F, i, -1);
                frame(imDrawList, f, g, h4, HudDraw.col(j, f5));
            }
        }
    }

    private float beat() {
        if (this.pulse.is("Steady")) {
            return 0.35F;
        } else {
            return this.pulse.is("Breath")
                    ? 0.5F - 0.5F * (float) Math.cos(6.2831855F * this.phase)
                    : Math.min(1.0F, bump(this.phase, 0.0F) + 0.62F * bump(this.phase, 0.2F));
        }
    }

    private static float bump(float f, float g) {
        float h = Math.abs(f - g);
        h = Math.min(h, 1.0F - h) / 0.065F;
        return (float) Math.exp(-h * h);
    }

    private boolean manual() {
        return this.colorMode.is("Manual");
    }

    private static int withAlpha(int i, float f) {
        int j = Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, f)));
        return i & 16777215 | j << 24;
    }

    private static void frame(ImDrawList imDrawList, float f, float g, float h, int i) {
        if (!(h < 1.0F) && i >>> 24 != 0) {
            int j = i & 16777215;
            imDrawList.addRectFilledMultiColor(0.0F, 0.0F, f, h, i, i, j, j);
            imDrawList.addRectFilledMultiColor(0.0F, g - h, f, g, j, j, i, i);
            imDrawList.addRectFilledMultiColor(0.0F, 0.0F, h, g, i, j, j, i);
            imDrawList.addRectFilledMultiColor(f - h, 0.0F, f, g, j, i, i, j);
        }
    }
}
