package net.efn.client.easyplace.builder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
final class Hop {

    private static final int TIME_LIMIT = 60;

    private static final double CENTERED = 0.15;

    private static final double STEP = 0.55;

    private static final double LINED_UP = 0.25;

    private static final double LEAP_AT = 0.3;

    private static final double LANDED = 0.55;

    private static final int SAFE_DROP = 3;

    private static final float TURN = 22.0F;

    private static final float WALK_OFF = 30.0F;

    private static final double JUMP_AT = 0.15;

    private final BlockPos spot;

    private final boolean through;

    private final boolean leap;

    private boolean sprint;

    private boolean careful;

    private boolean slow;

    private BlockPos from;

    private double floor = 0.0 / 0.0;

    private boolean centered;

    private int ticks;

    Hop(BlockPos pos) {
        this(pos, false, false);
    }

    Hop(BlockPos pos, boolean bl) {
        this(pos, bl, false);
    }

    Hop(BlockPos spot2, boolean through2, boolean leap2) {
        this.spot = spot2;
        this.through = through2;
        this.leap = leap2;
    }

    BlockPos spot() {
        return this.spot;
    }

    Hop sprinting() {
        this.sprint = true;
        return this;
    }

    Hop careful() {
        this.careful = true;
        this.sprint = false;
        return this;
    }

    Hop slow() {
        this.slow = true;
        this.sprint = false;
        return this;
    }

    static BlockPos spot(
            ClientWorld world, ClientPlayerEntity player, Predicate<BlockPos> predicate) {
        if (!player.isOnGround()) {
            return null;
        } else {
            BlockPos pos = player.getBlockPos();
            double floor2 = Walker.floorOr(world, pos);
            BlockPos pos2 = null;
            double d = 0.45;
            for (BlockPos pos3 : Walker.steps(world, pos)) {
                double e = Walker.floor(world, pos3) - floor2;
                if (e > d && !predicate.test(pos3)) {
                    d = e;
                    pos2 = pos3;
                }
            }
            return pos2;
        }
    }

    Hop.Result tick(MinecraftClient client) {
        float f = 0.0F;
        boolean bl = false;
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return Hop.Result.FAILED;
        } else {
            if (this.from == null) {
                this.from = player.getBlockPos();
            }
            if (Double.isNaN(this.floor) && client.world != null) {
                this.floor = Walker.floorOr(client.world, this.spot);
            }
            Vec3d entityPos = player.getEntityPos();
            client.options.sneakKey.setPressed(this.careful);
            double d = this.spot.getX() + 0.5 - entityPos.x;
            double e = this.spot.getZ() + 0.5 - entityPos.z;
            double d2 = this.through ? 0.35 : 0.12;
            boolean bl2 = Math.abs(d) < d2 && Math.abs(e) < d2;
            boolean bl3 = this.through || player.getVelocity().horizontalLengthSquared() < 0.0025;
            boolean bl4 = entityPos.y < this.floor + 0.55;
            if (bl2 && bl3 && bl4 && player.isOnGround() && entityPos.y >= this.floor - 0.05) {
                if (!this.through) {
                    this.release(client);
                }
                return Hop.Result.DONE;
            } else if (++this.ticks > 60) {
                this.release(client);
                return Hop.Result.FAILED;
            } else if (client.world != null
                    && player.isOnGround()
                    && this.intoHole(client.world, player, entityPos)) {
                client.options.forwardKey.setPressed(false);
                client.options.sprintKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                client.options.sneakKey.setPressed(true);
                return Hop.Result.RUNNING;
            } else {
                if (!this.centered) {
                    double e2 = this.from.getX() + 0.5 - entityPos.x;
                    double d3 = this.from.getZ() + 0.5 - entityPos.z;
                    double e3 = this.spot.getX() != this.from.getX() ? Math.abs(d3) : Math.abs(e2);
                    boolean bl5 = this.floor - entityPos.y <= 0.55 && e3 < 0.25;
                    if (!bl5
                            && (!(Math.abs(e2) < 0.15) || !(Math.abs(d3) < 0.15))
                            && (player.isOnGround() || this.ticks <= 10)) {
                        f =
                                Aim.turn(
                                        client,
                                        (float) Math.toDegrees(Math.atan2(-e2, d3)),
                                        player.getPitch(),
                                        22.0F);
                        client.options.forwardKey.setPressed(f < 30.0F);
                        client.options.jumpKey.setPressed(false);
                        return Hop.Result.RUNNING;
                    }
                    this.centered = true;
                }
                f =
                        Aim.turn(
                                client,
                                (float) Math.toDegrees(Math.atan2(-d, e)),
                                player.getPitch(),
                                22.0F);
                boolean bl6 = player.getVelocity().horizontalLengthSquared() > 4.0E-4;
                client.options.sprintKey.setPressed(this.sprint && !bl2 && f < 10.0F);
                bl = (!bl2 || !bl4) && (this.through || Math.hypot(d, e) > 0.3 || !bl6 || !bl4);
                if (this.slow) {
                    boolean bl7 =
                            player.isOnGround()
                                    && entityPos.y < this.floor + 0.05
                                    && Math.hypot(d, e) < 0.45;
                    boolean bl8;
                    if (bl) {
                        double horizontalLength = player.getVelocity().horizontalLength();
                        double d4 = bl7 ? 0.03 : 0.08;
                        bl8 = horizontalLength < d4;
                    } else {
                        bl8 = false;
                    }
                    bl = bl8;
                }
                client.options.forwardKey.setPressed(bl && f < 30.0F);
                double e4 =
                        (entityPos.x - (this.from.getX() + 0.5))
                                        * Integer.signum(this.spot.getX() - this.from.getX())
                                + (entityPos.z - (this.from.getZ() + 0.5))
                                        * Integer.signum(this.spot.getZ() - this.from.getZ());
                boolean bl9 =
                        client.player.horizontalCollision
                                || e4 > 0.15
                                || this.spot.getX() == this.from.getX()
                                        && this.spot.getZ() == this.from.getZ();
                client.options.jumpKey.setPressed(
                        client.player.isOnGround() && this.floor - entityPos.y > 0.55 && bl9
                                || this.leap && client.player.isOnGround() && e4 > 0.3 && e4 < 1.0);
                return Hop.Result.RUNNING;
            }
        }
    }

    private boolean intoHole(ClientWorld world, ClientPlayerEntity player, Vec3d vec) {
        Vec3d velocity = player.getVelocity();
        double horizontalLength = velocity.horizontalLength();
        if (horizontalLength < 0.02) {
            return false;
        } else {
            double d = horizontalLength * 1.3 + 0.35;
            BlockPos pos =
                    BlockPos.ofFloored(
                            vec.x + velocity.x / horizontalLength * d,
                            vec.y + 0.01,
                            vec.z + velocity.z / horizontalLength * d);
            BlockPos pos2 = player.getBlockPos();
            return (pos.getX() != this.spot.getX() || pos.getZ() != this.spot.getZ())
                            && (pos.getX() != pos2.getX() || pos.getZ() != pos2.getZ())
                    ? Walker.dropAt(world, pos) > 3
                    : false;
        }
    }

    void release(MinecraftClient client) {
        client.options.forwardKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        if (this.careful) {
            client.options.sneakKey.setPressed(false);
        }
    }

    @Environment(EnvType.CLIENT)
    static enum Result {
        RUNNING,
        DONE,
        FAILED;

        private Result() {}
    }
}
