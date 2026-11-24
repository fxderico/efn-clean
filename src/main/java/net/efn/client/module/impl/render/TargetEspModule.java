package net.efn.client.module.impl.render;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.MobilePerformance;
import net.efn.client.util.combat.CombatTarget;
import net.efn.client.util.glyph.GlyphCanvas;
import net.efn.client.util.glyph.GlyphShapes;
import net.efn.client.util.glyph.GlyphTint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public final class TargetEspModule extends ClientModule {

    private static final String STYLE_BOLT = "Bolt Circle";

    private static final String STYLE_HALO = "Halo";

    private static final String STYLE_VORTEX = "Vortex";

    private static final String STYLE_CAPSTONE = "Capstone";

    private static final float APPEAR_SECONDS = 0.3F;

    private static final float VANISH_SECONDS = 0.38F;

    private static final float HURT_TICKS = 10.0F;

    private static final double MAX_DISTANCE_SQR = 16384.0;

    private static final int GLYPH_MARKS = 6;

    private static final int PLATE_DASHES = 12;

    private static final int VORTEX_GRAINS = 40;

    private static final int VORTEX_POSTS = 3;

    private static final float VORTEX_FALL = 0.48F;

    private static final float VORTEX_SPIN = 0.8F;

    private static final float VORTEX_SWIRL = 3.1F;

    private static final float VORTEX_RIPPLE = 0.96F;

    private static final float[] GRAIN_SEEDS = scatter(120, 1515036955L);

    private static final double BLINK_PERIOD = 4.6;

    private static final double BLINK_LENGTH = 0.22;

    private final ModeSetting style =
            this.addSetting(
                    "style",
                    new ModeSetting(
                            "Style", "Bolt Circle", "Bolt Circle", "Halo", "Vortex", "Capstone"));

    private final ModeSetting colorMode =
            this.addSetting("color-mode", new ModeSetting("Color", "Theme", "Theme", "Manual"));

    private final ColorSetting mainColor =
            this.addSetting("main-color", new ColorSetting("Main Color", -12614409));

    private final ColorSetting secondColor =
            this.addSetting("second-color", new ColorSetting("Second Color", -15189606));

    private final ColorSetting hitColor =
            this.addSetting("hit-color", new ColorSetting("Hit Color", -2233345));

    private final NumberSetting scale =
            this.addSetting("scale", new NumberSetting("Scale", 1.0F, 0.6F, 1.8F).step(0.1F));

    private final NumberSetting tempo =
            this.addSetting("tempo", new NumberSetting("Tempo", 1.0F, 0.3F, 2.4F).step(0.1F));

    private final NumberSetting opacity =
            this.addSetting("opacity", new NumberSetting("Opacity", 0.9F, 0.2F, 1.0F).step(0.05F));

    private final NumberSetting memory =
            this.addSetting("memory", new NumberSetting("Memory", 4.0F, 0.5F, 15.0F).step(0.5F));

    private final BooleanSetting crosshair =
            this.addSetting("crosshair", new BooleanSetting("Crosshair Pick", true));

    private final BooleanSetting playersOnly =
            this.addSetting("players-only", new BooleanSetting("Players Only", false));

    private final BooleanSetting hurtFlash =
            this.addSetting("hurt-flash", new BooleanSetting("Hit Flash", true));

    private final BooleanSetting throughWalls =
            this.addSetting("through-walls", new BooleanSetting("Through Walls", false));

    private final GlyphCanvas canvas = new GlyphCanvas();

    private final GlyphTint palette = new GlyphTint();

    private final double[] capX = new double[4];

    private final double[] capZ = new double[4];

    private TargetEspModule.Mark current = new TargetEspModule.Mark();

    private TargetEspModule.Mark previous = new TargetEspModule.Mark();

    private ClientWorld boundLevel;

    private long lastFrame;

    private double phase;

    private int gold;

    private int deep;

    private int light;

    private int hit;

    public TargetEspModule() {
        super(
                "target-esp",
                "Target ESP",
                "Draws a mark on your combat target.",
                ModuleCategory.VISUALS);
        this.mainColor.visibleWhen(() -> this.colorMode.is("Manual"));
        this.secondColor.visibleWhen(() -> this.colorMode.is("Manual"));
        this.hitColor.visibleWhen(() -> this.colorMode.is("Manual") && this.hurtFlash.get());
    }

    @Override
    protected void onEnable() {
        this.reset();
        this.on(TickEvent.TYPE, Stage.POST, this::onTick);
        this.on(Render3DEvent.TYPE, this::onRender);
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    private void reset() {
        this.current.assign(null);
        this.previous.assign(null);
        this.boundLevel = null;
        this.lastFrame = 0L;
    }

    private void onTick(TickEvent event) {
        if (this.boundLevel != null && MinecraftClient.getInstance().world != this.boundLevel) {
            this.reset();
        }
    }

    private void onRender(Render3DEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world != null && client.player != null) {
            if (world != this.boundLevel) {
                this.reset();
                this.boundLevel = world;
            }
            long l = System.nanoTime();
            float f =
                    this.lastFrame == 0L
                            ? 0.0F
                            : Math.min(0.1F, (float) (l - this.lastFrame) / 1.0E9F);
            this.lastFrame = l;
            this.phase = this.phase + f * this.tempo.get();
            this.retarget(this.pick(client), event.partialTick, world);
            this.current.presence =
                    this.current.entity != null
                            ? Math.min(1.0F, this.current.presence + f / 0.3F)
                            : 0.0F;
            this.previous.presence = Math.max(0.0F, this.previous.presence - f / 0.38F);
            if (this.previous.presence <= 0.0F) {
                this.previous.entity = null;
            }
            this.current.follow(event.partialTick, world);
            this.previous.follow(event.partialTick, world);
            if (this.current.entity != null || this.previous.entity != null) {
                this.palette.resolve(
                        this.colorMode.get(),
                        CosmeticAccent.argb(),
                        this.mainColor.argb(),
                        this.secondColor.argb());
                this.hit =
                        this.colorMode.is("Manual") ? this.hitColor.argb() : this.palette.flash();
                this.canvas.begin(client.gameRenderer.getCamera());
                if (this.previous.entity != null) {
                    this.draw(this.previous, true);
                }
                if (this.current.entity != null) {
                    this.draw(this.current, false);
                }
                this.canvas.submit(this.throughWalls.get());
            }
        } else {
            if (this.boundLevel != null) {
                this.reset();
            }
        }
    }

    private void retarget(LivingEntity entity2, float f, ClientWorld world) {
        if (entity2 != this.current.entity) {
            if (entity2 != null && entity2 == this.previous.entity) {
                TargetEspModule.Mark current2 = this.current;
                this.current = this.previous;
                this.previous = current2;
            } else {
                if (this.current.entity != null && this.current.presence > 0.02F) {
                    TargetEspModule.Mark previous2 = this.previous;
                    this.previous = this.current;
                    this.current = previous2;
                }
                this.current.assign(entity2);
                this.current.follow(f, world);
            }
        }
    }

    private LivingEntity pick(MinecraftClient client) {
        long l = (long) (this.memory.get() * 1000.0F);
        LivingEntity entity2 =
                this.crosshair.get() ? CombatTarget.current(l) : CombatTarget.lastHit(l);
        if (entity2 == null
                || entity2 == client.player
                || !entity2.isAlive()
                || entity2.isSpectator()) {
            return null;
        } else if (entity2 instanceof ArmorStandEntity) {
            return null;
        } else {
            return this.playersOnly.get() && !(entity2 instanceof PlayerEntity) ? null : entity2;
        }
    }

    private void draw(TargetEspModule.Mark mark, boolean bl) {
        if (!(this.canvas.distanceSqr(mark.x, mark.y, mark.z) > 16384.0)) {
            float presence2 = mark.presence;
            float f =
                    bl
                            ? presence2 * presence2
                            : 1.0F - (1.0F - presence2) * (1.0F - presence2) * (1.0F - presence2);
            float g = bl ? 1.0F + 0.3F * (1.0F - presence2) : 0.55F + 0.45F * unfurl(presence2);
            float h = this.hurtFlash.get() ? mark.hurt : 0.0F;
            float f2 = this.opacity.get() * f;
            if (!(f2 < 0.01F)) {
                this.gold = GlyphTint.mix(this.palette.primary(), this.hit, h * 0.8F);
                this.deep = GlyphTint.mix(this.palette.secondary(), this.hit, h * 0.45F);
                this.light = GlyphTint.mix(this.palette.highlight(), this.hit, h * 0.6F);
                float g2 = this.scale.get() * g * (1.0F + 0.1F * h);
                float h2 = (mark.width * 0.5F + 0.4F) * g2;
                String style2 = this.style.get();
                switch (style2) {
                    case "Halo":
                        this.haloMark(mark, g2, f2);
                        break;
                    case "Vortex":
                        this.vortexMark(mark, g2, h2, f2);
                        break;
                    case "Capstone":
                        this.capstone(mark, g2, f2);
                        break;
                    default:
                        this.boltCircle(mark, g2, h2, f2);
                }
            }
        }
    }

    private void boltCircle(TargetEspModule.Mark mark, float f, float g, float h) {
        double d = mark.y + 0.02;
        this.ring(
                mark.x, d, mark.z, g * 0.62F, g * 0.86F, 40, GlyphTint.alpha(this.deep, h * 0.1F));
        this.ring(
                mark.x,
                d,
                mark.z,
                g * 0.86F,
                g * 0.965F,
                40,
                GlyphTint.alpha(this.gold, h * 0.22F));
        this.ring(
                mark.x,
                d,
                mark.z,
                g * 0.965F,
                g * 1.035F,
                48,
                GlyphTint.alpha(this.light, h * 0.9F));
        this.ring(
                mark.x, d, mark.z, g * 1.035F, g * 1.14F, 48, GlyphTint.alpha(this.gold, h * 0.2F));
        float f2 = 0.5235988F;
        float g2 = (float) (-this.phase * 0.7 % 6.2831854820251465);
        for (int i = 0; i < 12; i++) {
            float h2 = g2 + i * f2;
            this.canvas.band(
                    mark.x,
                    d,
                    mark.z,
                    g * 0.74F,
                    g * 0.8F,
                    h2,
                    h2 + f2 * 0.62F,
                    3,
                    GlyphTint.alpha(this.deep, h * 0.85F));
        }
        float f3 = (float) (this.phase * 0.9 % 6.2831854820251465);
        float g3 = 0.12F * f;
        for (int j = 0; j < 6; j++) {
            float h3 = f3 + j * 6.2831855F / 6.0F;
            double e = mark.x + MathHelper.cos(h3) * g;
            double d2 = mark.z + MathHelper.sin(h3) * g;
            double e2 = mark.y + 0.2 * f + 0.045 * f * MathHelper.sin(this.phase * 2.6 + j * 1.3);
            boolean bl = (j & 1) == 0;
            this.canvas.sprite(
                    GlyphShapes.OCTAGON,
                    e,
                    e2,
                    d2,
                    g3 * 2.1F,
                    0.0F,
                    GlyphTint.alpha(bl ? this.gold : this.deep, h * 0.14F));
            if (bl) {
                this.canvas.sprite(
                        GlyphShapes.BOLT, e, e2, d2, g3, 0.0F, GlyphTint.alpha(this.gold, h));
            } else {
                this.canvas.sprite(
                        GlyphShapes.GEM, e, e2, d2, g3 * 0.9F, 0.0F, GlyphTint.alpha(this.deep, h));
                this.canvas.sprite(
                        GlyphShapes.DISK,
                        e,
                        e2,
                        d2,
                        g3 * 0.3F,
                        g3 * 0.3F,
                        0.0F,
                        0.0F,
                        4.2F,
                        GlyphTint.alpha(this.light, h));
            }
        }
    }

    private void haloMark(TargetEspModule.Mark mark, float f, float g) {
        double d = mark.y + mark.height;
        float h = (mark.width * 0.5F + 0.2F) * f;
        double e = d + 0.08;
        this.ring(mark.x, e, mark.z, h * 0.9F, h, 32, GlyphTint.alpha(this.gold, g * 0.85F));
        this.ring(mark.x, e, mark.z, h, h * 1.18F, 32, GlyphTint.alpha(this.gold, g * 0.18F));
        float f2 = (float) (this.phase * 1.2 % 6.2831854820251465);
        for (int i = 0; i < 8; i++) {
            float g2 = f2 + i * 6.2831855F / 8.0F;
            this.canvas.band(
                    mark.x,
                    e,
                    mark.z,
                    h * 1.22F,
                    h * 1.44F,
                    g2,
                    g2 + 0.13F,
                    1,
                    GlyphTint.alpha(this.deep, g * 0.8F));
        }
        float h2 = 0.26F * f;
        double d2 = d + 0.52 * f + 0.05 * f * MathHelper.sin(this.phase * 1.8);
        float blink = this.blink();
        this.canvas.sprite(
                GlyphShapes.OCTAGON,
                mark.x,
                d2,
                mark.z,
                h2 * 1.95F,
                0.0F,
                GlyphTint.alpha(this.deep, g * 0.16F));
        this.canvas.sprite(
                GlyphShapes.DISK,
                mark.x,
                d2,
                mark.z,
                h2 * 1.3F,
                0.0F,
                GlyphTint.alpha(GlyphTint.mix(this.deep, -16777216, 0.45F), g * 0.6F));
        this.canvas.sprite(
                GlyphShapes.HALO,
                mark.x,
                d2,
                mark.z,
                h2 * 1.36F,
                0.0F,
                GlyphTint.alpha(this.gold, g));
        this.canvas.sprite(
                GlyphShapes.DIAMOND,
                mark.x,
                d2,
                mark.z,
                h2 * 0.85F,
                h2 * 0.85F * (0.7F + 0.3F * blink),
                0.0F,
                GlyphTint.alpha(this.gold, g));
        this.canvas.sprite(
                GlyphShapes.SPARK,
                mark.x,
                d2,
                mark.z,
                h2 * 0.6F,
                h2 * 0.6F,
                0.0F,
                GlyphTint.alpha(this.light, g));
    }

    private void vortexMark(TargetEspModule.Mark mark, float f, float g, float h) {
        double d = mark.y + 0.03;
        double e = mark.y + mark.height + 0.3 * f;
        double d2 = e - d;
        double e2 = mark.y + mark.height * 0.5;
        float f2 = MathHelper.clamp((float) ((e2 - d) / d2), 0.15F, 0.85F);
        float g2 = (mark.width * 0.5F + 0.12F) * f;
        float h2 = (float) (this.phase * 0.47999998927116394 % 1.0);
        float f3 = (float) (this.phase * 0.800000011920929 % 6.2831854820251465);
        float g3 = (float) (this.phase * 4.0 % 6.2831854820251465);
        this.ring(mark.x, e, mark.z, g * 0.9F, g * 0.98F, 40, GlyphTint.alpha(this.gold, h * 0.8F));
        this.ring(
                mark.x, e, mark.z, g * 0.98F, g * 1.1F, 40, GlyphTint.alpha(this.gold, h * 0.16F));
        for (int i = 0; i < 6; i++) {
            float h3 = -f3 * 1.5F + i * 6.2831855F / 6.0F;
            this.canvas.band(
                    mark.x,
                    e,
                    mark.z,
                    g * 0.76F,
                    g * 0.84F,
                    h3,
                    h3 + 0.5F,
                    2,
                    GlyphTint.alpha(this.deep, h * 0.7F));
        }
        this.ring(mark.x, e2, mark.z, g2 * 0.9F, g2, 24, GlyphTint.alpha(this.light, h * 0.3F));
        this.ring(mark.x, d, mark.z, g * 0.92F, g, 40, GlyphTint.alpha(this.deep, h * 0.6F));
        this.ring(mark.x, d, mark.z, 0.0F, g * 0.92F, 32, GlyphTint.alpha(this.gold, h * 0.08F));
        float f4 = (float) (this.phase * 0.9599999785423279 % 1.0);
        float g4 = g2 + (g * 1.12F - g2) * f4;
        this.ring(
                mark.x,
                d + 0.005,
                mark.z,
                g4 * 0.93F,
                g4,
                36,
                GlyphTint.alpha(this.gold, h * 0.55F * (1.0F - f4)));
        int alpha = GlyphTint.alpha(this.gold, h * 0.35F);
        for (int k = 0; k < 3; k++) {
            float h4 = f3 * 0.5F + k * 6.2831855F / 3.0F;
            double d3 = mark.x + MathHelper.cos(h4) * g * 1.04F;
            double e3 = mark.z + MathHelper.sin(h4) * g * 1.04F;
            this.canvas.line(d3, d, e3, d3, e, e3, alpha, 1.4F);
        }
        int particles = MobilePerformance.particles(40, 20);
        for (int n = 0; n < particles; n++) {
            float f5 = GRAIN_SEEDS[n * 3];
            float g5 = GRAIN_SEEDS[n * 3 + 1];
            float h5 = GRAIN_SEEDS[n * 3 + 2];
            float fractionalPart = MathHelper.fractionalPart(h2 + f5);
            float f6 = fractionalPart * (0.4F + 0.6F * fractionalPart);
            float g6 = 1.0F - f6;
            float h6 = g6 >= f2 ? (g6 - f2) / (1.0F - f2) : (f2 - g6) / f2;
            float f7 = h6 * (2.0F - h6);
            float g7 = g2 + (g - g2) * f7;
            float h7 = g5 * 6.2831855F + f3 + f6 * 3.1F;
            double d4 = mark.x + MathHelper.cos(h7) * g7;
            double e4 = mark.z + MathHelper.sin(h7) * g7;
            double d5 = e - f6 * d2;
            float f8 =
                    h
                            * Math.min(1.0F, fractionalPart * 10.0F)
                            * Math.min(1.0F, (1.0F - fractionalPart) * 8.0F);
            float g8 = 0.042F * f * (0.7F + 0.6F * h5);
            int j = GlyphTint.mix(this.light, this.gold, f7);
            if (n % 5 == 0) {
                j = GlyphTint.mix(j, this.deep, 0.6F);
            }
            if (h6 < 0.3F) {
                this.canvas.sprite(
                        GlyphShapes.OCTAGON,
                        d4,
                        d5,
                        e4,
                        g8 * 2.6F,
                        0.0F,
                        GlyphTint.alpha(this.light, f8 * 0.6F * (0.3F - h6)));
            }
            this.canvas.sprite(
                    GlyphShapes.GRAIN,
                    d4,
                    d5,
                    e4,
                    g8,
                    g3 + h5 * 6.2831855F,
                    GlyphTint.alpha(j, f8));
        }
    }

    private void capstone(TargetEspModule.Mark mark, float f, float g) {
        double d = mark.y + mark.height;
        double e = d + 0.28 * f + 0.06 * f * MathHelper.sin(this.phase * 2.1);
        float h = 0.34F * f;
        float f2 = 0.17F * f * MathHelper.SQUARE_ROOT_OF_TWO;
        double d2 = e + h;
        double e2 = e + h * 0.6;
        float g2 = (float) (this.phase * 1.3 % 6.2831854820251465);
        for (int i = 0; i < 4; i++) {
            float h2 = g2 + i * 1.5707964F;
            this.capX[i] = mark.x + MathHelper.cos(h2) * f2;
            this.capZ[i] = mark.z + MathHelper.sin(h2) * f2;
        }
        this.canvas.sprite(
                GlyphShapes.OCTAGON,
                mark.x,
                e2,
                mark.z,
                h * 1.7F,
                0.0F,
                GlyphTint.alpha(this.gold, g * 0.12F));
        for (int j = 0; j < 4; j++) {
            int k = j + 1 & 3;
            int m = (j & 1) == 0 ? this.gold : GlyphTint.mix(this.gold, this.deep, 0.35F);
            this.canvas.outwardTriangle(
                    this.capX[j],
                    d2,
                    this.capZ[j],
                    this.capX[k],
                    d2,
                    this.capZ[k],
                    mark.x,
                    e,
                    mark.z,
                    mark.x,
                    e2,
                    mark.z,
                    GlyphTint.alpha(m, g * 0.5F));
        }
        this.canvas.outwardTriangle(
                this.capX[0],
                d2,
                this.capZ[0],
                this.capX[1],
                d2,
                this.capZ[1],
                this.capX[2],
                d2,
                this.capZ[2],
                mark.x,
                e2,
                mark.z,
                GlyphTint.alpha(this.deep, g * 0.65F));
        this.canvas.outwardTriangle(
                this.capX[0],
                d2,
                this.capZ[0],
                this.capX[2],
                d2,
                this.capZ[2],
                this.capX[3],
                d2,
                this.capZ[3],
                mark.x,
                e2,
                mark.z,
                GlyphTint.alpha(this.deep, g * 0.65F));
        int alpha = GlyphTint.alpha(this.light, g * 0.95F);
        for (int o = 0; o < 4; o++) {
            int n = o + 1 & 3;
            this.canvas.line(
                    this.capX[o], d2, this.capZ[o], this.capX[n], d2, this.capZ[n], alpha, 2.2F);
            this.canvas.line(this.capX[o], d2, this.capZ[o], mark.x, e, mark.z, alpha, 2.2F);
        }
        double d3 = e - d;
        int alpha2 = GlyphTint.alpha(this.gold, g * 0.45F);
        for (int r = 0; r < 3; r++) {
            double e3 = e - d3 * (0.12 + r * 0.3);
            double d4 = e3 - d3 * 0.14;
            this.canvas.line(mark.x, e3, mark.z, mark.x, d4, mark.z, alpha2, 1.6F);
        }
        float f3 = (float) (-this.phase * 1.9 % 6.2831854820251465);
        float g3 = f2 * 1.7F;
        for (int t = 0; t < 4; t++) {
            float h3 = f3 + t * 1.5707964F + 0.7853982F;
            float f4 = 0.6F + 0.4F * MathHelper.sin(this.phase * 7.0 + t * 2.1);
            this.canvas.sprite(
                    GlyphShapes.SPARK,
                    mark.x + MathHelper.cos(h3) * g3,
                    d2 - h * 0.15,
                    mark.z + MathHelper.sin(h3) * g3,
                    0.06F * f * f4,
                    h3,
                    GlyphTint.alpha(this.light, g * f4));
        }
    }

    private float blink() {
        double d = this.phase % 4.6;
        return d > 0.22 ? 1.0F : 1.0F - 0.88F * MathHelper.sin(d / 0.22 * 3.141592653589793);
    }

    private void ring(double d, double e, double d2, float f, float g, int i, int j) {
        this.canvas.ring(d, e, d2, f, g, MobilePerformance.segments(i, 12), j);
    }

    private static float[] scatter(int i, long l) {
        Random random = Random.create(l);
        float[] floats = new float[i];
        for (int j = 0; j < i; j++) {
            floats[j] = random.nextFloat();
        }
        return floats;
    }

    private static float unfurl(float f) {
        float g = 1.0F - f;
        return 1.0F - g * g * (1.0F + 1.6F * f) * (1.0F - 0.6F * f);
    }

    @Environment(EnvType.CLIENT)
    private static final class Mark {

        LivingEntity entity;

        double x;

        double y;

        double z;

        float width = 0.6F;

        float height = 1.8F;

        float presence;

        float hurt;

        private Mark() {}

        void assign(LivingEntity entity2) {
            this.entity = entity2;
            this.presence = 0.0F;
            this.hurt = 0.0F;
        }

        void follow(float f, ClientWorld world) {
            LivingEntity entity2 = this.entity;
            if (entity2 != null) {
                if (!entity2.isRemoved() && entity2.getEntityWorld() == world) {
                    this.x = MathHelper.lerp((double) f, entity2.lastX, entity2.getX());
                    this.y = MathHelper.lerp((double) f, entity2.lastY, entity2.getY());
                    this.z = MathHelper.lerp((double) f, entity2.lastZ, entity2.getZ());
                    this.width = entity2.getWidth();
                    this.height = entity2.getHeight();
                    this.hurt =
                            entity2.hurtTime > 0
                                    ? MathHelper.clamp((entity2.hurtTime - f) / 10.0F, 0.0F, 1.0F)
                                    : 0.0F;
                } else {
                    this.hurt = 0.0F;
                }
            }
        }
    }
}
