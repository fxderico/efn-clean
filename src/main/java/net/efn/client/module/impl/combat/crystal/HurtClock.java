package net.efn.client.module.impl.combat.crystal;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public final class HurtClock {

    private static final int NEVER = -1000;

    private static final int WINDOW = 10;

    private static final int MAX_WAIT = 20;

    private static final int MAX_MARGIN = 2;

    private static final int RELAX_STREAK = 8;

    private static final int REMOVE_GRACE = 3;

    private static final double ANCHOR_MATCH_SQR = 0.25;

    private static final Int2ObjectOpenHashMap<HurtClock.Mark> MARKS =
            new Int2ObjectOpenHashMap<>();

    private static ClientPlayerEntity lastPlayer;

    private static World lastLevel;

    private static Packet<?> lastPacket;

    private static int now;

    private static int margin;

    private static int streak;

    private HurtClock() {}

    public static void note(ClientPlayerEntity player, Packet<?> packet) {
        if (packet != lastPacket) {
            lastPacket = packet;
            if (packet instanceof BundleS2CPacket bundlePacket) {
                for (Packet packet2 : bundlePacket.getPackets()) {
                    handle(player, packet2);
                }
            } else {
                handle(player, packet);
            }
        }
    }

    public static void tick(ClientPlayerEntity player) {
        now = player.age;
        if (player != lastPlayer || player.getEntityWorld() != lastLevel) {
            MARKS.clear();
            margin = 0;
            streak = 0;
            lastPlayer = player;
            lastLevel = player.getEntityWorld();
        }
        ObjectIterator objectIterator = MARKS.int2ObjectEntrySet().iterator();
        while (objectIterator.hasNext()) {
            Entry entry = (Entry) objectIterator.next();
            Entity entityById = lastLevel.getEntityById(entry.getIntKey());
            LivingEntity entity;
            if (entityById instanceof LivingEntity
                    && (entity = (LivingEntity) entityById).isAlive()) {
                HurtClock.Mark entry2 = (HurtClock.Mark) entry.getValue();
                if (entry2.removedAt != -1000 && !entry2.confirmed && now - entry2.removedAt > 3) {
                    margin = Math.min(2, margin + 1);
                    streak = 0;
                    entry2.shotTick = -1000;
                    entry2.removedAt = -1000;
                }
            } else {
                objectIterator.remove();
            }
        }
    }

    public static boolean freshAt(LivingEntity entity, int i, int j) {
        if (entity.timeUntilRegen - i - j > 10) {
            return false;
        } else {
            HurtClock.Mark mark = MARKS.get(entity.getId());
            return mark == null || mark.shotTick == -1000 || now + i - mark.shotTick >= 10 + margin;
        }
    }

    public static int opensIn(LivingEntity entity, int i) {
        for (int j = 0; j < 20; j++) {
            if (freshAt(entity, j, i)) {
                return j;
            }
        }
        return 20;
    }

    public static float effective(CrystalDamage.Victim victim, float f, int i, int j) {
        LivingEntity entity = victim.entity();
        if (freshAt(entity, i, j)) {
            return victim.reduce(f);
        } else {
            HurtClock.Mark mark = MARKS.get(entity.getId());
            float g = mark == null ? 3.4028235E38F : mark.lastRaw;
            return f > g ? victim.reduce(f - g) : 0.0F;
        }
    }

    public static void shot(
            ClientPlayerEntity player, LivingEntity entity, float f, Object object, int i) {
        HurtClock.Mark mark = MARKS.computeIfAbsent(entity.getId(), ix -> new HurtClock.Mark());
        if (freshAt(entity, 0, i)) {
            mark.shotTick = player.age;
            mark.shotRaw = f;
            mark.lastRaw = f;
            mark.shotKey = object;
            mark.confirmed = false;
            mark.removedAt = -1000;
        } else {
            mark.lastRaw = Math.max(mark.lastRaw, f);
        }
    }

    private static void handle(ClientPlayerEntity player, Packet<?> packet) {
        if (packet instanceof EntityDamageS2CPacket entityDamagePacket) {
            damaged(player, entityDamagePacket);
        } else if (packet instanceof EntitiesDestroyS2CPacket entitiesDestroyPacket) {
            removed(entitiesDestroyPacket.getEntityIds());
        }
    }

    private static void damaged(
            ClientPlayerEntity player, EntityDamageS2CPacket entityDamagePacket) {
        Entity entityById = player.getEntityWorld().getEntityById(entityDamagePacket.entityId());
        PlayerEntity player2;
        if (entityById instanceof PlayerEntity
                && MeleeAttacks.isEnemy(player, player2 = (PlayerEntity) entityById)) {
            HurtClock.Mark mark = MARKS.computeIfAbsent(player2.getId(), i -> new HurtClock.Mark());
            if (mark.shotTick != -1000
                    && !mark.confirmed
                    && ours(player, mark, entityDamagePacket)) {
                mark.lastRaw = mark.shotRaw;
                mark.confirmed = true;
                if (mark.shotKey instanceof Integer && ++streak >= 8) {
                    margin = Math.max(0, margin - 1);
                    streak = 0;
                }
            } else {
                RegistryEntry entry = entityDamagePacket.sourceType();
                mark.shotTick = -1000;
                mark.removedAt = -1000;
                mark.lastRaw =
                        !entry.matchesKey(DamageTypes.PLAYER_ATTACK)
                                        && !entry.matchesKey(DamageTypes.MOB_ATTACK)
                                ? 3.4028235E38F
                                : 0.0F;
            }
        }
    }

    private static boolean ours(
            ClientPlayerEntity player,
            HurtClock.Mark mark,
            EntityDamageS2CPacket entityDamagePacket) {
        RegistryEntry entry = entityDamagePacket.sourceType();
        if (entry.matchesKey(DamageTypes.PLAYER_EXPLOSION)) {
            if (entityDamagePacket.sourceCauseId() != player.getId()) {
                return false;
            } else {
                return !(mark.shotKey instanceof Integer integer)
                        ? false
                        : entityDamagePacket.sourceDirectId() == integer;
            }
        } else if (!entry.matchesKey(DamageTypes.BAD_RESPAWN_POINT)) {
            return false;
        } else {
            return !(mark.shotKey instanceof Vec3d vec)
                    ? false
                    : entityDamagePacket
                            .sourcePosition()
                            .filter(vec3d2 -> vec3d2.squaredDistanceTo(vec) < 0.25)
                            .isPresent();
        }
    }

    private static void removed(IntList intList) {
        for (HurtClock.Mark mark : MARKS.values()) {
            if (mark.shotTick != -1000 && !mark.confirmed) {
                Object shotKey2 = mark.shotKey;
                Integer integer;
                if (mark.shotKey instanceof Integer
                        && intList.contains((integer = (Integer) shotKey2).intValue())) {
                    mark.removedAt = now;
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Mark {

        float lastRaw = 3.4028235E38F;

        int shotTick = -1000;

        float shotRaw;

        Object shotKey;

        boolean confirmed;

        int removedAt = -1000;

        private Mark() {}
    }
}
