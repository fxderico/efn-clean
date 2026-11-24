package net.efn.client.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import imgui.type.ImBoolean;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class BlockColorListSetting extends Setting {

    static final String CHANGES = "changes";

    private static final String VANILLA = "minecraft:";

    private List<BlockColorListSetting.BlockEntry> entries;

    private Supplier<List<BlockColorListSetting.BlockEntry>> entriesSupplier;

    private final Supplier<List<BlockColorListSetting.BlockEntry>> shipped;

    private final float[] defaultColor;

    private final float[] shippedDefaultColor;

    private final boolean colorsHidden;

    private final List<String> defaultOrder;

    private final Map<String, BlockColorListSetting.BlockEntry> byId = new HashMap<>();

    private final ArrayList<BlockColorListSetting.BlockEntry> selection = new ArrayList<>();

    private final ReferenceOpenHashSet<BlockColorListSetting.BlockEntry> selectionSet =
            new ReferenceOpenHashSet<>();

    private final List<BlockColorListSetting.BlockEntry> selectionView =
            Collections.unmodifiableList(this.selection);

    private long selectionVersion = -1L;

    private Map<String, Integer> enabledLookup;

    private Map<String, Integer> tracerLookup;

    private Reference2IntOpenHashMap<Block> enabledByBlock;

    private Reference2IntOpenHashMap<Block> tracerByBlock;

    private long lookupVersion = -1L;

    private long currentVersion = 0L;

    private static final String WALL_BANNER = "_wall_banner";

    public BlockColorListSetting(
            String string, List<BlockColorListSetting.BlockEntry> list, float[] defaultColor2) {
        super(string);
        this.entries = new ArrayList<>(list);
        List list2 = list.stream().map(BlockColorListSetting::copyEntry).toList();
        this.shipped = () -> list2;
        this.defaultColor = defaultColor2;
        this.shippedDefaultColor = copyColor(defaultColor2);
        this.colorsHidden = false;
        this.defaultOrder = List.of();
        this.index();
        this.onChange(this::invalidateLookup);
    }

    public BlockColorListSetting(
            String string,
            Supplier<List<BlockColorListSetting.BlockEntry>> supplier,
            float[] floats) {
        this(string, supplier, floats, false, List.of());
    }

    private BlockColorListSetting(
            String string,
            Supplier<List<BlockColorListSetting.BlockEntry>> entriesSupplier2,
            float[] defaultColor2,
            boolean colorsHidden2,
            List<String> defaultOrder2) {
        super(string);
        this.entriesSupplier = entriesSupplier2;
        this.shipped = entriesSupplier2;
        this.defaultColor = defaultColor2;
        this.shippedDefaultColor = copyColor(defaultColor2);
        this.colorsHidden = colorsHidden2;
        this.defaultOrder = defaultOrder2;
        this.onChange(this::invalidateLookup);
    }

    public boolean colorsHidden() {
        return this.colorsHidden;
    }

    private List<BlockColorListSetting.BlockEntry> entriesInternal() {
        if (this.entries == null) {
            this.entries = new ArrayList<>(this.entriesSupplier.get());
            this.entriesSupplier = null;
            this.index();
        }
        return this.entries;
    }

    private void index() {
        for (BlockColorListSetting.BlockEntry entry : this.entries) {
            this.byId.putIfAbsent(entry.id(), entry);
        }
        for (String string : this.defaultOrder) {
            BlockColorListSetting.BlockEntry blockEntry = this.byId.get(string);
            if (blockEntry != null && blockEntry.enabled().get()) {
                this.addToSelection(blockEntry);
            }
        }
        for (BlockColorListSetting.BlockEntry entry2 : this.entries) {
            if (entry2.enabled().get()) {
                this.addToSelection(entry2);
            }
        }
        this.selectionVersion = this.currentVersion;
    }

    public List<BlockColorListSetting.BlockEntry> entries() {
        return Collections.unmodifiableList(this.entriesInternal());
    }

    public BlockColorListSetting.BlockEntry entry(String string) {
        this.entriesInternal();
        return this.byId.get(string);
    }

    public float[] defaultColor() {
        return this.defaultColor;
    }

    public void resetColors() {
        this.entriesInternal();
        for (BlockColorListSetting.BlockEntry blockEntry : this.shipped.get()) {
            BlockColorListSetting.BlockEntry blockEntry2 = this.byId.get(blockEntry.id());
            if (blockEntry2 != null) {
                System.arraycopy(blockEntry.color(), 0, blockEntry2.color(), 0, 4);
                blockEntry2.tracerEnabled().set(blockEntry.tracerEnabled().get());
                System.arraycopy(blockEntry.tracerColor(), 0, blockEntry2.tracerColor(), 0, 4);
            }
        }
        System.arraycopy(this.shippedDefaultColor, 0, this.defaultColor, 0, 4);
        this.notifyChanged();
    }

    public List<BlockColorListSetting.BlockEntry> selected() {
        List list = this.entriesInternal();
        if (this.selectionVersion != this.currentVersion) {
            for (int i = this.selection.size() - 1; i >= 0; i--) {
                BlockColorListSetting.BlockEntry blockEntry = this.selection.get(i);
                if (!blockEntry.enabled().get()) {
                    this.selection.remove(i);
                    this.selectionSet.remove(blockEntry);
                }
            }
            for (BlockColorListSetting.BlockEntry blockEntry2 :
                    (Iterable<BlockColorListSetting.BlockEntry>) list) {
                if (blockEntry2.enabled().get()) {
                    this.addToSelection(blockEntry2);
                }
            }
            this.selectionVersion = this.currentVersion;
        }
        return this.selectionView;
    }

    public boolean select(BlockColorListSetting.BlockEntry blockEntry) {
        this.selected();
        if (!this.addToSelection(blockEntry)) {
            return false;
        } else {
            blockEntry.enabled().set(true);
            this.notifyChanged();
            return true;
        }
    }

    public boolean deselect(BlockColorListSetting.BlockEntry blockEntry) {
        this.selected();
        if (!this.selectionSet.remove(blockEntry)) {
            return false;
        } else {
            for (int i = 0; i < this.selection.size(); i++) {
                if (this.selection.get(i) == blockEntry) {
                    this.selection.remove(i);
                    break;
                }
            }
            blockEntry.enabled().set(false);
            this.notifyChanged();
            return true;
        }
    }

    public int selectAll(Iterable<BlockColorListSetting.BlockEntry> iterable) {
        this.selected();
        int i = 0;
        for (BlockColorListSetting.BlockEntry blockEntry : iterable) {
            if (this.addToSelection(blockEntry)) {
                blockEntry.enabled().set(true);
                i++;
            }
        }
        if (i > 0) {
            this.notifyChanged();
        }
        return i;
    }

    public int deselectAll() {
        this.selected();
        int i = this.selection.size();
        if (i == 0) {
            return 0;
        } else {
            for (BlockColorListSetting.BlockEntry blockEntry : this.selection) {
                blockEntry.enabled().set(false);
            }
            this.selection.clear();
            this.selectionSet.clear();
            this.notifyChanged();
            return i;
        }
    }

    private boolean addToSelection(BlockColorListSetting.BlockEntry blockEntry) {
        if (!this.selectionSet.add(blockEntry)) {
            return false;
        } else {
            this.selection.add(blockEntry);
            return true;
        }
    }

    public void invalidateLookup() {
        this.currentVersion++;
    }

    private Map<String, Integer> lookup() {
        if (this.enabledLookup == null || this.lookupVersion != this.currentVersion) {
            this.rebuildLookup();
        }
        return this.enabledLookup;
    }

    private void rebuildLookup() {
        HashMap map = new HashMap();
        HashMap map2 = new HashMap();
        Reference2IntOpenHashMap reference2IntOpenHashMap = new Reference2IntOpenHashMap();
        Reference2IntOpenHashMap reference2IntOpenHashMap2 = new Reference2IntOpenHashMap();
        for (BlockColorListSetting.BlockEntry blockEntry : this.entriesInternal()) {
            if (blockEntry.enabled().get()) {
                BlockColorListSetting.BlockEntry blockEntry2 = this.colorSource(blockEntry);
                int rgbaToArgb = rgbaToArgb(blockEntry2.color());
                map.put(blockEntry.id(), rgbaToArgb);
                Block block = resolveBlock(blockEntry.id());
                if (block != null) {
                    reference2IntOpenHashMap.put(block, rgbaToArgb);
                }
                if (blockEntry2.tracerEnabled().get()) {
                    int rgbaToArgb2 = rgbaToArgb(blockEntry2.tracerColor());
                    map2.put(blockEntry.id(), rgbaToArgb2);
                    if (block != null) {
                        reference2IntOpenHashMap2.put(block, rgbaToArgb2);
                    }
                }
            }
        }
        this.enabledLookup = map;
        this.tracerLookup = map2;
        this.enabledByBlock = reference2IntOpenHashMap;
        this.tracerByBlock = reference2IntOpenHashMap2;
        this.lookupVersion = this.currentVersion;
    }

    private BlockColorListSetting.BlockEntry colorSource(
            BlockColorListSetting.BlockEntry blockEntry) {
        String string = blockEntry.id();
        if (!string.endsWith("_wall_banner")) {
            return blockEntry;
        } else {
            BlockColorListSetting.BlockEntry blockEntry2 =
                    this.byId.get(
                            string.substring(0, string.length() - "_wall_banner".length())
                                    + "_banner");
            return blockEntry2 != null && blockEntry2.enabled().get() ? blockEntry2 : blockEntry;
        }
    }

    private static Block resolveBlock(String string) {
        Identifier id2 = Identifier.tryParse(string);
        return id2 == null ? null : Registries.BLOCK.getOptionalValue(id2).orElse(null);
    }

    private Map<String, Integer> tracerLookup() {
        if (this.tracerLookup == null || this.lookupVersion != this.currentVersion) {
            this.rebuildLookup();
        }
        return this.tracerLookup;
    }

    private Reference2IntOpenHashMap<Block> blockLookup() {
        if (this.enabledByBlock == null || this.lookupVersion != this.currentVersion) {
            this.rebuildLookup();
        }
        return this.enabledByBlock;
    }

    private Reference2IntOpenHashMap<Block> tracerBlockLookup() {
        if (this.tracerByBlock == null || this.lookupVersion != this.currentVersion) {
            this.rebuildLookup();
        }
        return this.tracerByBlock;
    }

    public boolean isEnabled(Block block) {
        return this.blockLookup().containsKey(block);
    }

    public Set<Block> enabledBlocks() {
        return Collections.unmodifiableSet(this.blockLookup().keySet());
    }

    public int colorArgb(Block block) {
        Reference2IntOpenHashMap reference2IntOpenHashMap = this.blockLookup();
        return reference2IntOpenHashMap.containsKey(block)
                ? reference2IntOpenHashMap.getInt(block)
                : rgbaToArgb(this.defaultColor);
    }

    public boolean hasAnyEnabled() {
        return !this.lookup().isEmpty();
    }

    public Set<String> enabledIds() {
        return this.lookup().keySet();
    }

    public int tracerColorArgb(Block block) {
        Reference2IntOpenHashMap reference2IntOpenHashMap = this.tracerBlockLookup();
        return reference2IntOpenHashMap.containsKey(block)
                ? reference2IntOpenHashMap.getInt(block)
                : 0;
    }

    public boolean hasAnyTracerEnabled() {
        return !this.tracerLookup().isEmpty();
    }

    @Override
    public JsonElement serialize() {
        JsonObject json = new JsonObject();
        json.add("defaultColor", colorJson(this.defaultColor));
        JsonArray array = new JsonArray();
        for (BlockColorListSetting.BlockEntry blockEntry : this.selected()) {
            array.add(entryJson(blockEntry));
        }
        for (BlockColorListSetting.BlockEntry blockEntry2 : this.entriesInternal()) {
            if (!this.selectionSet.contains(blockEntry2)) {
                array.add(entryJson(blockEntry2));
            }
        }
        json.add("blocks", array);
        return json;
    }

    private static JsonObject entryJson(BlockColorListSetting.BlockEntry blockEntry) {
        JsonObject json = new JsonObject();
        json.addProperty("id", blockEntry.id());
        json.addProperty("enabled", blockEntry.enabled().get());
        json.add("color", colorJson(blockEntry.color()));
        json.addProperty("tracerEnabled", blockEntry.tracerEnabled().get());
        json.add("tracerColor", colorJson(blockEntry.tracerColor()));
        return json;
    }

    private static JsonArray colorJson(float[] floats) {
        JsonArray array = new JsonArray();
        for (float f : floats) {
            array.add(clamp01(f));
        }
        return array;
    }

    @Override
    public JsonElement compact(JsonElement json) {
        JsonElement json2 = this.defaultJson();
        if (json != null && json.isJsonObject() && json2 != null && json2.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonObject jsonObject2 = json2.getAsJsonObject();
            if (!jsonObject.has("changes")
                    && jsonObject.has("blocks")
                    && jsonObject2.has("blocks")) {
                try {
                    HashMap map = new HashMap();
                    for (JsonElement json3 : jsonObject2.getAsJsonArray("blocks")) {
                        JsonObject jsonObject3 = json3.getAsJsonObject();
                        map.put(jsonObject3.get("id").getAsString(), fields(jsonObject3));
                    }
                    JsonObject json4 = new JsonObject();
                    for (JsonElement json5 : jsonObject.getAsJsonArray("blocks")) {
                        if (json5.isJsonObject() && json5.getAsJsonObject().has("id")) {
                            JsonObject jsonObject4 = json5.getAsJsonObject();
                            String string = jsonObject4.get("id").getAsString();
                            String[] strings = fields(jsonObject4);
                            String[] strings2 = (String[]) map.get(string);
                            if (strings2 == null
                                    || !Arrays.equals((Object[]) strings, (Object[]) strings2)) {
                                boolean bl =
                                        strings2 == null
                                                || !strings[2].equals(strings2[2])
                                                || !strings[3].equals(strings2[3]);
                                StringBuilder sb = new StringBuilder(strings[0]);
                                if (bl || !strings[1].equals(strings2[1])) {
                                    sb.append(' ').append(strings[1]);
                                }
                                if (bl) {
                                    sb.append(' ')
                                            .append(strings[2])
                                            .append(' ')
                                            .append(strings[3]);
                                }
                                json4.addProperty(shortId(string), sb.toString());
                            }
                        }
                    }
                    JsonObject json6 = new JsonObject();
                    String hexColor = hexColor(jsonObject.get("defaultColor"));
                    if (hexColor != null
                            && !hexColor.equals(hexColor(jsonObject2.get("defaultColor")))) {
                        json6.addProperty("defaultColor", hexColor);
                    }
                    json6.add("changes", json4);
                    return json6;
                } catch (RuntimeException exception) {
                    return json;
                }
            } else {
                return json;
            }
        } else {
            return json;
        }
    }

    @Override
    protected void deserializeValue(JsonElement json) {
        if (json != null && json.isJsonObject()) {
            try {
                JsonObject jsonObject = json.getAsJsonObject();
                if (jsonObject.has("changes") && jsonObject.get("changes").isJsonObject()) {
                    this.applyChanges(jsonObject);
                } else {
                    this.applyFull(jsonObject);
                }
            } catch (Exception exception) {
            }
        }
    }

    private void applyFull(JsonObject json) {
        if (json.has("defaultColor")) {
            readColor(json.getAsJsonArray("defaultColor"), this.defaultColor);
        }
        if (json.has("blocks")) {
            JsonArray jsonArray = json.getAsJsonArray("blocks");
            ArrayList list = new ArrayList<>(this.selected());
            this.selection.clear();
            this.selectionSet.clear();
            for (JsonElement json2 : jsonArray) {
                BlockColorListSetting.BlockEntry blockEntry;
                JsonObject jsonObject;
                if (json2.isJsonObject()
                        && (jsonObject = json2.getAsJsonObject()).has("id")
                        && (blockEntry = this.byId.get(jsonObject.get("id").getAsString()))
                                != null) {
                    if (jsonObject.has("enabled")) {
                        blockEntry.enabled().set(jsonObject.get("enabled").getAsBoolean());
                    }
                    if (jsonObject.has("color")) {
                        readColor(jsonObject.getAsJsonArray("color"), blockEntry.color());
                    }
                    if (jsonObject.has("tracerEnabled")) {
                        blockEntry
                                .tracerEnabled()
                                .set(jsonObject.get("tracerEnabled").getAsBoolean());
                    }
                    if (jsonObject.has("tracerColor")) {
                        readColor(
                                jsonObject.getAsJsonArray("tracerColor"), blockEntry.tracerColor());
                    }
                    if (blockEntry.enabled().get()) {
                        this.addToSelection(blockEntry);
                    }
                }
            }
            for (BlockColorListSetting.BlockEntry blockEntry2 :
                    (Iterable<BlockColorListSetting.BlockEntry>) list) {
                if (blockEntry2.enabled().get()) {
                    this.addToSelection(blockEntry2);
                }
            }
        }
    }

    private void applyChanges(JsonObject json) {
        this.entriesInternal();
        JsonElement json2 = this.defaultJson();
        if (json2 != null && json2.isJsonObject()) {
            this.applyFull(json2.getAsJsonObject());
        }
        if (json.has("defaultColor")) {
            readHex(json.get("defaultColor").getAsString(), this.defaultColor);
        }
        for (Entry entry : json.getAsJsonObject("changes").entrySet()) {
            BlockColorListSetting.BlockEntry blockEntry =
                    this.byId.get(fullId((String) entry.getKey()));
            if (blockEntry != null && ((JsonElement) entry.getValue()).isJsonPrimitive()) {
                String[] strings = ((JsonElement) entry.getValue()).getAsString().split(" ");
                float[] floats;
                float[] floats2;
                try {
                    floats2 = strings.length >= 2 ? parseHex(strings[1]) : null;
                    floats = strings.length >= 4 ? parseHex(strings[3]) : null;
                } catch (NumberFormatException exception) {
                    continue;
                }
                blockEntry.enabled().set("1".equals(strings[0]));
                if (floats2 != null) {
                    System.arraycopy(floats2, 0, blockEntry.color(), 0, 4);
                }
                if (floats != null) {
                    blockEntry.tracerEnabled().set("1".equals(strings[2]));
                    System.arraycopy(floats, 0, blockEntry.tracerColor(), 0, 4);
                }
                if (blockEntry.enabled().get()) {
                    this.addToSelection(blockEntry);
                }
            }
        }
    }

    private static String[] fields(JsonObject json) {
        return new String[] {
            flag(json, "enabled"),
            String.valueOf(hexColor(json.get("color"))),
            flag(json, "tracerEnabled"),
            String.valueOf(hexColor(json.get("tracerColor")))
        };
    }

    private static String flag(JsonObject json, String string) {
        return json.has(string) && json.get(string).getAsBoolean() ? "1" : "0";
    }

    static String hexColor(JsonElement json) {
        if (json != null && json.isJsonArray()) {
            float[] floats = new float[] {1.0F, 1.0F, 1.0F, 1.0F};
            readColor(json.getAsJsonArray(), floats);
            return ColorSetting.hex(rgbaToArgb(floats));
        } else {
            return null;
        }
    }

    static void readHex(String string, float[] floats) {
        System.arraycopy(parseHex(string), 0, floats, 0, 4);
    }

    static float[] parseHex(String string) {
        int i = (int) Long.parseLong(string, 16);
        return new float[] {
            (i >> 16 & 0xFF) / 255.0F,
            (i >> 8 & 0xFF) / 255.0F,
            (i & 0xFF) / 255.0F,
            (i >>> 24) / 255.0F
        };
    }

    static String shortId(String string) {
        return string.startsWith("minecraft:") ? string.substring("minecraft:".length()) : string;
    }

    static String fullId(String string) {
        return string.indexOf(58) < 0 ? "minecraft:" + string : string;
    }

    private static void readColor(JsonArray array, float[] floats) {
        for (int i = 0; i < Math.min(4, array.size()); i++) {
            floats[i] = clamp01(array.get(i).getAsFloat());
        }
    }

    public static BlockColorListSetting createDefault() {
        return new BlockColorListSetting(
                "Block Colors", () -> buildEntries(value -> false), white());
    }

    public static BlockColorListSetting blockList(String string, List<String> list) {
        Set set = Set.copyOf(list);
        return new BlockColorListSetting(
                string, () -> buildEntries(set::contains), white(), true, List.copyOf(list));
    }

    private static List<BlockColorListSetting.BlockEntry> buildEntries(
            Predicate<String> predicate) {
        ArrayList list = new ArrayList();
        for (Block block : Registries.BLOCK) {
            Identifier id2;
            if (!block.getDefaultState().isAir() && (id2 = Registries.BLOCK.getId(block)) != null) {
                String string = id2.toString();
                list.add(
                        entry(
                                string,
                                prettify(id2.getPath()),
                                "Blocks",
                                predicate.test(string),
                                1.0F,
                                1.0F,
                                1.0F,
                                1.0F));
            }
        }
        return list;
    }

    public static BlockColorListSetting createForBlockEntityDebug() {
        return new BlockColorListSetting(
                "Block Colors", BlockColorListSetting::buildBlockEntityDebugEntries, white());
    }

    private static List<BlockColorListSetting.BlockEntry> buildBlockEntityDebugEntries() {
        ArrayList list = new ArrayList();
        for (Block block : Registries.BLOCK) {
            Identifier id2;
            if (!block.getDefaultState().isAir() && (id2 = Registries.BLOCK.getId(block)) != null) {
                String path = id2.getPath();
                int blockEntityDebugRgb = blockEntityDebugRgb(path);
                boolean bl = blockEntityDebugRgb != -1 || blockEntityDebugPicked(path);
                list.add(
                        entry(
                                id2.toString(),
                                prettify(path),
                                "Blocks",
                                bl,
                                blockEntityDebugRgb == -1 ? 16777215 : blockEntityDebugRgb,
                                true));
            }
        }
        return list;
    }

    private static int blockEntityDebugRgb(String string) {
        if (string.endsWith("copper_chest")) {
            return 10246912;
        } else if (string.endsWith("shulker_box")) {
            return 8781982;
        } else {
            return switch (string) {
                case "chest" -> 10246912;
                case "trapped_chest" -> 13775410;
                case "ender_chest" -> 7667967;
                case "barrel" -> 6572850;
                case "hopper" -> 5263440;
                case "dispenser", "dropper" -> 7887420;
                case "brewing_stand" -> 7874760;
                case "furnace", "blast_furnace", "smoker" -> 6579300;
                case "spawner" -> 9076390;
                case "enchanting_table" -> 13107200;
                case "crafter" -> 3978400;
                case "moving_piston", "piston", "sticky_piston", "piston_head" -> 65280;
                case "lectern" -> 13806160;
                case "jukebox" -> 2017380;
                case "decorated_pot" -> 11822120;
                case "beehive", "bee_nest" -> 15779840;
                case "beacon" -> 61660;
                case "conduit" -> 46320;
                case "comparator" -> 13120050;
                default -> -1;
            };
        }
    }

    private static boolean blockEntityDebugPicked(String string) {
        if (!string.contains("sign")
                && !string.endsWith("_bed")
                && !string.endsWith("_banner")
                && !string.endsWith("_skull")
                && !string.endsWith("_head")) {
            return switch (string) {
                case "bell",
                                "campfire",
                                "soul_campfire",
                                "jigsaw",
                                "structure_block",
                                "command_block",
                                "chain_command_block",
                                "repeating_command_block",
                                "end_portal",
                                "end_gateway",
                                "sculk_sensor",
                                "calibrated_sculk_sensor",
                                "sculk_catalyst",
                                "sculk_shrieker",
                                "suspicious_sand",
                                "suspicious_gravel",
                                "trial_spawner",
                                "vault",
                                "chiseled_bookshelf",
                                "daylight_detector" ->
                        true;
                default -> false;
            };
        } else {
            return true;
        }
    }

    private static String prettify(String string) {
        String[] strings = string.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            if (!strings[i].isEmpty()) {
                sb.append(Character.toUpperCase(strings[i].charAt(0)));
                sb.append(strings[i].substring(1));
                if (i < strings.length - 1) {
                    sb.append(" ");
                }
            }
        }
        return sb.toString();
    }

    private static float[] white() {
        return new float[] {1.0F, 1.0F, 1.0F, 1.0F};
    }

    private static BlockColorListSetting.BlockEntry entry(
            String string,
            String string2,
            String string3,
            boolean bl,
            float f,
            float g,
            float h,
            float f2) {
        float[] floats = new float[] {f, g, h, f2};
        return new BlockColorListSetting.BlockEntry(
                string,
                string2,
                string3,
                new ImBoolean(bl),
                floats,
                new ImBoolean(false),
                copyColor(floats));
    }

    private static BlockColorListSetting.BlockEntry entry(
            String string, String string2, String string3, boolean bl, int i, boolean bl2) {
        float[] floats =
                new float[] {
                    (i >> 16 & 0xFF) / 255.0F, (i >> 8 & 0xFF) / 255.0F, (i & 0xFF) / 255.0F, 1.0F
                };
        return new BlockColorListSetting.BlockEntry(
                string,
                string2,
                string3,
                new ImBoolean(bl),
                floats,
                new ImBoolean(bl2),
                copyColor(floats));
    }

    private static BlockColorListSetting.BlockEntry copyEntry(
            BlockColorListSetting.BlockEntry blockEntry) {
        return new BlockColorListSetting.BlockEntry(
                blockEntry.id(),
                blockEntry.displayName(),
                blockEntry.category(),
                new ImBoolean(blockEntry.enabled().get()),
                copyColor(blockEntry.color()),
                new ImBoolean(blockEntry.tracerEnabled().get()),
                copyColor(blockEntry.tracerColor()));
    }

    private static float[] copyColor(float[] floats) {
        return new float[] {floats[0], floats[1], floats[2], floats[3]};
    }

    public static int rgbaToArgb(float[] floats) {
        int i = Math.round(clamp01(floats[0]) * 255.0F);
        int j = Math.round(clamp01(floats[1]) * 255.0F);
        int k = Math.round(clamp01(floats[2]) * 255.0F);
        int m = Math.round(clamp01(floats[3]) * 255.0F);
        return m << 24 | i << 16 | j << 8 | k;
    }

    private static float clamp01(float f) {
        return !Float.isNaN(f) && !Float.isInfinite(f) ? Math.max(0.0F, Math.min(1.0F, f)) : 1.0F;
    }

    @Environment(EnvType.CLIENT)
    public record BlockEntry(
            String id,
            String displayName,
            String category,
            ImBoolean enabled,
            float[] color,
            ImBoolean tracerEnabled,
            float[] tracerColor) {}
}
