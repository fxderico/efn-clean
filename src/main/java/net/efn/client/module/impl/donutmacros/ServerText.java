/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.donutmacros;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(value = EnvType.CLIENT)
final class ServerText {

    private static final Pattern FORMATTING = Pattern.compile("\u00a7.");

    private static final Pattern MONEY_EXACT =
            Pattern.compile("^\\$?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kmb])?$");

    private static final Pattern MONEY_ANY =
            Pattern.compile("\\$?[ \\t]*([0-9][0-9,]*(?:\\.[0-9]+)?)[ \\t]*([kmb])?(?![a-z])");

    private static final Pattern DOLLARS =
            Pattern.compile("\\$[ \\t]*([0-9][0-9,]*(?:\\.[0-9]+)?)[ \\t]*([kmb])?(?![a-z])");

    private static final String[] NOT_PRICES =
            new String[] {"balance", "you have", "your money", "wallet", "bank"};

    private static final String SMALL_CAPS =
            "\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18\u0280\u0455\ua731\u1d1b\u1d1c\u1d20\u1d21\u028f\u1d22";

    private static final String SMALL_CAPS_PLAIN = "abcdefghijklmnoprsstuvwyz";

    private static final String[] ROMAN =
            new String[] {"i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x"};

    private ServerText() {}

