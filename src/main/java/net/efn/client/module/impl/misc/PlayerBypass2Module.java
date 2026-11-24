package net.efn.client.module.impl.misc;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ChallengeToast;
import net.efn.client.gui.GlintHeadRenderer;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.FoglessLines;
import net.efn.client.util.playerbypass2.SimulationSignals;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.dimension.DimensionTypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class PlayerBypass2Module extends ClientModule {

    private static final float LINE_WIDTH = 2.0F;

    private static final int MARK_COLOR = -11205;

    private static final int MARK_FILL_ALPHA = 34;

    private static final int MEMORY_SECONDS = 45;

    private static final float FALLOFF = 0.3F;

    private static final float MIN_SOURCE = 0.5F;

    private static final int MAX_FOUND = 256;

    private static final int ALERT_SPACING = 4;

    private static final int PIN_SPACING = 1;

    private static final float FROZEN_NEIGHBOUR = 0.5F;

    private static final int REPORT_SOURCES = 4;

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private final NumberSetting sensitivity =
            this.addSetting("sens", new NumberSetting("Sensitivity", 1.0F, 1.0F, 10.0F).step(1.0F));

    private final LongOpenHashSet peaks = new LongOpenHashSet();

    private final Map<RegistryKey<World>, LongLinkedOpenHashSet> foundByDimension = new HashMap<>();

    private LongLinkedOpenHashSet found = new LongLinkedOpenHashSet();

    private int session = -1;

    private final LongOpenHashSet shown = new LongOpenHashSet();

    private final Long2FloatOpenHashMap field = new Long2FloatOpenHashMap();

    private final Long2FloatOpenHashMap raw = new Long2FloatOpenHashMap();

    private int ticks;

    public PlayerBypass2Module() {
        super(
                "player-bypass-2",
                "Player Bypass 2",
                "Finds hidden players from chunks that keep ticking outside your simulation"
                        + " distance.",
                ModuleCategory.BASE_RAID);
    }

    @Override
    protected void onEnable() {
        this.foundByDimension.clear();
        this.found = new LongLinkedOpenHashSet();
        this.session = SimulationSignals.session();
        SimulationSignals.clear();
        this.configure();
        this.on(TickEvent.TYPE, Stage.POST, this::onTick);
    }

    @Override
    protected void onDisable() {
        SimulationSignals.configure(false, 0, 0, true, 30);
        this.peaks.clear();
        this.foundByDimension.clear();
        this.found = new LongLinkedOpenHashSet();
        this.shown.clear();
        this.field.clear();
        this.raw.clear();
    }

    private void configure() {
        SimulationSignals.configure(true, 0, 0, true, 45);
    }

    private static boolean netherOrEnd(World world) {
        RegistryKey registryKey = world.getRegistryKey();
        return registryKey.equals(World.NETHER)
                || registryKey.equals(World.END)
                || world.getDimensionEntry().matchesKey(DimensionTypes.THE_NETHER)
                || world.getDimensionEntry().matchesKey(DimensionTypes.THE_END)
                || !world.getDimension().hasSkyLight();
    }

    private float threshold() {
        return 8.0F * (float) Math.pow(2.0, (5.0 - this.sensitivity.get()) / 2.5);
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            if (this.session != SimulationSignals.session()) {
                this.session = SimulationSignals.session();
                this.foundByDimension.clear();
                this.peaks.clear();
                this.shown.clear();
            }
            this.found =
                    this.foundByDimension.computeIfAbsent(
                            client.world.getRegistryKey(),
                            registryKey -> new LongLinkedOpenHashSet());
            this.configure();
            SimulationSignals.tick();
            if (netherOrEnd(client.world)) {
                SimulationSignals.dropPins();
                this.shown.clear();
            } else {
                this.drainPins();
                if (++this.ticks % 5 == 0) {
                    this.collect(client.player);
                    this.alert();
                    this.refreshShown(client.player);
                }
            }
        }
    }

    private void collect(ClientPlayerEntity player) {
        this.peaks.clear();
        this.field.clear();
        this.raw.clear();
        long now = SimulationSignals.now();
        int simulation = SimulationSignals.simulation();
        int i = player.getBlockPos().getX() >> 4;
        int j = player.getBlockPos().getZ() >> 4;
        int ringStart = SimulationSignals.ringStart();
        int view = SimulationSignals.view();
        int k = 2 * simulation + 1;
        float[] floats = new float[k * k];
        float f = simulation * 1.4143F + 1.0F;
        for (int p = -simulation; p <= simulation; p++) {
            for (int q = -simulation; q <= simulation; q++) {
                floats[(p + simulation) * k + q + simulation] =
                        1.0F - 0.3F * (float) Math.sqrt(p * p + q * q) / f;
            }
        }
        for (Entry long2ObjectEntry : SimulationSignals.chunks().long2ObjectEntrySet()) {
            long longKey = long2ObjectEntry.getLongKey();
            int packedX = ChunkPos.getPackedX(longKey);
            int packedZ;
            int distance =
                    SimulationSignals.distance(
                            packedX, packedZ = ChunkPos.getPackedZ(longKey), i, j);
            float score;
            if (distance >= ringStart
                    && distance <= view
                    && !((score =
                                    ((SimulationSignals.ChunkSignal) long2ObjectEntry.getValue())
                                            .score(now))
                            < 0.5F)) {
                for (int v = -simulation; v <= simulation; v++) {
                    for (int w = -simulation; w <= simulation; w++) {
                        long l = ChunkPos.toLong(packedX + v, packedZ + w);
                        this.field.addTo(l, score * floats[(v + simulation) * k + w + simulation]);
                        this.raw.addTo(l, score);
                    }
                }
            }
        }
        LongIterator longIterator = SimulationSignals.frozen().iterator();
        while (longIterator.hasNext()) {
            long l2 = longIterator.next();
            this.field.remove(l2);
            this.raw.remove(l2);
            int packedX2 = ChunkPos.getPackedX(l2);
            int packedZ2 = ChunkPos.getPackedZ(l2);
            for (int k2 = -1; k2 <= 1; k2++) {
                for (int m2 = -1; m2 <= 1; m2++) {
                    long l3 = ChunkPos.toLong(packedX2 + k2, packedZ2 + m2);
                    if (this.field.containsKey(l3)) {
                        this.field.put(l3, this.field.get(l3) * 0.5F);
                        this.raw.put(l3, this.raw.get(l3) * 0.5F);
                    }
                }
            }
        }
        float threshold = this.threshold();
        for (it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry long2FloatEntry :
                this.field.long2FloatEntrySet()) {
            long longKey2 = long2FloatEntry.getLongKey();
            if (!(this.raw.get(longKey2) < threshold)
                    && this.isPeak(longKey2, long2FloatEntry.getFloatValue(), simulation)) {
                this.peaks.add(longKey2);
            }
        }
    }

    private boolean isPeak(long l, float f, int i) {
        int packedX = ChunkPos.getPackedX(l);
        int packedZ = ChunkPos.getPackedZ(l);
        for (int m = -i; m <= i; m++) {
            for (int n = -i; n <= i; n++) {
                long l2;
                float g;
                if ((m != 0 || n != 0)
                        && ((g = this.field.get(l2 = ChunkPos.toLong(packedX + m, packedZ + n))) > f
                                || g == f && l2 < l)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void drainPins() {
        SimulationSignals.Pin pin;
        while ((pin = SimulationSignals.pollPin()) != null) {
            BlockPos pos = pin.pos();
            long l = ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4);
            LOGGER.info("[Player Bypass 2] {}", pin.detail());
            boolean bl = false;
            LongListIterator longListIterator = this.found.iterator();
            while (longListIterator.hasNext()) {
                long l2 = longListIterator.next();
                if (SimulationSignals.distance(
                                ChunkPos.getPackedX(l2),
                                ChunkPos.getPackedZ(l2),
                                ChunkPos.getPackedX(l),
                                ChunkPos.getPackedZ(l))
                        <= 1) {
                    bl = true;
                    break;
                }
            }
            if (!bl) {
                this.found.add(l);
                while (this.found.size() > 256) {
                    this.found.removeFirstLong();
                }
                ModuleChat.send(this, pin.detail());
                ChallengeToast.addOrUpdate(
                        GlintHeadRenderer.stack(),
                        "Player Activity Detected",
                        Text.literal("at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                        true);
            }
        }
    }

    private void alert() {
        long now = SimulationSignals.now();
        int i = Math.max(4, 2 * SimulationSignals.simulation());
        LongIterator longIterator = this.peaks.iterator();
        while (longIterator.hasNext()) {
            long l = longIterator.next();
            boolean bl = false;
            LongListIterator longListIterator = this.found.iterator();
            while (longListIterator.hasNext()) {
                long l2 = longListIterator.next();
                if (SimulationSignals.distance(
                                ChunkPos.getPackedX(l2),
                                ChunkPos.getPackedZ(l2),
                                ChunkPos.getPackedX(l),
                                ChunkPos.getPackedZ(l))
                        <= i) {
                    bl = true;
                    break;
                }
            }
            if (!bl) {
                this.found.add(l);
                while (this.found.size() > 256) {
                    this.found.removeFirstLong();
                }
                int j = (ChunkPos.getPackedX(l) << 4) + 8;
                int k = (ChunkPos.getPackedZ(l) << 4) + 8;
                this.report(l, j, k, now);
                ChallengeToast.addOrUpdate(
                        GlintHeadRenderer.stack(),
                        "Player Activity Detected",
                        Text.literal("at " + j + ", " + k),
                        true);
            }
        }
    }

    private void refreshShown(ClientPlayerEntity player) {
        this.shown.clear();
        int i = player.getBlockPos().getX() >> 4;
        int j = player.getBlockPos().getZ() >> 4;
        int k = SimulationSignals.view() + SimulationSignals.simulation() + 2;
        LongListIterator longListIterator = this.found.iterator();
        while (longListIterator.hasNext()) {
            long l = longListIterator.next();
            if (SimulationSignals.distance(ChunkPos.getPackedX(l), ChunkPos.getPackedZ(l), i, j)
                    <= k) {
                this.shown.add(l);
            }
        }
    }

    private void report(long l, int i, int j, long l2) {
        int simulation = SimulationSignals.simulation();
        int packedX = ChunkPos.getPackedX(l);
        int packedZ = ChunkPos.getPackedZ(l);
        ArrayList list = new ArrayList();
        for (Entry long2ObjectEntry : SimulationSignals.chunks().long2ObjectEntrySet()) {
            long longKey = long2ObjectEntry.getLongKey();
            if (SimulationSignals.distance(
                                    ChunkPos.getPackedX(longKey),
                                    ChunkPos.getPackedZ(longKey),
                                    packedX,
                                    packedZ)
                            <= simulation
                    && !(((SimulationSignals.ChunkSignal) long2ObjectEntry.getValue()).score(l2)
                            < 0.5F)) {
                list.add(long2ObjectEntry);
            }
        }
        list.sort(
                (entry, entry2) ->
                        Float.compare(
                                ((SimulationSignals.ChunkSignal)
                                                ((java.util.Map.Entry) entry2).getValue())
                                        .score(l2),
                                ((SimulationSignals.ChunkSignal)
                                                ((java.util.Map.Entry) entry).getValue())
                                        .score(l2)));
        ArrayList list2 = new ArrayList();
        list2.add(
                "Found "
                        + i
                        + ", "
                        + j
                        + " from "
                        + list.size()
                        + " chunks (sim "
                        + simulation
                        + ", ring "
                        + SimulationSignals.ringStart()
                        + "-"
                        + SimulationSignals.view()
                        + ")");
        for (int p = 0; p < Math.min(4, list.size()); p++) {
            SimulationSignals.ChunkSignal chunkSignal =
                    (SimulationSignals.ChunkSignal) ((Entry) list.get(p)).getValue();
            long longKey2 = ((Entry) list.get(p)).getLongKey();
            StringBuilder sb = new StringBuilder();
            for (SimulationSignals.Kind kind : SimulationSignals.Kind.values()) {
                if (chunkSignal.count(kind) > 0) {
                    sb.append(sb.isEmpty() ? "" : ", ")
                            .append(kind.title)
                            .append(" x")
                            .append(chunkSignal.count(kind));
                }
            }
            list2.add(
                    " chunk "
                            + ((ChunkPos.getPackedX(longKey2) << 4) + 8)
                            + ", "
                            + ((ChunkPos.getPackedZ(longKey2) << 4) + 8)
                            + String.format(Locale.ROOT, " score %.1f: ", chunkSignal.score(l2))
                            + sb);
            for (String string : chunkSignal.recent()) {
                list2.add("   " + string);
            }
        }
        for (String string2 : (Iterable<String>) list2) {
            LOGGER.info("[Player Bypass 2] {}", string2);
            ModuleChat.send(this, string2);
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null && !netherOrEnd(client.world)) {
            if (!this.shown.isEmpty()) {
                FoglessLines foglessLines = FoglessLines.forFrame(event.partialTick);
                double bottomY = client.world.getBottomY();
                double d = client.world.getTopYInclusive() + 1;
                DrawStyle drawStyle = DrawStyle.filled(ColorHelper.withAlpha(34, -11205));
                LongIterator longIterator = this.shown.iterator();
                while (longIterator.hasNext()) {
                    long l = longIterator.next();
                    double e = ChunkPos.getPackedX(l) << 4;
                    double d2 = ChunkPos.getPackedZ(l) << 4;
                    double e2 = e + 16.0;
                    double d3 = d2 + 16.0;
                    GizmoDrawing.box(new Box(e, bottomY, d2, e2, d, d3), drawStyle)
                            .ignoreOcclusion();
                    column(foglessLines, e, d2, e2, d3, bottomY, d);
                }
            }
        }
    }

    private static void column(
            FoglessLines foglessLines,
            double d,
            double e,
            double d2,
            double e2,
            double d3,
            double e3) {
        double[][] doubleGrid = new double[][] {{d, e}, {d2, e}, {d2, e2}, {d, e2}};
        for (int j = 0; j < doubleGrid.length; j++) {
            double[] doubles = doubleGrid[j];
            double[] doubles2 = doubleGrid[(j + 1) % doubleGrid.length];
            foglessLines.line(
                    new Vec3d(doubles[0], d3, doubles[1]),
                    new Vec3d(doubles[0], e3, doubles[1]),
                    -11205,
                    2.0F);
            foglessLines.line(
                    new Vec3d(doubles[0], d3, doubles[1]),
                    new Vec3d(doubles2[0], d3, doubles2[1]),
                    -11205,
                    2.0F);
            foglessLines.line(
                    new Vec3d(doubles[0], e3, doubles[1]),
                    new Vec3d(doubles2[0], e3, doubles2[1]),
                    -11205,
                    2.0F);
        }
    }
}
