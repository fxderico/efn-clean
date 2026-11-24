package net.efn.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class EventType<E extends Event> {

    protected final ListenerList<E> list = new ListenerList<>();

    private final String name;

    public EventType() {
        this("unnamed");
    }

    public EventType(String name2) {
        this.name = name2;
    }

    public final String name() {
        return this.name;
    }

    public void register(ListenerHandle<E> listenerHandle) {
        this.list.register(listenerHandle);
    }

    public void unregister(ListenerHandle<E> listenerHandle) {
        this.list.unregister(listenerHandle);
    }

    public ListenerHandle<E> listen(Object object, Consumer<E> consumer) {
        return this.listen(object, Stage.PRE, EventPriority.NORMAL, consumer);
    }

    public ListenerHandle<E> listen(
            Object object, EventPriority eventPriority, Consumer<E> consumer) {
        return this.listen(object, Stage.PRE, eventPriority, consumer);
    }

    public ListenerHandle<E> listen(
            Object object, Stage stage, EventPriority eventPriority, Consumer<E> consumer) {
        ListenerHandle listenerHandle =
                new ListenerHandle<>(this, stage, eventPriority, object, consumer);
        this.register(listenerHandle);
        return listenerHandle;
    }

    public boolean hasPreListeners() {
        return this.list.preCallbacks().length != 0;
    }

    public boolean hasPostListeners() {
        return this.list.postCallbacks().length != 0;
    }

    public boolean hasListeners() {
        return this.hasPreListeners() || this.hasPostListeners();
    }

    public final void dispatch(E e) {
        this.dispatchPre((E) e);
    }

    public void dispatchPre(E e) {
        this.list.dispatchPre((E) e);
    }

    public void dispatchPost(E e) {
        this.list.dispatchPost((E) e);
    }
}
