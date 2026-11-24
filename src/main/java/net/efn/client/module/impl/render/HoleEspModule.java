package net.efn.client.module.impl.render;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.PerformanceBudget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Environment(EnvType.CLIENT)
public class HoleEspModule extends ClientModule {

    private static final String MODE_ALL = "All";

    private static final String MODE_HOLES_TUNNELS = "Holes + Tunnels";

    private static final String MODE_HOLES_STAIRS = "Holes + Stairs";

    private static final String MODE_TUNNELS_STAIRS = "Tunnels + Stairs";

    private static final String MODE_HOLES = "Holes";

    private static final String MODE_TUNNELS = "Tunnels";

    private static final String MODE_STAIRS = "Stairs";

    private static final int TYPE_HOLE = 0;

    private static final int TYPE_HOLE_3X1 = 1;

    private static final int TYPE_TUNNEL = 2;

    private static final int TYPE_STAIR = 3;

    private static final int MAX_HITS_PER_CHUNK = 1024;

    private static final int MAX_RENDER_BOXES = 8192;

    private static final int HOLE_GRID_W = 20;

    private static final int ELYTRA_REFRESH_TICKS = 5;

    private static final int NORMAL_REFRESH_TICKS = 20;

    private static final float ELYTRA_SCAN_RADIUS = 12.0F;

    private static final float ELYTRA_SCAN_BUDGET_MS = 8.0F;

    private static final int[][] HORIZONTAL_DIRS = new int[][] {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};

    private static final ThreadLocal<Mutable> PASSABLE_POS = ThreadLocal.withInitial(Mutable::new);

    private final ConcurrentHashMap<Long, List<HoleEspModule.Hit>> chunkCache =
            new ConcurrentHashMap<>();

    private final Queue<Long> scanQueue = new ConcurrentLinkedQueue<>();

    private final Set<Long> queued = ConcurrentHashMap.newKeySet();

    private long lastPlayerChunk = -9223372036854775808L;

    private int refreshTicks = 0;

    private ClientWorld sessionLevel;

    private boolean lastOptimized;

    private HoleEspModule.FastHoleScan activeScan;

    private boolean[] passableBuffer = new boolean[0];

    private final ModeSetting sMode =
            this.addSetting(
                    "detection",
                    new ModeSetting(
                            "Detection",
                            "Holes",
                            "Holes",
                            "All",
                            "Holes + Tunnels",
                            "Holes + Stairs",
                            "Tunnels + Stairs",
                            "Tunnels",
                            "Stairs"));

    private final NumberSetting sScanRadius =
            this.addSetting(
                    "scan-radius", new NumberSetting("Scan Radius", 8.0F, 1.0F, 24.0F).step(1.0F));

    private final NumberSetting sChunksPerTick =
            this.addSetting(
                    "chunks-tick",
                    new NumberSetting("Chunks/Tick", 64.0F, 1.0F, 100.0F).step(1.0F));

    private final NumberSetting sScanBudgetMs =
            this.addSetting(
                    "scan-budget-ms",
                    new NumberSetting("Scan Budget ms", 3.0F, 1.0F, 10.0F).step(1.0F));

    private final BooleanSetting sAirOnly =
            this.addSetting("only-air-blocks", new BooleanSetting("Only Air Blocks", true));

    private final NumberSetting sMinYOffset =
            this.addSetting(
                    "min-y-offset",
                    new NumberSetting("Min Y Offset", 0.0F, 0.0F, 319.0F).step(1.0F));

    private final NumberSetting sMaxYOffset =
            this.addSetting(
                    "max-y-offset",
                    new NumberSetting("Max Y Offset", 0.0F, 0.0F, 319.0F).step(1.0F));

    private final NumberSetting sMinHoleDepth =
            this.addSetting(
                    "min-hole-depth",
                    new NumberSetting("Min Hole Depth", 4.0F, 1.0F, 20.0F).step(1.0F));

    private final NumberSetting sMinTunnelLength =
            this.addSetting(
                    "min-tunnel-length",
                    new NumberSetting("Min Tunnel Length", 3.0F, 1.0F, 20.0F).step(1.0F));

    private final NumberSetting sMinTunnelHeight =
            this.addSetting(
                    "min-tunnel-height",
                    new NumberSetting("Min Tunnel Height", 2.0F, 1.0F, 10.0F).step(1.0F));

