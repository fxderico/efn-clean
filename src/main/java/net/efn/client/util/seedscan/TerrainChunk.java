package net.efn.client.util.seedscan;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;

@Environment(EnvType.CLIENT)
public final class TerrainChunk {

    public static final int MIN_Y = -64;

    public static final int HEIGHT = 384;

    public static final int SECTIONS = 24;

    public final int chunkX;

    public final int chunkZ;

    private final byte[][] nibbles = new byte[24][];

    private final byte[] uniform = new byte[24];

    private final short[] oceanFloor = new short[256];

    private TerrainChunk(int chunkX2, int chunkZ2) {
        this.chunkX = chunkX2;
        this.chunkZ = chunkZ2;
    }

    static TerrainChunk of(ProtoChunk protoChunk) {
        TerrainChunk terrainChunk = new TerrainChunk(protoChunk.getPos().x, protoChunk.getPos().z);
        for (int i = 0; i < 24; i++) {
            ChunkSection section = protoChunk.getSection(i);
            if (section.isEmpty()) {
                terrainChunk.uniform[i] = 0;
            } else {
                byte b = VBlock.of(section.getBlockState(0, 0, 0));
                byte[] bytes = null;
                for (int j = 0; j < 4096; j++) {
                    BlockState blockState = section.getBlockState(j & 15, j >> 8, j >> 4 & 15);
                    byte b2 = VBlock.of(blockState);
                    if (bytes == null) {
                        if (b2 == b) {
                            continue;
                        }
                        bytes = new byte[2048];
                        for (int k = 0; k < j; k++) {
                            put(bytes, k, b);
                        }
                    }
                    put(bytes, j, b2);
                }
                terrainChunk.uniform[i] = b;
                terrainChunk.nibbles[i] = bytes;
            }
        }
        for (int m = 0; m < 16; m++) {
            for (int n = 0; n < 16; n++) {
                terrainChunk.oceanFloor[n + 16 * m] =
                        (short) (protoChunk.sampleHeightmap(Type.OCEAN_FLOOR_WG, n, m) + 1);
            }
        }
        return terrainChunk;
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

    public int oceanFloor(int i, int j) {
        return this.oceanFloor[(i & 15) + 16 * (j & 15)];
    }
}
