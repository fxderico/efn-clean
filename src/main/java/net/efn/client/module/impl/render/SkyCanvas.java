package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
final class SkyCanvas implements SkyPaint {

    private static final float RADIUS = 90.0F;

    private static final float WIDEST_HALF_ANGLE = 1.35F;

    private static final int HALO_SIZE = 128;

    private static final int START_BYTES =
            4096 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize();

    private static final RenderPipeline BLEND = pipeline("sky_canvas", BlendFunction.TRANSLUCENT);

    private static final RenderPipeline ADD = pipeline("sky_canvas_add", BlendFunction.LIGHTNING);

    private static SkyTexture plain;

    private static SkyTexture halo;

    private SkyCanvas.Run[] runs = new SkyCanvas.Run[64];

    private int runCount;

    private BufferAllocator bytes;

    private BufferBuilder builder;

    private int quads;

    private float forwardX;

    private float forwardY;

    private float forwardZ;

    private float rightX;

    private float rightZ;

    private float upX;

    private float upY;

    private float upZ;

    SkyCanvas() {}

    private static RenderPipeline pipeline(String string, BlendFunction blendFunction) {
        return RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withLocation(Identifier.of("efn", "pipeline/" + string))
                .withVertexShader("core/position_tex_color")
                .withFragmentShader("core/position_tex_color")
                .withSampler("Sampler0")
                .withBlend(blendFunction)
                .withDepthWrite(false)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withCull(false)
                .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
                .build();
    }

    @Override
    public SkyImage upload(String string, int i, int j, int[] is) {
        return SkyTexture.upload(string, i, j, is);
    }

    @Override
    public SkyImage upload(String string, int i, int j, int[][] is) {
        return SkyTexture.upload(string, i, j, is);
    }

    @Override
    public void gradient(boolean bl, float f, float g, float h, float i, int j, int k) {
        BufferBuilder bufferBuilder = this.open(plain(), bl, 1);
        this.onSphere(bufferBuilder, f, h, 0.0F, 0.0F, j);
        this.onSphere(bufferBuilder, g, h, 1.0F, 0.0F, j);
        this.onSphere(bufferBuilder, g, i, 1.0F, 1.0F, k);
        this.onSphere(bufferBuilder, f, i, 0.0F, 1.0F, k);
    }

    @Override
    public void band(
            SkyImage skyImage, float f, float g, float h, float i, int j, boolean bl, int k) {
        BufferBuilder bufferBuilder = this.open((SkyTexture) skyImage, bl, k);
        for (int m = 0; m < k; m++) {
            float f2 = (float) m / k;
            float g2 = (float) (m + 1) / k;
            float h2 = MathHelper.lerp(f2, f, g);
            float f3 = MathHelper.lerp(g2, f, g);
            this.onSphere(bufferBuilder, h2, h, f2, 0.0F, j);
            this.onSphere(bufferBuilder, f3, h, g2, 0.0F, j);
            this.onSphere(bufferBuilder, f3, i, g2, 1.0F, j);
            this.onSphere(bufferBuilder, h2, i, f2, 1.0F, j);
        }
    }

    @Override
    public void sprite(
            SkyImage skyImage, float f, float g, float h, float i, float j, int k, boolean bl) {
        BufferBuilder bufferBuilder = this.open((SkyTexture) skyImage, bl, 1);
        this.face(f, g);
        float f2 = 90.0F * (float) Math.tan(Math.min(h * 1.0471976F * 0.5F, 1.35F));
        float g2 = 90.0F * (float) Math.tan(Math.min(i * 1.0471976F * 0.5F, 1.35F));
        float h2 = (float) Math.cos(j);
        float f3 = (float) Math.sin(j);
        this.onPlane(bufferBuilder, -f2, -g2, h2, f3, 0.0F, 0.0F, k);
        this.onPlane(bufferBuilder, f2, -g2, h2, f3, 1.0F, 0.0F, k);
        this.onPlane(bufferBuilder, f2, g2, h2, f3, 1.0F, 1.0F, k);
        this.onPlane(bufferBuilder, -f2, g2, h2, f3, 0.0F, 1.0F, k);
    }

    @Override
    public void glow(float f, float g, float h, int i, float j) {
        if (!(j <= 0.003F)) {
            this.sprite(
                    halo(),
                    f,
                    g,
                    h * 2.0F,
                    h * 2.0F,
                    0.0F,
                    ColorHelper.withAlpha(MathHelper.clamp(j, 0.0F, 1.0F), i),
                    true);
        }
    }

