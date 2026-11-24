package net.efn.client.util.seedscan;

import it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;

import java.util.Iterator;

@Environment(EnvType.CLIENT)
public final class OrePredictor {

    private static final ThreadLocal<OrePredictor.Answers> ANSWERS =
            ThreadLocal.withInitial(OrePredictor.Answers::new);

    private OrePredictor() {}

    static boolean baseish(byte b) {
        return VBlock.isBase(b) || b == 11;
    }

    static boolean disagreesOnBase(OrePredictor.Inputs inputs2, byte b, int i, int j, int k) {
        byte shown = inputs2.shown(i, j, k);
        if (shown == 15) {
            return true;
        } else {
            boolean baseish = baseish(b);
            return shown != 11
                    ? baseish != VBlock.isBase(shown)
                    : !baseish || b != 11 && !inputs2.predictedOre(i, j, k);
        }
    }

    static boolean disagreesOnAir(OrePredictor.Inputs inputs2, byte b, int i, int j, int k) {
        byte shown = inputs2.shown(i, j, k);
        return shown == 15 ? true : b == 0 != (shown == 0) || disagreesOnBase(inputs2, b, i, j, k);
    }

    public static ChunkDecoration predict(
            SeedWorldgen seedWorldgen, int i, int j, OrePredictor.Inputs inputs2) {
        OrePredictor.View view = new OrePredictor.View(seedWorldgen, i, j, inputs2);
        Iterator iterator = seedWorldgen.specs().iterator();
        while (iterator.hasNext()) {
            OreSpec oreSpec;
            view.spec = oreSpec = (OreSpec) iterator.next();
            OreSim.Chain chain = OreSim.run(oreSpec, i, j, view);
            ObjectIterator objectIterator = chain.writes.long2ByteEntrySet().fastIterator();
            while (objectIterator.hasNext()) {
                Entry entry = (Entry) objectIterator.next();
                view.builder.add(
                        entry.getLongKey(),
                        ChunkDecoration.pack(
                                oreSpec.order(), oreSpec.kind(), entry.getByteValue() == 2));
            }
            if (chain.fogged()) {
                int[] ints = chain.fogBox();
                view.builder.fog(
                        new ChunkDecoration.Fog(
                                oreSpec.order(),
                                oreSpec.kind(),
                                ints[0],
                                ints[1],
                                ints[2],
                                ints[3],
                                ints[4],
                                ints[5]));
            }
        }
        return view.builder.build();
    }

    @Environment(EnvType.CLIENT)
    private static final class Answers {

        static final int SIZE = 884736;

        final int[] stamp = new int[884736];

        final byte[] answer = new byte[884736];

        int generation;

        private Answers() {}
    }

    @Environment(EnvType.CLIENT)
    public interface Inputs {

        TerrainChunk terrain(int i, int j);

        GateHeights gateHeights(int i, int j);

        boolean othersMayWrite(int i, int j, int k);

        byte shown(int i, int j, int k);

        boolean predictedOre(int i, int j, int k);
    }

    @Environment(EnvType.CLIENT)
    private static final class View implements OreSim.World {

        final SeedWorldgen world;

        final int chunkX;

        final int chunkZ;

        final OrePredictor.Inputs inputs;

        final TerrainChunk[] terrains = new TerrainChunk[9];

        final GateHeights[] heights = new GateHeights[9];

        final ChunkDecoration.Builder builder;

        final OrePredictor.Answers answers = OrePredictor.ANSWERS.get();

        final int generation;

        final int minX;

        final int minZ;

        OreSpec spec;

        View(SeedWorldgen world2, int chunkX2, int chunkZ2, OrePredictor.Inputs inputs2) {
            this.world = world2;
            this.chunkX = chunkX2;
            this.chunkZ = chunkZ2;
            this.inputs = inputs2;
            this.builder = new ChunkDecoration.Builder(chunkX2, chunkZ2);
            for (int k = -1; k <= 1; k++) {
                for (int m = -1; m <= 1; m++) {
                    this.terrains[(k + 1) * 3 + m + 1] = inputs2.terrain(chunkX2 + k, chunkZ2 + m);
                    this.heights[(k + 1) * 3 + m + 1] =
                            inputs2.gateHeights(chunkX2 + k, chunkZ2 + m);
                }
            }
            this.generation = ++this.answers.generation;
            this.minX = chunkX2 - 1 << 4;
            this.minZ = chunkZ2 - 1 << 4;
        }

