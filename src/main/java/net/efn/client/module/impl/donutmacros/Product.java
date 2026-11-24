package net.efn.client.module.impl.donutmacros;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
record Product(
        Item item,
        String id,
        String search,
        String displayName,
        long minPrice,
        List<Product.Enchant> enchants,
        boolean restock,
        int orderAmount,
        boolean beatTopOrder,
        long fixedPrice,
        long bidIncrement,
        long maxPrice,
        Predicate<ItemStack> filter) {

    Product(
            Item item,
            String string,
            String string2,
            String string3,
            long l,
            List<Product.Enchant> list,
            boolean bl,
            int i,
            boolean bl2,
            long m,
            long n,
            long o) {
        this(item, string, string2, string3, l, list, bl, i, bl2, m, n, o, null);
    }

    static Item resolveItem(String string4) {
        String string5 =
                string4 == null ? "" : string4.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (string5.isEmpty()) {
            return null;
        } else {
            if (!string5.contains(":")) {
                string5 = "minecraft:" + string5;
            }
            Identifier id2;
            if ((id2 = Identifier.tryParse(string5)) != null && Registries.ITEM.containsId(id2)) {
                Item item2 = Registries.ITEM.get(id2);
                return item2 == Items.AIR ? null : item2;
            } else {
                return null;
            }
        }
    }

    static String searchText(Item item2) {
        return Registries.ITEM.getId(item2).getPath().replace('_', ' ');
    }

    static List<Product.Enchant> parseEnchants(String string4) {
        ArrayList list2 = new ArrayList();
        if (string4 == null) {
            return list2;
        } else {
            for (String string5 : string4.split("[,;]")) {
                String string6 = string5.trim();
                if (!string6.isEmpty()) {
                    int j = 1;
                    int k = string6.lastIndexOf(32);
                    int level2;
                    if (k > 0 && (level2 = parseLevel(string6.substring(k + 1).trim())) > 0) {
                        j = level2;
                        string6 = string6.substring(0, k).trim();
                    }
                    String key2;
                    if (!(key2 = ServerText.key(string6)).isEmpty()) {
                        list2.add(new Product.Enchant(key2, string6, j));
                    }
                }
            }
            return list2;
        }
    }

    private static int parseLevel(String string4) {
        try {
            int j = Integer.parseInt(string4);
            return j >= 1 && j <= 10 ? j : 0;
        } catch (NumberFormatException exception) {
            return ServerText.roman(string4);
        }
    }

    boolean matches(ItemStack stack) {
        if (!stack.isEmpty() && stack.isOf(this.item)) {
            if (this.filter != null) {
                return this.filter.test(stack);
            } else if (this.enchants.isEmpty()) {
                return true;
            } else {
                ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
                for (Product.Enchant enchant : this.enchants) {
                    if (levelOf(enchantments, enchant.key()) < enchant.level()) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }

    static boolean enchantsMatch(ItemStack stack, List<Product.Enchant> list2, boolean bl3) {
        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
        if (bl3 && enchantments.getSize() != list2.size()) {
            return false;
        } else {
            for (Product.Enchant enchant : list2) {
                int level2 = levelOf(enchantments, enchant.key());
                if (bl3 ? level2 != enchant.level() : level2 < enchant.level()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static int levelOf(
            ItemEnchantmentsComponent itemEnchantmentsComponent, String string4) {
        for (Entry enchantmentEntry : itemEnchantmentsComponent.getEnchantmentEntries()) {
            String string5 =
                    ((RegistryEntry<?>) enchantmentEntry.getKey())
                            .getKey()
                            .map(registryKey -> registryKey.getValue().getPath())
                            .orElse("");
            if (ServerText.key(string5).equals(string4)) {
                return enchantmentEntry.getIntValue();
            }
        }
        return 0;
    }

    boolean nameMatches(String string4, boolean bl3) {
        String key2 = ServerText.key(string4);
        String key3 = ServerText.key(Registries.ITEM.getId(this.item).getPath());
        String key4 = ServerText.key(this.displayName);
        return !key2.equals(key3) && !key2.equals(key4)
                ? bl3 && (key2.endsWith(key3) || !key4.isEmpty() && key2.endsWith(key4))
                : true;
    }

    String describe() {
        StringBuilder sb = new StringBuilder(this.displayName);
        if (!this.enchants.isEmpty()) {
            ArrayList list2 = new ArrayList();
            for (Product.Enchant enchant : this.enchants) {
                list2.add(enchant.label());
            }
            sb.append(" (").append(String.join(", ", list2)).append(')');
        }
        return sb.toString();
    }

    @Environment(EnvType.CLIENT)
    record Enchant(String key, String name, int level) {

        String label() {
            return this.name + (this.level > 1 ? " " + this.level : "");
        }
    }
}
