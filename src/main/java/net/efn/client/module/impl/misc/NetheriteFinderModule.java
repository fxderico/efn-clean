package net.efn.client.module.impl.misc;

import imgui.ImDrawList;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.HudOverlayProvider;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.netherite.NetheriteChunkTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class NetheriteFinderModule extends ClientModule implements HudOverlayProvider {

    private static final int MAX_RENDERED_SECTIONS = 128;

    private static final long NOTIFY_COOLDOWN_MS = 2000L;

    private final ColorSetting color =
            this.addSetting("color", new ColorSetting("Near Color", -34304));

    private final ColorSetting farColor =
            this.addSetting("far-color", new ColorSetting("Far Color", -11688961));

    private final NumberSetting fade =
            this.addSetting(
                    "fade-distance",
                    new NumberSetting("Fade Distance", 192.0F, 32.0F, 512.0F).step(8.0F));

    private final NumberSetting range =
            this.addSetting("range", new NumberSetting("Range", 512.0F, 64.0F, 1024.0F).step(1.0F));

    private final BooleanSetting tracers =
            this.addSetting("tracers", new BooleanSetting("Chunk Tracers", true));

    private final NumberSetting tracerCount =
            this.addSetting(
                    "tracer-count", new NumberSetting("Tracer Count", 3.0F, 1.0F, 8.0F).step(1.0F));

    private final BooleanSetting advanced =
            this.addSetting("advanced-finder", new BooleanSetting("Advanced Finder", false));

    private final BooleanSetting showProbes =
            this.addSetting("show-probes", new BooleanSetting("Show Probes", true));

    private final ModeSetting probeStyle =
            this.addSetting(
                    "probe-style",
                    new ModeSetting("Probe Style", "Dots", "Dots", "Cubes", "Pulses"));

    private final NumberSetting missingFrom =
            this.addSetting(
                    "missing-from",
                    new NumberSetting("Mark Missing From %", 60.0F, 30.0F, 100.0F).step(5.0F));

    private final AdvancedFinder advancedFinder = new AdvancedFinder(this);

    private final NetheriteVisuals visuals = new NetheriteVisuals();

    private int pendingCount;

    private BlockPos nearestPending;

    private double nearestPendingDistanceSq;

    private long lastNotifyAt;

    public NetheriteFinderModule() {
        super(
                "netherite-finder",
                "Netherite Finder",
                "Boxes Nether chunk sections hiding ancient debris.",
                ModuleCategory.DONUT);
        this.usesNotifications().set(false);
        this.tracerCount.visibleWhen(this.tracers::get);
        this.showProbes.visibleWhen(this.advanced::get);
        this.probeStyle.visibleWhen(() -> this.advanced.get() && this.showProbes.get());
        this.missingFrom.visibleWhen(() -> this.advanced.get() && this.showProbes.get());
    }

    @Override
    protected void onEnable() {
        this.clearPending();
        this.lastNotifyAt = 0L;
        NetheriteChunkTracker.setListener(this::onCandidate);
        NetheriteChunkTracker.setRevealListener(this::onRevealed);
    }

    @Override
    protected void onDisable() {
        NetheriteChunkTracker.setListener(null);
        NetheriteChunkTracker.setRevealListener(null);
        this.advancedFinder.reset(MinecraftClient.getInstance());
        this.visuals.clear();
    }

    private void onRevealed(BlockPos pos) {
        if (this.advancedFinder.onRevealed(pos)) {
            this.visuals.burst(pos, this.color.argb());
        }
    }

    private void onCandidate(NetheriteChunkTracker.Candidate candidate) {
        if (this.notificationsOn()) {
            BlockPos pos =
                    new BlockPos(
                            (candidate.chunkX() << 4) + 8,
                            (candidate.sectionY() << 4) + 8,
                            (candidate.chunkZ() << 4) + 8);
            this.pendingCount++;
            MinecraftClient client = MinecraftClient.getInstance();
            double d =
                    client.player != null
                            ? client.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ())
                            : 0.0;
            if (this.nearestPending == null || d < this.nearestPendingDistanceSq) {
                this.nearestPending = pos;
                this.nearestPendingDistanceSq = d;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        this.flushNotifications();
        MinecraftClient client = MinecraftClient.getInstance();
        this.advancedFinder.maintain(client);
        if (this.advanced.get()) {
            if (!this.advancedFinder.armed()) {
                this.advancedFinder.arm();
            }
            this.advancedFinder.tick(client);
        } else if (this.advancedFinder.armed()) {
            this.advancedFinder.disarm(client);
        }
    }

    private void flushNotifications() {
        if (this.pendingCount != 0) {
            if (!this.notificationsOn()) {
                this.clearPending();
            } else {
                long l = System.currentTimeMillis();
                if (l - this.lastNotifyAt >= 2000L) {
                    ModuleChat.send(
                            this,
                            ModuleChat.body()
                                    .value(this.pendingCount)
                                    .text(
                                            " new hidden-debris chunk"
                                                    + (this.pendingCount == 1 ? " " : "s "))
                                    .good("found")
                                    .text(", nearest at ")
                                    .coords(
                                            this.nearestPending.getX(),
                                            this.nearestPending.getY(),
                                            this.nearestPending.getZ()));
                    this.clearPending();
                    this.lastNotifyAt = l;
                }
            }
        }
    }

    private void clearPending() {
        this.pendingCount = 0;
        this.nearestPending = null;
        this.nearestPendingDistanceSq = 1.7976931348623157E308;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null
                && client.player != null
                && NetheriteChunkTracker.isNether(client.world)) {
            Vec3d cameraPosVec = client.player.getCameraPosVec(event.partialTick);
            double range2 = this.range.get();
            ArrayList list = new ArrayList();
            for (NetheriteChunkTracker.Candidate candidate : NetheriteChunkTracker.snapshot()) {
                double d;
                double e;
                double d2;
                Box box;
                double e2;
                if (!this.advancedFinder.isDone(candidate)
                        && (e2 =
                                        Math.sqrt(
                                                distanceSq(
                                                        box =
                                                                new Box(
                                                                        d2 =
                                                                                candidate.chunkX()
                                                                                        * 16.0,
                                                                        e =
                                                                                candidate.sectionY()
                                                                                        * 16.0,
                                                                        d =
                                                                                candidate.chunkZ()
                                                                                        * 16.0,
                                                                        d2 + 16.0,
                                                                        e + 16.0,
                                                                        d + 16.0),
                                                        cameraPosVec)))
                                <= range2) {
                    list.add(
                            new NetheriteVisuals.Section(
                                    box, e2, this.advancedFinder.progress(candidate)));
                }
            }
            list.sort(Comparator.comparingDouble(NetheriteVisuals.Section::distance));
            if (list.size() > 128) {
                list = new ArrayList(list.subList(0, 128));
            }
            NetheriteVisuals.Options options =
                    new NetheriteVisuals.Options(
                            this.color.argb(),
                            this.farColor.argb(),
                            this.fade.get(),
                            range2,
                            this.probeStyle.get(),
                            this.missingFrom.get() / 100.0F,
                            this.tracers.get() ? this.tracerCount.getInt() : 0,
                            this.showProbes.get());
            this.visuals.render(
                    client,
                    event.partialTick,
                    cameraPosVec,
                    list,
                    List.copyOf(this.advancedFinder.revealed()),
                    this.advancedFinder,
                    options);
        } else {
            this.visuals.clear();
        }
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled() && this.visuals.hasLabels();
    }

    @Override
    public void renderHudOverlay(ImDrawList imDrawList, float f, float g) {
        this.visuals.overlay(imDrawList, f, g);
    }

    private static double distanceSq(Box box, Vec3d vec) {
        double d = Math.max(0.0, Math.max(box.minX - vec.x, vec.x - box.maxX));
        double e = Math.max(0.0, Math.max(box.minY - vec.y, vec.y - box.maxY));
        double d2 = Math.max(0.0, Math.max(box.minZ - vec.z, vec.z - box.maxZ));
        return d * d + e * e + d2 * d2;
    }
}
