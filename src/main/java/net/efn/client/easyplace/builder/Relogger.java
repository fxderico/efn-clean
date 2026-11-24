/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.easyplace.builder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;

@Environment(value = EnvType.CLIENT)
final class Relogger {

    private static final int WAIT_TICKS = 60;

    private static final int TRIES = 5;

    private ServerInfo server;

    private boolean out;

    private int wait;

    private int tries;

    Relogger() {}

    boolean active() {
        return this.server != null;
    }

    boolean start(MinecraftClient client) {
        ServerInfo serverInfo = client.isInSingleplayer() ? null : client.getCurrentServerEntry();
        ServerInfo serverInfo2 = serverInfo;
        if (serverInfo == null || serverInfo.isRealm() || client.world == null) {
            return false;
        }
        this.server = serverInfo;
        this.out = false;
        this.wait = 60;
        this.tries = 0;
        client.send(
                () -> {
                    if (this.server != serverInfo || client.world == null) {
                        return;
                    }
                    client.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT);
                    this.out = true;
                });
        return true;
    }

    void tick(MinecraftClient client) {
        if (this.server == null || !this.out) {
            return;
        }
        if (client.world != null) {
            this.server = null;
            return;
        }
        if (client.currentScreen instanceof ConnectScreen || --this.wait > 0) {
            return;
        }
        if (++this.tries > 5) {
            this.server = null;
            return;
        }
        ServerInfo server2 = this.server;
        Screen screen =
                client.currentScreen instanceof MultiplayerScreen
                        ? client.currentScreen
                        : new MultiplayerScreen(new TitleScreen());
        this.wait = 180;
        client.send(
                () -> {
                    if (client.world == null) {
                        ConnectScreen.connect(
                                screen,
                                client,
                                ServerAddress.parse(server2.address),
                                server2,
                                false,
                                null);
                    }
                });
    }
}
