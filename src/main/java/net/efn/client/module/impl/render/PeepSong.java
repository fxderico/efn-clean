package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
enum PeepSong {
    STAR_SHOPPING("starshopping"),
    CRYBABY("crybaby"),
    CRY_ALONE("cryalone"),
    HELLBOY("hellboy"),
    BEAMER_BOY("beamerboy"),
    SPOTLIGHT("spotlight"),
    THE_BRIGHTSIDE("thebrightside", "brightside"),
    SUNLIGHT_ON_YOUR_SKIN("sunlightonyourskin"),
    LIFE_IS_BEAUTIFUL("lifeisbeautiful"),
    FALLING_DOWN("fallingdown"),
    SAVE_THAT_SHIT("savethatshit", "savethatsht"),
    AWFUL_THINGS("awfulthings"),
    WITCHBLADES("witchblades"),
    FOUR_GOLD_CHAINS("4goldchains", "fourgoldchains"),
    GHOST_BOY("ghostboy"),
    GHOST_GIRL("ghostgirl");

    static final PeepSong[] ALL = values();

    private static final Pattern SPACES = Pattern.compile("[\\s\\p{Z}]+");

    private static final Pattern BRACKETS = Pattern.compile("[(\\[{][^)\\]}]*[)\\]}]");

    private static final Pattern TAIL =
            Pattern.compile("\\s+(?:[-\u2013\u2014|]|feat\\.?|ft\\.?|with)\\s.*$");

    private static final Pattern VERSION =
            Pattern.compile(
                    "\\s*[-\u2013\u2014]\\s*(?:\\d{4}\\s*)?(?:remaster(?:ed)?|remix(?:ed)?|live)\\b.*$");

    private static final Pattern NOT_NAME = Pattern.compile("[^a-z0-9]");

    private final String[] names;

    private PeepSong(String... names2) {
        this.names = names2;
    }

    static PeepSong of(String string) {
        if (string != null && !string.isBlank()) {
            String string2 = Normalizer.normalize(string, Form.NFKC).toLowerCase(Locale.ROOT);
            string2 = SPACES.matcher(string2).replaceAll(" ");
            string2 = BRACKETS.matcher(string2).replaceAll(" ");
            string2 = TAIL.matcher(string2.strip()).replaceAll("");
            string2 = VERSION.matcher(string2).replaceAll("");
            string2 = NOT_NAME.matcher(string2).replaceAll("");
            for (PeepSong peepSong : ALL) {
                for (String name : peepSong.names) {
                    if (name.equals(string2)) {
                        return peepSong;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }
}
