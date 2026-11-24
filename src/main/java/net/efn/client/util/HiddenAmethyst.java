package net.efn.client.util;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

@Environment(EnvType.CLIENT)
public final class HiddenAmethyst {

    private static final int TOP_SECTION = 2;

    private static final int RING_LIGHT = 4;

    private static final int CLUSTER_LIGHT = 5;

    private static final int BRIGHT_NEIGHBOUR = 2;

    private static final int UNKNOWN_BLOCKER = 15;

    private static final Direction[] SIDES = Direction.values();

    private static final BlockState VOID = Blocks.VOID_AIR.getDefaultState();

    private HiddenAmethyst() {}

    public static HiddenAmethyst.Reading read(
            ClientWorld world, WorldChunk chunk, ServerBlockLight serverBlockLight, Boolean flag) {
        int bottomSectionCoord = chunk.getBottomSectionCoord();
        int i = Math.min(2, chunk.getTopSectionCoord());
        if (i < bottomSectionCoord) {
            return HiddenAmethyst.Reading.NONE;
        } else {
            boolean[] booleans = geodeSections(chunk, bottomSectionCoord, i);
            boolean bl = false;
            for (boolean bl2 : booleans) {
                bl |= bl2;
            }
            return !bl
                    ? HiddenAmethyst.Reading.NONE
                    : new HiddenAmethyst.Scan(world, chunk, serverBlockLight, flag)
                            .run(booleans, bottomSectionCoord, i);
        }
    }

    private static boolean[] geodeSections(WorldChunk chunk, int i, int j) {
        boolean[] booleans = new boolean[j - i + 1];
        if (chunk == null) {
            return booleans;
        } else {
            ChunkSection[] sectionArray = chunk.getSectionArray();
            for (int k = i; k <= j; k++) {
                ChunkSection section = sectionArray[chunk.sectionCoordToIndex(k)];
                booleans[k - i] =
                        section != null
                                && !section.isEmpty()
                                && section.hasAny(HiddenAmethyst::isAmethyst);
            }
            return booleans;
        }
    }

    private static boolean isLining(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.AMETHYST_BLOCK || block == Blocks.BUDDING_AMETHYST;
    }

    private static boolean isCrystal(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.SMALL_AMETHYST_BUD
                || block == Blocks.MEDIUM_AMETHYST_BUD
                || block == Blocks.LARGE_AMETHYST_BUD
                || block == Blocks.AMETHYST_CLUSTER;
    }

    private static boolean isAmethyst(BlockState state) {
        return isLining(state) || isCrystal(state);
    }

    private static boolean isOpen(BlockState state) {
        return state.isAir() && state != VOID || state.isOf(Blocks.WATER);
    }

    private static boolean isGrowthLight(int i) {
        return i == 1 || i == 2 || i == 4 || i == 5;
    }

    @Environment(EnvType.CLIENT)
    public record Reading(int buds, int clusters, int darkVotes, int glowVotes) {

        public static final HiddenAmethyst.Reading NONE = new HiddenAmethyst.Reading(0, 0, 0, 0);
    }

    @Environment(EnvType.CLIENT)
    private static final class Scan {

        private final WorldChunk[] chunks = new WorldChunk[9];

        private final ServerBlockLight.Column[] columns = new ServerBlockLight.Column[9];

        private final int chunkX;

        private final int chunkZ;

        private final ClientWorld level;

        private final boolean solidsPass;

        private final boolean solidRing;

        private final Long2ByteOpenHashMap darkClusters = new Long2ByteOpenHashMap();

