package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

import net.efn.client.util.ShaderOverlay;
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
final class HandAuraPass {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final RenderPipeline PIPELINE =
            RenderPipeline.builder()
                    .withLocation(Identifier.of("efn", "pipeline/hand_aura"))
                    .withVertexShader("core/screenquad")
                    .withFragmentShader(Identifier.of("efn", "core/hand_aura"))
                    .withSampler("CaptureSampler")
                    .withSampler("HaloSampler")
                    .withUniform("HandAuraConfig", UniformType.UNIFORM_BUFFER)
                    .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
                    .withColorWrite(true, false)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                    .build();

    private static final Supplier<String> PASS_LABEL = () -> "EFN hand aura";

    private static final Supplier<String> CONFIG_LABEL = () -> "EFN hand aura config";

    private static final int CONFIG_SIZE = 48;

    private static SimpleFramebuffer hand;

    private static AuraBlur blur;

    private static GpuBuffer config;

    private static GpuBufferSlice configSlice;

    private static ByteBuffer configData;

    private static boolean capturing;

    private static boolean invalidShaderLogged;

    private HandAuraPass() {}

    static boolean begin() {
        if (!capturing
                && RenderSystem.outputColorTextureOverride == null
                && RenderSystem.outputDepthTextureOverride == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            Framebuffer framebuffer = client.getFramebuffer();
            if (client.world != null
                    && !client.gameRenderer.isRenderingPanorama()
                    && framebuffer.getColorAttachment() != null
                    && framebuffer.textureWidth > 0
                    && framebuffer.textureHeight > 0
                    && !ShaderOverlay.shaderPackInUse()) {
                GpuDevice device = RenderSystem.getDevice();
                if (!shaderReady(device)) {
                    return false;
                } else {
                    if (hand == null) {
                        allocate(framebuffer.textureWidth, framebuffer.textureHeight);
                    } else if (hand.textureWidth != framebuffer.textureWidth
                            || hand.textureHeight != framebuffer.textureHeight) {
                        hand.resize(framebuffer.textureWidth, framebuffer.textureHeight);
                    }
                    device.createCommandEncoder()
                            .clearColorAndDepthTextures(
                                    hand.getColorAttachment(), 0, hand.getDepthAttachment(), 1.0);
                    RenderSystem.outputColorTextureOverride = hand.getColorAttachmentView();
                    RenderSystem.outputDepthTextureOverride = hand.getDepthAttachmentView();
                    capturing = true;
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    static boolean end() {
        if (!capturing) {
            return false;
        } else {
            capturing = false;
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            return true;
        }
    }

    static void compose(int i, float f, float g, float h, float f2, int j, float g2, float h2) {
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        GpuTextureView gpuTextureView =
                f > 0.0F
                        ? blur.spread(
                                commandEncoder,
                                hand.getColorAttachmentView(),
                                hand.textureWidth,
                                hand.textureHeight,
                                g)
                        : hand.getColorAttachmentView();
        putColor(0, i, f);
        putColor(16, j, g2);
        configData.putFloat(32, h);
        configData.putFloat(36, f2);
        configData.putFloat(40, h2);
        configData.putFloat(44, 0.0F);
        commandEncoder.writeToBuffer(configSlice, configData);
        try (RenderPass renderPass =
                commandEncoder.createRenderPass(
                        PASS_LABEL, framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
            renderPass.setPipeline(PIPELINE);
            renderPass.setUniform("HandAuraConfig", configSlice);
            renderPass.bindTexture(
                    "CaptureSampler",
                    hand.getColorAttachmentView(),
                    RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture(
                    "HaloSampler",
                    gpuTextureView,
                    RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.draw(0, 3);
        }
    }

    private static void putColor(int i, int j, float f) {
        configData.putFloat(i, (j >> 16 & 0xFF) / 255.0F);
        configData.putFloat(i + 4, (j >> 8 & 0xFF) / 255.0F);
        configData.putFloat(i + 8, (j & 0xFF) / 255.0F);
        configData.putFloat(i + 12, f);
    }

    private static boolean shaderReady(GpuDevice gpuDevice) {
        if (gpuDevice.precompilePipeline(PIPELINE).isValid() && AuraBlur.compiled(gpuDevice)) {
            invalidShaderLogged = false;
            return true;
        } else {
            if (!invalidShaderLogged) {
                invalidShaderLogged = true;
                LOGGER.warn("Hand Aura shaders did not compile; the hand is drawn as usual");
            }
            return false;
        }
    }

    private static void allocate(int i, int j) {
        hand = new SimpleFramebuffer("EFN hand aura", i, j, true);
        blur = new AuraBlur("EFN hand aura");
        config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 48L);
        configSlice = config.slice();
        configData = BufferUtils.createByteBuffer(48);
    }

    static void release() {
        if (capturing) {
            end();
        }
        if (hand != null) {
            hand.delete();
            hand = null;
        }
        if (blur != null) {
            blur.release();
            blur = null;
        }
        if (config != null) {
            config.close();
            config = null;
        }
        configSlice = null;
        configData = null;
    }
}
