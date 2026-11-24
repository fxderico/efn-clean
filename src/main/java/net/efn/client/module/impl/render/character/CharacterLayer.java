package net.efn.client.module.impl.render.character;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

@Environment(EnvType.CLIENT)
public final class CharacterLayer
        extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    public CharacterLayer(
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
        WornCharacter wornCharacter = CharacterModule.worn(playerEntityRenderState);
        if (wornCharacter != null
                && (!playerEntityRenderState.invisible
                        || !playerEntityRenderState.invisibleToPlayer)) {
            CharacterRenderer.submit(
                    wornCharacter,
                    this.getContextModel(),
                    matrices,
                    orderedRenderCommandQueue,
                    i,
                    playerEntityRenderState);
        }
    }

    public static boolean firstPersonHand(
            PlayerEntityModel playerEntityModel,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            ModelPart modelPart) {
        return CharacterRenderer.firstPersonHand(
                playerEntityModel, matrices, orderedRenderCommandQueue, i, modelPart);
    }

    public static boolean translateToHand(
            PlayerEntityModel playerEntityModel,
            PlayerEntityRenderState playerEntityRenderState,
            Arm arm,
            MatrixStack matrices) {
        return CharacterRenderer.translateToHand(
                playerEntityModel, playerEntityRenderState, arm, matrices);
    }
}
