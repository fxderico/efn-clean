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
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
final class ColorGradingPass {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final RenderPipeline PIPELINE =
            RenderPipeline.builder()
                    .withLocation(Identifier.of("efn", "pipeline/color_grading"))
                    .withVertexShader("core/screenquad")
                    .withFragmentShader(Identifier.of("efn", "core/color_grading"))
                    .withSampler("InSampler")
                    .withUniform("ColorGradingConfig", UniformType.UNIFORM_BUFFER)
                    .withoutBlend()
                    .withColorWrite(true, false)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                    .build();

    private static final Supplier<String> PASS_LABEL = () -> "EFN color grading";

    private static final Supplier<String> CONFIG_LABEL = () -> "EFN color grading config";

    private static final int CONFIG_SIZE = 32;

    private static SimpleFramebuffer copy;

    private static GpuBuffer config;

    private static GpuBufferSlice configSlice;

    private static ByteBuffer configData;

    private static boolean invalidShaderLogged;

    private ColorGradingPass() {}

    static void apply(boolean bl, float f, float g, float h, float f2, float g2) {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer framebuffer = client.getFramebuffer();
        GpuTexture colorAttachment = framebuffer.getColorAttachment();
        if (bl
                && client.world != null
                && client.isFinishedLoading()
                && !client.gameRenderer.isRenderingPanorama()
                && colorAttachment != null
                && framebuffer.textureWidth > 0
                && framebuffer.textureHeight > 0) {
            if (copy == null) {
                allocate(framebuffer.textureWidth, framebuffer.textureHeight);
            } else if (copy.textureWidth != framebuffer.textureWidth
                    || copy.textureHeight != framebuffer.textureHeight) {
                copy.resize(framebuffer.textureWidth, framebuffer.textureHeight);
            }
            GpuDevice device = RenderSystem.getDevice();
            if (shaderReady(device)) {
                CommandEncoder commandEncoder = device.createCommandEncoder();
                commandEncoder.copyTextureToTexture(
                        colorAttachment,
                        copy.getColorAttachment(),
                        0,
                        0,
                        0,
                        0,
                        0,
                        framebuffer.textureWidth,
                        framebuffer.textureHeight);
                configData.putFloat(0, f);
                configData.putFloat(4, g);
                configData.putFloat(8, h);
                configData.putFloat(12, f2);
                configData.putFloat(16, g2);
                commandEncoder.writeToBuffer(configSlice, configData);
                try (RenderPass renderPass =
                        commandEncoder.createRenderPass(
                                PASS_LABEL,
                                framebuffer.getColorAttachmentView(),
                                OptionalInt.empty())) {
                    renderPass.setPipeline(PIPELINE);
                    renderPass.setUniform("ColorGradingConfig", configSlice);
                    renderPass.bindTexture(
                            "InSampler",
                            copy.getColorAttachmentView(),
                            RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                    renderPass.draw(0, 3);
                }
            }
        }
    }

    private static boolean shaderReady(GpuDevice gpuDevice) {
        if (gpuDevice.precompilePipeline(PIPELINE).isValid()) {
            invalidShaderLogged = false;
            return true;
        } else {
            if (!invalidShaderLogged) {
                invalidShaderLogged = true;
                LOGGER.warn(
                        "Color Grading shader {} did not compile; the picture is left as it is",
                        PIPELINE.getFragmentShader());
            }
            return false;
        }
    }

    private static void allocate(int i, int j) {
        copy = new SimpleFramebuffer("EFN color grading copy", i, j, false);
        config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 32L);
        configSlice = config.slice();
        configData = BufferUtils.createByteBuffer(32);
    }

    static void release() {
        if (copy != null) {
            copy.delete();
            copy = null;
        }
        if (config != null) {
            config.close();
            config = null;
        }
        configSlice = null;
        configData = null;
    }
}