        private int around(int i, int j) {
            int k = (i >> 4) - this.chunkX;
            int m = (j >> 4) - this.chunkZ;
            if (k >= -1 && k <= 1 && m >= -1 && m <= 1) {
                return (k + 1) * 3 + m + 1;
            } else {
                throw new IllegalStateException("read outside the 3x3 chunks at " + i + ", " + j);
            }
        }

        private TerrainChunk terrain(int i, int j) {
            return this.terrains[this.around(i, j)];
        }

        private byte answers(int i, int j, int k) {
            int m = k - this.minZ;
            int n = i - this.minX;
            int o = ((j - -64) * 48 + m) * 48 + n;
            if (this.answers.stamp[o] == this.generation) {
                return this.answers.answer[o];
            } else {
                byte kind = this.terrain(i, k).kind(i, j, k);
                int p = VBlock.isBase(kind) ? 1 : 0;
                boolean disagreesOnBase = OrePredictor.disagreesOnBase(this.inputs, kind, i, j, k);
                int q = disagreesOnBase ? 2 : p;
                int r = q;
                if (q == 1 && this.inputs.othersMayWrite(i, j, k)) {
                    r = 2;
                }
                int t =
                        OrePredictor.disagreesOnAir(this.inputs, kind, i, j, k)
                                ? 2
                                : (kind == 0 ? 1 : 0);
                byte b = (byte) (r | q << 2 | t << 4);
                this.answers.stamp[o] = this.generation;
                this.answers.answer[o] = b;
                return b;
            }
        }

        @Override
        public long seed() {
            return this.world.seed;
        }

        @Override
        public int gateLow(int i, int j) {
            return this.heights[this.around(i, j)].low(i, j);
        }

        @Override
        public int gateHigh(int i, int j) {
            return this.heights[this.around(i, j)].high(i, j);
        }

        @Override
        public byte base(int i, int j, int k) {
            int m;
            if (this.spec.drawsOnTerrain()) {
                m = this.answers(i, j, k) & 3;
                if (m == 1 && this.ownFogCovers(i, j, k)) {
                    m = 2;
                }
            } else if (this.spec.discard() > 0.0F) {
                m = (byte) (this.answers(i, j, k) >> 2 & 3);
            } else {
                m = VBlock.isBase(this.terrain(i, k).kind(i, j, k)) ? 1 : 0;
            }
            ShortArrayList shortArrayList;
            if ((shortArrayList = this.builder.at(BlockPos.asLong(i, j, k))) != null) {
                for (int n = 0; n < shortArrayList.size(); n++) {
                    short s = shortArrayList.getShort(n);
                    if (!VBlock.isBase(ChunkDecoration.kind(s))) {
                        m = ChunkDecoration.possible(s) && m != 0 ? 2 : 0;
                    }
                }
            }
            return (byte) m;
        }

        private boolean ownFogCovers(int i, int j, int k) {
            for (ChunkDecoration.Fog fog : this.builder.fogList()) {
                if (!VBlock.isBase(fog.kind()) && fog.contains(i, j, k)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public byte air(int i, int j, int k) {
            if (j >= -64 && j <= 319) {
                if (this.spec.discard() > 0.0F) {
                    return (byte) (this.answers(i, j, k) >> 4 & 3);
                } else {
                    return (byte) (this.terrain(i, k).kind(i, j, k) == 0 ? 1 : 0);
                }
            } else {
                return 1;
            }
        }

        @Override
        public boolean inBiome(OreSpec oreSpec, int i, int j, int k) {
            return oreSpec.biomes() == null || oreSpec.inBiome(this.world.biomeAt(i, j, k));
        }
    }
}
