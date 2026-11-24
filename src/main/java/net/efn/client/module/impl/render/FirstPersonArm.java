package net.efn.client.module.impl.render;

import com.example.client.mixin.AccessorHandItemLayer;
import com.example.client.mixin.AccessorHandItemRenderState;
import com.example.client.mixin.AccessorSkinChangeEntityRenderDispatcher;

import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.SkinChangeModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
final class FirstPersonArm {

    static final Vector3f FOREARM = new Vector3f(0.75F, -0.45F, 0.5F).normalize();

    static final float ELBOW = 0.6F;

    private static final Vector3f ARM_UP = new Vector3f(0.0F, 1.0F, 0.0F);

    private static final float ARM_FIST = 0.5F;

    private static final float ARM_SHOULDER = -0.625F;

    private static final float SHOULDER_STRETCH = 3.0F;

    private static final float HANDLE_INSET = 0.1875F;

    private static final float BLOCK_BELOW = 0.17F;

    private static final Vector3f BLOCK_SHIFT = new Vector3f(0.09F, 0.0F, 0.06F);

    private static final ItemRenderState MODEL = new ItemRenderState();

    private FirstPersonArm() {}

    static FirstPersonArm.Arm arm() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player != null && !player.isInvisible()) {
            EntityRenderManager entityRenderDispatcher = client.getEntityRenderDispatcher();
            PlayerEntityRenderer playerRenderer = entityRenderDispatcher.getPlayerRenderer(player);
            SkinTextures skin = player.getSkin();
            SkinChangeModule module = ModuleRegistry.get(SkinChangeModule.class);
            SkinTextures skinTextures;
            if (module != null
                    && module.isEnabled()
                    && (skinTextures = module.skinFor(player.getId(), skin)) != skin) {
                PlayerEntityRenderer efn$skinChangePlayerRenderer =
                        ((AccessorSkinChangeEntityRenderDispatcher) entityRenderDispatcher)
                                .efn$skinChangePlayerRenderers()
                                .get(skinTextures.model());
                if (efn$skinChangePlayerRenderer != null) {
                    playerRenderer = efn$skinChangePlayerRenderer;
                }
                skin = skinTextures;
            }
            return new FirstPersonArm.Arm(
                    playerRenderer,
                    skin.body().texturePath(),
                    player.isModelPartVisible(PlayerModelPart.RIGHT_SLEEVE),
                    player.isModelPartVisible(PlayerModelPart.LEFT_SLEEVE),
                    skin.model() == PlayerSkinType.SLIM);
        } else {
            return null;
        }
    }

    static void hold(
            MatrixStack matrices,
            FirstPersonArm.Arm arm,
            ItemStack stack,
            ItemDisplayContext itemDisplayContext,
            LivingEntity entity,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            float f,
            float g,
            float h) {
        Vector3f vec = grip(stack, itemDisplayContext, entity);
        if (vec != null) {
            Vector3f vec2 = new Vector3f(vec).fma(0.6F, FOREARM);
            vec.add(f, g, h);
            boolean leftHand = itemDisplayContext.isLeftHand();
            matrices.push();
            if (leftHand) {
                matrices.scale(-1.0F, 1.0F, 1.0F);
            }
            place(
                    matrices,
                    vec,
                    vec2,
                    arm.slim(),
                    matrixStackx -> arm.draw(matrixStackx, leftHand, orderedRenderCommandQueue, i));
            matrices.pop();
        }
    }

    static Vector3f grip(
            ItemStack stack, ItemDisplayContext itemDisplayContext, LivingEntity entity) {
        if (!stack.isEmpty()
                && !stack.isOf(Items.BOW)
                && !stack.isOf(Items.CROSSBOW)
                && !stack.isOf(Items.SHIELD)
                && !stack.isOf(Items.TRIDENT)
                && !stack.isOf(Items.SPYGLASS)) {
            MinecraftClient.getInstance()
                    .getItemModelManager()
                    .clearAndUpdate(
                            MODEL,
                            stack,
                            itemDisplayContext,
                            entity.getEntityWorld(),
                            entity,
                            entity.getId() + itemDisplayContext.ordinal());
            AccessorHandItemRenderState accessorHandItemRenderState =
                    (AccessorHandItemRenderState) MODEL;
            if (accessorHandItemRenderState.efn$handLayerCount() <= 0) {
                return null;
            } else {
                AccessorHandItemLayer efn$handLayer =
                        (AccessorHandItemLayer) accessorHandItemRenderState.efn$handLayers()[0];
                Transformation transformation = efn$handLayer.efn$handTransform();
                Entry entry = new Entry();
                if (transformation != null) {
                    transformation.apply(itemDisplayContext.isLeftHand(), entry);
                }
                Matrix4f positionMatrix = entry.getPositionMatrix();
                if (!MODEL.isSideLit()) {
                    float[] floats =
                            handle(
                                    efn$handLayer.efn$handExtents() != null
                                            ? efn$handLayer.efn$handExtents().get()
                                            : null);
                    Vector3f vec =
                            positionMatrix.transformPosition(
                                    floats[0] - 0.5F, floats[1] - 0.5F, 0.0F, new Vector3f());
                    if (itemDisplayContext.isLeftHand()) {
                        vec.x = -vec.x;
                    }
                    return vec;
                } else if (!(stack.getItem() instanceof BlockItem)) {
                    return null;
                } else {
                    Vector3f vec2 =
                            positionMatrix.transformPosition(0.0F, 0.0F, 0.0F, new Vector3f());
                    if (itemDisplayContext.isLeftHand()) {
                        vec2.x = -vec2.x;
                    }
                    return blockGrip(
                            vec2, transformation != null ? transformation.scale().y() : 1.0F);
                }
            }
        } else {
            return null;
        }
    }

    static Vector3f blockGrip(Vector3f vec, float f) {
        return new Vector3f(vec).add(BLOCK_SHIFT).sub(0.0F, 0.5F * f + 0.17F, 0.0F);
    }

    static float[] handle(Vector3fc[] vector3fcs) {
        float f = 3.4028235E38F;
        if (vector3fcs != null) {
            for (Vector3fc vector3fc : vector3fcs) {
                if (!squareCorner(vector3fc)) {
                    f = Math.min(f, vector3fc.x() + vector3fc.y());
                }
            }
        }
        if (f == 3.4028235E38F) {
            return new float[] {0.1875F, 0.1875F};
        } else {
            float g = 0.0F;
            float h = 0.0F;
            int i = 0;
            for (Vector3fc vector3fc2 : vector3fcs) {
                if (!squareCorner(vector3fc2) && !(vector3fc2.x() + vector3fc2.y() > f + 1.0E-4F)) {
                    g += vector3fc2.x();
                    h += vector3fc2.y();
                    i++;
                }
            }
            return new float[] {g / i + 0.1875F, h / i + 0.1875F};
        }
    }

    private static boolean squareCorner(Vector3fc vector3fc) {
        return (vector3fc.x() < 1.0E-4F || vector3fc.x() > 0.9999F)
                && (vector3fc.y() < 1.0E-4F || vector3fc.y() > 0.9999F);
    }

    static void place(
            MatrixStack matrices,
            Vector3f vec,
            Vector3f vec2,
            boolean bl,
            Consumer<MatrixStack> consumer) {
        Vector3f vec3 = new Vector3f(vec).sub(vec2).normalize();
        Vector3f vec4 = new Vector3f(ARM_UP).fma(-ARM_UP.dot(vec3), vec3).normalize();
        Vector3f vec5 = new Vector3f(vec3).cross(vec4);
        matrices.push();
        matrices.translate(vec.x, vec.y, vec.z);
        matrices.multiply(new Quaternionf().setFromNormalized(new Matrix3f(vec5, vec3, vec4)));
        drawPart(matrices, bl, consumer);
        matrices.translate(0.0F, -0.625F, 0.0F);
        matrices.scale(-1.0F, -3.0F, 1.0F);
        matrices.translate(0.0F, 0.625F, 0.0F);
        drawPart(matrices, bl, consumer);
        matrices.pop();
    }

    private static void drawPart(MatrixStack matrices, boolean bl, Consumer<MatrixStack> consumer) {
        matrices.push();
        matrices.translate(bl ? 0.03125F : 0.0625F, -0.5F, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-0.1F));
        matrices.translate(0.3125F, -0.125F, 0.0F);
        consumer.accept(matrices);
        matrices.pop();
    }

    static void shoulder(MatrixStack matrices, boolean bl, boolean bl2) {
        float f = bl ? -0.3125F : 0.3125F;
        float g = 0.125F;
        float h = bl ? 0.1F : -0.1F;
        float f2 = (bl ? -1.0F : 1.0F) * (bl2 ? 0.5F : 1.0F) / 16.0F;
        float g2 = -0.125F;
        matrices.translate(f, g, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation(h));
        matrices.translate(f2, g2, 0.0F);
        matrices.scale(-1.0F, -3.0F, 1.0F);
        matrices.translate(-f2, -g2, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-h));
        matrices.translate(-f, -g, 0.0F);
    }

    @Environment(EnvType.CLIENT)
    record Arm(
            PlayerEntityRenderer<AbstractClientPlayerEntity> renderer,
            Identifier texture,
            boolean rightSleeve,
            boolean leftSleeve,
            boolean slim) {

        void draw(
                MatrixStack matrices,
                boolean bl,
                OrderedRenderCommandQueue orderedRenderCommandQueue,
                int i) {
            if (bl) {
                matrices.scale(-1.0F, 1.0F, 1.0F);
                this.renderer.renderLeftArm(
                        matrices, orderedRenderCommandQueue, i, this.texture, this.leftSleeve);
            } else {
                this.renderer.renderRightArm(
                        matrices, orderedRenderCommandQueue, i, this.texture, this.rightSleeve);
            }
        }
    }
}
