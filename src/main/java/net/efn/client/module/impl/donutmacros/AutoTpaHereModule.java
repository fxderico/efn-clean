package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.efn.client.config.FriendStore;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.misc.AutoEatModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.Setting;
import net.efn.client.module.setting.StringListSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.CompletionChannel;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.RotationUtils;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class AutoTpaHereModule extends ClientModule {

    private static final String ORDER_AZ = "A to Z";

    private static final String ORDER_ZA = "Z to A";

    private static final String ORDER_RANDOM = "Random";

    private static final String EXTRA_CHARS = "0123456789_";

    private static final String SUB_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_";

    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9._]{1,32}");

    private static final Pattern FORMATTING = Pattern.compile("\u00a7.");

    private static final Pattern RATE_LIMIT =
            Pattern.compile("^you need to wait another ([\\d.]+) seconds? to execute a command");

    private static final Pattern NOT_ONLINE =
            Pattern.compile("(that|this) (user|player) is not online|player is not online");

    private static final Pattern SENT_REQUEST =
            Pattern.compile("^you sent (\\S+) (a )?teleport( here)? request");

    private static final int SUGGEST_CAP = 990;

    private static final long DETECT_TIMEOUT_MS = 5000L;

    private static final long LETTER_TIMEOUT_MS = 5000L;

    private static final long LETTER_TIMEOUT_LOCAL_MS = 3000L;

    private static final long PREFIX_TIMEOUT_MS = 4000L;

    private static final int SPLIT_MAX_INFLIGHT = 6;

    private static final long PROBE_WAIT_MS = 700L;

    private static final long PROBE_MIN_WAIT_MS = 100L;

    private static final long PROBE_STALE_MS = 4000L;

    private static final int PROBE_GIVE_UP = 25;

    private static final long REPLY_WINDOW_MS = 2500L;

    private static final long JOIN_REPEAT_MS = 3600000L;

    private static final long SWEEP_GAP_MS = 200L;

    private static final long SWEEP_MIN_ROUND_MS = 45000L;

    private static final int SWEEP_MAX_INFLIGHT = 4;

    private static final int JOIN_QUEUE_MAX = 300;

    private static final long RANDOM_RESCAN_MS = 180000L;

    private static final long RESPAWN_FIRST_MS = 700L;

    private static final long RESPAWN_RETRY_MS = 600L;

    private static final int RESPAWN_COMMAND_DELAY_TICKS = 20;

    private static final int EAT_MAX_TICKS = 40;

    private static final long PRUNE_EVERY_MS = 60000L;

    private static final int CHAT_QUEUE_MAX = 256;

    private static final long SELL_COMMAND_GAP_MS = 400L;

    private static final String[] SWORD_RANK =
            new String[] {"netherite", "diamond", "iron", "stone", "golden", "wooden"};

    private final ModeSetting command =
            this.addSetting("command", new ModeSetting("Command", "tpahere", "tpahere", "tpa"));

    private final ModeSetting order =
            this.addSetting(
                    "order", new ModeSetting("Order", "A to Z", "A to Z", "Z to A", "Random"));

    private final StringSetting letters =
            this.addSetting(
                    "letters",
                    new StringSetting("Letters", "").hint("All letters, or e.g. abc or k-m"));

    private final StringSetting startLetter =
            this.addSetting(
                    "start-letter",
                    new StringSetting("Start Letter", "").hint("First letter in the order"));

    private final BooleanSetting numbers =
            this.addSetting("numbers", new BooleanSetting("Include 0-9 and _", false));

    private final BooleanSetting bedrock =
            this.addSetting("bedrock", new BooleanSetting("Bedrock Players Only", false));

    private final BooleanSetting repeat =
            this.addSetting("repeat", new BooleanSetting("Repeat Forever", true));

    private final NumberSetting delay =
            this.addSetting(
                    "delay", new NumberSetting("Delay (s)", 1.0F, 0.25F, 30.0F).step(0.25F));

    private final BooleanSetting skipOffline =
            this.addSetting("skip-offline", new BooleanSetting("Skip Offline Players", true));

    private final BooleanSetting joinersFirst =
            this.addSetting("joiners-first", new BooleanSetting("New Players First", false));

    private final StringListSetting neverRequest =
            this.addSetting("never-request", new StringListSetting("Never Request"));

    private final NumberSetting sendMinutes =
            this.addSetting(
                    "send-minutes",
                    new NumberSetting("Send For (min, 0 = always)", 0.0F, 0.0F, 240.0F).step(1.0F));

    private final NumberSetting restMinutes =
            this.addSetting(
                    "rest-minutes",
                    new NumberSetting("Then Rest (min)", 0.0F, 0.0F, 240.0F).step(1.0F));

    private final NumberSetting pauseRadius =
            this.addSetting(
                    "pause-radius",
                    new NumberSetting("Pause Radius", 6.0F, 1.0F, 64.0F).step(0.5F));

    private final NumberSetting resumeAfter =
            this.addSetting(
                    "resume-after",
                    new NumberSetting("Resume After (s)", 2.5F, 0.0F, 60.0F).step(0.5F));

    private final BooleanSetting attack =
            this.addSetting("attack", new BooleanSetting("Attack", true));

    private final NumberSetting attackRadius =
            this.addSetting(
                    "attack-radius",
                    new NumberSetting("Attack Radius", 1.5F, 0.5F, 6.0F).step(0.1F));

    private final NumberSetting swordSlot =
            this.addSetting(
                    "sword-slot",
                    new NumberSetting("Sword Slot (0 = auto)", 0.0F, 0.0F, 9.0F).step(1.0F));

    private final NumberSetting eatBelow =
            this.addSetting(
                    "eat-below",
                    new NumberSetting("Eat Below (hearts, 0 = never)", 5.0F, 0.0F, 10.0F)
                            .step(0.5F));

    private final NumberSetting appleSlot =
            this.addSetting(
                    "apple-slot",
                    new NumberSetting("Apple Slot (0 = auto)", 0.0F, 0.0F, 9.0F).step(1.0F));

    private final NumberSetting eatCooldown =
            this.addSetting(
                    "eat-cooldown",
                    new NumberSetting("Eat Cooldown (s)", 2.5F, 0.5F, 30.0F).step(0.5F));

    private final BooleanSetting enchantedFirst =
            this.addSetting("enchanted-first", new BooleanSetting("Enchanted Apples First", false));

    private final BooleanSetting autoRespawn =
            this.addSetting("auto-respawn", new BooleanSetting("Auto Respawn", true));

    private final StringSetting respawnCommand =
            this.addSetting(
                    "respawn-command", new StringSetting("Respawn Command", "").hint("e.g. /home"));

    private final BooleanSetting autoSell =
            this.addSetting("auto-sell", new BooleanSetting("Auto Sell", true));

    private final StringListSetting neverSell =
            this.addSetting(
                    "never-sell",
                    new StringListSetting(
                                    "Never Sell",
                                    "diamond_sword",
                                    "golden_apple",
                                    "totem_of_undying")
                            .itemPicker());

    private final SellRoutine sell =
            new SellRoutine(this, this.neverSell, this::keepForBot, this::onSellCommand);

    private final TeleportConfirm teleportConfirm = new TeleportConfirm(this);

    private final CompletionRequests completions = new CompletionRequests();

    private final Queue<String> chatLines = new ConcurrentLinkedQueue<>();

    private boolean progressReset = true;

    private String letter = "";

    private final Set<String> sentThisLetter = new HashSet<>();

    private final Map<String, Long> msgAt = new HashMap<>();

    private final Map<String, Long> joinMsgAt = new HashMap<>();

    private final Set<String> splitLetters = new HashSet<>();

    private final ArrayDeque<String> retryQueue = new ArrayDeque<>();

    private AutoTpaHereModule.Phase phase = AutoTpaHereModule.Phase.DETECT;

    private int detectSlash = -1;

    private int detectBare = -1;

    private long detectUntil;

    private AutoTpaHereModule.Collector collector;

    private List<String> queue = List.of();

    private int queueIndex;

    private AutoTpaHereModule.Delivery delivery;

    private final Map<String, AutoTpaHereModule.Bucket> buckets = new HashMap<>();

    private String lastRandomLetter = "";

    private boolean sweepOn;

    private List<String> sweepLetters = List.of();

    private List<String> sweepList = List.of();

    private int sweepIndex;

    private final List<AutoTpaHereModule.Inflight> sweepInflight = new ArrayList<>();

    private long sweepLastFire;

    private int sweepRounds;

    private long sweepRoundAt;

    private final Map<String, Set<String>> prefixCache = new HashMap<>();

    private final ArrayDeque<String> joinQueue = new ArrayDeque<>();

    private final Set<String> joinQueued = new HashSet<>();

    private String selfName = "";

    private boolean onlineCheckOff;

    private int probeMisses;

    private long lastSendAt;

    private long rateHoldUntil;

    private String lastTarget = "";

    private long lastTargetAt;

    private boolean lastTargetCounted = true;

    private long lastPruneAt;

    private final List<AutoTpaHereModule.Near> nearList = new ArrayList<>();

    private long lastNearAt;

    private boolean wasHolding;

    private long cycleStartedAt;

    private boolean wasResting;

    private boolean eating;

    private int eatTicks;

    private boolean eatStarted;

    private float eatStartHealth;

    private long lastEatAt;

    private boolean warnedNoSword;

    private boolean warnedNoApple;

    private boolean wasDead;

    private long deadSince;

    private long respawnTriedAt;

    private int respawnCommandIn;

    private int totalSent;

    private int totalOffline;

    private int totalDisabled;

    private int totalJoiners;

    private int totalArrivals;

    private int totalHits;

    private int totalEaten;

    private int totalDeaths;

    public AutoTpaHereModule() {
        super(
                "auto-tpahere",
                "Tpahere Kill Bot",
                "Sends tpahere requests to online players one by one, pauses when someone arrives"
                        + " and fights them. Servers can detect this.",
                ModuleCategory.DONUT_MACROS);
        this.usesNotifications();
        this.letters.onChange(this::lettersChanged);
        this.startLetter.onChange(this::lettersChanged);
        this.order.onChange(this::lettersChanged);
        this.numbers.onChange(this::lettersChanged);
        this.bedrock.onChange(this::lettersChanged);
        this.startLetter.visibleWhen(() -> !this.order.is("Random"));
        this.restMinutes.visibleWhen(() -> this.sendMinutes.get() > 0.0F);
        this.attackRadius.visibleWhen(this.attack::get);
        this.swordSlot.visibleWhen(this.attack::get);
        for (Setting setting :
                new Setting[] {this.appleSlot, this.eatCooldown, this.enchantedFirst}) {
            setting.visibleWhen(() -> this.eatBelow.get() > 0.0F);
        }
        this.respawnCommand.visibleWhen(this.autoRespawn::get);
        this.neverSell.visibleWhen(this.autoSell::get);
        this.autoSell.onChange(
                () -> {
                    if (!this.autoSell.get()) {
                        this.sell.abort(MinecraftClient.getInstance());
                    }
                });
        this.neverRequest.privateValue();
        this.respawnCommand.privateValue();
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        this.resetRun();
        if (this.progressReset || !this.letterList().contains(this.letter)) {
            this.letter = this.firstLetter();
            this.sentThisLetter.clear();
            this.progressReset = false;
        }
        this.info(
                ModuleChat.body()
                        .text("Started ")
                        .value("/" + this.command.get())
                        .text(", ")
                        .value(this.orderLabel())
                        .text(
                                this.order.is("Random")
                                        ? ""
                                        : " from " + this.letter.toUpperCase(Locale.ROOT))
                        .text(", every ")
                        .value(formatSeconds(this.delayMs()))
                        .text(this.attack.get() ? ", attacking" : ""));
    }

    @Override
    protected void onDisable() {
        this.sell.abort(MinecraftClient.getInstance());
        this.stopEating(MinecraftClient.getInstance());
        this.completions.clear();
        this.chatLines.clear();
        this.delivery = null;
        this.collector = null;
        if (this.totalSent > 0 || this.totalArrivals > 0) {
            this.info(ModuleChat.body().text(this.statsText()));
        }
    }

    private void lettersChanged() {
        this.progressReset = true;
        if (this.isEnabled()) {
            this.resetRun();
            this.letter = this.firstLetter();
            this.sentThisLetter.clear();
            this.progressReset = false;
        }
    }

    private void resetRun() {
        long l = System.currentTimeMillis();
        this.completions.clear();
        this.chatLines.clear();
        this.msgAt.clear();
        this.joinMsgAt.clear();
        this.splitLetters.clear();
        this.retryQueue.clear();
        this.phase = AutoTpaHereModule.Phase.DETECT;
        this.detectSlash = -1;
        this.detectBare = -1;
        this.collector = null;
        this.queue = List.of();
        this.queueIndex = 0;
        this.delivery = null;
        this.buckets.clear();
        this.lastRandomLetter = "";
        this.sweepOn = false;
        this.sweepInflight.clear();
        this.sweepList = List.of();
        this.sweepIndex = 0;
        this.sweepRounds = 0;
        this.prefixCache.clear();
        this.joinQueue.clear();
        this.joinQueued.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        this.selfName =
                client.getSession() == null ? "" : normalizeIgn(client.getSession().getUsername());
        this.onlineCheckOff = false;
        this.probeMisses = 0;
        this.lastSendAt = 0L;
        this.rateHoldUntil = 0L;
        this.lastTarget = "";
        this.lastTargetCounted = true;
        this.lastPruneAt = l;
        this.nearList.clear();
        this.lastNearAt = 0L;
        this.wasHolding = false;
        this.cycleStartedAt = l;
        this.wasResting = false;
        this.eating = false;
        this.lastEatAt = 0L;
        this.warnedNoSword = false;
        this.warnedNoApple = false;
        this.wasDead = false;
        this.respawnCommandIn = 0;
        this.sell.reset();
        this.teleportConfirm.reset();
        this.totalJoiners = 0;
        this.totalDisabled = 0;
        this.totalOffline = 0;
        this.totalSent = 0;
        this.totalDeaths = 0;
        this.totalEaten = 0;
        this.totalHits = 0;
        this.totalArrivals = 0;
    }

    @EventHandler
    public void onPacket(PacketReceiveNettyEvent event) {
        Packet packet = event.packet;
        GameMessageS2CPacket gameMessagePacket;
        if (packet instanceof GameMessageS2CPacket
                && (gameMessagePacket = (GameMessageS2CPacket) packet).content() != null
                && this.chatLines.size() < 256) {
            this.chatLines.offer(gameMessagePacket.content().getString());
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player2 = client.player;
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (player2 != null
                && client.world != null
                && networkHandler != null
                && client.interactionManager != null) {
            this.teleportConfirm.tick(client);
            long l = System.currentTimeMillis();
            this.drainChat(l);
            if (l - this.lastPruneAt > 60000L) {
                this.prune(l);
            }
            if (this.handleDeath(client, player2, l)) {
                this.sell.abort(client);
                this.stall();
            } else {
                this.tickRespawnCommand(networkHandler);
                this.scanNear(client, player2);
                if (this.sell.active()
                        && (!this.nearList.isEmpty() || this.wantsToEat(player2, l))) {
                    this.sell.abort(client);
                }
                if (!this.tickEating(client, player2, l) && !this.startEating(client, player2, l)) {
                    this.fight(client, player2);
                    this.pumpSweep(l);
                    if (this.holding(l)) {
                        this.stall();
                    } else if (this.autoSell.get()
                            && this.sell.step(
                                    client,
                                    player2,
                                    networkHandler,
                                    l,
                                    l >= this.rateHoldUntil && l - this.lastSendAt >= 400L)) {
                        this.stall();
                    } else if (!this.resting(l) && l >= this.rateHoldUntil) {
                        this.step(networkHandler, l);
                    }
                } else {
                    this.stall();
                }
            }
        } else {
            this.sell.abort(client);
            this.stopEating(client);
            this.stall();
        }
    }

    private void stall() {
        if (this.delivery != null) {
            this.delivery.stale = true;
        }
    }

    private void step(ClientPlayNetworkHandler clientPlayNetworkHandler, long l) {
        switch (this.phase) {
            case DETECT:
                this.stepDetect(l);
                break;
            case COLLECT:
                this.stepCollect(clientPlayNetworkHandler, l);
                break;
            case SEND:
                this.stepSend(clientPlayNetworkHandler, l);
                break;
            case PICK:
                this.stepPick(clientPlayNetworkHandler, l);
        }
    }

    private void stepDetect(long l) {
        String string = this.order.is("Random") ? "a" : this.letter;
        if (this.detectSlash < 0 && this.detectBare < 0) {
            this.detectSlash =
                    this.completions.fire(this.partialFor(string), CompletionChannel.Form.SLASH);
            this.detectBare =
                    this.completions.fire(this.partialFor(string), CompletionChannel.Form.BARE);
            this.detectUntil = l + 5000L;
        } else {
            if (this.completions.poll(this.detectSlash) != null) {
                this.completions.form(CompletionChannel.Form.SLASH);
            } else if (this.completions.poll(this.detectBare) != null) {
                this.completions.form(CompletionChannel.Form.BARE);
            } else if (l < this.detectUntil) {
                return;
            }
            this.completions.forget(this.detectSlash);
            this.completions.forget(this.detectBare);
            if (this.joinersFirst.get()) {
                this.startSweep();
            }
            if (this.order.is("Random")) {
                this.info(
                        ModuleChat.body()
                                .text("Random order, letters ")
                                .value(
                                        String.join("", this.letterList())
                                                .toUpperCase(Locale.ROOT)));
                this.phase = AutoTpaHereModule.Phase.PICK;
            } else {
                this.beginLetter();
            }
        }
    }

    private void beginLetter() {
        this.collector = new AutoTpaHereModule.Collector(this.letter);
        this.phase = AutoTpaHereModule.Phase.COLLECT;
    }

    private void stepCollect(ClientPlayNetworkHandler clientPlayNetworkHandler, long l) {
        if (this.collector.step(clientPlayNetworkHandler, l)) {
            AutoTpaHereModule.Collector collector2 = this.collector;
            this.collector = null;
            if (this.order.is("Random")) {
                AutoTpaHereModule.Bucket bucket = this.bucket(collector2.letter);
                bucket.scanned = true;
                bucket.at = l;
                bucket.names = collector2.names;
                bucket.unused = this.unusedFromNames(collector2.names);
                if (collector2.names.isEmpty()) {
                    this.info(
                            ModuleChat.body()
                                    .text(collector2.timedOut ? "No answer for " : "No names for ")
                                    .value(collector2.letter.toUpperCase(Locale.ROOT)));
                }
                this.phase = AutoTpaHereModule.Phase.PICK;
            } else {
                this.queue = this.unusedFromNames(collector2.names);
                this.queueIndex = 0;
                if (this.queue.isEmpty()) {
                    this.info(
                            ModuleChat.body()
                                    .text(
                                            collector2.timedOut
                                                    ? "No answer for "
                                                    : "No new names for ")
                                    .value(collector2.letter.toUpperCase(Locale.ROOT))
                                    .text(collector2.timedOut ? ", next letter" : ""));
                } else {
                    this.info(
                            ModuleChat.body()
                                    .text(isLetter(collector2.letter) ? "Letter " : "Prefix ")
                                    .value(collector2.letter.toUpperCase(Locale.ROOT))
                                    .text(": ")
                                    .value(this.queue.size())
                                    .text(this.queue.size() == 1 ? " player" : " players")
                                    .text(collector2.split ? " (past the 1000 cap)" : ""));
                }
                this.phase = AutoTpaHereModule.Phase.SEND;
            }
        }
    }

    private void stepSend(ClientPlayNetworkHandler clientPlayNetworkHandler, long l) {
        if (this.delivery == null) {
            this.delivery = this.nextPriority();
        }
        while (this.delivery == null && this.queueIndex < this.queue.size()) {
            String string;
            String key;
            if (!this.sentThisLetter.contains(key = key(string = this.queue.get(this.queueIndex++)))
                    && !this.recentlyJoinMessaged(key, l)
                    && !this.excluded(string)) {
                this.delivery = new AutoTpaHereModule.Delivery(string, false);
            }
        }
        if (this.delivery == null) {
            this.advanceLetter();
        } else {
            if (this.delivery.step(clientPlayNetworkHandler, l) != AutoTpaHereModule.Result.WAIT) {
                this.delivery = null;
            }
        }
    }

    private void stepPick(ClientPlayNetworkHandler clientPlayNetworkHandler, long l) {
        if (this.delivery == null) {
            this.delivery = this.nextPriority();
        }
        if (this.delivery == null) {
            List list = this.letterList();
            for (int i = 0; i < list.size() + 4 && this.delivery == null; i++) {
                String randomLetter = this.pickRandomLetter(list, l);
                if (randomLetter == null) {
                    if (!this.repeat.get()) {
                        this.finish("Finished the random pass.");
                        return;
                    }
                    this.reshuffle(list);
                    randomLetter = this.pickRandomLetter(list, l);
                    if (randomLetter == null) {
                        this.finish("No names found for these letters.");
                        return;
                    }
                    this.info(ModuleChat.body().text("Random pass done, starting over."));
                }
                AutoTpaHereModule.Bucket bucket = this.bucket(randomLetter);
                if (!bucket.scanned || stale(bucket, l)) {
                    this.collector = new AutoTpaHereModule.Collector(randomLetter);
                    this.phase = AutoTpaHereModule.Phase.COLLECT;
                    return;
                }
                if (!bucket.unused.isEmpty()) {
                    List unused2 = bucket.unused;
                    int j = ThreadLocalRandom.current().nextInt(unused2.size());
                    String string = (String) unused2.get(j);
                    unused2.set(j, (String) unused2.get(unused2.size() - 1));
                    unused2.remove(unused2.size() - 1);
                    this.lastRandomLetter = randomLetter;
                    if (!this.excluded(string) && !this.sentThisLetter.contains(key(string))) {
                        this.delivery = new AutoTpaHereModule.Delivery(string, false);
                    }
                }
            }
            if (this.delivery == null) {
                return;
            }
        }
        if (this.delivery.step(clientPlayNetworkHandler, l) != AutoTpaHereModule.Result.WAIT) {
            this.delivery = null;
        }
    }

    private String pickRandomLetter(List<String> list, long l) {
        ArrayList list2 = new ArrayList();
        for (String string : list) {
            AutoTpaHereModule.Bucket bucket = this.buckets.get(string);
            if (bucket == null
                    || !bucket.scanned
                    || !bucket.unused.isEmpty()
                    || this.repeat.get() && stale(bucket, l)) {
                list2.add(string);
            }
        }
        if (list2.size() > 1 && !this.lastRandomLetter.isEmpty()) {
            ArrayList list3 = new ArrayList(list2);
            list3.remove(this.lastRandomLetter);
            if (!list3.isEmpty()) {
                list2 = list3;
            }
        }
        return list2.isEmpty()
                ? null
                : (String) list2.get(ThreadLocalRandom.current().nextInt(list2.size()));
    }

    private void reshuffle(List<String> list) {
        this.sentThisLetter.clear();
        for (String string : list) {
            AutoTpaHereModule.Bucket bucket = this.buckets.get(string);
            if (bucket != null && bucket.scanned) {
                bucket.unused = this.unusedFromNames(bucket.names);
            }
        }
    }

    private AutoTpaHereModule.Bucket bucket(String string) {
        return this.buckets.computeIfAbsent(string, stringx -> new AutoTpaHereModule.Bucket());
    }

    private static boolean stale(AutoTpaHereModule.Bucket bucket, long l) {
        return l - bucket.at > 180000L;
    }

    private void advanceLetter() {
        List list = this.letterList();
        int i = list.indexOf(this.letter);
        boolean bl = i >= 0 && i >= list.size() - 1;
        this.letter = i >= 0 && !bl ? (String) list.get(i + 1) : (String) list.get(0);
        this.sentThisLetter.clear();
        if (bl && !this.repeat.get()) {
            this.progressReset = true;
            this.finish("Finished " + this.orderLabel() + ".");
        } else {
            this.beginLetter();
        }
    }

    private void finish(String string) {
        this.info(ModuleChat.body().text(string));
        this.requestDisable();
    }

    private AutoTpaHereModule.Delivery nextPriority() {
        while (!this.retryQueue.isEmpty()) {
            String retry = this.retryQueue.poll();
            if (!this.excluded(retry)) {
                return new AutoTpaHereModule.Delivery(retry, false);
            }
        }
        if (!this.sweepOn) {
            return null;
        } else {
            long l = System.currentTimeMillis();
            while (!this.joinQueue.isEmpty()) {
                String join = this.joinQueue.poll();
                this.joinQueued.remove(key(join));
                if (this.joinEligible(join, l)) {
                    return new AutoTpaHereModule.Delivery(join, true);
                }
            }
            return null;
        }
    }

    private void send(
            ClientPlayNetworkHandler clientPlayNetworkHandler,
            String lastTarget2,
            boolean bl,
            long lastTargetAt2) {
        this.lastTarget = lastTarget2;
        this.lastTargetAt = lastTargetAt2;
        this.lastTargetCounted = false;
        clientPlayNetworkHandler.sendChatCommand(this.command.get() + " " + lastTarget2);
        this.teleportConfirm.sent();
        this.lastSendAt = lastTargetAt2;
        this.totalSent++;
        if (bl) {
            this.totalJoiners++;
        }
        String key = key(lastTarget2);
        this.msgAt.put(key, lastTargetAt2);
        if (bl) {
            this.joinMsgAt.put(key, lastTargetAt2);
        }
        this.sentThisLetter.add(key);
    }

    private Boolean probeAnswer(List<String> list, String string) {
        if (list == null) {
            return null;
        } else {
            for (String string2 : list) {
                if (sameIgn(this.stripCommand(string2), string)) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }
    }

    private void noteProbe(Boolean flag) {
        if (!Boolean.FALSE.equals(flag)) {
            this.probeMisses = 0;
        } else {
            if (++this.probeMisses >= 25 && !this.onlineCheckOff) {
                this.onlineCheckOff = true;
                this.info(
                        ModuleChat.body()
                                .warn("Online check off")
                                .text(
                                        ": 25 players in a row looked offline, the server may not"
                                                + " confirm names."));
            }
        }
    }

    private List<String> localNames(
            ClientPlayNetworkHandler clientPlayNetworkHandler, String string) {
        ArrayList list = new ArrayList();
        for (PlayerListEntry player2 : clientPlayNetworkHandler.getPlayerList()) {
            String string2 = player2.getProfile() == null ? null : player2.getProfile().name();
            if (string2 != null && validName(string2) && this.matchesLetter(string2, string)) {
                list.add(string2);
            }
        }
        return list;
    }

    private void startSweep() {
        this.sweepLetters = this.letterList();
        this.sweepList = List.of();
        this.sweepIndex = 0;
        this.sweepRounds = 0;
        this.sweepInflight.clear();
        this.sweepOn = !this.sweepLetters.isEmpty();
    }

    private void pumpSweep(long sweepRoundAt2) {
        if (this.sweepOn) {
            this.sweepInflight.removeIf(
                    inflight -> {
                        List completion = this.completions.poll(inflight.id());
                        if (completion != null) {
                            this.sweepResult(inflight.prefix(), completion, sweepRoundAt2);
                        } else if (sweepRoundAt2 - inflight.at() <= 4000L) {
                            return false;
                        }
                        this.completions.forget(inflight.id());
                        return true;
                    });
            if (this.sweepInflight.size() < 4 && sweepRoundAt2 - this.sweepLastFire >= 200L) {
                if (this.sweepIndex >= this.sweepList.size()) {
                    if (this.sweepRounds > 0 && sweepRoundAt2 - this.sweepRoundAt < 45000L) {
                        return;
                    }
                    ArrayList list = new ArrayList();
                    for (String sweepLetter : this.sweepLetters) {
                        if (this.splitLetters.contains(sweepLetter)) {
                            list.addAll(subPrefixes(sweepLetter));
                        } else {
                            list.add(sweepLetter);
                        }
                    }
                    this.sweepList = list;
                    this.sweepIndex = 0;
                    this.sweepRounds++;
                    this.sweepRoundAt = sweepRoundAt2;
                }
                if (!this.sweepList.isEmpty()) {
                    String sweep = this.sweepList.get(this.sweepIndex++);
                    int fire = this.completions.fire(this.partialFor(sweep));
                    this.sweepLastFire = sweepRoundAt2;
                    if (fire >= 0) {
                        this.sweepInflight.add(
                                new AutoTpaHereModule.Inflight(fire, sweep, sweepRoundAt2));
                    }
                }
            }
        }
    }

    private void sweepResult(String string, List<String> list, long l) {
        String string2 = string.substring(0, 1);
        if (list.size() >= 990) {
            if (string.length() == 1 && this.splitLetters.add(string2)) {
                this.prefixCache.remove(string);
            }
        } else {
            HashMap map = new HashMap();
            for (String string3 : this.namesForPrefix(list, string)) {
                String cleanName = cleanName(this.stripCommand(string3));
                if (validName(cleanName)) {
                    map.put(key(cleanName), cleanName);
                }
            }
            Set set = this.prefixCache.get(string);
            if (set == null || set.isEmpty() || !map.isEmpty()) {
                this.prefixCache.put(string, new HashSet<>(map.keySet()));
                if (set != null) {
                    ArrayList list2 = new ArrayList();
                    for (Entry entry : (Iterable<Entry>) map.entrySet()) {
                        if (!set.contains(entry.getKey())) {
                            list2.add((String) entry.getValue());
                        }
                    }
                    if (list2.size() <= 15 || !(list2.size() > set.size() * 0.3)) {
                        for (String string4 : (Iterable<String>) list2) {
                            this.queueJoiner(string4, l);
                        }
                    }
                }
            }
        }
    }

    private void queueJoiner(String string, long l) {
        String key = key(string);
        if (!key.isEmpty() && !this.joinQueued.contains(key) && this.joinEligible(string, l)) {
            if (this.joinQueue.size() >= 300) {
                this.joinQueued.remove(key(this.joinQueue.poll()));
            }
            this.joinQueue.add(string);
            this.joinQueued.add(key);
        }
    }

    private boolean joinEligible(String string, long l) {
        String key = key(string);
        if (!key.isEmpty() && !this.excluded(string) && !this.sentThisLetter.contains(key)) {
            Long long_Value = this.msgAt.get(key);
            return long_Value == null || l - long_Value >= 3600000L;
        } else {
            return false;
        }
    }

    private boolean recentlyJoinMessaged(String string, long l) {
        Long long_Value = this.joinMsgAt.get(string);
        return long_Value != null && l - long_Value < 3600000L;
    }

    private List<String> unusedFromNames(List<String> list) {
        long l = System.currentTimeMillis();
        ArrayList list2 = new ArrayList();
        for (String string : list) {
            String key = key(string);
            if (!key.isEmpty()
                    && !this.excluded(string)
                    && !this.sentThisLetter.contains(key)
                    && !this.recentlyJoinMessaged(key, l)) {
                list2.add(string);
            }
        }
        return list2;
    }

    private void drainChat(long l) {
        String chatLine;
        while ((chatLine = this.chatLines.poll()) != null) {
            String string =
                    FORMATTING.matcher(chatLine).replaceAll("").trim().toLowerCase(Locale.ROOT);
            if (!string.isEmpty()) {
                if (string.contains("disabled tpahere")) {
                    if (this.claimReply(l)) {
                        this.totalDisabled++;
                    }
                } else if (NOT_ONLINE.matcher(string).find()) {
                    if (this.claimReply(l)) {
                        this.totalOffline++;
                    }
                } else {
                    Matcher matcher = RATE_LIMIT.matcher(string);
                    if (matcher.find()) {
                        try {
                            this.rateHoldUntil =
                                    l
                                            + (long)
                                                    Math.ceil(
                                                            Math.min(
                                                                            30.0,
                                                                            Double.parseDouble(
                                                                                    matcher.group(
                                                                                            1)))
                                                                    * 1000.0)
                                            + 80L;
                        } catch (NumberFormatException exception) {
                        }
                        this.sell.commandRejected(this.rateHoldUntil);
                        if (this.claimReply(l)) {
                            this.totalSent = Math.max(0, this.totalSent - 1);
                            if (!this.retryQueue.contains(this.lastTarget)) {
                                this.retryQueue.add(this.lastTarget);
                            }
                        }
                    } else {
                        Matcher matcher2 = SENT_REQUEST.matcher(string);
                        if (matcher2.find() && sameIgn(this.lastTarget, matcher2.group(1))) {
                            this.lastTargetCounted = true;
                        }
                    }
                }
            }
        }
    }

    private boolean claimReply(long l) {
        if (!this.lastTarget.isEmpty()
                && !this.lastTargetCounted
                && l - this.lastTargetAt <= 2500L) {
            this.lastTargetCounted = true;
            return true;
        } else {
            return false;
        }
    }

    private void prune(long lastPruneAt2) {
        this.lastPruneAt = lastPruneAt2;
        this.msgAt.values().removeIf(long_ -> lastPruneAt2 - long_ > 3600000L);
        this.joinMsgAt.values().removeIf(long_ -> lastPruneAt2 - long_ > 3600000L);
        this.completions.prune(lastPruneAt2);
    }

    private boolean handleDeath(
            MinecraftClient client, ClientPlayerEntity player2, long respawnTriedAt2) {
        if (player2.isDead()) {
            if (!this.wasDead) {
                this.wasDead = true;
                this.deadSince = respawnTriedAt2;
                this.respawnTriedAt = 0L;
                this.totalDeaths++;
                this.stopEating(client);
                this.info(ModuleChat.body().danger("You died."));
            }
            if (this.autoRespawn.get()
                    && respawnTriedAt2 - this.deadSince >= 700L
                    && respawnTriedAt2 - this.respawnTriedAt >= 600L) {
                this.respawnTriedAt = respawnTriedAt2;
                player2.requestRespawn();
            }
            return true;
        } else {
            if (this.wasDead) {
                this.wasDead = false;
                this.nearList.clear();
                this.lastNearAt = 0L;
                if (client.currentScreen instanceof DeathScreen) {
                    client.setScreen(null);
                }
                this.info(ModuleChat.body().good("Respawned."));
                if (!this.respawnCommand.get().isBlank()) {
                    this.respawnCommandIn = 20;
                }
            }
            return false;
        }
    }

    private void tickRespawnCommand(ClientPlayNetworkHandler clientPlayNetworkHandler) {
        if (this.respawnCommandIn > 0 && --this.respawnCommandIn <= 0) {
            String string = this.respawnCommand.get().trim();
            while (string.startsWith("/")) {
                string = string.substring(1);
            }
            if (!string.isEmpty()) {
                clientPlayNetworkHandler.sendChatCommand(string);
            }
        }
    }

    private void scanNear(MinecraftClient client, ClientPlayerEntity player2) {
        this.nearList.clear();
        double pauseRadius2 = this.pauseRadius.get();
        for (AbstractClientPlayerEntity player3 : client.world.getPlayers()) {
            double distance2;
            String string;
            if (player3 != player2
                    && !player3.getUuid().equals(player2.getUuid())
                    && (string = player3.getGameProfile().name()) != null
                    && !this.isSelf(string)
                    && !isFriend(string)
                    && (distance2 = player2.getEntityPos().distanceTo(player3.getEntityPos()))
                            <= pauseRadius2) {
                this.nearList.add(new AutoTpaHereModule.Near(player3, string, distance2));
            }
        }
        this.nearList.sort((near, near2) -> Double.compare(near.distance(), near2.distance()));
    }

    private boolean holding(long lastNearAt2) {
        if (!this.nearList.isEmpty()) {
            this.lastNearAt = lastNearAt2;
        }
        boolean bl =
                !this.nearList.isEmpty()
                        || this.lastNearAt > 0L
                                && lastNearAt2 - this.lastNearAt
                                        < (long) (this.resumeAfter.get() * 1000.0F);
        if (bl && !this.wasHolding && !this.nearList.isEmpty()) {
            AutoTpaHereModule.Near near = this.nearList.get(0);
            this.totalArrivals++;
            this.info(
                    ModuleChat.body()
                            .name(near.name())
                            .text(" is here (")
                            .value(String.format(Locale.ROOT, "%.1f", near.distance()))
                            .text(" blocks). Requests paused."));
        } else if (!bl && this.wasHolding) {
            this.info(ModuleChat.body().text("Alone again, resuming."));
        }
        this.wasHolding = bl;
        return bl;
    }

    private boolean resting(long l) {
        long l2 = (long) (this.sendMinutes.get() * 60000.0F);
        long l3 = (long) (this.restMinutes.get() * 60000.0F);
        boolean bl = l2 > 0L && l3 > 0L && (l - this.cycleStartedAt) % (l2 + l3) >= l2;
        if (bl != this.wasResting) {
            this.wasResting = bl;
            this.info(
                    bl
                            ? ModuleChat.body()
                                    .text("Resting for ")
                                    .value(this.restMinutes.getInt() + " min")
                            : ModuleChat.body().text("Rest over, sending again."));
        }
        return bl;
    }

    private void fight(MinecraftClient client, ClientPlayerEntity player2) {
        if (this.attack.get() && !this.eating && canFight(client, player2)) {
            AbstractClientPlayerEntity player3 = null;
            for (AutoTpaHereModule.Near near : this.nearList) {
                if (near.distance() > this.attackRadius.get()) {
                    break;
                }
                if (MeleeAttacks.isEnemy(player2, near.player())) {
                    player3 = near.player();
                    break;
                }
            }
            if (player3 != null) {
                if (!this.equipSword(player2)) {
                    Vec3d eyePos = player2.getEyePos();
                    Box boundingBox = player3.getBoundingBox();
                    Rotation rotation = RotationUtils.fromTo(eyePos, boundingBox.getCenter());
                    if (MeleeAttacks.charge(player2) < 1.0F) {
                        if (MeleeAttacks.ticksToFull(player2) <= 1) {
                            ServerRotation.request(rotation);
                        }
                    } else {
                        Rotation rotation2 = ServerRotation.sent();
                        if (rotation2 == null) {
                            rotation2 = RotationUtils.entityRotation(player2);
                        }
                        Vec3d vec;
                        if ((vec = hitPoint(player2, eyePos, boundingBox, rotation2)) == null) {
                            ServerRotation.request(rotation);
                        } else {
                            if (MeleeAttacks.attack(client, new EntityHitResult(player3, vec))) {
                                MeleeAttacks.claimTick(client);
                                this.totalHits++;
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean canFight(MinecraftClient client, ClientPlayerEntity player2) {
        return client.world != null
                && client.interactionManager != null
                && !player2.isSpectator()
                && player2.currentScreenHandler == player2.playerScreenHandler
                && !AutoEatModule.isEating();
    }

    private static Vec3d hitPoint(
            ClientPlayerEntity player2, Vec3d vec, Box box, Rotation rotation) {
        double entityInteractionRange = player2.getEntityInteractionRange();
        Vec3d vec2 = Vec3d.fromPolar(rotation.pitch(), rotation.yaw());
        Vec3d vec3 =
                box.raycast(vec, vec.add(vec2.multiply(entityInteractionRange)))
                        .orElse(box.contains(vec) ? vec : null);
        return vec3 != null
                        && vec.distanceTo(vec3) <= entityInteractionRange
                        && MeleeAttacks.lineIsClear(player2, vec, vec3)
                ? vec3
                : null;
    }

    private boolean equipSword(ClientPlayerEntity player2) {
        PlayerInventory inventory = player2.getInventory();
        int sword = this.findSword(inventory);
        if (sword < 0) {
            if (!this.warnedNoSword) {
                this.warnedNoSword = true;
                this.info(
                        ModuleChat.body()
                                .warn("No sword in the hotbar")
                                .text(", attacking with the held item."));
            }
            return false;
        } else if (inventory.getSelectedSlot() == sword) {
            return false;
        } else {
            inventory.setSelectedSlot(sword);
            return true;
        }
    }

    private int findSword(PlayerInventory playerInventory) {
        if (this.swordSlot.getInt() >= 1) {
            return this.swordSlot.getInt() - 1;
        } else {
            int i = -1;
            int j = 2147483647;
            for (int k = 0; k < PlayerInventory.getHotbarSize(); k++) {
                String itemPath = itemPath(playerInventory.getStack(k));
                if (itemPath.endsWith("_sword")) {
                    int m = SWORD_RANK.length;
                    for (int n = 0; n < SWORD_RANK.length; n++) {
                        if (itemPath.startsWith(SWORD_RANK[n])) {
                            m = n;
                            break;
                        }
                    }
                    if (m < j) {
                        j = m;
                        i = k;
                    }
                }
            }
            return i;
        }
    }

    private int findApple(PlayerInventory playerInventory) {
        int i = this.appleSlot.getInt() - 1;
        ItemStack stack;
        if (i < 0
                || i >= PlayerInventory.getHotbarSize()
                || !(stack = playerInventory.getStack(i)).isOf(Items.GOLDEN_APPLE)
                        && !stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
            int j = -1;
            int k = -1;
            for (int m = 0; m < PlayerInventory.getHotbarSize(); m++) {
                ItemStack stack2 = playerInventory.getStack(m);
                if (stack2.isOf(Items.ENCHANTED_GOLDEN_APPLE) && k < 0) {
                    k = m;
                } else if (stack2.isOf(Items.GOLDEN_APPLE) && j < 0) {
                    j = m;
                }
            }
            if (this.enchantedFirst.get()) {
                return k >= 0 ? k : j;
            } else {
                return j >= 0 ? j : k;
            }
        } else {
            return i;
        }
    }

    private static String itemPath(ItemStack stack) {
        return stack.isEmpty() ? "" : Registries.ITEM.getId(stack.getItem()).getPath();
    }

    private boolean wantsToEat(ClientPlayerEntity player2, long l) {
        float health = player2.getHealth();
        return this.eatBelow.get() > 0.0F
                && health > 0.0F
                && health < this.eatBelow.get() * 2.0F
                && l - this.lastEatAt >= (long) (this.eatCooldown.get() * 1000.0F)
                && this.findApple(player2.getInventory()) >= 0;
    }

    private boolean keepForBot(int i, ItemStack stack) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        return this.attack.get() && player2 != null && i == this.findSword(player2.getInventory())
                ? true
                : this.eatBelow.get() > 0.0F
                        && (stack.isOf(Items.GOLDEN_APPLE)
                                || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE));
    }

    private void onSellCommand() {
        this.lastSendAt = System.currentTimeMillis();
        this.lastTargetCounted = true;
    }

    private boolean startEating(
            MinecraftClient client, ClientPlayerEntity player2, long lastEatAt2) {
        if (!this.eating && !(this.eatBelow.get() <= 0.0F) && client.currentScreen == null) {
            float health = player2.getHealth();
            if (!(health <= 0.0F)
                    && !(health >= this.eatBelow.get() * 2.0F)
                    && lastEatAt2 - this.lastEatAt >= (long) (this.eatCooldown.get() * 1000.0F)) {
                this.lastEatAt = lastEatAt2;
                int apple = this.findApple(player2.getInventory());
                if (apple < 0) {
                    if (!this.warnedNoApple) {
                        this.warnedNoApple = true;
                        this.info(
                                ModuleChat.body()
                                        .warn("Low health but no golden apple in the hotbar."));
                    }
                    return false;
                } else {
                    player2.getInventory().setSelectedSlot(apple);
                    this.eating = true;
                    this.eatTicks = 0;
                    this.eatStarted = false;
                    this.eatStartHealth = health;
                    client.options.useKey.setPressed(true);
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean tickEating(
            MinecraftClient client, ClientPlayerEntity player2, long lastEatAt2) {
        if (!this.eating) {
            return false;
        } else {
            this.eatTicks++;
            if (player2.isUsingItem()) {
                this.eatStarted = true;
                if (this.eatTicks < 40) {
                    client.options.useKey.setPressed(true);
                    return true;
                }
            } else if (!this.eatStarted && this.eatTicks < 40) {
                client.options.useKey.setPressed(true);
                return true;
            }
            this.stopEating(client);
            this.lastEatAt = lastEatAt2;
            if (this.eatStarted) {
                this.totalEaten++;
                this.info(
                        ModuleChat.body()
                                .text("Ate a golden apple (")
                                .value(
                                        String.format(
                                                Locale.ROOT, "%.1f", this.eatStartHealth / 2.0F))
                                .text(" -> ")
                                .value(
                                        String.format(
                                                Locale.ROOT, "%.1f", player2.getHealth() / 2.0F))
                                .text(" hearts)."));
            }
            if (this.attack.get()) {
                this.equipSword(player2);
            }
            return false;
        }
    }

    private void stopEating(MinecraftClient client) {
        if (this.eating) {
            this.eating = false;
            client.options.useKey.setPressed(
                    Crystals.isPhysicallyDown(client, client.options.useKey));
        }
    }

    private List<String> letterList() {
        List letters2 = parseLetters(this.letters.get());
        if (this.order.is("Z to A")) {
            Collections.reverse(letters2);
        }
        if (!this.numbers.get()) {
            return letters2;
        } else {
            ArrayList list = new ArrayList();
            for (int i = 0; i < "0123456789_".length(); i++) {
                list.add(String.valueOf("0123456789_".charAt(i)));
            }
            list.addAll(letters2);
            return list;
        }
    }

    static List<String> parseLetters(String string) {
        String string2 =
                string == null ? "" : string.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        LinkedHashSet set = new LinkedHashSet();
        for (String string3 : string2.split(",")) {
            if (string3.length() == 3
                    && string3.charAt(1) == '-'
                    && isAz(string3.charAt(0))
                    && isAz(string3.charAt(2))) {
                char c = string3.charAt(0);
                char c2;
                int i = c <= (c2 = string3.charAt(2)) ? 1 : -1;
                for (char c3 = c; c3 != c2 + i; c3 = (char) (c3 + i)) {
                    set.add(String.valueOf(c3));
                }
            } else {
                for (int j = 0; j < string3.length(); j++) {
                    if (isAz(string3.charAt(j))) {
                        set.add(String.valueOf(string3.charAt(j)));
                    }
                }
            }
        }
        if (set.isEmpty()) {
            for (char c4 = 'a'; c4 <= 'z'; c4++) {
                set.add(String.valueOf(c4));
            }
        }
        return new ArrayList<>(set);
    }

    private static boolean isAz(char c) {
        return c >= 'a' && c <= 'z';
    }

    private String firstLetter() {
        List list = this.letterList();
        String string = this.startLetter.get().trim().toLowerCase(Locale.ROOT);
        String string2;
        return !string.isEmpty() && list.contains(string2 = string.substring(0, 1))
                ? string2
                : (String) list.get(0);
    }

    private static List<String> subPrefixes(String string) {
        ArrayList list = new ArrayList("abcdefghijklmnopqrstuvwxyz0123456789_".length());
        for (int i = 0; i < "abcdefghijklmnopqrstuvwxyz0123456789_".length(); i++) {
            list.add(string + "abcdefghijklmnopqrstuvwxyz0123456789_".charAt(i));
        }
        return list;
    }

    private String partialFor(String string) {
        return this.command.get() + " " + (this.bedrock.get() ? "." : "") + string;
    }

    private List<String> namesForPrefix(List<String> list, String string) {
        String string2 = stripDots(string).toLowerCase(Locale.ROOT);
        ArrayList list2 = new ArrayList();
        for (String string3 : list) {
            String cleanName = cleanName(this.stripCommand(string3));
            if (validName(cleanName)
                    && stripDots(cleanName).toLowerCase(Locale.ROOT).startsWith(string2)) {
                list2.add(cleanName);
            }
        }
        return list2;
    }

    private boolean matchesLetter(String string, String string2) {
        String string3 = this.bedrock.get() ? stripDots(string) : string;
        return this.bedrock.get() && string3.length() == string.length()
                ? false
                : !string3.isEmpty()
                        && string3.toLowerCase(Locale.ROOT)
                                .startsWith(string2.toLowerCase(Locale.ROOT));
    }

    private String stripCommand(String string) {
        String string2 = string == null ? "" : string.trim();
        String string3 = string2.toLowerCase(Locale.ROOT);
        String command2 = this.command.get();
        for (String string4 :
                new String[] {
                    "/" + command2 + " ", command2 + " ", "/tpahere ", "tpahere ", "/tpa ", "tpa "
                }) {
            if (string3.startsWith(string4)) {
                string2 = string2.substring(string4.length()).trim();
                break;
            }
        }
        int i;
        if ((i = string2.indexOf(32)) > 0) {
            string2 = string2.substring(0, i);
        }
        return string2.replaceAll("^[\"']|[\"']$", "");
    }

    private static String cleanName(String string) {
        return FORMATTING.matcher(string == null ? "" : string).replaceAll("").trim();
    }

    private static boolean validName(String string) {
        return string != null
                && VALID_NAME.matcher(string).matches()
                && !stripDots(string).isEmpty();
    }

    private static boolean isLetter(String string) {
        return !string.isEmpty() && isAz(string.charAt(0));
    }

    private static String stripDots(String string) {
        int i = 0;
        while (i < string.length() && string.charAt(i) == '.') {
            i++;
        }
        return string.substring(i);
    }

    private static String normalizeIgn(String string) {
        return string == null ? "" : stripDots(cleanName(string)).toLowerCase(Locale.ROOT);
    }

    private static String key(String string) {
        return string == null ? "" : string.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean sameIgn(String string, String string2) {
        String normalizeIgn = normalizeIgn(string);
        return !normalizeIgn.isEmpty() && normalizeIgn.equals(normalizeIgn(string2));
    }

    private boolean isSelf(String string) {
        return !this.selfName.isEmpty() && this.selfName.equals(normalizeIgn(string));
    }

    private static boolean isFriend(String string) {
        FriendStore friendStore = FriendStore.getInstance();
        return friendStore.isFriend(string)
                || friendStore.isFriend(stripDots(string))
                || friendStore.isFriend("." + stripDots(string));
    }

    private boolean blacklisted(String string) {
        String normalizeIgn = normalizeIgn(string);
        if (normalizeIgn.isEmpty()) {
            return false;
        } else {
            for (String string2 : this.neverRequest.get()) {
                if (normalizeIgn.equals(normalizeIgn(string2))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean excluded(String string) {
        return string == null
                || this.isSelf(string)
                || isFriend(string)
                || this.blacklisted(string);
    }

    private long delayMs() {
        return (long) (this.delay.get() * 1000.0F);
    }

    private String orderLabel() {
        List letters2 = parseLetters(this.letters.get());
        String string = this.numbers.get() ? "0-9_ " : "";
        if (this.order.is("Random")) {
            return "random "
                    + string
                    + (letters2.size() < 26
                            ? String.join("", letters2).toUpperCase(Locale.ROOT)
                            : "A-Z");
        } else if (letters2.size() < 26) {
            if (this.order.is("Z to A")) {
                Collections.reverse(letters2);
            }
            return string + String.join("", letters2).toUpperCase(Locale.ROOT);
        } else {
            return string + (this.order.is("Z to A") ? "Z to A" : "A to Z");
        }
    }

    private static String formatSeconds(long l) {
        return l % 1000L == 0L ? l / 1000L + "s" : String.format(Locale.ROOT, "%.2fs", l / 1000.0);
    }

    private String statsText() {
        StringBuilder sb =
                new StringBuilder("Sent ")
                        .append(this.totalSent)
                        .append(", offline ")
                        .append(this.totalOffline)
                        .append(", disabled ")
                        .append(this.totalDisabled);
        if (this.totalJoiners > 0) {
            sb.append(", new players ").append(this.totalJoiners);
        }
        if (this.totalArrivals > 0) {
            sb.append(", arrivals ").append(this.totalArrivals);
        }
        if (this.totalHits > 0) {
            sb.append(", hits ").append(this.totalHits);
        }
        if (this.totalEaten > 0) {
            sb.append(", apples ").append(this.totalEaten);
        }
        if (this.totalDeaths > 0) {
            sb.append(", deaths ").append(this.totalDeaths);
        }
        return sb.append('.').toString();
    }

    private void info(ModuleChat.Body body) {
        ModuleChat.send(this, body);
    }

    @Environment(EnvType.CLIENT)
    private static final class Bucket {

        List<String> names = List.of();

        List<String> unused = new ArrayList<>();

        boolean scanned;

        long at;

        private Bucket() {}
    }

    @Environment(EnvType.CLIENT)
    private final class Collector {

        final String letter;

        boolean split;

        boolean timedOut;

        List<String> names = List.of();

        private boolean started;

        private int mainId = -1;

        private long mainUntil;

        private final List<String> raw = new ArrayList<>();

        private List<String> subs;

        private int subIndex;

        private final List<AutoTpaHereModule.Inflight> inflight = new ArrayList<>();

        Collector(String letter2) {
            this.letter = letter2;
        }

        boolean step(ClientPlayNetworkHandler clientPlayNetworkHandler, long l) {
            if (!this.started) {
                this.started = true;
                this.split = AutoTpaHereModule.this.splitLetters.contains(this.letter);
                if (!this.split) {
                    this.mainId =
                            AutoTpaHereModule.this.completions.fire(
                                    AutoTpaHereModule.this.partialFor(this.letter));
                    this.mainUntil =
                            l
                                    + (AutoTpaHereModule.this
                                                    .localNames(
                                                            clientPlayNetworkHandler, this.letter)
                                                    .isEmpty()
                                            ? 5000L
                                            : 3000L);
                    if (this.mainId < 0) {
                        this.timedOut = true;
                    }
                }
            }
            if (this.mainId >= 0) {
                List completion = AutoTpaHereModule.this.completions.poll(this.mainId);
                if (completion == null && l < this.mainUntil) {
                    return false;
                }
                AutoTpaHereModule.this.completions.forget(this.mainId);
                this.mainId = -1;
                if (completion == null) {
                    this.timedOut = true;
                } else {
                    this.raw.addAll(AutoTpaHereModule.this.namesForPrefix(completion, this.letter));
                    if (completion.size() >= 990) {
                        this.split = true;
                        AutoTpaHereModule.this.splitLetters.add(this.letter);
                    }
                }
            }
            if (this.split) {
                if (this.subs == null) {
                    this.subs = AutoTpaHereModule.subPrefixes(this.letter);
                }
                if (!this.stepSplit(l)) {
                    return false;
                }
                this.timedOut = this.timedOut && this.raw.isEmpty();
            }
            this.raw.addAll(
                    AutoTpaHereModule.this.localNames(clientPlayNetworkHandler, this.letter));
            HashSet set = new HashSet();
            ArrayList list = new ArrayList();
            for (String string : this.raw) {
                String cleanName =
                        AutoTpaHereModule.cleanName(AutoTpaHereModule.this.stripCommand(string));
                if (AutoTpaHereModule.validName(cleanName)
                        && AutoTpaHereModule.this.matchesLetter(cleanName, this.letter)
                        && set.add(AutoTpaHereModule.key(cleanName))) {
                    list.add(cleanName);
                }
            }
            this.names = list;
            return true;
        }

        private boolean stepSplit(long l) {
            while (this.subIndex < this.subs.size() && this.inflight.size() < 6) {
                String sub;
                int fire;
                if ((fire =
                                AutoTpaHereModule.this.completions.fire(
                                        AutoTpaHereModule.this.partialFor(
                                                sub = this.subs.get(this.subIndex++))))
                        >= 0) {
                    this.inflight.add(new AutoTpaHereModule.Inflight(fire, sub, l));
                }
            }
            this.inflight.removeIf(
                    inflight -> {
                        List completion = AutoTpaHereModule.this.completions.poll(inflight.id());
                        if (completion != null) {
                            this.raw.addAll(
                                    AutoTpaHereModule.this.namesForPrefix(
                                            completion, inflight.prefix()));
                        } else if (l - inflight.at() <= 4000L) {
                            return false;
                        }
                        AutoTpaHereModule.this.completions.forget(inflight.id());
                        return true;
                    });
            return this.subIndex >= this.subs.size() && this.inflight.isEmpty();
        }
    }

    @Environment(EnvType.CLIENT)
    private final class Delivery {

        final String name;

        final boolean joiner;

        boolean stale;

        int probeId = -1;

        long probeAt;

        boolean finalWait;

        long finalUntil;

        Delivery(String name2, boolean joiner2) {
            this.name = name2;
            this.joiner = joiner2;
        }

        AutoTpaHereModule.Result step(
                ClientPlayNetworkHandler clientPlayNetworkHandler, long probeAt2) {
            if (this.stale) {
                this.stale = false;
                AutoTpaHereModule.this.completions.forget(this.probeId);
                this.probeId = -1;
                this.finalWait = false;
            }
            if (AutoTpaHereModule.this.excluded(this.name)) {
                return AutoTpaHereModule.Result.SKIPPED;
            } else {
                if (AutoTpaHereModule.this.skipOffline.get()
                        && !AutoTpaHereModule.this.onlineCheckOff
                        && this.probeId < 0
                        && !this.finalWait) {
                    this.probeId =
                            AutoTpaHereModule.this.completions.fire(
                                    AutoTpaHereModule.this.command.get() + " " + this.name);
                    this.probeAt = probeAt2;
                }
                if (probeAt2
                        < AutoTpaHereModule.this.lastSendAt + AutoTpaHereModule.this.delayMs()) {
                    if (this.probeId >= 0
                            && Boolean.FALSE.equals(
                                    AutoTpaHereModule.this.probeAnswer(
                                            AutoTpaHereModule.this.completions.poll(this.probeId),
                                            this.name))) {
                        AutoTpaHereModule.this.completions.forget(this.probeId);
                        AutoTpaHereModule.this.totalOffline++;
                        AutoTpaHereModule.this.noteProbe(false);
                        return AutoTpaHereModule.Result.SKIPPED;
                    } else {
                        return AutoTpaHereModule.Result.WAIT;
                    }
                } else {
                    if (this.probeId >= 0) {
                        if (!this.finalWait) {
                            this.finalWait = true;
                            if (probeAt2 - this.probeAt > 4000L) {
                                AutoTpaHereModule.this.completions.forget(this.probeId);
                                this.probeId =
                                        AutoTpaHereModule.this.completions.fire(
                                                AutoTpaHereModule.this.command.get()
                                                        + " "
                                                        + this.name);
                                this.probeAt = probeAt2;
                                this.finalUntil = probeAt2 + 700L;
                            } else {
                                this.finalUntil =
                                        probeAt2 + Math.max(100L, 700L - (probeAt2 - this.probeAt));
                            }
                        }
                        List completion;
                        if ((completion = AutoTpaHereModule.this.completions.poll(this.probeId))
                                        == null
                                && probeAt2 < this.finalUntil) {
                            return AutoTpaHereModule.Result.WAIT;
                        }
                        AutoTpaHereModule.this.completions.forget(this.probeId);
                        this.probeId = -1;
                        Boolean flag = AutoTpaHereModule.this.probeAnswer(completion, this.name);
                        AutoTpaHereModule.this.noteProbe(flag);
                        if (Boolean.FALSE.equals(flag)) {
                            AutoTpaHereModule.this.totalOffline++;
                            return AutoTpaHereModule.Result.SKIPPED;
                        }
                    }
                    AutoTpaHereModule.this.send(
                            clientPlayNetworkHandler, this.name, this.joiner, probeAt2);
                    return AutoTpaHereModule.Result.SENT;
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private record Inflight(int id, String prefix, long at) {}

    @Environment(EnvType.CLIENT)
    private record Near(AbstractClientPlayerEntity player, String name, double distance) {}

    @Environment(EnvType.CLIENT)
    private static enum Phase {
        DETECT,
        COLLECT,
        SEND,
        PICK;

        private Phase() {}
    }

    @Environment(EnvType.CLIENT)
    private static enum Result {
        WAIT,
        SENT,
        SKIPPED;

        private Result() {}
    }
}
