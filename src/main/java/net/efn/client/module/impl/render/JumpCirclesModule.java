package net.efn.client.module.impl.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.efn.client.event.Stage;
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
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.MipmapHelper;
import net.minecraft.client.texture.MipmapStrategy;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class JumpCirclesModule extends ClientModule {

    public static volatile JumpCirclesModule active;

    public static final Identifier LOGO_TEXTURE =
            Identifier.of("efn", "textures/misc/jump_circle_logo.png");

    public static final Identifier LOGO_GLOW_TEXTURE =
            Identifier.of("efn", "textures/misc/jump_circle_logo_glow.png");

    private static final String COLOR_ACCENT = "Accent";

    private static final String COLOR_CUSTOM = "Custom";

    private static final int MAX_RINGS = 24;

    private static final int SEGMENTS = 64;

    private static final float[] COS = new float[65];

    private static final float[] SIN = new float[65];

    private static final double LIFT = 0.015;

    private static final double MAX_DISTANCE_SQR = 16384.0;

    private static final double JUMP_RISE = 0.2;

    private static final int RISE_TICKS = 6;

    private static final int SWEEP_TICKS = 100;

    private static final float CORE_WHITE = 0.45F;

    private static final float CORE_ALPHA = 0.95F;

    private static final float HALO_ALPHA = 0.55F;

    private static final float FADE_IN_NANOS = 8.0E7F;

    private static final float LOGO_FILL = 0.75F;

    private static final float LOGO_SPAN = 0.9277344F;

    private static final float LOGO_DROP = 0.0126953125F;

    private static final float LOGO_GLOW_SCALE = 1.5F;

    private final ModeSetting colorMode =
            this.addSetting("color-mode", new ModeSetting("Color", "Accent", "Accent", "Custom"));

    private final ColorSetting color =
            this.addSetting("color", new ColorSetting("Custom Color", -1853860));

    private final NumberSetting size =
            this.addSetting("size", new NumberSetting("Size", 1.2F, 0.5F, 3.0F).step(0.1F));

    private final NumberSetting duration =
            this.addSetting("duration", new NumberSetting("Duration", 1.0F, 0.3F, 3.0F).step(0.1F));

    private final BooleanSetting logo = this.addSetting("logo", new BooleanSetting("Logo", true));

    private final BooleanSetting others =
            this.addSetting("others", new BooleanSetting("Others", false));

    private final double[] ringX = new double[24];

    private final double[] ringY = new double[24];

    private final double[] ringZ = new double[24];

    private final float[] ringFacingX = new float[24];

    private final float[] ringFacingZ = new float[24];

    private final long[] ringBorn = new long[24];

    private int head;

    private int count;

    private float poseX;

    private float poseY;

    private float poseZ;

    private float poseT;

    private float poseRadius;

    private float poseFade;

    private final Int2ObjectOpenHashMap<JumpCirclesModule.Lift> lifts =
            new Int2ObjectOpenHashMap<>();

    private int tick;

    private int levelKey;

    public JumpCirclesModule() {
        super(
                "jump-circles",
                "Jump Circles",
                "Draws a fading EFN ring where you jump.",
                ModuleCategory.VISUALS);
        this.color.visibleWhen(() -> this.colorMode.is("Custom"));
    }

    @Override
    protected void onEnable() {
        this.clear();
        active = this;
        this.on(TickEvent.TYPE, Stage.POST, this::onTick);
    }

    @Override
    protected void onDisable() {
        active = null;
        this.clear();
    }

    private void clear() {
        this.head = 0;
        this.count = 0;
        this.lifts.clear();
        this.levelKey = 0;
    }

    public static void onJump(LivingEntity entity) {
        JumpCirclesModule module = active;
        if (module != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (entity == client.player && client.world != null) {
                module.syncLevel(client.world);
                module.spawn(entity.getX(), entity.getY(), entity.getZ(), entity.getYaw());
            }
        }
    }

    private void spawn(double d, double e, double d2, float f) {
        if (this.count == 24) {
            this.head = (this.head + 1) % 24;
            this.count--;
        }
        int i = (this.head + this.count) % 24;
        this.ringX[i] = d;
        this.ringY[i] = e;
        this.ringZ[i] = d2;
        double e2 = Math.toRadians(f);
        this.ringFacingX[i] = (float) (-Math.sin(e2));
        this.ringFacingZ[i] = (float) Math.cos(e2);
        this.ringBorn[i] = System.nanoTime();
        this.count++;
    }

    private void expire(long l) {
        for (long l2 = this.durationNanos();
                this.count > 0 && l - this.ringBorn[this.head] >= l2;
                this.count--) {
            this.head = (this.head + 1) % 24;
        }
    }

    private long durationNanos() {
        return (long) (this.duration.get() * 1.0E9);
    }

    private void syncLevel(ClientWorld world) {
        int identityHashCode = System.identityHashCode(world);
        if (identityHashCode != this.levelKey) {
            this.levelKey = identityHashCode;
            this.head = 0;
            this.count = 0;
            this.lifts.clear();
        }
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) {
            this.clear();
        } else {
            this.syncLevel(world);
            if (!this.others.get()) {
                if (!this.lifts.isEmpty()) {
                    this.lifts.clear();
                }
            } else {
                this.tick++;
                List players = world.getPlayers();
                for (int i = 0; i < players.size(); i++) {
                    AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) players.get(i);
                    if (player != client.player) {
                        JumpCirclesModule.Lift lift2 = this.lifts.get(player.getId());
                        if (lift2 == null) {
                            lift2 = new JumpCirclesModule.Lift();
                            lift2.airTicks = 6;
                            lift2.judged = true;
                            this.lifts.put(player.getId(), lift2);
                        }
                        lift2.seen = this.tick;
                        this.track(player, lift2);
                    }
                }
                if (this.tick % 100 == 0) {
                    int tick2 = this.tick;
                    this.lifts.values().removeIf(lift -> lift.seen != tick2);
                }
            }
        }
    }

    private void track(AbstractClientPlayerEntity player, JumpCirclesModule.Lift lift) {
        if (player.isOnGround()) {
            lift.x = player.getX();
            lift.y = player.getY();
            lift.z = player.getZ();
            lift.yaw = player.getYaw();
            lift.airTicks = -1;
            lift.judged = false;
        } else if (!lift.judged) {
            lift.airTicks++;
            if (player.getY() - lift.y > 0.2) {
                lift.judged = true;
                if (canLeaveRing(player)) {
                    this.spawn(lift.x, lift.y, lift.z, lift.yaw);
                }
            } else if (lift.airTicks >= 6) {
                lift.judged = true;
            }
        }
    }

    private static boolean canLeaveRing(AbstractClientPlayerEntity player) {
        return !player.hasVehicle()
                && !player.isGliding()
                && !player.isInFluid()
                && !player.isClimbing()
                && player.hurtTime == 0
                && !player.isSpectator()
                && !player.isInvisible();
    }

    public static void renderLevel(CameraRenderState cameraRenderState) {
        JumpCirclesModule module = active;
        if (module != null && module.count > 0 && cameraRenderState.initialized) {
            module.render(cameraRenderState.pos);
        }
    }

    private void render(Vec3d vec) {
        long l = System.nanoTime();
        this.expire(l);
        if (this.count != 0) {
            int i = this.colorMode.is("Custom") ? this.color.argb() : CosmeticAccent.argb();
            int j = i & 16777215;
            float size2 = this.size.get();
            float durationNanos = (float) this.durationNanos();
            float f = 0.025F + 0.02F * size2;
            int towardsWhite = towardsWhite(j, 0.45F);
            Immediate entityVertexConsumers =
                    MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
            if (this.logo.get()) {
                float g = (i >>> 24) / 255.0F;
                VertexConsumer buffer =
                        entityVertexConsumers.getBuffer(JumpCirclesModule.Logo.GLOW_TYPE);
                for (int m = 0; m < this.count; m++) {
                    int k = (this.head + m) % 24;
                    if (this.pose(k, l, durationNanos, size2, vec)) {
                        this.logo(
                                buffer,
                                k,
                                this.poseRadius - f,
                                1.5F,
                                withAlpha(j, g * 0.55F * this.poseFade));
                    }
                }
                entityVertexConsumers.draw(JumpCirclesModule.Logo.GLOW_TYPE);
                buffer = entityVertexConsumers.getBuffer(JumpCirclesModule.Logo.TYPE);
                for (int o = 0; o < this.count; o++) {
                    int n = (this.head + o) % 24;
                    if (this.pose(n, l, durationNanos, size2, vec)) {
                        this.logo(
                                buffer,
                                n,
                                this.poseRadius - f,
                                1.0F,
                                withAlpha(towardsWhite, g * 0.95F * this.poseFade));
                    }
                }
                entityVertexConsumers.draw(JumpCirclesModule.Logo.TYPE);
            }
            VertexConsumer buffer2 = entityVertexConsumers.getBuffer(JumpCirclesModule.Ring.TYPE);
            for (int q = 0; q < this.count; q++) {
                int p = (this.head + q) % 24;
                if (this.pose(p, l, durationNanos, size2, vec)) {
                    float h = (0.08F + 0.08F * size2) * (1.0F - 0.35F * this.poseT);
                    int withAlpha = withAlpha(j, 0.55F * this.poseFade);
                    int withAlpha2 = withAlpha(towardsWhite, 0.95F * this.poseFade);
                    float poseX2 = this.poseX;
                    float poseY2 = this.poseY;
                    float poseZ2 = this.poseZ;
                    float poseRadius2 = this.poseRadius;
                    float f2 = poseRadius2 - f;
                    float g2 = poseRadius2 + f;
                    band(
                            buffer2,
                            poseX2,
                            poseY2,
                            poseZ2,
                            Math.max(0.0F, f2 - h * 0.7F),
                            j,
                            f2,
                            withAlpha);
                    band(buffer2, poseX2, poseY2, poseZ2, f2, withAlpha, poseRadius2, withAlpha2);
                    band(buffer2, poseX2, poseY2, poseZ2, poseRadius2, withAlpha2, g2, withAlpha);
                    band(buffer2, poseX2, poseY2, poseZ2, g2, withAlpha, g2 + h, j);
                }
            }
            entityVertexConsumers.draw(JumpCirclesModule.Ring.TYPE);
        }
    }

    private boolean pose(int i, long l, float f, float g, Vec3d vec) {
        float h = (float) (this.ringX[i] - vec.x);
        float f2 = (float) (this.ringY[i] + 0.015 - vec.y);
        float g2 = (float) (this.ringZ[i] - vec.z);
        if ((double) h * h + (double) f2 * f2 + (double) g2 * g2 > 16384.0) {
            return false;
        } else {
            long l2 = l - this.ringBorn[i];
            float h2 = Math.min(1.0F, (float) l2 / f);
            float f3 = 1.0F - (1.0F - h2) * (1.0F - h2) * (1.0F - h2);
            float g3 = (1.0F - h2) * (1.0F - h2) * Math.min(1.0F, (float) l2 / 8.0E7F);
            if (g3 <= 0.004F) {
                return false;
            } else {
                this.poseX = h;
                this.poseY = f2;
                this.poseZ = g2;
                this.poseT = h2;
                this.poseRadius = g * (0.18F + 0.82F * f3);
                this.poseFade = g3;
                return true;
            }
        }
    }

    private void logo(VertexConsumer vertexConsumer, int i, float f, float g, int j) {
        if (!(f <= 0.0F)) {
            float h = 0.75F * f / 0.9277344F;
            float f2 = this.ringFacingX[i];
            float g2 = this.ringFacingZ[i];
            float h2 = 0.025390625F * h;
            float f3 = this.poseX + f2 * h2;
            float poseY2 = this.poseY;
            float g3 = this.poseZ + g2 * h2;
            float h3;
            float f4 = (f2 + g2) * (h3 = h * g);
            float g4 = (g2 - f2) * h3;
            float h4 = (f2 - g2) * h3;
            float f5 = (g2 + f2) * h3;
            vertexConsumer.vertex(f3 + f4, poseY2, g3 + g4).texture(0.0F, 0.0F).color(j);
            vertexConsumer.vertex(f3 + h4, poseY2, g3 + f5).texture(1.0F, 0.0F).color(j);
            vertexConsumer.vertex(f3 - f4, poseY2, g3 - g4).texture(1.0F, 1.0F).color(j);
            vertexConsumer.vertex(f3 - h4, poseY2, g3 - f5).texture(0.0F, 1.0F).color(j);
        }
    }

    private static void band(
            VertexConsumer vertexConsumer,
            float f,
            float g,
            float h,
            float f2,
            int i,
            float g2,
            int j) {
        byte b = 64;
        for (int n = 0; n < b; n++) {
            int k = n * 64 / b;
            int m = (n + 1) * 64 / b;
            float h2 = COS[k];
            float f3 = SIN[k];
            float g3 = COS[m];
            float h3 = SIN[m];
            vertexConsumer.vertex(f + h2 * f2, g, h + f3 * f2).color(i);
            vertexConsumer.vertex(f + g3 * f2, g, h + h3 * f2).color(i);
            vertexConsumer.vertex(f + g3 * g2, g, h + h3 * g2).color(j);
            vertexConsumer.vertex(f + h2 * g2, g, h + f3 * g2).color(j);
        }
    }

    private static int withAlpha(int i, float f) {
        int j = (int) (Math.max(0.0F, Math.min(1.0F, f)) * 255.0F + 0.5F);
        return j << 24 | i;
    }

    private static int towardsWhite(int i, float f) {
        int j = i >> 16 & 0xFF;
        int k = i >> 8 & 0xFF;
        int m = i & 0xFF;
        j += (int) ((255 - j) * f);
        k += (int) ((255 - k) * f);
        m += (int) ((255 - m) * f);
        return j << 16 | k << 8 | m;
    }

    static {
        for (int i = 0; i <= 64; i++) {
            double d = 6.283185307179586 * i / 64.0;
            COS[i] = (float) Math.cos(d);
            SIN[i] = (float) Math.sin(d);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Lift {

        double x;

        double y;

        double z;

        float yaw;

        int airTicks;

        boolean judged;

        int seen;

        private Lift() {}
    }

    @Environment(EnvType.CLIENT)
    private static final class Logo {

        static final RenderPipeline PIPELINE =
                RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                        .withLocation(Identifier.of("efn", "pipeline/jump_circle_logo"))
                        .withVertexShader("core/position_tex_color")
                        .withFragmentShader("core/position_tex_color")
                        .withSampler("Sampler0")
                        .withBlend(BlendFunction.TRANSLUCENT)
                        .withDepthWrite(false)
                        .withCull(false)
                        .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
                        .build();

        static final RenderLayer TYPE =
                RenderLayer.of(
                        "efn_jump_circle_logo",
                        RenderSetup.builder(PIPELINE)
                                .texture("Sampler0", JumpCirclesModule.LOGO_TEXTURE)
                                .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                                .build());

        static final RenderLayer GLOW_TYPE =
                RenderLayer.of(
                        "efn_jump_circle_logo_glow",
                        RenderSetup.builder(PIPELINE)
                                .texture("Sampler0", JumpCirclesModule.LOGO_GLOW_TEXTURE)
                                .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                                .build());

        private Logo() {}

        static {
            MinecraftClient.getInstance()
                    .getTextureManager()
                    .registerTexture(
                            JumpCirclesModule.LOGO_TEXTURE,
                            (ReloadableTexture)
                                    (new JumpCirclesModule.LogoTexture(
                                            JumpCirclesModule.LOGO_TEXTURE)));
            MinecraftClient.getInstance()
                    .getTextureManager()
                    .registerTexture(
                            JumpCirclesModule.LOGO_GLOW_TEXTURE,
                            (ReloadableTexture)
                                    (new JumpCirclesModule.LogoTexture(
                                            JumpCirclesModule.LOGO_GLOW_TEXTURE)));
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class LogoTexture extends ResourceTexture {

        LogoTexture(Identifier id) {
            super(id);
        }

        @Override
        protected void load(NativeImage image) {
            int i =
                    32
                            - Integer.numberOfLeadingZeros(
                                    Math.min(image.getWidth(), image.getHeight()));
            NativeImage[] mipmapLevelsImages =
                    MipmapHelper.getMipmapLevelsImages(
                            this.getId(),
                            new NativeImage[] {image},
                            i - 1,
                            MipmapStrategy.MEAN,
                            0.0F);
            try {
                GpuDevice device = RenderSystem.getDevice();
                this.close();
                this.glTexture =
                        device.createTexture(
                                () -> this.getId().toString(),
                                5,
                                TextureFormat.RGBA8,
                                image.getWidth(),
                                image.getHeight(),
                                1,
                                i);
                this.glTextureView = device.createTextureView(this.glTexture);
                CommandEncoder commandEncoder = device.createCommandEncoder();
                for (int j = 0; j < i; j++) {
                    NativeImage mipmapLevelsImage = mipmapLevelsImages[j];
                    commandEncoder.writeToTexture(
                            this.glTexture,
                            mipmapLevelsImage,
                            j,
                            0,
                            0,
                            0,
                            mipmapLevelsImage.getWidth(),
                            mipmapLevelsImage.getHeight(),
                            0,
                            0);
                }
            } finally {
                for (int k = 1; k < mipmapLevelsImages.length; k++) {
                    mipmapLevelsImages[k].close();
                }
            }
            this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR, true);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Ring {

        static final RenderLayer TYPE =
                RenderLayer.of(
                        "efn_jump_circles",
                        RenderSetup.builder(RenderPipelines.DEBUG_QUADS)
                                .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                                .build());

        private Ring() {}
    }
}
