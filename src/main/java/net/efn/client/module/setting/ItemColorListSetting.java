package net.efn.client.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import imgui.type.ImBoolean;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class ItemColorListSetting extends Setting {

    private List<ItemColorListSetting.ItemEntry> entries;

    private Supplier<List<ItemColorListSetting.ItemEntry>> entriesSupplier;

    private final float[] defaultColor;

    private Map<String, Integer> enabledLookup;

    public ItemColorListSetting(
            String string, List<ItemColorListSetting.ItemEntry> list, float[] defaultColor2) {
        super(string);
        this.entries = new ArrayList<>(list);
        this.defaultColor = defaultColor2;
        this.onChange(this::invalidateLookup);
    }

    public ItemColorListSetting(
            String string,
            Supplier<List<ItemColorListSetting.ItemEntry>> entriesSupplier2,
            float[] defaultColor2) {
        super(string);
        this.entriesSupplier = entriesSupplier2;
        this.defaultColor = defaultColor2;
        this.onChange(this::invalidateLookup);
    }

    private List<ItemColorListSetting.ItemEntry> entriesInternal() {
        if (this.entries == null) {
            this.entries = new ArrayList<>(this.entriesSupplier.get());
            this.entriesSupplier = null;
        }
        return this.entries;
    }

    public List<ItemColorListSetting.ItemEntry> entries() {
        return Collections.unmodifiableList(this.entriesInternal());
    }

    public float[] defaultColor() {
        return this.defaultColor;
    }

    public void invalidateLookup() {
        this.enabledLookup = null;
    }

    private Map<String, Integer> lookup() {
        if (this.enabledLookup == null) {
            this.rebuildLookup();
        }
        return this.enabledLookup;
    }

    private void rebuildLookup() {
        HashMap map = new HashMap();
        for (ItemColorListSetting.ItemEntry itemEntry : this.entriesInternal()) {
            if (itemEntry.enabled().get()) {
                map.put(itemEntry.id(), rgbaToArgb(itemEntry.color()));
            }
        }
        this.enabledLookup = map;
    }

    public boolean isEnabled(Item item) {
        Identifier id2 = Registries.ITEM.getId(item);
        return id2 == null ? false : this.lookup().containsKey(id2.toString());
    }

    public int colorArgb(Item item) {
        Identifier id2 = Registries.ITEM.getId(item);
        if (id2 == null) {
            return rgbaToArgb(this.defaultColor);
        } else {
            Integer integer = this.lookup().get(id2.toString());
            return integer != null ? integer : rgbaToArgb(this.defaultColor);
        }
    }

    public boolean hasAnyEnabled() {
        return !this.lookup().isEmpty();
    }

    @Override
    public JsonElement serialize() {
        JsonObject json = new JsonObject();
        JsonArray array = new JsonArray();
        for (float f : this.defaultColor) {
            array.add(clamp01(f));
        }
        json.add("defaultColor", array);
        JsonArray array2 = new JsonArray();
        for (ItemColorListSetting.ItemEntry itemEntry : this.entriesInternal()) {
            JsonObject json2 = new JsonObject();
            json2.addProperty("id", itemEntry.id());
            json2.addProperty("enabled", itemEntry.enabled().get());
            JsonArray array3 = new JsonArray();
            for (float g : itemEntry.color()) {
                array3.add(clamp01(g));
            }
            json2.add("color", array3);
            array2.add(json2);
        }
        json.add("items", array2);
        return json;
    }

    @Override
    public JsonElement compact(JsonElement json) {
        JsonElement json2 = this.defaultJson();
        if (json != null && json.isJsonObject() && json2 != null && json2.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonObject jsonObject2 = json2.getAsJsonObject();
            if (!jsonObject.has("changes") && jsonObject.has("items") && jsonObject2.has("items")) {
                try {
                    HashMap map = new HashMap();
                    for (JsonElement json3 : jsonObject2.getAsJsonArray("items")) {
                        JsonObject jsonObject3 = json3.getAsJsonObject();
                        map.put(jsonObject3.get("id").getAsString(), packItem(jsonObject3));
                    }
                    JsonObject json4 = new JsonObject();
                    for (JsonElement json5 : jsonObject.getAsJsonArray("items")) {
                        if (json5.isJsonObject() && json5.getAsJsonObject().has("id")) {
                            JsonObject jsonObject4 = json5.getAsJsonObject();
                            String string = jsonObject4.get("id").getAsString();
                            String packItem = packItem(jsonObject4);
                            String string2;
                            if (!packItem.equals(string2 = (String) map.get(string))) {
                                boolean bl =
                                        string2 != null
                                                && packItem.substring(2)
                                                        .equals(string2.substring(2));
                                json4.addProperty(
                                        BlockColorListSetting.shortId(string),
                                        bl ? packItem.substring(0, 1) : packItem);
                            }
                        }
                    }
                    JsonObject json6 = new JsonObject();
                    String hexColor =
                            BlockColorListSetting.hexColor(jsonObject.get("defaultColor"));
                    if (hexColor != null
                            && !hexColor.equals(
                                    BlockColorListSetting.hexColor(
                                            jsonObject2.get("defaultColor")))) {
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

    private static String packItem(JsonObject json) {
        boolean bl = json.has("enabled") && json.get("enabled").getAsBoolean();
        return (bl ? "1" : "0") + " " + BlockColorListSetting.hexColor(json.get("color"));
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
            JsonArray jsonArray = json.getAsJsonArray("defaultColor");
            for (int i = 0; i < Math.min(4, jsonArray.size()); i++) {
                this.defaultColor[i] = clamp01(jsonArray.get(i).getAsFloat());
            }
        }
        if (json.has("items")) {
            Map map = this.byId();
            for (JsonElement json2 : json.getAsJsonArray("items")) {
                ItemColorListSetting.ItemEntry itemEntry;
                JsonObject jsonObject;
                if (json2.isJsonObject()
                        && (jsonObject = json2.getAsJsonObject()).has("id")
                        && (itemEntry =
                                        (ItemColorListSetting.ItemEntry)
                                                map.get(jsonObject.get("id").getAsString()))
                                != null) {
                    if (jsonObject.has("enabled")) {
                        itemEntry.enabled().set(jsonObject.get("enabled").getAsBoolean());
                    }
                    if (jsonObject.has("color")) {
                        JsonArray jsonArray2 = jsonObject.getAsJsonArray("color");
                        float[] floats = itemEntry.color();
                        for (int j = 0; j < Math.min(4, jsonArray2.size()); j++) {
                            floats[j] = clamp01(jsonArray2.get(j).getAsFloat());
                        }
                    }
                }
            }
        }
    }

    private void applyChanges(JsonObject json) {
        JsonElement json2 = this.defaultJson();
        if (json2 != null && json2.isJsonObject()) {
            this.applyFull(json2.getAsJsonObject());
        }
        if (json.has("defaultColor")) {
            BlockColorListSetting.readHex(
                    json.get("defaultColor").getAsString(), this.defaultColor);
        }
        Map map = this.byId();
        for (Entry entry : json.getAsJsonObject("changes").entrySet()) {
            ItemColorListSetting.ItemEntry itemEntry =
                    (ItemColorListSetting.ItemEntry)
                            map.get(BlockColorListSetting.fullId((String) entry.getKey()));
            if (itemEntry != null && ((JsonElement) entry.getValue()).isJsonPrimitive()) {
                String[] strings = ((JsonElement) entry.getValue()).getAsString().split(" ");
                float[] floats;
                try {
                    floats =
                            strings.length >= 2 ? BlockColorListSetting.parseHex(strings[1]) : null;
                } catch (NumberFormatException exception) {
                    continue;
                }
                itemEntry.enabled().set("1".equals(strings[0]));
                if (floats != null) {
                    System.arraycopy(floats, 0, itemEntry.color(), 0, 4);
                }
            }
        }
    }

    private Map<String, ItemColorListSetting.ItemEntry> byId() {
        HashMap map = new HashMap();
        for (ItemColorListSetting.ItemEntry itemEntry : this.entriesInternal()) {
            map.putIfAbsent(itemEntry.id(), itemEntry);
        }
        return map;
    }

    public static ItemColorListSetting createDefault() {
        return new ItemColorListSetting(
                "Item Colors",
                ItemColorListSetting::buildDefaultEntries,
                new float[] {1.0F, 1.0F, 1.0F, 0.8F});
    }

    private static List<ItemColorListSetting.ItemEntry> buildDefaultEntries() {
        ArrayList list = new ArrayList();
        for (Item item : Registries.ITEM) {
            Identifier id2 = Registries.ITEM.getId(item);
            if (id2 != null) {
                String string = id2.toString();
                String path = id2.getPath();
                String prettify = prettify(path);
                float[] floats = defaultColorFor(path);
                boolean preEnabled = isPreEnabled(path);
                list.add(entry(string, prettify, preEnabled, floats));
            }
        }
        list.sort(
                (itemEntry, itemEntry2) ->
                        ((ItemColorListSetting.ItemEntry) itemEntry)
                                .displayName()
                                .compareToIgnoreCase(
                                        ((ItemColorListSetting.ItemEntry) itemEntry2)
                                                .displayName()));
        return list;
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

    private static boolean isPreEnabled(String string) {
        return switch (string) {
            case "diamond",
                            "emerald",
                            "netherite_ingot",
                            "ancient_debris",
                            "elytra",
                            "totem_of_undying",
                            "trident",
                            "enchanted_golden_apple",
                            "golden_apple",
                            "nether_star",
                            "heart_of_the_sea",
                            "end_crystal",
                            "shulker_box" ->
                    true;
            case "gold_ingot", "gold_block", "gold_nugget" -> true;
            case "iron_ingot" -> true;
            default -> false;
        };
    }

    private static float[] defaultColorFor(String string) {
        return switch (string) {
            case "diamond",
                            "diamond_sword",
                            "diamond_pickaxe",
                            "diamond_axe",
                            "diamond_shovel",
                            "diamond_hoe",
                            "diamond_helmet",
                            "diamond_chestplate",
                            "diamond_leggings",
                            "diamond_boots" ->
                    rgba(0, 255, 255);
            case "emerald" -> rgba(0, 255, 68);
            case "netherite_ingot",
                            "ancient_debris",
                            "netherite_sword",
                            "netherite_pickaxe",
                            "netherite_axe",
                            "netherite_shovel",
                            "netherite_hoe",
                            "netherite_helmet",
                            "netherite_chestplate",
                            "netherite_leggings",
                            "netherite_boots" ->
                    rgba(128, 96, 144);
            case "gold_ingot",
                            "gold_block",
                            "gold_nugget",
                            "golden_sword",
                            "golden_pickaxe",
                            "golden_axe",
                            "golden_shovel",
                            "golden_hoe",
                            "golden_helmet",
                            "golden_chestplate",
                            "golden_leggings",
                            "golden_boots",
                            "golden_apple",
                            "enchanted_golden_apple" ->
                    rgba(255, 215, 0);
            case "iron_ingot" -> rgba(204, 204, 204);
            case "elytra" -> rgba(85, 85, 255);
            case "totem_of_undying" -> rgba(255, 170, 0);
            case "trident" -> rgba(0, 136, 255);
            case "nether_star" -> rgba(255, 255, 0);
            case "heart_of_the_sea" -> rgba(0, 255, 221);
            case "end_crystal" -> rgba(255, 0, 255);
            case "shulker_box" -> rgba(204, 68, 204);
            default -> rgba(255, 255, 255);
        };
    }

    private static float[] rgba(int i, int j, int k) {
        return new float[] {i / 255.0F, j / 255.0F, k / 255.0F, 1.0F};
    }

    private static ItemColorListSetting.ItemEntry entry(
            String string, String string2, boolean bl, float[] floats) {
        return new ItemColorListSetting.ItemEntry(string, string2, new ImBoolean(bl), floats);
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
    public record ItemEntry(String id, String displayName, ImBoolean enabled, float[] color) {}
}
