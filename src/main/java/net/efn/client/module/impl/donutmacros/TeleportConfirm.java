package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.module.ClientModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.util.Map;

@Environment(EnvType.CLIENT)
final class TeleportConfirm {

    private static final long WINDOW_MS = 3000L;

    private final ClientModule owner;

    private Object pressed;

    private boolean warned;

    private long sentAt;

    TeleportConfirm(ClientModule owner2) {
        this.owner = owner2;
    }

    void reset() {
        this.pressed = null;
        this.warned = false;
        this.sentAt = 0L;
    }

    void sent() {
        this.sentAt = System.currentTimeMillis();
    }

    boolean tick(MinecraftClient client) {
        Dialogs.View view = Dialogs.current(client);
        if (view != null && view.titled("teleport")) {
            if (view.dialog() == this.pressed) {
                return true;
            } else if (System.currentTimeMillis() - this.sentAt > 3000L) {
                return false;
            } else {
                Dialogs.Button button = view.labelled("confirm", "yes");
                if (button == null) {
                    return false;
                } else {
                    if (!this.warned) {
                        this.warned = true;
                        ModuleChat.send(
                                this.owner,
                                ModuleChat.body()
                                        .warn("Your teleport confirm menu is on")
                                        .text(
                                                ". Turn it off in /settings so requests go out"
                                                        + " faster. Confirming each one for now."));
                    }
                    this.pressed = view.dialog();
                    Dialogs.press(client, view, button, Map.of());
                    return true;
                }
            }
        } else {
            return false;
        }
    }
}
