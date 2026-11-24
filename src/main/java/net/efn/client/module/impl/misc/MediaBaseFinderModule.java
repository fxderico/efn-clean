/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.misc;

import imgui.ImDrawList;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.PacketSendEvent;
import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.HudPanel;
import net.efn.client.gui.ScreenText;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.NameFilter;
import net.efn.client.util.donut.DonutGoliaths;
import net.efn.client.util.staff.ShardTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ChatCommandSignedC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Environment(value = EnvType.CLIENT)
public final class MediaBaseFinderModule extends ClientModule implements HudOverlayProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final String MEDIA_PREFIX = "\ud83d\udcf9";

    private static final String MEDIA_TEAM = "09-";

    private static final int SETTLE_TICKS = 60;

    private static final int SCAN_TICKS = 10;

    private static final int MAX_LISTED = 8;

    private static final long RTP_WINDOW_NANOS = 60000000000L;

    private final BooleanSetting reportEmpty =
            this.addSetting("report-empty", new BooleanSetting("Report Empty Goliaths", true));

    private final BooleanSetting statusBox =
            this.addSetting("status-box", new BooleanSetting("Status Box", true));

    private final NumberSetting scale =
            this.addSetting("scale", new NumberSetting("Scale", 1.0f, 0.5f, 3.0f).step(0.05f));

    private final NumberSetting opacity = this.addSetting("opacity", HudPanel.opacitySetting());

    private final HudPanel panel =
            new HudPanel(
                    this.addSetting("pos-x", HudPanel.coordinate("X")),
                    this.addSetting("pos-y", HudPanel.coordinate("Y")),
                    this.scale,
                    this.opacity);

    private final ShardTracker shard = new ShardTracker();

    private final ScreenText screenText = new ScreenText(0xFF55FF, 3.0f);

    private final Queue<UUID> modeUpdates = new ConcurrentLinkedQueue<UUID>();

    private final Set<String> proven = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    private Set<String> here = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    private String goliath = "\u2014";

    private int settle;

    private int ticks;

    private boolean reported;

    private boolean afterRtp;

    private volatile long rtpAt;

    public MediaBaseFinderModule() {
        super(
                "media-base-finder",
                "Media Base Finder",
                "Tells you which DonutSMP media are on the goliath you land on (after /rtp), where"
                        + " their base most likely is.",
                ModuleCategory.BASE_RAID);
        this.usesNotifications();
        this.scale.visibleWhen(this.statusBox::get);
        this.opacity.visibleWhen(this.statusBox::get);
    }

    @Override
    protected void onEnable() {
        this.arrive(MinecraftClient.getInstance());
    }

    @Override
    protected void onDisable() {
        this.shard.clear();
        this.modeUpdates.clear();
        this.proven.clear();
        this.here = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    }

    private void arrive(MinecraftClient client) {
        this.modeUpdates.clear();
        this.proven.clear();
        this.here = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        this.settle = 60;
        this.reported = false;
        this.goliath = MediaBaseFinderModule.label(client);
        long rtpAt2 = this.rtpAt;
        this.afterRtp = rtpAt2 != 0L && System.nanoTime() - rtpAt2 < 60000000000L;
    }

    @EventHandler
    public void onPacketSend(PacketSendEvent event) {
        String command;
        Object object;
        Packet<?> packet = event.packet;
        if (packet instanceof CommandExecutionC2SPacket) {
            object = (CommandExecutionC2SPacket) packet;
            command = ((CommandExecutionC2SPacket) object).command();
        } else {
            packet = event.packet;
            if (packet instanceof ChatCommandSignedC2SPacket) {
                object = (ChatCommandSignedC2SPacket) packet;
                command = ((ChatCommandSignedC2SPacket) object).command();
            } else {
                command = null;
            }
        }
        if (command == null) {
            return;
        }
        object = command.trim().toLowerCase(Locale.ROOT);
        if (((String) object).equals("rtp") || ((String) object).startsWith("rtp ")) {
            this.rtpAt = System.nanoTime();
        }
    }

    @EventHandler(stage = Stage.POST)
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        ClientPlayerEntity player = client.player;
        if (networkHandler == null || player == null || client.world == null) {
            this.shard.clear();
            return;
        }
        if (this.shard.moved(client)) {
            this.arrive(client);
        }
        this.drainModeUpdates(networkHandler);
        if (this.settle > 0) {
            --this.settle;
            return;
        }
        if (this.reported && ++this.ticks % 10 != 0) {
            return;
        }
        Set<String> set = this.scan(client, networkHandler, player);
        this.goliath = MediaBaseFinderModule.label(client);
        if (!this.reported) {
            this.reported = true;
            this.here = set;
            this.report(set);
            return;
        }
        ArrayList<String> list = new ArrayList<String>();
        for (String string : set) {
            if (this.here.contains(string)) continue;
            list.add(string);
        }
        this.here = set;
        if (!list.isEmpty()) {
            this.announce(list, set.size(), false);
        }
    }

    private Set<String> scan(
            MinecraftClient client,
            ClientPlayNetworkHandler clientPlayNetworkHandler,
            ClientPlayerEntity player) {
        TreeSet<String> treeSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        String string = player.getGameProfile().name();
        for (Team team : client.world.getScoreboard().getTeams()) {
            if (!MediaBaseFinderModule.isMedia(team)) continue;
            for (String player2 : team.getPlayerList()) {
                if (player2.equalsIgnoreCase(string)
                        || clientPlayNetworkHandler.getPlayerListEntry(player2) != null
                                && !this.proven.contains(player2)) continue;
                treeSet.add(player2);
            }
        }
        return treeSet;
    }

    private static boolean isMedia(Team team) {
        return team.getName().startsWith(MEDIA_TEAM)
                || team.getPrefix().getString().contains(MEDIA_PREFIX);
    }

    @EventHandler
    public void onPacketReceiveNetty(PacketReceiveNettyEvent event) {
        this.collect(event.packet);
    }

    private void collect(Packet<?> packet) {
        if (packet instanceof BundleS2CPacket) {
            BundleS2CPacket bundlePacket = (BundleS2CPacket) packet;
            for (Packet packet2 : bundlePacket.getPackets()) {
                this.collect(packet2);
            }
            return;
        }
        if (!(packet instanceof PlayerListS2CPacket)) {
            return;
        }
        PlayerListS2CPacket playerListPacket = (PlayerListS2CPacket) packet;
        if (!playerListPacket
                        .getActions()
                        .contains((Object) PlayerListS2CPacket.Action.UPDATE_GAME_MODE)
                || playerListPacket
                        .getActions()
                        .contains((Object) PlayerListS2CPacket.Action.ADD_PLAYER)) {
            return;
        }
        for (PlayerListS2CPacket.Entry entry : playerListPacket.getEntries()) {
            if (entry == null || entry.profileId() == null) continue;
            this.modeUpdates.offer(entry.profileId());
        }
    }

    private void drainModeUpdates(ClientPlayNetworkHandler clientPlayNetworkHandler) {
        UUID modeUpdate;
        while ((modeUpdate = this.modeUpdates.poll()) != null) {
            String string;
            PlayerListEntry playerListEntry =
                    clientPlayNetworkHandler.getPlayerListEntry(modeUpdate);
            if (playerListEntry == null
                    || playerListEntry.getScoreboardTeam() == null
                    || !MediaBaseFinderModule.isMedia(playerListEntry.getScoreboardTeam())
                    || !this.proven.add(string = playerListEntry.getProfile().name())) continue;
            LOGGER.info(
                    "[Media Base Finder] game mode update places {} on {}",
                    (Object) string,
                    (Object) this.goliath);
        }
    }

    private void report(Set<String> set) {
        this.rtpAt = 0L;
        LOGGER.info("[Media Base Finder] {}: {} media {}", this.goliath, set.size(), set);
        if (set.isEmpty()) {
            if (this.reportEmpty.get()) {
                ModuleChat.send(
                        (ClientModule) this,
                        ModuleChat.body().text("No media on ").name(this.goliath).text("."));
            }
            return;
        }
        this.announce(new ArrayList<String>(set), set.size(), true);
    }

    private void announce(List<String> list, int i, boolean bl) {
        MinecraftClient client = MinecraftClient.getInstance();
        String names = MediaBaseFinderModule.names(list);
        String area = MediaBaseFinderModule.area(client);
        String string =
                i
                        + (i == 1 ? " media base" : " media bases")
                        + (String) (area == null ? "" : " within " + area + " blocks");
        if (bl) {
            ModuleChat.send(
                    (ClientModule) this,
                    ModuleChat.body()
                            .text(string + " on ")
                            .name(this.goliath)
                            .text(": ")
                            .name(names));
        } else {
            ModuleChat.send(
                    (ClientModule) this,
                    ModuleChat.body()
                            .name(names)
                            .text(" joined ")
                            .name(this.goliath)
                            .text(", now " + string + "."));
            LOGGER.info("[Media Base Finder] {} joined {}, now {}", names, this.goliath, i);
        }
        if (bl) {
            LOGGER.info(
                    "[Media Base Finder] screen text: afterRtp={} notifications={}",
                    (Object) this.afterRtp,
                    (Object) this.notificationsOn());
        }
        if (!(bl && this.afterRtp && this.notificationsOn())) {
            return;
        }
        this.screenText.show(
                i + (i == 1 ? " Media Base" : " Media Bases"),
                (String) (area == null ? "on this goliath" : "within " + area + " blocks"));
        client.getSoundManager()
                .play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.4f));
    }

    private static String area(MinecraftClient client) {
        String blocks;
        if (client.player == null
                || client.world == null
                || client.world.getRegistryKey() != World.OVERWORLD) {
            return null;
        }
        DonutGoliaths.Goliath goliath2 =
                DonutGoliaths.at(client.player.getX(), client.player.getZ());
        if (goliath2 == null) {
            return null;
        }
        int[] ints = DonutGoliaths.CELLS[goliath2.region()][goliath2.index()];
        String blocks2 = MediaBaseFinderModule.blocks(ints[2] - ints[0]);
        return blocks2.equals(blocks = MediaBaseFinderModule.blocks(ints[3] - ints[1]))
                ? blocks2
                : blocks2 + " \u00d7 " + blocks;
    }

    private static String blocks(int i) {
        int j = i * 12500;
        return j % 1000 == 0
                ? j / 1000 + "k"
                : String.format(Locale.ROOT, "%.1fk", (double) j / 1000.0);
    }

    private static String names(List<String> list) {
        StringBuilder sb = new StringBuilder();
        int j = Math.min(list.size(), 8);
        for (int i = 0; i < j; ++i) {
            sb.append(i == 0 ? "" : ", ").append(NameFilter.apply(list.get(i)));
        }
        if (list.size() > j) {
            sb.append(" +").append(list.size() - j);
        }
        return sb.toString();
    }

    private static String label(MinecraftClient client) {
        if (client.player == null
                || client.world == null
                || client.world.getRegistryKey() != World.OVERWORLD) {
            return "\u2014";
        }
        DonutGoliaths.Goliath goliath2 =
                DonutGoliaths.at(client.player.getX(), client.player.getZ());
        return goliath2 == null ? "Off the map" : goliath2.label();
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled()
                && this.statusBox.get()
                && MinecraftClient.getInstance().player != null;
    }

    @Override
    public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
        ArrayList<HudPanel.Line> list = new ArrayList<HudPanel.Line>();
        if (this.settle > 0) {
            list.add(new HudPanel.Line("Checking\u2026", -19648));
        } else if (this.here.isEmpty()) {
            list.add(new HudPanel.Line("None here", -6052949));
        } else {
            int i = 0;
            for (String string : this.here) {
                if (i++ == 8) {
                    list.add(new HudPanel.Line("+" + (this.here.size() - 8) + " more", -6052949));
                    break;
                }
                list.add(
                        new HudPanel.Line(
                                HudPanel.face(string), NameFilter.apply(string), -1, "", -6052949));
            }
        }
        String string2 =
                this.here.size() > 1 ? "MEDIA BASES ON YOUR GOLIATH" : "MEDIA BASE ON YOUR GOLIATH";
        this.panel.card(imDrawList, string2, "", -1, list, "staff_list", f, g, 1.0f, 220.0f);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        this.screenText.render(event.graphics);
    }
}
