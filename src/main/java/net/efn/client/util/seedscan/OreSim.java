/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@Environment(value = EnvType.CLIENT)
final class OreSim {

    static final byte NO = 0;

    static final byte YES = 1;

    static final byte MAYBE = 2;

    static final byte CERTAIN = 1;

    static final byte POSSIBLE = 2;

    static final int MAX_STATES = Integer.getInteger("efn.oreStates", 1024);

    static final int MAX_WORK = Integer.getInteger("efn.oreWork", 30000);

    static final int MIN_Y = -64;

    static final int MAX_Y = 319;

    private static final int[][] SIDES =
            new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private OreSim() {}

    static Chain run(OreSpec oreSpec, int i, int j, World world) {
        WgRandom wgRandom = new WgRandom();
        long setDecorationSeed = wgRandom.setDecorationSeed(world.seed(), i << 4, j << 4);
        wgRandom.setFeatureSeed(setDecorationSeed, oreSpec.index(), oreSpec.step());
        Chain chain = new Chain(oreSpec, i, j);
        int sampleCount = oreSpec.sampleCount(wgRandom);
        List<WgRandom> list = new ArrayList<WgRandom>();
        list.add(wgRandom);
        Long2ByteOpenHashMap writes2 = chain.writes;
        Long2ByteOpenHashMap long2ByteOpenHashMap = new Long2ByteOpenHashMap();
        int m = 0;
        for (int k = 0; k < sampleCount; ++k) {
            ArrayList<WgRandom> list2 = new ArrayList<WgRandom>(list.size() + 2);
            Long2ByteOpenHashMap long2ByteOpenHashMap2 = new Long2ByteOpenHashMap();
            LongOpenHashSet longOpenHashSet = null;
            for (WgRandom wgRandom2 : (Iterable<WgRandom>) list) {
                long2ByteOpenHashMap.clear();
                OreSim.attempt(
                        oreSpec, i, j, world, wgRandom2, writes2, long2ByteOpenHashMap, list2);
                LongOpenHashSet longOpenHashSet2 = new LongOpenHashSet();
                ObjectIterator<Long2ByteMap.Entry> objectIterator =
                        long2ByteOpenHashMap.long2ByteEntrySet().fastIterator();
                while (objectIterator.hasNext()) {
                    Long2ByteMap.Entry entry = (Long2ByteMap.Entry) objectIterator.next();
                    long2ByteOpenHashMap2.put(entry.getLongKey(), (byte) 2);
                    if (entry.getByteValue() != 1) continue;
                    longOpenHashSet2.add(entry.getLongKey());
                }
                if (longOpenHashSet == null) {
                    longOpenHashSet = longOpenHashSet2;
                    continue;
                }
                longOpenHashSet.retainAll(longOpenHashSet2);
            }
            Iterator iterator = long2ByteOpenHashMap2.long2ByteEntrySet().fastIterator();
            while (iterator.hasNext()) {
                long longKey = ((Long2ByteMap.Entry) iterator.next()).getLongKey();
                if (writes2.get(longKey) == 1) continue;
                writes2.put(longKey, (byte) 2);
            }
            if (longOpenHashSet != null) {
                LongIterator longIterator = longOpenHashSet.iterator();
                while (longIterator.hasNext()) {
                    long l = longIterator.next();
                    writes2.put(l, (byte) 1);
                }
            }
            if ((list = OreSim.distinct(list2)).size() <= MAX_STATES
                            && (m += list.size()) <= MAX_WORK
                    || k + 1 >= sampleCount) continue;
            chain.fogFrom = k + 1;
            break;
        }
        return chain;
    }

