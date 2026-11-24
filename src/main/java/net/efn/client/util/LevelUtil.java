package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;

@Environment(EnvType.CLIENT)
public final class LevelUtil {

    private LevelUtil() {}

    public static boolean isOverworld(ClientWorld world) {
        return world.getRegistryKey() == World.OVERWORLD
                || world.getDimensionEntry().matchesKey(DimensionTypes.OVERWORLD);
    }

    public static boolean isLoaded(ClientWorld world, int i, int j) {
        return world.getChunkManager().isChunkLoaded(i, j);
    }
}
