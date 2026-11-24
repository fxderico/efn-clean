package net.efn.client.event.impl;

import net.efn.client.event.CancellableEvent;
import net.efn.client.event.PooledCancellableEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class PlayerMoveEvent extends CancellableEvent {

    public static final PooledCancellableEventType<PlayerMoveEvent> TYPE =
            new PooledCancellableEventType<>("PlayerMoveEvent", PlayerMoveEvent::new, 16);

    public double x;

    public double y;

    public double z;

    private PlayerMoveEvent() {}

    public static void move(Vec3d vec, Consumer<Vec3d> consumer) {
        if (!TYPE.hasListeners()) {
            consumer.accept(vec);
        } else {
            PlayerMoveEvent event = TYPE.pool.acquire();
            event.x = vec.x;
            event.y = vec.y;
            event.z = vec.z;
            event.resetCancellation();
            try {
                TYPE.dispatchPre(event);
                if (event.isCancelled()) {
                    return;
                }
                if (Double.isFinite(event.x)
                        && Double.isFinite(event.y)
                        && Double.isFinite(event.z)) {
                    Vec3d vec2 =
                            event.x == vec.x && event.y == vec.y && event.z == vec.z
                                    ? vec
                                    : new Vec3d(event.x, event.y, event.z);
                    consumer.accept(vec2);
                    TYPE.dispatchPost(event);
                    return;
                }
            } finally {
                TYPE.pool.release(event);
            }
        }
    }
}
