/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.efn.client.chat.ModuleChat;
import net.efn.client.esp.BlockLayer;
import net.efn.client.esp.EspStyle;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BlockColorListSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.util.BlockEntityLeakTracker;
import net.efn.client.util.LightLeakScanner;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

@Environment(value = EnvType.CLIENT)
public final class BlockEntityDebugModule extends ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final int SETTINGS_VERSION = 1;

    private static final int SUMMARY_TICKS = 1200;

    private static final float FILL_ALPHA = 0.8f;

    private static final int MAX_CHAT_PER_TICK = 5;

    private static final int MAX_NBT_CHARS = 200;

    private static final Set<String> NEW_IN_TABLE =
            Set.of("minecraft:jukebox", "minecraft:piston", "minecraft:sticky_piston");

    private final ModeSetting sDetection =
            this.addSetting(
                    "detection",
                    new ModeSetting(
                            "Detection", "All", "All", "Below Zero", "Below -20", "Below -30"));

    private final BooleanSetting sBlockFilter =
            this.addSetting("block-filter", new BooleanSetting("Block Filter", false));

    private final BooleanSetting sChat =
            this.addSetting("chat", new BooleanSetting("Chat Notifications", false));

    private final BooleanSetting sShowNbt =
            this.addSetting("show-nbt", new BooleanSetting("Show NBT", true));

    private final BooleanSetting sHints =
            this.addSetting("hidden-hints", new BooleanSetting("Hidden Block Hints", true));

    private final BooleanSetting sLightLeaks =
            this.addSetting("light-leaks", new BooleanSetting("Light Leaks", true));

    private final EspStyle style = new EspStyle(this::addSetting, "Boxes", 0.8f, 256.0f);

    private final BooleanSetting sTracers =
            this.addSetting("tracers", new BooleanSetting("Tracers", true));

    private final BooleanSetting sCustomTracerColor =
            this.addSetting(
                    "custom-tracer-color", new BooleanSetting("Custom Tracer Color", false));

    private final ColorSetting sTracerColor =
            this.addSetting("tracer-color", new ColorSetting("Tracer Color", -1));

    private final BlockColorListSetting sBlockColors =
            this.addSetting(BlockColorListSetting.createForBlockEntityDebug());

    private final BlockLayer layer =
            new BlockLayer(
                    new BlockLayer.Palette() {

                        @Override
                        public int color(Object object) {
                            return BlockEntityDebugModule.this.sBlockColors.colorArgb(
                                    (Block) object);
                        }

                        @Override
                        public int tracerColor(Object object2) {
                            return BlockEntityDebugModule.this.tracerColorOf((Block) object2);
                        }
                    },
                    () ->
                            MinecraftClient.getInstance().world == null
                                    ? null
                                    : MinecraftClient.getInstance().world.getRegistryKey());

    private final LongOpenHashSet spawners = new LongOpenHashSet();

    private final LightLeakScanner lightScanner = new LightLeakScanner();

    private int ticks;

    private String lastSummary = "";

    private int seenGeneration = -1;

    private Set<String> scannedIds = Set.of();

    public BlockEntityDebugModule() {
        super(
                "block-entity-debug",
                "Block Entity Debug",
                "Renders every leaked block entity as ESP (always-on Netty capture), plus blocks"
                    + " the server hides but still gives away through chest/crafter events, sounds"
                    + " and light.",
                ModuleCategory.BASE_RAID);
        this.sShowNbt.visibleWhen(this.sChat::get);
        this.sLightLeaks.visibleWhen(this.sHints::get);
        this.sHints.onChange(this::resync);
        this.sLightLeaks.onChange(this::restartLightScan);
        this.sCustomTracerColor.visibleWhen(this.sTracers::get);
        this.sTracerColor.visibleWhen(() -> this.sTracers.get() && this.sCustomTracerColor.get());
        this.sDetection.onChange(this::resync);
        this.sBlockFilter.onChange(this::resync);
        this.sBlockColors.onChange(this::onBlockColorsChanged);
    }

    @Override
    public int settingsVersion() {
        return 1;
    }

    @Override
    public void migrateSettings(int i) {
        this.style.shape.set("Boxes");
        this.style.boxSize.set(1.0f);
        this.style.fillAlpha.set(0.8f);
        this.sBlockColors.resetColors();
        int rgbaToArgb = BlockColorListSetting.rgbaToArgb(this.sBlockColors.defaultColor());
        boolean sBlockFilter2 = this.sBlockFilter.get();
        for (BlockColorListSetting.BlockEntry entry : this.sBlockColors.entries()) {
            if (BlockColorListSetting.rgbaToArgb(entry.color()) == rgbaToArgb
                    || sBlockFilter2 && !NEW_IN_TABLE.contains(entry.id())) continue;
            this.sBlockColors.select(entry);
        }
    }

    private int tracerColorOf(Block block) {
        int i =
                this.sBlockColors.isEnabled(block)
                        ? this.sBlockColors.tracerColorArgb(block)
                        : this.sBlockColors.colorArgb(block);
        int j = i;
        if (i == 0) {
            return 0;
        }
        return this.sCustomTracerColor.get() ? this.sTracerColor.argb() : i;
    }

    private void onBlockColorsChanged() {
        if (this.isEnabled()
                && this.sBlockFilter.get()
                && !this.sBlockColors.enabledIds().equals(this.scannedIds)) {
            this.resync();
        }
    }

    @Override
    protected void onEnable() {
        BlockEntityLeakTracker.recordAdditions(true);
        this.resync();
        this.restartLightScan();
    }

    @Override
    protected void onDisable() {
        BlockEntityLeakTracker.recordAdditions(false);
        this.lightScanner.clear();
        this.layer.clear();
        this.spawners.clear();
    }

    private void restartLightScan() {
        this.lightScanner.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (!this.isEnabled()
                || !this.lightLeaksOn()
                || client.world == null
                || client.player == null) {
            return;
        }
        this.lightScanner.queueLoaded(
                client.world, client.player.getBlockPos(), client.options.getClampedViewDistance());
    }

    private boolean lightLeaksOn() {
        return this.sHints.get() && this.sLightLeaks.get();
    }

    private void resync() {
        if (!this.isEnabled()) {
            return;
        }
        this.layer.clear();
        this.spawners.clear();
        this.scannedIds = this.sBlockColors.enabledIds();
        this.seenGeneration = BlockEntityLeakTracker.generation();
        while (BlockEntityLeakTracker.pollAdded() != null) {}
        for (Map.Entry<BlockPos, BlockEntityLeakTracker.Entry> entry :
                BlockEntityLeakTracker.view().entrySet()) {
            this.accept(entry.getKey(), entry.getValue());
        }
    }

    @EventHandler
    public void onPacketApplied(PacketAppliedEvent event) {
        if (this.lightLeaksOn()) {
            this.lightScanner.onPacketApplied(event.packet);
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        BlockPos pos;
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.lightLeaksOn() && client.world != null) {
            this.lightScanner.tick(client.world);
        }
        if (++this.ticks % 1200 == 0) {
            this.logSources();
        }
        if (BlockEntityLeakTracker.generation() != this.seenGeneration) {
            this.resync();
            return;
        }
        int i = 0;
        int j = 0;
        while ((pos = BlockEntityLeakTracker.pollAdded()) != null) {
            BlockEntityLeakTracker.Entry entry = BlockEntityLeakTracker.view().get(pos);
            if (entry == null || !this.accept(pos, entry) || !this.sChat.get()) continue;
            if (i < 5) {
                this.announce(pos, entry);
                ++i;
                continue;
            }
            ++j;
        }
        if (j > 0) {
            ModuleChat.send(
                    (ClientModule) this, ModuleChat.body().text("+").value(j).text(" more"));
        }
    }

    private void logSources() {
        String string =
                "chunk="
                        + BlockEntityLeakTracker.count(BlockEntityLeakTracker.Source.CHUNK)
                        + " data="
                        + BlockEntityLeakTracker.count(BlockEntityLeakTracker.Source.DATA)
                        + " event="
                        + BlockEntityLeakTracker.count(BlockEntityLeakTracker.Source.EVENT)
                        + " sound="
                        + BlockEntityLeakTracker.count(BlockEntityLeakTracker.Source.SOUND)
                        + " light="
                        + BlockEntityLeakTracker.count(BlockEntityLeakTracker.Source.LIGHT);
        if (string.equals(this.lastSummary)) {
            return;
        }
        this.lastSummary = string;
        LOGGER.info("[EFN] Block Entity Debug sources: {}", (Object) string);
    }

    private boolean accept(BlockPos pos, BlockEntityLeakTracker.Entry entry) {
        if (!(entry.real()
                || this.sHints.get()
                        && (entry.source() != BlockEntityLeakTracker.Source.LIGHT
                                || this.sLightLeaks.get()))) {
            return false;
        }
        Block block = entry.block();
        if (!this.passesDetection(pos)) {
            return false;
        }
        if (this.sBlockFilter.get() && block != Blocks.AIR && !this.sBlockColors.isEnabled(block)) {
            return false;
        }
        this.layer.put(pos.asLong(), block);
        if (block == Blocks.SPAWNER) {
            this.spawners.add(pos.asLong());
        }
        return true;
    }

    private void announce(BlockPos pos, BlockEntityLeakTracker.Entry entry) {
        NbtCompound nbt = entry.tag();
        ModuleChat.Body body =
                ModuleChat.body()
                        .name(
                                entry.block() == Blocks.AIR
                                        ? "Hidden block"
                                        : entry.block().getName().getString());
        if (!entry.real()) {
            body.text(" (from " + entry.source().name().toLowerCase() + ")");
        }
        String string =
                nbt == null
                        ? null
                        : (String)
                                nbt.getCompound("SpawnData")
                                        .flatMap(nbtCompound -> nbtCompound.getCompound("entity"))
                                        .flatMap(nbtCompound -> nbtCompound.getString("id"))
                                        .orElse(null);
        String string2 = string;
        if (string != null) {
            body.text(" (").value(string.replace("minecraft:", "")).text(")");
        }
        body.text(" at ").coords(pos.getX(), pos.getY(), pos.getZ());
        if (this.sShowNbt.get() && nbt != null) {
            Object object = nbt.toString();
            if (((String) object).length() > 200) {
                object = ((String) object).substring(0, 200) + "...";
            }
            Text prettyPrintedText = NbtHelper.toPrettyPrintedText(nbt);
            body.append(
                    Text.literal(" " + (String) object)
                            .styled(
                                    style ->
                                            style.withHoverEvent(
                                                    new HoverEvent.ShowText(prettyPrintedText))));
        }
        ModuleChat.send((ClientModule) this, body);
    }

    private boolean passesDetection(BlockPos pos) {
        return switch (this.sDetection.get()) {
            case "Below Zero" -> {
                if (pos.getY() < 0) {
                    yield true;
                }
                yield false;
            }
            case "Below -20" -> {
                if (pos.getY() < -20) {
                    yield true;
                }
                yield false;
            }
            case "Below -30" -> {
                if (pos.getY() < -30) {
                    yield true;
                }
                yield false;
            }
            default -> true;
        };
    }

    @EventHandler
    public void onPacketReceiveNetty(PacketReceiveNettyEvent event) {
        Packet<?> packet = event.packet;
        if (!(packet instanceof BlockUpdateS2CPacket)) {
            return;
        }
        BlockUpdateS2CPacket blockUpdatePacket = (BlockUpdateS2CPacket) packet;
        if (!BlockEntityLeakTracker.containsReal(blockUpdatePacket.getPos())) {
            return;
        }
        BlockState state = blockUpdatePacket.getState();
        if (state.isAir() || state.isOf(Blocks.STONE) || state.isOf(Blocks.DEEPSLATE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        this.layer.render(event, this.style);
        if (this.sTracers.get()) {
            this.layer.renderTracers(this.style);
        }
        if (this.spawners.isEmpty()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        Vec3d cameraPosVec = client.player.getCameraPosVec(event.partialTick);
        double d = (double) this.style.range.get() * (double) this.style.range.get();
        int colorArgb = this.sBlockColors.colorArgb(Blocks.SPAWNER);
        DrawStyle drawStyle = DrawStyle.stroked(colorArgb, this.style.lineWidth.get());
        LongIterator longIterator = this.spawners.iterator();
        while (longIterator.hasNext()) {
            long l = longIterator.next();
            BlockPos pos = BlockPos.fromLong(l);
            if (cameraPosVec.squaredDistanceTo(
                            (double) pos.getX() + 0.5,
                            (double) pos.getY() + 1.0,
                            (double) pos.getZ() + 0.5)
                    > d) continue;
            GizmoDrawing.box(
                            new Box(
                                    (double) pos.getX() + 0.3,
                                    (double) pos.getY() + 1.0,
                                    (double) pos.getZ() + 0.3,
                                    (double) pos.getX() + 0.7,
                                    (double) pos.getY() + 257.0,
                                    (double) pos.getZ() + 0.7),
                            drawStyle)
                    .ignoreOcclusion();
        }
    }
}
