package net.efn.client.module.impl.misc;

import com.mojang.authlib.GameProfile;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.ListenerHandle;
import net.efn.client.event.impl.PacketReceiveEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ChallengeToast;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.playerbypass.AutoRtpLoop;
import net.efn.client.util.playerbypass.ChunkLightSnapshot;
import net.efn.client.util.playerbypass.PlayerBypassLightTracker;
import net.efn.client.util.playerbypass.RtpReply;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Environment(EnvType.CLIENT)
final class PlayerBypassAutoRtp {

    private static final double TELEPORT_DISTANCE_SQ = 256.0;

    private static final long REPLY_WINDOW_MS = 5000L;

    private static final long STOP_REPLY_WINDOW_MS = 2500L;

    private static final long RESUME_GRACE_MS = 1000L;

    private static final long RESPAWN_GRACE_MS = 3000L;

    private static final long OFFLINE_LIMIT_MS = 30000L;

    private static final long MENU_CLICK_DELAY_MS = 300L;

    private static final long MENU_FILL_MS = 2500L;

    private static final long MENU_CLAIM_MS = 4000L;

    private static final int MAX_MENU_CLICKS = 3;

    private static final int MENU_NAMES_SHOWN = 10;

    private static final long LONG_COOLDOWN_MS = 60000L;

    private static final long BAR_REFRESH_MS = 2000L;

    private static final long NO_CHUNK = -9223372036854775808L;

    private static final long NEVER = -9223372036854775808L;

    private final PlayerBypassModule module;

    private final BooleanSetting enabled;

    private final StringSetting command;

    private final NumberSetting scanTime;

    private final NumberSetting delay;

    private final NumberSetting timeout;

    private final NumberSetting maxRtps;

    private final BooleanSetting stopOnSuspect;

    private final StringSetting menuItem;

    private final BooleanSetting sound;

    private final BooleanSetting showCoordinates;

    private final AutoRtpLoop loop = new AutoRtpLoop();

    private final LongOpenHashSet knownSuspects = new LongOpenHashSet();

    private ListenerHandle<TickEvent> tickHandle;

    private ListenerHandle<PacketReceiveEvent> packetHandle;

    private ListenerHandle<PacketSendEvent> sendHandle;

    private ClientConnection link;

    private ClientWorld level;

    private Vec3d anchor;

    private int startChunkX;

    private int startChunkZ;

    private boolean wasDead;

    private long offlineSince;

    private long lastRevision;

    private boolean lastTryLanded;

    private long commandSentAt = -9223372036854775808L;

    private boolean sendingCommand;

    private volatile long userActionAt = -9223372036854775808L;

    private int menuId = -1;

    private int ignoredMenuId = -1;

    private long menuSeenAt;

    private boolean menuClicked;

    private int menuClicks;

    private long lastMenuClickAt;

    private long noticedCooldownUntil;

    private String lastBar = "";

    private long lastBarAt;

    PlayerBypassAutoRtp(
            PlayerBypassModule module2,
            BooleanSetting enabled2,
            StringSetting command2,
            NumberSetting scanTime2,
            NumberSetting delay2,
            NumberSetting timeout2,
            NumberSetting maxRtps2,
            BooleanSetting stopOnSuspect2,
            StringSetting menuItem2,
            BooleanSetting sound2,
            BooleanSetting showCoordinates2) {
        this.module = module2;
        this.enabled = enabled2;
        this.command = command2;
        this.scanTime = scanTime2;
        this.delay = delay2;
        this.timeout = timeout2;
        this.maxRtps = maxRtps2;
        this.stopOnSuspect = stopOnSuspect2;
        this.menuItem = menuItem2;
        this.sound = sound2;
        this.showCoordinates = showCoordinates2;
    }

