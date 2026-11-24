package net.efn.client.event.impl;

import net.efn.client.event.Event;
import net.efn.client.event.EventType;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModuleToggleEvent extends Event {

    public static final EventType<ModuleToggleEvent> TYPE = new EventType<>("ModuleToggleEvent");

    private static int silentDepth;

    public final ClientModule module;

    public final boolean enabled;

    private ModuleToggleEvent(ClientModule module2, boolean enabled2) {
        this.module = module2;
        this.enabled = enabled2;
    }

    public static void dispatch(ClientModule module2) {
        if (silentDepth <= 0 && TYPE.hasPreListeners()) {
            TYPE.dispatch(new ModuleToggleEvent(module2, module2.isEnabled()));
        }
    }

    public static void silently(Runnable runnable) {
        silentDepth++;
        try {
            runnable.run();
        } finally {
            silentDepth--;
        }
    }
}
