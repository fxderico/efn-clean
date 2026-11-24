package net.efn.client.easyplace.util;

import net.efn.client.easyplace.EasyPlace;
import net.efn.client.easyplace.placement.PlacementState;
import net.efn.client.easyplace.placement.PlacementTarget;
import net.efn.client.easyplace.sync.ServerAckManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class EasyPlaceDebug {

    private EasyPlaceDebug() {}

    public static void log(
            String string,
            PlacementState placementState,
            PlacementTarget placementTarget,
            float[] floats,
            ServerAckManager.Result result,
            int i) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        float f = player != null ? player.getYaw() : 0.0F / 0.0F;
        float g = player != null ? player.getPitch() : 0.0F / 0.0F;
        EasyPlace.LOG.info(
                "EasyPlace Debug | event={} | Face={} | PlaceSide={} | Yaw={} | Pitch={} |"
                        + " PlayerYaw={} | PlayerPitch={} | State={} | ACK={} | Retry={}",
                string,
                placementTarget == null ? "-" : placementTarget.clickedFace(),
                placementTarget == null ? "-" : placementTarget.placeSide(),
                floats == null ? "-" : String.format(Locale.ROOT, "%.2f", floats[0]),
                floats == null ? "-" : String.format(Locale.ROOT, "%.2f", floats[1]),
                String.format(Locale.ROOT, "%.2f", f),
                String.format(Locale.ROOT, "%.2f", g),
                placementState,
                result,
                i);
    }
}
