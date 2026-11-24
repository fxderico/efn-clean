package net.efn.client.util.playerbypass2;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Oxidizable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class SimulationSignals {

    private static final int RECENT = 6;

    private static final long SECOND = 1000000000L;

    private static final long CHUNK_SETTLE = 3000000000L;

    private static final long FRESH_ENTITY = 2000000000L;

    private static final long WANDER_MEMORY = 60000000000L;

    private static final long TELEPORT_QUIET = 2000000000L;

    private static final long LIGHTNING_LIFE = 3000000000L;

    private static final long UNDERGROUND_HOLD = 3000000000L;

    private static final long FROZEN_AFTER = 40000000000L;

    private static final int MAX_PINS = 32;

    private static final double MIN_MOVE_SQR = 1.0E-4;

    private static final double TELEPORT_JUMP_SQR = 64.0;

    private static final double SAMPLE_JUMP_SQR = 900.0;

    private static final double TRACKING_INSET = 24.0;

    private static final double TRACKED_DECAY = 0.5;

    private static final float FORGET_SCORE = 0.05F;

    private static final float WEAK_CAP = 4.0F;

    private static final float STRONG_CAP = 10.0F;

    private static final int MIN_Y = 0;

    private static final int BLOCK_SPILL = 1;

    private static final Set<Block> GROWS_FROM_AIR =
            Set.of(
                    Blocks.SUGAR_CANE,
                    Blocks.CACTUS,
                    Blocks.BAMBOO,
                    Blocks.BAMBOO_SAPLING,
                    Blocks.MELON,
                    Blocks.PUMPKIN,
                    Blocks.SNOW,
                    Blocks.FIRE,
                    Blocks.SOUL_FIRE,
                    Blocks.BROWN_MUSHROOM,
                    Blocks.RED_MUSHROOM,
                    Blocks.CHORUS_PLANT,
                    Blocks.CHORUS_FLOWER);

    private static final Set<Block> GONE_TO_AIR =
            Set.of(
                    Blocks.SNOW,
                    Blocks.FIRE,
                    Blocks.SOUL_FIRE,
                    Blocks.TURTLE_EGG,
                    Blocks.SNIFFER_EGG,
                    Blocks.FROGSPAWN);

    private static boolean active;

    private static int serverSim = -1;

    private static int serverView = -1;

    private static int simOverride;

    private static int margin;

    private static boolean ignoreVisiblePlayers = true;

    private static long memoryNanos = 30000000000L;

    private static int ticks;

    private static int session;

    private static RegistryKey<World> dimension;

    private static long quietUntil;

    private static long undergroundUntil;

    private static double lastX;

    private static double lastZ;

    private static double sampleX;

    private static double sampleZ;

    private static boolean hasLast;

    private static boolean hasSample;

    private static long passedCenter = -9223372036854775808L;

    private static int passedRadius = -1;

    private static final Long2ObjectOpenHashMap<SimulationSignals.ChunkSignal> CHUNKS =
            new Long2ObjectOpenHashMap<>();

    private static final Int2ObjectOpenHashMap<SimulationSignals.Share> ENTITY_SHARES =
            new Int2ObjectOpenHashMap<>();

    private static final Long2ObjectOpenHashMap<SimulationSignals.Share> BLOCK_SHARES =
            new Long2ObjectOpenHashMap<>();

    private static final Long2LongOpenHashMap CHUNK_LOADED = new Long2LongOpenHashMap();

    private static final Int2LongOpenHashMap ENTITY_ADDED = new Int2LongOpenHashMap();

    private static final Int2LongOpenHashMap ENTITY_INSIDE = new Int2LongOpenHashMap();

    private static final Map<RegistryKey<World>, LongOpenHashSet> PASSED_BY_DIMENSION =
            new HashMap<>();

    private static LongOpenHashSet passed = new LongOpenHashSet();

    private static final Int2DoubleOpenHashMap TRACKED_RANGE = new Int2DoubleOpenHashMap();

    private static final Int2ObjectOpenHashMap<long[]> LIGHTNING_PENDING =
            new Int2ObjectOpenHashMap<>();

    private static final Int2LongOpenHashMap LAST_ACTIVE = new Int2LongOpenHashMap();

    private static final LongOpenHashSet FROZEN = new LongOpenHashSet();

    private static final ArrayDeque<SimulationSignals.Pin> PINS = new ArrayDeque<>();

    private SimulationSignals() {}

    public static void configure(boolean bl, int i, int j, boolean bl2, int k) {
        if (active && !bl) {
            clear();
        }
        active = bl;
        simOverride = i;
        margin = j;
        ignoreVisiblePlayers = bl2;
        memoryNanos = Math.max(1, k) * 1000000000L;
    }

    public static void clear() {
        resetLevel();
        PASSED_BY_DIMENSION.clear();
        passed = new LongOpenHashSet();
        if (dimension != null) {
            PASSED_BY_DIMENSION.put(dimension, passed);
        }
    }

    private static void resetLevel() {
        CHUNKS.clear();
        ENTITY_SHARES.clear();
        BLOCK_SHARES.clear();
        ENTITY_ADDED.clear();
        ENTITY_INSIDE.clear();
        TRACKED_RANGE.clear();
        LIGHTNING_PENDING.clear();
        LAST_ACTIVE.clear();
        FROZEN.clear();
        PINS.clear();
        hasLast = false;
        hasSample = false;
        passedCenter = -9223372036854775808L;
        passedRadius = -1;
    }

    public static int session() {
        return session;
    }

    public static int serverView() {
        return serverView;
    }

    public static int simulation() {
        int i = simOverride > 0 ? simOverride : (serverSim > 0 ? serverSim : 5);
        return Math.max(1, Math.min(i, view() - 3 - margin));
    }

    public static int view() {
        int clampedViewDistance = MinecraftClient.getInstance().options.getClampedViewDistance();
        return serverView > 0 ? Math.min(serverView, clampedViewDistance) : clampedViewDistance;
    }

    public static int ringStart() {
        return simulation() + 2 + margin;
    }

    public static Long2ObjectMap<SimulationSignals.ChunkSignal> chunks() {
        return CHUNKS;
    }

    public static LongOpenHashSet frozen() {
        return FROZEN;
    }

    public static SimulationSignals.Pin pollPin() {
        return PINS.poll();
    }

    public static void dropPins() {
        PINS.clear();
    }

    public static boolean holding() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return now() < undergroundUntil || player != null && player.getY() < 0.0;
    }

    public static long now() {
        return System.nanoTime();
    }

    static float decay(long l) {
        // bytecode: L2D, DNEG, DDIV (decompiler dropped the cast -> integer division)
        return (float) Math.exp(-(double) l / memoryNanos);
    }

    public static void onLogin(int i, int j, RegistryKey<World> key) {
        serverSim = i;
        serverView = j;
        session++;
        dimension = key;
        CHUNK_LOADED.clear();
        clear();
        startQuiet();
    }

    public static void onRespawn(RegistryKey<World> key) {
        startQuiet();
        if (!key.equals(dimension)) {
            dimension = key;
            CHUNK_LOADED.clear();
            resetLevel();
            passed =
                    PASSED_BY_DIMENSION.computeIfAbsent(key, registryKeyx -> new LongOpenHashSet());
        }
    }

    public static void onTeleport() {
        startQuiet();
    }

    public static void onSimulationDistance(int i) {
        serverSim = i;
    }

    public static void onViewDistance(int i) {
        serverView = i;
    }

    public static void onChunk(int i, int j) {
        if (active) {
            CHUNK_LOADED.put(ChunkPos.toLong(i, j), now());
        }
    }

    public static void onMove(EntityS2CPacket entityPacket) {
        if (active) {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                Entity entity = entityPacket.getEntity(world);
                if (entity != null) {
                    boolean bl =
                            entityPacket.isPositionChanged()
                                    && (entityPacket.getDeltaX() != 0
                                            || entityPacket.getDeltaY() != 0
                                            || entityPacket.getDeltaZ() != 0);
                    if (bl || entityPacket.hasRotation()) {
                        LAST_ACTIVE.put(entity.getId(), now());
                    }
                    if (bl) {
                        entitySignal(entity, SimulationSignals.Kind.MOVE, 0);
                    } else if (entityPacket.hasRotation()) {
                        entitySignal(entity, SimulationSignals.Kind.HEAD, 0);
                    }
                }
            }
        }
    }

    public static void onPositionSync(EntityPositionSyncS2CPacket entityPositionSyncPacket) {
        if (active) {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                Entity entityById = world.getEntityById(entityPositionSyncPacket.id());
                if (entityById instanceof LivingEntity) {
                    if (!(entityById
                                    .getEntityPos()
                                    .squaredDistanceTo(entityPositionSyncPacket.values().position())
                            < 1.0E-4)) {
                        LAST_ACTIVE.put(entityById.getId(), now());
                        entitySignal(entityById, SimulationSignals.Kind.MOVE, 0);
                    }
                }
            }
        }
    }

    public static void onHeadRotation(EntitySetHeadYawS2CPacket entitySetHeadYawPacket) {
        if (active) {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                Entity entity = entitySetHeadYawPacket.getEntity(world);
                if (entity != null) {
                    LAST_ACTIVE.put(entity.getId(), now());
                    entitySignal(entity, SimulationSignals.Kind.HEAD, 0);
                }
            }
        }
    }

    public static void onEntityData(EntityTrackerUpdateS2CPacket entityTrackerUpdatePacket) {
        if (active) {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                if (now()
                                - ENTITY_ADDED.getOrDefault(
                                        entityTrackerUpdatePacket.id(), -4611686018427387904L)
                        >= 2000000000L) {
                    Entity entityById = world.getEntityById(entityTrackerUpdatePacket.id());
                    if (entityById != null) {
                        LAST_ACTIVE.put(entityById.getId(), now());
                        entitySignal(entityById, SimulationSignals.Kind.STATE, 0);
                    }
                }
            }
        }
    }

    public static void onEntityEvent(EntityStatusS2CPacket entityStatusPacket) {
        if (active) {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                Entity entity = entityStatusPacket.getEntity(world);
                if (entity != null) {
                    LAST_ACTIVE.put(entity.getId(), now());
                    entitySignal(entity, SimulationSignals.Kind.EVENT, 0);
                }
            }
        }
    }

    public static void onEquipment(EntityEquipmentUpdateS2CPacket entityEquipmentUpdatePacket) {
        if (active) {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                if (now()
                                - ENTITY_ADDED.getOrDefault(
                                        entityEquipmentUpdatePacket.getEntityId(),
                                        -4611686018427387904L)
                        >= 2000000000L) {
                    Entity entityById =
                            world.getEntityById(entityEquipmentUpdatePacket.getEntityId());
                    if (entityById != null) {
                        LAST_ACTIVE.put(entityById.getId(), now());
                        entitySignal(entityById, SimulationSignals.Kind.EQUIP, 0);
                    }
                }
            }
        }
    }

    public static void onDamage(EntityDamageS2CPacket entityDamagePacket) {
        if (active) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientWorld world = client.world;
            ClientPlayerEntity player = client.player;
            if (world != null && player != null) {
                RegistryEntry entry = entityDamagePacket.sourceType();
                if (!entry.matchesKey(DamageTypes.SWEET_BERRY_BUSH)
                        && !entry.matchesKey(DamageTypes.STALAGMITE)
                        && !entry.matchesKey(DamageTypes.FALLING_STALACTITE)) {
                    Entity entityById = world.getEntityById(entityDamagePacket.entityId());
                    if (entityById != null) {
                        int sourceCauseId = entityDamagePacket.sourceCauseId();
                        if (entry.matchesKey(DamageTypes.PLAYER_ATTACK)
                                && sourceCauseId >= 0
                                && sourceCauseId != player.getId()
                                && world.getEntityById(sourceCauseId) == null) {
                            pin(
                                    entityById.getBlockPos(),
                                    "Hidden Attacker hit " + name(entityById.getType()));
                        }
                        LAST_ACTIVE.put(entityById.getId(), now());
                        entitySignal(entityById, SimulationSignals.Kind.HURT, 0);
                    }
                }
            }
        }
    }

    public static void onTakeItem(ItemPickupAnimationS2CPacket itemPickupAnimationPacket) {
        if (active) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientWorld world = client.world;
            ClientPlayerEntity player = client.player;
            if (world != null && player != null) {
                int collectorEntityId = itemPickupAnimationPacket.getCollectorEntityId();
                if (collectorEntityId != player.getId()) {
                    Entity entityById =
                            world.getEntityById(itemPickupAnimationPacket.getEntityId());
                    if (entityById != null) {
                        Entity entityById2 = world.getEntityById(collectorEntityId);
                        if (entityById2 == null) {
                            String path;
                            if (entityById instanceof ItemEntity itemEntity) {
                                path =
                                        Registries.ITEM
                                                .getId(itemEntity.getStack().getItem())
                                                .getPath();
                            } else {
                                path = name(entityById.getType());
                            }
                            pin(entityById.getBlockPos(), "Hidden Pickup " + path);
                        } else if (!(entityById2 instanceof PlayerEntity)) {
                            LAST_ACTIVE.put(entityById2.getId(), now());
                            entitySignal(entityById2, SimulationSignals.Kind.EVENT, 0);
                        }
                    }
                }
            }
        }
    }

    public static void onAddEntity(EntitySpawnS2CPacket entitySpawnPacket) {
        if (active) {
            long now = now();
            ENTITY_ADDED.put(entitySpawnPacket.getEntityId(), now);
            LAST_ACTIVE.put(entitySpawnPacket.getEntityId(), now);
            EntityType entityType = entitySpawnPacket.getEntityType();
            if (entityType != EntityType.PLAYER) {
                BlockPos pos2 =
                        BlockPos.ofFloored(
                                entitySpawnPacket.getX(),
                                entitySpawnPacket.getY(),
                                entitySpawnPacket.getZ());
                if (entityType == EntityType.LIGHTNING_BOLT) {
                    LIGHTNING_PENDING.put(
                            entitySpawnPacket.getEntityId(), new long[] {pos2.asLong(), now});
                } else if (spawnable(entityType) && !isQuiet()) {
                    if (wellInsideTracking(
                            entitySpawnPacket.getX(), entitySpawnPacket.getZ(), entityType)) {
                        if (settled(pos2)) {
                            int entityId = entitySpawnPacket.getEntityId();
                            signal(
                                    pos2,
                                    SimulationSignals.Kind.SPAWN,
                                    spill(entityType),
                                    () -> entityShare(entityId),
                                    () -> name(entityType));
                        }
                    }
                }
            }
        }
    }

    public static void onRemoveEntities(IntList intList) {
        if (active) {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                long now = now();
                boolean quiet = isQuiet();
                for (int i = 0; i < intList.size(); i++) {
                    int j = intList.getInt(i);
                    long[] longs = LIGHTNING_PENDING.remove(j);
                    if (longs != null) {
                        if (!quiet && now - longs[1] < 3000000000L) {
                            signal(
                                    BlockPos.fromLong(longs[0]),
                                    SimulationSignals.Kind.LIGHTNING,
                                    0,
                                    () -> entityShare(j),
                                    () -> "lightning_bolt");
                        }
                    } else {
                        Entity entityById;
                        if (!quiet
                                && (entityById = world.getEntityById(j)) != null
                                && !(entityById instanceof PlayerEntity)
                                && now - ENTITY_ADDED.getOrDefault(j, -4611686018427387904L)
                                        >= 2000000000L
                                && spawnable(entityById.getType())
                                && wellInsideTracking(
                                        entityById.getX(), entityById.getZ(), entityById.getType())
                                && settled(entityById.getBlockPos())) {
                            entitySignal(
                                    entityById,
                                    SimulationSignals.Kind.REMOVE,
                                    spill(entityById.getType()));
                        }
                    }
                }
            }
        }
    }

    public static void onBlockUpdate(BlockUpdateS2CPacket blockUpdatePacket) {
        if (active) {
            block(blockUpdatePacket.getPos(), blockUpdatePacket.getState());
        }
    }

    public static void onSectionUpdate(ChunkDeltaUpdateS2CPacket chunkDeltaUpdatePacket) {
        if (active) {
            chunkDeltaUpdatePacket.visitUpdates(SimulationSignals::block);
        }
    }

    private static void block(BlockPos tled, BlockState state) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != null) {
            BlockState blockState = world.getBlockState(tled);
            if (blockState != state) {
                if (!ignored(blockState) && !ignored(state)) {
                    if (settled(tled)) {
                        SimulationSignals.Kind kind =
                                ticked(blockState, state)
                                        ? SimulationSignals.Kind.TICK
                                        : SimulationSignals.Kind.BLOCK;
                        signal(
                                tled,
                                kind,
                                1,
                                () -> blockShare(tled),
                                () -> describe(blockState) + " -> " + describe(state));
                    }
                }
            }
        }
    }

    static boolean ticked(BlockState state, BlockState state2) {
        Block block = state.getBlock();
        Block block3;
        if (block == (block3 = state2.getBlock())) {
            return true;
        } else if (!state.getFluidState().isEmpty() || !state2.getFluidState().isEmpty()) {
            return true;
        } else if (state.isIn(BlockTags.LEAVES) && state2.isAir()) {
            return true;
        } else if (state.isAir()
                && state2.isIn(BlockTags.LEAVES)
                && !state2.get(Properties.PERSISTENT, true)) {
            return true;
        } else if (!state.isIn(BlockTags.SAPLINGS)
                || !state2.isIn(BlockTags.LOGS) && !state2.isIn(BlockTags.LEAVES)) {
            if (state.isIn(BlockTags.CROPS) && state2.isAir()) {
                return true;
            } else if (block != Blocks.DIRT
                    || block3 != Blocks.GRASS_BLOCK && block3 != Blocks.MYCELIUM) {
                if (block == Blocks.FARMLAND && block3 == Blocks.DIRT) {
                    return true;
                } else if (block == Blocks.MELON_STEM && block3 == Blocks.ATTACHED_MELON_STEM) {
                    return true;
                } else if (block == Blocks.PUMPKIN_STEM && block3 == Blocks.ATTACHED_PUMPKIN_STEM) {
                    return true;
                } else if (state.isAir() && GROWS_FROM_AIR.contains(block3)) {
                    return true;
                } else {
                    return state2.isAir() && GONE_TO_AIR.contains(block)
                            ? true
                            : Oxidizable.getIncreasedOxidationBlock(block)
                                    .map(block2 -> block2 == block3)
                                    .orElse(false);
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    private static boolean ignored(BlockState state) {
        return state.isOf(Blocks.POINTED_DRIPSTONE)
                || state.isOf(Blocks.CAVE_VINES)
                || state.isOf(Blocks.CAVE_VINES_PLANT)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.BEEHIVE)
                || state.isOf(Blocks.BEE_NEST)
                || state.isOf(Blocks.VINE)
                || state.isOf(Blocks.WEEPING_VINES)
                || state.isOf(Blocks.WEEPING_VINES_PLANT)
                || state.isOf(Blocks.TWISTING_VINES)
                || state.isOf(Blocks.TWISTING_VINES_PLANT);
    }

    public static void tick() {
        if (active) {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (client.world != null && player != null) {
                long now = now();
                if (player.getY() < 0.0) {
                    undergroundUntil = now + 3000000000L;
                }
                double d = player.getX();
                double e = player.getZ();
                if (hasLast && square(d - lastX, e - lastZ) > 64.0) {
                    startQuiet();
                }
                lastX = d;
                lastZ = e;
                hasLast = true;
                refreshPassed(player);
                if (++ticks % 10 == 0) {
                    int j = player.getBlockPos().getX() >> 4;
                    int k = player.getBlockPos().getZ() >> 4;
                    int ringStart = ringStart();
                    boolean bl =
                            !isQuiet() && (!hasSample || square(d - sampleX, e - sampleZ) <= 900.0);
                    sampleX = d;
                    sampleZ = e;
                    hasSample = true;
                    Int2DoubleOpenHashMap int2DoubleOpenHashMap = new Int2DoubleOpenHashMap();
                    int view = view();
                    FROZEN.clear();
                    for (Entity entity : client.world.getEntities()) {
                        if (!(entity instanceof PlayerEntity)) {
                            long l = LAST_ACTIVE.get(entity.getId());
                            if (l == 0L) {
                                LAST_ACTIVE.put(entity.getId(), now);
                            } else {
                                int m;
                                int n;
                                int distance;
                                if (now - l > 40000000000L
                                        && witness(entity)
                                        && (distance =
                                                        distance(
                                                                n =
                                                                        entity.getBlockPos().getX()
                                                                                >> 4,
                                                                m =
                                                                        entity.getBlockPos().getZ()
                                                                                >> 4,
                                                                j,
                                                                k))
                                                >= ringStart
                                        && distance <= view
                                        && !nearVisiblePlayer(n, m, player, 0)) {
                                    FROZEN.add(ChunkPos.toLong(n, m));
                                }
                            }
                            if (bl
                                    && spawnable(entity.getType())
                                    && !entity.hasPassengers()
                                    && !entity.hasVehicle()) {
                                int maxTrackDistance = entity.getType().getMaxTrackDistance();
                                double d2 = Math.hypot(entity.getX() - d, entity.getZ() - e);
                                if (d2 > int2DoubleOpenHashMap.get(maxTrackDistance)) {
                                    int2DoubleOpenHashMap.put(maxTrackDistance, d2);
                                }
                            }
                            if (distance(
                                            entity.getBlockPos().getX() >> 4,
                                            entity.getBlockPos().getZ() >> 4,
                                            j,
                                            k)
                                    < ringStart) {
                                ENTITY_INSIDE.put(entity.getId(), now);
                            }
                        }
                    }
                    if (bl) {
                        for (int o : TRACKED_RANGE.keySet().toIntArray()) {
                            TRACKED_RANGE.put(
                                    o,
                                    Math.max(
                                            int2DoubleOpenHashMap.get(o),
                                            TRACKED_RANGE.get(o) - 0.5));
                        }
                        for (Entry int2DoubleEntry : int2DoubleOpenHashMap.int2DoubleEntrySet()) {
                            if (int2DoubleEntry.getDoubleValue()
                                    > TRACKED_RANGE.get(int2DoubleEntry.getIntKey())) {
                                TRACKED_RANGE.put(
                                        int2DoubleEntry.getIntKey(),
                                        int2DoubleEntry.getDoubleValue());
                            }
                        }
                    }
                    if (ticks % 200 == 0) {
                        ObjectIterator objectIterator = CHUNKS.long2ObjectEntrySet().iterator();
                        while (objectIterator.hasNext()) {
                            if (((SimulationSignals.ChunkSignal)
                                                    ((it.unimi.dsi.fastutil.longs.Long2ObjectMap
                                                                            .Entry)
                                                                    objectIterator.next())
                                                            .getValue())
                                            .score(now)
                                    < 0.05F) {
                                objectIterator.remove();
                            }
                        }
                        ENTITY_SHARES.values().removeIf(share -> share.amount(now) < 0.05F);
                        BLOCK_SHARES.values().removeIf(share -> share.amount(now) < 0.05F);
                        int p = view() + 4;
                        CHUNK_LOADED
                                .long2LongEntrySet()
                                .removeIf(
                                        entry ->
                                                distance(
                                                                ChunkPos.getPackedX(
                                                                        entry.getLongKey()),
                                                                ChunkPos.getPackedZ(
                                                                        entry.getLongKey()),
                                                                j,
                                                                k)
                                                        > p);
                        ENTITY_ADDED
                                .int2LongEntrySet()
                                .removeIf(entry -> now - entry.getLongValue() > 60000000000L);
                        ENTITY_INSIDE
                                .int2LongEntrySet()
                                .removeIf(entry -> now - entry.getLongValue() > 60000000000L);
                        LIGHTNING_PENDING.values().removeIf(ls -> now - ls[1] > 3000000000L);
                        LAST_ACTIVE.keySet().removeIf(i -> client.world.getEntityById(i) == null);
                    }
                }
            }
        }
    }

    private static boolean witness(Entity entity) {
        PathAwareEntity pathAwareEntity;
        return entity instanceof PathAwareEntity
                && !((pathAwareEntity = (PathAwareEntity) entity) instanceof ShulkerEntity)
                && !pathAwareEntity.isAiDisabled()
                && !pathAwareEntity.hasVehicle()
                && !pathAwareEntity.hasPassengers()
                && !pathAwareEntity.isSleeping();
    }

    private static void pin(BlockPos pos2, String string) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && !holding() && pos2.getY() >= 0) {
            if (PINS.size() >= 32) {
                PINS.poll();
            }
            PINS.add(
                    new SimulationSignals.Pin(
                            pos2.toImmutable(),
                            string + " @ " + pos2.getX() + " " + pos2.getY() + " " + pos2.getZ()));
        }
    }

    private static void refreshPassed(ClientPlayerEntity player) {
        int i = player.getBlockPos().getX() >> 4;
        int j = player.getBlockPos().getZ() >> 4;
        long l = ChunkPos.toLong(i, j);
        int k = ringStart() - 1 + 1;
        if (l != passedCenter || k != passedRadius) {
            passedCenter = l;
            passedRadius = k;
            for (int m = -k; m <= k; m++) {
                for (int n = -k; n <= k; n++) {
                    long l2 = ChunkPos.toLong(i + m, j + n);
                    if (passed.add(l2)) {
                        CHUNKS.remove(l2);
                    }
                }
            }
        }
    }

    private static void entitySignal(Entity entity, SimulationSignals.Kind kind, int i) {
        if (!(entity instanceof PlayerEntity)) {
            if (!(entity.getFirstPassenger() instanceof PlayerEntity)) {
                FallingBlockEntity fallingBlockEntity;
                if (!(entity instanceof FallingBlockEntity)
                        || !(fallingBlockEntity = (FallingBlockEntity) entity)
                                .getBlockState()
                                .isOf(Blocks.POINTED_DRIPSTONE)) {
                    ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    if (player != null) {
                        Tameable tameable;
                        LazyEntityReference ownerReference;
                        if (!(entity instanceof Tameable)
                                || (ownerReference =
                                                (tameable = (Tameable) entity).getOwnerReference())
                                        == null
                                || !player.getUuid().equals(ownerReference.getUuid())) {
                            if (now()
                                            - ENTITY_INSIDE.getOrDefault(
                                                    entity.getId(), -4611686018427387904L)
                                    >= 60000000000L) {
                                signal(
                                        entity.getBlockPos(),
                                        kind,
                                        i,
                                        () -> entityShare(entity.getId()),
                                        () -> name(entity.getType()));
                            }
                        }
                    }
                }
            }
        }
    }

    private static SimulationSignals.Share entityShare(int i) {
        return ENTITY_SHARES.computeIfAbsent(i, ix -> new SimulationSignals.Share());
    }

    private static SimulationSignals.Share blockShare(BlockPos pos2) {
        return BLOCK_SHARES.computeIfAbsent(pos2.asLong(), value -> new SimulationSignals.Share());
    }

    private static String name(EntityType<?> entityType) {
        return Registries.ENTITY_TYPE.getId(entityType).getPath();
    }

    private static String describe(BlockState state) {
        return state.toString().replace("Block{minecraft:", "").replace("}", "");
    }

    private static void signal(
            BlockPos pos2,
            SimulationSignals.Kind kind,
            int i,
            Supplier<SimulationSignals.Share> supplier,
            Supplier<String> supplier2) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            if (pos2.getY() >= 0 && !holding()) {
                int j = pos2.getX() >> 4;
                int k;
                int distance =
                        distance(
                                j,
                                k = pos2.getZ() >> 4,
                                player.getBlockPos().getX() >> 4,
                                player.getBlockPos().getZ() >> 4);
                if (distance >= ringStart() + i && distance <= view()) {
                    long l = ChunkPos.toLong(j, k);
                    if (!passed.contains(l)) {
                        if (!ignoreVisiblePlayers || !nearVisiblePlayer(j, k, player, i)) {
                            long now = now();
                            float take =
                                    ((SimulationSignals.Share) supplier.get())
                                            .take(kind.weight, kind.cap, now);
                            if (!(take <= 0.0F)) {
                                String string =
                                        kind.title
                                                + " "
                                                + (String) supplier2.get()
                                                + " @ "
                                                + pos2.getX()
                                                + " "
                                                + pos2.getY()
                                                + " "
                                                + pos2.getZ();
                                CHUNKS.computeIfAbsent(
                                                l, value -> new SimulationSignals.ChunkSignal())
                                        .add(kind, now, take, string);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean nearVisiblePlayer(int i, int j, ClientPlayerEntity player, int k) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return false;
        } else {
            int m = simulation() + 1 + k;
            for (PlayerEntity player2 : world.getPlayers()) {
                if (player2 != player
                        && distance(
                                        i,
                                        j,
                                        player2.getBlockPos().getX() >> 4,
                                        player2.getBlockPos().getZ() >> 4)
                                <= m) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean settled(BlockPos pos2) {
        long l =
                CHUNK_LOADED.getOrDefault(
                        ChunkPos.toLong(pos2.getX() >> 4, pos2.getZ() >> 4), -9223372036854775808L);
        return l == -9223372036854775808L || now() - l > 3000000000L;
    }

    private static boolean wellInsideTracking(double d, double e, EntityType<?> entityType) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        } else {
            double d2 =
                    Math.min(TRACKED_RANGE.get(entityType.getMaxTrackDistance()), view() * 16.0);
            return Math.hypot(d - player.getX(), e - player.getZ()) < d2 - 24.0;
        }
    }

    private static boolean spawnable(EntityType<?> entityType) {
        return entityType.getSpawnGroup() == SpawnGroup.MONSTER
                || entityType == EntityType.ITEM
                || entityType == EntityType.EXPERIENCE_ORB;
    }

    private static int spill(EntityType<?> entityType) {
        return entityType != EntityType.ITEM && entityType != EntityType.EXPERIENCE_ORB ? 0 : 1;
    }

    private static void startQuiet() {
        quietUntil = now() + 2000000000L;
    }

    private static boolean isQuiet() {
        return now() < quietUntil;
    }

    private static double square(double d, double e) {
        return d * d + e * e;
    }

    public static int distance(int i, int j, int k, int m) {
        return Math.max(Math.abs(i - k), Math.abs(j - m));
    }

    @Environment(EnvType.CLIENT)
    public static final class ChunkSignal {

        private float score;

        private long updated;

        private int kinds;

        private final int[] counts = new int[SimulationSignals.Kind.values().length];

        private final ArrayDeque<String> recent = new ArrayDeque<>();

        public ChunkSignal() {}

        public float score(long l) {
            return this.score * SimulationSignals.decay(l - this.updated);
        }

        public boolean has(SimulationSignals.Kind kind) {
            return (this.kinds & 1 << kind.ordinal()) != 0;
        }

        public int count(SimulationSignals.Kind kind) {
            return this.counts[kind.ordinal()];
        }

        public List<String> recent() {
            return List.copyOf(this.recent);
        }

        void add(SimulationSignals.Kind kind, long updated2, float f, String string) {
            this.recent.addLast(string);
            if (this.recent.size() > 6) {
                this.recent.removeFirst();
            }
            this.score = this.score(updated2) + f;
            this.updated = updated2;
            this.kinds = this.kinds | 1 << kind.ordinal();
            int ordinal = kind.ordinal();
            this.counts[ordinal]++;
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum Kind {
        MOVE("Mob Move", 2.5F, true),
        HEAD("Head Turn", 2.5F, true),
        SPAWN("Spawn / Drop", 2.0F, false),
        REMOVE("Despawn / Pickup", 1.5F, false),
        STATE("Entity State", 0.5F, false),
        EVENT("Entity Event", 5.0F, true),
        BLOCK("Block Change", 1.6F, false),
        TICK("Tick Change", 10.0F, true),
        LIGHTNING("Lightning", 10.0F, true),
        HURT("Hurt", 5.0F, true),
        EQUIP("Equipment", 5.0F, true);

        public final String title;

        final float weight;

        final float cap;

        private Kind(String title2, float weight2, boolean bl) {
            this.title = title2;
            this.weight = weight2;
            this.cap = bl ? 10.0F : 4.0F;
        }
    }

    @Environment(EnvType.CLIENT)
    public record Pin(BlockPos pos, String detail) {}

    @Environment(EnvType.CLIENT)
    private static final class Share {

        private float amount;

        private long updated;

        private Share() {}

        float amount(long l) {
            return this.amount * SimulationSignals.decay(l - this.updated);
        }

        float take(float f, float g, long updated2) {
            float amount2 = this.amount(updated2);
            float h = Math.min(f, Math.max(g, f) - amount2);
            if (h <= 0.0F) {
                return 0.0F;
            } else {
                this.amount = amount2 + h;
                this.updated = updated2;
                return h;
            }
        }
    }
}
