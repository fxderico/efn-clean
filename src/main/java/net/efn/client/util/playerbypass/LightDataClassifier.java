package net.efn.client.util.playerbypass;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.s2c.play.LightData;

import java.util.BitSet;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class LightDataClassifier {

    private static final int NIBBLES_PER_SECTION = 2048;

    private LightDataClassifier() {}

    public static LightSectionStatus classifyPayload(byte[] bytes) {
        if (bytes != null && bytes.length == 2048) {
            boolean bl = true;
            boolean bl2 = true;
            for (byte b : bytes) {
                if (b != 0) {
                    bl = false;
                }
                if (b != -1) {
                    bl2 = false;
                }
                if (!bl && !bl2) {
                    return LightSectionStatus.NORMAL;
                }
            }
            if (bl) {
                return LightSectionStatus.ZEROED_PRESENT;
            } else {
                return bl2 ? LightSectionStatus.UNIFORM_MAX : LightSectionStatus.NORMAL;
            }
        } else {
            return LightSectionStatus.UNKNOWN;
        }
    }

    public static void apply(LightData lightData, ChunkLightSnapshot chunkLightSnapshot) {
        applyTrack(
                lightData.getInitedSky(),
                lightData.getUninitedSky(),
                lightData.getSkyNibbles(),
                chunkLightSnapshot.sky);
    }

    static int sentNotDark(LightData lightData, int i, int j) {
        BitSet initedSky = lightData.getInitedSky();
        BitSet uninitedSky = lightData.getUninitedSky();
        List skyNibbles = lightData.getSkyNibbles();
        int k = 0;
        int m = 0;
        for (int n = 0; n < i + 2; n++) {
            boolean bl = initedSky.get(n);
            byte[] bytes = bl && m < skyNibbles.size() ? (byte[]) skyNibbles.get(m) : null;
            if (bl) {
                m++;
            }
            if (n >= 1 && n <= i && n < 32 && ChunkLightSnapshot.candidate(n, j)) {
                if (bl) {
                    LightSectionStatus lightSectionStatus = classifyPayload(bytes);
                    if (lightSectionStatus == LightSectionStatus.NORMAL
                            || lightSectionStatus == LightSectionStatus.UNIFORM_MAX) {
                        k |= 1 << n;
                    }
                } else if (uninitedSky.get(n)) {
                    k |= 1 << n;
                }
            }
        }
        return k;
    }

    static void applyTrack(
            BitSet bitSet,
            BitSet bitSet2,
            List<byte[]> list,
            LightSectionStatus[] lightSectionStatuses) {
        int i = 0;
        for (int j = 0; j < lightSectionStatuses.length; j++) {
            if (bitSet.get(j)) {
                byte[] bytes = i < list.size() ? (byte[]) list.get(i) : null;
                i++;
                lightSectionStatuses[j] = classifyPayload(bytes);
            } else if (bitSet2.get(j)) {
                lightSectionStatuses[j] = LightSectionStatus.EMPTY_MASK;
            }
        }
    }
}
