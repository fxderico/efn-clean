package net.efn.client.module.impl.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.efn.client.discord.DiscordIpc;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientConfigurationPacketListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Environment(EnvType.CLIENT)
public final class DiscordPresenceModule extends ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final String APPLICATION_ID = "1560799535184216114";

    private static final String LOGO_ASSET = "logo";

    private static final String LOGO_TEXT = "EFN Client";

    private static final String BUTTON_LABEL = "Discord";

    private static final String IN_MENU = "In Main Menu";

    private static final String SINGLEPLAYER = "Singleplayer";

    private static final String MULTIPLAYER = "Multiplayer";

    private static final String LAN_WORLD = "LAN World";

    private static final String REALMS = "Realms";

    private static final long GAME_STARTED =
            ProcessHandle.current().info().startInstant().orElseGet(Instant::now).getEpochSecond();

    private static final long RETRY_MIN_MS = 5000L;

    private static final long RETRY_MAX_MS = 30000L;

    private static final long IDLE_POLL_MS = 1000L;

    private static final long MIN_UPDATE_GAP_MS = 4000L;

    private static final long KEEP_ALIVE_MS = 60000L;

    private final BooleanSetting showServerIp =
            this.addSetting("show-server-ip", new BooleanSetting("Show Server IP", true));

    private final BooleanSetting discordButton =
            this.addSetting("discord-button", new BooleanSetting("Discord Button", true));

    private volatile DiscordPresenceModule.Card card;

    private DiscordPresenceModule.Link link;

    private ClientConnection session;

    private long joinedAt;

    private boolean loggedIdle;

    public DiscordPresenceModule() {
        super(
                "discord-presence",
                "Discord Presence",
                "Shows EFN Client and the server you are on in your Discord status.",
                ModuleCategory.CLIENT);
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    protected void onEnable() {
        if ("1560799535184216114".isBlank()) {
            if (!this.loggedIdle) {
                this.loggedIdle = true;
                LOGGER.info(
                        "[EFN] Discord Presence: no Discord application id set"
                                + " (DiscordPresenceModule.APPLICATION_ID); idle");
            }
        } else {
            this.session = null;
            this.card =
                    new DiscordPresenceModule.Card(
                            "In Main Menu", GAME_STARTED, this.discordButton.get());
            this.link = new DiscordPresenceModule.Link(this.link);
            this.refresh();
            this.on(TickEvent.TYPE, tickEvent -> this.refresh());
            this.link.start();
        }
    }

    @Override
    protected void onDisable() {
        if (this.link != null) {
            this.link.stop();
        }
        this.session = null;
    }

    private void refresh() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        long joinedAt2;
        String place;
        if (networkHandler != null) {
            if (networkHandler.getConnection() != this.session) {
                this.session = networkHandler.getConnection();
                this.joinedAt = System.currentTimeMillis() / 1000L;
            }
            place = this.place(client, networkHandler.getServerInfo());
            joinedAt2 = this.joinedAt;
        } else {
            if (this.session != null
                    && this.session.isOpen()
                    && this.session.getPacketListener()
                            instanceof ClientConfigurationPacketListener) {
                return;
            }
            this.session = null;
            place = "In Main Menu";
            joinedAt2 = GAME_STARTED;
        }
        boolean discordButton2 = this.discordButton.get();
        DiscordPresenceModule.Card card2 = this.card;
        if (joinedAt2 != card2.startedAt()
                || discordButton2 != card2.discordButton()
                || !place.equals(card2.details())) {
            this.card = new DiscordPresenceModule.Card(place, joinedAt2, discordButton2);
            this.link.wake();
        }
    }

    private String place(MinecraftClient client, ServerInfo serverInfo) {
        if (client.isInSingleplayer()) {
            return "Singleplayer";
        } else if (serverInfo == null) {
            return "Multiplayer";
        } else if (serverInfo.isRealm()) {
            return "Realms";
        } else if (serverInfo.isLocal()) {
            return "LAN World";
        } else {
            return this.showServerIp.get() ? serverInfo.address : "Multiplayer";
        }
    }

    private static JsonObject activity(DiscordPresenceModule.Card card2) {
        JsonObject json = new JsonObject();
        json.addProperty("name", "EFN Client");
        json.addProperty("details", card2.details());
        json.addProperty("state", "efn.lol");
        JsonObject json2 = new JsonObject();
        json2.addProperty("start", card2.startedAt());
        json.add("timestamps", json2);
        JsonObject json3 = new JsonObject();
        json3.addProperty("large_image", "logo");
        json3.addProperty("large_text", "EFN Client");
        json.add("assets", json3);
        if (card2.discordButton()) {
            JsonObject json4 = new JsonObject();
            json4.addProperty("label", "Discord");
            json4.addProperty("url", "https://discord.com/invite/pxHtFp5b6h");
            JsonArray array = new JsonArray();
            array.add(json4);
            json.add("buttons", array);
        }
        return json;
    }

    @Environment(EnvType.CLIENT)
    private record Card(String details, long startedAt, boolean discordButton) {}

    @Environment(EnvType.CLIENT)
    private final class Link implements Runnable {

        private final Thread thread = new Thread(this, "EFN Discord Presence");

        private volatile boolean running = true;

        private Thread previous;

        private DiscordIpc ipc;

        private DiscordPresenceModule.Card sent;

        private long sentAt;

        private long retryMs = 5000L;

        private String lastProblem;

        Link(DiscordPresenceModule.Link link2) {
            this.previous = link2 != null ? link2.thread : null;
            this.thread.setDaemon(true);
        }

        void start() {
            this.thread.start();
        }

        void wake() {
            LockSupport.unpark(this.thread);
        }

        void stop() {
            this.running = false;
            LockSupport.unpark(this.thread);
        }

        @Override
        public void run() {
            this.awaitPrevious();
            while (this.running) {
                if (this.ipc == null && !this.connect()) {
                    this.pause(this.retryMs);
                    this.retryMs = Math.min(this.retryMs * 2L, 30000L);
                } else {
                    try {
                        this.ipc.poll();
                        DiscordPresenceModule.Card card2 = DiscordPresenceModule.this.card;
                        long l = System.currentTimeMillis() - this.sentAt;
                        if (this.sent == null || l >= (card2.equals(this.sent) ? 60000L : 4000L)) {
                            this.send(card2);
                        }
                    } catch (IOException exception) {
                        this.report("lost Discord", exception);
                        this.hangUp();
                        continue;
                    }
                    this.pause(1000L);
                }
            }
            if (this.ipc != null) {
                this.clear();
            }
        }

        private void awaitPrevious() {
            if (this.previous != null) {
                try {
                    this.previous.join();
                } catch (InterruptedException exception) {
                    this.running = false;
                }
                this.previous = null;
            }
        }

        private boolean connect() {
            try {
                this.ipc = DiscordIpc.connect("1560799535184216114");
            } catch (IOException exception) {
                this.report("could not connect", exception);
                return false;
            }
            if (this.ipc == null) {
                return false;
            } else {
                DiscordPresenceModule.LOGGER.info("[EFN] Discord Presence: connected to Discord");
                this.retryMs = 5000L;
                this.lastProblem = null;
                this.sent = null;
                return true;
            }
        }

        private void send(DiscordPresenceModule.Card sent2) throws IOException {
            try {
                this.ipc.setActivity(DiscordPresenceModule.activity(sent2));
            } catch (DiscordIpc.RefusedException exception) {
                this.report("Discord refused the card", exception);
            }
            this.sent = sent2;
            this.sentAt = System.currentTimeMillis();
        }

        private void clear() {
            try {
                this.ipc.setActivity(null);
            } catch (DiscordIpc.RefusedException | IOException exception) {
            }
            this.hangUp();
        }

        private void hangUp() {
            this.ipc.close();
            this.ipc = null;
            this.sent = null;
        }

        private void pause(long l) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(l));
        }

        private void report(String string, Exception exception) {
            String string2 = string + " (" + exception + ")";
            if (!string2.equals(this.lastProblem)) {
                this.lastProblem = string2;
                DiscordPresenceModule.LOGGER.warn("[EFN] Discord Presence: {}", string2);
            }
        }
    }
}
