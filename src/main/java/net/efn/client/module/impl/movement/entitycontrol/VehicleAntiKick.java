/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.movement.entitycontrol;

import com.example.client.mixin.AccessorBoatFlyServerboundMoveVehiclePacket;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

@Environment(value = EnvType.CLIENT)
public final class VehicleAntiKick {

    private static final double Y_OFFSET = 0.0313;

    private static final long HOLD_NANOS = 50000000L;

    private int delayLeft;

    private double lastPacketY = Double.MAX_VALUE;

    private boolean sentPacket;

    private long dipNanos;

    private Entity trackedVehicle;

    public void start(int delayLeft2) {
        this.delayLeft = delayLeft2;
        this.forget();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        this.trackedVehicle = player != null ? player.getVehicle() : null;
    }

    public void stop() {
        this.trackedVehicle = null;
    }

    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        Entity entity = player != null ? player.getVehicle() : null;
        this.trackVehicle(entity);
        if (this.sentPacket && System.nanoTime() - this.dipNanos >= 50000000L) {
            ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
            if (entity != null && networkHandler != null && this.lastPacketY != Double.MAX_VALUE) {
                VehicleMoveC2SPacket vehicleMovePacket = VehicleMoveC2SPacket.fromVehicle(entity);
                Vec3d vec = vehicleMovePacket.position();
                networkHandler.sendPacket(
                        new VehicleMoveC2SPacket(
                                new Vec3d(vec.x, this.lastPacketY, vec.z),
                                vehicleMovePacket.yaw(),
                                vehicleMovePacket.pitch(),
                                vehicleMovePacket.onGround()));
            }
            this.sentPacket = false;
        }
        if (this.delayLeft > 0) {
            --this.delayLeft;
        }
    }

    public boolean onPacketSend(VehicleMoveC2SPacket vehicleMovePacket, int delayLeft2) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread()) {
            return false;
        }
        ClientPlayerEntity player = client.player;
        Entity entity = player != null ? player.getVehicle() : null;
        Entity entity2 = entity;
        if (entity == null) {
            return false;
        }
        this.trackVehicle(entity);
        Vec3d vec = vehicleMovePacket.position();
        double d = vec.y;
        if (this.sentPacket && System.nanoTime() - this.dipNanos < 50000000L) {
            this.lastPacketY = d;
            return true;
        }
        if (this.delayLeft <= 0
                && !this.sentPacket
                && this.shouldFlyDown(d)
                && EntityControlVehicleHook.isOnAir(entity)
                && !entity.isFlyingVehicle()) {
            ((AccessorBoatFlyServerboundMoveVehiclePacket) ((Object) vehicleMovePacket))
                    .efn$setPosition(new Vec3d(vec.x, this.lastPacketY - 0.0313, vec.z));
            this.sentPacket = true;
            this.dipNanos = System.nanoTime();
            this.delayLeft = delayLeft2;
        }
        this.lastPacketY = d;
        return false;
    }

    public void observe(VehicleMoveC2SPacket vehicleMovePacket) {
        this.lastPacketY = vehicleMovePacket.position().y;
    }

    public boolean isHolding() {
        return this.sentPacket && System.nanoTime() - this.dipNanos < 50000000L;
    }

    public void forget() {
        this.lastPacketY = Double.MAX_VALUE;
        this.sentPacket = false;
    }

    private boolean shouldFlyDown(double d) {
        if (d >= this.lastPacketY) {
            return true;
        }
        return this.lastPacketY - d < 0.0313;
    }

    private void trackVehicle(Entity trackedVehicle2) {
        if (trackedVehicle2 == this.trackedVehicle) {
            return;
        }
        this.trackedVehicle = trackedVehicle2;
        this.forget();
    }
}
