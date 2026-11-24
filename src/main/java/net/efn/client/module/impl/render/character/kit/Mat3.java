package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Mat3 {

    public static final Mat3 IDENTITY =
            new Mat3(1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F);

    public final float m00;

    public final float m01;

    public final float m02;

    public final float m10;

    public final float m11;

    public final float m12;

    public final float m20;

    public final float m21;

    public final float m22;

    public Mat3(
            float m002,
            float m012,
            float m022,
            float m102,
            float m112,
            float m122,
            float m202,
            float m212,
            float m222) {
        this.m00 = m002;
        this.m01 = m012;
        this.m02 = m022;
        this.m10 = m102;
        this.m11 = m112;
        this.m12 = m122;
        this.m20 = m202;
        this.m21 = m212;
        this.m22 = m222;
    }

    public static Mat3 euler(float f, float g, float h) {
        return radians(
                (float) Math.toRadians(f), (float) Math.toRadians(g), (float) Math.toRadians(h));
    }

    public static Mat3 radians(float f, float g, float h) {
        float f2 = (float) Math.cos(f);
        float g2 = (float) Math.sin(f);
        float h2 = (float) Math.cos(g);
        float f3 = (float) Math.sin(g);
        float g3 = (float) Math.cos(h);
        float h3 = (float) Math.sin(h);
        return new Mat3(
                g3 * h2,
                g3 * f3 * g2 - h3 * f2,
                g3 * f3 * f2 + h3 * g2,
                h3 * h2,
                h3 * f3 * g2 + g3 * f2,
                h3 * f3 * f2 - g3 * g2,
                -f3,
                h2 * g2,
                h2 * f2);
    }

    public static Mat3 about(float f, float g, float h, float f2) {
        float g2 = (float) Math.sqrt(f * f + g * g + h * h);
        if (g2 < 1.0E-8F) {
            return IDENTITY;
        } else {
            float h2 = (float) Math.toRadians(f2);
            float f3 = (float) Math.cos(h2);
            float g3 = (float) Math.sin(h2);
            float h3 = 1.0F - f3;
            float f4;
            float g4;
            float h4;
            return new Mat3(
                    h3 * f * f + f3,
                    h3 * f * g - g3 * (h4 = h / g2),
                    h3 * f * h4 + g3 * (g4 = g / g2),
                    h3 * (f4 = f / g2) * g4 + g3 * h4,
                    h3 * g4 * g4 + f3,
                    h3 * g4 * h4 - g3 * f4,
                    h3 * f4 * h4 - g3 * g4,
                    h3 * g4 * h4 + g3 * f4,
                    h3 * h4 * h4 + f3);
        }
    }

    public Mat3 mul(Mat3 mat3) {
        return new Mat3(
                this.m00 * mat3.m00 + this.m01 * mat3.m10 + this.m02 * mat3.m20,
                this.m00 * mat3.m01 + this.m01 * mat3.m11 + this.m02 * mat3.m21,
                this.m00 * mat3.m02 + this.m01 * mat3.m12 + this.m02 * mat3.m22,
                this.m10 * mat3.m00 + this.m11 * mat3.m10 + this.m12 * mat3.m20,
                this.m10 * mat3.m01 + this.m11 * mat3.m11 + this.m12 * mat3.m21,
                this.m10 * mat3.m02 + this.m11 * mat3.m12 + this.m12 * mat3.m22,
                this.m20 * mat3.m00 + this.m21 * mat3.m10 + this.m22 * mat3.m20,
                this.m20 * mat3.m01 + this.m21 * mat3.m11 + this.m22 * mat3.m21,
                this.m20 * mat3.m02 + this.m21 * mat3.m12 + this.m22 * mat3.m22);
    }

    public Mat3 transpose() {
        return new Mat3(
                this.m00, this.m10, this.m20, this.m01, this.m11, this.m21, this.m02, this.m12,
                this.m22);
    }

    public Vec3 apply(Vec3 vec3) {
        return new Vec3(
                this.x(vec3.x(), vec3.y(), vec3.z()),
                this.y(vec3.x(), vec3.y(), vec3.z()),
                this.z(vec3.x(), vec3.y(), vec3.z()));
    }

    public float x(float f, float g, float h) {
        return this.m00 * f + this.m01 * g + this.m02 * h;
    }

    public float y(float f, float g, float h) {
        return this.m10 * f + this.m11 * g + this.m12 * h;
    }

    public float z(float f, float g, float h) {
        return this.m20 * f + this.m21 * g + this.m22 * h;
    }
}
