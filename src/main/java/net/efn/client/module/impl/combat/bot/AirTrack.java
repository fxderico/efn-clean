package net.efn.client.module.impl.combat.bot;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
final class AirTrack {

    private static final double GRAVITY = 0.08;

    private static final double VERTICAL_DRAG = 0.98;

    private static final double AIR_DRAG = 0.91;

    private static final double HIT_KNOCK = 0.4;

    private static final double SPRINT_KNOCK = 0.5;

    private static final double MAX_LIFT = 0.4;

    private static final double KEY_PUSH = 0.026;

    private static final double MAX_PUSH = 0.045;

    private static final double MAX_LEARNED = 0.1;

    static final int LAND_TICKS = 40;

    private static final int KICK_WAIT = 8;

    private static final double BLAST_NEAR = 10.0;

    private PlayerEntity target;

    private RivalBook.Rival rival;

    private final List<AirTrack.Sample> flight = new ArrayList<>();

    private Vec3d lastSeen;

    private boolean airborne;

    private Vec3d pushSum = Vec3d.ZERO;

    private int pushes;

    private Vec3d early;

    private Vec3d kickFrom;

    private Vec3d kick;

    private Vec3d kickWay;

    private int kickAt = -1;

    private boolean kickSeen;

    private int kickSeenAt;

    private boolean kickSpoiled;

    AirTrack.Landing bare;

    private double gain;

    AirTrack.Landing guessed;

    AirTrack() {}

    void forget() {
        this.target = null;
        this.rival = null;
        this.flight.clear();
        this.lastSeen = null;
        this.airborne = false;
        this.pushSum = Vec3d.ZERO;
        this.pushes = 0;
        this.kickAt = -1;
        this.guessed = null;
        this.bare = null;
    }

    void note(PlayerEntity target2, int kickSeenAt2, RivalBook.Rival rival2) {
        if (target2 != this.target) {
            this.forget();
            this.target = target2;
        }
        this.rival = rival2;
        Vec3d vec = server(target2);
        boolean bl = !vec.equals(this.lastSeen);
        this.lastSeen = vec;
        if (this.kickAt >= 0 && !this.kickSeen) {
            if (vec.y > this.kickFrom.y + 0.05
                    || vec.subtract(this.kickFrom).horizontalLengthSquared() > 0.01) {
                this.kickSeen = true;
                this.kickSeenAt = kickSeenAt2;
            } else if (kickSeenAt2 - this.kickAt > 8) {
                this.kickAt = -1;
            }
        }
        if (this.kickAt >= 0 && kickSeenAt2 - this.kickAt > 40) {
            this.kickAt = -1;
        }
        boolean bl2 = !target2.isOnGround() && !floating(target2);
        if (bl2) {
            if (!this.airborne) {
                this.airborne = true;
                this.restart();
            }
            if (bl) {
                this.flight.add(new AirTrack.Sample(kickSeenAt2, vec));
                if (this.flight.size() == 2) {
                    this.readEarly();
                }
                this.read();
            }
        } else if (this.airborne) {
            this.airborne = false;
            if (this.kickAt >= 0 && this.kickSeen) {
                this.learn(vec);
            }
            this.kickAt = -1;
            this.restart();
        }
    }

