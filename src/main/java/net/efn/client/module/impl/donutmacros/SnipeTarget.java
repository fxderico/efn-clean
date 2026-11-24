/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.donutmacros;

import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(value = EnvType.CLIENT)
final class SnipeTarget {

    private static final Pattern DURATION = Pattern.compile("(?<!\\d)(\\d{1,5})\\s*([dhms])");

    final Item item;

    final boolean shard;

    final Preset preset;

    final String displayName;

    final String search;

    final List<Product.Enchant> enchants;

    final boolean exactEnchants;

    final long maxPrice;

    final int minMinutes;

    private final Predicate<ItemStack> filter;

    private SnipeTarget(
            Item item2,
            boolean shard2,
            Preset preset2,
            String displayName2,
            String search2,
            List<Product.Enchant> enchants2,
            boolean exactEnchants2,
            long maxPrice2,
            int minMinutes2,
            Predicate<ItemStack> filter2) {
        this.item = item2;
        this.shard = shard2;
        this.preset = preset2;
        this.displayName = displayName2;
        this.search = search2;
        this.enchants = enchants2;
        this.exactEnchants = exactEnchants2;
        this.maxPrice = maxPrice2;
        this.minMinutes = minMinutes2;
        this.filter = filter2;
    }

    static SnipeTarget matching(
            Item item2, String string, String string2, Predicate<ItemStack> predicate, long l) {
        return new SnipeTarget(
                item2, false, null, string, string2, List.of(), false, l, 0, predicate);
    }

    static SnipeTarget parse(String string, String string2, boolean bl, long l, int i) {
        boolean bl2;
        Item item2;
        List<Product.Enchant> enchants2 = Product.parseEnchants(string2);
        Preset preset2 = Preset.of(string);
        if (preset2 != null) {
            return new SnipeTarget(
                    preset2.item,
                    true,
                    preset2,
                    preset2.label,
                    preset2.search,
                    enchants2,
                    bl,
                    l,
                    i,
                    null);
        }
        String string3 =
                string == null ? "" : string.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        String string4 = string3;
        if (string3.startsWith("minecraft:")) {
            string3 = string3.substring("minecraft:".length());
        }
        if ((item2 =
                        Product.resolveItem(
                                (String)
                                        ((bl2 = string3.startsWith("shard_"))
                                                ? "netherite_"
                                                        + string3.substring("shard_".length())
                                                : string3)))
                == null) {
            return null;
        }
        Object object =
                bl2
                        ? "Shard " + SnipeTarget.capitalize(string3.substring("shard_".length()))
                        : new ItemStack(item2).getName().getString();
        return new SnipeTarget(
                item2,
                bl2,
                null,
                (String) object,
                Product.searchText(item2),
                enchants2,
                bl,
                l,
                bl2 ? i : 0,
                null);
    }

