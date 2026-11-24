package net.efn.client.util.glyph;

import net.efn.client.util.WorldQuads;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.Gizmo;
import net.minecraft.world.debug.gizmo.GizmoDrawer;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import net.minecraft.world.debug.gizmo.VisibilityConfigurable;

import org.joml.Vector3fc;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public final class GlyphCanvas implements Gizmo {

    public static final int MAX_QUADS = 12000;

    private static final int MAX_LINES = 4000;

    private static final int MIN_ALPHA = 3;

    private static final int VERTEX_BITS = 9;

    private static final double RIBBON_NEAR = 0.05;

    private final Vec3d[] vertices = new Vec3d[512];

    private double[] quadPos = new double[3072];

    private int[] quadColor = new int[256];

    private int quads;

    private double[] linePos = new double[384];

    private int[] lineColor = new int[64];

    private float[] lineWidth = new float[64];

    private int lines;

    private double camX;

    private double camY;

    private double camZ;

    private double rightX;

    private double rightY;

    private double rightZ;

    private double upX;

    private double upY;

    private double upZ;

    private double lookX;

    private double lookY;

    private double lookZ;

    public GlyphCanvas() {}

    public void begin(Camera camera) {
        this.quads = 0;
        this.lines = 0;
        Vec3d cameraPos = camera.getCameraPos();
        this.camX = cameraPos.x;
        this.camY = cameraPos.y;
        this.camZ = cameraPos.z;
        Vector3fc diagonalPlane = camera.getDiagonalPlane();
        Vector3fc verticalPlane = camera.getVerticalPlane();
        Vector3fc horizontalPlane = camera.getHorizontalPlane();
        this.rightX = -diagonalPlane.x();
        this.rightY = -diagonalPlane.y();
        this.rightZ = -diagonalPlane.z();
        this.upX = verticalPlane.x();
        this.upY = verticalPlane.y();
        this.upZ = verticalPlane.z();
        this.lookX = horizontalPlane.x();
        this.lookY = horizontalPlane.y();
        this.lookZ = horizontalPlane.z();
    }

    public void submit(boolean bl) {
        if (this.quads > 0) {
            WorldQuads.layer(bl).quads(this.quadPos, this.quadColor, this.quads);
        }
        this.quads = 0;
        if (this.lines != 0) {
            VisibilityConfigurable visibilityConfigurable = GizmoDrawing.collect(this);
            if (bl) {
                visibilityConfigurable.ignoreOcclusion();
            }
        }
    }

    public double camX() {
        return this.camX;
    }

    public double camY() {
        return this.camY;
    }

    public double camZ() {
        return this.camZ;
    }

    public double depth(double d, double e, double d2) {
        return (d - this.camX) * this.lookX
                + (e - this.camY) * this.lookY
                + (d2 - this.camZ) * this.lookZ;
    }

    public double distanceSqr(double d, double e, double d2) {
        double e2 = d - this.camX;
        double d3 = e - this.camY;
        double e3 = d2 - this.camZ;
        return e2 * e2 + d3 * d3 + e3 * e3;
    }

    public void quad(
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
        if (ColorHelper.getAlpha(i) >= 3 && this.reserveQuad()) {
            int j = this.quads * 12;
            double[] quadPos2 = this.quadPos;
            quadPos2[j] = d;
            quadPos2[j + 1] = e;
            quadPos2[j + 2] = d2;
            quadPos2[j + 3] = e2;
            quadPos2[j + 4] = d3;
            quadPos2[j + 5] = e3;
            quadPos2[j + 6] = d4;
            quadPos2[j + 7] = e4;
            quadPos2[j + 8] = d5;
            quadPos2[j + 9] = e5;
            quadPos2[j + 10] = d6;
            quadPos2[j + 11] = e6;
            this.quadColor[this.quads++] = i;
        }
    }

    public void outwardTriangle(
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
        double d7 = d3 - e;
        double e7 = d5 - d2;
        double d8 = e3 - d2;
        double e8 = e4 - e;
        double d9 = d7 * e7 - d8 * e8;
        double e9 = (d + e2 + d4) / 3.0 - e5;
        double d10 = d4 - d;
        double e10 = e2 - d;
        double d11 = d8 * d10 - e10 * e7;
        double e11 = (e + d3 + e4) / 3.0 - d6;
        double d12 = e10 * e8 - d7 * d10;
        double e12 = (d2 + e3 + d5) / 3.0 - e6;
        if (d9 * e9 + d11 * e11 + d12 * e12 >= 0.0) {
            this.quad(d, e, d2, e2, d3, e3, d4, e4, d5, d4, e4, d5, i);
        } else {
            this.quad(d, e, d2, d4, e4, d5, e2, d3, e3, e2, d3, e3, i);
        }
    }

    public void line(
            double d, double e, double d2, double e2, double d3, double e3, int i, float f) {
        if (ColorHelper.getAlpha(i) >= 3 && this.reserveLine()) {
            int j = this.lines * 6;
            double[] linePos2 = this.linePos;
            linePos2[j] = d;
            linePos2[j + 1] = e;
            linePos2[j + 2] = d2;
            linePos2[j + 3] = e2;
            linePos2[j + 4] = d3;
            linePos2[j + 5] = e3;
            this.lineColor[this.lines] = i;
            this.lineWidth[this.lines++] = f;
        }
    }

    public void ribbon(
            double d,
            double e,
            double d2,
            double e2,
            double d3,
            double e3,
            int i,
            float f,
            double d4) {
        double depth = this.depth(d, e, d2);
        double depth2 = this.depth(e2, d3, e3);
        if (!(depth < 0.05) || !(depth2 < 0.05)) {
            if (depth < 0.05) {
                double e4 = (0.05 - depth) / (depth2 - depth);
                d += (e2 - d) * e4;
                e += (d3 - e) * e4;
                d2 += (e3 - d2) * e4;
                depth = 0.05;
            } else if (depth2 < 0.05) {
                double d5 = (0.05 - depth2) / (depth - depth2);
                e2 += (d - e2) * d5;
                d3 += (e - d3) * d5;
                e3 += (d2 - e3) * d5;
                depth2 = 0.05;
            }
            double e5 = d - this.camX;
            double d6 = e - this.camY;
            double e6 = d2 - this.camZ;
            double d7 = e2 - d;
            double e7 = d3 - e;
            double d8 = e3 - d2;
            double e8 = d6 * d8 - e6 * e7;
            double d9 = e6 * d7 - e5 * d8;
            double e9 = e5 * e7 - d6 * d7;
            double d10 = Math.sqrt(e8 * e8 + d9 * d9 + e9 * e9);
            if (!(d10 < 1.0E-9)) {
                double e10 = f * 0.5 * d4 / d10;
                double d11 = e10 * depth;
                double e11 = e10 * depth2;
                this.quad(
                        d + e8 * d11,
                        e + d9 * d11,
                        d2 + e9 * d11,
                        e2 + e8 * e11,
                        d3 + d9 * e11,
                        e3 + e9 * e11,
                        e2 - e8 * e11,
                        d3 - d9 * e11,
                        e3 - e9 * e11,
                        d - e8 * d11,
                        e - d9 * d11,
                        d2 - e9 * d11,
                        i);
            }
        }
    }

    public void shape(
            float[] floats,
            double d,
            double e,
            double d2,
            double e2,
            double d3,
            double e3,
            double d4,
            double e4,
            double d5,
            int i) {
        if (ColorHelper.getAlpha(i) >= 3) {
            for (int n = 0; n + 7 < floats.length; n += 8) {
                float f = floats[n];
                float g = floats[n + 1];
                float h = floats[n + 2];
                float f2 = floats[n + 3];
                float g2 = floats[n + 4];
                float h2 = floats[n + 5];
                float f3 = floats[n + 6];
                float g3 = floats[n + 7];
                this.quad(
                        d + e2 * f + d4 * g,
                        e + d3 * f + e4 * g,
                        d2 + e3 * f + d5 * g,
                        d + e2 * h + d4 * f2,
                        e + d3 * h + e4 * f2,
                        d2 + e3 * h + d5 * f2,
                        d + e2 * g2 + d4 * h2,
                        e + d3 * g2 + e4 * h2,
                        d2 + e3 * g2 + d5 * h2,
                        d + e2 * f3 + d4 * g3,
                        e + d3 * f3 + e4 * g3,
                        d2 + e3 * f3 + d5 * g3,
                        i);
            }
        }
    }

    public void sprite(float[] floats, double d, double e, double d2, float f, float g, int i) {
        this.sprite(floats, d, e, d2, f, f, g, i);
    }

    public void sprite(
            float[] floats, double d, double e, double d2, float f, float g, float h, int i) {
        this.sprite(floats, d, e, d2, f, g, h, 0.0F, 0.0F, i);
    }

    public void sprite(
            float[] floats,
            double d,
            double e,
            double d2,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            int i) {
        if (ColorHelper.getAlpha(i) >= 3) {
            float h2 = MathHelper.cos(h);
            float f3 = MathHelper.sin(h);
            double e2 = (this.rightX * h2 + this.upX * f3) * f;
            double d3 = (this.rightY * h2 + this.upY * f3) * f;
            double e3 = (this.rightZ * h2 + this.upZ * f3) * f;
            double d4 = (this.upX * h2 - this.rightX * f3) * g;
            double e4 = (this.upY * h2 - this.rightY * f3) * g;
            double d5 = (this.upZ * h2 - this.rightZ * f3) * g;
            this.shape(
                    floats,
                    d + e2 * f2 + d4 * g2,
                    e + d3 * f2 + e4 * g2,
                    d2 + e3 * f2 + d5 * g2,
                    e2,
                    d3,
                    e3,
                    d4,
                    e4,
                    d5,
                    i);
        }
    }

    public void flat(float[] floats, double d, double e, double d2, float f, float g, int i) {
        float h = MathHelper.cos(g) * f;
        float f2 = MathHelper.sin(g) * f;
        double e2 = this.camY >= e ? -1.0 : 1.0;
        this.shape(floats, d, e, d2, h, 0.0, e2 * f2, -f2, 0.0, e2 * h, i);
    }

    public void band(
            double d, double e, double d2, float f, float g, float h, float f2, int i, int j) {
        if (!(g <= f) && i > 0 && ColorHelper.getAlpha(j) >= 3) {
            double e2 = this.camY >= e ? -1.0 : 1.0;
            float g2 = (f2 - h) / i;
            float h2 = MathHelper.cos(h);
            float f3 = MathHelper.sin(h);
            for (int n = 1; n <= i; n++) {
                float g3 = h + g2 * n;
                float h3 = MathHelper.cos(g3);
                float f4 = MathHelper.sin(g3);
                this.quad(
                        d + h2 * f,
                        e,
                        d2 + e2 * f3 * f,
                        d + h2 * g,
                        e,
                        d2 + e2 * f3 * g,
                        d + h3 * g,
                        e,
                        d2 + e2 * f4 * g,
                        d + h3 * f,
                        e,
                        d2 + e2 * f4 * f,
                        j);
                h2 = h3;
                f3 = f4;
            }
        }
    }

    public void ring(double d, double e, double d2, float f, float g, int i, int j) {
        this.band(d, e, d2, f, g, 0.0F, 6.2831855F, i, j);
    }

    @Override
    public void draw(GizmoDrawer consumer, float opacity) {
        boolean bl = opacity < 1.0F;
        double[] linePos2 = this.linePos;
        int i = 0;
        for (int j = 0; i < this.lines; j += 6) {
            int k = bl ? ColorHelper.scaleAlpha(this.lineColor[i], opacity) : this.lineColor[i];
            consumer.addLine(
                    this.vertex(linePos2[j], linePos2[j + 1], linePos2[j + 2]),
                    this.vertex(linePos2[j + 3], linePos2[j + 4], linePos2[j + 5]),
                    k,
                    this.lineWidth[i]);
            i++;
        }
    }

    private Vec3d vertex(double d, double e, double d2) {
        long l =
                Double.doubleToRawLongBits(d) * -7046029254386353131L
                        ^ Double.doubleToRawLongBits(e) * -4417276706812531889L
                        ^ Double.doubleToRawLongBits(d2) * 1609587929392839161L;
        int i = (int) (l >>> 55);
        Vec3d vertice = this.vertices[i];
        if (vertice != null && vertice.x == d && vertice.y == e && vertice.z == d2) {
            return vertice;
        } else {
            Vec3d vec;
            this.vertices[i] = vec = new Vec3d(d, e, d2);
            return vec;
        }
    }

    private boolean reserveQuad() {
        if (this.quads < this.quadColor.length) {
            return true;
        } else if (this.quads >= 12000) {
            return false;
        } else {
            int i = Math.min(12000, this.quadColor.length * 2);
            this.quadColor = Arrays.copyOf(this.quadColor, i);
            this.quadPos = Arrays.copyOf(this.quadPos, i * 12);
            return true;
        }
    }

    private boolean reserveLine() {
        if (this.lines < this.lineColor.length) {
            return true;
        } else if (this.lines >= 4000) {
            return false;
        } else {
            int i = Math.min(4000, this.lineColor.length * 2);
            this.lineColor = Arrays.copyOf(this.lineColor, i);
            this.lineWidth = Arrays.copyOf(this.lineWidth, i);
            this.linePos = Arrays.copyOf(this.linePos, i * 6);
            return true;
        }
    }
}
