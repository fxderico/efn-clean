package net.efn.client.module.impl.render.character;

import net.efn.client.module.impl.render.character.kit.Bone;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Arm;
import net.minecraft.util.SwingAnimationType;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class CharacterJoints {

    static final float CROUCH_HIP = 0.5F;

    static final float SADDLE_KNEE = 1.35F;

    static final float SEAT_KNEE = 0.3F;

    private static final float CROUCH_KNEE = 0.62F;

    private static final float CROUCH_ELBOW = 0.2F;

    private static final float MAX_KNEE = 1.7F;

    private static final float MAX_ELBOW = 1.6F;

    private CharacterJoints() {}

    static float angle(PlayerEntityRenderState playerEntityRenderState, Bone bone, float f) {
        float g =
                !playerEntityRenderState.isGliding && !playerEntityRenderState.usingRiptide
                        ? 1.0F - playerEntityRenderState.leaningPitch * 0.8F
                        : 0.0F;
        if (g <= 0.0F) {
            return 0.0F;
        } else if (bone.leg()) {
            return knee(playerEntityRenderState, bone) * g;
        } else {
            return bone.arm() ? -elbow(playerEntityRenderState, bone, f) * g : 0.0F;
        }
    }

    private static float knee(PlayerEntityRenderState playerEntityRenderState, Bone bone) {
        if (playerEntityRenderState.hasVehicle) {
            return CharacterModule.seat(playerEntityRenderState);
        } else {
            float f =
                    Math.min(
                            1.0F,
                            playerEntityRenderState.limbSwingAmplitude
                                    / playerEntityRenderState.limbAmplitudeInverse);
            float g =
                    playerEntityRenderState.limbSwingAnimationProgress * 0.6662F
                            + (bone == Bone.LEFT_LEG ? 3.1415927F : 0.0F);
            float h = f * (0.1F + 0.85F * Math.max(0.0F, MathHelper.sin(g)));
            if (playerEntityRenderState.isInSneakingPose) {
                h += 0.62F;
            }
            return Math.min(1.7F, h);
        }
    }

    private static float elbow(
            PlayerEntityRenderState playerEntityRenderState, Bone bone, float f) {
        boolean bl = bone == Bone.RIGHT_ARM;
        ArmPose armPose =
                bl ? playerEntityRenderState.rightArmPose : playerEntityRenderState.leftArmPose;
        ArmPose armPose2 =
                bl ? playerEntityRenderState.leftArmPose : playerEntityRenderState.rightArmPose;
        if (armPose2.isTwoHanded()) {
            return 0.0F;
        } else {
            float g =
                    Math.min(
                            1.0F,
                            playerEntityRenderState.limbSwingAmplitude
                                    / playerEntityRenderState.limbAmplitudeInverse);
            float h =
                    switch (armPose) {
                        case EMPTY -> 0.16F + 0.45F * Math.max(0.0F, -f) + 0.3F * g;
                        case ITEM -> 0.3F + 0.2F * g;
                        case BLOCK -> 0.55F;
                        case BRUSH -> 0.4F;
                        default -> 0.0F;
                    };
            if (h <= 0.0F) {
                return 0.0F;
            } else {
                if (playerEntityRenderState.isInSneakingPose) {
                    h += 0.2F;
                }
                Arm arm = bl ? Arm.RIGHT : Arm.LEFT;
                if (playerEntityRenderState.handSwingProgress > 0.0F
                        && playerEntityRenderState.preferredArm == arm
                        && playerEntityRenderState.swingAnimationType == SwingAnimationType.WHACK) {
                    h +=
                            0.6F
                                    * MathHelper.sin(
                                            playerEntityRenderState.handSwingProgress * 3.1415927F);
                }
                return Math.min(1.6F, h);
            }
        }
    }
}
