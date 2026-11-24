/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.esp;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.module.impl.client.PerformanceModule;
import net.efn.client.util.BlockBoxHelper;
import net.efn.client.util.BlockClusterGeometry;
import net.efn.client.util.TracerEngine;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongPredicate;
import java.util.function.Supplier;

@Environment(value = EnvType.CLIENT)
public final class BlockLayer {

    private static final long REVEAL_NANOS = 380000000L;

    private static final double FADE_START = 64.0;

    private static final double FADE_END = 256.0;

    private static final float FADE_FLOOR = 0.45f;

    private final Palette palette;

    private final Supplier<Object> dimension;

    private final Map<Object, World> worlds = new HashMap<Object, World>();

    public BlockLayer(Palette palette2, Supplier<Object> dimension2) {
        this.palette = palette2;
        this.dimension = dimension2;
    }

    public void put(long l, Object object) {
        World world = this.world(true);
        if (world == null) {
            return;
        }
        Found position = world.positions.get(l);
        if (position != null && Objects.equals(position.kind(), object)) {
            return;
        }
        world.positions.put(l, new Found(object, System.nanoTime()));
        world.byChunk.computeIfAbsent(BlockLayer.chunkOf(l), value -> new LongOpenHashSet()).add(l);
        BlockLayer.markDirty(world, l);
    }

    public void remove(long l) {
        World world = this.world(false);
        if (world == null || world.positions.remove(l) == null) {
            return;
        }
        long chunk = BlockLayer.chunkOf(l);
        LongOpenHashSet longOpenHashSet = world.byChunk.get(chunk);
        if (longOpenHashSet != null && longOpenHashSet.remove(l) && longOpenHashSet.isEmpty()) {
            world.byChunk.remove(chunk);
        }
        BlockLayer.markDirty(world, l);
    }

    public void clear() {
        this.worlds.clear();
    }

    public void retainChunk(long l, LongPredicate longPredicate) {
        World world = this.world(false);
        if (world == null) {
            return;
        }
        LongOpenHashSet longOpenHashSet = world.byChunk.get(l);
        if (longOpenHashSet == null) {
            return;
        }
        for (long l2 : longOpenHashSet.toLongArray()) {
            if (longPredicate.test(l2)) continue;
            this.remove(l2);
        }
    }

    public List<Vein> veins() {
        World world = this.world(false);
        if (world == null) {
            return List.of();
        }
        this.rebuildDirty(world);
        if (world.veinsDirty) {
            world.veins = BlockLayer.joinVeins(world);
            world.veinsDirty = false;
        }
        return world.veins;
    }

    private World world(boolean bl) {
        Object dimension2 = this.dimension.get();
        if (dimension2 == null) {
            return null;
        }
        return bl
                ? this.worlds.computeIfAbsent(dimension2, value -> new World())
                : this.worlds.get(dimension2);
    }

    private static long chunkKey(int i, int j) {
        return (long) i & 0xFFFFFFFFL | (long) j << 32;
    }

    private static long chunkOf(long l) {
        return BlockLayer.chunkKey(BlockPos.unpackLongX(l) >> 4, BlockPos.unpackLongZ(l) >> 4);
    }

    private static long regionOf(long l) {
        return BlockLayer.chunkKey((int) l >> 4, (int) (l >> 32) >> 4);
    }

    private static void markDirty(World world, long l) {
        ChunkScanner.ScanBatch scanBatch2 = ChunkScanner.deliveryBatch();
        LongOpenHashSet dirty2 = world.dirty;
        if (scanBatch2 != null && !scanBatch2.finished) {
            if (world.scanBatch != scanBatch2) {
                BlockLayer.flushScanDirty(world);
            }
            world.scanBatch = scanBatch2;
            dirty2 = world.scanDirty;
        }
        int unpackLongX = BlockPos.unpackLongX(l);
        int unpackLongZ = BlockPos.unpackLongZ(l);
        int i = unpackLongX >> 4;
        int j = unpackLongZ >> 4;
        dirty2.add(BlockLayer.chunkKey(i, j));
        if ((unpackLongX & 0xF) == 0) {
            dirty2.add(BlockLayer.chunkKey(i - 1, j));
        }
        if ((unpackLongX & 0xF) == 15) {
            dirty2.add(BlockLayer.chunkKey(i + 1, j));
        }
        if ((unpackLongZ & 0xF) == 0) {
            dirty2.add(BlockLayer.chunkKey(i, j - 1));
        }
        if ((unpackLongZ & 0xF) == 15) {
            dirty2.add(BlockLayer.chunkKey(i, j + 1));
        }
    }

