package net.efn.client.module.impl.movement;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.movement.speed.SpeedMode;
import net.efn.client.module.impl.movement.speed.StrafeSpeedMode;
import net.efn.client.module.impl.movement.speed.VanillaSpeedMode;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class SpeedModule extends ClientModule {

    public static volatile SpeedModule active;

    public static volatile float timerMultiplier = 1.0F;

    private final ModeSetting mode =
            this.addSetting("mode", new ModeSetting("Mode", "Vanilla", "Strafe", "Vanilla"));

    private final NumberSetting vanillaSpeed =
            this.addSetting(
                    "vanilla-speed",
                    new NumberSetting("Vanilla Speed", 5.6F, 0.0F, 20.0F).step(0.1F));

    private final NumberSetting strafeSpeed =
            this.addSetting(
                    "strafe-speed", new NumberSetting("Strafe Speed", 1.6F, 0.0F, 3.0F).step(0.1F));

    private final BooleanSetting speedLimit =
            this.addSetting("speed-limit", new BooleanSetting("Speed Limit", false));

    private final NumberSetting timer =
            this.addSetting("timer", new NumberSetting("Timer", 1.0F, 0.01F, 10.0F).step(0.01F));

    private final BooleanSetting inLiquids =
            this.addSetting("in-liquids", new BooleanSetting("In Liquids", false));

    private final BooleanSetting whenSneaking =
            this.addSetting("when-sneaking", new BooleanSetting("When Sneaking", false));

    private final BooleanSetting onlyOnGround =
            this.addSetting("only-on-ground", new BooleanSetting("Only On Ground", false));

    private SpeedMode currentMode;

    private volatile boolean rubberband;

    public SpeedModule() {
        super(
                "speed",
                "Speed",
                "Modifies your movement speed when moving on the ground.",
                ModuleCategory.MOVEMENT);
        this.vanillaSpeed.visibleWhen(() -> this.mode.is("Vanilla"));
        this.strafeSpeed.visibleWhen(() -> this.mode.is("Strafe"));
        this.speedLimit.visibleWhen(() -> this.mode.is("Strafe"));
        this.onlyOnGround.visibleWhen(() -> this.mode.is("Vanilla"));
        this.mode.onChange(() -> this.currentMode = this.createMode());
        this.currentMode = this.createMode();
    }

    @Override
    protected void onEnable() {
        this.currentMode = this.createMode();
        this.rubberband = false;
        active = this;
        this.currentMode.onActivate();
        this.on(TickEvent.TYPE, Stage.PRE, this::onPreTick);
        this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceive);
    }

    @Override
    protected void onDisable() {
        active = null;
        timerMultiplier = 1.0F;
        this.currentMode.onDeactivate();
    }

    private SpeedMode createMode() {
        return (SpeedMode)
                (this.mode.is("Strafe") ? new StrafeSpeedMode(this) : new VanillaSpeedMode(this));
    }

    private void onPreTick(TickEvent event) {
        if (this.rubberband) {
            this.rubberband = false;
            this.currentMode.onRubberband();
        }
        ClientPlayerEntity player;
        if ((player = MinecraftClient.getInstance().player) == null) {
            timerMultiplier = 1.0F;
        } else if (!this.stopSpeed(player)) {
            this.currentMode.onTick(player);
        }
    }

    private void onPacketReceive(PacketReceiveNettyEvent event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            this.rubberband = true;
        } else if (event.packet instanceof BundleS2CPacket bundlePacket) {
            for (Packet packet : bundlePacket.getPackets()) {
                if (packet instanceof PlayerPositionLookS2CPacket) {
                    this.rubberband = true;
                    break;
                }
            }
        }
    }

    public Vec3d onSelfMove(ClientPlayerEntity player, Vec3d vec) {
        if (player == MinecraftClient.getInstance().player && !this.stopSpeed(player)) {
            float timer2 = this.timer.get();
            timerMultiplier = timer2 != 1.0F && isMoving(player) ? timer2 : 1.0F;
            Vec3d vec2 = this.currentMode.onMove(player, vec);
            if (vec2 != vec && vec == player.getVelocity()) {
                player.setVelocity(vec2);
            }
            return vec2;
        } else {
            return vec;
        }
    }

    private boolean stopSpeed(ClientPlayerEntity player) {
        if (player.isGliding() || player.isClimbing() || player.getVehicle() != null) {
            return true;
        } else if (!this.whenSneaking.get() && player.isSneaking()) {
            return true;
        } else {
            return this.onlyOnGround.get() && !player.isOnGround() && this.mode.is("Vanilla")
                    ? true
                    : !this.inLiquids.get() && (player.isTouchingWater() || player.isInLava());
        }
    }

    public static boolean isMoving(ClientPlayerEntity player) {
        return player.forwardSpeed != 0.0F || player.sidewaysSpeed != 0.0F;
    }

    public double vanillaSpeed() {
        return this.vanillaSpeed.get();
    }

    public double strafeSpeed() {
        return this.strafeSpeed.get();
    }

    public boolean speedLimit() {
        return this.speedLimit.get();
    }
}
