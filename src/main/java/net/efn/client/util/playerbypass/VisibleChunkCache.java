package net.efn.client.util.playerbypass;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.LongPredicate;

@Environment(EnvType.CLIENT)
public final class VisibleChunkCache {

    private final int radius;

    private final LongOpenHashSet bases = new LongOpenHashSet();

    private final LongOpenHashSet suspects = new LongOpenHashSet();

    private final LongOpenHashSet nextBases = new LongOpenHashSet();

    private final LongOpenHashSet nextSuspects = new LongOpenHashSet();

    private boolean valid;

    private long revision;

    private int centerX;

    private int centerZ;

    public VisibleChunkCache(int radius2) {
        if (radius2 >= 0 && radius2 <= 512) {
            this.radius = radius2;
        } else {
            throw new IllegalArgumentException("Invalid chunk radius");
        }
    }

    public boolean update(
            long revision2,
            int centerX2,
            int centerZ2,
            Long2ObjectMap<ChunkLightSnapshot> long2ObjectMap,
            VisibleChunkCache.Flagged flagged,
            LongPredicate longPredicate) {
        if (this.valid
                && this.revision == revision2
                && this.centerX == centerX2
                && this.centerZ == centerZ2) {
            return false;
        } else {
            this.nextBases.clear();
            this.nextSuspects.clear();
            int i = this.radius * 2 + 1;
            if (long2ObjectMap.size() <= i * i) {
                for (Entry long2ObjectEntry : long2ObjectMap.long2ObjectEntrySet()) {
                    long longKey = long2ObjectEntry.getLongKey();
                    if (Math.abs((long) ((int) longKey) - centerX2) <= this.radius
                            && Math.abs((long) ((int) (longKey >>> 32)) - centerZ2)
                                    <= this.radius) {
                        this.accept(
                                longKey,
                                (ChunkLightSnapshot) long2ObjectEntry.getValue(),
                                flagged,
                                longPredicate);
                    }
                }
            } else {
                for (int m = -this.radius; m <= this.radius; m++) {
                    for (int n = -this.radius; n <= this.radius; n++) {
                        long l = centerX2 + m & 4294967295L | (long) (centerZ2 + n) << 32;
                        ChunkLightSnapshot chunkLightSnapshot =
                                (ChunkLightSnapshot) long2ObjectMap.get(l);
                        if (chunkLightSnapshot != null) {
                            this.accept(l, chunkLightSnapshot, flagged, longPredicate);
                        }
                    }
                }
            }
            boolean bl =
                    !this.valid
                            || !this.bases.equals(this.nextBases)
                            || !this.suspects.equals(this.nextSuspects);
            if (bl) {
                this.bases.clear();
                this.bases.addAll(this.nextBases);
                this.suspects.clear();
                this.suspects.addAll(this.nextSuspects);
            }
            this.revision = revision2;
            this.centerX = centerX2;
            this.centerZ = centerZ2;
            this.valid = true;
            return bl;
        }
    }

    private void accept(
            long l,
            ChunkLightSnapshot chunkLightSnapshot,
            VisibleChunkCache.Flagged flagged,
            LongPredicate longPredicate) {
        if (flagged.test(l, chunkLightSnapshot)) {
            (longPredicate.test(l) ? this.nextSuspects : this.nextBases).add(l);
        }
    }

    public LongOpenHashSet bases() {
        return this.bases;
    }

    public LongOpenHashSet suspects() {
        return this.suspects;
    }

    public void invalidate() {
        this.valid = false;
    }

    @FunctionalInterface
    @Environment(EnvType.CLIENT)
    public interface Flagged {

        boolean test(long l, ChunkLightSnapshot chunkLightSnapshot);
    }
}
