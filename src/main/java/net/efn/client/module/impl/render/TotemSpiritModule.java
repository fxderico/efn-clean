package net.efn.client.module.impl.render;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.MobilePerformance;
import net.efn.client.util.combat.TotemPops;
import net.efn.client.util.glyph.GlyphCanvas;
import net.efn.client.util.glyph.GlyphShapes;
import net.efn.client.util.glyph.GlyphTint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class TotemSpiritModule extends ClientModule {

    private static final String LOOK_GLASS = "Glass";

    private static final String LOOK_WIRE = "Wire";

    private static final int MAX_SPIRITS = 12;

    private static final double MAX_DISTANCE_SQR = 9216.0;

    private static final float MODEL_UNIT = 0.05859375F;

    private static final float HIP = 12.0F;

    private static final float HEAD_TOP = 32.0F;

    private static final float CROUCH_LEAN = 0.45F;

    private static final float CROUCH_DROP = 3.0F;

    private static final float KA_ARM_PITCH = 2.75F;

    private static final float KA_ARM_SPREAD = 0.32F;

    private static final float SHELL = 0.7F;

    private static final float SEAL_SPAN = 0.6F;

    private static final float[][] PARTS =
            new float[][] {
                {-1.9F, 12.0F, 0.0F, -2.0F, -12.0F, -2.0F, 2.0F, 0.0F, 2.0F, 0.0F},
                {1.9F, 12.0F, 0.0F, -2.0F, -12.0F, -2.0F, 2.0F, 0.0F, 2.0F, 0.0F},
                {0.0F, 12.0F, 0.0F, -4.0F, 0.0F, -2.0F, 4.0F, 12.0F, 2.0F, 0.4F},
                {-5.0F, 22.0F, 0.0F, -3.0F, -10.0F, -2.0F, 1.0F, 2.0F, 2.0F, 0.6F},
                {5.0F, 22.0F, 0.0F, -1.0F, -10.0F, -2.0F, 3.0F, 2.0F, 2.0F, 0.6F},
                {0.0F, 24.0F, 0.0F, -4.0F, 0.0F, -4.0F, 4.0F, 8.0F, 4.0F, 1.0F}
            };

    private static final int FIRST_TORSO_PART = 2;

    private static final int[] FACES =
            new int[] {1, 3, 7, 5, 0, 4, 6, 2, 2, 6, 7, 3, 0, 1, 5, 4, 4, 5, 7, 6, 0, 2, 3, 1};

    private static final int[] EDGES =
            new int[] {0, 1, 2, 3, 4, 5, 6, 7, 0, 2, 1, 3, 4, 6, 5, 7, 0, 4, 1, 5, 2, 6, 3, 7};

    private final NumberSetting kaTime =
            this.addSetting("ka-time", new NumberSetting("Ka Time", 2.4F, 0.6F, 6.0F).step(0.1F));

    private final NumberSetting ascent =
            this.addSetting("ascent", new NumberSetting("Ascent", 1.6F, 0.0F, 4.0F).step(0.1F));

    private final ModeSetting look =
            this.addSetting("look", new ModeSetting("Look", "Glass", "Glass", "Wire"));

    private final NumberSetting opacity =
            this.addSetting(
                    "opacity", new NumberSetting("Opacity", 0.65F, 0.15F, 1.0F).step(0.05F));

    private final BooleanSetting kaPose =
            this.addSetting("ka-pose", new BooleanSetting("Ka Pose", true));

    private final BooleanSetting crest =
            this.addSetting("crest", new BooleanSetting("Bolt Crest", true));

    private final BooleanSetting seal =
            this.addSetting("seal", new BooleanSetting("Ground Seal", true));

    private final BooleanSetting own =
            this.addSetting("own", new BooleanSetting("Own Pops", false));

    private final BooleanSetting throughWalls =
            this.addSetting("through-walls", new BooleanSetting("Through Walls", false));

    private final ModeSetting colorMode =
            this.addSetting("color-mode", new ModeSetting("Color", "Theme", "Theme", "Manual"));

    private final ColorSetting bodyColor =
            this.addSetting("body-color", new ColorSetting("Body Color", -11891723));

    private final ColorSetting auraColor =
            this.addSetting("aura-color", new ColorSetting("Aura Color", -14860125));

    private final TotemSpiritModule.Spirit[] spirits = new TotemSpiritModule.Spirit[12];

    private final GlyphCanvas canvas = new GlyphCanvas();

    private final GlyphTint palette = new GlyphTint();

    private final Consumer<LivingEntity> popSink = this::capture;

    private final double[] corners = new double[24];

    private final float[] pitch = new float[PARTS.length];

    private final float[] yaw = new float[PARTS.length];

    private final float[] roll = new float[PARTS.length];

    private final float[] rotations = new float[PARTS.length * 6];

    private boolean cachedPose;

    private int head;

    private int count;

    private ClientWorld boundLevel;

    private double originX;

    private double originY;

    private double originZ;

    private float leftX;

    private float leftZ;

    private float forwardX;

    private float forwardZ;

    private float unit;

    private float leanSin;

    private float leanCos;

    private float drop;

    public TotemSpiritModule() {
        super(
                "totem-spirit",
                "Totem Spirit",
                "A glowing ghost rises when a player pops a totem.",
                ModuleCategory.VISUALS);
        for (int i = 0; i < 12; i++) {
            this.spirits[i] = new TotemSpiritModule.Spirit();
        }
        this.bodyColor.visibleWhen(() -> this.colorMode.is("Manual"));
        this.auraColor.visibleWhen(() -> this.colorMode.is("Manual"));
    }

    @Override
    protected void onEnable() {
        this.reset();
        this.on(PacketReceiveEvent.TYPE, this::onPacket);
        this.on(TickEvent.TYPE, Stage.POST, this::onTick);
        this.on(Render3DEvent.TYPE, this::onRender);
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    private void reset() {
        this.head = 0;
        this.count = 0;
        this.boundLevel = null;
    }

    private boolean bind(ClientWorld boundLevel2) {
        if (boundLevel2 == null) {
            if (this.boundLevel != null) {
                this.reset();
            }
            return false;
        } else {
            if (boundLevel2 != this.boundLevel) {
                this.head = 0;
                this.count = 0;
                this.boundLevel = boundLevel2;
            }
            return true;
        }
    }

    private void onTick(TickEvent event) {
        if (this.boundLevel != null) {
            this.bind(MinecraftClient.getInstance().world);
        }
    }

    private void onPacket(PacketReceiveEvent event) {
        TotemPops.scan(event.packet, this.popSink);
    }

    private void capture(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity instanceof PlayerEntity player
                && this.bind(client.world)
                && player.getEntityWorld() == client.world) {
            if (!player.isSpectator()) {
                boolean bl = player == client.player;
                if (!bl || this.own.get()) {
                    if (this.count == 12) {
                        this.head = (this.head + 1) % 12;
                        this.count--;
                    }
                    TotemSpiritModule.Spirit spirit = this.spirits[(this.head + this.count) % 12];
                    this.count++;
                    spirit.x = player.getX();
                    spirit.y = player.getY();
                    spirit.z = player.getZ();
                    spirit.bodyYaw = player.bodyYaw;
                    spirit.headYaw = MathHelper.wrapDegrees(player.headYaw - player.bodyYaw);
                    spirit.headPitch = player.getPitch();
                    spirit.walk = player.limbAnimator.getAnimationProgress();
                    spirit.stride = Math.min(1.0F, player.limbAnimator.getSpeed());
                    spirit.scale = player.getScale();
                    spirit.crouch = player.isInSneakingPose();
                    spirit.mine = bl;
                    spirit.born = System.nanoTime();
                }
            }
        }
    }

    private void onRender(Render3DEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.bind(client.world) && this.count != 0) {
            long l = System.nanoTime();
            float f;
            for (f = this.kaTime.get() * 1.0E9F;
                    this.count > 0 && (float) (l - this.spirits[this.head].born) >= f;
                    this.count--) {
                this.head = (this.head + 1) % 12;
            }
            if (this.count != 0) {
                this.palette.resolve(
                        this.colorMode.get(),
                        CosmeticAccent.argb(),
                        this.bodyColor.argb(),
                        this.auraColor.argb());
                this.canvas.begin(client.gameRenderer.getCamera());
                for (int i = 0; i < this.count; i++) {
                    TotemSpiritModule.Spirit spirit = this.spirits[(this.head + i) % 12];
                    if ((!spirit.mine || this.own.get())
                            && !(this.canvas.distanceSqr(spirit.x, spirit.y, spirit.z) > 9216.0)
                            && (!MobilePerformance.active()
                                    || event.frustum == null
                                    || event.frustum.isVisible(this.renderBounds(spirit)))) {
                        this.draw(spirit, Math.min(1.0F, (float) (l - spirit.born) / f));
                    }
                }
                this.canvas.submit(this.throughWalls.get());
            }
        }
    }

    private Box renderBounds(TotemSpiritModule.Spirit spirit) {
        double d = Math.max(0.01F, spirit.scale);
        double e = d * 2.0 + 0.25;
        return new Box(
                spirit.x - e,
                spirit.y - d,
                spirit.z - e,
                spirit.x + e,
                spirit.y + this.ascent.get() * 1.3 + d * 3.5 + 0.5,
                spirit.z + e);
    }

    private void draw(TotemSpiritModule.Spirit spirit, float f) {
        float g = this.opacity.get() * Math.min(1.0F, f / 0.06F);
        float h = this.kaPose.get() ? smooth((f - 0.08F) / 0.45F) : 0.0F;
        float f2 = this.ascent.get() * ascend(f);
        if (this.seal.get() && f < 0.6F) {
            this.groundSeal(spirit, f / 0.6F, g);
        }
        double d = Math.toRadians(spirit.bodyYaw);
        float g2 = (float) Math.cos(d);
        float h2 = (float) Math.sin(d);
        this.leftX = g2;
        this.leftZ = h2;
        this.forwardX = -h2;
        this.forwardZ = g2;
        this.originX = spirit.x;
        this.originY = spirit.y + f2;
        this.originZ = spirit.z;
        this.unit = 0.05859375F * spirit.scale * (1.0F + 0.08F * f);
        float f3 = spirit.crouch ? 0.45F * (1.0F - h) : 0.0F;
        this.leanSin = MathHelper.sin(f3);
        this.leanCos = MathHelper.cos(f3);
        this.drop = spirit.crouch ? 3.0F * (1.0F - h) : 0.0F;
        this.pose(spirit, h);
        this.cachedPose = MobilePerformance.active();
        if (this.cachedPose) {
            this.cachePose();
        }
        boolean is = this.look.is("Glass");
        int primary = this.palette.primary();
        int secondary = this.palette.secondary();
        int highlight = this.palette.highlight();
        for (int m = 0; m < PARTS.length; m++) {
            float g3 = g * dissolve(f, PARTS[m][9]);
            if (!(g3 < 0.01F)) {
                this.place(m, 0.7F);
                this.fill(GlyphTint.alpha(secondary, g3 * (is ? 0.16F : 0.1F)));
            }
        }
        for (int n = 0; n < PARTS.length; n++) {
            float h3 = PARTS[n][9];
            float f4 = g * dissolve(f, h3);
            if (!(f4 < 0.01F)) {
                this.place(n, 0.0F);
                if (is) {
                    this.fill(
                            GlyphTint.alpha(
                                    GlyphTint.mix(primary, secondary, (1.0F - h3) * 0.45F),
                                    f4 * 0.32F));
                }
                this.edges(GlyphTint.alpha(highlight, f4 * (is ? 0.8F : 0.95F)), is ? 1.5F : 2.0F);
            }
        }
        if (this.crest.get()) {
            this.boltCrest(spirit, f, f2, g * dissolve(f, 1.1F));
        }
    }

    private void pose(TotemSpiritModule.Spirit spirit, float f) {
        float g = MathHelper.cos(spirit.walk * 0.6662F) * spirit.stride;
        float h = 1.0F - f;
        float f2 = spirit.crouch ? 0.35F : 0.0F;
        this.pitch[0] = g * 1.4F * h - 0.05F * f;
        this.pitch[1] = -g * 1.4F * h - 0.15F * f;
        this.pitch[2] = 0.0F;
        this.pitch[3] = MathHelper.lerp(f, -g + f2, 2.75F);
        this.pitch[4] = MathHelper.lerp(f, g + f2, 2.75F);
        this.pitch[5] = -spirit.headPitch * 0.017453292F * h + 0.3F * f;
        this.yaw[5] = spirit.headYaw * 0.017453292F * h;
        this.roll[3] = 0.32F * f;
        this.roll[4] = -0.32F * f;
    }

    private void place(int i, float f) {
        float[] floats = PARTS[i];
        boolean bl = i >= 2;
        int j = i * 6;
        float g = this.cachedPose ? this.rotations[j] : MathHelper.cos(this.pitch[i]);
        float h = this.cachedPose ? this.rotations[j + 1] : MathHelper.sin(this.pitch[i]);
        float f2 = this.cachedPose ? this.rotations[j + 2] : MathHelper.cos(this.yaw[i]);
        float g2 = this.cachedPose ? this.rotations[j + 3] : MathHelper.sin(this.yaw[i]);
        float h2 = this.cachedPose ? this.rotations[j + 4] : MathHelper.cos(this.roll[i]);
        float f3 = this.cachedPose ? this.rotations[j + 5] : MathHelper.sin(this.roll[i]);
        for (int k = 0; k < 8; k++) {
            float g3 = (k & 1) == 0 ? floats[3] - f : floats[6] + f;
            float h3 = (k & 2) == 0 ? floats[4] - f : floats[7] + f;
            float f4 = (k & 4) == 0 ? floats[5] - f : floats[8] + f;
            float g4 = h3 * g + f4 * h;
            float h4 = f4 * g - h3 * h;
            float f5 = g3 * f2 - h4 * g2;
            float g5 = g3 * g2 + h4 * f2;
            float h5 = f5 * h2 - g4 * f3 + floats[0];
            float f6 = f5 * f3 + g4 * h2 + floats[1];
            float g6 = g5 + floats[2];
            if (bl && this.lean()) {
                float h6 = f6 - 12.0F;
                float f7 = h6 * this.leanCos - g6 * this.leanSin;
                g6 = h6 * this.leanSin + g6 * this.leanCos;
                f6 = f7 + 12.0F;
            }
            int m = k * 3;
            this.corners[m] = this.originX + (h5 * this.leftX + g6 * this.forwardX) * this.unit;
            float g7;
            this.corners[m + 1] = this.originY + (g7 = f6 - this.drop) * this.unit;
            this.corners[m + 2] = this.originZ + (h5 * this.leftZ + g6 * this.forwardZ) * this.unit;
        }
    }

    private void cachePose() {
        for (int i = 0; i < PARTS.length; i++) {
            int j = i * 6;
            this.rotations[j] = MathHelper.cos(this.pitch[i]);
            this.rotations[j + 1] = MathHelper.sin(this.pitch[i]);
            this.rotations[j + 2] = MathHelper.cos(this.yaw[i]);
            this.rotations[j + 3] = MathHelper.sin(this.yaw[i]);
            this.rotations[j + 4] = MathHelper.cos(this.roll[i]);
            this.rotations[j + 5] = MathHelper.sin(this.roll[i]);
        }
    }

    private boolean lean() {
        return this.leanSin != 0.0F;
    }

    private void fill(int i) {
        double[] corners2 = this.corners;
        for (int j = 0; j < FACES.length; j += 4) {
            int k = FACES[j] * 3;
            int m = FACES[j + 1] * 3;
            int n = FACES[j + 2] * 3;
            int o = FACES[j + 3] * 3;
            this.canvas.quad(
                    corners2[k],
                    corners2[k + 1],
                    corners2[k + 2],
                    corners2[m],
                    corners2[m + 1],
                    corners2[m + 2],
                    corners2[n],
                    corners2[n + 1],
                    corners2[n + 2],
                    corners2[o],
                    corners2[o + 1],
                    corners2[o + 2],
                    i);
        }
    }

    private void edges(int i, float f) {
        double[] corners2 = this.corners;
        for (int j = 0; j < EDGES.length; j += 2) {
            int k = EDGES[j] * 3;
            int m = EDGES[j + 1] * 3;
            this.canvas.line(
                    corners2[k],
                    corners2[k + 1],
                    corners2[k + 2],
                    corners2[m],
                    corners2[m + 1],
                    corners2[m + 2],
                    i,
                    f);
        }
    }

    private void groundSeal(TotemSpiritModule.Spirit spirit, float f, float g) {
        float h = 1.0F - (1.0F - f) * (1.0F - f) * (1.0F - f);
        float f2 = (0.35F + 1.15F * h) * spirit.scale;
        float g2 = g * (1.0F - f) * 0.9F;
        double d = spirit.y + 0.02;
        int primary = this.palette.primary();
        int secondary = this.palette.secondary();
        int highlight = this.palette.highlight();
        this.canvas.ring(
                spirit.x,
                d,
                spirit.z,
                f2 * 0.93F,
                f2,
                MobilePerformance.segments(40, 24),
                GlyphTint.alpha(primary, g2));
        this.canvas.ring(
                spirit.x,
                d,
                spirit.z,
                f2,
                f2 * 1.12F,
                MobilePerformance.segments(40, 24),
                GlyphTint.alpha(primary, g2 * 0.25F));
        this.canvas.ring(
                spirit.x,
                d,
                spirit.z,
                f2 * 0.55F,
                f2 * 0.6F,
                MobilePerformance.segments(32, 16),
                GlyphTint.alpha(secondary, g2 * 0.7F));
        float h2 = (float) Math.toRadians(spirit.bodyYaw) + h * 0.6F;
        for (int m = 0; m < 4; m++) {
            float f3 = h2 + m * 1.5707964F;
            this.canvas.flat(
                    GlyphShapes.BOLT,
                    spirit.x + MathHelper.cos(f3) * f2 * 0.78F,
                    d + 0.004,
                    spirit.z + MathHelper.sin(f3) * f2 * 0.78F,
                    0.13F * spirit.scale,
                    -f3 - 1.5707964F,
                    GlyphTint.alpha(highlight, g2));
        }
    }

    private void boltCrest(TotemSpiritModule.Spirit spirit, float f, float g, float h) {
        if (!(h < 0.01F)) {
            float f2 = Math.min(1.0F, f / 0.2F);
            float g2 =
                    1.0F
                            + 1.7F * (f2 - 1.0F) * (f2 - 1.0F) * (f2 - 1.0F)
                            + 0.7F * (f2 - 1.0F) * (f2 - 1.0F);
            float h2 = 0.2F * spirit.scale * Math.max(0.0F, g2);
            if (!(h2 <= 0.001F)) {
                double d =
                        this.originY
                                + (32.0F - this.drop) * this.unit
                                + 0.35F * spirit.scale
                                + g * 0.3F;
                this.canvas.sprite(
                        GlyphShapes.OCTAGON,
                        spirit.x,
                        d,
                        spirit.z,
                        h2 * 2.2F,
                        0.0F,
                        GlyphTint.alpha(this.palette.secondary(), h * 0.2F));
                this.canvas.sprite(
                        GlyphShapes.BOLT,
                        spirit.x,
                        d,
                        spirit.z,
                        h2,
                        0.0F,
                        GlyphTint.alpha(this.palette.highlight(), h));
            }
        }
    }

    private static float dissolve(float f, float g) {
        return 1.0F - MathHelper.clamp((f - 0.45F - g * 0.22F) / 0.3F, 0.0F, 1.0F);
    }

    private static float ascend(float f) {
        float g = Math.max(0.0F, (f - 0.12F) / 0.88F);
        return g * g * (2.2F - 1.2F * g);
    }

    private static float smooth(float f) {
        float g = MathHelper.clamp(f, 0.0F, 1.0F);
        return g * g * (3.0F - 2.0F * g);
    }

    @Environment(EnvType.CLIENT)
    private static final class Spirit {

        double x;

        double y;

        double z;

        float bodyYaw;

        float headYaw;

        float headPitch;

        float walk;

        float stride;

        float scale;

        boolean crouch;

        boolean mine;

        long born;

        private Spirit() {}
    }
}
