package net.efn.client.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public final class HandAuraModule extends ClientModule {

    public static volatile HandAuraModule active;

    private static final String COLOR_THEME = "Theme";

    private static final String COLOR_MANUAL = "Manual";

    private static final int WHITE = -1;

    private static final float SHEEN_LIFT = 0.55F;

    private static final long BREATH_NANOS = 3200000000L;

    private static final long SHEEN_NANOS = 4400000000L;

    private final ModeSetting colorMode =
            this.addSetting("color", new ModeSetting("Color", "Theme", "Theme", "Manual"));

    private final ColorSetting glowColor =
            this.addSetting("glow-color", new ColorSetting("Glow Color", -13338378));

    private final NumberSetting aura =
            this.addSetting("aura", new NumberSetting("Aura", 65.0F, 0.0F, 100.0F).step(5.0F));

    private final NumberSetting reach =
            this.addSetting("reach", new NumberSetting("Reach", 8.0F, 2.0F, 20.0F).step(1.0F));

    private final NumberSetting rim =
            this.addSetting("rim", new NumberSetting("Rim", 40.0F, 0.0F, 100.0F).step(5.0F));

    private final NumberSetting sheen =
            this.addSetting("sheen", new NumberSetting("Sheen", 25.0F, 0.0F, 100.0F).step(5.0F));

    private final ColorSetting sheenColor =
            this.addSetting("sheen-color", new ColorSetting("Sheen Color", -3153665));

    private final BooleanSetting breathe =
            this.addSetting("breathe", new BooleanSetting("Breathe", true));

    public HandAuraModule() {
        super(
                "hand-aura",
                "Hand Aura",
                "Adds a glowing outline to your hand and held item.",
                ModuleCategory.VISUALS);
        this.glowColor.visibleWhen(this::manual);
        this.reach.visibleWhen(() -> this.aura.get() > 0.0F);
        this.sheenColor.visibleWhen(() -> this.sheen.get() > 0.0F && this.manual());
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
        if (RenderSystem.isOnRenderThread()) {
            HandAuraPass.release();
        } else {
            MinecraftClient.getInstance().execute(HandAuraModule::releaseIfOff);
        }
    }

    private static void releaseIfOff() {
        if (active == null) {
            HandAuraPass.release();
        }
    }

    public static boolean beginCapture() {
        HandAuraModule module = active;
        return module != null && module.shows() && HandAuraPass.begin();
    }

    public static void endCapture(boolean bl) {
        if (HandAuraPass.end() && bl) {
            HandAuraModule module = active;
            float f =
                    Math.max(
                            1.0F,
                            MinecraftClient.getInstance().getFramebuffer().textureHeight / 1080.0F);
            if (module == null) {
                HandAuraPass.compose(-1, 0.0F, 2.0F, 0.0F, 2.0F * f, -1, 0.0F, 0.0F);
            } else {
                long l = System.nanoTime();
                float g = (float) (l % 4400000000L / 4.4E9);
                int glowArgb = module.glowArgb();
                HandAuraPass.compose(
                        glowArgb,
                        module.auraStrength(l),
                        module.reach.get() * f,
                        module.rim.get() / 100.0F,
                        2.0F * f,
                        module.sheenArgb(glowArgb),
                        module.sheen.get() / 100.0F,
                        g);
            }
        }
    }

    private boolean manual() {
        return this.colorMode.is("Manual");
    }

    private int glowArgb() {
        return this.manual() ? this.glowColor.argb() : CosmeticAccent.argb();
    }

    private int sheenArgb(int i) {
        return this.manual() ? this.sheenColor.argb() : ColorHelper.lerp(0.55F, i, -1);
    }

    private boolean shows() {
        return this.aura.get() > 0.0F || this.rim.get() > 0.0F;
    }

    private float auraStrength(long l) {
        float f = this.aura.get() / 100.0F;
        if (!this.breathe.get()) {
            return f;
        } else {
            double d = l % 3200000000L / 3.2E9;
            float g = (float) Math.sin(d * 3.141592653589793);
            return f * (0.6F + 0.4F * g * g);
        }
    }
}
