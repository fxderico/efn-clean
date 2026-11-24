package net.efn.client.event.impl;

import net.efn.client.event.CancellableEvent;
import net.efn.client.event.PooledCancellableEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.Packet;

@Environment(EnvType.CLIENT)
public final class PacketReceiveNettyEvent extends CancellableEvent {

    public static final PooledCancellableEventType<PacketReceiveNettyEvent> TYPE =
            new PooledCancellableEventType<>(
                    "PacketReceiveNettyEvent", PacketReceiveNettyEvent::new, 1024);

    public Packet<?> packet;

    private PacketReceiveNettyEvent() {}

    public static boolean dispatchSync(Packet<?> packet2) {
        if (!TYPE.hasPreListeners()) {
            return false;
        } else {
            PacketReceiveNettyEvent event = TYPE.pool.acquire();
            event.packet = packet2;
            event.resetCancellation();
            boolean bl;
            try {
                TYPE.dispatch(event);
                boolean cancelled = event.isCancelled();
                bl = cancelled;
            } finally {
                event.packet = null;
                TYPE.pool.release(event);
            }
            return bl;
        }
    }
}
