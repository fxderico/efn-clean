package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Decal {

    final Canvas canvas;

    final boolean wrap;

    final float cx;

    final float cy;

    final float cz;

    final float dx;

    final float dy;

    final float dz;

    final float ux;

    final float uy;

    final float uz;

    final float rx;

    final float ry;

    final float rz;

    final float width;

    final float height;

    float near = -3.4028235E38F;

    float far = 3.4028235E38F;

    float cosLimit = (float) Math.cos(Math.toRadians(78.0));

    float offset = 0.035F;

    boolean glow;

    float flat = 0.0F / 0.0F;

    float occlusion = 1.0F;

    private Decal(
            Canvas canvas2,
            boolean wrap2,
            float cx2,
            float cy2,
            float cz2,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            float width2,
            float height2) {
        if (canvas2 == null) {
            throw new IllegalArgumentException("decal needs a canvas");
        } else if (!(width2 <= 0.0F) && !(height2 <= 0.0F)) {
            this.canvas = canvas2;
            this.wrap = wrap2;
            this.cx = cx2;
            this.cy = cy2;
            this.cz = cz2;
            float f3 = (float) Math.sqrt(f * f + g * g + h * h);
            if (f3 < 1.0E-6F) {
                throw new IllegalArgumentException("decal direction must not be zero");
            } else {
                this.dx = f / f3;
                this.dy = g / f3;
                this.dz = h / f3;
                float g3 = f2 * this.dx + g2 * this.dy + h2 * this.dz;
                float h3 = f2 - g3 * this.dx;
                float f4 = g2 - g3 * this.dy;
                float g4 = h2 - g3 * this.dz;
                float h4 = (float) Math.sqrt(h3 * h3 + f4 * f4 + g4 * g4);
                if (h4 < 1.0E-6F) {
                    throw new IllegalArgumentException(
                            "decal up/front direction must not be parallel to its axis");
                } else {
                    this.ux = h3 / h4;
                    this.uy = f4 / h4;
                    this.uz = g4 / h4;
                    if (wrap2) {
                        this.rx = this.dy * this.uz - this.dz * this.uy;
                        this.ry = this.dz * this.ux - this.dx * this.uz;
                        this.rz = this.dx * this.uy - this.dy * this.ux;
                    } else {
                        this.rx = this.uy * this.dz - this.uz * this.dy;
                        this.ry = this.uz * this.dx - this.ux * this.dz;
                        this.rz = this.ux * this.dy - this.uy * this.dx;
                    }
                    this.width = width2;
                    this.height = height2;
                }
            }
        } else {
            throw new IllegalArgumentException("decal size must be positive");
        }
    }

    public static Decal front(Canvas canvas2, float f, float g, float h, float f2, float g2) {
        return new Decal(canvas2, false, f, g, h, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, f2, g2);
    }

    public static Decal back(Canvas canvas2, float f, float g, float h, float f2, float g2) {
        return new Decal(canvas2, false, f, g, h, 0.0F, 0.0F, -1.0F, 0.0F, 1.0F, 0.0F, f2, g2);
    }

    public static Decal left(Canvas canvas2, float f, float g, float h, float f2, float g2) {
        return new Decal(canvas2, false, f, g, h, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, f2, g2);
    }

    public static Decal right(Canvas canvas2, float f, float g, float h, float f2, float g2) {
        return new Decal(canvas2, false, f, g, h, -1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, f2, g2);
    }

    public static Decal top(Canvas canvas2, float f, float g, float h, float f2, float g2) {
        return new Decal(canvas2, false, f, g, h, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F, f2, g2);
    }

    public static Decal project(
            Canvas canvas2,
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
            float g4) {
        return new Decal(canvas2, false, f, g, h, f2, g2, h2, f3, g3, h3, f4, g4);
    }

    public static Decal wrap(
            Canvas canvas2,
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
            float g4) {
        return new Decal(canvas2, true, f, g, h, f2, g2, h2, f3, g3, h3, Math.min(360.0F, f4), g4);
    }

    public Decal depth(float f, float g) {
        this.near = -Math.abs(f);
        this.far = Math.abs(g);
        return this;
    }

    public Decal radius(float near2, float far2) {
        this.near = near2;
        this.far = far2;
        return this;
    }

    public Decal maxAngle(float f) {
        this.cosLimit = (float) Math.cos(Math.toRadians(Math.max(1.0F, Math.min(89.5F, f))));
        return this;
    }

    public Decal offset(float offset2) {
        this.offset = offset2;
        return this;
    }

    public Decal glow() {
        this.glow = true;
        return this;
    }

    public Decal flatLight(float f) {
        this.flat = Math.max(0.0F, Math.min(1.0F, f));
        return this;
    }

    public Decal occlusion(float f) {
        this.occlusion = Math.max(0.0F, Math.min(1.0F, f));
        return this;
    }

    Decal mirrored() {
        Decal decal =
                this.wrap
                        ? new Decal(
                                this.canvas.mirrored(),
                                true,
                                -this.cx,
                                this.cy,
                                this.cz,
                                -this.dx,
                                this.dy,
                                this.dz,
                                -this.ux,
                                this.uy,
                                this.uz,
                                this.width,
                                this.height)
                        : new Decal(
                                this.canvas.mirrored(),
                                false,
                                -this.cx,
                                this.cy,
                                this.cz,
                                -this.dx,
                                this.dy,
                                this.dz,
                                -this.ux,
                                this.uy,
                                this.uz,
                                this.width,
                                this.height);
        decal.near = this.near;
        decal.far = this.far;
        decal.cosLimit = this.cosLimit;
        decal.offset = this.offset;
        decal.glow = this.glow;
        decal.flat = this.flat;
        decal.occlusion = this.occlusion;
        return decal;
    }
}
