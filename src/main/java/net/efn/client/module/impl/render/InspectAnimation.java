package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
final class InspectAnimation {

    static final String CLASSIC = "Classic";

    static final String TWIRL = "Twirl";

    static final int X = 0;

    static final int Y = 1;

    static final int Z = 2;

    static final int YAW = 3;

    static final int PITCH = 4;

    static final int ROLL = 5;

    static final int FLIP = 6;

    static final int SPIN = 7;

    static final int PART_A = 8;

    static final int PART_B = 9;

    static final int SHINE = 10;

    static final int SLIDE = 11;

    static final int DRAW = 12;

    static final int SHEATH = 13;

    static final int OFF_HAND = 14;

    static final int OFF_AT = 15;

    static final int CHANNELS = 16;

    static final float GRIP_X = 0.07F;

    static final float GRIP_Y = -0.05F;

    static final float GRIP_Z = -0.02F;

    static final float CENTER_X = 0.07F;

    static final float CENTER_Y = 0.2F;

    static final float CENTER_Z = 0.07F;

    static final float BLADE_Y = 0.94F;

    static final float BLADE_Z = 0.342F;

    static final float SECONDS = 3.2F;

    private static final float[][] CLASSIC_KEYS =
            new float[][] {
                {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                {0.14F, -0.24F, 0.17F, 0.1F, 60.0F, -10.0F, 50.0F, 0.0F, 0.0F},
                {0.3F, -0.25F, 0.18F, 0.11F, 70.0F, -5.0F, 46.0F, 0.0F, 0.0F},
                {0.4F, -0.24F, 0.21F, 0.11F, 66.0F, -6.0F, 48.0F, 180.0F, 0.0F},
                {0.46F, -0.24F, 0.2F, 0.11F, 64.0F, -7.0F, 49.0F, 184.0F, 0.0F},
                {0.56F, -0.25F, 0.19F, 0.11F, 70.0F, -5.0F, 46.0F, 360.0F, 0.0F},
                {0.62F, -0.25F, 0.18F, 0.11F, 70.0F, -5.0F, 46.0F, 360.0F, 0.0F},
                {0.8F, -0.23F, 0.19F, 0.1F, 56.0F, -10.0F, 52.0F, 366.0F, 0.0F},
                {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F}
            };

    private static final float[][] TWIRL_KEYS =
            new float[][] {
                {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                {0.12F, -0.2F, 0.15F, 0.08F, 75.0F, -6.0F, 20.0F, 0.0F, -20.0F},
                {0.22F, -0.21F, 0.2F, 0.09F, 78.0F, -6.0F, 22.0F, 0.0F, 0.0F},
                {0.34F, -0.22F, 0.3F, 0.1F, 80.0F, -4.0F, 24.0F, 0.0F, 180.0F},
                {0.46F, -0.21F, 0.2F, 0.09F, 78.0F, -6.0F, 22.0F, 0.0F, 360.0F},
                {0.7F, -0.24F, 0.17F, 0.1F, 68.0F, -8.0F, 42.0F, 0.0F, 360.0F},
                {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F}
            };

    private static final InspectAnimation.Clip CLASSIC_CLIP =
            new InspectAnimation.Clip(CLASSIC_KEYS, 3.2F, 0.3F, 0.62F);

    private static final InspectAnimation.Clip TWIRL_CLIP =
            new InspectAnimation.Clip(TWIRL_KEYS, 3.2F, 0.22F, 0.46F);

    private InspectAnimation() {}

    static InspectAnimation.Clip clip(String string) {
        return "Twirl".equals(string) ? TWIRL_CLIP : CLASSIC_CLIP;
    }

    static void place(
            MatrixStack matrices, float[] floats, float f, int i, float g, float h, float f2) {
        float g2 = 0.017453292F * f;
        float h2 = MathHelper.wrapDegrees(floats[3]) * g2 * i;
        float f3 = MathHelper.wrapDegrees(floats[4]) * g2;
        float g3 = MathHelper.wrapDegrees(floats[5]) * g2 * i;
        float h3 = MathHelper.wrapDegrees(floats[6]) * g2 * i;
        float f4 = MathHelper.wrapDegrees(floats[7]) * g2;
        matrices.translate(floats[0] * f * i, floats[1] * f, floats[2] * f);
        float g4 = 0.07F * i;
        matrices.multiply(
                new Quaternionf().rotateX(f3).rotateZ(g3).rotateY(h2), g4, -0.05F, -0.02F);
        matrices.multiply(new Quaternionf().rotateX(f4), g * i, h, f2);
        matrices.multiply(
                new Quaternionf().rotateAxis(h3, 0.0F, 0.94F, 0.342F), g4, -0.05F, -0.02F);
    }

    static float[] sample(float[][] floatGrid, float f, float[] floats) {
        f = MathHelper.clamp(f, 0.0F, 1.0F);
        int i = 0;
        while (i < floatGrid.length - 2 && f > floatGrid[i + 1][0]) {
            i++;
        }
        float[] floats2 = floatGrid[i + 1];
        float[] floats3 = floatGrid[i];
        float g = floats2[0] - floats3[0];
        float h = g <= 0.0F ? 1.0F : (f - floats3[0]) / g;
        for (int j = 0; j < 16; j++) {
            floats[j] =
                    hermite(
                            value(floats3, j),
                            value(floats2, j),
                            slope(floatGrid, i, j) * g,
                            slope(floatGrid, i + 1, j) * g,
                            h);
        }
        return floats;
    }

    static float[] sampleLoop(float[][] floatGrid, float f, float[] floats) {
        f -= (float) Math.floor(f);
        int i = floatGrid.length;
        int j = 0;
        while (j < i - 2 && f > floatGrid[j + 1][0]) {
            j++;
        }
        float[] floats2 = floatGrid[j + 1];
        float[] floats3 = floatGrid[j];
        float g = floats2[0] - floats3[0];
        float h = g <= 0.0F ? 1.0F : (f - floats3[0]) / g;
        for (int k = 0; k < 16; k++) {
            floats[k] =
                    hermite(
                            value(floats3, k),
                            value(floats2, k),
                            loopSlope(floatGrid, j, k) * g,
                            loopSlope(floatGrid, j + 1, k) * g,
                            h);
        }
        return floats;
    }

    private static float loopSlope(float[][] floatGrid, int i, int j) {
        int k = floatGrid.length;
        float f = value(floatGrid[k - 1], j) - value(floatGrid[0], j);
        float g;
        float h;
        if (i == 0) {
            h = floatGrid[k - 2][0] - 1.0F;
            g = value(floatGrid[k - 2], j) - f;
        } else {
            h = floatGrid[i - 1][0];
            g = value(floatGrid[i - 1], j);
        }
        float f2;
        float g2;
        if (i == k - 1) {
            g2 = floatGrid[1][0] + 1.0F;
            f2 = value(floatGrid[1], j) + f;
        } else {
            g2 = floatGrid[i + 1][0];
            f2 = value(floatGrid[i + 1], j);
        }
        return (f2 - g) / (g2 - h);
    }

    private static float value(float[] floats, int i) {
        return i + 1 < floats.length ? floats[i + 1] : 0.0F;
    }

    private static float slope(float[][] floatGrid, int i, int j) {
        if (i != 0 && i != floatGrid.length - 1) {
            float[] floats = floatGrid[i - 1];
            float[] floats2 = floatGrid[i + 1];
            return (value(floats2, j) - value(floats, j)) / (floats2[0] - floats[0]);
        } else {
            return 0.0F;
        }
    }

    private static float hermite(float f, float g, float h, float f2, float g2) {
        float h2 = g2 * g2;
        float f3 = h2 * g2;
        return (2.0F * f3 - 3.0F * h2 + 1.0F) * f
                + (f3 - 2.0F * h2 + g2) * h
                + (-2.0F * f3 + 3.0F * h2) * g
                + (f3 - h2) * f2;
    }

    @Environment(EnvType.CLIENT)
    static final class Clip {

        final float[][] keys;

        final float seconds;

        final float loopEnter;

        final float loopExit;

        private final float[][] loop;

        Clip(float[][] floatGrid, float f) {
            this(floatGrid, f, 0.0F, 0.0F);
        }

        Clip(float[][] keys2, float seconds2, float loopEnter2, float loopExit2) {
            this.keys = keys2;
            this.seconds = seconds2;
            this.loopEnter = loopEnter2;
            this.loopExit = loopExit2;
            this.loop = loopExit2 > loopEnter2 ? cut(keys2, loopEnter2, loopExit2) : null;
        }

        boolean loops() {
            return this.loop != null;
        }

        float[] sample(float f, float[] floats) {
            return this.loop != null && f > this.loopEnter && f < this.loopExit
                    ? InspectAnimation.sampleLoop(
                            this.loop,
                            (f - this.loopEnter) / (this.loopExit - this.loopEnter),
                            floats)
                    : InspectAnimation.sample(this.keys, f, floats);
        }

        private static float[][] cut(float[][] floatGrid, float f, float g) {
            int i = -1;
            int j = -1;
            for (int k = 0; k < floatGrid.length; k++) {
                if (Math.abs(floatGrid[k][0] - f) < 1.0E-4F) {
                    i = k;
                }
                if (Math.abs(floatGrid[k][0] - g) < 1.0E-4F) {
                    j = k;
                }
            }
            if (i >= 0 && j > i) {
                float[][] floatGrid2 = new float[j - i + 1][];
                for (int m = i; m <= j; m++) {
                    floatGrid2[m - i] = (float[]) floatGrid[m].clone();
                    floatGrid2[m - i][0] = (floatGrid[m][0] - f) / (g - f);
                }
                return floatGrid2;
            } else {
                throw new IllegalArgumentException("a loop starts and ends on keys");
            }
        }
    }
}
