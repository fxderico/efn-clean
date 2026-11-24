/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.misc;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.util.LevelUtil;
import net.efn.client.util.quantum.ChunkSnapshotStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Environment(value = EnvType.CLIENT)
public final class QuantumModule extends ClientModule {

    private static final int SCAN_RADIUS = 8;

    private static final int FLAG_RADIUS = 9;

    private static final double MARKER_Y = 63.0;

    private static final float LINE_WIDTH = 1.5f;

    private static final int CYAN = 65535;

    private static final int SOURCE_FILL_ALPHA = 40;

    private static final Set<Block> IGNORED_NEW_BLOCKS =
            Set.of(
                    Blocks.FIRE,
                    Blocks.FROSTED_ICE,
                    Blocks.COAL_ORE,
                    Blocks.DEEPSLATE_COAL_ORE,
                    Blocks.IRON_ORE,
                    Blocks.DEEPSLATE_IRON_ORE,
                    Blocks.COPPER_ORE,
                    Blocks.DEEPSLATE_COPPER_ORE,
                    Blocks.GOLD_ORE,
                    Blocks.DEEPSLATE_GOLD_ORE,
                    Blocks.REDSTONE_ORE,
                    Blocks.DEEPSLATE_REDSTONE_ORE,
                    Blocks.EMERALD_ORE,
                    Blocks.DEEPSLATE_EMERALD_ORE,
                    Blocks.LAPIS_ORE,
                    Blocks.DEEPSLATE_LAPIS_ORE,
                    Blocks.DIAMOND_ORE,
                    Blocks.DEEPSLATE_DIAMOND_ORE,
                    Blocks.OAK_LEAVES,
                    Blocks.ACACIA_LEAVES,
                    Blocks.BIRCH_LEAVES,
                    Blocks.JUNGLE_LEAVES,
                    Blocks.DARK_OAK_LEAVES,
                    Blocks.PALE_OAK_LEAVES,
                    Blocks.CHERRY_LEAVES,
                    Blocks.AZALEA_LEAVES,
                    Blocks.FLOWERING_AZALEA_LEAVES,
                    Blocks.MOSSY_COBBLESTONE,
                    Blocks.RAW_COPPER_BLOCK,
                    Blocks.STONE,
                    Blocks.OBSERVER,
                    Blocks.REPEATER,
                    Blocks.COMPARATOR,
                    Blocks.REDSTONE_TORCH,
                    Blocks.REDSTONE_LAMP,
                    Blocks.REDSTONE_WIRE,
                    Blocks.DROPPER);

    private static final Set<Block> IGNORED_OLD_BLOCKS =
            Set.of(Blocks.AMETHYST_BLOCK, Blocks.STONE, Blocks.FROSTED_ICE);

    private final BooleanSetting renderChunks =
            this.addSetting("render-chunks", new BooleanSetting("Render Chunks", true));

    private final BooleanSetting clearOnToggle =
            this.addSetting("clear-chunks-on-toggle", new BooleanSetting("Clear On Toggle", true));

    private final BooleanSetting highlightSources =
            this.addSetting(
                    "render-flagged-chunk", new BooleanSetting("Highlight Source Chunks", true));

    private final ColorSetting chunkFill =
            this.addSetting("chunk-side-color", new ColorSetting("Chunk Fill Color", 0x28FF2828));

    private final ColorSetting chunkLine =
            this.addSetting("chunk-line-color", new ColorSetting("Chunk Line Color", -55256));

    private final BooleanSetting advancedDebug =
            this.addSetting("advanced-debug", new BooleanSetting("Advanced Debug", false));

    private final BooleanSetting render =
            this.addSetting("render", new BooleanSetting("Debug Render", true));

    private final ColorSetting fillColor =
            this.addSetting("side-color", new ColorSetting("Debug Fill Color", 0x28FF2828));

    private final ColorSetting lineColor =
            this.addSetting("line-color", new ColorSetting("Debug Line Color", -55256));

    private final ChunkSnapshotStore snapshots =
            new ChunkSnapshotStore(Blocks.AIR.getDefaultState());

    private final Set<ChunkPos> justSent = new HashSet<ChunkPos>();

    private final List<BlockPos> changedBlocks = new ArrayList<BlockPos>();

    private final Set<ChunkPos> flaggedChunks = new HashSet<ChunkPos>();

    private final Set<ChunkPos> sourceChunks = new HashSet<ChunkPos>();

    private Set<ChunkPos> previousRange = Set.of();

    private ChunkPos lastPos;

    private ClientWorld sessionLevel;

    private ClientPlayNetworkHandler connection;

