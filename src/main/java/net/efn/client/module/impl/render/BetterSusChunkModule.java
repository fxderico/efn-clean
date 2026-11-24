package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
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
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.ChunkOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PropaguleBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome.Precipitation;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightProvider;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Environment(EnvType.CLIENT)
public final class BetterSusChunkModule extends ClientModule {

    private static final String DONUT_BRAND = "DonutFolia";

    private static final int KELP = 0;

    private static final int CAVE_VINES = 1;

    private static final int WEEPING_VINES = 2;

    private static final int BAMBOO = 3;

    private static final int SUGAR_CANE = 4;

    private static final int CACTUS = 5;

    private static final int COCOA = 6;

    private static final int BERRY = 7;

    private static final int NETHER_WART = 8;

    private static final int CROP = 9;

    private static final int STEM = 10;

    private static final int AMETHYST = 11;

    private static final int VINE = 12;

    private static final int LEAVES = 13;

    private static final int SOIL = 14;

    private static final int PROPAGULE = 15;

    private static final int EYEBLOSSOM = 16;

    private static final int SNOW = 17;

    private static final int ICE = 18;

    private static final int EVIDENCE_KINDS = 19;

    private static final int GROWTH_LIGHT = 9;

    private static final int FULL_LIGHT = 15;

    private static final int MELT_LIGHT = 10;

    private static final int BAMBOO_LIMIT = 16;

    private static final int SHORT_COLUMN = 3;

    private static final int KELP_MAX_AGE = 25;

    private static final int DAY_TICKS = 24000;

    private static final int EYEBLOSSOM_OPENS = 12600;

    private static final int EYEBLOSSOM_CLOSES = 23401;

    private static final int EYEBLOSSOM_SETTLE = 4000;

    private static final int EYEBLOSSOM_MIN = 3;

    private static final int HOLE_DEPTH = 2;

    private static final int ENCLOSED_SIDES = 3;

    private static final int REFRESH_TICKS = 20;

    private static final int LOADED_MARGIN = 3;

    private static final Direction[] DIRECTIONS = Direction.values();

    private static final int[][] SIDES = new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private static final int FILL_ALPHA = 100;

    private final NumberSetting simDistance =
            this.addSetting(
                    "sim-distance",
                    new NumberSetting("Simulation Distance", 4.0F, 2.0F, 16.0F).step(1.0F));

    private final ColorSetting chunkColor =
            this.addSetting("chunk-color", new ColorSetting("Chunk Color", -65536));

    private final NumberSetting sensitivity =
            this.addSetting(
                    "min-flag", new NumberSetting("Sensitivity", 40.0F, 1.0F, 500.0F).step(1.0F));

    private final BooleanSetting holeFilter =
            this.addSetting("hole-filter", new BooleanSetting("Hole Filter", false));

    private final NumberSetting holeDistance =
            this.addSetting(
                    "hole-distance",
                    new NumberSetting("Hole Distance", 2.0F, 0.0F, 8.0F).step(1.0F));

    private final BooleanSetting customEvidence =
            this.addSetting("custom-evidence", new BooleanSetting("Custom Flags", false));

    private final BooleanSetting[] evidence =
            new BooleanSetting[] {
                this.evidence("count-kelp", "Kelp"),
                this.evidence("count-cave-vines", "Cave Vines"),
                this.evidence("count-weeping-vines", "Weeping Vines"),
                this.evidence("count-bamboo", "Bamboo"),
                this.evidence("count-sugar-cane", "Sugar Cane"),
                this.evidence("count-cactus", "Cactus"),
                this.evidence("count-cocoa", "Cocoa"),
                this.evidence("count-sweet-berries", "Sweet Berries"),
                this.evidence("count-nether-wart", "Nether Wart"),
                this.evidence("count-crops", "Crops"),
                this.evidence("count-stems", "Stems"),
                this.evidence("count-amethyst", "Amethyst"),
                this.evidence("count-vines", "Vines"),
                this.evidence("count-leaves", "Leaves"),
                this.evidence("count-grass-dirt", "Grass & Dirt"),
                this.evidence("count-propagules", "Mangrove Propagules"),
                this.evidence("count-eyeblossoms", "Eyeblossoms"),
                this.evidence("count-snow", "Snow"),
                this.evidence("count-ice", "Ice")
            };

    private final ChunkOverlay primaryOverlay = new ChunkOverlay();

    private final Object lock = new Object();

    private final Long2ObjectOpenHashMap<BetterSusChunkModule.Record> records =
            new Long2ObjectOpenHashMap<>();

    private final Long2IntOpenHashMap clearCoverage = new Long2IntOpenHashMap();

    private final Long2IntOpenHashMap support = new Long2IntOpenHashMap();

    private final LongOpenHashSet scanned = new LongOpenHashSet();

