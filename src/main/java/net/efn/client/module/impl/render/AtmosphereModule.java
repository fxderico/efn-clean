package net.efn.client.module.impl.render;

import com.example.client.mixin.AccessorAtmosphereClientLevel;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

import org.joml.Vector3fc;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class AtmosphereModule extends ClientModule {

    private static final String CUSTOM = "Custom";

    private static final String AURORA_ALWAYS = "Always";

    private static final String AURORA_COLD = "Cold Biomes";

    private static final String AURORA_OFF = "Off";

    private static final float MOON_SCALE = 1.4F;

    private static final AtmosphereModule.Theme[] THEMES =
            new AtmosphereModule.Theme[] {
                new AtmosphereModule.Theme(
                        "Borealis", 198414, 729648, 13073302, 3080090, 9063935, 13163263),
                new AtmosphereModule.Theme(
                        "Nebula", 459791, 2233916, 14053304, 16732120, 5991679, 15124735),
                new AtmosphereModule.Theme(
                        "EFN", 657157, 2890512, 15239498, 16757306, 16730670, 16769200),
                new AtmosphereModule.Theme(
                        "Frost", 264724, 1454154, 11057384, 8382719, 15135999, 14544639),
                new AtmosphereModule.Theme(
                        "Crimson", 721926, 3018260, 15227490, 16724821, 16751164, 16765640)
            };

    private final ModeSetting theme =
            this.addSetting(
                    "theme",
                    new ModeSetting(
                            "Theme",
                            "Borealis",
                            "Borealis",
                            "Nebula",
                            "EFN",
                            "Frost",
                            "Crimson",
                            "Custom"));

    private final ColorSetting nightColor =
            this.addSetting("night-color", new ColorSetting("Night Sky Color", -16047568));

    private final ColorSetting sunsetColor =
            this.addSetting("sunset-color", new ColorSetting("Sunset Color", -3703914));

    private final ColorSetting auroraColor =
            this.addSetting("aurora-color", new ColorSetting("Aurora Color", -13697126));

    private final ColorSetting auroraTopColor =
            this.addSetting("aurora-top-color", new ColorSetting("Aurora Top Color", -7713281));

    private final ModeSetting skyShader =
            this.addSetting(
                    "sky-shader",
                    new ModeSetting(
                            "Sky Shader",
                            "Off",
                            "Off",
                            "Space",
                            "Northern Lights",
                            "Ringed Planet",
                            "Black Hole"));

    private final NumberSetting shaderDaytime =
            this.addSetting(
                    "sky-shader-daytime",
                    new NumberSetting("Daytime Strength", 1.0F, 0.0F, 1.0F).step(0.05F));

    private final NumberSetting shaderBrightness =
            this.addSetting(
                    "sky-shader-brightness",
                    new NumberSetting("Shader Brightness", 1.0F, 0.2F, 2.0F).step(0.1F));

    private final NumberSetting shaderSpeed =
            this.addSetting(
                    "sky-shader-speed",
                    new NumberSetting("Shader Speed", 1.0F, 0.0F, 3.0F).step(0.1F));

    private final NumberSetting shaderStars =
            this.addSetting(
                    "sky-shader-stars",
                    new NumberSetting("Shader Stars", 1.0F, 0.0F, 2.0F).step(0.1F));

    private final NumberSetting shaderEffect =
            this.addSetting(
                    "sky-shader-effect",
                    new NumberSetting("Nebula / Aurora Strength", 1.0F, 0.0F, 2.0F).step(0.1F));

    private final NumberSetting shaderBand =
            this.addSetting(
                    "sky-shader-galaxy",
                    new NumberSetting("Galaxy Band", 1.0F, 0.0F, 2.0F).step(0.1F));

    private final BooleanSetting gradientSky =
            this.addSetting("gradient-sky", new BooleanSetting("Gradient Sky", true));

    private final BooleanSetting keepInRain =
            this.addSetting("keep-in-rain", new BooleanSetting("Keep In Rain", true));

    private final BooleanSetting celestialGlow =
            this.addSetting("sun-moon-glow", new BooleanSetting("Sun & Moon Glow", true));

    private final BooleanSetting stars =
            this.addSetting("stars", new BooleanSetting("Stars", true));

    private final NumberSetting starBrightness =
            this.addSetting(
                    "star-brightness",
                    new NumberSetting("Star Brightness", 1.3F, 0.5F, 2.0F).step(0.1F));

    private final BooleanSetting milkyWay =
            this.addSetting("milky-way", new BooleanSetting("Milky Way", true));

    private final NumberSetting milkyWayBrightness =
            this.addSetting(
                    "milky-way-brightness",
                    new NumberSetting("Milky Way Brightness", 1.0F, 0.2F, 2.0F).step(0.1F));

    private final BooleanSetting shootingStars =
            this.addSetting("shooting-stars", new BooleanSetting("Shooting Stars", true));

    private final ModeSetting aurora =
            this.addSetting(
                    "aurora", new ModeSetting("Aurora", "Always", "Always", "Cold Biomes", "Off"));

    private final NumberSetting auroraBrightness =
            this.addSetting(
                    "aurora-brightness",
                    new NumberSetting("Aurora Brightness", 1.0F, 0.2F, 2.0F).step(0.1F));

    private final NumberSetting auroraSpeed =
            this.addSetting(
                    "aurora-speed", new NumberSetting("Aurora Speed", 1.0F, 0.2F, 3.0F).step(0.1F));

    private final BooleanSetting cloudColors =
            this.addSetting("cloud-colors", new BooleanSetting("Cloud Colors", true));

    private final BooleanSetting endSky =
            this.addSetting("end-sky", new BooleanSetting("End Sky", true));

    private final BooleanSetting netherColors =
            this.addSetting("nether-colors", new BooleanSetting("Nether Colors", true));

    private final AtmosphereSky sky = new AtmosphereSky();

    private final AtmosphereStars starField = new AtmosphereStars();

    private final AtmosphereAurora auroraMesh = new AtmosphereAurora();

    private final AtmosphereShaderSky shaderSky = new AtmosphereShaderSky();

    private final MatrixStack world = new MatrixStack();

    private float night;

    private float rainBrightness = 1.0F;

    private float auroraHere;

    private long lastFrame;

    private boolean moonScaled;

    private boolean shaderRendered;

    public AtmosphereModule() {
        super(
                "atmosphere",
                "Atmosphere",
                "A custom sky with stars and an aurora.",
                ModuleCategory.VISUALS);
        this.nightColor.visibleWhen(() -> this.theme.is("Custom"));
        this.sunsetColor.visibleWhen(() -> this.theme.is("Custom"));
        this.auroraColor.visibleWhen(() -> this.theme.is("Custom"));
        this.auroraTopColor.visibleWhen(() -> this.theme.is("Custom"));
        this.shaderDaytime.visibleWhen(this::shaderOn);
        this.shaderBrightness.visibleWhen(this::shaderOn);
        this.shaderSpeed.visibleWhen(this::shaderOn);
        this.shaderStars.visibleWhen(this::shaderOn);
        this.shaderEffect.visibleWhen(this::shaderOn);
        this.shaderBand.visibleWhen(this::shaderOn);
        this.stars.visibleWhen(this::ownSky);
        this.starBrightness.visibleWhen(() -> this.stars.get() && this.ownSky());
        this.milkyWay.visibleWhen(this::ownSky);
        this.milkyWayBrightness.visibleWhen(() -> this.milkyWay.get() && this.ownSky());
        this.aurora.visibleWhen(this::ownSky);
        this.auroraBrightness.visibleWhen(() -> !this.aurora.is("Off") && this.ownSky());
        this.auroraSpeed.visibleWhen(() -> !this.aurora.is("Off") && this.ownSky());
    }

    public static AtmosphereModule get() {
        AtmosphereModule module = ModuleRegistry.get(AtmosphereModule.class);
        return module != null && module.isEnabled() ? module : null;
    }

    @Override
    protected void onDisable() {
        this.sky.close();
        this.starField.close();
        this.auroraMesh.close();
        this.shaderSky.close();
        this.shaderRendered = false;
        this.lastFrame = 0L;
        this.auroraHere = 0.0F;
    }

    public void extract(ClientWorld world2, float f, SkyRenderState skyRenderState) {
        this.shaderRendered = false;
        this.night = MathHelper.clamp(skyRenderState.starBrightness * 2.0F, 0.0F, 1.0F);
        float g = 1.0F - skyRenderState.rainGradient;
        float thunderGradient = world2.getThunderGradient(f);
        if (this.keepInRain.get()) {
            this.night = Math.max(this.night, clearNight(skyRenderState.sunAngle) * g);
            skyRenderState.starBrightness =
                    Math.max(skyRenderState.starBrightness, this.night * 0.5F);
            skyRenderState.rainGradient = 1.0F;
            g = 0.0F;
            thunderGradient = 0.0F;
        }
        this.rainBrightness = 1.0F - g;
        AtmosphereModule.Theme theme2 = this.currentTheme();
        int efn$atmosphereSkyFlash =
                ((AccessorAtmosphereClientLevel) world2).efn$atmosphereSkyFlash();
        this.sky.update(
                skyRenderState.sunAngle,
                skyRenderState.moonAngle,
                g,
                thunderGradient,
                efn$atmosphereSkyFlash > 0
                        ? Math.min(1.0F, (efn$atmosphereSkyFlash - f) / 2.0F)
                        : 0.0F,
                theme2.nightZenith,
                theme2.nightHorizon,
                theme2.twilight);
    }

    public boolean keepsSkyInRain() {
        return this.keepInRain.get();
    }

    private static float clearNight(float f) {
        float g = MathHelper.clamp(0.75F - 2.0F * MathHelper.cos(f), 0.0F, 1.0F);
        return g * g;
    }

    public boolean skyDisc() {
        if (this.gradientSky.get() && this.sky.ready()) {
            this.sky.renderDome(this.world);
            this.renderShaderSky();
            return true;
        } else {
            return false;
        }
    }

    public void afterSkyDisc() {
        this.renderShaderSky();
    }

    private boolean ownSky() {
        return !this.shaderOn();
    }

    public int lyricGlow() {
        return this.currentTheme().auroraHigh;
    }

    private void renderShaderSky() {
        if (this.shaderOn()) {
            AtmosphereModule.Theme theme2 = this.currentTheme();
            this.shaderRendered =
                    this.shaderSky.render(
                            this.skyShader.get(),
                            this.shaderVisibility(),
                            this.shaderBrightness.get(),
                            this.shaderSpeed.get(),
                            this.shaderStars.get(),
                            this.shaderEffect.get(),
                            this.shaderBand.get(),
                            theme2.auroraLow,
                            theme2.auroraHigh,
                            theme2.milkyWay,
                            theme2.nightZenith,
                            theme2.nightHorizon);
        }
    }

    private boolean shaderOn() {
        return !this.skyShader.is("Off");
    }

    private boolean shaderAurora() {
        return this.skyShader.is("Northern Lights");
    }

    private float shaderVisibility() {
        return MathHelper.lerp(this.night, this.shaderDaytime.get(), 1.0F)
                * (0.4F + 0.6F * this.rainBrightness);
    }

    public boolean replacesSunrise() {
        return this.gradientSky.get() && this.sky.ready();
    }

    public boolean stars(MatrixStack matrices) {
        if (this.shaderRendered) {
            return true;
        } else {
            float f = this.night * this.rainBrightness;
            if (this.milkyWay.get()) {
                this.starField.renderBand(
                        matrices, f * this.milkyWayBrightness.get(), this.currentTheme().milkyWay);
            }
            if (!this.stars.get()) {
                return false;
            } else {
                this.starField.renderStars(matrices, f, this.starBrightness.get());
                return true;
            }
        }
    }

    public void beforeMoon(MatrixStack matrices) {
        this.moonScaled = this.celestialGlow.get();
        if (this.moonScaled) {
            matrices.push();
            matrices.scale(1.4F, 1.0F, 1.4F);
        }
    }

    public void afterMoon(MatrixStack matrices) {
        if (this.moonScaled) {
            matrices.pop();
        }
        this.moonScaled = false;
    }

    public void afterCelestials() {
        long l = System.nanoTime();
        float f =
                this.lastFrame == 0L ? 0.0F : Math.min((float) (l - this.lastFrame) / 1.0E9F, 0.1F);
        this.lastFrame = l;
        if (this.sky.ready()) {
            if (this.celestialGlow.get()) {
                this.sky.renderHalos(this.world, this.rainBrightness, this.night);
            }
            if (this.shootingStars.get()) {
                this.sky.renderMeteors(this.world, this.night, this.rainBrightness);
            }
            this.auroraHere =
                    this.auroraHere
                            + (this.auroraTarget() - this.auroraHere) * Math.min(1.0F, f * 0.5F);
            if (this.auroraHere > 0.01F && this.night > 0.01F && !this.shaderRendered) {
                AtmosphereModule.Theme theme2 = this.currentTheme();
                float g = (float) (l % 3600000000000L) / 1.0E9F * this.auroraSpeed.get();
                float h =
                        this.auroraHere
                                * this.night
                                * (0.35F + 0.65F * this.rainBrightness)
                                * this.auroraBrightness.get();
                this.auroraMesh.render(this.world, h, g, theme2.auroraLow, theme2.auroraHigh);
            }
        }
    }

    private float auroraTarget() {
        if (this.aurora.is("Off")) {
            return 0.0F;
        } else if (this.aurora.is("Always")) {
            return 1.0F;
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null) {
                return 0.0F;
            } else {
                BlockPos pos = BlockPos.ofFloored(client.gameRenderer.getCamera().getCameraPos());
                return client.world.getBiome(pos).value().isCold(pos, client.world.getSeaLevel())
                        ? 1.0F
                        : 0.0F;
            }
        }
    }

    public void fog(Camera camera, ClientWorld world2, Vector4f vector4f) {
        if (world2.getRegistryKey() == World.OVERWORLD) {
            if (this.gradientSky.get() && this.sky.ready()) {
                Vector3fc horizontalPlane = camera.getHorizontalPlane();
                int horizonToward =
                        this.sky.horizonToward(horizontalPlane.x(), horizontalPlane.z());
                vector4f.x = (horizonToward >> 16 & 0xFF) / 255.0F;
                vector4f.y = (horizonToward >> 8 & 0xFF) / 255.0F;
                vector4f.z = (horizonToward & 0xFF) / 255.0F;
            }
            if (this.shaderOn()) {
                this.shaderFog(vector4f);
            }
        } else if (world2.getRegistryKey() == World.NETHER && this.netherColors.get()) {
            float f = 0.2126F * vector4f.x + 0.7152F * vector4f.y + 0.0722F * vector4f.z;
            vector4f.x = MathHelper.clamp((f + (vector4f.x - f) * 1.45F) * 1.12F, 0.0F, 1.0F);
            vector4f.y = MathHelper.clamp((f + (vector4f.y - f) * 1.45F) * 1.12F, 0.0F, 1.0F);
            vector4f.z = MathHelper.clamp((f + (vector4f.z - f) * 1.45F) * 1.12F, 0.0F, 1.0F);
        }
    }

    private void shaderFog(Vector4f vector4f) {
        int nightHorizon2 = this.currentTheme().nightHorizon;
        float f = this.shaderAurora() ? 0.85F : 0.3F;
        float g = this.shaderVisibility() * 0.85F;
        vector4f.x = MathHelper.lerp(g, vector4f.x, (nightHorizon2 >> 16 & 0xFF) / 255.0F * f);
        vector4f.y = MathHelper.lerp(g, vector4f.y, (nightHorizon2 >> 8 & 0xFF) / 255.0F * f);
        vector4f.z = MathHelper.lerp(g, vector4f.z, (nightHorizon2 & 0xFF) / 255.0F * f);
    }

    public int clouds(int i) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.cloudColors.get()
                && this.sky.ready()
                && client.world != null
                && client.world.getRegistryKey() == World.OVERWORLD) {
            float golden = this.sky.golden();
            float nightness = this.sky.nightness();
            int j =
                    ColorHelper.lerp(
                            golden,
                            ColorHelper.lerp(nightness, 16777215, this.currentTheme().nightHorizon),
                            this.sky.twilightGlow());
            float f = Math.max(golden * 0.75F, nightness * 0.55F);
            return keepBrightness(i, j, f);
        } else {
            return i;
        }
    }

    public boolean endSky() {
        if (!this.endSky.get()) {
            return false;
        } else {
            long l = System.nanoTime();
            this.sky.renderEndDome(this.world);
            MatrixStack matrices = new MatrixStack();
            matrices.multiply(
                    RotationAxis.POSITIVE_Y.rotationDegrees(
                            (float) (l % 1200000000000L) / 1.0E9F * 0.3F));
            this.starField.renderNebulae(matrices, 1.0F);
            this.starField.renderStars(matrices, 1.0F, 1.2F);
            return true;
        }
    }

    private AtmosphereModule.Theme currentTheme() {
        for (AtmosphereModule.Theme theme2 : THEMES) {
            if (this.theme.is(theme2.name)) {
                return theme2;
            }
        }
        int rgb = this.nightColor.rgb();
        return new AtmosphereModule.Theme(
                "Custom",
                AtmosphereSky.scale(rgb, 0.3F),
                rgb,
                this.sunsetColor.rgb(),
                this.auroraColor.rgb(),
                this.auroraTopColor.rgb(),
                15263999);
    }

    private static int keepBrightness(int i, int j, float f) {
        if (f <= 0.0F) {
            return i;
        } else {
            float g = (i >> 16 & 0xFF) / 255.0F;
            float h = (i >> 8 & 0xFF) / 255.0F;
            float f2 = (i & 0xFF) / 255.0F;
            float g2 = (j >> 16 & 0xFF) / 255.0F;
            float h2 = (j >> 8 & 0xFF) / 255.0F;
            float f3 = (j & 0xFF) / 255.0F;
            float g3 = luminance(g, h, f2) / Math.max(luminance(g2, h2, f3), 0.001F);
            g = MathHelper.lerp(f, g, Math.min(1.0F, g2 * g3));
            h = MathHelper.lerp(f, h, Math.min(1.0F, h2 * g3));
            f2 = MathHelper.lerp(f, f2, Math.min(1.0F, f3 * g3));
            return i & 0xFF000000 | channel(g) << 16 | channel(h) << 8 | channel(f2);
        }
    }

    private static float luminance(float f, float g, float h) {
        return 0.2126F * f + 0.7152F * g + 0.0722F * h;
    }

    private static int channel(float f) {
        return MathHelper.clamp(Math.round(f * 255.0F), 0, 255);
    }

    @Environment(EnvType.CLIENT)
    private record Theme(
            String name,
            int nightZenith,
            int nightHorizon,
            int twilight,
            int auroraLow,
            int auroraHigh,
            int milkyWay) {}
}
