package net.efn.client.util;

import net.efn.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

@Environment(EnvType.CLIENT)
public final class BlockEntityLeakTracker {

    private static final double ON_EDGE = 0.07;

    private static final Map<BlockPos, BlockEntityLeakTracker.Entry> FOUND =
            new ConcurrentHashMap<>();

    private static final ConcurrentLinkedQueue<BlockPos> ADDED = new ConcurrentLinkedQueue<>();

    private static final AtomicInteger GENERATION = new AtomicInteger();

    private static final AtomicIntegerArray COUNTS =
            new AtomicIntegerArray(BlockEntityLeakTracker.Source.values().length);

    private static volatile boolean recordAdditions;

    private BlockEntityLeakTracker() {}

    public static Map<BlockPos, BlockEntityLeakTracker.Entry> view() {
        return FOUND;
    }

    public static boolean contains(BlockPos pos) {
        return FOUND.containsKey(pos);
    }

    public static boolean containsReal(BlockPos pos) {
        BlockEntityLeakTracker.Entry entry = FOUND.get(pos);
        return entry != null && entry.real();
    }

    public static int count(BlockEntityLeakTracker.Source source2) {
        return COUNTS.get(source2.ordinal());
    }

    public static void clear() {
        FOUND.clear();
        ADDED.clear();
        for (int i = 0; i < COUNTS.length(); i++) {
            COUNTS.set(i, 0);
        }
        GENERATION.incrementAndGet();
    }

    public static int generation() {
        return GENERATION.get();
    }

    public static void recordAdditions(boolean bl) {
        recordAdditions = bl;
        if (!bl) {
            ADDED.clear();
        }
    }

    public static BlockPos pollAdded() {
        return ADDED.poll();
    }

    public static void ingest(Packet<?> packet) {
        if (packet instanceof ChunkDataS2CPacket chunkDataPacket) {
            ChunkData chunkData = chunkDataPacket.getChunkData();
            try {
                chunkData
                        .getBlockEntities(chunkDataPacket.getChunkX(), chunkDataPacket.getChunkZ())
                        .accept(
                                (blockPos, blockEntityType, nbtCompound) -> {
                                    if (blockEntityType != null) {
                                        put(
                                                blockPos,
                                                blockEntityType,
                                                nbtCompound,
                                                BlockEntityLeakTracker.Source.CHUNK);
                                    }
                                });
            } catch (Throwable exception) {
            }
        } else if (packet instanceof BlockEntityUpdateS2CPacket blockEntityUpdatePacket) {
            put(
                    blockEntityUpdatePacket.getPos(),
                    blockEntityUpdatePacket.getBlockEntityType(),
                    blockEntityUpdatePacket.getNbt(),
                    BlockEntityLeakTracker.Source.DATA);
        } else if (packet instanceof BlockEventS2CPacket blockEventPacket) {
            if (blockEventPacket.getBlock() instanceof BlockEntityProvider) {
                hint(
                        blockEventPacket.getPos(),
                        blockEventPacket.getBlock(),
                        BlockEntityLeakTracker.Source.EVENT);
            }
        } else if (packet instanceof WorldEventS2CPacket worldEventPacket) {
            Block block2 = eventBlock(worldEventPacket.getEventId());
            if (block2 != null && !worldEventPacket.isGlobal()) {
                hint(worldEventPacket.getPos(), block2, BlockEntityLeakTracker.Source.EVENT);
            }
        } else if (packet instanceof PlaySoundS2CPacket playSoundPacket) {
            Block block3 = soundBlock(playSoundPacket.getSound());
            if (block3 != null) {
                hintAt(
                        playSoundPacket.getX(),
                        playSoundPacket.getY(),
                        playSoundPacket.getZ(),
                        block3);
            }
        } else if (packet instanceof BlockUpdateS2CPacket blockUpdatePacket) {
            dropRevealedHint(blockUpdatePacket.getPos(), blockUpdatePacket.getState());
        } else if (packet instanceof GameJoinS2CPacket
                || packet instanceof PlayerRespawnS2CPacket) {
            clear();
        }
    }

    public static void hint(BlockPos pos, Block block2, BlockEntityLeakTracker.Source source2) {
        BlockPos pos2 = pos.toImmutable();
        BlockEntityLeakTracker.Entry entry = FOUND.get(pos2);
        if (entry == null || !entry.real() && entry.block() == Blocks.AIR && block2 != Blocks.AIR) {
            FOUND.put(pos2, new BlockEntityLeakTracker.Entry(block2, null, source2));
            COUNTS.incrementAndGet(source2.ordinal());
            if (recordAdditions) {
                ADDED.add(pos2);
            }
        }
    }

