package net.efn.client.util.combat;

import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PlayerAttackEntityEvent;
import net.efn.client.event.impl.TickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class CombatTarget {

    private static final Object OWNER = new Object();

    private static final long MEMORY_CAP_MS = 30000L;

    private static LivingEntity lastHit;

    private static long lastHitAt;

    private CombatTarget() {}

    public static void install() {
        PlayerAttackEntityEvent.TYPE.listen(
                OWNER,
                Stage.PRE,
                EventPriority.LOWEST,
                playerAttackEntityEvent -> {
                    if (!playerAttackEntityEvent.isCancelled()) {
                        Entity target = playerAttackEntityEvent.target;
                        if (playerAttackEntityEvent.target instanceof LivingEntity) {
                            LivingEntity entity = (LivingEntity) target;
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.isOnThread() && entity.getEntityWorld() == client.world) {
                                lastHit = entity;
                                lastHitAt = Util.getMeasuringTimeMs();
                                return;
                            }
                            return;
                        }
                    }
                });
        TickEvent.TYPE.listen(OWNER, tickEvent -> forgetStale());
    }

    private static void forgetStale() {
        LivingEntity entity = lastHit;
        if (entity != null) {
            if (!entity.isAlive()
                    || entity.getEntityWorld() != MinecraftClient.getInstance().world
                    || Util.getMeasuringTimeMs() - lastHitAt > 30000L) {
                lastHit = null;
            }
        }
    }

    public static LivingEntity lastHit(long l) {
        LivingEntity entity = lastHit;
        if (entity != null
                && entity.isAlive()
                && entity.getEntityWorld() == MinecraftClient.getInstance().world) {
            return Util.getMeasuringTimeMs() - lastHitAt > l ? null : entity;
        } else {
            return null;
        }
    }

    public static LivingEntity current(long l) {
        LivingEntity entity = lastHit(l);
        if (entity != null) {
            return entity;
        } else {
            Entity targetedEntity = MinecraftClient.getInstance().targetedEntity;
            LivingEntity entity2;
            return targetedEntity instanceof LivingEntity
                            && (entity2 = (LivingEntity) targetedEntity).isAlive()
                    ? entity2
                    : null;
        }
    }
}
