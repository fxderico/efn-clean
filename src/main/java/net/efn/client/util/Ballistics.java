package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class Ballistics {

    public static final int DEFAULT_LIFETIME = 240;

    private static final double VOID_DEPTH = 64.0;

    private static final double FULL_MARGIN = 0.3;

    private static final double SEARCH_PAD = 1.3;

    private static final double FLUID_SKIN = 0.001;

    private static final double SHOOTER_CLEARANCE = 1.0;

    private Ballistics() {}

    @Environment(EnvType.CLIENT)
    public static enum Body {
        ARROW(0.05, 0.99, 0.6, true, 0.5),
        TRIDENT(0.05, 0.99, 0.99, true, 0.5),
        ORB(0.03, 0.99, 0.8, false, 0.25),
        FLASK(0.05, 0.99, 0.8, false, 0.25),
        BOTTLE(0.07, 0.99, 0.8, false, 0.25),
        ROCKET(0.0, 1.0, 1.0, true, 0.25);

        private final double gravity;

        private final double airKeep;

        private final double waterKeep;

        private final boolean movesFirst;

        private final double size;

        private Body(
                double gravity2,
                double airKeep2,
                double waterKeep2,
                boolean movesFirst2,
                double size2) {
            this.gravity = gravity2;
            this.airKeep = airKeep2;
            this.waterKeep = waterKeep2;
            this.movesFirst = movesFirst2;
            this.size = size2;
        }

        public double gravity() {
            return this.gravity;
        }
    }

    @Environment(EnvType.CLIENT)
    public static final class Flight {

        private double[] xs = new double[96];

        private double[] ys = new double[96];

        private double[] zs = new double[96];

        private int size;

        private int ticks;

        private double length;

        private Ballistics.Outcome outcome = Ballistics.Outcome.OPEN;

        private Direction face = Direction.UP;

        private Entity struck;

        private Ballistics.Body body = Ballistics.Body.ARROW;

        private double px;

        private double py;

        private double pz;

        private double vx;

        private double vy;

        private double vz;

        private double gravity;

        private double width;

        private double height;

        private boolean wet;

        private int age;

        private int lifetime = 240;

        private Entity shooter;

        private Entity self;

        private boolean leftShooter;

        private Entity shooterRoot;

        private final List<Box> shooterBoxes = new ArrayList<>();

        private final Mutable cursor = new Mutable();

        private double near;

        private double far;

        public Flight() {}

        public Ballistics.Flight launch(Ballistics.Body body2, Vec3d vec, Vec3d vec2) {
            this.body = body2;
            this.px = vec.x;
            this.py = vec.y;
            this.pz = vec.z;
            this.vx = vec2.x;
            this.vy = vec2.y;
            this.vz = vec2.z;
            this.gravity = body2.gravity;
            this.width = body2.size;
            this.height = body2.size;
            this.wet = false;
            this.age = 0;
            this.lifetime = 240;
            this.shooter = null;
            this.self = null;
            this.leftShooter = false;
            return this;
        }

        public Ballistics.Flight gravity(double gravity2) {
            this.gravity = gravity2;
            return this;
        }

        public Ballistics.Flight bounds(double width2, double height2) {
            this.width = width2;
            this.height = height2;
            return this;
        }

        public Ballistics.Flight wet(boolean wet2) {
            this.wet = wet2;
            return this;
        }

        public Ballistics.Flight age(int age2) {
            this.age = age2;
            return this;
        }

        public Ballistics.Flight lifetime(int i) {
            this.lifetime = Math.max(1, i);
            return this;
        }

        public Ballistics.Flight ignoring(Entity shooter2, Entity self2) {
            this.shooter = shooter2;
            this.self = self2;
            return this;
        }

        public Ballistics.Flight leftShooter(boolean leftShooter2) {
            this.leftShooter = leftShooter2;
            return this;
        }

        public Ballistics.Flight solve(World world) {
            this.size = 0;
            this.struck = null;
            this.outcome = Ballistics.Outcome.OPEN;
            this.face = Direction.UP;
            this.trace(world);
            this.strike(world);
            this.measure();
            this.release();
            return this;
        }

        public void clear() {
            this.size = 0;
            this.length = 0.0;
            this.struck = null;
            this.leftShooter = false;
            this.outcome = Ballistics.Outcome.OPEN;
            this.release();
        }

        private void release() {
            this.shooter = null;
            this.self = null;
            this.shooterRoot = null;
            this.shooterBoxes.clear();
        }

        public int size() {
            return this.size;
        }

        public double x(int i) {
            return this.xs[i];
        }

        public double y(int i) {
            return this.ys[i];
        }

        public double z(int i) {
            return this.zs[i];
        }

        public Vec3d point(int i) {
            return new Vec3d(this.xs[i], this.ys[i], this.zs[i]);
        }

        public Vec3d impact() {
            return this.point(this.size - 1);
        }

        public Vec3d heading() {
            if (this.size < 2) {
                return Vec3d.ZERO;
            } else {
                int i = this.size - 1;
                return new Vec3d(
                        this.xs[i] - this.xs[i - 1],
                        this.ys[i] - this.ys[i - 1],
                        this.zs[i] - this.zs[i - 1]);
            }
        }

        public int ticks() {
            return this.ticks;
        }

        public double length() {
            return this.length;
        }

        public Ballistics.Outcome outcome() {
            return this.outcome;
        }

        public Direction face() {
            return this.face;
        }

        public Entity struck() {
            return this.struck;
        }

        public boolean hasLeftShooter() {
            return this.leftShooter;
        }

        private void trace(World world) {
            ShapeContext shapeContext =
                    this.self != null ? ShapeContext.of(this.self) : ShapeContext.absent();
            double d = world.getBottomY() - 64.0;
            double px2 = this.px;
            double py2 = this.py;
            double pz2 = this.pz;
            double vx2 = this.vx;
            double vy2 = this.vy;
            double vz2 = this.vz;
            boolean wet2 = this.wet;
            this.push(px2, py2, pz2);
            int i = 0;
            while (i < this.lifetime) {
                i++;
                if (!this.body.movesFirst) {
                    vy2 -= this.gravity;
                    double e = wet2 ? this.body.waterKeep : this.body.airKeep;
                    vx2 *= e;
                    vy2 *= e;
                    vz2 *= e;
                }
                double d2;
                double e2;
                double d3;
                BlockHitResult collisionsIncludingWorldBorder;
                if ((collisionsIncludingWorldBorder =
                                        world.getCollisionsIncludingWorldBorder(
                                                new RaycastContext(
                                                        new Vec3d(px2, py2, pz2),
                                                        new Vec3d(
                                                                d3 = px2 + vx2,
                                                                e2 = py2 + vy2,
                                                                d2 = pz2 + vz2),
                                                        ShapeType.COLLIDER,
                                                        FluidHandling.NONE,
                                                        shapeContext)))
                                .getType()
                        != Type.MISS) {
                    Vec3d vec = collisionsIncludingWorldBorder.getPos();
                    this.push(vec.x, vec.y, vec.z);
                    this.face = collisionsIncludingWorldBorder.getSide();
                    this.outcome = Ballistics.Outcome.BLOCK;
                    this.ticks = i;
                    return;
                }
                px2 = d3;
                py2 = e2;
                pz2 = d2;
                this.push(d3, e2, d2);
                if (this.body.movesFirst) {
                    double e3 = wet2 ? this.body.waterKeep : this.body.airKeep;
                    vx2 *= e3;
                    vy2 = vy2 * e3 - this.gravity;
                    vz2 *= e3;
                }
                wet2 = this.submerged(world, d3, e2, d2);
                if (e2 < d) {
                    break;
                }
            }
            this.ticks = i;
            if (this.body == Ballistics.Body.ROCKET && i >= this.lifetime) {
                this.outcome = Ballistics.Outcome.BURST;
            }
        }

        private void strike(World world) {
            if (this.size >= 2) {
                boolean bl;
                this.leftShooter = bl = this.leftShooter || this.departs();
                double d = this.xs[0];
                double e = this.ys[0];
                double d2 = this.zs[0];
                double e2 = d;
                double d3 = e;
                double e3 = d2;
                for (int i = 1; i < this.size; i++) {
                    d = Math.min(d, this.xs[i]);
                    e = Math.min(e, this.ys[i]);
                    d2 = Math.min(d2, this.zs[i]);
                    e2 = Math.max(e2, this.xs[i]);
                    d3 = Math.max(d3, this.ys[i]);
                    e3 = Math.max(e3, this.zs[i]);
                }
                Box box = new Box(d, e, d2, e2, d3, e3).expand(1.3);
                List otherEntities = world.getOtherEntities(this.self, box, this::canStrike);
                if (!otherEntities.isEmpty()) {
                    for (int j = 1; j < this.size; j++) {
                        if (!bl && j > 1) {
                            bl = this.outside(j - 1);
                        }
                        double d4 = this.xs[j - 1];
                        double e4 = this.ys[j - 1];
                        double d5 = this.zs[j - 1];
                        double e5 = this.xs[j] - d4;
                        double d6 = this.ys[j] - e4;
                        double e6 = this.zs[j] - d5;
                        double d7 = MathHelper.clamp((this.age + j - 2) / 20.0, 0.0, 0.3);
                        double e7 = 1.7976931348623157E308;
                        Entity entity = null;
                        for (int k = 0; k < otherEntities.size(); k++) {
                            Entity otherEntity = (Entity) otherEntities.get(k);
                            double entry;
                            if ((bl || otherEntity.getRootVehicle() != this.shooterRoot)
                                    && (entry =
                                                    this.entry(
                                                            d4,
                                                            e4,
                                                            d5,
                                                            e5,
                                                            d6,
                                                            e6,
                                                            otherEntity.getBoundingBox(),
                                                            d7))
                                            < e7) {
                                e7 = entry;
                                entity = otherEntity;
                            }
                        }
                        if (entity != null) {
                            this.xs[j] = d4 + e5 * e7;
                            this.ys[j] = e4 + d6 * e7;
                            this.zs[j] = d5 + e6 * e7;
                            this.size = j + 1;
                            this.ticks = j;
                            this.struck = entity;
                            this.outcome = Ballistics.Outcome.ENTITY;
                            return;
                        }
                    }
                }
            }
        }

        private boolean canStrike(Entity entity) {
            return !entity.isSpectator() && entity.canBeHitByProjectile();
        }

        private boolean departs() {
            Entity entity =
                    this.shooterRoot = this.shooter != null ? this.shooter.getRootVehicle() : null;
            if (this.shooterRoot == null) {
                return true;
            } else {
                this.shooterBoxes.clear();
                if (EntityPredicates.CAN_HIT.test(this.shooterRoot)) {
                    this.shooterBoxes.add(this.shooterRoot.getBoundingBox());
                }
                for (Entity entity2 : this.shooterRoot.getPassengersDeep()) {
                    if (EntityPredicates.CAN_HIT.test(entity2)) {
                        this.shooterBoxes.add(entity2.getBoundingBox());
                    }
                }
                return this.outside(0);
            }
        }

        private boolean outside(int i) {
            double d = this.xs[i];
            double e = this.ys[i];
            double d2 = this.zs[i];
            double e2 = this.xs[i + 1] - d;
            double d3 = this.ys[i + 1] - e;
            double e3 = this.zs[i + 1] - d2;
            double d4 = this.width * 0.5;
            double e4 = d - d4 + Math.min(e2, 0.0) - 1.0;
            double d5 = d + d4 + Math.max(e2, 0.0) + 1.0;
            double e5 = e + Math.min(d3, 0.0) - 1.0;
            double d6 = e + this.height + Math.max(d3, 0.0) + 1.0;
            double e6 = d2 - d4 + Math.min(e3, 0.0) - 1.0;
            double d7 = d2 + d4 + Math.max(e3, 0.0) + 1.0;
            for (int j = 0; j < this.shooterBoxes.size(); j++) {
                if (this.shooterBoxes.get(j).intersects(e4, e5, e6, d5, d6, d7)) {
                    return false;
                }
            }
            return true;
        }

        private double entry(
                double d,
                double e,
                double d2,
                double e2,
                double d3,
                double e3,
                Box box,
                double d4) {
            this.near = 0.0;
            this.far = 1.0;
            if (!this.slab(d, e2, box.minX - d4, box.maxX + d4)) {
                return 1.7976931348623157E308;
            } else if (!this.slab(e, d3, box.minY - d4, box.maxY + d4)) {
                return 1.7976931348623157E308;
            } else if (!this.slab(d2, e3, box.minZ - d4, box.maxZ + d4)) {
                return 1.7976931348623157E308;
            } else {
                return this.near > 0.0 ? this.near : 1.7976931348623157E308;
            }
        }

        private boolean slab(double d, double e, double d2, double e2) {
            if (!(Math.abs(e) < 1.0E-12)) {
                double d3 = (d2 - d) / e;
                double e3 = (e2 - d) / e;
                if (d3 > e3) {
                    double d4 = d3;
                    d3 = e3;
                    e3 = d4;
                }
                if (d3 > this.near) {
                    this.near = d3;
                }
                if (e3 < this.far) {
                    this.far = e3;
                }
                return this.near <= this.far;
            } else {
                return d >= d2 && d <= e2;
            }
        }

        private boolean submerged(World world, double d, double e, double d2) {
            double e2 = this.width * 0.5 - 0.001;
            double d3 = e + 0.001;
            int i = MathHelper.floor(d - e2);
            int j = MathHelper.floor(d + e2);
            int k = MathHelper.floor(d3);
            int m = MathHelper.floor(e + this.height - 0.001);
            int n = MathHelper.floor(d2 - e2);
            int o = MathHelper.floor(d2 + e2);
            for (int p = i; p <= j; p++) {
                for (int q = k; q <= m; q++) {
                    for (int r = n; r <= o; r++) {
                        FluidState fluidState = world.getFluidState(this.cursor.set(p, q, r));
                        if (fluidState.isIn(FluidTags.WATER)
                                && q + fluidState.getHeight(world, this.cursor) >= d3) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private void measure() {
            double d = 0.0;
            for (int i = 1; i < this.size; i++) {
                double e = this.xs[i] - this.xs[i - 1];
                double d2 = this.ys[i] - this.ys[i - 1];
                double e2 = this.zs[i] - this.zs[i - 1];
                d += Math.sqrt(e * e + d2 * d2 + e2 * e2);
            }
            this.length = d;
        }

        private void push(double d, double e, double d2) {
            if (this.size == this.xs.length) {
                int i = this.size * 2;
                this.xs = Arrays.copyOf(this.xs, i);
                this.ys = Arrays.copyOf(this.ys, i);
                this.zs = Arrays.copyOf(this.zs, i);
            }
            this.xs[this.size] = d;
            this.ys[this.size] = e;
            this.zs[this.size] = d2;
            this.size++;
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum Outcome {
        OPEN,
        BLOCK,
        ENTITY,
        BURST;

        private Outcome() {}
    }
}
