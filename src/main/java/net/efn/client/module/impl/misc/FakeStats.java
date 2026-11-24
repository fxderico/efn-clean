package net.efn.client.module.impl.misc;

import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardCriterion.RenderType;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.FixedNumberFormat;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class FakeStats extends ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final String OBJECTIVE = "efn_fs";

    private static final String SHOWN_BY_DEFAULT = "efn.lol";

    private static final int LINES = 15;

    private static final Comparator<ScoreboardEntry> ORDER =
            Comparator.comparingInt(ScoreboardEntry::value)
                    .reversed()
                    .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER);

    private static final Pattern MONEY = Pattern.compile("\\$\\s?(\\d[\\d.,]*[KkMmBbTtQq]?)");

    private static final String SMALL_CAPS =
            "\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18\u01eb\u0280\ua731\u1d1b\u1d1c\u1d20\u1d21x\u028f\u1d22";

    private static volatile FakeStats active;

    private final StringSetting playerName =
            this.addSetting("name", new StringSetting("Name", "efn.lol"));

    private final FakeStats.Stat money = this.stat("Money", "money", "balance", "$");

    private final List<FakeStats.Stat> stats =
            List.of(
                    this.money,
                    this.stat("Shards", "shards", "\u2605"),
                    this.stat("Kills", "kills", "\ud83d\udde1", "\u2694"),
                    this.stat("Deaths", "deaths", "\u2620"),
                    this.stat("Playtime", "playtime", "\u231a"),
                    this.stat("Team", "team"));

    private long seed;

    private boolean applying;

    private Scoreboard board;

    private ScoreboardObjective fake;

    private ScoreboardObjective serverSidebar;

    private String sidebarMoney;

    private boolean nameReported;

    public FakeStats() {
        super(
                "fake-stats",
                "Fake Stats",
                "Shows your own name and stats on the sidebar and tab list. Blank stats get"
                        + " scrambled.",
                ModuleCategory.DONUT);
    }

    private FakeStats.Stat stat(String string, String... string2) {
        return new FakeStats.Stat(
                this.addSetting(
                        string.toLowerCase(Locale.ROOT), new StringSetting(string, "efn.lol")),
                string2);
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    public static boolean redirect(
            Scoreboard scoreboard,
            ScoreboardDisplaySlot scoreboardDisplaySlot,
            ScoreboardObjective scoreboardObjective) {
        FakeStats fakeStats = active;
        if (fakeStats == null
                || fakeStats.applying
                || scoreboardDisplaySlot != ScoreboardDisplaySlot.SIDEBAR) {
            return false;
        } else if (scoreboardObjective != null && "efn_fs".equals(scoreboardObjective.getName())) {
            return false;
        } else {
            fakeStats.serverSidebar = scoreboardObjective;
            if (scoreboardObjective == null) {
                return false;
            } else {
                ScoreboardObjective scoreboardObjective2 = fakeStats.install(scoreboard);
                if (scoreboardObjective2 == null) {
                    return false;
                } else {
                    fakeStats.applying = true;
                    try {
                        scoreboard.setObjectiveSlot(
                                ScoreboardDisplaySlot.SIDEBAR, scoreboardObjective2);
                    } finally {
                        fakeStats.applying = false;
                    }
                    return true;
                }
            }
        }
    }

    public static Text tabName(UUID uuid, Text text) {
        FakeStats fakeStats = active;
        return fakeStats != null && text != null && isSelf(uuid)
                ? fakeStats.fakeTabMoney(text)
                : text;
    }

    public static StringVisitable tabText(StringVisitable stringVisitable) {
        FakeStats fakeStats = active;
        Object object;
        if (fakeStats != null && stringVisitable instanceof Text text) {
            object = fakeStats.fakeTabMoney(text);
        } else {
            object = stringVisitable;
        }
        return (StringVisitable) object;
    }

    public static ReadableScoreboardScore tabScore(
            ScoreHolder scoreHolder, ReadableScoreboardScore readableScoreboardScore) {
        final FakeStats fakeStats = active;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return fakeStats != null
                        && readableScoreboardScore != null
                        && player != null
                        && player.getNameForScoreboard().equals(scoreHolder.getNameForScoreboard())
                ? new ReadableScoreboardScore() {

                    @Override
                    public int getScore() {
                        return readableScoreboardScore.getScore();
                    }

                    @Override
                    public boolean isLocked() {
                        return readableScoreboardScore.isLocked();
                    }

                    @Override
                    public NumberFormat getNumberFormat() {
                        return readableScoreboardScore.getNumberFormat();
                    }

                    @Override
                    public MutableText getFormattedScore(NumberFormat fallbackFormat) {
                        return Text.empty()
                                .append(
                                        fakeStats.fakeTabMoney(
                                                readableScoreboardScore.getFormattedScore(
                                                        fallbackFormat)));
                    }
                }
                : readableScoreboardScore;
    }

    @Override
    protected void onEnable() {
        this.seed = ThreadLocalRandom.current().nextLong();
        active = this;
        this.on(TickEvent.TYPE, Stage.POST, tickEvent -> this.refresh());
        this.refresh();
    }

    @Override
    protected void onDisable() {
        active = null;
        this.restore();
    }

    private void refresh() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            this.board = null;
            this.fake = null;
            this.serverSidebar = null;
            this.sidebarMoney = null;
        } else {
            Scoreboard scoreboard = client.world.getScoreboard();
            if (scoreboard != this.board) {
                this.board = scoreboard;
                this.fake = null;
                this.serverSidebar = null;
            }
            ScoreboardObjective objectiveForSlot =
                    scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (this.serverSidebar == null || this.serverSidebar.getScoreboard() != scoreboard) {
                ScoreboardObjective scoreboardObjective =
                        this.serverSidebar =
                                objectiveForSlot != null
                                                && !"efn_fs".equals(objectiveForSlot.getName())
                                        ? objectiveForSlot
                                        : null;
            }
            if (this.serverSidebar == null) {
                this.sidebarMoney = null;
                if (this.fake != null && objectiveForSlot == this.fake) {
                    this.applying = true;
                    try {
                        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, null);
                    } finally {
                        this.applying = false;
                    }
                }
            } else {
                ScoreboardObjective scoreboardObjective2 = this.install(scoreboard);
                if (scoreboardObjective2 != null) {
                    if (scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR)
                            != scoreboardObjective2) {
                        this.applying = true;
                        try {
                            scoreboard.setObjectiveSlot(
                                    ScoreboardDisplaySlot.SIDEBAR, scoreboardObjective2);
                        } finally {
                            this.applying = false;
                        }
                    }
                }
            }
        }
    }

    private ScoreboardObjective install(Scoreboard board2) {
        ScoreboardObjective serverSidebar2 = this.serverSidebar;
        if (serverSidebar2 != null && serverSidebar2.getScoreboard() == board2) {
            ScoreboardObjective nullableObjective = board2.getNullableObjective("efn_fs");
            if (nullableObjective == null) {
                nullableObjective =
                        board2.addObjective(
                                "efn_fs",
                                ScoreboardCriterion.DUMMY,
                                Text.empty(),
                                RenderType.INTEGER,
                                false,
                                serverSidebar2.getNumberFormat());
            }
            this.fake = nullableObjective;
            this.board = board2;
            nullableObjective.setNumberFormat(serverSidebar2.getNumberFormat());
            this.writeRows(board2, nullableObjective, serverSidebar2);
            return nullableObjective;
        } else {
            return this.fake;
        }
    }

    private void writeRows(
            Scoreboard scoreboard,
            ScoreboardObjective scoreboardObjective,
            ScoreboardObjective scoreboardObjective2) {
        ArrayList list = new ArrayList();
        for (ScoreboardEntry scoreboardEntry :
                scoreboard.getScoreboardEntries(scoreboardObjective2)) {
            if (!scoreboardEntry.hidden()) {
                list.add(scoreboardEntry);
            }
        }
        list.sort(ORDER);
        if (list.size() > 15) {
            list.subList(15, list.size()).clear();
        }
        Text text;
        Text text2;
        scoreboardObjective.setDisplayName(
                (text2 = this.renamed(text = visible(scoreboardObjective2.getDisplayName())))
                                != null
                        ? text2
                        : this.mask(text, 0));
        boolean bl = text2 != null;
        NumberFormat numberFormat = scoreboardObjective2.getNumberFormatOr(StyledNumberFormat.RED);
        this.sidebarMoney = null;
        for (int i = 0; i < list.size(); i++) {
            ScoreboardEntry scoreboardEntry2 = (ScoreboardEntry) list.get(i);
            Text text3 =
                    visible(
                            Team.decorateName(
                                    scoreboard.getScoreHolderTeam(scoreboardEntry2.owner()),
                                    scoreboardEntry2.name()));
            Text text4 = visible(scoreboardEntry2.formatted(numberFormat));
            Text text5 = this.renamed(text3);
            FakeStats.Hit hit =
                    text5 == null ? this.find(text3.getString(), text4.getString()) : null;
            String string = hit != null ? hit.stat().value().get().strip() : "";
            String string2 = null;
            if (text5 != null) {
                text3 = text5;
                bl = true;
            } else if (string.isEmpty()) {
                text3 = this.mask(text3, i + 1);
                text4 = this.mask((Text) text4, i + 1);
                if (hit != null) {
                    string2 =
                            (hit.inNumber() ? text4 : text3)
                                    .getString()
                                    .substring(hit.start(), hit.end());
                }
            } else {
                Text text6 = hit.inNumber() ? text4 : text3;
                string2 = fit(text6.getString().substring(hit.start(), hit.end()), string);
                MutableText text7 =
                        replace((Text) text6, List.of(new int[] {hit.start(), hit.end()}), string2);
                text3 = (Text) (hit.inNumber() ? this.mask(text3, i + 1) : text7);
                text4 = hit.inNumber() ? text7 : this.mask((Text) text4, i + 1);
            }
            if (hit != null && hit.stat() == this.money) {
                this.sidebarMoney = string2;
            }
            ScoreHolder scoreHolder = ScoreHolder.fromName(holderName(i));
            scoreboard.clearTeam(scoreHolder.getNameForScoreboard());
            ScoreAccess orCreateScore =
                    scoreboard.getOrCreateScore(scoreHolder, scoreboardObjective);
            orCreateScore.setScore(scoreboardEntry2.value());
            orCreateScore.setDisplayText(text3);
            orCreateScore.setNumberFormat(new FixedNumberFormat((Text) text4));
        }
        for (int j = list.size(); j < 15; j++) {
            ScoreHolder scoreHolder2 = ScoreHolder.fromName(holderName(j));
            if (scoreboard.getScore(scoreHolder2, scoreboardObjective) != null) {
                scoreboard.removeScore(scoreHolder2, scoreboardObjective);
            }
        }
        if (!bl && !this.playerName.get().isBlank() && !this.nameReported) {
            this.nameReported = true;
            this.reportMissingName(scoreboard, list, numberFormat, text);
        }
    }

    private void reportMissingName(
            Scoreboard scoreboard,
            List<ScoreboardEntry> list,
            NumberFormat numberFormat,
            Text text) {
        StringBuilder sb = new StringBuilder("title: ").append(escaped(text.getString()));
        for (ScoreboardEntry scoreboardEntry : list) {
            Text text2 =
                    visible(
                            Team.decorateName(
                                    scoreboard.getScoreHolderTeam(scoreboardEntry.owner()),
                                    scoreboardEntry.name()));
            sb.append("\n  ")
                    .append(escaped(text2.getString()))
                    .append(" | ")
                    .append(escaped(visible(scoreboardEntry.formatted(numberFormat)).getString()));
        }
        LOGGER.info("Fake Stats: no sidebar line shows the name {}\n{}", selfName(), sb);
    }

    private static String escaped(String string) {
        StringBuilder sb = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c >= ' ' && c < 127) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04X", c));
            }
        }
        return sb.toString();
    }

    private void restore() {
        Scoreboard board2 = this.board;
        ScoreboardObjective serverSidebar2 = this.serverSidebar;
        ScoreboardObjective fake2 = this.fake;
        this.board = null;
        this.fake = null;
        this.serverSidebar = null;
        this.sidebarMoney = null;
        if (board2 != null) {
            if (board2.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) == fake2) {
                ScoreboardObjective scoreboardObjective =
                        serverSidebar2 != null && serverSidebar2.getScoreboard() == board2
                                ? serverSidebar2
                                : null;
                board2.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, scoreboardObjective);
            }
            ScoreboardObjective nullableObjective;
            if ((nullableObjective = board2.getNullableObjective("efn_fs")) != null) {
                board2.removeObjective(nullableObjective);
            }
        }
    }

    private FakeStats.Hit find(String string, String string2) {
        for (FakeStats.Stat stat2 : this.stats) {
            int[] ints = valueAfter(string, stat2);
            if (ints != null && ints[0] < ints[1]) {
                return new FakeStats.Hit(stat2, false, ints[0], ints[1]);
            }
            if (ints != null) {
                return new FakeStats.Hit(stat2, true, 0, string2.length());
            }
            ints = valueAfter(string2, stat2);
            if (ints != null) {
                return new FakeStats.Hit(stat2, true, ints[0], ints[1]);
            }
        }
        return null;
    }

    private static int[] valueAfter(String string, FakeStats.Stat stat2) {
        for (String marker : stat2.markers()) {
            int marker2 = markerAt(string, marker);
            if (marker2 >= 0) {
                int i = string.length();
                int j = marker2 + marker.length();
                while (j < i
                        && (Character.isWhitespace(string.charAt(j)) || string.charAt(j) == ':')) {
                    j++;
                }
                while (i > j && Character.isWhitespace(string.charAt(i - 1))) {
                    i--;
                }
                return new int[] {j, i};
            }
        }
        return null;
    }

    private static int markerAt(String string, String string2) {
        String folded = folded(string);
        boolean wordChar = wordChar(string2.charAt(0));
        boolean wordChar2 = wordChar(string2.charAt(string2.length() - 1));
        for (int i = 0; i + string2.length() <= folded.length(); i++) {
            int j = i + string2.length();
            if (folded.regionMatches(true, i, string2, 0, string2.length())
                    && (!wordChar || i <= 0 || !wordChar(folded.charAt(i - 1)))
                    && (!wordChar2 || j >= folded.length() || !wordChar(folded.charAt(j)))) {
                return i;
            }
        }
        return -1;
    }

    private static String folded(String string) {
        char[] charArray = string.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            int j =
                    "\u1d00\u0299\u1d04\u1d05\u1d07\ua730\u0262\u029c\u026a\u1d0a\u1d0b\u029f\u1d0d\u0274\u1d0f\u1d18\u01eb\u0280\ua731\u1d1b\u1d1c\u1d20\u1d21x\u028f\u1d22"
                            .indexOf(charArray[i]);
            if (j >= 0) {
                charArray[i] = (char) (97 + j);
            }
        }
        return new String(charArray);
    }

    private static boolean wordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static String selfName() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null ? player.getGameProfile().name() : null;
    }

    private Text renamed(Text text) {
        String selfName = selfName();
        int i = selfName != null ? markerAt(text.getString(), selfName) : -1;
        if (i < 0) {
            return null;
        } else {
            String string = this.playerName.get().strip();
            return (Text)
                    (string.isEmpty()
                            ? text
                            : replace(text, List.of(new int[] {i, i + selfName.length()}), string));
        }
    }

    private static String fit(String string, String string2) {
        String string3 = string2.startsWith("$") ? string2.substring(1).strip() : string2;
        return string.startsWith("$") ? "$" + string3 : string3;
    }

    private String tabMoney() {
        String string = this.money.value().get().strip();
        String string2 = string.isEmpty() ? this.sidebarMoney : string;
        if (string2 == null) {
            return null;
        } else {
            string2 = string2.startsWith("$") ? string2.substring(1).strip() : string2.strip();
            return string2.isEmpty() ? null : string2;
        }
    }

    private Text fakeTabMoney(Text text) {
        String tabMoney = this.tabMoney();
        if (tabMoney != null && text.getString().indexOf(36) >= 0) {
            Text text2 = visible(text);
            Matcher matcher = MONEY.matcher(text2.getString());
            ArrayList list = new ArrayList();
            while (matcher.find()) {
                list.add(new int[] {matcher.start(1), matcher.end(1)});
            }
            return (Text) (list.isEmpty() ? text : replace(text2, list, tabMoney));
        } else {
            return text;
        }
    }

    private static boolean isSelf(UUID uuid) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && player.getUuid().equals(uuid);
    }

    private static Text visible(Text text) {
        if (text.getString().indexOf(167) < 0) {
            return text;
        } else {
            MutableText text2 = Text.empty();
            StringBuilder sb = new StringBuilder();
            Style[] styles = new Style[] {null};
            TextVisitFactory.visitFormatted(
                    text,
                    Style.EMPTY,
                    (i, style, j) -> {
                        if (styles[0] != null && !style.equals(styles[0])) {
                            text2.append(Text.literal(sb.toString()).setStyle(styles[0]));
                            sb.setLength(0);
                        }
                        styles[0] = style;
                        sb.appendCodePoint(j);
                        return true;
                    });
            if (!sb.isEmpty()) {
                text2.append(Text.literal(sb.toString()).setStyle(styles[0]));
            }
            return text2;
        }
    }

    private static MutableText replace(Text text, List<int[]> list, String string) {
        if (text.getString().isEmpty()) {
            return Text.literal(string);
        } else {
            MutableText text2 = Text.empty();
            int[] ints = new int[] {0};
            int[] ints2 = new int[] {0};
            text.visit(
                    (style, value2) -> {
                        int i = ints[0];
                        int j = i + value2.length();
                        int k = i;
                        while (k < j) {
                            int[] ints3 =
                                    ints2[0] < list.size() ? (int[]) list.get(ints2[0]) : null;
                            if (ints3 == null || ints3[0] >= j) {
                                text2.append(Text.literal(value2.substring(k - i)).setStyle(style));
                                k = j;
                            } else if (k < ints3[0]) {
                                text2.append(
                                        Text.literal(value2.substring(k - i, ints3[0] - i))
                                                .setStyle(style));
                                k = ints3[0];
                            } else {
                                if (k == ints3[0]) {
                                    text2.append(Text.literal(string).setStyle(style));
                                }
                                if ((k = Math.min(j, ints3[1])) == ints3[1]) {
                                    ints2[0]++;
                                }
                            }
                        }
                        ints[0] = j;
                        return Optional.empty();
                    },
                    Style.EMPTY);
            return text2;
        }
    }

    private static String holderName(int i) {
        return new String(new char[] {'\ue000', (char) (57360 + i)});
    }

    private Text mask(Text text, int i) {
        if (text == null) {
            return Text.empty();
        } else {
            MutableText text2 = Text.empty();
            boolean[] booleans = new boolean[] {false};
            int[] ints = new int[] {0};
            text.visit(
                    (style, value2) -> {
                        String maskText = this.maskText(value2, i, ints);
                        if (!maskText.equals(value2)) {
                            booleans[0] = true;
                        }
                        text2.append(Text.literal(maskText).setStyle(style));
                        return Optional.empty();
                    },
                    Style.EMPTY);
            return (Text) (booleans[0] ? text2 : text);
        }
    }

    private String maskText(String string, int i, int[] ints) {
        StringBuilder sb = null;
        int j = 0;
        while (j < string.length()) {
            int k = string.codePointAt(j);
            if (!Character.isDigit(k)) {
                if (sb != null) {
                    sb.appendCodePoint(k);
                }
                j += Character.charCount(k);
            } else {
                int m = j;
                while (true) {
                    if (m < string.length()) {
                        int n = string.codePointAt(m);
                        int charCount = Character.charCount(n);
                        if (Character.isDigit(n)) {
                            m += charCount;
                            continue;
                        }
                        if ((n == 44 || n == 46)
                                && m + charCount < string.length()
                                && Character.isDigit(string.codePointAt(m + charCount))) {
                            m += charCount;
                            continue;
                        }
                    }
                    String string2 = string.substring(j, m);
                    int o = ints[0]++;
                    String fakeNumber = this.fakeNumber(i, o, string2);
                    if (sb == null) {
                        if (!fakeNumber.equals(string2)) {
                            sb =
                                    new StringBuilder(string.length())
                                            .append(string, 0, j)
                                            .append(fakeNumber);
                        }
                    } else {
                        sb.append(fakeNumber);
                    }
                    j = m;
                    break;
                }
            }
        }
        return sb == null ? string : sb.toString();
    }

    private String fakeNumber(int i, int j, String string) {
        int k = 0;
        for (int m = 0; m < string.length(); m++) {
            char c = string.charAt(m);
            if (c >= '0' && c <= '9') {
                k++;
            }
        }
        if (k == 0) {
            return string;
        } else if (k > 15) {
            return this.shiftWide(i, j, string);
        } else {
            long l = 0L;
            for (int n = 0; n < string.length(); n++) {
                char c2 = string.charAt(n);
                if (c2 >= '0' && c2 <= '9') {
                    l = l * 10L + (c2 - '0');
                }
            }
            long pow10 = pow10(k);
            long remainderUnsigned =
                    Long.remainderUnsigned(
                            mix(this.seed ^ i * 1315423911L ^ j * 2654435761L ^ k), pow10);
            if (remainderUnsigned == 0L) {
                remainderUnsigned = 1L;
            }
            boolean bl;
            long shifted;
            if ((shifted =
                            shifted(
                                    l,
                                    remainderUnsigned,
                                    pow10,
                                    k,
                                    bl = string.charAt(0) == '0' && k > 1))
                    == l) {
                shifted =
                        shifted(
                                l,
                                remainderUnsigned == pow10 - 1L ? 1L : remainderUnsigned + 1L,
                                pow10,
                                k,
                                bl);
            }
            String string2;
            if ((string2 = Long.toString(shifted)).length() < k) {
                string2 = "0".repeat(k - string2.length()) + string2;
            }
            StringBuilder sb = new StringBuilder(string.length());
            int o = 0;
            for (int p = 0; p < string.length(); p++) {
                char c3 = string.charAt(p);
                if (c3 >= '0' && c3 <= '9') {
                    sb.append(string2.charAt(o++));
                } else {
                    sb.append(c3);
                }
            }
            return sb.toString();
        }
    }

    private static long shifted(long l, long l2, long l3, int i, boolean bl) {
        long l4 = Math.floorMod(l + l2, l3);
        if (!bl && i > 1 && l4 < l3 / 10L) {
            l4 += l3 / 10L;
        }
        return l4;
    }

    private String shiftWide(int i, int j, String string) {
        long l = mix(this.seed ^ i * 1315423911L ^ j);
        StringBuilder sb = new StringBuilder(string.length());
        int k = 0;
        boolean bl = true;
        for (int m = 0; m < string.length(); m++) {
            char c = string.charAt(m);
            if (c >= '0' && c <= '9') {
                int n = (int) Long.remainderUnsigned(mix(l + k), 9L) + 1;
                int o = (c - '0' + n) % 10;
                if (bl && o == 0) {
                    o = n;
                }
                bl = false;
                sb.append((char) (48 + o));
                k++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static long pow10(int i) {
        long l = 1L;
        for (int j = 0; j < i; j++) {
            l *= 10L;
        }
        return l;
    }

    private static long mix(long l) {
        l = (l ^ l >>> 30) * -4658895280553007687L;
        l = (l ^ l >>> 27) * -7723592293110705685L;
        return l ^ l >>> 31;
    }

    @Environment(EnvType.CLIENT)
    private record Hit(FakeStats.Stat stat, boolean inNumber, int start, int end) {}

    @Environment(EnvType.CLIENT)
    private record Stat(StringSetting value, String[] markers) {}
}
