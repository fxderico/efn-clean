package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class PredictionArea {

    public static final int MARGIN = 1;

    public static final int DOUBT_MISMATCH = 8;

    public static final int MISMATCH_COLUMNS = 26;

    private final SeedWorldgen world;

    private final Long2ObjectOpenHashMap<TerrainChunk> terrains = new Long2ObjectOpenHashMap<>();

    private final Long2ObjectOpenHashMap<ShownChunk> shown = new Long2ObjectOpenHashMap<>();

    private final Long2ObjectOpenHashMap<ChunkDecoration> decorations =
            new Long2ObjectOpenHashMap<>();

    private final Long2ObjectOpenHashMap<PredictionArea.MayWrite> may =
            new Long2ObjectOpenHashMap<>();

    private final Long2ObjectOpenHashMap<GateHeights> gateHeights = new Long2ObjectOpenHashMap<>();

    private final DecorationOrder order = new DecorationOrder();

    private final Long2IntOpenHashMap doubt = new Long2IntOpenHashMap();

    private LongOpenHashSet bandChunks = new LongOpenHashSet();

    private LongOpenHashSet decorationChunks = new LongOpenHashSet();

    private final LongOpenHashSet dirty = new LongOpenHashSet();

    private final LongOpenHashSet built = new LongOpenHashSet();

    private final Long2ObjectOpenHashMap<VanillaBand> bands = new Long2ObjectOpenHashMap<>();

    private final LongOpenHashSet clustersDue = new LongOpenHashSet();

    private final LongOpenHashSet clustersSent = new LongOpenHashSet();

    private final ArrayDeque<PredictionArea.ChunkClusters> out = new ArrayDeque<>();

    private final LongOpenHashSet collected = new LongOpenHashSet();

    private final Long2LongOpenHashMap signatures = new Long2LongOpenHashMap();

    private int focusX;

    private int focusZ;

    private int compared;

    private int mismatched;

    public PredictionArea(SeedWorldgen world2) {
        this.world = world2;
        this.doubt.defaultReturnValue(-1);
    }

    public void want(LongCollection longCollection, int focusX2, int focusZ2) {
        this.focusX = focusX2;
        this.focusZ = focusZ2;
        LongOpenHashSet longOpenHashSet = new LongOpenHashSet(longCollection);
        LongOpenHashSet longOpenHashSet2 = new LongOpenHashSet();
        byte b = 2;
        LongIterator longIterator = longOpenHashSet.iterator();
        while (longIterator.hasNext()) {
            long l = longIterator.nextLong();
            int packedX = ChunkPos.getPackedX(l);
            int packedZ = ChunkPos.getPackedZ(l);
            for (int n = -b; n <= b; n++) {
                for (int o = -b; o <= b; o++) {
                    longOpenHashSet2.add(ChunkPos.toLong(packedX + n, packedZ + o));
                }
            }
        }
        longIterator = longOpenHashSet2.iterator();
        while (longIterator.hasNext()) {
            long l2 = longIterator.nextLong();
            if (!this.decorations.containsKey(l2)) {
                this.dirty.add(l2);
            }
        }
        this.bandChunks = longOpenHashSet;
        this.decorationChunks = longOpenHashSet2;
        this.built.retainAll(longOpenHashSet);
        this.bands.keySet().retainAll(longOpenHashSet);
        this.clustersDue.retainAll(longOpenHashSet);
        this.clustersSent.retainAll(longOpenHashSet);
        this.dirty.retainAll(longOpenHashSet2);
        this.forgetOutside(longOpenHashSet2);
    }

    private void forgetOutside(LongOpenHashSet longOpenHashSet) {
        LongOpenHashSet longOpenHashSet2 = new LongOpenHashSet();
        LongIterator longIterator = longOpenHashSet.iterator();
        while (longIterator.hasNext()) {
            long l = longIterator.nextLong();
            int packedX = ChunkPos.getPackedX(l);
            int packedZ = ChunkPos.getPackedZ(l);
            for (int k = -1; k <= 1; k++) {
                for (int m = -1; m <= 1; m++) {
                    longOpenHashSet2.add(ChunkPos.toLong(packedX + k, packedZ + m));
                }
            }
        }
        this.terrains.keySet().retainAll(longOpenHashSet2);
        this.shown.keySet().retainAll(longOpenHashSet2);
        this.gateHeights.keySet().retainAll(longOpenHashSet2);
        this.doubt.keySet().retainAll(longOpenHashSet2);
        this.decorations.keySet().retainAll(longOpenHashSet);
        this.may.keySet().retainAll(longOpenHashSet);
        this.collected.retainAll(longOpenHashSet);
        this.signatures.keySet().retainAll(longOpenHashSet);
    }

    public void offer(ShownChunk shownChunk) {
        long l = ChunkPos.toLong(shownChunk.chunkX, shownChunk.chunkZ);
        this.shown.put(l, shownChunk);
        this.doubt.remove(l);
        this.gateHeights.remove(l);
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                this.collected.remove(
                        ChunkPos.toLong(shownChunk.chunkX + i, shownChunk.chunkZ + j));
            }
        }
        for (int k = -1; k <= 1; k++) {
            for (int m = -1; m <= 1; m++) {
                long l2 = ChunkPos.toLong(shownChunk.chunkX + k, shownChunk.chunkZ + m);
                if (this.decorationChunks.contains(l2)) {
                    this.markDirty(l2);
                }
            }
        }
    }

    public boolean idle() {
        if (this.dirty.isEmpty() && this.out.isEmpty() && this.clustersDue.isEmpty()) {
            LongIterator longIterator = this.bandChunks.iterator();
            while (longIterator.hasNext()) {
                if (!this.built.contains(longIterator.nextLong())) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public int compared() {
        return this.compared;
    }

    public int mismatched() {
        return this.mismatched;
    }

    public PredictionArea.ChunkClusters step() {
        if (!this.out.isEmpty()) {
            return this.out.poll();
        } else {
            LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
            LongIterator longIterator = this.dirty.iterator();
            while (longIterator.hasNext()) {
                long l = longIterator.nextLong();
                if (!this.decorations.containsKey(l)) {
                    longOpenHashSet.add(l);
                }
            }
            long nearest = this.nearest(longOpenHashSet.isEmpty() ? this.dirty : longOpenHashSet);
            if (nearest != -9223372036854775808L) {
                int packedX = ChunkPos.getPackedX(nearest);
                int packedZ = ChunkPos.getPackedZ(nearest);
                for (int n = -1; n <= 1; n++) {
                    for (int o = -1; o <= 1; o++) {
                        long l2 = ChunkPos.toLong(packedX + n, packedZ + o);
                        if (!this.terrains.containsKey(l2)) {
                            this.terrains.put(l2, this.world.terrain(packedX + n, packedZ + o));
                            return null;
                        }
                    }
                }
                this.predict(nearest);
                return null;
            } else {
                LongOpenHashSet longOpenHashSet2 = new LongOpenHashSet();
                LongIterator longIterator2 = this.bandChunks.iterator();
                while (longIterator2.hasNext()) {
                    long l3 = longIterator2.nextLong();
                    if (!this.built.contains(l3)) {
                        longOpenHashSet2.add(l3);
                    }
                }
                while (!longOpenHashSet2.isEmpty()) {
                    long nearest2 = this.nearest(longOpenHashSet2);
                    longOpenHashSet2.remove(nearest2);
                    if (this.ready(nearest2)) {
                        int packedX2 = ChunkPos.getPackedX(nearest2);
                        int packedZ2 = ChunkPos.getPackedZ(nearest2);
                        ChunkDecoration[] chunkDecorations = new ChunkDecoration[9];
                        for (int r = -1; r <= 1; r++) {
                            for (int t = -1; t <= 1; t++) {
                                chunkDecorations[(r + 1) * 3 + t + 1] =
                                        this.decorations.get(
                                                ChunkPos.toLong(packedX2 + r, packedZ2 + t));
                            }
                        }
                        VanillaBand vanillaBand =
                                VanillaBand.of(
                                        this.terrains.get(nearest2), chunkDecorations, this.order);
                        this.built.add(nearest2);
                        this.bands.put(nearest2, vanillaBand);
                        for (int u = -2; u <= 2; u++) {
                            for (int v = -2; v <= 2; v++) {
                                long l4 = ChunkPos.toLong(packedX2 + u, packedZ2 + v);
                                if (this.bandChunks.contains(l4)) {
                                    this.clustersDue.add(l4);
                                }
                            }
                        }
                        return null;
                    }
                }
                LongOpenHashSet longOpenHashSet3 = new LongOpenHashSet(this.clustersDue);
                while (!longOpenHashSet3.isEmpty()) {
                    long nearest3 = this.nearest(longOpenHashSet3);
                    longOpenHashSet3.remove(nearest3);
                    if (this.clustersReady(nearest3)) {
                        this.clustersDue.remove(nearest3);
                        this.clustersSent.add(nearest3);
                        return new PredictionArea.ChunkClusters(
                                nearest3,
                                List.copyOf(
                                        BlobJudge.clusters(
                                                ChunkPos.getPackedX(nearest3),
                                                ChunkPos.getPackedZ(nearest3),
                                                (i, j) -> this.bands.get(ChunkPos.toLong(i, j)))));
                    }
                }
                return null;
            }
        }
    }

    private boolean clustersReady(long l) {
        if (!this.bands.containsKey(l)) {
            return false;
        } else {
            int packedX = ChunkPos.getPackedX(l);
            int packedZ = ChunkPos.getPackedZ(l);
            for (int k = -1; k <= 1; k++) {
                for (int m = -1; m <= 1; m++) {
                    long l2 = ChunkPos.toLong(packedX + k, packedZ + m);
                    if (this.bandChunks.contains(l2) && !this.bands.containsKey(l2)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private void unbuild(long l) {
        if (this.built.remove(l)) {
            this.bands.remove(l);
            int packedX = ChunkPos.getPackedX(l);
            int packedZ = ChunkPos.getPackedZ(l);
            for (int k = -2; k <= 2; k++) {
                for (int m = -2; m <= 2; m++) {
                    long l2 = ChunkPos.toLong(packedX + k, packedZ + m);
                    if (this.clustersSent.remove(l2)) {
                        this.out.add(new PredictionArea.ChunkClusters(l2, null));
                    }
                    if (this.bandChunks.contains(l2)) {
                        this.clustersDue.add(l2);
                    }
                }
            }
        }
    }

    private boolean ready(long l) {
        int packedX = ChunkPos.getPackedX(l);
        int packedZ = ChunkPos.getPackedZ(l);
        for (int k = -1; k <= 1; k++) {
            for (int m = -1; m <= 1; m++) {
                if (!this.decorations.containsKey(ChunkPos.toLong(packedX + k, packedZ + m))) {
                    return false;
                }
            }
        }
        if (!this.terrains.containsKey(l)) {
            return false;
        } else {
            for (int n = -3; n <= 3; n++) {
                for (int o = -3; o <= 3; o++) {
                    if (this.dirty.contains(ChunkPos.toLong(packedX + n, packedZ + o))) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private void markDirty(long l) {
        if (this.decorationChunks.contains(l)) {
            this.dirty.add(l);
            int packedX = ChunkPos.getPackedX(l);
            int packedZ = ChunkPos.getPackedZ(l);
            for (int k = -1; k <= 1; k++) {
                for (int m = -1; m <= 1; m++) {
                    this.unbuild(ChunkPos.toLong(packedX + k, packedZ + m));
                }
            }
        }
    }

    private void predict(long l) {
        int packedX = ChunkPos.getPackedX(l);
        int packedZ = ChunkPos.getPackedZ(l);
        ChunkDecoration chunkDecoration =
                OrePredictor.predict(this.world, packedX, packedZ, this.inputs(packedX, packedZ));
        this.dirty.remove(l);
        ChunkDecoration chunkDecoration2 = chunkDecoration.trimmed(-16, 63, true);
        ChunkDecoration chunkDecoration3 = this.decorations.put(l, chunkDecoration2);
        for (int k = -1; k <= 1; k++) {
            for (int m = -1; m <= 1; m++) {
                this.unbuild(ChunkPos.toLong(packedX + k, packedZ + m));
            }
        }
        PredictionArea.MayWrite mayWrite =
                this.may.computeIfAbsent(l, lx -> new PredictionArea.MayWrite(packedX, packedZ));
        if (mayWrite.absorb(chunkDecoration)) {
            for (int n = -2; n <= 2; n++) {
                for (int o = -2; o <= 2; o++) {
                    if (n != 0 || o != 0) {
                        this.markDirty(ChunkPos.toLong(packedX + n, packedZ + o));
                    }
                }
            }
        }
        long evidenceSignature = DecorationOrder.evidenceSignature(chunkDecoration2);
        boolean bl =
                chunkDecoration3 == null
                        || this.signatures.get(l) != evidenceSignature
                        || !this.collected.contains(l);
        this.signatures.put(l, evidenceSignature);
        ArrayList list = new ArrayList();
        for (int p = -2; p <= 2; p++) {
            for (int q = -2; q <= 2; q++) {
                long l2 = ChunkPos.toLong(packedX + p, packedZ + q);
                ChunkDecoration decoration = this.decorations.get(l2);
                if (decoration != null
                        && this.neighbourhoodGuessed(l2)
                        && (bl || !this.collected.contains(l2))) {
                    list.addAll(
                            this.order.collect(
                                    decoration,
                                    this.decorations,
                                    this::terrainOrNull,
                                    this::shownKind));
                    this.collected.add(l2);
                }
            }
        }
        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            for (long l3 : (long[]) iterator.next()) {
                int packedX2 = ChunkPos.getPackedX(l3);
                int packedZ2 = ChunkPos.getPackedZ(l3);
                for (int u = -2; u <= 2; u++) {
                    for (int v = -2; v <= 2; v++) {
                        this.markDirty(ChunkPos.toLong(packedX2 + u, packedZ2 + v));
                    }
                }
            }
        }
    }

    private boolean neighbourhoodGuessed(long l) {
        int packedX = ChunkPos.getPackedX(l);
        int packedZ = ChunkPos.getPackedZ(l);
        for (int k = -2; k <= 2; k++) {
            for (int m = -2; m <= 2; m++) {
                long l2 = ChunkPos.toLong(packedX + k, packedZ + m);
                if (this.decorationChunks.contains(l2) && !this.decorations.containsKey(l2)) {
                    return false;
                }
            }
        }
        return true;
    }

    private TerrainChunk terrainOrNull(int i, int j) {
        return this.terrains.get(ChunkPos.toLong(i, j));
    }

    private byte shownKind(int i, int j, int k) {
        ShownChunk shownChunk = this.shown.get(ChunkPos.toLong(i >> 4, k >> 4));
        return shownChunk == null ? 15 : shownChunk.kind(i, j, k);
    }

    private int heightDoubt(long l, TerrainChunk terrainChunk, ShownChunk shownChunk) {
        int i = this.doubt.get(l);
        if (i != -1) {
            return i;
        } else {
            int j = 0;
            int k = 0;
            int packedX = ChunkPos.getPackedX(l);
            int packedZ = ChunkPos.getPackedZ(l);
            for (int o = 0; o < 16; o++) {
                for (int p = 0; p < 16; p++) {
                    int m = (packedX << 4) + p;
                    int n = (packedZ << 4) + o;
                    int terrainTop = shownChunk.terrainTop(m, n);
                    if (terrainTop != -2147483648) {
                        k++;
                        if (Math.abs(terrainTop - terrainChunk.oceanFloor(m, n)) > 2) {
                            j++;
                        }
                    }
                }
            }
            int q = k >= 64 && j * 256 > 26 * k ? 8 : 0;
            this.doubt.put(l, q);
            if (k >= 64) {
                this.compared++;
                if (q == 8) {
                    this.mismatched++;
                }
            }
            return q;
        }
    }

    private GateHeights gateHeights(int i, int j) {
        long l = ChunkPos.toLong(i, j);
        GateHeights gateHeight = this.gateHeights.get(l);
        if (gateHeight != null) {
            return gateHeight;
        } else {
            TerrainChunk terrain = this.terrains.get(l);
            ShownChunk shownChunk = this.shown.get(l);
            gateHeight =
                    GateHeights.of(
                            terrain,
                            shownChunk,
                            shownChunk == null ? 0 : this.heightDoubt(l, terrain, shownChunk),
                            this.world.treeAllowance(i, j));
            this.gateHeights.put(l, gateHeight);
            return gateHeight;
        }
    }

    private OrePredictor.Inputs inputs(int j, int m) {
        final long l = ChunkPos.toLong(j, m);
        return new OrePredictor.Inputs() {

            @Override
            public TerrainChunk terrain(int i, int jx) {
                return PredictionArea.this.terrains.get(ChunkPos.toLong(i, jx));
            }

            @Override
            public GateHeights gateHeights(int i, int jx) {
                return PredictionArea.this.gateHeights(i, jx);
            }

            @Override
            public boolean othersMayWrite(int i, int jx, int k) {
                int n = i >> 4;
                int q = k >> 4;
                for (int o = -1; o <= 1; o++) {
                    for (int p = -1; p <= 1; p++) {
                        long l2 = ChunkPos.toLong(n + o, q + p);
                        if (l2 != l && !PredictionArea.this.order.after(l2, l)) {
                            if (!PredictionArea.this.decorationChunks.contains(l2)) {
                                return true;
                            }
                            PredictionArea.MayWrite mayWrite = PredictionArea.this.may.get(l2);
                            if (mayWrite != null && mayWrite.nonBase(i, jx, k)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }

            @Override
            public byte shown(int i, int jx, int k) {
                return PredictionArea.this.shownKind(i, jx, k);
            }

            @Override
            public boolean predictedOre(int i, int jx, int k) {
                int r = i >> 4;
                int v = k >> 4;
                for (int t = -1; t <= 1; t++) {
                    for (int u = -1; u <= 1; u++) {
                        PredictionArea.MayWrite mayWrite2 =
                                PredictionArea.this.may.get(ChunkPos.toLong(r + t, v + u));
                        if (mayWrite2 != null && mayWrite2.ore(i, jx, k)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

    private long nearest(LongOpenHashSet longOpenHashSet) {
        long l = -9223372036854775808L;
        int i = 2147483647;
        LongIterator longIterator = longOpenHashSet.iterator();
        while (longIterator.hasNext()) {
            long l2 = longIterator.nextLong();
            int j =
                    Math.max(
                            Math.abs(ChunkPos.getPackedX(l2) - this.focusX),
                            Math.abs(ChunkPos.getPackedZ(l2) - this.focusZ));
            if (j < i) {
                i = j;
                l = l2;
            }
        }
        return l;
    }

    @Environment(EnvType.CLIENT)
    public record ChunkClusters(long chunk, List<BlobJudge.Cluster> clusters) {}

    @Environment(EnvType.CLIENT)
    static final class MayWrite {

        static final int TOP = 200;

        private final int originX;

        private final int originZ;

        private int[] nonBase = new int[0];

        private int[] ores = new int[0];

        private final List<ChunkDecoration.Fog> fogs = new ArrayList<>();

        MayWrite(int i, int j) {
            this.originX = i - 1 << 4;
            this.originZ = j - 1 << 4;
        }

        private int index(int i, int j, int k) {
            int m = i - this.originX;
            int n = k - this.originZ;
            return m >= 0 && m < 48 && n >= 0 && n < 48 && j >= -64 && j <= 200
                    ? ((j - -64) * 48 + n) * 48 + m
                    : -1;
        }

        boolean absorb(ChunkDecoration chunkDecoration) {
            IntOpenHashSet intOpenHashSet = new IntOpenHashSet();
            IntOpenHashSet intOpenHashSet2 = new IntOpenHashSet();
            for (int i = 0; i < chunkDecoration.size(); i++) {
                long key = chunkDecoration.key(i);
                int index =
                        this.index(
                                BlockPos.unpackLongX(key),
                                BlockPos.unpackLongY(key),
                                BlockPos.unpackLongZ(key));
                if (index >= 0) {
                    for (int k = 0; k < chunkDecoration.writeCount(i); k++) {
                        byte kind = ChunkDecoration.kind(chunkDecoration.write(i, k));
                        if (!VBlock.isBase(kind)) {
                            if (Arrays.binarySearch(this.nonBase, index) < 0) {
                                intOpenHashSet.add(index);
                            }
                            if (kind == 11 && Arrays.binarySearch(this.ores, index) < 0) {
                                intOpenHashSet2.add(index);
                            }
                        }
                    }
                }
            }
            boolean bl = !intOpenHashSet.isEmpty() || !intOpenHashSet2.isEmpty();
            this.nonBase = merged(this.nonBase, intOpenHashSet);
            this.ores = merged(this.ores, intOpenHashSet2);
            for (ChunkDecoration.Fog fog : chunkDecoration.fogs()) {
                if (!VBlock.isBase(fog.kind()) && !this.fogs.contains(fog)) {
                    this.fogs.add(fog);
                    bl = true;
                }
            }
            return bl;
        }

        private static int[] merged(int[] ints, IntOpenHashSet intOpenHashSet) {
            if (intOpenHashSet.isEmpty()) {
                return ints;
            } else {
                int[] ints2 = Arrays.copyOf(ints, ints.length + intOpenHashSet.size());
                int i = ints.length;
                IntIterator intIterator = intOpenHashSet.iterator();
                while (intIterator.hasNext()) {
                    ints2[i++] = intIterator.nextInt();
                }
                Arrays.sort(ints2);
                return ints2;
            }
        }

        boolean nonBase(int i, int j, int k) {
            int index = this.index(i, j, k);
            if (index >= 0 && Arrays.binarySearch(this.nonBase, index) >= 0) {
                return true;
            } else {
                for (ChunkDecoration.Fog fog : this.fogs) {
                    if (fog.contains(i, j, k)) {
                        return true;
                    }
                }
                return false;
            }
        }

        boolean ore(int i, int j, int k) {
            int index = this.index(i, j, k);
            if (index >= 0 && Arrays.binarySearch(this.ores, index) >= 0) {
                return true;
            } else {
                for (ChunkDecoration.Fog fog : this.fogs) {
                    if (fog.kind() == 11 && fog.contains(i, j, k)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
}
