package net.efn.client.module.impl.render;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.MobilePerformance;
import net.efn.client.util.glyph.GlyphCanvas;
import net.efn.client.util.glyph.GlyphShapes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;

@Environment(EnvType.CLIENT)
public final class FloatingIconsModule extends ClientModule {

    private static final String SYMBOL_EFN = "EFN";

    private static final String SYMBOL_LARP = "Money";

    private static final float[][] EFN_GLYPHS =
            new float[][] {
                IconGlyphs.LETTER_E,
                IconGlyphs.LETTER_F,
                IconGlyphs.LETTER_N,
                IconGlyphs.BOLT,
                IconGlyphs.GEM,
                GlyphShapes.SPARK
            };

    private static final float[] EFN_FIT = new float[] {1.0F, 1.0F, 1.0F, 1.0F, 0.96F, 1.0F};

    private static final float[][] LARP_GLYPHS =
            new float[][] {IconGlyphs.DOLLAR, IconGlyphs.EURO, IconGlyphs.BITCOIN};

    private static final float[] LARP_FIT = new float[] {1.0F, 0.92F, 0.96F};

    private static final String COLOR_THEME = "Theme";

    private static final String COLOR_MANUAL = "Manual";

    private static final int MAX_ICONS = 32;

    private static final float GRAVITY = 0.016F;

    private static final float BUOYANCY = 0.045F;

    private static final float AIR_DRAG = 0.982F;

    private static final float GROUND_GRIP = 0.86F;

    private static final float ORBIT_PULL = 0.012F;

    private static final float SWIRL = 0.0025F;

    private static final float FOLLOW = 0.035F;

    private static final float WANDER = 0.003F;

    private static final float MAX_SPEED = 0.9F;

    private static final float SPIN_SETTLE = 0.03F;

    private static final float ANGLE_WRAP = 512.0F;

    private static final float TILT_SPRING = 0.09F;

    private static final float TILT_DAMP = 0.9F;

    private static final float MAX_TILT = 0.55F;

    private static final int APPEAR_TICKS = 12;

    private static final int STAGGER_TICKS = 3;

    private static final float GLYPH_SCALE = 1.25F;

    private static final float THICKNESS = 0.07F;

    private static final float SWAY = 0.12F;

    private static final float BOB = 0.2F;

    private static final float FACE_ALPHA = 0.94F;

    private static final float BACK_ALPHA = 0.9F;

    private static final float GLINT = 0.45F;

    private static final float[] HALO_SCALES = new float[] {1.35F, 2.0F};

    private static final float[] HALO_WEIGHTS = new float[] {1.0F, 0.5F};

    private static final float HALO_PUSH = 0.6F;

    private static final double DETAIL_RANGE_SQR = 576.0;

    private static final float LIGHT_X;

    private static final float LIGHT_Y;

    private static final float LIGHT_Z;

    private static final long EPOCH = System.nanoTime();

    private final ModeSetting symbols =
            this.addSetting("symbols", new ModeSetting("Symbols", "EFN", "EFN", "Money"));

    private final NumberSetting count =
            this.addSetting("count", new NumberSetting("Count", 9.0F, 1.0F, 32.0F).step(1.0F));

    private final NumberSetting size =
            this.addSetting("size", new NumberSetting("Size", 0.3F, 0.15F, 0.75F).step(0.05F));

    private final NumberSetting spread =
            this.addSetting("spread", new NumberSetting("Spread", 2.6F, 1.2F, 6.0F).step(0.1F));

    private final NumberSetting bounce =
            this.addSetting("bounce", new NumberSetting("Bounce", 0.6F, 0.1F, 0.95F).step(0.05F));

    private final NumberSetting drift =
            this.addSetting("drift", new NumberSetting("Drift", 0.5F, 0.0F, 2.5F).step(0.1F));

    private final NumberSetting spinSpeed =
            this.addSetting("spin", new NumberSetting("Spin", 1.0F, 0.2F, 3.0F).step(0.1F));

    private final ModeSetting colorMode =
            this.addSetting("color", new ModeSetting("Color", "Theme", "Theme", "Manual"));

    private final ColorSetting mainColor =
            this.addSetting("main-color", new ColorSetting("Main Color", -12614409));

    private final ColorSetting secondColor =
            this.addSetting("second-color", new ColorSetting("Second Color", -14923108));

    private final BooleanSetting halo = this.addSetting("halo", new BooleanSetting("Halo", true));

    private final BooleanSetting blocks =
            this.addSetting("block-bounce", new BooleanSetting("Block Bounce", true));

