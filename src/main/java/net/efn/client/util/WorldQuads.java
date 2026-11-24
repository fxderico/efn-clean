package net.efn.client.util;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.ProjectionType;
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
import net.minecraft.client.render.RawProjectionMatrix;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public final class WorldQuads {

    private static final int MAX_QUADS = 64000;

    private static final int MIN_ALPHA = 3;

    private static final RenderPipeline DEPTH_TESTED =
            pipeline("world_quads", DepthTestFunction.LEQUAL_DEPTH_TEST);

    private static final RenderPipeline ON_TOP =
            pipeline("world_quads_on_top", DepthTestFunction.NO_DEPTH_TEST);

    private static final WorldQuads TESTED = new WorldQuads(DEPTH_TESTED);

    private static final WorldQuads TOP = new WorldQuads(ON_TOP);

    private static double originX;

    private static double originY;

    private static double originZ;

    private static BufferAllocator bytes;

    private static RawProjectionMatrix projectionBuffer;

    private final RenderPipeline pipeline;

    private float[] corners = new float[3072];

    private int[] colors = new int[256];

    private int quads;

    private WorldQuads(RenderPipeline pipeline2) {
        this.pipeline = pipeline2;
    }

    private static RenderPipeline pipeline(String string, DepthTestFunction depthTestFunction) {
        return RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withLocation(Identifier.of("efn", "pipeline/" + string))
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)
                .withDepthTestFunction(depthTestFunction)
                .withCull(false)
                .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
                .build();
    }

    public static WorldQuads layer(boolean bl) {
        return bl ? TOP : TESTED;
    }

    public static void beginFrame(Vec3d vec) {
        originX = vec.x;
        originY = vec.y;
        originZ = vec.z;
        TESTED.quads = 0;
        TOP.quads = 0;
    }

    public boolean quad(
            double d,
            double e,
            double d2,
            double e2,
            double d3,
            double e3,
            double d4,
            double e4,
            double d5,
            double e5,
            double d6,
            double e6,
            int i) {
        if (i >>> 24 >= 3 && this.reserve(1)) {
            int j = this.quads * 12;
            float[] corners2 = this.corners;
            corners2[j] = (float) (d - originX);
            corners2[j + 1] = (float) (e - originY);
            corners2[j + 2] = (float) (d2 - originZ);
            corners2[j + 3] = (float) (e2 - originX);
            corners2[j + 4] = (float) (d3 - originY);
            corners2[j + 5] = (float) (e3 - originZ);
            corners2[j + 6] = (float) (d4 - originX);
            corners2[j + 7] = (float) (e4 - originY);
            corners2[j + 8] = (float) (d5 - originZ);
            corners2[j + 9] = (float) (e5 - originX);
            corners2[j + 10] = (float) (d6 - originY);
            corners2[j + 11] = (float) (e6 - originZ);
            this.colors[this.quads++] = i;
            return true;
        } else {
            return false;
        }
    }

    public void quads(double[] doubles, int[] ints, int i) {
        if (i > 0) {
            int j = Math.min(i, 64000 - this.quads);
            if (j > 0 && this.reserve(j)) {
                float[] corners2 = this.corners;
                int k = this.quads * 12;
                int m = 0;
                for (int n = 0; n < j; n++) {
                    int o = ints[n];
                    if (o >>> 24 >= 3) {
                        int p = n * 12;
                        for (int q = 0; q < 12; q += 3) {
                            corners2[k + q] = (float) (doubles[p + q] - originX);
                            corners2[k + q + 1] = (float) (doubles[p + q + 1] - originY);
                            corners2[k + q + 2] = (float) (doubles[p + q + 2] - originZ);
                        }
                        this.colors[this.quads + m] = o;
                        m++;
                        k += 12;
                    }
                }
                this.quads += m;
            }
        }
    }

    private boolean reserve(int i) {
        if (this.quads + i <= this.colors.length) {
            return true;
        } else if (this.quads + i > 64000) {
            return false;
        } else {
            int j = Math.min(64000, Math.max(this.colors.length * 2, this.quads + i));
            this.colors = Arrays.copyOf(this.colors, j);
            this.corners = Arrays.copyOf(this.corners, j * 12);
            return true;
        }
    }

    public static void render(Matrix4f matrix, Matrix4f matrix2) {
        if (TESTED.quads != 0 || TOP.quads != 0) {
            if (projectionBuffer == null) {
                projectionBuffer = new RawProjectionMatrix("efn world quads");
            }
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(
                    projectionBuffer.set(matrix2), ProjectionType.PERSPECTIVE);
            try {
                TESTED.draw(matrix);
                TOP.draw(matrix);
            } finally {
                RenderSystem.restoreProjectionMatrix();
                TESTED.quads = 0;
                TOP.quads = 0;
            }
        }
    }

    private void draw(Matrix4f matrix) {
        if (this.quads != 0) {
            if (bytes == null) {
                bytes =
                        new BufferAllocator(
                                this.quads * 4 * VertexFormats.POSITION_COLOR.getVertexSize());
            }
            BufferBuilder bufferBuilder =
                    new BufferBuilder(bytes, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            float[] corners2 = this.corners;
            int i = 0;
            for (int j = 0; i < this.quads; j += 12) {
                int color = this.colors[i];
                bufferBuilder.vertex(corners2[j], corners2[j + 1], corners2[j + 2]).color(color);
                bufferBuilder
                        .vertex(corners2[j + 3], corners2[j + 4], corners2[j + 5])
                        .color(color);
                bufferBuilder
                        .vertex(corners2[j + 6], corners2[j + 7], corners2[j + 8])
                        .color(color);
                bufferBuilder
                        .vertex(corners2[j + 9], corners2[j + 10], corners2[j + 11])
                        .color(color);
                i++;
            }
            try (BuiltBuffer builtBuffer = bufferBuilder.endNullable()) {
                if (builtBuffer == null) {
                    return;
                }
                GpuBuffer gpuBuffer =
                        VertexFormats.POSITION_COLOR.uploadImmediateVertexBuffer(
                                builtBuffer.getBuffer());
                ShapeIndexBuffer sequentialBuffer =
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS);
                int indexCount = builtBuffer.getDrawParameters().indexCount();
                GpuBufferSlice gpuBufferSlice =
                        RenderSystem.getDynamicUniforms()
                                .write(
                                        matrix,
                                        new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                                        new Vector3f(),
                                        new Matrix4f());
                Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
                try (RenderPass renderPass =
                        RenderSystem.getDevice()
                                .createCommandEncoder()
                                .createRenderPass(
                                        () -> "EFN world quads",
                                        framebuffer.getColorAttachmentView(),
                                        OptionalInt.empty(),
                                        framebuffer.getDepthAttachmentView(),
                                        OptionalDouble.empty())) {
                    renderPass.setPipeline(this.pipeline);
                    RenderSystem.bindDefaultUniforms(renderPass);
                    renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                    renderPass.setVertexBuffer(0, gpuBuffer);
                    renderPass.setIndexBuffer(
                            sequentialBuffer.getIndexBuffer(indexCount),
                            sequentialBuffer.getIndexType());
                    renderPass.drawIndexed(0, 0, indexCount, 1);
                }
            }
        }
    }
}
