package net.efn.client.module.impl.movement.speed;

import net.efn.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class StrafeSpeedMode extends SpeedMode {

    private long timer = 0L;

    public StrafeSpeedMode(SpeedModule module) {
        super(module);
    }

    @Override
    public Vec3d onMove(ClientPlayerEntity player, Vec3d vec) {
        boolean moving = SpeedModule.isMoving(player);
        double d = vec.y;
        switch (this.stage) {
            case 0:
                if (moving) {
                    this.stage++;
                    this.speed = 1.1799999475479126 * this.getDefaultSpeed(player) - 0.01;
                }
            case 1:
                if (moving && player.isOnGround()) {
                    d = this.getHop(player, 0.40123128);
                    this.speed = this.speed * this.settings.strafeSpeed();
                    this.stage++;
                }
                break;
            case 2:
                this.speed = this.distance - 0.76 * (this.distance - this.getDefaultSpeed(player));
                this.stage++;
                break;
            case 3:
                if (!player.getEntityWorld()
                                .isSpaceEmpty(
                                        player.getBoundingBox()
                                                .offset(0.0, player.getVelocity().y, 0.0))
                        || player.verticalCollision && this.stage > 0) {
                    this.stage = 0;
                }
                this.speed = this.distance - this.distance / 159.0;
        }
        this.speed = Math.max(this.speed, this.getDefaultSpeed(player));
        if (this.settings.speedLimit()) {
            if (System.currentTimeMillis() - this.timer > 2500L) {
                this.timer = System.currentTimeMillis();
            }
            this.speed =
                    Math.min(
                            this.speed,
                            System.currentTimeMillis() - this.timer > 1250L ? 0.44 : 0.43);
        }
        double[] doubles = transformStrafe(player, this.speed);
        return new Vec3d(doubles[0], d, doubles[1]);
    }

    private static double[] transformStrafe(ClientPlayerEntity player, double d) {
        Vec2f movementInput = player.input.getMovementInput();
        float f = Math.signum(movementInput.y);
        float g = Math.signum(movementInput.x);
        float lerpedYaw =
                player.getLerpedYaw(
                        MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true));
        if (f == 0.0F && g == 0.0F) {
            return new double[2];
        } else {
            float h = 90.0F * g;
            if (f != 0.0F) {
                h *= f * 0.5F;
            }
            lerpedYaw -= h;
            if (f < 0.0F) {
                lerpedYaw -= 180.0F;
            }
            double e = Math.toRadians(lerpedYaw);
            return new double[] {-Math.sin(e) * d, Math.cos(e) * d};
        }
    }

    @Override
    public void onTick(ClientPlayerEntity player) {
        double d = player.getX() - player.lastX;
        double e = player.getZ() - player.lastZ;
        this.distance = Math.sqrt(d * d + e * e);
    }
}
