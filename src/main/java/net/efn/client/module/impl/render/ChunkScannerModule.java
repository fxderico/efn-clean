package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ChallengeToast;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.ChunkAreas;
import net.efn.client.util.ChunkOverlay;
import net.efn.client.util.LevelUtil;
import net.efn.client.util.PerformanceBudget;
import net.efn.client.util.seedscan.AdvancedScanner;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.GeneratorOptions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;

@Environment(EnvType.CLIENT)
public final class ChunkScannerModule extends ClientModule {

    private static final int PROBE_CHUNKS = 48;

    private static final byte OTHER = 0;

    private static final byte STONE = 1;

    private static final byte BLOB = 2;

    private static final byte AIR = 3;

    private static final byte MISSING = 4;

    private static final int[] SIDE_X = new int[] {1, -1, 0, 0};

    private static final int[] SIDE_Z = new int[] {0, 0, 1, -1};

    private static final int[] CORNER_X = new int[] {1, 1, -1, -1};

    private static final int[] CORNER_Z = new int[] {1, -1, 1, -1};

    private static final int MIN_Y = 0;

    private static final int MAX_Y = 32;

    private static final int CHUNK_COLOR = 1627389951;

    private static final int OLD_CHUNK_COLOR = 1614011733;

    private static final int SEED_SETTLE_TICKS = 20;

    private final BooleanSetting advancedScanner =
            this.addSetting("advanced-scanner", new BooleanSetting("Advanced Scanner", false));

    private final StringSetting seed = this.addSetting("seed", new StringSetting("Seed", ""));

    private final NumberSetting stackHeight =
            this.addSetting(
                    "stack-height", new NumberSetting("Sensitivity", 2.0F, 2.0F, 5.0F).step(1.0F));

    private final NumberSetting spreadRadius =
            this.addSetting(
                    "spread-radius",
                    new NumberSetting("Spread Radius", 1.0F, 0.0F, 16.0F).step(1.0F));

    private final ColorSetting chunkColor =
            this.addSetting("chunk-color", new ColorSetting("Chunk Color", 1627389951));

    private final BooleanSetting tracers =
            this.addSetting("tracers", new BooleanSetting("Tracers", true));

    private final BooleanSetting sound =
            this.addSetting("sound", new BooleanSetting("Sound", true));

    private final ChunkAreas chunkAreas = new ChunkAreas();

    private final ChunkOverlay chunkOverlay = new ChunkOverlay();

    private static final ChunkScannerModule.Find UNDECIDED =
            new ChunkScannerModule.Find(0L, 0, new long[0]);

    private final Long2ObjectOpenHashMap<ChunkScannerModule.Find> flagged =
            new Long2ObjectOpenHashMap<>();

    private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

    private final LongOpenHashSet queued = new LongOpenHashSet();

    private final List<ChunkScannerModule.Find> fresh = new ArrayList<>();

    private final Long2IntOpenHashMap heat = new Long2IntOpenHashMap();

    private int appliedRadius;

    private LongOpenHashSet shown = new LongOpenHashSet();

    private boolean shownDirty = true;

    private int shownRefreshTicks;

    private ClientWorld lastLevel;

    private boolean scanning;

    private AdvancedScanner advanced;

    private boolean appliedAdvanced;

    private String seedText;

    private int seedStill;

    private boolean seedAsked;

    private ChunkScannerModule.ScanConfig appliedConfig;

    private WeakReference<ClientPlayNetworkHandler> probeServer = new WeakReference<>(null);

    private boolean probing;

    private final LongOpenHashSet probed = new LongOpenHashSet();

    private final WorldChunk[] around = new WorldChunk[9];

    private final int[] groupX = new int[4];

    private final int[] groupZ = new int[4];

    private int groupSize;

    private boolean guessed;

    private Block wall;

    private Block lastBlock;

    private final int[] stackTop = new int[256];

    private final int[] stackShape = new int[256];

    private final int[] stackAreas = new int[256];

    private final boolean[] stackGuessed = new boolean[256];

    private final Block[] stackWall = new Block[256];

    private boolean sawBlob;

    private int scanX;

    private int scanZ;

    private int keepHeight;

    private long[] keptStacks = new long[16];

    private int keptStackCount;

    private int tallest;

    private long bottom;