    private final FloatingIconsModule.FloatIcon[] floatIcons =
            new FloatingIconsModule.FloatIcon[32];

    private final GlyphCanvas canvas = new GlyphCanvas();

    private final Random random = Random.create();

    private final Mutable cursor = new Mutable();

    private int live;

    private int age;

    private ClientWorld boundLevel;

    private int tintMain;

    private int tintShade;

    private int tintShine;

    public FloatingIconsModule() {
        super(
                "floating-icons",
                "Floating Icons",
                "EFN letters, bolts and gems float and spin around you.",
                ModuleCategory.VISUALS);
        this.mainColor.visibleWhen(() -> this.colorMode.is("Manual"));
        this.secondColor.visibleWhen(() -> this.colorMode.is("Manual"));
        for (int i = 0; i < 32; i++) {
            this.floatIcons[i] = new FloatingIconsModule.FloatIcon();
        }
    }

    @Override
    protected void onEnable() {
        this.reset();
        this.on(TickEvent.TYPE, Stage.POST, this::onTick);
        this.on(Render3DEvent.TYPE, this::onRender3D);
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    private void reset() {
        this.live = 0;
        this.age = 0;
        this.boundLevel = null;
    }

    private void syncLevel(ClientWorld boundLevel2) {
        if (boundLevel2 != this.boundLevel) {
            this.boundLevel = boundLevel2;
            this.live = 0;
        }
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player != null && world != null) {
            this.syncLevel(world);
            this.age++;
            int particles = MobilePerformance.particles(Math.min(32, this.count.getInt()), 6);
            for (int j = this.live; j < particles; j++) {
                this.wake(this.floatIcons[j], (j - this.live) * 3);
            }
            this.live = particles;
            double d = player.getX();
            double e = player.getY();
            double d2 = player.getZ();
            float height = player.getHeight();
            float f = (float) (d - player.lastX);
            float g = (float) (d2 - player.lastZ);
            float bounce2 = this.bounce.get();
            for (int k = 0; k < this.live; k++) {
                this.step(this.floatIcons[k], world, d, e, d2, height, f, g, bounce2);
            }
            for (int m = 0; m < this.live; m++) {
                for (int n = m + 1; n < this.live; n++) {
                    this.knock(this.floatIcons[m], this.floatIcons[n], bounce2);
                }
            }
        } else {
            this.reset();
        }
    }

    private void wake(FloatingIconsModule.FloatIcon floatIcon, int i) {
        floatIcon.placed = false;
        floatIcon.wakeAt = this.age + i;
        floatIcon.orbit = 0.55F + 0.45F * this.random.nextFloat();
        floatIcon.lift = this.random.nextFloat();
        floatIcon.grain = 0.82F + 0.3F * this.random.nextFloat();
        floatIcon.phase = this.random.nextFloat() * 6.2831855F;
        floatIcon.restTurn = this.signed(0.07F, 0.12F);
    }

    private float signed(float f, float g) {
        float h = f + (g - f) * this.random.nextFloat();
        return this.random.nextBoolean() ? h : -h;
    }

    private void place(FloatingIconsModule.FloatIcon floatIcon, double d, double e, double d2) {
        float f = this.random.nextFloat() * 6.2831855F;
        double e2 = this.spread.get() * floatIcon.orbit;
        floatIcon.x = d + MathHelper.cos(f) * e2;
        floatIcon.y = e + 0.15;
        floatIcon.z = d2 + MathHelper.sin(f) * e2;
        floatIcon.lastX = floatIcon.x;
        floatIcon.lastY = floatIcon.y;
        floatIcon.lastZ = floatIcon.z;
        floatIcon.vx = -MathHelper.sin(f) * 0.03F;
        floatIcon.vy = 0.14F + 0.08F * this.random.nextFloat();
        floatIcon.vz = MathHelper.cos(f) * 0.03F;
        floatIcon.lastYaw = floatIcon.yaw = this.random.nextFloat() * 6.2831855F;
        floatIcon.turn = floatIcon.restTurn * 3.0F;
        floatIcon.tiltX = 0.0F;
        floatIcon.tiltZ = 0.0F;
        floatIcon.lastTiltX = 0.0F;
        floatIcon.lastTiltZ = 0.0F;
        floatIcon.swayX = this.signed(0.02F, 0.05F);
        floatIcon.swayZ = this.signed(0.02F, 0.05F);
        floatIcon.placed = true;
        floatIcon.placedAt = this.age;
    }