    public QuantumModule() {
        super(
                "quantum",
                "Quantum & Render",
                "Finds player activity in resent chunks and marks the area around it.",
                ModuleCategory.BASE_RAID);
        this.usesNotifications().set(false);
        this.highlightSources.visibleWhen(this.renderChunks::get);
        this.chunkFill.visibleWhen(this.renderChunks::get);
        this.chunkLine.visibleWhen(this.renderChunks::get);
        this.render.visibleWhen(this.advancedDebug::get);
        this.fillColor.visibleWhen(() -> this.render.get() && this.advancedDebug.get());
        this.lineColor.visibleWhen(() -> this.render.get() && this.advancedDebug.get());
    }

    @Override
    protected void onEnable() {
        this.sessionLevel = null;
        this.resetSession();
        if (this.clearOnToggle.get()) {
            this.flaggedChunks.clear();
            this.sourceChunks.clear();
        }
    }

    @Override
    protected void onDisable() {
        this.snapshots.clear();
    }

    private void resetSession() {
        this.snapshots.clear();
        this.justSent.clear();
        this.changedBlocks.clear();
        this.previousRange = Set.of();
        this.lastPos = null;
    }

    private void ensureSession(ClientWorld sessionLevel2) {
        this.ensureConnection();
        if (sessionLevel2 == this.sessionLevel) {
            return;
        }
        this.sessionLevel = sessionLevel2;
        this.resetSession();
    }

