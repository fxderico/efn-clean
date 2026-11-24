package net.efn.client.util.quantum;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.PaletteProvider;
import net.minecraft.world.chunk.PalettedContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;

@Environment(EnvType.CLIENT)
public final class ChunkSnapshotStore {

    public static final int MIN_SECTION_INDEX = 6;

    public static final int MAX_SECTION_INDEX = 23;

    private static final PaletteProvider<BlockState> STRATEGY =
            PaletteProvider.forBlockStates(Block.STATE_IDS);

    private final Map<Long, PalettedContainer<BlockState>[]> snapshots = new HashMap<>();

    private final BlockState air;

    public ChunkSnapshotStore(BlockState air2) {
        this.air = air2;
    }

    public static long chunkKey(int i, int j) {
        return (long) i << 32 | j & 4294967295L;
    }

    public void store(int i, int j, PalettedContainer<BlockState>[] palettedContainers) {
        this.snapshots.put(chunkKey(i, j), palettedContainers);
    }

    public void retain(LongPredicate longPredicate) {
        this.snapshots.keySet().removeIf(long_ -> !longPredicate.test(long_));
    }

    public void clear() {
        this.snapshots.clear();
    }

    public void update(int i, int j, int k, int m, int n, int o, BlockState state) {
        if (k >= 6 && k <= 23) {
            PalettedContainer[] snapshot = this.snapshots.get(chunkKey(i, j));
            if (snapshot != null && k < snapshot.length) {
                PalettedContainer palettedContainer = snapshot[k];
                if (palettedContainer == null) {
                    if (state == this.air) {
                        return;
                    }
                    snapshot[k] = palettedContainer = new PalettedContainer<>(this.air, STRATEGY);
                }
                palettedContainer.set(m, n, o, state);
            }
        }
    }

    public List<ChunkSnapshotStore.BlockChange> compare(
            int i, int j, PalettedContainer<BlockState>[] palettedContainers, int k) {
        ArrayList list = new ArrayList();
        PalettedContainer[] snapshot = this.snapshots.get(chunkKey(i, j));
        if (this.isAllAir(snapshot)) {
            return list;
        } else {
            for (int m = 6; m <= 23; m++) {
                PalettedContainer palettedContainer = m < snapshot.length ? snapshot[m] : null;
                PalettedContainer palettedContainer2 =
                        m < palettedContainers.length ? palettedContainers[m] : null;
                if (palettedContainer != null || palettedContainer2 != null) {
                    int n = (m + k) * 16;
                    for (int o = 0; o < 16; o++) {
                        for (int p = 0; p < 16; p++) {
                            for (int q = 0; q < 16; q++) {
                                BlockState state =
                                        palettedContainer != null
                                                ? (BlockState) palettedContainer.get(q, o, p)
                                                : this.air;
                                BlockState state2 =
                                        palettedContainer2 != null
                                                ? (BlockState) palettedContainer2.get(q, o, p)
                                                : this.air;
                                if (state != state2) {
                                    list.add(
                                            new ChunkSnapshotStore.BlockChange(
                                                    i * 16 + q, n + o, j * 16 + p, state2, state));
                                }
                            }
                        }
                    }
                }
            }
            return list;
        }
    }

    private boolean isAllAir(PalettedContainer<BlockState>[] palettedContainers) {
        if (palettedContainers == null) {
            return true;
        } else {
            for (int i = 6; i <= 23 && i < palettedContainers.length; i++) {
                if (palettedContainers[i] != null) {
                    return false;
                }
            }
            return true;
        }
    }

    @Environment(EnvType.CLIENT)
    public record BlockChange(
            int worldX, int worldY, int worldZ, BlockState newState, BlockState oldState) {}
}
