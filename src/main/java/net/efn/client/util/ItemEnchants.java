/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Environment(value = EnvType.CLIENT)
public final class ItemEnchants {

    private static final String[] ROMAN =
            new String[] {"i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x"};

    private static DynamicRegistryManager cachedFor;

    private static List<RegistryEntry<Enchantment>> cached;

    private ItemEnchants() {}

    public static Item resolve(String string) {
        Identifier id;
        Object object =
                string == null ? "" : string.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        String string2 = (String) object;
        if (((String) object).isEmpty()) {
            return null;
        }
        if (((String) object).indexOf(58) < 0) {
            object = "minecraft:" + (String) object;
        }
        if ((id = Identifier.tryParse((String) object)) == null
                || !Registries.ITEM.containsId(id)) {
            return null;
        }
        Item item = Registries.ITEM.get(id);
        return item == Items.AIR ? null : item;
    }

    public static String stored(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    public static boolean supports(Item item) {
        if (item == null) {
            return false;
        }
        if (item == Items.ENCHANTED_BOOK) {
            return true;
        }
        List<RegistryEntry<Enchantment>> list = ItemEnchants.all();
        ItemStack defaultStack = item.getDefaultStack();
        if (list.isEmpty()) {
            return defaultStack.contains(DataComponentTypes.ENCHANTABLE);
        }
        for (RegistryEntry<Enchantment> entry : list) {
            if (!entry.value().isSupportedItem(defaultStack)) continue;
            return true;
        }
        return false;
    }

    public static List<RegistryEntry<Enchantment>> applicable(Item item) {
        ArrayList<RegistryEntry<Enchantment>> list = new ArrayList<RegistryEntry<Enchantment>>();
        if (item == null) {
            return list;
        }
        ItemStack defaultStack = item.getDefaultStack();
        boolean bl = item == Items.ENCHANTED_BOOK;
        for (RegistryEntry<Enchantment> entry : ItemEnchants.all()) {
            if (!bl && !entry.value().isSupportedItem(defaultStack)) continue;
            list.add(entry);
        }
        list.sort(
                Comparator.comparing(
                        registryEntry ->
                                ((Enchantment) registryEntry.value()).description().getString(),
                        String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public static List<RegistryEntry<Enchantment>> all() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return List.of();
        }
        DynamicRegistryManager registryManager = client.world.getRegistryManager();
        if (registryManager != cachedFor) {
            ArrayList list = new ArrayList();
            registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).streamEntries().forEach(list::add);
            cached = List.copyOf(list);
            cachedFor = registryManager;
        }
        return cached;
    }

    public static String path(RegistryEntry<Enchantment> entry) {
        return entry.getKey().map(registryKey -> registryKey.getValue().getPath()).orElse("");
    }

    public static String key(String string) {
        String string2 = string == null ? "" : string.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(string2.length());
        for (int i = 0; i < string2.length(); ++i) {
            char c = string2.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9')) continue;
            sb.append(c);
        }
        return sb.toString();
    }

    public static Map<String, Integer> parse(String string) {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        if (string == null) {
            return map;
        }
        for (String string2 : string.split("[,;]")) {
            String key;
            int level;
            String string3 = string2.trim();
            if (string3.isEmpty()) continue;
            int i = 1;
            int j = string3.lastIndexOf(32);
            if (j > 0 && (level = ItemEnchants.level(string3.substring(j + 1).trim())) > 0) {
                i = level;
                string3 = string3.substring(0, j).trim();
            }
            if ((key = ItemEnchants.key(string3)).isEmpty()) continue;
            map.put(key, i);
        }
        return map;
    }

    private static int level(String string) {
        try {
            int j = Integer.parseInt(string);
            return j >= 1 && j <= 10 ? j : 0;
        } catch (NumberFormatException exception) {
            String string2 = string.toLowerCase(Locale.ROOT);
            for (int i = 0; i < ROMAN.length; ++i) {
                if (!ROMAN[i].equals(string2)) continue;
                return i + 1;
            }
            return 0;
        }
    }

    public static String roman(int i) {
        return i >= 1 && i <= ROMAN.length
                ? ROMAN[i - 1].toUpperCase(Locale.ROOT)
                : Integer.toString(i);
    }

    static {
        cached = List.of();
    }
}
