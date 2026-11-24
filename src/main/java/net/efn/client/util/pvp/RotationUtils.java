package net.efn.client.util.pvp;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class RotationUtils {

    private RotationUtils() {}

    public static Rotation fromTo(Vec3d vec, Vec3d vec2) {
        double d = vec2.x - vec.x;
        double e = vec2.y - vec.y;
        double d2 = vec2.z - vec.z;
        double e2 = Math.sqrt(d * d + d2 * d2);
        float f = (float) (MathHelper.atan2(d2, d) * 57.2957763671875) - 90.0F;
        float g = (float) (-(MathHelper.atan2(e, e2) * 57.2957763671875));
        return new Rotation(MathHelper.wrapDegrees(f), MathHelper.clamp(g, -90.0F, 90.0F));
    }

    public static Rotation entityRotation(Entity entity) {
        return new Rotation(entity.getYaw(), entity.getPitch());
    }

    public static float yawDelta(float f, float g) {
        return MathHelper.wrapDegrees(g - f);
    }

    public static float pitchDelta(float f, float g) {
        return g - f;
    }

    public static double angleTo(Rotation rotation, Rotation rotation2) {
        float yawDelta = yawDelta(rotation.yaw(), rotation2.yaw());
        float pitchDelta = pitchDelta(rotation.pitch(), rotation2.pitch());
        return Math.hypot(yawDelta, pitchDelta);
    }

    public static double mouseScale(MinecraftClient client, ClientPlayerEntity player) {
        double d =
                client.options.getMouseSensitivity().getValue() * 0.6000000238418579
                        + 0.20000000298023224;
        return d
                * d
                * d
                * (client.options.getPerspective().isFirstPerson() && player.isUsingSpyglass()
                        ? 1.0
                        : 8.0);
    }
}
