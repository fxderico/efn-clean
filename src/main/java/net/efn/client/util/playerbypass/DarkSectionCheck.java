package net.efn.client.util.playerbypass;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

@Environment(EnvType.CLIENT)
final class DarkSectionCheck {

    private static final int[][] SIDES =
            new int[][] {{0, 1, 0}, {0, -1, 0}, {0, 0, -1}, {0, 0, 1}, {1, 0, 0}, {-1, 0, 0}};

    private DarkSectionCheck() {}

    static boolean airlessAround(ClientWorld world, int i, int j, int k) {
        if (!airless(world, i, j, k)) {
            return false;
        } else {
            for (int[] ints : SIDES) {
                if (!airless(world, i + ints[0], j + ints[1], k + ints[2])) {
                    return false;
                }
            }
            return true;
        }
    }

    private static boolean airless(ClientWorld world, int i, int j, int k) {
        if (j >= world.getBottomSectionCoord() && j <= world.getTopSectionCoord()) {
            WorldChunk chunk = world.getChunkManager().getChunk(i, k, ChunkStatus.FULL, false);
            if (chunk == null) {
                return false;
            } else {
                ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(j));
                if (section.isEmpty()) {
                    return false;
                } else if (!section.getBlockStateContainer().hasAny(AbstractBlockState::isAir)) {
                    return true;
                } else {
                    for (int m = 0; m < 16; m++) {
                        for (int n = 0; n < 16; n++) {
                            for (int o = 0; o < 16; o++) {
                                if (section.getBlockState(o, m, n).isAir()) {
                                    return false;
                                }
                            }
                        }
                    }
                    return true;
                }
            }
        } else {
            return true;
        }
    }
}
