package net.efn.client.module.impl.misc.nameprotect;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class AliasPolicy {

    public static final int MAX_LENGTH = 16;

    private static final int MIN_GLUED_NAME = 4;

    private static final String[] STAFF_FRAGMENTS =
            skeletons(
                    "admin",
                    "owner",
                    "moderat",
                    "staff",
                    "helper",
                    "founder",
                    "manager",
                    "develop",
                    "console",
                    "support",
                    "operator",
                    "official",
                    "donutsmp");

    private static final Set<String> STAFF_WORDS =
            Set.of(
                    skeletons(
                            "mod",
                            "mods",
                            "srmod",
                            "jrmod",
                            "trialmod",
                            "headmod",
                            "dev",
                            "devs",
                            "op",
                            "ops",
                            "server",
                            "system",
                            "builder"));

    private AliasPolicy() {}

    public static String sanitize(String string) {
        if (string == null) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder(Math.min(string.length(), 16));
            for (int i = 0; i < string.length() && sb.length() < 16; i++) {
                char c = string.charAt(i);
                if (c == 167) {
                    i++;
                } else if (NameMatcher.isNameChar(c)) {
                    sb.append(c);
                } else if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
            }
            return sb.toString().trim();
        }
    }

    public static String skeleton(CharSequence charSequence) {
        StringBuilder sb = new StringBuilder(charSequence.length());
        for (int i = 0; i < charSequence.length(); i++) {
            char c = Character.toLowerCase(charSequence.charAt(i));
            switch (c) {
                case ' ':
                case '_':
                    break;
                case '0':
                    sb.append('o');
                    break;
                case '1':
                case 'i':
                    sb.append('l');
                    break;
                case '3':
                    sb.append('e');
                    break;
                case '4':
                    sb.append('a');
                    break;
                case '5':
                    sb.append('s');
                    break;
                case '7':
                    sb.append('t');
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    public static AliasPolicy.Refusal check(String string, Predicate<String> predicate) {
        String skeleton = skeleton(string);
        for (String string2 : STAFF_FRAGMENTS) {
            if (skeleton.contains(string2)) {
                return AliasPolicy.Refusal.STAFF_TITLE;
            }
        }
        if (STAFF_WORDS.contains(skeleton)) {
            return AliasPolicy.Refusal.STAFF_TITLE;
        } else if (predicate.test(skeleton)) {
            return AliasPolicy.Refusal.PLAYER_NAME;
        } else {
            for (int i = 4; i < skeleton.length(); i++) {
                if (predicate.test(skeleton.substring(0, i))
                        || predicate.test(skeleton.substring(skeleton.length() - i))) {
                    return AliasPolicy.Refusal.PLAYER_NAME;
                }
            }
            for (String string3 : string.split("[ _]+")) {
                if (!string3.isEmpty()) {
                    String skeleton2 = skeleton(string3);
                    if (STAFF_WORDS.contains(skeleton2)) {
                        return AliasPolicy.Refusal.STAFF_TITLE;
                    }
                    if (predicate.test(skeleton2)) {
                        return AliasPolicy.Refusal.PLAYER_NAME;
                    }
                }
            }
            return null;
        }
    }

    private static String[] skeletons(String... string) {
        String[] strings = new String[string.length];
        for (int i = 0; i < string.length; i++) {
            strings[i] = skeleton(string[i].toLowerCase(Locale.ROOT));
        }
        return strings;
    }

    @Environment(EnvType.CLIENT)
    public static enum Refusal {
        STAFF_TITLE("it reads like a staff or server title"),
        PLAYER_NAME("it is the name of a player seen on this server");

        private final String reason;

        private Refusal(String reason2) {
            this.reason = reason2;
        }

        public String reason() {
            return this.reason;
        }
    }
}
