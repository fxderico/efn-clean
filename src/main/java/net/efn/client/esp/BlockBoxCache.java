package net.efn.client.esp;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import net.efn.client.util.BlockBoxHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.function.LongFunction;

@Environment(EnvType.CLIENT)
public final class BlockBoxCache {

    private final int capacity;

    private final LongFunction<Box> factory;

    private final Long2ObjectLinkedOpenHashMap<BlockBoxCache.Entry> boxes =
            new Long2ObjectLinkedOpenHashMap<>();

    private float size = 0.0F / 0.0F;

    private long frame;

    BlockBoxCache(int i) {
        this(i, null);
    }

    public BlockBoxCache(int i, LongFunction<Box> factory2) {
        this.capacity = Math.max(1, i);
        this.factory = factory2;
    }

    public void beginFrame(float size2) {
        if (Float.floatToIntBits(this.size) != Float.floatToIntBits(size2)) {
            this.boxes.clear();
            this.size = size2;
        }
        this.frame++;
    }

    public Box box(long l) {
        BlockBoxCache.Entry andMoveToLast = this.boxes.getAndMoveToLast(l);
        if (andMoveToLast != null) {
            andMoveToLast.frame = this.frame;
            return andMoveToLast.box;
        } else {
            Box box2 =
                    this.factory == null
                            ? BlockBoxHelper.scaledBlockBox(BlockPos.fromLong(l), this.size)
                            : this.factory.apply(l);
            if (this.boxes.size() == this.capacity) {
                if (this.boxes.get(this.boxes.firstLongKey()).frame == this.frame) {
                    return box2;
                }
                this.boxes.removeFirst();
            }
            this.boxes.put(l, new BlockBoxCache.Entry(box2, this.frame));
            return box2;
        }
    }

    int size() {
        return this.boxes.size();
    }

    @Environment(EnvType.CLIENT)
    private static final class Entry {

        final Box box;

        long frame;

        Entry(Box box2, long frame2) {
            this.box = box2;
            this.frame = frame2;
        }
    }
}
