package net.efn.client.module.impl.render;

import net.efn.client.gui.ThemeTint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class PeepWorld {

    private static final float PINK_R = 1.0F;

    private static final float PINK_G = 0.30980393F;

    private static final float PINK_B = 0.6901961F;

    private static final float PINK_LUMA = luma(1.0F, 0.30980393F, 0.6901961F);

    private static final long HORIZON_STALE_NANOS = 250000000L;

    private static volatile int horizon;

    private static volatile float horizonShare;

    private static volatile long horizonAt;

    private PeepWorld() {}

    static void horizon(int i, float f) {
        horizon = i;
        horizonShare = f;
        horizonAt = System.nanoTime();
    }

    public static void fog(Vector4f vector4f) {
        float f = horizonShare;
        if (f > 0.0F && System.nanoTime() - horizonAt > 250000000L) {
            f = 0.0F;
        }
        fog(vector4f, horizon, f);
    }

    private static void fog(Vector4f vector4f, int i, float f) {
        float world = ThemeTint.world();
        if (world > 0.0F) {
            float g = luma(vector4f.x, vector4f.y, vector4f.z) / PINK_LUMA;
            vector4f.x = vector4f.x + (Math.min(1.0F, 1.0F * g) - vector4f.x) * world;
            vector4f.y = vector4f.y + (Math.min(1.0F, 0.30980393F * g) - vector4f.y) * world;
            vector4f.z = vector4f.z + (Math.min(1.0F, 0.6901961F * g) - vector4f.z) * world;
        }
        if (f > 0.0F) {
            vector4f.x = vector4f.x + ((i >> 16 & 0xFF) / 255.0F - vector4f.x) * f;
            vector4f.y = vector4f.y + ((i >> 8 & 0xFF) / 255.0F - vector4f.y) * f;
            vector4f.z = vector4f.z + ((i & 0xFF) / 255.0F - vector4f.z) * f;
        }
    }

    static int fog(int i) {
        return fog(i, 0, 0.0F);
    }

    static int fog(int i, int j, float f) {
        Vector4f vector4f =
                new Vector4f(
                        (i >> 16 & 0xFF) / 255.0F,
                        (i >> 8 & 0xFF) / 255.0F,
                        (i & 0xFF) / 255.0F,
                        1.0F);
        fog(vector4f, j, f);
        return channel(vector4f.x) << 16 | channel(vector4f.y) << 8 | channel(vector4f.z);
    }

    private static float luma(float f, float g, float h) {
        return 0.2126F * f + 0.7152F * g + 0.0722F * h;
    }

    private static int channel(float f) {
        return Math.max(0, Math.min(255, Math.round(f * 255.0F)));
    }
}
