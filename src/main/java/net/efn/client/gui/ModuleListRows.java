package net.efn.client.gui;

import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRevision;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Environment(EnvType.CLIENT)
final class ModuleListRows {

    private static final float SNAP = 0.005F;

    private static final Comparator<ModuleListRows.Row> WIDEST_FIRST =
            Comparator.<ModuleListRows.Row>comparingDouble(row -> -row.nameW)
                    .thenComparing(row -> row.module.name());

    private static final Comparator<ModuleListRows.Row> WIDEST_LAST = WIDEST_FIRST.reversed();

    private final List<ModuleListRows.Row> rows = new ArrayList<>();

    private final List<ModuleListRows.Row> view = Collections.unmodifiableList(this.rows);

    private List<ClientModule> seenEnabled;

    private long seenRevision = -9223372036854775808L;

    private boolean seenEditMode;

    private boolean unsorted;

    private boolean sortedBottomFirst;

    ModuleListRows() {}

    void update(List<ClientModule> seenEnabled2, boolean seenEditMode2, float f) {
        long l = ModuleRevision.current();
        if (seenEnabled2 != this.seenEnabled
                || l != this.seenRevision
                || seenEditMode2 != this.seenEditMode) {
            this.seenEnabled = seenEnabled2;
            this.seenRevision = l;
            this.seenEditMode = seenEditMode2;
            this.rows.removeIf(
                    row ->
                            !listed(row.module, seenEditMode2)
                                    || row.module.isEnabled()
                                            && !seenEnabled2.contains(row.module));
            for (ClientModule module2 : seenEnabled2) {
                if (listed(module2, seenEditMode2) && !this.hasRow(module2)) {
                    this.rows.add(new ModuleListRows.Row(module2));
                    this.unsorted = true;
                }
            }
        }
        float g = 1.0F - (float) Math.exp(-12.0F * f);
        for (int i = this.rows.size() - 1; i >= 0; i--) {
            ModuleListRows.Row row2 = this.rows.get(i);
            float h = row2.module.isEnabled() ? 1.0F : 0.0F;
            if (row2.anim != h) {
                float f2 = row2.anim + (h - row2.anim) * g;
                if (Math.abs(f2 - h) < 0.005F) {
                    f2 = h;
                }
                row2.anim = f2;
                if (f2 == 0.0F) {
                    this.rows.remove(i);
                }
            }
        }
    }

    <F> List<ModuleListRows.Row> ordered(
            F f, int i, ModuleListRows.TextWidth<F> textWidth, boolean sortedBottomFirst2) {
        for (int j = 0; j < this.rows.size(); j++) {
            ModuleListRows.Row row = this.rows.get(j);
            if (row.measuredPx != i || row.measuredFont != f) {
                row.nameW = textWidth.of(f, i, row.module.name());
                row.measuredFont = f;
                row.measuredPx = i;
                this.unsorted = true;
            }
        }
        if (this.unsorted || sortedBottomFirst2 != this.sortedBottomFirst) {
            this.rows.sort(sortedBottomFirst2 ? WIDEST_LAST : WIDEST_FIRST);
            this.unsorted = false;
            this.sortedBottomFirst = sortedBottomFirst2;
        }
        return this.view;
    }

    private static boolean listed(ClientModule module2, boolean bl) {
        return module2.category() != ModuleCategory.CLIENT
                && (bl || module2.showInHud() == null || module2.showInHud().get());
    }

    private boolean hasRow(ClientModule module2) {
        for (int i = 0; i < this.rows.size(); i++) {
            if (this.rows.get(i).module == module2) {
                return true;
            }
        }
        return false;
    }

    @Environment(EnvType.CLIENT)
    static final class Row {

        private final ClientModule module;

        private float anim;

        private float nameW;

        private Object measuredFont;

        private int measuredPx = -1;

        private Row(ClientModule module2) {
            this.module = module2;
        }

        ClientModule module() {
            return this.module;
        }

        float anim() {
            return this.anim;
        }

        float nameW() {
            return this.nameW;
        }
    }

    @FunctionalInterface
    @Environment(EnvType.CLIENT)
    interface TextWidth<F> {

        float of(F f, int i, String string);
    }
}