    private static String capitalize(String string) {
        StringBuilder sb = new StringBuilder();
        for (String string2 : string.split("_")) {
            if (string2.isEmpty()) continue;
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(string2.charAt(0))).append(string2.substring(1));
        }
        return sb.toString();
    }

    boolean sameKind(ItemStack stack) {
        if (this.filter != null) {
            return !stack.isEmpty() && this.filter.test(stack);
        }
        if (stack.isEmpty() || !stack.isOf(this.item)) {
            return false;
        }
        String key = ServerText.key(stack.getName().getString());
        if (this.preset != null) {
            if (!key.contains(this.preset.keyword)) {
                return false;
            }
            if (!this.preset.exclude.isEmpty() && key.contains(this.preset.exclude)) {
                return false;
            }
        } else if (key.contains("shard") != this.shard) {
            return false;
        }
        return this.enchants.isEmpty()
                || Product.enchantsMatch(stack, this.enchants, this.exactEnchants);
    }

    boolean timed() {
        return this.preset != null || this.shard;
    }

    boolean matches(ItemStack stack) {
        if (!this.sameKind(stack)) {
            return false;
        }
        if (this.preset != null) {
            return SnipeTarget.minutesLeft(stack) >= Math.max(0, this.minMinutes);
        }
        return this.minMinutes <= 0 || SnipeTarget.minutesLeft(stack) >= this.minMinutes;
    }

    static int minutesLeft(ItemStack stack) {
        int j;
        ArrayList<String> list = new ArrayList<String>();
        for (String string : ServerText.lore(stack)) {
            list.add(ServerText.normalize(string));
        }
        ArrayList list2 = new ArrayList();
        for (j = 0; j < list.size(); ++j) {
            if (!((String) list.get(j)).contains("$") && !((String) list.get(j)).contains("seller"))
                continue;
            list2.add(j);
        }
        j = -1;
        int k = -1;
        int m = 0;
        for (int i = 0; i < list.size(); ++i) {
            int duration;
            String string2 = (String) list.get(i);
            boolean bl = string2.contains("destruct");
            if (!bl && !string2.contains("expire")
                    || (duration = SnipeTarget.duration(string2)) < 0) continue;
            if (bl) {
                return duration;
            }
            ++m;
            int mAX_VALUE = Integer.MAX_VALUE;
            Iterator iterator = list2.iterator();
            while (iterator.hasNext()) {
                int n = (Integer) iterator.next();
                mAX_VALUE = Math.min(mAX_VALUE, Math.abs(n - i));
            }
            if (mAX_VALUE <= k) continue;
            k = mAX_VALUE;
            j = duration;
        }
        if (j < 0) {
            return -1;
        }
        return list2.isEmpty() || m >= 2 || k >= 2 ? j : -1;
    }

    private static int duration(String string) {
        Matcher matcher = DURATION.matcher(string);
        int i = 0;
        boolean bl = false;
        while (matcher.find()) {
            bl = true;
            int j = Integer.parseInt(matcher.group(1));
            switch (matcher.group(2)) {
                case "d":
                    {
                        i += j * 1440;
                        break;
                    }
                case "h":
                    {
                        i += j * 60;
                        break;
                    }
                case "m":
                    {
                        i += j;
                    }
            }
        }
        return bl ? i : -1;
    }

    Product product(long l) {
        return new Product(
                this.item,
                Registries.ITEM.getId(this.item).getPath(),
                this.search,
                this.displayName,
                l,
                this.enchants,
                false,
                1,
                false,
                1L,
                1L,
                0L,
                this::sameKind);
    }

    String describe() {
        StringBuilder sb = new StringBuilder(this.displayName);
        if (!this.enchants.isEmpty()) {
            StringBuilder sb2 = new StringBuilder();
            for (Product.Enchant enchant : this.enchants) {
                if (sb2.length() > 0) {
                    sb2.append(", ");
                }
                sb2.append(enchant.label());
            }
            sb.append(" (")
                    .append(this.exactEnchants ? "exactly " : "at least ")
                    .append((CharSequence) sb2)
                    .append(')');
        }
        return sb.toString();
    }

    @Environment(value = EnvType.CLIENT)
    static enum Preset {
        SELL_AXE("sell_axe", "Sell Axe", Items.NETHERITE_AXE, "sell axe", "sell", ""),
        DRILL("drill", "Drill", Items.NETHERITE_PICKAXE, "drill", "drill", ""),
        SHARD_AXE("shard_axe", "Shard Axe", Items.NETHERITE_AXE, "shard axe", "shard", "sell");

        final String value;

        final String label;

        final Item item;

        final String search;

        final String keyword;

        final String exclude;

        private Preset(
                String value2,
                String label2,
                Item item2,
                String search2,
                String keyword2,
                String exclude2) {
            this.value = value2;
            this.label = label2;
            this.item = item2;
            this.search = search2;
            this.keyword = keyword2;
            this.exclude = exclude2;
        }

        static Preset of(String string) {
            String string2 =
                    string == null ? "" : string.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
            for (Preset preset2 : Preset.values()) {
                if (!preset2.value.equals(string2)) continue;
                return preset2;
            }
            return null;
        }

        static List<StringSetting.Preset> choices() {
            ArrayList<StringSetting.Preset> list = new ArrayList<StringSetting.Preset>();
            for (Preset preset2 : Preset.values()) {
                list.add(
                        new StringSetting.Preset(
                                preset2.value,
                                preset2.label,
                                Registries.ITEM.getId(preset2.item).getPath(),
                                true));
            }
            return list;
        }
    }
}
