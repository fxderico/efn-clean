package net.efn.client.util.seedscan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep.Feature;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig.Target;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.PlacedFeatureIndexer.IndexedFeatures;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public record OreSpec(
        String id,
        int step,
        int index,
        int order,
        int countMin,
        int countMax,
        int rarity,
        boolean trapezoid,
        int yMin,
        int yMax,
        int plateau,
        int size,
        float discard,
        byte kind,
        Set<RegistryEntry<Biome>> biomes) {

    private static final int[] STEPS =
            new int[] {
                Feature.UNDERGROUND_ORES.ordinal(), Feature.UNDERGROUND_DECORATION.ordinal()
            };

    public int reach() {
        return MathHelper.ceil((this.size / 16.0F * 2.0F + 1.0F) / 2.0F);
    }

    public int halfWidth() {
        return MathHelper.ceil(this.size / 8.0F) + this.reach();
    }

    public boolean drawsOnTerrain() {
        return this.discard > 0.0F && this.discard < 1.0F;
    }

    int sampleCount(WgRandom wgRandom) {
        if (this.rarity > 0) {
            return wgRandom.nextFloat() < 1.0F / this.rarity ? 1 : 0;
        } else {
            return this.countMin == this.countMax
                    ? this.countMin
                    : wgRandom.between(this.countMin, this.countMax);
        }
    }

    int sampleY(WgRandom wgRandom) {
        if (this.yMin > this.yMax) {
            return this.yMin;
        } else if (!this.trapezoid) {
            return wgRandom.between(this.yMin, this.yMax);
        } else {
            int i = this.yMax - this.yMin;
            if (this.plateau >= i) {
                return wgRandom.between(this.yMin, this.yMax);
            } else {
                int j = (i - this.plateau) / 2;
                int k = i - j;
                return this.yMin + wgRandom.between(0, k) + wgRandom.between(0, j);
            }
        }
    }

    boolean inBiome(RegistryEntry<Biome> entry) {
        return this.biomes == null || this.biomes.contains(entry);
    }

    @Override
    public String toString() {
        return this.id + "(" + this.step + ":" + this.index + ")";
    }

    static List<OreSpec> derive(
            List<IndexedFeatures> list,
            List<RegistryEntry<Biome>> list2,
            ChunkGenerator chunkGenerator,
            Function<PlacedFeature, String> function,
            int i,
            int j) {
        checkTags();
        ArrayList list3 = new ArrayList();
        for (int k : STEPS) {
            if (k < list.size()) {
                List list4 = ((IndexedFeatures) list.get(k)).features();
                for (int m = 0; m < list4.size(); m++) {
                    PlacedFeature placedFeature = (PlacedFeature) list4.get(m);
                    ConfiguredFeature feature = placedFeature.feature().value();
                    if (feature.feature() == net.minecraft.world.gen.feature.Feature.ORE) {
                        OreFeatureConfig oreFeatureConfig = (OreFeatureConfig) feature.config();
                        String string = (String) function.apply(placedFeature);
                        Set setFromMap = Collections.newSetFromMap(new IdentityHashMap());
                        for (RegistryEntry entry : list2) {
                            if (chunkGenerator
                                    .getGenerationSettings(entry)
                                    .isFeatureAllowed(placedFeature)) {
                                setFromMap.add(entry);
                            }
                        }
                        list3.add(
                                read(
                                        string,
                                        k,
                                        m,
                                        list3.size(),
                                        placedFeature,
                                        oreFeatureConfig,
                                        setFromMap.size() == list2.size()
                                                ? null
                                                : Set.copyOf(setFromMap),
                                        i,
                                        j));
                    }
                }
            }
        }
        return List.copyOf(list3);
    }

    private static OreSpec read(
            String string,
            int i,
            int j,
            int k,
            PlacedFeature placedFeature,
            OreFeatureConfig oreFeatureConfig,
            Set<RegistryEntry<Biome>> set,
            int m,
            int n) {
        List list = placedFeature.placementModifiers();
        if (list.size() != 4) {
            throw unsupported(string, "placement " + list.size() + " modifiers");
        } else {
            JsonObject json = json(string, (PlacementModifier) list.get(0));
            int o = 0;
            String string2 = type(json);
            int p;
            int q;
            switch (string2) {
                case "minecraft:count":
                    JsonElement json2 = json.get("count");
                    if (json2.isJsonPrimitive()) {
                        p = q = json2.getAsInt();
                    } else {
                        JsonObject jsonObject = json2.getAsJsonObject();
                        if (!type(jsonObject).equals("minecraft:uniform")) {
                            throw unsupported(string, "count " + jsonObject);
                        }
                        p = jsonObject.get("min_inclusive").getAsInt();
                        q = jsonObject.get("max_inclusive").getAsInt();
                    }
                    break;
                case "minecraft:rarity_filter":
                    q = 1;
                    p = 1;
                    o = json.get("chance").getAsInt();
                    break;
                default:
                    throw unsupported(string, "first modifier " + json);
            }
            if (!type(json(string, (PlacementModifier) list.get(1)))
                    .equals("minecraft:in_square")) {
                throw unsupported(string, "second modifier");
            } else {
                JsonObject json3 = json(string, (PlacementModifier) list.get(2));
                if (!type(json3).equals("minecraft:height_range")) {
                    throw unsupported(string, "third modifier");
                } else if (!type(json(string, (PlacementModifier) list.get(3)))
                        .equals("minecraft:biome")) {
                    throw unsupported(string, "fourth modifier");
                } else {
                    JsonObject jsonObject2 = json3.getAsJsonObject("height");
                    String string3 = type(jsonObject2);
                    boolean bl =
                            switch (string3) {
                                case "minecraft:uniform" -> false;
                                case "minecraft:trapezoid" -> true;
                                default -> throw unsupported(string, "height " + jsonObject2);
                            };
                    int r = jsonObject2.has("plateau") ? jsonObject2.get("plateau").getAsInt() : 0;
                    int anchor = anchor(string, jsonObject2.getAsJsonObject("min_inclusive"), m, n);
                    int anchor2 =
                            anchor(string, jsonObject2.getAsJsonObject("max_inclusive"), m, n);
                    return new OreSpec(
                            string,
                            i,
                            j,
                            k,
                            p,
                            q,
                            o,
                            bl,
                            anchor,
                            anchor2,
                            r,
                            oreFeatureConfig.size,
                            oreFeatureConfig.discardOnAirChance,
                            kindOf(string, oreFeatureConfig),
                            set);
                }
            }
        }
    }

    private static int anchor(String string, JsonObject json, int i, int j) {
        if (json.has("absolute")) {
            return json.get("absolute").getAsInt();
        } else if (json.has("above_bottom")) {
            return i + json.get("above_bottom").getAsInt();
        } else if (json.has("below_top")) {
            return j - json.get("below_top").getAsInt();
        } else {
            throw unsupported(string, "anchor " + json);
        }
    }

    private static byte kindOf(String string, OreFeatureConfig oreFeatureConfig) {
        HashSet set = new HashSet();
        byte b = -1;
        for (Target target : oreFeatureConfig.targets) {
            JsonObject jsonObject =
                    RuleTest.TYPE_CODEC
                            .encodeStart(JsonOps.INSTANCE, target.target)
                            .getOrThrow()
                            .getAsJsonObject();
            if (!jsonObject.get("predicate_type").getAsString().equals("minecraft:tag_match")) {
                throw unsupported(string, "target " + jsonObject);
            }
            set.add(jsonObject.get("tag").getAsString());
            byte b2 = VBlock.of(target.state);
            if (b2 == 12 || b2 == 0 || b != -1 && b != b2) {
                throw unsupported(string, "writes " + target.state);
            }
            b = b2;
        }
        boolean bl =
                set.equals(Set.of("minecraft:base_stone_overworld"))
                        || set.equals(
                                Set.of(
                                        "minecraft:stone_ore_replaceables",
                                        "minecraft:deepslate_ore_replaceables"));
        if (bl && b != -1) {
            return b;
        } else {
            throw unsupported(string, "targets " + set);
        }
    }

    static void checkTags() {
        expect(
                BlockTags.BASE_STONE_OVERWORLD,
                Blocks.STONE,
                Blocks.GRANITE,
                Blocks.DIORITE,
                Blocks.ANDESITE,
                Blocks.TUFF,
                Blocks.DEEPSLATE);
        expect(
                BlockTags.STONE_ORE_REPLACEABLES,
                Blocks.STONE,
                Blocks.GRANITE,
                Blocks.DIORITE,
                Blocks.ANDESITE);
        expect(BlockTags.DEEPSLATE_ORE_REPLACEABLES, Blocks.DEEPSLATE, Blocks.TUFF);
    }

    private static void expect(TagKey<Block> tagKey, Block... block) {
        Set setFromMap = Collections.newSetFromMap(new IdentityHashMap());
        Collections.addAll(setFromMap, block);
        Set setFromMap2 = Collections.newSetFromMap(new IdentityHashMap());
        for (RegistryEntry entry : Registries.BLOCK.iterateEntries(tagKey)) {
            setFromMap2.add((Block) entry.value());
        }
        if (!setFromMap2.equals(setFromMap)) {
            throw new IllegalStateException(
                    "block tag " + tagKey + " is not vanilla's: " + setFromMap2);
        }
    }

    private static JsonObject json(String string, PlacementModifier placementModifier) {
        try {
            return PlacementModifier.CODEC
                    .encodeStart(JsonOps.INSTANCE, placementModifier)
                    .getOrThrow()
                    .getAsJsonObject();
        } catch (RuntimeException exception) {
            throw unsupported(
                    string, "modifier " + placementModifier + ": " + exception.getMessage());
        }
    }

    private static String type(JsonObject json) {
        JsonElement json2 = json.get("type");
        return json2 == null ? "" : json2.getAsString();
    }

    private static IllegalStateException unsupported(String string, String string2) {
        return new IllegalStateException(
                "feature " + string + " is not placed like a vanilla ore: " + string2);
    }
}