    void start() {
        if (!this.loop.running()) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
            if (player != null
                    && client.world != null
                    && networkHandler != null
                    && networkHandler.getConnection().isOpen()) {
                long l = System.currentTimeMillis();
                AutoRtpLoop.Config config = this.config();
                this.loop.start(l, config);
                this.link = networkHandler.getConnection();
                this.level = client.world;
                this.anchor = player.getEntityPos();
                this.startChunkX = player.getBlockPos().getX() >> 4;
                this.startChunkZ = player.getBlockPos().getZ() >> 4;
                this.wasDead = player.isDead();
                this.offlineSince = 0L;
                this.lastRevision = PlayerBypassLightTracker.visibilityRevision();
                this.lastTryLanded = false;
                this.noticedCooldownUntil = 0L;
                this.lastBar = "";
                this.resetMenu();
                this.rememberSuspects();
                if (this.tickHandle == null) {
                    this.tickHandle = TickEvent.TYPE.listen(this, tickEvent -> this.tick());
                }
                if (this.packetHandle == null) {
                    this.packetHandle =
                            PacketReceiveEvent.TYPE.listen(
                                    this,
                                    packetReceiveEvent -> this.onPacket(packetReceiveEvent.packet));
                }
                if (this.sendHandle == null) {
                    this.sendHandle =
                            PacketSendEvent.TYPE.listen(
                                    this,
                                    packetSendEvent -> this.onPacketSent(packetSendEvent.packet));
                }
                ModuleChat.Body body =
                        ModuleChat.body()
                                .good("Auto RTP started")
                                .text(": ")
                                .value("/" + this.command())
                                .text(", ")
                                .value(seconds(config.scanMs()))
                                .text(" scan per spot, at least ")
                                .value(seconds(config.delayMs()))
                                .text(" between RTPs");
                if (config.maxRtps() > 0) {
                    body.text(", max ").value(config.maxRtps());
                }
                long cooldownLeft;
                if ((cooldownLeft = this.loop.cooldownLeft(l)) > 0L) {
                    this.noticedCooldownUntil = l + cooldownLeft;
                    body.text(", server cooldown ").value(seconds(cooldownLeft)).text(" left");
                    if (cooldownLeft >= 60000L) {
                        body.text(" (turn Player Bypass off and on to clear it)");
                    }
                }
                this.say(body.text("."));
            } else {
                this.say(ModuleChat.body().warn("Join a server first").text(", Auto RTP is off."));
                this.enabled.set(false);
            }
        }
    }

    void stopByUser() {
        if (!this.loop.running()) {
            this.unlisten();
        } else {
            this.stop(
                    ModuleChat.body()
                            .text("Auto RTP stopped after ")
                            .value(this.loop.landings())
                            .text(rtpWord(this.loop.landings()))
                            .text("."));
        }
    }

    void forgetCooldown() {
        this.loop.forgetCooldown();
    }

    private void stop(ModuleChat.Body body) {
        boolean bl = this.loop.running() || this.tickHandle != null;
        this.loop.stop();
        this.unlisten();
        this.resetMenu();
        this.link = null;
        this.level = null;
        this.anchor = null;
        if (bl && body != null) {
            this.say(body);
        }
    }

    private void stopWithRestartHint(ModuleChat.Body body) {
        this.stop(body.text(" Turn ").value("Auto RTP").text(" off and on to start again."));
    }

    private void unlisten() {
        if (this.tickHandle != null) {
            this.tickHandle.unregister();
        }
        if (this.packetHandle != null) {
            this.packetHandle.unregister();
        }
        if (this.sendHandle != null) {
            this.sendHandle.unregister();
        }
        this.tickHandle = null;
        this.packetHandle = null;
        this.sendHandle = null;
    }

    boolean baseSpotted(int i, int j, boolean bl) {
        if (this.loop.running() && !this.atStartSpot(i >> 4, j >> 4)) {
            this.found("Base", i, j, bl);
            return true;
        } else {
            return false;
        }
    }

    private void found(String string, int i, int j, boolean bl) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        boolean bl2 = this.loop.phase() == AutoRtpLoop.Phase.TELEPORTING;
        boolean bl3 =
                player != null
                        && this.anchor != null
                        && (player.getEntityWorld() != this.level
                                || player.getEntityPos().squaredDistanceTo(this.anchor) > 256.0);
        boolean bl4 = this.loop.awaitingTeleport() && player != null && !bl3;
        int k = this.loop.landings() + (bl2 && bl3 ? 1 : 0);
        boolean showCoordinates2 = this.showCoordinates.get();
        ModuleChat.Body body = ModuleChat.body().good(string + " found");
        if (showCoordinates2) {
            body.text(" at ").value(i + ", " + j);
        }
        this.stopWithRestartHint(
                body.text(" after ").value(k).text(rtpWord(k) + ". Auto RTP stopped."));
        if (bl4 && !bl) {
            this.say(
                    ModuleChat.body()
                            .warn("Your /" + this.command() + " is still pending")
                            .text(
                                    ": move a little to cancel it (most servers cancel a teleport"
                                            + " when you move),")
                            .text(" or the server may still teleport you away."));
        }
        ChallengeToast.addOrUpdate(
                new ItemStack(string.equals("Base") ? Items.PLAYER_HEAD : Items.SPYGLASS),
                string + " Found - Auto RTP",
                Text.literal(showCoordinates2 ? "at " + i + ", " + j : "after " + k + rtpWord(k)),
                this.sound.get());
        if (this.sound.get()) {
            MinecraftClient.getInstance()
                    .getSoundManager()
                    .play(PositionedSoundInstance.ui(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F));
        }
    }

    private void tick() {
        if (this.loop.running()) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
            long l = System.currentTimeMillis();
            if (this.link != null
                    && this.link.isOpen()
                    && (networkHandler == null || networkHandler.getConnection() == this.link)) {
                if (player != null
                        && client.world != null
                        && networkHandler != null
                        && client.interactionManager != null) {
                    this.offlineSince = 0L;
                    AutoRtpLoop.Config config = this.config();
                    long visibilityRevision = PlayerBypassLightTracker.visibilityRevision();
                    if (visibilityRevision != this.lastRevision) {
                        this.lastRevision = visibilityRevision;
                        this.loop.dataArrived(l);
                    }
                    if (player.isDead()) {
                        if (!this.wasDead) {
                            this.wasDead = true;
                            this.loop.interrupt(l, config);
                            this.resetMenu();
                        }
                        this.loop.holdUntil(l + 3000L);
                        this.bar(
                                l,
                                this.label(config) + " - paused, you died",
                                l - this.lastBarAt >= 2000L);
                    } else {
                        if (this.wasDead) {
                            this.wasDead = false;
                            this.level = client.world;
                            this.anchor = player.getEntityPos();
                            this.loop.holdUntil(l + 3000L);
                        }
                        boolean bl = client.world != this.level;
                        if (bl) {
                            this.level = client.world;
                            this.knownSuspects.clear();
                        }
                        if (bl
                                || this.anchor != null
                                        && player.getEntityPos().squaredDistanceTo(this.anchor)
                                                > 256.0) {
                            switch (this.loop.phase()) {
                                case TELEPORTING:
                                    this.land(player, l, config);
                                    break;
                                case SCANNING:
                                    this.anchor = player.getEntityPos();
                                    this.loop.relanded(l, config);
                                    break;
                                default:
                                    if (this.loop.landedLate(l, config)) {
                                        this.anchor = player.getEntityPos();
                                        this.closeMenu(player);
                                        this.resetMenu();
                                    } else if (bl) {
                                        this.stopWithRestartHint(
                                                ModuleChat.body()
                                                        .warn("World changed")
                                                        .text(", Auto RTP stopped."));
                                        return;
                                    }
                            }
                        }
                        long suspectNear;
                        if (this.stopOnSuspect.get()
                                && !bl
                                && (suspectNear = this.newSuspectNear(client, player))
                                        != -9223372036854775808L) {
                            this.found(
                                    "Suspect",
                                    (ChunkPos.getPackedX(suspectNear) << 4) + 8,
                                    (ChunkPos.getPackedZ(suspectNear) << 4) + 8,
                                    false);
                        } else if (!this.handleMenu(client, player, l, config)) {
                            String string =
                                    client.currentScreen != null
                                                    && !this.ourMenuOpen(client, player)
                                            ? "a menu is open"
                                            : null;
                            if (string != null) {
                                this.loop.holdUntil(l + 1000L);
                            }
                            switch (this.loop.tick(l, config)) {
                                case SEND:
                                    this.send(player, networkHandler, l);
                                    break;
                                case TIMED_OUT:
                                    this.lastTryLanded = false;
                                    this.closeMenu(player);
                                    break;
                                case SCAN_CLEAR:
                                    this.lastTryLanded = true;
                                    break;
                                case LIMIT:
                                    this.stopWithRestartHint(
                                            ModuleChat.body()
                                                    .text("Max RTPs reached (")
                                                    .value(config.maxRtps())
                                                    .text("), no base found. Auto RTP stopped."));
                                    return;
                                case GAVE_UP:
                                    this.closeMenu(player);
                                    this.stopWithRestartHint(
                                            ModuleChat.body()
                                                    .warn("No teleport after 5 tries")
                                                    .text(", check ")
                                                    .value("RTP Command")
                                                    .text(" and ")
                                                    .value("Menu Item")
                                                    .text(". Auto RTP stopped."));
                                    return;
                            }
                            this.progress(l, string, config);
                        }
                    }
                } else {
                    if (this.offlineSince == 0L) {
                        this.offlineSince = l;
                    }
                    if (l - this.offlineSince > 30000L) {
                        this.stopWithRestartHint(
                                ModuleChat.body()
                                        .warn("No world for 30s")
                                        .text(", Auto RTP stopped."));
                    }
                }
            } else {
                this.stopWithRestartHint(
                        ModuleChat.body().warn("Disconnected").text(", Auto RTP stopped."));
            }
        }
    }

    private void send(
            ClientPlayerEntity player,
            ClientPlayNetworkHandler clientPlayNetworkHandler,
            long commandSentAt2) {
        this.anchor = player.getEntityPos();
        this.resetMenu();
        this.rememberSuspects();
        this.commandSentAt = commandSentAt2;
        this.sendingCommand = true;
        try {
            clientPlayNetworkHandler.sendChatCommand(this.command());
        } finally {
            this.sendingCommand = false;
        }
    }

    private void land(ClientPlayerEntity player, long l, AutoRtpLoop.Config config) {
        this.anchor = player.getEntityPos();
        this.closeMenu(player);
        this.resetMenu();
        this.loop.landed(l, config);
    }

    private void onPacket(Packet<?> packet) {
        if (this.loop.running()) {
            if (packet instanceof EntityDamageS2CPacket entityDamagePacket) {
                this.onDamage(entityDamagePacket);
            } else if (packet instanceof GameMessageS2CPacket gameMessagePacket) {
                this.onServerText(
                        gameMessagePacket.content().getString(), !gameMessagePacket.overlay());
            } else if (packet instanceof TitleS2CPacket titlePacket) {
                this.onServerText(titlePacket.text().getString(), false);
            } else if (packet instanceof SubtitleS2CPacket subtitlePacket) {
                this.onServerText(subtitlePacket.text().getString(), false);
            }
        }
    }

    private void onPacketSent(Packet<?> packet) {
        if (!this.sendingCommand) {
            PlayerInteractEntityC2SPacket playerInteractEntityPacket;
            if (packet instanceof PlayerInteractBlockC2SPacket
                    || packet instanceof CommandExecutionC2SPacket
                    || packet instanceof ChatCommandSignedC2SPacket
                    || packet instanceof PlayerInteractEntityC2SPacket
                            && isInteraction(
                                    playerInteractEntityPacket =
                                            (PlayerInteractEntityC2SPacket) packet)) {
                this.userActionAt = System.currentTimeMillis();
            }
        }
    }

    private static boolean isInteraction(PlayerInteractEntityC2SPacket playerInteractEntityPacket) {
        final boolean[] booleans = new boolean[] {false};
        playerInteractEntityPacket.handle(
                new Handler() {

                    @Override
                    public void interact(Hand hand) {
                        booleans[0] = true;
                    }

                    @Override
                    public void interactAt(Hand hand2, Vec3d pos) {
                        booleans[0] = true;
                    }

                    @Override
                    public void attack() {}
                });
        return booleans[0];
    }

    private void onServerText(String string, boolean bl) {
        long l = System.currentTimeMillis();
        boolean bl2 = this.loop.phase() == AutoRtpLoop.Phase.TELEPORTING;
        if (bl2 || this.loop.sentWithin(l, 5000L)) {
            if (!bl || !RtpReply.looksLikePlayerChat(string, otherPlayerNames())) {
                RtpReply rtpReply = RtpReply.parse(string);
                boolean sentWithin = this.loop.sentWithin(l, 2500L);
                switch (rtpReply.kind()) {
                    case COOLDOWN:
                        this.loop.cooldown(l, rtpReply.millis());
                        this.noticeCooldown(l, rtpReply.millis());
                        break;
                    case WARMUP:
                        this.loop.warmup(l, rtpReply.millis(), this.config());
                        if (rtpReply.nextUseMs() >= 0L) {
                            this.loop.nextUseIn(l, rtpReply.nextUseMs());
                            this.noticeCooldown(l, rtpReply.nextUseMs());
                        }
                        break;
                    case CANCELLED:
                        this.loop.cancelled(l);
                        break;
                    case UNKNOWN_COMMAND:
                        if (sentWithin) {
                            this.stopWithRestartHint(
                                    ModuleChat.body()
                                            .warn("The server does not know ")
                                            .value("/" + this.command())
                                            .text(", check ")
                                            .value("RTP Command")
                                            .text(". Auto RTP stopped."));
                        }
                        break;
                    case NO_PERMISSION:
                        if (sentWithin) {
                            this.stopWithRestartHint(
                                    ModuleChat.body()
                                            .warn("No permission for ")
                                            .value("/" + this.command())
                                            .text(". Auto RTP stopped."));
                        }
                        break;
                    case COMBAT:
                        if (bl2 && sentWithin) {
                            this.stopWithRestartHint(
                                    ModuleChat.body()
                                            .danger("In combat")
                                            .text(", Auto RTP stopped."));
                        }
                }
            }
        }
    }

    private void noticeCooldown(long l, long l2) {
        long l3 = l + l2;
        if (l2 >= 60000L && l3 > this.noticedCooldownUntil + 5000L) {
            this.noticedCooldownUntil = l3;
            this.say(
                    ModuleChat.body()
                            .text("Server cooldown ")
                            .value(seconds(l2))
                            .text(", Auto RTP waits."));
        }
    }

    private static List<String> otherPlayerNames() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null) {
            return List.of();
        } else {
            ClientPlayerEntity player = client.player;
            UUID uuid = player == null ? null : player.getUuid();
            String string = player == null ? null : player.getGameProfile().name();
            ArrayList list = new ArrayList();
            for (PlayerListEntry player2 : networkHandler.getPlayerList()) {
                GameProfile profile = player2.getProfile();
                if (profile != null
                        && profile.name() != null
                        && !Objects.equals(profile.id(), uuid)
                        && !profile.name().equalsIgnoreCase(string)) {
                    list.add(profile.name());
                }
            }
            return list;
        }
    }

    private void onDamage(EntityDamageS2CPacket entityDamagePacket) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null
                && client.world != null
                && entityDamagePacket.entityId() == player.getId()) {
            Entity entity =
                    entityDamagePacket.sourceCauseId() >= 0
                            ? client.world.getEntityById(entityDamagePacket.sourceCauseId())
                            : null;
            if (entity != player) {
                boolean bl =
                        entity instanceof PlayerEntity
                                || entityDamagePacket
                                        .sourceType()
                                        .matchesKey(DamageTypes.PLAYER_ATTACK)
                                || entityDamagePacket
                                        .sourceType()
                                        .matchesKey(DamageTypes.PLAYER_EXPLOSION);
                if (bl) {
                    ModuleChat.Body body = ModuleChat.body().danger("Hit by ");
                    if (entity instanceof PlayerEntity player2) {
                        body.name(player2.getGameProfile().name());
                    } else {
                        body.text("a player");
                    }
                    this.stopWithRestartHint(body.text(", Auto RTP stopped (combat)."));
                }
            }
        }
    }

    private boolean handleMenu(
            MinecraftClient client,
            ClientPlayerEntity player,
            long lastMenuClickAt2,
            AutoRtpLoop.Config config) {
        if (this.loop.phase() == AutoRtpLoop.Phase.TELEPORTING && containerOpen(client, player)) {
            ScreenHandler currentScreenHandler = player.currentScreenHandler;
            if (currentScreenHandler.syncId != this.menuId) {
                if (currentScreenHandler.syncId == this.ignoredMenuId) {
                    return false;
                }
                if (!this.menuMayBeOurs(lastMenuClickAt2)) {
                    this.ignoredMenuId = currentScreenHandler.syncId;
                    return false;
                }
                if (this.menuClicks >= 3) {
                    this.stopWithRestartHint(
                            ModuleChat.body()
                                    .warn("The RTP menu keeps opening")
                                    .text(", Auto RTP stopped."));
                    return true;
                }
                this.menuId = currentScreenHandler.syncId;
                this.menuSeenAt = lastMenuClickAt2;
                this.menuClicked = false;
                this.loop.extend(lastMenuClickAt2, config);
            }
            if (!this.menuClicked && lastMenuClickAt2 - this.menuSeenAt >= 300L) {
                ArrayList list = new ArrayList();
                String string = this.menuItem.get().trim();
                int slot = findSlot(currentScreenHandler, string, list);
                boolean bl = lastMenuClickAt2 - this.menuSeenAt < 2500L;
                if (string.isEmpty()) {
                    if (list.isEmpty() && bl) {
                        return false;
                    } else {
                        this.stopWithRestartHint(
                                ModuleChat.body()
                                        .warn("The RTP command opened a menu")
                                        .text(", set ")
                                        .value("Menu Item")
                                        .text(" to the item to click")
                                        .text(nameList(list))
                                        .text(". Auto RTP stopped."));
                        return true;
                    }
                } else if (slot >= 0) {
                    client.interactionManager.clickSlot(
                            currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, player);
                    this.menuClicked = true;
                    this.menuClicks++;
                    this.lastMenuClickAt = lastMenuClickAt2;
                    this.loop.extend(lastMenuClickAt2, config);
                    return false;
                } else if (bl) {
                    return false;
                } else {
                    this.stopWithRestartHint(
                            ModuleChat.body()
                                    .warn("No \"" + string + "\" in the RTP menu")
                                    .text(nameList(list))
                                    .text(". Auto RTP stopped."));
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean menuMayBeOurs(long l) {
        if (this.commandSentAt != -9223372036854775808L && this.userActionAt < this.commandSentAt) {
            return l - this.commandSentAt <= 4000L
                    ? true
                    : this.menuClicks > 0 && l - this.lastMenuClickAt <= 4000L;
        } else {
            return false;
        }
    }

    private static int findSlot(ScreenHandler handler, String string, List<String> list) {
        ArrayList list2 = new ArrayList();
        ArrayList list3 = new ArrayList();
        ArrayList list4 = new ArrayList();
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory) {
                break;
            }
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                String string2 = RtpReply.normalize(stack.getName().getString());
                list2.add(slot);
                list3.add(string2);
                list4.add(Registries.ITEM.getId(stack.getItem()).getPath());
                if (!string2.isEmpty() && !list.contains(string2)) {
                    list.add(string2);
                }
            }
        }
        for (String string3 : string.split(",")) {
            String string4 = RtpReply.normalize(string3);
            if (!string4.isEmpty()) {
                String string5 = string4.replace(' ', '_');
                for (int i = 0; i < list2.size(); i++) {
                    if (((String) list3.get(i)).contains(string4)
                            || ((String) list4.get(i)).equals(string5)) {
                        return ((Slot) list2.get(i)).id;
                    }
                }
            }
        }
        return -1;
    }

    private static String nameList(List<String> list) {
        if (list.isEmpty()) {
            return "";
        } else {
            List list2 = list.size() > 10 ? list.subList(0, 10) : list;
            return " (items: "
                    + String.join(", ", list2)
                    + (list.size() > list2.size() ? ", ..." : "")
                    + ")";
        }
    }

    private static boolean containerOpen(MinecraftClient client, ClientPlayerEntity player) {
        return player.currentScreenHandler != player.playerScreenHandler
                && client.currentScreen instanceof HandledScreen;
    }

    private boolean ourMenuOpen(MinecraftClient client, ClientPlayerEntity player) {
        return this.menuId >= 0
                && containerOpen(client, player)
                && player.currentScreenHandler.syncId == this.menuId;
    }

    private void closeMenu(ClientPlayerEntity player) {
        if (this.menuId >= 0
                && player.currentScreenHandler != player.playerScreenHandler
                && player.currentScreenHandler.syncId == this.menuId) {
            player.closeHandledScreen();
        }
    }

    private void resetMenu() {
        this.menuId = -1;
        this.ignoredMenuId = -1;
        this.menuClicked = false;
        this.menuClicks = 0;
    }

    private void rememberSuspects() {
        this.knownSuspects.clear();
        LongIterator longIterator = PlayerBypassLightTracker.snapshots().keySet().iterator();
        while (longIterator.hasNext()) {
            long l = longIterator.next();
            if (PlayerBypassLightTracker.isSuspect(l)) {
                this.knownSuspects.add(l);
            }
        }
    }

    private long newSuspectNear(MinecraftClient client, ClientPlayerEntity player) {
        int i = client.options.getClampedViewDistance() + 1;
        int j = player.getBlockPos().getX() >> 4;
        int k = player.getBlockPos().getZ() >> 4;
        for (Entry long2ObjectEntry : PlayerBypassLightTracker.snapshots().long2ObjectEntrySet()) {
            long longKey = long2ObjectEntry.getLongKey();
            if (!this.knownSuspects.contains(longKey)
                    && PlayerBypassLightTracker.isSuspect(longKey)
                    && PlayerBypassLightTracker.isFlagged(
                            longKey, (ChunkLightSnapshot) long2ObjectEntry.getValue())) {
                int packedX = ChunkPos.getPackedX(longKey);
                int packedZ = ChunkPos.getPackedZ(longKey);
                if (Math.abs(packedX - j) <= i
                        && Math.abs(packedZ - k) <= i
                        && !this.atStartSpot(packedX, packedZ)) {
                    return longKey;
                }
            }
        }
        return -9223372036854775808L;
    }

    private boolean atStartSpot(int i, int j) {
        if (this.loop.landings() > 0) {
            return false;
        } else {
            int k = MinecraftClient.getInstance().options.getClampedViewDistance() + 1;
            return Math.abs(i - this.startChunkX) <= k && Math.abs(j - this.startChunkZ) <= k;
        }
    }

    private void progress(long tling, String string, AutoRtpLoop.Config config) {
        String label = this.label(config);
        String string2;
        if (string != null) {
            string2 = label + " - paused, " + string;
        } else {
            long nextSend = this.loop.nextSendIn(tling);
            string2 =
                    switch (this.loop.phase()) {
                        case TELEPORTING -> label + " - waiting for the teleport";
                        case SCANNING ->
                                label
                                        + " - scanning, "
                                        + seconds(this.loop.scanLeft(tling))
                                        + " left";
                        case WAITING ->
                                this.loop.cooldownLeft(tling) > 0L
                                        ? label + " - server cooldown, next in " + seconds(nextSend)
                                        : (this.loop.landings() == 0 && this.loop.sent() == 0
                                                ? label
                                                        + " - starting"
                                                        + (nextSend >= 1000L
                                                                ? " in " + seconds(nextSend)
                                                                : "")
                                                : (!this.lastTryLanded
                                                        ? label
                                                                + " - no teleport, retry in "
                                                                + seconds(nextSend)
                                                        : (this.loop.settling(tling)
                                                                ? label
                                                                        + " - no base yet, chunks"
                                                                        + " still loading"
                                                                : label
                                                                        + " - no base, next in "
                                                                        + seconds(nextSend))));
                        default -> "";
                    };
        }
        boolean bl =
                this.loop.phase() != AutoRtpLoop.Phase.TELEPORTING
                        && tling - this.lastBarAt >= 2000L;
        this.bar(tling, string2, bl);
    }

    private String label(AutoRtpLoop.Config config) {
        return "Auto RTP #"
                + this.loop.sent()
                + (config.maxRtps() > 0 ? "/" + config.maxRtps() : "");
    }

    private void bar(long lastBarAt2, String lastBar2, boolean bl) {
        if (!lastBar2.isEmpty() && (!lastBar2.equals(this.lastBar) || bl)) {
            this.lastBar = lastBar2;
            this.lastBarAt = lastBarAt2;
            ModuleChat.actionBarAlways(this.module, ModuleChat.body().text(lastBar2));
        }
    }

    private AutoRtpLoop.Config config() {
        return new AutoRtpLoop.Config(
                millis(this.scanTime),
                millis(this.delay),
                millis(this.timeout),
                this.maxRtps.getInt());
    }

    private String command() {
        String string = this.command.get().trim();
        while (string.startsWith("/")) {
            string = string.substring(1).trim();
        }
        return string.isEmpty() ? "rtp" : string;
    }

    private void say(ModuleChat.Body body) {
        ModuleChat.sendAlways(this.module, body);
    }

    private static long millis(NumberSetting setting) {
        return Math.round(setting.get() * 1000.0);
    }

    private static String seconds(long l) {
        long l2 = (l + 999L) / 1000L;
        return l2 >= 120L ? String.format(Locale.ROOT, "%dm %ds", l2 / 60L, l2 % 60L) : l2 + "s";
    }

    private static String rtpWord(int i) {
        return i == 1 ? " RTP" : " RTPs";
    }
}
