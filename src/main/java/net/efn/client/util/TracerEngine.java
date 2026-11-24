package net.efn.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class TracerEngine {

    private static final float WIDTH = 1.5F;

    private static boolean active;

    private static Vec3d cameraPos = Vec3d.ZERO;

    private static Vec3d origin = Vec3d.ZERO;

    private static FoglessLines lines;

    private static final Matrix4f viewProjection = new Matrix4f();

    private TracerEngine() {}

    public static void beginFrame(Matrix4fc matrix4fc, Matrix4fc matrix4fc2) {
        active = false;
        lines = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            if (client.currentScreen == null || client.currentScreen instanceof ChatScreen) {
                Camera camera = client.gameRenderer.getCamera();
                if (camera != null && camera.isReady()) {
                    viewProjection.set(matrix4fc2).mul(matrix4fc);
                    Matrix4f matrix = viewProjection.invert(new Matrix4f());
                    Vector4f vector4f = matrix.transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
                    Vector4f vector4f2 = matrix.transform(new Vector4f(0.0F, 0.0F, -1.0F, 0.0F));
                    cameraPos = camera.getCameraPos();
                    origin =
                            cameraPos.add(
                                    vector4f.x / vector4f.w,
                                    vector4f.y / vector4f.w,
                                    vector4f.z / vector4f.w);
                    Vec3d vec =
                            cameraPos.add(
                                    vector4f2.x / vector4f2.w,
                                    vector4f2.y / vector4f2.w,
                                    vector4f2.z / vector4f2.w);
                    Vector3f vec2 =
                            new Vector3f()
                                    .set(
                                            (float) (origin.x - vec.x),
                                            (float) (origin.y - vec.y),
                                            (float) (origin.z - vec.z))
                                    .normalize();
                    double d =
                            2.0
                                    / (matrix4fc2.m11()
                                            * Math.max(
                                                    1, client.getWindow().getFramebufferHeight()));
                    lines = FoglessLines.forView(vec, vec2, d);
                    active = true;
                }
            }
        }
    }

    public static void draw(Vec3d vec, int i) {
        if (active) {
            lines.line(origin, vec, i, 1.5F);
        }
    }

    public static void stem(Vec3d vec, double d, int i) {
        if (active) {
            lines.line(vec, vec.add(0.0, d, 0.0), i, 1.5F);
        }
    }

    public static Vector4f clip(Vec3d vec) {
        return !active
                ? null
                : viewProjection.transform(
                        new Vector4f(
                                (float) (vec.x - cameraPos.x),
                                (float) (vec.y - cameraPos.y),
                                (float) (vec.z - cameraPos.z),
                                1.0F));
    }

    public static boolean isInFront(Camera camera, Vec3d vec) {
        Vec3d vec2 = vec.subtract(camera.getCameraPos());
        Vector3fc horizontalPlane = camera.getHorizontalPlane();
        return vec2.x * horizontalPlane.x()
                        + vec2.y * horizontalPlane.y()
                        + vec2.z * horizontalPlane.z()
                > 0.05;
    }
}
