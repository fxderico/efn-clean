package net.efn.client.module.impl.render.character;

import net.efn.client.module.impl.render.character.kit.Bone;
import net.efn.client.module.impl.render.character.kit.CharacterModel;
import net.efn.client.module.impl.render.character.kit.Lights;
import net.efn.client.module.impl.render.character.kit.PartMesh;
import net.efn.client.module.impl.render.character.kit.Surface;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
final class CharacterRenderer {

    private static final int GHOST_ALPHA = 38;

    private static final Bone[] BONES = Bone.values();

    private static final Quaternionf ROTATION = new Quaternionf();

    private static final CharacterRotationTable LEVELS = new CharacterRotationTable();

    private static final float[] PITCH = new float[BONES.length];

    private static final float[] JOINT = new float[BONES.length];

    private static final CharacterRotationTable JOINT_LEVELS = new CharacterRotationTable();

    private static float[] scratch = new float[24576];

    private CharacterRenderer() {}

    static void submit(
            WornCharacter wornCharacter,
            PlayerEntityModel playerEntityModel,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            PlayerEntityRenderState playerEntityRenderState) {
        CharacterModel model = wornCharacter.model;
        PartMesh[] parts = model.parts;
        boolean invisible = playerEntityRenderState.invisible;
        int overlay = LivingEntityRenderer.getOverlay(playerEntityRenderState, 0.0F);
        float[] floats = pitches(playerEntityRenderState, model, playerEntityModel);
        float[] floats2 = joints(playerEntityRenderState, model);
        float pitch = playerEntityModel.head.pitch;
        float f = -playerEntityModel.head.yaw;
        float[] floats3 =
                CharacterMotion.update(playerEntityRenderState, model, playerEntityModel, floats);
        matrices.push();
        playerEntityModel.getRootPart().applyTransform(matrices);
        lift(matrices, playerEntityRenderState, model);
        for (int k = 0; k < parts.length; k++) {
            PartMesh part = parts[k];
            if (part.base != null || part.outline != null || part.decals != null) {
                boolean bl = part.bends() && floats3 != null;
                float g = bl ? floats3[part.motionSlot * 3] : 0.0F;
                float h = bl ? floats3[part.motionSlot * 3 + 1] : 0.0F;
                float f2 = bl ? floats3[part.motionSlot * 3 + 2] : 0.0F;
                float g2 = 0.0F;
                float h2 = 0.0F;
                if (part.neck) {
                    g2 = look(part.lookPitch * pitch, part.lookLimit);
                    h2 = look(part.lookYaw * f, part.lookLimit);
                } else if (part.jointed) {
                    g2 = floats2[part.bone.ordinal()];
                }
                matrices.push();
                chain(matrices, playerEntityModel, model, k, floats3, !bl, null, floats);
                submitPart(
                        wornCharacter,
                        part,
                        matrices,
                        orderedRenderCommandQueue,
                        i,
                        overlay,
                        invisible,
                        bl,
                        g,
                        h,
                        f2,
                        g2,
                        h2);
                matrices.pop();
            }
        }
        matrices.pop();
    }

