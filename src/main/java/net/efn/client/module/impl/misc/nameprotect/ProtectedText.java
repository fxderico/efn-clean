/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.misc.nameprotect;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;

@Environment(value = EnvType.CLIENT)
public final class ProtectedText {

    private ProtectedText() {}

    public static Text rewrite(NameMatcher nameMatcher, Text text2) {
        if (text2 == null || nameMatcher.isEmpty()) {
            return text2;
        }
        Text text3 = ProtectedText.rebuild(nameMatcher, text2, true);
        return text3 != null ? text3 : text2;
    }

    public static StringVisitable rewrite(
            NameMatcher nameMatcher, StringVisitable stringVisitable) {
        if (stringVisitable == null || nameMatcher.isEmpty()) {
            return stringVisitable;
        }
        Text text2 = ProtectedText.rebuild(nameMatcher, stringVisitable, true);
        return text2 != null ? text2 : stringVisitable;
    }

    private static Text rebuild(
            NameMatcher nameMatcher, StringVisitable stringVisitable, boolean bl) {
        Pieces pieces2 = new Pieces();
        stringVisitable.visit(pieces2, Style.EMPTY);
        if (pieces2.count == 0) {
            return null;
        }
        pieces2.seal();
        String string = pieces2.text.toString();
        List<NameMatcher.Match> list = nameMatcher.scan(string, pieces2.starts, pieces2.count);
        Style[] styles2 = bl ? ProtectedText.rewriteHovers(nameMatcher, pieces2) : null;
        Style[] styles3 = styles2;
        if (list.isEmpty() && styles2 == null) {
            return null;
        }
        Style[] styles4 = styles2 != null ? styles2 : pieces2.styles;
        Runs runs = new Runs(pieces2);
        int i = 0;
        for (NameMatcher.Match match : list) {
            runs.copy(string, i, match.start());
            runs.spread(match.alias(), match.start(), match.end());
            i = match.end();
        }
        runs.copy(string, i, string.length());
        return runs.build(styles4);
    }

    private static Style[] rewriteHovers(NameMatcher nameMatcher, Pieces pieces2) {
        Style[] styles2 = null;
        IdentityHashMap<HoverEvent, HoverEvent> map = null;
        for (int i = 0; i < pieces2.count; ++i) {
            HoverEvent event;
            Style style = pieces2.styles[i];
            HoverEvent hoverEvent = style.getHoverEvent();
            if (hoverEvent == null) continue;
            if (map == null) {
                map = new IdentityHashMap<HoverEvent, HoverEvent>();
            }
            if ((event = (HoverEvent) map.get(hoverEvent)) == null) {
                event = ProtectedText.rewriteHover(nameMatcher, hoverEvent);
                map.put(hoverEvent, event);
            }
            if (event == hoverEvent) continue;
            if (styles2 == null) {
                styles2 = Arrays.copyOf(pieces2.styles, pieces2.count);
            }
            styles2[i] = style.withHoverEvent(event);
        }
        return styles2;
    }

    private static HoverEvent rewriteHover(NameMatcher nameMatcher, HoverEvent event) {
        if (event instanceof HoverEvent.ShowText) {
            HoverEvent.ShowText showText = (HoverEvent.ShowText) event;
            Text showText2 = showText.value();
            Text text2 =
                    showText2 != null ? ProtectedText.rebuild(nameMatcher, showText2, false) : null;
            return text2 != null ? new HoverEvent.ShowText(text2) : event;
        }
        if (event instanceof HoverEvent.ShowEntity) {
            HoverEvent.ShowEntity showEntity = (HoverEvent.ShowEntity) event;
            HoverEvent.EntityContent entityContent = showEntity.entity();
            if (entityContent == null || entityContent.name.isEmpty()) {
                return event;
            }
            Text text3 = ProtectedText.rebuild(nameMatcher, entityContent.name.get(), false);
            return text3 != null
                    ? new HoverEvent.ShowEntity(
                            new HoverEvent.EntityContent(
                                    entityContent.entityType, entityContent.uuid, text3))
                    : event;
        }
        return event;
    }

    @Environment(value = EnvType.CLIENT)
    private static final class Pieces implements StringVisitable.StyledVisitor<Object> {

        final StringBuilder text = new StringBuilder();

        Style[] styles = new Style[8];

        int[] starts = new int[9];

        int count;

        private Pieces() {}

        @Override
        public Optional<Object> accept(Style style, String string) {
            if (string.isEmpty()) {
                return Optional.empty();
            }
            if (this.count == this.styles.length) {
                this.styles = Arrays.copyOf(this.styles, this.count * 2);
                this.starts = Arrays.copyOf(this.starts, this.count * 2 + 1);
            }
            this.styles[this.count] = style;
            this.starts[this.count] = this.text.length();
            ++this.count;
            this.text.append(string);
            return Optional.empty();
        }

        void seal() {
            this.starts[this.count] = this.text.length();
        }

        int pieceAt(int i) {
            int binarySearch = Arrays.binarySearch(this.starts, 0, this.count, i);
            return binarySearch >= 0 ? binarySearch : -binarySearch - 2;
        }
    }

    @Environment(value = EnvType.CLIENT)
    private static final class Runs {

        private final Pieces pieces;

        private final List<Integer> runPieces = new ArrayList<Integer>();

        private final List<StringBuilder> runTexts = new ArrayList<StringBuilder>();

        Runs(Pieces pieces2) {
            this.pieces = pieces2;
        }

        void copy(String string, int i, int j) {
            int k = i < j ? this.pieces.pieceAt(i) : 0;
            int m = k;
            while (i < j) {
                int n = Math.min(this.pieces.starts[k + 1], j);
                this.run(k).append(string, i, n);
                i = n;
                ++k;
            }
        }

        void spread(String string, int i, int j) {
            int k = j - i;
            int codePointCount = string.codePointCount(0, string.length());
            int m = 0;
            int n = 0;
            while (n < string.length()) {
                int o = string.codePointAt(n);
                int p = i + (int) ((long) m * (long) k / (long) codePointCount);
                this.run(this.pieces.pieceAt(p)).appendCodePoint(o);
                n += Character.charCount(o);
                ++m;
            }
        }

        private StringBuilder run(int i) {
            int j = this.runPieces.size() - 1;
            if (j >= 0 && this.runPieces.get(j) == i) {
                return this.runTexts.get(j);
            }
            StringBuilder sb = new StringBuilder();
            this.runPieces.add(i);
            this.runTexts.add(sb);
            return sb;
        }

        Text build(Style[] styles2) {
            MutableText text2 = Text.empty();
            for (int i = 0; i < this.runTexts.size(); ++i) {
                text2.append(
                        Text.literal(this.runTexts.get(i).toString())
                                .setStyle(styles2[this.runPieces.get(i)]));
            }
            return text2;
        }
    }
}