    private void ensureConnection() {
        ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler == this.connection) {
            return;
        }
        this.connection = networkHandler;
        this.flaggedChunks.clear();
        this.sourceChunks.clear();
    }

    @EventHandler(stage = Stage.POST)
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        this.ensureSession(client.world);
        if (!LevelUtil.isOverworld(client.world)) {
            return;
        }
        ChunkPos chunkPos2 = client.player.getChunkPos();
        if (this.lastPos != null && !this.lastPos.equals(chunkPos2)) {
            Set<ChunkPos> set = QuantumModule.chunksInRange(chunkPos2);
            for (ChunkPos chunkPos3 : set) {
                if (this.previousRange.contains(chunkPos3)) continue;
                this.snapshots.store(
                        chunkPos3.x,
                        chunkPos3.z,
                        QuantumModule.extractSections(
                                client.world.getChunk(chunkPos3.x, chunkPos3.z)));
            }
            this.justSent.removeIf(chunkPos -> !set.contains(chunkPos));
            HashSet set2 = new HashSet();
            for (ChunkPos chunkPos4 : set) {
                set2.add(ChunkSnapshotStore.chunkKey(chunkPos4.x, chunkPos4.z));
            }
            this.snapshots.retain(set2::contains);
            this.previousRange = set;
        }
        this.lastPos = chunkPos2;
    }

    @EventHandler
    public void onPacketApplied(PacketAppliedEvent event) {
        Packet<?> packet = event.packet;
        if (!(packet instanceof ChunkDataS2CPacket)) {
            return;
        }
        ChunkDataS2CPacket chunkDataPacket = (ChunkDataS2CPacket) packet;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (client.player == null || world == null) {
            return;
        }
        this.ensureSession(world);
        if (!LevelUtil.isOverworld(world)) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ());
        if (!QuantumModule.chunksInRange(client.player.getChunkPos()).contains(chunkPos)) {
            return;
        }
        if (!LevelUtil.isLoaded(world, chunkPos.x, chunkPos.z)) {
            return;
        }
        PalettedContainer<BlockState>[] sections =
                QuantumModule.extractSections(world.getChunk(chunkPos.x, chunkPos.z));
        List<ChunkSnapshotStore.BlockChange> list =
                this.snapshots.compare(
                        chunkPos.x, chunkPos.z, sections, world.getBottomSectionCoord());
        for (ChunkSnapshotStore.BlockChange blockChange : list) {
            Block block = blockChange.newState().getBlock();
            Block block2 = blockChange.oldState().getBlock();
            if (IGNORED_NEW_BLOCKS.contains(block) || IGNORED_OLD_BLOCKS.contains(block2)) continue;
            this.onActivity(chunkPos, blockChange, block, block2);
            break;
        }
        this.justSent.add(chunkPos);
        this.snapshots.store(chunkPos.x, chunkPos.z, sections);
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent event) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null || !LevelUtil.isOverworld(world)) {
            return;
        }
        BlockPos pos = event.pos;
        int i = (pos.getY() >> 4) - world.getBottomSectionCoord();
        this.snapshots.update(
                pos.getX() >> 4,
                pos.getZ() >> 4,
                i,
                pos.getX() & 0xF,
                pos.getY() & 0xF,
                pos.getZ() & 0xF,
                event.newState);
    }

    private void onActivity(
            ChunkPos chunkPos,
            ChunkSnapshotStore.BlockChange blockChange,
            Block block,
            Block block2) {
        ModuleChat.Body body =
                ModuleChat.body()
                        .text("Activity ")
                        .good("detected")
                        .text(" in chunk ")
                        .value(chunkPos.x + ", " + chunkPos.z);
        if (this.advancedDebug.get()) {
            body.text(": ")
                    .name(block2.getName().getString())
                    .text(" changed to ")
                    .name(block.getName().getString());
        }
        ModuleChat.send((ClientModule) this, body);
        this.changedBlocks.add(
                new BlockPos(blockChange.worldX(), blockChange.worldY(), blockChange.worldZ()));
        this.flagArea(chunkPos);
    }

    private void flagArea(ChunkPos chunkPos) {
        this.ensureConnection();
        this.sourceChunks.add(chunkPos);
        ArrayList<ChunkPos> list = new ArrayList<ChunkPos>();
        for (int i = -9; i <= 9; ++i) {
            for (int j = -9; j <= 9; ++j) {
                list.add(new ChunkPos(chunkPos.x + i, chunkPos.z + j));
            }
        }
        if (this.flaggedChunks.isEmpty()) {
            this.flaggedChunks.addAll(list);
            return;
        }
        ArrayList<ChunkPos> list2 = new ArrayList<ChunkPos>();
        for (ChunkPos chunkPos2 : list) {
            if (!this.flaggedChunks.contains(chunkPos2)) continue;
            list2.add(chunkPos2);
        }
        this.flaggedChunks.clear();
        this.flaggedChunks.addAll(list2.isEmpty() ? list : list2);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || !LevelUtil.isOverworld(client.world)) {
            return;
        }
        this.ensureConnection();
        if (this.renderChunks.get()) {
            this.renderFlaggedChunks();
        }
        if (!this.render.get() || !this.advancedDebug.get()) {
            return;
        }
        DrawStyle drawStyle =
                DrawStyle.filledAndStroked(this.lineColor.argb(), 1.5f, this.fillColor.argb());
        for (BlockPos changedBlock : this.changedBlocks) {
            GizmoDrawing.box(new Box(changedBlock), drawStyle).ignoreOcclusion();
        }
        DrawStyle drawStyle2 = DrawStyle.filledAndStroked(-16711936, 1.5f, 671153920);
        for (ChunkPos chunkPos : QuantumModule.chunksInRange(client.player.getChunkPos())) {
            if (!this.justSent.contains(chunkPos)) continue;
            GizmoDrawing.box(QuantumModule.marker(chunkPos), drawStyle2).ignoreOcclusion();
        }
    }

    private void renderFlaggedChunks() {
        DrawStyle drawStyle =
                DrawStyle.filledAndStroked(this.chunkLine.argb(), 1.5f, this.chunkFill.argb());
        boolean highlightSources2 = this.highlightSources.get();
        for (ChunkPos flaggedChunk : this.flaggedChunks) {
            if (highlightSources2 && this.sourceChunks.contains(flaggedChunk)) continue;
            GizmoDrawing.box(QuantumModule.marker(flaggedChunk), drawStyle).ignoreOcclusion();
        }
        if (!highlightSources2) {
            return;
        }
        DrawStyle drawStyle2 = DrawStyle.filledAndStroked(-16711681, 1.5f, 671154175);
        for (ChunkPos sourceChunk : this.sourceChunks) {
            GizmoDrawing.box(QuantumModule.marker(sourceChunk), drawStyle2).ignoreOcclusion();
        }
    }

    private static Box marker(ChunkPos chunkPos) {
        double d = (double) chunkPos.x * 16.0;
        double e = (double) chunkPos.z * 16.0;
        return new Box(d, 63.0, e, d + 16.0, 63.0, e + 16.0);
    }

    static Set<ChunkPos> chunksInRange(ChunkPos chunkPos) {
        LinkedHashSet<ChunkPos> set = new LinkedHashSet<ChunkPos>();
        for (int i = -8; i <= 8; ++i) {
            int k = Math.max(0, 3 - (8 - Math.abs(i)));
            int m = 8 - k;
            for (int j = -m; j <= m; ++j) {
                set.add(new ChunkPos(chunkPos.x + j, chunkPos.z + i));
            }
        }
        return set;
    }

    private static PalettedContainer<BlockState>[] extractSections(WorldChunk chunk) {
        ChunkSection[] sectionArray = chunk.getSectionArray();
        PalettedContainer[] palettedContainers = new PalettedContainer[sectionArray.length];
        for (int i = 6; i <= 23 && i < sectionArray.length; ++i) {
            ChunkSection section = sectionArray[i];
            if (section == null || section.isEmpty()) continue;
            palettedContainers[i] = section.getBlockStateContainer().copy();
        }
        return palettedContainers;
    }
}
