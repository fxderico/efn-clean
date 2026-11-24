package net.efn.client.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class CustomGlintModule extends ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    public static volatile CustomGlintModule active;

    private static final String COLOR_CUSTOM = "Custom";

    private static final String COLOR_ACCENT = "Accent";

    private final ModeSetting colorMode =
            this.addSetting("color-mode", new ModeSetting("Color", "Custom", "Custom", "Accent"));

    private final ColorSetting itemColor =
            this.addSetting("item-color", new ColorSetting("Item Color", -9524994));

    private final BooleanSetting separateArmor =
            this.addSetting("separate-armor", new BooleanSetting("Separate Armor", false));

    private final ColorSetting armorColor =
            this.addSetting("armor-color", new ColorSetting("Armor Color", -3697942));

    private final NumberSetting intensity =
            this.addSetting(
                    "intensity", new NumberSetting("Intensity", 1.0F, 0.05F, 1.0F).step(0.05F));

    private final NumberSetting speed =
            this.addSetting("speed", new NumberSetting("Speed", 1.0F, 0.1F, 4.0F).step(0.1F));

    private final NumberSetting patternSize =
            this.addSetting(
                    "pattern-size",
                    new NumberSetting("Pattern Size", 1.0F, 0.25F, 4.0F).step(0.25F));

    private final CustomGlintModule.GlintTexture items =
            new CustomGlintModule.GlintTexture(ItemRenderer.ITEM_ENCHANTMENT_GLINT);

    private final CustomGlintModule.GlintTexture armor =
            new CustomGlintModule.GlintTexture(ItemRenderer.ENTITY_ENCHANTMENT_GLINT);

    private double glintClock = 0.0 / 0.0;

    private long glintClockReal;

    public CustomGlintModule() {
        super(
                "custom-glint",
                "Custom Glint",
                "Recolours the enchantment glint and changes its speed and pattern size.",
                ModuleCategory.VISUALS);
        this.itemColor.visibleWhen(() -> this.colorMode.is("Custom"));
        this.separateArmor.visibleWhen(() -> this.colorMode.is("Custom"));
        this.armorColor.visibleWhen(() -> this.colorMode.is("Custom") && this.separateArmor.get());
    }

    @Override
    protected void onEnable() {
        this.glintClock = 0.0 / 0.0;
        active = this;
        this.on(TickEvent.TYPE, tickEvent -> this.refresh());
    }

    @Override
    protected void onDisable() {
        active = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) {
            this.restore();
        } else {
            client.execute(
                    () -> {
                        if (active == null) {
                            this.restore();
                        }
                    });
        }
    }

    public long glintMillis(long glintClockReal2) {
        this.glintClock =
                Double.isNaN(this.glintClock)
                        ? glintClockReal2
                        : (this.glintClock =
                                this.glintClock
                                        + (double)
                                                        Math.max(
                                                                0L,
                                                                glintClockReal2
                                                                        - this.glintClockReal)
                                                * this.speed.get());
        this.glintClockReal = glintClockReal2;
        return (long) this.glintClock;
    }

    public float uvScaleMultiplier() {
        return 1.0F / this.patternSize.get();
    }

    private void refresh() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null
                && client.getOverlay() == null
                && RenderSystem.tryGetDevice() != null) {
            int i =
                    this.colorMode.is("Accent")
                            ? CosmeticAccent.argb() & 16777215
                            : this.itemColor.rgb();
            int j =
                    this.colorMode.is("Custom") && this.separateArmor.get()
                            ? this.armorColor.rgb()
                            : i;
            float intensity2 = this.intensity.get();
            this.items.tint(client, i, intensity2);
            this.armor.tint(client, j, intensity2);
        }
    }

    private void restore() {
        MinecraftClient client = MinecraftClient.getInstance();
        this.items.restore(client);
        this.armor.restore(client);
    }

    @Environment(EnvType.CLIENT)
    private static final class GlintTexture {

        private final Identifier id;

        private int width;

        private int height;

        private int[] original;

        private int[] pattern;

        private GpuTexture written;

        private long writtenKey;

        private GpuTexture unusable;

        GlintTexture(Identifier id2) {
            this.id = id2;
        }

        void tint(MinecraftClient client, int i, float f) {
            GpuTexture gpuTexture = this.gpuTexture(client);
            if (gpuTexture != null && !gpuTexture.isClosed() && gpuTexture != this.unusable) {
                long l = (long) (i & 16777215) << 8 | Math.round(f * 255.0F);
                if (gpuTexture != this.written || l != this.writtenKey) {
                    if (gpuTexture != this.written) {
                        this.written = null;
                        if ((gpuTexture.usage() & 1) == 0
                                || !this.readShipped(client, gpuTexture)) {
                            this.unusable = gpuTexture;
                            return;
                        }
                    }
                    if (!this.upload(gpuTexture, this.tinted(i, f))) {
                        this.unusable = gpuTexture;
                    } else {
                        this.written = gpuTexture;
                        this.writtenKey = l;
                    }
                }
            }
        }

        void restore(MinecraftClient client) {
            GpuTexture gpuTexture = this.written != null ? this.gpuTexture(client) : null;
            if (gpuTexture != null
                    && gpuTexture == this.written
                    && !gpuTexture.isClosed()
                    && this.original != null) {
                this.upload(gpuTexture, this.original);
            }
            this.written = null;
            this.unusable = null;
            this.original = null;
            this.pattern = null;
        }

        private GpuTexture gpuTexture(MinecraftClient client) {
            try {
                AbstractTexture texture = client.getTextureManager().getTexture(this.id);
                return texture == null ? null : texture.getGlTexture();
            } catch (IllegalStateException exception) {
                return null;
            }
        }

        private boolean readShipped(MinecraftClient client, GpuTexture gpuTexture) {
            Optional resource = client.getResourceManager().getResource(this.id);
            if (resource.isEmpty()) {
                return false;
            } else {
                try {
                    boolean bl;
                    try (InputStream inputStream = ((Resource) resource.get()).getInputStream()) {
                        NativeImage nativeImage = NativeImage.read(inputStream);
                        int width2 = nativeImage.getWidth();
                        int height2 = nativeImage.getHeight();
                        if (width2 == gpuTexture.getWidth(0)
                                && height2 == gpuTexture.getHeight(0)) {
                            boolean bl2;
                            try {
                                int[] ints = new int[width2 * height2];
                                int[] ints2 = new int[width2 * height2];
                                int i = 0;
                                for (int m = 0; m < height2; m++) {
                                    for (int n = 0; n < width2; i++) {
                                        int colorArgb;
                                        ints[i] = colorArgb = nativeImage.getColorArgb(n, m);
                                        ints2[i] =
                                                Math.max(
                                                        ColorHelper.getRed(colorArgb),
                                                        Math.max(
                                                                ColorHelper.getGreen(colorArgb),
                                                                ColorHelper.getBlue(colorArgb)));
                                        n++;
                                    }
                                }
                                this.width = width2;
                                this.height = height2;
                                this.original = ints;
                                this.pattern = ints2;
                                bl2 = true;
                                if (nativeImage == null) {
                                    return bl2;
                                }
                            } catch (Throwable exception) {
                                if (nativeImage != null) {
                                    try {
                                        nativeImage.close();
                                    } catch (Throwable exception2) {
                                        exception.addSuppressed(exception2);
                                    }
                                }
                                throw exception;
                            }
                            nativeImage.close();
                            return bl2;
                        }
                        boolean bl3 = false;
                        if (nativeImage != null) {
                            nativeImage.close();
                        }
                        bl = bl3;
                    }
                    return bl;
                } catch (RuntimeException | IOException exception3) {
                    CustomGlintModule.LOGGER.warn(
                            "[EFN] Custom Glint could not read {}", this.id, exception3);
                    return false;
                }
            }
        }

        private int[] tinted(int i, float f) {
            float g = ColorHelper.getRed(i) * f / 255.0F;
            float h = ColorHelper.getGreen(i) * f / 255.0F;
            float f2 = ColorHelper.getBlue(i) * f / 255.0F;
            int[] ints = new int[this.pattern.length];
            for (int j = 0; j < ints.length; j++) {
                int k = this.pattern[j];
                ints[j] =
                        ColorHelper.getArgb(
                                ColorHelper.getAlpha(this.original[j]),
                                Math.round(k * g),
                                Math.round(k * h),
                                Math.round(k * f2));
            }
            return ints;
        }

        private boolean upload(GpuTexture gpuTexture, int[] ints) {
            // try-with-resources, re-sugared from the decompiler's expanded form
            try (NativeImage nativeImage = new NativeImage(this.width, this.height, false)) {
                int i = 0;
                for (int j = 0; j < this.height; j++) {
                    for (int k = 0; k < this.width; i++) {
                        nativeImage.setColorArgb(k, j, ints[i]);
                        k++;
                    }
                }
                RenderSystem.getDevice()
                        .createCommandEncoder()
                        .writeToTexture(gpuTexture, nativeImage);
                return true;
            } catch (RuntimeException exception) {
                CustomGlintModule.LOGGER.warn(
                        "[EFN] Custom Glint could not write {}", this.id, exception);
                return false;
            }
        }
    }
}