        private Scan(
                ClientWorld level2,
                WorldChunk chunk,
                ServerBlockLight serverBlockLight,
                Boolean flag) {
            this.level = level2;
            this.chunkX = chunk.getPos().x;
            this.chunkZ = chunk.getPos().z;
            this.solidsPass = Boolean.TRUE.equals(flag);
            this.solidRing = !Boolean.FALSE.equals(flag);
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int k = (i + 1) * 3 + j + 1;
                    this.chunks[k] =
                            i == 0 && j == 0
                                    ? chunk
                                    : level2.getChunkManager()
                                            .getWorldChunk(this.chunkX + i, this.chunkZ + j, false);
                    this.columns[k] =
                            serverBlockLight.column(level2, this.chunkX + i, this.chunkZ + j);
                }
            }
            this.darkClusters.defaultReturnValue((byte) -1);
        }

        private HiddenAmethyst.Reading run(boolean[] booleans, int i, int j) {
            boolean[][] booleanGrid =
                    new boolean[][] {
                        HiddenAmethyst.geodeSections(this.chunks[this.slot(1, 0)], i, j),
                        HiddenAmethyst.geodeSections(this.chunks[this.slot(-1, 0)], i, j),
                        HiddenAmethyst.geodeSections(this.chunks[this.slot(0, 1)], i, j),
                        HiddenAmethyst.geodeSections(this.chunks[this.slot(0, -1)], i, j)
                    };
            int k = 0;
            int m = 0;
            int n = 0;
            int o = 0;
            int p = this.chunkX << 4;
            int q = this.chunkZ << 4;
            for (int r = i; r <= j; r++) {
                int t = r - i;
                boolean bl =
                        booleans[t]
                                || t > 0 && booleans[t - 1]
                                || t + 1 < booleans.length && booleans[t + 1];
                for (boolean[] booleans2 : booleanGrid) {
                    bl |= booleans2[t];
                }
                int u;
                if (bl) {
                    for (int v = u = r << 4; v < u + 16; v++) {
                        for (int w = q; w < q + 16; w++) {
                            for (int i2 = p; i2 < p + 16; i2++) {
                                BlockState state = this.state(i2, v, w);
                                if (!HiddenAmethyst.isCrystal(state)) {
                                    int light = this.light(i2, v, w);
                                    if (light == 0) {
                                        if (this.brightestStored(i2, v, w) >= 2
                                                && this.darkCandidate(i2, v, w, state)) {
                                            if (this.darkNextToGlow(i2, v, w, state)) {
                                                n++;
                                            }
                                            if (this.darkCluster(i2, v, w)) {
                                                m++;
                                            }
                                        }
                                    } else if (light >= 0
                                            && this.passesLight(i2, v, w, state, light)
                                            && this.touchesLining(i2, v, w)
                                            && light >= this.brightestAround(i2, v, w)
                                            && (light != 4
                                                    || !this.besideDarkCluster(i2, v, w)
                                                            && (!this.solidRing
                                                                    || !this.besideDarkSolid(
                                                                            i2, v, w)))
                                            && HiddenAmethyst.isGrowthLight(light)
                                            && this.fadesEvenly(i2, v, w, light)) {
                                        if (light == 5) {
                                            o++;
                                            m++;
                                        } else {
                                            k++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return new HiddenAmethyst.Reading(k, m, n, o);
        }

        private int slot(int i, int j) {
            return (i + 1) * 3 + j + 1;
        }

        private int slotOf(int i, int j) {
            int k = (i >> 4) - this.chunkX;
            int m = (j >> 4) - this.chunkZ;
            return k >= -1 && k <= 1 && m >= -1 && m <= 1 ? this.slot(k, m) : -1;
        }

        private BlockState state(int i, int j, int k) {
            if (this.level.isOutOfHeightLimit(j)) {
                return HiddenAmethyst.VOID;
            } else {
                int slot = this.slotOf(i, k);
                if (slot < 0) {
                    return this.level.getBlockState(new BlockPos(i, j, k));
                } else {
                    WorldChunk chunk = this.chunks[slot];
                    if (chunk == null) {
                        return HiddenAmethyst.VOID;
                    } else {
                        ChunkSection section = chunk.getSectionArray()[chunk.getSectionIndex(j)];
                        return section == null
                                ? HiddenAmethyst.VOID
                                : section.getBlockState(i & 15, j & 15, k & 15);
                    }
                }
            }
        }

        private int light(int i, int j, int k) {
            int slot = this.slotOf(i, k);
            ServerBlockLight.Column column = slot < 0 ? null : this.columns[slot];
            return column == null ? -1 : column.get(i, j, k);
        }

        private boolean touchesLining(int i, int j, int k) {
            for (Direction direction : HiddenAmethyst.SIDES) {
                if (HiddenAmethyst.isLining(
                        this.state(
                                i + direction.getOffsetX(),
                                j + direction.getOffsetY(),
                                k + direction.getOffsetZ()))) {
                    return true;
                }
            }
            return false;
        }

        private boolean hiddenSolid(int i, int j, int k, BlockState state) {
            return state != HiddenAmethyst.VOID
                    && !HiddenAmethyst.isOpen(state)
                    && !HiddenAmethyst.isAmethyst(state)
                    && state.getLuminance() == 0
                    && this.touchesLining(i, j, k);
        }

        private boolean solidPasses(int i, int j, int k, BlockState state) {
            return this.solidsPass && this.hiddenSolid(i, j, k, state);
        }

        private boolean passesLight(int i, int j, int k, BlockState state, int m) {
            return HiddenAmethyst.isOpen(state) || m > 0 && this.solidPasses(i, j, k, state);
        }

        private boolean passesLight(int i, int j, int k) {
            BlockState state = this.state(i, j, k);
            return HiddenAmethyst.isOpen(state)
                    || this.solidPasses(i, j, k, state) && this.light(i, j, k) > 0;
        }

        private boolean darkCandidate(int i, int j, int k, BlockState state) {
            return (HiddenAmethyst.isOpen(state) || this.solidPasses(i, j, k, state))
                    && this.touchesLining(i, j, k);
        }

        private boolean darkNextToGlow(int i, int j, int k, BlockState state) {
            if (HiddenAmethyst.isOpen(state) && this.light(i, j, k) == 0) {
                for (Direction direction : HiddenAmethyst.SIDES) {
                    int m = i + direction.getOffsetX();
                    int n;
                    int o;
                    if (this.passesLight(
                                    m,
                                    o = j + direction.getOffsetY(),
                                    n = k + direction.getOffsetZ())
                            && this.light(m, o, n) >= 2) {
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        }

        private boolean countsAsNeighbour(int i, int j, int k) {
            return this.passesLight(i, j, k) && !this.darkNextToGlow(i, j, k, this.state(i, j, k));
        }

        private boolean darkCluster(int i, int j, int k) {
            long l = BlockPos.asLong(i, j, k);
            byte darkCluster = this.darkClusters.get(l);
            if (darkCluster >= 0) {
                return darkCluster == 1;
            } else {
                boolean bl = false;
                BlockState state = this.state(i, j, k);
                if (this.light(i, j, k) == 0 && this.darkCandidate(i, j, k, state)) {
                    int m = 0;
                    boolean bl2 = false;
                    for (Direction direction : HiddenAmethyst.SIDES) {
                        int n = i + direction.getOffsetX();
                        int light;
                        int o;
                        int p;
                        if (this.countsAsNeighbour(
                                        n,
                                        p = j + direction.getOffsetY(),
                                        o = k + direction.getOffsetZ())
                                && (light = this.light(n, p, o)) != -1) {
                            if (light >= 4) {
                                m++;
                            } else {
                                bl2 = true;
                            }
                        }
                    }
                    bl = !bl2 && m >= (HiddenAmethyst.isOpen(state) ? 1 : 2);
                }
                this.darkClusters.put(l, (byte) (bl ? 1 : 0));
                return bl;
            }
        }

        private boolean besideDarkCluster(int i, int j, int k) {
            for (Direction direction : HiddenAmethyst.SIDES) {
                if (this.darkCluster(
                        i + direction.getOffsetX(),
                        j + direction.getOffsetY(),
                        k + direction.getOffsetZ())) {
                    return true;
                }
            }
            return false;
        }

        private boolean besideDarkSolid(int i, int j, int k) {
            for (Direction direction : HiddenAmethyst.SIDES) {
                int m = i + direction.getOffsetX();
                int n;
                int o;
                if (this.light(m, o = j + direction.getOffsetY(), n = k + direction.getOffsetZ())
                                == 0
                        && this.hiddenSolid(m, o, n, this.state(m, o, n))) {
                    return true;
                }
            }
            return false;
        }

        private int seenLight(int i, int j, int k) {
            int light = this.light(i, j, k);
            if (light != -1) {
                return light;
            } else {
                BlockState state = this.state(i, j, k);
                boolean bl =
                        state != HiddenAmethyst.VOID
                                && !HiddenAmethyst.isOpen(state)
                                && !this.solidPasses(i, j, k, state)
                                && state.getLuminance() == 0;
                return bl ? 0 : 15;
            }
        }

        private int brightestStored(int i, int j, int k) {
            int m = -1;
            for (Direction direction : HiddenAmethyst.SIDES) {
                m =
                        Math.max(
                                m,
                                this.light(
                                        i + direction.getOffsetX(),
                                        j + direction.getOffsetY(),
                                        k + direction.getOffsetZ()));
            }
            return m;
        }

        private int brightestAround(int i, int j, int k) {
            int m = 0;
            for (Direction direction : HiddenAmethyst.SIDES) {
                m =
                        Math.max(
                                m,
                                this.seenLight(
                                        i + direction.getOffsetX(),
                                        j + direction.getOffsetY(),
                                        k + direction.getOffsetZ()));
            }
            return m;
        }

        private boolean fadesEvenly(int i, int j, int k, int m) {
            for (Direction direction : HiddenAmethyst.SIDES) {
                int n = i + direction.getOffsetX();
                int light;
                int o;
                int p;
                if (this.countsAsNeighbour(
                                n, p = j + direction.getOffsetY(), o = k + direction.getOffsetZ())
                        && (light = this.light(n, p, o)) != -1
                        && light < m - 1) {
                    return false;
                }
            }
            return true;
        }
    }
}
