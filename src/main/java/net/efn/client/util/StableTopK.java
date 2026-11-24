package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Environment(EnvType.CLIENT)
public final class StableTopK<T> {

    private final Comparator<? super T> priority;

    private final Comparator<StableTopK.Entry<T>> ranked;

    private final PriorityQueue<StableTopK.Entry<T>> heap;

    private final ArrayList<StableTopK.Entry<T>> ordered = new ArrayList<>();

    private final ArrayList<T> result = new ArrayList<>();

    private int limit;

    private long encountered;

    private boolean overflow;

    public StableTopK(Comparator<? super T> priority2) {
        this.priority = priority2;
        this.ranked =
                (entry, entry2) -> {
                    int compare = priority2.compare(entry.value(), entry2.value());
                    return compare != 0 ? compare : Long.compare(entry.order(), entry2.order());
                };
        this.heap = new PriorityQueue<>(this.ranked.reversed());
    }

    public void reset(int i) {
        this.limit = Math.max(0, i);
        this.encountered = 0L;
        this.overflow = false;
        this.heap.clear();
        this.ordered.clear();
        this.result.clear();
    }

    public void offer(T t) {
        long l = this.encountered++;
        if (this.heap.size() < this.limit) {
            this.heap.add(new StableTopK.Entry<>((T) t, l));
            this.result.add((T) t);
        } else {
            this.overflow = true;
            if (this.limit != 0 && this.priority.compare((T) t, this.heap.peek().value()) < 0) {
                this.heap.poll();
                this.heap.add(new StableTopK.Entry<>((T) t, l));
            }
        }
    }

    public T worst() {
        return this.limit > 0 && this.heap.size() == this.limit ? this.heap.peek().value() : null;
    }

    public List<T> result() {
        if (this.overflow) {
            this.ordered.clear();
            this.ordered.addAll(this.heap);
            this.ordered.sort(this.ranked);
            this.result.clear();
            for (StableTopK.Entry entry : this.ordered) {
                this.result.add((T) entry.value());
            }
        }
        return this.result;
    }

    @Environment(EnvType.CLIENT)
    private record Entry<T>(T value, long order) {}
}
