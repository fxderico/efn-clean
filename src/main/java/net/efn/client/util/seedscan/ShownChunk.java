package net.efn.client.util.seedscan;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.world.chunk.PalettedContainer;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public final class ShownChunk {

    static final int KEEP_TOP = 207;

    public final int chunkX;

    public final int chunkZ;

    private final byte[][] nibbles = new byte[24][];

    private final byte[] uniform = new byte[24];

    private final short[] terrainTop = new short[256];

    private final boolean[] iceTop = new boolean[256];

    private final short[] motionTop = new short[256];

    private ShownChunk(int chunkX2, int chunkZ2) {
        this.chunkX = chunkX2;
        this.chunkZ = chunkZ2;
        Arrays.fill(this.terrainTop, (short) -64);
        Arrays.fill(this.motionTop, (short) -64);
    }

    public static ShownChunk of(int i, int j, PalettedContainer<BlockState>[] palettedContainers) {
        ShownChunk shownChunk = new ShownChunk(i, j);
        for (int k = 0; k < 24; k++) {
            PalettedContainer palettedContainer =
                    k < palettedContainers.length ? palettedContainers[k] : null;
            if (palettedContainer == null) {
                shownChunk.uniform[k] = 0;
            } else {
                boolean bl = -64 + (k << 4) <= 207;
                byte b = VBlock.of((BlockState) palettedContainer.get(0, 0, 0));
                byte[] bytes = null;
                for (int m = 0; m < 4096; m++) {
                    int n = m & 15;
                    int o = m >> 8;
                    int p = m >> 4 & 15;
                    BlockState state = (BlockState) palettedContainer.get(n, o, p);
                    byte b2 = VBlock.of(state);
                    int q = -64 + (k << 4) + o + 1;
                    int r = n + 16 * p;
                    if (b2 != 0 && state.blocksMovement() && q > shownChunk.motionTop[r]) {
                        shownChunk.motionTop[r] = (short) q;
                    }
                    if (isTerrain(state, b2) && q > shownChunk.terrainTop[r]) {
                        shownChunk.terrainTop[r] = (short) q;
                        shownChunk.iceTop[r] = isIce(state);
                    }
                    if (bl) {
                        if (bytes == null) {
                            if (b2 == b) {
                                continue;
                            }
                            bytes = new byte[2048];
                            for (int t = 0; t < m; t++) {
                                put(bytes, t, b);
                            }
                        }
                        put(bytes, m, b2);
                    }
                }
                shownChunk.uniform[k] = bl ? b : 15;
                shownChunk.nibbles[k] = bytes;
            }
        }
        return shownChunk;
    }

    static boolean isTerrain(BlockState state, byte b) {
        if (b == 0 || b == 1 || b == 2) {
            return false;
        } else if (b != 12) {
            return true;
        } else if (!state.blocksMovement()) {
            return false;
        } else {
            String path = Registries.BLOCK.getId(state.getBlock()).getPath();
            return path.endsWith("dirt")
                    || path.equals("grass_block")
                    || path.equals("podzol")
                    || path.equals("mycelium")
                    || path.endsWith("sand")
                    || path.endsWith("sandstone")
                    || path.endsWith("terracotta")
                    || path.equals("snow_block")
                    || path.equals("packed_ice")
                    || path.equals("blue_ice")
                    || path.equals("mud")
                    || path.equals("calcite")
                    || path.equals("powder_snow")
                    || path.equals("moss_block")
                    || path.equals("clay")
                    || path.equals("dripstone_block")
                    || path.equals("magma_block")
                    || path.equals("smooth_basalt");
        }
    }

    static boolean isIce(BlockState state) {
        return state.isOf(Blocks.PACKED_ICE)
                || state.isOf(Blocks.BLUE_ICE)
                || state.isOf(Blocks.SNOW_BLOCK);
    }

    private static void put(byte[] bytes, int i, byte b) {
        int j = i >> 1;
        bytes[j] = (i & 1) == 0 ? (byte) (bytes[j] & 240 | b) : (byte) (bytes[j] & 15 | b << 4);
    }

    public byte kind(int i, int j, int k) {
        if (j >= -64 && j < 320) {
            int m = j - -64 >> 4;
            byte[] nibble = this.nibbles[m];
            if (nibble == null) {
                return this.uniform[m];
            } else {
                int n = (j & 15) << 8 | (k & 15) << 4 | i & 15;
                byte b = nibble[n >> 1];
                return (byte) ((n & 1) == 0 ? b & 15 : b >> 4 & 15);
            }
        } else {
            return 0;
        }
    }

    public int terrainTop(int i, int j) {
        int k = (i & 15) + 16 * (j & 15);
        return this.iceTop[k] ? -2147483648 : this.terrainTop[k];
    }

    public int motionTop(int i, int j) {
        return this.motionTop[(i & 15) + 16 * (j & 15)];
    }
}
