package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.LongSupplier;

@Environment(EnvType.CLIENT)
public final class TickWorkBudget {

    private final LongSupplier clock;

    private long limit;

    private long used;

    public TickWorkBudget(LongSupplier clock2) {
        this.clock = clock2;
    }

    public void reset(long l) {
        this.limit = Math.max(0L, l);
        this.used = 0L;
    }

    public long acquire(long l) {
        long l2 = this.clock.getAsLong();
        long l3 = Math.max(0L, this.limit - this.used);
        long l4 = Math.min(Math.max(0L, l), Math.min(l3, this.limit / 3L));
        return l2 + l4;
    }

    public void record(long l) {
        this.used = this.used + Math.max(0L, this.clock.getAsLong() - l);
    }

    public long usedNanos() {
        return this.used;
    }
}
