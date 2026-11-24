package net.efn.client.util;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RawProjectionMatrix;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.gizmo.GizmoDrawerImpl;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.world.debug.gizmo.GizmoCollectorImpl;
import net.minecraft.world.debug.gizmo.GizmoCollectorImpl.Entry;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.GizmoDrawing.CollectorScope;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Environment(EnvType.CLIENT)
public final class ShaderOverlay {

    private static final MethodHandle SHADER_PACK_IN_USE = irisProbe();

    private static final GizmoCollectorImpl COLLECTOR = new GizmoCollectorImpl();

    private static RawProjectionMatrix projectionBuffer;

    private ShaderOverlay() {}

    private static MethodHandle irisProbe() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return null;
        } else {
            try {
                Class classValue = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object object = classValue.getMethod("getInstance").invoke(null);
                return MethodHandles.publicLookup()
                        .findVirtual(
                                classValue,
                                "isShaderPackInUse",
                                MethodType.methodType(boolean.class))
                        .bindTo(object);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                return null;
            }
        }
    }

    public static boolean shaderPackInUse() {
        if (SHADER_PACK_IN_USE == null) {
            return false;
        } else {
            try {
                return (boolean) SHADER_PACK_IN_USE.invokeExact();
            } catch (Throwable exception) {
                return false;
            }
        }
    }

    public static CollectorScope capture() {
        if (!shaderPackInUse()) {
            return null;
        } else {
            if (pending()) {
                COLLECTOR.extractGizmos();
            }
            return GizmoDrawing.using(COLLECTOR);
        }
    }

    public static boolean pending() {
        return !COLLECTOR.getGizmos().isEmpty();
    }

    public static void render(
            Camera camera,
            Matrix4f matrix,
            Matrix4f matrix2,
            GpuBufferSlice shaderFog,
            GpuBufferSlice shaderFog2) {
        if (pending()) {
            GizmoDrawerImpl gizmoDrawerImpl = new GizmoDrawerImpl();
            GizmoDrawerImpl gizmoDrawerImpl2 = new GizmoDrawerImpl();
            long measuringTimeMs = Util.getMeasuringTimeMs();
            for (Entry gizmo : COLLECTOR.extractGizmos()) {
                gizmo.getGizmo()
                        .draw(
                                gizmo.ignoresOcclusion() ? gizmoDrawerImpl2 : gizmoDrawerImpl,
                                gizmo.getOpacity(measuringTimeMs));
            }
            if (!gizmoDrawerImpl.isEmpty() || !gizmoDrawerImpl2.isEmpty()) {
                CameraRenderState cameraRenderState = new CameraRenderState();
                cameraRenderState.initialized = camera.isReady();
                cameraRenderState.pos = camera.getCameraPos();
                cameraRenderState.blockPos = camera.getBlockPos();
                cameraRenderState.orientation = new Quaternionf(camera.getRotation());
                MinecraftClient client = MinecraftClient.getInstance();
                if (projectionBuffer == null) {
                    projectionBuffer = new RawProjectionMatrix("efn overlay");
                }
                Immediate entityVertexConsumers =
                        client.getBufferBuilders().getEntityVertexConsumers();
                MatrixStack matrices = new MatrixStack();
                Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
                GpuBufferSlice shaderFog3 = RenderSystem.getShaderFog();
                RenderSystem.backupProjectionMatrix();
                RenderSystem.setProjectionMatrix(
                        projectionBuffer.set(matrix2), ProjectionType.PERSPECTIVE);
                modelViewStack.pushMatrix();
                modelViewStack.mul(matrix);
                try {
                    if (!gizmoDrawerImpl.isEmpty()) {
                        RenderSystem.setShaderFog(shaderFog);
                        gizmoDrawerImpl.draw(
                                matrices, entityVertexConsumers, cameraRenderState, matrix);
                        entityVertexConsumers.draw();
                    }
                    if (!gizmoDrawerImpl2.isEmpty()) {
                        RenderSystem.getDevice()
                                .createCommandEncoder()
                                .clearDepthTexture(
                                        client.getFramebuffer().getDepthAttachment(), 1.0);
                        RenderSystem.setShaderFog(shaderFog2);
                        gizmoDrawerImpl2.draw(
                                matrices, entityVertexConsumers, cameraRenderState, matrix);
                        entityVertexConsumers.draw();
                    }
                } finally {
                    modelViewStack.popMatrix();
                    RenderSystem.restoreProjectionMatrix();
                    RenderSystem.setShaderFog(shaderFog3);
                }
            }
        }
    }
}
