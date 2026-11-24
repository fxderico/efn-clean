package net.efn.client.module.impl.movement;

import com.example.client.mixin.AccessorElytraFlyBlockBehaviour;

import net.efn.client.event.EventPriority;
import net.efn.client.event.ListenerHandle;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.PlayerMoveEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.impl.movement.elytrafly.BounceMode;
import net.efn.client.module.impl.movement.elytrafly.ElytraFlightMode;
import net.efn.client.module.impl.movement.elytrafly.ElytraFlightModes;
import net.efn.client.module.impl.movement.elytrafly.ElytraFlyInventory;
import net.efn.client.module.impl.movement.elytrafly.PacketMode;
import net.efn.client.module.impl.movement.elytrafly.Pitch40Mode;
import net.efn.client.module.impl.movement.elytrafly.VanillaMode;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class ElytraFlyModule extends ClientModule {

    public static volatile ElytraFlyModule active;

    private static final float ELYTRA_MAX_REPLACE_DURABILITY = 431.0F;

    public final ModeSetting flightMode =
            this.addSetting(
                    "mode",
                    new ModeSetting("Mode", "Vanilla", "Vanilla", "Packet", "Pitch40", "Bounce"));

    public final BooleanSetting autoTakeOff =
            this.addSetting("auto-take-off", new BooleanSetting("Auto Take Off", false));

    public final NumberSetting fallMultiplier =
            this.addSetting(
                    "fall-multiplier",
                    new NumberSetting("Fall Multiplier", 0.01F, 0.0F, 2.0F).step(0.01F));

    public final NumberSetting horizontalSpeed =
            this.addSetting(
                    "horizontal-speed",
                    new NumberSetting("Horizontal Speed", 1.0F, 0.0F, 10.0F).step(0.1F));

    public final NumberSetting verticalSpeed =
            this.addSetting(
                    "vertical-speed",
                    new NumberSetting("Vertical Speed", 1.0F, 0.0F, 10.0F).step(0.1F));

    public final BooleanSetting acceleration =
            this.addSetting("acceleration", new BooleanSetting("Acceleration", false));

    public final NumberSetting accelerationStep =
            this.addSetting(
                    "acceleration-step",
                    new NumberSetting("Acceleration Step", 1.0F, 0.1F, 5.0F).step(0.1F));

    public final NumberSetting accelerationMin =
            this.addSetting(
                    "acceleration-start",
                    new NumberSetting("Acceleration Start", 0.0F, 0.0F, 10.0F).step(0.1F));

    public final BooleanSetting stopInWater =
            this.addSetting("stop-in-water", new BooleanSetting("Stop In Water", true));

    public final BooleanSetting dontGoIntoUnloadedChunks =
            this.addSetting("no-unloaded-chunks", new BooleanSetting("No Unloaded Chunks", true));

    public final BooleanSetting autoHover =
            this.addSetting("auto-hover", new BooleanSetting("Auto Hover", false));

    public final BooleanSetting noCrash =
            this.addSetting("no-crash", new BooleanSetting("No Crash", false));

    public final NumberSetting crashLookAhead =
            this.addSetting(
                    "crash-look-ahead",
                    new NumberSetting("Crash Look Ahead", 5.0F, 1.0F, 15.0F).step(1.0F));

    private final BooleanSetting instaDrop =
            this.addSetting("insta-drop", new BooleanSetting("Insta Drop", false));

    public final NumberSetting pitch40lowerBounds =
            this.addSetting(
                    "pitch40-lower-bounds",
                    new NumberSetting("Pitch40 Lower Bounds", 180.0F, -128.0F, 360.0F).step(1.0F));

    public final NumberSetting pitch40upperBounds =
            this.addSetting(
                    "pitch40-upper-bounds",
                    new NumberSetting("Pitch40 Upper Bounds", 220.0F, -128.0F, 360.0F).step(1.0F));

    public final NumberSetting pitch40rotationSpeedUp =
            this.addSetting(
                    "pitch40-rotate-speed-up",
                    new NumberSetting("Pitch40 Rotate Speed Up", 5.45F, 1.0F, 20.0F).step(0.05F));

    public final NumberSetting pitch40rotationSpeedDown =
            this.addSetting(
                    "pitch40-rotate-speed-down",
                    new NumberSetting("Pitch40 Rotate Speed Down", 0.9F, 0.5F, 2.0F).step(0.05F));

    public final BooleanSetting autoJump =
            this.addSetting("auto-jump", new BooleanSetting("Auto Jump", true));

    public final ModeSetting yawLockMode =
            this.addSetting(
                    "yaw-lock", new ModeSetting("Yaw Lock", "Smart", "Smart", "Simple", "None"));

    public final NumberSetting yaw =
            this.addSetting("yaw", new NumberSetting("Yaw", 0.0F, 0.0F, 360.0F).step(1.0F));

    public final BooleanSetting lockPitch =
            this.addSetting("pitch-lock", new BooleanSetting("Pitch Lock", true));

    public final NumberSetting pitch =
            this.addSetting("pitch", new NumberSetting("Pitch", 85.0F, 0.0F, 90.0F).step(1.0F));

    public final BooleanSetting restart =
            this.addSetting("restart", new BooleanSetting("Restart", true));

    public final NumberSetting restartDelay =
            this.addSetting(
                    "restart-delay",
                    new NumberSetting("Restart Delay", 7.0F, 0.0F, 20.0F).step(1.0F));

    public final BooleanSetting sprint =
            this.addSetting("sprint-constantly", new BooleanSetting("Sprint Constantly", true));

    public final BooleanSetting manualTakeoff =
            this.addSetting("manual-takeoff", new BooleanSetting("Manual Takeoff", false));

    public final BooleanSetting replace =
            this.addSetting("elytra-replace", new BooleanSetting("Elytra Replace", false));

    public final NumberSetting replaceDurability =
            this.addSetting(
                    "replace-durability",
                    new NumberSetting("Replace Durability", 2.0F, 1.0F, 431.0F).step(1.0F));

    public final ModeSetting chestSwap =
            this.addSetting(
                    "chest-swap",
                    new ModeSetting("Chest Swap", "Never", "Always", "Never", "WaitForGround"));

    public final BooleanSetting autoReplenish =
            this.addSetting(
                    "replenish-fireworks", new BooleanSetting("Replenish Fireworks", false));

    public final NumberSetting replenishSlot =
            this.addSetting(
                    "replenish-slot",
                    new NumberSetting("Replenish Slot", 9.0F, 1.0F, 9.0F).step(1.0F));

    public final BooleanSetting autoPilot =
            this.addSetting("auto-pilot", new BooleanSetting("Auto Pilot", false));

    public final BooleanSetting useFireworks =
            this.addSetting("use-fireworks", new BooleanSetting("Use Fireworks", false));

    public final NumberSetting autoPilotFireworkDelay =
            this.addSetting(
                    "firework-delay",
                    new NumberSetting("Firework Delay", 8.0F, 1.0F, 20.0F).step(0.5F));

    public final NumberSetting autoPilotMinimumHeight =
            this.addSetting(
                    "minimum-height",
                    new NumberSetting("Minimum Height", 120.0F, -128.0F, 260.0F).step(1.0F));

    private ElytraFlightModes modeType = ElytraFlightModes.Vanilla;

    private ElytraFlightMode currentMode;

    private boolean modeRequestedDisable;

    private volatile boolean positionPacketReceived;

    private boolean moveIsVelocity;

    private ClientPlayerEntity recastPlayer;

    private boolean previousGliding;

    private ListenerHandle<PlayerMoveEvent> groundListener;

    private ListenerHandle<TickEvent> instaDropListener;

    public ElytraFlyModule() {
        super(
                "elytra-fly",
                "Elytra Fly",
                "Gives you more control over your elytra.",
                ModuleCategory.MOVEMENT);
        this.usesNotifications();
        this.flightMode.onChange(this::onModeChanged);
        this.autoTakeOff.visibleWhen(this::steerableMode);
        this.fallMultiplier.visibleWhen(this::steerableMode);
        this.horizontalSpeed.visibleWhen(this::steerableMode);
        this.verticalSpeed.visibleWhen(this::steerableMode);
        this.acceleration.visibleWhen(this::steerableMode);
        this.accelerationStep.visibleWhen(() -> this.steerableMode() && this.acceleration.get());
        this.accelerationMin.visibleWhen(() -> this.steerableMode() && this.acceleration.get());
        this.stopInWater.visibleWhen(() -> !this.flightMode.is("Bounce"));
        this.autoHover.visibleWhen(() -> !this.flightMode.is("Bounce"));
        this.noCrash.visibleWhen(() -> !this.flightMode.is("Bounce"));
        this.crashLookAhead.visibleWhen(() -> this.noCrash.get() && !this.flightMode.is("Bounce"));
        this.instaDrop.visibleWhen(() -> !this.flightMode.is("Bounce"));
        this.pitch40lowerBounds.visibleWhen(() -> this.flightMode.is("Pitch40"));
        this.pitch40upperBounds.visibleWhen(() -> this.flightMode.is("Pitch40"));
        this.pitch40rotationSpeedUp.visibleWhen(() -> this.flightMode.is("Pitch40"));
        this.pitch40rotationSpeedDown.visibleWhen(() -> this.flightMode.is("Pitch40"));
        this.autoJump.visibleWhen(() -> this.flightMode.is("Bounce"));
        this.yawLockMode.visibleWhen(() -> this.flightMode.is("Bounce"));
        this.yaw.visibleWhen(() -> this.flightMode.is("Bounce") && this.yawLockMode.is("Simple"));
        this.lockPitch.visibleWhen(() -> this.flightMode.is("Bounce"));
        this.pitch.visibleWhen(() -> this.flightMode.is("Bounce") && this.lockPitch.get());
        this.restart.visibleWhen(() -> this.flightMode.is("Bounce"));
        this.restartDelay.visibleWhen(() -> this.flightMode.is("Bounce") && this.restart.get());
        this.sprint.visibleWhen(() -> this.flightMode.is("Bounce"));
        this.manualTakeoff.visibleWhen(() -> this.flightMode.is("Bounce"));
        this.replaceDurability.visibleWhen(this.replace::get);
        this.replenishSlot.visibleWhen(this.autoReplenish::get);
        this.autoPilot.visibleWhen(this::steerableMode);
        this.useFireworks.visibleWhen(() -> this.autoPilot.get() && this.steerableMode());
        this.autoPilotFireworkDelay.visibleWhen(
                () -> this.useFireworks.get() && this.steerableMode());
        this.autoPilotMinimumHeight.visibleWhen(() -> this.autoPilot.get() && this.steerableMode());
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    private boolean steerableMode() {
        return !this.flightMode.is("Pitch40") && !this.flightMode.is("Bounce");
    }

    public ElytraFlightModes flightModeType() {
        return this.modeType;
    }

    @Override
    protected void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            this.requestDisable();
        } else {
            this.cancelGroundListener();
            this.cancelInstaDropListener();
            this.modeRequestedDisable = false;
            this.positionPacketReceived = false;
            this.moveIsVelocity = false;
            this.recastPlayer = player;
            this.previousGliding = player.isGliding();
            this.modeType = ElytraFlightModes.valueOf(this.flightMode.get());
            this.currentMode = this.createMode(this.modeType);
            this.currentMode.onActivate();
            if (!this.modeRequestedDisable) {
                if ((this.chestSwap.is("Always") || this.chestSwap.is("WaitForGround"))
                        && player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
                    ElytraFlyInventory.chestSwap();
                }
                active = this;
                this.on(
                        PlayerMoveEvent.TYPE,
                        Stage.PRE,
                        EventPriority.HIGHEST,
                        this::captureMoveAliasing);
                this.on(PlayerMoveEvent.TYPE, this::onPlayerMove);
                this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
                this.on(TickEvent.TYPE, Stage.POST, this::onPostTick);
                this.on(PacketSendEvent.TYPE, this::onPacketSend);
                this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceive);
            }
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ElytraFlightMode currentMode2 = this.currentMode;
        this.currentMode = null;
        try {
            if (player != null && currentMode2 != null) {
                if (this.autoPilot.get()) {
                    client.options.forwardKey.setPressed(false);
                }
                if (this.chestSwap.is("Always")
                        && player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                    ElytraFlyInventory.chestSwap();
                } else if (this.chestSwap.is("WaitForGround")) {
                    this.enableGroundListener();
                }
                if (player.isGliding() && this.instaDrop.get()) {
                    this.enableInstaDropListener();
                }
            }
            if (currentMode2 != null) {
                currentMode2.onDeactivate();
            }
        } finally {
            active = null;
            this.recastPlayer = null;
        }
    }

    private void onModeChanged() {
        this.modeType = ElytraFlightModes.valueOf(this.flightMode.get());
        if (this.isEnabled() && this.currentMode != null) {
            this.currentMode = this.createMode(this.modeType);
        }
        if (this.modeType == ElytraFlightModes.Pitch40) {
            this.autoPilot.set(false);
        }
    }

    private ElytraFlightMode createMode(ElytraFlightModes elytraFlightModes) {
        return (ElytraFlightMode)
                (switch (elytraFlightModes) {
                    case Vanilla -> new VanillaMode(this);
                    case Packet -> new PacketMode(this);
                    case Pitch40 -> new Pitch40Mode(this);
                    case Bounce -> new BounceMode(this);
                });
    }

    public void disableFromMode() {
        this.modeRequestedDisable = true;
        this.requestDisable();
    }

    private void onPreTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            ElytraFlightMode currentMode2 = this.currentMode;
            if (currentMode2 != null) {
                if (this.positionPacketReceived) {
                    this.positionPacketReceived = false;
                    currentMode2.zeroAcceleration();
                    currentMode2.onPositionPacket();
                }
                currentMode2.onPreTick();
            }
        } else {
            this.requestDisable();
        }
    }

    private void onPostTick(TickEvent event) {
        ElytraFlightMode currentMode2 = this.currentMode;
        if (MinecraftClient.getInstance().player != null && currentMode2 != null) {
            currentMode2.onTick();
        }
    }

    private void onPacketSend(PacketSendEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ElytraFlightMode currentMode2 = this.currentMode;
        if (client.isOnThread() && client.player != null && currentMode2 != null) {
            currentMode2.onPacketSend(event.packet);
        }
    }

    private void onPacketReceive(PacketReceiveNettyEvent event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            this.positionPacketReceived = true;
        }
    }

    private void captureMoveAliasing(PlayerMoveEvent event) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            this.moveIsVelocity = false;
        } else {
            Vec3d velocity = player.getVelocity();
            this.moveIsVelocity =
                    velocity.x == event.x && velocity.y == event.y && velocity.z == event.z;
        }
    }

    private void onPlayerMove(PlayerMoveEvent event) {
        boolean moveIsVelocity2 = this.moveIsVelocity;
        this.moveIsVelocity = false;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ElytraFlightMode currentMode2 = this.currentMode;
        if (player != null && client.world != null && currentMode2 != null) {
            if (player.getEquippedStack(EquipmentSlot.CHEST).contains(DataComponentTypes.GLIDER)) {
                currentMode2.autoTakeoff();
                boolean bl = this.modeType == ElytraFlightModes.Bounce;
                if (player.isGliding()) {
                    if (!bl) {
                        currentMode2.velX = 0.0;
                        currentMode2.velY = event.y;
                        currentMode2.velZ = 0.0;
                        currentMode2.forward = Vec3d.fromPolar(0.0F, player.getYaw()).multiply(0.1);
                        currentMode2.right =
                                Vec3d.fromPolar(0.0F, player.getYaw() + 90.0F).multiply(0.1);
                        if (player.isTouchingWater() && this.stopInWater.get()) {
                            player.networkHandler.sendPacket(
                                    new ClientCommandC2SPacket(player, Mode.START_FALL_FLYING));
                            return;
                        }
                        currentMode2.handleFallMultiplier();
                        currentMode2.handleAutopilot();
                        currentMode2.handleAcceleration();
                        currentMode2.handleHorizontalSpeed(event);
                        currentMode2.handleVerticalSpeed(event);
                    }
                    if (!bl) {
                        int i = (int) ((player.getX() + currentMode2.velX) / 16.0);
                        int j = (int) ((player.getZ() + currentMode2.velZ) / 16.0);
                        if (this.dontGoIntoUnloadedChunks.get()) {
                            if (client.world.getChunkManager().isChunkLoaded(i, j)) {
                                setMovement(
                                        player,
                                        moveIsVelocity2,
                                        event,
                                        currentMode2.velX,
                                        currentMode2.velY,
                                        currentMode2.velZ);
                            } else {
                                currentMode2.zeroAcceleration();
                                setMovement(
                                        player,
                                        moveIsVelocity2,
                                        event,
                                        0.0,
                                        currentMode2.velY,
                                        0.0);
                            }
                        } else {
                            setMovement(
                                    player,
                                    moveIsVelocity2,
                                    event,
                                    currentMode2.velX,
                                    currentMode2.velY,
                                    currentMode2.velZ);
                        }
                        currentMode2.onPlayerMove();
                    }
                } else if (currentMode2.lastForwardPressed && !bl) {
                    client.options.forwardKey.setPressed(false);
                    currentMode2.lastForwardPressed = false;
                }
                if (this.noCrash.get() && player.isGliding() && !bl) {
                    Vec3d entityPos = player.getEntityPos();
                    Vec3d vec =
                            entityPos.add(
                                    player.getVelocity()
                                            .normalize()
                                            .multiply(this.crashLookAhead.getInt()));
                    RaycastContext raycastContext =
                            new RaycastContext(
                                    entityPos,
                                    new Vec3d(vec.x, player.getY(), vec.z),
                                    ShapeType.COLLIDER,
                                    FluidHandling.NONE,
                                    player);
                    BlockHitResult blockHitResult = client.world.raycast(raycastContext);
                    if (blockHitResult != null && blockHitResult.getType() == Type.BLOCK) {
                        setMovement(player, moveIsVelocity2, event, 0.0, currentMode2.velY, 0.0);
                    }
                }
                if (this.autoHover.get()
                        && player.input.playerInput.sneak()
                        && !isFreecamActive()
                        && player.isGliding()
                        && !bl) {
                    BlockPos pos = player.getBlockPos().down();
                    BlockState blockState = client.world.getBlockState(pos);
                    BlockState blockState2 = client.world.getBlockState(pos.down());
                    boolean bl2 =
                            ((AccessorElytraFlyBlockBehaviour) blockState.getBlock())
                                            .efn$elytraFlyHasCollision()
                                    || !blockState.getFluidState().isEmpty();
                    boolean bl3 =
                            ((AccessorElytraFlyBlockBehaviour) blockState2.getBlock())
                                            .efn$elytraFlyHasCollision()
                                    || !blockState2.getFluidState().isEmpty();
                    if (!bl2 && bl3) {
                        setMovement(
                                player,
                                moveIsVelocity2,
                                event,
                                event.x,
                                -0.10000000149011612,
                                event.z);
                        player.setPitch(
                                MathHelper.clamp(player.getLerpedPitch(0.0F), -50.0F, 20.0F));
                    }
                    if (bl2) {
                        setMovement(
                                player,
                                moveIsVelocity2,
                                event,
                                event.x,
                                -0.029999999329447746,
                                event.z);
                        player.setPitch(
                                MathHelper.clamp(player.getLerpedPitch(0.0F), -50.0F, 20.0F));
                        if (player.getEntityPos().y <= pos.getY() + 1.34F) {
                            setMovement(player, moveIsVelocity2, event, event.x, 0.0, event.z);
                            player.setSneaking(false);
                        }
                    }
                }
            }
        }
    }

    private static void setMovement(
            ClientPlayerEntity player,
            boolean bl,
            PlayerMoveEvent event,
            double d,
            double e,
            double d2) {
        event.x = d;
        event.y = e;
        event.z = d2;
        if (bl) {
            player.setVelocity(d, e, d2);
        }
    }

    public boolean canPacketEfly(ClientPlayerEntity player) {
        return this.modeType == ElytraFlightModes.Packet
                && player.getEquippedStack(EquipmentSlot.CHEST).contains(DataComponentTypes.GLIDER)
                && !player.isOnGround();
    }

    public boolean modifyFallFlying(ClientPlayerEntity recastPlayer2, boolean bl) {
        boolean bl2 = bl || this.canPacketEfly(recastPlayer2);
        if (recastPlayer2 != this.recastPlayer) {
            this.recastPlayer = recastPlayer2;
            this.previousGliding = false;
        }
        boolean previousGliding2 = this.previousGliding;
        this.previousGliding = bl2;
        return previousGliding2 && !bl2 && this.modeType == ElytraFlightModes.Bounce
                ? BounceMode.recastElytra(recastPlayer2)
                : bl2;
    }

    private void enableGroundListener() {
        if (this.groundListener == null) {
            this.groundListener =
                    PlayerMoveEvent.TYPE.listen(
                            this,
                            Stage.PRE,
                            EventPriority.NORMAL,
                            playerMoveEvent -> {
                                ClientPlayerEntity player = MinecraftClient.getInstance().player;
                                if (player != null
                                        && player.isOnGround()
                                        && player.getEquippedStack(EquipmentSlot.CHEST).getItem()
                                                == Items.ELYTRA) {
                                    ElytraFlyInventory.chestSwap();
                                    this.cancelGroundListener();
                                }
                            });
        }
    }

    private void cancelGroundListener() {
        if (this.groundListener != null) {
            this.groundListener.unregister();
            this.groundListener = null;
        }
    }

    private void enableInstaDropListener() {
        if (this.instaDropListener == null) {
            this.instaDropListener =
                    TickEvent.TYPE.listen(
                            this,
                            Stage.POST,
                            EventPriority.NORMAL,
                            tickEvent -> {
                                ClientPlayerEntity player = MinecraftClient.getInstance().player;
                                if (player != null && player.isGliding()) {
                                    player.setVelocity(0.0, 0.0, 0.0);
                                    player.networkHandler.sendPacket(
                                            new OnGroundOnly(true, player.horizontalCollision));
                                } else {
                                    this.cancelInstaDropListener();
                                }
                            });
        }
    }

    private void cancelInstaDropListener() {
        if (this.instaDropListener != null) {
            this.instaDropListener.unregister();
            this.instaDropListener = null;
        }
    }

    private static boolean isFreecamActive() {
        FreecamModule module = ModuleRegistry.get(FreecamModule.class);
        return module != null && module.isEnabled();
    }
}
