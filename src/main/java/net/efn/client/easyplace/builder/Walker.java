package net.efn.client.easyplace.builder;

import net.efn.client.easyplace.compat.BaritoneCompat;
import net.efn.client.easyplace.util.StateRules;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
final class Walker {

    private static final int STILL_LIMIT = 60;

    private static final int GRACE = 40;

    private static final int TIME_LIMIT = 600;

    private static final double EYE = 1.62;

    private static final double HEIGHT = 1.8;

    private static final double JUMP = 1.2;

    private static final float SWIM_DEPTH = 0.4F;

    private static final double FULL = 0.999;

    private static final double STEP_UP = 0.55;

    private static final double MAX_DROP = 3.0;

    private static final double LEAP_ROOM = 2.6;

    private BlockPos goal;

    private long started;

    private long lastMove;

    private Vec3d lastPos;

    Walker() {}

    boolean active() {
        return this.goal != null;
    }

    BlockPos goal() {
        return this.goal;
    }

    void go(ClientPlayerEntity player, BlockPos goal2, long lastMove2) {
        this.goal = goal2;
        this.started = lastMove2;
        this.lastMove = lastMove2;
        this.lastPos = player.getEntityPos();
        BaritoneCompat.goTo(goal2);
    }

    Walker.Result tick(ClientPlayerEntity player, long lastMove2) {
        if (this.goal == null) {
            return Walker.Result.FAILED;
        } else {
            Vec3d entityPos = player.getEntityPos();
            double d = entityPos.x - (this.goal.getX() + 0.5);
            double e;
            if (d * d + (e = entityPos.z - (this.goal.getZ() + 0.5)) * e < 0.36
                    && Math.abs(entityPos.y - this.goal.getY()) < 0.6
                    && player.isOnGround()) {
                this.goal = null;
                return Walker.Result.ARRIVED;
            } else {
                if (entityPos.squaredDistanceTo(this.lastPos) > 0.0025) {
                    this.lastPos = entityPos;
                    this.lastMove = lastMove2;
                }
                if ((lastMove2 - this.started <= 40L || lastMove2 - this.lastMove <= 60L)
                        && lastMove2 - this.started <= 600L) {
                    return Walker.Result.RUNNING;
                } else {
                    this.stop();
                    return Walker.Result.FAILED;
                }
            }
        }
    }

    void stop() {
        if (this.goal != null) {
            BaritoneCompat.cancel();
        }
        this.goal = null;
    }

    static BlockPos standFor(
            ClientWorld world,
            ClientPlayerEntity player,
            BlockPos pos,
            BlockState state,
            Predicate<BlockPos> predicate) {
        return standFor(
                world, player, pos, state, predicate, player.getBlockInteractionRange() - 0.7);
    }

    static BlockPos standFor(
            ClientWorld world,
            ClientPlayerEntity player,
            BlockPos pos,
            BlockState state,
            Predicate<BlockPos> predicate,
            double d) {
        Direction direction = StateRules.lookFor(state);
        Direction direction2 = StateRules.flatLookFor(state);
        Vec3d entityPos = player.getEntityPos();
        int i = player.getBlockPos().getY();
        BlockPos pos2 = null;
        double e = 1.7976931348623157E308;
        for (int j = -1; j <= 2; j++) {
            for (int k = -3; k <= 3; k++) {
                for (int m = -3; m <= 3; m++) {
                    BlockPos pos3 = pos.add(k, j, m);
                    if (!pos3.equals(pos)
                            && !pos3.up().equals(pos)
                            && !predicate.test(pos3)
                            && standable(world, pos3)) {
                        Vec3d vec =
                                new Vec3d(
                                        pos3.getX() + 0.5,
                                        floor(world, pos3) + 1.62,
                                        pos3.getZ() + 0.5);
                        Vec3d vec2 = pos.toCenterPos().subtract(vec);
                        if (!(vec2.length() > d)
                                && (direction == null || dominant(vec2, direction))
                                && (direction2 == null || facingFlat(vec2, direction2))) {
                            double d2 =
                                    Math.hypot(
                                            entityPos.x - (pos3.getX() + 0.5),
                                            entityPos.z - (pos3.getZ() + 0.5));
                            int n;
                            double e2 =
                                    d2
                                            + ((n = pos3.getY() - i) > 0 ? n * 4.0 : -n * 0.3)
                                            + Math.abs(j) * 0.2;
                            if (e2 < e) {
                                e = e2;
                                pos2 = pos3;
                            }
                        }
                    }
                }
            }
        }
        return pos2;
    }