    private void step(
            FloatingIconsModule.FloatIcon floatIcon,
            ClientWorld world,
            double d,
            double e,
            double d2,
            float f,
            float g,
            float h,
            float f2) {
        if (!floatIcon.placed) {
            if (this.age < floatIcon.wakeAt) {
                return;
            }
            this.place(floatIcon, d, e, d2);
        }
        floatIcon.lastX = floatIcon.x;
        floatIcon.lastY = floatIcon.y;
        floatIcon.lastZ = floatIcon.z;
        double e2 = floatIcon.x - d;
        double d3 = floatIcon.z - d2;
        double e3 = Math.sqrt(e2 * e2 + d3 * d3);
        double d4 =
                e
                        + 0.35
                        + floatIcon.lift * (f + 0.2)
                        + 0.16 * MathHelper.sin((this.age + floatIcon.phase * 16.0F) * 0.045F);
        double e4 = this.spread.get() * 3.0 + 8.0;
        if (!(e3 > e4) && !(Math.abs(floatIcon.y - d4) > e4)) {
            float g2 = e3 > 1.0E-4 ? (float) (e2 / e3) : 1.0F;
            float h2 = e3 > 1.0E-4 ? (float) (d3 / e3) : 0.0F;
            float f3 = (float) (this.spread.get() * floatIcon.orbit - e3) * 0.012F;
            float g3 = this.drift.get() * 0.0025F;
            floatIcon.vx += g2 * f3 - h2 * g3 + g * 0.035F;
            floatIcon.vz += h2 * f3 + g2 * g3 + h * 0.035F;
            float h3 = (this.age + floatIcon.phase * 20.0F) * 0.06F;
            floatIcon.vx = floatIcon.vx + MathHelper.sin(h3) * 0.003F;
            floatIcon.vz = floatIcon.vz + MathHelper.cos(h3 * 1.31F) * 0.003F;
            floatIcon.vy -= 0.016F;
            double d5 = d4 - floatIcon.y;
            if (d5 > 0.0) {
                floatIcon.vy = floatIcon.vy + (float) Math.min(d5, 1.5) * 0.045F;
            }
            floatIcon.vx *= 0.982F;
            floatIcon.vy *= 0.982F;
            floatIcon.vz *= 0.982F;
            float f4 =
                    floatIcon.vx * floatIcon.vx
                            + floatIcon.vy * floatIcon.vy
                            + floatIcon.vz * floatIcon.vz;
            if (f4 > 0.80999994F) {
                float g4 = 0.9F / (float) Math.sqrt(f4);
                floatIcon.vx *= g4;
                floatIcon.vy *= g4;
                floatIcon.vz *= g4;
            }
            boolean blocks2 = this.blocks.get();
            float h4 = this.size.get() * floatIcon.grain * 0.5F;
            double e5 = floatIcon.x + floatIcon.vx;
            if (blocks2
                    && this.solid(
                            world,
                            e5 + Math.copySign(h4, floatIcon.vx),
                            floatIcon.y,
                            floatIcon.z)) {
                floatIcon.vx = -floatIcon.vx * f2;
                this.tumble(floatIcon, 0.6F);
            } else {
                floatIcon.x = e5;
            }
            e5 = floatIcon.y + floatIcon.vy;
            if (blocks2
                    && this.solid(
                            world,
                            floatIcon.x,
                            e5 + Math.copySign(h4, floatIcon.vy),
                            floatIcon.z)) {
                if (floatIcon.vy < 0.0F) {
                    floatIcon.vx *= 0.86F;
                    floatIcon.vz *= 0.86F;
                }
                floatIcon.vy = -floatIcon.vy * f2;
                this.tumble(floatIcon, 0.8F);
            } else {
                floatIcon.y = e5;
            }
            e5 = floatIcon.z + floatIcon.vz;
            if (blocks2
                    && this.solid(
                            world,
                            floatIcon.x,
                            floatIcon.y,
                            e5 + Math.copySign(h4, floatIcon.vz))) {
                floatIcon.vz = -floatIcon.vz * f2;
                this.tumble(floatIcon, 0.6F);
            } else {
                floatIcon.z = e5;
            }
            if (blocks2 && this.solid(world, floatIcon.x, floatIcon.y, floatIcon.z)) {
                floatIcon.y += 0.2;
                floatIcon.lastY = floatIcon.y;
                floatIcon.vy = Math.max(floatIcon.vy, 0.08F);
            }
            if (Math.abs(floatIcon.yaw) > 512.0F) {
                floatIcon.yaw %= 6.2831855F;
            }
            floatIcon.lastYaw = floatIcon.yaw;
            floatIcon.lastTiltX = floatIcon.tiltX;
            floatIcon.lastTiltZ = floatIcon.tiltZ;
            floatIcon.turn =
                    floatIcon.turn
                            + (floatIcon.restTurn * this.spinSpeed.get() - floatIcon.turn) * 0.03F;
            floatIcon.yaw = floatIcon.yaw + floatIcon.turn;
            floatIcon.swayX = (floatIcon.swayX - floatIcon.tiltX * 0.09F) * 0.9F;
            floatIcon.swayZ = (floatIcon.swayZ - floatIcon.tiltZ * 0.09F) * 0.9F;
            floatIcon.tiltX = MathHelper.clamp(floatIcon.tiltX + floatIcon.swayX, -0.55F, 0.55F);
            floatIcon.tiltZ = MathHelper.clamp(floatIcon.tiltZ + floatIcon.swayZ, -0.55F, 0.55F);
        } else {
            this.place(floatIcon, d, e, d2);
        }
    }

