package net.efn.client.module.impl.combat.bot;

import net.efn.client.util.pvp.Rotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;

import java.util.List;

@Environment(EnvType.CLIENT)
final class PearlFlight {

    private static final double SPEED = 1.5;

    private static final double GRAVITY = 0.03;

    private static final double DRAG = 0.99;

    private static final double WATER_DRAG = 0.8;

    private static final int MAX_TICKS = 100;

    private static final int MAX_DROP = 3;

    private static final double EDGE_MARGIN = 0.8;

    private PearlFlight() {}

    static PearlFlight.Landing land(ClientPlayerEntity player, float f, float g) {
        Vec3d vec = player.getEyePos().subtract(0.0, 0.1, 0.0);
        Vec3d movement = player.getMovement();
        Vec3d vec2 =
                Vec3d.fromPolar(g, f)
                        .multiply(1.5)
                        .add(movement.x, player.isOnGround() ? 0.0 : movement.y, movement.z);
        return fly(player.getEntityWorld(), player, vec, vec2);
    }

    static PearlFlight.Landing fly(World world, Entity entity, Vec3d vec, Vec3d vec2) {
        Vec3d vec3 = vec;
        Vec3d vec4 = vec2;
        for (int i = 1; i <= 100; i++) {
            BlockPos pos = BlockPos.ofFloored(vec3);
            if (!world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)
                    || vec3.y < world.getBottomY()) {
                return null;
            }
            boolean bl = world.getFluidState(pos).isIn(FluidTags.WATER);
            vec4 = new Vec3d(vec4.x, vec4.y - 0.03, vec4.z).multiply(bl ? 0.8 : 0.99);
            Vec3d vec5 = vec3.add(vec4);
            BlockHitResult blockHitResult =
                    world.raycast(
                            new RaycastContext(
                                    vec3, vec5, ShapeType.COLLIDER, FluidHandling.NONE, entity));
            if (blockHitResult.getType() != Type.MISS) {
                Vec3d vec6 = blockHitResult.getPos();
                BlockPos pos2 = floorUnder(world, vec6);
                return pos2 == null ? null : new PearlFlight.Landing(vec6, pos2, i);
            }
            vec3 = vec5;
        }
        return null;
    }

    static Rotation escape(ClientPlayerEntity player, List<? extends Entity> list, double d) {
        Vec3d zERO = Vec3d.ZERO;
        for (Entity entity : list) {
            zERO = zERO.add(entity.getEntityPos());
        }
        zERO = zERO.multiply(1.0 / Math.max(1, list.size()));
        float f =
                (float)
                        Math.toDegrees(
                                Math.atan2(-(player.getX() - zERO.x), player.getZ() - zERO.z));
        Rotation rotation = null;
        double e = -1.0 / 0.0;
        for (int i = -4; i <= 4; i++) {
            float g = MathHelper.wrapDegrees(f + i * 22.5F);
            for (float h = -45.0F; h <= 5.0F; h += 7.0F) {
                PearlFlight.Landing landing = land(player, g, h);
                double d2;
                double nearest;
                if (landing != null
                        && safe(player.getEntityWorld(), landing)
                        && !((nearest = nearest(list, landing.at())) < d)
                        && (d2 =
                                        nearest
                                                - 0.5
                                                        * (landing.at().y
                                                                - landing.floor().getY()
                                                                - 1.0)
                                                - 0.05 * landing.ticks())
                                > e) {
                    e = d2;
                    rotation = new Rotation(g, h);
                }
            }
        }
        return rotation;
    }

    static Rotation toward(ClientPlayerEntity player, Vec3d vec, double d, double e) {
        Vec3d eyePos = player.getEyePos();
        double d2 = Math.hypot(vec.x - eyePos.x, vec.z - eyePos.z);
        float f = (float) Math.toDegrees(Math.atan2(-(vec.x - eyePos.x), vec.z - eyePos.z));
        double e2 = (d + e) / 2.0;
        for (boolean bl : new boolean[] {false, true}) {
            double[] doubles;
            if (bl) {
                double[] doubles2 = new double[] {0.0, e2, 0.0};
                doubles = doubles2;
                doubles2[2] = -e2;
            } else {
                double[] doubles3 = new double[] {e2, 0.0};
                doubles = doubles3;
                doubles3[1] = -e2;
            }
            for (double d3 : doubles) {
                double e3 = d2 + d3;
                if (!(e3 < 1.0)) {
                    for (float g : new float[] {0.0F, -1.5F, 1.5F, -3.0F, 3.0F}) {
                        Rotation rotation =
                                pitchFor(player, eyePos, MathHelper.wrapDegrees(f + g), e3, bl);
                        if (rotation != null
                                && reaches(
                                        player,
                                        rotation.yaw(),
                                        rotation.pitch(),
                                        vec,
                                        bl ? 0.0 : d,
                                        e)) {
                            return rotation;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Rotation pitchFor(
            ClientPlayerEntity player, Vec3d vec, float f, double d, boolean bl) {
        float g = 0.0F / 0.0F;
        float h = bl ? -88.0F : 15.0F;
        float f2 = bl ? -40.0F : -60.0F;
        float g2 = bl ? 4.0F : -5.0F;
        for (float h2 = h; bl ? h2 <= f2 : h2 >= f2; h2 += g2) {
            if (!(carry(player, vec, f, h2) < d)) {
                if (Float.isNaN(g)) {
                    return null;
                }
                float f3 = g;
                float g3 = h2;
                for (int i = 0; i < 8; i++) {
                    float h3 = (f3 + g3) / 2.0F;
                    if (carry(player, vec, f, h3) >= d) {
                        g3 = h3;
                    } else {
                        f3 = h3;
                    }
                }
                return new Rotation(f, (f3 + g3) / 2.0F);
            }
            g = h2;
        }
        return null;
    }

    private static double carry(ClientPlayerEntity player, Vec3d vec, float f, float g) {
        PearlFlight.Landing landing = land(player, f, g);
        return landing == null
                ? 1.7976931348623157E308
                : Math.hypot(landing.at().x - vec.x, landing.at().z - vec.z);
    }

    static boolean escapes(
            ClientPlayerEntity player, float f, float g, List<? extends Entity> list, double d) {
        PearlFlight.Landing landing = land(player, f, g);
        return landing != null
                && safe(player.getEntityWorld(), landing)
                && nearest(list, landing.at()) >= d;
    }

    static boolean reaches(
            ClientPlayerEntity player, float f, float g, Vec3d vec, double d, double e) {
        PearlFlight.Landing landing = land(player, f, g);
        if (landing != null && safe(player.getEntityWorld(), landing)) {
            double horizontal = horizontal(landing.at(), vec);
            return horizontal >= d && horizontal <= e && Math.abs(landing.at().y - vec.y) <= 3.0;
        } else {
            return false;
        }
    }

    static int flightTicks(double d) {
        if (d <= 6.0) {
            return 5;
        } else if (d <= 10.0) {
            return 5 + (int) Math.round((d - 6.0) / 2.0);
        } else if (d <= 14.0) {
            return 7 + (int) Math.round((d - 10.0) * 0.75);
        } else {
            return d <= 20.0
                    ? 10 + (int) Math.round((d - 14.0) * 0.83)
                    : 15 + (int) Math.round((d - 20.0) * 0.8);
        }
    }

    static double nearest(List<? extends Entity> list, Vec3d vec) {
        double d = 1.7976931348623157E308;
        for (Entity entity : list) {
            d = Math.min(d, entity.getEntityPos().distanceTo(vec));
        }
        return d;
    }

    private static double horizontal(Vec3d vec, Vec3d vec2) {
        return Math.hypot(vec.x - vec2.x, vec.z - vec2.z);
    }

    private static BlockPos floorUnder(World world, Vec3d vec) {
        BlockPos pos = BlockPos.ofFloored(vec.x, vec.y + 0.05, vec.z);
        for (int i = 0; i <= 4; i++) {
            BlockPos pos2 = pos.down(i);
            if (!world.getBlockState(pos2).getCollisionShape(world, pos2).isEmpty()) {
                return pos2;
            }
        }
        return null;
    }

    private static boolean safe(World world, PearlFlight.Landing landing) {
        BlockPos pos = landing.floor();
        if (landing.at().y - (pos.getY() + 1.0) > 3.0) {
            return false;
        } else {
            for (double[] doubles :
                    new double[][] {{0.8, 0.0}, {-0.8, 0.0}, {0.0, 0.8}, {0.0, -0.8}}) {
                BlockPos pos2 = floorUnder(world, landing.at().add(doubles[0], 0.0, doubles[1]));
                if (pos2 == null || pos2.getY() != pos.getY()) {
                    return false;
                }
            }
            if (!harmful(world.getBlockState(pos))
                    && !world.getBlockState(pos).isOf(Blocks.MAGMA_BLOCK)) {
                for (int i = 1; i <= 2; i++) {
                    BlockPos pos3 = pos.up(i);
                    BlockState blockState = world.getBlockState(pos3);
                    if (harmful(blockState)
                            || !blockState.getCollisionShape(world, pos3).isEmpty()
                            || !world.getFluidState(pos3).isEmpty()) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    private static boolean harmful(BlockState state) {
        return state.isOf(Blocks.LAVA)
                || state.isIn(BlockTags.FIRE)
                || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.COBWEB)
                || state.isOf(Blocks.POWDER_SNOW)
                || state.isOf(Blocks.WITHER_ROSE);
    }

    @Environment(EnvType.CLIENT)
    record Landing(Vec3d at, BlockPos floor, int ticks) {}
}
