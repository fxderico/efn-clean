/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.combat.crystal;

import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntFunction;

@Environment(value = EnvType.CLIENT)
public final class Combo {

    public static final double TARGET_RANGE = 10.0;

    public static final int MAX_TARGETS = 3;

    public static final float LETHAL_RANK = 1000.0f;

    public static final float POP_RANK = 500.0f;

    public static final float SELF_WEIGHT = 0.5f;

    public static final float ANCHOR_EDGE = 1.0f;

    private static final float BLAST_PROOF = 600.0f;

    private static final double FOCUS_BONUS = 2.0;

    private static final double WEAK_BONUS = 2.0;

    private static final float WEAK_HEALTH = 10.0f;

    private static final double MAX_STEP_SQR = 1.0;

    private static final double GRAVITY = 0.08;

    private static final double AIR_DRAG = 0.98;

    private static final Kind[] SETUP =
            new Kind[] {Kind.CHARGE, Kind.CRYSTAL, Kind.OBSIDIAN, Kind.ANCHOR};

    private static final Map<LivingEntity, Track> TRACKS = new WeakHashMap<LivingEntity, Track>();

    private static final double PREFER_BONUS = 6.0;

    private static LivingEntity focus;

    private static LivingEntity preferred;

    private static int preferredUntil;

    private static LivingEntity flying;

    private static IntFunction<Vec3d> flight;

    private static int flightUntil;

    private Combo() {}

    public static List<LivingEntity> enemies(MinecraftClient client, ClientPlayerEntity player) {
        ArrayList<LivingEntity> list = new ArrayList<LivingEntity>();
        for (AbstractClientPlayerEntity player2 : client.world.getPlayers()) {
            if (!MeleeAttacks.isEnemy(player, player2)
                    || !(player.squaredDistanceTo(player2) < 100.0)) continue;
            list.add(player2);
        }
        LivingEntity entity = player.age <= preferredUntil ? preferred : null;
        list.sort(
                Comparator.comparingDouble(
                        livingEntity2 ->
                                (double) player.distanceTo((Entity) livingEntity2)
                                        - (livingEntity2 == focus ? 2.0 : 0.0)
                                        - (livingEntity2 == entity ? 6.0 : 0.0)
                                        - (CrystalDamage.health(livingEntity2) <= 10.0f
                                                ? 2.0
                                                : 0.0)));
        focus = list.isEmpty() ? null : (LivingEntity) list.get(0);
        return list.size() > 3 ? list.subList(0, 3) : list;
    }

    public static void prefer(ClientPlayerEntity player, LivingEntity entity) {
        preferred = entity;
        preferredUntil = player.age + 2;
    }

    public static void flight(
            ClientPlayerEntity player, LivingEntity entity, IntFunction<Vec3d> intFunction) {
        flying = entity;
        flight = intFunction;
        flightUntil = player.age + 2;
    }

    public static List<LivingEntity> friends(MinecraftClient client, ClientPlayerEntity player) {
        ArrayList<LivingEntity> list = new ArrayList<LivingEntity>();
        for (AbstractClientPlayerEntity player2 : client.world.getPlayers()) {
            if (player2 == player
                    || !player2.isAlive()
                    || !MeleeAttacks.isFriend(player2)
                    || !(player.squaredDistanceTo(player2) < 100.0)) continue;
            list.add(player2);
        }
        return list;
    }

    public static Vec3d lead(LivingEntity entity, int i) {
        Vec3d vec;
        PositionInterpolator interpolator = entity.getInterpolator();
        Vec3d vec2 = interpolator == null ? entity.getEntityPos() : interpolator.getLerpedPos();
        Vec3d vec3 = vec2.subtract(entity.getEntityPos());
        int j = i;
        if (entity instanceof ClientPlayerEntity) {
            vec = entity.getEntityPos().subtract(entity.lastX, entity.lastY, entity.lastZ);
        } else {
            Track track = TRACKS.computeIfAbsent(entity, livingEntity -> new Track());
            track.note(entity, vec2);
            vec = track.velocity(entity);
            j += Math.min(track.age(entity), entity.getType().getTrackTickInterval());
            Vec3d vec4 = Combo.flown(entity, i);
            if (vec4 != null) {
                return vec4;
            }
        }
        if (i <= 0 || vec.horizontalLengthSquared() > 1.0) {
            return vec3;
        }
        return vec3.add(Combo.travel(entity, vec2, vec, j));
    }

