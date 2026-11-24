/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@Environment(value = EnvType.CLIENT)
public final class SessionWorker<T> implements AutoCloseable {

    private final String name;

    private ExecutorService executor;

    private Future<T> pending;

    public SessionWorker(String name2) {
        this.name = name2;
    }

    public boolean busy() {
        return this.pending != null;
    }

    public void submit(Callable<T> callable) {
        if (this.pending != null) {
            throw new IllegalStateException("Worker is busy");
        }
        if (this.executor == null) {
            this.executor =
                    Executors.newSingleThreadExecutor(
                            runnable -> {
                                Thread thread = new Thread(runnable, this.name);
                                thread.setDaemon(true);
                                return thread;
                            });
        }
        this.pending = this.executor.submit(callable);
    }

    public void poll(Consumer<T> consumer) {
        if (this.pending == null || !this.pending.isDone()) {
            return;
        }
        Future<T> pending2 = this.pending;
        this.pending = null;
        try {
            consumer.accept(pending2.get());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException exception2) {
            Throwable cause = exception2.getCause();
            if (cause instanceof Error) {
                Error error = (Error) cause;
                throw error;
            }
            throw new IllegalStateException("Snapshot worker failed", exception2.getCause());
        } catch (CancellationException exception3) {
            // empty catch block
        }
    }

    @Override
    public void close() {
        if (this.pending != null) {
            this.pending.cancel(true);
        }
        this.pending = null;
        if (this.executor != null) {
            this.executor.shutdownNow();
        }
        this.executor = null;
    }
}
