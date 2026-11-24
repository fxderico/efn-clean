package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
final class AtmosphereShaderSky {

    static final String OFF = "Off";

    static final String SPACE = "Space";

    static final String NORTHERN_LIGHTS = "Northern Lights";

    static final String RINGED_PLANET = "Ringed Planet";

    static final String BLACK_HOLE = "Black Hole";

    private static final List<String> THEMES =
            List.of("Space", "Northern Lights", "Ringed Planet", "Black Hole");

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final RenderPipeline PIPELINE =
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(Identifier.of("efn", "pipeline/sky_theme"))
                    .withVertexShader(Identifier.of("efn", "core/sky_theme"))
                    .withFragmentShader(Identifier.of("efn", "core/sky_theme"))
                    .withUniform("EfnSky", UniformType.UNIFORM_BUFFER)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build();

    private static final int UNIFORM_SIZE = 112;

    private static final float CUBE = 10.0F;

    private static final int CUBE_INDICES = 36;

    private static final float MAX_STEP = 0.1F;

    private static final float TIME_WRAP = 4096.0F;

    private GpuBuffer cube;

    private GpuBuffer uniforms;

    private ByteBuffer uniformData;

    private boolean invalidLogged;

    private float time;

    private long lastNanos;

    AtmosphereShaderSky() {}

    boolean render(
            String string,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            int i,
            int j,
            int k,
            int m,
            int n) {
        int o = THEMES.indexOf(string);
        long l = System.nanoTime();
        float f3 =
                this.lastNanos == 0L ? 0.0F : Math.min((float) (l - this.lastNanos) / 1.0E9F, 0.1F);
        this.lastNanos = l;
        this.time = (this.time + f3 * h) % 4096.0F;
        if (o >= 0 && !(f <= 0.002F)) {
            GpuDevice device = RenderSystem.getDevice();
            if (!device.precompilePipeline(PIPELINE).isValid()) {
                if (!this.invalidLogged) {
                    this.invalidLogged = true;
                    LOGGER.warn(
                            "[EFN] Atmosphere sky shader {} did not compile; using the gradient"
                                    + " sky",
                            PIPELINE.getFragmentShader());
                }
                return false;
            } else {
                Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
                if (framebuffer.textureWidth > 0 && framebuffer.textureHeight > 0) {
                    if (this.cube == null) {
                        this.cube = buildCube(device);
                    }
                    if (this.uniforms == null) {
                        this.uniforms =
                                device.createBuffer(() -> "EFN sky theme settings", 136, 112L);
                        this.uniformData = BufferUtils.createByteBuffer(112);
                    }
                    this.putVec(0, o, f, this.time, g);
                    this.putVec(16, f2, g2, h2, 1.0F);
                    this.putColor(32, i);
                    this.putColor(48, j);
                    this.putColor(64, k);
                    this.putColor(80, m);
                    this.putColor(96, n);
                    CommandEncoder commandEncoder = device.createCommandEncoder();
                    commandEncoder.writeToBuffer(this.uniforms.slice(), this.uniformData);
                    GpuBufferSlice gpuBufferSlice =
                            RenderSystem.getDynamicUniforms()
                                    .write(
                                            RenderSystem.getModelViewStack(),
                                            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                                            new Vector3f(),
                                            new Matrix4f());
                    ShapeIndexBuffer sequentialBuffer =
                            RenderSystem.getSequentialBuffer(DrawMode.QUADS);
                    try (RenderPass renderPass =
                            commandEncoder.createRenderPass(
                                    () -> "EFN sky theme",
                                    framebuffer.getColorAttachmentView(),
                                    OptionalInt.empty(),
                                    framebuffer.getDepthAttachmentView(),
                                    OptionalDouble.empty())) {
                        this.drawSky(renderPass, PIPELINE, gpuBufferSlice, sequentialBuffer);
                    }
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private void drawSky(
            RenderPass renderPass,
            RenderPipeline pipeline,
            GpuBufferSlice gpuBufferSlice,
            ShapeIndexBuffer shapeIndexBuffer) {
        renderPass.setPipeline(pipeline);
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
        renderPass.setUniform("EfnSky", this.uniforms.slice());
        renderPass.setVertexBuffer(0, this.cube);
        renderPass.setIndexBuffer(
                shapeIndexBuffer.getIndexBuffer(36), shapeIndexBuffer.getIndexType());
        renderPass.drawIndexed(0, 0, 36, 1);
    }

    void close() {
        if (this.cube != null) {
            this.cube.close();
        }
        if (this.uniforms != null) {
            this.uniforms.close();
        }
        this.cube = null;
        this.uniforms = null;
        this.uniformData = null;
        this.lastNanos = 0L;
        this.invalidLogged = false;
    }

    private void putVec(int i, float f, float g, float h, float f2) {
        this.uniformData.putFloat(i, f);
        this.uniformData.putFloat(i + 4, g);
        this.uniformData.putFloat(i + 8, h);
        this.uniformData.putFloat(i + 12, f2);
    }

    private void putColor(int i, int j) {
        this.putVec(
                i, (j >> 16 & 0xFF) / 255.0F, (j >> 8 & 0xFF) / 255.0F, (j & 0xFF) / 255.0F, 1.0F);
    }

    private static GpuBuffer buildCube(GpuDevice gpuDevice) {
        GpuBuffer gpuBuffer;
        try (BufferAllocator bufferAllocator =
                new BufferAllocator(24 * VertexFormats.POSITION.getVertexSize())) {
            BufferBuilder bufferBuilder =
                    new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
            float f = 10.0F;
            face(bufferBuilder, f, -f, -f, f, f, -f, f, f, f, f, -f, f);
            face(bufferBuilder, -f, -f, f, -f, f, f, -f, f, -f, -f, -f, -f);
            face(bufferBuilder, -f, f, -f, -f, f, f, f, f, f, f, f, -f);
            face(bufferBuilder, -f, -f, f, -f, -f, -f, f, -f, -f, f, -f, f);
            face(bufferBuilder, f, -f, f, f, f, f, -f, f, f, -f, -f, f);
            face(bufferBuilder, -f, -f, -f, -f, f, -f, f, f, -f, f, -f, -f);
            BuiltBuffer builtBuffer = bufferBuilder.end();
            GpuBuffer buffer;
            label38:
            {
                try {
                    buffer =
                            gpuDevice.createBuffer(
                                    () -> "EFN sky theme cube", 32, builtBuffer.getBuffer());
                    if (builtBuffer == null) {
                        break label38;
                    }
                } catch (Throwable exception) {
                    if (builtBuffer != null) {
                        try {
                            builtBuffer.close();
                        } catch (Throwable exception2) {
                            exception.addSuppressed(exception2);
                        }
                    }
                    throw exception;
                }
                builtBuffer.close();
            }
            gpuBuffer = buffer;
        }
        return gpuBuffer;
    }

    private static void face(
            BufferBuilder bufferBuilder,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            float f3,
            float g3,
            float h3,
            float f4,
            float g4,
            float h4) {
        bufferBuilder.vertex(f, g, h);
        bufferBuilder.vertex(f2, g2, h2);
        bufferBuilder.vertex(f3, g3, h3);
        bufferBuilder.vertex(f4, g4, h4);
    }
}
