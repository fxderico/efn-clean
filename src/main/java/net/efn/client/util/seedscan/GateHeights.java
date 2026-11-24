package net.efn.client.util.seedscan;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GateHeights {

    public static final int TREE_ALLOWANCE = Integer.getInteger("efn.treeAllowance", 20);

    public static final int TALL_TREE_ALLOWANCE = Integer.getInteger("efn.tallTreeAllowance", 34);

    static final int LAND_FLOOR = Integer.getInteger("efn.landFloor", 57);

    static final int UNSEEN_BELOW = 8;

    static final int UNSEEN_ABOVE = 40;

    private final short[] low = new short[256];

    private final short[] high = new short[256];

    private GateHeights() {}

    public static GateHeights of(TerrainChunk terrainChunk, ShownChunk shownChunk, int i, int j) {
        GateHeights gateHeights = new GateHeights();
        for (int k = 0; k < 256; k++) {
            int m = k & 15;
            int n = k >> 4;
            int oceanFloor = terrainChunk.oceanFloor(m, n);
            int o;
            int p;
            if (shownChunk == null) {
                p = oceanFloor - 8;
                o = oceanFloor + 40;
            } else {
                int motionTop = shownChunk.motionTop(m, n);
                p = Math.min(oceanFloor, motionTop);
                o = Math.max(oceanFloor, motionTop);
                if (oceanFloor >= LAND_FLOOR) {
                    o = Math.max(o, oceanFloor + j);
                }
            }
            gateHeights.low[k] = (short) Math.max(p - i, -32768);
            gateHeights.high[k] = (short) Math.min(o + i, 32767);
        }
        return gateHeights;
    }

    public int low(int i, int j) {
        return this.low[(i & 15) + 16 * (j & 15)];
    }

    public int high(int i, int j) {
        return this.high[(i & 15) + 16 * (j & 15)];
    }
}
