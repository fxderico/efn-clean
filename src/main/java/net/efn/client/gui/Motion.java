package net.efn.client.gui;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
final class Motion {

    static final float SETTLE = 0.001F;

    private Motion() {}

    static float step(float f, float g, float h, float f2) {
        if (f != g && !Float.isNaN(f)) {
            float g2 = ClickGuiRenderer.smooth(f, g, h, f2);
            return Math.abs(g2 - g) < 0.001F ? g : g2;
        } else {
            return g;
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Rows {

        private List<?> rows;

        private float[] hover = new float[0];

        private float[] on = new float[0];

        Rows() {}

        void bind(List<?> rows2) {
            if (rows2 != this.rows) {
                this.rows = rows2;
                this.hover = new float[rows2.size()];
                this.on = new float[rows2.size()];
                Arrays.fill(this.hover, 0.0F / 0.0F);
                Arrays.fill(this.on, 0.0F / 0.0F);
            }
        }

        float hover(int i, boolean bl, float f, float g) {
            float h = this.hover[i];
            float f2 = bl ? 1.0F : 0.0F;
            return h == f2 ? f2 : Motion.step(h, f2, f, g);
        }

        float on(int i, boolean bl, float f, float g) {
            float h = this.on[i];
            float f2 = bl ? 1.0F : 0.0F;
            return h == f2 ? f2 : Motion.step(h, f2, f, g);
        }
    }

    @Environment(EnvType.CLIENT)
    static final class Store {

        private final Int2FloatOpenHashMap values = new Int2FloatOpenHashMap();

        Store() {
            this.values.defaultReturnValue(0.0F / 0.0F);
        }

        float ease(int i, float f, float g, float h) {
            float value = this.values.get(i);
            float step = Motion.step(value, f, g, h);
            if (step != value) {
                this.values.put(i, step);
            }
            return step;
        }
    }
}
