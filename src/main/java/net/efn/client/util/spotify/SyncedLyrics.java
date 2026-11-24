/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(value = EnvType.CLIENT)
public final class SyncedLyrics {

    public static final SyncedLyrics EMPTY = new SyncedLyrics(new Line[0]);

    private static final Pattern TIMESTAMP =
            Pattern.compile("(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?");

    private static final Pattern WORD_TIMESTAMP =
            Pattern.compile("<(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?>");

    private static final Pattern METADATA = Pattern.compile("[A-Za-z#][A-Za-z0-9_ ]*:.*", 32);

    private static final Pattern OFFSET = Pattern.compile("offset\\s*:\\s*([+-]?\\d{1,7})\\s*");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final long WORD_EARLY_MS = 1000L;

    private final Line[] lines;

    private final long[] times;

    private SyncedLyrics(Line[] lines2) {
        this.lines = lines2;
        this.times = new long[lines2.length];
        for (int i = 0; i < lines2.length; ++i) {
            this.times[i] = lines2[i].timeMs();
        }
    }

    public static SyncedLyrics parse(String string) {
        if (string == null || string.isBlank()) {
            return EMPTY;
        }
        ArrayList<Line> list = new ArrayList<Line>();
        ArrayList<Long> list2 = new ArrayList<Long>(4);
        int j = 0;
        for (String string2 : string.split("\r\n|\r|\n")) {
            Object object;
            String string3;
            int k;
            String string4 = string2.strip();
            if (!string4.isEmpty() && string4.charAt(0) == '\ufeff') {
                string4 = string4.substring(1).strip();
            }
            list2.clear();
            int m = 0;
            while (m < string4.length()
                    && string4.charAt(m) == '['
                    && (k = string4.indexOf(93, m + 1)) >= 0) {
                string3 = string4.substring(m + 1, k).strip();
                object = TIMESTAMP.matcher(string3);
                if (((Matcher) object).matches()) {
                    list2.add(SyncedLyrics.toMillis((Matcher) object));
                } else {
                    if (!METADATA.matcher(string3).matches()) break;
                    Matcher matcher = OFFSET.matcher(string3.toLowerCase(Locale.ROOT));
                    if (matcher.matches()) {
                        j = Integer.parseInt(matcher.group(1));
                    }
                }
                for (m = k + 1; m < string4.length() && string4.charAt(m) == ' '; ++m) {}
            }
            if (list2.isEmpty()) continue;
            string3 = string4.substring(m);
            object = SyncedLyrics.cleanText(string3);
            long l = (Long) Collections.min(list2);
            List<Long> list3 = SyncedLyrics.wordTimes(string3, (String) object, l);
            Iterator iterator = list2.iterator();
            while (iterator.hasNext()) {
                long l2 = (Long) iterator.next();
                list.add(new Line(l2, (String) object, SyncedLyrics.shift(list3, l2 - l)));
            }
        }
        if (list.isEmpty()) {
            return EMPTY;
        }
        Object[] objects = list.toArray(new Line[0]);
        if (j != 0) {
            for (int i = 0; i < objects.length; ++i) {
                Object object2 = objects[i];
                objects[i] =
                        new Line(
                                Math.max(0L, ((Line) object2).timeMs() - (long) j),
                                ((Line) object2).text(),
                                SyncedLyrics.shift(((Line) object2).wordTimes(), -j));
            }
        }
        Arrays.sort((Line[]) objects, Comparator.comparingLong(Line::timeMs));
        return new SyncedLyrics((Line[]) objects);
    }

    public int indexAt(long l) {
        int i = 0;
        int j = this.times.length;
        while (i < j) {
            int k = i + j >>> 1;
            if (this.times[k] <= l) {
                i = k + 1;
                continue;
            }
            j = k;
        }
        return i - 1;
    }

    public int firstWithTimeOf(int i) {
        if (i < 0 || i >= this.times.length) {
            return i;
        }
        long time = this.times[i];
        while (i > 0 && this.times[i - 1] == time) {
            --i;
        }
        return i;
    }

    public int lastWithTimeOf(int i) {
        if (i < 0 || i >= this.times.length) {
            return i;
        }
        long time = this.times[i];
        while (i + 1 < this.times.length && this.times[i + 1] == time) {
            ++i;
        }
        return i;
    }

    public Line line(int i) {
        return this.lines[i];
    }

    public int size() {
        return this.lines.length;
    }

    public boolean isEmpty() {
        return this.lines.length == 0;
    }

    public List<String> texts() {
        ArrayList<String> list = new ArrayList<String>(this.lines.length);
        for (Line line : this.lines) {
            if (line.isGap()) continue;
            list.add(line.text());
        }
        return List.copyOf(list);
    }

    private static long toMillis(Matcher matcher) {
        long l = Long.parseLong(matcher.group(1));
        long l2 = Long.parseLong(matcher.group(2));
        String group = matcher.group(3);
        long l3 = 0L;
        if (group != null) {
            l3 = Long.parseLong(group);
            if (group.length() == 1) {
                l3 *= 100L;
            } else if (group.length() == 2) {
                l3 *= 10L;
            }
        }
        return l * 60000L + l2 * 1000L + l3;
    }

    private static String cleanText(String string) {
        if (string.indexOf(60) >= 0) {
            string = WORD_TIMESTAMP.matcher(string).replaceAll("");
        }
        return WHITESPACE.matcher(string).replaceAll(" ").strip();
    }

    private static List<Long> wordTimes(String string, String string2, long l) {
        int i;
        int j;
        if (string.indexOf(60) < 0 || string2.isEmpty()) {
            return List.of();
        }
        ArrayList<Long> list = new ArrayList<Long>();
        Matcher matcher = WORD_TIMESTAMP.matcher(string);
        long l2 = -1L;
        boolean bl = false;
        int k = 0;
        while (k < string.length()) {
            if (string.charAt(k) == '<' && matcher.region(k, string.length()).lookingAt()) {
                k = matcher.end();
                int m = j = bl && k < string.length() && string.charAt(k) > ' ' ? 1 : 0;
                if (j != 0) continue;
                l2 = SyncedLyrics.toMillis(matcher);
                continue;
            }
            if ((i = string.charAt(k++)) <= 32) {
                bl = false;
                continue;
            }
            if (bl) continue;
            bl = true;
            if (l2 < 0L) {
                return List.of();
            }
            if (list.isEmpty() ? l2 < l - 1000L : l2 < (Long) list.getLast()) {
                return List.of();
            }
            list.add(l2);
            l2 = -1L;
        }
        i = 1;
        for (j = 0; j < string2.length(); ++j) {
            if (string2.charAt(j) != ' ') continue;
            ++i;
        }
        return list.size() == i ? list : List.of();
    }

    private static List<Long> shift(List<Long> list, long l) {
        if (l == 0L || list.isEmpty()) {
            return list;
        }
        ArrayList<Long> list2 = new ArrayList<Long>(list.size());
        for (long l2 : list) {
            list2.add(Math.max(0L, l2 + l));
        }
        return list2;
    }

    @Environment(value = EnvType.CLIENT)
    public record Line(long timeMs, String text, List<Long> wordTimes) {

        public Line {
            text = text == null ? "" : text;
            wordTimes = wordTimes == null ? List.of() : List.copyOf(wordTimes);
        }

        public boolean isGap() {
            return this.text.isEmpty();
        }
    }
}