    private void knock(
            FloatingIconsModule.FloatIcon floatIcon,
            FloatingIconsModule.FloatIcon floatIcon2,
            float f) {
        if (floatIcon.placed && floatIcon2.placed) {
            double d = floatIcon2.x - floatIcon.x;
            double e = floatIcon2.y - floatIcon.y;
            double d2 = floatIcon2.z - floatIcon.z;
            double e2 = d * d + e * e + d2 * d2;
            double d3 = this.size.get() * (floatIcon.grain + floatIcon2.grain) * 0.5 * 0.95;
            if (!(e2 >= d3 * d3) && !(e2 < 1.0E-8)) {
                double e3 = Math.sqrt(e2);
                float g = (float) (d / e3);
                float h = (float) (e / e3);
                float f2 = (float) (d2 / e3);
                double d4 = (d3 - e3) * 0.5;
                floatIcon.x -= g * d4;
                floatIcon.y -= h * d4;
                floatIcon.z -= f2 * d4;
                floatIcon2.x += g * d4;
                floatIcon2.y += h * d4;
                floatIcon2.z += f2 * d4;
                float g2 =
                        (floatIcon.vx - floatIcon2.vx) * g
                                + (floatIcon.vy - floatIcon2.vy) * h
                                + (floatIcon.vz - floatIcon2.vz) * f2;
                if (!(g2 <= 0.0F)) {
                    float h2 = g2 * (1.0F + f) * 0.5F;
                    floatIcon.vx -= h2 * g;
                    floatIcon.vy -= h2 * h;
                    floatIcon.vz -= h2 * f2;
                    floatIcon2.vx += h2 * g;
                    floatIcon2.vy += h2 * h;
                    floatIcon2.vz += h2 * f2;
                    this.tumble(floatIcon, 0.5F);
                    this.tumble(floatIcon2, 0.5F);
                }
            }
        }
    }

    private void tumble(FloatingIconsModule.FloatIcon floatIcon, float f) {
        floatIcon.turn = floatIcon.turn + (this.random.nextFloat() - 0.5F) * 0.3F * f;
        floatIcon.swayX = floatIcon.swayX + (this.random.nextFloat() - 0.5F) * 0.12F * f;
        floatIcon.swayZ = floatIcon.swayZ + (this.random.nextFloat() - 0.5F) * 0.12F * f;
    }

    private boolean solid(ClientWorld world, double d, double e, double d2) {
        this.cursor.set(MathHelper.floor(d), MathHelper.floor(e), MathHelper.floor(d2));
        BlockState blockState = world.getBlockState(this.cursor);
        if (blockState.isAir()) {
            return false;
        } else {
            VoxelShape collisionShape = blockState.getCollisionShape(world, this.cursor);
            if (collisionShape.isEmpty()) {
                return false;
            } else {
                double e2 = d - this.cursor.getX();
                double d3 = e - this.cursor.getY();
                double e3 = d2 - this.cursor.getZ();
                return e2 >= collisionShape.getMin(Axis.X)
                        && e2 <= collisionShape.getMax(Axis.X)
                        && d3 >= collisionShape.getMin(Axis.Y)
                        && d3 <= collisionShape.getMax(Axis.Y)
                        && e3 >= collisionShape.getMin(Axis.Z)
                        && e3 <= collisionShape.getMax(Axis.Z);
            }
        }
    }

