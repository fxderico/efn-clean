package net.efn.client.module.impl.misc;

import com.mojang.authlib.GameProfile;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.StaffGhost;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.util.NameFilter;
import net.efn.client.util.staff.ShardTracker;
import net.efn.client.util.staff.StaffAlarm;
import net.efn.client.util.staff.StaffMatcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class StaffListModule extends ClientModule {

    public static final List<String> DONUT_STAFF =
            List.of(
                    "FluffyMaster07",
                    "archivePedro",
                    "Munkerlich",
                    "Frenk_Btw",
                    "Napooo_",
                    "auzzitech",
                    "CryptoDaveYT",
                    "W1zox_",
                    "ZEEF69",
                    "showered",
                    "CaptainMoose35",
                    "BobIsFound",
                    "NoahvdAa",
                    "0GSummer",
                    "LzouzMP5",
                    "Pastagamer08",
                    "u_vv",
                    "Owen1212055",
                    "Splaterd",
                    "Fallerfly",
                    "Bloodspulse",
                    "_chaon",
                    "Dough4",
                    "evify");

    private static final Set<String> STAFF_KEYS = staffKeys();

    private static final StaffMatcher RANKS =
            new StaffMatcher(
                    "Ranks + Markers + Team",
                    "owner, co-owner, manager, admin, administrator, developer, dev, sr-admin,"
                            + " sr-mod, senior-mod, moderator, mod, trial-mod, sr-helper,"
                            + " senior-helper, helper, trial-helper, support, staff",
                    "U+2605 U+2606 U+2726 U+2727 U+272A U+272F U+2730 U+2B50",
                    false,
                    5,
                    String.join(",", DONUT_STAFF),
                    "");

    private static final int SCAN_INTERVAL_TICKS = 10;

    private static final int SETTLE_TICKS = 60;

    private static final int VANISH_CONFIRM_SCANS = 2;

    private static final int GROUP_THRESHOLD = 3;

    private static final int ALERT_GATHER_TICKS = 5;

    private static final int SWITCH_ECHO_TICKS = 20;

    public static final long ALERT_HIGHLIGHT_MS = 4000L;

    private static final long ALERT_COOLDOWN_MS = 120000L;

    private static final long SERVER_MARK_MS = 600000L;

    private static final int MAX_ROWS = 8;

    private static final float ALARM_SECONDS = 3.0F;

    private static final long SHARED_ALARM_MS = 5000L;

    private static final long NEARBY_GRACE_MS = 10000L;

    private static final long SIGHTING_GRACE_MS = 10000L;

    private static final long WATCH_GRACE_MS = 10000L;

    private static final Pattern ACCOUNT_NAME = Pattern.compile("[.*]?[A-Za-z0-9_]{1,16}");

    private final BooleanSetting regionAlert =
            this.addSetting("server-alert", new BooleanSetting("Region Alert", true));

    private final Map<UUID, StaffListModule.Staff> staff = new LinkedHashMap<>();

    private List<StaffListModule.Staff> sorted = List.of();

    private boolean resort;

    private final Map<UUID, StaffListModule.Classified> classified = new HashMap<>();

    private final Map<UUID, StaffListModule.Sighting> sightings = new HashMap<>();

    private final Queue<StaffListModule.GameModeSwitch> switches = new ConcurrentLinkedQueue<>();

    private final Map<UUID, String> pendingAlerts = new LinkedHashMap<>();

    private final StaffAlarm alarm = new StaffAlarm();

    private ClientPlayNetworkHandler connection;

    private final ShardTracker shard = new ShardTracker();

    private int ticksToScan;

    private int settleTicks;

    private boolean primed;

    private int alertGather;

    private int tickCount;

    public StaffListModule() {
        super("staff-list", "Staff List", "Lists online DonutSMP staff.", ModuleCategory.DONUT);
        this.usesNotifications();
        this.regionAlert.onChange(
                () -> {
                    if (!this.regionAlert.get()) {
                        this.forgetShard();
                    }
                });
    }

    @Override
    protected void onEnable() {
        this.forget();
        MinecraftClient client = MinecraftClient.getInstance();
        this.connection = client.getNetworkHandler();
        this.shard.clear();
        this.settleTicks = 0;
        this.ticksToScan = 0;
        this.primed = false;
        this.on(TickEvent.TYPE, Stage.POST, this::onTick);
        this.on(PacketReceiveNettyEvent.TYPE, this::onPacketNetty);
    }

    @Override
    protected void onDisable() {
        this.forget();
        this.connection = null;
        this.shard.clear();
    }

    private void forget() {
        this.staff.clear();
        this.sorted = List.of();
        this.classified.clear();
        this.forgetShard();
    }

    private void forgetShard() {
        this.switches.clear();
        this.sightings.clear();
        this.pendingAlerts.clear();
        this.alarm.stop();
        this.resort = true;
    }

    public static boolean shouldShowHud() {
        StaffListModule module = instance();
        return module != null && module.isEnabled() && !module.sorted.isEmpty();
    }

    public static StaffListModule.HudList hudRows() {
        StaffListModule module = instance();
        if (module != null && module.isEnabled()) {
            List sorted2 = module.sorted;
            int i = sorted2.size() <= 8 ? sorted2.size() : 7;
            long l = System.currentTimeMillis();
            ArrayList list = new ArrayList(i);
            for (int j = 0; j < i; j++) {
                StaffListModule.Staff staff2 = (StaffListModule.Staff) sorted2.get(j);
                StaffListModule.Sighting sighting = module.sightings.get(staff2.id);
                boolean bl = sighting != null && sighting.watching(l);
                boolean bl2 = sighting != null && sighting.nearby;
                float f =
                        sighting == null
                                ? 0.0F
                                : Math.max(
                                        0.0F,
                                        Math.min(
                                                1.0F,
                                                (float) (sighting.highlightUntil - l) / 4000.0F));
                String string = NameFilter.apply(staff2.name);
                list.add(
                        new StaffListModule.HudRow(
                                string,
                                staff2.id,
                                staff2.presence,
                                bl,
                                bl ? Math.max(0L, l - sighting.watchingSince) : -1L,
                                bl2,
                                bl2 ? sighting.distance : -1.0F,
                                f,
                                !string.equals(staff2.name)));
            }
            return new StaffListModule.HudList(list, sorted2.size() - i);
        } else {
            return StaffListModule.HudList.EMPTY;
        }
    }

    public static String formatAge(long l) {
        long l2 = Math.max(0L, l) / 1000L;
        long l3 = l2 / 3600L;
        long l4 = l2 / 60L % 60L;
        long l5 = l2 % 60L;
        StringBuilder sb = new StringBuilder(8);
        if (l3 > 0L) {
            sb.append(l3).append(l4 < 10L ? ":0" : ":");
        }
        return sb.append(l4).append(l5 < 10L ? ":0" : ":").append(l5).toString();
    }

    public static boolean isStaffName(String string) {
        return string != null && STAFF_KEYS.contains(string.toLowerCase(Locale.ROOT));
    }

    public static boolean isInYourRegion(UUID uuid) {
        StaffListModule module = instance();
        if (module != null && module.isEnabled() && uuid != null) {
            StaffListModule.Sighting sighting = module.sightings.get(uuid);
            return sighting != null && sighting.marked(System.currentTimeMillis());
        } else {
            return false;
        }
    }

    public static void provenHere(UUID uuid, String string) {
        StaffListModule module = instance();
        if (module != null
                && module.isEnabled()
                && module.regionAlert.get()
                && uuid != null
                && string != null) {
            long l = System.currentTimeMillis();
            StaffListModule.Sighting sighting =
                    module.sightings.computeIfAbsent(uuid, uUIDx -> new StaffListModule.Sighting());
            if (!sighting.marked(l)) {
                module.resort = true;
            }
            sighting.prove(l);
            if (!sighting.coolingDown(l)) {
                module.alert(uuid, sighting, l, string);
            }
        }
    }

    public static boolean alarmedRecently(UUID uuid) {
        StaffListModule module = instance();
        if (module != null && module.isEnabled() && uuid != null) {
            StaffListModule.Sighting sighting = module.sightings.get(uuid);
            return sighting != null
                    && sighting.alertedAt != 0L
                    && System.currentTimeMillis() - sighting.alertedAt < 5000L;
        } else {
            return false;
        }
    }

    private static Set<String> staffKeys() {
        HashSet set = new HashSet();
        for (String string : DONUT_STAFF) {
            set.add(string.toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(set);
    }

    private static StaffListModule instance() {
        return ModuleRegistry.get(StaffListModule.class);
    }

    private void onPacketNetty(PacketReceiveNettyEvent event) {
        if (this.regionAlert.get()) {
            if (event.packet instanceof PlayerListS2CPacket playerListPacket) {
                this.queueSwitches(playerListPacket);
            } else if (event.packet instanceof BundleS2CPacket bundlePacket) {
                for (Packet packet : bundlePacket.getPackets()) {
                    if (packet instanceof PlayerListS2CPacket playerListPacket2) {
                        this.queueSwitches(playerListPacket2);
                    }
                }
            }
        }
    }

    private void queueSwitches(PlayerListS2CPacket playerListPacket) {
        EnumSet actions = playerListPacket.getActions();
        if (actions.contains(Action.UPDATE_GAME_MODE) && !actions.contains(Action.ADD_PLAYER)) {
            for (Entry entry : playerListPacket.getEntries()) {
                if (entry != null && entry.profileId() != null) {
                    this.switches.offer(
                            new StaffListModule.GameModeSwitch(
                                    entry.profileId(), entry.gameMode()));
                }
            }
        }
    }

    private void onTick(TickEvent event) {
        this.alarm.tick();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler != null && client.player != null && client.world != null) {
            boolean moved = this.shard.moved(client);
            if (networkHandler != this.connection) {
                this.connection = networkHandler;
                this.forget();
                this.settleTicks = 60;
                this.primed = false;
            } else if (moved) {
                this.forgetShard();
            }
            if (this.settleTicks > 0) {
                this.settleTicks--;
            }
            this.tickCount++;
            long l = System.currentTimeMillis();
            if (this.regionAlert.get()) {
                this.drainSwitches(networkHandler, client.player.getUuid(), l);
                this.trackBodies(client, l);
            }
            if (--this.ticksToScan <= 0) {
                this.ticksToScan = 10;
                StaffListModule.Changes changes = this.scan(client, networkHandler, l);
                if (this.primed) {
                    this.announce(client, changes);
                } else if (this.settleTicks == 0) {
                    this.primed = true;
                }
            }
            if (!this.pendingAlerts.isEmpty() && --this.alertGather <= 0) {
                this.raise();
            }
            if (this.resort) {
                this.sortView(l);
            }
        } else {
            if (this.connection != null
                    || !this.staff.isEmpty()
                    || !this.classified.isEmpty()
                    || !this.sightings.isEmpty()) {
                this.forget();
            }
            this.switches.clear();
            this.connection = null;
            this.shard.clear();
            this.primed = false;
        }
    }

    private StaffListModule.Changes scan(
            MinecraftClient client, ClientPlayNetworkHandler clientPlayNetworkHandler, long l) {
        UUID uuid = client.player.getUuid();
        HashSet set = new HashSet();
        for (PlayerListEntry listedPlayerListEntry :
                clientPlayNetworkHandler.getListedPlayerListEntries()) {
            set.add(listedPlayerListEntry.getProfile().id());
        }
        StaffListModule.Changes changes = new StaffListModule.Changes();
        HashSet set2 = new HashSet();
        for (PlayerListEntry player : clientPlayNetworkHandler.getPlayerList()) {
            GameProfile profile = player.getProfile();
            UUID uuid2 = profile.id();
            String string = profile.name();
            if (uuid2 != null
                    && !uuid2.equals(uuid)
                    && isStaffName(string)
                    && isAccount(uuid2, string)) {
                set2.add(uuid2);
                StaffMatcher.Match match2 = this.rank(player, uuid2, string);
                StaffListModule.Presence presence2 =
                        player.getGameMode() == GameMode.SPECTATOR
                                ? StaffListModule.Presence.SPECTATOR
                                : (set.contains(uuid2)
                                        ? StaffListModule.Presence.ONLINE
                                        : StaffListModule.Presence.VANISHED);
                StaffListModule.Staff staff2 = this.staff.get(uuid2);
                if (staff2 == null) {
                    staff2 = new StaffListModule.Staff(uuid2);
                    this.staff.put(uuid2, staff2);
                    staff2.update(string, match2, presence2);
                    changes.joined.add(staff2);
                } else {
                    StaffListModule.Presence presence3 = staff2.presence;
                    staff2.update(string, match2, presence2);
                    staff2.offTabScans = 0;
                    if (presence3 != presence2) {
                        changes.changed.add(staff2);
                    }
                }
            }
        }
        this.classified.keySet().retainAll(set2);
        Iterator iterator = this.staff.values().iterator();
        while (iterator.hasNext()) {
            StaffListModule.Staff staff3 = (StaffListModule.Staff) iterator.next();
            if (!set2.contains(staff3.id)) {
                if (client.world.getPlayerByUuid(staff3.id) != null) {
                    if (++staff3.offTabScans >= 2
                            && staff3.presence != StaffListModule.Presence.VANISHED) {
                        staff3.presence = StaffListModule.Presence.VANISHED;
                        changes.changed.add(staff3);
                    }
                } else {
                    iterator.remove();
                    changes.left.add(staff3);
                }
            }
        }
        this.pruneSightings(l);
        this.sortView(l);
        return changes;
    }

    private void sortView(long l) {
        ArrayList list = new ArrayList<>(this.staff.values());
        list.sort(
                Comparator.<StaffListModule.Staff>comparingInt(staff -> this.group(staff, l))
                        .thenComparingInt(staff -> staff.seniority)
                        .thenComparing(staff -> staff.name, String.CASE_INSENSITIVE_ORDER));
        this.sorted = List.copyOf(list);
        this.resort = false;
    }

    private int group(StaffListModule.Staff staff2, long l) {
        StaffListModule.Sighting sighting = this.sightings.get(staff2.id);
        if (sighting != null && sighting.nearby) {
            return 0;
        } else if (sighting != null && sighting.marked(l)) {
            return 1;
        } else {
            return staff2.presence != StaffListModule.Presence.ONLINE ? 2 : 3;
        }
    }

    private static boolean isAccount(UUID uuid, String string) {
        return string != null && uuid.version() != 2 && ACCOUNT_NAME.matcher(string).matches();
    }

    private StaffMatcher.Match rank(PlayerListEntry listEntry, UUID uuid, String string) {
        Team scoreboardTeam = listEntry.getScoreboardTeam();
        Text displayName = listEntry.getDisplayName();
        Text text = scoreboardTeam == null ? null : scoreboardTeam.getPrefix();
        Text text2 = scoreboardTeam == null ? null : scoreboardTeam.getSuffix();
        StaffListModule.Classified classified2 = this.classified.get(uuid);
        if (classified2 != null
                && classified2.isFor(string, displayName, scoreboardTeam, text, text2)) {
            return classified2.match();
        } else {
            StaffMatcher.Match match2 =
                    RANKS.match(
                            string,
                            scoreboardTeam == null ? null : scoreboardTeam.getName(),
                            plain(displayName),
                            plain(text),
                            plain(text2));
            this.classified.put(
                    uuid,
                    new StaffListModule.Classified(
                            string, displayName, scoreboardTeam, text, text2, match2));
            return match2;
        }
    }

    private static String plain(Text text) {
        return text == null ? null : text.getString();
    }

    private void drainSwitches(
            ClientPlayNetworkHandler clientPlayNetworkHandler, UUID uuid, long l) {
        StaffListModule.GameModeSwitch gameModeSwitch;
        while ((gameModeSwitch = this.switches.poll()) != null) {
            UUID uuid2 = gameModeSwitch.id();
            if (!uuid2.equals(uuid)) {
                StaffListModule.Staff staff2 = this.staff.get(uuid2);
                String name2;
                if (staff2 != null) {
                    name2 = staff2.name;
                } else {
                    PlayerListEntry playerListEntry =
                            clientPlayNetworkHandler.getPlayerListEntry(uuid2);
                    name2 = playerListEntry == null ? null : playerListEntry.getProfile().name();
                    if (!isStaffName(name2) || !isAccount(uuid2, name2)) {
                        continue;
                    }
                }
                StaffListModule.Sighting sighting =
                        this.sightings.computeIfAbsent(
                                uuid2, uUIDx -> new StaffListModule.Sighting());
                if (!sighting.marked(l)) {
                    this.resort = true;
                }
                sighting.prove(l);
                if (!sighting.coolingDown(l)) {
                    GameMode gameMode = gameModeSwitch.mode();
                    sighting.switchAlertTick = this.tickCount;
                    sighting.switchedToSpectator = gameMode == GameMode.SPECTATOR;
                    this.alert(uuid2, sighting, l, name2);
                }
            }
        }
    }

    private void trackBodies(MinecraftClient client, long l) {
        ClientPlayerEntity player = client.player;
        for (AbstractClientPlayerEntity player2 : client.world.getPlayers()) {
            StaffListModule.Staff staff2;
            if (player2 != player && (staff2 = this.staff.get(player2.getUuid())) != null) {
                StaffListModule.Sighting sighting =
                        this.sightings.computeIfAbsent(
                                staff2.id, uUID -> new StaffListModule.Sighting());
                sighting.prove(l);
                sighting.bodyTick = this.tickCount;
                sighting.distance = player.distanceTo(player2);
                if (!sighting.nearby) {
                    sighting.nearby = true;
                    this.resort = true;
                    boolean bl = sighting.bodyLostAt == 0L || l - sighting.bodyLostAt > 10000L;
                    if (bl) {
                        this.alert(staff2.id, sighting, l, staff2.name);
                    }
                }
            }
        }
        for (StaffListModule.Sighting sighting2 : this.sightings.values()) {
            if (sighting2.nearby && sighting2.bodyTick != this.tickCount) {
                sighting2.nearby = false;
                sighting2.bodyLostAt = l;
                this.resort = true;
            }
        }
    }

    private void pruneSightings(long l) {
        Iterator iterator = this.sightings.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry entry = (java.util.Map.Entry) iterator.next();
            StaffListModule.Sighting entry2 = (StaffListModule.Sighting) entry.getValue();
            if (this.staff.containsKey(entry.getKey())) {
                entry2.untrackedSince = 0L;
            } else if (entry2.untrackedSince == 0L) {
                entry2.untrackedSince = l;
            } else if (l - entry2.untrackedSince > 10000L) {
                iterator.remove();
                this.resort = true;
            }
        }
    }

    private void alert(UUID uuid, StaffListModule.Sighting sighting, long l, String string) {
        sighting.alertedAt = l;
        sighting.highlightUntil = l + 4000L;
        if (this.pendingAlerts.isEmpty()) {
            this.alertGather = 5;
        }
        this.pendingAlerts.put(uuid, string);
    }

    private void raise() {
        java.util.Map.Entry entry = this.pendingAlerts.entrySet().iterator().next();
        UUID key = (UUID) entry.getKey();
        String entry2 = (String) entry.getValue();
        int i = this.pendingAlerts.size() - 1;
        this.pendingAlerts.clear();
        this.alarm.start("Guardian", 3.0F, 1.0F);
        StaffGhost.watching(key, entry2, i);
    }

    private void announce(MinecraftClient client, StaffListModule.Changes changes) {
        changes.changed.removeIf(this::echoesSwitchAlert);
        if (!changes.isEmpty()) {
            ArrayList list = new ArrayList();
            describe(
                    list,
                    changes.joined,
                    ModuleChat.body().warn("joined").build(),
                    (body, staff) -> {
                        body.warn("joined");
                        String note = note(staff);
                        if (note != null) {
                            body.text(" ").danger(note);
                        }
                    });
            describe(
                    list,
                    changes.changed,
                    ModuleChat.body().warn("changed visibility").build(),
                    (body, staff) -> {
                        switch (staff.presence) {
                            case ONLINE:
                                body.text("is ").good("visible again");
                                break;
                            case SPECTATOR:
                                switchedTo(body, GameMode.SPECTATOR.getId());
                                break;
                            case VANISHED:
                                body.danger("vanished")
                                        .text(
                                                staff.offTabScans > 0
                                                        ? " (off TAB, still nearby)"
                                                        : " (hidden from TAB)");
                        }
                    });
            describe(
                    list,
                    changes.left,
                    ModuleChat.body().good("left").build(),
                    (body, staff) -> {
                        if (staff.presence == StaffListModule.Presence.VANISHED) {
                            body.text("is ").warn("out of sight");
                        } else {
                            body.good("left");
                        }
                    });
            for (ModuleChat.Body body2 : (Iterable<ModuleChat.Body>) list) {
                ModuleChat.send(this, body2);
            }
            boolean bl =
                    !changes.joined.isEmpty()
                            || changes.changed.stream()
                                    .anyMatch(
                                            staff ->
                                                    staff.presence
                                                            != StaffListModule.Presence.ONLINE);
            if (bl && client.getSoundManager() != null) {
                client.getSoundManager()
                        .play(
                                PositionedSoundInstance.ui(
                                        SoundEvents.BLOCK_NOTE_BLOCK_CHIME, 1.25F));
            }
        }
    }

    private boolean echoesSwitchAlert(StaffListModule.Staff staff2) {
        StaffListModule.Sighting sighting = this.sightings.get(staff2.id);
        if (sighting != null
                && sighting.switchAlertTick >= 0
                && this.tickCount - sighting.switchAlertTick <= 20) {
            return staff2.presence == StaffListModule.Presence.SPECTATOR
                    ? sighting.switchedToSpectator
                    : staff2.presence == StaffListModule.Presence.ONLINE
                            && !sighting.switchedToSpectator;
        } else {
            return false;
        }
    }

    private static void describe(
            List<ModuleChat.Body> list,
            List<StaffListModule.Staff> list2,
            Text text,
            BiConsumer<ModuleChat.Body, StaffListModule.Staff> biConsumer) {
        if (!list2.isEmpty()) {
            if (list2.size() > 3) {
                ArrayList list3 = new ArrayList(list2.size());
                for (StaffListModule.Staff staff2 : list2) {
                    list3.add(label(staff2));
                }
                list.add(
                        list(
                                ModuleChat.body()
                                        .value(list2.size())
                                        .text(" staff ")
                                        .append(text)
                                        .text(": "),
                                list3));
            } else {
                for (StaffListModule.Staff staff3 : list2) {
                    ModuleChat.Body body = ModuleChat.body().append(label(staff3)).text(" ");
                    biConsumer.accept(body, staff3);
                    list.add(body);
                }
            }
        }
    }

    private static Text label(StaffListModule.Staff staff2) {
        return label(staff2.name, staff2.rank);
    }

    private static Text label(String string, String string2) {
        return ModuleChat.body().name(NameFilter.apply(string)).text(" (" + string2 + ")").build();
    }

    private static String note(StaffListModule.Staff staff2) {
        return switch (staff2.presence) {
            case ONLINE -> null;
            case SPECTATOR -> "spectating";
            case VANISHED -> "vanished";
        };
    }

    private static ModuleChat.Body list(ModuleChat.Body body, List<Text> list) {
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                body.text(", ");
            }
            body.append((Text) list.get(i));
        }
        return body;
    }

    static ModuleChat.Body switchedTo(ModuleChat.Body body, String string) {
        if (string == null) {
            return body.text("switched game mode");
        } else {
            body.text("switched to ");
            if (GameMode.SPECTATOR.getId().equals(string)) {
                return body.danger(string);
            } else {
                return GameMode.CREATIVE.getId().equals(string)
                        ? body.warn(string)
                        : body.name(string);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Changes {

        final List<StaffListModule.Staff> joined = new ArrayList<>();

        final List<StaffListModule.Staff> changed = new ArrayList<>();

        final List<StaffListModule.Staff> left = new ArrayList<>();

        private Changes() {}

        boolean isEmpty() {
            return this.joined.isEmpty() && this.changed.isEmpty() && this.left.isEmpty();
        }
    }

    @Environment(EnvType.CLIENT)
    private record Classified(
            String name,
            Text display,
            Team team,
            Text prefix,
            Text suffix,
            StaffMatcher.Match match) {

        boolean isFor(String string, Text text, Team team2, Text text2, Text text3) {
            return this.display == text
                    && this.team == team2
                    && this.prefix == text2
                    && this.suffix == text3
                    && this.name.equals(string);
        }
    }

    @Environment(EnvType.CLIENT)
    private record GameModeSwitch(UUID id, GameMode mode) {}

    @Environment(EnvType.CLIENT)
    public record HudList(List<StaffListModule.HudRow> rows, int more) {

        static final StaffListModule.HudList EMPTY = new StaffListModule.HudList(List.of(), 0);
    }

    @Environment(EnvType.CLIENT)
    public record HudRow(
            String name,
            UUID id,
            StaffListModule.Presence presence,
            boolean watching,
            long watchingForMs,
            boolean nearby,
            float distance,
            float alertLevel,
            boolean nameHidden) {

        public String watchingFor() {
            return this.watching ? StaffListModule.formatAge(this.watchingForMs) : "";
        }

        public String distanceLabel() {
            return this.nearby ? Math.round(this.distance) + "m" : "";
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum Presence {
        ONLINE,
        SPECTATOR,
        VANISHED;

        private Presence() {}
    }

    @Environment(EnvType.CLIENT)
    private static final class Sighting {

        long provenAt;

        long watchingSince;

        long alertedAt;

        long highlightUntil;

        boolean nearby;

        float distance;

        long bodyLostAt;

        int bodyTick;

        int switchAlertTick = -1;

        boolean switchedToSpectator;

        long untrackedSince;

        private Sighting() {}

        boolean marked(long l) {
            return this.nearby || this.provenAt != 0L && l - this.provenAt < 600000L;
        }

        void prove(long provenAt2) {
            if (!this.watching(provenAt2)) {
                this.watchingSince = provenAt2;
            }
            this.provenAt = provenAt2;
        }

        boolean watching(long l) {
            return this.nearby || this.provenAt != 0L && l - this.provenAt < 10000L;
        }

        boolean coolingDown(long l) {
            return this.alertedAt != 0L && l - this.alertedAt < 120000L;
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Staff {

        final UUID id;

        String name = "";

        String rank = "";

        int seniority;

        StaffListModule.Presence presence = StaffListModule.Presence.ONLINE;

        int offTabScans;

        Staff(UUID id2) {
            this.id = id2;
        }

        void update(String name2, StaffMatcher.Match match2, StaffListModule.Presence presence2) {
            this.name = name2;
            this.rank = match2.rank();
            this.seniority = match2.seniority();
            this.presence = presence2;
        }
    }
}
