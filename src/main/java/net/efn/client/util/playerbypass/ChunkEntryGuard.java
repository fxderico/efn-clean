package net.efn.client.util.playerbypass;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.LongPredicate;

@Environment(EnvType.CLIENT)
final class ChunkEntryGuard {

    private final LongOpenHashSet waiting = new LongOpenHashSet();

    private boolean inChunk;

    private long chunk;

    private boolean entryPending;

    ChunkEntryGuard() {}

    void reset() {
        this.inChunk = false;
        this.entryPending = false;
        this.waiting.clear();
    }

    void update(
            long chunk2,
            LongPredicate longPredicate,
            LongPredicate longPredicate2,
            ProtectedChunkStore protectedChunkStore) {
        if (!this.inChunk || chunk2 != this.chunk) {
            this.inChunk = true;
            this.chunk = chunk2;
            this.entryPending = true;
            this.waiting.clear();
        }
        if (this.entryPending) {
            if (longPredicate.test(chunk2)) {
                this.entryPending = false;
                if (!longPredicate2.test(chunk2)) {
                    ProtectedChunkStore.forEachInArea(
                            ProtectedChunkStore.chunkX(chunk2),
                            ProtectedChunkStore.chunkZ(chunk2),
                            lx -> {
                                if (!longPredicate.test(lx)) {
                                    this.waiting.add(lx);
                                } else if (!longPredicate2.test(lx)) {
                                    protectedChunkStore.protect(lx);
                                }
                            });
                }
            }
        } else if (!this.waiting.isEmpty()) {
            LongIterator longIterator = this.waiting.iterator();
            while (longIterator.hasNext()) {
                long l = longIterator.nextLong();
                if (longPredicate.test(l)) {
                    longIterator.remove();
                    if (!longPredicate2.test(l)) {
                        protectedChunkStore.protect(l);
                    }
                }
            }
        }
    }
}