    private final LongArrayList distant = new LongArrayList();

    private final Set<Long> pending = ConcurrentHashMap.newKeySet();

    private final Set<Long> kelpChunks = ConcurrentHashMap.newKeySet();

    private final Set<Long> seenKelp = ConcurrentHashMap.newKeySet();

    private volatile LongSet primary = LongSets.EMPTY_SET;

    private volatile int generation;

    private volatile boolean dirty;

    private volatile boolean[] counted = new boolean[19];

    private ExecutorService executor;

    private RegistryKey<World> worldKey;

    private int tickCounter;

    public BetterSusChunkModule() {
        super(
                "better-sus-chunk",
                "Better Sus Chunk",
                "Marks chunks no player has been near: areas with overdue natural growth rule"
                        + " themselves out.",
                ModuleCategory.BASE_RAID);
        this.holeDistance.visibleWhen(this.holeFilter::get);
        this.simDistance.onChange(() -> this.dirty = true);
        this.holeFilter.onChange(
                () -> {
                    if (this.holeFilter.get()) {
                        this.dirty = true;
                    }
                });
        this.customEvidence.onChange(this::refreshEvidence);
        for (BooleanSetting setting : this.evidence) {
            setting.visibleWhen(this.customEvidence::get);
            setting.onChange(this::refreshEvidence);
        }
        this.refreshEvidence();
    }

    private BooleanSetting evidence(String string, String string2) {
        return this.addSetting(string, new BooleanSetting(string2, true));
    }

    private void refreshEvidence() {
        boolean customEvidence2 = this.customEvidence.get();
        boolean[] booleans = new boolean[19];
        for (int i = 0; i < booleans.length; i++) {
            booleans[i] = !customEvidence2 || this.evidence[i].get();
        }
        this.counted = booleans;
        this.dirty = true;
    }

    @Override
    protected void onEnable() {
        this.resetState();
        this.executor =
                Executors.newSingleThreadExecutor(
                        runnable -> {
                            Thread thread = new Thread(runnable, "EFN Better Sus Chunk");
                            thread.setDaemon(true);
                            return thread;
                        });
        ClientWorld world = MinecraftClient.getInstance().world;
        this.worldKey = world == null ? null : world.getRegistryKey();
        this.dirty = false;
        this.rescanLoadedChunks();
    }

