package net.efn.client.module.impl.misc;

import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.LevelUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class AutoRelogModule extends ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final int REARM_HEIGHT = 4;

    private static final int REARM_TICKS = 40;

    private final NumberSetting yLevel =
            this.addSetting(
                    "y-level", new NumberSetting("Y Level", 0.0F, -60.0F, 16.0F).step(1.0F));

    private final NumberSetting delay =
            this.addSetting("delay", new NumberSetting("Delay", 3.0F, 1.0F, 30.0F).step(0.5F));

    private ClientWorld seenLevel;

    private ClientPlayerEntity seenPlayer;

    private int aboveTicks;

    private ServerInfo rejoin;

    private boolean disconnected;

    private int rejoinTicks;

    public AutoRelogModule() {
        super(
                "auto-relog",
                "Auto Relog",
                "Relogs once when you go down to deepslate level.",
                ModuleCategory.DONUT);
    }

    @Override
    protected void onEnable() {
        this.forget();
        this.on(TickEvent.TYPE, tickEvent -> this.onTick());
    }

    @Override
    protected void onDisable() {
        this.forget();
    }

    private void forget() {
        this.seenLevel = null;
        this.seenPlayer = null;
        this.aboveTicks = 0;
        this.rejoin = null;
        this.disconnected = false;
    }

    private void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.rejoin != null) {
            this.waitToRejoin(client);
        } else {
            ClientPlayerEntity player = client.player;
            ClientWorld world = client.world;
            if (player != null
                    && world != null
                    && !(client.currentScreen instanceof LevelLoadingScreen)) {
                if (world != this.seenLevel || player != this.seenPlayer) {
                    this.seenLevel = world;
                    this.seenPlayer = player;
                    this.aboveTicks = 0;
                }
                if (!LevelUtil.isOverworld(world)) {
                    this.aboveTicks = 0;
                } else {
                    double d = player.getY();
                    float yLevel2;
                    if (d >= (yLevel2 = this.yLevel.get()) + 4.0F) {
                        if (this.aboveTicks < 40) {
                            this.aboveTicks++;
                        }
                    } else if (d < yLevel2) {
                        boolean bl = this.aboveTicks >= 40;
                        this.aboveTicks = 0;
                        if (bl) {
                            this.relog(client, d);
                        }
                    }
                }
            } else {
                this.aboveTicks = 0;
            }
        }
    }

    private void relog(MinecraftClient client, double d) {
        ServerInfo serverInfo = client.isInSingleplayer() ? null : client.getCurrentServerEntry();
        if (serverInfo != null && !serverInfo.isRealm()) {
            LOGGER.info("[EFN] Auto Relog: reached Y {}, relogging", (int) Math.floor(d));
            this.rejoin = serverInfo;
            this.disconnected = false;
            this.rejoinTicks = Math.round(this.delay.get() * 20.0F);
            client.send(
                    () -> {
                        if (this.rejoin == serverInfo && client.world != null) {
                            client.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT);
                            this.disconnected = true;
                        }
                    });
        }
    }

    private void waitToRejoin(MinecraftClient client) {
        if (this.disconnected) {
            if (client.world == null && client.currentScreen instanceof MultiplayerScreen) {
                if (--this.rejoinTicks <= 0) {
                    ServerInfo rejoin2 = this.rejoin;
                    Screen currentScreen = client.currentScreen;
                    this.rejoin = null;
                    client.send(
                            () -> {
                                if (client.currentScreen == currentScreen && client.world == null) {
                                    ConnectScreen.connect(
                                            currentScreen,
                                            client,
                                            ServerAddress.parse(rejoin2.address),
                                            rejoin2,
                                            false,
                                            null);
                                }
                            });
                }
            } else {
                this.rejoin = null;
            }
        }
    }
}
