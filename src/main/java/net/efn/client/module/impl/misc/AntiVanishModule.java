/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.misc;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.StaffGhost;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.util.NameFilter;
import net.efn.client.util.antivanish.AntiVanishHeuristics;
import net.efn.client.util.antivanish.AntiVanishText;
import net.efn.client.util.staff.ShardTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(value = EnvType.CLIENT)
public final class AntiVanishModule extends ClientModule {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    private static final int SENSOR_RANGE = 64;

    private static final long SIGNAL_WINDOW_MS = 15000L;

    private static final long DETECTION_TTL_MS = 20000L;

    private static final long CRITICAL_COOLDOWN_MS = 12000L;

    private static final long ANNOUNCE_COOLDOWN_MS = 1500L;

    private static final long ANNOUNCE_WINDOW_MS = 4000L;

    private static final int MAX_ANNOUNCE_PER_WINDOW = 6;

    private static final int MAX_OBSERVATIONS_PER_TICK = 512;

    private static final int CRITICAL_SCORE = 35;

    private static final long SELF_PLACE_TTL_MS = 6000L;

    private static final long CONTAINER_SELF_GRACE_MS = 2500L;

    private static final long SPELL_SETTLE_MS = 1000L;

    private static final long GOLIATH_QUIET_MS = 4000L;

    private static final long HINT_HOLD_MS = 1000L;

    private static final long TAB_PAIR_MS = 1000L;

    private static final long TEAM_BATCH_MS = 50L;

    private static final int TEAM_BATCH = 5;

    private static final long HINT_MEMORY_MS = 5000L;

    private static final long ARRIVAL_REPEAT_MS = 60000L;

    private static final int STAFF_RANK_MAX = 8;

    private static final Pattern RANK_TEAM = Pattern.compile("(\\d{2})-(.+)-(?:true|false)");

    private static final Set<PlayerListS2CPacket.Action> ORDER_ONLY =
            EnumSet.of(
                    PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME,
                    PlayerListS2CPacket.Action.UPDATE_LIST_ORDER);

    private final ConcurrentLinkedQueue<Hint> hints = new ConcurrentLinkedQueue();

    private final List<Hint> heldHints = new ArrayList<Hint>();

    private final Deque<Hint> tabMoves = new ArrayDeque<Hint>();

    private final Deque<Long> teamChanges = new ArrayDeque<Long>();

    private final Map<UUID, String> tabNames = new HashMap<UUID, String>();

    private final Map<String, Long> arrivals = new HashMap<String, Long>();

    private volatile long quietUntilMs;

    private final ConcurrentLinkedQueue<Observation> observations = new ConcurrentLinkedQueue();

    private final Map<UUID, KnownPlayer> knownPlayers = new HashMap<UUID, KnownPlayer>();

    private final Map<String, Detection> detections = new LinkedHashMap<String, Detection>();

    private final Map<String, Long> signalCooldowns = new HashMap<String, Long>();

    private final Map<String, Long> announceCooldowns = new HashMap<String, Long>();

    private final Deque<Long> announceTimes = new ArrayDeque<Long>();

    private final Map<Long, Long> selfPlacedBlocks = new ConcurrentHashMap<Long, Long>();

    private final Map<UUID, Long> confirmedDepartures = new HashMap<UUID, Long>();

    private final Map<Long, Long> automatedMechanisms = new HashMap<Long, Long>();

    private final Map<String, Deque<Long>> weakParticleBursts = new HashMap<String, Deque<Long>>();

    private final Deque<Signal> signals = new ArrayDeque<Signal>();

    private final Deque<ExplosionEvent> recentExplosions = new ArrayDeque<ExplosionEvent>();

    private final Deque<PendingVanish> pendingVanishes = new ArrayDeque<PendingVanish>();

    private final Map<String, HiddenSpell> hiddenSpells = new LinkedHashMap<String, HiddenSpell>();

    private final Deque<RecentMessage> recentMessages = new ArrayDeque<RecentMessage>();

    private boolean serverSendsLeaveMessages;

    private static final long RECENT_MESSAGE_TTL_MS = 8000L;

    private static final long TRUSTED_LISTED_MS = 1500L;

    private final Map<UUID, Long> listedSinceMs = new HashMap<UUID, Long>();

    private final ShardTracker shard = new ShardTracker();

    private int tickCounter;

    private volatile long lastLocalActionMs;

    private long lastContainerActivityMs;

    private long lastCriticalMs;

    private long criticalUntilMs;

    private String criticalWatcher = "";

    private final Set<UUID> pendingChimes = new HashSet<UUID>();

    private UUID ghostId;

    private String ghostName;

    public AntiVanishModule() {
        super("anti-vanish", "Anti Vanish", "Detects vanished players.", ModuleCategory.DONUT);
        this.usesNotifications();
    }

    @Override
    protected void onEnable() {
        this.resetRuntime();
        if (AntiVanishModule.MC.player != null && AntiVanishModule.MC.world != null) {
            this.trackListedPlayers();
        }
    }

    @Override
    protected void onDisable() {
        this.resetRuntime();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (AntiVanishModule.MC.player == null
                || AntiVanishModule.MC.world == null
                || MC.getNetworkHandler() == null) {
            this.resetRuntime();
            return;
        }
        if (this.shard.moved(MC)) {
            this.resetRuntime();
        }
        this.playPendingChime();
        ++this.tickCounter;
        if (AntiVanishModule.MC.player.currentScreenHandler
                != AntiVanishModule.MC.player.playerScreenHandler) {
            this.lastContainerActivityMs = System.currentTimeMillis();
        }
        this.drainObservations();
        this.processPendingVanishes();
        this.judgeHints();
        if (this.tickCounter % 10 == 0) {
            this.trackListedPlayers();
            this.reviewSpells();
        }
        if (this.tickCounter % 5 == 0) {
            this.scanInvisiblePlayers();
        }
        this.pruneState();
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent event) {
        this.handleOutbound(event.packet);
    }

    private void handleOutbound(Packet<?> packet) {
        if (packet == null) {
            return;
        }
        if (packet instanceof PlayerInteractBlockC2SPacket) {
            PlayerInteractBlockC2SPacket playerInteractBlockPacket =
                    (PlayerInteractBlockC2SPacket) packet;
            this.lastLocalActionMs = System.currentTimeMillis();
            if (playerInteractBlockPacket.getBlockHitResult() != null
                    && playerInteractBlockPacket.getBlockHitResult().getBlockPos() != null) {
                long l = System.currentTimeMillis() + 6000L;
                BlockPos pos = playerInteractBlockPacket.getBlockHitResult().getBlockPos();
                BlockPos pos2 = pos.offset(playerInteractBlockPacket.getBlockHitResult().getSide());
                this.selfPlacedBlocks.put(pos.asLong(), l);
                this.selfPlacedBlocks.put(pos2.asLong(), l);
                String string = "";
                if (AntiVanishModule.MC.player != null
                        && playerInteractBlockPacket.getHand() != null) {
                    Identifier id2 =
                            Registries.ITEM.getId(
                                    AntiVanishModule.MC
                                            .player
                                            .getStackInHand(playerInteractBlockPacket.getHand())
                                            .getItem());
                    string = AntiVanishHeuristics.path(id2 == null ? "" : id2.toString());
                }
                AntiVanishModule.markSelfMultiBlockFootprint(pos, string, this.selfPlacedBlocks, l);
                AntiVanishModule.markSelfMultiBlockFootprint(
                        pos2, string, this.selfPlacedBlocks, l);
            }
            return;
        }
        if (packet instanceof PlayerActionC2SPacket
                || packet instanceof PlayerInteractItemC2SPacket
                || packet instanceof PlayerInteractEntityC2SPacket
                || packet instanceof HandSwingC2SPacket) {
            this.lastLocalActionMs = System.currentTimeMillis();
        }
    }

