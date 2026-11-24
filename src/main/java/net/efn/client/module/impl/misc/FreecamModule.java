package net.efn.client.module.impl.misc;

import com.example.client.mixin.AccessorMovementClientInput;

import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.movement.AutoWalkModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class FreecamModule extends ClientModule {

    private static final int WALK_FWD = 1;

    private static final int WALK_BACK = 2;

    private static final int WALK_LEFT = 4;

    private static final int WALK_RIGHT = 8;

    private static final int WALK_JUMP = 16;

    private static final int WALK_SNEAK = 32;

    private static final int WALK_SPRINT = 64;

    private static final int WALK_FLAT = 15;

    private static final int LEARN_TICKS = 40;

    private static final int CHUNK_CAP = 32;

    private static final float SCROLL_STEP = 1.2F;

    public static volatile FreecamModule active;

    private final NumberSetting speed =
            this.addSetting("speed", new NumberSetting("Speed", 1.0F, 0.1F, 50.0F).step(0.1F));

    private final NumberSetting verticalSpeed =
            this.addSetting(
                    "vertical-speed", new NumberSetting("Vertical", 1.0F, 0.2F, 5.0F).step(0.05F));

    private final BooleanSetting smoothing =
            this.addSetting("smoothing", new BooleanSetting("Smooth", true));

    private final BooleanSetting showOwnBody =
            this.addSetting("show-own-body", new BooleanSetting("Body", true));

    private final BooleanSetting showHands =
            this.addSetting("show-hands", new BooleanSetting("Hands", true));

    private final BooleanSetting bodyFollowsKeys =
            this.addSetting("body-uses-movement-keys", new BooleanSetting("Keys Move Body", false));

    private final BooleanSetting keepWalking =
            this.addSetting("keep-walking", new BooleanSetting("Keep Walking", true));

    private final BooleanSetting scrollSpeed =
            this.addSetting("scroll-speed", new BooleanSetting("Scroll Speed", true));

    private final BooleanSetting scrollResets =
            this.addSetting("scroll-resets", new BooleanSetting("Reset Scroll Speed", true));

    private final NumberSetting chunkDistance =
            this.addSetting(
                    "chunk-distance", new NumberSetting("Chunks", 12.0F, 2.0F, 32.0F).step(1.0F));

    private Vec3d pos = Vec3d.ZERO;

    private Vec3d prev = Vec3d.ZERO;

    private float yaw;

    private float pitch;

    private boolean flyingBefore;

    private boolean cullBefore;

    private Perspective viewBefore;

    private boolean viewPushed;

    private ChunkPos focus;

    private int radius = -1;

    private boolean waiting;

    private boolean live;

    private int walk;

    private int learn;

    private float scrolled = 1.0F;

    public FreecamModule() {
        super("freecam", "Freecam", "Detached camera.", ModuleCategory.MISC);
        this.scrollResets.visibleWhen(this.scrollSpeed::get);
        this.speed.onChange(() -> this.scrolled = 1.0F);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        this.waiting = true;
        this.live = false;
        this.on(TickEvent.TYPE, this::onTick);
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean live2 = this.live;
        this.waiting = false;
        this.live = false;
        if (active == this) {
            active = null;
        }
        this.walk = 0;
        this.learn = 0;
        this.scrolled = 1.0F;
        if (this.viewPushed) {
            client.options.setPerspective(this.viewBefore);
            this.viewPushed = false;
        }
        if (live2) {
            client.chunkCullingEnabled = this.cullBefore;
            if (client.interactionManager != null) {
                client.interactionManager.cancelBlockBreaking();
            }
            this.releaseChunks(client);
            if (client.player != null) {
                client.player.getAbilities().flying = this.flyingBefore;
            }
        }
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        this.open(client);
        ClientPlayerEntity player = client.player;
        if (this.live && player != null) {
            if (this.learn > 0) {
                this.learn--;
                this.absorb(player, false);
            }
            this.prev = this.pos;
            if (player.getAbilities().creativeMode) {
                player.getAbilities().flying = false;
            }
            this.stepCamera(client.options);
            this.followChunks(client);
        }
    }

    private void open(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (this.waiting && player != null && client.world != null) {
            this.flyingBefore = player.getAbilities().flying;
            this.pos =
                    this.prev =
                            new Vec3d(
                                    player.getX(),
                                    player.getY() + player.getEyeHeight(EntityPose.STANDING),
                                    player.getZ());
            this.yaw = player.getYaw();
            this.pitch = player.getPitch();
            this.viewPushed = false;
            if (this.showOwnBody.get()) {
                this.viewBefore = client.options.getPerspective();
                if (this.viewBefore.isFirstPerson()) {
                    client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                    this.viewPushed = true;
                }
            }
            this.waiting = false;
            this.live = true;
            active = this;
            this.walk = 0;
            this.absorb(player, true);
            this.learn = 40;
            this.focus = null;
            this.radius = -1;
            this.cullBefore = client.chunkCullingEnabled;
            client.chunkCullingEnabled = false;
            this.followChunks(client);
            drive(client);
        }
    }

    private void stepCamera(GameOptions gameOptions) {
        double d =
                (gameOptions.forwardKey.isPressed() ? 1 : 0)
                        - (gameOptions.backKey.isPressed() ? 1 : 0);
        double e =
                (gameOptions.leftKey.isPressed() ? 1 : 0)
                        - (gameOptions.rightKey.isPressed() ? 1 : 0);
        double d2 =
                (gameOptions.jumpKey.isPressed() ? 1 : 0)
                        - (gameOptions.sneakKey.isPressed() ? 1 : 0);
        double e2 = this.flySpeed() * (this.smoothing.get() ? 0.5 : 1.0);
        Vec3d vec = Vec3d.fromPolar(0.0F, this.yaw);
        Vec3d vec2 = new Vec3d(vec.z, 0.0, -vec.x);
        this.pos =
                this.pos.add(
                        (vec.x * d + vec2.x * e) * e2,
                        d2 * e2 * this.verticalSpeed.get(),
                        (vec.z * d + vec2.z * e) * e2);
    }

    private float flySpeed() {
        return !this.scrollResets.get()
                ? this.speed.get()
                : MathHelper.clamp(
                        this.speed.get() * this.scrolled, this.speed.min(), this.speed.max());
    }

    private void absorb(ClientPlayerEntity player, boolean bl) {
        GameOptions options = MinecraftClient.getInstance().options;
        PlayerInput playerInput = player.input.playerInput;
        boolean keepWalking2 = this.keepWalking.get();
        boolean bl2 = keepWalking2 && (bl || this.bodyFollowsKeys.get());
        if (bl2) {
            this.setWalk(1, options.forwardKey.isPressed() || playerInput.forward());
            this.setWalk(2, options.backKey.isPressed() || playerInput.backward());
            this.setWalk(4, options.leftKey.isPressed() || playerInput.left());
            this.setWalk(8, options.rightKey.isPressed() || playerInput.right());
            this.setWalk(16, options.jumpKey.isPressed() || playerInput.jump());
            this.setWalk(
                    32,
                    options.sneakKey.isPressed()
                            || playerInput.sneak()
                            || bl && player.isSneaking());
            this.setWalk(64, options.sprintKey.isPressed() || playerInput.sprint());
        } else if (player.isSneaking()) {
            this.walk |= 32;
        }
        AutoWalkModule module = ModuleRegistry.get(AutoWalkModule.class);
        if (module != null && module.isEnabled()) {
            this.walk |= 1;
        }
        if (keepWalking2) {
            if (player.forwardSpeed > 0.02F) {
                this.walk |= 1;
            }
            if (player.forwardSpeed < -0.02F) {
                this.walk |= 2;
            }
            if (player.sidewaysSpeed > 0.02F) {
                this.walk |= 4;
            }
            if (player.sidewaysSpeed < -0.02F) {
                this.walk |= 8;
            }
            if (!bl || (this.walk & 15) == 0) {
                this.aimFromMotion(player);
            }
        }
    }

    private void setWalk(int walk2, boolean bl) {
        if (bl) {
            this.walk |= walk2;
        }
    }

    private void aimFromMotion(ClientPlayerEntity player) {
        Vec3d velocity = player.getVelocity();
        double d = velocity.x;
        double e = velocity.z;
        if (!(d * d + e * e < 1.0E-8)) {
            Vec3d vec = Vec3d.fromPolar(0.0F, player.getYaw());
            double d2 = d * vec.x + e * vec.z;
            double e2 = e * vec.x - d * vec.z;
            if (Math.abs(d2) >= Math.abs(e2)) {
                if (d2 > 0.01) {
                    this.walk |= 1;
                } else if (d2 < -0.01) {
                    this.walk |= 2;
                }
            } else if (e2 > 0.01) {
                this.walk |= 4;
            } else if (e2 < -0.01) {
                this.walk |= 8;
            }
        }
    }

    public static void drive(MinecraftClient client) {
        FreecamModule module = active;
        if (module != null && client.player != null) {
            module.pushWalk(client);
        }
    }

    private void pushWalk(MinecraftClient client) {
        GameOptions options = client.options;
        boolean bodyFollowsKeys2 = this.bodyFollowsKeys.get();
        boolean bl = this.on(1) || bodyFollowsKeys2 && options.forwardKey.isPressed();
        boolean bl2 = this.on(2) || bodyFollowsKeys2 && options.backKey.isPressed();
        boolean bl3 = this.on(4) || bodyFollowsKeys2 && options.leftKey.isPressed();
        boolean bl4 = this.on(8) || bodyFollowsKeys2 && options.rightKey.isPressed();
        boolean on = this.on(16);
        boolean bl5 = this.on(32) || bodyFollowsKeys2 && options.sneakKey.isPressed();
        boolean bl6 = this.on(64) || bodyFollowsKeys2 && options.sprintKey.isPressed();
        if (bodyFollowsKeys2) {
            if (options.backKey.isPressed()) {
                this.walk &= -2;
            }
            if (options.forwardKey.isPressed()) {
                this.walk &= -3;
            }
            if (options.rightKey.isPressed()) {
                this.walk &= -5;
            }
            if (options.leftKey.isPressed()) {
                this.walk &= -9;
            }
        }
        ClientPlayerEntity player = client.player;
        player.input.playerInput = new PlayerInput(bl, bl2, bl3, bl4, on, bl5, bl6);
        float f = (bl3 ? 1.0F : 0.0F) - (bl4 ? 1.0F : 0.0F);
        float g = (bl ? 1.0F : 0.0F) - (bl2 ? 1.0F : 0.0F);
        ((AccessorMovementClientInput) player.input).efn$setMoveVector(new Vec2f(f, g).normalize());
        player.setSneaking(bl5);
    }

    private boolean on(int i) {
        return (this.walk & i) != 0;
    }

    private void followChunks(MinecraftClient client) {
        if (this.live && client.world != null && client.player != null) {
            ClientChunkManager chunkManager = client.world.getChunkManager();
            ChunkPos chunkPos =
                    new ChunkPos(
                            MathHelper.floor(this.pos.x) >> 4, MathHelper.floor(this.pos.z) >> 4);
            ChunkPos chunkPos2 = client.player.getChunkPos();
            int i =
                    Math.max(
                            Math.abs(chunkPos.x - chunkPos2.x), Math.abs(chunkPos.z - chunkPos2.z));
            int j;
            ChunkPos chunkPos3 =
                    i > (j = MathHelper.clamp(this.chunkDistance.getInt(), 2, 32))
                            ? new ChunkPos(
                                    chunkPos2.x + (chunkPos.x - chunkPos2.x) / 2,
                                    chunkPos2.z + (chunkPos.z - chunkPos2.z) / 2)
                            : chunkPos;
            int k = MathHelper.clamp(Math.max(j, i / 2 + 3), 2, 32);
            if (k != this.radius) {
                chunkManager.updateLoadDistance(k);
                this.radius = k;
            }
            if (this.focus == null || chunkPos3.x != this.focus.x || chunkPos3.z != this.focus.z) {
                chunkManager.setChunkMapCenter(chunkPos3.x, chunkPos3.z);
                this.focus = chunkPos3;
                if (client.worldRenderer != null) {
                    client.worldRenderer.scheduleTerrainUpdate();
                }
            }
        }
    }

    private void releaseChunks(MinecraftClient client) {
        this.focus = null;
        this.radius = -1;
        if (client.world != null) {
            ClientChunkManager chunkManager = client.world.getChunkManager();
            if (client.player != null) {
                ChunkPos chunkPos = client.player.getChunkPos();
                chunkManager.setChunkMapCenter(chunkPos.x, chunkPos.z);
            }
            chunkManager.updateLoadDistance(client.options.getClampedViewDistance());
            if (client.worldRenderer != null) {
                client.worldRenderer.scheduleTerrainUpdate();
            }
        }
    }

    public boolean drawsSelf(Entity entity, PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        return this.showOwnBody.get()
                && client.player != null
                && entity == client.player
                && player == client.player;
    }

    public boolean keepHands() {
        return this.showHands.get();
    }

    public boolean showBody() {
        return this.showOwnBody.get();
    }

    public static boolean scroll(double d) {
        FreecamModule module = active;
        if (module != null && module.scrollSpeed.get() && d != 0.0) {
            float f = d > 0.0 ? 1.2F : 0.8333333F;
            NumberSetting speed2 = module.speed;
            if (module.scrollResets.get()) {
                module.scrolled =
                        MathHelper.clamp(
                                module.scrolled * f,
                                speed2.min() / speed2.get(),
                                speed2.max() / speed2.get());
            } else {
                speed2.set(speed2.get() * f);
            }
            return true;
        } else {
            return false;
        }
    }

    public void turn(double d, double e) {
        this.yaw += (float) d * 0.15F;
        this.pitch = MathHelper.clamp(this.pitch + (float) e * 0.15F, -90.0F, 90.0F);
    }

    public double getX(float f) {
        return MathHelper.lerp((double) f, this.prev.x, this.pos.x);
    }

    public double getY(float f) {
        return MathHelper.lerp((double) f, this.prev.y, this.pos.y);
    }

    public double getZ(float f) {
        return MathHelper.lerp((double) f, this.prev.z, this.pos.z);
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }
}
