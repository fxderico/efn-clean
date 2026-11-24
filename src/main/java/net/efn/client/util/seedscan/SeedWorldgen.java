/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.util.seedscan;

import com.mojang.serialization.Lifecycle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.processor.StructureProcessorLists;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.BuiltinBiomes;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.PaletteProvider;
import net.minecraft.world.chunk.PalettesFactory;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.carver.CarverContext;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.carver.ConfiguredCarvers;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.densityfunction.DensityFunctions;
import net.minecraft.world.gen.feature.ConfiguredFeatures;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.PlacedFeatures;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer;
import net.minecraft.world.gen.noise.BuiltinNoiseParameters;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Environment(value = EnvType.CLIENT)
public final class SeedWorldgen {

    public static final int MIN_Y = -64;

    public static final int HEIGHT = 384;

    public static final int MAX_Y = 319;

    public final long seed;

    final NoiseChunkGenerator generator;

    final NoiseConfig randomState;

    final MultiNoiseBiomeSource biomeSource;

    final ChunkGeneratorSettings settings;

    private final List<OreSpec> specs;

    private final PalettesFactory containers;

    private final StructureAccessor noStructures;

    private final HeightLimitView height = HeightLimitView.create(-64, 384);

    private final long zoomSeed;

    private final Map<Long, GenerationSettings> carverBiomes =
            new ConcurrentHashMap<Long, GenerationSettings>();

    private final Map<Long, short[]> storedBiomes = new ConcurrentHashMap<Long, short[]>();

    private final List<RegistryEntry<Biome>> biomeIds;

    private final Map<RegistryEntry<Biome>, Integer> biomeIndex =
            new IdentityHashMap<RegistryEntry<Biome>, Integer>();

    private static final int MIN_QUART = -16;

    private static final int QUARTS_Y = 96;

    private static final int MAX_STORED_BIOMES = 4096;

    private static final int MAX_CARVER_BIOMES = 65536;

    private final BiomeAccess biomeManager;

    private static final DynamicRegistryManager CARVER_REGISTRIES =
            new DynamicRegistryManager.ImmutableImpl(
                    List.of(
                            new SimpleRegistry<Biome>(RegistryKeys.BIOME, Lifecycle.stable())
                                    .freeze()));

    private static final Set<RegistryKey<Biome>> TALL_TREES =
            Set.of(
                    BiomeKeys.JUNGLE,
                    BiomeKeys.BAMBOO_JUNGLE,
                    BiomeKeys.SPARSE_JUNGLE,
                    BiomeKeys.OLD_GROWTH_PINE_TAIGA,
                    BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA,
                    BiomeKeys.MANGROVE_SWAMP);

