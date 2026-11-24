package net.efn.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

@Environment(EnvType.CLIENT)
public final class EventBus {

    public static final EventBus INSTANCE = new EventBus();

    private final Map<Object, List<EventBus.RegistrationEntry<?>>> subscriptions =
            new IdentityHashMap<>();

    private final ArrayBlockingQueue<Runnable> asyncQueue = new ArrayBlockingQueue<>(4096);

    private volatile long failedTasks;

    private static final MethodType FACTORY_TYPE =
            MethodType.methodType(Listener.class, Object.class);

    private static final ClassValue<List<EventBus.HandlerSpec>> HANDLER_SPECS =
            new ClassValue<List<EventBus.HandlerSpec>>() {

                protected List<EventBus.HandlerSpec> computeValue(Class<?> classValue) {
                    return EventBus.scan(classValue);
                }
            };

    private static final ClassValue<EventType<?>> TYPE_CACHE =
            new ClassValue<EventType<?>>() {

                protected EventType<?> computeValue(Class<?> classValue) {
                    try {
                        Field field = classValue.getField("TYPE");
                        if (!EventType.class.isAssignableFrom(field.getType())) {
                            throw new IllegalArgumentException(
                                    classValue.getName() + ".TYPE must be an EventType instance.");
                        } else {
                            return (EventType<?>) field.get(null);
                        }
                    } catch (NoSuchFieldException exception) {
                        throw new IllegalArgumentException(
                                "Event class "
                                        + classValue.getName()
                                        + " must declare 'public static final EventType<...>"
                                        + " TYPE'.",
                                exception);
                    } catch (IllegalAccessException exception2) {
                        throw new IllegalArgumentException(
                                "Cannot access " + classValue.getName() + ".TYPE", exception2);
                    }
                }
            };

    public EventBus() {}

    public boolean postAsync(Runnable runnable) {
        return this.asyncQueue.offer(Objects.requireNonNull(runnable));
    }

    public int drainAsync() {
        return this.drainAsync(512, 2000000L);
    }

    public int drainAsync(int i, long l) {
        if (i > 0 && l > 0L) {
            long l2 = System.nanoTime();
            int j = 0;
            Runnable async;
            while (j < i
                    && System.nanoTime() - l2 < l
                    && (async = this.asyncQueue.poll()) != null) {
                j++;
                try {
                    async.run();
                } catch (RuntimeException exception) {
                    long l3;
                    if ((l3 = ++this.failedTasks) <= 4L || l3 % 256L == 0L) {
                        System.err.println("[EfnEventBus] Async task failed (#" + l3 + ")");
                        exception.printStackTrace();
                    }
                }
            }
            return j;
        } else {
            throw new IllegalArgumentException("Drain limits must be positive");
        }
    }

    public <E extends Event> void off(ListenerHandle<E> listenerHandle) {
        listenerHandle.unregister();
    }

    public int register(Object object) {
        List list = HANDLER_SPECS.get(object.getClass());
        if (list.isEmpty()) {
            return 0;
        } else {
            synchronized (this) {
                return this.register(object, list);
            }
        }
    }

    private int register(Object object, List<EventBus.HandlerSpec> list) {
        if (this.subscriptions.containsKey(object)) {
            throw new IllegalStateException(
                    "Subscriber already registered: " + object.getClass().getName());
        } else {
            List entries = buildEntries(object, list);
            try {
                for (EventBus.RegistrationEntry entry :
                        (Iterable<EventBus.RegistrationEntry>) entries) {
                    entry.register();
                }
            } catch (RuntimeException | Error exception) {
                for (EventBus.RegistrationEntry entry2 :
                        (Iterable<EventBus.RegistrationEntry>) entries) {
                    try {
                        entry2.unregister();
                    } catch (RuntimeException | Error exception2) {
                        if (exception != exception2) {
                            exception.addSuppressed(exception2);
                        }
                    }
                }
                throw exception;
            }
            this.subscriptions.put(object, entries);
            return entries.size();
        }
    }

    public synchronized void unregister(Object object) {
        List subscription = this.subscriptions.remove(object);
        if (subscription != null) {
            for (EventBus.RegistrationEntry registrationEntry :
                    (Iterable<EventBus.RegistrationEntry>) subscription) {
                registrationEntry.unregister();
            }
        }
    }