    private boolean unsure;

    public ChunkScannerModule() {
        super(
                "chunk-scanner",
                "Chunk Scanner",
                "Marks suspicious chunks with signs of player activity.",
                ModuleCategory.BASE_RAID);
        BooleanSetting setting = this.usesNotifications();
        this.sound.visibleWhen(setting::get);
        this.stackHeight.visibleWhen(() -> !this.advancedScanner.get());
        this.seed.visibleWhen(this.advancedScanner::get);
        this.seed.privateValue();
    }

    @Override
    public int settingsVersion() {
        return 1;
    }

    @Override
    public void migrateSettings(int i) {
        if (i < 1 && this.chunkColor.argb() == 1614011733) {
            this.chunkColor.setArgb(1627389951);
        }
    }

    @Override
    protected void onEnable() {
        this.startLevel(MinecraftClient.getInstance().world);
    }

    @Override
    protected void onDisable() {
        this.startLevel(null);
    }

    private void startLevel(ClientWorld lastLevel2) {
        this.closeAdvanced();
        this.appliedAdvanced = this.advancedScanner.get();
        this.flagged.clear();
        this.heat.clear();
        this.appliedRadius = this.spreadRadius.getInt();
        this.queue.clear();
        this.queued.clear();
        this.fresh.clear();
        this.shown = new LongOpenHashSet();
        this.shownDirty = true;
        this.shownRefreshTicks = 0;
        this.lastLevel = lastLevel2;
        boolean bl =
                this.scanning =
                        lastLevel2 != null
                                && lastLevel2.getRegistryKey() != World.NETHER
                                && lastLevel2.getRegistryKey() != World.END;
        if (this.scanning) {
            ClientPlayNetworkHandler networkHandler =
                    MinecraftClient.getInstance().getNetworkHandler();
            if (networkHandler != this.probeServer.get()) {
                this.probeServer = new WeakReference<>(networkHandler);
                this.probing = true;
                this.probed.clear();
            }
            this.appliedConfig = this.currentConfig(lastLevel2);
            this.queueLoadedChunks(lastLevel2);
        }
    }

    private ChunkScannerModule.ScanConfig currentConfig(ClientWorld world) {
        return new ChunkScannerModule.ScanConfig(
                Math.max(0, world.getBottomY()),
                Math.min(32, world.getTopYInclusive()),
                this.stackHeight.getInt());
    }

