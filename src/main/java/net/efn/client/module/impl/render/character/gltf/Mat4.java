package net.efn.client.module.impl.render.character.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class Mat4 {

    private Mat4() {}

    static float[] identity() {
        float[] floats = new float[16];
        floats[15] = 1.0F;
        floats[10] = 1.0F;
        floats[5] = 1.0F;
        floats[0] = 1.0F;
        return floats;
    }

    static float[] mul(float[] floats, float[] floats2) {
        float[] floats3 = new float[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                float f = 0.0F;
                for (int k = 0; k < 4; k++) {
                    f += floats[k * 4 + j] * floats2[i * 4 + k];
                }
                floats3[i * 4 + j] = f;
            }
        }
        return floats3;
    }

    static float[] local(JsonObject json) {
        JsonArray array = GltfFile.array(json, "matrix");
        if (array.size() != 16) {
            float[] floats = vec(json, "translation", 0.0F, 3);
            float[] floats2 = vec(json, "scale", 1.0F, 3);
            float[] floats3;
            if (json.has("rotation")) {
                floats3 = vec(json, "rotation", 0.0F, 4);
            } else {
                float[] floats4 = new float[] {0.0F, 0.0F, 0.0F, 0.0F};
                floats3 = floats4;
                floats4[3] = 1.0F;
            }
            float f = floats3[0];
            float g = floats3[1];
            float h = floats3[2];
            float f2 = floats3[3];
            return new float[] {
                (1.0F - 2.0F * (g * g + h * h)) * floats2[0],
                2.0F * (f * g + h * f2) * floats2[0],
                2.0F * (f * h - g * f2) * floats2[0],
                0.0F,
                2.0F * (f * g - h * f2) * floats2[1],
                (1.0F - 2.0F * (f * f + h * h)) * floats2[1],
                2.0F * (g * h + f * f2) * floats2[1],
                0.0F,
                2.0F * (f * h + g * f2) * floats2[2],
                2.0F * (g * h - f * f2) * floats2[2],
                (1.0F - 2.0F * (f * f + g * g)) * floats2[2],
                0.0F,
                floats[0],
                floats[1],
                floats[2],
                1.0F
            };
        } else {
            float[] floats5 = new float[16];
            for (int i = 0; i < 16; i++) {
                floats5[i] = array.get(i).getAsFloat();
            }
            return floats5;
        }
    }

    private static float[] vec(JsonObject json, String string, float f, int i) {
        float[] floats = new float[i];
        JsonArray array = GltfFile.array(json, string);
        for (int j = 0; j < i; j++) {
            floats[j] = j < array.size() ? array.get(j).getAsFloat() : f;
        }
        return floats;
    }

    static float[] rollAbout(float f, float[] floats) {
        double d = Math.toRadians(f);
        float g = (float) Math.cos(d);
        float h = (float) Math.sin(d);
        float[] floats2 = identity();
        floats2[0] = g;
        floats2[1] = h;
        floats2[4] = -h;
        floats2[5] = g;
        floats2[12] = floats[0] - g * floats[0] + h * floats[1];
        floats2[13] = floats[1] - h * floats[0] - g * floats[1];
        floats2[14] = 0.0F;
        return floats2;
    }

    static float[] point(float[] floats, float f, float g, float h) {
        return new float[] {
            floats[0] * f + floats[4] * g + floats[8] * h + floats[12],
            floats[1] * f + floats[5] * g + floats[9] * h + floats[13],
            floats[2] * f + floats[6] * g + floats[10] * h + floats[14]
        };
    }

    static float[] dir(float[] floats, float f, float g, float h) {
        return new float[] {
            floats[0] * f + floats[4] * g + floats[8] * h,
            floats[1] * f + floats[5] * g + floats[9] * h,
            floats[2] * f + floats[6] * g + floats[10] * h
        };
    }

    static void normalise(float[] floats) {
        float f =
                (float)
                        Math.sqrt(
                                floats[0] * floats[0]
                                        + floats[1] * floats[1]
                                        + floats[2] * floats[2]);
        if (f < 1.0E-12F) {
            floats[1] = 1.0F;
        } else {
            floats[0] /= f;
            floats[1] /= f;
            floats[2] /= f;
        }
    }

    static float[] faceNormal(float[] floats, int i, int j, int k) {
        float f = floats[j * 3] - floats[i * 3];
        float g = floats[j * 3 + 1] - floats[i * 3 + 1];
        float h = floats[j * 3 + 2] - floats[i * 3 + 2];
        float f2 = floats[k * 3] - floats[i * 3];
        float g2 = floats[k * 3 + 1] - floats[i * 3 + 1];
        float h2 = floats[k * 3 + 2] - floats[i * 3 + 2];
        return new float[] {g * h2 - h * g2, h * f2 - f * h2, f * g2 - g * f2};
    }

    static float area(float[] floats, int i, int j, int k) {
        float[] floats2 = faceNormal(floats, i, j, k);
        return (float)
                        Math.sqrt(
                                floats2[0] * floats2[0]
                                        + floats2[1] * floats2[1]
                                        + floats2[2] * floats2[2])
                * 0.5F;
    }
}