    void kicked(ClientPlayerEntity player, PlayerEntity player2, float f, boolean bl, int kickAt2) {
        if (player2 == this.target) {
            this.kickAt = -1;
            double d = 1.0 - resistance(player2);
            if (!(d <= 0.0)) {
                Vec3d vec = server(player2);
                Vec3d vec2 = vec.subtract(player.getEntityPos()).multiply(1.0, 0.0, 1.0);
                if (!(vec2.lengthSquared() < 1.0E-6)) {
                    vec2 = vec2.normalize();
                    Vec3d vec3 = vec2.multiply(0.4 * d);
                    double e = Math.min(0.4, 0.4 * d);
                    if (bl) {
                        double d2 = Math.toRadians(f);
                        Vec3d vec4 = new Vec3d(-Math.sin(d2), 0.0, Math.cos(d2));
                        vec3 = vec3.multiply(0.5).add(vec4.multiply(0.5 * d));
                        e = Math.min(0.4, e / 2.0 + 0.5 * d);
                    }
                    this.kick = new Vec3d(vec3.x, e, vec3.z);
                    this.kickWay = vec3.lengthSquared() < 1.0E-6 ? vec2 : vec3.normalize();
                    this.kickFrom = vec;
                    this.kickAt = kickAt2;
                    this.kickSpoiled = false;
                    this.kickSeen = false;
                    this.early = null;
                    World entityWorld = player2.getEntityWorld();
                    AirTrack.Stop stop =
                            fly(entityWorld, player2, vec, this.kick, Vec3d.ZERO, true, 40);
                    AirTrack.Stop stop2 =
                            fly(
                                    entityWorld,
                                    player2,
                                    vec,
                                    this.kick,
                                    this.kickWay.multiply(0.01),
                                    true,
                                    40);
                    this.bare = stop.down() ? new AirTrack.Landing(stop.at(), stop.ticks()) : null;
                    this.gain =
                            stop.down() && stop2.down()
                                    ? stop2.at().subtract(stop.at()).dotProduct(this.kickWay) / 0.01
                                    : 0.0;
                    AirTrack.Stop stop3 =
                            fly(entityWorld, player2, vec, this.kick, this.learned(0.1), true, 40);
                    this.guessed =
                            stop3.down() ? new AirTrack.Landing(stop3.at(), stop3.ticks()) : null;
                }
            }
        }
    }

    void blast(Vec3d vec) {
        if (this.target != null && !(server(this.target).distanceTo(vec) > 10.0)) {
            this.kickSpoiled = true;
            if (!this.kickSeen) {
                this.kickAt = -1;
            }
            this.restart();
        }
    }

    AirTrack.Landing landing(int i) {
        AirTrack.Stop stop = this.path(40, i);
        return stop != null && stop.down() ? new AirTrack.Landing(stop.at(), stop.ticks()) : null;
    }

    Vec3d lead(int i, int j) {
        if (this.target == null || i <= 0) {
            return null;
        } else if (this.kickAt < 0 && this.pushes < 2) {
            return null;
        } else {
            AirTrack.Stop stop = this.path(i, j);
            return stop == null ? null : stop.at().subtract(this.target.getEntityPos());
        }
    }

    private AirTrack.Stop path(int i, int j) {
        if (this.target != null && !floating(this.target)) {
            World entityWorld = this.target.getEntityWorld();
            Vec3d vec = this.airborne ? this.velocity() : null;
            if (this.kickAt < 0 || this.kickSeen && vec != null) {
                if (vec == null) {
                    return null;
                } else {
                    AirTrack.Sample sample = this.flight.get(this.flight.size() - 1);
                    int k = Math.min(3, j - sample.tick());
                    AirTrack.Stop stop =
                            fly(
                                    entityWorld,
                                    this.target,
                                    sample.at(),
                                    vec,
                                    this.push(vec),
                                    false,
                                    i + k);
                    return new AirTrack.Stop(stop.at(), Math.max(0, stop.ticks() - k), stop.down());
                }
            } else {
                int m = this.kickSeen ? j - this.kickSeenAt + 1 : 0;
                AirTrack.Stop stop2 =
                        fly(
                                entityWorld,
                                this.target,
                                this.kickFrom,
                                this.kick,
                                this.learned(0.1),
                                true,
                                i + m);
                return new AirTrack.Stop(stop2.at(), Math.max(0, stop2.ticks() - m), stop2.down());
            }
        } else {
            return null;
        }
    }

    private Vec3d push(Vec3d vec) {
        if (this.pushes >= 2) {
            return limit(this.pushSum.multiply(1.0 / this.pushes), 0.045);
        } else {
            boolean bl = this.kickAt >= 0 && this.rival != null && this.rival.knowsFlight();
            Vec3d vec2 =
                    this.kickAt >= 0
                            ? this.learned(0.026)
                            : limit(
                                    new Vec3d(vec.x, 0.0, vec.z).multiply(0.08999999999999997),
                                    0.026);
            Vec3d vec3 =
                    this.pushes == 1
                            ? (this.early != null
                                    ? this.pushSum.add(this.early).multiply(0.5)
                                    : this.pushSum)
                            : this.early;
            return vec3 == null ? vec2 : limit(bl ? vec3.add(vec2).multiply(0.5) : vec3, 0.045);
        }
    }

