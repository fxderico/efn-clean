package net.efn.client.util.staff;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class StaffMatcher {

    public static final String DEFAULT_RANK_WORDS =
            "owner, co-owner, manager, admin, administrator, developer, dev, sr-admin, sr-mod,"
                    + " senior-mod, moderator, mod, trial-mod, sr-helper, senior-helper, helper,"
                    + " trial-helper, support, staff";

    public static final String DEFAULT_MARKERS =
            "U+2605 U+2606 U+2726 U+2727 U+272A U+272F U+2730 U+2B50";

    public static final String MODE_ALL = "Ranks + Markers + Team";

    public static final String MODE_RANKS = "Ranks";

    public static final String MODE_MARKERS = "Markers";

    public static final String MODE_TEAM = "Team Rank";

    public static final int DEFAULT_TEAM_RANK_MAX = 5;

    private static final String GENERIC_RANK = "Staff";

    private static final Pattern CODEPOINT = Pattern.compile("(?i)(?:u\\+|\\\\u)([0-9a-f]{4,6})");

    private static final Pattern TEAM_RANK = Pattern.compile("^(\\d{2})-");

    private static final int MAX_SPAN = 3;

    private static final int TEAM_RANK_SLOTS = 100;

    private static final Map<String, String> LABELS =
            Map.ofEntries(
                    Map.entry("owner", "Owner"),
                    Map.entry("coowner", "Co-Owner"),
                    Map.entry("manager", "Manager"),
                    Map.entry("admin", "Admin"),
                    Map.entry("administrator", "Admin"),
                    Map.entry("sradmin", "Sr. Admin"),
                    Map.entry("developer", "Dev"),
                    Map.entry("dev", "Dev"),
                    Map.entry("srmod", "Sr. Mod"),
                    Map.entry("seniormod", "Sr. Mod"),
                    Map.entry("moderator", "Mod"),
                    Map.entry("mod", "Mod"),
                    Map.entry("trialmod", "Trial Mod"),
                    Map.entry("srhelper", "Sr. Helper"),
                    Map.entry("seniorhelper", "Sr. Helper"),
                    Map.entry("helper", "Helper"),
                    Map.entry("trialhelper", "Trial Helper"),
                    Map.entry("support", "Support"),
                    Map.entry("staff", "Staff"),
                    Map.entry("builder", "Builder"));

    private static final Map<Integer, Character> SMALL_CAPS = smallCaps();

    private static final char SMALL_CAPS_Q = '\u01eb';

    private final boolean useRanks;

    private final boolean useMarkers;

    private final boolean useTeamRank;

    private final int teamRankMax;

    private final Map<String, Integer> wordIndex = new HashMap<>();

    private final List<StaffMatcher.Word> words = new ArrayList<>();

    private final Set<Integer> markers;

    private final boolean iconGlyphs;

    private final Set<String> knownNames;

    private final Set<String> ignoredNames;

    public StaffMatcher(
            String string,
            String string2,
            String string3,
            boolean iconGlyphs2,
            int teamRankMax2,
            String string4,
            String string5) {
        this.useRanks = "Ranks + Markers + Team".equals(string) || "Ranks".equals(string);
        this.useMarkers = "Ranks + Markers + Team".equals(string) || "Markers".equals(string);
        this.useTeamRank = "Ranks + Markers + Team".equals(string) || "Team Rank".equals(string);
        this.teamRankMax = teamRankMax2;
        this.iconGlyphs = iconGlyphs2;
        for (String string6 : string2.split(",")) {
            String string7 = string6.trim();
            String string8 = fold(string7).replace(" ", "");
            if (!string8.isEmpty() && !this.wordIndex.containsKey(string8)) {
                this.wordIndex.put(string8, this.words.size());
                this.words.add(
                        new StaffMatcher.Word(
                                string8, LABELS.getOrDefault(string8, capitalize(string7))));
            }
        }
        this.markers = parseMarkers(string3);
        this.knownNames = parseNames(string4);
        this.ignoredNames = parseNames(string5);
    }

    public StaffMatcher.Match match(String string, String string2, String... string3) {
        if (string != null && !string.isEmpty()) {
            String string4 = string.toLowerCase(Locale.ROOT);
            if (this.ignoredNames.contains(string4)) {
                return null;
            } else {
                StringBuilder sb = new StringBuilder();
                for (String string5 : string3) {
                    if (string5 != null && !string5.isEmpty()) {
                        sb.append(string5).append(' ');
                    }
                }
                if (string2 != null && !string2.isEmpty()) {
                    sb.append(string2).append(' ');
                }
                String withoutName = withoutName(stripLegacyCodes(sb.toString()), string);
                StaffMatcher.Word word = this.useRanks ? this.bestRank(withoutName) : null;
                if (word != null) {
                    return new StaffMatcher.Match(word.label(), this.wordIndex.get(word.folded()));
                } else {
                    int i = this.useTeamRank ? teamRank(string2) : -1;
                    if (i >= 0 && i <= this.teamRankMax) {
                        return new StaffMatcher.Match(teamRankLabel(i), this.words.size() + i);
                    } else {
                        return (!this.useMarkers || !this.hasMarker(withoutName))
                                        && !this.knownNames.contains(string4)
                                ? null
                                : new StaffMatcher.Match("Staff", this.words.size() + 100);
                    }
                }
            }
        } else {
            return null;
        }
    }

    public static int teamRank(String string) {
        if (string != null && string.length() >= 3) {
            Matcher matcher = TEAM_RANK.matcher(string);
            return matcher.lookingAt() ? Integer.parseInt(matcher.group(1)) : -1;
        } else {
            return -1;
        }
    }

    public static String teamRankLabel(int i) {
        return i < 10 ? "0" + i : Integer.toString(i);
    }

    private StaffMatcher.Word bestRank(String string) {
        if (this.words.isEmpty()) {
            return null;
        } else {
            List list = tokens(string);
            int i = 2147483647;
            int j = 0;
            while (j < list.size()) {
                int k = 0;
                for (int m = Math.min(3, list.size() - j); m >= 1; m--) {
                    Integer integer = this.wordIndex.get(String.join("", list.subList(j, j + m)));
                    if (integer != null) {
                        i = Math.min(i, integer);
                        k = m;
                        break;
                    }
                }
                j += Math.max(1, k);
            }
            return i == 2147483647 ? null : this.words.get(i);
        }
    }

    static List<String> tokens(String string) {
        String fold = fold(string);
        ArrayList list = new ArrayList();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fold.length(); i++) {
            char c = fold.charAt(i);
            if (c >= 'a' && c <= 'z') {
                sb.append(c);
            } else if (!sb.isEmpty()) {
                list.add(sb.toString());
                sb.setLength(0);
            }
        }
        if (!sb.isEmpty()) {
            list.add(sb.toString());
        }
        return list;
    }

    static String stripLegacyCodes(String string) {
        if (string.indexOf(167) < 0) {
            return string;
        } else {
            StringBuilder sb = new StringBuilder(string.length());
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                if (c == 167) {
                    i++;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    static String fold(String string) {
        String string2 = Normalizer.normalize(string.replace('\u01eb', 'q'), Form.NFKD);
        StringBuilder sb = new StringBuilder(string2.length());
        int i = 0;
        while (i < string2.length()) {
            int j = string2.codePointAt(i);
            if (Character.getType(j) != 6) {
                Character ch = SMALL_CAPS.get(j);
                if (ch != null) {
                    sb.append(ch.charValue());
                } else {
                    int k = Character.toLowerCase(j);
                    sb.append(k >= 97 && k <= 122 ? (char) k : ' ');
                }
            }
            i += Character.charCount(j);
        }
        return sb.toString();
    }

    private boolean hasMarker(String string) {
        int i = 0;
        while (i < string.length()) {
            int j = string.codePointAt(i);
            if (this.markers.contains(j) || this.iconGlyphs && isPrivateUse(j)) {
                return true;
            }
            i += Character.charCount(j);
        }
        return false;
    }

    public static boolean isPrivateUse(int i) {
        return i >= 57344 && i <= 63743
                || i >= 983040 && i <= 1048573
                || i >= 1048576 && i <= 1114109;
    }

    static Set<Integer> parseMarkers(String string) {
        HashSet set = new HashSet();
        for (String string2 : string.split("[,\\s]+")) {
            if (!string2.isEmpty()) {
                Matcher matcher = CODEPOINT.matcher(string2);
                if (matcher.matches()) {
                    int i = Integer.parseInt(matcher.group(1), 16);
                    if (Character.isValidCodePoint(i)) {
                        set.add(i);
                    }
                } else {
                    string2.codePoints().forEach(set::add);
                }
            }
        }
        return set;
    }

    private static Set<String> parseNames(String string) {
        HashSet set = new HashSet();
        for (String string2 : string.split("[,\\s]+")) {
            if (!string2.isEmpty()) {
                set.add(string2.toLowerCase(Locale.ROOT));
            }
        }
        return set;
    }

    private static String withoutName(String string, String string2) {
        String string3 = string.toLowerCase(Locale.ROOT);
        String string4;
        if (!string3.contains(string4 = string2.toLowerCase(Locale.ROOT))) {
            return string;
        } else {
            StringBuilder sb = new StringBuilder(string.length());
            int i = 0;
            for (int j = string3.indexOf(string4); j >= 0; j = string3.indexOf(string4, i)) {
                sb.append(string, i, j).append(' ');
                i = j + string4.length();
            }
            return sb.append(string, i, string.length()).toString();
        }
    }

    private static String capitalize(String string) {
        String string2 = string.trim();
        return string2.isEmpty()
                ? "Staff"
                : string2.substring(0, 1).toUpperCase(Locale.ROOT) + string2.substring(1);
    }

    private static Map<Integer, Character> smallCaps() {
        int[] ints =
                new int[] {
                    7424, 665, 7428, 7429, 7431, 42800, 610, 668, 618, 7434, 7435, 671, 7437, 628,
                    7439, 7448, 42927, 640, 42801, 7451, 7452, 7456, 7457, 655, 7458
                };
        String string = "abcdefghijklmnopqrstuvwyz";
        HashMap map = new HashMap();
        for (int i = 0; i < ints.length; i++) {
            map.put(ints[i], string.charAt(i));
        }
        return Map.copyOf(map);
    }

    @Environment(EnvType.CLIENT)
    public record Match(String rank, int seniority) {}

    @Environment(EnvType.CLIENT)
    private record Word(String folded, String label) {}
}