    private static void put(
            BlockPos pos,
            BlockEntityType<?> blockEntityType,
            NbtCompound nbt,
            BlockEntityLeakTracker.Source source2) {
        if (!PerformanceModule.optimizationsEnabled() || !containsReal(pos)) {
            BlockPos pos2 = pos.toImmutable();
            NbtCompound nbt2 = nbt != null && !nbt.isEmpty() ? nbt : null;
            BlockEntityLeakTracker.Entry entry = FOUND.get(pos2);
            if (entry == null || !entry.real()) {
                FOUND.put(
                        pos2,
                        new BlockEntityLeakTracker.Entry(
                                blockFromType(blockEntityType), nbt2, source2));
                COUNTS.incrementAndGet(source2.ordinal());
                if (recordAdditions) {
                    ADDED.add(pos2);
                }
            } else if (nbt2 != null && !nbt2.equals(entry.tag())) {
                FOUND.replace(
                        pos2,
                        entry,
                        new BlockEntityLeakTracker.Entry(entry.block(), nbt2, entry.source()));
            }
        }
    }

    private static void dropRevealedHint(BlockPos pos, BlockState state) {
        BlockEntityLeakTracker.Entry entry = FOUND.get(pos);
        if (entry != null && !entry.real()) {
            if (!state.isAir()
                    && !state.isOf(Blocks.STONE)
                    && !state.isOf(Blocks.DEEPSLATE)
                    && !(state.getBlock() instanceof BlockEntityProvider)) {
                if (FOUND.remove(pos, entry)) {
                    GENERATION.incrementAndGet();
                }
            }
        }
    }

    private static void hintAt(double d, double e, double d2, Block block2) {
        int i = (int) Math.floor(d);
        int j = (int) Math.floor(e);
        int k = (int) Math.floor(d2);
        double e2 = d - i;
        double d3 = e - j;
        double e3 = d2 - k;
        if (!(Math.abs(d3 - 0.5) > 0.07)) {
            boolean bl = e2 < 0.07;
            boolean bl2 = e3 < 0.07;
            if ((bl || !(Math.abs(e2 - 0.5) > 0.07))
                    && (bl2 || !(Math.abs(e3 - 0.5) > 0.07))
                    && (!bl || !bl2)) {
                hint(new BlockPos(i, j, k), block2, BlockEntityLeakTracker.Source.SOUND);
                if (bl) {
                    hint(new BlockPos(i - 1, j, k), block2, BlockEntityLeakTracker.Source.SOUND);
                }
                if (bl2) {
                    hint(new BlockPos(i, j, k - 1), block2, BlockEntityLeakTracker.Source.SOUND);
                }
            }
        }
    }

    private static Block eventBlock(int i) {
        return switch (i) {
            case 1000, 1001, 1002, 2000 -> Blocks.DISPENSER;
            case 1035 -> Blocks.BREWING_STAND;
            case 1049, 1050, 2010 -> Blocks.CRAFTER;
            default -> null;
        };
    }

    private static Block soundBlock(RegistryEntry<SoundEvent> entry) {
        Identifier id = ((SoundEvent) entry.value()).id();
        if (!id.getNamespace().equals("minecraft")) {
            return null;
        } else {
            String path = id.getPath();
            if (!path.startsWith("block.")) {
                return null;
            } else {
                String string = path.substring("block.".length());
                int i = string.indexOf(46);
                if (i < 0) {
                    return null;
                } else {
                    String string2 = string.substring(0, i);
                    return switch (string2) {
                        case "chest" -> Blocks.CHEST;
                        case "ender_chest" -> Blocks.ENDER_CHEST;
                        case "barrel" -> Blocks.BARREL;
                        case "shulker_box" -> Blocks.SHULKER_BOX;
                        case "beacon" -> Blocks.BEACON;
                        case "vault" -> Blocks.VAULT;
                        case "trial_spawner" -> Blocks.TRIAL_SPAWNER;
                        case "chiseled_bookshelf" -> Blocks.CHISELED_BOOKSHELF;
                        case "copper_chest" ->
                                (Block) Registries.BLOCK.get(Identifier.ofVanilla("copper_chest"));
                        default -> null;
                    };
                }
            }
        }
    }

    public static Block blockFromType(BlockEntityType<?> blockEntityType) {
        Identifier id = Registries.BLOCK_ENTITY_TYPE.getId(blockEntityType);
        if (id == null) {
            return Blocks.AIR;
        } else {
            String path = id.getPath();
            Block block2 =
                    switch (path) {
                        case "mob_spawner" -> Blocks.SPAWNER;
                        case "piston" -> Blocks.MOVING_PISTON;
                        case "sign" -> Blocks.OAK_SIGN;
                        case "hanging_sign" -> Blocks.OAK_HANGING_SIGN;
                        case "skull" -> Blocks.SKELETON_SKULL;
                        case "banner" -> Blocks.WHITE_BANNER;
                        case "bed" -> Blocks.RED_BED;
                        case "brushable_block" -> Blocks.SUSPICIOUS_SAND;
                        default -> null;
                    };
            return block2 != null
                    ? block2
                    : Registries.BLOCK.getEntry(id).map(RegistryEntry::value).orElse(Blocks.AIR);
        }
    }

    @Environment(EnvType.CLIENT)
    public record Entry(Block block, NbtCompound tag, BlockEntityLeakTracker.Source source) {

        public boolean real() {
            return this.source == BlockEntityLeakTracker.Source.CHUNK
                    || this.source == BlockEntityLeakTracker.Source.DATA;
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum Source {
        CHUNK,
        DATA,
        EVENT,
        SOUND,
        LIGHT;

        private Source() {}
    }
}
