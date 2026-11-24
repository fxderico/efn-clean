package net.efn.client.util.playerbypass;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public record RtpReply(RtpReply.Kind kind, long millis, long nextUseMs) {

    public static final RtpReply NONE = new RtpReply(RtpReply.Kind.NONE, 0L);

    private static final Pattern FORMATTING = Pattern.compile("\u00a7.");

    private static final Pattern DURATION =
            Pattern.compile(
                    "(\\d+(?:[.,]\\d+)?)\\s*(hours?|hrs?|saat|h|minutes?|mins?|dakika|dk|m|seconds?|secs?|saniye|sn|s)(?![a-z])");

    private static final Pattern CLOCK =
            Pattern.compile("(?<![\\d:])(\\d{1,2}):(\\d{2})(?![\\d:])");

    private static final Pattern BRACKET_TIME =
            Pattern.compile("[\\[(<]\\s*\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:[ap]m)?\\s*[\\])>]");

    private static final Pattern LEADING_TIME =
            Pattern.compile(
                    "^\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s?[ap]m)?(?![\\d:])(?!\\s*(?:left|remaining|kaldi|kalan))");

    private static final Pattern SENTENCE = Pattern.compile("\\.(?!\\d)|[!?|;]");

    private static final String SMALL_CAPS =
            "\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18\u01eb\u0280\u0455\ua731\u1d1b\u1d1c\u1d20\u1d21\u028f\u1d22";

    private static final String SMALL_CAPS_PLAIN = "abcdefghijklmnopqrsstuvwyz";

    private static final String TURKISH =
            "\u0131\u0130\u015f\u015e\u011f\u011e\u00fc\u00dc\u00f6\u00d6\u00e7\u00c7";

    private static final String TURKISH_PLAIN = "iissgguuoocc";

    private static final String CHAT_SEPARATORS = ":>\u00bb\u203a\u226b\u27a4\u279c\u2192\u21d2|";

    private static final String[] UNKNOWN =
            new String[] {
                "unknown command",
                "unknown or incomplete command",
                "bilinmeyen komut",
                "komut bulunamadi",
                "boyle bir komut yok"
            };

    private static final String[] NO_PERMISSION =
            new String[] {
                "no permission",
                "don't have permission",
                "dont have permission",
                "do not have permission",
                "not have permission",
                "insufficient permission",
                "not allowed to use",
                "yetkin yok",
                "yetkiniz yok",
                "iznin yok",
                "izniniz yok",
                "yetkiniz bulunmuyor",
                "izniniz bulunmuyor"
            };

    private static final String[] COMBAT =
            new String[] {
                "combat", "savasta", "savas modunda", "pvp modunda", "pvp'de", "pvpde", "kombat"
            };

    private static final String[] COMBAT_STRONG =
            new String[] {
                "in combat",
                "combat tag",
                "combat-tag",
                "combattag",
                "savasta",
                "savas modunda",
                "pvp modunda",
                "pvp'de",
                "pvpde"
            };

    private static final String[] COMBAT_OVER =
            new String[] {
                "no longer in combat",
                "out of combat",
                "left combat",
                "not in combat",
                "savastan ciktin",
                "artik savasta degil"
            };

    private static final String[] COOLDOWN_STRONG =
            new String[] {
                "cooldown",
                "cool down",
                "again",
                "another",
                "next teleport",
                "next rtp",
                "next use",
                "tekrar",
                "yeniden",
                "bekleme suresi"
            };

    private static final String[] COOLDOWN_WEAK =
            new String[] {"wait", "bekle", "before", "until", "remaining", "kalan", "sonra"};

    private static final String[] WARMUP =
            new String[] {
                "teleporting in",
                "teleporting you in",
                "teleport in",
                "teleported in",
                "teleportation in",
                "teleportation will",
                "teleport will",
                "will be teleported",
                "will teleport",
                "before teleporting",
                "before you teleport",
                "before being teleported",
                "don't move",
                "dont move",
                "do not move",
                "stand still",
                "isinlaniyor",
                "isinlanacak",
                "isinlanmaya",
                "hareket etme"
            };

    private static final String[] PROGRESS =
            new String[] {
                "teleporting", "searching", "looking for a", "finding a", "araniyor", "konum bul"
            };

    private static final String[] CANCEL =
            new String[] {"cancel", "iptal", "you moved", "hareket ettin", "aborted"};

    public RtpReply(RtpReply.Kind kind, long l) {
        this(kind, l, -1L);
    }

    public static RtpReply parse(String string) {
        String string2 = normalize(string);
        if (string2.isEmpty()) {
            return NONE;
        } else if (containsAny(string2, UNKNOWN)) {
            return new RtpReply(RtpReply.Kind.UNKNOWN_COMMAND, 0L);
        } else if (containsAny(string2, NO_PERMISSION)) {
            return new RtpReply(RtpReply.Kind.NO_PERMISSION, 0L);
        } else {
            string2 =
                    LEADING_TIME
                            .matcher(BRACKET_TIME.matcher(string2).replaceAll(" ").trim())
                            .replaceFirst("")
                            .trim();
            String[] strings = SENTENCE.split(string2);
            long l2 = -1L;
            long l3 = -1L;
            long l4 = -1L;
            long l5 = -1L;
            for (String string3 : strings) {
                long durationMs = durationMs(string3);
                if (durationMs >= 0L) {
                    if (l2 < 0L) {
                        l2 = durationMs;
                    }
                    if (containsAny(string3, COOLDOWN_STRONG)) {
                        if (l3 < 0L) {
                            l3 = durationMs;
                        }
                    } else if (containsAny(string3, WARMUP)) {
                        if (l4 < 0L) {
                            l4 = durationMs;
                        }
                    } else if (containsAny(string3, COOLDOWN_WEAK) && l5 < 0L) {
                        l5 = durationMs;
                    }
                }
            }
            boolean containsAny = containsAny(string2, WARMUP);
            if (!containsAny(string2, COMBAT)
                    || containsAny(string2, COMBAT_OVER)
                    || containsAny && (l4 >= 0L || !containsAny(string2, COMBAT_STRONG))) {
                boolean containsAny2 = containsAny(string2, CANCEL);
                if (l2 < 0L) {
                    if (containsAny2) {
                        return new RtpReply(RtpReply.Kind.CANCELLED, 0L);
                    } else {
                        return !containsAny && !containsAny(string2, PROGRESS)
                                ? NONE
                                : new RtpReply(RtpReply.Kind.WARMUP, 0L);
                    }
                } else if (l4 >= 0L && l3 >= 0L) {
                    return new RtpReply(RtpReply.Kind.WARMUP, l4, l3);
                } else if (containsAny(string2, COOLDOWN_STRONG)) {
                    return new RtpReply(RtpReply.Kind.COOLDOWN, l3 >= 0L ? l3 : l2);
                } else if (containsAny) {
                    return new RtpReply(RtpReply.Kind.WARMUP, l4 >= 0L ? l4 : l2);
                } else if (containsAny2) {
                    return new RtpReply(RtpReply.Kind.CANCELLED, 0L);
                } else {
                    return containsAny(string2, COOLDOWN_WEAK)
                            ? new RtpReply(RtpReply.Kind.COOLDOWN, l5 >= 0L ? l5 : l2)
                            : NONE;
                }
            } else {
                return new RtpReply(RtpReply.Kind.COMBAT, 0L);
            }
        }
    }

    public static long durationMs(String string) {
        Matcher matcher = DURATION.matcher(string);
        double d = 0.0;
        boolean bl = false;
        while (matcher.find()) {
            double e;
            try {
                e = Double.parseDouble(matcher.group(1).replace(',', '.'));
            } catch (NumberFormatException exception) {
                continue;
            }
            d += e * unitMs(matcher.group(2));
            bl = true;
        }
        if (bl) {
            return Math.round(d);
        } else {
            Matcher matcher2 = CLOCK.matcher(string);
            return matcher2.find()
                    ? (Integer.parseInt(matcher2.group(1)) * 60L
                                    + Integer.parseInt(matcher2.group(2)))
                            * 1000L
                    : -1L;
        }
    }

    private static long unitMs(String string) {
        return switch (string) {
            case "h", "hr", "hrs", "hour", "hours", "saat" -> 3600000L;
            case "m", "min", "mins", "minute", "minutes", "dakika", "dk" -> 60000L;
            default -> 1000L;
        };
    }

    public static boolean looksLikePlayerChat(String string, Iterable<String> iterable) {
        String string2 = normalize(string);
        if (string2.isEmpty()) {
            return false;
        } else {
            for (String string3 : iterable) {
                if (string3 != null && !string3.isBlank()) {
                    String string4 = string3.toLowerCase(Locale.ROOT);
                    for (int i = string2.indexOf(string4);
                            i >= 0;
                            i = string2.indexOf(string4, i + 1)) {
                        int j = i + string4.length();
                        if ((i <= 0 || !nameChar(string2.charAt(i - 1)))
                                && (j >= string2.length() || !nameChar(string2.charAt(j)))
                                && separatorAfter(string2, j)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private static boolean nameChar(char c) {
        return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_';
    }

    private static boolean separatorAfter(String string, int i) {
        int j = i;
        while (j < string.length() && j - i < 3 && " ])}".indexOf(string.charAt(j)) >= 0) {
            j++;
        }
        if (j >= string.length()) {
            return false;
        } else {
            char c = string.charAt(j);
            return ":>\u00bb\u203a\u226b\u27a4\u279c\u2192\u21d2|".indexOf(c) >= 0
                    ? true
                    : (c == '-' || c == '=')
                            && j + 1 < string.length()
                            && string.charAt(j + 1) == '>';
        }
    }

    public static String normalize(String string) {
        if (string == null) {
            return "";
        } else {
            String string2 = FORMATTING.matcher(string).replaceAll("");
            StringBuilder sb = new StringBuilder(string2.length());
            for (int i = 0; i < string2.length(); i++) {
                char c = string2.charAt(i);
                int j =
                        "\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18\u01eb\u0280\u0455\ua731\u1d1b\u1d1c\u1d20\u1d21\u028f\u1d22"
                                .indexOf(c);
                int k =
                        "\u0131\u0130\u015f\u015e\u011f\u011e\u00fc\u00dc\u00f6\u00d6\u00e7\u00c7"
                                .indexOf(c);
                if (j >= 0) {
                    sb.append("abcdefghijklmnopqrsstuvwyz".charAt(j));
                } else if (k >= 0) {
                    sb.append("iissgguuoocc".charAt(k));
                } else if (c == 8217 || c == 8216) {
                    sb.append('\'');
                } else if (c != 775) {
                    sb.append(c);
                }
            }
            return sb.toString().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        }
    }

    private static boolean containsAny(String string, String[] strings) {
        for (String string2 : strings) {
            if (string.contains(string2)) {
                return true;
            }
        }
        return false;
    }

    @Environment(EnvType.CLIENT)
    public static enum Kind {
        NONE,
        COOLDOWN,
        WARMUP,
        CANCELLED,
        UNKNOWN_COMMAND,
        NO_PERMISSION,
        COMBAT;

        private Kind() {}
    }
}