    private static void attempt(
            OreSpec oreSpec,
            int i,
            int j,
            World world,
            WgRandom wgRandom,
            Long2ByteOpenHashMap long2ByteOpenHashMap,
            Long2ByteOpenHashMap long2ByteOpenHashMap2,
            List<WgRandom> list) {
        int k = wgRandom.nextInt(16) + (i << 4);
        int m = wgRandom.nextInt(16) + (j << 4);
        int sampleY = oreSpec.sampleY(wgRandom);
        if (!world.inBiome(oreSpec, k, sampleY, m)) {
            list.add(wgRandom);
            return;
        }
        int n = oreSpec.size();
        float f = wgRandom.nextFloat() * (float) Math.PI;
        float g = (float) n / 8.0f;
        int o = MathHelper.ceil(((float) n / 16.0f * 2.0f + 1.0f) / 2.0f);
        double d = (double) k + Math.sin(f) * (double) g;
        double e = (double) k - Math.sin(f) * (double) g;
        double d2 = (double) m + Math.cos(f) * (double) g;
        double e2 = (double) m - Math.cos(f) * (double) g;
        double d3 = sampleY + wgRandom.nextInt(3) - 2;
        double e3 = sampleY + wgRandom.nextInt(3) - 2;
        int p = k - MathHelper.ceil(g) - o;
        int q = sampleY - 2 - o;
        int r = m - MathHelper.ceil(g) - o;
        int t = 2 * (MathHelper.ceil(g) + o);
        int u = 2 * (2 + o);
        byte gate = OreSim.gate(world, q, p, r, t);
        if (gate == 0) {
            list.add(wgRandom);
            return;
        }
        if (gate == 2) {
            list.add(wgRandom.copy());
        }
        OreSim.doPlace(
                oreSpec,
                i,
                j,
                world,
                wgRandom,
                long2ByteOpenHashMap,
                long2ByteOpenHashMap2,
                gate == 2,
                d,
                e,
                d2,
                e2,
                d3,
                e3,
                p,
                q,
                r,
                t,
                u,
                list);
    }

    private static byte gate(World world, int i, int j, int k, int m) {
        boolean bl = false;
        for (int i2 = j; i2 <= j + m; ++i2) {
            for (int i3 = k; i3 <= k + m; ++i3) {
                if (i <= world.gateLow(i2, i3)) {
                    return 1;
                }
                if (i > world.gateHigh(i2, i3)) continue;
                bl = true;
            }
        }
        return bl ? (byte) 2 : 0;
    }

