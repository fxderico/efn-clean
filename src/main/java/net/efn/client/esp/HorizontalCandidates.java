package net.efn.client.esp;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

@Environment(EnvType.CLIENT)
public final class HorizontalCandidates<T> {

    private final ToDoubleFunction<T> x;

    private final ToDoubleFunction<T> z;

    private final Long2ObjectOpenHashMap<IntArrayList> regions = new Long2ObjectOpenHashMap<>();

    private final IntArrayList selected = new IntArrayList();

    private final ArrayList<T> candidates = new ArrayList<>();

    private List<T> indexed;

    private int cameraX;

    private int cameraZ;

    private double range;

    private boolean ready;

    public HorizontalCandidates(
            ToDoubleFunction<T> toDoubleFunction, ToDoubleFunction<T> toDoubleFunction2) {
        this.x = toDoubleFunction;
        this.z = toDoubleFunction2;
    }

    public List<T> select(List<T> indexed2, double d, double e, double range2) {
        if (indexed2.size() >= 64
                && Double.isFinite(d)
                && Double.isFinite(e)
                && Double.isFinite(range2)
                && !(range2 < 0.0)
                && !(range2 > 1000000.0)) {
            if (this.indexed != indexed2) {
                if (this.indexed != null && indexed2.size() < this.indexed.size() / 4) {
                    this.clear();
                }
                this.regions.clear();
                for (int i = 0; i < indexed2.size(); i++) {
                    Object object = indexed2.get(i);
                    this.regions
                            .computeIfAbsent(
                                    key(
                                            region(this.x.applyAsDouble((T) object)),
                                            region(this.z.applyAsDouble((T) object))),
                                    value -> new IntArrayList())
                            .add(i);
                }
                this.indexed = indexed2;
                this.ready = false;
            }
            int j = (int) Math.floor(d / 16.0);
            int k = (int) Math.floor(e / 16.0);
            if (this.ready && this.cameraX == j && this.cameraZ == k && this.range == range2) {
                return this.candidates;
            } else {
                this.selected.clear();
                this.candidates.clear();
                int region = region(j * 16.0 - range2);
                int region2 = region((j + 1.0) * 16.0 + range2);
                int region3 = region(k * 16.0 - range2);
                int region4 = region((k + 1.0) * 16.0 + range2);
                for (int q = region; q <= region2; q++) {
                    for (int r = region3; r <= region4; r++) {
                        IntArrayList region5 = this.regions.get(key(q, r));
                        if (region5 != null) {
                            this.selected.addAll(region5);
                        }
                    }
                }
                IntArrays.quickSort(this.selected.elements(), 0, this.selected.size());
                for (int t = 0; t < this.selected.size(); t++) {
                    this.candidates.add((T) indexed2.get(this.selected.getInt(t)));
                }
                this.cameraX = j;
                this.cameraZ = k;
                this.range = range2;
                this.ready = true;
                return this.candidates;
            }
        } else {
            if (this.indexed != null) {
                this.clear();
            }
            return indexed2;
        }
    }

    public void clear() {
        this.indexed = null;
        this.regions.clear();
        this.regions.trim();
        this.selected.clear();
        this.selected.trim();
        this.candidates.clear();
        this.candidates.trimToSize();
        this.ready = false;
    }

    private static int region(double d) {
        return (int) Math.floor(d / 256.0);
    }

    private static long key(int i, int j) {
        return i & 4294967295L | (long) j << 32;
    }
}