    private static Vec3d flown(LivingEntity entity, int i) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (i <= 0
                || entity != flying
                || flight == null
                || player == null
                || player.age > flightUntil) {
            return null;
        }
        return flight.apply(i);
    }

    private static Vec3d travel(LivingEntity entity, Vec3d vec, Vec3d vec2, int i) {
        Box box = entity.getBoundingBox().offset(vec.subtract(entity.getEntityPos()));
        boolean bl =
                entity.isGliding()
                        || entity.isInFluid()
                        || entity.isClimbing()
                        || entity.hasNoGravity()
                        || entity.hasVehicle();
        double d = vec2.x;
        double e = vec2.y;
        double d2 = vec2.z;
        Vec3d zERO = Vec3d.ZERO;
        for (int j = 0; j < i; ++j) {
            if (!bl) {
                e = (e - 0.08) * 0.98;
            }
            Vec3d vec3 =
                    Entity.adjustMovementForCollisions(
                            entity, new Vec3d(d, e, d2), box, entity.getEntityWorld(), List.of());
            if (vec3.x != d) {
                d = 0.0;
            }
            if (vec3.y != e) {
                e = 0.0;
            }
            if (vec3.z != d2) {
                d2 = 0.0;
            }
            box = box.offset(vec3);
            zERO = zERO.add(vec3);
        }
        return zERO;
    }

    public static boolean inHole(LivingEntity entity) {
        BlockPos pos = entity.getBlockPos();
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockState blockState = entity.getEntityWorld().getBlockState(pos.offset(direction));
            if (!(blockState.getBlock().getBlastResistance() < 600.0f)) continue;
            return false;
        }
        return true;
    }

    public static boolean kills(float f, LivingEntity entity) {
        return f >= CrystalDamage.health(entity);
    }

    public static float value(float f, LivingEntity entity) {
        float health = CrystalDamage.health(entity);
        float g = f < health ? 0.0f : (entity.isHolding(Items.TOTEM_OF_UNDYING) ? 500.0f : 1000.0f);
        return Math.min(f, health) + g;
    }

    public static Shot pick(List<Shot> list) {
        Shot shot = Combo.best(list, Kind.BREAK);
        Shot shot2 = Combo.best(list, Kind.DETONATE);
        if (shot != null || shot2 != null) {
            boolean bl =
                    shot == null
                            || shot2 != null
                                    && (shot2.value() > shot.value() + 1.0f
                                            || shot2.kills() && !shot.kills());
            return bl ? shot2 : shot;
        }
        for (Kind kind2 : SETUP) {
            Shot shot3 = Combo.best(list, kind2);
            if (shot3 == null) continue;
            return shot3;
        }
        return null;
    }

    private static Shot best(List<Shot> list, Kind kind2) {
        Shot shot = null;
        for (Shot shot2 : list) {
            if (shot2.kind() != kind2
                    || shot != null
                            && !(shot2.value() > shot.value())
                            && (shot2.value() != shot.value() || shot2.readyIn() >= shot.readyIn()))
                continue;
            shot = shot2;
        }
        return shot;
    }

    @Environment(value = EnvType.CLIENT)
    private static final class Track {

        private Vec3d last;

        private int lastAt;

        private int sampledAt;

        private Vec3d velocity = Vec3d.ZERO;

        private Track() {}

        void note(LivingEntity entity, Vec3d last2) {
            int age = entity.age;
            if (age < this.sampledAt) {
                this.last = null;
            }
            boolean bl = age - this.sampledAt <= 1;
            this.sampledAt = age;
            if (last2.equals(this.last)) {
                return;
            }
            int trackTickInterval = entity.getType().getTrackTickInterval();
            int i = bl ? trackTickInterval : Math.max(trackTickInterval, age - this.lastAt);
            this.velocity =
                    this.last == null
                            ? Vec3d.ZERO
                            : last2.subtract(this.last).multiply(1.0 / (double) i);
            this.last = last2;
            this.lastAt = age;
        }

        int age(LivingEntity entity) {
            return entity.age - this.lastAt;
        }

        Vec3d velocity(LivingEntity entity) {
            return this.age(entity) > entity.getType().getTrackTickInterval() + 1
                    ? Vec3d.ZERO
                    : this.velocity;
        }
    }

    @Environment(value = EnvType.CLIENT)
    public static enum Kind {
        BREAK,
        CRYSTAL,
        OBSIDIAN,
        ANCHOR,
        CHARGE,
        DETONATE
    }

    @Environment(value = EnvType.CLIENT)
    public record Shot(
            Kind kind,
            Vec3d aim,
            int route,
            float value,
            boolean kills,
            int readyIn,
            Object plan) {}
}