    private final NumberSetting sMaxTunnelHeight =
            this.addSetting(
                    "max-tunnel-height",
                    new NumberSetting("Max Tunnel Height", 3.0F, 2.0F, 10.0F).step(1.0F));

    private final NumberSetting sMinStairLength =
            this.addSetting(
                    "min-stair-length",
                    new NumberSetting("Min Stair Length", 3.0F, 1.0F, 20.0F).step(1.0F));

    private final NumberSetting sMinStairHeight =
            this.addSetting(
                    "min-stair-height",
                    new NumberSetting("Min Stair Height", 3.0F, 2.0F, 10.0F).step(1.0F));

    private final NumberSetting sMaxStairHeight =
            this.addSetting(
                    "max-stair-height",
                    new NumberSetting("Max Stair Height", 5.0F, 2.0F, 10.0F).step(1.0F));

    private final BooleanSetting sFill = this.addSetting("fill", new BooleanSetting("Fill", true));

    private final BooleanSetting sOutline =
            this.addSetting("outline", new BooleanSetting("Outline", true));

    private final NumberSetting sLineWidth =
            this.addSetting(
                    "line-width", new NumberSetting("Line Width", 1.5F, 0.5F, 5.0F).step(0.5F));

    private final ColorSetting sHoleLine =
            this.addSetting("1x1-line", new ColorSetting("1x1 Line", 1291845631));

    private final ColorSetting sHoleFill =
            this.addSetting("1x1-fill", new ColorSetting("1x1 Fill", 1342177279));

    private final ColorSetting sHole3Line =
            this.addSetting("3x1-line", new ColorSetting("3x1 Line", 644245094));

    private final ColorSetting sHole3Fill =
            this.addSetting("3x1-fill", new ColorSetting("3x1 Fill", 644245094));

    private final ColorSetting sTunnelLine =
            this.addSetting("tunnel-line", new ColorSetting("Tunnel Line", 1593835775));

    private final ColorSetting sTunnelFill =
            this.addSetting("tunnel-fill", new ColorSetting("Tunnel Fill", 503316735));

    private final ColorSetting sStairLine =
            this.addSetting("stair-line", new ColorSetting("Stair Line", 1610547455));

    private final ColorSetting sStairFill =
            this.addSetting("stair-fill", new ColorSetting("Stair Fill", 520028415));

    public HoleEspModule() {
        super(
                "hole-esp",
                "Hole ESP",
                "Highlights holes, tunnels, and staircases.",
                ModuleCategory.RENDER);
        this.sAirOnly.visibleWhen(
                () -> this.currentMode().tunnels() || this.currentMode().stairs());
        List.of(
                        this.sMode,
                        this.sScanRadius,
                        this.sAirOnly,
                        this.sMinYOffset,
                        this.sMaxYOffset,
                        this.sMinHoleDepth,
                        this.sMinTunnelLength,
                        this.sMinTunnelHeight,
                        this.sMaxTunnelHeight,
                        this.sMinStairLength,
                        this.sMinStairHeight,
                        this.sMaxStairHeight)
                .forEach(setting -> setting.onChange(this::resetScan));
    }

    @Override
    protected void onEnable() {
        this.resetScan();
    }

    private void resetScan() {
        this.lastPlayerChunk = -9223372036854775808L;
        this.refreshTicks = 0;
        this.chunkCache.clear();
        this.scanQueue.clear();
        this.queued.clear();
        this.activeScan = null;
    }

