package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface Paint2D {

    int argb(float f, float g);

    static Paint2D solid(int i) {
        return (value, value2) -> i;
    }

    static Paint2D linear(float f, float g, float h, float f2, int... i) {
        if (i.length == 0) {
            throw new IllegalArgumentException("gradient needs at least one colour");
        } else {
            float g2 = h - f;
            float h2 = f2 - g;
            float f3 = g2 * g2 + h2 * h2;
            float g3 = f3 < 1.0E-8F ? 0.0F : 1.0F / f3;
            return (k, value) -> stop(i, ((k - f) * g2 + (value - g) * h2) * g3);
        }
    }

    static Paint2D radial(float f, float g, float h, int... k) {
        if (k.length == 0) {
            throw new IllegalArgumentException("gradient needs at least one colour");
        } else {
            float f2 = 1.0F / Math.max(1.0E-4F, h);
            return (i, j) -> {
                float g2 = i - f;
                float h2 = j - g;
                return stop(k, (float) Math.sqrt(g2 * g2 + h2 * h2) * f2);
            };
        }
    }

    static Paint2D noise(int i, int j, float f, float g) {
        float h = 1.0F / Math.max(0.001F, f);
        return (value, k) -> {
            float f2 = Noise.fbm(value * h, k * h, 4) * g + 0.5F;
            return Colors.mix(i, j, f2 < 0.0F ? 0.0F : Math.min(f2, 1.0F));
        };
    }

    static Paint2D grain(int i, int j, float f, float g, float h) {
        double d = Math.toRadians(f);
        float f2 = (float) Math.cos(d);
        float g2 = (float) Math.sin(d);
        float h2 = 1.0F / Math.max(0.1F, g);
        return (value, m) -> {
            float f3 = -value * g2 + m * f2;
            float g3 = value * f2 + m * g2;
            float h3 = Noise.fbm(g3 * 0.02F, f3 * 0.05F, 3) * h;
            float f4 = (f3 + h3) * h2;
            float g4 = f4 - (float) Math.floor(f4);
            float h4 = Paint.smooth(0.6F, 0.8F, g4) * (1.0F - Paint.smooth(0.85F, 1.0F, g4));
            float f5 = h4 * 0.8F + (Noise.noise(g3 * 0.015F, f3 * h2 * 1.7F) * 0.5F + 0.5F) * 0.3F;
            return Colors.mix(i, j, f5 < 0.0F ? 0.0F : Math.min(f5, 1.0F));
        };
    }

    private static int stop(int[] ints, float f) {
        if (ints.length == 1) {
            return ints[0];
        } else {
            f = f < 0.0F ? 0.0F : Math.min(f, 1.0F);
            float g = f * (ints.length - 1);
            int i = Math.min((int) g, ints.length - 2);
            return Colors.mix(ints[i], ints[i + 1], g - i);
        }
    }
}