    static void markSelfMultiBlockFootprint(
            BlockPos pos, String string, Map<Long, Long> map, long l) {
        if (pos == null || map == null) {
            return;
        }
        map.put(pos.asLong(), l);
        String string2 = string == null ? "" : string;
        boolean bl =
                string2.endsWith("_door") && !string2.endsWith("trapdoor")
                        || string2.contains("sunflower")
                        || string2.contains("lilac")
                        || string2.contains("rose_bush")
                        || string2.contains("peony")
                        || string2.contains("tall_grass")
                        || string2.contains("large_fern")
                        || string2.contains("pitcher_plant");
        boolean bl2 = bl;
        if (bl) {
            map.put(pos.up().asLong(), l);
            map.put(pos.down().asLong(), l);
        }
        if (string2.endsWith("_bed")) {
            map.put(pos.add(1, 0, 0).asLong(), l);
            map.put(pos.add(-1, 0, 0).asLong(), l);
            map.put(pos.add(0, 0, 1).asLong(), l);
            map.put(pos.add(0, 0, -1).asLong(), l);
        }
    }

    @EventHandler
    public void onPacketReceiveNetty(PacketReceiveNettyEvent event) {
        if (this.handleInbound(event.packet)) {
            event.setCancelled(true);
        }
    }

    private boolean handleInbound(Packet<?> packet) {
        if (packet instanceof BundleS2CPacket) {
            BundleS2CPacket bundlePacket = (BundleS2CPacket) packet;
            for (Packet packet2 : bundlePacket.getPackets()) {
                this.handleInbound(packet2);
            }
            return false;
        }
        if (packet instanceof PlayerRemoveS2CPacket) {
            PlayerRemoveS2CPacket playerRemovePacket = (PlayerRemoveS2CPacket) packet;
            if (playerRemovePacket.profileIds().size() < 4) {
                for (UUID profileId2 : playerRemovePacket.profileIds()) {
                    this.observations.offer(Observation.tabRemove(profileId2));
                }
            }
            long l = System.currentTimeMillis();
            for (UUID profileId3 : playerRemovePacket.profileIds()) {
                this.hints.offer(new Hint(HintKind.TAB_OUT, profileId3, null, -1, l));
            }
        } else if (packet instanceof PlayerListS2CPacket) {
            PlayerListS2CPacket playerListPacket = (PlayerListS2CPacket) packet;
            this.queueTabHints(playerListPacket);
            if (playerListPacket
                    .getActions()
                    .contains((Object) PlayerListS2CPacket.Action.UPDATE_LISTED)) {
                int i = 0;
                for (PlayerListS2CPacket.Entry entry : playerListPacket.getEntries()) {
                    if (entry == null || entry.listed()) continue;
                    ++i;
                }
                if (i > 0 && i < 4) {
                    for (PlayerListS2CPacket.Entry entry2 : playerListPacket.getEntries()) {
                        if (entry2 == null || entry2.listed()) continue;
                        this.observations.offer(Observation.tabHide(entry2.profileId()));
                    }
                }
            }
            if (playerListPacket
                    .getActions()
                    .contains((Object) PlayerListS2CPacket.Action.UPDATE_GAME_MODE)) {
                this.queueGamemodeUpdates(playerListPacket);
            }
        } else if (packet instanceof GameMessageS2CPacket) {
            String departedPlayerName;
            GameMessageS2CPacket gameMessagePacket = (GameMessageS2CPacket) packet;
            String string =
                    gameMessagePacket.content() == null
                            ? ""
                            : gameMessagePacket.content().getString();
            String string2 = string;
            if (!string.isBlank()) {
                this.observations.offer(Observation.systemChat(string));
            }
            if (!(departedPlayerName =
                            AntiVanishModule.departedPlayerName(gameMessagePacket.content()))
                    .isBlank()) {
                this.observations.offer(Observation.playerLeft(departedPlayerName));
            }
        } else if (packet instanceof EntityTrackerUpdateS2CPacket) {
            EntityTrackerUpdateS2CPacket entityTrackerUpdatePacket =
                    (EntityTrackerUpdateS2CPacket) packet;
            this.observations.offer(Observation.entityMetadata(entityTrackerUpdatePacket.id()));
        } else if (packet instanceof ExplosionS2CPacket) {
            ExplosionS2CPacket explosionPacket = (ExplosionS2CPacket) packet;
            this.observations.offer(
                    Observation.explosion(explosionPacket.center(), explosionPacket.radius()));
        } else if (packet instanceof PlaySoundFromEntityS2CPacket) {
            PlaySoundFromEntityS2CPacket playSoundFromEntityPacket =
                    (PlaySoundFromEntityS2CPacket) packet;
            this.observations.offer(
                    Observation.entitySound(
                            playSoundFromEntityPacket.getEntityId(),
                            AntiVanishModule.soundId(
                                    playSoundFromEntityPacket.getSound().value().id())));
        } else if (packet instanceof ParticleS2CPacket) {
            ParticleS2CPacket particlePacket = (ParticleS2CPacket) packet;
            Identifier id2 =
                    Registries.PARTICLE_TYPE.getId(particlePacket.getParameters().getType());
            this.observations.offer(
                    Observation.position(
                            ObservationType.PARTICLE,
                            particlePacket.getX(),
                            particlePacket.getY(),
                            particlePacket.getZ(),
                            id2 == null ? "" : id2.toString()));
        } else if (packet instanceof PlaySoundS2CPacket) {
            PlaySoundS2CPacket playSoundPacket = (PlaySoundS2CPacket) packet;
            this.onSoundPacket(playSoundPacket);
        } else if (packet instanceof TeamS2CPacket) {
            TeamS2CPacket teamPacket = (TeamS2CPacket) packet;
            this.queueTeamHint(teamPacket);
        }
        return false;
    }

    private void queueTabHints(PlayerListS2CPacket playerListPacket) {
        block3:
        {
            long l;
            block2:
            {
                l = System.currentTimeMillis();
                if (!playerListPacket
                        .getActions()
                        .contains((Object) PlayerListS2CPacket.Action.ADD_PLAYER)) break block2;
                for (PlayerListS2CPacket.Entry entry : playerListPacket.getEntries()) {
                    if (entry == null || entry.profile() == null || entry.profile().name() == null)
                        continue;
                    this.hints.offer(
                            new Hint(
                                    HintKind.TAB_IN,
                                    entry.profileId(),
                                    entry.profile().name(),
                                    -1,
                                    l));
                }
                break block3;
            }
            if (!ORDER_ONLY.equals(playerListPacket.getActions())
                    || playerListPacket.getEntries().size() >= 4) break block3;
            for (PlayerListS2CPacket.Entry entry2 : playerListPacket.getEntries()) {
                if (entry2 == null || entry2.profileId() == null) continue;
                this.hints.offer(new Hint(HintKind.ORDER, entry2.profileId(), null, -1, l));
            }
        }
    }

    private void queueTeamHint(TeamS2CPacket teamPacket) {
        TeamS2CPacket.Operation teamOperation = teamPacket.getTeamOperation();
        Matcher matcher =
                teamOperation == null ? null : RANK_TEAM.matcher(teamPacket.getTeamName());
        Matcher matcher2 = matcher;
        if (matcher == null || !matcher.matches()) {
            return;
        }
        String group = matcher.group(2);
        boolean bl = teamOperation == TeamS2CPacket.Operation.ADD;
        boolean bl2 = bl;
        if (bl && !teamPacket.getPlayerNames().isEmpty()) {
            if (teamPacket.getPlayerNames().stream().noneMatch(group::equalsIgnoreCase)) {
                return;
            }
        }
        this.hints.offer(
                new Hint(
                        bl ? HintKind.TEAM_UP : HintKind.TEAM_DOWN,
                        null,
                        group,
                        Integer.parseInt(matcher.group(1)),
                        System.currentTimeMillis()));
    }

