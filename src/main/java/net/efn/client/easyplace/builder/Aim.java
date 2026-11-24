package net.efn.client.easyplace.builder;

import net.efn.client.easyplace.mixin.AccessorMouseHandler;
import net.efn.client.easyplace.util.PlacementUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FluidFillable;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

import java.util.Random;

@Environment(EnvType.CLIENT)
public final class Aim {

    private static final Direction[] ORDER =
            new Direction[] {
                Direction.UP,
                Direction.NORTH,
                Direction.SOUTH,
                Direction.EAST,
                Direction.WEST,
                Direction.DOWN
            };

    private static final double[][] FACE_POINTS =
            new double[][] {{0.5, 0.5}, {0.5, 0.85}, {0.5, 0.15}, {0.15, 0.5}, {0.85, 0.5}};

    private static final float SETTLED = 0.6F;

    private static final float RETARGET = 25.0F;

    private static Aim.Motion motion;

    private static final Random HAND = new Random();

    public static final float HAND_TURN = 22.0F;

    private Aim() {}

    static Aim.Shot at(MinecraftClient client, BlockPos pos) {
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null) {
            BlockState blockState = client.world.getBlockState(pos);
            VoxelShape outlineShape = blockState.getOutlineShape(client.world, pos);
            if (outlineShape.isEmpty()) {
                return null;
            } else {
                Box box = outlineShape.getBoundingBox().offset(pos);
                Vec3d eyePos = player.getEyePos();
                double blockInteractionRange = player.getBlockInteractionRange();
                for (Direction direction : ORDER) {
                    float[] rotation;
                    BlockHitResult blockHitResult;
                    Vec3d vec;
                    if (faces(box, direction, eyePos)
                            && !((vec = faceCenter(box, direction)).distanceTo(eyePos)
                                    > blockInteractionRange)
                            && (blockHitResult =
                                            trace(
                                                    client,
                                                    player,
                                                    (rotation =
                                                                    PlacementUtil.calcRotation(
                                                                            eyePos, vec))
                                                            [0],
                                                    rotation[1],
                                                    blockInteractionRange))
                                    != null
                            && blockHitResult.getType() == Type.BLOCK
                            && blockHitResult.getBlockPos().equals(pos)
                            && blockHitResult.getSide() == direction) {
                        return new Aim.Shot(blockHitResult, rotation[0], rotation[1]);
                    }
                }
                return null;
            }
        } else {
            return null;
        }
    }

    static Aim.Shot against(MinecraftClient client, BlockPos pos, Fluid fluid) {
        return client.player == null
                ? null
                : against(client, pos, fluid, client.player.getEyePos());
    }

    static Aim.Shot against(MinecraftClient client, BlockPos pos, Fluid fluid, Vec3d vec) {
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null) {
            double blockInteractionRange = player.getBlockInteractionRange();
            for (Direction direction : ORDER) {
                BlockPos pos2 = pos.offset(direction.getOpposite());
                BlockState blockState = client.world.getBlockState(pos2);
                Box box;
                VoxelShape outlineShape;
                FluidFillable fluidFillable;
                Block block;
                if (!blockState.isReplaceable()
                        && !clickable(client, pos2, blockState)
                        && (fluid == null
                                || !((block = blockState.getBlock()) instanceof FluidFillable)
                                || !(fluidFillable = (FluidFillable) block)
                                        .canFillWithFluid(
                                                player, client.world, pos2, blockState, fluid))
                        && !(outlineShape = blockState.getOutlineShape(client.world, pos2))
                                .isEmpty()
                        && faces(
                                box = outlineShape.getBoundingBox().offset(pos2), direction, vec)) {
                    for (double[] doubles : FACE_POINTS) {
                        Vec3d vec2 = facePoint(box, direction, doubles[0], doubles[1]);
                        float[] rotation;
                        BlockHitResult blockHitResult;
                        if (!(vec2.distanceTo(vec) > blockInteractionRange)
                                && (blockHitResult =
                                                PlacementUtil.rayTraceFrom(
                                                        player,
                                                        vec,
                                                        (rotation =
                                                                        PlacementUtil.calcRotation(
                                                                                vec, vec2))
                                                                [0],
                                                        rotation[1],
                                                        blockInteractionRange))
                                        != null
                                && blockHitResult.getType() == Type.BLOCK
                                && blockHitResult.getBlockPos().equals(pos2)
                                && blockHitResult.getSide() == direction) {
                            return new Aim.Shot(blockHitResult, rotation[0], rotation[1]);
                        }
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private static Vec3d facePoint(Box box, Direction direction, double d, double e) {
        double d2 = box.minX + (box.maxX - box.minX) * d;
        double e2 = box.minZ + (box.maxZ - box.minZ) * (direction.getAxis() == Axis.Y ? e : d);
        double d3 = box.minY + (box.maxY - box.minY) * e;
        return switch (direction) {
            case UP -> new Vec3d(d2, box.maxY, e2);
            case DOWN -> new Vec3d(d2, box.minY, e2);
            case NORTH -> new Vec3d(d2, d3, box.minZ);
            case SOUTH -> new Vec3d(d2, d3, box.maxZ);
            case WEST -> new Vec3d(box.minX, d3, e2);
            case EAST -> new Vec3d(box.maxX, d3, e2);
        };
    }

    static Aim.Shot side(MinecraftClient client, BlockPos pos, Direction direction) {
        return side(client, pos, direction, new double[][] {{0.5, 0.85}, {0.5, 0.5}, {0.5, 0.2}});
    }

    static Aim.Shot mine(
            MinecraftClient client, BlockPos pos, Direction direction, double d, double e) {
        return side(
                client,
                pos,
                direction,
                new double[][] {{d, e}, {0.5, 0.5}, {0.5, 0.85}, {0.5, 0.2}});
    }

    private static Aim.Shot side(
            MinecraftClient client, BlockPos pos, Direction direction, double[][] doubleGrid) {
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null) {
            VoxelShape outlineShape =
                    client.world.getBlockState(pos).getOutlineShape(client.world, pos);
            if (outlineShape.isEmpty()) {
                return null;
            } else {
                Box box = outlineShape.getBoundingBox().offset(pos);
                Vec3d eyePos = player.getEyePos();
                double blockInteractionRange = player.getBlockInteractionRange();
                if (!faces(box, direction, eyePos)) {
                    return null;
                } else {
                    for (double[] doubles : doubleGrid) {
                        Vec3d vec = facePoint(box, direction, doubles[0], doubles[1]);
                        float[] rotation;
                        BlockHitResult blockHitResult;
                        if (!(vec.distanceTo(eyePos) > blockInteractionRange)
                                && (blockHitResult =
                                                trace(
                                                        client,
                                                        player,
                                                        (rotation =
                                                                        PlacementUtil.calcRotation(
                                                                                eyePos, vec))
                                                                [0],
                                                        rotation[1],
                                                        blockInteractionRange))
                                        != null
                                && blockHitResult.getType() == Type.BLOCK
                                && blockHitResult.getBlockPos().equals(pos)
                                && blockHitResult.getSide() == direction) {
                            return new Aim.Shot(blockHitResult, rotation[0], rotation[1]);
                        }
                    }
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    static Aim.Shot water(MinecraftClient client, BlockPos pos) {
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null) {
            Vec3d eyePos = player.getEyePos();
            double blockInteractionRange = player.getBlockInteractionRange();
            Vec3d vec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5);
            if (!(eyePos.y <= vec.y) && !(vec.distanceTo(eyePos) > blockInteractionRange)) {
                float[] rotation = PlacementUtil.calcRotation(eyePos, vec);
                BlockHitResult blockHitResult =
                        trace(
                                client,
                                player,
                                rotation[0],
                                rotation[1],
                                blockInteractionRange,
                                FluidHandling.SOURCE_ONLY);
                return blockHitResult != null
                                && blockHitResult.getType() == Type.BLOCK
                                && blockHitResult.getBlockPos().equals(pos)
                        ? new Aim.Shot(blockHitResult, rotation[0], rotation[1])
                        : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    static boolean clickable(MinecraftClient client, BlockPos pos) {
        return clickable(client, pos, client.world.getBlockState(pos));
    }

    private static boolean clickable(MinecraftClient client, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        return client.world.getBlockEntity(pos) != null
                || block instanceof DoorBlock
                || block instanceof TrapdoorBlock
                || block instanceof FenceGateBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof AbstractRedstoneGateBlock
                || block instanceof NoteBlock;
    }

    static void look(MinecraftClient client, float f, float g) {
        ClientPlayerEntity player = client.player;
        if (player != null) {
            clearMouse(client);
            motion = null;
            float yaw2 = player.getYaw();
            float mouseStep = mouseStep(client);
            player.setYaw(yaw2 + mouse(MathHelper.wrapDegrees(f - yaw2), mouseStep));
            player.setPitch(
                    MathHelper.clamp(
                            player.getPitch() + mouse(g - player.getPitch(), mouseStep),
                            -90.0F,
                            90.0F));
        }
    }

    static float mouseStep(MinecraftClient client) {
        double d = client.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        return (float) (d * d * d * 8.0 * 0.15);
    }

    private static float mouse(float f, float g) {
        return g <= 0.0F ? f : Math.round(f / g) * g;
    }

    public static float turn(MinecraftClient client, float f, float g, float h) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return 0.0F;
        } else {
            clearMouse(client);
            float yaw2 = player.getYaw();
            float pitch2 = player.getPitch();
            float f2 = MathHelper.wrapDegrees(f - yaw2);
            float g2 = g - pitch2;
            float h2;
            float f3;
            if (Math.abs(f2) < 0.6F && Math.abs(g2) < 0.6F) {
                motion = null;
                f3 = f2;
                h2 = g2;
            } else {
                if (motion != null && !motion.done() && !motion.drifted(f, g)) {
                    motion.retarget(f, g);
                } else {
                    motion = Aim.Motion.start(yaw2, pitch2, f, g, h);
                }
                float[] floats = motion.next();
                f3 = MathHelper.clamp(MathHelper.wrapDegrees(floats[0] - yaw2), -h, h);
                h2 = MathHelper.clamp(floats[1] - pitch2, -h, h);
            }
            float mouseStep = mouseStep(client);
            player.setYaw(yaw2 + mouse(f3, mouseStep));
            player.setPitch(MathHelper.clamp(pitch2 + mouse(h2, mouseStep), -90.0F, 90.0F));
            return Math.abs(MathHelper.wrapDegrees(f - player.getYaw()));
        }
    }

    private static void clearMouse(MinecraftClient client) {
        if (client.mouse != null) {
            AccessorMouseHandler mouse = (AccessorMouseHandler) client.mouse;
            mouse.easyplace$setAccumulatedDX(0.0);
            mouse.easyplace$setAccumulatedDY(0.0);
        }
    }

    static boolean looking(ClientPlayerEntity player, float f, float g) {
        return Math.abs(MathHelper.wrapDegrees(player.getYaw() - f)) < 0.5F
                && Math.abs(player.getPitch() - g) < 0.5F;
    }

    static BlockHitResult trace(
            MinecraftClient client, ClientPlayerEntity player, float f, float g, double d) {
        return trace(client, player, f, g, d, FluidHandling.NONE);
    }

    private static BlockHitResult trace(
            MinecraftClient client,
            ClientPlayerEntity player,
            float f,
            float g,
            double d,
            FluidHandling fluidHandling) {
        Vec3d eyePos = player.getEyePos();
        double e = Math.toRadians(f);
        double d2 = Math.cos(Math.toRadians(g));
        Vec3d vec = new Vec3d(-Math.sin(e) * d2, -Math.sin(Math.toRadians(g)), Math.cos(e) * d2);
        return client.world.raycast(
                new RaycastContext(
                        eyePos,
                        eyePos.add(vec.multiply(d)),
                        ShapeType.OUTLINE,
                        fluidHandling,
                        player));
    }

    private static boolean faces(Box box, Direction direction, Vec3d vec) {
        double d = 0.02;
        return switch (direction) {
            case UP -> vec.y > box.maxY + d;
            case DOWN -> vec.y < box.minY - d;
            case NORTH -> vec.z < box.minZ - d;
            case SOUTH -> vec.z > box.maxZ + d;
            case WEST -> vec.x < box.minX - d;
            case EAST -> vec.x > box.maxX + d;
        };
    }

    private static Vec3d faceCenter(Box box, Direction direction) {
        Vec3d center = box.getCenter();
        return switch (direction) {
            case UP -> new Vec3d(center.x, box.maxY, center.z);
            case DOWN -> new Vec3d(center.x, box.minY, center.z);
            case NORTH -> new Vec3d(center.x, center.y, box.minZ);
            case SOUTH -> new Vec3d(center.x, center.y, box.maxZ);
            case WEST -> new Vec3d(box.minX, center.y, center.z);
            case EAST -> new Vec3d(box.maxX, center.y, center.z);
        };
    }

    @Environment(EnvType.CLIENT)
    private static final class Motion {

        private final float startYaw;

        private final float startPitch;

        private final float bend;

        private final int ticks;

        private float endYaw;

        private float endPitch;

        private float targetYaw;

        private float targetPitch;

        private int elapsed;

        private Motion(
                float startYaw2,
                float startPitch2,
                float endYaw2,
                float endPitch2,
                float targetYaw2,
                float targetPitch2,
                float bend2,
                int ticks2) {
            this.startYaw = startYaw2;
            this.startPitch = startPitch2;
            this.endYaw = endYaw2;
            this.endPitch = endPitch2;
            this.targetYaw = targetYaw2;
            this.targetPitch = targetPitch2;
            this.bend = bend2;
            this.ticks = ticks2;
        }

        static Aim.Motion start(float f, float g, float h, float f2, float g2) {
            float h2 = MathHelper.wrapDegrees(h - f);
            float f3 = f2 - g;
            double d = Math.hypot(h2, f3);
            double e =
                    (0.07 + 0.055 * Math.log(1.0 + d / 3.0) / Math.log(2.0))
                            * Math.exp(Aim.HAND.nextGaussian() * 0.22);
            int i =
                    (int)
                            Math.max(
                                    Math.ceil(d * 1.9 / g2),
                                    (double) Math.max(1L, Math.round(e * 20.0)));
            double d2 = d > 6.0 ? Aim.HAND.nextGaussian() * 0.045 : 0.0;
            double e2 = Math.min(1.2, d * 0.015);
            float g3 = (float) (f + h2 * (1.0 + d2) + Aim.HAND.nextGaussian() * e2);
            float h3 = (float) (g + f3 * (1.0 + d2) + Aim.HAND.nextGaussian() * e2 * 0.7);
            float f4 = (float) (Aim.HAND.nextGaussian() * 0.06);
            return new Aim.Motion(f, g, g3, h3, f + h2, f2, f4, i);
        }

        float[] next() {
            this.elapsed++;
            double d = Math.min(1.0, (double) this.elapsed / this.ticks);
            double e = d * d * d * (10.0 - 15.0 * d + 6.0 * d * d);
            double d2 = Math.sin(3.141592653589793 * d) * this.bend;
            float f = this.endYaw - this.startYaw;
            float g = this.endPitch - this.startPitch;
            return new float[] {
                (float) (this.startYaw + f * e - g * d2), (float) (this.startPitch + g * e + f * d2)
            };
        }

        boolean done() {
            return this.elapsed >= this.ticks;
        }

        boolean drifted(float f, float g) {
            return Math.hypot(MathHelper.wrapDegrees(f - this.targetYaw), g - this.targetPitch)
                    > 25.0;
        }

        void retarget(float f, float targetPitch2) {
            float g = MathHelper.wrapDegrees(f - this.targetYaw);
            float h = targetPitch2 - this.targetPitch;
            this.endYaw += g;
            this.endPitch += h;
            this.targetYaw += g;
            this.targetPitch = targetPitch2;
        }
    }

    @Environment(EnvType.CLIENT)
    record Shot(BlockHitResult hit, float yaw, float pitch) {}
}
