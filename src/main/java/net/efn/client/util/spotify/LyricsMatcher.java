/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Environment(value = EnvType.CLIENT)
final class LyricsMatcher {

    static final double DURATION_TOLERANCE_SECONDS = 3.0;

    private static final int TITLE_EXACT = 40;

    private static final int TITLE_BASE = 30;

    private static final int TITLE_CONTAINS = 12;

    private static final int ARTIST_EXACT = 30;

    private static final int ARTIST_SHARED = 20;

    private static final int ARTIST_CONTAINS = 10;

    private static final int DURATION_MAX = 20;

    private static final int SYNCED = 25;

    private static final int INSTRUMENTAL = 8;

    private static final int PLAIN = 5;

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");

    private static final Pattern APOSTROPHES = Pattern.compile("['\u2019`\u00b4]");

    private static final Pattern BRACKETED =
            Pattern.compile(
                    "\\s*[(\\[][^)\\]]*\\b(feat|ft|featuring|with|remaster(ed)?|explicit|clean|mono|stereo|bonus|deluxe|anniversary|edition|single"
                        + " version|album version)\\b[^)\\]]*[)\\]]",
                    2);

    private static final Pattern DASHED =
            Pattern.compile(
                    "\\s+[-\u2013\u2014]\\s+[^-\u2013\u2014]*\\b(remaster(ed)?|explicit|clean|mono|stereo|bonus"
                        + " track|deluxe|anniversary|edition|single version|album version)\\b.*$",
                    2);

    private static final Pattern FEATURING =
            Pattern.compile("\\s+(feat\\.?|ft\\.|featuring)\\s+.*$", 2);

    private static final Pattern ARTIST_SPLIT =
            Pattern.compile(
                    "\\s*(?:,|;|/|&|\u2022|\\+|\\bx\\b|\\band\\b|\\bwith\\b|\\bfeat\\b\\.?|\\bft\\b\\.?|\\bfeaturing\\b)\\s*",
                    2);

    private static final Pattern CREDIT_SPLIT =
            Pattern.compile("\\s*[,;\u2022]\\s*|\\s+(?:&|x|feat\\.?|ft\\.?|featuring|with)\\s+", 2);

    private LyricsMatcher() {}

    static LrclibTrack best(List<LrclibTrack> list, LyricsQuery lyricsQuery) {
        if (list == null || list.isEmpty() || lyricsQuery == null) {
            return null;
        }
        LrclibTrack lrclibTrack = null;
        int i = -1;
        double mAX_VALUE = Double.MAX_VALUE;
        for (LrclibTrack lrclibTrack2 : list) {
            int score;
            if (lrclibTrack2 == null
                    || (score = LyricsMatcher.score(lrclibTrack2, lyricsQuery)) < 0) continue;
            double drift = LyricsMatcher.drift(lrclibTrack2, lyricsQuery);
            if (!(score > i || score == i && drift < mAX_VALUE)) continue;
            lrclibTrack = lrclibTrack2;
            i = score;
            mAX_VALUE = drift;
        }
        return lrclibTrack;
    }

    static int score(LrclibTrack lrclibTrack, LyricsQuery lyricsQuery) {
        if (!lrclibTrack.usable()) {
            return -1;
        }
        int durationScore = LyricsMatcher.durationScore(lrclibTrack, lyricsQuery);
        if (durationScore < 0) {
            return -1;
        }
        int titleScore = LyricsMatcher.titleScore(lyricsQuery.title(), lrclibTrack.trackName());
        if (titleScore <= 0) {
            return -1;
        }
        int artistScore = LyricsMatcher.artistScore(lyricsQuery.artist(), lrclibTrack.artistName());
        if (artistScore <= 0) {
            return -1;
        }
        int i = lrclibTrack.hasSynced() ? 25 : (lrclibTrack.hasPlain() ? 5 : 8);
        return titleScore + artistScore + durationScore + i;
    }

    private static int durationScore(LrclibTrack lrclibTrack, LyricsQuery lyricsQuery) {
        if (!lyricsQuery.hasDuration() || lrclibTrack.duration() <= 0.0) {
            return 0;
        }
        double d = Math.abs(lrclibTrack.duration() - (double) lyricsQuery.durationSeconds());
        if (d > 3.0) {
            return -1;
        }
        return (int) Math.round(20.0 * (1.0 - d / 6.0));
    }

