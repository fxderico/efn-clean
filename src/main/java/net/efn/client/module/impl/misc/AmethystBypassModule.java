package net.efn.client.module.impl.misc;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.AmethystLight;
import net.efn.client.util.GeodeGlowScan;
import net.efn.client.util.LevelUtil;
import net.efn.client.util.PerformanceBudget;
import net.efn.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.ArrayDeque;
import java.util.Arrays;

@Environment(EnvType.CLIENT)
public final class AmethystBypassModule extends ClientModule {

    private static final int COLOR = -3636481;

    private static final float LINE_WIDTH = 1.5F;

    private static final int RESCAN_TICKS = 20;

    private static final long BUDGET_NANOS = 2000000L;

    private static final String ANBS_SCAN = "ANBS+ Scan";

    private static final int MAX_GLOW_MARKS = 200;

    private static final int FULL_REFRESH_TICKS = 600;

    private final ModeSetting method =
            this.addSetting(
                    "method", new ModeSetting("Method", "Bypass", "Bypass", "Light", "ANBS+ Scan"));

    private final NumberSetting minCells =
            this.addSetting(
                    "min-cells", new NumberSetting("Min Cells", 12.0F, 1.0F, 100.0F).step(1.0F));

    private final BooleanSetting tracer =
            this.addSetting("tracer", new BooleanSetting("Tracer", false));

    private final ArrayDeque<ChunkPos> queue = new ArrayDeque<>();

    private final LongOpenHashSet found = new LongOpenHashSet();

    private final LongOpenHashSet bypassScanned = new LongOpenHashSet();

    private final Long2ObjectOpenHashMap<BlockPos> bypassMarkers = new Long2ObjectOpenHashMap<>();

    private final Long2ObjectOpenHashMap<long[]> chunkHits = new Long2ObjectOpenHashMap<>();

    private final LongOpenHashSet lightScanned = new LongOpenHashSet();

    private final LongOpenHashSet chunkFound = new LongOpenHashSet();

    private boolean hitsChanged;

    private int sinceRefresh;

    private final GeodeGlowScan glow = new GeodeGlowScan();

    private final LongArrayList glowCells = new LongArrayList();

    private long[] hits = new long[0];

    private int tick;

    public AmethystBypassModule() {
        super(
                "amethyst-bypass",
                "Amethyst Bypass",
                "Shows amethyst geodes hidden by anti-xray.",
                ModuleCategory.BASE_RAID);
        this.method.onChange(this::restart);
        this.minCells.visibleWhen(() -> this.method.is("ANBS+ Scan"));
    }

    @Override
    protected void onEnable() {
        this.restart();
    }

    @Override
    protected void onDisable() {
        this.restart();
    }

    private void restart() {
        this.queue.clear();
        this.found.clear();
        this.bypassScanned.clear();
        this.bypassMarkers.clear();
        this.chunkHits.clear();
        this.lightScanned.clear();
        this.hitsChanged = false;
        this.sinceRefresh = 0;
        this.hits = new long[0];
        this.tick = 0;
    }

