package net.efn.client.module.impl.misc;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import net.efn.client.esp.ChunkScanner;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ChallengeToast;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BlockColorListSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class BlockNotifierModule extends ClientModule implements ChunkScanner.Consumer {

    private static final int MAX_KNOWN = 50000;

    private final BlockColorListSetting blocks =
            this.addSetting(
                    "blocks",
                    BlockColorListSetting.blockList("Blocks", List.of("minecraft:spawner")));

    private final BooleanSetting sound =
            this.addSetting("sound", new BooleanSetting("Sound", true));

    private final LongLinkedOpenHashSet known = new LongLinkedOpenHashSet();

    private final Map<Block, BlockNotifierModule.Finds> pending = new LinkedHashMap<>();

    private Set<String> scannedIds = Set.of();

    private RegistryKey<World> dimension;

    public BlockNotifierModule() {
        super(
                "block-notifier",
                "Block Notifier",
                "Shows a toast with the block and its coordinates when one of your chosen blocks"
                        + " shows up.",
                ModuleCategory.MISC);
        this.blocks.onChange(this::rescan);
    }

    @Override
    protected void onEnable() {
        this.known.clear();
        this.pending.clear();
        this.dimension = null;
        this.scannedIds = this.blocks.enabledIds();
        ChunkScanner.register(this);
        this.on(TickEvent.TYPE, Stage.POST, this::onTick);
    }

    @Override
    protected void onDisable() {
        ChunkScanner.unregister(this);
        this.known.clear();
        this.pending.clear();
    }

    private void rescan() {
        if (this.isEnabled()) {
            Set set = this.blocks.enabledIds();
            if (!set.equals(this.scannedIds)) {
                this.scannedIds = set;
                ChunkScanner.rescan();
            }
        }
    }

    @Override
    public boolean wants(BlockState state) {
        return this.blocks.isEnabled(state.getBlock());
    }

    @Override
    public void found(long l, BlockState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            this.syncDimension(client);
            if (this.known.add(l)) {
                if (this.known.size() > 50000) {
                    this.known.removeFirstLong();
                }
                BlockPos pos = BlockPos.fromLong(l);
                BlockNotifierModule.Finds finds =
                        this.pending.computeIfAbsent(
                                state.getBlock(), block -> new BlockNotifierModule.Finds());
                finds.count++;
                double d =
                        client.player == null
                                ? 0.0
                                : client.player.squaredDistanceTo(Vec3d.ofCenter(pos));
                if (d < finds.nearestSq) {
                    finds.nearestSq = d;
                    finds.nearest = pos;
                }
            }
        }
    }

    @Override
    public void removed(long l) {
        this.known.remove(l);
    }

    @Override
    public void reset() {
        this.known.clear();
        this.pending.clear();
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            this.syncDimension(client);
            if (!this.pending.isEmpty()) {
                for (Entry entry : this.pending.entrySet()) {
                    this.notify(
                            (Block) entry.getKey(), (BlockNotifierModule.Finds) entry.getValue());
                }
                this.pending.clear();
            }
        }
    }

    private void syncDimension(MinecraftClient client) {
        RegistryKey registryKey = client.world.getRegistryKey();
        if (!registryKey.equals(this.dimension)) {
            this.dimension = registryKey;
            this.known.clear();
            this.pending.clear();
        }
    }

    private void notify(Block block, BlockNotifierModule.Finds finds) {
        Item item = block.asItem();
        ItemStack stack = new ItemStack(item == Items.AIR ? Items.COMPASS : item);
        String string = block.getName().getString();
        BlockPos nearest2 = finds.nearest;
        MutableText text =
                Text.literal(
                        "at " + nearest2.getX() + ", " + nearest2.getY() + ", " + nearest2.getZ());
        for (int i = 0; i < finds.count; i++) {
            ChallengeToast.addOrUpdate(stack, string, text, this.sound.get());
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Finds {

        int count;

        BlockPos nearest;

        double nearestSq = 1.7976931348623157E308;

        private Finds() {}
    }
}
