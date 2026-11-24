package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.function.IntPredicate;

@Environment(EnvType.CLIENT)
public final class DisplayText {

    private DisplayText() {}

    public static String fold(String string) {
        if (string != null && !string.isEmpty()) {
            StringBuilder sb = null;
            boolean bl = false;
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                String replacement = replacement(c);
                if (replacement != null || c == ' ' && bl) {
                    if (sb == null) {
                        sb = new StringBuilder(string.length()).append(string, 0, i);
                    }
                    if (replacement != null) {
                        for (int j = 0; j < replacement.length(); j++) {
                            char c2 = replacement.charAt(j);
                            if (c2 != ' ' || !bl && !sb.isEmpty()) {
                                sb.append(c2);
                                bl = c2 == ' ';
                            }
                        }
                    }
                } else {
                    if (sb != null) {
                        sb.append(c);
                    }
                    bl = c == ' ';
                }
            }
            if (sb == null) {
                String string2 = string.strip();
                return string2.length() == string.length() ? string : string2;
            } else {
                return sb.toString().strip();
            }
        } else {
            return "";
        }
    }

    public static String fold(String string, IntPredicate intPredicate) {
        String fold = fold(string);
        StringBuilder sb = null;
        int j = 0;
        while (j < fold.length()) {
            int k = fold.codePointAt(j);
            int m = j + Character.charCount(k);
            String string2 =
                    k >= 128 && Character.isLetter(k) && !intPredicate.test(k)
                            ? baseLetters(k)
                            : null;
            if (string2 != null
                    && !string2.codePoints().allMatch(i -> i < 128 || intPredicate.test(i))) {
                string2 = null;
            }
            if (string2 != null && sb == null) {
                sb = new StringBuilder(fold.length()).append(fold, 0, j);
            }
            if (sb != null) {
                if (string2 != null) {
                    sb.append(string2);
                } else {
                    sb.append(fold, j, m);
                }
            }
            j = m;
        }
        return sb == null ? fold : sb.toString();
    }

    static String baseLetters(int i) {
        String string =
                switch (i) {
                    case 272 -> "D";
                    case 273 -> "d";
                    case 294 -> "H";
                    case 295 -> "h";
                    case 306 -> "IJ";
                    case 307 -> "ij";
                    case 319 -> "L";
                    case 320 -> "l";
                    case 321 -> "L";
                    case 322 -> "l";
                    case 338 -> "OE";
                    case 339 -> "oe";
                    case 358 -> "T";
                    case 359 -> "t";
                    case 383 -> "s";
                    default -> null;
                };
        if (string != null) {
            return string;
        } else {
            String string2 = Normalizer.normalize(new String(Character.toChars(i)), Form.NFD);
            StringBuilder sb = new StringBuilder(string2.length());
            string2.codePoints()
                    .filter(ix -> Character.getType(ix) != 6)
                    .forEach(sb::appendCodePoint);
            return !sb.isEmpty() && sb.length() != string2.length() ? sb.toString() : null;
        }
    }

    private static String replacement(char c) {
        return switch (c) {
            case '\t',
                            '\u00a0',
                            '\u2000',
                            '\u2001',
                            '\u2002',
                            '\u2003',
                            '\u2004',
                            '\u2005',
                            '\u2006',
                            '\u2007',
                            '\u2008',
                            '\u2009',
                            '\u200a',
                            '\u202f',
                            '\u205f',
                            '\u3000' ->
                    " ";
            case '\u200b',
                            '\u200c',
                            '\u200d',
                            '\u2060',
                            '\u2669',
                            '\u266a',
                            '\u266b',
                            '\u266c',
                            '\ufeff' ->
                    "";
            case '\u2010', '\u2011', '\u2012', '\u2013', '\u2014', '\u2015', '\u2212' -> "-";
            case '\u2018', '\u2019', '\u201a', '\u201b', '\u2032' -> "'";
            case '\u201c', '\u201d', '\u201e', '\u201f', '\u2033' -> "\"";
            case '\u2022', '\u2027', '\u2219' -> "\u00b7";
            case '\u2026' -> "...";
            default -> null;
        };
    }
}
