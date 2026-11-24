package net.efn.client.util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class ChunkAreas {

    private final DoubleArrayList areaX = new DoubleArrayList();

    private final DoubleArrayList areaZ = new DoubleArrayList();

    private int areasSize = -1;

    private int areasHash;

    private final LongOpenHashSet seen = new LongOpenHashSet();

    private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

    private final LongArrayList area = new LongArrayList();

    public ChunkAreas() {}

    public void drawTracers(LongSet longSet, double d, int i) {
        if (!longSet.isEmpty()) {
            this.find(longSet);
            for (int j = 0; j < this.areaX.size(); j++) {
                TracerEngine.draw(
                        new Vec3d(this.areaX.getDouble(j), d, this.areaZ.getDouble(j)), i);
            }
        }
    }

    private void find(LongSet longSet) {
        int i = longSet.hashCode();
        if (longSet.size() != this.areasSize || i != this.areasHash) {
            this.areasSize = longSet.size();
            this.areasHash = i;
            this.areaX.clear();
            this.areaZ.clear();
            this.seen.clear();
            LongIterator longIterator = longSet.iterator();
            while (longIterator.hasNext()) {
                long l = longIterator.next();
                if (this.seen.add(l)) {
                    this.area.clear();
                    this.queue.enqueue(l);
                    double d = 0.0;
                    double e = 0.0;
                    while (!this.queue.isEmpty()) {
                        long dequeueLong = this.queue.dequeueLong();
                        this.area.add(dequeueLong);
                        int packedX = ChunkPos.getPackedX(dequeueLong);
                        int packedZ = ChunkPos.getPackedZ(dequeueLong);
                        d += (packedX << 4) + 8.0;
                        e += (packedZ << 4) + 8.0;
                        for (int m = -1; m <= 1; m++) {
                            for (int n = -1; n <= 1; n++) {
                                long l2 = ChunkPos.toLong(packedX + m, packedZ + n);
                                if (longSet.contains(l2) && this.seen.add(l2)) {
                                    this.queue.enqueue(l2);
                                }
                            }
                        }
                    }
                    double d2 = d / this.area.size();
                    double e2 = e / this.area.size();
                    if (!longSet.contains(
                            ChunkPos.toLong(
                                    (int) Math.floor(d2) >> 4, (int) Math.floor(e2) >> 4))) {
                        double d3 = 1.7976931348623157E308;
                        double e3 = d2;
                        double d4 = e2;
                        for (int o = 0; o < this.area.size(); o++) {
                            long l3 = this.area.getLong(o);
                            double e4 = (ChunkPos.getPackedX(l3) << 4) + 8.0;
                            double d5;
                            double e5 =
                                    (e4 - d2) * (e4 - d2)
                                            + ((d5 = (ChunkPos.getPackedZ(l3) << 4) + 8.0) - e2)
                                                    * (d5 - e2);
                            if (e5 < d3) {
                                d3 = e5;
                                e3 = e4;
                                d4 = d5;
                            }
                        }
                        d2 = e3;
                        e2 = d4;
                    }
                    this.areaX.add(d2);
                    this.areaZ.add(e2);
                }
            }
        }
    }
}
