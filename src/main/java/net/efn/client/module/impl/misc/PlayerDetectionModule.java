package net.efn.client.module.impl.misc;

import net.efn.client.chat.ModuleChat;
import net.efn.client.config.ModuleConfigStore;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.security.PayGuard;
import net.efn.client.webhook.WebhookConfig;
import net.efn.client.webhook.WebhookEmbed;
import net.efn.client.webhook.WebhookPayloadBuilder;
import net.efn.client.webhook.WebhookService;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.SystemToast.Type;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Environment(EnvType.CLIENT)
public final class PlayerDetectionModule extends ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final Set<String> PERMANENT_WHITELIST = Set.of("freecamera");

    private static final long DISCONNECT_DELAY_MS = 500L;

    private static final String WEBHOOK_TITLE = "\ud83d\udea8 Player Detection Alert";

    private static final int WEBHOOK_COLOR = 15158332;

    private static final Type TOAST_ID = new Type();

    private static final ExecutorService WEBHOOK_EXECUTOR =
            Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread = new Thread(runnable, "EFN-PlayerDetection-Webhook");
                        thread.setDaemon(true);
                        return thread;
                    });

    private final StringSetting modulesToToggle =
            this.addSetting("modules-to-toggle", new StringSetting("Modules To Toggle", ""));

    private final BooleanSetting enableDisconnect =
            this.addSetting("disconnect", new BooleanSetting("Disconnect", true));

    private final ModeSetting notificationMode =
            this.addSetting(
                    "notification-mode",
                    new ModeSetting("Notification Mode", "Both", "Chat", "Toast", "Both"));

    private final BooleanSetting toggleOnPlayer =
            this.addSetting("toggle-on-detect", new BooleanSetting("Toggle On Detect", true));

    private final StringSetting userWhitelist =
            this.addSetting("user-whitelist", new StringSetting("User Whitelist", ""));

    private final BooleanSetting enableWebhook =
            this.addSetting("webhook", new BooleanSetting("Webhook", false));

    private final StringSetting webhookUrl =
            this.addSetting("webhook-url", new StringSetting("Webhook URL", ""));

    private final BooleanSetting selfPing =
            this.addSetting("self-ping", new BooleanSetting("Self Ping", false));

    private final StringSetting discordId =
            this.addSetting("discord-id", new StringSetting("Discord ID", ""));

    private final BooleanSetting enablePanicPay =
            this.addSetting("enable-panic-pay", new BooleanSetting("Enable Panic Pay", false));

    private final StringSetting panicPayTarget =
            this.addSetting("target-player", new StringSetting("Target Player", ""));

    private final StringSetting panicPayAmount =
            this.addSetting("amount", new StringSetting("Amount", ""));

    private final Set<String> detectedPlayers = new HashSet<>();

    private ClientWorld lastLevel;

    private String whitelistRaw;

    private Set<String> whitelist = Set.of();

    public PlayerDetectionModule() {
        super(
                "player-detection",
                "Player Detection",
                "Detects when players are in the world.",
                ModuleCategory.DONUT);
        this.usesNotifications();
        this.webhookUrl.visibleWhen(this.enableWebhook::get);
        this.selfPing.visibleWhen(this.enableWebhook::get);
        this.discordId.visibleWhen(() -> this.enableWebhook.get() && this.selfPing.get());
        this.panicPayTarget.visibleWhen(this.enablePanicPay::get);
        this.panicPayAmount.visibleWhen(this.enablePanicPay::get);
        this.webhookUrl.privateValue();
        this.discordId.privateValue();
        this.enablePanicPay.privateValue();
        this.panicPayTarget.privateValue();
        this.panicPayAmount.privateValue();
        this.userWhitelist.privateValue();
        this.userWhitelist.getImString().inputData.isResizable = true;
        this.modulesToToggle.getImString().inputData.isResizable = true;
    }

    @Override
    protected void onEnable() {
        this.detectedPlayers.clear();
        this.lastLevel = null;
        this.on(TickEvent.TYPE, Stage.POST, this::onTick);
    }

    @Override
    protected void onDisable() {
        this.detectedPlayers.clear();
        this.lastLevel = null;
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player != null && world != null) {
            if (world != this.lastLevel) {
                this.detectedPlayers.clear();
                this.lastLevel = world;
            }
            String string = player.getGameProfile().name();
            UUID uuid = player.getUuid();
            Set set = this.whitelist();
            LinkedHashSet set2 = null;
            for (AbstractClientPlayerEntity player2 : world.getPlayers()) {
                String string2;
                String string3;
                if (player2 != player
                        && !uuid.equals(player2.getUuid())
                        && (string3 = player2.getGameProfile().name()) != null
                        && !string3.equals(string)
                        && !PERMANENT_WHITELIST.contains(string2 = string3.toLowerCase(Locale.ROOT))
                        && !set.contains(string2)) {
                    if (set2 == null) {
                        set2 = new LinkedHashSet();
                    }
                    set2.add(string3);
                }
            }
            if (set2 == null) {
                this.detectedPlayers.clear();
            } else if (!set2.equals(this.detectedPlayers)) {
                this.detectedPlayers.clear();
                this.detectedPlayers.addAll(set2);
                this.handlePlayerDetection(set2);
            }
        } else {
            this.detectedPlayers.clear();
            this.lastLevel = null;
        }
    }

    private void handlePlayerDetection(Set<String> set) {
        MinecraftClient client = MinecraftClient.getInstance();
        String notificationMode2 = this.notificationMode.get();
        switch (notificationMode2) {
            case "Chat":
                this.announceDetection(set);
                break;
            case "Toast":
                this.showToast(client);
                break;
            default:
                this.announceDetection(set);
                this.showToast(client);
        }
        for (ClientModule module : this.resolveModulesToToggle()) {
            module.toggle();
        }
        if (this.enablePanicPay.get()) {
            this.panicPay(client);
        }
        if (this.enableWebhook.get()) {
            this.sendWebhookNotification(client, set);
        }
        if (this.toggleOnPlayer.get()) {
            this.toggle();
        }
        if (this.enableDisconnect.get()) {
            this.scheduleDisconnect(client, set);
        }
    }

    private void announceDetection(Set<String> set) {
        ModuleChat.send(
                this,
                names(
                        ModuleChat.body()
                                .danger(set.size() == 1 ? "Player detected" : "Players detected")
                                .text(": "),
                        set));
    }

    private static ModuleChat.Body names(ModuleChat.Body body, Collection<String> collection) {
        boolean bl = true;
        for (String string : collection) {
            if (!bl) {
                body.text(", ");
            }
            body.name(string);
            bl = false;
        }
        return body;
    }

    private void showToast(MinecraftClient client) {
        SystemToast.show(
                client.getToastManager(),
                TOAST_ID,
                Text.literal(this.name()),
                Text.literal("Player Detected!"));
    }

    private List<ClientModule> resolveModulesToToggle() {
        String modulesToToggle2 = this.modulesToToggle.get();
        if (modulesToToggle2.isBlank()) {
            return List.of();
        } else {
            LinkedHashSet set = new LinkedHashSet();
            for (String string : modulesToToggle2.split(",")) {
                String string2 = string.trim();
                if (!string2.isEmpty()) {
                    String renamedModuleId = ModuleConfigStore.renamedModuleId(string2);
                    if (renamedModuleId != null) {
                        string2 = renamedModuleId;
                    }
                    for (ClientModule module : ModuleRegistry.all()) {
                        if (module.id().equalsIgnoreCase(string2)
                                || module.name().equalsIgnoreCase(string2)) {
                            if (module != this && module.inConfigs()) {
                                set.add(module);
                            }
                            break;
                        }
                    }
                }
            }
            return new ArrayList<>(set);
        }
    }

    private Set<String> whitelist() {
        String userWhitelist2 = this.userWhitelist.get();
        if (!userWhitelist2.equals(this.whitelistRaw)) {
            HashSet set = new HashSet();
            for (String string : userWhitelist2.split(",")) {
                String string2 = string.trim();
                if (!string2.isEmpty()) {
                    set.add(string2.toLowerCase(Locale.ROOT));
                }
            }
            this.whitelist = set;
            this.whitelistRaw = userWhitelist2;
        }
        return this.whitelist;
    }

    private void panicPay(MinecraftClient client) {
        String string = this.panicPayTarget.get().trim();
        String string2 = this.panicPayAmount.get().trim();
        if (string.isEmpty()) {
            ModuleChat.send(this, ModuleChat.body().text("Panic pay target is ").warn("not set"));
        } else if (string2.isEmpty()) {
            ModuleChat.send(this, ModuleChat.body().text("Panic pay amount is ").warn("not set"));
        } else {
            ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
            if (networkHandler != null) {
                String string3 = String.format("/pay %s %s", string, string2);
                if (client.inGameHud != null) {
                    client.inGameHud.getChatHud().addToMessageHistory(string3);
                }
                PayGuard.allow(string3);
                networkHandler.sendChatCommand(string3.substring(1));
                ModuleChat.send(
                        this,
                        ModuleChat.body()
                                .text("Panic pay ")
                                .good("sent")
                                .text(": ")
                                .value(string2)
                                .text(" to ")
                                .name(string));
            }
        }
    }

    private void sendWebhookNotification(MinecraftClient client, Set<String> set) {
        String string = this.webhookUrl.get().trim();
        if (string.isEmpty()) {
            ModuleChat.send(this, ModuleChat.body().text("Webhook URL is ").warn("not set"));
        } else if (!isValidWebhookUrl(string)) {
            ModuleChat.send(this, ModuleChat.body().text("Webhook URL is ").danger("invalid"));
        } else {
            ServerInfo currentServerEntry = client.getCurrentServerEntry();
            String string2 =
                    currentServerEntry != null && currentServerEntry.name != null
                            ? currentServerEntry.name
                            : "Unknown Server";
            String string3 = this.discordId.get().trim();
            String string4 = this.selfPing.get() && !string3.isEmpty() ? "<@" + string3 + ">" : "";
            String string5 = this.panicPayTarget.get().trim();
            String string6 = this.panicPayAmount.get().trim();
            long l = System.currentTimeMillis() / 1000L;
            StringBuilder sb =
                    new StringBuilder("Player(s) detected on server!\n\n")
                            .append("**Players:** ")
                            .append(escapeMarkdown(String.join(", ", set)))
                            .append('\n')
                            .append("**Server:** ")
                            .append(escapeMarkdown(string2))
                            .append('\n')
                            .append("**Time:** <t:")
                            .append(l)
                            .append(":R>");
            if (this.enablePanicPay.get() && !string5.isEmpty() && !string6.isEmpty()) {
                sb.append("\n**Panic Pay:** Activated - Target: ")
                        .append(escapeMarkdown(string5))
                        .append(", Amount: ")
                        .append(escapeMarkdown(string6));
            }
            WebhookPayloadBuilder webhookPayloadBuilder =
                    new WebhookPayloadBuilder()
                            .content(string4)
                            .addEmbed(
                                    new WebhookEmbed(
                                            "\ud83d\udea8 Player Detection Alert",
                                            sb.toString(),
                                            15158332,
                                            null,
                                            null,
                                            "Sent by EFN",
                                            null));
            WebhookConfig webhookConfig = new WebhookConfig(string);
            WEBHOOK_EXECUTOR.execute(
                    () -> {
                        boolean send;
                        try {
                            send =
                                    PlayerDetectionModule.WebhookHolder.SERVICE.send(
                                            webhookConfig, webhookPayloadBuilder);
                        } catch (RuntimeException | LinkageError exception) {
                            LOGGER.warn(
                                    "[EFN] Player Detection webhook failed ({})",
                                    exception.getClass().getSimpleName());
                            send = false;
                        }
                        ModuleChat.send(
                                this,
                                send
                                        ? ModuleChat.body()
                                                .text("Webhook notification ")
                                                .good("sent")
                                        : ModuleChat.body()
                                                .text("Webhook notification ")
                                                .danger("failed")
                                                .text(" (see the log for details)"));
                    });
        }
    }

    private static boolean isValidWebhookUrl(String string) {
        try {
            URI uRI = new URI(string);
            String scheme = uRI.getScheme();
            return uRI.getHost() != null
                    && ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme));
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static String escapeMarkdown(String string) {
        StringBuilder sb = new StringBuilder(string.length() + 8);
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '\\' || c == '*' || c == '_' || c == '~' || c == '`' || c == '|') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private void scheduleDisconnect(MinecraftClient client, Set<String> set) {
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler != null) {
            ClientConnection connection = networkHandler.getConnection();
            CompletableFuture.delayedExecutor(500L, TimeUnit.MILLISECONDS, client)
                    .execute(() -> this.disconnectFromServer(client, connection, set));
        }
    }

    private void disconnectFromServer(
            MinecraftClient client, ClientConnection clientConnection, Set<String> set) {
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (client.world != null
                && networkHandler != null
                && networkHandler.getConnection() == clientConnection
                && clientConnection.isOpen()) {
            AutoReconnectModule.holdOff();
            clientConnection.disconnect(
                    Text.literal("Player(s) detected: " + String.join(", ", set)));
            ModuleChat.send(
                    this,
                    names(
                                    ModuleChat.body()
                                            .danger("Disconnected")
                                            .text(" from the server (")
                                            .text(set.size() == 1 ? "player" : "players")
                                            .text(" detected: "),
                                    set)
                            .text(")"));
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class WebhookHolder {

        static final WebhookService SERVICE = new WebhookService();

        private WebhookHolder() {}
    }
}
