package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Lights {

    public static final Lights WORLD = new Lights(0.2F, 1.0F, -0.7F, -0.2F, 1.0F, 0.7F);

    private static final float AMBIENT = 0.4F;

    private static final float POWER = 0.6F;

    private static final float FLAT_0 = 0.853F;

    private static final float FLAT_1 = 0.273F;

    private static final float FLAT_2 = -0.126F;

    public final float x0;

    public final float y0;

    public final float z0;

    public final float x1;

    public final float y1;

    public final float z1;

    private final float dx;

    private final float dy;

    private final float dz;

    private final boolean opposite;

    public Lights(float f, float g, float h, float f2, float g2, float h2) {
        float f3 = length(f, g, h);
        float g3 = length(f2, g2, h2);
        if (!(f3 < 1.0E-6F) && !(g3 < 1.0E-6F)) {
            this.x0 = f / f3;
            this.y0 = g / f3;
            this.z0 = h / f3;
            this.x1 = f2 / g3;
            this.y1 = g2 / g3;
            this.z1 = h2 / g3;
            float h3 = this.x0 + this.x1;
            float f4 = this.y0 + this.y1;
            float g4 = this.z0 + this.z1;
            float h4 = length(h3, f4, g4);
            this.opposite = h4 < 0.25F;
            this.dx = this.opposite ? this.x0 : h3 / h4;
            this.dy = this.opposite ? this.y0 : f4 / h4;
            this.dz = this.opposite ? this.z0 : g4 / h4;
        } else {
            throw new IllegalArgumentException("light directions must not be zero");
        }
    }

    public Lights rotateY(float f) {
        double d = Math.toRadians(f);
        float g = (float) Math.cos(d);
        float h = (float) Math.sin(d);
        return new Lights(
                g * this.x0 + h * this.z0,
                this.y0,
                g * this.z0 - h * this.x0,
                g * this.x1 + h * this.z1,
                this.y1,
                g * this.z1 - h * this.x1);
    }

    public float brightness(float f, float g, float h) {
        float f2 = Math.max(0.0F, this.x0 * f + this.y0 * g + this.z0 * h);
        float g2 = Math.max(0.0F, this.x1 * f + this.y1 * g + this.z1 * h);
        return Math.min(1.0F, (f2 + g2) * 0.6F + 0.4F);
    }

    public void flatten(float[] floats, int i, float f) {
        if (f > 0.0F) {
            float g = Math.min(1.0F, f);
            float h = floats[i];
            float f2 = floats[i + 1];
            float g2 = floats[i + 2];
            float dx2 = this.dx;
            float dy2 = this.dy;
            float dz2 = this.dz;
            float h2 = h * dx2 + f2 * dy2 + g2 * dz2;
            if (this.opposite && h2 < 0.0F) {
                dx2 = -dx2;
                dy2 = -dy2;
                dz2 = -dz2;
                h2 = -h2;
            }
            float f3 = h2 + g * (0.853F + (0.273F + -0.126F * h2) * h2 - h2);
            float g3 = h - h2 * dx2;
            float h3 = f2 - h2 * dy2;
            float f4 = g2 - h2 * dz2;
            float g4 = g3 * g3 + h3 * h3 + f4 * f4;
            if (g4 < 1.0E-10F) {
                if (h2 > 0.0F) {
                    return;
                }
                if (Math.abs(dx2) < 0.9F) {
                    g3 = 0.0F;
                    h3 = dz2;
                    f4 = -dy2;
                } else {
                    g3 = dy2;
                    h3 = -dx2;
                    f4 = 0.0F;
                }
                g4 = g3 * g3 + h3 * h3 + f4 * f4;
            }
            float h4 = (float) Math.sqrt(Math.max(0.0F, 1.0F - f3 * f3) / g4);
            floats[i] = dx2 * f3 + g3 * h4;
            floats[i + 1] = dy2 * f3 + h3 * h4;
            floats[i + 2] = dz2 * f3 + f4 * h4;
        }
    }

    private static float length(float f, float g, float h) {
        return (float) Math.sqrt(f * f + g * g + h * h);
    }
}
