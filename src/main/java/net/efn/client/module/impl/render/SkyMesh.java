package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
final class SkyMesh {

    static final RenderPipeline GLOW = pipeline("sky_glow", BlendFunction.LIGHTNING);

    static final RenderPipeline PAINT = pipeline("sky_paint", null);

    private static final int VERTEX_SIZE = VertexFormats.POSITION_COLOR.getVertexSize();

    private final String label;

    private final RenderPipeline pipeline;

    private final int maxQuads;

    private BufferAllocator bytes;

    private GpuBuffer buffer;

    private int indexCount;

    private boolean built;

    SkyMesh(String label2, RenderPipeline pipeline2, int maxQuads2) {
        this.label = label2;
        this.pipeline = pipeline2;
        this.maxQuads = maxQuads2;
    }

    private static RenderPipeline pipeline(String string, BlendFunction blendFunction) {
        Builder builder =
                RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                        .withLocation(Identifier.of("efn", "pipeline/" + string))
                        .withVertexShader("core/position_color")
                        .withFragmentShader("core/position_color")
                        .withDepthWrite(false)
                        .withCull(false)
                        .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS);
        return (blendFunction == null ? builder.withoutBlend() : builder.withBlend(blendFunction))
                .build();
    }

    BufferBuilder begin() {
        if (this.bytes == null) {
            this.bytes = new BufferAllocator(this.maxQuads * 4 * VERTEX_SIZE);
        }
        return new BufferBuilder(this.bytes, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    }

    void upload(BufferBuilder bufferBuilder) {
        this.built = true;
        try (BuiltBuffer builtBuffer = bufferBuilder.endNullable()) {
            if (builtBuffer == null) {
                this.indexCount = 0;
                return;
            }
            if (this.buffer == null) {
                this.buffer =
                        RenderSystem.getDevice()
                                .createBuffer(
                                        () -> this.label, 40, this.maxQuads * 4L * VERTEX_SIZE);
            }
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .writeToBuffer(
                            this.buffer.slice(0L, builtBuffer.getBuffer().remaining()),
                            builtBuffer.getBuffer());
            this.indexCount = builtBuffer.getDrawParameters().indexCount();
        }
    }

    boolean isBuilt() {
        return this.built;
    }

    void draw(MatrixStack matrices, float f) {
        if (this.indexCount != 0 && !(f <= 0.002F)) {
            ShapeIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer(DrawMode.QUADS);
            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            try {
                modelViewStack.mul(matrices.peek().getPositionMatrix());
                GpuBufferSlice gpuBufferSlice =
                        RenderSystem.getDynamicUniforms()
                                .write(
                                        modelViewStack,
                                        new Vector4f(1.0F, 1.0F, 1.0F, f),
                                        new Vector3f(),
                                        new Matrix4f());
                Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
                try (RenderPass renderPass =
                        RenderSystem.getDevice()
                                .createCommandEncoder()
                                .createRenderPass(
                                        () -> this.label,
                                        framebuffer.getColorAttachmentView(),
                                        OptionalInt.empty(),
                                        framebuffer.getDepthAttachmentView(),
                                        OptionalDouble.empty())) {
                    renderPass.setPipeline(this.pipeline);
                    RenderSystem.bindDefaultUniforms(renderPass);
                    renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                    renderPass.setVertexBuffer(0, this.buffer);
                    renderPass.setIndexBuffer(
                            sequentialBuffer.getIndexBuffer(this.indexCount),
                            sequentialBuffer.getIndexType());
                    renderPass.drawIndexed(0, 0, this.indexCount, 1);
                }
            } finally {
                modelViewStack.popMatrix();
            }
        }
    }

    void close() {
        if (this.buffer != null) {
            this.buffer.close();
        }
        if (this.bytes != null) {
            this.bytes.close();
        }
        this.buffer = null;
        this.bytes = null;
        this.indexCount = 0;
        this.built = false;
    }

    static void vertex(BufferBuilder bufferBuilder, float f, float g, float h, int i) {
        bufferBuilder.vertex(f, g, h).color(i);
    }
}