    void flush(float f) {
        if (this.builder != null) {
            BufferBuilder builder2 = this.builder;
            this.builder = null;
            label147:
            {
                try (BuiltBuffer builtBuffer = builder2.endNullable()) {
                    if (builtBuffer != null && this.runCount != 0) {
                        GpuBuffer gpuBuffer =
                                VertexFormats.POSITION_TEXTURE_COLOR.uploadImmediateVertexBuffer(
                                        builtBuffer.getBuffer());
                        ShapeIndexBuffer sequentialBuffer =
                                RenderSystem.getSequentialBuffer(DrawMode.QUADS);
                        GpuBuffer indexBuffer = sequentialBuffer.getIndexBuffer(this.quads * 6);
                        GpuBufferSlice gpuBufferSlice =
                                RenderSystem.getDynamicUniforms()
                                        .write(
                                                RenderSystem.getModelViewStack(),
                                                new Vector4f(1.0F, 1.0F, 1.0F, f),
                                                new Vector3f(),
                                                new Matrix4f());
                        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
                        try (RenderPass renderPass =
                                RenderSystem.getDevice()
                                        .createCommandEncoder()
                                        .createRenderPass(
                                                () -> "EFN sky canvas",
                                                framebuffer.getColorAttachmentView(),
                                                OptionalInt.empty(),
                                                framebuffer.getDepthAttachmentView(),
                                                OptionalDouble.empty())) {
                            RenderPipeline renderPipeline = null;
                            for (int i = 0; i < this.runCount; i++) {
                                SkyCanvas.Run run = this.runs[i];
                                RenderPipeline renderPipeline2 = run.additive ? ADD : BLEND;
                                if (renderPipeline2 != renderPipeline) {
                                    renderPipeline = renderPipeline2;
                                    renderPass.setPipeline(renderPipeline2);
                                    RenderSystem.bindDefaultUniforms(renderPass);
                                    renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                                    renderPass.setVertexBuffer(0, gpuBuffer);
                                    renderPass.setIndexBuffer(
                                            indexBuffer, sequentialBuffer.getIndexType());
                                }
                                renderPass.bindTexture(
                                        "Sampler0", run.texture.view(), run.texture.sampler());
                                renderPass.drawIndexed(0, run.first * 6, run.count * 6, 1);
                            }
                            break label147;
                        }
                    }
                } finally {
                    this.runCount = 0;
                    this.quads = 0;
                }
                return;
            }
        }
    }

    void clear() {
        BufferBuilder builder2 = this.builder;
        this.builder = null;
        this.runCount = 0;
        this.quads = 0;
        if (builder2 != null) {
            BuiltBuffer builtBuffer = builder2.endNullable();
            if (builtBuffer != null) {
                builtBuffer.close();
            }
        }
    }

    private BufferBuilder open(SkyTexture skyTexture, boolean bl, int quads2) {
        if (this.builder == null) {
            if (this.bytes == null) {
                this.bytes = new BufferAllocator(START_BYTES);
            }
            this.builder =
                    new BufferBuilder(
                            this.bytes, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        }
        SkyCanvas.Run run = this.runCount == 0 ? null : this.runs[this.runCount - 1];
        if (run == null || run.texture != skyTexture || run.additive != bl) {
            if (this.runCount == this.runs.length) {
                this.runs = Arrays.copyOf(this.runs, this.runCount * 2);
            }
            if ((run = this.runs[this.runCount]) == null) {
                run = this.runs[this.runCount] = new SkyCanvas.Run();
            }
            run.texture = skyTexture;
            run.additive = bl;
            run.first = this.quads;
            run.count = 0;
            this.runCount++;
        }
        run.count += quads2;
        this.quads += quads2;
        return this.builder;
    }

    private void face(float f, float g) {
        double d = (f - 0.5) * 6.2831854820251465;
        double e = (0.72F - g) * 1.0471976F;
        float h = (float) Math.cos(d);
        float f2 = (float) Math.sin(d);
        float g2 = (float) Math.cos(e);
        float h2 = (float) Math.sin(e);
        this.forwardX = -h * g2;
        this.forwardY = h2;
        this.forwardZ = -f2 * g2;
        this.rightX = f2;
        this.rightZ = -h;
        this.upX = h * h2;
        this.upY = g2;
        this.upZ = f2 * h2;
    }

    private void onSphere(BufferBuilder bufferBuilder, float f, float g, float h, float f2, int i) {
        this.face(f, g);
        bufferBuilder
                .vertex(this.forwardX * 90.0F, this.forwardY * 90.0F, this.forwardZ * 90.0F)
                .texture(h, f2)
                .color(i);
    }

    private void onPlane(
            BufferBuilder bufferBuilder,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            int i) {
        float f3 = f * h - g * f2;
        float g3 = f * f2 + g * h;
        bufferBuilder
                .vertex(
                        this.forwardX * 90.0F + this.rightX * f3 - this.upX * g3,
                        this.forwardY * 90.0F - this.upY * g3,
                        this.forwardZ * 90.0F + this.rightZ * f3 - this.upZ * g3)
                .texture(g2, h2)
                .color(i);
    }

    private static SkyTexture plain() {
        if (plain == null) {
            int[] ints = new int[4];
            Arrays.fill(ints, -1);
            plain = SkyTexture.upload("plain", 2, 2, ints);
        }
        return plain;
    }

    private static SkyTexture halo() {
        if (halo == null) {
            int[] ints = new int[16384];
            float f = 64.0F;
            for (int i = 0; i < 128; i++) {
                for (int j = 0; j < 128; j++) {
                    float g = Math.min(1.0F, (float) Math.hypot(j + 0.5F - f, i + 0.5F - f) / f);
                    float h = 1.0F - g;
                    float f2 = h * h;
                    ints[i * 128 + j] =
                            ColorHelper.withAlpha(
                                    MathHelper.clamp(0.85F * f2 * f2 + 0.15F * h, 0.0F, 1.0F),
                                    16777215);
                }
            }
            halo = SkyTexture.upload("halo", 128, 128, ints);
        }
        return halo;
    }

    @Environment(EnvType.CLIENT)
    private static final class Run {

        SkyTexture texture;

        boolean additive;

        int first;

        int count;

        private Run() {}
    }
}