    private static void flushScanDirty(World world) {
        world.dirty.addAll(world.scanDirty);
        world.scanDirty.clear();
        world.scanBatch = null;
    }

    private void rebuildDirty(World world) {
        if (world.scanBatch != null
                && (world.scanBatch.finished || !PerformanceModule.optimizationsEnabled())) {
            BlockLayer.flushScanDirty(world);
        }
        if (world.dirty.isEmpty()) {
            return;
        }
        LongIterator longIterator = world.dirty.iterator();
        while (longIterator.hasNext()) {
            long l = longIterator.next();
            this.rebuildChunk(world, l);
        }
        world.dirty.clear();
        world.veinsDirty = true;
    }

    private void rebuildChunk(World world, long l) {
        Object object;
        LongOpenHashSet longOpenHashSet = world.byChunk.get(l);
        if (longOpenHashSet == null) {
            if (world.shapes.remove(l) != null) {
                LongOpenHashSet shapeRegion = world.shapeRegions.get(BlockLayer.regionOf(l));
                if (shapeRegion != null && shapeRegion.remove(l) && shapeRegion.isEmpty()) {
                    world.shapeRegions.remove(BlockLayer.regionOf(l));
                }
                ++world.shapeRevision;
            }
            return;
        }
        LinkedHashMap<Object, List> map = new LinkedHashMap<Object, List>();
        int mAX_VALUE = Integer.MAX_VALUE;
        int mAX_VALUE2 = Integer.MAX_VALUE;
        int mAX_VALUE3 = Integer.MAX_VALUE;
        int mIN_VALUE = Integer.MIN_VALUE;
        int mIN_VALUE2 = Integer.MIN_VALUE;
        int mIN_VALUE3 = Integer.MIN_VALUE;
        LongIterator longIterator = longOpenHashSet.iterator();
        while (longIterator.hasNext()) {
            long l2 = longIterator.next();
            object = BlockPos.fromLong(l2);
            map.computeIfAbsent(world.positions.get(l2).kind(), value -> new ArrayList())
                    .add(object);
            mAX_VALUE = Math.min(mAX_VALUE, ((Vec3i) object).getX());
            mIN_VALUE = Math.max(mIN_VALUE, ((Vec3i) object).getX());
            mAX_VALUE2 = Math.min(mAX_VALUE2, ((Vec3i) object).getY());
            mIN_VALUE2 = Math.max(mIN_VALUE2, ((Vec3i) object).getY());
            mAX_VALUE3 = Math.min(mAX_VALUE3, ((Vec3i) object).getZ());
            mIN_VALUE3 = Math.max(mIN_VALUE3, ((Vec3i) object).getZ());
        }
        int m = (int) l;
        int n = (int) (l >> 32);
        object = new ArrayList();
        for (Map.Entry entry : map.entrySet()) {
            Object key = entry.getKey();
            BlockClusterGeometry.Membership membership =
                    (i, j, k) -> {
                        Found position = world.positions.get(BlockPos.asLong(i, j, k));
                        return position != null && Objects.equals(position.kind(), key);
                    };
            for (List<BlockPos> list :
                    (Iterable<List<BlockPos>>)
                            BlockClusterGeometry.connectedComponents(
                                    (Collection) entry.getValue())) {
                ((ArrayList) object)
                        .add(BlockLayer.buildCluster(world, key, list, membership, m, n));
            }
        }
        world.shapes.put(
                l,
                new Shapes(
                        (List<Cluster>) object,
                        new Box(
                                mAX_VALUE - 1,
                                mAX_VALUE2 - 1,
                                mAX_VALUE3 - 1,
                                mIN_VALUE + 2,
                                mIN_VALUE2 + 2,
                                mIN_VALUE3 + 2)));
        world.shapeRegions
                .computeIfAbsent(BlockLayer.regionOf(l), value2 -> new LongOpenHashSet())
                .add(l);
        ++world.shapeRevision;
    }

