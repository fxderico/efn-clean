package net.efn.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class ListenerList<E extends Event> {

    private static final Consumer[] NONE = new Consumer[0];

    private volatile Consumer<E>[] pre = none();

    private volatile Consumer<E>[] post = none();

    private final ListenerList.Handles<E> preHandles = new ListenerList.Handles<>();

    private final ListenerList.Handles<E> postHandles = new ListenerList.Handles<>();

    public ListenerList() {}

    public synchronized void register(ListenerHandle<E> listenerHandle) {
        if (listenerHandle.stage() == Stage.PRE) {
            this.pre = this.preHandles.insert(this.pre, listenerHandle);
        } else {
            this.post = this.postHandles.insert(this.post, listenerHandle);
        }
    }

    public synchronized void unregister(ListenerHandle<E> listenerHandle) {
        if (listenerHandle.stage() == Stage.PRE) {
            this.pre = this.preHandles.remove(this.pre, listenerHandle);
        } else {
            this.post = this.postHandles.remove(this.post, listenerHandle);
        }
    }

    public void dispatchPre(E e) {
        Consumer[] pre2 = this.pre;
        for (int i = 0; i < pre2.length; i++) {
            try {
                pre2[i].accept(e);
            } catch (Exception exception) {
                ListenerErrorReporter.report(this.handleOf(Stage.PRE, pre2[i]), e, exception);
            }
        }
    }

    public void dispatchPost(E e) {
        Consumer[] post2 = this.post;
        for (int i = 0; i < post2.length; i++) {
            try {
                post2[i].accept(e);
            } catch (Exception exception) {
                ListenerErrorReporter.report(this.handleOf(Stage.POST, post2[i]), e, exception);
            }
        }
    }

    Consumer<E>[] preCallbacks() {
        return this.pre;
    }

    Consumer<E>[] postCallbacks() {
        return this.post;
    }

    synchronized ListenerHandle<E> handleOf(Stage stage, Consumer<E> consumer) {
        ListenerHandle listenerHandle =
                stage == Stage.PRE
                        ? this.preHandles.of(this.pre, consumer)
                        : this.postHandles.of(this.post, consumer);
        return listenerHandle != null
                ? listenerHandle
                : new ListenerHandle<>(null, stage, EventPriority.NORMAL, null, consumer);
    }

    private static <E extends Event> Consumer<E>[] none() {
        return NONE;
    }

    @Environment(EnvType.CLIENT)
    private static final class Handles<E extends Event> {

        private ListenerHandle<E>[] handles = new ListenerHandle[4];

        private Handles() {}

        Consumer<E>[] insert(Consumer<E>[] consumers, ListenerHandle<E> listenerHandle) {
            int i = consumers.length;
            int value = listenerHandle.priority().value;
            int j = 0;
            int k = i;
            while (j < k) {
                int m = j + k >>> 1;
                if (this.handles[m].priority().value >= value) {
                    j = m + 1;
                } else {
                    k = m;
                }
            }
            if (i == this.handles.length) {
                this.handles = Arrays.copyOf(this.handles, i * 2);
            }
            System.arraycopy(this.handles, j, this.handles, j + 1, i - j);
            this.handles[j] = listenerHandle;
            Consumer[] consumers2 = new Consumer[i + 1];
            System.arraycopy(consumers, 0, consumers2, 0, j);
            consumers2[j] = listenerHandle.callback();
            System.arraycopy(consumers, j, consumers2, j + 1, i - j);
            return consumers2;
        }

        Consumer<E>[] remove(Consumer<E>[] consumers, ListenerHandle<E> listenerHandle) {
            int i = consumers.length;
            int j = this.indexOf(consumers, listenerHandle);
            if (j < 0) {
                return consumers;
            } else {
                System.arraycopy(this.handles, j + 1, this.handles, j, i - j - 1);
                this.handles[i - 1] = null;
                if (i == 1) {
                    return ListenerList.none();
                } else {
                    Consumer[] consumers2 = new Consumer[i - 1];
                    System.arraycopy(consumers, 0, consumers2, 0, j);
                    System.arraycopy(consumers, j + 1, consumers2, j, i - j - 1);
                    return consumers2;
                }
            }
        }

        private int indexOf(Consumer<E>[] consumers, ListenerHandle<E> listenerHandle) {
            Consumer consumer = listenerHandle.callback();
            for (int i = 0; i < consumers.length; i++) {
                if (consumers[i] == consumer && this.handles[i] == listenerHandle) {
                    return i;
                }
            }
            return -1;
        }

        ListenerHandle<E> of(Consumer<E>[] consumers, Consumer<E> consumer) {
            for (int i = 0; i < consumers.length; i++) {
                if (consumers[i] == consumer) {
                    return this.handles[i];
                }
            }
            return null;
        }
    }
}
