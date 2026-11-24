package net.efn.client.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RawProjectionMatrix;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.tick.TickManager;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
final class ShaderEspPass {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final RenderPipeline PIPELINE =
            RenderPipeline.builder()
                    .withLocation(Identifier.of("efn", "pipeline/glow_esp"))
                    .withVertexShader("core/screenquad")
                    .withFragmentShader(Identifier.of("efn", "core/glow_esp"))
                    .withSampler("SilhouetteSampler")
                    .withSampler("HaloSampler")
                    .withUniform("GlowEspConfig", UniformType.UNIFORM_BUFFER)
                    .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
                    .withColorWrite(true, false)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
                    .build();

    private static final Supplier<String> PASS_LABEL = () -> "EFN glow esp";

    private static final Supplier<String> CONFIG_LABEL = () -> "EFN glow esp config";

    private static final int CONFIG_SIZE = 32;

    private static final CameraRenderState CAMERA = new CameraRenderState();

    private static final MatrixStack POSE = new MatrixStack();

    private static final Map<Entity, EntityRenderState> KEPT = new IdentityHashMap<>();

    private static ShaderEspPass.Rig rig;

    private static RawProjectionMatrix projectionBuffer;

    private static SimpleFramebuffer mask;

    private static AuraBlur blur;

    private static GpuBuffer config;

    private static GpuBufferSlice configSlice;

    private static ByteBuffer configData;

    private static long keptFrame = -9223372036854775808L;

    private static boolean invalidShaderLogged;

    private ShaderEspPass() {}

    static void keep(Entity entity, EntityRenderState entityRenderState, long l) {
        if (keptFrame != l) {
            KEPT.clear();
            keptFrame = l;
        }
        KEPT.put(entity, entityRenderState);
    }

    static void dropKept() {
        KEPT.clear();
        keptFrame = -9223372036854775808L;
    }

    static boolean paint(
            Camera camera,
            Matrix4f matrix,
            Matrix4f matrix2,
            Entity[] entities,
            int[] ints,
            int i,
            long l) {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer framebuffer = client.getFramebuffer();
        if (i > 0
                && client.world != null
                && client.isFinishedLoading()
                && !client.gameRenderer.isRenderingPanorama()
                && framebuffer.getColorAttachment() != null
                && framebuffer.textureWidth > 0
                && framebuffer.textureHeight > 0) {
            GpuDevice device = RenderSystem.getDevice();
            if (!shaderReady(device)) {
                return false;
            } else {
                if (mask == null) {
                    allocate(framebuffer.textureWidth, framebuffer.textureHeight);
                } else if (mask.textureWidth != framebuffer.textureWidth
                        || mask.textureHeight != framebuffer.textureHeight) {
                    mask.resize(framebuffer.textureWidth, framebuffer.textureHeight);
                }
                if (rig == null) {
                    rig = new ShaderEspPass.Rig(client);
                }
                if (projectionBuffer == null) {
                    projectionBuffer = new RawProjectionMatrix("efn glow esp");
                }
                device.createCommandEncoder().clearColorTexture(mask.getColorAttachment(), 0);
                submit(client, camera, entities, ints, i, l);
                flush(matrix, matrix2);
                return true;
            }
        } else {
            return false;
        }
    }

    private static void submit(
            MinecraftClient client, Camera camera, Entity[] entities, int[] ints, int i, long l) {
        EntityRenderManager entityRenderDispatcher = client.getEntityRenderDispatcher();
        RenderTickCounter renderTickCounter = client.getRenderTickCounter();
        TickManager tickManager = client.world.getTickManager();
        Vec3d cameraPos = camera.getCameraPos();
        Entity focusedEntity = camera.getFocusedEntity();
        boolean bl = keptFrame == l;
        CAMERA.initialized = camera.isReady();
        CAMERA.pos = cameraPos;
        CAMERA.blockPos = camera.getBlockPos();
        CAMERA.entityPos =
                focusedEntity == null
                        ? cameraPos
                        : focusedEntity.getLerpedPos(renderTickCounter.getTickProgress(true));
        CAMERA.orientation.set(camera.getRotation());
        for (int j = 0; j < i; j++) {
            Entity entity = entities[j];
            if (entity != null && !entity.isRemoved()) {
                EntityRenderState entityRenderState = bl ? KEPT.get(entity) : null;
                if (entityRenderState == null) {
                    entityRenderState =
                            entityRenderDispatcher.getAndUpdateRenderState(
                                    entity,
                                    renderTickCounter.getTickProgress(
                                            !tickManager.shouldSkipTick(entity)));
                }
                silhouetteOnly(entityRenderState);
                entityRenderState.outlineColor = ints[j] | 0xFF000000;
                entityRenderDispatcher.render(
                        entityRenderState,
                        CAMERA,
                        entityRenderState.x - cameraPos.x,
                        entityRenderState.y - cameraPos.y,
                        entityRenderState.z - cameraPos.z,
                        POSE,
                        rig.storage);
            }
        }
    }

    private static void silhouetteOnly(EntityRenderState entityRenderState) {
        entityRenderState.displayName = null;
        entityRenderState.leashDatas = null;
        entityRenderState.onFire = false;
        entityRenderState.shadowPieces.clear();
        if (entityRenderState instanceof PlayerEntityRenderState playerEntityRenderState) {
            playerEntityRenderState.playerName = null;
        }
    }