    private static void doPlace(
            OreSpec oreSpec,
            int i,
            int j,
            World world,
            WgRandom wgRandom,
            Long2ByteOpenHashMap long2ByteOpenHashMap,
            Long2ByteOpenHashMap long2ByteOpenHashMap2,
            boolean bl,
            double d,
            double e,
            double d2,
            double e2,
            double d3,
            double e3,
            int k,
            int m,
            int n,
            int o,
            int p,
            List<WgRandom> list) {
        int q;
        double d4;
        double e4;
        double d5;
        double e5;
        int r;
        int t = oreSpec.size();
        double[] doubles = new double[t * 4];
        for (r = 0; r < t; ++r) {
            float f = (float) r / (float) t;
            e5 = MathHelper.lerp((double) f, d, e);
            d5 = MathHelper.lerp((double) f, d3, e3);
            e4 = MathHelper.lerp((double) f, d2, e2);
            d4 = wgRandom.nextDouble() * (double) t / 16.0;
            double d6 = ((double) (MathHelper.sin((float) Math.PI * f) + 1.0f) * d4 + 1.0) / 2.0;
            doubles[r * 4] = e5;
            doubles[r * 4 + 1] = d5;
            doubles[r * 4 + 2] = e4;
            doubles[r * 4 + 3] = d6;
        }
        for (r = 0; r < t - 1; ++r) {
            if (doubles[r * 4 + 3] <= 0.0) continue;
            for (int i2 = r + 1; i2 < t; ++i2) {
                if (doubles[i2 * 4 + 3] <= 0.0
                        || !((d4 = doubles[r * 4 + 3] - doubles[i2 * 4 + 3]) * d4
                                > (e5 = doubles[r * 4] - doubles[i2 * 4]) * e5
                                        + (d5 = doubles[r * 4 + 1] - doubles[i2 * 4 + 1]) * d5
                                        + (e4 = doubles[r * 4 + 2] - doubles[i2 * 4 + 2]) * e4))
                    continue;
                if (d4 > 0.0) {
                    doubles[i2 * 4 + 3] = -1.0;
                    continue;
                }
                doubles[r * 4 + 3] = -1.0;
            }
        }
        r = oreSpec.drawsOnTerrain() ? 1 : 0;
        float discard = oreSpec.discard();
        boolean base = VBlock.isBase(oreSpec.kind());
        WgRandom wgRandom2 = r != 0 ? wgRandom.copy() : null;
        int u = 0;
        int v = 0;
        boolean bl2 = false;
        BitSet bitSet = new BitSet(o * p * o);
        for (q = 0; q < t; ++q) {
            double e6 = doubles[q * 4 + 3];
            if (e6 < 0.0) continue;
            double d7 = doubles[q * 4];
            double e7 = doubles[q * 4 + 1];
            double d8 = doubles[q * 4 + 2];
            int w = Math.max(MathHelper.floor(d7 - e6), k);
            int j2 = Math.max(MathHelper.floor(e7 - e6), m);
            int k2 = Math.max(MathHelper.floor(d8 - e6), n);
            int m2 = Math.max(MathHelper.floor(d7 + e6), w);
            int n2 = Math.max(MathHelper.floor(e7 + e6), j2);
            int o2 = Math.max(MathHelper.floor(d8 + e6), k2);
            for (int i3 = w; i3 <= m2; ++i3) {
                double e8 = ((double) i3 + 0.5 - d7) / e6;
                if (e8 * e8 >= 1.0) continue;
                for (int i4 = j2; i4 <= n2; ++i4) {
                    double d9 = ((double) i4 + 0.5 - e7) / e6;
                    if (e8 * e8 + d9 * d9 >= 1.0) continue;
                    for (int i5 = k2; i5 <= o2; ++i5) {
                        int p2;
                        long l;
                        byte baseNow;
                        int q2;
                        double e9 = ((double) i5 + 0.5 - d8) / e6;
                        if (e8 * e8 + d9 * d9 + e9 * e9 >= 1.0
                                || i4 < -64
                                || i4 > 319
                                || bitSet.get(q2 = i3 - k + (i4 - m) * o + (i5 - n) * o * p))
                            continue;
                        bitSet.set(q2);
                        if (Math.abs((i3 >> 4) - i) > 1
                                || Math.abs((i5 >> 4) - j) > 1
                                || (baseNow =
                                                OreSim.baseNow(
                                                        world,
                                                        long2ByteOpenHashMap,
                                                        base,
                                                        i3,
                                                        i4,
                                                        i5,
                                                        l = BlockPos.asLong(i3, i4, i5)))
                                        == 0) continue;
                        if (baseNow == 2) {
                            if (r != 0) {
                                ++v;
                                bl2 = true;
                            }
                            long2ByteOpenHashMap2.put(l, (byte) 2);
                            continue;
                        }
                        if (discard <= 0.0f) {
                            p2 = 1;
                        } else if (discard >= 1.0f) {
                            p2 = OreSim.clearOfAir(world, long2ByteOpenHashMap, i3, i4, i5);
                        } else {
                            ++u;
                            if (!bl2) {
                                p2 =
                                        wgRandom.nextFloat() >= discard
                                                ? 1
                                                : (int)
                                                        OreSim.clearOfAir(
                                                                world,
                                                                long2ByteOpenHashMap,
                                                                i3,
                                                                i4,
                                                                i5);
                            } else {
                                p2 =
                                        OreSim.clearOfAir(world, long2ByteOpenHashMap, i3, i4, i5)
                                                        == 1
                                                ? 1
                                                : 2;
                                int r2 = p2;
                            }
                        }
                        if (p2 == 1) {
                            long2ByteOpenHashMap2.put(l, bl ? (byte) 2 : 1);
                            continue;
                        }
                        if (p2 != 2) continue;
                        long2ByteOpenHashMap2.put(l, (byte) 2);
                    }
                }
            }
        }
        if (r == 0) {
            list.add(wgRandom);
            return;
        }
        for (q = 0; q <= v; ++q) {
            WgRandom wgRandom3 = q == v ? wgRandom2 : wgRandom2.copy();
            wgRandom3.skip(u + q);
            list.add(wgRandom3);
        }
    }