    private void judgeHints() {
        Hint hint;
        while ((hint = this.hints.poll()) != null) {
            switch (hint.kind().ordinal()) {
                case 3:
                    {
                        this.tabMoves.addLast(hint);
                        break;
                    }
                case 4:
                    {
                        String tabName = this.tabNames.get(hint.id());
                        if (tabName == null) break;
                        this.tabMoves.addLast(
                                new Hint(HintKind.TAB_OUT, hint.id(), tabName, -1, hint.atMs()));
                        break;
                    }
                case 1:
                case 2:
                    {
                        this.teamChanges.addLast(hint.atMs());
                        this.heldHints.add(hint);
                        break;
                    }
                case 0:
                    {
                        this.heldHints.add(hint);
                    }
            }
        }
        long l = System.currentTimeMillis();
        Iterator<Hint> iterator = this.heldHints.iterator();
        while (iterator.hasNext()) {
            Hint hint2 = iterator.next();
            if (l - hint2.atMs() < 1000L) continue;
            iterator.remove();
            this.judge(hint2);
        }
        while (!this.tabMoves.isEmpty() && l - this.tabMoves.peekFirst().atMs() > 5000L) {
            this.tabMoves.removeFirst();
        }
        while (!this.teamChanges.isEmpty() && l - this.teamChanges.peekFirst() > 5000L) {
            this.teamChanges.removeFirst();
        }
        if (this.tabNames.size() > 4096) {
            this.tabNames.clear();
        }
        for (PlayerListEntry player : MC.getNetworkHandler().getPlayerList()) {
            if (player.getProfile() == null || player.getProfile().name() == null) continue;
            this.tabNames.put(player.getProfile().id(), player.getProfile().name());
        }
    }

    private void judge(Hint hint) {
        int i;
        String string;
        if (hint.atMs() < this.quietUntilMs) {
            return;
        }
        PlayerListEntry listEntry =
                hint.id() != null
                        ? MC.getNetworkHandler().getPlayerListEntry(hint.id())
                        : MC.getNetworkHandler().getCaseInsensitivePlayerInfo(hint.name());
        PlayerListEntry listEntry2 = listEntry;
        string =
                hint.name() != null
                        ? hint.name()
                        : (listEntry == null || listEntry.getProfile() == null
                                ? null
                                : listEntry.getProfile().name());
        if (string == null
                || string.equalsIgnoreCase(AntiVanishModule.MC.player.getGameProfile().name())) {
            return;
        }
        int j = i = hint.rank() >= 0 ? hint.rank() : AntiVanishModule.teamRank(listEntry);
        if (!(StaffListModule.isStaffName(string) || i >= 0 && i <= 8)) {
            return;
        }
        UUID uuid2 =
                listEntry != null && listEntry.getProfile() != null
                        ? listEntry.getProfile().id()
                        : hint.id();
        switch (hint.kind().ordinal()) {
            case 0:
                {
                    this.arrived(uuid2, string, "tab re-sort");
                    break;
                }
            case 1:
                {
                    if (!this.teamOnly(hint)) break;
                    this.arrived(uuid2, string, "team created");
                    break;
                }
            case 2:
                {
                    if (!this.teamOnly(hint)) break;
                    this.departed(string, "team removed");
                }
        }
    }

    private boolean teamOnly(Hint hint) {
        int i = 0;
        for (long teamChange : this.teamChanges) {
            if (Math.abs(teamChange - hint.atMs()) > 50L || ++i < 5) continue;
            return false;
        }
        for (Hint tabMove : this.tabMoves) {
            if (!tabMove.name().equalsIgnoreCase(hint.name())
                    || Math.abs(tabMove.atMs() - hint.atMs()) > 1000L) continue;
            return false;
        }
        return true;
    }

