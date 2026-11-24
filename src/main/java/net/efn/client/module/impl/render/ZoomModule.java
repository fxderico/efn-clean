package net.efn.client.module.impl.render;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public final class ZoomModule extends ClientModule {

    private static final float MIN_ZOOM = 1.1F;

    private static final float MAX_ZOOM = 50.0F;

    private static final float SCROLL_STEP = 1.2F;

    private static final float FAST_RATE = 30.0F;

    private static final float SLOW_RATE = 5.0F;

    private static final float MAX_FRAME_SECONDS = 0.1F;

    private static volatile ZoomModule instance;

    private static float target = 1.0F;

    private static float current = 1.0F;

    private static long lastNanos;

    private final NumberSetting zoom =
            this.addSetting("zoom", new NumberSetting("Zoom", 4.0F, 1.5F, 20.0F).step(0.5F));

    private final NumberSetting smoothness =
            this.addSetting(
                    "smoothness", new NumberSetting("Smoothness", 60.0F, 0.0F, 100.0F).step(5.0F));

    private final BooleanSetting scroll =
            this.addSetting("scroll", new BooleanSetting("Scroll", true));

    private final BooleanSetting lowerSensitivity =
            this.addSetting("lower-sensitivity", new BooleanSetting("Lower Sensitivity", true));

    private final BooleanSetting cinematic =
            this.addSetting("cinematic", new BooleanSetting("Cinematic Camera", false));

    private boolean smoothBefore;

    private boolean smoothPushed;

    public ZoomModule() {
        super("zoom", "Zoom", "Hold the key to zoom in.", ModuleCategory.VISUALS);
        this.setKeybind(67);
        this.setHoldMode(true);
        instance = this;
    }

    @Override
    protected void onEnable() {
        target = this.zoom.get();
        if (this.cinematic.get()) {
            MinecraftClient client = MinecraftClient.getInstance();
            this.smoothBefore = client.options.smoothCameraEnabled;
            client.options.smoothCameraEnabled = true;
            this.smoothPushed = true;
        }
    }

    @Override
    protected void onDisable() {
        target = 1.0F;
        if (this.smoothPushed) {
            MinecraftClient.getInstance().options.smoothCameraEnabled = this.smoothBefore;
            this.smoothPushed = false;
        }
    }

    public static float fov(float f) {
        float advance = advance();
        return advance > 1.0001F ? f / advance : f;
    }

    public static double turnScale() {
        ZoomModule module = instance;
        return module != null && module.lowerSensitivity.get() && !(current <= 1.0001F)
                ? 1.0 / current
                : 1.0;
    }

    public static boolean scroll(double d) {
        ZoomModule module = instance;
        if (module != null && module.isEnabled() && module.scroll.get() && d != 0.0) {
            float f = target * (d > 0.0 ? 1.2F : 0.8333333F);
            target = Math.max(1.1F, Math.min(50.0F, f));
            return true;
        } else {
            return false;
        }
    }

    private static float advance() {
        long l = System.nanoTime();
        float f = lastNanos == 0L ? 0.0F : Math.min(0.1F, (float) (l - lastNanos) / 1.0E9F);
        lastNanos = l;
        ZoomModule module = instance;
        float g = module == null ? 0.0F : module.smoothness.get() / 100.0F;
        if (!(g <= 0.0F) && current != target) {
            float h = 30.0F + -25.0F * g;
            float f2 = 1.0F - (float) Math.exp(-f * h);
            double d = Math.log(current);
            if (Math.abs((current = (float) Math.exp(d + (Math.log(target) - d) * f2)) - target)
                    < 0.002F) {
                current = target;
            }
            return current;
        } else {
            current = target;
            return current;
        }
    }
}
