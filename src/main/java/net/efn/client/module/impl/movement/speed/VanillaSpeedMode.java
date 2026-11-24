package net.efn.client.module.impl.movement.speed;

import net.efn.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class VanillaSpeedMode extends SpeedMode {

    private static final double DIAGONAL = 1.0 / Math.sqrt(2.0);

    public VanillaSpeedMode(SpeedModule module) {
        super(module);
    }

    @Override
    public Vec3d onMove(ClientPlayerEntity player, Vec3d vec) {
        Vec3d vec2 = horizontalVelocity(player, this.settings.vanillaSpeed());
        double d = vec2.x;
        double e = vec2.z;
        StatusEffectInstance statusEffect = player.getStatusEffect(StatusEffects.SPEED);
        if (statusEffect != null) {
            double d2 = (statusEffect.getAmplifier() + 1) * 0.205;
            d += d * d2;
            e += e * d2;
        }
        return new Vec3d(d, vec.y, e);
    }

    private static Vec3d horizontalVelocity(ClientPlayerEntity player, double d) {
        float yaw = player.getYaw();
        Vec3d vec = Vec3d.fromPolar(0.0F, yaw);
        Vec3d vec2 = Vec3d.fromPolar(0.0F, yaw + 90.0F);
        PlayerInput playerInput = player.input.playerInput;
        double e = 0.0;
        double d2 = 0.0;
        boolean bl = false;
        if (playerInput.forward()) {
            e += vec.x / 20.0 * d;
            d2 += vec.z / 20.0 * d;
            bl = true;
        }
        if (playerInput.backward()) {
            e -= vec.x / 20.0 * d;
            d2 -= vec.z / 20.0 * d;
            bl = true;
        }
        boolean bl2 = false;
        if (playerInput.right()) {
            e += vec2.x / 20.0 * d;
            d2 += vec2.z / 20.0 * d;
            bl2 = true;
        }
        if (playerInput.left()) {
            e -= vec2.x / 20.0 * d;
            d2 -= vec2.z / 20.0 * d;
            bl2 = true;
        }
        if (bl && bl2) {
            e *= DIAGONAL;
            d2 *= DIAGONAL;
        }
        return new Vec3d(e, 0.0, d2);
    }
}
