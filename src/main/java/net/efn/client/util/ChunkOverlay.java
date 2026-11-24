package net.efn.client.util;

import it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public final class ChunkOverlay {

    public static final double MARKER_Y = 63.0;

    private static final float EDGE_SHADE = 0.77F;

    private static final float LINE_WIDTH = 1.5F;

    private static final double CORNER_RADIUS = 2.0;

    private static final int ARC_SEGMENTS = 4;

    private static final float GROW_SECONDS = 0.3F;

    private static final float SHRINK_SECONDS = 0.25F;

    private static final float SETTLE_SECONDS = 0.25F;

    private static final float MAX_WAIT_SECONDS = 1.0F;

    private static final float RESET_GAP_SECONDS = 0.5F;

    private final Long2FloatOpenHashMap shown = new Long2FloatOpenHashMap();

    private final LongOpenHashSet pending = new LongOpenHashSet();

    private final LongOpenHashSet done = new LongOpenHashSet();

    private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

    private final LongArrayList group = new LongArrayList();

    private long lastNanos;

    private long pendingSince;

    private long lastAdded;

    private static final double[] OUTLINE = new double[40];

    private static int points;

    public ChunkOverlay() {}

    public void draw(LongSet longSet, int i, float f) {
        long l = System.nanoTime();
        float g = this.lastNanos == 0L ? 0.0F : (float) (l - this.lastNanos) / 1.0E9F;
        this.lastNanos = l;
        if (g > 0.5F) {
            this.shown.clear();
            this.pending.clear();
        }
        this.collect(longSet, l);
        this.step(longSet, g);
        if (!this.shown.isEmpty()) {
            int withAlpha =
                    ColorHelper.withAlpha(
                            Math.min(255, ColorHelper.getAlpha(i) * 3 / 2),
                            ColorHelper.scaleRgb(i, 0.77F));
            FoglessLines foglessLines = FoglessLines.forFrame(f);
            this.done.clear();
            LongIterator longIterator = this.shown.keySet().iterator();
            while (longIterator.hasNext()) {
                long l2 = longIterator.nextLong();
                if (this.done.add(l2)) {
                    this.drawGroup(foglessLines, longSet, l2, i, withAlpha);
                }
            }
        }
    }

    public void drawTiles(LongSet longSet, int i, int j, float f) {
        long l = System.nanoTime();
        float g = this.lastNanos == 0L ? 0.0F : (float) (l - this.lastNanos) / 1.0E9F;
        this.lastNanos = l;
        if (g > 0.5F) {
            this.shown.clear();
        }
        LongIterator longIterator = longSet.iterator();
        while (longIterator.hasNext()) {
            this.shown.putIfAbsent(longIterator.nextLong(), 0.0F);
        }
        this.step(longSet, g);
        if (!this.shown.isEmpty()) {
            FoglessLines foglessLines = FoglessLines.forFrame(f);
            ObjectIterator objectIterator = this.shown.long2FloatEntrySet().fastIterator();
            while (objectIterator.hasNext()) {
                Entry entry = (Entry) objectIterator.next();
                float easeOut = easeOut(entry.getFloatValue());
                if (!(easeOut <= 0.001F)) {
                    long longKey = entry.getLongKey();
                    int packedX = ChunkPos.getPackedX(longKey);
                    int packedZ = ChunkPos.getPackedZ(longKey);
                    if (!this.settled(longSet, longKey)) {
                        double d = 8.0 * easeOut;
                        double e = (packedX << 4) + 8.0;
                        double d2 = (packedZ << 4) + 8.0;
                        tile(
                                foglessLines,
                                e - d,
                                d2 - d,
                                e + d,
                                d2 + d,
                                2.0 * easeOut,
                                true,
                                true,
                                true,
                                true,
                                fade(i, easeOut),
                                fade(j, easeOut));
                    } else {
                        double e2 = packedX << 4;
                        double d3 = packedZ << 4;
                        double e3 = e2 + 16.0;
                        double d4 = d3 + 16.0;
                        boolean bl = !this.settled(longSet, ChunkPos.toLong(packedX, packedZ - 1));
                        boolean bl2 = !this.settled(longSet, ChunkPos.toLong(packedX, packedZ + 1));
                        boolean bl3 = !this.settled(longSet, ChunkPos.toLong(packedX - 1, packedZ));
                        boolean bl4 = !this.settled(longSet, ChunkPos.toLong(packedX + 1, packedZ));
                        tile(foglessLines, e2, d3, e3, d4, 2.0, bl, bl2, bl3, bl4, i, j);
                    }
                }
            }
        }
    }

    private boolean settled(LongSet longSet, long l) {
        return this.shown.get(l) >= 1.0F && longSet.contains(l);
    }

    private void collect(LongSet longSet, long pendingSince2) {
        LongIterator longIterator = longSet.iterator();
        while (longIterator.hasNext()) {
            long l = longIterator.nextLong();
            if (!this.shown.containsKey(l) && !this.pending.contains(l)) {
                if (this.pending.isEmpty()) {
                    this.pendingSince = pendingSince2;
                }
                this.pending.add(l);
                this.lastAdded = pendingSince2;
            }
        }
        longIterator = this.pending.iterator();
        while (longIterator.hasNext()) {
            if (!longSet.contains(longIterator.nextLong())) {
                longIterator.remove();
            }
        }
        if (!this.pending.isEmpty()) {
            if (!((float) (pendingSince2 - this.lastAdded) / 1.0E9F < 0.25F)
                    || !((float) (pendingSince2 - this.pendingSince) / 1.0E9F < 1.0F)) {
                longIterator = this.pending.iterator();
                while (longIterator.hasNext()) {
                    this.shown.put(longIterator.nextLong(), 0.0F);
                }
                this.pending.clear();
            }
        }
    }

    private void step(LongSet longSet, float f) {
        ObjectIterator objectIterator = this.shown.long2FloatEntrySet().fastIterator();
        while (objectIterator.hasNext()) {
            Entry entry = (Entry) objectIterator.next();
            if (longSet.contains(entry.getLongKey())) {
                entry.setValue(Math.min(1.0F, entry.getFloatValue() + f / 0.3F));
            } else {
                float g = entry.getFloatValue() - f / 0.25F;
                if (g <= 0.0F) {
                    objectIterator.remove();
                } else {
                    entry.setValue(g);
                }
            }
        }
    }

    private void drawGroup(FoglessLines foglessLines, LongSet longSet, long l, int i, int j) {
        float f = this.shown.get(l);
        boolean bl = longSet.contains(l);
        this.group.clear();
        this.queue.enqueue(l);
        double d = 0.0;
        double e = 0.0;
        while (!this.queue.isEmpty()) {
            long dequeueLong = this.queue.dequeueLong();
            this.group.add(dequeueLong);
            int packedX = ChunkPos.getPackedX(dequeueLong);
            int packedZ = ChunkPos.getPackedZ(dequeueLong);
            d += (packedX << 4) + 8.0;
            e += (packedZ << 4) + 8.0;
            this.visit(longSet, ChunkPos.toLong(packedX, packedZ - 1), f, bl);
            this.visit(longSet, ChunkPos.toLong(packedX, packedZ + 1), f, bl);
            this.visit(longSet, ChunkPos.toLong(packedX - 1, packedZ), f, bl);
            this.visit(longSet, ChunkPos.toLong(packedX + 1, packedZ), f, bl);
        }
        float easeOut = easeOut(f);
        if (!(easeOut <= 0.001F)) {
            double d2 = d / this.group.size();
            double e2 = e / this.group.size();
            int k = easeOut >= 1.0F ? i : fade(i, easeOut);
            int m = easeOut >= 1.0F ? j : fade(j, easeOut);
            for (int p = 0; p < this.group.size(); p++) {
                long l2 = this.group.getLong(p);
                int packedX2 = ChunkPos.getPackedX(l2);
                int packedZ2 = ChunkPos.getPackedZ(l2);
                double d3 = d2 + ((packedX2 << 4) - d2) * easeOut;
                double e3 = e2 + ((packedZ2 << 4) - e2) * easeOut;
                double d4 = d3 + 16.0 * easeOut;
                double e4 = e3 + 16.0 * easeOut;
                boolean bl2 = !this.joined(longSet, ChunkPos.toLong(packedX2, packedZ2 - 1), f, bl);
                boolean bl3 = !this.joined(longSet, ChunkPos.toLong(packedX2, packedZ2 + 1), f, bl);
                boolean bl4 = !this.joined(longSet, ChunkPos.toLong(packedX2 - 1, packedZ2), f, bl);
                boolean bl5 = !this.joined(longSet, ChunkPos.toLong(packedX2 + 1, packedZ2), f, bl);
                tile(foglessLines, d3, e3, d4, e4, 2.0 * easeOut, bl2, bl3, bl4, bl5, k, m);
            }
        }
    }

    private void visit(LongSet longSet, long l, float f, boolean bl) {
        if (this.joined(longSet, l, f, bl) && this.done.add(l)) {
            this.queue.enqueue(l);
        }
    }

    private boolean joined(LongSet longSet, long l, float f, boolean bl) {
        return this.shown.containsKey(l) && this.shown.get(l) == f && longSet.contains(l) == bl;
    }

    private static void tile(
            FoglessLines foglessLines,
            double d,
            double e,
            double d2,
            double e2,
            double d3,
            boolean bl,
            boolean bl2,
            boolean bl3,
            boolean bl4,
            int i,
            int j) {
        double e3 = bl && bl3 ? d3 : 0.0;
        double d4 = bl && bl4 ? d3 : 0.0;
        double e4 = bl2 && bl4 ? d3 : 0.0;
        double d5 = bl2 && bl3 ? d3 : 0.0;
        points = 0;
        corner(foglessLines, d + e3, e + e3, e3, 180.0, j);
        corner(foglessLines, d2 - d4, e + d4, d4, 270.0, j);
        corner(foglessLines, d2 - e4, e2 - e4, e4, 0.0, j);
        corner(foglessLines, d + d5, e2 - d5, d5, 90.0, j);
        WorldQuads worldQuads = WorldQuads.layer(true);
        double e5 = 63.0;
        for (int k = 1; k + 1 < points; k++) {
            worldQuads.quad(
                    OUTLINE[0],
                    e5,
                    OUTLINE[1],
                    OUTLINE[k * 2],
                    e5,
                    OUTLINE[k * 2 + 1],
                    OUTLINE[k * 2 + 2],
                    e5,
                    OUTLINE[k * 2 + 3],
                    OUTLINE[k * 2 + 2],
                    e5,
                    OUTLINE[k * 2 + 3],
                    i);
        }
        if (bl) {
            edge(foglessLines, d + e3, e, d2 - d4, e, j);
        }
        if (bl2) {
            edge(foglessLines, d + d5, e2, d2 - e4, e2, j);
        }
        if (bl3) {
            edge(foglessLines, d, e + e3, d, e2 - d5, j);
        }
        if (bl4) {
            edge(foglessLines, d2, e + d4, d2, e2 - e4, j);
        }
    }

    private static void corner(
            FoglessLines foglessLines, double d, double e, double d2, double e2, int i) {
        if (d2 <= 0.0) {
            point(d, e);
        } else {
            double d3 = 0.0;
            double e3 = 0.0;
            for (int j = 0; j <= 4; j++) {
                double d4 = Math.toRadians(e2 + 90.0 * j / 4.0);
                double e4 = d + Math.cos(d4) * d2;
                double d5 = e + Math.sin(d4) * d2;
                point(e4, d5);
                if (j > 0) {
                    foglessLines.line(d3, 63.0, e3, e4, 63.0, d5, i, 1.5F);
                }
                d3 = e4;
                e3 = d5;
            }
        }
    }

    private static void point(double d, double e) {
        OUTLINE[points * 2] = d;
        OUTLINE[points * 2 + 1] = e;
        points++;
    }

    private static float easeOut(float f) {
        float g = 1.0F - f;
        return 1.0F - g * g * g;
    }

    private static int fade(int i, float f) {
        return ColorHelper.withAlpha(Math.round(ColorHelper.getAlpha(i) * f), i);
    }

    private static void edge(
            FoglessLines foglessLines, double d, double e, double d2, double e2, int i) {
        foglessLines.line(d, 63.0, e, d2, 63.0, e2, i, 1.5F);
    }
}
