package net.efn.client.module.impl.donutmacros;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
final class MacroWorker {

    private static final long POLL_MS = 50L;

    private static final long CALL_TIMEOUT_SECONDS = 10L;

    private final String name;

    private volatile Thread thread;

    MacroWorker(String name2) {
        this.name = name2;
    }

    synchronized void start(Runnable runnable, Consumer<Throwable> consumer) {
        this.stop();
        Thread thread2 =
                new Thread(
                        () -> {
                            try {
                                runnable.run();
                            } catch (MacroWorker.Cancelled exception) {
                                synchronized (this) {
                                    if (this.thread == Thread.currentThread()) {
                                        this.thread = null;
                                    }
                                }
                            } catch (Throwable exception2) {
                                if (this.thread == Thread.currentThread()) {
                                    consumer.accept(exception2);
                                }
                            } finally {
                                synchronized (this) {
                                    if (this.thread == Thread.currentThread()) {
                                        this.thread = null;
                                    }
                                }
                            }
                        },
                        this.name);
        thread2.setDaemon(true);
        this.thread = thread2;
        thread2.start();
    }

    synchronized void stop() {
        Thread thread2 = this.thread;
        this.thread = null;
        if (thread2 != null && thread2 != Thread.currentThread()) {
            thread2.interrupt();
        }
    }

    boolean running() {
        return this.thread != null;
    }

    void check() {
        if (this.thread != Thread.currentThread() || Thread.currentThread().isInterrupted()) {
            throw new MacroWorker.Cancelled();
        }
    }

    void sleep(long l) {
        this.check();
        try {
            Thread.sleep(Math.max(0L, l));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MacroWorker.Cancelled();
        }
        this.check();
    }

    void ticks(int i) {
        this.sleep(i * 50L);
    }

    <T> T call(Supplier<T> supplier) {
        this.check();
        Thread thread2 = Thread.currentThread();
        CompletableFuture future =
                MinecraftClient.getInstance()
                        .submit(
                                () -> {
                                    if (this.thread != thread2) {
                                        throw new MacroWorker.Cancelled();
                                    } else {
                                        return supplier.get();
                                    }
                                });
        try {
            return (T) future.get(10L, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            future.cancel(false);
            Thread.currentThread().interrupt();
            throw new MacroWorker.Cancelled();
        } catch (ExecutionException exception2) {
            Throwable cause = exception2.getCause();
            if (cause instanceof RuntimeException exception3) {
                throw exception3;
            } else if (cause instanceof Error error) {
                throw error;
            } else {
                throw new IllegalStateException(cause);
            }
        } catch (TimeoutException exception4) {
            future.cancel(false);
            throw new IllegalStateException("The game did not respond");
        }
    }

    void run(Runnable runnable) {
        this.call(
                () -> {
                    runnable.run();
                    return Boolean.TRUE;
                });
    }

    boolean waitFor(BooleanSupplier booleanSupplier, long l) {
        long l2 = System.currentTimeMillis() + l;
        while (!this.call(booleanSupplier::getAsBoolean)) {
            if (System.currentTimeMillis() >= l2) {
                return false;
            }
            this.sleep(50L);
        }
        return true;
    }

    @Environment(EnvType.CLIENT)
    static final class Cancelled extends RuntimeException {

        Cancelled() {
            super(null, null, false, false);
        }
    }
}