    @EventHandler(stage = Stage.POST)
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world != null && client.player != null) {
            boolean is = this.method.is("Bypass");
            if (this.queue.isEmpty()) {
                if (++this.tick < 20) {
                    return;
                }
                this.tick = 0;
                ChunkPos chunkPos = client.player.getChunkPos();
                int viewDistance = client.options.getViewDistance().getValue();
                if (is) {
                    this.bypassScanned.removeIf(
                            value ->
                                    !inRange(value, chunkPos, viewDistance)
                                            || !LevelUtil.isLoaded(
                                                    world,
                                                    ChunkPos.getPackedX(value),
                                                    ChunkPos.getPackedZ(value)));
                    this.bypassMarkers
                            .keySet()
                            .removeIf(value2 -> !this.bypassScanned.contains(value2));
                    for (int k = chunkPos.x - viewDistance; k <= chunkPos.x + viewDistance; k++) {
                        for (int m = chunkPos.z - viewDistance;
                                m <= chunkPos.z + viewDistance;
                                m++) {
                            long l = ChunkPos.toLong(k, m);
                            if (LevelUtil.isLoaded(world, k, m)
                                    && !this.bypassScanned.contains(l)) {
                                this.queue.add(new ChunkPos(k, m));
                            }
                        }
                    }
                } else {
                    if (++this.sinceRefresh * 20 >= 600) {
                        this.sinceRefresh = 0;
                        this.lightScanned.clear();
                    }
                    this.hitsChanged =
                            this.hitsChanged
                                    | this.chunkHits
                                            .keySet()
                                            .removeIf(
                                                    value3 ->
                                                            !inRange(value3, chunkPos, viewDistance)
                                                                    || !LevelUtil.isLoaded(
                                                                            world,
                                                                            ChunkPos.getPackedX(
                                                                                    value3),
                                                                            ChunkPos.getPackedZ(
                                                                                    value3)));
                    this.lightScanned.removeIf(value4 -> !inRange(value4, chunkPos, viewDistance));
                    for (int n = chunkPos.x - viewDistance; n <= chunkPos.x + viewDistance; n++) {
                        for (int o = chunkPos.z - viewDistance;
                                o <= chunkPos.z + viewDistance;
                                o++) {
                            if (LevelUtil.isLoaded(world, n, o)
                                    && !this.lightScanned.contains(ChunkPos.toLong(n, o))) {
                                this.queue.add(new ChunkPos(n, o));
                            }
                        }
                    }
                }
            }
            boolean optimizationsEnabled = PerformanceModule.optimizationsEnabled();
            long l2 = System.nanoTime();
            long l3 = optimizationsEnabled ? PerformanceBudget.acquire(2000000L) : l2 + 2000000L;
            while (!this.queue.isEmpty() && System.nanoTime() < l3) {
                ChunkPos chunkPos2 = this.queue.poll();
                if (LevelUtil.isLoaded(world, chunkPos2.x, chunkPos2.z)) {
                    WorldChunk chunk = world.getChunk(chunkPos2.x, chunkPos2.z);
                    if (is) {
                        collectPaletteLeak(chunk, this.bypassMarkers);
                        this.bypassScanned.add(chunkPos2.toLong());
                    } else {
                        this.chunkFound.clear();
                        if (this.method.is("ANBS+ Scan")) {
                            this.collectGlow(world, chunk, this.chunkFound);
                        } else {
                            AmethystLight.scanSources(
                                    world, chunk, (value5, i) -> this.chunkFound.add(value5));
                        }
                        long l4 = chunkPos2.toLong();
                        long[] chunkHit = this.chunkHits.get(l4);
                        long[] longArray = this.chunkFound.toLongArray();
                        if (chunkHit != null && longArray.length < chunkHit.length) {
                            this.lightScanned.add(l4);
                        } else {
                            if (longArray.length == 0) {
                                if (this.chunkHits.remove(l4) != null) {
                                    this.hitsChanged = true;
                                }
                            } else if (chunkHit == null || !Arrays.equals(chunkHit, longArray)) {
                                this.chunkHits.put(l4, longArray);
                                this.hitsChanged = true;
                            }
                            this.lightScanned.add(l4);
                        }
                    }
                }
            }
            if (optimizationsEnabled) {
                PerformanceBudget.record(l2);
            }
            if (is) {
                if (this.queue.isEmpty()) {
                    this.hits = markersToLongArray(this.bypassMarkers);
                }
            } else if (this.hitsChanged) {
                this.hitsChanged = false;
                this.found.clear();
                ObjectIterator objectIterator = this.chunkHits.values().iterator();
                while (objectIterator.hasNext()) {
                    for (long l5 : (long[]) objectIterator.next()) {
                        this.found.add(l5);
                    }
                }
                this.hits = this.found.toLongArray();
            }
        }
    }

    private void collectGlow(ClientWorld world, WorldChunk chunk, LongOpenHashSet longOpenHashSet) {
        this.glowCells.clear();
        if (this.glow.scan(world, chunk, value -> this.glowCells.add(value))
                >= this.minCells.getInt()) {
            int i = Math.min(this.glowCells.size(), 200);
            for (int j = 0; j < i; j++) {
                longOpenHashSet.add(this.glowCells.getLong(j));
            }
        }
    }

    private static boolean inRange(long l, ChunkPos chunkPos, int i) {
        int packedX = ChunkPos.getPackedX(l);
        int packedZ = ChunkPos.getPackedZ(l);
        return Math.abs(packedX - chunkPos.x) <= i && Math.abs(packedZ - chunkPos.z) <= i;
    }

    private static long[] markersToLongArray(
            Long2ObjectOpenHashMap<BlockPos> long2ObjectOpenHashMap) {
        long[] longs = new long[long2ObjectOpenHashMap.size()];
        int i = 0;
        for (BlockPos pos : long2ObjectOpenHashMap.values()) {
            longs[i++] = pos.asLong();
        }
        return longs;
    }

    @EventHandler
    public void onPacketApplied(PacketAppliedEvent event) {
        if (!this.method.is("Bypass")) {
            if (event.packet instanceof ChunkDataS2CPacket chunkDataPacket) {
                this.dirtyAround(chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ());
            } else if (event.packet instanceof LightUpdateS2CPacket lightUpdatePacket) {
                this.dirtyAround(lightUpdatePacket.getChunkX(), lightUpdatePacket.getChunkZ());
            } else if (event.packet instanceof BlockUpdateS2CPacket blockUpdatePacket) {
                this.dirtyAround(
                        blockUpdatePacket.getPos().getX() >> 4,
                        blockUpdatePacket.getPos().getZ() >> 4);
            } else if (event.packet instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdatePacket) {
                chunkDeltaUpdatePacket.visitUpdates(
                        (blockPos, blockState) ->
                                this.dirtyAround(blockPos.getX() >> 4, blockPos.getZ() >> 4));
            }
        } else {
            if (event.packet instanceof ChunkDataS2CPacket chunkDataPacket2) {
                this.dirtyBypassChunk(chunkDataPacket2.getChunkX(), chunkDataPacket2.getChunkZ());
            } else if (event.packet instanceof BlockUpdateS2CPacket blockUpdatePacket2) {
                this.dirtyBypassChunk(
                        blockUpdatePacket2.getPos().getX() >> 4,
                        blockUpdatePacket2.getPos().getZ() >> 4);
            } else if (event.packet instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdatePacket2) {
                chunkDeltaUpdatePacket2.visitUpdates(
                        (blockPos, blockState) ->
                                this.dirtyBypassChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4));
            }
        }
    }

    private void dirtyAround(int i, int j) {
        for (int k = -1; k <= 1; k++) {
            for (int m = -1; m <= 1; m++) {
                this.lightScanned.remove(ChunkPos.toLong(i + k, j + m));
            }
        }
    }

    private void dirtyBypassChunk(int i, int j) {
        long l = ChunkPos.toLong(i, j);
        if (this.bypassScanned.remove(l)) {
            this.bypassMarkers.remove(l);
        }
    }

    private static void collectPaletteLeak(
            WorldChunk chunk, Long2ObjectOpenHashMap<BlockPos> long2ObjectOpenHashMap) {
        ChunkSection[] sectionArray = chunk.getSectionArray();
        int bottomSectionCoord = chunk.getBottomSectionCoord();
        ChunkPos chunkPos = chunk.getPos();
        for (int j = 0; j < sectionArray.length; j++) {
            ChunkSection section = sectionArray[j];
            PalettedContainer blockStateContainer;
            if (section != null
                    && !(blockStateContainer = section.getBlockStateContainer())
                            .hasAny(blockState -> false)
                    && blockStateContainer.hasAny(
                            bs ->
                                    AmethystBypassModule.isBud(
                                            (net.minecraft.block.BlockState) bs))) {
                int i = (bottomSectionCoord + j) * 16;
                BlockPos marker = pickMarker(chunk, section, i, chunkPos);
                long2ObjectOpenHashMap.put(
                        chunkPos.toLong(),
                        marker != null
                                ? marker
                                : new BlockPos(
                                        chunkPos.getStartX() + 8, i + 8, chunkPos.getStartZ() + 8));
                return;
            }
        }
    }

    private static BlockPos pickMarker(
            WorldChunk chunk, ChunkSection section, int i, ChunkPos chunkPos) {
        Mutable mutable = new Mutable();
        BlockPos pos = null;
        int j = 2147483647;
        for (int k = 0; k < 16; k++) {
            int m = i + k;
            for (int n = 0; n < 16; n++) {
                for (int o = 0; o < 16; o++) {
                    if (section.getBlockState(o, k, n).isAir()) {
                        int p = chunkPos.getStartX() + o;
                        int q = chunkPos.getStartZ() + n;
                        boolean bl = false;
                        for (Direction direction : Direction.values()) {
                            int r = p + direction.getOffsetX();
                            int t = q + direction.getOffsetZ();
                            if (r >> 4 == chunkPos.x && t >> 4 == chunkPos.z) {
                                mutable.set(r, m + direction.getOffsetY(), t);
                                Block block = chunk.getBlockState(mutable).getBlock();
                                if (block == Blocks.AMETHYST_BLOCK
                                        || block == Blocks.BUDDING_AMETHYST) {
                                    bl = true;
                                    break;
                                }
                            }
                        }
                        int scatter;
                        if (bl && (scatter = scatter(p, m, q)) < j) {
                            j = scatter;
                            pos = new BlockPos(p, m, q);
                        }
                    }
                }
            }
        }
        return pos;
    }

    static int scatter(int i, int j, int k) {
        long l = i * -7046029254386353131L ^ j * -4417276706812531889L ^ k * 1609587929392839161L;
        l ^= l >>> 29;
        l *= -4658895280553007687L;
        l ^= l >>> 32;
        return Math.floorMod(l, 100);
    }

    private static boolean isBud(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.SMALL_AMETHYST_BUD
                || block == Blocks.MEDIUM_AMETHYST_BUD
                || block == Blocks.LARGE_AMETHYST_BUD;
    }

    private static boolean isGeode(BlockState state) {
        return state.isOf(Blocks.AMETHYST_BLOCK) || state.isOf(Blocks.BUDDING_AMETHYST);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        ClientWorld world = MinecraftClient.getInstance().world;
        long[] hits2 = this.hits;
        if (world != null && hits2.length != 0) {
            DrawStyle drawStyle = DrawStyle.filledAndStroked(-3636481, 1.5F, -3636481);
            boolean tracer2 = this.tracer.get();
            for (long l : hits2) {
                BlockPos pos = BlockPos.fromLong(l);
                if (LevelUtil.isLoaded(world, pos.getX() >> 4, pos.getZ() >> 4)) {
                    Box box = new Box(pos);
                    if (event.frustum == null || event.frustum.isVisible(box)) {
                        GizmoDrawing.box(box, drawStyle).ignoreOcclusion();
                    }
                    if (tracer2) {
                        TracerEngine.draw(Vec3d.ofCenter(pos), -3636481);
                    }
                }
            }
        }
    }
}
