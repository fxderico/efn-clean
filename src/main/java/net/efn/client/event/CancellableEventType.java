package net.efn.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class CancellableEventType<E extends CancellableEvent> extends EventType<E> {

    public CancellableEventType() {}

    public CancellableEventType(String string) {
        super(string);
    }

    public void dispatchPre(E e) {
        dispatchPreCancellable(this.list, (E) e);
    }

    public void dispatchPost(E e) {
        dispatchPostGuarded(this.list, (E) e);
    }

    static <E extends CancellableEvent> void dispatchPreCancellable(
            ListenerList<E> listenerList, E e) {
        Consumer[] consumers = listenerList.preCallbacks();
        for (int i = 0; i < consumers.length; i++) {
            try {
                consumers[i].accept(e);
            } catch (Exception exception) {
                ListenerErrorReporter.report(
                        listenerList.handleOf(Stage.PRE, consumers[i]), e, exception);
            }
            if (e.cancelled) {
                break;
            }
        }
    }

    static <E extends CancellableEvent> void dispatchPostGuarded(
            ListenerList<E> listenerList, E e) {
        e.postStage = true;
        try {
            listenerList.dispatchPost(e);
        } finally {
            e.postStage = false;
        }
    }
}
