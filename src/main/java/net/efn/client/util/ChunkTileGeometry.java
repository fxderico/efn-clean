/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Environment(value = EnvType.CLIENT)
final class ChunkTileGeometry {

    final Vec3d[] outline;

    final Vec3d[] curves;

    final Vec3d[] edges;

    private final double x0;

    private final double z0;

    private final double x1;

    private final double z1;

    private final double radius;

    private final int mask;

    private long usedFrame;

    private ChunkTileGeometry(
            double x02, double z02, double x12, double z12, double radius2, int mask2) {
        this.x0 = x02;
        this.z0 = z02;
        this.x1 = x12;
        this.z1 = z12;
        this.radius = radius2;
        this.mask = mask2;
        boolean bl = (mask2 & 1) != 0;
        boolean bl2 = (mask2 & 2) != 0;
        boolean bl3 = (mask2 & 4) != 0;
        boolean bl4 = (mask2 & 8) != 0;
        double d = bl && bl3 ? radius2 : 0.0;
        double e = bl && bl4 ? radius2 : 0.0;
        double d2 = bl2 && bl4 ? radius2 : 0.0;
        double e2 = bl2 && bl3 ? radius2 : 0.0;
        ArrayList<Vec3d> list = new ArrayList<Vec3d>(20);
        ArrayList<Vec3d> list2 = new ArrayList<Vec3d>(32);
        ArrayList<Vec3d> list3 = new ArrayList<Vec3d>(8);
        ChunkTileGeometry.corner(list, list2, x02 + d, z02 + d, d, 180.0);
        ChunkTileGeometry.corner(list, list2, x12 - e, z02 + e, e, 270.0);
        ChunkTileGeometry.corner(list, list2, x12 - d2, z12 - d2, d2, 0.0);
        ChunkTileGeometry.corner(list, list2, x02 + e2, z12 - e2, e2, 90.0);
        if (bl) {
            ChunkTileGeometry.edge(list3, x02 + d, z02, x12 - e, z02);
        }
        if (bl2) {
            ChunkTileGeometry.edge(list3, x02 + e2, z12, x12 - d2, z12);
        }
        if (bl3) {
            ChunkTileGeometry.edge(list3, x02, z02 + d, x02, z12 - e2);
        }
        if (bl4) {
            ChunkTileGeometry.edge(list3, x12, z02 + e, x12, z12 - d2);
        }
        this.outline = list.toArray(new Vec3d[0]);
        this.curves = list2.toArray(new Vec3d[0]);
        this.edges = list3.toArray(new Vec3d[0]);
    }

    private boolean matches(double d, double e, double d2, double e2, double d3, int i) {
        return this.mask == i
                && Double.compare(this.x0, d) == 0
                && Double.compare(this.z0, e) == 0
                && Double.compare(this.x1, d2) == 0
                && Double.compare(this.z1, e2) == 0
                && Double.compare(this.radius, d3) == 0;
    }

    private static void corner(
            List<Vec3d> list, List<Vec3d> list2, double d, double e, double d2, double e2) {
        if (d2 <= 0.0) {
            list.add(new Vec3d(d, 63.0, e));
            return;
        }
        Vec3d vec = null;
        for (int i = 0; i <= 4; ++i) {
            double d3 = Math.toRadians(e2 + 90.0 * (double) i / 4.0);
            Vec3d vec2 = new Vec3d(d + Math.cos(d3) * d2, 63.0, e + Math.sin(d3) * d2);
            list.add(vec2);
            if (vec != null) {
                list2.add(vec);
                list2.add(vec2);
            }
            vec = vec2;
        }
    }

    private static void edge(List<Vec3d> list, double d, double e, double d2, double e2) {
        list.add(new Vec3d(d, 63.0, e));
        list.add(new Vec3d(d2, 63.0, e2));
    }

    @Environment(value = EnvType.CLIENT)
    static final class Cache {

        private static final int CAPACITY = 512;

        private final LinkedHashMap<Long, ChunkTileGeometry> tiles =
                new LinkedHashMap(64, 0.75f, true);

        private long frame;

        Cache() {}

        void beginFrame() {
            ++this.frame;
        }

        void clear() {
            this.tiles.clear();
        }

        int size() {
            return this.tiles.size();
        }

        ChunkTileGeometry get(long l, double d, double e, double d2, double e2, double d3, int i) {
            ChunkTileGeometry tile = this.tiles.get(l);
            if (tile != null && tile.matches(d, e, d2, e2, d3, i)) {
                tile.usedFrame = this.frame;
                return tile;
            }
            ChunkTileGeometry chunkTileGeometry = new ChunkTileGeometry(d, e, d2, e2, d3, i);
            chunkTileGeometry.usedFrame = this.frame;
            if (tile != null || this.tiles.size() < 512) {
                this.tiles.put(l, chunkTileGeometry);
            } else {
                Iterator<Map.Entry<Long, ChunkTileGeometry>> iterator =
                        this.tiles.entrySet().iterator();
                Map.Entry<Long, ChunkTileGeometry> entry = iterator.next();
                if (entry.getValue().usedFrame < this.frame - 1L) {
                    iterator.remove();
                    this.tiles.put(l, chunkTileGeometry);
                }
            }
            return chunkTileGeometry;
        }
    }
}