    private static byte baseNow(
            World world,
            Long2ByteOpenHashMap long2ByteOpenHashMap,
            boolean bl,
            int i,
            int j,
            int k,
            long l) {
        byte b = long2ByteOpenHashMap.get(l);
        if (b == 0 || bl) {
            return world.base(i, j, k);
        }
        return b == 1 ? (byte) 0 : 2;
    }

    private static byte clearOfAir(
            World world, Long2ByteOpenHashMap long2ByteOpenHashMap, int i, int j, int k) {
        int m = 1;
        for (int[] ints : SIDES) {
            int n = i + ints[0];
            int o = j + ints[1];
            int p = k + ints[2];
            if (o < -64 || o > 319) {
                return 0;
            }
            if (long2ByteOpenHashMap.get(BlockPos.asLong(n, o, p)) == 1) continue;
            byte air = world.air(n, o, p);
            if (air == 1) {
                return 0;
            }
            if (air != 2) continue;
            m = 2;
        }
        return (byte) m;
    }

    private static List<WgRandom> distinct(List<WgRandom> list) {
        if (list.size() <= 1) {
            return list;
        }
        HashMap<Long, List> map = new HashMap<Long, List>();
        ArrayList<WgRandom> list2 = new ArrayList<WgRandom>(list.size());
        for (WgRandom wgRandom : list) {
            List list3 = map.computeIfAbsent(wgRandom.stateHash(), long_ -> new ArrayList(1));
            boolean bl = false;
            for (WgRandom wgRandom2 : (Iterable<WgRandom>) list3) {
                if (!wgRandom2.sameState(wgRandom)) continue;
                bl = true;
                break;
            }
            if (bl) continue;
            list3.add(wgRandom);
            list2.add(wgRandom);
        }
        return list2;
    }

    @Environment(value = EnvType.CLIENT)
    static interface World {

        public long seed();

        public int gateLow(int i, int j);

        public int gateHigh(int i, int j);

        public byte base(int i, int j, int k);

        public byte air(int i, int j, int k);

        public boolean inBiome(OreSpec oreSpec, int i, int j, int k);
    }

    @Environment(value = EnvType.CLIENT)
    static final class Chain {

        final OreSpec spec;

        final int chunkX;

        final int chunkZ;

        final Long2ByteOpenHashMap writes = new Long2ByteOpenHashMap();

        int fogFrom = -1;

        Chain(OreSpec spec2, int chunkX2, int chunkZ2) {
            this.spec = spec2;
            this.chunkX = chunkX2;
            this.chunkZ = chunkZ2;
        }

        boolean fogged() {
            return this.fogFrom >= 0;
        }

        int[] fogBox() {
            int halfWidth = this.spec.halfWidth();
            int reach = this.spec.reach();
            int i = this.chunkX << 4;
            int j = this.chunkZ << 4;
            return new int[] {
                Math.max(i - halfWidth, i - 16),
                Math.max(Math.min(this.spec.yMin(), this.spec.yMax()) - 2 - reach, -64),
                Math.max(j - halfWidth, j - 16),
                Math.min(i + 15 + halfWidth, i + 31),
                Math.min(Math.max(this.spec.yMin(), this.spec.yMax()) + 2 + reach, 319),
                Math.min(j + 15 + halfWidth, j + 31)
            };
        }
    }
}
