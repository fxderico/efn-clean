package net.efn.client.module.impl.render.character;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.efn.client.module.impl.render.character.kit.Bone;
import net.efn.client.module.impl.render.character.kit.CharacterModel;
import net.efn.client.module.impl.render.character.kit.Motion;
import net.efn.client.module.impl.render.character.kit.PartMesh;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
final class CharacterMotion {

    private static final long FORGET_NANOS = 20000000000L;

    private static final Int2ObjectOpenHashMap<CharacterMotion.State> STATES =
            new Int2ObjectOpenHashMap<>();

    private static final Motion.Input INPUT = new Motion.Input();

    private static final float[] TARGET = new float[3];

    private static long prunedAt;

    private CharacterMotion() {}

    static float[] update(
            PlayerEntityRenderState playerEntityRenderState,
            CharacterModel characterModel,
            PlayerEntityModel playerEntityModel,
            float[] floats) {
        if (characterModel.motionSlots == 0) {
            return null;
        } else {
            long l = System.nanoTime();
            prune(l);
            CharacterMotion.State state = STATES.get(playerEntityRenderState.id);
            if (state == null || state.model != characterModel) {
                state = new CharacterMotion.State(characterModel);
                state.bodyRot = playerEntityRenderState.bodyYaw;
                STATES.put(playerEntityRenderState.id, state);
            }
            float f = state.nanos == 0L ? 0.0F : (float) (l - state.nanos) / 1.0E9F;
            if (state.nanos != 0L && f < 5.0E-4F) {
                return state.angles;
            } else {
                state.nanos = l;
                float g = MathHelper.wrapDegrees(playerEntityRenderState.bodyYaw - state.bodyRot);
                state.bodyRot = playerEntityRenderState.bodyYaw;
                state.turnRate =
                        f > 0.0F && f < 0.25F
                                ? (state.turnRate =
                                        state.turnRate
                                                + (g / f - state.turnRate)
                                                        * Math.min(1.0F, f * 12.0F))
                                : 0.0F;
                Motion.Input input = INPUT;
                input.time = playerEntityRenderState.age / 20.0F;
                input.walkPos = playerEntityRenderState.limbSwingAnimationProgress;
                input.walkSpeed = playerEntityRenderState.limbSwingAmplitude;
                input.turnRate = state.turnRate;
                bone(input, Bone.HEAD, playerEntityModel.head, floats);
                bone(input, Bone.BODY, playerEntityModel.body, floats);
                bone(input, Bone.RIGHT_ARM, playerEntityModel.rightArm, floats);
                bone(input, Bone.LEFT_ARM, playerEntityModel.leftArm, floats);
                bone(input, Bone.RIGHT_LEG, playerEntityModel.rightLeg, floats);
                bone(input, Bone.LEFT_LEG, playerEntityModel.leftLeg, floats);
                for (PartMesh part : characterModel.parts) {
                    if (part.motionSlot >= 0 && part.motion != null) {
                        part.motion.target(input, part.bone, TARGET);
                        part.motion.step(state.spring, part.motionSlot * 6, TARGET, f);
                        System.arraycopy(
                                state.spring,
                                part.motionSlot * 6,
                                state.angles,
                                part.motionSlot * 3,
                                3);
                    }
                }
                return state.angles;
            }
        }
    }

    private static void bone(Motion.Input input, Bone bone, ModelPart modelPart, float[] floats) {
        int ordinal = bone.ordinal();
        input.pitch[ordinal] = floats[ordinal];
        input.yaw[ordinal] = -modelPart.yaw;
        input.roll[ordinal] = -modelPart.roll;
    }

    private static void prune(long l) {
        if (l - prunedAt >= 5000000000L) {
            prunedAt = l;
            STATES.values().removeIf(state -> l - state.nanos > 20000000000L);
        }
    }

    static void clear() {
        STATES.clear();
    }

    @Environment(EnvType.CLIENT)
    private static final class State {

        final CharacterModel model;

        final float[] spring;

        final float[] angles;

        float bodyRot;

        float turnRate;

        long nanos;

        State(CharacterModel model2) {
            this.model = model2;
            this.spring = new float[model2.motionSlots * 6];
            this.angles = new float[model2.motionSlots * 3];
            for (int i = 0; i < model2.motionSlots; i++) {
                this.spring[i * 6] = 0.0F / 0.0F;
            }
        }
    }
}