    private Vec3d learned(double d) {
        return this.rival != null && this.rival.knowsFlight() && this.kickWay != null
                ? limit(
                        this.kickWay
                                .multiply(this.rival.airAlong)
                                .add(right(this.kickWay).multiply(this.rival.airAcross)),
                        d)
                : Vec3d.ZERO;
    }

    private void learn(Vec3d vec) {
        if (this.rival != null && !this.kickSpoiled && this.bare != null && !(this.gain < 1.0)) {
            if (!(Math.abs(vec.y - this.bare.at().y) > 0.6)) {
                Vec3d vec2 = vec.subtract(this.bare.at());
                double d = vec2.dotProduct(this.kickWay) / this.gain;
                double e = vec2.dotProduct(right(this.kickWay)) / this.gain;
                if (!(Math.abs(d) > 0.15000000000000002) && !(Math.abs(e) > 0.15000000000000002)) {
                    this.rival.flew(d, e);
                }
            }
        }
    }

    private void read() {
        int i = this.flight.size();
        if (i >= 3) {
            AirTrack.Sample sample = this.flight.get(i - 3);
            AirTrack.Sample sample2 = this.flight.get(i - 2);
            AirTrack.Sample sample3 = this.flight.get(i - 1);
            int j = sample2.tick() - sample.tick();
            int k = sample3.tick() - sample2.tick();
            if (j > 0 && k > 0 && j <= 4 && k <= 4) {
                Vec3d vec = sample2.at().subtract(sample.at()).multiply(1.0 / j);
                Vec3d vec2 = sample3.at().subtract(sample2.at()).multiply(1.0 / k);
                double d = Math.pow(0.91, (j + k) / 2.0);
                double e;
                Vec3d vec3 =
                        new Vec3d(
                                (vec2.x - d * vec.x) * (e = 0.08999999999999997 / (1.0 - d)),
                                0.0,
                                (vec2.z - d * vec.z) * e);
                if (!(vec3.horizontalLength() > 0.045)) {
                    this.pushSum = this.pushSum.add(vec3);
                    this.pushes++;
                }
            }
        }
    }

    private void readEarly() {
        if (this.kickAt >= 0 && this.kickSeen && !this.kickSpoiled) {
            AirTrack.Sample sample = this.flight.get(0);
            AirTrack.Sample sample2 = this.flight.get(1);
            int i = sample2.tick() - sample.tick();
            if (i > 0 && i <= 4) {
                Vec3d vec = sample2.at().subtract(sample.at()).multiply(1.0 / i);
                double d = friction(this.target.getEntityWorld(), this.kickFrom) * 0.91;
                byte b = 20;
                double[] doubles = new double[b + 1];
                double[] doubles2 = new double[b + 1];
                double[] doubles3 = new double[b + 1];
                double e = this.kick.y;
                double d2 = 1.0;
                double e2 = 1.0;
                for (int j = 1; j <= b; j++) {
                    if (j > 1) {
                        double d3 = j == 2 ? d : 0.91;
                        d2 *= d3;
                        e2 = e2 * d3 + 1.0;
                        e = (e - 0.08) * 0.98;
                    }
                    doubles[j] = e;
                    doubles2[j] = d2;
                    doubles3[j] = e2;
                }
                int k = -1;
                double e3 = 1.7976931348623157E308;
                for (int m = i; m <= b; m++) {
                    double d4 = Math.abs(window(doubles, m, i) - vec.y);
                    if (d4 < e3) {
                        e3 = d4;
                        k = m;
                    }
                }
                if (k >= 0 && !(e3 > 0.04)) {
                    double window = window(doubles2, k, i);
                    Vec3d vec2 =
                            new Vec3d(
                                            vec.x - this.kick.x * window,
                                            0.0,
                                            vec.z - this.kick.z * window)
                                    .multiply(1.0 / window(doubles3, k, i));
                    if (vec2.horizontalLength() <= 0.1) {
                        this.early = vec2;
                    }
                }
            }
        }
    }

    private static double window(double[] doubles, int i, int j) {
        double d = 0.0;
        for (int k = i - j + 1; k <= i; k++) {
            d += doubles[k];
        }
        return d / j;
    }