    private static List<EventBus.RegistrationEntry<?>> buildEntries(
            Object object, List<EventBus.HandlerSpec> list) {
        ArrayList list2 = new ArrayList(list.size());
        for (EventBus.HandlerSpec handlerSpec : list) {
            Listener listener = handlerSpec.staticListener();
            if (listener == null) {
                try {
                    listener =
                            castListener(
                                    (Listener)
                                            handlerSpec
                                                    .listenerFactory()
                                                    .invokeExact((Object) object));
                } catch (Throwable exception) {
                    throw new RuntimeException(
                            "Failed to bind listener for " + object.getClass().getName(),
                            exception);
                }
            }
            ListenerHandle listenerHandle =
                    new ListenerHandle<>(
                            handlerSpec.type(),
                            handlerSpec.stage(),
                            handlerSpec.priority(),
                            object,
                            listener::invoke);
            list2.add(new EventBus.RegistrationEntry<>(handlerSpec.type(), listenerHandle));
        }
        return list2;
    }

    private static Listener<Event> castListener(Listener<?> listener) {
        return (Listener<Event>) (Listener) listener;
    }

    private static List<EventBus.HandlerSpec> scan(Class<?> classValue) {
        ArrayList list = new ArrayList();
        for (Class classValue2 = classValue;
                classValue2 != null;
                classValue2 = classValue2.getSuperclass()) {
            for (Method declaredMethod : classValue2.getDeclaredMethods()) {
                if (isHandler(declaredMethod)
                        && !Modifier.isPublic(declaredMethod.getModifiers())) {
                    throw new IllegalArgumentException(
                            "Event handler must be public: " + declaredMethod);
                }
            }
        }
        for (Method method : classValue.getMethods()) {
            Stage pRE = Stage.PRE;
            EventPriority nORMAL = EventPriority.NORMAL;
            boolean bl = false;
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            if (annotation != null) {
                pRE = annotation.stage();
                nORMAL = annotation.priority();
                bl = true;
            } else {
                EventListener annotation2 = method.getAnnotation(EventListener.class);
                if (annotation2 != null) {
                    pRE = Stage.PRE;
                    nORMAL = EventPriority.fromInt(annotation2.priority());
                    bl = true;
                }
            }
            if (bl) {
                Class[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1
                        || !Event.class.isAssignableFrom(parameterTypes[0])
                        || method.getReturnType() != void.class) {
                    throw new IllegalArgumentException(
                            "Event handler must return void and accept one Event: " + method);
                }
                Class parameterType = parameterTypes[0];
                EventType type2 = resolveTypeFor(parameterType);
                if (Modifier.isStatic(method.getModifiers())) {
                    list.add(
                            new EventBus.HandlerSpec(
                                    type2,
                                    pRE,
                                    nORMAL,
                                    null,
                                    LambdaListenerFactory.createStatic(method)));
                } else {
                    list.add(
                            new EventBus.HandlerSpec(
                                    type2,
                                    pRE,
                                    nORMAL,
                                    LambdaListenerFactory.createFactory(method)
                                            .asType(FACTORY_TYPE),
                                    null));
                }
            }
        }
        return list.isEmpty() ? List.of() : List.copyOf(list);
    }

    private static boolean isHandler(Method method) {
        return method.isAnnotationPresent(EventHandler.class)
                || method.isAnnotationPresent(EventListener.class);
    }

    static <E extends Event> EventType<E> resolveTypeFor(Class<E> classValue) {
        return (EventType<E>) TYPE_CACHE.get(classValue);
    }

    @Environment(EnvType.CLIENT)
    private record HandlerSpec(
            EventType<Event> type,
            Stage stage,
            EventPriority priority,
            MethodHandle listenerFactory,
            Listener<Event> staticListener) {}

    @Environment(EnvType.CLIENT)
    private record RegistrationEntry<E extends Event>(EventType<E> type, ListenerHandle<E> handle) {

        void register() {
            this.type.register(this.handle);
        }

        void unregister() {
            this.type.unregister(this.handle);
        }
    }
}
