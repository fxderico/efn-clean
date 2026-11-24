package net.efn.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public record ListenerHandle<E extends Event>(
        EventType<E> type,
        Stage stage,
        EventPriority priority,
        Object owner,
        Consumer<E> callback) {
    public void unregister() {
        this.type.unregister(this);
    }
}
