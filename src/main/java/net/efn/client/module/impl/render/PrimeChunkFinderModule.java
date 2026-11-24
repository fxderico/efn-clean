package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ChallengeToast;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.util.ChunkAreas;
import net.efn.client.util.ChunkOverlay;
import net.efn.client.util.LevelUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CaveVines;
import net.minecraft.block.CrafterBlock;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonExtensionBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.RedstoneBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.TargetBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightingView;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class PrimeChunkFinderModule extends ClientModule {

    private static final int SECTION_BYTES = 2048;

    private static final int MAX_FLAGGED_CHUNKS = 256;

    private static final int AMETHYST_MAX_LIGHT = 5;

    private static final int AMETHYST_REACH = 5;

    private static final long REDSTONE_NOTIFY_COOLDOWN_MS = 15000L;

    private static final int ORE_LIGHT = 9;

    private static final long[] NO_ORES = new long[0];

    private static final int LAMP_LIGHT = 14;

    private static final int[] NO_LAMPS = new int[0];

    private static final long GONE_LAMP_MS = 5000L;

    private static final int MAX_TRACKED_CHUNKS = 5000;

    private static final int SNAPSHOT_RADIUS = 32;

    private static final byte[] ZERO_SECTION = new byte[2048];

    private static final double MAX_Y = 0.0;

    private final ColorSetting chunkColor =
            this.addSetting("chunk-color", new ColorSetting("Chunk Color", 1342242650));

    private final BooleanSetting tracers =
            this.addSetting("tracers", new BooleanSetting("Tracers", true));

    private final BooleanSetting sound =
            this.addSetting("sound", new BooleanSetting("Sound", true));

    private final LinkedHashMap<Long, Map<Integer, byte[]>> chunkLight =
            new LinkedHashMap<>(256, 0.75F, true);

    private final LongOpenHashSet passedChunks = new LongOpenHashSet();

    private final LongLinkedOpenHashSet flaggedChunks = new LongLinkedOpenHashSet();

    private final LongOpenHashSet shownChunks = new LongOpenHashSet();

    private final ChunkAreas chunkAreas = new ChunkAreas();

    private final ChunkOverlay chunkOverlay = new ChunkOverlay();

    private final LongOpenHashSet checkedSections = new LongOpenHashSet();

    private final LongOpenHashSet amethystSections = new LongOpenHashSet();

    private final Long2ObjectOpenHashMap<long[]> oreSections = new Long2ObjectOpenHashMap<>();

    private final Long2ObjectOpenHashMap<int[]> lampSections = new Long2ObjectOpenHashMap<>();

    private final Long2LongOpenHashMap goneLamps = new Long2LongOpenHashMap();

    private final Mutable cursor = new Mutable();

    private final Long2LongOpenHashMap redstoneNotified = new Long2LongOpenHashMap();

    public PrimeChunkFinderModule() {
        super(
                "prime-chunk-finder",
                "Prime Chunk Finder",
                "Flags chunks with light and redstone changes below Y 0.",
                ModuleCategory.BASE_RAID);
        BooleanSetting setting = this.usesNotifications();
        this.sound.visibleWhen(setting::get);
        this.redstoneNotified.defaultReturnValue(-4611686018427387904L);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        this.reset();
        this.snapshotLoadedChunks();
        this.on(PacketReceiveNettyEvent.TYPE, this::onPacket);
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    private void onPacket(PacketReceiveNettyEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (event.packet instanceof GameJoinS2CPacket) {
            client.execute(
                    () -> {
                        if (this.isEnabled()) {
                            this.reset();
                        }
                    });
        } else if (event.packet instanceof ChunkDataS2CPacket chunkDataPacket) {
            int chunkX = chunkDataPacket.getChunkX();
            int chunkZ = chunkDataPacket.getChunkZ();
            LightData lightData = chunkDataPacket.getLightData();
            client.execute(
                    () -> {
                        if (this.isEnabled() && client.world != null) {
                            this.baselineChunk(chunkX, chunkZ, lightData);
                        }
                    });
        } else if (event.packet instanceof LightUpdateS2CPacket lightUpdatePacket) {
            int chunkX2 = lightUpdatePacket.getChunkX();
            int chunkZ2 = lightUpdatePacket.getChunkZ();
            LightData data = lightUpdatePacket.getData();
            client.execute(
                    () -> {
                        if (this.isEnabled() && client.world != null) {
                            this.detectChanges(chunkX2, chunkZ2, data);
                        }
                    });
        }
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent event) {
        if (event.oldState != event.newState && MinecraftClient.getInstance().world != null) {
            this.rememberGoneLamp(event.pos, event.oldState, event.newState);
            this.detectRedstone(event.pos, event.oldState, event.newState);
        }
    }

    @EventHandler(stage = Stage.POST)
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            this.reset();
        } else {
            if (client.player != null && client.player.getY() < 0.0) {
                this.passedChunks.add(client.player.getChunkPos().toLong());
            }
            this.forgetOldLamps();
            this.refreshShown(client.world);
        }
    }

    private void refreshShown(ClientWorld world) {
        this.shownChunks.clear();
        LongListIterator longListIterator = this.flaggedChunks.iterator();
        while (longListIterator.hasNext()) {
            long l = longListIterator.next();
            if (LevelUtil.isLoaded(world, ChunkPos.getPackedX(l), ChunkPos.getPackedZ(l))) {
                this.shownChunks.add(l);
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!this.shownChunks.isEmpty() && MinecraftClient.getInstance().world != null) {
            int i = this.chunkColor.argb();
            this.chunkOverlay.draw(this.shownChunks, i, event.partialTick);
            if (this.tracers.get()) {
                this.chunkAreas.drawTracers(this.shownChunks, 63.0, ColorHelper.fullAlpha(i));
            }
        }
    }

    private void flagChunk(long l) {
        this.flaggedChunks.addAndMoveToLast(l);
        this.shownChunks.add(l);
        while (this.flaggedChunks.size() > 256) {
            this.flaggedChunks.removeFirstLong();
        }
    }

    private void snapshotLoadedChunks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            int i = client.player.getBlockX() >> 4;
            int j = client.player.getBlockZ() >> 4;
            for (int k = i - 32; k <= i + 32; k++) {
                for (int m = j - 32; m <= j + 32; m++) {
                    Map map = this.snapshotChunk(k, m);
                    if (map != null) {
                        this.chunkLight.put(ChunkPos.toLong(k, m), map);
                    }
                }
            }
            this.trimChunkCache();
        }
    }

    private Map<Integer, byte[]> snapshotChunk(int i, int j) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return null;
        } else {
            ClientChunkManager chunkManager = world.getChunkManager();
            if (!chunkManager.isChunkLoaded(i, j)) {
                return null;
            } else {
                ChunkLightingView chunkLightingView =
                        chunkManager.getLightingProvider().get(LightType.BLOCK);
                Map zeroedChunk = this.createZeroedChunk();
                for (int k = this.bottomSection(); k <= this.topSection(); k++) {
                    ChunkNibbleArray lightSection =
                            chunkLightingView.getLightSection(ChunkSectionPos.from(i, k, j));
                    if (lightSection != null && !lightSection.isUninitialized(0)) {
                        zeroedChunk.put(k, Arrays.copyOf(lightSection.asByteArray(), 2048));
                    }
                }
                return zeroedChunk;
            }
        }
    }

    private void baselineChunk(int i, int j, LightData lightData) {
        Map zeroedChunk = this.createZeroedChunk();
        this.applyInitializedSections(lightData, zeroedChunk, i, j, false, null);
        this.applyUninitializedSections(lightData, zeroedChunk, i, j, false, null);
        this.chunkLight.put(ChunkPos.toLong(i, j), zeroedChunk);
        this.trimChunkCache();
    }

    private void detectChanges(int i, int j, LightData lightData) {
        long l = ChunkPos.toLong(i, j);
        this.checkedSections.clear();
        this.amethystSections.clear();
        this.oreSections.clear();
        this.lampSections.clear();
        Map map = this.chunkLight.get(l);
        if (map == null) {
            map = this.snapshotChunk(i, j);
        }
        boolean bl = map != null && !this.isOwnChunk(l);
        if (map == null) {
            map = this.createZeroedChunk();
        }
        PrimeChunkFinderModule.ChangeSummary changeSummary =
                new PrimeChunkFinderModule.ChangeSummary();
        this.applyInitializedSections(lightData, map, i, j, bl, changeSummary);
        this.applyUninitializedSections(lightData, map, i, j, bl, changeSummary);
        this.chunkLight.put(l, map);
        this.trimChunkCache();
        if (changeSummary.count != 0) {
            this.flagChunk(l);
            if (this.notificationsOn()) {
                ChallengeToast.addOrUpdate(
                        new ItemStack(Items.EMERALD_BLOCK),
                        this.name(),
                        Text.literal(
                                changeSummary.x + ", " + changeSummary.y + ", " + changeSummary.z),
                        this.sound.get());
            }
        }
    }

    private void rememberGoneLamp(BlockPos pos, BlockState state, BlockState state2) {
        if (isLamp(state)) {
            int luminance = state.getLuminance();
            if (!isLamp(state2) || state2.getLuminance() < luminance) {
                this.goneLamps.put(
                        pos.asLong(), Util.getMeasuringTimeMs() + 5000L << 4 | luminance);
            }
        }
    }

    private void forgetOldLamps() {
        if (!this.goneLamps.isEmpty()) {
            long measuringTimeMs = Util.getMeasuringTimeMs();
            LongIterator longIterator = this.goneLamps.values().iterator();
            while (longIterator.hasNext()) {
                if (longIterator.nextLong() >>> 4 < measuringTimeMs) {
                    longIterator.remove();
                }
            }
        }
    }

    private void detectRedstone(BlockPos pos, BlockState state, BlockState state2) {
        if (!(pos.getY() >= 0.0) && (isRedstone(state2) || isRedstone(state))) {
            long l = ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4);
            if (!this.isOwnChunk(l)) {
                this.flagChunk(l);
                long measuringTimeMs = Util.getMeasuringTimeMs();
                if (this.notificationsOn()
                        && measuringTimeMs - this.redstoneNotified.get(l) >= 15000L) {
                    this.redstoneNotified.put(l, measuringTimeMs);
                    ChallengeToast.addOrUpdate(
                            new ItemStack(Items.REDSTONE),
                            this.name(),
                            Text.literal(
                                    "Redstone "
                                            + pos.getX()
                                            + ", "
                                            + pos.getY()
                                            + ", "
                                            + pos.getZ()),
                            this.sound.get());
                }
            }
        }
    }

    private static boolean isRedstone(BlockState state) {
        Block block = state.getBlock();
        return block instanceof RedstoneWireBlock
                || block instanceof AbstractRedstoneGateBlock
                || block instanceof RedstoneTorchBlock
                || block instanceof RedstoneLampBlock
                || block instanceof ObserverBlock
                || block instanceof PistonBlock
                || block instanceof PistonHeadBlock
                || block instanceof PistonExtensionBlock
                || block instanceof DispenserBlock
                || block instanceof HopperBlock
                || block instanceof NoteBlock
                || block instanceof DaylightDetectorBlock
                || block instanceof LeverBlock
                || block instanceof ButtonBlock
                || block instanceof AbstractPressurePlateBlock
                || block instanceof TripwireHookBlock
                || block instanceof TargetBlock
                || block instanceof CrafterBlock
                || block instanceof RedstoneBlock;
    }

    private boolean isOwnChunk(long l) {
        if (this.passedChunks.contains(l)) {
            return true;
        } else {
            MinecraftClient client = MinecraftClient.getInstance();
            return client.player != null && client.player.getChunkPos().toLong() == l;
        }
    }

    private void applyInitializedSections(
            LightData lightData,
            Map<Integer, byte[]> map,
            int i,
            int j,
            boolean bl,
            PrimeChunkFinderModule.ChangeSummary changeSummary) {
        BitSet initedBlock = lightData.getInitedBlock();
        List blockNibbles = lightData.getBlockNibbles();
        int k = 0;
        for (int m = initedBlock.nextSetBit(0);
                m >= 0 && k < blockNibbles.size();
                m = initedBlock.nextSetBit(m + 1)) {
            byte[] blockNibble = (byte[]) blockNibbles.get(k++);
            int lightSectionY = this.lightSectionY(m);
            if (this.isWorldSection(lightSectionY) && blockNibble.length == 2048) {
                this.updateSection(
                        map,
                        i,
                        j,
                        lightSectionY,
                        Arrays.copyOf(blockNibble, 2048),
                        bl,
                        changeSummary);
            }
        }
    }

    private void applyUninitializedSections(
            LightData lightData,
            Map<Integer, byte[]> map,
            int i,
            int j,
            boolean bl,
            PrimeChunkFinderModule.ChangeSummary changeSummary) {
        BitSet uninitedBlock = lightData.getUninitedBlock();
        for (int k = uninitedBlock.nextSetBit(0); k >= 0; k = uninitedBlock.nextSetBit(k + 1)) {
            int lightSectionY = this.lightSectionY(k);
            if (this.isWorldSection(lightSectionY)) {
                this.updateSection(map, i, j, lightSectionY, ZERO_SECTION, bl, changeSummary);
            }
        }
    }

    private void updateSection(
            Map<Integer, byte[]> map,
            int i,
            int j,
            int k,
            byte[] bytes,
            boolean bl,
            PrimeChunkFinderModule.ChangeSummary changeSummary) {
        byte[] bytes2 = (byte[]) map.get(k);
        if (bl && bytes2 != null && changeSummary != null) {
            this.compareSection(bytes2, bytes, i, j, k, changeSummary);
        }
        map.put(k, bytes);
    }

    private void compareSection(
            byte[] bytes,
            byte[] bytes2,
            int i,
            int j,
            int k,
            PrimeChunkFinderModule.ChangeSummary changeSummary) {
        for (int m = 0; m < 2048; m++) {
            int n = bytes[m] & 255;
            int o = bytes2[m] & 255;
            int p = n ^ o;
            if ((p & 15) != 0) {
                this.recordChange(i, j, k, m << 1, n & 15, o & 15, changeSummary);
            }
            if ((p & 240) != 0) {
                this.recordChange(i, j, k, (m << 1) + 1, n >>> 4 & 15, o >>> 4 & 15, changeSummary);
            }
        }
    }

    private void recordChange(
            int i,
            int j,
            int k,
            int m,
            int n,
            int o,
            PrimeChunkFinderModule.ChangeSummary changeSummary) {
        int p = (i << 4) + (m & 15);
        int q = (k << 4) + (m >>> 8 & 15);
        int r = (j << 4) + (m >>> 4 & 15);
        int t = Math.max(n, o);
        if (!this.fromAmethyst(p, q, r, n, o)
                && !this.fromRedstoneOre(p, q, r, t)
                && !this.fromLamp(p, q, r, t)) {
            changeSummary.add(p, q, r, n, o);
        }
    }

    private boolean fromLamp(int i, int j, int k, int m) {
        if (m > 14) {
            return false;
        } else {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world == null) {
                return false;
            } else {
                int n = 14 - m;
                for (int o = i - n >> 4; o <= i + n >> 4; o++) {
                    for (int p = j - n >> 4; p <= j + n >> 4; p++) {
                        for (int q = k - n >> 4; q <= k + n >> 4; q++) {
                            for (int r : this.lamps(world, o, p, q)) {
                                int t = r >>> 4;
                                int u = (o << 4) + (t & 15);
                                int v = (p << 4) + (t >>> 8);
                                int w = (q << 4) + (t >>> 4 & 15);
                                if (Math.abs(u - i) + Math.abs(v - j) + Math.abs(w - k)
                                        <= (r & 15) - m) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                long measuringTimeMs = Util.getMeasuringTimeMs();
                for (Entry long2LongEntry : this.goneLamps.long2LongEntrySet()) {
                    long longValue = long2LongEntry.getLongValue();
                    long longKey;
                    if (longValue >>> 4 >= measuringTimeMs
                            && Math.abs(
                                                    BlockPos.unpackLongX(
                                                                    longKey =
                                                                            long2LongEntry
                                                                                    .getLongKey())
                                                            - i)
                                            + Math.abs(BlockPos.unpackLongY(longKey) - j)
                                            + Math.abs(BlockPos.unpackLongZ(longKey) - k)
                                    <= (int) (longValue & 15L) - m) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private int[] lamps(ClientWorld world, int i, int j, int k) {
        long l = ChunkSectionPos.asLong(i, j, k);
        int[] lampSection = this.lampSections.get(l);
        if (lampSection != null) {
            return lampSection;
        } else {
            int[] ints = scanLamps(world, i, j, k);
            this.lampSections.put(l, ints);
            return ints;
        }
    }

    private static int[] scanLamps(ClientWorld world, int i, int j, int k) {
        if (j >= world.getBottomSectionCoord() && j <= world.getTopSectionCoord()) {
            WorldChunk chunk = world.getChunkManager().getChunk(i, k, ChunkStatus.FULL, false);
            if (chunk == null) {
                return NO_LAMPS;
            } else {
                ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(j));
                if (!section.isEmpty()
                        && section.getBlockStateContainer()
                                .hasAny(PrimeChunkFinderModule::isLamp)) {
                    IntArrayList intArrayList = new IntArrayList();
                    for (int m = 0; m < 4096; m++) {
                        BlockState blockState =
                                section.getBlockState(m & 15, m >>> 8, m >>> 4 & 15);
                        if (isLamp(blockState)) {
                            intArrayList.add(m << 4 | blockState.getLuminance());
                        }
                    }
                    return intArrayList.isEmpty() ? NO_LAMPS : intArrayList.toIntArray();
                } else {
                    return NO_LAMPS;
                }
            }
        } else {
            return NO_LAMPS;
        }
    }

    private static boolean isLamp(BlockState state) {
        return state.isOf(Blocks.TORCH)
                || state.isOf(Blocks.WALL_TORCH)
                || state.isOf(Blocks.SOUL_TORCH)
                || state.isOf(Blocks.SOUL_WALL_TORCH)
                || state.isOf(Blocks.COPPER_TORCH)
                || state.isOf(Blocks.COPPER_WALL_TORCH)
                || CaveVines.hasBerries(state);
    }

    private boolean fromAmethyst(int i, int j, int k, int m, int n) {
        if (Math.max(m, n) > 5) {
            return false;
        } else {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null && this.amethystSectionNear(world, i, j, k)) {
                for (int o = -5; o <= 5; o++) {
                    int p = 5 - Math.abs(o);
                    for (int q = -p; q <= p; q++) {
                        int r = p - Math.abs(q);
                        for (int t = -r; t <= r; t++) {
                            if (isAmethyst(
                                    world.getBlockState(this.cursor.set(i + o, j + q, k + t)))) {
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
    }

    private boolean amethystSectionNear(ClientWorld world, int i, int j, int k) {
        for (int m = i - 5 >> 4; m <= i + 5 >> 4; m++) {
            for (int n = j - 5 >> 4; n <= j + 5 >> 4; n++) {
                for (int o = k - 5 >> 4; o <= k + 5 >> 4; o++) {
                    long l = ChunkSectionPos.asLong(m, n, o);
                    if (this.checkedSections.add(l) && sectionHasAmethyst(world, m, n, o)) {
                        this.amethystSections.add(l);
                    }
                    if (this.amethystSections.contains(l)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean sectionHasAmethyst(ClientWorld world, int i, int j, int k) {
        if (j >= world.getBottomSectionCoord() && j <= world.getTopSectionCoord()) {
            WorldChunk chunk = world.getChunkManager().getChunk(i, k, ChunkStatus.FULL, false);
            if (chunk == null) {
                return false;
            } else {
                ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(j));
                return !section.isEmpty()
                        && section.getBlockStateContainer()
                                .hasAny(PrimeChunkFinderModule::isAmethyst);
            }
        } else {
            return false;
        }
    }

    private boolean fromRedstoneOre(int i, int j, int k, int m) {
        if (m > 9) {
            return false;
        } else {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world == null) {
                return false;
            } else {
                int n = 9 - m;
                for (int o = i - n >> 4; o <= i + n >> 4; o++) {
                    for (int p = j - n >> 4; p <= j + n >> 4; p++) {
                        for (int q = k - n >> 4; q <= k + n >> 4; q++) {
                            for (long l : this.exposedOres(world, o, p, q)) {
                                int r =
                                        Math.abs(BlockPos.unpackLongX(l) - i)
                                                + Math.abs(BlockPos.unpackLongY(l) - j)
                                                + Math.abs(BlockPos.unpackLongZ(l) - k);
                                if (r <= n) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;
            }
        }
    }

    private long[] exposedOres(ClientWorld world, int i, int j, int k) {
        long l = ChunkSectionPos.asLong(i, j, k);
        long[] oreSection = this.oreSections.get(l);
        if (oreSection != null) {
            return oreSection;
        } else {
            long[] longs = this.scanExposedOres(world, i, j, k);
            this.oreSections.put(l, longs);
            return longs;
        }
    }

    private long[] scanExposedOres(ClientWorld world, int i, int j, int k) {
        if (j >= world.getBottomSectionCoord() && j <= world.getTopSectionCoord()) {
            WorldChunk chunk = world.getChunkManager().getChunk(i, k, ChunkStatus.FULL, false);
            if (chunk == null) {
                return NO_ORES;
            } else {
                ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(j));
                if (!section.isEmpty()
                        && section.getBlockStateContainer()
                                .hasAny(PrimeChunkFinderModule::isRedstoneOre)) {
                    LongArrayList longArrayList = new LongArrayList();
                    for (int m = 0; m < 16; m++) {
                        for (int n = 0; n < 16; n++) {
                            for (int o = 0; o < 16; o++) {
                                int p;
                                int q;
                                int r;
                                if (isRedstoneOre(section.getBlockState(o, m, n))
                                        && this.exposed(
                                                world,
                                                r = (i << 4) + o,
                                                q = (j << 4) + m,
                                                p = (k << 4) + n)) {
                                    longArrayList.add(BlockPos.asLong(r, q, p));
                                }
                            }
                        }
                    }
                    return longArrayList.isEmpty() ? NO_ORES : longArrayList.toLongArray();
                } else {
                    return NO_ORES;
                }
            }
        } else {
            return NO_ORES;
        }
    }

    private boolean exposed(ClientWorld world, int i, int j, int k) {
        for (Direction direction : Direction.values()) {
            BlockState blockState =
                    world.getBlockState(
                            this.cursor.set(
                                    i + direction.getOffsetX(),
                                    j + direction.getOffsetY(),
                                    k + direction.getOffsetZ()));
            if (blockState.isAir() || !blockState.isOpaque()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRedstoneOre(BlockState state) {
        return state.isOf(Blocks.REDSTONE_ORE) || state.isOf(Blocks.DEEPSLATE_REDSTONE_ORE);
    }

    private static boolean isAmethyst(BlockState state) {
        return state.isOf(Blocks.AMETHYST_CLUSTER)
                || state.isOf(Blocks.LARGE_AMETHYST_BUD)
                || state.isOf(Blocks.MEDIUM_AMETHYST_BUD)
                || state.isOf(Blocks.SMALL_AMETHYST_BUD)
                || state.isOf(Blocks.BUDDING_AMETHYST)
                || state.isOf(Blocks.AMETHYST_BLOCK);
    }

    private Map<Integer, byte[]> createZeroedChunk() {
        HashMap map = new HashMap();
        for (int i = this.bottomSection(); i <= this.topSection(); i++) {
            map.put(i, ZERO_SECTION);
        }
        return map;
    }

    private int lightSectionY(int i) {
        ClientWorld world = MinecraftClient.getInstance().world;
        return world == null
                ? -2147483648
                : world.getChunkManager().getLightingProvider().getBottomY() + i;
    }

    private int bottomSection() {
        return MinecraftClient.getInstance().world.getBottomSectionCoord();
    }

    private int topSection() {
        return MinecraftClient.getInstance().world.getTopSectionCoord();
    }

    private boolean isWorldSection(int i) {
        return i >= this.bottomSection() && i <= this.topSection();
    }

    private void trimChunkCache() {
        while (this.chunkLight.size() > 5000) {
            Iterator iterator = this.chunkLight.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    private void reset() {
        this.chunkLight.clear();
        this.passedChunks.clear();
        this.flaggedChunks.clear();
        this.shownChunks.clear();
        this.redstoneNotified.clear();
        this.lampSections.clear();
        this.goneLamps.clear();
    }

    @Environment(EnvType.CLIENT)
    private static final class ChangeSummary {

        int count;

        int x;

        int y;

        int z;

        int oldValue;

        int newValue;

        private ChangeSummary() {}

        void add(int i, int j, int k, int oldValue2, int newValue2) {
            if (this.count == 0
                    || Math.abs(newValue2 - oldValue2) > Math.abs(this.newValue - this.oldValue)) {
                this.x = i;
                this.y = j;
                this.z = k;
                this.oldValue = oldValue2;
                this.newValue = newValue2;
            }
            this.count++;
        }
    }
}
