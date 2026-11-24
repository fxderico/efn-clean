package net.efn.client.easyplace.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@Environment(EnvType.CLIENT)
public final class PlacementUtil {

    private PlacementUtil() {}

    public static Vec3d faceCenterCursor(BlockPos pos, Direction direction) {
        return pos.toCenterPos()
                .add(
                        direction.getOffsetX() * 0.5,
                        direction.getOffsetY() * 0.5,
                        direction.getOffsetZ() * 0.5);
    }

    public static float[] calcRotation(Vec3d vec, Vec3d vec2) {
        double d = vec2.x - vec.x;
        double e = vec2.y - vec.y;
        double d2 = vec2.z - vec.z;
        double e2 = Math.sqrt(d * d + d2 * d2);
        float f = (float) Math.toDegrees(Math.atan2(-d, d2));
        float g = (float) Math.toDegrees(-Math.atan2(e, e2));
        return new float[] {MathHelper.wrapDegrees(f), MathHelper.clamp(g, -90.0F, 90.0F)};
    }

    public static BlockHitResult rayTraceTowards(PlayerEntity player, float f, float g, double d) {
        return rayTraceFrom(player, getEyePos(player), f, g, d);
    }

    public static BlockHitResult rayTraceFrom(
            PlayerEntity player, Vec3d vec, float f, float g, double d) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return null;
        } else {
            double e = Math.toRadians(f);
            double d2 = Math.cos(Math.toRadians(g));
            double e2 = Math.sin(Math.toRadians(g));
            Vec3d vec2 = new Vec3d(-Math.sin(e) * d2, -e2, Math.cos(e) * d2);
            Vec3d vec3 = vec.add(vec2.multiply(d));
            return world.raycast(
                    new RaycastContext(vec, vec3, ShapeType.OUTLINE, FluidHandling.NONE, player));
        }
    }

    public static Vec3d getEyePos(PlayerEntity player) {
        return new Vec3d(player.getX(), player.getEyeY(), player.getZ());
    }

    public static Direction getClickFace(Direction direction) {
        return direction.getOpposite();
    }

    public static Direction getPlaceSide(BlockPos pos, Vec3d vec) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return null;
        } else {
            Direction direction = null;
            double d = -1.7976931348623157E308;
            for (Direction direction2 : Direction.values()) {
                BlockPos pos2 = pos.offset(direction2);
                BlockState blockState = world.getBlockState(pos2);
                if (!blockState.isAir() && !blockState.getCollisionShape(world, pos2).isEmpty()) {
                    Vec3d vec2 = vec.subtract(pos2.toCenterPos());
                    double e =
                            vec2.x * direction2.getOffsetX()
                                    + vec2.y * direction2.getOffsetY()
                                    + vec2.z * direction2.getOffsetZ();
                    if (e > d) {
                        d = e;
                        direction = direction2;
                    }
                }
            }
            return direction;
        }
    }

    public static boolean faceMatchesAxis(BlockState state, Direction direction) {
        return !state.contains(Properties.AXIS)
                || state.get(Properties.AXIS) == direction.getAxis();
    }

    public static boolean isFaceVisibleToEye(BlockPos pos, Direction direction, Vec3d vec) {
        double d = 0.05;
        return switch (direction) {
            case NORTH -> vec.z < pos.getZ() - 0.05;
            case SOUTH -> vec.z > pos.getZ() + 1.0 + 0.05;
            case WEST -> vec.x < pos.getX() - 0.05;
            case EAST -> vec.x > pos.getX() + 1.0 + 0.05;
            case UP -> vec.y > pos.getY() + 1.0 + 0.05;
            case DOWN -> vec.y < pos.getY() - 0.05;
        };
    }
}