    private static double drift(LrclibTrack lrclibTrack, LyricsQuery lyricsQuery) {
        if (!lyricsQuery.hasDuration() || lrclibTrack.duration() <= 0.0) {
            return 3.0;
        }
        return Math.abs(lrclibTrack.duration() - (double) lyricsQuery.durationSeconds());
    }

    static int titleScore(String string, String string2) {
        String string3 = LyricsMatcher.normalize(string);
        String string4 = LyricsMatcher.normalize(string2);
        if (string3.isEmpty() || string4.isEmpty()) {
            return 0;
        }
        if (string3.equals(string4)) {
            return 40;
        }
        String string5 = LyricsMatcher.normalize(LyricsMatcher.baseTitle(string));
        String string6 = LyricsMatcher.normalize(LyricsMatcher.baseTitle(string2));
        if (!string5.isEmpty() && string5.equals(string6)) {
            return 30;
        }
        if (LyricsMatcher.containsWords(string5, string6)
                || LyricsMatcher.containsWords(string6, string5)) {
            return 12;
        }
        return 0;
    }

    static int artistScore(String string, String string2) {
        String string3 = LyricsMatcher.normalize(string);
        String string4 = LyricsMatcher.normalize(string2);
        if (string3.isEmpty() || string4.isEmpty()) {
            return 0;
        }
        if (string3.equals(string4)) {
            return 30;
        }
        Set<String> set = LyricsMatcher.artists(string);
        Set<String> set2 = LyricsMatcher.artists(string2);
        for (String string5 : set) {
            if (!set2.contains(string5)) continue;
            return 20;
        }
        String string6 = set.isEmpty() ? string3 : set.iterator().next();
        String string7 = string6;
        if (LyricsMatcher.containsWords(string4, string6)
                || LyricsMatcher.containsWords(string3, string4)) {
            return 10;
        }
        return 0;
    }

    static String baseTitle(String string) {
        if (string == null) {
            return "";
        }
        String string2 = BRACKETED.matcher(string).replaceAll("");
        string2 = DASHED.matcher(string2).replaceFirst("");
        string2 = FEATURING.matcher(string2).replaceFirst("");
        return (string2 = string2.strip()).isEmpty() ? string.strip() : string2;
    }

    static String primaryArtist(String string) {
        String[] strings;
        if (string == null) {
            return "";
        }
        for (String string2 : strings = CREDIT_SPLIT.split(string.strip())) {
            if (string2.isBlank()) continue;
            return string2.strip();
        }
        return string.strip();
    }

    static String normalize(String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        String string2 =
                string.toLowerCase(Locale.ROOT)
                        .replace('\u0131', 'i')
                        .replace("\u00df", "ss")
                        .replace("\u00e6", "ae")
                        .replace("\u0153", "oe")
                        .replace('\u00f8', 'o')
                        .replace('\u0142', 'l')
                        .replace('\u0111', 'd')
                        .replace("&", " and ");
        string2 = MARKS.matcher(Normalizer.normalize(string2, Normalizer.Form.NFD)).replaceAll("");
        string2 = APOSTROPHES.matcher(string2).replaceAll("");
        return NON_WORD.matcher(string2).replaceAll(" ").strip();
    }

    private static Set<String> artists(String string) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (String string2 : ARTIST_SPLIT.split(string)) {
            String string3 = LyricsMatcher.normalize(string2);
            if (string3.isEmpty()) continue;
            set.add(string3);
        }
        return set;
    }

    private static boolean containsWords(String string, String string2) {
        if (string2.length() < 3 || string.length() < string2.length()) {
            return false;
        }
        return (" " + string + " ").contains(" " + string2 + " ");
    }

    static List<String[]> searchTerms(LyricsQuery lyricsQuery) {
        ArrayList<String[]> list = new ArrayList<String[]>(2);
        list.add(new String[] {lyricsQuery.title(), lyricsQuery.artist()});
        String baseTitle = LyricsMatcher.baseTitle(lyricsQuery.title());
        String primaryArtist = LyricsMatcher.primaryArtist(lyricsQuery.artist());
        if (!baseTitle.equals(lyricsQuery.title()) || !primaryArtist.equals(lyricsQuery.artist())) {
            list.add(new String[] {baseTitle, primaryArtist});
        }
        return list;
    }
}
