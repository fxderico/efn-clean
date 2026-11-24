package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

@Environment(EnvType.CLIENT)
public final class BlockBoxHelper {

    private static final double HALF_BLOCK = 0.5;

    private static final float MIN_SIZE = 0.1F;

    private static final float MAX_SIZE = 2.0F;

    private BlockBoxHelper() {}

    public static Box scaledBlockBox(BlockPos pos, float f) {
        float g = Math.max(0.1F, Math.min(2.0F, f));
        double d = g * 0.5;
        double e = pos.getX() + 0.5;
        double d2 = pos.getY() + 0.5;
        double e2 = pos.getZ() + 0.5;
        return new Box(e - d, d2 - d, e2 - d, e + d, d2 + d, e2 + d);
    }
}
