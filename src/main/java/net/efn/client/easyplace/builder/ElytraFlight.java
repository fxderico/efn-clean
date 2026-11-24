package net.efn.client.easyplace.builder;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap.Type;

import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
final class ElytraFlight {

    static final int BOOST_TICKS = 45;

    static final int HORIZON = 60;

    private static final double CRASH = 1000000.0;

    private static final float[] PITCHES =
            new float[] {
                -62.0F, -46.0F, -33.0F, -23.0F, -15.0F, -9.0F, -4.0F, 1.0F, 6.0F, 12.0F, 19.0F,
                27.0F, 36.0F
            };

    private static final float[] NEAR_YAWS = new float[] {0.0F, 12.0F, -12.0F};

    private static final float[] WIDE_YAWS =
            new float[] {30.0F, -30.0F, 55.0F, -55.0F, 90.0F, -90.0F, 140.0F, -140.0F};

    private static final float[] LAND_PITCHES =
            new float[] {
                -28.0F, -18.0F, -10.0F, -4.0F, 0.0F, 4.0F, 8.0F, 12.0F, 16.0F, 21.0F, 27.0F, 34.0F
            };

    private static final float[] LAND_YAWS =
            new float[] {0.0F, 18.0F, -18.0F, 36.0F, -36.0F, 60.0F, -60.0F, 90.0F, -90.0F};

    private static final double FOOT = 0.3;

    private ElytraFlight() {}

    static float bearing(double d, double e, double d2, double e2) {
        return (float) (MathHelper.atan2(e2 - e, d2 - d) * 57.2957763671875) - 90.0F;
    }

    static double cost(
            ElytraFlight.Glide glide,
            ElytraFlight.Goal goal,
            ElytraFlight.Terrain terrain,
            float f,
            float g,
            boolean bl,
            float h) {
        ElytraFlight.Glide glide2 = glide.copy();
        if (bl) {
            glide2.boost = 45;
        }
        double d = Math.hypot(goal.x() - glide2.x, goal.z() - glide2.z);
        double e = 0.0;
        double d2 = 0.0;
        double e2 = 0.0;
        double d3 = 0.0;
        for (int i = 1; i <= 60; i++) {
            glide2.step(f, g, h);
            double e3 = glide2.y - terrain.under(glide2.x, glide2.z);
            if (e3 < 0.05) {
                return 1000000.0 + (60 - i) * 1000.0;
            }
            double d4 =
                    Math.max(goal.minClear(), Math.max(glide2.speed() * 5.0, -glide2.vy * 25.0));
            if (e3 < d4) {
                double e4 = d4 - e3;
                e += e4 * e4 / 2.0 + 2.0 * e4;
            }
            if (glide2.vy < -0.5) {
                d2 += (-glide2.vy - 0.5) * 25.0;
                if (e3 < 8.0) {
                    d2 += (-glide2.vy - 0.5) * 30.0;
                }
            }
            if (glide2.y < goal.floor()) {
                e2 += (goal.floor() - glide2.y) * 4.0;
            }
            d3 += Math.abs(glide2.y - goal.altitude()) * (i < 20 ? 0.4 : 1.0);
        }
        double d5 = e / 60.0 * 10.0 + d2 / 60.0 * 10.0 + e2 / 60.0 + d3 / 60.0 * 0.4;
        double e5 = glide2.y - goal.altitude();
        d5 += e5 < 0.0 ? -e5 * 2.2 : e5 * (goal.landing() ? 1.6 : 0.6);
        double d6 = Math.hypot(goal.x() - glide2.x, goal.z() - glide2.z);
        d5 -= d - d6;
        if (bl) {
            d5 += goal.rocketCost();
        }
        double flatSpeed;
        if ((flatSpeed = glide2.flatSpeed()) < 0.55) {
            d5 += (0.55 - flatSpeed) * 60.0;
        }
        if (goal.landing() && d6 < 120.0 && flatSpeed > goal.maxSpeed()) {
            d5 += (flatSpeed - goal.maxSpeed()) * 40.0;
        }
        return d5;
    }

