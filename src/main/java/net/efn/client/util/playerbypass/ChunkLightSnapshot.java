package net.efn.client.util.playerbypass;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public final class ChunkLightSnapshot {

    public static final int BASE_DETECT_CEILING_Y = 62;

    public final int sectionCount;

    public final int bottomSectionY;

    public final LightSectionStatus[] sky;

    public ChunkLightSnapshot(int sectionCount2, int bottomSectionY2) {
        this.sectionCount = sectionCount2;
        this.bottomSectionY = bottomSectionY2;
        this.sky = new LightSectionStatus[sectionCount2 + 2];
        Arrays.fill(this.sky, LightSectionStatus.UNKNOWN);
    }

    public boolean hasBaseSignal() {
        for (int i = 1; i < this.sectionCount + 1; i++) {
            if (candidate(i, this.bottomSectionY)
                    && this.sky[i] == LightSectionStatus.ZEROED_PRESENT) {
                return true;
            }
        }
        return false;
    }

    static boolean candidate(int i, int j) {
        return (j + i - 1) * 16 + 15 < 62;
    }
}