    private SeedWorldgen(long seed2) {
        this.seed = seed2;
        RegistryWrapper.WrapperLookup wrapperLookup =
                new RegistryBuilder()
                        .addRegistry(RegistryKeys.CONFIGURED_CARVER, ConfiguredCarvers::bootstrap)
                        .addRegistry(
                                RegistryKeys.PROCESSOR_LIST, StructureProcessorLists::bootstrap)
                        .addRegistry(RegistryKeys.CONFIGURED_FEATURE, ConfiguredFeatures::bootstrap)
                        .addRegistry(RegistryKeys.PLACED_FEATURE, PlacedFeatures::bootstrap)
                        .addRegistry(RegistryKeys.BIOME, BuiltinBiomes::bootstrap)
                        .addRegistry(
                                RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST,
                                MultiNoiseBiomeSourceParameterLists::bootstrap)
                        .addRegistry(
                                RegistryKeys.NOISE_PARAMETERS, BuiltinNoiseParameters::bootstrap)
                        .addRegistry(RegistryKeys.DENSITY_FUNCTION, DensityFunctions::bootstrap)
                        .addRegistry(
                                RegistryKeys.CHUNK_GENERATOR_SETTINGS,
                                ChunkGeneratorSettings::bootstrap)
                        .createWrapperLookup(DynamicRegistryManager.of(Registries.REGISTRIES));
        RegistryEntry.Reference<ChunkGeneratorSettings> orThrow =
                wrapperLookup
                        .getOrThrow(RegistryKeys.CHUNK_GENERATOR_SETTINGS)
                        .getOrThrow(ChunkGeneratorSettings.OVERWORLD);
        this.settings = orThrow.value();
        this.randomState =
                NoiseConfig.create(
                        this.settings,
                        wrapperLookup.getOrThrow(RegistryKeys.NOISE_PARAMETERS),
                        seed2);
        RegistryEntry.Reference<MultiNoiseBiomeSourceParameterList> orThrow2 =
                wrapperLookup
                        .getOrThrow(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                        .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
        this.biomeSource = MultiNoiseBiomeSource.create(orThrow2);
        this.generator = new NoiseChunkGenerator((BiomeSource) this.biomeSource, orThrow);
        List<RegistryEntry<Biome>> list = List.copyOf(this.biomeSource.getBiomes());
        List<PlacedFeatureIndexer.IndexedFeatures> indexedFeatures =
                PlacedFeatureIndexer.collectIndexedFeatures(
                        list,
                        registryEntry ->
                                this.generator
                                        .getGenerationSettings((RegistryEntry<Biome>) registryEntry)
                                        .getFeatures(),
                        true);
        IdentityHashMap map = new IdentityHashMap();
        wrapperLookup
                .getOrThrow(RegistryKeys.PLACED_FEATURE)
                .streamEntries()
                .forEach(
                        reference ->
                                map.put(
                                        (PlacedFeature) reference.value(),
                                        reference.registryKey().getValue().getPath()));
        this.specs =
                OreSpec.derive(
                        indexedFeatures,
                        list,
                        this.generator,
                        placedFeature -> (String) map.getOrDefault(placedFeature, "?"),
                        -64,
                        319);
        RegistryEntry.Reference<Biome> orThrow3 =
                wrapperLookup.getOrThrow(RegistryKeys.BIOME).getOrThrow(BiomeKeys.PLAINS);
        ArrayList<RegistryEntry<Biome>> list2 = new ArrayList<RegistryEntry<Biome>>(list);
        if (!list2.contains(orThrow3)) {
            list2.add(orThrow3);
        }
        this.biomeIds = List.copyOf(list2);
        for (int i = 0; i < this.biomeIds.size(); ++i) {
            this.biomeIndex.put(this.biomeIds.get(i), i);
        }
        this.containers =
                new PalettesFactory(
                        PaletteProvider.forBlockStates(Block.STATE_IDS),
                        Blocks.AIR.getDefaultState(),
                        null,
                        PaletteProvider.forBiomes(SeedWorldgen.listIds(list2)),
                        orThrow3,
                        null);
        this.noStructures =
                new StructureAccessor(
                        SeedWorldgen.emptyLevel(this.containers, this.height),
                        new GeneratorOptions(seed2, false, false),
                        null);
        this.zoomSeed = BiomeAccess.hashSeed(seed2);
        this.biomeManager = new BiomeAccess(this::storedBiome, this.zoomSeed);
    }

    public static SeedWorldgen create(long l) {
        return new SeedWorldgen(l);
    }

    public List<OreSpec> specs() {
        return this.specs;
    }

    public int treeAllowance(int i, int j) {
        for (int k = -1; k <= 1; ++k) {
            for (int i2 = -1; i2 <= 1; ++i2) {
                int m = (i + k << 4) + 8;
                int n = (j + i2 << 4) + 8;
                for (int i3 = 60; i3 <= 130; i3 += 10) {
                    if (!this.biomeAt(m, i3, n).getKey().filter(TALL_TREES::contains).isPresent())
                        continue;
                    return GateHeights.TALL_TREE_ALLOWANCE;
                }
            }
        }
        return GateHeights.TREE_ALLOWANCE;
    }

    public RegistryEntry<Biome> biomeAt(int i, int j, int k) {
        return this.biomeManager.getBiome(new BlockPos(i, j, k));
    }

    RegistryEntry<Biome> storedBiome(int i, int j, int k) {
        int m = Math.max(-16, Math.min(j, 79));
        short[] shorts = this.storedBiomes(BiomeCoords.toChunk(i), BiomeCoords.toChunk(k));
        return this.biomeIds.get(shorts[((m - -16) * 4 + (k & 3)) * 4 + (i & 3)]);
    }

    private short[] storedBiomes(int i, int j) {
        long l = ChunkPos.toLong(i, j);
        short[] storedBiome = this.storedBiomes.get(l);
        if (storedBiome != null) {
            return storedBiome;
        }
        ProtoChunk protoChunk =
                new ProtoChunk(
                        new ChunkPos(i, j),
                        UpgradeData.NO_UPGRADE_DATA,
                        this.height,
                        this.containers,
                        null);
        return this.fillBiomes(protoChunk);
    }

    private short[] fillBiomes(ProtoChunk protoChunk) {
        this.generator
                .populateBiomes(
                        this.randomState,
                        Blender.getNoBlending(),
                        this.noStructures,
                        (Chunk) protoChunk)
                .join();
        protoChunk.setStatus(ChunkStatus.BIOMES);
        int fromBlock = BiomeCoords.fromBlock(protoChunk.getPos().getStartX());
        int fromBlock2 = BiomeCoords.fromBlock(protoChunk.getPos().getStartZ());
        short[] shorts = new short[1536];
        for (int i = 0; i < 96; ++i) {
            for (int j = 0; j < 4; ++j) {
                for (int k = 0; k < 4; ++k) {
                    Integer integer =
                            this.biomeIndex.get(
                                    protoChunk.getBiomeForNoiseGen(
                                            fromBlock + k, -16 + i, fromBlock2 + j));
                    shorts[(i * 4 + j) * 4 + k] = (short) (integer == null ? 0 : integer);
                }
            }
        }
        if (this.storedBiomes.size() > 4096) {
            this.storedBiomes.clear();
        }
        this.storedBiomes.put(protoChunk.getPos().toLong(), shorts);
        return shorts;
    }

    public ProtoChunk generateTerrain(int i, int j) {
        ProtoChunk protoChunk =
                new ProtoChunk(
                        new ChunkPos(i, j),
                        UpgradeData.NO_UPGRADE_DATA,
                        this.height,
                        this.containers,
                        null);
        this.fillBiomes(protoChunk);
        this.generator
                .populateNoise(
                        Blender.getNoBlending(), this.randomState, this.noStructures, protoChunk)
                .join();
        protoChunk.setStatus(ChunkStatus.NOISE);
        this.generator.buildSurface(
                protoChunk,
                new HeightContext(this.generator, protoChunk),
                this.randomState,
                this.noStructures,
                this.biomeManager,
                null,
                Blender.getNoBlending());
        protoChunk.setStatus(ChunkStatus.SURFACE);
        this.carve(protoChunk);
        protoChunk.setStatus(ChunkStatus.CARVERS);
        return protoChunk;
    }

    public TerrainChunk terrain(int i, int j) {
        return TerrainChunk.of(this.generateTerrain(i, j));
    }

    void carve(ProtoChunk protoChunk) {
        if (this.carverBiomes.size() > 65536) {
            this.carverBiomes.clear();
        }
        BiomeAccess biomeAccess =
                new BiomeAccess(
                        (i, j, k) ->
                                this.biomeSource.getBiome(
                                        i, j, k, this.randomState.getMultiNoiseSampler()),
                        this.zoomSeed);
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
        ChunkPos chunkPos = protoChunk.getPos();
        ChunkNoiseSampler orCreateChunkNoiseSampler =
                protoChunk.getOrCreateChunkNoiseSampler(
                        chunk -> {
                            throw new IllegalStateException("carving before the noise fill");
                        });
        AquiferSampler aquiferSampler = orCreateChunkNoiseSampler.getAquiferSampler();
        CarverContext carverContext =
                new CarverContext(
                        this.generator,
                        CARVER_REGISTRIES,
                        protoChunk.getHeightLimitView(),
                        orCreateChunkNoiseSampler,
                        this.randomState,
                        this.settings.surfaceRule());
        CarvingMask orCreateCarvingMask = protoChunk.getOrCreateCarvingMask();
        for (int i2 = -8; i2 <= 8; ++i2) {
            for (int i3 = -8; i3 <= 8; ++i3) {
                ChunkPos chunkPos2 = new ChunkPos(chunkPos.x + i2, chunkPos.z + i3);
                GenerationSettings generationSettings =
                        this.carverBiomes.computeIfAbsent(
                                chunkPos2.toLong(),
                                long_ ->
                                        this.generator.getGenerationSettings(
                                                this.biomeSource.getBiome(
                                                        BiomeCoords.fromBlock(
                                                                chunkPos2.getStartX()),
                                                        0,
                                                        BiomeCoords.fromBlock(
                                                                chunkPos2.getStartZ()),
                                                        this.randomState.getMultiNoiseSampler())));
                int m = 0;
                for (RegistryEntry<ConfiguredCarver<?>> entry :
                        generationSettings.getCarversForStep()) {
                    chunkRandom.setCarverSeed(this.seed + (long) m, chunkPos2.x, chunkPos2.z);
                    if (entry.value().shouldCarve(chunkRandom)) {
                        entry.value()
                                .carve(
                                        carverContext,
                                        protoChunk,
                                        biomeAccess::getBiome,
                                        chunkRandom,
                                        aquiferSampler,
                                        chunkPos2,
                                        orCreateCarvingMask);
                    }
                    ++m;
                }
            }
        }
    }

    private static IndexedIterable<RegistryEntry<Biome>> listIds(
            final List<RegistryEntry<Biome>> list) {
        final IdentityHashMap<RegistryEntry<Biome>, Integer> map =
                new IdentityHashMap<RegistryEntry<Biome>, Integer>();
        for (int i = 0; i < list.size(); ++i) {
            map.put(list.get(i), i);
        }
        return new IndexedIterable<RegistryEntry<Biome>>() {

            public int getRawId(RegistryEntry<Biome> entry) {
                Integer integer = (Integer) map.get(entry);
                return integer == null ? -1 : integer;
            }

            public RegistryEntry<Biome> get(int j) {
                return j >= 0 && j < list.size() ? (RegistryEntry) list.get(j) : null;
            }

            @Override
            public int size() {
                return list.size();
            }

            @Override
            public Iterator<RegistryEntry<Biome>> iterator() {
                return list.iterator();
            }
        };
    }

    private static WorldAccess emptyLevel(
            PalettesFactory palettesFactory, HeightLimitView heightLimitView) {
        ProtoChunk protoChunk =
                new ProtoChunk(
                        new ChunkPos(0, 0),
                        UpgradeData.NO_UPGRADE_DATA,
                        heightLimitView,
                        palettesFactory,
                        null);
        InvocationHandler invocationHandler =
                (value, method, objects) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "equals" -> value == objects[0];
                            case "hashCode" -> System.identityHashCode(value);
                            default -> "SeedWorldgen.emptyLevel";
                        };
                    }
                    if (Chunk.class.isAssignableFrom(method.getReturnType())
                            && method.getParameterCount() == 4) {
                        return protoChunk;
                    }
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(value, method, objects);
                    }
                    throw new UnsupportedOperationException(
                            "structure-free level: " + String.valueOf(method));
                };
        return (WorldAccess)
                Proxy.newProxyInstance(
                        WorldAccess.class.getClassLoader(),
                        new Class[] {WorldAccess.class},
                        invocationHandler);
    }
}
