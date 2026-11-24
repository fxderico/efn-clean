package net.efn.client.module.impl.misc;

import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class AutoReconnectModule extends ClientModule {

    private static final long HOLD_MS = 15000L;

    private static volatile long holdUntil;

    private final NumberSetting delay =
            this.addSetting("delay", new NumberSetting("Delay", 5.0F, 3.0F, 60.0F).step(1.0F));

    private ServerInfo lastServer;

    private Screen activeScreen;

    private ButtonWidget reconnectButton;

    private int ticksLeft;

    private boolean heldScreen;

    public static void holdOff() {
        holdUntil = Util.getMeasuringTimeMs() + 15000L;
    }

    public AutoReconnectModule() {
        super(
                "auto-reconnect",
                "Auto Reconnect",
                "Reconnects to the last server after an unexpected disconnect.",
                ModuleCategory.MISC);
        TickEvent.TYPE.listen(this, this::trackServer);
        ScreenEvents.AFTER_INIT.register(this::onScreenInit);
    }

    private void trackServer(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            ServerInfo serverInfo =
                    client.isInSingleplayer() ? null : client.getCurrentServerEntry();
            this.lastServer =
                    serverInfo != null && !serverInfo.isLocal() && !serverInfo.isRealm()
                            ? serverInfo
                            : null;
        }
    }

    private void onScreenInit(MinecraftClient client, Screen activeScreen2, int i, int j) {
        if (activeScreen2 instanceof TitleScreen || activeScreen2 instanceof MultiplayerScreen) {
            this.lastServer = null;
        } else if (activeScreen2 instanceof DisconnectedScreen && this.lastServer != null) {
            ServerInfo lastServer2 = this.lastServer;
            if (activeScreen2 == this.activeScreen) {
                Screens.getButtons(activeScreen2).remove(this.reconnectButton);
            } else {
                this.activeScreen = activeScreen2;
                this.ticksLeft = Math.round(this.delay.get() * 20.0F);
                this.heldScreen = Util.getMeasuringTimeMs() < holdUntil || isBan(activeScreen2);
                holdUntil = 0L;
            }
            boolean bl = this.isEnabled() && !this.heldScreen;
            ButtonWidget buttonWidget2;
            this.reconnectButton =
                    buttonWidget2 =
                            ButtonWidget.builder(
                                            reconnectLabel(bl, this.ticksLeft),
                                            buttonWidget -> {
                                                buttonWidget.active = false;
                                                this.reconnect(client, lastServer2);
                                            })
                                    .dimensions(i - 150 - 10, j - 20 - 10, 150, 20)
                                    .build();
            Screens.getButtons(activeScreen2).add(buttonWidget2);
            if (bl) {
                ScreenEvents.afterTick(activeScreen2)
                        .register(
                                screenx -> {
                                    this.ticksLeft--;
                                    if (this.ticksLeft <= 0) {
                                        buttonWidget2.active = false;
                                        this.reconnect(client, lastServer2);
                                    } else {
                                        buttonWidget2.setMessage(
                                                reconnectLabel(true, this.ticksLeft));
                                    }
                                });
            }
        }
    }

    private static boolean isBan(Screen screen) {
        String string = screen.getNarratedTitle().getString().toLowerCase(Locale.ROOT);
        return string.contains("banned") || string.contains("blacklisted");
    }

    private void reconnect(MinecraftClient client, ServerInfo serverInfo) {
        ServerAddress serverAddress = ServerAddress.parse(serverInfo.address);
        ConnectScreen.connect(
                new MultiplayerScreen(new TitleScreen()),
                client,
                serverAddress,
                serverInfo,
                false,
                null);
    }

    private static Text reconnectLabel(boolean bl, int i) {
        return bl
                ? Text.literal(String.format(Locale.ROOT, "Reconnect (%.1f)", i / 20.0))
                : Text.literal("Reconnect");
    }
}