    static BlockPos stepToward(
            ClientWorld world,
            ClientPlayerEntity player,
            BlockPos pos,
            Predicate<BlockPos> predicate) {
        if (!player.isOnGround()) {
            return null;
        } else {
            BlockPos pos2 = player.getBlockPos();
            Vec3d centerPos = pos.toCenterPos();
            double distance = player.getEyePos().distanceTo(centerPos);
            BlockPos pos3 = null;
            double d = distance - 0.3;
            for (BlockPos pos4 : steps(world, pos2)) {
                double distance2;
                if (!predicate.test(pos4)
                        && (distance2 =
                                        new Vec3d(
                                                        pos4.getX() + 0.5,
                                                        floor(world, pos4) + 1.62,
                                                        pos4.getZ() + 0.5)
                                                .distanceTo(centerPos))
                                < d) {
                    d = distance2;
                    pos3 = pos4;
                }
            }
            return pos3;
        }
    }

    static List<BlockPos> steps(ClientWorld world, BlockPos pos) {
        return steps(world, pos, true);
    }

    private static List<BlockPos> steps(ClientWorld world, BlockPos pos, boolean bl) {
        ArrayList list = new ArrayList();
        double floor = floorOr(world, pos);
        for (Direction direction : Type.HORIZONTAL) {
            BlockPos pos2 = pos.offset(direction);
            for (int i = 1; i >= -3; i--) {
                BlockPos pos3 = pos2.up(i);
                double d = floor(world, pos3);
                if (!Double.isNaN(d)) {
                    boolean bl2 = floor - d > 0.6 && !world.getFluidState(pos3).isEmpty();
                    boolean bl3 = bl && floor - d > 0.01 && steps(world, pos3, false).isEmpty();
                    if (d - floor <= 1.2
                            && floor - d <= 3.0
                            && !bl2
                            && !bl3
                            && !nearLava(world, pos3)
                            && passage(world, pos, floor, pos3, d)) {
                        list.add(pos3);
                    }
                    break;
                }
            }
        }
        return list;
    }

