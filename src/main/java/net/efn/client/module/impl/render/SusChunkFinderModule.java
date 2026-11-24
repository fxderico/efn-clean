package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;

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
import net.efn.client.util.ChunkAreas;
import net.efn.client.util.ChunkOverlay;
import net.efn.client.util.GeodeGlowScan;
import net.efn.client.util.HiddenAmethyst;
import net.efn.client.util.LevelUtil;
import net.efn.client.util.PerformanceBudget;
import net.efn.client.util.ServerBlockLight;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BuddingAmethystBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

@Environment(EnvType.CLIENT)
public final class SusChunkFinderModule extends ClientModule {

    private static final long SCAN_BUDGET_NANOS = 2000000L;

    private static final int MIN_SCANNED_NEIGHBOURS = 3;

    private static final int NO_PACKET_SENSITIVITY = 1;

    private static final int VISIBLE_REFRESH_TICKS = 20;

    private static final int LIGHT_RESCAN_TICKS = 20;

    private static final int FULL_RESCAN_TICKS = 1200;

    private static final int VISIBLE_MIN_TICKS = 5;

    private static final int LIGHT_WAIT_LIMIT_TICKS = 200;

    private static final int PRUNE_TICKS = 200;

    private static final int PRUNE_MARGIN = 4;

    private static final int PACKET_MIN_Y = -16;

    private static final int PACKET_MAX_Y = 30;

    private static final String METHOD_CLASSIC = "Classic";

    private static final String METHOD_ANBS = "ANBS+ Scan";

    private static final int GLOW_CELLS_PER_POINT = 12;

    private static final int GEODE_TOP_Y = 47;

    private static final int MIN_EXPOSED_BUDDING = 10;

    private static final int MIN_EXPOSED_LINING = 160;

    private static final int GEODE_SETTLE_TICKS = 40;

    private static final int GEODE_CELL_SHIFT = 2;

    private static final int TORCH_LIGHT = 14;

    private static final Direction[] SIDES = Direction.values();

    private static final int AMETHYST = 0;

    private static final int KELP = 1;

    private static final int CAVE_VINES = 2;

    private static final int VINES = 3;

    private static final int BAMBOO = 4;

    private static final int COCOA = 5;

    private static final int BEE_NEST = 6;

    private static final int GROWTH_KINDS = 7;

    private static final int GROWN = 1;

    private static final int UNGROWN = -1;

    private static final int NO_SIGN = 0;

    private static final int BAMBOO_GROWTH_LIGHT = 9;

    private static final String DONUT_BRAND = "DonutFolia";

    private static final int STYLE_VOTES = 3;

    private final ModeSetting method =
            this.addSetting(
                    "method", new ModeSetting("Method", "Classic", "Classic", "ANBS+ Scan"));

    private final NumberSetting spreadRadius =
            this.addSetting(
                    "spread-radius",
                    new NumberSetting("Simulation Distance", 5.0F, 2.0F, 16.0F).step(1.0F));

    private final NumberSetting sensitivity =
            this.addSetting(
                    "packet-sensitivity",
                    new NumberSetting("Sensitivity", 6.0F, 1.0F, 20.0F).step(1.0F));

    private final BooleanSetting kelp = this.growthSign("growth-kelp", "Kelp");

    private final BooleanSetting caveVines = this.growthSign("growth-cave-vines", "Cave Vines");

    private final BooleanSetting vines = this.growthSign("growth-vines", "Vines");

    private final BooleanSetting bamboo = this.growthSign("growth-bamboo", "Bamboo");

    private final BooleanSetting cocoa = this.growthSign("growth-cocoa", "Cocoa");

    private final BooleanSetting beeNests = this.growthSign("growth-bee-nests", "Bee Nests");

    private final BooleanSetting[] growthSigns =
            new BooleanSetting[] {
                null, this.kelp, this.caveVines, this.vines, this.bamboo, this.cocoa, this.beeNests
            };

    private final ColorSetting chunkColor =
            this.addSetting("chunk-color", new ColorSetting("Chunk Color", 1627325444));

    private final BooleanSetting tracers =
            this.addSetting("tracers", new BooleanSetting("Tracers", true));

    private final BooleanSetting detectMined =
            this.addSetting(
                    "detect-mined-clusters", new BooleanSetting("Detect Mined Clusters", false));

    private final ColorSetting minedColor =
            this.addSetting("mined-color", new ColorSetting("Mined Color", 1621511679));

    private final ChunkAreas chunkAreas = new ChunkAreas();

    private final ChunkOverlay chunkOverlay = new ChunkOverlay();

    private final ChunkAreas minedAreas = new ChunkAreas();

    private final ChunkOverlay minedOverlay = new ChunkOverlay();

    private final GeodeGlowScan glow = new GeodeGlowScan();

    private final ServerBlockLight serverLight = new ServerBlockLight();

    private int darkVotes;

    private int glowVotes;

    private final Long2IntOpenHashMap[] heat = perKind();

    private final Long2IntOpenHashMap[] vetoes = perKind();

    private final Long2ObjectOpenHashMap<int[]> chunkGrown = new Long2ObjectOpenHashMap<>();

    private final Long2IntOpenHashMap chunkUngrown = new Long2IntOpenHashMap();

    private final Long2BooleanOpenHashMap chunkHasPacket = new Long2BooleanOpenHashMap();

    private final LongOpenHashSet scannedChunks = new LongOpenHashSet();

    private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

