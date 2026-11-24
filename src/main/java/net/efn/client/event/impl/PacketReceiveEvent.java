package net.efn.client.event.impl;

import net.efn.client.event.Event;
import net.efn.client.event.EventBus;
import net.efn.client.event.PooledEventType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.Packet;

import java.util.function.BooleanSupplier;

@Environment(EnvType.CLIENT)
public final class PacketReceiveEvent extends Event implements Runnable {

    public static final PooledEventType<PacketReceiveEvent> TYPE =
            new PooledEventType<>("PacketReceiveEvent", PacketReceiveEvent::new, 1024);

    public Packet<?> packet;

    private BooleanSupplier connectionActive;

    private PacketReceiveEvent() {}

    public static void dispatchAsync(Packet<?> packet2) {
        dispatchAsync(packet2, null);
    }

    public static void dispatchAsync(Packet<?> packet2, BooleanSupplier booleanSupplier) {
        if (TYPE.hasListeners()) {
            PacketReceiveEvent event = TYPE.pool.acquire();
            event.packet = packet2;
            event.connectionActive = booleanSupplier;
            if (!EventBus.INSTANCE.postAsync(event)) {
                event.release();
            }
        }
    }

    @Override
    public void run() {
        try {
            if (this.connectionActive == null || this.connectionActive.getAsBoolean()) {
                TYPE.dispatch(this);
                TYPE.dispatchPost(this);
                return;
            }
        } finally {
            this.release();
        }
    }

    private void release() {
        this.packet = null;
        this.connectionActive = null;
        TYPE.pool.release(this);
    }
}
