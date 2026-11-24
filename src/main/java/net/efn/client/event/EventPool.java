package net.efn.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class EventPool<E extends Event> {

    private final ArrayBlockingQueue<E> pool;

    private final Supplier<E> factory;

    public EventPool(Supplier<E> factory2, int i) {
        this.factory = factory2;
        this.pool = new ArrayBlockingQueue<>(i);
    }

    public E acquire() {
        Event event = this.pool.poll();
        if (event == null) {
            event = this.factory.get();
        }
        return (E) event;
    }

    public void release(E e) {
        this.pool.offer((E) e);
    }
}