    private final LongOpenHashSet queued = new LongOpenHashSet();

    private final LongOpenHashSet lightDirty = new LongOpenHashSet();

    private final Long2IntOpenHashMap waitingSince = new Long2IntOpenHashMap();

    private final Long2IntOpenHashMap bestLightClusters = new Long2IntOpenHashMap();

    private final Long2IntOpenHashMap bestLightBuds = new Long2IntOpenHashMap();

    private final LongOpenHashSet geodeChunks = new LongOpenHashSet();

    private Long2IntOpenHashMap visible = new Long2IntOpenHashMap();

    private boolean visibleDirty = true;

    private int visibleAge;

    private ClientWorld lastLevel;

    private int appliedRadius;

    private final Long2ObjectOpenHashMap<SusChunkFinderModule.GeodeSlice> geodeSlices =
            new Long2ObjectOpenHashMap<>();

    private final Long2IntOpenHashMap geodeWaiting = new Long2IntOpenHashMap();

    private final Long2IntOpenHashMap geodeScanTicks = new Long2IntOpenHashMap();

    private List<SusChunkFinderModule.GeodeCluster> geodeClusters = List.of();

    private boolean geodeClustersDirty;

    private LongOpenHashSet minedChunks = new LongOpenHashSet();

    private int minedAge;

    private int ticks;

    public SusChunkFinderModule() {
        super(
                "sus-chunk-finder",
                "Sus Chunk Finder",
                "Finds chunks with signs of player activity from grown amethyst, kelp, vines,"
                        + " bamboo, cocoa and bee nests.",
                ModuleCategory.BASE_RAID);
        this.method.onChange(this::rescan);
        this.sensitivity.onChange(() -> this.visibleDirty = true);
        for (BooleanSetting growthSign : this.growthSigns) {
            if (growthSign != null) {
                growthSign.visibleWhen(() -> this.method.is("Classic"));
                growthSign.onChange(this::rescan);
            }
        }
        this.detectMined.onChange(this::restartGeodes);
        this.minedColor.visibleWhen(this.detectMined::get);
    }

    private BooleanSetting growthSign(String string, String string2) {
        return this.addSetting(string, new BooleanSetting(string2, true));
    }

    private static Long2IntOpenHashMap[] perKind() {
        Long2IntOpenHashMap[] long2IntOpenHashMaps = new Long2IntOpenHashMap[7];
        for (int i = 0; i < 7; i++) {
            long2IntOpenHashMaps[i] = new Long2IntOpenHashMap();
        }
        return long2IntOpenHashMaps;
    }

    private int countedKinds() {
        int i = 1;
        for (int j = 0; j < 7; j++) {
            if (this.growthSigns[j] != null && this.growthSigns[j].get()) {
                i |= 1 << j;
            }
        }
        return i;
    }

    private void rescan() {
        this.clearAll();
        this.queueLoadedChunks();
    }

    private void restartGeodes() {
        this.clearGeodes();
        if (this.detectMined.get()) {
            this.queueLoadedChunks();
        }
    }

    @Override
    protected void onEnable() {
        this.clearAll();
        this.resetLight();
        this.lastLevel = MinecraftClient.getInstance().world;
        this.queueLoadedChunks();
    }

    @Override
    protected void onDisable() {
        this.clearAll();
        this.resetLight();
        this.lastLevel = null;
    }

    private void resetLight() {
        this.serverLight.clear();
        this.darkVotes = 0;
        this.glowVotes = 0;
    }

    private void clearAll() {
        for (int i = 0; i < 7; i++) {
            this.heat[i].clear();
            this.vetoes[i].clear();
        }
        this.chunkGrown.clear();
        this.chunkUngrown.clear();
        this.chunkHasPacket.clear();
        this.scannedChunks.clear();
        this.queue.clear();
        this.queued.clear();
        this.lightDirty.clear();
        this.waitingSince.clear();
        this.bestLightClusters.clear();
        this.bestLightBuds.clear();
        this.geodeChunks.clear();
        this.visible = new Long2IntOpenHashMap();
        this.visibleDirty = true;
        this.appliedRadius = this.spreadRadius.getInt();
        this.clearGeodes();
    }

    private void clearGeodes() {
        this.geodeSlices.clear();
        this.geodeWaiting.clear();
        this.geodeScanTicks.clear();
        this.geodeClusters = List.of();
        this.geodeClustersDirty = false;
        this.minedChunks = new LongOpenHashSet();
        this.minedAge = 0;
    }

