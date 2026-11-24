package net.efn.client.util.antivanish;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class AntiVanishText {

    private AntiVanishText() {}

    public static String normalize(String string) {
        if (string != null && !string.isBlank()) {
            String string2 =
                    Normalizer.normalize(stripLegacyCodes(string), Form.NFKD)
                            .toLowerCase(Locale.ROOT);
            StringBuilder sb = new StringBuilder(string2.length());
            boolean bl = true;
            int i = 0;
            while (i < string2.length()) {
                int j = string2.codePointAt(i);
                if (!isMark(j) && !isFormatCode(j)) {
                    int foldConfusable = foldConfusable(j);
                    if (foldConfusable >= 97 && foldConfusable <= 122
                            || foldConfusable >= 48 && foldConfusable <= 57) {
                        sb.appendCodePoint(foldConfusable);
                        bl = false;
                    }
                }
                i += Character.charCount(j);
            }
            i = sb.length();
            if (i > 0 && sb.charAt(i - 1) == ' ') {
                sb.setLength(i - 1);
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    public static boolean containsPlayerName(String string, String string2) {
        String string3 = stripLegacyCodes(string == null ? "" : string).toLowerCase(Locale.ROOT);
        String string4 =
                stripLegacyCodes(string2 == null ? "" : string2).trim().toLowerCase(Locale.ROOT);
        if (string4.matches("[a-z0-9_]{1,16}")) {
            int i = 0;
            while (i <= string3.length() - string4.length()) {
                int j = string3.indexOf(string4, i);
                if (j < 0) {
                    return false;
                }
                int k = j + string4.length();
                boolean bl = j == 0 || !isUsernameCharacter(string3.charAt(j - 1));
                boolean bl2 = k == string3.length() || !isUsernameCharacter(string3.charAt(k));
                if (bl && bl2) {
                    return true;
                }
                i = j + 1;
            }
            return false;
        } else {
            String string5 = normalize(string);
            String string6 = normalize(string2);
            return !string5.isBlank() && !string6.isBlank()
                    ? string5.equals(string6) || (" " + string5 + " ").contains(" " + string6 + " ")
                    : false;
        }
    }

    public static boolean looksLikeLeaveMessage(String string, String string2) {
        if (!containsPlayerName(string, string2)) {
            return false;
        } else {
            String string3 = " " + normalize(string) + " ";
            String string4 = normalize(string2);
            if (string4.isBlank()) {
                return false;
            } else {
                String string5 = " " + string4 + " ";
                return string3.contains(string5 + "left the game ")
                        || string3.contains(string5 + "left ")
                        || string3.contains(string5 + "has left ")
                        || string3.contains(string5 + "quit ")
                        || string3.contains(string5 + "has quit ")
                        || string3.contains(string5 + "disconnected ")
                        || string3.contains(string5 + "logged out ");
            }
        }
    }

    public static boolean isPlausiblePlayerName(String string) {
        if (string == null) {
            return false;
        } else {
            String string2 = string.trim();
            int i = string2.length();
            return i >= 3 && i <= 16 ? string2.indexOf(32) < 0 : false;
        }
    }

    public static boolean isUsername(String string) {
        if (string == null) {
            return false;
        } else {
            int i = string.length();
            if (i >= 2 && i <= 32) {
                boolean bl = false;
                for (int j = 0; j < i; j++) {
                    char c = string.charAt(j);
                    boolean bl2 =
                            c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9';
                    if (bl2) {
                        bl = true;
                    } else if (c != '_' && c != '.' && c != ' ') {
                        return false;
                    }
                }
                return bl;
            } else {
                return false;
            }
        }
    }

    private static boolean isUsernameCharacter(char c) {
        return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_';
    }

    private static boolean isMark(int i) {
        int j = Character.getType(i);
        return j == 6 || j == 8 || j == 7;
    }

    private static boolean isFormatCode(int i) {
        return i == 167 || i == 8203 || i == 8204 || i == 8205 || i == 65279;
    }

    private static String stripLegacyCodes(String string) {
        if (string != null && !string.isEmpty()) {
            StringBuilder sb = new StringBuilder(string.length());
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                if (c == 167
                        && i + 1 < string.length()
                        && "0123456789abcdefklmnorx"
                                        .indexOf(Character.toLowerCase(string.charAt(i + 1)))
                                >= 0) {
                    i++;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    private static int foldConfusable(int i) {
        return switch (i) {
            case 593, 945, 1072, 7424 -> 97;
            case 609, 610 -> 103;
            case 618, 953, 1110 -> 105;
            case 628, 1400 -> 110;
            case 640 -> 114;
            case 655, 1091 -> 121;
            case 665, 946, 1074 -> 98;
            case 668, 1085 -> 104;
            case 671 -> 108;
            case 949, 1077, 7431 -> 101;
            case 954, 1082, 7435 -> 107;
            case 957, 7456 -> 118;
            case 959, 1086, 7439 -> 111;
            case 961, 1088, 7448 -> 112;
            case 964, 1090, 7451 -> 116;
            case 965, 7452 -> 117;
            case 1010, 1089, 7428 -> 99;
            case 1084, 7437 -> 109;
            case 1093 -> 120;
            case 1109, 42801 -> 115;
            case 1112, 7434 -> 106;
            case 1121, 7457 -> 119;
            case 1281, 7429 -> 100;
            case 1307 -> 113;
            case 7458, 7459 -> 122;
            case 42800 -> 102;
            default -> i;
        };
    }
}