    private static int teamRank(PlayerListEntry listEntry) {
        Team team = listEntry == null ? null : listEntry.getScoreboardTeam();
        Matcher matcher = team == null ? null : RANK_TEAM.matcher(team.getName());
        return matcher != null && matcher.matches() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private void arrived(UUID uuid2, String string, String string2) {
        long l = System.currentTimeMillis();
        Long long_Value = this.arrivals.put(AntiVanishModule.spellKey(string), l);
        if (uuid2 != null) {
            StaffListModule.provenHere(uuid2, string);
        }
        if (long_Value != null && l - long_Value < 60000L) {
            return;
        }
        this.news(string, ModuleChat.body().text("is ").danger("on your goliath").build(), string2);
        this.watchingYou(string, new RegionProof(uuid2, -1));
    }

    private void departed(String string, String string2) {
        if (this.arrivals.remove(AntiVanishModule.spellKey(string)) == null) {
            return;
        }
        this.news(string, ModuleChat.body().good("left your goliath").build(), string2);
    }

    private void onSoundPacket(PlaySoundS2CPacket playSoundPacket) {
        if (playSoundPacket == null
                || playSoundPacket.getSound() == null
                || playSoundPacket.getSound().value() == null) {
            return;
        }
        String soundId = AntiVanishModule.soundId(playSoundPacket.getSound().value().id());
        this.observations.offer(
                Observation.position(
                        ObservationType.POSITIONAL_SOUND,
                        playSoundPacket.getX(),
                        playSoundPacket.getY(),
                        playSoundPacket.getZ(),
                        soundId));
    }

    public static boolean shouldShowHud() {
        AntiVanishModule module = AntiVanishModule.instance();
        return module != null && module.isEnabled() && module.hasHudContent();
    }

    private boolean hasHudContent() {
        long l = System.currentTimeMillis();
        if (l < this.criticalUntilMs) {
            return true;
        }
        for (Detection detection : this.detections.values()) {
            if (detection.expiresAt <= l || !AntiVanishModule.detectionWorthShowing(detection))
                continue;
            return true;
        }
        return false;
    }

    private static boolean detectionWorthShowing(Detection detection) {
        if (detection == null) {
            return false;
        }
        String string = detection.name == null ? "" : detection.name.trim();
        return !string.isBlank()
                && !"Unknown".equalsIgnoreCase(string)
                && !"You".equalsIgnoreCase(string)
                && !"CRITICAL".equalsIgnoreCase(string);
    }

    public static String hudTag(HudEntry hudEntry) {
        if (hudEntry == null) {
            return "WATCH";
        }
        if ("CRITICAL".equalsIgnoreCase(hudEntry.name())) {
            return "ALERT";
        }
        String string = hudEntry.reason() == null ? "" : hudEntry.reason().toLowerCase(Locale.ROOT);
        String string2 = string;
        if (string.startsWith("vanish event")) {
            return "VANISH";
        }
        if (string.startsWith("invisible entity")) {
            return "INVIS";
        }
        if (string.startsWith("suspicious sound")) {
            return "SOUND";
        }
        if (string.startsWith("ghost particle")) {
            return "PARTICLE";
        }
        return "WATCH";
    }

    public static String hudValue(HudEntry hudEntry) {
        if (hudEntry == null) {
            return "Staff";
        }
        if ("CRITICAL".equalsIgnoreCase(hudEntry.name())) {
            return AntiVanishModule.shortName(hudEntry.reason());
        }
        String string = hudEntry.name() == null ? "" : hudEntry.name().trim();
        String string2 = string;
        if (string.isBlank()
                || "Unknown".equalsIgnoreCase(string)
                || "You".equalsIgnoreCase(string)) {
            return "Staff";
        }
        return AntiVanishModule.shortName(string);
    }

    private static String shortName(String string) {
        String string2 = string == null ? "" : NameFilter.apply(string.trim());
        return string2.length() <= 16 ? string2 : string2.substring(0, 16);
    }

    public static List<HudEntry> hudEntries() {
        AntiVanishModule module = AntiVanishModule.instance();
        return module == null || !AntiVanishModule.shouldShowHud()
                ? List.of()
                : module.hudSnapshot();
    }

    private static AntiVanishModule instance() {
        return ModuleRegistry.get(AntiVanishModule.class);
    }

    private void drainObservations() {
        Observation observation;
        for (int i = 0; i < 512 && (observation = this.observations.poll()) != null; ++i) {
            this.processObservation(observation);
        }
        while (this.observations.size() > 4096) {
            this.observations.poll();
        }
    }

    private void processObservation(Observation observation) {
        switch (observation.type.ordinal()) {
            case 0:
                {
                    this.handleTabRemoval(observation.profileId);
                    break;
                }
            case 1:
                {
                    this.handleTabHidden(observation.profileId);
                    break;
                }
            case 2:
                {
                    this.serverSendsLeaveMessages = true;
                    this.confirmDeparture(observation.detail);
                    break;
                }
            case 3:
                {
                    this.cacheRecentMessage(observation.detail);
                    break;
                }
            case 4:
                {
                    this.inspectInvisibleEntity(observation.entityId);
                    break;
                }
            case 5:
                {
                    this.inspectPositionalSound(observation);
                    break;
                }
            case 6:
                {
                    this.inspectEntitySound(observation);
                    break;
                }
            case 7:
                {
                    this.inspectParticle(observation);
                    break;
                }
            case 8:
                {
                    this.rememberExplosion(observation);
                    break;
                }
            case 9:
                {
                    this.announceGamemode(observation.profileId, observation.detail);
                }
        }
    }

    private void trackListedPlayers() {
        if (MC.getNetworkHandler() == null) {
            return;
        }
        long l = System.currentTimeMillis();
        for (PlayerListEntry listedPlayerListEntry :
                MC.getNetworkHandler().getListedPlayerListEntries()) {
            if (listedPlayerListEntry == null
                    || listedPlayerListEntry.getProfile() == null
                    || listedPlayerListEntry.getProfile().id() == null) continue;
            UUID uuid2 = listedPlayerListEntry.getProfile().id();
            if (AntiVanishModule.MC.player != null
                    && AntiVanishModule.MC.player.getUuid().equals(uuid2)) continue;
            this.listedSinceMs.putIfAbsent(uuid2, l);
            String string = listedPlayerListEntry.getProfile().name();
            if (string != null && !this.knownPlayers.containsKey(uuid2)) {
                this.knownPlayers.put(uuid2, new KnownPlayer(uuid2, string));
            }
            if (string == null || this.hiddenSpells.isEmpty()) continue;
            this.endSpell(string, false, 1000L);
        }
        if (this.listedSinceMs.size() > 1024 || this.knownPlayers.size() > 1024) {
            HashSet set = new HashSet();
            for (PlayerListEntry player : MC.getNetworkHandler().getPlayerList()) {
                if (player == null || player.getProfile() == null) continue;
                set.add(player.getProfile().id());
            }
            this.listedSinceMs.keySet().retainAll(set);
            this.knownPlayers.keySet().retainAll(set);
        }
    }

    private boolean trustedListed(UUID uuid2) {
        Long long_Value = uuid2 == null ? null : this.listedSinceMs.get(uuid2);
        return long_Value != null && System.currentTimeMillis() - long_Value >= 1500L;
    }

    private boolean credibleSubject(UUID uuid2, String string) {
        return uuid2 != null
                && AntiVanishText.isUsername(string)
                && StaffListModule.isStaffName(string)
                && this.trustedListed(uuid2);
    }

    private void handleTabRemoval(UUID uuid2) {
        if (uuid2 == null || AntiVanishModule.MC.player.getUuid().equals(uuid2)) {
            return;
        }
        String knownName = this.knownName(uuid2);
        if (!this.credibleSubject(uuid2, knownName)
                || !AntiVanishText.isPlausiblePlayerName(knownName)) {
            return;
        }
        this.pendingVanishes.removeIf(pendingVanish -> pendingVanish.uuid.equals(uuid2));
        this.pendingVanishes.addLast(new PendingVanish(uuid2, knownName, this.tickCounter + 20));
    }

    private void handleTabHidden(UUID uuid2) {
        if (uuid2 == null || AntiVanishModule.MC.player.getUuid().equals(uuid2)) {
            return;
        }
        KnownPlayer knownPlayer = this.knownPlayers.get(uuid2);
        String string =
                knownPlayer != null && knownPlayer.name != null
                        ? knownPlayer.name
                        : this.knownName(uuid2);
        String string2 = string;
        if (!this.credibleSubject(uuid2, string) || !AntiVanishText.isPlausiblePlayerName(string)) {
            return;
        }
        if (this.recentMessageNames(string)) {
            return;
        }
        this.reportHidden(
                uuid2, string, false, "Vanish Event: hidden from TAB", "hidden from TAB", 100);
    }

    private String knownName(UUID uuid2) {
        PlayerListEntry playerListEntry;
        KnownPlayer knownPlayer = this.knownPlayers.get(uuid2);
        if (knownPlayer != null && knownPlayer.name != null && !knownPlayer.name.isBlank()) {
            return knownPlayer.name;
        }
        if (MC.getNetworkHandler() != null
                && (playerListEntry = MC.getNetworkHandler().getPlayerListEntry(uuid2)) != null
                && playerListEntry.getProfile() != null) {
            return playerListEntry.getProfile().name();
        }
        return null;
    }

    private void confirmDeparture(String string) {
        long l = System.currentTimeMillis();
        for (KnownPlayer knownPlayer : this.knownPlayers.values()) {
            if (!AntiVanishText.containsPlayerName(string, knownPlayer.name)) continue;
            this.confirmedDepartures.put(knownPlayer.uuid, l + 5000L);
            this.pendingVanishes.removeIf(
                    pendingVanish -> pendingVanish.uuid.equals(knownPlayer.uuid));
            this.hiddenSpells.remove(AntiVanishModule.spellKey(knownPlayer.name));
            Detection detection = this.detections.get(knownPlayer.uuid.toString());
            if (detection != null && AntiVanishModule.tabDepartureReason(detection.reason)) {
                this.detections.remove(knownPlayer.uuid.toString());
            }
            this.signals.removeIf(
                    signal ->
                            signal.type == SignalType.VANISH
                                    && signal.subject.equalsIgnoreCase(knownPlayer.name)
                                    && AntiVanishModule.tabDepartureReason(signal.reason));
        }
    }

    private void processPendingVanishes() {
        while (!this.pendingVanishes.isEmpty()
                && this.pendingVanishes.peekFirst().dueTick <= this.tickCounter) {
            PendingVanish pendingVanish = this.pendingVanishes.removeFirst();
            if (MC.getNetworkHandler().getPlayerListEntry(pendingVanish.uuid) != null) continue;
            long l = System.currentTimeMillis();
            if (this.confirmedDepartures.getOrDefault(pendingVanish.uuid, 0L) > l) continue;
            if (this.recentMessageNames(pendingVanish.name)) {
                this.serverSendsLeaveMessages = true;
                continue;
            }
            PlayerEntity playerByUuid =
                    AntiVanishModule.MC.world.getPlayerByUuid(pendingVanish.uuid);
            if (playerByUuid != null && !playerByUuid.isRemoved()) {
                this.reportHidden(
                        pendingVanish.uuid,
                        pendingVanish.name,
                        false,
                        "Vanish Event: entity remained",
                        "off TAB, still nearby",
                        100);
                continue;
            }
            String string = "Vanish Event: silent TAB disappearance";
            int silentTabRemovalScore =
                    AntiVanishModule.silentTabRemovalScore(this.serverSendsLeaveMessages);
            if (this.serverSendsLeaveMessages) {
                this.reportHidden(
                        pendingVanish.uuid,
                        pendingVanish.name,
                        false,
                        string,
                        "left TAB silently",
                        silentTabRemovalScore);
                continue;
            }
            RegionProof regionProof = this.regionProof(pendingVanish.uuid);
            if (regionProof == null) continue;
            this.signals.addLast(
                    new Signal(
                            SignalType.VANISH,
                            pendingVanish.name,
                            regionProof,
                            string,
                            silentTabRemovalScore,
                            l));
            this.evaluateCritical();
        }
    }

    static int silentTabRemovalScore(boolean bl) {
        return bl ? 100 : 30;
    }

    static boolean tabDepartureReason(String string) {
        if (string == null) {
            return false;
        }
        String string2 = string.toLowerCase(Locale.ROOT);
        return string2.startsWith("vanish event:")
                && (string2.contains("tab")
                        || string2.contains("entity remained")
                        || string2.contains("no leave packet"));
    }

    private void cacheRecentMessage(String string) {
        if (string == null || string.isBlank()) {
            return;
        }
        long l = System.currentTimeMillis();
        this.recentMessages.addLast(new RecentMessage(string, l));
        while (!(this.recentMessages.isEmpty()
                || l - this.recentMessages.peekFirst().atMs <= 8000L
                        && this.recentMessages.size() <= 64)) {
            this.recentMessages.removeFirst();
        }
    }

    private boolean recentMessageNames(String string) {
        if (string == null || string.isBlank()) {
            return false;
        }
        long l = System.currentTimeMillis();
        for (RecentMessage recentMessage : this.recentMessages) {
            if (l - recentMessage.atMs > 8000L
                    || !AntiVanishText.looksLikeLeaveMessage(recentMessage.text, string)) continue;
            return true;
        }
        return false;
    }

    private static String departedPlayerName(Text text2) {
        String string;
        TranslatableTextContent translatableTextContent;
        TextContent content;
        if (text2 == null
                || !((content = text2.getContent()) instanceof TranslatableTextContent)
                || !"multiplayer.player.left"
                        .equals(
                                (translatableTextContent = (TranslatableTextContent) content)
                                        .getKey())) {
            return "";
        }
        Object[] args = translatableTextContent.getArgs();
        if (args.length == 0 || args[0] == null) {
            return "";
        }
        Object arg = args[0];
        if (arg instanceof Text) {
            Text text3 = (Text) arg;
            string = text3.getString();
        } else {
            string = String.valueOf(args[0]);
        }
        return string;
    }

    private void reportHidden(
            UUID uuid2, String string, boolean bl, String string2, String string3, int i) {
        this.upsertDetection(
                AntiVanishModule.detectionKey(uuid2, string),
                string,
                string2,
                i,
                System.currentTimeMillis() + 20000L);
        HiddenSpell hiddenSpell = this.hiddenSpells.get(AntiVanishModule.spellKey(string));
        if (hiddenSpell != null && hiddenSpell.spectator == bl) {
            return;
        }
        this.hiddenSpells.put(
                AntiVanishModule.spellKey(string),
                new HiddenSpell(
                        uuid2, string, bl, string3, this.regionProof(uuid2), this.tickCounter));
    }

    private void reviewSpells() {
        Iterator<HiddenSpell> iterator = this.hiddenSpells.values().iterator();
        while (iterator.hasNext()) {
            HiddenSpell hiddenSpell = iterator.next();
            if (hiddenSpell.spectator
                    && MC.getNetworkHandler().getPlayerListEntry(hiddenSpell.uuid) == null) {
                iterator.remove();
                continue;
            }
            if (hiddenSpell.watched || hiddenSpell.sinceTick >= this.tickCounter) continue;
            RegionProof regionProof = this.regionProof(hiddenSpell.uuid);
            if (regionProof == null && !hiddenSpell.told) {
                regionProof = hiddenSpell.proof;
            }
            if (regionProof != null) {
                hiddenSpell.told = true;
                hiddenSpell.watched = true;
                this.watchingYou(hiddenSpell.name, regionProof);
                continue;
            }
            if (hiddenSpell.told) continue;
            hiddenSpell.told = true;
            this.news(hiddenSpell.name, hiddenSpell.how(), hiddenSpell.detail);
        }
    }

    private boolean endSpell(String string, boolean bl, long l) {
        HiddenSpell hiddenSpell = this.hiddenSpells.get(AntiVanishModule.spellKey(string));
        if (hiddenSpell == null
                || hiddenSpell.spectator != bl
                || System.currentTimeMillis() - hiddenSpell.sinceMs < l) {
            return false;
        }
        this.hiddenSpells.remove(AntiVanishModule.spellKey(string));
        this.detections.remove(AntiVanishModule.detectionKey(hiddenSpell.uuid, hiddenSpell.name));
        if (hiddenSpell.told) {
            this.news(
                    hiddenSpell.name,
                    ModuleChat.body().text("is ").good("visible again").build(),
                    "");
        }
        return true;
    }

    private RegionProof regionProof(UUID uuid2) {
        if (uuid2 == null) {
            return null;
        }
        PlayerEntity playerByUuid = AntiVanishModule.MC.world.getPlayerByUuid(uuid2);
        if (playerByUuid != null && playerByUuid != AntiVanishModule.MC.player) {
            return new RegionProof(
                    uuid2, Math.round(playerByUuid.distanceTo(AntiVanishModule.MC.player)));
        }
        return StaffListModule.isInYourRegion(uuid2) ? new RegionProof(uuid2, -1) : null;
    }

    private static String spellKey(String string) {
        return string.toLowerCase(Locale.ROOT);
    }

    private static String detectionKey(UUID uuid2, String string) {
        return uuid2 != null ? uuid2.toString() : string;
    }

    private void scanInvisiblePlayers() {
        double sensorRangeSq = this.sensorRangeSq();
        for (PlayerEntity player : AntiVanishModule.MC.world.getPlayers()) {
            if (player == null
                    || player == AntiVanishModule.MC.player
                    || !player.isInvisible()
                    || player.squaredDistanceTo(AntiVanishModule.MC.player) > sensorRangeSq
                    || !this.realPlayer(player)) continue;
            this.triggerSensor(
                    SignalType.INVISIBLE, player, "Invisible Entity: metadata flag", 35, 5000L);
        }
    }

    private void inspectInvisibleEntity(int i) {
        PlayerEntity player;
        Entity entityById = AntiVanishModule.MC.world.getEntityById(i);
        if (!(entityById instanceof PlayerEntity)
                || (player = (PlayerEntity) entityById) == AntiVanishModule.MC.player
                || !player.isInvisible()) {
            return;
        }
        if (player.squaredDistanceTo(AntiVanishModule.MC.player) > this.sensorRangeSq()) {
            return;
        }
        if (!this.realPlayer(player)) {
            return;
        }
        this.triggerSensor(
                SignalType.INVISIBLE, player, "Invisible Entity: metadata flag", 35, 5000L);
    }

    private boolean realPlayer(PlayerEntity player) {
        return this.credibleSubject(player.getUuid(), AntiVanishModule.profileName(player));
    }

    private static String profileName(PlayerEntity player) {
        return player.getGameProfile() == null
                ? player.getName().getString()
                : player.getGameProfile().name();
    }

    private void inspectPositionalSound(Observation observation) {
        if (!AntiVanishHeuristics.suspiciousSound(observation.detail)) {
            return;
        }
        Vec3d vec = observation.position();
        long l = System.currentTimeMillis();
        if (!this.nearPlayer(vec)
                || this.hasVisibleCause(vec)
                || this.isExplosionRelated(vec)
                || this.isPoweredMechanism(vec, observation.detail)
                || l - this.lastLocalActionMs < 1000L
                || this.recentlySelfPlaced(vec)
                || this.villagerToggledDoor(observation.detail, vec)
                || this.selfContainerActive()
                        && AntiVanishModule.isContainerSignal(observation.detail)) {
            return;
        }
        this.triggerSensor(
                SignalType.SOUND,
                this.locatedSubject(vec),
                "Suspicious Sound: " + AntiVanishModule.shortId(observation.detail),
                14,
                3000L);
    }

    private void inspectEntitySound(Observation observation) {
        PlayerEntity player;
        if (!AntiVanishHeuristics.suspiciousSound(observation.detail)) {
            return;
        }
        Entity entityById = AntiVanishModule.MC.world.getEntityById(observation.entityId);
        if (entityById instanceof PlayerEntity
                && (player = (PlayerEntity) entityById) != AntiVanishModule.MC.player
                && player.isInvisible()
                && player.squaredDistanceTo(AntiVanishModule.MC.player) <= this.sensorRangeSq()
                && this.realPlayer(player)) {
            this.triggerSensor(
                    SignalType.SOUND, player, "Suspicious Sound: invisible source", 16, 3000L);
        }
    }

    private void inspectParticle(Observation observation) {
        if (!AntiVanishHeuristics.suspiciousParticle(observation.detail)) {
            return;
        }
        Vec3d vec = observation.position();
        long l = System.currentTimeMillis();
        if (!this.nearPlayer(vec)
                || this.hasVisibleCause(vec)
                || this.isExplosionRelated(vec)
                || l - this.lastLocalActionMs < 900L
                || this.hasAmbientParticleSource(vec, observation.detail)) {
            return;
        }
        String shortId = AntiVanishModule.shortId(observation.detail);
        if ((shortId.equals("block") || shortId.contains("smoke")) && this.nearSelf(vec, 6.25)) {
            return;
        }
        if ((shortId.equals("block") || shortId.contains("smoke"))
                && !this.particleBurstReady(shortId, l)) {
            return;
        }
        this.triggerSensor(
                SignalType.PARTICLE,
                this.locatedSubject(vec),
                "Ghost Particle: " + AntiVanishModule.shortId(observation.detail),
                16,
                3000L);
    }

    private boolean villagerToggledDoor(String string, Vec3d vec) {
        String path = AntiVanishHeuristics.path(string);
        boolean bl =
                path.contains("door") && !path.contains("trapdoor") || path.contains("fence_gate");
        boolean bl2 = bl;
        if (!bl || vec == null || AntiVanishModule.MC.world == null) {
            return false;
        }
        for (Entity entity : AntiVanishModule.MC.world.getEntities()) {
            if (entity.getType() != EntityType.VILLAGER
                    || !(entity.getEntityPos().squaredDistanceTo(vec) <= 9.0)) continue;
            return true;
        }
        return false;
    }

    private boolean recentlySelfPlaced(Vec3d vec) {
        if (vec == null) {
            return false;
        }
        Long selfPlacedBlock = this.selfPlacedBlocks.get(BlockPos.ofFloored(vec).asLong());
        return selfPlacedBlock != null && selfPlacedBlock > System.currentTimeMillis();
    }

    private void rememberExplosion(Observation observation) {
        long l = System.currentTimeMillis();
        this.recentExplosions.addLast(
                new ExplosionEvent(
                        observation.position(),
                        Math.max(2.0, AntiVanishModule.parseDouble(observation.detail, 4.0) + 4.0),
                        l));
        while (this.recentExplosions.size() > 8) {
            this.recentExplosions.removeFirst();
        }
    }

    private boolean particleBurstReady(String string, long l) {
        Deque deque = this.weakParticleBursts.computeIfAbsent(string, value -> new ArrayDeque());
        deque.addLast(l);
        while (!deque.isEmpty() && l - (Long) deque.peekFirst() > 2000L) {
            deque.removeFirst();
        }
        if (deque.size() < 2) {
            return false;
        }
        deque.clear();
        return true;
    }

    private void triggerSensor(
            SignalType signalType, String string, String string2, int i, long l) {
        this.addSignal(signalType, string, null, string2, i, l);
    }

    private void triggerSensor(
            SignalType signalType, PlayerEntity player, String string, int i, long l) {
        this.addSignal(
                signalType,
                AntiVanishModule.profileName(player),
                this.regionProof(player.getUuid()),
                string,
                i,
                l);
    }

    private void addSignal(
            SignalType signalType,
            String string,
            RegionProof regionProof,
            String string2,
            int i,
            long l) {
        String string3;
        long l2;
        long l3 = System.currentTimeMillis();
        if (l3
                        - (l2 =
                                this.signalCooldowns
                                        .getOrDefault(
                                                string3 =
                                                        signalType.name()
                                                                + "|"
                                                                + string.toLowerCase(Locale.ROOT),
                                                0L)
                                        .longValue())
                < l) {
            return;
        }
        this.signalCooldowns.put(string3, l3);
        this.signals.addLast(new Signal(signalType, string, regionProof, string2, i, l3));
        this.upsertDetection("signal:" + string3, string, string2, i, l3 + 20000L);
        this.announceTrigger(string3, string, regionProof != null, string2);
        this.evaluateCritical();
    }

    private void evaluateCritical() {
        long l = System.currentTimeMillis();
        this.pruneSignals(l);
        EnumMap<SignalType, Integer> enumMap = new EnumMap<SignalType, Integer>(SignalType.class);
        Signal signal = null;
        for (Signal signal2 : this.signals) {
            enumMap.merge(signal2.type, signal2.weight, Math::max);
            if (signal2.proof == null) continue;
            signal = signal2;
        }
        int i = enumMap.values().stream().mapToInt(Integer::intValue).sum();
        if (signal == null || enumMap.size() < 2 || i < 35) {
            return;
        }
        if (l - this.lastCriticalMs < 12000L) {
            return;
        }
        this.watchingYou(signal.subject, signal.proof);
    }

    private void watchingYou(String ghostName2, RegionProof regionProof) {
        long l;
        this.lastCriticalMs = l = System.currentTimeMillis();
        this.criticalUntilMs = l + 7000L;
        this.criticalWatcher = ghostName2;
        this.pendingChimes.add(regionProof.id());
        this.ghostId = regionProof.id();
        this.ghostName = ghostName2;
    }

    private void playPendingChime() {
        if (this.pendingChimes.isEmpty()) {
            return;
        }
        boolean allMatch = this.pendingChimes.stream().allMatch(StaffListModule::alarmedRecently);
        this.pendingChimes.clear();
        if (allMatch) {
            return;
        }
        if (MC.getSoundManager() != null) {
            MC.getSoundManager()
                    .play(
                            PositionedSoundInstance.ui(
                                    SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.0f));
        }
        StaffGhost.watching(this.ghostId, this.ghostName, 0);
    }

    private void news(String string, Text text2, String string2) {
        ModuleChat.Body body =
                ModuleChat.body().name(NameFilter.apply(string)).text(" ").append(text2);
        if (!string2.isEmpty()) {
            body.text(" (" + string2 + ")");
        }
        ModuleChat.send((ClientModule) this, body);
    }

    private void queueGamemodeUpdates(PlayerListS2CPacket playerListPacket) {
        if (playerListPacket
                .getActions()
                .contains((Object) PlayerListS2CPacket.Action.ADD_PLAYER)) {
            return;
        }
        for (PlayerListS2CPacket.Entry entry : playerListPacket.getEntries()) {
            if (entry == null || entry.gameMode() == null || entry.profileId() == null) continue;
            this.observations.offer(
                    Observation.gamemode(entry.profileId(), entry.gameMode().getId()));
        }
    }

    private void announceGamemode(UUID uuid2, String string) {
        if (uuid2 == null
                || AntiVanishModule.MC.player != null
                        && AntiVanishModule.MC.player.getUuid().equals(uuid2)) {
            return;
        }
        KnownPlayer knownPlayer = this.knownPlayers.get(uuid2);
        if (knownPlayer == null || !StaffListModule.isStaffName(knownPlayer.name)) {
            return;
        }
        this.arrivals.put(AntiVanishModule.spellKey(knownPlayer.name), System.currentTimeMillis());
        if (GameMode.SPECTATOR.getId().equals(string)) {
            if (this.credibleSubject(uuid2, knownPlayer.name)) {
                this.reportHidden(
                        uuid2,
                        knownPlayer.name,
                        true,
                        "Vanish Event: switched to spectator",
                        "",
                        100);
                return;
            }
        } else if (this.endSpell(knownPlayer.name, true, 0L)) {
            return;
        }
        ModuleChat.send(
                (ClientModule) this,
                StaffListModule.switchedTo(
                        ModuleChat.body().name(NameFilter.apply(knownPlayer.name)).text(" "),
                        string));
    }

    private void announceTrigger(String string, String string2, boolean bl, String string3) {
        long l = System.currentTimeMillis();
        Long announceCooldown = this.announceCooldowns.get(string);
        if (announceCooldown != null && l - announceCooldown < 1500L) {
            return;
        }
        while (!this.announceTimes.isEmpty() && l - this.announceTimes.peekFirst() > 4000L) {
            this.announceTimes.removeFirst();
        }
        if (this.announceTimes.size() >= 6) {
            return;
        }
        this.announceCooldowns.put(string, l);
        this.announceTimes.addLast(l);
        int i = string3.indexOf(": ");
        ModuleChat.Body body =
                i < 0
                        ? ModuleChat.body().warn(string3)
                        : ModuleChat.body()
                                .warn(string3.substring(0, i))
                                .text(string3.substring(i));
        boolean bl2 =
                string2 != null
                        && !string2.isBlank()
                        && !"Unknown".equalsIgnoreCase(string2)
                        && !"CRITICAL".equalsIgnoreCase(string2);
        boolean bl3 = bl2;
        if (bl2) {
            body.text(" (");
            if (bl) {
                body.name(NameFilter.apply(string2));
            } else {
                body.value(string2);
            }
            body.text(")");
        }
        ModuleChat.send((ClientModule) this, body);
    }

    private void upsertDetection(String string, String string2, String string3, int i, long l) {
        Detection detection = this.detections.get(string);
        if (detection == null) {
            this.detections.put(string, new Detection(string2, string3, i, l));
            return;
        }
        detection.name = string2;
        detection.expiresAt = Math.max(detection.expiresAt, l);
        if (i >= detection.score) {
            detection.reason = string3;
            detection.score = i;
        }
    }

    private List<HudEntry> hudSnapshot() {
        long l = System.currentTimeMillis();
        ArrayList<HudEntry> list = new ArrayList<HudEntry>();
        if (l < this.criticalUntilMs) {
            list.add(new HudEntry("CRITICAL", this.criticalWatcher));
        }
        HashSet set = new HashSet();
        this.detections.values().stream()
                .filter(detection -> detection.expiresAt > l)
                .filter(AntiVanishModule::detectionWorthShowing)
                .sorted(
                        Comparator.comparingInt((Detection detection2) -> detection2.score)
                                .reversed()
                                .thenComparing(
                                        detection -> detection.name, String.CASE_INSENSITIVE_ORDER))
                .map(detection -> new HudEntry(detection.name, detection.reason))
                .filter(
                        hudEntry ->
                                set.add(
                                        AntiVanishModule.hudTag(hudEntry)
                                                + "\u0000"
                                                + AntiVanishModule.hudValue(hudEntry)))
                .limit(4L)
                .forEach(list::add);
        return List.copyOf(list);
    }

    private void pruneState() {
        long l = System.currentTimeMillis();
        this.detections.entrySet().removeIf(entry -> ((Detection) entry.getValue()).expiresAt <= l);
        this.signalCooldowns.entrySet().removeIf(entry -> l - (Long) entry.getValue() > 60000L);
        this.announceCooldowns.entrySet().removeIf(entry -> l - (Long) entry.getValue() > 60000L);
        this.selfPlacedBlocks.entrySet().removeIf(entry -> (Long) entry.getValue() <= l);
        this.confirmedDepartures.entrySet().removeIf(entry -> (Long) entry.getValue() <= l);
        this.automatedMechanisms.entrySet().removeIf(entry -> (Long) entry.getValue() <= l);
        this.weakParticleBursts
                .values()
                .removeIf(
                        deque -> {
                            while (!deque.isEmpty() && l - (Long) deque.peekFirst() > 2000L) {
                                deque.removeFirst();
                            }
                            return deque.isEmpty();
                        });
        while (!this.recentExplosions.isEmpty()
                && l - this.recentExplosions.peekFirst().timeMs > 3000L) {
            this.recentExplosions.removeFirst();
        }
        this.pruneSignals(l);
    }

    private void pruneSignals(long l) {
        while (!this.signals.isEmpty() && l - this.signals.peekFirst().timeMs > 15000L) {
            this.signals.removeFirst();
        }
    }

    private boolean nearPlayer(Vec3d vec) {
        return vec != null
                && vec.squaredDistanceTo(AntiVanishModule.MC.player.getEntityPos())
                        <= this.sensorRangeSq();
    }

    private boolean nearSelf(Vec3d vec, double d) {
        return vec != null
                && AntiVanishModule.MC.player != null
                && vec.squaredDistanceTo(AntiVanishModule.MC.player.getEntityPos()) < d;
    }

    private boolean selfContainerActive() {
        return System.currentTimeMillis() - this.lastContainerActivityMs < 2500L;
    }

    private static boolean isContainerSignal(String string) {
        String path = AntiVanishHeuristics.path(string);
        return path.contains("chest") || path.contains("barrel") || path.contains("shulker");
    }

    private String locatedSubject(Vec3d vec) {
        if (vec == null || AntiVanishModule.MC.player == null) {
            return "nearby";
        }
        Vec3d entityPos = AntiVanishModule.MC.player.getEntityPos();
        double d = vec.x - entityPos.x;
        double e = vec.y - entityPos.y;
        double d2 = vec.z - entityPos.z;
        long l = Math.round(Math.sqrt(d * d + e * e + d2 * d2));
        String compass = AntiVanishModule.compass(d, d2);
        return compass.isEmpty() ? l + "m" : l + "m " + compass;
    }

    private static String compass(double d, double e) {
        String string;
        string = e < -1.0 ? "N" : (e > 1.0 ? "S" : "");
        String string2 = d > 1.0 ? "E" : (d < -1.0 ? "W" : "");
        return string + string2;
    }

    private boolean hasVisibleCause(Vec3d vec) {
        if (vec == null) {
            return true;
        }
        for (Entity entity : AntiVanishModule.MC.world.getEntities()) {
            if (entity == AntiVanishModule.MC.player) continue;
            if (entity instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) entity;
                if (player.isInvisible() || !(entity.getEntityPos().squaredDistanceTo(vec) <= 16.0))
                    continue;
            } else if (!(entity instanceof ProjectileEntity)
                    || !(entity.getEntityPos().squaredDistanceTo(vec) <= 16.0)) continue;
            return true;
        }
        return false;
    }