    private static void flush(Matrix4f matrix, Matrix4f matrix2) {
        GpuTextureView outputColorTextureOverride = RenderSystem.outputColorTextureOverride;
        GpuTextureView outputDepthTextureOverride = RenderSystem.outputDepthTextureOverride;
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionBuffer.set(matrix2), ProjectionType.PERSPECTIVE);
        modelViewStack.pushMatrix();
        modelViewStack.mul(matrix);
        RenderSystem.outputColorTextureOverride = mask.getColorAttachmentView();
        RenderSystem.outputDepthTextureOverride = null;
        try {
            rig.features.render();
            rig.outlines.draw();
        } finally {
            RenderSystem.outputColorTextureOverride = outputColorTextureOverride;
            RenderSystem.outputDepthTextureOverride = outputDepthTextureOverride;
            modelViewStack.popMatrix();
            RenderSystem.restoreProjectionMatrix();
            rig.storage.clear();
        }
        rig.storage.onNextFrame();
    }

    static void compose(float f, float g, float h, float f2, float g2, int i, float h2) {
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        GpuTextureView gpuTextureView =
                blur.spread(
                        commandEncoder,
                        mask.getColorAttachmentView(),
                        mask.textureWidth,
                        mask.textureHeight,
                        Math.max(h, f + 2.0F));
        configData.putFloat(0, f);
        configData.putFloat(4, g);
        configData.putFloat(8, f2);
        configData.putFloat(12, g2);
        configData.putFloat(16, (i >> 16 & 0xFF) / 255.0F);
        configData.putFloat(20, (i >> 8 & 0xFF) / 255.0F);
        configData.putFloat(24, (i & 0xFF) / 255.0F);
        configData.putFloat(28, h2);
        commandEncoder.writeToBuffer(configSlice, configData);
        try (RenderPass renderPass =
                commandEncoder.createRenderPass(
                        PASS_LABEL, framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
            renderPass.setPipeline(PIPELINE);
            renderPass.setUniform("GlowEspConfig", configSlice);
            renderPass.bindTexture(
                    "SilhouetteSampler",
                    mask.getColorAttachmentView(),
                    RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture(
                    "HaloSampler",
                    gpuTextureView,
                    RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.draw(0, 3);
        }
    }

    private static boolean shaderReady(GpuDevice gpuDevice) {
        if (gpuDevice.precompilePipeline(PIPELINE).isValid() && AuraBlur.compiled(gpuDevice)) {
            invalidShaderLogged = false;
            return true;
        } else {
            if (!invalidShaderLogged) {
                invalidShaderLogged = true;
                LOGGER.warn("Glow ESP shaders did not compile; entities are left without outlines");
            }
            return false;
        }
    }

    private static void allocate(int i, int j) {
        mask = new SimpleFramebuffer("EFN glow esp mask", i, j, false);
        blur = new AuraBlur("EFN glow esp aura");
        config = RenderSystem.getDevice().createBuffer(CONFIG_LABEL, 136, 32L);
        configSlice = config.slice();
        configData = BufferUtils.createByteBuffer(32);
    }

    static void release() {
        dropKept();
        if (rig != null) {
            rig.close();
            rig = null;
        }
        if (mask != null) {
            mask.delete();
            mask = null;
        }
        if (blur != null) {
            blur.release();
            blur = null;
        }
        if (config != null) {
            config.close();
            config = null;
        }
        if (projectionBuffer != null) {
            projectionBuffer.close();
            projectionBuffer = null;
        }
        configSlice = null;
        configData = null;
    }

    @Environment(EnvType.CLIENT)
    private static final class Drain extends Immediate {

        private final Map<RenderLayer, VertexConsumer> sinks = new IdentityHashMap<>();

        Drain() {
            super(new BufferAllocator(64), new LinkedHashMap<>());
        }

        void close() {
            this.allocator.close();
        }

        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            return this.sinks.computeIfAbsent(layer, renderLayer -> new ShaderEspPass.Nowhere());
        }

        @Override
        public void drawCurrentLayer() {}

        @Override
        public void draw() {}

        @Override
        public void draw(RenderLayer layer) {}
    }

    @Environment(EnvType.CLIENT)
    private static final class Nowhere implements VertexConsumer {

        private Nowhere() {}

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            return this;
        }

        @Override
        public void vertex(
                float x,
                float y,
                float z,
                int color,
                float u,
                float v,
                int overlay,
                int light,
                float normalX,
                float normalY,
                float normalZ) {}

        @Override
        public void quad(
                Entry matrixEntry,
                BakedQuad quad,
                float red,
                float green,
                float blue,
                float alpha,
                int light,
                int overlay) {}

        @Override
        public void quad(
                Entry matrixEntry,
                BakedQuad quad,
                float[] brightnesses,
                float red,
                float green,
                float blue,
                float alpha,
                int[] lights,
                int overlay) {}

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer color(int argb) {
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer lineWidth(float width) {
            return this;
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Rig {

        final OrderedRenderCommandQueueImpl storage = new OrderedRenderCommandQueueImpl();

        final ShaderEspPass.Drain drain = new ShaderEspPass.Drain();

        final OutlineVertexConsumerProvider outlines;

        final RenderDispatcher features;

        Rig(MinecraftClient client) {
            this.outlines = client.getBufferBuilders().getOutlineVertexConsumers();
            this.features =
                    new RenderDispatcher(
                            this.storage,
                            client.getBlockRenderManager(),
                            this.drain,
                            client.getAtlasManager(),
                            this.outlines,
                            this.drain,
                            client.textRenderer);
        }

        void close() {
            this.features.close();
            this.drain.close();
        }
    }
}