    private void queueLoadedChunks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            int i = client.options.getViewDistance().getValue() + 1;
            ChunkPos chunkPos = client.player.getChunkPos();
            for (int j = chunkPos.x - i; j <= chunkPos.x + i; j++) {
                for (int k = chunkPos.z - i; k <= chunkPos.z + i; k++) {
                    if (LevelUtil.isLoaded(client.world, j, k)) {
                        this.enqueue(ChunkPos.toLong(j, k));
                    }
                }
            }
        }
    }

    private void enqueue(long l) {
        if (this.queued.add(l)) {
            this.queue.enqueue(l);
        }
    }

    @EventHandler
    public void onPacketApplied(PacketAppliedEvent event) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (event.packet instanceof ChunkDataS2CPacket chunkDataPacket) {
            if (world != null) {
                this.serverLight.onChunk(
                        world,
                        chunkDataPacket.getChunkX(),
                        chunkDataPacket.getChunkZ(),
                        chunkDataPacket.getLightData());
            }
            this.enqueue(ChunkPos.toLong(chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ()));
            this.markLightDirty(chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ());
        } else if (event.packet instanceof LightUpdateS2CPacket lightUpdatePacket) {
            if (world != null) {
                this.serverLight.onLight(
                        world,
                        lightUpdatePacket.getChunkX(),
                        lightUpdatePacket.getChunkZ(),
                        lightUpdatePacket.getData());
            }
            this.markLightDirty(lightUpdatePacket.getChunkX(), lightUpdatePacket.getChunkZ());
        }
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent event) {
        this.serverLight.onBlockChanged(event.pos);
    }

    private void markLightDirty(int i, int j) {
        for (int k = -1; k <= 1; k++) {
            for (int m = -1; m <= 1; m++) {
                long l = ChunkPos.toLong(i + k, j + m);
                if (this.geodeChunks.contains(l) || !this.scannedChunks.contains(l)) {
                    this.lightDirty.add(l);
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world != this.lastLevel) {
            this.clearAll();
            this.resetLight();
            this.lastLevel = world;
            this.queueLoadedChunks();
        }
        if (world != null) {
            this.ticks++;
            if (this.spreadRadius.getInt() != this.appliedRadius) {
                this.clearAll();
                this.queueLoadedChunks();
            }
            if (this.ticks % 1200 == 0) {
                this.lightDirty.clear();
                this.queueLoadedChunks();
            } else if (this.ticks % 20 == 0 && !this.lightDirty.isEmpty()) {
                LongIterator longIterator = this.lightDirty.iterator();
                while (longIterator.hasNext()) {
                    long l = longIterator.next();
                    if (LevelUtil.isLoaded(world, ChunkPos.getPackedX(l), ChunkPos.getPackedZ(l))) {
                        this.enqueue(l);
                    }
                }
                this.lightDirty.clear();
            }
            boolean optimizationsEnabled = PerformanceModule.optimizationsEnabled();
            long l2 = System.nanoTime();
            long l3 = optimizationsEnabled ? PerformanceBudget.acquire(2000000L) : l2 + 2000000L;
            while (!this.queue.isEmpty() && System.nanoTime() < l3) {
                long dequeueLong = this.queue.dequeueLong();
                this.queued.remove(dequeueLong);
                int packedX = ChunkPos.getPackedX(dequeueLong);
                int packedZ = ChunkPos.getPackedZ(dequeueLong);
                if (!LevelUtil.isLoaded(world, packedX, packedZ)) {
                    this.waitingSince.remove(dequeueLong);
                } else if (this.method.is("ANBS+ Scan") && !this.lightSettled(world, dequeueLong)) {
                    this.lightDirty.add(dequeueLong);
                } else {
                    this.waitingSince.remove(dequeueLong);
                    this.updateChunk(dequeueLong, world.getChunk(packedX, packedZ), world);
                    this.scannedChunks.add(dequeueLong);
                    this.visibleDirty = true;
                }
            }
            if (optimizationsEnabled) {
                PerformanceBudget.record(l2);
            }
            this.visibleAge++;
            if (this.visibleDirty && this.visibleAge >= 5 || this.visibleAge >= 20) {
                this.visible = this.computeVisible(world);
                this.visibleDirty = false;
                this.visibleAge = 0;
            }
            if (this.ticks % 200 == 0) {
                this.pruneFar(MinecraftClient.getInstance());
            }
            if (this.detectMined.get() && ++this.minedAge >= 20) {
                this.minedChunks = this.computeMined(world);
                this.minedAge = 0;
            }
        }
    }

    private boolean lightSettled(ClientWorld world, long l) {
        if (!this.waitingSince.containsKey(l)) {
            this.waitingSince.put(l, this.ticks);
        }
        if (this.ticks - this.waitingSince.get(l) >= 200) {
            return true;
        } else {
            LightingProvider lightingProvider = world.getLightingProvider();
            if (lightingProvider.hasUpdates()) {
                return false;
            } else {
                int packedX = ChunkPos.getPackedX(l);
                int packedZ = ChunkPos.getPackedZ(l);
                for (int k = -1; k <= 1; k++) {
                    for (int m = -1; m <= 1; m++) {
                        if (!LevelUtil.isLoaded(world, packedX + k, packedZ + m)) {
                            return false;
                        }
                        if (!lightingProvider.isLightingEnabled(
                                ChunkSectionPos.withZeroY(packedX + k, packedZ + m))) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
    }

    private void pruneFar(MinecraftClient client) {
        if (client.player != null) {
            int i = client.options.getClampedViewDistance() + 2 * this.spreadRadius.getInt() + 4;
            int j = client.player.getBlockX() >> 4;
            int k = client.player.getBlockZ() >> 4;
            LongArrayList longArrayList = new LongArrayList();
            LongIterator longIterator = this.scannedChunks.iterator();
            while (longIterator.hasNext()) {
                long l = longIterator.next();
                if (Math.max(
                                Math.abs(ChunkPos.getPackedX(l) - j),
                                Math.abs(ChunkPos.getPackedZ(l) - k))
                        > i) {
                    longArrayList.add(l);
                }
            }
            for (int m = 0; m < longArrayList.size(); m++) {
                this.forget(longArrayList.getLong(m));
            }
            if (!longArrayList.isEmpty()) {
                this.visibleDirty = true;
            }
            if (client.world != null) {
                this.serverLight.forgetUnloaded(client.world);
            }
        }
    }

    private void forget(long l) {
        int spreadRadius2 = this.spreadRadius.getInt();
        int packedX = ChunkPos.getPackedX(l);
        int packedZ = ChunkPos.getPackedZ(l);
        this.withdraw(
                packedX,
                packedZ,
                spreadRadius2,
                this.chunkGrown.remove(l),
                this.chunkUngrown.remove(l));
        this.chunkHasPacket.remove(l);
        this.scannedChunks.remove(l);
        this.bestLightClusters.remove(l);
        this.bestLightBuds.remove(l);
        this.geodeChunks.remove(l);
        this.waitingSince.remove(l);
        this.lightDirty.remove(l);
    }

    private void updateChunk(long l, WorldChunk chunk2, ClientWorld world) {
        int spreadRadius2 = this.spreadRadius.getInt();
        int packedX = ChunkPos.getPackedX(l);
        int packedZ = ChunkPos.getPackedZ(l);
        this.withdraw(
                packedX,
                packedZ,
                spreadRadius2,
                this.chunkGrown.remove(l),
                this.chunkUngrown.remove(l));
        this.chunkHasPacket.put(l, hasBlocksInRange(chunk2, -16, 30));
        if (hasLining(chunk2)) {
            this.geodeChunks.add(l);
        } else {
            this.geodeChunks.remove(l);
        }
        SusChunkFinderModule.ChunkScore chunkScore = this.scoreChunk(chunk2, world);
        int bestLightCluster = this.bestLightClusters.get(l);
        int bestLightBud = this.bestLightBuds.get(l);
        if (chunkScore.lightClusters() > bestLightCluster
                || chunkScore.lightClusters() == bestLightCluster
                        && chunkScore.lightBuds() > bestLightBud) {
            bestLightCluster = chunkScore.lightClusters();
            bestLightBud = chunkScore.lightBuds();
            this.bestLightClusters.put(l, bestLightCluster);
            this.bestLightBuds.put(l, bestLightBud);
        }
        int[] ints = chunkScore.grown();
        ints[0] += bestLightCluster;
        int i = bestLightBud > 0 ? chunkScore.ungrown() | 1 : chunkScore.ungrown();
        this.chunkGrown.put(l, ints);
        this.chunkUngrown.put(l, i);
        this.spread(packedX, packedZ, spreadRadius2, ints, i);
        if (this.detectMined.get()) {
            this.collectGeode(l, chunk2, world);
        }
    }

    private void spread(int i, int j, int k, int[] ints, int m) {
        for (int n = 0; n < 7; n++) {
            Long2IntOpenHashMap long2IntOpenHashMap = this.heat[n];
            Long2IntOpenHashMap vetoe = this.vetoes[n];
            int o = ints[n];
            if (o > 0) {
                forArea(i, j, k, lx -> long2IntOpenHashMap.addTo(lx, o));
            }
            if ((m & 1 << n) != 0) {
                forArea(i, j, k, lx -> vetoe.addTo(lx, 1));
            }
        }
    }

    private void withdraw(int i, int j, int k, int[] ints, int m) {
        for (int n = 0; n < 7; n++) {
            Long2IntOpenHashMap long2IntOpenHashMap = this.heat[n];
            Long2IntOpenHashMap vetoe = this.vetoes[n];
            int o = ints == null ? 0 : ints[n];
            if (o > 0) {
                forArea(i, j, k, lx -> subtract(long2IntOpenHashMap, lx, o));
            }
            if ((m & 1 << n) != 0) {
                forArea(i, j, k, lx -> subtract(vetoe, lx, 1));
            }
        }
    }

    private static void forArea(int i, int j, int k, LongConsumer longConsumer) {
        for (int m = -k; m <= k; m++) {
            for (int n = -k; n <= k; n++) {
                longConsumer.accept(ChunkPos.toLong(i + m, j + n));
            }
        }
    }

    private static void subtract(Long2IntOpenHashMap long2IntOpenHashMap, long l, int i) {
        int j = long2IntOpenHashMap.get(l) - i;
        if (j > 0) {
            long2IntOpenHashMap.put(l, j);
        } else {
            long2IntOpenHashMap.remove(l);
        }
    }

    private static boolean hasLining(WorldChunk chunk2) {
        for (ChunkSection section : chunk2.getSectionArray()) {
            if (section != null
                    && !section.isEmpty()
                    && section.hasAny(SusChunkFinderModule::isLining)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBlocksInRange(WorldChunk chunk2, int i, int j) {
        ChunkSection[] sectionArray = chunk2.getSectionArray();
        int bottomSectionCoord = chunk2.getBottomSectionCoord();
        for (int m = 0; m < sectionArray.length; m++) {
            ChunkSection section = sectionArray[m];
            int k;
            if (section != null
                    && !section.isEmpty()
                    && (k = bottomSectionCoord + m << 4) + 15 >= i
                    && k <= j) {
                for (int o = 0; o < 16; o++) {
                    int n = k + o;
                    if (n >= i && n <= j) {
                        for (int q = 0; q < 16; q++) {
                            for (int r = 0; r < 16; r++) {
                                if (!section.getBlockState(q, o, r).isAir()) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private SusChunkFinderModule.ChunkScore scoreChunk(WorldChunk chunk2, ClientWorld world) {
        int[] ints = new int[7];
        if (this.method.is("ANBS+ Scan")) {
            ints[0] = this.glow.scan(world, chunk2, null) / 12;
            return new SusChunkFinderModule.ChunkScore(ints, 0, 0, 0);
        } else {
            ChunkSection[] sectionArray = chunk2.getSectionArray();
            int bottomSectionCoord = chunk2.getBottomSectionCoord();
            ChunkPos chunkPos = chunk2.getPos();
            int countedKinds = this.countedKinds();
            int i = 0;
            Mutable mutable = new Mutable();
            for (int m = 0; m < sectionArray.length; m++) {
                ChunkSection section = sectionArray[m];
                if (section != null
                        && !section.isEmpty()
                        && section.hasAny(blockState -> isCounted(blockState, countedKinds))) {
                    int j = bottomSectionCoord + m << 4;
                    for (int o = 0; o < 16; o++) {
                        for (int p = 0; p < 16; p++) {
                            for (int q = 0; q < 16; q++) {
                                BlockState blockState2 = section.getBlockState(o, p, q);
                                int growthKind = growthKind(blockState2);
                                if (growthKind >= 0 && (countedKinds & 1 << growthKind) != 0) {
                                    mutable.set(
                                            chunkPos.getStartX() + o,
                                            j + p,
                                            chunkPos.getStartZ() + q);
                                    int sign = readSign(world, mutable, blockState2, growthKind);
                                    if (sign == 1) {
                                        ints[growthKind]++;
                                    } else if (sign == -1) {
                                        i |= 1 << growthKind;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            HiddenAmethyst.Reading reading =
                    HiddenAmethyst.read(world, chunk2, this.serverLight, this.hiddenSolids());
            this.darkVotes = this.darkVotes + reading.darkVotes();
            this.glowVotes = this.glowVotes + reading.glowVotes();
            return new SusChunkFinderModule.ChunkScore(ints, i, reading.clusters(), reading.buds());
        }
    }

    private Boolean hiddenSolids() {
        if (onDonut()) {
            return Boolean.TRUE;
        } else if (this.darkVotes >= 3 && this.darkVotes > this.glowVotes) {
            return Boolean.TRUE;
        } else {
            return this.glowVotes >= 3 && this.glowVotes > this.darkVotes ? Boolean.FALSE : null;
        }
    }

    private static boolean onDonut() {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        String string = networkHandler == null ? null : networkHandler.getBrand();
        return string != null && string.contains("DonutFolia");
    }

    private static boolean isCounted(BlockState state, int i) {
        int growthKind = growthKind(state);
        return growthKind >= 0 && (i & 1 << growthKind) != 0;
    }

    private static int growthKind(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.AMETHYST_CLUSTER || isBud(state)) {
            return 0;
        } else if (block == Blocks.KELP) {
            return 1;
        } else if (block == Blocks.CAVE_VINES) {
            return 2;
        } else if (block == Blocks.VINE) {
            return 3;
        } else if (block == Blocks.BAMBOO) {
            return 4;
        } else if (block == Blocks.COCOA) {
            return 5;
        } else {
            return block == Blocks.BEE_NEST ? 6 : -1;
        }
    }

    private static int readSign(ClientWorld world, Mutable mutable, BlockState state, int i) {
        return switch (i) {
            case 0 -> {
                if (state.isOf(Blocks.AMETHYST_CLUSTER)) {
                    yield 1;
                } else if (!state.contains(Properties.FACING)) {
                    yield 0;
                } else {
                    Direction direction = state.get(Properties.FACING);
                    yield world.getBlockState(mutable.move(direction.getOpposite()))
                                    .isOf(Blocks.BUDDING_AMETHYST)
                            ? -1
                            : 1;
                }
            }
            case 1 ->
                    state.get(Properties.AGE_25) != 25
                                    && world.getBlockState(mutable.move(Direction.UP))
                                            .isOf(Blocks.WATER)
                            ? -1
                            : 1;
            case 2 ->
                    state.get(Properties.AGE_25) != 25
                                    && world.getBlockState(mutable.move(Direction.DOWN)).isAir()
                            ? -1
                            : 1;
            case 3 -> {
                BlockState blockState = world.getBlockState(mutable.move(Direction.DOWN));
                yield blockState.isOf(Blocks.VINE)
                        ? 0
                        : (!blockState.isAir() ? 1 : (hangsFromSide(state) ? -1 : 1));
            }
            case 4 -> bambooSign(world, mutable, state);
            case 5 -> state.get(Properties.AGE_2) == 2 ? 1 : -1;
            case 6 -> state.get(BeehiveBlock.HONEY_LEVEL) == 5 ? 1 : -1;
            default -> 0;
        };
    }

    private static boolean hangsFromSide(BlockState state) {
        return state.get(Properties.NORTH)
                || state.get(Properties.EAST)
                || state.get(Properties.SOUTH)
                || state.get(Properties.WEST);
    }

    private static int bambooSign(ClientWorld world, Mutable mutable, BlockState state) {
        int i = mutable.getX();
        int j = mutable.getY();
        int k = mutable.getZ();
        BlockState blockState = world.getBlockState(mutable.move(Direction.UP));
        if (blockState.isOf(Blocks.BAMBOO)) {
            return 0;
        } else if (state.get(Properties.STAGE) == 1) {
            return 1;
        } else if (blockState.isAir() && world.getBaseLightLevel(mutable, 0) >= 9) {
            int m = 1;
            while (m < 16 && world.getBlockState(mutable.set(i, j - m, k)).isOf(Blocks.BAMBOO)) {
                m++;
            }
            return m < 16 ? -1 : 0;
        } else {
            return 0;
        }
    }

    private void collectGeode(long l, WorldChunk chunk2, ClientWorld world) {
        this.geodeScanTicks.put(l, this.ticks);
        if (this.geodeSlices.remove(l) != null) {
            this.geodeClustersDirty = true;
        }
        this.geodeWaiting.remove(l);
        int packedX = ChunkPos.getPackedX(l);
        int packedZ = ChunkPos.getPackedZ(l);
        for (int k = -1; k <= 1; k++) {
            for (int m = -1; m <= 1; m++) {
                long l2 = ChunkPos.toLong(packedX + k, packedZ + m);
                if ((k != 0 || m != 0) && (this.geodeWaiting.get(l2) & waitBit(-k, -m)) != 0) {
                    this.enqueue(l2);
                }
            }
        }
        if (LevelUtil.isOverworld(world)) {
            ChunkSection[] sectionArray = chunk2.getSectionArray();
            int bottomSectionCoord = chunk2.getBottomSectionCoord();
            int i = packedX << 4;
            int j = packedZ << 4;
            LongArrayList longArrayList = new LongArrayList();
            LongArrayList longArrayList2 = new LongArrayList();
            Mutable mutable = new Mutable();
            int n = 0;
            int o;
            for (int t = 0;
                    t < sectionArray.length && (o = bottomSectionCoord + t << 4) <= 47;
                    t++) {
                ChunkSection section = sectionArray[t];
                if (section != null
                        && !section.isEmpty()
                        && section.hasAny(SusChunkFinderModule::isLining)) {
                    for (int u = 0; u < 16; u++) {
                        for (int v = 0; v < 16; v++) {
                            for (int w = 0; w < 16; w++) {
                                BlockState blockState = section.getBlockState(w, u, v);
                                if (isLining(blockState)) {
                                    int p = i + w;
                                    int q = o + u;
                                    int r = j + v;
                                    boolean bl = false;
                                    int i2 = 0;
                                    for (Direction direction : SIDES) {
                                        int j2 = p + direction.getOffsetX();
                                        int k2 = q + direction.getOffsetY();
                                        int m2 = r + direction.getOffsetZ();
                                        if (!world.isOutOfHeightLimit(k2)) {
                                            int n2 = j2 >> 4;
                                            int o2 = m2 >> 4;
                                            boolean bl2 = n2 == packedX && o2 == packedZ;
                                            if (!bl2 && !LevelUtil.isLoaded(world, n2, o2)) {
                                                i2 |= waitBit(n2 - packedX, o2 - packedZ);
                                            } else {
                                                mutable.set(j2, k2, m2);
                                                if (isOpen(
                                                        bl2
                                                                ? chunk2.getBlockState(mutable)
                                                                : world.getBlockState(mutable))) {
                                                    bl = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    if (!bl) {
                                        n |= i2;
                                    } else {
                                        long l3 = BlockPos.asLong(p, q, r);
                                        longArrayList.add(l3);
                                        if (blockState.isOf(Blocks.BUDDING_AMETHYST)) {
                                            longArrayList2.add(l3);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (n != 0) {
                this.geodeWaiting.put(l, n);
            }
            if (!longArrayList.isEmpty()) {
                this.geodeSlices.put(
                        l,
                        new SusChunkFinderModule.GeodeSlice(
                                longArrayList.toLongArray(), longArrayList2.toLongArray()));
                this.geodeClustersDirty = true;
            }
        }
    }

    private static int waitBit(int i, int j) {
        return 1 << (i + 1) * 3 + j + 1;
    }

    private LongOpenHashSet computeMined(ClientWorld world) {
        if (this.geodeSlices
                .keySet()
                .removeIf(
                        value ->
                                !LevelUtil.isLoaded(
                                        world,
                                        ChunkPos.getPackedX(value),
                                        ChunkPos.getPackedZ(value)))) {
            this.geodeClustersDirty = true;
        }
        this.geodeWaiting
                .keySet()
                .removeIf(
                        value2 ->
                                !LevelUtil.isLoaded(
                                        world,
                                        ChunkPos.getPackedX(value2),
                                        ChunkPos.getPackedZ(value2)));
        this.geodeScanTicks
                .keySet()
                .removeIf(
                        value3 ->
                                !LevelUtil.isLoaded(
                                        world,
                                        ChunkPos.getPackedX(value3),
                                        ChunkPos.getPackedZ(value3)));
        if (this.geodeClustersDirty) {
            this.geodeClusters = this.clusterGeodes();
            this.geodeClustersDirty = false;
        }
        LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
        for (SusChunkFinderModule.GeodeCluster geodeCluster : this.geodeClusters) {
            boolean bl =
                    geodeCluster.budding() > 0
                            ? geodeCluster.budding() >= 10
                            : geodeCluster.lining().length >= 160;
            if (bl && this.settled(world, geodeCluster) && unlit(world, geodeCluster.lining())) {
                longOpenHashSet.add(geodeCluster.chunk());
            }
        }
        return longOpenHashSet;
    }

    private List<SusChunkFinderModule.GeodeCluster> clusterGeodes() {
        Long2ObjectOpenHashMap long2ObjectOpenHashMap = new Long2ObjectOpenHashMap();
        LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
        for (SusChunkFinderModule.GeodeSlice geodeSlice : this.geodeSlices.values()) {
            for (long l : geodeSlice.lining()) {
                long l2 =
                        BlockPos.asLong(
                                BlockPos.unpackLongX(l) >> 2,
                                BlockPos.unpackLongY(l) >> 2,
                                BlockPos.unpackLongZ(l) >> 2);
                LongArrayList longArrayList = (LongArrayList) long2ObjectOpenHashMap.get(l2);
                if (longArrayList == null) {
                    longArrayList = new LongArrayList();
                    long2ObjectOpenHashMap.put(l2, longArrayList);
                }
                longArrayList.add(l);
            }
            for (long l3 : geodeSlice.budding()) {
                longOpenHashSet.add(l3);
            }
        }
        ArrayList list = new ArrayList();
        LongOpenHashSet longOpenHashSet2 = new LongOpenHashSet();
        LongArrayFIFOQueue longArrayFIFOQueue = new LongArrayFIFOQueue();
        LongIterator longIterator = long2ObjectOpenHashMap.keySet().iterator();
        while (longIterator.hasNext()) {
            long l4 = longIterator.next();
            if (longOpenHashSet2.add(l4)) {
                LongArrayList longArrayList2 = new LongArrayList();
                longArrayFIFOQueue.enqueue(l4);
                while (!longArrayFIFOQueue.isEmpty()) {
                    long dequeueLong = longArrayFIFOQueue.dequeueLong();
                    longArrayList2.addAll((LongList) long2ObjectOpenHashMap.get(dequeueLong));
                    int unpackLongX = BlockPos.unpackLongX(dequeueLong);
                    int unpackLongY = BlockPos.unpackLongY(dequeueLong);
                    int unpackLongZ = BlockPos.unpackLongZ(dequeueLong);
                    for (int m = -1; m <= 1; m++) {
                        for (int n = -1; n <= 1; n++) {
                            for (int o = -1; o <= 1; o++) {
                                long l5 =
                                        BlockPos.asLong(
                                                unpackLongX + m, unpackLongY + n, unpackLongZ + o);
                                if (long2ObjectOpenHashMap.containsKey(l5)
                                        && longOpenHashSet2.add(l5)) {
                                    longArrayFIFOQueue.enqueue(l5);
                                }
                            }
                        }
                    }
                }
                list.add(toCluster(longArrayList2.toLongArray(), longOpenHashSet));
            }
        }
        return list;
    }

    private static SusChunkFinderModule.GeodeCluster toCluster(
            long[] longs, LongOpenHashSet longOpenHashSet) {
        int i = 0;
        int j = 2147483647;
        int k = 2147483647;
        int m = -2147483648;
        int n = -2147483648;
        long l = 0L;
        long l2 = 0L;
        for (long l3 : longs) {
            if (longOpenHashSet.contains(l3)) {
                i++;
            }
            int unpackLongX = BlockPos.unpackLongX(l3);
            int unpackLongZ = BlockPos.unpackLongZ(l3);
            j = Math.min(j, unpackLongX);
            k = Math.min(k, unpackLongZ);
            m = Math.max(m, unpackLongX);
            n = Math.max(n, unpackLongZ);
            l += unpackLongX;
            l2 += unpackLongZ;
        }
        int o = (int) Math.floorDiv(l, longs.length);
        int p = (int) Math.floorDiv(l2, longs.length);
        return new SusChunkFinderModule.GeodeCluster(
                longs, i, j, k, m, n, ChunkPos.toLong(o >> 4, p >> 4));
    }

    private boolean settled(ClientWorld world, SusChunkFinderModule.GeodeCluster geodeCluster) {
        for (int i = geodeCluster.minX() - 1 >> 4; i <= geodeCluster.maxX() + 1 >> 4; i++) {
            for (int j = geodeCluster.minZ() - 1 >> 4; j <= geodeCluster.maxZ() + 1 >> 4; j++) {
                long l = ChunkPos.toLong(i, j);
                if (!LevelUtil.isLoaded(world, i, j) || !this.geodeScanTicks.containsKey(l)) {
                    return false;
                }
                if (this.ticks - this.geodeScanTicks.get(l) < 40) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean unlit(ClientWorld world, long[] longs) {
        Mutable mutable = new Mutable();
        Long2IntOpenHashMap long2IntOpenHashMap = new Long2IntOpenHashMap();
        for (long l : longs) {
            int unpackLongX = BlockPos.unpackLongX(l);
            int unpackLongY = BlockPos.unpackLongY(l);
            int unpackLongZ = BlockPos.unpackLongZ(l);
            for (Direction direction : SIDES) {
                mutable.set(
                        unpackLongX + direction.getOffsetX(),
                        unpackLongY + direction.getOffsetY(),
                        unpackLongZ + direction.getOffsetZ());
                if (isAmethystGrowth(world.getBlockState(mutable))) {
                    return false;
                }
                int lightLevel = world.getLightLevel(LightType.BLOCK, mutable);
                if (lightLevel > 0
                        && sourceLight(world, mutable, lightLevel, long2IntOpenHashMap) != 14) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int sourceLight(
            ClientWorld world, BlockPos pos, int i, Long2IntOpenHashMap long2IntOpenHashMap) {
        LongArrayList longArrayList = new LongArrayList();
        Mutable mutable = pos.mutableCopy();
        Mutable mutable2 = new Mutable();
        int j;
        while (true) {
            long l;
            if (long2IntOpenHashMap.containsKey(l = mutable.asLong())) {
                j = long2IntOpenHashMap.get(l);
                break;
            }
            longArrayList.add(l);
            Direction direction = null;
            int k = i;
            for (Direction direction2 : SIDES) {
                int lightLevel =
                        world.getLightLevel(LightType.BLOCK, mutable2.set(mutable, direction2));
                if (lightLevel > k) {
                    k = lightLevel;
                    direction = direction2;
                }
            }
            if (direction == null) {
                j = i;
                break;
            }
            mutable.move(direction);
            i = k;
        }
        for (int n = 0; n < longArrayList.size(); n++) {
            long2IntOpenHashMap.put(longArrayList.getLong(n), j);
        }
        return j;
    }

    private static boolean isLining(BlockState state) {
        return state.isOf(Blocks.AMETHYST_BLOCK) || state.isOf(Blocks.BUDDING_AMETHYST);
    }

    private static boolean isOpen(BlockState state) {
        return BuddingAmethystBlock.canGrowIn(state) || isAmethystGrowth(state);
    }

    private static boolean isBud(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.SMALL_AMETHYST_BUD
                || block == Blocks.MEDIUM_AMETHYST_BUD
                || block == Blocks.LARGE_AMETHYST_BUD;
    }

    private static boolean isAmethystGrowth(BlockState state) {
        return state.isOf(Blocks.AMETHYST_CLUSTER) || isBud(state);
    }

    private Long2IntOpenHashMap computeVisible(ClientWorld world) {
        Long2IntOpenHashMap long2IntOpenHashMap = new Long2IntOpenHashMap();
        for (int i = 0; i < 7; i++) {
            Long2IntOpenHashMap vetoe = this.vetoes[i];
            for (Entry long2IntEntry : this.heat[i].long2IntEntrySet()) {
                if (!vetoe.containsKey(long2IntEntry.getLongKey())) {
                    long2IntOpenHashMap.addTo(
                            long2IntEntry.getLongKey(), long2IntEntry.getIntValue());
                }
            }
        }
        Long2IntOpenHashMap long2IntOpenHashMap2 = new Long2IntOpenHashMap();
        for (Entry long2IntEntry2 : long2IntOpenHashMap.long2IntEntrySet()) {
            long longKey = long2IntEntry2.getLongKey();
            int packedX = ChunkPos.getPackedX(longKey);
            int packedZ;
            if (LevelUtil.isLoaded(world, packedX, packedZ = ChunkPos.getPackedZ(longKey))
                    && this.scannedNeighbours(packedX, packedZ) >= 3) {
                long2IntOpenHashMap2.put(longKey, long2IntEntry2.getIntValue());
            }
        }
        int sensitivity2 = this.sensitivity.getInt();
        int appliedRadius2 = this.appliedRadius;
        Long2IntOpenHashMap long2IntOpenHashMap3 = new Long2IntOpenHashMap();
        for (Entry long2IntEntry3 : long2IntOpenHashMap2.long2IntEntrySet()) {
            long longKey2 = long2IntEntry3.getLongKey();
            int intValue = long2IntEntry3.getIntValue();
            boolean bl =
                    intValue >= sensitivity2
                            || !this.chunkHasPacket.getOrDefault(longKey2, true)
                                    && this.amethystHeat(longKey2) >= 1;
            if (bl && isPeak(long2IntOpenHashMap2, longKey2, intValue, appliedRadius2)) {
                long2IntOpenHashMap3.put(longKey2, intValue);
            }
        }
        return long2IntOpenHashMap3;
    }

    private int amethystHeat(long l) {
        return this.vetoes[0].containsKey(l) ? 0 : this.heat[0].get(l);
    }

    private static boolean isPeak(Long2IntOpenHashMap long2IntOpenHashMap, long l, int i, int j) {
        int packedX = ChunkPos.getPackedX(l);
        int packedZ = ChunkPos.getPackedZ(l);
        for (int n = -j; n <= j; n++) {
            for (int o = -j; o <= j; o++) {
                if ((n != 0 || o != 0)
                        && long2IntOpenHashMap.get(ChunkPos.toLong(packedX + n, packedZ + o)) > i) {
                    return false;
                }
            }
        }
        return true;
    }

    private int scannedNeighbours(int i, int j) {
        int k = 0;
        for (int m = -1; m <= 1; m++) {
            for (int n = -1; n <= 1; n++) {
                if ((m != 0 || n != 0)
                        && this.scannedChunks.contains(ChunkPos.toLong(i + m, j + n))) {
                    k++;
                }
            }
        }
        return k;
    }

    public LongSet flaggedChunks() {
        return LongSets.unmodifiable(this.visible.keySet());
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (MinecraftClient.getInstance().world != null) {
            this.chunkOverlay.draw(
                    this.visible.keySet(), this.chunkColor.argb(), event.partialTick);
            if (this.tracers.get()) {
                this.chunkAreas.drawTracers(
                        this.visible.keySet(), 63.0, ColorHelper.fullAlpha(this.chunkColor.argb()));
            }
            this.minedOverlay.draw(this.minedChunks, this.minedColor.argb(), event.partialTick);
            if (this.tracers.get()) {
                this.minedAreas.drawTracers(
                        this.minedChunks, 63.0, ColorHelper.fullAlpha(this.minedColor.argb()));
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private record ChunkScore(int[] grown, int ungrown, int lightClusters, int lightBuds) {}

    @Environment(EnvType.CLIENT)
    private record GeodeCluster(
            long[] lining, int budding, int minX, int minZ, int maxX, int maxZ, long chunk) {}

    @Environment(EnvType.CLIENT)
    private record GeodeSlice(long[] lining, long[] budding) {}
}