    private boolean isExplosionRelated(Vec3d vec) {
        if (vec == null) {
            return false;
        }
        long l = System.currentTimeMillis();
        while (!this.recentExplosions.isEmpty()
                && l - this.recentExplosions.peekFirst().timeMs > 3000L) {
            this.recentExplosions.removeFirst();
        }
        for (ExplosionEvent recentExplosion : this.recentExplosions) {
            if (!(recentExplosion.center.squaredDistanceTo(vec)
                    <= recentExplosion.radius * recentExplosion.radius)) continue;
            return true;
        }
        return false;
    }

    private boolean hasAmbientParticleSource(Vec3d vec, String string) {
        if (vec == null || !AntiVanishModule.shortId(string).contains("smoke")) {
            return false;
        }
        BlockPos pos = BlockPos.ofFloored(vec);
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                for (int k = -2; k <= 2; ++k) {
                    BlockPos pos2 = pos.add(i, j, k);
                    Identifier id2 =
                            Registries.BLOCK.getId(
                                    AntiVanishModule.MC.world.getBlockState(pos2).getBlock());
                    String shortId = AntiVanishModule.shortId(id2 == null ? "" : id2.toString());
                    if (!shortId.contains("campfire")
                            && !shortId.contains("furnace")
                            && !shortId.contains("smoker")
                            && !shortId.contains("torch")
                            && !shortId.contains("fire")
                            && !shortId.contains("candle")
                            && !shortId.contains("respawn_anchor")) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPoweredMechanism(Vec3d vec, String string) {
        String shortId = AntiVanishModule.shortId(string);
        if (!shortId.contains("door") && !shortId.contains("trapdoor")) {
            return false;
        }
        BlockPos pos = BlockPos.ofFloored(vec);
        long l = System.currentTimeMillis();
        for (int i = -1; i <= 1; ++i) {
            BlockPos pos2 = pos.add(0, i, 0);
            BlockState blockState = AntiVanishModule.MC.world.getBlockState(pos2);
            long l2 = pos2.asLong();
            boolean bl =
                    blockState.contains(Properties.POWERED)
                            && blockState.get(Properties.POWERED) != false;
            boolean bl2 = bl;
            if (bl || AntiVanishModule.MC.world.isReceivingRedstonePower(pos2)) {
                this.automatedMechanisms.put(l2, l + 5000L);
                return true;
            }
            if (this.automatedMechanisms.getOrDefault(l2, 0L) <= l) continue;
            return true;
        }
        return false;
    }

    private double sensorRangeSq() {
        double d = 64.0;
        return d * d;
    }

    private static double parseDouble(String string, double d) {
        try {
            return Double.parseDouble(string);
        } catch (Exception exception) {
            return d;
        }
    }

    private static String soundId(Identifier id2) {
        return id2 == null ? "" : id2.toString();
    }

    private static String shortId(String string) {
        return AntiVanishHeuristics.path(string);
    }

    private void resetRuntime() {
        this.observations.clear();
        this.knownPlayers.clear();
        this.detections.clear();
        this.signalCooldowns.clear();
        this.announceCooldowns.clear();
        this.announceTimes.clear();
        this.selfPlacedBlocks.clear();
        this.confirmedDepartures.clear();
        this.automatedMechanisms.clear();
        this.weakParticleBursts.clear();
        this.signals.clear();
        this.recentExplosions.clear();
        this.pendingVanishes.clear();
        this.hiddenSpells.clear();
        this.shard.clear();
        this.tickCounter = 0;
        this.lastLocalActionMs = 0L;
        this.lastContainerActivityMs = 0L;
        this.lastCriticalMs = 0L;
        this.criticalUntilMs = 0L;
        this.criticalWatcher = "";
        this.pendingChimes.clear();
        this.ghostId = null;
        this.ghostName = null;
        this.recentMessages.clear();
        this.listedSinceMs.clear();
        this.serverSendsLeaveMessages = false;
        this.hints.clear();
        this.heldHints.clear();
        this.tabMoves.clear();
        this.teamChanges.clear();
        this.tabNames.clear();
        this.arrivals.clear();
        this.quietUntilMs = System.currentTimeMillis() + 4000L;
    }

    @Environment(value = EnvType.CLIENT)
    private record Observation(
            ObservationType type,
            UUID profileId,
            int entityId,
            double x,
            double y,
            double z,
            String detail) {

        static Observation tabRemove(UUID uuid2) {
            return new Observation(ObservationType.TAB_REMOVE, uuid2, -1, 0.0, 0.0, 0.0, "");
        }

        static Observation tabHide(UUID uuid2) {
            return new Observation(ObservationType.TAB_HIDE, uuid2, -1, 0.0, 0.0, 0.0, "");
        }

        static Observation gamemode(UUID uuid2, String string) {
            return new Observation(ObservationType.GAMEMODE, uuid2, -1, 0.0, 0.0, 0.0, string);
        }

        static Observation playerLeft(String string) {
            return new Observation(ObservationType.PLAYER_LEFT, null, -1, 0.0, 0.0, 0.0, string);
        }

        static Observation systemChat(String string) {
            return new Observation(ObservationType.SYSTEM_CHAT, null, -1, 0.0, 0.0, 0.0, string);
        }

        static Observation entityMetadata(int i) {
            return new Observation(ObservationType.ENTITY_METADATA, null, i, 0.0, 0.0, 0.0, "");
        }

        static Observation explosion(Vec3d vec, float f) {
            return new Observation(
                    ObservationType.EXPLOSION, null, -1, vec.x, vec.y, vec.z, Float.toString(f));
        }

        static Observation entitySound(int i, String string) {
            return new Observation(ObservationType.ENTITY_SOUND, null, i, 0.0, 0.0, 0.0, string);
        }

        static Observation position(
                ObservationType observationType, double d, double e, double d2, String string) {
            return new Observation(observationType, null, -1, d, e, d2, string);
        }

        Vec3d position() {
            return new Vec3d(this.x, this.y, this.z);
        }
    }

    @Environment(value = EnvType.CLIENT)
    private record Hint(HintKind kind, UUID id, String name, int rank, long atMs) {}

    @Environment(value = EnvType.CLIENT)
    private static enum HintKind {
        ORDER,
        TEAM_UP,
        TEAM_DOWN,
        TAB_IN,
        TAB_OUT
    }

    @Environment(value = EnvType.CLIENT)
    private static enum ObservationType {
        TAB_REMOVE,
        TAB_HIDE,
        PLAYER_LEFT,
        SYSTEM_CHAT,
        ENTITY_METADATA,
        POSITIONAL_SOUND,
        ENTITY_SOUND,
        PARTICLE,
        EXPLOSION,
        GAMEMODE
    }

    @Environment(value = EnvType.CLIENT)
    private record RegionProof(UUID id, int distance) {}

    @Environment(value = EnvType.CLIENT)
    private static final class Detection {

        String name;

        String reason;

        int score;

        long expiresAt;

        Detection(String name2, String reason2, int score2, long expiresAt2) {
            this.name = name2;
            this.reason = reason2;
            this.score = score2;
            this.expiresAt = expiresAt2;
        }
    }

    @Environment(value = EnvType.CLIENT)
    public record HudEntry(String name, String reason) {}

    @Environment(value = EnvType.CLIENT)
    private record KnownPlayer(UUID uuid, String name) {}

    @Environment(value = EnvType.CLIENT)
    private record PendingVanish(UUID uuid, String name, int dueTick) {}

    @Environment(value = EnvType.CLIENT)
    private record Signal(
            SignalType type,
            String subject,
            RegionProof proof,
            String reason,
            int weight,
            long timeMs) {}

    @Environment(value = EnvType.CLIENT)
    private static enum SignalType {
        VANISH,
        INVISIBLE,
        PARTICLE,
        SOUND
    }

    @Environment(value = EnvType.CLIENT)
    private record RecentMessage(String text, long atMs) {}

    @Environment(value = EnvType.CLIENT)
    private static final class HiddenSpell {

        final UUID uuid;

        final String name;

        final boolean spectator;

        final String detail;

        final RegionProof proof;

        final int sinceTick;

        final long sinceMs = System.currentTimeMillis();

        boolean told;

        boolean watched;

        HiddenSpell(
                UUID uuid2,
                String name2,
                boolean spectator2,
                String detail2,
                RegionProof proof2,
                int sinceTick2) {
            this.uuid = uuid2;
            this.name = name2;
            this.spectator = spectator2;
            this.detail = detail2;
            this.proof = proof2;
            this.sinceTick = sinceTick2;
        }

        Text how() {
            return this.spectator
                    ? StaffListModule.switchedTo(ModuleChat.body(), GameMode.SPECTATOR.getId())
                            .build()
                    : ModuleChat.body().danger("vanished").build();
        }
    }

    @Environment(value = EnvType.CLIENT)
    private record ExplosionEvent(Vec3d center, double radius, long timeMs) {}
}