    private void onRender3D(Render3DEvent event) {
        if (this.live != 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && client.player != null) {
                this.canvas.begin(client.gameRenderer.getCamera());
                float partialTick = event.partialTick;
                float f = (float) (System.nanoTime() - EPOCH) / 1.0E9F;
                boolean active = MobilePerformance.active();
                Frustum frustum =
                        !PerformanceModule.frustumCulling() && !active ? null : event.frustum;
                float g = this.size.get() * 0.5F * 1.25F;
                boolean is = this.symbols.is("Money");
                float[][] floatGrid = is ? LARP_GLYPHS : EFN_GLYPHS;
                float[] floats = is ? LARP_FIT : EFN_FIT;
                boolean halo2 = this.halo.get();
                this.tint();
                for (int i = 0; i < this.live; i++) {
                    FloatingIconsModule.FloatIcon floatIcon = this.floatIcons[i];
                    float h;
                    int j;
                    float f2;
                    if (floatIcon.placed
                            && !((f2 =
                                            g
                                                    * floatIcon.grain
                                                    * floats[j = i % floatGrid.length]
                                                    * appear(
                                                            (h =
                                                                            this.age
                                                                                    - floatIcon
                                                                                            .placedAt
                                                                                    + partialTick)
                                                                    / 12.0F))
                                    < 0.004F)) {
                        double d =
                                MathHelper.lerp((double) partialTick, floatIcon.lastX, floatIcon.x);
                        double e =
                                MathHelper.lerp((double) partialTick, floatIcon.lastY, floatIcon.y)
                                        + f2
                                                * 0.2F
                                                * MathHelper.sin(f * 2.4F + floatIcon.phase * 3.0F);
                        double d2 =
                                MathHelper.lerp((double) partialTick, floatIcon.lastZ, floatIcon.z);
                        double renderReach = renderReach(f2, halo2, active);
                        if (frustum == null
                                || frustum.isVisible(
                                        new Box(
                                                d - renderReach,
                                                e - renderReach,
                                                d2 - renderReach,
                                                d + renderReach,
                                                e + renderReach,
                                                d2 + renderReach))) {
                            float g2 = Math.min(1.0F, h / 7.2000003F);
                            float h2 =
                                    MathHelper.lerp(partialTick, floatIcon.lastYaw, floatIcon.yaw);
                            float f3 =
                                    MathHelper.lerp(
                                                    partialTick,
                                                    floatIcon.lastTiltX,
                                                    floatIcon.tiltX)
                                            + 0.12F * MathHelper.sin(f * 1.3F + floatIcon.phase);
                            float g3 =
                                    MathHelper.lerp(
                                                    partialTick,
                                                    floatIcon.lastTiltZ,
                                                    floatIcon.tiltZ)
                                            + 0.12F
                                                    * MathHelper.sin(
                                                            f * 0.9F + floatIcon.phase * 2.3F);
                            float h3 =
                                    halo2
                                            ? (0.1F
                                                            + 0.07F
                                                                    * MathHelper.sin(
                                                                            f * 2.1F
                                                                                    + floatIcon
                                                                                            .phase))
                                                    * g2
                                            : 0.0F;
                            float[] floats2 =
                                    active ? IconGlyphs.mobile(floatGrid[j]) : floatGrid[j];
                            this.drawIcon(floats2, d, e, d2, f2, h2, f3, g3, g2, h3);
                        }
                    }
                }
                this.canvas.submit(false);
            }
        }
    }

    private static double renderReach(float f, boolean bl, boolean bl2) {
        return f * (bl2 && bl ? HALO_SCALES[HALO_SCALES.length - 1] + 0.6F : 2.0);
    }

    private void tint() {
        if (this.colorMode.is("Manual")) {
            this.tintMain = ColorHelper.fullAlpha(this.mainColor.argb());
            this.tintShade = ColorHelper.fullAlpha(this.secondColor.argb());
        } else {
            this.tintMain = CosmeticAccent.argb();
            this.tintShade = ColorHelper.scaleRgb(this.tintMain, 0.45F);
        }
        this.tintShine = ColorHelper.lerp(0.6F, this.tintMain, -1);
    }

    private void drawIcon(
            float[] floats,
            double d,
            double e,
            double d2,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2) {
        float f3 = MathHelper.cos(g);
        float g3 = MathHelper.sin(g);
        float h3 = MathHelper.cos(h);
        float f4 = MathHelper.sin(h);
        float g4 = MathHelper.cos(f2);
        float h4 = MathHelper.sin(f2);
        float f5 = f3 * g4 + g3 * f4 * h4;
        float g5 = h3 * h4;
        float h5 = -g3 * g4 + f3 * f4 * h4;
        float f6 = -f3 * h4 + g3 * f4 * g4;
        float g6 = h3 * g4;
        float h6 = g3 * h4 + f3 * f4 * g4;
        float f7 = g3 * h3;
        float g7 = -f4;
        float h7 = f3 * h3;
        double e2 = this.canvas.camX() - d;
        double d3;
        double e3;
        double d4 =
                e2 * e2 + (e3 = this.canvas.camY() - e) * e3 + (d3 = this.canvas.camZ() - d2) * d3;
        if (!(d4 < 1.0E-8)) {
            double e4 = Math.sqrt(d4);
            float f8 = (float) ((f7 * e2 + g7 * e3 + h7 * d3) / e4);
            if (f8 < 0.0F) {
                f5 = -f5;
                g5 = -g5;
                h5 = -h5;
                f7 = -f7;
                g7 = -g7;
                h7 = -h7;
                f8 = -f8;
            }
            if (h2 > 0.0F) {
                double d5 = f * 0.6F / e4;
                double e5 = d - e2 * d5;
                double d6 = e - e3 * d5;
                double e6 = d2 - d3 * d5;
                float[] floats2 =
                        MobilePerformance.active() ? GlyphShapes.OCTAGON : GlyphShapes.DISK;
                for (int m = 0; m < HALO_SCALES.length; m++) {
                    this.canvas.sprite(
                            floats2,
                            e5,
                            d6,
                            e6,
                            f * HALO_SCALES[m],
                            0.0F,
                            withAlpha(this.tintMain, h2 * HALO_WEIGHTS[m]));
                }
            }
            double d7 = f5 * f;
            double e7 = g5 * f;
            double d8 = h5 * f;
            double e8 = f6 * f;
            double d9 = g6 * f;
            double e9 = h6 * f;
            double d10 = f * 0.07F;
            if (d4 < 576.0 && f8 < 0.995F) {
                this.canvas.shape(
                        floats,
                        d - f7 * d10,
                        e - g7 * d10,
                        d2 - h7 * d10,
                        d7,
                        e7,
                        d8,
                        e8,
                        d9,
                        e9,
                        withAlpha(this.tintShade, 0.9F * g2));
            }
            float g8 = Math.max(0.0F, f7 * LIGHT_X + g7 * LIGHT_Y + h7 * LIGHT_Z);
            float h8 = f8 * f8;
            h8 *= h8;
            h8 *= h8;
            int i =
                    ColorHelper.lerp(
                            h8 * 0.45F,
                            ColorHelper.scaleRgb(this.tintMain, 0.6F + 0.22F * g8 + 0.18F * f8),
                            this.tintShine);
            this.canvas.shape(
                    floats,
                    d + f7 * d10,
                    e + g7 * d10,
                    d2 + h7 * d10,
                    d7,
                    e7,
                    d8,
                    e8,
                    d9,
                    e9,
                    withAlpha(i, 0.94F * g2));
        }
    }

    private static float appear(float f) {
        if (f >= 1.0F) {
            return 1.0F;
        } else if (f <= 0.0F) {
            return 0.0F;
        } else {
            float g = 1.0F - f;
            return 1.0F - g * g * g + 0.22F * MathHelper.sin(f * 3.1415927F) * g;
        }
    }

    private static int withAlpha(int i, float f) {
        int j = (int) (Math.max(0.0F, Math.min(1.0F, f)) * 255.0F + 0.5F);
        return j << 24 | i & 16777215;
    }

    static {
        float f = 0.32F;
        float g = 0.87F;
        float h = 0.37F;
        float f2 = (float) Math.sqrt(f * f + g * g + h * h);
        LIGHT_X = f / f2;
        LIGHT_Y = g / f2;
        LIGHT_Z = h / f2;
    }

    @Environment(EnvType.CLIENT)
    private static final class FloatIcon {

        double x;

        double y;

        double z;

        double lastX;

        double lastY;

        double lastZ;

        float vx;

        float vy;

        float vz;

        float yaw;

        float lastYaw;

        float turn;

        float restTurn;

        float tiltX;

        float tiltZ;

        float lastTiltX;

        float lastTiltZ;

        float swayX;

        float swayZ;

        float orbit;

        float lift;

        float grain;

        float phase;

        boolean placed;

        int wakeAt;

        int placedAt;

        private FloatIcon() {}
    }
}