    private static Iterable<Shapes> nearbyShapes(World world, Vec3d vec, double d) {
        if (!PerformanceModule.optimizationsEnabled()) {
            return world.shapes.values();
        }
        int k = (int) Math.floor(vec.x) >> 4;
        int m = (int) Math.floor(vec.z) >> 4;
        if (world.candidateRevision == world.shapeRevision
                && world.candidateX == k
                && world.candidateZ == m
                && world.candidateRange == d) {
            return world.candidates;
        }
        world.candidates.clear();
        int n = (int) Math.ceil(d / 16.0) + 1;
        for (int i = k - n >> 4; i <= k + n >> 4; ++i) {
            for (int j = m - n >> 4; j <= m + n >> 4; ++j) {
                LongOpenHashSet shapeRegion = world.shapeRegions.get(BlockLayer.chunkKey(i, j));
                if (shapeRegion == null) continue;
                LongIterator longIterator = shapeRegion.iterator();
                while (longIterator.hasNext()) {
                    Shapes shape;
                    long l = longIterator.next();
                    if (Math.abs((long) ((int) l) - (long) k) > (long) n
                            || Math.abs((long) ((int) (l >> 32)) - (long) m) > (long) n
                            || (shape = world.shapes.get(l)) == null) continue;
                    world.candidates.add(shape);
                }
            }
        }
        world.candidateRevision = world.shapeRevision;
        world.candidateX = k;
        world.candidateZ = m;
        world.candidateRange = d;
        return world.candidates;
    }

    private static Cluster buildCluster(
            World world,
            Object object,
            List<BlockPos> list,
            BlockClusterGeometry.Membership membership,
            int i,
            int j) {
        int k;
        long[] longs = new long[list.size()];
        ArrayList<Long> list2 = new ArrayList<Long>();
        double d = 0.0;
        double e = 0.0;
        double d2 = 0.0;
        long mAX_VALUE = Long.MAX_VALUE;
        for (k = 0; k < longs.length; ++k) {
            BlockPos pos = list.get(k);
            longs[k] = pos.asLong();
            d += (double) pos.getX() + 0.5;
            e += (double) pos.getY() + 0.5;
            d2 += (double) pos.getZ() + 0.5;
            mAX_VALUE = Math.min(mAX_VALUE, world.positions.get(longs[k]).bornAt());
            for (BlockPos pos2 :
                    new BlockPos[] {pos.east(), pos.west(), pos.south(), pos.north()}) {
                if (pos2.getX() >> 4 == i && pos2.getZ() >> 4 == j
                        || !membership.contains(pos2.getX(), pos2.getY(), pos2.getZ())) continue;
                list2.add(pos2.asLong());
            }
        }
        k = longs.length;
        return new Cluster(
                object,
                longs,
                new Vec3d(d / (double) k, e / (double) k, d2 / (double) k),
                mAX_VALUE,
                BlockClusterGeometry.build(list, membership),
                list2.stream().mapToLong(Long::longValue).toArray());
    }

    private static List<Vein> joinVeins(World world) {
        int j;
        Object object;
        ArrayList<Cluster> list = new ArrayList<Cluster>();
        Long2ObjectOpenHashMap<Cluster> long2ObjectOpenHashMap =
                new Long2ObjectOpenHashMap<Cluster>();
        for (Object shape : world.shapes.values()) {
            for (Cluster cluster2 : ((Shapes) shape).clusters()) {
                list.add(cluster2);
                if (cluster2.links().length == 0) continue;
                long[] longs = cluster2.positions();
                j = longs.length;
                for (int i = 0; i < j; ++i) {
                    long l = longs[i];
                    int k = BlockPos.unpackLongX(l) & 0xF;
                    int m = BlockPos.unpackLongZ(l) & 0xF;
                    if (k != 0 && k != 15 && m != 0 && m != 15) continue;
                    long2ObjectOpenHashMap.put(l, cluster2);
                }
            }
        }
        IdentityHashMap map = new IdentityHashMap();
        for (Cluster cluster3 : list) {
            map.put(cluster3, cluster3);
        }
        for (Cluster cluster4 : list) {
            for (long link : cluster4.links()) {
                Cluster cluster5 = (Cluster) long2ObjectOpenHashMap.get(link);
                if (cluster5 == null) continue;
                map.put(BlockLayer.find(map, cluster4), BlockLayer.find(map, cluster5));
            }
        }
        object = new IdentityHashMap();
        for (Cluster cluster6 : list) {
            double[] doubles =
                    (double[])
                            ((java.util.Map) object)
                                    .computeIfAbsent(
                                            BlockLayer.find(map, cluster6),
                                            cluster -> new double[4]);
            j = cluster6.positions().length;
            doubles[0] = doubles[0] + cluster6.center().x * (double) j;
            doubles[1] = doubles[1] + cluster6.center().y * (double) j;
            doubles[2] = doubles[2] + cluster6.center().z * (double) j;
            doubles[3] = doubles[3] + (double) j;
        }
        ArrayList<Vein> list2 = new ArrayList<Vein>(((IdentityHashMap) object).size());
        for (Map.Entry entry : (Iterable<Map.Entry>) ((IdentityHashMap) object).entrySet()) {
            double[] entry2 = (double[]) entry.getValue();
            list2.add(
                    new Vein(
                            ((Cluster) entry.getKey()).kind(),
                            new Vec3d(
                                    entry2[0] / entry2[3],
                                    entry2[1] / entry2[3],
                                    entry2[2] / entry2[3])));
        }
        return list2;
    }