    @Override
    protected void onDisable() {
        this.resetScan();
        this.sessionLevel = null;
        this.passableBuffer = new boolean[0];
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean optimizationsEnabled = PerformanceModule.optimizationsEnabled();
        if (this.lastOptimized != optimizationsEnabled || this.sessionLevel != client.world) {
            this.resetScan();
        }
        this.lastOptimized = optimizationsEnabled;
        this.sessionLevel = client.world;
        if (client.world != null && client.player != null) {
            int i = client.player.getBlockX() >> 4;
            int j = client.player.getBlockZ() >> 4;
            long l = ChunkPos.toLong(i, j);
            int scanRadius = this.scanRadius(client);
            boolean fastTravel = isFastTravel(client);
            boolean bl = ++this.refreshTicks >= (fastTravel ? 5 : 20);
            if (l != this.lastPlayerChunk || bl) {
                this.refreshTicks = 0;
                this.lastPlayerChunk = l;
                this.chunkCache
                        .keySet()
                        .removeIf(
                                long_ -> {
                                    int chunkX = chunkX(long_);
                                    int chunkZ = chunkZ(long_);
                                    return Math.abs(chunkX - i) > scanRadius
                                            || Math.abs(chunkZ - j) > scanRadius;
                                });
                ArrayList list = new ArrayList();
                Vec3d velocity = client.player.getVelocity();
                double d = velocity.x;
                double e = velocity.z;
                double d2 = Math.sqrt(d * d + e * e);
                if (d2 > 1.0E-4) {
                    d /= d2;
                    e /= d2;
                }
                for (int o = i - scanRadius; o <= i + scanRadius; o++) {
                    for (int p = j - scanRadius; p <= j + scanRadius; p++) {
                        long l2 = ChunkPos.toLong(o, p);
                        if (!this.chunkCache.containsKey(l2)
                                && !this.queued.contains(l2)
                                && (this.activeScan == null || this.activeScan.key != l2)
                                && client.world.getChunkManager().isChunkLoaded(o, p)) {
                            int k = o - i;
                            int m = p - j;
                            long l3 = (k * k + m * m) * 1024L;
                            long l4 = fastTravel ? Math.round((k * d + m * e) * 1536.0) : 0L;
                            list.add(new long[] {l2, l3 - l4});
                        }
                    }
                }
                list.sort(Comparator.comparingLong((long[] longs) -> longs[1]));
                for (long[] longs2 : (Iterable<long[]>) list) {
                    this.scanQueue.add(longs2[0]);
                    this.queued.add(longs2[0]);
                }
            }
            int n = Math.max(1, Math.round(value(this.sChunksPerTick, 4.0F)));
            float f =
                    fastTravel && !optimizationsEnabled
                            ? Math.max(value(this.sScanBudgetMs, 3.0F), 8.0F)
                            : value(this.sScanBudgetMs, 3.0F);
            long l5 = (long) (Math.max(1.0F, f) * 1000000.0F);
            long l6 = optimizationsEnabled ? PerformanceBudget.acquire(l5) : System.nanoTime() + l5;
            long l7 = System.nanoTime();
            if (l7 < l6) {
                try {
                    int q = 0;
                    while (q < n && System.nanoTime() < l6) {
                        if (this.activeScan != null) {
                            if (!this.activeScan.validFor(client.world, i, j, scanRadius)) {
                                int r = this.activeScan.chunk.getPos().x;
                                int t = this.activeScan.chunk.getPos().z;
                                long key2 = this.activeScan.key;
                                this.activeScan = null;
                                if (Math.abs((long) r - i) <= scanRadius
                                        && Math.abs((long) t - j) <= scanRadius
                                        && client.world.getChunkManager().isChunkLoaded(r, t)
                                        && this.queued.add(key2)) {
                                    this.scanQueue.add(key2);
                                }
                            } else if (this.activeScan.step()) {
                                this.storeHits(this.activeScan.key, this.activeScan.hits);
                                this.activeScan = null;
                                q++;
                            }
                        } else {
                            Long scan = this.scanQueue.poll();
                            if (scan == null) {
                                break;
                            }
                            this.queued.remove(scan);
                            int chunkX2 = chunkX(scan);
                            int chunkZ2 = chunkZ(scan);
                            if (client.world.getChunkManager().isChunkLoaded(chunkX2, chunkZ2)) {
                                this.scanChunk(client.world, chunkX2, chunkZ2, scan);
                            }
                            if (this.activeScan == null) {
                                q++;
                            }
                        }
                    }
                } finally {
                    if (optimizationsEnabled) {
                        PerformanceBudget.record(l7);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPacketApplied(PacketAppliedEvent event) {
        if (event.packet instanceof ChunkDataS2CPacket chunkDataPacket) {
            this.invalidateAround(chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ());
        }
    }

    private void invalidateAround(int i, int j) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            int k = this.scanRadius(client) + 1;
            if (Math.abs((long) i - (client.player.getBlockX() >> 4)) <= k
                    && Math.abs((long) j - (client.player.getBlockZ() >> 4)) <= k) {
                for (int m = i - 1; m <= i + 1; m++) {
                    for (int n = j - 1; n <= j + 1; n++) {
                        long l = ChunkPos.toLong(m, n);
                        boolean bl = this.chunkCache.containsKey(l);
                        if (this.activeScan != null && this.activeScan.key == l) {
                            this.activeScan = null;
                            bl = true;
                        }
                        if (bl && this.queued.add(l)) {
                            this.scanQueue.add(l);
                        }
                    }
                }
                this.lastPlayerChunk = -9223372036854775808L;
            }
        }
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent event) {
        if (event.pos != null) {
            this.invalidateAround(event.pos.getX() >> 4, event.pos.getZ() >> 4);
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            boolean bl = this.sFill == null || this.sFill.value().get();
            boolean bl2 = this.sOutline == null || this.sOutline.value().get();
            if (bl || bl2) {
                int i = client.player.getBlockX() >> 4;
                int j = client.player.getBlockZ() >> 4;
                int scanRadius = this.scanRadius(client);
                float value = value(this.sLineWidth, 1.5F);
                int k = 0;
                for (Entry entry : this.chunkCache.entrySet()) {
                    long key2 = (Long) entry.getKey();
                    if (Math.abs(chunkX(key2) - i) <= scanRadius
                            && Math.abs(chunkZ(key2) - j) <= scanRadius) {
                        for (HoleEspModule.Hit hit :
                                (Iterable<HoleEspModule.Hit>) (List) entry.getValue()) {
                            if (++k > 8192) {
                                return;
                            }
                            DrawStyle drawStyle =
                                    !bl
                                            ? DrawStyle.stroked(this.lineColor(hit.type()), value)
                                            : (!bl2
                                                    ? DrawStyle.filled(this.fillColor(hit.type()))
                                                    : DrawStyle.filledAndStroked(
                                                            this.lineColor(hit.type()),
                                                            value,
                                                            this.fillColor(hit.type())));
                            GizmoDrawing.box(hit.box(), drawStyle).ignoreOcclusion();
                        }
                    }
                }
            }
        }
    }

    private void scanChunk(ClientWorld world, int i, int j, long l) {
        WorldChunk worldChunk = world.getChunkManager().getWorldChunk(i, j);
        if (worldChunk != null) {
            int k = i << 4;
            int m = j << 4;
            int minY2 = world.getDimension().minY();
            int n = minY2 + worldChunk.getSectionArray().length * 16;
            int o = Math.min(n - 1, minY2 + Math.round(value(this.sMinYOffset, 0.0F)));
            int p = Math.max(o + 1, n - Math.round(value(this.sMaxYOffset, 0.0F)));
            HoleEspModule.Mode mode = this.currentMode();
            boolean bl = this.sAirOnly != null && this.sAirOnly.value().get();
            if (mode.holes() && !mode.tunnels() && !mode.stairs()) {
                if (PerformanceModule.optimizationsEnabled()) {
                    this.activeScan =
                            new HoleEspModule.FastHoleScan(this, worldChunk, l, k, m, o, p);
                } else {
                    this.storeHits(l, this.holeHits(world, k, m, o, p));
                }
            } else {
                List<HoleEspModule.Hit> list =
                        mode.holes() ? this.holeHits(world, k, m, o, p) : new ArrayList();
                HashSet set = new HashSet();
                Mutable mutable = new Mutable();
                ChunkSection[] sectionArray = worldChunk.getSectionArray();
                for (int r = 0; r < sectionArray.length; r++) {
                    ChunkSection section = sectionArray[r];
                    if (section != null && !section.isEmpty()) {
                        int q = minY2 + r * 16;
                        for (int u = 0; u < 16; u++) {
                            int t = q + u;
                            if (t > o && t < p) {
                                for (int w = 0; w < 16; w++) {
                                    for (int i2 = 0; i2 < 16; i2++) {
                                        int v = k + i2;
                                        int j2 = m + w;
                                        mutable.set(v, t, j2);
                                        if (isPassable(world, mutable, bl)) {
                                            if (mode.tunnels()) {
                                                this.checkTunnelStarts(
                                                        world,
                                                        mutable,
                                                        bl,
                                                        (List<HoleEspModule.Hit>) list,
                                                        set);
                                            }
                                            if (mode.stairs()) {
                                                this.checkStaircaseStarts(
                                                        world,
                                                        mutable,
                                                        bl,
                                                        (List<HoleEspModule.Hit>) list,
                                                        set);
                                            }
                                            if (list.size() >= 1024) {
                                                this.chunkCache.put(
                                                        l,
                                                        List.copyOf(
                                                                (Collection<
                                                                                ? extends
                                                                                        HoleEspModule
                                                                                                .Hit>)
                                                                        list));
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                this.storeHits(l, (List<HoleEspModule.Hit>) list);
            }
        }
    }

    private void storeHits(long l, List<HoleEspModule.Hit> list) {
        this.chunkCache.put(l, List.copyOf(list));
    }

    private List<HoleEspModule.Hit> holeHits(ClientWorld world, int i, int j, int k, int m) {
        ArrayList list = new ArrayList();
        int n = k - 1;
        int o = m - n + 1;
        if (o <= 1) {
            return list;
        } else {
            boolean[] booleans = new boolean[o * 20 * 20];
            Mutable mutable = new Mutable();
            for (int p = 0; p < o; p++) {
                int q = n + p;
                int r = p * 20 * 20;
                for (int t = -1; t <= 18; t++) {
                    int u = r + (t + 1) * 20;
                    for (int v = -1; v <= 18; v++) {
                        mutable.set(i + v, q, j + t);
                        booleans[u + v + 1] = isOpen(world, mutable);
                    }
                }
            }
            int w = Math.round(value(this.sMinHoleDepth, 4.0F));
            HashSet set = new HashSet();
            for (int i2 = 0; i2 < 256 && list.size() < 1024; i2++) {
                shaftsInColumn(booleans, i2 & 15, i2 >> 4, n, k, m, w, i, j, list, set);
            }
            return list;
        }
    }

    private static void shaftsInColumn(
            boolean[] booleans,
            int i,
            int j,
            int k,
            int m,
            int n,
            int o,
            int p,
            int q,
            List<HoleEspModule.Hit> list,
            Set<String> set) {
        int r = p + i;
        int t = q + j;
        int u = 0;
        int v = 0;
        int w = 0;
        for (int i2 = m; i2 <= n; i2++) {
            boolean bl = i2 < n;
            u =
                    bl && walled1x1(booleans, i2, i, j, k)
                            ? ++u
                            : endShaft(u, i2, o, list, set, r, t, 1, 1, 0);
            v =
                    bl && walled3x1X(booleans, i2, i, j, k)
                            ? ++v
                            : endShaft(v, i2, o, list, set, r, t, 3, 1, 1);
            if (bl && walled3x1Z(booleans, i2, i, j, k)) {
                w++;
            } else {
                w = endShaft(w, i2, o, list, set, r, t, 1, 3, 1);
            }
        }
    }

    private static int endShaft(
            int i,
            int j,
            int k,
            List<HoleEspModule.Hit> list,
            Set<String> set,
            int m,
            int n,
            int o,
            int p,
            int q) {
        if (i >= k) {
            addHit(list, set, new Box(m, j - i, n, m + o, j, n + p), q);
        }
        return 0;
    }

    private static boolean walled1x1(boolean[] booleans, int i, int j, int k, int m) {
        return grid(booleans, i, j, k, m)
                && !grid(booleans, i, j - 1, k, m)
                && !grid(booleans, i, j + 1, k, m)
                && !grid(booleans, i, j, k - 1, m)
                && !grid(booleans, i, j, k + 1, m);
    }

    private static boolean walled3x1X(boolean[] booleans, int i, int j, int k, int m) {
        return grid(booleans, i, j, k, m)
                && grid(booleans, i, j + 1, k, m)
                && grid(booleans, i, j + 2, k, m)
                && !grid(booleans, i, j - 1, k, m)
                && !grid(booleans, i, j + 3, k, m)
                && !grid(booleans, i, j, k - 1, m)
                && !grid(booleans, i, j, k + 1, m)
                && !grid(booleans, i, j + 1, k - 1, m)
                && !grid(booleans, i, j + 1, k + 1, m)
                && !grid(booleans, i, j + 2, k - 1, m)
                && !grid(booleans, i, j + 2, k + 1, m);
    }

    private static boolean walled3x1Z(boolean[] booleans, int i, int j, int k, int m) {
        return grid(booleans, i, j, k, m)
                && grid(booleans, i, j, k + 1, m)
                && grid(booleans, i, j, k + 2, m)
                && !grid(booleans, i, j, k - 1, m)
                && !grid(booleans, i, j, k + 3, m)
                && !grid(booleans, i, j - 1, k, m)
                && !grid(booleans, i, j + 1, k, m)
                && !grid(booleans, i, j - 1, k + 1, m)
                && !grid(booleans, i, j + 1, k + 1, m)
                && !grid(booleans, i, j - 1, k + 2, m)
                && !grid(booleans, i, j + 1, k + 2, m);
    }

    private static boolean grid(boolean[] booleans, int i, int j, int k, int m) {
        int n = i - m;
        return booleans[n * 20 * 20 + (k + 1) * 20 + j + 1];
    }

    private void checkTunnelStarts(
            ClientWorld world,
            BlockPos pos2,
            boolean bl,
            List<HoleEspModule.Hit> list,
            Set<String> set) {
        int i = Math.round(value(this.sMinTunnelLength, 3.0F));
        for (int[] ints : HORIZONTAL_DIRS) {
            Mutable mutable =
                    new Mutable(pos2.getX() - ints[0], pos2.getY(), pos2.getZ() - ints[1]);
            if (!this.isTunnelSection(world, mutable, ints, bl)) {
                Mutable mutable2 = new Mutable(pos2.getX(), pos2.getY(), pos2.getZ());
                BlockPos pos3 = null;
                BlockPos pos4 = null;
                int j = 0;
                int k = 0;
                if (this.isTunnelSection(world, mutable2, ints, bl)) {
                    pos3 = mutable2.toImmutable();
                }
                while (this.isTunnelSection(world, mutable2, ints, bl)) {
                    k = Math.max(k, this.getTunnelHeight(world, mutable2, bl));
                    pos4 = mutable2.toImmutable();
                    mutable2.move(ints[0], 0, ints[1]);
                    j++;
                }
                if (pos3 != null && pos4 != null && j >= i) {
                    addHit(
                            list,
                            set,
                            new Box(
                                    Math.min(pos3.getX(), pos4.getX()),
                                    pos3.getY(),
                                    Math.min(pos3.getZ(), pos4.getZ()),
                                    Math.max(pos3.getX(), pos4.getX()) + 1,
                                    pos3.getY() + k,
                                    Math.max(pos3.getZ(), pos4.getZ()) + 1),
                            2);
                }
            }
        }
    }

    private void checkStaircaseStarts(
            ClientWorld world,
            BlockPos pos2,
            boolean bl,
            List<HoleEspModule.Hit> list,
            Set<String> set) {
        int i = Math.round(value(this.sMinStairLength, 3.0F));
        for (int[] ints : HORIZONTAL_DIRS) {
            Mutable mutable =
                    new Mutable(pos2.getX() - ints[0], pos2.getY() - 1, pos2.getZ() - ints[1]);
            if (!this.isStaircaseSection(world, mutable, ints, bl)) {
                Mutable mutable2 = new Mutable(pos2.getX(), pos2.getY(), pos2.getZ());
                int j = 0;
                ArrayList list2;
                for (list2 = new ArrayList();
                        this.isStaircaseSection(world, mutable2, ints, bl);
                        j++) {
                    int staircaseHeight = this.getStaircaseHeight(world, mutable2, bl);
                    list2.add(
                            new Box(
                                    mutable2.getX(),
                                    mutable2.getY(),
                                    mutable2.getZ(),
                                    mutable2.getX() + 1,
                                    mutable2.getY() + staircaseHeight,
                                    mutable2.getZ() + 1));
                    mutable2.move(ints[0], 1, ints[1]);
                }
                if (j >= i) {
                    for (Box box2 : (Iterable<Box>) list2) {
                        addHit(list, set, box2, 3);
                    }
                }
            }
        }
    }

    private boolean isTunnelSection(ClientWorld world, BlockPos pos2, int[] ints, boolean bl) {
        int tunnelHeight = this.getTunnelHeight(world, pos2, bl);
        if (tunnelHeight >= Math.round(value(this.sMinTunnelHeight, 2.0F))
                && tunnelHeight <= Math.round(value(this.sMaxTunnelHeight, 3.0F))) {
            if (!isPassable(world, pos2.getX(), pos2.getY() - 1, pos2.getZ(), bl)
                    && !isPassable(
                            world, pos2.getX(), pos2.getY() + tunnelHeight, pos2.getZ(), bl)) {
                boolean bl2 = ints[0] != 0;
                for (int j = 0; j < tunnelHeight; j++) {
                    int i = pos2.getY() + j;
                    if (bl2
                            ? isPassable(world, pos2.getX(), i, pos2.getZ() - 1, bl)
                                    || isPassable(world, pos2.getX(), i, pos2.getZ() + 1, bl)
                            : isPassable(world, pos2.getX() - 1, i, pos2.getZ(), bl)
                                    || isPassable(world, pos2.getX() + 1, i, pos2.getZ(), bl)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isStaircaseSection(ClientWorld world, BlockPos pos2, int[] ints, boolean bl) {
        int staircaseHeight = this.getStaircaseHeight(world, pos2, bl);
        if (staircaseHeight >= Math.round(value(this.sMinStairHeight, 3.0F))
                && staircaseHeight <= Math.round(value(this.sMaxStairHeight, 5.0F))) {
            if (!isPassable(world, pos2.getX(), pos2.getY() - 1, pos2.getZ(), bl)
                    && !isPassable(
                            world, pos2.getX(), pos2.getY() + staircaseHeight, pos2.getZ(), bl)) {
                boolean bl2 = ints[0] != 0;
                for (int j = 0; j < staircaseHeight; j++) {
                    int i = pos2.getY() + j;
                    if (bl2
                            ? isPassable(world, pos2.getX(), i, pos2.getZ() - 1, bl)
                                    || isPassable(world, pos2.getX(), i, pos2.getZ() + 1, bl)
                            : isPassable(world, pos2.getX() - 1, i, pos2.getZ(), bl)
                                    || isPassable(world, pos2.getX() + 1, i, pos2.getZ(), bl)) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private int getTunnelHeight(ClientWorld world, BlockPos pos2, boolean bl) {
        int i = Math.round(value(this.sMaxTunnelHeight, 3.0F));
        int j = 0;
        while (j < i && isPassable(world, pos2.getX(), pos2.getY() + j, pos2.getZ(), bl)) {
            j++;
        }
        return j;
    }

    private int getStaircaseHeight(ClientWorld world, BlockPos pos2, boolean bl) {
        int i = Math.round(value(this.sMaxStairHeight, 5.0F));
        int j = 0;
        while (j < i && isPassable(world, pos2.getX(), pos2.getY() + j, pos2.getZ(), bl)) {
            j++;
        }
        return j;
    }

    private static boolean isOpen(ClientWorld world, BlockPos pos2) {
        BlockState blockState = world.getBlockState(pos2);
        return blockState.isAir() || blockState.getCollisionShape(world, pos2).isEmpty();
    }

    private static boolean isPassable(ClientWorld world, int i, int j, int k, boolean bl) {
        return isPassable(world, PASSABLE_POS.get().set(i, j, k), bl);
    }

    private static boolean isPassable(ClientWorld world, BlockPos pos2, boolean bl) {
        BlockState blockState = world.getBlockState(pos2);
        if (blockState.isAir()) {
            return true;
        } else if (bl) {
            return false;
        } else {
            VoxelShape collisionShape = blockState.getCollisionShape(world, pos2);
            return collisionShape.isEmpty() || !VoxelShapes.fullCube().equals(collisionShape);
        }
    }

    private static void addHit(List<HoleEspModule.Hit> list, Set<String> set, Box box2, int i) {
        String string =
                i + ":" + box2.minX + "," + box2.minY + "," + box2.minZ + "," + box2.maxX + ","
                        + box2.maxY + "," + box2.maxZ;
        if (set.add(string)) {
            for (HoleEspModule.Hit hit : list) {
                if (hit.type() == i && hit.box().intersects(box2)) {
                    return;
                }
            }
            list.add(new HoleEspModule.Hit(box2, i));
        }
    }

    private HoleEspModule.Mode currentMode() {
        String string = this.sMode == null ? "All" : this.sMode.value();
        return switch (string) {
            case "Holes + Tunnels" -> new HoleEspModule.Mode(true, true, false);
            case "Holes + Stairs" -> new HoleEspModule.Mode(true, false, true);
            case "Tunnels + Stairs" -> new HoleEspModule.Mode(false, true, true);
            case "Holes" -> new HoleEspModule.Mode(true, false, false);
            case "Tunnels" -> new HoleEspModule.Mode(false, true, false);
            case "Stairs" -> new HoleEspModule.Mode(false, false, true);
            default -> new HoleEspModule.Mode(true, true, true);
        };
    }

    private int lineColor(int i) {
        return switch (i) {
            case 1 -> color(this.sHole3Line, 1610589440);
            case 2 -> color(this.sTunnelLine, 1593835775);
            case 3 -> color(this.sStairLine, 1610547455);
            default -> color(this.sHoleLine, 1610547200);
        };
    }

    private int fillColor(int i) {
        return switch (i) {
            case 1 -> color(this.sHole3Fill, 520070400);
            case 2 -> color(this.sTunnelFill, 503316735);
            case 3 -> color(this.sStairFill, 520028415);
            default -> color(this.sHoleFill, 520028160);
        };
    }

    private static int chunkX(long l) {
        return (int) l;
    }

    private static int chunkZ(long l) {
        return (int) (l >> 32);
    }

    private int scanRadius(MinecraftClient client) {
        int viewDistance = client.options.getViewDistance().getValue();
        float value = value(this.sScanRadius, 4.0F);
        if (isFastTravel(client)) {
            value = Math.max(value, 12.0F);
        }
        int i = Math.max(1, Math.round(value));
        return Math.min(viewDistance, i);
    }

    private static boolean isFastTravel(MinecraftClient client) {
        if (client.player == null) {
            return false;
        } else {
            Vec3d velocity = client.player.getVelocity();
            double d = velocity.x * velocity.x + velocity.z * velocity.z;
            return client.player.isGliding() || d > 0.18;
        }
    }

    private static float value(NumberSetting setting, float f) {
        return setting == null ? f : setting.value()[0];
    }

    private static int color(ColorSetting setting, int i) {
        return setting == null ? i : setting.argb();
    }

    @Environment(EnvType.CLIENT)
    private final class FastHoleScan {

        final WorldChunk chunk;

        final ClientWorld level;

        final WorldChunk[] neighbors = new WorldChunk[9];

        final long key;

        final int baseX;

        final int baseZ;

        final int minY;

        final int maxY;

        final int gridMinY;

        final int yCount;

        final int minDepth;

        final boolean[] passable;

        final List<HoleEspModule.Hit> hits = new ArrayList<>();

        final Set<String> seen = new HashSet<>();

        final Mutable pos = new Mutable();

        int row;

        int column;

        FastHoleScan(
                HoleEspModule module,
                WorldChunk chunk2,
                long key2,
                int baseX2,
                int baseZ2,
                int minY2,
                int maxY2) {
            this.chunk = chunk2;
            this.level = module.sessionLevel;
            this.key = key2;
            this.baseX = baseX2;
            this.baseZ = baseZ2;
            this.minY = minY2;
            this.maxY = maxY2;
            this.gridMinY = minY2 - 1;
            this.yCount = maxY2 - this.gridMinY + 1;
            this.minDepth = Math.round(HoleEspModule.value(module.sMinHoleDepth, 4.0F));
            int i = Math.max(0, this.yCount) * 20 * 20;
            if (module.passableBuffer.length < i) {
                module.passableBuffer = new boolean[i];
            }
            this.passable = module.passableBuffer;
            for (int o = -1; o <= 1; o++) {
                for (int p = -1; p <= 1; p++) {
                    this.neighbors[(o + 1) * 3 + p + 1] =
                            this.level
                                    .getChunkManager()
                                    .getWorldChunk((baseX2 >> 4) + o, (baseZ2 >> 4) + p);
                }
            }
        }

        boolean validFor(ClientWorld world, int i, int j, int k) {
            if (world == this.level
                    && Math.abs((long) this.chunk.getPos().x - i) <= k
                    && Math.abs((long) this.chunk.getPos().z - j) <= k) {
                for (int m = -1; m <= 1; m++) {
                    for (int n = -1; n <= 1; n++) {
                        if (this.neighbors[(m + 1) * 3 + n + 1]
                                != this.level
                                        .getChunkManager()
                                        .getWorldChunk(
                                                (this.baseX >> 4) + m, (this.baseZ >> 4) + n)) {
                            return false;
                        }
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        boolean step() {
            if (this.yCount <= 1) {
                return true;
            } else if (this.row < this.yCount) {
                int i = this.gridMinY + this.row;
                int j = this.row++ * 20 * 20;
                for (int k = -1; k <= 18; k++) {
                    int m = j + (k + 1) * 20;
                    for (int n = -1; n <= 18; n++) {
                        this.pos.set(this.baseX + n, i, this.baseZ + k);
                        this.passable[m + n + 1] = HoleEspModule.isOpen(this.level, this.pos);
                    }
                }
                return false;
            } else if (this.column >= 256) {
                return true;
            } else {
                int o = this.column & 15;
                int p = this.column++ >> 4;
                HoleEspModule.shaftsInColumn(
                        this.passable,
                        o,
                        p,
                        this.gridMinY,
                        this.minY,
                        this.maxY,
                        this.minDepth,
                        this.baseX,
                        this.baseZ,
                        this.hits,
                        this.seen);
                return this.column >= 256 || this.hits.size() >= 1024;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private record Hit(Box box, int type) {}

    @Environment(EnvType.CLIENT)
    private record Mode(boolean holes, boolean tunnels, boolean stairs) {}
}
