package net.efn.client.gui;

import com.mojang.serialization.MapCodec;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.SkullBlock.Type;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel.SkullModelState;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.model.special.SimpleSpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer.BakeContext;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import org.joml.Vector3fc;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class GlintHeadRenderer implements SimpleSpecialModelRenderer {

    public static final Identifier ID = Identifier.of("efn", "glint_head");

    private final SkullBlockEntityModel model;

    private GlintHeadRenderer(SkullBlockEntityModel model2) {
        this.model = model2;
    }

    public static ItemStack stack() {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponentTypes.ITEM_MODEL, ID);
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    public Void getData(ItemStack stack) {
        return this.getData(stack);
    }

    public void render(
            Void voidValue,
            ItemDisplayContext itemDisplayContext,
            MatrixStack matrices,
            OrderedRenderCommandQueue orderedRenderCommandQueue,
            int i,
            int j,
            boolean bl,
            int k) {
        this.render(
                voidValue, itemDisplayContext, matrices, orderedRenderCommandQueue, i, j, bl, k);
    }

    @Override
    public void render(
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            int overlay,
            boolean glint,
            int i) {
        SkullBlockEntityRenderer.render(
                null,
                180.0F,
                0.0F,
                matrices,
                queue,
                light,
                this.model,
                PlayerSkinCache.DEFAULT_RENDER_LAYER,
                i,
                null);
        if (glint) {
            matrices.push();
            matrices.translate(0.5F, 0.0F, 0.5F);
            matrices.scale(-1.0F, -1.0F, 1.0F);
            SkullModelState skullModelState = new SkullModelState();
            skullModelState.yaw = 180.0F;
            queue.getBatchingQueue(1)
                    .submitModel(
                            this.model,
                            skullModelState,
                            matrices,
                            RenderLayers.entityGlint(),
                            light,
                            OverlayTexture.DEFAULT_UV,
                            0,
                            null);
            matrices.pop();
        }
    }

    @Override
    public void collectVertices(Consumer<Vector3fc> consumer) {
        MatrixStack matrices = new MatrixStack();
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.scale(-1.0F, -1.0F, 1.0F);
        this.model.getRootPart().collectVertices(matrices, consumer);
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked()
            implements net.minecraft.client.render.item.model.special.SpecialModelRenderer.Unbaked {

        public static final MapCodec<GlintHeadRenderer.Unbaked> MAP_CODEC =
                MapCodec.unit(new GlintHeadRenderer.Unbaked());

        @Override
        public MapCodec<GlintHeadRenderer.Unbaked> getCodec() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(BakeContext context) {
            SkullBlockEntityModel models =
                    SkullBlockEntityRenderer.getModels(context.entityModelSet(), Type.PLAYER);
            return models == null ? null : new GlintHeadRenderer(models);
        }
    }
}
