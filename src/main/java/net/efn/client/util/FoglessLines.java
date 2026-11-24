package net.efn.client.util;

import com.example.client.mixin.AccessorGameRenderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public final class FoglessLines {

    private static final double NEAR = 0.05;

    private final Vec3d eye;

    private final Vector3fc forward;

    private final double pixelPerDepth;

    private FoglessLines(Vec3d eye2, Vector3fc forward2, double pixelPerDepth2) {
        this.eye = eye2;
        this.forward = forward2;
        this.pixelPerDepth = pixelPerDepth2;
    }

    public static FoglessLines forFrame(float f) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        return new FoglessLines(camera.getCameraPos(), camera.getHorizontalPlane(), pixelScale(f));
    }

    public static double pixelScale(float f) {
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        float efn$getFov = ((AccessorGameRenderer) client.gameRenderer).efn$getFov(camera, f, true);
        return 2.0
                * Math.tan(Math.toRadians(efn$getFov) * 0.5)
                / Math.max(1, client.getWindow().getFramebufferHeight());
    }

    public static FoglessLines forView(Vec3d vec, Vector3fc vector3fc, double d) {
        return new FoglessLines(vec, vector3fc, d);
    }

    public void line(Vec3d vec, Vec3d vec2, int i, float f) {
        this.line(vec.x, vec.y, vec.z, vec2.x, vec2.y, vec2.z, i, f);
    }

    public void line(
            double d, double e, double d2, double e2, double d3, double e3, int i, float f) {
        double depth = this.depth(d, e, d2);
        double depth2 = this.depth(e2, d3, e3);
        if (!(depth < 0.05) || !(depth2 < 0.05)) {
            if (depth < 0.05) {
                double d4 = (0.05 - depth) / (depth2 - depth);
                d += (e2 - d) * d4;
                e += (d3 - e) * d4;
                d2 += (e3 - d2) * d4;
                depth = 0.05;
            } else if (depth2 < 0.05) {
                double e4 = (0.05 - depth2) / (depth - depth2);
                e2 += (d - e2) * e4;
                d3 += (e - d3) * e4;
                e3 += (d2 - e3) * e4;
                depth2 = 0.05;
            }
            double d5 = d - this.eye.x;
            double e5 = e - this.eye.y;
            double d6 = d2 - this.eye.z;
            double e6 = e2 - d;
            double d7 = d3 - e;
            double e7 = e3 - d2;
            double d8 = e5 * e7 - d6 * d7;
            double e8 = d6 * e6 - d5 * e7;
            double d9 = d5 * d7 - e5 * e6;
            double e9 = Math.sqrt(d8 * d8 + e8 * e8 + d9 * d9);
            if (!(e9 < 1.0E-9)) {
                double d10 = f * 0.5 * this.pixelPerDepth / e9;
                double e10 = d10 * depth;
                double d11 = d10 * depth2;
                WorldQuads.layer(true)
                        .quad(
                                d + d8 * e10,
                                e + e8 * e10,
                                d2 + d9 * e10,
                                e2 + d8 * d11,
                                d3 + e8 * d11,
                                e3 + d9 * d11,
                                e2 - d8 * d11,
                                d3 - e8 * d11,
                                e3 - d9 * d11,
                                d - d8 * e10,
                                e - e8 * e10,
                                d2 - d9 * e10,
                                i);
            }
        }
    }

    public void box(Box box, int i, float f) {
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;
        for (double d : new double[] {minY, maxY}) {
            this.line(new Vec3d(minX, d, minZ), new Vec3d(maxX, d, minZ), i, f);
            this.line(new Vec3d(maxX, d, minZ), new Vec3d(maxX, d, maxZ), i, f);
            this.line(new Vec3d(maxX, d, maxZ), new Vec3d(minX, d, maxZ), i, f);
            this.line(new Vec3d(minX, d, maxZ), new Vec3d(minX, d, minZ), i, f);
        }
        this.line(new Vec3d(minX, minY, minZ), new Vec3d(minX, maxY, minZ), i, f);
        this.line(new Vec3d(maxX, minY, minZ), new Vec3d(maxX, maxY, minZ), i, f);
        this.line(new Vec3d(maxX, minY, maxZ), new Vec3d(maxX, maxY, maxZ), i, f);
        this.line(new Vec3d(minX, minY, maxZ), new Vec3d(minX, maxY, maxZ), i, f);
    }

    private double depth(double d, double e, double d2) {
        return (d - this.eye.x) * this.forward.x()
                + (e - this.eye.y) * this.forward.y()
                + (d2 - this.eye.z) * this.forward.z();
    }
}
