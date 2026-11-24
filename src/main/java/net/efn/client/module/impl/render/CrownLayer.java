package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class CrownLayer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    private static final Identifier TEXTURE =
            Identifier.of("efn", "textures/cosmetic/headdress_base.png");

    private static final float HEAD_CENTRE = -0.25F;

    public CrownLayer(
            FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>
                    featureRendererContext) {
        super(featureRendererContext);
    }

    public void render(
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            PlayerEntityRenderState playerEntityRenderState,
            float f,
            float g) {
        HeadCosmeticModule worn = HeadCosmeticModule.worn;
        if (worn != null
                && !playerEntityRenderState.invisible
                && worn.wears(playerEntityRenderState)) {
            boolean bl =
                    !playerEntityRenderState.equippedHeadStack.isEmpty()
                            || !playerEntityRenderState.headItemRenderState.isEmpty()
                            || playerEntityRenderState.wearingSkullType != null;
            boolean bl2 =
                    !playerEntityRenderState.equippedChestStack.isEmpty()
                            && !playerEntityRenderState.equippedChestStack.contains(
                                    DataComponentTypes.GLIDER);
            CrownMesh crownMesh = worn.headMesh(bl);
            CrownMesh crownMesh2 = worn.bodyMesh(bl2);
            CrownMesh.Look look = worn.look();
            float scale = worn.scale();
            RenderLayer renderLayer = RenderLayers.entityCutoutNoCull(TEXTURE);
            PlayerEntityModel contextModel = this.getContextModel();
            matrices.push();
            contextModel.getRootPart().applyTransform(matrices);
            matrices.push();
            contextModel.getHead().applyTransform(matrices);
            if (scale != 1.0F) {
                matrices.translate(0.0F, -0.25F, 0.0F);
                matrices.scale(scale, scale, scale);
                matrices.translate(0.0F, 0.25F, 0.0F);
            }
            orderedRenderCommandQueue.submitCustom(
                    matrices,
                    renderLayer,
                    (entry, vertexConsumer) -> crownMesh.emit(entry, vertexConsumer, i, look));
            matrices.pop();
            if (!crownMesh2.isEmpty()) {
                contextModel.body.applyTransform(matrices);
                orderedRenderCommandQueue.submitCustom(
                        matrices,
                        renderLayer,
                        (entry, vertexConsumer) -> crownMesh2.emit(entry, vertexConsumer, i, look));
            }
            matrices.pop();
        }
    }
}
