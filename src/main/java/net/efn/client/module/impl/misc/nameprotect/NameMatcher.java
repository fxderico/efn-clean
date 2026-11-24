package net.efn.client.module.impl.misc.nameprotect;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

@Environment(EnvType.CLIENT)
public final class NameMatcher {

    public static final NameMatcher EMPTY = new NameMatcher(Map.of());

    public static final int MAX_NAME_LENGTH = 32;

    private final Map<String, String> mapping;

    private final String[] names;

    private final String[] aliases;

    private final int mask;

    private final long lengths;

    public NameMatcher(Map<String, String> map) {
        HashMap map2 = new HashMap();
        ArrayList list = new ArrayList(map.size());
        ArrayList list2 = new ArrayList(map.size());
        for (Entry entry : map.entrySet()) {
            String key = (String) entry.getKey();
            String entry2 = (String) entry.getValue();
            if (key != null
                    && !key.isEmpty()
                    && key.length() <= 32
                    && entry2 != null
                    && map2.putIfAbsent(key.toLowerCase(Locale.ROOT), entry2) == null) {
                list.add(key);
                list2.add(entry2);
            }
        }
        int i = Integer.highestOneBit(Math.max(1, list.size()) * 2) * 2;
        String[] strings = new String[i];
        String[] strings2 = new String[i];
        long l = 0L;
        for (int j = 0; j < list.size(); j++) {
            String string = (String) list.get(j);
            int k = hash(string, 0, string.length()) & i - 1;
            while (strings[k] != null) {
                k = k + 1 & i - 1;
            }
            strings[k] = string;
            strings2[k] = (String) list2.get(j);
            l |= 1L << string.length();
        }
        this.mapping = Map.copyOf(map2);
        this.names = strings;
        this.aliases = strings2;
        this.mask = i - 1;
        this.lengths = l;
    }

    public boolean isEmpty() {
        return this.mapping.isEmpty();
    }

    public boolean sameMapping(NameMatcher nameMatcher) {
        return nameMatcher != null && this.mapping.equals(nameMatcher.mapping);
    }

    public static boolean isNameChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
    }

    public static String nameCore(String string) {
        if (string == null) {
            return null;
        } else {
            int i = string.length();
            int j = 0;
            while (j < i && !isNameChar(string.charAt(j))) {
                j++;
            }
            while (i > j && !isNameChar(string.charAt(i - 1))) {
                i--;
            }
            if (j != i && i - j <= 32) {
                for (int k = j; k < i; k++) {
                    if (!isNameChar(string.charAt(k))) {
                        return null;
                    }
                }
                return string.substring(j, i);
            } else {
                return null;
            }
        }
    }

    public String aliasAt(String string, int i, int j) {
        int k = j - i;
        if (k > 0 && k <= 32 && (this.lengths & 1L << k) != 0L) {
            String name;
            for (int m = hash(string, i, j) & this.mask;
                    (name = this.names[m]) != null;
                    m = m + 1 & this.mask) {
                if (name.length() == k && name.regionMatches(true, 0, string, i, k)) {
                    return this.aliases[m];
                }
            }
            return null;
        } else {
            return null;
        }
    }

    public String replace(String string) {
        if (string != null && !string.isEmpty() && !this.isEmpty()) {
            List list = this.scan(string, new int[] {0, string.length()}, 1);
            if (list.isEmpty()) {
                return string;
            } else {
                StringBuilder sb = new StringBuilder(string.length() + 16);
                int i = 0;
                for (NameMatcher.Match match : (Iterable<NameMatcher.Match>) list) {
                    sb.append(string, i, match.start()).append(match.alias());
                    i = match.end();
                }
                return sb.append(string, i, string.length()).toString();
            }
        } else {
            return string;
        }
    }

    public boolean mentionsAny(String string) {
        return string != null
                && !string.isEmpty()
                && !this.isEmpty()
                && !this.scan(string, new int[] {0, string.length()}, 1).isEmpty();
    }

    public NameMatcher newNamesSince(NameMatcher nameMatcher) {
        HashMap map = null;
        for (Entry entry : this.mapping.entrySet()) {
            if (nameMatcher == null
                    || !((String) entry.getValue())
                            .equals(nameMatcher.mapping.get(entry.getKey()))) {
                if (map == null) {
                    map = new HashMap();
                }
                map.put((String) entry.getKey(), (String) entry.getValue());
            }
        }
        return map != null ? new NameMatcher(map) : EMPTY;
    }

    public List<NameMatcher.Match> scan(String string, int[] ints, int i) {
        if (!this.isEmpty() && !string.isEmpty()) {
            List list = null;
            int j = string.length();
            int k = 0;
            int m = 0;
            while (m < j) {
                while (k < i - 1 && ints[k + 1] <= m) {
                    k++;
                }
                int n = ints[k + 1];
                char c = string.charAt(m);
                if (c == 167) {
                    m += m + 1 < n ? 2 : 1;
                } else if (!isNameChar(c)) {
                    m++;
                } else {
                    int o = m + 1;
                    while (o < j && isNameChar(string.charAt(o))) {
                        o++;
                    }
                    String alias2 = this.aliasAt(string, m, o);
                    if (alias2 != null) {
                        int bedrockPrefix = bedrockPrefix(string, m);
                        list =
                                add(
                                        list,
                                        bedrockPrefix,
                                        o,
                                        bedrockPrefix == m
                                                ? alias2
                                                : string.substring(bedrockPrefix, m)
                                                                .replace(".", "")
                                                        + alias2);
                    } else if (n < o) {
                        for (int q = k + 1; q < i && ints[q] < o; q++) {
                            int p = ints[q];
                            String alias3 = this.aliasAt(string, p, o);
                            if (alias3 != null) {
                                list = add(list, p, o, alias3);
                                break;
                            }
                        }
                    }
                    m = o;
                }
            }
            return list != null ? list : List.of();
        } else {
            return List.of();
        }
    }

    private static int bedrockPrefix(String string, int i) {
        int j = i;
        int k = i;
        while (true) {
            while (j >= 2 && string.charAt(j - 2) == 167) {
                j -= 2;
            }
            if (j < 1 || string.charAt(j - 1) != '.') {
                if (k == i) {
                    return i;
                } else {
                    boolean bl =
                            k >= 1
                                    && isNameChar(string.charAt(k - 1))
                                    && (k < 2 || string.charAt(k - 2) != 167);
                    return bl ? i : k;
                }
            }
            k = --j;
        }
    }

    private static List<NameMatcher.Match> add(
            List<NameMatcher.Match> list, int i, int j, String string) {
        List<NameMatcher.Match> list2 = list != null ? list : new ArrayList<>(4);
        list2.add(new NameMatcher.Match(i, j, string));
        return (List<NameMatcher.Match>) list2;
    }

    private static int hash(String string, int i, int j) {
        int k = 0;
        for (int m = i; m < j; m++) {
            char c = string.charAt(m);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + ' ');
            }
            k = 31 * k + c;
        }
        return k ^ k >>> 16;
    }

    @Environment(EnvType.CLIENT)
    public record Match(int start, int end, String alias) {}
}
