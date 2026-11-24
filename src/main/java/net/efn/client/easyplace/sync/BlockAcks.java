package net.efn.client.easyplace.sync;

import net.efn.client.easyplace.mixin.AccessorClientLevel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public final class BlockAcks {

    private static ClientWorld level;

    private static int lastAcked = -1;

    private BlockAcks() {}

    public static void acked(int i) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world != level) {
            level = world;
            lastAcked = -1;
        }
        lastAcked = Math.max(lastAcked, i);
    }

    public static int lastSent(ClientWorld world) {
        return ((AccessorClientLevel) world).easyplace$predictions().getSequence();
    }

    public static boolean answered(ClientWorld world, int i) {
        return world == level && lastAcked >= i;
    }
}