    private void queueLoadedChunks(ClientWorld world) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            int i = client.options.getViewDistance().getValue() + 1;
            ChunkPos chunkPos = client.player.getChunkPos();
            for (int j = chunkPos.x - i; j <= chunkPos.x + i; j++) {
                for (int k = chunkPos.z - i; k <= chunkPos.z + i; k++) {
                    if (LevelUtil.isLoaded(world, j, k)) {
                        this.enqueue(ChunkPos.toLong(j, k));
                    }
                }
            }
        }
    }

    private void enqueue(long l) {
        if (this.scanning && !this.advancedScanner.get() && this.queued.add(l)) {
            this.queue.enqueue(l);
        }
    }

    @EventHandler
    public void onPacketApplied(PacketAppliedEvent event) {
        if (this.advanced != null) {
            Packet packet = event.packet;
            if (event.packet instanceof ChunkDataS2CPacket) {
                ChunkDataS2CPacket chunkDataPacket = (ChunkDataS2CPacket) packet;
                this.advanced.chunkArrived(
                        chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ());
            }
        }
        Packet packet2 = event.packet;
        if (event.packet instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacket chunkDataPacket2 = (ChunkDataS2CPacket) packet2;
            this.shownDirty = true;
            this.enqueue(
                    ChunkPos.toLong(chunkDataPacket2.getChunkX(), chunkDataPacket2.getChunkZ()));
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i != 0 || j != 0) {
                        this.enqueue(
                                ChunkPos.toLong(
                                        chunkDataPacket2.getChunkX() + i,
                                        chunkDataPacket2.getChunkZ() + j));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent event) {
        if (this.scanning && event.pos != null) {
            if (this.advanced != null) {
                this.advanced.blockChanged(event.pos);
            }
            int i;
            if ((i = event.pos.getY()) >= this.appliedConfig.minY() - 1
                    && i <= this.appliedConfig.maxY() + 1) {
                if (matters(event.oldState) || matters(event.newState)) {
                    int j = event.pos.getX();
                    int k = event.pos.getZ();
                    for (int m = j - 2 >> 4; m <= j + 1 >> 4; m++) {
                        for (int n = k - 2 >> 4; n <= k + 2 >> 4; n++) {
                            this.enqueue(ChunkPos.toLong(m, n));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != this.lastLevel) {
            this.startLevel(world);
        }
        if (this.scanning) {
            if (this.advancedScanner.get() != this.appliedAdvanced) {
                this.startLevel(world);
            }
            ChunkScannerModule.ScanConfig scanConfig;
            if (!(scanConfig = this.currentConfig(world)).equals(this.appliedConfig)) {
                this.appliedConfig = scanConfig;
                this.queueLoadedChunks(world);
            }
            int spreadRadius2;
            if ((spreadRadius2 = this.spreadRadius.getInt()) != this.appliedRadius) {
                this.appliedRadius = spreadRadius2;
                this.heat.clear();
                this.shownDirty = true;
                ObjectIterator objectIterator = this.flagged.long2ObjectEntrySet().fastIterator();
                while (objectIterator.hasNext()) {
                    Entry entry = (Entry) objectIterator.next();
                    this.spread(entry.getLongKey(), (ChunkScannerModule.Find) entry.getValue(), 1);
                }
            }
            boolean optimizationsEnabled;
            long l =
                    (optimizationsEnabled = PerformanceModule.optimizationsEnabled())
                            ? PerformanceBudget.acquire(PerformanceModule.scanBudgetNanos())
                            : System.nanoTime() + PerformanceModule.scanBudgetNanos();
            long l2 = System.nanoTime();
            try {
                if (this.advancedScanner.get() && l2 < l) {
                    this.tickAdvanced(world, l);
                }
                while (!this.queue.isEmpty() && System.nanoTime() < l) {
                    long dequeueLong = this.queue.dequeueLong();
                    this.queued.remove(dequeueLong);
                    int packedX = ChunkPos.getPackedX(dequeueLong);
                    int packedZ = ChunkPos.getPackedZ(dequeueLong);
                    if (LevelUtil.isLoaded(world, packedX, packedZ)) {
                        ChunkScannerModule.Find find =
                                this.scan(world, packedX, packedZ, scanConfig);
                        if (this.sawBlob) {
                            this.probe(dequeueLong);
                        }
                        if (find != UNDECIDED) {
                            ChunkScannerModule.Find find2 = this.flagged.get(dequeueLong);
                            if (find == null
                                    ? find2 != null
                                    : find2 == null || !find2.sameAs(find)) {
                                this.refile(dequeueLong, find2, find);
                                if (find2 == null) {
                                    this.fresh.add(find);
                                }
                            }
                        }
                    }
                }
                this.announce(this.fresh);
                this.fresh.clear();
                if (!optimizationsEnabled || this.shownDirty || ++this.shownRefreshTicks >= 5) {
                    this.shown = this.computeShown(world);
                    this.shownDirty = false;
                    this.shownRefreshTicks = 0;
                }
            } finally {
                if (optimizationsEnabled) {
                    PerformanceBudget.record(l2);
                }
            }
        }
    }

    private void refile(long l, ChunkScannerModule.Find find, ChunkScannerModule.Find find2) {
        this.shownDirty = true;
        if (find != null) {
            this.spread(l, find, -1);
        }
        if (find2 == null) {
            this.flagged.remove(l);
        } else {
            this.flagged.put(l, find2);
            this.spread(l, find2, 1);
        }
    }

    private void spread(long l, ChunkScannerModule.Find find, int i) {
        int j = i * find.stacks().length;
        int appliedRadius2 = this.appliedRadius;
        int packedX = ChunkPos.getPackedX(l);
        int packedZ = ChunkPos.getPackedZ(l);
        for (int o = packedX - appliedRadius2; o <= packedX + appliedRadius2; o++) {
            for (int p = packedZ - appliedRadius2; p <= packedZ + appliedRadius2; p++) {
                long l2 = ChunkPos.toLong(o, p);
                if (this.heat.addTo(l2, j) + j == 0) {
                    this.heat.remove(l2);
                }
            }
        }
    }

    private LongOpenHashSet computeShown(ClientWorld world) {
        Long2IntOpenHashMap long2IntOpenHashMap = new Long2IntOpenHashMap();
        for (it.unimi.dsi.fastutil.longs.Long2IntMap.Entry entry :
                Long2IntMaps.fastIterable(this.heat)) {
            long longKey = entry.getLongKey();
            if (LevelUtil.isLoaded(
                    world, ChunkPos.getPackedX(longKey), ChunkPos.getPackedZ(longKey))) {
                long2IntOpenHashMap.put(longKey, entry.getIntValue());
            }
        }
        return this.appliedRadius == 0
                ? new LongOpenHashSet(long2IntOpenHashMap.keySet())
                : hottestPerPatch(long2IntOpenHashMap);
    }

    private static LongOpenHashSet hottestPerPatch(Long2IntOpenHashMap long2IntOpenHashMap) {
        LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
        LongOpenHashSet longOpenHashSet2 = new LongOpenHashSet();
        LongArrayList longArrayList = new LongArrayList();
        LongArrayFIFOQueue longArrayFIFOQueue = new LongArrayFIFOQueue();
        LongIterator longIterator = long2IntOpenHashMap.keySet().iterator();
        while (longIterator.hasNext()) {
            long l = longIterator.next();
            if (longOpenHashSet2.add(l)) {
                longArrayList.clear();
                longArrayFIFOQueue.enqueue(l);
                int i = 0;
                while (!longArrayFIFOQueue.isEmpty()) {
                    long dequeueLong = longArrayFIFOQueue.dequeueLong();
                    longArrayList.add(dequeueLong);
                    i = Math.max(i, long2IntOpenHashMap.get(dequeueLong));
                    int packedX = ChunkPos.getPackedX(dequeueLong);
                    int packedZ = ChunkPos.getPackedZ(dequeueLong);
                    for (int m = 0; m < 4; m++) {
                        long l2 = ChunkPos.toLong(packedX + SIDE_X[m], packedZ + SIDE_Z[m]);
                        if (long2IntOpenHashMap.containsKey(l2) && longOpenHashSet2.add(l2)) {
                            longArrayFIFOQueue.enqueue(l2);
                        }
                    }
                }
                for (int n = 0; n < longArrayList.size(); n++) {
                    if (long2IntOpenHashMap.get(longArrayList.getLong(n)) == i) {
                        longOpenHashSet.add(longArrayList.getLong(n));
                    }
                }
            }
        }
        return longOpenHashSet;
    }

    private ChunkScannerModule.Find scan(
            ClientWorld world, int i, int j, ChunkScannerModule.ScanConfig scanConfig) {
        for (int k = -1; k <= 1; k++) {
            for (int m = -1; m <= 1; m++) {
                this.around[(k + 1) * 3 + m + 1] =
                        world.getChunkManager().getWorldChunk(i + k, j + m);
            }
        }
        this.sawBlob = false;
        this.scanX = i << 4;
        this.scanZ = j << 4;
        this.keepHeight = scanConfig.stackHeight();
        this.keptStackCount = 0;
        this.tallest = 0;
        this.bottom = 0L;
        this.unsure = false;
        ChunkScannerModule.Find find;
        try {
            WorldChunk chunk = this.around[4];
            ChunkSection[] sectionArray = chunk.getSectionArray();
            int bottomSectionCoord = chunk.getBottomSectionCoord();
            Arrays.fill(this.stackTop, -2147483648);
            boolean bl = true;
            for (int o = 0; o < sectionArray.length; o++) {
                ChunkSection section = sectionArray[o];
                int n;
                if (section != null
                        && !section.isEmpty()
                        && (n = bottomSectionCoord + o << 4) + 15 >= scanConfig.minY()
                        && n <= scanConfig.maxY()) {
                    int p = Math.max(scanConfig.minY() - n, 0);
                    int q = Math.min(scanConfig.maxY() - n, 15);
                    if (bl) {
                        bl = !hasBlocks(section, p, q);
                    }
                    if (section.hasAny(blockState -> blockState.isOf(Blocks.STONE))
                            && section.hasAny(blockState -> isBlob(blockState.getBlock()))) {
                        for (int t = p; t <= q; t++) {
                            for (int u = 0; u < 16; u++) {
                                boolean bl2 = false;
                                for (int v = 0; v < 16; v++) {
                                    Block block = section.getBlockState(v, t, u).getBlock();
                                    boolean bl3 = block == Blocks.STONE;
                                    if (bl3) {
                                        if (!bl2 && this.isArea(v, n + t, u)) {
                                            this.stack(v, n + t, u);
                                        }
                                    } else if (this.probing && isBlob(block)) {
                                        this.sawBlob = true;
                                        if (this.enclosed(v, n + t, u)) {
                                            this.probing = false;
                                            this.probed.clear();
                                        }
                                    }
                                    bl2 = bl3;
                                }
                            }
                        }
                    }
                }
            }
            for (int w = 0; w < 256; w++) {
                this.keep(w);
            }
            if (this.tallest < scanConfig.stackHeight()) {
                return !this.unsure && !bl ? null : UNDECIDED;
            }
            ChunkScannerModule.Find find2 =
                    new ChunkScannerModule.Find(
                            this.bottom,
                            this.tallest,
                            Arrays.copyOf(this.keptStacks, this.keptStackCount));
            find = find2;
        } finally {
            Arrays.fill(this.around, null);
        }
        return find;
    }

    private void stack(int i, int j, int k) {
        int m = k << 4 | i;
        int shape = this.shape(i, k);
        boolean bl =
                this.stackTop[m] == j - 1
                        && this.stackShape[m] == shape
                        && this.stackWall[m] == this.wall;
        if (!bl) {
            this.keep(m);
        }
        this.stackTop[m] = j;
        this.stackShape[m] = shape;
        this.stackWall[m] = this.wall;
        this.stackAreas[m] = bl ? this.stackAreas[m] + 1 : 1;
        this.stackGuessed[m] = this.guessed || bl && this.stackGuessed[m];
    }

    private void keep(int i) {
        int stackArea = this.stackAreas[i];
        int j = this.stackTop[i];
        if (j != -2147483648 && stackArea >= this.keepHeight) {
            int k = this.stackShape[i];
            if (this.goesOn(i & 15, j + 1, i >> 4, k)
                    && this.goesOn(i & 15, j - stackArea, i >> 4, k)) {
                if (this.stackGuessed[i]) {
                    this.unsure = true;
                } else {
                    int m = this.scanX + (i & 15);
                    int n = this.scanZ + (i >> 4);
                    long l = BlockPos.asLong(m, j - stackArea + 1, n);
                    if (stackArea > this.tallest
                            || stackArea == this.tallest && before(l, this.bottom)) {
                        this.tallest = stackArea;
                        this.bottom = l;
                    }
                    if (this.keptStackCount == this.keptStacks.length) {
                        this.keptStacks =
                                Arrays.copyOf(this.keptStacks, this.keptStacks.length * 2);
                    }
                    this.keptStacks[this.keptStackCount++] = l;
                }
            }
        }
    }

    private boolean goesOn(int i, int j, int k, int m) {
        for (int n = 0; n < 6; n++) {
            if ((m >> n & 1) != 0) {
                this.kind(i + n / 3, j, k + n % 3 - 1);
                if (!carriesStone(this.lastBlock)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean before(long l, long l2) {
        if (BlockPos.unpackLongY(l) != BlockPos.unpackLongY(l2)) {
            return BlockPos.unpackLongY(l) < BlockPos.unpackLongY(l2);
        } else {
            return BlockPos.unpackLongZ(l) != BlockPos.unpackLongZ(l2)
                    ? BlockPos.unpackLongZ(l) < BlockPos.unpackLongZ(l2)
                    : BlockPos.unpackLongX(l) < BlockPos.unpackLongX(l2);
        }
    }

    private int shape(int i, int j) {
        int k = 0;
        for (int m = 0; m < this.groupSize; m++) {
            k |= 1 << (this.groupX[m] - i) * 3 + this.groupZ[m] - j + 1;
        }
        return k;
    }

    private boolean isArea(int i, int j, int k) {
        this.guessed = false;
        this.wall = null;
        if (this.seen(i - 1, j, k) == 2
                && this.sameWall()
                && this.seen(i, j, k - 1) == 2
                && this.sameWall()) {
            this.groupX[0] = i;
            this.groupZ[0] = k;
            int m = 1;
            int n = i;
            int o = k;
            int p = k;
            for (int q = 0; q < m; q++) {
                for (int r = 0; r < 4; r++) {
                    int t = this.groupX[q] + SIDE_X[r];
                    int u = this.groupZ[q] + SIDE_Z[r];
                    if (!this.inGroup(t, u, m)) {
                        byte seen = this.seen(t, j, u);
                        if (seen == 2) {
                            if (!this.sameWall()) {
                                return false;
                            }
                        } else {
                            if (seen != 1) {
                                return false;
                            }
                            if (t < i || t == i && u < k) {
                                return false;
                            }
                            n = Math.max(n, t);
                            o = Math.min(o, u);
                            p = Math.max(p, u);
                            if (n - i > 1 || p - o > 1) {
                                return false;
                            }
                            this.groupX[m] = t;
                            this.groupZ[m] = u;
                            m++;
                        }
                    }
                }
            }
            for (int v = 0; v < m; v++) {
                for (int w = 0; w < 4; w++) {
                    int i2 = this.groupX[v] + CORNER_X[w];
                    int j2 = this.groupZ[v] + CORNER_Z[w];
                    if (!this.inGroup(i2, j2, m)
                            && (this.seen(i2, j, j2) != 2 || !this.sameWall())) {
                        return false;
                    }
                }
            }
            for (int k2 = 0; k2 < m; k2++) {
                if (this.kind(this.groupX[k2], j + 1, this.groupZ[k2]) == 3
                        || this.kind(this.groupX[k2], j - 1, this.groupZ[k2]) == 3) {
                    return false;
                }
            }
            this.groupSize = m;
            return true;
        } else {
            return false;
        }
    }

    private boolean inGroup(int i, int j, int k) {
        for (int m = 0; m < k; m++) {
            if (this.groupX[m] == i && this.groupZ[m] == j) {
                return true;
            }
        }
        return false;
    }

    private byte seen(int i, int j, int k) {
        byte kind = this.kind(i, j, k);
        if (kind != 4) {
            return kind;
        } else {
            this.guessed = true;
            return 2;
        }
    }

    private boolean sameWall() {
        if (this.lastBlock == null) {
            return true;
        } else {
            if (this.wall == null) {
                this.wall = this.lastBlock;
            }
            return this.lastBlock == this.wall;
        }
    }

    private byte kind(int i, int j, int k) {
        WorldChunk chunk = this.around[((i >> 4) + 1) * 3 + (k >> 4) + 1];
        this.lastBlock = null;
        if (chunk == null) {
            return 4;
        } else {
            int sectionIndex = chunk.getSectionIndex(j);
            if (sectionIndex >= 0 && sectionIndex < chunk.countVerticalSections()) {
                BlockState blockState =
                        chunk.getSection(sectionIndex).getBlockState(i & 15, j & 15, k & 15);
                this.lastBlock = blockState.getBlock();
                return kindOf(blockState);
            } else {
                return 0;
            }
        }
    }

    private boolean enclosed(int i, int j, int k) {
        return this.full(i + 1, j, k)
                && this.full(i - 1, j, k)
                && this.full(i, j, k + 1)
                && this.full(i, j, k - 1)
                && this.full(i, j + 1, k)
                && this.full(i, j - 1, k);
    }

    private boolean full(int i, int j, int k) {
        WorldChunk chunk = this.around[((i >> 4) + 1) * 3 + (k >> 4) + 1];
        if (chunk == null) {
            return false;
        } else {
            int sectionIndex = chunk.getSectionIndex(j);
            return sectionIndex >= 0
                    && sectionIndex < chunk.countVerticalSections()
                    && chunk.getSection(sectionIndex)
                            .getBlockState(i & 15, j & 15, k & 15)
                            .isOpaqueFullCube();
        }
    }

    private static boolean hasBlocks(ChunkSection section, int i, int j) {
        if (!section.hasAny(blockState -> !blockState.isAir())) {
            return false;
        } else {
            for (int k = i; k <= j; k++) {
                for (int m = 0; m < 16; m++) {
                    for (int n = 0; n < 16; n++) {
                        if (!section.getBlockState(n, k, m).isAir()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private static byte kindOf(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.STONE) {
            return 1;
        } else if (isBlob(block)) {
            return 2;
        } else {
            return (byte) (state.isAir() ? 3 : 0);
        }
    }

    private static boolean isBlob(Block block) {
        return block == Blocks.DIORITE
                || block == Blocks.ANDESITE
                || block == Blocks.GRANITE
                || block == Blocks.GRAVEL;
    }

    private static boolean carriesStone(Block block) {
        return block == Blocks.STONE || block == Blocks.DEEPSLATE;
    }

    private static boolean matters(BlockState state) {
        return state != null && (kindOf(state) != 0 || carriesStone(state.getBlock()));
    }

    private void probe(long l) {
        if (this.probing && this.probed.add(l) && this.probed.size() >= 48) {
            this.probing = false;
            this.probed.clear();
            ModuleChat.send(
                    this,
                    ModuleChat.body()
                            .text("This server ")
                            .warn("hides")
                            .text(" ores with anti-xray, so patched shafts can't be seen"));
        }
    }

    private void announce(List<ChunkScannerModule.Find> list) {
        if (this.notificationsOn()) {
            if (!list.isEmpty()) {
                ItemStack stack =
                        new ItemStack(this.advancedScanner.get() ? Items.DIORITE : Items.SPYGLASS);
                ChallengeToast.addOrUpdate(
                        stack,
                        this.name(),
                        Text.literal("Suspect Chunk Scanned"),
                        this.sound.get());
            }
        }
    }

    private void tickAdvanced(ClientWorld world, long l) {
        String seed2 = this.seed.get();
        if (!seed2.equals(this.seedText)) {
            this.seedStill = this.seedText == null ? 20 : 0;
            this.seedText = seed2;
        } else if (this.seedStill < 20) {
            this.seedStill++;
        }
        if (this.seedStill >= 20) {
            OptionalLong seed3 = GeneratorOptions.parseSeed(seed2);
            if (seed3.isEmpty()) {
                if (this.advanced != null) {
                    this.startLevel(world);
                }
                if (!this.seedAsked) {
                    this.seedAsked = true;
                    ModuleChat.send(
                            this,
                            ModuleChat.body()
                                    .text("Advanced Scanner needs the world seed: ")
                                    .warn("set Seed"));
                }
                return;
            }
            this.seedAsked = false;
            if (this.advanced == null || this.advanced.seed() != seed3.getAsLong()) {
                if (this.advanced != null) {
                    this.startLevel(world);
                }
                this.advanced = AdvancedScanner.start(seed3.getAsLong());
            }
        }
        if (this.advanced != null) {
            for (AdvancedScanner.ChunkResult chunkResult : this.advanced.tick(world, l)) {
                ChunkScannerModule.Find find =
                        chunkResult.flagged()
                                ? new ChunkScannerModule.Find(
                                        chunkResult.anchors()[0],
                                        chunkResult.blocks(),
                                        chunkResult.anchors())
                                : null;
                ChunkScannerModule.Find find2 = this.flagged.get(chunkResult.chunk());
                if (find == null ? find2 != null : find2 == null || !find2.sameAs(find)) {
                    this.refile(chunkResult.chunk(), find2, find);
                    if (find2 == null && find != null) {
                        this.fresh.add(find);
                    }
                }
            }
            for (String drainNotice : this.advanced.drainNotices()) {
                ModuleChat.send(this, drainNotice);
            }
        }
    }

    private void closeAdvanced() {
        if (this.advanced != null) {
            this.advanced.close();
        }
        this.advanced = null;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (MinecraftClient.getInstance().world != null) {
            this.chunkOverlay.draw(this.shown, this.chunkColor.argb(), event.partialTick);
            if (this.tracers.get()) {
                this.chunkAreas.drawTracers(
                        this.shown, 63.0, ColorHelper.fullAlpha(this.chunkColor.argb()));
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private record Find(long pos, int height, long[] stacks) {

        boolean sameAs(ChunkScannerModule.Find find) {
            return this.pos == find.pos
                    && this.height == find.height
                    && Arrays.equals(this.stacks, find.stacks);
        }
    }

    @Environment(EnvType.CLIENT)
    private record ScanConfig(int minY, int maxY, int stackHeight) {}
}
