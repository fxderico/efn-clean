package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Motion {

    private static final float DEG = 0.017453292F;

    private static final float TAU = 6.2831855F;

    private static final float STRIDE = 0.6662F;

    float restPitch;

    float restYaw;

    float restRoll;

    float idlePitch;

    float idleYaw;

    float idleRoll;

    float idleRate = 0.4F;

    float idlePhase;

    float walkPitch;

    float walkYaw;

    float walkRoll;

    float walkRate = 1.0F;

    float walkPhase;

    float drag;

    float hang;

    float turnRoll;

    float turnYaw;

    Bone follow;

    float followAmount;

    float followLimit;

    float stiffness;

    float damping;

    float bend;

    float limit = 80.0F;

    public Motion() {}

    public static Motion none() {
        return new Motion();
    }

    public static Motion hair() {
        return new Motion()
                .idle(2.5F, 0.0F, 2.0F, 0.35F)
                .walk(4.0F, 0.0F, 3.0F, 2.0F)
                .drag(16.0F)
                .hang(0.75F)
                .turn(12.0F, 0.0F)
                .spring(55.0F, 7.0F)
                .bend(1.3F);
    }

    public static Motion tail() {
        return new Motion()
                .idle(2.0F, 10.0F, 0.0F, 0.45F)
                .walk(3.0F, 9.0F, 0.0F, 1.0F)
                .drag(8.0F)
                .hang(0.25F)
                .turn(0.0F, 18.0F)
                .spring(35.0F, 5.0F)
                .bend(1.2F);
    }

    public static Motion skirt(Bone bone) {
        return new Motion().follow(bone, 0.45F).drag(4.0F).spring(90.0F, 12.0F).bend(1.0F);
    }

    public static Motion cloth() {
        return new Motion()
                .idle(1.5F, 0.0F, 1.5F, 0.3F)
                .walk(5.0F, 0.0F, 2.0F, 2.0F)
                .drag(10.0F)
                .hang(0.6F)
                .turn(8.0F, 0.0F)
                .spring(45.0F, 6.0F)
                .bend(1.4F);
    }

    public Motion rest(float restPitch2, float restYaw2, float restRoll2) {
        this.restPitch = restPitch2;
        this.restYaw = restYaw2;
        this.restRoll = restRoll2;
        return this;
    }

    public Motion idle(float idlePitch2, float idleYaw2, float idleRoll2, float idleRate2) {
        this.idlePitch = idlePitch2;
        this.idleYaw = idleYaw2;
        this.idleRoll = idleRoll2;
        this.idleRate = idleRate2;
        return this;
    }

    public Motion idlePhase(float f) {
        this.idlePhase = f * 0.017453292F;
        return this;
    }

    public Motion walk(float walkPitch2, float walkYaw2, float walkRoll2, float walkRate2) {
        this.walkPitch = walkPitch2;
        this.walkYaw = walkYaw2;
        this.walkRoll = walkRoll2;
        this.walkRate = walkRate2;
        return this;
    }

    public Motion walkPhase(float f) {
        this.walkPhase = f * 0.017453292F;
        return this;
    }

    public Motion drag(float drag2) {
        this.drag = drag2;
        return this;
    }

    public Motion hang(float hang2) {
        this.hang = hang2;
        return this;
    }

    public Motion turn(float turnRoll2, float turnYaw2) {
        this.turnRoll = turnRoll2;
        this.turnYaw = turnYaw2;
        return this;
    }

    public Motion follow(Bone bone, float f) {
        return this.follow(bone, f, 0.0F);
    }

    public Motion follow(Bone follow2, float followAmount2, float f) {
        this.follow = follow2;
        this.followAmount = followAmount2;
        this.followLimit = Math.max(0.0F, f);
        return this;
    }

    public Motion spring(float f, float g) {
        this.stiffness = Math.max(0.0F, f);
        this.damping = Math.max(0.0F, g);
        return this;
    }

    public Motion bend(float f) {
        this.bend = Math.max(0.0F, f);
        return this;
    }

    public Motion limit(float f) {
        this.limit = Math.abs(f);
        return this;
    }

    public Motion mirrored() {
        Motion motion = this.copy();
        motion.restYaw = -this.restYaw;
        motion.restRoll = -this.restRoll;
        motion.follow = this.follow == null ? null : this.follow.opposite();
        return motion;
    }

    public Motion copy() {
        Motion motion = new Motion();
        motion.restPitch = this.restPitch;
        motion.restYaw = this.restYaw;
        motion.restRoll = this.restRoll;
        motion.idlePitch = this.idlePitch;
        motion.idleYaw = this.idleYaw;
        motion.idleRoll = this.idleRoll;
        motion.idleRate = this.idleRate;
        motion.idlePhase = this.idlePhase;
        motion.walkPitch = this.walkPitch;
        motion.walkYaw = this.walkYaw;
        motion.walkRoll = this.walkRoll;
        motion.walkRate = this.walkRate;
        motion.walkPhase = this.walkPhase;
        motion.drag = this.drag;
        motion.hang = this.hang;
        motion.turnRoll = this.turnRoll;
        motion.turnYaw = this.turnYaw;
        motion.follow = this.follow;
        motion.followAmount = this.followAmount;
        motion.followLimit = this.followLimit;
        motion.stiffness = this.stiffness;
        motion.damping = this.damping;
        motion.bend = this.bend;
        motion.limit = this.limit;
        return motion;
    }

    public float bendPower() {
        return this.bend;
    }

    public void target(Motion.Input input, Bone bone, float[] floats) {
        float f = Math.min(1.0F, Math.max(0.0F, input.walkSpeed));
        float g = (float) Math.sin(input.time * this.idleRate * 6.2831855F + this.idlePhase);
        float h = (float) Math.sin(input.walkPos * 0.6662F * this.walkRate + this.walkPhase) * f;
        float f2 = Math.max(-1.5F, Math.min(1.5F, input.turnRate / 180.0F));
        int ordinal = bone.ordinal();
        float g2 =
                (this.restPitch + this.idlePitch * g + this.walkPitch * h + this.drag * f)
                                * 0.017453292F
                        - this.hang * input.pitch[ordinal];
        float h2 =
                (this.restYaw + this.idleYaw * g + this.walkYaw * h + this.turnYaw * f2)
                        * 0.017453292F;
        float f3 =
                (this.restRoll + this.idleRoll * g + this.walkRoll * h + this.turnRoll * f2)
                        * 0.017453292F;
        if (this.follow != null) {
            int ordinal2 = this.follow.ordinal();
            g2 +=
                    soft(
                            this.followAmount * (input.pitch[ordinal2] - input.pitch[ordinal]),
                            this.followLimit);
            h2 +=
                    soft(
                            this.followAmount * (input.yaw[ordinal2] - input.yaw[ordinal]),
                            this.followLimit);
            f3 +=
                    soft(
                            this.followAmount * (input.roll[ordinal2] - input.roll[ordinal]),
                            this.followLimit);
        }
        float g3 = this.limit * 0.017453292F;
        floats[0] = Math.max(-g3, Math.min(g3, g2));
        floats[1] = Math.max(-g3, Math.min(g3, h2));
        floats[2] = Math.max(-g3, Math.min(g3, f3));
    }

    static float soft(float f, float g) {
        if (g <= 0.0F) {
            return f;
        } else {
            float h = g * 0.017453292F;
            return h * (float) Math.tanh(f / h);
        }
    }

    public void step(float[] floats, int i, float[] floats2, float f) {
        if (!(this.stiffness <= 0.0F) && !(f <= 0.0F) && !(f > 0.25F) && !Float.isNaN(floats[i])) {
            int j = Math.max(1, (int) Math.ceil(f * 120.0F));
            float g = f / j;
            for (int k = 0; k < j; k++) {
                for (int m = 0; m < 3; m++) {
                    float h = floats[i + m];
                    float f2 = floats[i + 3 + m];
                    f2 += (this.stiffness * (floats2[m] - h) - this.damping * f2) * g;
                    floats[i + 3 + m] = f2;
                    floats[i + m] = h + f2 * g;
                }
            }
        } else {
            for (int n = 0; n < 3; n++) {
                floats[i + n] = floats2[n];
                floats[i + 3 + n] = 0.0F;
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static final class Input {

        public float time;

        public float walkPos;

        public float walkSpeed;

        public float turnRate;

        public final float[] pitch = new float[6];

        public final float[] yaw = new float[6];

        public final float[] roll = new float[6];

        public Input() {}
    }
}
