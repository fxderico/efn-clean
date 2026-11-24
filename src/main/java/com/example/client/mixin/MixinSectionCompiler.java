package com.example.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.VertexSorter;

import net.efn.client.module.impl.render.XrayModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.chunk.BlockBufferAllocatorStorage;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.render.chunk.SectionBuilder;
import net.minecraft.client.render.chunk.SectionBuilder.RenderData;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({SectionBuilder.class})
public abstract class MixinSectionCompiler {

    public MixinSectionCompiler() {}

    @WrapOperation(
            method = {
                "build(Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/client/render/chunk/ChunkRendererRegion;Lcom/mojang/blaze3d/systems/VertexSorter;Lnet/minecraft/client/render/chunk/BlockBufferAllocatorStorage;)Lnet/minecraft/client/render/chunk/SectionBuilder$RenderData;"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/render/chunk/ChunkRendererRegion;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;")
            })
    private BlockState efn$xrayBlockState(
            ChunkRendererRegion chunkRendererRegion,
            BlockPos pos,
            Operation<BlockState> operation) {
        BlockState state = (BlockState) operation.call(chunkRendererRegion, pos);
        XrayModule.Snapshot active = XrayModule.active;
        if (active != null && !state.isAir()) {
            float[] cURRENT_ALPHA = XrayModule.CURRENT_ALPHA.get();
            float alpha;
            cURRENT_ALPHA[0] = alpha = active.alphaFor(chunkRendererRegion, state, pos);
            return alpha <= 0.0F ? Blocks.AIR.getDefaultState() : state;
        } else {
            return state;
        }
    }

    @WrapOperation(
            method = {
                "build(Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/client/render/chunk/ChunkRendererRegion;Lcom/mojang/blaze3d/systems/VertexSorter;Lnet/minecraft/client/render/chunk/BlockBufferAllocatorStorage;)Lnet/minecraft/client/render/chunk/SectionBuilder$RenderData;"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/render/BlockRenderLayers;getBlockLayer(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/BlockRenderLayer;")
            })
    private BlockRenderLayer efn$xrayBlockLayer(
            BlockState state, Operation<BlockRenderLayer> operation) {
        if (XrayModule.active == null) {
            return (BlockRenderLayer) operation.call(state);
        } else {
            float f = XrayModule.CURRENT_ALPHA.get()[0];
            return f > 0.0F && f < 1.0F
                    ? BlockRenderLayer.TRANSLUCENT
                    : (BlockRenderLayer) operation.call(state);
        }
    }

    @WrapOperation(
            method = {
                "build(Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/client/render/chunk/ChunkRendererRegion;Lcom/mojang/blaze3d/systems/VertexSorter;Lnet/minecraft/client/render/chunk/BlockBufferAllocatorStorage;)Lnet/minecraft/client/render/chunk/SectionBuilder$RenderData;"
            },
            at = {
                @At(
                        value = "INVOKE",
                        target =
                                "Lnet/minecraft/client/render/BlockRenderLayers;getFluidLayer(Lnet/minecraft/fluid/FluidState;)Lnet/minecraft/client/render/BlockRenderLayer;")
            })
    private BlockRenderLayer efn$xrayFluidLayer(
            FluidState fluidState, Operation<BlockRenderLayer> operation) {
        if (XrayModule.active == null) {
            return (BlockRenderLayer) operation.call(fluidState);
        } else {
            float f = XrayModule.CURRENT_ALPHA.get()[0];
            return f > 0.0F && f < 1.0F
                    ? BlockRenderLayer.TRANSLUCENT
                    : (BlockRenderLayer) operation.call(fluidState);
        }
    }

    @Inject(
            method = {
                "build(Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/client/render/chunk/ChunkRendererRegion;Lcom/mojang/blaze3d/systems/VertexSorter;Lnet/minecraft/client/render/chunk/BlockBufferAllocatorStorage;)Lnet/minecraft/client/render/chunk/SectionBuilder$RenderData;"
            },
            at = {@At("RETURN")})
    private void efn$xrayResetAlpha(
            ChunkSectionPos chunkSectionPos,
            ChunkRendererRegion chunkRendererRegion,
            VertexSorter vertexSorter,
            BlockBufferAllocatorStorage blockBufferAllocatorStorage,
            CallbackInfoReturnable<RenderData> cir) {
        XrayModule.CURRENT_ALPHA.get()[0] = 1.0F;
    }
}
