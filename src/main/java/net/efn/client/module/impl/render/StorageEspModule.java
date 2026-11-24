package net.efn.client.module.impl.render;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.efn.client.esp.BlockLayer;
import net.efn.client.esp.ChunkScanner;
import net.efn.client.esp.EspStyle;
import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class StorageEspModule extends ClientModule implements ChunkScanner.Consumer {

    private final EspStyle style = new EspStyle(this::addSetting, "Outline", 0.25F, 256.0F);

    private final BooleanSetting sTracers =
            this.addSetting("tracers", new BooleanSetting("Tracers", false));

    private final StorageEspModule.Type chest =
            new StorageEspModule.Type(
                    "Chest",
                    true,
                    -6530304,
                    "chests",
                    "chest-color",
                    "chest-tracers",
                    "chest-tracer-color");

    private final StorageEspModule.Type enderChest =
            new StorageEspModule.Type(
                    "Ender Chest",
                    true,
                    -9109249,
                    "ender-chests",
                    "ender-chest-color",
                    "ender-chest-tracers",
                    "ender-chest-tracer-color");

    private final StorageEspModule.Type shulker =
            new StorageEspModule.Type(
                    "Shulker Box",
                    true,
                    -7995234,
                    "shulker-boxes",
                    "shulker-box-color",
                    "shulker-tracers",
                    "shulker-tracer-color");

    private final StorageEspModule.Type spawner =
            new StorageEspModule.Type(
                    "Spawner",
                    true,
                    -7700826,
                    "spawners",
                    "spawner-color",
                    "spawner-tracers",
                    "spawner-tracer-color");

    private final StorageEspModule.Type furnace =
            new StorageEspModule.Type(
                    "Furnace",
                    false,
                    -10197916,
                    "furnaces",
                    "furnace-color",
                    "furnace-tracers",
                    "furnace-tracer-color");

    private final StorageEspModule.Type barrel =
            new StorageEspModule.Type(
                    "Barrel",
                    true,
                    -10204366,
                    "barrels",
                    "barrel-color",
                    "barrel-tracers",
                    "barrel-tracer-color");

    private final StorageEspModule.Type enchanting =
            new StorageEspModule.Type(
                    "Enchanting Table",
                    true,
                    -3670016,
                    "enchanting-tables",
                    "enchanting-table-color",
                    "enchanting-tracers",
                    "enchanting-tracer-color");

    private final StorageEspModule.Type hopper =
            new StorageEspModule.Type(
                    "Hopper",
                    false,
                    -11513776,
                    "hoppers",
                    "hopper-color",
                    "hopper-tracers",
                    "hopper-tracer-color");

    private final StorageEspModule.Type[] types =
            new StorageEspModule.Type[] {
                this.chest,
                this.enderChest,
                this.shulker,
                this.spawner,
                this.furnace,
                this.barrel,
                this.enchanting,
                this.hopper
            };

    private final BlockLayer layer =
            new BlockLayer(
                    new BlockLayer.Palette() {

                        @Override
                        public int color(Object object) {
                            return ((StorageEspModule.Type) object).color.argb();
                        }

                        @Override
                        public int tracerColor(Object object2) {
                            StorageEspModule.Type type = (StorageEspModule.Type) object2;
                            return type.tracers.get() ? type.tracerColor.argb() : 0;
                        }
                    },
                    () ->
                            MinecraftClient.getInstance().world == null
                                    ? null
                                    : MinecraftClient.getInstance().world.getRegistryKey());

    private final Map<RegistryKey<World>, Long2ObjectOpenHashMap<StorageEspModule.Type>>
            detections = new HashMap<>();

    private RegistryKey<World> shownDimension;

    public StorageEspModule() {
        super(
                "storage-esp",
                "Storage ESP Bypass",
                "Highlights storage blocks and spawners with per-type colours.",
                ModuleCategory.BASE_RAID);
    }

    private StorageEspModule.Type typeOf(Block block) {
        if (block instanceof ChestBlock) {
            return this.chest;
        } else if (block instanceof EnderChestBlock) {
            return this.enderChest;
        } else if (block instanceof ShulkerBoxBlock) {
            return this.shulker;
        } else if (block instanceof SpawnerBlock) {
            return this.spawner;
        } else if (block instanceof AbstractFurnaceBlock) {
            return this.furnace;
        } else if (block instanceof BarrelBlock) {
            return this.barrel;
        } else if (block instanceof EnchantingTableBlock) {
            return this.enchanting;
        } else {
            return block instanceof HopperBlock ? this.hopper : null;
        }
    }

    private StorageEspModule.Type typeOf(BlockEntityType<?> blockEntityType) {
        if (blockEntityType == BlockEntityType.CHEST
                || blockEntityType == BlockEntityType.TRAPPED_CHEST) {
            return this.chest;
        } else if (blockEntityType == BlockEntityType.ENDER_CHEST) {
            return this.enderChest;
        } else if (blockEntityType == BlockEntityType.SHULKER_BOX) {
            return this.shulker;
        } else if (blockEntityType == BlockEntityType.MOB_SPAWNER) {
            return this.spawner;
        } else if (blockEntityType == BlockEntityType.FURNACE
                || blockEntityType == BlockEntityType.BLAST_FURNACE
                || blockEntityType == BlockEntityType.SMOKER) {
            return this.furnace;
        } else if (blockEntityType == BlockEntityType.BARREL) {
            return this.barrel;
        } else if (blockEntityType == BlockEntityType.ENCHANTING_TABLE) {
            return this.enchanting;
        } else {
            return blockEntityType == BlockEntityType.HOPPER ? this.hopper : null;
        }
    }

    private StorageEspModule.Type typeOf(BlockEntityType<?> blockEntityType, NbtCompound nbt) {
        StorageEspModule.Type type = this.typeOf(blockEntityType);
        if (type == null && nbt != null) {
            Identifier id = nbt.getString("id").map(Identifier::tryParse).orElse(null);
            return id == null ? null : this.typeOf(Registries.BLOCK_ENTITY_TYPE.get(id));
        } else {
            return type;
        }
    }

    private void rescan() {
        if (this.isEnabled()) {
            this.layer.clear();
            ClientWorld world = MinecraftClient.getInstance().world;
            this.shownDimension = world == null ? null : world.getRegistryKey();
            Long2ObjectOpenHashMap long2ObjectOpenHashMap =
                    this.shownDimension == null ? null : this.detections.get(this.shownDimension);
            if (long2ObjectOpenHashMap != null) {
                for (Entry long2ObjectEntry :
                        (Iterable<Entry>) long2ObjectOpenHashMap.long2ObjectEntrySet()) {
                    if (((StorageEspModule.Type) long2ObjectEntry.getValue()).enabled.get()) {
                        this.layer.put(long2ObjectEntry.getLongKey(), long2ObjectEntry.getValue());
                    }
                }
            }
        }
    }

    private boolean anyTracer() {
        if (!this.sTracers.get()) {
            return false;
        } else {
            for (StorageEspModule.Type type : this.types) {
                if (type.enabled.get() && type.tracers.get()) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    protected void onEnable() {
        this.detections.clear();
        this.layer.clear();
        ChunkScanner.register(this);
        ClientWorld world = MinecraftClient.getInstance().world;
        RegistryKey key = this.shownDimension = world == null ? null : world.getRegistryKey();
        if (world != null && MinecraftClient.getInstance().player != null) {
            ChunkPos chunkPos = MinecraftClient.getInstance().player.getChunkPos();
            int i = MinecraftClient.getInstance().options.getClampedViewDistance() + 1;
            for (int j = chunkPos.x - i; j <= chunkPos.x + i; j++) {
                for (int k = chunkPos.z - i; k <= chunkPos.z + i; k++) {
                    WorldChunk worldChunk = world.getChunkManager().getWorldChunk(j, k);
                    if (worldChunk != null) {
                        this.scanBlockEntities(worldChunk);
                    }
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        ChunkScanner.unregister(this);
        this.detections.clear();
        this.layer.clear();
        this.shownDimension = null;
    }

    private void found(BlockPos pos, StorageEspModule.Type type) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (type != null && world != null) {
            long l = pos.asLong();
            StorageEspModule.Type type2 =
                    this.detections
                            .computeIfAbsent(
                                    world.getRegistryKey(),
                                    registryKey -> new Long2ObjectOpenHashMap<>())
                            .put(l, type);
            if (type2 != type) {
                if (type2 != null) {
                    this.layer.remove(l);
                }
                if (type.enabled.get()) {
                    this.layer.put(l, type);
                }
            }
        }
    }

    private void gone(BlockPos pos) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null) {
            Long2ObjectOpenHashMap detection = this.detections.get(world.getRegistryKey());
            if (detection != null && detection.remove(pos.asLong()) != null) {
                this.layer.remove(pos.asLong());
            }
        }
    }

    @Override
    public boolean wants(BlockState state) {
        return this.typeOf(state.getBlock()) != null;
    }

    @Override
    public void found(long l, BlockState state) {
        this.found(BlockPos.fromLong(l), this.typeOf(state.getBlock()));
    }

    @Override
    public void removed(long l) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null && world.getBlockState(BlockPos.fromLong(l)).isAir()) {
            this.gone(BlockPos.fromLong(l));
        }
    }

    @Override
    public void reset() {
        this.detections.clear();
        this.layer.clear();
    }

    private void scanBlockEntities(WorldChunk chunk) {
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            this.found(blockEntity.getPos(), this.typeOf(blockEntity.getType()));
        }
    }

    private void blockChanged(BlockPos pos, BlockState state) {
        StorageEspModule.Type type = this.typeOf(state.getBlock());
        if (type != null) {
            this.found(pos, type);
        } else if (state.isAir()) {
            this.gone(pos);
        }
    }

    @EventHandler
    public void onPacketApplied(PacketAppliedEvent event) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null) {
            if (world.getRegistryKey() != this.shownDimension) {
                this.rescan();
            }
            Packet packet = event.packet;
            if (event.packet instanceof ChunkDataS2CPacket) {
                ChunkDataS2CPacket chunkDataPacket = (ChunkDataS2CPacket) packet;
                chunkDataPacket
                        .getChunkData()
                        .getBlockEntities(chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ())
                        .accept(
                                (blockPos, blockEntityType, nbtCompound) ->
                                        this.found(
                                                blockPos,
                                                this.typeOf(blockEntityType, nbtCompound)));
                WorldChunk worldChunk =
                        world.getChunkManager()
                                .getWorldChunk(
                                        chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ());
                if (worldChunk != null) {
                    this.scanBlockEntities(worldChunk);
                }
            } else if (event.packet instanceof BlockUpdateS2CPacket blockUpdatePacket) {
                this.blockChanged(blockUpdatePacket.getPos(), blockUpdatePacket.getState());
            } else if (event.packet instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdatePacket) {
                chunkDeltaUpdatePacket.visitUpdates(this::blockChanged);
            } else if (event.packet instanceof GameJoinS2CPacket) {
                this.detections.clear();
                this.layer.clear();
            }
        }
    }

    @EventHandler
    public void onPacketReceiveNetty(PacketReceiveNettyEvent event) {
        if (event.packet instanceof BlockEntityUpdateS2CPacket blockEntityUpdatePacket) {
            BlockPos pos = blockEntityUpdatePacket.getPos();
            StorageEspModule.Type type =
                    this.typeOf(
                            blockEntityUpdatePacket.getBlockEntityType(),
                            blockEntityUpdatePacket.getNbt());
            if (type != null) {
                MinecraftClient.getInstance()
                        .execute(
                                () -> {
                                    if (this.isEnabled()) {
                                        this.found(pos, type);
                                    }
                                });
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null && world.getRegistryKey() != this.shownDimension) {
            this.rescan();
        }
        this.layer.render(event, this.style);
        if (this.anyTracer()) {
            this.layer.renderTracers(this.style);
        }
    }

    @Environment(EnvType.CLIENT)
    private final class Type {

        final BooleanSetting enabled;

        final BooleanSetting tracers;

        final ColorSetting color;

        final ColorSetting tracerColor;

        Type(
                String string,
                boolean bl,
                int i,
                String string2,
                String string3,
                String string4,
                String string5) {
            this.enabled =
                    StorageEspModule.this.addSetting(string2, new BooleanSetting(string, bl));
            this.color =
                    StorageEspModule.this.addSetting(
                            string3, new ColorSetting(string + " Color", i));
            this.tracers =
                    StorageEspModule.this.addSetting(
                            string4, new BooleanSetting(string + " Tracers", false));
            this.tracerColor =
                    StorageEspModule.this.addSetting(
                            string5, new ColorSetting(string + " Tracer Color", i));
            this.color.visibleWhen(this.enabled::get);
            this.tracers.visibleWhen(
                    () -> StorageEspModule.this.sTracers.get() && this.enabled.get());
            this.tracerColor.visibleWhen(
                    () ->
                            StorageEspModule.this.sTracers.get()
                                    && this.enabled.get()
                                    && this.tracers.get());
            this.enabled.onChange(StorageEspModule.this::rescan);
        }
    }
}