    private Vec3d velocity() {
        int i = this.flight.size();
        if (i < 2) {
            return null;
        } else {
            AirTrack.Sample sample = this.flight.get(i - 2);
            AirTrack.Sample sample2 = this.flight.get(i - 1);
            int j = sample2.tick() - sample.tick();
            if (j > 0 && j <= 4) {
                Vec3d vec = sample2.at().subtract(sample.at()).multiply(1.0 / j);
                double d = (j - 1) / 2.0;
                double e = Math.pow(0.91, d);
                return new Vec3d(vec.x * e, vec.y - 0.0784 * d, vec.z * e);
            } else {
                return null;
            }
        }
    }

    private void restart() {
        this.flight.clear();
        this.pushSum = Vec3d.ZERO;
        this.pushes = 0;
        this.early = null;
    }

    private static AirTrack.Stop fly(
            World world,
            PlayerEntity player,
            Vec3d vec,
            Vec3d vec2,
            Vec3d vec3,
            boolean bl,
            int i) {
        Box box = player.getBoundingBox().offset(vec.subtract(player.getEntityPos()));
        double d = bl ? friction(world, vec) * 0.91 : 0.91;
        double e = vec2.x;
        double d2 = vec2.y;
        double e2 = vec2.z;
        Vec3d vec4 = vec;
        for (int j = 1; j <= i; j++) {
            if (j > 1 || !bl) {
                double d3 = bl && j == 2 ? d : 0.91;
                e *= d3;
                e2 *= d3;
                d2 = (d2 - 0.08) * 0.98;
            }
            Vec3d vec5 =
                    Entity.adjustMovementForCollisions(
                            player,
                            new Vec3d(e += vec3.x, d2, e2 += vec3.z),
                            box,
                            world,
                            List.of());
            box = box.offset(vec5);
            vec4 = vec4.add(vec5);
            if (vec5.x != e) {
                e = 0.0;
            }
            if (vec5.z != e2) {
                e2 = 0.0;
            }
            if (vec5.y != d2) {
                if (d2 < 0.0) {
                    return new AirTrack.Stop(vec4, j, true);
                }
                d2 = 0.0;
            }
        }
        return new AirTrack.Stop(vec4, i, false);
    }

    private static double friction(World world, Vec3d vec) {
        return world.getBlockState(BlockPos.ofFloored(vec.x, vec.y - 0.5000001, vec.z))
                .getBlock()
                .getSlipperiness();
    }

    private static double resistance(PlayerEntity player) {
        double[] doubles = new double[] {0.0};
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            if (equipmentSlot.getType() == Type.HUMANOID_ARMOR) {
                player.getEquippedStack(equipmentSlot)
                        .applyAttributeModifiers(
                                equipmentSlot,
                                (registryEntry, entityAttributeModifier) -> {
                                    if (registryEntry.value()
                                                    == EntityAttributes.KNOCKBACK_RESISTANCE.value()
                                            && entityAttributeModifier.operation()
                                                    == Operation.ADD_VALUE) {
                                        doubles[0] += entityAttributeModifier.value();
                                    }
                                });
            }
        }
        return Math.min(1.0, Math.max(0.0, doubles[0]));
    }

    private static boolean floating(PlayerEntity player) {
        return player.isGliding()
                || player.isInFluid()
                || player.isClimbing()
                || player.hasVehicle()
                || player.getAbilities().flying
                || player.hasStatusEffect(StatusEffects.LEVITATION)
                || player.hasStatusEffect(StatusEffects.SLOW_FALLING);
    }

    private static Vec3d server(PlayerEntity player) {
        PositionInterpolator interpolator = player.getInterpolator();
        return interpolator == null ? player.getEntityPos() : interpolator.getLerpedPos();
    }

    private static Vec3d right(Vec3d vec) {
        return new Vec3d(-vec.z, 0.0, vec.x);
    }

    private static Vec3d limit(Vec3d vec, double d) {
        double horizontalLength = vec.horizontalLength();
        return horizontalLength <= d ? vec : vec.multiply(d / horizontalLength);
    }

    @Environment(EnvType.CLIENT)
    record Landing(Vec3d at, int ticks) {}

    @Environment(EnvType.CLIENT)
    private record Sample(int tick, Vec3d at) {}

    @Environment(EnvType.CLIENT)
    private record Stop(Vec3d at, int ticks, boolean down) {}
}
