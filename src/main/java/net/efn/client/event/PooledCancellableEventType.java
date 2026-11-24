package net.efn.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class PooledCancellableEventType<E extends CancellableEvent> extends PooledEventType<E> {

    public PooledCancellableEventType(Supplier<E> supplier, int i) {
        super(supplier, i);
    }

    public PooledCancellableEventType(String string, Supplier<E> supplier, int i) {
        super(string, supplier, i);
    }

    public void dispatchPre(E e) {
        CancellableEventType.dispatchPreCancellable(this.list, (E) e);
    }

    public void dispatchPost(E e) {
        CancellableEventType.dispatchPostGuarded(this.list, (E) e);
    }
}