    static boolean firstPersonHand(
            PlayerEntityModel playerEntityModel,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            ModelPart modelPart) {
        WornCharacter wornCharacter = CharacterModule.firstPerson();
        if (wornCharacter == null) {
            return false;
        } else {
            CharacterModel model = wornCharacter.model;
            Bone bone = modelPart == playerEntityModel.rightArm ? Bone.RIGHT_ARM : Bone.LEFT_ARM;
            PartMesh partMesh = model.part(bone);
            if (partMesh != null && partMesh.base != null) {
                modelPart.resetTransform();
                modelPart.roll = bone == Bone.RIGHT_ARM ? 0.1F : -0.1F;
                PartMesh[] parts = model.parts;
                for (int j = 0; j < parts.length; j++) {
                    PartMesh part = parts[j];
                    if (part.bone == bone
                            && (part.base != null || part.outline != null || part.decals != null)) {
                        matrices.push();
                        chain(matrices, playerEntityModel, model, j, null, true, modelPart, null);
                        submitPart(
                                wornCharacter,
                                part,
                                matrices,
                                orderedRenderCommandQueue,
                                i,
                                OverlayTexture.DEFAULT_UV,
                                false,
                                false,
                                0.0F,
                                0.0F,
                                0.0F,
                                0.0F,
                                0.0F);
                        matrices.pop();
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }

    private static void submitPart(
            WornCharacter wornCharacter,
            PartMesh partMesh,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            int j,
            boolean bl,
            boolean bl2,
            float f,
            float g,
            float h,
            float f2,
            float g2) {
        if (bl) {
            if (partMesh.base != null) {
                orderedRenderCommandQueue
                        .getBatchingQueue(1)
                        .submitCustom(
                                matrices,
                                wornCharacter.ghost,
                                (entry, vertexConsumer) ->
                                        emit(
                                                entry,
                                                vertexConsumer,
                                                partMesh.base,
                                                i,
                                                j,
                                                38,
                                                bl2,
                                                f,
                                                g,
                                                h,
                                                partMesh,
                                                f2,
                                                g2));
            }
        } else {
            if (partMesh.base != null || partMesh.outline != null) {
                orderedRenderCommandQueue.submitCustom(
                        matrices,
                        wornCharacter.solid,
                        (entry, vertexConsumer) -> {
                            if (partMesh.base != null) {
                                emit(
                                        entry,
                                        vertexConsumer,
                                        partMesh.base,
                                        i,
                                        j,
                                        255,
                                        bl2,
                                        f,
                                        g,
                                        h,
                                        partMesh,
                                        f2,
                                        g2);
                            }
                            if (partMesh.outline != null) {
                                emit(
                                        entry,
                                        vertexConsumer,
                                        partMesh.outline,
                                        i,
                                        j,
                                        255,
                                        bl2,
                                        f,
                                        g,
                                        h,
                                        partMesh,
                                        f2,
                                        g2);
                            }
                        });
            }
            if (partMesh.decals != null) {
                orderedRenderCommandQueue
                        .getBatchingQueue(1)
                        .submitCustom(
                                matrices,
                                wornCharacter.decal,
                                (entry, vertexConsumer) ->
                                        emit(
                                                entry,
                                                vertexConsumer,
                                                partMesh.decals,
                                                i,
                                                j,
                                                255,
                                                bl2,
                                                f,
                                                g,
                                                h,
                                                partMesh,
                                                f2,
                                                g2));
            }
        }
    }

    static boolean translateToHand(
            PlayerEntityModel playerEntityModel,
            PlayerEntityRenderState playerEntityRenderState,
            Arm arm,
            MatrixStack matrices) {
        WornCharacter wornCharacter = CharacterModule.worn(playerEntityRenderState);
        if (wornCharacter == null) {
            return false;
        } else {
            CharacterModel model = wornCharacter.model;
            Bone bone = arm == Arm.RIGHT ? Bone.RIGHT_ARM : Bone.LEFT_ARM;
            PartMesh partMesh = model.part(bone);
            if (partMesh == null) {
                return false;
            } else {
                float scale = model.scale;
                ModelPart modelPart = vanilla(playerEntityModel, bone);
                playerEntityModel.getRootPart().applyTransform(matrices);
                lift(matrices, playerEntityRenderState, model);
                pivot(
                        matrices,
                        modelPart,
                        partMesh,
                        scale,
                        pitch(playerEntityRenderState, partMesh, modelPart.pitch));
                float f =
                        partMesh.jointed
                                ? CharacterJoints.angle(
                                        playerEntityRenderState,
                                        bone,
                                        walk(playerEntityRenderState, bone)
                                                * weight(playerEntityRenderState, bone))
                                : 0.0F;
                if (f != 0.0F) {
                    float g = partMesh.jointY * scale / 16.0F;
                    float h = partMesh.jointZ * scale / 16.0F;
                    matrices.translate(0.0F, -g, -h);
                    matrices.multiply(ROTATION.rotationX(f));
                    matrices.translate(0.0F, g, h);
                }
                matrices.translate(
                        (partMesh.gripX - partMesh.pivotX) * scale / 16.0F,
                        -(partMesh.gripY - partMesh.pivotY) * scale / 16.0F,
                        -(partMesh.gripZ - partMesh.pivotZ) * scale / 16.0F);
                matrices.scale(scale, scale, scale);
                matrices.translate((arm == Arm.RIGHT ? 1.0F : -1.0F) / 16.0F, -0.625F, 0.125F);
                return true;
            }
        }
    }

    private static float[] pitches(
            PlayerEntityRenderState playerEntityRenderState,
            CharacterModel characterModel,
            PlayerEntityModel playerEntityModel) {
        float[] floats = PITCH;
        for (Bone bone : BONES) {
            float pitch = vanilla(playerEntityModel, bone).pitch;
            PartMesh partMesh = !bone.arm() && !bone.leg() ? null : characterModel.part(bone);
            floats[bone.ordinal()] =
                    partMesh == null ? pitch : pitch(playerEntityRenderState, partMesh, pitch);
        }
        return floats;
    }

    private static float pitch(
            PlayerEntityRenderState playerEntityRenderState, PartMesh partMesh, float f) {
        float swingPitch =
                partMesh.swingPitch(
                        f,
                        walk(playerEntityRenderState, partMesh.bone),
                        weight(playerEntityRenderState, partMesh.bone));
        if (partMesh.jointed
                && partMesh.bone.leg()
                && playerEntityRenderState.isInSneakingPose
                && !playerEntityRenderState.hasVehicle) {
            swingPitch -= 0.5F * (1.0F - playerEntityRenderState.leaningPitch);
        }
        return swingPitch;
    }

    private static float[] joints(
            PlayerEntityRenderState playerEntityRenderState, CharacterModel characterModel) {
        float[] floats = JOINT;
        for (Bone bone : BONES) {
            PartMesh partMesh = !bone.arm() && !bone.leg() ? null : characterModel.part(bone);
            floats[bone.ordinal()] =
                    partMesh != null && partMesh.jointed
                            ? CharacterJoints.angle(
                                    playerEntityRenderState,
                                    bone,
                                    walk(playerEntityRenderState, bone)
                                            * weight(playerEntityRenderState, bone))
                            : 0.0F;
        }
        return floats;
    }

    private static float look(float f, float g) {
        float h = g * 0.017453292F;
        return MathHelper.clamp(f, -h, h);
    }

    private static float walk(PlayerEntityRenderState playerEntityRenderState, Bone bone) {
        float f = playerEntityRenderState.limbSwingAnimationProgress * 0.6662F;
        float g =
                playerEntityRenderState.limbSwingAmplitude
                        / playerEntityRenderState.limbAmplitudeInverse;
        return switch (bone) {
            case RIGHT_LEG -> MathHelper.cos(f) * 1.4F * g;
            case LEFT_LEG -> MathHelper.cos(f + 3.1415927F) * 1.4F * g;
            case RIGHT_ARM -> MathHelper.cos(f + 3.1415927F) * g;
            case LEFT_ARM -> MathHelper.cos(f) * g;
            default -> 0.0F;
        };
    }

    private static float weight(PlayerEntityRenderState playerEntityRenderState, Bone bone) {
        if (bone.leg()) {
            return playerEntityRenderState.hasVehicle
                    ? 0.0F
                    : 1.0F - playerEntityRenderState.leaningPitch;
        } else if (!bone.arm()) {
            return 0.0F;
        } else {
            ArmPose armPose =
                    bone == Bone.RIGHT_ARM
                            ? playerEntityRenderState.rightArmPose
                            : playerEntityRenderState.leftArmPose;
            ArmPose armPose2 =
                    bone == Bone.RIGHT_ARM
                            ? playerEntityRenderState.leftArmPose
                            : playerEntityRenderState.rightArmPose;
            if (armPose2.isTwoHanded()) {
                return 0.0F;
            } else {
                float f =
                        switch (armPose) {
                            case EMPTY -> 1.0F;
                            case ITEM, BLOCK, BRUSH, THROW_TRIDENT -> 0.5F;
                            default -> 0.0F;
                        };
                return playerEntityRenderState.isUsingItem
                        ? f
                        : f * (1.0F - playerEntityRenderState.leaningPitch);
            }
        }
    }

    private static void lift(
            MatrixStack matrices,
            PlayerEntityRenderState playerEntityRenderState,
            CharacterModel characterModel) {
        if (playerEntityRenderState.hasVehicle) {
            float f = 12.0F - characterModel.scale * characterModel.hipY;
            if (f != 0.0F) {
                matrices.translate(0.0F, -f / 16.0F, 0.0F);
            }
        }
    }

    private static void chain(
            MatrixStack matrices,
            PlayerEntityModel playerEntityModel,
            CharacterModel characterModel,
            int i,
            float[] floats,
            boolean bl,
            ModelPart modelPart,
            float[] floats2) {
        PartMesh part = characterModel.parts[i];
        float scale = characterModel.scale;
        if (!part.extra) {
            if (modelPart == null) {
                ModelPart modelPart2 = vanilla(playerEntityModel, part.bone);
                pivot(
                        matrices,
                        modelPart2,
                        part,
                        scale,
                        floats2 == null ? modelPart2.pitch : floats2[part.bone.ordinal()]);
            } else {
                matrices.translate(
                        modelPart.originX / 16.0F,
                        modelPart.originY / 16.0F,
                        modelPart.originZ / 16.0F);
                matrices.multiply(
                        ROTATION.rotationZYX(modelPart.roll, modelPart.yaw, modelPart.pitch));
                float f = part.bone == Bone.RIGHT_ARM ? -1.0F : 1.0F;
                matrices.translate(
                        (f - (part.gripX - part.pivotX) * scale) / 16.0F,
                        (10.0F + (part.gripY - part.pivotY) * scale) / 16.0F,
                        (-2.0F + (part.gripZ - part.pivotZ) * scale) / 16.0F);
            }
            float g = scale / 16.0F;
            matrices.scale(g, -g, -g);
        } else {
            chain(
                    matrices,
                    playerEntityModel,
                    characterModel,
                    part.parent,
                    floats,
                    true,
                    modelPart,
                    floats2);
            PartMesh part2 = characterModel.parts[part.parent];
            matrices.translate(
                    part.pivotX - part2.pivotX,
                    part.pivotY - part2.pivotY,
                    part.pivotZ - part2.pivotZ);
            if (bl && floats != null && part.motionSlot >= 0) {
                int j = part.motionSlot * 3;
                if (floats[j] != 0.0F || floats[j + 1] != 0.0F || floats[j + 2] != 0.0F) {
                    matrices.multiply(
                            ROTATION.rotationZYX(floats[j + 2], floats[j + 1], floats[j]));
                }
            }
        }
    }

    private static void pivot(
            MatrixStack matrices, ModelPart modelPart, PartMesh partMesh, float f, float g) {
        ModelTransform defaultTransform = modelPart.getDefaultTransform();
        matrices.translate(
                (f * partMesh.pivotX + (modelPart.originX - defaultTransform.x()) * f) / 16.0F,
                (24.0F - f * partMesh.pivotY + (modelPart.originY - defaultTransform.y()) * f)
                        / 16.0F,
                (-f * partMesh.pivotZ + (modelPart.originZ - defaultTransform.z()) * f) / 16.0F);
        if (g != 0.0F || modelPart.yaw != 0.0F || modelPart.roll != 0.0F) {
            matrices.multiply(ROTATION.rotationZYX(modelPart.roll, modelPart.yaw, g));
        }
        if (modelPart.xScale != 1.0F || modelPart.yScale != 1.0F || modelPart.zScale != 1.0F) {
            matrices.scale(modelPart.xScale, modelPart.yScale, modelPart.zScale);
        }
    }

    private static ModelPart vanilla(PlayerEntityModel playerEntityModel, Bone bone) {
        return switch (bone) {
            case RIGHT_LEG -> playerEntityModel.rightLeg;
            case LEFT_LEG -> playerEntityModel.leftLeg;
            case RIGHT_ARM -> playerEntityModel.rightArm;
            case LEFT_ARM -> playerEntityModel.leftArm;
            case HEAD -> playerEntityModel.head;
            case BODY -> playerEntityModel.body;
        };
    }

    private static void emit(
            Entry entry,
            VertexConsumer vertexConsumer,
            Surface surface,
            int i,
            int j,
            int k,
            boolean bl,
            float f,
            float g,
            float h,
            PartMesh partMesh,
            float f2,
            float g2) {
        int vertexCount = surface.vertexCount();
        if (scratch.length < vertexCount * 6) {
            scratch = new float[Math.max(vertexCount * 6, scratch.length * 2)];
        }
        float[] floats = scratch;
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        float m00 = positionMatrix.m00();
        float m10 = positionMatrix.m10();
        float m20 = positionMatrix.m20();
        float m30 = positionMatrix.m30();
        float m01 = positionMatrix.m01();
        float m11 = positionMatrix.m11();
        float m21 = positionMatrix.m21();
        float m31 = positionMatrix.m31();
        float m02 = positionMatrix.m02();
        float m12 = positionMatrix.m12();
        float m22 = positionMatrix.m22();
        float m32 = positionMatrix.m32();
        float m002 = normalMatrix.m00();
        float m102 = normalMatrix.m10();
        float m202 = normalMatrix.m20();
        float m012 = normalMatrix.m01();
        float m112 = normalMatrix.m11();
        float m212 = normalMatrix.m21();
        float m022 = normalMatrix.m02();
        float m122 = normalMatrix.m12();
        float m222 = normalMatrix.m22();
        float[] pos = surface.pos;
        float[] nrm = surface.nrm;
        float[] floats2 = bl ? surface.weight : null;
        float[] flat = surface.flat;
        Lights lights = flat != null ? CharacterLighting.current() : null;
        if (floats2 != null) {
            LEVELS.update(f, g, h);
        }
        float[] floats3 = f2 == 0.0F && g2 == 0.0F ? null : surface.joint;
        float jointX = partMesh.jointX;
        float jointY = partMesh.jointY;
        float jointZ = partMesh.jointZ;
        if (floats3 != null) {
            JOINT_LEVELS.update(f2, g2, 0.0F);
        }
        for (int o = 0; o < vertexCount; o++) {
            float h2 = pos[o * 3];
            float f3 = pos[o * 3 + 1];
            float g3 = pos[o * 3 + 2];
            float h3 = nrm[o * 3];
            float f4 = nrm[o * 3 + 1];
            float g4 = nrm[o * 3 + 2];
            int m;
            if (floats3 != null && (m = Math.round(floats3[o] * 16.0F)) > 0) {
                float[] values = JOINT_LEVELS.values;
                int n = m * 9;
                float h4 = h2 - jointX;
                float f5 = f3 - jointY;
                float g5 = g3 - jointZ;
                h2 = jointX + values[n] * h4 + values[n + 1] * f5 + values[n + 2] * g5;
                f3 = jointY + values[n + 3] * h4 + values[n + 4] * f5 + values[n + 5] * g5;
                g3 = jointZ + values[n + 6] * h4 + values[n + 7] * f5 + values[n + 8] * g5;
                float h5 = values[n] * h3 + values[n + 1] * f4 + values[n + 2] * g4;
                float f6 = values[n + 3] * h3 + values[n + 4] * f4 + values[n + 5] * g4;
                g4 = values[n + 6] * h3 + values[n + 7] * f4 + values[n + 8] * g4;
                h3 = h5;
                f4 = f6;
            }
            if (floats2 != null) {
                m = Math.round(floats2[o] * 16.0F) * 9;
                float[] values2 = LEVELS.values;
                float g6 = values2[m] * h2 + values2[m + 1] * f3 + values2[m + 2] * g3;
                float h6 = values2[m + 3] * h2 + values2[m + 4] * f3 + values2[m + 5] * g3;
                float f7 = values2[m + 6] * h2 + values2[m + 7] * f3 + values2[m + 8] * g3;
                float g7 = values2[m] * h3 + values2[m + 1] * f4 + values2[m + 2] * g4;
                float h7 = values2[m + 3] * h3 + values2[m + 4] * f4 + values2[m + 5] * g4;
                float f8 = values2[m + 6] * h3 + values2[m + 7] * f4 + values2[m + 8] * g4;
                h2 = g6;
                f3 = h6;
                g3 = f7;
                h3 = g7;
                f4 = h7;
                g4 = f8;
            }
            int p = o * 6;
            floats[p] = m00 * h2 + m10 * f3 + m20 * g3 + m30;
            floats[p + 1] = m01 * h2 + m11 * f3 + m21 * g3 + m31;
            floats[p + 2] = m02 * h2 + m12 * f3 + m22 * g3 + m32;
            float g8 = m002 * h3 + m102 * f4 + m202 * g4;
            float h8 = m012 * h3 + m112 * f4 + m212 * g4;
            float f9 = m022 * h3 + m122 * f4 + m222 * g4;
            float g9 = g8 * g8 + h8 * h8 + f9 * f9;
            if (g9 > 1.0E-12F && Math.abs(g9 - 1.0F) > 0.001F) {
                g9 = 1.0F / (float) Math.sqrt(g9);
                g8 *= g9;
                h8 *= g9;
                f9 *= g9;
            }
            floats[p + 3] = g8;
            floats[p + 4] = h8;
            floats[p + 5] = f9;
            if (lights != null && flat[o] > 0.0F) {
                lights.flatten(floats, p + 3, flat[o]);
            }
        }
        int[] quads = surface.quads;
        int[] color = surface.color;
        float[] uv = surface.uv;
        byte[] glow = surface.glow;
        for (int t = 0; t < quads.length; t++) {
            int quad = quads[t];
            int q = quad * 6;
            int r = k == 255 ? color[quad] : color[quad] & 16777215 | k << 24;
            int u = glow != null && glow[quad] != 0 ? 15728880 : i;
            vertexConsumer.vertex(
                    floats[q],
                    floats[q + 1],
                    floats[q + 2],
                    r,
                    uv[quad * 2],
                    uv[quad * 2 + 1],
                    j,
                    u,
                    floats[q + 3],
                    floats[q + 4],
                    floats[q + 5]);
        }
    }
}