    static String normalize(String string) {
        if (string == null) {
            return "";
        }
        String string2 = FORMATTING.matcher(string).replaceAll("");
        StringBuilder sb = new StringBuilder(string2.length());
        for (int i = 0; i < string2.length(); ++i) {
            char c = string2.charAt(i);
            int j = SMALL_CAPS.indexOf(c);
            if (j >= 0) {
                sb.append(SMALL_CAPS_PLAIN.charAt(j));
                continue;
            }
            if (c == '\u279c' || c == '\u2192') {
                sb.append('-');
                continue;
            }
            sb.append(c);
        }
        return sb.toString().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    static String normalize(Text text) {
        return text == null ? "" : ServerText.normalize(text.getString());
    }

    static String key(String string) {
        String string2 = ServerText.normalize(string);
        StringBuilder sb = new StringBuilder(string2.length());
        for (int i = 0; i < string2.length(); ++i) {
            char c = string2.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9')) continue;
            sb.append(c);
        }
        return sb.toString();
    }

    static long parseMoney(String string) {
        if (string == null) {
            return -1L;
        }
        Matcher matcher = MONEY_EXACT.matcher(string.trim().toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            return -1L;
        }
        return Math.round(ServerText.scale(matcher.group(1), matcher.group(2)));
    }

    static List<Double> moneyAll(String string) {
        ArrayList<Double> list = new ArrayList<Double>();
        if (string == null) {
            return list;
        }
        Matcher matcher = MONEY_ANY.matcher(ServerText.normalize(string));
        while (matcher.find()) {
            double scale = ServerText.scale(matcher.group(1), matcher.group(2));
            if (!(scale > 0.0)) continue;
            list.add(scale);
        }
        return list;
    }

    static List<Double> dollars(String string) {
        ArrayList<Double> list = new ArrayList<Double>();
        if (string == null) {
            return list;
        }
        Matcher matcher = DOLLARS.matcher(ServerText.normalize(string));
        while (matcher.find()) {
            double scale = ServerText.scale(matcher.group(1), matcher.group(2));
            if (!(scale > 0.0)) continue;
            list.add(scale);
        }
        return list;
    }

    static double askingPrice(List<String> list) {
        double d = -1.0;
        for (String string : list) {
            String string2 = ServerText.normalize(string);
            boolean bl = false;
            for (String string3 : NOT_PRICES) {
                if (!string2.contains(string3)) continue;
                bl = true;
                break;
            }
            if (bl) continue;
            List<Double> list2 =
                    string2.contains("$")
                            ? ServerText.dollars(string2)
                            : (string2.contains("price") || string2.contains("cost")
                                    ? ServerText.moneyAll(string2)
                                    : List.of());
            Iterator iterator = list2.iterator();
            while (iterator.hasNext()) {
                double e = (Double) iterator.next();
                if (!(e > d)) continue;
                d = e;
            }
        }
        return d;
    }

    static double askingPrice(ItemStack stack) {
        return ServerText.askingPrice(ServerText.lore(stack));
    }

    static String moneyToken(String string) {
        Matcher matcher = MONEY_ANY.matcher(ServerText.normalize(string));
        if (!matcher.find()) {
            return null;
        }
        String group = matcher.group(2);
        return matcher.group(1).replace(",", "") + (group == null ? "" : group);
    }

    static double displayStep(String string) {
        Matcher matcher = MONEY_ANY.matcher(ServerText.normalize(string));
        if (!matcher.find()) {
            return 1.0;
        }
        String group = matcher.group(1);
        int i = group.indexOf(46);
        int j = i < 0 ? 0 : group.length() - i - 1;
        return Math.max(1.0, ServerText.scale("1", matcher.group(2)) / Math.pow(10.0, j));
    }

    private static double scale(String string, String string2) {
        double d;
        try {
            d = Double.parseDouble(string.replace(",", ""));
        } catch (NumberFormatException exception) {
            return -1.0;
        }
        if (string2 == null) {
            return d;
        }
        return switch (string2) {
            case "k" -> d * 1000.0;
            case "m" -> d * 1000000.0;
            case "b" -> d * 1.0E9;
            default -> d;
        };
    }

    static String exactMoney(long l) {
        if (l >= 1000000000L && l % 1000000000L == 0L) {
            return l / 1000000000L + "b";
        }
        if (l >= 1000000L && l % 1000000L == 0L) {
            return l / 1000000L + "m";
        }
        if (l >= 1000000L && l % 100000L == 0L) {
            return l / 1000000L + "." + l % 1000000L / 100000L + "m";
        }
        if (l >= 1000L && l % 1000L == 0L) {
            return l / 1000L + "k";
        }
        return Long.toString(l);
    }

    static String shortMoney(double d) {
        if (d >= 1.0E9) {
            return ServerText.trim(d / 1.0E9) + "b";
        }
        if (d >= 1000000.0) {
            return ServerText.trim(d / 1000000.0) + "m";
        }
        if (d >= 1000.0) {
            return ServerText.trim(d / 1000.0) + "k";
        }
        return Long.toString(Math.round(d));
    }

    private static String trim(double d) {
        String string = String.format(Locale.ROOT, "%.2f", d);
        return string.contains(".") ? string.replaceAll("0+$", "").replaceAll("\\.$", "") : string;
    }

    static int roman(String string) {
        String string2 = ServerText.normalize(string);
        for (int i = 0; i < ROMAN.length; ++i) {
            if (!ROMAN[i].equals(string2)) continue;
            return i + 1;
        }
        return 0;
    }

    static String withoutRoman(String string) {
        String string2 = ServerText.normalize(string);
        int i = string2.lastIndexOf(32);
        if (i > 0 && ServerText.roman(string2.substring(i + 1)) > 0) {
            return string2.substring(0, i);
        }
        return string2;
    }

    static List<String> lore(ItemStack stack) {
        ArrayList<String> list = new ArrayList<String>();
        for (Text line :
                stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines()) {
            list.add(line.getString());
        }
        return list;
    }

    static double listingPrice(ItemStack stack) {
        List<String> list = ServerText.lore(stack);
        for (String string : list) {
            String withoutBullet = ServerText.withoutBullet(ServerText.normalize(string));
            if (!withoutBullet.startsWith("price")) continue;
            List<Double> list2 =
                    withoutBullet.contains("$")
                            ? ServerText.dollars(withoutBullet)
                            : ServerText.moneyAll(withoutBullet.substring("price".length()));
            return list2.isEmpty() ? -1.0 : list2.get(0);
        }
        double d = -1.0;
        for (String string2 : list) {
            String string3 = ServerText.normalize(string2);
            if (!string3.contains("$") || string3.contains("balance")) continue;
            for (double e : ServerText.dollars(string3)) {
                if (d > 0.0 && Math.abs(d - e) >= 1.0) {
                    return -1.0;
                }
                d = e;
            }
        }
        return d;
    }

    private static String withoutBullet(String string) {
        int i;
        for (i = 0; i < string.length() && !Character.isLetterOrDigit(string.charAt(i)); ++i) {}
        return string.substring(i);
    }

    static double orderPrice(ItemStack stack) {
        Object object;
        List<String> list = ServerText.lore(stack);
        double d = -1.0;
        for (String string : list) {
            object = ServerText.normalize(string);
            if (!string.contains("$")
                    || !((String) object).contains("price")
                            && !((String) object).contains("each")
                            && !((String) object).contains("per item")
                            && !((String) object).contains("pay")) continue;
            for (double e : ServerText.moneyAll(string)) {
                if (!(e > 0.0) || !(d < 0.0) && !(e < d)) continue;
                d = e;
            }
        }
        if (d > 0.0) {
            return d;
        }
        for (String string2 : list) {
            if (!string2.contains("$")
                    || ((java.util.List) (object = ServerText.moneyAll(string2))).isEmpty())
                continue;
            return (Double) ((java.util.List) object).get(0);
        }
        return -1.0;
    }
}
