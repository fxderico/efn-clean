package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.ChunkLightingView;

@Environment(EnvType.CLIENT)
public final class AmethystLight {

    public static final int SMALL_BUD = 1;

    public static final int MEDIUM_BUD = 2;

    public static final int LARGE_BUD = 4;

    public static final int CLUSTER = 5;

    private static final Direction[] SIDES = Direction.values();

    private AmethystLight() {}

    public static void scan(ClientWorld world, WorldChunk chunk, AmethystLight.Sink sink) {
        ChunkSection[] sectionArray = chunk.getSectionArray();
        int bottomSectionCoord = chunk.getBottomSectionCoord();
        ChunkPos chunkPos = chunk.getPos();
        Mutable mutable = new Mutable();
        for (int j = 0; j < sectionArray.length; j++) {
            ChunkSection section = sectionArray[j];
            if (section != null
                    && !section.isEmpty()
                    && section.getBlockStateContainer().hasAny(AmethystLight::isLining)) {
                int i = (bottomSectionCoord + j) * 16;
                for (int m = 0; m < 16; m++) {
                    for (int n = 0; n < 16; n++) {
                        for (int o = 0; o < 16; o++) {
                            if (section.getBlockState(o, m, n).getLuminance() <= 0) {
                                mutable.set(
                                        chunkPos.getStartX() + o, i + m, chunkPos.getStartZ() + n);
                                int lightLevel = world.getLightLevel(LightType.BLOCK, mutable);
                                if (isGrowthLight(lightLevel)
                                        && touchesLining(world, mutable)
                                        && isLocalMaximum(world, mutable, lightLevel)) {
                                    sink.accept(mutable.asLong(), lightLevel);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void scanSources(ClientWorld world, WorldChunk chunk, AmethystLight.Sink sink) {
        ChunkSection[] sectionArray = chunk.getSectionArray();
        int bottomSectionCoord = chunk.getBottomSectionCoord();
        ChunkPos chunkPos = chunk.getPos();
        AmethystLight.LightCache lightCache =
                new AmethystLight.LightCache(
                        world, chunkPos.x, chunkPos.z, bottomSectionCoord, sectionArray.length);
        Mutable mutable = new Mutable();
        for (int j = 0; j < sectionArray.length; j++) {
            ChunkSection section = sectionArray[j];
            if (section != null
                    && !section.isEmpty()
                    && !lightCache.quiet(j)
                    && section.getBlockStateContainer().hasAny(AmethystLight::isLining)) {
                int i = (bottomSectionCoord + j) * 16;
                for (int m = 0; m < 16; m++) {
                    for (int n = 0; n < 16; n++) {
                        for (int o = 0; o < 16; o++) {
                            BlockState blockState = section.getBlockState(o, m, n);
                            if (blockState.getLuminance() <= 0) {
                                int k = chunkPos.getStartX() + o;
                                int p = i + m;
                                int q = chunkPos.getStartZ() + n;
                                int r = lightCache.get(k, p, q);
                                int brightestAround = lightCache.brightestAround(k, p, q);
                                int t;
                                if (r == 0) {
                                    if (brightestAround != 4 && brightestAround != 3
                                            || !isOpen(blockState)) {
                                        continue;
                                    }
                                    mutable.set(k, p, q);
                                    if (!touchesLining(world, mutable)
                                            || !litOpening(
                                                    world, lightCache, mutable, brightestAround)) {
                                        continue;
                                    }
                                    t = brightestAround == 4 ? 5 : 4;
                                } else {
                                    if (!isGrowthLight(r) || brightestAround >= r) {
                                        continue;
                                    }
                                    mutable.set(k, p, q);
                                    if (!touchesLining(world, mutable)
                                            || r == 4
                                                    && besideDarkOpening(
                                                            world, lightCache, mutable)) {
                                        continue;
                                    }
                                    t = r;
                                }
                                sink.accept(mutable.asLong(), t);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean litOpening(
            ClientWorld world, AmethystLight.LightCache lightCache, BlockPos pos, int i) {
        Mutable mutable = new Mutable();
        for (Direction direction : SIDES) {
            mutable.set(pos, direction);
            if (lightCache.get(mutable.getX(), mutable.getY(), mutable.getZ()) == i
                    && isOpen(world.getBlockState(mutable))) {
                return true;
            }
        }
        return false;
    }

    private static boolean besideDarkOpening(
            ClientWorld world, AmethystLight.LightCache lightCache, BlockPos pos) {
        Mutable mutable = new Mutable();
        for (Direction direction : SIDES) {
            mutable.set(pos, direction);
            BlockState blockState;
            if (lightCache.get(mutable.getX(), mutable.getY(), mutable.getZ()) == 0
                    && isOpen(blockState = world.getBlockState(mutable))
                    && !blockState.isOf(Blocks.VOID_AIR)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOpen(BlockState state) {
        return state.isAir() || state.isOf(Blocks.AMETHYST_CLUSTER) || state.isOf(Blocks.WATER);
    }

    public static boolean isGrowthLight(int i) {
        return i == 1 || i == 2 || i == 4 || i == 5;
    }

    public static boolean touchesLining(ClientWorld world, BlockPos pos) {
        for (Direction direction : SIDES) {
            if (isLining(world.getBlockState(pos.offset(direction)))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isLining(BlockState state) {
        return state.isOf(Blocks.AMETHYST_BLOCK) || state.isOf(Blocks.BUDDING_AMETHYST);
    }

    private static boolean isLocalMaximum(ClientWorld world, BlockPos pos, int i) {
        for (Direction direction : SIDES) {
            if (world.getLightLevel(LightType.BLOCK, pos.offset(direction)) >= i) {
                return false;
            }
        }
        return true;
    }

    @Environment(EnvType.CLIENT)
    private static final class LightCache {

        private final ChunkLightingView listener;

        private final int chunkX;

        private final int chunkZ;

        private final int minSectionY;

        private final int sectionCount;

        private final ChunkNibbleArray[] layers;

        private final boolean[] fetched;

        private LightCache(ClientWorld world, int chunkX2, int chunkZ2, int i, int j) {
            this.listener = world.getLightingProvider().get(LightType.BLOCK);
            this.chunkX = chunkX2;
            this.chunkZ = chunkZ2;
            this.minSectionY = i - 1;
            this.sectionCount = j + 2;
            this.layers = new ChunkNibbleArray[9 * this.sectionCount];
            this.fetched = new boolean[this.layers.length];
        }

        private ChunkNibbleArray layer(int i, int j, int k) {
            int m = ((i + 1) * 3 + j + 1) * this.sectionCount + k;
            if (!this.fetched[m]) {
                this.fetched[m] = true;
                ChunkNibbleArray lightSection =
                        this.listener.getLightSection(
                                ChunkSectionPos.from(
                                        this.chunkX + i, this.minSectionY + k, this.chunkZ + j));
                this.layers[m] =
                        lightSection != null && !lightSection.isUninitialized()
                                ? lightSection
                                : null;
            }
            return this.layers[m];
        }

        int get(int i, int j, int k) {
            int m = (i >> 4) - this.chunkX;
            int n = (k >> 4) - this.chunkZ;
            int o = (j >> 4) - this.minSectionY;
            if (m >= -1 && m <= 1 && n >= -1 && n <= 1 && o >= 0 && o < this.sectionCount) {
                ChunkNibbleArray chunkNibbleArray = this.layer(m, n, o);
                return chunkNibbleArray == null ? 0 : chunkNibbleArray.get(i & 15, j & 15, k & 15);
            } else {
                return 0;
            }
        }

        int brightestAround(int i, int j, int k) {
            int m = this.get(i + 1, j, k);
            m = Math.max(m, this.get(i - 1, j, k));
            m = Math.max(m, this.get(i, j + 1, k));
            m = Math.max(m, this.get(i, j - 1, k));
            m = Math.max(m, this.get(i, j, k + 1));
            return Math.max(m, this.get(i, j, k - 1));
        }

        boolean quiet(int i) {
            int j = i + 1;
            return this.layer(0, 0, j) == null
                    && this.layer(0, 0, j - 1) == null
                    && this.layer(0, 0, j + 1) == null
                    && this.layer(-1, 0, j) == null
                    && this.layer(1, 0, j) == null
                    && this.layer(0, -1, j) == null
                    && this.layer(0, 1, j) == null;
        }
    }

    @FunctionalInterface
    @Environment(EnvType.CLIENT)
    public interface Sink {

        void accept(long l, int i);
    }
}