    static String explain(ClientWorld world, BlockPos pos) {
        StringBuilder sb =
                new StringBuilder(
                        "from " + pos.toShortString() + " floor " + floor(world, pos) + ":");
        double floor = floorOr(world, pos);
        for (Direction direction : Type.HORIZONTAL) {
            BlockPos pos2 = pos.offset(direction);
            sb.append(" ").append(direction).append("[");
            for (int i = 1; i >= -3; i--) {
                BlockPos pos3 = pos2.up(i);
                double d = floor(world, pos3);
                sb.append(i)
                        .append(":")
                        .append(
                                Double.isNaN(d)
                                        ? "-"
                                        : String.format(
                                                "%.2f%s",
                                                d,
                                                d - floor <= 1.2
                                                                && passage(
                                                                        world, pos, floor, pos3, d)
                                                        ? "ok"
                                                        : "x"))
                        .append(" ");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    static BlockPos fall(ClientWorld world, BlockPos pos, Direction direction, double d) {
        double floor = floorOr(world, pos);
        BlockPos pos2 = pos.offset(direction);
        if (Double.isNaN(floor(world, pos2)) && roomAt(world, pos2)) {
            for (int i = 1; i <= Math.ceil(d) + 1.0; i++) {
                BlockPos pos3 = pos2.down(i);
                double e = floor(world, pos3);
                if (!world.getFluidState(pos3).isEmpty()) {
                    return null;
                }
                if (!Double.isNaN(e)) {
                    if (!(floor - e <= 3.0)
                            && !(floor - e > d)
                            && !nearLava(world, pos3)
                            && passage(world, pos, floor, pos3, e)) {
                        return pos3;
                    }
                    return null;
                }
                if (!world.getBlockState(pos3).getCollisionShape(world, pos3).isEmpty()) {
                    return null;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    static double floor(ClientWorld world, BlockPos pos) {
        if (!world.isChunkLoaded(pos)) {
            return 0.0 / 0.0;
        } else if (world.getFluidState(pos).isIn(FluidTags.WATER)
                && world.getFluidState(pos).getHeight(world, pos) > 0.4F) {
            return 0.0 / 0.0;
        } else {
            double topUnderFoot = topUnderFoot(world, pos);
            double d;
            if (!Double.isNaN(topUnderFoot)) {
                if (topUnderFoot > 0.999) {
                    return 0.0 / 0.0;
                }
                d = pos.getY() + topUnderFoot;
            } else {
                if (world.getFluidState(pos).isIn(FluidTags.LAVA)
                        || world.getBlockState(pos).isOf(Blocks.BUBBLE_COLUMN)) {
                    return 0.0 / 0.0;
                }
                BlockPos pos2 = pos.down();
                double topUnderFoot2 = topUnderFoot(world, pos2);
                if (Double.isNaN(topUnderFoot2) || topUnderFoot2 <= 0.999) {
                    return 0.0 / 0.0;
                }
                d = pos2.getY() + topUnderFoot2;
            }
            if (!world.getFluidState(pos.up()).isEmpty()) {
                return 0.0 / 0.0;
            } else {
                double e = pos.getX();
                double d2;
                if (!world.isSpaceEmpty(
                        new Box(
                                e + 0.2,
                                d + 0.01,
                                (d2 = pos.getZ()) + 0.2,
                                e + 0.8,
                                d + 1.8,
                                d2 + 0.8))) {
                    return 0.0 / 0.0;
                } else {
                    return world.isSpaceEmpty(
                                    new Box(
                                            e + 0.2, d - 0.05, d2 + 0.2, e + 0.8, d - 0.001,
                                            d2 + 0.8))
                            ? 0.0 / 0.0
                            : d;
                }
            }
        }
    }

    static boolean roomAt(ClientWorld world, BlockPos pos) {
        if (world.isChunkLoaded(pos)
                && world.getFluidState(pos).isEmpty()
                && world.getFluidState(pos.up()).isEmpty()) {
            double d = pos.getX();
            double e = pos.getY();
            double d2 = pos.getZ();
            return world.isSpaceEmpty(
                    new Box(d + 0.2, e + 0.01, d2 + 0.2, d + 0.8, e + 1.8, d2 + 0.8));
        } else {
            return false;
        }
    }

    static boolean leap(ClientWorld world, BlockPos pos, double d, Direction direction) {
        return leap(world, pos, d, direction, 1);
    }

    static boolean leap(ClientWorld world, BlockPos pos, double d, Direction direction, int i) {
        BlockPos pos2 = pos.offset(direction, i + 1);
        double e = floor(world, pos2);
        if (!Double.isNaN(e) && !(Math.abs(e - d) > 0.1)) {
            for (int j = 1; j <= i; j++) {
                BlockPos pos3 = pos.offset(direction, j);
                if (!Double.isNaN(floor(world, pos3)) || !world.getFluidState(pos3).isEmpty()) {
                    return false;
                }
                if (!safeFall(world, pos3)) {
                    return false;
                }
                for (Direction direction2 :
                        new Direction[] {
                            direction.rotateYClockwise(), direction.rotateYCounterclockwise()
                        }) {
                    BlockPos pos4 = pos3.offset(direction2);
                    if (Double.isNaN(floor(world, pos4)) && !safeFall(world, pos4)) {
                        return false;
                    }
                }
            }
            BlockPos pos5 = pos2.offset(direction);
            if (Double.isNaN(floor(world, pos5)) && !safeFall(world, pos5)) {
                return false;
            } else {
                Box box = column(pos, d, d).union(column(pos2, d, d)).withMaxY(d + 2.6);
                return world.isSpaceEmpty(box);
            }
        } else {
            return false;
        }
    }

    private static boolean safeFall(ClientWorld world, BlockPos pos) {
        for (int i = 1; i <= 3; i++) {
            BlockPos pos2 = pos.down(i);
            if (world.getFluidState(pos2).isIn(FluidTags.LAVA)
                    || world.getBlockState(pos2).isIn(BlockTags.FIRE)) {
                return false;
            }
            if (!world.getBlockState(pos2).getCollisionShape(world, pos2).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static double topUnderFoot(ClientWorld world, BlockPos pos) {
        VoxelShape collisionShape = world.getBlockState(pos).getCollisionShape(world, pos);
        if (collisionShape.isEmpty()) {
            return 0.0 / 0.0;
        } else {
            double d = 0.0 / 0.0;
            for (Box boundingBox : collisionShape.getBoundingBoxes()) {
                if (!(boundingBox.maxX <= 0.2)
                        && !(boundingBox.minX >= 0.8)
                        && !(boundingBox.maxZ <= 0.2)
                        && !(boundingBox.minZ >= 0.8)) {
                    d = Double.isNaN(d) ? boundingBox.maxY : Math.max(d, boundingBox.maxY);
                }
            }
            return d;
        }
    }

    static boolean risky(ClientWorld world, BlockPos pos) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos pos2;
                if ((i != 0 || j != 0) && Double.isNaN(floor(world, pos2 = pos.add(i, 0, j)))) {
                    boolean bl = i == 0 || j == 0;
                    for (int k = 0; k <= 24; k++) {
                        BlockPos pos3 = pos2.down(k);
                        if (world.getFluidState(pos3).isIn(FluidTags.LAVA)
                                || world.getBlockState(pos3).isIn(BlockTags.FIRE)) {
                            if (k <= 4) {
                                return true;
                            }
                            break;
                        }
                        boolean bl2 = k > (bl ? 4 : 8);
                        if (world.getFluidState(pos3).isIn(FluidTags.WATER)) {
                            if (bl2) {
                                return true;
                            }
                            break;
                        }
                        if (!world.getBlockState(pos3).getCollisionShape(world, pos3).isEmpty()) {
                            if (bl2) {
                                return true;
                            }
                            break;
                        }
                        if (k == 24) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    static boolean icy(ClientWorld world, BlockPos pos) {
        return world.getBlockState(pos.down()).isIn(BlockTags.ICE)
                || world.getBlockState(pos).isIn(BlockTags.ICE);
    }

    static int dropAt(ClientWorld world, BlockPos pos) {
        if (!world.isChunkLoaded(pos)) {
            return 0;
        } else if (world.getFluidState(pos).isIn(FluidTags.LAVA)) {
            return 24;
        } else if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
            return 0;
        } else {
            int i = 0;
            while (i < 24) {
                BlockPos pos2 = pos.down(i + 1);
                if (!world.getFluidState(pos2).isIn(FluidTags.LAVA)
                        && !world.getBlockState(pos2).isIn(BlockTags.FIRE)) {
                    if (world.getFluidState(pos2).isEmpty()
                            && world.getBlockState(pos2).getCollisionShape(world, pos2).isEmpty()) {
                        i++;
                        continue;
                    }
                    return i;
                }
                return 24;
            }
            return 24;
        }
    }

    static boolean nearLava(ClientWorld world, BlockPos pos) {
        for (BlockPos pos2 :
                new BlockPos[] {
                    pos, pos.up(), pos.down(), pos.north(), pos.south(), pos.east(), pos.west()
                }) {
            if (world.getFluidState(pos2).isIn(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    static double floorOr(ClientWorld world, BlockPos pos) {
        double d = floor(world, pos);
        return Double.isNaN(d) ? pos.getY() : d;
    }

    private static boolean passage(
            ClientWorld world, BlockPos pos, double d, BlockPos pos2, double e) {
        double d2 = Math.max(d, e);
        Box box = column(pos, d, d2);
        Box box2 = column(pos2, e, d2);
        Box box3 =
                new Box(
                        Math.min(box.minX, box2.minX),
                        d2 + 0.55,
                        Math.min(box.minZ, box2.minZ),
                        Math.max(box.maxX, box2.maxX),
                        d2 + 1.8,
                        Math.max(box.maxZ, box2.maxZ));
        return world.isSpaceEmpty(box) && world.isSpaceEmpty(box2) && world.isSpaceEmpty(box3);
    }

    private static Box column(BlockPos pos, double d, double e) {
        return new Box(
                pos.getX() + 0.2,
                d + 0.01,
                pos.getZ() + 0.2,
                pos.getX() + 0.8,
                e + 1.8,
                pos.getZ() + 0.8);
    }

    static BlockPos stepAside(
            ClientWorld world, ClientPlayerEntity player, Predicate<BlockPos> predicate) {
        BlockPos pos = player.getBlockPos();
        BlockPos pos2 = null;
        double d = 1.7976931348623157E308;
        for (int i = -1; i <= 1; i++) {
            for (int j = -2; j <= 2; j++) {
                for (int k = -2; k <= 2; k++) {
                    double e;
                    BlockPos pos3;
                    if ((j != 0 || k != 0)
                            && !predicate.test(pos3 = pos.add(j, i, k))
                            && standable(world, pos3)
                            && (e = Math.abs(j) + Math.abs(k) + Math.abs(i) * 1.5) < d) {
                        d = e;
                        pos2 = pos3;
                    }
                }
            }
        }
        return pos2;
    }

    static boolean standable(ClientWorld world, BlockPos pos) {
        return !Double.isNaN(floor(world, pos));
    }

    static boolean facingFlat(Vec3d vec, Direction direction) {
        double d = vec.x * direction.getOffsetX() + vec.z * direction.getOffsetZ();
        return d
                > Math.abs(vec.x * direction.getOffsetZ())
                        + Math.abs(vec.z * direction.getOffsetX())
                        + 0.1;
    }

    static boolean dominant(Vec3d vec, Direction direction) {
        return dominant(vec, direction, 0.2);
    }

    static boolean dominant(Vec3d vec, Direction direction, double d) {
        double e =
                vec.x * direction.getOffsetX()
                        + vec.y * direction.getOffsetY()
                        + vec.z * direction.getOffsetZ();
        if (e <= 0.0) {
            return false;
        } else {
            double d2 = Math.abs(vec.x);
            double e2 = Math.abs(vec.y);
            double d3 = Math.abs(vec.z);
            return switch (direction.getAxis()) {
                case X -> d2 > e2 + d && d2 > d3 + d;
                case Y -> e2 > d2 + d && e2 > d3 + d;
                case Z -> d3 > d2 + d && d3 > e2 + d;
            };
        }
    }

    @Environment(EnvType.CLIENT)
    static enum Result {
        RUNNING,
        ARRIVED,
        FAILED;

        private Result() {}
    }
}