    static boolean clear(
            ElytraFlight.Glide glide,
            ElytraFlight.Goal goal,
            ElytraFlight.Terrain terrain,
            float f,
            float g,
            boolean bl,
            float h) {
        ElytraFlight.Glide glide2 = glide.copy();
        if (bl) {
            glide2.boost = 45;
        }
        for (int i = 1; i <= 60; i++) {
            glide2.step(f, g, h);
            if (glide2.y - terrain.under(glide2.x, glide2.z) < goal.minClear()) {
                return false;
            }
        }
        return glide2.flatSpeed() > 0.4;
    }

    static ElytraFlight.Plan search(
            ElytraFlight.Glide glide,
            ElytraFlight.Goal goal,
            ElytraFlight.Terrain terrain,
            boolean bl,
            float f) {
        float bearing = bearing(glide.x, glide.z, goal.x(), goal.z());
        ElytraFlight.Plan plan = pass(glide, goal, terrain, bearing, NEAR_YAWS, bl, f, false, null);
        if (plan.safe()) {
            return plan;
        } else {
            ElytraFlight.Plan plan2;
            return (plan2 = pass(glide, goal, terrain, bearing, NEAR_YAWS, bl, f, true, plan))
                            .safe()
                    ? plan2
                    : pass(glide, goal, terrain, bearing, WIDE_YAWS, bl, f, true, plan2);
        }
    }

    private static ElytraFlight.Plan pass(
            ElytraFlight.Glide glide,
            ElytraFlight.Goal goal,
            ElytraFlight.Terrain terrain,
            float f,
            float[] floats,
            boolean bl,
            float g,
            boolean bl2,
            ElytraFlight.Plan plan) {
        for (float h : floats) {
            float f2 = MathHelper.wrapDegrees(f + h);
            for (float g2 : PITCHES) {
                for (int i = 0; i < 2; i++) {
                    boolean bl3 = i == 1;
                    if (!bl3 || goal.rockets() && !bl && (bl2 || !(g2 > -30.0F))) {
                        double d = cost(glide, goal, terrain, f2, g2, bl3, g) + Math.abs(h) * 0.08;
                        if (plan == null || d < plan.cost()) {
                            plan = new ElytraFlight.Plan(f2, g2, bl3, d, d < 400.0);
                        }
                    }
                }
            }
        }
        return plan;
    }

    static ElytraFlight.Plan touchdown(
            ElytraFlight.Glide glide, BlockPos pos, ElytraFlight.Terrain terrain, float f) {
        float bearing = bearing(glide.x, glide.z, pos.getX() + 0.5, pos.getZ() + 0.5);
        ElytraFlight.Plan plan = null;
        for (float g : LAND_YAWS) {
            float h = MathHelper.wrapDegrees(bearing + g);
            for (float f2 : LAND_PITCHES) {
                double d = landing(glide, pos, terrain, h, f2, f) + Math.abs(g) * 0.15;
                if (plan == null || d < plan.cost()) {
                    plan = new ElytraFlight.Plan(h, f2, false, d, d < 900.0);
                }
            }
        }
        return plan;
    }

    private static double landing(
            ElytraFlight.Glide glide,
            BlockPos pos,
            ElytraFlight.Terrain terrain,
            float f,
            float g,
            float h) {
        ElytraFlight.Glide glide2 = glide.copy();
        double under = terrain.under(glide2.x, glide2.z);
        for (int i = 1; i <= 200; i++) {
            double d = under;
            glide2.step(f, g, h);
            under = terrain.under(glide2.x, glide2.z);
            if (!(glide2.y > under)) {
                if (under - d > 0.6 && glide2.flatSpeed() > 0.3) {
                    return 1000000.0;
                }
                double e = Math.hypot(glide2.x - (pos.getX() + 0.5), glide2.z - (pos.getZ() + 0.5));
                if (glide2.vy < -0.5) {
                    e += 200.0;
                }
                if (glide2.flatSpeed() > 1.0) {
                    e += (glide2.flatSpeed() - 1.0) * 20.0;
                }
                if (!goodFloor(
                        terrain.level(), BlockPos.ofFloored(glide2.x, under - 1.0, glide2.z))) {
                    e += 900.0;
                }
                return e;
            }
        }
        return 1000000.0;
    }

