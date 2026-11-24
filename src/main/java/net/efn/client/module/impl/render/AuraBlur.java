package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

import net.efn.client.module.impl.client.PerformanceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
final class AuraBlur {

    private static final RenderPipeline PIPELINE =
            RenderPipeline.builder()
                    .withLocation(Identifier.of("efn", "pipeline/aura_spread"))
                    .withVertexShader("core/screenquad")
                    .withFragmentShader(Identifier.of("efn", "core/aura_spread"))
                    .withSampler("InSampler")
                    .withUniform("AuraSpread", UniformType.UNIFORM_BUFFER)
                    .withoutBlend()
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                    .build();

    private static final int CONFIG_SIZE = 16;

    private static final float TAPS = 6.0F;

    private final String name;

    private final Supplier<String> passLabel;

    private SimpleFramebuffer across;

    private SimpleFramebuffer down;

    private GpuBuffer acrossConfig;

    private GpuBuffer downConfig;

    private GpuBufferSlice acrossSlice;

    private GpuBufferSlice downSlice;

    private ByteBuffer configData;

    AuraBlur(String name2) {
        this.name = name2;
        this.passLabel = () -> name2 + " spread";
    }

    static boolean compiled(GpuDevice gpuDevice) {
        return gpuDevice.precompilePipeline(PIPELINE).isValid();
    }

    GpuTextureView spread(
            CommandEncoder commandEncoder, GpuTextureView gpuTextureView, int i, int j, float f) {
        int k = PerformanceModule.optimizationsEnabled() && f >= 8.0F ? 4 : 2;
        int m = Math.max(1, (i + k - 1) / k);
        int n = Math.max(1, (j + k - 1) / k);
        if (this.across == null) {
            this.allocate(m, n);
        } else if (this.across.textureWidth != m || this.across.textureHeight != n) {
            this.across.resize(m, n);
            this.down.resize(m, n);
        }
        float g = Math.max(0.75F, f / 6.0F);
        this.run(
                commandEncoder,
                gpuTextureView,
                this.across,
                this.acrossSlice,
                g / i,
                0.0F,
                PIPELINE);
        this.run(
                commandEncoder,
                this.across.getColorAttachmentView(),
                this.down,
                this.downSlice,
                0.0F,
                g / j,
                PIPELINE);
        return this.down.getColorAttachmentView();
    }

    private void run(
            CommandEncoder commandEncoder,
            GpuTextureView gpuTextureView,
            SimpleFramebuffer simpleFramebuffer,
            GpuBufferSlice gpuBufferSlice,
            float f,
            float g,
            RenderPipeline pipeline) {
        this.configData.putFloat(0, f);
        this.configData.putFloat(4, g);
        commandEncoder.writeToBuffer(gpuBufferSlice, this.configData);
        try (RenderPass renderPass =
                commandEncoder.createRenderPass(
                        this.passLabel,
                        simpleFramebuffer.getColorAttachmentView(),
                        OptionalInt.empty())) {
            renderPass.setPipeline(pipeline);
            renderPass.setUniform("AuraSpread", gpuBufferSlice);
            renderPass.bindTexture(
                    "InSampler",
                    gpuTextureView,
                    RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.draw(0, 3);
        }
    }

    private void allocate(int i, int j) {
        GpuDevice device = RenderSystem.getDevice();
        this.across = new SimpleFramebuffer(this.name + " across", i, j, false);
        this.down = new SimpleFramebuffer(this.name + " down", i, j, false);
        this.acrossConfig = device.createBuffer(() -> this.name + " across config", 136, 16L);
        this.downConfig = device.createBuffer(() -> this.name + " down config", 136, 16L);
        this.acrossSlice = this.acrossConfig.slice();
        this.downSlice = this.downConfig.slice();
        this.configData = BufferUtils.createByteBuffer(16);
    }

    void release() {
        if (this.across != null) {
            this.across.delete();
            this.across = null;
        }
        if (this.down != null) {
            this.down.delete();
            this.down = null;
        }
        if (this.acrossConfig != null) {
            this.acrossConfig.close();
            this.acrossConfig = null;
        }
        if (this.downConfig != null) {
            this.downConfig.close();
            this.downConfig = null;
        }
        this.acrossSlice = null;
        this.downSlice = null;
        this.configData = null;
    }
}
