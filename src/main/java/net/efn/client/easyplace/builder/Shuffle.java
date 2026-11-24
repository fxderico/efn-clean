package net.efn.client.easyplace.builder;

import net.efn.client.easyplace.util.PlacementUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
final class Shuffle {

    private static final double CLOSE = 0.3;

    private static final double SLOW = 0.0025;

    private static final double KEY = 0.12;

    private static final double SLIDE = 1.2;

    private static final int TIME_LIMIT = 60;

    private static final double QUICK_SLOW = 0.0144;

    private final Vec3d spot;

    private final Vec3d look;

    private final double slow;

    private int ticks;

    Shuffle(Vec3d spot2, Vec3d look2, boolean bl) {
        this.spot = spot2;
        this.look = look2;
        this.slow = bl ? 0.0144 : 0.0025;
    }

    boolean quick() {
        return this.slow == 0.0144;
    }

    Vec3d spot() {
        return this.spot;
    }

    Shuffle.Result tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return Shuffle.Result.FAILED;
        } else {
            float[] rotation = PlacementUtil.calcRotation(player.getEyePos(), this.look);
            Aim.turn(client, rotation[0], rotation[1], 22.0F);
            double d = this.spot.x - player.getX();
            double e = this.spot.z - player.getZ();
            Vec3d velocity = player.getVelocity();
            double d2 = Math.hypot(d, e);
            if (d2 < 0.3 && velocity.horizontalLengthSquared() < this.slow) {
                this.release(client);
                return Shuffle.Result.DONE;
            } else if (++this.ticks > 60) {
                this.release(client);
                return d2 < 0.45 ? Shuffle.Result.DONE : Shuffle.Result.FAILED;
            } else {
                double e2 = Math.toRadians(player.getYaw());
                double d3 = Math.sin(e2);
                double e3 = Math.cos(e2);
                double d4 = -d3 * d + e3 * e - 1.2 * (-d3 * velocity.x + e3 * velocity.z);
                double e4 = e3 * d + d3 * e - 1.2 * (e3 * velocity.x + d3 * velocity.z);
                client.options.forwardKey.setPressed(d4 > 0.12);
                client.options.backKey.setPressed(d4 < -0.12);
                client.options.leftKey.setPressed(e4 > 0.12);
                client.options.rightKey.setPressed(e4 < -0.12);
                return Shuffle.Result.RUNNING;
            }
        }
    }

    void release(MinecraftClient client) {
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
    }

    @Environment(EnvType.CLIENT)
    static enum Result {
        RUNNING,
        DONE,
        FAILED;

        private Result() {}
    }
}