    static boolean goodFloor(ClientWorld world, BlockPos pos) {
        if (pos.getY() < world.getBottomY()) {
            return false;
        } else {
            BlockState blockState = world.getBlockState(pos);
            if (!blockState.getFluidState().isEmpty()
                    || blockState.isIn(BlockTags.LEAVES)
                    || hurts(blockState)) {
                return false;
            } else if (!blockState.isOf(Blocks.SLIME_BLOCK)
                    && !blockState.isOf(Blocks.HONEY_BLOCK)
                    && !blockState.isOf(Blocks.POWDER_SNOW)) {
                VoxelShape collisionShape = blockState.getCollisionShape(world, pos);
                if (collisionShape.isEmpty()) {
                    return false;
                } else {
                    Box boundingBox = collisionShape.getBoundingBox();
                    if (!(boundingBox.maxY < 0.45)
                            && !(boundingBox.maxY > 1.01)
                            && !(boundingBox.getLengthX() < 0.9)
                            && !(boundingBox.getLengthZ() < 0.9)) {
                        BlockPos pos2 = pos.up();
                        BlockState blockState2 = world.getBlockState(pos2);
                        return blockState2.getCollisionShape(world, pos2).isEmpty()
                                && blockState2.getFluidState().isEmpty()
                                && !hurts(blockState2);
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
    }

    private static boolean hurts(BlockState state) {
        return state.isOf(Blocks.MAGMA_BLOCK)
                || state.isIn(BlockTags.CAMPFIRES)
                || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isIn(BlockTags.FIRE)
                || state.isOf(Blocks.POINTED_DRIPSTONE)
                || state.isOf(Blocks.WITHER_ROSE)
                || state.isOf(Blocks.COBWEB)
                || state.isOf(Blocks.LAVA);
    }

    static BlockPos landingSpot(
            ElytraFlight.Terrain terrain,
            int i,
            int j,
            int k,
            boolean bl,
            Predicate<BlockPos> predicate) {
        ClientWorld world = terrain.level();
        for (int m = 0; m <= k; m += 2) {
            for (int n = -m; n <= m; n += 2) {
                for (int o = -m; o <= m; o += 2) {
                    int topY;
                    BlockPos pos;
                    int p;
                    int q;
                    if (Math.max(Math.abs(n), Math.abs(o)) == m
                            && terrain.known((q = i + n) - 1, (p = j + o) - 1)
                            && terrain.known(q + 1, p + 1)
                            && !predicate.test(
                                    pos =
                                            new BlockPos(
                                                    q,
                                                    topY =
                                                            world.getTopY(
                                                                    Type.MOTION_BLOCKING, q, p),
                                                    p))
                            && flat(world, q, p, topY, bl)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static boolean flat(ClientWorld world, int i, int j, int k, boolean bl) {
        if (!goodFloor(world, new BlockPos(i, k - 1, j))) {
            return false;
        } else {
            for (int m = -1; m <= 1; m++) {
                for (int n = -1; n <= 1; n++) {
                    if (m != 0 || n != 0) {
                        int topY = world.getTopY(Type.MOTION_BLOCKING, i + m, j + n);
                        if (bl ? topY != k : Math.abs(topY - k) > 1) {
                            return false;
                        }
                        if (!goodFloor(world, new BlockPos(i + m, topY - 1, j + n))) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Glide {

        double x;

        double y;

        double z;

        double vx;

        double vy;

        double vz;

        float yaw;

        float pitch;

        int boost;

        double gravity;

        Glide() {}

        static ElytraFlight.Glide of(ClientPlayerEntity player, int i) {
            ElytraFlight.Glide glide = new ElytraFlight.Glide();
            glide.x = player.getX();
            glide.y = player.getY();
            glide.z = player.getZ();
            Vec3d velocity = player.getVelocity();
            glide.vx = velocity.x;
            glide.vy = velocity.y;
            glide.vz = velocity.z;
            glide.yaw = player.getYaw();
            glide.pitch = player.getPitch();
            glide.boost = i;
            glide.gravity = player.hasStatusEffect(StatusEffects.SLOW_FALLING) ? 0.01 : 0.08;
            return glide;
        }

        ElytraFlight.Glide copy() {
            ElytraFlight.Glide glide = new ElytraFlight.Glide();
            glide.x = this.x;
            glide.y = this.y;
            glide.z = this.z;
            glide.vx = this.vx;
            glide.vy = this.vy;
            glide.vz = this.vz;
            glide.yaw = this.yaw;
            glide.pitch = this.pitch;
            glide.boost = this.boost;
            glide.gravity = this.gravity;
            return glide;
        }

        void step(float f, float g, float h) {
            this.yaw = this.yaw + MathHelper.clamp(MathHelper.wrapDegrees(f - this.yaw), -h, h);
            this.pitch =
                    MathHelper.clamp(
                            this.pitch + MathHelper.clamp(g - this.pitch, -h, h), -90.0F, 90.0F);
            Vec3d vec = Vec3d.fromPolar(this.pitch, this.yaw);
            double d = this.pitch * 0.017453292F;
            double e = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
            double d2 = Math.sqrt(this.vx * this.vx + this.vz * this.vz);
            double e2 = Math.cos(d);
            e2 *= e2;
            double d3 = this.vy <= 0.0 ? this.gravity : 0.08;
            this.vy += d3 * (-1.0 + e2 * 0.75);
            if (this.vy < 0.0 && e > 0.0) {
                double e3 = this.vy * -0.1 * e2;
                this.vx = this.vx + vec.x * e3 / e;
                this.vy += e3;
                this.vz = this.vz + vec.z * e3 / e;
            }
            if (d < 0.0 && e > 0.0) {
                double d4 = d2 * -Math.sin(d) * 0.04;
                this.vx = this.vx - vec.x * d4 / e;
                this.vy += d4 * 3.2;
                this.vz = this.vz - vec.z * d4 / e;
            }
            if (e > 0.0) {
                this.vx = this.vx + (vec.x / e * d2 - this.vx) * 0.1;
                this.vz = this.vz + (vec.z / e * d2 - this.vz) * 0.1;
            }
            this.vx *= 0.99;
            this.vy *= 0.98;
            this.vz *= 0.99;
            if (this.boost > 0) {
                this.vx = this.vx + (vec.x * 0.1 + (vec.x * 1.5 - this.vx) * 0.5);
                this.vy = this.vy + (vec.y * 0.1 + (vec.y * 1.5 - this.vy) * 0.5);
                this.vz = this.vz + (vec.z * 0.1 + (vec.z * 1.5 - this.vz) * 0.5);
                this.boost--;
            }
            this.x = this.x + this.vx;
            this.y = this.y + this.vy;
            this.z = this.z + this.vz;
        }

        double speed() {
            return Math.sqrt(this.vx * this.vx + this.vy * this.vy + this.vz * this.vz);
        }

        double flatSpeed() {
            return Math.sqrt(this.vx * this.vx + this.vz * this.vz);
        }
    }

    @Environment(EnvType.CLIENT)
    record Goal(
            double x,
            double z,
            double altitude,
            double minClear,
            boolean landing,
            boolean rockets,
            double rocketCost,
            double maxSpeed,
            double floor) {}

    @Environment(EnvType.CLIENT)
    record Plan(float yaw, float pitch, boolean rocket, double cost, boolean safe) {}

    @Environment(EnvType.CLIENT)
    static final class Terrain {

        private final ClientWorld level;

        private final double unknown;

        private final Long2DoubleOpenHashMap heights = new Long2DoubleOpenHashMap();

        Terrain(ClientWorld level2, double unknown2) {
            this.level = level2;
            this.unknown = unknown2;
            this.heights.defaultReturnValue(0.0 / 0.0);
        }

        double surface(double d, double e) {
            int i = MathHelper.floor(d);
            int j;
            long l = BlockPos.asLong(i, 0, j = MathHelper.floor(e));
            double height = this.heights.get(l);
            if (Double.isNaN(height)) {
                height =
                        this.known(i, j)
                                ? this.level.getTopY(Type.MOTION_BLOCKING, i, j)
                                : this.unknown;
                this.heights.put(l, height);
            }
            return height;
        }

        double under(double d, double e) {
            double surface = this.surface(d, e);
            surface = Math.max(surface, this.surface(d + 0.3, e + 0.3));
            surface = Math.max(surface, this.surface(d - 0.3, e - 0.3));
            surface = Math.max(surface, this.surface(d + 0.3, e - 0.3));
            return Math.max(surface, this.surface(d - 0.3, e + 0.3));
        }

        boolean known(int i, int j) {
            return this.level.getChunkManager().isChunkLoaded(i >> 4, j >> 4);
        }

        ClientWorld level() {
            return this.level;
        }
    }
}
