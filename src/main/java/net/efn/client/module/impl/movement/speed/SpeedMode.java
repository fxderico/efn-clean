package net.efn.client.module.impl.movement.speed;

import net.efn.client.module.impl.movement.SpeedModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public abstract class SpeedMode {

    private static final double BASE_SPEED = 0.2873;

    protected final SpeedModule settings;

    protected int stage;

    protected double distance;

    protected double speed;

    protected SpeedMode(SpeedModule settings2) {
        this.settings = settings2;
        this.reset();
    }

    public void onTick(ClientPlayerEntity player) {}

    public Vec3d onMove(ClientPlayerEntity player, Vec3d vec) {
        return vec;
    }

    public void onRubberband() {
        this.reset();
    }

    public void onActivate() {}

    public void onDeactivate() {}

    protected double getDefaultSpeed(ClientPlayerEntity player) {
        double d = 0.2873;
        StatusEffectInstance statusEffect = player.getStatusEffect(StatusEffects.SPEED);
        if (statusEffect != null) {
            d *= 1.0 + 0.2 * (statusEffect.getAmplifier() + 1);
        }
        StatusEffectInstance statusEffect2;
        if ((statusEffect2 = player.getStatusEffect(StatusEffects.SLOWNESS)) != null) {
            d /= 1.0 + 0.2 * (statusEffect2.getAmplifier() + 1);
        }
        return d;
    }

    protected void reset() {
        this.stage = 0;
        this.distance = 0.0;
        this.speed = 0.2873;
    }

    protected double getHop(ClientPlayerEntity player, double d) {
        StatusEffectInstance statusEffect = player.getStatusEffect(StatusEffects.JUMP_BOOST);
        if (statusEffect != null) {
            d += (statusEffect.getAmplifier() + 1) * 0.1F;
        }
        return d;
    }
}
