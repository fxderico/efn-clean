package net.efn.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaConversionException;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

@Environment(EnvType.CLIENT)
public final class LambdaListenerFactory {

    private static final Lookup LOOKUP = MethodHandles.lookup();

    private static final MethodType ERASED_INVOKE_TYPE =
            MethodType.methodType(void.class, Event.class);

    private LambdaListenerFactory() {}

    public static <E extends Event> Listener<E> create(Object object, Method method) {
        if (object == null) {
            return createStatic(method);
        } else {
            try {
                return (Listener) createFactory(method).invoke((Object) object);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Throwable exception2) {
                throw new RuntimeException(
                        "Unexpected error creating listener for method " + method, exception2);
            }
        }
    }

    public static <E extends Event> Listener<E> createStatic(Method method) {
        validateMethod(method);
        try {
            Lookup lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), LOOKUP);
            MethodHandle methodHandle = lookup.unreflect(method);
            MethodType methodType =
                    MethodType.methodType(void.class, method.getParameterTypes()[0]);
            CallSite callSite =
                    LambdaMetafactory.metafactory(
                            lookup,
                            "invoke",
                            MethodType.methodType(Listener.class),
                            ERASED_INVOKE_TYPE,
                            methodHandle,
                            methodType);
            return (Listener) callSite.getTarget().invoke();
        } catch (Throwable exception) {
            throw wrap(method, exception);
        }
    }

    public static MethodHandle createFactory(Method method) {
        validateMethod(method);
        try {
            Lookup lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), LOOKUP);
            MethodHandle methodHandle = lookup.unreflect(method);
            MethodType methodType =
                    MethodType.methodType(void.class, method.getParameterTypes()[0]);
            MethodType methodType2 =
                    MethodType.methodType(Listener.class, method.getDeclaringClass());
            CallSite callSite =
                    LambdaMetafactory.metafactory(
                            lookup,
                            "invoke",
                            methodType2,
                            ERASED_INVOKE_TYPE,
                            methodHandle,
                            methodType);
            return callSite.getTarget();
        } catch (Throwable exception) {
            throw wrap(method, exception);
        }
    }

    private static RuntimeException wrap(Method method, Throwable throwable) {
        if (throwable instanceof LambdaConversionException) {
            return new RuntimeException(
                    "LambdaMetafactory failed for method "
                            + method
                            + ". Ensure the listener method is public and its parameter type is a"
                            + " concrete Event subclass.",
                    throwable);
        } else {
            return throwable instanceof IllegalAccessException
                    ? new RuntimeException(
                            "Cannot access method "
                                    + method
                                    + ". Listener methods must be public and their declaring class"
                                    + " must be accessible.",
                            throwable)
                    : new RuntimeException(
                            "Unexpected error creating listener for method " + method, throwable);
        }
    }

    private static void validateMethod(Method method) {
        if (method.getReturnType() != void.class) {
            throw new IllegalArgumentException("@EventListener method must return void: " + method);
        } else {
            Class[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                throw new IllegalArgumentException(
                        "@EventListener method must have exactly one parameter: " + method);
            } else if (!Event.class.isAssignableFrom(parameterTypes[0])) {
                throw new IllegalArgumentException(
                        "@EventListener parameter must be a subclass of Event: " + method);
            }
        }
    }
}
