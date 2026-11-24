package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
interface SkyPaint {

    float HORIZON = 0.72F;

    float RADIANS_PER_UNIT = 1.0471976F;

    SkyImage upload(String string, int i, int j, int[] ints);

    SkyImage upload(String string, int i, int j, int[][] intGrid);

    void gradient(boolean bl, float f, float g, float h, float f2, int i, int j);

    void band(SkyImage skyImage, float f, float g, float h, float f2, int i, boolean bl, int j);

    void sprite(
            SkyImage skyImage, float f, float g, float h, float f2, float g2, int i, boolean bl);

    void glow(float f, float g, float h, int i, float f2);
}