    private static Cluster find(Map<Cluster, Cluster> map, Cluster cluster) {
        Cluster cluster2 = cluster;
        while (map.get(cluster2) != cluster2) {
            cluster2 = map.get(cluster2);
        }
        while (map.get(cluster) != cluster2) {
            Cluster cluster3 = map.get(cluster);
            map.put(cluster, cluster2);
            cluster = cluster3;
        }
        return cluster2;
    }

    public void render(Render3DEvent event, EspStyle espStyle) {
        MinecraftClient client = MinecraftClient.getInstance();
        World world = this.world(false);
        if (client.player == null || world == null) {
            return;
        }
        this.rebuildDirty(world);
        Frustum frustum = PerformanceModule.frustumCulling() ? event.frustum : null;
        long l = System.nanoTime();
        Vec3d cameraPosVec = client.player.getCameraPosVec(event.partialTick);
        boolean outline = espStyle.outline();
        boolean glow = espStyle.glow.get();
        boolean fade = espStyle.fade.get();
        boolean reveal = espStyle.reveal.get();
        float lineWidth = espStyle.lineWidth.get();
        float fillAlpha = espStyle.fillAlpha.get();
        float boxSize = espStyle.boxSize.get();
        double range = espStyle.range.get();
        for (Shapes shapes2 : BlockLayer.nearbyShapes(world, cameraPosVec, range)) {
            if (frustum != null && !frustum.isVisible(shapes2.bounds())) continue;
            for (Cluster cluster : shapes2.clusters()) {
                double distance = cameraPosVec.distanceTo(cluster.center());
                if (distance > range) continue;
                float f = fade ? BlockLayer.distanceFade(distance) : 1.0f;
                float g = reveal ? Math.min(1.0f, (float) (l - cluster.bornAt()) / 3.8E8f) : 1.0f;
                f *= BlockLayer.easeOutCubic(g);
                // CFR emitted f4 (uninitialized); bytecode compares the faded alpha
                if (f < 0.02f) continue;
                float h = g < 1.0f ? 0.55f + 0.45f * BlockLayer.easeOutBack(g) : 1.0f;
                float f2 =
                        fade
                                ? lineWidth
                                        * (1.0f - 0.3f * (float) Math.min(1.0, distance / 256.0))
                                : lineWidth;
                int color = this.palette.color(cluster.kind());
                int scaleAlpha = BlockLayer.scaleAlpha(color, f);
                int i = fillAlpha > 0.0f ? BlockLayer.withAlpha(color, fillAlpha * f) : 0;
                int j = i;
                if (outline) {
                    BlockLayer.drawOutline(cluster, scaleAlpha, i, f2, glow, h);
                    continue;
                }
                BlockLayer.drawBoxes(cluster, scaleAlpha, i, f2, boxSize * h);
            }
        }
    }

    private static void drawOutline(Cluster cluster, int i, int j, float f, boolean bl, float g) {
        Vec3d vec = cluster.center();
        BlockClusterGeometry.Mesh mesh2 = cluster.mesh();
        if (j >>> 24 > 0) {
            DrawStyle drawStyle = DrawStyle.filled(j);
            for (BlockClusterGeometry.Face face : mesh2.faces()) {
                GizmoDrawing.quad(
                                BlockLayer.grow(face.a(), vec, g),
                                BlockLayer.grow(face.b(), vec, g),
                                BlockLayer.grow(face.c(), vec, g),
                                BlockLayer.grow(face.d(), vec, g),
                                drawStyle)
                        .ignoreOcclusion();
            }
        }
        int k = bl ? BlockLayer.scaleAlpha(i, 0.18f) : 0;
        for (BlockClusterGeometry.Segment edge : mesh2.edges()) {
            Vec3d vec2 = BlockLayer.grow(edge.a(), vec, g);
            Vec3d vec3 = BlockLayer.grow(edge.b(), vec, g);
            if (bl) {
                GizmoDrawing.line(vec2, vec3, k, f * 3.0f).ignoreOcclusion();
            }
            GizmoDrawing.line(vec2, vec3, i, f).ignoreOcclusion();
        }
    }