    @Override
    protected void onDisable() {
        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }
        this.resetState();
        this.worldKey = null;
    }

    private void resetState() {
        Object lock2 = this.lock;
        synchronized (lock2) {
            this.generation++;
            this.clearCoverage.clear();
            this.support.clear();
            this.records.clear();
            this.scanned.clear();
            this.pending.clear();
            this.kelpChunks.clear();
            this.seenKelp.clear();
        }
        this.primary = LongSets.EMPTY_SET;
        this.tickCounter = 0;
    }

    private void rescanLoadedChunks() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (this.isEnabled() && client.player != null && world != null) {
            this.resetState();
            int i = client.options.getClampedViewDistance() + 3;
            ChunkPos chunkPos = client.player.getChunkPos();
            for (int j = chunkPos.x - i; j <= chunkPos.x + i; j++) {
                for (int k = chunkPos.z - i; k <= chunkPos.z + i; k++) {
                    if (world.isChunkLoaded(j, k)) {
                        this.scheduleScan(ChunkPos.toLong(j, k));
                    }
                }
            }
        }
    }

    private void scheduleScan(long l) {
        ExecutorService executor2 = this.executor;
        if (executor2 != null && !executor2.isShutdown()) {
            if (this.pending.add(l)) {
                int generation2 = this.generation;
                executor2.submit(() -> this.runScan(l, generation2));
            }
        }
    }

    private void runScan(long l, int i) {
        this.pending.remove(l);
        if (i == this.generation && this.isEnabled()) {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                int packedX = ChunkPos.getPackedX(l);
                int packedZ;
                if (world.isChunkLoaded(packedX, packedZ = ChunkPos.getPackedZ(l))) {
                    WorldChunk chunk2 = world.getChunk(packedX, packedZ);
                    if (chunk2 != null) {
                        this.rescan(world, l, chunk2, i);
                    }
                }
            }
        }
    }

    private void rescan(ClientWorld world, long l, WorldChunk chunk2, int i) {
        BetterSusChunkModule.Record recordValue = this.removeRecord(l);
        BetterSusChunkModule.Source source = this.scanChunk(world, l, chunk2);
        Object lock2 = this.lock;
        synchronized (lock2) {
            if (i == this.generation) {
                BetterSusChunkModule.Record recordValue2 =
                        new BetterSusChunkModule.Record(
                                source.clears() || recordValue != null && recordValue.clears(),
                                this.simDistance.getInt(),
                                source.hole() || recordValue != null && recordValue.hole(),
                                Math.max(
                                        source.weight(),
                                        recordValue == null ? 0 : recordValue.weight()));
                this.scanned.add(l);
                this.records.put(l, recordValue2);
                if (recordValue2.clears()) {
                    int packedX = ChunkPos.getPackedX(l);
                    int packedZ = ChunkPos.getPackedZ(l);
                    int j = 2 * recordValue2.radius() + 2;
                    for (int n = -recordValue2.radius(); n <= recordValue2.radius(); n++) {
                        for (int o = -recordValue2.radius(); o <= recordValue2.radius(); o++) {
                            this.clearCoverage.addTo(ChunkPos.toLong(packedX + n, packedZ + o), 1);
                        }
                    }
                    for (int p = -j; p <= j; p++) {
                        for (int q = -j; q <= j; q++) {
                            this.support.addTo(
                                    ChunkPos.toLong(packedX + p, packedZ + q),
                                    recordValue2.weight());
                        }
                    }
                }
            }
        }
    }

    private BetterSusChunkModule.Record removeRecord(long l) {
        Object lock2 = this.lock;
        synchronized (lock2) {
            BetterSusChunkModule.Record recordValue = this.records.remove(l);
            this.scanned.remove(l);
            if (recordValue != null && recordValue.clears()) {
                int packedX = ChunkPos.getPackedX(l);
                int packedZ = ChunkPos.getPackedZ(l);
                int i = 2 * recordValue.radius() + 2;
                for (int m = -recordValue.radius(); m <= recordValue.radius(); m++) {
                    for (int n = -recordValue.radius(); n <= recordValue.radius(); n++) {
                        subtract(this.clearCoverage, ChunkPos.toLong(packedX + m, packedZ + n), 1);
                    }
                }
                for (int o = -i; o <= i; o++) {
                    for (int p = -i; p <= i; p++) {
                        subtract(
                                this.support,
                                ChunkPos.toLong(packedX + o, packedZ + p),
                                recordValue.weight());
                    }
                }
                return recordValue;
            } else {
                return recordValue;
            }
        }
    }

    private static void subtract(Long2IntOpenHashMap long2IntOpenHashMap, long l, int i) {
        if (long2IntOpenHashMap.containsKey(l)) {
            int j = long2IntOpenHashMap.get(l);
            if (j <= i) {
                long2IntOpenHashMap.remove(l);
            } else {
                long2IntOpenHashMap.put(l, j - i);
            }
        }
    }

    private BetterSusChunkModule.Source scanChunk(ClientWorld world, long l, WorldChunk chunk2) {
        boolean donutServer = donutServer();
        boolean[] counted2 = this.counted;
        int i = donutServer && counted2[0] && this.kelpChunks.contains(l) ? 1 : 0;
        BetterSusChunkModule.BlockReader blockReader = new BetterSusChunkModule.BlockReader(world);
        ChunkPos chunkPos = chunk2.getPos();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        Boolean flag = eyeblossomsOpen(world);
        int j = 0;
        int k = 0;
        ChunkSection[] sectionArray = chunk2.getSectionArray();
        for (int o = 0; o < sectionArray.length; o++) {
            ChunkSection section = sectionArray[o];
            if (section != null
                    && !section.isEmpty()
                    && section.hasAny(blockState -> kindOf(blockState, counted2) >= 0)) {
                int m = chunk2.getBottomY() + (o << 4);
                for (int q = 0; q < 16; q++) {
                    for (int r = 0; r < 16; r++) {
                        for (int t = 0; t < 16; t++) {
                            BlockState blockState2 = section.getBlockState(t, q, r);
                            int kind = kindOf(blockState2, counted2);
                            if (kind >= 0) {
                                if (kind == 15) {
                                    if (blockState2.get(PropaguleBlock.AGE) < 4) {
                                        i++;
                                    }
                                } else if (kind == 16) {
                                    if (flag != null) {
                                        if (blockState2.isOf(Blocks.OPEN_EYEBLOSSOM) == flag) {
                                            k++;
                                        } else {
                                            j++;
                                        }
                                    }
                                } else if (owed(
                                        blockReader,
                                        startX + t,
                                        m + q,
                                        startZ + r,
                                        blockState2,
                                        kind,
                                        donutServer)) {
                                    i++;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (k == 0 && j >= 3) {
            i += j;
        }
        if ((counted2[17] || counted2[18]) && chunk2.hasHeightmap(Type.MOTION_BLOCKING)) {
            Heightmap heightmap = chunk2.getHeightmap(Type.MOTION_BLOCKING);
            int seaLevel = world.getSeaLevel();
            for (int w = 0; w < 16; w++) {
                for (int i2 = 0; i2 < 16; i2++) {
                    int n = heightmap.get(i2, w);
                    if (surfaceOwed(
                            world,
                            blockReader,
                            startX + i2,
                            n,
                            startZ + w,
                            seaLevel,
                            donutServer,
                            counted2)) {
                        i++;
                    }
                }
            }
        }
        boolean bl = this.holeFilter.get() && hasHole(world, chunkPos);
        return new BetterSusChunkModule.Source(i > 0, bl, i);
    }

    private static int kindOf(BlockState state, boolean[] booleans) {
        int classify = classify(state);
        return classify >= 0 && booleans[classify] ? classify : -1;
    }

    private static int classify(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.KELP) {
            return 0;
        } else if (block == Blocks.CAVE_VINES) {
            return 1;
        } else if (block == Blocks.WEEPING_VINES) {
            return 2;
        } else if (block == Blocks.BAMBOO) {
            return 3;
        } else if (block == Blocks.SUGAR_CANE) {
            return 4;
        } else if (block == Blocks.CACTUS) {
            return 5;
        } else if (block == Blocks.COCOA) {
            return 6;
        } else if (block == Blocks.SWEET_BERRY_BUSH) {
            return 7;
        } else if (block == Blocks.NETHER_WART) {
            return 8;
        } else if (block == Blocks.PUMPKIN_STEM || block == Blocks.MELON_STEM) {
            return 10;
        } else if (block == Blocks.VINE) {
            return 12;
        } else if (block == Blocks.BUDDING_AMETHYST
                || block == Blocks.AMETHYST_CLUSTER
                || isBud(state)) {
            return 11;
        } else if (block == Blocks.MANGROVE_PROPAGULE) {
            return state.get(PropaguleBlock.HANGING) ? 15 : -1;
        } else if (block == Blocks.OPEN_EYEBLOSSOM || block == Blocks.CLOSED_EYEBLOSSOM) {
            return 16;
        } else if (block instanceof CropBlock) {
            return 9;
        } else if (block instanceof LeavesBlock) {
            return 13;
        } else {
            return !isGrass(state) && !state.isOf(Blocks.DIRT) ? -1 : 14;
        }
    }

    private static Boolean eyeblossomsOpen(ClientWorld world) {
        if (world.getRegistryKey() == World.OVERWORLD && !world.getDimension().hasFixedTime()) {
            int i = Math.floorMod(world.getTimeOfDay(), 24000);
            boolean bl = i >= 12600 && i < 23401;
            int j = bl ? i - 12600 : Math.floorMod(i - 23401, 24000);
            return j >= 4000 ? bl : null;
        } else {
            return null;
        }
    }

    private static boolean owed(
            BetterSusChunkModule.BlockReader blockReader,
            int i,
            int j,
            int k,
            BlockState state,
            int m,
            boolean bl) {
        if (m == 14) {
            return soilOwed(blockReader, i, j, k, state);
        } else {
            if (bl) {
                if (m == 0) {
                    return blockReader.state(i, j + 1, k).isOf(Blocks.WATER)
                            && column(blockReader, i, j, k, Blocks.KELP_PLANT, 3) < 3;
                }
                if (m == 11) {
                    return isBud(state);
                }
                if (m == 1) {
                    return false;
                }
            }
            return growthOwed(blockReader, i, j, k, state);
        }
    }

    private static boolean growthOwed(
            BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.KELP) {
            return state.get(Properties.AGE_25) < 25
                    && blockReader.state(i, j + 1, k).isOf(Blocks.WATER);
        } else if (block == Blocks.CAVE_VINES || block == Blocks.WEEPING_VINES) {
            return state.get(Properties.AGE_25) < 25 && blockReader.state(i, j - 1, k).isAir();
        } else if (block == Blocks.BAMBOO) {
            return state.get(Properties.STAGE) == 0
                    && blockReader.state(i, j + 1, k).isAir()
                    && bambooBelow(blockReader, i, j, k) + 1 < 16
                    && blockReader.light(i, j + 1, k) >= 9;
        } else if (block == Blocks.SUGAR_CANE) {
            return blockReader.state(i, j + 1, k).isAir()
                    && column(blockReader, i, j, k, Blocks.SUGAR_CANE, 3) < 3;
        } else if (block == Blocks.CACTUS) {
            return blockReader.state(i, j + 1, k).isAir()
                    && (state.get(Properties.AGE_15) < 15
                            || column(blockReader, i, j, k, Blocks.CACTUS, 3) < 3);
        } else if (block == Blocks.COCOA) {
            return state.get(Properties.AGE_2) < 2;
        } else if (block == Blocks.NETHER_WART) {
            return state.get(Properties.AGE_3) < 3;
        } else if (block == Blocks.SWEET_BERRY_BUSH) {
            return state.get(Properties.AGE_3) < 3 && blockReader.light(i, j + 1, k) >= 9;
        } else if (block == Blocks.PUMPKIN_STEM || block == Blocks.MELON_STEM) {
            return state.get(Properties.AGE_7) < 7 && blockReader.light(i, j, k) >= 9;
        } else if (block instanceof CropBlock cropBlock) {
            return !cropBlock.isMature(state) && blockReader.light(i, j, k) >= 9;
        } else if (block == Blocks.VINE) {
            return blockReader.state(i, j - 1, k).isAir()
                    && (state.get(Properties.NORTH)
                            || state.get(Properties.EAST)
                            || state.get(Properties.SOUTH)
                            || state.get(Properties.WEST));
        } else {
            return !(block instanceof LeavesBlock)
                    ? amethystOwed(blockReader, i, j, k, state)
                    : state.contains(Properties.DISTANCE_1_7)
                            && state.contains(Properties.PERSISTENT)
                            && state.get(Properties.DISTANCE_1_7) == 7
                            && !state.get(Properties.PERSISTENT);
        }
    }

    private static boolean amethystOwed(
            BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, BlockState state) {
        if (state.isOf(Blocks.BUDDING_AMETHYST)) {
            for (Direction direction : DIRECTIONS) {
                BlockState state2 =
                        blockReader.state(
                                i + direction.getOffsetX(),
                                j + direction.getOffsetY(),
                                k + direction.getOffsetZ());
                if (state2.isAir()) {
                    return true;
                }
                if (state2.isOf(Blocks.WATER) && state2.getFluidState().getLevel() == 8) {
                    return true;
                }
            }
            return false;
        } else if (isBud(state) && state.contains(Properties.FACING)) {
            Direction direction2 = state.get(Properties.FACING);
            return blockReader
                    .state(
                            i - direction2.getOffsetX(),
                            j - direction2.getOffsetY(),
                            k - direction2.getOffsetZ())
                    .isOf(Blocks.BUDDING_AMETHYST);
        } else {
            return false;
        }
    }

    private static int column(
            BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, Block block, int m) {
        int n = 1;
        while (n < m && blockReader.state(i, j - n, k).isOf(block)) {
            n++;
        }
        return n;
    }

    private static int bambooBelow(
            BetterSusChunkModule.BlockReader blockReader, int i, int j, int k) {
        int m = 0;
        while (m < 16 && blockReader.state(i, j - m - 1, k).isOf(Blocks.BAMBOO)) {
            m++;
        }
        return m;
    }

    private static boolean soilOwed(
            BetterSusChunkModule.BlockReader blockReader, int i, int j, int k, BlockState state) {
        BlockState state2 = blockReader.state(i, j + 1, k);
        if (isGrass(state)) {
            return smothers(state2);
        } else if (state.isOf(Blocks.DIRT)
                && !smothers(state2)
                && !state2.getFluidState().isIn(FluidTags.WATER)) {
            for (int m = -1; m <= 1; m++) {
                for (int n = -1; n <= 1; n++) {
                    for (int o = j - 1; o <= j + 3; o++) {
                        if ((m != 0 || n != 0 || o != j)
                                && isGrass(blockReader.state(i + m, o, k + n))
                                && !smothers(blockReader.state(i + m, o + 1, k + n))
                                && blockReader.light(i + m, o + 1, k + n) >= 9) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private static boolean smothers(BlockState state) {
        if (state.isOf(Blocks.SNOW) && state.get(SnowBlock.LAYERS) == 1) {
            return false;
        } else {
            return state.getFluidState().getLevel() == 8
                    ? true
                    : ChunkLightProvider.getRealisticOpacity(
                                    Blocks.GRASS_BLOCK.getDefaultState(),
                                    state,
                                    Direction.UP,
                                    state.getOpacity())
                            >= 15;
        }
    }

    private static boolean surfaceOwed(
            ClientWorld world,
            BetterSusChunkModule.BlockReader blockReader,
            int i,
            int j,
            int k,
            int m,
            boolean bl,
            boolean[] booleans) {
        boolean bl2 =
                booleans[17] && (!bl || !blockReader.state(i, j - 1, k).isOf(Blocks.DEEPSLATE));
        return bl2 && snowOwed(world, blockReader, i, j, k, m)
                || booleans[18] && iceOwed(world, blockReader, i, j, k, m);
    }

    private static boolean snowOwed(
            ClientWorld world,
            BetterSusChunkModule.BlockReader blockReader,
            int i,
            int j,
            int k,
            int m) {
        if (!world.isOutOfHeightLimit(j) && blockReader.state(i, j, k).isAir()) {
            if (!holdsSnow(world, blockReader.state(i, j - 1, k), new BlockPos(i, j - 1, k))) {
                return false;
            } else {
                BlockPos pos = new BlockPos(i, j, k);
                return world.getBiome(pos).value().getPrecipitation(pos, m) != Precipitation.SNOW
                        ? false
                        : calmLight(world, blockReader, i, j, k);
            }
        } else {
            return false;
        }
    }

    private static boolean holdsSnow(ClientWorld world, BlockState state, BlockPos pos) {
        if (state.isIn(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON)) {
            return false;
        } else if (state.isIn(BlockTags.SNOW_LAYER_CAN_SURVIVE_ON)) {
            return true;
        } else {
            return state.isOf(Blocks.SNOW) && state.get(SnowBlock.LAYERS) == 8
                    ? true
                    : Block.isFaceFullSquare(state.getCollisionShape(world, pos), Direction.UP);
        }
    }

    private static boolean iceOwed(
            ClientWorld world,
            BetterSusChunkModule.BlockReader blockReader,
            int i,
            int j,
            int k,
            int m) {
        int n = j - 1;
        if (world.isOutOfHeightLimit(n)) {
            return false;
        } else {
            BlockState state = blockReader.state(i, n, k);
            if (state.getBlock() instanceof FluidBlock
                    && state.getFluidState().getFluid() == Fluids.WATER) {
                if (!world.isChunkLoaded(i - 1 >> 4, k >> 4)
                        || !world.isChunkLoaded(i + 1 >> 4, k >> 4)
                        || !world.isChunkLoaded(i >> 4, k - 1 >> 4)
                        || !world.isChunkLoaded(i >> 4, k + 1 >> 4)) {
                    return false;
                } else if (isWater(blockReader, i - 1, n, k)
                        && isWater(blockReader, i + 1, n, k)
                        && isWater(blockReader, i, n, k - 1)
                        && isWater(blockReader, i, n, k + 1)) {
                    return false;
                } else {
                    return world.getBiome(new BlockPos(i, j, k))
                                    .value()
                                    .doesNotSnow(new BlockPos(i, n, k), m)
                            ? false
                            : calmLight(world, blockReader, i, n, k);
                }
            } else {
                return false;
            }
        }
    }

    private static boolean isWater(
            BetterSusChunkModule.BlockReader blockReader, int i, int j, int k) {
        return blockReader.state(i, j, k).getFluidState().isIn(FluidTags.WATER);
    }

    private static boolean calmLight(
            ClientWorld world, BetterSusChunkModule.BlockReader blockReader, int i, int j, int k) {
        byte b = 5;
        for (int m = i - b >> 4; m <= i + b >> 4; m++) {
            for (int n = k - b >> 4; n <= k + b >> 4; n++) {
                if (!world.isChunkLoaded(m, n)) {
                    return false;
                }
            }
        }
        for (int o = -b; o <= b; o++) {
            int p = b - Math.abs(o);
            for (int q = -p; q <= p; q++) {
                int r = p - Math.abs(q);
                for (int t = -r; t <= r; t++) {
                    int luminance = blockReader.state(i + o, j + q, k + t).getLuminance();
                    if (luminance != 0
                            && luminance - Math.abs(o) - Math.abs(q) - Math.abs(t) >= 10) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean hasHole(ClientWorld world, ChunkPos chunkPos) {
        BetterSusChunkModule.BlockReader blockReader = new BetterSusChunkModule.BlockReader(world);
        int bottomY = world.getBottomY();
        int i = bottomY + world.getHeight();
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        for (int n = startX; n < startX + 16; n++) {
            for (int o = startZ; o < startZ + 16; o++) {
                int j = 0;
                int k = 0;
                int m = 0;
                for (int t = bottomY; t < i; t++) {
                    if (singleShaft(world, blockReader, n, t, o)) {
                        j++;
                    } else {
                        if (shaftEnds(world, blockReader, n, t, o, j)) {
                            return true;
                        }
                        j = 0;
                    }
                    if (wideShaft(world, blockReader, n, t, o, 1, 0)) {
                        k++;
                    } else {
                        if (shaftEnds(world, blockReader, n, t, o, k)) {
                            return true;
                        }
                        k = 0;
                    }
                    if (wideShaft(world, blockReader, n, t, o, 0, 1)) {
                        m++;
                    } else {
                        if (shaftEnds(world, blockReader, n, t, o, m)) {
                            return true;
                        }
                        m = 0;
                    }
                }
            }
        }
        return false;
    }

    private static boolean singleShaft(
            ClientWorld world, BetterSusChunkModule.BlockReader blockReader, int i, int j, int k) {
        return open(world, blockReader, i, j, k)
                && !open(world, blockReader, i - 1, j, k)
                && !open(world, blockReader, i + 1, j, k)
                && !open(world, blockReader, i, j, k - 1)
                && !open(world, blockReader, i, j, k + 1);
    }

    private static boolean wideShaft(
            ClientWorld world,
            BetterSusChunkModule.BlockReader blockReader,
            int i,
            int j,
            int k,
            int m,
            int n) {
        for (int o = 0; o < 3; o++) {
            if (!open(world, blockReader, i + m * o, j, k + n * o)) {
                return false;
            }
        }
        if (!open(world, blockReader, i - m, j, k - n)
                && !open(world, blockReader, i + m * 3, j, k + n * 3)) {
            for (int p = 0; p < 3; p++) {
                int q = i + m * p;
                int r = k + n * p;
                if (open(world, blockReader, q - n, j, r - m)
                        || open(world, blockReader, q + n, j, r + m)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private static boolean shaftEnds(
            ClientWorld world,
            BetterSusChunkModule.BlockReader blockReader,
            int i,
            int j,
            int k,
            int m) {
        return m >= 2
                && open(world, blockReader, i, j, k)
                && !open(world, blockReader, i, j - m - 1, k);
    }

    private static boolean open(
            ClientWorld world, BetterSusChunkModule.BlockReader blockReader, int i, int j, int k) {
        return blockReader.state(i, j, k).getCollisionShape(world, BlockPos.ORIGIN).isEmpty();
    }

    private static boolean isBud(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.SMALL_AMETHYST_BUD
                || block == Blocks.MEDIUM_AMETHYST_BUD
                || block == Blocks.LARGE_AMETHYST_BUD;
    }

    private static boolean isGrass(BlockState state) {
        return state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.MYCELIUM);
    }

    private static boolean donutServer() {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == null) {
            return false;
        } else {
            String brand = networkHandler.getBrand();
            return brand != null && brand.contains("DonutFolia");
        }
    }

    @EventHandler
    public void onPacketApplied(PacketAppliedEvent event) {
        if (event.packet instanceof ChunkDataS2CPacket chunkDataPacket) {
            this.scheduleScan(
                    ChunkPos.toLong(chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ()));
        } else if (event.packet instanceof GameJoinS2CPacket) {
            this.resetState();
            this.worldKey = null;
        }
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent event) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null && this.counted[0] && event.newState.isOf(Blocks.KELP) && donutServer()) {
            int i = event.newState.get(Properties.AGE_25);
            if (this.seenKelp.add(event.pos.asLong())) {
                if (i > 0 && i < 25) {
                    if (world.getBlockState(event.pos.up()).isOf(Blocks.WATER)) {
                        long l = ChunkPos.toLong(event.pos.getX() >> 4, event.pos.getZ() >> 4);
                        if (this.kelpChunks.add(l)) {
                            this.scheduleScan(l);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world != null && client.player != null) {
            RegistryKey registryKey = world.getRegistryKey();
            if (registryKey != this.worldKey) {
                this.resetState();
                this.worldKey = registryKey;
            }
            if (this.dirty) {
                this.dirty = false;
                this.rescanLoadedChunks();
            }
            this.pruneDistant(client);
            if (++this.tickCounter >= 20) {
                this.tickCounter = 0;
                this.rebuildDisplaySets(world);
            }
        }
    }

    private void pruneDistant(MinecraftClient client) {
        int i = client.options.getClampedViewDistance() + 2 * this.simDistance.getInt() + 2;
        int j = client.player.getBlockX() >> 4;
        int k = client.player.getBlockZ() >> 4;
        Object lock2 = this.lock;
        synchronized (lock2) {
            this.distant.clear();
            LongIterator longIterator = this.records.keySet().iterator();
            while (longIterator.hasNext()) {
                long l = longIterator.nextLong();
                if (Math.abs(ChunkPos.getPackedX(l) - j) > i
                        || Math.abs(ChunkPos.getPackedZ(l) - k) > i) {
                    this.distant.add(l);
                }
            }
            for (int m = 0; m < this.distant.size(); m++) {
                this.removeRecord(this.distant.getLong(m));
            }
        }
        this.kelpChunks.removeIf(
                long_ ->
                        Math.abs(ChunkPos.getPackedX(long_) - j) > i
                                || Math.abs(ChunkPos.getPackedZ(long_) - k) > i);
        this.seenKelp.removeIf(
                long_ ->
                        Math.abs((BlockPos.unpackLongX(long_) >> 4) - j) > i
                                || Math.abs((BlockPos.unpackLongZ(long_) >> 4) - k) > i);
    }

    private void rebuildDisplaySets(ClientWorld world) {
        boolean holeFilter2 = this.holeFilter.get();
        int sensitivity2 = this.sensitivity.getInt();
        int holeDistance2 = this.holeDistance.getInt();
        int i = 2 * this.simDistance.getInt() + 2;
        LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
        Object lock2 = this.lock;
        synchronized (lock2) {
            LongIterator longIterator = this.scanned.iterator();
            while (longIterator.hasNext()) {
                long l = longIterator.nextLong();
                int packedX = ChunkPos.getPackedX(l);
                int packedZ;
                if (world.isChunkLoaded(packedX, packedZ = ChunkPos.getPackedZ(l))
                        && !this.clearCoverage.containsKey(l)
                        && (!holeFilter2 || !this.holeNearby(packedX, packedZ, holeDistance2))
                        && this.support.get(l) >= sensitivity2
                        && this.coveredSides(packedX, packedZ, i) >= 3) {
                    longOpenHashSet.add(l);
                }
            }
        }
        this.primary = longOpenHashSet;
    }

    private boolean holeNearby(int i, int j, int k) {
        for (int m = -k; m <= k; m++) {
            for (int n = -k; n <= k; n++) {
                BetterSusChunkModule.Record recordValue =
                        this.records.get(ChunkPos.toLong(i + m, j + n));
                if (recordValue != null && recordValue.hole()) {
                    return true;
                }
            }
        }
        return false;
    }

    private int coveredSides(int i, int j, int k) {
        int m = 0;
        for (int[] ints : SIDES) {
            if (this.stripCleared(i, j, ints[0], ints[1], k)) {
                m++;
            }
        }
        return m;
    }

    private boolean stripCleared(int i, int j, int k, int m, int n) {
        for (int o = 1; o <= n; o++) {
            for (int p = -n; p <= n; p++) {
                long l = ChunkPos.toLong(i + k * o + m * p, j + m * o + k * p);
                if (this.clearCoverage.containsKey(l)) {
                    return true;
                }
            }
        }
        return false;
    }

    public LongSet flaggedChunks() {
        return LongSets.unmodifiable(this.primary);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (MinecraftClient.getInstance().world != null) {
            this.primaryOverlay.drawTiles(
                    this.primary,
                    this.chunkColor.argbWithAlpha(100),
                    this.chunkColor.argbWithAlpha(255),
                    event.partialTick);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class BlockReader {

        private final ClientWorld level;

        private WorldChunk chunk;

        private BlockReader(ClientWorld level2) {
            this.level = level2;
        }

        private BlockState state(int i, int j, int k) {
            if (this.level.isOutOfHeightLimit(j)) {
                return Blocks.VOID_AIR.getDefaultState();
            } else {
                WorldChunk chunk2 = this.chunkAt(i >> 4, k >> 4);
                if (chunk2 == null) {
                    return Blocks.VOID_AIR.getDefaultState();
                } else {
                    ChunkSection section = chunk2.getSectionArray()[chunk2.getSectionIndex(j)];
                    return section == null
                            ? Blocks.VOID_AIR.getDefaultState()
                            : section.getBlockState(i & 15, j & 15, k & 15);
                }
            }
        }

        private boolean emptySection(int i, int j, int k) {
            WorldChunk chunk2 = this.chunkAt(i >> 4, k >> 4);
            if (chunk2 == null) {
                return true;
            } else {
                ChunkSection section = chunk2.getSectionArray()[chunk2.getSectionIndex(j)];
                return section == null || section.isEmpty();
            }
        }

        private WorldChunk chunkAt(int i, int j) {
            if (this.chunk != null && this.chunk.getPos().x == i && this.chunk.getPos().z == j) {
                return this.chunk;
            } else {
                WorldChunk worldChunk = this.level.getChunkManager().getWorldChunk(i, j);
                if (worldChunk != null) {
                    this.chunk = worldChunk;
                }
                return worldChunk;
            }
        }

        private int light(int i, int j, int k) {
            int m =
                    Math.max(
                            this.rawLight(LightType.BLOCK, i, j, k),
                            this.rawLight(LightType.SKY, i, j, k));
            if (m < 15 && this.level.getDimension().hasSkyLight()) {
                return this.openSky(i, j, k) ? 15 : m;
            } else {
                return m;
            }
        }

        private int rawLight(LightType lightType, int i, int j, int k) {
            ChunkNibbleArray lightSection =
                    this.level
                            .getLightingProvider()
                            .get(lightType)
                            .getLightSection(ChunkSectionPos.from(i >> 4, j >> 4, k >> 4));
            return lightSection != null && !lightSection.isArrayUninitialized()
                    ? lightSection.get(i & 15, j & 15, k & 15)
                    : 0;
        }

        private boolean openSky(int i, int j, int k) {
            int topYInclusive = this.level.getTopYInclusive();
            for (int n = j + 1; n <= topYInclusive; n++) {
                if (this.emptySection(i, n, k)) {
                    n |= 15;
                } else if (this.state(i, n, k).getOpacity() > 0) {
                    return false;
                }
            }
            return true;
        }
    }

    @Environment(EnvType.CLIENT)
    private record Record(boolean clears, int radius, boolean hole, int weight) {}

    @Environment(EnvType.CLIENT)
    private record Source(boolean clears, boolean hole, int weight) {}
}
