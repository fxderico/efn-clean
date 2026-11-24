package net.efn.client.util.seedscan;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class WgRandom {

    private long lo;

    private long hi;

    WgRandom() {}

    private WgRandom(long lo2, long hi2) {
        this.lo = lo2;
        this.hi = hi2;
    }

    WgRandom copy() {
        return new WgRandom(this.lo, this.hi);
    }

    boolean sameState(WgRandom wgRandom) {
        return this.lo == wgRandom.lo && this.hi == wgRandom.hi;
    }

    long stateHash() {
        return this.lo * 31L + this.hi;
    }

    private static long mix(long l) {
        l = (l ^ l >>> 30) * -4658895280553007687L;
        l = (l ^ l >>> 27) * -7723592293110705685L;
        return l ^ l >>> 31;
    }

    void setSeed(long l) {
        long l2 = l ^ 7640891576956012809L;
        long l3 = l2 + -7046029254386353131L;
        this.lo = mix(l2);
        this.hi = mix(l3);
        if ((this.lo | this.hi) == 0L) {
            this.lo = -7046029254386353131L;
            this.hi = 7640891576956012809L;
        }
    }

    private long step() {
        long lo2 = this.lo;
        long hi2 = this.hi;
        long l = Long.rotateLeft(lo2 + hi2, 17) + lo2;
        long l2;
        this.lo = Long.rotateLeft(lo2, 49) ^ (l2 = hi2 ^ lo2) ^ l2 << 21;
        this.hi = Long.rotateLeft(l2, 28);
        return l;
    }

    void skip(int i) {
        for (int j = 0; j < i; j++) {
            this.step();
        }
    }

    int next(int i) {
        return (int) (this.step() >>> 64 - i);
    }

    int nextInt(int i) {
        if ((i & i - 1) == 0) {
            return (int) ((long) i * this.next(31) >> 31);
        } else {
            int j;
            int k;
            while ((k = this.next(31)) - (j = k % i) + (i - 1) < 0) {}
            return j;
        }
    }

    int between(int i, int j) {
        return this.nextInt(j - i + 1) + i;
    }

    long nextLong() {
        int i = this.next(32);
        int j = this.next(32);
        return ((long) i << 32) + j;
    }

    float nextFloat() {
        return this.next(24) * 5.9604645E-8F;
    }

    double nextDouble() {
        int i = this.next(26);
        int j = this.next(27);
        return (((long) i << 27) + j) * 1.1102230246251565E-16;
    }

    long setDecorationSeed(long seed, int i, int j) {
        this.setSeed(seed);
        long l = this.nextLong() | 1L;
        long l2 = this.nextLong() | 1L;
        long l3 = i * l + j * l2 ^ seed;
        this.setSeed(l3);
        return l3;
    }

    void setFeatureSeed(long l, int i, int j) {
        this.setSeed(l + i + 10000L * j);
    }
}