    private static void drawBoxes(Cluster cluster, int i, int j, float f, float g) {
        DrawStyle drawStyle =
                j >>> 24 > 0 ? DrawStyle.filledAndStroked(i, f, j) : DrawStyle.stroked(i, f);
        for (long position : cluster.positions()) {
            Box box = BlockBoxHelper.scaledBlockBox(BlockPos.fromLong(position), g);
            GizmoDrawing.box(box, drawStyle).ignoreOcclusion();
        }
    }

    private static Vec3d grow(Vec3d vec, Vec3d vec2, float f) {
        return f == 1.0f ? vec : vec2.add(vec.subtract(vec2).multiply(f));
    }

    private static float distanceFade(double d) {
        if (d <= 64.0) {
            return 1.0f;
        }
        float f = (float) Math.min(1.0, (d - 64.0) / 192.0);
        return 1.0f - 0.55f * f * f;
    }

    public void renderTracers(EspStyle espStyle) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        Vec3d cameraPosVec = client.player.getCameraPosVec(1.0f);
        double d = (double) espStyle.range.get() * (double) espStyle.range.get();
        for (Vein vein : this.veins()) {
            int tracerColor;
            if (cameraPosVec.squaredDistanceTo(vein.center()) > d
                    || (tracerColor = this.palette.tracerColor(vein.kind())) >>> 24 == 0) continue;
            TracerEngine.draw(vein.center(), tracerColor);
        }
    }

    private static int withAlpha(int i, float f) {
        return Math.round(255.0f * Math.max(0.0f, Math.min(1.0f, f))) << 24 | i & 0xFFFFFF;
    }

    private static int scaleAlpha(int i, float f) {
        int j = Math.round((float) (i >>> 24) * Math.max(0.0f, Math.min(1.0f, f)));
        return j << 24 | i & 0xFFFFFF;
    }

    private static float easeOutCubic(float f) {
        float g = 1.0f - f;
        return 1.0f - g * g * g;
    }

    private static float easeOutBack(float f) {
        float g = 1.70158f;
        float h = g + 1.0f;
        float f2 = f - 1.0f;
        return 1.0f + h * f2 * f2 * f2 + g * f2 * f2;
    }

    @Environment(value = EnvType.CLIENT)
    public static interface Palette {

        public int color(Object object);

        public int tracerColor(Object object);
    }

    @Environment(value = EnvType.CLIENT)
    private static final class World {

        final Long2ObjectOpenHashMap<Found> positions = new Long2ObjectOpenHashMap();

        final Long2ObjectOpenHashMap<LongOpenHashSet> byChunk = new Long2ObjectOpenHashMap();

        final Long2ObjectOpenHashMap<Shapes> shapes = new Long2ObjectOpenHashMap();

        final Long2ObjectOpenHashMap<LongOpenHashSet> shapeRegions = new Long2ObjectOpenHashMap();

        final LongOpenHashSet dirty = new LongOpenHashSet();

        final LongOpenHashSet scanDirty = new LongOpenHashSet();

        ChunkScanner.ScanBatch scanBatch;

        List<Vein> veins = List.of();

        boolean veinsDirty;

        long shapeRevision;

        long candidateRevision = -1L;

        int candidateX;

        int candidateZ;

        double candidateRange;

        final List<Shapes> candidates = new ArrayList<Shapes>();

        private World() {}
    }

    @Environment(value = EnvType.CLIENT)
    private record Found(Object kind, long bornAt) {}

    @Environment(value = EnvType.CLIENT)
    private record Cluster(
            Object kind,
            long[] positions,
            Vec3d center,
            long bornAt,
            BlockClusterGeometry.Mesh mesh,
            long[] links) {}

    @Environment(value = EnvType.CLIENT)
    private record Shapes(List<Cluster> clusters, Box bounds) {}

    @Environment(value = EnvType.CLIENT)
    public record Vein(Object kind, Vec3d center) {}
}
