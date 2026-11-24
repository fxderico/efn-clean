package net.efn.client.module.impl.render.character;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class CharacterRotationTable {

    final float[] values = new float[153];

    CharacterRotationTable() {}

    void update(float f, float g, float h) {
        for (int i = 0; i <= 16; i++) {
            float f2 = i / 16.0F;
            float g2 = (float) Math.cos(f * f2);
            float h2 = (float) Math.sin(f * f2);
            float f3 = (float) Math.cos(g * f2);
            float g3 = (float) Math.sin(g * f2);
            float h3 = (float) Math.cos(h * f2);
            float f4 = (float) Math.sin(h * f2);
            int j = i * 9;
            this.values[j] = h3 * f3;
            this.values[j + 1] = h3 * g3 * h2 - f4 * g2;
            this.values[j + 2] = h3 * g3 * g2 + f4 * h2;
            this.values[j + 3] = f4 * f3;
            this.values[j + 4] = f4 * g3 * h2 + h3 * g2;
            this.values[j + 5] = f4 * g3 * g2 - h3 * h2;
            this.values[j + 6] = -g3;
            this.values[j + 7] = f3 * h2;
            this.values[j + 8] = f3 * g2;
        }
    }
}
