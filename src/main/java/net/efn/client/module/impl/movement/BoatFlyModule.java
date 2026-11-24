package net.efn.client.module.impl.movement;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.movement.entitycontrol.EntityControlVehicleHook;
import net.efn.client.module.impl.movement.entitycontrol.VehicleAntiKick;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.JumpingMount;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;

@Environment(EnvType.CLIENT)
public final class BoatFlyModule extends ClientModule {

    public static volatile BoatFlyModule active;

    private final NumberSetting speed =
            this.addSetting("speed", new NumberSetting("Speed", 10.0F, 0.0F, 50.0F).step(0.5F));

    private final NumberSetting verticalSpeed =
            this.addSetting(
                    "vertical-speed",
                    new NumberSetting("Vertical Speed", 6.0F, 0.0F, 20.0F).step(0.5F));

    private final NumberSetting fallSpeed =
            this.addSetting(
                    "fall-speed", new NumberSetting("Fall Speed", 0.0F, 0.0F, 10.0F).step(0.1F));

    private final BooleanSetting antiKick =
            this.addSetting("anti-fly-kick", new BooleanSetting("Anti Fly Kick", true));

    private final NumberSetting delay =
            this.addSetting("delay", new NumberSetting("Delay", 40.0F, 1.0F, 80.0F).step(1.0F));

    private final BooleanSetting cancelServerPackets =
            this.addSetting(
                    "cancel-server-packets", new BooleanSetting("Cancel Server Packets", false));

    private final VehicleAntiKick antiKicker = new VehicleAntiKick();

    public BoatFlyModule() {
        super(
                "boat-fly",
                "Boat Fly",
                "Transforms your boat (or any ridden vehicle) into a plane.",
                ModuleCategory.MOVEMENT);
        this.delay.visibleWhen(this.antiKick::get);
        this.antiKick.onChange(this.antiKicker::forget);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        BoatNoclipModule module = ModuleRegistry.get(BoatNoclipModule.class);
        if (module != null) {
            module.setEnabled(false);
        }
        this.antiKicker.start(this.delay.getInt());
        active = this;
        EntityControlVehicleHook.refresh();
        this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.antiKicker.tick());
        this.on(PacketSendEvent.TYPE, this::onPacketSend);
        this.on(PacketReceiveNettyEvent.TYPE, this::onPacketReceive);
    }

    @Override
    protected void onDisable() {
        active = null;
        EntityControlVehicleHook.refresh();
        this.antiKicker.stop();
    }

    private void onPacketSend(PacketSendEvent event) {
        if (this.antiKick.get()) {
            Packet packet = event.packet;
            VehicleMoveC2SPacket vehicleMovePacket;
            if (event.packet instanceof VehicleMoveC2SPacket
                    && this.antiKicker.onPacketSend(
                            vehicleMovePacket = (VehicleMoveC2SPacket) packet,
                            this.delay.getInt())) {
                event.setCancelled(true);
            }
        }
    }

    private void onPacketReceive(PacketReceiveNettyEvent event) {
        if (this.cancelServerPackets.get() && event.packet instanceof VehicleMoveS2CPacket) {
            event.setCancelled(true);
        }
    }

    public void onVehicleSetback() {
        this.antiKicker.forget();
    }

    public double horizontalSpeed() {
        return this.speed.get();
    }

    public double verticalVelocity() {
        MinecraftClient client = MinecraftClient.getInstance();
        double d = 0.0;
        if (EntityControlVehicleHook.isPhysicallyDown(client.options.jumpKey)) {
            d += this.verticalSpeed.get() / 20.0;
        }
        double e;
        double d2;
        return EntityControlVehicleHook.isPhysicallyDown(client.options.sprintKey)
                ? (e = d - this.verticalSpeed.get() / 20.0)
                : (d2 = d - this.fallSpeed.get() / 20.0);
    }

    public static boolean cancelsMountJump(ClientPlayerEntity player) {
        return active != null && player.getVehicle() instanceof JumpingMount;
    }
}
